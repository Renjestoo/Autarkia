---
navigation:
  title: Superconductor Spool
  parent: t5/power.md
  position: 30
  icon: autarkia:superconductor_spool
item_ids:
  - autarkia:superconductor_spool
---

# Superconductor Spool

<ItemImage id="autarkia:superconductor_spool" scale="2" />

The superconductor spool places superconductor spans. A span carries 65,536 FE/t with no loss, but both of its ends
must stay cold. A warm span quenches with a blast.

## Get it

<RecipeFor id="autarkia:superconductor_spool" />

## Use it

1. Place two <ItemLink id="autarkia:cryo_node" /> blocks at most 32 blocks apart, or one node and a machine port.
2. Put coolant cells into every cryo node.
3. Hold the spool and click the first node. The message "First end set; click the second node" shows.
4. Click the second node within 600 ticks (30 s).
5. The message "Span placed: N blocks, M items" shows. The span costs 1 spool per 8 blocks of length.

To cancel the first end, sneak and use the spool.

A machine port takes a superconductor span when the machine has a metal cable terminal. A port holds 2 spans, and both
must be of the same tier. Use a <ItemLink id="autarkia:transformer" /> to join a superconductor line to a line of
another tier.

## Numbers

| Property | Value |
|---|---|
| Rated flow | 65,536 FE/t |
| Longest span | 32 blocks |
| Cost | 1 spool per 8 blocks (rounded up) |
| Loss | None |
| Parallel spans between two nodes | 4 at most |
| Output of one recipe | 4 spools |

## Cold and quench

A span stays safe while both ends are cold. A cryo node is cold while it has coolant. A machine port always counts as
cold. A <ItemLink id="autarkia:superconducting_coil" /> is cold while it has coolant and is not quenched.

| Temperature | What you see and hear |
|---|---|
| Below 30% | Frost along the span |
| 30% to 70% | Steam |
| 70% and more | Hissing |
| 100% | Quench: the span is removed with a blast of power 2.5 |

With both ends cold the temperature falls by 0.01 per tick. With one end warm it rises by 0.002 per tick, so a cold
span quenches after about 500 ticks (25 s). An overloaded span (load above 100%) heats faster. A quenched span drops
nothing. The blast breaks no blocks unless the config `quenchBreaksBlocks` is on.

## Problems

- "Too long: max 32 blocks for superconductor": put a cryo node between the two ends.
- "Needs N x Superconductor Spool": you carry fewer spools than the span costs.
- "This node does not take superconductor": the connector is of another tier. Use a cryo node.
- "Port is wired for another tier. Use a transformer.": the machine port already has a span of another tier.
- "Node is full": the node has no free span place.
- "Max 4 parallel spans between two nodes": the two nodes already have 4 spans between them.
- "The span is blocked by a block": a block is in the straight line between the ends. Clear the line.
- "The other end is not loaded": go nearer to the other end.

## Next

- Store energy in the <ItemLink id="autarkia:superconducting_coil" />.
- Feed the line from the <ItemLink id="autarkia:plasma_generator" />.
