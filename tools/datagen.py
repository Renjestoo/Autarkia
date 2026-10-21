"""Helpers that write vanilla 26.3 JSON formats (models, blockstates, item definitions, loot, recipes, tags,
advancements, worldgen) for tools/data.py. Content modules call these helpers; tags and lang accumulate and
are written at the end."""

import gzip
import importlib
import json
import struct
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GEN = ROOT / "src" / "generated" / "resources"
NS = "autarkia"
LANG = {}
TAGS = defaultdict(lambda: defaultdict(set))
COUNT = [0]
CONTENT_MODULES = ["content_base", "content_m1", "content_m2", "content_m3", "content_m4"]
DATA_VERSION = 5023


def rl(path):
    return path if ":" in path else f"{NS}:{path}"


def write_json(rel, obj):
    p = GEN / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    COUNT[0] += 1


def name(key, text):
    LANG[key] = text


def tag(kind, tag_id, *entries):
    for e in entries:
        TAGS[kind][tag_id].add(e)


def block_model(name_, obj):
    write_json(f"assets/{NS}/models/block/{name_}.json", obj)


def item_model(name_, obj):
    write_json(f"assets/{NS}/models/item/{name_}.json", obj)


def item_def(name_, model=None, obj=None):
    write_json(f"assets/{NS}/items/{name_}.json", obj or {"model": {"type": "minecraft:model", "model": model or rl(f"item/{name_}")}})


def blockstate(name_, obj):
    write_json(f"assets/{NS}/blockstates/{name_}.json", obj)


def simple_blockstate(name_, model=None):
    blockstate(name_, {"variants": {"": {"model": model or rl(f"block/{name_}")}}})


def cube_all(name_, tex=None):
    block_model(name_, {"parent": "minecraft:block/cube_all", "textures": {"all": rl(f"block/{tex or name_}")}})


def cube_column(name_, side, end):
    block_model(name_, {"parent": "minecraft:block/cube_column", "textures": {"side": rl(f"block/{side}"), "end": rl(f"block/{end}")}})


def orientable(name_, front, side, top, bottom=None):
    block_model(name_, {"parent": "minecraft:block/orientable_with_bottom", "textures": {
        "front": rl(f"block/{front}"), "side": rl(f"block/{side}"), "top": rl(f"block/{top}"),
        "bottom": rl(f"block/{bottom or top}")}})


def horizontal_states(name_, model=None, prop="facing", extra=None):
    variants = {}
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for d, y in rot.items():
        for suffix, m in (extra or {"": model or rl(f"block/{name_}")}).items():
            key = f"{prop}={d}" + (f",{suffix}" if suffix else "")
            v = {"model": m}
            if y:
                v["y"] = y
            variants[key] = v
    blockstate(name_, {"variants": variants})


def block_item(name_, model=None):
    item_def(name_, model=model or rl(f"block/{name_}"))


def generated_item(name_, tex=None, handheld=False):
    item_model(name_, {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
                       "textures": {"layer0": rl(f"item/{tex or name_}")}})
    item_def(name_)


def particle_model(name_, tex=None):
    block_model(name_, {"textures": {"particle": rl(f"block/{tex or name_}")}})


def loot_self(name_):
    """Write a 26.3 block loot table that drops the block itself when it survives the explosion."""
    write_json(f"data/{NS}/loot_table/blocks/{name_}.json", {
        "type": "minecraft:block", "random_sequence": rl(f"blocks/{name_}"),
        "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item", "name": rl(name_)}]}]})


def loot_center_only(name_):
    """Write a 26.3 big-block loot table that drops the item only for the center position."""
    write_json(f"data/{NS}/loot_table/blocks/{name_}.json", {
        "type": "minecraft:block", "random_sequence": rl(f"blocks/{name_}"),
        "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item", "name": rl(name_),
                                "condition": {"type": "minecraft:match_block", "blocks": rl(name_), "state": {"center": "true"}}}]}]})


def loot_ore(name_, drop, count=None):
    """Write a 26.3 ore loot table: silk touch drops the ore, else the drop with fortune ore_drops and explosion decay."""
    child = {"type": "minecraft:item", "name": rl(drop) if ":" not in drop else drop,
             "modifier": ([{"type": "minecraft:set_count", "count": count, "add": False}] if count else []) + [
                 {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                 {"type": "minecraft:explosion_decay"}]}
    write_json(f"data/{NS}/loot_table/blocks/{name_}.json", {
        "type": "minecraft:block", "random_sequence": rl(f"blocks/{name_}"),
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": rl(name_)}, child]}]}]})


def shaped(out, pattern, key, count=1, category="misc", rid=None):
    k = {}
    for ch, v in key.items():
        k[ch] = v if v.startswith("#") or ":" in v else rl(v)
    res = {"id": rl(out) if ":" not in out else out}
    if count != 1:
        res["count"] = count
    write_json(f"data/{NS}/recipe/{rid or out.split(':')[-1]}.json",
               {"type": "minecraft:crafting_shaped", "category": category, "key": k, "pattern": pattern, "result": res})


def shapeless(out, ingredients, count=1, category="misc", rid=None):
    ing = [i if i.startswith("#") or ":" in i else rl(i) for i in ingredients]
    res = {"id": rl(out) if ":" not in out else out}
    if count != 1:
        res["count"] = count
    write_json(f"data/{NS}/recipe/{rid or out.split(':')[-1]}.json",
               {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ing, "result": res})


def cooking(kind, out, ingredient, xp=0.7, time=None, rid=None):
    t = {"smelting": 200, "blasting": 100}[kind] if time is None else time
    ing = ingredient if ingredient.startswith("#") or ":" in ingredient else rl(ingredient)
    write_json(f"data/{NS}/recipe/{rid}.json", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": ing,
                                                 "result": {"id": rl(out) if ":" not in out else out}, "experience": xp, "cookingtime": t})


def machine_recipe(rtype, rid, inputs, results, stations=None, **extra):
    obj = {"type": rl(rtype)}
    if stations:
        obj["stations"] = stations
    if inputs:
        obj["inputs"] = [{"ingredient": i if i.startswith("#") or ":" in i else rl(i), "count": c} for i, c in inputs]
    res = []
    for r in results:
        rid_, cnt = r[0], r[1]
        e = {"id": rid_ if ":" in rid_ else rl(rid_), "count": cnt}
        if len(r) > 2 and r[2] < 1.0:
            e["chance"] = r[2]
        res.append(e)
    obj["results"] = res
    obj.update(extra)
    write_json(f"data/{NS}/recipe/{rtype}/{rid}.json", obj)


def advancement(path, title, desc, icon, parent=None, criteria=None, frame="task", background=None, hidden=False):
    disp = {"icon": {"id": rl(icon) if ":" not in icon else icon}, "title": {"translate": f"advancements.{NS}.{path.replace('/', '.')}.title"},
            "description": {"translate": f"advancements.{NS}.{path.replace('/', '.')}.description"}, "frame": frame,
            "show_toast": True, "announce_to_chat": True, "hidden": hidden}
    if background:
        disp["background"] = background
    obj = {"display": disp, "criteria": criteria or {"done": {"trigger": "minecraft:impossible"}}}
    if parent:
        obj["parent"] = rl(parent)
    write_json(f"data/{NS}/advancement/{path}.json", obj)
    name(f"advancements.{NS}.{path.replace('/', '.')}.title", title)
    name(f"advancements.{NS}.{path.replace('/', '.')}.description", desc)


def inv_changed(item=None, tag_id=None):
    items = {"items": f"#{tag_id}" if tag_id else rl(item)}
    return {"got": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [items]}}}


def _height_target(tag_id, lo, hi):
    return {"predicate_type": "minecraft:any_of", "rules": [
        {"predicate_type": "minecraft:all_of", "rules": [
            {"predicate_type": "minecraft:tag_match", "tag": "minecraft:height_specific_ore_replaceables"},
            {"predicate_type": "minecraft:height_match", "min_inclusive": lo, "max_inclusive": hi}]},
        {"predicate_type": "minecraft:all_of", "rules": [
            {"predicate_type": "minecraft:not", "rule": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:height_specific_ore_replaceables"}},
            {"predicate_type": "minecraft:tag_match", "tag": tag_id}]}]}


def ore_targets(stone_state, deep_state):
    """Return vanilla 26.3 ore targets (stone and deepslate, tuff by height like vanilla ores)."""
    return [{"state": rl(stone_state), "target": _height_target("minecraft:stone_ore_replaceables", 0, 2031)},
            {"state": rl(deep_state), "target": _height_target("minecraft:deepslate_ore_replaceables", -2032, 8)}]


def ore_feature(fid, size, targets, discard=0.0):
    write_json(f"data/{NS}/worldgen/feature/{fid}.json",
               {"type": "minecraft:ore", "size": size, "discard_chance_on_air_exposure": discard, "targets": targets})


def placed_feature(pid, feature, placement):
    write_json(f"data/{NS}/worldgen/placed_feature/{pid}.json", {"feature": rl(feature), "placement": placement})


def height_range(kind, lo, hi):
    return {"type": "minecraft:height_range", "height": {"type": f"minecraft:{kind}", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}}


def biome_modifier(mid, biomes, features, step):
    write_json(f"data/{NS}/neoforge/biome_modifier/{mid}.json",
               {"type": "neoforge:add_features", "biomes": biomes, "features": [rl(f) for f in features], "step": step})


def _nbt_payload(value):
    if isinstance(value, bool):
        return 1, struct.pack(">b", int(value))
    if isinstance(value, int):
        return 3, struct.pack(">i", value)
    if isinstance(value, str):
        raw = value.encode("utf-8")
        return 8, struct.pack(">H", len(raw)) + raw
    if isinstance(value, list):
        items = [_nbt_payload(v) for v in value]
        kind = items[0][0] if items else 0
        return 9, struct.pack(">bi", kind, len(items)) + b"".join(p for _, p in items)
    if isinstance(value, dict):
        out = b""
        for k, v in value.items():
            kind, payload = _nbt_payload(v)
            key = k.encode("utf-8")
            out += struct.pack(">bH", kind, len(key)) + key + payload
        return 10, out + b"\x00"
    raise TypeError(f"NBT writer: unsupported value {value!r}")


def structure(name_, size, cells):
    """Write a gzip-compressed vanilla structure file data/autarkia/structure/colony/<name>.nbt.

    Args:
        size: (x, y, z) footprint in blocks; it must contain every cell and the full shape of every big block.
        cells: (x, y, z, block id, properties dict) entries; a big block is its center cell only.
    """
    palette, blocks = [], []
    for x, y, z, block, props in cells:
        entry = {"id": rl(block)} | ({"properties": dict(props)} if props else {})
        if entry not in palette:
            palette.append(entry)
        blocks.append({"pos": [x, y, z], "state": palette.index(entry)})
    root = {"DataVersion": DATA_VERSION, "size": list(size), "palette": palette, "blocks": blocks, "entities": []}
    kind, payload = _nbt_payload(root)
    p = GEN / f"data/{NS}/structure/colony/{name_}.nbt"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(gzip.compress(struct.pack(">bH", kind, 0) + payload, mtime=0))
    COUNT[0] += 1


def write_tags():
    for kind, tags in TAGS.items():
        for tid, entries in tags.items():
            ns, path = tid.split(":", 1) if ":" in tid else (NS, tid)
            write_json(f"data/{ns}/tags/{kind}/{path}.json", {"replace": False, "values": sorted(entries)})


def write_lang():
    write_json(f"assets/{NS}/lang/en_us.json", dict(sorted(LANG.items())))


def generate_all():
    for mod in CONTENT_MODULES:
        importlib.import_module(mod).generate()
    write_tags()
