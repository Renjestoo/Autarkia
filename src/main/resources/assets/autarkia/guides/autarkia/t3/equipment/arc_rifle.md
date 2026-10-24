---
navigation:
  title: Arc Rifle
  parent: t3/equipment.md
  position: 40
  icon: autarkia:arc_rifle
item_ids:
  - autarkia:arc_rifle
---

# Arc Rifle

<ItemImage id="autarkia:arc_rifle" scale="2" />

The arc rifle shoots an electric arc. The arc hits the first target and then jumps to up to 3 more targets near it. It never hits you, your pets, villagers or colony workers.

## Get it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 4 <ItemLink id="autarkia:steel_plate" />, 4 <ItemLink id="autarkia:copper_coil" />, 2 <ItemLink id="autarkia:basic_circuit" />, 1 <ItemLink id="autarkia:servo" />, 2 <ItemLink id="autarkia:capacitor_bank" />, 1 lightning rod | <ItemLink id="autarkia:fabricator" /> | 400 ticks | 150 FE/t | 1 arc rifle (empty) |

## Use it

1. Charge the rifle in a <ItemLink id="autarkia:battery_bank" /> or a <ItemLink id="autarkia:capacitor_bank" />.
2. Aim at a mob within 32 blocks.
3. Push the use button to shoot. One shot uses 5,000 FE.
4. Wait 1 second before the next shot.

The arc stops at the first solid block. After the first target, the arc jumps to the nearest valid target within 6 blocks. Each jump does 70% of the damage before it.

| Hit | Damage |
|---|---|
| First target | 10 |
| Jump 1 | 7 |
| Jump 2 | 4.9 |
| Jump 3 | 3.43 |

Players are targets only when PvP is on.

## Numbers

| Property | Value |
|---|---|
| Capacity | 1,000,000 FE |
| Charge rate | up to 2,000 FE/t |
| Cost | 5,000 FE per shot |
| Shots per full charge | 200 |
| Range | 32 blocks |
| Jumps | 3, within 6 blocks each |
| Cooldown | 1 s |
| Stack size | 1 |

## Problems

- Nothing happens when you push use: the rifle has less than 5,000 FE. Charge it.
- The arc does not reach the mob: a block is in the way, or the mob is more than 32 blocks away.

## Next

- <ItemLink id="autarkia:rail_rifle" /> is the Tier 4 rifle. Its recipe uses the arc rifle and keeps its FE.
