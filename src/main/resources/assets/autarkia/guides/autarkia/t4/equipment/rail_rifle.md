---
navigation:
  title: Rail Rifle
  parent: t4/equipment.md
  position: 80
  icon: autarkia:rail_rifle
item_ids:
  - autarkia:rail_rifle
---

# Rail Rifle

<ItemImage id="autarkia:rail_rifle" scale="2" />

The rail rifle is the Tier 4 upgrade of the arc rifle. You charge a shot, then fire a slug in a straight line. It
goes through up to 5 targets. It runs on FE.

## Get it

| Inputs | Machine | Time | Output |
|---|---|---|---|
| 1 <ItemLink id="autarkia:arc_rifle" />, 6 <ItemLink id="autarkia:vantite_steel_ingot" />, 8 <ItemLink id="autarkia:copper_coil" />, 2 <ItemLink id="autarkia:advanced_circuit" />, 2 <ItemLink id="autarkia:battery_bank" /> | <ItemLink id="autarkia:fabricator" /> | 600 ticks, 500 FE/t (300,000 FE) | 1 Rail Rifle |

The FE in the old arc rifle goes into the new rifle.

## Use it

1. Charge the rifle in the item slot of a <ItemLink id="autarkia:battery_bank" /> (2,048 FE/t) or a
   <ItemLink id="autarkia:capacitor_bank" /> (512 FE/t).
2. Aim and hold use. You must hold it for at least 10 ticks.
3. Release use. The rifle fires and costs 20,000 FE.
4. After a shot the rifle has a cooldown of 40 ticks.

### The shot

- The damage is 8 + 0.8 x (charge ticks - 10). A charge of 10 ticks does 8 damage. A full charge of 30 ticks or
  more does 24 damage.
- The shot is hitscan: it hits at once, up to 96 blocks far.
- It hits the first 5 living entities on its line. Each one gets knockback 1.5 along the shot.
- It stops at the first solid block. It never breaks or changes blocks.
- It hits every living entity in its path, also animals and players.

## Numbers

| Property | Value |
|---|---|
| Energy | 3,000,000 FE |
| Charge rate | Up to 5,000 FE/t |
| Shot | 20,000 FE |
| Charge time | 10 to 30 ticks |
| Damage | 8 to 24 |
| Range | 96 blocks |
| Targets | Up to 5 |
| Knockback | 1.5 |
| Cooldown | 40 ticks (2 s) |
| Durability | None (it uses FE) |

## Problems

- The rifle does not start to charge: it has less than 20,000 FE, or the cooldown runs.
- No shot after release: you held use for less than 10 ticks.

## Next

- Fight from afar, then finish with the <ItemLink id="autarkia:vantite_blade" />.
