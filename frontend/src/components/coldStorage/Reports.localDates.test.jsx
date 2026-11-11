import React from "react";
import { vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import Reports from "./Reports";
import {
  downloadReportDirect,
  fetchAuditTrail,
  fetchReportExcursions,
} from "./api";

// OGC-1378: the report's LocalDate range was cut from toISOString(), so in
// Papua New Guinea (UTC+10) it started and ended a day early. At 06:00 local
// on 30 September it is still 29 September in UTC.
vi.hoisted(() => {
  process.env.TZ = "Pacific/Port_Moresby";
  vi.useFakeTimers({ toFake: ["Date"] });
  vi.setSystemTime(new Date("2026-09-29T20:00:00Z"));
});

vi.mock("./api", () => ({
  fetchReportExcursions: vi.fn(),
  fetchAuditTrail: vi.fn(),
  downloadReportDirect: vi.fn(),
}));

afterAll(() => {
  vi.useRealTimers();
});

describe("Reports date range east of UTC (OGC-1378)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchAuditTrail.mockResolvedValue([]);
    fetchReportExcursions.mockResolvedValue([]);
    downloadReportDirect.mockResolvedValue(new Blob(["pdf"]));
    window.URL.createObjectURL = vi.fn(() => "blob:report");
    window.URL.revokeObjectURL = vi.fn();
  });

  it("covers whole local days, in the preview and in the report", async () => {
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

    await act(async () => {
      fireEvent.click(
        await screen.findByRole("button", { name: /Generate Report/ }),
      );
    });

    expect(fetchReportExcursions).toHaveBeenCalledWith(
      expect.objectContaining({
        start: "2026-09-22T14:00:00.000Z",
        end: "2026-09-30T13:59:59.999Z",
      }),
    );
    expect(downloadReportDirect).toHaveBeenCalledWith(
      expect.objectContaining({
        startDate: "2026-09-23",
        endDate: "2026-09-30",
      }),
    );
  });
});
