const routerState = vi.hoisted(() => ({
  location: {
    pathname: "/analyzers/types/site.mock-hematology",
    search: "?revision=3",
  },
  history: { push: vi.fn(), replace: vi.fn() },
  params: { profileId: "site.mock-hematology" },
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => routerState.history,
    useLocation: () => routerState.location,
    useParams: () => routerState.params,
  };
});

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import AnalyzerTypeDetail from "./AnalyzerTypeDetail";

const profile = (overrides = {}) => ({
  profileId: "site.mock-hematology",
  revision: 3,
  displayName: "Mock Hematology",
  category: "HEMATOLOGY",
  protocol: "ASTM",
  source: "SITE",
  status: "ACTIVE",
  manufacturer: "OpenELIS",
  model: "Mock Heme",
  legacyVersion: "1.0",
  parentProfileId: "shipped.mock-hematology",
  parentRevision: 2,
  connectionTestSupported: true,
  bridgeFingerprint: `sha256:${"a".repeat(64)}`,
  bridgeAudit: {
    action: "UPDATED",
    actor: "oe-user",
    markedAt: "2026-08-13T18:30:00Z",
  },
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
  siteBinding: null,
  attentionCodes: ["UNRESOLVED_TEST_MAPPINGS", "RESULT_VALUE_BINDING_REQUIRED"],
  ...overrides,
});

const renderPage = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <AnalyzerTypeDetail />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyzerTypeDetail", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    routerState.location = {
      pathname: "/analyzers/types/site.mock-hematology",
      search: "?revision=3",
    };
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint.endsWith("/history")) {
        callback([
          {
            profile: { profileId: "site.mock-hematology", revision: 3 },
            audit: {
              action: "UPDATED",
              actor: "oe-user",
              markedAt: "2026-08-13T18:30:00Z",
            },
            fingerprint: `sha256:${"a".repeat(64)}`,
          },
        ]);
      } else {
        callback(profile());
      }
    });
  });

  test("renders a linkable lab-facing profile summary", async () => {
    renderPage();

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Mock Hematology",
      }),
    ).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Analyzer Types" }),
    ).toHaveAttribute("href", "/analyzers/types");
    expect(
      screen.getByText("OpenELIS \u00b7 Mock Heme \u00b7 v1.0"),
    ).toBeVisible();
    expect(screen.getByText("1 / 2 \u00b7 50%")).toBeVisible();
    expect(screen.getByText("0 / 2 \u00b7 0%")).toBeVisible();
    expect(screen.getByText("3 analyzers")).toBeVisible();
    expect(screen.getByText("Forked from").parentElement).toHaveTextContent(
      "shipped.mock-hematology \u00b7 revision 2",
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/analyzer/types/site.mock-hematology?revision=3",
      expect.any(Function),
      expect.any(AbortSignal),
    );
  });

  test("restores the history view from the URL", async () => {
    routerState.location = {
      pathname: "/analyzers/types/site.mock-hematology",
      search: "?revision=3&view=history",
    };

    renderPage();

    expect(
      await screen.findByRole("columnheader", { name: "Revision" }),
    ).toBeVisible();
    expect(screen.getByRole("cell", { name: "3" })).toBeVisible();
    expect(screen.getByRole("cell", { name: "Updated" })).toBeVisible();
    expect(screen.getByRole("cell", { name: "oe-user" })).toBeVisible();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/analyzer/types/site.mock-hematology/history",
      expect.any(Function),
      expect.any(AbortSignal),
    );
    expect(routerState.history.replace).toHaveBeenCalledWith({
      pathname: "/analyzers/types/site.mock-hematology",
      search: "revision=3&view=history",
    });
  });

  test("forks the selected revision through the Bridge-owned lifecycle", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_endpoint, _payload, callback) =>
        callback(
          profile({
            profileId: "site.madagascar-hematology",
            revision: 1,
            displayName: "Madagascar Hematology",
          }),
        ),
    );
    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Fork analyzer type" }),
    );
    fireEvent.change(screen.getByLabelText("Analyzer type name"), {
      target: { value: "Madagascar Hematology" },
    });
    fireEvent.change(screen.getByLabelText("Profile ID"), {
      target: { value: "site.madagascar-hematology" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create fork" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/analyzer/types/site.mock-hematology/fork",
      JSON.stringify({
        sourceRevision: 3,
        profileId: "site.madagascar-hematology",
        displayName: "Madagascar Hematology",
      }),
      expect.any(Function),
    );
    expect(routerState.history.push).toHaveBeenCalledWith(
      "/analyzers/types/site.madagascar-hematology?revision=1",
    );
  });

  test("deactivates without offering a destructive delete path", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_endpoint, _payload, callback) =>
        callback(profile({ status: "INACTIVE" })),
    );
    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: /Deactivate$/ }));
    const confirmButtons = screen.getAllByRole("button", {
      name: /Deactivate$/,
    });
    fireEvent.click(confirmButtons[confirmButtons.length - 1]);

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/analyzer/types/site.mock-hematology/deactivate",
      JSON.stringify({}),
      expect.any(Function),
    );
    expect(screen.getByText("Analyzer type deactivated")).toBeVisible();
    expect(screen.getByText("Inactive")).toBeVisible();
    expect(
      screen.queryByRole("button", { name: /delete/i }),
    ).not.toBeInTheDocument();
  });
});
