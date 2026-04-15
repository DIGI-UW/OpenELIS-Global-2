import { APIRequestContext, expect } from "@playwright/test";

export interface AnalyzerPayload {
  name: string;
  analyzerType: string;
  pluginTypeId: string;
  ipAddress: string;
  port: number;
  protocolVersion: string;
  identifierPattern: string;
  status: string;
  defaultConfigId: string;
}

export const GENEXPERT_DEFAULT_ANALYZER: AnalyzerPayload = {
  name: "Cepheid GeneXpert (ASTM Mode)",
  analyzerType: "MOLECULAR",
  pluginTypeId: "generic-astm",
  ipAddress: "10.42.20.10",
  port: 9600,
  protocolVersion: "ASTM_LIS2_A2",
  identifierPattern: "GENEXPERT|CEPHEID",
  status: "ACTIVE",
  defaultConfigId: "astm/genexpert-astm",
};

interface AnalyzerSummary {
  id: string | number;
  name?: string;
}

export async function ensureAnalyzerByName(
  request: APIRequestContext,
  matches: (analyzer: AnalyzerSummary) => boolean,
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

  const createResp = await request.post(
    "/api/OpenELIS-Global/rest/analyzer/analyzers",
    {
      data: payload,
    },
  );
  expect(createResp.ok()).toBeTruthy();
  const created = await createResp.json();
  return String(created.id);
}
