import TATTrendsTab from "../TATTrendsTab";
import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// Replaced inline utils require



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

const mockBuildQueryString = vi.fn(
  (filters, extra) => `fromDate=${filters.fromDate}${extra || ""}`,
);

const mockTrendData = {
  calculationMode: "CALENDAR",
  series: [
    {
      label: "All",
      dataPoints: [
        { period: "2026-03-01", mean: 3.5, median: 3.0, percentile90: 6.0, count: 10 },
        { period: "2026-03-02", mean: 4.0, median: 3.5, percentile90: 7.0, count: 12 },
        { period: "2026-03-03", mean: 2.5, median: 2.0, percentile90: 5.0, count: 8 },
      ],
    },
  ],
};

describe("TATTrendsTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(async () => {
    await new Promise((r) => setTimeout(r, 0));
  });

  test("shows no results when data is null", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(null);
    });

    renderWithIntl(
      <TATTrendsTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    expect(screen.getByText(/No results found/i)).toBeInTheDocument();
  });

  test("renders interval dropdown", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockTrendData);
    });

    const { container } = renderWithIntl(
      <TATTrendsTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      // Carbon Dropdown renders — check for the container element
      const dropdowns = container.querySelectorAll(".cds--dropdown");
      expect(dropdowns.length).toBeGreaterThanOrEqual(1);
    });
  });

  test("renders metric checkboxes", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockTrendData);
    });

    renderWithIntl(
      <TATTrendsTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/Median/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Mean/)).toBeInTheDocument();
    });
  });

  test("renders bars when trend data is provided", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockTrendData);
    });

    const { container } = renderWithIntl(
      <TATTrendsTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      // The component renders period labels
      expect(screen.getByText("03-01")).toBeInTheDocument();
      expect(screen.getByText("03-02")).toBeInTheDocument();
    });
  });

  test("compare-by dropdown renders", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockTrendData);
    });

    const { container } = renderWithIntl(
      <TATTrendsTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    await waitFor(() => {
      // At least 2 dropdowns: interval + compare-by
      const dropdowns = container.querySelectorAll(".cds--dropdown");
      expect(dropdowns.length).toBeGreaterThanOrEqual(2);
    });
  });

  test("fetches data with interval param", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockTrendData);
    });

    renderWithIntl(
      <TATTrendsTab filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/reports/tat/trend"),
      expect.any(Function),
    );
  });
});
