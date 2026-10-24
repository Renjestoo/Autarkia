"""Collision and outline shapes of the GeckoLib big machines, voxelized from their written geo.json models.

A footprint cell gets the boxes its model fills on a 4 px grid, taken from the rest pose and from the first and last
frame of every animation (so open petals or a lowered ram count, while a spinning rotor keeps its rest blades). Boxes
are cell-local pixels 0..16 for the block facing north; Tiny Multiblock Lib rotates them with the facing. A player can
then reach through the empty parts of a footprint and the selection outline follows the model. Bones named tier_* (and
their children) show only from a colony tier on, so they never count.
"""

import math
from pathlib import Path

import numpy as np

from art_spec import GEO_BLOCKS
from geo_core import geo_paths, load_model
from geo_render import _rot, bone_matrices

STEP = 4


def _poses(model) -> list[dict]:
    """Return the rest pose and the first and last frame of each animation."""
    poses = [{}]
    for anim in model.anims.values():
        poses += [anim.sample(0.0), anim.sample(anim.length)]
    return poses


def _boxes(model, off: np.ndarray, pose: dict) -> list[tuple[np.ndarray, np.ndarray, np.ndarray]]:
    """Return (centre, unit axes as columns, half extents) of every solid cube in the node frame, in pixels."""
    mats = bone_matrices(model, pose)
    parents = {b["name"]: b["parent"] for b in model.bones}
    out = []
    for b in model.bones:
        name = b["name"]
        while name and not name.startswith("tier_"):
            name = parents[name]
        if name:
            continue
        wm = mats[b["name"]]
        for c in b["cubes"]:
            o, s = np.array(c["origin"], dtype=float), np.array(c["size"], dtype=float)
            if (s <= 0).any():
                continue
            inf = c["inflate"] or 0.0
            pts = np.array([(x, y, z) for z in (o[2] - inf, o[2] + s[2] + inf) for y in (o[1] - inf, o[1] + s[1] + inf)
                            for x in (-(o[0] + s[0]) - inf, -o[0] + inf)])
            if c["rotation"]:
                r = c["rotation"]
                cp = c["pivot"] if c["pivot"] else tuple(o + s / 2.0)
                piv = np.array([-cp[0], cp[1], cp[2]])
                pts = (pts - piv) @ _rot(math.radians(-r[0]), math.radians(-r[1]), math.radians(r[2])).T + piv
            pts = pts @ wm[:3, :3].T + wm[:3, 3] + off
            axes = np.stack([pts[1] - pts[0], pts[2] - pts[0], pts[4] - pts[0]], axis=1)
            lens = np.linalg.norm(axes, axis=0)
            if (lens < 0.05).any():
                continue
            out.append((pts.mean(axis=0), axes / lens, lens / 2.0))
    return out


def _fill(centres: np.ndarray, boxes) -> np.ndarray:
    """Return which STEP-sized voxels (given by centre) overlap any oriented box (separating axis test)."""
    e = STEP / 2.0
    filled = np.zeros(len(centres), dtype=bool)
    eye = np.eye(3)
    for c, a, h in boxes:
        rel = centres - c
        sep = np.zeros(len(centres), dtype=bool)
        axes = [eye[i] for i in range(3)] + [a[:, j] for j in range(3)]
        axes += [np.cross(eye[i], a[:, j]) for i in range(3) for j in range(3)]
        for ax in axes:
            n = np.linalg.norm(ax)
            if n < 1e-6:
                continue
            ax = ax / n
            reach = e * np.abs(ax).sum() + float(np.sum(h * np.abs(ax @ a)))
            sep |= np.abs(rel @ ax) >= reach - 1e-6
        filled |= ~sep
    return filled


def _merge(grid: np.ndarray) -> list[tuple[int, int, int, int, int, int]]:
    """Merge a filled voxel grid into boxes (x0, y0, z0, x1, y1, z1) in pixels, greedily along x, then z, then y."""
    left = grid.copy()
    n = grid.shape[0]
    out = []
    for y in range(n):
        for z in range(n):
            for x in range(n):
                if not left[x, y, z]:
                    continue
                x1 = x
                while x1 + 1 < n and left[x1 + 1, y, z]:
                    x1 += 1
                z1 = z
                while z1 + 1 < n and left[x:x1 + 1, y, z1 + 1].all():
                    z1 += 1
                y1 = y
                while y1 + 1 < n and left[x:x1 + 1, y1 + 1, z:z1 + 1].all():
                    y1 += 1
                left[x:x1 + 1, y:y1 + 1, z:z1 + 1] = False
                out.append((x * STEP, y * STEP, z * STEP, (x1 + 1) * STEP, (y1 + 1) * STEP, (z1 + 1) * STEP))
    return out


def machine_cells(ident: str) -> dict[tuple[int, int, int], list[tuple[int, int, int, int, int, int]]]:
    """Return the boxes of every footprint cell of a GeckoLib big machine, keyed by the cell offset (facing north).

    Raises:
        SystemExit: when the centre cell has no box (Tiny Multiblock Lib needs a centre shape for the whole outline).
    """
    (x0, y0, z0), (x1, y1, z1) = GEO_BLOCKS[ident]["footprint"]
    p = geo_paths("block", ident, f"block/{ident}_geo.png")
    model, _ = load_model(p["geo"], p["anim"] if p["anim"].exists() else None, ident)
    off = np.array([(x0 + x1) / 2.0 * 16, 0.0, (z0 + z1) / 2.0 * 16])
    boxes = [b for pose in _poses(model) for b in _boxes(model, off, pose)]
    n = 16 // STEP
    idx = (np.arange(n) + 0.5) * STEP
    cells = {}
    for dx in range(x0, x1 + 1):
        for dy in range(y0, y1 + 1):
            for dz in range(z0, z1 + 1):
                gx, gy, gz = np.meshgrid(idx + dx * 16 - 8, idx + dy * 16, idx + dz * 16 - 8, indexing="ij")
                centres = np.stack([gx.ravel(), gy.ravel(), gz.ravel()], axis=1)
                cells[(dx, dy, dz)] = _merge(_fill(centres, boxes).reshape(n, n, n))
    if not cells[(0, 0, 0)]:
        raise SystemExit(f"{ident}: the centre cell of the model is empty")
    return cells


def big_machines() -> list[str]:
    """Return the GeckoLib blocks whose footprint is larger than one block."""
    return [i for i, spec in GEO_BLOCKS.items() if spec["footprint"][0] != (0, 0, 0) or spec["footprint"][1] != (0, 0, 0)]


def write_java(path: Path) -> None:
    """Write MachineShapes.java: per machine one string of 'dx,dy,dz,x0,y0,z0,x1,y1,z1' boxes separated by ';'."""
    rows = []
    for ident in big_machines():
        cells = machine_cells(ident)
        parts = [f"{d[0]},{d[1]},{d[2]}" for d, boxes in cells.items() if not boxes]
        parts += [f"{d[0]},{d[1]},{d[2]},{','.join(map(str, b))}" for d, boxes in cells.items() for b in boxes]
        rows.append(f'        Map.entry("{ident}", "{";".join(parts)}")')
        print(f"  shapes {ident}: {len(cells)} cells, {sum(len(v) for v in cells.values())} boxes, "
              f"{sum(1 for v in cells.values() if not v)} empty cells")
    lines = ["package dev.autarkia.registry;", "", "import java.util.HashMap;", "import java.util.Map;",
             "import java.util.concurrent.ConcurrentHashMap;", "import net.minecraft.core.Vec3i;",
             "import net.minecraft.world.level.block.Block;", "import net.minecraft.world.phys.shapes.Shapes;",
             "import net.minecraft.world.phys.shapes.VoxelShape;", "",
             "/**",
             " * Generated by tools/data.py from tools/shapes.py: the shape of every footprint cell of the GeckoLib big machines,",
             " * voxelized from their models, in cell-local pixels for the block facing north. A cell with only its offset is empty.",
             " */",
             "public final class MachineShapes {",
             "    private static final Map<String, String> DATA = Map.ofEntries(",
             ",\n".join(rows) + ");",
             "    private static final Map<String, Map<Vec3i, VoxelShape>> CACHE = new ConcurrentHashMap<>();", "",
             "    private MachineShapes() {", "    }", "",
             "    /** Return the shape of each footprint cell of a block id path, or null when the block has no generated shapes. */",
             "    public static Map<Vec3i, VoxelShape> of(String id) {",
             "        String data = DATA.get(id);",
             "        if (data == null) {", "            return null;", "        }",
             "        return CACHE.computeIfAbsent(id, k -> parse(data));", "    }", "",
             "    private static Map<Vec3i, VoxelShape> parse(String data) {",
             "        Map<Vec3i, VoxelShape> out = new HashMap<>();",
             "        for (String row : data.split(\";\")) {",
             "            String[] v = row.split(\",\");",
             "            Vec3i cell = new Vec3i(Integer.parseInt(v[0]), Integer.parseInt(v[1]), Integer.parseInt(v[2]));",
             "            VoxelShape shape = out.getOrDefault(cell, Shapes.empty());",
             "            if (v.length == 9) {",
             "                shape = Shapes.or(shape, Block.box(Integer.parseInt(v[3]), Integer.parseInt(v[4]), Integer.parseInt(v[5]),",
             "                    Integer.parseInt(v[6]), Integer.parseInt(v[7]), Integer.parseInt(v[8])));",
             "            }",
             "            out.put(cell, shape);",
             "        }",
             "        out.replaceAll((cell, shape) -> shape.optimize());",
             "        return Map.copyOf(out);",
             "    }", "}", ""]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8", newline="\n")
