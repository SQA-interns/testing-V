#!/usr/bin/env python3
"""Combine oracle-main.json, oracle-deadline.json and oracle-ratelimit.json into one result per scenario.
Usage: python summarize.py <folder-with-the-three-json-files>   -> prints the table and writes oracle-summary.json"""
import json, sys
from pathlib import Path
d = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
res = {}
for ph in ("main", "deadline", "ratelimit"):
    f = d / f"oracle-{ph}.json"
    if f.exists():
        res.update(json.loads(f.read_text(encoding="utf-8"))["results"])
def combine(*keys):
    vals = [res.get(k, {}).get("result", "missing") for k in keys]
    for v in ("missing", "harness", "n.a."):
        if v in vals:
            return v
    return "pass" if all(v == "pass" for v in vals) else "fail"
summary = {f"SC{i}": combine(f"SC{i}") for i in range(1, 12)}
summary["SC12"] = combine("SC12a", "SC12b")
passed = sum(v == "pass" for v in summary.values())
for k, v in summary.items():
    print(f"{k:5s} {v}")
print(f"M1 scenarios passed: {passed} of 12")
(d / "oracle-summary.json").write_text(json.dumps({"scenarios": summary, "passed": passed, "details": res}, indent=2), encoding="utf-8")
