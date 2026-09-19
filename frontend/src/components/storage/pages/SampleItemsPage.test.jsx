/**
 * SampleItemsPage tests — the Manage Location row action opens the shared
 * LocationPickerModal on this page instead of navigating to a route of
 * its own, and this page owns the assign/move decision and the refetch.
 */

import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, useLocation } from "react-router-dom";
import SampleItemsPage from "./SampleItemsPage";
import { NotificationContext } from "../../layout/Layout";
import * as Utils from "../../utils/Utils";
import messages from "../../../languages/en.json";

let capturedModalProps = null;
const mockAssignSampleItem = vi.fn();
const mockMoveSampleItem = vi.fn();

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

vi.mock("../LocationPicker/LocationPickerModal", () => ({
  default: (props) => {
    capturedModalProps = props;
    return props.isOpen ? <div data-testid="location-picker-modal" /> : null;
  },
}));

vi.mock("../hooks/useSampleStorage", () => ({
  default: () => ({
    assignSampleItem: mockAssignSampleItem,
    moveSampleItem: mockMoveSampleItem,
  }),
}));

const notifyCtx = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  addNotification: vi.fn(),
};

let currentLocation = null;

const LocationProbe = () => {
  currentLocation = useLocation();
  return null;
};

const unassignedItem = {
  sampleItemId: "123",
  sampleAccessionNumber: "ACC-123",
  type: "Whole Blood",
  status: "Active",
};

const assignedItem = {
  ...unassignedItem,
  location: "Main Lab > Freezer 1",
  positionCoordinate: "A1",
};

const renderPage = (item) => {
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => cb([item]));
  return render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notifyCtx}>
        <MemoryRouter initialEntries={["/Storage/sample-items"]}>
          <LocationProbe />
          <SampleItemsPage />
        </MemoryRouter>
      </NotificationContext.Provider>
    </IntlProvider>,
  );
};

const openManageLocation = () => {
  fireEvent.click(screen.getByTestId("sample-actions-overflow-menu"));
  fireEvent.click(screen.getByTestId("manage-location-menu-item"));
};

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.postToOpenElisServerJsonResponse.mockReset();
  notifyCtx.setNotificationVisible.mockReset();
  notifyCtx.addNotification.mockReset();
  mockAssignSampleItem.mockReset().mockResolvedValue({});
  mockMoveSampleItem.mockReset().mockResolvedValue({});
  capturedModalProps = null;
  currentLocation = null;
});

describe("SampleItemsPage — Manage Location", () => {
  it("opens the picker modal on the listing instead of navigating away", () => {
    renderPage(unassignedItem);
    expect(
      screen.queryByTestId("location-picker-modal"),
    ).not.toBeInTheDocument();

    openManageLocation();

    expect(screen.getByTestId("location-picker-modal")).toBeInTheDocument();
    expect(currentLocation.pathname).toBe("/Storage/sample-items");
    expect(capturedModalProps.occupantType).toBe("SAMPLE_ITEM");
    expect(capturedModalProps.occupant).toEqual({
      label: "ACC-123",
      type: "Whole Blood",
      status: "Active",
    });
  });

  it("passes the row's current location so the modal opens as a move", () => {
    renderPage(assignedItem);
    openManageLocation();

    expect(capturedModalProps.currentLocation).toEqual({
      selection: {},
      hierarchicalPath: "Main Lab > Freezer 1",
      position: { mode: "text", value: "A1" },
    });
  });

  it("moves with its reason when the row already has a location", async () => {
    renderPage(assignedItem);
    openManageLocation();

    await act(async () => {
      await capturedModalProps.onConfirm({
        selection: { device: { id: 7, name: "Freezer 2" } },
        position: { mode: "text", value: "B2" },
        reason: "Reassign",
        notes: "note",
      });
    });

    expect(mockMoveSampleItem).toHaveBeenCalledWith({
      sampleItemId: "123",
      locationId: "7",
      locationType: "device",
      positionCoordinate: "B2",
      notes: "note",
      reason: "Reassign",
    });
    expect(mockAssignSampleItem).not.toHaveBeenCalled();
  });

  it("assigns when the row has no location, then closes and refetches", async () => {
    renderPage(unassignedItem);
    const initialFetches = Utils.getFromOpenElisServer.mock.calls.length;
    openManageLocation();

    await act(async () => {
      await capturedModalProps.onConfirm({
        selection: { room: { id: 2, name: "Main Lab" } },
        position: null,
        reason: "",
        notes: "",
      });
    });

    expect(mockAssignSampleItem).toHaveBeenCalledWith({
      sampleItemId: "123",
      locationId: "2",
      locationType: "room",
      positionCoordinate: null,
      notes: null,
    });
    expect(mockMoveSampleItem).not.toHaveBeenCalled();
    expect(
      screen.queryByTestId("location-picker-modal"),
    ).not.toBeInTheDocument();
    expect(currentLocation.search).toMatch(/^\?t=\d+$/);
    expect(Utils.getFromOpenElisServer.mock.calls.length).toBeGreaterThan(
      initialFetches,
    );
  });

  it("reports an error and saves nothing when no location is selected", async () => {
    renderPage(unassignedItem);
    openManageLocation();

    await act(async () => {
      await capturedModalProps.onConfirm({
        selection: {},
        position: null,
        reason: "",
        notes: "",
      });
    });

    expect(notifyCtx.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message: "Select a device, shelf, rack, or box before saving",
      }),
    );
    expect(mockAssignSampleItem).not.toHaveBeenCalled();
    expect(mockMoveSampleItem).not.toHaveBeenCalled();
    expect(screen.getByTestId("location-picker-modal")).toBeInTheDocument();
  });

  it("reports the server's error and keeps the modal open when a save fails", async () => {
    mockAssignSampleItem.mockRejectedValue(new Error("Location is full"));
    renderPage(unassignedItem);
    openManageLocation();

    await act(async () => {
      await capturedModalProps.onConfirm({
        selection: { room: { id: 2, name: "Main Lab" } },
        position: null,
        reason: "",
        notes: "",
      });
    });

    expect(notifyCtx.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Location is full" }),
    );
    expect(screen.getByTestId("location-picker-modal")).toBeInTheDocument();
  });
});
