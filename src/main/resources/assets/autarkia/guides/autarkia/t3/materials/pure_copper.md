---
navigation:
  title: Pure Copper Ore
  parent: t3/materials.md
  position: 20
  icon: autarkia:pure_copper
item_ids:
  - autarkia:pure_copper
---

# Pure Copper Ore

<ItemImage id="autarkia:pure_copper" scale="2" />

Pure copper ore is the clean ore of the Tier 3 ore line. Each piece gives 1 copper ingot. One raw copper gives 3 pure copper ore through the full line.

## Get it

The <ItemLink id="autarkia:magnetic_separator" /> makes it. No world generation and no crafting recipe make pure ore.

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 2 <ItemLink id="autarkia:washed_copper" /> | <ItemLink id="autarkia:magnetic_separator" /> | 120 ticks | 128 FE/t | 3 pure copper ore, 1 <ItemLink id="autarkia:slag" />, 1 gold nugget at 50% |

The full line is: raw copper in the <ItemLink id="autarkia:jaw_crusher" /> (2 crushed copper), crushed copper in the <ItemLink id="autarkia:sluice" /> (washed copper), washed copper in the separator.

## Use it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 pure copper ore | furnace | 200 ticks | none | 1 copper ingot |
| 1 pure copper ore | blast furnace | 100 ticks | none | 1 copper ingot |
| 1 pure copper ore | <ItemLink id="autarkia:electrolyzer" /> | 160 ticks | 256 FE/t | 1 copper ingot, 1 gold nugget at 10% |

## Next

- Copper makes the <ItemLink id="autarkia:copper_coil" /> and the <ItemLink id="autarkia:copper_wire_spool" />. Basic circuits and servos need copper coils.
