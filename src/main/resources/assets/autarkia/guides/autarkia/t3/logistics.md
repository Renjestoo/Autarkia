---
navigation:
  title: Warehouse and Capsules
  parent: t3/index.md
  position: 3
  icon: autarkia:ledger_terminal
item_ids:
  - autarkia:steel_rack
  - autarkia:warehouse_controller
  - autarkia:ledger_terminal
  - autarkia:capsule_tube
  - autarkia:capsule_station
---

# Warehouse and Capsules

A <ItemLink id="autarkia:steel_rack" /> holds 3 crates and has a load rating of 8,192 units.

## Warehouse

A <ItemLink id="autarkia:warehouse_controller" /> indexes the racks within 16 blocks. A
<ItemLink id="autarkia:ledger_terminal" /> near it shows the stock. Select an item, set the amount and push Request.
A Loader drives to the rack, lifts the crate with its fork, brings it to the terminal and gives you the items when you
are within 6 blocks. Then it puts the crate back.

Put items into the input slots of the terminal to store them. Put empty crates into the crate slot for new stock.
Without a Loader, the request stays QUEUED and the terminal shows "No Loader assigned".

## Capsule tubes

<ItemLink id="autarkia:capsule_tube" /> carries one stack per capsule at 8 blocks per second.
A <ItemLink id="autarkia:capsule_station" /> sends, receives or does both. Capsules go to stations whose filter
matches first, then to stations without a filter. A full network turns red and hisses.

<RecipeFor id="autarkia:ledger_terminal" />
