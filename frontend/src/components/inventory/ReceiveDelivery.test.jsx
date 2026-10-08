import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ReceiveDelivery from "./ReceiveDelivery";
import { InventoryBoardAPI, InventoryManagementAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryBoardAPI: { get: vi.fn() },
  InventoryManagementAPI: { receiveBatch: vi.fn() },
}));

vi.mock("./InventoryItemForm", () => ({
  default: ({ open, initialUpc }) =>
    open ? <div data-testid="item-form">{initialUpc}</div> : null,
}));

const CARTRIDGE = {
  itemId: 1,
  name: "GeneXpert MTB/RIF cartridge",
  code: "GENEXPERT",
  units: "tests",
  upc: "08901234567890",
  trackLots: true,
};

const GLOVES = {
  itemId: 2,
  name: "Examination gloves (box)",
  code: "GLOVES",
  units: "boxes",
  upc: "05012345678900",
  trackLots: false,
};

const renderScreen = async () => {
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <ReceiveDelivery />
    </IntlProvider>,
  );
  await screen.findByLabelText(/scan the box/i);
  return view;
};

const scanField = () => screen.getByLabelText(/scan the box/i);

const scan = (code) => {
  fireEvent.change(scanField(), { target: { value: code } });
  fireEvent.keyDown(scanField(), { key: "Enter", code: "Enter" });
};

const lineRows = () => Array.from(document.querySelectorAll("tbody tr"));

beforeEach(() => {
  vi.clearAllMocks();
  InventoryBoardAPI.get.mockResolvedValue([CARTRIDGE, GLOVES]);
  InventoryManagementAPI.receiveBatch.mockResolvedValue([]);
});

describe("ReceiveDelivery", () => {
  it("fills the item, lot and expiry from one scan", async () => {
    await renderScreen();

    scan("010890123456789017271231101A2B3C");

    await waitFor(() =>
      expect(screen.getByLabelText(/lot number/i)).toHaveValue("1A2B3C"),
    );
    expect(screen.getByLabelText(/expiration date/i)).toHaveValue("2027-12-31");
    expect(screen.getByDisplayValue(/GeneXpert/)).toBeInTheDocument();
  });

  it("clears the scan field so the next box can be scanned straight away", async () => {
    await renderScreen();

    scan("0108901234567890");

    await waitFor(() => expect(scanField()).toHaveValue(""));
  });

  it("does not submit anything on the scanner's trailing Enter", async () => {
    await renderScreen();

    scan("0108901234567890");

    expect(InventoryManagementAPI.receiveBatch).not.toHaveBeenCalled();
  });

  it("builds a delivery one line at a time", async () => {
    await renderScreen();

    scan("010890123456789017271231101A2B3C");
    await waitFor(() =>
      expect(screen.getByLabelText(/lot number/i)).toHaveValue("1A2B3C"),
    );
    fireEvent.change(screen.getByLabelText(/current quantity|quantity/i), {
      target: { value: "20" },
    });
    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

    expect(lineRows()).toHaveLength(1);
    expect(lineRows()[0]).toHaveTextContent("1A2B3C");
    expect(lineRows()[0]).toHaveTextContent("20 tests");
  });

  it("clears the line after adding it, ready for the next scan", async () => {
    await renderScreen();
    scan("010890123456789017271231101A2B3C");
    await waitFor(() =>
      expect(screen.getByLabelText(/lot number/i)).toHaveValue("1A2B3C"),
    );

    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

    expect(screen.getByLabelText(/lot number/i)).toHaveValue("");
    expect(screen.getByLabelText(/expiration date/i)).toHaveValue("");
  });

  it("removes a line that was added by mistake", async () => {
    await renderScreen();
    scan("0108901234567890");
    await waitFor(() =>
      expect(screen.getByDisplayValue(/GeneXpert/)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));
    expect(lineRows()).toHaveLength(1);

    fireEvent.click(
      within(lineRows()[0]).getByRole("button", { name: "Remove" }),
    );

    expect(lineRows()).toHaveLength(0);
  });

  it("removes only the line asked for, after an earlier removal rewound the count", async () => {
    await renderScreen();
    const addSameBox = async () => {
      scan("010890123456789017271231101A2B3C");
      await waitFor(() =>
        expect(screen.getByLabelText(/lot number/i)).toHaveValue("1A2B3C"),
      );
      fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));
    };

    await addSameBox();
    await addSameBox();
    expect(lineRows()).toHaveLength(2);

    fireEvent.click(
      within(lineRows()[0]).getByRole("button", { name: "Remove" }),
    );
    expect(lineRows()).toHaveLength(1);

    await addSameBox();
    expect(lineRows()).toHaveLength(2);

    fireEvent.click(
      within(lineRows()[0]).getByRole("button", { name: "Remove" }),
    );
    expect(lineRows()).toHaveLength(1);
  });

  it("does not leave the previous label's batch on the next box", async () => {
    await renderScreen();

    scan("010890123456789017271231101A2B3C");
    await waitFor(() =>
      expect(screen.getByLabelText(/lot number/i)).toHaveValue("1A2B3C"),
    );

    scan("0108901234567890");

    await waitFor(() =>
      expect(screen.getByLabelText(/lot number/i)).toHaveValue(""),
    );
    expect(screen.getByLabelText(/expiration date/i)).toHaveValue("");
  });

  it("writes nothing until the delivery is confirmed", async () => {
    await renderScreen();
    scan("0108901234567890");
    await waitFor(() =>
      expect(screen.getByDisplayValue(/GeneXpert/)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

    expect(InventoryManagementAPI.receiveBatch).not.toHaveBeenCalled();
  });

  it("commits every line in one call", async () => {
    await renderScreen();

    scan("010890123456789017271231101A2B3C");
    await waitFor(() =>
      expect(screen.getByLabelText(/lot number/i)).toHaveValue("1A2B3C"),
    );
    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

    scan("0108901234567890");
    await waitFor(() =>
      expect(screen.getByDisplayValue(/GeneXpert/)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

    fireEvent.click(screen.getByRole("button", { name: /receive 2 lines/i }));

    await waitFor(() =>
      expect(InventoryManagementAPI.receiveBatch).toHaveBeenCalledTimes(1),
    );
    const payload = InventoryManagementAPI.receiveBatch.mock.calls[0][0];
    expect(payload).toHaveLength(2);
    expect(payload[0].inventoryItem).toEqual({ id: CARTRIDGE.itemId });
    expect(payload[0].lotNumber).toBe("1A2B3C");
    expect(payload[0].id).toBeUndefined();
  });

  it("discards a delivery without writing it", async () => {
    await renderScreen();
    scan("0108901234567890");
    await waitFor(() =>
      expect(screen.getByDisplayValue(/GeneXpert/)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

    fireEvent.click(screen.getByRole("button", { name: /discard delivery/i }));

    expect(lineRows()).toHaveLength(0);
    expect(InventoryManagementAPI.receiveBatch).not.toHaveBeenCalled();
  });

  describe("when the label does not cooperate", () => {
    it("says a code was not recognised and leaves the line to be typed", async () => {
      await renderScreen();

      scan("SOMETHING-FROM-ANOTHER-SYSTEM");

      expect(
        await screen.findByText(/was not recognised/i),
      ).toBeInTheDocument();
      expect(screen.getByLabelText(/lot number/i)).toHaveValue("");
    });

    it("keeps the raw scan on a line it could only partly read", async () => {
      await renderScreen();

      scan("010890123456789091UNKNOWNTAIL");
      await waitFor(() =>
        expect(screen.getByDisplayValue(/GeneXpert/)).toBeInTheDocument(),
      );
      fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

      expect(lineRows()[0]).toHaveTextContent("Partly scanned");
    });

    it("warns about an expired lot without refusing it", async () => {
      await renderScreen();

      scan("0108901234567890" + "17200101");
      await waitFor(() =>
        expect(screen.getByLabelText(/expiration date/i)).toHaveValue(
          "2020-01-01",
        ),
      );

      expect(
        screen.getByText(/already past its expiry date/i),
      ).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));
      expect(lineRows()).toHaveLength(1);
    });

    it("offers to define an item the barcode does not match, without opening the form itself", async () => {
      await renderScreen();

      scan("09999999999999");

      expect(
        await screen.findByText("No item carries that barcode"),
      ).toBeInTheDocument();
      expect(screen.queryByTestId("item-form")).not.toBeInTheDocument();
    });

    it("keeps the lines already entered while a new item is defined", async () => {
      await renderScreen();
      scan("0108901234567890");
      await waitFor(() =>
        expect(screen.getByDisplayValue(/GeneXpert/)).toBeInTheDocument(),
      );
      fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));
      expect(lineRows()).toHaveLength(1);

      scan("09999999999999");
      fireEvent.click(await screen.findByRole("button", { name: "New item" }));

      expect(screen.getByTestId("item-form")).toHaveTextContent(
        "09999999999999",
      );
      expect(lineRows()).toHaveLength(1);
    });
  });

  it("asks only the item and quantity for an item without lots, even when the label carries a batch", async () => {
    await renderScreen();

    scan("010501234567890017271231" + "10BOX-7");
    await waitFor(() =>
      expect(screen.getByDisplayValue(/gloves/)).toBeInTheDocument(),
    );

    expect(screen.queryByLabelText(/lot number/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/expiration date/i)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));
    fireEvent.click(screen.getByRole("button", { name: /receive 1 line/i }));

    await waitFor(() =>
      expect(InventoryManagementAPI.receiveBatch).toHaveBeenCalledTimes(1),
    );
    const [line] = InventoryManagementAPI.receiveBatch.mock.calls[0][0];
    expect(line.inventoryItem).toEqual({ id: GLOVES.itemId });
    expect(line.lotNumber).toBeNull();
    expect(line.expirationDate).toBeNull();
  });

  it("refuses a line with no item", async () => {
    await renderScreen();

    fireEvent.click(screen.getByRole("button", { name: /add to delivery/i }));

    expect(
      screen.getByText("Choose the item being received."),
    ).toBeInTheDocument();
    expect(lineRows()).toHaveLength(0);
  });
});
