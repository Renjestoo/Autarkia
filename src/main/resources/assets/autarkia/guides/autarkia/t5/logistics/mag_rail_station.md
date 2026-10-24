---
navigation:
  title: Mag Rail Station
  parent: t5/logistics.md
  position: 30
  icon: autarkia:mag_rail_station
item_ids:
  - autarkia:mag_rail_station
---

# Mag Rail Station

<ItemImage id="autarkia:mag_rail_station" scale="2" />

The mag rail station sends and receives sleds on a <ItemLink id="autarkia:mag_rail" /> line. It packs up to 27 stacks
into one empty crate and sends the crate as a floating sled to another station.

## Get it

<RecipeFor id="autarkia:mag_rail_station" />

## Use it

1. Place the station at the end of a line, or in the line. It is one straight piece of track along the way it faces.
2. Connect a span from your network to the station. A launch costs 2,000 FE.
3. Open the station.
4. Type a name into the name field (up to 16 characters).
5. Click "To: ..." to choose the destination: "Next" or the name of another station on the line.
6. Put empty <ItemLink id="autarkia:crate" /> blocks into the crate slot (up to 16).
7. Put items into the input. Hoppers and chutes can also fill the input and the crate slot.
8. The station sends when all 27 input slots have items, or when the input was not empty for 200 ticks (10 s).
9. At the destination, the items go into the output and the empty crate goes into the crate slot.

"Next" is the next station in the direction you faced when you placed this station. If there is no station that way,
it is the next station the other way. A named destination is the nearest station with that name.

## Screen

| Part | What it does |
|---|---|
| Name field | The station name, up to 16 characters |
| "To: ..." | Click to cycle the destination: "Next" and the named stations of the line |
| "Input" and "Output" tabs | Switch the 3 x 9 grid between the 27 input slots and the 27 output slots |
| Crate slot | Empty crates, up to 16 |
| Filter (3 x 3 ghost slots) | Which items the input takes; an empty filter takes everything |
| "W" or "B" | Whitelist or blacklist for the filter |
| "C" or "I" | Match components, or match the item only |
| Energy bar | Hover for "N / 16,000 FE" |
| Status line | The status, see below |
| "Line: N rails, M stations" | The size of the line |
| "Sleds in flight: N" | Sleds that move on the line now |

The input never takes a filled crate. Automation takes items only from the output.

## How a sled moves

| Property | Value |
|---|---|
| Height above the rail | 0.4 blocks |
| Top speed | 1 block per tick (20 blocks per second) |
| Braking | Over the last 20 blocks, down to 0.1 blocks per tick |
| Spacing | 8 blocks behind the next sled in the same direction |
| One track | A sled waits in the station while a sled runs the other way on the same section |
| Unloaded chunks | The sled waits until the chunk ahead is loaded |
| No room at the destination | The sled waits above the station |

## Numbers

| Property | Value |
|---|---|
| Input slots | 27 |
| Output slots | 27 |
| Crate slot | 16 empty crates |
| Energy buffer | 16,000 FE |
| Energy input | Up to 512 FE/t |
| Energy per launch | 2,000 FE |
| Idle send time | 200 ticks (10 s) |
| Line caps | 2,048 rails, 16 stations, 32 sleds |

## Problems

| Status | Meaning and what to do |
|---|---|
| "Ready" | The station can send. |
| "Waiting for items" | The input is empty, or not full and not yet 10 s old. |
| "No track" | No mag rail line is connected. Place rails at the station ends. |
| "Closed loop: no sends" | The line is a loop. Open it so that it has two ends. |
| "Line over the cap" | The line has more than 2,048 rails or more than 16 stations. Split it. |
| "No destination" | No station has the destination name, or no other station is on the line. |
| "No empty crate" | Put empty crates into the crate slot. |
| "No power" | The buffer has less than 2,000 FE. Give the station more energy. |
| "Too many sleds on the line" | 32 sleds are on the line. Wait. |
| "Waiting: lock or spacing" | A sled runs the other way, or a sled ahead is less than 8 blocks away. Wait. |

Jade shows "Station: name" (or "unnamed"), "Destination: ...", "Sleds in flight: N", "Direction lock: none", "sleds
coming in", "sleds going out" or "both ways", and the status when it is not "Ready" or "Waiting for items".

If you break the station, all its items drop.

## Next

- The advancement "Float On" comes when a sled delivers over 256 or more rails.
