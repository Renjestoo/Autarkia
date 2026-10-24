---
navigation:
  title: Drone Frame
  parent: t3/workers.md
  position: 20
  icon: autarkia:drone_frame
item_ids:
  - autarkia:drone_frame
---

# Drone Frame

<ItemImage id="autarkia:drone_frame" scale="2" />

The drone frame is the body of every flying worker. Put it into the worker assembler with the parts to build a Builder Drone. At Tier 4 it is also the frame of the Surveyor, the Cable-Layer Drone and the Sentinel.

## Get it

<RecipeFor id="autarkia:drone_frame" />

## Use it

1. Put the frame and the parts into the <ItemLink id="autarkia:worker_assembler" />.
2. Give the assembler FE.
3. The assembler builds the worker. The colony must be Tier 3 or higher.

| Worker | Frame | Parts | Time | Energy | Tier |
|---|---|---|---|---|---|
| Builder Drone | 1 drone frame | 2 <ItemLink id="autarkia:basic_circuit" />, 4 <ItemLink id="autarkia:servo" />, 4 <ItemLink id="autarkia:copper_coil" />, 4 <ItemLink id="autarkia:steel_plate" /> | 1,200 ticks | 100 FE/t | 3 |
| Surveyor | 1 drone frame | 2 advanced circuits, 2 servos, 1 survey lens, 2 vantite ingots | 1,600 ticks | 150 FE/t | 4 |
| Sentinel | 1 drone frame | 3 advanced circuits, 4 servos, 6 copper coils, 4 vantite steel ingots, 1 capacitor bank | 2,000 ticks | 200 FE/t | 4 |
| Cable-Layer Drone | 2 drone frames | 2 advanced circuits, 6 servos, 4 vantite steel ingots, 1 HV cable spool | 2,000 ticks | 200 FE/t | 4 |

## The Builder Drone

The Builder Drone is the Tier 3 builder of the colony. It flies, so it can place blocks high up and over gaps.

- It places any block, cuts blocks and levels ground for projects.
- It repairs watched Patterns: it puts back the blocks that a creeper or a stranger broke.
- During a Pattern repair it also strings spans up to insulated cable.
- It charges at a <ItemLink id="autarkia:recharge_dock" /> and lands on top of it.

| Property | Value |
|---|---|
| Battery | 80,000 FE |
| Health | 16 |
| Armor | 2 |
| Idle drain | 100 FE per second |
| Flying drain | 400 FE per second |
| Per placed block | 250 FE |
| Per strung span | 1,000 FE |
| Carry slots | 2 |
| Reach | 4 blocks |
| Most drones in a Tier 3 colony | 4 |
| Repair item | <ItemLink id="autarkia:steel_plate" /> (6 health) |

## Next

- <ItemLink id="autarkia:recharge_dock" /> keeps the drones flying.
- <ItemLink id="autarkia:worker_upgrade_speed" /> makes a drone fly faster.
