import { test, expect } from "../../../helpers/test-base";
import type { Locator } from "@playwright/test";
import {
  seedFinalizedMicrobiologyCase,
  seedMicrobiologyMvpCase,
  seedMicrobiologyReferenceAdmin,
  seedMicrobiologyWhonetExport,
  seedMicrobiologyWorklistCase,
  seedReviewedMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import { expectNoWcag21AaViolations } from "../../../helpers/accessibility";
import {
  createAndIdentifyMicrobiologyIsolate,
  openMicrobiologyCaseSection,
} from "../../../helpers/microbiology-ui";
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

  test("shared reagent lot picker in culture and AST", async ({
    page,
  }, testInfo) => {
    const seeded = await seedMicrobiologyMvpCase(page);

    await page.goto(`/Microbiology/cases/${seeded.caseId}?section=setup`, {
      waitUntil: "domcontentloaded",
    });
    const setup = page.getByRole("region", { name: "Inoculation" });
    await setup.getByRole("button", { name: "Start inoculation" }).click();
    await expect(
      setup.getByRole("searchbox", { name: "Scan or enter lot number" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(setup.getByText("FEFO - use first").first()).toBeVisible();
    await expect(setup.getByText("QC passed").first()).toBeVisible();
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "microbiology-reagent-picker-culture",
    );
    await setup.getByRole("button", { name: "Cancel" }).click();

    await createAndIdentifyMicrobiologyIsolate(page, seeded.organismId!);
    await openMicrobiologyCaseSection(page, "Manual AST");
    const ast = page.getByTestId("microbiology-ast-card");
    await expect(
      ast.getByRole("searchbox", { name: "Scan or enter lot number" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(ast.getByText("FEFO - use first").first()).toBeVisible();
    await expect(ast.getByText("QC passed").first()).toBeVisible();
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "microbiology-reagent-picker-ast",
    );
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
    const expectMobileNavigationClosed = async () => {
      if (!testInfo.project.name.endsWith("-mobile")) return;
      await expect(page.getByRole("button", { name: "Open menu" })).toBeVisible(
        { timeout: LONG_TIMEOUT },
      );
    };
    const expectInsideViewport = async (locator: Locator) => {
      const box = await locator.boundingBox();
      const viewport = page.viewportSize();
      if (!box || !viewport) {
        throw new Error("Expected a visible element inside an active viewport");
      }
      expect(box.x).toBeGreaterThanOrEqual(0);
      expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
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
        await expectMobileNavigationClosed();
        await expect(page.getByRole("heading", { name: heading })).toBeVisible({
          timeout: LONG_TIMEOUT,
        });
        if (testInfo.project.name.endsWith("-mobile")) {
          await expectInsideViewport(
            page.getByPlaceholder("Search reference data"),
          );
          if (evidenceName === "microbiology-reference-organisms") {
            await expectInsideViewport(
              page.getByRole("button", { name: "Add organism" }),
            );
          }
          if (evidenceName === "microbiology-reference-breakpoints") {
            await expectInsideViewport(
              page.getByRole("button", { name: "Activate standard" }),
            );
            await expectInsideViewport(
              page.getByRole("button", { name: "Archive standard" }),
            );
          }
        }
        await expectNoWcag21AaViolations(page, testInfo, evidenceName);
      });
    }
  });

  test("WHONET configuration and preview", async ({ page }, testInfo) => {
    const seeded = await seedMicrobiologyWhonetExport(page);
    const query = new URLSearchParams({
      from: seeded.exportDate,
      to: seeded.exportDate,
      significance: "CLINICALLY_SIGNIFICANT",
      dedup: "FIRST_ISOLATE_7_DAY",
      step: "configure",
      page: "1",
      pageSize: "20",
    });

    await page.goto(`/Microbiology/whonet?${query}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "WHONET export", exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "microbiology-whonet-configure",
    );

    const previewResponse = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/microbiology/whonet/preview?") &&
        response.status() === 200,
    );
    await page.getByRole("button", { name: "Preview export" }).click();
    await previewResponse;
    await expect(
      page.getByRole("heading", { name: "Preview", exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const tableRegion = page.getByRole("region", {
      name: "Eligible AST rows",
    });
    await tableRegion.focus();
    await expect(tableRegion).toBeFocused();
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "microbiology-whonet-preview",
    );
  });
});
