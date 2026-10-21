package dev.autarkia.colony;

import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.ColonySyncPayload;
import dev.autarkia.worker.WorkerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Builds the colony screen data of the active tab (05b section 9.1: Overview, Projects, Jobs, Workers, Settings) and
 * sends it every 20 ticks to each player with a colony menu open.
 */
public final class ColonySync {
    public static final int OVERVIEW = 0;
    public static final int PROJECTS = 1;
    public static final int JOBS = 2;
    public static final int WORKERS = 3;
    public static final int SETTINGS = 4;
    public static final int PATTERNS = 5;
    private static final Map<UUID, Integer> TAB = new HashMap<>();

    private ColonySync() {
    }

    /** Register the periodic sender. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            MinecraftServer server = e.getServer();
            if (server.getTickCount() % 20 != 0 || Colonies.server() == null) {
                return;
            }
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.containerMenu instanceof ColonyMenu m) {
                    Colony c = Colonies.registry().get(m.colonyId);
                    if (c != null) {
                        send(p, c, m.mode);
                    }
                }
            }
        });
    }

    /** Select the tab a player sees. */
    public static void selectTab(ServerPlayer player, int tab) {
        TAB.put(player.getUUID(), Math.max(0, Math.min(PATTERNS, tab)));
    }

    /** Send the active tab data now. */
    public static void send(ServerPlayer player, Colony c, ColonyMenu.Mode mode) {
        int tab = mode == ColonyMenu.Mode.CORE ? TAB.getOrDefault(player.getUUID(), OVERVIEW) : PROJECTS;
        ServerLevel level = Colonies.level(c);
        if (level == null) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putBoolean("manage", c.canManage(player.getUUID()));
        data.putBoolean("owner", c.owner.equals(player.getUUID()));
        data.putString("autonomy", c.autonomy.getSerializedName());
        switch (tab) {
            case PROJECTS -> projects(c, level, data);
            case JOBS -> jobs(c, level, data);
            case WORKERS -> workers(c, level, data);
            case SETTINGS -> settings(c, level, data);
            case PATTERNS -> patterns(c, level, data);
            default -> overview(c, level, data);
        }
        AutarkiaNetwork.toPlayer(player, new ColonySyncPayload(c.id, tab, data));
    }

    private static void overview(Colony c, ServerLevel level, CompoundTag t) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        long now = level.getGameTime();
        t.putString("name", c.name);
        t.putInt("tier", c.tier);
        t.putInt("half", ColonyTiers.domainHalf(c.tier));
        t.putDouble("s01", rt.signal("S01", now, 0));
        t.putDouble("s02", rt.signal("S02", now, 0));
        t.putDouble("s09", rt.signal("S09", now, 0));
        ListTag hist = new ListTag();
        for (double[] h : rt.powerHistory) {
            CompoundTag e = new CompoundTag();
            e.putDouble("s", h[0]);
            e.putDouble("d", h[1]);
            hist.add(e);
        }
        t.put("history", hist);
        t.putInt("workers", c.workerCost());
        t.putInt("cap", ColonyTiers.workerCap(c.tier));
        t.putDouble("budget", c.budgetRemaining);
        t.putInt("daily", ColonyTiers.dailyBudget(c.tier));
        t.putLong("day", ColonyClock.day(c));
        ListTag log = new ListTag();
        rt.log.forEach(l -> log.add(StringTag.valueOf(l)));
        t.put("log", log);
    }

    private static void projects(Colony c, ServerLevel level, CompoundTag t) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        ListTag list = new ListTag();
        for (Project p : c.projects) {
            if (!p.state.open() && p.state != Project.State.DONE) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putString("id", p.id.toString());
            e.putString("template", p.template.getPath());
            e.putString("state", p.state.getSerializedName());
            e.putString("reason", p.reason.key());
            e.putDouble("arg", p.reason.args().isEmpty() ? 0 : p.reason.args().get(0));
            e.putLong("origin", p.origin.asLong());
            e.putString("rotation", p.rotation.getSerializedName());
            e.putDouble("score", p.siteScore);
            e.putInt("edits", p.levelingEdits());
            e.putDouble("cost", p.cost);
            e.putDouble("priority", p.priority);
            e.putInt("adjust", p.adjust);
            e.putString("block", p.blockReason);
            ListTag bom = new ListTag();
            for (Map.Entry<Item, Integer> b : p.bom.entrySet()) {
                CompoundTag line = new CompoundTag();
                line.putString("item", BuiltInRegistries.ITEM.getKey(b.getKey()).toString());
                line.putInt("need", b.getValue());
                line.putInt("stock", ItemLedger.stock(level, rt, b.getKey()));
                line.putInt("reserved", rt.jobs.ledger.reservedFor(p.id, b.getKey()));
                bom.add(line);
            }
            e.put("bom", bom);
            CompoundTag labor = new CompoundTag();
            for (ColonyJob j : rt.jobs.all()) {
                if (p.id.equals(j.projectId)) {
                    labor.putInt(j.type.name().toLowerCase(java.util.Locale.ROOT), labor.getIntOr(j.type.name().toLowerCase(java.util.Locale.ROOT), 0) + 1);
                }
            }
            e.put("labor", labor);
            list.add(e);
        }
        t.put("projects", list);
        t.putDouble("budget", c.budgetRemaining);
        t.putString("warning", rt.rackWarning);
    }

    private static void jobs(Colony c, ServerLevel level, CompoundTag t) {
        ListTag list = new ListTag();
        for (ColonyJob j : ColonyRuntime.of(c).jobs.all()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", j.id.toString());
            e.putString("type", j.type.name().toLowerCase(java.util.Locale.ROOT));
            e.putLong("target", j.target.asLong());
            e.putString("status", j.status.name().toLowerCase(java.util.Locale.ROOT));
            e.putInt("attempts", j.attempts);
            Entity w = j.assignee == null ? null : level.getEntity(j.assignee);
            e.putString("assignee", w == null ? "" : w.getName().getString());
            list.add(e);
        }
        t.put("jobs", list);
    }

    private static void workers(Colony c, ServerLevel level, CompoundTag t) {
        ListTag list = new ListTag();
        for (Colony.WorkerEntry w : c.workers.values()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", w.id().toString());
            e.putString("type", w.type().getPath());
            if (level.getEntity(w.id()) instanceof WorkerEntity we) {
                e.putString("state", we.stateName());
                e.putFloat("charge", we.charge());
                e.putString("job", we.jobName());
                BlockPos home = we.workPos();
                e.putLong("home", home.asLong());
            } else {
                e.putString("state", "unloaded");
            }
            list.add(e);
        }
        t.put("workers", list);
    }

    private static void patterns(Colony c, ServerLevel level, CompoundTag t) {
        t.putInt("slots", ColonyTiers.patternSlots(c.tier));
        ListTag lib = new ListTag();
        for (Colony.LibraryEntry e : c.library) {
            Pattern p = Pattern.load(level.getServer(), e.pattern());
            CompoundTag row = new CompoundTag();
            row.putString("id", e.pattern().toString());
            row.putString("role", e.role());
            row.putBoolean("enabled", e.enabled());
            row.putString("name", p == null ? "" : p.name());
            row.putBoolean("missing", p == null);
            if (p != null) {
                row.putString("size", p.size().getX() + "x" + p.size().getY() + "x" + p.size().getZ());
            }
            row.putInt("instances", (int) c.instances.stream().filter(in -> in.pattern().equals(e.pattern()) && in.watched()).count());
            lib.add(row);
        }
        t.put("library", lib);
        ListTag inst = new ListTag();
        for (Colony.Instance in : c.instances) {
            Pattern p = Pattern.load(level.getServer(), in.pattern());
            CompoundTag row = new CompoundTag();
            row.putString("id", in.id().toString());
            row.putString("name", p == null ? "" : p.name());
            row.putLong("origin", in.origin().asLong());
            row.putBoolean("watched", in.watched());
            inst.add(row);
        }
        t.put("instances", inst);
    }

    private static void settings(Colony c, ServerLevel level, CompoundTag t) {
        ListTag tpl = new ListTag();
        for (String id : ColonyPlans.templates().keySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", id);
            e.putBoolean("enabled", c.template(id).enabled);
            e.putInt("tier", ColonyPlans.templates().get(id).tier());
            tpl.add(e);
        }
        t.put("templates", tpl);
        t.putBoolean("signs", c.signs);
        t.putDouble("multiplier", dev.autarkia.config.AutarkiaConfig.COLONY_BUDGET_MULTIPLIER.get());
        ListTag trusted = new ListTag();
        for (UUID u : c.trusted) {
            ServerPlayer online = level.getServer().getPlayerList().getPlayer(u);
            trusted.add(StringTag.valueOf(online == null ? u.toString() : online.getName().getString()));
        }
        t.put("trusted", trusted);
        ListTag stock = new ListTag();
        c.stockTargets.forEach((item, n) -> {
            CompoundTag e = new CompoundTag();
            e.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
            e.putInt("count", n);
            stock.add(e);
        });
        t.put("stock", stock);
    }
}
