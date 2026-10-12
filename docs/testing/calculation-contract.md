# Deterministic nutrition calculation contract

## Canonical rules

1. Mass is represented internally in grams using `BigDecimal`.
2. The core nutrient set is energy, protein, carbohydrate, fat, and fiber.
3. Each nutrient code has one canonical unit.
4. Arithmetic uses `MathContext.DECIMAL128`.
5. The domain does not round for display.
6. A true zero is a measured or estimated observation whose amount is zero.
7. Missing, not-applicable, and below-reporting-limit observations are not converted to zero.

## Food scaling

For a food value `N_ref` on reference mass `W_ref` and portion mass `W_portion`:

```text
factor = W_portion / W_ref
N_portion = N_ref * factor
```

The same factor applies to a reporting-limit upper bound.

## Recipe calculation

For each nutrient:

```text
N_batch_known = sum(known ingredient contributions)
```

The batch is then concentrated or diluted using the declared final cooked batch mass:

```text
N_per_100_g = N_batch_known * 100 / W_cooked_batch
N_portion   = N_batch_known * W_portion / W_cooked_batch
```

Water changes concentration through the final cooked mass. Nutrient retention, discarded cooking water, oil left in the vessel, bones, shells, and inedible fractions are not guessed. They must be represented by explicit ingredient quantities, edible portions, retention factors, or warnings.

## Completeness states

- `COMPLETE`: every component has a measured or estimated numeric value.
- `BOUNDED_INCOMPLETE`: no value is fully missing, but at least one is below a known reporting limit. The result has a lower and upper bound.
- `UNBOUNDED_INCOMPLETE`: at least one component is missing or not applicable. The numeric value is only the known subtotal, not a complete total.

## Required tests

- Linear scaling.
- True-zero preservation.
- Missing-value propagation.
- Below-reporting-limit range scaling.
- Multi-ingredient aggregation.
- Cooked-batch concentration.
- Invalid quantities.
- Portion greater than batch.
- Repeated-input determinism.
- No rounding inside the domain.
