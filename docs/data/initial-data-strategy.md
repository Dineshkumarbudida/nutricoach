# Initial food-data strategy

## Selected bootstrap source

Use USDA FoodData Central as the first import target for foundational ingredients because USDA publishes the data in the public domain under CC0 and provides versioned JSON and CSV downloads.

This is a legal and engineering bootstrap choice, not a claim that the source has adequate Indian dish coverage.

## Indian-food strategy

1. Seek written permission before embedding or deriving a distributable product database from Indian Food Composition Tables or Dietary Guidelines for Indians.
2. Support private user recipes immediately; private entries remain on the user's own instance.
3. Build project-authored Indian recipes only from ingredients whose data rights are documented, with explicit recipe assumptions and measured or declared yield.
4. Accept community data into a staging area, never directly into the trusted catalog.
5. Require review of identity, units, edible portion, preparation state, provenance, rights, and plausibility before publication.

## Source precedence

1. Exact verified product label for that product and label version.
2. Reviewed food-composition record for the exact food and preparation state.
3. User recipe with known ingredient quantities and cooked yield.
4. Reviewed generic recipe with disclosed assumptions.
5. Related-food approximation, clearly labeled and never silently substituted.
6. No estimate when a defensible match is unavailable.

## Import record requirements

- Publisher and owner.
- Dataset and release version.
- Source record identifier.
- Retrieval date.
- License and attribution.
- Reference quantity and unit.
- Preparation state and edible portion.
- Original nutrient units and methods where available.
- Transformation history and checksum.
- Quality review status.
