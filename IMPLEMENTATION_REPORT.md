# NutriCoach foundation implementation report

**Milestone:** 0 — trustworthy calculation foundation  
**Completed:** 2026-10-11  
**Status:** Calculation core verified; network-dependent full-stack build not yet executed

## What was implemented

### Product and architecture

- Concise product requirements with the confirmed general, age-inclusive, self-hosted direction.
- A modular-monolith architecture and a reference Docker Compose deployment.
- Explicit boundaries between deterministic nutrition logic, persistence, HTTP, and future optional AI.
- Apache-2.0 source-code licensing with a separate rights process for datasets and other assets.
- A documented assessment of the three reuse candidates named in the master prompt.
- A phased roadmap and quality gates.

### Deterministic calculation core

The pure Java domain has no Spring, database, HTTP, or model-provider dependency. It implements:

- Decimal mass scaling with `BigDecimal` and `MathContext.DECIMAL128`.
- Energy, protein, carbohydrate, fat, and fiber as the initial nutrient contract.
- Measured and estimated values.
- True numeric zero.
- Missing and not-applicable values without silently converting them to zero.
- Values below a declared reporting limit as bounded uncertainty.
- Batch recipe aggregation.
- Final cooked-batch yield.
- Per-100-g and selected-portion results.
- Warnings, assumptions, and source provenance.
- Validation for empty recipes, non-positive quantities, and portions larger than the batch.
- Immutable inputs and outputs.

### API

The Spring Boot scaffold exposes:

- `GET /api/v1/meta`
- `POST /api/v1/calculations/food-portions`
- `POST /api/v1/calculations/recipes`
- `GET /actuator/health`

The API validates request shapes, returns structured errors, and delegates arithmetic to the pure domain service. A versioned OpenAPI 3.1 contract is included.

### Database

The first Flyway migration defines:

- User profiles and dietary constraints.
- Data sources and data-rights reviews.
- Stable foods and immutable food versions.
- Aliases, nutrient definitions, observations, and serving conversions.
- Versioned recipes and ingredients.
- Reproducible calculation records.
- Food logs with planned/suggested/consumed separation.
- Availability records.
- Recommendation and feedback audit records.

The migration seeds only nutrient definitions. It does not embed nutrition values.

### Web workbench

The React/TypeScript foundation UI lets a developer submit a synthetic food snapshot and inspect:

- Scaled nutrient subtotals.
- Complete, bounded-incomplete, and unbounded-incomplete states.
- Warnings.
- Assumptions.
- Source provenance.

A blank nutrient field is intentionally submitted as `MISSING`, not as zero.

### Operations and governance

- Dockerfiles for API and web.
- PostgreSQL 18 Compose service with the PostgreSQL 18 volume layout.
- Health checks and environment-based configuration.
- Backup and destructive-restore scripts.
- CI jobs for the dependency-free core, backend, and web.
- Contribution, security, conduct, and data-policy documents.

## Verification actually performed

The following commands ran successfully in the available environment:

```text
./backend/scripts/run-core-tests.sh
PASS: 48 core assertions

./scripts/verify.sh
Repository structure verification passed.
```

Additional offline checks passed:

- Five TypeScript/TSX source files parsed and transpiled with the installed TypeScript compiler.
- `package.json` parsed as JSON.
- `pom.xml` parsed as XML.
- Compose, Spring configuration, CI, and OpenAPI files parsed as YAML.
- Shell scripts passed Bash syntax validation.

## What was not claimed as verified

The execution environment did not contain Maven, Docker, or PostgreSQL. It also could not resolve npm dependencies from the network. Therefore:

- The full Spring Boot Maven build was not executed here.
- The React application was not built against downloaded React/Vite type packages.
- The Flyway migration was not applied to a live PostgreSQL instance.
- The Docker images and Compose stack were not built or started.
- End-to-end HTTP tests were not run.
- A package lock could not be generated in this offline environment.

CI is configured to perform the network-dependent backend and web builds when the repository is run in a connected environment.

## Immediate next milestone

Milestone 1 is a trusted-catalog vertical slice:

1. Implement a versioned USDA FoodData Central importer behind a dataset adapter.
2. Record source release, rights, checksums, transformations, units, and preparation state.
3. Persist immutable food versions and nutrient observations.
4. Add normalized food and alias search.
5. Import a deliberately small, reviewed ingredient subset.
6. Add integration tests with PostgreSQL and a reproducible synthetic import fixture.

No Indian composition dataset should be embedded until its product-storage and redistribution rights have been recorded.
