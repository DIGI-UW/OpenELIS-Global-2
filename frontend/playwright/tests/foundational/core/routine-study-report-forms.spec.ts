import { test, expect, type Page } from "../../../helpers/test-base";
import { openSideNavItem } from "../../../helpers/sidenav";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Routine and study report forms, reached through the side nav.
 *
 * For every report the configured menu offers, the item opens its form under
 * the right heading, the Generate Printable Version button is gated the way
 * the form gates it (disabled until an input is given, or validated on click),
 * and once the required inputs are given it opens the printable report for
 * that report with those inputs.
 *
 * The printable report itself is not rendered: /ReportPrint is answered with
 * a stub so the check is the form contract (which report, which inputs) and
 * never depends on report data existing on the stack. Menu items that link
 * straight to /ReportPrint (validation backlog, section performance) have no
 * form and are not listed.
 */

type FormKind =
  /** ReportByDate: disabled until a date is given; both dates checked on click. */
  | "dateRange"
  /** ReportByDate with the Search By select (activity reports). */
  | "dateRangeAndValue"
  /** ReportByLabNo: disabled until a lab number is given. */
  | "labNumberRange"
  /** ReportByID: disabled until a national ID is given. */
  | "nationalId"
  /** Always enabled; dates checked on click (IntermediateByService). */
  | "datesCheckedOnClick"
  /** ReportByDateCSV: dates, then study type (and date type) checked on click. */
  | "studyExport"
  /** PatientStatusReport: always enabled, inputs in accordions. */
  | "patientStatus"
  /** NonConformityNotification: always enabled, lab number + site. */
  | "labNumberAndSite"
  /** ReferredOut: always enabled, date range + referral lab. */
  | "referredOut";

interface ReportForm {
  menu: string;
  path: string[];
  heading: string;
  /** `report` parameter the printable version must be opened with. */
  report: string;
  kind: FormKind;
}

const ROUTINE = ["menu_reports", "menu_reports_routine"];
const AGGREGATE = [...ROUTINE, "menu_reports_aggregate"];
const MANAGEMENT = [...ROUTINE, "menu_reports_management"];
const ACTIVITY = [...MANAGEMENT, "menu_reports_activity"];
const ROUTINE_NC = [...MANAGEMENT, "menu_reports_nonconformity"];
const STUDY = ["menu_reports", "menu_reports_study"];
const PATIENTS = [...STUDY, "menu_reports_patients"];
const ARV = [...PATIENTS, "menu_reports_arv"];
const EID = [...PATIENTS, "menu_reports_eid"];
const VL = [...PATIENTS, "menu_reports_vl"];
const INDETERMINATE = [...PATIENTS, "menu_reports_indeterminate"];
const STUDY_NC = [...STUDY, "menu_reports_nonconformity.study"];
const STUDY_EXPORT = [...STUDY, "menu_reports_export"];

const ROUTINE_REPORTS: ReportForm[] = [
  {
    menu: "Patient Status Report",
    path: [...ROUTINE, "menu_reports_status_patient"],
    heading: "Patient Status Report",
    report: "patientCILNSP_vreduit",
    kind: "patientStatus",
  },
  {
    menu: "Summary of All Tests",
    path: [...AGGREGATE, "menu_reports_aggregate_all"],
    heading: "Test Report Summary",
    report: "indicatorHaitiLNSPAllTests",
    kind: "dateRange",
  },
  {
    menu: "HIV Test Summary",
    path: [...AGGREGATE, "menu_reports_aggregate_hiv"],
    heading: "HIV Test Summary",
    report: "indicatorCDILNSPHIV",
    kind: "dateRange",
  },
  {
    menu: "Rejection Report",
    path: [...MANAGEMENT, "menu_reports_management_rejection"],
    heading: "Rejection Report",
    report: "sampleRejectionReport",
    kind: "dateRange",
  },
  {
    menu: "Activity report by test",
    path: [...ACTIVITY, "menu_activity_report_test"],
    heading: "Activity report By test",
    report: "activityReportByTest",
    kind: "dateRangeAndValue",
  },
  {
    menu: "Activity report by panel",
    path: [...ACTIVITY, "menu_activity_report_panel"],
    heading: "Activity report By Panel",
    report: "activityReportByPanel",
    kind: "dateRangeAndValue",
  },
  {
    menu: "Activity report by unit",
    path: [...ACTIVITY, "menu_activity_report_bench"],
    heading: "Activity report By Test Section",
    report: "activityReportByTestSection",
    kind: "dateRangeAndValue",
  },
  {
    menu: "Referred Out Tests Report",
    path: [...MANAGEMENT, "menu_reports_referred"],
    heading: "External Referrals Report",
    report: "referredOut",
    kind: "referredOut",
  },
  {
    menu: "Non-conformity by date",
    path: [...ROUTINE_NC, "menu_reports_nonconformity_date"],
    heading: "Non ConformityReport by Date",
    report: "haitiNonConformityByDate",
    kind: "dateRange",
  },
  {
    menu: "Non-conformity by unit and reason",
    path: [...ROUTINE_NC, "menu_reports_nonconformity_section"],
    heading: "Non Conformity Report by Unit and Reason",
    report: "haitiNonConformityBySectionReason",
    kind: "dateRange",
  },
  {
    menu: "Export Routine CSV file",
    path: [...ROUTINE, "menu_reports_export_routine"],
    heading: "Export Routine CSV file",
    report: "CISampleRoutineExport",
    kind: "dateRange",
  },
];

const STUDY_REPORTS: ReportForm[] = [
  {
    menu: "ARV initial version 1",
    path: [...ARV, "menu_reports_arv_initial1"],
    heading: "ARV-initial",
    report: "patientARVInitial1",
    kind: "labNumberRange",
  },
  {
    menu: "ARV initial version 2",
    path: [...ARV, "menu_reports_arv_initial2"],
    heading: "ARV-initial",
    report: "patientARVInitial2",
    kind: "labNumberRange",
  },
  {
    menu: "ARV follow-up version 1",
    path: [...ARV, "menu_reports_arv_followup1"],
    heading: "ARV-Follow-up",
    report: "patientARVFollowup1",
    kind: "labNumberRange",
  },
  {
    menu: "ARV follow-up version 2",
    path: [...ARV, "menu_reports_arv_followup2"],
    heading: "ARV-Follow-up",
    report: "patientARVFollowup2",
    kind: "labNumberRange",
  },
  {
    menu: "ARV initial, follow-up and VL",
    path: [...ARV, "menu_reports_arv_all"],
    heading: "ARV -->Initial-FollowUp-VL",
    report: "patientARV1",
    kind: "labNumberRange",
  },
  {
    menu: "EID version 1",
    path: [...EID, "menu_reports_eid_version1"],
    heading: "Diagnostic for children with DBS-PCR",
    report: "patientEID1",
    kind: "patientStatus",
  },
  {
    menu: "EID version 2",
    path: [...EID, "menu_reports_eid_version2"],
    heading: "Diagnostic for children with DBS-PCR",
    report: "patientEID2",
    kind: "labNumberRange",
  },
  {
    menu: "VL version 1",
    path: [...VL, "menu_reports_vl_version1"],
    heading: "Viral Load",
    report: "patientVL1",
    kind: "patientStatus",
  },
  {
    menu: "Indeterminate version 1",
    path: [...INDETERMINATE, "menu_reports_indeterminate_version1"],
    heading: "Indeterminate",
    report: "patientIndeterminate1",
    kind: "labNumberRange",
  },
  {
    menu: "Indeterminate version 2",
    path: [...INDETERMINATE, "menu_reports_indeterminate_version2"],
    heading: "Indeterminate",
    report: "patientIndeterminate2",
    kind: "labNumberRange",
  },
  {
    menu: "Indeterminate by service",
    path: [...INDETERMINATE, "menu_reports_indeterminate_location"],
    heading: "Indeterminate",
    report: "patientIndeterminateByLocation",
    kind: "datesCheckedOnClick",
  },
  {
    menu: "Special Request",
    path: [...PATIENTS, "menu_reports_special"],
    heading: "Special Request",
    report: "patientSpecialReport",
    kind: "labNumberRange",
  },
  {
    menu: "Collected ARV Patient Report",
    path: [...PATIENTS, "menu_reports_patient_collection"],
    heading: "Collected ARV Patient Report",
    report: "patientCollection",
    kind: "nationalId",
  },
  {
    menu: "Associated Patient Report",
    path: [...PATIENTS, "menu_reports_patient_associated"],
    heading: "Associated Patient Report",
    report: "patientAssociated",
    kind: "nationalId",
  },
  {
    menu: "Non-conformity by date (study)",
    path: [...STUDY_NC, "menu_reports_nonconformity_date.study"],
    heading: "Non-conformity Report By Date",
    report: "retroCINonConformityByDate",
    kind: "dateRange",
  },
  {
    menu: "Non-conformity by unit and reason (study)",
    path: [...STUDY_NC, "menu_reports_nonconformity_section.study"],
    heading: "Non Conformity Report by Unit and Reason",
    report: "retroCInonConformityBySectionReason",
    kind: "dateRange",
  },
  {
    menu: "Non-conformity by lab number (study)",
    path: [...STUDY_NC, "menu_reports_nonconformity.Labno"],
    // The form is titled with the ARV initial/follow-up message.
    heading: "ARV -->Initial-FollowUp-VL",
    report: "retroCINonConformityByLabno",
    kind: "labNumberRange",
  },
  {
    menu: "Non-conformity notification (study)",
    path: [...STUDY_NC, "menu_reports_nonconformity_notification.study"],
    heading: "Non-conformity notification",
    report: "retroCInonConformityNotification",
    kind: "labNumberAndSite",
  },
  {
    menu: "Follow-up required by location (study)",
    path: [...STUDY_NC, "menu_reports_followupRequired_ByLocation.study"],
    heading: "Follow-up Required",
    report: "retroCIFollowupRequiredByLocation",
    kind: "dateRange",
  },
  {
    menu: "General export by date (study)",
    path: [...STUDY_EXPORT, "menu_reports_export_general"],
    heading: "Export a CSV File by Date",
    report: "CIStudyExport",
    kind: "studyExport",
  },
  {
    menu: "Specific export by date (study)",
    path: [...STUDY_EXPORT, "menu_reports_export_specific"],
    heading: "Viral Load Data Export Report",
    report: "Trends",
    kind: "studyExport",
  },
];

const GENERATE = "Generate Printable Version";
const REPORT_PRINT_PATH = "/api/OpenELIS-Global/ReportPrint";
// Shaped like an accession; the stubbed report never looks it up.
const LAB_NUMBER = "DEV01269999999999999";
const NATIONAL_ID = "NID-FORM-CONTRACT";

/** Today on the server, in the site's date format. */
async function serverToday(page: Page): Promise<string> {
  const api = "/api/OpenELIS-Global/rest";
  const [time, config] = await Promise.all([
    page.request.get(`${api}/server-time`).then((r) => r.json()),
    page.request.get(`${api}/configuration-properties`).then((r) => r.json()),
  ]);
  const [y, m, d] = (time.date as string).split("-");
  return String(config.DEFAULT_DATE_LOCALE || "fr-FR").startsWith("en")
    ? `${m}/${d}/${y}`
    : `${d}/${m}/${y}`;
}

/** Popups opened so far; a click that must not open the report checks it. */
function countPopups(page: Page) {
  const opened: Page[] = [];
  page.on("popup", (popup) => opened.push(popup));
  return opened;
}

/** Click Generate and return the printable-report URL the new tab opens. */
async function generate(page: Page): Promise<URL> {
  const popupPromise = page.waitForEvent("popup");
  await page.getByRole("button", { name: GENERATE }).click();
  const popup = await popupPromise;
  await popup.waitForURL(/\/ReportPrint\?/);
  const url = new URL(popup.url());
  await popup.close();
  return url;
}

async function fillDate(page: Page, id: "startDate" | "endDate", date: string) {
  const input = page.locator(`input#${id}`);
  await input.fill(date);
  await input.press("Escape");
  await expect(input).toHaveValue(date);
}

async function openReport(page: Page, form: ReportForm) {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  await openSideNavItem(page, form.path);
  await expect(page).toHaveURL(
    new RegExp(`/Report\\?type=\\w+&report=${form.report}$`),
    { timeout: NAV_TIMEOUT },
  );
  await expect(
    page.getByRole("main").getByRole("heading", {
      name: form.heading,
      exact: true,
    }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

function expectReport(url: URL, form: ReportForm) {
  expect(url.pathname).toBe(REPORT_PRINT_PATH);
  expect(url.searchParams.get("report")).toBe(form.report);
}

/**
 * Exercise one form's contract, from empty inputs to an opened report, and
 * return the printable-report URL it opened.
 */
async function exerciseForm(page: Page, form: ReportForm): Promise<URL> {
  const button = page.getByRole("button", { name: GENERATE });
  const popups = countPopups(page);
  const today = await serverToday(page);

  switch (form.kind) {
    case "dateRange":
    case "dateRangeAndValue": {
      await expect(button).toBeDisabled();
      await fillDate(page, "startDate", today);
      await expect(button).toBeEnabled();
      // One date enables the button; the range is checked on click.
      await button.click();
      await expect(
        page.getByText("Please select Start and end date."),
      ).toBeVisible();
      expect(popups).toHaveLength(0);
      await fillDate(page, "endDate", today);
      if (form.kind === "dateRangeAndValue") {
        await button.click();
        await expect(page.getByText("Please select a value.")).toBeVisible();
        expect(popups).toHaveLength(0);
        const select = page.getByLabel("Search By", { exact: true });
        const value = await select
          .locator("option")
          .evaluateAll(
            (options) =>
              options
                .map((o) => (o as HTMLOptionElement).value)
                .find(Boolean) ?? "",
          );
        expect(value, "the Search By list has no entries").not.toBe("");
        await select.selectOption(value);
        const url = await generate(page);
        expect(url.searchParams.get("selectList.selection")).toBe(value);
        expect(url.searchParams.get("lowerDateRange")).toBe(today);
        expect(url.searchParams.get("upperDateRange")).toBe(today);
        return url;
      }
      const url = await generate(page);
      expect(url.searchParams.get("lowerDateRange")).toBe(today);
      expect(url.searchParams.get("upperDateRange")).toBe(today);
      return url;
    }

    case "labNumberRange": {
      await expect(button).toBeDisabled();
      await page
        .getByRole("textbox", { name: "From", exact: true })
        .fill(LAB_NUMBER);
      await expect(button).toBeEnabled();
      const url = await generate(page);
      expect(url.searchParams.get("accessionDirect")).toBe(LAB_NUMBER);
      return url;
    }

    case "nationalId": {
      await expect(button).toBeDisabled();
      await page
        .getByRole("textbox", { name: "National ID", exact: true })
        .fill(NATIONAL_ID);
      await expect(button).toBeEnabled();
      const url = await generate(page);
      expect(url.searchParams.get("patientNumberDirect")).toBe(NATIONAL_ID);
      return url;
    }

    case "datesCheckedOnClick": {
      await expect(button).toBeEnabled();
      await button.click();
      await expect(
        page.getByText("Please select Start and end date."),
      ).toBeVisible();
      expect(popups).toHaveLength(0);
      await fillDate(page, "startDate", today);
      await fillDate(page, "endDate", today);
      const url = await generate(page);
      expect(url.searchParams.get("lowerDateRange")).toBe(today);
      expect(url.searchParams.get("upperDateRange")).toBe(today);
      return url;
    }

    case "studyExport": {
      await expect(button).toBeEnabled();
      await button.click();
      await expect(
        page.getByText("Please select Start and end date."),
      ).toBeVisible();
      await fillDate(page, "startDate", today);
      await fillDate(page, "endDate", today);
      await button.click();
      await expect(page.getByText("Please select study type.")).toBeVisible();
      expect(popups).toHaveLength(0);
      const studyType = page.locator("select#studyType");
      const study = await studyType
        .locator("option")
        .evaluateAll(
          (options) =>
            options.map((o) => (o as HTMLOptionElement).value).find(Boolean) ??
            "",
        );
      expect(study, "the study type list has no entries").not.toBe("");
      await studyType.selectOption(study);
      if (form.report === "CIStudyExport") {
        await button.click();
        await expect(page.getByText("Please select date type.")).toBeVisible();
        expect(popups).toHaveLength(0);
        await page
          .locator("select#dateType")
          .selectOption({ label: "Order Date" });
      }
      const url = await generate(page);
      expect(url.searchParams.get("lowerDateRange")).toBe(today);
      if (form.report === "CIStudyExport") {
        expect(url.searchParams.get("projectCode")).toBe(study);
        expect(url.searchParams.get("dateType")).toBe("ORDER_DATE");
      } else {
        expect(url.searchParams.get("vlStudyType")).toBe(study);
      }
      return url;
    }

    case "patientStatus": {
      await expect(button).toBeEnabled();
      const byLabNumber = page.getByRole("button", {
        name: "Report By Lab Number",
        exact: true,
      });
      await byLabNumber.click();
      await expect(byLabNumber).toHaveAttribute("aria-expanded", "true");
      await page
        .getByRole("textbox", { name: "From", exact: true })
        .fill(LAB_NUMBER);
      const url = await generate(page);
      expect(url.searchParams.get("accessionDirect")).toBe(LAB_NUMBER);
      return url;
    }

    case "labNumberAndSite": {
      await expect(button).toBeEnabled();
      await page
        .getByRole("textbox", { name: "From", exact: true })
        .fill(LAB_NUMBER);
      const url = await generate(page);
      expect(url.searchParams.get("accessionDirect")).toBe(LAB_NUMBER);
      return url;
    }

    case "referredOut": {
      await expect(button).toBeEnabled();
      await fillDate(page, "startDate", today);
      await fillDate(page, "endDate", today);
      const url = await generate(page);
      expect(url.searchParams.get("lowerDateRange")).toBe(today);
      expect(url.searchParams.get("upperDateRange")).toBe(today);
      return url;
    }
  }
}

test.beforeEach(async ({ page }) => {
  await page.context().route("**/api/OpenELIS-Global/ReportPrint?**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<!doctype html><title>report</title>",
    }),
  );
});

for (const [group, forms] of [
  ["Routine reports", ROUTINE_REPORTS],
  ["Study reports", STUDY_REPORTS],
] as const) {
  test.describe(group, () => {
    for (const form of forms) {
      test(`${form.menu} opens its form and generates its report`, async ({
        page,
      }) => {
        await test.step("open from the side nav", () => openReport(page, form));
        const url = await test.step("form contract", () =>
          exerciseForm(page, form));
        expect(url.pathname).toBe(REPORT_PRINT_PATH);
        expect(url.searchParams.get("report")).toBe(form.report);
      });
    }
  });
}

test.describe("Routine reports", () => {
  test("Statistics Report: All follows the individual lab units and priorities", async ({
    page,
  }) => {
    const form: ReportForm = {
      menu: "Statistics Report",
      path: [...AGGREGATE, "menu_reports_aggregate_statistics"],
      heading: "Statistics Report",
      report: "statisticsReport",
      kind: "dateRange",
    };
    await openReport(page, form);

    const allUnits = page.locator("#select-all-lab-units");
    // Each Carbon checkbox sits in its own wrapper inside the group's div.
    const unitGroup = allUnits.locator("xpath=../..");
    const units = unitGroup.locator(
      'input[type="checkbox"]:not(#select-all-lab-units)',
    );

    await test.step("All ticks every lab unit", async () => {
      // At least two units, so one can be unticked with another still ticked.
      await expect(units.nth(1)).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(allUnits).not.toBeChecked();
      await page.locator('label[for="select-all-lab-units"]').click();
      await expect(allUnits).toBeChecked();
      const count = await units.count();
      for (let i = 0; i < count; i++) await expect(units.nth(i)).toBeChecked();
    });

    const removedUnit =
      await test.step("unticking one unit unticks All only", async () => {
        const unit = units.nth(1);
        const id = await unit.getAttribute("id");
        await page.locator(`label[for="${id}"]`).click();
        await expect(unit).not.toBeChecked();
        await expect(allUnits).not.toBeChecked();
        await expect(units.nth(0)).toBeChecked();
        return id;
      });

    const allPriorities = page.locator("#select-all-priorities");
    const priorities = allPriorities
      .locator("xpath=../..")
      .locator('input[type="checkbox"]:not(#select-all-priorities)');

    await test.step("All ticks and unticks every priority", async () => {
      await page.locator('label[for="select-all-priorities"]').click();
      await expect(allPriorities).toBeChecked();
      const count = await priorities.count();
      for (let i = 0; i < count; i++)
        await expect(priorities.nth(i)).toBeChecked();
      await page.locator('label[for="select-all-priorities"]').click();
      await expect(allPriorities).not.toBeChecked();
      for (let i = 0; i < count; i++)
        await expect(priorities.nth(i)).not.toBeChecked();
    });

    await test.step("generate carries the chosen units", async () => {
      const unitCount = await units.count();
      const url = await generate(page);
      expectReport(url, form);
      const sections = url.searchParams.getAll("labSections");
      expect(sections).toHaveLength(unitCount - 1);
      expect(sections).not.toContain(removedUnit);
      expect(url.searchParams.getAll("priority")).toHaveLength(0);
    });
  });
});
