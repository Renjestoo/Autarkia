---
navigation:
  title: Magma Tap
  parent: t4/power.md
  position: 60
  icon: autarkia:magma_tap
item_ids:
  - autarkia:magma_tap
---

# Magma Tap

<ItemImage id="autarkia:magma_tap" scale="2" />

The magma tap is a heat generator, 1 x 2 x 1 blocks. It stands on a magma vent or on a lava source and makes power
from the heat. On a magma vent it runs forever.

## Get it

<RecipeFor id="autarkia:magma_tap" />

## Use it

1. Find a <ItemLink id="autarkia:magma_vent" /> in a cave floor or in the Nether. A vanilla lava source also works.
2. Place the tap so that its bottom block stands directly on the vent or on the lava source.
3. Put a water source next to the upper block of the tap, on a side or on top. Without water the tap gives half
   output and smoke rises from it.
4. Connect the tap to the network: let its base touch the base of an <ItemLink id="autarkia:hv_pylon" />, or string a
   span to it.

On a lava source the tap uses the lava up. After 24,000 running ticks the lava source turns into obsidian and the
output stops. A vent never runs out.

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 2 x 1 blocks |
| Output on a magma vent | 2,048 FE/t, endless |
| Output on a lava source | 1,024 FE/t for 24,000 running ticks |
| Without water next to the top | Half output |
| Energy buffer | 20,000 FE |
| Maximum output to the network | 8,192 FE/t |
| Mine with | Iron pickaxe or better |

## Problems

Jade shows "Output: n FE/t" and "Buffer: stored / 20000 FE".

- "Output: 0 FE/t": the block below the tap is not a magma vent and not a lava source. Flowing lava does not count.
- Half output and smoke: no water source touches the upper block. Put a water source on a side or on top of it.
- The output stopped after a long time on lava: the lava turned into obsidian. Move the tap to a magma vent.

## Next

- Feed an HV line. Read <ItemLink id="autarkia:hv_pylon" />.
- The tap gives power on every face except the front.
