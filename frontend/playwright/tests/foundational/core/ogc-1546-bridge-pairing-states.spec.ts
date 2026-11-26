import { test, expect, type Page } from "../../../helpers/test-base";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1546 — the Analysers page said nothing when OpenELIS had no Bridge
 * address, and kept saying "Paired" when the Bridge could not be reached.
 * The pairing status is answered by the test, so each state shows on every
 * run whatever Bridge the stack has.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1546-bridge-pairing-states
 */

const BRIDGE_URL = "https://bridge.openelis.org:8443";

async function answerPairingStatus(page: Page, status: object) {
  await page.route("**/rest/analyzer/bridge-pairing", (route) =>
    route.request().method() === "GET"
      ? route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(status),
        })
      : route.continue(),
  );
  await page.goto("/analyzers", { waitUntil: "domcontentloaded" });
}

test.describe("OGC-1546 Bridge pairing states on the Analysers page", () => {
  test("no Bridge address says so", async ({ page }) => {
    await answerPairingStatus(page, {
      bridgeUrl: "",
      paired: false,
      pairsAutomatically: false,
    });

    await expect(page.getByTestId("bridge-pairing-not-configured")).toHaveText(
      /No Analyzer Bridge address is configured for OpenELIS\./,
      { timeout: UI_TIMEOUT },
    );
  });

  test("a paired install whose address was removed says no address is configured", async ({
    page,
  }) => {
    await answerPairingStatus(page, {
      bridgeUrl: "",
      paired: true,
      bridgeCertificateSha256: "b1e8c0442fcd7247c37a7eef105e0454",
      pairedAt: "2026-10-26T10:00:00Z",
      pairsAutomatically: true,
      pinCheck: null,
    });

    await expect(page.getByTestId("bridge-pairing-not-configured")).toHaveText(
      /No Analyzer Bridge address is configured for OpenELIS\./,
      { timeout: UI_TIMEOUT },
    );
    await expect(page.getByText("Paired with the Analyzer Bridge")).toHaveCount(
      0,
    );
  });

  test("an unreachable Bridge is not shown as paired", async ({ page }) => {
    await answerPairingStatus(page, {
      bridgeUrl: BRIDGE_URL,
      paired: true,
      bridgeCertificateSha256: "b1e8c0442fcd7247c37a7eef105e0454",
      pairedAt: "2026-10-26T10:00:00Z",
      pairsAutomatically: true,
      pinCheck: "UNREACHABLE",
    });

    const unreachable = page.getByTestId("bridge-pairing-unreachable");
    await expect(unreachable).toContainText(
      "OpenELIS could not reach the Analyzer Bridge.",
      { timeout: UI_TIMEOUT },
    );
    await expect(unreachable).toContainText(BRIDGE_URL);
    await expect(page.getByText("Paired with the Analyzer Bridge")).toHaveCount(
      0,
    );
  });

  test("a reachable, paired Bridge still reads Paired", async ({ page }) => {
    await answerPairingStatus(page, {
      bridgeUrl: BRIDGE_URL,
      paired: true,
      bridgeCertificateSha256: "b1e8c0442fcd7247c37a7eef105e0454",
      pairedAt: "2026-10-26T10:00:00Z",
      pairsAutomatically: true,
      pinCheck: "MATCHES",
    });

    await expect(page.getByText("Paired with the Analyzer Bridge")).toBeVisible(
      { timeout: UI_TIMEOUT },
    );
    await expect(page.getByTestId("bridge-pairing-unreachable")).toHaveCount(0);
    await expect(page.getByTestId("bridge-pairing-not-configured")).toHaveCount(
      0,
    );
  });
});
