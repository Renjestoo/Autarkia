## Autarkia 0.14.7

### Colony Core

- **The Core shows its tier.** Each colony tier adds parts to the Core model, and the parts of the lower tiers stay:
  - Tier 2: three brass steam pipes between the petals and steel bands on the column.
  - Tier 3: copper coils on the column, three ceramic insulators on the ring, and a cyan orb.
  - Tier 4: an antenna mast with a dish and a cyan beacon.
  - Tier 5: six lattice crystal clusters around the roots and a violet orb.
  - Tier 6: a gold halo with four glowing nodes and a white-gold orb.
- The tier parts have no collision, so they do not block clicks.
- The two bronze rings of the Core no longer flicker (Z-fighting) where their segments overlapped.

### Colony Core screen

- **Settings tab:** the project switches no longer run off the screen or cover the other controls. They show 10 per page in 2 wide columns, with page buttons. Long names show in full.
- **Overview tab:** the graph no longer covers the power line, and the graph legend fits in the panel.
- **Clear power readout:** the Core uses no power. Its two numbers describe the grid in the domain, so they now read "Power made" and "Power used" on the Overview tab and "Domain grid: 80 FE/t made, 0 FE/t used" in Jade. Power used counts only machines that work.
- **The colony log shows names, not ids.** For example, "Proposed Winding Post (priority 26)" instead of "Proposed winding_post P26".
- **Projects, Jobs and Workers tabs:** long lines stop at the edge of the panel or before the buttons.

### Colony and workers

- **Surveyors get survey jobs by themselves.** Before this fix, no colony rule opened survey jobs, so the colony never found deposits and never proposed an Outpost project. Now a Tier 4 colony with a Surveyor opens one survey job (radius 64) every 1,200 ticks when all of these are true:
  - It is short of a metal: an ingot is below half of its stock target (64 without a target).
  - It has a free outpost slot and no open Outpost project.
  - It knows no deposit that can get an outpost.
- The survey areas are 128 blocks apart in a square spiral around the Core, up to the maximum outpost distance (512 blocks). Each new job goes to the next area. After the last area, the list starts again.
- The colony does not make Outpost Seeds. For an Outpost project, keep one in colony storage.
- The colony builders follow the cable rule (see Power cables) and place a transformer when a new machine cannot take the network's cable.

### Field Manual

- **Rewritten from the start.**
- **Every item has its own page.** All 240 items, blocks, machines, tools and armor pieces have a dedicated page. Hold an item and press the guide key (G) to open its page.
- **Each page explains the item.** It shows:
  - what the item is;
  - how to get it (crafting recipe, machine recipe as a table, or where it generates);
  - how to use it, step by step;
  - its numbers;
  - the status messages and what they mean;
  - what to do next.
- **Full progression.** Each tier chapter starts with a numbered step list from the start of the tier to the Core Upgrade that opens the next tier. It also says what the colony and its workers do by themselves. The start page gives the path through all 6 tiers.
- **Pages no longer fail to open.** Six pages (Tier 1 power, storage and workers, Tier 2 steam and lightning, Tier 4 colony) did not open. Their 3D scenes placed machines in a preview world, and FrozenLib stopped the page when a machine block entity loaded. The guide now uses item pictures instead of 3D scenes.
- **New material chapters** for tin and bronze, zinc, brass and steel, pure metals, vantite and concentrates, and lattice.
- **Corrected facts.** The pages are written from the mod code, so some old statements are corrected. Examples: bronze armor gives 13 armor points for the full set, the Forge of Ascension needs 2 Aegis Plates, and the colony upgrades wood racks to steel racks only.
- The Frontier Workers, Outpost Seed and Tier 4 pages describe the survey rule.
- The Core Seed page has a table of the six Core looks.

### Big machines: shapes

- **Big machines follow their model.** The 38 machines with a 3D model and a footprint of more than one block no longer use a full box for every block of the footprint. The game builds the shape of each machine from its model on a 4-pixel grid.
- The selection outline follows the model, you can reach through the empty space around it, and you walk only into the parts that are really there.
- Examples: the Colony Core (3 x 3 x 3), the windmill tower, the waterwheel, the poles and pylons, and the open chamber of the Forge of Ascension.

### Machine models and animations

- **12 machines remodeled with moving parts.** Every motion follows the machine's real state:
  - Plate press: the ram presses.
  - Wire drawer: the capstan and the spool turn.
  - Magnetic separator: the drum turns and the belt runs.
  - Electrolyzer: the electrodes pulse.
  - Battery bank: the charge bar shows the stored energy.
  - Recharge dock: the contact arms lower while charging.
  - Domain beacon: the head turns while active.
  - Mag rail station: the clamps and carriages move when a sled docks.
  - Domain archive: the data drums turn.
  - Pattern loom: the heddles, beater and shuttle weave while scanning.
  - Winding post: the ratchet spindle turns while winding.
  - Pressure valve: the cap lifts while the boiler vents.
- **Belts:** the belt surface moves while the belt runs and stops when it is unpowered or jammed.
- **Gauges:** the boiler needle shows the steam pressure, and the steam turbine needle shows the rotor speed.
- **Animations:** the singularity core rings hover up and down while they turn; the magma vent, the molten crucible metal and the phase node crystal are animated; a warm cryo node shows melted frost; an empty gyro heart no longer wobbles.
- **Crucible:** the pot is now hollow. The rim is an open ring, you look into the pot and see its sooty floor, and the molten metal is visible inside while the crucible alloys. The flickering (Z-fighting) on the rim is gone.
- **Quern model:** both millstones are now solid stones without flickering (Z-fighting). The old stones were rings of overlapping pieces with a cap layer just above them.

### Power cables

- **Cable terminals on every machine.** Each machine, generator and storage block has a visible cable fitting: a wooden rope sheave on tier 1 blocks, a ceramic bushing with a copper cap from tier 2. Big machines have 2 to 4 fittings, and a cable leaves from the fitting that faces its other end. Before, cables ended at the middle of a block face, floated above big machines, or ran back into the machine.
- **Connectors:** ropes leave the rope pulley at the rim, cables end in the wall insulator cap, and cryo node cables end on 4 couplers at the head rim. The transformer and the fuse box have 2 bushings each.
- **Cable rule:** a rope ends only on a rope sheave, and metal cables end only on ceramic bushings. Machines with a turning shaft (tier 1 machines, the water wheel, the steam engine) have both; electric machines have only a bushing. A transformer joins rope and metal.
- **Belts take no cable:** a node next to any belt of a line powers the whole line.
- **Terminals on solid parts:** every terminal sits on a solid part, clear of turning wheels, rings, flywheels and other moving parts, and each cable leaves from the terminal that faces its other end. Ropes end on the rim of the sheave. The rope pulley has a real wheel.
- **Coils stack again:** a coil that you used to start a span now stacks with other coils. The first click of a span is kept for the player, not written on the item. Coils that still carry the old mark lose it in your inventory.
- Spans strung before this update keep their old ends and are not removed, even when they break the new rule. String them again to get the new ends.

### Fixes

- **Worlds did not load** ("Registry Loading" crash). The "It Lives" guide advancement used the Colony Core as its icon, but the Colony Core has no item. The icon is now the Worker Assembler.
- **Quern:** you can turn it again with an empty hand. Holding use with an empty hand made the quern try to insert nothing, and the server rejected that, so the stone never turned.
