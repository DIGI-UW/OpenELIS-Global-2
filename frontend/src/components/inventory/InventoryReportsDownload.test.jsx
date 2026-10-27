import { describe, it, expect, vi } from "vitest";

describe("export filename from Content-Disposition", () => {
  const filenameFrom = async (header) => {
    vi.resetModules();
    let captured;
    vi.doMock("../utils/Utils.ts", () => ({
      getFromOpenElisServer: vi.fn(),
      postToOpenElisServerJsonResponse: vi.fn(),
      postToOpenElisServerForBlob: (endpoint, body, onSuccess) =>
        onSuccess(new Blob(["a,b"]), {
          headers: {
            get: (name) =>
              name === "Content-Disposition" ? header : "text/csv",
          },
        }),
    }));
    const { ReportsAPI } = await import("./InventoryService");
    captured = await ReportsAPI.generate({
      reportType: "RECEIVED",
      exportFormat: "CSV",
    });
    return captured.filename;
  };

  it("keeps a quoted filename without its quotes", async () => {
    expect(await filenameFrom('attachment; filename="received.csv"')).toBe(
      "received.csv",
    );
  });

  it("reads an unquoted filename", async () => {
    expect(await filenameFrom("attachment; filename=stock-on-hand.csv")).toBe(
      "stock-on-hand.csv",
    );
  });

  it("stops at the next header parameter", async () => {
    expect(
      await filenameFrom('attachment; filename="expiring.csv"; charset=utf-8'),
    ).toBe("expiring.csv");
  });

  it("falls back when the header is absent", async () => {
    expect(await filenameFrom(null)).toBe("inventory-report");
  });
});
