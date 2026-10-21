---
navigation:
  title: Porters
  parent: t1/index.md
  position: 6
  icon: autarkia:walker_frame
item_ids:
  - autarkia:walker_frame
  - autarkia:worker_assembler
  - autarkia:winding_post
  - autarkia:worker_capsule
---

# Porters

The Porter is the Tier 1 worker. It runs on a wound spring. It fetches items from crates, levels the ground, places
blocks, strings rope and turns cranks. A Tier 1 colony has room for 4 Porters.

## Building a Porter

Put a <ItemLink id="autarkia:walker_frame" />, 2 mainsprings, 4 bronze gears and 4 bronze ingots into the
<ItemLink id="autarkia:worker_assembler" />. Give it 20 FE/t for 600 ticks. The Porter walks out of the front.

<RecipeFor id="autarkia:worker_assembler" />

<RecipeFor id="autarkia:walker_frame" />

## Winding

A Porter goes to a <ItemLink id="autarkia:winding_post" /> when its spring is low. The post winds it with power from a
span. You can also hold use on the post to wind by hand. A Porter with an empty spring stops; right-click it to give it
a little tension.

<GameScene zoom="5" interactive={true}>
  <ImportStructure src="/structures/winding_post.nbt" />
</GameScene>

<RecipeFor id="autarkia:winding_post" />

## Capsules

Sneak and use a <ItemLink id="autarkia:worker_capsule" /> on your idle Porter to store it. Use the capsule on the
ground in your domain to let it out again. A stored Porter does not count against the worker cap.

<RecipeFor id="autarkia:worker_capsule" />
