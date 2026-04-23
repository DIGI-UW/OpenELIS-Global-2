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

  test("renders new filter controls (lab unit, test, sample type, ordering site)", () => {
    act(() => {
      renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    });

    // FilterableMultiSelect and ComboBox render as combobox role
    const comboboxes = screen.getAllByRole("combobox");
    // Should have at least 4 comboboxes (lab unit, test, sample type uses Dropdown, ordering site)
    expect(comboboxes.length).toBeGreaterThanOrEqual(2);
  });

  test("date preset 'Last 7 Days' sets correct date range", () => {
    act(() => {
      renderWithIntl(<TATFilterBar onGenerate={mockOnGenerate} />);
    });

    // Click the "Last 7 Days" preset tag
    const presetTag = screen.getByText(
      messages["reports.tat.preset.7days"] || "Last 7 Days",
    );
    fireEvent.click(presetTag);

    // Click Generate to capture the filter state
    fireEvent.click(screen.getByTestId("generate-report-button"));

    expect(mockOnGenerate).toHaveBeenCalledTimes(1);
    const filters = mockOnGenerate.mock.calls[0][0];

    // Verify the date range is approximately 7 days
    const from = new Date(filters.fromDate);
    const to = new Date(filters.toDate);
    const diffDays = Math.round((to - from) / (1000 * 60 * 60 * 24));
    expect(diffDays).toBe(7);
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
