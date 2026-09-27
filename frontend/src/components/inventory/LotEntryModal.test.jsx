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
      <>
        <button
          onClick={() =>
            onConfirm({
              selection: { room: { id: 9, name: "Cold Room" } },
              position: null,
              reason: "Consolidating stock",
              notes: "",
            })
          }
        >
          mock-confirm-location
        </button>
        <button
          onClick={() =>
            onConfirm({ selection: {}, position: null, notes: "" })
          }
        >
          mock-confirm-nothing
        </button>
      </>
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
        expect.objectContaining({
          inventoryLotId: "11",
          locationId: "9",
          reason: "Consolidating stock",
        }),
      );
    });
    expect(InventoryLotStorageAPI.assignLocation).not.toHaveBeenCalled();
  });
});

describe("LotEntryModal — auto-generated lot number", () => {
  it("allows creating a lot with a blank lot number, sending null so the server generates one", async () => {
    InventoryManagementAPI.receive.mockResolvedValue({ id: 60 });
    InventoryLotStorageAPI.assignLocation.mockResolvedValue({
      assignmentId: "1",
    });

    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={null} />,
    );

    fireEvent.click(
      screen.getByRole("combobox", { name: /select catalog item/i }),
    );
    fireEvent.click(await screen.findByText("Malaria RDT (RDT)"));
    fireEvent.change(screen.getByLabelText(/initial quantity/i), {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryManagementAPI.receive).toHaveBeenCalledWith(
        expect.objectContaining({ lotNumber: null }),
      );
    });
  });

  it("still requires a lot number when editing an existing lot", async () => {
    const lot = {
      id: 12,
      inventoryItem: { id: "MALARIA_RDT" },
      lotNumber: "LOT-12",
      currentQuantity: 4,
      status: "ACTIVE",
      qcStatus: "PENDING",
    };
    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={lot} />,
    );

    fireEvent.change(screen.getByLabelText(/lot number/i), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText(/lot number is required/i),
    ).toBeInTheDocument();
    expect(InventoryLotAPI.update).not.toHaveBeenCalled();
  });
});

describe("LotEntryModal — partial save recovery", () => {
  it("does not receive the lot a second time when the first save's location assignment failed", async () => {
    InventoryManagementAPI.receive.mockResolvedValue({ id: 77 });
    InventoryLotStorageAPI.assignLocation
      .mockRejectedValueOnce(new Error("Position A1 is already occupied"))
      .mockResolvedValueOnce({ assignmentId: "1" });
    const onSave = vi.fn();

    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={onSave} lot={null} />,
    );
    await fillRequiredFieldsExceptLocation();

    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));
    fireEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText(/the lot was created, but assigning/i),
    ).toBeInTheDocument();
    expect(InventoryManagementAPI.receive).toHaveBeenCalledTimes(1);
    expect(onSave).not.toHaveBeenCalled();

    // The retry sends only the assignment, so the lot fields must not invite
    // edits that would be dropped.
    expect(screen.getByLabelText(/lot number/i)).toBeDisabled();
    expect(screen.getByLabelText(/initial quantity/i)).toBeDisabled();

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => expect(onSave).toHaveBeenCalled());
    expect(InventoryManagementAPI.receive).toHaveBeenCalledTimes(1);
    expect(InventoryLotStorageAPI.assignLocation).toHaveBeenCalledTimes(2);
  });

  it("shows the barcode the server minted rather than a blank locked field", async () => {
    InventoryManagementAPI.receive.mockResolvedValue({
      id: 79,
      barcode: "MALARIA-RDT-LOT-1",
    });
    InventoryLotStorageAPI.assignLocation.mockRejectedValue(
      new Error("Position A1 is already occupied"),
    );

    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={null} />,
    );
    await fillRequiredFieldsExceptLocation();

    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));
    fireEvent.click(screen.getByText("Save"));
    await screen.findByText(/the lot was created, but assigning/i);

    const barcode = document.getElementById("barcode");
    expect(barcode).toBeDisabled();
    expect(barcode).toHaveValue("MALARIA-RDT-LOT-1");
  });

  it("refreshes the caller's list on close when a lot was already committed", async () => {
    InventoryManagementAPI.receive.mockResolvedValue({ id: 78 });
    InventoryLotStorageAPI.assignLocation.mockRejectedValue(
      new Error("Position A1 is already occupied"),
    );
    const onSave = vi.fn();
    const onClose = vi.fn();

    renderWithIntl(
      <LotEntryModal open onClose={onClose} onSave={onSave} lot={null} />,
    );
    await fillRequiredFieldsExceptLocation();

    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));
    fireEvent.click(screen.getByText("Save"));
    await screen.findByText(/the lot was created, but assigning/i);

    fireEvent.click(screen.getByText("Cancel"));

    expect(onSave).toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });

  it("rejects a picker confirmation with no assignable location instead of posting a null locationId", async () => {
    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={null} />,
    );
    await fillRequiredFieldsExceptLocation();

    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-nothing"));

    expect(
      await screen.findByText(
        /select a device, shelf, rack, or box before saving/i,
      ),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText(/please assign a storage location/i),
    ).toBeInTheDocument();
    expect(InventoryManagementAPI.receive).not.toHaveBeenCalled();
    expect(InventoryLotStorageAPI.assignLocation).not.toHaveBeenCalled();
  });
});

describe("LotEntryModal — translated server refusals", () => {
  it("renders the duplicate-barcode refusal from errorCode and params, not the raw message", async () => {
    // The shape InventoryService.post builds from a {message, errorCode, params}
    // body; the message is the backend's own wording, not the en.json one.
    const err = new Error("Barcode ABC is already assigned to lot LOT-9");
    err.errorCode = "inventory.lot.error.duplicateBarcode";
    err.params = { barcode: "ABC", lotNumber: "LOT-9" };
    InventoryManagementAPI.receive.mockRejectedValue(err);

    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={null} />,
    );
    await fillRequiredFieldsExceptLocation();
    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));
    fireEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText('Barcode "ABC" is already assigned to lot LOT-9'),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Barcode ABC is already assigned to lot LOT-9"),
    ).not.toBeInTheDocument();
  });
});

describe("LotEntryModal — system-generated barcode", () => {
  it("sends null when the barcode is left blank so the server generates one", async () => {
    InventoryManagementAPI.receive.mockResolvedValue({ id: 77 });
    InventoryLotStorageAPI.assignLocation.mockResolvedValue({
      assignmentId: "1",
    });
    renderWithIntl(<LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} />);

    await fillRequiredFieldsExceptLocation();
    fireEvent.click(screen.getByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));
    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryManagementAPI.receive).toHaveBeenCalledWith(
        expect.objectContaining({ barcode: null }),
      );
    });
  });

  it("offers auto-generation when creating", () => {
    renderWithIntl(<LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} />);

    const barcode = screen.getByLabelText(/barcode/i);
    expect(barcode).toBeEnabled();
    expect(
      screen.getByText(/leave blank to generate one/i),
    ).toBeInTheDocument();
  });

  it("locks the barcode once the lot is saved", () => {
    const lot = {
      id: 12,
      inventoryItem: { id: "MALARIA_RDT" },
      lotNumber: "LOT-12",
      barcode: "TEST-REAGENT-A-LOT-12",
      currentQuantity: 4,
      status: "ACTIVE",
      qcStatus: "PENDING",
    };
    renderWithIntl(
      <LotEntryModal open onClose={vi.fn()} onSave={vi.fn()} lot={lot} />,
    );

    const barcode = screen.getByLabelText(/barcode/i);
    expect(barcode).toHaveValue("TEST-REAGENT-A-LOT-12");
    expect(barcode).toBeDisabled();
    expect(
      screen.getByText(/barcode is locked once saved/i),
    ).toBeInTheDocument();
  });
});

describe("LotEntryModal — barcode on a lot that has none", () => {
  const barcodelessLot = {
    id: 13,
    inventoryItem: { id: "MALARIA_RDT" },
    lotNumber: "LOT-13",
    barcode: null,
    currentQuantity: 4,
    status: "ACTIVE",
    qcStatus: "PENDING",
  };

  it("lets an operator give an existing lot its first barcode", () => {
    renderWithIntl(
      <LotEntryModal
        open
        onClose={vi.fn()}
        onSave={vi.fn()}
        lot={barcodelessLot}
      />,
    );

    const barcode = screen.getByLabelText(/barcode/i);
    expect(barcode).toBeEnabled();
    expect(screen.getByText(/has no barcode yet/i)).toBeInTheDocument();
    expect(
      screen.queryByText(/barcode is locked once saved/i),
    ).not.toBeInTheDocument();
  });

  it("sends the barcode typed on a barcodeless lot to the server", async () => {
    InventoryLotAPI.update.mockResolvedValue({});
    renderWithIntl(
      <LotEntryModal
        open
        onClose={vi.fn()}
        onSave={vi.fn()}
        lot={barcodelessLot}
      />,
    );

    fireEvent.change(screen.getByLabelText(/barcode/i), {
      target: { value: "BC-GIVEN-13" },
    });
    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryLotAPI.update).toHaveBeenCalledWith(
        13,
        expect.objectContaining({ barcode: "BC-GIVEN-13" }),
      );
    });
  });
});
