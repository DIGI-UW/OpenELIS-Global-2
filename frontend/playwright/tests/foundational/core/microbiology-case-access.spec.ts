import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyWorklistCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test.describe("Microbiology V2 MVP case visibility", () => {
  test(
    "finds an owned case and opens the sample summary and testing workspace",
    { tag: ["@evidence", "@microbiology", "@case"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Finding a microbiology case",
        "A technologist finds a case on the worklist by its accession number and opens its samples and testing.",
      );
      await demo.caption(
        "Off screen: a culture is ordered and collected for a synthetic patient, which opens a microbiology case.",
      );
      const seeded = await seedMicrobiologyWorklistCase(page);
      const shellResponse = await page.request.get(
        `/api/OpenELIS-Global/rest/microbiology/cases/${seeded.caseId}/shell`,
      );
      expect(shellResponse.ok()).toBeTruthy();
      const shell = await shellResponse.json();
      expect(shell.labUnitId).toBeTruthy();
      expect(shell.labUnit).toBeTruthy();
      const query = new URLSearchParams({
        grain: "cases",
        q: seeded.accessionNumber,
        labUnitId: shell.labUnitId,
      });
      await page.goto(`/Microbiology/worklist?${query}`, {
        waitUntil: "domcontentloaded",
      });
      const link = page.getByRole("link", {
        name: seeded.accessionNumber,
        exact: true,
      });
      await expect(link).toBeVisible({ timeout: LONG_TIMEOUT });
      const row = page.getByRole("row").filter({ has: link });
      await expect(row).toContainText(shell.labUnit);
      await demo.caption(
        "The worklist finds the case by accession number, in the laboratory unit that owns it.",
      );
      await demo.highlight(row);
      await page.screenshot({
        path: testInfo.outputPath("case-worklist-desktop.png"),
        fullPage: true,
      });
      await link.click();
      await expect(
        page.getByRole("heading", {
          name: `Case ${seeded.accessionNumber}`,
          exact: true,
        }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "Testing and results", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("heading", { name: "Samples", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("row").filter({ hasText: "Collected" }),
      ).toContainText(seeded.accessionNumber);
      await demo.caption(
        "The case opens on its header, its collected sample and the testing workspace.",
      );
      await demo.highlight(
        page.getByRole("row").filter({ hasText: "Collected" }),
      );
      await page.screenshot({
        path: testInfo.outputPath("case-shell-desktop.png"),
        fullPage: true,
      });
      const frame = page.viewportSize();
      await demo.caption(
        "The same page is checked at phone width, shown enlarged here: it fits without sideways scrolling.",
      );
      await page.setViewportSize({ width: 390, height: 844 });
      await expect(
        page.getByRole("heading", {
          name: `Case ${seeded.accessionNumber}`,
          exact: true,
        }),
      ).toBeVisible();
      await expect
        .poll(
          () =>
            page.evaluate(
              () => document.documentElement.scrollWidth <= window.innerWidth,
            ),
          { timeout: LONG_TIMEOUT },
        )
        .toBeTruthy();
      await page.screenshot({
        path: testInfo.outputPath("case-shell-mobile.png"),
        fullPage: true,
      });
      if (frame) await page.setViewportSize(frame);
      await demo.caption(
        "The breadcrumb returns to the same filtered worklist.",
      );
      await page
        .getByRole("link", { name: "Microbiology worklist", exact: true })
        .filter({ hasNot: page.locator(".cds--side-nav__link-text") })
        .click();
      await expect(page).toHaveURL(`/Microbiology/worklist?${query}`);
      const caseLink = page.getByRole("link", {
        name: seeded.accessionNumber,
        exact: true,
      });
      await expect(caseLink).toBeVisible();
      await demo.highlight(caseLink);
      await demo.verified(
        "The case is one click from the worklist and back",
        `${seeded.accessionNumber} opened with its sample and testing, and the worklist kept its filter.`,
      );
    },
  );
});
