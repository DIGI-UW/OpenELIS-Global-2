import { expect, test } from "../../../helpers/test-base";

const API = "/api/OpenELIS-Global/rest";

/** Each baseline profile's assays and the harness dictionary test each binds. */
const baselines = [
  {
    profileId: "cepheid-genexpert-astm",
    tests: {
      HIVVL: "HIV-1 Viral Load",
      SARSCOV2: "SARS-CoV-2 PCR",
      FLUA: "Influenza A PCR",
      FLUB: "Influenza B PCR",
      RSV: "RSV PCR",
      SARSCOV2_3: "SARS-CoV-2 PCR",
      MTB: "Xpert MTB/RIF",
      RIF: "Rifampin Resistance",
    },
  },
  {
    profileId: "hain-fluorocycler-xt",
    tests: { "VIH-1": "HIV-1 Viral Load" },
  },
  {
    profileId: "thermo-quantstudio",
    tests: { "VIH-1": "HIV-1 Viral Load", IC: "Internal Control DNA" },
  },
] as const;

type MappingRow = {
  rawCode: string;
  subIdentity: string | null;
  componentCode: string | null;
  mappingState: string;
  unresolvedReason: string | null;
  selectedTest: { name: string } | null;
  results: Array<{
    rawValue: string;
    mappingState: string;
    unresolvedReason: string | null;
  }>;
};

for (const baseline of baselines) {
  test(`a fresh ${baseline.profileId} setup binds every test, record and value`, async ({
    page,
  }) => {
    const types = await page.request.get(`${API}/analyzer-types`);
    expect(types.ok()).toBeTruthy();
    const shipped = (
      (await types.json()) as {
        types: Array<{
          profileId: string;
          revision: number;
          status: string;
          source: string;
        }>;
      }
    ).types.filter(
      (type) =>
        type.profileId === baseline.profileId &&
        type.status === "ACTIVE" &&
        type.source === "SHIPPED",
    );
    expect(shipped, `One active shipped ${baseline.profileId}`).toHaveLength(1);

    // With no analyzer, the mapping is what a new analyzer on this revision starts with.
    const response = await page.request.get(
      `${API}/analyzer-types/${baseline.profileId}/mapping?revision=${shipped[0].revision}`,
    );
    expect(response.ok()).toBeTruthy();
    const rows = ((await response.json()) as { tests: MappingRow[] }).tests;

    const mainRows = rows.filter((row) => !row.subIdentity);
    expect(
      Object.fromEntries(
        mainRows.map((row) => [row.rawCode, row.selectedTest?.name ?? null]),
      ),
    ).toEqual(baseline.tests);

    const unbound = rows.flatMap((row) => {
      const record = `${row.rawCode} ${row.subIdentity ?? ""}`.trim();
      return [
        ...(row.mappingState === "BOUND"
          ? []
          : [`${record}: ${row.unresolvedReason}`]),
        ...row.results
          .filter((result) => result.mappingState !== "BOUND")
          .map(
            (result) =>
              `${record} = ${result.rawValue}: ${result.unresolvedReason}`,
          ),
      ];
    });
    expect(unbound, "Rows a fresh setup leaves unresolved").toEqual([]);
  });
}
