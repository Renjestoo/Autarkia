---
navigation:
  title: Zinc Concentrate
  parent: t4/materials.md
  position: 180
  icon: autarkia:zinc_concentrate
item_ids:
  - autarkia:zinc_concentrate
---

# Zinc Concentrate

<ItemImage id="autarkia:zinc_concentrate" scale="2" />

Zinc concentrate comes from the froth flotation plant. 1 crushed zinc ore gives 2 concentrate, and each concentrate
gives 1 zinc ingot. With the jaw crusher this is 4 zinc ingots for each raw zinc.

## Get it

| Input | Machine | Time | Output |
|---|---|---|---|
| 1 <ItemLink id="autarkia:crushed_zinc" /> and 250 mB water | <ItemLink id="autarkia:froth_flotation_plant" /> | 200 ticks, 768 FE/t | 2 zinc concentrate, <ItemLink id="autarkia:slag" /> (50%), copper nugget (35%) |

## Use it

| Input | Machine | Time | Output |
|---|---|---|---|
| 1 zinc concentrate | Furnace | 200 ticks | 1 <ItemLink id="autarkia:zinc_ingot" /> |
| 1 zinc concentrate | Blast furnace | 100 ticks | 1 zinc ingot |
| 1 zinc concentrate | <ItemLink id="autarkia:electrolyzer" /> | 160 ticks, 256 FE/t | 1 zinc ingot, copper nugget (10%) |

The <ItemLink id="autarkia:blast_smelter" /> also smelts it, like a furnace.

## Next

- Zinc and copper make brass in the blast smelter.
