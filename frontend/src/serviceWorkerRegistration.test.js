import { getServiceWorkerUrl } from "./serviceWorkerRegistration";

describe("getServiceWorkerUrl", () => {
  test("keeps registration rooted when the application is on a nested route", () => {
    expect(
      getServiceWorkerUrl(
        new URL("https://openelis.example/analyzers/42/mappings"),
      ),
    ).toBe("https://openelis.example/service-worker.js");
  });
});
