import { test, expect } from "../../../helpers/test-base";
import {
  seedMicrobiologyCase,
  seedMicrobiologyClassificationCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const selectConfiguredOption = async (
  select: import("@playwright/test").Locator,
  fieldName: string,
) => {
  await expect(select).toBeEnabled();
  await expect
    .poll(async () =>
      select
        .locator("option")
        .evaluateAll((options) =>
          options
            .map((option) => (option as HTMLOptionElement).value)
            .filter(Boolean),
        ),
    )
    .not.toEqual([]);
  const values = await select
    .locator("option")
    .evaluateAll((options) =>
      options
        .map((option) => (option as HTMLOptionElement).value)
        .filter(Boolean),
    );
  if (!values[0]) {
    throw new Error(`${fieldName} has no configured options`);
  }
  await select.selectOption(values[0]);
};

test.describe("Microbiology case workbench", () => {
  test("drives a received culture through positive signal from the worklist", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyCase(page);
    const worklistUrl = `/Microbiology/worklist?q=${encodeURIComponent(
      seeded.accessionNumber,
    )}&sort=newest`;
    await page.goto(worklistUrl, { waitUntil: "domcontentloaded" });

    const row = page.getByTestId(`microbiology-worklist-row-${seeded.caseId}`);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText(seeded.accessionNumber);
    await row.getByRole("link", { name: seeded.accessionNumber }).click();

    await expect(page).toHaveURL(
      new RegExp(
        `/Microbiology/cases/${seeded.caseId}\\?q=${seeded.accessionNumber}&sort=newest&section=setup$`,
      ),
    );
    await expect(
      page.getByTestId("microbiology-current-step-action"),
    ).toContainText("Inoculation");
    const caseHeader = page.locator("header");
    await expect(caseHeader.getByTitle("Received")).toBeVisible();

    await page.getByRole("button", { name: "Start inoculation" }).click();
    await page.getByLabel("Bottle or plate ID").fill("UAT-M04-PRIMARY-01");
    await page.getByLabel("Media or bottle").fill("Blood culture bottle");
    await page.getByLabel("Incubation").fill("35 C for 24 hours");
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
    await expect(
      page.getByRole("cell", { name: "UAT-M04-PRIMARY-01" }),
    ).toBeVisible();
    const lotHistory = page.getByRole("table", {
      name: "Recorded lot usage",
    });
    await expect(lotHistory).toContainText("UAT-MICRO-MEDIA-FEFO");

    await page.getByRole("button", { name: "Add subculture" }).click();
    await page.getByLabel("Parent media").selectOption({
      label: "UAT-M04-PRIMARY-01 - Blood culture bottle",
    });
    await page.getByLabel("Bottle or plate ID").fill("UAT-M04-SUB-01");
    await page.getByLabel("Media or bottle").fill("MacConkey agar");
    await page.getByRole("button", { name: "Save media" }).click();
    await expect(
      page.getByRole("cell", { name: "UAT-M04-SUB-01" }),
    ).toBeVisible();
    await expect(
      page.getByRole("cell", { name: /Subculture UAT-M04-PRIMARY-01/ }),
    ).toBeVisible();

    const caseView = page.getByTestId("microbiology-case-view");
    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    const timeline = page.getByTestId("microbiology-timeline-card");
    await expect(
      timeline.getByText("Inoculation Recorded", { exact: true }),
    ).toBeVisible();
    await expect(
      timeline.getByText("Subculture Recorded", { exact: true }),
    ).toBeVisible();
    await expect(timeline.getByText("Auto", { exact: true })).toHaveCount(3);
    await page.getByRole("button", { name: "Add note" }).click();
    await page
      .getByLabel("Note or observation")
      .fill("Colonies visible at 18 hours");
    await page.getByRole("button", { name: "Save note" }).click();
    await expect(timeline.getByText("Manual", { exact: true })).toBeVisible();

    await caseView
      .getByRole("button", { name: "Inoculation", exact: true })
      .click();
    const markPositive = page
      .getByTestId("microbiology-case-section-setup")
      .getByRole("button", { name: "Mark positive" });
    await expect(markPositive).toBeVisible();
    await markPositive.click();
    await expect(page).toHaveURL(/section=setup&action=mark-positive$/);
    await page.getByRole("button", { name: "Confirm positive signal" }).click();

    await expect(caseHeader.getByTitle("Positive Signal")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page).toHaveURL(/section=setup$/);
    await expect(
      page.getByText(
        "Positive signal recorded. Subculture the bottle and record the Gram stain.",
      ),
    ).toBeVisible();
  });

  test("records inoculation lineage and completes two-pass isolate identification", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyCase(page);
    if (!seeded.organismId) {
      throw new Error(
        "Case fixture must provide an organism for identification",
      );
    }
    await page.goto(
      `/Microbiology/cases/${seeded.caseId}?workflow=BACTERIOLOGY&sort=newest`,
      {
        waitUntil: "commit",
      },
    );

    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page).toHaveURL(/section=setup/);
    await expect(
      page.getByTestId("microbiology-current-step-action"),
    ).toContainText("Inoculation");
    const caseHeader = page.locator("header");
    const caseView = page.getByTestId("microbiology-case-view");
    await expect(caseHeader.getByTitle("Received")).toBeVisible();

    await caseHeader
      .getByRole("button", { name: "Log critical notification" })
      .click();
    await expect(page).toHaveURL(
      new RegExp(
        `section=critical-communication&action=log-critical&targetType=CASE&targetId=${seeded.caseId}`,
      ),
    );
    await expect(page.getByLabel("Critical result target")).toBeDisabled();
    await expect(page.getByLabel("Target record")).toHaveValue(seeded.caseId);

    await caseView
      .getByRole("button", { name: "Inoculation", exact: true })
      .click();
    await expect(page).toHaveURL(/section=setup/);
    await page.getByRole("button", { name: "Start inoculation" }).click();
    await page.getByLabel("Bottle or plate ID").fill("BOTTLE-001");
    await page.getByLabel("Media or bottle").fill("Blood culture bottle");
    await page.getByLabel("Incubation").fill("35 C for 24 hours");
    await page.getByLabel("Atmosphere").fill("Ambient");
    await page.getByRole("button", { name: "Save media" }).click();
    await expect(caseHeader.getByTitle("Incubating")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByRole("cell", { name: "BOTTLE-001" })).toBeVisible();

    await page.getByRole("button", { name: "Add subculture" }).click();
    await page
      .getByLabel("Parent media")
      .selectOption({ label: "BOTTLE-001 - Blood culture bottle" });
    await page.getByLabel("Bottle or plate ID").fill("PLATE-002");
    await page.getByLabel("Media or bottle").fill("MacConkey agar");
    await page.getByRole("button", { name: "Save media" }).click();
    await expect(page.getByRole("cell", { name: "PLATE-002" })).toBeVisible();
    await expect(
      page.getByRole("cell", { name: /Subculture BOTTLE-001/ }),
    ).toBeVisible();

    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    await expect(page).toHaveURL(/section=timeline/);
    const timeline = page.getByTestId("microbiology-timeline-card");
    await expect(
      timeline.getByText("Inoculation Recorded", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText(/BOTTLE-001 - Blood culture bottle/),
    ).toBeVisible();
    await expect(
      timeline.getByText("Subculture Recorded", { exact: true }),
    ).toBeVisible();
    await expect(page.getByText(/PLATE-002 - MacConkey agar/)).toBeVisible();
    await page.getByRole("button", { name: "Add note" }).click();
    await page
      .getByLabel("Note or observation")
      .fill("Colonies visible at 18 hours");
    await page.getByRole("button", { name: "Save note" }).click();
    await expect(page.getByText("Colonies visible at 18 hours")).toBeVisible();
    await expect(timeline.getByText("Manual", { exact: true })).toBeVisible();

    await caseView
      .getByRole("button", { name: "Isolates", exact: true })
      .click();
    await expect(page).toHaveURL(/section=isolates/);
    await page.getByLabel("Gram stain").fill("Gram negative rods");
    await page
      .getByLabel("Colony morphology")
      .fill("Lactose fermenting colonies");
    await page.getByRole("button", { name: "Create isolate" }).click();
    await expect(page.getByText("Identification pending")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page
        .getByTestId("microbiology-isolates-card")
        .getByText("Gram negative rods", { exact: true }),
    ).toBeVisible();
    await caseView
      .getByRole("button", { name: "Manual AST", exact: true })
      .click();
    await expect(page).toHaveURL(/section=ast/);
    await expect(
      page.getByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();

    await caseView
      .getByRole("button", { name: "Isolates", exact: true })
      .click();

    await page.getByRole("button", { name: "Identify organism" }).click();
    await page.getByLabel("Organism").selectOption(seeded.organismId);
    await page.getByLabel("ID method").selectOption("MALDI_TOF");
    await page.getByLabel("ID confidence (%)").fill("99.5");
    await page.getByRole("button", { name: "Save identification" }).click();
    await expect(page.getByText("Identified", { exact: true })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByText(/MALDI-TOF.*99\.5%/)).toBeVisible();
    await caseView
      .getByRole("button", { name: "Manual AST", exact: true })
      .click();
    await expect(
      page.getByRole("button", { name: "Start AST run" }),
    ).toBeEnabled();

    await caseView
      .getByRole("button", { name: "Isolates", exact: true })
      .click();

    await page
      .getByRole("button", {
        name: "Log critical notification for ISO-1",
      })
      .click();
    await expect(page).toHaveURL(
      /section=critical-communication&action=log-critical&targetType=ISOLATE&targetId=/,
    );
    await expect(page.getByLabel("Critical result target")).toHaveValue(
      "ISOLATE",
    );
    await expect(page.getByLabel("Critical result target")).toBeDisabled();
    await expect(page.getByLabel("Target record")).toBeDisabled();

    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    await expect(page).toHaveURL(/section=timeline/);
    await expect(page.getByText(/Isolate Created/)).toBeVisible();
    await expect(page.getByText(/Isolate Updated/)).toBeVisible();
  });

  test("classifies unassigned work before profile-specific actions", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyClassificationCase(page);
    if (!seeded.siblingCaseId || !seeded.methodId) {
      throw new Error(
        "R1 classification fixture must provide a sibling case and compatible culture method",
      );
    }
    await page.goto(
      `/Microbiology/cases/${seeded.caseId}?workflow=UNASSIGNED&sort=newest&section=ast`,
      { waitUntil: "commit" },
    );

    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page).toHaveURL(
      new RegExp(
        `/Microbiology/cases/${seeded.caseId}\\?workflow=UNASSIGNED&sort=newest&section=case-info$`,
      ),
    );
    await expect(
      page.getByText("Workflow classification required"),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Inoculation", exact: true }),
    ).toBeDisabled();
    await expect(page.getByLabel("Bacteriology (Received)")).toHaveAttribute(
      "href",
      new RegExp(`/Microbiology/cases/${seeded.siblingCaseId}`),
    );

    await page
      .getByLabel("Workflow", { exact: true })
      .selectOption("MYCOBACTERIOLOGY_TB");
    await expect(
      page.getByLabel("Culture Method", { exact: true }),
    ).toBeEnabled();
    await page
      .getByLabel("Culture Method", { exact: true })
      .selectOption(seeded.methodId);
    await page
      .getByLabel("Reason for change")
      .fill("Corrected during accession review");
    await page.getByRole("button", { name: "Apply workflow" }).click();

    await expect(
      page.getByText("Workflow classification required"),
    ).toBeHidden();
    await expect(
      page.locator("header").getByText("Mycobacteriology/TB"),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Inoculation", exact: true }),
    ).toBeEnabled();
    await page.getByRole("button", { name: "Timeline", exact: true }).click();
    await expect(page).toHaveURL(/section=timeline/);
    const timeline = page.getByTestId("microbiology-timeline-card");
    await expect(
      timeline.getByText(/Corrected during accession review/),
    ).toBeVisible();
    await expect(
      timeline.getByText("Workflow Changed", { exact: true }),
    ).toBeVisible();
  });

  test("reports an NCE and marks a separate specimen lost", async ({
    page,
  }) => {
    const flagged = await seedMicrobiologyCase(page);
    await page.goto(`/Microbiology/cases/${flagged.caseId}`, {
      waitUntil: "commit",
    });
    const caseHeader = page.locator("header");
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

    const reportNce = caseHeader.getByRole("button", { name: "Report NCE" });
    await reportNce.focus();
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/section=nonconformance&action=report-nce/);
    await expect(
      page.getByRole("heading", { name: "Report nonconformance" }),
    ).toBeFocused();
    const reportPanel = page.getByTestId("microbiology-nce-panel");
    await expect(
      reportPanel.getByRole("heading", { name: "Report nonconformance" }),
    ).toBeVisible();
    await selectConfiguredOption(
      reportPanel.getByLabel("Category"),
      "Category",
    );
    await selectConfiguredOption(
      reportPanel.getByLabel("Reporting unit"),
      "Reporting unit",
    );
    const reportMajor = reportPanel.getByRole("radio", { name: "Major" });
    await reportMajor.focus();
    await page.keyboard.press("Space");
    await expect(reportMajor).toBeChecked();
    await reportPanel
      .getByLabel("Description")
      .fill("Container arrived cracked during receipt");
    const flagOnly = reportPanel.getByRole("radio", { name: "Flag only" });
    await flagOnly.focus();
    await page.keyboard.press("Space");
    await expect(flagOnly).toBeChecked();
    const submitNce = reportPanel.getByRole("button", { name: "Report NCE" });
    await submitNce.focus();
    await page.keyboard.press("Enter");

    await expect(page).toHaveURL(/section=timeline/);
    await expect(
      page.getByTestId("microbiology-case-section-timeline"),
    ).toBeFocused();
    await expect(
      page.getByText("Container arrived cracked during receipt"),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.getByText("Nonconformance Reported")).toBeVisible();

    const lost = await seedMicrobiologyCase(page);
    await page.goto(`/Microbiology/cases/${lost.caseId}`, {
      waitUntil: "commit",
    });
    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const markLost = page
      .locator("header")
      .getByRole("button", { name: /Mark lost/ });
    await markLost.focus();
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/section=nonconformance&action=mark-lost/);
    await expect(
      page.getByRole("heading", { name: "Mark specimen lost" }),
    ).toBeFocused();
    const lostPanel = page.getByTestId("microbiology-nce-panel");
    await expect(
      lostPanel.getByRole("heading", { name: "Mark specimen lost" }),
    ).toBeVisible();
    await expect(lostPanel.getByLabel("Category")).toBeDisabled();
    await expect(lostPanel.getByLabel("Type")).toBeDisabled();
    await expect(lostPanel.getByLabel("Type")).toHaveValue(/.+/);
    await selectConfiguredOption(
      lostPanel.getByLabel("Reporting unit"),
      "Reporting unit",
    );
    const lostMajor = lostPanel.getByRole("radio", { name: "Major" });
    await lostMajor.focus();
    await page.keyboard.press("Space");
    await expect(lostMajor).toBeChecked();
    await lostPanel
      .getByLabel("Description")
      .fill("Specimen cannot be located after accession");
    await expect(
      lostPanel.getByRole("radio", { name: "Reject affected tests" }),
    ).toBeChecked();
    const submitLost = lostPanel.getByRole("button", { name: "Mark lost" });
    await submitLost.focus();
    await page.keyboard.press("Enter");

    await expect(page).toHaveURL(/section=timeline/);
    await expect(
      page.getByTestId("microbiology-case-section-timeline"),
    ).toBeFocused();
    await expect(
      page.locator("header").getByText("Lost Specimen", { exact: true }),
    ).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByText("Specimen Lost")).toBeVisible();
  });
});
