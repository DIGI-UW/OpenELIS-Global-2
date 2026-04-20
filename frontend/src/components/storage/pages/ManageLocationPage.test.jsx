import React from "react";
import { render, screen, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ManageLocationPage from "./ManageLocationPage";

let mockHistoryPush = vi.fn();
let mockLocationState = {};
let capturedPickerProps = null;
let mockAssignSampleItem = vi.fn();
let mockMoveSampleItem = vi.fn();

vi.mock("react-router-dom", () => ({
  useParams: () => ({ id: "42" }),
  useHistory: () => ({ push: mockHistoryPush }),
  useLocation: () => ({ state: mockLocationState }),
}));

vi.mock("../LocationPicker/LocationPickerPage", () => ({
  default: (props) => {
    capturedPickerProps = props;
    return <div data-testid="location-picker-page-mock" />;
  },
}));

vi.mock("../hooks/useSampleStorage", () => ({
  default: () => ({
    assignSampleItem: mockAssignSampleItem,
    moveSampleItem: mockMoveSampleItem,
  }),
}));

vi.mock("../components/BreadcrumbNav", () => ({
  default: () => null,
}));

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={{}}>
      <ManageLocationPage />
    </IntlProvider>,
  );

beforeEach(() => {
  mockHistoryPush = vi.fn();
  mockAssignSampleItem = vi.fn().mockResolvedValue({});
  mockMoveSampleItem = vi.fn().mockResolvedValue({});
  capturedPickerProps = null;
});

describe("ManageLocationPage", () => {
  test("uses move endpoint when sample already has a location path", async () => {
    mockLocationState = {
      sample: {
        sampleItemId: "123",
        sampleAccessionNumber: "ACC-123",
        location: "Main Lab > Freezer 1",
      },
    };

    renderPage();

    expect(capturedPickerProps.currentLocation).not.toBeNull();

    await act(async () => {
      await capturedPickerProps.onSave({
        selection: { device: { id: 7, name: "Freezer 2" } },
        position: { mode: "text", value: "A1" },
        reason: "Reassign",
        notes: "note",
      });
    });

    expect(mockMoveSampleItem).toHaveBeenCalledWith({
      sampleItemId: "123",
      locationId: "7",
      locationType: "device",
      positionCoordinate: "A1",
      notes: "note",
      reason: "Reassign",
    });
    expect(mockAssignSampleItem).not.toHaveBeenCalled();
  });

  test("shows validation error for room-only selection", async () => {
    mockLocationState = {
      sample: {
        sampleItemId: "123",
        sampleAccessionNumber: "ACC-123",
      },
    };

    renderPage();

    await act(async () => {
      await capturedPickerProps.onSave({
        selection: { room: { id: 2, name: "Main Lab" } },
        position: null,
        reason: "",
        notes: "",
      });
    });

    expect(
      screen.getByText("Select a device, shelf, rack, or box before saving"),
    ).toBeInTheDocument();
    expect(mockMoveSampleItem).not.toHaveBeenCalled();
    expect(mockAssignSampleItem).not.toHaveBeenCalled();
  });
});
