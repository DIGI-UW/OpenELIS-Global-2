import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import InHousePanelsPage from "../InHousePanelsPage";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import { fetchInHouseSchemes, fetchPanelsForScheme } from "../inHouseApi";

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
});
