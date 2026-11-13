/** A run row's review category, as the server judges it (OGC-1200 FR-E1a). */
export type RunRowCategory =
  | "EXCEPTION"
  | "HELD"
  | "CRITICAL"
  | "ABNORMAL"
  | "NORMAL"
  | "CONTROL";

/** The run view's chips, in the order they are shown. */
export const RUN_CHIPS = [
  "ALL",
  "NEEDS_REVIEW",
  "CRITICAL",
  "ABNORMAL",
  "EXCEPTIONS",
  "HELD",
  "NORMAL",
] as const;

export type RunChip = (typeof RUN_CHIPS)[number];

export interface RunChipCounts {
  all: number;
  needsReview: number;
  critical: number;
  abnormal: number;
  exceptions: number;
  held: number;
  normal: number;
}

const CHIP_CATEGORIES: Record<RunChip, RunRowCategory[] | null> = {
  ALL: null,
  NEEDS_REVIEW: ["EXCEPTION", "HELD", "CRITICAL", "ABNORMAL"],
  CRITICAL: ["CRITICAL"],
  ABNORMAL: ["ABNORMAL"],
  EXCEPTIONS: ["EXCEPTION"],
  HELD: ["HELD"],
  NORMAL: ["NORMAL"],
};

const COUNT_FIELDS: Record<RunChip, keyof RunChipCounts> = {
  ALL: "all",
  NEEDS_REVIEW: "needsReview",
  CRITICAL: "critical",
  ABNORMAL: "abnormal",
  EXCEPTIONS: "exceptions",
  HELD: "held",
  NORMAL: "normal",
};

export const chipMatches = (chip: RunChip, category?: string | null) => {
  const categories = CHIP_CATEGORIES[chip];
  return categories === null || categories.includes(category as RunRowCategory);
};

export const chipCount = (counts: RunChipCounts | undefined, chip: RunChip) =>
  counts ? counts[COUNT_FIELDS[chip]] : 0;

/**
 * A selection that follows the run rather than the page (FR-E11a): the rows
 * picked one by one, plus, when `all` is set, every row a chip matches less the
 * rows taken out again. A row picked outside that chip is kept in `ids`.
 */
export interface RunSelection {
  all: boolean;
  chip: RunChip;
  ids: string[];
  excluded: string[];
}

export const EMPTY_SELECTION: RunSelection = {
  all: false,
  chip: "ALL",
  ids: [],
  excluded: [],
};

const inFilter = (
  selection: RunSelection,
  row: { id: string; category?: string | null },
) => selection.all && chipMatches(selection.chip, row.category);

export const isSelected = (
  selection: RunSelection,
  row: { id: string; category?: string | null },
) =>
  selection.ids.includes(row.id) ||
  (inFilter(selection, row) && !selection.excluded.includes(row.id));

export const toggleRow = (
  selection: RunSelection,
  row: { id: string; category?: string | null },
): RunSelection => {
  if (inFilter(selection, row)) {
    return {
      ...selection,
      excluded: selection.excluded.includes(row.id)
        ? selection.excluded.filter((id) => id !== row.id)
        : [...selection.excluded, row.id],
    };
  }
  return {
    ...selection,
    ids: selection.ids.includes(row.id)
      ? selection.ids.filter((id) => id !== row.id)
      : [...selection.ids, row.id],
  };
};

export const selectAllInFilter = (chip: RunChip): RunSelection => ({
  all: true,
  chip,
  ids: [],
  excluded: [],
});

export const isEmptySelection = (selection: RunSelection) =>
  !selection.all && selection.ids.length === 0;
