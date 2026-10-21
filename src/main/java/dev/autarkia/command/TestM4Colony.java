package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyCoreBlockEntity;
import dev.autarkia.colony.ColonyMemory;
import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonyTemplate;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.colony.Planner;
import dev.autarkia.colony.Project;
import dev.autarkia.colony.ProjectFactory;
import dev.autarkia.colony.SiteSearch;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * The T2 colony part of {@code /autarkia test m4}: the {@code core_upgrade_t2} rules, the memory signals, the
 * {@code arrester}, {@code valve} and {@code span_upgrade} handlers and the SR_FLOW_WATER and SR_PEAK site rules on a colony
 * owned by the M3 test fake player.
 */
final class TestM4Colony {
    private TestM4Colony() {
    }

    private static InteractionResult use(FakePlayer p, ItemStack stack, BlockPos clicked) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.snapTo(clicked.getX() + 0.5, clicked.getY() + 1, clicked.getZ() + 3.5, 180, 30);
        return stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false)));
    }

    static void run(TestM4.Run r) {
        SelfTest t = r.t();
        FakePlayer owner = FakePlayerFactory.get(t.level, new GameProfile(TestM3.OWNER, "AutarkiaTest"));
        FakePlayer stranger = FakePlayerFactory.get(t.level, new GameProfile(UUID.nameUUIDFromBytes("autarkia-test-stranger".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            "AutarkiaStranger"));
        BlockPos core = r.at(30, 0, 90);
        UUID[] id = new UUID[1];
        t.then(x -> {
            for (Colony c : java.util.List.copyOf(Colonies.registry().all())) {
                if (c.owner.equals(TestM3.OWNER) || c.dimension.equals(x.level.dimension()) && c.corePos.distManhattan(core) < 200) {
                    Colonies.dissolve(c);
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(r.at(4, 0, 64), r.at(56, 16, 116))) {
                x.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            for (BlockPos p : BlockPos.betweenClosed(r.at(4, -1, 64), r.at(56, -1, 116))) {
                x.level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            InteractionResult res = use(owner, new ItemStack(AutarkiaItems.CORE_SEED.get()), core.below());
            Colony c = Colonies.byCore(x.level, core);
            id[0] = c == null ? null : c.id;
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("T2 colony: the test seed grows a Core", res == InteractionResult.SUCCESS && c != null, res.toString());
        });
        t.waitUntil("T2 colony: the Core blooms at T1", 300, () -> colony(id) != null && colony(id).state == Colony.State.ACTIVE, x -> { });
        upgrade(r, owner, stranger, core, id);
        handlers(r, core, id);
        byHand(r, core, id);
        sites(r, core, id);
    }

    private static void byHand(TestM4.Run r, BlockPos core, UUID[] id) {
        SelfTest t = r.t();
        BlockPos crate = core.offset(4, 0, -12);
        Project[] pr = new Project[1];
        t.then(x -> {
            TestM1.place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
            if (x.level.getBlockEntity(crate) instanceof dev.autarkia.storage.CrateBlockEntity cb) {
                cb.items().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(AutarkiaItems.PRESSURE_VALVE.get())), 2);
            }
            Colony c = colony(id);
            pr[0] = ProjectFactory.fromHandler(c, x.level, "pressure_valve", tpl("pressure_valve"));
            Planner.propose(c, pr[0]);
            Planner.approve(c, pr[0], false);
        });
        t.waitUntil("T2 by hand: the approved valve project reserves its 2 valves from storage and starts building", 600,
            () -> pr[0] != null && pr[0].state == Project.State.BUILDING, x -> { });
        t.then(x -> pr[0].cells.forEach(cl -> TestM1.place(x, BlockPos.of(cl.pos()), cl.state())));
        t.waitUntil("T2 by hand: the valves a player places finish the project (no T2 worker yet)", 200,
            () -> pr[0].state == Project.State.DONE, x -> { });
    }

    private static @Nullable Colony colony(UUID[] id) {
        return id[0] == null ? null : Colonies.registry().get(id[0]);
    }

    private static void upgrade(TestM4.Run r, FakePlayer owner, FakePlayer stranger, BlockPos core, UUID[] id) {
        SelfTest t = r.t();
        ItemStack[] stack = new ItemStack[1];
        t.then(x -> {
            ItemStack s = new ItemStack(AutarkiaItems.CORE_UPGRADE_T2.get());
            InteractionResult res = use(stranger, s, core);
            x.report("core_upgrade_t2: a player without rights is refused and keeps the item", res == InteractionResult.FAIL && s.getCount() == 1, res.toString());
            stack[0] = new ItemStack(AutarkiaItems.CORE_UPGRADE_T2.get());
            InteractionResult ok = use(owner, stack[0], core);
            boolean upgrading = x.level.getBlockEntity(core) instanceof ColonyCoreBlockEntity be && be.upgrading();
            x.report("core_upgrade_t2: the owner's use is consumed and starts the 100-tick upgrade", ok == InteractionResult.SUCCESS && stack[0].isEmpty() && upgrading
                && colony(id).tier == 1, ok + ", upgrading " + upgrading);
        });
        t.waitUntil("core_upgrade_t2: after 100 ticks the colony is T2", 140, () -> colony(id) != null && colony(id).tier == 2, x -> { });
        t.check("core_upgrade_t2: the domain grows to 49 x 49 and a second upgrade is refused", () -> {
            int h = ColonyTiers.domainHalf(2);
            InteractionResult again = use(owner, new ItemStack(AutarkiaItems.CORE_UPGRADE_T2.get()), core);
            return h == 24 && Colonies.at(t.level, core.offset(h, 0, h)) == colony(id) && Colonies.at(t.level, core.offset(h + 1, 0, 0)) == null
                && again == InteractionResult.FAIL;
        }, () -> "half " + ColonyTiers.domainHalf(2));
    }

    private static ColonyTemplate tpl(String id) {
        return ColonyPlans.templates().get(id);
    }

    private static void handlers(TestM4.Run r, BlockPos core, UUID[] id) {
        SelfTest t = r.t();
        BlockPos a = core.offset(-16, 0, -16);
        BlockPos b = core.offset(-6, 0, -16);
        BlockPos boiler = core.offset(10, 0, -16);
        BlockPos crank = core.offset(-16, 0, 10);
        BlockPos p = core.offset(-8, 2, 10);
        BlockPos q = core.offset(0, 0, 10);
        t.then(x -> {
            Colony c = colony(id);
            boolean before = Planner.signal(c, x.level, "mem.lightning_surge") == 0 && Planner.signal(c, x.level, "recent.boiler_high") == 0;
            ColonyMemory.event(c, "boiler_high", "test", boiler);
            x.report("signals: mem.<type> and recent.<type> read the failure memory", before && Planner.signal(c, x.level, "recent.boiler_high") == 1
                && Planner.signal(c, x.level, "mem.boiler_high") >= 0.99, "boiler_high " + Planner.signal(c, x.level, "mem.boiler_high"));
            TestM4.pole(x, a);
            TestM4.pole(x, b);
            Grid.addSpan(x.level, a, b, CableTier.COPPER);
            TestM1.placeBig(x, boiler, AutarkiaBlocks.BOILER.get(), Direction.NORTH);
            TestM1.place(x, crank, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState());
            TestM1.place(x, p.below(2), Blocks.OAK_FENCE.defaultBlockState());
            TestM1.place(x, p.below(), Blocks.OAK_FENCE.defaultBlockState());
            TestM1.place(x, p, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            TestM1.place(x, q, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            Grid.SpanResult s1 = Grid.addSpan(x.level, crank, p, CableTier.ROPE);
            Grid.SpanResult s2 = Grid.addSpan(x.level, p, q, CableTier.ROPE);
            x.report("handler rigs: a copper pole pair, a boiler, and a crank, pulley on fences and pulley on rope", s1 == Grid.SpanResult.OK && s2 == Grid.SpanResult.OK,
                s1 + ", " + s2);
        });
        t.waitTicks(5);
        t.then(x -> {
            Colony c = colony(id);
            ColonyMemory.event(c, "lightning_surge", "test", a.east());
            Project pr = ProjectFactory.fromHandler(c, x.level, "lightning_arrester", tpl("lightning_arrester"));
            boolean ok = pr != null && pr.cells.size() == 2 && pr.cells.stream().allMatch(cl -> cl.state().is(AutarkiaBlocks.LIGHTNING_ARRESTER.get())
                && (BlockPos.of(cl.pos()).equals(a.above(3)) || BlockPos.of(cl.pos()).equals(b.above(3))));
            x.report("handler arrester: an arrester above the top of both poles of the surged network", ok, pr == null ? "none" : pr.cells.size() + " cells");
            ColonyMemory.event(c, "boiler_burst", "test", boiler);
            Project v = ProjectFactory.fromHandler(c, x.level, "pressure_valve", tpl("pressure_valve"));
            boolean vok = v != null && v.cells.size() == 2 && v.cells.stream().allMatch(cl -> cl.state().is(AutarkiaBlocks.PRESSURE_VALVE.get())
                && BlockPos.of(cl.pos()).getY() == boiler.getY() + 3);
            x.report("handler valve: after a burst memory 2 valves on the boiler top", vok, v == null ? "none" : v.cells.size() + " cells");
            Span hot = TestM1.spanBetween(x, p, q);
            ColonyRuntime.of(c).hotSpans.clear();
            ColonyRuntime.of(c).hotSpans.add(hot.id);
            Project up = ProjectFactory.fromHandler(c, x.level, "span_upgrade", tpl("span_upgrade"));
            boolean uok = up != null && up.cells.size() == 2 && up.cells.stream().allMatch(cl -> cl.state().is(AutarkiaBlocks.WOODEN_POLE.get()))
                && up.cells.stream().anyMatch(cl -> BlockPos.of(cl.pos()).equals(p.below(2))) && up.cells.stream().anyMatch(cl -> BlockPos.of(cl.pos()).equals(q))
                && up.spans.size() == 2 && up.spans.stream().allMatch(s -> s.tier() == CableTier.COPPER) && up.cuts.length == 4;
            x.report("handler span_upgrade: both pulleys become wooden poles (on the fence base) and both rope spans become copper", uok,
                up == null ? "none" : up.cells.size() + " poles, " + up.spans.size() + " spans, " + up.cuts.length + " cuts");
        });
    }

    private static void sites(TestM4.Run r, BlockPos core, UUID[] id) {
        SelfTest t = r.t();
        int z = core.getZ() + 16;
        BlockPos source = new BlockPos(core.getX() - 12, core.getY() - 1, z);
        BlockPos pillar = core.offset(16, 0, 16);
        t.then(x -> {
            for (int dx = 0; dx <= 8; dx++) {
                BlockPos p = source.offset(dx, 0, 0);
                x.level.setBlock(p.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                x.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            x.level.setBlock(source, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            for (int y = 0; y < 6; y++) {
                x.level.setBlock(pillar.above(y), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        t.waitTicks(60);
        t.then(x -> {
            Colony c = colony(id);
            SiteSearch.Result w = search(c, x, "waterwheel");
            x.report("SR_FLOW_WATER: the waterwheel site lies over the flowing channel with 3 flowing cells under the rim",
                w != null && w.ruleScore() == 1 && w.origin().getZ() == z && w.floor() == source.getY(), w == null ? "no site" : w.origin().toShortString() + " rule " + w.ruleScore());
            SiteSearch.Result m = search(c, x, "lightning_mast");
            x.report("SR_PEAK: the lightning mast site is the top of the highest column", m != null && m.floor() == pillar.getY() + 5
                && m.origin().getX() == pillar.getX() && m.origin().getZ() == pillar.getZ(), m == null ? "no site" : m.origin().toShortString() + " floor " + m.floor());
        });
    }

    /** Run a full site search for a template after dropping open proposals, whose reserved volume would hide their own site. */
    private static SiteSearch.@Nullable Result search(Colony c, SelfTest x, String template) {
        c.projects.removeIf(p -> p.state == Project.State.PROPOSED);
        ColonyTemplate tpl = tpl(template);
        SiteSearch s = SiteSearch.start(c, x.level, template, tpl, ColonyPlans.plan(tpl.plan().orElseThrow()));
        if (s == null) {
            return null;
        }
        for (int i = 0; i < 64 && !s.done(); i++) {
            s.step(c, x.level, 256);
        }
        return s.best();
    }
}
