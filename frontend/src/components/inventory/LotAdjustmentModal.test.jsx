import React, { useState } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LotAdjustmentModal from "./LotAdjustmentModal";
import { InventoryLotAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryLotAPI: { adjust: vi.fn() },
}));

const lot = {
  id: 3,
  lotNumber: "LOT-3",
  currentQuantity: 4,
  inventoryItem: { name: "Malaria RDT", units: "kits" },
};

// Mirrors the dashboard, which stops rendering the modal inside onSave.
const Host = ({ onSaved }) => {
  const [open, setOpen] = useState(true);
  return open ? (
    <LotAdjustmentModal
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

describe("LotAdjustmentModal", () => {
  it("does not update state after onSave has unmounted it", async () => {
    InventoryLotAPI.adjust.mockResolvedValue({});
    const onSaved = vi.fn();
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    render(
      <IntlProvider locale="en" messages={messages}>
        <Host onSaved={onSaved} />
      </IntlProvider>,
    );

    fireEvent.click(screen.getByText(messages["button.adjust"]));

    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 0));

    const unmountedWarnings = consoleError.mock.calls.filter((args) =>
      String(args[0]).includes("unmounted component"),
    );
    expect(unmountedWarnings).toEqual([]);
    consoleError.mockRestore();
  });

  it("sends the notes along with the reason", async () => {
    InventoryLotAPI.adjust.mockResolvedValue({});
    render(
      <IntlProvider locale="en" messages={messages}>
        <LotAdjustmentModal open lot={lot} onClose={vi.fn()} onSave={vi.fn()} />
      </IntlProvider>,
    );

    fireEvent.change(screen.getByLabelText(messages["adjustment.notes"]), {
      target: { value: "One box crushed in transit" },
    });
    fireEvent.click(screen.getByText(messages["button.adjust"]));

    await waitFor(() =>
      expect(InventoryLotAPI.adjust).toHaveBeenCalledWith(
        3,
        4,
        "INVENTORY_COUNT",
        "One box crushed in transit",
      ),
    );
  });

  it("lists the adjustment reasons in the reader language and still sends the reason code", async () => {
    InventoryLotAPI.adjust.mockResolvedValue({});
    const french = {
      ...messages,
      "adjustment.reason.INVENTORY_COUNT": "Inventaire physique",
      "adjustment.reason.DAMAGED": "Articles endommagés",
      "adjustment.reason.FOUND": "Articles retrouvés",
      "adjustment.reason.ERROR_CORRECTION": "Correction de saisie",
      "adjustment.reason.EXPIRED_DISPOSAL": "Élimination de périmés",
      "adjustment.reason.OTHER": "Autre",
    };
    render(
      <IntlProvider locale="fr" messages={french}>
        <LotAdjustmentModal open lot={lot} onClose={vi.fn()} onSave={vi.fn()} />
      </IntlProvider>,
    );

    fireEvent.click(
      screen.getByRole("combobox", { name: messages["adjustment.reason"] }),
    );
    expect(
      screen.getAllByRole("option").map((option) => option.textContent),
    ).toEqual([
      "Inventaire physique",
      "Articles endommagés",
      "Articles retrouvés",
      "Correction de saisie",
      "Élimination de périmés",
      "Autre",
    ]);

    fireEvent.click(screen.getByRole("option", { name: "Articles retrouvés" }));
    fireEvent.click(screen.getByText(messages["button.adjust"]));

    await waitFor(() =>
      expect(InventoryLotAPI.adjust).toHaveBeenCalledWith(3, 4, "FOUND", ""),
    );
  });
});
