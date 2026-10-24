---
navigation:
  title: Electric Drill
  parent: t3/equipment.md
  position: 10
  icon: autarkia:electric_drill
item_ids:
  - autarkia:electric_drill
---

# Electric Drill

<ItemImage id="autarkia:electric_drill" scale="2" />

The electric drill is a pickaxe and a shovel in one tool. It mines at diamond level and very fast. It uses FE instead of durability, so it never breaks.

## Get it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 3 <ItemLink id="autarkia:steel_plate" />, 1 <ItemLink id="autarkia:steel_gear" />, 1 <ItemLink id="autarkia:servo" />, 2 <ItemLink id="autarkia:basic_circuit" />, 1 <ItemLink id="autarkia:capacitor_bank" />, 1 diamond | <ItemLink id="autarkia:fabricator" /> | 200 ticks | 100 FE/t | 1 electric drill (empty) |

## Use it

1. Charge the drill: put it into the slot of a <ItemLink id="autarkia:battery_bank" /> or a <ItemLink id="autarkia:capacitor_bank" />.
2. Mine stone, ore, dirt, sand and gravel like with a pickaxe and a shovel.
3. Watch the blue bar of the item. It shows the charge.
4. Charge the drill again when the bar is low.

## Numbers

| Property | Value |
|---|---|
| Capacity | 400,000 FE |
| Charge rate | up to 2,000 FE/t |
| Cost | 200 FE per block |
| Blocks per full charge | 2,000 |
| Mining speed | 12 (pickaxe and shovel blocks) |
| Mining level | diamond |
| Attack damage | 5 |
| Durability | none (unbreakable) |

## Problems

- The drill mines very slowly and the block drops nothing: the drill is empty. With 0 FE it mines at speed 1 and gives no drops.
- A block drops nothing with a full drill: the block needs a tool above diamond level.

## Next

- <ItemLink id="autarkia:vantite_excavator" /> is the Tier 4 drill.
- The drill mines <ItemLink id="autarkia:vantite_ore" /> for Tier 4.
