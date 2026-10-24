---
navigation:
  title: Transformer
  parent: t3/power.md
  position: 40
  icon: autarkia:transformer
item_ids:
  - autarkia:transformer
---

# Transformer

<ItemImage id="autarkia:transformer" scale="2" />

The transformer is the only node that joins two cable tiers. Use it to connect your Tier 2 copper wire grid to the new insulated grid. It also joins rope drive to metal cable.

## Get it

<RecipeFor id="autarkia:transformer" />

## Use it

1. Place the transformer between the two networks.
2. String the first span to it. Its tier becomes side A.
3. String a span of another tier to it. That tier becomes side B.
4. Add more spans if necessary. Each side takes up to 4 spans.
5. Use a <ItemLink id="autarkia:wrench" /> on the transformer to change the mode. The action bar shows "Transformer mode: mode".

The modes are:

- **Auto**: energy goes from the network with more spare power to the other network.
- **A to B**: energy goes from side A to side B only.
- **B to A**: energy goes from side B to side A only.

The transformer feeds no machines next to it. Connect machines to a pylon, an insulator or a bushing.

## Numbers

| Property | Value |
|---|---|
| Flow per tick | at most the rating of the lower tier |
| Copper wire to insulated cable | at most 256 FE/t |
| Loss | 2% of the energy it takes in |
| Buffer | 2 x the rating of the lower tier |
| Spans | 4 per side, 2 sides |

## Problems

Jade shows "A: tier B: tier", "Mode: mode" and "Flow: N FE/t".

- "A: copper wire B: none": the transformer has spans of one tier only. String a span of a second tier.
- "Flow: 0 FE/t" in Auto mode: neither network has more spare power. Set the mode with the wrench.
- "This node does not take ...": the transformer already has two tiers. A third tier is not possible.
- "Node is full": that side has 4 spans.

## Next

- <ItemLink id="autarkia:fuse_box" /> protects a line from overload.
- <ItemLink id="autarkia:hv_cable_spool" /> is the Tier 4 line. A transformer joins it to the insulated grid.
