---
navigation:
  title: Capsule Station
  parent: t3/logistics.md
  position: 50
  icon: autarkia:capsule_station
item_ids:
  - autarkia:capsule_station
---

# Capsule Station

<ItemImage id="autarkia:capsule_station" scale="2" />

The capsule station sends and receives capsules in a <ItemLink id="autarkia:capsule_tube" /> network. Each capsule carries one stack. Filters and priorities tell the network where each stack goes. Use it to move items between machines, storage and bases without belts or workers.

## Get it

<RecipeFor id="autarkia:capsule_station" />

## Build a network

1. Lay the <ItemLink id="autarkia:capsule_tube" /> line first.
2. Hold the station and click the face of the end tube. The mouth of the station points into that tube.
3. If no tube is at the clicked face, the mouth points at a tube next to the station, in the order of your view. With no tube next to the station, it points up.
4. Connect each station to the grid: put a node next to it, or string a span to its bushing.
5. Open each station and set its mode, filter and priority.

The mouth is the one face that connects to the tube. The other 5 faces are for hoppers and chutes: they put items into the input buffer and take items out of the output buffer. A wrench does not turn a station. To change the mouth, break the station and place it again.

## Modes

| Mode | What the station does |
|---|---|
| Send | sends the stacks of its input buffer; it receives nothing |
| Receive | receives capsules into its output buffer; it sends nothing (default) |
| Both | sends its input and receives into its output |

The filter and the priority are for receiving only.

## Filter

The filter is a 3 x 3 grid of ghost slots on the right side of the screen.

1. Click a filter slot with an item on the cursor. The slot shows a copy of the item. The item stays on your cursor.
2. Click a filter slot with an empty cursor to clear it.
3. Push **Whitelist** or **Blacklist** to switch the rule.
4. Push **Exact** or **Item only** to switch the match.

- Whitelist: the station receives only the listed items.
- Blacklist: the station receives all items except the listed items.
- Exact: the item and all its data (for example damage and enchantments) must match.
- Item only: the item type must match.
- Empty filter: the station receives any item. It is a default sink.

## Routing order

When a station sends a stack, the network chooses the target in this order:

1. Receiving stations with a filter that the stack passes.
2. Then receiving stations with an empty filter.
3. In each group: the highest priority first (0 to 9).
4. At equal priority: the shortest tube route first.
5. A station is a target only when its output buffer has space for the stack. The capsules that already go to it count too.

A station never sends to itself. When no target has space, the stack stays in the input buffer. Jade then shows "Next target: none". The station always tries its first input stack. While that stack has no target, the stacks behind it also wait.

## Send and travel

1. Every 10 ticks, each sending station launches the stack of its first input slot that is not empty.
2. A launch costs 40 FE. The station must have 40 FE in its buffer.
3. The capsule flies along the shortest route at 8 blocks per second.
4. At the target mouth, the capsule puts its stack into the output buffer.

So one station sends at most 2 stacks per second. A network holds at most 64 capsules. At the limit, new launches wait.

## When a capsule cannot unload

1. The capsule waits at the target mouth for 30 seconds.
2. Then it looks for a new target with the routing order.
3. If there is no new target, it goes back to its home station and into the input buffer.
4. If the home station is also full, the capsule is clogged. It tries again every 5 seconds.

A clogged network shows "Clogged". The stopped capsules glow red. The tubes hiss and give smoke. Capsules that are not blocked keep moving.

## Fix a clogged network

1. Look at the stations with Jade. Find the station with a full output buffer.
2. Empty its output buffer, or put a hopper or a chute under it.
3. Add a station in Receive mode with an empty filter as an overflow sink.
4. Check the filters. Make sure that each item that you send has a target.
5. As a last step, break the tube where the capsule waits. The items drop.

If a tube breaks, the capsules in the cut-off part go to a station in their part, or they drop as items.

## Screen

| Part | Use |
|---|---|
| Send, Receive, Both | set the mode; the active mode is gray |
| - and + with the number | set the priority 0 to 9 |
| Input (left 3 x 3) | stacks to send; shift-click puts items here |
| Output (middle 3 x 3) | received stacks; take only |
| Filter (right 3 x 3) | ghost slots |
| Whitelist or Blacklist | the filter rule |
| Exact or Item only | the filter match |
| Tubes, Stations, Capsules | the size of the network and the capsules in flight |
| State line | green Running, red Clogged or Network too large, yellow for the other states |

## Numbers

| Property | Value |
|---|---|
| Stack per capsule | 1 |
| Launch | 40 FE, once per 10 ticks per station |
| Energy buffer | 800 FE |
| Max input | 40 FE/t |
| Wait at a full target | 30 s |
| Retry when clogged | every 5 s |
| Input and output | 9 slots each |
| Priority | 0 to 9 |

The input buffer takes no filled crates.

## Jade and states

Jade shows "Mode M, priority N", "Input N, output N" (item counts), "Next target: x y z" or "Next target: none", and "State: state".

- "No tube at the mouth": no tube touches the mouth. Place a tube there, or place the station again.
- "No power": the station has a stack to send but less than 40 FE. Connect it to the grid.
- "Chunks not loaded": a part of the network is in an unloaded chunk. All capsules wait.
- "Network too large": the network has more than 512 tubes or more than 32 stations. It stops.
- "Clogged": see above.
- "Next target: none": no receiving station takes the first input stack. Check the modes, filters and output space.

## Next

- A capsule trip of 100 blocks or more gives the advancement "Pneumatic Post".
- At Tier 4 a <ItemLink id="autarkia:depot" /> can push items into a capsule station next to it.
