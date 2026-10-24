---
navigation:
  title: Quern
  parent: t1/processing.md
  position: 20
  icon: autarkia:quern
item_ids:
  - autarkia:quern
---

# Quern

<ItemImage id="autarkia:quern" scale="2" />

The quern is a stone hand mill. It grinds raw ore into crushed ore. Crushed ore gives more ingots than raw ore. The
quern needs no FE: you turn it with your hand.

## Get it

<RecipeFor id="autarkia:quern" />

## Use it

1. Place the quern. Put a chest or a crate below it to catch the output.
2. Hold raw ore and use it on the quern. The ore goes in.
3. Hold use on the quern with an empty hand. The stone turns.
4. After 8 turns, the quern makes crushed ore.
5. The crushed ore goes into the inventory below. With no inventory below, it pops out on top.

A Porter can also turn the quern.

## Recipes

| Input | Machine | Time | Outputs |
|---|---|---|---|
| 1 <ItemLink id="autarkia:raw_tin" /> | Quern | 8 turns | 1 <ItemLink id="autarkia:crushed_tin" />, 1 more at 50% |
| 1 <ItemLink id="minecraft:raw_copper" /> | Quern | 8 turns | 1 <ItemLink id="autarkia:crushed_copper" />, 1 more at 50% |
| 1 <ItemLink id="minecraft:raw_iron" /> | Quern | 8 turns | 1 <ItemLink id="autarkia:crushed_iron" />, 1 more at 50% |
| 1 <ItemLink id="minecraft:raw_gold" /> | Quern | 8 turns | 1 <ItemLink id="autarkia:crushed_gold" />, 1 more at 50% |

## Numbers

| Property | Value |
|---|---|
| One turn | 10 ticks |
| Turns for one ore | 8 (80 ticks, 4 s) |
| Energy | None |
| Mining tool | Pickaxe |

## Problems

- "Idle": nobody turns the quern, or it has no ore. Hold use with an empty hand.
- "Output full": the inventory below is full, so the output stays in the quern. Empty the inventory below.

## Next

- Smelt the crushed ore in a furnace.
- Build a <ItemLink id="autarkia:stamp_mill" />. It grinds the same ores with rope power.
