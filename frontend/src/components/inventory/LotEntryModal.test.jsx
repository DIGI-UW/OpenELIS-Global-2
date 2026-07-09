import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LotEntryModal from "./LotEntryModal";
import {
  InventoryItemAPI,
  InventoryLotAPI,
  InventoryManagementAPI,
  InventoryLotStorageAPI,
} from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryItemAPI: { getAll: vi.fn() },
  InventoryLotAPI: { update: vi.fn() },
  InventoryManagementAPI: { receive: vi.fn() },
  InventoryLotStorageAPI: {
    getLocation: vi.fn(),
    assignLocation: vi.fn(),
    moveLocation: vi.fn(),
  },
}));

// Stand-in for the real picker: fires onConfirm with a canned selection so
// we can assert on LotEntryModal's own wiring (deferred vs. immediate
// assignment) without exercising the full Carbon picker UI.
vi.mock("../storage/LocationPicker/LocationPickerModal", () => ({
  default: ({ isOpen, onConfirm }) =>
    isOpen ? (
      <button
        onClick={() =>
          onConfirm({
            selection: { room: { id: 9, name: "Cold Room" } },
            position: null,
            notes: "",
          })
        }
      >
        mock-confirm-location
      </button>
    ) : null,
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

// Fills every required field except storage location, so tests can isolate
// the location-assignment behavior under test.
const fillRequiredFieldsExceptLocation = async () => {
  fireEvent.click(
    screen.getByRole("combobox", { name: /select catalog item/i }),
  );
  fireEvent.click(await screen.findByText("Malaria RDT (RDT)"));
  fireEvent.change(screen.getByLabelText(/lot number/i), {
    target: { value: "LOT-1" },
  });
  fireEvent.change(screen.getByLabelText(/initial quantity/i), {
    target: { value: "5" },
  });
};

beforeEach(() => {
  vi.clearAllMocks();
  InventoryItemAPI.getAll.mockResolvedValue([
    { id: "MALARIA_RDT", name: "Malaria RDT", itemType: "RDT" },
  ]);
  InventoryLotStorageAPI.getLocation.mockResolvedValue({});
});

describe("LotEntryModal — storage location wiring (OGC-657)", () => {
  it("blocks save when creating a lot without a storage location", async () => {
    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={null} />,
    );
    await fillRequiredFieldsExceptLocation();

    fireEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText(/please assign a storage location/i),
    ).toBeInTheDocument();
    expect(InventoryManagementAPI.receive).not.toHaveBeenCalled();
  });

  it("defers assignment until the new lot is saved, then assigns by the returned id", async () => {
    InventoryManagementAPI.receive.mockResolvedValue({ id: 55 });
    InventoryLotStorageAPI.assignLocation.mockResolvedValue({
      assignmentId: "1",
    });
    const onSave = vi.fn();

    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={onSave} lot={null} />,
    );
    await fillRequiredFieldsExceptLocation();

    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));

    // Not called yet — the lot doesn't exist until Save.
    expect(InventoryLotStorageAPI.assignLocation).not.toHaveBeenCalled();
    expect(await screen.findByText("Cold Room")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryLotStorageAPI.assignLocation).toHaveBeenCalledWith(
        expect.objectContaining({
          inventoryLotId: "55",
          locationId: "9",
          locationType: "room",
        }),
      );
    });
    expect(onSave).toHaveBeenCalled();
  });

  it("assigns immediately (not deferred) when editing an existing unassigned lot", async () => {
    const lot = {
      id: 10,
      inventoryItem: { id: "MALARIA_RDT" },
      lotNumber: "LOT-10",
      currentQuantity: 4,
      status: "ACTIVE",
      qcStatus: "PENDING",
    };
    InventoryLotStorageAPI.assignLocation.mockResolvedValue({
      assignmentId: "1",
    });

    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={lot} />,
    );

    await waitFor(() =>
      expect(InventoryLotStorageAPI.getLocation).toHaveBeenCalledWith(10),
    );

    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));

    await waitFor(() => {
      expect(InventoryLotStorageAPI.assignLocation).toHaveBeenCalledWith(
        expect.objectContaining({ inventoryLotId: "10", locationId: "9" }),
      );
    });
  });

  it("moves (not assigns) when the lot already has a location", async () => {
    const lot = {
      id: 11,
      inventoryItem: { id: "MALARIA_RDT" },
      lotNumber: "LOT-11",
      currentQuantity: 4,
      status: "ACTIVE",
      qcStatus: "PENDING",
    };
    InventoryLotStorageAPI.getLocation.mockResolvedValue({
      hierarchicalPath: "Main Lab > Freezer 1",
    });
    InventoryLotStorageAPI.moveLocation.mockResolvedValue({
      movementId: "1",
    });

    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={lot} />,
    );

    expect(await screen.findByText("Main Lab > Freezer 1")).toBeInTheDocument();

    fireEvent.click(screen.getByText(/move storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));

    await waitFor(() => {
      expect(InventoryLotStorageAPI.moveLocation).toHaveBeenCalledWith(
        expect.objectContaining({ inventoryLotId: "11", locationId: "9" }),
      );
    });
    expect(InventoryLotStorageAPI.assignLocation).not.toHaveBeenCalled();
  });
});
