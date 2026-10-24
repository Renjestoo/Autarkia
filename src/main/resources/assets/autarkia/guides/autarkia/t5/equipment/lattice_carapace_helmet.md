---
navigation:
  title: Lattice Carapace Helmet
  parent: t5/equipment.md
  position: 40
  icon: autarkia:lattice_carapace_helmet
item_ids:
  - autarkia:lattice_carapace_helmet
---

# Lattice Carapace Helmet

<ItemImage id="autarkia:lattice_carapace_helmet" scale="2" />

The Lattice Carapace Helmet is the helmet of the Tier 5 powered armor. It keeps every function of the Exo Rig and
Frontier Suit helmets, and it repairs itself with FE.

## Get it

The <ItemLink id="autarkia:fabricator" /> makes it from the <ItemLink id="autarkia:frontier_suit_helmet" />. Fabricator
recipes do not show in this book, so the table gives the recipe.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:frontier_suit_helmet" />, 5 <ItemLink id="autarkia:lattice_alloy_ingot" />, 2 <ItemLink id="autarkia:nanite_paste" />, 1 <ItemLink id="autarkia:lattice_crystal" />, 1 <ItemLink id="autarkia:coolant_cell" /> | <ItemLink id="autarkia:fabricator" /> | 600 ticks (30 s) | 1,334 FE/t | 1 Lattice Carapace Helmet |

The new helmet keeps the FE of the Frontier Suit helmet.

## Use it

1. Charge the helmet. Put it into the item slot of a <ItemLink id="autarkia:battery_bank" /> (2,048 FE/t) or a
   <ItemLink id="autarkia:capacitor_bank" /> (512 FE/t).
2. Wear it.
3. Press the suit key (default N) to switch all worn suit pieces on or off. The tooltip shows "Suit: on" or "Suit: off".

The functions work while the suit is on and the helmet has FE:

- Night vision, for 10 FE/t.
- +1 oxygen bonus.
- FE balance: every 20 ticks the helmet shares the FE of all worn suit pieces in proportion to their capacity.
- Self-repair: while the helmet is worn and damaged, it mends 1 durability every 20 ticks for 2,000 FE.

## Numbers

| Property | Value |
|---|---|
| Armor | 4 |
| Armor toughness | 4 |
| Knockback resistance | 0.15 |
| Durability | 550 |
| Energy capacity | 5,000,000 FE |
| Charge rate | Up to 20,000 FE/t |
| Night vision | 10 FE/t |
| Self-repair | 1 durability per 20 ticks, 2,000 FE each |
| Anvil repair | <ItemLink id="autarkia:lattice_alloy_ingot" /> |

## Problems

- No night vision: the suit is off, or the helmet has less than 10 FE. Press the suit key or charge the helmet.

## Next

- Wear all four pieces: <ItemLink id="autarkia:lattice_carapace_chest" />, <ItemLink id="autarkia:lattice_carapace_legs" />
  and <ItemLink id="autarkia:lattice_carapace_boots" />.
- In Tier 6 the <ItemLink id="autarkia:forge_of_ascension" /> turns the worn helmet into the
  <ItemLink id="autarkia:aegis_helmet" />.
