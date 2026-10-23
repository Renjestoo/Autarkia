package dev.autarkia.logistics;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.TransitDeltaPayload;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * One belt block (04 section 1.3). Each belt keeps the entries on its own 1,000 pu span (spacing 500 pu, 1 to 8 items
 * per entry); the head of a line (the belt that no belt feeds; in a loop the lowest position) ticks the whole line from
 * its end to its head, so an entry moves once per tick: speed {@code beltSpeedPu} while the head's 1,000 FE buffer pays
 * {@code beltFePerBlock} per belt per tick, else 0. The end belt outputs into the block in front (one up for an
 * {@code up} belt): a belt (side-load at its middle), chute, splitter, sorting chute or item handler. A refused output
 * waits; after {@code jamWarningTicks} the line is JAMMED (red stripe, scrape particles, sound) until one output works.
 * Inputs: the back face at 0 pu, the top and side faces at 500 pu.
 */
public class BeltBlockEntity extends AutarkiaBlockEntity implements Ticking, TransitOwner, GridMember, JadeInfo, dev.autarkia.colony.ColonySensors.Member {
    @Override
    public Set<dev.autarkia.colony.ColonySensors.Kind> colonyKinds() {
        return Set.of(dev.autarkia.colony.ColonySensors.Kind.TRANSPORT);
    }

    public static final int LENGTH = 1000;
    public static final int SPACING = 500;
    public static final int MAX_ITEMS = 8;
    public static final int MIDDLE = 500;
    private static final Map<Level, int[]> EPOCH = new WeakHashMap<>();
    private final TransitSegment segment = new TransitSegment(LENGTH);
    private final EnergyBuffer buffer = new EnergyBuffer(1000, 128, 0, this::setChanged);
    private final Intake back = new Intake(0);
    private final Intake middle = new Intake(MIDDLE);
    private final List<TransitDeltaPayload.Op> ops = new ArrayList<>();
    private int cacheEpoch = -1;
    private BlockPos head;
    private List<BlockPos> line = List.of();
    private boolean loop;
    private boolean powered;
    private int blockedTicks;
    private boolean jammed;
    private int delivered;
    private long lastSnapshot;
    private boolean changedSinceSnapshot;

    /** One input point of the belt: a one-slot handler of up to 8 items that the line moves onto the belt. */
    private final class Intake extends ItemStacksResourceHandler {
        private final int at;

        Intake(int at) {
            super(1);
            this.at = at;
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return MAX_ITEMS;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return segment.canInsertAt(at, SPACING) && !dev.autarkia.storage.CrateBlockEntity.isFilledCrate(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }

        void flush() {
            if (getAmountAsInt(0) > 0 && accept(getResource(0).toStack(getAmountAsInt(0)), at)) {
                set(0, ItemResource.EMPTY, 0);
            }
        }

        ItemStack held() {
            return getResource(0).toStack(getAmountAsInt(0));
        }
    }

    public BeltBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.BELT.get(), pos, state);
        head = pos;
    }

    /** Invalidate every cached belt line of the level. */
    public static void bumpEpoch(Level level) {
        EPOCH.computeIfAbsent(level, l -> new int[1])[0]++;
    }

    private static int epoch(Level level) {
        return EPOCH.computeIfAbsent(level, l -> new int[1])[0];
    }

    private Direction facing() {
        return MachineBlock.front(getBlockState());
    }

    /** Return the next belt of the line after the belt at pos, or null. */
    public static @Nullable BlockPos next(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof BeltBlock)) {
            return null;
        }
        Direction f = st.getValue(MachineBlock.HORIZONTAL_FACING);
        if (st.getValue(BeltBlock.SLOPE) == BeltBlock.Slope.UP) {
            BlockPos p = pos.relative(f).above();
            return BeltBlock.beltFacing(level.getBlockState(p), f) ? p : null;
        }
        BlockPos p = pos.relative(f);
        BlockState n = level.getBlockState(p);
        if (BeltBlock.beltFacing(n, f)) {
            return p;
        }
        BlockState below = level.getBlockState(p.below());
        return BeltBlock.beltFacing(below, f) && below.getValue(BeltBlock.SLOPE) == BeltBlock.Slope.DOWN ? p.below() : null;
    }

    /** Return the belt that feeds the belt at pos along its line, or null. */
    public static @Nullable BlockPos prev(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof BeltBlock)) {
            return null;
        }
        BlockPos back = pos.relative(st.getValue(MachineBlock.HORIZONTAL_FACING).getOpposite());
        for (BlockPos c : List.of(back, back.below(), back.above())) {
            if (pos.equals(next(level, c))) {
                return c;
            }
        }
        return null;
    }

    private void refreshCache() {
        int e = epoch(level);
        if (cacheEpoch == e) {
            return;
        }
        cacheEpoch = e;
        int max = AutarkiaConfig.BELT_MAX_LINE_LENGTH.get();
        BlockPos cur = worldPosition;
        Set<BlockPos> seen = new HashSet<>();
        loop = false;
        for (int i = 0; i < max; i++) {
            seen.add(cur);
            BlockPos p = prev(level, cur);
            if (p == null) {
                break;
            }
            if (seen.contains(p)) {
                loop = true;
                cur = seen.stream().min(java.util.Comparator.comparingLong(BlockPos::asLong)).orElse(cur);
                break;
            }
            cur = p;
        }
        head = cur.immutable();
        if (head.equals(worldPosition)) {
            List<BlockPos> list = new ArrayList<>();
            BlockPos p = worldPosition;
            Set<BlockPos> inLine = new HashSet<>();
            while (p != null && list.size() < max && inLine.add(p)) {
                list.add(p);
                p = next(level, p);
            }
            loop = p != null && p.equals(worldPosition);
            line = List.copyOf(list);
        } else {
            line = List.of();
        }
    }

    /** Return the head position of this belt's line. */
    public BlockPos headPos() {
        refreshCache();
        return head;
    }

    /** Return the head belt of the line (this belt when it is the head or the head is missing). */
    public BeltBlockEntity headBelt() {
        return level != null && level.getBlockEntity(headPos()) instanceof BeltBlockEntity h ? h : this;
    }

    /** Return the line from head to end (only on the head; empty elsewhere). */
    public List<BlockPos> line() {
        refreshCache();
        return line;
    }

    /** Return the line's shared energy buffer (in the head). */
    public EnergyBuffer lineBuffer() {
        return headBelt().buffer;
    }

    /** Return the input handler of a face: back 0 pu, top and sides 500 pu, front none. */
    public @Nullable ResourceHandler<ItemResource> intake(@Nullable Direction side) {
        if (side == facing()) {
            return null;
        }
        return side == facing().getOpposite() ? back : middle;
    }

    @Override
    public TransitSegment segment() {
        return segment;
    }

    /** Return true while the line is jammed. */
    public boolean jammed() {
        return jammed;
    }

    /** Return true while the line is powered (client: from the update tag). */
    public boolean powered() {
        return powered;
    }

    /** Return how long the end of the line has waited (ticks). */
    public int blockedTicks() {
        return blockedTicks;
    }

    /** Put an entry onto the belt at a position when there is room; returns false otherwise. */
    public boolean accept(ItemStack stack, int at) {
        if (stack.isEmpty() || stack.getCount() > MAX_ITEMS || !segment.canInsertAt(at, SPACING) || level == null
            || dev.autarkia.storage.CrateBlockEntity.isFilledCrate(stack)) {
            return false;
        }
        TransitSegment.Entry e = segment.insert(stack.copy(), at);
        ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.ADD, e.id, e.stack, at));
        changedSinceSnapshot = true;
        setChanged();
        return true;
    }

    private void setPowered(boolean on) {
        if (powered != on) {
            powered = on;
            syncNow();
        }
    }

    @Override
    public void serverTick() {
        if (!headPos().equals(worldPosition)) {
            return;
        }
        ServerLevel sl = serverLevel();
        List<BeltBlockEntity> belts = new ArrayList<>();
        for (BlockPos p : line()) {
            if (!sl.isLoaded(p) || !(sl.getBlockEntity(p) instanceof BeltBlockEntity b)) {
                return;
            }
            belts.add(b);
        }
        int cost = belts.size() * AutarkiaConfig.BELT_FE_PER_BLOCK.get();
        boolean on = buffer.stored() >= cost;
        if (on) {
            buffer.drain(cost);
        }
        for (BeltBlockEntity b : belts) {
            b.setPowered(on);
        }
        if (on) {
            int speed = AutarkiaConfig.BELT_SPEED_PU.get();
            for (int i = belts.size() - 1; i >= 0; i--) {
                BeltBlockEntity next = i + 1 < belts.size() ? belts.get(i + 1) : loop ? belts.getFirst() : null;
                belts.get(i).step(sl, next, speed, belts.size());
            }
        }
        for (BeltBlockEntity b : belts) {
            b.flushOps(sl);
        }
    }

    private void step(ServerLevel sl, @Nullable BeltBlockEntity next, int speed, int lineSize) {
        back.flush();
        middle.flush();
        segment.move(speed, SPACING, frontLimit(next));
        TransitSegment.Entry front = segment.atEnd();
        if (front == null) {
            return;
        }
        boolean ok = next != null ? next.accept(front.stack, 0) : output(sl, front);
        if (ok) {
            if (next == null) {
                delivered += front.stack.getCount();
                if (lineSize >= 32 && delivered >= 64) {
                    AutarkiaAdvancements.awardNear(sl, Vec3.atCenterOf(worldPosition), "logistics/belt_line");
                }
            }
            segment.remove(front.id);
            ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.REMOVE, front.id, ItemStack.EMPTY, 0));
            changedSinceSnapshot = true;
            if (next == null) {
                blockedTicks = 0;
                setJammed(false);
                if (segment.blocked) {
                    segment.blocked = false;
                    ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.UNBLOCK, 0, ItemStack.EMPTY, 0));
                }
            }
        } else if (next == null) {
            blockedTicks++;
            if (!segment.blocked) {
                segment.blocked = true;
                ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.BLOCK, 0, ItemStack.EMPTY, 0));
            }
            if (blockedTicks >= AutarkiaConfig.JAM_WARNING_TICKS.get()) {
                setJammed(true);
                if (blockedTicks % 40 == 0) {
                    sl.playSound(null, worldPosition, AutarkiaSounds.of("belt_jam"), SoundSource.BLOCKS, 0.5f, 0.6f);
                }
                if (blockedTicks % 10 == 0) {
                    sl.sendParticles(ParticleTypes.SCRAPE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.4, worldPosition.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.05);
                }
            }
        }
    }

    /** Return how far the front entry may move: the end, or less while the next belt's last entry is within spacing of its start. */
    private static int frontLimit(@Nullable BeltBlockEntity next) {
        if (next == null || next.segment.entries.isEmpty()) {
            return LENGTH;
        }
        return Math.min(LENGTH, LENGTH + next.segment.entries.getLast().pos - SPACING);
    }

    private void setJammed(boolean on) {
        if (jammed != on) {
            jammed = on;
            syncNow();
        }
    }

    /** Return the block the end belt outputs into. */
    public BlockPos outputPos() {
        BlockPos f = worldPosition.relative(facing());
        return getBlockState().getValue(BeltBlock.SLOPE) == BeltBlock.Slope.UP ? f.above() : f;
    }

    private boolean output(ServerLevel sl, TransitSegment.Entry front) {
        BlockPos t = outputPos();
        ItemStack stack = front.stack;
        if (sl.getBlockEntity(t) instanceof ChuteBlockEntity chute) {
            int n = Math.min(ChuteBlockEntity.MAX_ITEMS, stack.getCount());
            if (!chute.accept(stack.copyWithCount(n))) {
                return false;
            }
            if (n < stack.getCount()) {
                front.stack = stack.copyWithCount(stack.getCount() - n);
                return false;
            }
            return true;
        }
        return Transport.deliver(sl, t, facing(), stack);
    }

    private void flushOps(ServerLevel sl) {
        if (!ops.isEmpty()) {
            AutarkiaNetwork.toTracking(sl, ChunkPos.containing(worldPosition), new TransitDeltaPayload(worldPosition, sl.getGameTime(), List.copyOf(ops)));
            ops.clear();
            setChanged();
        }
        if (changedSinceSnapshot && !segment.entries.isEmpty() && sl.getGameTime() - lastSnapshot >= 100) {
            lastSnapshot = sl.getGameTime();
            changedSinceSnapshot = false;
            syncNow();
        }
    }

    @Override
    public void clientTick() {
        if (powered) {
            BlockPos n = next(level, worldPosition);
            segment.move(AutarkiaConfig.BELT_SPEED_PU.get(), SPACING,
                frontLimit(n != null && level.getBlockEntity(n) instanceof BeltBlockEntity b ? b : null));
        }
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return lineBuffer();
    }

    @Override
    public int gridMaxIn() {
        return lineBuffer().room() > 0 ? 128 : 0;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int workDraw() {
        BeltBlockEntity h = headBelt();
        return h.line().size() * AutarkiaConfig.BELT_FE_PER_BLOCK.get();
    }

    @Override
    public boolean takesSpans() {
        return false;
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
        BeltBlockEntity h = headBelt();
        int entries = 0;
        for (BlockPos p : h.line()) {
            if (level.getBlockEntity(p) instanceof BeltBlockEntity b) {
                entries += b.segment.entries.size();
            }
        }
        out.add(Component.translatable("jade.autarkia.belt_line", h.line().size(), entries));
        int cost = h.line().size() * AutarkiaConfig.BELT_FE_PER_BLOCK.get();
        out.add(Component.translatable(powered ? "jade.autarkia.belt_power_ok" : "jade.autarkia.belt_power_none", cost));
        BeltBlockEntity end = endBelt();
        out.add(end != null && end.jammed ? Component.translatable("jade.autarkia.belt_jammed", end.blockedTicks / 20)
            : Component.translatable("jade.autarkia.belt_running"));
    }

    /** Return the last belt of this belt's line, or null when it is not loaded. */
    public @Nullable BeltBlockEntity endBelt() {
        List<BlockPos> l = headBelt().line();
        return l.isEmpty() ? this : level.getBlockEntity(l.getLast()) instanceof BeltBlockEntity b ? b : null;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel) {
            for (TransitSegment.Entry e : segment.entries) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), e.stack);
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), back.held());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), middle.held());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        segment.save(out.child("segment"));
        back.serialize(out.child("back"));
        middle.serialize(out.child("middle"));
        buffer.serialize(out.child("energy"));
        out.putBoolean("powered", powered);
        out.putBoolean("jammed", jammed);
        out.putInt("blocked", blockedTicks);
        out.putInt("delivered", delivered);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("segment").ifPresent(segment::load);
        in.child("back").ifPresent(back::deserialize);
        in.child("middle").ifPresent(middle::deserialize);
        in.child("energy").ifPresent(buffer::deserialize);
        powered = in.getBooleanOr("powered", false);
        jammed = in.getBooleanOr("jammed", false);
        blockedTicks = in.getIntOr("blocked", 0);
        delivered = in.getIntOr("delivered", 0);
    }
}
