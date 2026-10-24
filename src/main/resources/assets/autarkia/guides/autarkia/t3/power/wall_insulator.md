---
navigation:
  title: Wall Insulator
  parent: t3/power.md
  position: 30
  icon: autarkia:wall_insulator
item_ids:
  - autarkia:wall_insulator
---

# Wall Insulator

<ItemImage id="autarkia:wall_insulator" scale="2" />

The wall insulator is a small node for <ItemLink id="autarkia:insulated_cable_spool" />. It sits on the face of a block. Use it to bring a line into a building, or to power a machine or a belt line from a wall.

## Get it

<RecipeFor id="autarkia:wall_insulator" />

One craft gives 4 insulators.

## Use it

1. Place the insulator on the face of a solid block.
2. Use an insulated cable spool on the insulator, then on the next node.
3. Put a machine next to the insulator. It joins the network.

The insulator drops as an item when its wall block is gone.

## Numbers

| Property | Value |
|---|---|
| Cable tier | insulated cable only |
| Spans | 2 |
| Longest span to or from it | 12 blocks |

## Problems

- "Too long: max 24 blocks for insulated cable": the message shows the cable limit. A span to a wall insulator is at most 12 blocks.
- "Node is full": the insulator has 2 spans. Use a <ItemLink id="autarkia:steel_pylon" /> for more spans.
- The insulator is gone: its wall block was removed. Place it again on a solid face.

## Next

- <ItemLink id="autarkia:steel_pylon" /> holds 6 spans of up to 24 blocks.
- A <ItemLink id="autarkia:belt" /> line takes its power from a node next to any of its belts.
