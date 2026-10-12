# Work log

## 2026-10-11 — Foundation milestone completed

Completed:

- Confirmed the general, age-inclusive, self-hosted product direction.
- Converted the master idea into a concise PRD, requirement identifiers, quality gates, roadmap, and backlog.
- Evaluated the three named reuse candidates and chose a clean modular-monolith implementation.
- Selected Apache-2.0 for source code while keeping dataset rights separate.
- Selected USDA FoodData Central as the first import target; no production dataset is bundled.
- Defined deterministic portion and recipe calculation semantics.
- Implemented the dependency-free Java calculation domain.
- Added food scaling, recipe aggregation, cooked-yield, uncertainty states, provenance, warnings, and validation.
- Added 47 passing dependency-free assertions.
- Added a Spring Boot API scaffold, validation mapping, and OpenAPI 3.1 contract.
- Added a PostgreSQL/Flyway baseline schema covering provenance, rights review, immutable versions, logs, availability, and recommendation audit records.
- Added a React/TypeScript calculation workbench using synthetic values.
- Added Dockerfiles, Docker Compose, PostgreSQL 18 persistence, health checks, backup/restore scripts, and CI.
- Added governance, security, self-hosting, data-policy, and implementation-status documentation.

Verification performed:

- Pure Java core compiled with Java 21, `-Xlint:all`, and `-Werror`.
- 48 core assertions passed.
- Repository structure verification passed.
- TypeScript/TSX sources passed offline parser/transpiler checks.
- JSON, XML, YAML, and Bash syntax checks passed.

Not verified in the offline environment:

- Full Maven/Spring build.
- Dependency-resolved React/Vite build.
- Flyway migration against a live PostgreSQL server.
- Docker image and Compose startup.
- End-to-end API behavior.

The next task is the trusted-catalog vertical slice: import a small rights-cleared FoodData Central subset through a versioned adapter and persist/search it with integration tests.
