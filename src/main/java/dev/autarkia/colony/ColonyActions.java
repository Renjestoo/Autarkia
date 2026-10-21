package dev.autarkia.colony;

import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.worker.WorkerEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;

/**
 * Server handling of {@code colony_action} (05b section 9.1) with the rights of 05 section 1.4: everyone may switch
 * tabs and show ghosts; owner and trusted players manage projects, jobs, workers and settings; only the owner renames
 * and edits the trusted list.
 */
public final class ColonyActions {
    private ColonyActions() {
    }

    /** Handle one action from a player. */
    public static void handle(ServerPlayer player, UUID colonyId, String action, List<String> args) {
        if (Colonies.server() == null) {
            return;
        }
        Colony c = Colonies.registry().get(colonyId);
        ServerLevel level = c == null ? null : Colonies.level(c);
        if (c == null || level == null) {
            return;
        }
        boolean manage = c.canManage(player.getUUID());
        boolean owner = c.owner.equals(player.getUUID());
        switch (action) {
            case "tab" -> ColonySync.selectTab(player, Integer.parseInt(args.get(0)));
            case "show" -> {
                Project p = project(c, args);
                if (p != null) {
                    AutarkiaNetwork.toPlayer(player, ColonyPreview.build(level, player, p));
                }
            }
            default -> {
                if (!manage || !managed(player, c, level, action, args, owner)) {
                    player.sendOverlayMessage(Component.translatable("message.autarkia.colony.no_rights"));
                }
            }
        }
        if (player.containerMenu instanceof ColonyMenu m && m.colonyId.equals(c.id)) {
            ColonySync.send(player, c, m.mode);
        }
    }

    private static Project project(Colony c, List<String> args) {
        return args.isEmpty() ? null : c.project(UUID.fromString(args.get(0)));
    }

    private static boolean managed(ServerPlayer player, Colony c, ServerLevel level, String action, List<String> args, boolean owner) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        switch (action) {
            case "approve" -> {
                Project p = project(c, args);
                if (p != null) {
                    Planner.approve(c, p, false);
                }
            }
            case "deny" -> {
                Project p = project(c, args);
                if (p != null) {
                    Planner.deny(c, p);
                }
            }
            case "cancel" -> {
                Project p = project(c, args);
                if (p != null) {
                    Planner.cancel(c, p);
                }
            }
            case "retry" -> {
                Project p = project(c, args);
                if (p != null) {
                    rt.jobs.retry(c, p);
                }
            }
            case "priority" -> {
                Project p = project(c, args);
                if (p != null) {
                    p.adjust = Math.max(-50, Math.min(50, p.adjust + Integer.parseInt(args.get(1))));
                    ColonyTemplate tpl = ColonyPlans.templates().get(p.template.getPath());
                    if (tpl != null) {
                        p.priority = Planner.priority(c, p.template.getPath(), tpl, p);
                    }
                }
            }
            case "rotate" -> rotate(player, c, level, project(c, args));
            case "autonomy" -> c.autonomy = Colony.Autonomy.valueOf(args.get(0).toUpperCase(java.util.Locale.ROOT));
            case "template" -> c.template(args.get(0)).enabled = args.get(1).equals("on");
            case "signs" -> c.signs = args.get(0).equals("on");
            case "stock" -> {
                var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(args.get(0)));
                int n = Integer.parseInt(args.get(1));
                if (n <= 0) {
                    c.stockTargets.remove(item);
                } else {
                    c.stockTargets.put(item, n);
                }
            }
            case "job_cancel" -> rt.jobs.cancel(c, level, UUID.fromString(args.get(0)));
            case "recall", "home" -> {
                if (level.getEntity(UUID.fromString(args.get(0))) instanceof WorkerEntity w) {
                    w.recall(c.corePos);
                }
            }
            case "rename" -> {
                if (!owner) {
                    return false;
                }
                c.name = args.get(0).substring(0, Math.min(32, args.get(0).length()));
            }
            case "trust" -> {
                if (!owner || c.trusted.size() >= 16) {
                    return false;
                }
                ServerPlayer target = level.getServer().getPlayerList().getPlayerByName(args.get(0));
                if (target == null || c.trusted.contains(target.getUUID()) || target.getUUID().equals(c.owner)) {
                    return false;
                }
                c.trusted.add(target.getUUID());
            }
            case "untrust" -> {
                if (!owner) {
                    return false;
                }
                c.trusted.removeIf(u -> u.toString().equals(args.get(0)));
            }
            case "pattern_record" -> {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(Integer.parseInt(args.get(0)), Integer.parseInt(args.get(1)),
                    Integer.parseInt(args.get(2)));
                if (!(level.getBlockEntity(pos) instanceof PatternLoomBlockEntity loom) || Colonies.at(level, pos) != c
                    || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64) {
                    return false;
                }
                String err = loom.record(player, args.size() > 3 ? args.get(3) : "");
                if (err != null) {
                    player.sendOverlayMessage(Component.translatable(err));
                }
            }
            case "pattern_add" -> {
                Pattern.Ref ref = player.getMainHandItem().get(dev.autarkia.registry.AutarkiaComponents.PATTERN_REF.get());
                Pattern pt = ref == null ? null : Pattern.load(level.getServer(), ref.id());
                String err = ref == null ? "message.autarkia.pattern.hold_card" : pt == null ? "message.autarkia.pattern.missing"
                    : c.library.stream().anyMatch(e -> e.pattern().equals(ref.id())) ? "message.autarkia.pattern.already"
                    : c.library.size() >= ColonyTiers.patternSlots(c.tier) ? "message.autarkia.pattern.library_full" : null;
                if (err != null) {
                    player.sendOverlayMessage(Component.translatable(err));
                } else {
                    c.library.add(new Colony.LibraryEntry(pt.id(), pt.role(), true));
                }
            }
            case "pattern_remove" -> {
                UUID id = UUID.fromString(args.get(0));
                c.library.removeIf(e -> e.pattern().equals(id));
                c.instances.replaceAll(in -> in.pattern().equals(id) ? in.watched(false) : in);
            }
            case "pattern_role" -> {
                UUID id = UUID.fromString(args.get(0));
                String role = args.get(1);
                if (!PATTERN_ROLES.contains(role)) {
                    return false;
                }
                c.library.replaceAll(e -> e.pattern().equals(id) ? new Colony.LibraryEntry(id, role, e.enabled()) : e);
            }
            case "pattern_enable" -> {
                UUID id = UUID.fromString(args.get(0));
                c.library.replaceAll(e -> e.pattern().equals(id) ? new Colony.LibraryEntry(id, e.role(), args.get(1).equals("on")) : e);
            }
            case "pattern_watch" -> {
                UUID id = UUID.fromString(args.get(0));
                c.instances.replaceAll(in -> in.id().equals(id) ? in.watched(args.get(1).equals("on")) : in);
            }
            default -> throw new IllegalArgumentException("Unknown colony action '" + action + "'");
        }
        Colonies.changed();
        return true;
    }

    /** Pattern roles of 05 section 5.2 in the order the Set role button cycles them. */
    public static final List<String> PATTERN_ROLES = List.of("generic", "line", "generator", "storage", "defense", "outpost");

    private static void rotate(ServerPlayer player, Colony c, ServerLevel level, Project p) {
        if (p == null || p.state != Project.State.PROPOSED) {
            return;
        }
        ColonyTemplate tpl = ColonyPlans.templates().get(p.template.getPath());
        ColonyPlans.Plan plan = tpl == null || tpl.plan().isEmpty() ? null : ColonyPlans.plan(tpl.plan().get());
        if (plan == null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.colony.rotate_failed"));
            return;
        }
        Vec3i size = ColonyPlans.rotatedSize(plan.size(), p.rotation);
        int cx = p.origin.getX() + size.getX() / 2;
        int cz = p.origin.getZ() + size.getZ() / 2;
        Rotation next = p.rotation.getRotated(Rotation.CLOCKWISE_90);
        SiteSearch s = SiteSearch.start(c, level, p.template.getPath(), tpl, plan);
        SiteSearch.Result r = s == null ? null : s.evaluateAt(c, level, cx, cz, next);
        if (r == null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.colony.rotate_failed"));
            return;
        }
        AutarkiaNetwork.toPlayer(player, new dev.autarkia.network.ProjectPreviewPayload(p.id, 0, List.of(), List.of()));
        Project fresh = ProjectFactory.fromSite(c, level, p.template.getPath(), tpl, r);
        fresh.adjust = p.adjust;
        fresh.priority = Planner.priority(c, p.template.getPath(), tpl, fresh);
        c.projects.set(c.projects.indexOf(p), fresh);
        AutarkiaNetwork.toPlayer(player, ColonyPreview.build(level, player, fresh));
    }
}
