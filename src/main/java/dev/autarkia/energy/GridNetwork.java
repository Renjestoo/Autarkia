package dev.autarkia.energy;

import dev.autarkia.config.AutarkiaConfig;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * One connected set of nodes with its cached members, taps and spanning tree; runs the per-tick distribution of
 * 02 section 2.3 (offers, demand, loss, surplus or deficit, rope clip, commit, tree flows, loss, physics input).
 */
public final class GridNetwork {
    /** A member attached to a node. */
    public static final class Attached {
        public final GridMember member;
        public final long node;
        int give;
        int take;
        int offer;
        int demand;

        Attached(GridMember member, long node) {
            this.member = member;
            this.node = node;
        }
    }

    /** A foreign energy handler tapped by a connector node (always a consumer). */
    public static final class Foreign {
        public final BlockCapabilityCache<EnergyHandler, ?> cache;
        public final long node;
        int demand;
        int take;

        Foreign(BlockCapabilityCache<EnergyHandler, ?> cache, long node) {
            this.cache = cache;
            this.node = node;
        }
    }

    public final int id;
    public final CableTier tier;
    public final LongArrayList nodes = new LongArrayList();
    public final List<Span> spans = new ArrayList<>();
    public final List<Attached> members = new ArrayList<>();
    public final List<Foreign> foreign = new ArrayList<>();
    public final List<GridInjector> injectors = new ArrayList<>();
    final Long2IntOpenHashMap nodeIndex = new Long2IntOpenHashMap();
    long root;
    int[] parent;
    Span[][] parentEdge;
    int[] order;
    boolean tapsDirty;
    long lastTick = -1;
    double rho;

    public double supply;
    public double demand;
    public double served;
    public double phi = 1;
    public double stored;
    public double capacity;
    public double loss;
    public double avgGeneration;
    public double avgDemand;
    public double avgPhi = 1;
    public boolean lagging;

    GridNetwork(int id, CableTier tier) {
        this.id = id;
        this.tier = tier;
        nodeIndex.defaultReturnValue(-1);
    }

    /** Build the BFS spanning tree from the node with the largest generator capacity (tie: lowest asLong). */
    void buildTree() {
        int n = nodes.size();
        for (int i = 0; i < n; i++) {
            nodeIndex.put(nodes.getLong(i), i);
        }
        Long2IntOpenHashMap genCap = new Long2IntOpenHashMap();
        for (Attached a : members) {
            if (a.member.gridRole() == GridMember.Role.GENERATOR) {
                genCap.addTo(a.node, a.member.gridMaxOut());
            }
        }
        root = nodes.getLong(0);
        int best = -1;
        for (int i = 0; i < n; i++) {
            long p = nodes.getLong(i);
            int c = genCap.get(p);
            if (c > best || (c == best && p < root)) {
                best = c;
                root = p;
            }
        }
        Long2ObjectOpenHashMap<List<Span>> adj = new Long2ObjectOpenHashMap<>();
        for (Span s : spans) {
            adj.computeIfAbsent(s.a, k -> new ArrayList<>()).add(s);
            adj.computeIfAbsent(s.b, k -> new ArrayList<>()).add(s);
            s.treeEdge = false;
            s.bundle = 1;
        }
        parent = new int[n];
        parentEdge = new Span[n][];
        order = new int[n];
        java.util.Arrays.fill(parent, -2);
        int head = 0;
        int tail = 0;
        int r = nodeIndex.get(root);
        parent[r] = -1;
        order[tail++] = r;
        while (head < tail) {
            int cur = order[head++];
            long cp = nodes.getLong(cur);
            List<Span> list = adj.getOrDefault(cp, List.of());
            Map<Long, List<Span>> bundles = new java.util.LinkedHashMap<>();
            for (Span s : list) {
                bundles.computeIfAbsent(s.other(cp), k -> new ArrayList<>()).add(s);
            }
            for (Map.Entry<Long, List<Span>> e : bundles.entrySet()) {
                int ni = nodeIndex.get((long) e.getKey());
                if (ni < 0 || parent[ni] != -2) {
                    continue;
                }
                parent[ni] = cur;
                Span[] group = e.getValue().toArray(new Span[0]);
                for (Span s : group) {
                    s.treeEdge = true;
                    s.bundle = group.length;
                }
                parentEdge[ni] = group;
                order[tail++] = ni;
            }
        }
    }

    /**
     * Run one distribution step over dt ticks.
     *
     * @return the FE delivered to consumers this step
     */
    double tick(ServerLevel level, int dt) {
        supply = 0;
        demand = 0;
        stored = 0;
        capacity = 0;
        double inj = 0;
        for (GridInjector g : injectors) {
            inj += g.takeInjected(tier.rated * dt);
        }
        List<Attached> gens = new ArrayList<>();
        List<Attached> cons = new ArrayList<>();
        List<List<Attached>> storageGroups = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (Attached a : members) {
            a.give = 0;
            a.take = 0;
            if (!a.member.gridValid()) {
                continue;
            }
            EnergyBuffer buf = a.member.gridBuffer();
            switch (a.member.gridRole()) {
                case GENERATOR -> {
                    a.offer = Math.min(buf.stored(), sat((long) a.member.gridMaxOut() * dt));
                    supply += a.offer;
                    gens.add(a);
                }
                case CONSUMER -> {
                    a.demand = Math.min(buf.room(), sat((long) a.member.gridMaxIn() * dt));
                    demand += a.demand;
                    cons.add(a);
                }
                case STORAGE -> {
                    int g = Math.max(1, Math.min(3, a.member.gridPriority())) - 1;
                    storageGroups.get(g).add(a);
                    stored += buf.stored();
                    capacity += buf.capacity();
                }
            }
        }
        for (Foreign f : foreign) {
            f.take = 0;
            f.demand = 0;
            EnergyHandler h = f.cache.getCapability();
            if (h != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    f.demand = h.insert(sat((long) tier.rated * dt), tx);
                }
                demand += f.demand;
            }
        }
        supply += inj;
        double need = demand * (1 + rho);
        double delivered;
        double fromGen;
        if (supply >= need) {
            for (Attached c : cons) {
                c.take = c.demand;
            }
            for (Foreign f : foreign) {
                f.take = f.demand;
            }
            double surplus = supply - need;
            double charged = 0;
            for (List<Attached> group : storageGroups) {
                if (surplus - charged <= 0.5) {
                    break;
                }
                charged += charge(group, surplus - charged, dt);
            }
            fromGen = need + charged;
            phi = 1;
            delivered = demand;
        } else {
            double shortfall = need - supply;
            double discharged = 0;
            for (List<Attached> group : storageGroups) {
                if (shortfall - discharged <= 0.5) {
                    break;
                }
                discharged += discharge(group, shortfall - discharged, dt);
            }
            phi = need <= 0 ? 1 : Math.min(1, (supply + discharged) / need);
            for (Attached c : cons) {
                c.take = (int) Math.floor(phi * c.demand);
            }
            for (Foreign f : foreign) {
                f.take = (int) Math.floor(phi * f.demand);
            }
            fromGen = supply;
            delivered = phi * demand;
        }
        double genShare = Math.max(0, fromGen - inj);
        double genTotal = supply - inj;
        for (Attached g : gens) {
            g.give = genTotal <= 0 ? 0 : (int) Math.ceil(genShare * g.offer / genTotal);
            g.give = Math.min(g.give, g.offer);
        }
        computeFlows();
        for (Span s : spans) {
            s.requestedFlow = s.flow;
        }
        if (tier == CableTier.ROPE) {
            ropeClip(dt);
            computeFlows();
        }
        commit();
        served = 0;
        for (Attached c : cons) {
            served += c.take;
        }
        for (Foreign f : foreign) {
            served += f.take;
        }
        double lambda = 0;
        for (Span s : spans) {
            if (s.treeEdge) {
                lambda += Math.abs(s.flow) * tier.lossPerBlock * s.length;
            }
        }
        loss = lambda;
        rho = lambda / Math.max(1, served);
        double a = 0.01;
        avgGeneration += a * ((supply - inj) / dt - avgGeneration);
        avgDemand += a * (demand / dt - avgDemand);
        avgPhi += a * (phi - avgPhi);
        for (Span s : spans) {
            s.load = (float) (Math.abs(s.flow) / ((double) tier.rated * dt));
            s.active = true;
        }
        return delivered;
    }

    private double charge(List<Attached> group, double amount, int dt) {
        double room = 0;
        int[] rooms = new int[group.size()];
        for (int i = 0; i < group.size(); i++) {
            Attached a = group.get(i);
            rooms[i] = Math.min(a.member.gridBuffer().room(), sat((long) a.member.gridMaxIn() * dt));
            room += rooms[i];
        }
        if (room <= 0) {
            return 0;
        }
        double use = Math.min(room, amount);
        double done = 0;
        for (int i = 0; i < group.size(); i++) {
            int t = (int) Math.floor(use * rooms[i] / room);
            group.get(i).take += t;
            done += t;
        }
        return done;
    }

    private double discharge(List<Attached> group, double amount, int dt) {
        double avail = 0;
        int[] r = new int[group.size()];
        for (int i = 0; i < group.size(); i++) {
            Attached a = group.get(i);
            r[i] = Math.min(a.member.gridBuffer().stored(), sat((long) a.member.gridMaxOut() * dt));
            avail += r[i];
        }
        if (avail <= 0) {
            return 0;
        }
        double use = Math.min(avail, amount);
        double done = 0;
        for (int i = 0; i < group.size(); i++) {
            int g = (int) Math.ceil(use * r[i] / avail);
            g = Math.min(g, r[i]);
            group.get(i).give += g;
            done += g;
        }
        return done;
    }

    private void computeFlows() {
        int n = nodes.size();
        double[] b = new double[n];
        for (Attached a : members) {
            int i = nodeIndex.get(a.node);
            if (i >= 0) {
                b[i] += a.give - a.take;
            }
        }
        for (Foreign f : foreign) {
            int i = nodeIndex.get(f.node);
            if (i >= 0) {
                b[i] -= f.take;
            }
        }
        for (Span s : spans) {
            s.flow = 0;
        }
        for (int k = n - 1; k > 0; k--) {
            int i = order[k];
            Span[] edge = parentEdge[i];
            if (edge == null) {
                continue;
            }
            double f = b[i];
            for (Span s : edge) {
                s.flow = f / edge.length;
            }
            b[parent[i]] += b[i];
        }
    }

    private void ropeClip(int dt) {
        double cap = 40.0 * dt;
        int n = nodes.size();
        boolean[] inSub = new boolean[n];
        for (int k = n - 1; k > 0; k--) {
            int i = order[k];
            Span[] edge = parentEdge[i];
            if (edge == null) {
                continue;
            }
            double f = 0;
            for (Span s : edge) {
                f += s.flow;
            }
            double per = Math.abs(f) / edge.length;
            for (Span s : edge) {
                s.slip = per > cap + 1e-6;
            }
            if (Math.abs(f) <= cap * edge.length + 1e-6) {
                continue;
            }
            java.util.Arrays.fill(inSub, false);
            markSubtree(i, inSub);
            double allowed = cap * edge.length;
            if (f < 0) {
                double k2 = allowed / Math.abs(f);
                for (Attached a : members) {
                    int ni = nodeIndex.get(a.node);
                    if (ni >= 0 && inSub[ni]) {
                        a.take = (int) Math.floor(a.take * k2);
                    }
                }
                for (Foreign fo : foreign) {
                    int ni = nodeIndex.get(fo.node);
                    if (ni >= 0 && inSub[ni]) {
                        fo.take = (int) Math.floor(fo.take * k2);
                    }
                }
            } else {
                double reduce = f - allowed;
                double outside = 0;
                for (Attached a : members) {
                    int ni = nodeIndex.get(a.node);
                    if (ni >= 0 && !inSub[ni]) {
                        outside += a.take;
                    }
                }
                double kOut = outside <= 0 ? 1 : Math.max(0, (outside - reduce) / outside);
                double givenInSub = 0;
                for (Attached a : members) {
                    int ni = nodeIndex.get(a.node);
                    if (ni >= 0 && !inSub[ni]) {
                        a.take = (int) Math.floor(a.take * kOut);
                    } else if (ni >= 0) {
                        givenInSub += a.give;
                    }
                }
                double kIn = givenInSub <= 0 ? 1 : Math.max(0, (givenInSub - reduce) / givenInSub);
                for (Attached a : members) {
                    int ni = nodeIndex.get(a.node);
                    if (ni >= 0 && inSub[ni]) {
                        a.give = (int) Math.floor(a.give * kIn);
                    }
                }
            }
            computeFlows();
        }
    }

    private void markSubtree(int root, boolean[] mark) {
        mark[root] = true;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < nodes.size(); i++) {
                if (!mark[i] && parent[i] >= 0 && mark[parent[i]]) {
                    mark[i] = true;
                    changed = true;
                }
            }
        }
    }

    private void commit() {
        Map<GridMember, int[]> net = new IdentityHashMap<>();
        for (Attached a : members) {
            net.computeIfAbsent(a.member, k -> new int[1])[0] += a.take - a.give;
        }
        for (Map.Entry<GridMember, int[]> e : net.entrySet()) {
            int d = e.getValue()[0];
            EnergyBuffer buf = e.getKey().gridBuffer();
            if (d > 0) {
                buf.fill(d);
            } else if (d < 0) {
                buf.drain(-d);
            }
            e.getKey().gridTransferred(-d);
        }
        for (Foreign f : foreign) {
            EnergyHandler h = f.cache.getCapability();
            if (h != null && f.take > 0) {
                try (Transaction tx = Transaction.openRoot()) {
                    h.insert(f.take, tx);
                    tx.commit();
                }
            }
        }
    }

    /** Return the node at the root of the spanning tree. */
    public long root() {
        return root;
    }

    /** Return true when the network should be counted as lagging this tick. */
    boolean isLagging(int dt) {
        return dt >= 20;
    }

    static int sat(long v) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, v));
    }

    /** Return the configured heat multiplier (kept here so physics reads config once per network tick). */
    static double heatMul() {
        return AutarkiaConfig.CABLE_HEAT_MULTIPLIER.get();
    }
}
