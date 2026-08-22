import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route, useLocation } from "react-router-dom";
import { vi } from "vitest";
import AnalyzerForm from "../AnalyzerForm";
import messages from "../../../../languages/en.json";
import * as analyzerService from "../../../../services/analyzerService";

vi.mock("../../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  getAnalyzerTypeRevision: vi.fn(),
  createAnalyzer: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

const catalog = {
  schemaVersion: "1.0",
  catalogFingerprint: "sha256:catalog",
  summary: { total: 2, inUse: 0, needsAttention: 1, deactivated: 1 },
  types: [
    {
      profileId: "shipped.validated-hl7-v25",
      revision: 2,
      revisionFingerprint: "sha256:validated-hl7-v25",
      displayName: "Validated HL7 v2.5 Analyzer",
      source: "SHIPPED",
      status: "ACTIVE",
      protocol: "HL7",
      instanceDefaults: {
        protocolVersion: "HL7_V2_5",
        communicationMode: "BOTH",
        port: 9111,
      },
    },
    {
      profileId: "shipped.retired",
      revision: 4,
      revisionFingerprint: "sha256:retired",
      displayName: "Retired Analyzer Type",
      source: "SHIPPED",
      status: "INACTIVE",
      protocol: "HL7",
    },
  ],
};

const LocationProbe = () => {
  const location = useLocation();
  return <output data-testid="location">{location.search}</output>;
};

const renderNewAnalyzer = (entry) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/new">
          <AnalyzerForm />
          <LocationProbe />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

const renderExistingAnalyzer = (entry) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:id/edit">
          <AnalyzerForm />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyzerForm profile revision pin", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    analyzerService.getAnalyzerTypeCatalog.mockImplementation((callback) => {
      callback(catalog);
    });
    analyzerService.createAnalyzer.mockImplementation((data, callback) => {
      callback({ id: "501", ...data });
    });
  });

  it("renders one semantic heading with a linkable setup breadcrumb", async () => {
    renderNewAnalyzer("/analyzers/new");

    await waitFor(() => {
      expect(analyzerService.getAnalyzerTypeCatalog).toHaveBeenCalledTimes(1);
    });

    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      "Add New Analyzer",
    );
    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(screen.getByRole("link", { name: "Analyzers" })).toHaveAttribute(
      "href",
      "/analyzers",
    );
    expect(breadcrumb.querySelector('[aria-current="page"]')).toHaveTextContent(
      "Add New Analyzer",
    );
  });

  it("restores an active Analyzer Type revision from the URL and submits that exact pin", async () => {
    renderNewAnalyzer(
      "/analyzers/new?profile=shipped.validated-hl7-v25&revision=2",
    );

    await waitFor(() => {
      expect(analyzerService.getAnalyzerTypeCatalog).toHaveBeenCalledTimes(1);
    });

    expect(
      screen.getByRole("combobox", { name: "Analyzer Type" }),
    ).toHaveTextContent("Validated HL7 v2.5 Analyzer");
    expect(screen.getByTestId("location")).toHaveTextContent(
      "?profile=shipped.validated-hl7-v25&revision=2",
    );
    expect(screen.getByTestId("analyzer-form-port-input")).toHaveValue("9111");
    expect(
      screen.getByTestId("analyzer-form-communication-mode-dropdown"),
    ).toHaveTextContent("Bidirectional (both directions)");
    expect(screen.queryByText("Plugin Type")).not.toBeInTheDocument();
    expect(screen.queryByText("Load Analyzer Profile")).not.toBeInTheDocument();

    await userEvent.type(
      screen.getByRole("textbox", { name: "Analyzer Name" }),
      "GeneXpert Bench 1",
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(analyzerService.createAnalyzer).toHaveBeenCalledTimes(1);
    });
    const submitted = analyzerService.createAnalyzer.mock.calls[0][0];
    expect(submitted).toMatchObject({
      name: "GeneXpert Bench 1",
      profileId: "shipped.validated-hl7-v25",
      profileRevision: 2,
      protocolVersion: "HL7_V2_5",
      communicationMode: "BOTH",
      port: 9111,
    });
    expect(submitted).not.toHaveProperty("analyzerType");
    expect(submitted).not.toHaveProperty("defaultConfigId");
    expect(submitted).not.toHaveProperty("pluginTypeId");
  });

  it("loads an existing analyzer's exact pinned revision instead of borrowing latest defaults", async () => {
    analyzerService.getAnalyzer.mockImplementation((_id, callback) => {
      callback({
        id: "501",
        name: "Pinned analyzer",
        profileId: "shipped.validated-hl7-v25",
        profileRevision: 1,
        protocolVersion: null,
        communicationMode: null,
        port: null,
        status: "SETUP",
        testUnitIds: [],
      });
    });
    analyzerService.getAnalyzerTypeRevision.mockImplementation(
      (_profileId, _revision, callback) => {
        callback({
          profileId: "shipped.validated-hl7-v25",
          revision: 1,
          revisionFingerprint: "sha256:validated-hl7-v25-r1",
          displayName: "Validated HL7 v2.5 Analyzer revision 1",
          source: "SHIPPED",
          status: "ACTIVE",
          protocol: "HL7",
          instanceDefaults: {
            protocolVersion: "HL7_V2_3_1",
            communicationMode: "ANALYZER_INITIATED",
            port: 8100,
          },
        });
      },
    );

    renderExistingAnalyzer("/analyzers/501/edit");

    await waitFor(() => {
      expect(analyzerService.getAnalyzerTypeRevision).toHaveBeenCalledWith(
        "shipped.validated-hl7-v25",
        1,
        expect.any(Function),
      );
    });
    expect(
      screen.getByRole("combobox", { name: "Analyzer Type" }),
    ).toHaveTextContent("Validated HL7 v2.5 Analyzer revision 1");
    expect(screen.getByTestId("analyzer-form-port-input")).toHaveValue("8100");
    expect(
      screen.getByTestId("analyzer-form-communication-mode-dropdown"),
    ).toHaveTextContent("Analyzer → LIS (analyzer connects to OpenELIS)");
  });
});
