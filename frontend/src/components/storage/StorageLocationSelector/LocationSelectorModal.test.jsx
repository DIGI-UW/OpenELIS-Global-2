import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import LocationSelectorModal from "./LocationSelectorModal";
import messages from "../../../languages/en.json";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("LocationSelectorModal", () => {
  const mockSampleInfo = {
    sampleId: "S-2025-001",
    type: "Blood Serum",
    status: "Active",
  };

  const mockCurrentLocation = {
    path: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5",
    position: {
      id: "1",
      coordinate: "A5",
    },
  };

  const mockOnClose = jest.fn();
  const mockOnSave = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T061b: Test renders sample information section
   */
  test("testRendersSampleInfoSection", () => {
    renderWithIntl(
      <LocationSelectorModal
        open={true}
        sampleInfo={mockSampleInfo}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    expect(screen.getByText("S-2025-001")).toBeTruthy();
    expect(screen.getByText("Blood Serum")).toBeTruthy();
    expect(screen.getByText("Active")).toBeTruthy();
  });

  /**
   * T061b: Test renders current location section
   */
  test("testRendersCurrentLocationSection", () => {
    renderWithIntl(
      <LocationSelectorModal
        open={true}
        sampleInfo={mockSampleInfo}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    expect(screen.getByText(mockCurrentLocation.path)).toBeTruthy();
  });

  /**
   * T061b: Test renders full assignment form
   */
  test("testRendersFullAssignmentForm", () => {
    renderWithIntl(
      <LocationSelectorModal
        open={true}
        sampleInfo={mockSampleInfo}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Check for Room dropdown (required, marked with *)
    expect(screen.getByTestId("room-dropdown")).toBeTruthy();
    // Check for Position input
    const positionInput = screen.getByPlaceholderText(/a5|1-1|red-12/i);
    expect(positionInput).toBeTruthy();
  });

  /**
   * T061b: Test pre-populates with current location
   */
  test("testPrePopulatesWithCurrentLocation", () => {
    renderWithIntl(
      <LocationSelectorModal
        open={true}
        sampleInfo={mockSampleInfo}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Position should be pre-populated
    const positionInput = screen.getByPlaceholderText(/a5|1-1|red-12/i);
    expect(positionInput.value).toBe("A5");
  });
});
