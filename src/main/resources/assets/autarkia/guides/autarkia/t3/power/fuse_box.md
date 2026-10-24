---
navigation:
  title: Fuse Box
  parent: t3/power.md
  position: 50
  icon: autarkia:fuse_box
item_ids:
  - autarkia:fuse_box
---

# Fuse Box

<ItemImage id="autarkia:fuse_box" scale="2" />

The fuse box is an inline node that protects a line. It opens before an overload makes a cable snap. An open fuse cuts the line, and the cable cools down.

## Get it

<RecipeFor id="autarkia:fuse_box" />

## Use it

1. Place the fuse box on the line between the generator and the consumers.
2. String one span from the generator side to the fuse box.
3. String one span from the fuse box to the consumer side. Both spans must be of one tier.
4. Open the fuse box screen to see its state.
5. Push **Reset** to close a locked fuse. You can also use a <ItemLink id="autarkia:wrench" /> on it.

The fuse box works with every cable tier except the phase link. It feeds no machines next to it.

## When it trips

The fuse opens when one of these occurs:

- The load of its spans is 100% or more for 1 second.
- The load is 150% or more at one time.
- Lightning reaches the fuse box.

An open fuse closes again after 10 seconds. After the third trip within 1 minute, the fuse stays open (locked) until you reset it.

## Numbers

| Property | Value |
|---|---|
| Spans | exactly 2, of one tier |
| Trip | 100% load for 1 s, or 150% at once, or lightning |
| Closes again | after 10 s |
| Lock | 3 trips within 1 minute |

## Screen

- The lamp at the top left: green is closed, yellow is open, red is locked.
- "State: closed", "State: open (N s)" or "State: locked": the state and the seconds until it closes.
- "Trips in the last minute: N": the trip count of the current minute.
- "Spans: N / 2": the spans on the fuse box.
- **Reset**: closes the fuse and clears the trip count.

## Problems

- "State: locked": the line was overloaded 3 times in 1 minute. Remove consumers or add a second line. Then push Reset.
- "Spans: 1 / 2": the fuse box is not inline. String the second span.
- "This node does not take ...": the second span has another tier than the first one.
- "Fuse reset": the wrench message after a reset.

## Next

- At Tier 3 the colony remembers a snapped cable. A Lineman then puts a fuse box on that line by himself.
- <ItemLink id="autarkia:transformer" /> joins two cable tiers.
