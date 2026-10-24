---
navigation:
  title: Wind Tower
  parent: t4/power.md
  position: 30
  icon: autarkia:wind_tower
item_ids:
  - autarkia:wind_tower
---

# Wind Tower

<ItemImage id="autarkia:wind_tower" scale="2" />

The wind tower is a tall wind generator, 1 x 8 x 1 blocks, with a rotor on the top block. It makes more power when
the rotor is high, when the wind is strong and in bad weather.

## Get it

<RecipeFor id="autarkia:wind_tower" />

## Use it

1. Find an open, high place. A hill top is good.
2. Place the tower. It needs 8 free blocks of height. The front of the tower faces you when you place it.
3. Keep the area in front of the rotor free: a 9 x 9 square of blocks, one block in front of the rotor, centered on
   it.
4. Connect the tower to the network: let its base touch the base of an <ItemLink id="autarkia:hv_pylon" />, or
   string a span to it.

The tower gives power on every face except the front. The rotor turns faster with more output, and the top sways in
strong wind.

### Output

1. Base output: 512 FE/t at rotor height Y 64, plus 20 FE/t for each block higher. The base is 512 to 3,072 FE/t.
2. Wind: the base times 0.5 plus the wind strength, from 0.5 to 1.5.
3. Weather: times 1.15 in rain at the rotor, times 1.3 in a thunderstorm.
4. Obstruction: minus 3% for each solid block in the 9 x 9 square in front of the rotor. With 25 or more blocks the
   output is 0.
5. The output is at most 4,096 FE/t. The tower calculates it again every 20 ticks.

## Numbers

| Property | Value |
|---|---|
| Size | 1 x 8 x 1 blocks |
| Output | 0 to 4,096 FE/t |
| Base output | 512 to 3,072 FE/t, from the rotor height |
| Wind factor | 0.5 to 1.5 |
| Rain, thunder | x1.15, x1.3 |
| Obstruction | -3% for each block, 0 at 25 blocks |
| Energy buffer | 40,000 FE |
| Maximum output to the network | 8,192 FE/t |
| Mine with | Iron pickaxe or better |

## Problems

Jade shows "Output: n FE/t", "Buffer: stored / 40000 FE", "Wind: n%" and "Blocked: n".

- "Blocked: n" is high: n solid blocks stand in the 9 x 9 square in front of the rotor. Remove them, or turn the
  tower.
- "Wind data: none": the wind data of the world is not ready. The tower then uses a wind strength of 0.5.
- Low output: the rotor is low. Build the tower higher up.

## Next

- Feed an HV line with it. Read <ItemLink id="autarkia:hv_pylon" />.
- The colony builds wind towers by itself when it is short of power and has an HV network.
