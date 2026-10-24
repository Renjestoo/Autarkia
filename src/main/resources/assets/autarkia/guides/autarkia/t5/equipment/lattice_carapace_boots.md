---
navigation:
  title: Lattice Carapace Boots
  parent: t5/equipment.md
  position: 70
  icon: autarkia:lattice_carapace_boots
item_ids:
  - autarkia:lattice_carapace_boots
---

# Lattice Carapace Boots

<ItemImage id="autarkia:lattice_carapace_boots" scale="2" />

The Lattice Carapace Boots are the boots of the Tier 5 powered armor. They keep the jet and the fall protection of the
Frontier Suit boots, and they repair themselves with FE.

## Get it

The <ItemLink id="autarkia:fabricator" /> makes them from the <ItemLink id="autarkia:frontier_suit_boots" />.
Fabricator recipes do not show in this book, so the table gives the recipe.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:frontier_suit_boots" />, 4 <ItemLink id="autarkia:lattice_alloy_ingot" />, 2 <ItemLink id="autarkia:nanite_paste" />, 1 <ItemLink id="autarkia:lattice_crystal" />, 1 <ItemLink id="autarkia:coolant_cell" /> | <ItemLink id="autarkia:fabricator" /> | 600 ticks (30 s) | 1,334 FE/t | 1 Lattice Carapace Boots |

The new boots keep the FE of the Frontier Suit boots.

## Use it

1. Charge the boots. Put them into the item slot of a <ItemLink id="autarkia:battery_bank" /> (2,048 FE/t) or a
   <ItemLink id="autarkia:capacitor_bank" /> (512 FE/t).
2. Wear them.
3. Press the suit key (default N) to switch the suit on or off.
4. Jump, then hold the jump key in the air to fire the jet.

The functions work while the suit is on and the boots have FE:

- Jet: while you hold jump in the air, your upward speed grows by 0.10 each tick, up to 0.5 blocks per tick. The jet
  costs 80 FE/t. The jet also gives better air control and resets your fall.
- Fall protection: the boots cancel fall damage for 50 FE per block above 3 blocks. With too little FE they cancel only
  part of the fall.
- Self-repair: while the boots are worn and damaged, they mend 1 durability every 20 ticks for 2,000 FE.

## Numbers

| Property | Value |
|---|---|
| Armor | 4 |
| Armor toughness | 4 |
| Knockback resistance | 0.15 |
| Durability | 650 |
| Energy capacity | 5,000,000 FE |
| Charge rate | Up to 20,000 FE/t |
| Jet | +0.10 per tick up to 0.5 blocks per tick, 80 FE/t |
| Fall protection | 50 FE per block above 3 blocks |
| Self-repair | 1 durability per 20 ticks, 2,000 FE each |
| Anvil repair | <ItemLink id="autarkia:lattice_alloy_ingot" /> |

## Problems

- The jet does not fire: the suit is off, the boots have less than 80 FE, or you are on the ground, in water, flying
  with an elytra or riding.

## Next

- In Tier 6 the <ItemLink id="autarkia:forge_of_ascension" /> turns the worn boots into the
  <ItemLink id="autarkia:aegis_boots" />.
