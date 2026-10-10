import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { seedMicrobiologyWhonetExport } from "../../../helpers/seed-microbiology-data";
import {
  buildWhonetExportQuery,
  expectWhonetExportReady,
} from "../../../helpers/whonet-export";

const configureAdvancedPolicy = async (
  page: import("@playwright/test").Page,
) => {
  await page
    .getByRole("button", { name: "Adjust first-isolate policy" })
    .click();
  await page
    .getByRole("combobox", { name: "Window length" })
    .selectOption("FIRST_ISOLATE_14_DAY");
  await page.getByText("Final result-release date", { exact: true }).click();
  await page.getByText("Same specimen source only", { exact: true }).click();
  await page
    .getByText("Exclude probable contaminants before selection", {
      exact: true,
    })
    .click();
  await page.getByText("Treat changed S/I/R as new", { exact: true }).click();
};

test.describe("OGC-782 R13 first-isolate policy", () => {
  test("captures the bookmarkable policy on desktop and mobile", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    const seeded = await seedMicrobiologyWhonetExport(page);
    const initialQuery = buildWhonetExportQuery(seeded.exportDate);
    const configuredQuery = buildWhonetExportQuery(seeded.exportDate, {
      dedup: "FIRST_ISOLATE_14_DAY",
      dedupBasis: "RELEASE_DATE",
      dedupScope: "SAME_SOURCE",
      excludeContaminants: false,
      profileSensitivity: "SENSITIVE",
    });

    await demo.chapter({
      eyebrow: "OGC-782 R13",
      title: "Configure first-isolate selection",
      subtitle:
        "Choose the surveillance window, chronology, specimen scope, contaminant handling, and S/I/R profile behavior",
    });
    await page.goto(`/Microbiology/whonet?${initialQuery}`, {
      waitUntil: "commit",
    });
    await expectWhonetExportReady(page);
    await configureAdvancedPolicy(page);
    await expect(page).toHaveURL(`/Microbiology/whonet?${configuredQuery}`);
    await demo.evidence("ogc-782-r13-01-first-isolate-policy-desktop", {
      locator: page.locator(".whonet-export__first-isolate"),
    });

    await page.setViewportSize({ width: 390, height: 844 });
    await page.reload({ waitUntil: "commit" });
    await expectWhonetExportReady(page);
    await page
      .getByRole("button", { name: "Adjust first-isolate policy" })
      .click();
    await expect(
      page.getByRole("combobox", { name: "Window length" }),
    ).toHaveValue("FIRST_ISOLATE_14_DAY");
    await expect(
      page.getByRole("radio", { name: "Final result-release date" }),
    ).toBeChecked();

    // Keep the mobile breakpoint while fitting the complete policy section below
    // Carbon's fixed header for an unobscured component evidence capture.
    await page.setViewportSize({ width: 390, height: 1200 });
    await demo.evidence("ogc-782-r13-02-first-isolate-policy-mobile", {
      locator: page.locator(".whonet-export__first-isolate"),
    });
  });
});
