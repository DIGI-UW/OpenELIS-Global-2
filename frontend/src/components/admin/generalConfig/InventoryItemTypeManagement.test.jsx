import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import InventoryItemTypeManagement from "./InventoryItemTypeManagement";
import { NotificationContext } from "../../layout/Layout";
import { getFromOpenElisServer } from "../../utils/Utils";
import { InventoryItemTypeAPI } from "../../inventory/InventoryService";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../inventory/InventoryService", () => ({
  InventoryItemTypeAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    deactivate: vi.fn(),
  },
}));

const LOCALES = [
  {
    localeCode: "en",
    displayName: "English",
    active: true,
    sortOrder: 1,
    fallback: true,
  },
  {
    localeCode: "fr",
    displayName: "Français",
    active: true,
    sortOrder: 2,
    fallback: false,
  },
];

const SEEDED_TYPES = [
  {
    id: 1,
    code: "REAGENT",
    name: "Reagent",
    localized: { en: "Reagent", fr: "Réactif" },
    active: true,
    sortOrder: 10,
    seeded: true,
  },
  {
    id: 2,
    code: "RDT",
    name: "RDT (Rapid Diagnostic Test)",
    localized: { en: "RDT (Rapid Diagnostic Test)" },
    active: true,
    sortOrder: 20,
    seeded: true,
  },
];

const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),
};

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <NotificationContext.Provider value={mockNotificationContext}>
          <InventoryItemTypeManagement />
        </NotificationContext.Provider>
      </MemoryRouter>
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((endpoint, callback) => {
    if (endpoint === "/rest/supportedlocales") {
      callback(LOCALES);
    }
  });
  InventoryItemTypeAPI.getAll.mockResolvedValue(SEEDED_TYPES);
});

describe("InventoryItemTypeManagement", () => {
  it("loads and displays the admin-managed item types", async () => {
    renderPage();

    expect(await screen.findByText("REAGENT")).toBeInTheDocument();
    expect(screen.getByText("RDT")).toBeInTheDocument();
    expect(screen.getAllByText("Active").length).toBeGreaterThan(0);
  });

  it("adds a new item type with an auto-generated code", async () => {
    InventoryItemTypeAPI.create.mockResolvedValue({
      id: 3,
      code: "CONSUMABLE",
      name: "Consumable",
      localized: { en: "Consumable" },
      active: true,
      sortOrder: 30,
      seeded: false,
    });
    renderPage();
    await screen.findByText("REAGENT");

    fireEvent.click(screen.getByRole("button", { name: /add item type/i }));
    const nameInput = await screen.findByLabelText(
      /name · editing in english/i,
    );
    fireEvent.change(nameInput, { target: { value: "Consumable" } });

    const addButtons = screen.getAllByRole("button", {
      name: /add item type/i,
    });
    fireEvent.click(addButtons[addButtons.length - 1]);

    await waitFor(() => {
      expect(InventoryItemTypeAPI.create).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "Consumable",
          locale: "en",
          active: true,
        }),
      );
    });
    expect(InventoryItemTypeAPI.getAll).toHaveBeenCalledTimes(2);
  });

  it("shows the translated message for a duplicate-code error instead of the raw backend text (OGC-658 C8)", async () => {
    const duplicateError = new Error(
      "Inventory item type code already exists: REAGENT",
    );
    duplicateError.errorCode = "inventoryItemType.error.duplicateCode";
    duplicateError.params = { code: "REAGENT" };
    InventoryItemTypeAPI.create.mockRejectedValue(duplicateError);
    renderPage();
    await screen.findByText("REAGENT");

    fireEvent.click(screen.getByRole("button", { name: /add item type/i }));
    const nameInput = await screen.findByLabelText(
      /name · editing in english/i,
    );
    fireEvent.change(nameInput, { target: { value: "Reagent" } });

    const addButtons = screen.getAllByRole("button", {
      name: /add item type/i,
    });
    fireEvent.click(addButtons[addButtons.length - 1]);

    await waitFor(() => {
      expect(mockNotificationContext.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          message: 'An item type with code "REAGENT" already exists.',
        }),
      );
    });
  });

  it("creates a new item type as inactive when the Active toggle is switched off", async () => {
    InventoryItemTypeAPI.create.mockResolvedValue({
      id: 4,
      code: "DRAFT_TYPE",
      name: "Draft Type",
      localized: { en: "Draft Type" },
      active: false,
      sortOrder: 30,
      seeded: false,
    });
    renderPage();
    await screen.findByText("REAGENT");

    fireEvent.click(screen.getByRole("button", { name: /add item type/i }));
    const nameInput = await screen.findByLabelText(
      /name · editing in english/i,
    );
    fireEvent.change(nameInput, { target: { value: "Draft Type" } });
    fireEvent.click(screen.getByRole("switch", { name: /status/i }));

    const addButtons = screen.getAllByRole("button", {
      name: /add item type/i,
    });
    fireEvent.click(addButtons[addButtons.length - 1]);

    await waitFor(() => {
      expect(InventoryItemTypeAPI.create).toHaveBeenCalledWith(
        expect.objectContaining({ name: "Draft Type", active: false }),
      );
    });
  });

  it("edits an existing type's name and sort order", async () => {
    InventoryItemTypeAPI.update.mockResolvedValue({
      ...SEEDED_TYPES[0],
      name: "Reagent (renamed)",
    });
    renderPage();
    await screen.findByText("REAGENT");

    const row = screen.getByText("REAGENT").closest("tr");
    fireEvent.click(within(row).getByRole("button", { name: /edit/i }));

    const nameInput = await screen.findByDisplayValue("Reagent");
    fireEvent.change(nameInput, { target: { value: "Reagent (renamed)" } });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    await waitFor(() => {
      expect(InventoryItemTypeAPI.update).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ name: "Reagent (renamed)", locale: "en" }),
      );
    });
  });

  it("deactivates a type after confirming the warning modal", async () => {
    InventoryItemTypeAPI.deactivate.mockResolvedValue({
      ...SEEDED_TYPES[0],
      active: false,
    });
    renderPage();
    await screen.findByText("REAGENT");

    const row = screen.getByText("REAGENT").closest("tr");
    fireEvent.click(within(row).getByRole("button", { name: /edit/i }));

    fireEvent.click(await screen.findByRole("button", { name: /deactivate/i }));
    fireEvent.click(
      await screen.findByRole("button", { name: /yes, deactivate/i }),
    );

    await waitFor(() => {
      expect(InventoryItemTypeAPI.deactivate).toHaveBeenCalledWith(1);
    });
  });

  it("filters the table by search term", async () => {
    renderPage();
    await screen.findByText("REAGENT");

    const search = screen.getByPlaceholderText(/search by code or name/i);
    fireEvent.change(search, { target: { value: "RDT" } });

    expect(screen.queryByText("REAGENT")).not.toBeInTheDocument();
    expect(screen.getByText("RDT")).toBeInTheDocument();
  });
});
