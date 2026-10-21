---
navigation:
  title: Crates and Chutes
  parent: t1/index.md
  position: 3
  icon: autarkia:crate
item_ids:
  - autarkia:crate
  - autarkia:tally_board
  - autarkia:chute
  - autarkia:wrench
---

# Crates and Chutes

The <ItemLink id="autarkia:crate" /> holds 27 stacks and shows piles of its contents on top. Sneak and use a
<ItemLink id="autarkia:wrench" /> to pick it up with everything inside.

The <ItemLink id="autarkia:tally_board" /> counts the items of all crates within 5 blocks. The Colony Core reads the
crates of its domain: when they are 85% full, it proposes a new crate cluster.

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="/structures/crate_cluster.nbt" />
</GameScene>

A <ItemLink id="autarkia:chute" /> slides items one block down and forward. It pulls from an inventory above it.

<RecipeFor id="autarkia:crate" />
