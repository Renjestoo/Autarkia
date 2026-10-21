---
navigation:
  title: HV Lines and Frontier Power
  parent: t4/index.md
  position: 1
  icon: autarkia:hv_pylon
item_ids:
  - autarkia:hv_cable_spool
  - autarkia:hv_pylon
  - autarkia:hydro_turbine
  - autarkia:wind_tower
  - autarkia:magma_tap
  - autarkia:magma_vent
---

# HV Lines and Frontier Power

<ItemLink id="autarkia:hv_cable_spool" /> carries 8,192 FE/t over spans of up to 48 blocks. It loses 0.0002 of the
flow per block. Each span costs 1 spool per 16 blocks of length.

Only the <ItemLink id="autarkia:hv_pylon" /> takes HV spans. The pylon is 1 x 7 x 1 and holds 4 spans at its top. It
feeds the machines next to its base. A transformer joins an HV line to an insulated line.

## Heat and wind

An HV span gets hot above its rating. It stays at a stable glow up to about 1.34 times its rated load. At 2 times the
rated load, a cold span snaps after about 280 ticks. Rain and wind cool the span: the cooling factor is 1, plus 0.25 in
rain, plus 0.5 x wind strength.

Long spans sway in the wind. The sway is up to 0.04 x span length x wind strength, at most 2 blocks, with a period of
40 + 2 x span length ticks.

A snapped HV cable whips for 200 ticks. A hit does 10 damage and sets the target on fire for 40 ticks. The arcs can
light flammable blocks near the tip.

## Hydro turbine

The <ItemLink id="autarkia:hydro_turbine" /> is 3 x 3 x 3. Its intake is the top center position. The head is the
number of water blocks (source or flowing) in a straight column directly above the intake, at most 32.

- Output: 128 FE/t per block of head, at most 4,096 FE/t. The water is not used up.
- The outlet is the front face of the bottom center position. It must be air or water, else the output is 0.
- Buffer 50,000 FE, output up to 8,192 FE/t. The output is calculated again every 20 ticks.

## Wind tower

The <ItemLink id="autarkia:wind_tower" /> is 1 x 8 x 1 with a rotor on the top block.

- Base output: 512 FE/t, plus 20 FE/t for each block of rotor height above Y 64, from 512 to 3,072 FE/t.
- Wind multiplies the base by 0.5 to 1.5. Rain multiplies it by 1.15, thunder by 1.3.
- Each solid block in the 9 x 9 plane one block in front of the rotor removes 3%. With 25 or more blocks, the output
  is 0.
- Maximum 4,096 FE/t. Buffer 40,000 FE, output up to 8,192 FE/t.

The tower top sways in the wind. A rotor under a roof gets no wind.

## Magma tap

The <ItemLink id="autarkia:magma_tap" /> is 1 x 2 x 1. Its bottom position stands on a
<ItemLink id="autarkia:magma_vent" /> or on a lava source.

- On a magma vent: 2,048 FE/t, with no end.
- On a vanilla lava source: 1,024 FE/t. After 24,000 running ticks, the source turns into obsidian and the output
  stops.
- A water source must touch the upper block. Without it, the output is half and the tap smokes.
- Buffer 20,000 FE, output up to 8,192 FE/t.

Magma vents are on cave floors from Y -60 to -16 (in 1 of 8 chunks) and in the Nether from Y 10 to 60. A pickaxe
drops a magma block. Use Silk Touch to get the vent.

<RecipeFor id="autarkia:hv_pylon" />
