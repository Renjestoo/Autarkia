package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import org.jspecify.annotations.Nullable;

/**
 * The W2 sign (05 section 4.4): when an auto-approved project with a sign key is done and signs are on, a worker puts a
 * sign from storage on the plan's sign spot, or the first free ground cell next to the anchor, facing the Core, with
 * the reason (rounded to 5), "Fixed.", the colony day and the template name.
 */
public final class Signs {
    private Signs() {
    }

    /** Return whether the project gets a sign. */
    public static boolean applies(Colony c, Project p) {
        ColonyTemplate tpl = ColonyPlans.templates().get(p.template.getPath());
        return p.auto && tpl != null && tpl.sign().isPresent() && c.signs && AutarkiaConfig.COLONY_SIGNS.get();
    }

    /** Return the sign cell, or null when no free cell exists. */
    public static @Nullable BlockPos spot(Colony c, ServerLevel level, Project p) {
        if (p.signPos != Long.MIN_VALUE && free(level, BlockPos.of(p.signPos))) {
            return BlockPos.of(p.signPos);
        }
        BlockPos anchor = BlockPos.of(p.anchor);
        for (int r = 1; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos q = anchor.offset(dx, dy, dz);
                        if (Colonies.inside(c, q) && free(level, q) && !occupiedByPlan(p, q)) {
                            return q;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean occupiedByPlan(Project p, BlockPos q) {
        for (Project.Cell cell : p.cells) {
            if (cell.pos() == q.asLong()) {
                return true;
            }
        }
        return false;
    }

    private static boolean free(ServerLevel level, BlockPos q) {
        return level.getBlockState(q).canBeReplaced() && level.getBlockState(q.below()).isFaceSturdy(level, q.below(), Direction.UP);
    }

    /** Return the oak sign state at the spot, rotated so its front faces the Core (the worker swaps in the fetched sign's block). */
    public static BlockState state(Colony c, BlockPos spot) {
        return withRotation(net.minecraft.world.level.block.Blocks.OAK_SIGN, c, spot);
    }

    /** Return the standing state of a sign block at the spot facing the Core. */
    public static BlockState withRotation(Block sign, Colony c, BlockPos spot) {
        float yaw = (float) Math.toDegrees(Mth.atan2(-(spot.getX() - c.corePos.getX()), spot.getZ() - c.corePos.getZ()));
        BlockState st = sign.defaultBlockState();
        return st.hasProperty(StandingSignBlock.ROTATION) ? st.setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(yaw + 180)) : st;
    }

    /** Write the four sign lines on a placed sign. */
    public static void write(ServerLevel level, BlockPos pos, Colony c, Project p) {
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        ColonyTemplate tpl = ColonyPlans.templates().get(p.template.getPath());
        String key = tpl == null ? "power_short" : tpl.sign().orElse("power_short");
        List<Double> args = p.reason.args();
        double raw = args.isEmpty() ? 0 : args.get(0);
        Object arg = key.equals("line_snapped") || key.equals("rack_failed") || key.equals("storm_hit") ? ColonyClock.day(c) : Math.round(raw / 5.0) * 5;
        List<Component> lines = List.of(Component.translatable("autarkia.sign." + key, arg), Component.translatable("autarkia.sign.fixed"),
            Component.translatable("autarkia.sign.day", ColonyClock.day(c)), Component.translatable("colony_template.autarkia." + p.template.getPath()));
        sign.setText(new SignText(lines, lines, net.minecraft.world.item.DyeColor.BLACK, false), net.minecraft.world.level.block.entity.SignTextSlot.FRONT);
        sign.setWaxed(true);
        sign.setChanged();
        level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_CLIENTS);
    }
}
