---
navigation:
  title: Replicators and Nanites
  parent: t5/index.md
  position: 6
  icon: autarkia:nanite_paste
---

# Replicators and Nanites

Tier 5 has two new workers. The worker assembler does not make them, and a worker capsule cannot catch them. They count
against the worker cap of your colony like other workers.

## Replicator Walker

The Replicator Walker is a walker at 2.5 times normal size. The <ItemLink id="autarkia:replicator_foundry" /> prints
it on a colony order. It carries an outpost kit to a free ore deposit, checks the site again, digs in for 100 ticks and
plants an outpost core. The kit then builds the outpost, and the walker is gone. If the site changed, the walker gets a
new site.

| Property | Value |
|---|---|
| Health | 150 |
| Armor | 14 |
| Step height | 1.5 blocks |
| Battery | 1,000,000 FE (full when printed) |
| Energy use | 100 FE per second idle, 1,000 FE per second while it moves |
| Energy needed at the site | 200,000 FE |
| Cap cost | 3 |
| Repair | Use a <ItemLink id="autarkia:lattice_alloy_ingot" /> on it: +20 health |

If a walker dies, its kit is lost, its deposit is free again, and the colony remembers the loss.

## Nanite Swarm

The Nanite Swarm is a cloud of particles with no body. The <ItemLink id="autarkia:nanite_hive" /> builds it from 4
<ItemLink id="autarkia:nanite_paste" /> for a claim. It floats through blocks to the claim, takes the box apart and
comes back when the claim is done. At the hive it dissolves into 2 paste.

| Property | Value |
|---|---|
| Integrity | 40 |
| Speed | 0.3 blocks per tick |
| Energy buffer | 50,000 FE, refilled by the hive tether at up to 4,000 FE/t within 64 blocks |
| Energy drain | 50 FE per second |
| Item buffer | 512 items |
| Work | 8 blocks per second |
| Cap cost | 2 |

Outside the tether range, or with an empty buffer, the swarm does no work. Only players and mobs can hurt it. At 0
integrity it dissolves where it is and drops 2 nanite paste and its items. Jade shows "Integrity N/40", "Buffer N/50,000
FE, N/512 items" and its state: Release, Travel, Disassemble, Return or Dissolve.

## Worker caps at tier 5

The colony worker cap is 26. Each outpost core has its own cap of 8.

| Worker | Tier 5 limit |
|---|---|
| Porter | 6 |
| Hauler Crab | 8 |
| Builder Drone | 8 |
| Loader | 4 |
| Lineman | 4 |
| Surveyor | 2 |
| Cable-Layer Drone | 3 |
| Sentinel | 6 |
| Replicator Walker | 2 |
| Nanite Swarm | 2 |

The Cable-Layer Drone strings superconductor spans too, so the colony can build and repair superconductor lines.

## Next

- Print walkers in the <ItemLink id="autarkia:replicator_foundry" />.
- Start claims in the <ItemLink id="autarkia:nanite_hive" />.
