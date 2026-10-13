import { afterEach, describe, expect, it, vi } from "vitest";
import { endServerSession } from "./endServerSession";

/**
 * OGC-1448: Logout on a non-default port. The server's redirect lost the port,
 * following it failed cross-origin, and the page never moved to the login page.
 */
describe("endServerSession", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("posts the logout with the CSRF token and does not follow the server's redirect", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(new Response(null, { status: 200 }));

    await endServerSession("/api/OpenELIS-Global", "csrf-1448");

    expect(fetchSpy).toHaveBeenCalledWith(
      "/api/OpenELIS-Global/Logout",
      expect.objectContaining({
        method: "POST",
        redirect: "manual",
        headers: expect.objectContaining({ "X-CSRF-Token": "csrf-1448" }),
      }),
    );
  });

  it("resolves when the request fails, so sign-out still reaches the login page", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(globalThis, "fetch").mockRejectedValue(
      new TypeError("Failed to fetch"),
    );

    await expect(
      endServerSession("/api/OpenELIS-Global", "csrf-1448"),
    ).resolves.toBeUndefined();
  });
});
