import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const EXPECTED_STEP_KEYS = [
  "AMR-1",
  "AMR-2",
  "AMR-3",
  "AMR-4",
  "AMR-5",
  "AMR-16",
  "AMR-6",
  "AMR-7",
  "AMR-20",
  "AMR-21",
];

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

    await test.step("Verify deployment and live UAT contracts", async () => {
      const targetResponse = await request.get("/__review/target.json");
      expect(targetResponse.ok()).toBeTruthy();
      const target = await targetResponse.json();
      expect(target).toMatchObject({
        instance: "amr",
        state: "ready",
        appSha: expectedAppSha,
      });
      expect(target.verification.health).toBe("passed");

      const checklistResponse = await request.get("/__review/uat-amr.json");
      expect(checklistResponse.ok()).toBeTruthy();
      const checklist = await checklistResponse.json();
      expect(checklist).toMatchObject({
        schemaVersion: 2,
        instance: "amr",
        jira: "OGC-782",
      });
      const steps = checklist.sections.flatMap((section) => section.steps);
      expect(steps.map((step) => step.key)).toEqual(EXPECTED_STEP_KEYS);
      expect(new Set(steps.map((step) => step.key)).size).toBe(steps.length);
      expect(checklist.checklistRevision).toMatch(/^[0-9a-f]{64}$/);
    });

    await test.step("Verify the injected review overlay", async () => {
      await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
      const widget = page.locator("#oe-review-host");
      await widget.getByRole("button", { name: "Review" }).click();
      await expect(
        widget.getByText("Microbiology MVP - review", { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(widget.locator(".step")).toHaveCount(
        EXPECTED_STEP_KEYS.length,
      );
      await expect(
        widget.getByText(
          "Follow the interpreted AST result through review and release, then inspect the patient-facing report.",
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
      const microbiologyMenu = page.locator("#menu_microbiology");
      await expect(microbiologyMenu).toBeVisible({ timeout: LONG_TIMEOUT });
      await microbiologyMenu
        .getByRole("button", { name: "Microbiology" })
        .click();
      const worklistLink = page.locator("#menu_microbiology_worklist_nav");
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
      await page
        .locator("#microbiology-worklist-workflow-filter")
        .selectOption("BACTERIOLOGY");
      await page.locator("#microbiology-worklist-sort").selectOption("newest");
      await expect(page).toHaveURL(
        /\/Microbiology\/worklist\?workflow=BACTERIOLOGY&sort=newest$/,
      );
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(
        page.locator("#microbiology-worklist-workflow-filter"),
      ).toHaveValue("BACTERIOLOGY");
      await expect(page.locator("#microbiology-worklist-sort")).toHaveValue(
        "newest",
      );
      await testInfo.attach("amr-filtered-worklist", {
        body: await page.screenshot(),
        contentType: "image/png",
      });
    });

    await test.step("Preserve worklist context through the case route", async () => {
      const openCaseButtons = page.getByRole("button", { name: "Open case" });
      await expect(openCaseButtons.first()).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await openCaseButtons.first().click();
      await expect(page).toHaveURL(
        /\/Microbiology\/cases\/[^?]+\?workflow=BACTERIOLOGY&sort=newest$/,
      );
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByTestId("microbiology-progress-rail"),
      ).toBeVisible();

      await page.getByRole("button", { name: "Isolates" }).click();
      await expect(page).toHaveURL(/&section=isolates$/);
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("button", { name: "Isolates" }),
      ).toHaveAttribute("aria-current", "location");
      await testInfo.attach("amr-case-isolates-section", {
        body: await page.screenshot(),
        contentType: "image/png",
      });

      await page
        .getByRole("button", { name: "Back to microbiology worklist" })
        .click();
      await expect(page).toHaveURL(
        "/Microbiology/worklist?workflow=BACTERIOLOGY&sort=newest",
      );
      await page.getByRole("button", { name: "Clear filters" }).click();
      await expect(page).toHaveURL("/Microbiology/worklist");
    });
  });
});
