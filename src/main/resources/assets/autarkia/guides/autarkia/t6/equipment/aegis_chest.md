---
navigation:
  title: Aegis Chestplate
  parent: t6/equipment.md
  position: 50
  icon: autarkia:aegis_chest
item_ids:
  - autarkia:aegis_chest
---

# Aegis Chestplate

<ItemImage id="autarkia:aegis_chest" scale="2" />

The Aegis Chestplate is the chest piece of the Aegis armor. It is unbreakable and holds 10,000,000 FE. It keeps every
function of the Exo Rig, Frontier Suit and Lattice Carapace chestplates. With all 4 Aegis pieces on, your domain grid
pays your damage and a lethal hit cannot kill you.

## Get it

You cannot craft it. The <ItemLink id="autarkia:forge_of_ascension" /> fits it on your body:

| Inputs | Worn piece | Machine | Time | FE | Output |
|---|---|---|---|---|---|
| 8 <ItemLink id="autarkia:aegis_plate" />, 1 <ItemLink id="autarkia:ai_core" />, 8 <ItemLink id="autarkia:nanite_paste" /> | <ItemLink id="autarkia:lattice_carapace_chest" /> | <ItemLink id="autarkia:forge_of_ascension" /> | ticks 280 to 420 of the fitting | 12,500,000 | 1 Aegis Chestplate |

## Use it

1. Wear it. The functions work while the suit is on and the piece has FE.
2. Press the suit key (N by default) to turn all your suit pieces on or off.
3. Stay within 64 blocks of a tether point. Then the functions cost no FE and the piece charges by itself.

A tether point is a Colony Core, an outpost core, a domain beacon or a phase node on a grid with an ONLINE Singularity
Core.

## Functions

- +0.25 jump strength. Without the tether a jump costs 100 FE.
- +0.3 knockback resistance and +0.5 explosion knockback resistance. Without the tether this costs 2 FE/t.

## Charging

- While you are tethered, the grid charges every Aegis item in your inventory at up to 50,000 FE/t.
- An external charger puts in at most 50,000 FE/t.

## The full set: Aegis rule

Wear all 4 pieces: <ItemLink id="autarkia:aegis_helmet" />, Aegis Chestplate, <ItemLink id="autarkia:aegis_legs" /> and
<ItemLink id="autarkia:aegis_boots" />. While you are tethered, the grid pays your damage.

- Each damage point (half a heart, before armor) costs 50,000 FE. A hit of 10 points costs 500,000 FE.
- The grid pays all or nothing: the Singularity Core buffers first, then the other storage of the grid.
- When the grid pays, you take no damage and no knockback. A gold ring glows at your chest.
- When the grid cannot pay, the hit goes through your armor as normal.
- Another hit of the same damage type within 10 ticks costs only the part above the larger hit. A smaller hit is free.
- Not covered: the /kill command, the void below the world and damage that ignores invulnerability.

When the grid has paid for 1,000 points of damage in total, you get the advancement "Paid in Full".

## The full set: Nanite Reform

With all 4 pieces on, a lethal hit does not kill you. Your body turns into a nanite cloud and forms again 3 blocks away
from the source of the damage.

- Cost: 2,000 free MU from the Matter Buffers of the domain where you stand, else of your tether colony. No FE.
- Cooldown: 30 seconds.
- You come back with half health, no fire and no harmful effects.
- You get 40 ticks of damage immunity and 20 ticks of invisibility.
- With the cooldown running, or with less than 2,000 free MU, you die as normal.
- The first Reform gives the advancement "Not Today".

## HUD

With the full set on, the equipment HUD shows two lines right of the hotbar:

- "Tether" with a link icon. Green: tethered and the grid holds 1,000,000 FE or more. Amber: tethered with less. Red:
  no tether.
- "Reform ready", or "Reform N s" for the cooldown. Grey: the domain holds less than 2,000 free MU.

## Numbers

| Property | Value |
|---|---|
| Armor | 10 |
| Toughness | 5 |
| Knockback resistance | 0.25 |
| FE capacity | 10,000,000 FE |
| Charging | up to 50,000 FE/t |
| Durability | unbreakable |
| Damage cost (full set) | 50,000 FE per point |
| Reform cost (full set) | 2,000 MU, cooldown 30 s |

## Problems

- "No suit pieces worn": you pressed the suit key without suit armor on.
- "Suit off": the functions stop. Press the suit key again.
- The HUD shows red: no tether point is within 64 blocks, or no Singularity Core is ONLINE.
- You still take damage: you do not wear all 4 Aegis pieces, you are not tethered, or the grid has too little FE.

## Next

- Use the <ItemLink id="autarkia:singularity_lance" /> and the <ItemLink id="autarkia:aegis_blade" /> with the armor.
