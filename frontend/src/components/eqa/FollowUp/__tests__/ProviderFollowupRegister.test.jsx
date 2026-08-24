import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import ProviderFollowupRegister from "../ProviderFollowupRegister";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const REGISTER = [
  {
    id: 12,
    schemeId: 3,
    schemeName: "Regional CD4 PT",
    schemeType: "REGIONAL_PT",
    cycleId: 9,
    cycleNumber: 4,
    cycleName: "2026 Round 4",
    participantOrgId: 550,
    organizationName: "Mbeya Regional Lab",
    followupStatus: "NOTIFIED",
    notifiedAt: "2026-08-20 09:15:00.0",
    responseReceivedAt: null,
    resolutionNotes: null,
    persistentFailureFlag: true,
    results: [
      {
        testId: 71,
        testName: "CD4 count",
        reported: "400",
        target: "100",
        zScore: 3.2,
        performanceStatus: "UNACCEPTABLE",
      },
    ],
  },
];

const renderPage = ({
  permissions = ["qa.view.eqa", "qa.eqa.provider"],
} = {}) =>
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
          <ProviderFollowupRegister />
        </MemoryRouter>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const jsonResponse = (ok, body) => ({ ok, json: () => Promise.resolve(body) });

const expand = () =>
  fireEvent.click(screen.getAllByRole("button", { name: /triage/i })[0]);

describe("ProviderFollowupRegister", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback(REGISTER),
    );
  });

  it("lists the participating laboratory, its cycle and its persistent-failure flag", async () => {
    renderPage();

    expect(await screen.findByText("Mbeya Regional Lab")).toBeInTheDocument();
    expect(screen.getByText("2026 Round 4")).toBeInTheDocument();
    expect(screen.getByText("Persistent failure")).toBeInTheDocument();
    expand();
    expect(screen.getByText("CD4 count")).toBeInTheDocument();
    expect(screen.getByText("3.2")).toBeInTheDocument();
  });

  it("moves the row through triage without asking for notes on the early steps", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(
        jsonResponse(true, {
          followupId: 12,
          followupStatus: "RESPONSE_RECEIVED",
        }),
      ),
    );
    renderPage();

    await screen.findByText("Mbeya Regional Lab");
    expand();
    fireEvent.click(screen.getByRole("button", { name: "Record response" }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/eqa/provider/followups/12/status",
        JSON.stringify({ target: "RESPONSE_RECEIVED", notes: null }),
        expect.any(Function),
      ),
    );
  });

  it("asks for notes before resolving and sends them with the transition", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(true, { followupId: 12, followupStatus: "RESOLVED" })),
    );
    renderPage();

    await screen.findByText("Mbeya Regional Lab");
    expand();
    fireEvent.click(screen.getByRole("button", { name: "Resolve" }));
    fireEvent.change(screen.getByLabelText("Notes"), {
      target: { value: "Analyser recalibrated" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/eqa/provider/followups/12/status",
        JSON.stringify({
          target: "RESOLVED",
          notes: "Analyser recalibrated",
        }),
        expect.any(Function),
      ),
    );
  });

  it("falls back to a downloadable CSV when the lab has no contact email", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(
        jsonResponse(true, { followupId: 12, emailed: false, recipient: null }),
      ),
    );
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});
    global.URL.createObjectURL = vi.fn(() => "blob:csv");
    global.URL.revokeObjectURL = vi.fn();
    renderPage();

    await screen.findByText("Mbeya Regional Lab");
    expand();
    fireEvent.click(screen.getByRole("button", { name: "Notify lab" }));

    await waitFor(() => expect(click).toHaveBeenCalled());
    expect(
      await screen.findByText(/downloaded as CSV to send by hand/),
    ).toBeInTheDocument();
    click.mockRestore();
  });

  it("shows the server's own refusal when a triage move is rejected", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(
        jsonResponse(false, {
          error: "Cannot move a follow-up from RESOLVED to UNDER_INVESTIGATION",
        }),
      ),
    );
    renderPage();

    await screen.findByText("Mbeya Regional Lab");
    expand();
    fireEvent.click(screen.getByRole("button", { name: "Investigate" }));

    expect(
      await screen.findByText(
        "Cannot move a follow-up from RESOLVED to UNDER_INVESTIGATION",
      ),
    ).toBeInTheDocument();
  });

  it("offers no triage to a viewer without the provider grant", async () => {
    renderPage({ permissions: ["qa.view.eqa"] });

    await screen.findByText("Mbeya Regional Lab");
    expand();
    expect(
      screen.queryByRole("button", { name: "Notify lab" }),
    ).not.toBeInTheDocument();
  });
});
