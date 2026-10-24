---
navigation:
  title: Claim Marker
  parent: t5/colony.md
  position: 20
  icon: autarkia:claim_marker
item_ids:
  - autarkia:claim_marker
---

# Claim Marker

<ItemImage id="autarkia:claim_marker" scale="2" />

The claim marker marks a corner of a nanite claim. Two markers at opposite corners make the box that a Nanite Swarm
takes apart.

## Get it

<RecipeFor id="autarkia:claim_marker" />

## Use it

1. Place one marker at a corner of the area that you want to remove.
2. Place the second marker at the opposite corner.
3. The lower of the two markers sets the reference height of the claim.
4. Open a <ItemLink id="autarkia:nanite_hive" /> within 64 blocks. It lists the marker pair.

The box goes from the lower marker up to the highest marker or the highest block in the box, whichever is higher. In
the hive you can add depth below the lower marker.

## Numbers

| Property | Value |
|---|---|
| Output of one recipe | 4 markers |
| Light level | 7 |
| Collision | None |
| Largest box | 32 x 32 blocks, 48 blocks high |
| Distance to the hive | 64 blocks at most |
| Hardness | 0.5 |

## Problems

- The hive does not list the pair: the box breaks a claim rule. It must be at most 32 x 32 blocks, lie fully in your
  domain, lie within 64 blocks of the hive and not overlap another claim, a watched Pattern or an open project.
- "Both claim markers must stand where the pair was listed": a marker was moved or broken. Place it again.

## Next

- Start the claim in the <ItemLink id="autarkia:nanite_hive" />.
