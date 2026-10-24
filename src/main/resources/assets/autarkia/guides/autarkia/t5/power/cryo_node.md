---
navigation:
  title: Cryo Node
  parent: t5/power.md
  position: 20
  icon: autarkia:cryo_node
item_ids:
  - autarkia:cryo_node
---

# Cryo Node

<ItemImage id="autarkia:cryo_node" scale="2" />

The cryo node is the pole of a superconductor line. It holds superconductor spans at its top and keeps them cold with
coolant cells. A span between two cold nodes carries 65,536 FE/t with no loss.

## Get it

<RecipeFor id="autarkia:cryo_node" />

## Use it

1. Place the node. It is 1 block wide and 3 blocks high, so keep 3 blocks of space free.
2. Open the node.
3. Put a <ItemLink id="autarkia:coolant_cell" /> into each of the 4 coolant slots.
4. Hold a <ItemLink id="autarkia:superconductor_spool" /> and click this node.
5. Click the next node or a machine port to place the span.
6. Keep coolant in every node of the line.

The player who last opened the node is its tender. When a span of the node quenches, the tender also gets the
"Quench" advancement.

## Screen

| Part | What it shows |
|---|---|
| 4 coolant slots | One coolant cell each |
| Blue bar (right) | Coolant units left; hover for "units / 96,000 coolant units" |
| "Span N: T%" | Temperature of each attached span: blue below 30%, yellow to 70%, red above |
| "No spans" | No span is attached yet |
| "Coolant: N s" | Seconds of coolant left at the current use; red at 0 |

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 3 x 1 |
| Spans | 4, at the top block |
| Cable tier | Superconductor only |
| Coolant slots | 4 (96,000 units with 4 fresh cells) |
| Coolant use | 1 unit per tick, plus 1 per attached span above 50% load |
| Cold | While the cells hold coolant |
| Hardness, blast resistance | 6, 12 |

## Span temperature

| Condition | Change per tick |
|---|---|
| Both ends cold | -0.01 (down to 0) |
| One end not cold | +0.002 |
| Overload (load u above 1) | +0.02 x (u - 1) more |

| Temperature | What you see and hear |
|---|---|
| Below 30% | Frost, frost sound |
| 30% to 70% | Steam |
| 70% and more | Hissing |
| 100% | Quench |

With one end warm, a cold span quenches after about 500 ticks (25 s). At a quench the span is removed. A blast of
power 2.5 goes off at its middle, with a steam cloud, a camera shake within 24 blocks and two loose cable ends. The
blast breaks no blocks unless the config `quenchBreaksBlocks` is on. The span drops nothing.

A machine port at the end of a span counts as cold. A superconducting coil counts as cold while it has coolant and is
not quenched.

## Problems

- Jade shows "Coolant: 0 s": the node is warm. Its spans warm up and quench. Put in coolant cells.
- "This node does not take ..." when you click it with another cable: a cryo node takes only superconductor spans.
- "Node is full": the node has 4 spans already.

Jade also shows "Tier: superconductor", "Spans: N / 4", "Network: N% load" and the highest span "Temperature: N%".

## Next

- String spans with the <ItemLink id="autarkia:superconductor_spool" />.
- Store energy in the <ItemLink id="autarkia:superconducting_coil" />.
- At Tier 5 the colony upgrades hot HV lines: an <ItemLink id="autarkia:hv_pylon" /> becomes a cryo node.
