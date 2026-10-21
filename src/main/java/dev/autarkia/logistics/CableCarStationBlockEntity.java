package dev.autarkia.logistics;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.energy.WrenchItem;
import dev.autarkia.energy.Wrenchable;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import dev.autarkia.storage.CrateBlockEntity;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * {@code cable_car_station} (04 section 1.5), a 2x3x2 big block (rules B1 to B7; all state in the center). Buffers:
 * input 27 slots (automation inserts, filled crates refused), output 27 slots (automation extracts), gondola parts 2
 * slots (steel plates and iron chains); a 6,400 FE buffer that takes FE on every face of every position. A wrench on
 * station A, then on station B links them into a line ({@link #link}); the station with the lowest {@code asLong}
 * owns, saves and ticks the {@link CableLine}. Breaking either station lets every gondola of the line fall.
 */
public class CableCarStationBlockEntity extends AutarkiaBlockEntity implements Ticking, GridMember, JadeInfo, MenuProvider, GeoBlockEntity, Wrenchable,
    dev.autarkia.colony.Colony.DeliveryTarget {
    /** Result of a link attempt; {@code blocked} is set for BLOCKED. */
    public record Link(Result result, @Nullable BlockPos blocked, double length) {
    }

    /** Outcome of a link attempt. */
    public enum Result { OK, SAME, NOT_STATION, LINKED, TOO_SHORT, TOO_LONG, TOO_STEEP, BLOCKED }

    public static final int SLOTS = 27;
    public static final int CAPACITY = 6400;
    public static final int MIN_LENGTH = 16;
    public static final int MAX_HEIGHT = 48;
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.cable_car_station.wheel_spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cable_car_station.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(SLOTS, SLOTS, s -> !CrateBlockEntity.isFilledCrate(s), this::setChanged);
    private final ItemStacksResourceHandler parts = new ItemStacksResourceHandler(2) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.is(AutarkiaItems.STEEL_PLATE.get()) || resource.is(Items.IRON_CHAIN);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 256, 0, this::setChanged);
    private final LinkerRef linker = new LinkerRef();
    private @Nullable BlockPos partner;
    private @Nullable Vec3 anchorA;
    private @Nullable Vec3 anchorB;
    private @Nullable CableLine line;
    private boolean running;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            CableLine l = ownerLine();
            return switch (i) {
                case 0 -> l == null ? 0 : l.target();
                case 1 -> l == null ? 0 : l.maxCount();
                case 2 -> l == null ? 0 : l.count();
                case 3 -> l == null ? 0 : (int) Math.round(l.geometry().chord);
                case 4 -> l == null ? CableLine.State.PAUSED.ordinal() : l.state().ordinal();
                case 5 -> l == null ? 0 : (int) Math.round(l.maxSwing() * 10);
                case 6 -> energy.stored();
                case 7 -> energy.capacity();
                default -> partner == null ? 0 : 1;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public CableCarStationBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CABLE_CAR_STATION.get(), pos, state);
    }

    /** Return the 54 buffer slots: 0 to 26 input, 27 to 53 output. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the two gondola part slots. */
    public ItemStacksResourceHandler parts() {
        return parts;
    }

    /** Return the gondola part slots: colony deliveries of {@code steel_plate} and {@code iron_chain} go there. */
    @Override
    public ResourceHandler<ItemResource> deliveryHandler() {
        return parts;
    }

    /** Return the handler other blocks see: insert into the input, extract from the output. */
    public ResourceHandler<ItemResource> items() {
        return inv.automation();
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the menu data: count, max, gondolas, length, state, swing x10, energy, capacity, linked. */
    public ContainerData data() {
        return data;
    }

    /** Return the linked station centre, or null. */
    public @Nullable BlockPos partner() {
        return partner;
    }

    /** Return true when this station owns its line (lowest {@code asLong} of the two). */
    public boolean isOwner() {
        return partner != null && worldPosition.asLong() < partner.asLong();
    }

    /** Return the geometry of the line (A = owner), or null while unlinked. */
    public @Nullable CableGeometry geometry() {
        return anchorA == null || anchorB == null ? null : new CableGeometry(anchorA, anchorB);
    }

    /** Return the line when this station owns it, else null. */
    public @Nullable CableLine line() {
        return line;
    }

    /** Return the line of this station, held by the owner (null while unlinked or the owner is not loaded). */
    public @Nullable CableLine ownerLine() {
        if (line != null || partner == null || level == null) {
            return line;
        }
        return level.isLoaded(partner) && level.getBlockEntity(partner) instanceof CableCarStationBlockEntity o ? o.line : null;
    }

    /** Return true while the bull wheel turns (synced to clients). */
    public boolean running() {
        return running;
    }

    void setRunning(boolean on) {
        if (running != on) {
            running = on;
            syncNow();
        }
    }

    /** Return true when every input slot is empty. */
    public boolean inputEmpty() {
        for (int i = 0; i < SLOTS; i++) {
            if (!inv.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Return the block positions of this station's footprint. */
    public Set<BlockPos> footprint() {
        return ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState()).getGlobalPositions();
    }

    private int outputRoom(ItemStack proto) {
        int room = 0;
        for (int i = SLOTS; i < inv.size(); i++) {
            ItemStack cur = inv.get(i);
            if (cur.isEmpty()) {
                room += proto.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(cur, proto)) {
                room += cur.getMaxStackSize() - cur.getCount();
            }
        }
        return room;
    }

    /** Move as much of the gondola cargo as fits into the output buffer. */
    void unload(GondolaEntity g) {
        ItemStacksResourceHandler cargo = g.cargo();
        for (int s = 0; s < cargo.size(); s++) {
            int amt = cargo.getAmountAsInt(s);
            if (amt == 0) {
                continue;
            }
            ItemResource res = cargo.getResource(s);
            int n = Math.min(amt, outputRoom(res.toStack(1)));
            if (n > 0) {
                inv.addOutputs(List.of(res.toStack(n)));
                cargo.set(s, n == amt ? ItemResource.EMPTY : res, amt - n);
            }
        }
    }

    /** Move as much of the input buffer as fits into the gondola cargo. */
    void load(GondolaEntity g) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = inv.get(i);
            if (s.isEmpty()) {
                continue;
            }
            int n;
            try (Transaction tx = Transaction.openRoot()) {
                n = ResourceHandlerUtil.insertStacking(g.cargo(), ItemResource.of(s), s.getCount(), tx);
                tx.commit();
            }
            if (n > 0) {
                inv.shrink(i, n);
            }
        }
    }

    /** Take the parts of one gondola (4 steel plates, 2 iron chains) from the part slots; false when they are short. */
    boolean takeParts() {
        int plates = 0;
        int chains = 0;
        for (int s = 0; s < parts.size(); s++) {
            plates += parts.getResource(s).is(AutarkiaItems.STEEL_PLATE.get()) ? parts.getAmountAsInt(s) : 0;
            chains += parts.getResource(s).is(Items.IRON_CHAIN) ? parts.getAmountAsInt(s) : 0;
        }
        if (plates < 4 || chains < 2) {
            return false;
        }
        int needPlates = 4;
        int needChains = 2;
        for (int s = 0; s < parts.size(); s++) {
            ItemResource r = parts.getResource(s);
            int amt = parts.getAmountAsInt(s);
            int take = r.is(AutarkiaItems.STEEL_PLATE.get()) ? Math.min(amt, needPlates) : r.is(Items.IRON_CHAIN) ? Math.min(amt, needChains) : 0;
            if (r.is(AutarkiaItems.STEEL_PLATE.get())) {
                needPlates -= take;
            } else {
                needChains -= take;
            }
            if (take > 0) {
                parts.set(s, amt == take ? ItemResource.EMPTY : r, amt - take);
            }
        }
        return true;
    }

    /**
     * Take a surplus gondola off the loop: its cargo goes into the output buffer and its parts back into the part slots
     * (parts that do not fit drop at the station). Returns false, and keeps the gondola, while the cargo does not fit.
     */
    boolean retire(ServerLevel sl, GondolaEntity g) {
        unload(g);
        if (!g.emptyCargo()) {
            return false;
        }
        for (ItemStack p : GondolaEntity.parts()) {
            int n;
            try (Transaction tx = Transaction.openRoot()) {
                n = ResourceHandlerUtil.insertStacking(parts, ItemResource.of(p), p.getCount(), tx);
                tx.commit();
            }
            if (n < p.getCount()) {
                Containers.dropItemStack(sl, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, p.copyWithCount(p.getCount() - n));
            }
        }
        g.discard();
        return true;
    }

    /** Award an advancement to the players near the event and to the player who linked the line. */
    void award(ServerLevel sl, Vec3 at, String path) {
        linker.award(sl, at, path);
    }

    /** Remember the player who linked the line (advancement recipient). */
    public void setLinker(ServerPlayer p) {
        linker.set(p);
        setChanged();
    }

    /**
     * Link two stations into a line, or only check the link when simulate is true. The stations must be different,
     * unlinked, 16 to {@code cableCarMaxLength} blocks apart horizontally and at most 48 blocks apart in height, and both
     * cables and the gondola volume under them must be free ({@link CableGeometry#firstBlocked}).
     */
    public static Link link(ServerLevel sl, BlockPos first, BlockPos second, boolean simulate) {
        if (!(sl.getBlockEntity(first) instanceof CableCarStationBlockEntity fa) || !(sl.getBlockEntity(second) instanceof CableCarStationBlockEntity fb)) {
            return new Link(Result.NOT_STATION, null, 0);
        }
        CableCarStationBlockEntity a = fa.center();
        CableCarStationBlockEntity b = fb.center();
        if (a == b) {
            return new Link(Result.SAME, null, 0);
        }
        if (a.partner != null || b.partner != null) {
            return new Link(Result.LINKED, a.partner != null ? a.partner : b.partner, 0);
        }
        CableCarStationBlockEntity own = a.worldPosition.asLong() < b.worldPosition.asLong() ? a : b;
        CableCarStationBlockEntity other = own == a ? b : a;
        CableGeometry geo = new CableGeometry(CableGeometry.anchor(own.footprint()), CableGeometry.anchor(other.footprint()));
        if (geo.horizontal < MIN_LENGTH) {
            return new Link(Result.TOO_SHORT, null, geo.horizontal);
        }
        if (geo.horizontal > AutarkiaConfig.CABLE_CAR_MAX_LENGTH.get()) {
            return new Link(Result.TOO_LONG, null, geo.horizontal);
        }
        if (Math.abs(geo.b.y - geo.a.y) > MAX_HEIGHT) {
            return new Link(Result.TOO_STEEP, null, Math.abs(geo.b.y - geo.a.y));
        }
        BlockPos blocked = geo.firstBlocked(sl, st -> st.is(dev.autarkia.registry.AutarkiaBlocks.CABLE_CAR_STATION.get()));
        if (blocked != null) {
            return new Link(Result.BLOCKED, blocked, geo.horizontal);
        }
        if (!simulate) {
            for (CableCarStationBlockEntity s : List.of(own, other)) {
                s.partner = (s == own ? other : own).worldPosition;
                s.anchorA = geo.a;
                s.anchorB = geo.b;
                s.syncNow();
            }
            own.line = new CableLine(own, geo);
            own.line.register(sl);
            Autarkia.LOG.info("[cable car] linked {} and {}: {} blocks", own.worldPosition.toShortString(), other.worldPosition.toShortString(),
                String.format(java.util.Locale.ROOT, "%.1f", geo.horizontal));
        }
        return new Link(Result.OK, null, geo.horizontal);
    }

    private CableCarStationBlockEntity center() {
        return !isCenter() && level != null && level.getBlockEntity(getCenter()) instanceof CableCarStationBlockEntity c ? c : this;
    }

    @Override
    public InteractionResult wrench(ServerPlayer player) {
        CableCarStationBlockEntity c = center();
        if (c != this) {
            return c.wrench(player);
        }
        ItemStack stack = player.getMainHandItem().getItem() instanceof WrenchItem ? player.getMainHandItem() : player.getOffhandItem();
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        AutarkiaComponents.LinkStart start = stack.get(AutarkiaComponents.LINK_START.get());
        if (start != null && start.node().dimension().equals(sl.dimension()) && now - start.time() <= dev.autarkia.energy.CableItem.LINK_TIMEOUT
            && !start.node().pos().equals(worldPosition) && sl.getBlockEntity(start.node().pos()) instanceof CableCarStationBlockEntity) {
            stack.remove(AutarkiaComponents.LINK_START.get());
            Link l = link(sl, start.node().pos(), worldPosition, false);
            player.sendSystemMessage(message(l));
            if (l.result() == Result.OK) {
                setLinker(player);
                if (sl.getBlockEntity(start.node().pos()) instanceof CableCarStationBlockEntity first) {
                    first.setLinker(player);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        if (partner != null) {
            player.sendSystemMessage(Component.translatable("message.autarkia.cable_car.linked_already", partner.getX(), partner.getY(), partner.getZ()));
            return InteractionResult.FAIL;
        }
        stack.set(AutarkiaComponents.LINK_START.get(), new AutarkiaComponents.LinkStart(GlobalPos.of(sl.dimension(), worldPosition), Direction.UP, now));
        player.sendOverlayMessage(Component.translatable("message.autarkia.cable_car.first"));
        return InteractionResult.SUCCESS;
    }

    /** Return the chat message of a link result. */
    public static Component message(Link l) {
        String len = String.format(java.util.Locale.ROOT, "%.1f", l.length());
        return switch (l.result()) {
            case OK -> Component.translatable("message.autarkia.cable_car.linked", len);
            case SAME -> Component.translatable("message.autarkia.cable_car.same");
            case NOT_STATION -> Component.translatable("message.autarkia.cable_car.not_station");
            case LINKED -> Component.translatable("message.autarkia.cable_car.linked_already", l.blocked().getX(), l.blocked().getY(), l.blocked().getZ());
            case TOO_SHORT -> Component.translatable("message.autarkia.cable_car.too_short", len, AutarkiaConfig.CABLE_CAR_MAX_LENGTH.get());
            case TOO_LONG -> Component.translatable("message.autarkia.cable_car.too_long", len, AutarkiaConfig.CABLE_CAR_MAX_LENGTH.get());
            case TOO_STEEP -> Component.translatable("message.autarkia.cable_car.too_steep", len);
            case BLOCKED -> Component.translatable("message.autarkia.cable_car.blocked", l.blocked().getX(), l.blocked().getY(), l.blocked().getZ());
        };
    }

    /** Apply a menu button: 2 one gondola less, 3 one gondola more. */
    public boolean button(int id) {
        CableLine l = ownerLine();
        if (l == null || id < 2 || id > 3) {
            return false;
        }
        l.setTarget(l.target() + (id == 3 ? 1 : -1));
        return true;
    }

    private void clearLink() {
        if (line != null && level != null) {
            line.unregister(level);
        }
        partner = null;
        anchorA = null;
        anchorB = null;
        line = null;
        running = false;
        linker.clear();
        syncNow();
    }

    private void breakLine(ServerLevel sl) {
        if (partner == null) {
            return;
        }
        CableCarStationBlockEntity other = sl.isLoaded(partner) && sl.getBlockEntity(partner) instanceof CableCarStationBlockEntity o
            && worldPosition.equals(o.partner) ? o : null;
        CableLine l = line != null ? line : other == null ? null : other.line;
        if (l != null) {
            l.detachAll(sl);
        }
        Autarkia.LOG.info("[cable car] line {} - {} broke", worldPosition.toShortString(), partner.toShortString());
        if (other != null) {
            other.clearLink();
        }
        clearLink();
    }

    @Override
    public void serverTick() {
        if (partner == null || !isOwner() || line == null) {
            return;
        }
        ServerLevel sl = serverLevel();
        if (!sl.isLoaded(partner)) {
            line.pause(sl);
            return;
        }
        if (!(sl.getBlockEntity(partner) instanceof CableCarStationBlockEntity o) || !worldPosition.equals(o.partner)) {
            breakLine(sl);
            return;
        }
        line.tick(sl, o);
    }

    @Override
    public void setRemoved() {
        if (line != null && level != null) {
            line.unregister(level);
        }
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl && isCenter()) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
            for (int i = 0; i < parts.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, parts.getResource(i).toStack(parts.getAmountAsInt(i)));
            }
            breakLine(sl);
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
        return energy.room() > 0 ? 256 : 0;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int workDraw() {
        return running ? CableLine.FE_PER_TICK : 0;
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
        CableLine l = ownerLine();
        if (partner == null) {
            out.add(Component.translatable("jade.autarkia.cable_car.unlinked"));
            return;
        }
        if (l == null) {
            out.add(Component.translatable("jade.autarkia.cable_car.state", Component.translatable(CableLine.State.PAUSED.key())));
            return;
        }
        out.add(Component.translatable("jade.autarkia.cable_car.line", Math.round(l.geometry().chord), l.count()));
        BlockPos o = l.obstruction();
        out.add(l.state() == CableLine.State.OBSTRUCTED && o != null ? Component.translatable("jade.autarkia.cable_car.obstructed", o.getX(), o.getY(), o.getZ())
            : Component.translatable("jade.autarkia.cable_car.state", Component.translatable(l.state().key())));
        out.add(Component.translatable("jade.autarkia.cable_car.swing", Math.round(l.maxSwing())));
    }

    @Override
    public float jadeProgress() {
        CableLine l = ownerLine();
        return l == null ? -1 : (float) Math.min(1, l.maxSwing() / 60.0);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new CableCarStationMenu(id, playerInv, this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CableCarStationBlockEntity>("wheel", 4, test -> test.setAndContinue(test.animatable().running ? SPIN : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("inv"));
        parts.serialize(out.child("parts"));
        energy.serialize(out.child("energy"));
        linker.save(out);
        out.putBoolean("running", running);
        if (partner != null && anchorA != null && anchorB != null) {
            out.store("partner", BlockPos.CODEC, partner);
            out.store("anchorA", Vec3.CODEC, anchorA);
            out.store("anchorB", Vec3.CODEC, anchorB);
        }
        if (line != null) {
            line.save(out.child("line"));
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inv").ifPresent(inv::deserialize);
        in.child("parts").ifPresent(parts::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        linker.load(in);
        running = in.getBooleanOr("running", false);
        partner = in.read("partner", BlockPos.CODEC).orElse(null);
        anchorA = in.read("anchorA", Vec3.CODEC).orElse(null);
        anchorB = in.read("anchorB", Vec3.CODEC).orElse(null);
        CableGeometry geo = geometry();
        if (partner == null || geo == null || !isOwner()) {
            line = null;
            return;
        }
        if (line == null) {
            line = new CableLine(this, geo);
        } else {
            line.setGeometry(geo);
        }
        in.child("line").ifPresent(line::load);
    }
}
