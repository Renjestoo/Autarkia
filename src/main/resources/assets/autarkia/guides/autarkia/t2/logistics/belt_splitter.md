---
navigation:
  title: Belt Splitter
  parent: t2/logistics.md
  position: 20
  icon: autarkia:belt_splitter
item_ids:
  - autarkia:belt_splitter
---

# Belt Splitter

<ItemImage id="autarkia:belt_splitter" scale="2" />

The belt splitter sits inside a belt line. It has one input at the back and two outputs: the front and one side. It
splits a line in two, or it takes the items that you choose out of the line.

## Get it

<RecipeFor id="autarkia:belt_splitter" />

## Use it

1. Look in the direction that the items move. Place the splitter at the end of a belt. It faces away from you, like
   the belt.
2. Put a belt, a chest or a machine in front of it and one at its output side.
3. Open the splitter and choose the mode and the side.
4. In Filter mode, set the filter: click a filter slot with an item. The slot shows a copy, and the item stays with you.
   Click a slot with an empty hand to clear it.

The splitter takes 2 FE/t from the belt line that feeds it.

## Screen

- "Mode: Alternate": entries go to the front and to the side in turn. When one output is full, they go to the other.
- "Mode: Filter": entries that match the filter go to the side. All other entries go to the front. A full output
  stops the splitter.
- "Side: Left" or "Side: Right": the side output, as you look in the direction of the items.
- "Whitelist": an entry matches when its item is in the filter. "Blacklist": an entry matches when its item is not in
  the filter.
- "Item only": the item type must match. "Match components": the item and its data (for example enchantments) must
  match.
- 3 x 3 filter slots.

Jade shows "Mode: Alternate, side left, filter 0" or "Mode: Filter, side right, filter 3". The number is the count of
filter slots with an item.

## Numbers

| Property | Value |
|---|---|
| Power | 2 FE/t |
| Buffer | 1 entry |
| Filter slots | 9 |
| Speed | the belt speed, 2 blocks per second |

## Problems

- Nothing moves: the splitter has no power. Feed it from a powered belt line.
- Items enter from the wrong side: the splitter takes items only at its back, from a belt or a block that faces the
  same way.
- The line stops in Filter mode: one output is full. Empty it.

## Next

- Sort without power with the <ItemLink id="autarkia:sorting_chute" />.
- The filter settings go into a Pattern. See the <ItemLink id="autarkia:pattern_loom" />.
