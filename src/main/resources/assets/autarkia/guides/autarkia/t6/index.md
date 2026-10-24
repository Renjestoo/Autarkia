---
navigation:
  title: "Tier 6: Ascendant"
  parent: index.md
  position: 6
  icon: autarkia:aegis_plate
---

# Tier 6: Ascendant

Tier 6 is the last tier. Your colony writes matter into metal, drinks explosions as energy and repairs its own damage.
The end goal has three parts:

- The <ItemLink id="autarkia:seed_matrix" /> packs a whole base into one seed.
- The <ItemLink id="autarkia:autarky_seed" /> unpacks that base where you throw it.
- The <ItemLink id="autarkia:forge_of_ascension" /> fits the Aegis armor on your body.

Terms on this page:

- FE is energy. FE/t is energy per tick (20 ticks are 1 second).
- MU (matter units) is the free matter in the Matter Buffers of your domain.
- The domain is the area that your colony owns: the Core square, the domain beacon squares and the outpost squares.

## Progression

1. Make a <ItemLink id="autarkia:matter_compiler" /> while your colony is still at tier 5. Put it on
   <ItemLink id="autarkia:foundation" /> and give it 16,384 FE/t.
2. Compile 2 <ItemLink id="autarkia:aegis_plate" />. Each plate costs 32,768 MU. The compiler takes the MU from the
   <ItemLink id="autarkia:matter_buffer" /> blocks of your domain, or from the items that you feed it.
3. Make a <ItemLink id="autarkia:core_upgrade_t6" /> in the <ItemLink id="autarkia:fabricator" />: 2 aegis plates, 2
   <ItemLink id="autarkia:ai_core" /> and 8 <ItemLink id="autarkia:lattice_crystal" />.
4. Use the upgrade on your active tier 5 Colony Core. After 100 ticks the colony is tier 6.
5. Build a <ItemLink id="autarkia:singularity_core" /> on a network that has a node inside your domain. Push Ignite.
   The ignition takes 100,000,000 FE from the network. Stored FE, for example in
   <ItemLink id="autarkia:superconducting_coil" /> blocks, makes it fast.
6. Add a <ItemLink id="autarkia:gyro_heart" /> to store the output. Carry the power with
   <ItemLink id="autarkia:phase_node" /> links instead of cables.
7. Put walls of <ItemLink id="autarkia:bastion_block" /> and <ItemLink id="autarkia:bastion_glass" /> around your base.
   They cannot break while the grid holds 1,000,000 FE or more.
8. Move items at once with <ItemLink id="autarkia:matter_conduit" /> blocks. Put one
   <ItemLink id="autarkia:domain_archive" /> in the domain to see and request all stock.
9. Place a <ItemLink id="autarkia:restoration_projector" /> over each important area. It puts back blocks that a blast
   or a mob destroys.
10. Make the <ItemLink id="autarkia:aegis_multitool" />, the <ItemLink id="autarkia:aegis_blade" /> and the
    <ItemLink id="autarkia:singularity_lance" /> in the fabricator.
11. Wear the Lattice Carapace armor and build the Forge of Ascension. The forge changes each carapace piece into an
    Aegis piece: <ItemLink id="autarkia:aegis_boots" />, <ItemLink id="autarkia:aegis_legs" />,
    <ItemLink id="autarkia:aegis_chest" /> and <ItemLink id="autarkia:aegis_helmet" />.
12. Record an outpost base at a <ItemLink id="autarkia:pattern_loom" />. Pack it in the Seed Matrix and throw the
    Autarky Seed. The whole base rises from the ground in about 1,100 ticks.

## Tier 6 values

| Property | Value |
|---|---|
| Domain half-size | 96 blocks |
| Worker cap | 32 |
| Active projects | 8 |
| Daily budget | 6,000 points |
| Domain beacons | 6, half-size 24 |
| Outposts | 6, square 65 x 65 |
| Worker cap of an outpost core | 10 |
| Pattern library | 128 Patterns, capture up to 48 blocks per axis |

## What the colony does by itself

- It keeps all the projects of the earlier tiers. It still proposes generators, storage, outposts and repairs when its
  sensors see a need.
- When a mob, an explosion or a stranger destroys a machine where no Restoration Field covers it, the colony remembers
  it. Then it proposes a Restoration Projector near that place. The proposal reason is "Machines were destroyed here
  (memory weight N) and no Restoration Field covers it". It builds up to 8 of these projectors.
- A Restoration Projector that the colony builds takes its snapshot by itself.
- While a Singularity Core is ONLINE, every explosion in the domain becomes FE. No block of the domain breaks. A Wither
  or an Ender Dragon cannot break blocks in the domain.
- The workers of an Autarky Seed unfold at the new outpost when the worker cap has room.

<SubPages />
