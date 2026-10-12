# NutriCoach Product Requirements Document

**Status:** Foundation baseline  
**Deployment:** Self-hosted, open source  
**Audience:** General users across ages and living situations  
**Initial food domain:** Indian foods, especially South Indian and Andhra/Telugu foods

## Product promise

NutriCoach helps a person decide what to eat next using what is realistically available. It combines food logging, deterministic nutrition calculations, user-selected goals, dietary constraints, budget, preparation limits, and uncertainty.

It is not merely a calorie tracker and is not a substitute for clinical care.

## Confirmed scope decisions

1. The product is not limited to one persona. Fitness users, hostel or PG residents, home cooks, families, budget-conscious users, and general-wellness users share one configurable core.
2. The product is not restricted to adults. Age-dependent targets and recommendations must use separately validated age-appropriate methods. When such methods are unavailable, the app must still allow non-age-dependent logging and recipe calculation without inventing personalized targets.
3. The reference deployment is self-hosted. Core use must not require a commercial cloud account, paid AI API, or proprietary database.
4. Nutrition arithmetic is authoritative only when produced by tested deterministic code.
5. AI is optional and may interpret or explain; it may not create authoritative nutrient values or override hard safety constraints.

## P0 user journeys

### Understand what I ate

A user can find or create a food, enter a realistic portion, inspect the source and assumptions, correct the match, and add the confirmed result to a diary.

### Calculate my recipe

A user can create a versioned recipe, enter ingredient quantities and preparation states, record a cooked batch weight, and obtain batch, per-100-g, and portion estimates with missing-data warnings.

### Decide what to eat next

A user can provide current intake and available foods, then receive feasible options that respect recorded restrictions, affordability, equipment, time, preferences, and uncertainty. The system may return “no feasible result” rather than violate a hard constraint.

## P0 functional requirements

- `CALC-001` Scale nutrient observations by mass using decimal arithmetic.
- `CALC-002` Preserve true zero, missing, not-applicable, estimated, and below-reporting-limit states.
- `CALC-003` Aggregate recipe ingredients without treating missing data as zero.
- `CALC-004` Calculate recipe values from final cooked batch weight.
- `CALC-005` Keep calculations reproducible from versioned inputs and sources.
- `FOOD-001` Store stable food identities separately from immutable food versions.
- `FOOD-002` Record source, license, reference basis, raw/cooked state, and limitations.
- `PORTION-001` Support grams first; household measures require explicit food-specific conversions.
- `RECIPE-001` Preserve historical recipe versions used by past logs.
- `LOG-001` Separate planned or suggested meals from confirmed consumption.
- `SAFE-001` Never silently apply an adult target method to a child or adolescent.
- `SAFE-002` Never violate recorded allergies or explicit exclusions.
- `DATA-001` Do not distribute a dataset without a completed rights record.
- `PRIV-001` Core use works without telemetry and supports export and deletion.
- `DEPLOY-001` A reference instance starts with Docker Compose.

## Non-goals for the first release

- Disease diagnosis or treatment.
- Medical nutrition therapy.
- Image-derived exact portions.
- Voice logging.
- Wearables.
- Autonomous clinical target adjustment.
- AI-generated nutrition facts.
- A social network, advertising platform, or food marketplace.

## Quality gates

1. **Data rights:** every distributed record has documented rights and attribution.
2. **Calculation correctness:** golden, unit, property, and edge-case tests pass.
3. **Safety:** hard constraints are never relaxed silently.
4. **Privacy and security:** multi-user isolation, export, deletion, logs, and backups are tested.
5. **Recommendation quality:** explanations correspond to actual rules and scores.
6. **Usability:** users can log, correct, and understand estimates without expert assistance.

## Foundation milestone completion criteria

- Pure calculation domain compiles independently of Spring and the database.
- Synthetic smoke tests pass with Java 21.
- Spring API and database contracts are scaffolded around the domain.
- No production nutrition data are bundled.
- Data-rights and reuse decisions are documented.
