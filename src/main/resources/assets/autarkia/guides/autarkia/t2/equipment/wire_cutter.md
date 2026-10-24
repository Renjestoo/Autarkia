---
navigation:
  title: Wire Cutter
  parent: t2/equipment.md
  position: 10
  icon: autarkia:wire_cutter
item_ids:
  - autarkia:wire_cutter
---

# Wire Cutter

<ItemImage id="autarkia:wire_cutter" scale="2" />

The wire cutter removes one span of rope, wire or cable between two nodes. A span is the line between two
connectors. You get all of its cable items back.

## Get it

<RecipeFor id="autarkia:wire_cutter" />

## Use it

1. Hold the wire cutter.
2. Use it on one node of the span: a connector or a machine port.
3. Within 30 seconds, use it on the other node of the same span.
4. The span goes away. The cable items go into your inventory. When your inventory is full, they drop at your feet.

To cancel after the first click, sneak and use the wire cutter.

## Numbers

| Property | Value |
|---|---|
| Durability | 250 cuts |
| Cost per cut | 1 durability |
| Time between the two clicks | 600 ticks (30 s) at most |
| Repair | Steel ingot in an anvil |

## Problems

- "Not a connector or machine port": you clicked a block that is not a node. Click the pole, the connector or the
  machine port.
- "First end set; click the other node of the span": the first click is saved. Click the other end now.
- "No span joins these two nodes": the two nodes have no span between them. Click the two ends of one span.
- "Span start cleared": you sneaked and used the cutter. Start again with the first node.
- "Span removed, n cable items returned": the cut is done.

When more than 30 seconds pass after the first click, the next click becomes a new first end.

## Next

- Read [Copper Wire](../power.md) to place spans again with the <ItemLink id="autarkia:copper_wire_spool" />.
