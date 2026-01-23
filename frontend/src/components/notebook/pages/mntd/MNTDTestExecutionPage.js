import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextInput,
  TextArea,
  Dropdown,
  MultiSelect,
  DatePicker,
  DatePickerInput,
  NumberInput,
  Modal,
  Tag,
  RadioButtonGroup,
  RadioButton,
  FileUploader,
  Checkbox,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  InlineLoading,
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  WarningAltFilled,
  Renew,
  Upload,
  DocumentImport,
  Run,
  Report,
  DataBase,
  Edit,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDTestExecutionPage - Page 8 of the MNTD workflow.
 * Handles test execution, raw data capture, and post-test QC.
 *
 * Purpose: Collect machine outputs and validate results.
 *
 * Who uses it:
 * - Technician
 * - Data manager
 *
 * Data Points:
 * - Execution Confirmation: Run completed (Yes/No), Run issues (free text)
 * - Raw Data: File upload (if not auto-integrated), Machine-generated data, Metadata (run ID, kit lot, operator)
 * - Post-Test QC: QC pass/fail, Repeat test option
 *
 * System Actions:
 * - Raw data stored securely
 * - Sample status updated
 *
 * Leads to: Data Analysis Page
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDTestExecutionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Reagents from inventory (for kit lot number selection)
  const [reagents, setReagents] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);

  // Execution confirmation modal state
  const [showExecutionModal, setShowExecutionModal] = useState(false);
  const [executionData, setExecutionData] = useState({
    runCompleted: "YES",
    runIssues: "",
    runId: "",
    selectedKits: [], // Array of selected kit IDs for multiselect
    operator: "",
    executionDate: new Date().toISOString().split("T")[0],
    executionTime: "",
    notes: "",
  });

  // Raw data upload modal state
  const [showDataUploadModal, setShowDataUploadModal] = useState(false);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [uploadMetadata, setUploadMetadata] = useState({
    machineType: "",
    runId: "",
    fileType: "",
    notes: "",
  });
  const [isUploading, setIsUploading] = useState(false);

  // Bulk value entry modal state
  const [showBulkValueModal, setShowBulkValueModal] = useState(false);
  const [bulkValueData, setBulkValueData] = useState({
    runId: "",
    kitLot: "",
    operator: "",
    machineType: "",
    testResult: "",
    ctValue: "",
    concentration: "",
    absorbance: "",
    applyToAll: true,
  });

  // Post-test QC modal state
  const [showPostTestQCModal, setShowPostTestQCModal] = useState(false);
  const [postTestQCData, setPostTestQCData] = useState({
    qcResult: "PASS",
    repeatTest: false,
    repeatReason: "",
    qcNotes: "",
    qcTechnician: "",
    qcDate: new Date().toISOString().split("T")[0],
  });

  // Machine type options
  const machineTypeOptions = [
    { id: "PCR", text: "PCR Machine" },
    { id: "ELISA_READER", text: "ELISA Reader" },
    { id: "SPECTROPHOTOMETER", text: "Spectrophotometer" },
    { id: "MICROSCOPE", text: "Digital Microscope" },
    { id: "SEQUENCER", text: "Sequencer" },
    { id: "FLOW_CYTOMETER", text: "Flow Cytometer" },
    { id: "HEMATOLOGY_ANALYZER", text: "Hematology Analyzer" },
    { id: "CHEMISTRY_ANALYZER", text: "Chemistry Analyzer" },
    { id: "OTHER", text: "Other" },
  ];

  // File type options
  const fileTypeOptions = [
    { id: "CSV", text: "CSV" },
    { id: "XLSX", text: "Excel (XLSX)" },
    { id: "XML", text: "XML" },
    { id: "JSON", text: "JSON" },
    { id: "TXT", text: "Plain Text" },
    { id: "PDF", text: "PDF Report" },
    { id: "OTHER", text: "Other" },
  ];

  // Load samples for this page
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

    // Skip loading for synthetic page IDs
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
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              // Execution data
              runCompleted: sample.data?.runCompleted,
              runId: sample.data?.runId,
              kitLot: sample.data?.kitLot,
              operator: sample.data?.operator,
              executionDate: sample.data?.executionDate,
              runIssues: sample.data?.runIssues,
              // Raw data info
              rawDataFile: sample.data?.rawDataFile,
              machineType: sample.data?.machineType,
              // Test results
              testResult: sample.data?.testResult,
              ctValue: sample.data?.ctValue,
              concentration: sample.data?.concentration,
              absorbance: sample.data?.absorbance,
              // Post-test QC
              postTestQCResult: sample.data?.postTestQCResult,
              repeatTest: sample.data?.repeatTest,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Load reagents from inventory (used for kit lot number selection)
  const loadReagents = useCallback(() => {
    setLoadingReagents(true);
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setReagents(
              response.map((r) => ({
                id: r.id,
                label: `${r.name} (Lot: ${r.lotNumber || "N/A"})`,
                name: r.name,
                lotNumber: r.lotNumber,
                ...r,
              })),
            );
          } else {
            setReagents([]);
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const executed = samples.filter((s) => s.runCompleted === "YES").length;
    const withIssues = samples.filter(
      (s) => s.runCompleted === "YES" && s.runIssues,
    ).length;
    const qcPassed = samples.filter(
      (s) => s.postTestQCResult === "PASS",
    ).length;
    const qcFailed = samples.filter(
      (s) => s.postTestQCResult === "FAIL",
    ).length;
    const pending = samples.filter((s) => !s.runCompleted).length;
    return {
      total: samples.length,
      executed,
      withIssues,
      qcPassed,
      qcFailed,
      pending,
    };
  }, [samples]);

  // Handle opening execution modal
  const handleOpenExecutionModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testexecution.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    // Reset execution data
    setExecutionData({
      runCompleted: "YES",
      runIssues: "",
      runId: "",
      selectedKits: [],
      operator: "",
      executionDate: new Date().toISOString().split("T")[0],
      executionTime: "",
      notes: "",
    });
    // Load reagents from inventory
    loadReagents();
    setShowExecutionModal(true);
  }, [selectedIds, intl, loadReagents]);

  // Handle saving execution confirmation data
  const handleSaveExecutionData = useCallback(() => {
    if (!hasRealPageId) {
      setShowExecutionModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Get selected kit objects from reagents list based on selected IDs
    const selectedKitObjects = reagents.filter((r) =>
      executionData.selectedKits.includes(r.id),
    );

    // Build kit lot numbers string from selected kits
    const kitLotNumbers = selectedKitObjects
      .map((kit) => kit.lotNumber)
      .filter(Boolean)
      .join(", ");

    // Build selectedReagents array for inventory consumption (using itemId)
    const selectedReagents = selectedKitObjects
      .map((kit) => kit.itemId)
      .filter(Boolean);

    const dataToSave = {
      runCompleted: executionData.runCompleted,
      runIssues: executionData.runIssues,
      runId: executionData.runId,
      kitLot: kitLotNumbers,
      selectedKitIds: executionData.selectedKits,
      selectedReagents: selectedReagents,
      operator: executionData.operator,
      executionDate: executionData.executionDate,
      executionTime: executionData.executionTime,
      executionNotes: executionData.notes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status to IN_PROGRESS
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: "IN_PROGRESS",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.mntd.testexecution.executionSaved",
                      defaultMessage:
                        "Execution data saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowExecutionModal(false);
                setSelectedIds([]);
                // Reset form
                setExecutionData({
                  runCompleted: "YES",
                  runIssues: "",
                  runId: "",
                  selectedKits: [],
                  operator: "",
                  executionDate: new Date().toISOString().split("T")[0],
                  executionTime: "",
                  notes: "",
                });
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save execution data.");
          }
        }
      },
    );
  }, [
    executionData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
    reagents,
  ]);

  // Handle file upload
  const handleFileUpload = useCallback(async (event) => {
    const file = event?.target?.files?.[0];
    if (!file) return;

    setUploadedFile(file);
  }, []);

  // Handle save upload with file
  const handleSaveDataUpload = useCallback(async () => {
    if (!uploadedFile) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testexecution.fileRequired",
          defaultMessage: "Please select a file to upload.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowDataUploadModal(false);
      return;
    }

    setIsUploading(true);

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Create form data for file upload
    const formData = new FormData();
    formData.append("file", uploadedFile);
    formData.append("sampleIds", JSON.stringify(numericIds));
    formData.append("machineType", uploadMetadata.machineType);
    formData.append("runId", uploadMetadata.runId);
    formData.append("fileType", uploadMetadata.fileType);
    formData.append("notes", uploadMetadata.notes);

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/upload-raw-data`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: formData,
        },
      );

      const result = await response.json();

      if (response.ok && result.success) {
        setSuccess(
          intl.formatMessage(
            {
              id: "notebook.mntd.testexecution.uploadSuccess",
              defaultMessage:
                "Raw data uploaded successfully for {count} samples.",
            },
            { count: selectedIds.length },
          ),
        );
        setShowDataUploadModal(false);
        setSelectedIds([]);
        setUploadedFile(null);
        setUploadMetadata({
          machineType: "",
          runId: "",
          fileType: "",
          notes: "",
        });
        loadPageSamples();
        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        setError(result.error || "Failed to upload raw data.");
      }
    } catch (err) {
      console.error("Upload error:", err);
      setError("Network error during upload.");
    } finally {
      setIsUploading(false);
    }
  }, [
    uploadedFile,
    uploadMetadata,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle bulk value application
  const handleApplyBulkValue = useCallback(() => {
    // Build data object from all non-empty fields
    const dataToSave = {};
    if (bulkValueData.runId) dataToSave.runId = bulkValueData.runId;
    if (bulkValueData.kitLot) dataToSave.kitLot = bulkValueData.kitLot;
    if (bulkValueData.operator) dataToSave.operator = bulkValueData.operator;
    if (bulkValueData.machineType)
      dataToSave.machineType = bulkValueData.machineType;
    if (bulkValueData.testResult)
      dataToSave.testResult = bulkValueData.testResult;
    if (bulkValueData.ctValue) dataToSave.ctValue = bulkValueData.ctValue;
    if (bulkValueData.concentration)
      dataToSave.concentration = bulkValueData.concentration;
    if (bulkValueData.absorbance)
      dataToSave.absorbance = bulkValueData.absorbance;

    // Check if at least one field has a value
    if (Object.keys(dataToSave).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testexecution.bulkValueRequired",
          defaultMessage: "Please enter at least one value to apply.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowBulkValueModal(false);
      return;
    }

    const targetIds = bulkValueData.applyToAll
      ? samples.map((s) => parseInt(s.id, 10))
      : selectedIds.map((id) => parseInt(id, 10));

    if (targetIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testexecution.noSamplesSelected",
          defaultMessage: "No samples to apply value to.",
        }),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: targetIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.mntd.testexecution.bulkValueApplied",
                  defaultMessage: "Bulk values applied to {count} samples.",
                },
                { count: targetIds.length },
              ),
            );
            setShowBulkValueModal(false);
            setBulkValueData({
              runId: "",
              kitLot: "",
              operator: "",
              machineType: "",
              testResult: "",
              ctValue: "",
              concentration: "",
              absorbance: "",
              applyToAll: true,
            });
            loadPageSamples();
          } else {
            setError(response?.error || "Failed to apply bulk values.");
          }
        }
      },
    );
  }, [
    bulkValueData,
    samples,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    intl,
  ]);

  // Handle opening post-test QC modal
  const handleOpenPostTestQCModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testexecution.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setShowPostTestQCModal(true);
  }, [selectedIds, intl]);

  // Handle saving post-test QC data
  const handleSavePostTestQCData = useCallback(() => {
    if (!postTestQCData.qcResult) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testexecution.qcResultRequired",
          defaultMessage: "QC result is required.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowPostTestQCModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      postTestQCResult: postTestQCData.qcResult,
      repeatTest: postTestQCData.repeatTest,
      repeatReason: postTestQCData.repeatTest
        ? postTestQCData.repeatReason
        : null,
      postTestQCNotes: postTestQCData.qcNotes,
      postTestQCTechnician: postTestQCData.qcTechnician,
      postTestQCDate: postTestQCData.qcDate,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status based on QC result
            // PASS without repeat -> COMPLETED, otherwise stay PENDING
            const newStatus =
              postTestQCData.qcResult === "PASS" && !postTestQCData.repeatTest
                ? "COMPLETED"
                : "PENDING";
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: newStatus,
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.mntd.testexecution.postTestQCSaved",
                      defaultMessage:
                        "Post-test QC data saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowPostTestQCModal(false);
                setSelectedIds([]);
                // Reset form
                setPostTestQCData({
                  qcResult: "PASS",
                  repeatTest: false,
                  repeatReason: "",
                  qcNotes: "",
                  qcTechnician: "",
                  qcDate: new Date().toISOString().split("T")[0],
                });
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save post-test QC data.");
          }
        }
      },
    );
  }, [
    postTestQCData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "notebook.mntd.testexecution.pageNotInitialized",
            defaultMessage:
              "Cannot update status: Page not properly initialized.",
          }),
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate, intl],
  );

  // Render execution info column
  const renderExecutionInfo = (sample) => {
    if (!sample) {
      return null;
    }
    if (sample.runCompleted) {
      const isCompleted = sample.runCompleted === "YES";
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type={isCompleted ? "green" : "red"} size="sm">
            {isCompleted ? (
              <>
                <CheckmarkFilled size={12} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.testexecution.runCompleted"
                  defaultMessage="Run Completed"
                />
              </>
            ) : (
              <>
                <WarningAltFilled size={12} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.testexecution.runNotCompleted"
                  defaultMessage="Run Not Completed"
                />
              </>
            )}
          </Tag>
          {sample.runId && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              Run: {sample.runId}
            </div>
          )}
          {sample.operator && (
            <div style={{ color: "#525252" }}>by {sample.operator}</div>
          )}
          {sample.runIssues && (
            <div
              style={{ marginTop: "2px", color: "#da1e28", fontSize: "11px" }}
            >
              Issues: {sample.runIssues.substring(0, 30)}...
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.testexecution.notExecuted"
          defaultMessage="Not executed"
        />
      </span>
    );
  };

  // Render raw data column
  const renderRawData = (sample) => {
    if (!sample) {
      return null;
    }
    if (sample.rawDataFile || sample.testResult) {
      return (
        <div style={{ fontSize: "12px" }}>
          {sample.rawDataFile && (
            <Tag type="blue" size="sm">
              <DataBase size={12} style={{ marginRight: "4px" }} />
              {sample.rawDataFile.substring(0, 15)}...
            </Tag>
          )}
          {sample.testResult && (
            <div style={{ marginTop: "2px" }}>
              Result: <strong>{sample.testResult}</strong>
            </div>
          )}
          {sample.ctValue && (
            <div style={{ color: "#525252" }}>Ct: {sample.ctValue}</div>
          )}
          {sample.concentration && (
            <div style={{ color: "#525252" }}>Conc: {sample.concentration}</div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.testexecution.noData"
          defaultMessage="No data"
        />
      </span>
    );
  };

  // Render post-test QC column
  const renderPostTestQC = (sample) => {
    if (!sample) {
      return null;
    }
    if (sample.postTestQCResult) {
      const isPassed = sample.postTestQCResult === "PASS";
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type={isPassed ? "green" : "red"} size="sm">
            {isPassed ? (
              <>
                <CheckmarkFilled size={12} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.testexecution.qcPassed"
                  defaultMessage="QC Passed"
                />
              </>
            ) : (
              <>
                <WarningAltFilled size={12} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.testexecution.qcFailed"
                  defaultMessage="QC Failed"
                />
              </>
            )}
          </Tag>
          {sample.repeatTest && (
            <div style={{ marginTop: "2px" }}>
              <Tag type="purple" size="sm">
                <FormattedMessage
                  id="notebook.mntd.testexecution.repeatRequired"
                  defaultMessage="Repeat Required"
                />
              </Tag>
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.testexecution.pendingQC"
          defaultMessage="Pending QC"
        />
      </span>
    );
  };

  return (
    <div className="mntd-test-execution-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.testexecution.title"
            defaultMessage="Test Execution & Raw Data Capture"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.testexecution.description"
            defaultMessage="Collect machine outputs, validate results, and perform post-test quality control. Upload raw data files or enter results manually."
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
                  id="notebook.mntd.testexecution.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testexecution.executed"
                  defaultMessage="Executed"
                />
              </span>
              <span className="progress-value">{stats.executed}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testexecution.withIssues"
                  defaultMessage="With Issues"
                />
              </span>
              <span className="progress-value">{stats.withIssues}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testexecution.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{stats.qcPassed}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testexecution.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{stats.pending}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Run}
          onClick={handleOpenExecutionModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.testexecution.recordExecution"
            defaultMessage="Record Execution ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Upload}
          onClick={() => setShowDataUploadModal(true)}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.testexecution.uploadData"
            defaultMessage="Upload Raw Data"
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Edit}
          onClick={() => setShowBulkValueModal(true)}
        >
          <FormattedMessage
            id="notebook.mntd.testexecution.bulkValue"
            defaultMessage="Bulk Value Entry"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Report}
          onClick={handleOpenPostTestQCModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.testexecution.postTestQC"
            defaultMessage="Post-Test QC ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.mntd.testexecution.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="mntd-test-execution"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={[
            {
              key: "executionInfo",
              header: intl.formatMessage({
                id: "notebook.mntd.testexecution.executionInfo",
                defaultMessage: "Execution Info",
              }),
              render: renderExecutionInfo,
            },
            {
              key: "rawData",
              header: intl.formatMessage({
                id: "notebook.mntd.testexecution.rawData",
                defaultMessage: "Raw Data / Results",
              }),
              render: renderRawData,
            },
            {
              key: "postTestQC",
              header: intl.formatMessage({
                id: "notebook.mntd.testexecution.postTestQC",
                defaultMessage: "Post-Test QC",
              }),
              render: renderPostTestQC,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.mntd.testexecution.empty"
              defaultMessage="No samples available for test execution. Please complete the test assignment step first."
            />
          </p>
        </div>
      )}

      {/* Record Execution Modal */}
      <Modal
        open={showExecutionModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testexecution.modal.executionTitle",
          defaultMessage: "Record Test Execution",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.testexecution.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowExecutionModal(false)}
        onRequestSubmit={handleSaveExecutionData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testexecution.modal.executionDescription"
              defaultMessage="Record test execution confirmation for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Run Completed */}
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "notebook.mntd.testexecution.runCompleted",
              defaultMessage: "Run Completed?",
            })}
            name="run-completed"
            valueSelected={executionData.runCompleted}
            onChange={(value) =>
              setExecutionData({ ...executionData, runCompleted: value })
            }
            style={{ marginBottom: "1rem" }}
          >
            <RadioButton
              id="run-yes"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.yes",
                defaultMessage: "Yes",
              })}
              value="YES"
            />
            <RadioButton
              id="run-no"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.no",
                defaultMessage: "No",
              })}
              value="NO"
            />
          </RadioButtonGroup>

          {/* Run Issues (shown when run is completed) */}
          {executionData.runCompleted === "YES" && (
            <TextArea
              id="run-issues"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.runIssues",
                defaultMessage: "Run Issues (if any)",
              })}
              value={executionData.runIssues}
              onChange={(e) =>
                setExecutionData({
                  ...executionData,
                  runIssues: e.target.value,
                })
              }
              rows={2}
              placeholder="Describe any issues encountered during the run..."
              style={{ marginBottom: "1rem" }}
            />
          )}

          {/* Metadata Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.mntd.testexecution.metadata"
                defaultMessage="Run Metadata"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="run-id"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.testexecution.runId",
                    defaultMessage: "Run ID",
                  })}
                  value={executionData.runId}
                  onChange={(e) =>
                    setExecutionData({
                      ...executionData,
                      runId: e.target.value,
                    })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                {loadingReagents ? (
                  <InlineLoading
                    description={intl.formatMessage({
                      id: "notebook.mntd.testexecution.loadingReagents",
                      defaultMessage: "Loading reagents...",
                    })}
                  />
                ) : (
                  <MultiSelect
                    id="kit-lot"
                    titleText={intl.formatMessage({
                      id: "notebook.mntd.testexecution.kitLot",
                      defaultMessage: "Kit Lot Number",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.mntd.testexecution.selectKits",
                      defaultMessage: "Select kits...",
                    })}
                    items={reagents}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItems={reagents.filter((r) =>
                      executionData.selectedKits.includes(r.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setExecutionData({
                        ...executionData,
                        selectedKits: selectedItems.map((item) => item.id),
                      })
                    }
                    disabled={loadingReagents}
                  />
                )}
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="operator"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.testexecution.operator",
                    defaultMessage: "Operator",
                  })}
                  value={executionData.operator}
                  onChange={(e) =>
                    setExecutionData({
                      ...executionData,
                      operator: e.target.value,
                    })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={([date]) =>
                    setExecutionData({
                      ...executionData,
                      executionDate: date?.toISOString().split("T")[0] || "",
                    })
                  }
                >
                  <DatePickerInput
                    id="execution-date"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.testexecution.executionDate",
                      defaultMessage: "Execution Date",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </Column>
            </Grid>
          </div>

          {/* Notes */}
          <TextArea
            id="execution-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testexecution.notes",
              defaultMessage: "Notes",
            })}
            value={executionData.notes}
            onChange={(e) =>
              setExecutionData({ ...executionData, notes: e.target.value })
            }
            rows={3}
          />
        </div>
      </Modal>

      {/* Upload Raw Data Modal */}
      <Modal
        open={showDataUploadModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testexecution.modal.uploadTitle",
          defaultMessage: "Upload Raw Data",
        })}
        primaryButtonText={
          isUploading
            ? intl.formatMessage({
                id: "notebook.mntd.testexecution.uploading",
                defaultMessage: "Uploading...",
              })
            : intl.formatMessage({
                id: "notebook.mntd.testexecution.modal.upload",
                defaultMessage: "Upload",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setShowDataUploadModal(false);
          setUploadedFile(null);
        }}
        onRequestSubmit={handleSaveDataUpload}
        primaryButtonDisabled={isUploading || !uploadedFile}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testexecution.modal.uploadDescription"
              defaultMessage="Upload machine-generated data file for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* File Upload */}
          <FileUploader
            labelTitle={intl.formatMessage({
              id: "notebook.mntd.testexecution.selectFile",
              defaultMessage: "Select file",
            })}
            labelDescription={intl.formatMessage({
              id: "notebook.mntd.testexecution.fileFormats",
              defaultMessage: "Supported formats: CSV, TXT",
            })}
            buttonLabel={intl.formatMessage({
              id: "notebook.mntd.testexecution.chooseFile",
              defaultMessage: "Choose file",
            })}
            filenameStatus="edit"
            accept={[".csv", ".txt"]}
            onChange={handleFileUpload}
            style={{ marginBottom: "0.5rem" }}
          />

          {/* Download Template Link */}
          <div style={{ marginBottom: "1rem" }}>
            <a
              href={`${config.serverBaseUrl}/rest/notebook/bulk/template/raw-data-upload`}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                color: "#0f62fe",
                textDecoration: "none",
                fontSize: "0.875rem",
              }}
            >
              <DocumentImport
                size={16}
                style={{ verticalAlign: "middle", marginRight: "0.25rem" }}
              />
              <FormattedMessage
                id="notebook.mntd.testexecution.downloadTemplate"
                defaultMessage="Download CSV template"
              />
            </a>
          </div>

          {/* Upload Metadata */}
          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="machine-type"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.machineType",
                  defaultMessage: "Machine Type",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.testexecution.selectMachine",
                  defaultMessage: "Select machine",
                })}
                items={machineTypeOptions}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={machineTypeOptions.find(
                  (m) => m.id === uploadMetadata.machineType,
                )}
                onChange={({ selectedItem }) =>
                  setUploadMetadata({
                    ...uploadMetadata,
                    machineType: selectedItem?.id || "",
                  })
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="file-type"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.fileType",
                  defaultMessage: "File Type",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.testexecution.selectFileType",
                  defaultMessage: "Select type",
                })}
                items={fileTypeOptions}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={fileTypeOptions.find(
                  (f) => f.id === uploadMetadata.fileType,
                )}
                onChange={({ selectedItem }) =>
                  setUploadMetadata({
                    ...uploadMetadata,
                    fileType: selectedItem?.id || "",
                  })
                }
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="upload-run-id"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.runId",
                  defaultMessage: "Run ID",
                })}
                value={uploadMetadata.runId}
                onChange={(e) =>
                  setUploadMetadata({
                    ...uploadMetadata,
                    runId: e.target.value,
                  })
                }
                style={{ marginTop: "1rem" }}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="upload-notes"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.uploadNotes",
                  defaultMessage: "Notes",
                })}
                value={uploadMetadata.notes}
                onChange={(e) =>
                  setUploadMetadata({
                    ...uploadMetadata,
                    notes: e.target.value,
                  })
                }
                rows={2}
                style={{ marginTop: "1rem" }}
              />
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* Bulk Value Entry Modal */}
      <Modal
        open={showBulkValueModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testexecution.modal.bulkValueTitle",
          defaultMessage: "Bulk Value Entry",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.testexecution.modal.apply",
          defaultMessage: "Apply",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowBulkValueModal(false)}
        onRequestSubmit={handleApplyBulkValue}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testexecution.modal.bulkValueDescription"
              defaultMessage="Enter values to apply to multiple samples at once. Only non-empty fields will be applied."
            />
          </p>

          {/* All fields displayed at once */}
          <Grid fullWidth>
            {/* Run Metadata Section */}
            <Column lg={16} md={8} sm={4}>
              <h5 style={{ marginBottom: "0.5rem", marginTop: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.mntd.testexecution.runMetadata"
                  defaultMessage="Run Metadata"
                />
              </h5>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="bulk-runId"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.runId",
                  defaultMessage: "Run ID",
                })}
                value={bulkValueData.runId}
                onChange={(e) =>
                  setBulkValueData({
                    ...bulkValueData,
                    runId: e.target.value,
                  })
                }
                style={{ marginBottom: "1rem" }}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="bulk-kitLot"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.kitLot",
                  defaultMessage: "Kit Lot Number",
                })}
                value={bulkValueData.kitLot}
                onChange={(e) =>
                  setBulkValueData({
                    ...bulkValueData,
                    kitLot: e.target.value,
                  })
                }
                style={{ marginBottom: "1rem" }}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="bulk-operator"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.operator",
                  defaultMessage: "Operator",
                })}
                value={bulkValueData.operator}
                onChange={(e) =>
                  setBulkValueData({
                    ...bulkValueData,
                    operator: e.target.value,
                  })
                }
                style={{ marginBottom: "1rem" }}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="bulk-machineType"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.machineType",
                  defaultMessage: "Machine Type",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.testexecution.selectMachine",
                  defaultMessage: "Select machine",
                })}
                items={machineTypeOptions}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={machineTypeOptions.find(
                  (m) => m.id === bulkValueData.machineType,
                )}
                onChange={({ selectedItem }) =>
                  setBulkValueData({
                    ...bulkValueData,
                    machineType: selectedItem?.id || "",
                  })
                }
                style={{ marginBottom: "1rem" }}
              />
            </Column>

            {/* Test Results Section */}
            <Column lg={16} md={8} sm={4}>
              <h5 style={{ marginBottom: "0.5rem", marginTop: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.mntd.testexecution.testResults"
                  defaultMessage="Test Results"
                />
              </h5>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="bulk-testResult"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.testResult",
                  defaultMessage: "Test Result",
                })}
                value={bulkValueData.testResult}
                onChange={(e) =>
                  setBulkValueData({
                    ...bulkValueData,
                    testResult: e.target.value,
                  })
                }
                placeholder="e.g., Positive, Negative, Indeterminate"
                style={{ marginBottom: "1rem" }}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="bulk-ctValue"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.ctValue",
                  defaultMessage: "Ct Value",
                })}
                value={bulkValueData.ctValue}
                onChange={(e) =>
                  setBulkValueData({
                    ...bulkValueData,
                    ctValue: e.target.value,
                  })
                }
                placeholder="e.g., 25.5"
                style={{ marginBottom: "1rem" }}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="bulk-concentration"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.concentration",
                  defaultMessage: "Concentration",
                })}
                value={bulkValueData.concentration}
                onChange={(e) =>
                  setBulkValueData({
                    ...bulkValueData,
                    concentration: e.target.value,
                  })
                }
                placeholder="e.g., 150 ng/mL"
                style={{ marginBottom: "1rem" }}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="bulk-absorbance"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testexecution.absorbance",
                  defaultMessage: "Absorbance (OD)",
                })}
                value={bulkValueData.absorbance}
                onChange={(e) =>
                  setBulkValueData({
                    ...bulkValueData,
                    absorbance: e.target.value,
                  })
                }
                placeholder="e.g., 0.450"
                style={{ marginBottom: "1rem" }}
              />
            </Column>
          </Grid>

          {/* Apply To Options */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <Checkbox
              id="apply-to-all"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.applyToAll",
                defaultMessage: "Apply to all samples on this page",
              })}
              checked={bulkValueData.applyToAll}
              onChange={(e, { checked }) =>
                setBulkValueData({ ...bulkValueData, applyToAll: checked })
              }
            />

            {!bulkValueData.applyToAll && (
              <p
                style={{
                  marginTop: "0.5rem",
                  fontSize: "12px",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="notebook.mntd.testexecution.applyToSelected"
                  defaultMessage="Will apply to {count} selected sample(s)"
                  values={{ count: selectedIds.length }}
                />
              </p>
            )}
          </div>
        </div>
      </Modal>

      {/* Post-Test QC Modal */}
      <Modal
        open={showPostTestQCModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testexecution.modal.postTestQCTitle",
          defaultMessage: "Record Post-Test Quality Control",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.testexecution.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowPostTestQCModal(false)}
        onRequestSubmit={handleSavePostTestQCData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testexecution.modal.postTestQCDescription"
              defaultMessage="Record post-test QC results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* QC Result */}
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "notebook.mntd.testexecution.qcResultLabel",
              defaultMessage: "QC Result",
            })}
            name="post-test-qc-result"
            valueSelected={postTestQCData.qcResult}
            onChange={(value) =>
              setPostTestQCData({ ...postTestQCData, qcResult: value })
            }
            style={{ marginBottom: "1rem" }}
          >
            <RadioButton
              id="postqc-pass"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.pass",
                defaultMessage: "Pass",
              })}
              value="PASS"
            />
            <RadioButton
              id="postqc-fail"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.fail",
                defaultMessage: "Fail",
              })}
              value="FAIL"
            />
          </RadioButtonGroup>

          {/* Repeat Test Option */}
          <Checkbox
            id="repeat-test"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testexecution.repeatTest",
              defaultMessage: "Repeat test required",
            })}
            checked={postTestQCData.repeatTest}
            onChange={(e, { checked }) =>
              setPostTestQCData({ ...postTestQCData, repeatTest: checked })
            }
            style={{ marginBottom: "1rem" }}
          />

          {postTestQCData.repeatTest && (
            <TextArea
              id="repeat-reason"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.repeatReason",
                defaultMessage: "Reason for Repeat",
              })}
              value={postTestQCData.repeatReason}
              onChange={(e) =>
                setPostTestQCData({
                  ...postTestQCData,
                  repeatReason: e.target.value,
                })
              }
              rows={2}
              style={{ marginBottom: "1rem" }}
            />
          )}

          {/* QC Technician */}
          <TextInput
            id="postqc-technician"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testexecution.qcTechnician",
              defaultMessage: "QC Technician",
            })}
            value={postTestQCData.qcTechnician}
            onChange={(e) =>
              setPostTestQCData({
                ...postTestQCData,
                qcTechnician: e.target.value,
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* QC Date */}
          <DatePicker
            datePickerType="single"
            onChange={([date]) =>
              setPostTestQCData({
                ...postTestQCData,
                qcDate: date?.toISOString().split("T")[0] || "",
              })
            }
          >
            <DatePickerInput
              id="postqc-date"
              labelText={intl.formatMessage({
                id: "notebook.mntd.testexecution.qcDate",
                defaultMessage: "QC Date",
              })}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>

          {/* QC Notes */}
          <TextArea
            id="postqc-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testexecution.qcNotes",
              defaultMessage: "QC Notes",
            })}
            value={postTestQCData.qcNotes}
            onChange={(e) =>
              setPostTestQCData({ ...postTestQCData, qcNotes: e.target.value })
            }
            rows={3}
            style={{ marginTop: "1rem" }}
          />
        </div>
      </Modal>
    </div>
  );
}

export default MNTDTestExecutionPage;
