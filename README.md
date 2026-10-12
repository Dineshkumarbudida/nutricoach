# NutriCoach

NutriCoach is a self-hosted, open-source nutrition and meal-planning application. Its central promise is to help a person decide what to eat next using foods they can actually access, while keeping nutrition calculations deterministic, inspectable, and independent of an AI model.

This repository is the first implementation milestone. It contains:

- A pure Java deterministic nutrition calculation core.
- Explicit missing-value and below-reporting-limit semantics.
- Recipe batch, per-100-g, and portion calculations.
- A Spring Boot HTTP API scaffold and OpenAPI 3.1 contract.
- A PostgreSQL/Flyway baseline schema with provenance, rights review, and versioning.
- A small React/TypeScript calculation workbench.
- A Docker Compose reference deployment.
- Backup/restore helpers, CI, and project governance.
- Product, architecture, data-rights, reuse, and testing decisions.

No production nutrition dataset is bundled. Test and UI fixtures are synthetic and must never be presented as food facts.

## Security status

**Do not expose this foundation build to the public internet.** Authentication and authorization are intentionally not implemented yet. Use it only on a private development machine or isolated network until object-level access control and the security release gate are complete.

## Current status

### Implemented and locally verified without external dependencies

- Linear portion scaling from an immutable, versioned food snapshot.
- Recipe aggregation from immutable ingredient snapshots.
- Cooked-batch yield, per-100-g, and portion concentration.
- Preservation of measured, estimated, missing, not-applicable, and below-reporting-limit states.
- Warning, assumption, and source-provenance output.
- Validation, determinism, and immutability tests.
- **48 dependency-free core assertions pass on Java 21.**

### Scaffolded but not yet production-complete

- Spring Boot API and structured validation errors.
- PostgreSQL persistence contract and first Flyway migration.
- React web client.
- Authentication and authorization.
- Food-data import, curation, and search.
- Diary, targets, availability, and recommendations.

See [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md) for exactly what was and was not executed in the build environment.

## Requirements

For the reference full stack:

- Docker Engine with Docker Compose v2, or
- Java 21 and Maven 3.6.3+, Node.js 22.12+, and PostgreSQL 18.

The pure calculation core can be compiled and tested with Java 21 alone.

## Verify the calculation core

```bash
./backend/scripts/run-core-tests.sh
```

Expected result:

```text
PASS: 48 core assertions
```

Run all repository checks available without downloading dependencies:

```bash
make verify
```

## Run with Docker Compose

```bash
cp .env.example .env
# Change POSTGRES_PASSWORD before any use beyond a private local machine.
docker compose up --build
```

Then open:

- Web workbench: `http://localhost:8081`
- API metadata: `http://localhost:8080/api/v1/meta`
- API health: `http://localhost:8080/actuator/health`

The web workbench submits synthetic values to the deterministic API. It is not a food database or nutrition-advice interface.

## Try the API directly

The following example contains synthetic test values, not nutrition facts:

```bash
curl --request POST http://localhost:8080/api/v1/calculations/food-portions \
  --header 'Content-Type: application/json' \
  --data '{
    "food": {
      "foodId": "synthetic-food",
      "version": "1",
      "displayName": "Synthetic fixture",
      "referenceMassGrams": 100,
      "nutrients": {
        "ENERGY": {"status": "MEASURED", "amount": 100, "reportingLimit": null},
        "PROTEIN": {"status": "MEASURED", "amount": 10, "reportingLimit": null},
        "CARBOHYDRATE": {"status": "MEASURED", "amount": 20, "reportingLimit": null},
        "FAT": {"status": "MEASURED", "amount": 2, "reportingLimit": null},
        "FIBER": {"status": "MISSING", "amount": null, "reportingLimit": null}
      },
      "source": {
        "sourceCode": "synthetic-test",
        "sourceVersion": "1",
        "sourceRecordId": "fixture-1",
        "licenseExpression": "TEST-ONLY"
      }
    },
    "portionMassGrams": 250
  }'
```

## Run services separately

Backend:

```bash
cd backend
mvn test
mvn spring-boot:run
```

Frontend:

```bash
cd web
npm install
npm run dev
```

## Back up and restore

```bash
./scripts/backup.sh
ALLOW_DESTRUCTIVE_RESTORE=1 ./scripts/restore.sh backups/example.dump
```

Read [docs/operations/self-hosting.md](docs/operations/self-hosting.md) before relying on a self-hosted instance.

## Repository map

```text
backend/   deterministic domain, API, migrations, and tests
web/       React/TypeScript calculation workbench
openapi/   versioned HTTP contract
docs/      product, architecture, operations, research, data, and test decisions
data/      dataset policy; no third-party dataset is bundled
scripts/   verification, backup, and restore helpers
```

## Non-negotiable rules

1. An LLM is never the authority for nutrition totals.
2. Missing nutrient data is never silently converted to zero.
3. Food and recipe versions used by a calculation remain reproducible.
4. Allergies and explicit exclusions are hard constraints in the future recommendation engine.
5. Dataset rights are separate from the source-code license.
6. Core workflows must work without a paid AI service.
7. Age-dependent targets may run only for populations covered by the selected, versioned method.

## License

Repository source code is provided under Apache License 2.0. Nutrition datasets, evidence publications, model weights, trademarks, labels, and user-contributed content may have separate rights and must be reviewed before distribution.
