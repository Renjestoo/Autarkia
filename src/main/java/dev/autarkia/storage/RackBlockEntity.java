package dev.autarkia.storage;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.ColonyMemory;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.colony.ColonyStorage;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.RackStatePayload;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * Rack block entity (04 sections 2.3.1 to 2.3.4): crate positions with 27 slots each behind one combined handler, the
 * load model in quarter units (light 1, standard 2, dense 4, machine 32, heavy 36, crate shell 32), the bend stage
 * from the load ratio, the stage-3 collapse timer, impact load from falling crates, and the collapse sequence.
 * Load is recomputed only when contents change. The machine class also holds every block item whose block has a
 * block entity, so the {@code load/machine} tag only lists the extra members (anvils).
 */
public class RackBlockEntity extends AutarkiaBlockEntity implements MenuProvider, JadeInfo, ColonyStorage, ColonySensors.Member, Ticking {
    public static final TagKey<Item> LIGHT = TagKey.create(Registries.ITEM, Autarkia.id("load/light"));
    public static final TagKey<Item> DENSE = TagKey.create(Registries.ITEM, Autarkia.id("load/dense"));
    public static final TagKey<Item> MACHINE = TagKey.create(Registries.ITEM, Autarkia.id("load/machine"));
    public static final TagKey<Item> HEAVY = TagKey.create(Registries.ITEM, Autarkia.id("load/heavy"));
    public static final int CRATE_SLOTS = 27;
    public static final int SHELL_Q = 32;

    /** One crate position: whether a crate stands there, the crate item without contents, and the contents. */
    private final class Position extends ItemStacksResourceHandler {
        boolean present;
        ItemStack shell = ItemStack.EMPTY;

        Position() {
            super(CRATE_SLOTS);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return present && !CrateBlockEntity.isFilledCrate(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (!bulk) {
                contentsChanged();
            }
        }
    }

    private final Position[] positions;
    private boolean bulk;
    private final CombinedResourceHandler<ItemResource> items;
    private int loadQ;
    private int stage;
    private int criticalTicks;
    private int impactQ;
    private long impactUntil;
    private double peakRatio;
    private boolean collapsing;
    private int clientSeconds;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> loadQ / 4;
                case 1 -> rating();
                case 2 -> stage;
                case 3 -> secondsLeft();
                default -> positions.length;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public RackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        int n = state.getBlock() instanceof RackBlock rb ? rb.positions : 2;
        positions = new Position[n];
        for (int i = 0; i < n; i++) {
            positions[i] = new Position();
        }
        items = new CombinedResourceHandler<>(positions);
    }

    private RackBlock rack() {
        return (RackBlock) getBlockState().getBlock();
    }

    /** Return the load rating in load units. */
    public int rating() {
        return rack().rating;
    }

    /** Return the number of crate positions. */
    public int positionCount() {
        return positions.length;
    }

    /** Return the load in load units. */
    public double load() {
        return loadQ / 4.0;
    }

    /** Return load / rating. */
    public double ratio() {
        return loadQ / (4.0 * rating());
    }

    /** Return the bend stage 0 to 3. */
    public int stage() {
        return stage;
    }

    /** Return the seconds left until collapse in stage 3, else 0. */
    public int secondsLeft() {
        return stage == 3 ? Math.max(0, (AutarkiaConfig.RACK_COLLAPSE_TICKS.get() - criticalTicks + 19) / 20) : 0;
    }

    /** Apply a {@code rack_state} payload on the client (stage and countdown for the renderer). */
    public void clientState(int stage, int secondsLeft) {
        this.stage = stage;
        this.clientSeconds = secondsLeft;
    }

    /** Return the countdown the client last received (seconds). */
    public int clientSecondsLeft() {
        return clientSeconds;
    }

    /** Return the number of crates on the rack. */
    public int crateCount() {
        int n = 0;
        for (Position p : positions) {
            n += p.present ? 1 : 0;
        }
        return n;
    }

    /** Return the menu container data: 0 load units, 1 rating, 2 stage, 3 seconds left, 4 positions. */
    public ContainerData data() {
        return data;
    }

    /** Return whether position i holds a crate. */
    public boolean present(int i) {
        return positions[i].present;
    }

    /** Return the 27-slot handler of position i (refuses inserts while no crate stands there). */
    public ItemStacksResourceHandler position(int i) {
        return positions[i];
    }

    /** Return the crate at position i as a crate item with its contents in {@code minecraft:container}, or empty. */
    public ItemStack crate(int i) {
        Position p = positions[i];
        if (!p.present) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> list = new ArrayList<>();
        for (int s = 0; s < CRATE_SLOTS; s++) {
            list.add(p.getResource(s).toStack(p.getAmountAsInt(s)));
        }
        ItemStack out = p.shell.copyWithCount(1);
        out.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return out;
    }

    /** Put a crate item (empty or filled) at position i, or clear it with an empty stack. */
    public void setCrate(int i, ItemStack stack) {
        Position p = positions[i];
        p.present = !stack.isEmpty();
        p.shell = p.present ? stack.copyWithCount(1) : ItemStack.EMPTY;
        p.shell.remove(DataComponents.CONTAINER);
        net.minecraft.core.NonNullList<ItemStack> nl = net.minecraft.core.NonNullList.withSize(CRATE_SLOTS, ItemStack.EMPTY);
        ItemContainerContents c = stack.isEmpty() ? null : stack.get(DataComponents.CONTAINER);
        if (c != null) {
            c.copyInto(nl);
        }
        bulk = true;
        for (int s = 0; s < CRATE_SLOTS; s++) {
            p.set(s, ItemResource.of(nl.get(s)), nl.get(s).getCount());
        }
        bulk = false;
        contentsChanged();
    }

    /** Put a crate into the first free position; returns false when every position is taken. */
    public boolean insertCrate(ItemStack crate) {
        for (int i = 0; i < positions.length; i++) {
            if (!positions[i].present) {
                setCrate(i, crate);
                return true;
            }
        }
        return false;
    }

    /** Return the load of a stack in quarter units (04 section 2.3.2; a higher class wins). */
    public static int unitsQ(ItemStack st) {
        if (st.isEmpty()) {
            return 0;
        }
        int per;
        if (st.is(HEAVY)) {
            per = 36;
        } else if (st.is(MACHINE) || st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof EntityBlock) {
            per = 32;
        } else if (st.is(DENSE)) {
            per = 4;
        } else if (st.is(LIGHT)) {
            per = 1;
        } else {
            per = 2;
        }
        return per * st.getCount();
    }

    /** Return the load of a crate item with its contents in quarter units, shell included. */
    public static int crateQ(ItemStack crate) {
        int q = SHELL_Q;
        ItemContainerContents c = crate.get(DataComponents.CONTAINER);
        if (c != null) {
            q += c.nonEmptyItemCopyStream().mapToInt(RackBlockEntity::unitsQ).sum();
        }
        return q;
    }

    private int computeLoadQ() {
        int q = 0;
        for (Position p : positions) {
            if (!p.present) {
                continue;
            }
            q += SHELL_Q;
            for (int s = 0; s < CRATE_SLOTS; s++) {
                q += unitsQ(p.getResource(s).toStack(p.getAmountAsInt(s)));
            }
        }
        return q;
    }

    private void contentsChanged() {
        loadQ = computeLoadQ();
        peakRatio = Math.max(peakRatio, ratio());
        updateStage();
        setChanged();
        if (level instanceof ServerLevel) {
            syncNow();
        }
    }

    private int stageFor(double r) {
        double bend = AutarkiaConfig.RACK_BEND_THRESHOLD.get();
        double crit = AutarkiaConfig.RACK_CRITICAL_THRESHOLD.get();
        if (r <= bend) {
            return 0;
        }
        if (r <= (bend + crit) / 2) {
            return 1;
        }
        return r <= crit ? 2 : 3;
    }

    private void updateStage() {
        int s = stageFor(ratio());
        if (s == stage) {
            return;
        }
        stage = s;
        if (s < 3) {
            criticalTicks = 0;
        }
        if (level instanceof ServerLevel sl && !collapsing) {
            BlockState st = getBlockState();
            if (st.hasProperty(RackBlock.BEND) && st.getValue(RackBlock.BEND) != s) {
                sl.setBlock(worldPosition, st.setValue(RackBlock.BEND, s), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
            AutarkiaNetwork.toTracking(sl, net.minecraft.world.level.ChunkPos.containing(worldPosition), new RackStatePayload(worldPosition, s,
                (int) Math.round(ratio() * 1000), secondsLeft()));
        }
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        if (sl == null || collapsing) {
            return;
        }
        long now = sl.getGameTime();
        if (impactQ > 0 && now >= impactUntil) {
            impactQ = 0;
        }
        if (stage == 0) {
            return;
        }
        int period = stage == 1 ? 200 : stage == 2 ? 100 : 40;
        if ((now + worldPosition.asLong()) % period == 0) {
            sl.playSound(null, worldPosition, AutarkiaSounds.of("rack_creak"), SoundSource.BLOCKS, 0.8f, 0.5f);
            Vec3 c = Vec3.atCenterOf(worldPosition);
            if (stage >= 2) {
                sl.sendParticles(ParticleTypes.DUST_PLUME, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.01);
            }
            if (stage == 3) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()), c.x, c.y, c.z, 8, 0.3, 0.4, 0.3, 0.05);
            }
        }
        if (stage == 3) {
            criticalTicks++;
            if (criticalTicks % 20 == 0) {
                AutarkiaNetwork.toTracking(sl, net.minecraft.world.level.ChunkPos.containing(worldPosition), new RackStatePayload(worldPosition, stage,
                    (int) Math.round(ratio() * 1000), secondsLeft()));
            }
            if (criticalTicks >= AutarkiaConfig.RACK_COLLAPSE_TICKS.get()) {
                collapse("overload");
            }
        }
    }

    /**
     * Add impact load from a falling crate for 40 ticks (04 section 2.3.3); the rack collapses at once when
     * (load + impact) / rating exceeds the impact threshold.
     */
    public void impact(int q) {
        ServerLevel sl = serverLevel();
        if (sl == null || collapsing) {
            return;
        }
        impactQ += q;
        impactUntil = sl.getGameTime() + 40;
        double r = (loadQ + impactQ) / (4.0 * rating());
        peakRatio = Math.max(peakRatio, r);
        if (r > AutarkiaConfig.RACK_IMPACT_THRESHOLD.get()) {
            collapse("impact");
        }
    }

    /**
     * Run the collapse sequence (04 section 2.3.4): sound and particles, remove the rack, drop the salvage, turn every
     * crate into a falling crate (over the level cap: a crate block on the nearest free floor), record the failure
     * event and collapse the racks directly above. With {@code rackCollapseEnabled} off, overload and impact do
     * nothing and a rack that loses its support breaks with its normal drops.
     */
    public void collapse(String cause) {
        ServerLevel sl = serverLevel();
        if (sl == null || collapsing) {
            return;
        }
        if (!AutarkiaConfig.RACK_COLLAPSE_ENABLED.get()) {
            if (cause.equals("support")) {
                sl.destroyBlock(worldPosition, true);
            }
            return;
        }
        collapsing = true;
        BlockState state = getBlockState();
        Direction front = MachineBlock.front(state);
        Vec3 c = Vec3.atCenterOf(worldPosition);
        sl.playSound(null, worldPosition, AutarkiaSounds.of("rack_collapse"), SoundSource.BLOCKS, 1.2f, 0.5f);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), c.x, c.y, c.z, 40, 0.4, 0.5, 0.4, 0.1);
        sl.sendParticles(ParticleTypes.DUST_PLUME, c.x, c.y, c.z, 16, 0.4, 0.5, 0.4, 0.02);
        List<ItemStack> crates = new ArrayList<>();
        for (int i = 0; i < positions.length; i++) {
            ItemStack cr = crate(i);
            crates.add(cr);
        }
        BlockPos rowStart = rowStart(sl, state, front);
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        BlockPos back = worldPosition.relative(front.getOpposite());
        boolean wall = sl.getBlockState(back).isFaceSturdy(sl, back, front);
        Autarkia.LOG.info("[rack] {} at {} collapsed ({}): peak {}%, {} crates", id, worldPosition.toShortString(), cause,
            Math.round(peakRatio * 100), crateCount());
        sl.removeBlock(worldPosition, false);
        Containers.dropItemStack(sl, c.x, c.y, c.z, rack().salvage());
        var rnd = sl.getRandom();
        for (int i = 0; i < crates.size(); i++) {
            ItemStack cr = crates.get(i);
            if (cr.isEmpty()) {
                continue;
            }
            double speed = 0.05 + rnd.nextDouble() * 0.10;
            double ang = wall ? Math.atan2(front.getStepZ(), front.getStepX()) + (rnd.nextDouble() - 0.5) * 0.6 : rnd.nextDouble() * Math.PI * 2;
            Vec3 start = new Vec3(c.x, worldPosition.getY() + 0.05 + i * (1.0 / positions.length), c.z);
            FallingCrateEntity.collapse(sl, start, new Vec3(Math.cos(ang) * speed, 0.05, Math.sin(ang) * speed), cr);
        }
        ColonyMemory.at(sl, rowStart, "rack_collapse", id + "@" + rowStart.toShortString());
        AutarkiaAdvancements.awardNear(sl, c, "logistics/rack_collapse");
        BlockPos up = worldPosition.above();
        if (sl.getBlockEntity(up) instanceof RackBlockEntity above) {
            above.collapse("cascade");
        }
    }

    private BlockPos rowStart(ServerLevel sl, BlockState state, Direction front) {
        Direction along = front.getCounterClockWise();
        BlockPos p = worldPosition;
        for (int i = 0; i < 64 && sl.getBlockState(p.relative(along)).is(state.getBlock()); i++) {
            p = p.relative(along);
        }
        return p.immutable();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel && !collapsing) {
            for (int i = 0; i < positions.length; i++) {
                ItemStack cr = crate(i);
                if (!cr.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cr);
                }
            }
        }
    }

    @Override
    public ResourceHandler<ItemResource> colonyItems() {
        return items;
    }

    /** Return the combined item handler of all crates. */
    public ResourceHandler<ItemResource> items() {
        return items;
    }

    @Override
    public java.util.Set<ColonySensors.Kind> colonyKinds() {
        return java.util.Set.of(ColonySensors.Kind.STORAGE);
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.rack.load", Math.round(load()), rating(), Math.round(ratio() * 100)));
        out.add(Component.translatable("jade.autarkia.rack.stage", stage));
        if (stage == 3) {
            out.add(Component.translatable("jade.autarkia.rack.collapse_in", secondsLeft()));
        }
        out.add(Component.translatable("jade.autarkia.rack.crates", crateCount(), positions.length));
    }

    @Override
    public float jadeProgress() {
        return (float) Math.min(1, ratio());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new RackMenu(id, playerInv, this);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        for (int i = 0; i < positions.length; i++) {
            Position p = positions[i];
            out.putBoolean("present" + i, p.present);
            if (p.present) {
                out.store("shell" + i, ItemStack.CODEC, p.shell);
            }
            p.serialize(out.child("crate" + i));
        }
        out.putInt("critical", criticalTicks);
        out.putDouble("peak", peakRatio);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        for (int i = 0; i < positions.length; i++) {
            Position p = positions[i];
            p.present = in.getBooleanOr("present" + i, false);
            p.shell = p.present ? in.read("shell" + i, ItemStack.CODEC).orElse(new ItemStack(AutarkiaItems.CRATE.get())) : ItemStack.EMPTY;
            final int idx = i;
            in.child("crate" + idx).ifPresent(positions[idx]::deserialize);
        }
        criticalTicks = in.getIntOr("critical", 0);
        peakRatio = in.getDoubleOr("peak", 0);
        loadQ = computeLoadQ();
        stage = stageFor(ratio());
    }
}
