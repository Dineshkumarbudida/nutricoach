export const nutrientCodes = [
  'ENERGY',
  'PROTEIN',
  'CARBOHYDRATE',
  'FAT',
  'FIBER',
] as const;

export type NutrientCode = (typeof nutrientCodes)[number];
export type ObservationStatus =
  | 'MEASURED'
  | 'ESTIMATED'
  | 'BELOW_REPORTING_LIMIT'
  | 'MISSING'
  | 'NOT_APPLICABLE';
export type Completeness = 'COMPLETE' | 'BOUNDED_INCOMPLETE' | 'UNBOUNDED_INCOMPLETE';

export interface NutrientObservationRequest {
  status: ObservationStatus;
  amount: number | null;
  reportingLimit: number | null;
}

export interface FoodPortionCalculationRequest {
  food: {
    foodId: string;
    version: string;
    displayName: string;
    referenceMassGrams: number;
    nutrients: Record<NutrientCode, NutrientObservationRequest>;
    source: {
      sourceCode: string;
      sourceVersion: string;
      sourceRecordId: string;
      licenseExpression: string;
    };
  };
  portionMassGrams: number;
}

export interface NutrientTotal {
  nutrient: NutrientCode;
  unit: 'KILOCALORIE' | 'GRAM' | 'MILLIGRAM' | 'MICROGRAM';
  knownAmount: number;
  completeness: Completeness;
  additionalUpperBound: number | null;
  measuredComponentCount: number;
  estimatedComponentCount: number;
  belowLimitComponentCount: number;
  missingComponentCount: number;
  notApplicableComponentCount: number;
}

export interface CalculationWarning {
  code: string;
  nutrient: NutrientCode | null;
  message: string;
}

export interface NutritionCalculationResult {
  calculationType: string;
  calculationContractVersion: string;
  basisMassGrams: number;
  totals: Record<NutrientCode, NutrientTotal>;
  sources: Array<{
    sourceCode: string;
    sourceVersion: string;
    sourceRecordId: string;
    licenseExpression: string;
  }>;
  warnings: CalculationWarning[];
  assumptions: Record<string, string>;
}

export interface ApiError {
  timestamp: string;
  code: string;
  message: string;
  fieldErrors: Array<{ field: string; message: string }>;
}
