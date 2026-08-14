const routerState = vi.hoisted(() => ({
  location: { pathname: "/analyzers/types", search: "" },
  history: { push: vi.fn(), replace: vi.fn() },
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => routerState.history,
    useLocation: () => routerState.location,
  };
});

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import AnalyzerTypeManagement from "./AnalyzerTypeManagement";

const profile = (overrides = {}) => ({
  profileId: "shipped.mock-hematology",
  revision: 3,
  displayName: "Mock Hematology",
  category: "HEMATOLOGY",
  protocol: "ASTM",
  source: "SHIPPED",
  status: "ACTIVE",
  manufacturer: "OpenELIS",
  model: "Mock Heme",
  legacyVersion: "1.0",
  parentProfileId: null,
  parentRevision: null,
  connectionTestSupported: true,
  testMappings: {
    total: 2,
    bound: 1,
    unresolved: 1,
    ignored: 0,
    missing: 0,
    extra: 0,
  },
  resultValueMappings: {
    total: 2,
    bound: 0,
    unresolved: 2,
    ignored: 0,
    missing: 0,
    extra: 0,
  },
  qcIdentificationRuleCount: 1,
  analyzerCount: 3,
  attentionCodes: ["UNRESOLVED_TEST_MAPPINGS", "RESULT_VALUE_BINDING_REQUIRED"],
  ...overrides,
});

const renderPage = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <AnalyzerTypeManagement />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyzerTypeManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    routerState.location = { pathname: "/analyzers/types", search: "" };
    getFromOpenElisServer.mockImplementation((_endpoint, callback) => {
      callback([profile()]);
    });
  });

  test("renders the lab-facing composed Analyzer Types catalog", async () => {
    renderPage();

    expect(
      await screen.findByRole("heading", { level: 1, name: "Analyzer Types" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "Analyzer type" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "Tests mapped" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "Results mapped" }),
    ).toBeVisible();
    expect(screen.getByRole("columnheader", { name: "Used by" })).toBeVisible();
    expect(screen.getByText("1 / 2 · 50%")).toBeVisible();
    expect(screen.getByText("0 / 2 · 0%")).toBeVisible();
    expect(screen.getByText("3 analyzers")).toBeVisible();
    expect(screen.getByText("OpenELIS · Mock Heme · v1.0")).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Mock Hematology" }),
    ).toHaveAttribute(
      "href",
      "/analyzers/types/shipped.mock-hematology?revision=3",
    );
    expect(screen.queryByText("Plugin Class")).not.toBeInTheDocument();
    expect(screen.queryByText("Identifier Pattern")).not.toBeInTheDocument();
  });

  test("restores bookmarkable filters and applies mapping status locally", async () => {
    routerState.location = {
      pathname: "/analyzers/types",
      search:
        "?q=mock&source=SHIPPED&protocol=ASTM&mappingStatus=INCOMPLETE&showInactive=true",
    };
    getFromOpenElisServer.mockImplementation((_endpoint, callback) => {
      callback([
        profile(),
        profile({
          profileId: "site.complete",
          displayName: "Complete Profile",
          source: "SITE",
          testMappings: {
            total: 1,
            bound: 1,
            unresolved: 0,
            ignored: 0,
            missing: 0,
            extra: 0,
          },
          resultValueMappings: {
            total: 0,
            bound: 0,
            unresolved: 0,
            ignored: 0,
            missing: 0,
            extra: 0,
          },
          attentionCodes: [],
        }),
      ]);
    });

    renderPage();

    expect(await screen.findByText("Mock Hematology")).toBeVisible();
    expect(screen.queryByText("Complete Profile")).not.toBeInTheDocument();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/analyzer/types?q=mock&source=SHIPPED&protocol=ASTM",
      expect.any(Function),
      expect.any(AbortSignal),
    );
    expect(routerState.history.replace).toHaveBeenCalledWith({
      pathname: "/analyzers/types",
      search:
        "q=mock&source=SHIPPED&protocol=ASTM&mappingStatus=INCOMPLETE&showInactive=true",
    });
  });

  test("updates the bookmarkable query and reloads after search", async () => {
    vi.useFakeTimers();
    renderPage();
    const search = await screen.findByRole("searchbox", {
      name: "Search analyzer types",
    });

    fireEvent.change(search, { target: { value: "mindray" } });
    await act(async () => vi.advanceTimersByTime(300));

    expect(routerState.history.replace).toHaveBeenLastCalledWith({
      pathname: "/analyzers/types",
      search: "q=mindray",
    });
    expect(getFromOpenElisServer).toHaveBeenLastCalledWith(
      "/rest/analyzer/types?q=mindray&status=ACTIVE",
      expect.any(Function),
      expect.any(AbortSignal),
    );
    vi.useRealTimers();
  });

  test("shows a catalog error rather than an empty library", async () => {
    getFromOpenElisServer.mockImplementation((_endpoint, callback) => {
      callback({ error: "Analyzer Bridge profile catalog returned HTTP 503" });
    });

    renderPage();

    expect(
      await screen.findByText("Analyzer types could not be loaded"),
    ).toBeVisible();
    expect(
      screen.getByText("Analyzer Bridge profile catalog returned HTTP 503"),
    ).toBeVisible();
  });

  test("routes row lifecycle actions through bookmarkable detail state", async () => {
    renderPage();

    fireEvent.click(
      await screen.findByRole("button", {
        name: "Actions for Mock Hematology",
      }),
    );
    fireEvent.click(screen.getByText("Fork"));

    expect(routerState.history.push).toHaveBeenCalledWith(
      "/analyzers/types/shipped.mock-hematology?revision=3&action=fork",
    );
  });
});
