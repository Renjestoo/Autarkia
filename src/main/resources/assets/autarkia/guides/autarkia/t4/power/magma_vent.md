---
navigation:
  title: Magma Vent
  parent: t4/power.md
  position: 50
  icon: autarkia:magma_vent
item_ids:
  - autarkia:magma_vent
---

# Magma Vent

<ItemImage id="autarkia:magma_vent" scale="2" />

A magma vent is a glowing block in cave floors and in the Nether. It is an endless heat source for the
<ItemLink id="autarkia:magma_tap" />. Lava drops and large smoke rise from its top, so you can see it from afar.

## Get it

Magma vents generate by themselves:

| Dimension | Y range | Frequency | Where |
|---|---|---|---|
| Overworld (all biomes) | -60 to -16 | 1 try in 8 chunks | On a cave floor: it replaces the stone under an air space |
| Nether (all biomes) | 10 to 60 | 1 try in each chunk | On the floor of an open space: it replaces netherrack, basalt or blackstone |

Each try starts at a random point in the Y range. The point must be in air. From there the try goes down through the
air, up to 16 blocks, and replaces the first solid floor block. A try in solid rock places no vent.

## Use it

1. Find a vent. It glows (light level 6) and smokes.
2. Leave it in place. Build the magma tap on top of it.
3. To move a vent, break it with a Silk Touch pickaxe. Without Silk Touch it drops a magma block.

## Numbers

| Property | Value |
|---|---|
| Hardness | 3.0 |
| Blast resistance | 6.0 |
| Light level | 6 |
| Tool | Any pickaxe |
| Drop | 1 magma block |
| Silk Touch drop | Magma Vent |
| Magma tap output on a vent | 2,048 FE/t, endless |

## Next

- Place a <ItemLink id="autarkia:magma_tap" /> on it.
