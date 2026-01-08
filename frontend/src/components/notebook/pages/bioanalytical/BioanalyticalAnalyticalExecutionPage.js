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
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
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
  const [acceptanceCriteria, setAcceptanceCriteria] = useState(null);

  // Deviations
  const [deviations, setDeviations] = useState([]);

  // Completion progress tracking
  const [completionProgress, setCompletionProgress] = useState({
    step1: { name: "Apply execution data", status: "pending" }, // pending, in-progress, completed, failed
    step2: { name: "Update sample status", status: "pending" },
    step3: { name: "Advance to Stage 4", status: "pending" },
  });

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
          setAcceptanceCriteria(qcData.acceptanceCriteria || null);

          // Also extract uploaded files and execution data for completed samples
          if (qcData.uploadedFiles) {
            setUploadedFiles(qcData.uploadedFiles);
          }

          // Extract execution data if present
          if (qcData.executionData) {
            setExecutionData((prev) => ({
              ...prev,
              ...qcData.executionData,
              isExecuting: false, // Reset executing state
            }));
          }

          // Auto-select all samples if they're already completed
          if (qcData.stage3Completed) {
            const completedSampleIds = cleanSamples
              .filter((s) => s.data?.stage3Completed)
              .map((s) => s.id);
            setSelectedSampleIds(completedSampleIds);
          }
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

      // Tab-specific data refresh logic
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
        // Backend returns "quantification" key (note: not "quantificationResults")
        const quantResults =
          result.quantificationResults || result.quantification || [];
        if (quantResults.length > 0) {
          setQuantificationResults(quantResults);
          console.log("Stage 3 - Quantification results loaded:", quantResults);
        }
        if (result.calibrationData) {
          setCalibrationData(result.calibrationData);
          console.log(
            "Stage 3 - Calibration data loaded:",
            result.calibrationData,
          );
        }

        const resultsCount = result.analyzerResults?.length || 0;
        notify({
          kind: NotificationKinds.success,
          title: "Success",
          message: `Files processed successfully. ${resultsCount} results extracted.`,
        });
      } catch (error) {
        console.error("File processing error:", error);
        notify({
          kind: NotificationKinds.error,
          title: "Processing Error",
          message: `File processing failed: ${error.message}`,
        });
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

    // Enhanced execution data validation
    if (!executionData || !executionData.analystId) {
      notify({
        kind: NotificationKinds.error,
        title: "Validation Error",
        message:
          "Analyst information is required. Please complete execution data.",
      });
      return;
    }

    if (!executionData.selectedInstrument) {
      notify({
        kind: NotificationKinds.error,
        title: "Validation Error",
        message: "Please select an instrument before completing execution.",
      });
      return;
    }

    // Enhanced QC validation
    if (qcResults && qcResults.length > 0) {
      const invalidQCResults = qcResults.filter(
        (qc) =>
          !qc.accuracy ||
          typeof qc.accuracy !== "number" ||
          qc.accuracy <= 0 ||
          qc.accuracy > 200, // Reasonable upper bound
      );

      if (invalidQCResults.length > 0) {
        notify({
          kind: NotificationKinds.error,
          title: "QC Validation Error",
          message: `${invalidQCResults.length} QC result(s) have invalid accuracy values. Please review QC data.`,
        });
        return;
      }

      // Check if QC results meet basic acceptance criteria
      const passableQCResults = qcResults.filter((qc) => {
        const accuracy = parseFloat(qc.accuracy || 0);
        return accuracy >= 80 && accuracy <= 120; // Basic FDA criteria
      });

      if (passableQCResults.length === 0) {
        const proceed = window.confirm(
          "Warning: All QC results are outside acceptable range (80-120%). This may affect data quality. Do you want to proceed?",
        );
        if (!proceed) {
          return;
        }
      }
    }

    // Enhanced calibration validation
    if (calibrationData) {
      const { rSquared, slope } = calibrationData;

      if (rSquared !== undefined && rSquared < 0.95) {
        const proceed = window.confirm(
          `Warning: Calibration R² (${rSquared.toFixed(4)}) is below 0.95. This may affect data quality. Do you want to proceed?`,
        );
        if (!proceed) {
          return;
        }
      }

      if (slope !== undefined && Math.abs(slope) < 0.001) {
        notify({
          kind: NotificationKinds.error,
          title: "Calibration Error",
          message:
            "Calibration curve slope is too low. Please review calibration data.",
        });
        return;
      }
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

    // Bioanalytical regulatory compliance: Raw data files mandatory
    if (!uploadedFiles || uploadedFiles.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: "Regulatory Compliance Error",
        message:
          "Raw data files are required for bioanalytical studies. Please upload instrument data files (.mzml, .cdf) or processed data files (.csv, .pdf) before completing execution.",
      });
      return;
    }

    // Validate at least one file has been processed
    const processedFiles = uploadedFiles.filter((file) => file.processed);
    if (processedFiles.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: "Data Processing Required",
        message:
          "At least one uploaded file must be processed to extract QC results before completing execution.",
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

    // Reset progress tracker
    setCompletionProgress({
      step1: { name: "Apply execution data", status: "in-progress" },
      step2: { name: "Update sample status", status: "pending" },
      step3: { name: "Advance to Stage 4", status: "pending" },
    });

    // Step 1: Apply completion data to Stage 3 page
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(completionPayload),
      async (response) => {
        // Check if response indicates success (same pattern as Stage 1)
        if (response && !response.error && !response.status) {
          // Step 1 completed successfully
          setCompletionProgress((prev) => ({
            ...prev,
            step1: { name: "Apply execution data", status: "completed" },
            step2: { name: "Update sample status", status: "in-progress" },
          }));

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

            // Step 2 completed successfully
            setCompletionProgress((prev) => ({
              ...prev,
              step2: { name: "Update sample status", status: "completed" },
              step3: { name: "Advance to Stage 4", status: "in-progress" },
            }));

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
                  setCompletionProgress((prev) => ({
                    ...prev,
                    step3: { name: "Advance to Stage 4", status: "completed" },
                  }));
                  setExecutionData((prev) => ({ ...prev, isExecuting: false }));
                  notify({
                    kind: NotificationKinds.success,
                    title: "✓ Success",
                    message: `Test execution completed successfully for ${selectedSampleIds.length} sample(s). Samples advanced to Stage 4 (Reporting & Release).`,
                  });
                } else {
                  // Partial success: Data saved but advance failed
                  setCompletionProgress((prev) => ({
                    ...prev,
                    step3: { name: "Advance to Stage 4", status: "failed" },
                  }));
                  setExecutionData((prev) => ({ ...prev, isExecuting: false }));
                  notify({
                    kind: NotificationKinds.warning,
                    title: "⚠ Partial Success",
                    message: `Steps 1 & 2 completed successfully. Step 3 (advancement) failed. Data is saved and samples are COMPLETED in Stage 3. Contact admin to manually advance to Stage 4.`,
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
    assignedMethod:
      sample.assignedMethod ||
      sample.data?.analyticalMethod ||
      sample.data?.assignedMethod ||
      "Not assigned",
    instrument: sample.instrumentName || sample.instrumentId || "-",
    status: "Ready",
    progress: sample.data?.executionStatus || "Pending",
    _original: sample, // Store original sample data for reference
  }));

  // ============================================================================
  // SIMPLE SELECTION HANDLERS (following SampleGrid pattern)
  // ============================================================================

  // Calculate selection state for checkboxes
  const allSelected =
    sampleTableRows.length > 0 &&
    selectedSampleIds.length === sampleTableRows.length;
  const someSelected =
    selectedSampleIds.length > 0 &&
    selectedSampleIds.length < sampleTableRows.length;

  // Handle individual row selection (toggle based on current state)
  const handleSelectRow = useCallback(
    (id) => {
      const isCurrentlySelected = selectedSampleIds.includes(id);
      if (isCurrentlySelected) {
        setSelectedSampleIds(selectedSampleIds.filter((sid) => sid !== id));
      } else {
        setSelectedSampleIds([...selectedSampleIds, id]);
      }
    },
    [selectedSampleIds],
  );

  // Handle select all (toggle all based on current state)
  const handleSelectAll = useCallback(() => {
    if (allSelected) {
      setSelectedSampleIds([]);
    } else {
      setSelectedSampleIds(sampleTableRows.map((row) => row.id));
    }
  }, [allSelected, sampleTableRows]);

  // ============================================================================
  // LOADING STATE
  // ============================================================================

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

                {/* Main Samples Table (Simple Implementation like SampleGrid) */}
                <TableContainer
                  title="Samples for Execution"
                  description={`${assignedSamples.length} samples available for execution`}
                >
                  <TableToolbar>
                    <TableToolbarContent>
                      <TableToolbarSearch
                        placeholder="Search samples..."
                        onChange={(e) => {
                          // Add search functionality if needed later
                        }}
                      />
                    </TableToolbarContent>
                  </TableToolbar>

                  <Table size="md">
                    <TableHead>
                      <TableRow>
                        {/* Select All Checkbox */}
                        <TableHeader className="cds--table-column-checkbox">
                          <Checkbox
                            id="select-all-samples"
                            checked={allSelected}
                            indeterminate={someSelected}
                            onChange={handleSelectAll}
                            labelText=""
                            hideLabel
                          />
                        </TableHeader>
                        {/* Column Headers */}
                        {sampleTableHeaders.map((header) => (
                          <TableHeader key={header.key}>
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {sampleTableRows.length > 0 ? (
                        sampleTableRows.map((row) => (
                          <TableRow
                            key={row.id}
                            className={
                              selectedSampleIds.includes(row.id)
                                ? "selected"
                                : ""
                            }
                          >
                            {/* Selection Checkbox */}
                            <TableCell
                              className="cds--table-column-checkbox"
                              onClick={(e) => e.stopPropagation()}
                            >
                              <Checkbox
                                id={`select-row-${row.id}`}
                                checked={selectedSampleIds.includes(row.id)}
                                onChange={() => handleSelectRow(row.id)}
                                labelText=""
                                hideLabel
                              />
                            </TableCell>
                            {/* Data Cells */}
                            <TableCell>{row.accessionNumber}</TableCell>
                            <TableCell>{row.sampleType}</TableCell>
                            <TableCell>{row.sampleId}</TableCell>
                            <TableCell>{row.assignedStaff}</TableCell>
                            <TableCell>
                              {row.assignedMethod !== "Not assigned" ? (
                                <Tag type="blue" size="sm">
                                  {row.assignedMethod}
                                </Tag>
                              ) : (
                                <span style={{ color: "#8d8d8d" }}>
                                  Not assigned
                                </span>
                              )}
                            </TableCell>
                            <TableCell>{row.instrument}</TableCell>
                            <TableCell>
                              <Tag type="green" size="sm">
                                {row.status}
                              </Tag>
                            </TableCell>
                            <TableCell>
                              <Tag
                                type={
                                  row.progress === "Pending" ? "gray" : "blue"
                                }
                                size="sm"
                              >
                                {row.progress}
                              </Tag>
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell
                            colSpan={sampleTableHeaders.length + 1}
                            style={{ textAlign: "center" }}
                          >
                            No samples available for execution. Please complete
                            Stage 2 first.
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
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
                    <p style={{ marginBottom: "1rem" }}>
                      Review QC results below and verify all criteria are met
                      before approval.
                    </p>

                    {/* Calibration Data Display */}
                    {calibrationData && (
                      <div
                        style={{
                          marginBottom: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <h5 style={{ marginBottom: "0.5rem" }}>
                          Calibration Curve Results
                        </h5>
                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns: "1fr 1fr 1fr",
                            gap: "1rem",
                          }}
                        >
                          <p>
                            <strong>R²:</strong>{" "}
                            {calibrationData?.rSquared?.toFixed(4) || "N/A"}
                          </p>
                          <p>
                            <strong>Slope:</strong>{" "}
                            {calibrationData?.slope?.toFixed(3) || "N/A"}
                          </p>
                          <p>
                            <strong>Intercept:</strong>{" "}
                            {calibrationData?.intercept?.toFixed(3) || "N/A"}
                          </p>
                        </div>
                        <p
                          style={{
                            marginTop: "0.5rem",
                            color:
                              calibrationData?.rSquared >=
                              (parseFloat(acceptanceCriteria?.rSquaredMin) ||
                                0.99)
                                ? "#24a148"
                                : "#da1e28",
                            fontWeight: "500",
                          }}
                        >
                          Status:{" "}
                          {calibrationData?.rSquared >=
                          (parseFloat(acceptanceCriteria?.rSquaredMin) || 0.99)
                            ? "✓ ACCEPTABLE"
                            : "✗ FAILS CRITERIA"}
                          (Required: R² ≥{" "}
                          {acceptanceCriteria?.rSquaredMin || "0.99"})
                        </p>
                      </div>
                    )}

                    {/* QC Results Table */}
                    <div
                      style={{
                        marginBottom: "1.5rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <h5 style={{ marginBottom: "0.5rem" }}>
                        QC Control Results ({qcResults.length} measurements)
                      </h5>
                      <div style={{ overflowX: "auto" }}>
                        <table
                          style={{
                            width: "100%",
                            borderCollapse: "collapse",
                            fontSize: "0.875rem",
                          }}
                        >
                          <thead>
                            <tr style={{ borderBottom: "2px solid #393939" }}>
                              <th
                                style={{ padding: "0.5rem", textAlign: "left" }}
                              >
                                Control Level
                              </th>
                              <th
                                style={{
                                  padding: "0.5rem",
                                  textAlign: "right",
                                }}
                              >
                                Accuracy (%)
                              </th>
                              <th
                                style={{
                                  padding: "0.5rem",
                                  textAlign: "right",
                                }}
                              >
                                Precision
                              </th>
                              <th
                                style={{
                                  padding: "0.5rem",
                                  textAlign: "right",
                                }}
                              >
                                Measured Value
                              </th>
                              <th
                                style={{
                                  padding: "0.5rem",
                                  textAlign: "center",
                                }}
                              >
                                Status
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {qcResults.map((qc, index) => {
                              const accuracy = parseFloat(qc.accuracy || 0);
                              const isAcceptable =
                                accuracy >= 85 && accuracy <= 115; // ±15% at most levels
                              return (
                                <tr
                                  key={index}
                                  style={{ borderBottom: "1px solid #e0e0e0" }}
                                >
                                  <td style={{ padding: "0.5rem" }}>
                                    {qc.controlLevel || `Control ${index + 1}`}
                                  </td>
                                  <td
                                    style={{
                                      padding: "0.5rem",
                                      textAlign: "right",
                                    }}
                                  >
                                    {qc.accuracy || "N/A"}
                                  </td>
                                  <td
                                    style={{
                                      padding: "0.5rem",
                                      textAlign: "right",
                                    }}
                                  >
                                    {qc.precision || "N/A"}
                                  </td>
                                  <td
                                    style={{
                                      padding: "0.5rem",
                                      textAlign: "right",
                                    }}
                                  >
                                    {qc.measuredValue || "N/A"}
                                  </td>
                                  <td
                                    style={{
                                      padding: "0.5rem",
                                      textAlign: "center",
                                      color: isAcceptable
                                        ? "#24a148"
                                        : "#da1e28",
                                      fontWeight: "500",
                                    }}
                                  >
                                    {isAcceptable ? "✓ PASS" : "✗ FAIL"}
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>

                      {/* QC Summary */}
                      <div
                        style={{
                          marginTop: "1rem",
                          padding: "0.75rem",
                          backgroundColor: "#e7f1f5",
                          borderRadius: "4px",
                        }}
                      >
                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns: "1fr 1fr 1fr",
                            gap: "1rem",
                            fontSize: "0.875rem",
                          }}
                        >
                          {(() => {
                            const accuracies = qcResults
                              .map((qc) => parseFloat(qc.accuracy || 0))
                              .filter((a) => a > 0);
                            const mean =
                              accuracies.length > 0
                                ? accuracies.reduce((a, b) => a + b) /
                                  accuracies.length
                                : 0;
                            const variance =
                              accuracies.length > 0
                                ? accuracies.reduce(
                                    (sq, n) => sq + Math.pow(n - mean, 2),
                                    0,
                                  ) / accuracies.length
                                : 0;
                            const cv =
                              mean > 0 ? (Math.sqrt(variance) / mean) * 100 : 0;
                            const passCount = qcResults.filter((qc) => {
                              const acc = parseFloat(qc.accuracy || 0);
                              return acc >= 85 && acc <= 115;
                            }).length;

                            return (
                              <>
                                <p>
                                  <strong>Mean Accuracy:</strong>{" "}
                                  {mean.toFixed(1)}%
                                </p>
                                <p>
                                  <strong>CV:</strong> {cv.toFixed(1)}%
                                </p>
                                <p>
                                  <strong>Pass Rate:</strong> {passCount}/
                                  {qcResults.length} (
                                  {(
                                    (passCount / qcResults.length) *
                                    100
                                  ).toFixed(0)}
                                  %)
                                </p>
                              </>
                            );
                          })()}
                        </div>
                      </div>
                    </div>

                    {/* System Suitability */}
                    {executionData.instrumentId && (
                      <div
                        style={{
                          marginBottom: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <h5 style={{ marginBottom: "0.5rem" }}>
                          System Suitability
                        </h5>
                        <p>
                          <strong>Instrument:</strong>{" "}
                          {executionData.instrumentId}
                        </p>
                        <p>
                          <strong>Method:</strong> {executionData.method}
                        </p>
                        <p style={{ color: "#24a148", fontWeight: "500" }}>
                          ✓ System suitability verified
                        </p>
                      </div>
                    )}

                    {/* Quantification Results Section */}
                    <div
                      style={{
                        marginBottom: "1.5rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <h5 style={{ marginBottom: "0.5rem" }}>
                        Unknown Sample Quantification Results
                      </h5>
                      <p
                        style={{
                          fontSize: "0.875rem",
                          color: "#525252",
                          marginBottom: "1rem",
                        }}
                      >
                        Enter the calculated concentrations for unknown/test
                        samples analyzed. These values will be used for
                        bioequivalence assessment.
                      </p>

                      {quantificationResults &&
                      quantificationResults.length > 0 ? (
                        <div style={{ overflowX: "auto" }}>
                          <table
                            style={{
                              width: "100%",
                              borderCollapse: "collapse",
                              fontSize: "0.875rem",
                            }}
                          >
                            <thead>
                              <tr style={{ borderBottom: "2px solid #393939" }}>
                                <th
                                  style={{
                                    padding: "0.5rem",
                                    textAlign: "left",
                                  }}
                                >
                                  Sample ID
                                </th>
                                <th
                                  style={{
                                    padding: "0.5rem",
                                    textAlign: "right",
                                  }}
                                >
                                  Concentration (ng/mL)
                                </th>
                                <th
                                  style={{
                                    padding: "0.5rem",
                                    textAlign: "center",
                                  }}
                                >
                                  Status
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              {quantificationResults.map((qr, index) => (
                                <tr
                                  key={index}
                                  style={{ borderBottom: "1px solid #e0e0e0" }}
                                >
                                  <td style={{ padding: "0.5rem" }}>
                                    {qr.sampleId ||
                                      qr.id ||
                                      `Sample ${index + 1}`}
                                  </td>
                                  <td
                                    style={{
                                      padding: "0.5rem",
                                      textAlign: "right",
                                    }}
                                  >
                                    {qr.concentration ||
                                      qr.measuredValue ||
                                      "N/A"}
                                  </td>
                                  <td
                                    style={{
                                      padding: "0.5rem",
                                      textAlign: "center",
                                      color: "#24a148",
                                      fontWeight: "500",
                                    }}
                                  >
                                    ✓ Quantified
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : (
                        <p style={{ color: "#666", fontSize: "0.875rem" }}>
                          No quantification results available yet.
                          Quantification data will appear here after file
                          processing.
                        </p>
                      )}
                    </div>

                    <Checkbox
                      id="qc-approval"
                      labelText="I have reviewed all QC results above and approve them for release"
                      checked={qcApproved}
                      onChange={(event, { checked }) => setQcApproved(checked)}
                      style={{ marginTop: "1rem" }}
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

                {/* Completion Progress Tracker */}
                {executionData.isExecuting && (
                  <div
                    style={{
                      marginTop: "1.5rem",
                      marginBottom: "1rem",
                      padding: "1rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <h5 style={{ marginBottom: "0.75rem" }}>
                      Completion Progress
                    </h5>
                    {Object.entries(completionProgress).map(
                      ([stepKey, step]) => (
                        <div
                          key={stepKey}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            marginBottom: "0.5rem",
                            fontSize: "0.875rem",
                          }}
                        >
                          <span
                            style={{
                              display: "inline-block",
                              width: "16px",
                              height: "16px",
                              borderRadius: "50%",
                              marginRight: "0.75rem",
                              backgroundColor:
                                step.status === "completed"
                                  ? "#24a148"
                                  : step.status === "in-progress"
                                    ? "#0043ce"
                                    : step.status === "failed"
                                      ? "#da1e28"
                                      : "#e0e0e0",
                              color:
                                step.status !== "pending"
                                  ? "white"
                                  : "transparent",
                              textAlign: "center",
                              lineHeight: "16px",
                              fontSize: "0.75rem",
                            }}
                          >
                            {step.status === "completed"
                              ? "✓"
                              : step.status === "in-progress"
                                ? "⋯"
                                : step.status === "failed"
                                  ? "✗"
                                  : ""}
                          </span>
                          <span
                            style={{
                              color:
                                step.status === "failed"
                                  ? "#da1e28"
                                  : "#161616",
                            }}
                          >
                            {step.name}
                          </span>
                        </div>
                      ),
                    )}
                  </div>
                )}

                {/* Pre-Completion Validation Checklist */}
                <div
                  style={{
                    marginTop: "2rem",
                    padding: "1rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    border:
                      selectedSampleIds.length > 0
                        ? "1px solid #0f62fe"
                        : "1px solid #ddd",
                  }}
                >
                  <h5 style={{ marginBottom: "1rem", color: "#161616" }}>
                    Pre-Completion Validation Checklist
                  </h5>

                  <div style={{ display: "grid", gap: "0.5rem" }}>
                    {/* Sample Selection Check */}
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <span
                        style={{
                          fontSize: "1rem",
                          color:
                            selectedSampleIds.length > 0
                              ? "#198038"
                              : "#da1e28",
                        }}
                      >
                        {selectedSampleIds.length > 0 ? "✓" : "✗"}
                      </span>
                      <span
                        style={{
                          color:
                            selectedSampleIds.length > 0
                              ? "#161616"
                              : "#6f6f6f",
                        }}
                      >
                        Samples selected ({selectedSampleIds.length} of{" "}
                        {assignedSamples.length})
                      </span>
                    </div>

                    {/* Analyst Information Check */}
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <span
                        style={{
                          fontSize: "1rem",
                          color: executionData?.analystId
                            ? "#198038"
                            : "#da1e28",
                        }}
                      >
                        {executionData?.analystId ? "✓" : "✗"}
                      </span>
                      <span
                        style={{
                          color: executionData?.analystId
                            ? "#161616"
                            : "#6f6f6f",
                        }}
                      >
                        Analyst information provided
                      </span>
                    </div>

                    {/* Instrument Selection Check */}
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <span
                        style={{
                          fontSize: "1rem",
                          color: executionData?.selectedInstrument
                            ? "#198038"
                            : "#da1e28",
                        }}
                      >
                        {executionData?.selectedInstrument ? "✓" : "✗"}
                      </span>
                      <span
                        style={{
                          color: executionData?.selectedInstrument
                            ? "#161616"
                            : "#6f6f6f",
                        }}
                      >
                        Instrument selected
                      </span>
                    </div>

                    {/* QC Results Check */}
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <span
                        style={{
                          fontSize: "1rem",
                          color:
                            qcResults && qcResults.length > 0
                              ? "#198038"
                              : "#f1c21b",
                        }}
                      >
                        {qcResults && qcResults.length > 0 ? "✓" : "⚠"}
                      </span>
                      <span
                        style={{
                          color:
                            qcResults && qcResults.length > 0
                              ? "#161616"
                              : "#6f6f6f",
                        }}
                      >
                        QC results available ({qcResults ? qcResults.length : 0}{" "}
                        controls)
                      </span>
                    </div>

                    {/* QC Approval Check */}
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <span
                        style={{
                          fontSize: "1rem",
                          color: qcApproved ? "#198038" : "#da1e28",
                        }}
                      >
                        {qcApproved ? "✓" : "✗"}
                      </span>
                      <span
                        style={{
                          color: qcApproved ? "#161616" : "#6f6f6f",
                        }}
                      >
                        QC results approved
                      </span>
                    </div>

                    {/* File Upload Compliance Check */}
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <span
                        style={{
                          fontSize: "1rem",
                          color:
                            uploadedFiles && uploadedFiles.length > 0
                              ? "#198038"
                              : "#da1e28",
                        }}
                      >
                        {uploadedFiles && uploadedFiles.length > 0 ? "✓" : "✗"}
                      </span>
                      <span
                        style={{
                          color:
                            uploadedFiles && uploadedFiles.length > 0
                              ? "#161616"
                              : "#6f6f6f",
                        }}
                      >
                        Raw data files uploaded (
                        {uploadedFiles ? uploadedFiles.length : 0} files) -
                        Required for FDA compliance
                      </span>
                    </div>

                    {/* File Processing Check */}
                    {uploadedFiles && uploadedFiles.length > 0 && (
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <span
                          style={{
                            fontSize: "1rem",
                            color:
                              uploadedFiles.filter((f) => f.processed).length >
                              0
                                ? "#198038"
                                : "#f1c21b",
                          }}
                        >
                          {uploadedFiles.filter((f) => f.processed).length > 0
                            ? "✓"
                            : "⚠"}
                        </span>
                        <span
                          style={{
                            color:
                              uploadedFiles.filter((f) => f.processed).length >
                              0
                                ? "#161616"
                                : "#6f6f6f",
                          }}
                        >
                          Data files processed (
                          {uploadedFiles.filter((f) => f.processed).length} of{" "}
                          {uploadedFiles.length})
                        </span>
                      </div>
                    )}

                    {/* Calibration Check */}
                    {calibrationData && (
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <span
                          style={{
                            fontSize: "1rem",
                            color:
                              calibrationData.rSquared >= 0.95
                                ? "#198038"
                                : "#f1c21b",
                          }}
                        >
                          {calibrationData.rSquared >= 0.95 ? "✓" : "⚠"}
                        </span>
                        <span
                          style={{
                            color:
                              calibrationData.rSquared >= 0.95
                                ? "#161616"
                                : "#6f6f6f",
                          }}
                        >
                          Calibration curve acceptable (R² ={" "}
                          {calibrationData.rSquared?.toFixed(4) || "N/A"})
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Summary Status */}
                  <div
                    style={{
                      marginTop: "1rem",
                      padding: "0.75rem",
                      backgroundColor:
                        qcApproved &&
                        selectedSampleIds.length > 0 &&
                        executionData?.analystId &&
                        executionData?.selectedInstrument &&
                        uploadedFiles &&
                        uploadedFiles.length > 0 &&
                        uploadedFiles.filter((f) => f.processed).length > 0
                          ? "#e7f6ed"
                          : "#fff3e0",
                      borderRadius: "4px",
                      border:
                        qcApproved &&
                        selectedSampleIds.length > 0 &&
                        executionData?.analystId &&
                        executionData?.selectedInstrument &&
                        uploadedFiles &&
                        uploadedFiles.length > 0 &&
                        uploadedFiles.filter((f) => f.processed).length > 0
                          ? "1px solid #198038"
                          : "1px solid #f1c21b",
                    }}
                  >
                    <strong
                      style={{
                        color:
                          qcApproved &&
                          selectedSampleIds.length > 0 &&
                          executionData?.analystId &&
                          executionData?.selectedInstrument &&
                          uploadedFiles &&
                          uploadedFiles.length > 0 &&
                          uploadedFiles.filter((f) => f.processed).length > 0
                            ? "#198038"
                            : "#8d4004",
                      }}
                    >
                      {qcApproved &&
                      selectedSampleIds.length > 0 &&
                      executionData?.analystId &&
                      executionData?.selectedInstrument &&
                      uploadedFiles &&
                      uploadedFiles.length > 0 &&
                      uploadedFiles.filter((f) => f.processed).length > 0
                        ? "✓ Ready for completion - All FDA compliance requirements met"
                        : "⚠ Complete all required items for FDA-compliant bioanalytical execution"}
                    </strong>
                  </div>
                </div>

                <div style={{ marginTop: "1rem" }}>
                  <Button
                    kind="primary"
                    onClick={handleCompleteExecution}
                    disabled={
                      executionData.isExecuting ||
                      selectedSampleIds.length === 0 ||
                      !qcApproved ||
                      !uploadedFiles ||
                      uploadedFiles.length === 0 ||
                      uploadedFiles.filter((f) => f.processed).length === 0
                    }
                  >
                    {executionData.isExecuting
                      ? "Completing..."
                      : "Complete Test Execution"}
                  </Button>
                  {(!uploadedFiles || uploadedFiles.length === 0) && (
                    <p
                      style={{
                        marginTop: "0.5rem",
                        fontSize: "0.875rem",
                        color: "#6f6f6f",
                        fontStyle: "italic",
                      }}
                    >
                      📋 Upload raw data files (required for FDA bioanalytical
                      compliance)
                    </p>
                  )}
                  {uploadedFiles &&
                    uploadedFiles.length > 0 &&
                    uploadedFiles.filter((f) => f.processed).length === 0 && (
                      <p
                        style={{
                          marginTop: "0.5rem",
                          fontSize: "0.875rem",
                          color: "#f1c21b",
                          fontStyle: "italic",
                        }}
                      >
                        ⚠ Process uploaded files to extract QC data before
                        completion
                      </p>
                    )}
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
            <h4 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              Raw Data File Upload{" "}
              <span style={{ color: "#da1e28", fontSize: "1rem" }}>*</span>
            </h4>
            <p
              style={{
                marginBottom: "1rem",
                fontSize: "0.875rem",
                color: "#6f6f6f",
                lineHeight: "1.4",
              }}
            >
              <strong>FDA Requirement:</strong> Original instrument files are
              mandatory for bioanalytical/bioequivalence studies. Upload raw
              data (.mzml, .cdf) or processed files (.csv, .pdf) for regulatory
              compliance.
            </p>
            <FileUploader
              accept={[".csv", ".pdf", ".mzml", ".cdf"]}
              buttonLabel="Choose Files"
              filenameStatus="edit"
              iconDescription="Clear file"
              labelDescription="Drag and drop files here or click to browse"
              labelTitle="Upload Raw Data Files (Required for FDA Compliance)"
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
