import { test, expect, Page } from "../../../helpers/test-base";
import { API_PREFIX } from "../../../helpers/admin-ui";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

interface RegistryModule {
  id: string;
  state: string;
  assetBase?: string;
  frontend?: {
    entry?: string;
    routes?: Array<{ path: string; expose?: string }>;
  } | null;
}

interface Registry {
  modules: RegistryModule[];
}

/** How many more of the module's files the page requested over 1.5 s. */
function moduleRequestsOverASecond(
  page: Page,
  assetBase: string,
): Promise<number> {
  return page.evaluate(async (base) => {
    const count = () =>
      performance
        .getEntriesByType("resource")
        .filter((entry) => entry.name.includes(base)).length;
    const before = count();
    await new Promise((resolve) => setTimeout(resolve, 1500));
    return count() - before;
  }, assetBase);
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
    // The registry names the files under the running JAR's digest, so the
    // entry itself is immutable there (OGC-1638); the plain form stays no-cache.
    expect(started!.assetBase).toMatch(
      /\/modules\/[a-z0-9-]+\/[0-9a-f]{16}\/$/,
    );
    expect(first.headers()["cache-control"]).toContain("immutable");
    expect(first.headers()["content-encoding"]).toBe("gzip");
    const plain = await page.request.get(
      `${API_PREFIX}/modules/${started!.id}/${started!.frontend!.entry}`,
      { headers: { "Accept-Encoding": "gzip" }, maxRedirects: 0 },
    );
    expect(plain.status()).toBe(200);
    // Tomcat's authenticator adds its own "Cache-Control: private" header
    // in front of the controller's, so the directives are read, not the
    // whole value.
    expect(plain.headers()["cache-control"]).toContain("no-cache");
    expect(plain.headers()["cache-control"]).not.toContain("immutable");
    const otherDigest = await page.request.get(
      `${API_PREFIX}/modules/${started!.id}/ffffffffffffffff/${started!.frontend!.entry}`,
      { maxRedirects: 0 },
    );
    expect(otherDigest.status()).toBe(404);

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

  test("a warm reload of a module page sends no request for the module's files", async ({
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
        module.frontend?.entry &&
        module.frontend?.routes?.length,
    );
    test.skip(!started, "no started module with pages in this installation");
    const entryUrl = `${API_PREFIX}${started!.assetBase}${started!.frontend!.entry}`;
    const served = await page.request.get(entryUrl, { maxRedirects: 0 });
    test.skip(
      served.status() === 307,
      "the module's pages come from its dev server here",
    );
    // Under the shell's own dev server a page's late chunks (the ones first
    // fetched after other module files were already cached) are fetched
    // again on a warm load, whatever their headers; a built shell caches
    // every one of them (verified on an openelis-docker deployment). The case
    // is about the built shell, so it runs against one.
    const shellFromDevServer = await page.evaluate(() =>
      Boolean(document.querySelector('script[src*="/@vite/client"]')),
    );
    test.skip(
      shellFromDevServer,
      "the shell comes from its dev server here; run against a built shell",
    );

    // A dev server loads more than the 250 resources the timing buffer keeps
    // by default, and the module's files come last.
    await page.addInitScript(() =>
      performance.setResourceTimingBufferSize(5000),
    );
    const route = started!.frontend!.routes![0].path;
    await page.goto(route, { waitUntil: "networkidle" });
    await expect(page.locator(".cds--side-nav")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    // The page's own chunk arrives only once the shell has read the registry
    // and the remote entry, later than the navigation's network-idle, and the
    // page pulls in more chunks after it renders. Wait for the page's chunk,
    // then until the module's requests stop: whatever the warm load fetches
    // first would count as a transfer.
    const pageChunk = `assets/${started!.frontend!.routes![0].expose!.replace(/^\.\//, "")}-`;
    await expect
      .poll(
        () =>
          page.evaluate(
            ({ base, chunk }) =>
              performance
                .getEntriesByType("resource")
                .some(
                  (entry) =>
                    entry.name.includes(base) && entry.name.includes(chunk),
                ),
            { base: started!.assetBase!, chunk: pageChunk },
          ),
        { timeout: UI_TIMEOUT },
      )
      .toBe(true);
    await expect
      .poll(() => moduleRequestsOverASecond(page, started!.assetBase!), {
        timeout: UI_TIMEOUT,
      })
      .toBe(0);
    const coldFiles = new Set(
      await page.evaluate(
        (base) =>
          performance
            .getEntriesByType("resource")
            .filter((entry) => entry.name.includes(base))
            .map((entry) => entry.name),
        started!.assetBase!,
      ),
    );

    await page.goto(route, { waitUntil: "networkidle" });
    await expect(page.locator(".cds--side-nav")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    // Every file sits under the JAR's digest and is immutable: the second
    // load comes from the browser cache without a revalidation, which the
    // resource timing reports as a transfer of zero bytes (a 304 would be a
    // few hundred). Chromium still emits request events for cache hits, so
    // the timing entries are the evidence, not the request count.
    const moduleTransfers = await page.evaluate(
      (assetBase) =>
        performance
          .getEntriesByType("resource")
          .filter((entry) => entry.name.includes(assetBase))
          .map((entry) => ({
            url: entry.name,
            transfer: (entry as PerformanceResourceTiming).transferSize,
          })),
      started!.assetBase!,
    );
    expect(moduleTransfers.length).toBeGreaterThan(0);
    // A file the cold load never reached is fetched for the first time here;
    // every file it did fetch must come back without a byte from the server.
    expect(
      moduleTransfers.filter(
        (entry) => coldFiles.has(entry.url) && entry.transfer > 0,
      ),
    ).toEqual([]);
  });
});
