import { afterEach, describe, expect, it, vi } from "vitest";
import { getFromOpenElisServer } from "./Utils";

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
