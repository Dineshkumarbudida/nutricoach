import { FormEvent, useMemo, useState } from 'react';
import { calculateFoodPortion, NutriCoachApiError } from './api';
import {
  nutrientCodes,
  type FoodPortionCalculationRequest,
  type NutrientCode,
  type NutritionCalculationResult,
} from './types';

const nutrientLabels: Record<NutrientCode, string> = {
  ENERGY: 'Energy (kcal)',
  PROTEIN: 'Protein (g)',
  CARBOHYDRATE: 'Carbohydrate (g)',
  FAT: 'Total fat (g)',
  FIBER: 'Dietary fiber (g)',
};

const unitLabels: Record<string, string> = {
  KILOCALORIE: 'kcal',
  GRAM: 'g',
  MILLIGRAM: 'mg',
  MICROGRAM: 'µg',
};

type NutrientInput = Record<NutrientCode, string>;

const initialNutrients: NutrientInput = {
  ENERGY: '100',
  PROTEIN: '10',
  CARBOHYDRATE: '20',
  FAT: '2',
  FIBER: '',
};

function parsePositiveNumber(value: string, field: string): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${field} must be a positive number.`);
  }
  return parsed;
}

function parseNonNegativeOrMissing(value: string): number | null {
  if (value.trim() === '') {
    return null;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) {
    throw new Error('Nutrient values must be non-negative or blank for missing.');
  }
  return parsed;
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 4,
  }).format(value);
}

function App() {
  const [displayName, setDisplayName] = useState('Synthetic demonstration food');
  const [referenceMass, setReferenceMass] = useState('100');
  const [portionMass, setPortionMass] = useState('250');
  const [nutrients, setNutrients] = useState<NutrientInput>(initialNutrients);
  const [result, setResult] = useState<NutritionCalculationResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const resultRows = useMemo(() => {
    if (result === null) {
      return [];
    }
    return nutrientCodes.map((code) => result.totals[code]);
  }, [result]);

  function updateNutrient(code: NutrientCode, value: string) {
    setNutrients((current) => ({ ...current, [code]: value }));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setResult(null);

    try {
      const nutrientPayload = Object.fromEntries(
        nutrientCodes.map((code) => {
          const amount = parseNonNegativeOrMissing(nutrients[code]);
          return [
            code,
            amount === null
              ? { status: 'MISSING', amount: null, reportingLimit: null }
              : { status: 'MEASURED', amount, reportingLimit: null },
          ];
        }),
      ) as FoodPortionCalculationRequest['food']['nutrients'];

      const request: FoodPortionCalculationRequest = {
        food: {
          foodId: 'synthetic-ui-fixture',
          version: '1',
          displayName: displayName.trim() || 'Synthetic demonstration food',
          referenceMassGrams: parsePositiveNumber(referenceMass, 'Reference mass'),
          nutrients: nutrientPayload,
          source: {
            sourceCode: 'synthetic-ui',
            sourceVersion: '1',
            sourceRecordId: 'fixture-1',
            licenseExpression: 'TEST-ONLY',
          },
        },
        portionMassGrams: parsePositiveNumber(portionMass, 'Portion mass'),
      };

      setBusy(true);
      setResult(await calculateFoodPortion(request));
    } catch (caught) {
      if (caught instanceof NutriCoachApiError) {
        const details = caught.fieldErrors
          .map((fieldError) => `${fieldError.field}: ${fieldError.message}`)
          .join('; ');
        setError(details ? `${caught.message}: ${details}` : caught.message);
      } else if (caught instanceof Error) {
        setError(caught.message);
      } else {
        setError('An unknown error occurred.');
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <main>
      <header className="hero">
        <p className="eyebrow">Foundation milestone</p>
        <h1>NutriCoach calculation workbench</h1>
        <p className="lead">
          Test deterministic mass scaling while preserving missing nutrient data. This screen uses
          synthetic inputs only; it is not a food database and does not provide dietary advice.
        </p>
      </header>

      <section className="panel" aria-labelledby="input-heading">
        <div className="section-heading">
          <div>
            <p className="step">Step 1</p>
            <h2 id="input-heading">Describe a versioned food snapshot</h2>
          </div>
          <span className="badge">Synthetic fixture</span>
        </div>

        <form onSubmit={submit}>
          <div className="form-grid form-grid-three">
            <label>
              Display name
              <input
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                required
              />
            </label>
            <label>
              Reference mass (g)
              <input
                type="number"
                min="0.000001"
                step="any"
                value={referenceMass}
                onChange={(event) => setReferenceMass(event.target.value)}
                required
              />
            </label>
            <label>
              Portion mass (g)
              <input
                type="number"
                min="0.000001"
                step="any"
                value={portionMass}
                onChange={(event) => setPortionMass(event.target.value)}
                required
              />
            </label>
          </div>

          <fieldset>
            <legend>Nutrients on the reference-mass basis</legend>
            <p className="hint">Leave a field blank to mark it as missing—not zero.</p>
            <div className="form-grid nutrients-grid">
              {nutrientCodes.map((code) => (
                <label key={code}>
                  {nutrientLabels[code]}
                  <input
                    type="number"
                    min="0"
                    step="any"
                    value={nutrients[code]}
                    onChange={(event) => updateNutrient(code, event.target.value)}
                    placeholder="Missing"
                  />
                </label>
              ))}
            </div>
          </fieldset>

          <button type="submit" disabled={busy}>
            {busy ? 'Calculating…' : 'Calculate portion'}
          </button>
        </form>

        {error !== null && (
          <div className="alert alert-error" role="alert">
            <strong>Calculation failed.</strong> {error}
          </div>
        )}
      </section>

      <section className="panel" aria-labelledby="result-heading">
        <div className="section-heading">
          <div>
            <p className="step">Step 2</p>
            <h2 id="result-heading">Inspect the deterministic result</h2>
          </div>
          {result !== null && <span className="badge badge-success">Calculated by code</span>}
        </div>

        {result === null ? (
          <p className="empty-state">Submit the synthetic food snapshot to see scaled totals.</p>
        ) : (
          <>
            <div className="result-summary">
              <div>
                <span>Calculation</span>
                <strong>{result.calculationType}</strong>
              </div>
              <div>
                <span>Contract</span>
                <strong>{result.calculationContractVersion}</strong>
              </div>
              <div>
                <span>Basis mass</span>
                <strong>{formatNumber(result.basisMassGrams)} g</strong>
              </div>
              <div>
                <span>Warnings</span>
                <strong>{result.warnings.length}</strong>
              </div>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th scope="col">Nutrient</th>
                    <th scope="col">Known subtotal</th>
                    <th scope="col">Completeness</th>
                    <th scope="col">Interpretation</th>
                  </tr>
                </thead>
                <tbody>
                  {resultRows.map((total) => (
                    <tr key={total.nutrient}>
                      <th scope="row">{nutrientLabels[total.nutrient]}</th>
                      <td>
                        {formatNumber(total.knownAmount)} {unitLabels[total.unit] ?? total.unit}
                      </td>
                      <td>
                        <span className={`status status-${total.completeness.toLowerCase()}`}>
                          {total.completeness.replaceAll('_', ' ')}
                        </span>
                      </td>
                      <td>
                        {total.completeness === 'COMPLETE'
                          ? 'All supplied components had numeric observations.'
                          : total.completeness === 'BOUNDED_INCOMPLETE'
                            ? `Known subtotal plus less than ${formatNumber(total.additionalUpperBound ?? 0)} ${unitLabels[total.unit] ?? total.unit}.`
                            : 'Known subtotal only; no defensible upper bound is available.'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {result.warnings.length > 0 && (
              <div className="alert alert-warning">
                <strong>Uncertainty warnings</strong>
                <ul>
                  {result.warnings.map((warning, index) => (
                    <li key={`${warning.code}-${warning.nutrient ?? 'general'}-${index}`}>
                      {warning.message}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <details>
              <summary>Calculation assumptions and provenance</summary>
              <dl className="details-grid">
                {Object.entries(result.assumptions).map(([key, value]) => (
                  <div key={key}>
                    <dt>{key}</dt>
                    <dd>{value}</dd>
                  </div>
                ))}
              </dl>
              {result.sources.map((source) => (
                <p key={`${source.sourceCode}-${source.sourceRecordId}`} className="source-line">
                  Source: {source.sourceCode} {source.sourceVersion} / {source.sourceRecordId} /{' '}
                  {source.licenseExpression}
                </p>
              ))}
            </details>
          </>
        )}
      </section>
    </main>
  );
}

export default App;
