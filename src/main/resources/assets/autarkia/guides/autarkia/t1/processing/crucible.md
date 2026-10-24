---
navigation:
  title: Crucible
  parent: t1/processing.md
  position: 10
  icon: autarkia:crucible
item_ids:
  - autarkia:crucible
---

# Crucible

<ItemImage id="autarkia:crucible" scale="2" />

The crucible is a clay pot that melts metals together. It makes bronze, the first alloy of Autarkia. It needs heat
from below, not FE.

## Get it

<RecipeFor id="autarkia:crucible" />

Smelt clay balls in a furnace to get bricks.

## Use it

1. Place a heat source: a lit campfire, a lit furnace (or smoker or blast furnace) or a lava source block.
2. Place the crucible directly on top of the heat source.
3. Open the crucible.
4. Put 3 copper ingots and 1 tin ingot into the two input slots.
5. Wait. The metal glows while the crucible works.
6. Take the 4 bronze ingots from the output slot.

## Recipe

| Inputs | Machine | Time at 1.0x | Output |
|---|---|---|---|
| 3 <ItemLink id="minecraft:copper_ingot" /> + 1 <ItemLink id="autarkia:tin_ingot" /> | Crucible | 300 ticks (15 s) | 4 <ItemLink id="autarkia:bronze_ingot" /> |

## Numbers

| Heat source | Speed | Time for one batch |
|---|---|---|
| Lit campfire | 0.5x | 600 ticks (30 s) |
| Lit furnace, smoker or blast furnace | 1.0x | 300 ticks (15 s) |
| Lava source | 1.5x | 200 ticks (10 s) |

| Property | Value |
|---|---|
| Energy | None |
| Input slots | 2 |
| Output slots | 1 |
| Mining tool | Pickaxe |

## Problems

- "Condition missing" and "No heat source": no lit heat source is directly below. Light the campfire or the furnace.
  Flowing lava does not count; use a source block.
- "Output full": take the bronze out of the output slot.

## Next

- Craft bronze into <ItemLink id="autarkia:bronze_gear" />, tools and the <ItemLink id="autarkia:hand_crank" />.
- In Tier 2 the blast smelter also makes bronze.
