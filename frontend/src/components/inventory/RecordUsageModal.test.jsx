import React, { useState } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import RecordUsageModal from "./RecordUsageModal";
import { InventoryManagementAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryManagementAPI: { consume: vi.fn() },
}));
vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const lot = {
  id: 1,
  lotNumber: "LOT-100",
  currentQuantity: 3,
  inventoryItem: { id: 7, name: "Malaria RDT", units: "kits" },
};

const renderWithIntl = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

// Mirrors the dashboard, which stops rendering the modal inside onSave.
const Host = ({ onSaved }) => {
  const [open, setOpen] = useState(true);
  return open ? (
    <RecordUsageModal
      open
      lot={lot}
      onClose={vi.fn()}
      onSave={() => {
        setOpen(false);
        onSaved();
      }}
    />
  ) : null;
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("RecordUsageModal", () => {
  // POST /rest/inventory/management/consume is item-level FEFO; there is no
  // lot-level consume endpoint, so the modal must not present the lot it
  // was opened from as the target.
  it("presents usage as item-level FEFO rather than against the opened lot", () => {
    renderWithIntl(
      <RecordUsageModal open lot={lot} onClose={vi.fn()} onSave={vi.fn()} />,
    );

    expect(screen.getByText("Malaria RDT")).toBeInTheDocument();
    expect(screen.getByText(messages["usage.fefo.note"])).toBeInTheDocument();
    expect(screen.queryByText("LOT-100")).not.toBeInTheDocument();
  });

  it("consumes against the item and lets the server decide sufficiency", async () => {
    InventoryManagementAPI.consume.mockResolvedValue({ consumedLots: [] });
    const onSave = vi.fn();
    renderWithIntl(
      <RecordUsageModal open lot={lot} onClose={vi.fn()} onSave={onSave} />,
    );

    // More than the opened lot holds: other lots of the item may cover it.
    fireEvent.change(screen.getByLabelText(/quantity used/i), {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByText(messages["button.record"]));

    await waitFor(() =>
      expect(InventoryManagementAPI.consume).toHaveBeenCalledWith(
        expect.objectContaining({ itemId: "7", quantity: 5 }),
      ),
    );
    expect(onSave).toHaveBeenCalled();
  });

  it("shows a server rejection once", async () => {
    const message =
      "No available lots for item Malaria RDT: 1 lot is awaiting QC";
    InventoryManagementAPI.consume.mockRejectedValue(new Error(message));
    renderWithIntl(
      <RecordUsageModal open lot={lot} onClose={vi.fn()} onSave={vi.fn()} />,
    );

    fireEvent.click(screen.getByText(messages["button.record"]));

    expect(await screen.findAllByText(message)).toHaveLength(1);
  });

  it("renders a QC-gate refusal from errorCode and params, not the raw message", async () => {
    // The shape InventoryService.post builds from the 409 body; the message is
    // the backend's own wording, which differs from the en.json text.
    const err = new Error(
      "No QC-passed stock for Malaria RDT (MAL-RDT): 1 lot(s) with stock are awaiting QC; mark QC as passed to use them",
    );
    err.errorCode = "inventory.consume.error.noLotsAwaitingQc";
    err.params = { name: "Malaria RDT", code: "MAL-RDT", count: "1" };
    InventoryManagementAPI.consume.mockRejectedValue(err);
    renderWithIntl(
      <RecordUsageModal open lot={lot} onClose={vi.fn()} onSave={vi.fn()} />,
    );

    fireEvent.click(screen.getByText(messages["button.record"]));

    expect(
      await screen.findByText(
        "No QC-passed stock for Malaria RDT (MAL-RDT): 1 lot(s) with stock are awaiting QC. Mark QC as passed to use them.",
      ),
    ).toBeInTheDocument();
  });

  it("does not update state after onSave has unmounted it", async () => {
    InventoryManagementAPI.consume.mockResolvedValue({ consumedLots: [] });
    const onSaved = vi.fn();
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    renderWithIntl(<Host onSaved={onSaved} />);

    fireEvent.click(screen.getByText(messages["button.record"]));

    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 0));

    const unmountedWarnings = consoleError.mock.calls.filter((args) =>
      String(args[0]).includes("unmounted component"),
    );
    expect(unmountedWarnings).toEqual([]);
    consoleError.mockRestore();
  });
});
