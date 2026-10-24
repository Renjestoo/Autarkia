---
navigation:
  title: Rope Coil
  parent: t1/power.md
  position: 20
  icon: autarkia:rope_coil
item_ids:
  - autarkia:rope_coil
---

# Rope Coil

<ItemImage id="autarkia:rope_coil" scale="2" />

The rope coil is the cable of Tier 1. It makes a rope span: one length of rope between two nodes. The span carries
power from a generator to a machine.

## Get it

<RecipeFor id="autarkia:rope_coil" />

## Use it

1. Hold the rope coil.
2. Use it on the first node. The message "First end set; click the second node" shows.
3. Use it on the second node within 600 ticks (30 seconds). The span appears.
4. To cancel the first end, sneak and use the coil.

A node is a <ItemLink id="autarkia:rope_pulley" /> or a block with a rope sheave (a grooved wheel for rope). These
blocks have a rope sheave:

- <ItemLink id="autarkia:hand_crank" />, <ItemLink id="autarkia:treadwheel" />, <ItemLink id="autarkia:windmill" />
- <ItemLink id="autarkia:stone_flywheel" />, <ItemLink id="autarkia:stamp_mill" />
- <ItemLink id="autarkia:winding_post" />, <ItemLink id="autarkia:worker_assembler" />
- The waterwheel and the steam engine of Tier 2

A rope span costs 1 coil for each 8 blocks of length or part of it. A span of 12 blocks costs 2 coils.

## Numbers

| Property | Value |
|---|---|
| Maximum span length | 12 blocks |
| Rated flow | 40 FE/t |
| Loss | 0.5% of the flow for each block of length |
| Cost | 1 coil for each 8 blocks |
| Stack size | 64 |
| First end kept for | 600 ticks |
| Spans on a machine | 2 |
| Spans on a rope pulley | 4 |
| Parallel spans between two nodes | 4 |

## Problems

- The rope squeals: the span must carry more than 40 FE/t. It slips and wears. Add a parallel span, or move a
  generator nearer to the machine.
- The rope snaps: the wear became too large. The two loose ends whip around and can hurt you. Place a new span.
- "Too long: max 12 blocks for rope drive": put a rope pulley between the two nodes.
- "Node is full": the node has no free place for a span.
- "Not a connector or machine port": the block is not a node.
- "This machine has no terminal for rope drive. Rope needs a rope sheave; wire and cable need a bushing.": this
  machine takes no rope. Put a rope pulley next to it.
- "The span is blocked by a block": a solid block is in the line. Remove it or move a node.
- "Pick a different node": the two ends are on the same node.
- "Max 4 parallel spans between two nodes": the two nodes have 4 spans already.
- "The other end is not loaded": the first node is in a chunk that is not loaded.
- "Needs 2 x Rope Coil": you have not enough coils for this span. The number shows the cost of the span.
- "Port is wired for another tier. Use a transformer.": the machine has a span of another cable type already.

When the load drops below 40 FE/t, the wear goes down again slowly.

## Next

- Use the <ItemLink id="autarkia:multimeter" /> to see the flow and wear of each span.
- In Tier 2, copper wire carries more power.
