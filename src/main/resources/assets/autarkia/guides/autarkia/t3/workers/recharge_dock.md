---
navigation:
  title: Recharge Dock
  parent: t3/workers.md
  position: 10
  icon: autarkia:recharge_dock
item_ids:
  - autarkia:recharge_dock
---

# Recharge Dock

<ItemImage id="autarkia:recharge_dock" scale="2" />

The recharge dock charges the battery of the electric workers of Tier 3 and up. It also repairs them slowly. Without docks, Builder Drones, Loaders and Linemen stop when their battery is empty.

## Get it

<RecipeFor id="autarkia:recharge_dock" />

## Use it

1. Place the dock near the place where the workers work.
2. Connect it to the grid: put a node next to it, or string a span to one of its bushings.
3. When a worker needs energy, it goes to a free dock by itself. A drone lands on top of the dock. A walking worker stands next to it.
4. The worker charges until its battery is full. Then it goes back to work.

One dock serves one worker at a time. Build more docks for more workers. Spring workers (Porter, Hauler Crab) use the <ItemLink id="autarkia:winding_post" /> instead.

## Numbers

| Property | Value |
|---|---|
| Buffer | 50,000 FE |
| Charge rate | up to 2,000 FE/t per worker (config dockChargeRate) |
| Repair | 1 health per 5 s, 500 FE each |
| Workers at a time | 1 |

| Worker | Battery |
|---|---|
| Builder Drone | 80,000 FE |
| Loader | 120,000 FE |
| Lineman | 60,000 FE |

## Jade

- "Buffer: N / 50,000 FE": the stored energy.
- "Free": no worker uses the dock.
- "Charging": the dock charges or repairs a worker.
- "Worker docked": a worker is at the dock but does not take energy now.

## Problems

- "Buffer: 0 / 50,000 FE": the dock has no power. Check the connection.
- A worker waits far from a dock: build a dock nearer. The colony also builds docks by itself when its workers work 32 blocks or more from a charger.

## Next

- <ItemLink id="autarkia:drone_frame" /> is the frame of the Builder Drone.
- <ItemLink id="autarkia:worker_upgrade_speed" /> and the other upgrades improve a worker.
