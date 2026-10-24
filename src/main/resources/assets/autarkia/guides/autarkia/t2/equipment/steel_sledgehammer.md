---
navigation:
  title: Steel Sledgehammer
  parent: t2/equipment.md
  position: 60
  icon: autarkia:steel_sledgehammer
item_ids:
  - autarkia:steel_sledgehammer
---

# Steel Sledgehammer

<ItemImage id="autarkia:steel_sledgehammer" scale="2" />

The steel sledgehammer is a heavy pickaxe with two modes. In 3x3 mode it breaks a square of 9 blocks with one swing.
Use it to dig tunnels and to clear stone fast.

## Get it

<RecipeFor id="autarkia:steel_sledgehammer" />

Make the steel plates in the <ItemLink id="autarkia:plate_press" />. Craft the <ItemLink id="autarkia:steel_gear" />
from steel plates and an iron ingot.

## Use it

1. Hold the sledgehammer in your main hand.
2. Push the mode key (V by default) to change the mode. The text above the hotbar shows "Mode: 1x1" or "Mode: 3x3".
   The mode also shows to the left of the hotbar.
3. In 1x1 mode, it mines one block like a pickaxe.
4. In 3x3 mode, hit a block. When the block breaks, the 8 blocks around it break too.

The square is at right angles to the face you hit. Hit a wall to dig a tunnel. Hit the floor to dig down.

In 3x3 mode the hammer breaks an extra block only when:

- the hammer can drop that block,
- the block is not much harder than the block you hit (hardness at most 2 more).

## Numbers

| Property | Value |
|---|---|
| Modes | 1x1, 3x3 |
| Mining speed | 7.5 (1x1), 35% of that in 3x3 |
| Mining level | Same as diamond |
| Durability | 2,400 |
| Durability per block | 1 |
| Attack damage | 10.5 |
| Attack speed | 0.6 |
| Shield disable | 5 s |
| Repair | Steel plate |

## Problems

- "This item has no modes.": the item in your main hand has no modes. Hold the sledgehammer in your main hand, then
  push the mode key.
- Some blocks around the hit block do not break: they are too hard, or the hammer cannot drop them. Mine them one by
  one.
- The hammer mines slowly: 3x3 mode uses 35% speed. Change to 1x1 mode for single blocks.

## Next

- <ItemLink id="autarkia:steel_pickaxe" /> for normal mining.
- Steel armor gives +4% block break speed for each piece: <ItemLink id="autarkia:steel_helmet" />.
