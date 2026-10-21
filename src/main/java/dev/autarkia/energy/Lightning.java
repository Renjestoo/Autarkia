package dev.autarkia.energy;

import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.LightningPulsePayload;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaSounds;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Lightning on the grid (02 section 5, W7 and W8). A natural bolt (not visual-only) moves to the nearest valid
 * {@code lightning_mast} within the mast radius, which then charges its capacitor banks; the mast takes the whole hit,
 * so that bolt is made visual only (no entity damage, no fire). Otherwise it moves to the
 * highest attach point of a metal span node within the attract radius (attach y at least strike y - 8) and starts a
 * pulse: nodes in order of arrival time (ties by lowest position) at {@code lightningPulseSpeed} blocks per tick along
 * conducting spans. Each span passed gains heat (copper 0.30, insulated 0.20, HV 0.10, superconductor temperature
 * 0.25). At a node an arrester on the block above the node top ends that branch; a capacitor bank absorbs 120,000 FE
 * (or is destroyed when it has less room) and ends the pulse; the first unprotected machine is destroyed (surge) and
 * ends the pulse. Living entities within 1.5 blocks of the struck node take 5 damage.
 */
public final class Lightning {
    private static final Map<ServerLevel, Set<BlockPos>> MASTS = new IdentityHashMap<>();
    private static final List<Pulse> PULSES = new ArrayList<>();

    /** One scheduled node arrival of a pulse plan. */
    private record Step(long tick, BlockPos node, String action, @Nullable BlockPos target) {
    }

    private record Pulse(ServerLevel level, List<Step> steps, int[] next) {
    }

    private record Arrival(double time, long node, int spanId, double startTime, boolean fromA) {
    }

    private Lightning() {
    }

    /** Register the bolt and tick listeners. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(Lightning::onJoin);
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                tick(sl);
            }
        });
    }

    /** Index a loaded mast. */
    public static void addMast(ServerLevel level, BlockPos pos) {
        MASTS.computeIfAbsent(level, k -> new HashSet<>()).add(pos.immutable());
    }

    /** Remove a mast from the index. */
    public static void removeMast(ServerLevel level, BlockPos pos) {
        Set<BlockPos> s = MASTS.get(level);
        if (s != null) {
            s.remove(pos);
        }
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getEntity() instanceof LightningBolt bolt) || !(e.getLevel() instanceof ServerLevel sl) || bolt.visualOnly || e.loadedFromDisk()) {
            return;
        }
        strike(sl, bolt);
    }

    /** Handle a real bolt: move it to a mast or to the highest metal node and start the pulse. */
    public static void strike(ServerLevel level, LightningBolt bolt) {
        Vec3 at = bolt.position();
        LightningMastBlockEntity mast = nearestMast(level, at);
        if (mast != null) {
            BlockPos top = mast.top();
            bolt.setPos(top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5);
            bolt.setVisualOnly(true);
            dev.autarkia.colony.ColonySensors.strike(level, top);
            mast.strike();
            return;
        }
        long node = attractNode(level, at);
        if (node == Long.MIN_VALUE) {
            dev.autarkia.colony.ColonySensors.strike(level, BlockPos.containing(at));
            return;
        }
        dev.autarkia.colony.ColonySensors.strike(level, BlockPos.of(node));
        Vec3 hit = attach(level, node);
        bolt.setPos(hit.x, hit.y, hit.z);
        Autarkia.LOG.info("[lightning] bolt at {} moved to node {}", BlockPos.containing(at).toShortString(), BlockPos.of(node).toShortString());
        startPulse(level, node);
    }

    private static @Nullable LightningMastBlockEntity nearestMast(ServerLevel level, Vec3 at) {
        double r = AutarkiaConfig.LIGHTNING_MAST_RADIUS.get();
        LightningMastBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : MASTS.getOrDefault(level, Set.of())) {
            if (!(level.getBlockEntity(p) instanceof LightningMastBlockEntity m) || !m.valid()) {
                continue;
            }
            double d = Math.hypot(p.getX() + 0.5 - at.x, p.getZ() + 0.5 - at.z);
            if (d <= r && d < bestD) {
                best = m;
                bestD = d;
            }
        }
        return best;
    }

    private static Vec3 attach(ServerLevel level, long node) {
        GridSavedData data = Grid.of(level).data();
        IntArrayList ids = data.at(node);
        for (int i = 0; i < ids.size(); i++) {
            Span s = data.spans.get(ids.getInt(i));
            if (s != null) {
                return s.a == node ? s.attachA : s.attachB;
            }
        }
        return Vec3.atCenterOf(BlockPos.of(node));
    }

    private static long attractNode(ServerLevel level, Vec3 at) {
        int r = AutarkiaConfig.SPAN_ATTRACT_RADIUS.get();
        GridSavedData data = Grid.of(level).data();
        long best = Long.MIN_VALUE;
        double bestY = -Double.MAX_VALUE;
        int cx0 = ((int) Math.floor(at.x) - r) >> 4;
        int cx1 = ((int) Math.floor(at.x) + r) >> 4;
        int cz0 = ((int) Math.floor(at.z) - r) >> 4;
        int cz1 = ((int) Math.floor(at.z) + r) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                for (long node : data.nodesIn(net.minecraft.world.level.ChunkPos.pack(cx, cz))) {
                    if (!conducts(data, node)) {
                        continue;
                    }
                    Vec3 a = attach(level, node);
                    if (Math.hypot(a.x - at.x, a.z - at.z) > r || a.y < at.y - 8) {
                        continue;
                    }
                    if (a.y > bestY + 1e-9 || Math.abs(a.y - bestY) <= 1e-9 && node < best) {
                        best = node;
                        bestY = a.y;
                    }
                }
            }
        }
        return best;
    }

    private static boolean conducts(GridSavedData data, long node) {
        IntArrayList ids = data.at(node);
        for (int i = 0; i < ids.size(); i++) {
            Span s = data.spans.get(ids.getInt(i));
            if (s != null && s.tier.conductsLightning) {
                return true;
            }
        }
        return false;
    }

    /** Plan the pulse from the struck node with the current world state, send the visual and schedule the effects. */
    static void startPulse(ServerLevel level, long start) {
        GridLevel g = Grid.of(level);
        long now = level.getGameTime();
        double speed = AutarkiaConfig.LIGHTNING_PULSE_SPEED.get();
        PriorityQueue<Arrival> queue = new PriorityQueue<>((x, y) -> x.time != y.time ? Double.compare(x.time, y.time) : Long.compare(x.node, y.node));
        Map<Long, Double> best = new HashMap<>();
        LongOpenHashSet done = new LongOpenHashSet();
        List<Step> steps = new ArrayList<>();
        List<LightningPulsePayload.Entry> visual = new ArrayList<>();
        queue.add(new Arrival(0, start, -1, 0, false));
        best.put(start, 0.0);
        boolean ended = false;
        while (!queue.isEmpty() && !ended) {
            Arrival a = queue.poll();
            if (!done.add(a.node)) {
                continue;
            }
            if (a.spanId >= 0) {
                Span via = g.data().spans.get(a.spanId);
                if (via != null) {
                    visual.add(new LightningPulsePayload.Entry(a.spanId, now + (long) Math.floor(a.startTime), a.fromA));
                    heat(via, now);
                }
            }
            long tick = now + (long) Math.ceil(a.time);
            BlockPos pos = BlockPos.of(a.node);
            String action = "pass";
            BlockPos target = null;
            if (level.getBlockEntity(pos) instanceof FuseBoxBlockEntity) {
                action = "fuse";
            } else if (arresterAbove(level, pos)) {
                action = "arrester";
            } else {
                for (BlockPos m : machines(level, pos)) {
                    if (level.getBlockEntity(m) instanceof CapacitorBankBlockEntity) {
                        action = "capacitor";
                        target = m;
                        break;
                    }
                    if (!protectedMachine(level, m)) {
                        action = "surge";
                        target = m;
                        break;
                    }
                }
            }
            steps.add(new Step(tick, pos, action, target));
            if (!action.equals("pass")) {
                ended = !action.equals("arrester");
                continue;
            }
            IntArrayList ids = g.data().at(a.node);
            for (int i = 0; i < ids.size(); i++) {
                Span s = g.data().spans.get(ids.getInt(i));
                if (s == null || !s.tier.conductsLightning) {
                    continue;
                }
                long o = s.other(a.node);
                double t = a.time + s.length / speed;
                if (!done.contains(o) && t < best.getOrDefault(o, Double.MAX_VALUE)) {
                    best.put(o, t);
                    queue.add(new Arrival(t, o, s.id, a.time, s.a == a.node));
                }
            }
        }
        if (!ended && !steps.isEmpty()) {
            Step last = steps.get(steps.size() - 1);
            steps.add(new Step(last.tick, last.node, "fade", null));
        }
        hurtNear(level, attach(level, start));
        AutarkiaNetwork.toNear(level, Vec3.atCenterOf(BlockPos.of(start)), 160, new LightningPulsePayload(visual, (float) speed));
        PULSES.add(new Pulse(level, steps, new int[1]));
        Autarkia.LOG.info("[lightning] pulse from {}: {} node arrivals, end {}", BlockPos.of(start).toShortString(), steps.size(),
            steps.isEmpty() ? "none" : steps.get(steps.size() - 1).action);
    }

    private static void heat(Span s, long now) {
        s.struckAt = now;
        switch (s.tier) {
            case COPPER -> s.heat += 0.30f;
            case INSULATED -> s.heat += 0.20f;
            case HV -> s.heat += 0.10f;
            case SUPERCONDUCTOR -> s.temp += 0.25f;
            default -> {
            }
        }
    }

    private static void hurtNear(ServerLevel level, Vec3 at) {
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.5))) {
            if (le.position().distanceTo(at) <= 1.5 + le.getBbWidth()) {
                le.hurtServer(level, level.damageSources().lightningBolt(), 5f);
            }
        }
    }

    /** Return the top block of a node: the attach block of a connector, else the highest block of the machine. */
    public static BlockPos top(Level level, BlockPos node) {
        if (level.getBlockState(node).getBlock() instanceof GridConnector c) {
            return BlockPos.containing(c.attachPoint(node, level.getBlockState(node)));
        }
        BlockPos p = node;
        while (level.getBlockEntity(p.above()) instanceof IMultiBlockEntity m && m.getCenter().equals(node)) {
            p = p.above();
        }
        return p;
    }

    private static boolean arresterAbove(Level level, BlockPos node) {
        return level.getBlockState(top(level, node).above()).is(AutarkiaBlocks.LIGHTNING_ARRESTER.get());
    }

    /** Return the machines at a node: its own member (a port node) or the tapped neighbours of a connector. */
    private static List<BlockPos> machines(ServerLevel level, BlockPos node) {
        List<BlockPos> out = new ArrayList<>();
        if (!(level.getBlockState(node).getBlock() instanceof GridConnector)) {
            if (Grid.memberAt(level, node) != null && !(level.getBlockEntity(node) instanceof LightningMastBlockEntity)) {
                out.add(node);
            }
            return out;
        }
        for (Direction d : Direction.values()) {
            BlockPos np = node.relative(d);
            if (level.getBlockState(np).getBlock() instanceof GridConnector) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(np);
            if (be instanceof IMultiBlockEntity m && !m.isCenter()) {
                np = m.getCenter();
            }
            if (Grid.memberAt(level, np) != null || be != null && level.getCapability(Capabilities.Energy.BLOCK, np, d.getOpposite()) != null) {
                out.add(np.immutable());
            }
        }
        return out;
    }

    private static boolean protectedMachine(ServerLevel level, BlockPos machine) {
        BlockPos topPos = top(level, machine);
        for (BlockPos p = machine; p.getY() <= topPos.getY(); p = p.above()) {
            for (Direction d : Direction.values()) {
                if (level.getBlockState(p.relative(d)).is(AutarkiaBlocks.LIGHTNING_ARRESTER.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void tick(ServerLevel level) {
        long now = level.getGameTime();
        PULSES.removeIf(p -> {
            if (p.level != level) {
                return false;
            }
            while (p.next[0] < p.steps.size() && p.steps.get(p.next[0]).tick <= now) {
                apply(level, p.steps.get(p.next[0]++));
            }
            return p.next[0] >= p.steps.size();
        });
    }

    private static void apply(ServerLevel level, Step s) {
        Vec3 at = attach(level, s.node.asLong());
        var rnd = level.getRandom();
        for (int i = 0; i < 3; i++) {
            Vec3 tgt = at.add(rnd.nextDouble() * 2 - 1, rnd.nextDouble() - 0.5, rnd.nextDouble() * 2 - 1);
            level.sendParticles(new ElectricArcParticleOptions(tgt, 0xFFDDF4FF, 1.0F, rnd.nextInt(), 3), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, at.x, at.y, at.z, AutarkiaSounds.of("lightning.pulse"), SoundSource.BLOCKS, 0.8f, 1.4f);
        switch (s.action) {
            case "capacitor" -> {
                if (level.getBlockEntity(s.target) instanceof CapacitorBankBlockEntity c) {
                    if (c.buffer().room() < 120000) {
                        level.destroyBlock(s.target, true);
                    } else {
                        c.absorbLightning(120000);
                    }
                }
            }
            case "surge" -> surge(level, s.node, s.target);
            case "fuse" -> Grid.of(level).tripFuse(s.node.asLong(), "lightning");
            case "fade" -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.1);
            default -> {
            }
        }
    }

    private static void surge(ServerLevel level, BlockPos node, BlockPos machine) {
        if (!AutarkiaConfig.LIGHTNING_SURGE_ENABLED.get()) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, machine.getX() + 0.5, machine.getY() + 0.5, machine.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.1);
            return;
        }
        Vec3 c = Vec3.atCenterOf(machine);
        GridNetwork net = Grid.of(level).network(node.asLong());
        level.explode(null, c.x, c.y, c.z, 1.5F, false, Level.ExplosionInteraction.NONE);
        level.destroyBlock(machine, true);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 12, 0.4, 0.4, 0.4, 0.02);
        level.playSound(null, machine, AutarkiaSounds.of("surge.blow"), SoundSource.BLOCKS, 1f, 1.3f);
        dev.autarkia.colony.ColonyMemory.at(level, machine, "lightning_surge", String.valueOf(net == null ? 0 : net.id));
        AutarkiaAdvancements.awardNear(level, c, "power/surge");
        Autarkia.LOG.info("[lightning] surge destroyed the machine at {}", machine.toShortString());
    }
}
