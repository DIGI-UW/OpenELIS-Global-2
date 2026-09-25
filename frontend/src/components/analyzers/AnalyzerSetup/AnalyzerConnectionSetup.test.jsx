import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { createMemoryHistory } from "history";
import { IntlProvider } from "react-intl";
import { Router } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  activateAnalyzer,
  getAnalyzerActivationReadiness,
  testConnection,
  updateAnalyzer,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";
import AnalyzerConnectionSetup from "./AnalyzerConnectionSetup";

vi.mock("../../../services/analyzerService", () => ({
  activateAnalyzer: vi.fn(),
  getAnalyzerActivationReadiness: vi.fn(),
  testConnection: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

const profileRef = {
  profileId: "site.synthetic-analyzer",
  revision: 4,
  fingerprint: `sha256:${"a".repeat(64)}`,
};

const connection = {
  schemaVersion: "1.0",
  connectionId: "bridge-42",
  clientAnalyzerId: "42",
  displayName: "Synthetic bench 1",
  profileRef,
  configRevision: 3,
  configFingerprint: `sha256:${"b".repeat(64)}`,
  fields: [
    {
      key: "transport",
      labelKey: "analyzer.connection.field.transport",
      inputKind: "SELECT",
      required: true,
      currentValue: "TCP/IP",
      defaultValue: "TCP/IP",
      choices: [
        {
          value: "TCP/IP",
          labelKey: "analyzer.connection.transport.TCP/IP",
        },
        {
          value: "RS-232",
          labelKey: "analyzer.connection.transport.RS-232",
        },
      ],
      validationErrors: [],
    },
    {
      key: "connectionRole",
      labelKey: "analyzer.connection.field.connectionRole",
      inputKind: "SELECT",
      required: true,
      currentValue: "SERVER",
      defaultValue: "SERVER",
      choices: [
        { value: "SERVER", labelKey: "analyzer.connection.role.server" },
        { value: "CLIENT", labelKey: "analyzer.connection.role.client" },
      ],
      visibleWhen: {
        fieldKey: "transport",
        operator: "NOT_EQUALS",
        value: "RS-232",
      },
      validationErrors: [],
    },
    {
      key: "host",
      labelKey: "analyzer.connection.field.host",
      inputKind: "TEXT",
      required: true,
      defaultValue: null,
      choices: [],
      visibleWhen: {
        fieldKey: "connectionRole",
        operator: "EQUALS",
        value: "CLIENT",
      },
      validationErrors: [],
    },
    {
      key: "port",
      labelKey: "analyzer.connection.field.port",
      inputKind: "NUMBER",
      required: true,
      currentValue: 55000,
      defaultValue: null,
      choices: [],
      visibleWhen: {
        fieldKey: "transport",
        operator: "EQUALS",
        value: "TCP/IP",
      },
      validationErrors: [],
    },
    {
      key: "serialPort",
      labelKey: "analyzer.connection.field.serialPort",
      inputKind: "TEXT",
      required: true,
      defaultValue: null,
      choices: [],
      visibleWhen: {
        fieldKey: "transport",
        operator: "EQUALS",
        value: "RS-232",
      },
      validationErrors: [],
    },
    {
      key: "watchDirectory",
      labelKey: "analyzer.connection.field.directory",
      helpTextKey: "analyzer.connection.field.directory.help",
      inputKind: "FILE_PATH",
      required: false,
      currentValue: "/data/instruments/synthetic",
      defaultValue: "/data/instruments/synthetic",
      choices: [],
      validationErrors: [],
    },
    {
      key: "enabled",
      labelKey: "analyzer.connection.field.enabled",
      inputKind: "BOOLEAN",
      required: false,
      currentValue: true,
      defaultValue: true,
      choices: [],
      validationErrors: [],
    },
    {
      key: "apiToken",
      labelKey: "analyzer.connection.field.apiToken",
      inputKind: "SECRET",
      required: true,
      defaultValue: null,
      isSet: true,
      maskedValue: "********",
      choices: [],
      validationErrors: [],
    },
  ],
  readiness: { ready: true, blockers: [] },
  latestProbe: null,
  desiredRuntimeState: "INACTIVE",
  actualRuntimeState: "INACTIVE",
  updatedAt: "2026-08-25T00:00:00Z",
};

const candidate = {
  id: "42",
  name: "Synthetic bench 1",
  profileId: profileRef.profileId,
  profileRevision: profileRef.revision,
  profileFingerprint: profileRef.fingerprint,
  bridgeConnectionId: connection.connectionId,
  testUnitIds: ["7"],
  status: "SETUP",
  connected: true,
  connection,
};

const renderConnection = ({
  onCandidateChange = vi.fn(),
  onClose = vi.fn(),
  onVerifyMappings,
} = {}) => {
  const history = createMemoryHistory({
    initialEntries: [
      `/analyzers?setup=connect&analyzerId=42&profile=${profileRef.profileId}&revision=4`,
    ],
  });
  render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <AnalyzerConnectionSetup
          candidate={candidate}
          onCandidateChange={onCandidateChange}
          onClose={onClose}
          onVerifyMappings={onVerifyMappings}
        />
      </IntlProvider>
    </Router>,
  );
  return history;
};

describe("AnalyzerConnectionSetup", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzerActivationReadiness.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "SETUP",
        ready: true,
        activated: false,
        blockers: [],
      }),
    );
    updateAnalyzer.mockImplementation((_id, _payload, callback) =>
      callback(candidate),
    );
  });

  it("renders and saves generic Bridge fields without analyzer-specific branching", async () => {
    const onClose = vi.fn();
    const history = renderConnection({ onClose });

    expect(await screen.findByLabelText("Transport")).toHaveDisplayValue(
      "Network (TCP/IP)",
    );
    expect(screen.getByLabelText("Connection role")).toHaveDisplayValue(
      "Server",
    );
    expect(screen.getByRole("spinbutton", { name: "Port" })).toHaveValue(55000);
    expect(
      screen.queryByRole("textbox", { name: "Host" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("textbox", { name: "Serial device" }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Directory" })).toHaveValue(
      "/data/instruments/synthetic",
    );
    expect(screen.getByLabelText("Enabled")).toBeChecked();
    expect(screen.getByLabelText("API token")).toHaveAttribute(
      "placeholder",
      "********",
    );

    await userEvent.selectOptions(
      screen.getByLabelText("Connection role"),
      "Client",
    );
    expect(screen.getByRole("textbox", { name: "Host" })).toBeVisible();

    await userEvent.selectOptions(
      screen.getByLabelText("Transport"),
      "Serial (RS-232)",
    );
    expect(screen.queryByLabelText("Connection role")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("textbox", { name: "Host" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("spinbutton", { name: "Port" }),
    ).not.toBeInTheDocument();
    const serial = screen.getByRole("textbox", { name: "Serial device" });
    await userEvent.type(serial, "/dev/ttyUSB0");

    await userEvent.click(
      screen.getByRole("button", { name: "Save and finish later" }),
    );

    await waitFor(() =>
      expect(updateAnalyzer).toHaveBeenCalledWith(
        "42",
        {
          name: candidate.name,
          profileId: candidate.profileId,
          profileRevision: candidate.profileRevision,
          testUnitIds: candidate.testUnitIds,
          connectionValues: {
            transport: "RS-232",
            serialPort: "/dev/ttyUSB0",
            watchDirectory: "/data/instruments/synthetic",
            enabled: true,
          },
        },
        expect.any(Function),
      ),
    );
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(history.location.search).toContain("setup=connect");
  });

  it("saves an edited Carbon numeric field as the generic connection value", async () => {
    renderConnection();
    const port = await screen.findByRole("spinbutton", { name: "Port" });

    await userEvent.clear(port);
    await userEvent.type(port, "45587");
    await userEvent.click(
      screen.getByRole("button", { name: "Save and finish later" }),
    );

    await waitFor(() =>
      expect(updateAnalyzer).toHaveBeenCalledWith(
        "42",
        expect.objectContaining({
          connectionValues: expect.objectContaining({ port: 45587 }),
        }),
        expect.any(Function),
      ),
    );
    expect(
      screen.queryByText("This field is required."),
    ).not.toBeInTheDocument();
  });

  it("shows required field validation before saving or probing", async () => {
    renderConnection();

    await userEvent.selectOptions(
      await screen.findByLabelText("Connection role"),
      "Client",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Test connection" }),
    );

    expect(await screen.findByText("This field is required.")).toBeVisible();
    expect(updateAnalyzer).not.toHaveBeenCalled();
    expect(testConnection).not.toHaveBeenCalled();
  });

  it("renders exact non-mutating Bridge probe evidence", async () => {
    testConnection.mockImplementation((_id, callback) =>
      callback({
        schemaVersion: "1.0",
        requestId: "probe-1",
        connectionId: connection.connectionId,
        profileRef,
        configRevision: connection.configRevision,
        configFingerprint: connection.configFingerprint,
        nonMutating: true,
        status: "SUCCEEDED",
        startedAt: "2026-08-25T00:01:00Z",
        completedAt: "2026-08-25T00:01:01Z",
        checks: [
          {
            key: "listener",
            status: "PASSED",
            messageKey: "listener.ready",
            durationMillis: 3,
            details: {},
          },
        ],
      }),
    );
    renderConnection();

    await userEvent.click(
      await screen.findByRole("button", { name: "Test connection" }),
    );

    expect(testConnection).toHaveBeenCalledWith("42", expect.any(Function));
    expect(await screen.findByText("Connection ready")).toBeVisible();
    expect(screen.getByText("Bridge listener is ready.")).toBeVisible();
    expect(screen.getByText("Bridge listener")).toBeVisible();
  });

  it("renders structured failure evidence when an equivalent saved revision advances", async () => {
    testConnection.mockImplementation((_id, callback) =>
      callback({
        schemaVersion: "1.0",
        requestId: "probe-failed-1",
        connectionId: connection.connectionId,
        profileRef,
        configRevision: connection.configRevision + 1,
        configFingerprint: connection.configFingerprint,
        nonMutating: true,
        status: "FAILED",
        startedAt: "2026-08-25T00:01:00Z",
        completedAt: "2026-08-25T00:01:01Z",
        checks: [
          {
            key: "listener",
            status: "FAILED",
            messageKey: "listener.not.listening",
            durationMillis: 3,
            details: { port: 12001 },
          },
        ],
      }),
    );
    renderConnection();

    await userEvent.click(
      await screen.findByRole("button", { name: "Test connection" }),
    );

    expect(await screen.findByText("Connection failed")).toBeVisible();
    expect(
      screen.getByText("The Bridge listener is not accepting connections."),
    ).toBeVisible();
    expect(
      screen.queryByText("Connection settings could not be saved or tested."),
    ).not.toBeInTheDocument();
  });

  describe("when the analyzer dials the Bridge", () => {
    const serverProbe = (status, checks) => ({
      schemaVersion: "1.0",
      requestId: "probe-server-1",
      connectionId: connection.connectionId,
      profileRef,
      configRevision: connection.configRevision,
      configFingerprint: connection.configFingerprint,
      nonMutating: true,
      status,
      startedAt: "2026-09-23T00:01:00Z",
      completedAt: "2026-09-23T00:01:01Z",
      checks,
    });
    const listenerReady = {
      key: "listener",
      status: "PASSED",
      messageKey: "listener.ready",
      durationMillis: 2,
      details: { port: 55000 },
    };

    const probeWith = async (result) => {
      testConnection.mockImplementation((_id, callback) => callback(result));
      renderConnection();
      await userEvent.click(
        await screen.findByRole("button", { name: "Test connection" }),
      );
    };

    it("explains an unchecked analyzer without reporting a failure", async () => {
      await probeWith(
        serverProbe("SUCCEEDED", [
          listenerReady,
          {
            key: "analyzer",
            status: "SKIPPED",
            messageKey: "analyzer.address.missing",
            durationMillis: 0,
          },
        ]),
      );

      expect(await screen.findByText("Connection ready")).toBeVisible();
      expect(screen.getByText("Analyzer (for information)")).toBeVisible();
      expect(
        screen.getByText(
          "No analyzer address is saved, so the analyzer was not checked. Results arriving from the analyzer prove the connection works.",
        ),
      ).toBeVisible();
      expect(
        screen.queryByText("Connection check failed."),
      ).not.toBeInTheDocument();
    });

    it("explains an unreachable analyzer without reporting a failure", async () => {
      await probeWith(
        serverProbe("SUCCEEDED", [
          listenerReady,
          {
            key: "analyzer",
            status: "FAILED",
            messageKey: "analyzer.unreachable",
            durationMillis: 1500,
            details: { host: "10.1.2.3" },
          },
        ]),
      );

      expect(await screen.findByText("Connection ready")).toBeVisible();
      expect(
        screen.getByText(
          "The Bridge could not reach the analyzer at 10.1.2.3. Analyzers that connect to the Bridge often cannot be reached from it (a firewall, NAT, or a hosted server). Results arriving from the analyzer prove the connection works.",
        ),
      ).toBeVisible();
      expect(
        screen.queryByText("Connection check failed."),
      ).not.toBeInTheDocument();
      expect(screen.queryByText("Connection failed")).not.toBeInTheDocument();
    });

    it("asks for the listener port when the Bridge cannot check without it", async () => {
      await probeWith(
        serverProbe("BLOCKED", [
          {
            key: "listener",
            status: "SKIPPED",
            messageKey: "listener.configuration.missing",
            durationMillis: 0,
          },
        ]),
      );

      expect(
        await screen.findByText("Connection settings are incomplete"),
      ).toBeVisible();
      expect(
        screen.getByText("Enter the Bridge listener port before testing."),
      ).toBeVisible();
      expect(
        screen.queryByText("This check could not be run."),
      ).not.toBeInTheDocument();
    });

    it("explains that an analyzer uploading files cannot be tested from the Bridge", async () => {
      await probeWith(
        serverProbe("FAILED", [
          {
            key: "http-input",
            status: "FAILED",
            messageKey: "http.input.verify.with.delivery",
            durationMillis: 0,
          },
        ]),
      );

      expect(await screen.findByText("Analyzer file upload")).toBeVisible();
      expect(
        screen.getByText(
          "This analyzer sends files to the Bridge, so the Bridge cannot test the connection from here. A delivered file is the proof.",
        ),
      ).toBeVisible();
      expect(
        screen.queryByText("Connection check failed."),
      ).not.toBeInTheDocument();
    });

    it("reports a listener failure as an error", async () => {
      await probeWith(
        serverProbe("FAILED", [
          {
            key: "listener",
            status: "FAILED",
            messageKey: "listener.not.listening",
            durationMillis: 2,
            details: { port: 55000 },
          },
          {
            key: "analyzer",
            status: "PASSED",
            messageKey: "analyzer.reachable",
            durationMillis: 4,
            details: { host: "10.1.2.3" },
          },
        ]),
      );

      const outcome = await screen.findByText("Connection failed");
      expect(outcome.closest(".cds--inline-notification")).toHaveClass(
        "cds--inline-notification--error",
      );
      expect(
        screen.getByText("The Bridge listener is not accepting connections."),
      ).toBeVisible();
      expect(
        screen.getByText("The Bridge reached the analyzer at 10.1.2.3."),
      ).toBeVisible();
    });
  });

  describe("when activation is blocked", () => {
    const blockedBy = (...codes) =>
      getAnalyzerActivationReadiness.mockImplementation((_id, callback) =>
        callback({
          analyzerId: "42",
          status: "SETUP",
          ready: false,
          activated: false,
          blockers: codes.map((code) => ({ code, args: {} })),
        }),
      );

    it("offers to verify mappings when the pinned mappings are stale", async () => {
      blockedBy("analyzer.activation.blocker.recognition");
      const onVerifyMappings = vi.fn();
      renderConnection({ onVerifyMappings });

      await userEvent.click(
        await screen.findByRole("button", { name: "Verify mappings" }),
      );
      expect(onVerifyMappings).toHaveBeenCalledTimes(1);
    });

    it("does not offer mapping verification for unrelated blockers", async () => {
      blockedBy("analyzer.activation.blocker.labUnit");
      renderConnection({ onVerifyMappings: vi.fn() });

      expect(
        await screen.findByText("Assign at least one active lab unit."),
      ).toBeVisible();
      expect(
        screen.queryByRole("button", { name: "Verify mappings" }),
      ).not.toBeInTheDocument();
    });
  });

  it("does not resend a masked secret unless the user replaces it", async () => {
    renderConnection();

    await userEvent.click(
      await screen.findByRole("button", { name: "Save and finish later" }),
    );

    await waitFor(() => expect(updateAnalyzer).toHaveBeenCalledTimes(1));
    const payload = updateAnalyzer.mock.calls[0][1];
    expect(payload.connectionValues).not.toHaveProperty("apiToken");
  });

  it("saves an active analyzer without offering or repeating activation", async () => {
    const onClose = vi.fn();
    getAnalyzerActivationReadiness.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "ACTIVE",
        ready: true,
        activated: true,
        blockers: [],
      }),
    );
    renderConnection({ onClose });

    expect(
      await screen.findByRole("button", { name: "Save changes" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Finish and activate" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Save and finish later" }),
    ).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() => expect(updateAnalyzer).toHaveBeenCalledTimes(1));
    expect(activateAnalyzer).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
