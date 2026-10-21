---
navigation:
  title: The Colony
  parent: t1/index.md
  position: 5
  icon: autarkia:core_seed
item_ids:
  - autarkia:core_seed
  - autarkia:project_board
  - autarkia:planning_tablet
---

# The Colony

## Planting the Core

Use a <ItemLink id="autarkia:core_seed" /> on the top of dirt or stone. The 3 x 3 ground below must be solid, and the
3 x 3 x 3 space above must be free. Roots grow for 10 seconds, then the Core blooms. It claims a domain of 33 x 33
blocks and gives you a Porter capsule and this manual.

<RecipeFor id="autarkia:core_seed" />

## What the colony sees

Every 5 seconds the Core reads the machines, generators, spans, crates and workers in its domain. It knows the power
supply and demand, the stored energy, the span load, the storage fill and the worker charge.

## Projects

When a need stays for a while, the colony proposes a project:

| Need | Project |
|---|---|
| Power 25% short | Windmill, or a treadwheel |
| Crates 85% full | Crate cluster |
| Too little stored power | Flywheel |
| A rope span at 90% load | A parallel span |
| Workers low on charge | Winding post |

A proposal shows its reason, site, cost and bill of materials. See the proposals at a
<ItemLink id="autarkia:project_board" />, or hold a <ItemLink id="autarkia:planning_tablet" /> to see them as ghost
blocks.

## Autonomy

| Level | What happens |
|---|---|
| Manual | Nothing is built until you approve. |
| Assisted | A proposal is approved after one day. |
| Autonomous | A proposal is approved at once. |

A project approved by the colony uses the daily budget. It starts only when the crates hold every item. When it is
done, a worker puts a sign next to it: the reason, "Fixed." and the day.

<RecipeFor id="autarkia:project_board" />

<RecipeFor id="autarkia:planning_tablet" />
