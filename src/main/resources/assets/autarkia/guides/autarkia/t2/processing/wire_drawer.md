---
navigation:
  title: Wire Drawer
  parent: t2/processing.md
  position: 20
  icon: autarkia:wire_drawer
item_ids:
  - autarkia:wire_drawer
---

# Wire Drawer

<ItemImage id="autarkia:wire_drawer" scale="2" />

The wire drawer is a powered machine. It pulls copper ingots into wire. It makes the
<ItemLink id="autarkia:copper_coil" /> for Tier 2 machines and the <ItemLink id="autarkia:copper_wire_spool" /> for
your power network.

## Get it

<RecipeFor id="autarkia:wire_drawer" />

Make the brass ingots in a <ItemLink id="autarkia:crucible" /> (3 copper ingots and 1 zinc ingot make 4 brass ingots).

## Use it

1. Place the wire drawer. It is one block.
2. Connect it to a power network. A machine next to the base of a <ItemLink id="autarkia:wooden_pole" /> joins the
   network of that pole. Read [Copper Wire](../power.md) for the network.
3. Use the wire drawer to open its menu. It has two input slots: one for the copper, one for the core.
4. For copper coils, put copper ingots in one input slot and iron ingots in the other input slot.
5. For a copper wire spool, put copper ingots in one input slot. Keep the other input slot empty.
6. Take the result from the output slot.

Hoppers and belts can put ingots into the input slots and take the result from the output slot.

### Recipes

| Inputs | Time | Output |
|---|---|---|
| 4 copper ingots and 1 iron ingot (the core) | 100 ticks (5 s) | 2 <ItemLink id="autarkia:copper_coil" /> |
| 2 copper ingots, no core | 100 ticks (5 s) | 1 <ItemLink id="autarkia:copper_wire_spool" /> |

## Numbers

| Property | Value |
|---|---|
| Size | 1 block |
| Slots | 2 inputs (material and core), 1 output |
| Power use | 40 FE/t while it works |
| Energy per operation | 4,000 FE |
| Energy buffer | 8,000 FE |
| Maximum input | 160 FE/t |
| Mine with | Pickaxe |

## Problems

- "Idle": the input slots do not hold a full recipe. A spool needs the second slot empty. Copper in both slots
  matches no recipe.
- "No power": the buffer has less than 40 FE. Connect the drawer to a network, or add generators.
- "Output full": the output slot is full. Take out the result, or let a hopper or belt take it.

Jade also shows "Energy: stored / capacity FE" and "Queue: n", the number of items in the input slots.

## Next

- Copper coils make the <ItemLink id="autarkia:blast_smelter" />, the <ItemLink id="autarkia:belt" />, the
  <ItemLink id="autarkia:steam_engine" />, the <ItemLink id="autarkia:waterwheel" /> and the
  <ItemLink id="autarkia:capacitor_bank" />.
- Copper wire spools connect <ItemLink id="autarkia:wooden_pole" /> nodes. Read [Copper Wire](../power.md).
