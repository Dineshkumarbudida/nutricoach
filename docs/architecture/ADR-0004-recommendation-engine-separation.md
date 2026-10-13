# ADR-0004: Evidence rules are separate from food composition

## Decision

NutriCoach uses two independently versioned knowledge layers.

### Evidence and rules

This layer contains reviewed rules for:

- energy estimation
- protein targets
- fiber guidance
- micronutrient reference values
- meal-timing heuristics
- safety constraints

Each rule records its population, supporting sources, review date, version, and limitations.

### Food composition

This layer contains food and recipe observations with:

- source
- source record ID
- preparation state
- serving basis
- nutrient values
- completeness
- provenance

## Runtime flow

1. Load the user's saved profile.
2. Calculate targets using a deterministic method and version.
3. Load confirmed intake and today's availability.
4. Retrieve source-backed food observations.
5. Calculate nutrient contributions.
6. Identify material nutrient gaps.
7. Build feasible meal candidates.
8. Rank candidates using deterministic product rules.
9. Explain the result using rule references and food sources.
10. Store the rule version and source versions with the plan.

## AI boundary

AI may explain, summarize, translate, and help navigate the catalog.

AI must not be the authoritative calculator of nutrient amounts, target equations, or safety constraints.

## Research update model

New literature is monitored continuously but is not deployed directly into production rules.

Flow:

literature watch -> evidence review -> change proposal -> automated tests -> approval -> versioned release

This prevents a newly published single study from silently changing every user's diet.
