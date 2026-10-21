package dev.autarkia.storage;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.logistics.MatterConduitBlockEntity;
import dev.autarkia.network.StockViewPayload;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * {@code domain_archive} (04 section 2.8, T6): one per colony domain (Core, beacon and outpost squares); the loaded
 * archive placed first is the primary, any other shows "Duplicate archive" and does nothing. The primary keeps a live
 * stock view of the {@link ArchiveSources} of its domain (cache 20 ticks), shows the free MU of the domain as one row
 * and the last 16 {@link MatterBank} withdrawals, and serves requests instantly: each moves 1 to 1,728 items from the
 * sources in one transaction into its 27-slot buffer or into the first RECEIVE {@code matter_conduit} of the chosen
 * channel, at most 4 requests per tick, 16 FE per item, idle 256 FE/t (FE buffer 256 x 200, input 256 x 4 x 16, M9 G7).
 * Sources and target flash REVERSE_PORTAL and {@code archive_deliver} plays. Outside every domain, as a duplicate or
 * without the idle FE it refuses requests. Players and other blocks only take items out of the buffer.
 */
public class DomainArchiveBlockEntity extends AutarkiaBlockEntity implements Ticking, JadeInfo, MenuProvider, GridMember {
    public static final int SLOTS = 27;
    public static final int MAX_REQUEST = 1728;
    public static final int REQUESTS_PER_TICK = 4;
    public static final int FE_PER_ITEM = 16;
    public static final int IDLE_FE = 256;
    public static final int CAPACITY = IDLE_FE * 200;
    public static final int MAX_IN = IDLE_FE * 4 * FE_PER_ITEM;
    public static final int TO_BUFFER = -1;
    public static final int QUEUE = 64;
    private static final Map<Level, Set<BlockPos>> LIVE = new WeakHashMap<>();

    /** Archive state. */
    public enum Status { ACTIVE, DUPLICATE, OUTSIDE, NO_POWER }

    private record Request(@Nullable ServerPlayer player, ItemStack item, int amount, int target) {
    }

    private final Buffer buffer = new Buffer();
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IN, 0, this::setChanged);
    private final ResourceHandler<ItemResource> automation = new Automation();
    private final ArrayDeque<Request> pending = new ArrayDeque<>();
    private ArchiveSources.Scan scan = ArchiveSources.Scan.EMPTY;
    private long scannedAt = Long.MIN_VALUE / 2;
    private long created;
    private Status status = Status.OUTSIDE;
    private int lastFe;
    private int lastMoved;
    private boolean delivering;
    private long viewFree;
    private List<MatterBank.Withdrawal> viewLog = List.of();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 2 -> energy.stored();
                case 3 -> energy.capacity();
                case 4 -> status.ordinal();
                case 5 -> scan.sources().size();
                case 6 -> pending.size();
                case 7 -> lastFe;
                default -> 0;
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

    public DomainArchiveBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.DOMAIN_ARCHIVE.get(), pos, state);
    }

    /** Return the 27-slot buffer (live). */
    public ItemStacksResourceHandler buffer() {
        return buffer;
    }

    /** Return the extract-only handler other blocks see. */
    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    /** Return the FE buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the status of the last tick. */
    public Status status() {
        return status;
    }

    /** Return the number of sources of the cached scan. */
    public int sources() {
        return scan.sources().size();
    }

    /** Return the number of queued requests. */
    public int pendingRequests() {
        return pending.size();
    }

    /** Return the FE used in the last tick (idle plus deliveries). */
    public int lastFe() {
        return lastFe;
    }

    /** Return the items delivered in the last tick. */
    public int lastMoved() {
        return lastMoved;
    }

    /** Return the free MU of the domain as last synced (client and server). */
    public long viewFree() {
        return viewFree;
    }

    /** Return the last withdrawals of the domain as last synced, oldest first. */
    public List<MatterBank.Withdrawal> viewLog() {
        return viewLog;
    }

    /** Return the cached stock per item as {available, total}. */
    public Map<ItemResource, long[]> stock() {
        return scan.stock();
    }

    /** Return the stock view sent to an open archive screen. */
    public StockViewPayload stockView() {
        List<StockViewPayload.Stock> out = new ArrayList<>();
        scan.stock().forEach((k, v) -> out.add(new StockViewPayload.Stock(k.toStack(1), v[0], v[1])));
        return new StockViewPayload(worldPosition, out);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl) {
            if (created == 0) {
                created = Math.max(1, sl.getGameTime());
            }
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

    private boolean primary(ServerLevel sl, Colony c) {
        for (BlockPos p : List.copyOf(LIVE.getOrDefault(sl, Set.of()))) {
            if (p.equals(worldPosition) || !sl.isLoaded(p) || !(sl.getBlockEntity(p) instanceof DomainArchiveBlockEntity o) || o.isRemoved()
                || Colonies.at(sl, p) != c) {
                continue;
            }
            if (o.created < created || o.created == created && p.asLong() < worldPosition.asLong()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Queue a request of amount (clamped to 1 to 1,728 when it runs) of the item into the buffer ({@link #TO_BUFFER}) or
     * to the RECEIVE conduit of channel target; return the refusal message key, or null when queued.
     */
    public @Nullable String submit(@Nullable ServerPlayer player, ItemStack item, int amount, int target) {
        if (status != Status.ACTIVE) {
            return "message.autarkia.domain_archive." + status.name().toLowerCase(Locale.ROOT);
        }
        if (item.isEmpty() || target < TO_BUFFER || target > 15) {
            return "message.autarkia.domain_archive.none_available";
        }
        if (pending.size() >= QUEUE) {
            return "message.autarkia.domain_archive.queue_full";
        }
        pending.addLast(new Request(player, item.copyWithCount(1), amount, target));
        return null;
    }

    /** Submit a player's request and tell the player when it is refused. */
    public void request(ServerPlayer player, ItemStack item, int amount, int target) {
        String refusal = submit(player, item, amount, target);
        if (refusal != null) {
            player.sendSystemMessage(Component.translatable(refusal));
        }
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        lastFe = 0;
        lastMoved = 0;
        Colony c = Colonies.server() == null ? null : Colonies.at(sl, worldPosition);
        Status next;
        if (c == null) {
            next = Status.OUTSIDE;
        } else if (!primary(sl, c)) {
            next = Status.DUPLICATE;
        } else if (energy.stored() < IDLE_FE) {
            next = Status.NO_POWER;
        } else {
            energy.drain(IDLE_FE);
            lastFe = IDLE_FE;
            next = Status.ACTIVE;
        }
        if (next != status) {
            status = next;
            syncNow();
        }
        if (status != Status.ACTIVE) {
            refuseAll("message.autarkia.domain_archive." + status.name().toLowerCase(Locale.ROOT));
            scan = ArchiveSources.Scan.EMPTY;
        } else {
            if (now - scannedAt >= 20) {
                scan = ArchiveSources.scan(sl, c);
                scannedAt = now;
            }
            for (int i = 0; i < REQUESTS_PER_TICK && !pending.isEmpty(); i++) {
                serve(sl, c, pending.pollFirst());
            }
        }
        long free = c == null ? 0 : MatterBank.available(sl, c);
        List<MatterBank.Withdrawal> log = c == null ? List.of() : MatterBank.log(c);
        if (free != viewFree || !log.equals(viewLog)) {
            viewFree = free;
            viewLog = log;
            syncThrottled(20);
        }
        flushSync(20);
    }

    private void refuseAll(String key) {
        while (!pending.isEmpty()) {
            Request r = pending.pollFirst();
            if (r.player() != null) {
                r.player().sendSystemMessage(Component.translatable(key));
            }
        }
    }

    private void serve(ServerLevel sl, Colony c, Request r) {
        int amount = Math.min(Math.min(MAX_REQUEST, Math.max(1, r.amount())), energy.stored() / FE_PER_ITEM);
        if (amount <= 0) {
            tell(r, Component.translatable("message.autarkia.domain_archive.no_power"));
            return;
        }
        ResourceHandler<ItemResource> target;
        MatterConduitBlockEntity conduit = null;
        BlockPos at = worldPosition;
        BlockPos skip = worldPosition;
        if (r.target() == TO_BUFFER) {
            target = buffer;
        } else {
            List<MatterConduitBlockEntity> rx = MatterConduitBlockEntity.receivers(sl, c, r.target());
            if (rx.isEmpty()) {
                tell(r, Component.translatable("message.autarkia.domain_archive.no_conduit", MatterConduitBlockEntity.channelName(r.target())));
                return;
            }
            conduit = rx.getFirst();
            target = conduit.acceptor();
            at = conduit.getBlockPos();
            skip = at.relative(conduit.outputFace());
        }
        List<BlockPos> from = new ArrayList<>();
        int moved;
        delivering = true;
        try (Transaction tx = Transaction.openRoot()) {
            moved = ArchiveSources.deliver(sl, scan.sources(), ItemResource.of(r.item()), amount, target, skip, from, tx);
            tx.commit();
        } finally {
            delivering = false;
        }
        scannedAt = Long.MIN_VALUE / 2;
        if (moved <= 0) {
            tell(r, Component.translatable("message.autarkia.domain_archive.none_available"));
            return;
        }
        energy.drain(moved * FE_PER_ITEM);
        lastFe += moved * FE_PER_ITEM;
        lastMoved += moved;
        if (conduit != null) {
            conduit.received(moved);
        }
        for (BlockPos p : from) {
            burst(sl, p);
        }
        burst(sl, at);
        sl.playSound(null, worldPosition, AutarkiaSounds.of("archive_deliver"), SoundSource.BLOCKS, 0.6f, 1.6f);
        if (conduit != null) {
            sl.playSound(null, at, AutarkiaSounds.of("archive_deliver"), SoundSource.BLOCKS, 0.6f, 1.6f);
        }
        tell(r, Component.translatable("message.autarkia.domain_archive.delivered", moved, r.item().getHoverName()));
        setChanged();
    }

    private static void tell(Request r, Component msg) {
        if (r.player() != null) {
            r.player().sendSystemMessage(msg);
        }
    }

    private static void burst(ServerLevel sl, BlockPos p) {
        Vec3 c = Vec3.atCenterOf(p);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 24, 0.4, 0.4, 0.4, 0.06);
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        if (status != Status.ACTIVE) {
            out.add(Component.translatable("jade.autarkia.domain_archive.status." + status.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.RED));
        }
        out.add(Component.translatable("jade.autarkia.domain_archive.sources", scan.sources().size(), lastFe));
        out.add(Component.translatable("jade.autarkia.energy", energy.stored(), energy.capacity()));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.DOMAIN_ARCHIVE.get(), id, playerInv, worldPosition, buffer, data, GuiLayouts.DOMAIN_ARCHIVE);
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
        return Math.max(IDLE_FE, lastFe);
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
        out.putLong("created", created);
        out.putString("status", status.name());
        out.putLong("view_free", viewFree);
        out.store("view_log", MatterBank.Withdrawal.CODEC.listOf(), viewLog);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(buffer::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        created = in.getLongOr("created", 0);
        String s = in.getStringOr("status", Status.OUTSIDE.name());
        status = java.util.Arrays.stream(Status.values()).filter(v -> v.name().equals(s)).findFirst().orElse(Status.OUTSIDE);
        viewFree = in.getLongOr("view_free", 0);
        viewLog = in.read("view_log", MatterBank.Withdrawal.CODEC.listOf()).orElse(List.of());
    }

    private final class Buffer extends ItemStacksResourceHandler {
        Buffer() {
            super(SLOTS);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return delivering && !resource.isEmpty();
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
            return false;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            return buffer.extract(index, resource, amount, tx);
        }
    }
}
