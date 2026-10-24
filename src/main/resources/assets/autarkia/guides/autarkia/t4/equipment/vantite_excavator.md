---
navigation:
  title: Vantite Excavator
  parent: t4/equipment.md
  position: 20
  icon: autarkia:vantite_excavator
item_ids:
  - autarkia:vantite_excavator
---

# Vantite Excavator

<ItemImage id="autarkia:vantite_excavator" scale="2" />

The vantite excavator is a powered pickaxe and shovel in one tool, at netherite level. It runs on FE and never
breaks. It mines one block, a 3 x 3 square, or a whole ore vein.

## Get it

| Inputs | Machine | Time | Output |
|---|---|---|---|
| 5 <ItemLink id="autarkia:vantite_steel_ingot" />, 2 <ItemLink id="autarkia:steel_gear" />, 2 <ItemLink id="autarkia:servo" />, 2 <ItemLink id="autarkia:advanced_circuit" />, 1 <ItemLink id="autarkia:battery_bank" />, 2 diamonds | <ItemLink id="autarkia:fabricator" /> | 400 ticks, 375 FE/t (150,000 FE) | 1 Vantite Excavator |

The excavator comes out empty. Charge it before you use it.

## Use it

1. Put the excavator into the item slot of a <ItemLink id="autarkia:battery_bank" /> (2,048 FE/t) or a
   <ItemLink id="autarkia:capacitor_bank" /> (512 FE/t) to charge it.
2. Press the mode key (default V) to change the mode. The action bar shows "Mode: 1x1", "Mode: 3x3" or "Mode: Vein".
3. In 1x1 mode, mine like with a pickaxe or a shovel. Each block costs 250 FE.
4. In 3x3 mode, mine a block. The tool also breaks the other blocks of the 3 x 3 square at right angles to the face
   you hit. It breaks only blocks that it can drop and that are at most 2 harder than the center block. It mines at
   half speed.
5. In vein mode, mine an ore. The tool also breaks the connected blocks of the same ore, up to 64 blocks. Stone and
   deepslate forms of one ore count as one vein. It mines at half speed. Each block costs 400 FE.

## Numbers

| Property | Value |
|---|---|
| Tool | Pickaxe and shovel, netherite level |
| Mining speed | 16 (8 in 3x3 and vein mode) |
| Attack damage | 7 |
| Energy | 2,000,000 FE |
| Charge rate | Up to 5,000 FE/t |
| Cost for each block | 250 FE, 400 FE in vein mode |
| Vein size | 64 blocks at most |
| Durability | None (it uses FE) |

The server option `tools.maxVeinBlocks` (default 64) changes the vein size.

## Problems

- The tool mines very slowly and drops nothing: it has 0 FE. Charge it.
- The vein stops early: the tool needs 400 FE for each block and stops when it runs low.
- "This item has no modes.": you pressed the mode key with another item in your hand.

## Next

- Use vein mode on <ItemLink id="autarkia:vantite_ore" />.
- The <ItemLink id="autarkia:grapple_launcher" /> takes you up cliffs and over ravines.
