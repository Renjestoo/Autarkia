---
navigation:
  title: Jaw Crusher
  parent: t3/processing.md
  position: 10
  icon: autarkia:jaw_crusher
item_ids:
  - autarkia:jaw_crusher
---

# Jaw Crusher

<ItemImage id="autarkia:jaw_crusher" scale="2" />

The jaw crusher is the first step of the Tier 3 ore line. It is an electric heavy machine. It always turns 1 raw ore into 2 crushed ore, with no chance roll.

## Get it

<RecipeFor id="autarkia:jaw_crusher" />

## Use it

1. Place 4 <ItemLink id="autarkia:foundation" /> blocks in a 2 x 2 square.
2. Place the crusher on the foundation. It is 2 x 2 x 2 blocks.
3. Connect it to the grid: put a node next to it, or string a span to one of its bushings.
4. Put raw ore, cobblestone or gravel into the input slot.
5. Take the products from the 2 output slots, or pull them out with a chute or a hopper.

## Recipes

| Input | Time | Energy | Output |
|---|---|---|---|
| 1 raw iron, copper, gold, tin or zinc | 100 ticks (5 s) | 192 FE/t | 2 crushed ore of that metal |
| 1 <ItemLink id="autarkia:raw_vantite" /> | 100 ticks (5 s) | 192 FE/t | 2 <ItemLink id="autarkia:crushed_vantite" /> |
| 1 cobblestone | 100 ticks (5 s) | 192 FE/t | 1 gravel |
| 1 gravel | 100 ticks (5 s) | 192 FE/t | 1 sand |

The crushed ores are <ItemLink id="autarkia:crushed_iron" />, <ItemLink id="autarkia:crushed_copper" />, <ItemLink id="autarkia:crushed_gold" />, <ItemLink id="autarkia:crushed_tin" /> and <ItemLink id="autarkia:crushed_zinc" />.

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2, on 4 foundation blocks |
| Energy | 192 FE/t while it works |
| Time | 100 ticks per item |
| Buffer | 38,400 FE |
| Max input | 768 FE/t |
| Slots | 1 input, 2 outputs |

## Problems

The status shows on the screen and in Jade.

- "Condition missing" and "Needs a foundation under every bottom block": one of the 4 bottom blocks has no foundation below it.
- "No power": the buffer has less energy than one tick of work. Check the connection and the generators.
- "Output full": empty the output slots.
- "Idle": the input slot is empty.
- The item does not go into the slot: the crusher has no recipe for it.

## Next

- Wash the crushed ore in a <ItemLink id="autarkia:sluice" />.
- Then separate the washed ore in a <ItemLink id="autarkia:magnetic_separator" />.
