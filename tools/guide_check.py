"""Statically check the GuideME Field Manual pages against the GuideME 26.3.4 compiler rules and the mod registries.

Run: F:/envs/5070_11/python.exe tools/guide_check.py
Prints every phase and every problem with page and line; exits 1 when any problem exists.
"""

import gzip
import io
import json
import re
import struct
import sys
from pathlib import Path
from urllib.parse import urljoin

import yaml

ROOT = Path(__file__).resolve().parent.parent
NS = "autarkia"
GUIDE_DIR = ROOT / "src/main/resources/assets/autarkia/guides/autarkia"
RESOURCE_ROOTS = [ROOT / "src/main/resources", ROOT / "src/generated/resources"]
STRUCTURE_COPY_SOURCES = [r / "data/autarkia/structure/colony" for r in RESOURCE_ROOTS]
ITEMS_JAVA = ROOT / "src/main/java/dev/autarkia/registry/AutarkiaItems.java"
BLOCKS_JAVA = ROOT / "src/main/java/dev/autarkia/registry/AutarkiaBlocks.java"
LANG = ROOT / "src/generated/resources/assets/autarkia/lang/en_us.json"
MC_REFS = ROOT / "api/mc/net/minecraft/references"
BLOCKSTATES = ROOT / "src/generated/resources/assets/autarkia/blockstates"

DISPLAYABLE_RECIPE_TYPES = {
    "minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting", "minecraft:blasting",
    "minecraft:smithing_transform", "minecraft:smithing_trim",
}
TOP_TAGS = {
    "a", "br", "div", "Row", "Column", "CommandLink", "CategoryIndex", "FloatingImage", "Color", "ItemGrid", "ItemLink",
    "KeyBind", "PlayerName", "Recipe", "RecipeFor", "RecipesFor", "SubPages", "BlockImage", "ItemImage", "GameScene",
}
SCENE_TAGS = {
    "Entity", "Block", "ImportStructure", "IsometricCamera", "BlockAnnotation", "BoxAnnotation", "LineAnnotation",
    "DiamondAnnotation", "BlockAnnotationTemplate", "RemoveBlocks",
}
BLOCK_TAG_FLOAT = {"float": "string"}
ATTRS = {
    "ItemLink": {"id": "item", "components": "string", "fallback": "string", "optional": "bool"},
    "RecipeFor": {"id": "item", "fallbackText": "string", **BLOCK_TAG_FLOAT},
    "RecipesFor": {"id": "item", "fallbackText": "string", **BLOCK_TAG_FLOAT},
    "Recipe": {"id": "string", "fallbackText": "string", **BLOCK_TAG_FLOAT},
    "SubPages": {"id": "string", "icons": "bool", "alphabetical": "bool", **BLOCK_TAG_FLOAT},
    "GameScene": {"zoom": "float", "padding": "int", "background": "color", "interactive": "bool", "fullWidth": "bool",
                  **BLOCK_TAG_FLOAT},
    "Block": {"id": "block", "x": "int", "y": "int", "z": "int", "p:*": "prop"},
    "ImportStructure": {"src": "string", "pos": "blockpos"},
    "br": {},
}
COLOR_RE = re.compile(r"^#([0-9a-fA-F]{2}){3,4}$")
NAME_START = re.compile(r"[A-Za-z$_]")
NAME_CONT = re.compile(r"[A-Za-z0-9$_\-]")

problems: list[str] = []


def problem(page: str, line: int, text: str) -> None:
    """Record and print one problem."""
    msg = f"{page}:{line}: {text}"
    problems.append(msg)
    print(f"  PROBLEM {msg}")


def phase(title: str) -> None:
    """Print a phase header."""
    print(f"\n== {title}")


def java_ids() -> tuple[set[str], set[str]]:
    """Return the registered Autarkia item and block ids parsed from the registry classes."""
    blocks_src = BLOCKS_JAVA.read_text(encoding="utf-8")
    block_const = dict(re.findall(r"DeferredBlock<[^=]*?>\s+(\w+)\s*=\s*BLOCKS\.\w+\(\s*\"([a-z0-9_/]+)\"", blocks_src, re.S))
    blocks = {f"{NS}:{v}" for v in block_const.values()}
    items_src = ITEMS_JAVA.read_text(encoding="utf-8")
    items = set()
    for _name, helper, args in re.findall(r"DeferredItem<[^=]*?>\s+(\w+)\s*=\s*(\w+)\((.*?)\);", items_src, re.S):
        if helper == "block":
            ref = re.search(r"AutarkiaBlocks\.(\w+)", args)
            if not ref or ref.group(1) not in block_const:
                problem(ITEMS_JAVA.name, 0, f"cannot resolve block item {_name}")
                continue
            items.add(f"{NS}:{block_const[ref.group(1)]}")
        else:
            lit = re.search(r"\"([a-z0-9_/]+)\"", args)
            if not lit:
                problem(ITEMS_JAVA.name, 0, f"cannot resolve item id of {_name}")
                continue
            items.add(f"{NS}:{lit.group(1)}")
    return items, blocks


def vanilla_ids() -> tuple[set[str], set[str]]:
    """Return vanilla item and block ids from the decompiled id reference classes (plain create("x") entries only)."""
    def names(file: str) -> set[str]:
        return {f"minecraft:{n}" for n in re.findall(r"create\(\"([a-z0-9_/]+)\"\)", (MC_REFS / file).read_text(encoding="utf-8"))}
    block_items = names("BlockItemIds.java")
    return block_items | names("ItemIds.java"), block_items | names("BlockIds.java")


def recipes_by_result() -> dict[str, set[str]]:
    """Map each recipe result item id to the recipe type ids that produce it."""
    out: dict[str, set[str]] = {}
    for root in RESOURCE_ROOTS:
        for path in sorted((root / "data").glob("*/recipe/**/*.json")):
            data = json.loads(path.read_text(encoding="utf-8"))
            rtype = data.get("type", "")
            results = []
            if isinstance(data.get("result"), dict):
                results.append(data["result"].get("id"))
            elif isinstance(data.get("result"), str):
                results.append(data["result"])
            for r in data.get("results", []) or []:
                if isinstance(r, dict):
                    results.append(r.get("id") or r.get("item"))
            if "neoforge:conditions" in data:
                rtype += " (conditional)"
            for rid in filter(None, results):
                out.setdefault(rid, set()).add(rtype)
    return out


def structure_assets() -> dict[str, Path]:
    """Return guide asset paths (relative to the guide folder) that exist after processResources."""
    assets: dict[str, Path] = {}
    for root in RESOURCE_ROOTS:
        folder = root / "assets/autarkia/guides/autarkia"
        if folder.is_dir():
            for p in folder.rglob("*"):
                if p.is_file() and p.suffix != ".md":
                    assets[p.relative_to(folder).as_posix()] = p
    for src in STRUCTURE_COPY_SOURCES:
        if src.is_dir():
            for p in src.glob("*.nbt"):
                assets[f"structures/{p.name}"] = p
    return assets


def read_nbt(data: bytes):
    """Parse a gzip-compressed NBT compound and return it as nested Python values."""
    buf = io.BytesIO(gzip.decompress(data))

    def rd(fmt: str):
        size = struct.calcsize(fmt)
        return struct.unpack(">" + fmt, buf.read(size))[0]

    def rstr() -> str:
        return buf.read(rd("H")).decode("utf-8")

    def payload(t: int):
        if t == 1:
            return rd("b")
        if t == 2:
            return rd("h")
        if t == 3:
            return rd("i")
        if t == 4:
            return rd("q")
        if t == 5:
            return rd("f")
        if t == 6:
            return rd("d")
        if t == 7:
            return buf.read(rd("i"))
        if t == 8:
            return rstr()
        if t == 9:
            et, n = rd("b"), rd("i")
            return [payload(et) for _ in range(n)]
        if t == 10:
            comp = {}
            while (ct := rd("b")) != 0:
                key = rstr()
                comp[key] = payload(ct)
            return comp
        if t == 11:
            return [rd("i") for _ in range(rd("i"))]
        if t == 12:
            return [rd("q") for _ in range(rd("i"))]
        raise ValueError(f"unknown NBT tag type {t}")

    if rd("b") != 10:
        raise ValueError("root is not a compound")
    rstr()
    return payload(10)


def block_properties(block_id: str) -> dict[str, set[str]] | None:
    """Return the blockstate property values of an Autarkia block from its generated blockstate file, or None."""
    path = BLOCKSTATES / f"{block_id.split(':', 1)[1]}.json"
    if not path.is_file():
        return None
    data = json.loads(path.read_text(encoding="utf-8"))
    props: dict[str, set[str]] = {}
    keys = list(data.get("variants", {}).keys())
    for part in data.get("multipart", []):
        when = part.get("when", {})
        for cond in when.get("OR", [when]) if isinstance(when, dict) else []:
            keys.append(",".join(f"{k}={v}" for k, v in cond.items()))
    for key in keys:
        for kv in filter(None, key.split(",")):
            k, _, v = kv.partition("=")
            props.setdefault(k, set()).update(v.split("|"))
    return props


def mask_code(body: str) -> str:
    """Blank out fenced code blocks and inline code spans so tags inside them are ignored."""
    out = re.sub(r"(?ms)^```.*?^```", lambda m: re.sub(r"[^\n]", " ", m.group(0)), body)
    return re.sub(r"`[^`\n]*`", lambda m: " " * len(m.group(0)), out)


class Tag:
    """One parsed JSX tag occurrence."""

    def __init__(self, name: str, attrs: list, closing: bool, self_closing: bool, line: int):
        self.name, self.attrs, self.closing, self.self_closing, self.line = name, attrs, closing, self_closing, line


def parse_tags(text: str, offset_line: int, page: str) -> list[Tag]:
    """Tokenize JSX tags the way GuideME's micromark MDX extension does; report inputs that throw ParseException."""
    tags: list[Tag] = []
    i, n = 0, len(text)
    while True:
        i = text.find("<", i)
        if i < 0:
            return tags
        line = offset_line + text.count("\n", 0, i)
        start = i
        nxt = text[i + 1] if i + 1 < n else ""
        if nxt in (" ", "\t", "\n", ""):
            i += 1
            continue
        j = i + 1
        try:
            def ws(k: int) -> int:
                while k < n and text[k] in " \t\r\n":
                    k += 1
                return k

            j = ws(j)
            closing = False
            if j < n and text[j] == "/":
                closing, j = True, ws(j + 1)
            if j < n and text[j] == ">":
                raise ValueError("fragments are not supported by GuideME tag compilers")
            if j >= n or not NAME_START.match(text[j]):
                raise ValueError(f"unexpected character {text[j:j+1]!r} before tag name")
            k = j
            while k < n and NAME_CONT.match(text[k]):
                k += 1
            name = text[j:k]
            if k < n and text[k] in ".:":
                raise ValueError(f"member or prefixed tag names are not supported: {text[j:k+8]!r}")
            j = ws(k)
            attrs = []
            self_closing = False
            while True:
                if j >= n:
                    raise ValueError("unexpected end of file inside tag")
                c = text[j]
                if c == "/":
                    j = ws(j + 1)
                    if j >= n or text[j] != ">":
                        raise ValueError("expected `>` after self-closing slash")
                    self_closing = True
                    j += 1
                    break
                if c == ">":
                    j += 1
                    break
                if c == "{":
                    raise ValueError("spread attribute expressions are not supported")
                if not NAME_START.match(c):
                    raise ValueError(f"unexpected character {c!r} before attribute name")
                k = j
                while k < n and (NAME_CONT.match(text[k]) or text[k] == ":"):
                    k += 1
                aname = text[j:k]
                j = ws(k)
                if j < n and text[j] == "=":
                    j = ws(j + 1)
                    q = text[j] if j < n else ""
                    if q in "\"'" and q:
                        end = text.find(q, j + 1)
                        if end < 0:
                            raise ValueError(f"unterminated attribute value for {aname}")
                        attrs.append((aname, "literal", text[j + 1:end]))
                        j = ws(end + 1)
                    elif q == "{":
                        depth, k = 0, j
                        while k < n:
                            depth += {"{": 1, "}": -1}.get(text[k], 0)
                            if depth == 0:
                                break
                            k += 1
                        if k >= n:
                            raise ValueError(f"unterminated expression for {aname}")
                        attrs.append((aname, "expr", text[j + 1:k].strip()))
                        j = ws(k + 1)
                    else:
                        raise ValueError(f"attribute {aname} value must start with a quote or `{{`")
                else:
                    attrs.append((aname, "bare", None))
            tags.append(Tag(name, attrs, closing, self_closing, line))
            i = j
        except ValueError as e:
            problem(page, line, f"MDX parse error (page shows PARSING ERROR): {e} near {text[start:start+40]!r}")
            i = start + 1


def check_attr(page: str, tag: Tag, aname: str, kind: str, form: str, value, ctx: dict) -> None:
    """Validate one attribute value against the MdxAttrs accessor GuideME uses for it."""
    where = f"<{tag.name}> {aname}"
    if kind in ("string", "item", "block", "int", "color", "blockpos", "prop"):
        if form == "expr":
            problem(page, tag.line, f"{where} is an expression; GuideME reads it as a string literal and errors")
            return
        if form == "bare":
            problem(page, tag.line, f"{where} has no value")
            return
    if kind == "int":
        if not re.fullmatch(r"[+-]?\d+", value):
            problem(page, tag.line, f"{where}='{value}' is not an integer")
    elif kind == "float":
        if form == "bare" or not re.fullmatch(r"[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?", value or ""):
            problem(page, tag.line, f"{where}={value!r} is not a float")
    elif kind == "bool":
        if form == "literal" or (form == "expr" and value not in ("true", "false")):
            problem(page, tag.line, f"{where} must be {{true}}, {{false}} or bare, got {value!r}")
    elif kind == "color":
        if value != "transparent" and not COLOR_RE.match(value):
            problem(page, tag.line, f"{where}='{value}' is not #RRGGBB or #AARRGGBB")
    elif kind == "blockpos":
        if not re.fullmatch(r"\s*-?\d+(\s+-?\d+){0,2}\s*", value):
            problem(page, tag.line, f"{where}='{value}' is not a block position")
    elif kind == "item":
        iid = value.strip() if ":" in value else f"{NS}:{value.strip()}"
        if iid not in ctx["items"]:
            problem(page, tag.line, f"{where}: unknown item {iid}")
    elif kind == "block":
        bid = value.strip() if ":" in value else f"{NS}:{value.strip()}"
        if bid not in ctx["blocks"]:
            problem(page, tag.line, f"{where}: unknown block {bid}")


def check_tag(page: str, page_id: str, tag: Tag, parent: str | None, ctx: dict) -> None:
    """Check tag placement, attributes and the references each GuideME tag compiler resolves."""
    in_scene = parent == "GameScene"
    if in_scene and tag.name not in SCENE_TAGS:
        problem(page, tag.line, f"<{tag.name}> inside GameScene is not a scene element (Unknown scene element)")
        return
    if not in_scene and tag.name not in TOP_TAGS:
        problem(page, tag.line, f"<{tag.name}> is not a registered GuideME tag here (Unhandled MDX element)")
        return
    spec = ATTRS.get(tag.name)
    if spec is None:
        problem(page, tag.line, f"<{tag.name}> has no attribute spec in guide_check.py; extend the checker")
        return
    values = {}
    for aname, form, value in tag.attrs:
        kind = spec.get(aname) or (spec.get("p:*") if aname.startswith("p:") else None)
        if kind is None:
            problem(page, tag.line, f"<{tag.name}> has unknown attribute {aname} (GuideME ignores it)")
            continue
        check_attr(page, tag, aname, kind, form, value, ctx)
        values[aname] = (form, value)
    get = lambda a: values[a][1] if a in values and values[a][0] == "literal" else None
    if tag.name in ("ItemLink", "RecipeFor", "RecipesFor", "Block") and "id" not in values:
        if not (tag.name == "ItemLink" and "fallback" in values):
            problem(page, tag.line, f"<{tag.name}> is missing id")
    iid = get("id")
    if iid is not None and ":" not in iid:
        iid = f"{NS}:{iid}"
    if tag.name == "ItemLink" and iid:
        optional_default = not iid.startswith(NS + ":")
        opt = values.get("optional")
        optional = optional_default if opt is None else (opt[0] == "bare" or opt[1] == "true")
        if iid in ctx["items"] and iid not in ctx["item_index"] and not optional:
            problem(page, tag.line, f"<ItemLink {iid}>: no page lists it in item_ids (No page found for item)")
        if iid.startswith(NS + ":") and f"item.{NS}.{iid[len(NS)+1:]}" not in ctx["lang"] and f"block.{NS}.{iid[len(NS)+1:]}" not in ctx["lang"]:
            problem(page, tag.line, f"<ItemLink {iid}>: no en_us name, shows a raw key")
    if tag.name in ("RecipeFor", "RecipesFor") and iid and iid in ctx["items"]:
        types = ctx["recipes"].get(iid, set())
        shown = types & DISPLAYABLE_RECIPE_TYPES
        if not shown and "fallbackText" not in values:
            what = ", ".join(sorted(types)) or "no recipe at all"
            problem(page, tag.line, f"<{tag.name} {iid}>: no recipe GuideME can show ({what}); shows 'Couldn't find recipe'")
    if tag.name == "Block" and iid and iid in ctx["blocks"]:
        props = block_properties(iid) if iid.startswith(NS + ":") else None
        for aname, form, value in tag.attrs:
            if aname.startswith("p:") and props is None:
                problem(page, tag.line, f"<Block {iid}> {aname}: no generated blockstate file to verify the property against")
            elif aname.startswith("p:"):
                pname = aname[2:]
                if pname not in props:
                    problem(page, tag.line, f"<Block {iid}> {aname}: blockstate file has no property {pname}")
                elif value not in props[pname]:
                    problem(page, tag.line, f"<Block {iid}> {aname}='{value}': not one of {sorted(props[pname])}")
        coords = [get(a) or "0" for a in ("x", "y", "z")]
        if in_scene and all(re.fullmatch(r"-?\d+", c) for c in coords):
            props = {a[2:]: v for a, f, v in tag.attrs if a.startswith("p:")}
            ctx["scene"].append((tuple(int(c) for c in coords), iid, props, tag.line))
    if tag.name == "ImportStructure":
        src = get("src")
        if src is None:
            if "src" not in values:
                problem(page, tag.line, "<ImportStructure> missing src")
            return
        rel = src[1:] if src.startswith("/") else urljoin(page_id, src)
        asset = ctx["assets"].get(rel)
        if asset is None:
            problem(page, tag.line, f"<ImportStructure src='{src}'>: guides/autarkia/{rel} does not exist after processResources")
        else:
            check_structure(page, tag.line, rel, asset, ctx)
            off = [int(v) for v in (get("pos") or "0").split()] + [0, 0]
            for pos, bid, props in ctx["structure_blocks"].get(rel, []):
                ctx["scene"].append((tuple(p + o for p, o in zip(pos, off[:3])), bid, props, tag.line))
    if tag.name == "SubPages":
        sid = get("id")
        target = page_id if sid is None else ("" if sid == "" else sid.split(":", 1)[-1])
        if target and target not in ctx["nav_nodes"]:
            problem(page, tag.line, f"<SubPages>: page {NS}:{target} is not in the navigation tree")


def check_structure(page: str, line: int, rel: str, path: Path, ctx: dict) -> None:
    """Check that a structure file reads as NBT and that every palette block exists."""
    if rel in ctx["structures_checked"]:
        for msg in ctx["structures_checked"][rel]:
            problem(page, line, msg)
        return
    msgs = []
    try:
        nbt = read_nbt(path.read_bytes())
    except Exception as e:
        msgs.append(f"structure {rel}: cannot read NBT ({e})")
        nbt = None
    if nbt is not None:
        palettes = [nbt["palette"]] if "palette" in nbt else nbt.get("palettes", [])
        if not palettes or not nbt.get("blocks"):
            msgs.append(f"structure {rel}: no palette or no blocks")
        if nbt.get("DataVersion") is None:
            msgs.append(f"structure {rel}: no DataVersion; GuideME runs it through the datafixer from version 500")
        for pal in palettes:
            for entry in pal:
                bid = entry.get("id", entry.get("Name", ""))
                if bid not in ctx["blocks"]:
                    msgs.append(f"structure {rel}: palette block {bid!r} is not registered (placed as air)")
        if palettes:
            pal = palettes[0]
            ctx["structure_blocks"][rel] = [
                (tuple(b["pos"]), pal[b["state"]].get("id", pal[b["state"]].get("Name")),
                 pal[b["state"]].get("properties", pal[b["state"]].get("Properties", {}))) for b in nbt.get("blocks", [])]
    ctx["structures_checked"][rel] = msgs
    for msg in msgs:
        problem(page, line, msg)


def check_scene(page: str, line: int, entries: list, ctx: dict) -> None:
    """Check that each multiblock center in a GameScene has a free footprint inside the scene bounds.

    GuideME places only the center block of a Tiny Multiblock Lib machine (its level is client side, so onPlace never
    runs) and sizes and centers the scene on the filled blocks only, so a footprint outside those bounds is clipped.
    """
    filled = {}
    for pos, bid, props, _line in entries:
        if bid in ("minecraft:air", "minecraft:structure_void"):
            filled.pop(pos, None)
        else:
            filled[pos] = (bid, props)
    if not filled:
        problem(page, line, "GameScene has no blocks")
        return
    lo = tuple(min(p[i] for p in filled) for i in range(3))
    hi = tuple(max(p[i] for p in filled) for i in range(3))
    owner = {}
    for pos, (bid, props) in sorted(filled.items()):
        if bid not in ctx["multiblocks"] or props.get("center", "true") != "true":
            continue
        (x0, y0, z0), (x1, y1, z1), geo = ctx["multiblocks"][bid]
        if props.get("facing", "north") != "north":
            problem(page, line, f"{bid} at {pos} faces {props['facing']}; the checker only handles north")
            continue
        if not geo:
            ctx["notes"].append(f"{page}:{line}: {bid} is not a GeckoLib machine; only its center model renders")
        for dx in range(x0, x1 + 1):
            for dy in range(y0, y1 + 1):
                for dz in range(z0, z1 + 1):
                    fp = (pos[0] + dx, pos[1] + dy, pos[2] + dz)
                    if fp in owner:
                        problem(page, line, f"{bid} at {pos} overlaps the footprint of {owner[fp]} at {fp}")
                    owner[fp] = f"{bid} at {pos}"
                    if fp != pos and fp in filled and filled[fp][0] != "minecraft:light":
                        problem(page, line, f"{filled[fp][0]} at {fp} sits inside the footprint of {bid} at {pos}")
        corners = [(pos[0] + x0, pos[1] + y0, pos[2] + z0), (pos[0] + x1, pos[1] + y1, pos[2] + z1)]
        missing = [c for c in corners if any(c[i] < lo[i] or c[i] > hi[i] for i in range(3))]
        if missing:
            problem(page, line, f"{bid} at {pos}: footprint corner(s) {missing} lie outside the scene bounds {lo}..{hi}; "
                                "the model is clipped (add minecraft:light blocks there)")


def check_links(page: str, page_id: str, body: str, offset_line: int, ctx: dict) -> None:
    """Check markdown links and images resolve to existing pages and assets."""
    for m in re.finditer(r"(!?)\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)", body):
        line = offset_line + body.count("\n", 0, m.start())
        href = m.group(2)
        if re.match(r"https?://", href):
            continue
        target = href.split("#", 1)[0]
        if ":" in target:
            ns, _, path = target.partition(":")
            resolved = path if ns == NS else None
        else:
            resolved = target[1:] if target.startswith("/") else urljoin(page_id, target)
        if m.group(1):
            if resolved is None or resolved not in ctx["assets"]:
                problem(page, line, f"image {href} does not exist")
        elif resolved is None or resolved not in ctx["pages"]:
            problem(page, line, f"link {href} points to a page that does not exist")


def main() -> int:
    """Run every phase and return the process exit code."""
    phase("Phase 1: registries and language")
    items, blocks = java_ids()
    v_items, v_blocks = vanilla_ids()
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    blocks_src = BLOCKS_JAVA.read_text(encoding="utf-8")
    multiblocks = {}
    for name, box, geo in re.findall(r"BLOCKS\.\w+\(\s*\"([a-z0-9_]+)\"\s*,\s*p\s*->\s*new\s+BigMachineBlock\([^;]*?"
                                     r"BigMachineBlock\.box\(([-\d,\s]+)\)\s*,\s*(true|false)\)", blocks_src, re.S):
        v = [int(s) for s in box.split(",")]
        multiblocks[f"{NS}:{name}"] = ((v[0], v[1], v[2]), (v[3], v[4], v[5]), geo == "true")
    if len(multiblocks) != blocks_src.count("new BigMachineBlock("):
        problem(BLOCKS_JAVA.name, 0, "could not parse every BigMachineBlock footprint")
    print(f"  {len(items)} Autarkia items, {len(blocks)} blocks ({len(multiblocks)} BigMachineBlock), "
          f"{len(v_items)} vanilla items, {len(v_blocks)} vanilla blocks, {len(lang)} lang keys")
    for iid in sorted(items):
        p = iid.split(":", 1)[1]
        if f"item.{NS}.{p}" not in lang and f"block.{NS}.{p}" not in lang:
            print(f"  note: {iid} has no en_us name")

    phase("Phase 2: recipes and guide assets")
    recipes = recipes_by_result()
    assets = structure_assets()
    print(f"  {sum(len(v) for v in recipes.values())} recipe outputs for {len(recipes)} items; {len(assets)} guide assets")

    phase("Phase 3: pages and frontmatter")
    page_files = sorted(GUIDE_DIR.rglob("*.md"))
    pages: dict[str, dict] = {}
    for path in page_files:
        pid = path.relative_to(GUIDE_DIR).as_posix()
        raw = path.read_text(encoding="utf-8")
        if "\u2014" in raw:
            problem(pid, 0, "contains an em dash")
        fm, body, body_line = {}, raw, 1
        m = re.match(r"---\n(.*?)\n---\n", raw.replace("\r\n", "\n"), re.S)
        if m:
            try:
                fm = yaml.safe_load(m.group(1)) or {}
            except yaml.YAMLError as e:
                problem(pid, 1, f"frontmatter YAML error (page loses navigation and item_ids): {e}")
                fm = {}
            body = raw.replace("\r\n", "\n")[m.end():]
            body_line = m.group(0).count("\n") + 1
        pages[pid] = {"fm": fm if isinstance(fm, dict) else {}, "body": body, "line": body_line}
    print(f"  {len(pages)} pages")

    item_index: dict[str, str] = {}
    nav_nodes = set()
    for pid, pg in pages.items():
        fm = pg["fm"]
        nav = fm.get("navigation")
        if nav is not None:
            if not isinstance(nav, dict):
                problem(pid, 1, "navigation must be a map")
            else:
                if not isinstance(nav.get("title"), str):
                    problem(pid, 1, "navigation.title missing or not a string (frontmatter fails to parse)")
                parent = nav.get("parent")
                if parent is not None:
                    ppath = parent.split(":", 1)[1] if ":" in parent else parent
                    if not isinstance(parent, str) or ppath not in pages or (":" in parent and not parent.startswith(NS + ":")):
                        problem(pid, 1, f"navigation.parent {parent!r} is not an existing page id")
                    elif pages[ppath]["fm"].get("navigation") is None:
                        problem(pid, 1, f"navigation.parent {parent} has no navigation entry itself")
                if "position" in nav and not isinstance(nav["position"], (int, float)):
                    problem(pid, 1, "navigation.position must be a number")
                icon = nav.get("icon")
                if icon is not None:
                    iid = icon if ":" in str(icon) else f"{NS}:{icon}"
                    if iid not in items and iid not in v_items:
                        problem(pid, 1, f"navigation.icon {icon} is not a registered item")
                nav_nodes.add(pid)
        ids = fm.get("item_ids")
        if ids is not None:
            if not isinstance(ids, list):
                problem(pid, 1, "item_ids must be a list")
                continue
            for entry in ids:
                iid = entry if ":" in str(entry) else f"{NS}:{entry}"
                if not isinstance(entry, str):
                    problem(pid, 1, f"item_ids entry {entry!r} is not a string")
                elif iid not in items and iid not in v_items:
                    problem(pid, 1, f"item_ids entry {iid} is not a registered item")
                elif iid in item_index:
                    problem(pid, 1, f"item_ids entry {iid} is also claimed by {item_index[iid]} (index key conflict)")
                else:
                    item_index[iid] = pid
    for pid in sorted(nav_nodes):
        seen, cur = set(), pid
        while cur in nav_nodes and cur not in seen:
            seen.add(cur)
            parent = pages[cur]["fm"]["navigation"].get("parent")
            cur = parent.split(":", 1)[-1] if isinstance(parent, str) else None
        if cur in seen:
            problem(pid, 1, "navigation parent cycle")
    if "index.md" not in pages:
        problem("index.md", 0, "start page index.md is missing")
    print(f"  {len(nav_nodes)} navigation nodes, {len(item_index)} indexed items")

    phase("Phase 4: tags, references, links and structures")
    ctx = {"items": items | v_items, "blocks": blocks | v_blocks, "lang": lang, "recipes": recipes, "assets": assets,
           "item_index": item_index, "nav_nodes": nav_nodes, "pages": pages, "multiblocks": multiblocks,
           "structures_checked": {}, "structure_blocks": {}, "scene": [], "notes": []}
    tag_counts: dict[str, int] = {}
    for pid, pg in pages.items():
        body = mask_code(pg["body"])
        tags = parse_tags(body, pg["line"], pid)
        stack: list[Tag] = []
        for tag in tags:
            tag_counts[tag.name] = tag_counts.get(tag.name, 0) + 1
            if tag.closing:
                if not stack or stack[-1].name != tag.name:
                    problem(pid, tag.line, f"closing </{tag.name}> does not match an open tag")
                else:
                    opened = stack.pop()
                    if tag.name == "GameScene":
                        check_scene(pid, opened.line, ctx["scene"], ctx)
                continue
            if tag.name == "GameScene":
                ctx["scene"] = []
            check_tag(pid, pid, tag, stack[-1].name if stack else None, ctx)
            if tag.name == "GameScene" and tag.self_closing:
                problem(pid, tag.line, "GameScene has no blocks")
            if not tag.self_closing and tag.name != "br":
                stack.append(tag)
            elif tag.name == "br" and not tag.self_closing:
                problem(pid, tag.line, "<br> must be written <br /> (htmlText is disabled)")
        for tag in stack:
            problem(pid, tag.line, f"<{tag.name}> is never closed")
        check_links(pid, pid, body, pg["line"], ctx)
    print("  tags used: " + ", ".join(f"{k}={v}" for k, v in sorted(tag_counts.items())))
    for note in sorted(set(ctx["notes"])):
        print(f"  note: {note}")

    phase("Result")
    if problems:
        print(f"  {len(problems)} problem(s)")
        return 1
    print("  0 problems")
    return 0


if __name__ == "__main__":
    sys.exit(main())
