import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryItemForm from "./InventoryItemForm";
import { NotificationContext } from "../layout/Layout";
import { InventoryItemAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryItemAPI: {
    getItemTypes: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
  },
}));

const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),
};

const renderForm = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={mockNotificationContext}>
        <InventoryItemForm
          open
          onClose={vi.fn()}
          onSave={vi.fn()}
          item={null}
          {...props}
        />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  InventoryItemAPI.getItemTypes.mockResolvedValue([
    { code: "REAGENT", label: "Reagent" },
    { code: "RDT", label: "RDT (Rapid Diagnostic Test)" },
  ]);
});

describe("InventoryItemForm — Code field (OGC-658 Part C)", () => {
  it("shows an editable Code field with an auto-generate hint when adding a new item", async () => {
    renderForm();

    const codeInput = await screen.findByLabelText(/code/i);
    expect(codeInput).not.toBeDisabled();
    expect(codeInput).toHaveValue("");
  });

  it("uppercases the code as it's typed and submits it on create", async () => {
    InventoryItemAPI.create.mockResolvedValue({ id: "MY_REAGENT" });
    const onSave = vi.fn();
    renderForm({ onSave });

    fireEvent.change(await screen.findByLabelText(/^item name/i), {
      target: { value: "My Reagent" },
    });
    const codeInput = screen.getByLabelText(/code/i);
    fireEvent.change(codeInput, { target: { value: "my_reagent" } });
    expect(codeInput).toHaveValue("MY_REAGENT");
    fireEvent.change(screen.getByLabelText(/stability after opening/i), {
      target: { value: "30" },
    });

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryItemAPI.create).toHaveBeenCalledWith(
        expect.objectContaining({ id: "MY_REAGENT", name: "My Reagent" }),
      );
    });
    expect(onSave).toHaveBeenCalled();
  });

  it("submits a null code when left blank, letting the server auto-generate one", async () => {
    InventoryItemAPI.create.mockResolvedValue({ id: "GENERATED" });
    renderForm();

    fireEvent.change(await screen.findByLabelText(/^item name/i), {
      target: { value: "Auto Generated Item" },
    });
    fireEvent.change(screen.getByLabelText(/stability after opening/i), {
      target: { value: "30" },
    });
    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryItemAPI.create).toHaveBeenCalledWith(
        expect.objectContaining({ id: null }),
      );
    });
  });

  it("locks the Code field and does not submit it when editing an existing item", async () => {
    InventoryItemAPI.update.mockResolvedValue({});
    const existingItem = {
      id: "EXISTING_CODE",
      name: "Existing Item",
      itemType: "REAGENT",
      units: "mL",
      stabilityAfterOpening: 30,
    };
    renderForm({ item: existingItem });

    const codeInput = await screen.findByLabelText(/code/i);
    expect(codeInput).toBeDisabled();
    expect(codeInput).toHaveValue("EXISTING_CODE");

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryItemAPI.update).toHaveBeenCalled();
    });
    const [, payload] = InventoryItemAPI.update.mock.calls[0];
    expect(payload.id).toBeUndefined();
  });
});
