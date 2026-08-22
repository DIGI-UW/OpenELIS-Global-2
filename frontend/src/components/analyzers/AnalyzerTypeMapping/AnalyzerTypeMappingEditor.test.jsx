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
  confirmAnalyzerTypeMapping,
  getAnalyzerMappingResultOptions,
  getAnalyzerMappingTests,
  getAnalyzerTypeMapping,
  getAnalyzerTypeRevision,
  saveAnalyzerTypeMapping,
} from "../../../services/analyzerService";
import AnalyzerTypeMappingEditor from "./AnalyzerTypeMappingEditor";

vi.mock("../../../services/analyzerService", () => ({
  confirmAnalyzerTypeMapping: vi.fn(),
  getAnalyzerMappingResultOptions: vi.fn(),
  getAnalyzerMappingTests: vi.fn(),
  getAnalyzerTypeMapping: vi.fn(),
  getAnalyzerTypeRevision: vi.fn(),
  saveAnalyzerTypeMapping: vi.fn(),
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
  bindingFingerprint: null,
  recognitionFingerprint: null,
  confirmedBy: null,
  confirmedAt: null,
  confirmedRows: [],
  excludedRows: [],
};

const mapping = {
  profileId: "shipped.genexpert",
  profileRevision: 2,
  profileFingerprint: `sha256:${"a".repeat(64)}`,
  displayName: "Cepheid GeneXpert MTB/RIF",
  protocol: "ASTM",
  siteBindingId: "11",
  siteBindingRevision: 3,
  bindingFingerprint: `sha256:${"b".repeat(64)}`,
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

const LocationProbe = () => {
  const location = useLocation();
  return (
    <output data-testid="location">
      {location.pathname + location.search}
    </output>
  );
};

const renderEditor = (
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
    getAnalyzerTypeMapping.mockImplementation(
      (_profileId, _revision, callback) => callback(mapping),
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
  });

  it("restores a bookmarkable shared-type editor with breadcrumbs and every independent source row", async () => {
    renderEditor();

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Cepheid GeneXpert MTB/RIF mappings",
      }),
    ).toBeVisible();
    expect(document.querySelectorAll("h1")).toHaveLength(1);
    expect(getAnalyzerTypeMapping).toHaveBeenCalledWith(
      "shipped.genexpert",
      2,
      expect.any(Function),
    );

    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(
      within(breadcrumb).getByRole("link", { name: "Analyzers" }),
    ).toHaveAttribute("href", "/analyzers");
    expect(
      within(breadcrumb).getByRole("link", { name: "Analyzer Types" }),
    ).toHaveAttribute("href", "/analyzers/types?mapping=INCOMPLETE");
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
    expect(screen.getByText("GeneXpert - Main Lab")).toBeVisible();
    expect(screen.getByText("GeneXpert - TB Bench")).toBeVisible();
    expect(screen.getByText("GeneXpert - Reference Lab")).toBeVisible();
    expect(screen.getByText("Update available")).toBeVisible();
    expect(screen.queryByText(/regex/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/operational QC/i)).not.toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(
      "revision=2&returnTo=%2Fanalyzers%2Ftypes%3Fmapping%3DINCOMPLETE",
    );
  });

  it("renders explicit NONE recognition without server-authored technical details", async () => {
    getAnalyzerTypeMapping.mockImplementation(
      (_profileId, _revision, callback) =>
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
    expect(
      screen.getByRole("button", { name: "Update shared mappings" }),
    ).toBeEnabled();
  });

  it("saves independent catalog-bound decisions and confirms exact evidence", async () => {
    const saved = {
      ...mapping,
      siteBindingRevision: 4,
      bindingFingerprint: `sha256:${"d".repeat(64)}`,
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
    saveAnalyzerTypeMapping.mockImplementation(
      (_profileId, _revision, _request, callback) => callback(saved),
    );
    confirmAnalyzerTypeMapping.mockImplementation(
      (_profileId, _revision, request, callback) =>
        callback({
          state: "CURRENT",
          profileId: mapping.profileId,
          profileRevision: mapping.profileRevision,
          bindingFingerprint: request.baseBindingFingerprint,
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

    const save = screen.getByRole("button", {
      name: "Update shared mappings",
    });
    expect(save).toBeEnabled();
    await userEvent.click(save);

    await waitFor(() =>
      expect(saveAnalyzerTypeMapping).toHaveBeenCalledTimes(1),
    );
    expect(saveAnalyzerTypeMapping.mock.calls[0].slice(0, 3)).toEqual([
      "shipped.genexpert",
      2,
      {
        baseBindingFingerprint: mapping.bindingFingerprint,
        tests: [
          { sourceRowKey: "RAW-A", mappingState: "BOUND", testId: "9701" },
          { sourceRowKey: "RAW-B", mappingState: "BOUND", testId: "9702" },
          { sourceRowKey: "RAW-C", mappingState: "EXCLUDED", testId: null },
        ],
        results: [
          {
            sourceRowKey: "RAW-A",
            rawValue: "DETECTED",
            mappingState: "BOUND",
            testResultId: "811",
          },
          {
            sourceRowKey: "RAW-A",
            rawValue: "NOT DETECTED",
            mappingState: "BOUND",
            testResultId: "812",
          },
          {
            sourceRowKey: "RAW-C",
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
      expect(confirmAnalyzerTypeMapping).toHaveBeenCalledWith(
        "shipped.genexpert",
        2,
        {
          baseBindingFingerprint: saved.bindingFingerprint,
          recognitionFingerprint: recognition.recognitionFingerprint,
          confirmedRows: [
            { sourceRowKey: "RAW-A", rawValue: null },
            { sourceRowKey: "RAW-A", rawValue: "DETECTED" },
            { sourceRowKey: "RAW-A", rawValue: "NOT DETECTED" },
            { sourceRowKey: "RAW-B", rawValue: null },
          ],
          excludedRows: [
            { sourceRowKey: "RAW-C", rawValue: null },
            { sourceRowKey: "RAW-C", rawValue: "HIGH" },
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
});
