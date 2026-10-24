---
navigation:
  title: Steam Repeater
  parent: t2/equipment.md
  position: 110
  icon: autarkia:steam_repeater
item_ids:
  - autarkia:steam_repeater
---

# Steam Repeater

<ItemImage id="autarkia:steam_repeater" scale="2" />

The steam repeater is a ranged weapon. You pump air pressure into it by hand. Then it fires steel nuggets fast, one
shot every quarter second.

## Get it

<RecipeFor id="autarkia:steam_repeater" />

## Use it

1. Carry <ItemLink id="autarkia:steel_nugget" /> in your inventory or in your off hand. They are the ammunition.
2. Hold the steam repeater.
3. Sneak and hold use to pump. The pressure goes from 0 to 100 in 40 ticks (2 s).
4. Stop sneaking. Hold use to fire. Each shot uses 10 pressure and 1 steel nugget.
5. A full charge gives 10 shots. Pump again when the pressure is low.

The gauge to the right of the hotbar shows the pressure while you hold the repeater in your main hand.

## Numbers

| Property | Value |
|---|---|
| Pressure | 0 to 100 |
| Pump speed | 2.5 per tick (40 ticks from 0 to 100) |
| Shot cost | 10 pressure and 1 steel nugget |
| Fire rate | 1 shot every 5 ticks (4 shots per second) |
| Shots per full charge | 10 |
| Damage per shot | 5 |
| Pressure leak | 1 per second when you do not use it |
| Durability | 1,000 (1 per shot) |
| Repair | Steel plate |

## Problems

- **It does not fire**: the pressure is below 10, or you have no steel nuggets. Pump, or get more nuggets.
- **It pumps but does not fire**: you still sneak. Stop sneaking, then hold use.
- **The pressure goes down**: the repeater leaks 1 pressure each second when you do not use it. Pump just before a
  fight.

## Next

- Fire 10 shots from one full charge for the advancement "Full Pressure".
- <ItemLink id="autarkia:steel_nugget" />
