import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import LabelPresetList from "./LabelPresetList";
import messages from "../../../languages/en.json";

// Mock the server utils
vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  patchToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

// Mock the layout NotificationContext using vi.importMock pattern
// vi.mock is hoisted so we can't use React.createContext inside the factory directly
vi.mock("../../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      addNotification: () => {},
      notificationVisible: false,
      setNotificationVisible: () => {},
    }),
  };
});

import {
  getFromOpenElisServer,
  patchToOpenElisServerFullResponse,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";

const mockPresets = [
  {
    id: 1,
    name: "standard order",
    barcodeType: "CODE_128",
    heightMm: 20,
    widthMm: 40,
    printsPerOrder: true,
    printsPerSample: false,
    defaultPerOrder: 1,
    maxPerOrder: 5,
    defaultPerSample: 0,
    maxPerSample: 10,
    isSystem: false,
    isActive: true,
    fields: [],
  },
  {
    id: 2,
    name: "system preset",
    barcodeType: "QR",
    heightMm: 30,
    widthMm: 60,
    printsPerOrder: false,
    printsPerSample: true,
    defaultPerOrder: 0,
    maxPerOrder: 10,
    defaultPerSample: 2,
    maxPerSample: 10,
    isSystem: true,
    isActive: true,
    fields: [],
  },
];

const renderWithProviders = (component, addNotification = vi.fn()) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ addNotification, notificationVisible: false }}
        >
          {component}
        </NotificationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("LabelPresetList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders the page heading", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([]);
    });
    renderWithProviders(<LabelPresetList />);
    // Use getAllByText since the title appears in both breadcrumb and heading
    const matches = screen.getAllByText(messages["admin.labelPresets.title"]);
    expect(matches.length).toBeGreaterThanOrEqual(1);
  });

  test("renders Add Preset button", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([]);
    });
    renderWithProviders(<LabelPresetList />);
    expect(screen.getByTestId("add-preset-btn")).toBeInTheDocument();
  });

  test("renders preset rows after loading", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockPresets);
    });
    renderWithProviders(<LabelPresetList />);

    await waitFor(() => {
      expect(screen.getByText("standard order")).toBeInTheDocument();
      expect(screen.getByText("system preset")).toBeInTheDocument();
    });
  });

  test("tags system preset with System badge", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockPresets);
    });
    renderWithProviders(<LabelPresetList />);

    await waitFor(() => {
      expect(
        screen.getByText(messages["admin.labelPresets.systemTag"]),
      ).toBeInTheDocument();
    });
  });

  test("shows active/inactive status tags", async () => {
    const presetsWithInactive = [
      ...mockPresets,
      {
        ...mockPresets[0],
        id: 3,
        name: "inactive preset",
        isActive: false,
        isSystem: false,
      },
    ];
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(presetsWithInactive);
    });
    renderWithProviders(<LabelPresetList />);

    await waitFor(() => {
      const activeLabels = screen.getAllByText(
        messages["admin.labelPresets.status.active"],
      );
      expect(activeLabels.length).toBeGreaterThanOrEqual(1);
      expect(
        screen.getByText(messages["admin.labelPresets.status.inactive"]),
      ).toBeInTheDocument();
    });
  });

  // ── Save feedback and status toggle (OGC-1227) ───────────────────────────

  test("shows a success toast and reloads after the editor saves a new preset", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockPresets);
    });
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => {
        callback({ status: 201 });
      },
    );
    const addNotification = vi.fn();
    renderWithProviders(<LabelPresetList />, addNotification);
    await waitFor(() => {
      expect(screen.getByText("standard order")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("add-preset-btn"));
    const nameInput = await screen.findByLabelText(
      messages["admin.labelPresets.field.name"],
    );
    fireEvent.change(nameInput, { target: { value: "Toast Preset" } });
    fireEvent.click(screen.getByText(messages["label.button.save"]));

    await waitFor(() => {
      expect(addNotification).toHaveBeenCalledWith({
        kind: "success",
        title: messages["admin.labelPresets.created"],
      });
    });
    const presetLoads = getFromOpenElisServer.mock.calls.filter(
      ([url]) => url === "/api/labelPresets",
    );
    expect(presetLoads).toHaveLength(2);
  });

  // ── Site-wide barcode settings (OGC-1217) ────────────────────────────────

  test("renders the site-wide barcode settings card above the preset table", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/api/siteSettings/barcode") {
        callback({
          prePrintUseAltAccession: true,
          prePrintAltAccessionPrefix: "ABCD",
        });
      } else {
        callback(mockPresets);
      }
    });
    renderWithProviders(<LabelPresetList />);

    const card = await screen.findByTestId("site-wide-barcode-settings");
    await waitFor(() => {
      expect(screen.getByText("standard order")).toBeInTheDocument();
    });
    const table = screen.getByRole("table");
    expect(
      card.compareDocumentPosition(table) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expect(
      within(card).getByLabelText(
        messages["admin.labelPresets.siteWide.prePrint.separate"],
      ),
    ).toBeChecked();
    expect(
      within(card).getByLabelText(
        messages["admin.labelPresets.siteWide.prefix.label"],
      ),
    ).toHaveValue("ABCD");
  });

  test("Deactivate sends a PATCH to the activate endpoint", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockPresets);
    });
    patchToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => {
        callback({ status: 200, ok: true });
      },
    );
    const addNotification = vi.fn();
    renderWithProviders(<LabelPresetList />, addNotification);
    await waitFor(() => {
      expect(screen.getByText("standard order")).toBeInTheDocument();
    });

    const row = screen.getByText("standard order").closest("tr");
    fireEvent.click(within(row).getByRole("button", { name: /options/i }));
    fireEvent.click(
      await screen.findByText(messages["admin.labelPresets.action.deactivate"]),
    );

    await waitFor(() => {
      expect(patchToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/api/labelPresets/1/activate",
        JSON.stringify({ isActive: false }),
        expect.any(Function),
      );
    });
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
    expect(addNotification).toHaveBeenCalledWith({
      kind: "success",
      title: messages["admin.labelPresets.deactivated"],
    });
  });

  test("loads presets from /api/labelPresets endpoint", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([]);
    });
    renderWithProviders(<LabelPresetList />);

    await waitFor(() => {
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/api/labelPresets",
        expect.any(Function),
      );
    });
  });
});
