import { expect, test } from "../../../helpers/test-base";
import { expectNoWcag21AaViolations } from "../../../helpers/accessibility";
import { captureEvidenceScreenshot } from "../../../helpers/demo-presentation";
import { seedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import { ensureTextMacroViaAdmin } from "../../../helpers/text-macro";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const macro = {
  code: ".uat_ng24",
  expansionText: "No growth at 24 hours",
  contexts: ["Culture activity"] as const,
};

test.describe("OGC-788 macro library accessibility", () => {
  test("admin library and suggestion list meet WCAG 2.1 AA", async ({
    page,
  }, testInfo) => {
    test.setTimeout(90_000);
    await ensureTextMacroViaAdmin(page, {
      ...macro,
      contexts: [...macro.contexts],
    });
    if (testInfo.project.name.endsWith("-mobile")) {
      const tableContent = page.locator(
        ".text-macro-admin .cds--data-table-content",
      );
      const dimensions = await tableContent.evaluate((element) => ({
        clientWidth: element.clientWidth,
        scrollWidth: element.scrollWidth,
      }));
      expect(dimensions.scrollWidth).toBeLessThanOrEqual(
        dimensions.clientWidth,
      );
      await expect(
        page.getByRole("columnheader", { name: "Shortcut code" }),
      ).toBeInViewport();
      await expect(
        page.getByRole("columnheader", { name: "Phrase text" }),
      ).toBeInViewport();
      await captureEvidenceScreenshot(
        page,
        testInfo,
        "ogc-788-m1-04-mobile-admin",
        {
          locator: page.locator(".text-macro-admin .cds--data-table-container"),
        },
      );
    }
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "text-macro-admin-library",
    );

    const seeded = await seedMicrobiologyCase(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}?section=setup`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

    const runtimeLoaded = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/text-macros?") &&
        response.request().method() === "GET" &&
        response.ok(),
    );
    const note = page.getByLabel("Activity note");
    await note.focus();
    await runtimeLoaded;
    await note.fill(macro.code);
    await expect(
      page.getByRole("option", {
        name: `${macro.code} ${macro.expansionText}`,
      }),
    ).toBeVisible();
    if (testInfo.project.name.endsWith("-mobile")) {
      const optionBounds = await page
        .getByRole("option", {
          name: `${macro.code} ${macro.expansionText}`,
        })
        .boundingBox();
      const actionBounds = await page
        .getByRole("button", { name: "Start inoculation" })
        .boundingBox();
      if (!optionBounds || !actionBounds) {
        throw new Error("Macro option and primary action must be rendered");
      }
      expect(optionBounds.y + optionBounds.height).toBeLessThanOrEqual(
        actionBounds.y,
      );
      await note.scrollIntoViewIfNeeded();
      await captureEvidenceScreenshot(
        page,
        testInfo,
        "ogc-788-m1-05-mobile-suggestions",
      );
    }
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "text-macro-runtime-suggestions",
    );
  });

  test("exact shortcut expansion is keyboard operable", async ({
    page,
  }, testInfo) => {
    test.skip(
      testInfo.project.name.endsWith("-mobile"),
      "Keyboard qualification runs in the desktop accessibility project",
    );
    test.setTimeout(90_000);
    await ensureTextMacroViaAdmin(page, {
      ...macro,
      contexts: [...macro.contexts],
    });
    const seeded = await seedMicrobiologyCase(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}?section=setup`, {
      waitUntil: "domcontentloaded",
    });

    const runtimeLoaded = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/text-macros?") &&
        response.request().method() === "GET" &&
        response.ok(),
    );
    const note = page.getByLabel("Activity note");
    await note.focus();
    await runtimeLoaded;
    await note.fill(macro.code);
    await expect(
      page.getByRole("option", {
        name: `${macro.code} ${macro.expansionText}`,
      }),
    ).toBeVisible();
    await note.press("Tab");
    await expect(note).toHaveValue(macro.expansionText);
    await expect(note).toBeFocused();
  });
});
