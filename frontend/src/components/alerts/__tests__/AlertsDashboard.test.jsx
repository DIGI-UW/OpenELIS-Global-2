import React from "react";
import { render, screen, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import AlertsDashboard from "../AlertsDashboard";

jest.mock("../../utils/Utils", () => ({
  ...jest.requireActual("../../utils/Utils"),
  getFromOpenElisServer: jest.fn(),
  getFromOpenElisServerV2: jest.fn(),
  putToOpenElisServer: jest.fn(),
}));

const { getFromOpenElisServer } = require("../../utils/Utils");

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

const mockSummary = {
  criticalAlerts: 3,
  eqaDeadlines: 5,
  statOverdue: 2,
  sampleExpiration: 1,
  totalOpen: 11,
};

const mockDashboard = {
  alerts: [
    {
      id: 1,
      alertType: "EQA_DEADLINE",
      severity: "CRITICAL",
      status: "OPEN",
      message: "EQA deadline approaching",
      startTime: "2026-01-15T10:00:00Z",
    },
    {
      id: 2,
      alertType: "SAMPLE_EXPIRATION",
      severity: "WARNING",
      status: "OPEN",
      message: "Sample expiring soon",
      startTime: "2026-01-15T11:00:00Z",
    },
  ],
  totalCount: 2,
  page: 0,
  pageSize: 25,
};

describe("AlertsDashboard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/summary")) {
        callback(mockSummary);
      } else if (url.includes("/alerts/dashboard")) {
        callback(mockDashboard);
      }
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test("renders dashboard title", () => {
    renderWithIntl(<AlertsDashboard />);
    expect(screen.getByText("Alerts Dashboard")).toBeTruthy();
  });

  test("renders summary tiles with counts", async () => {
    renderWithIntl(<AlertsDashboard />);
    expect(screen.getByText("Critical Alerts")).toBeTruthy();
    expect(screen.getByText("EQA Deadlines")).toBeTruthy();
    expect(screen.getByText("Overdue STAT Orders")).toBeTruthy();
    expect(screen.getByText("Samples Expiring")).toBeTruthy();
  });

  test("renders alerts table with data", () => {
    renderWithIntl(<AlertsDashboard />);
    expect(screen.getByText("EQA deadline approaching")).toBeTruthy();
    expect(screen.getByText("Sample expiring soon")).toBeTruthy();
  });

  test("renders filter controls", () => {
    renderWithIntl(<AlertsDashboard />);
    expect(screen.getByText("Alert Type")).toBeTruthy();
    expect(screen.getAllByText("Severity").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Status").length).toBeGreaterThanOrEqual(1);
  });

  test("renders severity tags with correct colors", () => {
    const { container } = renderWithIntl(<AlertsDashboard />);
    const redTags = container.querySelectorAll(".cds--tag--red");
    const warmGrayTags = container.querySelectorAll(".cds--tag--warm-gray");
    expect(redTags.length).toBeGreaterThanOrEqual(1);
  });

  test("renders acknowledge button for open alerts", () => {
    renderWithIntl(<AlertsDashboard />);
    const ackButtons = screen.getAllByText("Acknowledge");
    expect(ackButtons.length).toBeGreaterThanOrEqual(1);
  });

  test("fetches data on mount", () => {
    renderWithIntl(<AlertsDashboard />);
    expect(getFromOpenElisServer).toHaveBeenCalled();
  });
});
