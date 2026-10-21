---
navigation:
  title: Insulated Grid
  parent: t3/index.md
  position: 1
  icon: autarkia:steel_pylon
item_ids:
  - autarkia:insulated_cable_spool
  - autarkia:steel_pylon
  - autarkia:wall_insulator
  - autarkia:transformer
  - autarkia:fuse_box
  - autarkia:steam_turbine
  - autarkia:battery_bank
---

# Insulated Grid

<ItemLink id="autarkia:insulated_cable_spool" /> carries 1,024 FE/t over spans of up to 24 blocks. Put it on a
<ItemLink id="autarkia:steel_pylon" /> (6 spans) or on a <ItemLink id="autarkia:wall_insulator" /> (2 spans of up to
12 blocks). Machines next to the base of a pylon join its network.

## Transformer

A <ItemLink id="autarkia:transformer" /> joins two cable tiers. It moves up to the rating of the lower tier per tick
and loses 2%. Use the wrench to set the mode: Auto, A to B or B to A.

## Fuse box

A <ItemLink id="autarkia:fuse_box" /> sits in a line with exactly 2 spans of one tier. It opens at 100% load for 1
second, at 150% at once, or when lightning reaches it. It closes again after 10 seconds. After 3 trips in one minute it
stays open until you reset it.

## Steam turbine and battery

The <ItemLink id="autarkia:steam_turbine" /> is 3 x 3 x 2 and takes steam from 1 to 4 boilers that touch its back or
sides. The rotor needs 30 seconds to spin up. The <ItemLink id="autarkia:battery_bank" /> stores 400,000 FE and charges
an FE item in its slot.

<RecipeFor id="autarkia:fuse_box" />
