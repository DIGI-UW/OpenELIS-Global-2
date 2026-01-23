import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import StorageDashboard from "../StorageDashboard";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  ...jest.requireActual("../../utils/Utils"),
  getFromOpenElisServer: jest.fn(),
}));

// Mock react-router-dom
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useHistory: () => ({
    push: jest.fn(),
    replace: jest.fn(),
  }),
  useLocation: () => ({
    pathname: "/Storage/samples",
  }),
}));

// Mock NotificationContext provider
const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: jest.fn(),
  addNotification: jest.fn(),
};

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <BrowserRouter>
        <NotificationContext.Provider value={mockNotificationContext}>
          {component}
        </NotificationContext.Provider>
      </BrowserRouter>
    </IntlProvider>,
  );
};

describe("StorageDashboard Search Functionality (FR-064, FR-064a)", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  // Test 1: Can we render the component at all?
  test("testComponent_Renders", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      // Mock all API calls to return empty arrays immediately
      if (url.includes("/rest/storage/sample-items")) {
        callback([]);
      } else if (url.includes("/rest/storage/rooms")) {
        callback([]);
      } else if (url.includes("/rest/storage/devices")) {
        callback([]);
      } else if (url.includes("/rest/storage/shelves")) {
        callback([]);
      } else if (url.includes("/rest/storage/racks")) {
        callback([]);
      } else if (url.includes("/dashboard/location-counts")) {
        callback({ rooms: 0, devices: 0, shelves: 0, racks: 0 });
      } else if (url.includes("/rest/displayList/sample-item-status-types")) {
        callback([
          { id: "", value: "All" },
          { id: "active", value: "Active" },
          { id: "disposed", value: "Disposed" },
        ]);
      } else if (url.includes("/rest/storage/sample-items?countOnly=true")) {
        callback({
          totalSampleItems: 0,
          active: 0,
          disposed: 0,
          storageLocations: 0,
        });
      } else {
        callback([]);
      }
    });

    renderWithIntl(<StorageDashboard />);

    // Wait for component to initialize
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100));
    });

    // Component should render without errors - API should be called
    expect(getFromOpenElisServer).toHaveBeenCalled();
  });

  // Test 2: Does the search input exist after rendering?
  test("testSearchInput_Exists", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/sample-items")) {
        callback([]);
      } else if (url.includes("/rest/storage/rooms")) {
        callback([]);
      } else if (url.includes("/rest/storage/devices")) {
        callback([]);
      } else if (url.includes("/rest/storage/shelves")) {
        callback([]);
      } else if (url.includes("/rest/storage/racks")) {
        callback([]);
      } else if (url.includes("/dashboard/location-counts")) {
        callback({ rooms: 0, devices: 0, shelves: 0, racks: 0 });
      } else if (url.includes("/rest/displayList/sample-item-status-types")) {
        callback([
          { id: "", value: "All" },
          { id: "active", value: "Active" },
          { id: "disposed", value: "Disposed" },
        ]);
      } else if (url.includes("/rest/storage/sample-items?countOnly=true")) {
        callback({
          totalSampleItems: 0,
          active: 0,
          disposed: 0,
          storageLocations: 0,
        });
      } else {
        callback([]);
      }
    });

    renderWithIntl(<StorageDashboard />);

    // Wait a bit for component to render
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 500));
    });

    // Check if search input exists
    const searchInput = screen.queryByTestId("sample-search-input");
    expect(searchInput).toBeTruthy();
  });

  // Test 3: Can we type in the search input?
  test("testSearchInput_AcceptsInput", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/sample-items")) {
        callback([]);
      } else if (url.includes("/rest/storage/rooms")) {
        callback([]);
      } else if (url.includes("/rest/storage/devices")) {
        callback([]);
      } else if (url.includes("/rest/storage/shelves")) {
        callback([]);
      } else if (url.includes("/rest/storage/racks")) {
        callback([]);
      } else if (url.includes("/dashboard/location-counts")) {
        callback({ rooms: 0, devices: 0, shelves: 0, racks: 0 });
      } else if (url.includes("/rest/displayList/sample-item-status-types")) {
        callback([
          { id: "", value: "All" },
          { id: "active", value: "Active" },
          { id: "disposed", value: "Disposed" },
        ]);
      } else if (url.includes("/rest/storage/sample-items?countOnly=true")) {
        callback({
          totalSampleItems: 0,
          active: 0,
          disposed: 0,
          storageLocations: 0,
        });
      } else {
        callback([]);
      }
    });

    renderWithIntl(<StorageDashboard />);

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 500));
    });

    const searchInput = screen.queryByTestId("sample-search-input");
    expect(searchInput).toBeTruthy();

    if (searchInput) {
      const inputElement = searchInput.querySelector("input") || searchInput;
      fireEvent.change(inputElement, { target: { value: "test" } });

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      expect(inputElement.value).toBe("test");
    }
  });

  // Test 4: Does search trigger API call after debounce?
  test("testSearch_Debounced", async () => {
    const searchCallback = jest.fn();

    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/sample-items/search")) {
        searchCallback();
        callback([]);
      } else if (url.includes("/rest/storage/sample-items")) {
        callback([]);
      } else if (url.includes("/rest/storage/rooms")) {
        callback([]);
      } else if (url.includes("/rest/storage/devices")) {
        callback([]);
      } else if (url.includes("/rest/storage/shelves")) {
        callback([]);
      } else if (url.includes("/rest/storage/racks")) {
        callback([]);
      } else if (url.includes("/dashboard/location-counts")) {
        callback({ rooms: 0, devices: 0, shelves: 0, racks: 0 });
      } else if (url.includes("/rest/displayList/sample-item-status-types")) {
        callback([
          { id: "", value: "All" },
          { id: "active", value: "Active" },
          { id: "disposed", value: "Disposed" },
        ]);
      } else if (url.includes("/rest/storage/sample-items?countOnly=true")) {
        callback({
          totalSampleItems: 0,
          active: 0,
          disposed: 0,
          storageLocations: 0,
        });
      } else {
        callback([]);
      }
    });

    renderWithIntl(<StorageDashboard />);

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 500));
    });

    const searchInput = screen.queryByTestId("sample-search-input");
    expect(searchInput).toBeTruthy();

    if (searchInput) {
      const inputElement = searchInput.querySelector("input") || searchInput;
      fireEvent.change(inputElement, { target: { value: "test" } });

      // Search should NOT be called immediately
      expect(searchCallback).not.toHaveBeenCalled();

      // Wait for debounce delay (400ms) + some buffer
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 500));
      });

      // Now search should be called
      expect(searchCallback).toHaveBeenCalled();
    }
  });
});
