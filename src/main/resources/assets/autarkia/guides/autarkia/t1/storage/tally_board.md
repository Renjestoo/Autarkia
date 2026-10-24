---
navigation:
  title: Tally Board
  parent: t1/storage.md
  position: 40
  icon: autarkia:tally_board
item_ids:
  - autarkia:tally_board
---

# Tally Board

<ItemImage id="autarkia:tally_board" scale="2" />

The tally board counts the items in the crates near it. Its face shows the 6 items with the largest counts. The
Colony Core also reads the counts.

## Get it

<RecipeFor id="autarkia:tally_board" />

## Use it

1. Place the tally board near your crates.
2. All crates within 5 blocks in each direction count. This is a cube of 11 x 11 x 11 blocks.
3. Read the 6 largest counts on its face. The board counts again every 2 seconds.

## Numbers

| Property | Value |
|---|---|
| Range | 5 blocks in each direction |
| Update | Every 40 ticks (2 s) |
| Counts shown | 6 |
| Counted blocks | Crates |
| Mining tool | Axe |

## Problems

- An item does not show: it is not in the 6 largest counts, or its crate is more than 5 blocks away.
- A chest does not count: the tally board counts only crates.

## Next

- Keep many <ItemLink id="autarkia:crate" /> blocks around one tally board.
