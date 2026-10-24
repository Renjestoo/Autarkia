---
navigation:
  title: Pure Gold Ore
  parent: t3/materials.md
  position: 30
  icon: autarkia:pure_gold
item_ids:
  - autarkia:pure_gold
---

# Pure Gold Ore

<ItemImage id="autarkia:pure_gold" scale="2" />

Pure gold ore is the clean ore of the Tier 3 ore line. Each piece gives 1 gold ingot. One raw gold gives 3 pure gold ore through the full line.

## Get it

The <ItemLink id="autarkia:magnetic_separator" /> makes it. No world generation and no crafting recipe make pure ore.

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 2 <ItemLink id="autarkia:washed_gold" /> | <ItemLink id="autarkia:magnetic_separator" /> | 120 ticks | 128 FE/t | 3 pure gold ore, 1 <ItemLink id="autarkia:slag" />, 1 copper nugget at 50% |

The full line is: raw gold in the <ItemLink id="autarkia:jaw_crusher" /> (2 crushed gold), crushed gold in the <ItemLink id="autarkia:sluice" /> (washed gold), washed gold in the separator.

## Use it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 pure gold ore | furnace | 200 ticks | none | 1 gold ingot |
| 1 pure gold ore | blast furnace | 100 ticks | none | 1 gold ingot |
| 1 pure gold ore | <ItemLink id="autarkia:electrolyzer" /> | 160 ticks | 256 FE/t | 1 gold ingot, 1 copper nugget at 10% |
