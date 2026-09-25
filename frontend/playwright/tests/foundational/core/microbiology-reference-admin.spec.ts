import type { Page } from "@playwright/test";
import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import {
  seedMicrobiologyReferenceAdmin,
  seedReviewedMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const ADMIN_BASE = "/MasterListsPage/MicrobiologyReference";
const API_BASE = "/api/OpenELIS-Global/rest/microbiology";

const canonicalQuery = (values: Record<string, string> = {}) => {
  const params = new URLSearchParams({
    status: "ALL",
    sort: "name",
    page: "1",
    pageSize: "20",
    ...values,
  });
  return params.toString();
};

const expectQuery = async (page: Page, key: string, value: string) => {
  await expect
    .poll(() => new URL(page.url()).searchParams.get(key))
    .toBe(value);
};

const openRowAction = async (page: Page, rowText: string, action: string) => {
  const row = page.getByRole("row").filter({ hasText: rowText });
  await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  await row.getByRole("button", { name: "Options" }).click();
  await page.getByRole("menuitem").filter({ hasText: action }).click();
};

const expectActiveReference = async (
  page: Page,
  resource: "organisms" | "antibiotics",
  id: string,
  active: boolean,
) => {
  await expect
    .poll(async () => {
      const response = await page.request.get(
        `${API_BASE}/reference/${resource}`,
      );
      expect(response.ok()).toBeTruthy();
      const options = (await response.json()) as Array<{ id: string }>;
      return options.some((option) => option.id === id);
    })
    .toBe(active);
};

test.describe("OGC-782 M3 microbiology reference administration", () => {
  test("lists deployment Patient Origins without Phase 1B mutation controls", async ({
    page,
  }) => {
    await page.goto(
      `${ADMIN_BASE}/patient-origins?${canonicalQuery({ q: "" })}`,
      { waitUntil: "domcontentloaded" },
    );

    await expect(
      page.getByRole("heading", { name: "Microbiology reference data" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const table = page.getByRole("table");
    await expect(table).toContainText("Inpatient");
    await expect(table).toContainText("Outpatient");
    await expect(table).toContainText("ICU");
    await expect(table).toContainText("Emergency");
    await expect(table).toContainText("Long-term Care");
    await expect(table).toContainText("Unknown");
    await expect(table).toContainText("INP");
    await expect(table).toContainText("LTC");
    await expect(table).toContainText("UNK");
    await expect(
      page.getByRole("button", { name: /Add patient origin/i }),
    ).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Options" })).toHaveCount(0);

    await page.getByPlaceholder("Search reference data").fill("Long-term Care");
    await expectQuery(page, "q", "Long-term Care");
    await expect(table.getByRole("row")).toHaveCount(2);
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(table).toContainText("Long-term Care", {
      timeout: LONG_TIMEOUT,
    });
    await expect(table).not.toContainText("Inpatient");
  });

  test("edits and safely deactivates service-created vocabulary", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyReferenceAdmin(page);

    await test.step("Reload canonical organism search state", async () => {
      await page.goto(
        `${ADMIN_BASE}/organisms?${canonicalQuery({ q: "Reference organism (UAT)" })}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(
        page.getByRole("heading", { name: "Microbiology reference data" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByRole("row").filter({ hasText: "Reference organism (UAT)" }),
      ).toBeVisible();
      await expectQuery(page, "q", "Reference organism (UAT)");
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("row").filter({ hasText: "Reference organism (UAT)" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Edit and confirm organism deactivation impact", async () => {
      await openRowAction(page, "Reference organism (UAT)", "Edit");
      await expectQuery(page, "edit", seeded.organismId);
      const organismDialog = page.getByRole("dialog");
      await organismDialog
        .getByLabel("Notes")
        .fill("Synthetic UAT vocabulary reviewed in M3");
      await organismDialog.getByRole("button", { name: "Save" }).click();
      await expect(page.getByText("Reference saved")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });

      await openRowAction(page, "Reference organism (UAT)", "Deactivate");
      await expectQuery(page, "edit", `deactivate:${seeded.organismId}`);
      const impactDialog = page.getByRole("dialog");
      await expect(
        impactDialog.getByRole("heading", {
          name: "Review deactivation impact",
        }),
      ).toBeVisible();
      await expect(impactDialog).toContainText(
        "prevents new selection but preserves those historical references",
      );
      await impactDialog.getByRole("button", { name: /Deactivate$/ }).click();
      await expectActiveReference(page, "organisms", seeded.organismId, false);

      await openRowAction(page, "Reference organism (UAT)", "Reactivate");
      await expectActiveReference(page, "organisms", seeded.organismId, true);
    });

    await test.step("Repeat the guarded workflow for an antibiotic", async () => {
      await page.goto(
        `${ADMIN_BASE}/antibiotics?${canonicalQuery({ q: "REFUAT" })}`,
        { waitUntil: "domcontentloaded" },
      );
      await openRowAction(page, "Reference antibiotic (UAT)", "Edit");
      const antibioticDialog = page.getByRole("dialog");
      await antibioticDialog
        .getByLabel("Notes")
        .fill("Synthetic UAT antibiotic reviewed in M3");
      await antibioticDialog.getByRole("button", { name: "Save" }).click();
      await openRowAction(page, "Reference antibiotic (UAT)", "Deactivate");
      await page
        .getByRole("dialog")
        .getByRole("button", { name: /Deactivate$/ })
        .click();
      await expectActiveReference(
        page,
        "antibiotics",
        seeded.antibioticId,
        false,
      );
      await openRowAction(page, "Reference antibiotic (UAT)", "Reactivate");
      await expectActiveReference(
        page,
        "antibiotics",
        seeded.antibioticId,
        true,
      );
    });
  });

  test("publishes an immutable AST panel version", async ({ page }) => {
    const seeded = await seedMicrobiologyReferenceAdmin(page);
    const reviewed = await seedReviewedMicrobiologyCase(page);
    const originalRunsResponse = await page.request.get(
      `${API_BASE}/ast/runs?isolateId=${encodeURIComponent(reviewed.isolateId)}`,
    );
    expect(originalRunsResponse.ok()).toBeTruthy();
    const originalRuns = (await originalRunsResponse.json()) as Array<{
      id: string;
      panelId: string;
    }>;
    const originalRun = originalRuns.find(
      (run) => run.id === reviewed.astRunId,
    );
    expect(originalRun?.panelId).toBe(seeded.astPanelId);

    await page.goto(
      `${ADMIN_BASE}/ast-panels?${canonicalQuery({ q: "Gram negative AST panel (UAT)" })}`,
      { waitUntil: "domcontentloaded" },
    );
    const currentRow = page
      .getByRole("row")
      .filter({ hasText: "Gram negative AST panel (UAT)" })
      .filter({ hasText: "Current" });
    await expect(currentRow).toBeVisible({ timeout: LONG_TIMEOUT });
    const versionText = await currentRow.getByRole("cell").nth(2).innerText();
    const originalVersion = Number(versionText.replace(/^v/, ""));
    await currentRow.getByRole("button", { name: "Options" }).click();
    await page.getByRole("menuitem", { name: "Publish new version" }).click();
    await expectQuery(page, "edit", seeded.astPanelId);
    const editor = page.getByRole("dialog");
    await expect(
      editor.getByText(/Saving creates a new panel version/),
    ).toBeVisible();
    await editor.getByLabel("Tier").first().selectOption("2");
    await editor.getByRole("button", { name: "Publish new version" }).click();
    const confirmation = page.getByRole("dialog");
    await expect(
      confirmation.getByRole("heading", {
        name: "Confirm panel publication",
      }),
    ).toBeVisible();
    await confirmation
      .getByRole("button", { name: "Publish new version" })
      .click();

    await expect(
      page
        .getByRole("row")
        .filter({ hasText: "Gram negative AST panel (UAT)" })
        .filter({ hasText: `v${originalVersion + 1}` })
        .filter({ hasText: "Current" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const preservedRunsResponse = await page.request.get(
      `${API_BASE}/ast/runs?isolateId=${encodeURIComponent(reviewed.isolateId)}`,
    );
    const preservedRuns = (await preservedRunsResponse.json()) as Array<{
      id: string;
      panelId: string;
    }>;
    expect(
      preservedRuns.find((run) => run.id === reviewed.astRunId)?.panelId,
    ).toBe(seeded.astPanelId);
  });

  test("activates a loaded standard without changing reviewed AST history", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyReferenceAdmin(page);
    const reviewed = await seedReviewedMicrobiologyCase(page);

    await page.goto(
      `${ADMIN_BASE}/breakpoints/${seeded.loadedBreakpointStandardId}?${canonicalQuery({ method: "MIC" })}`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(
      page.getByRole("heading", { name: "CLSI SYNTH-UAT-LOADED" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.getByRole("table")).toContainText("UAT_SYNTHETIC");
    await expect(page.getByRole("table")).toContainText(
      "Reference antibiotic (UAT)",
    );
    await expectQuery(page, "method", "MIC");
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page.getByRole("table")).toContainText("UAT_SYNTHETIC", {
      timeout: LONG_TIMEOUT,
    });

    await page.getByRole("button", { name: "Activate standard" }).click();
    const activationDialog = page.getByRole("dialog");
    await activationDialog
      .getByLabel("Effective date")
      .fill(new Date().toISOString().slice(0, 10));
    await activationDialog
      .getByRole("button", { name: "Activate standard" })
      .click();
    await expect(page.getByText("Active", { exact: true })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    const runsResponse = await page.request.get(
      `${API_BASE}/ast/runs?isolateId=${encodeURIComponent(reviewed.isolateId)}`,
    );
    expect(runsResponse.ok()).toBeTruthy();
    const runs = (await runsResponse.json()) as Array<{
      id: string;
      breakpointStandardId: string;
    }>;
    expect(
      runs.find((run) => run.id === reviewed.astRunId)?.breakpointStandardId,
    ).toBe(seeded.activeBreakpointStandardId);
  });

  test("safely imports mixed CSV and protects local corrections", async ({
    page,
  }) => {
    await seedMicrobiologyReferenceAdmin(page);
    const importVersion = `SYNTH-UAT-${randomUUID().slice(0, 8).toUpperCase()}`;
    const csv = [
      "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,resistant_value,units",
      `CLSI,${importVersion},group:UAT_SYNTHETIC,REFUAT,MIC,,MIC,1,2,2,4,synthetic-mg/L`,
      `CLSI,${importVersion},Unknown organism,REFUAT,MIC,,MIC,1,2,2,4,synthetic-mg/L`,
      `CLSI,${importVersion},group:UAT_SYNTHETIC,REFUAT,MIC,,MIC,not-a-number,2,2,4,synthetic-mg/L`,
    ].join("\n");

    const openImport = async () => {
      await page.getByRole("button", { name: "Import CSV" }).click();
      await page.locator('input[type="file"]').setInputFiles({
        name: "synthetic-breakpoints.csv",
        mimeType: "text/csv",
        buffer: Buffer.from(csv),
      });
      await expect(page.getByText("1 valid")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByText("2 skipped")).toBeVisible();
      await expect(page.getByText(/Unknown organism/)).toBeVisible();
      await expect(page.getByText(/Invalid decimal/)).toBeVisible();
    };

    await page.goto(`${ADMIN_BASE}/breakpoints?${canonicalQuery()}`, {
      waitUntil: "domcontentloaded",
    });
    await openImport();
    const download = page.waitForEvent("download");
    await page.getByRole("button", { name: "Download rejected rows" }).click();
    await expect((await download).suggestedFilename()).toBe(
      "breakpoint-import-rejected.csv",
    );
    await page.getByRole("button", { name: "Apply valid rows" }).click();
    await expect(page.getByText(/1 (unchanged|valid)/)).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await page.getByRole("button", { name: "Cancel" }).click();
    await expect(page.getByRole("dialog")).toBeHidden();

    await openImport();
    await page.getByRole("button", { name: "Apply valid rows" }).click();
    await expect(page.getByText("1 unchanged")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    await test.step("Protect a locally corrected imported rule", async () => {
      await page.getByRole("button", { name: "Cancel" }).click();
      await expect(page.getByRole("dialog")).toBeHidden();
      await openRowAction(page, importVersion, "View rules");

      await openRowAction(
        page,
        "Reference antibiotic (UAT)",
        "Edit local correction",
      );
      const correctionDialog = page.getByRole("dialog");
      await correctionDialog.getByLabel("Susceptible").fill("1.5");
      await correctionDialog
        .getByLabel("Notes")
        .fill("Synthetic local UAT correction");
      await correctionDialog.getByRole("button", { name: "Save" }).click();

      const correctedRow = page
        .getByRole("row")
        .filter({ hasText: "Reference antibiotic (UAT)" });
      await expect(correctedRow).toContainText("Local correction", {
        timeout: LONG_TIMEOUT,
      });
      await expect(correctedRow).toContainText("1.5");

      await page.getByRole("button", { name: "Back to standards" }).click();
      await page.getByRole("button", { name: "Import CSV" }).click();
      await page.locator('input[type="file"]').setInputFiles({
        name: "synthetic-breakpoints.csv",
        mimeType: "text/csv",
        buffer: Buffer.from(csv),
      });
      await expect(page.getByText("1 valid")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("button", { name: "Apply valid rows" }).click();
      await expect(page.getByText("0 valid")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByText("3 skipped")).toBeVisible();
      await expect(page.getByText(/locally customized/)).toBeVisible();
    });
  });

  test("reaches each section through config-driven navigation and breadcrumbs", async ({
    page,
  }) => {
    await seedMicrobiologyReferenceAdmin(page);
    await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });
    await page
      .getByRole("button", { name: "Microbiology reference data", exact: true })
      .click();
    await page
      .getByRole("link", { name: "Breakpoint standards", exact: true })
      .click();
    await expect(page).toHaveURL(/\/MicrobiologyReference\/breakpoints/);
    await expect(
      page.getByRole("navigation", { name: "Breadcrumb" }),
    ).toContainText("Home");
    await expect(
      page.getByRole("navigation", { name: "Breadcrumb" }),
    ).toContainText("Admin Management");

    for (const [section, label] of [
      ["organisms", "Organisms"],
      ["antibiotics", "Antibiotics"],
      ["ast-panels", "AST panels"],
      ["culture-setups", "Culture methods"],
      ["breakpoints", "Breakpoint standards"],
    ]) {
      await page.getByRole("link", { name: label, exact: true }).click();
      await expect(page).toHaveURL(
        new RegExp(`/MicrobiologyReference/${section}`),
      );
    }
  });
});
