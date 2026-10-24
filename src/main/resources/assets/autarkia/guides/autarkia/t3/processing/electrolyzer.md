---
navigation:
  title: Electrolyzer
  parent: t3/processing.md
  position: 30
  icon: autarkia:electrolyzer
item_ids:
  - autarkia:electrolyzer
---

# Electrolyzer

<ItemImage id="autarkia:electrolyzer" scale="2" />

The electrolyzer is the last step of the Tier 3 ore line. It turns 1 pure ore into 1 ingot and sometimes a nugget of a second metal. At Tier 4 it also makes the advanced circuit.

## Get it

<RecipeFor id="autarkia:electrolyzer" />

## Use it

1. Place the electrolyzer. It is one block.
2. Connect it to the grid: put a node next to it, or string a span to its bushing.
3. Put pure ore into an input slot.
4. Take the ingots and the nuggets from the 2 output slots.

## Recipes

Default: 160 ticks (8 s) at 256 FE/t.

| Input | Time | Output | Secondary (10%) |
|---|---|---|---|
| 1 <ItemLink id="autarkia:pure_iron" /> | 160 ticks | 1 iron ingot | 1 <ItemLink id="autarkia:zinc_nugget" /> |
| 1 <ItemLink id="autarkia:pure_copper" /> | 160 ticks | 1 copper ingot | 1 gold nugget |
| 1 <ItemLink id="autarkia:pure_gold" /> | 160 ticks | 1 gold ingot | 1 copper nugget |
| 1 <ItemLink id="autarkia:pure_tin" /> | 160 ticks | 1 <ItemLink id="autarkia:tin_ingot" /> | 1 iron nugget |
| 1 <ItemLink id="autarkia:pure_zinc" /> | 160 ticks | 1 <ItemLink id="autarkia:zinc_ingot" /> | 1 copper nugget |
| 1 <ItemLink id="autarkia:pure_vantite" /> | 160 ticks | 1 <ItemLink id="autarkia:vantite_ingot" /> | 1 gold nugget |
| 2 <ItemLink id="autarkia:basic_circuit" /> and 1 vantite ingot | 400 ticks (20 s) | 1 <ItemLink id="autarkia:advanced_circuit" /> | none |

The electrolyzer also takes the Tier 4 concentrates, for example <ItemLink id="autarkia:iron_concentrate" />.

## Numbers

| Property | Value |
|---|---|
| Energy | 256 FE/t while it works |
| Time | 160 ticks per ore |
| Buffer | 51,200 FE |
| Max input | 1,024 FE/t |
| Slots | 2 inputs, 2 outputs |

A furnace also smelts pure ore into 1 ingot. The electrolyzer adds the 10% nugget.

## Problems

- "No power": the buffer has too little energy. One electrolyzer needs 256 FE/t.
- "Output full": empty the output slots.
- "Idle": the input is empty, or the input has no recipe.

## Next

- The <ItemLink id="autarkia:advanced_circuit" /> opens Tier 4. The <ItemLink id="autarkia:core_upgrade_t4" /> needs 4 of them.
