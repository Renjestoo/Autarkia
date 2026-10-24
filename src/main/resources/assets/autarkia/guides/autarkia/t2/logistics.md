---
navigation:
  title: Belts and Sorting
  parent: t2/index.md
  position: 5
  icon: autarkia:belt
---

# Belts and Sorting

In Tier 2 items move on powered belts. A belt line carries items from a chest, a crate or a machine to the next block,
at 2 blocks per second. It goes up and down slopes by itself.

- The <ItemLink id="autarkia:belt" /> is the conveyor. Belts in a row that face the same way make one line of up to
  128 belts. The whole line uses 1 FE/t per belt.
- The <ItemLink id="autarkia:belt_splitter" /> sits in a line. It sends items to the front and to one side in turn, or
  it sends items that match its filter to the side. It uses 2 FE/t.
- The <ItemLink id="autarkia:sorting_chute" /> also sits in a line. It needs no power. Items that match its filter
  leave through the side, the rest go on.

A belt takes no cable span. Put a <ItemLink id="autarkia:wooden_pole" /> or a <ItemLink id="autarkia:rope_pulley" />
next to any belt of the line to power the whole line.

Use the Tier 1 <ItemLink id="autarkia:chute" /> where items only have to go down.

<SubPages />
