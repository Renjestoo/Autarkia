---
navigation:
  title: Aegis Blade
  parent: t6/equipment.md
  position: 30
  icon: autarkia:aegis_blade
item_ids:
  - autarkia:aegis_blade
---

# Aegis Blade

<ItemImage id="autarkia:aegis_blade" scale="2" />

The Aegis Blade is the tier 6 sword. A hit does 30 damage and goes through most of the armor of the target. Its use
action, the Aegis sweep, hits every hostile mob around you. It is unbreakable and charges by itself from your domain
grid.

## Get it

There is no crafting recipe. The <ItemLink id="autarkia:fabricator" /> makes it:

| Inputs | Machine | Time | FE | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:lattice_blade" />, 4 <ItemLink id="autarkia:aegis_plate" />, 1 <ItemLink id="autarkia:ai_core" /> | <ItemLink id="autarkia:fabricator" /> | 1,000 ticks | 8,000 FE/t | 1 Aegis Blade |

## Use it

1. Hit a target. With 500 FE or more, the hit costs 500 FE and the armor of the target protects only a quarter as well.
2. Use the blade (right-click) for the Aegis sweep. It costs 50,000 FE.
3. The sweep hits every hostile mob within 6 blocks of you for 20 damage. Players, villagers and other mobs are safe.
4. Wait 100 ticks (5 s) for the next sweep.

## Charging

- Keep it anywhere in your inventory within 64 blocks of a tether point. The grid charges it at up to 50,000 FE/t.
- A tether point is a Colony Core, an outpost core, a domain beacon or a phase node on a grid with an ONLINE
  Singularity Core.
- An external charger also works.

## Numbers

| Property | Value |
|---|---|
| Attack damage | 30 |
| Attack speed | 2 per second |
| Armor pierce | the armor of the target counts only 25% (500 FE per hit) |
| Aegis sweep | 20 damage, 6 blocks, 50,000 FE |
| Sweep cooldown | 100 ticks |
| FE capacity | 100,000,000 FE |
| Charging | up to 50,000 FE/t |
| Durability | unbreakable |

## Problems

- The sweep does nothing: the cooldown runs, or the blade has less than 50,000 FE.
- A hit does less damage through armor: the blade has less than 500 FE.

## Next

- For targets far away, use the <ItemLink id="autarkia:singularity_lance" />.
