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
  adoptAnalyzerRevision,
  getAnalyzerAdoption,
  getAnalyzerMappingComponents,
  getAnalyzerMappingResultOptions,
  getAnalyzerMappingTests,
} from "../../../services/analyzerService";
import AnalyzerTypeMappingEditor from "./AnalyzerTypeMappingEditor";

vi.mock("../../../services/analyzerService", () => ({
  adoptAnalyzerRevision: vi.fn(),
  applyAnalyzerMapping: vi.fn(),
  confirmAnalyzerMapping: vi.fn(),
  getAnalyzerAdoption: vi.fn(),
  getAnalyzerMapping: vi.fn(),
  getAnalyzerMappingComponents: vi.fn(),
  getAnalyzerMappingResultOptions: vi.fn(),
  getAnalyzerMappingTests: vi.fn(),
  getAnalyzerTypeDefaults: vi.fn(),
  getAnalyzerTypeRevision: vi.fn(),
  saveAnalyzerMapping: vi.fn(),
}));

const glucose = { id: "9701", name: "Glucose", code: "GLU", loincCodes: [] };
const fastingGlucose = {
  id: "9702",
  name: "Fasting glucose",
  code: "FGLU",
  loincCodes: [],
};
const sodium = { id: "9703", name: "Sodium", code: "NA", loincCodes: [] };
const retiredTest = {
  id: "9704",
  name: "Old assay",
  code: "OLD",
  loincCodes: [],
};

const row = (code, overrides) => ({
  sourceRowKey: code,
  rawCode: code,
  aliases: [],
  testNameHint: null,
  loinc: null,
  unit: null,
  resultType: "quantitative",
  normalizedCoding: null,
  mappingState: "UNRESOLVED",
  origin: "DEFAULT",
  testId: null,
  selectedTest: null,
  suggestedTest: null,
  results: [],
  subIdentity: "",
  ...overrides,
});

const decision = (code, testId, origin = "DEFAULT") => ({
  test: {
    sourceRowKey: code,
    subIdentity: "",
    mappingState: testId ? "BOUND" : "UNRESOLVED",
    testId,
    origin,
  },
  results: [],
});

const planRow = (code, bucket, extra = {}) => ({
  key: { sourceRowKey: code, subIdentity: "" },
  bucket,
  current: null,
  newDefault: null,
  proposed: null,
  alsoDefault: false,
  blockReason: null,
  ...extra,
});

const review = (rows) => ({
  analyzerId: "501",
  profileId: "shipped.chemistry",
  fromRevision: 1,
  toRevision: 2,
  rows,
  proposal: {
    analyzerId: "501",
    profileId: "shipped.chemistry",
    profileRevision: 2,
    profileFingerprint: `sha256:${"2".repeat(64)}`,
    displayName: "Chemistry analyzer",
    protocol: "ASTM",
    mappingId: null,
    mappingRevision: 0,
    mappingFingerprint: null,
    tests: [
      row("NA", {
        mappingState: "BOUND",
        testId: sodium.id,
        selectedTest: sodium,
      }),
      row("GLU", {
        mappingState: "BOUND",
        origin: "OVERRIDE",
        testId: fastingGlucose.id,
        selectedTest: fastingGlucose,
      }),
      row("K"),
    ],
    controlRecognition: {
      recognitionFingerprint: `sha256:${"c".repeat(64)}`,
      mode: "NONE",
      conditions: [],
    },
    confirmation: { state: "UNCONFIRMED" },
  },
});

const changedGlucose = planRow("GLU", "CHANGED", {
  current: decision("GLU", fastingGlucose.id, "OVERRIDE"),
  newDefault: decision("GLU", glucose.id),
  proposed: decision("GLU", fastingGlucose.id, "OVERRIDE"),
});

const plan = review([
  planRow("NA", "UNCHANGED", {
    current: decision("NA", sodium.id),
    newDefault: decision("NA", sodium.id),
    proposed: decision("NA", sodium.id),
  }),
  changedGlucose,
  planRow("K", "NEEDS_MAPPING", {
    newDefault: decision("K", null),
    proposed: decision("K", null),
  }),
  planRow("OLD", "RETIRED", { current: decision("OLD", retiredTest.id) }),
]);

const LocationProbe = () => {
  const location = useLocation();
  return (
    <output data-testid="location">
      {location.pathname}|{JSON.stringify(location.state || null)}
    </output>
  );
};

const renderAdoption = () =>
  render(
    <MemoryRouter initialEntries={["/analyzers/501/adoption?revision=2"]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:analyzerId/adoption">
          <AnalyzerTypeMappingEditor />
        </Route>
        <LocationProbe />
      </IntlProvider>
    </MemoryRouter>,
  );

const section = (name) =>
  screen.getByRole("heading", { name }).closest("section");

describe("AnalyzerTypeMappingEditor adopting a newer revision", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzerAdoption.mockImplementation((_id, _revision, callback) =>
      callback(plan),
    );
    getAnalyzerMappingTests.mockImplementation((callback) =>
      callback([glucose, fastingGlucose, sodium, retiredTest]),
    );
    getAnalyzerMappingResultOptions.mockImplementation((_id, callback) =>
      callback([]),
    );
    getAnalyzerMappingComponents.mockImplementation((_id, callback) =>
      callback([]),
    );
  });

  it("groups each record under what adoption does to it", async () => {
    renderAdoption();

    expect(
      await screen.findByRole("heading", {
        name: "Adopt revision 2 of Chemistry analyzer",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Adopting revision 2")).toBeInTheDocument();
    expect(screen.queryByText("Confirmation")).not.toBeInTheDocument();
    expect(getAnalyzerAdoption).toHaveBeenCalledWith(
      "501",
      2,
      expect.any(Function),
    );
    const record = (bucket, code) =>
      within(section(bucket)).getByRole("button", {
        name: new RegExp(`^${code}\\b`),
      });
    expect(record("Changed", "GLU")).toBeInTheDocument();
    expect(record("Needs mapping", "K")).toBeInTheDocument();
    expect(record("Unchanged", "NA")).toBeInTheDocument();
    expect(
      within(section("No longer sent")).getByText("OLD"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: "Apply mappings and retry held results",
      }),
    ).not.toBeInTheDocument();
  });

  it("shows a changed row's current choice beside the new default, saves the one taken and goes on to confirm", async () => {
    adoptAnalyzerRevision.mockImplementation(
      (_id, _revision, _update, callback) =>
        callback({
          mappingId: "12",
          mappingRevision: 4,
          mappingFingerprint: `sha256:${"4".repeat(64)}`,
        }),
    );
    renderAdoption();

    const changed = await screen.findByTestId("adoption-comparison-GLU");
    expect(
      within(changed).getByText("Current: Fasting glucose"),
    ).toBeInTheDocument();
    expect(
      within(changed).getByText("New default: Glucose"),
    ).toBeInTheDocument();
    expect(
      within(changed).getByRole("button", { name: "Keep current" }),
    ).toBeDisabled();
    await userEvent.click(
      within(changed).getByRole("button", { name: "Use new default" }),
    );
    expect(
      within(changed).getByRole("button", { name: "Use new default" }),
    ).toBeDisabled();
    expect(
      within(changed).getByRole("button", { name: "Keep current" }),
    ).toBeEnabled();
    await userEvent.click(
      screen.getByRole("button", { name: "Save as revision 2" }),
    );

    const [analyzerId, revision, update] = adoptAnalyzerRevision.mock.calls[0];
    expect(analyzerId).toBe("501");
    expect(revision).toBe(2);
    expect(
      update.tests.find((test) => test.sourceRowKey === "GLU"),
    ).toMatchObject({ mappingState: "BOUND", testId: glucose.id });
    await waitFor(() =>
      expect(screen.getByTestId("location")).toHaveTextContent(
        '/analyzers/501/mapping|{"adoptedRevision":2}',
      ),
    );
  });

  it("will not save while a dropped record still has held results", async () => {
    getAnalyzerAdoption.mockImplementation((_id, _revision, callback) =>
      callback({
        ...plan,
        rows: [
          ...plan.rows,
          planRow("GONE", "BLOCKED", {
            current: decision("GONE", retiredTest.id),
            blockReason: "HELD_RESULTS",
          }),
        ],
      }),
    );
    renderAdoption();

    expect(
      await screen.findByText(
        "GONE still has held results from revision 1. Resolve them before adopting.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Save as revision 2" }),
    ).toBeDisabled();
  });
});
