package dev.autarkia.command;

import dev.autarkia.colony.Builds;
import dev.autarkia.colony.Pattern;
import dev.autarkia.colony.RisingBlockEntity;
import dev.autarkia.colony.SeedMatrixBlockEntity;
import dev.autarkia.energy.CableTier;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.SeedUnpackPayload;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;

/**
 * The seed piece of {@code /autarkia showcase m9} (20 x 20 blocks from the origin): a {@code seed_matrix} (idle loop)
 * holding the three demo Pattern cards with a full energy buffer, and beside it a 7 x 7 seed pad whose blocks rise
 * from the soil as RISE {@code rising_block}s under the seed glow ring and leveling sweep. The demo Patterns (an OUTPOST
 * Pattern with an outpost core, a yard with a copper span between two wooden poles, and a tower) are also the Patterns
 * of the W21 self test.
 */
public final class ShowcaseM9Seed {
    private ShowcaseM9Seed() {
    }

    /** Build the seed piece at the origin and start its rising pad. */
    public static void buildSeed(ServerLevel level, BlockPos origin) {
        BlockPos matrix = origin.offset(4, 0, 4);
        if (TestM1.placeBigAt(level, matrix, AutarkiaBlocks.SEED_MATRIX.get(), Direction.SOUTH)
            && level.getBlockEntity(matrix) instanceof SeedMatrixBlockEntity m) {
            List<Pattern> demo = demoPatterns(level, new UUID(0, 0));
            for (int i = 0; i < demo.size(); i++) {
                m.inventory().put(i, card(demo.get(i)));
            }
            m.energy().fill(m.energy().room());
        }
        BlockPos pad = origin.offset(10, 0, 10);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                boolean rim = x == 0 || z == 0 || x == 6 || z == 6;
                RisingBlockEntity.rise(level, pad.offset(x, 0, z), rim ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                if (rim && (x + z) % 3 == 0) {
                    RisingBlockEntity.rise(level, pad.offset(x, 1, z), Blocks.SEA_LANTERN.defaultBlockState());
                }
            }
        }
        BlockPos max = pad.offset(6, 1, 6);
        AutarkiaNetwork.toNear(level, Vec3.atCenterOf(pad.offset(3, 0, 3)), 96, new SeedUnpackPayload(pad.offset(3, 0, 3), pad, max, level.getGameTime()));
    }

    /** Return a pattern card that references the Pattern. */
    static ItemStack card(Pattern p) {
        ItemStack card = new ItemStack(AutarkiaItems.PATTERN_CARD.get());
        card.set(AutarkiaComponents.PATTERN_REF.get(), Pattern.Ref.of(p, p.blocks()));
        return card;
    }

    /**
     * Record and return the three demo Patterns: "Seed Outpost" 21 x 4 x 21 (role outpost: stone brick floor, outpost
     * core at (10, 1, 10), a rim, corner pillars with sea lanterns), "Seed Yard" 21 x 4 x 21 (smooth stone floor, two
     * wooden poles 12 apart with a copper span) and "Seed Tower" 11 x 12 x 11 (hollow polished andesite tower with a roof).
     */
    static List<Pattern> demoPatterns(ServerLevel level, UUID author) {
        List<Pattern> out = new ArrayList<>();
        Map<BlockPos, BlockState> a = new LinkedHashMap<>();
        floor(a, 21, 21, Blocks.STONE_BRICKS.defaultBlockState());
        for (int i = 0; i < 21; i++) {
            for (int[] c : new int[][] {{i, 0}, {i, 20}, {0, i}, {20, i}}) {
                a.put(new BlockPos(c[0], 1, c[1]), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        for (int[] c : new int[][] {{0, 0}, {0, 20}, {20, 0}, {20, 20}}) {
            a.put(new BlockPos(c[0], 2, c[1]), Blocks.POLISHED_ANDESITE.defaultBlockState());
            a.put(new BlockPos(c[0], 3, c[1]), Blocks.SEA_LANTERN.defaultBlockState());
        }
        a.put(new BlockPos(10, 1, 10), AutarkiaBlocks.OUTPOST_CORE.get().defaultBlockState().setValue(BaseMultiblock.CENTER, true)
            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        out.add(pattern(level, "Seed Outpost", author, new Vec3i(21, 4, 21), "outpost", a, List.of()));
        Map<BlockPos, BlockState> b = new LinkedHashMap<>();
        floor(b, 21, 21, Blocks.SMOOTH_STONE.defaultBlockState());
        BlockState pole = AutarkiaBlocks.WOODEN_POLE.get().defaultBlockState().setValue(BaseMultiblock.CENTER, true);
        b.put(new BlockPos(4, 1, 10), pole);
        b.put(new BlockPos(16, 1, 10), pole);
        out.add(pattern(level, "Seed Yard", author, new Vec3i(21, 4, 21), "generic", b,
            List.of(new Pattern.SpanRecord(new BlockPos(4, 1, 10), new BlockPos(16, 1, 10), CableTier.COPPER))));
        Map<BlockPos, BlockState> c = new LinkedHashMap<>();
        floor(c, 11, 11, Blocks.POLISHED_ANDESITE.defaultBlockState());
        for (int y = 1; y <= 10; y++) {
            for (int i = 0; i < 11; i++) {
                for (int[] q : new int[][] {{i, 0}, {i, 10}, {0, i}, {10, i}}) {
                    c.put(new BlockPos(q[0], y, q[1]), Blocks.POLISHED_ANDESITE.defaultBlockState());
                }
            }
        }
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                c.put(new BlockPos(x, 11, z), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        out.add(pattern(level, "Seed Tower", author, new Vec3i(11, 12, 11), "generic", c, List.of()));
        return out;
    }

    private static void floor(Map<BlockPos, BlockState> cells, int sx, int sz, BlockState s) {
        for (int x = 0; x < sx; x++) {
            for (int z = 0; z < sz; z++) {
                cells.put(new BlockPos(x, 0, z), s);
            }
        }
    }

    private static Pattern pattern(ServerLevel level, String name, UUID author, Vec3i size, String role, Map<BlockPos, BlockState> cells,
                                   List<Pattern.SpanRecord> spans) {
        List<BlockState> palette = new ArrayList<>(List.of(Blocks.AIR.defaultBlockState()));
        int[] idx = new int[size.getX() * size.getY() * size.getZ()];
        Map<Item, Integer> bom = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : cells.entrySet()) {
            int k = palette.indexOf(e.getValue());
            if (k < 0) {
                palette.add(e.getValue());
                k = palette.size() - 1;
            }
            BlockPos p = e.getKey();
            idx[p.getX() + size.getX() * (p.getZ() + size.getZ() * p.getY())] = k;
            bom.merge(Builds.itemFor(e.getValue()), 1, Integer::sum);
        }
        for (Pattern.SpanRecord s : spans) {
            bom.merge(dev.autarkia.energy.Grid.cableItem(s.tier()), s.tier().itemCost(Math.sqrt(s.a().distSqr(s.b()))), Integer::sum);
        }
        int bits = Pattern.bitsFor(palette.size());
        Pattern p = new Pattern(UUID.randomUUID(), name, author, level.getGameTime(), size, List.copyOf(palette), Pattern.pack(idx, bits), bits,
            List.of(), List.of(), Map.of(), List.copyOf(spans), List.of(), Map.copyOf(bom), java.util.Arrays.hashCode(idx), role);
        Pattern.store(level.getServer(), p);
        return p;
    }
}
