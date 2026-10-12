from __future__ import annotations

import csv
import json
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
IMPORTER = ROOT / "scripts" / "build_fdc_catalog.py"


class FoodDataCatalogImporterTest(unittest.TestCase):
    def write_csv(self, archive: zipfile.ZipFile, name: str, fieldnames: list[str], rows: list[dict[str, str]]) -> None:
        temporary = tempfile.NamedTemporaryFile(mode="w", newline="", encoding="utf-8", delete=False)
        try:
            writer = csv.DictWriter(temporary, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(rows)
            temporary.close()
            archive.write(temporary.name, arcname=f"fixture/{name}")
        finally:
            Path(temporary.name).unlink(missing_ok=True)

    def make_fixture(self, directory: Path) -> Path:
        archive_path = directory / "fixture.zip"
        with zipfile.ZipFile(archive_path, "w") as archive:
            self.write_csv(
                archive,
                "food.csv",
                ["fdc_id", "data_type", "description", "food_category_id", "publication_date"],
                [
                    {
                        "fdc_id": "1001",
                        "data_type": "sr_legacy_food",
                        "description": "Egg, whole, cooked, hard-boiled",
                        "food_category_id": "1",
                        "publication_date": "2019-04-01",
                    },
                    {
                        "fdc_id": "1002",
                        "data_type": "sr_legacy_food",
                        "description": "Bananas, raw",
                        "food_category_id": "2",
                        "publication_date": "2019-04-01",
                    },
                ],
            )
            self.write_csv(
                archive,
                "nutrient.csv",
                ["id", "name", "unit_name", "nutrient_nbr", "rank"],
                [
                    {"id": "1008", "name": "Energy", "unit_name": "KCAL", "nutrient_nbr": "208", "rank": "300"},
                    {"id": "1003", "name": "Protein", "unit_name": "G", "nutrient_nbr": "203", "rank": "600"},
                    {"id": "1079", "name": "Fiber, total dietary", "unit_name": "G", "nutrient_nbr": "291", "rank": "1200"},
                    {"id": "1092", "name": "Potassium, K", "unit_name": "MG", "nutrient_nbr": "306", "rank": "5700"},
                ],
            )
            self.write_csv(
                archive,
                "food_nutrient.csv",
                ["id", "fdc_id", "nutrient_id", "amount"],
                [
                    {"id": "1", "fdc_id": "1001", "nutrient_id": "1008", "amount": "155"},
                    {"id": "2", "fdc_id": "1001", "nutrient_id": "1003", "amount": "12.58"},
                    {"id": "3", "fdc_id": "1001", "nutrient_id": "1079", "amount": "0"},
                    {"id": "4", "fdc_id": "1002", "nutrient_id": "1008", "amount": "89"},
                    {"id": "5", "fdc_id": "1002", "nutrient_id": "1003", "amount": "1.09"},
                    {"id": "6", "fdc_id": "1002", "nutrient_id": "1079", "amount": "2.6"},
                    {"id": "7", "fdc_id": "1002", "nutrient_id": "1092", "amount": "358"},
                ],
            )
            self.write_csv(
                archive,
                "measure_unit.csv",
                ["id", "name"],
                [{"id": "9999", "name": "undetermined"}],
            )
            self.write_csv(
                archive,
                "food_portion.csv",
                ["id", "fdc_id", "seq_num", "amount", "measure_unit_id", "portion_description", "modifier", "gram_weight"],
                [
                    {
                        "id": "1",
                        "fdc_id": "1001",
                        "seq_num": "1",
                        "amount": "1",
                        "measure_unit_id": "9999",
                        "portion_description": "1 large egg",
                        "modifier": "large",
                        "gram_weight": "50",
                    }
                ],
            )
        return archive_path

    def make_manifest(self, directory: Path, include_missing: bool = False) -> Path:
        foods = [
            {
                "id": "egg-hard-boiled",
                "displayName": "Egg, whole, hard-boiled",
                "acceptedDescriptions": ["Egg, whole, cooked, hard-boiled"],
                "category": "Eggs",
                "roles": ["protein"],
                "mealTags": ["breakfast"],
                "defaultPortionGrams": 50,
                "defaultAvailableGrams": 200,
            },
            {
                "id": "banana-raw",
                "displayName": "Banana, raw",
                "acceptedDescriptions": ["Bananas, raw"],
                "category": "Fruit",
                "roles": ["fiber"],
                "mealTags": ["snack"],
                "defaultPortionGrams": 118,
                "defaultAvailableGrams": 236,
            },
        ]
        if include_missing:
            foods.append(
                {
                    "id": "missing",
                    "displayName": "Missing food",
                    "acceptedDescriptions": ["Not in fixture"],
                    "category": "Other",
                    "roles": [],
                    "mealTags": [],
                    "defaultPortionGrams": 100,
                    "defaultAvailableGrams": 100,
                }
            )
        path = directory / "manifest.json"
        path.write_text(
            json.dumps(
                {
                    "catalogId": "fixture",
                    "catalogVersion": "1",
                    "displayLanguage": "en",
                    "source": {
                        "name": "USDA FoodData Central SR Legacy fixture",
                        "release": "test",
                        "archiveUrl": "https://example.invalid/fixture.zip",
                        "license": "CC0-1.0",
                        "citation": "Test fixture",
                    },
                    "foods": foods,
                }
            ),
            encoding="utf-8",
        )
        return path

    def run_import(self, directory: Path, include_missing: bool = False) -> subprocess.CompletedProcess[str]:
        archive = self.make_fixture(directory)
        manifest = self.make_manifest(directory, include_missing=include_missing)
        return subprocess.run(
            [
                "python3",
                str(IMPORTER),
                "--manifest",
                str(manifest),
                "--archive",
                str(archive),
                "--output",
                str(directory / "catalog.json"),
                "--report",
                str(directory / "report.json"),
                "--fixture",
            ],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

    def test_import_preserves_true_zero_and_missing(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            result = self.run_import(directory)
            self.assertEqual(0, result.returncode, result.stderr)
            catalog = json.loads((directory / "catalog.json").read_text())
            self.assertEqual("en", catalog["displayLanguage"])
            self.assertEqual(2, len(catalog["foods"]))

            egg = next(food for food in catalog["foods"] if food["id"] == "egg-hard-boiled")
            self.assertEqual("155", egg["nutrients"]["ENERGY"]["amountPer100g"])
            self.assertEqual("0", egg["nutrients"]["FIBER"]["amountPer100g"])
            self.assertIn("POTASSIUM", egg["missingNutrients"])
            self.assertEqual(1001, egg["source"]["fdcId"])
            self.assertEqual("50", egg["sourcePortions"][0]["gramWeight"])

    def test_unresolved_food_fails_but_writes_report(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            result = self.run_import(directory, include_missing=True)
            self.assertEqual(2, result.returncode)
            report = json.loads((directory / "report.json").read_text())
            self.assertEqual("missing", report["unresolved"][0]["id"])
            self.assertTrue((directory / "catalog.json").exists())


if __name__ == "__main__":
    unittest.main()
