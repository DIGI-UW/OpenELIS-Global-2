import React from "react";
import { render, screen, fireEvent, wait } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { IntlProvider } from "react-intl";
import TBSampleCreationPage from "./TBSampleCreationPage";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";

// Alias wait as waitFor for cleaner code (v9 compatibility)
const waitFor = wait;

// Mock the API utilities
jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
  postToOpenElisServerFormDataJson: jest.fn(),
}));

// Mock config
jest.mock("../../../../config.json", () => ({
  serverBaseUrl: "http://localhost:8080",
}));

// Mock TBManifestImportModal
jest.mock("../../workflow/TBManifestImportModal", () => {
  return function MockTBManifestImportModal({
    open,
    onClose,
    onImportSuccess,
  }) {
    if (!open) return null;
    return (
      <div data-testid="tb-manifest-import-modal">
        <button onClick={onClose} data-testid="close-modal">
          Close
        </button>
        <button
          onClick={() => onImportSuccess({ totalCreated: 5 })}
          data-testid="import-success"
        >
          Import Success
        </button>
      </div>
    );
  };
});

// Mock SampleGrid
jest.mock("../../workflow/SampleGrid", () => {
  return function MockSampleGrid({
    gridId,
    samples,
    selectedIds,
    onSelectionChange,
    showSelection,
    additionalColumns,
  }) {
    return (
      <div data-testid={`sample-grid-${gridId || "default"}`}>
        <div data-testid={`sample-count-${gridId || "default"}`}>
          {samples.length}
        </div>
        {samples.map((sample) => (
          <div key={sample.id} data-testid={`sample-${sample.id}`}>
            <span data-testid={`sample-${sample.id}-patientName`}>
              {sample.patientName || "-"}
            </span>
            <span data-testid={`sample-${sample.id}-patientId`}>
              {sample.patientId || "-"}
            </span>
            <span data-testid={`sample-${sample.id}-specimenType`}>
              {sample.specimenType || "-"}
            </span>
            <span data-testid={`sample-${sample.id}-status`}>
              {sample.status || "-"}
            </span>
            <span data-testid={`sample-${sample.id}-treatmentHistory`}>
              {sample.treatmentHistory || "-"}
            </span>
            <span data-testid={`sample-${sample.id}-requestedTests`}>
              {Array.isArray(sample.requestedTests)
                ? sample.requestedTests.join(", ")
                : sample.requestedTests || "-"}
            </span>
            {showSelection && selectedIds && onSelectionChange && (
              <input
                type="checkbox"
                data-testid={`sample-${sample.id}-checkbox`}
                checked={selectedIds.includes(sample.id)}
                onChange={(e) => {
                  if (e.target.checked) {
                    onSelectionChange([...selectedIds, sample.id]);
                  } else {
                    onSelectionChange(
                      selectedIds.filter((id) => id !== sample.id),
                    );
                  }
                }}
              />
            )}
          </div>
        ))}
      </div>
    );
  };
});

// Test wrapper with IntlProvider
const renderWithIntl = (component) => {
  return render(<IntlProvider locale="en">{component}</IntlProvider>);
};

describe("TBSampleCreationPage", () => {
  const mockPageData = { id: 53 };
  const mockEntryId = 2;

  // Mock sample data as returned by the API (before transformation)
  const mockApiResponse = [
    {
      id: "31",
      sampleItemId: "31",
      externalId: "TB-TB-001-001",
      accessionNumber: "DEV01250000000000030",
      typeOfSample: { description: "Sputum" },
      pageStatus: "PENDING",
      data: {
        patientName: "John Doe",
        patientId: "PAT-001",
        patientAge: "45",
        patientSex: "Male",
        specimenType: "Sputum",
        specimenQuality: "Good",
        documentNumber: "DOC-2025-001",
        referringFacility: "District Hospital A",
        requestedTests: "Culture, Smear Microscopy",
        receivedDate: "2025-01-15",
        receivedSite: "Central TB Lab",
      },
    },
    {
      id: "32",
      sampleItemId: "32",
      externalId: "TB-TB-002-001",
      accessionNumber: "DEV01250000000000031",
      typeOfSample: { description: "Sputum" },
      pageStatus: "PENDING",
      data: {
        patientName: "Jane Smith",
        patientId: "PAT-002",
        patientAge: "32",
        patientSex: "Female",
        specimenType: "Sputum",
        specimenQuality: "Good",
        documentNumber: "DOC-2025-002",
        referringFacility: "District Hospital A",
        requestedTests: "Culture, Smear Microscopy",
        receivedDate: "2025-01-15",
        receivedSite: "Central TB Lab",
      },
    },
    {
      id: "33",
      sampleItemId: "33",
      externalId: "TB-TB-003-001",
      accessionNumber: "DEV01250000000000032",
      typeOfSample: { description: "Fluid" },
      pageStatus: "COMPLETED",
      data: {
        patientName: "Robert Johnson",
        patientId: "PAT-003",
        patientAge: "58",
        patientSex: "Male",
        specimenType: "Fluid",
        specimenQuality: "Adequate",
        documentNumber: "DOC-2025-003",
        referringFacility: "Regional Hospital B",
        requestedTests: "Culture",
        receivedDate: "2025-01-15",
        receivedSite: "Central TB Lab",
      },
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();

    // Default mock implementations
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/user-sample-types")) {
        callback([{ id: "1", description: "Sputum" }]);
      } else if (url.includes("/rest/notebook/page/")) {
        callback(mockApiResponse);
      }
    });
  });

  describe("Initial Rendering", () => {
    test("renders page title and description", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByText(/Sample Accession & Registration/i),
        ).toBeInTheDocument();
      });
    });

    test("renders Import from Manifest button", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText(/Import from Manifest/i)).toBeInTheDocument();
      });
    });

    test("renders progress tiles with correct counts", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // Pending: 2 samples, Completed: 1 sample
        expect(
          screen.getByTestId("sample-count-pending-samples"),
        ).toHaveTextContent("2");
        expect(
          screen.getByTestId("sample-count-completed-samples"),
        ).toHaveTextContent("1");
      });
    });
  });

  describe("Sample Loading", () => {
    test("loads samples from API on mount", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(getFromOpenElisServer).toHaveBeenCalledWith(
          `/rest/notebook/page/${mockPageData.id}/samples`,
          expect.any(Function),
        );
      });
    });

    test("does not load samples when pageData.id is missing", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={{ id: null }}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // Should only call for sample types, not for page samples
        const pageSamplesCalls = getFromOpenElisServer.mock.calls.filter(
          (call) => call[0].includes("/rest/notebook/page/"),
        );
        expect(pageSamplesCalls.length).toBe(0);
      });
    });

    test("does not load samples when pageData.id starts with 'default-'", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={{ id: "default-123" }}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        const pageSamplesCalls = getFromOpenElisServer.mock.calls.filter(
          (call) => call[0].includes("/rest/notebook/page/"),
        );
        expect(pageSamplesCalls.length).toBe(0);
      });
    });
  });

  describe("Sample Data Transformation", () => {
    test("extracts patientName from sample.data object", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(screen.getByTestId("sample-31-patientName")).toHaveTextContent(
          "John Doe",
        );
        expect(screen.getByTestId("sample-32-patientName")).toHaveTextContent(
          "Jane Smith",
        );
        expect(screen.getByTestId("sample-33-patientName")).toHaveTextContent(
          "Robert Johnson",
        );
      });
    });

    test("extracts patientId from sample.data object", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(screen.getByTestId("sample-31-patientId")).toHaveTextContent(
          "PAT-001",
        );
        expect(screen.getByTestId("sample-32-patientId")).toHaveTextContent(
          "PAT-002",
        );
        expect(screen.getByTestId("sample-33-patientId")).toHaveTextContent(
          "PAT-003",
        );
      });
    });

    test("extracts specimenType from sample.data object", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(screen.getByTestId("sample-31-specimenType")).toHaveTextContent(
          "Sputum",
        );
        expect(screen.getByTestId("sample-33-specimenType")).toHaveTextContent(
          "Fluid",
        );
      });
    });

    test("handles missing data object gracefully", async () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/notebook/page/")) {
          callback([
            {
              id: "99",
              sampleItemId: "99",
              externalId: "TB-TB-099-001",
              accessionNumber: "DEV01250000000000099",
              pageStatus: "PENDING",
              // No data object
            },
          ]);
        } else {
          callback([]);
        }
      });

      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(screen.getByTestId("sample-99-patientName")).toHaveTextContent(
          "-",
        );
        expect(screen.getByTestId("sample-99-patientId")).toHaveTextContent(
          "-",
        );
      });
    });
  });

  describe("Import Modal", () => {
    test("opens import modal when Import from Manifest button is clicked", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-grid-pending-samples"),
        ).toBeInTheDocument();
      });

      const importButton = screen.getByText(/Import from Manifest/i);
      fireEvent.click(importButton);

      await waitFor(() => {
        expect(
          screen.getByTestId("tb-manifest-import-modal"),
        ).toBeInTheDocument();
      });
    });

    test("closes import modal when close button is clicked", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-grid-pending-samples"),
        ).toBeInTheDocument();
      });

      // Open modal
      fireEvent.click(screen.getByText(/Import from Manifest/i));

      await waitFor(() => {
        expect(
          screen.getByTestId("tb-manifest-import-modal"),
        ).toBeInTheDocument();
      });

      // Close modal
      fireEvent.click(screen.getByTestId("close-modal"));

      await waitFor(() => {
        expect(
          screen.queryByTestId("tb-manifest-import-modal"),
        ).not.toBeInTheDocument();
      });
    });

    test("reloads samples after successful import", async () => {
      const mockOnProgressUpdate = jest.fn();

      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={mockOnProgressUpdate}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-grid-pending-samples"),
        ).toBeInTheDocument();
      });

      // Clear mocks to track new calls
      getFromOpenElisServer.mockClear();

      // Open modal and trigger success
      fireEvent.click(screen.getByText(/Import from Manifest/i));

      await waitFor(() => {
        expect(
          screen.getByTestId("tb-manifest-import-modal"),
        ).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId("import-success"));

      await waitFor(() => {
        // Should reload samples
        expect(getFromOpenElisServer).toHaveBeenCalledWith(
          `/rest/notebook/page/${mockPageData.id}/samples`,
          expect.any(Function),
        );
        // Should call progress update
        expect(mockOnProgressUpdate).toHaveBeenCalled();
      });
    });
  });

  describe("Bulk Mark as Verified", () => {
    test("shows Mark as Verified button when samples are selected", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-grid-pending-samples"),
        ).toBeInTheDocument();
      });

      // Select a sample
      fireEvent.click(screen.getByTestId("sample-31-checkbox"));

      await waitFor(() => {
        expect(
          screen.getByTestId("mark-as-verified-button"),
        ).toBeInTheDocument();
      });
    });

    test("calls API to update status when Mark as Verified is clicked", async () => {
      postToOpenElisServer.mockImplementation((url, data, callback) => {
        callback(200);
      });

      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-grid-pending-samples"),
        ).toBeInTheDocument();
      });

      // Select a sample
      fireEvent.click(screen.getByTestId("sample-31-checkbox"));

      await waitFor(() => {
        expect(
          screen.getByTestId("mark-as-verified-button"),
        ).toBeInTheDocument();
      });

      // Click Mark as Verified button
      fireEvent.click(screen.getByTestId("mark-as-verified-button"));

      await waitFor(() => {
        expect(postToOpenElisServer).toHaveBeenCalledWith(
          `/rest/notebook/bulk/page/${mockPageData.id}/samples/status`,
          JSON.stringify({
            sampleIds: [31],
            status: "COMPLETED",
          }),
          expect.any(Function),
        );
      });
    });
  });

  describe("Empty State", () => {
    test("shows empty state message when no samples exist", async () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/notebook/page/")) {
          callback([]);
        } else {
          callback([]);
        }
      });

      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByText(/No samples have been added yet/i),
        ).toBeInTheDocument();
      });
    });
  });

  describe("Two-Section Layout (Pending/Completed)", () => {
    test("renders separate Pending and Completed sections", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // Should have both pending and completed sample grids
        expect(
          screen.getByTestId("sample-grid-pending-samples"),
        ).toBeInTheDocument();
        expect(
          screen.getByTestId("sample-grid-completed-samples"),
        ).toBeInTheDocument();
      });
    });

    test("filters pending samples into pending section", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // Pending section should have 2 samples (samples 31 and 32 are PENDING)
        expect(
          screen.getByTestId("sample-count-pending-samples"),
        ).toHaveTextContent("2");
      });
    });

    test("filters completed samples into completed section", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // Completed section should have 1 sample (sample 33 is COMPLETED)
        expect(
          screen.getByTestId("sample-count-completed-samples"),
        ).toHaveTextContent("1");
      });
    });

    test("shows selection checkboxes only in pending section", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-grid-pending-samples"),
        ).toBeInTheDocument();
      });

      // Pending samples should have checkboxes
      expect(screen.getByTestId("sample-31-checkbox")).toBeInTheDocument();
      expect(screen.getByTestId("sample-32-checkbox")).toBeInTheDocument();

      // Completed sample should NOT have checkbox
      expect(
        screen.queryByTestId("sample-33-checkbox"),
      ).not.toBeInTheDocument();
    });
  });

  describe("Treatment History Display", () => {
    test("displays treatment history from sample data", async () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/notebook/page/")) {
          callback([
            {
              id: "41",
              sampleItemId: "41",
              pageStatus: "PENDING",
              data: {
                treatmentHistory: "New",
              },
            },
            {
              id: "42",
              sampleItemId: "42",
              pageStatus: "PENDING",
              data: {
                treatmentHistory: "Retreatment",
              },
            },
            {
              id: "43",
              sampleItemId: "43",
              pageStatus: "PENDING",
              data: {
                treatmentHistory: "MDR-TB",
              },
            },
          ]);
        } else {
          callback([]);
        }
      });

      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-41-treatmentHistory"),
        ).toHaveTextContent("New");
        expect(
          screen.getByTestId("sample-42-treatmentHistory"),
        ).toHaveTextContent("Retreatment");
        expect(
          screen.getByTestId("sample-43-treatmentHistory"),
        ).toHaveTextContent("MDR-TB");
      });
    });
  });

  describe("Requested Tests Display", () => {
    test("displays requested tests from sample data", async () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/notebook/page/")) {
          callback([
            {
              id: "51",
              sampleItemId: "51",
              pageStatus: "PENDING",
              data: {
                requestedTests: ["Culture", "Smear Microscopy"],
              },
            },
          ]);
        } else {
          callback([]);
        }
      });

      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-51-requestedTests"),
        ).toHaveTextContent("Culture, Smear Microscopy");
      });
    });

    test("handles string format for requested tests", async () => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/notebook/page/")) {
          callback([
            {
              id: "52",
              sampleItemId: "52",
              pageStatus: "PENDING",
              data: {
                requestedTests: "Culture, Smear Microscopy, GeneXpert",
              },
            },
          ]);
        } else {
          callback([]);
        }
      });

      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId("sample-52-requestedTests"),
        ).toHaveTextContent("Culture, Smear Microscopy, GeneXpert");
      });
    });
  });

  describe("Progress Tiles", () => {
    test("displays correct pending count - verified via section grid", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // The pending section should have exactly 2 samples
        // This is the authoritative source for pending count
        expect(
          screen.getByTestId("sample-count-pending-samples"),
        ).toHaveTextContent("2");
      });
    });

    test("displays correct verified count - verified via section grid", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // The completed section should have exactly 1 sample
        // This is the authoritative source for verified count
        expect(
          screen.getByTestId("sample-count-completed-samples"),
        ).toHaveTextContent("1");
      });
    });

    test("displays correct total sample count - sum of sections", async () => {
      renderWithIntl(
        <TBSampleCreationPage
          entryId={mockEntryId}
          pageData={mockPageData}
          onProgressUpdate={jest.fn()}
        />,
      );

      await waitFor(() => {
        // Total = pending (2) + completed (1) = 3
        const pendingCount = parseInt(
          screen.getByTestId("sample-count-pending-samples").textContent,
          10,
        );
        const completedCount = parseInt(
          screen.getByTestId("sample-count-completed-samples").textContent,
          10,
        );
        expect(pendingCount + completedCount).toBe(3);
      });
    });
  });
});
