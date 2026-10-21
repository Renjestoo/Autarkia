package dev.autarkia.equipment;

import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyEvents;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.DomainPowerIndex;
import dev.autarkia.registry.EventTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * {@code bastion_block} (07 section 3.4, decisions C8, C11, G14). Powered while it lies in a colony domain whose grid has
 * an ONLINE {@code singularity_core} ({@link DomainPowerIndex#poweredColony}, so only the own colony's grid powers it)
 * and that grid stores at least {@code bastion.feReserve}; nothing is paid per hit. Powered: survival players make no
 * break progress (creative still breaks), explosion resistance {@link #POWERED_RESISTANCE}, entities cannot destroy it
 * ({@code canEntityDestroy} false; a Wither that tries fires {@code bastion_wither} for the players within 32 blocks and
 * the online members of that colony) and {@link LivingDestroyBlockEvent} is cancelled. Unpowered it behaves like
 * obsidian (hardness 50, resistance 1,200, diamond tool). Pistons never move it (obsidian is immovable as well). The
 * {@code disassembler_beam} never touches it (its id and {@code #autarkia:disassembler_immune}).
 */
public class BastionBlock extends Block {
    public static final float POWERED_RESISTANCE = 3_600_000.0F;
    public static final float UNPOWERED_RESISTANCE = 1200.0F;

    public BastionBlock(Properties props) {
        super(props);
    }

    /** Register the entity block-destroy listener (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((LivingDestroyBlockEvent e) -> {
            if (bastion(e.getState()) && e.getEntity().level() instanceof ServerLevel sl && powered(sl, e.getPos())) {
                e.setCanceled(true);
            }
        });
    }

    /** Return true for a {@code bastion_block} or {@code bastion_glass} state. */
    public static boolean bastion(BlockState state) {
        return state.getBlock() instanceof BastionBlock || state.getBlock() instanceof BastionGlassBlock;
    }

    /** Return the colony whose grid protects a Bastion block at pos, or null when it is unpowered (reads the power index only). */
    public static @Nullable Colony protector(ServerLevel level, BlockPos pos) {
        Colony c = DomainPowerIndex.poweredColony(level, pos);
        return c != null && DomainPowerIndex.gridStored(level, c) >= AutarkiaConfig.BASTION_FE_RESERVE.get() ? c : null;
    }

    /** Return true while a Bastion block at pos is powered. */
    public static boolean powered(ServerLevel level, BlockPos pos) {
        return protector(level, pos) != null;
    }

    private static boolean powered(BlockGetter level, BlockPos pos) {
        return level instanceof ServerLevel sl && powered(sl, pos);
    }

    static float progress(BlockGetter level, BlockPos pos, Player player, float vanilla) {
        return !player.isCreative() && powered(level, pos) ? 0.0F : vanilla;
    }

    static float resistance(BlockGetter level, BlockPos pos) {
        return powered(level, pos) ? POWERED_RESISTANCE : UNPOWERED_RESISTANCE;
    }

    static boolean entityMayDestroy(BlockGetter level, BlockPos pos, Entity entity, boolean vanilla) {
        if (!(level instanceof ServerLevel sl)) {
            return vanilla;
        }
        Colony c = protector(sl, pos);
        if (c == null) {
            return vanilla;
        }
        if (entity instanceof WitherBoss) {
            witherRefused(sl, pos, c);
        }
        return false;
    }

    private static void witherRefused(ServerLevel level, BlockPos pos, Colony c) {
        level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(32)).forEach(p -> EventTrigger.EQUIPMENT.get().fire(p, "bastion_wither"));
        ColonyEvents.equipment(c, "bastion_wither");
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return progress(level, pos, player, super.getDestroyProgress(state, player, level, pos));
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return resistance(level, pos);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return entityMayDestroy(level, pos, entity, super.canEntityDestroy(state, level, pos, entity));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.IMMOVEABLE;
    }
}
