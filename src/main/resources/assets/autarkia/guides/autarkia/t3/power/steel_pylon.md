---
navigation:
  title: Steel Pylon
  parent: t3/power.md
  position: 20
  icon: autarkia:steel_pylon
item_ids:
  - autarkia:steel_pylon
---

# Steel Pylon

<ItemImage id="autarkia:steel_pylon" scale="2" />

The steel pylon is the tall node of the insulated grid. It holds up to 6 spans of <ItemLink id="autarkia:insulated_cable_spool" /> at its top. Machines next to it join its network.

## Get it

<RecipeFor id="autarkia:steel_pylon" />

## Use it

1. Find a spot with 5 free blocks of height.
2. Place the pylon. It is 1 x 5 x 1 blocks.
3. Use an insulated cable spool on the pylon, then on the next node.
4. Put a machine, a generator or a storage block next to the pylon. It joins the network of the pylon.

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 5 x 1 |
| Cable tier | insulated cable only |
| Spans | 6 |
| Longest span | 24 blocks |

## Problems

The pylon shows its state in Jade:

- "Tier: insulated cable": the pylon takes only this tier.
- "Spans: N / 6": the number of spans on the pylon.
- "Network: N% load": the highest load of its spans. Above 100% the cable heats up.
- "This node does not take copper wire": a pylon takes no copper wire. Join copper and insulated cable with a <ItemLink id="autarkia:transformer" />.
- "Node is full": the pylon has 6 spans. Place another pylon.

## Next

- <ItemLink id="autarkia:wall_insulator" /> is the small node for walls.
- <ItemLink id="autarkia:hv_pylon" /> is the Tier 4 pylon for HV lines.
