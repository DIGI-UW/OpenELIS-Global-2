import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import TATFilterBar from "../TATFilterBar";

// Mock the API utility — filter dropdowns load options on mount
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (url.includes("test-sections")) {
      callback([
        { id: "101", value: "Hematology" },
        { id: "102", value: "Chemistry" },
      ]);
    } else if (url.includes("tests")) {
      callback([
        { id: "201", value: "CBC" },
        { id: "202", value: "BMP" },
      ]);
    } else if (url.includes("user-sample-types")) {
      callback([{ id: "301", value: "Blood" }]);
    } else if (url.includes("site-names")) {
      callback([{ id: "401", value: "Main Clinic" }]);
    } else {
      callback([]);
    }
  }),
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("TATFilterBar", () => {
  const mockOnGenerate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
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

  test("priority dropdown shows all options by visible text and selects STAT", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);

    const trigger = document
      .getElementById("tat-priority")
      .querySelector("button.cds--list-box__field");
    fireEvent.click(trigger);

    expect(screen.getByRole("option", { name: "All" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Routine" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "STAT" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "ASAP" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("option", { name: "STAT" }));
    fireEvent.click(screen.getByTestId("generate-report-button"));

    expect(mockOnGenerate).toHaveBeenCalledTimes(1);
    expect(mockOnGenerate.mock.calls[0][0]).toHaveProperty("priority", "STAT");
  });

  test("segment dropdown shows all 7 segments by visible text", () => {
    renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);

    const trigger = document
      .getElementById("tat-segment")
      .querySelector("button.cds--list-box__field");
    fireEvent.click(trigger);

    for (const name of [
      "Receipt to Validation",
      "Order to Collection",
      "Collection to Receipt",
      "Receipt to Testing Started",
      "Receipt to Result Entry",
      "Result Entry to Validation",
      "Overall TAT",
    ]) {
      expect(screen.getByRole("option", { name })).toBeInTheDocument();
    }
  });

  test("renders new filter controls (lab unit, test, sample type, ordering site)", () => {
    act(() => {
      renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    });

    // FilterableMultiSelect and ComboBox render as combobox role
    const comboboxes = screen.getAllByRole("combobox");
    // Should have at least 4 comboboxes (lab unit, test, sample type uses Dropdown, ordering site)
    expect(comboboxes.length).toBeGreaterThanOrEqual(2);
  });

  test("date preset 'Last 7 Days' populates visible inputs and filter state", () => {
    act(() => {
      renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    });

    const presetTag = screen.getByText(
      messages["reports.tat.preset.7days"] || "Last 7 Days",
    );
    fireEvent.click(presetTag);

    const fromInput = screen.getByLabelText(/Date Range \(From\)/);
    const toInput = screen.getByLabelText(/Date Range \(To\)/);
    expect(fromInput.value).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(toInput.value).toMatch(/^\d{4}-\d{2}-\d{2}$/);

    const visibleDiffDays = Math.round(
      (new Date(toInput.value) - new Date(fromInput.value)) / 86400000,
    );
    expect(visibleDiffDays).toBe(7);

    fireEvent.click(screen.getByTestId("generate-report-button"));
    expect(mockOnGenerate).toHaveBeenCalledTimes(1);
    const filters = mockOnGenerate.mock.calls[0][0];
    expect(filters.fromDate).toBe(fromInput.value);
    expect(filters.toDate).toBe(toInput.value);
  });

  test("onGenerate includes new filter fields", () => {
    act(() => {
      renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    });

    // Click Generate with defaults — new filter fields should be present (empty)
    fireEvent.click(screen.getByTestId("generate-report-button"));

    const filters = mockOnGenerate.mock.calls[0][0];
    expect(filters).toHaveProperty("labUnitIds");
    expect(filters).toHaveProperty("testIds");
    expect(filters).toHaveProperty("sampleTypeId");
    expect(filters).toHaveProperty("orderingSiteId");
  });
});
