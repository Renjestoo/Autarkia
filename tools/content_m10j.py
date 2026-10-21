"""M10 content data, JEI and Jade part (10 sections 3 and 4): the worker_assembly category texts, the JEI info texts of the
workers the category shows as tooltips (06 sections 1.2 and 1.3) and of core_upgrade_t5 (05 section 1.2), the battery
line of FE workers (06 section 7.1) and the Jade config name of the rolling_flywheel component (02 section 9.2)."""

from datagen import name

INFO = {
    "porter": "T1 walker, spring 6,000 T (idle 1 T/s, moving 20 T/s, carrying +10 T/s, 40 T per placed block). Carries 1 slot (64 "
              "items) or 1 crate; builds T1 projects, strings rope spans, gives 10 FE/t at a hand crank and 24 FE/t in a treadwheel. "
              "Winds at a winding post. Cap cost 1.",
    "hauler_crab": "T2 crab, spring 12,000 T (idle 1 T/s, moving 24 T/s, carrying +12 T/s). Carries 4 slots; hauls items and collects "
                   "debris (30 T per item). Winds at a winding post. Cap cost 1.",
    "replicator_walker": "T5 walker, FE battery 1,000,000 (idle 100 FE/s, moving 1,000 FE/s). Carries an outpost kit to a far site and "
                         "founds an outpost there; it needs 200,000 FE left on arrival. Cap cost 3. It cannot be caught in a capsule.",
    "nanite_swarm": "T5 swarm, FE buffer 50,000 (50 FE/s), integrity 40. Floats through blocks at 0.3 blocks/tick and takes claimed "
                    "blocks apart (200 + 100 x hardness FE per block) into a 512-item buffer. Cap cost 2. It cannot be caught in a "
                    "capsule.",
    "core_upgrade_t5": "Use on an active tier 4 Core (owner or trusted): after 100 ticks the colony is tier 5. Domain half-size 64, "
                       "worker cap 26, 6 projects, 4 domain beacons of half-size 24, outposts of half-size 32.",
}


def generate():
    print("content_m10j: JEI and Jade")
    name("jei.autarkia.category.worker_assembly", "Worker assembly")
    name("jei.autarkia.worker_assembly.frame", "Frame")
    name("jei.autarkia.worker_assembly.station", "%s, Core T%s")
    print("  worker_assembly category: title, frame tooltip, station line")
    for k, v in INFO.items():
        name(f"jei.autarkia.info.{k}", v)
    print(f"  {len(INFO)} JEI info texts")
    name("jade.autarkia.worker.battery", "Battery %s / %s FE")
    name("config.jade.plugin_autarkia.rolling_flywheel", "Autarkia rolling flywheel")
    print("  Jade worker battery line, rolling_flywheel config name")
    print("content_m10j: done")
