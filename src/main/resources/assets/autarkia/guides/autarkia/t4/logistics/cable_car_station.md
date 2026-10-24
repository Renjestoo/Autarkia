---
navigation:
  title: Cable Car Station
  parent: t4/logistics.md
  position: 20
  icon: autarkia:cable_car_station
item_ids:
  - autarkia:cable_car_station
---

# Cable Car Station

<ItemImage id="autarkia:cable_car_station" scale="2" />

The cable car station is one end of a cable car line, 2 x 3 x 2 blocks. Two stations hold a loop of two cables.
Gondolas ride the loop and carry 27 stacks each. A line crosses valleys, rivers and ravines.

## Get it

<RecipeFor id="autarkia:cable_car_station" />

## Use it

1. Place station A. Place station B 16 to 160 blocks away (horizontal). The height difference can be at most 48
   blocks.
2. Keep both cables free. The cables run 1.5 blocks apart, near the top of the stations. The space where the gondolas
   hang under the cables must be free too.
3. Use a <ItemLink id="autarkia:wrench" /> on station A. The action bar shows "Station marked. Use the wrench on the
   second station."
4. Use the wrench on station B within 600 ticks. The chat shows "Cable line linked: length blocks".
5. Put steel plates and iron chains into the gondola part slots of a station. Each gondola needs 4
   <ItemLink id="autarkia:steel_plate" /> and 2 iron chains.
6. Set the number of gondolas with "-" and "+". The new number applies at the next stop.
7. Give both stations power. The line uses 64 FE/t while it moves or serves a stop.
8. Put items into the input of a station, by hand, with a hopper or from a depot. Take them from the output of the
   other station.

### How the line runs

- All gondolas grip one loop at even spacing. The loop stops when a gondola reaches a station.
- At a station a gondola first unloads into the output, then loads from the input.
- The loop leaves after 60 ticks when every stopped gondola is full, else after 200 ticks.
- With both inputs empty and every gondola empty, the line parks and uses no power.
- Gondolas move at 6 blocks per second.
- The line runs only while every chunk under it is loaded.

### Wind

The gondolas swing in the wind, along and across the cable. Above 30 degrees of swing the line runs at half speed.
Above 45 degrees it stops ("Wind hold"). It goes on after the swing stays below 20 degrees for 100 ticks.

A gondola that swings into a block stops the line ("Obstructed"). It loses no items. The line goes on when the block
is gone. If a station breaks, all gondolas fall. Each one lands as a crate block with its cargo, and its parts drop.

### Menu

| Part | What it does |
|---|---|
| "Input" and "Output" buttons | Show the 27 input slots or the 27 output slots |
| "Gondola parts" slots (2) | Steel plates and iron chains only |
| "-", "n / max", "+" | The gondola count you set, and the most this line can hold |
| "Swing n°" and the gauge | The largest gondola swing. Green below 30, orange to 45, red from 45 degrees. |
| "Line: n blocks" or "Not linked" | The line length. Point at it to see "Linked to x y z". |
| State text | See Problems |
| Energy bar (bottom) | Stored FE. Point at it to see the amount. |

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 3 x 2 blocks |
| Line length | 16 to 160 blocks horizontal |
| Height difference | 48 blocks at most |
| Gondolas | 1 to 8, at most 2 for each 16 blocks of length |
| Gondola cargo | 27 stacks |
| Gondola parts | 4 steel plates, 2 iron chains |
| Speed | 6 blocks per second |
| Stop | 60 to 200 ticks |
| Power use | 64 FE/t while running or at a stop, 0 when parked |
| Energy buffer | 6,400 FE in each station |
| Cable sag | 3% of the length at the middle |
| Slow, hold, resume | 30, 45, 20 degrees of swing |
| Mine with | Iron pickaxe or better |

The server options `cableCarMaxLength` (default 160) and `gondolaHoldDegrees` (default 45) change the length limit and
the hold angle.

## Problems

Jade shows "Line length n blocks, gondolas n", the state and "Swing: n°". A gondola shows "Cargo n stacks. Swing n°".

- "Not linked": use the wrench on both stations.
- "Too short: n blocks (16 to 160)" or "Too long: n blocks (16 to 160)": move a station.
- "Height difference n blocks (at most 48)": the two stations differ too much in height.
- "Blocked at x y z": a block stands in the cable path or in the gondola space. Remove it.
- "This station is already linked to x y z": a station takes one line only.
- "Use the wrench on a different station" or "The first station is gone": start again on station A.
- "No gondolas": put 4 steel plates and 2 iron chains into the part slots.
- "No power": both stations together have less than 64 FE.
- "Slowed by wind": the swing is above 30 degrees. The line runs at half speed.
- "Wind hold": the swing is above 45 degrees. Wait for calmer wind.
- "Obstructed at x y z": a gondola touches this block. Remove it.
- "Chunks not loaded": a chunk under the line is not loaded. The line waits.
- "Parked": nothing to carry. This is normal.

## Next

- Put a <ItemLink id="autarkia:depot" /> next to the outpost station. Orders from that site then ride the line.
- The colony builds a cable car line by itself when workers carry many items by hand between an outpost and the
  main site.
