---
navigation:
  title: HV Lines and Frontier Power
  parent: t4/index.md
  position: 3
  icon: autarkia:hv_pylon
---

# HV Lines and Frontier Power

Tier 4 power is big and travels far. FE/t means FE (Forge Energy) per tick.

## HV lines

An HV line is a span of <ItemLink id="autarkia:hv_cable_spool" /> between two nodes. It carries 8,192 FE/t over spans
of up to 48 blocks. The <ItemLink id="autarkia:hv_pylon" />, the <ItemLink id="autarkia:transformer" /> and machine ports take HV
spans. A pylon feeds the machines that touch its base. A transformer joins an HV line to a line of another tier.

An HV span gets hot when it carries more than 8,192 FE/t. Too much heat snaps it. Rain and wind cool the span. Long
spans sway in the wind.

## Tier 4 generators

| Generator | Source | Output |
|---|---|---|
| <ItemLink id="autarkia:wind_tower" /> | Wind at the rotor, 8 blocks up | 512 to 4,096 FE/t |
| <ItemLink id="autarkia:hydro_turbine" /> | A water column above the turbine | 128 FE/t per block of water, at most 4,096 FE/t |
| <ItemLink id="autarkia:magma_tap" /> | A <ItemLink id="autarkia:magma_vent" /> or a lava source below it | 2,048 FE/t on a vent, 1,024 FE/t on lava |

Each generator gives up to 8,192 FE/t to the network. Connect each one to an HV pylon. The colony builds wind towers
and hydro turbines by itself when it is short of power.

<SubPages />
