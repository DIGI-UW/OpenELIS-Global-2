import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import QCDashboard from "../QCDashboard";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  hasPermissionOrGlobalAdmin: vi.fn(() => true),
  postToOpenElisServerFullResponse: vi.fn(),
}));

let violations;

const violation = (id, testName) => ({
  id,
  severity: "REJECTION",
  ruleCode: "MANUAL_FAIL",
  violationDateTime: new Date().toISOString(),
  instrumentName: "Bench",
  testName,
  resolutionStatus: "UNRESOLVED",
});

const renderDashboard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <UserSessionDetailsContext.Provider
          value={{ userSessionDetails: { roles: ["Global Administrator"] } }}
        >
          <QCDashboard initialTab={1} />
        </UserSessionDetailsContext.Provider>
      </MemoryRouter>
    </IntlProvider>,
  );

beforeEach(() => {
  violations = [violation("v1", "Amylase"), violation("v2", "GPT/ALAT")];
  getFromOpenElisServer.mockReset();
  postToOpenElisServerFullResponse.mockReset();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/qc/violations")) {
      const open = url.includes("unresolved=true")
        ? violations.filter((v) => v.resolutionStatus === "UNRESOLVED")
        : violations;
      callback(open.map((v) => ({ ...v })));
      return;
    }
    callback(url.includes("summary") ? {} : []);
  });
  postToOpenElisServerFullResponse.mockImplementation(
    (url, _body, callback) => {
      const id = url.split("/")[4];
      violations = violations.map((v) =>
        v.id === id ? { ...v, resolutionStatus: "ACKNOWLEDGED" } : v,
      );
      callback({ ok: true });
    },
  );
});

describe("QCDashboard acknowledgement", () => {
  it("updates the Alerts tab when a violation is acknowledged from the banner", async () => {
    renderDashboard();
    expect(
      await screen.findByText(
        "2 unacknowledged violation(s) requiring attention",
      ),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByTestId("banner-acknowledge-v2"));

    expect(
      await screen.findByText(
        "1 unacknowledged violation(s) requiring attention",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText("1 violation(s) have been acknowledged"),
    ).toBeInTheDocument();
  });

  it("updates the banner when a violation is acknowledged from the Alerts tab", async () => {
    renderDashboard();
    expect(
      await screen.findByText("2 active QC violation(s) require attention"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByTestId("alert-acknowledge-v1"));

    await waitFor(() =>
      expect(
        screen.getByText("1 active QC violation(s) require attention"),
      ).toBeInTheDocument(),
    );
  });
});
