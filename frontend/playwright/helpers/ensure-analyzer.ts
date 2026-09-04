import { APIRequestContext, expect } from "@playwright/test";

export interface AnalyzerPayload {
  name: string;
  profileId: string;
  ipAddress?: string;
  port?: number;
  importDirectory?: string;
}

export const GENEXPERT_DEFAULT_ANALYZER: AnalyzerPayload = {
  name: "Cepheid GeneXpert (ASTM Mode)",
  profileId: "genexpert-astm",
  ipAddress: "10.42.20.10",
  port: 9600,
};

interface AnalyzerSummary {
  id: string | number;
  name?: string;
}

interface AnalyzerTypeSummary {
  profileId: string;
  revision: number;
  status: string;
}

async function resolveActiveProfileRevision(
  request: APIRequestContext,
  profileId: string,
): Promise<number> {
  const response = await request.get(
    "/api/OpenELIS-Global/rest/analyzer-types",
  );
  expect(response.ok()).toBeTruthy();
  const catalog = await response.json();
  const active = (catalog.types ?? []).filter(
    (type: AnalyzerTypeSummary) =>
      type.profileId === profileId && type.status === "ACTIVE",
  );
  expect(
    active,
    `Expected exactly one active revision for ${profileId}`,
  ).toHaveLength(1);
  return active[0].revision;
}

export async function ensureAnalyzerByName(
  request: APIRequestContext,
  matches: (analyzer: AnalyzerSummary) => boolean | undefined,
  payload: AnalyzerPayload,
): Promise<string> {
  const listResp = await request.get(
    "/api/OpenELIS-Global/rest/analyzer/analyzers",
  );
  expect(listResp.ok()).toBeTruthy();
  const data = await listResp.json();
  const existing = (data.analyzers ?? []).find(matches);
  if (existing) {
    return String(existing.id);
  }

  const profileRevision = await resolveActiveProfileRevision(
    request,
    payload.profileId,
  );
  const createResp = await request.post(
    "/api/OpenELIS-Global/rest/analyzer/analyzers",
    {
      data: { ...payload, profileRevision },
    },
  );
  expect(createResp.ok()).toBeTruthy();
  const created = await createResp.json();
  return String(created.id);
}
