import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getAnalyzers,
  getAnalyzerTypeCatalog,
} from "../../../services/analyzerService";
import AnalyzersList from "./AnalyzersList";

vi.mock("../../../services/analyzerService", () => ({
  createAnalyzer: vi.fn(),
  getAnalyzer: vi.fn(),
  getAnalyzers: vi.fn(),
  getAnalyzerLabUnits: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  getAnalyzerTypeMapping: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

const renderList = () =>
  render(
    <MemoryRouter initialEntries={["/analyzers"]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers">
          <AnalyzersList />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyzersList profile-backed presentation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzers.mockImplementation((_filters, callback) =>
      callback({
        analyzers: [
          {
            id: "1",
            name: "M1 GeneXpert A",
            type: "ASTM",
            status: "SETUP",
            pluginLoaded: false,
            profileId: "genexpert-astm",
            profileRevision: 1,
            profileBindingStatus: "PINNED",
            testUnitIds: ["1"],
          },
          {
            id: "2",
            name: "Unbound analyzer",
            type: "ASTM",
            status: "SETUP",
            pluginLoaded: false,
            profileBindingStatus: "UNBOUND",
            testUnitIds: ["1"],
          },
        ],
      }),
    );
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"a".repeat(64)}`,
        summary: {
          total: 1,
          inUse: 1,
          needsAttention: 1,
          deactivated: 0,
        },
        types: [
          {
            profileId: "genexpert-astm",
            revision: 1,
            revisionFingerprint: `sha256:${"b".repeat(64)}`,
            displayName: "Cepheid GeneXpert ASTM",
            source: "SHIPPED",
            status: "ACTIVE",
            protocol: "ASTM",
          },
        ],
      }),
    );
  });

  it("presents the pinned Bridge profile without an OE plugin warning", async () => {
    renderList();

    expect(await screen.findByTestId("analyzer-name-1")).toHaveTextContent(
      "M1 GeneXpert A",
    );
    expect(screen.getByTestId("analyzer-type-1")).toHaveTextContent(
      "Cepheid GeneXpert ASTM",
    );
    expect(screen.queryByTestId("plugin-warning-1")).not.toBeInTheDocument();
    expect(screen.getByTestId("plugin-warning-2")).toBeInTheDocument();
    expect(screen.getByTestId("stat-plugin-warnings")).toHaveTextContent("1");

    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      "Analyzers",
    );
    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(
      within(breadcrumb).getByRole("link", { name: "Home" }),
    ).toHaveAttribute("href", "/");
    expect(breadcrumb.querySelector('[aria-current="page"]')).toHaveTextContent(
      "Analyzers",
    );
  });
});
