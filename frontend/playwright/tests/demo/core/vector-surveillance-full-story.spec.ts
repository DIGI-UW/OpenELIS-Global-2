import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  identifyAndResolvePositive,
  addRequestingOrganization,
} from "../../../helpers/seed-vector-data";

/**
 * Vector Surveillance — full user story demo (single continuous take).
 *
 * Records the end-to-end feature against a populated instance: show the live
 * dashboard, add a sampling site inline on the order form, record a collection
 * with trap-effort, confirm the species + a positive pathogen result, then show
 * the dashboard reflecting the new collection's density AND positivity.
 *
 * Record against the real demo dataset:
 *   BASE_URL=https://vector-demo.openelis-global.org TEST_USER=admin \
 *   TEST_PASS='adminADMIN!' npm run pw:test:core-demo-video -- \
 *   vector-surveillance-full-story.spec.ts
 *
 * The assertions make a passing recording a true proof: the new site shows a
 * computed per-trap-night density (24 / (6x2) = 2.00) grouped under its name.
 */
test.describe("Vector Surveillance — full user story", () => {
  test("field collection to surveillance insight", async ({
    page,
  }, testInfo) => {
    test.setTimeout(240_000);
    const demo = createDemoPresentation(page, testInfo);

    // Site is env-overridable so the demo can be re-recorded against an
    // instance that already has the default site (adding a site with a code
    // that already exists would fail the inline-create step).
    const siteCode = process.env.DEMO_SITE_CODE || "IDN-BKS-01";
    const siteName = process.env.DEMO_SITE_NAME || "Bekasi Riverside";
    const quantity = 24;
    const traps = 6;
    const nights = 2; // trap-nights = 12 → density = 24 / 12 = 2.00

    await test.step("Title card", async () => {
      await demo.title(
        "Vector Surveillance",
        "From a field collection to computed surveillance indices",
        4000,
      );
    });

    await test.step("Act 1 — the live surveillance dashboard", async () => {
      await page.goto("/VectorSurveillanceReport");
      await expect(
        page.getByRole("heading", { name: /Vector Surveillance/i }),
      ).toBeVisible({ timeout: 20_000 });
      await page.locator('[data-testid="vector-apply"]').click();
      await expect(page.locator('[data-testid="panel-density"]')).toBeVisible({
        timeout: 20_000,
      });
      await demo.evidence("story-1-dashboard-before");
      await demo.pause(3500);
    });

    await test.step("Act 2 — open the order form and add the site inline", async () => {
      await page.goto("/order/vector/enter");
      await expect(page.locator("#labNumber")).toBeVisible({ timeout: 15_000 });
      await page.locator(".generate-link").click();
      await expect(page.locator("#labNumber")).not.toHaveValue("", {
        timeout: 15_000,
      });

      // The feature Casey asked for: add a sampling site without admin access or
      // a second screen. Search by name; with no match, create it inline — the
      // site is persisted server-side when the order is saved.
      await page.locator("#vec-site-search").fill(siteName);
      await page.getByRole("button", { name: /Add new site/i }).click();
      await page.locator("#vec-site-code").fill(siteCode);
      await expect(page.getByText(`${siteCode} — ${siteName}`)).toBeVisible({
        timeout: 15_000,
      });
      await demo.evidence("story-2-site-added-inline");
      await demo.pause(2000);
    });

    await test.step("Act 3 — record the collection with trap-effort + pathogen tests", async () => {
      await page.locator("#sampleType-0").selectOption({ label: "Mosquito" });
      await page.locator("#collectedVolume-0").fill(String(quantity));
      await page.locator("#trapCount-0").fill(String(traps));
      await page.locator("#trapNights-0").fill(String(nights));
      for (const term of ["Malaria", "CSP"]) {
        await page.locator("#testSearch-0").fill(term);
        await page.locator('label[for^="test-0-"]').first().click();
      }
      // The requesting organization, added inline on the same form (no admin
      // screen). Required to save an ENV/Vector order. Derived from the site
      // name so it stays unique per recording.
      await addRequestingOrganization(
        page,
        `${siteName} District Health Office`,
      );
      await demo.evidence("story-3-collection-entered");
      await page.getByRole("button", { name: "Save", exact: true }).click();
      await expect(
        page
          .getByText("Sample Order Entry has been saved successfully")
          .first(),
      ).toBeVisible({ timeout: 20_000 });
      await demo.pause(1500);
    });

    await test.step("Act 4 — lab confirms the species and enters a positive result", async () => {
      const labNumber = await page.locator("#labNumber").inputValue();
      await demo.title(
        "In the lab",
        `Specimens from ${siteName} confirmed as Anopheles; a positive Malaria result is entered`,
        4000,
      );
      await identifyAndResolvePositive(page, labNumber, "anopheles");
    });

    await test.step("Act 5 — the dashboard reflects the new collection", async () => {
      await page.goto("/VectorSurveillanceReport");
      await page.locator('[data-testid="vector-apply"]').click();
      await expect(page.locator('[data-testid="panel-density"]')).toBeVisible({
        timeout: 20_000,
      });

      const table = page.locator('[data-testid="density-table"]');
      const row = table.locator("tbody tr", { hasText: siteName });
      await expect(row).toBeVisible({ timeout: 15_000 });
      await row.scrollIntoViewIfNeeded();
      const cells = row.locator("td");
      await expect(cells.nth(2)).toHaveText(String(quantity)); // abundance
      await expect(cells.nth(3)).toHaveText(String(traps * nights)); // trap-nights
      await expect(cells.nth(4)).toHaveText("2.00"); // organisms per trap-night
      await demo.evidence("story-4-dashboard-density");
      await demo.pause(2500);

      // The positive result flows through to the positivity/MIR panels.
      await expect(
        page.locator('[data-testid="panel-positivity"]'),
      ).toBeVisible();
      await demo.evidence("story-5-dashboard-positivity");
      await demo.pause(3000);
    });
  });
});
