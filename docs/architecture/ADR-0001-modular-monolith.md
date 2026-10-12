# ADR-0001: Begin as a modular monolith

**Status:** Accepted  
**Date:** 2026-10-11

## Context

NutriCoach needs food data, recipes, calculations, diaries, targets, availability, recommendations, evidence, and optional AI. These areas need clear boundaries, but the project does not yet have scale evidence that justifies distributed services.

## Decision

Build one deployable backend with domain modules and explicit dependency rules:

- `nutrition`: food arithmetic and missing-value semantics.
- `catalog`: food identity, aliases, versions, sources, and search.
- `recipes`: versioned ingredients and yields.
- `diary`: confirmed consumption and daily ledger.
- `profiles`: optional user context and restrictions.
- `availability`: pantry and daily menus.
- `recommendations`: eligibility, candidate generation, scoring, and explanation.
- `evidence`: source and claims registry.
- `ai-adapter`: optional untrusted language interface.

The deterministic nutrition domain must not depend on Spring, persistence, HTTP, or an AI provider.

## Consequences

- Local development and self-hosting remain simple.
- Transactions and schema evolution are easier.
- Modules can be extracted later only when operational evidence requires it.
- Dependency discipline must be enforced through package structure and tests rather than network boundaries.
