import { randomUUID } from "crypto";
import type { Page } from "@playwright/test";

const API_PREFIX = "/api/OpenELIS-Global";

export interface SeededMicrobiologyCase {
  accessionNumber: string;
  caseId: string;
  sampleItemId: string;
  sampleId: string;
  patientId: string;
  patientExternalId?: string;
  analysisId: string;
  isolateId?: string;
  astRunId?: string;
  analyzerInstrumentId?: string;
  analyzerCardId?: string;
  siblingCaseId?: string;
  organismId?: string;
  antibioticId?: string;
  astPanelId?: string;
  activeBreakpointStandardId?: string;
  loadedBreakpointStandardId?: string;
  unmappedOrganismId?: string;
  methodId?: string;
  alternateMethodId?: string;
  sampleTypeId?: string;
  cultureTestId?: string;
  tbCultureTestId?: string;
  nonCultureTestId?: string;
}

export interface SeededReviewedMicrobiologyCase extends SeededMicrobiologyCase {
  isolateId: string;
  astRunId: string;
}

export interface SeededAnalyzerReviewMicrobiologyCase extends SeededReviewedMicrobiologyCase {
  analyzerInstrumentId: string;
  analyzerCardId: string;
  organismId: string;
  antibioticId: string;
}

export type SeededMicrobiologyAstWorklistCase = SeededReviewedMicrobiologyCase;

export interface SeededDenseMicrobiologyCase extends SeededMicrobiologyCase {
  isolateIds: string[];
  astReadingCount: number;
  timelineEventCount: number;
}

export interface SeededMicrobiologyReferenceAdmin extends SeededMicrobiologyCase {
  organismId: string;
  antibioticId: string;
  astPanelId: string;
  activeBreakpointStandardId: string;
  loadedBreakpointStandardId: string;
  methodId: string;
}

export interface SeededMicrobiologyWhonetExport extends SeededMicrobiologyReferenceAdmin {
  exportDate: string;
  sampleTypeId: string;
  unmappedOrganismId: string;
}

export type SeededFinalMicrobiologyCase = SeededReviewedMicrobiologyCase;

type MicrobiologyScenario =
  | "CASE"
  | "MVP"
  | "WORKLIST"
  | "M3"
  | "M4"
  | "R1"
  | "AST_REVIEWED"
  | "AST_ANALYZER_REVIEW";

interface MicrobiologyReferenceOption {
  id: string;
  label: string;
  code: string;
}

interface MicrobiologyIsolateFixture {
  id: string;
  isolateLabel: string;
  organismId: string;
  identificationStatus: string;
  significance?: string;
}

interface MicrobiologyCaseFixture {
  closedAt?: number | string;
  finalReleaseState: string;
  isolates: MicrobiologyIsolateFixture[];
  orderDetail?: { patientOrigin?: string };
}

interface MicrobiologyAstRunFixture {
  id: string;
  status: string;
  readings: Array<{ antibioticId: string }>;
}

interface MicrobiologyReleaseFixture {
  closedAt: number | string;
}

interface SampleTypeManagementResponse {
  success: boolean;
  data?: {
    id: string;
    whonetCode?: string;
  };
}

async function getCsrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

async function provisionMicrobiologyScenario(
  page: Page,
  scenario: MicrobiologyScenario,
  scenarioKey = `playwright-${scenario.toLowerCase()}-${randomUUID()}`,
): Promise<SeededMicrobiologyCase> {
  const csrfToken = await getCsrfToken(page);
  const response = await page.request.post(
    `${API_PREFIX}/rest/microbiology/uat/scenarios`,
    {
      data: {
        scenario,
        scenarioKey,
      },
      headers: { "X-CSRF-Token": csrfToken },
    },
  );

  if (!response.ok()) {
    const body = await response.text().catch(() => "");
    throw new Error(
      `Microbiology ${scenario} scenario provisioning failed: HTTP ${response.status()} ${body.slice(0, 500)}`,
    );
  }

  const body = await response.json();
  return {
    caseId: body.caseId,
    sampleItemId: body.sampleItemId,
    sampleId: body.sampleId,
    patientId: body.patientId,
    analysisId: body.analysisId,
    siblingCaseId: body.siblingCaseId,
    ...body,
  };
}

export function seedMicrobiologyCase(
  page: Page,
): Promise<SeededMicrobiologyCase> {
  return provisionMicrobiologyScenario(page, "CASE");
}

export function seedMicrobiologyMvpCase(
  page: Page,
): Promise<SeededMicrobiologyCase> {
  return provisionMicrobiologyScenario(page, "MVP");
}

export function seedMicrobiologyWorklistCase(
  page: Page,
): Promise<SeededMicrobiologyCase> {
  return provisionMicrobiologyScenario(page, "WORKLIST");
}

export function seedMicrobiologyClassificationCase(
  page: Page,
): Promise<SeededMicrobiologyCase> {
  return provisionMicrobiologyScenario(
    page,
    "R1",
    `playwright-r1-classification-${randomUUID()}`,
  );
}

export async function seedMicrobiologyReferenceAdmin(
  page: Page,
): Promise<SeededMicrobiologyReferenceAdmin> {
  const seeded = await provisionMicrobiologyScenario(page, "M3");
  const required = [
    "organismId",
    "antibioticId",
    "astPanelId",
    "activeBreakpointStandardId",
    "loadedBreakpointStandardId",
    "methodId",
    "sampleTypeId",
  ] as const;
  for (const field of required) {
    if (!seeded[field]) {
      throw new Error(`Microbiology M3 scenario is missing ${field}`);
    }
  }
  return seeded as SeededMicrobiologyReferenceAdmin;
}

async function getAstRuns(
  page: Page,
  isolateId: string,
): Promise<MicrobiologyAstRunFixture[]> {
  return requireJsonResponse<MicrobiologyAstRunFixture[]>(
    "Load isolate AST runs",
    await page.request.get(
      `${API_PREFIX}/rest/microbiology/ast/runs?isolateId=${encodeURIComponent(isolateId)}`,
    ),
  );
}

const hasAllAntibiotics = (
  run: MicrobiologyAstRunFixture,
  antibiotics: MicrobiologyReferenceOption[],
) =>
  antibiotics.every((antibiotic) =>
    run.readings.some((reading) => reading.antibioticId === antibiotic.id),
  );

async function ensureReviewedAstIsolate(
  page: Page,
  seeded: SeededMicrobiologyReferenceAdmin,
  organismId: string,
  isolateLabel: string,
  organismText: string,
  antibiotics: MicrobiologyReferenceOption[],
  existing?: MicrobiologyIsolateFixture,
  significance = "CLINICALLY_SIGNIFICANT",
) {
  const headers = { "X-CSRF-Token": await getCsrfToken(page) };
  const isolate =
    existing ||
    (await requireJsonResponse<MicrobiologyIsolateFixture>(
      `Create ${isolateLabel}`,
      await page.request.post(`${API_PREFIX}/rest/microbiology/isolates`, {
        headers,
        data: {
          caseId: seeded.caseId,
          isolateLabel,
          gramStain: "Gram negative rods",
          colonyMorphology: "Lactose fermenting colonies",
          significance,
        },
      }),
    ));
  if (
    isolate.organismId !== organismId ||
    isolate.identificationStatus !== "CONFIRMED" ||
    isolate.significance !== significance
  ) {
    await requireJsonResponse(
      `Confirm ${isolateLabel}`,
      await page.request.put(
        `${API_PREFIX}/rest/microbiology/isolates/${isolate.id}/identification`,
        {
          headers,
          data: {
            organismId,
            preliminaryOrganismText: organismText,
            significance,
            identificationStatus: "CONFIRMED",
            identificationMethod: "MALDI_TOF",
            identificationConfidence: 99.5,
          },
        },
      ),
    );
  }

  const runs = await getAstRuns(page, isolate.id);
  const reviewed = runs.find(
    (candidate) =>
      candidate.status === "REVIEWED" &&
      hasAllAntibiotics(candidate, antibiotics),
  );
  if (reviewed) {
    return;
  }

  const run =
    runs.find((candidate) => candidate.status === "IN_PROGRESS") ||
    (await requireJsonResponse<MicrobiologyAstRunFixture>(
      `Start ${isolateLabel} AST run`,
      await page.request.post(`${API_PREFIX}/rest/microbiology/ast/runs`, {
        headers,
        data: {
          isolateId: isolate.id,
          panelId: seeded.astPanelId,
          breakpointStandardId: seeded.activeBreakpointStandardId,
          panelAdjustmentReason: "Synthetic UAT fixture panel assignment",
          technique: "BROTH_MICRODILUTION",
        },
      }),
    ));
  const recordedAntibiotics = new Set(
    run.readings.map((reading) => reading.antibioticId),
  );
  for (const antibiotic of antibiotics) {
    if (recordedAntibiotics.has(antibiotic.id)) continue;
    await requireJsonResponse(
      `Record ${isolateLabel} ${antibiotic.code}`,
      await page.request.post(
        `${API_PREFIX}/rest/microbiology/ast/runs/${run.id}/readings`,
        {
          headers,
          data: {
            antibioticId: antibiotic.id,
            method: "MIC",
            rawValue: antibiotic.code === "CIPUAT" ? 4 : 32,
          },
        },
      ),
    );
  }
  await requireJsonResponse(
    `Review ${isolateLabel} AST run`,
    await page.request.post(
      `${API_PREFIX}/rest/microbiology/ast/runs/${run.id}/review`,
      { headers, data: {} },
    ),
  );
}

interface WhonetExportScenarioOptions {
  scenarioKey: string;
  patientOrigin?: string;
  specimenWhonetCode?: string;
  unmappedSignificance?: string;
}

async function ensureSampleTypeWhonetCode(
  page: Page,
  sampleTypeId: string,
  whonetCode: string,
): Promise<void> {
  const current = await requireJsonResponse<SampleTypeManagementResponse>(
    "Load WHONET specimen mapping",
    await page.request.get(`${API_PREFIX}/rest/sample-types/${sampleTypeId}`),
  );
  if (!current.success || !current.data) {
    throw new Error("WHONET specimen mapping response is incomplete");
  }
  if (current.data.whonetCode === whonetCode) {
    return;
  }

  const updated = await requireJsonResponse<SampleTypeManagementResponse>(
    "Set WHONET specimen mapping",
    await page.request.put(`${API_PREFIX}/rest/sample-types/${sampleTypeId}`, {
      headers: { "X-CSRF-Token": await getCsrfToken(page) },
      data: { whonetCode },
    }),
  );
  if (!updated.success || updated.data?.whonetCode !== whonetCode) {
    throw new Error("WHONET specimen mapping was not persisted");
  }
}

async function seedMicrobiologyWhonetExportScenario(
  page: Page,
  {
    scenarioKey,
    patientOrigin,
    specimenWhonetCode,
    unmappedSignificance = "CLINICALLY_SIGNIFICANT",
  }: WhonetExportScenarioOptions,
): Promise<SeededMicrobiologyWhonetExport> {
  const seeded = await provisionMicrobiologyScenario(page, "M4", scenarioKey);
  const required = [
    "organismId",
    "unmappedOrganismId",
    "antibioticId",
    "astPanelId",
    "activeBreakpointStandardId",
    "loadedBreakpointStandardId",
    "methodId",
  ] as const;
  for (const field of required) {
    if (!seeded[field]) {
      throw new Error(`Microbiology M4 scenario is missing ${field}`);
    }
  }
  const reference = seeded as SeededMicrobiologyReferenceAdmin & {
    sampleTypeId: string;
    unmappedOrganismId: string;
  };
  if (specimenWhonetCode) {
    await ensureSampleTypeWhonetCode(
      page,
      reference.sampleTypeId,
      specimenWhonetCode,
    );
  }
  const antibiotics = (
    await requireJsonResponse<MicrobiologyReferenceOption[]>(
      "Load M4 antibiotics",
      await page.request.get(
        `${API_PREFIX}/rest/microbiology/reference/antibiotics`,
      ),
    )
  ).filter((candidate) => ["CIPUAT", "GENUAT"].includes(candidate.code));
  if (antibiotics.length !== 2) {
    throw new Error("Microbiology M4 scenario requires CIPUAT and GENUAT");
  }

  const caseDetail = await requireJsonResponse<MicrobiologyCaseFixture>(
    "Load M4 case",
    await page.request.get(
      `${API_PREFIX}/rest/microbiology/cases/${reference.caseId}`,
    ),
  );
  const mappedExisting = caseDetail.isolates.find(
    (isolate) => isolate.isolateLabel === "WHONET-MAPPED",
  );
  const unmappedExisting = caseDetail.isolates.find(
    (isolate) => isolate.isolateLabel === "WHONET-UNMAPPED",
  );

  if (
    caseDetail.finalReleaseState === "FINAL_RELEASED" &&
    (!mappedExisting ||
      !unmappedExisting ||
      unmappedExisting.significance !== unmappedSignificance ||
      (patientOrigin &&
        caseDetail.orderDetail?.patientOrigin !== patientOrigin))
  ) {
    throw new Error(
      "Final WHONET fixture does not match its expected population",
    );
  }

  if (
    patientOrigin &&
    caseDetail.finalReleaseState !== "FINAL_RELEASED" &&
    caseDetail.orderDetail?.patientOrigin !== patientOrigin
  ) {
    await requireJsonResponse(
      "Set WHONET fixture patient origin",
      await page.request.put(
        `${API_PREFIX}/rest/microbiology/cases/${reference.caseId}/order-detail`,
        {
          headers: { "X-CSRF-Token": await getCsrfToken(page) },
          data: {
            cultureMethodId: reference.methodId,
            patientOrigin,
            admissionDate: "2026-08-17",
            numberOfSets: 1,
            clinicalHistory: "Synthetic WHONET export filter fixture",
            antibioticExposure: false,
          },
        },
      ),
    );
  }

  await ensureReviewedAstIsolate(
    page,
    reference,
    reference.organismId,
    "WHONET-MAPPED",
    "Reference organism (UAT)",
    antibiotics,
    mappedExisting,
  );
  await ensureReviewedAstIsolate(
    page,
    reference,
    reference.unmappedOrganismId,
    "WHONET-UNMAPPED",
    "WHONET mapping pending (UAT)",
    antibiotics,
    unmappedExisting,
    unmappedSignificance,
  );
  let closedAt = caseDetail.closedAt;
  if (caseDetail.finalReleaseState !== "FINAL_RELEASED") {
    const headers = { "X-CSRF-Token": await getCsrfToken(page) };
    const release = await requireJsonResponse<MicrobiologyReleaseFixture>(
      "Release M4 case for WHONET export",
      await page.request.post(
        `${API_PREFIX}/rest/microbiology/cases/${reference.caseId}/release/final`,
        { headers, data: {} },
      ),
    );
    closedAt = release.closedAt;
  }
  if (closedAt === undefined || closedAt === null || closedAt === "") {
    throw new Error("Final M4 fixture is missing its server release time");
  }
  const exportDate = new Date(closedAt).toISOString().slice(0, 10);

  return {
    ...reference,
    exportDate,
  };
}

export function seedMicrobiologyWhonetExport(
  page: Page,
): Promise<SeededMicrobiologyWhonetExport> {
  return seedMicrobiologyWhonetExportScenario(page, {
    scenarioKey: "playwright-m4-whonet-export",
  });
}

export function seedMicrobiologyWhonetExportFilters(
  page: Page,
): Promise<SeededMicrobiologyWhonetExport> {
  return seedMicrobiologyWhonetExportScenario(page, {
    scenarioKey: "playwright-r9-whonet-export-filters",
    patientOrigin: "INPATIENT",
    specimenWhonetCode: "BLD",
    unmappedSignificance: "CONTAMINANT",
  });
}

export async function seedMicrobiologyWorklistCases(
  page: Page,
  count = 200,
): Promise<SeededMicrobiologyCase[]> {
  if (!Number.isInteger(count) || count < 1 || count > 500) {
    throw new Error(
      "Microbiology worklist seed count must be between 1 and 500",
    );
  }
  const cases: SeededMicrobiologyCase[] = [];
  for (let index = 0; index < count; index += 1) {
    cases.push(await seedMicrobiologyWorklistCase(page));
  }
  return cases;
}

export async function seedMicrobiologyAstWorklistCases(
  page: Page,
  count = 200,
): Promise<SeededMicrobiologyAstWorklistCase[]> {
  if (!Number.isInteger(count) || count < 1 || count > 500) {
    throw new Error(
      "Microbiology AST worklist seed count must be between 1 and 500",
    );
  }
  const cases: SeededMicrobiologyAstWorklistCase[] = [];
  for (let index = 0; index < count; index += 1) {
    cases.push(await seedMicrobiologyAstWorklistCase(page));
  }
  return cases;
}

async function requireJsonResponse<T>(
  label: string,
  response: Awaited<ReturnType<Page["request"]["get"]>>,
): Promise<T> {
  if (!response.ok()) {
    const body = await response.text().catch(() => "");
    throw new Error(
      `${label} failed: HTTP ${response.status()} ${body.slice(0, 500)}`,
    );
  }
  return response.json() as Promise<T>;
}

interface PreparedMicrobiologyAstCase extends SeededMicrobiologyAstWorklistCase {
  antibioticId: string;
  orderedAntibioticIds: string[];
}

async function prepareMicrobiologyAstCase(
  page: Page,
): Promise<PreparedMicrobiologyAstCase> {
  const seeded = await seedMicrobiologyMvpCase(page);
  if (!seeded.organismId) {
    throw new Error("Microbiology MVP scenario is missing organismId");
  }
  const headers = { "X-CSRF-Token": await getCsrfToken(page) };

  const isolate = await requireJsonResponse<{ id: string }>(
    "Create microbiology isolate",
    await page.request.post(`${API_PREFIX}/rest/microbiology/isolates`, {
      headers,
      data: {
        caseId: seeded.caseId,
        isolateLabel: "ISO-1",
        gramStain: "Gram negative rods",
        colonyMorphology: "Synthetic lactose-fermenting colonies",
        preliminaryOrganismText: "Escherichia coli",
        significance: "CLINICALLY_SIGNIFICANT",
      },
    }),
  );
  await requireJsonResponse(
    "Confirm microbiology isolate",
    await page.request.put(
      `${API_PREFIX}/rest/microbiology/isolates/${isolate.id}/identification`,
      {
        headers,
        data: {
          organismId: seeded.organismId,
          preliminaryOrganismText: "Escherichia coli",
          significance: "CLINICALLY_SIGNIFICANT",
          identificationStatus: "CONFIRMED",
          identificationMethod: "MALDI_TOF",
          identificationConfidence: 99.5,
        },
      },
    ),
  );

  const [panels, standards, antibiotics] = await Promise.all([
    requireJsonResponse<MicrobiologyReferenceOption[]>(
      "Load AST panels",
      await page.request.get(
        `${API_PREFIX}/rest/microbiology/reference/ast-panels?workflowType=BACTERIOLOGY`,
      ),
    ),
    requireJsonResponse<MicrobiologyReferenceOption[]>(
      "Load breakpoint standards",
      await page.request.get(
        `${API_PREFIX}/rest/microbiology/reference/breakpoint-standards`,
      ),
    ),
    requireJsonResponse<MicrobiologyReferenceOption[]>(
      "Load antibiotics",
      await page.request.get(
        `${API_PREFIX}/rest/microbiology/reference/antibiotics`,
      ),
    ),
  ]);
  const panel = panels.find(
    (candidate) => candidate.label === "Gram negative AST panel (UAT)",
  );
  const standard = standards.find(
    (candidate) => candidate.label === "CLSI 2026",
  );
  const antibiotic = antibiotics.find(
    (candidate) => candidate.code === "CIPUAT",
  );
  if (!panel || !standard || !antibiotic) {
    throw new Error("Microbiology UAT AST reference data is incomplete");
  }
  const panelDetail = await requireJsonResponse<{
    antibiotics: Array<{ antibioticId: string }>;
  }>(
    "Load selected AST panel",
    await page.request.get(
      `${API_PREFIX}/rest/microbiology/admin/reference/ast-panels/${encodeURIComponent(panel.id)}`,
    ),
  );
  const panelAntibioticIds = panelDetail.antibiotics.map(
    (row) => row.antibioticId,
  );
  if (!panelAntibioticIds.includes(antibiotic.id)) {
    throw new Error("Microbiology UAT AST panel has no usable ordered drugs");
  }
  const orderedAntibioticIds = [antibiotic.id];

  const run = await requireJsonResponse<{ id: string }>(
    "Start AST run",
    await page.request.post(`${API_PREFIX}/rest/microbiology/ast/runs`, {
      headers,
      data: {
        isolateId: isolate.id,
        panelId: panel.id,
        panelAdjustmentReason:
          "Reference-admin evidence uses the seeded single-drug breakpoint",
        breakpointStandardId: standard.id,
        technique: "BROTH_MICRODILUTION",
        orderedAntibioticIds,
      },
    }),
  );
  return {
    ...seeded,
    isolateId: isolate.id,
    astRunId: run.id,
    antibioticId: antibiotic.id,
    orderedAntibioticIds,
  };
}

/**
 * Creates one actionable in-progress AST worklist row through authenticated
 * application endpoints. The run deliberately has no reading or review yet.
 */
export async function seedMicrobiologyAstWorklistCase(
  page: Page,
): Promise<SeededMicrobiologyAstWorklistCase> {
  return prepareMicrobiologyAstCase(page);
}

/**
 * Creates a reviewed, reportable bacteriology case through the authenticated,
 * property-gated scenario endpoint. Application services create every record
 * with server-generated identifiers and normal validation/auditing.
 */
export async function seedReviewedMicrobiologyCase(
  page: Page,
): Promise<SeededReviewedMicrobiologyCase> {
  const seeded = await provisionMicrobiologyScenario(page, "AST_REVIEWED");
  if (!seeded.isolateId || !seeded.astRunId) {
    throw new Error("Microbiology AST_REVIEWED scenario is incomplete");
  }
  return seeded as SeededReviewedMicrobiologyCase;
}

export async function seedAnalyzerReviewMicrobiologyCase(
  page: Page,
): Promise<SeededAnalyzerReviewMicrobiologyCase> {
  const seeded = await provisionMicrobiologyScenario(
    page,
    "AST_ANALYZER_REVIEW",
  );
  if (
    !seeded.isolateId ||
    !seeded.astRunId ||
    !seeded.analyzerInstrumentId ||
    !seeded.analyzerCardId ||
    !seeded.organismId ||
    !seeded.antibioticId
  ) {
    throw new Error("Microbiology AST_ANALYZER_REVIEW scenario is incomplete");
  }
  return seeded as SeededAnalyzerReviewMicrobiologyCase;
}

export async function submitQcFailedAstAnalyzerResults(
  page: Page,
  seeded: SeededAnalyzerReviewMicrobiologyCase,
) {
  const externalEventId = `playwright-ast-result-${randomUUID()}`;
  const response = await page.request.post(
    `${API_PREFIX}/rest/analyzer/events/ast`,
    {
      headers: { "X-CSRF-Token": await getCsrfToken(page) },
      data: {
        externalEventId,
        eventType: "AST_RESULT_AVAILABLE",
        analyzerId: seeded.analyzerInstrumentId,
        sourceId: seeded.analyzerCardId,
        payload: {
          analyzerInstrumentId: seeded.analyzerInstrumentId,
          analyzerCardId: seeded.analyzerCardId,
          analyzerSoftwareVersion: "UAT-1.0",
          analyzerOrganismId: seeded.organismId,
          analyzerOrganismName: "Escherichia coli (UAT)",
          analyzerOrganismConfidence: 99.5,
          instrumentQcReference: "UAT-QC-CONTROL-17",
          qcPassed: false,
          analyzerMessageCodes: ["CONTROL_OUT_OF_RANGE"],
          readings: [
            {
              antibioticId: seeded.antibioticId,
              rawValue: 4,
              units: "mg/L",
              instrumentInterpretation: "SUSCEPTIBLE",
              analyzerResultReference: `${externalEventId}-CIP`,
            },
          ],
        },
      },
    },
  );
  if (response.status() !== 202) {
    const body = await response.text().catch(() => "");
    throw new Error(
      `AST analyzer event failed: HTTP ${response.status()} ${body.slice(0, 500)}`,
    );
  }
  return response.json();
}

export async function submitUnmatchedAstAnalyzerResults(
  page: Page,
  seeded: SeededAnalyzerReviewMicrobiologyCase,
) {
  const externalEventId = `playwright-ast-unmatched-${randomUUID()}`;
  const sourceId = `${seeded.analyzerCardId}-UNMATCHED`;
  const response = await page.request.post(
    `${API_PREFIX}/rest/analyzer/events/ast`,
    {
      headers: { "X-CSRF-Token": await getCsrfToken(page) },
      data: {
        externalEventId,
        eventType: "AST_RESULT_AVAILABLE",
        analyzerId: seeded.analyzerInstrumentId,
        sourceId,
        payload: {
          analyzerInstrumentId: seeded.analyzerInstrumentId,
          analyzerCardId: sourceId,
          analyzerSoftwareVersion: "UAT-1.0",
          readings: [
            {
              antibioticId: seeded.antibioticId,
              rawValue: 4,
              units: "mg/L",
              instrumentInterpretation: "SUSCEPTIBLE",
              analyzerResultReference: `${externalEventId}-CIP`,
            },
          ],
        },
      },
    },
  );
  if (response.status() !== 422) {
    const body = await response.text().catch(() => "");
    throw new Error(
      `Unmatched AST analyzer event returned HTTP ${response.status()} ${body.slice(0, 500)}`,
    );
  }
  return { externalEventId, sourceId };
}

export async function seedDenseMicrobiologyCase(
  page: Page,
): Promise<SeededDenseMicrobiologyCase> {
  const seeded = await seedMicrobiologyMvpCase(page);
  const headers = { "X-CSRF-Token": await getCsrfToken(page) };
  const [panels, standards, antibiotics] = await Promise.all([
    requireJsonResponse<MicrobiologyReferenceOption[]>(
      "Load AST panels",
      await page.request.get(
        `${API_PREFIX}/rest/microbiology/reference/ast-panels?workflowType=BACTERIOLOGY`,
      ),
    ),
    requireJsonResponse<MicrobiologyReferenceOption[]>(
      "Load breakpoint standards",
      await page.request.get(
        `${API_PREFIX}/rest/microbiology/reference/breakpoint-standards`,
      ),
    ),
    requireJsonResponse<MicrobiologyReferenceOption[]>(
      "Load antibiotics",
      await page.request.get(
        `${API_PREFIX}/rest/microbiology/reference/antibiotics`,
      ),
    ),
  ]);
  const panel = panels.find(
    (candidate) => candidate.label === "Gram negative AST panel (UAT)",
  );
  const standard = standards.find(
    (candidate) => candidate.label === "CLSI 2026",
  );
  const readingAntibiotics = antibiotics.filter((candidate) =>
    ["CIPUAT", "GENUAT"].includes(candidate.code),
  );
  if (!panel || !standard || readingAntibiotics.length !== 2) {
    throw new Error("Microbiology UAT AST reference data is incomplete");
  }

  const isolateIds: string[] = [];
  let astReadingCount = 0;
  for (let isolateIndex = 1; isolateIndex <= 5; isolateIndex += 1) {
    const isolate = await requireJsonResponse<{ id: string }>(
      "Create dense-case isolate",
      await page.request.post(`${API_PREFIX}/rest/microbiology/isolates`, {
        headers,
        data: {
          caseId: seeded.caseId,
          isolateLabel: `QISO-${isolateIndex}`,
          gramStain: "Gram negative rods",
          colonyMorphology: `Qualification morphology ${isolateIndex}`,
          significance: "CLINICALLY_SIGNIFICANT",
        },
      }),
    );
    await requireJsonResponse(
      "Identify dense-case isolate",
      await page.request.put(
        `${API_PREFIX}/rest/microbiology/isolates/${isolate.id}/identification`,
        {
          headers,
          data: {
            organismId: seeded.organismId,
            preliminaryOrganismText: `Qualification organism ${isolateIndex}`,
            significance: "CLINICALLY_SIGNIFICANT",
            identificationStatus: "CONFIRMED",
            identificationMethod: "MALDI_TOF",
            identificationConfidence: 99.5,
          },
        },
      ),
    );
    isolateIds.push(isolate.id);
    for (let runIndex = 0; runIndex < 8; runIndex += 1) {
      const run = await requireJsonResponse<{ id: string }>(
        "Start dense-case AST run",
        await page.request.post(`${API_PREFIX}/rest/microbiology/ast/runs`, {
          headers,
          data: {
            isolateId: isolate.id,
            panelId: panel.id,
            panelAdjustmentReason:
              "Qualification fixture exercises the seeded two-drug panel",
            breakpointStandardId: standard.id,
            technique: "BROTH_MICRODILUTION",
          },
        }),
      );
      for (const [readingIndex, antibiotic] of readingAntibiotics.entries()) {
        await requireJsonResponse(
          "Record dense-case AST reading",
          await page.request.post(
            `${API_PREFIX}/rest/microbiology/ast/runs/${run.id}/readings`,
            {
              headers,
              data: {
                antibioticId: antibiotic.id,
                method: "MIC",
                rawValue: readingIndex === 0 ? 4 : 32,
              },
            },
          ),
        );
        astReadingCount += 1;
      }
      await requireJsonResponse(
        "Review dense-case AST run",
        await page.request.post(
          `${API_PREFIX}/rest/microbiology/ast/runs/${run.id}/review`,
          { headers, data: {} },
        ),
      );
    }
  }
  const timeline = await requireJsonResponse<unknown[]>(
    "Load dense-case timeline",
    await page.request.get(
      `${API_PREFIX}/rest/microbiology/cases/${seeded.caseId}/timeline`,
    ),
  );
  return {
    ...seeded,
    isolateIds,
    astReadingCount,
    timelineEventCount: timeline.length,
  };
}

export async function seedFinalizedMicrobiologyCase(
  page: Page,
): Promise<SeededFinalMicrobiologyCase> {
  const seeded = await seedReviewedMicrobiologyCase(page);
  const headers = { "X-CSRF-Token": await getCsrfToken(page) };
  await requireJsonResponse(
    "Release final microbiology report",
    await page.request.post(
      `${API_PREFIX}/rest/microbiology/cases/${seeded.caseId}/release/final`,
      { headers, data: {} },
    ),
  );

  return seeded;
}
