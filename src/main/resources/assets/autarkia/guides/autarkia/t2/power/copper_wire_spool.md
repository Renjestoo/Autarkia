---
navigation:
  title: Copper Wire Spool
  parent: t2/power.md
  position: 20
  icon: autarkia:copper_wire_spool
item_ids:
  - autarkia:copper_wire_spool
---

# Copper Wire Spool

<ItemImage id="autarkia:copper_wire_spool" scale="2" />

The copper wire spool is the cable of Tier 2. It makes a copper span between two nodes. A copper span carries 256 FE/t,
much more than a rope span (40 FE/t). Above its rating the wire gets hot, and at full heat it snaps.

## Get it

Make it in the <ItemLink id="autarkia:wire_drawer" />:

| Input | Machine | Time | Power | Output |
|---|---|---|---|---|
| 2 copper ingots | <ItemLink id="autarkia:wire_drawer" /> | 100 ticks | 40 FE/t | 1 copper wire spool |

## Use it

1. Hold the spool.
2. Use it on the first node. The message "First end set; click the second node" shows.
3. Use it on the second node within 600 ticks (30 seconds). The message "Span placed: 12.5 blocks, 1 items" shows the
   length and the cost.
4. To cancel the first end, sneak and use the spool.

A node for copper wire is:

- a <ItemLink id="autarkia:wooden_pole" /> (4 spans), or
- a machine with a ceramic bushing (2 spans). For example the <ItemLink id="autarkia:industrial_lamp" />, the
  <ItemLink id="autarkia:capacitor_bank" />, the <ItemLink id="autarkia:lightning_mast" />, the
  <ItemLink id="autarkia:waterwheel" />, the <ItemLink id="autarkia:steam_engine" />, the
  <ItemLink id="autarkia:blast_smelter" />, the <ItemLink id="autarkia:plate_press" />, the
  <ItemLink id="autarkia:wire_drawer" /> and the <ItemLink id="autarkia:hand_crank" />.

All spans on one machine must be of the same cable type. A rope span ends only on a rope sheave. A copper span ends
only on a bushing or a pole. In Tier 3 a <ItemLink id="autarkia:transformer" /> joins rope and metal cable.

## Heat

Above 100% load a copper span gets hot. The more it is overloaded, the faster it heats. A hot wire sags more.

1. From 40% heat the wire glows orange.
2. From 70% heat it glows red, sizzles and throws sparks.
3. At 100% heat it snaps. Both loose ends whip and arc for 7 seconds. A loose end that touches you does 4 damage.
   Near wood and other blocks that burn, the arcs can start a fire.
4. After the whip, each loose end drops its share of the wire on the ground.

A span cools when its load goes down again. Rain cools it faster. At two times its rating a dry span snaps after about
8 seconds.

## Numbers

| Property | Value |
|---|---|
| Rated flow | 256 FE/t |
| Longest span | 16 blocks |
| Cost | 1 spool for each 16 blocks or part of it |
| Loss | 0.2% of the flow for each block of length |
| First end kept for | 600 ticks |
| Spans on a wooden pole | 4 |
| Spans on a machine | 2 |
| Parallel spans between two nodes | 4 |
| Snapped end whips for | 140 ticks |
| Damage of a snapped end | 4 |
| Heat from one lightning pulse | 30% |

## Problems

- "This node does not take copper wire": you used the spool on a rope pulley or another node of a different cable type.
- "This machine has no terminal for copper wire. Rope needs a rope sheave; wire and cable need a bushing.": the machine
  has no bushing. Put a <ItemLink id="autarkia:wooden_pole" /> next to the machine.
- "Port is wired for another tier. Use a transformer.": the machine has a rope span already.
- "Too long: max 16 blocks for copper wire": put a pole between the two nodes.
- "Node is full": the node has no free place for a span.
- "Not a connector or machine port": the block is not a node. A belt is not a node either.
- "The span is blocked by a block": a solid block is in the line.
- "Max 4 parallel spans between two nodes": the two nodes have 4 spans already.
- "Needs 2 x Copper Wire Spool": you have not enough spools for this span.
- "The other end is not loaded": the first node is in a chunk that is not loaded.
- "Pick a different node": the two ends are on the same node.
- The wire glows red: the span carries too much. Add a parallel span, or move a generator nearer to the machines.

## Next

- Remove a span and get the wire back with the <ItemLink id="autarkia:wire_cutter" />.
- See the load and heat of each span with the <ItemLink id="autarkia:multimeter" />.
- In Tier 3, the <ItemLink id="autarkia:insulated_cable_spool" /> carries 1,024 FE/t. You craft it from a copper wire
  spool and 3 dried kelp.
