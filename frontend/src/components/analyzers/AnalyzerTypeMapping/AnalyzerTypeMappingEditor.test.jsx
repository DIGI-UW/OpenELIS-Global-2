import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route, useLocation } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  applyAnalyzerMapping,
  confirmAnalyzerMapping,
  getAnalyzerMapping,
  getAnalyzerMappingComponents,
  getAnalyzerMappingResultOptions,
  getAnalyzerMappingTests,
  getAnalyzerTypeDefaults,
  getAnalyzerTypeRevision,
  saveAnalyzerMapping,
} from "../../../services/analyzerService";
import AnalyzerTypeMappingEditor from "./AnalyzerTypeMappingEditor";

vi.mock("../../../services/analyzerService", () => ({
  applyAnalyzerMapping: vi.fn(),
  confirmAnalyzerMapping: vi.fn(),
  getAnalyzerMapping: vi.fn(),
  getAnalyzerMappingComponents: vi.fn(),
  getAnalyzerMappingResultOptions: vi.fn(),
  getAnalyzerMappingTests: vi.fn(),
  getAnalyzerTypeDefaults: vi.fn(),
  getAnalyzerTypeRevision: vi.fn(),
  saveAnalyzerMapping: vi.fn(),
}));

const recognition = {
  recognitionFingerprint: `sha256:${"c".repeat(64)}`,
  mode: "RULES",
  description: "Control results are recognized by specimen identifiers.",
  affirmedNoControlResults: false,
  conditions: [
    {
      key: "positive-control",
      kind: "SPECIMEN_ID_STARTS_WITH",
      sourceLabel: "Specimen ID",
      value: "CPOS",
      description: "SERVER DESCRIPTION MUST NOT RENDER",
      controlLevel: "POSITIVE",
      controlType: "ASSAY_CONTROL",
    },
  ],
};

const unconfirmed = {
  state: "UNCONFIRMED",
  profileId: null,
  profileRevision: 0,
  mappingFingerprint: null,
  recognitionFingerprint: null,
  confirmedBy: null,
  confirmedAt: null,
  confirmedRows: [],
  excludedRows: [],
};

const mapping = {
  analyzerId: "501",
  profileId: "shipped.genexpert",
  profileRevision: 2,
  profileFingerprint: `sha256:${"a".repeat(64)}`,
  displayName: "Cepheid GeneXpert MTB/RIF",
  protocol: "ASTM",
  mappingId: "11",
  mappingRevision: 3,
  mappingFingerprint: `sha256:${"b".repeat(64)}`,
  tests: [
    {
      sourceRowKey: "RAW-A",
      rawCode: "RAW-A",
      aliases: ["RAW A"],
      testNameHint: "Rifampin Resistance",
      loinc: "46244-0",
      unit: null,
      resultType: "qualitative",
      normalizedCoding: {
        system: "urn:openelis:analyzer-test",
        code: "SHARED",
        display: "Shared normalized identity",
      },
      mappingState: "BOUND",
      testId: "9701",
      selectedTest: {
        id: "9701",
        name: "Rifampin Resistance",
        code: "RIF",
        loincCodes: ["46244-0"],
      },
      suggestedTest: null,
      results: [
        {
          rawValue: "DETECTED",
          mappingState: "BOUND",
          resultOptionId: "811",
          selectedOption: {
            id: "811",
            value: "R",
            label: "Resistant",
          },
        },
        {
          rawValue: "NOT DETECTED",
          mappingState: "UNRESOLVED",
          resultOptionId: null,
          selectedOption: null,
        },
      ],
    },
    {
      sourceRowKey: "RAW-B",
      rawCode: "RAW-B",
      aliases: [],
      testNameHint: "COVID-19 PCR",
      loinc: "94500-6",
      unit: null,
      resultType: "quantitative",
      normalizedCoding: {
        system: "urn:openelis:analyzer-test",
        code: "SHARED",
        display: "Shared normalized identity",
      },
      mappingState: "UNRESOLVED",
      testId: null,
      selectedTest: null,
      suggestedTest: {
        id: "9702",
        name: "COVID-19 PCR",
        code: "COVID19",
        loincCodes: ["94500-6"],
      },
      results: [],
    },
    {
      sourceRowKey: "RAW-C",
      rawCode: "RAW-C",
      aliases: [],
      testNameHint: "Unconfigured qualitative test",
      loinc: "94558-4",
      unit: null,
      resultType: "qualitative",
      normalizedCoding: null,
      mappingState: "UNRESOLVED",
      testId: null,
      selectedTest: null,
      suggestedTest: null,
      results: [
        {
          rawValue: "HIGH",
          mappingState: "UNRESOLVED",
          resultOptionId: null,
          selectedOption: null,
        },
      ],
    },
  ],
  controlRecognition: recognition,
  confirmation: unconfirmed,
};

const catalogTests = [
  mapping.tests[0].selectedTest,
  mapping.tests[1].suggestedTest,
  {
    id: "9703",
    name: "Unconfigured qualitative test",
    code: "UNCONFIGURED",
    loincCodes: ["94558-4"],
  },
];

const resultOptions = {
  9701: [
    { id: "811", value: "R", label: "Resistant" },
    { id: "812", value: "S", label: "Susceptible" },
  ],
  9702: [],
  9703: [],
};

const componentsByTest = {};

const LocationProbe = () => {
  const location = useLocation();
  return (
    <output data-testid="location">
      {location.pathname + location.search}
    </output>
  );
};

const ReturnStateProbe = () => {
  const location = useLocation();
  return (
    <output data-testid="return-state">{JSON.stringify(location.state)}</output>
  );
};

const renderEditor = (
  entry = "/analyzers/501/mapping?returnTo=%2FAnalyzerResults%3Fid%3D501",
) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:analyzerId/mapping">
          <AnalyzerTypeMappingEditor />
          <LocationProbe />
        </Route>
        <Route path="/AnalyzerResults">
          <ReturnStateProbe />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

const renderDefaults = (
  entry = "/analyzers/types/shipped.genexpert/mapping?revision=2&returnTo=%2Fanalyzers%2Ftypes%3Fmapping%3DINCOMPLETE",
) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/types/:profileId/mapping">
          <AnalyzerTypeMappingEditor />
          <LocationProbe />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyzerTypeMappingEditor", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzerMapping.mockImplementation((_id, callback) => callback(mapping));
    getAnalyzerTypeDefaults.mockImplementation(
      (_profileId, _revision, callback) =>
        callback({ ...mapping, analyzerId: null, mappingId: null }),
    );
    getAnalyzerTypeRevision.mockImplementation(
      (_profileId, _revision, callback) =>
        callback({
          profileId: mapping.profileId,
          revision: mapping.profileRevision,
          displayName: mapping.displayName,
          usedBy: 3,
          affectedAnalyzers: [
            {
              id: "501",
              name: "GeneXpert - Main Lab",
              active: true,
              pinnedProfileRevision: 2,
              pinnedMappingRevision: 3,
              updateAvailable: false,
            },
            {
              id: "502",
              name: "GeneXpert - TB Bench",
              active: true,
              pinnedProfileRevision: 1,
              pinnedMappingRevision: 2,
              updateAvailable: true,
            },
            {
              id: "503",
              name: "GeneXpert - Reference Lab",
              active: false,
              pinnedProfileRevision: 2,
              pinnedMappingRevision: 3,
              updateAvailable: false,
            },
          ],
        }),
    );
    getAnalyzerMappingTests.mockImplementation((callback) =>
      callback(catalogTests),
    );
    getAnalyzerMappingResultOptions.mockImplementation((testId, callback) =>
      callback(resultOptions[testId] || []),
    );
    getAnalyzerMappingComponents.mockImplementation((testId, callback) =>
      callback(componentsByTest[testId] || []),
    );
  });

  describe("a test that reports several records", () => {
    const viralLoadTest = {
      id: "9801",
      name: "HIV-1 viral load",
      code: "HIVVL",
      loincCodes: ["20447-9"],
    };
    const otherViralLoadTest = {
      id: "9802",
      name: "HIV-1 viral load (plasma)",
      code: "HIVVLP",
      loincCodes: ["20447-9"],
    };
    const componentsByTestForRecords = {
      9801: [
        { id: "comp-call", code: "call", label: "Call" },
        { id: "comp-LOG", code: "LOG", label: "Log viral load" },
        { id: "comp-ept", code: "EPT", label: "End point" },
      ],
      9802: [
        { id: "plasma-call", code: "call", label: "Call" },
        { id: "plasma-LOG", code: "LOG", label: "Log viral load" },
      ],
    };
    const recordRow = (subIdentity, overrides = {}) => ({
      sourceRowKey: "HIVVL",
      rawCode: "HIVVL",
      subIdentity,
      aliases: [],
      testNameHint: "HIV-1 viral load",
      loinc: "20447-9",
      unit: null,
      resultType: "quantitative",
      normalizedCoding: null,
      mappingState: "BOUND",
      testId: "9801",
      selectedTest: viralLoadTest,
      suggestedTest: null,
      componentId: null,
      callComponentId: null,
      componentCode: null,
      callComponentCode: null,
      results: [],
      ...overrides,
    });
    const viralLoadMapping = {
      ...mapping,
      tests: [
        recordRow("", {
          callComponentId: "comp-call",
          callComponentCode: "call",
          results: [
            {
              rawValue: "NOT DETECTED",
              mappingState: "UNRESOLVED",
              resultOptionId: null,
              selectedOption: null,
            },
          ],
        }),
        recordRow("&LOG", { componentId: "comp-LOG", componentCode: "LOG" }),
      ],
    };

    beforeEach(() => {
      getAnalyzerMapping.mockImplementation((_id, callback) =>
        callback(viralLoadMapping),
      );
      getAnalyzerMappingTests.mockImplementation((callback) =>
        callback([viralLoadTest, otherViralLoadTest]),
      );
      getAnalyzerMappingResultOptions.mockImplementation((testId, callback) =>
        callback(
          testId === "9801"
            ? [
                {
                  id: "nd",
                  value: "ND",
                  label: "Not detected",
                  componentId: "comp-call",
                },
                {
                  id: "other",
                  value: "X",
                  label: "Belongs to another component",
                  componentId: "comp-ept",
                },
              ]
            : [],
        ),
      );
      getAnalyzerMappingComponents.mockImplementation((testId, callback) =>
        callback(componentsByTestForRecords[testId] || []),
      );
      saveAnalyzerMapping.mockImplementation((_id, request, callback) =>
        callback(viralLoadMapping),
      );
    });

    const rowFor = (title) =>
      screen
        .getAllByTestId("analyzer-type-mapping-row")
        .find((row) => within(row).queryByText(title, { selector: "strong" }));

    const saveAndReadPayload = async () => {
      await userEvent.click(
        screen.getByRole("button", { name: "Save mapping" }),
      );
      await userEvent.click(
        within(await screen.findByRole("dialog")).getByRole("button", {
          name: "Save changes",
        }),
      );
      await waitFor(() => expect(saveAnalyzerMapping).toHaveBeenCalledTimes(1));
      return saveAnalyzerMapping.mock.calls[0][1];
    };

    it("shows each record as its own row and saves each with its own identity and targets", async () => {
      renderEditor();
      await screen.findByRole("heading", {
        level: 1,
        name: "Cepheid GeneXpert MTB/RIF mappings",
      });

      const main = rowFor("HIVVL");
      expect(rowFor("HIVVL &LOG")).toBeDefined();
      expect(
        within(main).getByRole("combobox", {
          name: "Call result for HIVVL goes to",
        }),
      ).toHaveTextContent("Call");

      await userEvent.click(
        within(main).getByRole("combobox", {
          name: "OpenELIS result for NOT DETECTED",
        }),
      );
      expect(
        screen.queryByRole("option", { name: "Belongs to another component" }),
      ).not.toBeInTheDocument();
      await userEvent.click(
        await screen.findByRole("option", { name: "Not detected" }),
      );

      const payload = await saveAndReadPayload();
      expect(payload.tests).toEqual([
        {
          sourceRowKey: "HIVVL",
          subIdentity: "",
          mappingState: "BOUND",
          testId: "9801",
          componentId: null,
          callComponentId: "comp-call",
        },
        {
          sourceRowKey: "HIVVL",
          subIdentity: "&LOG",
          mappingState: "BOUND",
          testId: "9801",
          componentId: "comp-LOG",
          callComponentId: null,
        },
      ]);
      expect(payload.results).toEqual([
        {
          sourceRowKey: "HIVVL",
          subIdentity: "",
          rawValue: "NOT DETECTED",
          mappingState: "BOUND",
          testResultId: "nd",
        },
      ]);
    });

    it("places a record on the new test's component with the same code when its test changes", async () => {
      renderEditor();
      await screen.findByRole("heading", {
        level: 1,
        name: "Cepheid GeneXpert MTB/RIF mappings",
      });

      const log = rowFor("HIVVL &LOG");
      const picker = within(log).getByRole("combobox", {
        name: "OpenELIS test for HIVVL &LOG",
      });
      await userEvent.clear(picker);
      await userEvent.type(picker, "HIVVLP");
      await userEvent.click(
        await screen.findByRole("option", {
          name: "HIV-1 viral load (plasma) · HIVVLP · 20447-9",
        }),
      );

      await waitFor(() =>
        expect(
          within(log).getByRole("combobox", {
            name: "OpenELIS component for HIVVL &LOG",
          }),
        ).toHaveTextContent("Log viral load"),
      );
      const payload = await saveAndReadPayload();
      expect(payload.tests[1]).toEqual({
        sourceRowKey: "HIVVL",
        subIdentity: "&LOG",
        mappingState: "BOUND",
        testId: "9802",
        componentId: "plasma-LOG",
        callComponentId: null,
      });
    });

    it("lets the operator place a received record the profile does not declare", async () => {
      getAnalyzerMapping.mockImplementation((_id, callback) =>
        callback({
          ...viralLoadMapping,
          tests: [
            ...viralLoadMapping.tests,
            recordRow("HIV-1&EndPt", {
              mappingState: "UNRESOLVED",
              testId: null,
              selectedTest: null,
            }),
          ],
        }),
      );
      renderEditor();
      await screen.findByRole("heading", {
        level: 1,
        name: "Cepheid GeneXpert MTB/RIF mappings",
      });

      const endPoint = rowFor("HIVVL HIV-1&EndPt");
      await userEvent.click(
        within(endPoint).getByRole("combobox", {
          name: "OpenELIS test for HIVVL HIV-1&EndPt",
        }),
      );
      await userEvent.click(
        await screen.findByRole("option", {
          name: "HIV-1 viral load · HIVVL · 20447-9",
        }),
      );
      await userEvent.click(
        within(endPoint).getByRole("combobox", {
          name: "OpenELIS component for HIVVL HIV-1&EndPt",
        }),
      );
      await userEvent.click(
        await screen.findByRole("option", { name: "End point" }),
      );

      const payload = await saveAndReadPayload();
      expect(payload.tests[2]).toEqual({
        sourceRowKey: "HIVVL",
        subIdentity: "HIV-1&EndPt",
        mappingState: "BOUND",
        testId: "9801",
        componentId: "comp-ept",
        callComponentId: null,
      });
    });
  });

  it("returns to the worklist with its unsaved review choices", async () => {
    const worklistDraft = {
      analyzerId: "501",
      page: 2,
      edits: { 1005: { isAccepted: true, note: "Reviewed" } },
    };
    renderEditor({
      pathname: "/analyzers/501/mapping",
      search: "?returnTo=%2FAnalyzerResults%3Fid%3D501",
      state: { worklistDraft },
    });

    await userEvent.click(await screen.findByRole("link", { name: "Back" }));

    expect(screen.getByTestId("return-state")).toHaveTextContent(
      JSON.stringify({ worklistDraft }),
    );
  });

  it("applies the current mapping only to the named analyzer and retries its holds", async () => {
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...mapping,
        confirmation: { ...unconfirmed, state: "CURRENT" },
      }),
    );
    applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
      callback({ id: "501" }),
    );
    renderEditor();

    await userEvent.click(
      await screen.findByRole("button", {
        name: "Apply mappings and retry held results",
      }),
    );

    expect(applyAnalyzerMapping).toHaveBeenCalledWith(
      "501",
      {
        mappingId: mapping.mappingId,
        revision: mapping.mappingRevision,
        mappingFingerprint: mapping.mappingFingerprint,
      },
      expect.any(Function),
    );
    expect(
      screen.getByText(
        "Current mappings applied to this analyzer. Eligible held results were retried.",
      ),
    ).toBeVisible();
  });

  it.each(["UNCONFIRMED", "STALE"])(
    "does not apply a %s mapping to held results",
    async (state) => {
      getAnalyzerMapping.mockImplementation((_id, callback) =>
        callback({ ...mapping, confirmation: { ...unconfirmed, state } }),
      );
      renderEditor();

      expect(
        await screen.findByRole("button", {
          name: "Apply mappings and retry held results",
        }),
      ).toBeDisabled();
      expect(applyAnalyzerMapping).not.toHaveBeenCalled();
    },
  );

  it("restores a bookmarkable analyzer mapping with breadcrumbs and every independent source row", async () => {
    renderEditor();

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Cepheid GeneXpert MTB/RIF mappings",
      }),
    ).toBeVisible();
    expect(document.querySelectorAll("h1")).toHaveLength(1);
    expect(getAnalyzerMapping).toHaveBeenCalledWith(
      "501",
      expect.any(Function),
    );
    expect(getAnalyzerTypeRevision).not.toHaveBeenCalled();
    expect(screen.getByText("This analyzer's own mapping")).toBeVisible();

    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(
      within(breadcrumb).getByRole("link", { name: "Analyzers" }),
    ).toHaveAttribute("href", "/analyzers");
    expect(breadcrumb.querySelector('[aria-current="page"]')).toHaveTextContent(
      "Cepheid GeneXpert MTB/RIF mappings",
    );

    const sourceRows = screen.getAllByTestId("analyzer-type-mapping-row");
    expect(sourceRows).toHaveLength(3);
    expect(within(sourceRows[0]).getByText("RAW-A")).toBeVisible();
    expect(
      within(sourceRows[0]).getByRole("combobox", {
        name: "OpenELIS result for DETECTED",
      }),
    ).toHaveTextContent("Resistant");
    expect(within(sourceRows[1]).getByText("RAW-B")).toBeVisible();
    expect(screen.getAllByText("Shared normalized identity")).toHaveLength(2);
    expect(screen.getByText("Alias: RAW A")).toBeVisible();
    expect(screen.getByText("Specimen ID starts with CPOS")).toBeVisible();
    expect(
      screen.queryByText("SERVER DESCRIPTION MUST NOT RENDER"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("GeneXpert - Main Lab")).not.toBeInTheDocument();
    expect(screen.queryByText(/regex/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/operational QC/i)).not.toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(
      "/analyzers/501/mapping?returnTo=%2FAnalyzerResults%3Fid%3D501",
    );
  });

  it("confirms partial mappings without excluding unresolved rows", async () => {
    renderEditor();
    const button = await screen.findByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    expect(button).toBeEnabled();
    expect(
      screen.getByText(/unresolved mappings.*saved for correction/),
    ).toBeVisible();
    await userEvent.click(button);
    const request = confirmAnalyzerMapping.mock.calls[0][1];
    expect(request.confirmedRows).toContainEqual({
      sourceRowKey: "RAW-A",
      subIdentity: "",
      rawValue: "DETECTED",
    });
    expect(request.confirmedRows).not.toContainEqual({
      sourceRowKey: "RAW-A",
      subIdentity: "",
      rawValue: "NOT DETECTED",
    });
    expect(request.excludedRows).not.toContainEqual({
      sourceRowKey: "RAW-A",
      subIdentity: "",
      rawValue: "NOT DETECTED",
    });
    expect(request.excludedRows).not.toContainEqual({
      sourceRowKey: "RAW-B",
      subIdentity: "",
      rawValue: null,
    });
  });

  it("renders explicit NONE recognition without server-authored technical details", async () => {
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...mapping,
        controlRecognition: {
          recognitionFingerprint: `sha256:${"d".repeat(64)}`,
          mode: "NONE",
          description: "SERVER NONE DESCRIPTION MUST NOT RENDER",
          affirmedNoControlResults: true,
          conditions: [],
        },
      }),
    );

    renderEditor();

    expect(
      await screen.findByText(
        "This Analyzer Type explicitly declares that the interface transports no control results.",
      ),
    ).toBeVisible();
    expect(
      screen.queryByText("SERVER NONE DESCRIPTION MUST NOT RENDER"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/regex/i)).not.toBeInTheDocument();
  });

  it("shows unconfigured rules without claiming the interface sends no controls", async () => {
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...mapping,
        controlRecognition: {
          ...recognition,
          description: "SERVER DESCRIPTION MUST NOT RENDER",
          conditions: [],
        },
      }),
    );

    renderEditor();

    expect(
      await screen.findAllByText("Control recognition not configured"),
    ).toHaveLength(2);
    expect(
      screen.getByText(
        "No control recognition rules are configured. Control results may not be identified automatically.",
      ),
    ).toBeVisible();
    expect(
      screen.queryByText("This interface does not transmit control results"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("SERVER DESCRIPTION MUST NOT RENDER"),
    ).not.toBeInTheDocument();
  });

  it("opens and focuses the held analyzer value named in the bookmark", async () => {
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...mapping,
        tests: mapping.tests.map((test) =>
          test.sourceRowKey === "RAW-A"
            ? {
                ...test,
                results: test.results.map((result) =>
                  result.rawValue === "NOT DETECTED"
                    ? { ...result, observed: true }
                    : result,
                ),
              }
            : test,
        ),
      }),
    );

    renderEditor(
      "/analyzers/501/mapping?returnTo=%2FAnalyzerResults%3Fid%3D2001&focusTest=RAW-A&focusValue=NOT+DETECTED",
    );

    expect(await screen.findByText("Observed in held results")).toBeVisible();
    await waitFor(() =>
      expect(
        screen.getByRole("combobox", {
          name: "OpenELIS result for NOT DETECTED",
        }),
      ).toHaveFocus(),
    );
  });

  it("repoints one row by LOINC without blocking independent unresolved rows", async () => {
    renderEditor();
    await screen.findByRole("heading", {
      level: 1,
      name: "Cepheid GeneXpert MTB/RIF mappings",
    });

    const rawC = screen
      .getAllByTestId("analyzer-type-mapping-row")
      .find((row) => within(row).queryByText("RAW-C"));
    const picker = within(rawC).getByRole("combobox", {
      name: "OpenELIS test for RAW-C",
    });
    await userEvent.click(picker);
    await userEvent.type(picker, "94558-4");
    expect(
      screen.queryByRole("option", {
        name: "Rifampin Resistance · RIF · 46244-0",
      }),
    ).not.toBeInTheDocument();
    await userEvent.click(
      await screen.findByRole("option", {
        name: "Unconfigured qualitative test · UNCONFIGURED · 94558-4",
      }),
    );

    expect(
      within(rawC).getByRole("combobox", {
        name: "OpenELIS test for RAW-C",
      }),
    ).toHaveValue("Unconfigured qualitative test · UNCONFIGURED · 94558-4");
    expect(
      within(rawC).getByRole("link", {
        name: "Add result options in Test Catalog",
      }),
    ).toHaveAttribute(
      "href",
      expect.stringContaining(
        "/TestCatalogEditor/9703/sample-results?returnTo=",
      ),
    );
    expect(within(rawC).getByText("HIGH")).toBeVisible();
    const rawB = screen
      .getAllByTestId("analyzer-type-mapping-row")
      .find((row) => within(row).queryByText("RAW-B"));
    expect(
      within(rawB).getByRole("combobox", {
        name: "OpenELIS test for RAW-B",
      }),
    ).toHaveValue("");
    expect(
      within(rawB).getByText("Suggested match: COVID-19 PCR"),
    ).toBeVisible();
    expect(screen.getByRole("button", { name: "Save mapping" })).toBeEnabled();
  });

  it("saves independent catalog-bound decisions and confirms exact evidence", async () => {
    const saved = {
      ...mapping,
      mappingRevision: 4,
      mappingFingerprint: `sha256:${"d".repeat(64)}`,
      tests: mapping.tests.map((test) => {
        if (test.sourceRowKey === "RAW-A") {
          return {
            ...test,
            results: test.results.map((result) =>
              result.rawValue === "NOT DETECTED"
                ? {
                    ...result,
                    mappingState: "BOUND",
                    resultOptionId: "812",
                    selectedOption: resultOptions["9701"][1],
                  }
                : result,
            ),
          };
        }
        if (test.sourceRowKey === "RAW-B") {
          return {
            ...test,
            mappingState: "BOUND",
            testId: "9702",
            selectedTest: catalogTests[1],
          };
        }
        return {
          ...test,
          mappingState: "EXCLUDED",
          results: test.results.map((result) => ({
            ...result,
            mappingState: "EXCLUDED",
          })),
        };
      }),
      confirmation: { ...unconfirmed, state: "STALE" },
    };
    saveAnalyzerMapping.mockImplementation((_id, _request, callback) =>
      callback(saved),
    );
    confirmAnalyzerMapping.mockImplementation((_id, request, callback) =>
      callback({
        state: "CURRENT",
        profileId: mapping.profileId,
        profileRevision: mapping.profileRevision,
        mappingFingerprint: request.baseMappingFingerprint,
        recognitionFingerprint: request.recognitionFingerprint,
        confirmedBy: "17",
        confirmedByDisplayName: "Lab Admin",
        confirmedAt: "2026-08-22T12:00:00Z",
        confirmedRows: request.confirmedRows,
        excludedRows: request.excludedRows,
      }),
    );

    renderEditor();
    await screen.findByRole("heading", {
      level: 1,
      name: "Cepheid GeneXpert MTB/RIF mappings",
    });

    const rawB = screen
      .getAllByTestId("analyzer-type-mapping-row")
      .find((row) => within(row).queryByText("RAW-B"));
    await userEvent.click(
      within(rawB).getByRole("button", { name: "Use suggested test" }),
    );

    const rawC = screen
      .getAllByTestId("analyzer-type-mapping-row")
      .find((row) => within(row).queryByText("RAW-C"));
    await userEvent.click(
      within(rawC).getByRole("checkbox", {
        name: "Do not receive RAW-C",
      }),
    );

    const rawA = screen
      .getAllByTestId("analyzer-type-mapping-row")
      .find((row) => within(row).queryByText("RAW-A"));
    await userEvent.click(
      within(rawA).getByRole("combobox", {
        name: "OpenELIS result for NOT DETECTED",
      }),
    );
    await userEvent.click(
      await screen.findByRole("option", { name: "Susceptible" }),
    );

    const save = screen.getByRole("button", { name: "Save mapping" });
    expect(save).toBeEnabled();
    await userEvent.click(save);
    await userEvent.click(
      within(await screen.findByRole("dialog")).getByRole("button", {
        name: "Save changes",
      }),
    );

    await waitFor(() => expect(saveAnalyzerMapping).toHaveBeenCalledTimes(1));
    expect(saveAnalyzerMapping.mock.calls[0].slice(0, 2)).toEqual([
      "501",
      {
        baseMappingFingerprint: mapping.mappingFingerprint,
        tests: [
          {
            sourceRowKey: "RAW-A",
            subIdentity: "",
            mappingState: "BOUND",
            testId: "9701",
            componentId: null,
            callComponentId: null,
          },
          {
            sourceRowKey: "RAW-B",
            subIdentity: "",
            mappingState: "BOUND",
            testId: "9702",
            componentId: null,
            callComponentId: null,
          },
          {
            sourceRowKey: "RAW-C",
            subIdentity: "",
            mappingState: "EXCLUDED",
            testId: null,
            componentId: null,
            callComponentId: null,
          },
        ],
        results: [
          {
            sourceRowKey: "RAW-A",
            subIdentity: "",
            rawValue: "DETECTED",
            mappingState: "BOUND",
            testResultId: "811",
          },
          {
            sourceRowKey: "RAW-A",
            subIdentity: "",
            rawValue: "NOT DETECTED",
            mappingState: "BOUND",
            testResultId: "812",
          },
          {
            sourceRowKey: "RAW-C",
            subIdentity: "",
            rawValue: "HIGH",
            mappingState: "EXCLUDED",
            testResultId: null,
          },
        ],
      },
    ]);

    expect(screen.getByText("Mappings saved")).toBeVisible();
    const confirm = screen.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    expect(confirm).toBeEnabled();
    await userEvent.click(confirm);

    await waitFor(() =>
      expect(confirmAnalyzerMapping).toHaveBeenCalledWith(
        "501",
        {
          baseMappingFingerprint: saved.mappingFingerprint,
          recognitionFingerprint: recognition.recognitionFingerprint,
          confirmedRows: [
            { sourceRowKey: "RAW-A", subIdentity: "", rawValue: null },
            { sourceRowKey: "RAW-A", subIdentity: "", rawValue: "DETECTED" },
            {
              sourceRowKey: "RAW-A",
              subIdentity: "",
              rawValue: "NOT DETECTED",
            },
            { sourceRowKey: "RAW-B", subIdentity: "", rawValue: null },
          ],
          excludedRows: [
            { sourceRowKey: "RAW-C", subIdentity: "", rawValue: null },
            { sourceRowKey: "RAW-C", subIdentity: "", rawValue: "HIGH" },
          ],
        },
        expect.any(Function),
      ),
    );
    expect(
      screen.getByText(/Confirmed by Lab Admin on Aug 22, 2026/),
    ).toBeVisible();
    expect(screen.getByText("Current confirmation")).toBeVisible();
  });

  it("keeps the selections of an excluded test so that un-excluding restores them", async () => {
    renderEditor();
    await screen.findByRole("heading", {
      level: 1,
      name: "Cepheid GeneXpert MTB/RIF mappings",
    });
    const rawA = () =>
      screen
        .getAllByTestId("analyzer-type-mapping-row")
        .find((row) => within(row).queryByText("RAW-A"));

    await userEvent.click(
      within(rawA()).getByRole("checkbox", { name: "Do not receive RAW-A" }),
    );
    expect(
      within(rawA()).getByRole("combobox", { name: "OpenELIS test for RAW-A" }),
    ).toBeDisabled();

    await userEvent.click(
      within(rawA()).getByRole("checkbox", { name: "Do not receive RAW-A" }),
    );

    expect(
      within(rawA()).getByRole("combobox", { name: "OpenELIS test for RAW-A" }),
    ).toHaveValue("Rifampin Resistance · RIF · 46244-0");
    expect(
      within(rawA()).getByRole("combobox", {
        name: "OpenELIS result for DETECTED",
      }),
    ).toHaveTextContent("Resistant");
    // Back where it started, so Save has nothing to write.
    await userEvent.click(screen.getByRole("button", { name: "Save mapping" }));
    expect(
      within(await screen.findByRole("dialog")).getByText(
        "Nothing has changed since the last save.",
      ),
    ).toBeVisible();
  });

  it("keeps a result the operator excluded excluded when its test is excluded and un-excluded", async () => {
    renderEditor();
    await screen.findByRole("heading", {
      level: 1,
      name: "Cepheid GeneXpert MTB/RIF mappings",
    });
    const rawA = () =>
      screen
        .getAllByTestId("analyzer-type-mapping-row")
        .find((row) => within(row).queryByText("RAW-A"));
    const detected = () =>
      within(rawA()).getByRole("checkbox", {
        name: "Do not receive DETECTED",
      });

    await userEvent.click(detected());
    await userEvent.click(
      within(rawA()).getByRole("checkbox", { name: "Do not receive RAW-A" }),
    );
    await userEvent.click(
      within(rawA()).getByRole("checkbox", { name: "Do not receive RAW-A" }),
    );

    expect(detected()).toBeChecked();
  });

  it("names the old and new test when a mapped row is pointed at another test", async () => {
    renderEditor();
    await screen.findByRole("heading", {
      level: 1,
      name: "Cepheid GeneXpert MTB/RIF mappings",
    });
    const rawA = screen
      .getAllByTestId("analyzer-type-mapping-row")
      .find((row) => within(row).queryByText("RAW-A"));
    const picker = within(rawA).getByRole("combobox", {
      name: "OpenELIS test for RAW-A",
    });
    await userEvent.clear(picker);
    await userEvent.type(picker, "94558-4");
    await userEvent.click(
      await screen.findByRole("option", {
        name: "Unconfigured qualitative test · UNCONFIGURED · 94558-4",
      }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Save mapping" }));

    const changes = within(
      await screen.findByTestId("analyzer-mapping-changes"),
    );
    expect(
      changes.getByText("Rifampin Resistance to Unconfigured qualitative test"),
    ).toBeVisible();
    expect(changes.getByText("Resistant to Needs mapping")).toBeVisible();
  });

  it("lists every mapped row that becomes excluded before Save writes it", async () => {
    renderEditor();
    await screen.findByRole("heading", {
      level: 1,
      name: "Cepheid GeneXpert MTB/RIF mappings",
    });
    const rawA = screen
      .getAllByTestId("analyzer-type-mapping-row")
      .find((row) => within(row).queryByText("RAW-A"));

    await userEvent.click(
      within(rawA).getByRole("checkbox", { name: "Do not receive RAW-A" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Save mapping" }));

    const changes = within(
      await screen.findByTestId("analyzer-mapping-changes"),
    );
    expect(changes.getByText("RAW-A")).toBeVisible();
    expect(changes.getByText("RAW-A / DETECTED")).toBeVisible();
    expect(
      changes.getByText("Rifampin Resistance to Do not receive"),
    ).toBeVisible();
    expect(changes.getByText("Resistant to Do not receive")).toBeVisible();
    expect(saveAnalyzerMapping).not.toHaveBeenCalled();
  });

  it("marks a row the operator edited and leaves defaults unmarked", async () => {
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...mapping,
        tests: mapping.tests.map((test) =>
          test.sourceRowKey === "RAW-A"
            ? { ...test, origin: "OVERRIDE" }
            : { ...test, origin: "DEFAULT" },
        ),
      }),
    );
    renderEditor();
    await screen.findByRole("heading", {
      level: 1,
      name: "Cepheid GeneXpert MTB/RIF mappings",
    });

    expect(screen.getAllByText("Edited")).toHaveLength(1);
  });

  it("shows a type's defaults read-only, with no way to save, confirm or apply them", async () => {
    renderDefaults();

    expect(await screen.findByText("Defaults for new analyzers")).toBeVisible();
    expect(getAnalyzerTypeDefaults).toHaveBeenCalledWith(
      "shipped.genexpert",
      2,
      expect.any(Function),
    );
    expect(getAnalyzerMapping).not.toHaveBeenCalled();
    expect(screen.getByText("GeneXpert - Main Lab")).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Save mapping" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: "Confirm mappings and control recognition",
      }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: "Apply mappings and retry held results",
      }),
    ).not.toBeInTheDocument();
    screen
      .getAllByRole("combobox", { name: /OpenELIS test for/ })
      .forEach((picker) => expect(picker).toBeDisabled());
    screen
      .getAllByRole("checkbox")
      .forEach((checkbox) => expect(checkbox).toBeDisabled());
  });
});
