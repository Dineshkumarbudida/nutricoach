# ADR-0003: AI is never authoritative for nutrition calculations

**Status:** Accepted  
**Date:** 2026-10-11

## Decision

Only tested application code may produce authoritative totals, targets, hard-constraint decisions, and recommendation scores.

An optional AI adapter may:

- Parse natural-language food descriptions.
- Suggest candidate database identifiers.
- Ask clarification questions.
- Translate or summarize validated results.

It may not:

- Invent foods or nutrient values.
- Perform the final arithmetic.
- Override allergies or explicit exclusions.
- Directly write curated catalog data.
- Present an uncertain match as exact.

Every structured model response must be schema-validated and rechecked by deterministic services.
