---
navigation:
  title: Outpost Seed
  parent: t4/colony.md
  position: 20
  icon: autarkia:outpost_seed
item_ids:
  - autarkia:outpost_seed
---

# Outpost Seed

<ItemImage id="autarkia:outpost_seed" scale="2" />

The Outpost Seed founds an outpost: a small second base of your colony at a far ore deposit. An outpost core grows
from the seed. The colony then builds the outpost, links it to the main base and mines there.

## Get it

| Inputs | Machine | Time | Output |
|---|---|---|---|
| 1 <ItemLink id="autarkia:advanced_circuit" />, 2 <ItemLink id="autarkia:servo" />, 1 <ItemLink id="autarkia:crate" />, 4 <ItemLink id="autarkia:vantite_steel_ingot" />, 1 oak sapling | <ItemLink id="autarkia:fabricator" /> | 400 ticks, 160 FE/t (64,000 FE) | 1 Outpost Seed |

The seed stacks to 16. The outpost core starts with the 64,000 FE of the fabrication.

## Use it

### Plant it yourself

1. Go to a place 64 to 512 blocks (horizontal) from your Colony Core.
2. Hold the seed. Use it on the top of a block. The outpost core needs a free 2 x 2 x 2 space there.
3. The outpost core grows for 200 ticks. Jade shows "Growing n%".

You must be the owner of the colony or a trusted player. The seed serves the colony it is linked to, else the
nearest colony that you manage in this dimension.

### Let the colony do it

1. Keep an Outpost Seed in colony storage.
2. The Core proposes an Outpost project when a known deposit lies far from the Core and the colony is short of its
   metal. These are the conditions:
   - The deposit is unclaimed.
   - It is more than the domain half-size + 16 blocks and at most 512 blocks from the Core.
   - The stock of its ingot stays below half of its stock target for 24,000 ticks. Without a target, the target is 64.
   - The colony has a free outpost slot.
3. A Porter or a Hauler Crab carries the seed to the site and plants it. A Sentinel escorts it when the colony has
   one.
4. When the core has grown, the nearest idle Builder Drone and the nearest idle carrier move to the outpost.
5. Workers build the outpost layout around the core: a <ItemLink id="autarkia:depot" />, an
   <ItemLink id="autarkia:hv_pylon" /> with an HV span to a <ItemLink id="autarkia:jaw_crusher" /> on foundation, a
   <ItemLink id="autarkia:wind_tower" /> and a <ItemLink id="autarkia:recharge_dock" />. An enabled OUTPOST Pattern
   in your Pattern library replaces this layout. It must hold an outpost core.

The colony knows a deposit only after a Surveyor scan reported its <ItemLink id="autarkia:ore_outcrop" />. A planted
outpost claims the nearest unclaimed known deposit within 32 blocks.

### After the outpost grows

- HV Link project: Cable-Layer Drones build a line of HV pylons from the outpost toward the main network, about one
  pylon every 44 blocks on the surface, one at a time. With the server option `outpost.hvLinkLamps` (default on) each
  pylon gets an industrial lamp beside its base. When the link runs at 80% load for 1,200 ticks, a second line goes
  3 blocks to the side.
- Cable Car project: when workers carry 256 or more items a day by hand between the outpost and the main site, the
  colony builds two <ItemLink id="autarkia:cable_car_station" /> blocks. One stands beside the outpost depot, one
  beside the nearest main storage at most 128 blocks away. The line runs 2 gondolas.

## Outpost rules

| Rule | Value |
|---|---|
| Distance to the Core | 64 to 512 blocks, horizontal |
| Distance to other outposts | 64 blocks or more |
| Outposts for each colony | 6 at Tier 4 |
| Outpost square | Half-size 24 at Tier 4, 32 at Tier 5 and 6. It is part of the domain. |
| Outpost core | 2 x 2 x 2 blocks, grows in 200 ticks |
| Power use of the core | 256 FE/t from the outpost network |
| Worker charger | The core charges one worker at a time from its own store |
| Depots | Every depot within 16 blocks of the core belongs to the outpost |

The outpost core keeps its own chunk loaded. The server option `outpost.chunkTickets` (default on) controls this. On a
dedicated server the world still pauses when nobody is online, unless the server owner sets
`pause-when-empty-seconds=0`. Breaking the outpost core removes the outpost.

### Catch-up

While all chunks of the outpost square are loaded, the outpost core measures what enters its depots, every 1,200
ticks. While the outpost is not loaded, it works on paper:

1. It needs 24,000 ticks of measured production first.
2. Every 1,200 ticks it adds the measured items per tick, times the time away, times 0.75, times its power share.
3. The time away counts at most 72,000 ticks.
4. Ore products lower the estimate of the claimed deposit. They stop when the deposit is empty.
5. Items that do not fit in the depots go into a virtual buffer of 4,096 items. The rest is lost.

The power share is the part of the last 1,200 ticks in which the core got its 256 FE/t.

### Outpost core menu and Jade

| Line | Meaning |
|---|---|
| "Outpost n/m" | The number of this outpost, and the outposts the tier allows |
| "Link: none", "Link: HV", "Link: cable car", "Link: HV and cable car" | How the outpost connects to the main base |
| "Last catch-up: +n items" | The items of the last catch-up |
| "Power: n%" | The power share. Below 100% the menu shows it in red. |
| "Profile: n min" | How long the outpost measured its production |
| "Virtual buffer: n items" | Items that wait for room in the depots |
| "Growing n%" (Jade) | The core still grows |
| "No colony" (Jade) | The core belongs to no colony |

## Problems

- "You manage no colony this seed can serve": you are not the owner or a trusted player of a colony in this
  dimension.
- "Outposts need an active colony of tier 4 or higher": upgrade the Core with the
  <ItemLink id="autarkia:core_upgrade_t4" />.
- "This colony has all the outposts its tier allows": the colony has 6 outposts.
- "An outpost must be 64 to 512 blocks from the Core": go nearer or farther.
- "Too close to another outpost (64 blocks minimum)": move away from the other outpost.
- "The outpost area would overlap another colony": the outpost square touches the domain of another colony.
- "The 2 x 2 x 2 space for the outpost core must be free": clear the space above the block.

## Next

- Put a <ItemLink id="autarkia:depot" /> at the outpost if your own layout has none.
- Link the outpost: <ItemLink id="autarkia:hv_pylon" /> lines for power, a
  <ItemLink id="autarkia:cable_car_station" /> line or a <ItemLink id="autarkia:crate_launcher" /> for goods.
