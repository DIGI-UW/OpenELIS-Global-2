import { afterEach, describe, expect, test, vi } from "vitest";
import { removeMockNetwork } from "../playwright/helpers/create-analyzer-from-profile";

describe("removeMockNetwork", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("continues polling after the mock control port briefly disconnects", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ analyzers: [{ name: "gene-xpert" }] })),
      )
      .mockResolvedValueOnce(new Response(null, { status: 202 }))
      .mockRejectedValueOnce(new TypeError("fetch failed"))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ analyzers: [] }), { status: 200 }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(removeMockNetwork("gene-xpert")).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenCalledTimes(4);
  });
});
