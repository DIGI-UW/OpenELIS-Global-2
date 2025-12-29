import React from "react";
import { render, screen, fireEvent, act, wait } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { IntlProvider } from "react-intl";
import TBManifestImportModal from "./TBManifestImportModal";

// Alias wait as waitFor for cleaner code (v9 compatibility)
const waitFor = wait;

// Helper to simulate file upload (v8 of user-event doesn't have upload)
const uploadFile = (input, file) => {
  Object.defineProperty(input, "files", {
    value: [file],
    writable: false,
  });
  fireEvent.change(input);
};

// Mock config
jest.mock("../../../config.json", () => ({
  serverBaseUrl: "http://localhost:8080",
}));

// Mock fetch
global.fetch = jest.fn();

// Mock localStorage
Object.defineProperty(window, "localStorage", {
  value: {
    getItem: jest.fn(() => "mock-csrf-token"),
    setItem: jest.fn(),
    clear: jest.fn(),
  },
});

// Mock messages for react-intl
const mockMessages = {
  "label.button.next": "Next",
  "label.button.back": "Back",
  "label.button.close": "Close",
  "label.select": "Select...",
  "notebook.tb.manifest.title": "Import TB Samples from Manifest",
  "notebook.manifest.preview": "Preview",
  "notebook.manifest.import": "Import",
  "notebook.manifest.dropzone":
    "Drag and drop a CSV file here or click to upload",
  "notebook.manifest.loading": "Loading...",
  "notebook.manifest.importing": "Creating samples...",
  "notebook.manifest.validationErrors": "Validation Errors",
  "notebook.manifest.preview.rows": "{count} rows",
  "notebook.manifest.preview.samples": "{count} samples to create",
  "notebook.manifest.success.title": "Import Successful",
  "notebook.manifest.success.message":
    "Samples have been created and linked to the notebook entry.",
  "notebook.tb.manifest.uploadDescription":
    "Upload a CSV file containing TB sample information with complete metadata including specimen details, patient information, requested tests, and receipt details.",
  "notebook.tb.manifest.step1":
    "Upload a CSV file with TB sample accession metadata. Review the expected data points below before uploading.",
  "notebook.tb.manifest.step2":
    "Map your CSV columns to the expected TB sample metadata fields. Required fields (marked with *) must be mapped.",
  "notebook.tb.manifest.requiredMappings": "Required Fields",
  "notebook.tb.manifest.error.invalidFileType": "Please upload a CSV file",
  "notebook.tb.manifest.section.sampleIdentity": "A. Sample Identity",
  "notebook.tb.manifest.section.specimenInfo": "B. Specimen Information",
  "notebook.tb.manifest.section.requestDetails": "C. Request Paper Details",
  "notebook.tb.manifest.section.patientMetadata":
    "D. Patient / Participant Metadata",
  "notebook.tb.manifest.section.clinicalContext": "E. Clinical Context",
  "notebook.tb.manifest.section.requestedTests": "F. Requested Tests",
  "notebook.tb.manifest.section.receiptDetails": "G. Receipt Details",
  "notebook.tb.manifest.section.common": "Common Fields",
  "notebook.tb.manifest.column.sampleId": "Sample ID",
  "notebook.tb.manifest.column.specimenType": "Specimen Type",
  "notebook.tb.manifest.column.specimenQuality": "Specimen Quality",
  "notebook.tb.manifest.column.documentNumber": "Document Number",
  "notebook.tb.manifest.column.referringFacility": "Referring Facility",
  "notebook.tb.manifest.column.patientName": "Patient Name",
  "notebook.tb.manifest.column.patientAge": "Patient Age",
  "notebook.tb.manifest.column.patientSex": "Patient Sex",
  "notebook.tb.manifest.column.patientId": "Patient ID",
  "notebook.tb.manifest.column.studyId": "Study ID",
  "notebook.tb.manifest.column.patientAddress": "Patient Address",
  "notebook.tb.manifest.column.patientPhone": "Patient Phone",
  "notebook.tb.manifest.column.physicianPhone": "Physician Phone",
  "notebook.tb.manifest.column.consentStatus": "Consent Status",
  "notebook.tb.manifest.column.treatmentHistory": "Treatment History",
  "notebook.tb.manifest.column.culture": "Culture",
  "notebook.tb.manifest.column.smearMicroscopy": "Smear Microscopy",
  "notebook.tb.manifest.column.genexpert": "GeneXpert",
  "notebook.tb.manifest.column.identification": "Identification",
  "notebook.tb.manifest.column.dstFirstLine": "DST First Line",
  "notebook.tb.manifest.column.dstSecondLine": "DST Second Line",
  "notebook.tb.manifest.column.intendedMethod": "Intended Method",
  "notebook.tb.manifest.column.receivedSite": "Received Site",
  "notebook.tb.manifest.column.receivedDate": "Received Date",
  "notebook.tb.manifest.column.receivedTime": "Received Time",
  "notebook.tb.manifest.column.numOfSamples": "Num Of Samples",
};

// Test wrapper
const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={mockMessages}>
      {component}
    </IntlProvider>,
  );
};

// Helper to create a File object
const createCsvFile = (content, filename = "test.csv") => {
  return new File([content], filename, { type: "text/csv" });
};

describe("TBManifestImportModal", () => {
  const mockOnClose = jest.fn();
  const mockOnImportSuccess = jest.fn();
  const mockEntryId = 2;

  const csvHeaders =
    "Sample ID,Specimen Type,Patient Name,Patient ID,Referring Facility,Number of Samples";
  const csvRow1 = "TB-001,Sputum,John Doe,PAT-001,District Hospital A,1";
  const csvRow2 = "TB-002,Sputum,Jane Smith,PAT-002,District Hospital A,2";
  const validCsvContent = `${csvHeaders}\n${csvRow1}\n${csvRow2}`;

  // Mock specimen types response
  const mockSpecimenTypes = [
    { id: "1", description: "Sputum" },
    { id: "2", description: "BAL" },
    { id: "3", description: "Pleural Fluid" },
    { id: "4", description: "CSF" },
  ];

  // Helper function to set up default mocks for specimen types
  const setupDefaultMocks = () => {
    global.fetch.mockImplementation((url) => {
      if (url && url.includes("/specimen-types")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockSpecimenTypes),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });
  };

  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch.mockReset();
    // Set up default mocks that can be overridden by individual tests
    setupDefaultMocks();
  });

  describe("Expected Data Points Accordion (Landing Page)", () => {
    test("renders Expected CSV Columns accordion on landing page", () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Should show the accordion with Expected CSV Columns
      expect(
        screen.getByText(/Expected CSV Columns & Data Points/i),
      ).toBeInTheDocument();
    });

    test("displays Required Fields section in accordion", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Expand the accordion
      const accordionButton = screen.getByText(
        /Expected CSV Columns & Data Points/i,
      );
      await act(async () => {
        fireEvent.click(accordionButton);
      });

      // Should show Required Fields section
      await waitFor(() => {
        expect(screen.getByText(/Required Fields/i)).toBeInTheDocument();
      });
    });

    test("displays Specimen Type validation info in accordion", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Expand the accordion
      const accordionButton = screen.getByText(
        /Expected CSV Columns & Data Points/i,
      );
      await act(async () => {
        fireEvent.click(accordionButton);
      });

      // Should show specimen type validation info
      await waitFor(() => {
        expect(
          screen.getByText(/Specimen Type \(Validated\)/i),
        ).toBeInTheDocument();
      });
    });

    test("displays Optional Fields section in accordion", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Expand the accordion
      const accordionButton = screen.getByText(
        /Expected CSV Columns & Data Points/i,
      );
      await act(async () => {
        fireEvent.click(accordionButton);
      });

      // Should show Optional Fields section
      await waitFor(() => {
        expect(screen.getByText(/Optional Fields/i)).toBeInTheDocument();
      });
    });

    test("loads and displays dynamic specimen types from backend", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Should have called the specimen types endpoint
      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          expect.stringContaining(`/specimen-types`),
          expect.any(Object),
        );
      });
    });
  });

  describe("Step 1: File Upload", () => {
    test("renders upload instructions when modal opens", () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      expect(
        screen.getByText(
          /Upload a CSV file with TB sample accession metadata/i,
        ),
      ).toBeInTheDocument();
      expect(screen.getByText(/Next/i)).toBeDisabled();
    });

    test("parses CSV headers and advances to step 2 after file upload", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      const file = createCsvFile(validCsvContent);

      // Find the file input (Carbon wraps it)
      const fileInput = document.querySelector('input[type="file"]');
      expect(fileInput).toBeTruthy();

      await act(async () => {
        uploadFile(fileInput, file);
      });

      // Should advance to step 2 (column mapping)
      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });
    });

    test("advances to column mapping step after successful upload", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      const file = createCsvFile(validCsvContent, "tb-samples.csv");
      const fileInput = document.querySelector('input[type="file"]');

      await act(async () => {
        uploadFile(fileInput, file);
      });

      // After upload, we should be on step 2 (column mapping)
      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      // Verify step 1 upload instructions are no longer visible
      expect(
        screen.queryByText(
          /Upload a CSV file with TB sample accession metadata/i,
        ),
      ).not.toBeInTheDocument();
    });
  });

  describe("Step 2: Column Mapping", () => {
    const setupColumnMappingStep = async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');

      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });
    };

    test("displays all TB-specific field sections", async () => {
      await setupColumnMappingStep();

      // Verify all section headers are present
      expect(screen.getByText(/A\. Sample Identity/i)).toBeInTheDocument();
      expect(screen.getByText(/B\. Specimen Information/i)).toBeInTheDocument();
      expect(
        screen.getByText(/C\. Request Paper Details/i),
      ).toBeInTheDocument();
      expect(
        screen.getByText(/D\. Patient \/ Participant Metadata/i),
      ).toBeInTheDocument();
      expect(screen.getByText(/E\. Clinical Context/i)).toBeInTheDocument();
      expect(screen.getByText(/F\. Requested Tests/i)).toBeInTheDocument();
      expect(screen.getByText(/G\. Receipt Details/i)).toBeInTheDocument();
    });

    test("disables Preview button until required columns are mapped", async () => {
      await setupColumnMappingStep();

      // Preview button should be disabled initially
      const previewButton = screen.getByText(/Preview/i);
      expect(previewButton).toBeDisabled();
    });

    test("enables Preview button when required columns are mapped", async () => {
      await setupColumnMappingStep();

      // Map required columns: specimenTypeColumn and numOfSamplesColumn
      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);
      const numOfSamplesSelect = screen.getByLabelText(/Num Of Samples/i);

      await act(async () => {
        fireEvent.change(specimenTypeSelect, {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(numOfSamplesSelect, {
          target: { value: "Number of Samples" },
        });
      });

      // Preview button should be enabled
      await waitFor(() => {
        const previewButton = screen.getByText(/Preview/i);
        expect(previewButton).not.toBeDisabled();
      });
    });

    test("populates select options with CSV headers", async () => {
      await setupColumnMappingStep();

      // Find a select dropdown and verify it has the CSV headers as options
      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);

      // The select should have options from CSV headers
      const options = specimenTypeSelect.querySelectorAll("option");
      const optionValues = Array.from(options).map((opt) => opt.value);

      expect(optionValues).toContain("Specimen Type");
      expect(optionValues).toContain("Patient Name");
      expect(optionValues).toContain("Number of Samples");
    });

    test("auto-maps columns based on CSV header names", async () => {
      // CSV with headers that match expected field names
      const autoMapCsvHeaders =
        "Sample ID,Specimen Type,Patient Name,Patient ID,Num Of Samples";
      const autoMapCsvRow = "TB-001,Sputum,John Doe,PAT-001,2";
      const autoMapCsvContent = `${autoMapCsvHeaders}\n${autoMapCsvRow}`;

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      const file = createCsvFile(autoMapCsvContent);
      const fileInput = document.querySelector('input[type="file"]');

      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      // Specimen Type should be auto-mapped based on header name match
      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);
      expect(specimenTypeSelect.value).toBe("Specimen Type");
    });
  });

  describe("Multiple Samples Per Row", () => {
    test("preview shows correct total sample count when numOfSamples > 1", async () => {
      // CSV where one row has numOfSamples = 3
      const mockPreviewResponse = {
        totalRows: 2,
        totalSamples: 4, // Row 1: 1 sample, Row 2: 3 samples
        validRows: 2,
        valid: true,
        errors: [],
        rows: [
          {
            rowNumber: 2,
            specimenType: "Sputum",
            patientName: "John Doe",
            numOfSamples: 1,
          },
          {
            rowNumber: 3,
            specimenType: "Sputum",
            patientName: "Jane Smith",
            numOfSamples: 3,
          },
        ],
      };

      global.fetch.mockImplementation((url) => {
        if (url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewResponse),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Upload file
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      // Map required columns
      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);
      const numOfSamplesSelect = screen.getByLabelText(/Num Of Samples/i);

      await act(async () => {
        fireEvent.change(specimenTypeSelect, {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(numOfSamplesSelect, {
          target: { value: "Number of Samples" },
        });
      });

      // Click Preview
      const previewButton = screen.getByText(/Preview/i);
      await act(async () => {
        fireEvent.click(previewButton);
      });

      // Should show 4 samples to create (not 2 rows)
      await waitFor(() => {
        expect(screen.getByText(/4 samples to create/i)).toBeInTheDocument();
      });
    });
  });

  describe("Step 3: Preview", () => {
    const mockPreviewResponse = {
      totalRows: 2,
      totalSamples: 3,
      validRows: 2,
      valid: true,
      errors: [],
      rows: [
        {
          rowNumber: 2,
          specimenType: "Sputum",
          patientName: "John Doe",
          patientId: "PAT-001",
          referringFacility: "District Hospital A",
          numOfSamples: 1,
        },
        {
          rowNumber: 3,
          specimenType: "Sputum",
          patientName: "Jane Smith",
          patientId: "PAT-002",
          referringFacility: "District Hospital A",
          numOfSamples: 2,
        },
      ],
    };

    const setupPreviewStep = async () => {
      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewResponse),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Upload file
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      // Map required columns
      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);
      const numOfSamplesSelect = screen.getByLabelText(/Num Of Samples/i);

      await act(async () => {
        fireEvent.change(specimenTypeSelect, {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(numOfSamplesSelect, {
          target: { value: "Number of Samples" },
        });
      });

      // Click Preview
      const previewButton = screen.getByText(/Preview/i);
      await act(async () => {
        fireEvent.click(previewButton);
      });

      return mockPreviewResponse;
    };

    test("sends preview request to correct endpoint", async () => {
      await setupPreviewStep();

      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          `http://localhost:8080/rest/notebook/tb/entry/${mockEntryId}/samples/preview-manifest`,
          expect.objectContaining({
            method: "POST",
            credentials: "include",
          }),
        );
      });
    });

    test("displays preview summary with row and sample counts", async () => {
      await setupPreviewStep();

      await waitFor(() => {
        expect(screen.getByText(/2 rows/i)).toBeInTheDocument();
        expect(screen.getByText(/3 samples to create/i)).toBeInTheDocument();
      });
    });

    test("enables Import button when preview has no errors", async () => {
      await setupPreviewStep();

      await waitFor(() => {
        // Verify we're on step 3 with valid preview
        expect(screen.getByText(/2 rows/i)).toBeInTheDocument();
        expect(screen.getByText(/3 samples to create/i)).toBeInTheDocument();
      });

      // Import button should be enabled (not disabled)
      const importButton = screen.getByRole("button", { name: /^Import$/i });
      expect(importButton).not.toBeDisabled();
    });

    test("displays validation errors when preview returns errors", async () => {
      const mockPreviewResponseWithErrors = {
        totalRows: 2,
        totalSamples: 0,
        validRows: 0,
        valid: false,
        errors: [
          {
            rowNumber: 2,
            column: "specimenType",
            message: "Unknown specimen type: InvalidType",
          },
        ],
        rows: [],
      };

      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewResponseWithErrors),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Upload and map columns
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);
      const numOfSamplesSelect = screen.getByLabelText(/Num Of Samples/i);

      await act(async () => {
        fireEvent.change(specimenTypeSelect, {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(numOfSamplesSelect, {
          target: { value: "Number of Samples" },
        });
      });

      const previewButton = screen.getByText(/Preview/i);
      await act(async () => {
        fireEvent.click(previewButton);
      });

      // Should display error message
      await waitFor(() => {
        expect(
          screen.getByText(/Unknown specimen type: InvalidType/i),
        ).toBeInTheDocument();
      });
    });

    test("displays department-based validation errors", async () => {
      const mockPreviewWithDeptErrors = {
        totalRows: 2,
        totalSamples: 0,
        validRows: 1,
        valid: false,
        errors: [
          {
            rowNumber: 3,
            column: "specimenType",
            message:
              "Specimen type 'Blood' is not allowed for the linked departments",
          },
        ],
        rows: [],
      };

      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewWithDeptErrors),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Upload file
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      // Map required columns
      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);
      const numOfSamplesSelect = screen.getByLabelText(/Num Of Samples/i);

      await act(async () => {
        fireEvent.change(specimenTypeSelect, {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(numOfSamplesSelect, {
          target: { value: "Number of Samples" },
        });
      });

      const previewButton = screen.getByText(/Preview/i);
      await act(async () => {
        fireEvent.click(previewButton);
      });

      // Should display department-based validation error
      await waitFor(() => {
        expect(
          screen.getByText(/not allowed for the linked departments/i),
        ).toBeInTheDocument();
      });
    });

    test("displays multiple validation errors from different rows", async () => {
      const mockPreviewWithMultipleErrors = {
        totalRows: 6,
        totalSamples: 0,
        validRows: 3,
        valid: false,
        errors: [
          {
            rowNumber: 3,
            column: "specimenType",
            message:
              "Specimen type 'Blood' is not allowed for the linked departments",
          },
          {
            rowNumber: 5,
            column: "specimenType",
            message:
              "Specimen type 'Urine' is not allowed for the linked departments",
          },
          {
            rowNumber: 7,
            column: "specimenType",
            message: "Unknown specimen type: Stool",
          },
        ],
        rows: [],
      };

      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewWithMultipleErrors),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Upload file
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      // Map required columns
      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      await act(async () => {
        fireEvent.change(screen.getByLabelText(/Specimen Type/i), {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(screen.getByLabelText(/Num Of Samples/i), {
          target: { value: "Number of Samples" },
        });
      });

      await act(async () => {
        fireEvent.click(screen.getByText(/Preview/i));
      });

      // Should display all three errors
      await waitFor(() => {
        expect(screen.getByText(/Blood.*not allowed/i)).toBeInTheDocument();
        expect(screen.getByText(/Urine.*not allowed/i)).toBeInTheDocument();
        expect(
          screen.getByText(/Unknown specimen type: Stool/i),
        ).toBeInTheDocument();
      });
    });

    test("disables Import button when validation errors exist", async () => {
      const mockPreviewWithErrors = {
        totalRows: 2,
        totalSamples: 0,
        validRows: 0,
        valid: false,
        errors: [
          {
            rowNumber: 2,
            column: "specimenType",
            message:
              "Specimen type 'Blood' is not allowed for the linked departments",
          },
        ],
        rows: [],
      };

      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewWithErrors),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Upload file
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      // Map required columns
      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      await act(async () => {
        fireEvent.change(screen.getByLabelText(/Specimen Type/i), {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(screen.getByLabelText(/Num Of Samples/i), {
          target: { value: "Number of Samples" },
        });
      });

      await act(async () => {
        fireEvent.click(screen.getByText(/Preview/i));
      });

      // Import button should be disabled when there are validation errors
      await waitFor(() => {
        const importButton = screen.getByRole("button", { name: /^Import$/i });
        expect(importButton).toBeDisabled();
      });
    });
  });

  describe("Step 4: Import Success", () => {
    test("calls onImportSuccess after successful import", async () => {
      const mockPreviewResponseLocal = {
        totalRows: 2,
        totalSamples: 3,
        valid: true,
        errors: [],
        rows: [
          {
            rowNumber: 2,
            specimenType: "Sputum",
            patientName: "John Doe",
            patientId: "PAT-001",
            numOfSamples: 1,
          },
        ],
      };

      const mockImportResponse = {
        success: true,
        totalRequested: 3,
        totalCreated: 3,
        createdAccessionNumbers: ["DEV001", "DEV002", "DEV003"],
        errors: [],
      };

      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewResponseLocal),
          });
        }
        if (url && url.includes("/create-from-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockImportResponse),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Upload file and map columns
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() => {
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
      });

      const specimenTypeSelect = screen.getByLabelText(/Specimen Type/i);
      const numOfSamplesSelect = screen.getByLabelText(/Num Of Samples/i);

      await act(async () => {
        fireEvent.change(specimenTypeSelect, {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(numOfSamplesSelect, {
          target: { value: "Number of Samples" },
        });
      });

      // Preview
      await act(async () => {
        fireEvent.click(screen.getByText(/Preview/i));
      });

      await waitFor(() => {
        expect(screen.getByText(/2 rows/i)).toBeInTheDocument();
      });

      // Import - use getByRole to get the button specifically
      const importButton = screen.getByRole("button", { name: /^Import$/i });
      await act(async () => {
        fireEvent.click(importButton);
      });

      // Verify success callback
      await waitFor(() => {
        expect(mockOnImportSuccess).toHaveBeenCalledWith(mockImportResponse);
      });

      // Verify success message
      await waitFor(() => {
        expect(screen.getByText(/Import Successful/i)).toBeInTheDocument();
      });
    });

    test("sends import request to correct endpoint", async () => {
      const mockPreviewResponseLocal = {
        totalRows: 1,
        totalSamples: 1,
        valid: true,
        errors: [],
        rows: [{ rowNumber: 2, specimenType: "Sputum", numOfSamples: 1 }],
      };

      const mockImportResponse = {
        success: true,
        totalCreated: 1,
        errors: [],
      };

      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviewResponseLocal),
          });
        }
        if (url && url.includes("/create-from-manifest")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockImportResponse),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Quick setup
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() =>
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument(),
      );

      await act(async () => {
        fireEvent.change(screen.getByLabelText(/Specimen Type/i), {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(screen.getByLabelText(/Num Of Samples/i), {
          target: { value: "Number of Samples" },
        });
      });

      await act(async () => {
        fireEvent.click(screen.getByText(/Preview/i));
      });

      await waitFor(() =>
        expect(screen.getByText(/1 rows/i)).toBeInTheDocument(),
      );

      await act(async () => {
        // Use getByRole to specifically get the button, not the modal title
        const importButton = screen.getByRole("button", { name: /^Import$/i });
        fireEvent.click(importButton);
      });

      await waitFor(() => {
        expect(global.fetch).toHaveBeenLastCalledWith(
          `http://localhost:8080/rest/notebook/tb/entry/${mockEntryId}/samples/create-from-manifest`,
          expect.objectContaining({
            method: "POST",
            credentials: "include",
          }),
        );
      });
    });
  });

  describe("Error Handling", () => {
    test("handles network error during preview", async () => {
      // Mock specimen types success, but preview fails
      global.fetch.mockImplementation((url) => {
        if (url && url.includes("/specimen-types")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSpecimenTypes),
          });
        }
        if (url && url.includes("/preview-manifest")) {
          return Promise.reject(new Error("Network error"));
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({}),
        });
      });

      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() =>
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument(),
      );

      await act(async () => {
        fireEvent.change(screen.getByLabelText(/Specimen Type/i), {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(screen.getByLabelText(/Num Of Samples/i), {
          target: { value: "Number of Samples" },
        });
      });

      await act(async () => {
        fireEvent.click(screen.getByText(/Preview/i));
      });

      await waitFor(() => {
        expect(screen.getByText(/Network error/i)).toBeInTheDocument();
      });
    });

    test("does not call preview API when entryId is missing", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={null}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() =>
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument(),
      );

      // Reset fetch mock to track only preview calls
      global.fetch.mockClear();

      await act(async () => {
        fireEvent.change(screen.getByLabelText(/Specimen Type/i), {
          target: { value: "Specimen Type" },
        });
        fireEvent.change(screen.getByLabelText(/Num Of Samples/i), {
          target: { value: "Number of Samples" },
        });
      });

      await act(async () => {
        fireEvent.click(screen.getByText(/Preview/i));
      });

      // Should not call preview API since entryId is null
      expect(global.fetch).not.toHaveBeenCalledWith(
        expect.stringContaining("/preview-manifest"),
        expect.any(Object),
      );

      // Should remain on step 2 (mapping step)
      expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument();
    });
  });

  describe("Modal Controls", () => {
    test("calls onClose when modal close icon is clicked", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Wait for modal to render
      await waitFor(() => {
        expect(
          screen.getByText(/Expected CSV Columns & Data Points/i),
        ).toBeInTheDocument();
      });

      // Find the modal close button (X icon in header) - Carbon uses aria-label="close"
      const closeIcon = document.querySelector('button[aria-label="close"]');
      expect(closeIcon).toBeTruthy();

      await act(async () => {
        fireEvent.click(closeIcon);
      });

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    test("Back button returns to previous step", async () => {
      renderWithIntl(
        <TBManifestImportModal
          open={true}
          onClose={mockOnClose}
          entryId={mockEntryId}
          onImportSuccess={mockOnImportSuccess}
        />,
      );

      // Wait for modal to be ready
      await waitFor(() => {
        expect(
          screen.getByText(/Expected CSV Columns & Data Points/i),
        ).toBeInTheDocument();
      });

      // Upload file to get to step 2
      const file = createCsvFile(validCsvContent);
      const fileInput = document.querySelector('input[type="file"]');
      expect(fileInput).toBeTruthy();

      await act(async () => {
        uploadFile(fileInput, file);
      });

      await waitFor(() =>
        expect(screen.getByText(/Map your CSV columns/i)).toBeInTheDocument(),
      );

      // Verify Back button is present and click it
      const backButton = screen.getByText(/Back/i);
      expect(backButton).toBeInTheDocument();

      await act(async () => {
        fireEvent.click(backButton);
      });

      // Should be back on step 1 - check for upload instructions
      await waitFor(() => {
        expect(
          screen.getByText(/Expected CSV Columns & Data Points/i),
        ).toBeInTheDocument();
      });
    }, 15000);
  });
});
