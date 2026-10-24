---
navigation:
  title: Domain Beacon
  parent: t3/colony.md
  position: 20
  icon: autarkia:domain_beacon
item_ids:
  - autarkia:domain_beacon
---

# Domain Beacon

<ItemImage id="autarkia:domain_beacon" scale="2" />

The domain beacon makes your colony domain larger. It adds a square around itself to the domain. The colony can then build and work there. The beacon needs power all the time.

## Get it

<RecipeFor id="autarkia:domain_beacon" />

## Use it

1. Go to the place where you want more domain. The beacon must be inside your Tier 3 or higher domain.
2. Place the beacon. You must be the owner or a trusted player.
3. Connect it to the grid: put a node next to it, or string a span to one of its bushings.
4. Look at the beacon with Jade. "Power OK" means its square is active.

Without power the square is inactive. The colony planner then does not use it. When you break the beacon, its square leaves the domain.

## Numbers

| Property | Value |
|---|---|
| Square | half-size 16 around the beacon (33 x 33 blocks); 24 from Tier 5 |
| Energy | 32 FE/t |
| Buffer | 6,400 FE |
| Beacons per colony | 2 at Tier 3, 3 at Tier 4, 4 at Tier 5, 6 at Tier 6 |
| Light | level 10 |

## Problems

The beacon breaks and drops as an item when it cannot work there:

- "Place the beacon inside your T3 or higher domain": the spot is outside the domain, or the colony is below Tier 3.
- "Only the owner or a trusted player": you have no rights in this colony.
- "This colony has all the beacons its tier allows": remove a beacon, or upgrade the Core.
- "The beacon area would overlap another colony": choose a spot farther from the other colony.

Jade shows "Domain +N blocks" and "Power OK" or "Power OFF".

- "Power OFF": the beacon gets less than 32 FE/t. The square is inactive.

## Next

- <ItemLink id="autarkia:survey_lens" /> shows where the ore is, so you know where to grow the domain.
- At Tier 4 outposts add domain far from the Core.
