package dev.autarkia.command;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /autarkia test m7}: the M7 registry data, then the materials ({@link TestM7Materials}), power
 * ({@link TestM7Power}), equipment ({@link TestM7Equipment}), logistics ({@link TestM7Logistics}), workers
 * ({@link TestM7Workers}) and colony ({@link TestM7Colony}, its own strip at x 424 to 716) parts on a platform at y 120
 * or higher (x -2 to 160, z -2 to 280, air from y 0 to 24 over a stone floor at y -1; the Surveyor's radius-64 spiral
 * around x 90, z 210 stays over it). Only x -8 to 136, z -8 to 248 is force-loaded, so the outer spiral points need
 * travel tickets.
 */
public final class TestM7 {
    private static final List<String> SECTIONS = List.of("all", "materials", "power", "equipment", "logistics", "workers", "colony");
    private static final int SLAB = 22;

    private TestM7() {
    }

    /** One M7 run: the test and the platform base. */
    record Run(SelfTest t, BlockPos base) {
        BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }
    }

    /** Build the platform and run the whole M7 script. */
    public static int run(CommandSourceStack src) {
        return run(src, "all");
    }

    /**
     * Build the platform and run the M7 script from a section to the end: {@code all}, {@code materials}, {@code power},
     * {@code equipment}, {@code logistics}, {@code workers} or {@code colony}. An unknown section fails the command
     * without a run.
     */
    public static int run(CommandSourceStack src, String from) {
        int start = SECTIONS.indexOf(from);
        if (start < 0) {
            src.sendFailure(Component.literal("Unknown M7 section '" + from + "'; use one of " + String.join(", ", SECTIONS)));
            return 0;
        }
        SelfTest t = SelfTest.create("m7", src);
        BlockPos o = t.origin;
        Run r = new Run(t, new BlockPos(o.getX(), Math.max(o.getY() + 8, 120), o.getZ()));
        reset(r);
        data(r);
        if (start <= 1) {
            TestM7Materials.run(r);
        }
        if (start <= 2) {
            TestM7Power.run(r);
        }
        if (start <= 3) {
            TestM7Equipment.run(r);
        }
        if (start <= 4) {
            TestM7Logistics.run(r);
        }
        if (start <= 5) {
            TestM7Workers.run(r);
        }
        TestM7Colony.run(r);
        return t.start();
    }

    static void box(Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t().level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void reset(Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            x.command("forceload remove " + (r.base().getX() - 8) + " " + (r.base().getZ() - 8) + " " + (r.base().getX() + 166) + " "
                + (r.base().getZ() + 288));
            x.command("forceload add " + (r.base().getX() - 8) + " " + (r.base().getZ() - 8) + " " + (r.base().getX() + 136) + " "
                + (r.base().getZ() + 248));
            x.command("weather clear");
            x.command("gamerule doMobSpawning false");
            AABB area = new AABB(Vec3.atCenterOf(r.at(-8, -60, -8)), Vec3.atCenterOf(r.at(166, 40, 288)));
            x.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(e -> e.discard());
            SelfTest.clearSpans(x.level, area);
        });
        for (int z0 = -2; z0 <= 280; z0 += SLAB) {
            int a = z0;
            int b = Math.min(280, z0 + SLAB - 1);
            t.then(x -> {
                box(r, -2, 0, a, 160, 24, b, Blocks.AIR.defaultBlockState());
                box(r, -2, -1, a, 160, -1, b, Blocks.STONE.defaultBlockState());
            });
        }
        t.then(x -> Autarkia.LOG.info("[TEST m7] platform at {}", r.base().toShortString()));
    }

    private static void data(Run r) {
        SelfTest t = r.t();
        t.check("data: the M7 blocks, items, block entities, grapple hook, flotation menu and recipe type, and rail damage register", () -> {
            for (String id : List.of("vantite_ore", "raw_vantite_block", "vantite_block", "vantite_steel_block", "ore_outcrop", "magma_vent",
                "froth_flotation_plant", "hv_pylon", "hydro_turbine", "wind_tower", "magma_tap")) {
                if (!BuiltInRegistries.BLOCK.containsKey(Autarkia.id(id)) || !BuiltInRegistries.ITEM.containsKey(Autarkia.id(id))) {
                    Autarkia.LOG.info("[TEST m7] missing block or block item {}", id);
                    return false;
                }
            }
            for (String id : List.of("raw_vantite", "vantite_ingot", "vantite_steel_ingot", "crushed_vantite", "washed_vantite", "pure_vantite",
                "iron_concentrate", "copper_concentrate", "gold_concentrate", "tin_concentrate", "zinc_concentrate", "vantite_concentrate",
                "advanced_circuit", "hv_cable_spool", "vantite_excavator", "grapple_launcher", "vantite_blade", "frontier_suit_helmet",
                "frontier_suit_chest", "frontier_suit_legs", "frontier_suit_boots", "rail_rifle")) {
                if (!BuiltInRegistries.ITEM.containsKey(Autarkia.id(id))) {
                    Autarkia.LOG.info("[TEST m7] missing item {}", id);
                    return false;
                }
            }
            for (String id : List.of("froth_flotation_plant", "hv_pylon", "hydro_turbine", "wind_tower", "magma_tap")) {
                if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(Autarkia.id(id))) {
                    Autarkia.LOG.info("[TEST m7] missing block entity type {}", id);
                    return false;
                }
            }
            return BuiltInRegistries.ENTITY_TYPE.containsKey(Autarkia.id("grapple_hook")) && BuiltInRegistries.MENU.containsKey(Autarkia.id("froth_flotation_plant"))
                && BuiltInRegistries.RECIPE_TYPE.containsKey(Autarkia.id("flotation"))
                && t.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).containsKey(Autarkia.id("rail"));
        }, () -> "11 blocks, 33 items, 5 block entity types, 1 entity type, 1 menu, 1 recipe type, 1 damage type");
    }
}
