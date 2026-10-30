import React from "react";
import { act, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { createMemoryHistory } from "history";
import { BrowserRouter, Router, useLocation } from "react-router-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  activateAnalyzer,
  startAnalyzerSetupReception,
  applyAnalyzerMapping,
  createAnalyzer,
  getAnalyzer,
  getAnalyzerMapping,
  getAnalyzerMappingComponents,
  getAnalyzerMappingResultOptions,
  getAnalyzerMappingTests,
  getAnalyzerActivationReadiness,
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
  testConnection,
  updateAnalyzer,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";
import AnalyzerSetup from "./AnalyzerSetup";

vi.mock("../../../services/analyzerService", () => ({
  activateAnalyzer: vi.fn(),
  startAnalyzerSetupReception: vi.fn(),
  deactivateAnalyzer: vi.fn(),
  applyAnalyzerMapping: vi.fn(),
  createAnalyzer: vi.fn(),
  getAnalyzer: vi.fn(),
  confirmAnalyzerMapping: vi.fn(),
  getAnalyzerMapping: vi.fn(),
  getAnalyzerMappingComponents: vi.fn(),
  getAnalyzerMappingResultOptions: vi.fn(),
  getAnalyzerMappingTests: vi.fn(),
  getAnalyzerActivationReadiness: vi.fn(),
  getAnalyzerLabUnits: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  testConnection: vi.fn(),
  saveAnalyzerMapping: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

const activeType = {
  profileId: "shipped.genexpert-astm",
  revision: 3,
  revisionFingerprint: `sha256:${"a".repeat(64)}`,
  displayName: "GeneXpert MTB/RIF",
  manufacturer: "Cepheid",
  model: "GeneXpert",
  source: "SHIPPED",
  status: "ACTIVE",
  protocol: "ASTM",
};

const currentMapping = {
  profileId: activeType.profileId,
  profileRevision: activeType.revision,
  profileFingerprint: activeType.revisionFingerprint,
  displayName: activeType.displayName,
  protocol: activeType.protocol,
  analyzerId: "42",
  mappingId: "12",
  mappingRevision: 2,
  mappingFingerprint: `sha256:${"c".repeat(64)}`,
  tests: [
    {
      sourceRowKey: "test:MTB-RIF",
      rawCode: "MTB-RIF",
      aliases: [],
      mappingState: "BOUND",
      testId: "301",
      selectedTest: {
        id: "301",
        name: "MTB Detection",
        code: "MTB",
        codings: [{ system: "http://loinc.org", code: "85362-2" }],
      },
      results: [
        {
          rawValue: "POS",
          mappingState: "BOUND",
          resultOptionId: "701",
          selectedOption: { id: "701", value: "POS", label: "Detected" },
        },
        {
          rawValue: "INVALID",
          mappingState: "EXCLUDED",
          resultOptionId: null,
          selectedOption: null,
        },
      ],
    },
    {
      sourceRowKey: "test:SERVICE",
      rawCode: "SERVICE",
      aliases: [],
      mappingState: "EXCLUDED",
      testId: null,
      selectedTest: null,
      results: [],
    },
  ],
  controlRecognition: {
    recognitionFingerprint: `sha256:${"d".repeat(64)}`,
    mode: "RULES",
    description: "Control specimen identifiers start with QC",
    affirmedNoControlResults: false,
    conditions: [
      {
        key: "specimen-prefix",
        kind: "SPECIMEN_ID_STARTS_WITH",
        sourceLabel: "Specimen ID",
        value: "QC",
        description: "Control specimen identifiers start with QC",
      },
    ],
  },
  confirmation: {
    state: "CURRENT",
    profileId: activeType.profileId,
    profileRevision: activeType.revision,
    mappingFingerprint: `sha256:${"c".repeat(64)}`,
    recognitionFingerprint: `sha256:${"d".repeat(64)}`,
    confirmedBy: "19",
    confirmedByDisplayName: "Casey Iiams-Hauser",
    confirmedAt: "2026-08-23T15:30:00Z",
    confirmedRows: [
      { sourceRowKey: "test:MTB-RIF", rawValue: null },
      { sourceRowKey: "test:MTB-RIF", rawValue: "POS" },
    ],
    excludedRows: [
      { sourceRowKey: "test:MTB-RIF", rawValue: "INVALID" },
      { sourceRowKey: "test:SERVICE", rawValue: null },
    ],
  },
};

const connectedCandidate = (overrides = {}) => ({
  id: "42",
  name: "GX bench 1",
  profileId: activeType.profileId,
  profileRevision: activeType.revision,
  profileFingerprint: activeType.revisionFingerprint,
  bridgeConnectionId: "bridge-42",
  testUnitIds: ["7"],
  status: "SETUP",
  connected: true,
  connection: {
    schemaVersion: "1.0",
    connectionId: "bridge-42",
    clientAnalyzerId: "42",
    displayName: "GX bench 1",
    profileRef: {
      profileId: activeType.profileId,
      revision: activeType.revision,
      fingerprint: activeType.revisionFingerprint,
    },
    configRevision: 2,
    configFingerprint: `sha256:${"e".repeat(64)}`,
    fields: [
      {
        key: "listenerPort",
        labelKey: "analyzer.connection.field.port",
        inputKind: "NUMBER",
        required: true,
        defaultValue: null,
        currentValue: 55000,
        choices: [],
        validationErrors: [],
      },
    ],
    readiness: { ready: true, blockers: [] },
    latestProbe: null,
    desiredRuntimeState: "INACTIVE",
    actualRuntimeState: "INACTIVE",
    updatedAt: "2026-08-25T00:00:00Z",
  },
  ...overrides,
});

const SetupHarness = ({ onClose = vi.fn() }) => {
  const location = useLocation();
  const currentStep =
    new URLSearchParams(location.search).get("setup") || "instrument";

  return <AnalyzerSetup currentStep={currentStep} onClose={onClose} />;
};

const renderSetup = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <SetupHarness />
      </IntlProvider>
    </BrowserRouter>,
  );

const renderSetupWithHistory = (initialEntry, onClose = vi.fn()) => {
  const history = createMemoryHistory({ initialEntries: [initialEntry] });
  render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <SetupHarness onClose={onClose} />
      </IntlProvider>
    </Router>,
  );
  return history;
};

describe("AnalyzerSetup Instrument step", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.replaceState({}, "", "/analyzers?setup=instrument");
    // Verify embeds the mapping editor, which loads the catalog's tests and,
    // for a mapped test, its answers and components.
    activateAnalyzer.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        activated: true,
        status: "ACTIVE",
        blockers: [],
      }),
    );
    applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
      callback(connectedCandidate()),
    );
    getAnalyzerMappingTests.mockImplementation((callback) => callback([]));
    getAnalyzerMappingResultOptions.mockImplementation((_id, callback) =>
      callback([]),
    );
    getAnalyzerMappingComponents.mockImplementation((_id, callback) =>
      callback([]),
    );
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"b".repeat(64)}`,
        summary: {
          total: 3,
          inUse: 0,
          needsAttention: 0,
          deactivated: 1,
        },
        types: [
          activeType,
          {
            ...activeType,
            profileId: "shipped.fluorocycler-xt",
            displayName: "FluoroCycler XT",
            manufacturer: "Bruker",
            protocol: "FILE",
          },
          {
            ...activeType,
            profileId: "site.inactive",
            displayName: "Inactive site type",
            source: "SITE",
            status: "INACTIVE",
          },
        ],
      }),
    );
    getAnalyzerLabUnits.mockImplementation((callback) =>
      callback([
        { id: "7", name: "Molecular Biology" },
        { id: "8", name: "Hematology" },
      ]),
    );
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback(currentMapping),
    );
  });

  it("completes an upgraded empty draft without creating a duplicate analyzer", async () => {
    getAnalyzer.mockImplementation((_id, callback) =>
      callback({
        id: "42",
        name: "Retained draft",
        profileId: "",
        profileRevision: 0,
        profileFingerprint: "",
        bridgeConnectionId: null,
        testUnitIds: [],
        status: "SETUP",
      }),
    );
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback(connectedCandidate(payload)),
    );
    const history = renderSetupWithHistory(
      "/analyzers?setup=instrument&analyzerId=42",
    );

    expect(
      await screen.findByRole("textbox", { name: "Analyzer name" }),
    ).toHaveValue("Retained draft");
    const typePicker = screen.getByRole("combobox", { name: "Analyzer type" });
    await userEvent.click(typePicker);
    await userEvent.click(
      await screen.findByRole("option", {
        name: "GeneXpert MTB/RIF · Cepheid · ASTM · revision 3",
      }),
    );
    await userEvent.click(screen.getByRole("combobox", { name: /^Lab units/ }));
    await userEvent.click(
      await screen.findByRole("option", { name: "Molecular Biology" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Connect" }),
    );

    await waitFor(() =>
      expect(updateAnalyzer).toHaveBeenCalledWith(
        "42",
        {
          name: "Retained draft",
          profileId: activeType.profileId,
          profileRevision: activeType.revision,
          testUnitIds: ["7"],
        },
        expect.any(Function),
      ),
    );
    expect(createAnalyzer).not.toHaveBeenCalled();
    expect(new URLSearchParams(history.location.search).get("analyzerId")).toBe(
      "42",
    );
    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "connect",
    );
  });

  it("selects an active Analyzer Type through a searchable, URL-backed lab form", async () => {
    renderSetup();

    expect(
      await screen.findByRole("textbox", { name: "Analyzer name" }),
    ).toBeVisible();
    const typePicker = screen.getByRole("combobox", {
      name: "Analyzer type",
    });
    expect(screen.getByRole("combobox", { name: /^Lab units/ })).toBeVisible();

    await userEvent.click(typePicker);
    await userEvent.type(typePicker, "GeneXpert");
    expect(
      screen.queryByRole("option", {
        name: "FluoroCycler XT · Bruker · FILE · revision 3",
      }),
    ).not.toBeInTheDocument();
    await userEvent.click(
      await screen.findByRole("option", {
        name: "GeneXpert MTB/RIF · Cepheid · ASTM · revision 3",
      }),
    );

    const params = new URLSearchParams(window.location.search);
    expect(params.get("setup")).toBe("instrument");
    expect(params.get("profile")).toBe("shipped.genexpert-astm");
    expect(params.get("revision")).toBe("3");
    expect(screen.queryByText("Inactive site type")).not.toBeInTheDocument();

    const notListed = screen.getByRole("link", {
      name: "Instrument not listed?",
    });
    const notListedUrl = new URL(notListed.href);
    expect(notListedUrl.pathname).toBe("/analyzers/types");
    expect(notListedUrl.searchParams.get("action")).toBe("create");
    expect(notListedUrl.searchParams.get("returnTo")).toBe(
      "/analyzers?setup=instrument",
    );
    expect(
      screen.getByText(
        "Choose the listed type for your instrument, even if it will only send results. Whether it sends results only or also receives orders is set later, in Connect.",
      ),
    ).toBeVisible();

    expect(screen.queryByLabelText("Status")).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Communication Mode"),
    ).not.toBeInTheDocument();
    expect(screen.queryByLabelText("IP Address")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Port Number")).not.toBeInTheDocument();
  });

  it("requires a type, name, and lab unit before creating a candidate", async () => {
    renderSetup();

    await userEvent.click(
      await screen.findByRole("button", { name: "Continue to Connect" }),
    );

    expect(screen.getByText("Select an analyzer type")).toBeVisible();
    expect(screen.getByText("Enter an analyzer name")).toBeVisible();
    expect(screen.getByText("Select at least one lab unit")).toBeVisible();
    expect(createAnalyzer).not.toHaveBeenCalled();
  });

  it("persists the selected candidate and advances to URL-backed Connect", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers?search=gene&setup=instrument",
    );
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
    };
    createAnalyzer.mockImplementation((_payload, callback) =>
      callback(candidate),
    );
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    renderSetup();

    const typePicker = await screen.findByRole("combobox", {
      name: "Analyzer type",
    });
    await userEvent.click(typePicker);
    await userEvent.type(typePicker, "GeneXpert");
    await userEvent.click(
      await screen.findByRole("option", {
        name: "GeneXpert MTB/RIF · Cepheid · ASTM · revision 3",
      }),
    );
    await userEvent.type(
      screen.getByRole("textbox", { name: "Analyzer name" }),
      "GX bench 1",
    );
    const labUnitPicker = screen.getByRole("combobox", {
      name: /^Lab units/,
    });
    await userEvent.click(labUnitPicker);
    await userEvent.type(labUnitPicker, "Molecular");
    await userEvent.click(
      await screen.findByRole("option", { name: "Molecular Biology" }),
    );

    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Connect" }),
    );

    expect(createAnalyzer).toHaveBeenCalledWith(
      {
        name: "GX bench 1",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        testUnitIds: ["7"],
      },
      expect.any(Function),
    );
    const params = new URLSearchParams(window.location.search);
    expect(params.get("search")).toBe("gene");
    expect(params.get("setup")).toBe("connect");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe("3");
    expect(
      screen.getByRole("heading", { level: 3, name: "Connect" }).closest("li"),
    ).toHaveAttribute("aria-current", "step");
    expect(screen.getByText("GX bench 1")).toBeVisible();
    expect(screen.getByText("Molecular Biology")).toBeVisible();
  });

  it("reloads a bookmarked Verify step from the persisted analyzer", async () => {
    window.history.replaceState(
      {},
      "",
      `/analyzers?setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );
    getAnalyzer.mockImplementation((_id, callback) =>
      callback({
        id: "42",
        name: "GX bench 1",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        testUnitIds: ["7"],
        status: "SETUP",
      }),
    );

    renderSetup();

    expect(await screen.findByText("GX bench 1")).toBeVisible();
    expect(screen.getByText("Molecular Biology")).toBeVisible();
    expect(getAnalyzer).toHaveBeenCalledWith(
      "42",
      expect.any(Function),
      expect.any(AbortSignal),
    );
    expect(createAnalyzer).not.toHaveBeenCalled();
    expect(
      screen.getByRole("heading", { level: 3, name: "Verify" }).closest("li"),
    ).toHaveAttribute("aria-current", "step");
    expect(
      screen.getByRole("region", { name: "Set up GX bench 1" }),
    ).toBeVisible();
  });

  it("edits completed setup sections through bookmarkable URLs", async () => {
    getAnalyzer.mockImplementation((_id, callback) =>
      callback(connectedCandidate()),
    );
    const history = renderSetupWithHistory(
      `/analyzers?search=gene&setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );

    await userEvent.click(
      await screen.findByRole("button", { name: "Edit Connect" }),
    );

    let params = new URLSearchParams(history.location.search);
    expect(params.get("search")).toBe("gene");
    expect(params.get("setup")).toBe("connect");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe("3");
    expect(
      screen.getByRole("heading", { level: 3, name: "Connect" }).closest("li"),
    ).toHaveAttribute("aria-current", "step");

    await userEvent.click(
      screen.getByRole("button", { name: "Edit Instrument" }),
    );

    params = new URLSearchParams(history.location.search);
    expect(params.get("search")).toBe("gene");
    expect(params.get("setup")).toBe("instrument");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe("3");
    expect(
      screen
        .getByRole("heading", { level: 3, name: "Instrument" })
        .closest("li"),
    ).toHaveAttribute("aria-current", "step");
  });

  it("loads verification only for the persisted candidate profile revision", async () => {
    let loadCandidate;
    getAnalyzer.mockImplementation((_id, callback) => {
      loadCandidate = callback;
    });
    const history = renderSetupWithHistory(
      "/analyzers?setup=verify&analyzerId=42&profile=site.inactive&revision=9",
    );

    expect(getAnalyzerMapping).not.toHaveBeenCalled();

    await act(async () => {
      loadCandidate({
        id: "42",
        name: "GX bench 1",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        testUnitIds: ["7"],
        status: "SETUP",
      });
    });

    await waitFor(() =>
      expect(getAnalyzerMapping).toHaveBeenCalledWith(
        "42",
        expect.any(Function),
      ),
    );
    expect(getAnalyzerMapping).toHaveBeenCalledTimes(1);
    const params = new URLSearchParams(history.location.search);
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe(String(activeType.revision));
  });

  it("reloads the analyzer mapping sign-off and activates only the reviewed candidate", async () => {
    const entry = `/analyzers?search=gene&setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`;
    getAnalyzer.mockImplementation((_id, callback) =>
      callback(connectedCandidate()),
    );
    applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
      callback(connectedCandidate()),
    );
    const onClose = vi.fn();
    renderSetupWithHistory(entry, onClose);

    expect(
      await screen.findAllByTestId("analyzer-type-mapping-row"),
    ).toHaveLength(2);
    expect(
      screen.getAllByText("Rule-based control recognition")[0],
    ).toBeVisible();
    expect(screen.getByText("Specimen ID starts with QC")).toBeVisible();
    expect(screen.getByText(/Casey Iiams-Hauser/)).toBeVisible();
    expect(getAnalyzerMapping).toHaveBeenCalledWith("42", expect.any(Function));

    await userEvent.click(
      screen.getByRole("button", { name: "Finish and activate" }),
    );
    expect(applyAnalyzerMapping).toHaveBeenCalledWith(
      "42",
      {
        mappingId: currentMapping.mappingId,
        revision: currentMapping.mappingRevision,
        mappingFingerprint: currentMapping.mappingFingerprint,
      },
      expect.any(Function),
    );
    expect(activateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("shows unconfigured control recognition during verification without blocking connection setup", async () => {
    getAnalyzer.mockImplementation((_id, callback) =>
      callback(connectedCandidate()),
    );
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...currentMapping,
        controlRecognition: {
          ...currentMapping.controlRecognition,
          description: "SERVER DESCRIPTION MUST NOT RENDER",
          conditions: [],
        },
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );

    expect(
      (await screen.findAllByText("Control recognition not configured"))[0],
    ).toBeVisible();
    expect(
      screen.getByText(
        "No control recognition rules are configured. Control results may not be identified automatically.",
      ),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Finish and activate" }),
    ).toBeEnabled();
    expect(
      screen.queryByText("This interface does not transmit control results"),
    ).not.toBeInTheDocument();
  });

  it("blocks routine activation while a record of an assay that is on is unmapped", async () => {
    getAnalyzer.mockImplementation((_id, callback) =>
      callback(connectedCandidate()),
    );
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...currentMapping,
        tests: [
          {
            ...currentMapping.tests[0],
            mappingState: "UNRESOLVED",
            testId: null,
            selectedTest: null,
          },
          currentMapping.tests[1],
        ],
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );

    expect(
      await screen.findByText(
        "1 record or value of the assays that are on still need mapping. Map them below, or turn the assay off in Assays if this instrument does not run it.",
      ),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Finish and activate" }),
    ).toBeDisabled();
  });

  it("does not block routine activation for an assay this instrument does not run", async () => {
    getAnalyzer.mockImplementation((_id, callback) =>
      callback(connectedCandidate()),
    );
    applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
      callback(connectedCandidate()),
    );
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...currentMapping,
        tests: [
          currentMapping.tests[0],
          {
            sourceRowKey: "test:FLU",
            rawCode: "FLU",
            aliases: [],
            mappingState: "UNRESOLVED",
            unresolvedReason: "NO_MATCH",
            enabled: false,
            testId: null,
            selectedTest: null,
            results: [],
          },
        ],
      }),
    );
    const onClose = vi.fn();
    renderSetupWithHistory(
      `/analyzers?setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`,
      onClose,
    );

    const button = await screen.findByRole("button", {
      name: "Finish and activate",
    });
    await waitFor(() => expect(button).toBeEnabled());
    expect(screen.queryByText("FLU")).not.toBeInTheDocument();
    await userEvent.click(button);
    expect(applyAnalyzerMapping).toHaveBeenCalled();
    expect(activateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("stays on Verify and explains when the reviewed mapping revision changed", async () => {
    const entry = `/analyzers?setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`;
    getAnalyzer.mockImplementation((_id, callback) =>
      callback(connectedCandidate()),
    );
    applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
      callback({
        error: "stale binding",
        statusCode: 400,
        messageKey: "analyzer.mapping.error.changedSinceLoaded",
      }),
    );
    const history = renderSetupWithHistory(entry);

    await userEvent.click(
      await screen.findByRole("button", { name: "Finish and activate" }),
    );

    expect(
      await screen.findByText(
        "Could not apply the reviewed mappings. Reload Verify and try again.",
      ),
    ).toBeVisible();
    expect(
      screen.getByText(
        "This analyzer's mapping changed after this page was loaded. Reload the page to see the current mapping.",
      ),
    ).toBeVisible();
    expect(screen.queryByText("stale binding")).not.toBeInTheDocument();
    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "verify",
    );
  });

  it("blocks routine activation and resolves in the embedded mapping editor when verification needs attention", async () => {
    getAnalyzer.mockImplementation((_id, callback) =>
      callback({
        id: "42",
        name: "GX bench 1",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        testUnitIds: ["7"],
        status: "SETUP",
      }),
    );
    getAnalyzerMapping.mockImplementation((_id, callback) =>
      callback({
        ...currentMapping,
        tests: [
          {
            ...currentMapping.tests[0],
            mappingState: "UNRESOLVED",
            testId: null,
            selectedTest: null,
          },
        ],
        confirmation: {
          ...currentMapping.confirmation,
          state: "STALE",
        },
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );

    expect(
      await screen.findByText(
        "1 record or value of the assays that are on still need mapping. Map them below, or turn the assay off in Assays if this instrument does not run it.",
      ),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Finish and activate" }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("link", { name: "Review mappings" }),
    ).not.toBeInTheDocument();
  });

  it("returns to and updates the same candidate through browser history", async () => {
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
    };
    createAnalyzer.mockImplementation((_payload, callback) =>
      callback(candidate),
    );
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    const history = renderSetupWithHistory("/analyzers?setup=instrument");

    const typePicker = await screen.findByRole("combobox", {
      name: "Analyzer type",
    });
    await userEvent.click(typePicker);
    await userEvent.type(typePicker, "GeneXpert");
    await userEvent.click(
      await screen.findByRole("option", {
        name: "GeneXpert MTB/RIF · Cepheid · ASTM · revision 3",
      }),
    );
    await userEvent.type(
      screen.getByRole("textbox", { name: "Analyzer name" }),
      "GX bench 1",
    );
    const labUnitPicker = screen.getByRole("combobox", {
      name: /^Lab units/,
    });
    await userEvent.click(labUnitPicker);
    await userEvent.type(labUnitPicker, "Molecular");
    await userEvent.click(
      await screen.findByRole("option", { name: "Molecular Biology" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Connect" }),
    );

    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "connect",
    );
    history.goBack();
    await waitFor(() =>
      expect(new URLSearchParams(history.location.search).get("setup")).toBe(
        "instrument",
      ),
    );
    expect(new URLSearchParams(history.location.search).get("analyzerId")).toBe(
      "42",
    );

    createAnalyzer.mockClear();
    const nameInput = screen.getByRole("textbox", { name: "Analyzer name" });
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "GX bench A");
    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Connect" }),
    );

    expect(createAnalyzer).not.toHaveBeenCalled();
    expect(updateAnalyzer).toHaveBeenCalledWith(
      "42",
      {
        name: "GX bench A",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        testUnitIds: ["7"],
      },
      expect.any(Function),
    );
    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "connect",
    );
    expect(new URLSearchParams(history.location.search).get("analyzerId")).toBe(
      "42",
    );
  });

  it("keeps routine analyzer restart recovery on the activation controls", async () => {
    const candidate = connectedCandidate({ status: "ACTIVE" });
    candidate.connection = {
      ...candidate.connection,
      readiness: {
        ready: false,
        blockers: [
          {
            messageKey: "analyzer.connection.readiness.restoreFailed",
          },
        ],
      },
    };
    const onClose = vi.fn();
    activateAnalyzer.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "ACTIVE",
        ready: true,
        activated: true,
        blockers: [],
      }),
    );
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, _payload, callback) =>
      callback(candidate),
    );
    getAnalyzerActivationReadiness.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "ACTIVE",
        ready: true,
        activated: false,
        blockers: [],
      }),
    );
    renderSetupWithHistory(`/analyzers?setup=connect&analyzerId=42`, onClose);
    expect(
      await screen.findByRole("button", { name: "Finish and activate" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Start receiving setup samples" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Continue to Assays" }),
    ).not.toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: "Finish and activate" }),
    );
    expect(activateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it.each(["INACTIVE", "ERROR"])(
    "restores %s routine reception from Verify before closing",
    async (runtimeState) => {
      const candidate = connectedCandidate({ status: "ACTIVE" });
      candidate.connection.actualRuntimeState = runtimeState;
      candidate.connection.readiness = {
        ready: false,
        blockers: [
          { messageKey: "analyzer.connection.readiness.restoreFailed" },
        ],
      };
      getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
      applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
        callback(candidate),
      );
      const onClose = vi.fn();
      renderSetupWithHistory(`/analyzers?setup=verify&analyzerId=42`, onClose);
      await userEvent.click(
        await screen.findByRole("button", { name: "Finish and activate" }),
      );
      expect(activateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
      expect(onClose).toHaveBeenCalledOnce();
    },
  );

  it("keeps Verify open when routine reception cannot be restored", async () => {
    const candidate = connectedCandidate({ status: "ACTIVE" });
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
      callback(candidate),
    );
    activateAnalyzer.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "ACTIVE",
        activated: false,
        blockers: [],
      }),
    );
    const onClose = vi.fn();
    renderSetupWithHistory(`/analyzers?setup=verify&analyzerId=42`, onClose);
    await userEvent.click(
      await screen.findByRole("button", { name: "Finish and activate" }),
    );
    expect(
      await screen.findByText(
        "Could not apply the reviewed mappings. Reload Verify and try again.",
      ),
    ).toBeVisible();
    expect(onClose).not.toHaveBeenCalled();
  });

  it("uses the post-apply runtime state without activating a recovered connection twice", async () => {
    const candidate = connectedCandidate({ status: "ACTIVE" });
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    const recovered = {
      ...candidate,
      connection: { ...candidate.connection, actualRuntimeState: "ACTIVE" },
    };
    applyAnalyzerMapping.mockImplementation((_id, _selection, callback) =>
      callback(recovered),
    );
    const onClose = vi.fn();
    renderSetupWithHistory(`/analyzers?setup=verify&analyzerId=42`, onClose);
    await userEvent.click(
      await screen.findByRole("button", { name: "Finish and activate" }),
    );
    expect(activateAnalyzer).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("opens setup reception before mapping approval and keeps clinical activation separate", async () => {
    const candidate = connectedCandidate();
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    getAnalyzerActivationReadiness.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        ready: false,
        status: "SETUP",
        activated: false,
        blockers: [{ code: "analyzer.activation.blocker.mappings", args: {} }],
      }),
    );
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    startAnalyzerSetupReception.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        receiving: true,
        status: "VALIDATION",
        blockers: [],
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );
    await userEvent.click(
      await screen.findByRole("button", {
        name: "Start receiving setup samples",
      }),
    );
    expect(updateAnalyzer).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        connectionValues: { listenerPort: 55000 },
      }),
      expect.any(Function),
    );
    expect(startAnalyzerSetupReception).toHaveBeenCalledWith(
      "42",
      expect.any(Function),
    );
    expect(activateAnalyzer).not.toHaveBeenCalled();
    expect(applyAnalyzerMapping).not.toHaveBeenCalled();
    expect(
      await screen.findByRole("button", { name: "Stop setup reception" }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Continue to Assays" }),
    ).toBeEnabled();
  });

  it("saves connection settings first and activates from Verify without requiring a probe", async () => {
    const onClose = vi.fn();
    const candidate = connectedCandidate();
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=42&profile=${activeType.profileId}&revision=3`,
      onClose,
    );
    await userEvent.click(
      await screen.findByRole("button", { name: "Continue to Assays" }),
    );
    expect(updateAnalyzer).toHaveBeenCalledWith(
      "42",
      expect.objectContaining({
        connectionValues: { listenerPort: 55000 },
      }),
      expect.any(Function),
    );
    expect(activateAnalyzer).not.toHaveBeenCalled();
    await userEvent.click(
      await screen.findByRole("button", { name: "Continue to Verify" }),
    );
    await userEvent.click(
      await screen.findByRole("button", { name: "Finish and activate" }),
    );
    expect(activateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
    expect(testConnection).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("saves setup for later without activating or testing the connection", async () => {
    const onClose = vi.fn();
    const candidate = connectedCandidate();
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    getAnalyzerActivationReadiness.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "SETUP",
        ready: true,
        activated: false,
        blockers: [],
      }),
    );
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=42&profile=${activeType.profileId}&revision=3`,
      onClose,
    );

    await userEvent.click(
      await screen.findByRole("button", { name: "Save and finish later" }),
    );

    await waitFor(() => expect(updateAnalyzer).toHaveBeenCalledTimes(1));
    expect(activateAnalyzer).not.toHaveBeenCalled();
    expect(testConnection).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
