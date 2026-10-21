---
navigation:
  title: Racks and Load
  parent: t2/index.md
  position: 6
  icon: autarkia:wood_rack
item_ids:
  - autarkia:wood_rack
---

# Racks and Load

A <ItemLink id="autarkia:wood_rack" /> holds two crates, empty or full. Belts, hoppers and workers can put items into
the crates on a rack. A rack needs a solid block or another rack below it.

Every rack has a load rating: 2,048 units for wood. Each crate weighs 8 units, and each item adds its class:

| Class | Units | Examples |
|---|---|---|
| Light | 0.25 | paper, seeds, food, nuggets |
| Standard | 0.5 | most items, cobblestone |
| Dense | 1 | ores, ingots, plates, gears |
| Machine | 8 | machines, anvils |
| Heavy | 9 | storage blocks, obsidian |

Two crates of cobblestone are 85% of a wood rack. Two crates of iron ingots are 170%.

## Bend and collapse

Above 100% the shelves sag and creak. Above 110% the uprights lean. Above 120% a red tag shows a countdown: after 60
seconds the rack collapses. The crates fall, land as crate blocks with everything inside, and can hit the next rack.
A falling crate that pushes a rack above 150% makes it collapse at once.

<RecipeFor id="autarkia:wood_rack" />
