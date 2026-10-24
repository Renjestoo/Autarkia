---
navigation:
  title: Lattice Geode Shell
  parent: t5/materials.md
  position: 10
  icon: autarkia:lattice_geode_shell
item_ids:
  - autarkia:lattice_geode_shell
---

# Lattice Geode Shell

<ItemImage id="autarkia:lattice_geode_shell" scale="2" />

The lattice geode shell is the inner wall of a lattice geode. Some shells are charged. A charged shell grows new
<ItemLink id="autarkia:lattice_cluster" /> crystals, and the clusters give the lattice shards of Tier 5.

## Get it

The shell has no recipe. It generates in lattice geodes deep under the Overworld.

| Worldgen property | Value |
|---|---|
| Dimension and biomes | Every Overworld biome |
| Height | Y -58 to Y -24 (uniform) |
| Rarity | About 1 geode in 40 chunks |
| Outer layer | Blackstone |
| Middle layer | Calcite |
| Inner layer | Lattice geode shell |
| Charged part of the inner layer | 6% |
| Inside | Air, with lattice clusters on the charged shells |
| Crack in the wall | 60% chance |

1. Dig down to Y -24 or lower.
2. Look for blackstone and calcite in the deepslate. They show the outside of a geode.
3. Break through to the hollow center.

An uncharged shell drops itself when you mine it with a pickaxe. A charged shell drops nothing.

## Use it

1. Find the charged shells in the geode. They have bright glowing veins and show glow particles now and then.
2. Leave the charged shells in place. You cannot move them, and they break into nothing.
3. Keep the faces next to the charged shells free. A new cluster grows only into air or a water source.
4. Come back later and mine the new clusters.

## Numbers

| Property | Value |
|---|---|
| Growth chance of a charged shell | 0.2 per random tick (config `geode.chargedShellGrowthChance`) |
| Growth face | A random side; it must be air or a water source |
| Charged shell drop | Nothing |
| Charged shell and pistons | Cannot be moved |
| Uncharged shell drop | Itself |
| Hardness, blast resistance | 2, 6 |
| Mining tool | Pickaxe |
| Matter value | None |

## Problems

- The shell drops nothing: it is a charged shell. This is normal. Do not break charged shells; they are the only
  source of new clusters.
- No cluster grows: the faces next to the charged shell are blocked. Remove the blocks there.

Jade shows "Charged" on a charged shell.

## Next

- Mine the <ItemLink id="autarkia:lattice_cluster" /> for <ItemLink id="autarkia:lattice_shard" />.
