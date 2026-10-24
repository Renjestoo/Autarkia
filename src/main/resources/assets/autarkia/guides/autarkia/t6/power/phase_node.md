---
navigation:
  title: Phase Node
  parent: t6/power.md
  position: 30
  icon: autarkia:phase_node
item_ids:
  - autarkia:phase_node
---

# Phase Node

<ItemImage id="autarkia:phase_node" scale="2" />

The phase node is the tier 6 connector. Two nodes link through the air with no cable item. A phase link carries
1,048,576 FE/t with no loss, no heat and no sag.

## Get it

<RecipeFor id="autarkia:phase_node" />

One craft gives 2 nodes.

## Use it

1. Place two phase nodes up to 64 blocks apart. Keep the line between their centers clear.
2. Hold the <ItemLink id="autarkia:wrench" />. Use it on the first node. The message is "First end set; click the second
   node".
3. Use the wrench on the second node within 600 ticks. The message is "Phase link: N blocks".
4. Place machines, generators or storage next to a node. A node feeds the blocks on its 6 sides.

To join a phase link to another cable tier, link the node to a <ItemLink id="autarkia:transformer" />.

## Numbers

| Property | Value |
|---|---|
| Rating | 1,048,576 FE/t |
| Link length | up to 64 blocks |
| Links per node | 4 |
| Loss | none |
| Line of sight check | every 40 ticks |
| Mining tool | Diamond pickaxe or better |

## Jade

- "Links: N": the phase links on this node.
- "Line of sight: ok" or "Line of sight: blocked".

## Problems

- "The span is blocked by a block": a block stands on the line between the nodes. Remove it, or move a node.
- "Too long: max 64 blocks for phase link": the nodes are too far apart.
- "Node is full": the node already has 4 links.
- "Pick a different node": you used the wrench twice on the same node.
- The beam goes dim: a block now stands on the line. The link stops. It comes back by itself when the line is clear.

## Next

- A phase node on a grid with an ONLINE <ItemLink id="autarkia:singularity_core" /> is a tether point for the Aegis
  gear. You stay tethered within 64 blocks of it.
