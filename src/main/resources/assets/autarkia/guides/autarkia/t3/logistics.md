---
navigation:
  title: Warehouse and Capsules
  parent: t3/index.md
  position: 4
  icon: autarkia:ledger_terminal
---

# Warehouse and Capsules

Tier 3 brings two ways to move items: the warehouse with its Loaders, and the capsule tube network.

## Warehouse

- <ItemLink id="autarkia:steel_rack" /> holds 3 crates and carries 8,192 load units.
- <ItemLink id="autarkia:warehouse_controller" /> keeps a list of all crates on the racks within 16 blocks.
- <ItemLink id="autarkia:ledger_terminal" /> shows the stock. You request items there, and you store items there.

A Loader worker carries the crates between the racks and the terminal. Without a Loader, requests wait.

## Capsule network

- <ItemLink id="autarkia:capsule_tube" /> carries capsules at 8 blocks per second.
- <ItemLink id="autarkia:capsule_station" /> sends and receives one stack per capsule. Its mode, filter and priority decide where each stack goes.

Use the capsule network to feed machines, to collect products and to link storage rooms.

<SubPages />
