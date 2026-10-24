---
navigation:
  title: Pure Zinc Ore
  parent: t3/materials.md
  position: 50
  icon: autarkia:pure_zinc
item_ids:
  - autarkia:pure_zinc
---

# Pure Zinc Ore

<ItemImage id="autarkia:pure_zinc" scale="2" />

Pure zinc ore is the clean ore of the Tier 3 ore line. Each piece gives 1 zinc ingot. One raw zinc gives 3 pure zinc ore through the full line.

## Get it

The <ItemLink id="autarkia:magnetic_separator" /> makes it. No world generation and no crafting recipe make pure ore.

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 2 <ItemLink id="autarkia:washed_zinc" /> | <ItemLink id="autarkia:magnetic_separator" /> | 120 ticks | 128 FE/t | 3 pure zinc ore, 1 <ItemLink id="autarkia:slag" />, 1 copper nugget at 50% |

The full line is: <ItemLink id="autarkia:raw_zinc" /> in the <ItemLink id="autarkia:jaw_crusher" /> (2 crushed zinc), crushed zinc in the <ItemLink id="autarkia:sluice" /> (washed zinc), washed zinc in the separator.

## Use it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 pure zinc ore | furnace | 200 ticks | none | 1 <ItemLink id="autarkia:zinc_ingot" /> |
| 1 pure zinc ore | blast furnace | 100 ticks | none | 1 zinc ingot |
| 1 pure zinc ore | <ItemLink id="autarkia:electrolyzer" /> | 160 ticks | 256 FE/t | 1 zinc ingot, 1 copper nugget at 10% |

## Next

- Zinc and copper make <ItemLink id="autarkia:brass_ingot" />.
- The <ItemLink id="autarkia:battery_bank" /> needs 2 <ItemLink id="autarkia:zinc_block" />.
