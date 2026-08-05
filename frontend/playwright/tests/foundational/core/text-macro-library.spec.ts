import { readFile } from "node:fs/promises";
import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import {
  confirmTextMacroBulkAction,
  ensureTextMacroViaAdmin,
  expandTextMacro,
  exportTextMacroLibrary,
  openTextMacroLibrary,
  textMacroRow,
} from "../../../helpers/text-macro";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const macro = {
  code: ".uat_ng24",
  expansionText: "No growth at 24 hours",
  contexts: ["Culture activity"] as const,
};

const bulkMacros = [
  {
    code: ".uat_bulk_followup",
    expansionText: "Follow-up culture is recommended",
    contexts: ["Culture activity"] as const,
  },
  {
    code: ".uat_bulk_negative",
    expansionText: "Culture remains negative",
    contexts: ["Culture activity"] as const,
  },
];

test.describe("OGC-788 managed macro runtime", () => {
  test("administers a phrase and persists its expansion in a microbiology case", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await ensureTextMacroViaAdmin(page, {
      ...macro,
      contexts: [...macro.contexts],
    });
    expect(new URL(page.url()).searchParams.get("q")).toBe(macro.code);

    const seeded = await seedMicrobiologyCase(page);
    expect(seeded.textMacroCode).toBe(macro.code);
    const caseUrl = `/Microbiology/cases/${seeded.caseId}?section=setup`;
    await page.goto(caseUrl, { waitUntil: "domcontentloaded" });
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

    const note = page.getByLabel("Activity note");
    await expandTextMacro(page, note, macro, "Culture observation: ");
    const recorded = page.waitForResponse(
      (response) =>
        response
          .url()
          .includes(`/rest/microbiology/cases/${seeded.caseId}/activities`) &&
        response.request().method() === "POST" &&
        response.ok(),
    );
    await page.getByRole("button", { name: "Start inoculation" }).click();
    await recorded;

    await page.getByRole("button", { name: "Timeline", exact: true }).click();
    await expect(page).toHaveURL(
      new RegExp(`/Microbiology/cases/${seeded.caseId}\\?section=timeline$`),
    );
    await expect(
      page.getByText(`Culture observation: ${macro.expansionText}`),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

    const bookmark = page.url();
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(bookmark);
    await expect(
      page.getByText(`Culture observation: ${macro.expansionText}`),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  });

  test("bulk administers local phrases and downloads a deterministic CSV", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    for (const fixture of bulkMacros) {
      await ensureTextMacroViaAdmin(page, {
        ...fixture,
        contexts: [...fixture.contexts],
      });
    }

    const codes = bulkMacros.map(({ code }) => code);
    await openTextMacroLibrary(page, { q: ".uat_bulk_", status: "all" });
    const canonicalUrl = page.url();

    await confirmTextMacroBulkAction(page, {
      codes,
      actionLabel: "Deactivate",
      dialogName: "Deactivate 2 phrases?",
      confirmLabel: "Deactivate phrases",
    });
    for (const code of codes) {
      await expect(textMacroRow(page, code)).toContainText("Inactive", {
        timeout: LONG_TIMEOUT,
      });
    }
    await expect(page).toHaveURL(canonicalUrl);

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(canonicalUrl);
    for (const code of codes) {
      await expect(textMacroRow(page, code)).toContainText("Inactive", {
        timeout: LONG_TIMEOUT,
      });
    }

    const download = await exportTextMacroLibrary(page);
    const downloadPath = await download.path();
    expect(downloadPath).not.toBeNull();
    const csv = await readFile(downloadPath as string, "utf8");
    const lines = csv.trimEnd().split(/\r?\n/);
    expect(lines[0]).toBe(
      "code,expansion_text,contexts,active,provenance,source_key,source_version",
    );
    for (const fixture of bulkMacros) {
      expect(lines.some((line) => line.startsWith(`${fixture.code},`))).toBe(
        true,
      );
    }
    expect(
      lines.findIndex((line) => line.startsWith(`${codes[0]},`)),
    ).toBeLessThan(lines.findIndex((line) => line.startsWith(`${codes[1]},`)));

    await confirmTextMacroBulkAction(page, {
      codes,
      actionLabel: "Activate",
      dialogName: "Activate 2 phrases?",
      confirmLabel: "Activate phrases",
    });
    for (const code of codes) {
      await expect(textMacroRow(page, code)).toContainText("Active", {
        timeout: LONG_TIMEOUT,
      });
    }

    await confirmTextMacroBulkAction(page, {
      codes,
      actionLabel: "Remove local phrases",
      dialogName: "Remove 2 local phrases?",
      confirmLabel: "Remove phrases",
    });
    for (const code of codes) {
      await expect(textMacroRow(page, code)).toHaveCount(0);
    }
    await expect(page).toHaveURL(canonicalUrl);
  });
});
