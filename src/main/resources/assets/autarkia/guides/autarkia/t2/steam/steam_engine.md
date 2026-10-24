---
navigation:
  title: Steam Engine
  parent: t2/steam.md
  position: 40
  icon: autarkia:steam_engine
item_ids:
  - autarkia:steam_engine
---

# Steam Engine

<ItemImage id="autarkia:steam_engine" scale="2" />

The steam engine turns the steam of a <ItemLink id="autarkia:boiler" /> into power. It makes up to 120 FE/t. Its piston
runs faster when it gets more steam.

## Get it

<RecipeFor id="autarkia:steam_engine" />

## Use it

1. Build a <ItemLink id="autarkia:boiler" /> first.
2. Look at the boiler. Place the engine on the ground with one free block between it and the boiler. The engine is
   1 block wide, 2 blocks tall and 2 blocks long. Its front faces you, and its second block fills the free block.
3. Make sure that the back of the engine touches the boiler.
4. Connect the engine to your network: a rope span to its sheave, a copper span to one of its bushings, or a
   <ItemLink id="autarkia:wooden_pole" /> next to it.
5. Light the boiler at fire level 2 or more. The engine starts when the boiler makes steam.

A boiler feeds at most 2 engines. Fire level 2 gives one engine all the steam that it can use. Fire level 4 feeds two
engines.

## Numbers

| Property | Value |
|---|---|
| Size | 1 wide x 2 tall x 2 long |
| Steam use | up to 20 mB per tick |
| Energy | 6 FE for each mB of steam |
| Maximum output | 120 FE/t |
| Buffer | 4,000 FE |
| Output to the network | up to 256 FE/t |
| Engines on one boiler | at most 2 |

Jade shows "Output: 120 FE/t" (the power that the engine makes now) and "Buffer: 2500 / 4000 FE".

## Problems

- "Output: 0 FE/t": the back of the engine does not touch a boiler, the boiler makes no steam, or the buffer is full
  because nothing uses the power. Turn the engine so that its back touches the boiler.
- The engine stops and the pressure goes up: the network takes no power, so the engine takes no steam. Add pressure
  valves to the boiler.
- A third engine does not run: one boiler feeds only 2 engines.

## What the colony does

The colony can be 30% short of power for 60 seconds while its storage holds fuel worth 64 coal. Then it proposes a
Boiler and Engine project. It builds at most 4.

## Next

- Store the power in a <ItemLink id="autarkia:capacitor_bank" />.
- In Tier 3, the <ItemLink id="autarkia:steam_turbine" /> makes up to 1,000 FE/t from steam.
