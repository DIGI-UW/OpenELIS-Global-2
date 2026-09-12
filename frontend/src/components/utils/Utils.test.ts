import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  fetchFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServer,
} from "./Utils";

const settlePromiseChain = async () => {
  await new Promise((resolve) => setTimeout(resolve, 0));
};

describe("getFromOpenElisServer", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("silently ignores a failed fetch when its caller has already aborted it", async () => {
    let rejectFetch: (reason: Error) => void = () => undefined;
    const fetchPromise = new Promise<Response>((_resolve, reject) => {
      rejectFetch = reject;
    });
    const fetchMock = vi.fn().mockReturnValue(fetchPromise);
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => undefined);
    const callback = vi.fn();
    const controller = new AbortController();
    vi.stubGlobal("fetch", fetchMock);

    getFromOpenElisServer("/rest/analyzer-types", callback, controller.signal);
    controller.abort();
    rejectFetch(new TypeError("Failed to fetch"));
    await fetchPromise.catch(() => undefined);
    await settlePromiseChain();

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/rest/analyzer-types"),
      expect.objectContaining({ signal: controller.signal }),
    );
    expect(consoleError).not.toHaveBeenCalled();
    expect(callback).not.toHaveBeenCalled();
  });

  it("still reports a real network failure and completes with undefined", async () => {
    let rejectFetch: (reason: Error) => void = () => undefined;
    const fetchPromise = new Promise<Response>((_resolve, reject) => {
      rejectFetch = reject;
    });
    const error = new TypeError("Failed to fetch");
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => undefined);
    const callback = vi.fn();
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(fetchPromise));

    getFromOpenElisServer("/rest/analyzer-types", callback);
    rejectFetch(error);
    await fetchPromise.catch(() => undefined);
    await settlePromiseChain();

    expect(consoleError).toHaveBeenCalledWith(error);
    expect(callback).toHaveBeenCalledOnce();
    expect(callback).toHaveBeenCalledWith(undefined);
  });
});

describe("fetchFromOpenElisServer", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("returns JSON and forwards the session, locale and cancellation signal", async () => {
    const controller = new AbortController();
    const payload = { activeTestList: [] };
    vi.spyOn(Storage.prototype, "getItem").mockImplementation((key) =>
      key === "locale" ? "fr" : null,
    );
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({
        "content-type": "application/json; charset=utf-8",
      }),
      json: async () => payload,
    });
    vi.stubGlobal("fetch", fetchMock);
    await expect(
      fetchFromOpenElisServer("/rest/TestActivation", controller.signal),
    ).resolves.toEqual(payload);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/rest/TestActivation"),
      expect.objectContaining({
        credentials: "include",
        method: "GET",
        signal: controller.signal,
        headers: { "Accept-Language": "fr" },
      }),
    );
  });

  it("rejects an HTML login page returned with HTTP 200", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        headers: new Headers({ "content-type": "text/html" }),
      }),
    );
    await expect(
      fetchFromOpenElisServer("/rest/TestActivation"),
    ).rejects.toThrow("Expected a JSON response");
  });

  it("rejects an HTTP error response even when it has a JSON body", async () => {
    const json = vi.fn();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        headers: new Headers({ "content-type": "application/json" }),
        json,
      }),
    );

    await expect(
      fetchFromOpenElisServer("/rest/TestActivation"),
    ).rejects.toThrow("Request failed (500): /rest/TestActivation");
    expect(json).not.toHaveBeenCalled();
  });
});

describe("a rejected CSRF token", () => {
  const CSRF_REJECTION = {
    status: 403,
    json: async () => ({
      status: 403,
      message: "CSRF token missing or invalid",
    }),
  };
  const asResponse = (r: object) =>
    ({ ...r, clone: () => ({ ...r, clone: () => r }) }) as unknown as Response;

  let store: Record<string, string>;
  let reload: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    store = { CSRF: "stale-token" };
    reload = vi.fn();
    vi.stubGlobal("localStorage", {
      getItem: (k: string) => store[k] ?? null,
      setItem: (k: string, v: string) => {
        store[k] = v;
      },
    });
    vi.stubGlobal("location", { reload });
    vi.stubGlobal("alert", vi.fn());
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("refreshes the token and replays the request instead of reloading", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(asResponse(CSRF_REJECTION))
      .mockResolvedValueOnce(
        asResponse({
          ok: true,
          status: 200,
          json: async () => ({ csrf: "fresh-token" }),
        }),
      )
      .mockResolvedValueOnce(
        asResponse({ status: 200, json: async () => ({}) }),
      );
    vi.stubGlobal("fetch", fetchMock);
    const callback = vi.fn();

    postToOpenElisServer("/rest/thing", "{}" as unknown as never, callback);
    await settlePromiseChain();

    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls.filter((u) => u.endsWith("/session"))).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(store.CSRF).toBe("fresh-token");
    expect(fetchMock.mock.calls[2][1].headers["X-CSRF-Token"]).toBe(
      "fresh-token",
    );
    expect(callback).toHaveBeenCalledWith(200, undefined);
    expect(reload).not.toHaveBeenCalled();
  });

  it("fetches one session for requests rejected together", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => {
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
          ? asResponse({ status: 200, json: async () => ({}) })
          : asResponse(CSRF_REJECTION),
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    postToOpenElisServer("/rest/a", "{}" as unknown as never, vi.fn());
    postToOpenElisServer("/rest/b", "{}" as unknown as never, vi.fn());
    await settlePromiseChain();

    const sessions = fetchMock.mock.calls.filter((c) =>
      String(c[0]).endsWith("/session"),
    );
    expect(sessions).toHaveLength(1);
    expect(reload).not.toHaveBeenCalled();
  });

  it("still reloads when the replay is rejected too", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) =>
      Promise.resolve(
        String(url).endsWith("/session")
          ? asResponse({
              ok: true,
              status: 200,
              json: async () => ({ csrf: "fresh-token" }),
            })
          : asResponse(CSRF_REJECTION),
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    postToOpenElisServer("/rest/thing", "{}" as unknown as never, vi.fn());
    await settlePromiseChain();

    expect(reload).toHaveBeenCalledTimes(1);
  });
});
