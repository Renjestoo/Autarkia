---
navigation:
  title: Lattice and the Plasma Separator
  parent: t5/index.md
  position: 2
  icon: autarkia:plasma_separator
item_ids:
  - autarkia:lattice_cluster
  - autarkia:lattice_geode_shell
  - autarkia:lattice_shard
  - autarkia:lattice_crystal
  - autarkia:lattice_alloy_ingot
  - autarkia:lattice_alloy_block
  - autarkia:lattice_block
  - autarkia:plasma_separator
  - autarkia:nanite_paste
  - autarkia:ai_core
---

# Lattice and the Plasma Separator

## Lattice geodes

Lattice geodes are deep in the Overworld, from Y -58 to -24, in 1 of 40 chunks. They have an outer layer of
blackstone, a middle layer of calcite and an inner layer of <ItemLink id="autarkia:lattice_geode_shell" />.

- A charged shell grows a <ItemLink id="autarkia:lattice_cluster" /> on a free face next to it. Each random tick has a
  chance of 0.2. A charged shell drops nothing and pistons cannot move it. An uncharged shell drops itself.
- A cluster gives light level 7. Only a netherite level pickaxe (vantite tools and better) gets a drop: 2
  <ItemLink id="autarkia:lattice_shard" />, more with Fortune.

## Lattice materials

- <ItemLink id="autarkia:lattice_crystal" />: 4 lattice shards in a 2 x 2 crafting grid.
- <ItemLink id="autarkia:lattice_alloy_ingot" />: the plasma separator alloys 1 lattice crystal and 1 vantite steel
  ingot in 200 ticks. 9 ingots make a <ItemLink id="autarkia:lattice_alloy_block" />.
- <ItemLink id="autarkia:lattice_block" />: 8 stone around 1 lattice alloy ingot give 8. Hardness 6, blast resistance
  1,200. It needs a diamond pickaxe.

## Plasma separator

The <ItemLink id="autarkia:plasma_separator" /> is 2 x 2 x 2. It uses 4,096 FE/t and needs 100 ticks per operation.
It turns 1 raw ore into 6 ingots, with a 20% chance of 1 ingot of the secondary metal. With 2 inputs that match an
alloy recipe, it alloys instead.

This is the 6x ore step of tier 5: one machine, one step. Raw ore from nanite claims goes straight into it.

## Components

The fabricator makes the tier 5 components:

| Component | Inputs | Time |
|---|---|---|
| 2 <ItemLink id="autarkia:nanite_paste" /> | 1 lattice shard, 4 redstone, 1 slime ball | 200 ticks |
| 1 <ItemLink id="autarkia:ai_core" /> | 4 advanced circuits, 1 lattice crystal, 2 nanite paste, 1 diamond | 400 ticks |
| 1 lattice fuel cell | 2 lattice shards, 1 vantite steel ingot, 1 blaze powder | 200 ticks |
| 2 coolant cells | 1 vantite steel ingot, 2 blue ice, 1 lattice shard | 200 ticks |

The fabricator uses 160 FE per tick of recipe time for these recipes.

<RecipeFor id="autarkia:plasma_separator" />
