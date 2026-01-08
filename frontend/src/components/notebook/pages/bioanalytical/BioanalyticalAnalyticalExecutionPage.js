import { useState, useCallback, useEffect, useContext, useRef } from "react";
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
import config from "../../../../config.json";
import "./BioanalyticalPages.css";

/**
 * Analytical methods available for bioanalytical testing (from Stage 2)
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
 * Bioanalytical Analyzers - Match Stage 2 assignments
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
 * Stage 3: Analytical Test Execution (Simplified)
 * - Tab 1: Test Execution & Data Capture
 * - Tab 2: QC Verification
 * - Tab 3: Deviations & Completion
 */
function BioanalyticalAnalyticalExecutionPage({
  pageData,
  onProgressUpdate,
  templateInstruments,
  entryId,
}) {
  const { addNotification } = useContext(NotificationContext);
  const isMountedRef = useRef(true);

  // ============================================================================
  // STATE MANAGEMENT
  // ============================================================================

  // UI State
  const [selectedTab, setSelectedTab] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);

  // Tab 1: Test Execution & Data Capture
  const [assignedSamples, setAssignedSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState(new Set());
  const [selectedInstrument, setSelectedInstrument] = useState("");
  const [executionData, setExecutionData] = useState({
    analystId: "",
    executionDate: new Date().toISOString().split("T")[0],
    testParameters: {},
    notes: "",
  });
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);

  // Tab 2: QC Results
  const [qcResults, setQcResults] = useState([]);
  const [qcApproved, setQcApproved] = useState(false);
  const [calibrationData, setCalibrationData] = useState(null);
  const [quantificationResults, setQuantificationResults] = useState([]);

  // Tab 3: Deviations
  const [deviations, setDeviations] = useState([]);
  const [deviationForm, setDeviationForm] = useState({
    type: "",
    description: "",
    correctiveAction: "",
  });
  const [isDeviationModalOpen, setIsDeviationModalOpen] = useState(false);

  // Execution Details Modal
  const [isExecutionModalOpen, setIsExecutionModalOpen] = useState(false);

  // Instruments
  const [instruments, setInstruments] = useState([]);

  // ============================================================================
  // EFFECTS
  // ============================================================================

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    const availableInstruments = (
      templateInstruments && templateInstruments.length > 0
        ? templateInstruments
        : BIOANALYTICAL_ANALYZERS
    ).sort((a, b) => a.machine.localeCompare(b.machine));
    setInstruments(availableInstruments);
  }, [templateInstruments]);

  // Load assigned samples from Stage 2 page
  useEffect(() => {
    const loadAssignedSamples = async () => {
      if (!pageData?.id) return;

      try {
        setIsLoading(true);
        // Fetch samples for this Stage 3 page using the same endpoint as Stage 2
        // This endpoint returns enriched sample data including accession numbers
        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`,
          { credentials: "include" },
        );

        if (!response.ok) {
          throw new Error("Failed to load samples for Stage 3");
        }

        const data = await response.json();
        const samples = Array.isArray(data) ? data : [];

        const processedSamples = samples.map((sample) => {
          // The endpoint returns enriched sample data with accession number, sampleType, etc.
          const assignmentData = sample.data || {};

          // Find the analytical method from our methods list
          const analyticalMethod = ANALYTICAL_METHODS.find(
            (m) => m.id === assignmentData.analyticalMethod,
          );
          const methodName =
            analyticalMethod?.name || assignmentData.analyticalMethod;

          // Find instrument name from available instruments
          const instrument = instruments.find(
            (i) => i.id === assignmentData.instrumentId,
          );

          return {
            id: sample.id,
            sampleItemId: sample.id,
            accessionNumber: sample.accessionNumber || "-",
            sampleType: sample.sampleType || "-",
            requestedTests: "-",
            assignedMethod: methodName,
            assignedMethodId: assignmentData.analyticalMethod,
            methodDescription: analyticalMethod?.description,
            instrumentId: assignmentData.instrumentId,
            instrumentName:
              instrument?.machine ||
              `Instrument ${assignmentData.instrumentId}`,
            assignedStaff: assignmentData.assignedStaff,
            status: sample.status || "PENDING",
            data: assignmentData,
          };
        });

        setAssignedSamples(processedSamples);
      } catch (error) {
        console.error("Error loading assigned samples:", error);
        // Don't show error notification on initial load - use empty state instead
        setAssignedSamples([]);
      } finally {
        setIsLoading(false);
      }
    };

    loadAssignedSamples();
  }, [pageData?.id]);

  // Extract QC data from samples when they're loaded (for page refresh scenarios)
  useEffect(() => {
    if (assignedSamples.length > 0 && qcResults.length === 0) {
      // Check if any sample has processing results with QC data
      const extractedQcResults = [];
      let extractedCalibrationData = null;
      let extractedQuantificationResults = [];

      assignedSamples.forEach((sample) => {
        const uploadedFiles = sample.data?.uploadedFiles || [];
        uploadedFiles.forEach((file) => {
          if (file.processingResults && file.processingResults.success) {
            // Extract QC results
            if (
              file.processingResults.qcResults &&
              file.processingResults.qcResults.length > 0
            ) {
              extractedQcResults.push(...file.processingResults.qcResults);
            }
            // Extract calibration data (only once, from first file)
            if (
              file.processingResults.calibrationData &&
              !extractedCalibrationData
            ) {
              extractedCalibrationData = file.processingResults.calibrationData;
            }
            // Extract quantification results
            if (
              file.processingResults.quantification &&
              file.processingResults.quantification.length > 0
            ) {
              extractedQuantificationResults.push(
                ...file.processingResults.quantification,
              );
            }
          }
        });
      });

      // Set state only if we found data
      if (extractedQcResults.length > 0) {
        setQcResults(extractedQcResults);
      }
      if (extractedCalibrationData) {
        setCalibrationData(extractedCalibrationData);
      }
      if (extractedQuantificationResults.length > 0) {
        setQuantificationResults(extractedQuantificationResults);
      }
    }
  }, [assignedSamples]);

  // ============================================================================
  // HANDLERS
  // ============================================================================

  const notify = useCallback(
    (message, kind = NotificationKinds.success) => {
      addNotification({ message, kind });
    },
    [addNotification],
  );

  const handleSampleSelection = useCallback((sampleId, isSelected) => {
    setSelectedSampleIds((prev) => {
      const newSet = new Set(prev);
      if (isSelected) {
        newSet.add(sampleId);
      } else {
        newSet.delete(sampleId);
      }
      return newSet;
    });
  }, []);

  const handleSelectAll = useCallback(
    (isSelected) => {
      if (isSelected) {
        setSelectedSampleIds(new Set(assignedSamples.map((s) => s.id)));
      } else {
        setSelectedSampleIds(new Set());
      }
    },
    [assignedSamples],
  );

  const handleFileSelect = useCallback((file) => {
    setSelectedFile(file);
  }, []);

  const handleRemoveSelectedFile = useCallback(() => {
    setSelectedFile(null);
  }, []);

  const handleFileUpload = useCallback(async () => {
    if (!selectedFile) {
      notify("Please select a file first", NotificationKinds.warning);
      return;
    }
    if (!selectedInstrument) {
      notify("Please select an instrument first", NotificationKinds.warning);
      return;
    }

    const fileExtension = selectedFile.name.split(".").pop().toUpperCase();
    const instrument = instruments.find((i) => i.id === selectedInstrument);

    if (!instrument.formats.includes(fileExtension)) {
      notify(
        `Invalid format. Expected: ${instrument.formats.join(", ")}`,
        NotificationKinds.error,
      );
      return;
    }

    try {
      // Create FormData for file upload
      const formData = new FormData();
      formData.append("file", selectedFile);
      formData.append("instrumentId", selectedInstrument);
      formData.append("pageId", pageData?.id);
      formData.append("entryId", entryId || "");

      // Upload file to correct endpoint
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData?.id}/files/upload`,
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
        throw new Error(`Upload failed: ${response.status}`);
      }

      const result = await response.json();

      if (result.success) {
        // Store file metadata from upload response
        const fileMetadata = {
          id: result.fileId,
          name: result.fileName,
          size: result.fileSize,
          fileUrl: result.fileUrl,
          uploadedAt: result.uploadedAt,
          instrumentId: selectedInstrument,
          instrumentName: instrument.machine,
          uploaded: true,
          processed: false, // Will be true after processing
        };

        setUploadedFiles((prev) => [...prev, fileMetadata]);
        setSelectedFile(null); // Clear selection after upload
        notify(`File uploaded successfully: ${result.fileName}`);
      } else {
        throw new Error(result.error || "File upload failed");
      }
    } catch (error) {
      console.error("File upload error:", error);
      notify(`File upload failed: ${error.message}`, NotificationKinds.error);
    }
  }, [
    selectedFile,
    selectedInstrument,
    instruments,
    pageData?.id,
    entryId,
    notify,
  ]);

  const handleProcessFiles = useCallback(
    async (fileIds) => {
      if (!fileIds || fileIds.length === 0) {
        notify("No files to process", NotificationKinds.warning);
        return;
      }

      try {
        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData?.id}/files/process`,
          {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
              "X-CSRF-Token": localStorage.getItem("CSRF"),
            },
            body: JSON.stringify({
              fileIds: fileIds,
              samples: Array.from(selectedSampleIds).map((id) => ({
                sampleId: id,
                // Add any additional sample info needed
              })),
            }),
          },
        );

        if (!response.ok) {
          throw new Error(`Processing failed: ${response.status}`);
        }

        const result = await response.json();

        if (result.success) {
          // Update files to show they've been processed
          setUploadedFiles((prev) =>
            prev.map((file) =>
              fileIds.includes(file.id)
                ? { ...file, processed: true, processingResults: result }
                : file,
            ),
          );

          // Extract and populate QC data from processing results
          if (result.qcResults && result.qcResults.length > 0) {
            setQcResults(result.qcResults);
          }
          if (result.calibrationData) {
            setCalibrationData(result.calibrationData);
          }
          if (
            result.quantificationResults &&
            result.quantificationResults.length > 0
          ) {
            setQuantificationResults(result.quantificationResults);
          }

          notify(
            `Files processed successfully. QC data automatically loaded for Tab 2 verification.`,
          );
        } else {
          throw new Error(result.error || "File processing failed");
        }
      } catch (error) {
        console.error("File processing error:", error);
        notify(
          `File processing failed: ${error.message}`,
          NotificationKinds.error,
        );
      }
    },
    [pageData?.id, selectedSampleIds, notify],
  );

  const handleExecuteTest = useCallback(async () => {
    if (selectedSampleIds.size === 0) {
      notify("Please select at least one sample", NotificationKinds.warning);
      return;
    }
    if (!selectedInstrument) {
      notify("Please select an instrument", NotificationKinds.warning);
      return;
    }
    if (!executionData.analystId) {
      notify("Please enter Analyst ID", NotificationKinds.warning);
      return;
    }
    if (uploadedFiles.length === 0) {
      notify(
        "Please upload at least one raw data file",
        NotificationKinds.warning,
      );
      return;
    }
    if (!uploadedFiles.some((file) => file.processed)) {
      notify(
        "Please process all uploaded files before recording execution",
        NotificationKinds.warning,
      );
      return;
    }

    try {
      setIsExecuting(true);

      // Build complete execution data with all QC results collected from files
      const executionPayload = {
        sampleIds: Array.from(selectedSampleIds),
        data: {
          // Core execution information
          executionStatus: "EXECUTED",
          executedAt: new Date().toISOString(),
          executedBy: executionData.analystId,
          instrumentId: selectedInstrument,
          executionDate: executionData.executionDate,
          notes: executionData.notes,

          // Raw data capture
          uploadedFiles: uploadedFiles,

          // QC results from file processing
          qcResults: qcResults,
          quantificationResults: quantificationResults,
          calibrationData: calibrationData,

          // QC verification status
          qcVerified: false, // Will be set to true in Tab 3
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

      notify(
        "Test execution recorded with raw data and QC results. Proceeding to QC Verification.",
      );
      setSelectedTab(1); // Move to QC verification tab
    } catch (error) {
      console.error("Execution error:", error);
      notify("Test execution failed", NotificationKinds.error);
    } finally {
      setIsExecuting(false);
    }
  }, [
    selectedSampleIds,
    selectedInstrument,
    pageData?.id,
    executionData,
    uploadedFiles,
    qcResults,
    quantificationResults,
    calibrationData,
    notify,
  ]);

  const handleAddDeviation = useCallback(() => {
    if (!deviationForm.type || !deviationForm.description) {
      notify("Please fill in all required fields", NotificationKinds.warning);
      return;
    }

    const deviation = {
      id: `DEV-${Date.now()}`,
      ...deviationForm,
      recordedAt: new Date().toISOString(),
      recordedBy: executionData.analystId,
    };

    setDeviations((prev) => [...prev, deviation]);
    setDeviationForm({ type: "", description: "", correctiveAction: "" });
    setIsDeviationModalOpen(false);
    notify("Deviation recorded");
  }, [deviationForm, executionData.analystId, notify]);

  const handleCompleteExecution = useCallback(async () => {
    // Validate that we have all required data BEFORE setting isExecuting
    if (!qcApproved) {
      notify(
        "Please approve QC results before completing execution",
        NotificationKinds.warning,
      );
      return;
    }

    if (qcResults.length === 0 && quantificationResults.length === 0) {
      notify(
        "No QC or quantification data found. Please load QC results first.",
        NotificationKinds.warning,
      );
      return;
    }

    try {
      setIsExecuting(true);

      // Build common execution data that applies to all samples
      const commonExecutionData = {
        // Execution status and metadata (common to all samples)
        executionStatus: "COMPLETED",
        completedAt: new Date().toISOString(),
        completedBy: executionData.analystId,
        executionDate: executionData.executionDate,
        notes: executionData.notes,

        // QC verification data (summary for all samples)
        qcApproved: qcApproved,
        qcSummary: {
          totalQcSamples: qcResults.length,
          passedQcSamples: qcResults.filter((qc) => qc.status === "PASS")
            .length,
          failedQcSamples: qcResults.filter((qc) => qc.status !== "PASS")
            .length,
        },

        // Calibration data (shared across all samples)
        calibrationData: calibrationData,
        calibrationAccepted: calibrationData
          ? calibrationData.qualityAssessment === "PASS"
          : false,

        // Instrument and file information (common to all samples)
        instrumentId: selectedInstrument,
        instrumentName: instruments.find((i) => i.id === selectedInstrument)
          ?.machine,
        uploadedFiles: uploadedFiles,

        // Deviations (common issues)
        deviations: deviations,
        deviationCount: deviations.length,

        // Stage 3 completion certification (common to all samples)
        stage3Completed: true,
        stage3CompletedBy: executionData.analystId,
        stage3CompletedAt: new Date().toISOString(),
        readyForReporting: true,

        // Sample-specific results (organized by sample ID for easy lookup in Stage 4)
        sampleSpecificResults: {},
      };

      // Add sample-specific results organized by sample ID
      Array.from(selectedSampleIds).forEach((sampleId) => {
        // Get QC results for this sample
        const sampleQcResults = qcResults.filter(
          (qc) => qc.sampleId === sampleId,
        );

        // Get quantification results for this sample
        const sampleQuantResults = quantificationResults.filter(
          (quant) => quant.sampleId === sampleId,
        );

        commonExecutionData.sampleSpecificResults[sampleId] = {
          qcResults: sampleQcResults,
          quantificationResults: sampleQuantResults,
          analyzerResults: sampleQuantResults.map((quant) => ({
            result: `${quant.concentration} ${quant.units}`,
            quality: quant.status,
            instrumentSampleId: quant.instrumentSampleId,
            peakArea: quant.peakArea,
            accuracy: quant.accuracyPercent,
            cvPercent: quant.cvPercent,
          })),
          sampleId: sampleId,
          processedAt: new Date().toISOString(),
        };
      });

      // Send the execution data to the backend using the correct format
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify({
            sampleIds: Array.from(selectedSampleIds),
            data: commonExecutionData,
          }),
        },
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Completion failed");
      }

      const responseData = await response.json();

      notify(
        `Test execution completed successfully for ${selectedSampleIds.size} samples. All data has been preserved for reporting.`,
      );

      if (onProgressUpdate) {
        onProgressUpdate();
      }

      // Clear local state as data is now persisted
      setSelectedSampleIds(new Set());
      setQcResults([]);
      setQuantificationResults([]);
      setCalibrationData(null);
      setDeviations([]);
      setQcApproved(false);
    } catch (error) {
      console.error("Completion error:", error);
      notify(`Completion failed: ${error.message}`, NotificationKinds.error);
    } finally {
      setIsExecuting(false);
    }
  }, [
    pageData?.id,
    selectedSampleIds,
    executionData,
    qcApproved,
    qcResults,
    quantificationResults,
    calibrationData,
    deviations,
    selectedInstrument,
    instruments,
    uploadedFiles,
    onProgressUpdate,
    notify,
  ]);

  // Render execution status column with QC processing indicator
  const renderExecutionStatus = (sample) => {
    if (!sample) {
      return null;
    }

    const uploadedFiles = sample.data?.uploadedFiles || [];
    const hasProcessedFiles = uploadedFiles.some((f) => f.processed);
    const executionStatus = sample.data?.executionStatus;

    // Check if files have been processed and uploaded
    if (uploadedFiles.length > 0 && hasProcessedFiles) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="green" size="sm">
            ✅ QC Data Loaded
          </Tag>
          {executionStatus === "EXECUTED" && (
            <div style={{ marginTop: "4px" }}>
              <Tag type="blue" size="sm">
                Execution Recorded
              </Tag>
            </div>
          )}
        </div>
      );
    }

    if (uploadedFiles.length > 0) {
      return (
        <span style={{ color: "#f1c21b", fontSize: "12px" }}>
          ⏳ Processing Files...
        </span>
      );
    }

    return <span style={{ color: "#8d8d8d", fontSize: "12px" }}>Pending</span>;
  };

  // ============================================================================
  // RENDER
  // ============================================================================

  if (isLoading && assignedSamples.length === 0) {
    return <Loading description="Loading assigned samples..." withOverlay />;
  }

  return (
    <div style={{ padding: "1.5rem" }}>
      <h2>
        <FormattedMessage
          id="notebook.bioanalytical.execution.title"
          defaultMessage="Stage 3: Analytical Test Execution"
        />
      </h2>

      <Tabs
        selectedIndex={selectedTab}
        onChange={(evt) => setSelectedTab(evt.selectedIndex)}
      >
        <TabList aria-label="Analytical execution tabs">
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab1"
              defaultMessage="Test Execution & Data"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab2"
              defaultMessage="QC Verification"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab3"
              defaultMessage="Deviations & Completion"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* TAB 1: Test Execution & Data Capture */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <h4>Execute Assigned Tests & Capture Raw Data</h4>

                  {/* Selected Samples */}
                  <div style={{ marginBottom: "2rem", marginTop: "1.5rem" }}>
                    <Grid style={{ marginBottom: "1rem" }}>
                      <Column
                        lg={16}
                        md={8}
                        sm={4}
                        style={{ textAlign: "right" }}
                      >
                        <Button
                          kind="primary"
                          onClick={() => setIsExecutionModalOpen(true)}
                          disabled={selectedSampleIds.size === 0}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.continueExecution"
                            defaultMessage="Continue with Execution (Steps 2-4)"
                          />
                        </Button>
                      </Column>
                    </Grid>
                    <div
                      style={{
                        overflowX: "auto",
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                      }}
                    >
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <Checkbox
                                id="select-all-samples"
                                labelText="Select all samples"
                                checked={
                                  selectedSampleIds.size > 0 &&
                                  selectedSampleIds.size ===
                                    assignedSamples.length
                                }
                                onChange={(event, { checked, id }) => {
                                  handleSelectAll(checked);
                                }}
                                hideLabel
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.sampleId"
                                defaultMessage="Sample ID"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.assignedMethod"
                                defaultMessage="Assigned Method"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.assignedStaff"
                                defaultMessage="Assigned Staff"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.instrument"
                                defaultMessage="Instrument"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.status"
                                defaultMessage="Status"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.executionProgress"
                                defaultMessage="Execution Progress"
                              />
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {assignedSamples.length > 0 ? (
                            assignedSamples.map((sample) => (
                              <TableRow key={sample.id}>
                                <TableCell>
                                  <Checkbox
                                    id={`sample-${sample.id}`}
                                    labelText={`Select Sample ${sample.sampleItemId}`}
                                    checked={selectedSampleIds.has(sample.id)}
                                    onChange={(event, { checked, id }) => {
                                      handleSampleSelection(sample.id, checked);
                                    }}
                                    hideLabel
                                  />
                                </TableCell>
                                <TableCell>
                                  <strong>{sample.accessionNumber}</strong>
                                </TableCell>
                                <TableCell>
                                  <Tag type="blue" size="sm">
                                    {sample.assignedMethod || "Not assigned"}
                                  </Tag>
                                </TableCell>
                                <TableCell>
                                  <Tag type="cyan" size="sm">
                                    {sample.assignedStaff || "-"}
                                  </Tag>
                                </TableCell>
                                <TableCell>
                                  <Tag type="gray" size="sm">
                                    {sample.instrumentName ||
                                      sample.instrumentId ||
                                      "-"}
                                  </Tag>
                                </TableCell>
                                <TableCell>
                                  <Tag type="green" size="sm">
                                    Ready
                                  </Tag>
                                </TableCell>
                                <TableCell>
                                  {renderExecutionStatus(sample)}
                                </TableCell>
                              </TableRow>
                            ))
                          ) : (
                            <TableRow>
                              <TableCell
                                colSpan="7"
                                style={{
                                  textAlign: "center",
                                  padding: "2rem",
                                }}
                              >
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.noSamples"
                                  defaultMessage="No samples available for execution. Please complete Stage 2 (Test Assignment) first."
                                />
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </div>
                    <p style={{ marginTop: "0.5rem", fontSize: "0.875rem" }}>
                      {selectedSampleIds.size} of {assignedSamples.length}{" "}
                      selected
                    </p>
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* TAB 2: QC Verification */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <h4>QC Verification: Controls & Acceptance Criteria</h4>

                  {isLoading ? (
                    <Loading description="Loading QC results..." />
                  ) : qcResults.length > 0 ? (
                    <>
                      <div
                        style={{
                          marginTop: "1.5rem",
                          overflowX: "auto",
                          border: "1px solid #e0e0e0",
                          borderRadius: "4px",
                        }}
                      >
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>QC Level</TableHeader>
                              <TableHeader>Spiked Value</TableHeader>
                              <TableHeader>Measured Value</TableHeader>
                              <TableHeader>Accuracy %</TableHeader>
                              <TableHeader>Precision %</TableHeader>
                              <TableHeader>Acceptance Range</TableHeader>
                              <TableHeader>Status</TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {qcResults.map((qc) => (
                              <TableRow key={qc.id}>
                                <TableCell>
                                  <strong>{qc.level || "QC"}</strong>
                                </TableCell>
                                <TableCell>
                                  {qc.spikedConcentration || "-"}
                                </TableCell>
                                <TableCell>{qc.measuredValue || "-"}</TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      color:
                                        qc.accuracy !== null &&
                                        qc.acceptanceCriteria &&
                                        qc.accuracy >=
                                          qc.acceptanceCriteria.min &&
                                        qc.accuracy <= qc.acceptanceCriteria.max
                                          ? "#24a148"
                                          : "#da1e28",
                                      fontWeight: "bold",
                                    }}
                                  >
                                    {qc.accuracy !== null
                                      ? `${qc.accuracy.toFixed(1)}%`
                                      : "-"}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      color:
                                        qc.precision !== null &&
                                        qc.precision <=
                                          (qc.precisionLimit || 20)
                                          ? "#24a148"
                                          : "#da1e28",
                                      fontWeight: "bold",
                                    }}
                                  >
                                    {qc.precision !== null
                                      ? `${qc.precision.toFixed(1)}%`
                                      : "-"}
                                  </span>
                                </TableCell>
                                <TableCell style={{ fontSize: "0.875rem" }}>
                                  {qc.acceptanceCriteria
                                    ? `${qc.acceptanceCriteria.min}-${qc.acceptanceCriteria.max}%`
                                    : "N/A"}
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      backgroundColor:
                                        (qc.accuracy !== null &&
                                          qc.acceptanceCriteria &&
                                          qc.accuracy >=
                                            qc.acceptanceCriteria.min &&
                                          qc.accuracy <=
                                            qc.acceptanceCriteria.max) ||
                                        (qc.precision !== null &&
                                          qc.precision <=
                                            (qc.precisionLimit || 20))
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {(qc.accuracy !== null &&
                                      qc.acceptanceCriteria &&
                                      qc.accuracy >=
                                        qc.acceptanceCriteria.min &&
                                      qc.accuracy <=
                                        qc.acceptanceCriteria.max) ||
                                    (qc.precision !== null &&
                                      qc.precision <= (qc.precisionLimit || 20))
                                      ? "PASS"
                                      : "FAIL"}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>

                      {/* Calibration Curve Section */}
                      {calibrationData && (
                        <div
                          style={{
                            marginTop: "2rem",
                            padding: "1rem",
                            backgroundColor: "#e8f5e9",
                            borderRadius: "4px",
                            border: "1px solid #24a148",
                          }}
                        >
                          <h5>📈 Calibration Curve Assessment</h5>
                          <Grid style={{ marginTop: "1rem" }}>
                            <Column lg={8} md={4} sm={2}>
                              <div style={{ marginBottom: "0.5rem" }}>
                                <strong>R²:</strong> {calibrationData.rSquared}
                                <Tag
                                  type={
                                    calibrationData.rSquared >= 0.995
                                      ? "green"
                                      : "red"
                                  }
                                  size="sm"
                                  style={{ marginLeft: "0.5rem" }}
                                >
                                  {calibrationData.rSquared >= 0.995
                                    ? "PASS"
                                    : "FAIL"}
                                </Tag>
                              </div>
                              <div style={{ marginBottom: "0.5rem" }}>
                                <strong>Equation:</strong>{" "}
                                {calibrationData.equation}
                              </div>
                              <div style={{ marginBottom: "0.5rem" }}>
                                <strong>Slope:</strong> {calibrationData.slope}
                              </div>
                            </Column>
                            <Column lg={8} md={4} sm={2}>
                              <div style={{ marginBottom: "0.5rem" }}>
                                <strong>Intercept:</strong>{" "}
                                {calibrationData.intercept}
                              </div>
                              <div style={{ marginBottom: "0.5rem" }}>
                                <strong>Source:</strong>{" "}
                                {calibrationData.instrumentName} -{" "}
                                {calibrationData.fileName}
                              </div>
                              <div style={{ marginBottom: "0.5rem" }}>
                                <strong>Overall Assessment:</strong>
                                <Tag
                                  type={
                                    calibrationData.qualityAssessment === "PASS"
                                      ? "green"
                                      : "red"
                                  }
                                  size="sm"
                                  style={{ marginLeft: "0.5rem" }}
                                >
                                  {calibrationData.qualityAssessment}
                                </Tag>
                              </div>
                            </Column>
                          </Grid>
                        </div>
                      )}

                      {/* Quantification Results Section */}
                      {quantificationResults.length > 0 && (
                        <div style={{ marginTop: "2rem" }}>
                          <h5>🧪 Sample Quantification Results</h5>
                          <div
                            style={{
                              marginTop: "1rem",
                              overflowX: "auto",
                              border: "1px solid #e0e0e0",
                              borderRadius: "4px",
                            }}
                          >
                            <Table>
                              <TableHead>
                                <TableRow>
                                  <TableHeader>Sample</TableHeader>
                                  <TableHeader>Instrument ID</TableHeader>
                                  <TableHeader>Concentration</TableHeader>
                                  <TableHeader>Units</TableHeader>
                                  <TableHeader>Peak Area</TableHeader>
                                  <TableHeader>CV %</TableHeader>
                                  <TableHeader>Accuracy %</TableHeader>
                                  <TableHeader>Status</TableHeader>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {quantificationResults.map((result, index) => (
                                  <TableRow
                                    key={`${result.instrumentSampleId}-${index}`}
                                  >
                                    <TableCell>
                                      <div>
                                        <strong>{result.sampleName}</strong>
                                        <div
                                          style={{
                                            fontSize: "0.75rem",
                                            color: "#525252",
                                          }}
                                        >
                                          {result.accessionNumber}
                                        </div>
                                      </div>
                                    </TableCell>
                                    <TableCell>
                                      {result.instrumentSampleId}
                                    </TableCell>
                                    <TableCell>
                                      <strong>{result.concentration}</strong>
                                    </TableCell>
                                    <TableCell>{result.units}</TableCell>
                                    <TableCell>{result.peakArea}</TableCell>
                                    <TableCell>
                                      <span
                                        style={{
                                          color:
                                            result.cvPercent <= 2
                                              ? "#24a148"
                                              : "#f1c21b",
                                        }}
                                      >
                                        {result.cvPercent}%
                                      </span>
                                    </TableCell>
                                    <TableCell>
                                      <span
                                        style={{
                                          color:
                                            result.accuracyPercent >= 95
                                              ? "#24a148"
                                              : "#f1c21b",
                                        }}
                                      >
                                        {result.accuracyPercent}%
                                      </span>
                                    </TableCell>
                                    <TableCell>
                                      <Tag
                                        type={
                                          result.status === "VALID"
                                            ? "green"
                                            : "red"
                                        }
                                        size="sm"
                                      >
                                        {result.status}
                                      </Tag>
                                    </TableCell>
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          </div>
                        </div>
                      )}

                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <Checkbox
                          id="qc-approved"
                          labelText="All QC criteria met and accepted *"
                          checked={qcApproved}
                          onChange={(checked) => setQcApproved(checked)}
                        />
                      </div>
                    </>
                  ) : (
                    <div
                      style={{
                        marginTop: "2rem",
                        padding: "2rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                        textAlign: "center",
                      }}
                    >
                      <p>No QC results available. Load results from above.</p>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* TAB 3: Deviations & Completion */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <h4>Document Deviations & Complete Execution</h4>

                  {/* Deviations List */}
                  {deviations.length > 0 && (
                    <div
                      style={{
                        marginTop: "1.5rem",
                        overflowX: "auto",
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                      }}
                    >
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>Type</TableHeader>
                            <TableHeader>Description</TableHeader>
                            <TableHeader>Corrective Action</TableHeader>
                            <TableHeader>Recorded By</TableHeader>
                            <TableHeader>Date/Time</TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {deviations.map((dev) => (
                            <TableRow key={dev.id}>
                              <TableCell>
                                <strong>{dev.type}</strong>
                              </TableCell>
                              <TableCell>{dev.description}</TableCell>
                              <TableCell>{dev.correctiveAction}</TableCell>
                              <TableCell>{dev.recordedBy}</TableCell>
                              <TableCell>
                                {new Date(dev.recordedAt).toLocaleString()}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  )}

                  <div style={{ marginTop: "1.5rem" }}>
                    <Button
                      kind="secondary"
                      onClick={() => setIsDeviationModalOpen(true)}
                    >
                      Add Deviation
                    </Button>
                  </div>

                  {/* Completion Summary */}
                  <div
                    style={{
                      marginTop: "2rem",
                      padding: "1.5rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <h5>Execution Summary</h5>
                    <ul
                      style={{
                        listStyle: "none",
                        paddingLeft: "0",
                        marginTop: "1rem",
                      }}
                    >
                      <li>✓ Samples Executed: {selectedSampleIds.size}</li>
                      <li>
                        ✓ Instrument: {selectedInstrument ? "Selected" : "N/A"}
                      </li>
                      <li>
                        ✓ QC Verification: {qcApproved ? "Approved" : "Pending"}
                      </li>
                      <li>✓ Deviations Recorded: {deviations.length}</li>
                      <li>
                        ✓ Analyst: {executionData.analystId || "Not recorded"}
                      </li>
                    </ul>
                  </div>

                  {/* Complete Button */}
                  <div style={{ marginTop: "2rem" }}>
                    <Button
                      kind="primary"
                      onClick={handleCompleteExecution}
                      disabled={
                        isExecuting ||
                        selectedSampleIds.size === 0 ||
                        !qcApproved
                      }
                    >
                      {isExecuting
                        ? "Completing..."
                        : "Complete Test Execution"}
                    </Button>
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Deviation Modal */}
      <Modal
        modalHeading="Record Deviation"
        primaryButtonText="Save"
        secondaryButtonText="Cancel"
        open={isDeviationModalOpen}
        onRequestClose={() => setIsDeviationModalOpen(false)}
        onRequestSubmit={handleAddDeviation}
      >
        <div style={{ paddingBottom: "1.5rem" }}>
          <Select
            id="deviation-type"
            labelText="Deviation Type *"
            value={deviationForm.type}
            onChange={(e) =>
              setDeviationForm({ ...deviationForm, type: e.target.value })
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
              setDeviationForm({
                ...deviationForm,
                description: e.target.value,
              })
            }
            style={{ marginTop: "1rem" }}
          />

          <TextArea
            id="corrective-action"
            labelText="Corrective Action"
            value={deviationForm.correctiveAction}
            onChange={(e) =>
              setDeviationForm({
                ...deviationForm,
                correctiveAction: e.target.value,
              })
            }
            style={{ marginTop: "1rem" }}
          />
        </div>
      </Modal>

      {/* Execution Details Modal */}
      <Modal
        modalHeading="Test Execution Workflow: Capture Raw Data & QC Results"
        primaryButtonText="Record Test Execution & Save Data"
        secondaryButtonText="Cancel"
        open={isExecutionModalOpen}
        onRequestClose={() => setIsExecutionModalOpen(false)}
        onRequestSubmit={async () => {
          await handleExecuteTest();
          setIsExecutionModalOpen(false);
        }}
        size="lg"
      >
        <div style={{ paddingBottom: "1.5rem" }}>
          <div
            style={{
              marginBottom: "1.5rem",
              padding: "1rem",
              backgroundColor: "#e3f2fd",
              borderRadius: "4px",
              border: "1px solid #1976d2",
            }}
          >
            <strong>📋 Workflow:</strong> The following steps will capture all
            test execution data and QC results for the selected samples. All
            data will be saved together.
          </div>
          {/* Step 2: Instrument Selection */}
          <div style={{ marginBottom: "2rem" }}>
            <h5>Step 2: Select Instrument Used for Testing</h5>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="modal-instrument-select"
                  labelText="Instrument *"
                  value={selectedInstrument}
                  onChange={(e) => setSelectedInstrument(e.target.value)}
                >
                  <SelectItem value="" text="-- Select instrument --" />
                  {instruments.map((inst) => (
                    <SelectItem
                      key={inst.id}
                      value={inst.id}
                      text={inst.machine}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>
          </div>

          {/* Step 3: Execution Parameters */}
          <div style={{ marginBottom: "2rem" }}>
            <h5>
              Step 3: Record Analyst & Execution Details (21 CFR Part 11
              Compliance)
            </h5>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="modal-analyst-id"
                  labelText="Analyst ID *"
                  value={executionData.analystId}
                  onChange={(e) =>
                    setExecutionData({
                      ...executionData,
                      analystId: e.target.value,
                    })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <DatePickerInput
                  id="modal-execution-date"
                  labelText="Execution Date *"
                  pattern="dd/mm/yyyy"
                  placeholder="dd/mm/yyyy"
                  value={executionData.executionDate}
                  onChange={(e) =>
                    setExecutionData({
                      ...executionData,
                      executionDate: e[0]
                        ? e[0].toISOString().split("T")[0]
                        : executionData.executionDate,
                    })
                  }
                />
              </Column>
            </Grid>
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="modal-test-notes"
                  labelText="Test Parameters / Notes"
                  value={executionData.notes}
                  onChange={(e) =>
                    setExecutionData({
                      ...executionData,
                      notes: e.target.value,
                    })
                  }
                  placeholder="e.g., Temperature: 25°C, Flow rate: 1.0 mL/min..."
                />
              </Column>
            </Grid>
          </div>

          {/* Step 4: File Upload & QC Processing - REQUIRED */}
          <div
            style={{
              marginBottom: "2rem",
              padding: "1rem",
              backgroundColor:
                uploadedFiles.length === 0 ? "#fff3e0" : "#f5f5f5",
              border:
                uploadedFiles.length === 0
                  ? "2px solid #ff9800"
                  : "1px solid #e0e0e0",
              borderRadius: "4px",
            }}
          >
            <h5 style={{ margin: "0 0 0.5rem 0" }}>
              Step 4: Upload & Process Raw Instrument Data{" "}
              <span style={{ color: "#d32f2f", fontWeight: "bold" }}>
                *REQUIRED
              </span>
            </h5>
            <div
              style={{
                fontSize: "0.875rem",
                color: uploadedFiles.length === 0 ? "#e65100" : "#525252",
                marginBottom: "1rem",
                margin: 0,
              }}
            >
              <p style={{ margin: "0 0 0.5rem 0" }}>
                Upload raw data files (chromatograms, spectra, or test results)
                from your instrument. Files will be automatically processed to
                extract QC results, calibration data, and quantification
                results.
              </p>
              {uploadedFiles.length === 0 && (
                <div style={{ marginTop: "0.5rem", fontWeight: "500" }}>
                  ⚠️ At least one file must be uploaded before recording test
                  execution.
                </div>
              )}
            </div>

            {/* File Selector */}
            <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}>
              <div>
                <input
                  type="file"
                  id="file-input"
                  style={{ display: "none" }}
                  accept={
                    selectedInstrument
                      ? instruments
                          .find((i) => i.id === selectedInstrument)
                          ?.formats.map((f) => `.${f.toLowerCase()}`)
                          .join(",") || ""
                      : ".csv,.pdf,.mzml,.cdf"
                  }
                  onChange={(e) => {
                    const file = e.target.files?.[0] || null;
                    handleFileSelect(file);
                  }}
                  disabled={!selectedInstrument}
                />
                <Button
                  kind="secondary"
                  onClick={() => document.getElementById("file-input").click()}
                  disabled={!selectedInstrument}
                  renderIcon={DocumentAdd}
                >
                  Select File
                </Button>
              </div>
              <div>
                <Button
                  kind="primary"
                  onClick={handleFileUpload}
                  disabled={!selectedFile || !selectedInstrument}
                  renderIcon={Upload}
                >
                  Upload File
                </Button>
              </div>
            </div>

            {/* Selected File Display */}
            {selectedFile && (
              <div
                style={{
                  marginBottom: "1rem",
                  padding: "0.75rem",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                  border: "1px solid #e0e0e0",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <strong>Selected File:</strong> {selectedFile.name} (
                    {(selectedFile.size / 1024 / 1024).toFixed(2)} MB)
                  </div>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={handleRemoveSelectedFile}
                  >
                    Remove
                  </Button>
                </div>
              </div>
            )}

            {/* Uploaded Files Display */}
            {uploadedFiles.length > 0 && (
              <div
                style={{
                  marginTop: "1rem",
                  padding: "0.75rem",
                  backgroundColor: "#e8f5e9",
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
                  <strong>Uploaded Files ({uploadedFiles.length}):</strong>
                  <Button
                    kind="tertiary"
                    size="sm"
                    onClick={() =>
                      handleProcessFiles(
                        uploadedFiles
                          .filter((f) => !f.processed)
                          .map((f) => f.id),
                      )
                    }
                    disabled={uploadedFiles.every((f) => f.processed)}
                  >
                    Process All Files
                  </Button>
                </div>
                <ul style={{ marginTop: "0.5rem", marginBottom: 0 }}>
                  {uploadedFiles.map((file) => (
                    <li key={file.id} style={{ marginBottom: "0.5rem" }}>
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                        }}
                      >
                        <div>
                          <strong>{file.name}</strong> - {file.instrumentName}
                          <div
                            style={{
                              fontSize: "0.875rem",
                              color: "#525252",
                              marginTop: "0.25rem",
                            }}
                          >
                            {file.processed ? (
                              <span style={{ color: "#24a148" }}>
                                ✅ Processed -{" "}
                                {file.processingResults?.analyzerResults
                                  ?.length || 0}{" "}
                                results extracted
                              </span>
                            ) : (
                              <span style={{ color: "#f1c21b" }}>
                                ⏳ Uploaded - Ready for processing
                              </span>
                            )}
                          </div>
                        </div>
                        {!file.processed && (
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => handleProcessFiles([file.id])}
                          >
                            Process
                          </Button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default BioanalyticalAnalyticalExecutionPage;
