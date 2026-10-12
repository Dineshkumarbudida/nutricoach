# English starter food catalog: rights and provenance

NutriCoach's generated starter catalog is built from the official **USDA FoodData Central SR Legacy CSV release (April 2018)**.

## Source

- Publisher: U.S. Department of Agriculture, Agricultural Research Service.
- Dataset: FoodData Central, SR Legacy.
- Release used by the manifest: `2018-04`.
- Official archive: `https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip`.
- Data terms: public domain / CC0 as documented by FoodData Central.

The catalog importer retains each selected record's FDC identifier, exact source description, publication date, nutrient identifiers, units, release, and license metadata.

## Selection policy

`english-foods.json` is a reviewed manifest, not a nutrition dataset. It contains English display names, exact accepted USDA descriptions, UI categories, and planning tags. `scripts/build_fdc_catalog.py` downloads the official archive and resolves only exact normalized descriptions from this manifest.

The importer fails when a food is missing or ambiguous. It never guesses a nearest match and never converts a missing nutrient observation to zero.

## Generated files

- `public-demo/data/food-catalog.json` — verified records used by the browser prototype.
- `data/catalog/fdc-import-report.json` — resolution report and unresolved/ambiguous checks.

Both files are reproducible outputs. Do not hand-edit nutrient values.

## Foods not represented

A generic paneer record is not included because this release does not silently substitute cottage cheese or another cheese for paneer. Users may add paneer from a package label or a versioned personal recipe; omitted nutrients remain missing.

Indian Food Composition Tables and other NIN materials are not copied into this catalog. Their use and redistribution require a separate rights review and, where applicable, permission.
