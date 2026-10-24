---
navigation:
  title: Stone Flywheel
  parent: t1/power.md
  position: 60
  icon: autarkia:stone_flywheel
item_ids:
  - autarkia:stone_flywheel
---

# Stone Flywheel

<ItemImage id="autarkia:stone_flywheel" scale="2" />

The stone flywheel stores energy as spin. It takes the surplus of your generators and gives it back when the machines
need more. A full wheel turns fast and gives more FE/t. An empty wheel turns slowly.

## Get it

<RecipeFor id="autarkia:stone_flywheel" />

## Use it

1. Find a place with a solid block under the middle of the wheel.
2. Place the flywheel. It is 3 blocks wide, 3 blocks tall and 1 block deep.
3. Connect it to a node of your network with a <ItemLink id="autarkia:rope_coil" />.
4. Look at it with Jade. It shows the spin, the stored energy and the input and output.

## Numbers

Spin = square root of (stored FE / 40,000). Input and output = 40 x spin, but at least 10 FE/t.

| Property | Value |
|---|---|
| Capacity | 40,000 FE |
| Input and output | 10 to 40 FE/t |
| Self-loss | 0.5 FE/t + 0.005% of the stored FE each tick |
| Release limit | 2,000 FE |
| Size | 3 x 3 x 1 |
| Mining tool | Pickaxe |

## Problems

- You cannot place it: the block under the middle is not solid.
- The wheel breaks by itself: the block under the middle was removed.
- The wheel rolls away: you broke it while it held 2,000 FE or more. The wheel rolls down the slope and hits what is
  in its path for 2 to 20 damage. It also breaks soft blocks. When it stops, it drops as an item. The stored energy is
  lost.

Spin the wheel down before you move it: let the machines use the stored energy first.

## Next

- The colony builds flywheels by itself when the stored power lasts less than 30 seconds.
- In Tier 6 the gyro heart stores much more energy.
