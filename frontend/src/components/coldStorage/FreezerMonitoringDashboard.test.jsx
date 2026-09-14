import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen, wait } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import FreezerMonitoringDashboard from "./FreezerMonitoringDashboard";
import { deleteAlert, fetchFreezerStatus, fetchOpenAlerts } from "./api";

vi.mock("./api", () => ({
  fetchFreezerStatus: vi.fn(() => Promise.resolve([])),
  fetchOpenAlerts: vi.fn(() => Promise.resolve([])),
  acknowledgeAlert: vi.fn(() => Promise.resolve()),
  resolveAlert: vi.fn(() => Promise.resolve()),
  deleteAlert: vi.fn(() => Promise.resolve()),
  createCorrectiveAction: vi.fn(() => Promise.resolve()),
  fetchAlertDetails: vi.fn(() =>
    Promise.resolve({
      id: 1104,
      alertType: "FREEZER_TEMPERATURE",
      alertEntityType: "Freezer",
      alertEntityId: 47,
      severity: "CRITICAL",
      status: "OPEN",
      message: "Temperature threshold violated",
    }),
  ),
}));

vi.mock("./CorrectiveActions", () => ({
  default: () => <div />,
  getActionTypes: () => [{ id: "CALIBRATION", text: "Calibration" }],
}));
vi.mock("./HistoricalTrends", () => ({ default: () => <div /> }));
vi.mock("./Reports", () => ({ default: () => <div /> }));
vi.mock("./Settings", () => ({ default: () => <div /> }));
vi.mock("./DeviceHistoryExpansion", () => ({ default: () => <div /> }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => <div /> }));

const twentyMinutesAgo = () =>
  new Date(Date.now() - 20 * 60 * 1000).toISOString();

const unitReportingTwentyMinutesAgo = (staleAfterSeconds) => ({
  freezerId: 47,
  freezerName: "Demo Vaccine Fridge",
  locationName: "Demo Cold Room",
  deviceType: "Fridge",
  protocol: "TCP",
  status: "NORMAL",
  temperatureCelsius: 4.0,
  targetTemperatureCelsius: 4.0,
  recordedAt: twentyMinutesAgo(),
  staleAfterSeconds,
});

const OPEN_ALERT = {
  id: 1104,
  alertType: "FREEZER_TEMPERATURE",
  alertEntityType: "Freezer",
  alertEntityId: 47,
  severity: "CRITICAL",
  status: "OPEN",
  message: "Temperature threshold violated",
  startTime: "2026-09-08T07:31:12Z",
  contextData: '{"temperature": 5.0}',
  freezer: { id: 47, name: "Demo Vaccine Fridge", code: "Demo Cold Room" },
};

const SECOND_OPEN_ALERT = {
  ...OPEN_ALERT,
  id: 1105,
  alertEntityId: 48,
  freezer: { id: 48, name: "Demo Blood Bank Freezer", code: "Demo Lab" },
};

const renderDashboard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles: ["Global Administrator"] } }}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <FreezerMonitoringDashboard />
        </NotificationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

/**
 * Deleting an alert from the detail modal has to clear it from the Active
 * Alerts table straight away. Left stale, the row's View and Delete both hit a
 * 404 on an id the server no longer has, so a successful delete reads as an
 * error until the 60s auto-refresh happens to fire.
 */
describe("FreezerMonitoringDashboard alert detail modal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchOpenAlerts.mockResolvedValue([OPEN_ALERT]);
  });

  it("reloads the alert list once the detail modal has deleted an alert", async () => {
    renderDashboard();

    expect(await screen.findByText("Demo Vaccine Fridge")).toBeInTheDocument();
    await wait(() => expect(fetchOpenAlerts).toHaveBeenCalledTimes(1));

    fireEvent.click(
      screen.getByRole("button", { name: /view alert details/i }),
    );
    fireEvent.click(await screen.findByText("Delete Alert"));
    fireEvent.click(await screen.findByText("Delete permanently"));

    await wait(() => expect(deleteAlert).toHaveBeenCalledWith(1104));
    await wait(() => expect(fetchOpenAlerts).toHaveBeenCalledTimes(2));
  });
});

/**
 * The offline cutoff has to follow the backend's configured poll cadence. A
 * site polling less often than the client's old fixed 15 minutes would other-
 * wise show every healthy device as permanently Offline.
 */
describe("FreezerMonitoringDashboard staleness tag", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchOpenAlerts.mockResolvedValue([]);
  });

  it("does not tag a device offline when the server's cutoff is wider than the reading's age", async () => {
    fetchFreezerStatus.mockResolvedValue([
      unitReportingTwentyMinutesAgo(60 * 60),
    ]);

    renderDashboard();

    expect(await screen.findByText("Demo Vaccine Fridge")).toBeInTheDocument();
    expect(screen.queryByText("Offline")).not.toBeInTheDocument();
  });

  it("tags a device offline once the reading is older than the server's cutoff", async () => {
    fetchFreezerStatus.mockResolvedValue([
      unitReportingTwentyMinutesAgo(5 * 60),
    ]);

    renderDashboard();

    expect(await screen.findByText("Demo Vaccine Fridge")).toBeInTheDocument();
    expect(screen.getByText("Offline")).toBeInTheDocument();
  });
});

/**
 * Carbon's DataTable syncs its row ids in an effect, so the render pass right
 * after a refresh drops an alert still lists the removed id. Every alert the
 * row body reads is looked up in activeAlerts by that id, so an unguarded
 * lookup throws during render and blanks the whole page.
 */
describe("FreezerMonitoringDashboard active alerts table", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchFreezerStatus.mockResolvedValue([]);
  });

  it("keeps rendering when a refresh drops an alert the table still holds a row id for", async () => {
    fetchOpenAlerts
      .mockResolvedValueOnce([OPEN_ALERT, SECOND_OPEN_ALERT])
      .mockResolvedValue([OPEN_ALERT]);

    renderDashboard();

    expect(
      await screen.findByText("Demo Blood Bank Freezer"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Refresh" }));

    await wait(() => expect(fetchOpenAlerts).toHaveBeenCalledTimes(2));
    await wait(() =>
      expect(
        screen.queryByText("Demo Blood Bank Freezer"),
      ).not.toBeInTheDocument(),
    );
    expect(screen.getByText("Demo Vaccine Fridge")).toBeInTheDocument();
  });
});
