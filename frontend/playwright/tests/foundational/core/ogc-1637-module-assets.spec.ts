import { test, expect } from "../../../helpers/test-base";
import { API_PREFIX } from "../../../helpers/admin-ui";

interface RegistryModule {
  id: string;
  state: string;
  assetBase?: string;
  frontend?: { entry?: string } | null;
}

interface Registry {
  modules: RegistryModule[];
}

// Tomcat's connector compresses every text response above 1 kB, so the REST
// API and a module's files reach the browser gzipped on every deployment that
// runs the image's server.xml. A module's entry is revalidated with a weak
// ETag that names the JAR build, and a matching If-None-Match is a 304 before
// the JAR is read. CI runs with no module installed; a developer's stack may
// carry one, and then its files are checked too.
test.describe("Module assets (OGC-1637)", () => {
  test("a REST response above 1 kB is compressed", async ({ page }) => {
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });

    const response = await page.request.get(`${API_PREFIX}/rest/menu`, {
      headers: { "Accept-Encoding": "gzip" },
    });
    expect(response.status()).toBe(200);
    expect((await response.body()).length).toBeGreaterThan(1024);
    expect(response.headers()["content-encoding"]).toBe("gzip");
    expect(response.headers()["vary"]?.toLowerCase()).toContain(
      "accept-encoding",
    );
  });

  test("a started module's entry is compressed and revalidates with a weak ETag", async ({
    page,
  }) => {
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    const registry = (await (
      await page.request.get(`${API_PREFIX}/rest/module-registry`)
    ).json()) as Registry;
    const started = registry.modules.find(
      (module) =>
        module.state === "STARTED" &&
        module.assetBase &&
        module.frontend?.entry,
    );
    test.skip(!started, "no started module with pages in this installation");

    const entryUrl = `${API_PREFIX}${started!.assetBase}${started!.frontend!.entry}`;
    const first = await page.request.get(entryUrl, {
      headers: { "Accept-Encoding": "gzip" },
      maxRedirects: 0,
    });
    // A dev stack started with --module redirects the entry to the module's
    // Vite dev server; only the JAR-served entry carries the core's headers.
    test.skip(
      first.status() === 307,
      "the module's pages come from its dev server here",
    );
    expect(first.status()).toBe(200);
    const etag = first.headers()["etag"];
    expect(etag).toMatch(/^W\/"[0-9a-f]{16}-[0-9a-f]+"$/);
    expect(first.headers()["cache-control"]).toBe("no-cache");
    expect(first.headers()["content-encoding"]).toBe("gzip");

    const again = await page.request.get(entryUrl, {
      headers: { "Accept-Encoding": "gzip", "If-None-Match": etag },
      maxRedirects: 0,
    });
    expect(again.status()).toBe(304);
    expect(again.headers()["etag"]).toBe(etag);

    const outdated = await page.request.get(entryUrl, {
      headers: { "Accept-Encoding": "gzip", "If-None-Match": 'W/"stale-0"' },
    });
    expect(outdated.status()).toBe(200);
    expect(outdated.headers()["etag"]).toBe(etag);
  });
});
