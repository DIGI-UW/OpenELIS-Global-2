import { ComponentType } from "react";

// We test the internal logic by importing the module and calling the factory
// that lazyWithRetry passes to React.lazy. Since React.lazy is opaque, we
// mock it to capture the factory function and invoke it directly.

let capturedFactory: (() => Promise<{ default: ComponentType<any> }>) | null =
  null;

jest.mock("react", () => ({
  ...jest.requireActual("react"),
  lazy: (factory: () => Promise<{ default: ComponentType<any> }>) => {
    capturedFactory = factory;
    return factory;
  },
}));

// Must import AFTER mock is set up
import lazyWithRetry from "./lazyWithRetry";

describe("lazyWithRetry", () => {
  const DummyComponent = (() => null) as unknown as ComponentType<any>;

  beforeEach(() => {
    capturedFactory = null;
    sessionStorage.clear();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("returns the component on first successful load", async () => {
    const factory = jest.fn().mockResolvedValue({ default: DummyComponent });
    lazyWithRetry(factory);

    const result = await capturedFactory!();
    expect(result.default).toBe(DummyComponent);
    expect(factory).toHaveBeenCalledTimes(1);
  });

  it("clears sessionStorage reload flag on success", async () => {
    const factory = jest.fn().mockResolvedValue({ default: DummyComponent });
    const key = `lazyRetry:${factory.toString().slice(0, 100)}`;
    sessionStorage.setItem(key, "true");

    lazyWithRetry(factory);
    await capturedFactory!();

    expect(sessionStorage.getItem(key)).toBeNull();
  });

  it("does NOT retry on non-chunk errors (TypeError)", async () => {
    const typeError = new TypeError("Cannot read properties of undefined");
    const factory = jest.fn().mockRejectedValue(typeError);

    lazyWithRetry(factory);

    await expect(capturedFactory!()).rejects.toThrow(typeError);
    expect(factory).toHaveBeenCalledTimes(1);
  });

  it("does NOT retry on non-chunk errors (SyntaxError)", async () => {
    const syntaxError = new SyntaxError("Unexpected token");
    const factory = jest.fn().mockRejectedValue(syntaxError);

    lazyWithRetry(factory);

    await expect(capturedFactory!()).rejects.toThrow(syntaxError);
    expect(factory).toHaveBeenCalledTimes(1);
  });

  // The retry tests use real timers with the actual 1500ms delay mocked out
  // by overriding setTimeout to fire immediately.
  describe("with mocked setTimeout for retry delay", () => {
    let origSetTimeout: typeof setTimeout;

    beforeEach(() => {
      origSetTimeout = global.setTimeout;
      // Make setTimeout fire immediately (skip the 1500ms delay)
      global.setTimeout = ((fn: () => void) => {
        fn();
        return 0;
      }) as any;
    });

    afterEach(() => {
      global.setTimeout = origSetTimeout;
    });

    it("retries on ChunkLoadError and succeeds", async () => {
      const chunkError = new Error("Loading chunk 42 failed");
      chunkError.name = "ChunkLoadError";

      const factory = jest
        .fn()
        .mockRejectedValueOnce(chunkError)
        .mockRejectedValueOnce(chunkError)
        .mockResolvedValue({ default: DummyComponent });

      lazyWithRetry(factory);
      const result = await capturedFactory!();

      expect(result.default).toBe(DummyComponent);
      expect(factory).toHaveBeenCalledTimes(3);
    });

    it("triggers page reload after retries exhaust on chunk error", async () => {
      const reloadMock = jest.fn();
      Object.defineProperty(window, "location", {
        configurable: true,
        value: { reload: reloadMock },
      });

      const chunkError = new Error("Loading chunk 99 failed");
      chunkError.name = "ChunkLoadError";

      const factory = jest.fn().mockRejectedValue(chunkError);

      lazyWithRetry(factory);

      // The promise never resolves (page is reloading), so we race it
      const timeout = new Promise((r) => origSetTimeout(r, 100));
      await Promise.race([capturedFactory!().catch(() => {}), timeout]);

      expect(reloadMock).toHaveBeenCalledTimes(1);
      const key = `lazyRetry:${factory.toString().slice(0, 100)}`;
      expect(sessionStorage.getItem(key)).toBe("true");
    });

    it("throws instead of reloading if already reloaded once", async () => {
      const chunkError = new Error("Loading chunk 99 failed");
      chunkError.name = "ChunkLoadError";

      const factory = jest.fn().mockRejectedValue(chunkError);
      const key = `lazyRetry:${factory.toString().slice(0, 100)}`;
      sessionStorage.setItem(key, "true");

      lazyWithRetry(factory);

      await expect(capturedFactory!()).rejects.toThrow(
        "Loading chunk 99 failed",
      );
    });

    it("detects chunk errors by message pattern (not just name)", async () => {
      const networkError = new Error(
        "Failed to fetch dynamically imported module",
      );
      const factory = jest
        .fn()
        .mockRejectedValueOnce(networkError)
        .mockResolvedValue({ default: DummyComponent });

      lazyWithRetry(factory);
      const result = await capturedFactory!();

      expect(result.default).toBe(DummyComponent);
      expect(factory).toHaveBeenCalledTimes(2);
    });
  });
});
