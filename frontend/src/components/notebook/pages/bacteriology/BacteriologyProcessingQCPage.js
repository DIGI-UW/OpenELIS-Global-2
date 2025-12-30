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
  Modal,
  Tag,
  RadioButtonGroup,
  RadioButton,
  Checkbox,
  Tooltip,
} from "@carbon/react";
import {
  CheckmarkFilled,
  Renew,
  Chemistry,
  Microscope,
  Settings,
  Calendar,
  WarningAlt,
  Edit,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
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

// Media QC Failure Reasons
const MEDIA_QC_FAILURE_REASONS = [
  {
    id: "CONTAMINATION_DETECTED",
    text: "Contamination detected during sterility test",
  },
  { id: "NO_GROWTH_CONTROL", text: "No growth with positive control organism" },
  { id: "POOR_GROWTH_SUPPORT", text: "Poor growth support quality" },
  { id: "INADEQUATE_REACTIVITY", text: "Inadequate reactivity response" },
  { id: "INVALID_CONTROL", text: "Invalid control organism result" },
  { id: "PH_OUT_OF_RANGE", text: "pH out of acceptable range" },
  {
    id: "PHYSICAL_DEFECTS",
    text: "Physical defects (color, clarity, consistency)",
  },
  { id: "EQUIPMENT_MALFUNCTION", text: "Equipment malfunction during testing" },
  {
    id: "PROCEDURE_NOT_FOLLOWED",
    text: "Test procedure not properly followed",
  },
  { id: "EXPIRED_REAGENTS", text: "Expired or degraded reagents used" },
  { id: "ENVIRONMENTAL_CONDITIONS", text: "Improper environmental conditions" },
  { id: "OTHER_MEDIA_QC", text: "Other - specify in notes" },
];

// Control Organisms for QC Testing
const QC_TEST_TYPES = [
  { id: "sterility", text: "Sterility Test" },
  { id: "growth_support", text: "Growth Support Test" },
  { id: "physical_inspection", text: "Physical Inspection" },
];

// Helper Functions for QC Status Rendering
const getQCStatusTag = (status) => {
  const statusMap = {
    PENDING: { type: "gray", label: "Pending QC" },
    PASS: { type: "green", label: "Pass" },
    FAIL: { type: "red", label: "Fail" },
    PASSED: { type: "green", label: "Passed" },
    FAILED: { type: "red", label: "Failed" },
    QUARANTINED: { type: "magenta", label: "Quarantined" },
    TESTING: { type: "blue", label: "Testing" },
  };
  const config = statusMap[status] || statusMap.PENDING;
  return (
    <Tag type={config.type} size="sm">
      {config.label}
    </Tag>
  );
};

const getOverallQCStatus = (qcData) => {
  if (!qcData || typeof qcData !== "object") {
    return "PENDING";
  }

  // For simplified interface, use direct qcStatus field
  return qcData.qcStatus || "PENDING";
};

const getQCProgress = (qcData) => {
  if (!qcData || typeof qcData !== "object") {
    return 0;
  }

  // For simplified interface, progress based on completion
  const hasTestType = qcData.testType && qcData.testType !== "";
  const hasResult = qcData.qcStatus && qcData.qcStatus !== "PENDING";

  if (hasTestType && hasResult) return 100;
  if (hasTestType) return 50;
  return 0;
};

// QC Section Component for Media
const MediaQCSection = ({ mediaType, qcData = {}, onQCUpdate }) => {
  const intl = useIntl();

  // Provide default QC data structure for simplified interface
  const defaultQCData = {
    testType: "",
    qcStatus: "PENDING",
    qcFailureReason: "",
    qcNotes: "",
    qcPerformedBy: "",
    qcDate: "",
  };

  const safeQCData = { ...defaultQCData, ...qcData };
  const overallStatus = getOverallQCStatus(safeQCData);
  const progress = getQCProgress(safeQCData);

  const handleQCUpdate = (field, value) => {
    const updatedQC = {
      ...safeQCData,
      [field]: value,
    };

    // Update overall status based on test results
    updatedQC.qcStatus = value; // Direct status for simplified interface

    // Set date when test is completed
    if (value !== "PENDING") {
      updatedQC.qcDate = new Date().toISOString().split("T")[0];
    }

    onQCUpdate(updatedQC);
  };

  return (
    <div
      style={{
        padding: "1rem",
        backgroundColor:
          overallStatus === "PASSED"
            ? "#e5f6ff"
            : overallStatus === "FAILED"
              ? "#fff1f1"
              : "#f4f4f4",
        borderRadius: "4px",
        marginTop: "1rem",
        borderLeft:
          overallStatus === "PASSED"
            ? "4px solid #0f62fe"
            : overallStatus === "FAILED"
              ? "4px solid #da1e28"
              : "4px solid #8d8d8d",
      }}
    >
      <h6
        style={{
          marginBottom: "0.75rem",
          color:
            overallStatus === "PASSED"
              ? "#0f62fe"
              : overallStatus === "FAILED"
                ? "#da1e28"
                : "#525252",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <span>
          <FormattedMessage
            id={`media.qc.title.${mediaType.toLowerCase()}`}
            defaultMessage={`${mediaType} Quality Control`}
          />
        </span>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <span style={{ fontSize: "12px", color: "#525252" }}>
            {progress}% Complete
          </span>
          {getQCStatusTag(overallStatus)}
        </div>
      </h6>

      <Grid fullWidth>
        {/* Test Type Selection */}
        <Column lg={6} md={4} sm={4}>
          <Dropdown
            id={`testType_${mediaType}`}
            titleText={intl.formatMessage({
              id: "media.qc.test.type",
              defaultMessage: "Quality Control Test Type",
            })}
            label="Select test type..."
            items={QC_TEST_TYPES}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={QC_TEST_TYPES.find(
              (test) => test.id === safeQCData.testType,
            )}
            onChange={({ selectedItem }) => {
              handleQCUpdate("testType", selectedItem?.id || "");
            }}
          />
        </Column>

        {/* QC Result */}
        <Column lg={6} md={4} sm={4}>
          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                fontSize: "14px",
                fontWeight: "600",
                marginBottom: "0.5rem",
                display: "block",
              }}
            >
              <FormattedMessage
                id="media.qc.result"
                defaultMessage="Quality Control Result"
              />
            </label>
            <RadioButtonGroup
              name={`qcResult_${mediaType}`}
              value={safeQCData.qcStatus || "PENDING"}
              onChange={(value) => handleQCUpdate("qcStatus", value)}
            >
              <RadioButton value="PENDING" labelText="Pending" />
              <RadioButton value="PASSED" labelText="Pass" />
              <RadioButton value="FAILED" labelText="Fail" />
            </RadioButtonGroup>
          </div>
        </Column>
      </Grid>

      {/* Failure Reason and Notes - Show when any test fails */}
      {overallStatus === "FAILED" && (
        <Grid fullWidth style={{ marginTop: "1rem" }}>
          <Column lg={6} md={4} sm={4}>
            <Dropdown
              id={`failureReason_${mediaType}`}
              titleText={intl.formatMessage({
                id: "media.qc.failure.reason",
                defaultMessage: "QC Failure Reason",
              })}
              label="Select failure reason..."
              items={MEDIA_QC_FAILURE_REASONS}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={MEDIA_QC_FAILURE_REASONS.find(
                (reason) => reason.id === safeQCData.qcFailureReason,
              )}
              onChange={({ selectedItem }) => {
                onQCUpdate({
                  ...safeQCData,
                  qcFailureReason: selectedItem?.id || "",
                });
              }}
            />
          </Column>
          <Column lg={10} md={4} sm={4}>
            <TextArea
              id={`qcNotes_${mediaType}`}
              labelText={intl.formatMessage({
                id: "media.qc.notes",
                defaultMessage: "QC Notes and Observations",
              })}
              placeholder="Detailed notes about QC failure and corrective actions..."
              value={safeQCData.qcNotes}
              onChange={(e) => {
                onQCUpdate({
                  ...safeQCData,
                  qcNotes: e.target.value,
                });
              }}
              rows={3}
            />
          </Column>
        </Grid>
      )}

      {/* QC Status Warning */}
      {overallStatus === "FAILED" && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "media.qc.failure.warning.title",
            defaultMessage: "QC Failure",
          })}
          subtitle={intl.formatMessage({
            id: "media.qc.failure.warning.message",
            defaultMessage:
              "This media batch has failed QC and will be quarantined. It cannot be used for sample processing.",
          })}
          hideCloseButton
          lowContrast
          style={{ marginTop: "1rem" }}
        />
      )}

      {overallStatus === "PASSED" && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "media.qc.success.title",
            defaultMessage: "QC Passed",
          })}
          subtitle={intl.formatMessage({
            id: "media.qc.success.message",
            defaultMessage:
              "This media batch has passed all QC tests and is approved for use.",
          })}
          hideCloseButton
          lowContrast
          style={{ marginTop: "1rem" }}
        />
      )}
    </div>
  );
};

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
    // Culture Media QC - Object storing QC data for each media by ID
    cultureMediaQC: {
      // Format: [mediaId]: { sterilityTest: "PENDING", ... }
    },
    // Biochemical Media Selection
    biochemicalMedia: [],
    biochemicalMediaBatch: "",
    // Biochemical Media QC - Object storing QC data for each media by ID
    biochemicalMediaQC: {
      // Format: [mediaId]: { sterilityTest: "PENDING", reactivityTest: "PENDING", ... }
    },
    // Enrichment Media (optional)
    enrichmentMedia: "",
    enrichmentMediaBatch: "",
    // Enrichment Media QC
    enrichmentMediaQC: {
      sterilityTest: "PENDING",
      sterilityTestDate: "",
      selectivityTest: "PENDING", // PENDING, PASS, FAIL
      selectivityTestDate: "",
      controlOrganism: "",
      enrichmentTest: "PENDING", // PENDING, PASS, FAIL
      qcStatus: "PENDING",
      qcFailureReason: "",
      qcNotes: "",
      qcPerformedBy: "",
      qcDate: "",
    },
    // Incubation Settings
    incubationCondition: "",
    incubationDuration: "",
    customDuration: "",
    // Assignment metadata
    notes: "",
  });

  // ==========================================
  // QC Override Modal State
  // ==========================================
  const [qcOverrideModalOpen, setQcOverrideModalOpen] = useState(false);
  const [selectedQcOverrideSampleId, setSelectedQcOverrideSampleId] =
    useState(null);

  // State for QC outcome choice in preparation modal
  const [qcOutcomeChoice, setQcOutcomeChoice] = useState(null); // FAILED, CAUTION, PASSED, or null

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

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load preparation data from pageData when component mounts
  useEffect(() => {
    let dataRestored = false;

    if (pageData?.preparationData) {
      console.log("Restoring preparation data from pageData:", {
        hasQCData: !!(
          pageData.preparationData.cultureMediaQC ||
          pageData.preparationData.biochemicalMediaQC ||
          pageData.preparationData.enrichmentMediaQC
        ),
        cultureMediaQC: pageData.preparationData.cultureMediaQC,
        biochemicalMediaQC: pageData.preparationData.biochemicalMediaQC,
        enrichmentMediaQC: pageData.preparationData.enrichmentMediaQC,
      });
      setPreparationData((prev) => ({
        ...prev,
        ...pageData.preparationData,
      }));
      dataRestored = true;
    }

    // Also restore samples if they were saved
    if (pageData?.samples && Array.isArray(pageData.samples)) {
      console.log("Restoring saved samples:", pageData.samples);
      setSamples(pageData.samples);
      dataRestored = true;
    }

    // If no data was restored from backend, try localStorage fallback
    if (!dataRestored && hasRealPageId) {
      try {
        const localStorageKey = `notebook-page-${pageData.id}-data`;
        const savedData = localStorage.getItem(localStorageKey);
        if (savedData) {
          const parsedData = JSON.parse(savedData);
          console.log(
            "Restoring QC data from localStorage fallback:",
            parsedData,
          );

          if (parsedData.preparationData) {
            setPreparationData((prev) => ({
              ...prev,
              ...parsedData.preparationData,
            }));
          }

          if (parsedData.samples && Array.isArray(parsedData.samples)) {
            setSamples(parsedData.samples);
          }

          console.log("QC data successfully restored from localStorage");
        }
      } catch (error) {
        console.error("Failed to restore QC data from localStorage:", error);
      }
    }
  }, [
    pageData?.preparationData,
    pageData?.samples,
    pageData?.id,
    hasRealPageId,
  ]);

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

  // Save QC data and sample data to page level for persistence across navigation
  const saveQCDataToPage = useCallback(() => {
    if (!hasRealPageId) {
      return;
    }

    const pageQCData = {
      cultureMediaQC: preparationData.cultureMediaQC,
      biochemicalMediaQC: preparationData.biochemicalMediaQC,
      enrichmentMediaQC: preparationData.enrichmentMediaQC,
      // Also save the complete preparation data for persistence
      ...preparationData,
    };

    // Also save current sample states for persistence
    const saveData = {
      preparationData: pageQCData,
      samples: samples, // Save current sample states
      lastUpdated: Date.now(),
    };

    // QC data is now managed in React state only
  }, [preparationData, samples, hasRealPageId, pageData?.id]);

  // Auto-save QC data and samples with debounce
  useEffect(() => {
    if (!hasRealPageId) return;

    const timeoutId = setTimeout(() => {
      saveQCDataToPage();
    }, 1000); // 1 second debounce

    return () => clearTimeout(timeoutId);
  }, [preparationData, samples, saveQCDataToPage, hasRealPageId]);

  // Handle QC results locally (frontend-only)
  const submitQCLocally = useCallback(
    async (mediaId, mediaName, mediaType, qcTests) => {
      // Update the preparationData with the QC results
      setPreparationData((prev) => {
        let updateField;
        if (mediaType === "culture") {
          updateField = "cultureMediaQC";
        } else if (mediaType === "biochemical") {
          updateField = "biochemicalMediaQC";
        } else if (mediaType === "enrichment") {
          updateField = "enrichmentMediaQC";
        } else {
          console.error("Unknown media type:", mediaType);
          return prev;
        }

        return {
          ...prev,
          [updateField]: {
            ...prev[updateField],
            [mediaId]: qcTests,
          },
        };
      });

      setSuccess("QC results updated successfully");
    },
    [setSuccess],
  );

  // No filtering - show all samples like other pages
  const filteredSamples = samples;

  // Simple QC failure check - blocks assignment if any QC test fails
  const hasQCFailures = useMemo(() => {
    const qcData = [
      preparationData.cultureMediaQC,
      preparationData.biochemicalMediaQC,
      preparationData.enrichmentMediaQC,
    ].filter((qc) => qc?.qcStatus === "FAILED");

    return qcData.length > 0;
  }, [
    preparationData.cultureMediaQC,
    preparationData.biochemicalMediaQC,
    preparationData.enrichmentMediaQC,
  ]);

  // Get QC status for individual samples
  const getSampleQCStatus = useCallback(
    (sample) => {
      if (!sample.preparationAssigned) {
        return {
          status: "NOT_APPLICABLE",
          label: "N/A",
          details: "No preparation assigned",
        };
      }

      // Check QC status based on sample's assigned media
      const sampleMediaType = sample.cultureMedia;
      let qcData = null;

      // Find the QC data for the media type this sample is using
      if (preparationData.cultureMediaQC && sampleMediaType) {
        // Look for QC data that matches this sample's media
        const mediaEntries = Object.entries(
          preparationData.cultureMediaQC || {},
        );
        for (const [mediaId, qc] of mediaEntries) {
          // Find media info by mediaId to check if it matches sample's media
          const matchingMedia = CULTURE_MEDIA_TYPES.find(
            (m) => m.id === mediaId,
          );
          if (matchingMedia?.text === sampleMediaType) {
            qcData = qc;
            break;
          }
        }
      }

      // Also check biochemical media QC if no culture media QC found
      if (
        !qcData &&
        preparationData.biochemicalMediaQC &&
        sample.biochemicalMedia
      ) {
        const mediaEntries = Object.entries(
          preparationData.biochemicalMediaQC || {},
        );
        for (const [mediaId, qc] of mediaEntries) {
          const matchingMedia = BIOCHEMICAL_MEDIA_TYPES.find(
            (m) => m.id === mediaId,
          );
          if (matchingMedia?.text === sample.biochemicalMedia) {
            qcData = qc;
            break;
          }
        }
      }

      // Also check enrichment media QC if no other QC found
      if (!qcData && preparationData.enrichmentMediaQC) {
        qcData = preparationData.enrichmentMediaQC;
      }

      if (!qcData) {
        return {
          status: "PENDING",
          label: "QC Pending",
          details: "QC not performed for assigned media",
        };
      }

      // Check the QC status
      if (qcData.qcStatus === "FAILED") {
        const failureReason = qcData.qcFailureReason
          ? QC_FAILURE_REASONS.find((r) => r.id === qcData.qcFailureReason)
              ?.text || qcData.qcFailureReason
          : "Unknown reason";
        return {
          status: "FAILED",
          label: "QC Failed",
          details: `Failed: ${failureReason}`,
        };
      } else if (qcData.qcStatus === "PASSED") {
        return {
          status: "PASSED",
          label: "QC Passed",
          details: "QC test passed",
        };
      } else {
        return {
          status: "PENDING",
          label: "QC Pending",
          details: "QC test pending",
        };
      }
    },
    [
      preparationData.cultureMediaQC,
      preparationData.biochemicalMediaQC,
      preparationData.enrichmentMediaQC,
    ],
  );

  // Calculate simple stats like other pages
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
    const fullyComplete = samples.filter(
      (s) => s.preparationAssigned && s.processingStatus === "COMPLETED",
    ).length;

    return {
      total: samples.length,
      preparationAssigned,
      processed,
      fullyComplete,
    };
  }, [samples]);

  // Check if any selected samples have QC failures (used to disable action buttons)
  const selectedSamplesQCStatus = useMemo(() => {
    if (selectedIds.length === 0) {
      return { hasQCFailed: false, qcFailedCount: 0, qcFailedSamples: [] };
    }

    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const qcFailedSamples = selectedSamples.filter(
      (s) =>
        s.processingStatus === "QC_FAILED" ||
        s.qcResult === "FAIL" ||
        s.status === "REJECTED",
    );

    return {
      hasQCFailed: qcFailedSamples.length > 0,
      qcFailedCount: qcFailedSamples.length,
      qcFailedSamples: qcFailedSamples,
    };
  }, [selectedIds, samples]);

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
    setQcOutcomeChoice(null);
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

    // Check if any QC has failed for the selected media
    const hasAnyQCFailed = () => {
      // Check culture media QC
      for (const media of preparationData.cultureMedia) {
        const qc = preparationData.cultureMediaQC?.[media.id];
        if (qc?.qcStatus === "FAILED") return true;
      }
      // Check biochemical media QC
      for (const media of preparationData.biochemicalMedia) {
        const qc = preparationData.biochemicalMediaQC?.[media.id];
        if (qc?.qcStatus === "FAILED") return true;
      }
      // Check enrichment media QC
      if (
        preparationData.enrichmentMedia &&
        preparationData.enrichmentMediaQC?.qcStatus === "FAILED"
      ) {
        return true;
      }
      return false;
    };

    // Check if all QC has passed for the selected media
    const hasAllQCPassed = () => {
      // Check culture media QC - at least one must be selected and all must pass
      if (preparationData.cultureMedia.length === 0) return false;
      for (const media of preparationData.cultureMedia) {
        const qc = preparationData.cultureMediaQC?.[media.id];
        if (!qc || qc.qcStatus !== "PASSED") return false;
      }
      // Check biochemical media QC if any selected
      for (const media of preparationData.biochemicalMedia) {
        const qc = preparationData.biochemicalMediaQC?.[media.id];
        if (!qc || qc.qcStatus !== "PASSED") return false;
      }
      // Check enrichment media QC if selected
      if (preparationData.enrichmentMedia) {
        if (
          !preparationData.enrichmentMediaQC ||
          preparationData.enrichmentMediaQC.qcStatus !== "PASSED"
        ) {
          return false;
        }
      }
      return true;
    };

    const qcFailed = hasAnyQCFailed();
    const qcPassed = hasAllQCPassed();

    // Determine the QC result and processing status
    // If user selected a QC outcome choice, use that; otherwise use media QC results
    let qcResult = "PENDING";
    let processingStatus = "IN_PROGRESS";
    let sampleStatus = "IN_PROGRESS";

    if (qcOutcomeChoice) {
      // User explicitly chose an outcome
      if (qcOutcomeChoice === "FAILED") {
        qcResult = "FAIL";
        processingStatus = "QC_FAILED";
        sampleStatus = "REJECTED";
      } else if (qcOutcomeChoice === "CAUTION") {
        qcResult = "FAIL";
        processingStatus = "IN_PROGRESS";
        sampleStatus = "IN_PROGRESS";
      } else if (qcOutcomeChoice === "PASSED") {
        qcResult = "PASS";
        processingStatus = "QC_PASSED";
        sampleStatus = "COMPLETED";
      }
    } else {
      // No outcome choice - use media QC results
      if (qcFailed) {
        qcResult = "FAIL";
        processingStatus = "QC_FAILED";
        sampleStatus = "REJECTED";
      } else if (qcPassed) {
        qcResult = "PASS";
        processingStatus = "QC_PASSED";
        sampleStatus = "COMPLETED";
      }
    }

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
      // Include QC data
      cultureMediaQC: preparationData.cultureMediaQC,
      biochemicalMediaQC: preparationData.biochemicalMediaQC,
      enrichmentMediaQC: preparationData.enrichmentMediaQC,
      // Include QC result and processing status
      qcResult: qcResult,
      processingStatus: processingStatus,
      // Include the user's QC outcome choice for display purposes
      qcOutcomeChoice: qcOutcomeChoice || null,
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
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: sampleStatus,
              }),
              () => {
                let successMessage;
                if (qcOutcomeChoice === "FAILED") {
                  successMessage = intl.formatMessage(
                    {
                      id: "notebook.bacteriology.preparation.qcFailedOutcome",
                      defaultMessage:
                        "Preparation assigned to {count} samples. QC FAILED - samples rejected and must be retaken.",
                    },
                    { count: selectedIds.length },
                  );
                } else if (qcOutcomeChoice === "CAUTION") {
                  successMessage = intl.formatMessage(
                    {
                      id: "notebook.bacteriology.preparation.qcCautionOutcome",
                      defaultMessage:
                        "Preparation assigned to {count} samples. Proceeding with CAUTION - flagged for supervisor review.",
                    },
                    { count: selectedIds.length },
                  );
                } else if (qcOutcomeChoice === "PASSED") {
                  successMessage = intl.formatMessage(
                    {
                      id: "notebook.bacteriology.preparation.qcPassedOverride",
                      defaultMessage:
                        "Preparation assigned to {count} samples. QC OVERRIDDEN - proceeding to testing.",
                    },
                    { count: selectedIds.length },
                  );
                } else if (qcFailed) {
                  successMessage = intl.formatMessage(
                    {
                      id: "notebook.bacteriology.preparation.qcFailed",
                      defaultMessage:
                        "Preparation assigned to {count} samples. QC FAILED - samples marked for review.",
                    },
                    { count: selectedIds.length },
                  );
                } else if (qcPassed) {
                  successMessage = intl.formatMessage(
                    {
                      id: "notebook.bacteriology.preparation.qcPassed",
                      defaultMessage:
                        "Preparation assigned to {count} samples. QC PASSED - ready for next step.",
                    },
                    { count: selectedIds.length },
                  );
                } else {
                  successMessage = intl.formatMessage(
                    {
                      id: "notebook.bacteriology.preparation.saved",
                      defaultMessage:
                        "Preparation assigned to {count} samples.",
                    },
                    { count: selectedIds.length },
                  );
                }
                setSuccess(successMessage);
                setPreparationModalOpen(false);
                setQcOutcomeChoice(null);

                // Update local samples state immediately with new status
                setSamples((prevSamples) =>
                  prevSamples.map((sample) => {
                    if (selectedIds.includes(sample.id)) {
                      return {
                        ...sample,
                        status: sampleStatus,
                        preparationAssigned: true,
                        cultureMedia: preparationData.cultureMedia.map(
                          (m) => m.id,
                        ),
                        processingStatus: processingStatus,
                        qcResult: qcResult,
                      };
                    }
                    return sample;
                  }),
                );

                setSelectedIds([]);
                loadPageSamples();
                // Save QC data to page level
                saveQCDataToPage();
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
    const selectedSamples = filteredSamples.filter((s) =>
      selectedIds.includes(s.id),
    );
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

    // Check QC status for selected samples - exclude those with failed QC
    const samplesWithFailedQC = selectedSamples.filter((sample) => {
      const qcStatus = getSampleQCStatus(sample);
      return qcStatus.status === "FAILED";
    });

    if (samplesWithFailedQC.length === selectedSamples.length) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.processing.allSamplesFailedQC",
          defaultMessage:
            "All selected samples have failed QC and cannot be processed. Please resolve QC issues first.",
        }),
      );
      return;
    }

    if (samplesWithFailedQC.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.bacteriology.processing.someQCFailed",
            defaultMessage:
              "{count} sample(s) have failed QC and will be excluded from processing. Only samples with passed QC will be processed.",
          },
          { count: samplesWithFailedQC.length },
        ),
      );

      // Filter out failed QC samples from selection
      const validSampleIds = selectedSamples
        .filter((sample) => {
          const qcStatus = getSampleQCStatus(sample);
          return qcStatus.status !== "FAILED";
        })
        .map((sample) => sample.id);

      setSelectedIds(validSampleIds);

      // Continue with valid samples
      if (validSampleIds.length === 0) {
        return;
      }
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
  }, [selectedIds, filteredSamples, intl]);

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
    const selectedSamples = filteredSamples.filter((s) =>
      selectedIds.includes(s.id),
    );
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
    filteredSamples,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // ==========================================
  // QC Override Modal Handlers
  // ==========================================

  const handleOpenQcOverrideModal = useCallback((sampleId) => {
    setSelectedQcOverrideSampleId(sampleId);
    setQcOverrideModalOpen(true);
  }, []);

  const handleQcOverrideOption = useCallback(
    (option) => {
      if (!selectedQcOverrideSampleId) {
        setError("Sample not found. Please try again.");
        setQcOverrideModalOpen(false);
        return;
      }

      const numericId = parseInt(selectedQcOverrideSampleId, 10);
      const dataToSave = {};

      if (option === "FAILED") {
        // Keep as QC_FAILED - do nothing, just close modal
        setSuccess(
          intl.formatMessage({
            id: "notebook.bacteriology.processing.qcFailedConfirmed",
            defaultMessage:
              "Sample processing halted due to QC failure. Please retake sample.",
          }),
        );
        setQcOverrideModalOpen(false);
        return;
      } else if (option === "CAUTION") {
        // Continue with Caution - mark status as IN_PROGRESS/COMPLETED but keep warning
        dataToSave.processingStatus = "COMPLETED";
        dataToSave.qcOverride = "CAUTION";
        dataToSave.qcOverrideNote = "Continued with caution despite QC failure";
      } else if (option === "PASSED") {
        // Mark as QC Passed - allow to continue to next step
        dataToSave.processingStatus = "QC_PASSED";
        dataToSave.qcResult = "PASS";
        dataToSave.qcOverride = "PASSED";
        dataToSave.qcOverrideNote =
          "QC manually overridden to passed by user decision";
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: [numericId],
          data: dataToSave,
        }),
        (response) => {
          if (response && !response.error) {
            // Update status for PASSED override
            if (option === "PASSED") {
              postToOpenElisServer(
                `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                JSON.stringify({
                  sampleIds: [numericId],
                  status: "COMPLETED",
                }),
                () => {
                  setSuccess(
                    intl.formatMessage({
                      id: "notebook.bacteriology.processing.qcPassedOverride",
                      defaultMessage:
                        "QC status overridden to PASSED. Sample can proceed to testing.",
                    }),
                  );
                  loadPageSamples();
                  setQcOverrideModalOpen(false);
                  setSelectedQcOverrideSampleId(null);
                  if (onProgressUpdate) {
                    onProgressUpdate();
                  }
                },
              );
            } else if (option === "CAUTION") {
              setSuccess(
                intl.formatMessage({
                  id: "notebook.bacteriology.processing.continueCaution",
                  defaultMessage:
                    "Sample marked to continue with caution. Verify results carefully.",
                }),
              );
              loadPageSamples();
              setQcOverrideModalOpen(false);
              setSelectedQcOverrideSampleId(null);
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            }
          } else {
            setError("Failed to update QC status. Please try again.");
          }
        },
      );
    },
    [selectedQcOverrideSampleId, pageData?.id, loadPageSamples, intl],
  );

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
  // Note: SampleGrid calls render(value, sample) - sample is the second parameter
  const renderPreparationInfo = (value, sample) => {
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
  // Note: SampleGrid calls render(value, sample) - sample is the second parameter
  const renderProcessingInfo = (value, sample) => {
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

  // Custom status renderer that checks for QC failures
  // Note: SampleGrid calls render(value, sample) - sample is the second parameter
  const renderCustomStatus = useCallback(
    (value, sample) => {
      if (!sample) return null;

      // First check the regular sample status
      const status = sample.status || "PENDING";

      // Check if sample has QC result from processing status or qcResult field
      const processingStatus = sample.processingStatus;
      const qcResult = sample.qcResult;
      const qcOutcomeChoice = sample.qcOutcomeChoice;

      // If user chose "Continue with Caution", show orange caution tag
      if (qcOutcomeChoice === "CAUTION") {
        return (
          <Tag type="orange" size="sm">
            <FormattedMessage
              id="notebook.status.continueWithCaution"
              defaultMessage="Continue with Caution"
            />
          </Tag>
        );
      }

      // If user chose "QC Failed", show red failed tag
      if (qcOutcomeChoice === "FAILED") {
        return (
          <Tag type="red" size="sm">
            <FormattedMessage
              id="notebook.status.qcFailed"
              defaultMessage="QC Failed"
            />
          </Tag>
        );
      }

      // If user chose "Mark as QC Passed", show green passed tag
      if (qcOutcomeChoice === "PASSED") {
        return (
          <Tag type="green" size="sm">
            <FormattedMessage
              id="notebook.status.qcPassedPrepDone"
              defaultMessage="QC Passed - Prep Done"
            />
          </Tag>
        );
      }

      // If sample has explicit QC_FAILED processing status or FAIL qcResult, show QC Failed with option to override
      if (processingStatus === "QC_FAILED" || qcResult === "FAIL") {
        return (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "4px",
            }}
          >
            <Tag type="red" size="sm">
              <FormattedMessage
                id="notebook.status.qcFailed"
                defaultMessage="QC Failed"
              />
            </Tag>
            <Tooltip
              label={intl.formatMessage({
                id: "notebook.bacteriology.processing.qcFailedTooltip",
                defaultMessage: "Click to select QC outcome",
              })}
            >
              <button
                onClick={() => handleOpenQcOverrideModal(sample.id)}
                style={{
                  background: "none",
                  border: "none",
                  padding: "0 2px",
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  color: "#d32f2f",
                }}
                aria-label="QC options"
              >
                <Edit size={14} />
              </button>
            </Tooltip>
          </div>
        );
      }

      // If sample has explicit QC_PASSED processing status or PASS qcResult, show QC Passed - Preparation Done
      if (processingStatus === "QC_PASSED" || qcResult === "PASS") {
        return (
          <Tag type="green" size="sm">
            <FormattedMessage
              id="notebook.status.qcPassedPrepDone"
              defaultMessage="QC Passed - Prep Done"
            />
          </Tag>
        );
      }

      // Check QC status from preparationData if sample has preparation assigned
      if (
        sample.preparationAssigned &&
        (sample.cultureMedia || sample.biochemicalMedia)
      ) {
        const qcStatus = getSampleQCStatus(sample);
        if (qcStatus.status === "FAILED") {
          return (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "4px",
              }}
            >
              <Tag type="red" size="sm">
                <FormattedMessage
                  id="notebook.status.qcFailed"
                  defaultMessage="QC Failed"
                />
              </Tag>
              <Tooltip
                label={intl.formatMessage({
                  id: "notebook.bacteriology.processing.qcFailedTooltip",
                  defaultMessage: "Click to select QC outcome",
                })}
              >
                <button
                  onClick={() => handleOpenQcOverrideModal(sample.id)}
                  style={{
                    background: "none",
                    border: "none",
                    padding: "0 2px",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    color: "#d32f2f",
                  }}
                  aria-label="QC options"
                >
                  <Edit size={14} />
                </button>
              </Tooltip>
            </div>
          );
        }
        if (qcStatus.status === "PASSED") {
          return (
            <Tag type="green" size="sm">
              <FormattedMessage
                id="notebook.status.qcPassedPrepDone"
                defaultMessage="QC Passed - Prep Done"
              />
            </Tag>
          );
        }
      }

      // Otherwise use the regular status
      switch (status) {
        case "COMPLETED":
          return (
            <Tag type="green" size="sm">
              <FormattedMessage
                id="notebook.status.completed"
                defaultMessage="Completed"
              />
            </Tag>
          );
        case "REJECTED":
          return (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "4px",
              }}
            >
              <Tag type="red" size="sm">
                <FormattedMessage
                  id="notebook.status.qcFailed"
                  defaultMessage="QC Failed"
                />
              </Tag>
              <Tooltip
                label={intl.formatMessage({
                  id: "notebook.bacteriology.processing.qcFailedTooltip",
                  defaultMessage: "Click to select QC outcome",
                })}
              >
                <button
                  onClick={() => handleOpenQcOverrideModal(sample.id)}
                  style={{
                    background: "none",
                    border: "none",
                    padding: "0 2px",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    color: "#d32f2f",
                  }}
                  aria-label="QC options"
                >
                  <Edit size={14} />
                </button>
              </Tooltip>
            </div>
          );
        case "IN_PROGRESS":
          return (
            <Tag type="blue" size="sm">
              <FormattedMessage
                id="notebook.status.inProgress"
                defaultMessage="In Progress"
              />
            </Tag>
          );
        case "SKIPPED":
          return (
            <Tag type="gray" size="sm">
              <FormattedMessage
                id="notebook.status.skipped"
                defaultMessage="Skipped"
              />
            </Tag>
          );
        case "PENDING":
          return (
            <Tag type="gray" size="sm">
              <FormattedMessage
                id="notebook.status.pending"
                defaultMessage="Pending"
              />
            </Tag>
          );
        default:
          return (
            <Tag type="gray" size="sm">
              <FormattedMessage
                id="notebook.status.pending"
                defaultMessage="Pending"
              />
            </Tag>
          );
      }
    },
    [getSampleQCStatus],
  );

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
        <Tooltip
          align="bottom"
          label={
            selectedSamplesQCStatus.hasQCFailed
              ? intl.formatMessage(
                  {
                    id: "notebook.bacteriology.processing.qcFailedWarning",
                    defaultMessage:
                      "{count} selected sample(s) have failed QC. Proceeding with caution - these samples may require re-assignment of preparation with passing QC media.",
                  },
                  { count: selectedSamplesQCStatus.qcFailedCount },
                )
              : ""
          }
        >
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
        </Tooltip>

        {/* Quick QC Pass */}
        {selectedIds.length > 0 && (
          <Tooltip
            align="bottom"
            label={
              selectedSamplesQCStatus.hasQCFailed
                ? intl.formatMessage(
                    {
                      id: "notebook.bacteriology.processing.markQCPassedWarning",
                      defaultMessage:
                        "{count} selected sample(s) have failed QC. Consider re-assigning preparation with passing QC media, or click to override if appropriate.",
                    },
                    { count: selectedSamplesQCStatus.qcFailedCount },
                  )
                : ""
            }
          >
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={CheckmarkFilled}
              onClick={handleBulkMarkQCPassed}
              disabled={false}
            >
              <FormattedMessage
                id="notebook.bacteriology.processing.markQCPassed"
                defaultMessage="Mark QC Passed ({count})"
                values={{ count: selectedIds.length }}
              />
            </Button>
          </Tooltip>
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

        {/* Complete & Continue Button */}
        <Tooltip
          align="bottom"
          label={
            samples.some(
              (s) =>
                s.processingStatus === "QC_FAILED" ||
                s.qcResult === "FAIL" ||
                s.status === "REJECTED",
            )
              ? intl.formatMessage({
                  id: "notebook.bacteriology.processing.proceedWithCautionTooltip",
                  defaultMessage:
                    "Some samples have failed QC. You can proceed, but be aware that these samples may require attention.",
                })
              : ""
          }
        >
          <Button
            kind="primary"
            size="sm"
            onClick={() => {
              // Check for incomplete samples - this is now a warning only
              const incompleteCount = samples.filter(
                (s) =>
                  !s.preparationAssigned ||
                  s.processingStatus === "NOT_STARTED" ||
                  s.processingStatus === "PENDING",
              ).length;

              // Check for QC failed samples - now a warning, not blocking
              const qcFailedCount = samples.filter(
                (s) =>
                  s.processingStatus === "QC_FAILED" ||
                  s.qcResult === "FAIL" ||
                  s.status === "REJECTED",
              ).length;

              if (incompleteCount > 0) {
                setError(
                  intl.formatMessage(
                    {
                      id: "notebook.bacteriology.processing.incompleteError",
                      defaultMessage:
                        "{count} samples are not yet complete. Please ensure all samples have preparation assigned and processing completed before continuing.",
                    },
                    { count: incompleteCount },
                  ),
                );
              } else {
                // Allow proceeding even with QC failed samples, but show warning
                if (qcFailedCount > 0) {
                  setSuccess(
                    intl.formatMessage(
                      {
                        id: "notebook.bacteriology.processing.proceedWithCautionSuccess",
                        defaultMessage:
                          "Proceeding to next step. {count} sample(s) have failed QC - please review carefully.",
                      },
                      { count: qcFailedCount },
                    ),
                  );
                } else {
                  // All samples are complete with passing QC
                  setSuccess(
                    intl.formatMessage({
                      id: "notebook.bacteriology.processing.completeSuccess",
                      defaultMessage:
                        "All samples completed successfully. You can now proceed to the next step.",
                    }),
                  );
                }
              }
            }}
            disabled={samples.length === 0}
          >
            <FormattedMessage
              id="notebook.bacteriology.processing.completeAndContinue"
              defaultMessage="Complete & Continue"
            />
          </Button>
        </Tooltip>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          key={`bacteriology-processing-${JSON.stringify(preparationData.cultureMediaQC)}-${JSON.stringify(preparationData.biochemicalMediaQC)}-${JSON.stringify(preparationData.enrichmentMediaQC)}`}
          gridId="bacteriology-processing"
          samples={filteredSamples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            {
              key: "externalId",
              header: intl.formatMessage({
                id: "notebook.sample.externalId",
                defaultMessage: "External ID",
              }),
            },
            {
              key: "accessionNumber",
              header: intl.formatMessage({
                id: "notebook.sample.accessionNumber",
                defaultMessage: "Accession #",
              }),
            },
            {
              key: "sampleType",
              header: intl.formatMessage({
                id: "notebook.sample.type",
                defaultMessage: "Sample Type",
              }),
            },
            {
              key: "collectionDate",
              header: intl.formatMessage({
                id: "notebook.sample.collectionDate",
                defaultMessage: "Collection Date",
              }),
            },
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
            {
              key: "status",
              header: intl.formatMessage({
                id: "notebook.sample.status",
                defaultMessage: "Status",
              }),
              render: renderCustomStatus,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && filteredSamples.length === 0 && (
        <div className="empty-state">
          <p>
            {samples.length === 0 ? (
              <FormattedMessage
                id="notebook.bacteriology.processing.empty"
                defaultMessage="No samples available for processing. Please complete the temporary storage step first."
              />
            ) : (
              <FormattedMessage
                id="notebook.bacteriology.processing.allFiltered"
                defaultMessage="All samples are using media that has failed QC and are quarantined. Please resolve media QC failures to continue processing."
              />
            )}
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
        onRequestClose={() => {
          setPreparationModalOpen(false);
          setQcOutcomeChoice(null);
        }}
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

            {/* Culture Media QC Section - Individual QC for each selected media */}
            {preparationData.cultureMedia.length > 0 ? (
              preparationData.cultureMedia.map((media, index) => (
                <MediaQCSection
                  key={`culture-${media.id}-${index}`}
                  mediaType={media.text}
                  qcData={preparationData.cultureMediaQC?.[media.id] || {}}
                  onQCUpdate={(updatedQC) => {
                    setPreparationData((prev) => ({
                      ...prev,
                      cultureMediaQC: {
                        ...prev.cultureMediaQC,
                        [media.id]: updatedQC,
                      },
                    }));

                    // Submit QC results locally
                    submitQCLocally(media.id, media.text, "culture", updatedQC);
                  }}
                  showGrowthSupport={true}
                  showReactivity={false}
                  showSelectivity={false}
                  showEnrichment={false}
                  showPHVerification={false}
                />
              ))
            ) : (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  color: "#525252",
                  fontStyle: "italic",
                  backgroundColor: "#f9f9f9",
                  borderRadius: "4px",
                  border: "1px dashed #d0d0d0",
                  marginTop: "1rem",
                }}
              >
                <FormattedMessage
                  id="media.qc.empty.culture"
                  defaultMessage="Select culture media above to perform Quality Control testing"
                />
              </div>
            )}
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

            {/* Biochemical Media QC Section - Individual QC for each selected media */}
            {preparationData.biochemicalMedia.length > 0 ? (
              preparationData.biochemicalMedia.map((media, index) => (
                <MediaQCSection
                  key={`biochemical-${media.id}-${index}`}
                  mediaType={media.text}
                  qcData={preparationData.biochemicalMediaQC?.[media.id] || {}}
                  onQCUpdate={(updatedQC) => {
                    setPreparationData((prev) => ({
                      ...prev,
                      biochemicalMediaQC: {
                        ...prev.biochemicalMediaQC,
                        [media.id]: updatedQC,
                      },
                    }));

                    // Submit QC results locally
                    submitQCLocally(
                      media.id,
                      media.text,
                      "biochemical",
                      updatedQC,
                    );
                  }}
                  showGrowthSupport={false}
                  showReactivity={true}
                  showSelectivity={false}
                  showEnrichment={false}
                  showPHVerification={true}
                />
              ))
            ) : (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  color: "#525252",
                  fontStyle: "italic",
                  backgroundColor: "#f9f9f9",
                  borderRadius: "4px",
                  border: "1px dashed #d0d0d0",
                  marginTop: "1rem",
                }}
              >
                <FormattedMessage
                  id="media.qc.empty.biochemical"
                  defaultMessage="Select biochemical media above to perform Quality Control testing"
                />
              </div>
            )}
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

            {/* Enrichment Media QC Section - Individual QC for selected media */}
            {preparationData.enrichmentMedia ? (
              (() => {
                const selectedMedia = ENRICHMENT_MEDIA_TYPES.find(
                  (m) => m.id === preparationData.enrichmentMedia,
                );
                return selectedMedia ? (
                  <MediaQCSection
                    key={`enrichment-${selectedMedia.id}`}
                    mediaType={selectedMedia.text}
                    qcData={preparationData.enrichmentMediaQC || {}}
                    onQCUpdate={(updatedQC) => {
                      setPreparationData((prev) => ({
                        ...prev,
                        enrichmentMediaQC: updatedQC,
                      }));

                      // Submit QC results locally
                      submitQCLocally(
                        selectedMedia.id,
                        selectedMedia.text,
                        "enrichment",
                        updatedQC,
                      );
                    }}
                    showGrowthSupport={false}
                    showReactivity={false}
                    showSelectivity={true}
                    showEnrichment={true}
                    showPHVerification={false}
                  />
                ) : null;
              })()
            ) : (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  color: "#525252",
                  fontStyle: "italic",
                  backgroundColor: "#f9f9f9",
                  borderRadius: "4px",
                  border: "1px dashed #d0d0d0",
                  marginTop: "1rem",
                }}
              >
                <FormattedMessage
                  id="media.qc.empty.enrichment"
                  defaultMessage="Select enrichment media above to perform Quality Control testing"
                />
              </div>
            )}
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

          {/* QC Outcome Selection Buttons */}
          <div style={{ marginTop: "2rem" }}>
            <h5
              style={{
                marginBottom: "1rem",
                color: "#161616",
                fontWeight: "600",
              }}
            >
              <FormattedMessage
                id="notebook.bacteriology.preparation.qcOutcomeTitle"
                defaultMessage="QC Outcome (Optional)"
              />
            </h5>
            <p
              style={{
                fontSize: "12px",
                color: "#525252",
                marginBottom: "1rem",
              }}
            >
              <FormattedMessage
                id="notebook.bacteriology.preparation.qcOutcomeDescription"
                defaultMessage="If QC fails, choose how to proceed. If not selected, status will be determined by media QC results."
              />
            </p>

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.75rem",
              }}
            >
              {/* Option 1: QC Failed */}
              <button
                onClick={() => setQcOutcomeChoice("FAILED")}
                style={{
                  padding: "0.75rem",
                  border:
                    qcOutcomeChoice === "FAILED"
                      ? "3px solid #d32f2f"
                      : "2px solid #d32f2f",
                  backgroundColor:
                    qcOutcomeChoice === "FAILED" ? "#ffebee" : "#fff",
                  borderRadius: "4px",
                  cursor: "pointer",
                  textAlign: "left",
                  transition: "all 0.2s",
                  fontWeight: qcOutcomeChoice === "FAILED" ? "600" : "400",
                }}
              >
                <div
                  style={{
                    color: "#d32f2f",
                    fontWeight: "600",
                    marginBottom: "0.25rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.preparation.qcFailedOutcome"
                    defaultMessage="QC Failed"
                  />
                </div>
                <div style={{ fontSize: "11px", color: "#525252" }}>
                  <FormattedMessage
                    id="notebook.bacteriology.preparation.qcFailedOutcomeDesc"
                    defaultMessage="Stop processing. Sample must be retaken."
                  />
                </div>
              </button>

              {/* Option 2: Continue with Caution */}
              <button
                onClick={() => setQcOutcomeChoice("CAUTION")}
                style={{
                  padding: "0.75rem",
                  border:
                    qcOutcomeChoice === "CAUTION"
                      ? "3px solid #f57f17"
                      : "2px solid #f57f17",
                  backgroundColor:
                    qcOutcomeChoice === "CAUTION" ? "#fff3e0" : "#fff",
                  borderRadius: "4px",
                  cursor: "pointer",
                  textAlign: "left",
                  transition: "all 0.2s",
                  fontWeight: qcOutcomeChoice === "CAUTION" ? "600" : "400",
                }}
              >
                <div
                  style={{
                    color: "#f57f17",
                    fontWeight: "600",
                    marginBottom: "0.25rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.preparation.qcCautionOutcome"
                    defaultMessage="Continue with Caution"
                  />
                </div>
                <div style={{ fontSize: "11px", color: "#525252" }}>
                  <FormattedMessage
                    id="notebook.bacteriology.preparation.qcCautionOutcomeDesc"
                    defaultMessage="Proceed to testing, flag for supervisor review."
                  />
                </div>
              </button>

              {/* Option 3: Mark as QC Passed */}
              <button
                onClick={() => setQcOutcomeChoice("PASSED")}
                style={{
                  padding: "0.75rem",
                  border:
                    qcOutcomeChoice === "PASSED"
                      ? "3px solid #2e7d32"
                      : "2px solid #2e7d32",
                  backgroundColor:
                    qcOutcomeChoice === "PASSED" ? "#e8f5e9" : "#fff",
                  borderRadius: "4px",
                  cursor: "pointer",
                  textAlign: "left",
                  transition: "all 0.2s",
                  fontWeight: qcOutcomeChoice === "PASSED" ? "600" : "400",
                }}
              >
                <div
                  style={{
                    color: "#2e7d32",
                    fontWeight: "600",
                    marginBottom: "0.25rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.bacteriology.preparation.qcPassedOutcome"
                    defaultMessage="Mark as QC Passed"
                  />
                </div>
                <div style={{ fontSize: "11px", color: "#525252" }}>
                  <FormattedMessage
                    id="notebook.bacteriology.preparation.qcPassedOutcomeDesc"
                    defaultMessage="Override QC failure. Proceed to testing."
                  />
                </div>
              </button>
            </div>
          </div>
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

      {/* ==========================================
          MODAL - QC Override Options
          Allows supervisor to choose action when QC fails
          ========================================== */}
      <Modal
        open={qcOverrideModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.processing.qcOverrideTitle",
          defaultMessage: "QC Failure - Choose Action",
        })}
        onRequestClose={() => setQcOverrideModalOpen(false)}
        size="sm"
        danger
      >
        <div style={{ marginBottom: "2rem" }}>
          <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
            <FormattedMessage
              id="notebook.bacteriology.processing.qcOverrideDescription"
              defaultMessage="The preparation has failed QC checks. Choose how to proceed:"
            />
          </p>

          {/* QC Options */}
          <div
            style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
          >
            {/* Option 1: Keep QC Failed */}
            <button
              onClick={() => handleQcOverrideOption("FAILED")}
              style={{
                padding: "1rem",
                border: "2px solid #d32f2f",
                backgroundColor: "#fff",
                borderRadius: "4px",
                cursor: "pointer",
                textAlign: "left",
                transition: "all 0.2s",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = "#ffebee";
                e.currentTarget.style.borderColor = "#c62828";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = "#fff";
                e.currentTarget.style.borderColor = "#d32f2f";
              }}
            >
              <div
                style={{
                  fontWeight: "600",
                  color: "#d32f2f",
                  marginBottom: "0.5rem",
                }}
              >
                <FormattedMessage
                  id="notebook.bacteriology.processing.qcFailedOption"
                  defaultMessage="QC Failed"
                />
              </div>
              <div style={{ fontSize: "12px", color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.processing.qcFailedDescription"
                  defaultMessage="Stop processing. Sample must be retaken and reprocessed."
                />
              </div>
            </button>

            {/* Option 2: Continue with Caution */}
            <button
              onClick={() => handleQcOverrideOption("CAUTION")}
              style={{
                padding: "1rem",
                border: "2px solid #f57f17",
                backgroundColor: "#fff",
                borderRadius: "4px",
                cursor: "pointer",
                textAlign: "left",
                transition: "all 0.2s",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = "#fff3e0";
                e.currentTarget.style.borderColor = "#e65100";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = "#fff";
                e.currentTarget.style.borderColor = "#f57f17";
              }}
            >
              <div
                style={{
                  fontWeight: "600",
                  color: "#f57f17",
                  marginBottom: "0.5rem",
                }}
              >
                <FormattedMessage
                  id="notebook.bacteriology.processing.continueWithCautionOption"
                  defaultMessage="Continue with Caution"
                />
              </div>
              <div style={{ fontSize: "12px", color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.processing.continueWithCautionDescription"
                  defaultMessage="Proceed to testing, but flag for supervisor review. Results require verification."
                />
              </div>
            </button>

            {/* Option 3: Mark as QC Passed */}
            <button
              onClick={() => handleQcOverrideOption("PASSED")}
              style={{
                padding: "1rem",
                border: "2px solid #2e7d32",
                backgroundColor: "#fff",
                borderRadius: "4px",
                cursor: "pointer",
                textAlign: "left",
                transition: "all 0.2s",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = "#e8f5e9";
                e.currentTarget.style.borderColor = "#1b5e20";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = "#fff";
                e.currentTarget.style.borderColor = "#2e7d32";
              }}
            >
              <div
                style={{
                  fontWeight: "600",
                  color: "#2e7d32",
                  marginBottom: "0.5rem",
                }}
              >
                <FormattedMessage
                  id="notebook.bacteriology.processing.markQcPassedOption"
                  defaultMessage="Mark as QC Passed"
                />
              </div>
              <div style={{ fontSize: "12px", color: "#525252" }}>
                <FormattedMessage
                  id="notebook.bacteriology.processing.markQcPassedDescription"
                  defaultMessage="Override QC failure. Sample proceeds to testing. Document reason in notes."
                />
              </div>
            </button>
          </div>

          {/* Warning */}
          <div
            style={{
              marginTop: "1.5rem",
              padding: "0.75rem",
              backgroundColor: "#fff3cd",
              border: "1px solid #ffc107",
              borderRadius: "4px",
              fontSize: "12px",
              color: "#664d03",
            }}
          >
            <strong>Warning:</strong> Overriding QC failures may compromise
            sample integrity and result quality. Ensure appropriate supervisor
            authorization and documentation.
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologyProcessingQCPage;
