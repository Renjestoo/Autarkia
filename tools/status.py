"""Render STATUS.md from tools/status_items.py and the evidence store tools/status_evidence.json.

Usage:
    status.py                      regenerate STATUS.md
    status.py set KEY EVIDENCE     record evidence for one key (checks the line)
    status.py setmany FILE         record evidence from a JSON object {key: evidence}
    status.py open                 list unchecked keys
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sounds_spec
import status_items

ROOT = Path(__file__).resolve().parent.parent
EVIDENCE = Path(__file__).resolve().parent / "status_evidence.json"
OUT = ROOT / "STATUS.md"


def load():
    return json.loads(EVIDENCE.read_text(encoding="utf-8")) if EVIDENCE.exists() else {}


def save(ev):
    EVIDENCE.write_text(json.dumps(ev, indent=1, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")


def all_keys():
    keys = []
    for _, (_, groups) in status_items.M.items():
        for items in groups.values():
            keys.extend(items)
    keys.extend(f"sound:{s}" for s in sounds_spec.ALL)
    return keys


def render(ev):
    lines = ["# Autarkia build status", "",
             "Every line is checked only with evidence (test result, log line or screenshot path).", ""]
    total = done = 0
    for key, (title, groups) in status_items.M.items():
        lines.append(f"## {key} - {title}")
        lines.append("")
        for gname, items in groups.items():
            lines.append(f"### {gname}")
            for it in items:
                e = ev.get(it, "")
                total += 1
                done += bool(e)
                lines.append(f"- [{'x' if e else ' '}] `{it}`" + (f" - {e}" if e else ""))
            lines.append("")
        if key == "M10":
            lines.append("### Sounds (107)")
            for s in sounds_spec.ALL:
                it = f"sound:{s}"
                e = ev.get(it, "")
                total += 1
                done += bool(e)
                lines.append(f"- [{'x' if e else ' '}] `{it}`" + (f" - {e}" if e else ""))
            lines.append("")
    lines.insert(3, f"Progress: {done} / {total} checked.")
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"STATUS.md: {done}/{total} checked")


def main():
    ev = load()
    if len(sys.argv) >= 4 and sys.argv[1] == "set":
        key = sys.argv[2]
        if key not in all_keys():
            raise SystemExit(f"unknown status key {key}")
        ev[key] = sys.argv[3]
        save(ev)
    elif len(sys.argv) == 3 and sys.argv[1] == "setmany":
        data = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
        known = set(all_keys())
        bad = [k for k in data if k not in known]
        if bad:
            raise SystemExit(f"unknown status keys {bad}")
        ev.update(data)
        save(ev)
    elif len(sys.argv) == 2 and sys.argv[1] == "open":
        for k in all_keys():
            if not ev.get(k):
                print(k)
        return
    render(ev)


if __name__ == "__main__":
    main()
