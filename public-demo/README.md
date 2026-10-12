# NutriCoach public demo

This directory contains a self-contained, browser-only demonstration of NutriCoach's deterministic portion-scaling and missing-data semantics.

## Live public URL

[Open the NutriCoach browser demo](https://rawcdn.githack.com/Dineshkumarbudida/nutricoach/d3a1bbcaba6175625ab3c13b14eff3f1a4d30af8/public-demo/index.html)

The production URL is pinned to the commit that introduced the demo, so the served page is immutable. A development URL that follows the current `main` branch is also available:

`https://raw.githack.com/Dineshkumarbudida/nutricoach/main/public-demo/index.html`

The page has no external JavaScript dependencies, account system, analytics, cookies, server API, or production nutrition dataset. All values are synthetic and calculations happen only in the browser.

The complete Java/Spring Boot, PostgreSQL, React, and Docker Compose foundation remains in the repository root. The browser demo is intentionally narrower than the self-hosted application and is not a replacement for the authoritative backend calculation service.

The free raw.githack service has no formal uptime guarantee. The repository retains the complete source so the demo can be hosted on any static web server.
