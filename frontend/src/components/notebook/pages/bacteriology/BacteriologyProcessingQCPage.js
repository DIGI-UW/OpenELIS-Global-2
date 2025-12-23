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
  InlineLoading,
  Checkbox,
} from "@carbon/react";
import {
  CheckmarkFilled,
  Renew,
  Chemistry,
  Microscope,
  WarningAlt,
  Add,
  Settings,
  Calendar,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologyProcessingQCPage - Page 4 of the Bacteriology workflow.
 * Two-step workflow similar to MNTD Test Assignment & Machine Scheduling.
 *
 * STEP 1: Preparation Assignment (bulk assign to selected samples)
 * - Assign culture media, biochemical media, incubation conditions
 * - Link samples to QC-verified reagent batches
 *
 * STEP 2: Sample Processing (bulk process selected samples)
 * - Gram staining, Microscopy, Inoculation, Incubation
 * - QC Pass/Fail determination
 *
 * Who uses it:
 * - Lab technician
 * - Microbiologist
 * - Supervisor
 *
 * System Actions:
 * - Status updated: Processing Complete or Processing Failed
 *
 * Leads to: Next workflow step (Assay/Test Execution)
 */

// Culture Media Types
const CULTURE_MEDIA_TYPES = [
  { id: "BLOOD_AGAR", text: "Blood Agar" },
  { id: "MACCONKEY", text: "MacConkey Agar" },
  { id: "SABOURAUD", text: "Sabouraud Dextrose Agar" },
  { id: "CHOCOLATE_AGAR", text: "Chocolate Agar" },
  { id: "MUELLER_HINTON", text: "Mueller-Hinton Agar" },
  { id: "XLD", text: "XLD Agar" },
  { id: "SS_AGAR", text: "Salmonella-Shigella Agar" },
  { id: "TCBS", text: "TCBS Agar" },
  { id: "MANNITOL_SALT", text: "Mannitol Salt Agar" },
  { id: "EMB", text: "Eosin Methylene Blue Agar" },
  { id: "CLED", text: "CLED Agar" },
  { id: "OTHER", text: "Other (specify)" },
];

// Biochemical Media Types
const BIOCHEMICAL_MEDIA_TYPES = [
  { id: "TSI", text: "Triple Sugar Iron (TSI)" },
  { id: "UREASE", text: "Urease Test Medium" },
  { id: "CITRATE", text: "Simmons Citrate Agar" },
  { id: "INDOLE", text: "Indole Test Medium" },
  { id: "MR_VP", text: "MR-VP Broth" },
  { id: "MOTILITY", text: "Motility Test Medium" },
  { id: "LYSINE", text: "Lysine Decarboxylase" },
  { id: "ORNITHINE", text: "Ornithine Decarboxylase" },
  { id: "OXIDASE", text: "Oxidase Reagent" },
  { id: "CATALASE", text: "Catalase Reagent" },
  { id: "COAGULASE", text: "Coagulase Test" },
  { id: "OTHER", text: "Other (specify)" },
];

// Enrichment Media Types
const ENRICHMENT_MEDIA_TYPES = [
  { id: "SELENITE_F", text: "Selenite F Broth" },
  { id: "TETRATHIONATE", text: "Tetrathionate Broth" },
  { id: "RAPPAPORT", text: "Rappaport-Vassiliadis Broth" },
  { id: "BHI", text: "Brain Heart Infusion Broth" },
  { id: "THIOGLYCOLLATE", text: "Thioglycollate Broth" },
  { id: "ALKALINE_PEPTONE", text: "Alkaline Peptone Water" },
  { id: "BUFFERED_PEPTONE", text: "Buffered Peptone Water" },
  { id: "TRYPTIC_SOY", text: "Tryptic Soy Broth" },
  { id: "OTHER", text: "Other (specify)" },
];

// Incubation Conditions
const INCUBATION_CONDITIONS = [
  { id: "AEROBIC_37", text: "Aerobic, 37°C" },
  { id: "AEROBIC_35", text: "Aerobic, 35°C" },
  { id: "CO2_37", text: "5-10% CO2, 37°C" },
  { id: "ANAEROBIC_37", text: "Anaerobic, 37°C" },
  { id: "MICROAEROPHILIC", text: "Microaerophilic, 37°C" },
  { id: "ROOM_TEMP", text: "Room Temperature (25°C)" },
  { id: "REFRIGERATED", text: "Refrigerated (4°C)" },
  { id: "OTHER", text: "Other (specify)" },
];

// Incubation Duration Options
const INCUBATION_DURATIONS = [
  { id: "18_24", text: "18-24 hours (Standard)" },
  { id: "24_48", text: "24-48 hours" },
  { id: "48_72", text: "48-72 hours" },
  { id: "72_96", text: "72-96 hours (Extended)" },
  { id: "5_7_DAYS", text: "5-7 days (Slow growers)" },
  { id: "CUSTOM", text: "Custom duration" },
];

// Gram Stain Results
const GRAM_STAIN_RESULTS = [
  { id: "GRAM_POS_COCCI", text: "Gram-positive cocci" },
  { id: "GRAM_POS_BACILLI", text: "Gram-positive bacilli" },
  { id: "GRAM_NEG_COCCI", text: "Gram-negative cocci" },
  { id: "GRAM_NEG_BACILLI", text: "Gram-negative bacilli" },
  { id: "MIXED_FLORA", text: "Mixed flora" },
  { id: "NO_ORGANISMS", text: "No organisms seen" },
  { id: "YEAST", text: "Yeast cells" },
  { id: "OTHER", text: "Other (specify)" },
];

// QC Failure Reasons
const QC_FAILURE_REASONS = [
  { id: "CONTAMINATION", text: "Contamination detected" },
  { id: "NO_GROWTH", text: "No growth after expected time" },
  { id: "POOR_ISOLATION", text: "Poor isolation of colonies" },
  { id: "MEDIA_FAILURE", text: "Media quality issue" },
  {
    id: "TEMPERATURE_DEVIATION",
    text: "Temperature deviation during incubation",
  },
  { id: "TECHNICAL_ERROR", text: "Technical error during processing" },
  { id: "SAMPLE_QUALITY", text: "Poor sample quality" },
  { id: "OTHER", text: "Other (specify)" },
];

function BacteriologyProcessingQCPage({ entryId, pageData, onProgressUpdate }) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // ==========================================
  // STEP 1: Preparation Assignment Modal State
  // ==========================================
  const [preparationModalOpen, setPreparationModalOpen] = useState(false);
  const [preparationData, setPreparationData] = useState({
    // Culture Media Selection
    cultureMedia: [],
    cultureMediaBatch: "",
    // Biochemical Media Selection
    biochemicalMedia: [],
    biochemicalMediaBatch: "",
    // Enrichment Media (optional)
    enrichmentMedia: "",
    enrichmentMediaBatch: "",
    // Incubation Settings
    incubationCondition: "",
    incubationDuration: "",
    customDuration: "",
    // Assignment metadata
    notes: "",
  });

  // ==========================================
  // STEP 2: Sample Processing Modal State
  // ==========================================
  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [processingData, setProcessingData] = useState({
    // Gram Staining
    gramStainPerformed: false,
    gramStainResult: "",
    otherGramStainResult: "",
    gramStainDate: new Date().toISOString().split("T")[0],
    gramStainBy: "",
    // Microscopy
    microscopyPerformed: false,
    microscopyFindings: "",
    preliminaryIdentification: "",
    microscopyDate: "",
    microscopyBy: "",
    // Inoculation Confirmation
    inoculationConfirmed: false,
    inoculationDate: new Date().toISOString().split("T")[0],
    inoculationBy: "",
    // Incubation Start
    incubationStarted: false,
    incubationStartTime: "",
    expectedReadTime: "",
    // QC Result
    qcResult: "",
    qcFailureReason: "",
    retakeRequired: false,
    notes: "",
  });

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
              // Preparation assignment data (Step 1)
              cultureMedia: sample.data?.cultureMedia,
              cultureMediaBatch: sample.data?.cultureMediaBatch,
              biochemicalMedia: sample.data?.biochemicalMedia,
              incubationCondition: sample.data?.incubationCondition,
              incubationDuration: sample.data?.incubationDuration,
              preparationAssigned: sample.data?.preparationAssigned,
              preparationDate: sample.data?.preparationDate,
              // Processing data (Step 2)
              processingStatus: sample.data?.processingStatus || "NOT_STARTED",
              gramStainResult: sample.data?.gramStainResult,
              microscopyFindings: sample.data?.microscopyFindings,
              qcResult: sample.data?.qcResult,
              qcFailureReason: sample.data?.qcFailureReason,
              processedDate: sample.data?.processedDate,
              // From previous pages
              sampleOrigin: sample.data?.sampleOrigin,
              projectName: sample.data?.projectName,
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

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const preparationAssigned = samples.filter(
      (s) => s.preparationAssigned,
    ).length;
    const processed = samples.filter(
      (s) =>
        s.processingStatus &&
        s.processingStatus !== "NOT_STARTED" &&
        s.processingStatus !== "PENDING",
    ).length;
    const qcPassed = samples.filter(
      (s) =>
        s.qcResult === "PASS" ||
        s.processingStatus === "QC_PASSED" ||
        s.processingStatus === "COMPLETED",
    ).length;
    const qcFailed = samples.filter(
      (s) => s.qcResult === "FAIL" || s.processingStatus === "QC_FAILED",
    ).length;
    const fullyComplete = samples.filter(
      (s) => s.preparationAssigned && s.qcResult === "PASS",
    ).length;
    return {
      total: samples.length,
      preparationAssigned,
      processed,
      qcPassed,
      qcFailed,
      fullyComplete,
    };
  }, [samples]);

  // ==========================================
  // STEP 1: Preparation Assignment Handlers
  // ==========================================

  const handleOpenPreparationModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.processing.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    // Reset preparation data
    setPreparationData({
      cultureMedia: [],
      cultureMediaBatch: "",
      biochemicalMedia: [],
      biochemicalMediaBatch: "",
      enrichmentMedia: "",
      enrichmentMediaBatch: "",
      incubationCondition: "",
      incubationDuration: "",
      customDuration: "",
      notes: "",
    });
    setPreparationModalOpen(true);
  }, [selectedIds, intl]);

  const handleSavePreparationData = useCallback(() => {
    if (preparationData.cultureMedia.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.preparation.cultureMediaRequired",
          defaultMessage: "Please select at least one culture media type.",
        }),
      );
      return;
    }

    if (!preparationData.incubationCondition) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.preparation.incubationRequired",
          defaultMessage: "Please select incubation condition.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setPreparationModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      preparationAssigned: true,
      preparationDate: new Date().toISOString().split("T")[0],
      cultureMedia: preparationData.cultureMedia.map((m) => m.id),
      cultureMediaBatch: preparationData.cultureMediaBatch,
      biochemicalMedia: preparationData.biochemicalMedia.map((m) => m.id),
      biochemicalMediaBatch: preparationData.biochemicalMediaBatch,
      enrichmentMedia: preparationData.enrichmentMedia,
      enrichmentMediaBatch: preparationData.enrichmentMediaBatch,
      incubationCondition: preparationData.incubationCondition,
      incubationDuration:
        preparationData.incubationDuration === "CUSTOM"
          ? preparationData.customDuration
          : preparationData.incubationDuration,
      preparationNotes: preparationData.notes,
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
                      id: "notebook.bacteriology.preparation.saved",
                      defaultMessage:
                        "Preparation assigned to {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setPreparationModalOpen(false);
                setSelectedIds([]);
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save preparation data.");
          }
        }
      },
    );
  }, [
    preparationData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // STEP 2: Sample Processing Handlers
  // ==========================================

  const handleOpenProcessingModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.processing.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    // Check if selected samples have preparation assigned
    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const withoutPreparation = selectedSamples.filter(
      (s) => !s.preparationAssigned,
    ).length;

    if (withoutPreparation > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.bacteriology.processing.preparationRequired",
            defaultMessage:
              "{count} sample(s) do not have preparation assigned. Please assign preparation first.",
          },
          { count: withoutPreparation },
        ),
      );
      return;
    }

    // Reset processing data
    setProcessingData({
      gramStainPerformed: false,
      gramStainResult: "",
      otherGramStainResult: "",
      gramStainDate: new Date().toISOString().split("T")[0],
      gramStainBy: "",
      microscopyPerformed: false,
      microscopyFindings: "",
      preliminaryIdentification: "",
      microscopyDate: "",
      microscopyBy: "",
      inoculationConfirmed: false,
      inoculationDate: new Date().toISOString().split("T")[0],
      inoculationBy: "",
      incubationStarted: false,
      incubationStartTime: "",
      expectedReadTime: "",
      qcResult: "",
      qcFailureReason: "",
      retakeRequired: false,
      notes: "",
    });
    setProcessingModalOpen(true);
  }, [selectedIds, samples, intl]);

  const handleSaveProcessingData = useCallback(() => {
    if (!hasRealPageId) {
      setProcessingModalOpen(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Determine processing status based on what was done
    let processingStatus = "IN_PROGRESS";
    if (processingData.qcResult === "PASS") {
      processingStatus = "QC_PASSED";
    } else if (processingData.qcResult === "FAIL") {
      processingStatus = "QC_FAILED";
    } else if (processingData.incubationStarted) {
      processingStatus = "INCUBATING";
    } else if (processingData.inoculationConfirmed) {
      processingStatus = "INOCULATED";
    } else if (processingData.microscopyPerformed) {
      processingStatus = "MICROSCOPY_DONE";
    } else if (processingData.gramStainPerformed) {
      processingStatus = "GRAM_STAIN_DONE";
    }

    const dataToSave = {
      processingStatus,
      gramStainPerformed: processingData.gramStainPerformed,
      gramStainResult: processingData.gramStainResult,
      gramStainDate: processingData.gramStainDate,
      gramStainBy: processingData.gramStainBy,
      microscopyPerformed: processingData.microscopyPerformed,
      microscopyFindings: processingData.microscopyFindings,
      preliminaryIdentification: processingData.preliminaryIdentification,
      microscopyDate: processingData.microscopyDate,
      microscopyBy: processingData.microscopyBy,
      inoculationConfirmed: processingData.inoculationConfirmed,
      inoculationDate: processingData.inoculationDate,
      inoculationBy: processingData.inoculationBy,
      incubationStarted: processingData.incubationStarted,
      incubationStartTime: processingData.incubationStartTime,
      expectedReadTime: processingData.expectedReadTime,
      qcResult: processingData.qcResult,
      qcFailureReason: processingData.qcFailureReason,
      retakeRequired: processingData.retakeRequired,
      processingNotes: processingData.notes,
      processedDate: new Date().toISOString(),
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
            const newStatus =
              processingStatus === "QC_PASSED"
                ? "COMPLETED"
                : processingStatus === "QC_FAILED"
                  ? "REJECTED"
                  : "IN_PROGRESS";
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
                      id: "notebook.bacteriology.processing.saved",
                      defaultMessage:
                        "Processing data saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setProcessingModalOpen(false);
                setSelectedIds([]);
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save processing data.");
          }
        }
      },
    );
  }, [
    processingData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Bulk mark as QC Passed
  const handleBulkMarkQCPassed = useCallback(() => {
    if (selectedIds.length === 0) return;

    // Check that selected samples have preparation assigned
    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const incompleteCount = selectedSamples.filter(
      (s) => !s.preparationAssigned,
    ).length;

    if (incompleteCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.bacteriology.processing.incompleteWarning",
            defaultMessage:
              "{count} sample(s) are missing preparation assignment. Please complete preparation first.",
          },
          { count: incompleteCount },
        ),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.processing.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // First apply QC data
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: {
          qcResult: "PASS",
          processingStatus: "QC_PASSED",
          processedDate: new Date().toISOString(),
        },
      }),
      (response) => {
        if (response && !response.error) {
          // Then update status
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: numericIds,
              status: "COMPLETED",
            }),
            (status) => {
              if (status === 200) {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.bacteriology.processing.markedQCPassed",
                      defaultMessage:
                        "Marked {count} samples as QC Passed (ready for testing).",
                    },
                    { count: selectedIds.length },
                  ),
                );
                loadPageSamples();
                setSelectedIds([]);
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              } else {
                setError("Failed to update sample status. Please try again.");
              }
            },
          );
        } else {
          setError(response?.error || "Failed to apply QC data.");
        }
      },
    );
  }, [
    selectedIds,
    samples,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Get culture media labels
  const getCultureMediaLabel = (mediaIds) => {
    if (!mediaIds || !Array.isArray(mediaIds) || mediaIds.length === 0)
      return null;
    return mediaIds
      .map((id) => CULTURE_MEDIA_TYPES.find((m) => m.id === id)?.text || id)
      .join(", ");
  };

  // Get incubation condition label
  const getIncubationLabel = (conditionId) => {
    const condition = INCUBATION_CONDITIONS.find((c) => c.id === conditionId);
    return condition ? condition.text : conditionId;
  };

  // Render preparation info column
  const renderPreparationInfo = (sample) => {
    if (!sample) return null;
    if (sample.preparationAssigned) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="blue" size="sm">
            <Chemistry size={12} style={{ marginRight: "4px" }} />
            Prep Assigned
          </Tag>
          {sample.cultureMedia && (
            <div style={{ marginTop: "4px", color: "#525252" }}>
              Media: {getCultureMediaLabel(sample.cultureMedia)}
            </div>
          )}
          {sample.incubationCondition && (
            <div style={{ color: "#525252" }}>
              {getIncubationLabel(sample.incubationCondition)}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.bacteriology.preparation.notAssigned"
          defaultMessage="Not assigned"
        />
      </span>
    );
  };

  // Render processing info column
  const renderProcessingInfo = (sample) => {
    if (!sample) return null;
    if (sample.qcResult || sample.processingStatus !== "NOT_STARTED") {
      const statusConfig = {
        NOT_STARTED: { type: "gray", text: "Not Started" },
        GRAM_STAIN_DONE: { type: "blue", text: "Gram Stain Done" },
        MICROSCOPY_DONE: { type: "blue", text: "Microscopy Done" },
        INOCULATED: { type: "purple", text: "Inoculated" },
        INCUBATING: { type: "teal", text: "Incubating" },
        QC_PASSED: { type: "green", text: "QC Passed" },
        QC_FAILED: { type: "red", text: "QC Failed" },
        COMPLETED: { type: "green", text: "Completed" },
        IN_PROGRESS: { type: "blue", text: "In Progress" },
      };
      const config =
        statusConfig[sample.processingStatus] ||
        statusConfig[sample.qcResult === "PASS" ? "QC_PASSED" : "IN_PROGRESS"];

      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type={config.type} size="sm">
            {config.text}
          </Tag>
          {sample.gramStainResult && (
            <div style={{ marginTop: "4px", color: "#525252" }}>
              Gram:{" "}
              {GRAM_STAIN_RESULTS.find((g) => g.id === sample.gramStainResult)
                ?.text || sample.gramStainResult}
            </div>
          )}
          {sample.microscopyFindings && (
            <div style={{ color: "#525252", fontSize: "11px" }}>
              {sample.microscopyFindings.substring(0, 30)}...
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.bacteriology.processing.notProcessed"
          defaultMessage="Not processed"
        />
      </span>
    );
  };

  return (
    <div className="bacteriology-processing-qc-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <Microscope
            size={20}
            style={{ marginRight: "8px", verticalAlign: "middle" }}
          />
          <FormattedMessage
            id="notebook.page.bacteriology.processingQC.title"
            defaultMessage="Processing & Quality Control"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.processingQC.description"
            defaultMessage="Step 1: Assign preparation (media, incubation) to samples. Step 2: Process samples through Gram staining, microscopy, inoculation, and QC verification."
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
                  id="notebook.bacteriology.processing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.processing.prepAssigned"
                  defaultMessage="Prep Assigned"
                />
              </span>
              <span className="progress-value">
                {stats.preparationAssigned}
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.processing.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{stats.processed}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.processing.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{stats.qcPassed}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bacteriology.processing.qcFailed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{stats.qcFailed}</span>
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

      {/* Action Buttons - Two-step workflow similar to MNTD */}
      <div className="page-actions-bar">
        {/* Step 1: Assign Preparation */}
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={handleOpenPreparationModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.bacteriology.processing.assignPreparation"
            defaultMessage="Assign Preparation ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {/* Step 2: Record Processing */}
        <Button
          kind="secondary"
          size="sm"
          renderIcon={Microscope}
          onClick={handleOpenProcessingModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.bacteriology.processing.recordProcessing"
            defaultMessage="Record Processing ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {/* Quick QC Pass */}
        {selectedIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleBulkMarkQCPassed}
          >
            <FormattedMessage
              id="notebook.bacteriology.processing.markQCPassed"
              defaultMessage="Mark QC Passed ({count})"
              values={{ count: selectedIds.length }}
            />
          </Button>
        )}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.bacteriology.processing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="bacteriology-processing"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={[
            {
              key: "preparationInfo",
              header: intl.formatMessage({
                id: "notebook.bacteriology.processing.preparationInfo",
                defaultMessage: "Preparation",
              }),
              render: renderPreparationInfo,
            },
            {
              key: "processingInfo",
              header: intl.formatMessage({
                id: "notebook.bacteriology.processing.processingInfo",
                defaultMessage: "Processing",
              }),
              render: renderProcessingInfo,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.bacteriology.processing.empty"
              defaultMessage="No samples available for processing. Please complete the temporary storage step first."
            />
          </p>
        </div>
      )}

      {/* ========================================== */}
      {/* STEP 1: Preparation Assignment Modal */}
      {/* ========================================== */}
      <Modal
        open={preparationModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.preparation.modal.title",
          defaultMessage: "Assign Preparation",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setPreparationModalOpen(false)}
        onRequestSubmit={handleSavePreparationData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.preparation.modal.description"
              defaultMessage="Assign preparation settings for {count} selected sample(s). All samples will receive the same media and incubation configuration."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Culture Media Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#e0f0ff",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #0f62fe",
            }}
          >
            <h5 style={{ marginBottom: "0.75rem", color: "#0f62fe" }}>
              <Chemistry
                size={16}
                style={{ marginRight: "0.5rem", verticalAlign: "middle" }}
              />
              <FormattedMessage
                id="notebook.bacteriology.preparation.cultureMedia"
                defaultMessage="Culture Media Selection *"
              />
            </h5>

            <Grid fullWidth>
              <Column lg={12} md={6} sm={4}>
                <MultiSelect
                  id="cultureMediaSelection"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.cultureMediaTypes",
                    defaultMessage: "Culture Media Types",
                  })}
                  label="Select media..."
                  items={CULTURE_MEDIA_TYPES}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItems={preparationData.cultureMedia}
                  onChange={({ selectedItems }) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      cultureMedia: selectedItems,
                    }))
                  }
                />
              </Column>
              <Column lg={4} md={2} sm={4}>
                <TextInput
                  id="cultureMediaBatch"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.batchNumber",
                    defaultMessage: "Batch Number",
                  })}
                  value={preparationData.cultureMediaBatch}
                  onChange={(e) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      cultureMediaBatch: e.target.value,
                    }))
                  }
                  placeholder="e.g., BA-2024-001"
                />
              </Column>
            </Grid>
          </div>

          {/* Biochemical Media Section (Optional) */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #6f6f6f",
            }}
          >
            <h5 style={{ marginBottom: "0.75rem", color: "#525252" }}>
              <Settings
                size={16}
                style={{ marginRight: "0.5rem", verticalAlign: "middle" }}
              />
              <FormattedMessage
                id="notebook.bacteriology.preparation.biochemMedia"
                defaultMessage="Biochemical Media (Optional)"
              />
            </h5>

            <Grid fullWidth>
              <Column lg={12} md={6} sm={4}>
                <MultiSelect
                  id="biochemMediaSelection"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.biochemMediaTypes",
                    defaultMessage: "Biochemical Media Types",
                  })}
                  label="Select media..."
                  items={BIOCHEMICAL_MEDIA_TYPES}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItems={preparationData.biochemicalMedia}
                  onChange={({ selectedItems }) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      biochemicalMedia: selectedItems,
                    }))
                  }
                />
              </Column>
              <Column lg={4} md={2} sm={4}>
                <TextInput
                  id="biochemMediaBatch"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.batchNumber",
                    defaultMessage: "Batch Number",
                  })}
                  value={preparationData.biochemicalMediaBatch}
                  onChange={(e) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      biochemicalMediaBatch: e.target.value,
                    }))
                  }
                  placeholder="e.g., TSI-2024-001"
                />
              </Column>
            </Grid>
          </div>

          {/* Enrichment Media Section (Optional) */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #6f6f6f",
            }}
          >
            <h5 style={{ marginBottom: "0.75rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.bacteriology.preparation.enrichmentMedia"
                defaultMessage="Enrichment Media (Optional)"
              />
            </h5>

            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="enrichmentMediaSelection"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.enrichmentMediaType",
                    defaultMessage: "Enrichment Media Type",
                  })}
                  label="Select enrichment media..."
                  items={ENRICHMENT_MEDIA_TYPES}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={ENRICHMENT_MEDIA_TYPES.find(
                    (m) => m.id === preparationData.enrichmentMedia,
                  )}
                  onChange={({ selectedItem }) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      enrichmentMedia: selectedItem?.id || "",
                    }))
                  }
                />
              </Column>
              <Column lg={4} md={2} sm={4}>
                <TextInput
                  id="enrichmentMediaBatch"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.batchNumber",
                    defaultMessage: "Batch Number",
                  })}
                  value={preparationData.enrichmentMediaBatch}
                  onChange={(e) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      enrichmentMediaBatch: e.target.value,
                    }))
                  }
                  placeholder="e.g., SEL-2024-001"
                />
              </Column>
            </Grid>
          </div>

          {/* Incubation Settings Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#defbe6",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #24a148",
            }}
          >
            <h5 style={{ marginBottom: "0.75rem", color: "#24a148" }}>
              <Calendar
                size={16}
                style={{ marginRight: "0.5rem", verticalAlign: "middle" }}
              />
              <FormattedMessage
                id="notebook.bacteriology.preparation.incubationSettings"
                defaultMessage="Incubation Settings *"
              />
            </h5>

            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="incubationCondition"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.incubationCondition",
                    defaultMessage: "Incubation Condition *",
                  })}
                  label="Select condition..."
                  items={INCUBATION_CONDITIONS}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={INCUBATION_CONDITIONS.find(
                    (c) => c.id === preparationData.incubationCondition,
                  )}
                  onChange={({ selectedItem }) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      incubationCondition: selectedItem?.id || "",
                    }))
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="incubationDuration"
                  titleText={intl.formatMessage({
                    id: "notebook.bacteriology.preparation.incubationDuration",
                    defaultMessage: "Incubation Duration",
                  })}
                  label="Select duration..."
                  items={INCUBATION_DURATIONS}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={INCUBATION_DURATIONS.find(
                    (d) => d.id === preparationData.incubationDuration,
                  )}
                  onChange={({ selectedItem }) =>
                    setPreparationData((prev) => ({
                      ...prev,
                      incubationDuration: selectedItem?.id || "",
                    }))
                  }
                />
              </Column>
              {preparationData.incubationDuration === "CUSTOM" && (
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="customDuration"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.preparation.customDuration",
                      defaultMessage: "Custom Duration (hours)",
                    })}
                    value={preparationData.customDuration}
                    onChange={(e) =>
                      setPreparationData((prev) => ({
                        ...prev,
                        customDuration: e.target.value,
                      }))
                    }
                    placeholder="e.g., 36"
                  />
                </Column>
              )}
            </Grid>
          </div>

          {/* Notes */}
          <TextArea
            id="preparationNotes"
            labelText={intl.formatMessage({
              id: "notebook.bacteriology.preparation.notes",
              defaultMessage: "Notes",
            })}
            value={preparationData.notes}
            onChange={(e) =>
              setPreparationData((prev) => ({
                ...prev,
                notes: e.target.value,
              }))
            }
            rows={2}
            placeholder="Additional preparation notes..."
          />
        </div>
      </Modal>

      {/* ========================================== */}
      {/* STEP 2: Sample Processing Modal */}
      {/* ========================================== */}
      <Modal
        open={processingModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.processing.modal.title",
          defaultMessage: "Record Sample Processing",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setProcessingModalOpen(false)}
        onRequestSubmit={handleSaveProcessingData}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.bacteriology.processing.modal.description"
              defaultMessage="Record processing data for {count} selected sample(s). Check the steps you have completed."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Gram Staining Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: processingData.gramStainPerformed
                ? "#defbe6"
                : "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: `4px solid ${processingData.gramStainPerformed ? "#24a148" : "#8d8d8d"}`,
            }}
          >
            <Checkbox
              id="gramStainPerformed"
              labelText={
                <span style={{ fontWeight: "600" }}>
                  <FormattedMessage
                    id="notebook.bacteriology.processing.gramStainPerformed"
                    defaultMessage="Gram Staining Performed"
                  />
                </span>
              }
              checked={processingData.gramStainPerformed}
              onChange={(e, { checked }) =>
                setProcessingData((prev) => ({
                  ...prev,
                  gramStainPerformed: checked,
                }))
              }
            />

            {processingData.gramStainPerformed && (
              <Grid fullWidth style={{ marginTop: "0.75rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <Dropdown
                    id="gramStainResult"
                    titleText={intl.formatMessage({
                      id: "notebook.bacteriology.processing.gramResult",
                      defaultMessage: "Gram Stain Result",
                    })}
                    label="Select result..."
                    items={GRAM_STAIN_RESULTS}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={GRAM_STAIN_RESULTS.find(
                      (r) => r.id === processingData.gramStainResult,
                    )}
                    onChange={({ selectedItem }) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        gramStainResult: selectedItem?.id || "",
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="gramStainBy"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.processing.performedBy",
                      defaultMessage: "Performed By",
                    })}
                    value={processingData.gramStainBy}
                    onChange={(e) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        gramStainBy: e.target.value,
                      }))
                    }
                  />
                </Column>
              </Grid>
            )}
          </div>

          {/* Microscopy Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: processingData.microscopyPerformed
                ? "#defbe6"
                : "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: `4px solid ${processingData.microscopyPerformed ? "#24a148" : "#8d8d8d"}`,
            }}
          >
            <Checkbox
              id="microscopyPerformed"
              labelText={
                <span style={{ fontWeight: "600" }}>
                  <FormattedMessage
                    id="notebook.bacteriology.processing.microscopyPerformed"
                    defaultMessage="Microscopy Examination Performed"
                  />
                </span>
              }
              checked={processingData.microscopyPerformed}
              onChange={(e, { checked }) =>
                setProcessingData((prev) => ({
                  ...prev,
                  microscopyPerformed: checked,
                }))
              }
            />

            {processingData.microscopyPerformed && (
              <Grid fullWidth style={{ marginTop: "0.75rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="microscopyFindings"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.processing.microscopyFindings",
                      defaultMessage: "Microscopy Findings",
                    })}
                    value={processingData.microscopyFindings}
                    onChange={(e) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        microscopyFindings: e.target.value,
                      }))
                    }
                    rows={2}
                    placeholder="Describe morphology, arrangement, quantity..."
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="preliminaryId"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.processing.preliminaryId",
                      defaultMessage: "Preliminary Identification",
                    })}
                    value={processingData.preliminaryIdentification}
                    onChange={(e) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        preliminaryIdentification: e.target.value,
                      }))
                    }
                    placeholder="e.g., Gram-positive cocci in clusters"
                  />
                </Column>
              </Grid>
            )}
          </div>

          {/* Inoculation Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: processingData.inoculationConfirmed
                ? "#e5f6ff"
                : "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: `4px solid ${processingData.inoculationConfirmed ? "#0f62fe" : "#8d8d8d"}`,
            }}
          >
            <Checkbox
              id="inoculationConfirmed"
              labelText={
                <span style={{ fontWeight: "600" }}>
                  <FormattedMessage
                    id="notebook.bacteriology.processing.inoculationConfirmed"
                    defaultMessage="Inoculation Confirmed"
                  />
                </span>
              }
              checked={processingData.inoculationConfirmed}
              onChange={(e, { checked }) =>
                setProcessingData((prev) => ({
                  ...prev,
                  inoculationConfirmed: checked,
                }))
              }
            />

            {processingData.inoculationConfirmed && (
              <Grid fullWidth style={{ marginTop: "0.75rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <DatePicker
                    datePickerType="single"
                    value={processingData.inoculationDate}
                    onChange={([date]) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        inoculationDate:
                          date?.toISOString().split("T")[0] || "",
                      }))
                    }
                  >
                    <DatePickerInput
                      id="inoculationDate"
                      labelText={intl.formatMessage({
                        id: "notebook.bacteriology.processing.inoculationDate",
                        defaultMessage: "Inoculation Date",
                      })}
                      placeholder="mm/dd/yyyy"
                    />
                  </DatePicker>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="inoculationBy"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.processing.performedBy",
                      defaultMessage: "Performed By",
                    })}
                    value={processingData.inoculationBy}
                    onChange={(e) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        inoculationBy: e.target.value,
                      }))
                    }
                  />
                </Column>
              </Grid>
            )}
          </div>

          {/* Incubation Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: processingData.incubationStarted
                ? "#fff1e0"
                : "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: `4px solid ${processingData.incubationStarted ? "#eb8000" : "#8d8d8d"}`,
            }}
          >
            <Checkbox
              id="incubationStarted"
              labelText={
                <span style={{ fontWeight: "600" }}>
                  <FormattedMessage
                    id="notebook.bacteriology.processing.incubationStarted"
                    defaultMessage="Incubation Started"
                  />
                </span>
              }
              checked={processingData.incubationStarted}
              onChange={(e, { checked }) =>
                setProcessingData((prev) => ({
                  ...prev,
                  incubationStarted: checked,
                }))
              }
            />

            {processingData.incubationStarted && (
              <Grid fullWidth style={{ marginTop: "0.75rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="incubationStartTime"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.processing.startTime",
                      defaultMessage: "Start Time",
                    })}
                    value={processingData.incubationStartTime}
                    onChange={(e) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        incubationStartTime: e.target.value,
                      }))
                    }
                    placeholder="HH:MM (e.g., 14:30)"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="expectedReadTime"
                    labelText={intl.formatMessage({
                      id: "notebook.bacteriology.processing.expectedReadTime",
                      defaultMessage: "Expected Read Time",
                    })}
                    value={processingData.expectedReadTime}
                    onChange={(e) =>
                      setProcessingData((prev) => ({
                        ...prev,
                        expectedReadTime: e.target.value,
                      }))
                    }
                    placeholder="e.g., Tomorrow 08:00"
                  />
                </Column>
              </Grid>
            )}
          </div>

          {/* QC Result Section */}
          <div
            style={{
              padding: "1rem",
              backgroundColor:
                processingData.qcResult === "PASS"
                  ? "#defbe6"
                  : processingData.qcResult === "FAIL"
                    ? "#fff1f1"
                    : "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: `4px solid ${processingData.qcResult === "PASS" ? "#24a148" : processingData.qcResult === "FAIL" ? "#da1e28" : "#8d8d8d"}`,
            }}
          >
            <h5 style={{ marginBottom: "0.75rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.processing.qcResult"
                defaultMessage="Quality Control Result"
              />
            </h5>

            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.bacteriology.processing.qcResultLabel",
                    defaultMessage: "QC Result",
                  })}
                  name="qcResult"
                  valueSelected={processingData.qcResult}
                  onChange={(value) =>
                    setProcessingData((prev) => ({ ...prev, qcResult: value }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton id="qc-pass" labelText="Pass" value="PASS" />
                  <RadioButton id="qc-fail" labelText="Fail" value="FAIL" />
                </RadioButtonGroup>
              </Column>

              {processingData.qcResult === "FAIL" && (
                <>
                  <Column lg={8} md={4} sm={4}>
                    <Dropdown
                      id="qcFailureReason"
                      titleText={intl.formatMessage({
                        id: "notebook.bacteriology.processing.failureReason",
                        defaultMessage: "Failure Reason",
                      })}
                      label="Select reason..."
                      items={QC_FAILURE_REASONS}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={QC_FAILURE_REASONS.find(
                        (r) => r.id === processingData.qcFailureReason,
                      )}
                      onChange={({ selectedItem }) =>
                        setProcessingData((prev) => ({
                          ...prev,
                          qcFailureReason: selectedItem?.id || "",
                        }))
                      }
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="retakeRequired"
                      labelText={intl.formatMessage({
                        id: "notebook.bacteriology.processing.retakeRequired",
                        defaultMessage: "Retake process required",
                      })}
                      checked={processingData.retakeRequired}
                      onChange={(e, { checked }) =>
                        setProcessingData((prev) => ({
                          ...prev,
                          retakeRequired: checked,
                        }))
                      }
                    />
                  </Column>
                </>
              )}
            </Grid>
          </div>

          {/* Notes */}
          <TextArea
            id="processingNotes"
            labelText={intl.formatMessage({
              id: "notebook.bacteriology.processing.notes",
              defaultMessage: "Notes",
            })}
            value={processingData.notes}
            onChange={(e) =>
              setProcessingData((prev) => ({
                ...prev,
                notes: e.target.value,
              }))
            }
            rows={2}
            placeholder="Additional observations, deviations, or comments..."
          />
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologyProcessingQCPage;
