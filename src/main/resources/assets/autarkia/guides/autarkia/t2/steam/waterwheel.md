---
navigation:
  title: Waterwheel
  parent: t2/steam.md
  position: 10
  icon: autarkia:waterwheel
item_ids:
  - autarkia:waterwheel
---

# Waterwheel

<ItemImage id="autarkia:waterwheel" scale="2" />

The waterwheel is a generator that turns in moving water. It needs no fuel and no worker. It makes up to 128 FE/t,
day and night.

## Get it

<RecipeFor id="autarkia:waterwheel" />

## Use it

The wheel is 3 blocks long, 3 blocks tall and 1 block thick. It stands in line with the direction that you look when
you place it. The block where you place it becomes the middle of its bottom row.

1. Find or make a water stream that flows in a straight line.
2. Place the wheel so that the stream flows under its bottom row, along the wheel. Each of the 3 blocks under the
   bottom row with flowing water gives 24 FE/t.
3. Or let water fall onto the top of the wheel. Each of the 3 blocks above the top row with flowing or falling water
   gives 40 FE/t. Falling water must fall on one end of the wheel, not on the middle and not on both ends.
4. Connect the wheel with a rope span or a copper span to the sheave or the bushing on its side. You can also put it
   next to a <ItemLink id="autarkia:wooden_pole" /> or a <ItemLink id="autarkia:rope_pulley" />.

Water source blocks give nothing. Water that flows across the wheel gives nothing. Water that turns the wheel in the
opposite direction works against the other water.

## Numbers

| Property | Value |
|---|---|
| Size | 3 long x 3 tall x 1 thick |
| Flowing water under the bottom row | 24 FE/t per block |
| Flowing or falling water above the top row | 40 FE/t per block |
| Maximum output | 128 FE/t |
| Buffer | 4,000 FE |
| Output to the network | up to 256 FE/t |
| Checks the water | every 20 ticks |

Jade shows "Output: 72 FE/t" (the power that the wheel makes now) and "Buffer: 4000 / 4000 FE".

## Problems

- "Output: 0 FE/t": the water does not flow along the wheel, or the blocks are water sources. Make the water flow
  under or onto the wheel.
- The output is lower than you expect: water on both sides works against itself. Use water on one side only.

## What the colony does

The colony can be 20% short of power for 60 seconds while the domain has a site with flowing water. Then it proposes
a Waterwheel project. It builds at most 6.

## Next

- For more power, build a <ItemLink id="autarkia:boiler" /> with a <ItemLink id="autarkia:steam_engine" />.
- Carry the power with the <ItemLink id="autarkia:copper_wire_spool" />.
