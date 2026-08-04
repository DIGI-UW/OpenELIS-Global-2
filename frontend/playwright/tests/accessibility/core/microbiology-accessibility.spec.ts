import { test, expect } from "../../../helpers/test-base";
import {
  seedFinalizedMicrobiologyCase,
  seedMicrobiologyReferenceAdmin,
  seedMicrobiologyWorklistCase,
  seedReviewedMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import { expectNoWcag21AaViolations } from "../../../helpers/accessibility";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Microbiology WCAG 2.1 AA qualification", () => {
  test("worklist and case overview", async ({ page }, testInfo) => {
    const seeded = await seedMicrobiologyWorklistCase(page);

    await page.goto(
      "/Microbiology/worklist?workflow=BACTERIOLOGY&stage=ALL&urgency=ALL&due=ALL&q=&sort=accessionNumber%2Casc&page=1&pageSize=10",
      { waitUntil: "domcontentloaded" },
    );
    await expect(
      page.getByRole("heading", { name: "Microbiology worklist" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expectNoWcag21AaViolations(page, testInfo, "microbiology-worklist");

    await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "microbiology-case-overview",
    );
  });

  test("isolate, AST, critical communication, and reporting panels", async ({
    page,
  }, testInfo) => {
    const seeded = await seedReviewedMicrobiologyCase(page);
    const surfaces = [
      ["isolates", "Isolates", "microbiology-isolates"],
      ["ast", "Manual AST", "microbiology-ast"],
      [
        "critical-communication",
        "Critical communication",
        "microbiology-critical-communication",
      ],
      ["reports", "Report readiness", "microbiology-reporting"],
    ] as const;

    for (const [section, heading, evidenceName] of surfaces) {
      await test.step(`Scan ${heading}`, async () => {
        await page.goto(
          `/Microbiology/cases/${seeded.caseId}?section=${section}`,
          { waitUntil: "domcontentloaded" },
        );
        await expect(page.getByRole("heading", { name: heading })).toBeVisible({
          timeout: LONG_TIMEOUT,
        });
        await expectNoWcag21AaViolations(page, testInfo, evidenceName);
      });
    }
  });

  test("final-case amendment panel", async ({ page }, testInfo) => {
    const seeded = await seedFinalizedMicrobiologyCase(page);

    await page.goto(`/Microbiology/cases/${seeded.caseId}?section=amendment`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByRole("heading", { name: "Amendments" })).toBeVisible(
      { timeout: LONG_TIMEOUT },
    );
    await expectNoWcag21AaViolations(page, testInfo, "microbiology-amendment");
  });

  test("reference and breakpoint administration", async ({
    page,
  }, testInfo) => {
    const seeded = await seedMicrobiologyReferenceAdmin(page);
    const closeMobileNavigation = async () => {
      if (!testInfo.project.name.endsWith("-mobile")) return;
      const closeMenu = page.getByRole("button", { name: "Close menu" });
      await expect(closeMenu).toBeVisible({ timeout: LONG_TIMEOUT });
      await closeMenu.click();
      await expect(
        page.getByRole("button", { name: "Open menu" }),
      ).toBeVisible();
    };
    const surfaces = [
      [
        "/MasterListsPage/MicrobiologyReference/organisms?q=Reference%20organism%20%28UAT%29&status=ALL&sort=name&page=1&pageSize=20",
        "Organisms",
        "microbiology-reference-organisms",
      ],
      [
        "/MasterListsPage/MicrobiologyReference/ast-panels?q=Gram%20negative%20AST%20panel%20%28UAT%29&status=ALL&sort=name&page=1&pageSize=20",
        "AST panels",
        "microbiology-reference-ast-panels",
      ],
      [
        `/MasterListsPage/MicrobiologyReference/breakpoints/${seeded.loadedBreakpointStandardId}?status=ALL&sort=name&page=1&pageSize=20`,
        "CLSI SYNTH-UAT-LOADED",
        "microbiology-reference-breakpoints",
      ],
    ] as const;

    for (const [route, heading, evidenceName] of surfaces) {
      await test.step(`Scan ${heading}`, async () => {
        await page.goto(route, { waitUntil: "domcontentloaded" });
        await closeMobileNavigation();
        await expect(page.getByRole("heading", { name: heading })).toBeVisible({
          timeout: LONG_TIMEOUT,
        });
        await expectNoWcag21AaViolations(page, testInfo, evidenceName);
      });
    }
  });
});
