---
navigation:
  title: Muscle and Wind
  parent: t1/index.md
  position: 2
  icon: autarkia:hand_crank
---

# Muscle and Wind

Tier 1 power comes from muscles and wind. FE/t is energy per game tick (20 ticks are one second). A rope drive carries
the power from the generators to the machines.

## Generators

- <ItemLink id="autarkia:hand_crank" />: you hold use on it. 20 FE/t.
- <ItemLink id="autarkia:treadwheel" />: an animal, a villager or a Porter walks in it. 12 to 24 FE/t.
- <ItemLink id="autarkia:windmill" />: the wind turns it. 4 to 40 FE/t.

## Rope drive

- A <ItemLink id="autarkia:rope_coil" /> makes a rope span between two nodes. A span is at most 12 blocks long and
  carries 40 FE/t.
- A node is a rope pulley or a block with a rope sheave (a grooved wheel for rope). In Tier 1 these blocks have a
  rope sheave: hand crank, treadwheel, windmill, stone flywheel, stamp mill, winding post and worker assembler.
- A <ItemLink id="autarkia:rope_pulley" /> joins up to 4 spans. Machines and generators next to it join its network.
- All nodes that spans connect make one network. The network sends the power from the generators to the machines.

## Storage and test tool

- The <ItemLink id="autarkia:stone_flywheel" /> stores 40,000 FE. It takes the surplus and gives it back later.
- The <ItemLink id="autarkia:multimeter" /> shows the generation, demand, losses and spans of a network.

<SubPages />
