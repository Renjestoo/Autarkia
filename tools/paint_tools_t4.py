"""Tier 4 tool sprite painters: SHAPES[shape](name, palette, rng) -> 16x16 RGBA.

Same conventions as paint_tools: the tool lies on the anti-diagonal, handle at the bottom-left, working end at the
top-right; u runs along the handle and v across it (paint_items.diag).
"""

import numpy as np

from paint import ramp
from paint_items import Sprite, diag, mask_rows

HEAD = [
    "................",
    "....#######.....",
    "..###########...",
    ".#####....####..",
    ".###.......###..",
    ".##.........#...",
    ".#..............",
]


def _antisym(m: np.ndarray) -> np.ndarray:
    """Return the union of a mask and its mirror across the anti-diagonal (x, y) -> (15 - y, 15 - x)."""
    return m | m[::-1, ::-1].T


def excavator(name, pal, rng):
    """Vantite excavator: rubber grip on a vantite steel shaft, heavy pick arc over the top-right corner, a spade blade
    at the tip and a glowing teal core line through head and blade."""
    body, frame, grip = ramp(pal), ramp("vantite_steel"), ramp("rubber")
    s = Sprite()
    u, v = diag()
    shaft = (u >= -13) & (u <= 6)
    s.paint(shaft & (v == 0), frame, 2)
    s.paint(shaft & (v == 1), frame, 1)
    hold = (u >= -13) & (u <= -6)
    s.paint(hold & (v == 0), grip, 3)
    s.paint(hold & (v == 1), grip, 1)
    s.paint(hold & (v == 0) & (u.astype(int) % 3 == 0), grip, 4)
    collar = (u >= -1) & (u <= 1.5) & (np.abs(v - 0.5) <= 1.6)
    s.layer(collar, frame, 2.0, 0.7, crease=True)
    head = _antisym(mask_rows(HEAD))
    s.layer(head, body, 2.3, 0.8, crease=True)
    blade = (((u - 9.5) / 4.0) ** 2 + ((v - 0.5) / 2.6) ** 2 <= 1.0) & (u >= 7.0)
    s.layer(blade, body, 2.6, 0.8, crease=True)
    s.paint((u >= 3) & (u <= 12) & (v == 0) & (head | blade), body, 4)
    s.glint([(12, 3)])
    return s.render()


SHAPES = {"excavator": excavator}
