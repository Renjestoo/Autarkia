---
navigation:
  title: Aegis and Ascension
  parent: t6/index.md
  position: 5
  icon: autarkia:aegis_chest
item_ids:
  - autarkia:aegis_multitool
  - autarkia:aegis_blade
  - autarkia:aegis_helmet
  - autarkia:aegis_chest
  - autarkia:aegis_legs
  - autarkia:aegis_boots
  - autarkia:singularity_lance
  - autarkia:forge_of_ascension
---

# Aegis and Ascension

Aegis items are unbreakable. They charge from the tether at up to 50,000 FE/t per item.

## Tools and weapons

- <ItemLink id="autarkia:aegis_multitool" />: all tools in one, speed 2,000 (instant up to hardness 66), 100,000,000
  FE, 50 FE per block. The mode key (V) cycles 1x1, 3x3 and 5x5 at full speed. The enchant key (B) cycles none, Silk
  Touch and Fortune III.
- <ItemLink id="autarkia:aegis_blade" />: attack 30, 100,000,000 FE, 500 FE per hit. Armor protects the target only a
  quarter as well. Use: Aegis sweep, 20 damage to hostile mobs within 6 blocks, 50,000 FE, cooldown 100 ticks.
- <ItemLink id="autarkia:singularity_lance" />: 50,000,000 FE. Hold use for 60 ticks: a beam of 128 blocks for 20
  ticks, 80 damage to each creature in its path (radius 1.5) or at the impact (radius 4), 1,000,000 FE per shot,
  cooldown 200 ticks. The mode key switches terrain-safe (default) and terrain-destroy. Inside any domain it is always
  terrain-safe. It never hits you or your allies.

## Aegis armor

<ItemLink id="autarkia:aegis_helmet" />, <ItemLink id="autarkia:aegis_chest" />, <ItemLink id="autarkia:aegis_legs" />
and <ItemLink id="autarkia:aegis_boots" /> hold 10,000,000 FE each and keep every Exo Rig and Frontier Suit function.
Defense 5, 10, 8 and 5, toughness 5, knockback resistance 0.25 per piece. While tethered, the suit functions cost no
FE.

You cannot craft Aegis armor. The Forge of Ascension fits it.

## Forge of Ascension

The <ItemLink id="autarkia:forge_of_ascension" /> is 3 x 4 x 3 with 27 input slots.

1. Put the inputs into the forge. The network of the forge must store 50,000,000 FE.
2. Wear the Lattice Carapace pieces. Stand on the center pad, then sneak and use the pad with an empty hand.
3. The arms fit each piece that has its inputs, in 600 ticks for the full set. Each piece costs 12,500,000 FE.

| Piece | Inputs | Fitted at tick |
|---|---|---|
| Boots | 4 aegis plates, 1 AI core, 4 nanite paste | 150 |
| Legs | 7 aegis plates, 1 AI core, 6 nanite paste | 270 |
| Chest | 8 aegis plates, 1 AI core, 8 nanite paste | 410 |
| Helmet | 5 aegis plates, 1 AI core, 4 nanite paste | 510 |

You cannot move during the fit, and you take no damage. Sparks fly, arcs jump from the arms and the camera shakes. At
tick 600 the forge seals the set with a glow burst. If you leave the pad, log out, or the grid cannot pay, the arms
retract. Fitted pieces stay, and a removed carapace piece without its new piece goes back to its slot.

## Aegis rule

With all 4 Aegis pieces on, the grid takes your damage.

- Tether: a Colony Core, outpost core, domain beacon or phase node within 64 blocks, in the same dimension, on a grid
  with an ONLINE Singularity Core.
- Each point of damage (half a heart, before armor) costs 50,000 FE. Example: a zombie hit (3) costs 150,000 FE, a
  creeper (49) 2,450,000 FE.
- The grid pays all or nothing: the Singularity Core buffer first, then the other domain storage. If it pays, you take
  no damage and no knockback. If not, the damage goes through your armor.
- Another hit of the same type within 10 ticks costs only the difference to the larger hit.
- Not covered: /kill, the void below the world, and damage that bypasses invulnerability.

The HUD shows the tether: green when linked with 1,000,000 FE or more on the grid, amber when linked with less, red
without a tether.

## Nanite Reform

With all 4 Aegis pieces on, a lethal hit does not kill you. Your body turns into a nanite cloud and forms again 3
blocks away from the source of the damage.

- Cost: 2,000 matter units from the Matter Buffers of the domain, fullest first. No FE.
- Cooldown: 30 seconds.
- You come back with half health, no fire and no harmful effects, 40 ticks of damage immunity and 20 ticks of
  invisibility.
- With the cooldown running or less than 2,000 matter units, you die normally.

The HUD shows "Reform ready" or the cooldown seconds. It is grey when the domain has less than 2,000 matter units.

<RecipeFor id="autarkia:forge_of_ascension" />
