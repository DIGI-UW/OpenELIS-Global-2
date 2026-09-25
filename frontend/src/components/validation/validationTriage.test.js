import {
  LANE_CLEAR,
  LANE_NEEDS_REVIEW,
  QC_FAIL,
  QC_PASS,
  bulkUnavailableReasons,
  countByFilter,
  deriveSignals,
  filterTriaged,
  laneOf,
  triageRows,
} from "./validationTriage";

/** A row the server holds clear, with every signal input clean. */
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
  clear: true,
  ...overrides,
});

/** A row the server holds back, with the given signal inputs. */
const heldRow = (overrides = {}) => clearRow({ clear: false, ...overrides });

describe("validationTriage — chips (FR-A2)", () => {
  // OGC-1121: a critical value is a row-level signal, not only a filter.
  it("a critical row carries the Critical chip", () => {
    const [item] = triageRows([heldRow({ critical: true, normal: false })]);
    expect(item.chips).toEqual(["critical"]);
    expect(item.lane).toBe(LANE_NEEDS_REVIEW);
  });

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

  it("every signal the server holds a row back for shows as a chip", () => {
    const rows = [
      heldRow({ nceOpen: true }),
      heldRow({ qcStatus: QC_FAIL }),
      heldRow({ modified: true }),
      heldRow({ ackPending: true }),
      heldRow({ nonconforming: true }),
    ];
    for (const item of triageRows(rows)) {
      expect(item.chips.length).toBeGreaterThan(0);
      expect(item.lane).toBe(LANE_NEEDS_REVIEW);
    }
  });
});

describe("validationTriage — lanes (OGC-1226 FR-5, FR-6)", () => {
  it("the lane is the server's verdict on the row", () => {
    expect(laneOf(clearRow())).toBe(LANE_CLEAR);
    expect(laneOf(heldRow())).toBe(LANE_NEEDS_REVIEW);
    expect(triageRows([clearRow()])[0].lane).toBe(LANE_CLEAR);
  });

  it("anything but an explicit clear is Needs-review, whatever the signals say", () => {
    expect(laneOf(clearRow({ clear: undefined }))).toBe(LANE_NEEDS_REVIEW);
    expect(laneOf(clearRow({ clear: "true" }))).toBe(LANE_NEEDS_REVIEW);
    expect(laneOf(null)).toBe(LANE_NEEDS_REVIEW);
    expect(laneOf(heldRow({ chips: [] }))).toBe(LANE_NEEDS_REVIEW);
  });

  it("a QC verdict that does not exist raises no chip and does not decide the lane", () => {
    const [unknown] = triageRows([clearRow({ qcStatus: "UNKNOWN" })]);
    const [missing] = triageRows([clearRow({ qcStatus: undefined })]);
    expect(unknown.chips).toEqual([]);
    expect(unknown.lane).toBe(LANE_CLEAR);
    expect(missing.lane).toBe(LANE_CLEAR);
    expect(triageRows([heldRow({ qcStatus: QC_FAIL })])[0].chips).toEqual([
      "qcFail",
    ]);
  });

  it("a row with no reference range is not range-matched either way", () => {
    expect(deriveSignals(clearRow({ normalRange: "" })).rangeKnown).toBe(false);
    expect(deriveSignals(clearRow({ normalRange: null })).inRange).toBe(false);
    expect(deriveSignals(clearRow({ normal: undefined })).inRange).toBe(false);
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
    heldRow({ analysisId: "2", nceOpen: true }),
    heldRow({ analysisId: "3", modified: true, critical: true }),
    heldRow({ analysisId: "4", normal: false }),
    heldRow({ analysisId: "5", qcStatus: QC_FAIL, ackPending: true }),
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

describe("validationTriage — why the bulk button is unavailable (OGC-1226 FR-13 to FR-15)", () => {
  const keys = (reasons) => reasons.map((reason) => reason.key);

  it("says nothing while there is something to release", () => {
    expect(bulkUnavailableReasons(triageRows([clearRow(), heldRow()]))).toEqual(
      [],
    );
  });

  it("an empty queue is its own reason", () => {
    expect(keys(bulkUnavailableReasons(triageRows([])))).toEqual([
      "queueEmpty",
    ]);
    expect(keys(bulkUnavailableReasons(undefined))).toEqual(["queueEmpty"]);
  });

  it("bulk release switched off is named first, even when rows are clear", () => {
    expect(
      keys(
        bulkUnavailableReasons(triageRows([clearRow()]), {
          bulkAllowed: false,
        }),
      ),
    ).toEqual(["bulkDisabled"]);
    expect(
      keys(bulkUnavailableReasons(triageRows([]), { bulkAllowed: false })),
    ).toEqual(["bulkDisabled", "queueEmpty"]);
  });

  it("rows carrying signals are counted and the most common signals named", () => {
    const reasons = bulkUnavailableReasons(
      triageRows([
        heldRow({ normal: false }),
        heldRow({ normal: false, modified: true }),
        heldRow({ nceOpen: true }),
      ]),
    );
    expect(reasons).toEqual([
      { key: "signals", count: 3, dominant: ["abnormal", "modified", "nce"] },
    ]);
  });

  it("tests with no reference value are reported apart from risk signals, each with its count", () => {
    const reasons = bulkUnavailableReasons(
      triageRows([
        heldRow({ normalRange: "" }),
        heldRow({ normalRange: null, normal: undefined }),
        heldRow({ critical: true, normal: false }),
      ]),
    );
    expect(reasons).toEqual([
      { key: "signals", count: 1, dominant: ["critical"] },
      { key: "noReference", count: 2 },
    ]);
    expect(
      bulkUnavailableReasons(triageRows([heldRow({ normalRange: "" })])),
    ).toEqual([{ key: "noReference", count: 1 }]);
  });
});
