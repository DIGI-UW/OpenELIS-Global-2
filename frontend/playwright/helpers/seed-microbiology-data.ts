import { randomUUID } from "crypto";
import type { Page } from "@playwright/test";

const API_PREFIX = "/api/OpenELIS-Global";

export interface SeededMicrobiologyCase {
  accessionNumber: string;
  caseId: string;
  sampleItemId: string;
  sampleId: string;
  patientId: string;
  analysisId: string;
  siblingCaseId?: string;
  organismId?: string;
  antibioticId?: string;
  astPanelId?: string;
  activeBreakpointStandardId?: string;
  loadedBreakpointStandardId?: string;
  unmappedOrganismId?: string;
  methodId?: string;
}

export interface SeededReviewedMicrobiologyCase extends SeededMicrobiologyCase {
  isolateId: string;
  astRunId: string;
}

export interface SeededDenseMicrobiologyCase extends SeededMicrobiologyCase {
  isolateIds: string[];
  astReadingCount: number;
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
  unmappedOrganismId: string;
}

export type SeededFinalMicrobiologyCase = SeededReviewedMicrobiologyCase;

type MicrobiologyScenario = "CASE" | "MVP" | "WORKLIST" | "M3" | "M4" | "R1";

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
}

interface MicrobiologyCaseFixture {
  closedAt?: number | string;
  finalReleaseState: string;
  isolates: MicrobiologyIsolateFixture[];
}

interface MicrobiologyAstRunFixture {
  id: string;
  status: string;
  readings: Array<{ antibioticId: string }>;
}

interface MicrobiologyReleaseFixture {
  closedAt: number | string;
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
          organismId,
          preliminaryOrganismText: organismText,
          significance: "CLINICALLY_SIGNIFICANT",
        },
      }),
    ));
  if (
    isolate.organismId !== organismId ||
    isolate.identificationStatus !== "CONFIRMED"
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
            significance: "CLINICALLY_SIGNIFICANT",
            identificationStatus: "CONFIRMED",
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

export async function seedMicrobiologyWhonetExport(
  page: Page,
): Promise<SeededMicrobiologyWhonetExport> {
  const seeded = await provisionMicrobiologyScenario(
    page,
    "M4",
    "playwright-m4-whonet-export",
  );
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
    unmappedOrganismId: string;
  };
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
    (!mappedExisting || !unmappedExisting)
  ) {
    throw new Error("Final M4 fixture is missing its expected isolates");
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

/**
 * Creates a reviewed, reportable bacteriology case through authenticated HTTP
 * endpoints. Every persisted record is therefore created by application
 * services, with server-generated identifiers and normal validation/auditing.
 */
export async function seedReviewedMicrobiologyCase(
  page: Page,
): Promise<SeededReviewedMicrobiologyCase> {
  const seeded = await seedMicrobiologyMvpCase(page);
  const headers = { "X-CSRF-Token": await getCsrfToken(page) };

  const isolate = await requireJsonResponse<{ id: string }>(
    "Create microbiology isolate",
    await page.request.post(`${API_PREFIX}/rest/microbiology/isolates`, {
      headers,
      data: {
        caseId: seeded.caseId,
        isolateLabel: "ISO-1",
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
          preliminaryOrganismText: "Escherichia coli",
          significance: "CLINICALLY_SIGNIFICANT",
          identificationStatus: "CONFIRMED",
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

  const run = await requireJsonResponse<{ id: string }>(
    "Start AST run",
    await page.request.post(`${API_PREFIX}/rest/microbiology/ast/runs`, {
      headers,
      data: {
        isolateId: isolate.id,
        panelId: panel.id,
        breakpointStandardId: standard.id,
      },
    }),
  );
  await requireJsonResponse(
    "Record AST reading",
    await page.request.post(
      `${API_PREFIX}/rest/microbiology/ast/runs/${run.id}/readings`,
      {
        headers,
        data: {
          antibioticId: antibiotic.id,
          method: "MIC",
          rawValue: 4,
        },
      },
    ),
  );
  await requireJsonResponse(
    "Review AST run",
    await page.request.post(
      `${API_PREFIX}/rest/microbiology/ast/runs/${run.id}/review`,
      { headers, data: {} },
    ),
  );

  return { ...seeded, isolateId: isolate.id, astRunId: run.id };
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
          preliminaryOrganismText: `Qualification organism ${isolateIndex}`,
          significance: "CLINICALLY_SIGNIFICANT",
        },
      }),
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
            breakpointStandardId: standard.id,
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
    }
  }
  return { ...seeded, isolateIds, astReadingCount };
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
