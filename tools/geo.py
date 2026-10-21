"""Generate every GeckoLib model, animation and atlas listed in tools/art_spec.py, then validate them.

Usage: F:/envs/5070_11/python.exe tools/geo.py [id ...]
Without ids everything is regenerated and validated; exits 1 on any contract violation.
"""

import sys

from tqdm import tqdm

from art_spec import ARMOR_BONES, GEO_ARMOR, GEO_BLOCKS, GEO_ENTITIES, GEO_ITEMS, RIGS
from geo_core import anim_json, build_atlas, builder, check_model, geo_paths, geo_json, write_json
from geo_faces import paint_face
from paint import TEXTURES, save_png


def jobs() -> list[tuple]:
    """Return (group, id, texture, bones, anims, prefix) for every model in the spec."""
    out = []
    for i, s in GEO_BLOCKS.items():
        out.append(("block", i, f"block/{i}_geo.png", s["bones"], s["anims"], i))
    for r, s in RIGS.items():
        out.append(("rig", r, None, s["base"] + s["optional"] + s["upgrades"], s["anims"], r))
    for i, s in GEO_ENTITIES.items():
        out.append(("entity", i, f"entity/{i}.png", s["bones"], s["anims"], i))
    for i, s in GEO_ITEMS.items():
        out.append(("item", i, f"item/{i}_geo.png", s["bones"], s["anims"], i))
    for i, s in GEO_ARMOR.items():
        out.append(("armor", i, f"armor/{i}.png", ARMOR_BONES, s["anims"], i))
    return out


def run(job) -> list[str]:
    """Build, write and validate one model; return its contract violations."""
    group, ident, texture, bones, anims, prefix = job
    model = builder(group, ident)()
    if model.ident != ident or model.prefix != prefix:
        return [f"{group}/{ident}: builder returned ident={model.ident} prefix={model.prefix}"]
    if group == "rig":
        from geo_rigs import rig_atlas, write_rig_textures
        atlas, mask, uvs = rig_atlas(model)
        textures = write_rig_textures(ident, atlas, mask)
    else:
        atlas, uvs = build_atlas(model, paint_face)
        textures = [save_png(atlas, TEXTURES / texture)]
    paths = geo_paths("entity" if group == "rig" else group, ident, texture or "")
    write_json(geo_json(model, uvs, atlas.shape[1], atlas.shape[0]), paths["geo"])
    write_json(anim_json(model), paths["anim"])
    return check_model(paths["geo"], paths["anim"], bones, anims, prefix, textures)


def main(argv: list[str]) -> int:
    """Generate the selected (default: all) models and report violations."""
    every = jobs()
    unknown = sorted(set(argv) - {j[1] for j in every})
    if unknown:
        print(f"[geo] unknown ids: {unknown}")
        return 1
    selected = [j for j in every if not argv or j[1] in argv]
    print(f"[geo] generating {len(selected)} models")
    errors = []
    for job in tqdm(selected, desc="geo", unit="model"):
        errors += run(job)
    if errors:
        print(f"[geo] FAILED with {len(errors)} violation(s):")
        for e in errors:
            print("  -", e)
        return 1
    print(f"[geo] ok: {len(selected)} models written and validated")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
