import { writeFile } from "node:fs/promises";
import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyMissingContext } from "../../../helpers/seed-microbiology-data";
import { selectCultureMedium } from "../../../helpers/microbiology-ui";
import { readMicrobiologyPatientReport } from "../../../helpers/microbiology-patient-report";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test(
  "completes missing clinical context through release blockers, preserves row notes and opens an amendment",
  { tag: ["@evidence", "@microbiology", "@case"] },
  async ({ page }, info) => {
    test.setTimeout(150_000);
    const demo = createDemoPresentation(page, info);
    await demo.intro(
      "Completing clinical context before release",
      "Release blockers link straight to the missing context, culture notes choose whether they reach the report, and a released case opens for amendment.",
    );
    await demo.caption(
      "Off screen: a culture case is ordered without the patient origin or antibiotic history.",
    );
    const seeded = await seedMicrobiologyMissingContext(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}?scope=REPORT`);
    const origin = page.getByLabel("Patient origin", { exact: true });
    await expect(origin).toHaveValue("");
    await page.getByRole("button", { name: "Case info", exact: true }).click();
    await expect(origin).not.toBeVisible();
    const blocker = page.getByRole("link", { name: /Patient origin/ });
    await demo.caption(
      "The report section lists what blocks release. Patient origin links to the field.",
    );
    await demo.highlight(blocker);
    await blocker.click();
    await expect(
      page.getByRole("region", { name: "Case info", exact: true }),
    ).toBeFocused();
    await expect(origin).toBeVisible();
    await demo.caption(
      "The link opens Case info at the field. Saving the origin clears that blocker.",
    );
    await origin.selectOption("INPATIENT");
    await page
      .getByRole("button", { name: "Save order detail", exact: true })
      .click();
    await expect(blocker).toHaveCount(0, { timeout: LONG_TIMEOUT });
    await demo.caption(
      "The antibiotic exposure questions are answered the same way.",
    );
    const exposure = page.getByLabel(/Recent antibiotic exposure/);
    await exposure.selectOption("true");
    await page.getByLabel(/Antibiotic name/).fill("Ampicillin");
    await page
      .getByRole("button", { name: "Save questionnaire answers", exact: true })
      .click();
    await expect(
      page.getByRole("link", {
        name: /Recent antibiotic exposure|Antibiotic name/,
      }),
    ).toHaveCount(0, { timeout: LONG_TIMEOUT });
    await page.reload();
    await expect(origin).toHaveValue("INPATIENT");
    await expect(exposure).toHaveValue("true");
    await expect(page.getByLabel(/Antibiotic name/)).toHaveValue("Ampicillin");
    await expect(
      page.getByLabel("Date of admission", { exact: true }),
    ).toHaveValue("");
    await demo.caption(
      "A culture is inoculated. It gets two notes: one internal, one marked Send with Result.",
    );
    const section = page.getByLabel("Testing section", { exact: true });
    await section.selectOption("CULTURE");
    await page
      .getByRole("button", { name: "Inoculate culture", exact: true })
      .click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel("Bottle or plate identifier").fill("M9-PLATE");
    await selectCultureMedium(page, seeded.mediumItemId!);
    for (const label of ["Medium lot", "Atmosphere"]) {
      const field = dialog.getByLabel(label, { exact: true });
      const value = await field
        .locator("option")
        .evaluateAll((options) =>
          options.map((o) => (o as HTMLOptionElement).value).find(Boolean),
        );
      if (!value) throw new Error(`Fixture has no ${label}`);
      await field.selectOption(value);
    }
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    const culture = page
      .locator('[data-testid^="culture-row-"]')
      .filter({ has: page.getByRole("heading", { name: /M9-PLATE/ }) });
    await expect(culture).toBeVisible({ timeout: LONG_TIMEOUT });
    await culture
      .getByRole("button", { name: "Notes (0)", exact: true })
      .click();
    await dialog
      .getByLabel("New note", { exact: true })
      .fill("M9 internal culture note");
    await dialog
      .getByRole("button", { name: "Save note", exact: true })
      .click();
    await expect(dialog.getByLabel("New note", { exact: true })).toHaveValue(
      "",
    );
    await expect(
      dialog.locator("p").filter({ hasText: /^M9 internal culture note$/ }),
    ).toBeVisible();
    await dialog
      .getByLabel("New note", { exact: true })
      .fill("M9 culture-specific report note");
    await dialog.getByText("Send with Result", { exact: true }).click();
    await dialog
      .getByRole("button", { name: "Save note", exact: true })
      .click();
    await expect(dialog.getByLabel("New note", { exact: true })).toHaveValue(
      "",
    );
    await expect(
      dialog
        .locator("p")
        .filter({ hasText: /^M9 culture-specific report note$/ }),
    ).toBeVisible();
    await dialog
      .getByRole("button", { name: "Close", exact: true })
      .last()
      .click();
    await demo.highlight(
      culture.getByRole("button", { name: "Notes (2)", exact: true }),
    );
    await expect(
      culture.getByRole("button", { name: "Notes (2)", exact: true }),
    ).toBeVisible();
    await page.reload();
    await expect(
      culture.getByRole("button", { name: "Notes (2)", exact: true }),
    ).toBeVisible();
    await demo.caption(
      "The culture shows no growth, and with nothing blocking it the final report is released.",
    );
    await culture
      .getByRole("button", { name: "Record outcome", exact: true })
      .click();
    await dialog
      .getByLabel("Outcome", { exact: true })
      .selectOption("NO_GROWTH");
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(culture.getByText("No growth", { exact: true })).toBeVisible();
    await section.selectOption("REPORT");
    const release = page.getByRole("button", {
      name: "Release final report",
      exact: true,
    });
    await expect(release).toBeEnabled({ timeout: LONG_TIMEOUT });
    await release.click();
    await expect(
      page.getByText("Final case is read-only", { exact: true }),
    ).toBeVisible();
    await demo.highlight(
      page.getByText("Final case is read-only", { exact: true }),
    );
    const { text, pdf } = await readMicrobiologyPatientReport(page, seeded);
    expect(text).toContain("M9-PLATE: No growth");
    expect(text).toContain("M9 culture-specific report note");
    expect(text).not.toContain("M9 internal culture note");
    expect(text.indexOf("M9 culture-specific report note")).toBeLessThan(
      text.indexOf("AST / DST"),
    );
    await writeFile(info.outputPath("m9-report-notes.pdf"), pdf);
    await info.attach("m9-report-notes.pdf", {
      path: info.outputPath("m9-report-notes.pdf"),
      contentType: "application/pdf",
    });
    await demo.caption(
      "The printed report carries the note marked Send with Result, not the internal one. Next, an amendment reopens the case.",
    );
    await page.goto(`/Microbiology/cases/${seeded.caseId}?scope=AMENDMENT`);
    const amendment = page.getByTestId("microbiology-amendment-card");
    await amendment
      .getByLabel("Amendment reason")
      .fill("Confirm corrected clinical history");
    await amendment
      .getByRole("button", { name: "Open amendment", exact: true })
      .click();
    await expect(origin).toBeEnabled({ timeout: LONG_TIMEOUT });
    await demo.caption(
      "With the amendment open, the clinical context can be corrected again.",
    );
    await demo.highlight(origin);
    const frame = page.viewportSize();
    for (const width of [1440, 390]) {
      await page.setViewportSize({ width, height: 1000 });
      await expect
        .poll(() =>
          page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        )
        .toBe(true);
      await page.evaluate(() => window.scrollTo(0, 0));
      await page.screenshot({
        path: info.outputPath(`m9-viewport-${width}.png`),
      });
      await page.screenshot({
        path: info.outputPath(`m9-canonical-${width}.png`),
        fullPage: true,
      });
    }
    if (frame) await page.setViewportSize(frame);
    await demo.verified(
      "Clinical context completed, the report released, and an amendment open",
      "The blockers led to each missing field, and only the note marked for the report was printed.",
    );
  },
);
