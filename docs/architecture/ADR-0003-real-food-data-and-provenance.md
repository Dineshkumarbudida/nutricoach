# ADR-0003: Real food data and provenance

## Decision

NutriCoach will not maintain a hand-authored production nutrient catalog.

Production nutrition observations must originate from an identified source record with:

- source ID
- source record ID
- source release or version
- original food description
- reference mass and original unit
- normalized unit
- nutrient code
- observed amount
- observation status
- license metadata
- ingestion timestamp
- completeness and quality status

The application may normalize units, de-duplicate source records, attach English search aliases, and compose user-owned recipes from source ingredients. It must never silently replace missing values with zero.

## Source hierarchy

### Tier A: public-domain authoritative food composition

USDA FoodData Central is the primary broad composition source. USDA states that FoodData Central data are public domain and CC0-licensed. NutriCoach preserves FDC identifiers and release metadata.

### Tier B: open packaged-food data

Open Food Facts provides barcode-oriented product data under ODbL, with separate image licensing. NutriCoach will retain required attribution and mark quality/completeness explicitly. Published API rate limits and User-Agent requirements must be respected.

### Tier C: Indian-food discovery data

Third-party Indian datasets can improve search and discovery, but a third-party repository's license statement does not by itself prove the uploader can relicense an underlying publication. Such data remain review-required until the provenance chain is documented.

## Production-real

A record is production-real only when source provenance exists.

Forbidden in production:

- random nutrient values
- placeholder values
- invented averages
- silent nutrient imputation
- copied values with no source record
- records with unclear redistribution rights

A user-entered package label or personal recipe is real user data, but it is clearly labeled as user-provided and is not presented as an authoritative composition record.

## Search

The public application should not depend on an external API for every keystroke.

1. Ingest permitted source records.
2. Validate schema, provenance, license, and units.
3. Normalize to the canonical nutrient model.
4. Index the normalized catalog locally.
5. Search locally.
6. Refresh by source release or documented schedule.
7. Preserve the source version with historical calculations.

## Quality states

Foods expose data quality separately from recommendation confidence:

- SOURCE_COMPLETE
- SOURCE_PARTIAL
- LABEL_COMPLETE
- USER_ENTERED
- ESTIMATED_RECIPE
- INSUFFICIENT_FOR_CLAIM

A missing micronutrient remains unknown. The planner can show known subtotals but cannot claim complete intake when observations are missing.

## Production gate

Before the production release:

- all bundled datasets have documented licensing and provenance
- production foods have source record IDs
- nutrient units are normalized and tested
- English display names are stable
- search returns source-backed foods
- missing values remain unknown
- source/version metadata survive calculations
- the planner never uses foods outside today's availability
- no demo/example data path remains
