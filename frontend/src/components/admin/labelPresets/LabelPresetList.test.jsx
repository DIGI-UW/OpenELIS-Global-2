import React from "react";
import { render, screen } from "@testing-library/react";
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

import { getFromOpenElisServer } from "../../utils/Utils";
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

const renderWithProviders = (component) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ addNotification: vi.fn(), notificationVisible: false }}
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
