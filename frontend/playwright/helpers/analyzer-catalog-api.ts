/**
 * Putting tests in the catalog through the same CSV import the Admin screen
 * uses, so an analyzer spec controls the LOINC codes it maps against.
 */
import { expect, type Page } from "@playwright/test";
import { API } from "./analyzer-profile-api";
import { csrfToken } from "./api-session";

export type SeededTest = { id: string; name: string; loinc: string };

async function importCsv(page: Page, name: string, lines: string[]) {
  const response = await page.request.post(
    `${API}/configuration/import/apply`,
    {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      multipart: {
        files: {
          name,
          mimeType: "text/csv",
          buffer: Buffer.from(`${lines.join("\n")}\n`),
        },
      },
    },
  );
  expect(
    response.ok(),
    `Import ${name}: ${response.status()} ${await response.text()}`,
  ).toBeTruthy();
  const plan = (await response.json()) as {
    files: Array<{ fileName: string; error?: string | null }>;
  };
  expect(plan.files.filter((file) => file.error)).toEqual([]);
}

/**
 * Numeric catalog tests, each with its own name and LOINC code, in an existing
 * lab unit and specimen type. Returns them with the ids the catalog gave them.
 */
export async function seedNumericTests(
  page: Page,
  run: string,
  tests: Array<{ name: string; loinc: string }>,
  placement: { testSection: string; sampleType: string },
): Promise<SeededTest[]> {
  await importCsv(page, `tests-e2e-${run}.csv`, [
    "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure",
    ...tests.map(
      ({ name, loinc }) =>
        `${name},${placement.testSection},${placement.sampleType},${loinc},Y,Y,990,`,
    ),
  ]);
  await importCsv(page, `result-components-e2e-${run}.csv`, [
    "testName,code,label,resultType,significantDigits,isPrimary,showOnReport",
    ...tests.map(({ name }) => `${name},PRIMARY,${name},N,2,Y,Y`),
  ]);
  const catalog = (await (
    await page.request.get(`${API}/analyzer-types/mapping-catalog/tests`)
  ).json()) as Array<{ id: string; name: string; loincCodes: string[] }>;
  return tests.map(({ name, loinc }) => {
    const found = catalog.filter((candidate) =>
      candidate.loincCodes.includes(loinc),
    );
    const match = found.find((candidate) => candidate.name.startsWith(name));
    expect(
      match,
      `${name} (${loinc}) is in the mapping catalog; found ${JSON.stringify(found)}`,
    ).toBeTruthy();
    return { id: match!.id, name: match!.name, loinc };
  });
}
