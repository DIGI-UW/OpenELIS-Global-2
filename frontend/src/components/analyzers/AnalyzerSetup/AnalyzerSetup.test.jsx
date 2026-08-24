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
  createAnalyzer,
  getAnalyzer,
  getAnalyzerActivationReadiness,
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
  getAnalyzerTypeMapping,
  testConnection,
  updateAnalyzer,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";
import AnalyzerSetup from "./AnalyzerSetup";

vi.mock("../../../services/analyzerService", () => ({
  activateAnalyzer: vi.fn(),
  createAnalyzer: vi.fn(),
  getAnalyzer: vi.fn(),
  getAnalyzerActivationReadiness: vi.fn(),
  getAnalyzerLabUnits: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  getAnalyzerTypeMapping: vi.fn(),
  testConnection: vi.fn(),
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
  instanceDefaults: {
    protocolVersion: "ASTM_LIS2_A2",
    communicationMode: "BOTH",
    supportsLisInitiated: true,
    port: null,
  },
};

const currentMapping = {
  profileId: activeType.profileId,
  profileRevision: activeType.revision,
  profileFingerprint: activeType.revisionFingerprint,
  displayName: activeType.displayName,
  protocol: activeType.protocol,
  siteBindingId: "12",
  siteBindingRevision: 2,
  bindingFingerprint: `sha256:${"c".repeat(64)}`,
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
        loincCodes: ["85362-2"],
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
    bindingFingerprint: `sha256:${"c".repeat(64)}`,
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
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"b".repeat(64)}`,
        summary: {
          total: 2,
          inUse: 0,
          needsAttention: 0,
          deactivated: 1,
        },
        types: [
          activeType,
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
    getAnalyzerTypeMapping.mockImplementation(
      (_profileId, _revision, callback) => callback(currentMapping),
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
      await screen.findByRole("button", { name: "Continue to Verify" }),
    );

    expect(screen.getByText("Select an analyzer type")).toBeVisible();
    expect(screen.getByText("Enter an analyzer name")).toBeVisible();
    expect(screen.getByText("Select at least one lab unit")).toBeVisible();
    expect(createAnalyzer).not.toHaveBeenCalled();
  });

  it("persists the selected candidate and advances to URL-backed Verify", async () => {
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
      screen.getByRole("button", { name: "Continue to Verify" }),
    );

    expect(createAnalyzer).toHaveBeenCalledWith(
      {
        name: "GX bench 1",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        status: "SETUP",
        testUnitIds: ["7"],
      },
      expect.any(Function),
    );
    const params = new URLSearchParams(window.location.search);
    expect(params.get("search")).toBe("gene");
    expect(params.get("setup")).toBe("verify");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe("3");
    expect(
      screen.getByRole("heading", { level: 3, name: "Verify" }).closest("li"),
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
  });

  it("loads verification only for the persisted candidate profile revision", async () => {
    let loadCandidate;
    getAnalyzer.mockImplementation((_id, callback) => {
      loadCandidate = callback;
    });
    const history = renderSetupWithHistory(
      "/analyzers?setup=verify&analyzerId=42&profile=site.inactive&revision=9",
    );

    expect(getAnalyzerTypeMapping).not.toHaveBeenCalled();

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
      expect(getAnalyzerTypeMapping).toHaveBeenCalledWith(
        activeType.profileId,
        activeType.revision,
        expect.any(Function),
      ),
    );
    expect(getAnalyzerTypeMapping).toHaveBeenCalledTimes(1);
    const params = new URLSearchParams(history.location.search);
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe(String(activeType.revision));
  });

  it("reloads the shared mapping sign-off and advances a current candidate to Connect", async () => {
    const entry = `/analyzers?search=gene&setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`;
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
    const history = renderSetupWithHistory(entry);

    expect(
      await screen.findByRole("heading", {
        name: "Review analyzer type mappings",
      }),
    ).toBeVisible();
    expect(screen.getByText("2 of 2 tests ready")).toBeVisible();
    expect(screen.getByText("2 of 2 result values ready")).toBeVisible();
    expect(screen.getByText("Rule-based control recognition")).toBeVisible();
    expect(screen.getByText("Specimen ID starts with QC")).toBeVisible();
    expect(screen.getByText(/Casey Iiams-Hauser/)).toBeVisible();
    expect(getAnalyzerTypeMapping).toHaveBeenCalledWith(
      activeType.profileId,
      activeType.revision,
      expect.any(Function),
    );
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();

    const reviewLink = screen.getByRole("link", {
      name: "Review mappings in Analyzer Types",
    });
    const reviewUrl = new URL(reviewLink.href);
    expect(reviewUrl.pathname).toBe(
      `/analyzers/types/${activeType.profileId}/mapping`,
    );
    expect(reviewUrl.searchParams.get("revision")).toBe("3");
    expect(reviewUrl.searchParams.get("returnTo")).toBe(entry);

    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Connect" }),
    );
    const params = new URLSearchParams(history.location.search);
    expect(params.get("search")).toBe("gene");
    expect(params.get("setup")).toBe("connect");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe("3");
  });

  it("blocks Connect and uses the sole Analyzer Types editor when verification needs attention", async () => {
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
    getAnalyzerTypeMapping.mockImplementation(
      (_profileId, _revision, callback) =>
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
      await screen.findByText("Verification needs attention"),
    ).toBeVisible();
    expect(screen.getByText("0 of 1 tests ready")).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Continue to Connect" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("link", { name: "Review mappings in Analyzer Types" }),
    ).toBeVisible();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
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
      screen.getByRole("button", { name: "Continue to Verify" }),
    );

    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "verify",
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
      screen.getByRole("button", { name: "Continue to Verify" }),
    );

    expect(createAnalyzer).not.toHaveBeenCalled();
    expect(updateAnalyzer).toHaveBeenCalledWith(
      "42",
      {
        name: "GX bench A",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        status: "SETUP",
        testUnitIds: ["7"],
      },
      expect.any(Function),
    );
    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "verify",
    );
    expect(new URLSearchParams(history.location.search).get("analyzerId")).toBe(
      "42",
    );
  });

  it("restores a bookmarked Results only receiver and renders structured Bridge evidence inline", async () => {
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
      ipAddress: "10.20.30.40",
      port: null,
      communicationMode: "ANALYZER_INITIATED",
      effectiveCommunicationMode: "ANALYZER_INITIATED",
      transportMode: "TCP",
      connectionRole: "RECEIVER",
    };
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    testConnection.mockImplementation((_id, callback) =>
      callback({
        schemaVersion: "1.0",
        analyzerId: "42",
        profileRef: {
          profileId: activeType.profileId,
          revision: activeType.revision,
        },
        desiredStateFingerprint: `sha256:${"e".repeat(64)}`,
        connection: { mode: "TCP", role: "RECEIVER" },
        dataFlow: "RESULTS_ONLY",
        outcome: "SUCCESS",
        configureEndpoint: {
          kind: "NETWORK",
          host: "bridge.lab.example",
          port: 55000,
        },
        resultsOnlyAvailable: false,
        checks: [
          {
            kind: "LISTENER",
            status: "PASSED",
            code: "listener.ready",
            responseTimeMs: 3,
          },
        ],
      }),
    );
    const entry = `/analyzers?search=gene&setup=connect&analyzerId=42&profile=${activeType.profileId}&revision=3`;
    const history = renderSetupWithHistory(entry);

    expect(
      await screen.findByRole("group", { name: "Data flow" }),
    ).toBeVisible();
    expect(screen.getByRole("radio", { name: "Results only" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "Two-way" })).toBeVisible();
    expect(
      screen.getByRole("textbox", { name: "Analyzer source address" }),
    ).toHaveValue("10.20.30.40");
    expect(
      screen.queryByRole("spinbutton", { name: "Analyzer port" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Bridge listener port"),
    ).not.toBeInTheDocument();
    expect(history.location.pathname + history.location.search).toBe(entry);

    await userEvent.click(
      screen.getByRole("button", { name: "Test connection" }),
    );

    await waitFor(() =>
      expect(updateAnalyzer).toHaveBeenCalledWith(
        "42",
        {
          ipAddress: "10.20.30.40",
          port: null,
          communicationMode: "ANALYZER_INITIATED",
          transportMode: "TCP",
          connectionRole: "RECEIVER",
          importDirectory: null,
        },
        expect.any(Function),
      ),
    );
    expect(testConnection).toHaveBeenCalledWith("42", expect.any(Function));
    expect(await screen.findByText("Connection ready")).toBeVisible();
    expect(screen.getByText("Bridge listener is ready.")).toBeVisible();
    expect(screen.getByText("bridge.lab.example:55000")).toBeVisible();
    expect(screen.queryByText("Connection log")).not.toBeInTheDocument();
  });

  it("keeps a failed Two-way choice until the user explicitly selects Results only", async () => {
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
      ipAddress: "10.20.30.40",
      port: 9200,
      communicationMode: "BOTH",
      effectiveCommunicationMode: "BOTH",
      transportMode: "TCP",
      connectionRole: "RECEIVER",
    };
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    testConnection.mockImplementation((_id, callback) =>
      callback({
        schemaVersion: "1.0",
        analyzerId: "42",
        profileRef: {
          profileId: activeType.profileId,
          revision: activeType.revision,
        },
        desiredStateFingerprint: `sha256:${"f".repeat(64)}`,
        connection: { mode: "TCP", role: "RECEIVER" },
        dataFlow: "TWO_WAY",
        outcome: "TIMEOUT",
        configureEndpoint: {
          kind: "NETWORK",
          host: "bridge.lab.example",
          port: 55000,
        },
        resultsOnlyAvailable: true,
        checks: [
          {
            kind: "LISTENER",
            status: "PASSED",
            code: "listener.ready",
            responseTimeMs: 2,
          },
          {
            kind: "REMOTE_PROTOCOL",
            status: "TIMED_OUT",
            code: "remote.timeout",
            responseTimeMs: 5000,
          },
          {
            kind: "REMOTE_PROTOCOL",
            status: "FAILED",
            code: "remote.detail.unrecognized",
            responseTimeMs: 5000,
          },
        ],
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );

    expect(await screen.findByRole("radio", { name: "Two-way" })).toBeChecked();
    expect(
      screen.getByRole("textbox", { name: "Analyzer address" }),
    ).toHaveValue("10.20.30.40");
    expect(
      screen.getByRole("spinbutton", { name: "Analyzer port" }),
    ).toHaveValue(9200);

    await userEvent.click(
      screen.getByRole("button", { name: "Test connection" }),
    );

    expect(await screen.findByText("Connection timed out")).toBeVisible();
    expect(
      screen.getByText("The analyzer did not respond before the timeout."),
    ).toBeVisible();
    expect(screen.getByText("Connection check failed.")).toBeVisible();
    expect(
      screen.queryByText("remote.detail.unrecognized"),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Two-way" })).toBeChecked();
    expect(updateAnalyzer).toHaveBeenCalledTimes(1);

    await userEvent.click(
      screen.getByRole("button", { name: "Use Results only" }),
    );

    expect(screen.getByRole("radio", { name: "Results only" })).toBeChecked();
    expect(
      screen.queryByRole("spinbutton", { name: "Analyzer port" }),
    ).not.toBeInTheDocument();
    expect(updateAnalyzer).toHaveBeenCalledTimes(1);

    await userEvent.click(
      screen.getByRole("button", { name: "Test connection" }),
    );
    await waitFor(() => expect(updateAnalyzer).toHaveBeenCalledTimes(2));
    expect(updateAnalyzer).toHaveBeenLastCalledWith(
      "42",
      {
        ipAddress: "10.20.30.40",
        port: null,
        communicationMode: "ANALYZER_INITIATED",
        transportMode: "TCP",
        connectionRole: "RECEIVER",
        importDirectory: null,
      },
      expect.any(Function),
    );
  });

  it("configures a FILE analyzer without inventing network fields", async () => {
    const fileType = {
      ...activeType,
      profileId: "shipped.fluorocycler-file",
      revision: 2,
      displayName: "FluoroCycler XT",
      manufacturer: "Hain Lifescience",
      model: "FluoroCycler XT",
      protocol: "FILE",
      instanceDefaults: {
        protocolVersion: "FILE_SPREADSHEET",
        communicationMode: null,
        supportsLisInitiated: null,
        port: null,
      },
    };
    const candidate = {
      id: "54",
      name: "FluoroCycler bench 1",
      profileId: fileType.profileId,
      profileRevision: fileType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
      ipAddress: null,
      port: null,
      communicationMode: null,
      effectiveCommunicationMode: null,
      transportMode: "FILE",
      connectionRole: "RECEIVER",
      importDirectory: "/data/analyzers/fluorocycler",
    };
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"b".repeat(64)}`,
        summary: { total: 1, inUse: 1, needsAttention: 0, deactivated: 0 },
        types: [fileType],
      }),
    );
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    testConnection.mockImplementation((_id, callback) =>
      callback({
        schemaVersion: "1.0",
        analyzerId: "54",
        profileRef: {
          profileId: fileType.profileId,
          revision: fileType.revision,
        },
        desiredStateFingerprint: `sha256:${"9".repeat(64)}`,
        connection: { mode: "FILE", role: "RECEIVER" },
        dataFlow: "RESULTS_ONLY",
        outcome: "SUCCESS",
        configureEndpoint: {
          kind: "DIRECTORY",
          path: "/data/analyzers/fluorocycler",
        },
        resultsOnlyAvailable: false,
        checks: [
          {
            kind: "DIRECTORY",
            status: "PASSED",
            code: "directory.ready",
            args: { path: "/data/analyzers/fluorocycler" },
            responseTimeMs: 1,
          },
        ],
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=54&profile=${fileType.profileId}&revision=2`,
    );

    expect(
      await screen.findByRole("textbox", { name: "Analyzer file directory" }),
    ).toHaveValue("/data/analyzers/fluorocycler");
    expect(
      screen.queryByRole("group", { name: "Data flow" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("textbox", { name: "Analyzer address" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("spinbutton", { name: "Analyzer port" }),
    ).not.toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "Test connection" }),
    );

    await waitFor(() =>
      expect(updateAnalyzer).toHaveBeenCalledWith(
        "54",
        {
          ipAddress: null,
          port: null,
          communicationMode: null,
          transportMode: "FILE",
          connectionRole: "RECEIVER",
          importDirectory: "/data/analyzers/fluorocycler",
        },
        expect.any(Function),
      ),
    );
    expect(await screen.findByText("Directory is ready.")).toBeVisible();
    expect(screen.getByText("/data/analyzers/fluorocycler")).toBeVisible();
  });

  it("uses a one-way profile default without offering unsupported Two-way setup", async () => {
    const resultsOnlyType = {
      ...activeType,
      profileId: "site.results-only-astm",
      displayName: "Results-only ASTM analyzer",
      instanceDefaults: {
        ...activeType.instanceDefaults,
        communicationMode: "ANALYZER_INITIATED",
        supportsLisInitiated: false,
      },
    };
    const candidate = {
      id: "61",
      name: "Results-only bench 1",
      profileId: resultsOnlyType.profileId,
      profileRevision: resultsOnlyType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
      ipAddress: "10.30.40.50",
      port: null,
      communicationMode: null,
      effectiveCommunicationMode: null,
      transportMode: "TCP",
      connectionRole: "RECEIVER",
    };
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"b".repeat(64)}`,
        summary: { total: 1, inUse: 0, needsAttention: 0, deactivated: 0 },
        types: [resultsOnlyType],
      }),
    );
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    testConnection.mockImplementation((_id, callback) =>
      callback({
        schemaVersion: "1.0",
        analyzerId: "61",
        profileRef: {
          profileId: resultsOnlyType.profileId,
          revision: resultsOnlyType.revision,
        },
        desiredStateFingerprint: `sha256:${"8".repeat(64)}`,
        connection: { mode: "TCP", role: "RECEIVER" },
        dataFlow: "RESULTS_ONLY",
        outcome: "SUCCESS",
        configureEndpoint: {
          kind: "NETWORK",
          host: "bridge.lab.example",
          port: 55000,
        },
        resultsOnlyAvailable: false,
        checks: [
          {
            kind: "LISTENER",
            status: "PASSED",
            code: "listener.ready",
            responseTimeMs: 2,
          },
        ],
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=61&profile=${resultsOnlyType.profileId}&revision=3`,
    );

    expect(
      await screen.findByRole("radio", { name: "Results only" }),
    ).toBeChecked();
    expect(
      screen.queryByRole("radio", { name: "Two-way" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("spinbutton", { name: "Analyzer port" }),
    ).not.toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "Test connection" }),
    );

    await waitFor(() =>
      expect(updateAnalyzer).toHaveBeenCalledWith(
        "61",
        expect.objectContaining({
          communicationMode: "ANALYZER_INITIATED",
          port: null,
        }),
        expect.any(Function),
      ),
    );
  });

  it("shows exact mapping and recognition blockers in the Connect step", async () => {
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
      ipAddress: "10.20.30.40",
      port: null,
      communicationMode: "ANALYZER_INITIATED",
      effectiveCommunicationMode: "ANALYZER_INITIATED",
      transportMode: "TCP",
      connectionRole: "RECEIVER",
    };
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    getAnalyzerActivationReadiness.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "SETUP",
        ready: false,
        activated: false,
        blockers: [
          { code: "analyzer.activation.blocker.mappings", args: {} },
          { code: "analyzer.activation.blocker.recognition", args: {} },
        ],
      }),
    );

    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );

    expect(
      await screen.findByText("Analyzer mappings must be verified again."),
    ).toBeVisible();
    expect(
      screen.getByText("Control recognition must be verified again."),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Finish and activate" }),
    ).toBeVisible();
    expect(activateAnalyzer).not.toHaveBeenCalled();
  });

  it("saves current settings and activates without requiring a connection test", async () => {
    const onClose = vi.fn();
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
      ipAddress: "10.20.30.40",
      port: null,
      communicationMode: "ANALYZER_INITIATED",
      effectiveCommunicationMode: "ANALYZER_INITIATED",
      transportMode: "TCP",
      connectionRole: "RECEIVER",
    };
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
    activateAnalyzer.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "ACTIVE",
        ready: true,
        activated: true,
        blockers: [],
      }),
    );
    renderSetupWithHistory(
      `/analyzers?setup=connect&analyzerId=42&profile=${activeType.profileId}&revision=3`,
      onClose,
    );

    await userEvent.click(
      await screen.findByRole("button", { name: "Finish and activate" }),
    );

    await waitFor(() =>
      expect(updateAnalyzer).toHaveBeenCalledWith(
        "42",
        {
          ipAddress: "10.20.30.40",
          port: null,
          communicationMode: "ANALYZER_INITIATED",
          transportMode: "TCP",
          connectionRole: "RECEIVER",
          importDirectory: null,
        },
        expect.any(Function),
      ),
    );
    expect(activateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
    expect(testConnection).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
