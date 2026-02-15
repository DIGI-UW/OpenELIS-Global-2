import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Loading,
  Modal,
  Tag,
  Tile,
  Checkbox,
  NumberInput,
  MultiSelect,
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  Warning,
  Renew,
  InventoryManagement,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";
import {
  ESignatureModal,
  SignatureMeaning,
  useESign,
} from "../../../esignature";
import PermissionGate from "../../../security/PermissionGate";
import { Permissions } from "../../../../constants/roles";

/**
 * PharmaceuticalTestingPage - Page 4: Assay & Test Execution
 *
 * Allows technicians to:
 * - Perform pharmaceutical assays (TLC, UV-Vis, FTIR, HPLC, GC, LC-MS/MS)
 * - Execute physical tests (dissolution, friability, hardness)
 * - Record quality controls (positive/negative, internal standards)
 * - Log deviations with CAPA
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PharmaceuticalTestingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Instruments and reagents from inventory
  const [instruments, setInstruments] = useState([]);
  const [loadingInstruments, setLoadingInstruments] = useState(false);
  const [reagents, setReagents] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);

  // Test Execution modal state (Phase 1)
  const [showExecutionModal, setShowExecutionModal] = useState(false);
  const [testExecutionData, setTestExecutionData] = useState({
    assayCategory: "",
    testType: "",
    operator: "",
    performedDate: new Date().toISOString().split("T")[0],
    instrumentsUsed: [],
    reagentsUsed: [],
    // QC fields
    qcData: {
      positiveControlResult: "",
      negativeControlResult: "",
      internalStandardUsed: false,
      replicateCount: 2,
      acceptanceCriteria: "",
      rsdValue: "",
    },
    // Deviation fields
    hasDeviation: false,
    deviation: null,
  });

  // Result Entry modal state (Phase 2 - Universal Structure)
  const [showResultModal, setShowResultModal] = useState(false);
  const [testResultData, setTestResultData] = useState({
    value: "",
    unit: "",
    outcome: "", // PASS | FAIL | INCONCLUSIVE
    notes: "",
    recordedBy: "",
  });

  // Outcome options for result entry
  const outcomeOptions = [
    { id: "PASS", text: "Pass" },
    { id: "FAIL", text: "Fail" },
    { id: "INCONCLUSIVE", text: "Inconclusive" },
  ];

  // Common units for pharmaceutical testing results
  const unitOptions = [
    { id: "%", text: "% (Percentage)" },
    { id: "mg/mL", text: "mg/mL" },
    { id: "mg/g", text: "mg/g" },
    { id: "µg/mL", text: "µg/mL" },
    { id: "ng/mL", text: "ng/mL" },
    { id: "mg", text: "mg" },
    { id: "µg", text: "µg" },
    { id: "IU/mL", text: "IU/mL" },
    { id: "CFU/g", text: "CFU/g" },
    { id: "CFU/mL", text: "CFU/mL" },
    { id: "EU/mL", text: "EU/mL (Endotoxin)" },
    { id: "ppm", text: "ppm" },
    { id: "ppb", text: "ppb" },
    { id: "min", text: "min (Minutes)" },
    { id: "sec", text: "sec (Seconds)" },
    { id: "N", text: "N (Newtons)" },
    { id: "kP", text: "kP (Kiloponds)" },
    { id: "mPa·s", text: "mPa·s (Viscosity)" },
    { id: "pH", text: "pH" },
    { id: "mS/cm", text: "mS/cm (Conductivity)" },
  ];

  // Assay category and test types for pharmaceuticals
  const assayCategories = [
    { id: "identification", text: "Identification Tests" },
    { id: "potency", text: "Potency / Assay" },
    { id: "impurity", text: "Impurity Profiling" },
    { id: "physical", text: "Physical & Performance Tests" },
    { id: "stability", text: "Stability Testing" },
    { id: "biological", text: "Biological Assays" },
    { id: "microbiological", text: "Microbiological Tests" },
    { id: "herbal", text: "Herbal/Natural Product Assays" },
  ];

  const testTypesByCategory = {
    identification: [
      { id: "tlc", text: "TLC (Thin Layer Chromatography)" },
      { id: "uv_vis", text: "UV/Visible Spectroscopy" },
      { id: "ftir", text: "FTIR (Fourier-Transform Infrared)" },
    ],
    potency: [
      { id: "hplc", text: "HPLC (High-Performance Liquid Chromatography)" },
      { id: "titration", text: "Titration" },
      { id: "uv_vis_spectro", text: "UV/Visible Spectrophotometry" },
    ],
    impurity: [
      { id: "hplc_stability", text: "HPLC (Stability-Indicating)" },
      { id: "gc", text: "Gas Chromatography (GC)" },
      { id: "lc_msms", text: "LC-MS/MS" },
    ],
    physical: [
      { id: "dissolution", text: "Dissolution" },
      { id: "disintegration", text: "Disintegration Time" },
      { id: "friability", text: "Friability" },
      { id: "hardness", text: "Hardness" },
      { id: "viscosity", text: "Viscosity" },
    ],
    stability: [
      { id: "ich_accelerated", text: "ICH Accelerated (40C/75% RH)" },
      { id: "ich_intermediate", text: "ICH Intermediate (30C/65% RH)" },
      { id: "ich_longterm", text: "ICH Long-term (25C/60% RH)" },
    ],
    biological: [
      { id: "clinical_chemistry", text: "Clinical Chemistry" },
      { id: "elisa", text: "ELISA" },
      { id: "hplc_pk", text: "HPLC/LC-MS/MS (Pharmacokinetics)" },
      { id: "pcr", text: "PCR/qPCR" },
      { id: "genotyping", text: "Genotyping" },
    ],
    microbiological: [
      { id: "tamc", text: "TAMC (Total Aerobic Microbial Count)" },
      { id: "tymc", text: "TYMC (Total Yeast & Mold Count)" },
      { id: "sterility", text: "Sterility Test" },
      { id: "lal", text: "LAL Assay (Endotoxin)" },
      { id: "toc", text: "TOC (Total Organic Carbon)" },
      { id: "conductivity", text: "Conductivity" },
      { id: "mic", text: "MIC (Minimum Inhibitory Concentration)" },
      { id: "aet", text: "AET (Antimicrobial Effectiveness)" },
    ],
    herbal: [
      { id: "phytochemical", text: "Phytochemical Screening" },
      { id: "hplc_fingerprint", text: "HPLC Fingerprinting" },
      { id: "bioactivity", text: "Bioactivity Assays" },
      { id: "toxicity", text: "Toxicity Assays" },
    ],
  };

  // Get test types based on selected category
  const availableTestTypes =
    testTypesByCategory[testExecutionData.assayCategory] || [];

  // Check if all selected samples are awaiting results (for "Record Results" button)
  const allSelectedAwaitingResults = useCallback(() => {
    if (selectedIds.length === 0) return false;
    return selectedIds.every((id) => {
      const sample = samples.find((s) => s.id === id);
      return sample?.testStatus === "EXECUTED";
    });
  }, [selectedIds, samples]);

  // Check if all selected samples have results recorded (for "Mark Complete" button)
  const allSelectedHaveResults = useCallback(() => {
    if (selectedIds.length === 0) return false;
    return selectedIds.every((id) => {
      const sample = samples.find((s) => s.id === id);
      return sample?.testStatus === "RESULTS_RECORDED";
    });
  }, [selectedIds, samples]);

  // Load instruments from template or inventory
  const loadInstruments = useCallback(() => {
    // If template has configured instruments, use those exclusively
    if (templateInstruments && templateInstruments.length > 0) {
      setInstruments(
        templateInstruments.map((analyzer) => ({
          id: analyzer.id,
          label: analyzer.value,
          name: analyzer.value,
        })),
      );
      setLoadingInstruments(false);
      return;
    }

    // Fallback: load from inventory if no template instruments configured
    setLoadingInstruments(true);
    getFromOpenElisServer(
      "/rest/inventory/instruments?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setInstruments(
              response.map((i) => ({
                id: i.id,
                label: `${i.name} (${i.serialNumber || "N/A"})`,
                name: i.name,
                serialNumber: i.serialNumber,
                ...i,
              })),
            );
          }
          setLoadingInstruments(false);
        }
      },
    );
  }, [templateInstruments]);

  // Load reagents from inventory for consumption tracking
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
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  // Helper to get category display text from ID
  const getCategoryDisplayText = useCallback((categoryId) => {
    if (!categoryId) return "";
    const category = assayCategories.find((c) => c.id === categoryId);
    return category ? category.text : categoryId;
  }, []);

  // Load samples for this specific page
  const loadSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
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
              // Read from new structure (testExecution / testResult)
              const testExecution = sample.data?.testExecution;
              const testResult = sample.data?.testResult;
              const testStatus = sample.data?.testStatus || null;

              const categoryId = testExecution?.assayCategory || "";
              const testTypeValue = testExecution?.testType || "";

              return {
                id: String(sample.id || sample.sampleItemId),
                sampleItemId: sample.sampleItemId,
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                collectionDate: sample.collectionDate,
                status: sample.pageStatus || "PENDING",
                sampleCategory:
                  sample.data?.sampleCategory ||
                  sample.sampleCategory ||
                  sample.typeOfSample?.description ||
                  "",
                lotNumber:
                  sample.data?.lotNumber ||
                  sample.lotNumber ||
                  sample.batchNumber ||
                  "",
                // Test execution data (new structure)
                testExecution: testExecution || null,
                testResult: testResult || null,
                testStatus: testStatus,
                // Display fields
                assayCategory: categoryId,
                assayCategoryDisplay: getCategoryDisplayText(categoryId),
                assayTestType: testTypeValue,
                assayTestTypeDisplay: testTypeValue,
                assayOperator: testExecution?.operator || "",
                // Result display
                resultValue: testResult?.value || "",
                resultOutcome: testResult?.outcome || "",
                hasDeviation: testExecution?.hasDeviation || false,
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
  }, [pageData?.id, getCategoryDisplayText]);

  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();
    loadInstruments();
    loadReagents();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples, loadInstruments, loadReagents]);

  const handleStatusChange = useCallback(
    (sampleIds, newStatus) => {
      if (!pageData?.id || String(pageData.id).startsWith("default-")) {
        return;
      }

      const numericIds = sampleIds.map((id) => parseInt(id, 10));

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({ sampleIds: numericIds, status: newStatus }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.pharma.testing.statusUpdated",
                    defaultMessage: "Updated {count} samples to {status}",
                  },
                  { count: sampleIds.length, status: newStatus },
                ),
              );
              loadSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            } else {
              setError(response?.error || "Failed to update status");
            }
          }
        },
      );
    },
    [pageData?.id, intl, loadSamples, onProgressUpdate],
  );

  // Handler for opening Test Execution modal (Phase 1)
  const handleRecordExecution = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.testing.noSamplesSelected",
          defaultMessage: "Please select samples to record test execution",
        }),
      );
      return;
    }
    // Reset execution form
    setTestExecutionData({
      assayCategory: "",
      testType: "",
      operator: "",
      performedDate: new Date().toISOString().split("T")[0],
      instrumentsUsed: [],
      reagentsUsed: [],
      qcData: {
        positiveControlResult: "",
        negativeControlResult: "",
        internalStandardUsed: false,
        replicateCount: 2,
        acceptanceCriteria: "",
        rsdValue: "",
      },
      hasDeviation: false,
      deviation: null,
    });
    setShowExecutionModal(true);
  };

  // Handler for opening Result Entry modal (Phase 2)
  const handleRecordResults = () => {
    if (!allSelectedAwaitingResults()) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.testing.noExecutedSamples",
          defaultMessage:
            "All selected samples must have test execution recorded",
        }),
      );
      return;
    }
    // Reset result form
    setTestResultData({
      value: "",
      unit: "",
      outcome: "",
      notes: "",
      recordedBy: "",
    });
    setShowResultModal(true);
  };

  // Save Test Execution data (Phase 1)
  const handleSaveExecution = () => {
    if (!testExecutionData.assayCategory || !testExecutionData.testType) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.testing.categoryAndTestRequired",
          defaultMessage: "Assay category and test type are required",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowExecutionModal(false);
      return;
    }

    // Convert selected IDs to sampleItemIds for the backend API
    const sampleItemIds = selectedIds
      .map((id) => {
        const sample = samples.find((s) => s.id === id);
        return sample?.sampleItemId;
      })
      .filter((id) => id != null);

    // Build the test execution object with timestamp
    const executionPayload = {
      testExecution: {
        assayCategory: testExecutionData.assayCategory,
        testType: testExecutionData.testType,
        operator: testExecutionData.operator,
        performedDate: testExecutionData.performedDate,
        instrumentsUsed: testExecutionData.instrumentsUsed,
        reagentsUsed: testExecutionData.reagentsUsed,
        qcData: testExecutionData.qcData,
        hasDeviation: testExecutionData.hasDeviation,
        deviation: testExecutionData.hasDeviation
          ? {
              description: testExecutionData.deviation?.description || "",
              rootCause: testExecutionData.deviation?.rootCause || "",
              capaAction: testExecutionData.deviation?.capaAction || "",
            }
          : null,
        executedAt: new Date().toISOString(),
      },
      testStatus: "EXECUTED",
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleItemIds,
        data: executionPayload,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status to IN_PROGRESS
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: sampleItemIds,
                status: "IN_PROGRESS",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.pharma.testing.executionSaved",
                      defaultMessage:
                        "Test execution recorded for {count} samples",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowExecutionModal(false);
                setSelectedIds([]);
                loadSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save test execution");
          }
        }
      },
    );
  };

  // Check if result form is valid (no side effects - safe to call during render)
  const isResultFormValid = useCallback(() => {
    if (!testResultData.value || !testResultData.outcome) {
      return false;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return false;
    }

    // Check if there are executed samples
    const hasExecutedSamples = selectedIds.some((id) => {
      const sample = samples.find((s) => s.id === id);
      return sample?.testStatus === "EXECUTED";
    });

    return hasExecutedSamples;
  }, [testResultData, pageData?.id, selectedIds, samples]);

  // Get executed sample IDs for e-signature record linking
  const getExecutedSampleIds = useCallback(() => {
    return selectedIds
      .map((id) => {
        const sample = samples.find((s) => s.id === id);
        if (sample?.testStatus === "EXECUTED") {
          return sample?.sampleItemId;
        }
        return null;
      })
      .filter((id) => id != null);
  }, [selectedIds, samples]);

  // Get sample IDs with results recorded for validation e-signature
  const getResultsRecordedSampleIds = useCallback(() => {
    return selectedIds
      .map((id) => {
        const sample = samples.find((s) => s.id === id);
        if (sample?.testStatus === "RESULTS_RECORDED") {
          return sample?.sampleItemId;
        }
        return null;
      })
      .filter((id) => id != null);
  }, [selectedIds, samples]);

  // Save Test Results (Phase 2) - called after successful e-signature
  const handleSaveResults = useCallback(
    (signature) => {
      if (!pageData?.id || String(pageData.id).startsWith("default-")) {
        setShowResultModal(false);
        return;
      }

      const executedSampleItemIds = getExecutedSampleIds();

      if (executedSampleItemIds.length === 0) {
        setError(
          intl.formatMessage({
            id: "notebook.pharma.testing.noExecutedSamples",
            defaultMessage: "No executed samples selected for result entry",
          }),
        );
        return;
      }

      // Build the test result object with timestamp and e-signature reference
      const resultPayload = {
        testResult: {
          value: testResultData.value,
          unit: testResultData.unit,
          outcome: testResultData.outcome,
          notes: testResultData.notes,
          recordedBy: testResultData.recordedBy,
          recordedAt: new Date().toISOString(),
          // Include e-signature reference for 21 CFR Part 11 compliance
          electronicSignature: signature
            ? {
                signatureId: signature.signatureId,
                signerId: signature.signerId,
                signerName: signature.signerNamePrinted,
                signatureMeaning: signature.signatureMeaning,
                signedAt: signature.signedAt,
              }
            : null,
        },
        testStatus: "RESULTS_RECORDED",
      };

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: executedSampleItemIds,
          data: resultPayload,
        }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.pharma.testing.resultsSaved",
                    defaultMessage: "Test results recorded for {count} samples",
                  },
                  { count: executedSampleItemIds.length },
                ),
              );
              setShowResultModal(false);
              setSelectedIds([]);
              loadSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            } else {
              setError(response?.error || "Failed to save test results");
            }
          }
        },
      );
    },
    [
      pageData?.id,
      getExecutedSampleIds,
      testResultData,
      intl,
      loadSamples,
      onProgressUpdate,
    ],
  );

  // Handle e-signature success - validate first, then save
  const handleSignAndSaveResults = useCallback(
    (signature) => {
      // Signature can be null if e-signatures are disabled
      handleSaveResults(signature);
    },
    [handleSaveResults],
  );

  // Handle e-signature success for Mark Complete (validation action)
  const handleSignAndMarkComplete = useCallback(
    // eslint-disable-next-line no-unused-vars
    (signature) => {
      // Signature is already stored in the electronic_signature table via useESign hook
      // Just update the status using existing handler
      handleStatusChange(selectedIds, "COMPLETED");
      setSelectedIds([]);
    },
    [handleStatusChange, selectedIds],
  );

  // E-Signature hook for result entry (AUTHORED meaning)
  const { openSignatureModal, signatureModalProps, isCheckingEnabled } =
    useESign({
      meaning: SignatureMeaning.AUTHORED,
      context: intl.formatMessage(
        {
          id: "notebook.pharma.testing.esigContext",
          defaultMessage: "Sign {count} test result(s) as authored",
        },
        { count: getExecutedSampleIds().length },
      ),
      recordType: "NOTEBOOK_PAGE_SAMPLE",
      recordId: pageData?.id || 0,
      onSuccess: handleSignAndSaveResults,
      onCancel: () => setShowResultModal(true), // Reopen result modal on cancel
    });

  // E-Signature hook for validation/completion (VALIDATED_AND_RELEASED meaning)
  const {
    openSignatureModal: openValidationSignatureModal,
    signatureModalProps: validationSignatureModalProps,
  } = useESign({
    meaning: SignatureMeaning.VALIDATED_AND_RELEASED,
    context: intl.formatMessage(
      {
        id: "notebook.pharma.testing.validationEsigContext",
        defaultMessage:
          "Validate and release {count} test result(s) as complete",
      },
      { count: getResultsRecordedSampleIds().length },
    ),
    recordType: "NOTEBOOK_PAGE_SAMPLE",
    recordId: pageData?.id || 0,
    onSuccess: handleSignAndMarkComplete,
    onCancel: () => {}, // No modal to reopen
  });

  // Handle "Save Results" button click - close result modal, then open e-sig
  const handleSaveResultsClick = useCallback(() => {
    setShowResultModal(false);
    openSignatureModal();
  }, [openSignatureModal]);

  const handleMarkComplete = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.testing.noSamplesSelected",
          defaultMessage: "Please select samples to mark as complete",
        }),
      );
      return;
    }
    // Open validation signature modal - actual status change happens after successful e-signature
    openValidationSignatureModal();
  };

  // Calculate stats
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const deviationCount = samples.filter((s) => s.hasDeviation).length;

  // Render page status with color-coded tags
  const renderStatus = (status) => {
    const statusConfig = {
      COMPLETED: { type: "green", label: "Completed" },
      IN_PROGRESS: { type: "blue", label: "In Progress" },
      PENDING: { type: "gray", label: "Pending" },
      FAILED: { type: "red", label: "Failed" },
      ON_HOLD: { type: "purple", label: "On Hold" },
    };

    const config = statusConfig[status] || { type: "gray", label: status };

    return (
      <Tag type={config.type} size="sm">
        {config.label}
      </Tag>
    );
  };

  // Render test status (EXECUTED or RESULTS_RECORDED)
  const renderTestStatus = (sample) => {
    if (!sample.testStatus) {
      return (
        <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
          <FormattedMessage
            id="notebook.pharma.testing.notExecuted"
            defaultMessage="Not Executed"
          />
        </span>
      );
    }

    if (sample.testStatus === "EXECUTED") {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.pharma.testing.awaitingResults"
            defaultMessage="Awaiting Results"
          />
        </Tag>
      );
    }

    if (sample.testStatus === "RESULTS_RECORDED") {
      return (
        <Tag type="green" size="sm">
          <FormattedMessage
            id="notebook.pharma.testing.resultsRecorded"
            defaultMessage="Results Recorded"
          />
        </Tag>
      );
    }

    return <Tag size="sm">{sample.testStatus}</Tag>;
  };

  // Render result value with outcome indicator
  const renderResultValue = (sample) => {
    if (!sample.testResult) {
      return <span style={{ color: "#8d8d8d" }}>-</span>;
    }

    return (
      <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
        <span>
          {sample.resultValue}
          {sample.testResult?.unit ? ` ${sample.testResult.unit}` : ""}
        </span>
        {sample.resultOutcome && (
          <Tag
            type={
              sample.resultOutcome === "PASS"
                ? "green"
                : sample.resultOutcome === "FAIL"
                  ? "red"
                  : "gray"
            }
            size="sm"
          >
            {sample.resultOutcome}
          </Tag>
        )}
      </div>
    );
  };

  // Render test info column
  const renderTestInfo = (sample) => {
    if (sample.assayTestType) {
      return (
        <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
          <Tag type="blue" size="sm">
            {sample.assayTestType}
          </Tag>
          {sample.hasDeviation && (
            <Tag type="red" size="sm" renderIcon={Warning}>
              Deviation
            </Tag>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.pharma.testing.noTestRecorded"
          defaultMessage="No test recorded"
        />
      </span>
    );
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="pharma-testing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.testing.title"
            defaultMessage="Assay &amp; Test Execution"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.testing.description"
            defaultMessage="Perform analytical testing with data integrity. Execute pharmaceutical assays, record quality controls, and log any deviations with CAPA."
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
                  id="notebook.page.pharma.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.testing.tested"
                  defaultMessage="Tested"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.testing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.testing.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            {deviationCount > 0 && (
              <Tile
                className="progress-tile"
                style={{ borderColor: "#da1e28" }}
              >
                <span className="progress-label" style={{ color: "#da1e28" }}>
                  <FormattedMessage
                    id="notebook.page.pharma.testing.deviations"
                    defaultMessage="Deviations"
                  />
                </span>
                <span className="progress-value" style={{ color: "#da1e28" }}>
                  {deviationCount}
                </span>
              </Tile>
            )}
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Add}
          onClick={handleRecordExecution}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.pharma.testing.recordExecution"
            defaultMessage="Record Test Execution ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleRecordResults}
          disabled={!allSelectedAwaitingResults()}
        >
          <FormattedMessage
            id="notebook.pharma.testing.recordResults"
            defaultMessage="Record Results ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <PermissionGate
          roles={Permissions.VALIDATE_RESULTS}
          disabledTooltip="You need validation permission to mark results as complete"
        >
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleMarkComplete}
            disabled={!allSelectedHaveResults()}
          >
            <FormattedMessage
              id="notebook.pharma.testing.markComplete"
              defaultMessage="Mark Complete"
            />
          </Button>
        </PermissionGate>

        <Button kind="ghost" size="sm" renderIcon={Renew} onClick={loadSamples}>
          <FormattedMessage
            id="notebook.pharma.testing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="pharma-testing"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            {
              key: "assayCategoryDisplay",
              header: "Category",
              render: (value) => value || "-",
            },
            {
              key: "assayTestTypeDisplay",
              header: "Test Type",
              render: (value) => value || "-",
            },
            {
              key: "testStatus",
              header: intl.formatMessage({
                id: "notebook.pharma.testing.testStatusHeader",
                defaultMessage: "Test Status",
              }),
              render: (_, sample) => renderTestStatus(sample),
            },
            {
              key: "result",
              header: intl.formatMessage({
                id: "notebook.pharma.testing.resultHeader",
                defaultMessage: "Result",
              }),
              render: (_, sample) => renderResultValue(sample),
            },
            {
              key: "status",
              header: "Page Status",
              render: (value) => renderStatus(value),
            },
          ]}
          additionalColumns={[
            {
              key: "testInfo",
              header: intl.formatMessage({
                id: "notebook.pharma.testing.testInfo",
                defaultMessage: "Test Info",
              }),
              render: (_, sample) => renderTestInfo(sample),
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.pharma.testing.empty"
              defaultMessage="No samples available for testing. Complete Sample Processing first."
            />
          </p>
        </div>
      )}

      {/* Test Execution Modal (Phase 1) */}
      <Modal
        open={showExecutionModal}
        onRequestClose={() => setShowExecutionModal(false)}
        onRequestSubmit={handleSaveExecution}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.testing.recordExecutionTitle",
          defaultMessage: "Record Test Execution",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.pharma.testing.saveExecution",
          defaultMessage: "Save Execution",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.pharma.testing.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p style={{ color: "#525252" }}>
            <FormattedMessage
              id="notebook.pharma.testing.executionDescription"
              defaultMessage="Recording test execution details for {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Assay Type Selection */}
          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="assay-category"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.category",
                  defaultMessage: "Assay Category",
                })}
                value={testExecutionData.assayCategory}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    assayCategory: e.target.value,
                    testType: "",
                  })
                }
              >
                <SelectItem value="" text="Select category..." />
                {assayCategories.map((cat) => (
                  <SelectItem key={cat.id} value={cat.id} text={cat.text} />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="test-type"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.testType",
                  defaultMessage: "Test Type",
                })}
                value={testExecutionData.testType}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    testType: e.target.value,
                  })
                }
                disabled={!testExecutionData.assayCategory}
              >
                <SelectItem value="" text="Select test type..." />
                {availableTestTypes.map((test) => (
                  <SelectItem
                    key={test.id}
                    value={test.text}
                    text={test.text}
                  />
                ))}
              </Select>
            </Column>
          </Grid>

          {/* Operator and Date */}
          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="operator"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.operator",
                  defaultMessage: "Operator / Analyst",
                })}
                value={testExecutionData.operator}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    operator: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    performedDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="performed-date"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.testing.performedDate",
                    defaultMessage: "Date Performed",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
          </Grid>

          {/* Reagent & Instrument Selection */}
          <div style={{ marginTop: "1rem" }}>
            <h5
              style={{
                marginBottom: "0.5rem",
                display: "flex",
                alignItems: "center",
              }}
            >
              <InventoryManagement
                size={16}
                style={{ marginRight: "0.5rem" }}
              />
              <FormattedMessage
                id="notebook.pharma.testing.inventorySelection"
                defaultMessage="Reagent & Instrument Selection"
              />
            </h5>
            <Grid fullWidth narrow>
              <Column lg={8} md={4} sm={4}>
                <MultiSelect
                  id="selectedReagents"
                  titleText={intl.formatMessage({
                    id: "notebook.pharma.testing.reagentsUsed",
                    defaultMessage: "Reagents Used",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.pharma.testing.reagents.placeholder",
                    defaultMessage: "Select reagents...",
                  })}
                  items={reagents}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItems={reagents.filter((r) =>
                    testExecutionData.reagentsUsed.includes(r.id),
                  )}
                  onChange={({ selectedItems }) =>
                    setTestExecutionData({
                      ...testExecutionData,
                      reagentsUsed: selectedItems.map((r) => r.id),
                    })
                  }
                  disabled={loadingReagents}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <MultiSelect
                  id="selectedInstruments"
                  titleText={intl.formatMessage({
                    id: "notebook.pharma.testing.instrumentsUsed",
                    defaultMessage: "Instruments Used",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.pharma.testing.instruments.placeholder",
                    defaultMessage: "Select test instruments...",
                  })}
                  items={instruments}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItems={instruments.filter((i) =>
                    testExecutionData.instrumentsUsed.includes(i.id),
                  )}
                  onChange={({ selectedItems }) =>
                    setTestExecutionData({
                      ...testExecutionData,
                      instrumentsUsed: selectedItems.map((i) => i.id),
                    })
                  }
                  disabled={loadingInstruments}
                />
              </Column>
            </Grid>
          </div>

          {/* Quality Controls Section */}
          <h5 style={{ marginTop: "0.5rem", marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="notebook.pharma.testing.qualityControls"
              defaultMessage="Quality Controls"
            />
          </h5>

          <Grid fullWidth narrow>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="positive-control"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.positiveControl",
                  defaultMessage: "Positive Control",
                })}
                value={testExecutionData.qcData.positiveControlResult}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    qcData: {
                      ...testExecutionData.qcData,
                      positiveControlResult: e.target.value,
                    },
                  })
                }
                placeholder="Pass/Fail"
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="negative-control"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.negativeControl",
                  defaultMessage: "Negative Control",
                })}
                value={testExecutionData.qcData.negativeControlResult}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    qcData: {
                      ...testExecutionData.qcData,
                      negativeControlResult: e.target.value,
                    },
                  })
                }
                placeholder="Pass/Fail"
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <NumberInput
                id="replicate-count"
                label={intl.formatMessage({
                  id: "notebook.pharma.testing.replicates",
                  defaultMessage: "Replicates",
                })}
                value={testExecutionData.qcData.replicateCount}
                onChange={(_, { value }) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    qcData: {
                      ...testExecutionData.qcData,
                      replicateCount: value,
                    },
                  })
                }
                min={1}
                max={10}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="rsd-value"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.rsd",
                  defaultMessage: "%RSD / CV",
                })}
                value={testExecutionData.qcData.rsdValue}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    qcData: {
                      ...testExecutionData.qcData,
                      rsdValue: e.target.value,
                    },
                  })
                }
                placeholder="e.g., 2.5%"
              />
            </Column>
          </Grid>

          <Checkbox
            id="internal-standard"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.internalStandard",
              defaultMessage: "Internal Standard Used",
            })}
            checked={testExecutionData.qcData.internalStandardUsed}
            onChange={(_, { checked }) =>
              setTestExecutionData({
                ...testExecutionData,
                qcData: {
                  ...testExecutionData.qcData,
                  internalStandardUsed: checked,
                },
              })
            }
          />

          <TextInput
            id="acceptance-criteria"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.acceptanceCriteria",
              defaultMessage: "Acceptance Criteria",
            })}
            value={testExecutionData.qcData.acceptanceCriteria}
            onChange={(e) =>
              setTestExecutionData({
                ...testExecutionData,
                qcData: {
                  ...testExecutionData.qcData,
                  acceptanceCriteria: e.target.value,
                },
              })
            }
            placeholder="e.g., 95.0% - 105.0% of label claim"
          />

          {/* Deviation Section */}
          <h5 style={{ marginTop: "0.5rem", marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="notebook.pharma.testing.deviationHandling"
              defaultMessage="Deviation Handling"
            />
          </h5>

          <Checkbox
            id="has-deviation"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.hasDeviation",
              defaultMessage: "Log a Deviation for this test",
            })}
            checked={testExecutionData.hasDeviation}
            onChange={(_, { checked }) =>
              setTestExecutionData({
                ...testExecutionData,
                hasDeviation: checked,
                deviation: checked
                  ? { description: "", rootCause: "", capaAction: "" }
                  : null,
              })
            }
          />

          {testExecutionData.hasDeviation && (
            <>
              <TextArea
                id="deviation-description"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.deviationDescription",
                  defaultMessage: "Deviation Description",
                })}
                value={testExecutionData.deviation?.description || ""}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    deviation: {
                      ...testExecutionData.deviation,
                      description: e.target.value,
                    },
                  })
                }
                placeholder="Describe what went wrong..."
                rows={2}
              />
              <TextInput
                id="root-cause"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.rootCause",
                  defaultMessage: "Root Cause",
                })}
                value={testExecutionData.deviation?.rootCause || ""}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    deviation: {
                      ...testExecutionData.deviation,
                      rootCause: e.target.value,
                    },
                  })
                }
              />
              <TextArea
                id="capa-action"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.capaAction",
                  defaultMessage: "CAPA (Corrective and Preventive Action)",
                })}
                value={testExecutionData.deviation?.capaAction || ""}
                onChange={(e) =>
                  setTestExecutionData({
                    ...testExecutionData,
                    deviation: {
                      ...testExecutionData.deviation,
                      capaAction: e.target.value,
                    },
                  })
                }
                placeholder="Describe corrective actions taken..."
                rows={2}
              />
            </>
          )}
        </div>
      </Modal>

      {/* Result Entry Modal (Phase 2) - With Electronic Signature */}
      <Modal
        open={showResultModal}
        onRequestClose={() => setShowResultModal(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.testing.recordResultsTitle",
          defaultMessage: "Record Test Results",
        })}
        passiveModal
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p style={{ color: "#525252" }}>
            <FormattedMessage
              id="notebook.pharma.testing.resultsDescription"
              defaultMessage="Recording results for {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="result-value"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.resultValue",
                  defaultMessage: "Result Value",
                })}
                value={testResultData.value}
                onChange={(e) =>
                  setTestResultData({
                    ...testResultData,
                    value: e.target.value,
                  })
                }
                placeholder="e.g., 98.5"
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="result-unit"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.resultUnit",
                  defaultMessage: "Unit",
                })}
                value={testResultData.unit}
                onChange={(e) =>
                  setTestResultData({
                    ...testResultData,
                    unit: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select unit..." />
                {unitOptions.map((opt) => (
                  <SelectItem key={opt.id} value={opt.id} text={opt.text} />
                ))}
              </Select>
            </Column>
          </Grid>

          <Select
            id="result-outcome"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.resultOutcome",
              defaultMessage: "Outcome",
            })}
            value={testResultData.outcome}
            onChange={(e) =>
              setTestResultData({
                ...testResultData,
                outcome: e.target.value,
              })
            }
          >
            <SelectItem value="" text="Select outcome..." />
            {outcomeOptions.map((opt) => (
              <SelectItem key={opt.id} value={opt.id} text={opt.text} />
            ))}
          </Select>

          <TextArea
            id="result-notes"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.resultNotes",
              defaultMessage: "Notes",
            })}
            value={testResultData.notes}
            onChange={(e) =>
              setTestResultData({
                ...testResultData,
                notes: e.target.value,
              })
            }
            placeholder="Additional observations..."
            rows={3}
          />

          <TextInput
            id="recorded-by"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.recordedBy",
              defaultMessage: "Recorded By",
            })}
            value={testResultData.recordedBy}
            onChange={(e) =>
              setTestResultData({
                ...testResultData,
                recordedBy: e.target.value,
              })
            }
            required
          />

          {/* Modal Footer with E-Signature Button */}
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: "1rem",
              marginTop: "1rem",
              paddingTop: "1rem",
              borderTop: "1px solid #e0e0e0",
            }}
          >
            <Button kind="secondary" onClick={() => setShowResultModal(false)}>
              <FormattedMessage
                id="notebook.pharma.testing.cancel"
                defaultMessage="Cancel"
              />
            </Button>
            <Button
              kind="primary"
              onClick={handleSaveResultsClick}
              disabled={!isResultFormValid() || isCheckingEnabled}
            >
              <FormattedMessage
                id="notebook.pharma.testing.saveResults"
                defaultMessage="Save Results"
              />
            </Button>
          </div>
        </div>
      </Modal>

      {/* E-Signature Modal for Result Entry (AUTHORED) */}
      <ESignatureModal {...signatureModalProps} />

      {/* E-Signature Modal for Validation/Completion (VALIDATED_AND_RELEASED) */}
      <ESignatureModal {...validationSignatureModalProps} />
    </div>
  );
}

export default PharmaceuticalTestingPage;
