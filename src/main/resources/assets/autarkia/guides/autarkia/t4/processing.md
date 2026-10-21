---
navigation:
  title: Vantite and Flotation
  parent: t4/index.md
  position: 2
  icon: autarkia:froth_flotation_plant
item_ids:
  - autarkia:vantite_ore
  - autarkia:raw_vantite
  - autarkia:raw_vantite_block
  - autarkia:vantite_ingot
  - autarkia:vantite_block
  - autarkia:vantite_steel_ingot
  - autarkia:vantite_steel_block
  - autarkia:crushed_vantite
  - autarkia:washed_vantite
  - autarkia:pure_vantite
  - autarkia:ore_outcrop
  - autarkia:froth_flotation_plant
  - autarkia:iron_concentrate
  - autarkia:copper_concentrate
  - autarkia:gold_concentrate
  - autarkia:tin_concentrate
  - autarkia:zinc_concentrate
  - autarkia:vantite_concentrate
  - autarkia:advanced_circuit
---

# Vantite and Flotation

## Vantite

<ItemLink id="autarkia:vantite_ore" /> is only found from Y -64 to 0. It needs a diamond pickaxe or better. Small
veins of 5 blocks come 3 times per chunk, and half of the blocks next to air are removed, so most ore is hidden in the
rock. A vantite deposit (1 in 16 chunks, Y -56 to -16) is a large vein with an outcrop above it.

The ore drops <ItemLink id="autarkia:raw_vantite" />. Smelt it to <ItemLink id="autarkia:vantite_ingot" />. There is
no vantite nugget. Store 9 in a <ItemLink id="autarkia:raw_vantite_block" /> or a
<ItemLink id="autarkia:vantite_block" />.

<ItemLink id="autarkia:vantite_steel_ingot" />: the blast smelter alloys 1 vantite ingot and 1 steel ingot in 300
ticks. 9 ingots make a <ItemLink id="autarkia:vantite_steel_block" />. Vantite steel goes into most tier 4 and tier 5
machines and the vantite equipment.

Vantite has the full set of ore forms: <ItemLink id="autarkia:crushed_vantite" />,
<ItemLink id="autarkia:washed_vantite" />, <ItemLink id="autarkia:pure_vantite" /> and
<ItemLink id="autarkia:vantite_concentrate" />.

## Ore outcrops

An <ItemLink id="autarkia:ore_outcrop" /> is a small rock pile on the surface above an ore deposit. Its type is iron,
copper, gold, tin, zinc or vantite. It only appears when the deposit below it has at least 12 ore blocks (8 for gold
and vantite). Break it to get 1 raw ore of its type. The Surveyor, the survey lens and the colony deposit list use
the outcrops to find deposits.

## Froth flotation plant

The <ItemLink id="autarkia:froth_flotation_plant" /> is 3 x 3 x 3. It uses 768 FE/t and needs 200 ticks per
operation.

- Input: 1 crushed ore and 250 mB of water.
- Output: 2 concentrate. Chance outputs: 1 slag (50%) and 1 secondary nugget (35%).
- Water tank: 8,000 mB. Fill it with a water bucket in the bucket slot (the empty bucket goes back into the same
  slot), with a fluid pipe, or with its pump: 50 mB per tick from a water source that touches a bottom position. The
  source stays.

Bubbles rise in the tank window while it runs. Concentrate (<ItemLink id="autarkia:iron_concentrate" />,
<ItemLink id="autarkia:copper_concentrate" />, <ItemLink id="autarkia:gold_concentrate" />,
<ItemLink id="autarkia:tin_concentrate" />, <ItemLink id="autarkia:zinc_concentrate" />, vantite concentrate) smelts
1 to 1 in a furnace, the blast smelter or the electrolyzer.

## The 4x ore line

1. Jaw crusher: 1 raw ore to 2 crushed ore.
2. Froth flotation plant: 1 crushed ore to 2 concentrate.
3. Smelt: 1 concentrate to 1 ingot.

This gives 4 ingots per raw ore, with about 0.7 secondary nuggets and 1 slag.

## Advanced circuit

The electrolyzer etches an <ItemLink id="autarkia:advanced_circuit" /> from 2 basic circuits and 1 vantite ingot in
400 ticks. Tier 4 machines, outposts and the tier 4 workers need it.

<RecipeFor id="autarkia:froth_flotation_plant" />
