---
navigation:
  title: Crate Launcher
  parent: t4/logistics.md
  position: 30
  icon: autarkia:crate_launcher
item_ids:
  - autarkia:crate_launcher
---

# Crate Launcher

<ItemImage id="autarkia:crate_launcher" scale="2" />

The crate launcher is a 1 x 2 x 1 machine that throws a full crate on a high arc into a
<ItemLink id="autarkia:catch_net" />. It sends 27 stacks at once over hills and rivers, 8 to 96 blocks far.

## Get it

<RecipeFor id="autarkia:crate_launcher" />

## Use it

1. Place the launcher. Place a <ItemLink id="autarkia:catch_net" /> 8 to 96 blocks away (horizontal), at most 32
   blocks lower or 24 blocks higher.
2. Put a storage block directly below the center of the net. The net puts the caught items there.
3. Use a <ItemLink id="autarkia:wrench" /> on the launcher. The action bar shows "Launcher marked. Use the wrench on a
   catch net."
4. Use the wrench on the net. The chat shows "Target set: catch net at x y z, n blocks".
5. Put empty <ItemLink id="autarkia:crate" /> items into the crate slot, up to 16.
6. Give the launcher power.
7. Put items into the 27 input slots, by hand, with a hopper or from a depot.

The launcher fires when all 27 input slots hold items, or when the input is not empty and got no new item for 200
ticks. All 27 slots go into one crate. Each shot needs 1 empty crate and 1,000 FE + 40 FE for each block of distance.
After a shot it reloads for 60 ticks.

### The arc

The launcher tries the angles 45, 55, 65 and 75 degrees. It uses the lowest angle with a clear path. It aims with the
wind at the moment of the shot, so strong changing wind can make it miss. A crate that hits a living entity does 4
damage to it and loses half its speed.

### Hits and misses

- The net catches a crate that comes down within 1.5 blocks of its center. All items and the empty crate go into the
  storage below the net.
- When the storage below has no room for everything, the crate bounces off and lands beside the net.
- A miss lands as a crate block with all items. Hauler Crabs collect stray crates. A crate in lava burns and spills.
- After 3 misses within 12,000 ticks the launcher pauses. Press "Reset" in its menu. Inside a colony domain the Core
  resets it after 6,000 ticks.

### Menu

| Part | What it does |
|---|---|
| Input slots (3 rows of 9) | The items for the next crate. Filled crates are refused. |
| Crate slot | Empty crates only, up to 16 |
| Status text | See Problems |
| "Target x y z" or "No target" | The catch net |
| "Distance n, angle n°" | The distance to the net and the last angle |
| "Last shot: HIT", "Last shot: MISS by n blocks" or "Last shot: none" | The result of the last shot |
| "Reset" button | Shows only while the launcher is paused |
| Energy bar (bottom) | Stored FE. Point at it to see the amount. |

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 2 x 1 blocks |
| Range | 8 to 96 blocks horizontal, 32 down to 24 up |
| Load | 27 stacks in one crate |
| Energy for each shot | 1,000 FE + 40 FE for each block of distance |
| Reload | 60 ticks |
| Fires when idle for | 200 ticks |
| Angles | 45, 55, 65, 75 degrees |
| Catch radius | 1.5 blocks |
| Crates in the air | 8 at most in one dimension |
| Pause | 3 misses in 12,000 ticks |
| Energy buffer | 16,000 FE |
| Mine with | Iron pickaxe or better |

The server options `launcherMaxRange` (default 96), `launcherCatchRadius` (default 1.5) and `launcherMaxInFlight`
(default 8) change these values.

## Problems

Jade shows "Target n blocks, angle n°", "Energy stored / capacity FE", the last shot and the status.

- "No target (wrench the launcher, then a net)": set the target with the wrench.
- "Use the wrench on a crate launcher first": you used the wrench on the net first.
- "The net must be 8 to 96 blocks away": move the net.
- "The net must be at most 32 blocks lower or 24 blocks higher": move the net up or down.
- "Target out of range": the net is outside the range, for example after a change of `launcherMaxRange`.
- "No target (wrench the launcher, then a net)" after a working time: the net is gone or its chunk is not loaded.
- "Waiting for items": the input is empty, or it changed in the last 200 ticks and is not full.
- "Reloading": the 60 ticks after a shot.
- "No empty crate": put empty crates into the crate slot.
- "No power": the buffer holds less than the shot cost.
- "Too many crates in the air": 8 crates fly in this dimension. Wait.
- "No clear arc": blocks stand in the path at all four angles. Clear the space above the path. The launcher tries
  again after 20 ticks.
- "Paused: check arc and wind": 3 misses in a short time. Check the path and the storage below the net, then press
  "Reset".
- "Last shot: MISS by n blocks (net output full)": the storage below the net was full.

## Next

- Put a launcher next to an outpost <ItemLink id="autarkia:depot" />. Orders from that site then fly home.
