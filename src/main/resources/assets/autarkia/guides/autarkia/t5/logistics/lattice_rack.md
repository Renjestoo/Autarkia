---
navigation:
  title: Lattice Rack
  parent: t5/logistics.md
  position: 40
  icon: autarkia:lattice_rack
item_ids:
  - autarkia:lattice_rack
---

# Lattice Rack

<ItemImage id="autarkia:lattice_rack" scale="2" />

The lattice rack is the strongest crate rack. It holds 4 crates and carries 4 crates of iron blocks with room to spare.

## Get it

<RecipeFor id="autarkia:lattice_rack" />

## Use it

1. Place the rack on a solid block or on another rack.
2. Hold a <ItemLink id="autarkia:crate" /> and use it on the rack. The crate goes into the first free position.
3. Watch the load. The load of a crate shows in its tooltip as "Load: N units".

## Numbers

| Property | Value |
|---|---|
| Crate positions | 4 (filled or empty crates) |
| Load rating | 65,536 units |
| Example | 4 crates of iron blocks: 62,240 units (95%) |
| Bends | Above 100% of the rating |
| Collapses | After 60 s above 120%, at once above 150% |
| Drop when it collapses | 2 <ItemLink id="autarkia:lattice_shard" /> |
| Mining tool | Iron-level pickaxe |

## Problems

- The rack bends: the load is above 100%. Take crates out before it reaches 120%.
- The rack collapses: the load was too high, or its support went. A rack needs a solid block or a rack below it.

## Next

- Fill the crates with a mag rail line: see the <ItemLink id="autarkia:mag_rail_station" />.
