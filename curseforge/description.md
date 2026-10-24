![Autarkia](banner.png)

# A factory that builds itself

You start alone with a hand crank and a seed. The seed grows into a **Colony Core**: a factory brain that watches your base, plans what it lacks, records what you build, and sends robot workers to build, repair, extend and copy it.

Everything is physical. Cables sag, heat up and snap. Items travel where you can see them. Racks bend and collapse. Flywheels keep their momentum. At the top, the colony takes matter apart and writes it again, and the power grid makes you and your base practically indestructible.

> **Autarkia is new.** There could still be bugs that have to be fixed.

---

## How the colony works

**Sense, plan, propose, build, run, fail, learn, expand.**

- The **Colony Core** reads sensors: power deficit, full storage, ore stock, failures.
- It scores **project templates** and your own recorded **Patterns**, and proposes a project with ghost blocks and a project card.
- **Workers** carry real items from your storage and build it. Nothing appears from nowhere.
- When something fails (a cable snaps, a boiler bursts, a rack collapses), the colony **remembers** it and builds better next time.
- **Your builds become the colony's plans.** Record a line once with the Pattern Loom. The Core can then build it at a new site, rebuild it after damage, put it in an outpost, and late game pack it into a seed.
- You choose how much freedom the colony has: **Manual**, **Assisted** or **Autonomous**.
- **The Core grows with your colony.** Each tier adds parts to the Colony Core, from brass steam pipes to a gold halo, and its orb changes colour.

![The Core grows with your colony](core_tiers.png)

---

## Six tiers

![Tier 1 scene](scene_1_muscle_and_timber.png)

![Tier 1 machines](tier_1.png)

### Tier 1: Muscle and Timber
Hand cranks, treadwheels, windmills and rope drives. Plant the Core Seed: roots crawl over the ground, a frame rises and the Core opens with a slow mechanical bloom. Your first Porters build what the Core proposes. Overload a rope drive and it slips, squeals, then snaps and whips. Break the mount of a spinning stone flywheel and it rolls down the hill with its stored energy.

![Tier 2 scene](scene_2_steam_and_copper.png)

![Tier 2 machines](tier_2.png)

### Tier 2: Steam and Copper
Boilers, steam engines and copper wire. A copper wire that overloads glows orange, then red, sags lower and snaps. Hauler Crabs collect the pieces. Lightning hits your highest span and blows up the first unprotected machine; build a Lightning Mast and storms fill your capacitors for free. Overfire a boiler and the gauge climbs, the valve screams and the boiler bursts.

![Tier 3 scene](scene_3_electric_works.png)

![Tier 3 machines](tier_3.png)

### Tier 3: Electric Works
Builder Drones, Loaders, Linemen, capsule tubes and a warehouse. A creeper blows a hole in the factory and the drones put back the same blocks from the Pattern. An overloaded rack bends and fails, crates fall like dominoes, and the next day the colony rebuilds it in steel. Request 64 iron at the Ledger Terminal and a Loader brings the crate.

![Tier 4 scene](scene_4_frontier.png)

![Tier 4 machines](tier_4.png)

### Tier 4: Frontier
Outposts, high-voltage lines, cable cars and crate launchers. Surveyors search the land for ore deposits. When the ore is too far, an Outpost Seed from colony storage walks out with an escort, a second base grows at the deposit and a pylon line goes up across the hills. Gondolas full of ore swing across a ravine in the wind.

![Tier 5 scene](scene_5_matter.png)

![Tier 5 machines](tier_5.png)

### Tier 5: Matter
Lattice crystals, plasma generators, superconductors, the Matter Buffer and mag rails. Mark a hill and a nanite cloud takes it apart layer by layer; ores fly out as sorted colour streams and a buried chest floats in a bubble. The Replicator Foundry prints a walker that founds an outpost you did not build.

![Tier 6 scene](scene_6_ascendant.png)

![Tier 6 machines](tier_6.png)

### Tier 6: Ascendant
The Singularity Core, phase links, the Matter Compiler, the Autarky Seed and Aegis gear. Throw the Autarky Seed and a powered base rises from the soil in about a minute. A Wither explodes against your walls and no block breaks. A lethal hit turns your body into a nanite cloud that forms again 3 blocks away. A creeper crater refills by itself.

---

## Features

![The workers](workers.png)

![Armour sets](armor.png)

![Tools and weapons](items_equipment.png)

![Materials](items_materials.png)

![Components and colony items](items_components.png)

- **Power:** one energy unit (FE, compatible with other mods), generators and storage per tier, and 6 cable tiers from 40 FE/t to 1,048,576 FE/t. Cables have load, heat and sag, and fail in visible ways. Ropes connect to rope sheaves and metal cables to ceramic bushings; a transformer joins the two.
- **Processing:** ore chains that raise the yield per raw ore from tier to tier, up to 8 ingots per raw ore with the Matter Compiler.
- **Logistics:** rope drives, chutes, belts, capsule tubes, cable cars, crate launchers, mag rails and matter conduits. Every item that moves is visible until tier 6.
- **Workers:** 10 worker types (walkers, crabs, drones, a self-replicating walker and a nanite swarm). The Worker Assembler builds most of them; the Replicator Foundry prints the Replicator Walker and the Nanite Hive makes the swarm.
- **Big machines:** the outline and the collision follow the model, so you can reach past a machine through its empty space.
- **Colony intelligence:** rule-based planning from sensors, templates, your Patterns and failure memory. No machine learning, and no hidden cheats.
- **Equipment:** 6 tiers of tools, armor and weapons, 50 items in total. Tier 6 Aegis gear draws on your grid to absorb damage.
- **Field Manual:** an in-game guide (GuideME) with a page for each of the 240 items, blocks, machines, tools and armor pieces. Hold an item and press G to open its page. Each tier chapter gives the full progression, step by step.
- **Integrations:** JEI recipe categories and info pages, Jade tooltips, and a YACL config screen (optional).

---

## Getting started

1. Find tin ore. Make bronze in a **Crucible**.
2. Turn a **Hand Crank** and send the power over rope to a **Stamp Mill**.
3. Craft a **Core Seed** and plant it.
4. Watch the colony propose its first project.

Open the **Field Manual** for the full guide.

---

## Requirements

- Minecraft 26.3 with NeoForge 26.3.0.95 or newer.
- Required libraries: GeckoLib, SmartBrainLib, Tiny Multiblock Lib, Magic Particles Lib, GuideME, FrozenLib, anthology. Player Animation Library is also required on the client, but not on a server.
- Optional: JEI, Jade, YACL.

All textures, models, animations and code of Autarkia are original work.
