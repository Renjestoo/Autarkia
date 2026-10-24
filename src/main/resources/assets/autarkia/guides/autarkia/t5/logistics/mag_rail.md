---
navigation:
  title: Mag Rail
  parent: t5/logistics.md
  position: 20
  icon: autarkia:mag_rail
item_ids:
  - autarkia:mag_rail
---

# Mag Rail

<ItemImage id="autarkia:mag_rail" scale="2" />

The mag rail is the track of the mag rail line. Sleds float above it from one
<ItemLink id="autarkia:mag_rail_station" /> to another at up to 20 blocks per second. Each sled carries one full crate.

## Get it

<RecipeFor id="autarkia:mag_rail" />

## Use it

1. Place a <ItemLink id="autarkia:mag_rail_station" /> at the start.
2. Place mag rails in a chain from the station. A rail takes its direction from the way you face.
3. A rail connects to the next rail or station on the same level, one block up or one block down.
4. Make curves and slopes like a normal rail track. There are no junctions.
5. Place a second station at the end.

Every rail needs a solid block below it. A slope also needs a solid block below its high side. Minecarts and normal
rails do not use the mag rail.

## Numbers

| Property | Value |
|---|---|
| Rails per line | 2,048 at most (config `magRailMaxRails`) |
| Stations per line | 16 at most |
| Sleds per line | 32 at most (config `magRailMaxSleds`) |
| Sled speed | Up to 20 blocks per second |
| Collision | None |
| Output of one recipe | 16 rails |

## Problems

- A rail drops: the block below it is not solid. Place a solid block under it.
- A sled reaches a gap: it falls and lands as a crate block with all its items. Close the gap.
- A creature on the track takes 6 damage and a push from each sled.
- A closed loop of rails sends nothing. Use a line with two ends.

## Next

- Set up the <ItemLink id="autarkia:mag_rail_station" />.
