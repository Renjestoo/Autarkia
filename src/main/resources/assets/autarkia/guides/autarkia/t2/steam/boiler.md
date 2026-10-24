---
navigation:
  title: Boiler
  parent: t2/steam.md
  position: 20
  icon: autarkia:boiler
item_ids:
  - autarkia:boiler
---

# Boiler

<ItemImage id="autarkia:boiler" scale="2" />

The boiler burns fuel and boils water into steam. A <ItemLink id="autarkia:steam_engine" /> against the boiler turns the
steam into power. The boiler makes no power by itself. Steam that nobody uses raises the pressure, and too much pressure
bursts the boiler.

## Get it

<RecipeFor id="autarkia:boiler" />

## Use it

1. Place the boiler. It is 2 blocks wide, 3 blocks tall and 2 blocks deep.
2. Give it water. Put a water bucket into its bucket slot, connect a pipe of another mod, or have water source blocks
   next to its bottom layer.
3. Put fuel into its fuel slot. Every furnace fuel works, for example coal or charcoal.
4. Place one or two steam engines with their back against the boiler.
5. Put a <ItemLink id="autarkia:pressure_valve" /> on top of the boiler. Put a second valve there for fire level 4.
6. Open the boiler and click a fire level button from 0 to 4.
7. The water heats up. At 100 C the boiler makes steam, and the engines start.

Each fire level makes 10 mB of steam per tick and uses 10 mB of water per tick. Level 2 feeds one engine at full power.
Level 4 feeds two engines. A higher fire level also burns the fuel faster.

## Screen

- Fuel slot: any furnace fuel.
- Bucket slot: a water bucket adds 1,000 mB of water. The empty bucket goes to the output slot next to it.
- Blue bar: the water in the tank, up to 8,000 mB. Point at it to see "Water: 6000 / 8000 mB".
- White bar: the steam in the boiler. Point at it to see "Steam: 1200 mB".
- Dial: the pressure from 0 to 12 bar. It is yellow from 9 bar and red from 10 bar.
- "P 3.0 bar": the pressure. It turns red from 10 bar.
- "T 100 C": the temperature of the water.
- "Fire 2": the fire level.
- "Venting": the pressure valves let out steam now.
- Buttons 0 to 4: set the fire level. Level 0 puts the fire out.

Jade shows "Pressure: 3.0 bar", "Water: 6000 mB" and "Fire level: 2".

## Pressure

The pressure is the steam divided by 400 (400 mB of steam is 1 bar).

| Pressure | What happens |
|---|---|
| above 9 bar | Each pressure valve lets out 25 mB of steam per tick with a scream |
| 10 bar | The gauge ticks, steam leaks from the boiler, the colony remembers a high-pressure day |
| 11 bar | The ground shakes |
| 12 bar | The boiler bursts |

When the boiler bursts, it is gone. Its blocks fly away as bricks and gravel, and a blast hurts the entities near it.
The blast does not break other blocks.

## Running dry

Without water the boiler gets hotter than 100 C. When it stays at 200 C or more for 200 ticks (10 seconds), it bursts
too. Keep the water tank full while the fire burns.

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 3 x 2 blocks |
| Water tank | 8,000 mB |
| Water from each source block next to the bottom layer | 25 mB per tick |
| Steam | 10 mB per tick for each fire level |
| Fire levels | 0 to 4 |
| Boiling point | 100 C |
| Heating | 0.25 C per tick for each fire level |
| Cooling without fire | 0.05 C per tick |
| Pressure | steam / 400 bar |
| Valves vent | above 9 bar, 25 mB per tick each |
| Bursts at | 12 bar, or 200 ticks at 200 C or more |
| Engines | at most 2 |

## Problems

- The engines do not run: the water is below 100 C, the fire level is 0, the fuel slot is empty or the tank is
  empty. The boiler also needs at least 10 mB of water for each fire level.
- The pressure goes up: the engines use less steam than the boiler makes. Lower the fire level or add a valve.
- One valve is not enough at fire level 4 when the engines stop. Two valves are.
- A third engine does nothing: one boiler feeds at most 2 engines.

## What the colony does

- The colony can be 30% short of power for 60 seconds while its storage holds fuel worth 64 coal. Then it proposes a
  Boiler and Engine project. It builds at most 4.
- When a boiler reached 10 bar today, the colony proposes a Pressure Valve project: one valve on each boiler. After a
  boiler burst, it asks for two valves on each boiler.

## Next

- Put a <ItemLink id="autarkia:pressure_valve" /> on top.
- Connect the <ItemLink id="autarkia:steam_engine" /> to your network.
- In Tier 3, the <ItemLink id="autarkia:steam_turbine" /> takes the steam of up to 4 boilers.
