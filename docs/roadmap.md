# Implementation roadmap

## Milestone 0 — Foundation (current)

- Repository governance and reference deployment.
- Pure deterministic nutrition domain.
- Missing-value semantics and recipe yield.
- API, migration, and web-client scaffolds.
- Data-source and reuse decisions.

Exit: standalone calculation tests pass and the repository can be built in a networked Docker environment.

## Milestone 1 — Trusted catalog

- USDA FoodData Central import adapter.
- Data-source registry and immutable food versions.
- English aliases and normalized search.
- Review workflow and import checksums.
- Small rights-cleared ingredient catalog.

Exit: selected records can be imported reproducibly with provenance and searched by stable identity.

## Milestone 2 — Recipes and diary

- User accounts and object-level authorization.
- Recipe creation and immutable versions.
- Meal logging and daily ledger.
- Export and deletion.
- Telugu and transliterated aliases for the pilot catalog.

Exit: a user can calculate, save, log, edit, and reproduce a mixed meal safely.

## Milestone 3 — Targets and age-aware modes

- Configurable target-method registry.
- Adult methods only where verified.
- Separate age-appropriate methods before enabling personalized child/adolescent targets.
- General logging remains available when target methods are unavailable.

Exit: no method is silently applied outside its validated population.

## Milestone 4 — Availability-aware recommendations

- Pantry and daily-menu records.
- Reviewed culturally coherent meal templates.
- Hard eligibility constraints.
- Inspectable scoring, alternatives, and no-feasible-result behavior.

Exit: recommendations are reproducible, explainable, and use only available safe candidates.

## Milestone 5 — Optional language assistant

- Provider-independent interface.
- Schema-validated extraction.
- Local-model option.
- Prompt-injection and hallucination tests.

Exit: core workflows remain fully usable when the model is disabled.
