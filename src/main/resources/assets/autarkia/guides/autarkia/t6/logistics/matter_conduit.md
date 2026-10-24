---
navigation:
  title: Matter Conduit
  parent: t6/logistics.md
  position: 10
  icon: autarkia:matter_conduit
item_ids:
  - autarkia:matter_conduit
---

# Matter Conduit

<ItemImage id="autarkia:matter_conduit" scale="2" />

The matter conduit moves items at once, with no pipe and no carrier. A conduit in SEND mode sends its items to the
conduits in RECEIVE mode on the same color channel. It works anywhere inside your colony domain.

## Get it

<RecipeFor id="autarkia:matter_conduit" />

One craft gives 4 conduits.

## Use it

1. Place a conduit next to the inventory that gives the items. Open it. Leave the mode at "Send".
2. Click a color square to choose the channel.
3. Place a second conduit next to the inventory that gets the items. Open it. Click the mode button to set "Receive".
4. Choose the same channel on the second conduit.
5. Click the output button until it shows the face of the target inventory.
6. Connect both conduits to your network. Each moved item costs 8 FE.
7. Put items into the sending conduit, by hand or with a machine, a chute or a belt.

## The screen

- Mode button: "Mode: Send" or "Mode: Receive".
- Output button: "Output: Down", "Up", "North", "South", "West" or "East". A receiving conduit pushes its items into the
  block on this face.
- 16 color squares in two rows: the channels 0 to 15. The chosen channel has a white frame.
- 9 buffer slots.
- Energy bar.
- Top right: "Ch N", or "Outside domain" in red. Bottom right: "Items: N/s".

## How it moves items

- Each tick a sending conduit moves up to 64 items. The receivers on its channel get the items in turn.
- An item goes into the block on the output face of the receiver. When that block is full, the item goes into the
  buffer of the receiver.
- A receiving conduit pushes its own buffer into its output face. This costs no FE.
- Only conduits of the same colony get the items. Receivers in unloaded chunks are skipped.
- The buffer does not take filled crates.
- Other blocks can put items into a sending conduit and take items out of a receiving conduit.

## Numbers

| Property | Value |
|---|---|
| Items per tick | up to 64 |
| Energy | 8 FE per item |
| FE buffer | 102,400 FE, input up to 2,048 FE/t |
| Item buffer | 9 slots |
| Channels | 16 |

## Jade

- "Mode: Send" or "Mode: Receive".
- "Channel N: color".
- "Items: N/s": the items moved in the last 20 ticks.
- "Energy: N / 102400 FE".
- "Outside domain": the conduit is not in a colony domain. It does nothing.

## Problems

- The items stay in the buffer: the sender has less than 8 FE, or no receiver on the channel is loaded.
- "Outside domain": move the conduit into your domain.
- The receiver fills its buffer: the block on its output face is full or takes no items. Check the output face.

## Next

- Send items to a conduit from the <ItemLink id="autarkia:domain_archive" />.
- A sending conduit that moves 1,000 items gives the advancement "Everywhere at Once".
