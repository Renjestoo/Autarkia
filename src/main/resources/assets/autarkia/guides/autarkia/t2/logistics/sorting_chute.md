---
navigation:
  title: Sorting Chute
  parent: t2/logistics.md
  position: 30
  icon: autarkia:sorting_chute
item_ids:
  - autarkia:sorting_chute
---

# Sorting Chute

<ItemImage id="autarkia:sorting_chute" scale="2" />

The sorting chute sits inside a belt line and sorts items without power. Entries that match its filter leave through
the side. All other entries go on to the front.

## Get it

<RecipeFor id="autarkia:sorting_chute" />

## Use it

1. Look in the direction that the items move. Place the sorting chute at the end of a belt. It faces away from you.
2. Put a belt, a chest or a machine in front of it and one at its output side.
3. Open the sorting chute and choose the side.
4. Set the filter: click a filter slot with an item. The slot shows a copy, and the item stays with you. Click a slot
   with an empty hand to clear it.

## Screen

- "Side: Left" or "Side: Right": the side output, as you look in the direction of the items.
- "Whitelist": an entry matches when its item is in the filter. "Blacklist": an entry matches when its item is not in
  the filter.
- "Item only": the item type must match. "Match components": the item and its data (for example enchantments) must
  match.
- 3 x 3 filter slots.

With an empty filter and Whitelist, no entry matches, so all entries go to the front.

Jade shows "Mode: Filter, side left, filter 2". The number is the count of filter slots with an item.

## Numbers

| Property | Value |
|---|---|
| Power | none |
| Buffer | 1 entry |
| Filter slots | 9 |
| Speed | the belt speed, 2 blocks per second |

## Problems

- The line stops: the side output or the front output is full. A full output blocks the chute. Empty it.

## Next

- Split a line in two with the <ItemLink id="autarkia:belt_splitter" />.
- Store the sorted items on a <ItemLink id="autarkia:wood_rack" />.
