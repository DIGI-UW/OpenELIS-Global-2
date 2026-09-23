import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryItemForm from "./InventoryItemForm";
import { NotificationContext } from "../layout/Layout";
import { InventoryItemAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryItemAPI: {
    getTags: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
  },
}));

const notificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  addNotification: vi.fn(),
};

const renderForm = async (props = {}) => {
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notificationContext}>
        <InventoryItemForm open onClose={vi.fn()} onSave={vi.fn()} {...props} />
      </NotificationContext.Provider>
    </IntlProvider>,
  );
  // The suggestion fetch resolves before anything is asserted.
  await screen.findByLabelText(/item name/i);
  return view;
};

const tagField = () => screen.getByLabelText(/^tags$/i);

const typeTag = (value) => {
  const field = tagField();
  fireEvent.change(field, { target: { value } });
  fireEvent.keyDown(field, { key: "Enter", code: "Enter" });
};

const appliedTags = () =>
  Array.from(document.querySelectorAll(".inventory-item-tags__chips .cds--tag"))
    .map((chip) => chip.textContent.replace(/Remove.*$/, "").trim())
    .filter(Boolean);

beforeEach(() => {
  vi.clearAllMocks();
  InventoryItemAPI.getTags.mockResolvedValue(["Cartridge", "Reagent"]);
  InventoryItemAPI.create.mockResolvedValue({ id: 1 });
  InventoryItemAPI.update.mockResolvedValue({ id: 1 });
});

describe("InventoryItemForm — item type is a tag now", () => {
  it("has no item type control at all", async () => {
    await renderForm();

    expect(screen.queryByLabelText(/item type/i)).not.toBeInTheDocument();
    expect(screen.queryByText("Analyzer Cartridge")).not.toBeInTheDocument();
  });

  it("offers the tags already in use as suggestions", async () => {
    await renderForm();

    const options = Array.from(
      document.querySelectorAll("#inventory-tag-suggestions option"),
    ).map((option) => option.value);
    expect(options).toEqual(["Cartridge", "Reagent"]);
  });

  it("clears the field after a tag is added, ready for the next", async () => {
    await renderForm();

    typeTag("TB");

    expect(tagField()).toHaveValue("");
  });

  it("stops Enter reaching the dialog so confirming a tag cannot save the item", async () => {
    const onSave = vi.fn();
    await renderForm({ onSave });

    typeTag("TB");

    expect(InventoryItemAPI.create).not.toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
  });

  it("keeps a tag that was typed but never confirmed with Enter", async () => {
    await renderForm();
    fireEvent.change(screen.getByLabelText(/item name/i), {
      target: { value: "Half typed" },
    });
    fireEvent.change(screen.getByLabelText(/units/i), {
      target: { value: "tests" },
    });

    fireEvent.change(tagField(), { target: { value: "Consumable" } });
    fireEvent.click(screen.getByText("Save"));

    expect(InventoryItemAPI.create.mock.calls[0][0].tags).toEqual([
      "Consumable",
    ]);
  });

  it("adds a typed tag as a chip and sends it on save", async () => {
    await renderForm();
    fireEvent.change(screen.getByLabelText(/item name/i), {
      target: { value: "GeneXpert cartridge" },
    });
    fireEvent.change(screen.getByLabelText(/units/i), {
      target: { value: "tests" },
    });

    typeTag("TB");

    expect(appliedTags()).toEqual(["TB"]);

    fireEvent.click(screen.getByText("Save"));

    expect(InventoryItemAPI.create).toHaveBeenCalledTimes(1);
    const payload = InventoryItemAPI.create.mock.calls[0][0];
    expect(payload.tags).toEqual(["TB"]);
    expect(payload.itemType).toBeUndefined();
  });

  it("carries several tags on one item, which the old single type could not", async () => {
    await renderForm();

    typeTag("Cartridge");
    typeTag("TB");

    expect(appliedTags()).toEqual(["Cartridge", "TB"]);
  });

  it("removes a tag when its chip is dismissed", async () => {
    await renderForm();
    typeTag("Cartridge");
    typeTag("TB");

    const chips = document.querySelectorAll(
      ".inventory-item-tags__chips .cds--tag",
    );
    fireEvent.click(within(chips[0]).getByRole("button"));

    expect(appliedTags()).toEqual(["TB"]);
  });

  it("refuses a duplicate tag whatever its spelling", async () => {
    await renderForm();

    typeTag("Cartridge");
    typeTag("cartridge");
    typeTag("  CARTRIDGE  ");

    expect(appliedTags()).toEqual(["Cartridge"]);
  });

  it("ignores a blank tag", async () => {
    await renderForm();

    typeTag("   ");

    expect(appliedTags()).toEqual([]);
  });

  it("opens an existing item on the tags it already carries", async () => {
    await renderForm({
      item: {
        id: 7,
        name: "GeneXpert cartridge",
        units: "tests",
        tags: ["Cartridge", "TB"],
      },
    });

    expect(appliedTags()).toEqual(["Cartridge", "TB"]);
  });

  /**
   * Stability, storage, analyzers and tests-per-kit used to appear only for the
   * matching item type and were required when they did. Nothing keys on a type
   * any more, so every item is offered all four and required to give none.
   */
  it("offers every former type-specific field to every item, none required", async () => {
    await renderForm();
    fireEvent.change(screen.getByLabelText(/item name/i), {
      target: { value: "Examination gloves" },
    });
    fireEvent.change(screen.getByLabelText(/units/i), {
      target: { value: "boxes" },
    });

    expect(screen.getByLabelText(/stability after opening/i)).toBeVisible();
    expect(screen.getByLabelText(/storage requirements/i)).toBeVisible();
    expect(screen.getByLabelText(/compatible analyzers/i)).toBeVisible();
    expect(screen.getByLabelText(/tests per kit/i)).toBeVisible();

    fireEvent.click(screen.getByText("Save"));

    expect(InventoryItemAPI.create).toHaveBeenCalledTimes(1);
  });

  /**
   * Both numbers carry a minimum of 1 on the entity. Sending an untouched field
   * as 0 — which is what happened once they stopped being gated by item type —
   * is a 400 from bean validation, not an empty field.
   */
  it("sends an untouched optional number as null, never as zero", async () => {
    await renderForm();
    fireEvent.change(screen.getByLabelText(/item name/i), {
      target: { value: "Examination gloves" },
    });
    fireEvent.change(screen.getByLabelText(/units/i), {
      target: { value: "boxes" },
    });

    fireEvent.click(screen.getByText("Save"));

    const payload = InventoryItemAPI.create.mock.calls[0][0];
    expect(payload.stabilityAfterOpening).toBeNull();
    expect(payload.testsPerKit).toBeNull();
  });

  it("sends a filled optional number as itself", async () => {
    await renderForm();
    fireEvent.change(screen.getByLabelText(/item name/i), {
      target: { value: "A reagent" },
    });
    fireEvent.change(screen.getByLabelText(/units/i), {
      target: { value: "mL" },
    });
    fireEvent.change(screen.getByLabelText(/stability after opening/i), {
      target: { value: "30" },
    });

    fireEvent.click(screen.getByText("Save"));

    expect(InventoryItemAPI.create.mock.calls[0][0].stabilityAfterOpening).toBe(
      30,
    );
  });

  it("says why there is no auto-consume switch", async () => {
    await renderForm();

    expect(
      screen.getByText(/There is no auto-consume setting/i),
    ).toBeInTheDocument();
  });

  it("still saves when the tag suggestions cannot be loaded", async () => {
    InventoryItemAPI.getTags.mockRejectedValue(new Error("offline"));
    await renderForm();
    fireEvent.change(screen.getByLabelText(/item name/i), {
      target: { value: "Offline item" },
    });
    fireEvent.change(screen.getByLabelText(/units/i), {
      target: { value: "tests" },
    });

    typeTag("Consumable");
    fireEvent.click(screen.getByText("Save"));

    expect(InventoryItemAPI.create.mock.calls[0][0].tags).toEqual([
      "Consumable",
    ]);
  });
});

const codeFieldNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),
};

const renderCodeForm = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={codeFieldNotificationContext}>
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

describe("InventoryItemForm — Code field (OGC-658 Part C)", () => {
  it("shows an editable Code field with an auto-generate hint when adding a new item", async () => {
    renderCodeForm();

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
    renderCodeForm({ onSave });

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
    renderCodeForm();

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
    renderCodeForm();

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
    renderCodeForm();

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
    renderCodeForm({ item: existingItem });

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

  /**
   * Stability is no longer demanded of anything. It used to be required when the
   * item type was REAGENT, and a new item defaulted to that type; with the type
   * gone there is no category left to demand it of. A lot gets an open-vial
   * expiry date when its item declares a stability period and none when it does
   * not, which is now the whole of the rule.
   */
  it("creates an item with no stability value, sending null rather than zero", async () => {
    InventoryItemAPI.create.mockResolvedValue({ id: 1005 });
    renderForm();

    fireEvent.change(await screen.findByLabelText(/^item name/i), {
      target: { value: "Examination gloves" },
    });
    fireEvent.click(screen.getByText("Save"));

    await waitFor(() => expect(InventoryItemAPI.create).toHaveBeenCalled());
    const [payload] = InventoryItemAPI.create.mock.calls[0];
    // The entity is @Min(1), so a 0 would be a 400; null keeps the field unset.
    expect(payload.stabilityAfterOpening).toBeNull();
    expect(
      screen.queryByText(/stability after opening is required/i),
    ).not.toBeInTheDocument();
  });

  it("shows the translated message for a duplicate-code error instead of the raw backend text (OGC-658 C8)", async () => {
    const duplicateError = new Error(
      "Inventory item code already exists: MY-REAGENT",
    );
    duplicateError.errorCode = "inventory.item.error.duplicateCode";
    duplicateError.params = { code: "MY-REAGENT" };
    InventoryItemAPI.create.mockRejectedValue(duplicateError);
    renderCodeForm();

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
    expect(codeFieldNotificationContext.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        subtitle: 'An inventory item with code "MY-REAGENT" already exists.',
      }),
    );
  });
});
