---
navigation:
  title: A Colony That Learns
  parent: t3/index.md
  position: 5
  icon: autarkia:core_upgrade_t3
item_ids:
  - autarkia:core_upgrade_t3
  - autarkia:domain_beacon
  - autarkia:prop_beam
  - autarkia:survey_lens
---

# A Colony That Learns

## Failure memory

The colony remembers each failure: snapped cables, fuse trips, rack collapses, boiler bursts and destroyed machines.
A memory loses half its weight in 7 days. While it is strong, the colony prefers the project that prevents the same
failure. Example: a cable snapped last week, so today a Lineman puts a fuse box on that line.

## Repair and copies

A watched Pattern is a build that the colony protects. When a creeper or a stranger breaks it, Builder Drones put
the same blocks back, bottom row first. Blocks that you or a trusted player break stay removed.

When the machines of a LINE Pattern have a long queue, the colony builds a second line from the Pattern. It moves part
of the feed belt to the new line with a belt splitter.

## Domain beacon

A <ItemLink id="autarkia:domain_beacon" /> adds a square of half-size 16 around itself. It needs 32 FE/t. A T3 colony
can have 2 beacons.

## Survey lens

Hold a <ItemLink id="autarkia:survey_lens" />. It shows outlines of the ore deposits within 64 blocks, through the
ground.

<RecipeFor id="autarkia:domain_beacon" />
