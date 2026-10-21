---
navigation:
  title: Copper Wire
  parent: t2/index.md
  position: 1
  icon: autarkia:wooden_pole
item_ids:
  - autarkia:wooden_pole
  - autarkia:copper_wire_spool
  - autarkia:industrial_lamp
  - autarkia:capacitor_bank
  - autarkia:catwalk
  - autarkia:catwalk_stairs
  - autarkia:hazard_block
---

# Copper Wire

## Poles and wire

A <ItemLink id="autarkia:wooden_pole" /> is 3 blocks tall and takes up to 4 copper spans at its top. A span of
<ItemLink id="autarkia:copper_wire_spool" /> is max 16 blocks long and is rated for 256 FE/t. Machines next to the
base of a pole join its network.

<RecipeFor id="autarkia:wooden_pole" />

## Heat

Above its rating a copper span heats up. It glows orange from 40% heat and red from 70%, and it sags more. At 100%
it snaps with arcs and sparks that can set fire to wood. At twice its rating that takes about 8 seconds. Rain cools
wires.

## Lamps

The <ItemLink id="autarkia:industrial_lamp" /> gives light 15 for 1 FE/t. It flickers when its network is short of
power. When a wire snaps and cuts the supply, the lamps go dark one after another, starting at the break.

## Capacitor bank

The <ItemLink id="autarkia:capacitor_bank" /> stores 50,000 FE with 2,048 FE/t in and out. Four stripes on its front
show the charge. It also takes lightning energy without a limit.

<RecipeFor id="autarkia:capacitor_bank" />
