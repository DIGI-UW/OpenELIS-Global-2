import React, {
  useState,
  useCallback,
  useEffect,
  useContext,
  useRef,
} from "react";
import {
  Grid,
  Column,
  Button,
  Loading,
  Tabs,
  TabList,
  Tab,
  TabPanel,
  TabPanels,
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
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Select,
  SelectItem,
  FileUploader,
  Checkbox,
  TextInput,
  TextArea,
  Modal,
  DatePickerInput,
  Tag,
} from "@carbon/react";
import { DocumentAdd, Upload } from "@carbon/react/icons";
import { FormattedMessage } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import config from "../../../../config.json";

/**
 * Analytical methods available for bioanalytical testing
 */
const ANALYTICAL_METHODS = [
  {
    id: "HPLC_UV_VIS",
    name: "HPLC / UV-Vis",
    description:
      "High Performance Liquid Chromatography with UV-Visible detection",
  },
  {
    id: "LC_MS_MS",
    name: "LC-MS/MS",
    description: "Liquid Chromatography with Tandem Mass Spectrometry",
  },
  {
    id: "DISSOLUTION_USP",
    name: "Dissolution (USP I/II)",
    description:
      "Dissolution testing using USP Apparatus I (Basket) or II (Paddle)",
  },
  {
    id: "PHYSICAL_TESTING",
    name: "Hardness / Friability / Disintegration Test",
    description: "Physical testing for pharmaceutical dosage forms",
  },
  {
    id: "IDENTITY_TEST",
    name: "Identity Test",
    description: "Verification of pharmaceutical substances and products",
  },
];

/**
 * Bioanalytical Analyzers
 */
const BIOANALYTICAL_ANALYZERS = [
  { id: "1", machine: "LC-MS/MS", formats: ["MZML", "CDF"] },
  { id: "2", machine: "HPLC", formats: ["CSV", "PDF"] },
  { id: "3", machine: "Dissolution Apparatus", formats: ["CSV"] },
  { id: "4", machine: "Disintegration Tester", formats: ["CSV"] },
  { id: "5", machine: "Hardness Tester", formats: ["CSV"] },
  { id: "6", machine: "Friability Tester", formats: ["CSV"] },
  { id: "7", machine: "Melting Point Apparatus", formats: ["CSV"] },
  { id: "8", machine: "Water Content Analyzer", formats: ["CSV"] },
  { id: "9", machine: "Viscosity Meter", formats: ["CSV"] },
  { id: "10", machine: "pH Meter", formats: ["CSV"] },
  { id: "11", machine: "Karl Fischer Titrator", formats: ["CSV"] },
];

/**
 * Stage 3: Analytical Test Execution - Clean Implementation
 * Features:
 * - Proper Carbon DataTable with selection
 * - Clean tab-based data fetching
 * - Proper QC results handling without mock data
 * - Persistent execution data
 * - Default Carbon Design System styling
 */
function BioanalyticalAnalyticalExecutionPage({
  pageData,
  onProgressUpdate,
  templateInstruments,
  entryId,
}) {
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const isMountedRef = useRef(true);

  // ============================================================================
  // CORE STATE
  // ============================================================================

  // UI State
  const [isLoading, setIsLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);

  // Sample Data
  const [assignedSamples, setAssignedSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Execution Data
  const [executionData, setExecutionData] = useState({
    analystId: "",
    executionDate: new Date().toISOString().split("T")[0],
    selectedInstrument: "",
    notes: "",
    isExecuting: false,
  });

  // QC Data (fetched fresh per tab)
  const [qcResults, setQcResults] = useState([]);
  const [calibrationData, setCalibrationData] = useState(null);
  const [quantificationResults, setQuantificationResults] = useState([]);
  const [qcApproved, setQcApproved] = useState(false);

  // Deviations
  const [deviations, setDeviations] = useState([]);

  // File Upload
  const [uploadedFiles, setUploadedFiles] = useState([]);

  // Modals
  const [isExecutionModalOpen, setIsExecutionModalOpen] = useState(false);
  const [isDeviationModalOpen, setIsDeviationModalOpen] = useState(false);
  const [deviationForm, setDeviationForm] = useState({
    type: "",
    description: "",
    correctiveAction: "",
  });

  // Notebook ID for stage advancement
  const [notebookId, setNotebookId] = useState(null);

  // ============================================================================
  // UTILITY FUNCTIONS
  // ============================================================================

  // Notification helper following established patterns (same as other bioanalytical pages)
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // ============================================================================
  // DATA FETCHING
  // ============================================================================

  const fetchSamples = useCallback(async () => {
    if (!pageData?.id) return;

    try {
      setIsLoading(true);
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`,
        { credentials: "include" },
      );

      if (!response.ok) {
        throw new Error("Failed to load samples for Stage 3");
      }

      const data = await response.json();
      const samples = Array.isArray(data) ? data : [];

      console.log("Stage 3 - Loaded samples:", samples);

      // Clean and structure sample data - preserving all previous stage data
      const cleanSamples = samples.map((sample) => ({
        id: sample.id,
        accessionNumber: sample.accessionNumber,
        // Use sample.data?.sampleType first (from previous stages), fallback to direct properties
        sampleType:
          sample.data?.sampleType ||
          sample.sampleType ||
          sample.typeOfSample?.type ||
          "-",
        sampleItemId: sample.sampleItemId || sample.id,
        assignedStaff:
          sample.assignedStaff || sample.data?.assignedStaff || "-",
        // Fix: Use sample.data?.analyticalMethod (not assignedMethod)
        assignedMethod:
          sample.assignedMethod ||
          sample.data?.analyticalMethod ||
          sample.data?.assignedMethod ||
          "Not assigned",
        instrumentName:
          sample.instrumentName || sample.data?.instrumentName || "-",
        instrumentId: sample.instrumentId || sample.data?.instrumentId || null,
        // Preserve all data from previous stages
        data: sample.data || {},
      }));

      setAssignedSamples(cleanSamples);

      // Load existing execution data if present
      if (cleanSamples.length > 0) {
        const firstSample = cleanSamples[0];
        if (firstSample.data?.executionData) {
          setExecutionData((prev) => ({
            ...prev,
            ...firstSample.data.executionData,
          }));
        }

        // Load QC data if present in any sample
        const qcData = cleanSamples.find((s) => s.data?.qcResults)?.data;
        if (qcData) {
          setQcResults(qcData.qcResults || []);
          setCalibrationData(qcData.calibrationData || null);
          setQuantificationResults(qcData.quantificationResults || []);
          setQcApproved(qcData.qcApproved || false);
        }
      }
    } catch (error) {
      console.error("Error loading samples:", error);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        message: "Failed to load samples for Stage 3",
      });
    } finally {
      setIsLoading(false);
    }
  }, [pageData?.id, notify]);

  const refreshTabData = useCallback(
    async (tabIndex) => {
      console.log(`Refreshing data for tab ${tabIndex}`);

      // Always fetch fresh sample data when switching tabs
      await fetchSamples();

      // Tab-specific data refresh logic can be added here
      switch (tabIndex) {
        case 0: // Test Execution
          // Data already refreshed by fetchSamples
          break;
        case 1: // QC Verification
          // QC data already loaded by fetchSamples
          break;
        case 2: // Deviations & Completion
          // Deviations loaded with sample data
          break;
        default:
          break;
      }
    },
    [fetchSamples],
  );

  // ============================================================================
  // EVENT HANDLERS
  // ============================================================================

  const handleTabChange = useCallback(
    (evt) => {
      const newTabIndex = evt.selectedIndex;
      setSelectedTab(newTabIndex);
      refreshTabData(newTabIndex);
    },
    [refreshTabData],
  );

  const handleFileUpload = useCallback(
    async (event) => {
      const files = Array.from(event.target.files);
      if (files.length === 0) return;

      if (!pageData?.id) {
        notify({
          kind: NotificationKinds.error,
          title: "Error",
          message: "Page ID not found",
        });
        return;
      }

      if (!executionData.selectedInstrument) {
        notify({
          kind: NotificationKinds.warning,
          title: "Warning",
          message: "Please select an instrument first",
        });
        return;
      }

      try {
        const uploadPromises = files.map(async (file) => {
          const formData = new FormData();
          formData.append("file", file);
          formData.append("instrumentId", executionData.selectedInstrument);
          formData.append("pageId", pageData.id.toString());
          formData.append("entryId", entryId || pageData.id.toString());

          const response = await fetch(
            `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/files/upload`,
            {
              method: "POST",
              credentials: "include",
              headers: {
                "X-CSRF-Token": localStorage.getItem("CSRF"),
              },
              body: formData,
            },
          );

          if (!response.ok) {
            throw new Error(`Upload failed for ${file.name}`);
          }

          const result = await response.json();

          return {
            id: result.fileId,
            name: file.name,
            size: file.size,
            fileName: result.fileName,
            filePath: result.filePath,
            uploaded: true,
            processed: false,
            analyzerResultsCount: 0,
          };
        });

        const uploadedFileResults = await Promise.all(uploadPromises);
        setUploadedFiles((prev) => [...prev, ...uploadedFileResults]);
        notify({
          kind: NotificationKinds.success,
          title: "Success",
          message: `${files.length} file(s) uploaded successfully`,
        });
      } catch (error) {
        console.error("File upload error:", error);
        notify({
          kind: NotificationKinds.error,
          title: "Error",
          message: `File upload failed: ${error.message}`,
        });
      }
    },
    [notify, pageData?.id, executionData.selectedInstrument, entryId],
  );

  const handleProcessFiles = useCallback(
    async (fileIds) => {
      if (!pageData?.id) {
        notify({
          kind: NotificationKinds.error,
          title: "Error",
          message: "Page ID not found",
        });
        return;
      }

      try {
        // Prepare samples data for processing request
        const samplesData = selectedSampleIds.map((sampleId) => {
          const sample = assignedSamples.find((s) => s.id === sampleId);
          return {
            id: sample?.id || sampleId,
            accessionNumber: sample?.accessionNumber,
            sampleId: sample?.sampleItemId,
            assignedMethod: sample?.assignedMethod,
            instrumentId: sample?.instrumentId,
          };
        });

        // Create FileProcessingRequest structure as expected by backend
        const processingRequest = {
          fileIds: fileIds,
          samples: samplesData,
        };

        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/files/process`,
          {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
              "X-CSRF-Token": localStorage.getItem("CSRF"),
            },
            body: JSON.stringify(processingRequest),
          },
        );

        if (!response.ok) {
          throw new Error("File processing failed");
        }

        const result = await response.json();

        // Update files to show they've been processed with actual results from backend
        setUploadedFiles((prev) =>
          prev.map((file) =>
            fileIds.includes(file.id)
              ? {
                  ...file,
                  processed: true,
                  analyzerResultsCount: result.analyzerResults?.length || 0,
                }
              : file,
          ),
        );

        // Store processed results in state for QC verification
        if (result.qcResults && result.qcResults.length > 0) {
          setQcResults(result.qcResults);
        }
        if (
          result.quantificationResults &&
          result.quantificationResults.length > 0
        ) {
          setQuantificationResults(result.quantificationResults);
        }
        if (result.calibrationData) {
          setCalibrationData(result.calibrationData);
        }

        const resultsCount = result.analyzerResults?.length || 0;
        notify(
          `Files processed successfully. ${resultsCount} results extracted.`,
        );
      } catch (error) {
        console.error("File processing error:", error);
        notify(
          `File processing failed: ${error.message}`,
          NotificationKinds.error,
        );
      }
    },
    [notify, pageData?.id, selectedSampleIds, assignedSamples],
  );

  const handleExecuteTest = useCallback(async () => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: "Warning",
        message: "Please select at least one sample",
      });
      return;
    }
    if (!executionData.selectedInstrument) {
      notify({
        kind: NotificationKinds.warning,
        title: "Warning",
        message: "Please select an instrument",
      });
      return;
    }
    if (!executionData.analystId) {
      notify({
        kind: NotificationKinds.warning,
        title: "Warning",
        message: "Please enter Analyst ID",
      });
      return;
    }

    try {
      setExecutionData((prev) => ({ ...prev, isExecuting: true }));

      const executionPayload = {
        sampleIds: selectedSampleIds,
        data: {
          executionStatus: "EXECUTED",
          executedAt: new Date().toISOString(),
          executedBy: executionData.analystId,
          instrumentId: executionData.selectedInstrument,
          executionDate: executionData.executionDate,
          notes: executionData.notes,
          uploadedFiles: uploadedFiles.map((file) => ({
            id: file.id,
            name: file.name,
            size: file.size,
            uploaded: file.uploaded,
            processed: file.processed,
            analyzerResultsCount: file.analyzerResultsCount,
          })),
        },
      };

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(executionPayload),
        },
      );

      if (!response.ok) throw new Error("Test execution failed");

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        message:
          "Test execution recorded successfully. Proceeding to QC Verification.",
      });
      setSelectedTab(1);
      await refreshTabData(1);
    } catch (error) {
      console.error("Execution error:", error);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        message: "Test execution failed",
      });
    } finally {
      setExecutionData((prev) => ({ ...prev, isExecuting: false }));
    }
  }, [
    selectedSampleIds,
    executionData,
    uploadedFiles,
    pageData?.id,
    notify,
    refreshTabData,
  ]);

  const handleCompleteExecution = useCallback(() => {
    // Validation: Check if samples are selected (same as Stage 1)
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        message: "Please select at least one sample.",
      });
      return;
    }

    // Validation: Check if page is properly initialized (same as Stage 1)
    if (!pageData?.id) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        message: "Cannot update samples: Page not properly initialized.",
      });
      return;
    }

    // Stage 3 specific validation: Check QC approval
    if (!qcApproved) {
      notify({
        kind: NotificationKinds.warning,
        title: "Warning",
        message: "Please approve QC results before completing execution.",
      });
      return;
    }

    // Set executing state
    setExecutionData((prev) => ({ ...prev, isExecuting: true }));

    // Get existing data from first selected sample to preserve all previous stage data
    const firstSelectedSample = assignedSamples.find((s) =>
      selectedSampleIds.includes(s.id),
    );
    const existingData = firstSelectedSample?.data || {};

    const completionPayload = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)), // Same format as Stage 1
      data: {
        // Preserve ALL existing data from previous stages (Stage 1 & 2)
        ...existingData,
        // Add Stage 3 execution data - Stage 4 compatible format
        executionStatus: "EXECUTED", // Stage 4 expects "EXECUTED" not "COMPLETED"
        completedAt: new Date().toISOString(),
        completedBy: executionData.analystId,
        stage3Completed: true,
        stage3CompletedBy: executionData.analystId,
        stage3CompletedAt: new Date().toISOString(),
        readyForReporting: true,
        qcApproved: qcApproved,
        resultsApproved: qcApproved, // Stage 4 requires this flag
        deviations: deviations,
        // Include QC and execution data from this stage
        qcResults: qcResults,
        calibrationData: calibrationData,
        quantificationResults: quantificationResults,
        testExecution: {
          // Stage 4 expects "testExecution" not "executionData"
          ...executionData,
          completedAt: new Date().toISOString(),
          status: "EXECUTED",
          analystId: executionData.analystId,
          selectedInstrument: executionData.selectedInstrument,
          method: executionData.method,
          qcApproved: qcApproved,
          deviations: deviations.length,
          executionDate: new Date().toISOString(),
        },
        executionData: {
          // Keep for backward compatibility
          ...executionData,
          completedAt: new Date().toISOString(),
          status: "EXECUTED",
        },
      },
    };

    // Step 1: Apply completion data to Stage 3 page
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(completionPayload),
      async (response) => {
        // Check if response indicates success (same pattern as Stage 1)
        if (response && !response.error && !response.status) {
          // Step 2: Mark samples as COMPLETED on Stage 3
          try {
            const statusResponse = await fetch(
              `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/status-string`,
              {
                method: "POST",
                credentials: "include",
                headers: {
                  "Content-Type": "application/json",
                  "X-CSRF-Token": localStorage.getItem("CSRF"),
                },
                body: JSON.stringify({
                  sampleIds: selectedSampleIds.map((id) => String(id)),
                  status: "COMPLETED",
                }),
              },
            );

            if (!statusResponse.ok) {
              throw new Error("Failed to mark samples as completed");
            }

            // Step 3: Advance samples to Stage 4 (Reporting & Release) if notebookId is available
            if (notebookId) {
              try {
                const advanceResponse = await fetch(
                  `${config.serverBaseUrl}/rest/notebook/${notebookId}/samples/advance-string`,
                  {
                    method: "POST",
                    credentials: "include",
                    headers: {
                      "Content-Type": "application/json",
                      "X-CSRF-Token": localStorage.getItem("CSRF"),
                    },
                    body: JSON.stringify({
                      sampleIds: selectedSampleIds.map((id) => String(id)),
                      fromPageId: pageData.id,
                      toPageIndex: 4, // Stage 4: Reporting & Release
                    }),
                  },
                );

                if (advanceResponse.ok) {
                  // Success: All steps completed
                  setExecutionData((prev) => ({ ...prev, isExecuting: false }));
                  notify({
                    kind: NotificationKinds.success,
                    title: "Success",
                    message: `Test execution completed successfully for ${selectedSampleIds.length} sample(s). Samples advanced to Stage 4 (Reporting & Release).`,
                  });
                } else {
                  // Partial success: Data saved but advance failed
                  setExecutionData((prev) => ({ ...prev, isExecuting: false }));
                  notify({
                    kind: NotificationKinds.warning,
                    title: "Partial Success",
                    message: `Test execution completed for ${selectedSampleIds.length} sample(s), but advance to reporting stage failed. Please refresh to see updated status.`,
                  });
                }
              } catch (advanceError) {
                // Advance failed but data was saved
                setExecutionData((prev) => ({ ...prev, isExecuting: false }));
                console.error("Stage advancement error:", advanceError);
                notify({
                  kind: NotificationKinds.warning,
                  title: "Partial Success",
                  message: `Test execution completed for ${selectedSampleIds.length} sample(s), but advance to reporting stage failed. Please refresh to see updated status.`,
                });
              }
            } else {
              // No notebookId available, but data was saved
              setExecutionData((prev) => ({ ...prev, isExecuting: false }));
              notify({
                kind: NotificationKinds.warning,
                title: "Partial Success",
                message: `Test execution completed for ${selectedSampleIds.length} sample(s). Samples are ready for reporting but could not auto-advance. Please refresh to proceed.`,
              });
            }

            // Clear selection and refresh regardless of advance success
            setSelectedSampleIds([]);
            fetchSamples();

            // Notify parent
            if (onProgressUpdate) {
              onProgressUpdate({
                stage: 3,
                completed: true,
                timestamp: new Date(),
              });
            }
          } catch (statusError) {
            // Data applied but status update failed
            setExecutionData((prev) => ({ ...prev, isExecuting: false }));
            console.error("Status update error:", statusError);
            notify({
              kind: NotificationKinds.error,
              title: "Error",
              message:
                "Test execution completed but failed to update sample status. Please refresh.",
            });
          }
        } else {
          // Apply failed
          setExecutionData((prev) => ({ ...prev, isExecuting: false }));
          notify({
            kind: NotificationKinds.error,
            title: "Error",
            message:
              response?.error ||
              "Failed to complete test execution. Please try again.",
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    qcApproved,
    executionData.analystId,
    deviations,
    qcResults,
    calibrationData,
    quantificationResults,
    executionData,
    assignedSamples,
    notify,
    onProgressUpdate,
    fetchSamples,
    notebookId,
  ]);

  const handleAddDeviation = useCallback(() => {
    if (!deviationForm.type || !deviationForm.description) {
      notify({
        kind: NotificationKinds.warning,
        title: "Warning",
        message: "Please fill in all required fields",
      });
      return;
    }

    const deviation = {
      ...deviationForm,
      recordedAt: new Date().toISOString(),
      recordedBy: executionData.analystId,
    };

    setDeviations((prev) => [...prev, deviation]);
    setDeviationForm({ type: "", description: "", correctiveAction: "" });
    setIsDeviationModalOpen(false);
    notify({
      kind: NotificationKinds.success,
      title: "Success",
      message: "Deviation recorded successfully",
    });
  }, [deviationForm, executionData.analystId, notify]);

  // ============================================================================
  // EFFECTS
  // ============================================================================

  // Fetch notebookId from entry details for stage advancement
  const fetchNotebookId = useCallback(async () => {
    if (!entryId) return;

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook-entry/${entryId}`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const entryData = await response.json();
        const nbId = entryData.notebook?.id || entryData.notebookInstanceId;
        if (nbId) {
          setNotebookId(nbId);
        }
      }
    } catch (error) {
      console.error("Error fetching notebook ID:", error);
    }
  }, [entryId]);

  useEffect(() => {
    fetchSamples();
    fetchNotebookId();
  }, [fetchSamples, fetchNotebookId]);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // ============================================================================
  // TABLE CONFIGURATION
  // ============================================================================

  const sampleTableHeaders = [
    { key: "accessionNumber", header: "Accession Number" },
    { key: "sampleType", header: "Sample Type" },
    { key: "sampleId", header: "Sample ID" },
    { key: "assignedStaff", header: "Assigned Staff" },
    { key: "assignedMethod", header: "Assigned Method" },
    { key: "instrument", header: "Instrument" },
    { key: "status", header: "Status" },
    { key: "progress", header: "Progress" },
  ];

  const sampleTableRows = assignedSamples.map((sample) => ({
    id: sample.id,
    accessionNumber: sample.accessionNumber,
    sampleType: sample.sampleType,
    sampleId: sample.sampleItemId,
    assignedStaff: sample.assignedStaff,
    assignedMethod: sample.assignedMethod,
    instrument: sample.instrumentName || sample.instrumentId || "-",
    status: "Ready",
    progress: sample.data?.executionStatus || "Pending",
  }));

  // ============================================================================
  // RENDER HELPERS
  // ============================================================================

  const renderTableCell = (cell) => {
    switch (cell.info.header) {
      case "assignedMethod":
        return (
          <Tag type="blue" size="sm">
            {cell.value}
          </Tag>
        );
      case "assignedStaff":
        return (
          <Tag type="cyan" size="sm">
            {cell.value}
          </Tag>
        );
      case "instrument":
        return (
          <Tag type="gray" size="sm">
            {cell.value}
          </Tag>
        );
      case "status":
        return (
          <Tag type="green" size="sm">
            {cell.value}
          </Tag>
        );
      case "accessionNumber":
        return <strong>{cell.value}</strong>;
      default:
        return cell.value;
    }
  };

  if (isLoading) {
    return <Loading description="Loading Stage 3 data..." />;
  }

  // ============================================================================
  // MAIN RENDER
  // ============================================================================

  return (
    <div>
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h2>
            <FormattedMessage
              id="notebook.bioanalytical.execution.title"
              defaultMessage="Stage 3: Analytical Test Execution"
            />
          </h2>
        </Column>
      </Grid>

      <Tabs selectedIndex={selectedTab} onChange={handleTabChange}>
        <TabList aria-label="Analytical execution tabs">
          <Tab>Test Execution & Data</Tab>
          <Tab>QC Verification</Tab>
          <Tab>Deviations & Completion</Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Test Execution & Data */}
          <TabPanel>
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <div style={{ marginTop: "1rem", marginBottom: "2rem" }}>
                  <Button
                    kind="primary"
                    onClick={() => setIsExecutionModalOpen(true)}
                    disabled={selectedSampleIds.length === 0}
                  >
                    Configure Test Execution ({selectedSampleIds.length}{" "}
                    selected)
                  </Button>
                  {selectedSampleIds.length > 0 && (
                    <p
                      style={{
                        marginTop: "0.5rem",
                        fontSize: "0.875rem",
                        color: "#6f6f6f",
                      }}
                    >
                      {selectedSampleIds.length} of {assignedSamples.length}{" "}
                      samples selected
                    </p>
                  )}
                </div>

                {/* Main Samples DataTable */}
                <DataTable rows={sampleTableRows} headers={sampleTableHeaders}>
                  {({
                    rows,
                    headers,
                    getHeaderProps,
                    getSelectionProps,
                    getTableProps,
                    getRowProps,
                    onInputChange,
                    selectedRows,
                  }) => (
                    <TableContainer
                      title="Samples for Execution"
                      description={`${assignedSamples.length} samples available for execution`}
                    >
                      <TableToolbar>
                        <TableToolbarContent>
                          <TableToolbarSearch
                            onChange={onInputChange}
                            placeholder="Search samples..."
                          />
                        </TableToolbarContent>
                      </TableToolbar>
                      <Table {...getTableProps()}>
                        <TableHead>
                          <TableRow>
                            <TableSelectAll
                              {...getSelectionProps()}
                              onSelect={(event) => {
                                // Handle select all
                                const allIds = rows.map((row) => row.id);
                                if (event.target.checked) {
                                  setSelectedSampleIds(allIds);
                                } else {
                                  setSelectedSampleIds([]);
                                }
                              }}
                            />
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
                          {rows.length > 0 ? (
                            rows.map((row) => (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableSelectRow
                                  {...getSelectionProps({ row })}
                                  onSelect={(event) => {
                                    // Handle individual row selection
                                    setSelectedSampleIds((prev) => {
                                      const newSet = new Set(prev);
                                      if (event.target.checked) {
                                        newSet.add(row.id);
                                      } else {
                                        newSet.delete(row.id);
                                      }
                                      return Array.from(newSet);
                                    });
                                  }}
                                />
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {renderTableCell(cell)}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))
                          ) : (
                            <TableRow>
                              <TableCell
                                colSpan={headers.length + 1}
                                style={{ textAlign: "center" }}
                              >
                                No samples available for execution. Please
                                complete Stage 2 first.
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              </Column>
            </Grid>
          </TabPanel>

          {/* Tab 2: QC Verification */}
          <TabPanel>
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <h4>QC Results Verification</h4>
                {qcResults.length > 0 ? (
                  <div>
                    <p>
                      QC results loaded from processed files. Please review and
                      approve.
                    </p>
                    <Checkbox
                      id="qc-approval"
                      labelText="All QC criteria met and accepted"
                      checked={qcApproved}
                      onChange={(event, { checked }) => setQcApproved(checked)}
                    />
                  </div>
                ) : (
                  <p>
                    No QC data available. Please process data files in Tab 1.
                  </p>
                )}
              </Column>
            </Grid>
          </TabPanel>

          {/* Tab 3: Deviations & Completion */}
          <TabPanel>
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <h4>Deviations & Final Completion</h4>

                <div style={{ marginBottom: "2rem" }}>
                  <Button
                    kind="secondary"
                    onClick={() => setIsDeviationModalOpen(true)}
                  >
                    Add Deviation
                  </Button>
                </div>

                {deviations.length > 0 && (
                  <div style={{ marginBottom: "2rem" }}>
                    <h5>Recorded Deviations</h5>
                    {deviations.map((deviation, index) => (
                      <div
                        key={index}
                        style={{
                          marginBottom: "1rem",
                          padding: "1rem",
                          border: "1px solid #e0e0e0",
                        }}
                      >
                        <strong>{deviation.type}</strong>
                        <p>{deviation.description}</p>
                        {deviation.correctiveAction && (
                          <p>
                            <em>Action: {deviation.correctiveAction}</em>
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                )}

                <div style={{ marginTop: "2rem" }}>
                  <Button
                    kind="primary"
                    onClick={handleCompleteExecution}
                    disabled={
                      executionData.isExecuting ||
                      selectedSampleIds.length === 0 ||
                      !qcApproved
                    }
                  >
                    {executionData.isExecuting
                      ? "Completing..."
                      : "Complete Test Execution"}
                  </Button>
                </div>
              </Column>
            </Grid>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Execution Configuration Modal */}
      <Modal
        modalHeading="Configure Test Execution"
        primaryButtonText="Start Execution"
        secondaryButtonText="Cancel"
        open={isExecutionModalOpen}
        onRequestClose={() => setIsExecutionModalOpen(false)}
        onRequestSubmit={() => {
          handleExecuteTest();
          setIsExecutionModalOpen(false);
        }}
        size="lg"
      >
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="instrument-select"
              labelText="Instrument *"
              value={executionData.selectedInstrument}
              onChange={(e) =>
                setExecutionData((prev) => ({
                  ...prev,
                  selectedInstrument: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="-- Select instrument --" />
              {BIOANALYTICAL_ANALYZERS.map((instrument) => (
                <SelectItem
                  key={instrument.id}
                  value={instrument.id}
                  text={instrument.machine}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="analyst-id"
              labelText="Analyst ID *"
              value={executionData.analystId}
              onChange={(e) =>
                setExecutionData((prev) => ({
                  ...prev,
                  analystId: e.target.value,
                }))
              }
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="execution-notes"
              labelText="Execution Notes"
              value={executionData.notes}
              onChange={(e) =>
                setExecutionData((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
            />
          </Column>

          {/* File Upload Section */}
          <Column lg={16} md={8} sm={4}>
            <h4 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              Data File Upload
            </h4>
            <FileUploader
              accept={[".csv", ".pdf", ".mzml", ".cdf"]}
              buttonLabel="Choose Files"
              filenameStatus="edit"
              iconDescription="Clear file"
              labelDescription="Drag and drop files here or click to browse"
              labelTitle="Upload Data Files"
              multiple
              onChange={handleFileUpload}
              size="md"
            />

            {uploadedFiles.length > 0 && (
              <div style={{ marginTop: "1rem" }}>
                <h5>Uploaded Files</h5>
                {uploadedFiles.map((file) => (
                  <div key={file.id} style={{ margin: "0.5rem 0" }}>
                    <Tag type={file.processed ? "green" : "gray"} size="sm">
                      {file.name} -{" "}
                      {file.processed
                        ? `${file.analyzerResultsCount} results`
                        : "Uploaded"}
                    </Tag>
                    {!file.processed && (
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => handleProcessFiles([file.id])}
                        style={{ marginLeft: "0.5rem" }}
                      >
                        Process
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Column>
        </Grid>
      </Modal>

      {/* Deviation Modal */}
      <Modal
        modalHeading="Record Deviation"
        primaryButtonText="Save"
        secondaryButtonText="Cancel"
        open={isDeviationModalOpen}
        onRequestClose={() => setIsDeviationModalOpen(false)}
        onRequestSubmit={handleAddDeviation}
      >
        <Select
          id="deviation-type"
          labelText="Deviation Type *"
          value={deviationForm.type}
          onChange={(e) =>
            setDeviationForm((prev) => ({ ...prev, type: e.target.value }))
          }
        >
          <SelectItem value="" text="-- Select type --" />
          <SelectItem
            value="Out of Specification"
            text="Out of Specification"
          />
          <SelectItem value="Equipment Issue" text="Equipment Issue" />
          <SelectItem value="Analyst Error" text="Analyst Error" />
          <SelectItem value="Environmental" text="Environmental" />
          <SelectItem value="Other" text="Other" />
        </Select>

        <TextArea
          id="deviation-description"
          labelText="Description *"
          value={deviationForm.description}
          onChange={(e) =>
            setDeviationForm((prev) => ({
              ...prev,
              description: e.target.value,
            }))
          }
          style={{ marginTop: "1rem" }}
        />

        <TextArea
          id="corrective-action"
          labelText="Corrective Action"
          value={deviationForm.correctiveAction}
          onChange={(e) =>
            setDeviationForm((prev) => ({
              ...prev,
              correctiveAction: e.target.value,
            }))
          }
          style={{ marginTop: "1rem" }}
        />
      </Modal>
    </div>
  );
}

export default BioanalyticalAnalyticalExecutionPage;
