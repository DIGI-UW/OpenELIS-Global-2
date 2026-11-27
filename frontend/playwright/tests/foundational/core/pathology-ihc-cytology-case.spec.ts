import {
  test,
  expect,
  type Locator,
  type Page,
} from "../../../helpers/test-base";
import { createProgramOrder } from "../../../helpers/program-order";
import {
  moveCaseToStage,
  readCase,
  saveCaseThroughApi,
  type PathologyStageId,
} from "../../../helpers/seed-pathology-data";
import {
  expectStage as expectSummaryStage,
  pickerOptions,
  STAGE_LABELS,
} from "../../../helpers/pathology-case-view";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Program case workflow on the Pathology, Immunohistochemistry and Cytology
 * dashboards.
 *
 * An order placed under the program creates the case the program's dashboard
 * lists. From the dashboard the case opens in its case view, where the bench
 * sets the stage, the assigned technician and the assigned (cyto)pathologist
 * and saves. Reloading the case view must show what was saved, the dashboard
 * row must carry the new stage, and completing the case (ticking "Ready For
 * release" and saving on Immunohistochemistry and Cytology, signing it out
 * from the action bar on Pathology) lands it at Completed.
 *
 * Each test seeds its own order with a per-run patient name and works only on
 * that case, so dashboards holding other cases do not affect it.
 *
 * Immunohistochemistry and Cytology use native selects and one save button.
 * Pathology runs on the case-view shell: the case is sent on one stage at a
 * time, each move doing the bench's work for that stage first, so the test
 * takes the case to the stage it needs through the same move endpoints the
 * screen posts to and reads the case again; the assignment controls sit in
 * accordion sections that unlock with the stage and are expanded before use,
 * the case is saved with Save draft and signed out from the action bar. Each
 * program
 * names the screen driver that knows which of the two it is talking to.
 */

interface CaseProgram {
  name: string;
  program: string;
  sampleType: string;
  dashboardPath: string;
  caseViewPath: RegExp;
  heading: string;
  /**
   * The accessible name of the stage select, on the screens that have one.
   * The case-view shell has none: its case is sent on with buttons.
   */
  statusLabel?: string;
  pathologistLabel: string;
  /**
   * A stage the case view offers, by its label, at which the pathologist can
   * be assigned.
   */
  workingStage: string;
  screen: CaseScreen;
}

/** How a test drives one program's case view. */
interface CaseScreen {
  setStage(page: Page, program: CaseProgram, label: string): Promise<void>;
  expectStage(page: Page, program: CaseProgram, label: string): Promise<void>;
  /** Bring the case to where a pathologist can be assigned and sign it out. */
  reachPathologist(page: Page, program: CaseProgram): Promise<void>;
  technician(page: Page): Promise<Locator>;
  pathologist(page: Page, program: CaseProgram): Promise<Locator>;
  /** Assign the first pathologist offered; returns the name chosen. */
  choosePathologist(page: Page, program: CaseProgram): Promise<string>;
  /** The pathologist control shows `name` as the one assigned. */
  expectPathologist(
    page: Page,
    program: CaseProgram,
    name: string,
  ): Promise<void>;
  /** Complete the case the way the screen does, and wait for its save. */
  complete(page: Page, program: CaseProgram): Promise<void>;
  saveButton(page: Page): Locator;
  /**
   * Registered before the save is clicked and awaited after the toast: a
   * response the screen waits for once it has saved, so that a test does not
   * navigate away while that request is still in flight.
   */
  afterSave?(page: Page): Promise<unknown>;
}

/** The stage select of a legacy case view, by its accessible name. */
function stageSelect(page: Page, program: CaseProgram) {
  if (!program.statusLabel) {
    throw new Error(`${program.name} names no stage select`);
  }
  return page.getByLabel(program.statusLabel, { exact: true });
}

/** The legacy case views: native selects, every control always in reach. */
const nativeSelectScreen: CaseScreen = {
  async setStage(page, program, label) {
    await stageSelect(page, program).selectOption({ label });
  },
  async expectStage(page, program, label) {
    await expect(
      stageSelect(page, program).locator("option:checked"),
    ).toHaveText(label, { timeout: UI_TIMEOUT });
  },
  async reachPathologist() {
    // The pathologist and release are offered at any stage.
  },
  async technician(page) {
    return page.locator("select#assignedTechnician");
  },
  async pathologist(page, program) {
    return page.getByLabel(program.pathologistLabel, { exact: true });
  },
  async choosePathologist(page, program) {
    const pathologist = await this.pathologist(page, program);
    const name = await firstOptionLabel(pathologist);
    await pathologist.selectOption({ label: name });
    return name;
  },
  async expectPathologist(page, program, name) {
    await expect(
      (await this.pathologist(page, program)).locator("option:checked"),
    ).toHaveText(name);
  },
  // Offered only once both a technician and a pathologist are assigned.
  async complete(page, program) {
    const release = page.getByRole("checkbox", { name: "Ready For release" });
    await expect(release).toBeVisible({ timeout: UI_TIMEOUT });
    await page.locator('label[for="release"]').click();
    await expect(release).toBeChecked();
    await saveCase(page, program);
  },
  saveButton(page) {
    return page.locator("#pathology_save2");
  },
};

/**
 * Expand one accordion section of the shell, addressed by its item id. A
 * section unlocks as soon as the stage reaches it, but keeps the open state
 * it had when the screen mounted, so it is opened here when it is shut.
 */
async function expandSection(page: Page, sectionId: string) {
  const header = page.locator(`#${sectionId} button.cds--accordion__heading`);
  await expect(header).toBeEnabled({ timeout: UI_TIMEOUT });
  if ((await header.getAttribute("aria-expanded")) === "false") {
    await header.click();
  }
  await expect(header).toHaveAttribute("aria-expanded", "true");
}

/** The pathology case view on the case-view shell. */
const caseViewShellScreen: CaseScreen = {
  async setStage(page, _program, label) {
    const stageId = (Object.keys(STAGE_LABELS) as PathologyStageId[]).find(
      (id) => STAGE_LABELS[id] === label,
    );
    if (!stageId) {
      throw new Error(`No pathology stage is labelled "${label}"`);
    }
    const caseId = /\/PathologyCaseView\/(\d+)/.exec(page.url())?.[1];
    if (!caseId) {
      throw new Error(`Not on a pathology case: ${page.url()}`);
    }
    await moveCaseToStage(page, { pathologySampleId: caseId }, stageId);
    await page.reload({ waitUntil: "domcontentloaded" });
    await expectSummaryStage(page, label);
  },
  async expectStage(page, _program, label) {
    await expectSummaryStage(page, label);
  },
  // Findings open at the stage the findings are written at: Under
  // Pathologist Review where the laboratory uses it, which the move begins.
  // A sign-out needs both descriptions and a conclusion, so they are written
  // through the case save before the screen is read again.
  async reachPathologist(page, program) {
    const caseId = /\/PathologyCaseView\/(\d+)/.exec(page.url())?.[1];
    if (!caseId) {
      throw new Error(`Not on a pathology case: ${page.url()}`);
    }
    const readingStage =
      (await readCase(page, caseId)).readingStage ?? "READY_PATHOLOGIST";
    await this.setStage(page, program, STAGE_LABELS[readingStage]);
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      grossExam: "Skin, punch biopsy, 4 mm.",
      microscopyExam: "Unremarkable epidermis and dermis.",
      conclusionText: "Benign skin.",
    });
    await page.reload({ waitUntil: "domcontentloaded" });
    await expectSummaryStage(page, STAGE_LABELS[readingStage]);
  },
  async technician(page) {
    await expandSection(page, "pathology-section-grossing");
    return page.locator("select#assignedTechnician");
  },
  async pathologist(page, program) {
    await expandSection(page, "pathology-section-review");
    // The picker's label names its list as well as its box.
    return page.getByRole("combobox", {
      name: program.pathologistLabel,
      exact: true,
    });
  },
  // A typed picker: the first name it offers is typed in and picked.
  async choosePathologist(page, program) {
    const pathologist = await this.pathologist(page, program);
    const [name] = await pickerOptions(pathologist);
    if (!name) {
      throw new Error("The stack offers no pathologist to assign");
    }
    await chooseCarbonOption(pathologist, name);
    return name;
  },
  async expectPathologist(page, program, name) {
    await expect(await this.pathologist(page, program)).toHaveValue(name);
  },
  // The sign-out sends the form as it stands, the pathologist just chosen
  // included. It asks first, then opens the final report in a tab of its
  // own, which is closed here.
  async complete(page) {
    const signOut = page.locator(
      ".case-view__action-bar-actions .cds--btn--primary",
    );
    await expect(signOut).toHaveText("Sign out & finalize");
    await expect(signOut).toBeEnabled({ timeout: UI_TIMEOUT });
    await signOut.click();
    const reportTab = page.context().waitForEvent("page", {
      timeout: UI_TIMEOUT,
    });
    const readBack = this.afterSave!(page);
    await page
      .getByTestId("sign-out-confirm")
      .getByRole("button", { name: "Sign out", exact: true })
      .click();
    await (await reportTab).close();
    await expect(
      page
        .locator(".cds--toast-notification")
        .filter({ hasText: "Case signed out" }),
    ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
    await readBack;
  },
  saveButton(page) {
    return page.getByRole("button", { name: "Save draft" });
  },
  // The shell reads the saved case back after every save; a reload or a
  // dashboard visit before that read lands aborts it and logs a fetch error.
  afterSave(page) {
    return page.waitForResponse(
      (r) => r.url().includes("/caseView/") && r.request().method() === "GET",
    );
  },
};

const PROGRAMS: CaseProgram[] = [
  {
    name: "Pathology",
    program: "Histopathology",
    sampleType: "Histopathology specimen",
    dashboardPath: "/PathologyDashboard",
    caseViewPath: /\/PathologyCaseView\/\d+$/,
    heading: "Pathology",
    pathologistLabel: "Select Pathologist",
    workingStage: "Ready for Pathologist",
    screen: caseViewShellScreen,
  },
  {
    name: "Immunohistochemistry",
    program: "Immunohistochemistry",
    sampleType: "Immunohistochemistry specimen",
    dashboardPath: "/ImmunohistochemistryDashboard",
    caseViewPath: /\/ImmunohistochemistryCaseView\/\d+$/,
    heading: "Immunohistochemistry",
    statusLabel: "Select Status",
    pathologistLabel: "Select Pathologist",
    workingStage: "Ready for Pathologist",
    screen: nativeSelectScreen,
  },
  {
    name: "Cytology",
    program: "Cytology",
    sampleType: "Fluid",
    dashboardPath: "/CytologyDashboard",
    caseViewPath: /\/CytologyCaseView\/\d+$/,
    heading: "Cytology",
    statusLabel: "Status",
    pathologistLabel: "CytoPathologist Assigned",
    workingStage: "Screening",
    screen: nativeSelectScreen,
  },
];

/** Patient names reject digits, so the per-run tag is spelled in letters. */
const runTag = () =>
  `${Date.now()}${Math.floor(Math.random() * 1e3)}`
    .split("")
    .map((digit) => String.fromCharCode(97 + Number(digit)))
    .join("");

async function seedCase(page: Page, program: CaseProgram) {
  const lastName = `Case${runTag()}`;
  const { labNo } = await createProgramOrder(page, {
    program: program.program,
    sampleType: program.sampleType,
    patient: { firstName: program.name, lastName },
  });
  return { labNo, patientName: `${lastName} ${program.name}` };
}

async function findOnDashboard(
  page: Page,
  program: CaseProgram,
  labNo: string,
) {
  await page.goto(program.dashboardPath, { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { name: program.heading, exact: true }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  // Each dashboard opens on its own in-progress grouping, which does not hold
  // every stage (IHC's is the IN_PROGRESS stage alone), so widen to All.
  const stageFilter = page.locator("select#statusFilter");
  await expect(stageFilter.locator('option[value="All"]')).toHaveCount(1, {
    timeout: NAV_TIMEOUT,
  });
  // The dashboard's first load can land after the pick and put its own
  // grouping back, so the pick is repeated until it holds.
  await expect(async () => {
    await stageFilter.selectOption("All");
    await expect(stageFilter).toHaveValue("All", { timeout: 1000 });
  }).toPass({ timeout: NAV_TIMEOUT });
  await page
    .getByRole("searchbox", { name: "Search by LabNo or Family Name" })
    .fill(labNo);
  const row = page.getByRole("row").filter({ hasText: labNo });
  await expect(row).toHaveCount(1, { timeout: LONG_TIMEOUT });
  return row;
}

async function openCase(
  page: Page,
  program: CaseProgram,
  labNo: string,
  patientName: string,
) {
  const row = await findOnDashboard(page, program, labNo);
  await row.getByRole("cell", { name: labNo, exact: true }).click();
  await expect(page).toHaveURL(program.caseViewPath, { timeout: NAV_TIMEOUT });
  await expectCaseLoaded(page, patientName);
}

/**
 * The case view is ready once the loaded case fills the patient header, which
 * prints "<last> <first>".
 */
async function expectCaseLoaded(page: Page, patientName: string) {
  await expect(page.getByText(patientName, { exact: true })).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
}

/** The label of the first real option, skipping the empty placeholder. */
async function firstOptionLabel(select: Locator) {
  const label = await select.locator("option").evaluateAll(
    (options) =>
      (options as HTMLOptionElement[])
        .filter((o) => o.value !== "")
        .map((o) => o.textContent?.trim() ?? "")
        .find((text) => text.length > 0) ?? "",
  );
  if (!label) throw new Error("The select offers no named option");
  return label;
}

/** Save, then read the outcome off the toast the save raises. */
async function saveCase(page: Page, program: CaseProgram) {
  const outcome = page
    .locator(".cds--toast-notification")
    .filter({ hasText: /Successfully saved|Error While saving/ });
  await expect(outcome).toHaveCount(0);
  const response = page.waitForResponse(
    (r) => r.url().includes("/caseView/") && r.request().method() === "POST",
  );
  const followUp = program.screen.afterSave?.(page);
  await program.screen.saveButton(page).click();
  await response;
  await expect(outcome).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  // Toasts dismiss themselves, so read them at once rather than waiting for
  // a failure one to go away.
  for (const text of await outcome.allTextContents()) {
    expect(text).toContain("Successfully saved");
  }
  await followUp;
}

/**
 * Set the working stage, a technician other than the preassigned signed-in
 * user, and the first pathologist; save. Returns what was chosen.
 */
async function assignAndSave(page: Page, program: CaseProgram) {
  const { screen } = program;
  await screen.setStage(page, program, program.workingStage);

  const technician = await screen.technician(page);
  const preassigned = await technician.inputValue();
  const otherTechnician = await technician
    .locator("option")
    .evaluateAll(
      (options, currentId) =>
        options
          .map((o) => (o as HTMLOptionElement).value)
          .find((value) => value && value !== currentId) ?? "",
      preassigned,
    );
  if (!otherTechnician) {
    throw new Error("The stack offers only one technician to assign");
  }
  await technician.selectOption(otherTechnician);
  const pathologistName = await screen.choosePathologist(page, program);

  await saveCase(page, program);
  return { otherTechnician, pathologistName };
}

for (const program of PROGRAMS) {
  test.describe(`${program.name} case view`, () => {
    // Each test seeds an order and walks dashboard, case view and a reload.
    test.slow();

    test(`stage, technician and pathologist saved on a ${program.program} case survive a reload`, async ({
      page,
    }) => {
      const { labNo, patientName } = await test.step("seed the order", () =>
        seedCase(page, program));
      await test.step("open the case from the dashboard", () =>
        openCase(page, program, labNo, patientName));
      const saved = await test.step("assign and save", () =>
        assignAndSave(page, program));

      await test.step("the reloaded case shows what was saved", async () => {
        await page.reload({ waitUntil: "domcontentloaded" });
        await expectCaseLoaded(page, patientName);
        await program.screen.expectStage(page, program, program.workingStage);
        await expect(await program.screen.technician(page)).toHaveValue(
          saved.otherTechnician,
        );
        await program.screen.expectPathologist(
          page,
          program,
          saved.pathologistName,
        );
      });
    });

    test(`the ${program.name} dashboard row shows the saved stage by its label`, async ({
      page,
    }) => {
      const { labNo, patientName } = await test.step("seed the order", () =>
        seedCase(page, program));
      await test.step("set the stage in the case view", async () => {
        await openCase(page, program, labNo, patientName);
        await assignAndSave(page, program);
      });

      await test.step("the dashboard row carries the stage label", async () => {
        const row = await findOnDashboard(page, program, labNo);
        await expect(
          row.getByRole("cell", { name: program.workingStage, exact: true }),
        ).toBeVisible({ timeout: UI_TIMEOUT });
      });
    });

    test(`completing a ${program.name} case lands it at Completed`, async ({
      page,
    }) => {
      const { labNo, patientName } = await test.step("seed the order", () =>
        seedCase(page, program));
      await test.step("assign a pathologist", async () => {
        await openCase(page, program, labNo, patientName);
        await program.screen.reachPathologist(page, program);
        await program.screen.choosePathologist(page, program);
      });

      await test.step("complete the case", () =>
        program.screen.complete(page, program));

      await test.step("the reloaded case is Completed", async () => {
        await page.reload({ waitUntil: "domcontentloaded" });
        await expectCaseLoaded(page, patientName);
        await expect(page).toHaveURL(program.caseViewPath);
        await program.screen.expectStage(page, program, "Completed");
      });
    });
  });
}
