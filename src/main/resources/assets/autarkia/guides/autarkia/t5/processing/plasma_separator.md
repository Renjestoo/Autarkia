---
navigation:
  title: Plasma Separator
  parent: t5/processing.md
  position: 10
  icon: autarkia:plasma_separator
item_ids:
  - autarkia:plasma_separator
---

# Plasma Separator

<ItemImage id="autarkia:plasma_separator" scale="2" />

The plasma separator is the ore machine of Tier 5. It turns 1 raw ore into 6 ingots in one step. It also alloys the
lattice alloy ingot.

## Get it

<RecipeFor id="autarkia:plasma_separator" />

## Use it

1. Place 4 <ItemLink id="autarkia:foundation" /> blocks in a 2 x 2 square.
2. Place the separator on the foundation. It is 2 x 2 x 2 blocks, and every bottom block must stand on foundation.
3. Connect a span from your network to its machine port.
4. Open the separator.
5. To separate ore: put raw ore into input A. Leave input B empty.
6. To alloy: put 1 <ItemLink id="autarkia:lattice_crystal" /> and 1 <ItemLink id="autarkia:vantite_steel_ingot" /> into
   inputs A and B.
7. Take the products from the 3 output slots.

## Recipes

Machine recipes do not show in this book, so the tables give them.

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="minecraft:raw_iron" /> | Plasma separator | 100 ticks (5 s) | 4,096 FE/t | 6 <ItemLink id="minecraft:iron_ingot" />, 20%: 1 <ItemLink id="autarkia:zinc_ingot" /> |
| 1 <ItemLink id="minecraft:raw_copper" /> | Plasma separator | 100 ticks (5 s) | 4,096 FE/t | 6 <ItemLink id="minecraft:copper_ingot" />, 20%: 1 <ItemLink id="minecraft:gold_ingot" /> |
| 1 <ItemLink id="minecraft:raw_gold" /> | Plasma separator | 100 ticks (5 s) | 4,096 FE/t | 6 <ItemLink id="minecraft:gold_ingot" />, 20%: 1 <ItemLink id="minecraft:copper_ingot" /> |
| 1 <ItemLink id="autarkia:raw_tin" /> | Plasma separator | 100 ticks (5 s) | 4,096 FE/t | 6 <ItemLink id="autarkia:tin_ingot" />, 20%: 1 <ItemLink id="minecraft:iron_ingot" /> |
| 1 <ItemLink id="autarkia:raw_zinc" /> | Plasma separator | 100 ticks (5 s) | 4,096 FE/t | 6 <ItemLink id="autarkia:zinc_ingot" />, 20%: 1 <ItemLink id="minecraft:copper_ingot" /> |
| 1 <ItemLink id="autarkia:raw_vantite" /> | Plasma separator | 100 ticks (5 s) | 4,096 FE/t | 6 <ItemLink id="autarkia:vantite_ingot" />, 20%: 1 <ItemLink id="minecraft:gold_ingot" /> |

The raw ore inputs use the common raw ore tags (for example `c:raw_materials/iron`), so raw ores of other mods with
these tags work too.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:lattice_crystal" /> + 1 <ItemLink id="autarkia:vantite_steel_ingot" /> | Plasma separator | 200 ticks (10 s) | 4,096 FE/t | 1 <ItemLink id="autarkia:lattice_alloy_ingot" /> |

## Slots

| Slot | What it takes |
|---|---|
| Input A | Raw ore or an alloy input |
| Input B | Alloy inputs only, so raw ore never blocks this slot |
| Output 1 to 3 | The products |

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2, on foundation |
| Energy use | 4,096 FE/t while it works |
| Energy buffer | 819,200 FE |
| Energy input | Up to 16,384 FE/t |
| Ore yield | 6 ingots per raw ore, plus a secondary ingot at 20% |
| Hardness, blast resistance | 5, 8 |

## Problems

- "Condition missing" and "Needs a foundation under every bottom block": a bottom block of the separator does not
  stand on foundation. Place foundation under all 4 bottom blocks. The separator checks this every 20 ticks.
- "No power": the buffer has less than 4,096 FE. Give it more energy.
- "Output full": take the products out of the output slots.
- "Idle": no recipe matches the inputs.

Jade shows the status, "Energy: N / 819,200 FE" and the queue.

## Next

- Make <ItemLink id="autarkia:lattice_alloy_ingot" /> for the <ItemLink id="autarkia:core_upgrade_t5" />.
- Feed it raw ore from your <ItemLink id="autarkia:nanite_hive" /> claims.
