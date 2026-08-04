import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const EXPECTED_STEP_KEYS = [
  "AMR-1",
  "AMR-2",
  "AMR-3",
  "AMR-4",
  "AMR-5",
  "AMR-6",
  "AMR-7",
  "AMR-16",
  "AMR-20",
  "AMR-21",
  "AMR-22",
  "AMR-23",
  "AMR-24",
  "AMR-25",
  "AMR-26",
  "AMR-27",
  "AMR-28",
  "AMR-29",
  "AMR-30",
  "AMR-31",
];

type UatStep = { key: string; required: boolean };
type UatSection = {
  key: string;
  title: string;
  version: string;
  steps: UatStep[];
};
type UatChecklist = {
  title: string;
  sections: UatSection[];
  checklistRevision: string;
};

const EXPECTED_STORIES = [
  {
    key: "AMR-S01",
    title: "M1 - Find and route microbiology work",
    version: "1.0",
  },
  {
    key: "AMR-S02",
    title: "M1 - Work the seeded bacteriology case",
    version: "1.0",
  },
  {
    key: "AMR-S03",
    title: "M1 - AST, critical communication, and reporting",
    version: "1.0",
  },
  {
    key: "AMR-S04",
    title: "M1 - Shared-specimen reflection (optional)",
    version: "1.0",
  },
  {
    key: "AMR-S05",
    title: "M2 - Open a controlled correction",
    version: "1.0",
  },
  {
    key: "AMR-S06",
    title: "M2 - Preserve repeat and retest AST attempts",
    version: "1.0",
  },
  {
    key: "AMR-S07",
    title: "M2 - Release and verify corrected results",
    version: "1.0",
  },
  {
    key: "AMR-S08",
    title: "M2 - Review the workflow by keyboard",
    version: "1.0",
  },
  {
    key: "AMR-S09",
    title: "M2 - Trace bench consumable lots",
    version: "1.0",
  },
];

const EXPECTED_REQUIRED_STEP_KEYS = [
  "AMR-1",
  "AMR-2",
  "AMR-3",
  "AMR-4",
  "AMR-5",
  "AMR-6",
  "AMR-7",
  "AMR-16",
  "AMR-20",
  "AMR-22",
  "AMR-23",
  "AMR-24",
  "AMR-25",
  "AMR-26",
  "AMR-27",
  "AMR-28",
  "AMR-29",
  "AMR-30",
  "AMR-31",
];

const EXPECTED_CHECKLIST_REVISION =
  "00242ff2e232998d6bab03844de68975a8dd6ef4117fd81eb51af997db4d5afe";

test.describe("OGC-782 live AMR UAT", () => {
  test("binds the review overlay to the deployed feature and verifies stable navigation", async ({
    page,
    request,
  }, testInfo) => {
    testInfo.setTimeout(120_000);
    const expectedAppSha = process.env.EXPECTED_APP_SHA;
    if (!expectedAppSha) {
      throw new Error(
        "EXPECTED_APP_SHA is required so live UAT cannot pass against an unintended build.",
      );
    }
    const expectedScope = process.env.EXPECTED_APP_SCOPE || "app";
    const expectedSchemaAffecting =
      (process.env.EXPECTED_SCHEMA_AFFECTING || "true") === "true";

    await test.step("Verify deployment and live UAT contracts", async () => {
      const targetResponse = await request.get("/__review/target.json");
      expect(targetResponse.ok()).toBeTruthy();
      const target = await targetResponse.json();
      expect(target).toMatchObject({
        instance: "amr",
        state: "ready",
        appSha: expectedAppSha,
        appBranch: "feat/782-ogc-782-microbiology-m8-clinical-completeness",
        scope: expectedScope,
        schemaAffecting: expectedSchemaAffecting,
      });
      expect(target.verification.health).toBe("passed");
      expect(target.verification.smoke).toBe("passed");

      const checklistResponse = await request.get("/__review/uat-amr.json");
      expect(checklistResponse.ok()).toBeTruthy();
      const checklist = (await checklistResponse.json()) as UatChecklist;
      expect(checklist).toMatchObject({
        schemaVersion: 2,
        instance: "amr",
        jira: "OGC-782",
        title: "Microbiology M1 + M2 - review",
      });
      expect(
        checklist.sections.map(({ key, title, version }) => ({
          key,
          title,
          version,
        })),
      ).toEqual(EXPECTED_STORIES);
      const steps = checklist.sections.flatMap((section) => section.steps);
      expect(steps.map((step) => step.key)).toEqual(EXPECTED_STEP_KEYS);
      expect(new Set(steps.map((step) => step.key)).size).toBe(steps.length);
      expect(
        steps.filter((step) => step.required).map((step) => step.key),
      ).toEqual(EXPECTED_REQUIRED_STEP_KEYS);
      expect(
        steps.filter((step) => !step.required).map((step) => step.key),
      ).toEqual(["AMR-21"]);
      expect(checklist.checklistRevision).toBe(EXPECTED_CHECKLIST_REVISION);
    });

    await test.step("Verify the injected review overlay", async () => {
      await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
      const widget = page.locator("#oe-review-host");
      await widget.getByRole("button", { name: "Review" }).click();
      await expect(
        widget.getByText("Microbiology M1 + M2 - review", { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      for (const story of EXPECTED_STORIES) {
        await expect(
          widget.getByRole("heading", { name: story.title, exact: true }),
        ).toBeVisible();
      }
      await expect(
        widget.getByText(
          "From the final-released bacteriology case, open Amendments, enter a reason that describes the correction, and open the amendment.",
          { exact: true },
        ),
      ).toBeVisible();
      await expect(
        widget.getByText(
          "In Setup, inspect the culture-media lots before selecting one.",
          { exact: true },
        ),
      ).toBeVisible();
      await testInfo.attach("amr-review-overlay", {
        body: await page.screenshot(),
        contentType: "image/png",
      });
      await widget.getByRole("button", { name: "Minimize" }).click();
    });

    await test.step("Open the configured Microbiology sidenav route", async () => {
      await page.getByRole("button", { name: "Open menu" }).click();
      const microbiologyMenu = page.getByRole("button", {
        name: "Microbiology",
        exact: true,
      });
      await expect(microbiologyMenu).toBeVisible({ timeout: LONG_TIMEOUT });
      await microbiologyMenu.click();
      const worklistLink = page.getByRole("link", {
        name: "Microbiology worklist",
        exact: true,
      });
      await expect(worklistLink).toHaveAttribute(
        "href",
        "/Microbiology/worklist",
      );
      await worklistLink.click();
      await expect(page).toHaveURL(/\/Microbiology\/worklist$/);
      await expect(
        page.getByRole("heading", { name: "Microbiology worklist" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByTestId("content-wrapper")).toHaveClass(
        /content-nav-locked/,
      );
    });

    await test.step("Preserve worklist state in the canonical URL", async () => {
      const workflowFilter = page.getByRole("combobox", {
        name: "Workflow",
        exact: true,
      });
      const sort = page.getByRole("combobox", {
        name: "Sort",
        exact: true,
      });
      await workflowFilter.selectOption("BACTERIOLOGY");
      await sort.selectOption("newest");
      await expect(page).toHaveURL(
        /\/Microbiology\/worklist\?workflow=BACTERIOLOGY&sort=newest$/,
      );
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(workflowFilter).toHaveValue("BACTERIOLOGY");
      await expect(sort).toHaveValue("newest");
      await testInfo.attach("amr-filtered-worklist", {
        body: await page.screenshot(),
        contentType: "image/png",
      });
    });

    await test.step("Preserve worklist context through the case route", async () => {
      const openCaseButtons = page.getByRole("button", { name: "Open case" });
      const firstOpenCase = openCaseButtons.first();
      await expect(firstOpenCase).toBeVisible({ timeout: LONG_TIMEOUT });
      await firstOpenCase.click();
      await expect(page).toHaveURL(
        /\/Microbiology\/cases\/[^?]+\?workflow=BACTERIOLOGY&sort=newest$/,
      );
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByTestId("microbiology-progress-rail"),
      ).toBeVisible();

      await page.getByRole("button", { name: "Isolates", exact: true }).click();
      await expect(page).toHaveURL(/&section=isolates$/);
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("button", { name: "Isolates", exact: true }),
      ).toHaveAttribute("aria-expanded", "true");
      await testInfo.attach("amr-case-isolates-section", {
        body: await page.screenshot(),
        contentType: "image/png",
      });

      await page
        .getByRole("navigation", { name: "Breadcrumb" })
        .getByRole("link", {
          name: "Microbiology worklist",
          exact: true,
        })
        .click();
      await expect(page).toHaveURL(
        "/Microbiology/worklist?workflow=BACTERIOLOGY&sort=newest",
      );
      await page.getByRole("button", { name: "Clear filters" }).click();
      await expect(page).toHaveURL("/Microbiology/worklist");
    });
  });
});
