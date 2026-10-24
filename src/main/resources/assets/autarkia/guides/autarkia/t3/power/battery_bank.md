---
navigation:
  title: Battery Bank
  parent: t3/power.md
  position: 60
  icon: autarkia:battery_bank
item_ids:
  - autarkia:battery_bank
---

# Battery Bank

<ItemImage id="autarkia:battery_bank" scale="2" />

The battery bank is the large energy store of Tier 3. It holds 400,000 FE and does not leak. Its slot charges electric tools and armor.

## Get it

<RecipeFor id="autarkia:battery_bank" />

## Use it

1. Place the battery bank next to a <ItemLink id="autarkia:steel_pylon" /> or a <ItemLink id="autarkia:wall_insulator" />.
2. You can also string a span directly to its ceramic bushing.
3. The bank stores the spare energy of the network and gives it back when the generators cannot supply enough.
4. To charge an item, open the bank and put the item into its slot.
5. Take the item out when the bar of the item is full.

Pipes and hoppers can use the item slot too.

## Numbers

| Property | Value |
|---|---|
| Capacity | 400,000 FE |
| Input and output | 1,024 FE/t each |
| Leak | none |
| Item charge rate | up to 2,048 FE/t |

The electric tools and the exo rig take at most 2,000 FE/t.

## Screen and Jade

- "N / M FE": the stored energy and the capacity.
- "N% charged": the fill level.
- "In N, out M FE/t": the energy that went in and out in the last tick.
- Jade shows "Stored: N / M FE (P%)" and "I/O: in N / out M FE/t".

## Problems

- The item does not charge: the bank is empty. Check the generators of the network.
- "In 0, out 0 FE/t" for a long time: the bank is not connected. Put a node next to it.

## Next

- The <ItemLink id="autarkia:recharge_dock" /> uses 2 battery banks.
- The <ItemLink id="autarkia:core_upgrade_t3" /> needs 1 battery bank.
- Tier 4 suit parts and tools use battery banks: <ItemLink id="autarkia:frontier_suit_chest" />, <ItemLink id="autarkia:rail_rifle" />.
- At Tier 3 the colony builds a Battery Row by itself when its stored power lasts less than 60 seconds.
