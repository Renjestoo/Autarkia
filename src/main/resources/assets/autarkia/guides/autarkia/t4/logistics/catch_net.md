---
navigation:
  title: Catch Net
  parent: t4/logistics.md
  position: 40
  icon: autarkia:catch_net
item_ids:
  - autarkia:catch_net
---

# Catch Net

<ItemImage id="autarkia:catch_net" scale="2" />

The catch net is the target of a <ItemLink id="autarkia:crate_launcher" />. It is 3 x 1 x 3 blocks. It catches a
launched crate and puts all items into the storage below its center.

## Get it

<RecipeFor id="autarkia:catch_net" />

## Use it

1. Place the net. It covers 3 x 3 blocks around the block you place it on. The net hangs near the top of the blocks.
2. Put a chest, a crate or other storage directly below the center block of the net.
3. Use a <ItemLink id="autarkia:wrench" /> on a crate launcher, then on the net. The launcher now aims at the net.
4. A crate that comes down within 1.5 blocks of the center is caught. The items and the empty crate go into the
   storage below. The net bounces and a puff of smoke shows.

When the storage below has no room for all items and the crate, nothing goes in. The crate bounces off and lands
beside the net as a crate block. The launcher counts this as a miss.

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 1 x 3 blocks |
| Catch radius | 1.5 blocks from the center |
| Output | The storage directly below the center block |
| Hardness | 1.5 |
| Mine with | Any tool, a pickaxe is fastest |

## Problems

Jade shows "Output below: OK", "Output below: Missing" or "Output below: Full".

- "Output below: Missing": no storage stands below the center. Place one there.
- "Output below: Full": the storage below is full. Empty it, or the crates bounce off.

## Next

- Read <ItemLink id="autarkia:crate_launcher" /> for the range and the arcs.
