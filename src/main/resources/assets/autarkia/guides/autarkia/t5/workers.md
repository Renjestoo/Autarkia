---
navigation:
  title: Replicators and Nanites
  parent: t5/index.md
  position: 4
  icon: autarkia:nanite_paste
---

# Replicators and Nanites

Tier 5 has two new workers. Neither is made in the worker assembler, and neither fits in a worker capsule.

## Replicator Walker

A walker at 2.5 times normal size: health 150, armor 14, battery 1,000,000 FE. It costs 3 of the worker cap.

The Replicator Foundry prints it from 2 walker frames, 1 AI core, 8 nanite paste, 16 lattice alloy ingots, 4
advanced circuits, 8 servos and 1 outpost seed. The print takes 2,400 ticks at 2,000 FE/t. The walker carries an
outpost kit inside. It walks up to 1,024 blocks to a deposit. There it checks the site again, digs in for 100 ticks
and becomes an outpost core. If the site changed, it gets a new one.

Repair it with lattice alloy ingots: 20 health each. If it dies, its kit is lost.

## Nanite Swarm

A cloud of particles with 40 integrity. It floats through blocks at 0.3 blocks per tick. It costs 2 of the worker cap.

The Nanite Hive makes it from 4 nanite paste in 200 ticks at 100 FE/t. The swarm has a 50,000 FE buffer. The hive
refills it through a tether at up to 4,000 FE/t within 64 blocks. Outside that range, or with an empty buffer, the
swarm pauses.

- It takes apart up to 8 blocks per second.
- It holds up to 512 items. The items reach the Matter Buffer as visible streams.
- Fall, suffocation and wall damage do not hurt it. Players and mobs can. At 0 integrity it dissolves and drops 2
  nanite paste.

## Caps at tier 5

The domain cap is 26. Sub-caps: Porter 6, Hauler Crab 8, Builder Drone 8, Loader 4, Lineman 4, Surveyor 2,
Cable-Layer 3, Sentinel 6, Replicator Walker 2, Nanite Swarm 2. Each outpost core has its own cap of 8.

At tier 5 the Cable-Layer Drone also places cryo nodes and strings superconductor spans.
