package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.processing.OreOutcropBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * The T4 colony signals (05 section 2.2) and the template 21 to 26 signals: {@code S11.<item>} available / target per
 * stock target; {@code S16} known deposits; {@code S22} hostile mobs in the domain (EMA); {@code S23} colony members
 * destroyed by mobs or explosions in the last colony day; {@code S25.<outpost>} items carried by hand between an outpost
 * depot and the main site in the last colony day; {@code net.hv} 1 when a domain network carries HV;
 * {@code water.column} the tallest falling water column in the domain (blocks, sampled every 5th period);
 * {@code outpost.need} 1 when an unclaimed deposit 16 or more blocks beyond the domain edge and at most
 * {@code outpost.maxDistance} from the Core has its metal below half its stock target (target 64 ingots when none is
 * set); {@code outpost.room} free outpost slots; {@code outpost.link_need} 1 when an outpost with an HV pylon has no HV
 * link; {@code outpost.link_load} the highest link span load of the HV-linked outposts; {@code cable_car.need} the
 * highest S25 of an outpost without a cable car that has a free cable car route (depot at most 128 blocks from the
 * main storage).
 */
public final class FrontierSignals {
    private static final String DAMAGE = "S23.events";
    static final int DEFAULT_TARGET = 64;

    private FrontierSignals() {
    }

    /** Register the member damage listeners (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ExplosionEvent.Detonate e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                e.getAffectedBlocks().forEach(p -> damaged(sl, p));
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDestroyBlockEvent e) -> {
            if (e.getEntity().level() instanceof ServerLevel sl) {
                damaged(sl, e.getPos());
            }
        });
    }

    private static void damaged(ServerLevel level, BlockPos pos) {
        if (Colonies.server() == null || !(level.getBlockEntity(pos) instanceof ColonySensors.Member)) {
            return;
        }
        Colony c = Colonies.at(level, pos);
        if (c != null) {
            c.peaks.computeIfAbsent(DAMAGE, k -> new ArrayList<>()).add((double) c.clock);
            List<BlockPos> edge = perimeter(c);
            int best = 0;
            for (int i = 1; i < edge.size(); i++) {
                best = OutpostProjects.horizontal(edge.get(i), pos) < OutpostProjects.horizontal(edge.get(best), pos) ? i : best;
            }
            c.stat("S23.edge." + best, 1);
            Colonies.changed();
        }
    }

    /**
     * Return the 8 perimeter points of the Core square (corners and edge middles, 6 blocks inside the edge) in the order
     * north-west, north, north-east, east, south-east, south, south-west, west; S23 events count toward the nearest one.
     */
    public static List<BlockPos> perimeter(Colony c) {
        int h = ColonyTiers.domainHalf(c.tier) - 6;
        int[][] d = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        List<BlockPos> out = new ArrayList<>();
        for (int[] o : d) {
            out.add(c.corePos.offset(o[0] * h, 0, o[1] * h));
        }
        return out;
    }

    /** Count items a worker carried by hand from one site to the other (outpost depot and main site) toward S25. */
    public static void carried(Colony c, BlockPos from, BlockPos to, int count) {
        Colony.Outpost a = OutpostProjects.outpostAt(c, from);
        Colony.Outpost b = OutpostProjects.outpostAt(c, to);
        if (a == b || count <= 0) {
            return;
        }
        Colony.Outpost o = a != null ? a : b;
        c.stat("S25." + o.id + "." + ColonyClock.day(c), count);
    }

    /** Return S25 of an outpost: items carried by hand in the current colony day plus the unexpired share of the previous one. */
    public static double flow(Colony c, Colony.Outpost o) {
        long day = ColonyClock.day(c);
        double frac = (c.clock % ColonyClock.DAY) / (double) ColonyClock.DAY;
        return c.stats.getOrDefault("S25." + o.id + "." + day, 0L) + c.stats.getOrDefault("S25." + o.id + "." + (day - 1), 0L) * (1 - frac);
    }

    /** Sample the signals into the raw map (EMA keys) and the snapshot (the others). */
    static void sample(Colony c, ServerLevel level, ColonyRuntime rt, Map<String, Double> raw) {
        for (Map.Entry<Item, Integer> e : c.stockTargets.entrySet()) {
            rt.snapshot.put("S11." + BuiltInRegistries.ITEM.getKey(e.getKey()).getPath(), ratio(c, level, rt, e.getKey()));
        }
        rt.snapshot.put("S16", (double) c.deposits.size());
        raw.put("S22", (double) hostiles(c, level));
        List<Double> events = c.peaks.computeIfAbsent(DAMAGE, k -> new ArrayList<>());
        events.removeIf(t -> c.clock - t > ColonyClock.DAY);
        rt.snapshot.put("S23", (double) events.size());
        boolean hv = false;
        for (GridNetwork n : Grid.of(level).networks()) {
            hv |= rt.networks.contains(n.id) && n.tier == CableTier.HV;
        }
        rt.snapshot.put("net.hv", hv ? 1.0 : 0.0);
        if (rt.waterColumnAt < 0 || level.getGameTime() - rt.waterColumnAt >= 5L * AutarkiaConfig.COLONY_SENSOR_PERIOD_TICKS.get()) {
            rt.waterColumn = waterColumn(c, level);
            rt.waterColumnAt = level.getGameTime();
        }
        rt.snapshot.put("water.column", rt.waterColumn == null ? 0.0 : (double) rt.waterColumn[3]);
        rt.snapshot.put("outpost.need", needDeposit(c, level, rt) == null ? 0.0 : 1.0);
        rt.snapshot.put("outpost.room", (double) Math.max(0, ColonyTiers.maxOutposts(c.tier) - c.outposts.size()));
        rt.snapshot.put("outpost.link_need", OutpostProjects.unlinked(c, level) == null ? 0.0 : 1.0);
        rt.snapshot.put("outpost.link_load", OutpostProjects.linkLoad(c, level));
        double car = 0;
        for (Colony.Outpost o : c.outposts) {
            double f = flow(c, o);
            if (f > car && o.link != Colony.Link.CABLE_CAR && o.link != Colony.Link.BOTH && OutpostProjects.cableCarRoute(c, level, o) != null) {
                car = f;
            }
        }
        rt.snapshot.put("cable_car.need", car);
        rt.snapshot.put("replicator.ready", Replication.ready(c, level));
        rt.snapshot.put("replicator.active", (double) c.replicatorsActive.size());
        Replication.tick(c, level);
        long today = ColonyClock.day(c);
        c.stats.keySet().removeIf(k -> k.startsWith("S25.") && Long.parseLong(k.substring(k.lastIndexOf('.') + 1)) < today - 1);
        for (Colony.Outpost o : c.outposts) {
            rt.snapshot.put("S25." + (c.outposts.indexOf(o) + 1), flow(c, o));
        }
    }

    /** Return available / target of an item for S11 (the default target is 64). */
    public static double ratio(Colony c, ServerLevel level, ColonyRuntime rt, Item item) {
        int target = c.stockTargets.getOrDefault(item, DEFAULT_TARGET);
        return target <= 0 ? 1 : (double) rt.jobs.ledger.available(level, rt, item) / target;
    }

    /** Return the ingot item of an ore id path (vanilla ingots for iron, copper and gold). */
    public static Item ingot(String ore) {
        return switch (ore) {
            case "iron" -> Items.IRON_INGOT;
            case "copper" -> Items.COPPER_INGOT;
            case "gold" -> Items.GOLD_INGOT;
            default -> BuiltInRegistries.ITEM.getValue(dev.autarkia.Autarkia.id(ore + "_ingot"));
        };
    }

    /**
     * Return the deposit an outpost should mine (template 21): unclaimed, 16 or more blocks beyond the domain edge, at
     * most {@code outpost.maxDistance} from the Core, at least {@code outpost.minSpacing} from every outpost, with its
     * metal below half its stock target; the lowest ratio, then the nearest. Null when none qualifies.
     */
    public static Colony.Deposit needDeposit(Colony c, ServerLevel level, ColonyRuntime rt) {
        if (c.tier < 4 || c.outposts.size() >= ColonyTiers.maxOutposts(c.tier)) {
            return null;
        }
        Colony.Deposit best = null;
        double bestRatio = 0;
        for (Colony.Deposit d : c.deposits) {
            double dist = Outposts.horizontal(c.corePos, d.pos());
            if (d.outpost().isPresent() || dist <= ColonyTiers.domainHalf(c.tier) + 16 || dist > AutarkiaConfig.OUTPOST_MAX_DISTANCE.get()) {
                continue;
            }
            if (c.outposts.stream().anyMatch(o -> Outposts.horizontal(o.corePos, d.pos()) < AutarkiaConfig.OUTPOST_MIN_SPACING.get())) {
                continue;
            }
            double r = ratio(c, level, rt, ingot(d.ore()));
            if (r >= 0.5) {
                continue;
            }
            if (best == null || r < bestRatio || r == bestRatio && dist < Outposts.horizontal(c.corePos, best.pos())) {
                best = d;
                bestRatio = r;
            }
        }
        return best;
    }

    /**
     * Return whether the colony should survey for deposits: tier 4 or higher, the {@code outpost} template enabled, a
     * free outpost slot, no open outpost project, the ingot of some outcrop ore below half its stock target, and no known
     * deposit that {@link #needDeposit} accepts (or the last outpost proposal found no site).
     */
    public static boolean needSurvey(Colony c, ServerLevel level, ColonyRuntime rt) {
        Colony.TemplateState ts = c.template("outpost");
        if (c.tier < 4 || !ts.enabled || c.outposts.size() >= ColonyTiers.maxOutposts(c.tier)
            || c.openProjects().stream().anyMatch(p -> p.template.getPath().equals("outpost"))) {
            return false;
        }
        boolean shortMetal = false;
        for (OreOutcropBlock.Ore o : OreOutcropBlock.Ore.values()) {
            shortMetal |= ratio(c, level, rt, ingot(o.getSerializedName())) < 0.5;
        }
        return shortMetal && (ts.noSiteUntil > c.clock || needDeposit(c, level, rt) == null);
    }

    private static int hostiles(Colony c, ServerLevel level) {
        int x0 = Integer.MAX_VALUE;
        int z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int z1 = Integer.MIN_VALUE;
        for (int[] s : Colonies.squares(c)) {
            x0 = Math.min(x0, s[0] - s[2]);
            z0 = Math.min(z0, s[1] - s[2]);
            x1 = Math.max(x1, s[0] + s[2] + 1);
            z1 = Math.max(z1, s[1] + s[2] + 1);
        }
        AABB box = new AABB(x0, level.getMinY(), z0, x1, level.getMaxY(), z1);
        return level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && m.isAlive() && Colonies.inside(c, m.blockPosition())).size();
    }

    /**
     * Return the tallest falling water column in the Core square as {x, top y, z, height}, or null: a vertical run of
     * flowing (falling) water blocks under the surface water of a column.
     */
    private static int[] waterColumn(Colony c, ServerLevel level) {
        int h = ColonyTiers.domainHalf(c.tier);
        int[] best = null;
        for (int x = c.corePos.getX() - h; x <= c.corePos.getX() + h; x++) {
            for (int z = c.corePos.getZ() - h; z <= c.corePos.getZ() + h; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                int y = top;
                while (y > level.getMinY() && falling(level, new BlockPos(x, y, z))) {
                    y--;
                }
                int height = top - y;
                if (height >= 2 && (best == null || height > best[3])) {
                    best = new int[] {x, top, z, height};
                }
            }
        }
        return best;
    }

    private static boolean falling(ServerLevel level, BlockPos p) {
        var f = level.getFluidState(p);
        return f.is(net.minecraft.tags.FluidTags.WATER) && !f.isSource();
    }
}
