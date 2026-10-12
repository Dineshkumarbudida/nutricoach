# Existing-project reuse assessment

**Review date:** 2026-10-11

The master specification named three candidate projects. None is selected as the NutriCoach codebase.

## InTake

Repository: `github.com/michaeldvinci/intake`

Strengths:

- A real self-hosted multi-service application.
- Next.js, Go, PostgreSQL, and Docker Compose.
- Food logging, recipes, pantry, planning, authentication, export/import, and an extensive API contract.

Blocking issues:

- The repository file list and community profile do not expose a code license. Public visibility is not permission to copy or create derivatives.
- Its stack differs from the confirmed React/Spring Boot direction.
- Its food-data provenance and Indian-food requirements do not match NutriCoach's central problem.

Decision: study architecture and workflows only; do not copy code unless the owner adds a compatible license and a dependency review is completed.

## Nourish — Indian Diet Planner

Repository: `github.com/paradise-007/nourish-diet-planner`

Strengths:

- Indian-food orientation.
- Rule-based recommendations and optional AI.
- A large food JSON file and familiar Indian dish names.

Blocking issues:

- A small Streamlit codebase with a large application file is not a suitable foundation for the intended modular, multi-user system.
- The repository states that its food data use ICMR-NIN/IFCT and manual curation, but the rights chain for distributing the derived database is not demonstrated.
- IFCT states that prior written permission is required to store or reproduce the publication electronically for creating a product.
- Several medical-condition features exceed NutriCoach's initial general-wellness boundary.

Decision: do not import its data or use it as the base. Revisit individual design ideas only after independent validation.

## NutriSnap

Repository: `github.com/amritpatro/nutrisnap`

Strengths:

- Simple offline-first browser prototype.
- Hostel-oriented interaction ideas.
- MIT-licensed code claim.

Blocking issues:

- Single-file localStorage application with no backend, provenance model, versioning, authorization, or test architecture.
- Its food-data acknowledgment points to ICMR-NIN without documenting product redistribution permission.

Decision: prototype inspiration only; do not import the food data.

## Overall decision

Start a clean modular monolith. This avoids inheriting an incompatible architecture or uncertain nutrition-data rights while allowing useful interaction patterns to be reimplemented independently.
