# NutriCoach

NutriCoach is a self-hosted, open-source nutrition and meal-planning application. Its central promise is to help a person decide what to eat next using foods they can actually access, while keeping nutrition calculations deterministic, inspectable, and independent of an AI model.

This repository is the first implementation milestone. It contains:

- A pure Java deterministic nutrition calculation core.
- Explicit missing-value and below-reporting-limit semantics.
- Recipe batch, per-100-g, and portion calculations.
- A Spring Boot HTTP API and OpenAPI 3.1 contract.
- A PostgreSQL/Flyway baseline schema with provenance, rights review, and versioning.
- A small React/TypeScript calculation workbench.
- A free and open-source Docker Compose deployment path.
- Optional Caddy HTTPS with an outer Basic Authentication gate.
- Backup/restore helpers, runtime smoke tests, CI, and project governance.
- Product, architecture, data-rights, reuse, and testing decisions.

No production nutrition dataset is bundled. Test and UI fixtures are synthetic and must never be presented as food facts.

## Security status

Application-level accounts and per-user authorization are not implemented yet.

- **Local mode** is the safest default and binds to host loopback only.
- **LAN mode** is for a trusted private network only and must not be port-forwarded.
- **HTTPS mode** adds Caddy-managed TLS and Basic Authentication as a single-instance outer gate.

The HTTPS gate does not turn this foundation into a safe multi-tenant service. Do not use it for mutually untrusted users or medically sensitive production data until object-level authorization and the security release gate are complete.

## Current status

### Implemented and verified

- Linear portion scaling from an immutable, versioned food snapshot.
- Recipe aggregation from immutable ingredient snapshots.
- Cooked-batch yield, per-100-g, and portion concentration.
- Preservation of measured, estimated, missing, not-applicable, and below-reporting-limit states.
- Warning, assumption, and source-provenance output.
- Validation, determinism, and immutability tests.
- **48 dependency-free core assertions pass on Java 21.**
- Spring Boot, Flyway, PostgreSQL, React, the frontend proxy, and the calculation API run together in automated runtime tests.

### Scaffolded but not yet product-complete

- Authentication and per-user authorization.
- Food-data import, curation, and search.
- Profiles, diary, targets, availability, and recommendations.
- Production nutrition datasets.

See [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md) for the foundation report.

## Requirements

For the reference full stack:

- A Linux host.
- Docker Engine and Docker Compose, or equivalent compatible open-source OCI tooling.
- About 2 GB RAM minimum; 4 GB is more comfortable while building images.
- About 10 GB free disk plus room for database growth and backups.

For development without containers:

- Java 21 and Maven 3.6.3+.
- Node.js 22.12+.
- PostgreSQL 18.

The pure calculation core can be compiled and tested with Java 21 alone.

## Verify the calculation core

```bash
./backend/scripts/run-core-tests.sh
```

Expected result:

```text
PASS: 48 core assertions
```

Run repository checks available without downloading dependencies:

```bash
make verify
```

## Deploy locally with free/open-source software

```bash
./scripts/init-deployment.sh local
./scripts/deploy.sh local
```

Open:

- Web workbench: `http://127.0.0.1:8081`
- API metadata: `http://127.0.0.1:8080/api/v1/meta`
- API health: `http://127.0.0.1:8080/actuator/health`

The initializer creates a random PostgreSQL password under the ignored `secrets/` directory. Plaintext passwords are not placed in `.env` or committed to Git.

## Other deployment modes

Trusted private LAN:

```bash
./scripts/init-deployment.sh lan
./scripts/deploy.sh lan
```

Public HTTPS for a single protected instance:

```bash
NUTRICOACH_SITE_ADDRESS=nutrition.example.org \
  ./scripts/init-deployment.sh https
./scripts/deploy.sh https
```

The HTTPS initializer displays a generated Basic Authentication password once and stores only its one-way hash. Caddy obtains and renews certificates automatically.

Read [Free and open-source deployment](docs/operations/free-open-source-deployment.md) before choosing a mode. The self-hosted privacy baseline is documented in [PRIVACY.md](PRIVACY.md).

## Try the API directly

The following example contains synthetic test values, not nutrition facts:

```bash
curl --request POST http://127.0.0.1:8080/api/v1/calculations/food-portions \
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
BACKUP_RETENTION_DAYS=14 ./scripts/backup-rotate.sh
ALLOW_DESTRUCTIVE_RESTORE=1 ./scripts/restore.sh backups/example.dump
```

Back up `.env` and `secrets/` securely and separately from the database dump. Read [Self-hosting](docs/operations/self-hosting.md) before relying on an installation.

## Repository map

```text
backend/   deterministic domain, API, migrations, and tests
web/       React/TypeScript calculation workbench
openapi/   versioned HTTP contract
docs/      product, architecture, operations, research, data, and test decisions
data/      dataset policy; no third-party dataset is bundled
deploy/    open-source reverse-proxy configuration
scripts/   verification, deployment, health, backup, and restore helpers
```

## Non-negotiable rules

1. An LLM is never the authority for nutrition totals.
2. Missing nutrient data is never silently converted to zero.
3. Food and recipe versions used by a calculation remain reproducible.
4. Allergies and explicit exclusions are hard constraints in the future recommendation engine.
5. Dataset rights are separate from the source-code license.
6. Core workflows must work without a paid AI service.
7. Age-dependent targets may run only for populations covered by the selected, versioned method.
8. The reference deployment must remain usable without a proprietary hosting platform.

## License

Repository source code is provided under Apache License 2.0. Nutrition datasets, evidence publications, model weights, trademarks, labels, and user-contributed content may have separate rights and must be reviewed before distribution.
