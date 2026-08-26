import {
  generateWhonetExport,
  getWhonetFilterOptions,
  getWhonetPreview,
} from "./WhonetService";

const query = {
  from: "2026-07-01",
  to: "2026-07-31",
  specimen: ["sample-type-2", "sample-type-1"],
  organism: ["organism-1"],
  origin: ["OUTPATIENT"],
  significance: ["NORMAL_FLORA", "CLINICALLY_SIGNIFICANT"],
  dedup: "FIRST_ISOLATE_7_DAY",
  page: 2,
  pageSize: 50,
};

describe("WhonetService", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("requests preview with deterministic query composition", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ exportedRows: 2 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(getWhonetPreview(query)).resolves.toEqual({ exportedRows: 2 });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/OpenELIS-Global/rest/microbiology/whonet/preview?from=2026-07-01&to=2026-07-31&specimen=sample-type-1&specimen=sample-type-2&organism=organism-1&origin=OUTPATIENT&significance=CLINICALLY_SIGNIFICANT&significance=NORMAL_FLORA&dedup=FIRST_ISOLATE_7_DAY&page=2&pageSize=50",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("loads period-scoped filter choices from the same date contract", async () => {
    const options = {
      specimenTypes: [{ id: "sample-type-1", label: "Blood" }],
    };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(options), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(getWhonetFilterOptions(query)).resolves.toEqual(options);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/OpenELIS-Global/rest/microbiology/whonet/filter-options?from=2026-07-01&to=2026-07-31",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("rejects structured preview errors", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          error: "MICROBIOLOGY_REFERENCE_INVALID",
          message: "to must be on or after from",
        }),
        {
          status: 400,
          statusText: "Bad Request",
          headers: { "Content-Type": "application/json" },
        },
      ),
    );

    await expect(getWhonetPreview(query)).rejects.toMatchObject({
      status: 400,
      code: "MICROBIOLOGY_REFERENCE_INVALID",
    });
  });

  it("returns the server attachment name", async () => {
    const response = new Response("csv", {
      headers: {
        "Content-Type": "text/csv",
        "Content-Disposition":
          'attachment; filename="WHONET_2026-07-01_to_2026-07-31.csv"',
      },
    });
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(response);

    const result = await generateWhonetExport(query);
    expect(result.filename).toBe("WHONET_2026-07-01_to_2026-07-31.csv");
    await expect(result.blob.text()).resolves.toBe("csv");
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/OpenELIS-Global/rest/microbiology/whonet/exports",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify(query),
      }),
    );
  });
});
