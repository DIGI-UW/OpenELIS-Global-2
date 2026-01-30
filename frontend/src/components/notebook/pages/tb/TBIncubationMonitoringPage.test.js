import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { IntlProvider } from "react-intl";
import TBIncubationMonitoringPage from "./TBIncubationMonitoringPage";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";

// Mock the API utilities
jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
}));

// Mock config
jest.mock("../../../../config.json", () => ({
  serverBaseUrl: "http://localhost:8080",
}));

// Mock RecordReadingModal
jest.mock("./components/RecordReadingModal", () => {
  return function MockRecordReadingModal({ open, onClose, onSave, sample }) {
    if (!open) return null;
    return (
      <div data-testid="record-reading-modal">
        <span data-testid="modal-sample-id">{sample?.accessionNumber}</span>
        <button onClick={onClose} data-testid="close-modal">
          Close
        </button>
        <button
          onClick={() =>
            onSave({
              cultureReadingId: sample?.id,
              weekNumber: 3,
              observation: "NO_GROWTH",
              notes: "Test note",
            })
          }
          data-testid="save-reading"
        >
          Save
        </button>
      </div>
    );
  };
});

// Mock WeeklyReadingTable
jest.mock("./components/WeeklyReadingTable", () => {
  return function MockWeeklyReadingTable({
    readings,
    currentWeek,
    onAddReading,
    onMarkPositive,
    onMarkNegative,
    cultureResult,
  }) {
    return (
      <div data-testid="weekly-reading-table">
        <span data-testid="readings-count">{readings?.length || 0}</span>
        <span data-testid="current-week">{currentWeek}</span>
        {cultureResult && (
          <span data-testid="culture-result">{cultureResult}</span>
        )}
        <button onClick={onAddReading} data-testid="add-reading-btn">
          Add Reading
        </button>
        <button onClick={onMarkPositive} data-testid="mark-positive-btn">
          Mark Positive
        </button>
        <button onClick={onMarkNegative} data-testid="mark-negative-btn">
          Mark Negative
        </button>
      </div>
    );
  };
});

// Test wrapper with IntlProvider
const renderWithIntl = (component) => {
  return render(<IntlProvider locale="en">{component}</IntlProvider>);
};

describe("TBIncubationMonitoringPage", () => {
  const mockEntryId = 2;
  const mockPageData = { id: 55 };

  // Mock incubating samples data
  const mockIncubatingSamples = [
    {
      id: 101,
      sampleItemId: "101",
      sampleItem: {
        id: "101",
        sample: { accessionNumber: "TB-001/25" },
      },
      accessionNumber: "TB-001/25",
      cultureMethod: "MGIT",
      inoculationDate: "2025-01-10",
      weekNumber: 3,
      growthObservation: "NO_GROWTH",
      cultureResult: null,
    },
    {
      id: 102,
      sampleItemId: "102",
      sampleItem: {
        id: "102",
        sample: { accessionNumber: "TB-002/25" },
      },
      accessionNumber: "TB-002/25",
      cultureMethod: "LJ",
      inoculationDate: "2025-01-05",
      weekNumber: 5,
      growthObservation: "GROWTH_DETECTED",
      cultureResult: "POSITIVE",
      positiveWeek: 5,
    },
  ];

  // Mock summary data
  const mockSummary = {
    totalIncubating: 10,
    week1to4: 6,
    week5to8: 4,
    positive: 3,
    negative: 2,
  };

  beforeEach(() => {
    jest.clearAllMocks();

    // Default mock implementations - synchronous callbacks
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/tb/incubation/samples")) {
        if (url.includes("/readings")) {
          callback([
            {
              id: 1,
              weekNumber: 1,
              growthObservation: "NO_GROWTH",
              readBy: "JD",
            },
            {
              id: 2,
              weekNumber: 2,
              growthObservation: "NO_GROWTH",
              readBy: "JD",
            },
          ]);
        } else {
          callback(mockIncubatingSamples);
        }
      } else if (url.includes("/rest/tb/incubation/summary")) {
        callback(mockSummary);
      }
    });

    postToOpenElisServer.mockImplementation((url, data, callback) => {
      callback({ id: 1, message: "Success" });
    });
  });

  describe("Initial Rendering", () => {
    test("renders page title", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      expect(screen.getByText(/TB Incubation Monitoring/i)).toBeInTheDocument();
    });

    test("renders summary tile labels", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      expect(screen.getByText(/Total Incubating/i)).toBeInTheDocument();
      expect(screen.getByText(/Week 1-4/i)).toBeInTheDocument();
      // Week 5-8 may appear in both summary tile and table
      expect(screen.getAllByText(/Week 5-8/i).length).toBeGreaterThan(0);
    });

    test("renders DataTable headers", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      // Should show table headers
      expect(screen.getByText("Sample ID")).toBeInTheDocument();
      expect(screen.getByText("Method")).toBeInTheDocument();
      expect(screen.getByText("Week")).toBeInTheDocument();
      expect(screen.getByText("Status")).toBeInTheDocument();
    });
  });

  describe("API Calls", () => {
    test("fetches incubating samples on mount", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/tb/incubation/samples",
        expect.any(Function),
      );
    });

    test("fetches summary on mount", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/tb/incubation/summary",
        expect.any(Function),
      );
    });
  });

  describe("Summary Tiles Values", () => {
    test("displays summary tile values for total incubating", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      // Check that the main summary value is displayed
      expect(screen.getByText("10")).toBeInTheDocument(); // totalIncubating
      expect(screen.getByText("6")).toBeInTheDocument(); // week1to4
      expect(screen.getByText("4")).toBeInTheDocument(); // week5to8
    });
  });

  describe("Search Functionality", () => {
    test("renders search input", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      const searchInput = screen.getByPlaceholderText(/Search samples/i);
      expect(searchInput).toBeInTheDocument();
    });
  });

  describe("Refresh Button", () => {
    test("renders refresh button", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      expect(screen.getByText(/Refresh/i)).toBeInTheDocument();
    });

    test("reloads data when refresh is clicked", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      expect(screen.getByText(/Refresh/i)).toBeInTheDocument();

      // Clear mock calls
      getFromOpenElisServer.mockClear();

      // Click refresh
      fireEvent.click(screen.getByText(/Refresh/i));

      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/tb/incubation/samples",
        expect.any(Function),
      );
    });
  });

  describe("Empty State", () => {
    test("shows empty message when no samples", () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/tb/incubation/samples")) {
          callback([]);
        } else if (url.includes("/rest/tb/incubation/summary")) {
          callback({
            totalIncubating: 0,
            week1to4: 0,
            week5to8: 0,
            positive: 0,
            negative: 0,
          });
        }
      });

      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      expect(
        screen.getByText(/No samples currently incubating/i),
      ).toBeInTheDocument();
    });
  });

  describe("Data Table Content", () => {
    test("displays sample data in table when loaded", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      // Check that accession numbers appear
      expect(screen.getByText("TB-001/25")).toBeInTheDocument();
      expect(screen.getByText("TB-002/25")).toBeInTheDocument();

      // Check culture methods
      expect(screen.getByText("MGIT")).toBeInTheDocument();
      expect(screen.getByText("LJ")).toBeInTheDocument();
    });

    test("displays different data when API returns different samples", () => {
      // Override mock with different data
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/tb/incubation/samples")) {
          callback([
            {
              id: 999,
              sampleItemId: "999",
              sampleItem: {
                id: "999",
                sample: { accessionNumber: "DIFFERENT-SAMPLE-ID" },
              },
              accessionNumber: "DIFFERENT-SAMPLE-ID",
              cultureMethod: "CUSTOM-METHOD",
              inoculationDate: "2025-02-15",
              weekNumber: 7,
              growthObservation: "CONTAMINATED",
              cultureResult: null,
            },
          ]);
        } else if (url.includes("/rest/tb/incubation/summary")) {
          callback({
            totalIncubating: 1,
            week1to4: 0,
            week5to8: 1,
            positive: 0,
            negative: 0,
          });
        }
      });

      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      // This sample should appear, proving data flows from API to UI
      expect(screen.getByText("DIFFERENT-SAMPLE-ID")).toBeInTheDocument();
      expect(screen.getByText("CUSTOM-METHOD")).toBeInTheDocument();

      // Original samples should NOT appear
      expect(screen.queryByText("TB-001/25")).not.toBeInTheDocument();
      expect(screen.queryByText("MGIT")).not.toBeInTheDocument();
    });

    test("summary values update based on API response", () => {
      // Override mock with specific summary values
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/tb/incubation/samples")) {
          callback([]);
        } else if (url.includes("/rest/tb/incubation/summary")) {
          callback({
            totalIncubating: 99,
            week1to4: 55,
            week5to8: 44,
            positive: 11,
            negative: 22,
          });
        }
      });

      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      // Verify the specific values from our mock appear
      expect(screen.getByText("99")).toBeInTheDocument();
      expect(screen.getByText("55")).toBeInTheDocument();
      expect(screen.getByText("44")).toBeInTheDocument();

      // Original values should NOT appear
      expect(screen.queryByText("10")).not.toBeInTheDocument();
    });
  });

  describe("API Error Handling", () => {
    test("handles null API response gracefully", () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        callback(null);
      });

      // Should not throw
      expect(() => {
        renderWithIntl(
          <TBIncubationMonitoringPage
            entryId={mockEntryId}
            pageData={mockPageData}
            onProgressUpdate={jest.fn()}
          />,
        );
      }).not.toThrow();
    });

    test("handles undefined API response gracefully", () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        callback(undefined);
      });

      // Should not throw
      expect(() => {
        renderWithIntl(
          <TBIncubationMonitoringPage
            entryId={mockEntryId}
            pageData={mockPageData}
            onProgressUpdate={jest.fn()}
          />,
        );
      }).not.toThrow();
    });
  });

  describe("Correct API Endpoints", () => {
    test("calls correct endpoint for samples", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      // Verify exact endpoint - would fail if someone changes the URL
      const samplesCalls = getFromOpenElisServer.mock.calls.filter((call) =>
        call[0].includes("/rest/tb/incubation/samples"),
      );
      expect(samplesCalls.length).toBeGreaterThan(0);
      expect(samplesCalls[0][0]).toBe("/rest/tb/incubation/samples");
    });

    test("calls correct endpoint for summary", () => {
      renderWithIntl(
        <TBIncubationMonitoringPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      // Verify exact endpoint
      const summaryCalls = getFromOpenElisServer.mock.calls.filter((call) =>
        call[0].includes("/rest/tb/incubation/summary"),
      );
      expect(summaryCalls.length).toBeGreaterThan(0);
      expect(summaryCalls[0][0]).toBe("/rest/tb/incubation/summary");
    });
  });
});
