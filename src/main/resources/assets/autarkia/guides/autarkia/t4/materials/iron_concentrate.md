---
navigation:
  title: Iron Concentrate
  parent: t4/materials.md
  position: 140
  icon: autarkia:iron_concentrate
item_ids:
  - autarkia:iron_concentrate
---

# Iron Concentrate

<ItemImage id="autarkia:iron_concentrate" scale="2" />

Iron concentrate comes from the froth flotation plant. 1 crushed iron ore gives 2 concentrate, and each concentrate
gives 1 iron ingot. With the jaw crusher this is 4 iron ingots for each raw iron.

## Get it

| Input | Machine | Time | Output |
|---|---|---|---|
| 1 <ItemLink id="autarkia:crushed_iron" /> and 250 mB water | <ItemLink id="autarkia:froth_flotation_plant" /> | 200 ticks, 768 FE/t | 2 iron concentrate, <ItemLink id="autarkia:slag" /> (50%), <ItemLink id="autarkia:zinc_nugget" /> (35%) |

## Use it

| Input | Machine | Time | Output |
|---|---|---|---|
| 1 iron concentrate | Furnace | 200 ticks | 1 iron ingot |
| 1 iron concentrate | Blast furnace | 100 ticks | 1 iron ingot |
| 1 iron concentrate | <ItemLink id="autarkia:electrolyzer" /> | 160 ticks, 256 FE/t | 1 iron ingot, zinc nugget (10%) |

The <ItemLink id="autarkia:blast_smelter" /> also smelts it, like a furnace.

## Next

- Use the iron for <ItemLink id="autarkia:steel_ingot" /> and then <ItemLink id="autarkia:vantite_steel_ingot" />.
