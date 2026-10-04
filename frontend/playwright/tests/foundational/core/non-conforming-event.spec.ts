import { test, expect, Page } from "../../../helpers/test-base";
import {
  RBC_TEST,
  WHOLE_BLOOD,
  firstRealOption,
  labUnitOf,
  orderTests,
  reportNceViaApi,
  serverTodayFor,
} from "../../../helpers/results-nce-ui";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * A non-conforming event is reported against an order's specimen, reviewed
 * (classified and scored) in View NCE, and given corrective actions. Each case
 * orders its own accession, so the NCE it works on is found by that lab
 * number or by the NCE number the server allocated for it.
 */

const posted = (page: Page, path: string) =>
  page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      new URL(response.url()).pathname.endsWith(path),
    { timeout: LONG_TIMEOUT },
  );

/** The success toast dismisses itself, so it is checked softly. */
async function softSaveSuccess(page: Page) {
  await expect
    .soft(
      page.getByText("Save was successful").first(),
      "the save is confirmed",
    )
    .toBeVisible({ timeout: UI_TIMEOUT });
}

async function searchNce(
  page: Page,
  by: "labNumber" | "nceNumber",
  value: string,
) {
  const main = page.getByRole("main");
  const searchBy = main.getByRole("combobox", { name: "Search By" });
  await expect(searchBy).toBeVisible({ timeout: NAV_TIMEOUT });
  await searchBy.selectOption(by);
  await main.getByRole("textbox", { name: "Text Value" }).fill(value);
  await main.getByTestId("nce-search-button").click();
}

test.describe("Non-conforming events", () => {
  test("an NCE reported against a linked specimen is on record for that order", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const reportingUnit = await labUnitOf(page, RBC_TEST);
    const description = `E2E NCE report ${Date.now()}`;
    const main = page.getByRole("main");
    let category = { value: "", label: "" };
    let type = { value: "", label: "" };

    await test.step("classify and describe the event", async () => {
      await page.goto("/ReportNonConformingEvent", {
        waitUntil: "domcontentloaded",
      });
      await expect(main.getByLabel("NCE Number")).toHaveValue(/^NCE-/, {
        timeout: NAV_TIMEOUT,
      });
      await main.getByLabel("Reporting Unit *").selectOption(reportingUnit);
      const categorySelect = main.getByLabel("Category *");
      category = await firstRealOption(categorySelect);
      await categorySelect.selectOption(category.value);
      const typeSelect = main.getByLabel("Subcategory");
      type = await firstRealOption(typeSelect);
      await typeSelect.selectOption(type.value);
      const major = main.locator(".nce-severity-card", { hasText: "Major" });
      await major.click();
      await expect(major).toHaveClass(/selected/);
      await main.getByLabel("Description *").fill(description);
      await expect(main.getByLabel("Description *")).toHaveValue(description);
    });

    await test.step("link the order's specimen", async () => {
      const search = main.locator(".nce-search-section");
      await search
        .getByRole("combobox", { name: "Search By" })
        .selectOption("labNumber");
      await search.getByRole("textbox", { name: "Text Value" }).fill(accession);
      await search.getByRole("button", { name: "Search", exact: true }).click();
      const specimens = main.locator(".nce-search-results");
      const specimen = specimens.getByText(
        new RegExp(`\\(${accession}-\\d+\\)$`),
      );
      await expect(specimen).toHaveCount(1, { timeout: UI_TIMEOUT });
      await specimen.click();
      await expect(specimens.getByRole("checkbox")).toBeChecked();
      await specimens
        .getByRole("button", { name: "Link Selected Samples" })
        .click();
      await expect(
        main
          .locator(".nce-linked-samples")
          .getByText(accession, { exact: true }),
      ).toBeVisible();
    });

    await test.step("submit the report", async () => {
      const reported = posted(page, "/rest/reportnonconformingevent");
      await main.getByRole("button", { name: "Submit NCE" }).click();
      await reported;
      await softSaveSuccess(page);
      await expect(main.getByLabel("Description *")).toHaveValue("", {
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("View NCE finds the event by the order's lab number", async () => {
      await page.goto("/ViewNonConformingEvent", {
        waitUntil: "domcontentloaded",
      });
      await searchNce(page, "labNumber", accession);
      await expect(main.getByTestId("nce-search-result")).toHaveText(
        accession,
        { timeout: UI_TIMEOUT },
      );
      await expect(main.getByTestId("nce-number-result")).toHaveText(/^NCE-/);
      await expect(main.getByText(description, { exact: true })).toBeVisible();
      const saved = (label: string) =>
        main
          .getByText(label, { exact: true })
          .locator("xpath=ancestor::div[2]");
      await expect(saved("Severity")).toContainText("Major");
      await expect(saved("Category")).toContainText(category.label);
      await expect(saved("Type")).toContainText(type.label);
      await expect(
        main.getByRole("combobox", { name: "NCE Category" }),
      ).toHaveValue(category.value);
      await expect(
        main.getByRole("combobox", { name: "NCE Type" }),
      ).toHaveValue(type.value);
    });
  });

  test("reviewing an NCE in View NCE records its classification and moves it to corrective action", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const nceNumber = await reportNceViaApi(
      page,
      accession,
      `E2E NCE review ${Date.now()}`,
    );
    const main = page.getByRole("main");
    let category = { value: "", label: "" };
    let type = { value: "", label: "" };
    let component = { value: "", label: "" };

    await test.step("open the pending NCE by its number", async () => {
      await page.goto("/ViewNonConformingEvent", {
        waitUntil: "domcontentloaded",
      });
      await searchNce(page, "nceNumber", nceNumber);
      await expect(main.getByTestId("nce-number-result")).toHaveText(
        nceNumber,
        { timeout: UI_TIMEOUT },
      );
      await expect(main.getByTestId("nce-search-result")).toHaveText(accession);
    });

    await test.step("classify and score it", async () => {
      const categorySelect = main.getByRole("combobox", {
        name: "NCE Category",
      });
      category = await firstRealOption(categorySelect);
      await categorySelect.selectOption(category.value);
      const typeSelect = main.getByRole("combobox", { name: "NCE Type" });
      type = await firstRealOption(typeSelect);
      await typeSelect.selectOption(type.value);
      const consequences = main.locator("#consequences");
      const recurrence = main.locator("#recurrence");
      const consequence = await consequences
        .locator("option")
        .last()
        .getAttribute("value");
      const likelihood = await recurrence
        .locator("option")
        .last()
        .getAttribute("value");
      await consequences.selectOption(consequence!);
      await recurrence.selectOption(likelihood!);
      const componentSelect = main.locator("#labComponent");
      component = await firstRealOption(componentSelect);
      await componentSelect.selectOption(component.value);
      const score = main
        .getByText("Severity Score", { exact: true })
        .locator("xpath=ancestor::div[2]");
      await expect(score).toContainText(
        String(Number(consequence) * Number(likelihood)),
      );
    });

    await test.step("submit the review", async () => {
      const submit = main.getByTestId("nce-submit-button");
      await expect(submit).toBeEnabled();
      const reviewed = posted(page, "/rest/viewNonConformEvents");
      await submit.click();
      await reviewed;
      await softSaveSuccess(page);
      await expect(main.getByTestId("nce-number-result")).toHaveCount(0);
    });

    await test.step("Corrective Action lists it with the review's classification", async () => {
      await page.goto("/NCECorrectiveAction", {
        waitUntil: "domcontentloaded",
      });
      await searchNce(page, "labNumber", accession);
      await expect(main.getByTestId("nce-number-result")).toHaveText(
        nceNumber,
        { timeout: UI_TIMEOUT },
      );
      await expect(main).toContainText(component.label);
      await expect(main).toContainText(category.label);
      await expect(main).toContainText(type.label);
    });
  });

  test("a corrective action and discussion date saved for an NCE are on its record", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const nceNumber = await reportNceViaApi(
      page,
      accession,
      `E2E NCE action ${Date.now()}`,
    );
    const action = `Recalibrate and repeat ${Date.now()}`;
    const owner = `E2E owner ${Date.now()}`;
    const main = page.getByRole("main");
    let today = "";
    // Discussion dates render as paragraphs; the event and report dates as divs.

    await test.step("open the NCE by its number", async () => {
      await page.goto("/NCECorrectiveAction", {
        waitUntil: "domcontentloaded",
      });
      await searchNce(page, "nceNumber", nceNumber);
      await expect(main.getByTestId("nce-number-result")).toHaveText(
        nceNumber,
        { timeout: UI_TIMEOUT },
      );
    });

    await test.step("add a discussion date", async () => {
      const discussed = main.getByRole("textbox", {
        name: "Date of discussion with NCE Staff",
      });
      today = await serverTodayFor(discussed);
      await discussed.fill(today);
      await page.keyboard.press("Escape");
      await main.getByRole("button", { name: "Add new date" }).click();
      await expect(
        main.getByRole("paragraph").filter({ hasText: today }),
      ).toBeVisible();
    });

    await test.step("fill and submit the corrective action", async () => {
      await main.locator("#text-area-corrective").fill(action);
      await main.locator("#text-area-person").fill(owner);
      await main.locator("#dateCompleted").fill(today);
      await page.keyboard.press("Escape");
      await main.locator('label[for="correctiveAction"]').click();
      await expect(main.getByTestId("nce-action-checkbox")).toBeChecked();
      const recorded = posted(page, "/rest/NCECorrectiveAction");
      await main.getByRole("button", { name: "Submit", exact: true }).click();
      await recorded;
      await softSaveSuccess(page);
      await expect(main.getByTestId("nce-number-result")).toHaveCount(0);
    });

    await test.step("the NCE's record shows the saved action and date", async () => {
      await page.goto(
        `/NCECorrectiveAction?nceNumber=${encodeURIComponent(nceNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(main.getByTestId("nce-number-result")).toHaveText(
        nceNumber,
        { timeout: NAV_TIMEOUT },
      );
      const savedLog = main.locator("textarea[disabled]");
      await expect(savedLog.filter({ hasText: action })).toHaveValue(action);
      await expect(savedLog.filter({ hasText: owner })).toHaveValue(owner);
      await expect(
        main.getByRole("paragraph").filter({ hasText: today }),
      ).toBeVisible();
    });
  });
});
