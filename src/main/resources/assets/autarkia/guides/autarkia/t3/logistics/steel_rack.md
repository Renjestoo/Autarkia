---
navigation:
  title: Steel Rack
  parent: t3/logistics.md
  position: 10
  icon: autarkia:steel_rack
item_ids:
  - autarkia:steel_rack
---

# Steel Rack

<ItemImage id="autarkia:steel_rack" scale="2" />

The steel rack holds 3 crates and carries 4 times the load of a wood rack. It is the shelf of the warehouse. An overloaded rack bends and then collapses, so watch its load.

## Get it

<RecipeFor id="autarkia:steel_rack" />

## Use it

1. Place the rack on a solid block or on top of another rack.
2. Use a <ItemLink id="autarkia:crate" /> on the rack. The crate goes into the first free position.
3. Open the rack to see its crates.
4. Push the **»** button next to a crate to show the 27 slots of that crate.
5. Move items into or out of the open crate.
6. Put the rack in the area of a <ItemLink id="autarkia:warehouse_controller" /> to add it to the warehouse.

Hoppers and chutes can reach all crates of a rack. A rack takes crates with contents or empty crates.

## Load

Each item has a load in units. A crate adds 8 units.

| Item class | Load per item |
|---|---|
| Light items | 0.25 |
| Most items | 0.5 |
| Ingots and ores | 1 |
| Machines (blocks with a block entity) | 8 |
| Storage blocks | 9 |
| Each crate | 8 |

| Load | What occurs |
|---|---|
| up to 100% | normal |
| above 100% | the rack bends and creaks |
| above 120% | the rack is critical: it collapses after 60 s |
| above 150% with the impact of a falling crate | the rack collapses at once |

When a rack collapses, its crates fall as falling crates. A rack on top of it also collapses. A falling crate can hit the next rack in the row and make it collapse too. The rack drops 2 <ItemLink id="autarkia:steel_nugget" />.

## Numbers

| Property | Value |
|---|---|
| Crate positions | 3 |
| Slots | 3 x 27 |
| Load rating | 8,192 units |
| Collapse time when critical | 60 s |

## Screen and Jade

- The load bar is green up to 100%, yellow up to 120% and red above. The white mark is 100%.
- "N / M units": the load and the rating.
- "Collapse in N s": the countdown of a critical rack.
- Jade shows "Load: N / M units (P%)", "Stage N" (0 to 3), "Crates: N / 3" and "Collapse in N s".

## Problems

- "Stage 1" or "Stage 2": the rack is above 100%. Take heavy items out.
- "Collapse in N s": the rack is above 120%. Take items out before the countdown ends.
- The rack collapsed when you broke the block below: a rack needs a sturdy block or a rack below it.

## Next

- At Tier 3 the colony replaces the wood racks of an overloaded row with steel racks. It puts a <ItemLink id="autarkia:prop_beam" /> under racks that stand on nothing.
- <ItemLink id="autarkia:lattice_rack" /> is the Tier 5 rack.
