import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route, useLocation } from "react-router-dom";
import AnalyzerForm from "../AnalyzerForm";
import messages from "../../../../languages/en.json";
import * as analyzerService from "../../../../services/analyzerService";

vi.mock("../../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  getAnalyzerTypes: vi.fn(),
  getDefaultConfigs: vi.fn(),
  getDefaultConfig: vi.fn(),
  createAnalyzer: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

const catalog = {
  schemaVersion: "1.0",
  catalogFingerprint: "sha256:catalog",
  summary: { total: 2, inUse: 0, needsAttention: 1, deactivated: 1 },
  types: [
    {
      profileId: "shipped.genexpert",
      revision: 2,
      revisionFingerprint: "sha256:genexpert",
      displayName: "Cepheid GeneXpert MTB/RIF",
      source: "SHIPPED",
      status: "ACTIVE",
      protocol: "ASTM",
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

describe("AnalyzerForm profile revision pin", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    analyzerService.getAnalyzerTypeCatalog.mockImplementation((callback) => {
      callback(catalog);
    });
    analyzerService.getAnalyzerTypes.mockImplementation((filters, callback) => {
      callback([]);
    });
    analyzerService.getDefaultConfigs.mockImplementation((callback) => {
      callback([]);
    });
    analyzerService.getDefaultConfig.mockImplementation(() => {});
    analyzerService.createAnalyzer.mockImplementation((data, callback) => {
      callback({ id: "501", ...data });
    });
  });

  it("restores an active Analyzer Type revision from the URL and submits that exact pin", async () => {
    renderNewAnalyzer("/analyzers/new?profile=shipped.genexpert&revision=2");

    await waitFor(() => {
      expect(analyzerService.getAnalyzerTypeCatalog).toHaveBeenCalledTimes(1);
    });

    expect(
      screen.getByRole("combobox", { name: "Analyzer Type" }),
    ).toHaveTextContent("Cepheid GeneXpert MTB/RIF");
    expect(screen.getByTestId("location")).toHaveTextContent(
      "?profile=shipped.genexpert&revision=2",
    );
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
      analyzerType: "ASTM",
      profileId: "shipped.genexpert",
      profileRevision: 2,
    });
    expect(submitted).not.toHaveProperty("defaultConfigId");
    expect(submitted).not.toHaveProperty("pluginTypeId");
  });
});
