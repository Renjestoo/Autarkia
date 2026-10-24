---
navigation:
  title: Core Seed
  parent: t1/colony.md
  position: 20
  icon: autarkia:core_seed
item_ids:
  - autarkia:core_seed
---

# Core Seed

<ItemImage id="autarkia:core_seed" scale="2" />

The Core Seed grows into a Colony Core. The Core starts your colony. It claims a square of land, the domain. Inside
the domain the colony reads its machines, proposes projects and sends workers to build them.

## Get it

<RecipeFor id="autarkia:core_seed" />

You need 4 wheat seeds, 4 <ItemLink id="autarkia:bronze_ingot" /> and 1 <ItemLink id="autarkia:bronze_gear" />.

## Use it

1. Find a flat place for your base. The Core is the center of a 33 x 33 domain.
2. Make sure that the 3 x 3 ground has a solid top. The center block must be dirt or overworld stone.
3. Make sure that the 3 x 3 x 3 space above the ground is free.
4. Point at the top face of the center ground block.
5. Press use. The seed goes into the ground.
6. Wait 10 seconds. Roots grow, then a frame, then the Core blooms.
7. Take the two gifts of your first Core: a <ItemLink id="autarkia:worker_capsule" /> with a Porter in it, and a
   <ItemLink id="autarkia:field_manual" />. They go into your inventory when you are near. Else they drop on the Core.
8. Use the capsule on the ground to release the Porter. See [Porters](../workers.md).
9. Right-click the Core to open the colony screen.

## The colony screen

The Core screen has these tabs:

| Tab | What it shows |
|---|---|
| Overview | Name and tier, domain size, power made and power used in FE/t, storage fill, workers, autonomy, today's budget, a graph of power made and used, and the colony log |
| Projects | The projects of the colony. See <ItemLink id="autarkia:project_board" />. |
| Jobs | The open jobs: type, place, worker, tries and status. A Cancel button removes a job. |
| Workers | Each worker with its state, its job and its charge bar. A Recall button sends the worker back to the Core. |
| Settings | The autonomy switch, a switch for each project type, the signs switch, item stock targets and the trusted players |
| Patterns | The Pattern library. Tier 1 has no Pattern slots. |

### Power made and power used

The Colony Core uses no power. The two power numbers on the Overview tab and in Jade ("Domain grid: 80 FE/t made,
0 FE/t used") describe every power network that has a node inside the domain:

- **Power made** is the average output of all generators in those networks.
- **Power used** is the power that the connected machines need while they work. A machine that stands idle adds
  nothing.

Example: a hand crank that a Porter turns makes 10 FE/t. When no machine works, the colony shows "10 FE/t made, 0 FE/t
used". When power used is higher than power made, the colony sees a power shortage and proposes a generator project.

The owner and trusted players can use the buttons. Only the owner can rename the colony and add trusted players. Other
players can only look.

## Move the Core

1. Break the Core. Only the owner or a trusted player can do this.
2. The colony goes dormant. Its workers stop their jobs.
3. Take the Core Seed that drops. Its tooltip says "Restores a dormant colony".
4. Plant this seed in a new place. The colony comes back with its name, tier and projects.

## Numbers

| Property | Value |
|---|---|
| Stack size | 16 |
| Growth time | 200 ticks (10 seconds) |
| Core size | 3 x 3 x 3 |
| Domain at Tier 1 | 33 x 33 blocks, centered on the Core |
| Worker cap at Tier 1 | 4 |
| Projects that build at the same time, Tier 1 | 1 |
| Daily budget at Tier 1 | 64 points |
| Colonies per player | 1 (server option `colony.maxColoniesPerPlayer`) |
| Sensor reading | every 100 ticks (5 seconds) |
| Planner pass | every 500 ticks (25 seconds) |

## Problems

- "Use the seed on the top face of a block": you pointed at a side. Point at the top face.
- "The seed needs dirt or stone below": the center ground block is not dirt-like or overworld stone.
- "The 3 x 3 ground below must be solid": a ground block has no solid top. Fill the gap.
- "The 3 x 3 x 3 space above must be free": a block is in the way. Remove it.
- "This is inside another colony's domain": move away from the other Core.
- "Too close to another colony's domain": keep the new Core at least 17 blocks away from the edge of another domain.
- "You already lead the maximum number of colonies": you already have a colony. Use a linked seed to move it.
- "This seed's colony is still active": the colony of this linked seed has a Core. Break that Core first.
- "This seed's colony no longer exists": the colony of this linked seed is gone. Craft a new seed.
- "The Core is growing", with a percent: you used the Core before it bloomed. Wait until 100%.
- The broken Core dropped no seed: you broke it in creative mode. A Core broken in creative mode drops nothing.

## Next

- Release the Porter from the <ItemLink id="autarkia:worker_capsule" />.
- Place <ItemLink id="autarkia:crate" /> blocks in the domain. The colony stores its items there.
- Place a <ItemLink id="autarkia:project_board" /> or make a <ItemLink id="autarkia:planning_tablet" /> to see the
  projects.
- Later, use a <ItemLink id="autarkia:core_upgrade_t2" /> on the Core to start Tier 2.
