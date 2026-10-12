import type {
  ApiError,
  FoodPortionCalculationRequest,
  NutritionCalculationResult,
} from './types';

export class NutriCoachApiError extends Error {
  readonly code: string;
  readonly fieldErrors: ApiError['fieldErrors'];

  constructor(error: ApiError) {
    super(error.message);
    this.name = 'NutriCoachApiError';
    this.code = error.code;
    this.fieldErrors = error.fieldErrors;
  }
}

export async function calculateFoodPortion(
  request: FoodPortionCalculationRequest,
): Promise<NutritionCalculationResult> {
  const response = await fetch('/api/v1/calculations/food-portions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiError | null;
    if (body !== null) {
      throw new NutriCoachApiError(body);
    }
    throw new Error(`The API returned HTTP ${response.status}.`);
  }

  return (await response.json()) as NutritionCalculationResult;
}
