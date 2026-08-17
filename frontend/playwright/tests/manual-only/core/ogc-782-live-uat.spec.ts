import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

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

type UatStoryIndex = {
  schemaVersion: number;
  stories: Array<{
    id: string;
    review: string;
    key: string;
    title: string;
    hosts: string[];
    steps: number;
    required: number;
  }>;
};

const EXPECTED_AMR_STORIES = [
  ["AMR-S01", "R2 - M-03 - Route and contextualize a culture order"],
  ["AMR-S17", "R1 - M-07 - Work the Culture queue"],
  ["AMR-S18", "R1 - M-04 - Classify and navigate sibling cases"],
  ["AMR-S02", "R2 - M-04 - Record culture progression"],
  ["AMR-S28", "R2 - M-04 - Set or change the bench protocol"],
  ["AMR-S19", "R1 - M-04 - Identify isolates and manage exceptions"],
  ["AMR-S20", "R1 - M-05 - Review manual AST"],
  ["AMR-S21", "R1 - M-05 - Review analyzer AST and QC"],
  ["AMR-S22", "R1 - M-07 - Work the AST queue"],
  ["AMR-S03", "R1 - M-11 - Communicate and release results"],
  ["AMR-S04", "M1 - Shared-specimen reflection (optional)"],
  ["AMR-S05", "M2 - Open a controlled correction"],
  ["AMR-S06", "M2 - Preserve repeat and retest AST attempts"],
  ["AMR-S07", "M2 - Release and verify corrected results"],
  ["AMR-S08", "M2 - Review the workflow by keyboard"],
  ["AMR-S09", "M2/R1 - Trace bench consumable lots"],
  ["AMR-S10", "M3 - Maintain organism and antibiotic vocabularies"],
  ["AMR-S11", "M3 - Publish immutable AST panel versions"],
  ["AMR-S12", "M3 - Control breakpoint catalog lifecycle"],
  ["AMR-S13", "M3 - Import breakpoint updates safely"],
  ["AMR-S14", "M4 - Preview and export WHONET CSV"],
];

const EXPECTED_ALIGNMENT_STEPS = [
  ["AMR-S01", ["AMR-2", "AMR-79", "AMR-63", "AMR-64", "AMR-83"]],
  ["AMR-S17", ["AMR-1", "AMR-3", "AMR-75"]],
  ["AMR-S18", ["AMR-65", "AMR-66"]],
  ["AMR-S02", ["AMR-4", "AMR-67", "AMR-84", "AMR-68"]],
  ["AMR-S28", ["AMR-81", "AMR-82"]],
  ["AMR-S19", ["AMR-5", "AMR-69", "AMR-70"]],
  ["AMR-S20", ["AMR-6", "AMR-71", "AMR-72"]],
  ["AMR-S21", ["AMR-73", "AMR-74"]],
  ["AMR-S22", ["AMR-76", "AMR-77", "AMR-78"]],
  ["AMR-S03", ["AMR-7", "AMR-16", "AMR-20"]],
];

test.describe("OGC-782 live AMR UAT", () => {
  test("binds the review overlay to the deployed feature and verifies stable navigation", async ({
    page,
    request,
  }, testInfo) => {
    testInfo.setTimeout(120_000);
    const expectedAppSha = process.env.EXPECTED_APP_SHA;
    const expectedAppBranch = process.env.EXPECTED_APP_BRANCH;
    const expectedHarnessSha = process.env.EXPECTED_HARNESS_SHA;
    const expectedChecklistRevision = process.env.EXPECTED_CHECKLIST_REVISION;
    const expectedAccession = process.env.EXPECTED_ACCESSION;
    if (
      !expectedAppSha ||
      !expectedAppBranch ||
      !expectedHarnessSha ||
      !expectedChecklistRevision ||
      !expectedAccession
    ) {
      throw new Error(
        "EXPECTED_APP_SHA, EXPECTED_APP_BRANCH, EXPECTED_HARNESS_SHA, EXPECTED_CHECKLIST_REVISION, and EXPECTED_ACCESSION are required so live UAT cannot pass against unintended revisions or fixture data.",
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
        appBranch: expectedAppBranch,
        harnessSha: expectedHarnessSha,
        scope: expectedScope,
        schemaAffecting: expectedSchemaAffecting,
      });
      expect(target.verification.health).toBe("passed");
      expect(target.verification.smoke).toBe("passed");

      const indexResponse = await request.get("/__review/uat-index.json");
      expect(indexResponse.ok()).toBeTruthy();
      const index = (await indexResponse.json()) as UatStoryIndex;
      expect(index.schemaVersion).toBe(2);
      const amrStories = index.stories.filter(
        (story) =>
          story.review === "amr" &&
          story.hosts.includes("amr.openelis-global.org"),
      );
      expect(amrStories.map(({ key, title }) => [key, title])).toEqual(
        EXPECTED_AMR_STORIES,
      );
      expect(amrStories.map((story) => story.key)).not.toContain("AMR-S15");
      expect(amrStories.map((story) => story.key)).not.toContain("AMR-S16");

      const checklistResponse = await request.get("/__review/uat-amr.json");
      expect(checklistResponse.ok()).toBeTruthy();
      const checklist = (await checklistResponse.json()) as UatChecklist;
      expect(checklist).toMatchObject({
        schemaVersion: 2,
        instance: "amr",
        jira: "OGC-782",
        title: "Microbiology M1-M4 + R1/R2 authoritative alignment review",
      });
      expect(
        checklist.sections
          .filter((section) =>
            [
              "AMR-S01",
              "AMR-S17",
              "AMR-S18",
              "AMR-S02",
              "AMR-S28",
              "AMR-S19",
              "AMR-S20",
              "AMR-S21",
              "AMR-S22",
              "AMR-S03",
            ].includes(section.key),
          )
          .map((section) => [
            section.key,
            section.steps.map((step) => step.key),
          ]),
      ).toEqual(EXPECTED_ALIGNMENT_STEPS);
      const steps = checklist.sections.flatMap((section) => section.steps);
      expect(checklist.sections).toHaveLength(23);
      expect(steps).toHaveLength(72);
      expect(new Set(steps.map((step) => step.key)).size).toBe(steps.length);
      expect(
        steps.filter((step) => !step.required).map((step) => step.key),
      ).toEqual(["AMR-73", "AMR-74", "AMR-21"]);
      expect(checklist.checklistRevision).toBe(expectedChecklistRevision);
    });

    await test.step("Verify the injected review overlay", async () => {
      await page.goto("/", { waitUntil: "domcontentloaded" });
      const widget = page.locator("#oe-review-host");
      await widget.getByRole("button", { name: "Review" }).click();
      await expect(widget.locator(".panel")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(widget.getByText("Reviewing as Open ELIS")).toBeVisible();

      await widget.getByRole("button", { name: "Choose story" }).click();
      const storyList = widget.getByRole("listbox", { name: /stories/i });
      await expect(storyList).toBeVisible();
      await expect(storyList.getByRole("option")).toHaveCount(21);
      await expect(
        storyList.getByRole("option", { name: /OGC-788/ }),
      ).toHaveCount(0);
      await storyList
        .getByRole("option", { name: /R1 - M-05 - Review manual AST/ })
        .click();
      await expect(
        widget.getByRole("heading", {
          name: "R1 - M-05 - Review manual AST - review",
          exact: true,
        }),
      ).toBeVisible();
      await expect(widget.locator(".step")).toHaveCount(3);
      await expect(widget.locator(".storydescription")).toContainText(
        "record and interpret readings",
      );

      const refreshedChecklist = page.waitForResponse(
        (response) =>
          response.url().endsWith("/__review/uat-amr.json") && response.ok(),
      );
      await widget.getByRole("button", { name: "Refresh checklist" }).click();
      await refreshedChecklist;
      await expect(widget.locator(".step")).toHaveCount(3);
      await page.reload({ waitUntil: "domcontentloaded" });
      const reloadedWidget = page.locator("#oe-review-host");
      await expect(
        reloadedWidget.getByRole("heading", {
          name: "R1 - M-05 - Review manual AST - review",
          exact: true,
        }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(reloadedWidget.locator(".panel")).toBeVisible();
      await expect(reloadedWidget.locator(".step")).toHaveCount(3);
      await testInfo.attach("amr-review-overlay", {
        body: await page.screenshot(),
        contentType: "image/png",
      });
      await reloadedWidget.getByRole("button", { name: "Minimize" }).click();
    });

    await test.step("Preserve worklist state in the canonical URL", async () => {
      await page.goto("/Microbiology/worklist", {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Microbiology worklist" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const workflowFilter = page.getByRole("combobox", {
        name: "Workflow",
        exact: true,
      });
      const sort = page.getByRole("combobox", {
        name: "Sort",
        exact: true,
      });
      const workflowRows = page.waitForResponse(
        (response) =>
          response
            .url()
            .includes(
              "/rest/microbiology/worklist?grain=cultures&workflow=BACTERIOLOGY",
            ) && response.ok(),
      );
      await workflowFilter.selectOption("BACTERIOLOGY");
      await workflowRows;
      const sortedRows = page.waitForResponse(
        (response) =>
          response.url().includes("workflow=BACTERIOLOGY&sort=newest") &&
          response.ok(),
      );
      await sort.selectOption("newest");
      await sortedRows;
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
      const caseLink = page.getByRole("link", {
        name: expectedAccession,
        exact: true,
      });
      await expect(caseLink).toBeVisible({ timeout: LONG_TIMEOUT });
      await caseLink.click();
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
      const unfilteredRows = page.waitForResponse(
        (response) =>
          response
            .url()
            .includes("/rest/microbiology/worklist?grain=cultures") &&
          !response.url().includes("workflow=BACTERIOLOGY") &&
          response.ok(),
      );
      await page.getByRole("button", { name: "Clear filters" }).click();
      await unfilteredRows;
      await expect(page).toHaveURL("/Microbiology/worklist");
    });
  });
});
