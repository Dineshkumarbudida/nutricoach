# NutriCoach availability-aware public prototype

This directory contains a self-contained browser prototype of NutriCoach's central workflow:

1. Set a goal, daily targets, normal meal times, and workout time.
2. Select foods that are actually available today and enter the available quantities.
3. Apply dietary pattern, cooking-access, preparation-time, and appetite constraints.
4. Generate breakfast, lunch, pre-workout, post-workout, snack, and dinner suggestions with times, portions, preparation notes, and estimated nutrition.
5. See when the selected stock cannot fully cover the chosen targets rather than inventing unavailable food.

The prototype has no account, analytics, cookies, external AI call, backend API, or production food dataset. Approximate demonstration food records are embedded in the page, all calculations happen in the browser, and the page is not medical advice.

The complete Java/Spring Boot, PostgreSQL, React, and Docker Compose foundation remains in the repository root. The public prototype demonstrates the intended product workflow but is not yet the production recommendation service.
