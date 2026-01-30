import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationAutocomplete from "./LocationAutocomplete";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("LocationAutocomplete", () => {
  const mockSearchResults = [
    {
      id: "1",
      name: "Main Laboratory",
      code: "MAIN",
      type: "room",
      hierarchical_path: "Main Laboratory",
      active: true,
    },
    {
      id: "10",
      name: "Freezer Unit 1",
      code: "FRZ01",
      type: "device",
      hierarchical_path: "Main Laboratory > Freezer Unit 1",
      active: true,
    },
    {
      id: "20",
      label: "Shelf-A",
      type: "shelf",
      hierarchical_path: "Main Laboratory > Freezer Unit 1 > Shelf-A",
      active: true,
    },
    {
      id: "30",
      label: "Rack R1",
      type: "rack",
      hierarchical_path: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1",
      active: true,
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T062i2: Test search matches location names and codes
   * Autocomplete should match both name and code fields
   */
  test("testSearchMatchesLocationNamesCodes", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/locations/search")) {
        callback(mockSearchResults);
      }
    });

    const onLocationSelect = jest.fn();
    const { rerender } = renderWithIntl(
      <LocationAutocomplete
        onLocationSelect={onLocationSelect}
        searchTerm="Freezer"
      />,
    );

    // Wait for search results to appear
    const freezerElements = await screen.findAllByText(/Freezer Unit 1/i);
    expect(freezerElements.length).toBeGreaterThan(0);

    // Search by code - rerender with new searchTerm
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <LocationAutocomplete
          onLocationSelect={onLocationSelect}
          searchTerm="FRZ01"
        />
      </IntlProvider>,
    );

    const freezerElements2 = await screen.findAllByText(/Freezer Unit 1/i);
    expect(freezerElements2.length).toBeGreaterThan(0);
  });

  /**
   * T062i2: Test displays full path in results
   * Each result should show full hierarchical path
   */
  test("testDisplaysFullPathInResults", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/locations/search")) {
        callback(mockSearchResults);
      }
    });

    const onLocationSelect = jest.fn();
    renderWithIntl(
      <LocationAutocomplete
        onLocationSelect={onLocationSelect}
        searchTerm="Shelf"
      />,
    );

    // Verify full path is displayed (may appear multiple times)
    const shelfPaths = await screen.findAllByText(
      /Main Laboratory > Freezer Unit 1 > Shelf-A/i,
    );
    expect(shelfPaths.length).toBeGreaterThan(0);
  });

  /**
   * T062i2: Test filters by search term
   * Results should be filtered based on search input
   */
  test("testFiltersBySearchTerm", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/locations/search")) {
        callback(mockSearchResults);
      }
    });

    const onLocationSelect = jest.fn();
    renderWithIntl(
      <LocationAutocomplete
        onLocationSelect={onLocationSelect}
        searchTerm="Main"
      />,
    );

    // Wait a bit for debounced search to trigger
    await new Promise((resolve) => setTimeout(resolve, 400));
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("q=Main"),
      expect.any(Function),
      expect.any(Function),
    );
  });
});
