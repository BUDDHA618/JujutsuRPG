"""Regression guard for the vanilla 1.20.1 JigsawStructure.verifyRange rule."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/"src/main/resources/data"
def valid(distance,terrain):
    return distance+(0 if terrain=="none" else 12)<=128
assert not valid(128,"beard_thin"),"Must detect the original registry-loading failure"
assert valid(116,"beard_thin")
assert valid(128,"none")
assert not valid(117,"beard_thin")
count=0
for path in root.glob("*/worldgen/structure/*.json"):
    data=json.loads(path.read_text())
    if data.get("type")=="minecraft:jigsaw":
        distance=data["max_distance_from_center"]
        terrain=data.get("terrain_adaptation","none")
        assert valid(distance,terrain),f"{path}: {distance} plus terrain padding exceeds128"
        count+=1
print(f"PASS: {count} jigsaw structures; original128+12 failure caught;116+12 boundary valid")
