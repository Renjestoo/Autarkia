---
navigation:
  title: Multimeter
  parent: t1/power.md
  position: 70
  icon: autarkia:multimeter
item_ids:
  - autarkia:multimeter
---

# Multimeter

<ItemImage id="autarkia:multimeter" scale="2" />

The multimeter is a test tool for power networks. It shows how much power the network makes and uses, and the state of
every span on a node.

## Get it

<RecipeFor id="autarkia:multimeter" />

The recipe needs a <ItemLink id="autarkia:brass_plate" /> and a <ItemLink id="autarkia:copper_coil" />. You make
these parts in Tier 2.

## Use it

1. Hold the multimeter.
2. Use it on a node: a rope pulley, a pole or a machine port.
3. A screen shows the reading of the network.

## Numbers

The reading shows:

| Line | Meaning |
|---|---|
| Node | The position of the node |
| Network | The network number, the cable type, the number of nodes and spans |
| Service | The part of the demand that the network supplies, in %. The bar is green at 99% or more. |
| Gen | The energy that the generators make |
| Demand | The energy that the machines ask for |
| Loss | The energy that the spans lose |
| Stored | The energy in storage blocks, and the capacity |
| Span | Each span of the node: its number, length and flow. The title is red while a rope slips. |
| Parallel, idle | A parallel span that carries no flow now |
| Load | A bar: the flow of the span compared to its rating |
| Wear | A bar: the wear of a rope span. A full bar snaps the rope. |
| Heat | A bar: the heat of a metal span |

## Problems

- "Not a grid node": the block is not a node. Use the multimeter on a rope pulley or a machine port.
- "No network (node not connected)": the node is not part of a network yet. Connect it with a span.

## Next

- Find overloaded rope spans before they snap. See <ItemLink id="autarkia:rope_coil" />.
