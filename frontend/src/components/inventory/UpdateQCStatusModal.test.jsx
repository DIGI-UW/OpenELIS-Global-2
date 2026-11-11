import React, { useState } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import UpdateQCStatusModal from "./UpdateQCStatusModal";
import { InventoryLotAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryLotAPI: { updateQCStatus: vi.fn() },
}));

const lot = {
  id: 5,
  lotNumber: "LOT-5",
  qcStatus: "PENDING",
  inventoryItem: { name: "Malaria RDT" },
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
    <UpdateQCStatusModal
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
  InventoryLotAPI.updateQCStatus.mockResolvedValue({});
});

describe("UpdateQCStatusModal", () => {
  it("offers exactly the QC statuses the database accepts", async () => {
    renderWithIntl(
      <UpdateQCStatusModal open lot={lot} onClose={vi.fn()} onSave={vi.fn()} />,
    );

    fireEvent.click(document.querySelector("#qcStatus button"));

    expect(
      await screen.findByRole("option", { name: "Quarantined" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Not Required" }),
    ).not.toBeInTheDocument();
  });

  it("submits QUARANTINED when chosen", async () => {
    renderWithIntl(
      <UpdateQCStatusModal open lot={lot} onClose={vi.fn()} onSave={vi.fn()} />,
    );

    fireEvent.click(document.querySelector("#qcStatus button"));
    fireEvent.click(await screen.findByRole("option", { name: "Quarantined" }));
    fireEvent.click(screen.getByText(messages["button.update"]));

    await waitFor(() =>
      expect(InventoryLotAPI.updateQCStatus).toHaveBeenCalledWith(
        5,
        "QUARANTINED",
        "",
      ),
    );
  });

  it("does not update state after onSave has unmounted it", async () => {
    const onSaved = vi.fn();
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    renderWithIntl(<Host onSaved={onSaved} />);

    fireEvent.click(document.querySelector("#qcStatus button"));
    fireEvent.click(await screen.findByRole("option", { name: "Passed" }));
    fireEvent.click(screen.getByText(messages["button.update"]));

    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 0));

    const unmountedWarnings = consoleError.mock.calls.filter((args) =>
      String(args[0]).includes("unmounted component"),
    );
    expect(unmountedWarnings).toEqual([]);
    consoleError.mockRestore();
  });
});
