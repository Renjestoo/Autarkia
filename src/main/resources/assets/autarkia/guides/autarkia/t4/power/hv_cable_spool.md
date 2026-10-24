---
navigation:
  title: HV Cable Spool
  parent: t4/power.md
  position: 20
  icon: autarkia:hv_cable_spool
item_ids:
  - autarkia:hv_cable_spool
---

# HV Cable Spool

<ItemImage id="autarkia:hv_cable_spool" scale="2" />

The HV cable spool strings HV spans: the high voltage line of Tier 4. One span carries 8,192 FE/t over up to 48
blocks, with very little loss.

## Get it

<RecipeFor id="autarkia:hv_cable_spool" />

One craft gives 2 spools.

## Use it

1. Hold the spool. Use it on the first node: an <ItemLink id="autarkia:hv_pylon" /> or a machine port. The action
   bar shows "First end set; click the second node".
2. Use it on the second node within 600 ticks. The action bar shows "Span placed: length blocks, n items".
3. The span costs 1 spool for each 16 blocks of length, rounded up. In creative mode it costs nothing.
4. To cancel the first end, sneak and use the spool. The action bar shows "Span start cleared".

A machine port takes at most 2 spans, all of one tier. Two nodes can have at most 4 parallel spans between them.

### Heat

A span heats up when it carries more than its rated 8,192 FE/t. The heat goes down by itself, faster in rain and in
wind. The cooling is 1, plus 0.25 in rain, plus 0.5 times the wind strength. A hot span sizzles and throws sparks.
When the heat reaches its limit, the span snaps.

### Sway

Long HV spans sway in the wind. The sway is 0.04 x span length x wind strength blocks, at most 2 blocks. One swing
takes 40 + 2 x span length ticks.

### Snapped cable

A snapped HV span leaves a whipping cable end at each node for 200 ticks. The end throws arcs and can light flammable
blocks near its tip. A hit does 10 damage and sets the target on fire for 40 ticks. At the end, the cable drops 1
spool for each 16 blocks of span length where its tip rests. A Hauler Crab can collect it.

## Numbers

| Property | Value |
|---|---|
| Rated flow | 8,192 FE/t |
| Longest span | 48 blocks |
| Loss | 0.02% of the flow for each block |
| Cost | 1 spool for each 16 blocks |
| Snapped cable | 200 ticks, 10 damage, fire for 40 ticks |
| Lightning | An HV span carries lightning |

## Problems

- "Not a connector or machine port": the block takes no spans. Use the spool on a pylon or a machine.
- "This node does not take HV line": the node is of another tier, for example a steel pylon. Use an HV pylon.
- "Port is wired for another tier. Use a transformer.": the machine already has a span of another tier.
- "Node is full": the node has no free span place.
- "Too long: max 48 blocks for HV line": the nodes are too far apart.
- "The span is blocked by a block": a block stands in the straight line between the two ends.
- "Max 4 parallel spans between two nodes": the two nodes already have 4 spans between them.
- "The other end is not loaded": go nearer to both nodes.
- "Pick a different node": you used the spool twice on the same node.
- "Needs n x HV Cable Spool": you carry too few spools for this span.

## Next

- Build long lines with <ItemLink id="autarkia:hv_pylon" /> blocks.
- The Cable-Layer Drone strings HV spans for the colony. It needs 1 spool in its recipe.
- The <ItemLink id="autarkia:grapple_launcher" /> needs 1 spool.
