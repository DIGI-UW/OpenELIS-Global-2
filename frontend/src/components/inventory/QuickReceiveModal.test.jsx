import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import QuickReceiveModal from "./QuickReceiveModal";
import { InventoryManagementAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryManagementAPI: { receive: vi.fn() },
  InventoryLotStorageAPI: { assignLocation: vi.fn() },
}));

vi.mock("../storage/LocationPicker/LocationPickerModal", () => ({
  default: ({ isOpen }) =>
    isOpen ? <div data-testid="location-picker" /> : null,
}));

const TRACKED = {
  itemId: 1,
  name: "GeneXpert MTB/RIF cartridge",
  code: "GENEXPERT",
  units: "tests",
  onHand: 12,
  trackLots: true,
  upc: "08901234567890",
};

const UNTRACKED = {
  itemId: 2,
  name: "Examination gloves (box)",
  code: "GLOVES",
  units: "boxes",
  onHand: 14,
  trackLots: false,
  upc: null,
};

const renderModal = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <QuickReceiveModal
        open
        items={[TRACKED, UNTRACKED]}
        onClose={vi.fn()}
        onSave={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );

const scanField = () => screen.getByLabelText(/scan a product barcode/i);

const scan = (code) => {
  fireEvent.change(scanField(), { target: { value: code } });
  fireEvent.keyDown(scanField(), { key: "Enter", code: "Enter" });
};

beforeEach(() => {
  vi.clearAllMocks();
  InventoryManagementAPI.receive.mockResolvedValue({ id: 99 });
});

describe("QuickReceiveModal", () => {
  it("asks only for a quantity when the item is not tracked by lot", () => {
    renderModal({ initialItemId: UNTRACKED.itemId });

    expect(screen.getByLabelText(/quantity received/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/lot number/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/expiration date/i)).not.toBeInTheDocument();
  });

  it("asks for a lot number and expiry when the item is tracked by lot", () => {
    renderModal({ initialItemId: TRACKED.itemId });

    expect(screen.getByLabelText(/lot number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/expiration date/i)).toBeInTheDocument();
  });

  it("receives a quantity against the chosen item", async () => {
    const onSave = vi.fn();
    renderModal({ initialItemId: UNTRACKED.itemId, onSave });

    fireEvent.change(screen.getByLabelText(/quantity received/i), {
      target: { value: "6" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Receive" }));

    await waitFor(() => expect(onSave).toHaveBeenCalled());
    // The caller is told how much arrived so it can explain that a received
    // lot does not count towards on-hand until its QC passes.
    expect(onSave).toHaveBeenCalledWith({ quantity: 6, units: "boxes" });
    const payload = InventoryManagementAPI.receive.mock.calls[0][0];
    expect(payload.inventoryItem).toEqual({ id: UNTRACKED.itemId });
    expect(payload.currentQuantity).toBe(6);
    expect(payload.initialQuantity).toBe(6);
  });

  /**
   * The receive endpoint routes a body carrying an id into an overwrite of that
   * lot rather than a new receipt, while still logging the full quantity as
   * arriving. Nothing here may send one.
   */
  it("never sends a lot id, which would overwrite a lot instead of adding stock", async () => {
    renderModal({ initialItemId: UNTRACKED.itemId });

    fireEvent.click(screen.getByRole("button", { name: "Receive" }));

    await waitFor(() =>
      expect(InventoryManagementAPI.receive).toHaveBeenCalled(),
    );
    expect(InventoryManagementAPI.receive.mock.calls[0][0].id).toBeUndefined();
  });

  /** The server stamps its own clock over it, so offering the field would lie. */
  it("never sends a receipt date", async () => {
    renderModal({ initialItemId: UNTRACKED.itemId });

    fireEvent.click(screen.getByRole("button", { name: "Receive" }));

    await waitFor(() =>
      expect(InventoryManagementAPI.receive).toHaveBeenCalled(),
    );
    expect(
      InventoryManagementAPI.receive.mock.calls[0][0].receiptDate,
    ).toBeUndefined();
    expect(screen.queryByLabelText(/receipt date/i)).not.toBeInTheDocument();
  });

  it("sends a blank lot number as null so the server mints one", async () => {
    renderModal({ initialItemId: TRACKED.itemId });

    fireEvent.click(screen.getByRole("button", { name: "Receive" }));

    await waitFor(() =>
      expect(InventoryManagementAPI.receive).toHaveBeenCalled(),
    );
    expect(
      InventoryManagementAPI.receive.mock.calls[0][0].lotNumber,
    ).toBeNull();
  });

  it("refuses to receive nothing", async () => {
    renderModal({ initialItemId: UNTRACKED.itemId });

    fireEvent.change(screen.getByLabelText(/quantity received/i), {
      target: { value: "0" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Receive" }));

    expect(screen.getByText("Enter how much arrived.")).toBeInTheDocument();
    expect(InventoryManagementAPI.receive).not.toHaveBeenCalled();
  });

  it("refuses to receive against no item", async () => {
    renderModal();

    fireEvent.click(screen.getByRole("button", { name: "Receive" }));

    expect(
      screen.getByText("Choose the item being received."),
    ).toBeInTheDocument();
    expect(InventoryManagementAPI.receive).not.toHaveBeenCalled();
  });

  describe("scan to identify", () => {
    it("resolves an item from its product barcode without searching", () => {
      renderModal();

      scan(TRACKED.upc);

      // Selecting the tracked item is what reveals the lot fields.
      expect(screen.getByLabelText(/lot number/i)).toBeInTheDocument();
      expect(scanField()).toHaveValue("");
    });

    it("ignores surrounding whitespace a scanner may add", () => {
      renderModal();

      scan(`  ${TRACKED.upc} `);

      expect(screen.getByLabelText(/lot number/i)).toBeInTheDocument();
    });

    /**
     * A code matching nothing is the normal way a lab meets a product it has
     * not defined. Stopping with an error would be the wrong answer.
     */
    it("offers to define a new item when the barcode matches nothing", () => {
      const onDefineNew = vi.fn();
      renderModal({ onDefineNew });

      scan("00000000000000");

      expect(
        screen.getByText("No item carries that barcode"),
      ).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: "New item" }));
      expect(onDefineNew).toHaveBeenCalledWith("00000000000000");
    });

    it("does not submit the dialog on the scanner's trailing Enter", () => {
      renderModal({ initialItemId: UNTRACKED.itemId });

      scan(UNTRACKED.code);

      expect(InventoryManagementAPI.receive).not.toHaveBeenCalled();
    });

    it("does not match an item that has no barcode at all", () => {
      renderModal();

      scan("");
      fireEvent.change(scanField(), { target: { value: "   " } });
      fireEvent.keyDown(scanField(), { key: "Enter", code: "Enter" });

      expect(
        screen.queryByText("No item carries that barcode"),
      ).not.toBeInTheDocument();
    });
  });

  it("says the location is optional rather than demanding one", () => {
    renderModal({ initialItemId: UNTRACKED.itemId });

    expect(
      screen.getByText(/You can set this now or from the lot later/i),
    ).toBeInTheDocument();
  });
});
