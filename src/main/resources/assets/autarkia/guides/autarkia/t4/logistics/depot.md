---
navigation:
  title: Depot
  parent: t4/logistics.md
  position: 10
  icon: autarkia:depot
item_ids:
  - autarkia:depot
---

# Depot

<ItemImage id="autarkia:depot" scale="2" />

The depot is a 54-slot storage block for a site. A depot near an outpost core belongs to that outpost. Other depots
belong to the main site. Every ledger terminal shows the stock of all depots, and you can order items from a far
site.

## Get it

<RecipeFor id="autarkia:depot" />

## Use it

1. Place the depot. Within 16 blocks of an outpost core it belongs to that outpost. Else it belongs to the main site.
2. Use it to open its menu: 6 rows of 9 slots.
3. Type a site name in the field at the top, up to 32 characters. The name shows in the linked stock view.
4. Press "Linked view" to see the stock of all sites. This view only shows. It does not move items.
5. Workers fetch from depots and deliver to them like any colony storage.
6. Put a sender station next to the depot: a <ItemLink id="autarkia:cable_car_station" />, a
   <ItemLink id="autarkia:crate_launcher" />, a capsule station or a mag rail station.

### Orders from a far site

1. Open the <ItemLink id="autarkia:ledger_terminal" /> and its Sites tab.
2. Select a row of an outpost site.
3. Set the amount with "-64" and "+64" (1 to 1,728).
4. Press "Request from site". The chat shows "Transfer order queued".
5. Every 20 ticks the depot of that site pushes the ordered items into the sender station next to it.
6. The station carries the items to the other end of its line.

### The linked stock view

| Part | What it shows |
|---|---|
| Site list (left) | "Main site", then each depot by its name or as "Depot x y z", with "Slots used / total" |
| Stock rows (right) | The items of the selected site, or of all sites with a site column, 256 rows on each page |
| Transfer orders | Recent orders: "Waiting", "Moving", "Done", "Partial" or "Failed", with moved / ordered |
| Grey site | No update for 1,200 ticks and not loaded: "Last seen n min ago" |

A depot sends its stock when it changes (at most once every 100 ticks) and at least every 600 ticks.

## Numbers

| Property | Value |
|---|---|
| Slots | 54 |
| Outpost range | 16 blocks from the outpost core |
| Order service | Every 20 ticks |
| Stock update | When it changes, and at least every 600 ticks |
| Mine with | Iron pickaxe or better |

A depot takes no filled crates. Breaking it drops its contents.

## Problems

Jade shows "Fill: used / 54 slots" and "Site: main" or "Site: outpost at x y z".

- Order "Waiting" with "No transport at site": no sender station touches the source depot. Place a cable car station,
  a crate launcher, a capsule station or a mag rail station next to it.
- Order "Failed" or "Partial" with "None left at the site": the depot has no more of that item.
- "Choose a row of a remote site": you selected a row of the main site. Orders come only from outpost sites.
- "None of that item at the site": the site has none of the selected item.
- "That site is gone": the depot of that site no longer exists.

## Next

- Link the outpost depot with a <ItemLink id="autarkia:cable_car_station" /> line.
- Or throw its goods home with a <ItemLink id="autarkia:crate_launcher" />.
