import { expect, type Page } from "@playwright/test";
import { API } from "./analyzer-profile-api";

export type Analyzer = {
  id: string;
  name: string;
  profileId: string;
  profileRevision: number;
  bridgeConnectionId: string;
  status: string;
};

export type WorklistRow = {
  id: string;
  accessionNumber: string;
  rawTestCode: string;
  testId: string | null;
  rawResultValue: string;
  importIssueReason: string | null;
};

export async function analyzerByName(
  page: Page,
  name: string,
  profileId: string,
): Promise<Analyzer> {
  const response = await page.request.get(`${API}/analyzer/analyzers`);
  expect(response.ok()).toBeTruthy();
  const payload = (await response.json()) as { analyzers: Analyzer[] };
  const matches = payload.analyzers.filter(
    (candidate) => candidate.name === name && candidate.profileId === profileId,
  );
  expect(matches, `One provisioned ${name} connection`).toHaveLength(1);
  return matches[0];
}

/**
 * What the analyzer's results screen holds for one accession, as the API
 * reports it. The screen is paged by accession; this reads the accession's page.
 */
export async function worklistFor<Row extends WorklistRow = WorklistRow>(
  page: Page,
  analyzerId: string,
  accession: string,
): Promise<Row[]> {
  const read = async (pageNumber?: string) => {
    const response = await page.request.get(
      `${API}/AnalyzerResults?id=${analyzerId}${pageNumber ? `&page=${pageNumber}` : ""}`,
    );
    expect(response.ok()).toBeTruthy();
    return (await response.json()) as {
      resultList: Row[];
      paging?: { searchTermToPage?: Array<{ id: string; value: string }> };
    };
  };
  let payload = await read();
  const accessionPage = payload.paging?.searchTermToPage?.find(
    (term) => term.id === accession,
  )?.value;
  if (accessionPage && accessionPage !== "1") {
    payload = await read(accessionPage);
  }
  return payload.resultList.filter((row) => row.accessionNumber === accession);
}
