# Production release gate

The next release is the first production release with real data.

## Hard gates

- No synthetic nutrition observations in production data.
- Every bundled food has a source record ID, source version, and rights status.
- Every nutrient value has an observation status.
- Missing values remain unknown.
- Food state and serving basis are explicit.
- English display names are used by the UI.
- User-entered labels and recipes are clearly distinguished from sourced records.
- Energy, protein, fiber, and micronutrient targets use versioned methods.
- Research changes are reviewed and versioned before activation.
- The deterministic calculation core passes contract tests.
- Search can return source-backed records.
- Planner never consumes unavailable inventory.
- Planned meals and consumed meals are separate.
- Safety gates block unsupported high-risk clinical use.
- Production build contains no demo/example food path.

## Release evidence

Attach to the release:

- source manifest
- source/license review
- catalog validation report
- calculation test report
- planner scenario report
- security/privacy review
- deployment smoke-test report
- change log for evidence/ruleset versions

A green build is necessary but is not alone sufficient to claim medical validity.
