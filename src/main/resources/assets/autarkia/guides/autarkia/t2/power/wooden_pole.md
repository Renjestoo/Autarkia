---
navigation:
  title: Wooden Pole
  parent: t2/power.md
  position: 10
  icon: autarkia:wooden_pole
item_ids:
  - autarkia:wooden_pole
---

# Wooden Pole

<ItemImage id="autarkia:wooden_pole" scale="2" />

The wooden pole is the connector node of copper wire. It is 3 blocks tall. Copper spans attach to the insulators at its
top. Machines and generators next to its bottom block join the network of the pole.

## Get it

<RecipeFor id="autarkia:wooden_pole" />

## Use it

1. Place the pole on the ground. It needs 3 free blocks of height.
2. Hold a <ItemLink id="autarkia:copper_wire_spool" />.
3. Use the spool on the pole, then on the next pole or on a machine with a bushing.
4. Put a machine, a generator or a <ItemLink id="autarkia:capacitor_bank" /> directly next to the bottom block of the
   pole. It joins the network of the pole without a span.
5. Put a pole every 16 blocks or less along a long line.

A pole takes copper wire only. It does not take rope. To feed a belt line, put a pole next to any belt of the line.

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 3 x 1 blocks |
| Cable type | Copper wire |
| Spans | 4 |
| Longest span | 16 blocks |
| Parallel spans between two nodes | 4 |
| Feeds | The 6 blocks next to its bottom block |

Look at a pole with Jade to see 3 lines:

- "Tier: copper wire": the cable type of the pole.
- "Spans: 2 / 4": the spans on the pole and the maximum.
- "Network: 35% load": the load of the most loaded span on the pole, in percent of its rating.

## Problems

- "This node does not take rope drive": you used a rope coil on the pole. Poles take copper wire only.
- "Node is full": the pole has 4 spans. Put a second pole next to it.
- "Too long: max 16 blocks for copper wire": the other node is too far. Put a pole between them.
- "The span is blocked by a block": a solid block is in the straight line between the two tops. Make the line free.
- A bolt hits your pole in a storm: poles are the highest metal points, so they attract lightning. Put a
  <ItemLink id="autarkia:lightning_arrester" /> on top of the pole.
- When you break a pole, all its spans go away and their wire drops at the pole.

## Next

- String the spans with the <ItemLink id="autarkia:copper_wire_spool" />.
- Light the line with <ItemLink id="autarkia:industrial_lamp" /> blocks.
- Protect the pole with a <ItemLink id="autarkia:lightning_arrester" />.
- In Tier 3, the <ItemLink id="autarkia:steel_pylon" /> carries insulated cable.
