# NutriCoach live demo

## Open it

**Public browser demo:**

https://rawcdn.githack.com/Dineshkumarbudida/nutricoach/d3a1bbcaba6175625ab3c13b14eff3f1a4d30af8/public-demo/index.html

This is a free, browser-only deployment served from the public repository through the open-source raw.githack caching proxy.

## What works

- Deterministic portion scaling for energy, protein, carbohydrate, fat, and fiber.
- Explicit distinction between zero and missing nutrient data.
- Completeness and uncertainty messages.
- Synthetic values only.
- No account, cookies, analytics, external API, database, or AI dependency.

## What is not included

This static demo is not the full Spring Boot/PostgreSQL application. Profiles, food search, diaries, persistent recipes, food availability, and meal recommendations remain future milestones or require the self-hosted backend.

## Full self-hosted application

The complete foundation can be deployed with open-source Linux, Docker Engine, Docker Compose, PostgreSQL, Nginx, and optional Caddy. See `docs/operations/free-open-source-deployment.md`.
