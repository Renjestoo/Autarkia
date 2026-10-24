---
navigation:
  title: Belt
  parent: t2/logistics.md
  position: 10
  icon: autarkia:belt
item_ids:
  - autarkia:belt
---

# Belt

<ItemImage id="autarkia:belt" scale="2" />

The belt is a powered conveyor. It carries items at 2 blocks per second. Belts in a row make one line, and the line
puts its items into the block in front of its last belt: a chest, a crate, a machine, a chute or another belt.

## Get it

One craft gives 8 belts.

<RecipeFor id="autarkia:belt" />

## Use it

1. Look in the direction that the items must go. Place the belts in a row. Each belt faces away from you.
2. To go up or down, place the next belt one block higher or lower. The belts slope by themselves.
3. Put a <ItemLink id="autarkia:wooden_pole" /> or a <ItemLink id="autarkia:rope_pulley" /> next to any belt of the
   line. A network with enough power runs the whole line.
4. Put items onto the line:
   - from a hopper, chest or machine at the back of the first belt,
   - from the side or the top of any belt,
   - or use a stack on a belt. Up to 8 items go onto its middle.
5. Put a chest, a crate or a machine in front of the last belt.

A belt carries 2 entries. One entry is a group of 1 to 8 items. A full crate cannot go onto a belt.

## Numbers

| Property | Value |
|---|---|
| Speed | 2 blocks per second |
| Entries on one belt | 2 |
| Items in one entry | 1 to 8 |
| Longest line | 128 belts |
| Power | 1 FE/t for each belt of the line |
| Line buffer | 1,000 FE (in the first belt) |
| Input | up to 128 FE/t |
| Jam warning | after 600 ticks (30 seconds) |

Jade shows 3 lines:

- "Line: 12 belts, 5 entries": the length of the line and the entries on it.
- "Power: 12 FE/t (OK)": the power that the line needs. "(No power)" shows when the buffer cannot pay it.
- "State: Running", or "State: Jammed (45 s)": the line is blocked for that long.

## Problems

- "(No power)": the line stops, and the items stay where they are. Put a pole or a rope pulley next to a belt, and make
  sure that the network has power.
- "Not a connector or machine port": a cable span cannot end on a belt. Use a pole next to the belt.
- "State: Jammed": the block in front of the last belt takes no more items for 30 seconds. A red stripe shows, the belt
  scrapes and the colony notices. Empty the target or add storage.
- When you break a belt, its items drop.

## Next

- Split a line with the <ItemLink id="autarkia:belt_splitter" />.
- Sort items out of a line with the <ItemLink id="autarkia:sorting_chute" />.
