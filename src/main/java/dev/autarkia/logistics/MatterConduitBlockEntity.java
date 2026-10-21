package dev.autarkia.logistics;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.MenuButtons;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import dev.autarkia.storage.CrateBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * {@code matter_conduit} (04 section 1.8, T6): mode SEND or RECEIVE, channel 0 to 15 (dye colours), output face =
 * block property {@code facing}, a 9-slot buffer that refuses filled crates (04 C6) and an FE buffer of 512 x 200 with
 * input 512 x 4 FE/t (M9 G7). Each tick a SEND conduit inside a colony domain moves up to {@code conduitItemsPerTick}
 * items, 8 FE each, to the RECEIVE conduits of the same channel and the same colony, round robin per stack, in one
 * transaction (04 C5): an item goes into the handler on the receiver's output face, else into the receiver's buffer.
 * Receivers are found through a per-tick index of the loaded conduits by colony and channel, so receivers in unloaded
 * chunks are skipped. A RECEIVE conduit pushes its own buffer into its output face (no FE). Without FE for an item it
 * stays in the buffer; outside every domain the conduit is inactive ("Outside domain"). Both ends flash END_ROD, the
 * sender plays {@code conduit_send} at most every 10 ticks. When a SEND conduit has moved 1,000 items it grants
 * {@code logistics/everywhere} to the players within 32 blocks, the last player who opened or set it, and the online
 * colony owner. The item capability inserts into the buffer of a SEND conduit and extracts from a RECEIVE conduit.
 * Menu buttons: 0 toggles the mode, 1 + c sets channel c, 17 turns the output face.
 */
public class MatterConduitBlockEntity extends AutarkiaBlockEntity implements Ticking, JadeInfo, MenuProvider, GridMember, MenuButtons {
    public static final int SLOTS = 9;
    public static final int FE_PER_ITEM = 8;
    public static final int RATED = 512;
    public static final int CAPACITY = RATED * 200;
    public static final int MAX_IN = RATED * 4;
    public static final long EVERYWHERE = 1000;
    public static final int MODE_BUTTON = 0;
    public static final int CHANNEL_BUTTON = 1;
    public static final int FACE_BUTTON = 17;
    private static final Map<Level, Set<BlockPos>> LIVE = new WeakHashMap<>();
    private static final Map<Level, Index> INDEX = new WeakHashMap<>();

    /** Transfer direction. */
    public enum Mode { SEND, RECEIVE }

    private record Key(UUID colony, int channel) {
    }

    private record Index(long tick, Map<Key, List<BlockPos>> receivers) {
    }

    private final Buffer buffer = new Buffer();
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IN, 0, this::setChanged);
    private final ResourceHandler<ItemResource> automation = new Automation();
    private final ResourceHandler<ItemResource> acceptor = new Acceptor();
    private final LinkerRef tender = new LinkerRef();
    private final int[] window = new int[20];
    private Mode mode = Mode.SEND;
    private int channel;
    private int rr;
    private long moved;
    private int lastDraw;
    private int lastMoved;
    private boolean outside;
    private long lastSound = Long.MIN_VALUE / 2;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 2 -> energy.stored();
                case 3 -> energy.capacity();
                case 5 -> mode.ordinal();
                case 6 -> channel;
                case 7 -> outputFace().ordinal();
                case 8 -> itemsPerSecond();
                case 9 -> outside ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public MatterConduitBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.MATTER_CONDUIT.get(), pos, state);
    }

    /** Return the 9-slot buffer (live). */
    public ItemStacksResourceHandler buffer() {
        return buffer;
    }

    /** Return the FE buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the handler other blocks see: insert into a SEND buffer, extract from a RECEIVE buffer. */
    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    /** Return the mode. */
    public Mode mode() {
        return mode;
    }

    /** Return the channel 0 to 15. */
    public int channel() {
        return channel;
    }

    /** Return the face a RECEIVE conduit pushes into. */
    public Direction outputFace() {
        return MachineBlock.front(getBlockState());
    }

    /** Return the items this conduit sent (SEND) or received (RECEIVE) since placement. */
    public long moved() {
        return moved;
    }

    /** Return the items it sent or received in the last tick. */
    public int lastMoved() {
        return lastMoved;
    }

    /** Return the items sent or received in the last 20 ticks. */
    public int itemsPerSecond() {
        int n = 0;
        for (int v : window) {
            n += v;
        }
        return n;
    }

    /** Return true when the conduit stood outside every domain at its last tick. */
    public boolean outside() {
        return outside;
    }

    /** Set the mode. */
    public void setMode(Mode m) {
        mode = m;
        syncNow();
    }

    /** Set the channel, clamped to 0 to 15. */
    public void setChannel(int c) {
        channel = Math.clamp(c, 0, 15);
        syncNow();
    }

    /** Remember the player who opened or set this conduit (receives {@code logistics/everywhere}). */
    public void setTender(ServerPlayer p) {
        tender.set(p);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl) {
            LIVE.computeIfAbsent(sl, k -> new HashSet<>()).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            Set<BlockPos> s = LIVE.get(sl);
            if (s != null) {
                s.remove(worldPosition);
            }
        }
    }

    /** Return the loaded RECEIVE conduits of the colony on the channel in position order (index built once per tick). */
    public static List<MatterConduitBlockEntity> receivers(ServerLevel level, Colony c, int channel) {
        long now = level.getGameTime();
        Index idx = INDEX.get(level);
        if (idx == null || idx.tick() != now) {
            idx = build(level, now);
            INDEX.put(level, idx);
        }
        List<MatterConduitBlockEntity> out = new ArrayList<>();
        for (BlockPos p : idx.receivers().getOrDefault(new Key(c.id, channel), List.of())) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof MatterConduitBlockEntity m && !m.isRemoved() && m.mode == Mode.RECEIVE
                && m.channel == channel) {
                out.add(m);
            }
        }
        return out;
    }

    private static Index build(ServerLevel level, long now) {
        Map<Key, List<BlockPos>> map = new HashMap<>();
        for (BlockPos p : List.copyOf(LIVE.getOrDefault(level, Set.of()))) {
            if (!level.isLoaded(p) || !(level.getBlockEntity(p) instanceof MatterConduitBlockEntity m) || m.mode != Mode.RECEIVE) {
                continue;
            }
            Colony c = Colonies.at(level, p);
            if (c != null) {
                map.computeIfAbsent(new Key(c.id, m.channel), k -> new ArrayList<>()).add(p.immutable());
            }
        }
        map.values().forEach(l -> l.sort(Comparator.comparingLong(BlockPos::asLong)));
        return new Index(now, map);
    }

    /** Return the handler that takes items delivered to this receiver: its output face first, then its buffer. */
    public ResourceHandler<ItemResource> acceptor() {
        return acceptor;
    }

    private @Nullable ResourceHandler<ItemResource> target() {
        if (!(level instanceof ServerLevel sl)) {
            return null;
        }
        Direction face = outputFace();
        return sl.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(face), face.getOpposite());
    }

    /** Count items another block delivered to this receiver after their transaction committed (Jade rate, flash). */
    public void received(int n) {
        if (n <= 0 || !(level instanceof ServerLevel sl)) {
            return;
        }
        moved += n;
        lastMoved += n;
        window[(int) (sl.getGameTime() % 20)] += n;
        flash(sl);
        setChanged();
    }

    private void flash(ServerLevel sl) {
        Vec3 c = Vec3.atCenterOf(worldPosition);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 4, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        window[(int) (now % 20)] = 0;
        lastDraw = 0;
        lastMoved = 0;
        Colony c = Colonies.server() == null ? null : Colonies.at(sl, worldPosition);
        boolean out = c == null;
        if (out != outside) {
            outside = out;
            syncNow();
        }
        if (c == null) {
            return;
        }
        if (mode == Mode.SEND) {
            send(sl, c, now);
        } else {
            push(sl, now);
        }
    }

    private void send(ServerLevel sl, Colony c, long now) {
        int budget = Math.min(AutarkiaConfig.CONDUIT_ITEMS_PER_TICK.get(), energy.stored() / FE_PER_ITEM);
        if (budget <= 0 || ResourceHandlerUtil.isEmpty(buffer)) {
            return;
        }
        List<MatterConduitBlockEntity> rx = receivers(sl, c, channel);
        rx.remove(this);
        if (rx.isEmpty()) {
            return;
        }
        int sent = 0;
        Map<MatterConduitBlockEntity, Integer> got = new IdentityHashMap<>();
        try (Transaction tx = Transaction.openRoot()) {
            for (int s = 0; s < SLOTS && sent < budget; s++) {
                ItemResource res = buffer.getResource(s);
                int want = Math.min(buffer.getAmountAsInt(s), budget - sent);
                if (res.isEmpty() || want <= 0) {
                    continue;
                }
                for (int k = 0; k < rx.size(); k++) {
                    MatterConduitBlockEntity r = rx.get((rr + k) % rx.size());
                    int n = moveTo(r.acceptor, s, res, want, tx);
                    if (n > 0) {
                        sent += n;
                        got.merge(r, n, Integer::sum);
                        rr = (rr + k + 1) % rx.size();
                        break;
                    }
                }
            }
            tx.commit();
        }
        if (sent <= 0) {
            return;
        }
        energy.drain(sent * FE_PER_ITEM);
        lastDraw = sent * FE_PER_ITEM;
        long before = moved;
        moved += sent;
        lastMoved = sent;
        window[(int) (now % 20)] += sent;
        got.forEach(MatterConduitBlockEntity::received);
        flash(sl);
        if (now - lastSound >= 10) {
            lastSound = now;
            sl.playSound(null, worldPosition, AutarkiaSounds.of("conduit_send"), SoundSource.BLOCKS, 0.5f, 1.8f);
        }
        if (before < EVERYWHERE && moved >= EVERYWHERE) {
            everywhere(sl, c);
        }
        setChanged();
    }

    private int moveTo(ResourceHandler<ItemResource> to, int slot, ItemResource res, int want, TransactionContext tx) {
        try (Transaction sub = Transaction.open(tx)) {
            int n = to.insert(res, want, sub);
            if (n <= 0 || buffer.extract(slot, res, n, sub) != n) {
                return 0;
            }
            sub.commit();
            return n;
        }
    }

    private void push(ServerLevel sl, long now) {
        ResourceHandler<ItemResource> to = target();
        if (to == null || ResourceHandlerUtil.isEmpty(buffer)) {
            return;
        }
        int budget = AutarkiaConfig.CONDUIT_ITEMS_PER_TICK.get();
        int pushed = 0;
        try (Transaction tx = Transaction.openRoot()) {
            for (int s = 0; s < SLOTS && pushed < budget; s++) {
                ItemResource res = buffer.getResource(s);
                int want = Math.min(buffer.getAmountAsInt(s), budget - pushed);
                if (!res.isEmpty() && want > 0) {
                    pushed += moveTo(to, s, res, want, tx);
                }
            }
            tx.commit();
        }
        if (pushed > 0) {
            setChanged();
        }
    }

    private void everywhere(ServerLevel sl, Colony c) {
        tender.award(sl, Vec3.atCenterOf(worldPosition), "logistics/everywhere");
        ServerPlayer owner = c.owner == null ? null : sl.getServer().getPlayerList().getPlayer(c.owner);
        if (owner != null) {
            AutarkiaAdvancements.award(owner, "logistics/everywhere");
        }
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (player instanceof ServerPlayer sp) {
            tender.set(sp);
        }
        if (id == MODE_BUTTON) {
            setMode(mode == Mode.SEND ? Mode.RECEIVE : Mode.SEND);
            return true;
        }
        if (id >= CHANNEL_BUTTON && id < CHANNEL_BUTTON + 16) {
            setChannel(id - CHANNEL_BUTTON);
            return true;
        }
        if (id == FACE_BUTTON) {
            Direction next = Direction.values()[(outputFace().ordinal() + 1) % Direction.values().length];
            level.setBlock(worldPosition, getBlockState().setValue(MachineBlock.FACING, next), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** Return the dye colour name component of a channel. */
    public static Component channelName(int channel) {
        return Component.translatable("color.minecraft." + DyeColor.byId(channel).getName());
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        if (outside) {
            out.add(Component.translatable("jade.autarkia.matter_conduit.outside").withStyle(ChatFormatting.RED));
        }
        out.add(Component.translatable("jade.autarkia.matter_conduit.mode", Component.translatable("jade.autarkia.matter_conduit.mode."
            + mode.name().toLowerCase(java.util.Locale.ROOT))));
        out.add(Component.translatable("jade.autarkia.matter_conduit.channel", channel, channelName(channel)));
        out.add(Component.translatable("jade.autarkia.matter_conduit.rate", itemsPerSecond()));
        out.add(Component.translatable("jade.autarkia.energy", energy.stored(), energy.capacity()));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        if (player instanceof ServerPlayer sp) {
            tender.set(sp);
        }
        return new SlotMenu(AutarkiaMenus.MATTER_CONDUIT.get(), id, playerInv, worldPosition, buffer, data, GuiLayouts.MATTER_CONDUIT);
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
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return MAX_IN;
    }

    @Override
    public int workDraw() {
        return lastDraw;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel) {
            for (int i = 0; i < SLOTS; i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, buffer.getResource(i).toStack(buffer.getAmountAsInt(i)));
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("items"));
        energy.serialize(out.child("energy"));
        out.putString("mode", mode.name());
        out.putInt("channel", channel);
        out.putInt("rr", rr);
        out.putLong("moved", moved);
        out.putBoolean("outside", outside);
        tender.save(out);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(buffer::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        mode = "RECEIVE".equals(in.getStringOr("mode", "SEND")) ? Mode.RECEIVE : Mode.SEND;
        channel = Math.clamp(in.getIntOr("channel", 0), 0, 15);
        rr = in.getIntOr("rr", 0);
        moved = in.getLongOr("moved", 0);
        outside = in.getBooleanOr("outside", false);
        tender.load(in);
    }

    private final class Buffer extends ItemStacksResourceHandler {
        Buffer() {
            super(SLOTS);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && !CrateBlockEntity.isFilledCrate(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    }

    private final class Automation implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return SLOTS;
        }

        @Override
        public ItemResource getResource(int index) {
            return buffer.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return buffer.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return buffer.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return mode == Mode.SEND && buffer.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            return mode == Mode.SEND ? buffer.insert(index, resource, amount, tx) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            return mode == Mode.RECEIVE ? buffer.extract(index, resource, amount, tx) : 0;
        }
    }

    private final class Acceptor implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return buffer.isValid(0, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            return insert(resource, amount, tx);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext tx) {
            if (amount <= 0 || !isValid(0, resource)) {
                return 0;
            }
            ResourceHandler<ItemResource> to = target();
            int n = to == null ? 0 : to.insert(resource, amount, tx);
            if (n < amount) {
                n += buffer.insert(resource, amount - n, tx);
            }
            return n;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            return 0;
        }
    }
}
