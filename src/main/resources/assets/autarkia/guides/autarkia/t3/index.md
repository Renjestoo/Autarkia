---
navigation:
  title: "Tier 3: Electric Works"
  parent: index.md
  position: 3
  icon: autarkia:basic_circuit
---

# Tier 3: Electric Works

The goal of Tier 3 is an electric base. You build an insulated grid with a steam turbine, an electric ore line that gives 3 ingots per ore, and a warehouse. Electric workers and drones then take over much of the work. At the end you make the advanced circuits and vantite steel for the <ItemLink id="autarkia:core_upgrade_t4" />.

## Progression

1. **Install the upgrade.** Make the <ItemLink id="autarkia:core_upgrade_t3" /> in the <ItemLink id="autarkia:fabricator" />. Use it on your Tier 2 Colony Core. The domain grows to 65 x 65 blocks and the worker cap to 14.
2. **Build the insulated grid.** Make <ItemLink id="autarkia:insulated_cable_spool" /> and <ItemLink id="autarkia:steel_pylon" /> blocks. Use <ItemLink id="autarkia:wall_insulator" /> blocks inside buildings.
3. **Join the old grid.** Put a <ItemLink id="autarkia:transformer" /> between the copper wire grid and the insulated grid.
4. **Add steam power.** Upgrade a steam engine to a <ItemLink id="autarkia:steam_turbine" />. Put up to 4 boilers against its back and sides.
5. **Store and protect.** Add <ItemLink id="autarkia:battery_bank" /> blocks next to a pylon. Put a <ItemLink id="autarkia:fuse_box" /> on each long line.
6. **Make parts.** Make <ItemLink id="autarkia:basic_circuit" /> items in the fabricator: it gives 2 per craft. Make <ItemLink id="autarkia:servo" /> items there too.
7. **Build the ore line.** Place a <ItemLink id="autarkia:jaw_crusher" /> on 4 foundation blocks. Feed its crushed ore into a sluice. Then use a <ItemLink id="autarkia:magnetic_separator" /> and an <ItemLink id="autarkia:electrolyzer" />.
8. **Build electric workers.** Make a <ItemLink id="autarkia:drone_frame" /> for a Builder Drone. Build a Loader and a Lineman too. Place <ItemLink id="autarkia:recharge_dock" /> blocks near their work.
9. **Build the warehouse.** Place <ItemLink id="autarkia:steel_rack" /> blocks with crates, a <ItemLink id="autarkia:warehouse_controller" /> and a <ItemLink id="autarkia:ledger_terminal" />. Request items at the terminal; the Loader brings them.
10. **Lay capsule tubes.** Link machines and storage with <ItemLink id="autarkia:capsule_tube" /> lines and <ItemLink id="autarkia:capsule_station" /> blocks.
11. **Equip yourself.** Make the <ItemLink id="autarkia:electric_drill" />, the <ItemLink id="autarkia:electric_chainsaw" /> and the exo rig, from <ItemLink id="autarkia:exo_rig_helmet" /> to <ItemLink id="autarkia:exo_rig_boots" />. Charge them in a battery bank.
12. **Grow the domain.** Hold a <ItemLink id="autarkia:survey_lens" /> to find ore. Place up to 2 <ItemLink id="autarkia:domain_beacon" /> blocks there.
13. **Find vantite.** Mine <ItemLink id="autarkia:vantite_ore" /> from Y -64 to 0 with the electric drill. Put <ItemLink id="autarkia:raw_vantite" /> through the ore line to get <ItemLink id="autarkia:vantite_ingot" />.
14. **Make the Tier 4 parts.** In the electrolyzer, 2 basic circuits and 1 vantite ingot make 1 <ItemLink id="autarkia:advanced_circuit" />. In the blast smelter, 1 vantite ingot and 1 steel ingot make 1 <ItemLink id="autarkia:vantite_steel_ingot" />.
15. **Open Tier 4.** Make the <ItemLink id="autarkia:core_upgrade_t4" /> in the fabricator: 4 advanced circuits, 8 vantite steel ingots and 4 servos. Use it on the Core.

## What the colony does by itself

At Tier 3 the colony proposes these projects when its sensors see a need:

| Project | When |
|---|---|
| Fuse Box | a cable on a line snapped; a Lineman puts a fuse box on the line |
| Rack Upgrade | a rack collapsed, or a wood rack stays at 85% load or more; the row gets steel racks |
| Second Line | the machines of a LINE Pattern have a long queue; the colony builds a copy of the line |
| Pattern Repair | blocks of a watched build are missing; Builder Drones put them back |
| Recharge Dock | workers work far from a charger |
| Turbine Hall | the power is short for a long time; it builds a steam turbine with a boiler on foundation |
| Battery Row | the stored power lasts less than 60 seconds; it builds 3 battery banks around a pylon |

The colony also remembers failures. A remembered failure makes the matching project more likely for about a week.

The workers do the jobs of these projects:

- Builder Drones fly, place and cut any block, level ground and repair watched Patterns.
- Loaders carry the warehouse crates for your ledger requests and put away the items of the terminal input.
- Linemen climb poles, string and cut spans, and place nodes and fuse boxes.
- All electric workers go to a recharge dock by themselves when they need energy.

In Assisted mode the colony approves a Pattern Repair at once. In Autonomous mode it approves all projects itself.

<SubPages />
