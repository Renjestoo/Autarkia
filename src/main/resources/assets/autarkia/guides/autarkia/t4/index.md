---
navigation:
  title: "Tier 4: Frontier"
  parent: index.md
  position: 4
  icon: autarkia:vantite_ingot
---

# Tier 4: Frontier

In Tier 4 your colony reaches far from its Core. You mine vantite deep underground and carry power on HV lines. The
colony founds outposts at far ore deposits. Cable cars and crate launchers move goods between the outposts and the
main base. The goal of this tier is a colony that mines far deposits by itself, and the materials for the next Core
Upgrade.

## Steps

1. Find vantite. An <ItemLink id="autarkia:ore_outcrop" /> on the surface marks a deposit below it. Mine
   <ItemLink id="autarkia:vantite_ore" /> between Y -64 and Y 0 with a <ItemLink id="autarkia:steel_pickaxe" /> or a
   diamond pickaxe.
2. Put the <ItemLink id="autarkia:raw_vantite" /> into the <ItemLink id="autarkia:blast_smelter" />. Each raw vantite
   gives 1 <ItemLink id="autarkia:vantite_ingot" />. A furnace cannot smelt vantite.
3. Make <ItemLink id="autarkia:advanced_circuit" /> in the <ItemLink id="autarkia:electrolyzer" />: 2 basic circuits
   and 1 vantite ingot for each circuit.
4. Make <ItemLink id="autarkia:vantite_steel_ingot" /> in the blast smelter: 1 vantite ingot and 1 steel ingot for
   each ingot.
5. Set the target of the <ItemLink id="autarkia:fabricator" /> to the <ItemLink id="autarkia:core_upgrade_t4" />. It
   needs 4 advanced circuits, 8 vantite steel ingots and 4 servos.
6. Use the Core Upgrade T4 on your Colony Core. After 100 ticks the colony is Tier 4.
7. Build a <ItemLink id="autarkia:froth_flotation_plant" /> behind your jaw crusher. Now 1 raw ore gives 4 ingots.
8. Build an HV line with <ItemLink id="autarkia:hv_pylon" /> blocks and the
   <ItemLink id="autarkia:hv_cable_spool" />. One HV line carries 8,192 FE/t over spans of up to 48 blocks.
9. Add Tier 4 generators: the <ItemLink id="autarkia:wind_tower" />, the <ItemLink id="autarkia:hydro_turbine" />
   or a <ItemLink id="autarkia:magma_tap" /> on a <ItemLink id="autarkia:magma_vent" />.
10. Assemble the Tier 4 workers in the worker assembler: the Surveyor, the Cable-Layer Drone and the Sentinel. Read
    [Frontier Workers](workers.md).
11. Found outposts at far deposits with an <ItemLink id="autarkia:outpost_seed" />. Each outpost stores its goods in
    a <ItemLink id="autarkia:depot" />.
12. Move goods between the sites with a <ItemLink id="autarkia:cable_car_station" /> line, or with a
    <ItemLink id="autarkia:crate_launcher" /> that throws crates into a <ItemLink id="autarkia:catch_net" />.
13. Make the vantite equipment: the <ItemLink id="autarkia:vantite_blade" />, the
    <ItemLink id="autarkia:vantite_excavator" />, the <ItemLink id="autarkia:grapple_launcher" />, the Frontier Suit
    and the <ItemLink id="autarkia:rail_rifle" />.
14. Prepare the <ItemLink id="autarkia:core_upgrade_t5" />. The fabricator makes it from 1 AI core, 8 lattice alloy
    ingots and 8 nanite paste. These are Tier 5 materials. Read [Tier 5](../t5/index.md).

## The colony at Tier 4

| Tier 4 value | Amount |
|---|---|
| Domain half-size | 48 blocks |
| Worker cap | 20 |
| Active projects | 4 |
| Daily budget | 1,000 points |
| Pattern library | 24 Patterns, capture up to 32 blocks on each axis |
| Domain beacons | 3, half-size 16 each |
| Outposts | 6, half-size 24 each, 6 workers each |

## What the colony does by itself

The Core proposes these Tier 4 projects when its sensors see the need:

| Project | When the Core proposes it |
|---|---|
| Outpost | A known deposit lies far from the Core, its metal stock is below half of its target for 24,000 ticks, and an outpost slot is free. |
| HV Link | An outpost has no HV link. Or an HV link runs at 80% load or more for 1,200 ticks: then a second line goes 3 blocks to the side. |
| Cable Car | Workers carry 256 or more items a day by hand between an outpost and the main site. |
| Sentinel Post | 3 or more hostile mobs stay in the domain for 2,400 ticks, or mobs and explosions destroy 8 or more colony blocks in one colony day. |
| Wind Tower | The power is short by 20% or more for 2,400 ticks, and the domain has an HV network. |
| Hydro Turbine | The power is short by 20% or more for 2,400 ticks, and the domain has a falling water column of 6 or more blocks. |
| Span Upgrade | An insulated cable span runs hot. The colony replaces the steel pylons on that path with HV pylons and HV spans. |

The workers do this work by themselves:

- A Porter or a Hauler Crab carries the Outpost Seed to the site and plants it. A Sentinel escorts it when the
  colony has one.
- Builder Drones build the outpost layout. Cable-Layer Drones build the HV link, one pylon at a time.
- Sentinels patrol the Core, the domain beacons and the outpost cores. They shoot hostile mobs.
- When the colony is short of a metal and knows no deposit for an outpost, it opens survey jobs. A Surveyor flies
  over each area and reports each ore outcrop to the colony deposit list.

The colony knows a deposit only after a Surveyor scan reported it. Without a Surveyor, plant an Outpost Seed yourself.
The colony does not make Outpost Seeds: for an Outpost project, keep one in colony storage. Read
<ItemLink id="autarkia:outpost_seed" />.

<SubPages />
