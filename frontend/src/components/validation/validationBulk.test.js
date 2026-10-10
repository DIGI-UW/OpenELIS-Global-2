/**
 * OGC-1029 (Validation v4 slice V3) — the guarded bulk release helpers: only
 * the Clear lane is ever sent, the request carries the page's own search key so
 * the server can re-derive the lane, and outcomes map to messages.
 */
import {
  bulkOutcomeKey,
  bulkReleaseRequest,
  clearRows,
  triageRows,
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
