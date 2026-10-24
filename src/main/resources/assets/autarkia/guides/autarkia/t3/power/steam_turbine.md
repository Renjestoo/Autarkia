---
navigation:
  title: Steam Turbine
  parent: t3/power.md
  position: 70
  icon: autarkia:steam_turbine
item_ids:
  - autarkia:steam_turbine
---

# Steam Turbine

<ItemImage id="autarkia:steam_turbine" scale="2" />

The steam turbine is the main generator of Tier 3. It takes steam from up to 4 boilers and makes up to 1,000 FE/t. The rotor needs time to spin up, so the turbine is good for a steady load.

## Get it

<RecipeFor id="autarkia:steam_turbine" />

The recipe uses a <ItemLink id="autarkia:steam_engine" />.

## Use it

1. Find a space of 3 x 3 x 2 blocks.
2. Place the turbine.
3. Put 1 to 4 <ItemLink id="autarkia:boiler" /> blocks against its back or its sides.
4. Give the boilers water and fuel.
5. Connect the turbine: put a <ItemLink id="autarkia:steel_pylon" /> next to it, or string a span to one of its two bushings.
6. Wait. The rotor needs about 30 seconds to reach full speed.

The turbine takes steam only while its buffer has 1,000 FE of free space. It takes the steam equally from all touching boilers.

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 3 x 2 |
| Steam | up to 100 mB per tick |
| Boilers | 1 to 4, at the back or the sides |
| Output | rotor x steam x 10 FE/t, max 1,000 FE/t |
| Spin up | about 30 s at 10 mB/t or more |
| Spin down | about 60 s without steam |
| Buffer | 20,000 FE |
| Out | 1,024 FE/t |

Example: at full rotor speed, 100 mB of steam per tick gives 1,000 FE/t. A boiler makes 10 mB of steam per tick for each fire level, up to level 4.

## Screen and Jade

- The dial and "Rotor N%": the rotor speed.
- "Steam: N mB/t": the steam the turbine took in the last tick.
- "Out: N FE/t": the energy made in the last tick.
- The bar at the left: the buffer.
- Jade shows "Output: N FE/t", "Buffer: N / M FE" and "Rotor: N%".

## Problems

- "Steam: 0 mB/t": no boiler touches the back or a side, or the boilers have no steam. The front does not take steam.
- The rotor slows down: the turbine gets less than 10 mB of steam per tick. Add boilers or raise the fire level.
- "Steam: 0 mB/t" with a full buffer: the network takes no energy. Connect consumers or a <ItemLink id="autarkia:battery_bank" />.

## Next

- <ItemLink id="autarkia:battery_bank" /> stores the output.
- At Tier 3 the colony builds a Turbine Hall by itself when its power is short.
- <ItemLink id="autarkia:hydro_turbine" /> is a Tier 4 generator.
