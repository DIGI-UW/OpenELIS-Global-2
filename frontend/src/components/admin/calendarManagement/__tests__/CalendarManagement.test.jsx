import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import CalendarManagement from "../CalendarManagement";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../../components/common/PageBreadCrumb", () => {
  return {
    default: function MockBreadCrumb() {
      return <div data-testid="breadcrumb" />;
    },
  };
});

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

// Replaced inline utils require

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

const mockHolidays = {
  year: 2026,
  holidays: [
    {
      id: 1,
      date: "2026-01-01",
      name: "New Year's Day",
      isRecurring: true,
      isActive: true,
      dayOfWeek: "Thu",
      isWeekendDay: false,
    },
    {
      id: 2,
      date: "2026-04-07",
      name: "Genocide Memorial Day",
      isRecurring: true,
      isActive: true,
      dayOfWeek: "Tue",
      isWeekendDay: false,
    },
    {
      id: 3,
      date: "2026-12-25",
      name: "Christmas Day",
      isRecurring: true,
      isActive: false,
      dayOfWeek: "Fri",
      isWeekendDay: false,
    },
  ],
};

describe("CalendarManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/calendar/holidays")) {
        callback(mockHolidays);
      } else if (url.includes("/rest/calendar/weekends")) {
        callback({ weekendDays: [0, 6] });
      }
    });
  });

  test("renders page title and description", async () => {
    renderWithIntl(<CalendarManagement />);
    expect(screen.getByText("Calendar Management")).toBeInTheDocument();
    expect(screen.getByText(/Manage public holidays/)).toBeInTheDocument();
  });

  test("renders holiday table with data", async () => {
    renderWithIntl(<CalendarManagement />);
    await waitFor(() => {
      expect(screen.getByText("New Year's Day")).toBeInTheDocument();
      expect(screen.getByText("Genocide Memorial Day")).toBeInTheDocument();
      expect(screen.getByText("Christmas Day")).toBeInTheDocument();
    });
  });

  test("renders Add Holiday button", async () => {
    renderWithIntl(<CalendarManagement />);
    expect(screen.getByTestId("add-holiday-button")).toBeInTheDocument();
  });

  test("shows inline add row when Add Holiday clicked", async () => {
    renderWithIntl(<CalendarManagement />);
    fireEvent.click(screen.getByTestId("add-holiday-button"));
    await waitFor(() => {
      expect(screen.getByTestId("holiday-inline-row")).toBeInTheDocument();
    });
  });

  test("disables save when form is empty", async () => {
    renderWithIntl(<CalendarManagement />);
    fireEvent.click(screen.getByTestId("add-holiday-button"));
    await waitFor(() => {
      expect(screen.getByTestId("save-holiday-button")).toBeDisabled();
    });
  });

  test("shows holiday count footer", async () => {
    renderWithIntl(<CalendarManagement />);
    await waitFor(() => {
      expect(screen.getByTestId("holiday-count-footer")).toHaveTextContent(
        "3 holidays configured for 2026",
      );
    });
  });

  test("renders year dropdown", async () => {
    renderWithIntl(<CalendarManagement />);
    expect(screen.getByTestId("year-dropdown")).toBeInTheDocument();
  });

  test("renders import and export buttons", async () => {
    renderWithIntl(<CalendarManagement />);
    expect(screen.getByTestId("import-csv-button")).toBeInTheDocument();
    expect(screen.getByTestId("export-csv-button")).toBeInTheDocument();
  });

  test("inactive holidays are visually dimmed (opacity)", async () => {
    renderWithIntl(<CalendarManagement />);
    await waitFor(() => {
      // Christmas Day (id: 3) is inactive
      const christmasRow = screen.getByText("Christmas Day").closest("tr");
      expect(christmasRow).toHaveStyle("opacity: 0.5");
    });
  });

  test("shows Annual/One-time tags for recurring status", async () => {
    renderWithIntl(<CalendarManagement />);
    await waitFor(() => {
      const annualTags = screen.getAllByText("Annual");
      expect(annualTags.length).toBeGreaterThan(0);
    });
  });

  test("shows Active/Inactive tags for status", async () => {
    renderWithIntl(<CalendarManagement />);
    await waitFor(() => {
      const activeTags = screen.getAllByText("Active");
      expect(activeTags.length).toBeGreaterThan(0);
      expect(screen.getByText("Inactive")).toBeInTheDocument();
    });
  });

  test("shows empty state when no holidays", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/calendar/holidays")) {
        callback({ year: 2026, holidays: [] });
      } else if (url.includes("/rest/calendar/weekends")) {
        callback({ weekendDays: [0, 6] });
      }
    });
    renderWithIntl(<CalendarManagement />);
    await waitFor(() => {
      expect(screen.getByText(/No holidays configured/)).toBeInTheDocument();
    });
  });

  test("shows loading skeleton while fetching", () => {
    getFromOpenElisServer.mockImplementation(() => {
      // Never call callback - stays loading
    });
    renderWithIntl(<CalendarManagement />);
    // DataTableSkeleton should render
    expect(document.querySelector(".cds--skeleton")).toBeTruthy();
  });

  test("export button constructs URL with config.serverBaseUrl prefix", async () => {
    const originalOpen = window.open;
    window.open = vi.fn();

    renderWithIntl(<CalendarManagement />);
    await waitFor(() => {
      expect(screen.getByText("New Year's Day")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("export-csv-button"));

    expect(window.open).toHaveBeenCalledTimes(1);
    const url = window.open.mock.calls[0][0];
    expect(url).toMatch(
      /^\/api\/OpenELIS-Global\/rest\/calendar\/holidays\/export/,
    );

    window.open = originalOpen;
  });
});
