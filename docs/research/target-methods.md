# Adult daily target methods

**Method version:** `adult-targets/1.0.0`  
**Evidence review date:** 2026-10-12  
**Supported population:** generally healthy, non-pregnant adults aged 19 years and older.

This method estimates planning targets. It does not measure metabolism, diagnose nutrient deficiency, or prescribe treatment. Childhood, pregnancy, lactation, kidney disease, medically managed diabetes, eating disorders, and other clinical contexts require separately reviewed methods.

## Energy requirement

NutriCoach implements the 2023 Dietary Reference Intake adult Estimated Energy Requirement equations published by Health Canada. Inputs are age in years, height in centimetres, weight in kilograms, the published male/female coefficient set, and activity category.

For the male equation set:

| Activity | Equation (kcal/day) |
|---|---|
| Inactive | `753.07 - 10.83 × age + 6.50 × height + 14.10 × weight` |
| Low active | `581.47 - 10.83 × age + 8.30 × height + 14.94 × weight` |
| Active | `1004.82 - 10.83 × age + 6.52 × height + 15.91 × weight` |
| Very active | `-517.88 - 10.83 × age + 15.61 × height + 19.11 × weight` |

For the female equation set:

| Activity | Equation (kcal/day) |
|---|---|
| Inactive | `584.90 - 7.01 × age + 5.72 × height + 11.71 × weight` |
| Low active | `575.77 - 7.01 × age + 6.60 × height + 12.14 × weight` |
| Active | `710.25 - 7.01 × age + 6.54 × height + 12.34 × weight` |
| Very active | `511.83 - 7.01 × age + 9.07 × height + 12.56 × weight` |

Source: `https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/equations-estimate-energy-requirement.html`

The result is shown as **estimated maintenance energy**. The application must not call it measured total energy expenditure.

## Goal planning adjustment

The planning target is:

```text
planning energy = estimated maintenance × (1 + adjustment / 100)
```

Default adjustments are product heuristics, not DRI equations:

| Goal | Default adjustment |
|---|---:|
| General health | 0% |
| Weight maintenance | 0% |
| Body recomposition | 0% |
| Fat loss | -10% |
| Muscle gain | +5% |

The user may inspect and override the adjustment only within `-15%` through `+10%`. The maintenance estimate remains visible separately. This method does not promise a rate of weight change.

## Protein

Protein is calculated from current body weight:

```text
protein target = body weight (kg) × protein factor (g/kg/day)
```

| Context | Factor |
|---|---:|
| Generally healthy, non-training adult | 0.8 g/kg/day |
| Generally healthy exercising adult | 1.4 g/kg/day |
| Resistance training with muscle gain, recomposition, or fat-loss goal | 1.6 g/kg/day |

The 0.8 g/kg value is the adult RDA. The 1.4 g/kg value is the lower end of the ISSN range for generally healthy exercising adults. The 1.6 g/kg value follows the approximate pooled breakpoint reported in the Morton et al. resistance-training meta-analysis. Higher-protein rules are not applied to clinical contexts.

Sources:

- DRI macronutrients: `https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/reference-values-macronutrients.html`
- ISSN protein and exercise position stand: `https://pubmed.ncbi.nlm.nih.gov/28642676/`
- Morton et al. meta-analysis: `https://pubmed.ncbi.nlm.nih.gov/28698222/`

## Fiber and macronutrient ranges

- Fiber guidance: `14 g × planning kcal / 1,000`.
- Carbohydrate range: 45–65% of planning energy, converted with 4 kcal/g.
- Fat range: 20–35% of planning energy, converted with 9 kcal/g.

These are planning references, not a requirement to hit an exact number every day.

## Micronutrients

The current adult target service includes age- and equation-set-appropriate DRI values for calcium, iron, magnesium, phosphorus, potassium, sodium, zinc, selenium, copper, iodine, manganese, vitamins A/C/D/E/K, thiamin, riboflavin, niacin, vitamin B6, folate, vitamin B12, choline, pantothenic acid, and biotin.

Each value records whether it is an RDA or AI and, where available, an upper or reduction threshold. The UI must show source-data completeness. A known subtotal with one or more missing food observations must not be presented as the full intake.

Sources:

- Elements: `https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/reference-values-elements.html`
- Vitamins: `https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/reference-values-vitamins.html`

## Evidence-update policy

`scripts/evidence_watch.py` searches newly indexed PubMed literature every day. Its output is an **unreviewed evidence inbox** only. It cannot modify target methods or recommendation rules.

A rule update requires:

1. Review of population, methods, risk of bias, consistency, and applicability.
2. Comparison with official guidance and the existing body of evidence.
3. A documented change proposal.
4. Automated unit and scenario tests.
5. A method-version increment.
6. Human approval through a pull request.

Historical plans must retain the method version used at the time.
