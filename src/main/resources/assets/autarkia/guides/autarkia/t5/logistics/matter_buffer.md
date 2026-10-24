---
navigation:
  title: Matter Buffer
  parent: t5/logistics.md
  position: 10
  icon: autarkia:matter_buffer
item_ids:
  - autarkia:matter_buffer
---

# Matter Buffer

<ItemImage id="autarkia:matter_buffer" scale="2" />

The matter buffer stores items as matter units (MU). It also holds free matter: matter that is no longer an item.
Nanite claims, linked tools and the Replicator use the matter buffers of your domain. It needs no power.

## Get it

<RecipeFor id="autarkia:matter_buffer" />

## Use it

1. Place the buffer inside your colony domain, so that your colony can use it. It is 2 x 2 x 2 blocks.
2. Open the buffer.
3. Shift-click a stack in your inventory to store it. Hoppers, chutes and workers can also put items in on any face.
4. Click "Take" on a row to get one stack back.
5. Click "Dissolve" on a row, then "Sure?" within 3 s, to turn all of that item into free matter.
6. Click "Auto: on" or "Auto: off" to switch auto-dissolve.

Stored items keep their identity: you get the same items back. Stored items and free matter share one capacity.
Dissolve cannot be undone.

## Screen

| Part | What it shows |
|---|---|
| Bar | Violet: the MU of the stored items. Cyan: the free MU. The full bar is the capacity. |
| "N + M / C MU" | Stored MU, free MU and capacity |
| Rows | Item, "count x MU each", the "Take" and "Dissolve" buttons; scroll for more |
| Row hover | "N stored, M MU each, T MU in all" |
| "Empty" | No items are stored |
| "Auto: on/off" | The auto-dissolve switch (on by default) |

## Auto-dissolve

While auto-dissolve is on, the buffer checks every 20 ticks. When the free matter of the whole domain is below 65,536
MU (config `matterReserve`), it dissolves its stored cobblestone, cobbled deepslate, dirt, gravel, sand, netherrack,
tuff and slag until the domain has that reserve again.

## Matter values

Every item has a matter value from the matter table. Hold Shift on an item tooltip to see it.

| Item | MU |
|---|---|
| Stone, cobblestone, dirt, sand, gravel, glass, planks | 1 |
| Log | 4 |
| Coal | 9 |
| Copper, tin, zinc, bronze or brass ingot | 27 |
| Iron ingot | 36 |
| Steel ingot | 54 |
| Gold ingot | 108 |
| Lattice shard | 216 |
| Vantite ingot | 270 |
| Vantite steel ingot | 324 |
| Diamond | 512 |
| Lattice crystal | 864 |
| Lattice alloy ingot | 1,188 |
| Netherite ingot | 4,096 |
| Raw ore | Its ingot x 6 in a domain of tier 5 or lower, x 8 in a tier 6 domain |
| Storage block | 9 x its ingot |
| Nugget | Its ingot / 9 |

## Numbers

| Property | Value |
|---|---|
| Size | 2 x 2 x 2 |
| Capacity | 1,048,576 MU (config `matterBufferCapacity`) |
| Power | None |
| Light level | 6 |
| Hardness, blast resistance | 5, 12 |

## Problems

- An item does not go in: it has no matter value, it is a container with items inside, it is a spawn egg, or it is
  on the blacklist (core seed, outpost seed, worker capsule, Autarky Seed, command blocks). Or the buffer is full.
- If you break the buffer, the stored items drop. The free matter is lost.

Jade shows "Matter: used / capacity MU" and "Free: N MU".

## Next

- Link a <ItemLink id="autarkia:nanite_hive" />: the hive uses the nearest buffer within 64 blocks.
- Link the <ItemLink id="autarkia:nanite_multitool" /> or the <ItemLink id="autarkia:disassembler_beam" />: sneak and
  use the tool on the buffer.
- In Tier 6 the <ItemLink id="autarkia:matter_compiler" /> writes items from free matter.
- The advancement "Matter of Fact" comes when your domain holds 100,000 MU or more.
