---
navigation:
  title: Nanite Multitool
  parent: t5/equipment.md
  position: 10
  icon: autarkia:nanite_multitool
item_ids:
  - autarkia:nanite_multitool
---

# Nanite Multitool

<ItemImage id="autarkia:nanite_multitool" scale="2" />

The Nanite Multitool is a pickaxe, axe, shovel, hoe, shears and sword in one tool. It mines at netherite level and can
mine a 3 x 3 area. Linked to a matter buffer, it turns the drops into matter and streams them home.

## Get it

The <ItemLink id="autarkia:fabricator" /> makes it from the <ItemLink id="autarkia:vantite_excavator" />. Fabricator
recipes do not show in this book, so the table gives the recipe.

| Inputs | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:vantite_excavator" />, 4 <ItemLink id="autarkia:lattice_alloy_ingot" />, 4 <ItemLink id="autarkia:nanite_paste" />, 1 <ItemLink id="autarkia:ai_core" />, 2 <ItemLink id="autarkia:lattice_crystal" />, 1 <ItemLink id="autarkia:coolant_cell" /> | <ItemLink id="autarkia:fabricator" /> | 800 ticks (40 s) | 1,250 FE/t | 1 Nanite Multitool |

The multitool keeps the FE of the excavator.

## Use it

1. Charge the multitool. Put it into the item slot of a <ItemLink id="autarkia:battery_bank" /> (2,048 FE/t) or a
   <ItemLink id="autarkia:capacitor_bank" /> (512 FE/t).
2. Mine any block. Each block costs 100 FE.
3. Press the mode key (default V) to switch between 1 x 1 and 3 x 3. In 3 x 3 the tool mines at half speed.
4. Use it on a block to strip a log or to make a path. Sneak and use it to till the ground.
5. To link it, sneak and use it on a <ItemLink id="autarkia:matter_buffer" />. The message "Linked to the matter
   buffer at X, Y, Z" shows.

While it is linked, each drop with a matter value goes into the matter cache of the tool, for 20 FE per item. Within
32 blocks of the linked buffer, the cache flows into the buffer at 64 MU per second as a visible nanite stream. When
the cache is full, or a drop has no matter value, the drop falls as a normal item.

## Numbers

| Property | Value |
|---|---|
| Tool types | Pickaxe, axe, shovel, hoe, shears, sword-efficient blocks, cobweb |
| Mining level | Netherite |
| Mining speed | 24 (half speed in 3 x 3) |
| Attack damage | 9 |
| Attack speed | 1.2 attacks per second |
| Energy capacity | 10,000,000 FE |
| Charge rate | Up to 20,000 FE/t |
| Cost per block | 100 FE |
| Matter cache | 4,096 MU, 20 FE per item |
| Stream to the buffer | 64 MU per second within 32 blocks (config `tools.matterStreamRange`) |
| Durability | None: it does not break |

The tooltip shows the FE, "Matter link: X, Y, Z (dimension)" or "Matter link: none (sneak + use on a matter
buffer)", and "Matter cache: N / 4,096 MU".

## Problems

- The tool mines slowly and drops nothing: its FE is 0. Charge it. At 0 FE the tool also cannot strip, till or shear.
- "This matter buffer is not complete": the buffer is not fully formed. Check the 2 x 2 x 2 buffer.
- The cache does not empty: you are more than 32 blocks from the linked buffer, or the buffer is full.

## Next

- The advancement "Ghost in the Matter" comes when a matter cache holds 4,096 MU.
- In Tier 6 the fabricator makes the <ItemLink id="autarkia:aegis_multitool" /> from it.
