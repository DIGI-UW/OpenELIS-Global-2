import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { Sidenav } from "../../../fixtures/sidenav";
import { seedMicrobiologyWorklistCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

const openCaseSection = async (page: Page, name: string, section: string) => {
  const scopes: Record<string, string> = {
    "critical-communication": "CRITICAL",
    reports: "REPORT",
    timeline: "HISTORY",
    amendment: "AMENDMENT",
    isolates: "ISOLATES",
  };
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption(scopes[section]);
  await expect
    .poll(() => new URL(page.url()).searchParams.get("scope"))
    .toBe(scopes[section]);
};

const waitForWorklistResponse = (
  page: Page,
  expectedParams: Record<string, string>,
) =>
  page.waitForResponse(
    (response) => {
      const url = new URL(response.url());
      return (
        response.ok() &&
        url.pathname.endsWith("/rest/microbiology/worklist") &&
        Object.entries(expectedParams).every(
          ([name, value]) => url.searchParams.get(name) === value,
        )
      );
    },
    { timeout: LONG_TIMEOUT },
  );

test.describe("microbiology worklist and critical communication", () => {
  test("worklist contains its wide table on a mobile viewport", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/Microbiology/worklist?grain=cultures", {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Microbiology worklist" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const closeMenuButton = page.getByRole("button", { name: "Close menu" });
    if (await closeMenuButton.isVisible()) {
      await closeMenuButton.click();
    }

    const tableScroll = page.locator(".microbiology-worklist__table-scroll");
    await expect(tableScroll).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect
      .poll(
        () =>
          tableScroll.evaluate(
            (element) => element.scrollWidth > element.clientWidth,
          ),
        { timeout: LONG_TIMEOUT },
      )
      .toBeTruthy();
    await expect
      .poll(
        () =>
          page.evaluate(
            () => document.documentElement.scrollWidth <= window.innerWidth,
          ),
        { timeout: LONG_TIMEOUT },
      )
      .toBeTruthy();
  });

  test(
    "canonical worklist state survives reload and the focused row opens the exact case",
    { tag: ["@evidence", "@microbiology", "@worklist"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "A worklist that keeps its place",
        "Search, sort and page size live in the address, survive a reload, and a row opened from the keyboard goes to exactly that case.",
      );
      await demo.caption("Off screen: a culture case is ordered.");
      const seeded = await seedMicrobiologyWorklistCase(page);
      const query = new URLSearchParams({
        grain: "cultures",
        q: seeded.caseId,
        sort: "newest",
        pageSize: "10",
      });
      const worklistUrl = `/Microbiology/worklist?${query}`;
      const caseUrl = `/Microbiology/cases/${seeded.caseId}?${query}`;

      const initialResponse = waitForWorklistResponse(page, {
        q: seeded.caseId,
        sort: "newest",
        pageSize: "10",
      });
      await page.goto(worklistUrl, { waitUntil: "domcontentloaded" });
      await initialResponse;

      const row = page.getByTestId(
        `microbiology-worklist-row-${seeded.caseId}`,
      );
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByPlaceholder("Search lab number, patient, or specimen"),
      ).toHaveValue(seeded.caseId);
      await expect(page.getByLabel("Sort", { exact: true })).toHaveValue(
        "newest",
      );
      await demo.caption(
        "The worklist is searched for the case and sorted newest first.",
      );
      await demo.highlight(row);
      await expect(page).toHaveURL(worklistUrl);

      await demo.caption(
        "After a reload, the same search, sort and row are back.",
      );
      const reloadResponse = waitForWorklistResponse(page, {
        q: seeded.caseId,
        sort: "newest",
        pageSize: "10",
      });
      await page.reload({ waitUntil: "domcontentloaded" });
      await reloadResponse;
      await expect(page).toHaveURL(worklistUrl);
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });

      await expect(row).toHaveAttribute("tabindex", "0");
      await demo.caption(
        "Pressing Enter on the focused row opens that case, keeping the worklist state for the way back.",
      );
      await row.focus();
      await expect(row).toBeFocused();
      await page.keyboard.press("Enter");
      await expect(page).toHaveURL(caseUrl);
      await expect(
        page.getByRole("heading", { name: `Case ${seeded.accessionNumber}` }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await demo.verified(
        "The worklist kept its state and opened the exact case",
        `Search, sort and page size survived the reload, and Enter opened ${seeded.accessionNumber}.`,
      );
    },
  );

  test(
    "critical communication raises worklist priority",
    { tag: ["@evidence", "@microbiology", "@worklist"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "A critical result raises the case on the worklist",
        "Logging a critical communication makes the case high priority on the worklist until it is acknowledged.",
      );
      await demo.caption("Off screen: a culture case is ordered.");
      test.setTimeout(120_000);
      const seeded = await seedMicrobiologyWorklistCase(page);
      const scopedWorklistUrl = `/Microbiology/worklist?grain=cultures&q=${encodeURIComponent(
        seeded.caseId,
      )}&sort=newest`;
      const scopedCaseUrl = `/Microbiology/cases/${seeded.caseId}?grain=cultures&q=${encodeURIComponent(
        seeded.caseId,
      )}&sort=newest`;

      await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
      const sidenav = new Sidenav(page);
      await sidenav.ensureExpanded();
      await sidenav.expandMenu("Microbiology");

      const worklistLink = sidenav.nav.getByRole("link", {
        name: "Microbiology worklist",
        exact: true,
      });
      await expect(worklistLink).toHaveAttribute(
        "href",
        "/Microbiology/worklist",
      );
      await demo.caption(
        "The microbiology worklist opens from the main menu, with its summary of critical work.",
      );
      await sidenav.clickMenu("Microbiology worklist");
      await expect(page).toHaveURL(/\/Microbiology\/worklist\?grain=cultures$/);
      await expect(
        page.getByRole("heading", { name: "Microbiology worklist" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await page.goto("/Microbiology/worklist?grain=cultures", {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByTestId("microbiology-worklist-summary-total"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByTestId("microbiology-worklist-summary-critical"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByTestId("content-wrapper")).toHaveClass(
        /content-nav-locked/,
      );
      const sortResponse = waitForWorklistResponse(page, {
        sort: "newest",
      });
      await page.getByLabel("Sort", { exact: true }).selectOption("newest");
      await expect(page).toHaveURL(
        /\/Microbiology\/worklist\?grain=cultures&sort=newest$/,
      );
      await sortResponse;
      await demo.highlight(
        page.getByTestId("microbiology-worklist-summary-critical"),
      );

      await demo.caption(
        "On the case, a critical communication is logged: who was called and what was said.",
      );
      await page.goto(`/Microbiology/cases/${seeded.caseId}?view=workbench`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: `Case ${seeded.accessionNumber}` }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      await openCaseSection(
        page,
        "Critical communication",
        "critical-communication",
      );
      const criticalCommunication = page.getByRole("region", {
        name: "Critical communication",
      });
      await expect(criticalCommunication).toBeVisible();
      await criticalCommunication
        .getByLabel("Recipient", { exact: true })
        .fill("Provider on call");
      await criticalCommunication
        .getByLabel("Message")
        .fill("Positive blood culture called to provider");
      await criticalCommunication
        .getByRole("button", { name: "Log communication" })
        .click();
      await expect(
        page.getByTestId("microbiology-critical-status"),
      ).toContainText("Open", { timeout: LONG_TIMEOUT });

      await demo.highlight(page.getByTestId("microbiology-critical-status"));
      await page.goto(scopedWorklistUrl, { waitUntil: "domcontentloaded" });
      const row = page.getByTestId(
        `microbiology-worklist-row-${seeded.caseId}`,
      );
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(row).toContainText("High");
      await expect(row).toContainText("Critical communication");
      await expect(
        page.getByTestId("microbiology-worklist-summary-critical"),
      ).toContainText("1");
      await demo.caption(
        "Back on the worklist, the case is high priority, flagged for critical communication.",
      );
      await demo.highlight(row);

      await row.getByRole("button", { name: "Row actions" }).click();
      await page.getByRole("menuitem", { name: "Open case" }).click();
      await expect(page).toHaveURL(scopedCaseUrl);
      await expect(
        page.getByRole("heading", { name: `Case ${seeded.accessionNumber}` }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await openCaseSection(
        page,
        "Critical communication",
        "critical-communication",
      );
      await demo.caption(
        "Opened from the row, the case shows the communication. It is acknowledged.",
      );
      await page.getByRole("button", { name: "Acknowledge" }).click();
      await expect(
        page.getByTestId("microbiology-critical-status"),
      ).toContainText("Acknowledged", { timeout: LONG_TIMEOUT });
      await openCaseSection(page, "Isolates", "isolates");
      await expect(
        page.getByRole("heading", { name: "Isolates", exact: true }),
      ).toBeVisible();
      const restoredWorklistResponse = waitForWorklistResponse(page, {
        q: seeded.caseId,
        sort: "newest",
      });
      await page
        .getByTestId("microbiology-case-view")
        .getByRole("link", { name: "Microbiology worklist", exact: true })
        .click();
      await expect(page).toHaveURL(scopedWorklistUrl);
      await restoredWorklistResponse;
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await demo.verified(
        "The critical communication is logged, raised and acknowledged",
        "The case was high priority on the worklist until the call was acknowledged, and the worklist kept its search.",
      );
    },
  );
});
