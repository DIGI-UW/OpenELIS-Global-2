import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getAnalyzerMapping,
  saveAnalyzerMapping,
} from "../../../services/analyzerService";
import AnalyzerAssaysSetup from "./AnalyzerAssaysSetup";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzerMapping: vi.fn(),
  saveAnalyzerMapping: vi.fn(),
}));

const assay = (code, overrides) => ({
  sourceRowKey: code,
  rawCode: code,
  subIdentity: "",
  testNameHint: null,
  mappingState: "UNRESOLVED",
  testId: null,
  componentId: null,
  callComponentId: null,
  selectedTest: null,
  unresolvedReason: null,
  enabled: true,
  instrumentCode: null,
  results: [],
  ...overrides,
});

const mapping = {
  analyzerId: "42",
  mappingFingerprint: `sha256:${"b".repeat(64)}`,
  tests: [
    assay("MTB-RIF", {
      mappingState: "BOUND",
      testId: "9701",
      selectedTest: { id: "9701", name: "Xpert MTB/RIF" },
      results: [
        {
          rawValue: "DETECTED",
          mappingState: "BOUND",
          resultOptionId: "811",
        },
      ],
    }),
    assay("MTB-RIF", {
      subIdentity: "Rif&Ct",
      mappingState: "BOUND",
      testId: "9701",
      componentId: "c1",
    }),
    assay("HBV-VL", { unresolvedReason: "AMBIGUOUS" }),
    assay("FLU", { unresolvedReason: "NO_MATCH", enabled: false }),
  ],
};

const row = (code) => screen.getByTestId(`analyzer-assay-${code}`);

const renderAssays = (onContinue = vi.fn()) => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <AnalyzerAssaysSetup analyzerId="42" onContinue={onContinue} />
    </IntlProvider>,
  );
  return onContinue;
};

describe("AnalyzerAssaysSetup", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzerMapping.mockImplementation((_id, callback) => callback(mapping));
  });

  it("lists each assay once, says how the catalog matched it, and shows which are on", async () => {
    renderAssays();

    expect(await screen.findByText("2 of 3 assays on")).toBeInTheDocument();
    expect(screen.getAllByTestId(/^analyzer-assay-/)).toHaveLength(3);
    expect(
      within(row("MTB-RIF")).getByText("Matches Xpert MTB/RIF"),
    ).toBeVisible();
    expect(
      within(row("HBV-VL")).getByText(
        "Several tests in this lab's catalog match",
      ),
    ).toBeVisible();
    expect(
      within(row("FLU")).getByText("No test in this lab's catalog"),
    ).toBeVisible();
    expect(
      within(row("FLU")).getByRole("checkbox", { name: /FLU/ }),
    ).not.toBeChecked();
  });

  it("saves the assays turned on and the codes this instrument sends, then continues", async () => {
    saveAnalyzerMapping.mockImplementation((_id, _update, callback) =>
      callback({ ...mapping, tests: mapping.tests }),
    );
    const onContinue = renderAssays();

    await userEvent.click(
      within(await screen.findByTestId("analyzer-assay-FLU")).getByRole(
        "checkbox",
        { name: /FLU/ },
      ),
    );
    const code = within(row("MTB-RIF")).getByRole("textbox", {
      name: "Code the instrument sends for MTB-RIF",
    });
    await userEvent.type(code, "MTBU");
    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Verify" }),
    );

    const [analyzerId, update] = saveAnalyzerMapping.mock.calls[0];
    expect(analyzerId).toBe("42");
    expect(update.baseMappingFingerprint).toBe(mapping.mappingFingerprint);
    const main = (key) =>
      update.tests.find(
        (test) => test.sourceRowKey === key && test.subIdentity === "",
      );
    expect(main("FLU")).toMatchObject({ enabled: true });
    expect(main("MTB-RIF")).toMatchObject({
      enabled: true,
      instrumentCode: "MTBU",
      mappingState: "BOUND",
      testId: "9701",
    });
    expect(update.tests).toHaveLength(4);
    expect(update.results).toEqual([
      {
        sourceRowKey: "MTB-RIF",
        subIdentity: "",
        rawValue: "DETECTED",
        mappingState: "BOUND",
        testResultId: "811",
      },
    ]);
    expect(onContinue).toHaveBeenCalled();
  });

  it("continues without saving when nothing changed", async () => {
    const onContinue = renderAssays();

    await userEvent.click(
      await screen.findByRole("button", { name: "Continue to Verify" }),
    );

    expect(saveAnalyzerMapping).not.toHaveBeenCalled();
    expect(onContinue).toHaveBeenCalled();
  });

  it("explains an analyzer type that declares no assays", async () => {
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({ ...mapping, tests: [] }),
    );
    renderAssays();

    expect(
      await screen.findByText(messages["analyzer.setup.assays.none"]),
    ).toBeInTheDocument();
  });
});
