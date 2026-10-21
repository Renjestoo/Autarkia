package dev.autarkia.worker;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.registry.AutarkiaItems;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * The {@code worker_capsule} (06 section 5.3): sneak-use on your own idle, recharging or failed worker to store it in
 * the vanilla {@code entity_data} component (energy, health, cargo, name); use a filled capsule on the top face of a
 * block in your colony's domain to place it again. A filled capsule does not count against the cap.
 */
public class WorkerCapsuleItem extends Item {
    public WorkerCapsuleItem(Properties props) {
        super(props);
    }

    /** Return a filled capsule holding a fresh worker of the type (the Core's starter Porter). */
    public static ItemStack filled(ServerLevel level, EntityType<? extends WorkerEntity> type) {
        WorkerEntity w = type.create(level, EntitySpawnReason.TRIGGERED);
        if (w == null) {
            throw new IllegalStateException("Cannot create worker " + type);
        }
        ItemStack stack = new ItemStack(AutarkiaItems.WORKER_CAPSULE.get());
        stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(type, save(w)));
        w.discard();
        return stack;
    }

    private static CompoundTag save(Entity e) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, e.registryAccess());
        e.saveWithoutId(out);
        return out.buildResult();
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof WorkerEntity w) || stack.has(DataComponents.ENTITY_DATA) || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        Colony c = w.colony();
        if (c != null && !c.canManage(player.getUUID())) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.capsule.not_yours").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        WorkerEntity.State s = w.state();
        if (s != WorkerEntity.State.IDLE && s != WorkerEntity.State.RECHARGING && s != WorkerEntity.State.FAILED && s != WorkerEntity.State.RETURNING) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.capsule.busy").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        w.unbind();
        stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(w.getType(), save(w)));
        w.discard();
        Autarkia.LOG.info("[worker] {} captured {} at {}", player.getName().getString(), w.workerType.id().getPath(), w.blockPosition().toShortString());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
        if (data == null || ctx.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos at = ctx.getClickedPos().above();
        Colony c = Colonies.at(level, at);
        if (c == null || !c.canManage(sp.getUUID())) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.capsule.outside").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!(data.type().create(level, EntitySpawnReason.TRIGGERED) instanceof WorkerEntity w)) {
            return InteractionResult.FAIL;
        }
        if (c.workerCost() + w.workerType.capCost() > ColonyTiers.workerCap(c.tier)
            || c.workerCount(w.workerType.id()) >= w.workerType.subCap(c.tier)) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.capsule.cap_full").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        data.loadInto(w);
        w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sp.getYRot() + 180, 0);
        level.addFreshEntity(w);
        w.bind(c);
        w.trigger("unfold");
        w.sound("entity.worker.servo", 0.6f, 1.2f);
        stack.remove(DataComponents.ENTITY_DATA);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
        if (data == null) {
            out.accept(Component.translatable("tooltip.autarkia.capsule.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        int tension = data.copyTagWithoutId().getIntOr("tension", 0);
        int cap = WorkerType.of(data.type()).capacity();
        out.accept(Component.translatable("tooltip.autarkia.capsule.filled", data.type().getDescription(), tension * 100 / Math.max(1, cap))
            .withStyle(ChatFormatting.GRAY));
    }
}
