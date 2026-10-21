---
navigation:
  title: Frontier Workers
  parent: t4/index.md
  position: 4
  icon: autarkia:drone_frame
---

# Frontier Workers

Tier 4 has three new flying workers. Make them in the worker assembler. They use an FE battery and charge at a
recharge dock.

| Worker | Frame | Parts | Time | Power |
|---|---|---|---|---|
| Surveyor | 1 <ItemLink id="autarkia:drone_frame" /> | 2 advanced circuits, 2 servos, 1 survey lens, 2 vantite ingots | 1,600 ticks | 150 FE/t |
| Cable-Layer Drone | 2 drone frames | 2 advanced circuits, 6 servos, 4 vantite steel ingots, 1 HV cable spool | 2,000 ticks | 200 FE/t |
| Sentinel | 1 drone frame | 3 advanced circuits, 4 servos, 6 copper coils, 4 vantite steel ingots, 1 capacitor bank | 2,000 ticks | 200 FE/t |

## Surveyor

Health 14, battery 60,000 FE. It flies a square spiral over the job area, with points 32 blocks apart, 6 blocks above
the ground. At each point it scans (2,000 FE) with a radius of 32 blocks, plus 16 per range upgrade. It reports each
outcrop it finds to the colony deposit list. Before a scan it checks that it has the scan cost plus the energy to fly
back.

## Cable-Layer Drone

Health 30, armor 6, battery 200,000 FE. It builds pylon lines: it places HV pylons (5,000 FE each) and strings HV
spans (2,000 FE each). It carries 3 items, one pylon per slot, and goes back for more when it has none.

## Sentinel

Health 40, armor 10, battery 150,000 FE. It guards the domain on a patrol loop through the Core, the domain beacons
and the outpost cores.

- Arc gun: every 30 ticks, range 16 blocks, line of sight, 8 damage, 1,500 FE per shot.
- Targets: hostile mobs inside the domain plus 8 blocks. Creepers within 10 blocks of a domain block come first.
- It never attacks players, tamed animals, villagers or workers.
- Escort: it follows an outpost convoy at 4 to 8 blocks and attacks hostile mobs within 12 blocks of it.

## Long trips

Surveyors, Cable-Layers and escorting Sentinels keep the chunk they are in loaded while they travel. A domain has at
most 4 of these travel tickets. A worker without a free ticket waits.

## Caps at tier 4

The domain cap is 20. Sub-caps: Porter 6, Hauler Crab 6, Builder Drone 6, Loader 3, Lineman 3, Surveyor 2,
Cable-Layer 2, Sentinel 4. Each outpost core has its own cap of 6.
