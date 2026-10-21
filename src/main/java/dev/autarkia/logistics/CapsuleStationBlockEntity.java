package dev.autarkia.logistics;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * {@code capsule_station} (04 section 1.4): one block whose mouth (its {@code facing}) connects to a capsule tube.
 * Buffers: input 9 slots (automation inserts only), output 9 slots (automation extracts only); the item capability
 * covers every face except the mouth. Settings: mode SEND / RECEIVE / BOTH, filter (rule C8: 9 ghost slots, whitelist
 * or blacklist, match components or item only), priority 0 to 9. A grid consumer with an 800 FE buffer; each launch
 * costs 40 FE. The anchor station of a network (lowest {@code BlockPos.asLong}) owns, saves and ticks its capsules;
 * the network runs only while complete, within the caps and with all chunks loaded (rule C3).
 */
public class CapsuleStationBlockEntity extends AutarkiaBlockEntity implements Ticking, GridMember, JadeInfo, MenuProvider, GeoBlockEntity,
    dev.autarkia.colony.PatternDataHolder {
    /** Station mode. */
    public enum Mode {
        SEND, RECEIVE, BOTH;

        /** Return true when the station launches capsules. */
        public boolean sends() {
            return this != RECEIVE;
        }

        /** Return true when the station is a target. */
        public boolean receives() {
            return this != SEND;
        }

        /** Return the translation key of the mode name. */
        public String key() {
            return "screen.autarkia.capsule_station." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final int SLOTS = 9;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.capsule_station.idle");
    private static final RawAnimation PUFF = RawAnimation.begin().thenPlay("animation.capsule_station.puff");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(SLOTS, SLOTS, s -> !dev.autarkia.storage.CrateBlockEntity.isFilledCrate(s), this::setChanged);
    private final EnergyBuffer energy = new EnergyBuffer(800, CapsuleNetwork.LAUNCH_FE, 0, this::setChanged);
    private final NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
    private final CapsuleNetwork net = new CapsuleNetwork(this);
    private CapsuleTopology topology = CapsuleTopology.EMPTY;
    private int topologyEpoch = -1;
    private long retryAt;
    private Mode mode = Mode.RECEIVE;
    private int priority;
    private boolean whitelist = true;
    private boolean matchComponents;
    private boolean noPower;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> mode.ordinal();
                case 1 -> priority;
                case 2 -> whitelist ? 1 : 0;
                case 3 -> matchComponents ? 1 : 0;
                case 4 -> topology.tubes.size();
                case 5 -> topology.stations.size();
                case 6 -> capsulesInFlight();
                default -> displayState().ordinal();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public CapsuleStationBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CAPSULE_STATION.get(), pos, state);
    }

    /** Return the mouth direction (the face that connects to the tube). */
    public Direction mouth() {
        return MachineBlock.front(getBlockState());
    }

    /** Return the center of the mouth face in world coordinates. */
    public Vec3 mouthPoint() {
        return Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(mouth().getUnitVec3i()).scale(0.5));
    }

    /** Return the item handler of a face: insert into the input, extract from the output; none at the mouth. */
    public @Nullable ResourceHandler<ItemResource> items(@Nullable Direction side) {
        return side == mouth() ? null : inv.automation();
    }

    /** Return the 18 slots: 0 to 8 input, 9 to 17 output. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the 9 ghost filter stacks (live list). */
    public NonNullList<ItemStack> filter() {
        return filter;
    }

    /** Return the menu data: mode, priority, whitelist, components, tubes, stations, capsules in flight, state. */
    public ContainerData data() {
        return data;
    }

    /** Return the capsule network this station owns (the anchor's holds the capsules). */
    public CapsuleNetwork network() {
        return net;
    }

    /** Return the network view of this station (server). */
    public CapsuleTopology topology() {
        return topology;
    }

    /** Return the mode. */
    public Mode mode() {
        return mode;
    }

    /** Return the priority 0 to 9 (higher is chosen first). */
    public int priority() {
        return priority;
    }

    /** Set the no-power flag shown while a send waits for 40 FE. */
    void setNoPower(boolean on) {
        noPower = on;
    }

    /** Return true when every ghost slot is empty (a default sink). */
    public boolean filterEmpty() {
        return filter.stream().allMatch(ItemStack::isEmpty);
    }

    /** Return true when the stack passes the filter (whitelist or blacklist, items or items with components). */
    public boolean matches(ItemStack stack) {
        boolean listed = false;
        for (ItemStack f : filter) {
            if (!f.isEmpty() && (matchComponents ? ItemStack.isSameItemSameComponents(f, stack) : ItemStack.isSameItem(f, stack))) {
                listed = true;
                break;
            }
        }
        return whitelist == listed;
    }

    /** Apply a menu button: 0 send, 1 receive, 2 both, 3 priority down, 4 priority up, 5 whitelist, 6 match components. */
    public boolean button(int id) {
        switch (id) {
            case 0, 1, 2 -> mode = Mode.values()[id];
            case 3 -> priority = Math.max(0, priority - 1);
            case 4 -> priority = Math.min(9, priority + 1);
            case 5 -> whitelist = !whitelist;
            case 6 -> matchComponents = !matchComponents;
            default -> {
                return false;
            }
        }
        setChanged();
        return true;
    }

    /** Set ghost slot i to a one-item copy of the stack (empty clears it). */
    public void setGhost(int i, ItemStack stack) {
        filter.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    /** Return the first non-empty input slot, or -1. */
    public int firstInput() {
        for (int i = 0; i < SLOTS; i++) {
            if (!inv.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** Return true when the output buffer takes the reserved stacks and then the stack (simulated, nothing changes). */
    public boolean fitsOutput(List<ItemStack> reserved, ItemStack stack) {
        List<ItemStack> all = new java.util.ArrayList<>(reserved);
        all.add(stack);
        return inv.fitsOutputs(all);
    }

    /** Put an arriving stack into the output buffer; returns false and changes nothing when it does not fit. */
    public boolean acceptDelivery(ItemStack stack) {
        if (!inv.fitsOutputs(List.of(stack))) {
            return false;
        }
        inv.addOutputs(List.of(stack.copy()));
        return true;
    }

    /** Return true when the input buffer takes the other returning stacks and then the stack (simulated). */
    public boolean fitsReturn(List<ItemStack> others, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            for (ItemStack o : others) {
                if (inv.insert(ItemResource.of(o), o.getCount(), tx) < o.getCount()) {
                    return false;
                }
            }
            return inv.insert(ItemResource.of(stack), stack.getCount(), tx) == stack.getCount();
        }
    }

    /** Put a returning stack back into the input buffer in one transaction; returns false when it does not fit whole. */
    public boolean acceptReturn(ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            if (inv.insert(ItemResource.of(stack), stack.getCount(), tx) == stack.getCount()) {
                tx.commit();
                return true;
            }
        }
        return false;
    }

    /** Play the valve puff animation on tracking clients. */
    public void puff() {
        triggerAnim("main", "puff");
    }

    /** Return the anchor station of this station's network, or null (server). */
    public @Nullable CapsuleStationBlockEntity anchorStation() {
        Long a = topology.anchor();
        return a == null || level == null ? null : a == worldPosition.asLong() ? this : CapsuleNetworks.station(level, a);
    }

    /** Return the number of capsules in flight in this station's network (server). */
    public int capsulesInFlight() {
        CapsuleStationBlockEntity a = anchorStation();
        return a == null ? net.size() : a.net.size();
    }

    /** Return the state shown in the menu and by Jade (server). */
    public CapsuleNetwork.State displayState() {
        if (topology.tooLarge) {
            return CapsuleNetwork.State.TOO_LARGE;
        }
        if (topology.tubes.isEmpty() && !topology.incomplete) {
            return CapsuleNetwork.State.NO_TUBE;
        }
        CapsuleStationBlockEntity a = anchorStation();
        if (a == null || topology.incomplete || level == null || !topology.loaded(level)) {
            return CapsuleNetwork.State.PAUSED;
        }
        return noPower ? CapsuleNetwork.State.NO_POWER : a.net.state();
    }

    private void refreshTopology(ServerLevel sl) {
        int e = CapsuleNetworks.epoch(sl);
        if (e == topologyEpoch && !(topology.incomplete && sl.getGameTime() >= retryAt)) {
            return;
        }
        topologyEpoch = e;
        retryAt = sl.getGameTime() + 20;
        topology = CapsuleTopology.flood(sl, worldPosition.relative(mouth()), AutarkiaConfig.TUBE_MAX_NETWORK_TUBES.get());
        net.setTopology(topology);
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        refreshTopology(sl);
        boolean anchor = Long.valueOf(worldPosition.asLong()).equals(topology.anchor());
        if (topology.tooLarge || topology.incomplete || !topology.loaded(sl)) {
            if (net.holds()) {
                net.pause(sl, topology.tooLarge ? CapsuleNetwork.State.TOO_LARGE : CapsuleNetwork.State.PAUSED);
            }
            return;
        }
        if (anchor) {
            net.tickAnchor(sl);
        } else if (net.holds()) {
            net.handOff(sl);
        } else {
            net.unregister(sl);
        }
    }

    @Override
    public void clientTick() {
        net.clientTick(level);
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            if (level.isClientSide()) {
                net.clientRemove(level);
            } else {
                net.unregister(level);
            }
        }
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inv.get(i).copy());
            }
            CapsuleNetworks.bump(sl);
            net.ownerRemoved(sl);
        }
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return energy;
    }

    @Override
    public int gridMaxIn() {
        return energy.room() > 0 ? CapsuleNetwork.LAUNCH_FE : 0;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int workDraw() {
        return mode.sends() && firstInput() >= 0 ? CapsuleNetwork.LAUNCH_FE / CapsuleNetwork.LAUNCH_INTERVAL : 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        int in = 0;
        int outCount = 0;
        for (int i = 0; i < SLOTS; i++) {
            in += inv.get(i).getCount();
            outCount += inv.get(SLOTS + i).getCount();
        }
        out.add(Component.translatable("jade.autarkia.capsule_station", Component.translatable(mode.key()), priority));
        out.add(Component.translatable("jade.autarkia.capsule_station_buffers", in, outCount));
        BlockPos next = nextTarget();
        out.add(next == null ? Component.translatable("jade.autarkia.capsule_station_no_target")
            : Component.translatable("jade.autarkia.capsule_station_target", next.getX(), next.getY(), next.getZ()));
        out.add(Component.translatable("jade.autarkia.capsule_station_state", Component.translatable(displayState().key())));
    }

    private @Nullable BlockPos nextTarget() {
        int slot = mode.sends() ? firstInput() : -1;
        CapsuleStationBlockEntity a = anchorStation();
        if (slot < 0 || a == null || !topology.runnable()) {
            return null;
        }
        CapsuleNetwork.Choice c = a.net.choose(level, inv.get(slot), worldPosition.asLong(), -1);
        return c == null ? null : BlockPos.of(c.station());
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new CapsuleStationMenu(id, playerInv, this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CapsuleStationBlockEntity> main = new AnimationController<>("main", 0, test -> test.setAndContinue(IDLE));
        main.triggerableAnim("puff", PUFF);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void writePatternData(ValueOutput out) {
        writeSettings(out);
    }

    @Override
    public void readPatternData(ValueInput in) {
        readSettings(in);
        setChanged();
        syncNow();
    }

    private void readSettings(ValueInput in) {
        List<ItemStack> f = in.read("filter", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < filter.size(); i++) {
            filter.set(i, i < f.size() ? f.get(i) : ItemStack.EMPTY);
        }
        mode = Mode.values()[Math.clamp(in.getIntOr("mode", Mode.RECEIVE.ordinal()), 0, Mode.values().length - 1)];
        priority = Math.clamp(in.getIntOr("priority", 0), 0, 9);
        whitelist = in.getBooleanOr("whitelist", true);
        matchComponents = in.getBooleanOr("matchComponents", false);
    }

    private void writeSettings(ValueOutput out) {
        out.store("filter", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(filter));
        out.putInt("mode", mode.ordinal());
        out.putInt("priority", priority);
        out.putBoolean("whitelist", whitelist);
        out.putBoolean("matchComponents", matchComponents);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        writeSettings(out);
        inv.serialize(out.child("inv"));
        energy.serialize(out.child("energy"));
        out.putBoolean("noPower", noPower);
        net.save(out.child("net"));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        readSettings(in);
        noPower = in.getBooleanOr("noPower", false);
        in.child("inv").ifPresent(inv::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        in.child("net").ifPresent(net::load);
    }
}
