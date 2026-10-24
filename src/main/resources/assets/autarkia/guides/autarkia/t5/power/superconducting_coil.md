---
navigation:
  title: Superconducting Coil
  parent: t5/power.md
  position: 40
  icon: autarkia:superconducting_coil
item_ids:
  - autarkia:superconducting_coil
---

# Superconducting Coil

<ItemImage id="autarkia:superconducting_coil" scale="2" />

The superconducting coil is the large energy store of Tier 5. It holds 400,000,000 FE. It must stay cold: a dry coil
warms up, and at full temperature it quenches with a blast and loses all its energy.

## Get it

<RecipeFor id="autarkia:superconducting_coil" />

## Use it

1. Place the coil. It is 2 x 2 x 2 blocks.
2. Open the coil.
3. Put a <ItemLink id="autarkia:coolant_cell" /> into each of the 4 coolant slots.
4. Connect the coil to your network with a span to its machine port.
5. Optional: click a face with the <ItemLink id="autarkia:wrench" /> to set what that face does.

Each wrench click on a face moves its mode to the next value: none, in, out, in and out. A message shows the new
mode, for example "North face: in". By default every face is "in and out", and the front is "none". The face modes apply to FE blocks
that touch the coil.

## Screen

| Part | What it shows |
|---|---|
| 4 coolant slots | One coolant cell each |
| Temperature bar | Blue when cold, red when hot. The white line marks 50%. Hover: "Temperature T%, I/O limit N FE/t" |
| "N% charged" | Stored energy; "QUENCHED" in red after a quench |
| "In N, out M FE/t" | Energy in and out in the last tick |
| "Coolant: N s" | Seconds of coolant left; red at 0 |

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2 |
| Capacity | 400,000,000 FE |
| Input and output | 65,536 FE/t; 32,768 FE/t from 50% temperature |
| Coolant slots | 4 |
| Coolant use | 2 units per tick (1 cell lasts 12,000 ticks) |
| Temperature with coolant | -0.5% per tick |
| Temperature without coolant | +0.1% per tick |
| Dry time to half I/O | 500 ticks (25 s) |
| Dry time to quench | 1,000 ticks (50 s) |
| Quench blast power | 2 + 4 x (stored FE / 400,000,000), from 2 to 6 |
| Storage priority group | 2 |
| Hardness, blast resistance | 6, 12 |

## Quench

At 100% temperature the coil quenches. A blast goes off at its center. The blast is stronger when the coil holds more
energy, and it breaks no blocks unless the config `quenchBreaksBlocks` is on. All stored energy is lost. The coil stays
in place, but it is quenched: it takes and gives no energy until its temperature is 0% again. With coolant this takes
200 ticks (10 s) from 100%.

A quenched coil is not cold, so the superconductor spans at the coil warm up too.

## Problems

- "QUENCHED" on the screen and "Quenched: no I/O until cold again" in Jade: put in coolant cells and wait until the
  temperature is 0%.
- The I/O limit is 32,768 FE/t: the temperature is 50% or more. Add coolant.
- "Coolant: 0 s" in red: the coil is dry. Add coolant cells at once.

Jade also shows "Stored: N / 400,000,000 FE (P%)", "I/O: in N / out M FE/t", the coolant seconds and the temperature.

## Next

- Feed the coil from the <ItemLink id="autarkia:plasma_generator" />.
- A coil supplies the 1,000,000 FE that the plasma generator needs for its ignition.
- In Tier 6 the <ItemLink id="autarkia:singularity_core" /> uses 2 coils in its recipe.
