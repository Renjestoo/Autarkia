"""Block texture painters for BLOCK_TEXTURES styles: STYLES[style](name, palette, rng) -> 16x16 RGBA.

The painters live in four themed modules and the T5 logistics module; this index merges them and rejects duplicate style names.
"""

from paint_blocks_machine import STYLES as MACHINE
from paint_blocks_metal import STYLES as METAL
from paint_blocks_nature import STYLES as NATURE
from paint_blocks_wood import STYLES as WOOD
from paint_logistics_t5 import STYLES as LOGISTICS_T5


def _merge(*parts: dict) -> dict:
    """Merge style dicts, failing loudly when two modules define the same style."""
    out = {}
    for part in parts:
        clash = set(out) & set(part)
        if clash:
            raise ValueError(f"block styles defined twice: {sorted(clash)}")
        out.update(part)
    return out


STYLES = _merge(NATURE, METAL, WOOD, MACHINE, LOGISTICS_T5)

FRAMES = {"magma_vent": (8, 3), "molten": (8, 3), "phase_node": (8, 4)}
"""Animated styles: style -> (frame count, game ticks per frame). Their painters take a phase keyword (0..1) and loop over it."""
