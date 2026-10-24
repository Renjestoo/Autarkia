---
navigation:
  title: Porters
  parent: t1/index.md
  position: 6
  icon: autarkia:walker_frame
---

# Porters

The Porter is the Tier 1 worker. It is a small walker that runs on a wound spring. Porters do the work of the colony:
they carry items from crates, level the ground, place blocks, string rope and turn cranks. A Tier 1 colony has room for
4 Porters.

## Get a Porter

1. Your first Porter comes in a <ItemLink id="autarkia:worker_capsule" /> when the Colony Core blooms.
2. Use the capsule on the top of a block inside your domain. The Porter unfolds.
3. To make more Porters, craft a <ItemLink id="autarkia:walker_frame" /> and a
   <ItemLink id="autarkia:worker_assembler" />.
4. Put the frame, 2 <ItemLink id="autarkia:mainspring" />, 4 <ItemLink id="autarkia:bronze_gear" /> and 4
   <ItemLink id="autarkia:bronze_ingot" /> into the assembler. Give it 20 FE/t for 600 ticks.

## Energy

A Porter stores energy as spring tension (T). Walking and work use tension. When the spring falls below the reserve
that the Porter needs to walk back, it stops its job. It walks to the nearest free
<ItemLink id="autarkia:winding_post" /> and winds up. The post uses FE from your power network, or you wind it by hand.

A Porter with an empty spring stops and smokes. Right-click it with an empty hand to give it 100 T.

## What a Porter does

A Porter with 20% charge or more takes open jobs from the colony:

- Fetch items from <ItemLink id="autarkia:crate" /> blocks, carry them and deliver them.
- Cut and fill the ground on a project site.
- Place the blocks of a project.
- String rope spans.
- Turn a <ItemLink id="autarkia:hand_crank" /> or walk in a <ItemLink id="autarkia:treadwheel" /> when power is short.
- Put up a sign when a project is done.

## Porter numbers

| Property | Value |
|---|---|
| Health | 20 |
| Armor | 2 |
| Spring | 6,000 T |
| Use when idle | 1 T each second |
| Use when walking | 20 T each second, 10 T more when it carries items |
| Use for each placed block | 40 T |
| Cargo | 1 stack (64 items) or 1 crate |
| Power at a hand crank | 10 FE/t |
| Power in a treadwheel | 24 FE/t |
| Cables | Rope only |
| Repair | Right-click with a <ItemLink id="autarkia:bronze_ingot" />: 4 health |
| Cap cost | 1 |

## Control your Porters

- The Workers tab of the Colony Core shows each worker, its state, its job and its charge.
- Click Recall to send a worker back to the Core.
- Sneak and use a worker capsule on an idle worker to pick it up.
- When a worker is stuck, members inside the domain see "A worker is stuck at" and its position.

<SubPages />
