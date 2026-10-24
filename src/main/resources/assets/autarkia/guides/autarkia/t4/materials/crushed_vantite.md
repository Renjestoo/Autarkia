---
navigation:
  title: Crushed Vantite Ore
  parent: t4/materials.md
  position: 50
  icon: autarkia:crushed_vantite
item_ids:
  - autarkia:crushed_vantite
---

# Crushed Vantite Ore

<ItemImage id="autarkia:crushed_vantite" scale="2" />

Crushed vantite ore is raw vantite in small pieces. It is the start of every vantite line that gives more than 1
ingot for each raw vantite.

## Get it

| Input | Machine | Time | Output |
|---|---|---|---|
| 1 <ItemLink id="autarkia:raw_vantite" /> | <ItemLink id="autarkia:jaw_crusher" /> | 100 ticks, 192 FE/t | 2 crushed vantite |
| 1 raw vantite | <ItemLink id="autarkia:stamp_mill" /> | 160 ticks, 16 FE/t | 1 crushed vantite, plus 1 more (50%) |
| 1 raw vantite | <ItemLink id="autarkia:quern" /> | 8 turns by hand | 1 crushed vantite, plus 1 more (50%) |

The jaw crusher is a heavy machine. It needs foundation under its bottom blocks.

## Use it

| Input | Machine | Time | Output |
|---|---|---|---|
| 1 crushed vantite and 250 mB water | <ItemLink id="autarkia:froth_flotation_plant" /> | 200 ticks, 768 FE/t | 2 <ItemLink id="autarkia:vantite_concentrate" />, slag (50%), gold nugget (35%) |
| 1 crushed vantite | <ItemLink id="autarkia:sluice" /> | 100 ticks, water | 1 <ItemLink id="autarkia:washed_vantite" />, gold nugget (10%) |
| 1 crushed vantite | <ItemLink id="autarkia:blast_smelter" /> | 200 ticks, 64 FE/t | 1 <ItemLink id="autarkia:vantite_ingot" /> |

## Next

- Use the <ItemLink id="autarkia:froth_flotation_plant" />. With the jaw crusher this gives 4 ingots for each raw
  vantite.
- Without flotation, wash the crushed ore in the sluice for <ItemLink id="autarkia:washed_vantite" />.
