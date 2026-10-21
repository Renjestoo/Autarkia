"""Build GeckoLib (Bedrock 1.12.0) models: bones, cubes, per-face UV atlases and animations.

Coordinates are model pixels in the geo.json file frame. GeckoLib mirrors X on load (render x = -file x,
bone and cube rotations about X and Y are negated, animation X positions are negated). Face names are
GeckoLib directions in render space, so the "east" face of a cube lies on the cube's file -X side.
Patch orientation as seen from outside the face: side faces have their top row at the top; the "up"
patch has its top row on the south edge and its left column on the render-east edge.
"""

import importlib
import json
import math
import re
from pathlib import Path

import numpy as np
from PIL import Image

from art_spec import GEO_BLOCKS
from paint import ASSETS, new, rng_for

LOOP = {"loop": True, "once": False, "hold": "hold_on_last_frame"}
NUM_LIST = re.compile(r"\[\s*(-?[\d.eE+-]+(?:\s*,\s*-?[\d.eE+-]+)*)\s*\]")
FACES = ("north", "south", "east", "west", "up", "down")


def num(v: float):
    """Return v rounded to 4 decimals, as int when integral (clean JSON)."""
    r = round(float(v), 4)
    return int(r) if r == int(r) else r


def vec(v) -> list:
    """Return a clean JSON vector."""
    return [num(x) for x in v]


def face_dims(size, face: str) -> tuple[int, int]:
    """Return the (width, height) in texels of one cube face."""
    sx, sy, sz = (int(round(s)) for s in size)
    if face in ("north", "south"):
        return sx, sy
    if face in ("east", "west"):
        return sz, sy
    return sx, sz


def face_skipped(size, face: str) -> bool:
    """Return True when GeckoLib drops this face because the cube is flat along another axis."""
    sx, sy, sz = size
    axis = {"east": 0, "west": 0, "up": 1, "down": 1, "north": 2, "south": 2}[face]
    if sx == 0:
        return axis != 0
    if sy == 0:
        return axis != 1
    if sz == 0:
        return axis != 2
    return False


class Anim:
    """Keyframes of one animation; channels are rotation (degrees), position (pixels) and scale."""

    def __init__(self, loop: str, length: float):
        if loop not in LOOP:
            raise ValueError(f"loop type '{loop}' must be one of {sorted(LOOP)}")
        self.loop, self.length, self.bones = loop, float(length), {}

    def key(self, bone: str, channel: str, t: float, value, easing: str | None = None) -> "Anim":
        """Set one keyframe; t is clamped to [0, length]."""
        if channel not in ("rotation", "position", "scale"):
            raise ValueError(f"channel '{channel}' is not rotation, position or scale")
        t = min(max(0.0, float(t)), self.length)
        self.bones.setdefault(bone, {}).setdefault(channel, {})[round(t, 4)] = (tuple(float(x) for x in value), easing)
        return self

    def keys(self, bone: str, channel: str, frames) -> "Anim":
        """Set several (t, value) keyframes."""
        for t, v in frames:
            self.key(bone, channel, t, v)
        return self

    def spin(self, bone: str, axis: str, turns: float = 1.0, base=(0.0, 0.0, 0.0), t0: float = 0.0,
             t1: float | None = None, steps: int = 8) -> "Anim":
        """Rotate linearly about one axis by turns * 360 degrees between t0 and t1 (default: whole length)."""
        t1 = self.length if t1 is None else t1
        i = "xyz".index(axis)
        for s in range(steps + 1):
            v = list(base)
            v[i] += 360.0 * turns * s / steps
            self.key(bone, "rotation", t0 + (t1 - t0) * s / steps, v)
        return self

    def osc(self, bone: str, channel: str, amp, base=None, cycles: float = 1.0, phase: float = 0.0,
            steps: int = 8, t0: float = 0.0, t1: float | None = None) -> "Anim":
        """Add a sine oscillation base + amp * sin(2 pi (cycles * u + phase)) sampled at steps + 1 keys."""
        t1 = self.length if t1 is None else t1
        if base is None:
            base = (1.0, 1.0, 1.0) if channel == "scale" else (0.0, 0.0, 0.0)
        for s in range(steps + 1):
            u = s / steps
            k = math.sin(2 * math.pi * (cycles * u + phase))
            self.key(bone, channel, t0 + (t1 - t0) * u, [b + a * k for b, a in zip(base, amp)])
        return self

    def sample(self, t: float) -> dict:
        """Return {bone: {channel: (x, y, z)}} linearly interpolated at time t (file conventions)."""
        out = {}
        for bone, chans in self.bones.items():
            for ch, frames in chans.items():
                ts = sorted(frames)
                if t <= ts[0]:
                    v = frames[ts[0]][0]
                elif t >= ts[-1]:
                    v = frames[ts[-1]][0]
                else:
                    j = next(k for k in range(len(ts) - 1) if ts[k] <= t <= ts[k + 1])
                    a, b = ts[j], ts[j + 1]
                    u = (t - a) / (b - a) if b > a else 0.0
                    va, vb = frames[a][0], frames[b][0]
                    v = tuple(x + (y - x) * u for x, y in zip(va, vb))
                out.setdefault(bone, {})[ch] = v
        return out

    def to_json(self) -> dict:
        """Return the Bedrock animation object."""
        bones = {}
        for bone, chans in self.bones.items():
            bj = {}
            for ch in ("rotation", "position", "scale"):
                if ch not in chans:
                    continue
                frames = {}
                for t in sorted(chans[ch]):
                    v, easing = chans[ch][t]
                    frames[repr(float(t))] = {"vector": vec(v), "easing": easing} if easing else vec(v)
                bj[ch] = frames
            bones[bone] = bj
        return {"loop": LOOP[self.loop], "animation_length": num(self.length), "bones": bones}


class Model:
    """A GeckoLib model under construction: ordered bones with cubes plus named animations."""

    def __init__(self, ident: str, prefix: str | None = None):
        self.ident = ident
        self.prefix = prefix or ident
        self.bones: list[dict] = []
        self.index: dict[str, dict] = {}
        self.anims: dict[str, Anim] = {}

    def bone(self, name: str, parent: str | None = None, pivot=(0, 0, 0), rotation=None) -> str:
        """Add a bone; pivot and rotation are in file coordinates and degrees."""
        if name in self.index:
            raise ValueError(f"{self.ident}: duplicate bone '{name}'")
        if parent is not None and parent not in self.index:
            raise ValueError(f"{self.ident}: bone '{name}' has unknown parent '{parent}'")
        b = {"name": name, "parent": parent, "pivot": tuple(float(p) for p in pivot),
             "rotation": None if rotation is None else tuple(float(r) for r in rotation), "cubes": []}
        self.bones.append(b)
        self.index[name] = b
        return name

    def box(self, bone: str, origin, size, mat: str, faces: dict | None = None, rotation=None, pivot=None,
            inflate: float | None = None) -> None:
        """Add a cube with integer size; mat is 'palette:finish[:arg]', faces overrides mat per face."""
        if any(abs(s - round(s)) > 1e-6 or s < 0 for s in size):
            raise ValueError(f"{self.ident}/{bone}: cube size {size} must be non-negative integers")
        if sum(1 for s in size if round(s) == 0) > 1:
            raise ValueError(f"{self.ident}/{bone}: cube size {size} may be flat along one axis only")
        cube = {"origin": tuple(float(o) for o in origin), "size": tuple(int(round(s)) for s in size), "mat": mat,
                "faces": dict(faces or {}), "rotation": None if rotation is None else tuple(float(r) for r in rotation),
                "pivot": None if pivot is None else tuple(float(p) for p in pivot), "inflate": inflate}
        self.index[bone]["cubes"].append(cube)

    def cbox(self, bone: str, center, size, mat: str, **kw) -> None:
        """Add a cube centred on (cx, cz) in X/Z whose bottom sits at y = center[1]."""
        cx, y, cz = center
        self.box(bone, (cx - size[0] / 2.0, y, cz - size[2] / 2.0), size, mat, **kw)

    def anim(self, name: str, loop: str, length: float) -> Anim:
        """Create (or replace) the animation '<prefix>.<name>'."""
        a = Anim(loop, length)
        self.anims[name] = a
        return a

    def bounds(self) -> tuple[np.ndarray, np.ndarray]:
        """Return the min and max corners of all cubes in file coordinates (ignoring rotations)."""
        pts = []
        for b in self.bones:
            for c in b["cubes"]:
                o, s = np.array(c["origin"]), np.array(c["size"], dtype=float)
                pts += [o, o + s]
        if not pts:
            return np.zeros(3), np.zeros(3)
        arr = np.array(pts)
        return arr.min(axis=0), arr.max(axis=0)


def builder(group: str, ident: str):
    """Return the model builder for a spec id; group is block, rig, entity, item or armor.

    Block builders live in geo_blocks_t<tier>.py; the others in geo_rigs, geo_entities, geo_items and
    geo_armor. Each module exposes MODELS = {id: callable() -> Model}.
    """
    module = {"rig": "geo_rigs", "entity": "geo_entities", "item": "geo_items", "armor": "geo_armor"}.get(group)
    if group == "block":
        module = f"geo_blocks_t{GEO_BLOCKS[ident]['tier']}"
    models = importlib.import_module(module).MODELS
    if ident not in models:
        raise KeyError(f"{module}.MODELS has no builder for '{ident}'")
    return models[ident]


def footprint_px(footprint) -> tuple[float, float, float]:
    """Return the model box (W, H, D) in pixels for a block footprint ((minX,minY,minZ),(maxX,maxY,maxZ)).

    The model spans x in [-W/2, W/2], y in [0, H], z in [-D/2, D/2] around its origin.
    """
    (x0, y0, z0), (x1, y1, z1) = footprint
    return (x1 - x0 + 1) * 16.0, (y1 - y0 + 1) * 16.0, (z1 - z0 + 1) * 16.0


def mat_parts(mat: str) -> tuple[str, str, str]:
    """Split 'palette:finish[:arg]' into its three parts (arg may be '')."""
    parts = mat.split(":")
    if len(parts) < 2:
        raise ValueError(f"material '{mat}' must look like 'palette:finish[:arg]'")
    return parts[0], parts[1], ":".join(parts[2:])


def collect_faces(model: Model):
    """Yield (bone, cube, face, mat, w, h) for every rendered face in a stable order."""
    for b in model.bones:
        for c in b["cubes"]:
            for f in FACES:
                if face_skipped(c["size"], f):
                    continue
                w, h = face_dims(c["size"], f)
                yield b, c, f, c["faces"].get(f, c["mat"]), w, h


def pack(sizes: list[tuple[int, int]]) -> tuple[list[tuple[int, int]], int, int]:
    """Shelf-pack (w, h) rectangles; return positions in input order and power-of-two atlas size."""
    order = sorted(range(len(sizes)), key=lambda i: (-sizes[i][1], -sizes[i][0], i))
    area = sum(w * h for w, h in sizes)
    width = 16
    while width * width < area or width < max((w for w, _ in sizes), default=1):
        width *= 2
    while True:
        pos, x, y, shelf = [None] * len(sizes), 0, 0, 0
        for i in order:
            w, h = sizes[i]
            if x + w > width:
                x, y, shelf = 0, y + shelf, 0
            pos[i] = (x, y)
            x += w
            shelf = max(shelf, h)
        used = y + shelf
        height = 16
        while height < used:
            height *= 2
        if height <= width * 2:
            return pos, width, height
        width *= 2


def build_atlas(model: Model, painter, key_extra: str = "") -> tuple[np.ndarray, dict]:
    """Paint and pack one patch per distinct (mat, side/top, w, h); return atlas and face UV map.

    Args:
        painter: callable(mat, group, w, h, rng) -> RGBA array of shape (h, w, 4).

    Returns:
        (atlas, uvs) where uvs[(id(cube), face)] = (u, v, w, h).
    """
    keys, sizes, patches = {}, [], []
    face_keys = []
    for b, c, f, mat, w, h in collect_faces(model):
        group = "top" if f in ("up", "down") else "side"
        k = (mat, group, w, h)
        if k not in keys:
            patch = painter(mat, group, w, h, rng_for(model.ident, key_extra, *k))
            if patch.shape != (h, w, 4):
                raise ValueError(f"{model.ident}: painter for '{mat}' returned {patch.shape}, expected {(h, w, 4)}")
            keys[k] = len(patches)
            patches.append(patch)
            sizes.append((w, h))
        face_keys.append(((id(c), f), keys[k]))
    pos, aw, ah = pack(sizes)
    atlas = new(aw, ah)
    for p, (x, y), (w, h) in zip(patches, pos, sizes):
        atlas[y:y + h, x:x + w] = p
    uvs = {fk: (pos[i][0], pos[i][1], sizes[i][0], sizes[i][1]) for fk, i in face_keys}
    return atlas, uvs


def geo_json(model: Model, uvs: dict, aw: int, ah: int) -> dict:
    """Return the geo.json document for a model whose faces were packed by build_atlas."""
    lo, hi = model.bounds()
    reach = max(abs(lo[0]), abs(hi[0]), abs(lo[2]), abs(hi[2]), 8.0)
    desc = {"identifier": f"geometry.{model.ident}", "texture_width": aw, "texture_height": ah,
            "visible_bounds_width": num(math.ceil(2 * reach / 16.0) + 1),
            "visible_bounds_height": num(math.ceil((hi[1] - min(lo[1], 0)) / 16.0) + 1),
            "visible_bounds_offset": [0, num((hi[1] + min(lo[1], 0)) / 32.0), 0]}
    bones = []
    for b in model.bones:
        bj = {"name": b["name"]}
        if b["parent"]:
            bj["parent"] = b["parent"]
        bj["pivot"] = vec(b["pivot"])
        if b["rotation"]:
            bj["rotation"] = vec(b["rotation"])
        cubes = []
        for c in b["cubes"]:
            cj = {"origin": vec(c["origin"]), "size": vec(c["size"])}
            if c["rotation"]:
                cj["pivot"] = vec(c["pivot"] if c["pivot"] else np.array(c["origin"]) + np.array(c["size"]) / 2.0)
                cj["rotation"] = vec(c["rotation"])
            if c["inflate"]:
                cj["inflate"] = num(c["inflate"])
            uv = {}
            for f in FACES:
                if face_skipped(c["size"], f):
                    continue
                u, v, w, h = uvs[(id(c), f)]
                uv[f] = {"uv": [u, v], "uv_size": [w, h]}
            cj["uv"] = uv
            cubes.append(cj)
        if cubes:
            bj["cubes"] = cubes
        bones.append(bj)
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": desc, "bones": bones}]}


def anim_json(model: Model) -> dict:
    """Return the animation.json document of a model."""
    return {"format_version": "1.8.0",
            "animations": {f"animation.{model.prefix}.{n}": a.to_json() for n, a in model.anims.items()}}


def write_json(doc: dict, path: Path) -> Path:
    """Write JSON deterministically (UTF-8, LF, sorted insertion order preserved)."""
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(doc, indent=1, ensure_ascii=False)
    text = NUM_LIST.sub(lambda m: "[" + ", ".join(s.strip() for s in m.group(1).split(",")) + "]", text)
    path.write_text(text + "\n", encoding="utf-8", newline="\n")
    return path


def geo_paths(group: str, ident: str, texture: str) -> dict[str, Path]:
    """Return the geo, animation and texture paths of a model in the generated assets tree."""
    return {"geo": ASSETS / "geckolib" / "models" / group / f"{ident}.geo.json",
            "anim": ASSETS / "geckolib" / "animations" / group / f"{ident}.animation.json",
            "tex": ASSETS / "textures" / texture}


def load_model(path_geo: Path, path_anim: Path | None = None, prefix: str | None = None) -> tuple[Model, dict]:
    """Rebuild a Model and its face UV map from written geo/animation JSON (materials become '?')."""
    g = json.loads(path_geo.read_text(encoding="utf-8"))["minecraft:geometry"][0]
    ident = g["description"]["identifier"].removeprefix("geometry.")
    m = Model(ident, prefix)
    uvs = {}
    for bj in g["bones"]:
        m.bone(bj["name"], bj.get("parent"), bj.get("pivot", (0, 0, 0)), bj.get("rotation"))
        for cj in bj.get("cubes", []):
            m.box(bj["name"], cj["origin"], cj["size"], "?:?", rotation=cj.get("rotation"), pivot=cj.get("pivot"),
                  inflate=cj.get("inflate"))
            c = m.index[bj["name"]]["cubes"][-1]
            for f, d in cj["uv"].items():
                uvs[(id(c), f)] = (d["uv"][0], d["uv"][1], d["uv_size"][0], d["uv_size"][1])
    if path_anim is not None:
        doc = json.loads(path_anim.read_text(encoding="utf-8"))["animations"]
        inv = {v: k for k, v in LOOP.items()}
        for full, aj in doc.items():
            a = m.anim(full.split(".")[-1], inv[aj["loop"]], aj["animation_length"])
            for bn, chans in aj["bones"].items():
                for ch, frames in chans.items():
                    for t, val in frames.items():
                        v = val["vector"] if isinstance(val, dict) else val
                        a.key(bn, ch, float(t), v, val.get("easing") if isinstance(val, dict) else None)
    return m, uvs


def check_model(path_geo: Path, path_anim: Path, bones: list[str], anims, prefix: str, textures: list[Path]) -> list[str]:
    """Re-read written files and return a list of contract violations (empty when valid).

    Args:
        anims: list of animation names, or {name: (loop_type, seconds)} to also check loop and length.
        textures: PNGs that must match the geo texture size exactly.
    """
    errs = []
    try:
        g = json.loads(path_geo.read_text(encoding="utf-8"))
        a = json.loads(path_anim.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as e:
        return [f"{path_geo.name}: unreadable JSON ({e})"]
    geo = g["minecraft:geometry"][0]
    aw, ah = geo["description"]["texture_width"], geo["description"]["texture_height"]
    if aw & (aw - 1) or ah & (ah - 1):
        errs.append(f"{path_geo.name}: atlas {aw}x{ah} is not power-of-two")
    names = {b["name"] for b in geo["bones"]}
    for b in bones:
        if b not in names:
            errs.append(f"{path_geo.name}: missing bone '{b}'")
    for tp in textures:
        try:
            with Image.open(tp) as im:
                if im.size != (aw, ah) or im.mode != "RGBA":
                    errs.append(f"{tp.name}: {im.mode} {im.size}, geo expects RGBA {(aw, ah)}")
        except OSError as e:
            errs.append(f"{tp}: unreadable texture ({e})")
    for b in geo["bones"]:
        if b.get("parent") and b["parent"] not in names:
            errs.append(f"{path_geo.name}: bone '{b['name']}' parent '{b['parent']}' missing")
        for c in b.get("cubes", []):
            for f, d in c["uv"].items():
                u, v = d["uv"]
                w, h = d["uv_size"]
                if u < 0 or v < 0 or u + w > aw or v + h > ah or w <= 0 or h <= 0:
                    errs.append(f"{path_geo.name}: bone '{b['name']}' face {f} uv {d} outside {aw}x{ah}")
    for n in anims:
        full = f"animation.{prefix}.{n}"
        if full not in a["animations"]:
            errs.append(f"{path_anim.name}: missing animation '{full}'")
        else:
            if isinstance(anims, dict):
                loop, secs = anims[n]
                aj = a["animations"][full]
                if aj["loop"] != LOOP[loop] or abs(aj["animation_length"] - secs) > 1e-6:
                    errs.append(f"{path_anim.name}: '{full}' is loop={aj['loop']} length={aj['animation_length']}, "
                                f"spec says {loop} {secs}s")
            for bn in a["animations"][full]["bones"]:
                if bn not in names:
                    errs.append(f"{path_anim.name}: '{full}' animates unknown bone '{bn}'")
            if not a["animations"][full]["bones"]:
                errs.append(f"{path_anim.name}: '{full}' animates nothing")
    return errs
