import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import InHousePanelsPage from "../InHousePanelsPage";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import {
  fetchInHouseSchemes,
  fetchPanelsForScheme,
  unblindPanel,
} from "../inHouseApi";

vi.mock("../inHouseApi", () => ({
  fetchInHouseSchemes: vi.fn(),
  fetchPanelsForScheme: vi.fn(),
  downloadLabelSheet: vi.fn(),
  unblindPanel: vi.fn(),
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const renderPage = (permissions) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { authenticated: true, roles: [], permissions },
          errorLoadingSessionDetails: false,
          isCheckingLogin: () => false,
          logout: vi.fn(),
        }}
      >
        <MemoryRouter>
          <InHousePanelsPage />
        </MemoryRouter>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

describe("InHousePanelsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchInHouseSchemes.mockImplementation((callback) =>
      callback([{ id: "3", name: "In-house malaria RDT" }]),
    );
    fetchPanelsForScheme.mockImplementation((_schemeId, callback) =>
      callback([]),
    );
  });

  // F-27: the launcher opened four steps of panel design in front of a seal
  // this persona cannot perform.
  it("hides the wizard launcher from a persona without the manage grant", () => {
    renderPage(["qa.view.eqa", "qa.eqa.participant"]);

    expect(
      screen.queryByRole("button", { name: "Launch blinding wizard" }),
    ).toBeNull();
    expect(
      screen.getByText("Read-only view of in-house panels"),
    ).toBeInTheDocument();
  });

  it("offers the launcher to a persona holding the manage grant", () => {
    renderPage(["qa.view.eqa", "qa.manage.eqa"]);

    expect(
      screen.getByRole("button", { name: "Launch blinding wizard" }),
    ).toBeInTheDocument();
    expect(screen.queryByText("Read-only view of in-house panels")).toBeNull();
  });

  it("keeps the chosen scheme's panels when an earlier scheme's reply lands late", () => {
    fetchInHouseSchemes.mockImplementation((callback) =>
      callback([
        { id: "3", name: "Scheme A" },
        { id: "4", name: "Scheme B" },
      ]),
    );
    const replies = {};
    fetchPanelsForScheme.mockImplementation((schemeId, callback) => {
      replies[schemeId] = callback;
    });
    renderPage(["qa.view.eqa"]);

    fireEvent.change(screen.getByLabelText("In-house scheme"), {
      target: { value: "4" },
    });
    act(() => replies["4"]([{ id: 2, panelName: "Panel of B" }]));
    act(() => replies["3"]([{ id: 1, panelName: "Panel of A" }]));

    expect(screen.getByText("Panel of B")).toBeInTheDocument();
    expect(screen.queryByText("Panel of A")).toBeNull();
  });

  // OGC-1408: an unblind whose request got no answer (status 0) must not be
  // reported as done, and the panel list is not reloaded over a seal that
  // may still be in place.
  it("an unblind that got no answer reports the failure and keeps the panel", () => {
    fetchPanelsForScheme.mockImplementation((_schemeId, callback) =>
      callback([
        { id: 7, panelName: "Panel 7", status: "DISTRIBUTED", sampleCount: 3 },
      ]),
    );
    unblindPanel.mockImplementation((_panelId, callback) =>
      callback({
        error: "Failed to fetch",
        message: "Failed to fetch",
        status: 0,
      }),
    );
    renderPage(["qa.view.eqa", "qa.manage.eqa"]);

    fireEvent.click(screen.getByRole("button", { name: "Unblind now" }));

    expect(
      screen.getByText(messages["eqa.inhouse.unblind.error"]),
    ).toBeInTheDocument();
    expect(screen.queryByText(messages["eqa.inhouse.unblind.done"])).toBeNull();
    expect(screen.queryByText("Failed to fetch")).toBeNull();
    expect(fetchPanelsForScheme).toHaveBeenCalledTimes(1);
  });
});
