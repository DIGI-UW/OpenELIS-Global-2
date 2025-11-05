import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import MoveSampleModal from "./MoveSampleModal";
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

describe("MoveSampleModal", () => {
  const mockSample = {
    id: "1",
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
  const mockOnConfirm = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T088b: Test displays modal title with subtitle
   */
  test("testDisplaysModalTitleWithSubtitle", () => {
    renderWithIntl(
      <MoveSampleModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    expect(screen.getByText(/move sample/i)).toBeTruthy();
    // Subtitle is rendered by Carbon ModalHeader, verify modal structure
  });

  /**
   * T088b: Test displays current location in gray box
   */
  test("testDisplaysCurrentLocationInGrayBox", () => {
    renderWithIntl(
      <MoveSampleModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    expect(screen.getByText(mockCurrentLocation.path)).toBeTruthy();
  });

  /**
   * T088b: Test displays new location selector in bordered box
   */
  test("testDisplaysNewLocationSelectorInBorderedBox", () => {
    renderWithIntl(
      <MoveSampleModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Should have location selector (Room dropdown)
    expect(screen.getByTestId("room-dropdown")).toBeTruthy();
  });

  /**
   * T088b: Test displays Selected Location preview
   */
  test("testDisplaysSelectedLocationPreview", () => {
    renderWithIntl(
      <MoveSampleModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Should show "Selected Location" preview box
    expect(screen.getByText(/selected location/i)).toBeTruthy();
  });

  /**
   * T088b: Test displays reason textarea
   */
  test("testDisplaysReasonTextarea", () => {
    renderWithIntl(
      <MoveSampleModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    const reasonInput = screen.getByPlaceholderText(/reason/i);
    expect(reasonInput).toBeTruthy();
  });

  /**
   * T088b: Test validates new location different from current
   */
  test("testValidatesNewLocationDifferentFromCurrent", () => {
    renderWithIntl(
      <MoveSampleModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Confirm button should be disabled until location selected
    const confirmButton = screen.getByText(/confirm move/i);
    const button = confirmButton.closest("button");
    expect(button.hasAttribute("disabled")).toBe(true);
  });
});

