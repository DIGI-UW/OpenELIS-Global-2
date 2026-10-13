import { expect, type Page } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../fixtures/analyzer-setup";
import { analyzerByName, type Analyzer } from "./analyzer-api";
import { csrfToken } from "./api-session";
import { API } from "./analyzer-profile-api";
import { BC5380, GENEXPERT, type ShippedProfile } from "./shipped-profiles";

/** How the instrument reaches the Bridge: a sender on the shared listener, or a watched directory. */
export type Connection = { senderId: string } | { importDirectory: string };

/** The assays turned off in the Assays step. */
export type AssayOptions = {
  assaysOff?: string[];
};

/**
 * Set an analyzer up on a shipped baseline profile through the setup screens:
 * the assays named in `assaysOff` turned off, its shipped mapping confirmed,
 * its connection set, and the connection active.
 */
export async function activateShippedAnalyzer(
  page: Page,
  profile: ShippedProfile,
  name: string,
  connection: Connection,
  assays: AssayOptions = {},
): Promise<Analyzer> {
  const list = new AnalyzerListPage(page);
  const setup = new AnalyzerSetupPage(page);
  await list.goto();
  await list.clickAdd();
  await setup.expectOpen();
  await setup.selectProfile(profile.displayName, profile);
  await setup.fillName(name);
  await setup.selectLabUnit(profile.labUnit);
  await setup.continueToVerify(assays);
  const mapping = (await (
    await page.request.get(
      `${API}/analyzer-types/${profile.profileId}/mapping?revision=${profile.revision}`,
    )
  ).json()) as { confirmation: { state: string } };
  if (mapping.confirmation.state !== "CURRENT") {
    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeEnabled();
    await confirm.click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible();
  }
  await setup.continueToConnect();
  if ("senderId" in connection) {
    await setup.fillSenderId(connection.senderId);
  } else {
    await setup.fillImportDirectory(connection.importDirectory);
  }
  await page.getByRole("button", { name: "Finish and activate" }).click();
  const analyzer = await analyzerByName(page, name, profile.profileId);
  await expect(page.getByTestId(`analyzer-row-${analyzer.id}`)).toContainText(
    "Active",
  );
  return analyzer;
}

/** A Mindray BC-5380 on the shipped baseline profile, active on the shared MLLP listener. */
export const activateShippedBc5380 = (
  page: Page,
  name: string,
  senderId: string,
  options: AssayOptions = {},
) => activateShippedAnalyzer(page, BC5380, name, { senderId }, options);

/** A GeneXpert on the shipped baseline profile, active on the shared listener. */
export const activateShippedGeneXpert = (
  page: Page,
  name: string,
  senderId: string,
  options: AssayOptions = {},
) => activateShippedAnalyzer(page, GENEXPERT, name, { senderId }, options);

type MappingRow = { sourceRowKey: string; subIdentity: string | null };
type MappingView = {
  mappingId: string;
  mappingRevision: number;
  mappingFingerprint: string;
  controlRecognition: { recognitionFingerprint: string };
  tests: Array<
    MappingRow & {
      mappingState: string;
      testId: string | null;
      componentId: string | null;
      callComponentId: string | null;
      results: Array<{
        rawValue: string;
        mappingState: string;
        resultOptionId: string | null;
      }>;
    }
  >;
};

/**
 * The lab excludes codes its analyzer has sent, as on the analyzer's mapping
 * page: saved, confirmed and applied, which retries what was held on them.
 */
export async function excludeCodes(
  page: Page,
  analyzerId: string,
  codes: string[],
) {
  const url = `${API}/analyzer/analyzers/${analyzerId}/mapping`;
  const read = async () =>
    (await (await page.request.get(url)).json()) as MappingView;
  const send = async (method: "put" | "post", path: string, data: object) => {
    const response = await page.request[method](path, {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data,
    });
    expect(response.status(), await response.text()).toBe(200);
  };

  const current = await read();
  await send("put", url, {
    baseMappingFingerprint: current.mappingFingerprint,
    tests: current.tests.map((test) => {
      const state = codes.includes(test.sourceRowKey)
        ? "EXCLUDED"
        : test.mappingState;
      const bound = state === "BOUND";
      return {
        sourceRowKey: test.sourceRowKey,
        subIdentity: test.subIdentity || "",
        mappingState: state,
        testId: bound ? test.testId : null,
        componentId: bound ? test.componentId : null,
        callComponentId: bound ? test.callComponentId : null,
      };
    }),
    results: current.tests.flatMap((test) =>
      test.results.map((result) => ({
        sourceRowKey: test.sourceRowKey,
        subIdentity: test.subIdentity || "",
        rawValue: result.rawValue,
        mappingState: result.mappingState,
        testResultId:
          result.mappingState === "BOUND" ? result.resultOptionId : null,
      })),
    ),
  });

  const saved = await read();
  const rows = (state: string) =>
    saved.tests.flatMap((test) =>
      [
        { state: test.mappingState, rawValue: null as string | null },
        ...test.results.map((result) => ({
          state: result.mappingState,
          rawValue: result.rawValue,
        })),
      ]
        .filter((row) => row.state === state)
        .map((row) => ({
          sourceRowKey: test.sourceRowKey,
          subIdentity: test.subIdentity || "",
          rawValue: row.rawValue,
        })),
    );
  await send("post", `${url}/confirm`, {
    baseMappingFingerprint: saved.mappingFingerprint,
    recognitionFingerprint: saved.controlRecognition.recognitionFingerprint,
    confirmedRows: rows("BOUND"),
    excludedRows: rows("EXCLUDED"),
  });

  const confirmed = await read();
  await send("put", `${url}/apply`, {
    mappingId: confirmed.mappingId,
    revision: confirmed.mappingRevision,
    mappingFingerprint: confirmed.mappingFingerprint,
  });
}
