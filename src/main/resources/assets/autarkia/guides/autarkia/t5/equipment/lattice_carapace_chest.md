---
navigation:
  title: Lattice Carapace Chestplate
  parent: t5/equipment.md
  position: 50
  icon: autarkia:lattice_carapace_chest
item_ids:
  - autarkia:lattice_carapace_chest
---

# Lattice Carapace Chestplate

<ItemImage id="autarkia:lattice_carapace_chest" scale="2" />

The Lattice Carapace Chestplate is the chest piece of the Tier 5 powered armor. It keeps the Exo Rig and Frontier Suit
chest functions, and it repairs itself with FE.

## Get it

The <ItemLink id="autarkia:fabricator" /> makes it from the <ItemLink id="autarkia:frontier_suit_chest" />. Fabricator
recipes do not show in this book, so the table gives the recipe.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:frontier_suit_chest" />, 8 <ItemLink id="autarkia:lattice_alloy_ingot" />, 4 <ItemLink id="autarkia:nanite_paste" />, 2 <ItemLink id="autarkia:lattice_crystal" />, 1 <ItemLink id="autarkia:coolant_cell" /> | <ItemLink id="autarkia:fabricator" /> | 800 ticks (40 s) | 1,500 FE/t | 1 Lattice Carapace Chestplate |

The new chestplate keeps the FE of the Frontier Suit chestplate.

## Use it

1. Charge the chestplate. Put it into the item slot of a <ItemLink id="autarkia:battery_bank" /> (2,048 FE/t) or a
   <ItemLink id="autarkia:capacitor_bank" /> (512 FE/t).
2. Wear it.
3. Press the suit key (default N) to switch the suit on or off.

The functions work while the suit is on and the chestplate has FE:

- +0.25 jump strength, for 100 FE per jump.
- +0.3 knockback resistance and +0.5 explosion knockback resistance, for 2 FE/t.
- Self-repair: while the chestplate is worn and damaged, it mends 1 durability every 20 ticks for 2,000 FE.

## Numbers

| Property | Value |
|---|---|
| Armor | 9 |
| Armor toughness | 4 |
| Knockback resistance | 0.15, plus 0.3 while the suit is on |
| Durability | 800 |
| Energy capacity | 5,000,000 FE |
| Charge rate | Up to 20,000 FE/t |
| Self-repair | 1 durability per 20 ticks, 2,000 FE each |
| Anvil repair | <ItemLink id="autarkia:lattice_alloy_ingot" /> |

## Problems

- No higher jump: the suit is off, or the chestplate has too little FE. Press the suit key or charge it.

## Next

- In Tier 6 the <ItemLink id="autarkia:forge_of_ascension" /> turns the worn chestplate into the
  <ItemLink id="autarkia:aegis_chest" />.
