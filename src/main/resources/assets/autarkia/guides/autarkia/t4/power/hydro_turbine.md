---
navigation:
  title: Hydro Turbine
  parent: t4/power.md
  position: 40
  icon: autarkia:hydro_turbine
item_ids:
  - autarkia:hydro_turbine
---

# Hydro Turbine

<ItemImage id="autarkia:hydro_turbine" scale="2" />

The hydro turbine is a water generator, 3 x 3 x 3 blocks. A column of water stands on its top center. The higher the
column, the more power. The water is not used up.

## Get it

<RecipeFor id="autarkia:hydro_turbine" />

## Use it

1. Place the turbine. The front faces you when you place it.
2. Keep the block in front of the bottom center free: the outlet. It must be air or water.
3. Build a straight column of water on the top center block of the turbine: the intake. Source and flowing water both
   count. A waterfall that falls onto the intake works.
4. Connect the turbine to the network: let a bottom block touch the base of an
   <ItemLink id="autarkia:hv_pylon" />, or string a span to it.

The head is the number of water blocks in the column directly above the intake, at most 32. The column stops at the
first block that is not water. The turbine measures the head again every 20 ticks.

## Numbers

| Property | Value |
|---|---|
| Size | 3 x 3 x 3 blocks |
| Output | 128 FE/t for each block of head |
| Maximum output | 4,096 FE/t at a head of 32 blocks |
| Water use | None |
| Energy buffer | 50,000 FE |
| Maximum output to the network | 8,192 FE/t |
| Mine with | Iron pickaxe or better |

| Head | Output |
|---|---|
| 6 blocks | 768 FE/t |
| 16 blocks | 2,048 FE/t |
| 32 blocks | 4,096 FE/t |

## Problems

Jade shows "Output: n FE/t", "Buffer: stored / 50000 FE" and "Head: n blocks".

- "Output: 0 FE/t" with a head above 0: the outlet in front of the bottom center is blocked. Make it air or water.
- "Head: 0 blocks": no water stands on the intake. The column must start on the top center block.
- Low output: the column is short or has a gap. Each block of the column must be water.

## Next

- The colony builds a hydro turbine by itself when it is short of power and its domain has a falling water column of
  6 or more blocks.
- The turbine gives power on every face except the front.
