---
navigation:
  title: Singularity Lance
  parent: t6/equipment.md
  position: 80
  icon: autarkia:singularity_lance
item_ids:
  - autarkia:singularity_lance
---

# Singularity Lance

<ItemImage id="autarkia:singularity_lance" scale="2" />

The Singularity Lance is the strongest weapon of Autarkia. You charge it for 3 seconds. Then it fires a beam of 128
blocks for 1 second. Every creature in the beam takes 80 damage. It never hits you or your allies. It is unbreakable
and charges by itself from your domain grid.

## Get it

There is no crafting recipe. The <ItemLink id="autarkia:fabricator" /> makes it:

| Inputs | Machine | Time | FE | Output |
|---|---|---|---|---|
| 1 <ItemLink id="autarkia:rail_rifle" />, 1 <ItemLink id="autarkia:disassembler_beam" />, 6 <ItemLink id="autarkia:aegis_plate" />, 2 <ItemLink id="autarkia:ai_core" />, 8 <ItemLink id="autarkia:lattice_crystal" /> | <ItemLink id="autarkia:fabricator" /> | 1,600 ticks | 12,500 FE/t | 1 Singularity Lance |

## Use it

1. Make sure that the lance has 1,000,000 FE or more.
2. Aim. Hold the use key for 60 ticks (3 s). Lightning arcs gather at the tip.
3. The lance fires by itself. The shot costs 1,000,000 FE.
4. The beam lasts 20 ticks. It follows your aim while it lasts.
5. Wait 200 ticks (10 s) for the next shot.

If you let go of the use key before the charge is full, nothing happens and no FE is used.

## The beam

- Each creature within 1.5 blocks of the beam, or within 4 blocks of its end, takes 80 damage once per shot. The target
  must be in line of sight. It also gets a knockback.
- The beam never hits you, your team allies, your tamed animals, players of a colony that you share, or players in
  creative or spectator mode.

## Modes

Press the mode key (V by default) to change the mode.

- Terrain safe (default): the beam never changes blocks.
- Terrain destroy: the end of the beam removes blocks within 3 blocks, at most 64 blocks per tick. It removes only
  blocks with a blast resistance of 50 or less, never block entities, and it drops nothing.

Terrain destroy works only when the server config allows it and the mobGriefing game rule is on. Near or inside the
domain of any colony, the beam is always terrain safe.

## Charging

- Keep it anywhere in your inventory within 64 blocks of a tether point. The grid charges it at up to 50,000 FE/t.
- A tether point is a Colony Core, an outpost core, a domain beacon or a phase node on a grid with an ONLINE
  Singularity Core.
- An external charger also works.

## Numbers

| Property | Value |
|---|---|
| Charge time | 60 ticks |
| Beam | 128 blocks, 20 ticks |
| Damage | 80, once per shot per target |
| Hit radius | 1.5 blocks on the path, 4 blocks at the end |
| Energy per shot | 1,000,000 FE |
| Cooldown | 200 ticks |
| Terrain destroy radius | 3 blocks |
| FE capacity | 50,000,000 FE |
| Charging | up to 50,000 FE/t |
| Durability | unbreakable |

## Problems

- The lance does not charge: it has less than 1,000,000 FE, the cooldown runs, or a beam still runs.
- Terrain destroy changes no blocks: you are near a colony domain, mobGriefing is off, or the server config turns it
  off.
- The mode message shows "Mode: Terrain safe" or "Mode: Terrain destroy".

## Next

- Wear the full Aegis set, for example the <ItemLink id="autarkia:aegis_chest" />, so that the grid pays your damage in
  a fight.
