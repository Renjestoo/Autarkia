package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;

/**
 * The {@code core_seed} (05 section 1.1, W1): used on the top face of dirt or overworld stone it checks the ground,
 * the free 3x3x3 volume, the distance to every domain (no overlap) and the per-player colony count, then places the
 * Colony Core in state GROWING. A seed with {@code autarkia:colony_link} restores its dormant colony instead.
 */
public class CoreSeedItem extends Item {
    public CoreSeedItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        String fail = check(ctx, level, player);
        if (fail != null) {
            Autarkia.LOG.info("[colony] {} could not plant a Core at {}: {}", player.getName().getString(), ctx.getClickedPos().toShortString(), fail);
            player.sendOverlayMessage(Component.translatable("message.autarkia.seed." + fail).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        BlockPos center = ctx.getClickedPos().above();
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockState state = AutarkiaBlocks.COLONY_CORE.get().defaultBlockState().setValue(BaseMultiblock.CENTER, true)
            .setValue(HorizontalDirectionalBlock.FACING, facing);
        if (!AutarkiaBlocks.COLONY_CORE.get().canPlace(level, center, state, null, false)) {
            Autarkia.LOG.info("[colony] {} could not plant a Core at {}: the Core shape does not fit", player.getName().getString(), center.toShortString());
            player.sendOverlayMessage(Component.translatable("message.autarkia.seed.blocked").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        UUID link = ctx.getItemInHand().get(AutarkiaComponents.COLONY_LINK.get());
        Colony c = link == null ? null : Colonies.registry().get(link);
        if (c == null) {
            c = Colony.create(player.getUUID(), player.getName().getString() + "'s Colony", level.dimension(), center);
            Colonies.registry().add(c);
        } else {
            c.corePos = center;
            c.growth = 0;
        }
        c.state = Colony.State.GROWING;
        level.setBlock(center, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(center) instanceof ColonyCoreBlockEntity core) {
            core.link(c);
        }
        Colonies.rebuildIndex();
        Colonies.changed();
        ctx.getItemInHand().consume(1, player);
        Autarkia.LOG.info("[colony] {} planted a Core at {} ({})", player.getName().getString(), center.toShortString(), link == null ? "new" : "relink");
        return InteractionResult.SUCCESS;
    }

    private static String check(UseOnContext ctx, ServerLevel level, ServerPlayer player) {
        if (ctx.getClickedFace() != Direction.UP) {
            return "top_face";
        }
        BlockPos ground = ctx.getClickedPos();
        BlockState gs = level.getBlockState(ground);
        if (!gs.is(BlockTags.SUBSTRATE_OVERWORLD) && !gs.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return "ground_type";
        }
        BlockPos center = ground.above();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos below = ground.offset(dx, 0, dz);
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    return "ground";
                }
                for (int dy = 0; dy <= 2; dy++) {
                    if (!level.getBlockState(center.offset(dx, dy, dz)).canBeReplaced()) {
                        return "blocked";
                    }
                }
            }
        }
        UUID link = ctx.getItemInHand().get(AutarkiaComponents.COLONY_LINK.get());
        int half = ColonyTiers.domainHalf(link != null && Colonies.registry().get(link) != null ? Colonies.registry().get(link).tier : 1);
        for (Colony other : Colonies.registry().all()) {
            if (other.id.equals(link) || !other.dimension.equals(level.dimension())) {
                continue;
            }
            int h = ColonyTiers.domainHalf(other.tier);
            int d = Math.max(Math.abs(center.getX() - other.corePos.getX()), Math.abs(center.getZ() - other.corePos.getZ())) - h;
            if (d <= half || d < 16) {
                return d < 0 ? "inside_domain" : "near_domain";
            }
        }
        if (link == null && Colonies.ownedBy(player.getUUID()).size() >= AutarkiaConfig.COLONY_MAX_COLONIES_PER_PLAYER.get()) {
            return "too_many";
        }
        if (link != null) {
            Colony c = Colonies.registry().get(link);
            if (c == null) {
                return "lost_link";
            }
            if (c.state != Colony.State.DORMANT) {
                return "link_active";
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        UUID link = stack.get(AutarkiaComponents.COLONY_LINK.get());
        out.accept(Component.translatable(link == null ? "tooltip.autarkia.core_seed" : "tooltip.autarkia.core_seed.linked").withStyle(ChatFormatting.GRAY));
    }
}
