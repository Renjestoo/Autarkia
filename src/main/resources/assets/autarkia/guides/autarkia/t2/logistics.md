---
navigation:
  title: Belts and Sorting
  parent: t2/index.md
  position: 5
  icon: autarkia:belt
item_ids:
  - autarkia:belt
  - autarkia:belt_splitter
  - autarkia:sorting_chute
---

# Belts and Sorting

A <ItemLink id="autarkia:belt" /> carries up to 8 items per entry at 2 blocks per second, two entries per block.
Belts that face the same way join into a line of up to 128 belts. A belt slopes up or down by itself when the next belt
of the line is one block higher or lower.

All belts of a line share one 1,000 FE buffer and use 1 FE/t each. Without power the whole line stops and the items
stay where they are. A line whose last belt cannot hand over its items jams after 30 seconds: a red stripe, a scraping
sound, and the Colony Core notices.

Items enter at the back of the first belt, or from the side or the top of any belt. The last belt puts them into the
block in front: a chest, a crate, a machine, a chute or another belt.

## Splitting and sorting

The <ItemLink id="autarkia:belt_splitter" /> sends entries to the front and to one side in turn, or with a filter sends
matching items to the side. The <ItemLink id="autarkia:sorting_chute" /> needs no power: matching items leave to the
side, the rest goes on. Both use a 3 x 3 filter: a click with an item sets a slot, a click with an empty hand clears it.

<RecipeFor id="autarkia:belt" />
