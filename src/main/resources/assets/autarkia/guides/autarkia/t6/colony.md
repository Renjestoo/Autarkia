---
navigation:
  title: Seeds, Restoration and Bastion
  parent: t6/index.md
  position: 4
  icon: autarkia:autarky_seed
item_ids:
  - autarkia:core_upgrade_t6
  - autarkia:seed_matrix
  - autarkia:autarky_seed
  - autarkia:restoration_projector
  - autarkia:bastion_block
  - autarkia:bastion_glass
---

# Seeds, Restoration and Bastion

<ItemLink id="autarkia:core_upgrade_t6" /> raises the Colony Core to tier 6. The fabricator makes it from 2 aegis
plates, 2 AI cores and 8 lattice crystals in 200 ticks (32,000 FE).

| Tier 6 value | |
|---|---|
| Domain half-size | 96 blocks |
| Domain beacons | 6, half-size 24 |
| Worker cap | 32 |
| Active projects | 8 |
| Daily budget | 6,000 points |
| Pattern library slots | 128, capture up to 48 blocks |
| Outpost square half-size | 32 (65 x 65 blocks) |

## Seed Matrix

The <ItemLink id="autarkia:seed_matrix" /> is 3 x 3 x 3. It packs recorded Patterns into an
<ItemLink id="autarkia:autarky_seed" />.

- Put in 1 to 8 recorded pattern cards. The first Pattern must have the role OUTPOST. The cards stay in the matrix.
- In the layout editor, set the offset and the rotation of each Pattern. The footprints must not overlap and must fit
  the outpost square.
- Cost: the materials of all Patterns as matter units from the Matter Buffers, plus 1 AI core, 16 nanite paste, the
  parts of 2 Porters and 2 Builder Drones, and 1,000,000 FE.
- Packing takes 1,200 ticks at 834 FE/t. The matter units are taken in 20 parts.

### Throw the seed

Use the seed to throw it. Where it lands, the server checks: the colony has a free outpost slot, no domain overlaps,
all chunks of the footprint are loaded, the footprint has no block entities and less than half of it is fluid. The
outpost distance rules also apply. If a check fails, the seed drops as an item and you get a message.

1. A ring of light grows to the edge of the footprint (40 ticks).
2. The ground is levelled (100 ticks).
3. Blocks rise 3 blocks from the soil into place, bottom layer first and from the center outward.
4. The spans connect with arcs. The outpost registers, the machines get their charge, and the workers unfold when the
   worker cap has room.

The whole base stands after about 1,100 to 1,200 ticks. The unpack pauses while a chunk of the footprint is unloaded.

## Restoration Field

A <ItemLink id="autarkia:restoration_projector" /> is 1 x 2 x 1. It takes a snapshot of a box and repairs it.

- Box: at most 64 x 32 x 64, inside your domain. By default it is centered on the projector, from 8 blocks below to 23
  blocks above it. Take the snapshot with the button; it also takes one when it first gets power.
- When you or a trusted player break or place a block in the box, the snapshot follows.
- Every 100 ticks it looks for missing cells: air, fluid, fire or a replaceable block where the snapshot has a block.
- It repairs from the edge of the damage toward its center, bottom first: 8 blocks per tick per projector, 32 per tick
  on the whole server. Each block costs 512 FE.
- Material: first a matching dropped item within 16 blocks, which flies back to its place on a reverse arc. Else
  matter units from the Matter Buffers. Else the cell stays missing.
- Containers come back empty. Items are never copied.
- Buffer 819,200 FE, input up to 16,384 FE/t.

When a machine is destroyed in an area no field covers, the colony proposes a restoration projector there.

## Bastion blocks

<ItemLink id="autarkia:bastion_block" /> and <ItemLink id="autarkia:bastion_glass" /> are powered when they are in
their own colony's domain, the domain grid has an ONLINE Singularity Core, and the grid stores at least 1,000,000 FE.

- Powered: players cannot break them (creative mode can). They resist explosions, a Wither or an Ender Dragon cannot
  break them, and pistons cannot push them. You see crack stages, but the block never breaks.
- Not powered: they act like obsidian (hardness 50, blast resistance 1,200, diamond pickaxe).

The recipe: 8 lattice blocks (or 8 glass for Bastion glass) around 1 aegis plate give 8.

## Workers at tier 6

The domain cap is 32. Sub-caps: Porter 6, Hauler Crab 8, Builder Drone 10, Loader 4, Lineman 4, Surveyor 3,
Cable-Layer 4, Sentinel 8, Replicator Walker 2, Nanite Swarm 3. Each outpost core has its own cap of 10.

<RecipeFor id="autarkia:seed_matrix" />
