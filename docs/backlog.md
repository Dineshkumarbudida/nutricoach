# Prioritized implementation backlog

## Now — Milestone 1 trusted catalog

- `DATA-002` Add a dataset-release and import-run model with checksums.
- `DATA-003` Implement the FoodData Central adapter without coupling the domain to USDA field names.
- `DATA-004` Reject imports without an approved rights record.
- `FOOD-003` Persist stable food identities and immutable food versions.
- `FOOD-004` Add normalized English alias search.
- `FOOD-005` Add a curation state: staged, reviewed, published, retired.
- `TEST-IMPORT-001` Add synthetic importer golden files and failure cases.
- `TEST-DB-001` Apply migrations and repository tests against PostgreSQL in CI.

## Next — Recipes, accounts, and diary

- `AUTH-001` Choose a self-hosted authentication design and threat model.
- `AUTH-002` Implement per-user object authorization before public network exposure.
- `RECIPE-002` Persist immutable recipe versions and source snapshots.
- `LOG-002` Record confirmed consumption separately from suggestions.
- `PRIV-002` Implement export, account deletion, and backup-deletion behavior.
- `SEARCH-002` Add Telugu aliases and reviewed transliterations.

## Later — Targets and recommendations

- `TARGET-001` Create a versioned target-method registry with validated populations.
- `SAFE-003` Block personalized target methods outside their validated age/population range.
- `AVAIL-001` Add pantry and current-menu workflows.
- `REC-001` Add reviewed meal templates before combinatorial candidate generation.
- `REC-002` Enforce hard constraints before scoring.
- `REC-003` Implement an explicit no-feasible-result outcome.
- `REC-004` Store score components, rejected reasons, and ruleset versions.

## Optional after deterministic workflows

- `AI-001` Add a provider-independent structured extraction interface.
- `AI-002` Validate all model output against schemas and existing catalog identifiers.
- `AI-003` Add local-model documentation and no-model fallback tests.
