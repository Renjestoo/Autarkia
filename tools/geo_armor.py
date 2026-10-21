"""GeckoLib armour models around the vanilla humanoid pivots (07 section 4.2).

Every set shares one shell layout: an open-faced helmet made of plates (the face shows through),
a torso shell, shoulder pauldrons with bracers, thigh shells with knee plates and boots. Each shell
sits 0.5-1px outside the vanilla part it covers. Animated details live in child bones so the eight
armour bones stay free for GeckoLib to pose.
"""

from geo_core import Model

PIVOTS = {"armorHead": (0, 24, 0), "armorBody": (0, 24, 0), "armorRightArm": (-5, 22, 0), "armorLeftArm": (5, 22, 0),
          "armorRightLeg": (-1.9, 12, 0), "armorLeftLeg": (1.9, 12, 0), "armorRightBoot": (-1.9, 12, 0),
          "armorLeftBoot": (1.9, 12, 0)}
SIDES = (("Right", -1), ("Left", 1))


def _shell(m: Model, mat: dict) -> None:
    """Add the eight armour bones and the shared shell plates.

    Args:
        mat: material per role: plate, trim, under, visor, knee, boot, pauldron.
    """
    for name, pivot in PIVOTS.items():
        m.bone(name, pivot=pivot)
    h = "armorHead"
    m.box(h, (-5, 30, -5), (10, 3, 10), mat["plate"])
    m.box(h, (-5, 24, 3), (10, 6, 2), mat["plate"])
    m.box(h, (-5, 24, -5), (1, 6, 8), mat["plate"])
    m.box(h, (4, 24, -5), (1, 6, 8), mat["plate"])
    m.box(h, (-4, 29, -5), (8, 1, 1), mat["trim"])
    m.box(h, (-4, 27, -5), (8, 2, 1), mat["visor"])
    m.box(h, (-4, 23.5, -5), (8, 1, 1), mat["trim"])
    b = "armorBody"
    m.box(b, (-4.5, 12, -2.5), (9, 12, 5), mat["under"])
    m.box(b, (-4, 17, -3), (8, 6, 1), mat["plate"])
    m.box(b, (-4, 13, -3), (8, 3, 1), mat["plate"])
    m.box(b, (-4, 15, 2), (8, 9, 1), mat["plate"])
    m.box(b, (-4.5, 23, -3), (9, 2, 6), mat["trim"])
    for side, s in SIDES:
        arm = f"armor{side}Arm"
        x0 = -9 if s < 0 else 3
        m.box(arm, (x0, 19, -3), (6, 6, 6), mat["pauldron"])
        m.box(arm, (x0 + 0.5, 12, -2.5), (5, 5, 5), mat["trim"])
        leg = f"armor{side}Leg"
        lx = -4.4 if s < 0 else -0.6
        m.box(leg, (lx, 5, -2.5), (5, 7, 5), mat["under"])
        m.box(leg, (lx + 0.5, 8, -3), (4, 3, 1), mat["plate"])
        m.box(leg, (lx + 0.5, 4, -3), (4, 3, 1), mat["knee"])
        m.box(leg, (lx, 10.5, -3), (5, 2, 6), mat["trim"])
        boot = f"armor{side}Boot"
        m.box(boot, (lx, 0, -2.5), (5, 5, 5), mat["boot"])
        m.box(boot, (lx, 0, -3.5), (5, 2, 1), mat["trim"])
        m.box(boot, (lx - 0.1, 4.5, -2.6), (5, 1, 5), mat["trim"])


def exo_rig() -> Model:
    """Steel exoskeleton: strut frame, leg pistons, back battery pack, blue visor."""
    m = Model("exo_rig")
    _shell(m, dict(plate="electric:shell:2.6", trim="electric:shell:1.6", under="electric:dark", visor="electric_accent:glow",
                   knee="electric:panel", boot="electric:shell:2.2", pauldron="electric:shell:2.8"))
    h = "armorHead"
    for x in (-6, 5):
        m.box(h, (x, 25, -2), (1, 3, 3), "electric:vent")
    m.box(h, (5.5, 28, 1), (1, 5, 1), "steel:plate")
    m.box(h, (5.5, 33, 1), (1, 1, 1), "electric_accent:glow")
    b = "armorBody"
    m.box(b, (-1, 19, -3.5), (2, 2, 1), "electric_accent:glow")
    m.box(b, (-3, 14, 3), (6, 9, 3), "electric:panel:4")
    m.box(b, (-3, 23, 3), (6, 1, 3), "hazard:hazard")
    m.box(b, (-2, 16, 6), (4, 1, 1), "electric_accent:glow")
    m.box(b, (-2, 18, 6), (4, 1, 1), "electric_accent:glow")
    m.box(b, (-0.5, 12, 2.5), (1, 12, 1), "steel:plate")
    for side, s in SIDES:
        arm = f"armor{side}Arm"
        m.box(arm, (-9.5 if s < 0 else 8.5, 13, -0.5), (1, 8, 1), "steel:plate")
        leg = f"armor{side}Leg"
        lx = -4.4 if s < 0 else -0.6
        ox = lx - 1 if s < 0 else lx + 5
        m.box(leg, (ox, 6, -1), (1, 5, 2), "electric:plate")
        rod = m.bone(f"exo_piston_{side.lower()}", leg, pivot=(ox + 0.5, 6, 0))
        m.box(rod, (ox + 0.25 if s < 0 else ox, 2, -0.5), (1, 5, 1), "iron:plate")
        m.box(f"armor{side}Boot", (ox, 1, -1), (1, 2, 2), "electric:plate")
    idle = m.anim("idle", "loop", 2.0)
    for side in ("right", "left"):
        idle.keys(f"exo_piston_{side}", "position", [(0, (0, 0, 0)), (1.0, (0, -0.8, 0)), (2.0, (0, 0, 0))])
    return m


def frontier_suit() -> Model:
    """Teal expedition suit: glowing visor, oxygen tanks, jet nozzles with flames on the boots."""
    m = Model("frontier_suit")
    _shell(m, dict(plate="vantite:shell:3", trim="vantite_steel:shell:2.4", under="vantite_steel:dark",
                   visor="#A8F0DC:glow", knee="vantite_steel:panel", boot="vantite_steel:shell:2.2", pauldron="vantite:shell:3"))
    h = "armorHead"
    m.box(h, (-4, 25, -5), (8, 2, 1), "#A8F0DC:glow")
    for x in (-6, 5):
        m.box(h, (x, 25, -2), (1, 4, 4), "vantite_steel:vent")
    m.box(h, (-6, 29, 0), (1, 5, 1), "vantite_steel:plate")
    b = "armorBody"
    m.box(b, (-3, 18, -3.5), (6, 4, 1), "vantite_steel:panel")
    m.box(b, (-2, 19, -3.6), (4, 1, 0), "#A8F0DC:glow")
    for x in (-4, 1):
        m.box(b, (x, 14, 3), (3, 9, 3), "vantite:plate")
        m.box(b, (x + 0.5, 23, 3.5), (2, 1, 2), "vantite_steel:plate")
    m.box(b, (-1, 15, 3.5), (2, 6, 2), "vantite_steel:coil")
    for side, s in SIDES:
        boot = f"armor{side}Boot"
        lx = -4.4 if s < 0 else -0.6
        m.box(boot, (lx + 1, 0.5, 2.5), (3, 3, 2), "vantite_steel:grille")
        flame = m.bone(f"jet_flame_{side.lower()}", boot, pivot=(lx + 2.5, 0.5, 4))
        m.box(flame, (lx + 1.5, -3.5, 3), (2, 4, 1), "#9BE7FF:glow")
        m.box(flame, (lx + 2, -5.5, 3.25), (1, 2, 0), "#D8FFFA:glow")
        m.box(f"armor{side}Arm", (-8.5 if s < 0 else 3.5, 15, -2.6), (5, 1, 5), "#A8F0DC:glow")
    idle = m.anim("idle", "loop", 2.0)
    jet = m.anim("jet", "loop", 0.3)
    for side in ("right", "left"):
        f = f"jet_flame_{side}"
        idle.keys(f, "scale", [(0, (0, 0, 0)), (2.0, (0, 0, 0))])
        ph = 0.0 if side == "right" else 0.15
        jet.keys(f, "scale", [(0, (1, 1 + ph, 1)), (0.1, (1.15, 1.4 - ph, 1.15)), (0.2, (0.9, 0.85 + ph, 0.9)),
                              (0.3, (1, 1 + ph, 1))])
    return m


def lattice_carapace() -> Model:
    """Violet crystalline plates over a dark undersuit; crystal shards shimmer."""
    m = Model("lattice_carapace")
    _shell(m, dict(plate="lattice:shell:2.7", trim="lattice:shell:1.2", under="lattice:dark", visor="#D9CCFF:glow",
                   knee="lattice:crystal", boot="lattice:shell:1.4", pauldron="lattice:shell:1.2"))
    crest = m.bone("shards_head", "armorHead", pivot=(0, 33, 0))
    m.box(crest, (-0.5, 33, -2), (1, 4, 2), "lattice:crystal", rotation=(-10, 0, 0), pivot=(0, 33, -1))
    m.box(crest, (-3, 33, -1), (1, 3, 1), "lattice:crystal", rotation=(0, 0, 25), pivot=(-2.5, 33, -0.5))
    m.box(crest, (2, 33, -1), (1, 3, 1), "lattice:crystal", rotation=(0, 0, -25), pivot=(2.5, 33, -0.5))
    chest = m.bone("shards_body", "armorBody", pivot=(0, 18, -3))
    m.box(chest, (-4, 17, -4), (4, 6, 1), "lattice:crystal", rotation=(0, 12, 0), pivot=(0, 20, -3.5))
    m.box(chest, (0, 17, -4), (4, 6, 1), "lattice:crystal", rotation=(0, -12, 0), pivot=(0, 20, -3.5))
    m.box(chest, (-1, 19, -4.6), (2, 2, 1), "#B39DFF:glow", rotation=(0, 0, 45), pivot=(0, 20, -4.1))
    for side, s in SIDES:
        sh = m.bone(f"shards_{side.lower()}", f"armor{side}Arm", pivot=(-6 if s < 0 else 6, 25, 0))
        cx = -6 if s < 0 else 6
        m.box(sh, (cx - 0.5, 25, -2), (1, 4, 1), "lattice:crystal", rotation=(0, 0, 20 * s), pivot=(cx, 25, -1.5))
        m.box(sh, (cx - 0.5, 25, 1), (1, 3, 1), "lattice:crystal", rotation=(0, 0, 35 * s), pivot=(cx, 25, 1.5))
        m.box(sh, (cx - 2, 24.5, -1), (1, 3, 2), "lattice:crystal", rotation=(0, 0, 10 * s), pivot=(cx - 1.5, 24.5, 0))
        lx = -4.4 if s < 0 else -0.6
        m.box(f"armor{side}Boot", (lx + 0.5, 0, -4), (4, 2, 1), "lattice:crystal")
    idle = m.anim("idle", "loop", 3.0)
    idle.osc("shards_head", "scale", (0.06, 0.08, 0.06), cycles=1)
    idle.osc("shards_body", "scale", (0.04, 0.04, 0.0), cycles=1, phase=0.33)
    idle.osc("shards_right", "scale", (0.06, 0.08, 0.06), cycles=1, phase=0.5)
    idle.osc("shards_left", "scale", (0.06, 0.08, 0.06), cycles=1, phase=0.83)
    return m


def _ring(m: Model, bone: str, center, radius: float, mat: str, segments: int = 8) -> None:
    """Add a flat ring of rotated segments around `center` in the bone's XZ plane."""
    cx, cy, cz = center
    length = round(2 * radius * 0.42)
    for k in range(segments):
        m.box(bone, (cx - length / 2, cy - 0.5, cz - radius - 0.5), (length, 1, 1), mat,
              rotation=(0, k * 360.0 / segments, 0), pivot=(cx, cy, cz))


def aegis() -> Model:
    """Black-gold armour with cyan glow seams and orbit rings circling the shoulders."""
    m = Model("aegis")
    _shell(m, dict(plate="aegis_body:seam:#7FFFF0", trim="aegis_gold:plate", under="aegis_body:shell:1.2",
                   visor="aegis_glow:glow", knee="aegis_gold:rivet", boot="aegis_body:seam:#7FFFF0",
                   pauldron="aegis_body:seam:#7FFFF0"))
    h = "armorHead"
    m.box(h, (-0.5, 33, -4), (1, 1, 8), "aegis_gold:plate")
    for x in (-6, 5):
        m.box(h, (x, 25, -4), (1, 4, 3), "aegis_gold:plate")
    core = m.bone("aegis_core", "armorBody", pivot=(0, 20, -3.5))
    m.box(core, (-1, 19, -4), (2, 2, 1), "aegis_glow:glow", rotation=(0, 0, 45), pivot=(0, 20, -3.5))
    m.box("armorBody", (-0.5, 12, -3.1), (1, 5, 1), "aegis_gold:plate")
    for side, s in SIDES:
        arm = f"armor{side}Arm"
        cx = -6 if s < 0 else 6
        m.box(arm, (cx - 3.5, 24.5, -3.5), (7, 1, 7), "aegis_gold:plate")
        rx = cx + 1.5 * s
        ring = m.bone(f"orbit_{side.lower()}", arm, pivot=(rx, 22, 0), rotation=(0, 0, 75 * s))
        _ring(m, ring, (rx, 22, 0), 3.5, "aegis_glow:glow")
    idle = m.anim("idle", "loop", 4.0)
    idle.spin("orbit_right", "y", 1)
    idle.spin("orbit_left", "y", -1)
    idle.osc("aegis_core", "scale", (0.15, 0.15, 0.15), cycles=2)
    return m


MODELS = {"exo_rig": exo_rig, "frontier_suit": frontier_suit, "lattice_carapace": lattice_carapace, "aegis": aegis}
