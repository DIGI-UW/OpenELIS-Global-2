import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchFromOpenElisServer, getFromOpenElisServer } from "./Utils";

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
