---
navigation:
  title: Singularity Core
  parent: t6/power.md
  position: 10
  icon: autarkia:singularity_core
item_ids:
  - autarkia:singularity_core
---

# Singularity Core

<ItemImage id="autarkia:singularity_core" scale="2" />

The Singularity Core is the tier 6 generator. It needs one large ignition of 100,000,000 FE. After that it makes
131,072 FE/t for as long as its chunk is loaded. In a tier 6 domain it also changes every explosion into FE, and no
block of the domain breaks.

## Get it

<RecipeFor id="autarkia:singularity_core" />

It needs 4 <ItemLink id="autarkia:aegis_plate" />, 2 <ItemLink id="autarkia:ai_core" />, 2
<ItemLink id="autarkia:superconducting_coil" /> and 1 <ItemLink id="minecraft:nether_star" />.

## Use it

1. Find a free space of 5 x 5 x 5 blocks. Place the core. Its center is the bottom middle block.
2. Connect the core to a network. Use its machine port, or put a connector next to it. The network must have a node
   inside your colony domain.
3. Make sure that the network can give 100,000,000 FE in total. Stored FE, for example in superconducting coils, makes
   the ignition fast.
4. Open the core. Push Ignite. The core takes the ignition FE from the network at up to 1,048,576 FE/t.
5. When the ignition bar is full, the state changes to ONLINE. The core now fills its own buffer and gives FE to the
   network.
6. To stop it, push Stop, or use the <ItemLink id="autarkia:wrench" /> on it while you sneak. The buffer stays. A
   partial ignition is lost.

## The screen

- State: Off, Igniting or Online.
- "Base output": the FE that the core made in the last tick.
- "Damage points paid": the damage that the domain grid paid for players in Aegis armor.
- "Explosions absorbed": the number of explosions that the core changed into FE.
- "FE paid (60 s)": the FE paid for damage in the last 1,200 ticks.
- The bar under the text is the ignition progress. Hold the mouse over it to see the percent.
- The energy bar is the 1,000,000,000 FE buffer.

## Explosions become energy

When the core is ONLINE and the colony is tier 6, each explosion in the domain gives 50,000 FE x radius x radius. For
example, an explosion with radius 3 gives 450,000 FE.

- No block of the domain breaks, drops or burns.
- Every <ItemLink id="autarkia:industrial_lamp" /> in the domain flashes for 10 ticks. The core plays a drinking sound.
- A Wither or an Ender Dragon cannot break blocks in the domain.
- Creatures in the blast still take damage. The Aegis armor protects you.

## Numbers

| Property | Value |
|---|---|
| Size | 5 x 5 x 5 |
| Ignition | 100,000,000 FE, at up to 1,048,576 FE/t |
| Output when ONLINE | 131,072 FE/t |
| Buffer | 1,000,000,000 FE |
| Output to the network | up to 1,048,576 FE/t |
| Explosion gain | 50,000 FE x radius x radius |
| Hardness, blast resistance | 8, 1,200 |
| Mining tool | Diamond pickaxe or better |

## Jade

Jade is the info box that shows when you look at a block.

- "Output: N FE/t": the FE made in the last tick.
- "Buffer: N / 1000000000 FE": the stored FE.
- "State: Off", "State: Igniting" or "State: Online".
- "Ignition: N%": the ignition progress, only while it ignites.

## Problems

- The ignition does not move: the network has no spare FE. Add generators or charged storage to the network.
- Explosions still break blocks: the colony is not tier 6, the core is not ONLINE, or the network of the core has no
  node inside the domain.
- "Singularity Core stopped": you used the wrench while you sneaked. Push Ignite to start a new ignition.
- If you break the core, it drops as an item. The FE in its buffer is lost.

## Next

- Store the output in a <ItemLink id="autarkia:gyro_heart" />.
- Carry the power with <ItemLink id="autarkia:phase_node" /> links.
- An ONLINE core powers <ItemLink id="autarkia:bastion_block" /> walls and pays for the
  <ItemLink id="autarkia:aegis_chest" /> armor set.
