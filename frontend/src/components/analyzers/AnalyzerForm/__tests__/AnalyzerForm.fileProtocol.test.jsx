import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import AnalyzerForm from "../AnalyzerForm";
import messages from "../../../../languages/en.json";
import * as analyzerService from "../../../../services/analyzerService";

vi.mock("../../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  createAnalyzer: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

const catalog = {
  schemaVersion: "1.0",
  catalogFingerprint: "sha256:catalog",
  summary: { total: 2, inUse: 2, needsAttention: 2, deactivated: 0 },
  types: [
    {
      profileId: "site.file",
      revision: 3,
      revisionFingerprint: "sha256:file",
      displayName: "Site FILE Analyzer",
      source: "SITE",
      status: "ACTIVE",
      protocol: "FILE",
    },
    {
      profileId: "shipped.astm",
      revision: 2,
      revisionFingerprint: "sha256:astm",
      displayName: "Shipped ASTM Analyzer",
      source: "SHIPPED",
      status: "ACTIVE",
      protocol: "ASTM",
    },
  ],
};

const renderAtEditRoute = (analyzerId) =>
  render(
    <MemoryRouter initialEntries={[`/analyzers/${analyzerId}/edit`]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:id/edit">
          <AnalyzerForm />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyzerForm protocol-specific instance fields", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    analyzerService.getAnalyzerTypeCatalog.mockImplementation((callback) => {
      callback(catalog);
    });
  });

  it("keeps Bridge-owned FILE parsing fields out of analyzer setup", async () => {
    analyzerService.getAnalyzer.mockImplementation((id, callback) => {
      callback({
        id,
        name: "FILE Bench 1",
        analyzerType: "FILE",
        profileId: "site.file",
        profileRevision: 3,
        status: "SETUP",
        importDirectory: "/data/analyzer-imports/file-bench-1",
      });
    });

    renderAtEditRoute("file-1");

    expect(
      await screen.findByRole("combobox", { name: "Analyzer Type" }),
    ).toHaveTextContent("Site FILE Analyzer");
    expect(
      screen.queryByTestId("analyzer-form-connection-fields"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId("analyzer-form-import-directory-input"),
    ).toHaveValue("/data/analyzer-imports/file-bench-1");

    expect(
      screen.queryByTestId("analyzer-form-file-format-dropdown"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-file-pattern-input"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-column-mappings-input"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-delimiter-input"),
    ).not.toBeInTheDocument();
  });

  it("shows connection fields for an ASTM profile revision", async () => {
    analyzerService.getAnalyzer.mockImplementation((id, callback) => {
      callback({
        id,
        name: "ASTM Bench 1",
        analyzerType: "ASTM",
        profileId: "shipped.astm",
        profileRevision: 2,
        ipAddress: "192.168.1.100",
        port: 9600,
        status: "SETUP",
      });
    });

    renderAtEditRoute("astm-1");

    expect(
      await screen.findByRole("combobox", { name: "Analyzer Type" }),
    ).toHaveTextContent("Shipped ASTM Analyzer");
    expect(
      screen.getByTestId("analyzer-form-connection-fields"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("analyzer-form-ip-input")).toHaveValue(
      "192.168.1.100",
    );
    expect(screen.getByTestId("analyzer-form-port-input")).toHaveValue("9600");
    expect(
      screen.getByTestId("analyzer-form-test-connection-button"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-form-import-directory-input"),
    ).not.toBeInTheDocument();
  });
});
