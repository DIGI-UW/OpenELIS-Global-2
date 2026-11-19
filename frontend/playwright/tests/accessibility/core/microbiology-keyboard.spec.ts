import { test, expect } from "../../../helpers/test-base";
import type { Locator, Page, TestInfo } from "@playwright/test";
import {
  seedFinalizedMicrobiologyCase,
  seedMicrobiologyMvpCase,
  seedReviewedMicrobiologyCase,
  ensureWorkspaceGrowthCulture,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

async function tabTo(page: Page, target: Locator, maxTabs = 120) {
  await expect(target).toHaveCount(1);
  const candidate = target;
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
  test("filters and opens work, picks an isolate, and releases partial and amended reports", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const workingCase = await seedMicrobiologyMvpCase(page);
    await ensureWorkspaceGrowthCulture(page, workingCase);

    await test.step("Filter the worklist and open the case with the keyboard", async () => {
      await page.goto("/Microbiology/worklist?grain=cultures", {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Microbiology worklist" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const incubatingSummary = page.getByRole("link", {
        name: "Incubating",
        exact: true,
      });
      await tabTo(page, incubatingSummary);
      await page.keyboard.press("Enter");
      await expect(page).toHaveURL(/status=incubating/);

      const totalSummary = page.getByRole("link", {
        name: "Total pending",
        exact: true,
      });
      await tabTo(page, totalSummary);
      await page.keyboard.press("Enter");
      await expect(page).not.toHaveURL(/status=incubating/);

      const search = page.getByRole("searchbox", { name: "Filter table" });
      await tabTo(page, search);
      await page.keyboard.type(workingCase.accessionNumber);
      await expect(page).toHaveURL(
        new RegExp(`q=${encodeURIComponent(workingCase.accessionNumber)}`),
      );

      const caseLink = page.getByRole("link", {
        name: workingCase.accessionNumber,
        exact: true,
      });
      await tabTo(page, caseLink);
      await page.keyboard.press("Enter");
      await expect(page).toHaveURL(
        new RegExp(`/Microbiology/cases/${workingCase.caseId}`),
      );
      await expect(
        page.getByRole("heading", {
          level: 1,
          name: `Case ${workingCase.accessionNumber}`,
        }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Pick an isolate in a dialog that takes and returns focus", async () => {
      const section = page.getByLabel("Testing section", { exact: true });
      await tabTo(page, section);
      await page.keyboard.type("Isolates");
      await expect(section).toHaveValue("ISOLATES");
      const pick = page
        .getByRole("button", { name: "Pick isolate", exact: true })
        .filter({ visible: true })
        .first();
      await tabTo(page, pick);
      await page.keyboard.press("Enter");
      const dialog = page.getByRole("dialog");
      const source = dialog.getByLabel("Source culture row", { exact: true });
      await expect(source).toBeFocused();
      await page.keyboard.type("AST-FIXTURE-GROWTH");
      await expect(source.locator("option:checked")).toHaveText(
        /AST-FIXTURE-GROWTH/,
      );
      await tabTo(page, dialog.getByLabel("Gram stain", { exact: true }));
      await page.keyboard.type("Gram negative rods");
      await tabTo(
        page,
        dialog.getByLabel("Colony morphology", { exact: true }),
      );
      await page.keyboard.type("Lactose fermenting colonies");
      await tabTo(
        page,
        dialog.getByRole("button", { name: "Save", exact: true }),
      );
      await page.keyboard.press("Enter");
      await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByText("Identification pending", { exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(pick).toBeFocused();
      await attachScreenshot(page, testInfo, "keyboard-isolate-picked");
    });

    await test.step("Release a partial report with the keyboard", async () => {
      const reviewed = await seedReviewedMicrobiologyCase(page);
      await page.goto(
        `/Microbiology/cases/${reviewed.caseId}?section=reports`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      await expect(
        page.getByRole("heading", { name: "Report and release", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const releasePartial = page.getByRole("button", {
        name: "Release partial report",
      });
      await expect(releasePartial).toBeEnabled({ timeout: LONG_TIMEOUT });
      await tabTo(page, releasePartial);
      await page.keyboard.press("Enter");
      await expect(
        page.getByLabel("Report version").locator("option:checked"),
      ).toContainText("Partial", { timeout: LONG_TIMEOUT });
      await expect(releasePartial).toBeFocused();
      await attachScreenshot(page, testInfo, "keyboard-partial-release");
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
        exact: true,
      });
      await tabTo(page, openAmendment);
      await page.keyboard.press("Enter");
      await expect(
        page.getByText(
          "Amendment opened; clinical corrections are now enabled",
        ),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const releaseAmendment = page.getByRole("button", {
        name: "Release amended report",
        exact: true,
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
