import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import ViewStorageModal from "./ViewStorageModal";
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

describe("ViewStorageModal", () => {
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
  const mockOnSave = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T088d: Test displays modal title
   */
  test("testDisplaysModalTitle", () => {
    renderWithIntl(
      <ViewStorageModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    expect(screen.getByText(/storage location assignment/i)).toBeTruthy();
  });

  /**
   * T088d: Test displays sample info section
   */
  test("testDisplaysSampleInfoSection", () => {
    renderWithIntl(
      <ViewStorageModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    expect(screen.getByText(mockSample.sampleId)).toBeTruthy();
    expect(screen.getByText(mockSample.type)).toBeTruthy();
    expect(screen.getByText(mockSample.status)).toBeTruthy();
  });

  /**
   * T088d: Test displays current location section
   */
  test("testDisplaysCurrentLocationSection", () => {
    renderWithIntl(
      <ViewStorageModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    expect(screen.getByText(mockCurrentLocation.path)).toBeTruthy();
  });

  /**
   * T088d: Test displays full assignment form
   */
  test("testDisplaysFullAssignmentForm", () => {
    renderWithIntl(
      <ViewStorageModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Should have Room dropdown
    expect(screen.getByTestId("room-dropdown")).toBeTruthy();
    // Should have Position input
    const positionInput = screen.getByPlaceholderText(/a5|1-1|red-12/i);
    expect(positionInput).toBeTruthy();
  });

  /**
   * T088d: Test pre-populates with current location
   */
  test("testPrePopulatesWithCurrentLocation", () => {
    renderWithIntl(
      <ViewStorageModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Position should be pre-populated
    const positionInput = screen.getByPlaceholderText(/a5|1-1|red-12/i);
    expect(positionInput.value).toBe("A5");
  });

  /**
   * T088d: Test allows editing location assignment
   */
  test("testAllowsEditingLocationAssignment", () => {
    renderWithIntl(
      <ViewStorageModal
        open={true}
        sample={mockSample}
        currentLocation={mockCurrentLocation}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Form fields should be editable
    const positionInput = screen.getByPlaceholderText(/a5|1-1|red-12/i);
    expect(positionInput.hasAttribute("readonly")).toBe(false);
    expect(positionInput.hasAttribute("disabled")).toBe(false);
  });
});

