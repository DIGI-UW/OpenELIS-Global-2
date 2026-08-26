import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { Sidenav } from "../../../fixtures/sidenav";
import { seedMicrobiologyWorklistCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const accordionButton = (page: Page, name: string) =>
  page.locator(".cds--accordion__heading").filter({ hasText: name });

test.describe("microbiology worklist and critical communication", () => {
  test("worklist contains its wide table on a mobile viewport", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/Microbiology/worklist", {
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

  test("critical communication raises worklist priority and sibling visibility", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = seedMicrobiologyWorklistCase();
    const scopedWorklistUrl = `/Microbiology/worklist?workflow=BACTERIOLOGY&q=${encodeURIComponent(
      seeded.caseId,
    )}&sort=newest`;
    const scopedCaseUrl = `/Microbiology/cases/${seeded.caseId}?workflow=BACTERIOLOGY&q=${encodeURIComponent(
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
    await sidenav.clickMenu("Microbiology worklist");
    await expect(page).toHaveURL(/\/Microbiology\/worklist$/);
    await expect(
      page.getByRole("heading", { name: "Microbiology worklist" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      page.getByTestId("microbiology-worklist-summary-total"),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      page.getByTestId("microbiology-worklist-summary-critical"),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.getByTestId("content-wrapper")).toHaveClass(
      /content-nav-locked/,
    );
    await page
      .locator("#microbiology-worklist-workflow-filter")
      .selectOption("BACTERIOLOGY");
    await expect(page).toHaveURL(
      /\/Microbiology\/worklist\?workflow=BACTERIOLOGY$/,
    );
    await page.locator("#microbiology-worklist-sort").selectOption("newest");
    await expect(page).toHaveURL(
      /\/Microbiology\/worklist\?workflow=BACTERIOLOGY&sort=newest$/,
    );

    await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

    await accordionButton(page, "Critical communication").click();
    await expect(page).toHaveURL(
      `/Microbiology/cases/${seeded.caseId}?section=critical-communication`,
    );
    await page
      .getByLabel("Recipient", { exact: true })
      .fill("Provider on call");
    await page
      .getByLabel("Message")
      .fill("Positive blood culture called to provider");
    await page.getByRole("button", { name: "Log communication" }).click();
    await expect(
      page.getByTestId("microbiology-critical-status"),
    ).toContainText("Open", { timeout: LONG_TIMEOUT });

    await page.goto(scopedWorklistUrl, { waitUntil: "domcontentloaded" });
    const row = page.getByTestId(`microbiology-worklist-row-${seeded.caseId}`);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText("High");
    await expect(row).toContainText("Critical communication");
    await expect(row).toContainText("Mycobacteriology Tb");
    await expect(
      page.getByTestId("microbiology-worklist-summary-critical"),
    ).toContainText("1");

    await row.getByRole("button", { name: "Open case" }).click();
    await expect(page).toHaveURL(scopedCaseUrl);
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await accordionButton(page, "Critical communication").click();
    await expect(page).toHaveURL(
      `${scopedCaseUrl}&section=critical-communication`,
    );
    await page.getByRole("button", { name: "Acknowledge" }).click();
    await expect(
      page.getByTestId("microbiology-critical-status"),
    ).toContainText("Acknowledged", { timeout: LONG_TIMEOUT });
    await accordionButton(page, "Isolates").click();
    await expect(page).toHaveURL(`${scopedCaseUrl}&section=isolates`);
    await page
      .getByRole("navigation", { name: "Breadcrumb" })
      .getByRole("link", { name: "Microbiology worklist" })
      .click();
    await expect(page).toHaveURL(scopedWorklistUrl);
  });
});
