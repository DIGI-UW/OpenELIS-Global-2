import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { seedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import {
  ensureTextMacroViaAdmin,
  expandTextMacro,
} from "../../../helpers/text-macro";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const macro = {
  code: ".uat_ng24",
  expansionText: "No growth at 24 hours",
  contexts: ["Culture activity"] as const,
};

test.describe("OGC-788 M1 managed macro runtime demo", () => {
  test("manages a phrase and expands it into a culture activity", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);

    await demo.chapter({
      eyebrow: "OGC-788 M1",
      title: "Managed macro phrases",
      subtitle:
        "Administrators maintain reusable text while laboratory staff expand it in context",
    });

    await test.step("Manage a reusable phrase", async () => {
      await demo.chapter({
        eyebrow: "Story 1",
        title: "Maintain the phrase library",
        subtitle:
          "A bookmarkable Carbon table keeps shortcuts, text, context, and status explicit",
        accent: "#24a148",
      });
      await ensureTextMacroViaAdmin(page, {
        ...macro,
        contexts: [...macro.contexts],
      });
      expect(new URL(page.url()).searchParams.get("q")).toBe(macro.code);
      const row = page.getByRole("row").filter({ hasText: macro.code });
      await expect(row).toContainText(macro.expansionText);
      await demo.evidence("ogc-788-m1-01-managed-phrase", {
        fullPage: true,
      });
      await demo.pause(2500);
    });

    const seeded = await seedMicrobiologyCase(page);
    const caseUrl = `/Microbiology/cases/${seeded.caseId}?section=setup`;

    await test.step("Expand the phrase in laboratory work", async () => {
      await demo.chapter({
        eyebrow: "Story 2",
        title: "Expand text in context",
        subtitle:
          "The culture activity field suggests eligible phrases and replaces the exact shortcut",
        accent: "#ff832b",
      });
      await page.goto(caseUrl, { waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const note = page.getByLabel("Activity note");
      await expandTextMacro(page, note, macro, "Culture observation: ");
      await demo.evidence("ogc-788-m1-02-expanded-activity", {
        locator: page.getByTestId("microbiology-case-view"),
      });
      await demo.pause(2500);

      await page.getByRole("button", { name: "Start inoculation" }).click();
      await expect(
        page.locator("header").getByTitle("Setup Recorded"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Verify persisted plain text", async () => {
      await demo.chapter({
        eyebrow: "Story 3",
        title: "Preserve the recorded meaning",
        subtitle:
          "The expanded phrase is stored as ordinary clinical text and survives a bookmarked reload",
        accent: "#8a3ffc",
      });
      await page.getByRole("button", { name: "Timeline", exact: true }).click();
      const expectedText = `Culture observation: ${macro.expansionText}`;
      await expect(page).toHaveURL(
        new RegExp(`/Microbiology/cases/${seeded.caseId}\\?section=timeline$`),
      );
      await expect(page.getByText(expectedText)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      const bookmark = page.url();
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page).toHaveURL(bookmark);
      await expect(page.getByText(expectedText)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await demo.evidence("ogc-788-m1-03-persisted-timeline", {
        fullPage: true,
      });
      await demo.pause(3000);
    });

    await demo.chapter({
      eyebrow: "Evidence complete",
      title: "Automated OGC-788 M1 journey passed",
      subtitle:
        "Phrase administration, contextual expansion, persistence, and bookmark recovery passed; human Review-overlay rulings remain separate.",
      durationMs: 4000,
    });
  });
});
