import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import TATSummaryTab from "../TATSummaryTab";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

const mockSummaryData = {
  calculationMode: "CALENDAR",
  excludedDaysCount: 0,
  totalCount: 1247,
  mean: 3.7,
  median: 2.97,
  percentile90: 6.25,
  min: 0.37,
  max: 48.25,
  stdDeviation: 2.17,
  histogram: [
    { binLabel: "0-1h", binMin: 0, binMax: 1, count: 142 },
    { binLabel: "1-2h", binMin: 1, binMax: 2, count: 287 },
    { binLabel: "2-3h", binMin: 2, binMax: 3, count: 324 },
  ],
  breakdown: [
    {
      dimensionValue: "Hematology",
      count: 412,
      mean: 2.25,
      median: 1.8,
      percentile90: 4.5,
      max: 18.37,
    },
    {
      dimensionValue: "Chemistry",
      count: 356,
      mean: 3.7,
      median: 3.17,
      percentile90: 6.25,
      max: 36.08,
    },
  ],
};

const defaultFilters = {
  fromDate: "2026-01-01",
  toDate: "2026-01-31",
  segment: "RECEIPT_TO_VALIDATION",
  calculationMode: "CALENDAR",
};

describe("TATSummaryTab", () => {
  test("renders 7 stat cards with correct labels", () => {
    renderWithIntl(
      <TATSummaryTab
        data={mockSummaryData}
        loading={false}
        filters={defaultFilters}
      />,
    );
    expect(screen.getByText("Total Results")).toBeInTheDocument();
    expect(screen.getByText("Mean TAT")).toBeInTheDocument();
    expect(screen.getByText("Median TAT")).toBeInTheDocument();
    expect(screen.getByText("90th Percentile")).toBeInTheDocument();
    expect(screen.getByText("Min TAT")).toBeInTheDocument();
    expect(screen.getByText("Max TAT")).toBeInTheDocument();
    expect(screen.getByText("Std Deviation")).toBeInTheDocument();
  });

  test("renders stat card values formatted as hours and minutes", () => {
    renderWithIntl(
      <TATSummaryTab
        data={mockSummaryData}
        loading={false}
        filters={defaultFilters}
      />,
    );
    // 3.7 hours = "3h 42m"
    expect(screen.getByText("3h 42m")).toBeInTheDocument();
    // 2.97 hours = "2h 58m"
    expect(screen.getByText("2h 58m")).toBeInTheDocument();
    // Total count as number
    expect(screen.getByText("1,247")).toBeInTheDocument();
  });

  test("renders histogram when data is present", () => {
    renderWithIntl(
      <TATSummaryTab
        data={mockSummaryData}
        loading={false}
        filters={defaultFilters}
      />,
    );
    expect(screen.getByText("TAT Distribution")).toBeInTheDocument();
    expect(screen.getByText("0-1h")).toBeInTheDocument();
    expect(screen.getByText("1-2h")).toBeInTheDocument();
  });

  test("renders breakdown table with dimension rows", () => {
    renderWithIntl(
      <TATSummaryTab
        data={mockSummaryData}
        loading={false}
        filters={defaultFilters}
      />,
    );
    expect(screen.getByText("Hematology")).toBeInTheDocument();
    expect(screen.getByText("Chemistry")).toBeInTheDocument();
  });

  test("shows no results message when no filters applied", () => {
    renderWithIntl(
      <TATSummaryTab data={null} loading={false} filters={null} />,
    );
    expect(screen.getByText(/No results found/)).toBeInTheDocument();
  });

  test("shows loading skeleton when loading", () => {
    const { container } = renderWithIntl(
      <TATSummaryTab data={null} loading={true} filters={defaultFilters} />,
    );
    expect(container.querySelector(".cds--skeleton")).toBeTruthy();
  });

  test("shows Working Time info bar when mode is WORKING_TIME", () => {
    const workingTimeFilters = {
      ...defaultFilters,
      calculationMode: "WORKING_TIME",
    };
    const workingTimeData = {
      ...mockSummaryData,
      excludedDaysCount: 8,
    };
    renderWithIntl(
      <TATSummaryTab
        data={workingTimeData}
        loading={false}
        filters={workingTimeFilters}
      />,
    );
    expect(screen.getByText(/Working Time mode/)).toBeInTheDocument();
  });

  test("shows no holidays warning when Working Time with 0 excluded days", () => {
    const workingTimeFilters = {
      ...defaultFilters,
      calculationMode: "WORKING_TIME",
    };
    const noHolidayData = {
      ...mockSummaryData,
      excludedDaysCount: 0,
    };
    renderWithIntl(
      <TATSummaryTab
        data={noHolidayData}
        loading={false}
        filters={workingTimeFilters}
      />,
    );
    expect(
      screen.getByText(/No public holidays configured/),
    ).toBeInTheDocument();
  });

  test("shows Insufficient data when totalCount is 0", () => {
    const emptyData = {
      ...mockSummaryData,
      totalCount: 0,
      mean: null,
      median: null,
    };
    renderWithIntl(
      <TATSummaryTab
        data={emptyData}
        loading={false}
        filters={defaultFilters}
      />,
    );
    const insufficientTexts = screen.getAllByText("Insufficient data");
    expect(insufficientTexts.length).toBeGreaterThan(0);
  });

  test("breakdown table shows click hint", () => {
    renderWithIntl(
      <TATSummaryTab
        data={mockSummaryData}
        loading={false}
        filters={defaultFilters}
      />,
    );
    expect(
      screen.getByText("Click a row to view individual results"),
    ).toBeInTheDocument();
  });
});
