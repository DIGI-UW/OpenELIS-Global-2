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

export interface SeededFinalMicrobiologyCase extends SeededMicrobiologyCase {
  isolateId: string;
  astRunId: string;
}

type MicrobiologyScenario = "CASE" | "MVP" | "WORKLIST";

interface MicrobiologyReferenceOption {
  id: string;
  label: string;
  code: string;
}

export async function getCsrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  throw new Error(
    "Authenticated Playwright storage state is missing the CSRF token",
  );
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
 * Creates a final, reportable bacteriology case through authenticated HTTP
 * endpoints. Every persisted record is therefore created by application
 * services, with server-generated identifiers and normal validation/auditing.
 */
export async function seedFinalizedMicrobiologyCase(
  page: Page,
): Promise<SeededFinalMicrobiologyCase> {
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
  await requireJsonResponse(
    "Release final microbiology report",
    await page.request.post(
      `${API_PREFIX}/rest/microbiology/cases/${seeded.caseId}/release/final`,
      { headers, data: {} },
    ),
  );

  return { ...seeded, isolateId: isolate.id, astRunId: run.id };
}
