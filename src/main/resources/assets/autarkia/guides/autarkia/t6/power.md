---
navigation:
  title: Singularity and Phase Links
  parent: t6/index.md
  position: 1
  icon: autarkia:singularity_core
item_ids:
  - autarkia:singularity_core
  - autarkia:gyro_heart
  - autarkia:phase_node
---

# Singularity and Phase Links

## Singularity Core

The <ItemLink id="autarkia:singularity_core" /> is 5 x 5 x 5. Its states are OFF, IGNITING and ONLINE.

1. Push the ignition button. The core takes 100,000,000 FE from its network, at most 1,048,576 FE/t.
2. Then it is ONLINE and makes 131,072 FE/t into its 1,000,000,000 FE buffer while it is loaded. It stays ONLINE
   across a reload.
3. The stop button, or a sneak use with the wrench, stops it. The buffer stays.

It gives up to 1,048,576 FE/t through phase links and through adjacent HV and superconductor ports. If you break it,
it drops itself and the buffer is lost.

### Explosions become energy

While the core is ONLINE in a tier 6 domain, each explosion in the domain adds 50,000 x radius x radius FE to its
buffer: a creeper gives 450,000 FE, TNT 800,000 FE, a spawning Wither 2,450,000 FE. No block of the domain breaks,
drops or burns. Every industrial lamp in the domain flashes for 10 ticks and the core makes a drinking sound. A Wither
or an Ender Dragon cannot break blocks in such a domain.

Entities in the blast still take damage. The Aegis rule protects you (see the equipment page).

The buffer also pays for the Aegis rule. It pays first, then the other storage of the domain grid.

## Gyro Heart

The <ItemLink id="autarkia:gyro_heart" /> is 5 x 3 x 5 and stores 2,000,000,000 FE. Its spin is the square root of its
fill. Input and output are 1,048,576 FE/t x the spin, at least 10% of that. It loses 1 FE + 1 FE per 10,000,000 stored
each tick.

While it holds 1% or more, explosions cannot break it, and a break in survival is cancelled with "Spin it down first."

## Phase links

A <ItemLink id="autarkia:phase_node" /> links without cable. Use the wrench on node A, then on node B.

- Node B must be within 64 blocks with a clear line between the centers.
- A link carries 1,048,576 FE/t with no loss, sag, heat, wind or lightning.
- The line of sight is checked every 40 ticks. A blocked link turns inactive and its beam goes dim. It comes back by
  itself when the line is clear.
- A phase node feeds the blocks on its 6 sides. A transformer can join a phase link to another cable tier.

<RecipeFor id="autarkia:phase_node" />
