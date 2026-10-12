#!/usr/bin/env python3
"""Build the NutriCoach English food catalog from an official USDA FDC archive.

The importer is deliberately strict:
- it downloads only the archive declared by the reviewed manifest;
- it accepts only exact, normalized descriptions listed in that manifest;
- it fails on missing or ambiguous records;
- it preserves FDC identifiers, nutrient identifiers, units, and provenance;
- it never fills a missing nutrient with zero.

No third-party Python package is required.
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import re
import sys
import tempfile
import urllib.request
import zipfile
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Iterable

USER_AGENT = "NutriCoach/0.2 (+https://github.com/Dineshkumarbudida/nutricoach)"


@dataclass(frozen=True)
class NutrientSpec:
    code: str
    preferred_ids: tuple[int, ...]
    accepted_names: tuple[str, ...]
    canonical_unit: str


NUTRIENT_SPECS: tuple[NutrientSpec, ...] = (
    NutrientSpec("ENERGY", (1008,), ("Energy",), "kcal"),
    NutrientSpec("PROTEIN", (1003,), ("Protein",), "g"),
    NutrientSpec("FAT", (1004,), ("Total lipid (fat)",), "g"),
    NutrientSpec("CARBOHYDRATE", (1005,), ("Carbohydrate, by difference",), "g"),
    NutrientSpec("FIBER", (1079,), ("Fiber, total dietary",), "g"),
    NutrientSpec("SUGARS", (1063,), ("Sugars, Total", "Sugars, total including NLEA"), "g"),
    NutrientSpec("SATURATED_FAT", (1258,), ("Fatty acids, total saturated",), "g"),
    NutrientSpec("CALCIUM", (1087,), ("Calcium, Ca",), "mg"),
    NutrientSpec("IRON", (1089,), ("Iron, Fe",), "mg"),
    NutrientSpec("MAGNESIUM", (1090,), ("Magnesium, Mg",), "mg"),
    NutrientSpec("PHOSPHORUS", (1091,), ("Phosphorus, P",), "mg"),
    NutrientSpec("POTASSIUM", (1092,), ("Potassium, K",), "mg"),
    NutrientSpec("SODIUM", (1093,), ("Sodium, Na",), "mg"),
    NutrientSpec("ZINC", (1095,), ("Zinc, Zn",), "mg"),
    NutrientSpec("COPPER", (1098,), ("Copper, Cu",), "mg"),
    NutrientSpec("MANGANESE", (1101,), ("Manganese, Mn",), "mg"),
    NutrientSpec("SELENIUM", (1103,), ("Selenium, Se",), "mcg"),
    NutrientSpec("VITAMIN_A_RAE", (1106,), ("Vitamin A, RAE",), "mcg RAE"),
    NutrientSpec("VITAMIN_E", (1109,), ("Vitamin E (alpha-tocopherol)",), "mg"),
    NutrientSpec("VITAMIN_D", (1114,), ("Vitamin D (D2 + D3)",), "mcg"),
    NutrientSpec("VITAMIN_C", (1162,), ("Vitamin C, total ascorbic acid",), "mg"),
    NutrientSpec("THIAMIN", (1165,), ("Thiamin",), "mg"),
    NutrientSpec("RIBOFLAVIN", (1166,), ("Riboflavin",), "mg"),
    NutrientSpec("NIACIN", (1167,), ("Niacin",), "mg"),
    NutrientSpec("VITAMIN_B6", (1175,), ("Vitamin B-6",), "mg"),
    NutrientSpec("FOLATE_DFE", (1190,), ("Folate, DFE",), "mcg DFE"),
    NutrientSpec("VITAMIN_B12", (1178,), ("Vitamin B-12",), "mcg"),
    NutrientSpec("CHOLINE", (1180,), ("Choline, total",), "mg"),
    NutrientSpec("VITAMIN_K", (1185,), ("Vitamin K (phylloquinone)",), "mcg"),
    NutrientSpec("PANTOTHENIC_ACID", (1170,), ("Pantothenic acid",), "mg"),
)

UNIT_ALIASES = {
    "KCAL": "kcal",
    "G": "g",
    "MG": "mg",
    "UG": "mcg",
    "MCG": "mcg",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", default="data/catalog/english-foods.json")
    parser.add_argument("--archive", help="Use a local FDC ZIP instead of downloading the manifest URL")
    parser.add_argument("--output", default="public-demo/data/food-catalog.json")
    parser.add_argument("--report", default="data/catalog/fdc-import-report.json")
    parser.add_argument(
        "--allow-unresolved",
        action="store_true",
        help="Write a partial catalog/report and return success even when records are unresolved",
    )
    parser.add_argument("--fixture", action="store_true", help="Marks a synthetic test archive in report metadata")
    return parser.parse_args()


def normalize_description(value: str) -> str:
    value = value.casefold().replace("’", "'")
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def decimal_string(value: str | Decimal) -> str:
    try:
        number = value if isinstance(value, Decimal) else Decimal(value)
    except (InvalidOperation, TypeError) as exc:
        raise ValueError(f"Invalid numeric value: {value!r}") from exc
    if not number.is_finite():
        raise ValueError(f"Non-finite numeric value: {value!r}")
    normalized = number.normalize()
    result = format(normalized, "f")
    if "." in result:
        result = result.rstrip("0").rstrip(".")
    return result or "0"


def download_archive(url: str) -> Path:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    temporary = tempfile.NamedTemporaryFile(prefix="fdc-", suffix=".zip", delete=False)
    temporary.close()
    destination = Path(temporary.name)
    try:
        with urllib.request.urlopen(request, timeout=180) as response, destination.open("wb") as output:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                output.write(chunk)
    except Exception:
        destination.unlink(missing_ok=True)
        raise
    return destination


def locate_member(archive: zipfile.ZipFile, basename: str) -> str:
    matches = [name for name in archive.namelist() if Path(name).name.casefold() == basename.casefold()]
    if len(matches) != 1:
        raise ValueError(f"Expected exactly one {basename!r} in archive, found {matches}")
    return matches[0]


def rows_from_zip(archive: zipfile.ZipFile, basename: str) -> Iterable[dict[str, str]]:
    member = locate_member(archive, basename)
    with archive.open(member, "r") as raw:
        text = io.TextIOWrapper(raw, encoding="utf-8-sig", newline="")
        yield from csv.DictReader(text)


def read_manifest(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("displayLanguage") != "en":
        raise ValueError("The release manifest must use displayLanguage='en'")
    if not data.get("foods"):
        raise ValueError("The manifest must contain at least one food")
    seen: set[str] = set()
    for food in data["foods"]:
        food_id = food.get("id")
        if not food_id or food_id in seen:
            raise ValueError(f"Food IDs must be non-empty and unique: {food_id!r}")
        seen.add(food_id)
        if not food.get("displayName") or not food.get("acceptedDescriptions"):
            raise ValueError(f"Food {food_id} lacks an English display name or accepted description")
    return data


def build_nutrient_lookup(nutrient_rows: Iterable[dict[str, str]]) -> tuple[dict[int, dict[str, str]], dict[str, int]]:
    by_id: dict[int, dict[str, str]] = {}
    by_name: dict[str, int] = {}
    for row in nutrient_rows:
        nutrient_id = int(row["id"])
        by_id[nutrient_id] = row
        by_name[normalize_description(row["name"])] = nutrient_id
    return by_id, by_name


def resolve_spec_ids(by_id: dict[int, dict[str, str]], by_name: dict[str, int]) -> dict[str, int]:
    result: dict[str, int] = {}
    for spec in NUTRIENT_SPECS:
        chosen: int | None = None
        for candidate in spec.preferred_ids:
            if candidate in by_id:
                chosen = candidate
                break
        if chosen is None:
            for name in spec.accepted_names:
                candidate = by_name.get(normalize_description(name))
                if candidate is not None:
                    chosen = candidate
                    break
        if chosen is not None:
            result[spec.code] = chosen
    return result


def canonical_unit(source_unit: str, expected: str) -> str:
    normalized = UNIT_ALIASES.get(source_unit.strip().upper(), source_unit.strip())
    if expected in {"mcg", "mcg DFE", "mcg RAE"} and normalized == "mcg":
        return expected
    if normalized != expected:
        raise ValueError(f"Unexpected nutrient unit {source_unit!r}; expected {expected!r}")
    return expected


def output_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    manifest_path = Path(args.manifest)
    output_path = Path(args.output)
    report_path = Path(args.report)
    manifest = read_manifest(manifest_path)
    source = manifest["source"]

    downloaded: Path | None = None
    archive_path = Path(args.archive) if args.archive else None
    try:
        if archive_path is None:
            downloaded = download_archive(source["archiveUrl"])
            archive_path = downloaded

        report: dict[str, Any] = {
            "importerVersion": "fdc-catalog-importer/1.0.0",
            "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
            "manifest": str(manifest_path),
            "fixture": bool(args.fixture),
            "source": source,
            "resolved": [],
            "unresolved": [],
            "ambiguous": [],
            "warnings": [],
        }

        with zipfile.ZipFile(archive_path) as archive:
            foods_by_description: dict[str, list[dict[str, str]]] = defaultdict(list)
            for row in rows_from_zip(archive, "food.csv"):
                foods_by_description[normalize_description(row["description"])].append(row)

            nutrient_by_id, nutrient_by_name = build_nutrient_lookup(rows_from_zip(archive, "nutrient.csv"))
            nutrient_id_by_code = resolve_spec_ids(nutrient_by_id, nutrient_by_name)

            candidate_fdc_ids: set[int] = set()
            candidate_rows_by_manifest_id: dict[str, list[dict[str, str]]] = {}

            for food in manifest["foods"]:
                matches: dict[int, dict[str, str]] = {}
                for description in food["acceptedDescriptions"]:
                    for row in foods_by_description.get(normalize_description(description), []):
                        matches[int(row["fdc_id"])] = row
                candidates = list(matches.values())
                candidate_rows_by_manifest_id[food["id"]] = candidates
                candidate_fdc_ids.update(int(row["fdc_id"]) for row in candidates)

            nutrient_values: dict[int, dict[int, dict[str, str]]] = defaultdict(dict)
            for row in rows_from_zip(archive, "food_nutrient.csv"):
                fdc_id = int(row["fdc_id"])
                if fdc_id not in candidate_fdc_ids:
                    continue
                nutrient_id = int(row["nutrient_id"])
                if nutrient_id not in nutrient_by_id:
                    continue
                amount = row.get("amount", "").strip()
                if amount == "":
                    continue
                nutrient_values[fdc_id][nutrient_id] = row

            units: dict[int, str] = {}
            try:
                for row in rows_from_zip(archive, "measure_unit.csv"):
                    units[int(row["id"])] = row["name"]
            except ValueError:
                report["warnings"].append("measure_unit.csv was not present; portion unit names were omitted")

            portions: dict[int, list[dict[str, Any]]] = defaultdict(list)
            try:
                for row in rows_from_zip(archive, "food_portion.csv"):
                    fdc_id = int(row["fdc_id"])
                    if fdc_id not in candidate_fdc_ids:
                        continue
                    gram_weight = row.get("gram_weight", "").strip()
                    if not gram_weight:
                        continue
                    measure_unit_id = int(row["measure_unit_id"]) if row.get("measure_unit_id") else None
                    portions[fdc_id].append({
                        "amount": decimal_string(row.get("amount", "1") or "1"),
                        "measureUnit": units.get(measure_unit_id, "") if measure_unit_id else "",
                        "description": (row.get("portion_description") or row.get("modifier") or "").strip(),
                        "gramWeight": decimal_string(gram_weight),
                    })
            except ValueError:
                report["warnings"].append("food_portion.csv was not present; source portions were omitted")

            output_foods: list[dict[str, Any]] = []
            spec_by_code = {spec.code: spec for spec in NUTRIENT_SPECS}

            for food in manifest["foods"]:
                candidates = candidate_rows_by_manifest_id[food["id"]]
                if not candidates:
                    report["unresolved"].append({
                        "id": food["id"],
                        "displayName": food["displayName"],
                        "acceptedDescriptions": food["acceptedDescriptions"],
                    })
                    continue
                if len(candidates) != 1:
                    report["ambiguous"].append({
                        "id": food["id"],
                        "displayName": food["displayName"],
                        "matches": [
                            {"fdcId": int(row["fdc_id"]), "description": row["description"]}
                            for row in candidates
                        ],
                    })
                    continue

                source_food = candidates[0]
                fdc_id = int(source_food["fdc_id"])
                observations: dict[str, dict[str, Any]] = {}
                missing: list[str] = []

                for spec in NUTRIENT_SPECS:
                    nutrient_id = nutrient_id_by_code.get(spec.code)
                    source_observation = nutrient_values.get(fdc_id, {}).get(nutrient_id) if nutrient_id else None
                    if source_observation is None:
                        missing.append(spec.code)
                        continue
                    nutrient_definition = nutrient_by_id[nutrient_id]
                    unit = canonical_unit(nutrient_definition["unit_name"], spec.canonical_unit)
                    observations[spec.code] = {
                        "amountPer100g": decimal_string(source_observation["amount"]),
                        "unit": unit,
                        "sourceNutrientId": nutrient_id,
                        "sourceNutrientName": nutrient_definition["name"],
                    }

                report["resolved"].append({
                    "id": food["id"],
                    "displayName": food["displayName"],
                    "fdcId": fdc_id,
                    "sourceDescription": source_food["description"],
                    "nutrientCount": len(observations),
                    "missingNutrients": missing,
                })

                output_foods.append({
                    "id": food["id"],
                    "displayName": food["displayName"],
                    "displayLanguage": "en",
                    "category": food["category"],
                    "roles": food.get("roles", []),
                    "mealTags": food.get("mealTags", []),
                    "defaultPortionGrams": decimal_string(str(food["defaultPortionGrams"])),
                    "defaultAvailableGrams": decimal_string(str(food["defaultAvailableGrams"])),
                    "referenceBasisGrams": "100",
                    "nutrients": observations,
                    "missingNutrients": missing,
                    "sourcePortions": sorted(
                        portions.get(fdc_id, []),
                        key=lambda item: (item["description"], Decimal(item["gramWeight"])),
                    )[:12],
                    "source": {
                        "name": source["name"],
                        "fdcId": fdc_id,
                        "description": source_food["description"],
                        "dataType": source_food.get("data_type", "SR Legacy"),
                        "publicationDate": source_food.get("publication_date", ""),
                        "release": source["release"],
                        "license": source["license"],
                        "archiveUrl": source["archiveUrl"],
                    },
                })

            catalog = {
                "catalogId": manifest["catalogId"],
                "catalogVersion": manifest["catalogVersion"],
                "generatedAt": report["generatedAt"],
                "displayLanguage": "en",
                "source": source,
                "nutrientDefinitions": {
                    code: {
                        "canonicalUnit": spec_by_code[code].canonical_unit,
                        "sourceNutrientId": nutrient_id,
                        "sourceNutrientName": nutrient_by_id[nutrient_id]["name"],
                    }
                    for code, nutrient_id in sorted(nutrient_id_by_code.items())
                },
                "foods": sorted(output_foods, key=lambda item: item["displayName"].casefold()),
                "customFoodNotice": (
                    "Foods not present in this verified starter catalog, including paneer, must be added "
                    "from a package label or a versioned personal recipe. Missing nutrients remain missing."
                ),
            }

            output_json(output_path, catalog)
            output_json(report_path, report)

        unresolved_count = len(report["unresolved"]) + len(report["ambiguous"])
        print(
            f"Resolved {len(report['resolved'])} food records; "
            f"unresolved/ambiguous: {unresolved_count}; output: {output_path}"
        )
        if unresolved_count and not args.allow_unresolved:
            print(f"See {report_path} for exact unresolved records.", file=sys.stderr)
            return 2
        return 0
    finally:
        if downloaded is not None:
            downloaded.unlink(missing_ok=True)


if __name__ == "__main__":
    raise SystemExit(main())
