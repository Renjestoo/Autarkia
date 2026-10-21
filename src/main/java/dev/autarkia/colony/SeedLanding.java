package dev.autarkia.colony;

import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.integration.AnthologyBridge;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.SeedUnpackPayload;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * The landing of a thrown Autarky Seed (05 section 8.4, C3, C5, G10). The seed belongs to the nearest colony in the
 * dimension that the thrower manages (owner or trusted). The footprint is centred on the landing column, rotated by the
 * thrower's facing, with the floor at the median surface height of its columns. The checks, before any block changes:
 * seed contents and its Patterns, {@code seed.enabled}, a tier 6 colony, every footprint chunk loaded, the outpost rules
 * of the core position (free outpost slot, 64 to {@code outpost.maxDistance} from the Core, {@code outpost.minSpacing}
 * from other outposts, no overlap with another colony), no block entity in the footprint volume, fluid under 50% of the
 * columns, and no protection cancelling the clearing of the core space. Then the outpost core of Pattern 1 is planted
 * with the plan rotation, the outpost and its chunk ticket are registered, the unpack state goes into the core, the
 * players near get {@code seed_unpack}, and {@code seed_unpack_wave} plays with a landing shake (0.5, 20 ticks, 24).
 */
public final class SeedLanding {
    public static final double PAYLOAD_RANGE = 160;
    private static @Nullable String lastFailure;

    private SeedLanding() {
    }

    /** Return the translation key of the last failed landing on this server, or null after a success (diagnostics). */
    public static @Nullable String lastFailure() {
        return lastFailure;
    }

    /** Return the nearest colony in the level's dimension that the player manages, or null. */
    public static @Nullable Colony colonyOf(ServerLevel level, UUID player, BlockPos at) {
        Colony best = null;
        for (Colony c : Colonies.registry().all()) {
            if (c.dimension.equals(level.dimension()) && c.canManage(player)
                && (best == null || Outposts.horizontal(c.corePos, at) < Outposts.horizontal(best.corePos, at))) {
                best = c;
            }
        }
        return best;
    }

    /**
     * Land a seed stack at the column of {@code at}; returns null on success, else the translation key of the reason
     * (no block has changed then, except when protection refused the core space after it was found free).
     */
    public static @Nullable String land(ServerLevel level, ItemStack stack, UUID thrower, Direction facing, BlockPos at) {
        String why = tryLand(level, stack, thrower, facing, at);
        lastFailure = why;
        if (why != null) {
            Autarkia.LOG.info("[colony] seed landing at {} failed: {}", at.toShortString(), why);
        }
        return why;
    }

    private static @Nullable String tryLand(ServerLevel level, ItemStack stack, UUID thrower, Direction facing, BlockPos at) {
        SeedContents contents = stack.get(AutarkiaComponents.SEED_CONTENTS.get());
        if (contents == null || contents.entries().isEmpty()) {
            return "message.autarkia.seed.empty";
        }
        if (!AutarkiaConfig.SEED_ENABLED.get()) {
            return "message.autarkia.seed.disabled";
        }
        Colony c = colonyOf(level, thrower, at);
        if (c == null) {
            return "message.autarkia.outpost.no_colony";
        }
        if (c.tier < 6 || c.state != Colony.State.ACTIVE) {
            return "message.autarkia.seed.tier";
        }
        SeedLayout.Result r = SeedLayout.check(level.getServer(), contents.entries(), ColonyTiers.outpostHalf(c.tier));
        if (r.problem() == SeedLayout.Problem.NO_PATTERN || r.problem() == SeedLayout.Problem.NOT_OUTPOST) {
            return "message.autarkia.seed.patterns";
        }
        if (r.problem() != SeedLayout.Problem.NONE) {
            return "message.autarkia.seed.layout";
        }
        SeedLayout.World w0 = SeedLayout.place(r, at.getX(), 0, at.getZ(), SeedLayout.facing(facing));
        BlockPos lo = w0.min();
        BlockPos hi = w0.max();
        for (int cx = lo.getX() >> 4; cx <= hi.getX() >> 4; cx++) {
            for (int cz = lo.getZ() >> 4; cz <= hi.getZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return "message.autarkia.seed.chunks";
                }
            }
        }
        int[] heights = new int[w0.size().getX() * w0.size().getZ()];
        int k = 0;
        int fluid = 0;
        int lowest = Integer.MAX_VALUE;
        for (int x = lo.getX(); x <= hi.getX(); x++) {
            for (int z = lo.getZ(); z <= hi.getZ(); z++) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                heights[k++] = h;
                lowest = Math.min(lowest, h);
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                fluid += level.getBlockState(new BlockPos(x, surface, z)).getFluidState().isEmpty() ? 0 : 1;
            }
        }
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        int floor = sorted[(sorted.length - 1) / 2];
        SeedLayout.World w = SeedLayout.place(r, at.getX(), floor, at.getZ(), SeedLayout.facing(facing));
        BlockPos core = SeedLayout.coreWorld(r, w);
        String rule = Outposts.invalid(level, c, core);
        if (rule != null) {
            return rule;
        }
        if (blockEntityIn(level, w, Math.min(lowest + 1, floor + 1))) {
            return "message.autarkia.seed.block_entity";
        }
        if (fluid * 2 >= heights.length) {
            return "message.autarkia.seed.fluid";
        }
        BlockState coreState = SeedLayout.coreState(r, w.rotation()).setValue(BaseMultiblock.CENTER, true);
        Block coreBlock = AutarkiaBlocks.OUTPOST_CORE.get();
        Set<BlockPos> space = ((BaseMultiblock) coreBlock).getMultiblockShape(level, core, coreState).getGlobalPositions();
        net.neoforged.neoforge.common.util.FakePlayer owner = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new GameProfile(c.owner, "[autarkia]"));
        for (BlockPos p : space) {
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && !st.canBeReplaced() && NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, p, st, owner)).isCanceled()) {
                return "message.autarkia.seed.protected";
            }
        }
        long material = 0;
        for (BlockPos p : space) {
            BlockState st = level.getBlockState(p);
            if (!st.isAir()) {
                material += st.canBeReplaced() ? 0 : 1;
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (!Outposts.plant(level, c, core, coreState, null) || !(level.getBlockEntity(core) instanceof OutpostCoreBlockEntity planted)) {
            return "message.autarkia.outpost.blocked";
        }
        Outposts.ticket(level, core, true);
        planted.startSeed(new SeedUnpack(contents, w, level.getGameTime(), material));
        Vec3 center = new Vec3(w.min().getX() + w.size().getX() / 2.0, w.min().getY(), w.min().getZ() + w.size().getZ() / 2.0);
        AutarkiaNetwork.toNear(level, center, PAYLOAD_RANGE, new SeedUnpackPayload(BlockPos.containing(center), w.min(), w.max(), level.getGameTime()));
        level.playSound(null, center.x, center.y, center.z, AutarkiaSounds.of("seed_unpack_wave"), SoundSource.BLOCKS, 1f, 1f);
        AnthologyBridge.shake(level, center, 0.5F, 20, 24F);
        ColonyRuntime.of(c).log("Autarky Seed landed at " + core.toShortString());
        Autarkia.LOG.info("[colony] {} seed landed: core {}, footprint {} to {}, floor {}, {} blocks", c.name, core.toShortString(), w.min().toShortString(),
            w.max().toShortString(), floor, r.blocks());
        return null;
    }

    private static boolean blockEntityIn(ServerLevel level, SeedLayout.World w, int bottom) {
        BlockPos lo = w.min();
        BlockPos hi = w.max();
        for (int cx = lo.getX() >> 4; cx <= hi.getX() >> 4; cx++) {
            for (int cz = lo.getZ() >> 4; cz <= hi.getZ() >> 4; cz++) {
                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) {
                    return true;
                }
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (p.getX() >= lo.getX() && p.getX() <= hi.getX() && p.getZ() >= lo.getZ() && p.getZ() <= hi.getZ() && p.getY() >= bottom
                        && p.getY() <= hi.getY()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
