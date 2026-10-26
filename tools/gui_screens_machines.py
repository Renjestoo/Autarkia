"""Widget specs of the machine and storage screens with their own Java screen class (see gui_screens).

Coordinates are panel-relative, exactly as each screen in src/main/java/dev/autarkia/client/screen computes them. Text
the Java scales down to a width (ArchiveScreen.small) is measured against the unscaled width
it is cut or scaled to (room=..., cut=True), so its drawn box here is wider than on screen.
"""

import gui_screens as g

BIG_COORD = -30000000
MU = "1,048,576"


def _t(r, key, *args):
    return r.text_of(key, args)


def _keys(prefix, names, *args):
    return [(prefix + n, args) for n in names]


def _title_room(spec, room):
    spec["elements"][0]["room"] = room
    return spec


def _boiler(r):
    els = [g.bar(28, 18, 12, 52, "water bar", 0x18243A), g.bar(46, 18, 12, 52, "steam bar", 0x18243A),
           g.bar(64, 22, 49, 27, "pressure dial", 0x2A2A2A),
           g.text(116, 18, "screen.autarkia.boiler.pressure", "12.0"),
           g.text(116, 30, "screen.autarkia.boiler.temperature", 999),
           g.text(116, 42, "screen.autarkia.boiler.fire", 4),
           g.text(116, 52, "screen.autarkia.boiler.venting", color=0xB01818)]
    els += [g.button(116 + i * 11, 62, 10, 12, str(i)) for i in range(5)]
    title_w = r.font.width(r.text_of(g.title_key(r, "boiler")))
    state = _keys("screen.autarkia.boiler.", ["no_water", "fire_off", "no_fuel", "heating", "boiling"])
    els.append(g.text(168, 6, state[0][0], variants=state, right=True, color=0xB01818, room=168 - 8 - title_w))
    return g.slot_screen(r, "boiler", status=False, extra=els)


def _steam_turbine(r):
    t = "screen.autarkia.turbine."
    state = [("status.autarkia.running", ())] + _keys(t, ["no_boiler", "low_steam", "full"])
    return g.slot_screen(r, "steam_turbine", status=False, extra=[
        g.bar(25, 25, 54, 29, "rotor dial", 0x404040),
        g.text(32, 58, t + "rotor", 100),
        g.text(88, 26, t + "steam", 100, room=168 - 88),
        g.text(88, 38, t + "output", 1000, room=168 - 88),
        g.text(88, 50, state[0][0], variants=state, room=168 - 88, color=0xB01818)])


def _fuse_box(r):
    state = [("jade.autarkia.fuse.closed", ()), ("jade.autarkia.fuse.open", (999,)), ("jade.autarkia.fuse.locked", ())]
    return g.slot_screen(r, "fuse_box", status=False, extra=[
        g.button(108, 50, 60, 16, "screen.autarkia.fuse.reset"),
        g.bar(8, 20, 12, 12, "fuse lamp", 0x3FA040),
        g.text(24, 22, state[0][0], variants=state),
        g.text(8, 40, "screen.autarkia.fuse.trips", 99),
        g.text(8, 54, "screen.autarkia.fuse.spans", 2, room=108 - 8)])


def _warehouse(r):
    w = "screen.autarkia.warehouse."
    inv_w = r.font.width(r.text_of("container.inventory"))
    return g.slot_screen(r, "warehouse_controller", status=False, extra=[
        g.text(8, 18, w + "racks", 256), g.text(8, 28, w + "fill", 100), g.text(8, 38, w + "overloaded", 256, color=0xB01818),
        g.text(8, 48, w + "requests", 16), g.text(8, 58, w + "free", 99999),
        g.text(168, 166 - 82 - 11, "status.autarkia.no_power", right=True, color=0xB01818, room=168 - 8 - inv_w)])


def _outpost(r):
    o = "screen.autarkia.outpost."
    links = [(o + "link", (_t(r, "link.autarkia." + k),)) for k in ("none", "hv", "cable_car", "both")]
    return g.slot_screen(r, "outpost_core", status=False, title="menu.autarkia.outpost_core", extra=[
        g.text(8, 18, o + "index", 6, 6), g.text(8, 28, links[0][0], variants=links),
        g.text(8, 38, o + "catch_up", 99999), g.text(8, 48, o + "power", 100),
        g.text(8, 58, o + "profile", 999), g.text(8, 68, o + "buffer", 4096)])


def _capsule_station(r):
    c = "screen.autarkia.capsule_station."
    states = _keys("state.autarkia.capsule.", ["running", "clogged", "too_large", "paused", "no_tube", "no_power", "no_station"])
    els = [g.button(x, 16, w, 13, c + mode, active=i != 0) for i, ((x, w), mode) in enumerate(zip([(7, 36), (45, 46), (93, 36)],
                                                                                                    ["send", "receive", "both"]))]
    els += [g.button(132, 16, 12, 13, "-"), g.button(158, 16, 12, 13, "+"),
            g.text(151 - r.font.width("9") // 2, 19, "9", room=158 - 144),
            g.text(7, 33, c + "input", room=63 - 7), g.text(63, 33, c + "output", room=119 - 63), g.text(119, 33, c + "filter"),
            g.button(118, 99, 54, 12, c + "whitelist", variants=[c + "blacklist"]),
            g.button(118, 113, 54, 12, c + "exact", variants=[c + "item_only"]),
            g.text(7, 100, c + "tubes", 9999, room=118 - 7), g.text(7, 109, c + "stations", 32, room=118 - 7),
            g.text(7, 118, c + "capsules", 999, room=118 - 7), g.text(7, 127, states[0][0], variants=states)]
    return g.slot_screen(r, "capsule_station", status=False, extra=els)


def _grid_message(r, x, y, w, h, variants, lines):
    """Return the stock grid bar with its empty-grid message, and their overlap pairs (the message shows only on an empty grid)."""
    room = w - 8
    els = [g.bar(x, y, w, h, "stock grid", 0x8B8B8B), g.text(x + 4, y + 5, variants[0][0], variants=variants, room=room, lines=lines, color=0x303030)]
    ok = [("stock grid", line) for k, a in variants for line in r.split(r.text_of(k, a), room)]
    return els, ok


def _ledger(r):
    w, h = 264, 240
    l = "screen.autarkia.ledger."
    inv_w = r.font.width(r.text_of("container.inventory"))
    inv_x = g.gui.inv_x(g.gui.LAYOUTS["ledger_terminal"])
    warn = [(k, ()) for k in r.lang if k.startswith(l + "warning.")]
    els, ok = _grid_message(r, 8, 30, 162, 36, [(l + "empty", ()), (l + "no_match", ())], 2)
    els += [g.button(w - 58, 4, 50, 11, l + "sites"), g.edit(8, 17, 162, 11, l + "search")]
    no_req = r.text_of(l + "no_requests")
    els.append(g.text(179, 21, l + "no_requests", color=0x505050, room=78 - 2))
    for i in range(6):
        y = 17 + i * 17
        els += [g.bar(177, y, 16, 16, f"queue item {i}", 0x6A6A6A), g.bar(177, y + 15, 78, 1, f"queue status {i}", 0x3F7FD0),
                g.text(177 + 18, y + 4, "1728/1728", color=0x303030, room=78 - 18)]
        ok.append((f"queue item {i}", f"queue status {i}"))
    ok += [("queue item 0", no_req), ("1728/1728", no_req)]
    els += [g.button(8, 70, 22, 14, "-64"), g.button(30, 70, 16, 14, "-1"), g.button(76, 70, 16, 14, "+1"),
            g.button(92, 70, 22, 14, "+64"), g.button(118, 70, 52, 14, l + "request"),
            g.text(61 - r.font.width("1728") // 2, 73, "1728", color=0x303030, room=76 - 46),
            g.text(198, 129, l + "crates", room=w - 8 - 198),
            g.text(w - 8, h - 82 - 11, warn[0][0] if warn else l + "warning.no_warehouse", variants=warn or None,
                   right=True, color=0xB01818, room=w - 8 - (inv_x + inv_w + 8))]
    spec = _title_room(g.slot_screen(r, "ledger_terminal", status=False, extra=els), w - 58 - 8 - 4)
    spec["overlap_ok"] = ok
    return spec


def _cable_car(r):
    c = "screen.autarkia.cable_car."
    states = _keys("state.autarkia.cable_car.", ["running", "slowed", "wind_hold", "obstructed", "parked", "no_power", "paused",
                                                  "no_gondolas"])
    count = "8 / 8"
    els = [g.button(7, 16, 44, 12, c + "input", active=False), g.button(53, 16, 44, 12, c + "output"),
           g.button(48, 93, 12, 12, "-"), g.button(90, 93, 12, 12, "+"),
           g.text(8, 84, c + "parts"), g.text(75 - r.font.width(count) // 2, 95, count, color=0x303030, room=90 - 60),
           g.text(112, 96, c + "swing", 60),
           g.text(8, 114, c + "line", 160, variants=[(c + "unlinked", ()), (c + "line", (160,))], room=115 - 8),
           g.text(8, 124, states[0][0], variants=states, room=115 - 8),
           g.bar(8, 134, 96, 3, "energy line", 0x2A1A12), g.bar(115, 105, 50, 27, "swing gauge", 0x3FB04F)]
    L = g.gui.LAYOUTS["cable_car_station"]
    spec = g.slot_screen(r, "cable_car_station", status=False, extra=els)
    inv_y = L["height"] - 82
    spec["slots"] = list(L["inputs"]) + [(8 + c * 18, inv_y + rr * 18) for rr in range(3) for c in range(9)] + \
        [(8 + c * 18, inv_y + 58) for c in range(9)]
    return spec


def _depot(r):
    spec = g.slot_screen(r, "depot", extra=[g.edit(8, 17, 88, 12, "screen.autarkia.depot.name"),
                                            g.button(100, 16, 68, 14, "screen.autarkia.depot.linked")])
    spec["elements"][0].update(variants=[("block.autarkia.depot", ()), ("North Quarry Overflow Depot East", ())], room=176 - 16, cut=True)
    return spec


def _flotation(r):
    return g.slot_screen(r, "froth_flotation_plant", extra=[g.bar(152, 18, 16, 60, "water tank", 0x1C3F73)])


def _cryo(r):
    spans = [("screen.autarkia.cryo.no_spans", ()), ("screen.autarkia.cryo.span", (1, 100))]
    els = [g.bar(152, 18, 12, 52, "coolant bar", 0x18243A), g.text(84, 20, spans[1][0], 1, 100, variants=spans, room=152 - 84)]
    els += [g.text(84, 20 + 10 * k, "screen.autarkia.cryo.span", k + 1, 100, room=152 - 84) for k in range(1, 4)]
    els.append(g.text(8, 58, "screen.autarkia.cryo.coolant", 9999, room=152 - 8))
    return g.slot_screen(r, "cryo_node", status=False, extra=els)


def _coil(r):
    charge = [("screen.autarkia.coil.quenched", ()), ("screen.autarkia.energy_storage.percent", (100,))]
    spec = g.slot_screen(r, "superconducting_coil", status=False, extra=[
        g.bar(134, 18, 12, 52, "temperature bar", 0x18243A), g.bar(133, 44, 14, 1, "half mark", 0xFFFFFF),
        g.text(8, 38, charge[0][0], variants=charge, room=134 - 12),
        g.text(8, 47, "screen.autarkia.energy_storage.io_in", 65536, room=134 - 12),
        g.text(8, 56, "screen.autarkia.energy_storage.io_out", 65536, room=134 - 12),
        g.text(8, 65, "screen.autarkia.cryo.coolant", 9999, room=134 - 12)])
    spec["overlap_ok"] = [("temperature bar", "half mark")]
    return spec


def _plasma(r):
    states = _keys("plasma.autarkia.state.", ["cold", "igniting", "ramping", "running", "hot_idle"])
    line = [("screen.autarkia.plasma.hot", (10,)), ("screen.autarkia.plasma.ignition", (100,)), ("screen.autarkia.plasma.lit", ()),
            ("screen.autarkia.plasma.no_fuel", ())]
    return g.slot_screen(r, "plasma_generator", status=False, extra=[
        g.button(116, 16, 52, 14, "screen.autarkia.plasma.ignite"),
        g.text(52, 20, states[0][0], variants=states, room=116 - 52),
        g.text(52, 32, "jade.autarkia.output", 65536),
        g.text(30, 50, line[0][0], variants=line, room=168 - 30),
        g.bar(30, 62, 138, 6, "ignition bar", 0xFF5CF0)])


def _rack(r, layout, block, rating):
    n = len(g.gui.LAYOUTS[layout]["inputs"])
    load = f"{rating * 3 // 2:,}"
    els = [g.button(8 + i * 40 + 18, 18, 16, 16, "»") for i in range(n)]
    els += [g.bar(7, 39, 162, 8, "load bar", 0x373737), g.bar(8 + round(160 / 1.5), 38, 1, 10, "full mark", 0xFFFFFF),
            g.bar(6, 16, 20, 1, "selected crate", 0xF2D3A0),
            g.text(8, 49, "screen.autarkia.rack.load", load, f"{rating:,}", room=176 - 16)]
    title = f"block.autarkia.{block}"
    els.append(g.text(176 - 8, 6, "screen.autarkia.rack.countdown", 60, color=0xB01818, right=True,
                      room=176 - 16 - 8 - r.font.width(r.text_of(title))))
    spec = g.slot_screen(r, layout, block=block, status=False, title=title, extra=els)
    spec["overlap_ok"] = [("load bar", "full mark")]
    return spec


def _pattern_loom(r):
    els = []
    for row, (key, value) in enumerate((("screen.autarkia.loom.offset", "-24"), ("screen.autarkia.loom.size", "48"))):
        y = 16 + row * 13
        els.append(g.text(8, y + 2, key, room=44 - 8))
        for a in range(3):
            bx = 44 + a * 42
            els += [g.button(bx, y, 10, 12, "-"), g.button(bx + 40 - 10, y, 10, 12, "+"),
                    g.text(bx + 20 - r.font.width(value) // 2, y + 2, value, room=40 - 20)]
    lm = "screen.autarkia.loom."
    els += [g.button(8 + t * 54, 43, 52, 12, lm + k) for t, k in enumerate(("protect", "library", "strict"))]
    state = [(lm + "scanning", (100,)), (lm + "ready", (48,)), (lm + "too_big", (48,))] + _keys(lm, ["outside", "no_card", "used"])
    els += [g.edit(30, 59, 76, 13, lm + "name"), g.button(110, 59, 58, 13, lm + "record"),
            g.text(8, 78, state[0][0], *state[0][1], variants=state, room=176 - 16, cut=True)]
    return _title_room(g.slot_screen(r, "pattern_loom", status=False, title="menu.autarkia.pattern_loom", extra=els), 176 - 16)


def _longest_name(r):
    names = [v for k, v in r.lang.items() if k.count(".") == 2 and k.split(".")[0] in ("item", "block") and ".autarkia." in k]
    return max(names, key=r.font.width)


def _matter_buffer(r):
    m = "screen.autarkia.matter_buffer."
    name = _longest_name(r)
    rows = [(m + "row", (MU, 1)), (m + "row", ("16", "65,536"))]
    w = g.gui.LAYOUTS["matter_buffer"]["width"]
    inv_w = r.font.width(r.text_of("container.inventory"))
    scroll_x = 8 + 200 - 3
    dis_x = scroll_x - 2 - 46
    take_x = dis_x - 2 - 28
    text_w = take_x - 2 - 28
    auto_x = w - 8 - 64
    els = [g.button(auto_x, 4, 64, 12, m + "auto_on", variants=[m + "auto_off"]), g.bar(7, 17, 202, 8, "MU bar", 0x1A1424),
           g.text(8, 27, m + "bar", MU, 0, MU, variants=[(m + "bar", (MU, 0, MU)), (m + "bar", ("524,288", "524,288", MU))], room=200),
           g.text(w - 8, 222 - 82 - 11, m + "refused", variants=_keys(m, ["refused", "full"]), right=True, color=0xB01818,
                  room=w - 8 - g.gui.inv_x(g.gui.LAYOUTS["matter_buffer"]) - inv_w)]
    for i in range(4):
        ry = 38 + i * 20
        els += [g.bar(9, ry + 2, 16, 16, f"item {i}", 0x6A6A6A), g.text(28, ry + 2, name, color=0x303030, room=text_w, cut=True),
                g.text(28, ry + 11, m + "row", MU, 1, variants=rows, color=0x5A5A5A, room=text_w, cut=True),
                g.button(take_x, ry + 3, 28, 14, m + "extract"), g.button(dis_x, ry + 3, 46, 14, m + "dissolve", variants=[m + "confirm"])]
    els.append(g.bar(scroll_x, 38, 2, 80, "scroll bar", 0x2A2A2A))
    return _title_room(g.slot_screen(r, "matter_buffer", status=False, extra=els), auto_x - 8)


def _mag_rail(r):
    m = "screen.autarkia.mag_rail."
    to_next = _t(r, m + "to", _t(r, m + "next"))
    to_named = _t(r, m + "to", "North Quarry Stn")
    states = _keys("state.autarkia.mag_rail.", ["ready", "waiting", "no_line", "loop", "over_cap", "no_destination", "no_crate",
                                                 "no_power", "busy", "locked"])
    room = g.gui.LAYOUTS["mag_rail_station"]["width"] - 16
    return _title_room(g.slot_screen(r, "mag_rail_station", status=False, extra=[
        g.edit(8, 18, 96, 12, m + "name"), g.button(108, 17, 120, 14, to_next, variants=[to_named]),
        g.button(138, 4, 44, 12, m + "input", active=False), g.button(184, 4, 44, 12, m + "output"),
        g.button(176, 90, 16, 16, "W", variants=["B"]), g.button(194, 90, 16, 16, "C", variants=["I"]),
        g.bar(28, 94, 64, 5, "energy line", 0x1A1424),
        g.text(8, 110, states[0][0], variants=states, room=room, cut=True),
        g.text(8, 120, m + "line", 2048, 16, room=room, cut=True),
        g.text(8, 130, m + "in_flight", 32, room=room, cut=True)]), 138 - 4 - 8)


def _nanite_hive(r):
    n = "screen.autarkia.nanite_hive."
    pair = _t(r, n + "pair", 32, 32, BIG_COORD, BIG_COORD, 48)
    els = [g.button(8, 18 + 10 * i, 232, 10, ("> " if i == 0 else "") + pair) for i in range(8)]
    els += [g.button(8, 101, 16, 12, "-8"), g.button(25, 101, 16, 12, "-1"), g.button(60, 101, 16, 12, "+1"),
            g.button(77, 101, 16, 12, "+8"), g.button(96, 101, 72, 12, n + "terrace_on", variants=[n + "terrace_off"]),
            g.button(172, 101, 44, 12, n + "start"), g.button(220, 101, 44, 12, n + "stop"),
            g.text(44, 103, "48", room=60 - 44)]
    swarm = [(n + "no_recipe", ()), (n + "needs_paste", ()), (n + "cap_full", ()), (n + "assembling", (100,)), (n + "swarms", (9,))]
    claim = [(n + "no_claim", ())] + [(n + "state", (_t(r, "nanite.autarkia.state." + s), 100, 48, 48))
                                      for s in ("run", "wait_chunk", "wait_buffer", "wait_fe", "paused", "done")]
    info = [(n + "stopped", ()), (n + "no_colony", ()), (n + "no_buffer", ()), (n + "height", (48, 48)), (n + "pick", ())]
    els += [g.text(8, 116, claim[0][0], variants=claim), g.text(8, 126, info[0][0], variants=info),
            g.text(8, 136, swarm[0][0], variants=swarm)]
    return g.slot_screen(r, "nanite_hive", status=False, title="menu.autarkia.nanite_hive", extra=els)


def _compiler(r):
    c = "screen.autarkia.matter_compiler."
    inv_w = r.font.width(r.text_of("container.inventory"))
    state = [(k, ()) for k in g.STATUS if k != "status.autarkia.no_condition"] + _keys(c, ["none", "no_mu", "no_foundation"])
    return g.slot_screen(r, "matter_compiler", status=False, extra=[
        g.bar(24, 18, 6, 52, "MU bar", 0x8A4FFF), g.bar(79, 17, 16, 16, "target ghost", 0x373737), g.button(98, 19, 12, 12, "x"),
        g.text(168, 166 - 82 - 11, state[0][0], variants=state, right=True, room=168 - 8 - inv_w)])


def _conduit(r):
    c = "screen.autarkia.matter_conduit."
    title_w = r.font.width(r.text_of(g.title_key(r, "matter_conduit")))
    inv_w = r.font.width(r.text_of("container.inventory"))
    modes = [_t(r, c + "mode", _t(r, "jade.autarkia.matter_conduit.mode." + k)) for k in ("send", "receive")]
    faces = [_t(r, c + "face", _t(r, c + "face." + k)) for k in ("down", "up", "north", "south", "west", "east")]
    state = [("jade.autarkia.matter_conduit.outside", ()), (c + "channel", (15,)), ("status.autarkia.no_power", ())] + \
        _keys(c, ["no_receiver", "full", "no_target"])
    els = [g.button(86, 17, 82, 14, modes[0], variants=modes[1:]), g.button(86, 33, 82, 14, faces[0], variants=faces[1:]),
           g.text(168, 6, state[0][0], variants=state, right=True, color=0xB01818, room=168 - 8 - title_w),
           g.text(168, 166 - 82 - 11, "jade.autarkia.matter_conduit.rate", 1280, right=True, room=168 - 8 - inv_w)]
    els += [g.bar(88 + (k % 8) * 10, 52 + (k // 8) * 10, 9, 9, f"channel {k}", 0x3F7FD0) for k in range(16)]
    return g.slot_screen(r, "matter_conduit", status=False, extra=els)


def _archive(r):
    w, h = 264, 240
    d = "screen.autarkia.domain_archive."
    inv_w = r.font.width(r.text_of("container.inventory"))
    inv_x = g.gui.inv_x(g.gui.LAYOUTS["domain_archive"])
    status = _keys("jade.autarkia.domain_archive.status.", ["duplicate", "outside", "no_power"])
    reasons = [(d + "row", (_t(r, k), MU)) for k in r.lang if k.startswith(d + "reason.")]
    els, ok = _grid_message(r, 8, 30, 126, 36, [(d + "empty", ()), (d + "no_match", ())], 3)
    els += [g.edit(8, 17, 126, 11, d + "search"),
            g.button(8, 70, 22, 12, "-64"), g.button(30, 70, 16, 12, "-1"), g.button(76, 70, 16, 12, "+1"), g.button(92, 70, 22, 12, "+64"),
            g.text(61 - r.font.width("1728") // 2, 72, "1728", color=0x303030, room=76 - 46),
            g.button(176, 88, 64, 12, d + "to_buffer"), g.button(176, 102, 64, 12, d + "to_conduit"),
            g.bar(176, 116, 12, 12, "channel swatch", 0x3F7FD0), g.text(176 + 12 + 3, 118, d + "channel", 15, room=244 - 2 - 191),
            g.text(w - 8, h - 82 - 11, status[0][0], variants=status, right=True, color=0xB01818, room=w - 8 - (inv_x + inv_w + 8)),
            g.text(141, 17, d + "free", room=115, cut=True), g.text(141, 26, "16,777,216 MU", room=115, cut=True, color=0x2A6A8A)]
    els += [g.text(141, 37 + i * 9, reasons[0][0], *reasons[0][1], variants=reasons + ([(d + "no_log", ())] if i == 0 else []), room=115, cut=True,
                   color=0x5A3A3A) for i in range(5)]
    spec = g.slot_screen(r, "domain_archive", status=False, extra=els)
    spec["overlap_ok"] = ok
    return spec


def register(screens: dict) -> None:
    """Add this module's specs to screens."""
    screens.update({
        "boiler": _boiler, "steam_turbine": _steam_turbine, "fuse_box": _fuse_box, "warehouse_controller": _warehouse,
        "outpost_core": _outpost, "capsule_station": _capsule_station, "ledger_terminal": _ledger, "cable_car_station": _cable_car,
        "depot": _depot, "froth_flotation_plant": _flotation, "cryo_node": _cryo, "superconducting_coil": _coil,
        "plasma_generator": _plasma, "wood_rack": lambda r: _rack(r, "rack_2", "wood_rack", 2048),
        "steel_rack": lambda r: _rack(r, "rack_3", "steel_rack", 8192), "lattice_rack": lambda r: _rack(r, "rack_4", "lattice_rack", 65536),
        "pattern_loom": _pattern_loom, "matter_buffer": _matter_buffer, "mag_rail_station": _mag_rail, "nanite_hive": _nanite_hive,
        "matter_compiler": _compiler, "matter_conduit": _conduit, "domain_archive": _archive,
    })
