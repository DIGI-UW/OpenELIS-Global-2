import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const { getFromOpenElisServer } = require("../../../utils/Utils");

// Must import AFTER mock setup
const TATDetailListTab =
  require("../TATDetailListTab").default;

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

const mockFilters = {
  fromDate: "2026-03-01",
  toDate: "2026-03-31",
  segment: "RECEIPT_TO_VALIDATION",
  calculationMode: "CALENDAR",
};

const mockBuildQueryString = jest.fn(
  (filters, extra) => `fromDate=${filters.fromDate}&toDate=${filters.toDate}${extra || ""}`,
);

const mockData = {
  totalCount: 3,
  page: 0,
  pageSize: 25,
  calculationMode: "CALENDAR",
  results: [
    {
      labNumber: "LAB-001",
      testName: "CBC",
      labUnit: "Hematology",
      priority: "Routine",
      orderCreated: "2026-03-10T09:00:00",
      collected: "2026-03-10T09:30:00",
      received: "2026-03-10T10:00:00",
      testingStarted: "2026-03-10T11:00:00",
      resultEntered: "2026-03-10T13:30:00",
      validated: "2026-03-10T14:00:00",
      selectedSegmentTat: 4.0,
      overallTat: 5.0,
    },
    {
      labNumber: "LAB-002",
      testName: "Blood Culture",
      labUnit: "Microbiology",
      priority: "STAT",
      orderCreated: "2026-03-11T08:00:00",
      collected: null,
      received: "2026-03-11T09:00:00",
      testingStarted: null,
      resultEntered: "2026-03-11T15:00:00",
      validated: "2026-03-11T16:00:00",
      selectedSegmentTat: 7.0,
      overallTat: 8.0,
    },
    {
      labNumber: "LAB-003",
      testName: "Urinalysis",
      labUnit: "Chemistry",
      priority: "Routine",
      orderCreated: "2026-03-12T10:00:00",
      collected: "2026-03-12T10:30:00",
      received: "2026-03-12T11:00:00",
      testingStarted: "2026-03-12T12:00:00",
      resultEntered: "2026-03-12T13:00:00",
      validated: "2026-03-12T14:00:00",
      selectedSegmentTat: 3.0,
      overallTat: 4.0,
    },
  ],
};

describe("TATDetailListTab", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("shows no results when filters not applied", () => {
    renderWithIntl(
      <TATDetailListTab filters={null} buildQueryString={mockBuildQueryString} />,
    );
    expect(screen.getByText(/No results found/i)).toBeInTheDocument();
  });

  test("renders data table when data is provided", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockData);
    });

    renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      expect(screen.getByText("LAB-001")).toBeInTheDocument();
      expect(screen.getByText("LAB-002")).toBeInTheDocument();
      expect(screen.getByText("LAB-003")).toBeInTheDocument();
    });
  });

  test("formats TAT values as hours and minutes", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockData);
    });

    renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      // Multiple cells may show same TAT value (selectedTat + overallTat)
      expect(screen.getAllByText("4h").length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText("7h").length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText("3h").length).toBeGreaterThanOrEqual(1);
    });
  });

  test("renders missing timestamps as dash", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockData);
    });

    renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      // LAB-002 has null collected and testingStarted
      const dashes = screen.getAllByText("—");
      expect(dashes.length).toBeGreaterThan(0);
    });
  });

  test("lab number renders as link with target _blank", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockData);
    });

    const { container } = renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      const link = container.querySelector('a[href*="LAB-001"]');
      expect(link).toBeInTheDocument();
      expect(link.getAttribute("target")).toBe("_blank");
      expect(link.getAttribute("rel")).toBe("noopener noreferrer");
    });
  });

  test("STAT priority rows have red left border", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockData);
    });

    const { container } = renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      // Find the table row containing LAB-002 (STAT priority)
      const rows = container.querySelectorAll("tr");
      const statRow = Array.from(rows).find((r) =>
        r.textContent.includes("LAB-002"),
      );
      expect(statRow).toBeTruthy();
      expect(statRow.style.borderLeft).toBe("3px solid #da1e28");
    });
  });

  test("renders pagination controls", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockData);
    });

    renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      // Pagination component renders page size selector
      expect(screen.getByText(/1–3 of 3/i)).toBeInTheDocument();
    });
  });

  test("shows no results when data is empty", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback({ totalCount: 0, page: 0, pageSize: 25, results: [] });
    });

    renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      expect(screen.getByText(/No results found/i)).toBeInTheDocument();
    });
  });

  test("fetches data with correct query params", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockData);
    });

    renderWithIntl(
      <TATDetailListTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/reports/tat/detail"),
      expect.any(Function),
    );
    expect(mockBuildQueryString).toHaveBeenCalledWith(
      mockFilters,
      expect.stringContaining("page=0"),
    );
  });
});
