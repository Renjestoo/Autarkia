---
navigation:
  title: Gyro Heart
  parent: t6/power.md
  position: 20
  icon: autarkia:gyro_heart
item_ids:
  - autarkia:gyro_heart
---

# Gyro Heart

<ItemImage id="autarkia:gyro_heart" scale="2" />

The Gyro Heart is a very large flywheel. It stores 2,000,000,000 FE as spin. A full heart spins fast and moves much
FE per tick. While it holds 1% or more, nothing can break it.

## Get it

<RecipeFor id="autarkia:gyro_heart" />

It needs 4 <ItemLink id="autarkia:aegis_plate" />, 2 <ItemLink id="autarkia:stone_flywheel" />, 2
<ItemLink id="autarkia:lattice_alloy_block" /> and 1 <ItemLink id="autarkia:ai_core" />.

## Use it

1. Find a free space of 5 x 3 x 5 blocks. Place the heart. Its center is the bottom middle block.
2. Connect it to your network.
3. Use the <ItemLink id="autarkia:wrench" /> on a face to set that face. Each use goes to the next mode: none, in, out,
   in and out.
4. Look at it with Jade to see the stored FE, the input and output and the spin.

By default every face is "in and out", but the front face is "none".

## Numbers

Spin = square root of (stored FE / 2,000,000,000). Input and output = 1,048,576 FE/t x spin, but at least 10% of
1,048,576 FE/t.

| Property | Value |
|---|---|
| Size | 5 x 3 x 5 |
| Capacity | 2,000,000,000 FE |
| Input and output | 104,857 to 1,048,576 FE/t |
| Self-loss | 1 FE + 1 FE per 10,000,000 FE stored, each tick |
| Break lock | from 1% (20,000,000 FE) |
| Hardness, blast resistance | 8, 1,200 |
| Mining tool | Diamond pickaxe or better |

## Jade

- "Stored: N / 2000000000 FE (N%)".
- "I/O: in N / out N FE/t": the FE that went in and out in the last tick.
- "Spin: N%".
- "Energy: N FE".

## Problems

- "Spin it down first.": you tried to break it while it holds 1% or more. Let the machines use the FE first. In creative
  mode you can break it.
- An explosion cannot break the heart while it holds 1% or more.
- No FE goes in or out at one face: that face is set to none. Use the wrench on it.

## Next

- Feed it from the <ItemLink id="autarkia:singularity_core" />.
- The stored FE counts for the domain grid. It powers <ItemLink id="autarkia:bastion_block" /> walls and the Aegis
  armor.
