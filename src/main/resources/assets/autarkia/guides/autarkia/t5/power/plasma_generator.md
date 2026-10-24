---
navigation:
  title: Plasma Generator
  parent: t5/power.md
  position: 60
  icon: autarkia:plasma_generator
item_ids:
  - autarkia:plasma_generator
---

# Plasma Generator

<ItemImage id="autarkia:plasma_generator" scale="2" />

The plasma generator is the power plant of Tier 5. It burns lattice fuel cells and gives 32,768 FE/t. It needs a large
amount of energy to start: the ignition takes 1,000,000 FE from your network.

## Get it

<RecipeFor id="autarkia:plasma_generator" />

## Use it

1. Place the generator. It is 3 x 3 x 3 blocks; its center is the bottom middle block.
2. Connect a span from your network to its machine port.
3. Make sure the network can give 1,000,000 FE, for example from a charged
   <ItemLink id="autarkia:superconducting_coil" />.
4. Open the generator.
5. Put <ItemLink id="autarkia:lattice_fuel_cell" /> into the fuel slot.
6. Push "Ignite". The state changes to "Igniting".
7. Wait. The generator takes 1,000,000 FE from the network at up to 5,000 FE/t.
8. The state changes to "Ramping up". The output rises to 32,768 FE/t over 200 ticks.
9. The state changes to "Running". Keep fuel in the slot and keep the buffer from filling up.

## States

| State | Meaning |
|---|---|
| Cold | The plasma is off. "Ignite" works when a cell is in the slot. |
| Igniting | The generator takes the ignition energy from the network. It gives no energy now. |
| Ramping up | The output rises from 0 to 32,768 FE/t over 200 burning ticks. |
| Running | Full output, 32,768 FE/t. |
| Hot, idle | No fuel, or the buffer has no room for one tick of output. The plasma stays hot for 200 ticks. Then it is cold. |

The burn pauses while the buffer is full. A pause longer than 200 ticks makes the plasma cold, and you must ignite it
again with another 1,000,000 FE.

## Screen

| Part | What it shows |
|---|---|
| Fuel slot | Lattice fuel cells, up to 16 |
| State text | The state, in color |
| "Output: N FE/t" | Energy made in the last tick |
| "Ignite" button | Active while the generator is cold and has fuel |
| "Ignition: N%" | Ignition energy taken so far; the bar below shows it, hover for "N / 1,000,000 FE" |
| "Hot for N s" | Shown in the state "Hot, idle": seconds until the plasma is cold |

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 3 x 3 |
| Full output | 32,768 FE/t |
| Ignition | 1,000,000 FE, at most 5,000 FE/t from the network |
| Ramp | 200 burning ticks (10 s) |
| Fuel | 1 <ItemLink id="autarkia:lattice_fuel_cell" /> = 78,643,200 FE = 2,400 ticks at full output |
| Hot time without burning | 200 ticks (10 s) |
| Buffer | 1,000,000 FE |
| Output to the network | Up to 65,536 FE/t |
| Hardness, blast resistance | 6, 12 |

## Problems

- "Ignite" is grey: the generator is not cold, or the fuel slot is empty.
- The ignition stays at a low percent: the network gives too little energy. Charge storage first; the ignition takes up
  to 5,000 FE/t.
- "Hot, idle": the buffer is full or the fuel is gone. Use more energy on the network, or add fuel within 10 s.
- Breaking the generator while it is hot (ramping, running or hot, idle) makes an explosion of power 4. It hurts
  creatures but breaks no blocks. The fuel drops as an item.

Jade shows "Output: N FE/t", "Buffer: N / 1,000,000 FE", "State: X" and, while igniting, "Ignition: N%".

## Next

- Carry the energy with <ItemLink id="autarkia:superconductor_spool" /> spans.
- Power the <ItemLink id="autarkia:plasma_separator" />, the <ItemLink id="autarkia:nanite_hive" /> and the
  <ItemLink id="autarkia:replicator_foundry" />.
