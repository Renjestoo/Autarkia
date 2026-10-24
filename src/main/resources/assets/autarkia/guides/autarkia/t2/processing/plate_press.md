---
navigation:
  title: Plate Press
  parent: t2/processing.md
  position: 10
  icon: autarkia:plate_press
item_ids:
  - autarkia:plate_press
---

# Plate Press

<ItemImage id="autarkia:plate_press" scale="2" />

The plate press is a powered machine. It presses one ingot into one plate. Many Tier 2 machines need
<ItemLink id="autarkia:brass_plate" /> and <ItemLink id="autarkia:steel_plate" />, so make this machine first.

## Get it

<RecipeFor id="autarkia:plate_press" />

Make the brass ingots in a <ItemLink id="autarkia:crucible" /> (3 copper ingots and 1 zinc ingot make 4 brass ingots).

## Use it

1. Place the plate press. It is one block.
2. Connect it to a power network. A machine next to the base of a <ItemLink id="autarkia:wooden_pole" /> joins the
   network of that pole. Read [Copper Wire](../power.md) for the network.
3. Use the plate press to open its menu.
4. Put brass ingots or steel ingots in the input slot.
5. Take the plates from the output slot.

Hoppers and belts can put ingots into the input slot and take plates from the output slot.

### Recipes

| Input | Time | Output |
|---|---|---|
| 1 <ItemLink id="autarkia:brass_ingot" /> | 80 ticks (4 s) | 1 <ItemLink id="autarkia:brass_plate" /> |
| 1 <ItemLink id="autarkia:steel_ingot" /> | 80 ticks (4 s) | 1 <ItemLink id="autarkia:steel_plate" /> |

## Numbers

| Property | Value |
|---|---|
| Size | 1 block |
| Slots | 1 input, 1 output |
| Power use | 48 FE/t while it works |
| Energy per plate | 3,840 FE |
| Energy buffer | 9,600 FE |
| Maximum input | 192 FE/t |
| Mine with | Pickaxe |

## Problems

- "Idle": the input slot has no ingot that the press can use. Put in a brass ingot or a steel ingot.
- "No power": the buffer has less than 48 FE. Connect the press to a network, or add generators.
- "Output full": the output slot is full. Take out the plates, or let a hopper or belt take them.

Jade also shows "Energy: stored / capacity FE" and "Queue: n", the number of items in the input slot.

## Next

- Brass plates make the <ItemLink id="autarkia:sluice" />, the <ItemLink id="autarkia:blast_smelter" />, the
  <ItemLink id="autarkia:capacitor_bank" /> and the <ItemLink id="autarkia:pattern_loom" />.
- Steel plates make the <ItemLink id="autarkia:steel_gear" />, the <ItemLink id="autarkia:boiler" />, the
  <ItemLink id="autarkia:steam_engine" /> and the steel armor.
