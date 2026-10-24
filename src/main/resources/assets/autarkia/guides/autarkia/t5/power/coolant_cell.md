---
navigation:
  title: Coolant Cell
  parent: t5/power.md
  position: 10
  icon: autarkia:coolant_cell
item_ids:
  - autarkia:coolant_cell
---

# Coolant Cell

<ItemImage id="autarkia:coolant_cell" scale="2" />

The coolant cell keeps superconductors cold. A cryo node or a superconducting coil without coolant warms up, and a
warm superconductor quenches with a blast.

## Get it

The <ItemLink id="autarkia:fabricator" /> makes it. Fabricator recipes do not show in this book, so the table gives
the recipe.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:vantite_steel_ingot" />, 2 <ItemLink id="minecraft:blue_ice" />, 1 <ItemLink id="autarkia:lattice_shard" /> | <ItemLink id="autarkia:fabricator" /> | 200 ticks (10 s) | 160 FE/t | 2 coolant cells |

## Use it

1. Open a <ItemLink id="autarkia:cryo_node" /> or a <ItemLink id="autarkia:superconducting_coil" />.
2. Put one cell into each of the 4 coolant slots.
3. Watch the coolant time in the screen. Add new cells before it reaches 0.

The machine uses the units inside the cell in place. A used cell keeps its remaining units, and an empty cell
disappears. If you take the cells out, the cooling stops at once. Hoppers and other automation can put cells in, but
they cannot take them out.

## Numbers

| Property | Value |
|---|---|
| Coolant per cell | 24,000 units |
| Cells per slot | 1 |
| Cryo node use | 1 unit per tick, plus 1 per attached span above 50% load |
| Cryo node, 1 cell, no loaded span | 24,000 ticks (20 minutes) |
| Superconducting coil use | 2 units per tick |
| Superconducting coil, 1 cell | 12,000 ticks (10 minutes) |

The hover text of the coolant bar shows "units / capacity coolant units".

## Problems

- "Coolant: 0 s" in red: the cells are empty. Put in new cells now, or the superconductor warms up and quenches.

## Next

- Recipes that use a coolant cell: <ItemLink id="autarkia:cryo_node" /> (1), <ItemLink id="autarkia:plasma_generator" />
  (2), <ItemLink id="autarkia:disassembler_beam" /> (2), <ItemLink id="autarkia:nanite_multitool" /> (1) and each
  Lattice Carapace piece (1).
- Each quench raises the colony stock target for coolant cells by 8.
