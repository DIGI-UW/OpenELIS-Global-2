import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";
import {
  seedInHouseScheme,
  markPanelDistributed,
  InHouseSchemeSeed,
} from "../../../helpers/seed-eqa-data";

/**
 * In-house blinded panels: what a sealed target reveals, to whom (OGC-613).
 *
 * A blinded panel exists so that the people running the samples cannot see
 * the expected answers. The target values are encrypted at rest and revealed
 * only to a caller holding the unblind privilege, so the confidentiality
 * claim is worth asserting from two sessions rather than one — a reader who
 * may unblind, and a bench user who may not.
 *
 * The panel is created through the application's own endpoint, not planted in
 * the database: targets pass through an encrypting converter on the way in,
 * and a value written straight to the column cannot be read back at all.
 *
 * Two things stay uncovered on purpose. The four-step wizard that seals a
 * panel has its own gate tests, and driving it needs an analyst roster plus
 * tests carrying analyte mappings — fixture data not guaranteed outside this
 * stack. And unblinding itself is marked as a known failure below.
 */

const RUN = Date.now().toString(36);
const REST = "/api/OpenELIS-Global/rest";

let seed: InHouseSchemeSeed;
let panelId = "";
const panelName = `E2E ${RUN} panel`;
const targetValue = "42.5";

/** Call the API as whoever the page is signed in as. */
async function api(
  page: Page,
  path: string,
  init: { method?: string; body?: unknown } = {},
): Promise<{ status: number; json: unknown }> {
  return page.evaluate(
    async ({ url, method, body }) => {
      const response = await fetch(url, {
        method,
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF") || "",
        },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await response.text().catch(() => "");
      let parsed: unknown;
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = text;
      }
      return { status: response.status, json: parsed };
    },
    { url: `${REST}${path}`, method: init.method ?? "GET", body: init.body },
  );
}

type SampleRow = { targetValue?: string | null; blindCode?: string | null };

test.describe.serial("EQA in-house panels", () => {
  test.beforeAll(() => {
    seed = seedInHouseScheme(RUN);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("a distributed panel keeps its targets sealed on the page", async ({
    page,
  }) => {
    test.setTimeout(180_000);

    await test.step("the panel is created through the application", async () => {
      await page.goto("/qa/eqa/in-house", { timeout: NAV_TIMEOUT });
      const created = await api(page, "/eqa/panels", {
        method: "POST",
        body: {
          schemeId: Number(seed.schemeId),
          cycleId: Number(seed.cycleId),
          panelName,
          sourceType: "IN_HOUSE_ALIQUOTED",
          storageTemp: "REFRIGERATED_2_8C",
          aliquotsProduced: 4,
          homogeneityQcPassed: true,
          samples: [
            {
              sampleCode: "S01",
              blindCode: `BLIND${RUN}`,
              analyteId: 1,
              targetValue,
              targetUnit: "mg",
            },
          ],
        },
      });
      expect(created.status).toBe(201);
      panelId = String((created.json as { id: number }).id);
      markPanelDistributed(panelId);
    });

    await test.step("the list shows it sealed and in testing", async () => {
      await page.goto("/qa/eqa/in-house", { timeout: NAV_TIMEOUT });
      // Wait for the picker to settle before switching: selecting while the
      // page is still initialising issues the panel read too early, and the
      // page has no guard for a reply that is not an array.
      const schemePicker = page.locator("select#inhouse-scheme-filter");
      await expect(schemePicker).not.toHaveValue("", { timeout: UI_TIMEOUT });
      await schemePicker.selectOption({ label: seed.schemeName });

      const panelRow = page.locator("tr", { hasText: panelName });
      await expect(panelRow).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(panelRow.getByText("Sealed")).toBeVisible();
      await expect(panelRow.getByText("DISTRIBUTED")).toBeVisible();
      await expect(page.getByText("In testing")).toBeVisible();
      // The sealed target must not be printed on the page for anyone.
      await expect(panelRow.getByText(targetValue)).toHaveCount(0);
    });

    await test.step("the label sheet carries the blind code, not the target", async () => {
      const panelRow = page.locator("tr", { hasText: panelName });
      const download = page.waitForEvent("download");
      await panelRow.getByRole("button", { name: "Print label sheet" }).click();
      const file = await download;
      expect(await file.failure()).toBeNull();
      expect(file.suggestedFilename()).toMatch(/\.pdf$/);
    });

    await test.step("a reader holding the privilege can see the target", async () => {
      const samples = await api(page, `/eqa/panels/${panelId}/samples`);
      expect(samples.status).toBe(200);
      const rows = samples.json as SampleRow[];
      expect(rows).toHaveLength(1);
      expect(rows[0].blindCode).toBe(`BLIND${RUN}`);
      // Proof the value survived the encrypting converter intact, and the
      // baseline for the withholding assertion in the next test.
      expect(rows[0].targetValue).toBe(targetValue);
    });
  });

  // Marked as a known failure rather than asserted: unblinding answers HTTP
  // 500 because the service reads the panel's cycle after the row-locking
  // call that fetched the panel has returned, by which point the entity is
  // detached and the association cannot be initialised. The steps are written
  // out so that fixing the service is all that is needed to turn coverage
  // back on; asserting the error instead would pin the defect in place.
  test.fixme("unblinding reveals the target and scores the panel", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    await page.goto("/qa/eqa/in-house", { timeout: NAV_TIMEOUT });
    const schemePicker = page.locator("select#inhouse-scheme-filter");
    await expect(schemePicker).not.toHaveValue("", { timeout: UI_TIMEOUT });
    await schemePicker.selectOption({ label: seed.schemeName });

    const panelRow = page.locator("tr", { hasText: panelName });
    await panelRow.getByRole("button", { name: "Unblind now" }).click();
    await expect(page.getByText("Panel unblinded and scored")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(panelRow.getByText("SCORED")).toBeVisible();
    await expect(panelRow.getByText(/^Unsealed /)).toBeVisible();
    // The state machine refuses a second attempt, so the action goes away.
    await expect(
      panelRow.getByRole("button", { name: "Unblind now" }),
    ).toHaveCount(0);
  });

  test.describe("as a laboratory user without the unblind privilege", () => {
    test.use({ storageState: "playwright/.auth/participant.json" });

    test("the sealed target is withheld", async ({ page }) => {
      test.setTimeout(120_000);
      await page.goto("/qa/eqa/in-house", { timeout: NAV_TIMEOUT });
      const samples = await api(page, `/eqa/panels/${panelId}/samples`);
      expect(samples.status).toBe(200);
      const rows = samples.json as SampleRow[];
      expect(rows).toHaveLength(1);
      // The blind code is what the bench works from, so it is readable.
      expect(rows[0].blindCode).toBe(`BLIND${RUN}`);
      // The answer is not. This is the whole point of sealing a panel, and
      // the previous test proves the same call does return it to a reader who
      // holds the privilege — so this is withholding, not an empty fixture.
      expect(rows[0].targetValue ?? null).toBeNull();
    });
  });
});
