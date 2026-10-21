---
navigation:
  title: Water and Steam
  parent: t2/index.md
  position: 2
  icon: autarkia:boiler
item_ids:
  - autarkia:waterwheel
  - autarkia:boiler
  - autarkia:pressure_valve
  - autarkia:steam_engine
---

# Water and Steam

## Waterwheel

The <ItemLink id="autarkia:waterwheel" /> is 3 wide and 3 tall. Flowing water under its bottom row gives 24 FE/t per
block, falling water on its top 40 FE/t per block, up to 128 FE/t. Still water sources give nothing.

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="/structures/waterwheel.nbt" />
</GameScene>

## Boiler and steam engine

The <ItemLink id="autarkia:boiler" /> burns fuel at fire level 0 to 4 and turns water into steam. It takes water from a
bucket, from pipes, or from water sources next to its bottom. A <ItemLink id="autarkia:steam_engine" /> with its back
against the boiler makes up to 120 FE/t. One boiler feeds two engines.

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="/structures/boiler_engine.nbt" />
</GameScene>

## Pressure

Pressure is steam / 400 bar. Above 9 bar each <ItemLink id="autarkia:pressure_valve" /> on the top vents steam with a
scream. From 10 bar the gauge is red, at 11 bar the ground shakes, and at 12 bar the boiler bursts. One valve cannot
hold fire level 4 without a load. Two valves can. A boiler that runs dry and hot also bursts.

<RecipeFor id="autarkia:boiler" />
