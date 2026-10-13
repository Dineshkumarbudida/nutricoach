#!/usr/bin/env python3
"""Build a compact, source-preserving NutriCoach food search catalog from USDA FDC JSON archives.

No nutrient values are invented. Each emitted value comes directly from an FDC record.
The build keeps English display names and preserves FDC provenance.
"""
from __future__ import annotations

import argparse
import json
import re
import zipfile
from pathlib import Path
from typing import Any

KEEP = {
    "Energy": ("ENERGY", "KCAL"),
    "Energy (Atwater General Factors)": ("ENERGY", "KCAL"),
    "Protein": ("PROTEIN", "G"),
    "Carbohydrate, by difference": ("CARBOHYDRATE", "G"),
    "Total lipid (fat)": ("FAT", "G"),
    "Fiber, total dietary": ("FIBER", "G"),
    "Sugars, total including NLEA": ("SUGARS", "G"),
    "Sodium, Na": ("SODIUM", "MG"),
    "Potassium, K": ("POTASSIUM", "MG"),
    "Calcium, Ca": ("CALCIUM", "MG"),
    "Iron, Fe": ("IRON", "MG"),
    "Magnesium, Mg": ("MAGNESIUM", "MG"),
    "Phosphorus, P": ("PHOSPHORUS", "MG"),
    "Zinc, Zn": ("ZINC", "MG"),
    "Copper, Cu": ("COPPER", "MG"),
    "Manganese, Mn": ("MANGANESE", "MG"),
    "Selenium, Se": ("SELENIUM", "UG"),
    "Vitamin A, RAE": ("VITAMIN_A_RAE", "UG"),
    "Vitamin C, total ascorbic acid": ("VITAMIN_C", "MG"),
    "Vitamin D (D2 + D3)": ("VITAMIN_D", "UG"),
    "Vitamin E (alpha-tocopherol)": ("VITAMIN_E", "MG"),
    "Vitamin K (phylloquinone)": ("VITAMIN_K", "UG"),
    "Folate, total": ("FOLATE", "UG"),
    "Folic acid": ("FOLIC_ACID", "UG"),
    "Vitamin B-12": ("VITAMIN_B12", "UG"),
    "Thiamin": ("THIAMIN", "MG"),
    "Riboflavin": ("RIBOFLAVIN", "MG"),
    "Niacin": ("NIACIN", "MG"),
    "Vitamin B-6": ("VITAMIN_B6", "MG"),
    "Choline, total": ("CHOLINE", "MG"),
}

NUTRIENT_ID_FALLBACK = {
    1008: ("ENERGY", "KCAL"),
    2047: ("ENERGY", "KCAL"),
    2048: ("ENERGY", "KCAL"),
    1003: ("PROTEIN", "G"),
    1005: ("CARBOHYDRATE", "G"),
    1004: ("FAT", "G"),
    1079: ("FIBER", "G"),
    2000: ("SUGARS", "G"),
    1093: ("SODIUM", "MG"),
    1092: ("POTASSIUM", "MG"),
    1087: ("CALCIUM", "MG"),
    1089: ("IRON", "MG"),
    1090: ("MAGNESIUM", "MG"),
    1091: ("PHOSPHORUS", "MG"),
    1095: ("ZINC", "MG"),
    1098: ("COPPER", "MG"),
    1101: ("MANGANESE", "MG"),
    1103: ("SELENIUM", "UG"),
    1106: ("VITAMIN_A_RAE", "UG"),
    1162: ("VITAMIN_C", "MG"),
    1114: ("VITAMIN_D", "UG"),
    1109: ("VITAMIN_E", "MG"),
    1185: ("VITAMIN_K", "UG"),
    1177: ("FOLATE", "UG"),
    1170: ("THIAMIN", "MG"),
    1173: ("RIBOFLAVIN", "MG"),
    1167: ("NIACIN", "MG"),
    1175: ("VITAMIN_B6", "MG"),
    1180: ("CHOLINE", "MG"),
}

UNIT_SCALE = {
    ("KCAL", "KCAL"): 1,
    ("G", "G"): 1,
    ("MG", "MG"): 1,
    ("UG", "UG"): 1,
    ("KG", "G"): 1000,
    ("MG", "G"): 0.001,
    ("UG", "G"): 0.000001,
    ("G", "MG"): 1000,
    ("UG", "MG"): 0.001,
    ("MG", "UG"): 1000,
    ("G", "UG"): 1_000_000,
}

def norm_text(value: Any) -> str:
    return re.sub(r"\s+", " ", str(value or "")).strip()

def unit_name(n: dict[str, Any]) -> str:
    return norm_text(n.get("unitName") or n.get("unit_name")).upper().replace("Μ", "U").replace("µ", "U")

def map_nutrient(n: dict[str, Any]) -> tuple[str, str] | None:
    name = norm_text(n.get("nutrientName") or n.get("name"))
    if name in KEEP:
        return KEEP[name]
    nid = n.get("nutrientId") or n.get("nutrient_id")
    try:
        return NUTRIENT_ID_FALLBACK.get(int(nid))
    except (TypeError, ValueError):
        return None

def normalize_amount(n: dict[str, Any], target_unit: str) -> float | None:
    raw = n.get("value", n.get("amount"))
    if raw is None:
        return None
    try:
        value = float(raw)
    except (TypeError, ValueError):
        return None
    source_unit = unit_name(n)
    target_unit = target_unit.upper()
    scale = UNIT_SCALE.get((source_unit, target_unit))
    if scale is None:
        return None
    result = value * scale
    if result < 0:
        return None
    return result

def iter_records(zip_path: Path):
    with zipfile.ZipFile(zip_path) as zf:
        names = [n for n in zf.namelist() if n.lower().endswith(".json")]
        if not names:
            raise SystemExit(f"No JSON file found in {zip_path}")
        for name in names:
            with zf.open(name) as fh:
                data = json.load(fh)
            if isinstance(data, list):
                for record in data:
                    if isinstance(record, dict) and ("fdcId" in record or "fdc_id" in record):
                        yield record
            elif isinstance(data, dict):
                if isinstance(data.get("foods"), list):
                    for record in data["foods"]:
                        if isinstance(record, dict):
                            yield record
                elif "fdcId" in data or "fdc_id" in data:
                    yield data

def build(args: argparse.Namespace) -> dict[str, Any]:
    selected: dict[str, dict[str, Any]] = {}
    counts = {"foundation": 0, "sr_legacy": 0, "emitted": 0}

    sources = [
        ("foundation", Path(args.foundation_zip), "Foundation Foods", args.foundation_release),
        ("sr_legacy", Path(args.sr_legacy_zip), "SR Legacy", args.sr_release),
    ]

    for source_key, path, source_name, release in sources:
        for record in iter_records(path):
            fdc_id = record.get("fdcId", record.get("fdc_id"))
            description = norm_text(record.get("description"))
            if not fdc_id or not description:
                continue
            counts[source_key] += 1

            nutrients: dict[str, dict[str, Any]] = {}
            for n in record.get("foodNutrients", record.get("food_nutrients", [])) or []:
                if not isinstance(n, dict):
                    continue
                mapped = map_nutrient(n)
                if not mapped:
                    continue
                code, target_unit = mapped
                amount = normalize_amount(n, target_unit)
                if amount is None or code in nutrients:
                    continue
                nutrients[code] = {
                    "status": "MEASURED",
                    "amount": round(amount, 8),
                    "unit": target_unit,
                }

            # Prefer current Foundation Foods records over legacy records on FDC ID.
            item = {
                "foodId": f"fdc-{fdc_id}",
                "fdcId": int(fdc_id),
                "displayName": description,
                "foodCategory": norm_text(record.get("foodCategory") or record.get("foodCategoryDescription")),
                "dataType": norm_text(record.get("dataType")),
                "release": release,
                "source": {
                    "sourceCode": "USDA-FDC",
                    "sourceName": source_name,
                    "sourceRecordId": str(fdc_id),
                    "sourceRelease": release,
                    "license": "PUBLIC-DOMAIN-US-GOVERNMENT-WORK",
                    "citation": "U.S. Department of Agriculture, Agricultural Research Service. FoodData Central.",
                },
                "referenceMassGrams": 100,
                "nutrientsPer100g": nutrients,
            }

            key = description.casefold()
            if source_key == "foundation" or key not in selected:
                selected[key] = item

    foods = sorted(selected.values(), key=lambda x: x["displayName"].casefold())
    counts["emitted"] = len(foods)

    if counts["emitted"] < args.minimum_records:
        raise SystemExit(
            f"Only {counts['emitted']} unique foods were emitted; expected at least {args.minimum_records}."
        )

    return {
        "catalogId": "nutricoach-usda-fdc",
        "catalogVersion": "2026-04-foundation-plus-2018-sr-legacy",
        "displayLanguage": "en",
        "sourcePolicy": {
            "primary": "USDA FoodData Central Foundation Foods April 2026",
            "gapFill": "USDA FoodData Central SR Legacy April 2018",
            "values": "source-derived only; no synthetic nutrient values",
        },
        "counts": counts,
        "foods": foods,
    }

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--foundation-zip", required=True)
    ap.add_argument("--sr-legacy-zip", required=True)
    ap.add_argument("--foundation-release", default="2026-04")
    ap.add_argument("--sr-release", default="2018-04")
    ap.add_argument("--minimum-records", type=int, default=5000)
    ap.add_argument("--output", required=True)
    args = ap.parse_args()

    catalog = build(args)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(json.dumps({"counts": catalog["counts"], "output": str(output)}, indent=2))

if __name__ == "__main__":
    main()
