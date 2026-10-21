---
navigation:
  title: Lattice Equipment
  parent: t5/index.md
  position: 6
  icon: autarkia:nanite_multitool
item_ids:
  - autarkia:nanite_multitool
  - autarkia:lattice_blade
  - autarkia:lattice_carapace_helmet
  - autarkia:lattice_carapace_chest
  - autarkia:lattice_carapace_legs
  - autarkia:lattice_carapace_boots
  - autarkia:disassembler_beam
---

# Lattice Equipment

All tier 5 equipment uses FE and charges at up to 20,000 FE/t. The fabricator makes it from the tier 4 item and
lattice materials. The new item keeps the FE of the old one.

## Matter link

Sneak and use the <ItemLink id="autarkia:nanite_multitool" /> or the <ItemLink id="autarkia:disassembler_beam" /> on
a Matter Buffer to link it. A linked tool turns drops into matter in its cache (at most 4,096 MU, 20 FE per item).
Within 32 blocks of the linked buffer, the cache flows to it at 64 MU per second as a visible nanite stream. When the
cache is full, or an item has no matter value, the drops fall normally.

## Tools and weapons

- <ItemLink id="autarkia:nanite_multitool" />: pickaxe, axe, shovel, hoe, shears and sword in one, netherite level,
  speed 24, 10,000,000 FE. 100 FE per block. The mode key (V) switches 1x1 and 3x3 (3x3 at half speed). Sneak and use
  on any block other than a Matter Buffer to till it.
- <ItemLink id="autarkia:lattice_blade" />: attack 12, 5,000,000 FE, 200 FE per hit. Its hits halve the armor of the
  target. At 0 FE it hits normally.
- <ItemLink id="autarkia:disassembler_beam" />: 20,000,000 FE. Hold use for a beam of up to 100 ticks at 2,000 FE/t,
  range 24. It takes apart 2 blocks per tick (blast resistance up to 50) and does 6 damage to the first creature it
  hits, at most once per 10 ticks per target. It never touches block entities, Bastion blocks or blocks in another
  colony's domain. When the beam overheats, it cools down for 60 ticks. Without a link the drops fall as items.

## Lattice Carapace

Each piece holds 5,000,000 FE and keeps every Exo Rig and Frontier Suit function. Defense 4, 9, 7 and 4, toughness 4,
knockback resistance 0.15 per piece.

<ItemLink id="autarkia:lattice_carapace_helmet" />, <ItemLink id="autarkia:lattice_carapace_chest" />,
<ItemLink id="autarkia:lattice_carapace_legs" /> and <ItemLink id="autarkia:lattice_carapace_boots" /> repair
themselves: 1 point every 20 ticks for 2,000 FE per point.

You need the Lattice Carapace for the Forge of Ascension at tier 6.
