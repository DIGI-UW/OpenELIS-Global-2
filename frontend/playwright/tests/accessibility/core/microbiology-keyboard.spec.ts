import { test, expect } from "../../../helpers/test-base";
import type { Locator, Page, TestInfo } from "@playwright/test";
import {
  seedFinalizedMicrobiologyCase,
  seedMicrobiologyMvpCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

async function tabTo(page: Page, target: Locator, maxTabs = 120) {
  const candidate = target.first();
  await expect(candidate).toBeVisible({ timeout: LONG_TIMEOUT });
  for (let index = 0; index < maxTabs; index += 1) {
    await page.keyboard.press("Tab");
    const reached = await candidate.evaluate(
      (element) =>
        element === document.activeElement ||
        element.contains(document.activeElement),
    );
    if (reached) {
      await expect(candidate).toBeFocused();
      return;
    }
  }
  throw new Error(
    `Keyboard focus did not reach ${await candidate.evaluate((element) => element.outerHTML.slice(0, 300))}`,
  );
}

const attachScreenshot = async (
  page: Page,
  testInfo: TestInfo,
  name: string,
) => {
  await testInfo.attach(name, {
    body: await page.screenshot({ fullPage: true }),
    contentType: "image/png",
  });
};

test.describe("Microbiology keyboard-only workflow", () => {
  test("filters and opens work, records an isolate and AST, and releases an amendment", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const workingCase = await seedMicrobiologyMvpCase(page);

    await test.step("Filter the worklist and open the case with the keyboard", async () => {
      await page.goto("/Microbiology/worklist", {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Microbiology worklist" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const workflowFilter = page.getByLabel("Workflow", { exact: true });
      await tabTo(page, workflowFilter);
      await page.keyboard.press("b");
      await page.keyboard.press("Tab");
      await expect(page).toHaveURL(/workflow=BACTERIOLOGY/);

      const search = page.getByPlaceholder("Search sample or workflow");
      await tabTo(page, search);
      await page.keyboard.type(workingCase.sampleItemId);
      await expect(page).toHaveURL(
        new RegExp(`q=${encodeURIComponent(workingCase.sampleItemId)}`),
      );

      const caseLink = page.getByRole("link", {
        name: workingCase.sampleItemId,
        exact: true,
      });
      await tabTo(page, caseLink);
      await page.keyboard.press("Enter");
      await expect(page).toHaveURL(
        new RegExp(`/Microbiology/cases/${workingCase.caseId}`),
      );
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Create an isolate and record AST with the keyboard", async () => {
      const isolatesPanel = page
        .getByTestId("microbiology-case-view")
        .getByRole("button", {
          name: "Isolates",
          exact: true,
        });
      await tabTo(page, isolatesPanel);
      await page.keyboard.press("Enter");
      await expect(page).toHaveURL(/section=isolates/);

      const organism = page.getByLabel("Preliminary organism");
      await tabTo(page, organism);
      await page.keyboard.type("Escherichia coli");
      const createIsolate = page.getByRole("button", {
        name: "Create isolate",
      });
      await tabTo(page, createIsolate);
      await page.keyboard.press("Enter");
      await expect(page.getByText(/ISO-1: Escherichia coli/)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });

      const astPanel = page
        .getByTestId("microbiology-case-view")
        .getByRole("button", {
          name: "Manual AST",
          exact: true,
        });
      await tabTo(page, astPanel);
      await page.keyboard.press("Enter");
      await expect(page).toHaveURL(/section=ast/);

      const startRun = page.getByRole("button", { name: "Start AST run" });
      await expect(startRun).toBeEnabled({ timeout: LONG_TIMEOUT });
      await tabTo(page, startRun);
      await page.keyboard.press("Enter");
      await expect(
        page.getByTestId("microbiology-ast-run-status"),
      ).toContainText("In Progress", { timeout: LONG_TIMEOUT });

      const recordReading = page.getByRole("button", {
        name: "Record AST reading",
      });
      await expect(recordReading).toBeEnabled({ timeout: LONG_TIMEOUT });
      await tabTo(page, recordReading);
      await page.keyboard.press("Enter");
      await expect(
        page.getByTestId("microbiology-ast-interpretation"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const reviewRun = page.getByRole("button", { name: "Review AST run" });
      await tabTo(page, reviewRun);
      await page.keyboard.press("Enter");
      await expect(
        page.getByTestId("microbiology-ast-run-status"),
      ).toContainText("Reviewed", { timeout: LONG_TIMEOUT });
      await attachScreenshot(page, testInfo, "keyboard-ast-reviewed");
    });

    await test.step("Open and release an amendment with the keyboard", async () => {
      const finalCase = await seedFinalizedMicrobiologyCase(page);
      await page.goto(
        `/Microbiology/cases/${finalCase.caseId}?section=amendment`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(
        page.getByRole("heading", { name: "Amendments" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const reason = page.getByLabel("Amendment reason");
      await tabTo(page, reason);
      await page.keyboard.type("Keyboard qualification correction");
      const openAmendment = page.getByRole("button", {
        name: "Open amendment",
      });
      await tabTo(page, openAmendment);
      await page.keyboard.press("Enter");
      await expect(
        page.getByText("Amendment in progress", { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const releaseAmendment = page.getByRole("button", {
        name: "Release amended report",
      });
      await tabTo(page, releaseAmendment);
      await page.keyboard.press("Enter");
      await expect(
        page.getByText("Amended report released; the case is locked again"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await attachScreenshot(page, testInfo, "keyboard-amendment-released");
    });
  });
});
