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
  InventoryItemAPI.getItemTypes.mockResolvedValue(["REAGENT", "RDT"]);
});

describe("InventoryItemForm — Code field (OGC-658 Part C)", () => {
  it("shows an editable Code field with an auto-generate hint when adding a new item", async () => {
    renderForm();

    const codeInput = await screen.findByLabelText(/code/i);
    expect(codeInput).not.toBeDisabled();
    expect(codeInput).toHaveValue("");
  });

  it("leaves the code as typed, previews the normalized form, and submits that form on create", async () => {
    InventoryItemAPI.create.mockResolvedValue({
      id: 1000,
      code: "MY-REAGENT-1",
    });
    const onSave = vi.fn();
    renderForm({ onSave });

    fireEvent.change(await screen.findByLabelText(/^item name/i), {
      target: { value: "My Reagent" },
    });
    const codeInput = screen.getByLabelText(/code/i);
    fireEvent.change(codeInput, { target: { value: "my reagent 1" } });
    expect(codeInput).toHaveValue("my reagent 1");
    expect(
      screen.getByText("Will be saved as MY-REAGENT-1"),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/stability after opening/i), {
      target: { value: "30" },
    });

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryItemAPI.create).toHaveBeenCalledWith(
        expect.objectContaining({ code: "MY-REAGENT-1", name: "My Reagent" }),
      );
    });
    expect(onSave).toHaveBeenCalled();
  });

  it("normalizes the code like the server does on save and caps the input at 64 characters", async () => {
    InventoryItemAPI.create.mockResolvedValue({
      id: 1003,
      code: "MY-REAGENT-V1",
    });
    renderForm();

    fireEvent.change(await screen.findByLabelText(/^item name/i), {
      target: { value: "My Reagent" },
    });
    const codeInput = screen.getByLabelText(/code/i);
    expect(codeInput).toHaveAttribute("maxlength", "64");

    fireEvent.change(codeInput, { target: { value: " my reagent, v1 " } });
    expect(codeInput).toHaveValue(" my reagent, v1 ");
    expect(
      screen.getByText("Will be saved as MY-REAGENT-V1"),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/stability after opening/i), {
      target: { value: "30" },
    });

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryItemAPI.create).toHaveBeenCalledWith(
        expect.objectContaining({ code: "MY-REAGENT-V1" }),
      );
    });
  });

  it("keeps the auto-generate hint when the typed code is already in its saved form", async () => {
    renderForm();

    const codeInput = await screen.findByLabelText(/code/i);
    fireEvent.change(codeInput, { target: { value: "MY-REAGENT" } });

    expect(codeInput).toHaveValue("MY-REAGENT");
    expect(screen.queryByText(/will be saved as/i)).not.toBeInTheDocument();
    expect(
      screen.getByText(messages["catalog.item.code.hint"]),
    ).toBeInTheDocument();
  });

  it("submits a null code when left blank, letting the server auto-generate one", async () => {
    InventoryItemAPI.create.mockResolvedValue({ id: 1001, code: "GENERATED" });
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
        expect.objectContaining({ code: null }),
      );
    });
  });

  it("locks the Code field and does not submit it when editing an existing item", async () => {
    InventoryItemAPI.update.mockResolvedValue({});
    const existingItem = {
      id: 1002,
      code: "EXISTING-CODE",
      name: "Existing Item",
      itemType: "REAGENT",
      units: "mL",
      stabilityAfterOpening: 30,
    };
    renderForm({ item: existingItem });

    const codeInput = await screen.findByLabelText(/code/i);
    expect(codeInput).toBeDisabled();
    expect(codeInput).toHaveValue("EXISTING-CODE");

    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(InventoryItemAPI.update).toHaveBeenCalled();
    });
    const [, payload] = InventoryItemAPI.update.mock.calls[0];
    expect(payload.code).toBeUndefined();
    expect(payload.id).toBeUndefined();
  });

  it("lets a legacy reagent with no stability value be edited without inventing one", async () => {
    InventoryItemAPI.update.mockResolvedValue({});
    const onSave = vi.fn();
    renderForm({
      onSave,
      item: {
        id: 1004,
        code: "LEGACY",
        name: "Legacy Reagent",
        itemType: "REAGENT",
        units: "mL",
        stabilityAfterOpening: null,
      },
    });

    await screen.findByLabelText(/code/i);
    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => expect(InventoryItemAPI.update).toHaveBeenCalled());
    const [, payload] = InventoryItemAPI.update.mock.calls[0];
    // The entity is @Min(1), so 0 would be rejected; null keeps it unset.
    expect(payload.stabilityAfterOpening).toBeNull();
    expect(onSave).toHaveBeenCalled();
    expect(
      screen.queryByText(/stability after opening is required/i),
    ).not.toBeInTheDocument();
  });

  it("still requires stability after opening when creating a reagent", async () => {
    renderForm();

    fireEvent.change(await screen.findByLabelText(/^item name/i), {
      target: { value: "New Reagent" },
    });
    fireEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText(/stability after opening is required/i),
    ).toBeInTheDocument();
    expect(InventoryItemAPI.create).not.toHaveBeenCalled();
  });

  it("shows the translated message for a duplicate-code error instead of the raw backend text (OGC-658 C8)", async () => {
    const duplicateError = new Error(
      "Inventory item code already exists: MY-REAGENT",
    );
    duplicateError.errorCode = "inventory.item.error.duplicateCode";
    duplicateError.params = { code: "MY-REAGENT" };
    InventoryItemAPI.create.mockRejectedValue(duplicateError);
    renderForm();

    fireEvent.change(await screen.findByLabelText(/^item name/i), {
      target: { value: "My Reagent" },
    });
    fireEvent.change(screen.getByLabelText(/stability after opening/i), {
      target: { value: "30" },
    });
    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => {
      expect(
        screen.getByText(
          'An inventory item with code "MY-REAGENT" already exists.',
        ),
      ).toBeInTheDocument();
    });
    expect(screen.queryByText(duplicateError.message)).not.toBeInTheDocument();
    expect(mockNotificationContext.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        subtitle: 'An inventory item with code "MY-REAGENT" already exists.',
      }),
    );
  });
});
