---
navigation:
  title: Chute
  parent: t1/storage.md
  position: 30
  icon: autarkia:chute
item_ids:
  - autarkia:chute
---

# Chute

<ItemImage id="autarkia:chute" scale="2" />

The chute is a wooden slide. Items slide down it by gravity, so it needs no power. Each chute moves items one block
forward and one block down. A row of chutes makes a slope.

## Get it

<RecipeFor id="autarkia:chute" />

## Use it

1. Place the chute. Its front is the low end.
2. Put the target one block lower, in front of the chute: a crate, a chest, another chute or a belt.
3. To make a slope, place the next chute one block forward and one block down.
4. Put items in. Use the chute with an item stack: up to 4 items go in at a time.
5. A chute also pulls up to 4 items from an inventory directly above it.
6. Hoppers and pipes can also put items into the chute.

## Numbers

| Property | Value |
|---|---|
| Items in one entry | 1 to 4 |
| Speed | 150 path units per tick (1,000 path units are one block) |
| Energy | None |
| Mining tool | Axe |

## Problems

Look at the chute with Jade:

- "State: Moving": items slide.
- "State: Waiting at the end": the target is full or missing. The items wait. Nothing drops.
- "State: Blocked": the items waited 30 seconds or more. Empty the target, or place a target in front, one block lower.
- A filled crate item does not go into a chute.

## Next

- Fill a <ItemLink id="autarkia:crate" /> from a machine above.
- In Tier 2 the sorting chute sends items to the side by a filter.
