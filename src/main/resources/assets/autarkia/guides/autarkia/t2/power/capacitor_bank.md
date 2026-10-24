---
navigation:
  title: Capacitor Bank
  parent: t2/power.md
  position: 40
  icon: autarkia:capacitor_bank
item_ids:
  - autarkia:capacitor_bank
---

# Capacitor Bank

<ItemImage id="autarkia:capacitor_bank" scale="2" />

The capacitor bank stores energy for your network. It takes power when the generators make more than the machines use,
and it gives power back when they make less. It is also the store of the <ItemLink id="autarkia:lightning_mast" />.

## Get it

<RecipeFor id="autarkia:capacitor_bank" />

## Use it

1. Place the bank next to the bottom block of a <ItemLink id="autarkia:wooden_pole" />, or connect a copper span to the
   bushing on its top.
2. The bank fills and empties by itself.
3. Look at the four stripes on its front. Each stripe is one quarter of the full charge.
4. To charge an item that holds FE, open the bank and put the item into its slot.

## Screen

- The item slot: an item that holds FE charges here at up to 512 FE/t.
- "12000 / 50000 FE": the energy in the bank.
- "24% charged": the charge in percent.
- "In 300, out 0 FE/t": the energy that went in and out in the last tick.

Jade shows "Stored: 12000 / 50000 FE (24%)" and "I/O: in 300 / out 0 FE/t".

## Numbers

| Property | Value |
|---|---|
| Capacity | 50,000 FE |
| Input and output | 2,048 FE/t |
| Item charge rate | 512 FE/t |
| Leak | 0.01% of the stored energy each tick |
| Lightning mast input | no rate limit |
| Spans on the bank | 2 (copper or other metal cable) |

## Problems

- The bank loses energy slowly when nothing uses it: this is the leak. A full bank leaks 5 FE/t.
- A lightning pulse along your wires reaches the bank: the bank breaks and drops as an item. The pulse stops there.
  Use a <ItemLink id="autarkia:lightning_mast" /> to catch lightning safely. Strikes on a mast do not break banks.

## Next

- Catch lightning with the <ItemLink id="autarkia:lightning_mast" />. Each strike fills the banks of its network.
- In Tier 3, the <ItemLink id="autarkia:battery_bank" /> stores 400,000 FE and does not leak.
