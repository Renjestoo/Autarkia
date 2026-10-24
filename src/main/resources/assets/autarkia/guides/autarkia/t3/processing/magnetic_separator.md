---
navigation:
  title: Magnetic Separator
  parent: t3/processing.md
  position: 20
  icon: autarkia:magnetic_separator
item_ids:
  - autarkia:magnetic_separator
---

# Magnetic Separator

<ItemImage id="autarkia:magnetic_separator" scale="2" />

The magnetic separator is the third step of the Tier 3 ore line. It turns 2 washed ore into 3 pure ore. It also gives slag and sometimes a nugget of a second metal.

## Get it

<RecipeFor id="autarkia:magnetic_separator" />

## Use it

1. Place the separator. It is one block.
2. Connect it to the grid: put a node next to it, or string a span to its bushing.
3. Put washed ore into the input slot.
4. Take the pure ore, the slag and the nuggets from the 3 output slots.

## Recipes

All recipes take 120 ticks (6 s) at 128 FE/t.

| Input | Output | Secondary (50%) |
|---|---|---|
| 2 <ItemLink id="autarkia:washed_iron" /> | 3 <ItemLink id="autarkia:pure_iron" />, 1 <ItemLink id="autarkia:slag" /> | 1 <ItemLink id="autarkia:zinc_nugget" /> |
| 2 <ItemLink id="autarkia:washed_copper" /> | 3 <ItemLink id="autarkia:pure_copper" />, 1 slag | 1 gold nugget |
| 2 <ItemLink id="autarkia:washed_gold" /> | 3 <ItemLink id="autarkia:pure_gold" />, 1 slag | 1 copper nugget |
| 2 <ItemLink id="autarkia:washed_tin" /> | 3 <ItemLink id="autarkia:pure_tin" />, 1 slag | 1 iron nugget |
| 2 <ItemLink id="autarkia:washed_zinc" /> | 3 <ItemLink id="autarkia:pure_zinc" />, 1 slag | 1 copper nugget |
| 2 <ItemLink id="autarkia:washed_vantite" /> | 3 <ItemLink id="autarkia:pure_vantite" />, 1 slag | 1 gold nugget |

## Numbers

| Property | Value |
|---|---|
| Energy | 128 FE/t while it works |
| Time | 120 ticks per operation |
| Buffer | 25,600 FE |
| Max input | 512 FE/t |
| Slots | 1 input, 3 outputs |

With the full line, 1 raw ore gives 2 crushed ore, 2 washed ore and then 3 pure ore.

## Problems

- "No power": the buffer has too little energy. Check the connection.
- "Output full": empty the 3 output slots.
- "Idle": the input has fewer than 2 washed ore, or it is empty.

## Next

- Smelt the pure ore, or put it into an <ItemLink id="autarkia:electrolyzer" />.
- The first pure ore gives the advancement "Clean Separation".
