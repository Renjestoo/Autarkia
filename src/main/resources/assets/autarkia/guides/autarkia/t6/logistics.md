---
navigation:
  title: Archive and Conduits
  parent: t6/index.md
  position: 3
  icon: autarkia:domain_archive
item_ids:
  - autarkia:domain_archive
  - autarkia:matter_conduit
---

# Archive and Conduits

Tier 6 is the first tier where items move at once, without a visible carrier. It works only inside the domain.

## Domain archive

A domain has one <ItemLink id="autarkia:domain_archive" />. A second one shows "Duplicate archive" and does nothing.

- It shows the live stock of all warehouses, depots, Matter Buffers and tally board areas of the domain. The view
  refreshes every 20 ticks.
- A request moves the items at once from their storage into the 27-slot archive buffer, or into a matter conduit in
  RECEIVE mode that you choose.
- At most 1,728 items per request and 4 requests per tick. 16 FE per item, plus 256 FE/t while idle.
- It also lists the last 16 matter withdrawals of the colony and their reason, for example a Nanite Reform.

## Matter conduit

A <ItemLink id="autarkia:matter_conduit" /> has a 9-slot buffer, a mode (SEND or RECEIVE), a channel from 0 to 15 and,
in RECEIVE mode, an output face.

- Each tick a SEND conduit moves up to 64 items to the RECEIVE conduits on the same channel in the same domain, in
  turn. A RECEIVE conduit pushes the items into the inventory on its output face.
- Each item costs 8 FE. Without FE the items stay in the buffer.
- Outside the domain a conduit does nothing, and Jade shows "Outside domain". Receivers in unloaded chunks are skipped.

<RecipeFor id="autarkia:domain_archive" />
