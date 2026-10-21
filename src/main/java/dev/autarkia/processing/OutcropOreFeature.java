package dev.autarkia.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Feature type {@code autarkia:outcrop_ore} (03 section 2.5): a {@code minecraft:ore} vein that also marks itself with
 * an {@code ore_outcrop[ore=<ore>]} on the surface of its column when it placed at least {@code min_blocks} target-state
 * blocks (every state of the targets list counts) in the cube of radius min(16, ceil(size / 2)) around its origin, and
 * the surface ({@code OCEAN_FLOOR_WG}) is at most {@code max_depth} blocks above the origin. The surface block must be
 * air, water or {@code #minecraft:replaceable} on a sturdy top face; in water the outcrop is waterlogged. With config
 * {@code worldgen.outcropsEnabled} false it only places the vein. It returns true when the vein placed a block.
 */
public final class OutcropOreFeature implements Feature {
    /** JSON form: the three {@code minecraft:ore} fields plus {@code ore}, {@code min_blocks} and {@code max_depth} (default 128). */
    public static final MapCodec<OutcropOreFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BlockReplacement.CODEC.listOf().fieldOf("targets").forGetter(f -> f.targets),
        Codec.intRange(0, 64).fieldOf("size").forGetter(f -> f.size),
        Codec.floatRange(0, 1).fieldOf("discard_chance_on_air_exposure").forGetter(f -> f.discard),
        OreOutcropBlock.Ore.CODEC.fieldOf("ore").forGetter(f -> f.ore),
        Codec.intRange(0, 4096).fieldOf("min_blocks").forGetter(f -> f.minBlocks),
        Codec.intRange(0, 4096).optionalFieldOf("max_depth", 128).forGetter(f -> f.maxDepth)).apply(i, OutcropOreFeature::new));
    /** The feature type register of Autarkia ({@code Registries.FEATURE_TYPE}). */
    public static final DeferredRegister<MapCodec<? extends Feature>> TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, Autarkia.MOD_ID);
    /** The {@code autarkia:outcrop_ore} feature type. */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<OutcropOreFeature>> TYPE = TYPES.register("outcrop_ore", () -> CODEC);

    private final List<BlockReplacement> targets;
    private final int size;
    private final float discard;
    private final OreOutcropBlock.Ore ore;
    private final int minBlocks;
    private final int maxDepth;
    private final OreFeature vein;

    public OutcropOreFeature(List<BlockReplacement> targets, int size, float discard, OreOutcropBlock.Ore ore, int minBlocks, int maxDepth) {
        this.targets = targets;
        this.size = size;
        this.discard = discard;
        this.ore = ore;
        this.minBlocks = minBlocks;
        this.maxDepth = maxDepth;
        this.vein = new OreFeature(targets, size, discard);
    }

    @Override
    public MapCodec<OutcropOreFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!AutarkiaConfig.WORLDGEN_OUTCROPS_ENABLED.get()) {
            return vein.place(level, generator, random, origin);
        }
        int radius = Math.min(16, (size + 1) / 2);
        int before = countTargets(level, origin, radius);
        boolean placedAny = vein.place(level, generator, random, origin);
        int placed = countTargets(level, origin, radius) - before;
        if (placed < minBlocks) {
            return placedAny;
        }
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (y - origin.getY() > maxDepth) {
            return placedAny;
        }
        BlockPos at = new BlockPos(origin.getX(), y, origin.getZ());
        BlockPos below = at.below();
        BlockState there = level.getBlockState(at);
        boolean water = level.getFluidState(at).isSourceOfType(Fluids.WATER);
        if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && (there.isAir() || there.is(Blocks.WATER) || there.is(BlockTags.REPLACEABLE))) {
            setBlock(level, at, AutarkiaBlocks.ORE_OUTCROP.get().defaultBlockState().setValue(OreOutcropBlock.ORE, ore).setValue(OreOutcropBlock.WATERLOGGED, water));
        }
        return placedAny;
    }

    private int countTargets(WorldGenLevel level, BlockPos origin, int radius) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (level.isOutsideBuildHeight(p)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            for (BlockReplacement t : targets) {
                if (s == t.state()) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }
}
