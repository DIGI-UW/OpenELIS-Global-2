import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import {
  appShellReachable,
  createServerRetrier,
  readSessionResponse,
  requestSession,
  retryServerNow,
  serverRetryDelay,
  subscribeServerWait,
} from "./serverConnection";

const jsonResponse = (status, body) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

describe("serverRetryDelay", () => {
  test("grows 1, 2, 4, 8, 15 s, then stays at 30 s with no limit", () => {
    const middle = () => 0.5;
    const delays = [1, 2, 3, 4, 5, 6, 7, 50, 1000].map((failures) =>
      serverRetryDelay(failures, middle),
    );
    expect(delays).toEqual([
      1000, 2000, 4000, 8000, 15000, 30000, 30000, 30000, 30000,
    ]);
  });

  test("spreads each gap by at most a tenth either way", () => {
    expect(serverRetryDelay(4, () => 0)).toBe(7200);
    expect(serverRetryDelay(4, () => 1)).toBe(8800);
    expect(serverRetryDelay(6, () => 0)).toBe(27000);
  });

  test("treats zero or negative failures as the first", () => {
    expect(serverRetryDelay(0, () => 0.5)).toBe(1000);
    expect(serverRetryDelay(-3, () => 0.5)).toBe(1000);
  });
});

describe("readSessionResponse", () => {
  test("returns the session the server answered", async () => {
    const session = { authenticated: true, csrf: "token" };
    await expect(
      readSessionResponse(jsonResponse(200, session)),
    ).resolves.toEqual(session);
  });

  test("returns a signed-out session for 401 and 403", async () => {
    for (const status of [401, 403]) {
      await expect(
        readSessionResponse(new Response("", { status })),
      ).resolves.toEqual({ authenticated: false });
    }
  });

  test.each([404, 408, 429, 500, 502, 503, 504])(
    "treats HTTP %i as the server not reached",
    async (status) => {
      await expect(
        readSessionResponse(
          new Response("<html>Bad Gateway</html>", { status }),
        ),
      ).rejects.toThrow(`HTTP ${status}`);
    },
  );

  test("treats a page served in place of the session as the server not reached", async () => {
    await expect(
      readSessionResponse(
        new Response("<!DOCTYPE html><html></html>", { status: 200 }),
      ),
    ).rejects.toThrow();
  });

  test("treats JSON without an authenticated flag as the server not reached", async () => {
    await expect(
      readSessionResponse(jsonResponse(200, { status: "starting" })),
    ).rejects.toThrow("without a session");
    await expect(readSessionResponse(jsonResponse(200, null))).rejects.toThrow(
      "without a session",
    );
  });
});

describe("requestSession", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  test("sends the session cookie and reads the answer", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse(200, { authenticated: false }));

    await expect(requestSession("/api/session")).resolves.toEqual({
      authenticated: false,
    });
    expect(fetchSpy).toHaveBeenCalledWith(
      "/api/session",
      expect.objectContaining({ credentials: "include" }),
    );
  });

  test("gives up on a request the proxy holds open", async () => {
    vi.useFakeTimers();
    vi.spyOn(globalThis, "fetch").mockImplementation(
      (url, { signal }) =>
        new Promise((resolve, reject) => {
          signal.addEventListener("abort", () =>
            reject(new DOMException("aborted", "AbortError")),
          );
        }),
    );

    const pending = requestSession("/api/session", 5000);
    const outcome = expect(pending).rejects.toThrow("aborted");
    await vi.advanceTimersByTimeAsync(5000);
    await outcome;
  });
});

describe("requestSession on a response that stalls mid-body", () => {
  afterEach(() => vi.restoreAllMocks());

  test("gives up when the headers arrive but the session body never finishes", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(
      (url, { signal }) =>
        new Promise((resolve) => {
          const body = new ReadableStream({
            start(controller) {
              controller.enqueue(new TextEncoder().encode('{"authenticated":'));
              signal.addEventListener("abort", () =>
                controller.error(new DOMException("aborted", "AbortError")),
              );
            },
          });
          resolve(
            new Response(body, {
              status: 200,
              headers: { "Content-Type": "application/json" },
            }),
          );
        }),
    );

    const stillPending = new Promise((resolve) =>
      setTimeout(() => resolve("still pending"), 500),
    );
    const outcome = await Promise.race([
      requestSession("/api/session", 25).then(
        () => "answered",
        (error) => `failed: ${error.name}`,
      ),
      stillPending,
    ]);

    expect(outcome).toBe("failed: AbortError");
  });
});

describe("appShellReachable", () => {
  afterEach(() => vi.restoreAllMocks());

  test("is true only when the start page is served", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response("<html></html>", { status: 200 }))
      .mockResolvedValueOnce(new Response("Bad Gateway", { status: 502 }))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"));

    await expect(appShellReachable()).resolves.toBe(true);
    await expect(appShellReachable()).resolves.toBe(false);
    await expect(appShellReachable()).resolves.toBe(false);
    expect(fetchSpy).toHaveBeenCalledWith(
      window.location.origin + "/",
      expect.objectContaining({ cache: "no-store" }),
    );
  });
});

describe("createServerRetrier", () => {
  let snapshots;
  let unsubscribe;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(Date.parse("2031-03-05T08:00:00Z"));
    snapshots = [];
    unsubscribe = subscribeServerWait((wait) => snapshots.push(wait));
  });

  afterEach(() => {
    unsubscribe();
    vi.useRealTimers();
  });

  const latest = () => snapshots[snapshots.length - 1];

  test("announces the next attempt while waiting and clears once done", async () => {
    expect(latest()).toBeNull();
    const retrier = createServerRetrier();
    const start = Date.now();

    const waiting = retrier.wait(4000);
    expect(latest()).toEqual({ retryAt: start + 4000 });

    await vi.advanceTimersByTimeAsync(4000);
    await waiting;
    expect(latest().retryAt).toBeLessThanOrEqual(Date.now());

    retrier.done();
    expect(latest()).toBeNull();
  });

  test("announces the soonest attempt when several wait", () => {
    const route = createServerRetrier();
    const session = createServerRetrier();
    const start = Date.now();

    route.wait(30000);
    session.wait(2000);
    expect(latest()).toEqual({ retryAt: start + 2000 });

    session.done();
    expect(latest()).toEqual({ retryAt: start + 30000 });
    route.done();
    expect(latest()).toBeNull();
  });

  test.each([
    ["Try now", () => retryServerNow()],
    [
      "the browser coming back online",
      () => window.dispatchEvent(new Event("online")),
    ],
    [
      "the tab becoming visible",
      () => {
        Object.defineProperty(document, "visibilityState", {
          configurable: true,
          get: () => "visible",
        });
        document.dispatchEvent(new Event("visibilitychange"));
      },
    ],
  ])("ends the wait at once on %s", async (_label, trigger) => {
    const retrier = createServerRetrier();
    let woke = false;
    retrier.wait(30000).then(() => {
      woke = true;
    });

    trigger();
    await Promise.resolve();

    expect(woke).toBe(true);
    retrier.done();
  });

  test("does not end the wait when the tab is hidden", async () => {
    Object.defineProperty(document, "visibilityState", {
      configurable: true,
      get: () => "hidden",
    });
    const retrier = createServerRetrier();
    let woke = false;
    retrier.wait(30000).then(() => {
      woke = true;
    });

    document.dispatchEvent(new Event("visibilitychange"));
    await Promise.resolve();

    expect(woke).toBe(false);
    retrier.done();
  });
});
