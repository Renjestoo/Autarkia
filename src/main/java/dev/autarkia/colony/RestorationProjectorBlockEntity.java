package dev.autarkia.colony;

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
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MenuButtons;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * The {@code restoration_projector} (05 section 8.5, W25): a 1x2x1 projector (center at the bottom) with a box of at most
 * 64 x 32 x 64 inside its own colony domain, by default centered in X and Z from y-8 to y+23 and clipped to the domain
 * square that holds the projector. The menu edits size and offset (buttons 0 to 11 step 1, 20 to 31 step 8: size x-, x+,
 * y-, y+, z-, z+, then offset in the same order) and takes the snapshot (button 12); only the owner or a trusted player
 * may use them. A snapshot is also taken automatically when the projector first gets power (M9 decision G11) and when a
 * colony project builds it (G22). FE buffer 512 x 8 x 200, input 512 x 8 x 4 FE/t. The work runs in {@link Restoration}
 * under the server budgets; breaking the projector removes its field and deletes its snapshot file.
 */
public class RestorationProjectorBlockEntity extends AutarkiaBlockEntity implements Ticking, GeoBlockEntity, MenuProvider, MenuButtons, GridMember,
    JadeInfo {
    /** Projector states for the GUI and Jade. */
    public enum Status { NO_SNAPSHOT, SNAPSHOT, WATCHING, RESTORING, NO_POWER, NO_MATTER, OUTSIDE, DISABLED }

    public static final int MAX_X = 64;
    public static final int MAX_Y = 32;
    public static final int MAX_Z = 64;
    public static final int CAPACITY = 512 * 8 * 200;
    public static final int MAX_IN = 512 * 8 * 4;
    public static final int BUTTON_SNAPSHOT = 12;
    public static final int COARSE = 20;
    public static final int MAX_OFFSET = 64;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.restoration_projector.idle");
    private static final RawAnimation PROJECT = RawAnimation.begin().thenLoop("animation.restoration_projector.project");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IN, 0, this::setChanged);
    final RestorationRun run = new RestorationRun(this);
    private final int[] box = {MAX_X, MAX_Y, MAX_Z, -MAX_X / 2, -8, -MAX_Z / 2};
    private @Nullable UUID snapshot;
    private boolean poweredOnce;
    private boolean snapshotPending;
    Status status = Status.NO_SNAPSHOT;
    int missing;
    private long eventStart = -1;
    private int eventCount;
    private boolean eventFired;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            Snapshot.Builder b = run.builder;
            return switch (i) {
                case 0 -> b == null ? 0 : b.cursor();
                case 1 -> b == null ? 0 : b.volume();
                case 2 -> energy.stored();
                case 3 -> energy.capacity();
                case 4 -> status.ordinal();
                case 5, 6, 7, 8, 9, 10 -> box[i - 5];
                case 11 -> missing;
                default -> snapshot == null ? 0 : 1;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 13;
        }
    };

    public RestorationProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.RESTORATION_PROJECTOR.get(), pos, state);
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the status. */
    public Status status() {
        return status;
    }

    /** Return the missing cells of the last scan less the cells restored or no longer missing since. */
    public int missing() {
        return missing;
    }

    /** Return the snapshot id, or null before the first snapshot. */
    public @Nullable UUID snapshotId() {
        return snapshot;
    }

    /** Return whether a snapshot read is running or waiting to start. */
    public boolean snapshotting() {
        return run.builder != null || snapshotPending;
    }

    /** Return the box size {x, y, z} the next snapshot uses. */
    public Vec3i boxSize() {
        return new Vec3i(box[0], box[1], box[2]);
    }

    /** Return the min corner of the box the next snapshot uses. */
    public BlockPos boxMin() {
        return worldPosition.offset(box[3], box[4], box[5]);
    }

    /** Return the running work state (counters, restore order) for the status command and the self test. */
    RestorationRun run() {
        return run;
    }

    /** Return the most cells placed in one tick so far. */
    public int maxPerTick() {
        return run.maxPerTick;
    }

    /** Return the cells restored so far, and of these the ones paid with debris items and with matter units. */
    public long[] restoredCounts() {
        return new long[] {run.restored, run.fromItems, run.fromMatter, run.noMatter, run.feUsed};
    }

    /** Return the snapshot cell indices in their restore order since the last queue started (at most 4,096). */
    public int[] restoreOrder() {
        return run.order.toIntArray();
    }

    @Override
    public void serverTick() {
        Restoration.enqueue(serverLevel(), this);
    }

    /** Run one tick of work under the server budgets (called by {@link Restoration} after the block entities ticked). */
    void work(ServerLevel level, Restoration.Budget budget) {
        Status before = status;
        long now = level.getGameTime();
        run.settle(level, now);
        Colony c = Colonies.at(level, worldPosition);
        if (!poweredOnce && energy.stored() > 0) {
            poweredOnce = true;
            if (snapshot == null) {
                snapshotPending = true;
            }
            setChanged();
        }
        if (!AutarkiaConfig.RESTORATION_ENABLED.get()) {
            status = Status.DISABLED;
        } else if (c == null) {
            status = Status.OUTSIDE;
        } else {
            step(level, c, budget, now);
        }
        if (status != before) {
            syncNow();
        } else {
            flushSync(20);
        }
    }

    private void step(ServerLevel level, Colony c, Restoration.Budget budget, long now) {
        if (snapshotPending && run.builder == null) {
            snapshotPending = false;
            begin(level, c, now);
        }
        if (run.builder != null) {
            status = Status.SNAPSHOT;
            if (run.builder.step(level, Snapshot.CELLS_PER_TICK)) {
                finish(level, c, run.builder.build());
                run.builder = null;
            }
            return;
        }
        Snapshot s = snapshot == null ? null : Snapshot.load(level.getServer(), snapshot);
        if (s == null) {
            status = Status.NO_SNAPSHOT;
            return;
        }
        if (budget.scan > 0) {
            budget.scan -= run.scan(level, c, s, budget.scan, now);
        }
        int allowed = Math.min(AutarkiaConfig.RESTORATION_CELLS_PER_TICK_PER_PROJECTOR.get(), budget.place);
        status = Status.WATCHING;
        if (run.queued() > 0) {
            budget.place -= run.restore(level, c, s, allowed, now);
            if (status == Status.RESTORING && now % 40 == 0) {
                level.playSound(null, worldPosition, AutarkiaSounds.of("restoration_hum"), SoundSource.BLOCKS, 1f, 1f);
            }
            syncThrottled(10);
        }
    }

    private void begin(ServerLevel level, Colony c, long now) {
        fit(level, c);
        UUID id = snapshot != null ? snapshot : UUID.randomUUID();
        run.reset();
        run.builder = new Snapshot.Builder(id, boxMin(), boxSize(), now);
        status = Status.SNAPSHOT;
        Autarkia.LOG.info("[restore] {} snapshot {} of {} x {} x {} at {}", worldPosition.toShortString(), id, box[0], box[1], box[2],
            boxMin().toShortString());
        setChanged();
    }

    private void finish(ServerLevel level, Colony c, Snapshot s) {
        Snapshot.store(level.getServer(), s);
        snapshot = s.id;
        missing = 0;
        RestorationField f = new RestorationField(worldPosition.immutable(), s.id, s.min, s.max());
        int at = -1;
        for (int i = 0; i < c.fields.size(); i++) {
            if (c.fields.get(i).projectorPos().equals(worldPosition)) {
                at = i;
            }
        }
        if (at >= 0) {
            c.fields.set(at, f);
        } else {
            c.fields.add(f);
        }
        Colonies.changed();
        Autarkia.LOG.info("[restore] {} snapshot {} done: {} cells, palette {}", worldPosition.toShortString(), s.id, s.volume(), s.paletteSize());
        setChanged();
    }

    /** Start a snapshot now (Snapshot button, first power, a finished colony project); refused outside a domain. */
    public boolean takeSnapshot() {
        if (!(level instanceof ServerLevel sl) || Colonies.at(sl, worldPosition) == null) {
            return false;
        }
        snapshotPending = true;
        run.builder = null;
        setChanged();
        return true;
    }

    /** Reset the box to the default (64 x 32 x 64, centered in X and Z, y-8 to y+23); the next snapshot clips it to the domain. */
    public void defaultBox() {
        System.arraycopy(new int[] {MAX_X, MAX_Y, MAX_Z, -MAX_X / 2, -8, -MAX_Z / 2}, 0, box, 0, 6);
        setChanged();
    }

    /** Set the box size and min-corner offset (relative to the projector); return false when it breaks the limits or leaves the domain. */
    public boolean setBox(int sx, int sy, int sz, int ox, int oy, int oz) {
        int[] old = box.clone();
        int[] next = {sx, sy, sz, ox, oy, oz};
        if (sx < 1 || sy < 1 || sz < 1 || sx > MAX_X || sy > MAX_Y || sz > MAX_Z || Math.abs(ox) > MAX_OFFSET || Math.abs(oy) > MAX_OFFSET
            || Math.abs(oz) > MAX_OFFSET) {
            return false;
        }
        System.arraycopy(next, 0, box, 0, 6);
        Colony c = level instanceof ServerLevel sl ? Colonies.at(sl, worldPosition) : null;
        if (c == null || !inside(c)) {
            System.arraycopy(old, 0, box, 0, 6);
            return false;
        }
        setChanged();
        return true;
    }

    private boolean inside(Colony c) {
        BlockPos min = boxMin();
        if (level == null || min.getY() < level.getMinY() || min.getY() + box[1] - 1 > level.getMaxY()) {
            return false;
        }
        for (int x = 0; x < box[0]; x++) {
            for (int z = 0; z < box[2]; z++) {
                if (Colonies.at(level, min.offset(x, 0, z)) != c) {
                    return false;
                }
            }
        }
        return true;
    }

    private void fit(ServerLevel level, Colony c) {
        if (inside(c)) {
            return;
        }
        for (int[] sq : Colonies.squares(c)) {
            if (Math.abs(worldPosition.getX() - sq[0]) > sq[2] || Math.abs(worldPosition.getZ() - sq[1]) > sq[2]) {
                continue;
            }
            int x0 = Math.max(worldPosition.getX() + box[3], sq[0] - sq[2]);
            int x1 = Math.min(worldPosition.getX() + box[3] + box[0] - 1, sq[0] + sq[2]);
            int z0 = Math.max(worldPosition.getZ() + box[5], sq[1] - sq[2]);
            int z1 = Math.min(worldPosition.getZ() + box[5] + box[2] - 1, sq[1] + sq[2]);
            int y0 = Math.max(worldPosition.getY() + box[4], level.getMinY());
            int y1 = Math.min(worldPosition.getY() + box[4] + box[1] - 1, level.getMaxY());
            System.arraycopy(new int[] {x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1, x0 - worldPosition.getX(), y0 - worldPosition.getY(),
                z0 - worldPosition.getZ()}, 0, box, 0, 6);
            Autarkia.LOG.info("[restore] {} box clipped to the domain: {} x {} x {}", worldPosition.toShortString(), box[0], box[1], box[2]);
            return;
        }
    }

    /** Open the event window of 200 ticks when a blast hits the box, and drop the scan and queue made before it. */
    void blastHit(long now) {
        if (eventStart < 0 || now - eventStart > Restoration.EVENT_WINDOW) {
            eventStart = now;
            eventCount = 0;
            eventFired = false;
        }
        run.blast();
        setChanged();
    }

    /** Count a restored cell toward the open event; 64 cells in one event fire {@code field_restored_64} once. */
    void countEvent(Colony c, long now) {
        if (eventStart < 0 || now - eventStart > Restoration.EVENT_WINDOW) {
            return;
        }
        eventCount++;
        if (eventCount >= Restoration.EVENT_CELLS && !eventFired) {
            eventFired = true;
            ColonyEvents.fire(c, "field_restored_64");
            Autarkia.LOG.info("[restore] {} restored {} cells in one event", worldPosition.toShortString(), eventCount);
        }
    }

    /** Return the cells restored in the current or last event window. */
    public int eventCells() {
        return eventCount;
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (!(level instanceof ServerLevel sl)) {
            return false;
        }
        Colony c = Colonies.at(sl, worldPosition);
        if (c == null || !c.canManage(player.getUUID())) {
            return false;
        }
        if (id == BUTTON_SNAPSHOT) {
            return takeSnapshot();
        }
        int k = id >= COARSE ? id - COARSE : id;
        if (k < 0 || k > 11) {
            return false;
        }
        int step = (id >= COARSE ? 8 : 1) * (k % 2 == 0 ? -1 : 1);
        int[] next = box.clone();
        next[k / 2] += step;
        return setBox(next[0], next[1], next[2], next[3], next[4], next[5]);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (isCenter() && level instanceof ServerLevel sl) {
            Restoration.removed(sl, pos, snapshot);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.restoration_projector");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.RESTORATION_PROJECTOR.get(), id, playerInv, worldPosition, new ItemStacksResourceHandler(0), data,
            GuiLayouts.RESTORATION_PROJECTOR);
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
        return energy.room() > 0 ? MAX_IN : 0;
    }

    @Override
    public int workDraw() {
        return status == Status.RESTORING || status == Status.NO_POWER
            ? AutarkiaConfig.RESTORATION_FE_PER_BLOCK.get() * AutarkiaConfig.RESTORATION_CELLS_PER_TICK_PER_PROJECTOR.get() : 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "colony";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Snapshot s = snapshot == null || !(level instanceof ServerLevel sl) ? null : Snapshot.load(sl.getServer(), snapshot);
        out.add(s == null ? Component.translatable("jade.autarkia.restoration.no_snapshot")
            : Component.translatable("jade.autarkia.restoration.snapshot", s.size.getX(), s.size.getY(), s.size.getZ()));
        out.add(Component.translatable("jade.autarkia.restoration.missing", missing));
        out.add(Component.translatable("jade.autarkia.restoration.state." + status.name().toLowerCase(java.util.Locale.ROOT)));
        out.add(Component.translatable("jade.autarkia.buffer", energy.stored(), energy.capacity()));
    }

    @Override
    public float jadeProgress() {
        Snapshot.Builder b = run.builder;
        return b == null ? -1 : b.cursor() / (float) Math.max(1, b.volume());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RestorationProjectorBlockEntity>("main", 4,
            test -> test.setAndContinue(test.animatable().status == Status.RESTORING || test.animatable().status == Status.SNAPSHOT ? PROJECT : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        energy.serialize(out.child("energy"));
        out.putIntArray("box", box.clone());
        if (snapshot != null) {
            out.store("snapshot", UUIDUtil.CODEC, snapshot);
        }
        out.putBoolean("powered_once", poweredOnce);
        out.putBoolean("snapshotting", snapshotting());
        out.putInt("status", status.ordinal());
        out.putInt("missing", missing);
        out.putLong("event_start", eventStart);
        out.putInt("event_count", eventCount);
        out.putBoolean("event_fired", eventFired);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(energy::deserialize);
        in.getIntArray("box").filter(a -> a.length == 6).ifPresent(a -> System.arraycopy(a, 0, box, 0, 6));
        snapshot = in.read("snapshot", UUIDUtil.CODEC).orElse(null);
        poweredOnce = in.getBooleanOr("powered_once", false);
        snapshotPending = in.getBooleanOr("snapshotting", false);
        status = Status.values()[Math.max(0, Math.min(Status.values().length - 1, in.getIntOr("status", 0)))];
        missing = in.getIntOr("missing", 0);
        eventStart = in.getLongOr("event_start", -1L);
        eventCount = in.getIntOr("event_count", 0);
        eventFired = in.getBooleanOr("event_fired", false);
    }
}
