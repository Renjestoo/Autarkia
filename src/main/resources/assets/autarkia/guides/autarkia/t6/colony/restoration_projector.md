---
navigation:
  title: Restoration Projector
  parent: t6/colony.md
  position: 20
  icon: autarkia:restoration_projector
item_ids:
  - autarkia:restoration_projector
---

# Restoration Projector

<ItemImage id="autarkia:restoration_projector" scale="2" />

The restoration projector makes a Restoration Field. It takes a snapshot of a box around it. When blocks in the box go
missing, for example after a creeper blast, the projector puts them back. Dropped items fly back to their places.

## Get it

<RecipeFor id="autarkia:restoration_projector" />

The colony can also build one by itself. When a machine in your domain is destroyed and no field covers it, the colony
proposes a projector near that place.

## Use it

1. Place the projector inside your colony domain. It is 1 block wide and 2 blocks tall.
2. Connect it to your network. When it first gets power, it takes the snapshot by itself.
3. Open it. Set the size and the offset of the box with the - and + buttons. Hold Shift for steps of 8.
4. Click Snapshot to take a new snapshot of the box.
5. Keep free MU in the <ItemLink id="autarkia:matter_buffer" /> blocks of the domain. The projector uses it when no
   dropped item is near.

The default box is 64 x 32 x 64. It is centered on the projector, from 8 blocks below it to 23 blocks above it. When the
box goes out of the domain, the snapshot cuts it to the domain square of the projector.

## How it works

- When you or a trusted player break or place a block in the box, the snapshot changes too.
- Every 100 ticks the projector looks for missing cells. A cell is missing when the snapshot has a block there and the
  world now has air, fluid, fire or a replaceable block.
- It repairs from the edge of the damage toward the center, bottom first.
- Each block costs 512 FE.
- Material: first a matching dropped item within 16 blocks. The item flies back on a glow trail. Else the matter price
  of the block in free MU. Else the cell stays missing.
- Containers come back empty. Items are never copied.

## The screen

- State line at the top. While it reads, it shows "Taking the snapshot: N%".
- "Box size (shift: step 8)": X, Y and Z with - and + buttons.
- "Box offset from the projector": X, Y and Z with - and + buttons.
- Snapshot button.
- "Missing N": the missing cells of the last scan.
- Energy bar.

Only the owner or a trusted player can use the buttons.

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 2 x 1 |
| Largest box | 64 x 32 x 64 |
| Offset | up to 64 blocks on each axis |
| Scan | every 100 ticks |
| Repair speed | 8 blocks per tick per projector, 32 per tick on the server |
| Energy | 512 FE per block |
| Item range | 16 blocks |
| FE buffer | 819,200 FE, input up to 16,384 FE/t |
| Mining tool | Diamond pickaxe or better |

## Jade

- "Snapshot NxNxN" or "No snapshot".
- "Missing N".
- The state: "Watching the field", "Restoring", "Taking the snapshot", "No snapshot", "No power", "No material for some
  cells", "Outside a colony domain" or "Restoration is switched off".
- "Buffer: N / 819200 FE".

## Problems

- "No power": the projector has too little FE. Connect it, or add generators.
- "No material for some cells": no matching item is near and the domain has too little free MU. Fill your Matter
  Buffers.
- "Outside a colony domain": move the projector into your domain.
- "No snapshot": click Snapshot.
- "Restoration is switched off": the server config turns restoration off.
- A size button does nothing: the new box is too big, or it leaves the domain.
- If you break the projector, its field and its snapshot are gone.

## Next

- A field that puts back 64 or more blocks after one blast gives the advancement "Undo the Crater".
- Protect the projector itself with <ItemLink id="autarkia:bastion_block" /> walls.
