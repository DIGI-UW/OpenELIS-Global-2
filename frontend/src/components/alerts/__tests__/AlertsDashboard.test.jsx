import React from "react";
import { act, render, screen, within, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import AlertsDashboard from "../AlertsDashboard";
import { getFromOpenElisServer, putToOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    getFromOpenElisServerV2: vi.fn(),
    putToOpenElisServer: vi.fn(),
  };
});

// Replaced inline utils require

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>{component}</MemoryRouter>
    </IntlProvider>,
  );
};

const alertTypes = [
  "EQA_DEADLINE",
  "SAMPLE_EXPIRATION",
  "EQA_SUBMISSION_FAILED",
  "MICROBIOLOGY_CRITICAL",
  "REFERRAL_REJECTED",
];

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
    {
      id: 3,
      alertType: "EQA_SUBMISSION_FAILED",
      severity: "CRITICAL",
      status: "OPEN",
      message: "Automatic EQA submission failed 5 times",
      startTime: "2026-01-15T12:00:00Z",
    },
  ],
  totalCount: 3,
  page: 0,
  pageSize: 25,
};

describe("AlertsDashboard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/types")) {
        callback(alertTypes);
      } else if (url.includes("/summary")) {
        callback(mockSummary);
      } else if (url.includes("/alerts/dashboard")) {
        callback(mockDashboard);
      }
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test("renders dashboard title", () => {
    renderWithIntl(<AlertsDashboard />);
    // the page name also appears as the current breadcrumb, so scope to the heading
    expect(
      screen.getByRole("heading", { name: "Alerts Dashboard" }),
    ).toBeTruthy();
  });

  test("renders the full breadcrumb path with Home clickable", () => {
    renderWithIntl(<AlertsDashboard />);
    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(breadcrumb).toBeTruthy();
    expect(breadcrumb.querySelector('a[href="/"]')?.textContent).toBe("Home");
    // the current page is named but not a link
    expect(breadcrumb.textContent).toContain("Alerts Dashboard");
    expect(
      Array.from(breadcrumb.querySelectorAll("a")).map((a) => a.textContent),
    ).toEqual(["Home"]);
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

  // Every alert type the enum can produce needs a label, or the Type column
  // prints the raw enum beside rows that read as English.
  test("every alert type reads as a label, not as its enum", () => {
    renderWithIntl(<AlertsDashboard />);

    // The filter's own options carry the same labels, so read the table.
    const table = document.querySelector("table");
    expect(within(table).getByText("EQA Deadline")).toBeTruthy();
    expect(within(table).getByText("EQA Submission Failed")).toBeTruthy();
    expect(within(table).queryByText("EQA_SUBMISSION_FAILED")).toBeNull();
  });

  test("Created column shows the time as the server formatted it", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/types")) {
        callback(alertTypes);
      } else if (url.includes("/summary")) {
        callback(mockSummary);
      } else if (url.includes("/alerts/dashboard")) {
        callback({
          alerts: [
            {
              ...mockDashboard.alerts[0],
              startTime: "2026-01-15T10:00:00+03:00",
              startTimeForDisplay: "15/01/2026 10:00",
            },
          ],
          totalCount: 1,
        });
      }
    });

    renderWithIntl(<AlertsDashboard />);

    const table = document.querySelector("table");
    expect(within(table).getByText("15/01/2026 10:00")).toBeTruthy();
  });

  test("Alert Type filter offers the types the server lists, by label", async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderWithIntl(<AlertsDashboard />);

    const box = screen.getByRole("combobox", { name: "Alert Type" });
    await user.click(box);

    expect(
      within(document.getElementById(box.getAttribute("aria-controls")))
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual([
      "EQA Deadline",
      "Sample Expiration",
      "EQA Submission Failed",
      "Microbiology Critical",
      "Referral Rejected",
    ]);
  });

  test("Alert Type is searched by typing and filters the alerts by the chosen type (OGC-1515)", async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderWithIntl(<AlertsDashboard />);
    const box = screen.getByRole("combobox", { name: "Alert Type" });

    await user.type(box, "eqa");
    expect(
      within(document.getElementById(box.getAttribute("aria-controls")))
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual(["EQA Deadline", "EQA Submission Failed"]);
    await user.click(
      screen.getByRole("option", { name: "EQA Submission Failed" }),
    );

    await act(async () => {
      vi.runOnlyPendingTimers();
    });
    const dashboardCalls = getFromOpenElisServer.mock.calls
      .map(([url]) => url)
      .filter((url) => url.startsWith("/rest/alerts/dashboard?"));
    expect(dashboardCalls[dashboardCalls.length - 1]).toContain(
      "type=EQA_SUBMISSION_FAILED",
    );
  });

  test("an acknowledged alert offers Resolve, which posts the comment to resolve", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/types")) {
        callback(alertTypes);
      } else if (url.includes("/summary")) {
        callback(mockSummary);
      } else if (url.includes("/alerts/dashboard")) {
        callback({
          alerts: [{ ...mockDashboard.alerts[1], status: "ACKNOWLEDGED" }],
          totalCount: 1,
        });
      }
    });
    renderWithIntl(<AlertsDashboard />);

    fireEvent.click(screen.getByRole("button", { name: "Resolve" }));
    const modal = document.querySelector(".cds--modal.is-visible");
    fireEvent.change(within(modal).getByRole("textbox"), {
      target: { value: "Sample re-run" },
    });
    fireEvent.click(within(modal).getByRole("button", { name: "Resolve" }));

    expect(putToOpenElisServer).toHaveBeenCalledWith(
      "/rest/alerts/dashboard/2/resolve",
      JSON.stringify({ notes: "Sample re-run" }),
      expect.any(Function),
    );
  });

  test("a failed acknowledge is reported instead of passing silently", () => {
    putToOpenElisServer.mockImplementation((url, payload, callback) =>
      callback(500),
    );
    renderWithIntl(<AlertsDashboard />);

    fireEvent.click(screen.getAllByRole("button", { name: "Acknowledge" })[1]);
    const modal = document.querySelector(".cds--modal.is-visible");
    fireEvent.click(within(modal).getByRole("button", { name: "Acknowledge" }));

    expect(within(modal).getByText("Failed to acknowledge alert")).toBeTruthy();
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

  test("offers the microbiology critical alert type filter", async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderWithIntl(<AlertsDashboard />);

    await user.click(screen.getByRole("combobox", { name: "Alert Type" }));

    expect(
      screen.getByRole("option", { name: "Microbiology Critical" }),
    ).toBeTruthy();
  });

  test("renders a microbiology critical alert row", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/types")) {
        callback(alertTypes);
      } else if (url.includes("/summary")) {
        callback(mockSummary);
      } else if (url.includes("/alerts/dashboard")) {
        callback({
          alerts: [
            {
              id: 3,
              alertType: "MICROBIOLOGY_CRITICAL",
              severity: "CRITICAL",
              status: "OPEN",
              message: "Positive blood culture called",
              startTime: "2026-01-15T12:00:00Z",
            },
          ],
          totalCount: 1,
          page: 0,
          pageSize: 25,
        });
      }
    });

    renderWithIntl(<AlertsDashboard />);

    expect(screen.getByText("Positive blood culture called")).toBeTruthy();
  });
});
