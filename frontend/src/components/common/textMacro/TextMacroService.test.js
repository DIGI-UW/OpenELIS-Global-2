import { bulkAdminMacros, exportAdminMacros } from "./TextMacroService";

beforeEach(() => {
  vi.restoreAllMocks();
  localStorage.setItem("CSRF", "csrf-token");
  global.fetch = vi.fn();
});

describe("TextMacroService administration", () => {
  it("returns the server attachment without inventing a filename", async () => {
    fetch.mockResolvedValue(
      new Response("code,expansion_text\r\n.gpc,Text\r\n", {
        status: 200,
        headers: {
          "Content-Type": "text/csv;charset=UTF-8",
          "Content-Disposition":
            'attachment; filename="reviewed-text-macros.csv"',
        },
      }),
    );

    const result = await exportAdminMacros();

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/rest/text-macros/admin/export"),
      expect.objectContaining({ credentials: "include" }),
    );
    expect(result.filename).toBe("reviewed-text-macros.csv");
    expect(await result.blob.text()).toContain(".gpc,Text");
  });

  it("posts only selected IDs and the explicit action with CSRF", async () => {
    fetch.mockResolvedValue(
      new Response(
        JSON.stringify({
          action: "DEACTIVATE",
          affectedCount: 1,
          affectedCodes: [".gpc"],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    await bulkAdminMacros({ ids: ["generated-id"], action: "DEACTIVATE" });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/rest/text-macros/admin/bulk"),
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          ids: ["generated-id"],
          action: "DEACTIVATE",
        }),
        headers: expect.objectContaining({ "X-CSRF-Token": "csrf-token" }),
      }),
    );
    expect(fetch.mock.calls[0][1].body).not.toContain("actor");
  });
});
