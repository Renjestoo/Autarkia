---
navigation:
  title: Industrial Lamp
  parent: t2/power.md
  position: 30
  icon: autarkia:industrial_lamp
item_ids:
  - autarkia:industrial_lamp
---

# Industrial Lamp

<ItemImage id="autarkia:industrial_lamp" scale="2" />

The industrial lamp is an electric light. It gives light level 15 while it has power. It uses only 1 FE/t. The lamps of
a line also show you the state of the line: they flicker when power is short and go dark when a span snaps.

## Get it

One craft gives 2 lamps.

<RecipeFor id="autarkia:industrial_lamp" />

## Use it

1. Place the lamp next to the bottom block of a <ItemLink id="autarkia:wooden_pole" />, or next to a
   <ItemLink id="autarkia:rope_pulley" />.
2. Or connect a <ItemLink id="autarkia:copper_wire_spool" /> span to the bushing on top of the lamp.
3. The lamp lights when its small buffer has energy.

## What the light tells you

- Steady light: the network keeps the lamp full.
- Flicker: the buffer of the lamp is below half. The network has not enough power for all its machines.
- Dark: the lamp has no energy. When a span snaps and the lamps lose their supply, they go dark one after another.
  A lamp goes dark 3 ticks for each node between it and the break. The first dark lamp shows where the break is.

## Numbers

| Property | Value |
|---|---|
| Light level | 15 while lit, 0 when dark |
| Use | 1 FE/t |
| Buffer | 40 FE |
| Input | up to 2 FE/t |
| Flickers below | 20 FE in the buffer |
| Spans on the lamp | 2 (copper or other metal cable) |

Jade shows "Buffer: 40 / 40 FE": the energy in the lamp.

## Next

- Store energy for dark hours in a <ItemLink id="autarkia:capacitor_bank" />.
- Mark dangerous places with the <ItemLink id="autarkia:hazard_block" />.
