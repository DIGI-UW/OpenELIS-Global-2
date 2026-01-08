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
} from "@carbon/react";
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

  // Tab 2: QC Results
  const [qcResults, setQcResults] = useState([]);
  const [qcApproved, setQcApproved] = useState(false);

  // Tab 3: Deviations
  const [deviations, setDeviations] = useState([]);
  const [deviationForm, setDeviationForm] = useState({
    type: "",
    description: "",
    correctiveAction: "",
  });
  const [isDeviationModalOpen, setIsDeviationModalOpen] = useState(false);

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
    const availableInstruments = (templateInstruments &&
      templateInstruments.length > 0
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
        // Fetch samples for this Stage 3 page
        // The page will have NotebookPageSample records that reference samples from Stage 2
        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples`,
          { credentials: "include" }
        );

        if (!response.ok) {
          throw new Error("Failed to load samples for Stage 3");
        }

        const data = await response.json();
        const samples = Array.isArray(data) ? data : data.samples || [];

        const processedSamples = samples.map((sample) => {
          const assignmentData = sample.data || {};

          // Find the analytical method from our methods list
          const analyticalMethod = ANALYTICAL_METHODS.find(
            (m) => m.id === assignmentData.analyticalMethod
          );
          const methodName = analyticalMethod?.name || assignmentData.analyticalMethod;

          // Find instrument name from available instruments
          const instrument = instruments.find(
            (i) => i.id === assignmentData.instrumentId
          );

          return {
            id: sample.id,
            sampleItemId: sample.sampleItemId,
            accessionNumber: `Sample-${sample.sampleItemId}`,
            sampleType: "-",
            requestedTests: "-",
            assignedMethod: methodName,
            assignedMethodId: assignmentData.analyticalMethod,
            methodDescription: analyticalMethod?.description,
            instrumentId: assignmentData.instrumentId,
            instrumentName: instrument?.machine || `Instrument ${assignmentData.instrumentId}`,
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

  // ============================================================================
  // HANDLERS
  // ============================================================================

  const notify = useCallback(
    (message, kind = NotificationKinds.success) => {
      addNotification({ message, kind });
    },
    [addNotification]
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

  const handleSelectAll = useCallback((isSelected) => {
    if (isSelected) {
      setSelectedSampleIds(
        new Set(assignedSamples.map((s) => s.id))
      );
    } else {
      setSelectedSampleIds(new Set());
    }
  }, [assignedSamples]);

  const handleFileUpload = useCallback(
    async (files) => {
      if (!files || files.length === 0) return;
      if (!selectedInstrument) {
        notify("Please select an instrument first", NotificationKinds.warning);
        return;
      }

      const file = files[0];
      const fileExtension = file.name.split(".").pop().toUpperCase();
      const instrument = instruments.find((i) => i.id === selectedInstrument);

      if (
        !instrument.formats.includes(fileExtension)
      ) {
        notify(
          `Invalid format. Expected: ${instrument.formats.join(", ")}`,
          NotificationKinds.error
        );
        return;
      }

      try {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("instrumentId", selectedInstrument);
        formData.append("pageId", pageData?.id);

        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/page/${pageData?.id}/files/upload`,
          {
            method: "POST",
            credentials: "include",
            body: formData,
          }
        );

        if (!response.ok) throw new Error("File upload failed");

        const result = await response.json();
        setUploadedFiles((prev) => [...prev, result]);
        notify("File uploaded successfully");
      } catch (error) {
        console.error("Upload error:", error);
        notify("File upload failed", NotificationKinds.error);
      }
    },
    [selectedInstrument, instruments, pageData?.id, notify]
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

    try {
      setIsExecuting(true);
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
            data: {
              executionStatus: "EXECUTED",
              executedAt: new Date().toISOString(),
              executedBy: executionData.analystId,
              instrumentId: selectedInstrument,
              testParameters: executionData.testParameters,
              notes: executionData.notes,
            },
          }),
        }
      );

      if (!response.ok) throw new Error("Test execution failed");

      notify("Test execution recorded successfully");
      setSelectedTab(1); // Move to QC verification
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
    notify,
  ]);

  const handleLoadQCResults = useCallback(async () => {
    try {
      setIsLoading(true);
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/page/${pageData?.id}/samples`,
        { credentials: "include" }
      );

      if (!response.ok) throw new Error("Failed to load QC results");

      const data = await response.json();
      const qcData = (data.samples || [])
        .filter((s) => selectedSampleIds.has(s.id))
        .flatMap((s) => s.qcResults || []);

      setQcResults(qcData);
    } catch (error) {
      console.error("Error loading QC results:", error);
      notify("Failed to load QC results", NotificationKinds.error);
    } finally {
      setIsLoading(false);
    }
  }, [pageData?.id, selectedSampleIds, notify]);

  const handleAddDeviation = useCallback(() => {
    if (
      !deviationForm.type ||
      !deviationForm.description
    ) {
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
    try {
      setIsExecuting(true);
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
            data: {
              executionStatus: "COMPLETED",
              completedAt: new Date().toISOString(),
              completedBy: executionData.analystId,
              qcApproved,
              deviationCount: deviations.length,
              deviations,
            },
          }),
        }
      );

      if (!response.ok) throw new Error("Completion failed");

      notify("Test execution completed successfully");
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    } catch (error) {
      console.error("Completion error:", error);
      notify("Completion failed", NotificationKinds.error);
    } finally {
      setIsExecuting(false);
    }
  }, [
    pageData?.id,
    selectedSampleIds,
    executionData.analystId,
    qcApproved,
    deviations,
    onProgressUpdate,
    notify,
  ]);

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
                    <h5>Step 1: Select Samples to Execute</h5>
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
                              <input
                                type="checkbox"
                                checked={
                                  selectedSampleIds.size > 0 &&
                                  selectedSampleIds.size ===
                                    assignedSamples.length
                                }
                                onChange={(e) => {
                                  handleSelectAll(e.target.checked);
                                }}
                                title="Select all samples"
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
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {assignedSamples.length > 0 ? (
                            assignedSamples.map((sample) => (
                              <TableRow key={sample.id}>
                                <TableCell>
                                  <input
                                    type="checkbox"
                                    checked={selectedSampleIds.has(sample.id)}
                                    onChange={(e) => {
                                      handleSampleSelection(
                                        sample.id,
                                        e.target.checked
                                      );
                                    }}
                                    title={`Select Sample ${sample.sampleItemId}`}
                                  />
                                </TableCell>
                                <TableCell>
                                  <strong>
                                    {sample.accessionNumber || sample.id}
                                  </strong>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      backgroundColor: "#0043ce",
                                      color: "white",
                                      padding: "0.25rem 0.75rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      fontWeight: 600,
                                      whiteSpace: "nowrap",
                                    }}
                                  >
                                    {sample.assignedMethod ||
                                      "Not assigned"}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      backgroundColor: "#e3f2fd",
                                      color: "#1565c0",
                                      padding: "0.25rem 0.75rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      fontWeight: 600,
                                      whiteSpace: "nowrap",
                                      border: "1px solid #bbdefb",
                                    }}
                                  >
                                    {sample.assignedStaff ||
                                      "-"}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      backgroundColor: "#f0f0f0",
                                      color: "#161616",
                                      padding: "0.25rem 0.75rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      fontWeight: 600,
                                      whiteSpace: "nowrap",
                                      border: "1px solid #d0d0d0",
                                    }}
                                  >
                                    {sample.instrumentName ||
                                      sample.instrumentId ||
                                      "-"}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      backgroundColor: "#e8f5e9",
                                      color: "#24a148",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      fontWeight: 600,
                                    }}
                                  >
                                    Ready
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))
                          ) : (
                            <TableRow>
                              <TableCell
                                colSpan="6"
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

                  {/* Instrument Selection */}
                  <div style={{ marginBottom: "2rem" }}>
                    <h5>Step 2: Select Instrument</h5>
                    <Grid>
                      <Column lg={6} md={4} sm={4}>
                        <Select
                          id="instrument-select"
                          labelText="Instrument *"
                          value={selectedInstrument}
                          onChange={(e) =>
                            setSelectedInstrument(e.target.value)
                          }
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

                  {/* Execution Parameters */}
                  <div style={{ marginBottom: "2rem" }}>
                    <h5>Step 3: Record Execution Details</h5>
                    <Grid>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="analyst-id"
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
                          id="execution-date"
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
                          id="test-notes"
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

                  {/* File Upload */}
                  <div style={{ marginBottom: "2rem" }}>
                    <h5>Step 4: Upload Raw Data Files</h5>
                    <Grid>
                      <Column lg={16} md={8} sm={4}>
                        <FileUploader
                          id="file-upload"
                          labelTitle="Upload raw instrument data"
                          buttonLabel="Add file"
                          accept={
                            selectedInstrument
                              ? instruments
                                  .find((i) => i.id === selectedInstrument)
                                  ?.formats.map((f) => `.${f.toLowerCase()}`)
                                  .join(",")
                              : ".csv,.pdf,.mzml,.cdf"
                          }
                          onChange={(e) => handleFileUpload(e.target.files)}
                          disabled={!selectedInstrument}
                        />
                      </Column>
                    </Grid>

                    {uploadedFiles.length > 0 && (
                      <div
                        style={{
                          marginTop: "1rem",
                          padding: "0.75rem",
                          backgroundColor: "#e8f5e9",
                          borderRadius: "4px",
                        }}
                      >
                        <strong>Uploaded Files ({uploadedFiles.length}):</strong>
                        <ul>
                          {uploadedFiles.map((file) => (
                            <li key={file.id}>{file.name}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>

                  {/* Execute Button */}
                  <div style={{ marginTop: "2rem" }}>
                    <Button
                      kind="primary"
                      onClick={handleExecuteTest}
                      disabled={
                        isExecuting ||
                        selectedSampleIds.size === 0 ||
                        !selectedInstrument ||
                        !executionData.analystId
                      }
                    >
                      {isExecuting ? "Recording..." : "Record Test Execution"}
                    </Button>
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

                  <div style={{ marginTop: "1.5rem" }}>
                    <Button
                      kind="secondary"
                      onClick={handleLoadQCResults}
                      disabled={isLoading || selectedSampleIds.size === 0}
                    >
                      Load QC Results
                    </Button>
                  </div>

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
                                <TableCell>
                                  {qc.measuredValue || "-"}
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      color:
                                        qc.accuracy !== null &&
                                        qc.acceptanceCriteria &&
                                        qc.accuracy >=
                                          qc.acceptanceCriteria.min &&
                                        qc.accuracy <=
                                          qc.acceptanceCriteria.max
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
                                      qc.precision <=
                                        (qc.precisionLimit || 20))
                                      ? "PASS"
                                      : "FAIL"}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>

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
                      <li>✓ QC Verification: {qcApproved ? "Approved" : "Pending"}</li>
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
                      {isExecuting ? "Completing..." : "Complete Test Execution"}
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
            <SelectItem value="Out of Specification" text="Out of Specification" />
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
    </div>
  );
}

export default BioanalyticalAnalyticalExecutionPage;
