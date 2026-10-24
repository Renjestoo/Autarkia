---
navigation:
  title: Electric Chainsaw
  parent: t3/equipment.md
  position: 20
  icon: autarkia:electric_chainsaw
item_ids:
  - autarkia:electric_chainsaw
---

# Electric Chainsaw

<ItemImage id="autarkia:electric_chainsaw" scale="2" />

The electric chainsaw is an axe that fells a whole tree with one cut. It uses FE instead of durability, so it never breaks.

## Get it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 3 <ItemLink id="autarkia:steel_plate" />, 2 <ItemLink id="autarkia:steel_gear" />, 1 <ItemLink id="autarkia:servo" />, 2 <ItemLink id="autarkia:basic_circuit" />, 1 <ItemLink id="autarkia:capacitor_bank" /> | <ItemLink id="autarkia:fabricator" /> | 200 ticks | 100 FE/t | 1 electric chainsaw (empty) |

## Use it

1. Charge the chainsaw in a <ItemLink id="autarkia:battery_bank" /> or a <ItemLink id="autarkia:capacitor_bank" />.
2. Hold it in your main hand.
3. Cut one log of a tree. All logs that touch it, also at the corners, fall one after the other.
4. To cut one log only, sneak while you cut.

The felling stops when the chainsaw has less than 150 FE.

## Numbers

| Property | Value |
|---|---|
| Capacity | 400,000 FE |
| Charge rate | up to 2,000 FE/t |
| Cost | 150 FE per log |
| Logs per cut | up to 256 (config tools.maxFellLogs) |
| Felling speed | 16 logs per tick |
| Mining speed | 12 (axe blocks) |
| Attack damage | 9 |
| Durability | none (unbreakable) |

## Problems

- Only one log falls: you were sneaking, or the chainsaw is empty.
- The chainsaw mines slowly and the log drops nothing: the chainsaw has 0 FE. Charge it.

## Next

- Fell 64 or more logs in one cut for the advancement "Clean Cut".
