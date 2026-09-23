import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import {
  getServiceWorkerUrl,
  registerServiceWorker,
} from "./serviceWorkerRegistration";

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

describe("getServiceWorkerUrl", () => {
  test("uses the application root instead of the current route", () => {
    expect(getServiceWorkerUrl("/")).toBe("/service-worker.js");
  });

  test("preserves a configured application context path", () => {
    expect(getServiceWorkerUrl("/openelis")).toBe(
      "/openelis/service-worker.js",
    );
  });
});
