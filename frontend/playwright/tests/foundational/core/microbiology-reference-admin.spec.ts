import type { Page } from "@playwright/test";
import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import {
  seedMicrobiologyReferenceAdmin,
  seedReviewedMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

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
  test(
    "lists deployment Patient Origins without Phase 1B mutation controls",
    { tag: ["@evidence", "@microbiology", "@admin"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Patient origins come from the deployment",
        "The origins a site uses are configured with the deployment, so the admin screen lists and searches them but does not edit them.",
      );
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
      await expect(page.getByRole("button", { name: "Options" })).toHaveCount(
        0,
      );
      await demo.caption(
        "All six origins are listed with their codes, with no add or edit actions.",
      );
      await demo.highlight(table);
      await demo.caption("A search narrows the list and survives a reload.");

      await page
        .getByPlaceholder("Search reference data")
        .fill("Long-term Care");
      await expectQuery(page, "q", "Long-term Care");
      await expect(table.getByRole("row")).toHaveCount(2);
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(table).toContainText("Long-term Care", {
        timeout: LONG_TIMEOUT,
      });
      await expect(table).not.toContainText("Inpatient");
      await demo.verified(
        "Patient origins are listed, searchable and read-only",
        "The search for Long-term Care survived a reload.",
      );
    },
  );

  test(
    "edits and safely deactivates service-created vocabulary",
    { tag: ["@evidence", "@microbiology", "@admin"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Editing and retiring reference vocabulary",
        "Organisms and antibiotics are edited in place, and deactivating one first shows what it would affect.",
      );
      await demo.caption(
        "Off screen: a synthetic organism and antibiotic are created.",
      );
      const seeded = await seedMicrobiologyReferenceAdmin(page);

      await test.step("Reload canonical organism search state", async () => {
        await demo.caption(
          "The organism list is searched, and the search survives a reload.",
        );
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
        await demo.caption(
          "The organism gets a note. Deactivating it first shows the impact.",
        );
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
        await demo.highlight(impactDialog);
        await impactDialog.getByRole("button", { name: /Deactivate$/ }).click();
        await expectActiveReference(
          page,
          "organisms",
          seeded.organismId,
          false,
        );

        await demo.caption(
          "Deactivated, it can no longer be chosen. Reactivating restores it.",
        );
        await openRowAction(page, "Reference organism (UAT)", "Reactivate");
        await expectActiveReference(page, "organisms", seeded.organismId, true);
      });

      await test.step("Repeat the guarded workflow for an antibiotic", async () => {
        await demo.caption(
          "The same edit, deactivation and reactivation apply to an antibiotic.",
        );
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
      await demo.verified(
        "Vocabulary is edited and retired safely",
        "Deactivation stops new selection but keeps history, and both items were restored.",
      );
    },
  );

  test(
    "publishes an immutable AST panel version",
    { tag: ["@evidence", "@microbiology", "@admin"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Publishing a new AST panel version",
        "Changing a panel publishes a new version, and runs already done on the old version keep it.",
      );
      await demo.caption(
        "Off screen: a reviewed AST run is recorded on the current panel version.",
      );
      await seedMicrobiologyReferenceAdmin(page);
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
      expect(originalRun?.panelId).toBe(reviewed.astPanelId);

      await page.goto(
        `${ADMIN_BASE}/ast-panels?${canonicalQuery({ q: "Gram negative AST panel (UAT)" })}`,
        { waitUntil: "domcontentloaded" },
      );
      const currentRow = page
        .getByRole("row")
        .filter({ hasText: "Gram negative AST panel (UAT)" })
        .filter({ hasText: "Current" });
      await expect(currentRow).toBeVisible({ timeout: LONG_TIMEOUT });
      const versionText = await currentRow
        .getByRole("cell")
        .filter({ hasText: /^v\d+$/ })
        .innerText();
      const originalVersion = Number(versionText.replace(/^v/, ""));
      await demo.highlight(currentRow);
      await demo.caption(
        "Publish new version opens the panel. Saving creates a version; it never edits the old one.",
      );
      await currentRow.getByRole("button", { name: "Options" }).click();
      await page.getByRole("menuitem", { name: "Publish new version" }).click();
      await expectQuery(page, "edit", reviewed.astPanelId);
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
      await demo.caption(
        "One drug moves to tier 2, and publication is confirmed.",
      );
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
      ).toBe(reviewed.astPanelId);
      await demo.verified(
        "A new panel version is current, and the old run keeps its version",
        `The panel is now v${originalVersion + 1}; the reviewed run still points at the version it used.`,
      );
    },
  );

  test(
    "activates a loaded standard without changing reviewed AST history",
    { tag: ["@evidence", "@microbiology", "@admin"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Activating a loaded breakpoint standard",
        "A loaded standard is reviewed rule by rule, then activated; AST already reviewed keeps the standard it used.",
      );
      await demo.caption(
        "Off screen: a standard is loaded but not active, and a reviewed AST run uses the active one.",
      );
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

      await demo.caption(
        "The loaded standard's rules are reviewed. Activating it asks for an effective date.",
      );
      await demo.highlight(page.getByRole("table"));
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
      await demo.highlight(page.getByText("Active", { exact: true }));

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
      await demo.verified(
        "The standard is active, and reviewed AST keeps its own",
        "The reviewed run still records the standard it was interpreted with.",
      );
    },
  );

  test(
    "safely imports mixed CSV and protects local corrections",
    { tag: ["@evidence", "@microbiology", "@admin"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Importing breakpoints safely",
        "A CSV import applies only valid rows, explains the rest, is safe to repeat, and never overwrites a local correction.",
      );
      await demo.caption(
        "Off screen: a reference organism and antibiotic exist. The CSV has one valid row and two bad ones.",
      );
      await seedMicrobiologyReferenceAdmin(page);
      const importVersion = `SYNTH-UAT-${randomUUID().slice(0, 8).toUpperCase()}`;
      const csv = [
        "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,resistant_value,units",
        `CLSI,${importVersion},group:UAT_SYNTHETIC,REFUAT,MIC,,MIC,1,2,2,4,mg/L`,
        `CLSI,${importVersion},Unknown organism,REFUAT,MIC,,MIC,1,2,2,4,mg/L`,
        `CLSI,${importVersion},group:UAT_SYNTHETIC,REFUAT,MIC,,MIC,not-a-number,2,2,4,mg/L`,
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
      await demo.caption(
        "The preview shows one valid row and two skipped, with their reasons. The rejected rows can be downloaded.",
      );
      await page
        .getByRole("button", { name: "Download rejected rows" })
        .click();
      await expect((await download).suggestedFilename()).toBe(
        "breakpoint-import-rejected.csv",
      );
      await demo.caption("Only the valid row is applied.");
      await page.getByRole("button", { name: "Apply valid rows" }).click();
      await expect(page.getByText(/1 (unchanged|valid)/)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("button", { name: "Cancel" }).click();
      await expect(page.getByRole("dialog")).toBeHidden();

      await demo.caption(
        "Importing the same file again changes nothing: the row is unchanged.",
      );
      await openImport();
      await page.getByRole("button", { name: "Apply valid rows" }).click();
      await expect(page.getByText("1 unchanged")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });

      await test.step("Protect a locally corrected imported rule", async () => {
        await demo.caption(
          "The imported rule gets a local correction: susceptible becomes 1.5.",
        );
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
        await demo.highlight(correctedRow);
        await demo.caption(
          "Importing the file once more skips the corrected rule instead of overwriting it.",
        );

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
      await demo.verified(
        "Imports apply only valid rows and keep local corrections",
        "Bad rows were explained, a repeat changed nothing, and the corrected rule was not overwritten.",
      );
    },
  );

  test(
    "reaches each section through config-driven navigation and breadcrumbs",
    { tag: ["@evidence", "@microbiology", "@admin"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Finding microbiology reference data",
        "Admin Management leads to each reference section, with breadcrumbs back.",
      );
      await demo.caption("Off screen: synthetic reference data is created.");
      await seedMicrobiologyReferenceAdmin(page);
      await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });
      await demo.caption(
        "Microbiology reference data opens from Admin Management, starting at breakpoint standards.",
      );
      await page
        .getByRole("button", {
          name: "Microbiology reference data",
          exact: true,
        })
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

      await demo.caption(
        "Organisms, antibiotics, AST panels and breakpoint standards are each one link away.",
      );
      for (const [section, label] of [
        ["organisms", "Organisms"],
        ["antibiotics", "Antibiotics"],
        ["ast-panels", "AST panels"],
        ["breakpoints", "Breakpoint standards"],
      ]) {
        await page.getByRole("link", { name: label, exact: true }).click();
        await expect(page).toHaveURL(
          new RegExp(`/MicrobiologyReference/${section}`),
        );
      }
      await demo.verified(
        "Every reference section is reachable",
        "Each section opened from the navigation, under breadcrumbs to Admin Management.",
      );
    },
  );
});
