import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { acknowledgeAlert } from "./api";

/**
 * Issue #4280 asked for one place that holds the CSRF token, so a refresh
 * reaches every caller. The writes outside Utils.ts used to build the header
 * themselves and had no recovery at all: a token rotated by a login in another
 * tab failed the save and stayed failed. acknowledgeAlert stands in here for
 * the rest of them — they all reach the backend through the same apiFetch.
 */
describe("a write outside Utils.ts meeting a rotated CSRF token", () => {
  const csrfRejection = {
    ok: false,
    status: 403,
    json: async () => ({
      status: 403,
      message: "CSRF token missing or invalid",
    }),
  };
  const asResponse = (r) => ({ ...r, clone: () => ({ ...r, clone: () => r }) });

  let store;

  beforeEach(() => {
    store = { CSRF: "stale-token" };
    vi.stubGlobal("localStorage", {
      getItem: (key) => store[key] ?? null,
      setItem: (key, value) => {
        store[key] = value;
      },
    });
    vi.stubGlobal("location", { reload: vi.fn() });
    vi.stubGlobal("alert", vi.fn());
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("refreshes the token and lands the write", async () => {
    const fetchMock = vi.fn().mockImplementation((url) => {
      if (String(url).endsWith("/session")) {
        return Promise.resolve(
          asResponse({
            ok: true,
            status: 200,
            json: async () => ({ csrf: "fresh-token" }),
          }),
        );
      }
      return Promise.resolve(
        store.CSRF === "fresh-token"
          ? asResponse({
              ok: true,
              status: 200,
              json: async () => ({ acknowledged: true }),
            })
          : asResponse(csrfRejection),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(acknowledgeAlert("42", "seen")).resolves.toEqual({
      acknowledged: true,
    });

    const sessions = fetchMock.mock.calls.filter((call) =>
      String(call[0]).endsWith("/session"),
    );
    expect(sessions).toHaveLength(1);
    expect(store.CSRF).toBe("fresh-token");
  });
});
