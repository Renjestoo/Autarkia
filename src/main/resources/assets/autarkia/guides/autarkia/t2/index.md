---
navigation:
  title: "Tier 2: Steam and Copper"
  parent: index.md
  position: 2
  icon: autarkia:copper_wire_spool
---

# Tier 2: Steam and Copper

Tier 2 brings a new metal (zinc), two alloys (brass and steel), copper wire on wooden poles, water and steam power,
belts, racks and Patterns. It also brings new dangers: hot wires, boilers that burst and lightning.

**Goal of Tier 2:** make brass and steel, and carry power over copper wire. Run boilers and engines safely, move items
on belts and grow the colony with Hauler Crabs. At the end, use a Core Upgrade (Tier 3) on the Core.

## Steps

1. Craft a <ItemLink id="autarkia:core_upgrade_t2" /> and use it on your active Colony Core. After 5 seconds the colony
   is Tier 2. The domain grows to 49 x 49 blocks and the worker cap to 8.
2. Find <ItemLink id="autarkia:zinc_ore" /> between Y 0 and Y 64. In deepslate it is
   <ItemLink id="autarkia:deepslate_zinc_ore" />. Mine it to get <ItemLink id="autarkia:raw_zinc" />.
3. Smelt the raw zinc in a furnace to get a <ItemLink id="autarkia:zinc_ingot" />. For more ingots, crush it first in
   the quern or the stamp mill to get <ItemLink id="autarkia:crushed_zinc" />.
4. Put 3 copper ingots and 1 zinc ingot into the heated <ItemLink id="autarkia:crucible" />. You get 4
   <ItemLink id="autarkia:brass_ingot" />.
5. Craft the <ItemLink id="autarkia:wire_drawer" /> from brass ingots. Power it from your rope network: put a
   <ItemLink id="autarkia:rope_pulley" /> next to it. It needs 40 FE/t.
6. In the wire drawer, make <ItemLink id="autarkia:copper_coil" /> (4 copper ingots and 1 iron ingot give 2 coils)
   and the <ItemLink id="autarkia:copper_wire_spool" /> (2 copper ingots give 1 spool).
7. Craft <ItemLink id="autarkia:wooden_pole" /> blocks. Connect your Tier 1 generators to the poles with copper wire.
   The hand crank, the treadwheel, the windmill and the stone flywheel have a bushing for copper wire.
8. Craft the <ItemLink id="autarkia:plate_press" /> from brass ingots. It makes 1
   <ItemLink id="autarkia:brass_plate" /> from 1 brass ingot at 48 FE/t.
9. Craft the <ItemLink id="autarkia:blast_smelter" />. Place it on a 2 x 2 floor of
   <ItemLink id="autarkia:foundation" /> and give it 64 FE/t. It works only when it is hot.
10. In the blast smelter, make <ItemLink id="autarkia:steel_ingot" /> from 1 iron ingot and 2 coal. The waste is
    <ItemLink id="autarkia:slag" />.
11. Press steel ingots into <ItemLink id="autarkia:steel_plate" />. Craft 4 steel plates and 1 iron ingot into 2
    <ItemLink id="autarkia:steel_gear" />.
12. Build the <ItemLink id="autarkia:sluice" />. It washes crushed ore into washed ore, for example
    <ItemLink id="autarkia:washed_iron" />. The blast smelter turns 3 washed ore into 4 ingots.
13. Make steel tools and armor: the <ItemLink id="autarkia:steel_pickaxe" />, <ItemLink id="autarkia:steel_axe" />,
    <ItemLink id="autarkia:steel_shovel" />, <ItemLink id="autarkia:steel_sword" /> and
    <ItemLink id="autarkia:steel_sledgehammer" />, the armor <ItemLink id="autarkia:steel_helmet" />,
    <ItemLink id="autarkia:steel_chest" />, <ItemLink id="autarkia:steel_legs" /> and
    <ItemLink id="autarkia:steel_boots" />, the <ItemLink id="autarkia:wire_cutter" /> and the
    <ItemLink id="autarkia:steam_repeater" />.
14. Build a <ItemLink id="autarkia:waterwheel" /> in flowing water. It makes up to 128 FE/t without fuel.
15. Build a <ItemLink id="autarkia:boiler" /> with a <ItemLink id="autarkia:steam_engine" /> against it. Put a
    <ItemLink id="autarkia:pressure_valve" /> on top before you light the fire. One boiler feeds 2 engines of up to
    120 FE/t each.
16. Store energy in a <ItemLink id="autarkia:capacitor_bank" />. Light your works with the
    <ItemLink id="autarkia:industrial_lamp" />.
17. Keep the load of each copper span at 256 FE/t or less. Above that the wire gets hot and snaps. Check the spans
    with the <ItemLink id="autarkia:multimeter" />.
18. Protect your lines from lightning. Put a <ItemLink id="autarkia:lightning_arrester" /> on top of your poles. Build
    a <ItemLink id="autarkia:lightning_mast" /> with capacitor banks to catch the bolts and store their energy.
19. Move items with the <ItemLink id="autarkia:belt" />. Split and sort them with the
    <ItemLink id="autarkia:belt_splitter" /> and the <ItemLink id="autarkia:sorting_chute" />.
20. Put your crates on a <ItemLink id="autarkia:wood_rack" />. Keep each rack at 100% of its load rating or less.
21. Craft a <ItemLink id="autarkia:crab_frame" />. Build a Hauler Crab in the
    <ItemLink id="autarkia:worker_assembler" />: 1 crab frame, 3 mainsprings, 4 steel gears, 2 copper coils and
    4 brass plates, 900 ticks at 40 FE/t.
22. Craft a <ItemLink id="autarkia:pattern_loom" />. Craft blank cards (<ItemLink id="autarkia:pattern_card" />)
    from paper and brass plates. Record your best builds as Patterns.
23. Prepare Tier 3. Craft the <ItemLink id="autarkia:basic_circuit" /> from redstone, copper coils and a brass
    plate. Craft the <ItemLink id="autarkia:fabricator" /> and give it 160 FE/t. Make the
    <ItemLink id="autarkia:servo" /> in the fabricator. Craft a <ItemLink id="autarkia:battery_bank" />.
24. In the fabricator, make the <ItemLink id="autarkia:core_upgrade_t3" /> from 4 basic circuits, 2 servos, 4 steel
    plates and 1 battery bank (400 ticks). Use it on the active Core. After 5 seconds the colony is Tier 3.

Then go to [Tier 3](../t3/index.md).

## What the colony does by itself

The colony reads its sensors every 5 seconds. In Tier 2 it learns new projects:

| Need | Project | Most built |
|---|---|---|
| Power 20% short for 60 s, and flowing water in the domain | Waterwheel | 6 |
| Power 30% short for 60 s, and fuel worth 64 coal in storage | Boiler and Engine | 4 |
| Crates 85% full for 60 s | Wood Racks | 12 |
| A span at 80% load for 60 s, a span at 80% heat for 30 s, or a snapped span | Span Upgrade (rope to copper) | No limit |
| 2 lightning strikes in one week, or a lightning surge | Lightning Mast | 2 |
| A lightning surge destroyed a machine | Lightning Arresters | No limit |
| A boiler reached 10 bar today, or a boiler burst | Pressure Valve | No limit |

Porters build only Tier 1 projects, and the Hauler Crab places no blocks. So you build the Tier 2 projects yourself:
place the blocks and string the spans that the ghost shows. The colony sees each finished part and marks it done.

Hauler Crabs keep the domain clean. When debris lies in the domain, the colony sends a crab to collect it. Debris is
snapped cable ends, falling crates from collapsed racks and the items that spill from them. The crab puts it into the
colony storage. See [The T2 Colony](colony.md) for all limits.

## Tier 2 colony limits

| Property | Value |
|---|---|
| Domain | 49 x 49 blocks |
| Worker cap | 8 |
| Porters at most | 6 |
| Hauler Crabs at most | 4 |
| Active projects at the same time | 2 |
| Daily build budget | 160 points |
| Pattern library | 4 Patterns |
| Largest Pattern box | 16 x 16 x 16 |

<SubPages />
