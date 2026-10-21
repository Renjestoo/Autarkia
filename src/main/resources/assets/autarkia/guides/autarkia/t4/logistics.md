---
navigation:
  title: Cable Cars, Launchers and Depots
  parent: t4/index.md
  position: 3
  icon: autarkia:cable_car_station
item_ids:
  - autarkia:cable_car_station
  - autarkia:crate_launcher
  - autarkia:catch_net
  - autarkia:depot
---

# Cable Cars, Launchers and Depots

Tier 4 moves whole crates across the land.

## Cable car

A <ItemLink id="autarkia:cable_car_station" /> is 2 x 3 x 2. It has 27 input slots, 27 output slots and 2 slots for
gondola parts. Use a wrench on station A, then on station B.

- Horizontal distance 16 to 160 blocks, height difference at most 48 blocks.
- Both cables and the space 2 blocks under them must be free. If not, the chat names the first blocked position.
- A loop of two cables, 1.5 blocks apart, sags 3% of its length at the middle.
- 1 to 8 gondolas, at most 2 for each 16 blocks of length. Each gondola costs 4 steel plates and 2 iron chains from
  the parts slots. Each gondola carries 27 stacks.
- Gondolas move at 6 blocks per second. At a station a gondola waits 60 ticks: it unloads, then loads. It leaves when
  it is full, or after 200 ticks.
- The line uses 64 FE/t while it moves or serves a stop. A parked line uses nothing. Buffer 6,400 FE.

### Wind

The gondolas swing in the wind. Above 30 degrees the line runs at half speed. Above 45 degrees it stops (WIND_HOLD).
It runs again after 100 ticks below 20 degrees. A gondola that swings into a block stops (OBSTRUCTED) and loses no
items. The line goes on when the block is gone. If a station breaks, the gondolas fall and land as crates with their
contents.

## Crate launcher and catch net

A <ItemLink id="autarkia:crate_launcher" /> is 1 x 2 x 1 with 27 input slots, a slot for 16 empty crates and a
16,000 FE buffer. Use a wrench on the launcher, then on a <ItemLink id="autarkia:catch_net" /> (3 x 1 x 3).

- Range: 8 to 96 blocks horizontal, from 32 blocks down to 24 blocks up.
- It fires when the input is full, or when no new item came for 200 ticks. All 27 slots go into one crate.
- Each shot needs 1 empty crate and 1,000 FE + 40 FE per block of distance. Cooldown 60 ticks.
- It tries the angles 45, 55, 65 and 75 degrees and uses the lowest one with a clear path. It aims with the wind of
  the moment of launch. With no clear angle, Jade shows "No clear arc".
- The net catches a crate that falls within 1.5 blocks of its center. It puts all items and the empty crate into the
  inventory directly below its center. Without space there, the crate bounces off.
- A miss lands as a crate block with all items. Hauler Crabs collect stray crates. After 3 misses in 12,000 ticks the
  launcher pauses ("Check arc and wind"). Reset it with its button. Inside a colony domain it resets by itself after
  6,000 ticks.
- A crate that hits an entity does 4 damage and loses half its speed.

## Depot and the linked stock view

A <ItemLink id="autarkia:depot" /> has 54 slots. A depot within 16 blocks of an outpost core belongs to that outpost.
Other depots belong to the main site. Workers fetch from depots and deliver to them like any colony storage.

The ledger terminal has a Sites tab. It shows the merged stock of the main site and of every outpost. A site that
had no update for 1,200 ticks and is not loaded shows in grey with "Last seen N min ago".

Request items from a remote site in the Sites tab. The source depot then pushes the items into a sender station next
to it (cable car station, crate launcher or capsule station). Without one, the order waits with "no_transport".

<RecipeFor id="autarkia:cable_car_station" />
