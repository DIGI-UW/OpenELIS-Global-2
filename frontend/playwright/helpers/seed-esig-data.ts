/**
 * E-Sig E2E test data seeding helpers.
 *
 * Creates sample orders via the OpenELIS REST API using the authenticated
 * browser session — same path as the React UI. No direct database access.
 */
import { Page } from "@playwright/test";
import { createSampleOrder } from "./seed-tat-data";

export interface EsigTestData {
  accessionNumber: string;
}

/**
 * Create a sample order for e-sig testing via the REST API.
 *
 * Returns the generated accession number. The order will have analyses
 * in "Not Tested" status, ready for result entry.
 */
export async function createEsigSampleOrder(
  page: Page,
): Promise<EsigTestData | null> {
  // UTC-based: server runs UTC (docker-compose TZ), aligning here prevents
  // midnight-rollover flakes when dev/runner and server fall on different dates.
  const now = new Date();
  const today = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, "0")}-${String(now.getUTCDate()).padStart(2, "0")}`;
  const time = `${String(now.getUTCHours()).padStart(2, "0")}:${String(now.getUTCMinutes()).padStart(2, "0")}`;

  const accessionNumber = await createSampleOrder(page, {
    labNo: "",
    receivedDate: today,
    receivedTime: time,
  });

  if (!accessionNumber) {
    return null;
  }

  return { accessionNumber };
}
