---
navigation:
  title: Pure Iron Ore
  parent: t3/materials.md
  position: 10
  icon: autarkia:pure_iron
item_ids:
  - autarkia:pure_iron
---

# Pure Iron Ore

<ItemImage id="autarkia:pure_iron" scale="2" />

Pure iron ore is the clean ore of the Tier 3 ore line. Each piece gives 1 iron ingot. One raw iron gives 3 pure iron ore through the full line.

## Get it

The <ItemLink id="autarkia:magnetic_separator" /> makes it. No world generation and no crafting recipe make pure ore.

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 2 <ItemLink id="autarkia:washed_iron" /> | <ItemLink id="autarkia:magnetic_separator" /> | 120 ticks | 128 FE/t | 3 pure iron ore, 1 <ItemLink id="autarkia:slag" />, 1 <ItemLink id="autarkia:zinc_nugget" /> at 50% |

The full line is: raw iron in the <ItemLink id="autarkia:jaw_crusher" /> (2 crushed iron), crushed iron in the <ItemLink id="autarkia:sluice" /> (washed iron), washed iron in the separator.

## Use it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 pure iron ore | furnace | 200 ticks | none | 1 iron ingot |
| 1 pure iron ore | blast furnace | 100 ticks | none | 1 iron ingot |
| 1 pure iron ore | <ItemLink id="autarkia:electrolyzer" /> | 160 ticks | 256 FE/t | 1 iron ingot, 1 <ItemLink id="autarkia:zinc_nugget" /> at 10% |

## Next

- Iron ingots make <ItemLink id="autarkia:steel_ingot" /> in the <ItemLink id="autarkia:blast_smelter" />.
