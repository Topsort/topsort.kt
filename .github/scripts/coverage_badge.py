"""Turn a JaCoCo XML report into shields.io endpoint-badge JSON: coverage_badge.py REPORT OUT.

A missing or empty report yields a grey "unknown" badge rather than failing the docs deploy.
"""
import json
import sys
import xml.etree.ElementTree as ET

report, out = sys.argv[1], sys.argv[2]
badge = {"schemaVersion": 1, "label": "coverage", "message": "unknown", "color": "lightgrey"}
try:
    counters = ET.parse(report).getroot().findall("counter")
    line = next(c for c in counters if c.get("type") == "LINE")
    covered, missed = int(line.get("covered")), int(line.get("missed"))
    percent = 100 * covered / (covered + missed)
    badge.update(message=f"{percent:.0f}%", color="brightgreen" if percent >= 75 else "orange")
except (OSError, StopIteration, ZeroDivisionError, ET.ParseError) as e:
    print(f"no usable report ({e}); publishing an unknown badge", file=sys.stderr)
with open(out, "w") as f:
    json.dump(badge, f)
print(badge["message"], "->", out)
