package dev.autarkia.logistics;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * {@code belt_splitter} and {@code sorting_chute} (04 section 1.3): inline blocks with a buffer of 1 entry, input at the
 * back, outputs at the front and at the chosen side. The splitter needs 2 FE/t (from the feeding belt line, else its
 * own energy face) and runs ALTERNATE (front, side, ...; when one refuses it uses the other) or FILTER (matching
 * entries to the side, the rest to the front, no fallback). The sorting chute needs no power: matching entries leave
 * through the side, the rest go on; a refused side blocks it. Filters: 9 ghost slots, whitelist or blacklist, match
 * components or item only (04 rule C8).
 */
public class FilterBlockEntity extends AutarkiaBlockEntity implements Ticking, TransitOwner, JadeInfo, MenuProvider,
    dev.autarkia.colony.PatternDataHolder {
    public static final int LENGTH = 1000;
    private final boolean splitter;
    private final TransitSegment segment = new TransitSegment(LENGTH);
    private final EnergyBuffer energy = new EnergyBuffer(400, 20, 0, this::setChanged);
    private final NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
    private final ItemStacksResourceHandler intake = new ItemStacksResourceHandler(1) {
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return BeltBlockEntity.MAX_ITEMS;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return segment.entries.isEmpty() && !dev.autarkia.storage.CrateBlockEntity.isFilledCrate(resource.toStack(1));
        }
    };
    private final List<TransitDeltaPayload.Op> ops = new ArrayList<>();
    private boolean filterMode;
    private boolean rightSide;
    private boolean whitelist = true;
    private boolean matchComponents;
    private boolean nextSide;
    private boolean powered;
    private int sorted;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> filterMode ? 1 : 0;
                case 1 -> rightSide ? 1 : 0;
                case 2 -> whitelist ? 1 : 0;
                case 3 -> matchComponents ? 1 : 0;
                default -> splitter ? 1 : 0;
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

    private FilterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean splitter) {
        super(type, pos, state);
        this.splitter = splitter;
    }

    /** Create a belt splitter block entity. */
    public static FilterBlockEntity splitter(BlockPos pos, BlockState state) {
        return new FilterBlockEntity(AutarkiaBlockEntities.BELT_SPLITTER.get(), pos, state, true);
    }

    /** Create a sorting chute block entity. */
    public static FilterBlockEntity sortingChute(BlockPos pos, BlockState state) {
        return new FilterBlockEntity(AutarkiaBlockEntities.SORTING_CHUTE.get(), pos, state, false);
    }

    /** Return the facing (entries enter at the back and travel toward it). */
    public Direction facing() {
        return MachineBlock.front(getBlockState());
    }

    /** Put the side output on the right (clockwise of the facing) or on the left. */
    public void setRightSide(boolean right) {
        rightSide = right;
        setChanged();
    }

    /** Return the side output direction. */
    public Direction sideDir() {
        return rightSide ? facing().getClockWise() : facing().getCounterClockWise();
    }

    @Override
    public TransitSegment segment() {
        return segment;
    }

    /** Return the intake handler of the back face. */
    public ItemStacksResourceHandler intake() {
        return intake;
    }

    /** Return the own energy buffer of the splitter. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the 9 ghost filter stacks (live list). */
    public NonNullList<ItemStack> filter() {
        return filter;
    }

    /** Return the menu data: mode, side, whitelist, components, splitter flag. */
    public ContainerData data() {
        return data;
    }

    /** Return true for the splitter, false for the sorting chute. */
    public boolean isSplitter() {
        return splitter;
    }

    /** Apply a menu button: 0 mode, 1 side, 2 whitelist, 3 components. */
    public void button(int id) {
        switch (id) {
            case 0 -> filterMode = splitter && !filterMode;
            case 1 -> rightSide = !rightSide;
            case 2 -> whitelist = !whitelist;
            case 3 -> matchComponents = !matchComponents;
            default -> {
                return;
            }
        }
        setChanged();
        syncNow();
    }

    /** Set ghost slot i to a one-item copy of the stack (empty clears it). */
    public void setGhost(int i, ItemStack stack) {
        filter.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
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

    /** Take an entry at the back when the buffer is empty. */
    public boolean accept(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() > BeltBlockEntity.MAX_ITEMS || !segment.entries.isEmpty() || level == null) {
            return false;
        }
        TransitSegment.Entry e = segment.insert(stack.copy(), 0);
        ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.ADD, e.id, e.stack, 0));
        setChanged();
        return true;
    }

    private boolean pay(ServerLevel sl) {
        if (!splitter) {
            return true;
        }
        BlockPos behind = worldPosition.relative(facing().getOpposite());
        if (sl.getBlockEntity(behind) instanceof BeltBlockEntity belt && belt.lineBuffer().drain(2) == 2) {
            return true;
        }
        return energy.drain(2) == 2;
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        if (intake.getAmountAsInt(0) > 0 && accept(intake.getResource(0).toStack(intake.getAmountAsInt(0)))) {
            intake.set(0, ItemResource.EMPTY, 0);
        }
        boolean on = pay(sl);
        if (on != powered) {
            powered = on;
            syncNow();
        }
        if (on) {
            segment.move(AutarkiaConfig.BELT_SPEED_PU.get(), BeltBlockEntity.SPACING);
            TransitSegment.Entry front = segment.atEnd();
            if (front != null && route(sl, front.stack)) {
                segment.remove(front.id);
                ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.REMOVE, front.id, ItemStack.EMPTY, 0));
            }
        }
        if (!ops.isEmpty()) {
            AutarkiaNetwork.toTracking(sl, ChunkPos.containing(worldPosition), new TransitDeltaPayload(worldPosition, sl.getGameTime(), List.copyOf(ops)));
            ops.clear();
            setChanged();
        }
    }

    private boolean route(ServerLevel sl, ItemStack stack) {
        BlockPos front = worldPosition.relative(facing());
        BlockPos side = worldPosition.relative(sideDir());
        if (!splitter || filterMode) {
            boolean toSide = matches(stack);
            boolean ok = toSide ? Transport.deliver(sl, side, sideDir(), stack) : Transport.deliver(sl, front, facing(), stack);
            if (ok) {
                sorted += stack.getCount();
                if (sorted >= 256) {
                    AutarkiaAdvancements.awardNear(sl, Vec3.atCenterOf(worldPosition), "logistics/sorted");
                }
            }
            return ok;
        }
        boolean first = nextSide ? Transport.deliver(sl, side, sideDir(), stack) : Transport.deliver(sl, front, facing(), stack);
        if (first) {
            nextSide = !nextSide;
            sl.playSound(null, worldPosition, AutarkiaSounds.of("splitter_flip"), SoundSource.BLOCKS, 0.4f, 1.4f);
            return true;
        }
        return nextSide ? Transport.deliver(sl, front, facing(), stack) : Transport.deliver(sl, side, sideDir(), stack);
    }

    @Override
    public void clientTick() {
        if (powered) {
            segment.move(AutarkiaConfig.BELT_SPEED_PU.get(), BeltBlockEntity.SPACING);
        }
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        int n = (int) filter.stream().filter(s -> !s.isEmpty()).count();
        out.add(Component.translatable(splitter && !filterMode ? "jade.autarkia.filter_alternate" : "jade.autarkia.filter_filter",
            Component.translatable(rightSide ? "jade.autarkia.side_right" : "jade.autarkia.side_left"), n));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new FilterMenu(id, playerInv, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel) {
            for (TransitSegment.Entry e : segment.entries) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), e.stack);
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), intake.getResource(0).toStack(intake.getAmountAsInt(0)));
        }
    }

    @Override
    public void writePatternData(ValueOutput out) {
        out.store("filter", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(filter));
        out.putBoolean("filterMode", filterMode);
        out.putBoolean("right", rightSide);
        out.putBoolean("whitelist", whitelist);
        out.putBoolean("matchComponents", matchComponents);
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
        filterMode = in.getBooleanOr("filterMode", false);
        rightSide = in.getBooleanOr("right", false);
        whitelist = in.getBooleanOr("whitelist", true);
        matchComponents = in.getBooleanOr("matchComponents", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        segment.save(out.child("segment"));
        intake.serialize(out.child("intake"));
        energy.serialize(out.child("energy"));
        out.store("filter", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(filter));
        out.putBoolean("filterMode", filterMode);
        out.putBoolean("right", rightSide);
        out.putBoolean("whitelist", whitelist);
        out.putBoolean("matchComponents", matchComponents);
        out.putBoolean("nextSide", nextSide);
        out.putBoolean("powered", powered);
        out.putInt("sorted", sorted);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("segment").ifPresent(segment::load);
        in.child("intake").ifPresent(intake::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        readSettings(in);
        nextSide = in.getBooleanOr("nextSide", false);
        powered = in.getBooleanOr("powered", false);
        sorted = in.getIntOr("sorted", 0);
    }
}
