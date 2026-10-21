---
navigation:
  title: Claims and Replication
  parent: t5/index.md
  position: 5
  icon: autarkia:nanite_hive
item_ids:
  - autarkia:core_upgrade_t5
  - autarkia:nanite_hive
  - autarkia:claim_marker
  - autarkia:replicator_foundry
---

# Claims and Replication

<ItemLink id="autarkia:core_upgrade_t5" /> raises the Colony Core to tier 5. The fabricator makes it from 1 AI core,
8 lattice alloy ingots and 8 nanite paste in 200 ticks (32,000 FE).

| Tier 5 value | |
|---|---|
| Domain half-size | 64 blocks |
| Domain beacons | 4, half-size 24 |
| Worker cap | 26 |
| Active projects | 6 |
| Daily budget | 2,500 points |
| Pattern library slots | 48, capture up to 40 blocks |
| Outpost square half-size | 32 |

## Nanite claims

1. Place 2 <ItemLink id="autarkia:claim_marker" /> blocks at opposite corners of the area.
2. Open the <ItemLink id="autarkia:nanite_hive" />. It lists the marker pairs within 64 blocks. Select one and set the
   depth below the lower marker. The hive screen works only inside a colony domain.
3. The hive releases a Nanite Swarm into the claim box.

Rules:

- The box is at most 32 x 32 x 48 blocks and must be fully inside your own domain. It cannot overlap other claims,
  watched Patterns or open projects.
- The swarm takes the box apart from the top layer down, in a serpentine order. Below the lower marker, each layer is
  inset by 1 block per 2 layers on each side, down to 4 x 4. This gives a terraced pit.
- Each block costs 200 FE + 50 FE x its hardness, at most 2,000 FE.
- Ores and raw ores stay items. Other drops with a matter value become free matter. Both go into the linked Matter
  Buffer; when it is full, the claim waits. Drops without a matter value fall at the swarm.
- Ores fly to the buffer as coloured streams: iron tan, copper orange, gold yellow, tin pale grey, zinc grey-green,
  vantite violet, diamond cyan, emerald green, redstone red, lapis blue, coal black.
- Block entities stay. A buried chest stays in place in a floating bubble of glow particles.
- Bedrock stays. A cell at the box edge with fluid outside it stays as a wall. An inner fluid source costs 100 FE.
- The whole server edits at most 48 blocks per tick for all claims.
- Stop all claims at once with `/autarkia nanite stop`, or with the config switch `nanite.enabled`.

## Replicator Foundry

The <ItemLink id="autarkia:replicator_foundry" /> is 3 x 3 x 3. It prints Replicator Walkers when all of these are
true:

- The colony is tier 5 or higher and the foundry has power.
- Fewer than 2 walkers are active, and outposts + walkers are fewer than 6.
- A free deposit exists outside all domains, at most 512 blocks from the Core, with no other colony domain within 96
  blocks.
- The parts are in storage, and the Matter Buffers hold the matter units for the outpost kit.
- At least 24,000 ticks have passed since the last print. In Manual autonomy the print is a proposal.

The walker leaves with a full battery, unfolds in 60 ticks and walks to the deposit. It digs in, and an outpost core
takes its place. The kit builds the outpost from rising blocks, 4 per tick, and strings its spans. Then the colony
proposes an HV link with lamps. At dusk you see a lit cable line cross the land to a base you did not build.

## More tier 5 projects

- Matter Buffer: storage is 90% full for 2,400 ticks, or the buffers are 90% full.
- Span upgrade: HV pylons become cryo nodes, and quenched spans get repaired.

<RecipeFor id="autarkia:nanite_hive" />
