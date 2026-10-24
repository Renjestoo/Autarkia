---
navigation:
  title: Worker Assembler
  parent: t1/workers.md
  position: 40
  icon: autarkia:worker_assembler
item_ids:
  - autarkia:worker_assembler
---

# Worker Assembler

<ItemImage id="autarkia:worker_assembler" scale="2" />

The worker assembler is the workshop that builds workers. At Tier 1 it builds the Porter. It is a 2 x 2 x 2 machine
and it runs on FE.

## Get it

<RecipeFor id="autarkia:worker_assembler" />

You need 5 <ItemLink id="autarkia:bronze_block" />, 1 <ItemLink id="autarkia:bronze_gear" />, 2 planks and 1
crafting table.

## Use it

1. Place the assembler inside your colony's domain. Its front faces you. The new worker walks out of the front.
2. Keep the space in front of the assembler free.
3. Connect the assembler to your power network. See [Power](../power.md).
4. Right-click the assembler. The screen has 1 frame slot and 6 part slots.
5. Put a <ItemLink id="autarkia:walker_frame" /> into the frame slot.
6. Put the parts into the part slots. Put each part type in its own slot, with the full count in that slot.
7. Wait. The assembler works for 600 ticks (30 seconds) at 20 FE/t.
8. The Porter walks out, unfolds and joins your colony.

## Porter assembly

GuideME cannot show this recipe type, so it is shown here as a table.

| Input | Count |
|---|---|
| <ItemLink id="autarkia:walker_frame" /> | 1 (frame slot) |
| <ItemLink id="autarkia:mainspring" /> | 2 |
| <ItemLink id="autarkia:bronze_gear" /> | 4 |
| <ItemLink id="autarkia:bronze_ingot" /> | 4 |

| Property | Value |
|---|---|
| Time | 600 ticks (30 seconds) |
| Power | 20 FE/t (12,000 FE in total) |
| Lowest Core tier | 1 |
| Output | 1 Porter |

Later tiers build more types of workers in this assembler.

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2 |
| Slots | 1 frame slot, 6 part slots |
| FE buffer | 40,000 FE |
| Largest FE input | 400 FE/t |

## Problems

The assembler screen and Jade show the status.

- "Waiting for parts": a part is missing, or a count in one slot is too low. Check the table above.
- "No power": the buffer has less than 20 FE. The progress stops until power comes back. Check the power network.
- "Worker cap full": the colony has 4 Porters. Pick up a worker with a <ItemLink id="autarkia:worker_capsule" />, or
  upgrade the Core. The assembler waits with the parts inside.
- "Core tier too low": this worker needs a Core of a higher tier.
- "Outside a colony domain": the assembler is not inside a domain. Move it.

## Next

- Wind your new Porter at a <ItemLink id="autarkia:winding_post" />.
- Read what a Porter does in [Porters](../workers.md).
