---
navigation:
  title: Lightning Mast
  parent: t2/lightning.md
  position: 20
  icon: autarkia:lightning_mast
item_ids:
  - autarkia:lightning_mast
---

# Lightning Mast

<ItemImage id="autarkia:lightning_mast" scale="2" />

The lightning mast catches lightning. It pulls every natural bolt within 48 blocks to itself. The mast takes the whole
strike, so the bolt does no damage and starts no fire. The energy of each strike goes into the
<ItemLink id="autarkia:capacitor_bank" /> blocks of its network.

## Get it

<RecipeFor id="autarkia:lightning_mast" />

## Use it

1. Place the mast on a high point. It is 4 blocks tall.
2. Keep the sky above its top open. A roof or a tree above it stops it.
3. Place one or more capacitor banks.
4. Connect the bushing at the bottom of the mast with a <ItemLink id="autarkia:copper_wire_spool" /> span to a bank or
   to a <ItemLink id="autarkia:wooden_pole" />. The mast must have its own span. A mast that only stands next to a pole
   finds no network.
5. Put the banks on the same network: with spans, or next to the bottom block of a pole of that network.
6. Wait for a thunderstorm. During thunder the mast also calls strikes by itself.

Each strike gives 120,000 FE. The mast splits it equally between the capacitor banks of its network. A bank takes only
what fits into it. The rest goes into the ground with sparks at the foot of the mast. One bank holds 50,000 FE, so
3 empty banks take a full strike.

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 4 x 1 blocks |
| Catch radius | 48 blocks (horizontal) |
| Energy per strike | 120,000 FE |
| Own strikes during thunder | a chance of 1 in 3,000 per tick |
| Needs | open sky above its top |
| Cable | copper or other metal cable at its bottom bushing |

Jade shows 2 lines:

- "Open sky: catches bolts": the mast works. "Covered: no open sky above": something is above the mast. Remove it.
- "Strikes: 3, last stored 120000 FE": the number of strikes and the energy that the last strike put into the banks.

## Problems

- "Covered: no open sky above": a block is above the top of the mast.
- Sparks at the foot of the mast after a strike: the banks were full, or the mast has no banks in its network. Add
  capacitor banks, and give the mast its own copper span.
- Bolts still hit your poles: the poles are more than 48 blocks from the mast. Build a second mast.

## What the colony does

Lightning can strike the domain 2 times in one week, or a lightning surge can destroy a machine. Then the colony
proposes a Lightning Mast project on a high point. It builds at most 2.

## Next

- Store the strikes in a <ItemLink id="autarkia:capacitor_bank" />. Only capacitor banks take the energy of a strike.
- Protect the rest of your lines with the <ItemLink id="autarkia:lightning_arrester" />.
