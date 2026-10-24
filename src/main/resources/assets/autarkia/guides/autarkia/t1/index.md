---
navigation:
  title: "Tier 1: Rope and Bronze"
  parent: index.md
  position: 1
  icon: autarkia:bronze_gear
---

# Tier 1: Rope and Bronze

Tier 1 has no electric wires. Your arms, animals and the wind make the power. Rope drives carry the power to the
machines. Bronze is your first alloy (a metal that you make from two metals).

**Goal of Tier 1:** make bronze, power your first machines over rope, plant a Colony Core and let the colony grow.
At the end, use a Core Upgrade (Tier 2) on the Core.

FE/t is the unit of power in this manual: energy (FE) per game tick. One second has 20 ticks.

## Steps

1. Read this manual. Later you can craft a new <ItemLink id="autarkia:field_manual" /> from a book and a bronze nugget.
2. Make a stone pickaxe or better. Find <ItemLink id="autarkia:tin_ore" /> between Y -16 and Y 96. In deepslate it is
   <ItemLink id="autarkia:deepslate_tin_ore" />.
3. Mine the ore to get <ItemLink id="autarkia:raw_tin" />. Smelt the raw tin in a furnace to get a
   <ItemLink id="autarkia:tin_ingot" />.
4. Craft a <ItemLink id="autarkia:crucible" /> from 7 bricks. Put it on a lit campfire, a lit furnace or a lava source.
5. Put 3 copper ingots and 1 tin ingot into the crucible. You get 4 <ItemLink id="autarkia:bronze_ingot" />.
6. Craft 2 <ItemLink id="autarkia:bronze_gear" /> from 4 bronze ingots. Craft 9
   <ItemLink id="autarkia:bronze_nugget" /> from 1 bronze ingot.
7. Craft bronze tools: <ItemLink id="autarkia:bronze_pickaxe" />, <ItemLink id="autarkia:bronze_axe" />,
   <ItemLink id="autarkia:bronze_shovel" />, <ItemLink id="autarkia:bronze_hoe" /> and
   <ItemLink id="autarkia:bronze_sword" />. The <ItemLink id="autarkia:bronze_sling" /> throws cobblestone and nuggets.
8. Craft bronze armor: <ItemLink id="autarkia:bronze_helmet" />, <ItemLink id="autarkia:bronze_chest" />,
   <ItemLink id="autarkia:bronze_legs" /> and <ItemLink id="autarkia:bronze_boots" />.
9. Craft a <ItemLink id="autarkia:hand_crank" />. Hold use on it to make 20 FE/t.
10. Craft a <ItemLink id="autarkia:rope_coil" /> from string. Use it on the crank, then on a machine. This makes a rope
    span (one length of rope between two nodes) of at most 12 blocks.
11. For longer lines, put a <ItemLink id="autarkia:rope_pulley" /> every 12 blocks or less.
12. Grind raw ore. The <ItemLink id="autarkia:quern" /> needs only your hand. The
    <ItemLink id="autarkia:stamp_mill" /> uses 16 FE/t from a rope span.
13. Smelt the crushed ore, for example <ItemLink id="autarkia:crushed_tin" />,
    <ItemLink id="autarkia:crushed_copper" />, <ItemLink id="autarkia:crushed_iron" /> and
    <ItemLink id="autarkia:crushed_gold" />. One raw ore gives 1 crushed ore, and 1 more at a chance of 50%.
14. Make storage: <ItemLink id="autarkia:crate" />, <ItemLink id="autarkia:chute" />,
    <ItemLink id="autarkia:tally_board" /> and the <ItemLink id="autarkia:wrench" />.
15. Make more power: the <ItemLink id="autarkia:treadwheel" />, the <ItemLink id="autarkia:windmill" /> and the
    <ItemLink id="autarkia:stone_flywheel" /> to store energy.
16. Craft a <ItemLink id="autarkia:core_seed" /> from 4 wheat seeds, 4 bronze ingots and 1 bronze gear.
17. Use the seed on the top of grass, dirt or stone. The 3 x 3 x 3 space above must be free. The Core grows for 10
    seconds.
18. The Core claims a domain (the colony area) of 33 x 33 blocks. It gives you a
    <ItemLink id="autarkia:worker_capsule" /> with a Porter and a field manual.
19. Use the capsule on the ground in your domain. The Porter (the Tier 1 worker) comes out.
20. Put building materials into crates in the domain. The colony proposes its first project. Approve it in the Core
    screen, at a <ItemLink id="autarkia:project_board" /> or with a <ItemLink id="autarkia:planning_tablet" />.
21. Let your Porters build the project with the items from your crates.
22. Make more Porters in the <ItemLink id="autarkia:worker_assembler" />: a <ItemLink id="autarkia:walker_frame" />,
    2 <ItemLink id="autarkia:mainspring" />, 4 bronze gears and 4 bronze ingots, 600 ticks at 20 FE/t.
23. Porters run on a wound spring. A <ItemLink id="autarkia:winding_post" /> winds them again.
24. Craft a <ItemLink id="autarkia:core_upgrade_t2" /> from 4 bronze ingots, 2 copper ingots, 2 bronze gears and
    1 iron ingot. Use it on the active Core. After 5 seconds the colony is Tier 2.

Then go to [Tier 2](../t2/index.md).

## What the colony builds by itself

Every 5 seconds the Core reads the machines, generators, spans, crates and workers of its domain. When a need stays,
the colony proposes a project:

| Need | Project | Most built |
|---|---|---|
| Power 25% short for 60 s | Windmill on a high, open site | 8 |
| Power 25% short for 60 s | Treadwheel near the network | 4 |
| Crates 85% full for 30 s | Crate Cluster | 16 |
| Stored power lasts less than 30 s, and supply is more than 1.2 x demand | Flywheel | 4 |
| A rope span at 90% load for 30 s | Parallel Span (a second rope next to the first) | No limit |
| A worker low on charge, or workers far from a winding post | Winding Post | 4 |

## Tier 1 colony limits

| Property | Value |
|---|---|
| Domain | 33 x 33 blocks |
| Worker cap | 4 |
| Active projects at the same time | 1 |

<SubPages />
