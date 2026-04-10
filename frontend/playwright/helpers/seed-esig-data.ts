/**
 * E-Sig E2E test data seeding helpers.
 *
 * Creates sample orders via the OpenELIS REST API using the authenticated
 * browser session — same path as the React UI. No direct database access.
 *
 * Modeled on seed-tat-data.ts.
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
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  const time = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

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
