---
navigation:
  title: Ore Outcrop
  parent: t4/materials.md
  position: 20
  icon: autarkia:ore_outcrop
item_ids:
  - autarkia:ore_outcrop
---

# Ore Outcrop

<ItemImage id="autarkia:ore_outcrop" scale="2" />

An ore outcrop is a small rock pile on the surface. It marks a large ore deposit in the ground below it. Each outcrop
has one ore kind: iron, copper, gold, tin, zinc or vantite. Look for outcrops to find deposits without digging.

## Get it

Outcrops generate in all Overworld biomes, above the big ore deposits. A deposit gets an outcrop only when it placed
enough ore blocks:

| Ore | Deposit Y range | Frequency | Vein size | Ore blocks for an outcrop |
|---|---|---|---|---|
| Iron | -8 to 56 | 1 in 8 chunks | 20 | 12 or more |
| Copper | 0 to 64 | 1 in 8 chunks | 20 | 12 or more |
| Gold | -48 to -8 | 1 in 16 chunks | 12 | 8 or more |
| Tin | 0 to 64 | 1 in 6 chunks | 20 | 12 or more |
| Zinc | 8 to 48 | 1 in 8 chunks | 18 | 12 or more |
| Vantite | -56 to -16 | 1 in 16 chunks | 12 | 8 or more |

The outcrop stands on the top block of the column above the center of the deposit. The surface must be at most 128
blocks above the deposit. On the ocean floor the outcrop stands in the water. The server option
`worldgen.outcropsEnabled` (default on) turns outcrops off.

## Use it

1. Look at the outcrop. Jade names its ore kind, for example "Marks a vantite deposit below".
2. Dig straight down below the outcrop to the Y range of its ore.
3. Break the outcrop with any tool or by hand. It drops 1 raw ore of its kind: raw iron, raw copper, raw gold, raw
   tin, raw zinc or <ItemLink id="autarkia:raw_vantite" />.
4. Silk Touch gives the outcrop block itself, with its ore kind.

Surveyors scan the surface for outcrops. Each outcrop they find goes to the colony deposit list with an estimate of
the ore under it. An outpost project needs such a known deposit.

## Numbers

| Property | Value |
|---|---|
| Hardness | 1.0 |
| Tool | None needed |
| Drop | 1 raw ore of its kind (no Fortune bonus) |
| Silk Touch drop | Ore Outcrop of the same kind |
| Size | 12 x 6 x 12 pixels |

## Next

- Mine the deposit below. For vantite read <ItemLink id="autarkia:vantite_ore" />.
- Found an outpost near a far deposit with an <ItemLink id="autarkia:outpost_seed" />.
