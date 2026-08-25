import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { Sidenav } from "../../../fixtures/sidenav";
import { seedMicrobiologyWorklistCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const accordionButton = (page: Page, name: string) =>
  page
    .getByTestId("microbiology-case-view")
    .getByRole("button", { name, exact: true });

const openCaseSection = async (page: Page, name: string, section: string) => {
  const sectionButton = accordionButton(page, name);
  await expect(sectionButton).toBeVisible({ timeout: LONG_TIMEOUT });
  await sectionButton.click();
  await expect
    .poll(() => new URL(page.url()).searchParams.get("section"))
    .toBe(section);
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

  test("canonical worklist state survives reload and the focused row opens the exact case", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyWorklistCase(page);
    const query = new URLSearchParams({
      workflow: "BACTERIOLOGY",
      q: seeded.caseId,
      sort: "newest",
      pageSize: "10",
    });
    const worklistUrl = `/Microbiology/worklist?${query}`;
    const caseUrl = `/Microbiology/cases/${seeded.caseId}?${query}`;

    const initialResponse = waitForWorklistResponse(page, {
      workflow: "BACTERIOLOGY",
      q: seeded.caseId,
      sort: "newest",
      pageSize: "10",
    });
    await page.goto(worklistUrl, { waitUntil: "domcontentloaded" });
    await initialResponse;

    const row = page.getByTestId(`microbiology-worklist-row-${seeded.caseId}`);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.getByLabel("Workflow", { exact: true })).toHaveValue(
      "BACTERIOLOGY",
    );
    await expect(
      page.getByPlaceholder(
        "Search lab number, patient, specimen, or workflow",
      ),
    ).toHaveValue(seeded.caseId);
    await expect(page.getByLabel("Sort", { exact: true })).toHaveValue(
      "newest",
    );
    await expect(page).toHaveURL(worklistUrl);

    const reloadResponse = waitForWorklistResponse(page, {
      workflow: "BACTERIOLOGY",
      q: seeded.caseId,
      sort: "newest",
      pageSize: "10",
    });
    await page.reload({ waitUntil: "domcontentloaded" });
    await reloadResponse;
    await expect(page).toHaveURL(worklistUrl);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });

    await expect(row).toHaveAttribute("tabindex", "0");
    await row.focus();
    await expect(row).toBeFocused();
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(caseUrl);
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  });

  test("critical communication raises worklist priority and sibling visibility", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = await seedMicrobiologyWorklistCase(page);
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
    const workflowResponse = waitForWorklistResponse(page, {
      workflow: "BACTERIOLOGY",
    });
    await page
      .getByLabel("Workflow", { exact: true })
      .selectOption("BACTERIOLOGY");
    await expect(page).toHaveURL(
      /\/Microbiology\/worklist\?workflow=BACTERIOLOGY$/,
    );
    await workflowResponse;
    const sortResponse = waitForWorklistResponse(page, {
      workflow: "BACTERIOLOGY",
      sort: "newest",
    });
    await page.getByLabel("Sort", { exact: true }).selectOption("newest");
    await expect(page).toHaveURL(
      /\/Microbiology\/worklist\?workflow=BACTERIOLOGY&sort=newest$/,
    );
    await sortResponse;

    await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
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

    await page.goto(scopedWorklistUrl, { waitUntil: "domcontentloaded" });
    const row = page.getByTestId(`microbiology-worklist-row-${seeded.caseId}`);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText("High");
    await expect(row).toContainText("Critical communication");
    await expect(row).toContainText("Mycobacteriology/TB");
    await expect(
      page.getByTestId("microbiology-worklist-summary-critical"),
    ).toContainText("1");

    await row.getByRole("button", { name: "Open case" }).click();
    await expect(page).toHaveURL(scopedCaseUrl);
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await openCaseSection(
      page,
      "Critical communication",
      "critical-communication",
    );
    await page.getByRole("button", { name: "Acknowledge" }).click();
    await expect(
      page.getByTestId("microbiology-critical-status"),
    ).toContainText("Acknowledged", { timeout: LONG_TIMEOUT });
    await openCaseSection(page, "Isolates", "isolates");
    await expect(page.getByRole("region", { name: "Isolates" })).toBeVisible();
    const restoredWorklistResponse = waitForWorklistResponse(page, {
      workflow: "BACTERIOLOGY",
      q: seeded.caseId,
      sort: "newest",
    });
    await page
      .getByRole("navigation", { name: "Breadcrumb" })
      .getByRole("link", { name: "Microbiology worklist" })
      .click();
    await expect(page).toHaveURL(scopedWorklistUrl);
    await restoredWorklistResponse;
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  });
});
