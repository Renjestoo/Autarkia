---
navigation:
  title: Rope Pulley
  parent: t1/power.md
  position: 30
  icon: autarkia:rope_pulley
item_ids:
  - autarkia:rope_pulley
---

# Rope Pulley

<ItemImage id="autarkia:rope_pulley" scale="2" />

The rope pulley is a connector node for the rope drive. It joins up to 4 rope spans. Use it to make long lines and to
split one line into more lines.

## Get it

<RecipeFor id="autarkia:rope_pulley" />

## Use it

1. Place the pulley. It can face all 6 directions.
2. Use a <ItemLink id="autarkia:rope_coil" /> on a generator or a machine, then on the pulley.
3. Use the rope coil on the pulley again, then on the next node. Each span can be 12 blocks long.
4. To turn the pulley, use a <ItemLink id="autarkia:wrench" /> on it. Each use turns it to the next of the 6 directions.

Machines and generators next to the pulley join its network. This is also how machines without a rope sheave get
power from the rope drive.

## Numbers

| Property | Value |
|---|---|
| Spans | 4 |
| Span length | 12 blocks |
| Cable | Rope only |
| Mining tool | Axe |

## Problems

- "This node does not take rope drive": you used a cable that is not rope. The pulley takes only rope.
- "Node is full": the pulley has 4 spans. Place one more pulley.

## Next

- Read the flow of the network with a <ItemLink id="autarkia:multimeter" />.
- Connect a <ItemLink id="autarkia:windmill" /> on a hill to your machines below.
