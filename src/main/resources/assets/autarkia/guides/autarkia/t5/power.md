---
navigation:
  title: Plasma and Superconductors
  parent: t5/index.md
  position: 1
  icon: autarkia:plasma_generator
item_ids:
  - autarkia:plasma_generator
  - autarkia:lattice_fuel_cell
  - autarkia:superconductor_spool
  - autarkia:cryo_node
  - autarkia:coolant_cell
  - autarkia:superconducting_coil
---

# Plasma and Superconductors

## Plasma generator

The <ItemLink id="autarkia:plasma_generator" /> is 3 x 3 x 3. It burns <ItemLink id="autarkia:lattice_fuel_cell" />.

1. Push the ignition button. The generator takes 1,000,000 FE from its network, at most 5,000 FE/t.
2. The output ramps from 0 to 32,768 FE/t in 200 ticks.
3. One fuel cell lasts 2,400 ticks at 32,768 FE/t.

The buffer holds 1,000,000 FE and gives up to 65,536 FE/t. When the buffer is full, the burn pauses. The plasma stays
hot for 200 ticks; after that it is cold and needs a new ignition. The states are COLD, IGNITING, RAMPING, RUNNING and
HOT_IDLE. If you break it while it is hot, it explodes.

The fabricator makes a fuel cell from 2 lattice shards, 1 vantite steel ingot and 1 blaze powder in 200 ticks.

## Superconductor

<ItemLink id="autarkia:superconductor_spool" /> carries 65,536 FE/t over spans of up to 32 blocks with no loss. Each
span costs 1 spool per 8 blocks. Only the <ItemLink id="autarkia:cryo_node" /> takes it. The node is 1 x 3 x 1 and
holds 4 spans at its top.

A cryo node has 4 slots for <ItemLink id="autarkia:coolant_cell" />. Each cell gives 24,000 coolant units. The node
uses 1 unit per tick, plus 1 for each of its spans above half load. It is cold while it has coolant. If you take the
cells out, the cooling stops at once.

The fabricator makes 2 coolant cells from 1 vantite steel ingot, 2 blue ice and 1 lattice shard in 200 ticks.

### Quench

A span is safe while both of its nodes are cold. Its temperature then falls by 0.01 per tick. When a node is warm, the
temperature rises by 0.002 per tick: from cold to quench takes 500 ticks. An overload adds 0.02 x (load - 1) per tick.

| Temperature | What you see |
|---|---|
| Below 0.3 | Frost and snowflakes along the span |
| 0.3 to 0.7 | The frost is gone, steam drips |
| 0.7 to 1.0 | Cloud jets and hissing |
| 1.0 | Quench |

At a quench the span is removed with a blast of power 2.5, a cloud of steam and a camera shake within 24 blocks. Two
loose cable ends whip for 60 ticks. The colony remembers it: each quench raises the coolant cell stock target by 8,
the colony plans shorter superconductor spans while the memory is strong, and a span upgrade repairs the line with a
new cryo node under its middle.

## Superconducting coil

The <ItemLink id="autarkia:superconducting_coil" /> is 2 x 2 x 2 and stores 400,000,000 FE. Its input and output is
65,536 FE/t, half of that while its temperature is 0.5 or more. It has 4 coolant slots and uses 2 units per tick.
Without coolant its temperature rises by 0.001 per tick; with coolant it falls by 0.005 per tick.

At temperature 1.0 the coil quenches: a blast that grows with the stored energy (power 2 + 4 x fill), and the stored
energy is lost. The coil stays, but it is quenched (no input or output) until its temperature is 0 again.

<RecipeFor id="autarkia:cryo_node" />
