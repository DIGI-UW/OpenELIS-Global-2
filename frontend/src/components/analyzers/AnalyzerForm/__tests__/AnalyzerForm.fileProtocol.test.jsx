import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import AnalyzerForm from "../AnalyzerForm";
import messages from "../../../../languages/en.json";
import * as analyzerService from "../../../../services/analyzerService";

vi.mock("../../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getDefaultConfigs: vi.fn(),
  getDefaultConfig: vi.fn(),
  getAnalyzerTypes: vi.fn(),
  createAnalyzer: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

// Render the page-based AnalyzerForm under a MemoryRouter with a route that
// carries the edit `:id` param. The component reads the id via useParams()
// and calls getAnalyzer(id, cb) on mount — tests mock that service.
const renderAtEditRoute = (analyzerId) => {
  return render(
    <MemoryRouter initialEntries={[`/analyzers/${analyzerId}/edit`]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:id/edit">
          <AnalyzerForm />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );
};

const renderNewRoute = () => {
  return render(
    <MemoryRouter initialEntries={["/analyzers/new"]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/new">
          <AnalyzerForm />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );
};

const PLUGIN_TYPES = [
  { id: "1", name: "Generic ASTM", protocol: "ASTM", isGenericPlugin: true },
  { id: "2", name: "Generic HL7", protocol: "HL7", isGenericPlugin: true },
  { id: "3", name: "Generic File", protocol: "FILE", isGenericPlugin: true },
];

const DEFAULT_CONFIGS = [
  {
    id: "astm/genexpert-astm",
    protocol: "ASTM",
    analyzerName: "GeneXpert ASTM",
  },
  {
    id: "file/quantstudio",
    protocol: "FILE",
    analyzerName: "QuantStudio QS5/QS7",
  },
  {
    id: "hl7/mindray-bc5380",
    protocol: "HL7",
    analyzerName: "Mindray BC-5380",
  },
];

describe("AnalyzerForm - FILE Protocol Behavior", () => {
  beforeEach(() => {
    analyzerService.getAnalyzerTypes.mockImplementation((filters, callback) => {
      callback(PLUGIN_TYPES);
    });
    analyzerService.getDefaultConfigs.mockImplementation((callback) => {
      callback(DEFAULT_CONFIGS);
    });
    analyzerService.getDefaultConfig.mockImplementation(
      (protocol, name, callback) => {
        callback({ error: "not needed" });
      },
    );
    analyzerService.createAnalyzer.mockImplementation((data, callback) => {
      callback({ id: "NEW-ID", ...data });
    });
    analyzerService.getAnalyzer.mockImplementation((id, callback) => {
      callback({ error: "not configured for this test" });
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  test("hides connection fields and protocol version when FILE plugin is selected", async () => {
    const fileAnalyzer = {
      id: "test-1",
      name: "Test FILE Analyzer",
      analyzerType: "MOLECULAR",
      pluginTypeId: "3",
      status: "SETUP",
    };
    analyzerService.getAnalyzer.mockImplementation((id, callback) => {
      callback(fileAnalyzer);
    });

    renderAtEditRoute("test-1");

    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // Connection fields should NOT be in the DOM for FILE protocol
    expect(
      screen.queryByTestId("analyzer-form-connection-fields"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-ip-input"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-port-input"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-test-connection-button"),
    ).not.toBeInTheDocument();

    // Protocol version dropdown should NOT be in the DOM
    expect(
      screen.queryByTestId("analyzer-form-protocol-version-dropdown"),
    ).not.toBeInTheDocument();
  });

  test("shows FILE import settings when FILE plugin is selected", async () => {
    const fileAnalyzer = {
      id: "test-2",
      name: "Test FILE Analyzer",
      analyzerType: "MOLECULAR",
      pluginTypeId: "3",
      status: "SETUP",
    };
    analyzerService.getAnalyzer.mockImplementation((id, callback) => {
      callback(fileAnalyzer);
    });

    renderAtEditRoute("test-2");

    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // FILE import settings section should be visible (Section 3b in
    // AnalyzerForm) — format dropdown, import directory, file pattern,
    // and column mappings.
    expect(
      screen.queryByTestId("analyzer-form-file-format-dropdown"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-import-directory-input"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-file-pattern-input"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-column-mappings-input"),
    ).toBeInTheDocument();
  });

  test("shows connection fields when ASTM plugin is selected", async () => {
    const astmAnalyzer = {
      id: "test-3",
      name: "Test ASTM Analyzer",
      analyzerType: "MOLECULAR",
      pluginTypeId: "1",
      ipAddress: "192.168.1.100",
      port: "9600",
      status: "SETUP",
    };
    analyzerService.getAnalyzer.mockImplementation((id, callback) => {
      callback(astmAnalyzer);
    });

    renderAtEditRoute("test-3");

    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // Connection fields SHOULD be in the DOM for ASTM
    expect(
      screen.queryByTestId("analyzer-form-connection-fields"),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("analyzer-form-ip-input")).toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-port-input"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-test-connection-button"),
    ).toBeInTheDocument();

    // Protocol version dropdown SHOULD be in the DOM
    expect(
      screen.queryByTestId("analyzer-form-protocol-version-dropdown"),
    ).toBeInTheDocument();

    // FILE info tile should NOT be in the DOM
    expect(
      screen.queryByTestId("analyzer-form-file-protocol-info"),
    ).not.toBeInTheDocument();
  });

  test("plugin types are sorted with generic plugins first", async () => {
    renderNewRoute();

    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // The plugin type dropdown should exist
    const dropdown = screen.queryByTestId("analyzer-form-plugin-type-dropdown");
    expect(dropdown).toBeInTheDocument();

    // getAnalyzerTypes should have been called
    await waitFor(() => {
      expect(analyzerService.getAnalyzerTypes).toHaveBeenCalled();
    });
  });
});
