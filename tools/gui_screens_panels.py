"""Widget specs of the code-drawn panel screens: colony (core, project board, tablet), multimeter and linked sites (see gui_screens)."""

from gui_screens import bar, button, edit, text

C = "screen.autarkia.colony."
W, H = 330, 220
TABS = ["overview", "projects", "jobs", "workers", "settings", "patterns"]
AUTONOMY = ["manual", "assisted", "autonomous"]
ROLES = ["generic", "line", "generator", "storage", "defense", "outpost"]
WORKER_TYPES = ["porter", "hauler_crab", "lineman", "loader", "builder_drone", "cable_layer_drone", "surveyor", "sentinel", "nanite_swarm",
                "replicator_walker"]
JOB_TYPES = ["fetch", "carry", "deliver", "level_cut", "level_fill", "place", "string_cable", "crank", "sign", "collect_debris", "warehouse_deliver",
             "warehouse_store", "cut_span", "survey", "escort", "found_outpost"]
JOB_STATUS = ["open", "assigned", "done", "failed", "blocked"]
FAILURES = ["no_path", "item_missing", "target_obstructed", "worker_lost", "chunk_unloaded", "no_power", "timeout"]
WORKER_STATES = ["idle", "assigned", "fetching", "travelling", "working", "returning", "recharging", "failed", "waiting", "unloaded"] + \
    [f"failed:{f}" for f in FAILURES]
TIERS = ["rope", "copper", "insulated", "hv", "superconductor", "phase"]
GOLD, LIGHT, GREY, DIM, LOG = 0xF2D3A0, 0xDDDDDD, 0xBBBBBB, 0x888888, 0xAAAAAA
N = 99999
POS = (-29999, -64, -29999)
SHORT_POS = "-29999, -64, -29999"
COLONY_NAME = "The Great Northern Mining Colony"
PATTERN_NAME = "Main Smelting Line With Overflow"
DEPOT_NAME = "Northern Vantite Mine Depot No 2"
LABOR = "deliver 999  level_cut 999  level_fill 999  place 999  string_cable 99  sign 1"
WARNING = f"Rack critical at {SHORT_POS}: 150%, collapse in 60 s"


def split(r, s: str, width: int) -> list[str]:
    """Return the lines Minecraft's Font.split breaks s into: at the last space before the width, else before the first char past it."""
    lines, start = [], 0
    while start < len(s):
        w, space, end = 0, -1, None
        for i in range(start, len(s)):
            if s[i] == " ":
                space = i
            w += r.font.width(s[i])
            if i > start and w > width:
                end = i
                break
        if end is None:
            lines.append(s[start:])
            break
        cut = space if space != -1 else end
        lines.append(s[start:cut])
        start = cut + 1 if s[cut] == " " else cut
    return lines


def _wrap(r, els: list, s: str, x: int, y: int, width: int, color: int, max_y: int) -> int:
    fit = max(0, (max_y - 9 - y) // 10 + 1)
    if fit:
        els.append(text(x, y, s, color=color, room=width, shadow=True, lines=fit, line_h=10))
    return y + 10 * min(fit, len(split(r, s, width)))


def _by_width(r, keys: list[str], args=()) -> list[str]:
    return sorted(keys, key=lambda k: -r.font.width(r.text_of(k, args)))


def _templates(r) -> list[str]:
    return _by_width(r, [k for k in r.lang if k.startswith("colony_template.autarkia.")])


def _panel(els: list) -> dict:
    return dict(size=(W, H), panel="colony_panel", elements=els)


def _tabs(active: int) -> list:
    return [button(4 + i * 54, 4, 52, 16, C + "tab." + t, active=i != active) for i, t in enumerate(TABS)]


def _autonomy(x: int, y: int, now: str = "assisted") -> list:
    return [button(x + i * 82, y, 80, 16, "colony.autonomy.autarkia." + lv, active=lv != now) for i, lv in enumerate(AUTONOMY)]


def _log_variants(r) -> list:
    tpl = r.text_of(_templates(r)[0])
    keyed = [("colony.log.autarkia." + k, (tpl,)) for k in ("building", "no_anchor", "no_site", "waiting_items")]
    keyed += [("colony.log.autarkia.proposed", (tpl, N)), ("colony.log.autarkia.rack_critical", (SHORT_POS, 150, 60))]
    raw = [f"Job warehouse_deliver at {SHORT_POS} waits: a worker still carries lattice_carapace_chest",
           f"Job warehouse_deliver at {SHORT_POS} failed: target_obstructed", "Blocked restoration_projector: target_obstructed",
           f"A Replicator Walker was lost at {SHORT_POS}; its kit is gone", f"Pattern {PATTERN_NAME} recorded: {N} blocks",
           "Seed worker cable_layer_drone has no cap room; its parts went to the outpost depot",
           f"Restoration field at {SHORT_POS}: outside the domain"]
    return keyed + [(s, ()) for s in raw]


def colony_overview(r) -> dict:
    """Return the Core Overview tab for the owner: 8 fitted stat lines, the power graph, 7 log lines, the rename row."""
    els = _tabs(0)
    gx = W - 8 - 24 * 5
    width = gx - 14
    autonomy = [r.text_of("colony.autonomy.autarkia." + a) for a in AUTONOMY]
    lines = [[(C + "title", (COLONY_NAME, 6))], [(C + "domain", (193, 193))], [(C + "supply", ("99999.0",))], [(C + "demand", ("99999.0",))],
             [(C + "storage", (100,))], [(C + "workers", (32, 32))], [(C + "autonomy", (a,)) for a in autonomy],
             [(C + "budget", ("99999.0", N, N))]]
    colors = [GOLD, LIGHT, 0x4DCCFF, 0xFFA040, 0xB8E8D6, LIGHT, LIGHT, 0xFFC27A]
    for i, (v, c) in enumerate(zip(lines, colors, strict=True)):
        els.append(text(8, 26 + i * 11, v[0][0], *v[0][1], variants=v, color=c, room=width, cut=True, shadow=True))
    gy, gh = 28, 60
    els.append(bar(gx - 1, gy - 1, 24 * 5 + 2, gh + 2, "power graph", 0x333333))
    made_w = r.font.width(r.text_of(C + "graph_supply"))
    els += [text(gx, gy + gh + 3, C + "graph_supply", color=0x4DCCFF, room=W - 8 - gx, shadow=True),
            text(gx + made_w + 8, gy + gh + 3, C + "graph_demand", color=0xFFA040, room=W - 8 - gx - made_w - 8, shadow=True),
            text(gx, gy + gh + 13, C + "graph_samples", 24, color=DIM, room=W - 8 - gx, shadow=True)]
    logs = _log_variants(r)
    for i in range(7):
        els.append(text(8, 26 + 92 + i * 10, logs[0][0], *logs[0][1], variants=logs, color=LOG, room=W - 16, cut=True, shadow=True))
    els += [edit(8, H - 22, 160, 16, COLONY_NAME), button(172, H - 22, 70, 16, C + "rename")]
    return _panel(els)


def _action_row(r, labels: list[str], by: int) -> tuple[list, int]:
    bx, right = 112, W - 8
    cx, cy, els = bx, by, []
    for key in labels:
        bw = r.font.width(r.text_of(key)) + 6
        if cx + bw > right:
            cx = bx
            cy -= 18
        els.append(button(cx, cy, bw, 16, key))
        cx += bw + 2
    return els, cy


def _project_card(r, state: str, block: str, top: int, max_y: int) -> list:
    els = []
    x, wide = 116, W - 116 - 8
    state_key = "colony.state.autarkia." + state
    sw = r.font.width(r.text_of(state_key))
    els.append(text(x + wide, top, state_key, color=0x9BE7FF, right=True, room=wide, shadow=True))
    ty = _wrap(r, els, r.text_of(_templates(r)[0]), x, top, wide - sw - 6, GOLD, max_y)
    reason = _by_width(r, [k for k in r.lang if k.startswith("colony.reason.autarkia.")], (N,))[0]
    ty = _wrap(r, els, r.text_of(reason, (N,)), x, ty + 1, wide, LIGHT, max_y)
    if block:
        ty = _wrap(r, els, r.text_of(C + "blocked", (r.text_of("jade.autarkia.worker.reason." + block),)), x, ty + 1, wide, 0xFF6B6B, max_y)
    ty = _wrap(r, els, r.text_of(C + "cost", ("99999.0", "99999.0")), x, ty + 1, wide, 0xFFC27A, max_y)
    bom_max = max_y
    ty = _wrap(r, els, r.text_of(C + "bom"), x, ty + 5, wide, GOLD, bom_max)
    items = _by_width(r, [k for k in r.lang if k.startswith("block.autarkia.")])[:6]
    for i, item in enumerate(items):
        line = r.text_of(C + "bom_line", (r.text_of(item), N, N - 1, N))
        lines = len(split(r, line, wide - 4))
        last = i == len(items) - 1
        if ty + lines * 10 + (0 if last else 10) > bom_max + 1:
            ty = _wrap(r, els, r.text_of(C + "bom_more", (len(items) - i,)), x + 4, ty, wide - 4, DIM, bom_max)
            break
        ty = _wrap(r, els, line, x + 4, ty, wide - 4, 0xFF9B6B, bom_max)
    ty = _wrap(r, els, r.text_of(C + "priority", ("99999.0", -N)), x, ty + 3, wide, GREY, max_y)
    ty = _wrap(r, els, r.text_of(C + "site", (*POS, "counterclockwise_90", "99999.0", N)), x, ty + 1, wide, GREY, max_y)
    _wrap(r, els, r.text_of(C + "labor", (LABOR,)), x, ty + 1, wide, LOG, max_y)
    return els


def _projects(r, mode: str, state: str, actions: list[str], block: str = "", manage: bool = True) -> dict:
    core = mode == "core"
    els = _tabs(1) if core else []
    top = 24 if core else 6
    for i, t in enumerate(_templates(r)[:9]):
        els.append(button(4, top + i * 18, 104, 16, t, active=i != 0))
    els.append(text(8, top + 9 * 18, C + "bom_more", N, color=DIM, room=100, shadow=True))
    labels = [C + "show"] + ([C + a for a in actions] + ["+10", "-10"] if manage else [])
    row, cy = _action_row(r, labels, H - (42 if mode == "tablet" and manage else 22))
    els += row
    wide = W - 116 - 8
    max_y = cy - 3 - 10 * len(split(r, WARNING, wide))
    _wrap(r, els, WARNING, 116, max_y + 1, wide, 0xFF4040, cy)
    els += _project_card(r, state, block, 26 if core else 8, max_y)
    if mode == "tablet" and manage:
        els += _autonomy(4, H - 20)
    if not manage:
        note = r.text_of(C + "view_only")
        _wrap(r, els, note, 8, H - 4 - 10 * len(split(r, note, 100)), 100, LOG, H - 2)
    return _panel(els)


def _rows(r, key: str, label: str) -> list:
    els = _tabs(2 if key == "jobs" else 3) + [text(8, 26, C + key + "_header", color=GOLD, shadow=True)]
    if key == "jobs":
        assignee = r.text_of(_by_width(r, ["entity.autarkia." + t for t in WORKER_TYPES])[0])
        v = [(C + "job_row", (t, *POS, assignee, N, s)) for t in JOB_TYPES for s in JOB_STATUS]
        width = W - 82
    else:
        names = [r.text_of("entity.autarkia." + t) for t in WORKER_TYPES]
        states = [r.text_of(C + "worker_failed", (r.text_of("jade.autarkia.worker.reason." + s[7:]),)) if s.startswith("failed:")
                  else r.text_of(C + "worker_state." + s) for s in WORKER_STATES]
        v = [(C + "worker_row", (n, s, j)) for n in names for s in states for j in JOB_TYPES]
        width = 186
    for i in range(10):
        ry = 38 + i * 17
        els.append(text(8, ry, v[0][0], *v[0][1], variants=v, color=LIGHT, room=width, cut=True, shadow=True))
        if key == "workers":
            els.append(bar(200, ry + 1, 50, 6, f"charge bar {i}"))
        els.append(button(W - 66, 36 + i * 17, 60, 15, C + label))
    return els


def colony_settings(r) -> dict:
    """Return the Core Settings tab for the owner on template page 1: autonomy, 10 template toggles, signs, paging, stock and trust rows."""
    els = _tabs(4) + [text(8, 26, C + "settings_title", color=GOLD, shadow=True)] + _autonomy(8, 36)
    tpl = _templates(r)
    for k, t in enumerate(tpl[:10]):
        els.append(button(8 + (k % 2) * 158, 58 + (k // 2) * 18, 156, 16, "[x] " + r.text_of(t)))
    els.append(button(8, 150, 104, 16, C + "signs_on", variants=[C + "signs_off"]))
    pages = max(1, (len(tpl) + 9) // 10)
    if pages > 1:
        page = [(f"{p} / {pages}", ()) for p in range(1, pages + 1)]
        pw = max(r.font.width(p[0]) for p in page)
        els += [button(W - 80, 150, 20, 16, "<", active=False), button(W - 28, 150, 20, 16, ">"),
                text(W - 44 - pw // 2, 154, page[0][0], variants=page, color=LIGHT, room=W - 28 - (W - 60), shadow=True)]
    els += [edit(8, 172, 130, 16, "minecraft:coal"), edit(142, 172, 40, 16, "64"), button(186, 172, 50, 16, C + "stock_set"),
            edit(8, 194, 130, 16, C + "trust_name"), button(142, 194, 50, 16, C + "trust")]
    return _panel(els)


def colony_patterns(r) -> dict:
    """Return the Core Patterns tab with manage rights: 6 library rows with role, toggle and Remove, 3 instances with Watch."""
    els = _tabs(5) + [text(8, 26, C + "pattern_header", 6, N, color=GOLD, shadow=True)]
    rows = [(C + "pattern_row", (PATTERN_NAME, "96x48x96", N)), (C + "pattern_missing", ())]
    for i in range(6):
        els.append(text(8, 42 + i * 18, rows[0][0], *rows[0][1], variants=rows, color=LIGHT, room=164, cut=True, shadow=True))
        ry = 38 + i * 18
        roles = [C + "pattern_role." + ro for ro in ROLES]
        els += [button(176, ry, 56, 16, roles[0], variants=roles[1:]), button(234, ry, 26, 16, "[x]", variants=["[ ]"]),
                button(262, ry, 62, 16, C + "pattern_remove")]
    els.append(text(8, 150, C + "pattern_instances", N, color=GOLD, shadow=True))
    watched = [r.text_of(C + "pattern_watched"), r.text_of(C + "pattern_unwatched")]
    inst = [(C + "pattern_instance", (PATTERN_NAME, *POS, w)) for w in watched]
    for i in range(3):
        els += [text(8, 162 + i * 12, inst[0][0], *inst[0][1], variants=inst, color=GREY, room=250, cut=True, shadow=True),
                button(262, 160 + i * 12, 62, 11, C + "pattern_watch", variants=[C + "pattern_unwatch"])]
    els.append(button(8, H - 22, 120, 16, C + "pattern_add"))
    return _panel(els)


def multimeter(r) -> dict:
    """Return the multimeter reading of a connected node with 3 spans that all show heat (height as the Java computes it)."""
    w, pad, line = 280, 8, 11
    h = pad * 2 + line * 7 + 3 * (line * 3 + 4)
    m = "multimeter.autarkia."
    fe = "131072.0 FE/t"
    col = (w - pad * 2) // 2
    tiers = [r.text_of("tier.autarkia." + t) for t in TIERS]
    els = [text(pad, pad, m + "node", *POS, color=GOLD, room=w - 2 * pad, cut=True, shadow=True)]
    ty = pad + line
    els.append(text(pad, ty, m + "network", variants=[(m + "network", (N, t, N, N)) for t in tiers], color=LIGHT, room=w - 2 * pad, cut=True,
                    shadow=True))
    ty += line
    els += [text(pad, ty, m + "service", 100, color=LIGHT, room=90 - pad, shadow=True), bar(90, ty + 1, w - 90 - pad, 7, "service bar")]
    ty += line
    els += [text(pad, ty, m + "generation", fe, color=0x9BE7FF, room=col - 4, cut=True, shadow=True),
            text(pad + col, ty, m + "demand", fe, color=0xFFC27A, room=col, cut=True, shadow=True)]
    ty += line
    els.append(text(pad, ty, m + "loss", fe, color=0xFF8C8C, room=w - 2 * pad, cut=True, shadow=True))
    ty += line
    els.append(text(pad, ty, m + "stored", 2000000000, 2000000000, color=0xB8E8D6, room=w - 2 * pad, cut=True, shadow=True))
    ty += line
    rho = [(m + "rho", ("0.0001",)), (m + "rho_lagging", ("0.0001",))]
    els.append(text(pad, ty, rho[0][0], *rho[0][1], variants=rho, color=DIM, room=w - 2 * pad, shadow=True))
    ty += line
    span = [(m + "span_flow", (N, "64.0", "1048576.0 FE/t")), (m + "span_idle", (N, "64.0"))]
    for i in range(3):
        els.append(text(pad, ty, span[0][0], *span[0][1], variants=span, color=0xE0E0E0, room=w - 2 * pad, cut=True, shadow=True))
        ty += line
        els += [text(pad + 4, ty, m + "load", color=LOG, room=50 - pad - 4, shadow=True), bar(50, ty + 1, 70, 7, f"load bar {i}"),
                text(126, ty, m + "wear", color=LOG, room=160 - 126, shadow=True), bar(160, ty + 1, 68, 7, f"wear bar {i}")]
        ty += line
        els += [text(pad + 4, ty, m + "heat", color=LOG, room=50 - pad - 4, shadow=True), bar(50, ty + 1, 70, 7, f"heat bar {i}")]
        ty += line + 4
    return dict(size=(w, h), panel="multimeter_panel", elements=els)


def sites(r, ledger: bool) -> dict:
    """Return the linked stock view with 8 sites, 3 orders and 10 stock rows of all sites; ledger adds the request controls."""
    w, h, s = 320, 230, "screen.autarkia.sites."
    names = [(s + "main", ()), (s + "depot", POS), (DEPOT_NAME, ())]
    pct_w = r.font.width("100%")
    els = [text(8, 8, s + "title", color=0xE0F0F0, room=w - 16, shadow=True)]
    for i in range(8):
        ry = 22 + i * 14
        v = names[:1] if i == 0 else names[1:]
        els += [text(10, ry + 2, v[0][0], *v[0][1], variants=v, color=0xE0E0E0, room=100 - pct_w, cut=True, shadow=True),
                text(116, ry + 2, "100%", color=0x9BE7FF, right=True, room=116 - 10, shadow=True)]
    oy = 22 + 8 * 14 + 4
    els.append(text(8, oy, s + "orders", color=0xB0B0B0, room=118 - 8, shadow=True))
    orders = [(s + "order." + st, (1728, 1728)) for st in ("waiting", "moving", "done", "partial", "failed")]
    orders += [(s + "reason." + k, ()) for k in ("no_transport", "none_available")]
    for i in range(3):
        els += [bar(8, oy + 10 + i * 17, 16, 16, f"order item {i}", 0x8B6B4B),
                text(26, oy + 14 + i * 17, orders[0][0], *orders[0][1], variants=orders, color=LIGHT, room=92, cut=True, shadow=True)]
    count = "1769472"
    count_w = r.font.width(count)
    for i in range(10):
        ry = 22 + i * 16
        els += [bar(124, ry, 16, 16, f"stock item {i}", 0x8B6B4B), text(144, ry + 4, count, color=0xFFFFFF, shadow=True),
                text(w - 10, ry + 4, names[0][0], variants=names, color=0x9BE7FF, right=True, room=w - 10 - 148 - count_w, cut=True, shadow=True)]
    page = "99 / 99"
    els += [button(8, h - 22, 40, 16, s + "back"), button(124, h - 22, 16, 16, "<"), button(196, h - 22, 16, 16, ">"),
            text(168 - r.font.width(page) // 2, h - 18, page, color=0xE0E0E0, room=196 - 140, shadow=True)]
    if ledger:
        am = "1728"
        els += [button(124, h - 42, 24, 16, "-64"), button(184, h - 42, 24, 16, "+64"), button(214, h - 42, 98, 16, s + "request"),
                text(166 - r.font.width(am) // 2, h - 38, am, color=0xE0E0E0, room=184 - 148, shadow=True)]
    return dict(size=(w, h), panel="sites_panel", elements=els)


def register(screens: dict) -> None:
    """Add this module's specs to screens."""
    screens["colony_core_overview"] = colony_overview
    screens["colony_core_projects"] = lambda r: _projects(r, "core", "proposed", ["approve", "deny", "rotate"])
    screens["colony_core_projects_blocked"] = lambda r: _projects(r, "core", "blocked", ["retry", "cancel"], "target_obstructed")
    screens["colony_core_jobs"] = lambda r: _panel(_rows(r, "jobs", "cancel"))
    screens["colony_core_workers"] = lambda r: _panel(_rows(r, "workers", "recall"))
    screens["colony_core_settings"] = colony_settings
    screens["colony_core_patterns"] = colony_patterns
    screens["project_board"] = lambda r: _projects(r, "project", "commissioning", ["cancel"])
    screens["planning_tablet"] = lambda r: _projects(r, "tablet", "proposed", ["approve", "deny", "rotate"])
    screens["planning_tablet_view_only"] = lambda r: _projects(r, "tablet", "proposed", [], manage=False)
    screens["multimeter"] = multimeter
    screens["sites_ledger"] = lambda r: sites(r, True)
    screens["sites_depot"] = lambda r: sites(r, False)
