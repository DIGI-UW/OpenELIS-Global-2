import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Microbiology no-growth review and release", () => {
  test("records no growth without publishing, then releases one final negative result", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyCase(page);
    const worklistUrl = `/Microbiology/worklist?q=${encodeURIComponent(
      seeded.accessionNumber,
    )}&sort=newest`;
    await page.goto(worklistUrl, { waitUntil: "domcontentloaded" });

    const row = page.getByTestId(`microbiology-worklist-row-${seeded.caseId}`);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await row.getByRole("link", { name: seeded.accessionNumber }).click();
    await expect(page).toHaveURL(
      new RegExp(
        `/Microbiology/cases/${seeded.caseId}\\?q=${seeded.accessionNumber}&sort=newest&section=setup$`,
      ),
    );
    await expect(
      page
        .getByRole("navigation", { name: "Breadcrumb" })
        .getByRole("link", { name: "Microbiology worklist" }),
    ).toHaveAttribute(
      "href",
      `/Microbiology/worklist?q=${seeded.accessionNumber}&sort=newest`,
    );

    const caseHeader = page.locator("header");
    const caseView = page.getByTestId("microbiology-case-view");
    const nextStep = page.getByTestId("microbiology-next-step");
    await expect(caseHeader.getByTitle("Received")).toBeVisible();
    await nextStep.getByRole("button", { name: "Start inoculation" }).click();
    await expect(page).toHaveURL(/section=setup&action=start-inoculation$/);
    await page.getByLabel("Bottle or plate ID").fill("UAT-NO-GROWTH-01");
    await page.getByLabel("Media or bottle").fill("Blood culture bottle");
    await page.getByLabel("Incubation").fill("35 C for 48 hours");
    await page.getByLabel("Atmosphere").fill("Ambient");
    const mediaLot = page.getByRole("radio", {
      name: /UAT-MICRO-MEDIA-FEFO/,
    });
    await mediaLot.focus();
    await page.keyboard.press("Space");
    await expect(mediaLot).toBeChecked();
    await page.getByRole("button", { name: "Save media" }).click();
    await expect(caseHeader.getByTitle("Incubating")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    await caseView
      .getByRole("button", { name: "Mark no growth", exact: true })
      .click();
    await expect(page).toHaveURL(/section=setup&action=mark-no-growth$/);
    const transitionHeading = page.getByRole("heading", {
      name: "Mark culture as no growth",
    });
    await expect(transitionHeading).toBeFocused();
    await page.getByRole("button", { name: "Confirm no growth" }).click();

    await expect(caseHeader.getByTitle("No Growth Ready")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page).toHaveURL(
      new RegExp(
        `/Microbiology/cases/${seeded.caseId}\\?q=${seeded.accessionNumber}&sort=newest&section=setup$`,
      ),
    );
    await expect(nextStep).toContainText(
      "No growth recorded. Review and release the final negative report.",
    );
    const currentStep = page.getByTestId("microbiology-current-step-action");
    await expect(currentStep).toContainText("Reports");

    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    await expect(page).toHaveURL(/section=timeline$/);
    const timeline = page.getByTestId("microbiology-timeline-card");
    const noGrowthEvent = timeline.getByRole("listitem").filter({
      hasText: "Incubation complete with no growth",
    });
    await expect(noGrowthEvent).toContainText("Stage Changed");
    await expect(noGrowthEvent).toContainText("Performed by Open ELIS");

    await currentStep.getByRole("button", { name: "Open Reports" }).click();
    await expect(page).toHaveURL(/section=reports$/);
    await expect(
      page.getByTestId("microbiology-case-section-reports"),
    ).toBeFocused();
    await expect(
      page.getByTestId("microbiology-report-projection-content"),
    ).toHaveText("No growth");
    await expect(
      page.getByRole("button", { name: "Release preliminary report" }),
    ).toHaveCount(0);
    const releaseFinal = page.getByRole("button", {
      name: "Release final report",
    });
    await expect(releaseFinal).toBeEnabled({ timeout: LONG_TIMEOUT });

    await page.goto(`/PatientResults/${seeded.patientId}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Patient History" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      page.getByRole("row", {
        name: /UAT microbiology culture.*No growth/,
      }),
    ).toHaveCount(0);

    await page.goto(
      `/Microbiology/cases/${seeded.caseId}?q=${encodeURIComponent(
        seeded.accessionNumber,
      )}&sort=newest&section=reports`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(
      page.getByTestId("microbiology-report-projection-content"),
    ).toHaveText("No growth", { timeout: LONG_TIMEOUT });
    await page.getByRole("button", { name: "Release final report" }).click();
    await expect(page.getByTestId("microbiology-release-state")).toContainText(
      "Final Released",
      { timeout: LONG_TIMEOUT },
    );
    await expect(page.getByText("Final case is read-only")).toBeVisible();

    await page.getByRole("link", { name: "View patient results" }).click();
    await expect(
      page.getByRole("heading", { name: "Patient History" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      page.getByRole("row", {
        name: /UAT microbiology culture.*No growth/,
      }),
    ).toHaveCount(1);

    await page.goto(
      `/Microbiology/cases/${seeded.caseId}?q=${encodeURIComponent(
        seeded.accessionNumber,
      )}&sort=newest&section=setup`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(page.getByText("Final case is read-only")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page.getByRole("button", { name: "Change protocol" }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", { name: "Start inoculation" }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", { name: "Add subculture" }),
    ).toBeDisabled();
  });
});
