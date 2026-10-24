---
navigation:
  title: Shock Baton
  parent: t3/equipment.md
  position: 30
  icon: autarkia:shock_baton
item_ids:
  - autarkia:shock_baton
---

# Shock Baton

<ItemImage id="autarkia:shock_baton" scale="2" />

The shock baton is an electric melee weapon. A charged hit adds shock damage and slows the target. It uses FE instead of durability, so it never breaks.

## Get it

| Input | Machine | Time | Energy | Output |
|---|---|---|---|---|
| 2 <ItemLink id="autarkia:steel_ingot" />, 2 <ItemLink id="autarkia:copper_coil" />, 1 <ItemLink id="autarkia:basic_circuit" />, 1 <ItemLink id="autarkia:capacitor_bank" />, 1 leather | <ItemLink id="autarkia:fabricator" /> | 100 ticks | 100 FE/t | 1 shock baton (empty) |

## Use it

1. Charge the baton in a <ItemLink id="autarkia:battery_bank" /> or a <ItemLink id="autarkia:capacitor_bank" />.
2. Hit a mob with the baton.
3. With 1,000 FE or more, the hit uses 1,000 FE. The target takes 4 more shock damage and gets Slowness II for 2 seconds. An electric arc shows the shock.

With less than 1,000 FE the baton hits as a normal weapon.

## Numbers

| Property | Value |
|---|---|
| Capacity | 200,000 FE |
| Charge rate | up to 2,000 FE/t |
| Attack damage | 6 |
| Shock | +4 damage, Slowness II for 2 s |
| Cost | 1,000 FE per shock hit |
| Shock hits per full charge | 200 |
| Durability | none (unbreakable) |

## Next

- <ItemLink id="autarkia:arc_rifle" /> hits targets from a distance.
