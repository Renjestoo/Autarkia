---
navigation:
  title: Matter Buffer and Mag Rail
  parent: t5/index.md
  position: 3
  icon: autarkia:matter_buffer
item_ids:
  - autarkia:matter_buffer
  - autarkia:mag_rail
  - autarkia:mag_rail_station
  - autarkia:lattice_rack
---

# Matter Buffer and Mag Rail

## Matter Buffer

The <ItemLink id="autarkia:matter_buffer" /> is 2 x 2 x 2. It holds 1,048,576 matter units (MU). Items keep their
identity: you get the same items back. Stored items and free matter share the capacity.

- Dissolve: in the screen, turn an item into free matter. This cannot be undone.
- Auto-dissolve: when the free matter of the domain is low, the buffer dissolves cobblestone, cobbled deepslate, dirt,
  gravel, sand, netherrack, tuff and slag.
- It refuses items without a matter value, filled containers, seeds of the colony and worker capsules.

Free matter feeds the matter compiler, Nanite Reform, the Restoration Field and the Autarky Seed. Nanite claims fill
it. When the buffers are full, the colony proposes another one.

| Item | MU |
|---|---|
| Stone, dirt, sand, gravel, glass, planks | 1 |
| Log | 4 |
| Coal | 9 |
| Copper, tin, zinc, bronze or brass ingot | 27 |
| Iron ingot | 36 |
| Steel ingot | 54 |
| Gold ingot | 108 |
| Vantite ingot | 270 |
| Vantite steel ingot | 324 |
| Lattice shard, crystal, alloy ingot | 216, 864, 1,188 |
| Diamond | 512 |
| Netherite ingot | 4,096 |
| Aegis plate | 32,768 |
| Raw ore | its ingot x 6 in a tier 5 domain, x 8 in a tier 6 domain |

## Mag rail

<ItemLink id="autarkia:mag_rail" /> is a floating crate track. It needs a solid block below it. It has slopes and
corners but no junctions. Minecarts do not use it.

A <ItemLink id="autarkia:mag_rail_station" /> sits in the line or at its end. It has 27 input slots, 27 output slots,
a slot for 16 empty crates and a 16,000 FE buffer. Set its name (up to 16 characters), its destination (a station
name, or Next) and a filter.

- It sends when the input is full, or not empty for 200 ticks. It packs the items into an empty crate and launches a
  sled that floats 0.4 blocks above the rail. A launch costs 2,000 FE.
- The sled speeds up to 20 blocks per second and brakes in the last 20 blocks. It passes other stations.
- One track for both directions: a sled waits until no sled comes the other way. Sleds in the same direction keep 8
  blocks apart.
- At the destination, the items go into the output slots and the crate into the crate slot.
- A missing rail ahead makes the sled fall and land as a crate block. An entity on the track takes 6 damage.
- A line has at most 2,048 rails, 16 stations and 32 sleds.

## Lattice rack

A <ItemLink id="autarkia:lattice_rack" /> holds 4 crates and has a load rating of 65,536 units. Example: 4 crates of
iron blocks load it to 95%. A collapse leaves 2 lattice shards. At tier 5 the colony upgrades overloaded or collapsed
steel racks to lattice racks.

<RecipeFor id="autarkia:mag_rail" />
