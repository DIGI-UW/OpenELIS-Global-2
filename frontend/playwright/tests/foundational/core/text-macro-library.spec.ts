import { expect, test } from "../../../helpers/test-base";
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
});
