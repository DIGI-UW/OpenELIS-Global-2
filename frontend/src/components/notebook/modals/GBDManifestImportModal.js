import React, { useState, useCallback, useEffect, useContext } from "react";
import {
  Modal,
  FileUploaderDropContainer,
  FileUploaderItem,
  Select,
  SelectItem,
  Button,
  Tag,
  Loading,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Accordion,
  AccordionItem,
  Grid,
  Column,
} from "@carbon/react";
import {
  Upload,
  Checkmark,
  Warning,
  Information,
  Download,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";

/**
 * GBD Sample Reception Manifest Import Modal
 *
 * Handles bulk sample creation via CSV manifest file.
 * Follows Bioanalytical Lab pattern for consistency.
 *
 * Expected CSV columns:
 * - Required: Sample ID, Sample Type, Source, Project/Study, Collection Date, Reception Date/Time
 * - Reception Optional: Volume/Concentration, A260/280, A260/230, RIN
 * - Processing Optional: Extraction Method/Kit, PCR Protocol, Library Prep Protocol,
 *                        Sequencing Platform, Run ID, Operator, Processing Date/Time
 * - General Optional: Notes
 */
const EXPECTED_DATA_POINTS = {
  required: [
    {
      key: "sampleId",
      label: "Sample ID",
      description: "Unique identifier for the sample",
      example: "GBD-2024-001, BIO-DNA-001",
    },
    {
      key: "sampleType",
      label: "Sample Type",
      description: "Type of incoming material",
      example:
        "DNA, RNA, tissue, isolate, pcr_product, library, swab, Extracted DNA/RNA",
    },
    {
      key: "source",
      label: "Source",
      description: "Origin of the sample",
      example: "patient, animal, environmental, culture",
    },
    {
      key: "projectStudyAssociation",
      label: "Project/Study Association",
      description: "Project or study identifier",
      example: "Project Alpha, Study-2024-001, RE-001",
    },
    {
      key: "collectionDate",
      label: "Collection Date",
      description: "Date sample was collected (YYYY-MM-DD)",
      example: "2024-01-15",
    },
    {
      key: "receptionDateTime",
      label: "Reception Date/Time",
      description:
        "Date and time sample was received (YYYY-MM-DD HH:MM or 'now')",
      example: "2024-01-20 14:30, now",
    },
  ],
  optional: [
    {
      key: "volumeConcentration",
      label: "Volume/Concentration",
      description: "Initial volume or concentration if available",
      example: "100 ng/µL, 50 µL, 5 mg, 1 mL",
      category: "Reception Metrics",
    },
    {
      key: "a260_280",
      label: "A260/280 Ratio",
      description: "Pre-assessed DNA purity metric",
      example: "1.85, 1.90",
      category: "Reception Metrics",
    },
    {
      key: "a260_230",
      label: "A260/230 Ratio",
      description: "Pre-assessed purity metric (salt/phenol check)",
      example: "2.1, 2.0",
      category: "Reception Metrics",
    },
    {
      key: "rin",
      label: "RIN (RNA Integrity Number)",
      description: "Pre-assessed RNA integrity score",
      example: "8.5, 7.2",
      category: "Reception Metrics",
    },
    {
      key: "extractionMethodKit",
      label: "Extraction Method/Kit",
      description: "Kit or protocol used for nucleic acid extraction",
      example:
        "TRIzol Extraction, RNeasy Mini Kit, CTAB Protocol, Phenol-Chloroform",
      category: "Processing Metadata",
    },
    {
      key: "pcrProtocol",
      label: "PCR Protocol",
      description: "PCR amplification protocol or kit used",
      example: "Standard PCR, RT-PCR, qRT-PCR, Nested PCR, Colony PCR",
      category: "Processing Metadata",
    },
    {
      key: "libraryPrepProtocol",
      label: "Library Prep Protocol",
      description: "Library preparation kit or protocol",
      example: "NEBNext Ultra II, TruSeq DNA, Nextera XT, KAPA Hyper Prep",
      category: "Processing Metadata",
    },
    {
      key: "sequencingPlatform",
      label: "Sequencing Platform",
      description: "Sequencing instrument used",
      example: "Illumina MiSeq, Illumina NextSeq, Illumina HiSeq, Ion Torrent",
      category: "Processing Metadata",
    },
    {
      key: "runId",
      label: "Run ID",
      description: "Sequencing run identifier",
      example: "RUN-2024-001, RUN-001",
      category: "Processing Metadata",
    },
    {
      key: "operator",
      label: "Operator",
      description: "Name or identifier of person who performed the processing",
      example: "Dr. Smith, Tech Martinez, Dr. Lee",
      category: "Processing Metadata",
    },
    {
      key: "processingDateTime",
      label: "Processing Date/Time",
      description: "Date and time processing was performed (YYYY-MM-DD HH:MM)",
      example: "2024-01-20 15:00",
      category: "Processing Metadata",
    },
    {
      key: "notes",
      label: "Notes",
      description: "Any additional metadata or special handling instructions",
      example: "Keep at -20°C, Urgent processing, High quality RNA",
      category: "General",
    },
  ],
};

/**
 * CSV Template for GBD Sample Manifest
 */
const generateCSVTemplate = () => {
  const headers = [
    "Sample ID",
    "Sample Type",
    "Source",
    "Project/Study Association",
    "Collection Date",
    "Reception Date/Time",
    "Volume/Concentration",
    "A260/280",
    "A260/230",
    "RIN",
    "Extraction Method/Kit",
    "PCR Protocol",
    "Library Prep Protocol",
    "Sequencing Platform",
    "Run ID",
    "Operator",
    "Processing Date/Time",
    "Notes",
  ];

  const examples = [
    [
      "GBD-2024-001",
      "DNA",
      "patient",
      "Project Alpha",
      "2024-01-15",
      "2024-01-20 14:30",
      "100 ng/µL",
      "1.85",
      "2.1",
      "",
      "TRIzol Extraction",
      "Standard PCR",
      "NEBNext Ultra II",
      "Illumina MiSeq",
      "RUN-2024-001",
      "Dr. Smith",
      "2024-01-20 15:00",
      "Standard processing",
    ],
    [
      "GBD-2024-002",
      "RNA",
      "animal",
      "Project Alpha",
      "2024-01-16",
      "2024-01-21 09:00",
      "50 ng/µL",
      "1.88",
      "2.0",
      "8.5",
      "RNeasy Mini Kit",
      "RT-PCR",
      "NEBNext Ultra II Stranded RNA",
      "Illumina NextSeq",
      "RUN-2024-002",
      "Dr. Johnson",
      "2024-01-21 10:00",
      "Keep at -20C",
    ],
    [
      "GBD-2024-003",
      "tissue",
      "environmental",
      "Study-2024-001",
      "2024-01-17",
      "now",
      "",
      "",
      "",
      "",
      "CTAB Protocol",
      "Standard PCR",
      "TruSeq DNA",
      "Illumina HiSeq",
      "RUN-2024-003",
      "Dr. Lee",
      "2024-01-22 09:00",
      "Urgent processing",
    ],
  ];

  const csv = [headers, ...examples]
    .map((row) => row.map((cell) => `"${cell}"`).join(","))
    .join("\n");
  return csv;
};

/**
 * Download CSV template
 */
const downloadTemplate = () => {
  const csv = generateCSVTemplate();
  const blob = new Blob([csv], { type: "text/csv" });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "GBD_Sample_Manifest_Template.csv";
  a.click();
  window.URL.revokeObjectURL(url);
};

/**
 * Parse CSV content
 */
const parseCSV = (content) => {
  const lines = content.split("\n").filter((line) => line.trim());
  if (lines.length < 2) {
    throw new Error("CSV file must contain headers and at least one row");
  }

  const headers = lines[0]
    .split(",")
    .map((h) => h.trim().replace(/^"|"$/g, ""));
  const rows = lines.slice(1).map((line) => {
    const cells = line.split(",").map((c) => c.trim().replace(/^"|"$/g, ""));
    const row = {};
    headers.forEach((header, index) => {
      const key = header
        .toLowerCase()
        .replace(/\s+/g, "_")
        .replace(/[^\w_]/g, "");
      row[key] = cells[index] || "";
    });
    return row;
  });

  return rows;
};

/**
 * Validate parsed rows
 */
const validateRows = (rows) => {
  const errors = [];
  const validRows = [];

  rows.forEach((row, index) => {
    const rowErrors = [];

    // Check required fields
    EXPECTED_DATA_POINTS.required.forEach((field) => {
      if (!row[field.key] || row[field.key].trim() === "") {
        rowErrors.push(`Missing required field: ${field.label}`);
      }
    });

    if (rowErrors.length > 0) {
      errors.push({
        rowNumber: index + 2, // +2 for header and 1-based indexing
        errors: rowErrors,
      });
    } else {
      validRows.push(row);
    }
  });

  return { validRows, errors };
};

function GBDManifestImportModal({
  isOpen,
  onClose,
  onImport,
  isLoading = false,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [uploadedFile, setUploadedFile] = useState(null);
  const [parseErrors, setParseErrors] = useState([]);
  const [parsedData, setParsedData] = useState([]);
  const [validRows, setValidRows] = useState([]);
  const [step, setStep] = useState("upload"); // upload, preview, or confirm

  // Handle file upload
  const handleFileUpload = useCallback(
    (files) => {
      const file = files[0];
      if (!file) return;

      setUploadedFile(file);
      const reader = new FileReader();

      reader.onload = (e) => {
        try {
          const content = e.target.result;
          const rows = parseCSV(content);
          const { validRows: valid, errors } = validateRows(rows);

          setParsedData(rows);
          setValidRows(valid);
          setParseErrors(errors);
          setStep("preview");

          if (errors.length > 0) {
            setNotificationVisible(true);
            addNotification({
              kind: NotificationKinds.warning,
              title: intl.formatMessage({
                id: "notebook.gbd.manifest.validation.warning",
                defaultMessage: "Some rows have validation errors",
              }),
              message: intl.formatMessage(
                {
                  id: "notebook.gbd.manifest.validation.message",
                  defaultMessage: "{count} out of {total} rows are valid",
                },
                { count: valid.length, total: rows.length },
              ),
            });
          }
        } catch (error) {
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.gbd.manifest.parse.error",
              defaultMessage: "Error parsing CSV file",
            }),
            message: error.message,
          });
        }
      };

      reader.readAsText(file);
    },
    [intl, setNotificationVisible, addNotification],
  );

  // Handle import
  const handleImport = useCallback(async () => {
    if (validRows.length === 0) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.manifest.noValid",
          defaultMessage: "No Valid Samples",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.manifest.noValidMessage",
          defaultMessage: "Please fix validation errors before importing",
        }),
      });
      return;
    }

    try {
      await onImport(validRows);
      handleReset();
      onClose();
    } catch (error) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.manifest.import.error",
          defaultMessage: "Import Failed",
        }),
        message: error.message,
      });
    }
  }, [
    validRows,
    onImport,
    onClose,
    intl,
    setNotificationVisible,
    addNotification,
  ]);

  const handleReset = useCallback(() => {
    setUploadedFile(null);
    setParseErrors([]);
    setParsedData([]);
    setValidRows([]);
    setStep("upload");
  }, []);

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "notebook.gbd.manifest.modal.title",
        defaultMessage: "Import Sample Manifest",
      })}
      primaryButtonText={
        step === "preview"
          ? intl.formatMessage({
              id: "notebook.gbd.manifest.import",
              defaultMessage: "Import Samples",
            })
          : intl.formatMessage({
              id: "notebook.gbd.cancel",
              defaultMessage: "Cancel",
            })
      }
      secondaryButtonText={intl.formatMessage({
        id: "notebook.gbd.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={step === "preview" ? handleImport : undefined}
      onRequestClose={onClose}
      size="lg"
      danger={false}
    >
      {isLoading && <Loading />}

      {step === "upload" && (
        <>
          <div style={{ marginBottom: "2rem" }}>
            <Button
              kind="secondary"
              renderIcon={Download}
              onClick={downloadTemplate}
              style={{ marginBottom: "1rem" }}
            >
              <FormattedMessage
                id="notebook.gbd.manifest.download"
                defaultMessage="Download CSV Template"
              />
            </Button>
          </div>

          <FileUploaderDropContainer
            accept={[".csv"]}
            onAddFiles={(e) => handleFileUpload(e.addedFiles)}
            disabled={isLoading}
            label={intl.formatMessage({
              id: "notebook.gbd.manifest.dropzone",
              defaultMessage: "Drag and drop CSV file here or click to select",
            })}
          />

          <Accordion style={{ marginTop: "2rem" }}>
            <AccordionItem
              title={intl.formatMessage({
                id: "notebook.gbd.manifest.requiredFields",
                defaultMessage: "Required Fields",
              })}
            >
              <StructuredListWrapper>
                <StructuredListHead>
                  <StructuredListRow head>
                    <StructuredListCell head>Field</StructuredListCell>
                    <StructuredListCell head>Description</StructuredListCell>
                    <StructuredListCell head>Example</StructuredListCell>
                  </StructuredListRow>
                </StructuredListHead>
                <StructuredListBody>
                  {EXPECTED_DATA_POINTS.required.map((field) => (
                    <StructuredListRow key={field.key}>
                      <StructuredListCell>{field.label}</StructuredListCell>
                      <StructuredListCell>
                        {field.description}
                      </StructuredListCell>
                      <StructuredListCell>{field.example}</StructuredListCell>
                    </StructuredListRow>
                  ))}
                </StructuredListBody>
              </StructuredListWrapper>
            </AccordionItem>

            <AccordionItem
              title={intl.formatMessage({
                id: "notebook.gbd.manifest.optionalFields",
                defaultMessage: "Optional Fields",
              })}
            >
              <StructuredListWrapper>
                <StructuredListHead>
                  <StructuredListRow head>
                    <StructuredListCell head>Field</StructuredListCell>
                    <StructuredListCell head>Description</StructuredListCell>
                    <StructuredListCell head>Example</StructuredListCell>
                  </StructuredListRow>
                </StructuredListHead>
                <StructuredListBody>
                  {EXPECTED_DATA_POINTS.optional.map((field) => (
                    <StructuredListRow key={field.key}>
                      <StructuredListCell>{field.label}</StructuredListCell>
                      <StructuredListCell>
                        {field.description}
                      </StructuredListCell>
                      <StructuredListCell>{field.example}</StructuredListCell>
                    </StructuredListRow>
                  ))}
                </StructuredListBody>
              </StructuredListWrapper>
            </AccordionItem>
          </Accordion>
        </>
      )}

      {step === "preview" && (
        <>
          <Grid style={{ marginBottom: "2rem" }}>
            <Column lg={6} md={6} sm={4}>
              <div
                style={{
                  padding: "1rem",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <div
                  style={{
                    fontSize: "1.5rem",
                    fontWeight: "bold",
                    color: "#24a148",
                  }}
                >
                  {validRows.length}
                </div>
                <div style={{ color: "#666", marginTop: "0.5rem" }}>
                  <FormattedMessage
                    id="notebook.gbd.manifest.validSamples"
                    defaultMessage="Valid Samples"
                  />
                </div>
              </div>
            </Column>
            <Column lg={6} md={6} sm={4}>
              <div
                style={{
                  padding: "1rem",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <div
                  style={{
                    fontSize: "1.5rem",
                    fontWeight: "bold",
                    color: parseErrors.length > 0 ? "#da1e28" : "#24a148",
                  }}
                >
                  {parseErrors.length}
                </div>
                <div style={{ color: "#666", marginTop: "0.5rem" }}>
                  <FormattedMessage
                    id="notebook.gbd.manifest.errors"
                    defaultMessage="Rows with Errors"
                  />
                </div>
              </div>
            </Column>
          </Grid>

          {parseErrors.length > 0 && (
            <Accordion style={{ marginBottom: "2rem" }}>
              <AccordionItem
                title={intl.formatMessage(
                  {
                    id: "notebook.gbd.manifest.validationErrors",
                    defaultMessage: "Validation Errors ({count})",
                  },
                  { count: parseErrors.length },
                )}
              >
                <div style={{ maxHeight: "300px", overflowY: "auto" }}>
                  {parseErrors.map((error) => (
                    <div
                      key={error.rowNumber}
                      style={{
                        marginBottom: "1rem",
                        padding: "1rem",
                        backgroundColor: "#fff3cd",
                        borderRadius: "4px",
                      }}
                    >
                      <div style={{ fontWeight: "bold" }}>
                        <FormattedMessage
                          id="notebook.gbd.manifest.row"
                          defaultMessage="Row {number}"
                          values={{ number: error.rowNumber }}
                        />
                      </div>
                      {error.errors.map((err, idx) => (
                        <div
                          key={idx}
                          style={{ color: "#da1e28", marginTop: "0.5rem" }}
                        >
                          ❌ {err}
                        </div>
                      ))}
                    </div>
                  ))}
                </div>
              </AccordionItem>
            </Accordion>
          )}

          <Accordion>
            <AccordionItem
              title={intl.formatMessage(
                {
                  id: "notebook.gbd.manifest.preview",
                  defaultMessage: "Preview Valid Samples ({count})",
                },
                { count: validRows.length },
              )}
            >
              <div
                style={{
                  maxHeight: "400px",
                  overflowY: "auto",
                  overflowX: "auto",
                }}
              >
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead>
                    <tr style={{ backgroundColor: "#f4f4f4" }}>
                      <th
                        style={{
                          padding: "0.5rem",
                          textAlign: "left",
                          borderBottom: "1px solid #d0d0d0",
                        }}
                      >
                        Sample ID
                      </th>
                      <th
                        style={{
                          padding: "0.5rem",
                          textAlign: "left",
                          borderBottom: "1px solid #d0d0d0",
                        }}
                      >
                        Type
                      </th>
                      <th
                        style={{
                          padding: "0.5rem",
                          textAlign: "left",
                          borderBottom: "1px solid #d0d0d0",
                        }}
                      >
                        Source
                      </th>
                      <th
                        style={{
                          padding: "0.5rem",
                          textAlign: "left",
                          borderBottom: "1px solid #d0d0d0",
                        }}
                      >
                        Project
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {validRows.map((row, idx) => (
                      <tr
                        key={idx}
                        style={{ borderBottom: "1px solid #e0e0e0" }}
                      >
                        <td style={{ padding: "0.5rem" }}>
                          {row.sampleid || "-"}
                        </td>
                        <td style={{ padding: "0.5rem" }}>
                          {row.sampletype || "-"}
                        </td>
                        <td style={{ padding: "0.5rem" }}>
                          {row.source || "-"}
                        </td>
                        <td style={{ padding: "0.5rem" }}>
                          {row.projectstudyassociation || "-"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AccordionItem>
          </Accordion>
        </>
      )}
    </Modal>
  );
}

export default GBDManifestImportModal;
