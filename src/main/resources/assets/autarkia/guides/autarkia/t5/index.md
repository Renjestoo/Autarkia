---
navigation:
  title: "Tier 5: Matter"
  parent: index.md
  position: 5
  icon: autarkia:lattice_crystal
---

# Tier 5: Matter

The goal of Tier 5 is to take matter apart and to let the colony grow by itself. You mine lattice crystals deep
underground, separate each raw ore into 6 ingots, run superconductor power, store items as matter units, let nanites
remove whole hills and let Replicator Walkers found outposts that you did not build.

## Progression

### Prepare the upgrade (colony still at tier 4)

1. Get a netherite-level pickaxe, for example the <ItemLink id="autarkia:vantite_excavator" />.
2. Find a lattice geode between Y -58 and Y -24: blackstone outside, calcite, then the
   <ItemLink id="autarkia:lattice_geode_shell" />.
3. Mine the <ItemLink id="autarkia:lattice_cluster" /> crystals for <ItemLink id="autarkia:lattice_shard" />. Leave the
   charged shells in place, so that new clusters grow.
4. Craft 4 shards into a <ItemLink id="autarkia:lattice_crystal" />.
5. In the <ItemLink id="autarkia:fabricator" />, make <ItemLink id="autarkia:nanite_paste" /> from shards, redstone and
   slime balls.
6. Fabricate an <ItemLink id="autarkia:ai_core" /> from 4 advanced circuits, 1 crystal, 2 paste and 1 diamond.
7. Build the <ItemLink id="autarkia:plasma_separator" /> on 4 foundation blocks.
8. Alloy 8 <ItemLink id="autarkia:lattice_alloy_ingot" /> in the separator: 1 crystal and 1 vantite steel ingot each.
9. Fabricate the <ItemLink id="autarkia:core_upgrade_t5" /> from 1 AI core, 8 lattice alloy ingots and 8 paste.
10. Use the upgrade on your active tier 4 Colony Core. After 100 ticks the colony is tier 5.

### Build Tier 5 power

11. Fabricate <ItemLink id="autarkia:coolant_cell" /> from vantite steel, blue ice and shards.
12. Place <ItemLink id="autarkia:cryo_node" /> poles and fill each with 4 coolant cells.
13. String spans with the <ItemLink id="autarkia:superconductor_spool" />: 65,536 FE/t, up to 32 blocks, no loss.
14. Build a <ItemLink id="autarkia:superconducting_coil" />, give it coolant and charge it from your Tier 4 power.
15. Fabricate <ItemLink id="autarkia:lattice_fuel_cell" /> and build the <ItemLink id="autarkia:plasma_generator" />.
16. Ignite the generator. The ignition takes 1,000,000 FE from the coil; then the generator gives 32,768 FE/t.

### Matter, claims and transport

17. Place a <ItemLink id="autarkia:matter_buffer" /> in your domain. It stores items as matter units and holds free
    matter.
18. Build a <ItemLink id="autarkia:nanite_hive" /> within 64 blocks of the buffer.
19. Mark a hill with two <ItemLink id="autarkia:claim_marker" /> blocks and start the claim in the hive.
20. Put the raw ore from the claim into the plasma separator: 6 ingots from each raw ore.
21. Connect your bases with <ItemLink id="autarkia:mag_rail" /> lines and
    <ItemLink id="autarkia:mag_rail_station" /> blocks. Store heavy crates on <ItemLink id="autarkia:lattice_rack" />
    blocks.

### Equipment

22. Upgrade your Tier 4 tools: <ItemLink id="autarkia:nanite_multitool" />, <ItemLink id="autarkia:lattice_blade" />
    and the <ItemLink id="autarkia:disassembler_beam" />.
23. Upgrade the Frontier Suit to the Lattice Carapace: <ItemLink id="autarkia:lattice_carapace_helmet" />,
    <ItemLink id="autarkia:lattice_carapace_chest" />, <ItemLink id="autarkia:lattice_carapace_legs" /> and
    <ItemLink id="autarkia:lattice_carapace_boots" />.
24. Sneak and use the multitool or the beam on the matter buffer to link it.

### Replication

25. Build the <ItemLink id="autarkia:replicator_foundry" /> in your domain and give it power.
26. Keep the walker parts in storage and free matter in the buffers. The colony orders the prints by itself.

### Go to Tier 6

27. Build a <ItemLink id="autarkia:matter_compiler" /> on foundation. Its recipe needs a nether star.
28. Compile 2 <ItemLink id="autarkia:aegis_plate" /> from free matter: 32,768 MU each.
29. Fabricate the <ItemLink id="autarkia:core_upgrade_t6" /> from 2 aegis plates, 2 AI cores and 8 lattice crystals
    (200 ticks at 160 FE/t).
30. Use it on your active tier 5 Colony Core. See [Tier 6](../t6/index.md).

## What the colony does by itself

- The domain has a half-size of 64 blocks, with up to 4 domain beacons of half-size 24. The worker cap is 26, and the
  colony runs up to 6 projects at a time with a daily budget of 2,500 points.
- It proposes a Matter Buffer when storage is 90% full for 2,400 ticks, or when the matter buffers are 90% full.
- It upgrades hot HV lines to superconductor lines: HV pylons become cryo nodes.
- After a quench it repairs the line with a new cryo node, plans shorter superconductor spans for a while and raises its
  stock target for coolant cells by 8.
- When the print rules are true, it orders a Replicator Walker. The walker walks to a free ore deposit 64 to 512 blocks
  from the Core, digs in and becomes an outpost. The kit builds the outpost from rising blocks. You can have 6
  outposts, each with a half-size of 32 and its own worker cap of 8.
- A Nanite Swarm works through its claim by itself. Ores fly to the matter buffer as colored streams, a buried chest
  stays in place in a glowing bubble, and you get a terraced pit.

See the [workers page](workers.md) for the Replicator Walker and the Nanite Swarm.

<SubPages />
