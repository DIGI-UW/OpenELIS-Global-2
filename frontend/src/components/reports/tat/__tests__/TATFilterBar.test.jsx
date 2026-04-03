import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import TATFilterBar from "../TATFilterBar";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("TATFilterBar", () => {
  const mockOnGenerate = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders all filter controls", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);

    // Date pickers
    expect(screen.getByLabelText(/Date Range \(From\)/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Date Range \(To\)/)).toBeInTheDocument();

    // Generate button
    expect(
      screen.getByTestId("generate-report-button"),
    ).toBeInTheDocument();

    // Clear Filters button
    expect(screen.getByText("Clear Filters")).toBeInTheDocument();

    // Include cancelled checkbox
    expect(
      screen.getByLabelText(/Include cancelled/),
    ).toBeInTheDocument();
  });

  test("renders Generate Report button", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    const btn = screen.getByTestId("generate-report-button");
    expect(btn).toBeInTheDocument();
    expect(btn).toHaveTextContent("Generate Report");
  });

  test("calls onGenerate with filter state when Generate clicked", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    fireEvent.click(screen.getByTestId("generate-report-button"));

    expect(mockOnGenerate).toHaveBeenCalledTimes(1);
    const filters = mockOnGenerate.mock.calls[0][0];
    expect(filters).toHaveProperty("fromDate");
    expect(filters).toHaveProperty("toDate");
    expect(filters).toHaveProperty("segment", "RECEIPT_TO_VALIDATION");
    expect(filters).toHaveProperty("calculationMode", "CALENDAR");
    expect(filters).toHaveProperty("includeCancelled", false);
  });

  test("defaults segment to Receipt to Validation", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    fireEvent.click(screen.getByTestId("generate-report-button"));

    const filters = mockOnGenerate.mock.calls[0][0];
    expect(filters.segment).toBe("RECEIPT_TO_VALIDATION");
  });

  test("defaults calculation mode to CALENDAR", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    fireEvent.click(screen.getByTestId("generate-report-button"));

    const filters = mockOnGenerate.mock.calls[0][0];
    expect(filters.calculationMode).toBe("CALENDAR");
  });

  test("renders ContentSwitcher for Calendar/Working Time", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    expect(screen.getByText("Calendar Time")).toBeInTheDocument();
    expect(screen.getByText("Working Time")).toBeInTheDocument();
  });

  test("renders include-cancelled checkbox unchecked by default", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    const checkbox = screen.getByLabelText(/Include cancelled/);
    expect(checkbox).not.toBeChecked();
  });
});
