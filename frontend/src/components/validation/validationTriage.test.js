import {
  LANE_CLEAR,
  LANE_NEEDS_REVIEW,
  QC_FAIL,
  QC_PASS,
  computeLane,
  countByFilter,
  deriveSignals,
  filterTriaged,
  triageRows,
} from "./validationTriage";

/** A row every clearance input affirmatively clears. */
const clearRow = (overrides = {}) => ({
  analysisId: "1",
  normalRange: "10 - 20",
  normal: true,
  qcStatus: QC_PASS,
  nceOpen: false,
  modified: false,
  ackPending: false,
  nonconforming: false,
  critical: false,
  ...overrides,
});

describe("validationTriage — chips (FR-A2)", () => {
  it("a clean row carries no chips", () => {
    const [item] = triageRows([clearRow()]);
    expect(item.chips).toEqual([]);
  });

  it("renders a chip only for the signals present, in display order", () => {
    const [item] = triageRows([
      clearRow({ nceOpen: true, modified: true, qcStatus: QC_FAIL }),
    ]);
    expect(item.chips).toEqual(["nce", "qcFail", "modified"]);
  });

  it("any chip puts the row in Needs-review (chips correspond to lane)", () => {
    const rows = [
      clearRow({ nceOpen: true }),
      clearRow({ qcStatus: QC_FAIL }),
      clearRow({ modified: true }),
      clearRow({ ackPending: true }),
      clearRow({ nonconforming: true }),
    ];
    for (const item of triageRows(rows)) {
      expect(item.chips.length).toBeGreaterThan(0);
      expect(item.lane).toBe(LANE_NEEDS_REVIEW);
    }
  });
});

describe("validationTriage — lanes (FR-B1)", () => {
  it("a fully clear row lands in the Clear lane", () => {
    expect(computeLane(deriveSignals(clearRow()))).toBe(LANE_CLEAR);
  });

  it("critical and abnormal results are never Clear even with no chip", () => {
    expect(computeLane(deriveSignals(clearRow({ critical: true })))).toBe(
      LANE_NEEDS_REVIEW,
    );
    expect(computeLane(deriveSignals(clearRow({ normal: false })))).toBe(
      LANE_NEEDS_REVIEW,
    );
  });

  it("fail-safe: QC not evaluated is never read as QC passed", () => {
    const [unknown] = triageRows([clearRow({ qcStatus: "UNKNOWN" })]);
    const [missing] = triageRows([clearRow({ qcStatus: undefined })]);
    expect(unknown.chips).toEqual([]);
    expect(unknown.lane).toBe(LANE_NEEDS_REVIEW);
    expect(missing.lane).toBe(LANE_NEEDS_REVIEW);
  });

  it("fail-safe: a row with no reference range is not confidently range-matched", () => {
    expect(computeLane(deriveSignals(clearRow({ normalRange: "" })))).toBe(
      LANE_NEEDS_REVIEW,
    );
    expect(computeLane(deriveSignals(clearRow({ normalRange: null })))).toBe(
      LANE_NEEDS_REVIEW,
    );
    expect(computeLane(deriveSignals(clearRow({ normal: undefined })))).toBe(
      LANE_NEEDS_REVIEW,
    );
  });

  it("abnormal is only asserted when a range is known", () => {
    expect(
      deriveSignals(clearRow({ normalRange: "", normal: false })).abnormal,
    ).toBe(false);
    expect(deriveSignals(clearRow({ normal: false })).abnormal).toBe(true);
  });
});

describe("validationTriage — filters (FR-A3)", () => {
  const queue = triageRows([
    clearRow({ analysisId: "1" }),
    clearRow({ analysisId: "2", nceOpen: true }),
    clearRow({ analysisId: "3", modified: true, critical: true }),
    clearRow({ analysisId: "4", normal: false }),
    clearRow({ analysisId: "5", qcStatus: QC_FAIL, ackPending: true }),
  ]);

  it("counts are computed over the whole queue", () => {
    expect(countByFilter(queue)).toEqual({
      all: 5,
      needsReview: 4,
      nce: 1,
      qcFail: 1,
      modified: 1,
      ackPending: 1,
      critical: 1,
      abnormal: 1,
    });
  });

  it("a filter narrows the visible rows without changing the counts", () => {
    const narrowed = filterTriaged(queue, "modified");
    expect(narrowed.map((item) => item.row.analysisId)).toEqual(["3"]);
    expect(countByFilter(queue).all).toBe(5);
  });

  it("'needsReview' shows every non-clear row and 'all' shows everything", () => {
    expect(
      filterTriaged(queue, "needsReview").map((i) => i.row.analysisId),
    ).toEqual(["2", "3", "4", "5"]);
    expect(filterTriaged(queue, "all")).toHaveLength(5);
  });
});
