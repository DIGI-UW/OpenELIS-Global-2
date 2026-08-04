vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerForBlob: vi.fn(),
}));

import {
  getFromOpenElisServer,
  postToOpenElisServerForBlob,
} from "../utils/Utils";
import { generateWhonetExport, getWhonetPreview } from "./WhonetService";

const query = {
  from: "2026-07-01",
  to: "2026-07-31",
  significance: "CLINICALLY_SIGNIFICANT",
  dedup: "FIRST_ISOLATE_7_DAY",
  page: 2,
  pageSize: 50,
};

describe("WhonetService", () => {
  beforeEach(() => vi.clearAllMocks());

  it("requests preview with deterministic query composition", async () => {
    getFromOpenElisServer.mockImplementation((path, callback) =>
      callback({ exportedRows: 2 }),
    );

    await expect(getWhonetPreview(query)).resolves.toEqual({ exportedRows: 2 });
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/microbiology/whonet/preview?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&dedup=FIRST_ISOLATE_7_DAY&page=2&pageSize=50",
      expect.any(Function),
    );
  });

  it("returns the server attachment name and audit run identifier", async () => {
    const blob = new Blob(["csv"], { type: "text/csv" });
    const response = new Response(blob, {
      headers: {
        "Content-Disposition":
          'attachment; filename="WHONET_2026-07-01_to_2026-07-31.csv"',
        "X-WHONET-Export-Run-Id": "run-1",
      },
    });
    postToOpenElisServerForBlob.mockImplementation(
      (_path, _payload, callback) => callback(blob, response),
    );

    await expect(generateWhonetExport(query)).resolves.toEqual({
      blob,
      filename: "WHONET_2026-07-01_to_2026-07-31.csv",
      runId: "run-1",
    });
    expect(postToOpenElisServerForBlob).toHaveBeenCalledWith(
      "/rest/microbiology/whonet/exports",
      JSON.stringify(query),
      expect.any(Function),
      expect.any(Function),
    );
  });
});
