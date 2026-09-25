import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  seedVectorSite,
  addRequestingOrganization,
} from "../../../helpers/seed-vector-data";

/**
 * Add-order → collection density end-to-end (vector surveillance).
 *
 * The guard for the collection-site lineage: it drives the real intake path (no
 * significance/positivity data needed) and proves the dashboard density panel
 * groups the new collection under the site chosen at order entry.
 *
 * Chain exercised: order entry captures the sampling site + Quantity in Pool +
 * Traps/Nights → SampleAddService writes sample_item.collectionLocationId at
 * intake → fan-out carries it to the pool sibling → the dashboard groups
 * collectionDensity by that site and computes organisms per trap-night. A
 * regression that drops collectionLocationId at intake surfaces here as the row
 * appearing under a null site (or absent), not a value drift.
 *
 * Runs under the `core-demo` (CI) and `core-demo-video` (local) projects:
 *   npm run pw:test:core-demo -- ogc-372-vector-add-order-density.spec.ts
 *
 * Self-seeding: the sampling site is created via the admin REST API
 * (seedVectorSite) and the vector sample type is config-imported from the
 * classpath (configuration/sample-types/vector-sample-types.csv), so this spec
 * does not depend on any pre-seeded vector data.
 */

test.describe("OGC-372: Vector add-order → collection density", () => {
  test.describe.configure({ mode: "serial" });

  test("a new vector collection with trap-effort produces a per-trap-night density under its site", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const demo = createDemoPresentation(page, testInfo);

    // Unique per run so reruns against a persistent stack never collide and the
    // density row is unambiguous to locate by site name.
    const stamp = Date.now().toString().slice(-6);
    const siteCode = `E2E-VS-${stamp}`;
    const siteName = `E2E Vector Density Site ${stamp}`;

    // Quantity in Pool > 1 triggers fan-out (parent → pool sibling), which is
    // what makes the collection visible to the density query.
    const quantity = 12;
    const traps = 6;
    const nights = 2; // trap-nights = 12 → density = 12 / 12 = 1.00

    await test.step("Title card", async () => {
      await demo.title(
        "OGC-372 / V-04: Add a vector collection → density",
        "Capture Traps + Nights at order entry; the dashboard computes organisms per trap-night, grouped by the collection site",
      );
    });

    await test.step("Seed a sampling site via the admin API", async () => {
      await seedVectorSite(page, { code: siteCode, name: siteName });
    });

    await test.step("Open the vector order-entry form", async () => {
      await page.goto("/order/vector/enter");
      await expect(page.locator("#labNumber")).toBeVisible({ timeout: 15_000 });
      await demo.evidence("372-1-order-entry");
    });

    await test.step("Generate a lab number", async () => {
      await page.locator(".generate-link").click();
      // Generate calls the accession-number provider; wait for it to populate.
      await expect(page.locator("#labNumber")).not.toHaveValue("", {
        timeout: 15_000,
      });
    });

    await test.step("Select the seeded sampling site", async () => {
      await page.locator("#vec-site-search").fill(siteName);
      const resultRow = page.locator(".search-results tr", {
        hasText: siteCode,
      });
      await expect(resultRow).toBeVisible({ timeout: 10_000 });
      await resultRow.getByRole("button", { name: "Select" }).click();
      // The selected-site card confirms the choice stuck.
      await expect(page.getByText(`${siteCode} — ${siteName}`)).toBeVisible();
      // The collection date defaults to today and persists to
      // sample_item.collectionDate; we deliberately do NOT touch the picker so
      // this exercises the real default-entry path the dashboard must handle.
    });

    await test.step("Pick a vector sample type (config-imported from the classpath)", async () => {
      // Domain-V sample types come from /rest/vector-sample-types, populated by
      // configuration/sample-types/vector-sample-types.csv at startup.
      await expect(page.locator("#sampleType-0")).toBeVisible();
      await page.locator("#sampleType-0").selectOption({ label: "Mosquito" });
    });

    await test.step("Enter Quantity in Pool + Traps + Nights (trap-effort)", async () => {
      await page.locator("#collectedVolume-0").fill(String(quantity));
      await page.locator("#trapCount-0").fill(String(traps));
      await page.locator("#trapNights-0").fill(String(nights));
      // Confirm the values committed to the inputs before saving (guards against
      // a lost onChange dropping the trap-effort from the payload).
      await expect(page.locator("#collectedVolume-0")).toHaveValue(
        String(quantity),
      );
      await expect(page.locator("#trapCount-0")).toHaveValue(String(traps));
      await expect(page.locator("#trapNights-0")).toHaveValue(String(nights));
      await demo.evidence("372-2-trap-effort-entered");
    });

    await test.step("Add the requesting organization (required for ENV/Vector)", async () => {
      await addRequestingOrganization(page, `E2E Vector Org ${stamp}`);
      await demo.evidence("372-2b-requester-added");
    });

    await test.step("Save the collection", async () => {
      await page.getByRole("button", { name: "Save", exact: true }).click();
      // Carbon renders the toast text in more than one node; assert at least one.
      await expect(
        page
          .getByText("Sample Order Entry has been saved successfully")
          .first(),
      ).toBeVisible({ timeout: 20_000 });
      await demo.evidence("372-3-saved");
    });

    await test.step("Open the dashboard and apply", async () => {
      await page.goto("/VectorSurveillanceReport");
      await expect(
        page.getByRole("heading", { name: /Vector Surveillance/i }),
      ).toBeVisible({ timeout: 15_000 });
      await page.locator('[data-testid="vector-apply"]').click();
      await expect(page.locator('[data-testid="panel-density"]')).toBeVisible({
        timeout: 15_000,
      });
    });

    await test.step("Density table shows the collection under its site with a real per-trap-night density", async () => {
      const table = page.locator('[data-testid="density-table"]');
      await expect(table).toBeVisible({ timeout: 10_000 });

      // The row must group under the site chosen at order entry — the lineage
      // guard. A dropped collectionLocationId would land it under a null site.
      const row = table.locator("tbody tr", { hasText: siteName });
      await expect(row).toBeVisible({ timeout: 10_000 });

      const cells = row.locator("td");
      // Specimens (abundance): SUM of pool quantities = the entered quantity.
      await expect(cells.nth(2)).toHaveText(String(quantity));
      // Trap-nights: traps × nights, proving effort was captured at intake.
      await expect(cells.nth(3)).toHaveText(String(traps * nights));
      // Per-trap-night density: a real 2-decimal value, NOT the degrade text.
      await expect(cells.nth(4)).toHaveText(/^\d+\.\d{2}$/);

      await demo.evidence("372-4-density-by-site");
    });
  });

  test("adding a sampling site inline selects it without leaving the order form", async ({
    page,
  }) => {
    test.setTimeout(60_000);
    const stamp = Date.now().toString().slice(-6);
    const siteCode = `E2E-INLINE-${stamp}`;
    const siteName = `E2E Inline Site ${stamp}`;

    await page.goto("/order/vector/enter");
    await expect(page.locator("#vec-site-search")).toBeVisible({
      timeout: 15_000,
    });

    // No admin screen, no navigation away — search by name, and when there's no
    // match, create the site inline. It's persisted server-side at order-save.
    await page.locator("#vec-site-search").fill(siteName);
    await page.getByRole("button", { name: /Add new site/i }).click();

    // The inline new site is selected with editable name/code; set our code.
    await page.locator("#vec-site-code").fill(siteCode);
    await expect(page.locator("#vec-site-name")).toHaveValue(siteName);
    await expect(page.getByText(`${siteCode} — ${siteName}`)).toBeVisible({
      timeout: 15_000,
    });
  });
});
