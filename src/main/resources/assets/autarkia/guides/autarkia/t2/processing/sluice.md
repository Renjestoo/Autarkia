---
navigation:
  title: Sluice
  parent: t2/processing.md
  position: 30
  icon: autarkia:sluice
item_ids:
  - autarkia:sluice
---

# Sluice

<ItemImage id="autarkia:sluice" scale="2" />

The sluice is a long wooden trough that uses water, not power. It washes crushed ore into washed ore. Washed ore gives
more ingots in the <ItemLink id="autarkia:blast_smelter" />, and the sluice sometimes finds a nugget of a second metal.

## Get it

<RecipeFor id="autarkia:sluice" />

Make the brass plates in the <ItemLink id="autarkia:plate_press" />.

## Use it

1. Place the sluice. It is 3 blocks long and points away from you. Its middle block goes where you place it.
2. Find the two ends. The far end is the head. The end near you is the tail.
3. Put water in the block behind the head, or in the block on top of the head. Flowing water gives full speed. A
   water source block only gives half speed.
4. Put a chest, a belt or another inventory in the block in front of the tail. Without one, the sluice drops its
   outputs on the ground there.
5. Use the sluice to open its menu. Put crushed ore in the input slot. A hopper or a belt can also fill it.
6. The washed ore and the nuggets go out of the tail.

The menu and Jade show the water state at the head.

### Recipes

Each washing takes 100 ticks (5 s) with flowing water.

| Input | Output | Extra output (10% chance) |
|---|---|---|
| 1 <ItemLink id="autarkia:crushed_iron" /> | 1 <ItemLink id="autarkia:washed_iron" /> | 1 <ItemLink id="autarkia:zinc_nugget" /> |
| 1 <ItemLink id="autarkia:crushed_copper" /> | 1 <ItemLink id="autarkia:washed_copper" /> | 1 <ItemLink id="minecraft:gold_nugget" /> |
| 1 <ItemLink id="autarkia:crushed_gold" /> | 1 <ItemLink id="autarkia:washed_gold" /> | 1 <ItemLink id="minecraft:copper_nugget" /> |
| 1 <ItemLink id="autarkia:crushed_tin" /> | 1 <ItemLink id="autarkia:washed_tin" /> | 1 <ItemLink id="minecraft:iron_nugget" /> |
| 1 <ItemLink id="autarkia:crushed_zinc" /> | 1 <ItemLink id="autarkia:washed_zinc" /> | 1 <ItemLink id="minecraft:copper_nugget" /> |

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 1 x 3 blocks |
| Slots | 1 input, 2 outputs |
| Power use | None |
| Time per washing | 100 ticks (5 s) with flowing water |
| Speed with a water source only | 50% (200 ticks per washing) |
| Mine with | Axe |

## Problems

- "Condition missing" with "Water: none": no water is behind or on top of the head. Add water there.
- "Water: source 50%": the water is a still source block. The sluice works at half speed. Let the water flow for full
  speed ("Water: flowing 100%").
- "Idle": the input slot has no crushed ore. Put in crushed ore.
- "Output full": the outputs cannot leave. Empty the inventory in front of the tail, or clear the belt.

Jade also shows "Queue: n", the number of items in the input slot.

## Next

- Smelt 3 washed ore in the <ItemLink id="autarkia:blast_smelter" /> for 4 ingots.
- Read [Zinc, Brass and Steel](../materials.md) for the full ore chain.
