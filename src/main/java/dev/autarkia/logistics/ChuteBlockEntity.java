package dev.autarkia.logistics;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.TransitDeltaPayload;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Gravity chute (04 section 1.2): 1,000 pu long, spacing 1,000 pu, 1 to 4 items per entry. Entries leave at the front,
 * one block down (handler, chute or belt); a refused entry waits at the low end and never drops.
 */
public class ChuteBlockEntity extends AutarkiaBlockEntity implements Ticking, TransitOwner, dev.autarkia.registry.JadeInfo {
    public static final int LENGTH = 1000;
    public static final int SPACING = 1000;
    public static final int MAX_ITEMS = 4;
    public static final int STUCK_TICKS = 600;
    private final TransitSegment segment = new TransitSegment(LENGTH);
    private final ItemStacksResourceHandler intake = new ItemStacksResourceHandler(1) {
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return MAX_ITEMS;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return segment.canInsertAt(0, SPACING) && !dev.autarkia.storage.CrateBlockEntity.isFilledCrate(resource.toStack(1));
        }
    };
    private final List<TransitDeltaPayload.Op> ops = new ArrayList<>();
    private int blockedTicks;
    private long lastSnapshot;
    private boolean changedSinceSnapshot;

    public ChuteBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CHUTE.get(), pos, state);
    }

    @Override
    public TransitSegment segment() {
        return segment;
    }

    /** Return the handler that hoppers and pipes insert into (one entry of up to 4 items). */
    public ResourceHandler<ItemResource> intake() {
        return intake;
    }

    /** Return how long the front entry has waited at the end (ticks). */
    public int blockedTicks() {
        return blockedTicks;
    }

    /** Return true when the chute is blocked for 30 s or more (sensor and stuck visual). */
    public boolean stuck() {
        return segment.blocked && blockedTicks >= STUCK_TICKS;
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<net.minecraft.network.chat.Component> out) {
        int items = 0;
        for (TransitSegment.Entry e : segment.entries) {
            items += e.stack.getCount();
        }
        out.add(net.minecraft.network.chat.Component.translatable("jade.autarkia.items", items));
        out.add(net.minecraft.network.chat.Component.translatable(stuck() ? "jade.autarkia.chute_blocked"
            : segment.blocked ? "jade.autarkia.chute_waiting" : "jade.autarkia.chute_moving"));
    }

    /** Accept an entry at the head when there is room; returns false otherwise. */
    public boolean accept(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() > MAX_ITEMS || !segment.canInsertAt(0, SPACING) || level == null) {
            return false;
        }
        TransitSegment.Entry e = segment.insert(stack.copy(), 0);
        ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.ADD, e.id, e.stack, 0));
        changedSinceSnapshot = true;
        setChanged();
        return true;
    }

    /** Return the block position entries leave into: the front, one block down. */
    public BlockPos target() {
        return worldPosition.relative(MachineBlock.front(getBlockState())).below();
    }

    @Override
    public void serverTick() {
        ServerLevel sl = (ServerLevel) level;
        if (!sl.isLoaded(target())) {
            return;
        }
        if (intake.getAmountAsInt(0) > 0 && segment.canInsertAt(0, SPACING)) {
            ItemStack in = intake.getResource(0).toStack(intake.getAmountAsInt(0));
            intake.set(0, ItemResource.EMPTY, 0);
            accept(in);
        }
        if (sl.getGameTime() % 8 == Math.floorMod(worldPosition.asLong(), 8) && segment.canInsertAt(0, SPACING)) {
            pullFromAbove(sl);
        }
        segment.move(AutarkiaConfig.CHUTE_SPEED_PU.get(), SPACING);
        TransitSegment.Entry front = segment.atEnd();
        if (front != null) {
            if (output(sl, front.stack)) {
                segment.remove(front.id);
                ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.REMOVE, front.id, ItemStack.EMPTY, 0));
                sl.playSound(null, worldPosition, AutarkiaSounds.of("chute_slide"), SoundSource.BLOCKS, 0.4f, 0.7f);
                if (segment.blocked) {
                    segment.blocked = false;
                    ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.UNBLOCK, 0, ItemStack.EMPTY, 0));
                }
                blockedTicks = 0;
                changedSinceSnapshot = true;
            } else {
                blockedTicks++;
                if (!segment.blocked) {
                    segment.blocked = true;
                    ops.add(new TransitDeltaPayload.Op(TransitDeltaPayload.BLOCK, 0, ItemStack.EMPTY, 0));
                    changedSinceSnapshot = true;
                }
                if (blockedTicks == STUCK_TICKS) {
                    syncNow();
                }
            }
        }
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

    private void pullFromAbove(ServerLevel sl) {
        if (sl.getBlockEntity(worldPosition.above()) instanceof ChuteBlockEntity) {
            return;
        }
        ResourceHandler<ItemResource> above = sl.getCapability(Capabilities.Item.BLOCK, worldPosition.above(), Direction.DOWN);
        if (above == null) {
            return;
        }
        for (int i = 0; i < above.size(); i++) {
            ItemResource r = above.getResource(i);
            if (r.isEmpty() || dev.autarkia.storage.CrateBlockEntity.isFilledCrate(r.toStack(1))) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int got = above.extract(i, r, MAX_ITEMS, tx);
                if (got > 0) {
                    tx.commit();
                    accept(r.toStack(got));
                    return;
                }
            }
        }
    }

    private boolean output(ServerLevel sl, ItemStack stack) {
        BlockPos t = target();
        if (sl.getBlockEntity(t) instanceof ChuteBlockEntity next) {
            return next.accept(stack);
        }
        ResourceHandler<ItemResource> h = sl.getCapability(Capabilities.Item.BLOCK, t, Direction.UP);
        if (h == null) {
            return false;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int moved = h.insert(ItemResource.of(stack), stack.getCount(), tx);
            if (moved == stack.getCount()) {
                tx.commit();
                return true;
            }
        }
        return false;
    }

    @Override
    public void clientTick() {
        segment.move(AutarkiaConfig.CHUTE_SPEED_PU.get(), SPACING);
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
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        segment.save(out.child("segment"));
        intake.serialize(out.child("intake"));
        out.putInt("blockedTicks", blockedTicks);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("segment").ifPresent(segment::load);
        in.child("intake").ifPresent(intake::deserialize);
        blockedTicks = in.getIntOr("blockedTicks", 0);
    }
}
