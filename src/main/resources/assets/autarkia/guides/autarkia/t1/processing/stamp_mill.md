---
navigation:
  title: Stamp Mill
  parent: t1/processing.md
  position: 30
  icon: autarkia:stamp_mill
item_ids:
  - autarkia:stamp_mill
---

# Stamp Mill

<ItemImage id="autarkia:stamp_mill" scale="2" />

The stamp mill is a powered ore mill. A heavy hammer crushes raw ore. It does the work of the quern, but it uses FE
from a rope span, so you do not have to turn it.

## Get it

<RecipeFor id="autarkia:stamp_mill" />

## Use it

1. Place the stamp mill. It is 2 blocks tall.
2. Connect a generator to the mill with a <ItemLink id="autarkia:rope_coil" />. The mill has a rope sheave.
3. Give the mill power, for example with a <ItemLink id="autarkia:hand_crank" />.
4. Open the mill. Put raw ore into the input slot.
5. Take the crushed ore from the two output slots.

## Recipes

| Input | Machine | Time | Energy | Outputs |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:raw_tin" /> | Stamp Mill | 160 ticks (8 s) | 16 FE/t | 1 <ItemLink id="autarkia:crushed_tin" />, 1 more at 50% |
| 1 <ItemLink id="minecraft:raw_copper" /> | Stamp Mill | 160 ticks (8 s) | 16 FE/t | 1 <ItemLink id="autarkia:crushed_copper" />, 1 more at 50% |
| 1 <ItemLink id="minecraft:raw_iron" /> | Stamp Mill | 160 ticks (8 s) | 16 FE/t | 1 <ItemLink id="autarkia:crushed_iron" />, 1 more at 50% |
| 1 <ItemLink id="minecraft:raw_gold" /> | Stamp Mill | 160 ticks (8 s) | 16 FE/t | 1 <ItemLink id="autarkia:crushed_gold" />, 1 more at 50% |

## Numbers

| Property | Value |
|---|---|
| Energy use | 16 FE/t while it works |
| Time for one ore | 160 ticks (8 s) |
| Buffer | 3,200 FE |
| Input slots | 1 |
| Output slots | 2 |
| Rope spans | 2 |
| Size | 1 x 2 x 1 |
| Mining tool | Axe |

## Problems

- "No power": the buffer is empty. Turn the crank, or add a generator to the network.
- "Output full": take the crushed ore out.
- "Idle": the input slot is empty.

## Next

- Smelt the crushed ore in a furnace for more ingots.
- In Tier 3 the jaw crusher gives 2 crushed ores for each raw ore.
