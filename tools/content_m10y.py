"""M10 content data, YACL part (10 section 6): names, descriptions, category and section titles of the YACL config screen
(integration.yacl.YaclScreen). Keys follow the full config path; the check fails when a key of the two config specs has
no entry or an entry has no key."""

import re

from datagen import ROOT, name

KEY = "autarkia.configuration."
CONFIG = ROOT / "src" / "main" / "java" / "dev" / "autarkia" / "config"
CATEGORIES = {"power": "Power", "materials": "Materials", "logistics": "Logistics", "colony": "Colony", "workers": "Workers",
              "equipment": "Equipment", "client": "Client"}
SECTIONS = {"memory": "Memory", "pattern": "Patterns", "outpost": "Outposts", "replicator": "Replicators", "nanite": "Nanites",
            "seed": "Autarky Seeds", "restoration": "Restoration", "absorb": "Explosion Absorb", "aegis": "Aegis", "reform": "Nanite Reform",
            "bastion": "Bastion", "tools": "Tools", "weapons": "Weapons", "forge": "Forge of Ascension", "render": "Rendering", "hud": "HUD",
            "fx": "Effects"}

OPTIONS = {
    "power.gridTickBudgetMicros": ("Grid tick budget", "Microseconds that all power networks of one level can use per tick. Networks that do not fit wait for the next tick."),
    "power.gridRebuildsPerTick": ("Grid rebuilds per tick", "Maximum number of network rebuilds per level tick after blocks or spans change."),
    "power.spanSyncInterval": ("Span sync interval", "Ticks between span state updates (heat, load, temperature) to clients."),
    "power.cableHeatMultiplier": ("Cable heat multiplier", "Scales the heat that overloaded cables gain."),
    "power.cableCoolingMultiplier": ("Cable cooling multiplier", "Scales how fast hot cables cool down."),
    "power.ropeWearMultiplier": ("Rope wear multiplier", "Scales the wear of overloaded rope spans. 0 turns rope wear off."),
    "power.cableFireChanceMultiplier": ("Cable fire chance multiplier", "Scales the chance that a snapped cable sets fire to blocks near its tip. 0 turns these fires off."),
    "power.snappedCableDamageMultiplier": ("Snapped cable damage multiplier", "Scales the damage that a snapped cable does to entities."),
    "power.windReference": ("Wind reference", "Wind speed that counts as strength 1.0 for windmills, wind towers, HV sway and gondolas. A lower value makes the same wind stronger."),
    "power.boilerBurstEnabled": ("Boiler bursts", "When on, a boiler bursts at the burst pressure or after a long dry fire."),
    "power.boilerBurstPressure": ("Boiler burst pressure", "Pressure in bar at which a boiler bursts."),
    "power.quenchBreaksBlocks": ("Quench breaks blocks", "When on, the quench explosions of superconducting cables and coils break blocks."),
    "power.flywheelReleaseMinFE": ("Flywheel release minimum", "Minimum stored FE for a broken stone flywheel to roll away as an entity."),
    "power.rollingFlywheelBreaksBlocks": ("Rolling flywheel breaks blocks", "When on, a fast rolling flywheel breaks soft blocks that it hits."),
    "power.lightningPulseSpeed": ("Lightning pulse speed", "Speed in blocks per tick of a lightning pulse along cable spans."),
    "power.lightningSurgeEnabled": ("Lightning surges", "When on, lightning that hits the grid sends a surge pulse through the network."),
    "power.spanAttractRadius": ("Span attract radius", "Horizontal radius in blocks in which a lightning bolt moves to the highest span node. 0 turns it off."),
    "power.lightningMastRadius": ("Lightning mast radius", "Horizontal radius in blocks in which natural bolts move to the nearest lightning mast."),
    "power.lightningMastStrikeChance": ("Lightning mast strike chance", "During thunder each valid mast calls a strike with a chance of 1 in this value per tick."),
    "power.lightningMastStrikeFE": ("Lightning mast strike energy", "FE that one strike puts into the capacitor banks of the mast network."),
    "power.fuseResetTicks": ("Fuse reset time", "Ticks until a blown fuse resets itself."),
    "power.singularityFePerExplosionRadiusSq": ("Singularity explosion gain", "FE that a running singularity core gains per explosion in its domain, times the explosion radius squared."),
    "materials.processing.energyMultiplier": ("Processing energy multiplier", "Scales the FE cost of all processing machines."),
    "materials.processing.speedMultiplier": ("Processing speed multiplier", "Scales the work speed of all processing machines."),
    "materials.quern.turnsPerItem": ("Quern turns per item", "Crank turns that a quern needs for one item."),
    "materials.crucible.campfireSpeed": ("Crucible speed on a campfire", "Melt speed of a crucible over a campfire."),
    "materials.crucible.furnaceSpeed": ("Crucible speed on a furnace", "Melt speed of a crucible over a lit furnace."),
    "materials.crucible.lavaSpeed": ("Crucible speed on lava", "Melt speed of a crucible over lava."),
    "materials.sluice.sourceWaterSpeed": ("Sluice speed with source water", "Work speed of a sluice that has only a water source block. Flowing water gives speed 1.0."),
    "materials.flotation.pumpFromSource": ("Flotation pump from source", "When on, a froth flotation plant pumps water from a source block that touches its bottom. The source stays."),
    "materials.geode.chargedShellGrowthChance": ("Charged geode growth chance", "Chance per random tick that a charged lattice geode shell grows a lattice cluster."),
    "materials.worldgen.outcropsEnabled": ("Ore outcrops", "When on, ore outcrops generate in new chunks."),
    "materials.matter.rawOreMultiplierT5": ("Raw ore matter multiplier (T5)", "Matter value multiplier of raw ores in a domain below T6."),
    "materials.matter.rawOreMultiplierT6": ("Raw ore matter multiplier (T6)", "Matter value multiplier of raw ores in a T6 domain."),
    "materials.matter.compilerMuPerTick": ("Compiler speed", "Matter units per tick that a matter compiler processes. A higher value makes compiling faster."),
    "logistics.chuteSpeedPu": ("Chute speed", "Item speed in chutes, in path units per tick (1,000 pu = 1 block)."),
    "logistics.beltSpeedPu": ("Belt speed", "Item speed on belts, in path units per tick (1,000 pu = 1 block)."),
    "logistics.beltMaxLineLength": ("Belt max line length", "Maximum number of belt blocks in one line."),
    "logistics.beltFePerBlock": ("Belt energy per block", "FE per tick that each powered belt block uses."),
    "logistics.jamWarningTicks": ("Jam warning time", "Ticks that a belt can stay blocked before it reports a jam."),
    "logistics.tubeSpeedPu": ("Capsule tube speed", "Capsule speed in tubes, in path units per tick (1,000 pu = 1 block)."),
    "logistics.tubeMaxNetworkTubes": ("Max tubes per network", "Maximum number of capsule tubes in one network."),
    "logistics.tubeMaxCapsules": ("Max capsules per network", "Maximum number of capsules that travel in one tube network."),
    "logistics.cableCarMaxLength": ("Cable car max length", "Maximum horizontal distance in blocks between two cable car stations."),
    "logistics.gondolaWindFactor": ("Gondola wind factor", "How strongly the wind swings a gondola."),
    "logistics.gondolaHoldDegrees": ("Gondola hold angle", "Swing angle in degrees at which a gondola stops and waits for calmer wind."),
    "logistics.launcherMaxRange": ("Crate launcher max range", "Maximum distance in blocks of a crate launcher shot."),
    "logistics.launcherCatchRadius": ("Crate launcher catch radius", "Distance in blocks at which a target catches a launched crate."),
    "logistics.launcherMaxInFlight": ("Crates in flight", "Maximum number of launched crates in the air in one dimension."),
    "logistics.magRailMaxRails": ("Max rails per mag rail line", "Maximum number of rails in one mag rail line."),
    "logistics.magRailMaxSleds": ("Max sleds per mag rail line", "Maximum number of sleds on one mag rail line."),
    "logistics.conduitItemsPerTick": ("Matter conduit rate", "Maximum number of items that a matter conduit moves per tick."),
    "logistics.rackCollapseEnabled": ("Rack collapse", "When on, overloaded racks bend and then collapse."),
    "logistics.rackBendThreshold": ("Rack bend threshold", "Load ratio above which a rack starts to bend."),
    "logistics.rackCriticalThreshold": ("Rack critical threshold", "Load ratio above which a rack is critical and collapses after the collapse time."),
    "logistics.rackCollapseTicks": ("Rack collapse time", "Ticks that a critical rack holds before it collapses."),
    "logistics.rackImpactThreshold": ("Rack impact threshold", "Load ratio with impact load at which a rack collapses at once."),
    "logistics.fallingCrateCap": ("Falling crate limit", "Maximum number of falling crates in one dimension. Above it, a collapsing crate lands at once as a crate block."),
    "logistics.warehouseRadius": ("Warehouse radius", "Radius in blocks that a warehouse controller serves."),
    "logistics.warehouseQueueSize": ("Warehouse queue size", "Maximum number of open requests in one warehouse controller."),
    "logistics.matterBufferCapacity": ("Matter buffer capacity", "Matter units that one matter buffer holds."),
    "logistics.matterReserve": ("Matter reserve", "When the free matter of a domain is below this value, matter buffers dissolve items from the dissolvable tag."),
    "logistics.stockSyncIntervalTicks": ("Stock sync interval", "Ticks between stock updates of depots, tally boards and warehouses to clients."),
    "colony.colony.enabled": ("Colonies", "Kill switch. When off, all colony planners and jobs stop."),
    "colony.colony.maxColoniesPerPlayer": ("Colonies per player", "Maximum number of colonies that one player can own."),
    "colony.colony.starterPorter": ("Starter Porter", "When on, a new colony core gives a worker capsule with one Porter."),
    "colony.colony.starterManual": ("Starter Field Manual", "When on, each player gets one Field Manual on the first join of a world."),
    "colony.colony.sensorPeriodTicks": ("Sensor period", "Ticks between two colony sensor samples."),
    "colony.colony.plannerPeriodTicks": ("Planner period", "Ticks between two planner runs."),
    "colony.colony.emaAlpha": ("Sensor smoothing", "Weight of a new sensor sample in the moving average. 1.0 uses only the newest sample."),
    "colony.colony.maxOpenProposals": ("Open proposals", "Maximum number of open project proposals per colony."),
    "colony.colony.assistedDelayTicks": ("Assisted delay", "Colony clock ticks after which the Assisted mode approves a proposal itself."),
    "colony.colony.budgetMultiplier": ("Budget multiplier", "Scales the daily build budget of a colony."),
    "colony.colony.siteEvalsPerTick": ("Site checks per tick", "Number of candidate build sites that the planner checks per tick."),
    "colony.colony.jobAssignPerPass": ("Job assignments per pass", "Maximum number of jobs given to workers in one pass."),
    "colony.colony.jobRetryLimit": ("Job retry limit", "Failed attempts after which a job and its project are blocked."),
    "colony.colony.signs": ("Project signs", "When on, the colony puts a sign at finished automatic projects."),
    "colony.colony.ghostMaxBlocks": ("Ghost preview blocks", "Maximum number of ghost blocks in one project preview. The blocks nearest to the player go first."),
    "memory.memory.halfLifeTicks": ("Memory half-life", "Colony clock ticks after which the weight of a memory entry is half."),
    "memory.memory.maxEntries": ("Memory size", "Maximum number of entries in the memory of one colony."),
    "pattern.pattern.scanCellsPerTick": ("Pattern scan rate", "Cells per tick that a pattern scan reads."),
    "pattern.pattern.repairScanCellsPerTick": ("Pattern repair scan rate", "Cells per tick that a pattern repair scan reads."),
    "pattern.pattern.repairScanPeriodTicks": ("Pattern repair scan period", "Ticks between two repair scans of a built pattern."),
    "outpost.outpost.chunkTickets": ("Outpost chunk tickets", "Kill switch. When on, each outpost core keeps its own chunk loaded with one ticket. When off, unloaded outposts get only catch-up credit."),
    "outpost.outpost.max": ("Outposts per colony", "Maximum number of outposts per colony. Active replicator walkers count too."),
    "outpost.outpost.maxDistance": ("Outpost max distance", "Maximum horizontal distance in blocks from the colony core to an outpost."),
    "outpost.outpost.minSpacing": ("Outpost min spacing", "Minimum horizontal distance in blocks from the colony core and from other outposts."),
    "outpost.outpost.catchUpCapTicks": ("Outpost catch-up cap", "Maximum number of unloaded ticks that an outpost gets credit for."),
    "outpost.outpost.catchUpEfficiency": ("Outpost catch-up efficiency", "Part of the normal output that an outpost gets for unloaded time."),
    "outpost.outpost.hvLinkLamps": ("HV link lamps", "When on, each pylon of an outpost HV link gets an industrial lamp beside its base."),
    "replicator.replicator.enabled": ("Replicators", "Kill switch. When off, replicator foundries and replicator walkers stop."),
    "replicator.replicator.maxPerDomain": ("Walkers per domain", "Maximum number of active replicator walkers per domain."),
    "nanite.nanite.enabled": ("Nanites", "Kill switch. When off, all nanite claims pause at once."),
    "nanite.nanite.globalEditsPerTick": ("Nanite edits per tick", "Cell edits per tick for all nanite claims on the server together."),
    "nanite.nanite.maxClaimXZ": ("Nanite claim width", "Maximum width and length in blocks of one nanite claim."),
    "nanite.nanite.maxClaimY": ("Nanite claim height", "Maximum height in blocks of one nanite claim."),
    "nanite.nanite.feBase": ("Nanite base energy", "Base FE cost of one nanite cell edit."),
    "nanite.nanite.fePerHardness": ("Nanite energy per hardness", "Added FE per point of block hardness for one nanite cell edit."),
    "nanite.nanite.feCap": ("Nanite energy cap", "Maximum FE cost of one nanite cell edit."),
    "nanite.nanite.maxStreamsPerTick": ("Nanite ore streams per tick", "Maximum number of ore stream effects that nanites start per server tick."),
    "seed.seed.enabled": ("Autarky seeds", "Kill switch. When off, autarky seeds do not land or unpack."),
    "seed.seed.targetRiseTicks": ("Seed rise time", "Target number of ticks for all blocks of a seed to rise into place."),
    "seed.seed.maxRisingPerTick": ("Rising blocks per tick", "Maximum number of rising blocks that a seed spawns per tick."),
    "seed.seed.maxRisingEntities": ("Rising block limit", "Maximum number of rising block entities at the same time."),
    "restoration.restoration.enabled": ("Restoration", "Kill switch. When off, restoration projectors do not restore blocks."),
    "restoration.restoration.cellsPerTickPerProjector": ("Restoration rate per projector", "Cells per tick that one restoration projector restores."),
    "restoration.restoration.globalCellsPerTick": ("Restoration rate per server", "Cells per tick that all restoration projectors restore together."),
    "restoration.restoration.scanCellsPerTick": ("Restoration scan rate", "Cells per tick that restoration projectors read for snapshots and for lost blocks."),
    "restoration.restoration.fePerBlock": ("Restoration energy per block", "FE cost of each restored block."),
    "absorb.absorb.enabled": ("Explosion absorb", "Kill switch. When off, singularity cores of T6 colonies do not absorb explosions in their domain."),
    "workers.workerBlockEdits": ("Worker block edits", "Kill switch. When off, workers do no place and break steps."),
    "workers.workerCapMultiplier": ("Worker cap multiplier", "Scales all worker caps (rounded down, at least 1)."),
    "workers.workersDamageableByNonMembers": ("Workers take PvP damage", "When on, players who are not colony members can damage workers."),
    "workers.pathBudgetPerDomain": ("Path budget per domain", "New worker paths per tick for each domain."),
    "workers.idleBrainInterval": ("Idle brain interval", "Ticks between brain updates of the idle and rest behaviours."),
    "workers.maxTravelTickets": ("Travel tickets", "Chunk tickets per domain for workers that travel far."),
    "workers.dockChargeRate": ("Dock charge rate", "FE per tick that a recharge dock gives a worker."),
    "workers.windingFePerTension": ("Winding energy per tension", "FE per tension unit at a winding post. The minimum of 2 prevents energy loops."),
    "workers.naniteBlocksPerSecond": ("Nanite swarm speed", "Blocks per second that one nanite swarm edits, inside the global nanite budget."),
    "workers.sentinelArcDamage": ("Sentinel arc damage", "Damage of one Sentinel arc shot."),
    "workers.stuckAlerts": ("Stuck alerts", "When on, a stuck worker gives a level 4 alert on the project board."),
    "aegis.aegis.fePerDamage": ("Aegis energy per damage", "FE that the Aegis pays per damage point (half a heart)."),
    "aegis.aegis.tetherRange": ("Aegis tether range", "Maximum distance in blocks from a colony domain at which the Aegis tether stays connected."),
    "aegis.aegis.repeatWindowTicks": ("Aegis repeat window", "Ticks after a payment in which the same damage type costs only the difference to the larger amount."),
    "reform.reform.matterCost": ("Reform matter cost", "Matter units that one Nanite Reform costs."),
    "reform.reform.cooldownSeconds": ("Reform cooldown", "Seconds before the Nanite Reform can save the player again."),
    "reform.reform.offsetBlocks": ("Reform offset", "Distance in blocks from the damage source at which the player forms again."),
    "bastion.bastion.feReserve": ("Bastion energy reserve", "Minimum stored grid FE for bastion blocks to stay unbreakable."),
    "tools.tools.maxFellLogs": ("Max felled logs", "Maximum number of logs that one tree fell breaks."),
    "tools.tools.maxVeinBlocks": ("Max vein blocks", "Maximum number of ore blocks that one vein mine breaks."),
    "tools.tools.matterStreamRange": ("Matter stream range", "Maximum distance in blocks over which tools stream drops to their matter link."),
    "weapons.weapons.lanceAllowTerrain": ("Lance breaks terrain", "When on, the singularity lance breaks blocks (only with the mobGriefing game rule)."),
    "weapons.weapons.disassemblerMaxResistance": ("Disassembler max resistance", "Highest explosion resistance of a block that the disassembler beam can take apart."),
    "forge.forge.feTotal": ("Forge energy", "Stored FE that the forge network needs to start an ascension fitting."),
    "render.spanRenderDistance": ("Span render distance", "Distance in blocks up to which cable spans render."),
    "render.cableParticles": ("Cable particles", "When on, hot and overloaded cables show particles."),
    "render.clientNaniteParticles": ("Nanite particles", "Swarm particles per tick. 0 turns them off."),
    "render.showLoadTooltip": ("Load tooltip", "When on, item tooltips show the load units and the matter value with Shift held."),
    "hud.enabled": ("Equipment HUD", "When on, the equipment HUD shows."),
    "hud.scale": ("HUD scale", "Size of the equipment HUD."),
    "fx.shakeScale": ("Camera shake scale", "Strength of camera shakes. 0 turns them off."),
}


def spec_paths():
    """Return the full path of every value defined in the two config spec classes."""
    paths = []
    for f in ("AutarkiaConfig.java", "AutarkiaClientConfig.java"):
        section = None
        for line in (CONFIG / f).read_text(encoding="utf-8").splitlines():
            if m := re.search(r'b\.push\("([^"]+)"\)', line):
                section = m.group(1)
            elif m := re.search(r'b\.define(?:InRange)?\("([^"]+)"', line):
                paths.append(f"{section}.{m.group(1)}")
    return paths


def generate():
    paths = spec_paths()
    missing = sorted(set(paths) - OPTIONS.keys())
    extra = sorted(OPTIONS.keys() - set(paths))
    if missing or extra:
        raise SystemExit(f"content_m10y: config keys without YACL text {missing}; YACL texts without config key {extra}")
    for path, (title, desc) in OPTIONS.items():
        name(KEY + path, title)
        name(KEY + path + ".tooltip", desc)
    for k, v in CATEGORIES.items():
        name(f"{KEY}category.{k}", v)
    for k, v in SECTIONS.items():
        name(f"{KEY}section.{k}", v)
    name(f"{KEY}yacl.range", "Range: %s to %s. Default: %s.")
    name(f"{KEY}yacl.default", "Default: %s.")
    print(f"  YACL config screen text: {len(OPTIONS)} options, {len(CATEGORIES)} categories, {len(SECTIONS)} sections")
