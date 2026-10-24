---
navigation:
  title: Treadwheel
  parent: t1/power.md
  position: 40
  icon: autarkia:treadwheel
item_ids:
  - autarkia:treadwheel
---

# Treadwheel

<ItemImage id="autarkia:treadwheel" scale="2" />

The treadwheel is a large wheel that an animal walks in. It makes power all the time while the walker is inside. You
do not have to hold use, as with the hand crank.

## Get it

<RecipeFor id="autarkia:treadwheel" />

## Use it

1. Place the treadwheel. It is 3 blocks wide, 3 blocks tall and 1 block deep.
2. Put a lead on a horse, donkey, mule, llama, villager, cow, pig, sheep or goat.
3. Lead the walker near the wheel. Hold one more lead in your hand and use it on the wheel.
4. The walker goes onto the inner floor and walks. The lead stays in the wheel.
5. Connect the wheel to a node with a <ItemLink id="autarkia:rope_coil" />.
6. To take the walker back, sneak and use the wheel with an empty hand. The walker comes back on your lead.

A Porter can also walk in the wheel.

## Numbers

| Walker | Output |
|---|---|
| Porter | 24 FE/t |
| Horse, donkey, mule, llama | 20 FE/t |
| Villager | 16 FE/t |
| Cow, pig, sheep, goat | 12 FE/t |

| Property | Value |
|---|---|
| Walkers at the same time | 1 |
| Buffer | 4,000 FE |
| Maximum output to the network | 40 FE/t |
| Size | 3 x 3 x 1 |
| Mining tool | Axe |

## Problems

- "Lead an animal or villager to the wheel (one walker at a time)": you lead no walker of a correct type, or the wheel
  has a walker already.
- The walker is gone: when the walker dies or disappears, the wheel drops the lead after 40 ticks.
- When you break the wheel, it drops the lead and the walker can move again.

## Next

- Store the power in a <ItemLink id="autarkia:stone_flywheel" />.
- The colony builds treadwheels by itself when the power is 25% short.
