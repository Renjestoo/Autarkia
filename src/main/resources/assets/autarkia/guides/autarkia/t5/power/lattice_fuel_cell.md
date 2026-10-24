---
navigation:
  title: Lattice Fuel Cell
  parent: t5/power.md
  position: 50
  icon: autarkia:lattice_fuel_cell
item_ids:
  - autarkia:lattice_fuel_cell
---

# Lattice Fuel Cell

<ItemImage id="autarkia:lattice_fuel_cell" scale="2" />

The lattice fuel cell is the fuel of the plasma generator. One cell gives 78,643,200 FE.

## Get it

The <ItemLink id="autarkia:fabricator" /> makes it. Fabricator recipes do not show in this book, so the table gives
the recipe.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 2 <ItemLink id="autarkia:lattice_shard" />, 1 <ItemLink id="autarkia:vantite_steel_ingot" />, 1 <ItemLink id="minecraft:blaze_powder" /> | <ItemLink id="autarkia:fabricator" /> | 200 ticks (10 s) | 160 FE/t | 1 lattice fuel cell |

## Use it

1. Open the <ItemLink id="autarkia:plasma_generator" />.
2. Put the cells into the fuel slot. The slot holds up to 16 cells.
3. Push "Ignite" when the generator is cold.

The generator loads the next cell when the fuel that is left cannot pay for one more tick.

## Numbers

| Property | Value |
|---|---|
| Energy per cell | 78,643,200 FE |
| Burn time at full output | 2,400 ticks (2 minutes) at 32,768 FE/t |
| Cells in the fuel slot | 16 at most |

## Next

- Burn the cells in the <ItemLink id="autarkia:plasma_generator" />.
