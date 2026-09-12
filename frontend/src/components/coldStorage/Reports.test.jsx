import React from "react";
import { vi } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/contexts";
import Reports from "./Reports";
import { fetchReportExcursions, fetchAuditTrail } from "./api";

vi.mock("./api", () => ({
  fetchReportExcursions: vi.fn(),
  fetchAuditTrail: vi.fn(),
  downloadReportDirect: vi.fn(),
}));

const EXCURSION = {
  alertId: 1104,
  freezerId: 47,
  freezerName: "Demo Vaccine Fridge",
  locationName: "Demo Cold Room",
  startTime: "2026-09-08T07:31:12Z",
  endTime: "2026-09-08T07:32:13Z",
  durationSeconds: 60,
  minTemperature: -17.0,
  maxTemperature: -17.0,
  severity: "WARNING",
  status: "RESOLVED",
};

const renderReports = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        <Reports />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("Reports", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchAuditTrail.mockResolvedValue([]);
  });

  /**
   * The rows were built with the payload on a `source` key that is not a
   * declared DataTable header, so Carbon dropped it, `row.source` was
   * undefined and every row was skipped - leaving a table of headers with no
   * empty-state row either, because rows.length was non-zero.
   */
  it("renders a row per excursion the endpoint returns", async () => {
    fetchReportExcursions.mockResolvedValue([EXCURSION]);

    renderReports();

    expect(await screen.findByText("Demo Vaccine Fridge")).toBeInTheDocument();
    expect(screen.getByText("Demo Cold Room")).toBeInTheDocument();
    expect(
      screen.queryByText("No excursions available for the selected filters."),
    ).not.toBeInTheDocument();
  });

  it("shows the empty state when there are no excursions", async () => {
    fetchReportExcursions.mockResolvedValue([]);

    renderReports();

    expect(
      await screen.findByText(
        "No excursions available for the selected filters.",
      ),
    ).toBeInTheDocument();
  });

  /**
   * The queried window defaults to the last seven days, but no value reached
   * either DatePickerInput, so the operator could not see what the report
   * actually covered.
   */
  it("shows the date range it is going to query", async () => {
    fetchReportExcursions.mockResolvedValue([]);

    renderReports();

    const start = await screen.findByLabelText("Start date");
    const end = screen.getByLabelText("End date");

    const mmddyyyy = /^\d{2}\/\d{2}\/\d{4}$/;
    expect(start.value).toMatch(mmddyyyy);
    expect(end.value).toMatch(mmddyyyy);

    const days =
      (new Date(end.value) - new Date(start.value)) / (1000 * 60 * 60 * 24);
    expect(days).toBe(7);
  });
});
