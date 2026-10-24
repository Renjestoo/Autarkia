---
navigation:
  title: Frontier Workers
  parent: t4/index.md
  position: 6
  icon: autarkia:drone_frame
---

# Frontier Workers

Tier 4 has three new flying workers. The worker assembler makes them from a
<ItemLink id="autarkia:drone_frame" /> and parts. They need a colony of Tier 4 or higher. They use an FE battery and
charge at a recharge dock or at an outpost core.

| Worker | Frame | Parts | Time | Power |
|---|---|---|---|---|
| Surveyor | 1 drone frame | 2 <ItemLink id="autarkia:advanced_circuit" />, 2 servos, 1 survey lens, 2 <ItemLink id="autarkia:vantite_ingot" /> | 1,600 ticks | 150 FE/t |
| Cable-Layer Drone | 2 drone frames | 2 advanced circuits, 6 servos, 4 <ItemLink id="autarkia:vantite_steel_ingot" />, 1 <ItemLink id="autarkia:hv_cable_spool" /> | 2,000 ticks | 200 FE/t |
| Sentinel | 1 drone frame | 3 advanced circuits, 4 servos, 6 copper coils, 4 vantite steel ingots, 1 capacitor bank | 2,000 ticks | 200 FE/t |

## Surveyor

Health 14, armor 2, battery 60,000 FE. On a survey job it flies a square spiral over the job area. The points are 32
blocks apart, 6 blocks above the ground. At each point it scans (2,000 FE) all columns within 32 blocks, plus 16 for
each range upgrade. It reports each <ItemLink id="autarkia:ore_outcrop" /> to the colony deposit list, with an
estimate of the ore under it. Before a scan it checks that it keeps enough energy to fly back.

### Survey jobs

The colony opens survey jobs by itself. Every 1,200 ticks it opens one survey job when all these conditions are true:

- The colony is Tier 4 or higher and has a Surveyor.
- No survey job is open.
- The Outpost template is on, an outpost slot is free and no Outpost project is open.
- The stock of one or more ingots (iron, copper, gold, tin, zinc or vantite) is below half of its stock target.
  Without a target, the target is 64.
- No known deposit can get an outpost, or the last Outpost proposal found no site.

Each survey job has a radius of 64 blocks (25 scan points). The job areas are 128 blocks apart in a square spiral
around the Core, at most 512 blocks from the Core. The area of the Core is not in the list. Each new job goes to the
next area. After the last area, the list starts again.

## Cable-Layer Drone

Health 30, armor 6, battery 200,000 FE. It builds pylon lines: it places <ItemLink id="autarkia:hv_pylon" /> blocks
(5,000 FE each) and strings HV spans (2,000 FE each). It carries 3 items, one pylon in each slot. When it has no
pylon left, it gets more.

## Sentinel

Health 40, armor 10, battery 150,000 FE. It guards the domain on a patrol loop through the Core, the domain beacons
and the outpost cores.

- Arc gun: one shot every 30 ticks, range 16 blocks, line of sight, 8 damage, 1,500 FE for each shot.
- Targets: hostile mobs in the domain plus 8 blocks. Creepers within 10 blocks of the domain come first.
- It never attacks players, tamed animals, villagers or workers.
- Escort: it flies with a worker that carries an Outpost Seed and attacks hostile mobs within 12 blocks of it.

## Long trips

Surveyors, Cable-Layers and escorts keep the chunk they are in loaded while they travel. A colony has at most 4 of
these travel tickets. A worker without a free ticket waits. Jade then shows "Waiting for a travel ticket".

## Caps at Tier 4

The colony cap is 20 workers. The caps for each type are: Porter 6, Hauler Crab 6, Builder Drone 6, Loader 3,
Lineman 3, Surveyor 2, Cable-Layer 2, Sentinel 4. Each outpost core has its own cap of 6.

<SubPages />
