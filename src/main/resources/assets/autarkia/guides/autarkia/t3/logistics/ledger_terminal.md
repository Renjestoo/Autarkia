---
navigation:
  title: Ledger Terminal
  parent: t3/logistics.md
  position: 30
  icon: autarkia:ledger_terminal
item_ids:
  - autarkia:ledger_terminal
---

# Ledger Terminal

<ItemImage id="autarkia:ledger_terminal" scale="2" />

The ledger terminal is the counter of the warehouse. It shows the stock of all racks. Select an item, set the amount and push Request. A Loader drives to the rack, lifts the crate and brings the items to you.

## Get it

<RecipeFor id="autarkia:ledger_terminal" />

## Use it

To request items:

1. Place the terminal within 16 blocks of a <ItemLink id="autarkia:warehouse_controller" />.
2. Open the terminal.
3. Type a name into the search box to find an item.
4. Click the item in the stock grid. The selected item has a blue frame.
5. Set the amount with -64, -1, +1 and +64. The amount is 1 to 1,728.
6. Push **Request**.
7. Stay within 6 blocks of the terminal. The Loader gives the items to you, or else puts them into the output slots.

To store items:

1. Put the items into the 9 input slots.
2. The terminal sends a put-away request by itself. A Loader brings a crate, fills it from the input slots and takes it back.

To add new stock positions:

1. Put empty <ItemLink id="autarkia:crate" /> items into the crate slot.
2. The Loaders put the crates onto free rack positions when they store items.

Hoppers and chutes can fill the input slots and the crate slot, and empty the output slots. The input slots take no filled crates.

## Screen

- Stock grid (left): each item with its available count. A red count is 0 available.
- Item tooltip: "Available N / total M". Items that other requests have reserved are not available.
- Request queue (right): the last 5 requests with "delivered / amount" and a colored bar.
- Queue tooltip: the state, "Delivered N / M", "ETA N s" and the reason.
- **Sites**: opens the linked stock view of all sites (Tier 4 depots).
- Warning at the right of the inventory label: "No warehouse", "No Loader assigned" or "Warehouse full".

Request states and bar colors:

| State | Color | Meaning |
|---|---|---|
| Queued | gray | waits for a Loader |
| Assigned, Fetching | blue | a Loader goes to the rack |
| Delivering | orange | the Loader brings the crate |
| Done | green | all items are delivered |
| Partial | yellow | some items are delivered |
| Failed | red | no items are delivered |

## Numbers

| Property | Value |
|---|---|
| Request size | 1 to 1,728 items |
| Delivery to you | within 6 blocks of the terminal |
| Input slots | 9 |
| Crate slot | 1 |
| Output slots | 18 |
| Open requests | 16 per warehouse |

## Problems

- "No warehouse": no controller is within 16 blocks. Place the terminal nearer to a controller.
- "No Loader assigned": the colony has no Loader, or the controller is outside the domain. Build a Loader.
- "Not enough in the warehouse" (Partial): the warehouse had fewer items than you requested.
- "None available": no crate holds the item.
- "FAILED: output full": you were not near, and the 18 output slots were full for 30 s. Empty the output.
- "FAILED: Loader stuck": the Loader did not finish within 120 s of its assignment. Check its path.
- "Warehouse full": no rack position was free for a returning crate. The crate lands as a crate block near the top of the terminal. Add racks.
- "The warehouse queue is full (16 requests)": wait until requests are done.
- "None of that item is available": the item is reserved or gone.

## Next

- A Loader delivery gives the advancement "Fetch!".
- <ItemLink id="autarkia:capsule_station" /> moves items between places without a Loader.
