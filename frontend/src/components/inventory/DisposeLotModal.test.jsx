import React, { useState } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import DisposeLotModal from "./DisposeLotModal";
import { InventoryLotAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryLotAPI: { dispose: vi.fn() },
}));

const lot = {
  id: 4,
  lotNumber: "LOT-4",
  currentQuantity: 4,
  inventoryItem: { name: "Malaria RDT", units: "kits" },
};

// Mirrors the dashboard, which stops rendering the modal inside onSave.
const Host = ({ onSaved }) => {
  const [open, setOpen] = useState(true);
  return open ? (
    <DisposeLotModal
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

describe("DisposeLotModal", () => {
  it("does not update state after onSave has unmounted it", async () => {
    InventoryLotAPI.dispose.mockResolvedValue({});
    const onSaved = vi.fn();
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    render(
      <IntlProvider locale="en" messages={messages}>
        <Host onSaved={onSaved} />
      </IntlProvider>,
    );

    fireEvent.click(screen.getByText(messages["button.dispose"]));

    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 0));

    const unmountedWarnings = consoleError.mock.calls.filter((args) =>
      String(args[0]).includes("unmounted component"),
    );
    expect(unmountedWarnings).toEqual([]);
    consoleError.mockRestore();
  });
});
