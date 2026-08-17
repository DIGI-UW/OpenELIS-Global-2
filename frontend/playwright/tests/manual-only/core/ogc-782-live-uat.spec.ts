import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

type UatStep = { key: string; required: boolean };
type UatSection = {
  key: string;
  title: string;
  steps: UatStep[];
};
type UatChecklist = {
  schemaVersion: number;
  instance: string;
  jira: string;
  sections: UatSection[];
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

type UatStory = UatStoryIndex["stories"][number];

test.describe("OGC-782 deployed Review integration", () => {
  test("binds the review overlay to the deployed feature and verifies stable navigation", async ({
    page,
    request,
  }, testInfo) => {
    testInfo.setTimeout(120_000);
    const expectedAppSha = process.env.EXPECTED_APP_SHA;
    const expectedAppBranch = process.env.EXPECTED_APP_BRANCH;
    const expectedHarnessSha = process.env.EXPECTED_HARNESS_SHA;
    const expectedAccession = process.env.EXPECTED_ACCESSION;
    if (
      !expectedAppSha ||
      !expectedAppBranch ||
      !expectedHarnessSha ||
      !expectedAccession
    ) {
      throw new Error(
        "EXPECTED_APP_SHA, EXPECTED_APP_BRANCH, EXPECTED_HARNESS_SHA, and EXPECTED_ACCESSION are required so deployed verification cannot pass against unintended runtime or fixture data.",
      );
    }
    const expectedScope = process.env.EXPECTED_APP_SCOPE || "app";
    const expectedSchemaAffecting =
      (process.env.EXPECTED_SCHEMA_AFFECTING || "true") === "true";
    let selectedStory: UatStory | undefined;
    let selectedStoryStepCount = 0;

    await test.step("Verify deployment and Review integration contracts", async () => {
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
      expect(amrStories.length).toBeGreaterThan(0);

      const checklistResponse = await request.get("/__review/uat-amr.json");
      expect(checklistResponse.ok()).toBeTruthy();
      const checklist = (await checklistResponse.json()) as UatChecklist;
      expect(checklist).toMatchObject({
        schemaVersion: 2,
        instance: "amr",
        jira: "OGC-782",
      });
      selectedStory = amrStories.find((story) =>
        checklist.sections.some((section) => section.key === story.key),
      );
      expect(selectedStory).toBeDefined();
      const selectedSection = checklist.sections.find(
        (section) => section.key === selectedStory?.key,
      );
      expect(selectedSection).toBeDefined();
      expect(selectedSection?.steps.length).toBeGreaterThan(0);
      selectedStoryStepCount = selectedSection?.steps.length || 0;
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
      await expect(storyList.getByRole("option").first()).toBeVisible();
      await expect(
        storyList.getByRole("option", { name: /OGC-788/ }),
      ).toHaveCount(0);
      if (!selectedStory) {
        throw new Error("The live AMR checklist has no selectable story.");
      }
      await storyList
        .getByRole("option")
        .filter({ hasText: selectedStory.title })
        .click();
      await expect(
        widget.getByRole("heading", {
          name: `${selectedStory.title} - review`,
          exact: true,
        }),
      ).toBeVisible();
      await expect(widget.locator(".step")).toHaveCount(selectedStoryStepCount);

      const refreshedChecklist = page.waitForResponse(
        (response) =>
          response.url().endsWith("/__review/uat-amr.json") && response.ok(),
      );
      await widget.getByRole("button", { name: "Refresh checklist" }).click();
      await refreshedChecklist;
      await expect(widget.locator(".step")).toHaveCount(selectedStoryStepCount);
      await page.reload({ waitUntil: "domcontentloaded" });
      const reloadedWidget = page.locator("#oe-review-host");
      await expect(
        reloadedWidget.getByRole("heading", {
          name: `${selectedStory.title} - review`,
          exact: true,
        }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(reloadedWidget.locator(".panel")).toBeVisible();
      await expect(reloadedWidget.locator(".step")).toHaveCount(
        selectedStoryStepCount,
      );
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
