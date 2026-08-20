import { readFile } from "node:fs/promises";
import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  ensureTextMacroViaAdminApi,
  exportTextMacroLibrary,
  openTextMacroBulkAction,
  openTextMacroLibrary,
  selectTextMacroRows,
  submitOpenTextMacroBulkAction,
  textMacroRow,
} from "../../../helpers/text-macro";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const macros = [
  {
    code: ".uat_admin_followup",
    expansionText: "Follow-up culture is recommended",
    contexts: ["Culture activity"] as const,
  },
  {
    code: ".uat_admin_negative",
    expansionText: "Culture remains negative",
    contexts: ["Culture activity"] as const,
  },
];

test.describe("OGC-788 M2 broader administration demo", () => {
  test("bulk manages and exports reusable phrases", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    const codes = macros.map(({ code }) => code);

    await Promise.all([
      demo.chapter({
        eyebrow: "OGC-788 M2",
        title: "Broader phrase administration",
        subtitle:
          "Administrators select, confirm, update, and export reusable phrases through one Carbon workflow",
        durationMs: 4500,
      }),
      ...macros.map((fixture) =>
        ensureTextMacroViaAdminApi(page, {
          ...fixture,
          contexts: [...fixture.contexts],
        }),
      ),
    ]);
    await openTextMacroLibrary(page, { q: ".uat_admin_", status: "all" });
    const canonicalUrl = page.url();

    await test.step("Select phrases in the shared library", async () => {
      await demo.chapter({
        eyebrow: "Story 1",
        title: "Select related phrases",
        subtitle:
          "The same bookmarkable Carbon table supports explicit multi-row administration",
        accent: "#24a148",
        durationMs: 4500,
      });
      await selectTextMacroRows(page, codes);
      await expect(
        page.getByRole("button", { name: "Deactivate", exact: true }),
      ).toBeVisible();
      await demo.evidence("ogc-788-m2-01-selected-phrases");
      await demo.pause(4500);
    });

    await test.step("Confirm a named bulk update", async () => {
      await demo.chapter({
        eyebrow: "Story 2",
        title: "Review before changing availability",
        subtitle:
          "The confirmation names the action and every shortcut before applying the change",
        accent: "#ff832b",
        durationMs: 4500,
      });
      const dialog = await openTextMacroBulkAction(page, {
        codes,
        actionLabel: "Deactivate",
        dialogName: "Deactivate 2 phrases?",
        confirmLabel: "Deactivate phrases",
      });
      await demo.evidence("ogc-788-m2-02-named-confirmation");
      await demo.pause(4500);
      await submitOpenTextMacroBulkAction(page, dialog, "Deactivate phrases");
      for (const code of codes) {
        await expect(textMacroRow(page, code)).toContainText("Inactive", {
          timeout: LONG_TIMEOUT,
        });
      }
      await expect(page).toHaveURL(canonicalUrl);
      await demo.evidence("ogc-788-m2-03-inactive-phrases");
      await demo.pause(4000);
    });

    await test.step("Export an auditable snapshot", async () => {
      await demo.chapter({
        eyebrow: "Story 3",
        title: "Export the phrase library",
        subtitle:
          "A deliberate download produces a deterministic CSV without internal identifiers",
        accent: "#8a3ffc",
        durationMs: 4500,
      });
      const download = await exportTextMacroLibrary(page);
      const downloadPath = await download.path();
      expect(downloadPath).not.toBeNull();
      const csv = await readFile(downloadPath as string, "utf8");
      for (const code of codes) expect(csv).toContain(code);
      await testInfo.attach("openelis-text-macros.csv", {
        body: Buffer.from(csv),
        contentType: "text/csv",
      });
      await demo.evidence("ogc-788-m2-04-exported-library");
      await demo.pause(4000);
    });

    await demo.chapter({
      eyebrow: "Evidence complete",
      title: "Automated OGC-788 M2 journey passed",
      subtitle:
        "Selection, named confirmation, availability changes, canonical URL retention, and CSV export passed; reviewed package import remains a separate clinical-input gate.",
      durationMs: 5000,
    });
  });
});
