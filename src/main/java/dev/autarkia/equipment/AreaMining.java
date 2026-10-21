package dev.autarkia.equipment;

import dev.autarkia.registry.AutarkiaComponents;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Area mining for modal tools (07 section 2.2): the left click stores the hit face per player, a broken block breaks
 * the rest of the square in the plane at right angles to that face with {@code ServerPlayerGameMode.destroyBlock}
 * (only blocks the tool may drop, with hardness at most the center hardness + 2), and the area mode scales the mining
 * speed. A guard stops the extra breaks from starting their own area break.
 */
public final class AreaMining {
    private static final Map<UUID, Direction> FACE = new HashMap<>();
    private static boolean busy;

    private AreaMining() {
    }

    /** Register the event listeners (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> {
            if (e.getFace() != null) {
                remember(e.getEntity(), e.getFace());
            }
        });
        NeoForge.EVENT_BUS.addListener(AreaMining::onBreak);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.BreakSpeed e) -> {
            ItemStack held = e.getEntity().getMainHandItem();
            ToolMode mode = held.get(AutarkiaComponents.TOOL_MODE.get());
            if (held.getItem() instanceof ModalItem m && mode != null) {
                e.setNewSpeed(e.getNewSpeed() * m.areaSpeed(mode));
            }
        });
    }

    /** Store the face a player hit last; the next area break uses its plane. */
    public static void remember(net.minecraft.world.entity.player.Player player, Direction face) {
        FACE.put(player.getUUID(), face);
    }

    /** Return the square radius of a mode (0 for single-block modes). */
    public static int radius(ToolMode mode) {
        return mode == ToolMode.AREA3 ? 1 : mode == ToolMode.AREA5 ? 2 : 0;
    }

    private static void onBreak(BreakBlockEvent e) {
        if (busy || !(e.getPlayer() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel level) || e.isCanceled()) {
            return;
        }
        ItemStack held = sp.getMainHandItem();
        ToolMode mode = held.get(AutarkiaComponents.TOOL_MODE.get());
        int r = mode == null || !(held.getItem() instanceof ModalItem) ? 0 : radius(mode);
        if (r == 0) {
            return;
        }
        Direction face = FACE.getOrDefault(sp.getUUID(), Direction.UP);
        BlockPos center = e.getPos();
        float hardness = e.getState().getDestroySpeed(level, center);
        busy = true;
        try {
            for (int a = -r; a <= r; a++) {
                for (int b = -r; b <= r; b++) {
                    if (a == 0 && b == 0) {
                        continue;
                    }
                    BlockPos p = switch (face.getAxis()) {
                        case X -> center.offset(0, a, b);
                        case Y -> center.offset(a, 0, b);
                        case Z -> center.offset(a, b, 0);
                    };
                    BlockState st = level.getBlockState(p);
                    float h = st.getDestroySpeed(level, p);
                    if (st.isAir() || h < 0 || h > hardness + 2 || !held.isCorrectToolForDrops(st)) {
                        continue;
                    }
                    sp.gameMode.destroyBlock(p);
                    if (sp.getMainHandItem() != held || held.isEmpty()) {
                        return;
                    }
                }
            }
        } finally {
            busy = false;
        }
    }
}
