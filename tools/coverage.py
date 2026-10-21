"""Content coverage check: every canonical block and item id (08a, through tools/status_items.py) up to a milestone
must be registered, have its blockstate or item definition, resolvable models and textures, a language name, a loot
table (blocks) and a survival source (recipe result, loot drop or machine result). Exits 1 when anything is missing.
Blocks that only an item places (05: the Core grows from core_seed, the outpost core from outpost_seed) need that
placing item with a survival source instead of a block item.

Usage: coverage.py [--upto M1]
"""

import json
import re
import sys
from pathlib import Path

import status_items

ROOT = Path(__file__).resolve().parent.parent
GEN = ROOT / "src" / "generated" / "resources"
MAIN = ROOT / "src" / "main" / "resources"
JAVA = ROOT / "src" / "main" / "java" / "dev" / "autarkia" / "registry"
NS = "autarkia"
PLACED_BY = {"colony_core": "core_seed", "outpost_core": "outpost_seed"}


def find(rel):
    for base in (GEN, MAIN):
        p = base / rel
        if p.exists():
            return p
    return None


def load(rel):
    p = find(rel)
    return json.loads(p.read_text(encoding="utf-8")) if p else None


def registered():
    blocks = set(re.findall(r'BLOCKS\.register\w*\("([a-z0-9_]+)"', (JAVA / "AutarkiaBlocks.java").read_text(encoding="utf-8")))
    items_src = (JAVA / "AutarkiaItems.java").read_text(encoding="utf-8")
    items = set(re.findall(r'(?:simple|item)\(\d, "([a-z0-9_]+)"', items_src)) | set(re.findall(r'[aA]rmor\("([a-z0-9_]+)"', items_src))
    for const in re.findall(r"block\(\d, AutarkiaBlocks\.([A-Z0-9_]+)\)", items_src):
        items.add(const.lower())
    return blocks, items


def model_ok(model_id, problems, seen):
    if model_id in seen:
        return
    seen.add(model_id)
    ns, path = model_id.split(":", 1) if ":" in model_id else ("minecraft", model_id)
    if ns != NS:
        return
    m = load(f"assets/{NS}/models/{path}.json")
    if m is None:
        problems.append(f"missing model {model_id}")
        return
    for tex in (m.get("textures") or {}).values():
        if tex.startswith("#"):
            continue
        tns, tpath = tex.split(":", 1) if ":" in tex else ("minecraft", tex)
        if tns == NS and not find(f"assets/{NS}/textures/{tpath}.png"):
            problems.append(f"model {model_id} texture {tex} missing")
    if m.get("parent"):
        model_ok(m["parent"], problems, seen)


def sources():
    out = set()
    for p in list((GEN / "data" / NS / "recipe").rglob("*.json")):
        r = json.loads(p.read_text(encoding="utf-8"))
        res = r.get("result")
        if isinstance(res, dict) and "id" in res:
            out.add(res["id"])
        for e in r.get("results", []):
            out.add(e["id"])
    for p in (GEN / "data" / NS / "loot_table").rglob("*.json"):
        out |= set(re.findall(r'"name": "(autarkia:[a-z0-9_]+)"', p.read_text(encoding="utf-8")))
    return out


def main():
    upto = sys.argv[sys.argv.index("--upto") + 1] if "--upto" in sys.argv else "M10"
    keys = []
    for mkey, (_, groups) in status_items.M.items():
        for items in groups.values():
            keys.extend(items)
        if mkey == upto:
            break
    blocks_want = [k.split(":", 1)[1] for k in keys if k.startswith("block:")]
    items_want = [k.split(":", 1)[1] for k in keys if k.startswith("item:")]
    print(f"phase 1: {len(blocks_want)} blocks and {len(items_want)} items up to {upto}")
    blocks, items = registered()
    lang = load(f"assets/{NS}/lang/en_us.json") or {}
    src = sources()
    seen = set()
    missing = []
    for b in blocks_want:
        p = []
        placer = PLACED_BY.get(b)
        if b not in blocks:
            p.append("not registered as block")
        if placer is None and b not in items:
            p.append("no block item")
        if placer is not None and (placer not in items or f"{NS}:{placer}" not in src):
            p.append(f"placing item {placer} missing or without survival source")
        bs = load(f"assets/{NS}/blockstates/{b}.json")
        if bs is None:
            p.append("no blockstate")
        else:
            for v in re.findall(r'"model": "([^"]+)"', json.dumps(bs)):
                model_ok(v, p, seen)
        if not load(f"data/{NS}/loot_table/blocks/{b}.json"):
            p.append("no loot table")
        if f"block.{NS}.{b}" not in lang:
            p.append("no language name")
        if placer is None and f"{NS}:{b}" not in src:
            p.append("no survival source")
        missing += [f"block {b}: {x}" for x in p]
    print("phase 2: items")
    for i in items_want:
        p = []
        if i not in items:
            p.append("not registered")
        d = load(f"assets/{NS}/items/{i}.json")
        if d is None:
            p.append("no item definition")
        else:
            for v in re.findall(r'"model": "([^"]+)"', json.dumps(d)):
                model_ok(v, p, seen)
        if f"item.{NS}.{i}" not in lang:
            p.append("no language name")
        if f"{NS}:{i}" not in src:
            p.append("no survival source")
        missing += [f"item {i}: {x}" for x in p]
    print("phase 3: language keys named in Java sources")
    used = set()
    for src_file in (ROOT / "src" / "main" / "java").rglob("*.java"):
        used |= set(re.findall(r'"((?:[a-z_]+\.)+autarkia\.[a-z0-9_.]+|autarkia\.[a-z0-9_]+\.[a-z0-9_.]+)"', src_file.read_text(encoding="utf-8")))
    missing += [f"language key {k}: not in en_us.json" for k in sorted(used) if not k.endswith(".") and k not in lang]
    print(f"done: {len(blocks_want) + len(items_want)} ids and {len(used)} language keys checked, {len(missing)} missing")
    for m in missing:
        print("  MISSING", m)
    sys.exit(1 if missing else 0)


if __name__ == "__main__":
    main()
