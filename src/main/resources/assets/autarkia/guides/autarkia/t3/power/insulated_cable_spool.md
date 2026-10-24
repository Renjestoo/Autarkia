---
navigation:
  title: Insulated Cable Spool
  parent: t3/power.md
  position: 10
  icon: autarkia:insulated_cable_spool
item_ids:
  - autarkia:insulated_cable_spool
---

# Insulated Cable Spool

<ItemImage id="autarkia:insulated_cable_spool" scale="2" />

The insulated cable is the power line of Tier 3. One span carries 1,024 FE/t, four times the copper wire. Spans are longer and lose less energy.

## Get it

<RecipeFor id="autarkia:insulated_cable_spool" />

## Use it

1. Hold the spool.
2. Use it on the first node.
3. Use it on the second node within 30 seconds.
4. The span appears. In survival, the span takes 1 spool for each 16 blocks of length (rounded up).
5. To cancel the first click, sneak and use the spool.

A node is a block that holds spans: a <ItemLink id="autarkia:steel_pylon" />, a <ItemLink id="autarkia:wall_insulator" />, a <ItemLink id="autarkia:transformer" /> or a <ItemLink id="autarkia:fuse_box" />. A machine with a ceramic bushing is also a node (a machine port). A machine port takes at most 2 spans of one tier.

## Numbers

| Property | Value |
|---|---|
| Rated flow | 1,024 FE/t |
| Longest span | 24 blocks |
| Longest span to or from a wall insulator | 12 blocks |
| Loss | 0.1% per block |
| Cost | 1 spool per 16 blocks |
| Parallel spans between two nodes | 4 at most |
| Snapped end | whips for 7 s, 6 damage |
| Lightning | the cable conducts lightning |

## Overload

A span above 100% load heats up. Rain cools a span faster. At high heat the span sparks and sizzles. When the heat is full, the span snaps. The loose end whips around and hurts all near it. After 7 seconds it drops the spools of the span where its tip rests. A snap can start a fire.

Put a <ItemLink id="autarkia:fuse_box" /> into a line to stop an overload before the cable snaps. The <ItemLink id="autarkia:multimeter" /> shows the load of a span.

## Problems

- "Not a connector or machine port": the block takes no spans. Click a node or a machine with a bushing.
- "This node does not take insulated cable": the node carries another tier. Use a <ItemLink id="autarkia:transformer" />.
- "Port is wired for another tier. Use a transformer.": the machine already has a span of another tier.
- "Too long: max 24 blocks for insulated cable": move the nodes nearer. The limit is 12 blocks at a wall insulator.
- "The span is blocked by a block": a solid block is on the straight line between the nodes. Remove it or move a node.
- "Node is full": the node has its maximum number of spans.
- "Max 4 parallel spans between two nodes": use another node.
- "Needs N x Insulated Cable Spool": you have too few spools for this span length.
- "The other end is not loaded": go nearer to the other node.

## Next

- <ItemLink id="autarkia:steel_pylon" /> and <ItemLink id="autarkia:wall_insulator" /> hold the spans.
- <ItemLink id="autarkia:hv_cable_spool" /> is the next cable tier (Tier 4). Its recipe uses this spool.
- A Lineman needs 1 spool to be built in the <ItemLink id="autarkia:worker_assembler" />.
