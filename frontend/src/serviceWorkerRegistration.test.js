import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { registerServiceWorker } from "./serviceWorkerRegistration";

describe("registerServiceWorker", () => {
  let loadHandler;
  let register;

  beforeEach(() => {
    vi.stubEnv("MODE", "production");
    register = vi.fn().mockResolvedValue({ scope: "/" });
    Object.defineProperty(global.navigator, "serviceWorker", {
      value: { register },
      configurable: true,
    });
    vi.spyOn(window, "addEventListener").mockImplementation(
      (event, handler) => {
        if (event === "load") {
          loadHandler = handler;
        }
      },
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllEnvs();
  });

  test("registers from the application root on nested routes", async () => {
    window.history.replaceState({}, "", "/analyzers/types");

    registerServiceWorker();
    loadHandler();
    await Promise.resolve();

    expect(register).toHaveBeenCalledWith("/service-worker.js");
  });
});
