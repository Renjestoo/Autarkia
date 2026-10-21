"""M7 content data, worker part: the T4 workers Surveyor, Cable-Layer Drone and Sentinel (06): worker_assembly recipes
with min tier 4 (06 section 5.1), entity loot (frame 100%, parts 50% rounded down), the workers entity tag, the
sentinel_never_target tag (08b section 16), names, Jade states and jobs, JEI info pages and the advancements
workers/pylon_line and workers/sentinel_kill (06 section 7.3)."""

from content_m6c import ev
from datagen import advancement, name, rl, tag, write_json

WORKERS = (
    ("surveyor", "Surveyor", "drone_frame", 1, [("advanced_circuit", 2), ("servo", 2), ("survey_lens", 1), ("vantite_ingot", 2)], 1600, 150),
    ("cable_layer_drone", "Cable-Layer Drone", "drone_frame", 2, [("advanced_circuit", 2), ("servo", 6), ("vantite_steel_ingot", 4), ("hv_cable_spool", 1)],
     2000, 200),
    ("sentinel", "Sentinel", "drone_frame", 1, [("advanced_circuit", 3), ("servo", 4), ("copper_coil", 6), ("vantite_steel_ingot", 4),
                                                ("capacitor_bank", 1)], 2000, 200),
)
NEVER_TARGET = ("minecraft:villager", "minecraft:wandering_trader", "minecraft:iron_golem", "minecraft:snow_golem")


def assembly(worker, frame, frames, parts, time, fe, min_tier):
    """Write the worker_assembly recipe, the entity loot table (frame 100%, parts 50% rounded down) and the workers tag."""
    write_json(f"data/autarkia/recipe/worker_assembly/{worker}.json", {
        "type": rl("worker_assembly"), "worker": rl(worker), "frame": {"ingredient": rl(frame), "count": frames},
        "parts": [{"ingredient": rl(i), "count": n} for i, n in parts], "time": time, "fe_per_tick": fe, "min_tier": min_tier})
    drops = [(frame, frames)] + [(i, n // 2) for i, n in parts if n // 2 > 0]
    write_json(f"data/autarkia/loot_table/entities/{worker}.json", {"type": "minecraft:entity", "random_sequence": rl(f"entities/{worker}"), "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(i), "modifier": [{"type": "minecraft:set_count", "count": n, "add": False}]}]}
        for i, n in drops]})
    tag("entity_type", "autarkia:workers", rl(worker))


def workers():
    for eid, title, frame, frames, parts, time, fe in WORKERS:
        assembly(eid, frame, frames, parts, time, fe, 4)
        name(f"entity.autarkia.{eid}", title)
        print(f"  worker {eid}: recipe ({frames} x {frame}, {len(parts)} parts, {time} ticks, {fe} FE/t, T4), loot, tag")
    tag("entity_type", "autarkia:sentinel_never_target", *NEVER_TARGET)
    name("jade.autarkia.worker.state.waiting", "Waiting for a travel ticket")
    for j, v in (("survey", "Survey"), ("escort", "Escort"), ("found_outpost", "Found outpost")):
        name(f"jade.autarkia.worker.job.{j}", v)
    info = {
        "surveyor": "T4 drone, FE battery 60,000. Flies a square spiral over a SURVEY area (points 32 blocks apart) and scans 32 blocks around "
                    "each point for 2,000 FE (+16 per range upgrade); every ore outcrop goes to the colony deposit list. Holds a travel ticket "
                    "outside force-loaded chunks.",
        "cable_layer_drone": "T4 drone, FE battery 200,000. Places hv_pylons (5,000 FE each, one pylon per cargo slot, 3 slots) and strings HV "
                             "spans (2,000 FE). Holds a travel ticket outside force-loaded chunks.",
        "sentinel": "T4 drone, FE battery 150,000. Patrols the Core, beacons and outposts and shoots hostiles in the domain (+8 blocks) with "
                    "arcs: 8 damage every 30 ticks for 1,500 FE, range 16 (+4 per range upgrade), creepers first. Escorts outpost carriers. "
                    "Never targets players, villagers, tamed animals or workers.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def advancements():
    advancement("workers/pylon_line", "One Pylon at a Time", "Cable-Layers finish an HV pylon line to an outpost", "hv_pylon",
                parent="workers/drone_repair", criteria=ev("worker_event", "pylon_line_complete"), frame="goal")
    advancement("workers/sentinel_kill", "Arc Watch", "A Sentinel kills a hostile near you", "drone_frame", parent="workers/self_built",
                criteria=ev("worker_event", "sentinel_kill"))
    print("  advancements workers/pylon_line, workers/sentinel_kill")


def generate():
    print("content_m7w: T4 workers")
    workers()
    advancements()
    print("content_m7w: done")
