/**
 * OGC-1029 (Validation v4 slice V3) — the guarded bulk release helpers: only
 * the Clear lane is ever sent, the request carries the page's own search key so
 * the server can re-derive the lane, and outcomes map to messages.
 */
import {
  bulkOutcomeKey,
  bulkSuccessMessages,
  bulkReleaseRequest,
  clearRows,
  earlierLevelClearRows,
  nextLevelOf,
  scopeToSignable,
  signClearMessages,
  triageRows,
  waitingForOthers,
} from "./validationTriage";

const row = (id, overrides = {}) => ({
  id,
  analysisId: String(100 + id),
  accessionNumber: "ACC1",
  normalRange: "10 - 20",
  normal: true,
  qcStatus: "PASS",
  nceOpen: false,
  modified: false,
  ackPending: false,
  nonconforming: false,
  critical: false,
  clear: true,
  ...overrides,
});

describe("clearRows", () => {
  it("returns only the rows the server holds clear, in queue order", () => {
    const triaged = triageRows([
      row(0),
      row(1, { normal: false, clear: false }),
      row(2, { qcStatus: "UNKNOWN" }),
      row(3),
      row(4, { modified: true, clear: false }),
    ]);
    expect(clearRows(triaged).map((r) => r.id)).toEqual([0, 2, 3]);
    expect(clearRows(undefined)).toEqual([]);
  });

  it("leaves out rows at an earlier review level or that this user cannot sign (OGC-1364 v2)", () => {
    const triaged = triageRows([
      row(0, { reviewLevels: 2, reviewLevel: 1 }),
      row(1, { reviewLevels: 2, reviewLevel: 2 }),
      row(2, {
        reviewLevels: 2,
        reviewLevel: 2,
        reviewBlockedReason: "alreadySigned",
      }),
      row(3),
    ]);
    expect(clearRows(triaged).map((r) => r.id)).toEqual([1, 3]);
  });
});

describe("bulkReleaseRequest", () => {
  it("an accession search is unranged and carries the accession", () => {
    const request = bulkReleaseRequest(
      { accessionNumber: "ACC1", testSectionId: "", testDate: "" },
      "?type=order&accessionNumber=ACC1",
      [row(0, { note: "ok", noteVisibility: "E" }), row(3)],
    );
    expect(request.accessionNumber).toBe("ACC1");
    expect(request.doRange).toBe(false);
    expect(request.rows).toEqual([
      {
        analysisId: "100",
        accessionNumber: "ACC1",
        note: "ok",
        noteVisibility: "E",
        noteContext: "VALIDATION",
      },
      {
        analysisId: "103",
        accessionNumber: "ACC1",
        note: "",
        noteVisibility: "",
        noteContext: "VALIDATION",
      },
    ]);
  });

  it("a lab-unit or date search is ranged and carries that key", () => {
    expect(
      bulkReleaseRequest(
        { testSectionId: "7" },
        "?type=routine&testSectionId=7",
        [],
      ),
    ).toMatchObject({ testSectionId: "7", doRange: true, rows: [] });
    expect(
      bulkReleaseRequest(undefined, "?type=testDate&date=01/09/2026", []),
    ).toMatchObject({
      fromDate: "01/09/2026",
      toDate: "01/09/2026",
      doRange: true,
    });
  });

  it("OGC-1418: carries the whole combined search, so the release covers only the rows it shows", () => {
    expect(
      bulkReleaseRequest(
        undefined,
        "?labNumber=ACC1..ACC9&testSectionId=7&fromDate=01/09/2026&toDate=30/09/2026&patientId=42",
        [],
      ),
    ).toMatchObject({
      labNumberFrom: "ACC1",
      labNumberTo: "ACC9",
      testSectionId: "7",
      fromDate: "01/09/2026",
      toDate: "30/09/2026",
      patientId: "42",
      accessionNumber: "",
      doRange: true,
    });
  });
});

describe("bulkOutcomeKey", () => {
  it("maps the server's reasons and an empty release", () => {
    expect(bulkOutcomeKey({ error: "bulkReleaseDisabled" })).toBe(
      "label.validation.bulk.error.bulkReleaseDisabled",
    );
    expect(bulkOutcomeKey({ error: "qcAcknowledgmentRequired" })).toBe(
      "label.validation.bulk.error.qcAcknowledgmentRequired",
    );
    expect(bulkOutcomeKey({ released: [], skipped: [{}] })).toBe(
      "label.validation.bulk.error.nothingReleased",
    );
    expect(bulkOutcomeKey({ error: "persistFailed" })).toBe(
      "label.validation.bulk.error.generic",
    );
    expect(
      bulkOutcomeKey({
        released: [],
        skipped: [{ analysisId: "1", reason: "stale" }],
      }),
    ).toBe("label.validation.bulk.error.stale");
    expect(
      bulkOutcomeKey({
        released: [],
        skipped: [
          { analysisId: "1", reason: "stale" },
          { analysisId: "2", reason: "notClear" },
        ],
      }),
    ).toBe("label.validation.bulk.error.nothingReleased");
    expect(bulkOutcomeKey(undefined)).toBe(
      "label.validation.bulk.error.generic",
    );
  });
});

describe("bulkSuccessMessages (OGC-1608)", () => {
  it("names tests validated in part apart from the ones released", () => {
    expect(
      bulkSuccessMessages({ released: ["1"], accepted: ["2"], skipped: [] }),
    ).toEqual([
      { id: "label.validation.bulk.success", values: { count: 1 } },
      { id: "label.validation.bulk.accepted", values: { count: 1 } },
    ]);
    expect(bulkSuccessMessages({ released: [], accepted: ["2"] })).toEqual([
      { id: "label.validation.bulk.accepted", values: { count: 1 } },
    ]);
    expect(
      bulkSuccessMessages({ released: ["1"], skipped: [{}], accepted: [] }),
    ).toEqual([
      {
        id: "label.validation.bulk.partial",
        values: { released: 1, skipped: 1 },
      },
    ]);
  });
});

describe("sequential sign-off in the queue (OGC-1364 v2 slice 2)", () => {
  const level = (n, of, overrides = {}) => ({
    sequentialSignOff: true,
    reviewLevels: of,
    reviewLevel: n,
    reviewBlockedReason: null,
    ...overrides,
  });

  it("a row someone else must sign waits for others; rows without the policy never do", () => {
    expect(
      waitingForOthers(
        row(0, level(2, 2, { reviewBlockedReason: "alreadySigned" })),
      ),
    ).toBe(true);
    expect(
      waitingForOthers(row(1, level(1, 2, { selfValidationBlocked: true }))),
    ).toBe(true);
    expect(waitingForOthers(row(2, level(1, 2)))).toBe(false);
    expect(waitingForOthers(row(3, { selfValidationBlocked: true }))).toBe(
      false,
    );
    expect(waitingForOthers(row(4))).toBe(false);
    expect(
      waitingForOthers(
        row(5, {
          reviewLevels: 1,
          reviewLevel: 1,
          selfValidationBlocked: true,
        }),
      ),
    ).toBe(false);
  });

  it("scoping hides rows waiting for others unless they are asked for", () => {
    const triaged = triageRows([
      row(0, level(1, 2)),
      row(1, level(2, 2, { reviewBlockedReason: "alreadySigned" })),
      row(
        2,
        level(2, 2, { reviewBlockedReason: "roleRequired", clear: false }),
      ),
    ]);
    expect(scopeToSignable(triaged, false).map((item) => item.row.id)).toEqual([
      0,
    ]);
    expect(scopeToSignable(triaged, true).map((item) => item.row.id)).toEqual([
      0, 1, 2,
    ]);
  });

  it("only clear rows at an earlier level this person can sign are bulk signed", () => {
    const triaged = triageRows([
      row(0, level(1, 2)),
      row(1, level(2, 2)),
      row(2, level(1, 3, { clear: false })),
      row(3, level(2, 3, { reviewBlockedReason: "alreadySigned" })),
      row(4, level(1, 2, { selfValidationBlocked: true })),
      row(5),
      row(6, level(2, 3)),
      row(7, { reviewLevels: 2, reviewLevel: 1 }),
    ]);
    expect(earlierLevelClearRows(triaged).map((r) => r.id)).toEqual([0, 6]);
    expect(earlierLevelClearRows(undefined)).toEqual([]);
  });

  it("names the level the bulk signature signs when every row shares it", () => {
    expect(nextLevelOf([row(0, level(1, 2)), row(1, level(1, 3))])).toBe(1);
    expect(nextLevelOf([row(0, level(1, 2)), row(1, level(2, 3))])).toBe(null);
    expect(nextLevelOf([])).toBe(null);
  });

  it("reports what the bulk signature signed and skipped", () => {
    expect(signClearMessages({ signed: ["101", "102"], skipped: [] })).toEqual({
      id: "label.validation.bulk.signed",
      values: { count: 2 },
    });
    expect(
      signClearMessages({
        signed: ["101"],
        skipped: [{ analysisId: "102", reason: "alreadySigned" }],
      }),
    ).toEqual({
      id: "label.validation.bulk.signedPartial",
      values: { signed: 1, skipped: 1 },
    });
    expect(
      signClearMessages({ signed: [], skipped: [{ reason: "notClear" }] }),
    ).toEqual({
      id: "label.validation.bulk.error.nothingSigned",
      values: {},
    });
  });
});
