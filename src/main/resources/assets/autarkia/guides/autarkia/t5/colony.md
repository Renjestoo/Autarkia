---
navigation:
  title: Claims and Replication
  parent: t5/index.md
  position: 5
  icon: autarkia:nanite_hive
---

# Claims and Replication

The <ItemLink id="autarkia:core_upgrade_t5" /> raises the Colony Core to tier 5. The domain grows to a half-size of 64
blocks, the worker cap to 26, and the colony can run 6 projects at a time.

Tier 5 gives the colony two new powers:

- Nanite claims. Mark a box with two <ItemLink id="autarkia:claim_marker" /> blocks and start the claim in a
  <ItemLink id="autarkia:nanite_hive" />. A Nanite Swarm takes the box apart layer by layer. Ores fly to your matter
  buffer as items, the rest becomes free matter, and you get a clean, terraced pit.
- Replication. The <ItemLink id="autarkia:replicator_foundry" /> prints Replicator Walkers on a colony order. A walker
  carries an outpost kit to a free ore deposit, digs in and becomes a new outpost.

What the colony does by itself at tier 5:

- It proposes a Matter Buffer when storage is 90% full for 2,400 ticks, or when the matter buffers are 90% full.
- It proposes a walker print when all print rules are true.
- It upgrades hot HV lines to superconductor lines: HV pylons become cryo nodes.
- After a quench it repairs the line with a new cryo node under the middle of the span, plans shorter superconductor
  spans for a while (at most 24 blocks), and raises its stock target for coolant cells by 8.

<SubPages />
