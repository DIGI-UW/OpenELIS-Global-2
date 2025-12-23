import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  Loading,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Checkbox,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  FileUploaderDropContainer,
  FileUploaderItem,
} from "@carbon/react";
import {
  Add,
  Checkmark,
  Microscope,
  Renew,
  Chemistry,
  DocumentView,
  WarningAlt,
  CheckmarkFilled,
  CloseFilled,
  Upload,
  Edit,
  Download,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyTestingMicroscopyPage - Page 4 of the pathology workflow.
 * Purpose: Perform diagnostic or research assays on sample-derived material.
 * Who uses it: Pathologists / PIs / technicians
 *
 * Two-phase workflow:
 * 1. Add Tests - Record staining, advanced techniques, controls, microscopy observation
 * 2. Enter Results - After tests are added, enter results via CSV import or manual entry
 */
function PathologyTestingMicroscopyPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state for bulk operations
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Add Test Modal state
  const [testingModalOpen, setTestingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [isBulkMode, setIsBulkMode] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Enter Results Modal state
  const [resultsModalOpen, setResultsModalOpen] = useState(false);
  const [resultsEntryMode, setResultsEntryMode] = useState("manual"); // "manual" or "csv"
  const [resultsData, setResultsData] = useState({
    resultFindings: "",
    diagnosisCode: "",
    clinicalInterpretation: "",
    verifiedByPathologist: false,
    verifyingPathologistName: "",
    verificationDate: "",
    pathologistSignature: "",
    pathologistDate: "",
    additionalNotes: "",
  });

  // CSV Import state for results
  const [csvFile, setCsvFile] = useState(null);
  const [csvPreview, setCsvPreview] = useState(null);
  const [csvErrors, setCsvErrors] = useState([]);
  const [isImporting, setIsImporting] = useState(false);

  // Test form state - for Add Test modal
  const [testData, setTestData] = useState({
    // === STAINING & SLIDE PREPARATION ===
    routineStainingCategory: "", // histology, cytology, blood
    routineStains: [],
    specialStains: [],
    specialStainIndication: "",

    // === ADVANCED TECHNIQUES ===
    // IHC/ICC
    ihcIccPerformed: false,
    ihcIccSampleType: "", // FFPE, cell_block, cytology_smear
    ihcIccMarkers: "",
    ihcIccPrimaryAntibody: "",
    ihcIccClone: "",
    ihcIccDilution: "",
    ihcIccAntigenRetrieval: "",

    // ISH
    ishPerformed: false,
    ishTargetType: "", // DNA, RNA
    ishTargetSequence: "",
    ishProbeDetails: "",

    // Research Assays
    researchAssays: [],
    researchAssayDetails: "",

    // === CONTROLS & VALIDATION ===
    positiveControlRun: false,
    positiveControlTissue: "",
    positiveControlResult: "",
    positiveControlExpectedStaining: "",
    negativeControlRun: false,
    negativeControlType: "",
    negativeControlResult: "",
    batchNumber: "",
    batchDate: "",
    controlsAccepted: false,
    stainQualityAdequate: false,

    // === MICROSCOPY OBSERVATION ===
    microscopyPerformed: false,
    microscopist: "", // pathologist or PI name
    magnificationUsed: "",
    microscopeType: "",
    findingsDocumented: false,
    microscopicFindings: "",

    // === DOCUMENTATION ===
    accessionNumber: "",
    blockSlideId: "",
    testName: "",
    testType: "",
    controlsUsedSummary: "",
    technicianSignature: "",
    technicianDate: "",
  });

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => {
              const sampleData = sample.data || {};
              const allStains = [
                ...(sampleData.routineStains || []),
                ...(sampleData.specialStains || []),
              ];
              const stainsDisplay =
                allStains.length > 0
                  ? allStains.slice(0, 3).join(", ") +
                    (allStains.length > 3 ? "..." : "")
                  : "";

              return {
                id: String(sample.id || sample.sampleItemId),
                accessionNumber: sample.accessionNumber,
                blockSlideId: sampleData.blockSlideId || sample.blockSlideId,
                specimenType:
                  sample.sampleType || sample.typeOfSample?.description,
                status: sample.pageStatus || "PENDING",
                testsPerformed: sample.testsPerformed || 0,
                testName: sampleData.testName || "",
                result: sampleData.resultFindings || sampleData.result || "",
                stains: stainsDisplay,
                hasTestData: !!(sampleData.testName || allStains.length > 0),
                verifiedByPathologist:
                  sampleData.verifiedByPathologist === true ||
                  sampleData.verifiedByPathologist === "true",
                technicianSignature: sampleData.technicianSignature || "",
                testDate: sampleData.technicianDate || "",
              };
            });
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setTestData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleResultsInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setResultsData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleToggleChange = (fieldName, checked) => {
    setTestData((prev) => ({
      ...prev,
      [fieldName]: checked,
    }));
  };

  const handleMultiCheckbox = (fieldName, value, checked) => {
    setTestData((prev) => ({
      ...prev,
      [fieldName]: checked
        ? [...prev[fieldName], value]
        : prev[fieldName].filter((v) => v !== value),
    }));
  };

  const handleDateChange = (dates, fieldName, isResultsForm = false) => {
    if (dates && dates.length > 0) {
      const date = dates[0];
      const formattedDate = date.toISOString().split("T")[0];
      if (isResultsForm) {
        setResultsData((prev) => ({
          ...prev,
          [fieldName]: formattedDate,
        }));
      } else {
        setTestData((prev) => ({
          ...prev,
          [fieldName]: formattedDate,
        }));
      }
    }
  };

  // Reset test form data
  const resetTestData = (sample = null) => {
    setTestData({
      routineStainingCategory: "",
      routineStains: [],
      specialStains: [],
      specialStainIndication: "",
      ihcIccPerformed: false,
      ihcIccSampleType: "",
      ihcIccMarkers: "",
      ihcIccPrimaryAntibody: "",
      ihcIccClone: "",
      ihcIccDilution: "",
      ihcIccAntigenRetrieval: "",
      ishPerformed: false,
      ishTargetType: "",
      ishTargetSequence: "",
      ishProbeDetails: "",
      researchAssays: [],
      researchAssayDetails: "",
      positiveControlRun: false,
      positiveControlTissue: "",
      positiveControlResult: "",
      positiveControlExpectedStaining: "",
      negativeControlRun: false,
      negativeControlType: "",
      negativeControlResult: "",
      batchNumber: "",
      batchDate: new Date().toISOString().split("T")[0],
      controlsAccepted: false,
      stainQualityAdequate: false,
      microscopyPerformed: false,
      microscopist: "",
      magnificationUsed: "",
      microscopeType: "",
      findingsDocumented: false,
      microscopicFindings: "",
      accessionNumber: sample?.accessionNumber || "",
      blockSlideId: sample?.blockSlideId || "",
      testName: "",
      testType: "",
      controlsUsedSummary: "",
      technicianSignature: "",
      technicianDate: new Date().toISOString().split("T")[0],
    });
  };

  // Reset results form data
  const resetResultsData = () => {
    setResultsData({
      resultFindings: "",
      diagnosisCode: "",
      clinicalInterpretation: "",
      verifiedByPathologist: false,
      verifyingPathologistName: "",
      verificationDate: "",
      pathologistSignature: "",
      pathologistDate: "",
      additionalNotes: "",
    });
    setCsvFile(null);
    setCsvPreview(null);
    setCsvErrors([]);
  };

  // Open modal for bulk Add Test operation
  const openBulkTestingModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noSelection",
          defaultMessage: "Please select at least one sample for bulk testing.",
        }),
      );
      return;
    }

    setSelectedSample(null);
    setIsBulkMode(true);
    resetTestData();
    setTestingModalOpen(true);
  };

  // Open modal for single sample Add Test
  const openTestingModal = (sample) => {
    setSelectedSample(sample);
    setIsBulkMode(false);
    resetTestData(sample);
    setTestingModalOpen(true);
  };

  // Open Results Entry modal
  const openResultsModal = () => {
    // Only allow entering results for samples that have tests added
    const samplesWithTests = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.hasTestData,
    );

    if (samplesWithTests.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noTestsAdded",
          defaultMessage:
            "Please add tests to samples first before entering results.",
        }),
      );
      return;
    }

    resetResultsData();
    setResultsModalOpen(true);
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
  };

  // Get selected samples for display
  const getSelectedSamples = () => {
    return samples.filter((s) => selectedSampleIds.includes(s.id));
  };

  // Get samples with tests for results entry
  const getSamplesWithTests = () => {
    return samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.hasTestData,
    );
  };

  // Submit Add Test data
  const handleSubmitTest = () => {
    if (submitting) return;

    // Validate required fields
    if (!testData.testName || !testData.technicianSignature) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.requiredFields",
          defaultMessage: "Please fill in Test Name and Technician Signature",
        }),
      );
      return;
    }

    // For single mode, blockSlideId is required
    if (!isBulkMode && !testData.blockSlideId) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.blockSlideRequired",
          defaultMessage: "Please fill in Block/Slide ID",
        }),
      );
      return;
    }

    // Validate controls for IHC/ICC/ISH
    if (testData.ihcIccPerformed || testData.ishPerformed) {
      if (!testData.positiveControlRun || !testData.negativeControlRun) {
        setError(
          intl.formatMessage({
            id: "pathology.testing.error.controlsRequired",
            defaultMessage:
              "For IHC/ICC/ISH, both positive and negative controls are required.",
          }),
        );
        return;
      }
    }

    setSubmitting(true);
    setError(null);

    const sampleIdsToProcess = isBulkMode
      ? selectedSampleIds.map((id) => parseInt(id, 10))
      : [parseInt(selectedSample?.id, 10)];

    // Save test data (status stays IN_PROGRESS until results are entered)
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleIdsToProcess,
        data: testData,
      }),
      (applyResponse) => {
        if (applyResponse && applyResponse.success) {
          // Update status to IN_PROGRESS (tests added, awaiting results)
          postToOpenElisServerJsonResponse(
            `/rest/notebook/bulk/page/${pageData?.id}/samples/status`,
            JSON.stringify({
              sampleIds: sampleIdsToProcess,
              status: "IN_PROGRESS",
            }),
            (statusResponse) => {
              setSubmitting(false);
              if (statusResponse && statusResponse.success) {
                setTestingModalOpen(false);
                if (isBulkMode) {
                  setSelectedSampleIds([]);
                } else {
                  setSelectedSample(null);
                }
                setSuccessMessage(
                  intl.formatMessage(
                    {
                      id: "pathology.testing.success.testAdded",
                      defaultMessage:
                        "Successfully added tests to {count} samples. You can now enter results.",
                    },
                    { count: sampleIdsToProcess.length },
                  ),
                );
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              } else {
                setError(
                  statusResponse?.error ||
                    intl.formatMessage({
                      id: "pathology.testing.error.statusUpdateFailed",
                      defaultMessage:
                        "Failed to update sample status. Please try again.",
                    }),
                );
              }
            },
          );
        } else {
          setSubmitting(false);
          setError(
            applyResponse?.error ||
              intl.formatMessage({
                id: "pathology.testing.error.bulkSubmitFailed",
                defaultMessage: "Failed to submit test data. Please try again.",
              }),
          );
        }
      },
    );
  };

  // Handle manual results submission
  const handleSubmitResults = () => {
    if (submitting) return;

    const samplesWithTests = getSamplesWithTests();
    if (samplesWithTests.length === 0) return;

    if (!resultsData.resultFindings) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.resultRequired",
          defaultMessage: "Please enter the result findings.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const sampleIds = samplesWithTests.map((s) => parseInt(s.id, 10));

    // Save results data - keep samples IN_PROGRESS after result entry
    // Use "Mark Complete" button to mark as COMPLETED and advance to next page
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleIds,
        data: resultsData,
      }),
      (applyResponse) => {
        if (applyResponse && applyResponse.success) {
          // Keep samples as IN_PROGRESS after result entry
          // Completion is done via the "Mark Complete" button
          postToOpenElisServerJsonResponse(
            `/rest/notebook/bulk/page/${pageData?.id}/samples/status`,
            JSON.stringify({
              sampleIds: sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusResponse) => {
              setSubmitting(false);
              if (statusResponse && statusResponse.success) {
                setResultsModalOpen(false);
                setSelectedSampleIds([]);
                setSuccessMessage(
                  intl.formatMessage(
                    {
                      id: "pathology.testing.success.resultsEntered",
                      defaultMessage:
                        "Successfully entered results for {count} samples. Use 'Mark Complete' to advance samples to the next page.",
                    },
                    { count: sampleIds.length },
                  ),
                );
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              } else {
                setError(statusResponse?.error || "Failed to update status.");
              }
            },
          );
        } else {
          setSubmitting(false);
          setError(applyResponse?.error || "Failed to save results.");
        }
      },
    );
  };

  // Handle marking samples as complete (pushes to next page)
  const handleMarkComplete = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    // Allow marking complete for samples that:
    // 1. Are selected
    // 2. Are IN_PROGRESS status (results have been entered)
    // OR have result data (result field) OR have test data (hasTestData)
    const samplesWithResults = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        (s.status === "IN_PROGRESS" || s.result || s.hasTestData),
    );

    if (samplesWithResults.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noResultsToComplete",
          defaultMessage:
            "Please enter results for samples before marking them as complete.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const sampleIds = samplesWithResults.map((s) => parseInt(s.id, 10));

    // Mark as COMPLETED - this triggers the next page creation in the backend
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/status`,
      JSON.stringify({
        sampleIds: sampleIds,
        status: "COMPLETED",
      }),
      (statusResponse) => {
        setSubmitting(false);
        if (statusResponse && statusResponse.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.testing.success.markedComplete",
                defaultMessage:
                  "Successfully marked {count} samples as complete. Samples will advance to the next page.",
              },
              { count: sampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            statusResponse?.error || "Failed to mark samples as complete.",
          );
        }
      },
    );
  };

  // Handle verifying results (pathologist sign-off)
  // This keeps samples IN_PROGRESS - only Mark Complete changes status to COMPLETED
  const handleVerifyResults = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    // Only allow verifying samples that have results/test data and are not already verified
    const samplesToVerify = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        (s.result || s.hasTestData || s.status === "IN_PROGRESS") &&
        !s.verifiedByPathologist,
    );

    if (samplesToVerify.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noSamplesToVerify",
          defaultMessage:
            "No unverified samples with results found in selection.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const sampleIds = samplesToVerify.map((s) => parseInt(s.id, 10));

    // Apply verification data to selected samples
    const verificationData = {
      verifiedByPathologist: true,
      verificationDate: new Date().toISOString().split("T")[0],
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleIds,
        data: verificationData,
      }),
      (applyResponse) => {
        setSubmitting(false);
        if (applyResponse && applyResponse.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.testing.success.verified",
                defaultMessage:
                  "Successfully verified results for {count} samples.",
              },
              { count: sampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(applyResponse?.error || "Failed to verify results.");
        }
      },
    );
  };

  // Handle CSV file upload for results
  const handleCsvFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

      if (!addedFile.name.endsWith(".csv")) {
        setCsvErrors([
          {
            message: intl.formatMessage({
              id: "pathology.testing.error.invalidFileType",
              defaultMessage: "Please upload a CSV file",
            }),
          },
        ]);
        return;
      }

      setCsvFile(addedFile);
      setCsvErrors([]);

      // Parse CSV for preview
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target.result;
        const lines = text.split("\n").filter((line) => line.trim());
        if (lines.length < 2) {
          setCsvErrors([
            { message: "CSV file must have header and data rows" },
          ]);
          return;
        }

        const headers = lines[0]
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        const rows = lines.slice(1).map((line, idx) => {
          const values = line.split(",").map((v) => v.trim().replace(/"/g, ""));
          const row = { rowNumber: idx + 2 };
          headers.forEach((h, i) => {
            row[h] = values[i] || "";
          });
          return row;
        });

        setCsvPreview({ headers, rows, totalRows: rows.length });
      };
      reader.readAsText(addedFile);
    },
    [intl],
  );

  // Handle CSV import for results
  const handleCsvImport = async () => {
    if (!csvPreview?.rows || !pageData?.id) return;

    setIsImporting(true);
    setCsvErrors([]);

    const endpoint = `${config.serverBaseUrl}/rest/notebook/pathology/page/${pageData.id}/results/import-csv`;

    try {
      // Send parsed CSV rows as JSON (backend expects { rows: [...] })
      const response = await fetch(endpoint, {
        method: "POST",
        body: JSON.stringify({ rows: csvPreview.rows }),
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setResultsModalOpen(false);
        setSelectedSampleIds([]);
        setCsvFile(null);
        setCsvPreview(null);
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "pathology.testing.success.csvImport",
              defaultMessage:
                "Successfully imported results for {count} samples.",
            },
            { count: data.processedCount || 0 },
          ),
        );
        loadPageSamples();
        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        setCsvErrors(
          data.errors || [{ message: data.error || "Import failed" }],
        );
      }
    } catch (error) {
      setCsvErrors([{ message: error.message }]);
    } finally {
      setIsImporting(false);
    }
  };

  // Download CSV template for results
  const handleDownloadResultsTemplate = () => {
    const templateContent = `accessionNumber,blockSlideId,resultFindings,diagnosisCode,clinicalInterpretation,verifiedByPathologist,verifyingPathologistName,verificationDate,additionalNotes
ACC-2024-001,BLK-001-A,"Positive for malignancy",C50.9,"Invasive ductal carcinoma, Grade 2",true,Dr. Smith,2024-01-15,"Review recommended"
ACC-2024-002,BLK-002-A,"Negative for malignancy",,Benign fibrocystic changes,true,Dr. Jones,2024-01-15,""`;

    const blob = new Blob([templateContent], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "pathology_results_template.csv";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  };

  // Calculate stats
  const testedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const verifiedCount = samples.filter((s) => s.verifiedByPathologist).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.table.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "blockSlideId",
      header: intl.formatMessage({
        id: "pathology.table.blockSlideId",
        defaultMessage: "Block/Slide ID",
      }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "testName",
      header: intl.formatMessage({
        id: "pathology.table.testName",
        defaultMessage: "Test Name",
      }),
    },
    {
      key: "stains",
      header: intl.formatMessage({
        id: "pathology.table.stains",
        defaultMessage: "Stains",
      }),
    },
    {
      key: "result",
      header: intl.formatMessage({
        id: "pathology.table.result",
        defaultMessage: "Result",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "pathology.table.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "verified",
      header: intl.formatMessage({
        id: "pathology.table.verified",
        defaultMessage: "Verified",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "pathology.table.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  // === STAIN OPTIONS ===
  const routineStainsByCategory = {
    histology: [{ id: "HE", label: "H&E (Hematoxylin & Eosin)" }],
    cytology: [
      { id: "Pap", label: "Papanicolaou (Pap)" },
      { id: "Romanowsky_Giemsa", label: "Romanowsky (Giemsa)" },
      { id: "Romanowsky_DiffQuik", label: "Romanowsky (Diff-Quik)" },
    ],
    blood: [
      { id: "Giemsa", label: "Giemsa" },
      { id: "Wright", label: "Wright" },
    ],
  };

  const specialStainOptions = [
    {
      id: "AFB",
      label: "AFB (Acid-Fast Bacilli)",
      indication: "Tuberculosis, Leprosy",
    },
    {
      id: "GMS",
      label: "GMS (Grocott's Methenamine Silver)",
      indication: "Fungi",
    },
    {
      id: "PAS",
      label: "PAS (Periodic Acid-Schiff)",
      indication: "Glycogen, Fungi, Basement membranes",
    },
    { id: "Gram", label: "Gram Stain", indication: "Bacteria" },
    { id: "Trichrome", label: "Masson's Trichrome", indication: "Fibrosis" },
    { id: "Reticulin", label: "Reticulin", indication: "Liver architecture" },
    { id: "Iron", label: "Prussian Blue (Iron)", indication: "Iron deposits" },
    { id: "Congo", label: "Congo Red", indication: "Amyloid" },
    { id: "Mucicarmine", label: "Mucicarmine", indication: "Mucin" },
    {
      id: "Oil_Red_O",
      label: "Oil Red O",
      indication: "Lipids (frozen sections)",
    },
  ];

  const researchAssayOptions = [
    { id: "laser_microdissection", label: "Laser Microdissection" },
    { id: "multiplex_ihc", label: "Multiplex IHC" },
    { id: "immunofluorescent", label: "Immunofluorescent Staining" },
    { id: "rnascope", label: "RNAscope®" },
  ];

  const commonIhcMarkers = [
    "Ki-67",
    "p53",
    "ER",
    "PR",
    "HER2",
    "CK7",
    "CK20",
    "CD3",
    "CD20",
    "CD45",
    "S100",
    "Melan-A",
    "TTF-1",
    "CDX2",
    "PAX8",
    "GATA3",
  ];

  return (
    <div className="pathology-testing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.testing.title"
            defaultMessage="Testing, Staining & Microscopy"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.testing.description"
            defaultMessage="Perform diagnostic or research assays on sample-derived material. First add tests, then enter results via manual entry or CSV import."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.pending"
                  defaultMessage="Pending Tests"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f0ff" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.inProgress"
                  defaultMessage="Awaiting Results"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{testedCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f7e0" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <Grid fullWidth className="action-buttons-section">
        <Column lg={16} md={8} sm={4}>
          <div
            className="action-buttons"
            style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
          >
            <Button
              kind="primary"
              size="md"
              renderIcon={Add}
              onClick={openBulkTestingModal}
              disabled={selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="pathology.page.testing.addTests"
                defaultMessage="Add Tests ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="secondary"
              size="md"
              renderIcon={Edit}
              onClick={openResultsModal}
              disabled={selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="pathology.page.testing.enterResults"
                defaultMessage="Enter Results ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="tertiary"
              size="md"
              renderIcon={CheckmarkFilled}
              onClick={handleVerifyResults}
              disabled={selectedSampleIds.length === 0 || submitting}
            >
              <FormattedMessage
                id="pathology.page.testing.verifyResults"
                defaultMessage="Verify Results ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="tertiary"
              size="md"
              renderIcon={Checkmark}
              onClick={handleMarkComplete}
              disabled={selectedSampleIds.length === 0 || submitting}
            >
              <FormattedMessage
                id="pathology.page.testing.markComplete"
                defaultMessage="Mark Complete ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="ghost"
              size="md"
              renderIcon={Renew}
              onClick={loadPageSamples}
            >
              <FormattedMessage
                id="label.button.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>
        </Column>
      </Grid>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Success Display */}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Sample Table */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading withOverlay={false} description="Loading samples..." />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.testing.empty"
                defaultMessage="No samples available for testing. Samples must be processed on the previous page first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((s) => ({
              ...s,
              verified: s.verifiedByPathologist ? "Yes" : "No",
            }))}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              if (
                selectedRows.length !== selectedSampleIds.length ||
                !selectedRows.every((r) => selectedSampleIds.includes(r.id))
              ) {
                setTimeout(() => handleSelectionChange(selectedRows), 0);
              }

              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = samples.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "status" ? (
                                  <Tag
                                    type={
                                      cell.value === "COMPLETED"
                                        ? "green"
                                        : cell.value === "IN_PROGRESS"
                                          ? "blue"
                                          : "gray"
                                    }
                                    size="sm"
                                  >
                                    {cell.value === "IN_PROGRESS"
                                      ? "Tests Added"
                                      : cell.value}
                                  </Tag>
                                ) : cell.info.header === "verified" ? (
                                  sample?.verifiedByPathologist ? (
                                    <CheckmarkFilled
                                      size={16}
                                      style={{ color: "#24a148" }}
                                    />
                                  ) : (
                                    <CloseFilled
                                      size={16}
                                      style={{ color: "#da1e28" }}
                                    />
                                  )
                                ) : cell.info.header === "actions" ? (
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Microscope}
                                    onClick={() => openTestingModal(sample)}
                                  >
                                    <FormattedMessage
                                      id="pathology.page.testing.addTest"
                                      defaultMessage="Add Test"
                                    />
                                  </Button>
                                ) : cell.info.header === "result" ? (
                                  <span
                                    title={cell.value}
                                    style={{
                                      maxWidth: "150px",
                                      overflow: "hidden",
                                      textOverflow: "ellipsis",
                                      whiteSpace: "nowrap",
                                      display: "inline-block",
                                    }}
                                  >
                                    {cell.value
                                      ? cell.value.length > 30
                                        ? cell.value.substring(0, 30) + "..."
                                        : cell.value
                                      : "-"}
                                  </span>
                                ) : (
                                  cell.value || "-"
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          />
        )}
      </div>

      {/* Add Test Modal */}
      <Modal
        open={testingModalOpen}
        modalHeading={
          isBulkMode
            ? intl.formatMessage(
                {
                  id: "pathology.modal.testing.bulkTitle",
                  defaultMessage: "Add Tests - {count} Samples",
                },
                { count: selectedSampleIds.length },
              )
            : intl.formatMessage(
                {
                  id: "pathology.modal.testing.title",
                  defaultMessage: "Add Test - {accession}",
                },
                { accession: selectedSample?.accessionNumber || "" },
              )
        }
        primaryButtonText={intl.formatMessage({
          id: "label.button.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setTestingModalOpen(false);
          setSelectedSample(null);
          setIsBulkMode(false);
          setError(null);
        }}
        onRequestSubmit={handleSubmitTest}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
          {/* Show selected samples in bulk mode */}
          {isBulkMode && (
            <div style={{ marginBottom: "1.5rem" }}>
              <p style={{ marginBottom: "0.5rem", fontWeight: "bold" }}>
                <FormattedMessage
                  id="pathology.modal.testing.selectedSamples"
                  defaultMessage="Selected Samples:"
                />
              </p>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                {getSelectedSamples().map((sample) => (
                  <Tag key={sample.id} type="blue" size="sm">
                    {sample.accessionNumber || sample.id}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          <Tabs>
            <TabList aria-label="Testing sections">
              <Tab>
                <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.staining"
                  defaultMessage="Staining"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="pathology.testing.tab.advanced"
                  defaultMessage="Advanced Techniques"
                />
              </Tab>
              <Tab>
                <Microscope size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.microscopy"
                  defaultMessage="Microscopy"
                />
              </Tab>
              <Tab>
                <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.controls"
                  defaultMessage="Controls"
                />
              </Tab>
              <Tab>
                <DocumentView size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.documentation"
                  defaultMessage="Documentation"
                />
              </Tab>
            </TabList>

            <TabPanels>
              {/* === TAB 1: STAINING === */}
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.routineStaining"
                        defaultMessage="Routine Staining"
                      />
                    </h5>
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="routineStainingCategory"
                      name="routineStainingCategory"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.stainingCategory",
                        defaultMessage: "Specimen Category",
                      })}
                      value={testData.routineStainingCategory}
                      onChange={handleInputChange}
                    >
                      <SelectItem value="" text="-- Select Category --" />
                      <SelectItem value="histology" text="Histology (Tissue)" />
                      <SelectItem
                        value="cytology"
                        text="Cytology (Cells/Fluids)"
                      />
                      <SelectItem value="blood" text="Blood/Bone Marrow" />
                    </Select>
                  </Column>

                  {testData.routineStainingCategory && (
                    <Column lg={16} md={8} sm={4}>
                      <div style={{ marginTop: "1rem" }}>
                        <p style={{ marginBottom: "0.5rem", fontWeight: 600 }}>
                          <FormattedMessage
                            id="pathology.testing.selectRoutineStains"
                            defaultMessage="Select Routine Stains:"
                          />
                        </p>
                        <div
                          style={{
                            display: "flex",
                            flexWrap: "wrap",
                            gap: "1rem",
                          }}
                        >
                          {routineStainsByCategory[
                            testData.routineStainingCategory
                          ]?.map((stain) => (
                            <Checkbox
                              key={stain.id}
                              id={`routine-${stain.id}`}
                              labelText={stain.label}
                              checked={testData.routineStains.includes(
                                stain.id,
                              )}
                              onChange={(e) =>
                                handleMultiCheckbox(
                                  "routineStains",
                                  stain.id,
                                  e.target.checked,
                                )
                              }
                            />
                          ))}
                        </div>
                      </div>
                    </Column>
                  )}

                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.specialStains"
                        defaultMessage="Special Stains"
                      />
                    </h5>
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns:
                          "repeat(auto-fill, minmax(250px, 1fr))",
                        gap: "0.75rem",
                      }}
                    >
                      {specialStainOptions.map((stain) => (
                        <Checkbox
                          key={stain.id}
                          id={`special-${stain.id}`}
                          labelText={`${stain.label} - ${stain.indication}`}
                          checked={testData.specialStains.includes(stain.id)}
                          onChange={(e) =>
                            handleMultiCheckbox(
                              "specialStains",
                              stain.id,
                              e.target.checked,
                            )
                          }
                        />
                      ))}
                    </div>
                  </Column>

                  {testData.specialStains.length > 0 && (
                    <Column lg={16} md={8} sm={4}>
                      <TextInput
                        id="specialStainIndication"
                        name="specialStainIndication"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.specialStainIndication",
                          defaultMessage:
                            "Clinical Indication for Special Stain",
                        })}
                        value={testData.specialStainIndication}
                        onChange={handleInputChange}
                        placeholder="e.g., Suspected fungal infection, rule out TB"
                        style={{ marginTop: "1rem" }}
                      />
                    </Column>
                  )}
                </Grid>
              </TabPanel>

              {/* === TAB 2: ADVANCED TECHNIQUES === */}
              <TabPanel>
                <Grid fullWidth>
                  {/* IHC/ICC Section */}
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "1rem",
                        marginBottom: "1rem",
                      }}
                    >
                      <h5 style={{ margin: 0 }}>
                        <FormattedMessage
                          id="pathology.testing.ihcIcc"
                          defaultMessage="IHC/ICC (Immunohistochemistry/Immunocytochemistry)"
                        />
                      </h5>
                      <Toggle
                        id="ihcIccPerformed"
                        labelA="Off"
                        labelB="On"
                        toggled={testData.ihcIccPerformed}
                        onToggle={(checked) =>
                          handleToggleChange("ihcIccPerformed", checked)
                        }
                      />
                    </div>
                  </Column>

                  {testData.ihcIccPerformed && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="ihcIccSampleType"
                          name="ihcIccSampleType"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccSampleType",
                            defaultMessage: "Sample Type",
                          })}
                          value={testData.ihcIccSampleType}
                          onChange={handleInputChange}
                        >
                          <SelectItem value="" text="-- Select Type --" />
                          <SelectItem
                            value="FFPE"
                            text="FFPE Blocks (Formalin-Fixed Paraffin-Embedded)"
                          />
                          <SelectItem value="cell_block" text="Cell Blocks" />
                          <SelectItem
                            value="cytology_smear"
                            text="Cytology Smears"
                          />
                          <SelectItem value="frozen" text="Frozen Sections" />
                        </Select>
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="ihcIccMarkers"
                          name="ihcIccMarkers"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccMarkers",
                            defaultMessage:
                              "Markers/Antigens (comma-separated)",
                          })}
                          value={testData.ihcIccMarkers}
                          onChange={handleInputChange}
                          placeholder="e.g., Ki-67, ER, PR, HER2"
                          helperText={`Common: ${commonIhcMarkers.slice(0, 5).join(", ")}...`}
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="ihcIccPrimaryAntibody"
                          name="ihcIccPrimaryAntibody"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccPrimaryAntibody",
                            defaultMessage: "Primary Antibody",
                          })}
                          value={testData.ihcIccPrimaryAntibody}
                          onChange={handleInputChange}
                        />
                      </Column>

                      <Column lg={4} md={2} sm={4}>
                        <TextInput
                          id="ihcIccClone"
                          name="ihcIccClone"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccClone",
                            defaultMessage: "Clone",
                          })}
                          value={testData.ihcIccClone}
                          onChange={handleInputChange}
                        />
                      </Column>

                      <Column lg={4} md={2} sm={4}>
                        <TextInput
                          id="ihcIccDilution"
                          name="ihcIccDilution"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccDilution",
                            defaultMessage: "Dilution",
                          })}
                          value={testData.ihcIccDilution}
                          onChange={handleInputChange}
                          placeholder="e.g., 1:100"
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="ihcIccAntigenRetrieval"
                          name="ihcIccAntigenRetrieval"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccAntigenRetrieval",
                            defaultMessage: "Antigen Retrieval Method",
                          })}
                          value={testData.ihcIccAntigenRetrieval}
                          onChange={handleInputChange}
                        >
                          <SelectItem value="" text="-- Select Method --" />
                          <SelectItem
                            value="heat_citrate"
                            text="Heat-Induced (Citrate Buffer pH 6)"
                          />
                          <SelectItem
                            value="heat_edta"
                            text="Heat-Induced (EDTA Buffer pH 9)"
                          />
                          <SelectItem
                            value="enzyme_proteinase"
                            text="Enzymatic (Proteinase K)"
                          />
                          <SelectItem
                            value="enzyme_trypsin"
                            text="Enzymatic (Trypsin)"
                          />
                          <SelectItem value="none" text="None Required" />
                        </Select>
                      </Column>
                    </>
                  )}

                  {/* ISH Section */}
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "1rem",
                        marginTop: "2rem",
                        marginBottom: "1rem",
                      }}
                    >
                      <h5 style={{ margin: 0 }}>
                        <FormattedMessage
                          id="pathology.testing.ish"
                          defaultMessage="ISH (In Situ Hybridization)"
                        />
                      </h5>
                      <Toggle
                        id="ishPerformed"
                        labelA="Off"
                        labelB="On"
                        toggled={testData.ishPerformed}
                        onToggle={(checked) =>
                          handleToggleChange("ishPerformed", checked)
                        }
                      />
                    </div>
                  </Column>

                  {testData.ishPerformed && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <RadioButtonGroup
                          legendText={intl.formatMessage({
                            id: "pathology.testing.ishTargetType",
                            defaultMessage: "Target Type",
                          })}
                          name="ishTargetType"
                          valueSelected={testData.ishTargetType}
                          onChange={(value) =>
                            setTestData((prev) => ({
                              ...prev,
                              ishTargetType: value,
                            }))
                          }
                        >
                          <RadioButton
                            labelText="DNA"
                            value="DNA"
                            id="ish-dna"
                          />
                          <RadioButton
                            labelText="RNA"
                            value="RNA"
                            id="ish-rna"
                          />
                        </RadioButtonGroup>
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="ishTargetSequence"
                          name="ishTargetSequence"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ishTargetSequence",
                            defaultMessage: "Target Sequence/Gene",
                          })}
                          value={testData.ishTargetSequence}
                          onChange={handleInputChange}
                          placeholder="e.g., HPV, EBER, HER2"
                        />
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="ishProbeDetails"
                          name="ishProbeDetails"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ishProbeDetails",
                            defaultMessage: "Probe Details",
                          })}
                          value={testData.ishProbeDetails}
                          onChange={handleInputChange}
                          rows={2}
                          placeholder="Probe manufacturer, catalog number, etc."
                        />
                      </Column>
                    </>
                  )}

                  {/* Research Assays */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.researchAssays"
                        defaultMessage="Research Assays"
                      />
                    </h5>
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        flexWrap: "wrap",
                        gap: "1rem",
                      }}
                    >
                      {researchAssayOptions.map((assay) => (
                        <Checkbox
                          key={assay.id}
                          id={`research-${assay.id}`}
                          labelText={assay.label}
                          checked={testData.researchAssays.includes(assay.id)}
                          onChange={(e) =>
                            handleMultiCheckbox(
                              "researchAssays",
                              assay.id,
                              e.target.checked,
                            )
                          }
                        />
                      ))}
                    </div>
                  </Column>

                  {testData.researchAssays.length > 0 && (
                    <Column lg={16} md={8} sm={4}>
                      <TextArea
                        id="researchAssayDetails"
                        name="researchAssayDetails"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.researchAssayDetails",
                          defaultMessage: "Research Assay Details",
                        })}
                        value={testData.researchAssayDetails}
                        onChange={handleInputChange}
                        rows={3}
                        placeholder="Protocol details, parameters, etc."
                        style={{ marginTop: "1rem" }}
                      />
                    </Column>
                  )}
                </Grid>
              </TabPanel>

              {/* === TAB 3: MICROSCOPY OBSERVATION === */}
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "1rem",
                        marginBottom: "1rem",
                      }}
                    >
                      <h5 style={{ margin: 0 }}>
                        <FormattedMessage
                          id="pathology.testing.microscopyObservation"
                          defaultMessage="Microscopy Observation"
                        />
                      </h5>
                      <Toggle
                        id="microscopyPerformed"
                        labelA="Not Done"
                        labelB="Performed"
                        toggled={testData.microscopyPerformed}
                        onToggle={(checked) =>
                          handleToggleChange("microscopyPerformed", checked)
                        }
                      />
                    </div>
                    <p
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        marginBottom: "1rem",
                      }}
                    >
                      <FormattedMessage
                        id="pathology.testing.microscopyObservation.help"
                        defaultMessage="Performed by pathologist or PI (for research). Findings must be documented."
                      />
                    </p>
                  </Column>

                  {testData.microscopyPerformed && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="microscopist"
                          name="microscopist"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.microscopist",
                            defaultMessage: "Microscopist (Pathologist/PI)",
                          })}
                          value={testData.microscopist}
                          onChange={handleInputChange}
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="microscopeType"
                          name="microscopeType"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.microscopeType",
                            defaultMessage: "Microscope Type",
                          })}
                          value={testData.microscopeType}
                          onChange={handleInputChange}
                        >
                          <SelectItem value="" text="-- Select Type --" />
                          <SelectItem value="brightfield" text="Brightfield" />
                          <SelectItem
                            value="fluorescence"
                            text="Fluorescence"
                          />
                          <SelectItem value="confocal" text="Confocal" />
                          <SelectItem
                            value="phase_contrast"
                            text="Phase Contrast"
                          />
                          <SelectItem
                            value="polarized"
                            text="Polarized Light"
                          />
                          <SelectItem value="darkfield" text="Darkfield" />
                        </Select>
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="magnificationUsed"
                          name="magnificationUsed"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.magnificationUsed",
                            defaultMessage: "Magnification(s) Used",
                          })}
                          value={testData.magnificationUsed}
                          onChange={handleInputChange}
                          placeholder="e.g., 4x, 10x, 40x, 100x (oil)"
                        />
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <Checkbox
                          id="findingsDocumented"
                          name="findingsDocumented"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.findingsDocumented",
                            defaultMessage: "Findings Documented",
                          })}
                          checked={testData.findingsDocumented}
                          onChange={handleInputChange}
                          style={{ marginTop: "1rem" }}
                        />
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="microscopicFindings"
                          name="microscopicFindings"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.microscopicFindings",
                            defaultMessage: "Microscopic Findings",
                          })}
                          value={testData.microscopicFindings}
                          onChange={handleInputChange}
                          rows={5}
                          placeholder="Describe morphological features, staining patterns, cellular characteristics, etc."
                          style={{ marginTop: "1rem" }}
                        />
                      </Column>
                    </>
                  )}
                </Grid>
              </TabPanel>

              {/* === TAB 4: CONTROLS & VALIDATION === */}
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "pathology.testing.controls.requirement",
                        defaultMessage:
                          "For IHC/ICC/ISH: Controls must be run with each batch. Both positive and negative controls must pass for assay acceptance.",
                      })}
                      hideCloseButton
                      lowContrast
                      style={{ marginBottom: "1rem" }}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="batchNumber"
                      name="batchNumber"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.batchNumber",
                        defaultMessage: "Batch Number",
                      })}
                      value={testData.batchNumber}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) => handleDateChange(dates, "batchDate")}
                    >
                      <DatePickerInput
                        id="batchDate"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.batchDate",
                          defaultMessage: "Batch Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>

                  {/* Positive Control */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.positiveControl"
                        defaultMessage="Positive Control"
                      />
                    </h5>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <Checkbox
                      id="positiveControlRun"
                      name="positiveControlRun"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlRun",
                        defaultMessage: "Positive Control Run",
                      })}
                      checked={testData.positiveControlRun}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <TextInput
                      id="positiveControlTissue"
                      name="positiveControlTissue"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlTissue",
                        defaultMessage: "Control Tissue/Sample",
                      })}
                      value={testData.positiveControlTissue}
                      onChange={handleInputChange}
                      disabled={!testData.positiveControlRun}
                      placeholder="e.g., Tonsil, Placenta"
                    />
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <Select
                      id="positiveControlResult"
                      name="positiveControlResult"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlResult",
                        defaultMessage: "Result",
                      })}
                      value={testData.positiveControlResult}
                      onChange={handleInputChange}
                      disabled={!testData.positiveControlRun}
                    >
                      <SelectItem value="" text="-- Select --" />
                      <SelectItem value="pass" text="Pass" />
                      <SelectItem value="fail" text="Fail" />
                    </Select>
                  </Column>

                  {/* Negative Control */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.negativeControl"
                        defaultMessage="Negative Control"
                      />
                    </h5>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <Checkbox
                      id="negativeControlRun"
                      name="negativeControlRun"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlRun",
                        defaultMessage: "Negative Control Run",
                      })}
                      checked={testData.negativeControlRun}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <Select
                      id="negativeControlType"
                      name="negativeControlType"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlType",
                        defaultMessage: "Control Type",
                      })}
                      value={testData.negativeControlType}
                      onChange={handleInputChange}
                      disabled={!testData.negativeControlRun}
                    >
                      <SelectItem value="" text="-- Select --" />
                      <SelectItem
                        value="omit_primary"
                        text="Omit Primary Antibody"
                      />
                      <SelectItem
                        value="irrelevant_antibody"
                        text="Isotype Control"
                      />
                      <SelectItem
                        value="known_negative"
                        text="Known Negative Tissue"
                      />
                    </Select>
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <Select
                      id="negativeControlResult"
                      name="negativeControlResult"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlResult",
                        defaultMessage: "Result",
                      })}
                      value={testData.negativeControlResult}
                      onChange={handleInputChange}
                      disabled={!testData.negativeControlRun}
                    >
                      <SelectItem value="" text="-- Select --" />
                      <SelectItem value="pass" text="Pass" />
                      <SelectItem value="fail" text="Fail" />
                    </Select>
                  </Column>

                  {/* Acceptance */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.acceptance"
                        defaultMessage="Assay Acceptance"
                      />
                    </h5>
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="controlsAccepted"
                      name="controlsAccepted"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.controlsAccepted",
                        defaultMessage: "Controls Passed",
                      })}
                      checked={testData.controlsAccepted}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="stainQualityAdequate"
                      name="stainQualityAdequate"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.stainQualityAdequate",
                        defaultMessage: "Stain Quality Adequate",
                      })}
                      checked={testData.stainQualityAdequate}
                      onChange={handleInputChange}
                    />
                  </Column>
                </Grid>
              </TabPanel>

              {/* === TAB 5: DOCUMENTATION === */}
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.requisitionLogbook"
                        defaultMessage="Test Requisition & Result Logbook"
                      />
                    </h5>
                  </Column>

                  {!isBulkMode && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="accessionNumber"
                          name="accessionNumber"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.accessionNumber",
                            defaultMessage: "Accession Number",
                          })}
                          value={testData.accessionNumber}
                          onChange={handleInputChange}
                          disabled
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="blockSlideId"
                          name="blockSlideId"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.blockSlideId",
                            defaultMessage: "Block/Slide ID *",
                          })}
                          value={testData.blockSlideId}
                          onChange={handleInputChange}
                        />
                      </Column>
                    </>
                  )}

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="testName"
                      name="testName"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.testName",
                        defaultMessage: "Test Name *",
                      })}
                      value={testData.testName}
                      onChange={handleInputChange}
                      placeholder="e.g., H&E, Ki-67 IHC, AFB stain"
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="testType"
                      name="testType"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.testType",
                        defaultMessage: "Test Type",
                      })}
                      value={testData.testType}
                      onChange={handleInputChange}
                    >
                      <SelectItem value="" text="-- Select Type --" />
                      <SelectItem value="HE" text="H&E (Routine)" />
                      <SelectItem value="special_stain" text="Special Stain" />
                      <SelectItem value="IHC" text="IHC Marker" />
                      <SelectItem value="ICC" text="ICC Marker" />
                      <SelectItem value="ISH" text="ISH" />
                      <SelectItem value="FISH" text="FISH" />
                      <SelectItem value="research" text="Research Assay" />
                    </Select>
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <TextInput
                      id="controlsUsedSummary"
                      name="controlsUsedSummary"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.controlsUsedSummary",
                        defaultMessage: "Controls Used (Summary)",
                      })}
                      value={testData.controlsUsedSummary}
                      onChange={handleInputChange}
                      placeholder="e.g., Pos: Tonsil (Pass), Neg: No primary (Pass)"
                      style={{ marginTop: "1rem" }}
                    />
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.technicianSignoff"
                        defaultMessage="Technician Sign-off"
                      />
                    </h5>
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="technicianSignature"
                      name="technicianSignature"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.technicianSignature",
                        defaultMessage: "Technician Signature *",
                      })}
                      value={testData.technicianSignature}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) =>
                        handleDateChange(dates, "technicianDate")
                      }
                    >
                      <DatePickerInput
                        id="technicianDate"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.technicianDate",
                          defaultMessage: "Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>
                </Grid>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </div>
      </Modal>

      {/* Enter Results Modal */}
      <Modal
        open={resultsModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.results.title",
            defaultMessage: "Enter Results - {count} Samples",
          },
          { count: getSamplesWithTests().length },
        )}
        primaryButtonText={
          resultsEntryMode === "csv"
            ? intl.formatMessage({
                id: "pathology.results.import",
                defaultMessage: "Import",
              })
            : intl.formatMessage({
                id: "label.button.submit",
                defaultMessage: "Submit",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setResultsModalOpen(false);
          resetResultsData();
          setError(null);
        }}
        onRequestSubmit={
          resultsEntryMode === "csv" ? handleCsvImport : handleSubmitResults
        }
        primaryButtonDisabled={
          submitting || isImporting || (resultsEntryMode === "csv" && !csvFile)
        }
        size="lg"
      >
        <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
          {/* Mode Selection */}
          <div style={{ marginBottom: "1.5rem" }}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "pathology.results.entryMode",
                defaultMessage: "Entry Mode",
              })}
              name="resultsEntryMode"
              valueSelected={resultsEntryMode}
              onChange={(value) => setResultsEntryMode(value)}
            >
              <RadioButton
                labelText={intl.formatMessage({
                  id: "pathology.results.manualEntry",
                  defaultMessage: "Manual Entry",
                })}
                value="manual"
                id="results-manual"
              />
              <RadioButton
                labelText={intl.formatMessage({
                  id: "pathology.results.csvImport",
                  defaultMessage: "CSV Import",
                })}
                value="csv"
                id="results-csv"
              />
            </RadioButtonGroup>
          </div>

          {/* Selected samples display */}
          <div style={{ marginBottom: "1.5rem" }}>
            <p style={{ marginBottom: "0.5rem", fontWeight: "bold" }}>
              <FormattedMessage
                id="pathology.results.samplesWithTests"
                defaultMessage="Samples with Tests:"
              />
            </p>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
              {getSamplesWithTests().map((sample) => (
                <Tag key={sample.id} type="blue" size="sm">
                  {sample.accessionNumber || sample.id}
                </Tag>
              ))}
            </div>
          </div>

          {resultsEntryMode === "manual" ? (
            /* Manual Entry Form */
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="resultFindings"
                  name="resultFindings"
                  labelText={intl.formatMessage({
                    id: "pathology.results.resultFindings",
                    defaultMessage: "Result Findings *",
                  })}
                  value={resultsData.resultFindings}
                  onChange={handleResultsInputChange}
                  rows={4}
                  placeholder="Enter the test results/findings..."
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="diagnosisCode"
                  name="diagnosisCode"
                  labelText={intl.formatMessage({
                    id: "pathology.results.diagnosisCode",
                    defaultMessage: "Diagnosis Code (ICD-10/SNOMED)",
                  })}
                  value={resultsData.diagnosisCode}
                  onChange={handleResultsInputChange}
                  placeholder="e.g., C50.9, M8500/3"
                  style={{ marginTop: "1rem" }}
                />
              </Column>

              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="clinicalInterpretation"
                  name="clinicalInterpretation"
                  labelText={intl.formatMessage({
                    id: "pathology.results.clinicalInterpretation",
                    defaultMessage: "Clinical Interpretation / Diagnosis",
                  })}
                  value={resultsData.clinicalInterpretation}
                  onChange={handleResultsInputChange}
                  rows={4}
                  placeholder="Final pathological diagnosis and interpretation..."
                  style={{ marginTop: "1rem" }}
                />
              </Column>

              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.results.pathologistVerification"
                    defaultMessage="Pathologist Verification"
                  />
                </h5>
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage({
                    id: "pathology.results.verificationRequired",
                    defaultMessage:
                      "Clinical results must be verified by a certified/licensed pathologist before release.",
                  })}
                  hideCloseButton
                  lowContrast
                  style={{ marginBottom: "1rem" }}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <Checkbox
                  id="verifiedByPathologist"
                  name="verifiedByPathologist"
                  labelText={intl.formatMessage({
                    id: "pathology.results.verifiedByPathologist",
                    defaultMessage: "Verified by Certified Pathologist",
                  })}
                  checked={resultsData.verifiedByPathologist}
                  onChange={handleResultsInputChange}
                />
              </Column>

              {resultsData.verifiedByPathologist && (
                <>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="verifyingPathologistName"
                      name="verifyingPathologistName"
                      labelText={intl.formatMessage({
                        id: "pathology.results.verifyingPathologistName",
                        defaultMessage: "Verifying Pathologist Name",
                      })}
                      value={resultsData.verifyingPathologistName}
                      onChange={handleResultsInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) =>
                        handleDateChange(dates, "verificationDate", true)
                      }
                    >
                      <DatePickerInput
                        id="verificationDate"
                        labelText={intl.formatMessage({
                          id: "pathology.results.verificationDate",
                          defaultMessage: "Verification Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="pathologistSignature"
                      name="pathologistSignature"
                      labelText={intl.formatMessage({
                        id: "pathology.results.pathologistSignature",
                        defaultMessage: "Pathologist Signature",
                      })}
                      value={resultsData.pathologistSignature}
                      onChange={handleResultsInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) =>
                        handleDateChange(dates, "pathologistDate", true)
                      }
                    >
                      <DatePickerInput
                        id="pathologistDate"
                        labelText={intl.formatMessage({
                          id: "pathology.results.pathologistDate",
                          defaultMessage: "Pathologist Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>
                </>
              )}

              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="additionalNotes"
                  name="additionalNotes"
                  labelText={intl.formatMessage({
                    id: "pathology.results.additionalNotes",
                    defaultMessage: "Additional Notes",
                  })}
                  value={resultsData.additionalNotes}
                  onChange={handleResultsInputChange}
                  rows={3}
                  style={{ marginTop: "1rem" }}
                />
              </Column>
            </Grid>
          ) : (
            /* CSV Import */
            <div>
              <p style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="pathology.results.csvDescription"
                  defaultMessage="Upload a CSV file with results for multiple samples. The CSV must include accessionNumber or blockSlideId to match samples."
                />
              </p>

              <Button
                kind="ghost"
                size="sm"
                renderIcon={Download}
                onClick={handleDownloadResultsTemplate}
                style={{ marginBottom: "1rem" }}
              >
                <FormattedMessage
                  id="pathology.results.downloadTemplate"
                  defaultMessage="Download CSV Template"
                />
              </Button>

              {!csvFile ? (
                <FileUploaderDropContainer
                  accept={[".csv"]}
                  labelText={intl.formatMessage({
                    id: "pathology.results.dropzone",
                    defaultMessage:
                      "Drag and drop a CSV file here or click to upload",
                  })}
                  onAddFiles={handleCsvFileAdded}
                />
              ) : (
                <FileUploaderItem
                  name={csvFile.name}
                  status="edit"
                  onDelete={() => {
                    setCsvFile(null);
                    setCsvPreview(null);
                    setCsvErrors([]);
                  }}
                />
              )}

              {csvErrors.length > 0 && (
                <InlineNotification
                  kind="error"
                  title="Validation Errors"
                  subtitle={
                    <ul style={{ margin: 0, paddingLeft: "1rem" }}>
                      {csvErrors.map((err, idx) => (
                        <li key={idx}>{err.message}</li>
                      ))}
                    </ul>
                  }
                  hideCloseButton
                  lowContrast
                  style={{ marginTop: "1rem" }}
                />
              )}

              {csvPreview && csvErrors.length === 0 && (
                <div style={{ marginTop: "1rem" }}>
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      marginBottom: "0.5rem",
                    }}
                  >
                    <Tag type="blue">{csvPreview.totalRows} rows</Tag>
                    <Tag type="green">Ready to import</Tag>
                  </div>
                  <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                    <FormattedMessage
                      id="pathology.results.csvPreviewColumns"
                      defaultMessage="Columns: {columns}"
                      values={{ columns: csvPreview.headers.join(", ") }}
                    />
                  </p>
                </div>
              )}

              {isImporting && (
                <Loading
                  withOverlay={false}
                  description="Importing results..."
                />
              )}

              {/* Example CSV format */}
              <div
                style={{
                  marginTop: "1.5rem",
                  padding: "1rem",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "0.5rem",
                  }}
                >
                  <p style={{ fontWeight: "bold", margin: 0 }}>
                    <FormattedMessage
                      id="pathology.results.csvExample.title"
                      defaultMessage="Example CSV format:"
                    />
                  </p>
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={Download}
                    onClick={() => {
                      const csvContent = `accessionNumber,blockSlideId,resultFindings,diagnosisCode,clinicalInterpretation,verifiedByPathologist,verifyingPathologistName,verificationDate,additionalNotes
ACC-2024-001,BLK-001-A,"Positive for malignancy",C50.9,"Invasive ductal carcinoma",true,Dr. Smith,2024-01-15,""
ACC-2024-002,BLK-002-A,"Negative for malignancy",,"Benign changes",true,Dr. Jones,2024-01-15,""`;
                      const blob = new Blob([csvContent], {
                        type: "text/csv;charset=utf-8;",
                      });
                      const url = URL.createObjectURL(blob);
                      const link = document.createElement("a");
                      link.href = url;
                      link.download = "pathology_results_template.csv";
                      document.body.appendChild(link);
                      link.click();
                      document.body.removeChild(link);
                      URL.revokeObjectURL(url);
                    }}
                  >
                    <FormattedMessage
                      id="pathology.results.downloadTemplate"
                      defaultMessage="Download Template"
                    />
                  </Button>
                </div>
                <code
                  style={{
                    fontSize: "0.75rem",
                    display: "block",
                    backgroundColor: "#e0e0e0",
                    padding: "0.5rem",
                    overflowX: "auto",
                    whiteSpace: "pre",
                  }}
                >
                  {`accessionNumber,blockSlideId,resultFindings,diagnosisCode,clinicalInterpretation,verifiedByPathologist,verifyingPathologistName,verificationDate,additionalNotes
ACC-2024-001,BLK-001-A,"Positive for malignancy",C50.9,"Invasive ductal carcinoma",true,Dr. Smith,2024-01-15,""
ACC-2024-002,BLK-002-A,"Negative for malignancy",,"Benign changes",true,Dr. Jones,2024-01-15,""`}
                </code>
              </div>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default PathologyTestingMicroscopyPage;
