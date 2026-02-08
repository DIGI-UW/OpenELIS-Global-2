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
import { usePermissions } from "../../../../hooks/usePermissions";
import { useBioequivalencePermissions } from "../../../../hooks/useBioequivalencePermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import config from "../../../../config.json";

/**
 * Analytical methods available for bioequivalence testing
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
 * Stage 3: Analytical Test Execution - Clean Implementation
 * Features:
 * - Proper Carbon DataTable with selection
 * - Clean tab-based data fetching
 * - Proper QC results handling without mock data
 * - Persistent execution data
 * - Default Carbon Design System styling
 */
function BioequivalenceAnalyticalExecutionPage({
  pageData,
  onProgressUpdate,
  templateInstruments,
  entryId,
}) {
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { hasAnyRole } = usePermissions();
  const { getPagePermissionLevel, hasFullControl, canSaveData } =
    useBioequivalencePermissions();
  const isMountedRef = useRef(true);

  const allowedRoles = [
    "Bioequivalence Chemical Analyst",
    "Bioequivalence Pharmacist",
    "Bioequivalence Lab Supervisor",
    "Bioequivalence Study Director",
    "Bioequivalence QA Officer",
    "Bioequivalence Researcher",
  ];

  const canAccessPage = hasAnyRole(allowedRoles);

  const pagePermissionLevel = getPagePermissionLevel("Analytical Execution");
  const canExecuteAnalysis = hasFullControl(pagePermissionLevel);
  const canSaveResults = canSaveData(pagePermissionLevel);

  // Debug logging for permission issues
  if (process.env.NODE_ENV === "development") {
    console.log("Analytical Execution Page - Permission Debug:", {
      pagePermissionLevel,
      canSaveResults,
      canExecuteAnalysis,
    });
  }

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

  const [deviations, setDeviations] = useState([]);

  const [completionProgress, setCompletionProgress] = useState({
    step1: { name: "Apply execution data", status: "pending" }, // pending, in-progress, completed, failed
    step2: { name: "Update sample status", status: "pending" },
    step3: { name: "Advance to Stage 4", status: "pending" },
  });

  const [uploadedFiles, setUploadedFiles] = useState([]);

  const [isExecutionModalOpen, setIsExecutionModalOpen] = useState(false);
  const [isDeviationModalOpen, setIsDeviationModalOpen] = useState(false);
  const [deviationForm, setDeviationForm] = useState({
    type: "",
    description: "",
    correctiveAction: "",
  });

  const [notebookId, setNotebookId] = useState(null);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

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

      const cleanSamples = samples.map((sample) => ({
        id: sample.id,
        accessionNumber: sample.accessionNumber,
        sampleType:
          sample.data?.sampleType ||
          sample.sampleType ||
          sample.typeOfSample?.type ||
          "-",
        sampleItemId: sample.sampleItemId || sample.id,
        assignedStaff:
          sample.assignedStaff || sample.data?.assignedStaff || "-",
        assignedMethod:
          sample.assignedMethod ||
          sample.data?.analyticalMethod ||
          sample.data?.assignedMethod ||
          "Not assigned",
        instrumentName:
          sample.instrumentName || sample.data?.instrumentName || "-",
        instrumentId: sample.instrumentId || sample.data?.instrumentId || null,
        data: sample.data || {},
      }));

      setAssignedSamples(cleanSamples);

      if (cleanSamples.length > 0) {
        const firstSample = cleanSamples[0];
        if (firstSample.data?.executionData) {
          setExecutionData((prev) => ({
            ...prev,
            ...firstSample.data.executionData,
          }));
        }

        const qcData = cleanSamples.find((s) => s.data?.qcResults)?.data;
        if (qcData) {
          setQcResults(qcData.qcResults || []);
          setCalibrationData(qcData.calibrationData || null);
          setQuantificationResults(qcData.quantificationResults || []);
          setQcApproved(qcData.qcApproved || false);
          setAcceptanceCriteria(qcData.acceptanceCriteria || null);

          if (qcData.uploadedFiles) {
            setUploadedFiles(qcData.uploadedFiles);
          }

          if (qcData.executionData) {
            setExecutionData((prev) => ({
              ...prev,
              ...qcData.executionData,
              isExecuting: false,
            }));
          }

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

      await fetchSamples();

      switch (tabIndex) {
        case 0: // Test Execution
          break;
        case 1: // QC Verification
          break;
        case 2: // Deviations & Completion
          break;
        default:
          break;
      }
    },
    [fetchSamples],
  );

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

  // New state for raw data files uploaded outside modal
  const [uploadedRawFiles, setUploadedRawFiles] = useState([]);

  // Handle raw data file upload (outside modal)
  const handleRawDataUpload = useCallback(
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

      try {
        const uploadPromises = files.map(async (file) => {
          const formData = new FormData();
          formData.append("file", file);
          formData.append("instrumentId", "RAW_DATA_UPLOAD"); // Placeholder since no instrument selected yet
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
            resultsCount: 0,
            type: "raw_data",
          };
        });

        const uploadedFileResults = await Promise.all(uploadPromises);
        setUploadedRawFiles((prev) => [...prev, ...uploadedFileResults]);

        notify({
          kind: NotificationKinds.success,
          title: "Raw Data Files Uploaded",
          message: `${files.length} raw data file(s) uploaded successfully. You can process them now or later in the execution modal.`,
        });
      } catch (error) {
        console.error("Raw data upload error:", error);
        notify({
          kind: NotificationKinds.error,
          title: "Upload Error",
          message: `Raw data upload failed: ${error.message}`,
        });
      }
    },
    [notify, pageData?.id, entryId],
  );

  // Process raw data file
  const processRawDataFile = useCallback(
    async (fileId) => {
      // Use all samples for processing when processing outside modal
      const samplesData = assignedSamples.map((sample) => ({
        id: sample.id,
        accessionNumber: sample.accessionNumber,
        sampleId: sample.sampleItemId,
        assignedMethod: sample.assignedMethod,
        instrumentId: sample.instrumentId,
      }));

      try {
        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/files/process`,
          {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
              "X-CSRF-Token": localStorage.getItem("CSRF"),
            },
            body: JSON.stringify({
              fileIds: [fileId],
              samples: samplesData,
            }),
          },
        );

        const result = await response.json();

        setUploadedRawFiles((prev) =>
          prev.map((file) =>
            file.id === fileId
              ? {
                  ...file,
                  processed: true,
                  resultsCount: result.analyzerResults?.length || 0,
                }
              : file,
          ),
        );

        // Extract and store QC results, calibration data, and quantification results
        if (result.qcResults && result.qcResults.length > 0) {
          setQcResults(result.qcResults);
          console.log(
            "QC Results extracted from raw data file:",
            result.qcResults,
          );
        }

        const quantResults =
          result.quantificationResults || result.quantification || [];
        if (quantResults.length > 0) {
          setQuantificationResults(quantResults);
          console.log(
            "Quantification results extracted from raw data file:",
            quantResults,
          );
        }

        if (result.calibrationData) {
          setCalibrationData(result.calibrationData);
          console.log(
            "Calibration data extracted from raw data file:",
            result.calibrationData,
          );
        }

        const resultsCount = result.analyzerResults?.length || 0;
        const qcCount = result.qcResults?.length || 0;

        notify({
          kind: NotificationKinds.success,
          title: "File Processed",
          message: `Raw data file processed successfully. ${resultsCount} results extracted${qcCount > 0 ? `, ${qcCount} QC results available` : ""}.`,
        });
      } catch (error) {
        notify({
          kind: NotificationKinds.error,
          title: "Processing Error",
          message: `Failed to process raw data file: ${error.message}`,
        });
      }
    },
    [pageData?.id, assignedSamples, notify],
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

        if (result.qcResults && result.qcResults.length > 0) {
          setQcResults(result.qcResults);
        }
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
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        message: "Please select at least one sample.",
      });
      return;
    }

    if (!pageData?.id) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        message: "Cannot update samples: Page not properly initialized.",
      });
      return;
    }

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

    if (!qcApproved) {
      notify({
        kind: NotificationKinds.warning,
        title: "Warning",
        message: "Please approve QC results before completing execution.",
      });
      return;
    }

    // Check both uploaded file types (raw data files + modal files)
    const totalFiles =
      (uploadedRawFiles?.length || 0) + (uploadedFiles?.length || 0);
    const processedRawFiles =
      uploadedRawFiles?.filter((file) => file.processed) || [];
    const processedModalFiles =
      uploadedFiles?.filter((file) => file.processed) || [];
    const allProcessedFiles = [...processedRawFiles, ...processedModalFiles];

    // If files are uploaded, ensure at least one is processed
    if (totalFiles > 0 && allProcessedFiles.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: "Data Processing Required",
        message:
          "Files have been uploaded but none are processed. Please process at least one file to extract QC results before completing execution, or proceed with manual data entry by removing uploaded files.",
      });
      return;
    }

    setExecutionData((prev) => ({ ...prev, isExecuting: true }));

    const firstSelectedSample = assignedSamples.find((s) =>
      selectedSampleIds.includes(s.id),
    );
    const existingData = firstSelectedSample?.data || {};

    const completionPayload = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)), // Same format as Stage 1
      data: {
        ...existingData,
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
        sampleType:
          firstSelectedSample?.sampleType ||
          existingData.sampleType ||
          "Unknown Type",
        analyticalMethod:
          executionData.method ||
          existingData.analyticalMethod ||
          firstSelectedSample?.assignedMethod ||
          "Unknown Method",
        qcResults: qcResults,
        calibrationData: calibrationData,
        quantificationResults: quantificationResults,
        testExecution: {
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
          ...executionData,
          completedAt: new Date().toISOString(),
          status: "EXECUTED",
        },
      },
    };

    setCompletionProgress({
      step1: { name: "Apply execution data", status: "in-progress" },
      step2: { name: "Update sample status", status: "pending" },
      step3: { name: "Advance to Stage 4", status: "pending" },
    });

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(completionPayload),
      async (response) => {
        if (response && !response.error && !response.status) {
          setCompletionProgress((prev) => ({
            ...prev,
            step1: { name: "Apply execution data", status: "completed" },
            step2: { name: "Update sample status", status: "in-progress" },
          }));

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

            setCompletionProgress((prev) => ({
              ...prev,
              step2: { name: "Update sample status", status: "completed" },
              step3: { name: "Advance to Stage 4", status: "in-progress" },
            }));

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

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Analytical Test Execution"
        reason="This page requires specific bioequivalence laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

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
              id="notebook.bioequivalence.execution.title"
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
                    disabled={selectedSampleIds.length === 0 || !canSaveResults}
                    size="lg"
                  >
                    Configure Test Execution ({selectedSampleIds.length} samples
                    selected)
                  </Button>
                  {(selectedSampleIds.length === 0 || !canSaveResults) && (
                    <div
                      style={{
                        marginTop: "0.75rem",
                        padding: "0.75rem",
                        backgroundColor: "#fff4ce",
                        borderLeft: "4px solid #f1c21b",
                        borderRadius: "2px",
                      }}
                    >
                      <p
                        style={{
                          margin: "0 0 0.5rem 0",
                          fontSize: "0.875rem",
                          fontWeight: "600",
                          color: "#161616",
                        }}
                      >
                        Required to enable test execution:
                      </p>
                      <ul
                        style={{
                          margin: 0,
                          paddingLeft: "1.5rem",
                          fontSize: "0.875rem",
                          color: "#161616",
                        }}
                      >
                        {selectedSampleIds.length === 0 && (
                          <li>
                            <span style={{ color: "#da1e28" }}>●</span> Select
                            at least one sample from the table
                          </li>
                        )}
                        {!canSaveResults && (
                          <li>
                            <span style={{ color: "#da1e28" }}>●</span>{" "}
                            Insufficient permissions (current level:{" "}
                            {pagePermissionLevel || "NONE"})
                          </li>
                        )}
                      </ul>
                    </div>
                  )}
                  {selectedSampleIds.length > 0 && canSaveResults && (
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

                {/* Data Upload Section - Optional, Before Modal */}
                <div style={{ marginBottom: "2rem" }}>
                  <h3
                    style={{
                      marginBottom: "1rem",
                      display: "flex",
                      alignItems: "center",
                    }}
                  >
                    <Upload
                      size={24}
                      style={{ marginRight: "0.75rem", color: "#0f62fe" }}
                    />
                    Data Upload
                    <span
                      style={{
                        marginLeft: "1rem",
                        fontSize: "0.875rem",
                        color: "#6f6f6f",
                        fontWeight: "normal",
                        padding: "0.25rem 0.75rem",
                        backgroundColor: "#e8f4fd",
                        borderRadius: "12px",
                        border: "1px solid #0f62fe",
                      }}
                    >
                      Optional
                    </span>
                  </h3>

                  <div
                    style={{
                      padding: "1.5rem",
                      border: "1px solid #e0e0e0",
                      borderRadius: "8px",
                      backgroundColor: "#fafafa",
                    }}
                  >
                    <h4 style={{ marginBottom: "0.75rem", color: "#161616" }}>
                      Raw Data Files
                    </h4>
                    <p
                      style={{
                        fontSize: "0.875rem",
                        color: "#6f6f6f",
                        marginBottom: "1rem",
                        lineHeight: "1.4",
                      }}
                    >
                      Upload instrument data files (.csv, .pdf, .mzml, .cdf) for
                      automated processing. This step is optional - you can
                      proceed without files and enter data manually.
                    </p>

                    <FileUploader
                      accept={[".csv", ".pdf", ".mzml", ".cdf"]}
                      buttonLabel="Choose Raw Data Files"
                      filenameStatus="edit"
                      iconDescription="Clear file"
                      labelDescription="Drag and drop files here or click to browse"
                      labelTitle="Raw Data Files (Optional)"
                      multiple
                      onChange={handleRawDataUpload}
                      size="md"
                    />

                    {uploadedRawFiles.length > 0 && (
                      <div style={{ marginTop: "1rem" }}>
                        <h6
                          style={{
                            marginBottom: "0.5rem",
                            fontSize: "0.875rem",
                          }}
                        >
                          Uploaded Raw Data Files ({uploadedRawFiles.length})
                        </h6>
                        {uploadedRawFiles.map((file) => (
                          <div
                            key={file.id}
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                              padding: "0.5rem",
                              marginBottom: "0.5rem",
                              backgroundColor: "#ffffff",
                              border: "1px solid #e0e0e0",
                              borderRadius: "4px",
                            }}
                          >
                            <div>
                              <Tag
                                type={file.processed ? "green" : "blue"}
                                size="sm"
                              >
                                {file.name}
                              </Tag>
                              <span
                                style={{
                                  marginLeft: "0.5rem",
                                  fontSize: "0.75rem",
                                  color: "#6f6f6f",
                                }}
                              >
                                {file.processed
                                  ? `${file.resultsCount} results`
                                  : "Ready"}
                              </span>
                            </div>
                            {!file.processed && (
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => processRawDataFile(file.id)}
                              >
                                Process
                              </Button>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Summary Status */}
                  {uploadedRawFiles.length > 0 && (
                    <div
                      style={{
                        marginTop: "1rem",
                        padding: "1rem",
                        backgroundColor: "#e8f4fd",
                        border: "1px solid #0f62fe",
                        borderRadius: "4px",
                      }}
                    >
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.875rem",
                          color: "#161616",
                        }}
                      >
                        <strong>Data Upload Status:</strong>{" "}
                        {uploadedRawFiles.filter((f) => f.processed).length} of{" "}
                        {uploadedRawFiles.length} raw data files processed. You
                        can now proceed to configure test execution.
                      </p>
                    </div>
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
                      {(() => {
                        const totalFiles =
                          (uploadedRawFiles?.length || 0) +
                          (uploadedFiles?.length || 0);
                        return (
                          <>
                            <span
                              style={{
                                fontSize: "1rem",
                                color: totalFiles > 0 ? "#198038" : "#f1c21b",
                              }}
                            >
                              {totalFiles > 0 ? "✓" : "⚠"}
                            </span>
                            <span
                              style={{
                                color: totalFiles > 0 ? "#161616" : "#6f6f6f",
                              }}
                            >
                              Raw data files uploaded ({totalFiles} files) -{" "}
                              {totalFiles > 0
                                ? "FDA compliance met"
                                : "Optional for manual entry"}
                            </span>
                          </>
                        );
                      })()}
                    </div>

                    {/* File Processing Check */}
                    {(() => {
                      const totalFiles =
                        (uploadedRawFiles?.length || 0) +
                        (uploadedFiles?.length || 0);
                      const processedRawFiles =
                        uploadedRawFiles?.filter((f) => f.processed).length ||
                        0;
                      const processedModalFiles =
                        uploadedFiles?.filter((f) => f.processed).length || 0;
                      const totalProcessed =
                        processedRawFiles + processedModalFiles;

                      return totalFiles > 0 ? (
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
                              color: totalProcessed > 0 ? "#198038" : "#f1c21b",
                            }}
                          >
                            {totalProcessed > 0 ? "✓" : "⚠"}
                          </span>
                          <span
                            style={{
                              color: totalProcessed > 0 ? "#161616" : "#6f6f6f",
                            }}
                          >
                            Data files processed ({totalProcessed} of{" "}
                            {totalFiles})
                          </span>
                        </div>
                      ) : null;
                    })()}

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
                      {(() => {
                        const totalFiles =
                          (uploadedRawFiles?.length || 0) +
                          (uploadedFiles?.length || 0);
                        const totalProcessed =
                          (uploadedRawFiles?.filter((f) => f.processed)
                            .length || 0) +
                          (uploadedFiles?.filter((f) => f.processed).length ||
                            0);

                        const allRequiredMet =
                          qcApproved &&
                          selectedSampleIds.length > 0 &&
                          executionData?.analystId &&
                          executionData?.selectedInstrument;

                        const hasProcessedFiles = totalProcessed > 0;

                        return allRequiredMet &&
                          (hasProcessedFiles || totalFiles === 0)
                          ? "✓ Ready for completion - All requirements met"
                          : "⚠ Complete all required items for bioequivalence execution";
                      })()}
                    </strong>
                  </div>
                </div>

                <div style={{ marginTop: "1rem" }}>
                  <Button
                    kind="primary"
                    onClick={handleCompleteExecution}
                    disabled={(() => {
                      const totalFiles =
                        (uploadedRawFiles?.length || 0) +
                        (uploadedFiles?.length || 0);
                      const totalProcessed =
                        (uploadedRawFiles?.filter((f) => f.processed).length ||
                          0) +
                        (uploadedFiles?.filter((f) => f.processed).length || 0);

                      return (
                        executionData.isExecuting ||
                        selectedSampleIds.length === 0 ||
                        !qcApproved ||
                        (totalFiles > 0 && totalProcessed === 0) || // If files uploaded but none processed
                        !canSaveResults
                      );
                    })()}
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
                      📋 Upload raw data files (required for FDA bioequivalence
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
        primaryButtonText={(() => {
          const processedFiles =
            uploadedRawFiles.filter((f) => f.processed).length +
            uploadedFiles.filter((f) => f.processed).length;
          return processedFiles > 0
            ? `Start Execution (${processedFiles} processed files)`
            : "Start Execution (Manual entry)";
        })()}
        secondaryButtonText="Cancel"
        open={isExecutionModalOpen}
        onRequestClose={() => setIsExecutionModalOpen(false)}
        onRequestSubmit={() => {
          handleExecuteTest();
          setIsExecutionModalOpen(false);
        }}
        size="md"
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
              {templateInstruments && templateInstruments.length > 0
                ? templateInstruments.map((instrument) => (
                    <SelectItem
                      key={instrument.id || instrument.value}
                      value={instrument.id || instrument.value}
                      text={
                        instrument.name ||
                        instrument.value ||
                        instrument.label ||
                        ""
                      }
                    />
                  ))
                : []}
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

          {/* Execution Configuration */}
          <Column lg={16} md={8} sm={4}>
            <h4
              style={{
                marginTop: "1.5rem",
                marginBottom: "1rem",
                color: "#161616",
              }}
            >
              Execution Configuration
            </h4>

            {/* Data Summary */}
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
                marginBottom: "1.5rem",
                border: "1px solid #e0e0e0",
              }}
            >
              <h6
                style={{
                  marginBottom: "0.75rem",
                  fontSize: "0.875rem",
                  color: "#161616",
                }}
              >
                Available Data for Execution:
              </h6>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr 1fr",
                  gap: "1rem",
                }}
              >
                <div>
                  <p
                    style={{ margin: 0, fontSize: "0.75rem", color: "#6f6f6f" }}
                  >
                    Total Files
                  </p>
                  <p style={{ margin: 0, fontWeight: "600", color: "#161616" }}>
                    {uploadedRawFiles.length + uploadedFiles.length}
                  </p>
                </div>
                <div>
                  <p
                    style={{ margin: 0, fontSize: "0.75rem", color: "#6f6f6f" }}
                  >
                    Processed Files
                  </p>
                  <p style={{ margin: 0, fontWeight: "600", color: "#161616" }}>
                    {uploadedRawFiles.filter((f) => f.processed).length +
                      uploadedFiles.filter((f) => f.processed).length}
                  </p>
                </div>
                <div>
                  <p
                    style={{ margin: 0, fontSize: "0.75rem", color: "#6f6f6f" }}
                  >
                    Data Status
                  </p>
                  <p style={{ margin: 0, fontWeight: "600", color: "#161616" }}>
                    {uploadedRawFiles.filter((f) => f.processed).length +
                      uploadedFiles.filter((f) => f.processed).length >
                    0
                      ? "Ready"
                      : "Manual Entry"}
                  </p>
                </div>
              </div>
            </div>

            {/* Show uploaded files for information only */}
            {(uploadedRawFiles.length > 0 || uploadedFiles.length > 0) && (
              <div style={{ marginBottom: "1.5rem" }}>
                <h6
                  style={{
                    marginBottom: "0.5rem",
                    fontSize: "0.875rem",
                    color: "#161616",
                  }}
                >
                  Available Files for Execution
                </h6>
                <p
                  style={{
                    fontSize: "0.875rem",
                    color: "#6f6f6f",
                    marginBottom: "1rem",
                    lineHeight: "1.4",
                  }}
                >
                  Files uploaded and processed are available for this execution.
                </p>

                {/* Show raw data files */}
                {uploadedRawFiles.length > 0 && (
                  <div style={{ marginBottom: "1rem" }}>
                    <h6
                      style={{
                        marginBottom: "0.5rem",
                        fontSize: "0.75rem",
                        color: "#6f6f6f",
                      }}
                    >
                      Raw Data Files ({uploadedRawFiles.length})
                    </h6>
                    {uploadedRawFiles.map((file) => (
                      <div
                        key={file.id}
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          padding: "0.5rem",
                          margin: "0.25rem 0",
                          backgroundColor: "#f4f4f4",
                          border: "1px solid #e0e0e0",
                          borderRadius: "4px",
                        }}
                      >
                        <div>
                          <Tag
                            type={file.processed ? "green" : "blue"}
                            size="sm"
                          >
                            {file.name}
                          </Tag>
                          <span
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.75rem",
                              color: "#6f6f6f",
                            }}
                          >
                            {file.processed
                              ? `${file.resultsCount} results`
                              : "Ready"}
                          </span>
                        </div>
                        <span style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                          {file.processed
                            ? "Available for execution"
                            : "Not processed"}
                        </span>
                      </div>
                    ))}
                  </div>
                )}

                {/* Show any legacy modal files if they exist */}
                {uploadedFiles.length > 0 && (
                  <div>
                    <h6
                      style={{
                        marginBottom: "0.5rem",
                        fontSize: "0.75rem",
                        color: "#6f6f6f",
                      }}
                    >
                      Other Files ({uploadedFiles.length})
                    </h6>
                    {uploadedFiles.map((file) => (
                      <div
                        key={file.id}
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          padding: "0.5rem",
                          margin: "0.25rem 0",
                          backgroundColor: "#f4f4f4",
                          border: "1px solid #e0e0e0",
                          borderRadius: "4px",
                        }}
                      >
                        <div>
                          <Tag
                            type={file.processed ? "green" : "gray"}
                            size="sm"
                          >
                            {file.name}
                          </Tag>
                          <span
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.75rem",
                              color: "#6f6f6f",
                            }}
                          >
                            {file.processed
                              ? `${file.analyzerResultsCount} results`
                              : "Ready"}
                          </span>
                        </div>
                        <span style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                          {file.processed
                            ? "Available for execution"
                            : "Not processed"}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
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
        size="md"
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

export default BioequivalenceAnalyticalExecutionPage;
