---
navigation:
  title: HV Pylon
  parent: t4/power.md
  position: 10
  icon: autarkia:hv_pylon
item_ids:
  - autarkia:hv_pylon
---

# HV Pylon

<ItemImage id="autarkia:hv_pylon" scale="2" />

The HV pylon is the node of the HV line, 1 x 7 x 1 blocks. HV spans hang from its top arms. It carries power across
the land and feeds the machines next to its base.

## Get it

<RecipeFor id="autarkia:hv_pylon" />

## Use it

1. Place the pylon on solid ground. It needs 7 free blocks of height.
2. Place a second pylon up to 48 blocks away. Keep the straight line between the two tops free of blocks.
3. Hold an <ItemLink id="autarkia:hv_cable_spool" />. Use it on the first pylon, then on the second pylon. The span
   hangs from the top of each pylon.
4. Place machines and generators so that a block of each touches the base block of a pylon. They join the network of
   that pylon.
5. To join a line of another tier, put a <ItemLink id="autarkia:transformer" /> between the two lines.

A pylon takes at most 4 HV spans. It takes no other cable tier. A lightning arrester on the block above the top of
the pylon protects it.

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 7 x 1 blocks |
| Cable tier | HV only |
| Spans | 4 at most |
| Longest span | 48 blocks |
| Rated flow | 8,192 FE/t for each span |
| Taps | The blocks that touch its base |
| Hardness | 5.0 |
| Mine with | Iron pickaxe or better |

Breaking the pylon removes all its spans. The full cable cost of each span drops at the pylon.

## Problems

Jade shows "Tier: HV line", "Spans: n / 4" and "Network: n% load" (the highest load of its spans).

- "This node does not take insulated cable" (or another tier): you used a spool of another tier. Use HV cable.
- "Node is full": the pylon has 4 spans. Use another pylon.
- "Too long: max 48 blocks for HV line": place the pylons closer.
- "The span is blocked by a block": a block stands in the straight line between the two tops. Remove it or move a
  pylon.

## Next

- String the line with <ItemLink id="autarkia:hv_cable_spool" />.
- Feed it with a <ItemLink id="autarkia:wind_tower" />, a <ItemLink id="autarkia:hydro_turbine" /> or a
  <ItemLink id="autarkia:magma_tap" />.
- Cable-Layer Drones place HV pylons for the colony. The colony also uses them to upgrade hot insulated lines.
