---
navigation:
  title: Warehouse Controller
  parent: t3/logistics.md
  position: 20
  icon: autarkia:warehouse_controller
item_ids:
  - autarkia:warehouse_controller
---

# Warehouse Controller

<ItemImage id="autarkia:warehouse_controller" scale="2" />

The warehouse controller is the brain of the warehouse. It keeps a list of all crates on the racks around it. It takes the requests of the ledger terminals and gives each trip to a Loader of the colony.

## Get it

<RecipeFor id="autarkia:warehouse_controller" />

## Use it

1. Place the controller inside your colony domain.
2. Place <ItemLink id="autarkia:steel_rack" /> or <ItemLink id="autarkia:wood_rack" /> blocks with crates within 16 blocks of it. The area is a cube.
3. Connect the controller to the grid: put a node next to it, or string a span to its bushing.
4. Place a <ItemLink id="autarkia:ledger_terminal" /> in the same area.
5. Build at least one Loader in the <ItemLink id="autarkia:worker_assembler" />. The Loader carries the crates.

A rack belongs to the nearest controller whose area contains it. The controller uses energy only while requests are open.

## Numbers

| Property | Value |
|---|---|
| Area | a cube within 16 blocks (config 8 to 32) |
| Racks | up to 256, the nearest first |
| Request queue | 16 open requests (config 4 to 64) |
| Energy | 64 FE/t while requests are open |
| Buffer | 12,800 FE |
| Crates per trip | 1, or 2 with a Cargo Upgrade on the Loader |

## Screen and Jade

- "Racks: N": the racks the controller knows.
- "Fill: N%": the used slots of all its crates.
- "Overloaded racks: N": racks above 100% load. The line is red when N is more than 0.
- "Open requests: N": requests that are not done.
- "Free crate positions: N": empty positions on the racks.
- Jade shows "Racks N, fill P%, overloaded N" and "Queue N, Loaders N".

## Problems

- "No power" in Jade: the controller has open requests but no energy. The queue waits.
- "Loaders 0": the colony has no Loader. Requests stay "Queued".
- "Racks: 0": no rack is within 16 blocks, or another controller is nearer to the racks.
- "Overloaded racks: N": a rack is above 100%. Open it and take heavy items out.

## Next

- <ItemLink id="autarkia:ledger_terminal" /> is the screen where you request items.
- <ItemLink id="autarkia:worker_upgrade_cargo" /> lets a Loader carry 2 crates per trip.
- At Tier 4 the <ItemLink id="autarkia:depot" /> adds the stock of an outpost to the linked view.
