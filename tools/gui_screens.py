"""Widget specs of every Autarkia GUI for tools/gui_render.py, mirroring the screens' Java code (client/screen).

SCREENS maps a screen name to a function that takes the Renderer and returns a spec: size (w, h), background (a
textures/gui name) or panel (a nine-slice sprite), slots, and elements. Elements are dicts of kind text, button, edit or
bar in panel coordinates; a text lists variants (every text the screen can show there, with worst-case arguments) so
the widest one is measured, and room is the width the screen leaves for it. slot_screen() builds what SlotScreen draws
for a generated layout; the custom screens add their own widgets. When a screen's Java layout changes, its spec here
must change with it.
"""

import gui

STATUS = ["status.autarkia.running", "status.autarkia.idle_no_input", "status.autarkia.blocked_output_full", "status.autarkia.no_power",
          "status.autarkia.no_condition"]
HEAT = ["heat.autarkia.campfire", "heat.autarkia.furnace", "heat.autarkia.lava", "heat.autarkia.none"]
DARK = 0x404040
BIG = 1048576
SCREENS = {}


def text(x, y, key, *args, color=DARK, room=None, right=False, cut=False, variants=None, shadow=False, lines=0, line_h=9):
    """Return a text element; variants are (key, args) pairs measured for the widest; lines > 0 word-wraps to room."""
    return dict(kind="text", x=x, y=y, text=key, args=args, color=color, room=room, right=right, cut=cut, shadow=shadow,
                variants=variants, lines=lines, line_h=line_h)


def button(x, y, w, h, key, *args, active=True, variants=()):
    """Return a vanilla button element; variants are other label keys the button can show (the widest is measured)."""
    return dict(kind="button", x=x, y=y, w=w, h=h, text=key, args=args, active=active, variants=list(variants))


def edit(x, y, w, h, hint=""):
    """Return an edit box element."""
    return dict(kind="edit", x=x, y=y, w=w, h=h, hint=hint)


def bar(x, y, w, h, label, color=0x2A2A2A):
    """Return a code-drawn bar or box element (counts for overlaps)."""
    return dict(kind="bar", x=x, y=y, w=w, h=h, label=label, color=color)


def title_key(r, name, block=None):
    """Return the title lang key a menu shows: the block name (getDisplayName of most block entities), else the menu key."""
    for k in (f"block.autarkia.{block or name}", f"menu.autarkia.{name}"):
        if k in r.lang:
            return k
    return f"block.autarkia.{block or name}"


def layout_slots(spec):
    """Return every machine slot position of a gui.py layout."""
    out = list(spec.get("inputs", [])) + list(spec.get("outputs", []))
    if "grid" in spec:
        gx, gy, cols, rows = spec["grid"]
        out += [(gx + c * 18, gy + r * 18) for r in range(rows) for c in range(cols)]
    return out


def slot_screen(r, name, block=None, status=True, extra=(), title=None, inv_label=True):
    """Return the spec SlotScreen draws for layout name: background, slots, title, status, inventory label, widgets."""
    L = gui.LAYOUTS[name]
    w, h = L.get("width", 176), L["height"]
    inv_y, inv_x = h - 82, gui.inv_x(L)
    slots = layout_slots(L) + [(inv_x + c * 18, inv_y + rr * 18) for rr in range(3) for c in range(9)] + [(inv_x + c * 18, inv_y + 58) for c in range(9)]
    tkey = title or title_key(r, name, block)
    els = [text(8, 6, tkey)]
    machine = L["data"] >= 5
    if inv_label:
        els.append(text(inv_x, inv_y - 11, "container.inventory"))
    if "energy" in L:
        els.append(bar(L["energy"][0], L["energy"][1], 12, 52, "energy bar", 0x281612))
    if "arrow" in L:
        els.append(bar(L["arrow"][0], L["arrow"][1], 24, 17, "arrow", 0x786E62))
    if "flame" in L:
        els.append(bar(L["flame"][0], L["flame"][1], 14, 14, "flame", 0x5A5A5A))
    if machine and status:
        tw = r.font.width(r.text_of(tkey))
        own = f"status.autarkia.condition.{name}"
        keys = [own if k.endswith("no_condition") and own in r.lang else k for k in STATUS]
        beside = [k for k in keys if tw + r.font.width(r.text_of(k)) + 8 <= w - 16]
        below = [k for k in keys if k not in beside]
        if beside:
            els.append(text(w - 8, 6, beside[0], variants=[(k, ()) for k in beside], right=True))
        if below:
            sx = L["energy"][0] + 16 if "energy" in L else 8
            els.append(text(sx, 16, below[0], variants=[(k, ()) for k in below], room=w - 8 - sx))
        if "flame" in L:
            fx, fy = L["flame"]
            els.append(text(fx + 18, fy + 3, HEAT[0], variants=[(k, ()) for k in HEAT]))
    els += list(extra)
    return dict(size=(w, h), background=name, slots=slots, elements=els)


def plain(name, block=None, **kw):
    """Register a screen that is exactly SlotScreen on its layout."""
    SCREENS[block or name] = lambda r: slot_screen(r, name, block, **kw)


for _n in ["stamp_mill", "crucible", "crate", "sluice", "blast_smelter", "plate_press", "wire_drawer", "jaw_crusher", "magnetic_separator",
           "electrolyzer", "plasma_separator", "worker_assembler"]:
    plain(_n)


def _energy_storage(r, block):
    stored = [("screen.autarkia.energy_storage.stored", (400000, 400000))]
    return slot_screen(r, "energy_storage", block, status=False, extra=[
        text(26, 22, "screen.autarkia.energy_storage.stored", variants=stored, room=150 - 26),
        text(26, 34, "screen.autarkia.energy_storage.percent", 100, room=150 - 26),
        text(26, 46, "screen.autarkia.energy_storage.io_in", 99999, room=150 - 26),
        text(26, 58, "screen.autarkia.energy_storage.io_out", 99999, room=150 - 26)])


SCREENS["capacitor_bank"] = lambda r: _energy_storage(r, "capacitor_bank")
SCREENS["battery_bank"] = lambda r: _energy_storage(r, "battery_bank")


def _filter(r, name):
    L = gui.LAYOUTS[name]
    spec = slot_screen(r, name, status=False)
    f = "screen.autarkia.filter."
    els = [button(8, 16, 84, 12, f + "mode_alternate", variants=[f + "mode_filter"])] if name == "belt_splitter" else []
    els += [button(8, 29, 84, 12, f + "side_left", variants=[f + "side_right"]), button(8, 42, 84, 12, f + "whitelist", variants=[f + "blacklist"]),
            button(8, 55, 84, 12, f + "item_only", variants=[f + "components"])]
    spec["elements"] += els
    return spec


SCREENS["belt_splitter"] = lambda r: _filter(r, "belt_splitter")
SCREENS["sorting_chute"] = lambda r: _filter(r, "sorting_chute")


def _fabricator(r):
    x, y = 124 - 6, 17 + 20
    return slot_screen(r, "fabricator", extra=[bar(123, 16, 18, 18, "target ghost", 0x373737), button(x, y, 12, 12, "<"), button(x + 13, y, 12, 12, ">"),
                                               button(x + 26, y, 12, 12, "x")])


SCREENS["fabricator"] = _fabricator


def _foundry(r):
    states = [(f"jade.autarkia.replicator.{k}", ()) for k in ("running", "no_print", "no_power", "cap_full", "outside", "no_parts")]
    states = [s for s in states if s[0] in r.lang] or [("jade.autarkia.replicator.no_print", ())]
    walkers = text(176 - 8, 6, "jade.autarkia.replicator.walkers", 9, 9, right=True)
    return slot_screen(r, "replicator_foundry", status=False, extra=[text(24, 16, states[0][0], variants=states), walkers])


SCREENS["replicator_foundry"] = _foundry


def _forge(r):
    st = [(k, ()) for k in r.lang if k.startswith("jade.autarkia.forge.") and k.count(".") == 3 and k.split(".")[-1].islower()]
    return slot_screen(r, "forge_of_ascension", status=False, extra=[
        text(11, 79, "jade.autarkia.forge.idle", variants=st or None, room=154),
        text(11, 88, "jade.autarkia.forge.fitted", 9, room=154),
        text(11, 97, "screen.autarkia.forge.hint", room=154, lines=2, color=0xBFBFBF),
        text(11, 115, "screen.autarkia.forge.grid", 99999, room=154, color=0xBFBFBF)])


SCREENS["forge_of_ascension"] = _forge


def _singularity(r):
    return slot_screen(r, "singularity_core", status=False, extra=[
        button(82, 16, 42, 14, "screen.autarkia.singularity.ignite"), button(126, 16, 42, 14, "screen.autarkia.singularity.stop"),
        text(26, 20, "singularity.autarkia.state.igniting", variants=[(f"singularity.autarkia.state.{s}", ()) for s in ("off", "igniting", "online")],
             room=52, cut=True),
        text(26, 34, "screen.autarkia.singularity.base", 99999, room=142, cut=True),
        text(26, 48, "screen.autarkia.singularity.points", 99999, room=142, cut=True),
        text(26, 58, "screen.autarkia.singularity.explosions", 999, room=142, cut=True),
        text(26, 68, "screen.autarkia.singularity.paid", 99999999, room=142, cut=True),
        bar(26, 80, 142, 6, "ignition bar", 0x1A1A1A)])


SCREENS["singularity_core"] = _singularity


def _projector(r):
    els = [text(8, 17, "jade.autarkia.restoration.state.watching", room=140, cut=True),
           text(8, 30, "screen.autarkia.restoration.size", room=140, cut=True),
           text(8, 56, "screen.autarkia.restoration.offset", room=140, cut=True),
           button(96, 80, 52, 14, "screen.autarkia.restoration.snapshot"), text(8, 84, "jade.autarkia.restoration.missing", 99999, room=86)]
    for row, y in enumerate((40, 66)):
        for axis in range(3):
            x0 = 8 + axis * 50
            els += [button(x0, y, 10, 12, "-"), button(x0 + 36, y, 10, 12, "+")]
            v = "XYZ"[axis] + ("-64" if row else "64")
            els.append(text(x0 + 10 + (26 - r.font.width(v) + 1) // 2, y + 2, v, room=26))
    return slot_screen(r, "restoration_projector", status=False, extra=els)


SCREENS["restoration_projector"] = _projector


def _crate_launcher(r):
    states = [(f"state.autarkia.launcher.{s}", ()) for s in ("ready", "no_target", "out_of_range", "waiting", "cooldown", "no_crate", "no_power",
                                                              "busy", "no_arc", "paused")]
    shots = [("screen.autarkia.launcher.hit", ()), ("screen.autarkia.launcher.miss", (99.9,)), ("screen.autarkia.launcher.miss_full", (99.9,)),
             ("screen.autarkia.launcher.none", ())]
    return slot_screen(r, "crate_launcher", status=False, extra=[
        text(8, 96, "state.autarkia.launcher.no_target", variants=states, room=160, lines=2),
        text(8, 114, "screen.autarkia.launcher.distance", 128, 45, room=160),
        text(8, 123, "screen.autarkia.launcher.miss_full", variants=shots, room=160),
        bar(8, 134, 160, 3, "energy line", 0x2A1A12)])


SCREENS["crate_launcher"] = _crate_launcher


def _seed_matrix(r):
    els = [button(304 - 52, 4, 44, 11, "screen.autarkia.seed_matrix.pack")]
    for c in range(2):
        x0 = 8 + 146 * c
        for label, cx in (("X", 41), ("Z", 87), ("Rot", 126)):
            els.append(text(x0 + cx - r.font.width(label) // 2, 16, label, room=30))
    for i in range(8):
        x0, y = 8 + 146 * (i // 4), 26 + 18 * (i % 4)
        els += [button(x0 + 20, y + 3, 10, 10, "-"), button(x0 + 52, y + 3, 10, 10, "+"), button(x0 + 66, y + 3, 10, 10, "-"),
                button(x0 + 98, y + 3, 10, 10, "+"), button(x0 + 112, y + 3, 28, 10, "270°")]
        for cx in (41, 87):
            els.append(text(x0 + cx - r.font.width("-99") // 2, y + 4, "-99", room=22))
    st = [(f"jade.autarkia.seed_matrix.{s}", ()) for s in ("ready", "no_cards", "no_pattern", "not_outpost", "overlap", "too_big", "no_fit", "outside",
                                                          "tier_low", "output_full", "missing_items", "running", "no_power", "no_matter")]
    els += [text(8, 101, "screen.autarkia.seed_matrix.size", 64, 64, 64, 99999, room=260, cut=True),
            text(8, 111, "jade.autarkia.seed_matrix.ready", variants=st, room=260, cut=True), bar(8, 121, 260, 3, "pack bar", 0x1A1A1A)]
    return slot_screen(r, "seed_matrix", status=False, extra=els)


SCREENS["seed_matrix"] = _seed_matrix

import gui_screens_machines  # noqa: E402
import gui_screens_panels  # noqa: E402

gui_screens_machines.register(SCREENS)
gui_screens_panels.register(SCREENS)
