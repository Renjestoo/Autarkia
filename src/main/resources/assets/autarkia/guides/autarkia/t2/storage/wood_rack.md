---
navigation:
  title: Wood Rack
  parent: t2/storage.md
  position: 10
  icon: autarkia:wood_rack
item_ids:
  - autarkia:wood_rack
---

# Wood Rack

<ItemImage id="autarkia:wood_rack" scale="2" />

The wood rack is a shelf for 2 crates. It holds empty or full crates. Belts, hoppers and workers can put items into the
crates on it. A rack carries only a limited load: too much weight bends it, and then it collapses.

## Get it

<RecipeFor id="autarkia:wood_rack" />

## Use it

1. Place the rack on a solid block or on another rack.
2. Use a <ItemLink id="autarkia:crate" /> on the rack. The crate goes into the first free place. A full crate keeps its
   items.
3. Or open the rack and put the crates into its crate slots.
4. Keep the load at 100% or less. Look at the rack with Jade to see the load.

## Screen

- Crate slots: one slot for each crate place. Only crates go in.
- The button next to a crate slot: opens that crate. Its 27 slots show below.
- Load bar: green up to 100%, yellow up to 120%, red above. The white line is 100%.
- "1,744 / 2,048 units": the load and the rating.
- "Collapse in 45 s": shows in red when the rack is critical.

## Load

Each crate weighs 8 units. Each item adds the units of its class:

| Class | Units per item | Examples |
|---|---|---|
| Light | 0.25 | dyes, food, nuggets, seeds, flowers, leaves, saplings, feathers, paper, string |
| Standard | 0.5 | most items, for example cobblestone |
| Dense | 1 | ingots, ores, raw ores, crushed and washed ores, gears, plates, gems, cable |
| Machine | 8 | every block with a block entity (machines, chests), anvils |
| Heavy | 9 | storage blocks, foundation, obsidian, crying obsidian |

Two full crates of cobblestone are 85% of a wood rack. Two full crates of iron ingots are 170%.

## Bend and collapse

| Load | Stage | What happens |
|---|---|---|
| up to 100% | 0 | Nothing |
| above 100% up to 110% | 1 | The shelves sag and creak |
| above 110% up to 120% | 2 | The rack bends more and drops dust |
| above 120% | 3 | Critical: after 60 seconds the rack collapses |

When a rack collapses:

1. The rack is gone. It drops 3 sticks.
2. Its crates fall. They land as crate blocks with all their items inside.
3. A falling crate that hits the next rack adds its weight for a moment. When that rack goes above 150%, it collapses
   at once. A full row of racks can fall like dominoes.
4. A rack on top of the collapsed rack collapses too.
5. A rack also collapses when the block below it goes away.

Jade shows "Load: 1744 / 2048 units (85%)", "Stage 0", "Crates: 2 / 2" and in stage 3 "Collapse in 45 s".

## Numbers

| Property | Value |
|---|---|
| Crate places | 2 |
| Load rating | 2,048 units |
| Crate weight | 8 units |
| Bends above | 100% |
| Critical above | 120% |
| Collapse time | 1,200 ticks (60 seconds) |
| Collapses at once above | 150% with the impact of a falling crate |
| Drops on collapse | 3 sticks |

## Problems

- The rack creaks: it is above 100%. Take heavy items out, or move them to another rack.
- "Collapse in 45 s": take items out now. At 120% or less the countdown stops. Above 120% again, it starts again
  from 60 seconds.
- The rack breaks when you mine the block under it: a rack needs support.

## What the colony does

When the colony storage is 85% full for 60 seconds, it proposes a Wood Racks project near the storage. It builds at
most 12.

## Next

- Hauler Crabs collect the crates and items after a collapse. See the <ItemLink id="autarkia:crab_frame" />.
- In Tier 3, the <ItemLink id="autarkia:steel_rack" /> holds 3 crates and 8,192 units.
