---
navigation:
  title: Nanite Hive
  parent: t5/colony.md
  position: 30
  icon: autarkia:nanite_hive
item_ids:
  - autarkia:nanite_hive
---

# Nanite Hive

<ItemImage id="autarkia:nanite_hive" scale="2" />

The Nanite Hive takes whole hills apart. It builds a Nanite Swarm from nanite paste and sends it into a claim box. The
swarm removes the box layer by layer. Ores fly to your matter buffer as items, and the other blocks become free
matter. You get a clean, terraced pit.

## Get it

<RecipeFor id="autarkia:nanite_hive" />

## Use it

1. Upgrade your colony to tier 5 with the <ItemLink id="autarkia:core_upgrade_t5" />.
2. Place the hive in your colony domain. It is 2 x 2 x 2 blocks.
3. Connect a span from your network to the hive.
4. Place a <ItemLink id="autarkia:matter_buffer" /> in your domain within 64 blocks of the hive.
5. Mark the claim with two <ItemLink id="autarkia:claim_marker" /> blocks at opposite corners.
6. Open the hive and put at least 4 <ItemLink id="autarkia:nanite_paste" /> into the paste slot.
7. Click a marker pair in the list.
8. Set the depth below the lower marker with "-8", "-1", "+1" and "+8".
9. Click "Terrace on" or "Terrace off" to choose the pit shape.
10. Click "Start".

The hive builds a swarm from 4 paste in 200 ticks at 100 FE/t. The swarm flies to the box and works. "Stop" pauses the
claim; "Start" continues it at the same place. When the claim is done, the swarm flies back and dissolves into 2 paste
for the hive.

## Screen

| Part | What it shows |
|---|---|
| Pair list | Up to 8 marker pairs: "W x D at X, Z (H high)"; ">" marks your choice |
| Depth | Blocks below the lower marker, with "-8", "-1", "+1" and "+8" |
| "Terrace on/off" | The pit shape |
| "Start", "Stop" | Start or continue the claim, or pause it |
| Swarm line | "Swarms out: N", "Assembling N%", "Needs 4 paste", "Worker cap full" or "No swarm recipe" |
| Claim line | "No claim", or the state with progress and layer, for example "Running: 40%, layer 3/20" |
| Info line | "Height H/48", "Pick a marker pair", "No matter buffer linked", "Outside a colony domain" or "Nanites stopped (server)" |
| Energy bar | Hover for "N / 200,000 FE" |
| Paste slot | Nanite paste for new swarms |

## How the swarm works

- It works from the top layer down, in a back-and-forth order.
- With terraces on, each layer below the lower marker is 1 block smaller on each side for each 2 layers. The pit
  stops getting smaller at 4 blocks wide.
- Each block costs the swarm 200 FE + 50 FE x the block hardness, at most 2,000 FE. An inner fluid source costs 100 FE.
- The swarm mines like a netherite tool. Ores and raw ores stay items. They fly to the matter buffer as colored
  streams. Other drops with a matter value become free matter in the buffer. Drops without a matter value fall at the
  swarm.
- Block entities stay. A chest or another block with items inside stays in place, and glow particles show it.
- Unbreakable blocks such as bedrock stay. A block next to a fluid outside the box stays as a wall.
- The nanites act as the colony owner. A protection mod can stop a block edit; that block stays.

| Ore | Stream color |
|---|---|
| Iron | Tan |
| Copper | Orange |
| Gold | Yellow |
| Tin | Pale grey |
| Zinc | Grey-green |
| Vantite | Violet |
| Diamond | Cyan |
| Emerald | Green |
| Redstone | Red |
| Lapis | Blue |
| Coal | Black |

## Claim rules

| Rule | Value |
|---|---|
| Colony tier | 5 or higher |
| Box width and length | 32 blocks at most (config `nanite.maxClaimXZ`) |
| Box height | 48 blocks at most (config `nanite.maxClaimY`) |
| Distance | Markers and box corners within 64 blocks of the hive |
| Domain | The whole box inside your own domain |
| Overlap | No other claim, watched Pattern or open project in the box |
| Claims per hive | 1 |
| Swarms per claim | 1 at tier 5 |

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2 |
| Energy buffer | 200,000 FE |
| Energy input | Up to 8,000 FE/t |
| Swarm assembly | 4 paste, 200 ticks, 100 FE/t |
| Tether to each swarm | Up to 4,000 FE/t within 64 blocks |
| Swarm speed | 8 blocks per second (config `naniteBlocksPerSecond`) |
| Server limit | 48 block edits per tick for all claims (config `nanite.globalEditsPerTick`) |
| Light level | 5 |

## Problems

| Message or state | Meaning and what to do |
|---|---|
| "Claim started" | The claim runs. |
| "This hive already runs a claim" | Wait until the claim is done, or use another hive. |
| "Nanite claims need a tier 5 colony" | Use the Core Upgrade T5 first. |
| "The claim box must lie fully inside your colony domain" | Move the markers into your domain. |
| "The claim box must lie within 64 blocks of the hive" | Move the markers or the hive. |
| "The claim box is wider than nanite.maxClaimXZ" | Make the box at most 32 x 32. |
| "The claim box is higher than nanite.maxClaimY" | Use less depth. |
| "The claim box overlaps another claim, a watched Pattern or an open project" | Move the markers. |
| "Both claim markers must stand where the pair was listed" | Place the markers again. |
| "No nanite hive of this colony here" | The hive is not in your colony domain. |
| "Waiting for FE" | The swarm has too little energy. Give the hive more power, or bring the swarm within 64 blocks. |
| "Waiting for the matter buffer" | The buffer is full, or the swarm holds 512 items. Empty the buffer or add one. |
| "Waiting for chunks" | The chunks of the layer are not loaded. Stay nearer. |
| "Paused" | The claim is stopped, or the server stopped all nanites. |
| "Done" | The claim is finished. The message "Claim ... to ... is done" shows. |
| "Worker cap full" | The colony has no free worker cap. A swarm costs 2, and tier 5 allows 2 swarms. |

To stop every claim on the server at once, use `/autarkia nanite stop`, and `/autarkia nanite resume` to continue.
The config switch `nanite.enabled` stops them for good. If you break the hive, its claim ends and its swarms fly home.

Jade shows "Claim P%, layer N/M" or "No claim", "State X" and the energy buffer.

## Next

- Feed the raw ore to the <ItemLink id="autarkia:plasma_separator" />.
- The advancements "Clean Pit" and "Clean Cut Pit" come when a claim is done.
