---
navigation:
  title: Domain Archive
  parent: t6/logistics.md
  position: 20
  icon: autarkia:domain_archive
item_ids:
  - autarkia:domain_archive
---

# Domain Archive

<ItemImage id="autarkia:domain_archive" scale="2" />

The domain archive is the stock list of your whole domain. It shows every item in your storage. You choose an item and
an amount, and the archive moves the items at once into its buffer or into a receiving
<ItemLink id="autarkia:matter_conduit" />. It also shows the free MU (matter units) and the last matter withdrawals.

## Get it

<RecipeFor id="autarkia:domain_archive" />

## Use it

1. Place one archive inside your colony domain.
2. Connect it to your network. It uses 256 FE/t all the time and 16 FE per moved item.
3. Open it. Type a name into the search box, or scroll the item grid.
4. Click an item to choose it.
5. Set the amount with the -64, -1, +1 and +64 buttons. The amount goes from 1 to 1,728.
6. Click "Deliver to buffer". The items come into the 27 slots at the bottom. Take them out.
7. To send the items to a conduit, click the channel square to choose the channel. Then click "Deliver to conduit".

## The screen

- Search box and item grid (left): the stock of the domain. The small number is the available count. Hold the mouse
  over an item to see "Available N / total N".
- Free MU (right): the free matter of the domain.
- Withdrawal list (right): the last 16 matter withdrawals, newest first, with their reason. Examples of reasons:
  Compiler (matter compiler), Seed (Seed Matrix and seed unpack), Reform (Nanite Reform) and Restore (Restoration
  Field). The list shows 8 rows. Hold the mouse over it to see all rows with each amount and its age in seconds.
- Amount row: -64, -1, the amount, +1, +64.
- Channel square: a left click goes to the next channel, a right click to the previous one. The text shows "Ch N".
- "Deliver to buffer" and "Deliver to conduit" buttons.
- 27 buffer slots and the energy bar.
- A red state text shows when the archive does not work.

## Sources

The archive reads these blocks in the loaded chunks of the domain (the Core square, the beacon squares and the outpost
squares):

- Every <ItemLink id="autarkia:warehouse_controller" />. It delivers only items that no request has reserved.
- Every <ItemLink id="autarkia:depot" />.
- Every <ItemLink id="autarkia:matter_buffer" />: its stored items, not its free MU.
- The crates in the area of every <ItemLink id="autarkia:tally_board" />. A crate in two areas counts once.

## Numbers

| Property | Value |
|---|---|
| Archives per domain | 1 |
| Amount per request | 1 to 1,728 items |
| Requests per tick | 4 |
| Idle power | 256 FE/t |
| Delivery | 16 FE per item |
| FE buffer | 51,200 FE, input up to 16,384 FE/t |
| Item buffer | 27 slots |
| Stock refresh | every 20 ticks |

## Jade

- "Sources N, N FE/t": the number of source blocks and the FE used in the last tick.
- "Energy: N / 51200 FE".
- A red line when it does not work: "Duplicate archive", "Outside domain" or "No power".

## Problems

- "Delivered N x item": the request worked.
- "None available in the domain": no source holds that item.
- "No receiving matter conduit on channel color": set a conduit on that channel to Receive.
- "The archive has no power": the archive has less than 256 FE, or too little FE for the items.
- "Duplicate archive: this domain already has one": the archive that you placed first works. Break the second one.
- "The archive is outside every domain": move it into your domain.
- "The archive queue is full": wait. The archive handles 4 requests per tick.
- Other blocks can take items out of the buffer, but they cannot put items in.

## Next

- The withdrawal list shows the cost of the Nanite Reform of the <ItemLink id="autarkia:aegis_chest" /> armor set.
