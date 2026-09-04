export type TatSegment =
  | "RECEIPT_TO_VALIDATION"
  | "ORDER_TO_COLLECTION"
  | "COLLECTION_TO_RECEIPT"
  | "RECEIPT_TO_TESTING"
  | "RECEIPT_TO_RESULT"
  | "RESULT_TO_VALIDATION"
  | "OVERALL";

export type CalculationMode = "CALENDAR" | "WORKING_TIME";

export type Priority = "" | "ROUTINE" | "STAT" | "ASAP";

export type TrendInterval = "DAILY" | "WEEKLY" | "MONTHLY";

export type CompareBy = "" | "LAB_UNIT" | "PRIORITY" | "SAMPLE_TYPE" | "ORDERING_SITE";

export interface SelectOption {
  id?: string;
  text?: string;
}

export interface DisplayListItem {
  id?: string;
  value?: string;
}

export interface TatFilters {
  fromDate: string;
  toDate: string;
  segment: TatSegment;
  calculationMode: CalculationMode;
  priority: Priority;
  includeCancelled: boolean;
  labUnitIds: Array<string | undefined>;
  testIds: Array<string | undefined>;
  sampleTypeId: string;
  orderingSiteId: string;
}

export type BuildTatQueryString = (filters: TatFilters | null, extra?: string) => string;

export interface TatBreakdownRow {
  dimensionValue: string;
  count: number;
  mean: number | null;
  median: number | null;
  percentile90: number | null;
  max: number;
}

export interface TatHistogramBin {
  binLabel: string;
  binMin?: number;
  binMax?: number;
  count: number;
}

export interface TatSummaryData {
  calculationMode?: CalculationMode;
  excludedDaysCount: number;
  totalCount: number;
  mean: number | null;
  median: number | null;
  percentile90: number | null;
  min: number | null;
  max: number | null;
  stdDeviation: number | null;
  histogram?: TatHistogramBin[];
  breakdown?: TatBreakdownRow[];
}

export interface TatDetailResult {
  labNumber?: string;
  testName?: string;
  labUnit?: string;
  priority?: string;
  orderCreated?: string | null;
  collected?: string | null;
  received?: string | null;
  testingStarted?: string | null;
  resultEntered?: string | null;
  validated?: string | null;
  selectedSegmentTat?: number | null;
  overallTat?: number | null;
}

export interface TatDetailResponse {
  totalCount: number;
  page?: number;
  pageSize?: number;
  calculationMode?: CalculationMode;
  results: TatDetailResult[];
}

export interface TatTrendPoint {
  period: string;
  mean?: number | null;
  median?: number | null;
  percentile90?: number | null;
  count: number;
}

export interface TatTrendSeries {
  label: string;
  dataPoints: TatTrendPoint[];
}

export interface TatTrendResponse {
  calculationMode?: CalculationMode;
  series: TatTrendSeries[];
}
