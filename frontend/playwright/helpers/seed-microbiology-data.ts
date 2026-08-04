import { randomUUID } from "crypto";
import type { Page } from "@playwright/test";

const API_PREFIX = "/api/OpenELIS-Global";

export interface SeededMicrobiologyCase {
  caseId: string;
  sampleItemId: string;
  sampleId: string;
  patientId: string;
  analysisId: string;
  siblingCaseId?: string;
}

export interface SeededReviewedMicrobiologyCase extends SeededMicrobiologyCase {
  isolateId: string;
  astRunId: string;
}

export interface SeededDenseMicrobiologyCase extends SeededMicrobiologyCase {
  isolateIds: string[];
  astReadingCount: number;
}

export type SeededFinalMicrobiologyCase = SeededReviewedMicrobiologyCase;

type MicrobiologyScenario = "CASE" | "MVP" | "WORKLIST";

interface MicrobiologyReferenceOption {
  id: string;
  label: string;
  code: string;
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
): Promise<SeededMicrobiologyCase> {
  const csrfToken = await getCsrfToken(page);
  const response = await page.request.post(
    `${API_PREFIX}/rest/microbiology/uat/scenarios`,
    {
      data: {
        scenario,
        scenarioKey: `playwright-${scenario.toLowerCase()}-${randomUUID()}`,
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
    cases.push(await seedMicrobiologyMvpCase(page));
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
