---
navigation:
  title: Blast Smelter
  parent: t2/processing.md
  position: 40
  icon: autarkia:blast_smelter
item_ids:
  - autarkia:blast_smelter
---

# Blast Smelter

<ItemImage id="autarkia:blast_smelter" scale="2" />

The blast smelter is a heavy powered furnace, 2 x 2 x 2 blocks. It makes 4 ingots from 3 washed ore, steel from iron
and coal, and brass from copper and zinc. It also smelts anything a furnace smelts. It must get hot before it works.

## Get it

<RecipeFor id="autarkia:blast_smelter" />

Make the brass plates in the <ItemLink id="autarkia:plate_press" /> and the copper coil in the
<ItemLink id="autarkia:wire_drawer" />.

## Use it

1. Put 4 <ItemLink id="autarkia:foundation" /> blocks in a 2 x 2 square. A heavy machine needs foundation under
   every bottom block.
2. Place the blast smelter on the foundation. If Jade shows "Needs a foundation under every bottom block", move the
   foundation.
3. Connect it to a power network. Read [Copper Wire](../power.md) for the network.
4. Use the blast smelter to open its menu. It has three input slots and two output slots.
5. Put in the inputs of one recipe. Each ingredient goes in its own input slot. Keep the other input slots empty.
6. The smelter heats up by 10 heat each tick and uses 64 FE/t. From cold it needs 100 ticks (5 s) to reach 1,000 heat.
7. At 1,000 heat it starts the recipe. Take the results from the output slots.

The smelter heats only when it has a recipe to work on. Without work or power it cools by 2 heat each tick.

### Recipes

| Inputs | Time | Outputs |
|---|---|---|
| 3 <ItemLink id="autarkia:washed_iron" /> | 200 ticks (10 s) | 4 iron ingots, <ItemLink id="autarkia:slag" /> (30%) |
| 3 <ItemLink id="autarkia:washed_copper" /> | 200 ticks (10 s) | 4 copper ingots, slag (30%) |
| 3 <ItemLink id="autarkia:washed_gold" /> | 200 ticks (10 s) | 4 gold ingots, slag (30%) |
| 3 <ItemLink id="autarkia:washed_tin" /> | 200 ticks (10 s) | 4 <ItemLink id="autarkia:tin_ingot" />, slag (30%) |
| 3 <ItemLink id="autarkia:washed_zinc" /> | 200 ticks (10 s) | 4 <ItemLink id="autarkia:zinc_ingot" />, slag (30%) |
| 1 iron ingot and 2 coal or charcoal | 200 ticks (10 s) | 1 <ItemLink id="autarkia:steel_ingot" />, slag (25%) |
| 3 copper ingots and 1 zinc ingot | 300 ticks (15 s) | 4 <ItemLink id="autarkia:brass_ingot" /> |
| 1 item that a furnace smelts (one input slot only) | 100 ticks (5 s) | The furnace result |

In Tier 4 the smelter also makes vantite ingots from washed vantite ore, and the
<ItemLink id="autarkia:vantite_steel_ingot" /> from 1 vantite ingot and 1 steel ingot (300 ticks).

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2 blocks, on foundation |
| Slots | 3 inputs, 2 outputs |
| Power use | 64 FE/t while it heats and while it works |
| Heat | 0 to 1,000; +10 per tick when it heats, -2 per tick when it cools |
| Works at | 1,000 heat only |
| Energy per steel ingot | 12,800 FE (when hot) |
| Energy buffer | 12,800 FE |
| Maximum input | 256 FE/t |
| Mine with | Pickaxe |

## Problems

- "Condition missing" with "Needs a foundation under every bottom block": a bottom block has no foundation under
  it. Put foundation under all four bottom blocks.
- "No power": the buffer has less than 64 FE. The smelter does not heat and cools down. Give it more power.
- "Idle": the input slots do not hold one full recipe. An extra item in a third slot stops a recipe. A furnace
  recipe needs one filled input slot only.
- "Output full": there is no room for every possible result, slag too. Take out the outputs.
- "Running" with "Heat" below 1,000: the smelter heats up. Wait until "Heat: 1000 / 1000".

Jade also shows "Energy: stored / capacity FE" and "Queue: n", the number of items in the input slots.

## Next

- Make <ItemLink id="autarkia:steel_ingot" /> for the steel tools, the <ItemLink id="autarkia:steel_plate" /> and the
  <ItemLink id="autarkia:steel_gear" />.
- Use <ItemLink id="autarkia:slag" /> for more foundation: 4 slag make 2 foundation.
- Read [Zinc, Brass and Steel](../materials.md) for the full ore chain.
