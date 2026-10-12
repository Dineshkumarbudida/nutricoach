# Contributing

NutriCoach welcomes code, documentation, translation, data-engineering, nutrition-science, accessibility, and security contributions.

## Before contributing food or evidence data

Do not copy nutrition values, recipes, guideline text, tables, images, or model outputs into the repository unless redistribution rights are documented. Open-source code licensing does not grant rights to third-party data.

Every proposed data source must record:

- Owner and publisher.
- Source version and retrieval date.
- License or written permission.
- Redistribution, modification, commercial-use, and attribution terms.
- Known methodological limitations.

## Development workflow

1. Create a focused branch.
2. Add or update tests with behavior changes.
3. Run `make verify`.
4. Explain safety, data, privacy, and migration implications in the pull request.
5. Avoid unrelated formatting or dependency changes.

## Engineering rules

- Keep authoritative calculations outside controllers and AI adapters.
- Use `BigDecimal` for nutrition arithmetic.
- Do not round inside the calculation domain.
- Preserve missing-value semantics.
- Use stable identifiers and versioned source records.
- Never commit secrets or personal data.
