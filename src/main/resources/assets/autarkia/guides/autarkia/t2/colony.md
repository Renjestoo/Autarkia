---
navigation:
  title: The T2 Colony
  parent: t2/index.md
  position: 4
  icon: autarkia:core_upgrade_t2
---

# The T2 Colony

The <ItemLink id="autarkia:core_upgrade_t2" /> raises your Colony Core to Tier 2. The domain (the colony area) grows,
the colony can have more workers and more projects at the same time, and it learns new projects.

## Tier 2 colony limits

| Property | Tier 1 | Tier 2 |
|---|---|---|
| Domain | 33 x 33 blocks | 49 x 49 blocks |
| Worker cap | 4 | 8 |
| Porters at most | 4 | 6 |
| Hauler Crabs at most | 0 | 4 |
| Active projects at the same time | 1 | 2 |
| Daily build budget | 64 points | 160 points |
| Pattern library | 0 | 4 Patterns |
| Largest Pattern box | none | 16 x 16 x 16 |

## New projects

The colony reads its sensors every 5 seconds. When a need stays, it proposes a project:

| Need | Project | Most built |
|---|---|---|
| Power 20% short for 60 s, and flowing water in the domain | Waterwheel | 6 |
| Power 30% short for 60 s, and fuel worth 64 coal in storage | Boiler and Engine | 4 |
| Crates 85% full for 60 s | Wood Racks | 12 |
| A span at 80% load for 60 s, a span at 80% heat for 30 s, or a snapped span | Span Upgrade | No limit |
| 2 lightning strikes in one week, or a lightning surge | Lightning Mast | 2 |
| A lightning surge destroyed a machine | Lightning Arresters | No limit |
| A boiler reached 10 bar today, or a boiler burst | Pressure Valve | No limit |

The Span Upgrade turns a rope line into a copper line: each rope pulley on the line becomes a
<ItemLink id="autarkia:wooden_pole" />, and each rope span becomes a copper span. In Tier 2 the colony stops proposing
Parallel Spans.

## Who builds Tier 2 projects

Porters build only Tier 1 projects. The Hauler Crab carries items and collects debris, but it places no blocks. So in
Tier 2 you build the Tier 2 projects yourself:

1. Approve the project.
2. Look at its ghost with the Show button or a <ItemLink id="autarkia:planning_tablet" />.
3. Clear and fill the ground where the ghost shows it.
4. Place each block and string each span that the ghost shows.
5. The colony sees each finished part and marks it done.

A finished Waterwheel project must make power within 200 ticks, or the project is blocked. From Tier 3,
the Builder Drone builds projects of every tier.

<SubPages />
