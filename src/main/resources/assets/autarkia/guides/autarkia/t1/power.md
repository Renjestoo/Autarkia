---
navigation:
  title: Muscle and Wind
  parent: t1/index.md
  position: 1
  icon: autarkia:hand_crank
item_ids:
  - autarkia:hand_crank
  - autarkia:rope_pulley
  - autarkia:rope_coil
  - autarkia:treadwheel
  - autarkia:windmill
  - autarkia:stone_flywheel
  - autarkia:multimeter
---

# Muscle and Wind

## Hand crank

Hold use on the <ItemLink id="autarkia:hand_crank" /> to make 20 FE/t. A Porter makes 10 FE/t. The crank stops
6 ticks after you let go.

<RecipeFor id="autarkia:hand_crank" />

## Rope drive

A rope span carries up to 40 FE/t over 12 blocks. Click one node with a <ItemLink id="autarkia:rope_coil" />, then
click the second node. Machines, generators and the <ItemLink id="autarkia:rope_pulley" /> are nodes. Above 40 FE/t
the rope slips, wears and snaps.

<RecipeFor id="autarkia:rope_pulley" />

## Treadwheel

Lead a horse, donkey, mule or llama into the <ItemLink id="autarkia:treadwheel" /> for 20 FE/t. A villager gives 16,
a cow, pig, sheep or goat 12, and a Porter 24.

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="/structures/treadwheel.nbt" />
</GameScene>

## Windmill

The <ItemLink id="autarkia:windmill" /> is 4 blocks tall and makes 4 to 40 FE/t. Height, wind, rain and thunder add
output. Each solid block in the 5 x 5 area in front of the rotor takes 10%.

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="/structures/windmill.nbt" />
</GameScene>

## Stone flywheel

The <ItemLink id="autarkia:stone_flywheel" /> stores 40,000 FE. Its input and output grow with its spin. Do not
break it while it holds 2,000 FE or more: it rolls away.

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="/structures/flywheel.nbt" />
</GameScene>

Use a <ItemLink id="autarkia:multimeter" /> on a node to see generation, demand, losses and every span.
