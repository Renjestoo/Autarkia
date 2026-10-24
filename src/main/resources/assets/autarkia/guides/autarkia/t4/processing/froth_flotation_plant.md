---
navigation:
  title: Froth Flotation Plant
  parent: t4/processing.md
  position: 10
  icon: autarkia:froth_flotation_plant
item_ids:
  - autarkia:froth_flotation_plant
---

# Froth Flotation Plant

<ItemImage id="autarkia:froth_flotation_plant" scale="2" />

The froth flotation plant is a heavy machine, 3 x 3 x 3 blocks. It turns 1 crushed ore and water into 2 concentrate.
After a jaw crusher, this gives 4 ingots for each raw ore.

## Get it

<RecipeFor id="autarkia:froth_flotation_plant" />

## Use it

1. Put 9 <ItemLink id="autarkia:foundation" /> blocks in a 3 x 3 square. A heavy machine needs foundation under each
   bottom block.
2. Place the plant on the foundation.
3. Connect it to a power network. For example, let one of its bottom blocks touch the base of an
   <ItemLink id="autarkia:hv_pylon" />.
4. Give it water. Put a water source next to one of its bottom blocks, or put water buckets into the bucket slot, or
   pump water in with a fluid pipe.
5. Use the plant to open its menu.
6. Put crushed ore into the input slot. Each run uses 1 crushed ore and 250 mB of water.
7. Take the concentrate, slag and nuggets from the three output slots.

### Menu

| Part | What it does |
|---|---|
| Input slot (middle left) | Crushed ore of iron, copper, gold, tin, zinc or vantite |
| Bucket slot (lower left) | Takes water buckets only. It empties a bucket into the tank when 1,000 mB of room is free. The empty bucket stays in the slot. |
| Output slots (3, right of the arrow) | Concentrate, slag and the nugget of a second metal |
| Energy bar (left) | Stored FE. Point at it to see the amount. |
| Arrow | Progress of the current run |
| Tank gauge (right) | Water in the tank, with a mark for each 1,000 mB. Point at it to see "Water: amount / 8,000 mB". |
| Status text (top) | The machine state, see Problems |

Pipes and hoppers can put crushed ore and water buckets in. They can take out the outputs and the empty buckets. The
tank takes water from fluid pipes on every face.

### Recipes

| Input | Time | Outputs |
|---|---|---|
| 1 <ItemLink id="autarkia:crushed_iron" /> and 250 mB water | 200 ticks | 2 <ItemLink id="autarkia:iron_concentrate" />, slag (50%), zinc nugget (35%) |
| 1 <ItemLink id="autarkia:crushed_copper" /> and 250 mB water | 200 ticks | 2 <ItemLink id="autarkia:copper_concentrate" />, slag (50%), gold nugget (35%) |
| 1 <ItemLink id="autarkia:crushed_gold" /> and 250 mB water | 200 ticks | 2 <ItemLink id="autarkia:gold_concentrate" />, slag (50%), copper nugget (35%) |
| 1 <ItemLink id="autarkia:crushed_tin" /> and 250 mB water | 200 ticks | 2 <ItemLink id="autarkia:tin_concentrate" />, slag (50%), iron nugget (35%) |
| 1 <ItemLink id="autarkia:crushed_zinc" /> and 250 mB water | 200 ticks | 2 <ItemLink id="autarkia:zinc_concentrate" />, slag (50%), copper nugget (35%) |
| 1 <ItemLink id="autarkia:crushed_vantite" /> and 250 mB water | 200 ticks | 2 <ItemLink id="autarkia:vantite_concentrate" />, slag (50%), gold nugget (35%) |

The plant uses the water at the end of each run. A water source next to it pumps 50 mB per tick into the tank and
stays in place. The server option `flotation.pumpFromSource` (default on) controls the pump.

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 3 x 3 blocks, on 9 foundation |
| Power use | 768 FE/t while it works |
| Time | 200 ticks (10 s) for each run |
| Energy for each run | 153,600 FE |
| Energy buffer | 153,600 FE |
| Maximum input | 3,072 FE/t |
| Water tank | 8,000 mB, 250 mB for each run |
| Pump | 50 mB/t from an adjacent water source |
| Water bucket | 1,000 mB |
| Mine with | Iron pickaxe or better |

## Problems

- "Condition missing" with "Needs a foundation under every bottom block": a bottom block has no foundation under
  it. Put foundation under all 9 bottom blocks.
- "Condition missing" without the foundation line: the tank has less than 250 mB of water. Add water.
- "No power": the buffer holds less than one tick of work. Give it more power.
- "Idle": the input slot is empty, or it holds an item without a flotation recipe.
- "Output full": there is no room for every possible result. Take out the outputs.

Jade shows the state, "Energy: stored / capacity FE", "Queue: n" (items in the input slot) and "Water: amount /
8,000 mB".

## Next

- Smelt the concentrate. Read [Vantite and Concentrates](../materials.md).
- Use <ItemLink id="autarkia:slag" /> for more foundation.
