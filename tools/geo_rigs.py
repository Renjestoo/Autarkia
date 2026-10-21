"""Worker rigs (walker, crab, drone): models, grey templates, region masks and per-worker recolours.

Rig cube materials name a region instead of a palette: 'primary:<finish>', 'secondary:<finish>',
'lamp:<finish>' or 'neutral:<finish>'. Each region paints with its own grey ramp; the mask stores the
region as R (primary), G (secondary) or B (lamp) with the template's alpha; neutral pixels stay grey.
"""

from pathlib import Path

import numpy as np

from art_spec import WORKER_TYPES
from geo_core import Model, build_atlas, mat_parts
from geo_faces import paint_face
from geo_rigs_crab import crab
from geo_rigs_drone import drone
from geo_rigs_walker import walker
from paint import TEXTURES, hex_rgb, save_png

MODELS = {"walker": walker, "crab": crab, "drone": drone}

REGION_RAMPS = {
    "primary": [118, 165, 198, 224, 248],
    "secondary": [118, 165, 198, 224, 248],
    "lamp": [150, 175, 200, 228, 252],
    "neutral": [38, 58, 84, 112, 146],
}
CHANNEL = {"primary": 0, "secondary": 1, "lamp": 2}
NORM = 200.0
LUMA = np.array([0.299, 0.587, 0.114])


def _ramp(region: str) -> np.ndarray:
    if region not in REGION_RAMPS:
        raise ValueError(f"rig material region '{region}' must be one of {sorted(REGION_RAMPS)}")
    return np.array([[v, v, v] for v in REGION_RAMPS[region]], dtype=np.int64)


def gray_patch(mat: str, group: str, w: int, h: int, rng: np.random.Generator) -> np.ndarray:
    """Paint a rig material patch in its region's grey ramp and flatten it to luminance."""
    region, finish, arg = mat_parts(mat)
    img = paint_face(":".join(["rig_gray", finish] + ([arg] if arg else [])), group, w, h, rng, r=_ramp(region))
    lum = np.clip(np.rint(img[..., :3].astype(np.float64) @ LUMA), 0, 255).astype(np.uint8)
    out = img.copy()
    out[..., 0] = out[..., 1] = out[..., 2] = lum
    return out


def mask_patch(mat: str, group: str, w: int, h: int, rng: np.random.Generator) -> np.ndarray:
    """Paint the region mask of a rig material patch (same alpha as its grey patch)."""
    region = mat_parts(mat)[0]
    alpha = gray_patch(mat, group, w, h, rng)[..., 3]
    out = np.zeros((h, w, 4), dtype=np.uint8)
    if region in CHANNEL:
        out[..., CHANNEL[region]] = 255
    out[..., 3] = alpha
    out[alpha == 0] = 0
    return out


def rig_atlas(model: Model) -> tuple[np.ndarray, np.ndarray, dict]:
    """Return (grey template atlas, region mask atlas, face UV map) packed identically."""
    gray, uvs = build_atlas(model, gray_patch)
    mask, uvs_mask = build_atlas(model, mask_patch)
    if uvs != uvs_mask or gray.shape != mask.shape:
        raise RuntimeError(f"{model.ident}: template and mask packing differ")
    return gray, mask, uvs


def recolour(gray: np.ndarray, mask: np.ndarray, primary: str, secondary: str, lamp: str) -> tuple[np.ndarray, np.ndarray]:
    """Return (texture, glowmask) for one worker.

    Each region pixel becomes clip(grey * colour / 200) per channel, so template grey 200 reproduces the
    palette colour exactly, darker greys shade it and brighter greys (up to 255) highlight it. Pixels
    outside every region keep their grey. The glowmask holds only the recoloured lamp pixels.
    """
    g = gray[..., 0].astype(np.float64)
    out = gray.copy()
    for ch, code in enumerate((primary, secondary, lamp)):
        sel = mask[..., ch] > 0
        c = np.array(hex_rgb(code), dtype=np.float64)
        out[sel, :3] = np.clip(np.rint(g[sel, None] * c / NORM), 0, 255).astype(np.uint8)
    glow = np.zeros_like(out)
    lamp_sel = mask[..., 2] > 0
    glow[lamp_sel] = out[lamp_sel]
    return out, glow


def write_rig_textures(rig: str, gray: np.ndarray, mask: np.ndarray) -> list[Path]:
    """Write the rig template and mask plus every worker texture and glowmask of that rig."""
    base = TEXTURES / "entity"
    paths = [save_png(gray, base / "template" / f"{rig}.png"), save_png(mask, base / "template" / f"{rig}_mask.png")]
    for wid, (wrig, primary, secondary, lamp) in WORKER_TYPES.items():
        if wrig != rig:
            continue
        tex, glow = recolour(gray, mask, primary, secondary, lamp)
        paths.append(save_png(tex, base / f"{wid}.png"))
        paths.append(save_png(glow, base / f"{wid}_glowmask.png"))
    return paths
