---
navigation:
  title: Windmill
  parent: t1/power.md
  position: 50
  icon: autarkia:windmill
item_ids:
  - autarkia:windmill
---

# Windmill

<ItemImage id="autarkia:windmill" scale="2" />

The windmill is a generator that the wind turns. It needs no player, no animal and no fuel. A high rotor in open air
gives the most power.

## Get it

<RecipeFor id="autarkia:windmill" />

## Use it

1. Find a high place with open air. A hill or a tower is good.
2. Place the windmill. It is 4 blocks tall. The rotor is on the top block.
3. Keep the 5 x 5 area in front of the rotor free of solid blocks.
4. Connect the windmill to a node with a <ItemLink id="autarkia:rope_coil" />.
5. Look at the windmill with Jade. It shows the output, the wind and the number of blocked blocks.

## Numbers

The windmill calculates its output again every 20 ticks:

Output = base x wind x weather x free area, at most 40 FE/t.

| Factor | Value |
|---|---|
| Base | 4 + 0.4 for each block of rotor height above Y 62, from 4 to 32 |
| Wind | 0.5 to 1.5 |
| Weather | Clear 1.0, rain 1.15, thunder 1.3 |
| Free area | 10% less for each solid block in front of the rotor; 0 at 5 or more blocks |

A rotor at Y 62 or lower has a base of 4. A rotor at Y 132 or higher has the full base of 32.

| Property | Value |
|---|---|
| Output | 4 to 40 FE/t |
| Buffer | 2,000 FE |
| Maximum output to the network | 40 FE/t |
| Size | 1 x 4 x 1 |
| Mining tool | Axe |

## Problems

- The output is 0: 5 or more solid blocks are in the 5 x 5 area in front of the rotor. Remove them.
- "Wind data: none" in Jade: the world has no wind data yet. The windmill then uses a wind factor of 1.0.

## Next

- The colony builds windmills by itself on high, open sites when the power is 25% short.
- In Tier 4 the wind tower makes much more power.
