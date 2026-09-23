import React from "react";
import { act, render, screen, within } from "@testing-library/react";
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
            profileId: "genexpert-astm",
            profileRevision: 1,
            testUnitIds: ["1"],
          },
          {
            id: "2",
            name: "Second pinned analyzer",
            type: "ASTM",
            status: "SETUP",
            profileId: "genexpert-astm",
            profileRevision: 1,
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

  it("presents the pinned Bridge profile by its catalog display name", async () => {
    renderList();

    expect(await screen.findByTestId("analyzer-name-1")).toHaveTextContent(
      "M1 GeneXpert A",
    );
    expect(screen.getByTestId("analyzer-type-1")).toHaveTextContent(
      "Cepheid GeneXpert ASTM",
    );
    expect(screen.getByTestId("analyzer-type-2")).toHaveTextContent(
      "Cepheid GeneXpert ASTM",
    );

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

  it("does not expose an internal profile id while the catalog loads", async () => {
    let resolveCatalog;
    getAnalyzerTypeCatalog.mockImplementation((callback) => {
      resolveCatalog = callback;
    });

    renderList();

    const typeCell = await screen.findByTestId("analyzer-type-1");
    expect(typeCell).not.toHaveTextContent("genexpert-astm");

    await act(async () => {
      resolveCatalog({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"a".repeat(64)}`,
        summary: {
          total: 1,
          inUse: 1,
          needsAttention: 0,
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
      });
    });
    expect(typeCell).toHaveTextContent("Cepheid GeneXpert ASTM");
  });
});
