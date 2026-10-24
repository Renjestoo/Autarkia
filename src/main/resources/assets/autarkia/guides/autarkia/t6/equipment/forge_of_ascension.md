---
navigation:
  title: Forge of Ascension
  parent: t6/equipment.md
  position: 10
  icon: autarkia:forge_of_ascension
item_ids:
  - autarkia:forge_of_ascension
---

# Forge of Ascension

<ItemImage id="autarkia:forge_of_ascension" scale="2" />

The Forge of Ascension is an open chamber that fits the Aegis armor on your body. You wear the Lattice Carapace armor
and stand on the pad. Robot arms take off each carapace piece and put on the Aegis piece, from the boots up to the
helmet. You cannot craft Aegis armor in any other way.

## Get it

<RecipeFor id="autarkia:forge_of_ascension" />

## Use it

1. Find a free space of 3 x 4 x 3 blocks. Place the forge. Its center is the middle of the floor: the pad.
2. Connect the forge to your network. The network must store 50,000,000 FE before you start.
3. Open the forge. Put the inputs of the pieces into the 27 slots (see the table).
4. Wear the Lattice Carapace pieces that you want to change:
   <ItemLink id="autarkia:lattice_carapace_boots" />, <ItemLink id="autarkia:lattice_carapace_legs" />,
   <ItemLink id="autarkia:lattice_carapace_chest" /> and <ItemLink id="autarkia:lattice_carapace_helmet" />.
5. Walk into the chamber. Stand on the pad.
6. Empty both hands. Sneak and use the pad.
7. Do not move. The fitting takes 600 ticks (30 s).

## The fitting

| Piece | Inputs | Worn piece | Steps (ticks) | FE |
|---|---|---|---|---|
| <ItemLink id="autarkia:aegis_boots" /> | 4 aegis plates, 1 AI core, 4 nanite paste | Lattice Carapace Boots | 60 to 160 | 12,500,000 |
| <ItemLink id="autarkia:aegis_legs" /> | 7 aegis plates, 1 AI core, 6 nanite paste | Lattice Carapace Leggings | 160 to 280 | 12,500,000 |
| <ItemLink id="autarkia:aegis_chest" /> | 8 aegis plates, 1 AI core, 8 nanite paste | Lattice Carapace Chestplate | 280 to 420 | 12,500,000 |
| <ItemLink id="autarkia:aegis_helmet" /> | 5 aegis plates, 1 AI core, 4 nanite paste | Lattice Carapace Helmet | 420 to 520 | 12,500,000 |

- Tick 0: the forge locks you on the pad. Tick 20: it scans you.
- At the start of a step, the arms take off the carapace piece.
- During the step, the forge takes the FE of that piece from the network, evenly each tick.
- 10 ticks before the end of the step, the forge takes the inputs and puts on the Aegis piece. The camera shakes.
- The new piece keeps the FE of the old carapace piece.
- A step without its inputs or without its worn carapace piece is skipped.
- Tick 520: the forge seals the set with a burst of light. Tick 600: you are free.

While the fitting runs, you cannot move or jump and you take no damage.

A full set of 4 Aegis pieces gives the advancement "Ascended".

## The screen

- 27 input slots.
- The state: "Idle", "Fitting", "Fitting complete" or "Fitting aborted".
- During a fitting: the step and its progress, for example "Step 2: boots, 15%", and "FE per tick: N".
- Else: "Pieces fitted: N" and the hint "Sneak + use the pad with empty hands".
- "Grid: N kFE": the stored FE of the forge network, in thousands.

The steps are "Lock", "Scan", "Step 2: boots", "Step 3: legs", "Step 4: chest", "Step 5: helmet" and "Seal".

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 4 x 3, open chamber |
| Input slots | 27 |
| Start condition | 50,000,000 FE stored on the network |
| Fitting time | 600 ticks |
| Energy per piece | 12,500,000 FE |
| Energy for 4 pieces | 50,000,000 FE |
| Hardness, blast resistance | 8, 1,200 |
| Mining tool | Diamond pickaxe or better |

## Problems

- "Stand on the pad in the middle of the Forge": move to the middle of the floor.
- "Nothing to fit: wear lattice carapace pieces and load their inputs": wear at least one carapace piece and put its
  inputs into the slots.
- "The Forge needs 50000000 FE on its grid (N FE now)": charge the storage of the forge network.
- "A fitting is already running": wait for the end.
- "The Forge of Ascension is not complete": use the middle of the floor.
- "Fitting aborted: you left the pad", "logged out", "the grid cannot pay", "the inputs are missing", "the Forge was
  broken" or "the fitting recipe is gone": the arms retract. The pieces that the forge already fitted stay on you. A
  carapace piece that the arms took off comes back to its slot.

## Next

- Wear the full Aegis set. See the <ItemLink id="autarkia:aegis_chest" /> page for the Aegis rule and the Nanite
  Reform.
