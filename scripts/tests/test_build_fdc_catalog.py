import json
import tempfile
import zipfile
from pathlib import Path
import unittest

from scripts.build_fdc_catalog import build

FOUNDATION_RECORD = {
    "fdcId": 999001,
    "description": "Test Apple, raw",
    "foodCategory": "Fruits",
    "foodNutrients": [
        {"nutrientId": 1008, "nutrientName": "Energy", "unitName": "kcal", "value": 52},
        {"nutrientId": 1003, "nutrientName": "Protein", "unitName": "g", "value": 0.3},
        {"nutrientId": 1079, "nutrientName": "Fiber, total dietary", "unitName": "g", "value": 2.4},
    ],
}
LEGACY_RECORD = {
    "fdcId": 999002,
    "description": "Test Lentils, cooked",
    "foodCategory": "Legumes",
    "foodNutrients": [
        {"nutrientId": 1008, "nutrientName": "Energy", "unitName": "kcal", "value": 116},
        {"nutrientId": 1003, "nutrientName": "Protein", "unitName": "g", "value": 9.0},
        {"nutrientId": 1079, "nutrientName": "Fiber, total dietary", "unitName": "g", "value": 7.9},
    ],
}

class CatalogBuilderTests(unittest.TestCase):
    def write_zip(self, path: Path, name: str, record: dict) -> None:
        with zipfile.ZipFile(path, "w") as zf:
            zf.writestr(name, json.dumps([record]))

    def test_real_source_values_are_preserved_and_units_normalized(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            foundation = root / "foundation.zip"
            legacy = root / "legacy.zip"
            self.write_zip(foundation, "food.json", FOUNDATION_RECORD)
            self.write_zip(legacy, "food.json", LEGACY_RECORD)

            class Args:
                foundation_zip = str(foundation)
                sr_legacy_zip = str(legacy)
                foundation_release = "2026-04"
                sr_release = "2018-04"
                minimum_records = 2

            result = build(Args())
            apple = next(x for x in result["foods"] if x["displayName"] == "Test Apple, raw")
            assert apple["nutrientsPer100g"]["ENERGY"]["amount"] == 52
            assert apple["nutrientsPer100g"]["FIBER"]["amount"] == 2.4
            assert result["counts"]["emitted"] == 2

if __name__ == "__main__":
    unittest.main()
