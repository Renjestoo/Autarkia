---
navigation:
  title: Fabricator
  parent: t3/processing.md
  position: 40
  icon: autarkia:fabricator
item_ids:
  - autarkia:fabricator
---

# Fabricator

<ItemImage id="autarkia:fabricator" scale="2" />

The fabricator is the electric workshop of Tier 3. You select one target recipe. The fabricator then takes the ingredients from its 3 x 3 buffer and makes the target again and again. It makes circuits, servos, the Core Upgrade T3 and all electric equipment.

## Get it

<RecipeFor id="autarkia:fabricator" />

## Use it

1. Place the fabricator. It is 2 blocks wide, 2 blocks high and 1 block deep.
2. Connect it to the grid: put a node next to it, or string a span to its bushing.
3. Open the screen.
4. Push the arrow buttons under the target square until the target shows the item you want.
5. Point at the target square to see the name of the target.
6. Put the ingredients into the 3 x 3 buffer.
7. Take the product from the output slot.
8. Push the **x** button ("No target") to clear the target.

The fabricator stays idle while an ingredient is missing. Only items that a fabricator recipe uses go into the buffer. Hoppers and chutes can fill the buffer and empty the output.

The colony can set the target itself when it needs parts for a project.

## Tier 3 recipes

| Product | Ingredients | Time | Energy |
|---|---|---|---|
| 2 <ItemLink id="autarkia:basic_circuit" /> | 1 brass plate, 2 redstone, 1 copper coil | 200 ticks | 160 FE/t |
| 1 <ItemLink id="autarkia:servo" /> | 2 steel gears, 1 copper coil, 1 steel plate, 1 basic circuit | 200 ticks | 160 FE/t |
| 1 <ItemLink id="autarkia:core_upgrade_t3" /> | 4 basic circuits, 2 servos, 4 steel plates, 1 battery bank | 400 ticks | 160 FE/t |

The fabricator also makes the <ItemLink id="autarkia:electric_drill" />, the <ItemLink id="autarkia:electric_chainsaw" />, the <ItemLink id="autarkia:shock_baton" />, the <ItemLink id="autarkia:arc_rifle" /> and the four exo rig parts. Their pages show the ingredients.

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 1 |
| Energy | 160 FE/t, or the recipe value |
| Default time | 200 ticks (10 s) |
| Buffer | 200 x the energy per tick, at least 32,000 FE |
| Max input | 4 x the energy per tick, at least 640 FE/t |
| Slots | 9 buffer slots, 1 output |

Some later recipes need more than 160 FE/t. The fabricator then works at the energy per tick of that recipe.

## Screen

- The target square at the top right shows the target item. Its tooltip shows "Target: item" or "No target recipe".
- The three buttons under the target square: previous recipe, next recipe and no target. Their tooltips show "Previous recipe", "Next recipe" and "No target".
- The bar at the left: the energy buffer.
- The status text: "Running", "Idle", "No power" or "Output full".

## Problems

- "Idle": there is no target, or an ingredient is missing. Compare the buffer with the recipe on the page of the product.
- "No power": the buffer has less energy than one tick of work.
- "Output full": take the product out.
- An item does not go into the buffer: no fabricator recipe uses it.

## Next

- <ItemLink id="autarkia:core_upgrade_t3" /> opens Tier 3.
- <ItemLink id="autarkia:core_upgrade_t4" /> is also made in the fabricator.
