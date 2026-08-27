import { describe, expect, it } from "vitest";
import { getServiceWorkerUrl } from "./serviceWorkerRegistration";

describe("getServiceWorkerUrl", () => {
  it("uses the application root instead of the current route", () => {
    expect(getServiceWorkerUrl("/")).toBe("/service-worker.js");
  });

  it("preserves a configured application context path", () => {
    expect(getServiceWorkerUrl("/openelis")).toBe(
      "/openelis/service-worker.js",
    );
  });
});
