---
navigation:
  title: Pressure Valve
  parent: t2/steam.md
  position: 30
  icon: autarkia:pressure_valve
item_ids:
  - autarkia:pressure_valve
---

# Pressure Valve

<ItemImage id="autarkia:pressure_valve" scale="2" />

The pressure valve keeps a <ItemLink id="autarkia:boiler" /> from bursting. Above 9 bar it lets out steam. Without
valves, a boiler that makes more steam than its engines use bursts at 12 bar.

## Get it

<RecipeFor id="autarkia:pressure_valve" />

## Use it

1. Place the valve on the top of a boiler. The top of a boiler has room for 4 valves.
2. Above 9 bar the valve opens. Its cap lifts, it screams and a white cloud comes out.
3. Below 9 bar it stays closed and lets out no steam.

## How many valves

Each fire level makes 10 mB of steam per tick. Each valve lets out 25 mB per tick.

| Fire level | Steam per tick | Valves for a boiler with no load |
|---|---|---|
| 1 | 10 mB | 1 |
| 2 | 20 mB | 1 |
| 3 | 30 mB | 2 |
| 4 | 40 mB | 2 |

One valve cannot hold fire level 4 when the engines stop. Two valves can.

## Numbers

| Property | Value |
|---|---|
| Opens above | 9 bar |
| Vents | 25 mB of steam per tick |
| Valves on one boiler | up to 4 |

The boiler screen shows "Venting" while its valves let out steam.

## Problems

- The valve does nothing: the pressure is 9 bar or less. This is correct.
- The pressure still goes up: the boiler makes more steam than the valves and the engines can take. Add a valve or
  lower the fire level.

## What the colony does

When a boiler of the colony reached 10 bar today, the colony proposes a Pressure Valve project with one valve on each
boiler. After a boiler burst, it asks for two valves on each boiler.

## Next

- Use the steam in a <ItemLink id="autarkia:steam_engine" /> instead of letting it out.
