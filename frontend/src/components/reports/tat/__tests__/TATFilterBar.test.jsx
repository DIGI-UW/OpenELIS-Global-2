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

  test("onGenerate callback includes priority when selected", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);

    // Open the priority dropdown — Carbon Dropdown uses Downshift
    const priorityWrapper = document.getElementById("tat-priority");
    const trigger = priorityWrapper.querySelector(
      "button.cds--list-box__field",
    );
    fireEvent.click(trigger);

    // Select the STAT option (index 2: All=0, Routine=1, STAT=2, ASAP=3)
    // Carbon renders items as role="option" but without visible text in JSDOM
    // because itemToString defaults to String(item) for object items
    const options = priorityWrapper.querySelectorAll('[role="option"]');
    expect(options.length).toBe(4);
    fireEvent.click(options[2]); // STAT

    // Click Generate and verify the priority is passed through
    fireEvent.click(screen.getByTestId("generate-report-button"));

    expect(mockOnGenerate).toHaveBeenCalledTimes(1);
    const filters = mockOnGenerate.mock.calls[0][0];
    expect(filters).toHaveProperty("priority", "STAT");
  });
});
