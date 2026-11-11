import { dispositionRequests } from "./nceDisposition";

/** OGC-1023 (R4, FR-E3) — disposition → shipped endpoint sequences. */
describe("nceDisposition (FR-E3)", () => {
  const row = { analysisId: "18", sampleItemId: "17", testId: "6" };

  it("CANCEL maps to sample-management cancel-test", () => {
    expect(dispositionRequests("CANCEL", row)).toEqual([
      {
        url: "/rest/sample-management/cancel-test",
        body: { analysisId: "18", sampleItemId: "17" },
      },
    ]);
  });

  it("RETEST cancels the non-conforming analysis then re-orders the test", () => {
    expect(dispositionRequests("RETEST", row)).toEqual([
      {
        url: "/rest/sample-management/cancel-test",
        body: { analysisId: "18", sampleItemId: "17" },
      },
      {
        url: "/rest/sample-management/add-tests",
        body: { sampleItemIds: ["17"], testIds: ["6"] },
      },
    ]);
  });

  it("REJECT and NONE ride the row save — no direct requests", () => {
    expect(dispositionRequests("REJECT", row)).toEqual([]);
    expect(dispositionRequests("NONE", row)).toEqual([]);
  });

  it("missing identifiers disable the sequence rather than sending garbage", () => {
    expect(dispositionRequests("CANCEL", { analysisId: "18" })).toEqual([]);
    expect(dispositionRequests("RETEST", { sampleItemId: "17" })).toEqual([]);
  });
});
