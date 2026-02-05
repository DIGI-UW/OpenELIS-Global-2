import React, { useState, useCallback, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  Button,
  Loading,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Form,
  FormGroup,
  NumberInput,
  DatePicker,
  DatePickerInput,
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Tag,
  DataTable,
  TableContainer,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../../config.json";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useBioanalyticalPermissions } from "../../../../hooks/useBioanalyticalPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "./BioanalyticalPages.css";

/**
 * Analytical methods available for bioanalytical testing as per SRS Stage 2 requirements
 */
const ANALYTICAL_METHODS = [
  {
    id: "HPLC_UV_VIS",
    name: "HPLC / UV-Vis",
    description:
      "High Performance Liquid Chromatography with UV-Visible detection",
    applications: ["Drug concentration", "Assay", "Content uniformity"],
    suitableFor: ["API", "Tablet", "Capsule", "Suspension"],
    preparationSteps: [
      "Obtain pharmaceutical sample (tablet, capsule, or API)",
      "Record sample weight/volume and appearance",
      "Prepare standard solution using reference standard",
      "Prepare sample solution by dissolution/extraction as per USP method",
      "Filter solution through 0.45 µm PTFE filter",
      "Prepare mobile phase as specified",
      "Record solution preparation time and analyst",
      "Label all solutions with date, time, and analyst initials",
      "Document instrument configuration and calibration status",
    ],
    controlRequirements: [
      { type: "POSITIVE", count: 2, frequency: "PER_RUN", description: "Reference standard solution" },
      { type: "NEGATIVE", count: 1, frequency: "PER_RUN", description: "Solvent blank" },
      { type: "QC_LOW", count: 1, frequency: "PER_RUN", description: "Low concentration QC (20-30% of nominal)" },
      { type: "QC_MEDIUM", count: 1, frequency: "PER_RUN", description: "Medium concentration QC (50% of nominal)" },
      { type: "QC_HIGH", count: 1, frequency: "PER_RUN", description: "High concentration QC (150% of nominal)" }
    ],
  },
  {
    id: "LC_MS_MS",
    name: "LC-MS/MS",
    description: "Liquid Chromatography with Tandem Mass Spectrometry",
    applications: [
      "Pharmacokinetic analysis",
      "Bioanalytical testing",
      "Metabolite identification",
    ],
    suitableFor: ["Plasma", "Serum", "Urine", "Whole Blood", "API"],
    preparationSteps: [
      "Obtain biological sample (plasma/serum/urine) from biorepository",
      "Verify sample integrity and stability upon thawing",
      "Record sample identification and collection timepoint",
      "Add appropriate internal standard to sample",
      "Perform liquid-liquid extraction (LLE) or solid-phase extraction (SPE)",
      "Evaporate to dryness and reconstitute in LC-MS mobile phase",
      "Filter through 0.2 µm nylon filter",
      "Prepare calibration standard curve (typically 6-8 point curve)",
      "Prepare quality control samples (Low, Medium, High)",
      "Document all preparation conditions and lot numbers",
    ],
    controlRequirements: [
      { type: "POSITIVE", count: 3, frequency: "PER_RUN", description: "Calibration standards (Low, Med, High)" },
      { type: "NEGATIVE", count: 2, frequency: "PER_RUN", description: "Blank matrix (plasma/serum)" },
      { type: "QC_LOW", count: 2, frequency: "PER_RUN", description: "QC Low (3x LLOQ)" },
      { type: "QC_MEDIUM", count: 2, frequency: "PER_RUN", description: "QC Medium (mid-range)" },
      { type: "QC_HIGH", count: 2, frequency: "PER_RUN", description: "QC High (80% of ULOQ)" },
      { type: "BLANK", count: 1, frequency: "PER_RUN", description: "Double blank (no IS, no analyte)" }
    ],
  },
  {
    id: "DISSOLUTION_USP",
    name: "Dissolution (USP I/II)",
    description:
      "Dissolution testing using USP Apparatus I (Basket) or II (Paddle)",
    applications: [
      "Pharmaceutical quality testing",
      "Release profile",
      "Bioequivalence",
    ],
    suitableFor: ["Tablet", "Capsule", "Suspension"],
    preparationSteps: [
      "Obtain pharmaceutical dosage forms (tablets/capsules)",
      "Record sample appearance and weight",
      "Prepare dissolution medium as per USP specification",
      "Equilibrate dissolution medium to 37°C ± 0.5°C",
      "Calibrate UV spectrophotometer if using spectroscopic detection",
      "Place apparatus vessels in dissolution bath",
      "Place samples in baskets (Apparatus I) or on paddles (Apparatus II)",
      "Start rotation at specified RPM (typically 50-100 RPM)",
      "Record sampling timepoints (typically 15, 30, 45, 60 minutes)",
      "Prepare sampling solutions and document collection times",
    ],
    controlRequirements: [
      { type: "POSITIVE", count: 1, frequency: "PER_BATCH", description: "Reference tablet (certified standard)" },
      { type: "QC_MEDIUM", count: 1, frequency: "PER_RUN", description: "System suitability tablet" },
      { type: "BLANK", count: 1, frequency: "PER_RUN", description: "Dissolution medium blank" }
    ],
  },
  {
    id: "PHYSICAL_TESTING",
    name: "Hardness / Friability / Disintegration Test",
    description: "Physical testing for pharmaceutical dosage forms",
    applications: ["Physical testing", "Quality control", "Batch release"],
    suitableFor: ["Tablet", "Capsule"],
    preparationSteps: [
      "Obtain tablet or capsule samples (minimum 10 units per test)",
      "Inspect samples visually for defects and record appearance",
      "Measure and record tablet dimensions if applicable",
      "Calibrate hardness tester using reference tablets",
      "For friability: Weigh sample tablets (6-20 tablets, total weight ~6.5 g)",
      "Place tablets in friability apparatus and rotate for 4 minutes at 25 RPM",
      "Reweigh friability sample and calculate % weight loss",
      "For disintegration: Place tablets in disintegration apparatus",
      "Conduct test in deionized water at 37°C ± 2°C for 30 minutes",
      "Record results and document any deviations from specifications",
    ],
    controlRequirements: [
      { type: "POSITIVE", count: 1, frequency: "PER_BATCH", description: "Reference tablets (hardness ~50N)" },
      { type: "QC_MEDIUM", count: 1, frequency: "PER_TEST", description: "Calibration tablet for hardness" }
    ],
  },
  {
    id: "IDENTITY_TEST",
    name: "Identity Test",
    description: "Verification of pharmaceutical substances and products",
    applications: [
      "Identity verification",
      "Raw material testing",
      "QC testing",
    ],
    suitableFor: [
      "API",
      "Tablet",
      "Capsule",
      "Suspension",
      "Reference Standard",
      "Excipient",
    ],
    preparationSteps: [
      "Obtain sample from batch to be tested",
      "Verify sample integrity and authenticity markings",
      "Record sample lot number and receipt date",
      "Prepare reference standard solution if using chromatographic method",
      "Prepare sample solution at appropriate concentration",
      "Select appropriate analytical method (IR, HPLC, TLC, or Mass Spec)",
      "For chromatographic methods: Prepare mobile phase and validate system",
      "Inject reference standard and document retention time",
      "Inject sample solution and compare with reference",
      "Document all analytical parameters and calibration data",
    ],
    controlRequirements: [
      { type: "POSITIVE", count: 1, frequency: "PER_RUN", description: "Certified reference standard" },
      { type: "NEGATIVE", count: 1, frequency: "PER_RUN", description: "Solvent/excipient blank" },
      { type: "QC_MEDIUM", count: 1, frequency: "PER_BATCH", description: "System suitability test" }
    ],
  },
];

/**
 * Bioanalytical Control Sample Repository - Pre-defined reference standards
 * Following OpenELIS patterns from virology, TB, and MedLab modules
 * Stored in sample.data.controlRepository and persisted via bulk endpoints
 */
const BIOANALYTICAL_CONTROL_STANDARDS = [
  // HPLC Controls
  {
    id: "HPLC_POS_001",
    name: "HPLC Positive Control Standard",
    type: "POSITIVE",
    methods: ["HPLC_UV_VIS"],
    description: "Certified reference standard for HPLC UV-VIS analysis",
    supplier: "USP Reference Standards",
    catalogNumber: "USP-RS-001",
    concentration: { value: 100.0, unit: "mg/L" },
    acceptanceCriteria: { min: 95.0, max: 105.0, unit: "%" },
    storageConditions: "Store at 2-8°C, protect from light",
    shelfLife: { value: 24, unit: "months" },
    qcLevel: "MEDIUM",
    certificateOfAnalysis: "COA-HPLC-001.pdf"
  },
  {
    id: "HPLC_NEG_001",
    name: "HPLC Negative Control (Blank)",
    type: "NEGATIVE",
    methods: ["HPLC_UV_VIS"],
    description: "Solvent blank for baseline verification",
    supplier: "In-house preparation",
    catalogNumber: "IH-BLANK-001",
    concentration: { value: 0.0, unit: "mg/L" },
    acceptanceCriteria: { max: 0.1, unit: "% of LOQ" },
    storageConditions: "Store at room temperature",
    shelfLife: { value: 1, unit: "week" },
    qcLevel: "LOW",
    preparationSOP: "SOP-BLANK-PREP-001"
  },

  // LC-MS/MS Controls
  {
    id: "LCMS_POS_001",
    name: "LC-MS/MS Internal Standard",
    type: "POSITIVE",
    methods: ["LC_MS_MS"],
    description: "Deuterated internal standard for LC-MS/MS quantification",
    supplier: "Cambridge Isotope Laboratories",
    catalogNumber: "DLM-1234-1",
    concentration: { value: 500.0, unit: "ng/mL" },
    acceptanceCriteria: { min: 90.0, max: 110.0, unit: "%" },
    storageConditions: "Store at -20°C",
    shelfLife: { value: 12, unit: "months" },
    qcLevel: "HIGH",
    mzRatio: { precursor: 285.2, product: 158.1 }
  },
  {
    id: "LCMS_NEG_001",
    name: "LC-MS/MS Matrix Blank",
    type: "NEGATIVE",
    methods: ["LC_MS_MS"],
    description: "Drug-free biological matrix for interference assessment",
    supplier: "BioIVT",
    catalogNumber: "BF-PLASMA-001",
    concentration: { value: 0.0, unit: "ng/mL" },
    acceptanceCriteria: { max: 20.0, unit: "% of LLOQ" },
    storageConditions: "Store at -70°C",
    shelfLife: { value: 6, unit: "months" },
    qcLevel: "LOW",
    matrixType: "Human plasma"
  },

  // Dissolution Controls
  {
    id: "DISS_POS_001",
    name: "Dissolution Reference Tablet",
    type: "POSITIVE",
    methods: ["DISSOLUTION_USP"],
    description: "USP Dissolution Calibrator Tablets",
    supplier: "USP",
    catalogNumber: "USP-DCT-001",
    concentration: { value: 10.0, unit: "mg per tablet" },
    acceptanceCriteria: { min: 85.0, max: 115.0, unit: "% dissolved at 30 min" },
    storageConditions: "Store in original container, desiccant present",
    shelfLife: { value: 60, unit: "months" },
    qcLevel: "MEDIUM",
    dissolutionMedium: "pH 6.8 phosphate buffer",
    apparatus: "USP Apparatus 2 (paddle)"
  },

  // Physical Testing Controls
  {
    id: "PHYS_POS_001",
    name: "Hardness Reference Standard",
    type: "POSITIVE",
    methods: ["PHYSICAL_TESTING"],
    description: "Calibrated reference tablets for hardness testing",
    supplier: "Pharma Test",
    catalogNumber: "PT-HT-REF-50N",
    concentration: { value: 50.0, unit: "N" },
    acceptanceCriteria: { min: 47.5, max: 52.5, unit: "N" },
    storageConditions: "Store at ambient conditions",
    shelfLife: { value: 12, unit: "months" },
    qcLevel: "LOW",
    calibrationCertificate: "CERT-HT-2024-001"
  },

  // Identity Test Controls
  {
    id: "ID_POS_001",
    name: "Identity Reference Standard",
    type: "POSITIVE",
    methods: ["IDENTITY_TEST"],
    description: "Authenticated reference material for identity confirmation",
    supplier: "European Pharmacopoeia",
    catalogNumber: "EP-REF-001",
    concentration: { value: 99.8, unit: "% purity" },
    acceptanceCriteria: { min: 99.0, max: 100.5, unit: "% identity match" },
    storageConditions: "Store in original vial, desiccated",
    shelfLife: { value: 36, unit: "months" },
    qcLevel: "HIGH",
    identityMethods: ["IR", "HPLC", "MS"]
  },
  {
    id: "ID_NEG_001",
    name: "Identity Negative Control",
    type: "NEGATIVE",
    methods: ["IDENTITY_TEST"],
    description: "Different compound for specificity verification",
    supplier: "Sigma-Aldrich",
    catalogNumber: "SA-ALT-001",
    concentration: { value: 99.5, unit: "% purity" },
    acceptanceCriteria: { max: 5.0, unit: "% cross-reactivity" },
    storageConditions: "Store at room temperature",
    shelfLife: { value: 24, unit: "months" },
    qcLevel: "MEDIUM",
    structuralSimilarity: "Different functional groups"
  }
];

/**
 * Staff roles for test assignment per SRS requirements
 */
const STAFF_ROLES = [
  {
    id: "CHEMICAL_ANALYST",
    name: "Chemical Analyst",
    specialties: ["HPLC_UV_VIS", "LC_MS_MS", "IDENTITY_TEST"],
    description: "Specialist in analytical chemistry and instrumentation",
  },
  {
    id: "PHARMACIST",
    name: "Pharmacist",
    specialties: ["DISSOLUTION_USP", "PHYSICAL_TESTING", "IDENTITY_TEST"],
    description: "Licensed pharmacist for pharmaceutical testing",
  },
  {
    id: "RESEARCHER",
    name: "Researcher",
    specialties: ["LC_MS_MS", "HPLC_UV_VIS", "DISSOLUTION_USP"],
    description: "Research scientist for method development",
  },
];

/**
 * BioanalyticalTestAssignmentPage - Stage 2 of bioanalytical workflow.
 *
 * Features:
 * - Sample selection for test assignment
 * - Analytical method selection (5 SRS methods)
 * - Staff responsibility allocation (Chemical Analysts, Pharmacists, Researchers)
 * - QC level configuration (Low, Medium, High)
 * - Sample preparation documentation
 * - Test assignment persistence via backend API
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 * @param {Array} props.templateInstruments - Available instruments
 */
function BioanalyticalTestAssignmentPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { hasAnyRole } = usePermissions();
  const {
    getPagePermissionLevel,
    canSaveData,
    hasFullControl,
    canAccessTestAssignment,
    BIOANALYTICAL_ROLES,
  } = useBioanalyticalPermissions();

  // PAGE 2 allowed roles per test.pdf Section 11
  const allowedRoles = [
    BIOANALYTICAL_ROLES.CHEMICAL_ANALYST,
    BIOANALYTICAL_ROLES.PHARMACIST,
    BIOANALYTICAL_ROLES.LAB_SUPERVISOR,
    BIOANALYTICAL_ROLES.STUDY_DIRECTOR,
    BIOANALYTICAL_ROLES.QA_OFFICER,
    BIOANALYTICAL_ROLES.RESEARCHER,
  ];

  const canAccessPage = canAccessTestAssignment();

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel("Test Assignment");
  const canAssignTests = hasFullControl(pagePermissionLevel);
  const canSaveAssignments = canSaveData(pagePermissionLevel);

  // Debug logging for permission issues
  if (process.env.NODE_ENV === "development") {
    console.log("Test Assignment Page - Permission Debug:", {
      pagePermissionLevel,
      canSaveAssignments,
      canAssignTests,
    });
  }

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Test Assignment & Preparation"
        reason="This page requires specific bioanalytical laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Loading and data states
  const [isLoading, setIsLoading] = useState(false);
  const [isAssigning, setIsAssigning] = useState(false);
  const [isAdvancing, setIsAdvancing] = useState(false);
  const [notebookId, setNotebookId] = useState(null);
  const [samples, setSamples] = useState([]);
  const [selectedSamples, setSelectedSamples] = useState(new Set());
  const [testAssignments, setTestAssignments] = useState({});

  // Form states for test assignment configuration
  const [assignmentConfig, setAssignmentConfig] = useState({
    analyticalMethod: "",
    assignedStaff: "",
    instrumentId: "",
    qcLevels: {
      low: { concentration: "5", tolerance: "20" },
      medium: { concentration: "50", tolerance: "20" },
      high: { concentration: "500", tolerance: "20" },
    },
    acceptanceCriteria: {
      rSquaredMin: "0.995",
      slopeRange: { min: "0.8", max: "1.2" },
      interceptMax: "20",
    },
    samplePreparation: "",
    expectedAnalysisDate: "",
    notes: "",
  });

  // UI states
  const [showAssignmentForm, setShowAssignmentForm] = useState(false);

  // Render QC Status for Stage 2 sample table
  const renderStage2QCStatus = useCallback((sample) => {
    // Check for reception QC from Stage 1
    if (sample.data?.receptionQC || sample.receptionQC) {
      const receptionQC = sample.data?.receptionQC || sample.receptionQC;
      if (receptionQC.qcPerformed) {
        return (
          <Tag
            type={receptionQC.qcPassed ? "green" : "red"}
            size="sm"
            title={`Reception QC ${receptionQC.overallStatus} - ${receptionQC.passedChecks}/${receptionQC.totalChecks} checks`}
          >
            RECEPTION {receptionQC.overallStatus}
          </Tag>
        );
      }
    }

    // Check if sample has any QC data from downstream stages
    if (sample.data?.qcApproved) {
      return (
        <Tag type="green" size="sm" title="QC approved in downstream stage">
          QC APPROVED
        </Tag>
      );
    }

    // Default state for Stage 2 (before QC verification)
    return (
      <Tag type="gray" size="sm" title="Reception QC pending - complete Stage 1 first">
        QC PENDING
      </Tag>
    );
  }, []);

  // Render Control Type for Stage 2 sample table
  const renderStage2ControlType = useCallback((sample) => {
    // Safety check for undefined sample
    if (!sample || typeof sample !== 'object') {
      return (
        <Tag type="gray" size="sm" title="No classification">
          Study Sample
        </Tag>
      );
    }

    // Check for control classification data from Stage 1
    const classification = sample.sampleClassification;

    if (!classification || !classification.isControlSample) {
      return (
        <Tag type="gray" size="sm" title="Regular study sample">
          Study Sample
        </Tag>
      );
    }

    // Determine tag type and text based on control type
    const getControlTypeDisplay = () => {
      switch (classification.type) {
        case "POSITIVE_CONTROL":
          return { type: "green", text: "Positive Ctrl", title: `Positive Control - ${classification.controlType || 'Generic'}` };
        case "NEGATIVE_CONTROL":
          return { type: "red", text: "Negative Ctrl", title: `Negative Control - ${classification.controlType || 'Generic'}` };
        case "QC_SAMPLE":
          return { type: "blue", text: "QC Sample", title: `QC Sample - ${classification.controlType || 'Generic'}` };
        case "BLANK":
          return { type: "purple", text: "Blank", title: `Blank Control - ${classification.controlType || 'Generic'}` };
        default:
          return { type: "cyan", text: "Control", title: `Control Sample - ${classification.type}` };
      }
    };

    const { type, text, title } = getControlTypeDisplay();

    return (
      <Tag type={type} size="sm" title={title}>
        {text}
      </Tag>
    );
  }, []);

  // Notification helper
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // Fetch Stage 1 sample data for reference (to display sampleType, requestedTests, etc.)
  // Stage 1 is the Sample Reception & Registration page (usually the first page)
  const fetchStage1DataForSamples = useCallback(
    async (sampleItemIds) => {
      if (!entryId || sampleItemIds.length === 0) {
        return {};
      }

      try {
        const stage1DataMap = {};

        // Step 1: Get entry details to find the notebook ID
        const entryResponse = await fetch(
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

        if (!entryResponse.ok) {
          console.debug("Could not fetch entry details");
          return stage1DataMap;
        }

        const entryData = await entryResponse.json();
        const nbId = entryData.notebook?.id || entryData.notebookInstanceId;

        if (!nbId) {
          console.debug("No notebook ID found in entry");
          return stage1DataMap;
        }

        // Store notebook ID for later use in advancement
        setNotebookId(nbId);

        // Step 2: Get all pages for this notebook
        const notebookResponse = await fetch(
          `${config.serverBaseUrl}/rest/notebook/view/${nbId}`,
          {
            method: "GET",
            credentials: "include",
            headers: {
              "X-CSRF-Token": localStorage.getItem("CSRF"),
              "Content-Type": "application/json",
            },
          },
        );

        if (!notebookResponse.ok) {
          console.debug("Could not fetch notebook details");
          return stage1DataMap;
        }

        const notebookData = await notebookResponse.json();
        const pages = notebookData.pages || [];

        // Find Stage 1 page (usually the first page by order)
        const stage1Page = pages.find(
          (p) => p.displayOrder === 1 || p.order === 1,
        );

        if (!stage1Page || !stage1Page.id) {
          console.debug("Could not find Stage 1 page");
          return stage1DataMap;
        }

        // Step 3: Fetch samples from Stage 1 page
        const stage1Response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/page/${stage1Page.id}/samples`,
          {
            method: "GET",
            credentials: "include",
            headers: {
              "X-CSRF-Token": localStorage.getItem("CSRF"),
              "Content-Type": "application/json",
            },
          },
        );

        if (stage1Response.ok) {
          const stage1Samples = await stage1Response.json();
          const stage1Array = Array.isArray(stage1Samples)
            ? stage1Samples
            : stage1Samples.samples || [];

          // Map Stage 1 sample data by sampleItemId
          stage1Array.forEach((sample) => {
            if (sample.sampleItemId && sample.data) {
              stage1DataMap[sample.sampleItemId] = sample.data;
            }
          });

          console.debug(
            "Loaded Stage 1 reference data for",
            Object.keys(stage1DataMap).length,
            "samples",
          );
        }

        return stage1DataMap;
      } catch (error) {
        console.debug("Error fetching Stage 1 reference data:", error);
        return {};
      }
    },
    [entryId],
  );

  // Load samples from Stage 2 on component mount
  useEffect(() => {
    const loadSamples = async () => {
      // Only load if we have a valid page ID (not a placeholder)
      if (!pageData?.id || String(pageData.id).startsWith("default-")) {
        setSamples([]);
        return;
      }

      setIsLoading(true);
      try {
        // Load samples specifically for this page (Stage 2)
        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`,
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
          const data = await response.json();
          console.debug(
            "Loaded samples for Stage 2 (page ID " + pageData.id + "):",
            Array.isArray(data) ? data.length : data.samples?.length || 0,
            "samples",
          );

          const rawSamples = Array.isArray(data) ? data : data.samples || [];

          // Fetch Stage 1 reference data for all samples
          const stage1DataMap = await fetchStage1DataForSamples(
            rawSamples.map((s) => s.sampleItemId),
          );

          // Transform samples to extract JSONB data fields
          const transformedSamples = rawSamples.map((sample) => {
            const sampleDataFields = sample.data || {};
            const stage1Data = stage1DataMap[sample.sampleItemId] || {};

            return {
              ...sample,
              // Extract fields from Stage 2 JSONB data object
              sampleType:
                sampleDataFields.sampleType ||
                stage1Data.sampleType ||
                sample.sampleType,
              requestedTests:
                sampleDataFields.requestedTests ||
                stage1Data.requestedTests ||
                sample.requestedTests,
              timepoint:
                sampleDataFields.timepoint ||
                stage1Data.timepoint ||
                sample.timepoint,
              sourceOrigin:
                sampleDataFields.sourceOrigin ||
                stage1Data.sourceOrigin ||
                sample.sourceOrigin,
              // Spread all JSONB fields to make them available
              ...stage1Data,
              ...sampleDataFields,
            };
          });

          console.debug(
            "Transformed Stage 2 samples with Stage 1 reference:",
            transformedSamples.length,
            transformedSamples,
          );

          setSamples(transformedSamples);
        } else {
          console.error("Failed to load samples:", response.status);
          setSamples([]);
        }
      } catch (error) {
        console.error("Error loading samples:", error);
        setSamples([]);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.testassignment.error",
            defaultMessage: "Error",
          }),
          message: intl.formatMessage({
            id: "notebook.bioanalytical.testassignment.loadError",
            defaultMessage: "Failed to load samples. Please refresh the page.",
          }),
        });
      } finally {
        setIsLoading(false);
      }
    };

    loadSamples();
  }, [entryId, intl, pageData?.id, fetchStage1DataForSamples, notify]);

  // Load existing test assignments from samples on component mount or when samples change
  useEffect(() => {
    if (samples && samples.length > 0) {
      const assignments = {};
      samples.forEach((sample) => {
        // Extract test assignment data from the sample's JSONB data
        if (sample.data && sample.data.analyticalMethod) {
          assignments[sample.id] = {
            analyticalMethod: sample.data.analyticalMethod,
            assignedStaff: sample.data.assignedStaff || "",
            instrumentId: sample.data.instrumentId || "",
            qcLevels: sample.data.qcLevels || {
              low: { concentration: "", tolerance: "" },
              medium: { concentration: "", tolerance: "" },
              high: { concentration: "", tolerance: "" },
            },
            acceptanceCriteria: sample.data.acceptanceCriteria || {
              rSquaredMin: "0.995",
              slopeRange: { min: "0.8", max: "1.2" },
              interceptMax: "20",
            },
            samplePreparation: sample.data.samplePreparation || "",
            expectedAnalysisDate: sample.data.expectedAnalysisDate || "",
            notes: sample.data.notes || "",
          };
        }
      });
      if (Object.keys(assignments).length > 0) {
        console.debug(
          "Loaded test assignments for",
          Object.keys(assignments).length,
          "samples from backend",
        );
        setTestAssignments(assignments);
      }
    }
  }, [samples]);

  const toggleSampleSelection = (sampleId) => {
    const newSelection = new Set(selectedSamples);
    if (newSelection.has(sampleId)) {
      newSelection.delete(sampleId);
    } else {
      newSelection.add(sampleId);
    }
    setSelectedSamples(newSelection);
  };

  // Utility functions for method compatibility
  const getCompatibleMethods = useCallback((sampleType) => {
    return ANALYTICAL_METHODS.filter(
      (method) =>
        method.suitableFor.includes(sampleType) ||
        method.suitableFor.includes("All"),
    );
  }, []);

  const getRecommendedStaff = useCallback((methodId) => {
    return STAFF_ROLES.filter((role) => role.specialties.includes(methodId));
  }, []);

  // Get analytical method name by ID
  const getMethodName = useCallback((methodId) => {
    return ANALYTICAL_METHODS.find((m) => m.id === methodId)?.name || methodId;
  }, []);

  // Get analyzer/instrument name by ID from templateInstruments
  const getAnalyzerName = useCallback(
    (analyzerId) => {
      if (!templateInstruments || templateInstruments.length === 0) {
        return analyzerId;
      }
      return (
        templateInstruments.find(
          (a) => a.id === analyzerId || a.value === analyzerId,
        )?.name ||
        templateInstruments.find((a) => String(a.id) === String(analyzerId))
          ?.name ||
        analyzerId
      );
    },
    [templateInstruments],
  );

  // Get color based on analytical method
  const getMethodColor = useCallback((methodId) => {
    const colors = {
      HPLC_UV_VIS: "#0043CE", // Blue
      LC_MS_MS: "#7F10F0", // Purple
      DISSOLUTION_USP: "#F1C21B", // Yellow
      PHYSICAL_TESTING: "#FF832B", // Orange
      IDENTITY_TEST: "#24A148", // Green
    };
    return colors[methodId] || "#525252"; // Gray default
  }, []);

  // Show assignment form when samples are selected
  const handleShowAssignmentForm = useCallback(() => {
    if (selectedSamples.size === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.noSamplesSelected",
          defaultMessage: "Please select at least one sample to assign tests",
        }),
      });
      return;
    }
    setShowAssignmentForm(true);
  }, [selectedSamples.size, intl, notify]);

  // Handle form field changes
  const handleConfigChange = useCallback((field, value, subField = null) => {
    setAssignmentConfig((prev) => {
      if (subField) {
        return {
          ...prev,
          [field]: {
            ...prev[field],
            [subField]: value,
          },
        };
      }
      return {
        ...prev,
        [field]: value,
      };
    });
  }, []);

  // Handle QC level changes
  const handleQcLevelChange = useCallback((level, field, value) => {
    setAssignmentConfig((prev) => ({
      ...prev,
      qcLevels: {
        ...prev.qcLevels,
        [level]: {
          ...prev.qcLevels[level],
          [field]: value,
        },
      },
    }));
  }, []);

  // Handle acceptance criteria changes
  const handleAcceptanceCriteriaChange = useCallback(
    (field, value, subField = null) => {
      setAssignmentConfig((prev) => ({
        ...prev,
        acceptanceCriteria: {
          ...prev.acceptanceCriteria,
          [field]: subField
            ? {
                ...prev.acceptanceCriteria[field],
                [subField]: value,
              }
            : value,
        },
      }));
    },
    [],
  );

  // Main test assignment function with backend API call
  const handleTestAssignment = useCallback(async () => {
    if (selectedSamples.size === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.noSamplesSelected",
          defaultMessage: "Please select at least one sample to assign tests",
        }),
      });
      return;
    }

    // Validate required fields
    if (!assignmentConfig.analyticalMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.methodRequired",
          defaultMessage: "Please select an analytical method",
        }),
      });
      return;
    }

    if (!assignmentConfig.assignedStaff) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.staffRequired",
          defaultMessage: "Please assign a staff member",
        }),
      });
      return;
    }

    if (
      !assignmentConfig.samplePreparation ||
      assignmentConfig.samplePreparation.trim().length === 0
    ) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.preparationRequired",
          defaultMessage:
            "Please document the sample preparation method according to the selected analytical method requirements",
        }),
      });
      return;
    }

    setIsAssigning(true);

    try {
      // Prepare assignment data
      const assignmentData = {
        entryId: entryId,
        sampleIds: Array.from(selectedSamples),
        analyticalMethod: assignmentConfig.analyticalMethod,
        assignedStaff: assignmentConfig.assignedStaff,
        instrumentId: assignmentConfig.instrumentId,
        qcConfiguration: {
          levels: assignmentConfig.qcLevels,
          acceptanceCriteria: assignmentConfig.acceptanceCriteria,
        },
        samplePreparation: assignmentConfig.samplePreparation,
        expectedAnalysisDate: assignmentConfig.expectedAnalysisDate,
        notes: assignmentConfig.notes,
      };

      // Use existing bulk operation endpoint to store test assignment data
      // Store test assignment configuration in the sample's JSONB data field
      // Get assigned control standards from repository for this method
      const assignedControlStandards = BIOANALYTICAL_CONTROL_STANDARDS.filter(
        control => control.methods.includes(assignmentConfig.analyticalMethod)
      );

      const controlStandardAnalysis = {
        totalSelectedSamples: Array.from(selectedSamples).length,
        availableControlStandards: assignedControlStandards,
        controlStandardsByType: assignedControlStandards.reduce((acc, standard) => {
          acc[standard.type] = (acc[standard.type] || []).concat(standard);
          return acc;
        }, {}),
      };

      // Get method control requirements
      const method = ANALYTICAL_METHODS.find(m => m.id === assignmentConfig.analyticalMethod);
      const controlRequirements = method?.controlRequirements || [];

      const testAssignmentData = {
        analyticalMethod: assignmentConfig.analyticalMethod,
        assignedStaff: assignmentConfig.assignedStaff,
        instrumentId: assignmentConfig.instrumentId,
        qcConfiguration: assignmentConfig.qcLevels,
        acceptanceCriteria: assignmentConfig.acceptanceCriteria,
        samplePreparation: assignmentConfig.samplePreparation,
        expectedAnalysisDate: assignmentConfig.expectedAnalysisDate,
        notes: assignmentConfig.notes,
        assignmentDate: new Date().toISOString(),
        assignmentStatus: "ASSIGNED",
        // Control Standards Repository and Assignment Tracking
        controlStandardsConfig: {
          [assignmentConfig.analyticalMethod]: {
            methodRequirements: controlRequirements,
            availableStandards: assignedControlStandards.map(standard => ({
              controlId: standard.id,
              controlName: standard.name,
              controlType: standard.type,
              supplier: standard.supplier,
              catalogNumber: standard.catalogNumber,
              concentration: standard.concentration,
              acceptanceCriteria: standard.acceptanceCriteria,
              qcLevel: standard.qcLevel,
              storageConditions: standard.storageConditions,
              shelfLife: standard.shelfLife,
              assignedAt: new Date().toISOString()
            })),
            assignmentSummary: {
              totalStandardsAvailable: assignedControlStandards.length,
              totalControlsRequired: controlRequirements.reduce((sum, req) => sum + req.count, 0),
              standardsByType: controlStandardAnalysis.controlStandardsByType,
              complianceStatus: (() => {
                // Check if each required control type has sufficient standards available
                const allRequirementsMet = controlRequirements.every(req => {
                  const availableForType = assignedControlStandards.filter(s => s.type === req.type);
                  return availableForType.length >= req.count;
                });
                return allRequirementsMet ? "COMPLIANT" : "INSUFFICIENT_STANDARDS";
              })(),
              assignmentDate: new Date().toISOString()
            }
          }
        }
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
          body: JSON.stringify({
            sampleIds: Array.from(selectedSamples).map((id) =>
              parseInt(id, 10),
            ),
            data: testAssignmentData,
          }),
        },
      );

      if (response.ok) {
        const result = await response.json();

        // Update local state - track which samples have assignments
        const newAssignments = {};
        Array.from(selectedSamples).forEach((sampleId) => {
          newAssignments[sampleId] = {
            ...testAssignmentData,
            count: 1,
          };
        });

        setTestAssignments((prev) => ({
          ...prev,
          ...newAssignments,
        }));

        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.testassignment.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage(
            {
              id: "notebook.bioanalytical.testassignment.successMessage",
              defaultMessage:
                "Tests assigned to {count} samples using {method}",
            },
            {
              count: selectedSamples.size,
              method:
                ANALYTICAL_METHODS.find(
                  (m) => m.id === assignmentConfig.analyticalMethod,
                )?.name || "selected method",
            },
          ),
        });

        // Reset form and close assignment modal
        setShowAssignmentForm(false);
        setSelectedSamples(new Set());
        setAssignmentConfig({
          analyticalMethod: "",
          assignedStaff: "",
          instrumentId: "",
          qcLevels: {
            low: { concentration: "5", tolerance: "20" },
            medium: { concentration: "50", tolerance: "20" },
            high: { concentration: "500", tolerance: "20" },
          },
          acceptanceCriteria: {
            rSquaredMin: "0.995",
            slopeRange: { min: "0.8", max: "1.2" },
            interceptMax: "20",
          },
          samplePreparation: "",
          expectedAnalysisDate: "",
          notes: "",
        });

        // Notify parent component of progress update
        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.message || errorData.error || "Test assignment failed",
        );
      }
    } catch (error) {
      console.error("Test assignment error:", error);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage(
          {
            id: "notebook.bioanalytical.testassignment.assignmentError",
            defaultMessage: "Failed to assign tests: {error}",
          },
          { error: error.message },
        ),
      });
    } finally {
      setIsAssigning(false);
    }
  }, [
    selectedSamples,
    assignmentConfig,
    entryId,
    intl,
    onProgressUpdate,
    notify,
  ]);

  // Handle marking samples complete and advancing to Stage 3 (Analytical Execution)
  const handleMarkCompleteAndAdvance = useCallback(async () => {
    // Get samples that have test assignments (completed)
    const assignedSamples = samples.filter(
      (s) => testAssignments[s.id] && s.status !== "COMPLETED",
    );

    if (assignedSamples.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.noAssignedSamples",
          defaultMessage:
            "No samples with test assignments to move to the next stage. Please assign tests first.",
        }),
      });
      return;
    }

    setIsAdvancing(true);

    try {
      // Get sample IDs as strings
      const sampleIds = assignedSamples.map((s) => String(s.id));

      // Step 1: Mark samples as COMPLETED on Stage 2
      const statusResponse = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/status-string`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
        },
      );

      if (!statusResponse.ok) {
        throw new Error("Failed to mark samples as completed");
      }

      // Step 2: Advance samples to Stage 3 (Analytical Execution)
      // Stage 3 is displayOrder/pageIndex 3
      if (!notebookId) {
        console.warn(
          "notebookId not available, cannot advance samples to Stage 3",
        );
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.testassignment.completed",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage(
            {
              id: "notebook.bioanalytical.testassignment.completeSuccess",
              defaultMessage:
                "Successfully marked {count} samples as complete.",
            },
            { count: assignedSamples.length },
          ),
        });
        if (onProgressUpdate) {
          onProgressUpdate();
        }
        setIsAdvancing(false);
        return;
      }

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
            sampleIds: sampleIds,
            fromPageId: pageData.id,
            toPageIndex: 3, // Stage 3: Analytical Execution
          }),
        },
      );

      if (advanceResponse.ok) {
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.testassignment.completed",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage(
            {
              id: "notebook.bioanalytical.testassignment.completeAndAdvanceSuccess",
              defaultMessage:
                "Successfully completed {count} samples and advanced to Analytical Execution stage.",
            },
            { count: assignedSamples.length },
          ),
        });
      } else {
        // Samples marked complete but advance failed - show partial success
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.testassignment.completed",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage(
            {
              id: "notebook.bioanalytical.testassignment.completeSuccess",
              defaultMessage:
                "Successfully marked {count} samples as complete.",
            },
            { count: assignedSamples.length },
          ),
        });
        console.warn(
          "Failed to advance samples to Stage 3:",
          advanceResponse.status,
        );
      }

      // Refresh progress and reload samples
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    } catch (error) {
      console.error("Error completing and advancing samples:", error);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage(
          {
            id: "notebook.bioanalytical.testassignment.advanceError",
            defaultMessage: "Failed to complete and advance samples: {error}",
          },
          { error: error.message },
        ),
      });
    } finally {
      setIsAdvancing(false);
    }
  }, [
    samples,
    testAssignments,
    notebookId,
    pageData?.id,
    intl,
    notify,
    onProgressUpdate,
  ]);

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.testassignment.title"
            defaultMessage="Analytical Test Assignment"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.testassignment.description"
            defaultMessage="Assign analytical tests to received samples, configure QC levels, and select analytical methods. Tests are configured based on sample type and requested analyses."
          />
        </p>
      </div>

      <Grid>
        <Column lg={16} md={8} sm={4}>
          <div className="section-header">
            <h4>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.selectSamples"
                defaultMessage="Select Samples for Test Assignment"
              />
            </h4>
            <p>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.selectHelp"
                defaultMessage="Choose samples that need test assignment and configuration."
              />
            </p>

            {isLoading ? (
              <Loading description="Loading samples..." />
            ) : (
              <>
                {/* Action buttons positioned at top left of datatable */}
                <div
                  style={{
                    marginTop: "1.5rem",
                    marginBottom: "1rem",
                    display: "flex",
                    gap: "0.75rem",
                  }}
                >
                  {selectedSamples.size > 0 && (
                    <Button
                      kind="primary"
                      onClick={handleShowAssignmentForm}
                      disabled={!canAssignTests}
                      title={
                        !canAssignTests
                          ? intl.formatMessage({
                              id: "notebook.bioanalytical.testassignment.insufficientPermissions",
                              defaultMessage:
                                "Insufficient permissions to configure test assignments. Only Pharmacists and Lab Supervisors can assign tests.",
                            })
                          : undefined
                      }
                    >
                      <FormattedMessage
                        id="notebook.bioanalytical.testassignment.configureTests"
                        defaultMessage="Configure Tests for {count} Sample(s)"
                        values={{ count: selectedSamples.size }}
                      />
                    </Button>
                  )}

                  {/* Show completion button if samples have test assignments */}
                  {samples.filter((s) => testAssignments[s.id]).length > 0 && (
                    <Button
                      kind="secondary"
                      onClick={handleMarkCompleteAndAdvance}
                      disabled={isAdvancing || !canAssignTests}
                      title={
                        !canAssignTests
                          ? intl.formatMessage({
                              id: "notebook.bioanalytical.testassignment.completeInsufficientPermissions",
                              defaultMessage:
                                "Insufficient permissions to complete test assignments. Only Pharmacists and Lab Supervisors can complete assignments.",
                            })
                          : undefined
                      }
                    >
                      <FormattedMessage
                        id="notebook.bioanalytical.testassignment.completeAndAdvance"
                        defaultMessage="Mark Complete & Move to Next Stage"
                      />
                    </Button>
                  )}
                </div>

                <div style={{ marginBottom: "1.5rem" }}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <input
                            type="checkbox"
                            checked={
                              selectedSamples.size === samples.length &&
                              samples.length > 0
                            }
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedSamples(
                                  new Set(samples.map((s) => s.id)),
                                );
                              } else {
                                setSelectedSamples(new Set());
                              }
                            }}
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.sampleId"
                            defaultMessage="Sample ID"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.type"
                            defaultMessage="Sample Type"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.requestedTests"
                            defaultMessage="Requested Tests"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.assignedTests"
                            defaultMessage="Assigned Tests"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.qcStatus"
                            defaultMessage="QC Status"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.controlType"
                            defaultMessage="Control Type"
                          />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {samples.length > 0 ? (
                        samples.map((sample) => (
                          <TableRow key={sample.id}>
                            <TableCell>
                              <input
                                type="checkbox"
                                checked={selectedSamples.has(sample.id)}
                                onChange={() =>
                                  toggleSampleSelection(sample.id)
                                }
                              />
                            </TableCell>
                            <TableCell>
                              {sample.accessionNumber || "-"}
                            </TableCell>
                            <TableCell>
                              {typeof sample.sampleType === "object"
                                ? sample.sampleType?.name || "-"
                                : sample.sampleType || "-"}
                            </TableCell>
                            <TableCell>
                              {Array.isArray(sample.requestedTests)
                                ? sample.requestedTests.join(", ")
                                : sample.requestedTests || "-"}
                            </TableCell>
                            <TableCell>
                              {testAssignments[sample.id] ? (
                                <div
                                  style={{
                                    display: "flex",
                                    gap: "0.5rem",
                                    flexWrap: "wrap",
                                  }}
                                >
                                  <span
                                    style={{
                                      backgroundColor: getMethodColor(
                                        testAssignments[sample.id]
                                          .analyticalMethod,
                                      ),
                                      color: "white",
                                      padding: "0.25rem 0.75rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      fontWeight: 600,
                                      whiteSpace: "nowrap",
                                    }}
                                  >
                                    {getMethodName(
                                      testAssignments[sample.id]
                                        .analyticalMethod,
                                    )}
                                  </span>
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
                                    {testAssignments[sample.id].instrumentId
                                      ? getAnalyzerName(
                                          testAssignments[sample.id]
                                            .instrumentId,
                                        )
                                      : "No Analyzer"}
                                  </span>
                                </div>
                              ) : (
                                <span style={{ color: "#a8a8a8" }}>
                                  <FormattedMessage
                                    id="notebook.bioanalytical.testassignment.notAssigned"
                                    defaultMessage="Not assigned"
                                  />
                                </span>
                              )}
                            </TableCell>
                            <TableCell>
                              {renderStage2QCStatus(sample)}
                            </TableCell>
                            <TableCell>
                              {renderStage2ControlType(sample)}
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell
                            colSpan="7"
                            style={{ textAlign: "center" }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.testassignment.noSamples"
                              defaultMessage="No samples available for test assignment. Please complete Stage 1 (Sample Reception) first."
                            />
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </div>

                {/* Helper text for completion action */}
                {samples.filter((s) => testAssignments[s.id]).length > 0 && (
                  <p
                    style={{
                      fontSize: "0.875rem",
                      color: "#525252",
                      marginTop: "0.5rem",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.bioanalytical.testassignment.completeNote"
                      defaultMessage="Moves all samples with assigned tests to Analytical Execution (Stage 3)"
                    />
                  </p>
                )}
              </>
            )}
          </div>
        </Column>
      </Grid>

      {/* Test Assignment Configuration Modal */}
      <Modal
        open={showAssignmentForm}
        onRequestClose={() => {
          setShowAssignmentForm(false);
          setSelectedSamples(new Set());
        }}
        modalHeading={
          <FormattedMessage
            id="notebook.bioanalytical.testassignment.configurationForm"
            defaultMessage="Test Assignment Configuration"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="notebook.bioanalytical.testassignment.assignTests"
            defaultMessage="Assign Tests to {count} Sample(s)"
            values={{ count: selectedSamples.size }}
          />
        }
        secondaryButtonText={
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleTestAssignment}
        primaryButtonDisabled={
          isAssigning ||
          !assignmentConfig.analyticalMethod ||
          !assignmentConfig.assignedStaff ||
          !assignmentConfig.samplePreparation?.trim() ||
          !canSaveAssignments
        }
        size="md"
      >
        <Form>
          {/* Diagnostic: Show what's blocking the button */}
          {(isAssigning ||
            !assignmentConfig.analyticalMethod ||
            !assignmentConfig.assignedStaff ||
            !assignmentConfig.samplePreparation?.trim() ||
            !canSaveAssignments) && (
            <div
              style={{
                marginBottom: "1rem",
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
                Required fields to enable assignment:
              </p>
              <ul
                style={{
                  margin: 0,
                  paddingLeft: "1.5rem",
                  fontSize: "0.875rem",
                  color: "#161616",
                }}
              >
                {isAssigning && (
                  <li>
                    <span style={{ color: "#da1e28" }}>●</span> Assigning in
                    progress...
                  </li>
                )}
                {!assignmentConfig.analyticalMethod && (
                  <li>
                    <span style={{ color: "#da1e28" }}>●</span> Select
                    analytical method
                  </li>
                )}
                {!assignmentConfig.assignedStaff && (
                  <li>
                    <span style={{ color: "#da1e28" }}>●</span> Assign staff
                    member
                  </li>
                )}
                {!assignmentConfig.samplePreparation?.trim() && (
                  <li>
                    <span style={{ color: "#da1e28" }}>●</span> Document
                    preparation method
                  </li>
                )}
                {!canSaveAssignments && (
                  <li>
                    <span style={{ color: "#da1e28" }}>●</span> Insufficient
                    permissions (current level: {pagePermissionLevel || "NONE"})
                  </li>
                )}
              </ul>
            </div>
          )}

          {/* Analytical Method Selection */}
          <FormGroup legendText="">
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="analytical-method"
                  labelText={
                    <FormattedMessage
                      id="notebook.bioanalytical.testassignment.analyticalMethod"
                      defaultMessage="Analytical Method *"
                    />
                  }
                  value={assignmentConfig.analyticalMethod}
                  onChange={(e) =>
                    handleConfigChange("analyticalMethod", e.target.value)
                  }
                  helperText={
                    <FormattedMessage
                      id="notebook.bioanalytical.testassignment.methodHelp"
                      defaultMessage="Select the analytical method based on sample type and test requirements"
                    />
                  }
                >
                  <SelectItem value="" text="Select analytical method..." />
                  {ANALYTICAL_METHODS.map((method) => (
                    <SelectItem
                      key={method.id}
                      value={method.id}
                      text={`${method.name} - ${method.description}`}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={8} md={4} sm={4}>
                <Select
                  id="assigned-staff"
                  labelText={
                    <FormattedMessage
                      id="notebook.bioanalytical.testassignment.assignedStaff"
                      defaultMessage="Assigned Staff *"
                    />
                  }
                  value={assignmentConfig.assignedStaff}
                  onChange={(e) =>
                    handleConfigChange("assignedStaff", e.target.value)
                  }
                  helperText={
                    assignmentConfig.analyticalMethod
                      ? `Recommended: ${getRecommendedStaff(
                          assignmentConfig.analyticalMethod,
                        )
                          .map((s) => s.name)
                          .join(", ")}`
                      : "Staff assignment based on test category and method"
                  }
                >
                  <SelectItem value="" text="Select staff member..." />
                  {STAFF_ROLES.map((role) => (
                    <SelectItem
                      key={role.id}
                      value={role.id}
                      text={`${role.name} - ${role.description}`}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>
          </FormGroup>

          {/* Instrument Selection */}
          <FormGroup legendText="">
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="instrument-id"
                  labelText={
                    <FormattedMessage
                      id="notebook.bioanalytical.testassignment.instrument"
                      defaultMessage="Analyzer / Instrument"
                    />
                  }
                  value={assignmentConfig.instrumentId}
                  onChange={(e) =>
                    handleConfigChange("instrumentId", e.target.value)
                  }
                  helperText={
                    <FormattedMessage
                      id="notebook.bioanalytical.testassignment.instrumentHelp"
                      defaultMessage="Select the analyzer/instrument for analysis"
                    />
                  }
                >
                  <SelectItem value="" text="Select analyzer..." />
                  {templateInstruments && templateInstruments.length > 0
                    ? templateInstruments.map((analyzer) => (
                        <SelectItem
                          key={analyzer.id || analyzer.value}
                          value={analyzer.id || analyzer.value}
                          text={
                            analyzer.name ||
                            analyzer.value ||
                            analyzer.label ||
                            ""
                          }
                        />
                      ))
                    : []}
                </Select>
              </Column>

              <Column lg={8} md={4} sm={4}>
                <DatePicker
                  dateFormat="Y-m-d"
                  datePickerType="single"
                  onChange={(dates) => {
                    if (dates && dates.length > 0) {
                      const dateStr = dates[0].toISOString().split("T")[0];
                      handleConfigChange("expectedAnalysisDate", dateStr);
                    }
                  }}
                >
                  <DatePickerInput
                    id="expected-analysis-date"
                    labelText={
                      <FormattedMessage
                        id="notebook.bioanalytical.testassignment.expectedDate"
                        defaultMessage="Expected Analysis Date"
                      />
                    }
                    placeholder="YYYY-MM-DD"
                    value={assignmentConfig.expectedAnalysisDate}
                  />
                </DatePicker>
              </Column>
            </Grid>
          </FormGroup>

          {/* QC Levels Configuration */}
          <FormGroup legendText="">
            <h5 style={{ marginBottom: "1rem", color: "#161616" }}>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.qcLevelsTitle"
                defaultMessage="QC Levels Configuration"
              />
            </h5>
            <p
              style={{
                color: "#525252",
                fontSize: "0.875rem",
                marginBottom: "1rem",
              }}
            >
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.qcLevelInfo"
                defaultMessage="QC Levels: Low (LLOQ - 2x LLOQ), Medium (30% span), High (near upper limit)"
              />
            </p>

            <Grid>
              <Column lg={5} md={3} sm={4}>
                <h6>Low QC Level</h6>
                <NumberInput
                  id="qc-low-concentration"
                  label="Concentration (ng/mL)"
                  min={0}
                  max={10000}
                  step={0.1}
                  defaultValue={5}
                  value={assignmentConfig.qcLevels.low.concentration}
                  onChange={({ value }) =>
                    handleQcLevelChange("low", "concentration", String(value))
                  }
                  style={{ marginBottom: "0.5rem" }}
                />
                <NumberInput
                  id="qc-low-tolerance"
                  label="Tolerance (%)"
                  min={0}
                  max={50}
                  step={0.1}
                  defaultValue={20}
                  value={assignmentConfig.qcLevels.low.tolerance}
                  onChange={({ value }) =>
                    handleQcLevelChange("low", "tolerance", String(value))
                  }
                />
              </Column>

              <Column lg={5} md={3} sm={4}>
                <h6>Medium QC Level</h6>
                <NumberInput
                  id="qc-medium-concentration"
                  label="Concentration (ng/mL)"
                  min={0}
                  max={10000}
                  step={0.1}
                  defaultValue={50}
                  value={assignmentConfig.qcLevels.medium.concentration}
                  onChange={({ value }) =>
                    handleQcLevelChange(
                      "medium",
                      "concentration",
                      String(value),
                    )
                  }
                  style={{ marginBottom: "0.5rem" }}
                />
                <NumberInput
                  id="qc-medium-tolerance"
                  label="Tolerance (%)"
                  min={0}
                  max={50}
                  step={0.1}
                  defaultValue={20}
                  value={assignmentConfig.qcLevels.medium.tolerance}
                  onChange={({ value }) =>
                    handleQcLevelChange("medium", "tolerance", String(value))
                  }
                />
              </Column>

              <Column lg={5} md={2} sm={4}>
                <h6>High QC Level</h6>
                <NumberInput
                  id="qc-high-concentration"
                  label="Concentration (ng/mL)"
                  min={0}
                  max={10000}
                  step={0.1}
                  defaultValue={500}
                  value={assignmentConfig.qcLevels.high.concentration}
                  onChange={({ value }) =>
                    handleQcLevelChange("high", "concentration", String(value))
                  }
                  style={{ marginBottom: "0.5rem" }}
                />
                <NumberInput
                  id="qc-high-tolerance"
                  label="Tolerance (%)"
                  min={0}
                  max={50}
                  step={0.1}
                  defaultValue={20}
                  value={assignmentConfig.qcLevels.high.tolerance}
                  onChange={({ value }) =>
                    handleQcLevelChange("high", "tolerance", String(value))
                  }
                />
              </Column>
            </Grid>
          </FormGroup>

          {/* Acceptance Criteria */}
          <FormGroup legendText="">
            <h5 style={{ marginBottom: "1rem", color: "#161616" }}>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.acceptanceCriteriaTitle"
                defaultMessage="Acceptance Criteria"
              />
            </h5>

            <Grid>
              <Column lg={5} md={3} sm={4}>
                <NumberInput
                  id="r-squared-min"
                  label="R² Minimum"
                  min={0.5}
                  max={1.0}
                  step={0.001}
                  defaultValue={0.995}
                  value={assignmentConfig.acceptanceCriteria.rSquaredMin}
                  onChange={({ value }) =>
                    handleAcceptanceCriteriaChange("rSquaredMin", String(value))
                  }
                />
              </Column>

              <Column lg={5} md={3} sm={4}>
                <TextInput
                  id="slope-min"
                  labelText="Slope Range Min"
                  value={assignmentConfig.acceptanceCriteria.slopeRange.min}
                  onChange={(e) =>
                    handleAcceptanceCriteriaChange(
                      "slopeRange",
                      e.target.value,
                      "min",
                    )
                  }
                />
                <TextInput
                  id="slope-max"
                  labelText="Slope Range Max"
                  value={assignmentConfig.acceptanceCriteria.slopeRange.max}
                  onChange={(e) =>
                    handleAcceptanceCriteriaChange(
                      "slopeRange",
                      e.target.value,
                      "max",
                    )
                  }
                  style={{ marginTop: "0.5rem" }}
                />
              </Column>

              <Column lg={5} md={2} sm={4}>
                <NumberInput
                  id="intercept-max"
                  label="Intercept Max (%)"
                  min={0}
                  max={100}
                  step={1}
                  defaultValue={20}
                  value={assignmentConfig.acceptanceCriteria.interceptMax}
                  onChange={({ value }) =>
                    handleAcceptanceCriteriaChange(
                      "interceptMax",
                      String(value),
                    )
                  }
                />
              </Column>
            </Grid>
          </FormGroup>

          {/* Control Sample Requirements */}
          <FormGroup legendText="">
            <h5 style={{ marginBottom: "1rem", color: "#161616" }}>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.controlRequirements"
                defaultMessage="Control Sample Requirements"
              />
            </h5>

            {assignmentConfig.analyticalMethod && (
              <>
                <p style={{ color: "#525252", fontSize: "0.875rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="notebook.bioanalytical.testassignment.controlInfo"
                    defaultMessage="Control samples required per analytical run based on selected method"
                  />
                </p>

                <div style={{
                  marginBottom: "1rem",
                  padding: "1rem",
                  backgroundColor: "#f2f2f2",
                  borderRadius: "4px",
                  border: "1px solid #d1d1d1"
                }}>
                  <h6 style={{ marginBottom: "0.75rem", fontSize: "0.875rem", fontWeight: "500" }}>
                    Required Controls for {getMethodName(assignmentConfig.analyticalMethod)}:
                  </h6>

                  {(() => {
                    const method = ANALYTICAL_METHODS.find(m => m.id === assignmentConfig.analyticalMethod);
                    if (!method || !method.controlRequirements) return null;

                    return (
                      <div style={{ display: "grid", gap: "0.75rem" }}>
                        {method.controlRequirements.map((control, index) => (
                          <div key={index} style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            padding: "0.5rem",
                            backgroundColor: "white",
                            borderRadius: "4px",
                            border: "1px solid #e0e0e0"
                          }}>
                            <div style={{ flex: 1 }}>
                              <div style={{ fontWeight: "500", fontSize: "0.875rem" }}>
                                {control.type.replace(/_/g, ' ')}
                              </div>
                              <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                                {control.description}
                              </div>
                            </div>
                            <div style={{ textAlign: "right" }}>
                              <Tag
                                type={control.type.includes('QC') ? 'blue' :
                                      control.type === 'POSITIVE' ? 'green' :
                                      control.type === 'NEGATIVE' ? 'red' : 'purple'}
                                size="sm"
                              >
                                {control.count}x {control.frequency.replace(/_/g, ' ')}
                              </Tag>
                            </div>
                          </div>
                        ))}
                      </div>
                    );
                  })()}

                  <div style={{ marginTop: "1rem", padding: "0.75rem", backgroundColor: "#e7f6ed", borderRadius: "4px" }}>
                    <div style={{ fontSize: "0.875rem", fontWeight: "500", marginBottom: "0.25rem" }}>
                      📋 Control Sample Status:
                    </div>
                    <div style={{ fontSize: "0.875rem" }}>
                      {(() => {
                        // Get available control standards for selected method
                        const method = ANALYTICAL_METHODS.find(m => m.id === assignmentConfig.analyticalMethod);
                        const requiredControls = method?.controlRequirements || [];

                        // Find available control standards for this method
                        const availableControls = BIOANALYTICAL_CONTROL_STANDARDS.filter(
                          control => control.methods.includes(assignmentConfig.analyticalMethod)
                        );

                        // Check control availability by type
                        const controlTypeStatus = requiredControls.map(req => {
                          const availableForType = availableControls.filter(c => c.type === req.type);
                          return {
                            type: req.type,
                            required: req.count,
                            available: availableForType.length,
                            sufficient: availableForType.length >= req.count
                          };
                        });

                        const totalRequired = requiredControls.reduce((sum, req) => sum + req.count, 0);
                        const totalAvailable = availableControls.length;
                        const allSufficient = controlTypeStatus.every(status => status.sufficient);

                        if (totalAvailable === 0) {
                          return "⚠️ No control standards configured for this method - contact lab administrator";
                        } else if (!allSufficient) {
                          const insufficientTypes = controlTypeStatus
                            .filter(s => !s.sufficient)
                            .map(s => `${s.type} (${s.available}/${s.required})`)
                            .join(', ');
                          return `⚠️ Insufficient controls: ${insufficientTypes}`;
                        } else {
                          return `✅ ${totalAvailable} control standards available (${totalRequired} required)`;
                        }
                      })()}
                    </div>
                  </div>
                </div>
              </>
            )}
          </FormGroup>

          {/* Control Standards Management */}
          {assignmentConfig.analyticalMethod && (
            <FormGroup legendText="">
              <h5 style={{ marginBottom: "1rem", color: "#161616" }}>
                <FormattedMessage
                  id="bioanalytical.testAssignment.controlStandards.title"
                  defaultMessage="Control Standards Configuration"
                />
              </h5>
              {(() => {
                const availableControls = BIOANALYTICAL_CONTROL_STANDARDS.filter(
                  control => control.methods.includes(assignmentConfig.analyticalMethod)
                );

                if (availableControls.length === 0) {
                  return (
                    <div className="control-standards-empty">
                      <InlineNotification
                        kind="warning"
                        title="No Control Standards"
                        subtitle="No control standards are configured for this analytical method. Contact your laboratory administrator to set up control standards."
                        hideCloseButton
                      />
                    </div>
                  );
                }

                return (
                  <div className="control-standards-grid">
                    <DataTable
                      rows={availableControls.map(control => ({
                        id: control.id,
                        name: control.name,
                        type: control.type,
                        supplier: control.supplier,
                        catalogNumber: control.catalogNumber,
                        concentration: `${control.concentration.value} ${control.concentration.unit}`,
                        acceptanceCriteria: control.acceptanceCriteria.min
                          ? `${control.acceptanceCriteria.min}-${control.acceptanceCriteria.max} ${control.acceptanceCriteria.unit}`
                          : `≤${control.acceptanceCriteria.max} ${control.acceptanceCriteria.unit}`,
                        qcLevel: control.qcLevel,
                        shelfLife: `${control.shelfLife.value} ${control.shelfLife.unit}`,
                        storageConditions: control.storageConditions
                      }))}
                      headers={[
                        { key: 'name', header: 'Control Standard' },
                        { key: 'type', header: 'Type' },
                        { key: 'concentration', header: 'Concentration' },
                        { key: 'acceptanceCriteria', header: 'Acceptance Criteria' },
                        { key: 'qcLevel', header: 'QC Level' },
                        { key: 'supplier', header: 'Supplier' },
                        { key: 'catalogNumber', header: 'Cat. Number' },
                        { key: 'shelfLife', header: 'Shelf Life' }
                      ]}
                      size="sm"
                    >
                      {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
                        <TableContainer title={`Control Standards for ${assignmentConfig.analyticalMethod}`}>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                {headers.map((header) => (
                                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <TableRow key={row.id} {...getRowProps({ row })}>
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>{cell.value}</TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </div>
                );
              })()}
            </FormGroup>
          )}

          {/* Method-Specific Sample Preparation */}
          <FormGroup legendText="">
            <h5 style={{ marginBottom: "1rem", color: "#161616" }}>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.methodSpecificPreparation"
                defaultMessage="Sample Preparation According to Method Requirements"
              />
            </h5>

            {assignmentConfig.analyticalMethod && (
              <div
                style={{
                  marginBottom: "1rem",
                  padding: "1rem",
                  backgroundColor: "#e8f4f8",
                  borderRadius: "4px",
                  border: "1px solid #0072c3",
                }}
              >
                <h6 style={{ color: "#161616", marginBottom: "0.5rem" }}>
                  {
                    ANALYTICAL_METHODS.find(
                      (m) => m.id === assignmentConfig.analyticalMethod,
                    )?.name
                  }{" "}
                  Preparation Steps:
                </h6>
                <ol style={{ marginLeft: "1rem", color: "#525252" }}>
                  {ANALYTICAL_METHODS.find(
                    (m) => m.id === assignmentConfig.analyticalMethod,
                  )?.preparationSteps?.map((step, index) => (
                    <li key={index} style={{ marginBottom: "0.25rem" }}>
                      {step}
                    </li>
                  ))}
                </ol>
                <p
                  style={{
                    marginTop: "0.5rem",
                    fontStyle: "italic",
                    color: "#525252",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.bioanalytical.testassignment.methodGuidance"
                    defaultMessage="Follow these method-specific preparation steps. Document any deviations or additional procedures below."
                  />
                </p>
              </div>
            )}

            <TextArea
              id="sample-preparation"
              labelText={
                <FormattedMessage
                  id="notebook.bioanalytical.testassignment.preparationDocumentation"
                  defaultMessage="Preparation Method Documentation *"
                />
              }
              placeholder={
                assignmentConfig.analyticalMethod
                  ? `Document how you will follow the ${ANALYTICAL_METHODS.find((m) => m.id === assignmentConfig.analyticalMethod)?.name} preparation steps above. Include any specific parameters, deviations, or additional procedures...`
                  : "Select an analytical method above to see method-specific preparation requirements..."
              }
              rows={6}
              value={assignmentConfig.samplePreparation}
              onChange={(e) =>
                handleConfigChange("samplePreparation", e.target.value)
              }
              helperText={
                <FormattedMessage
                  id="notebook.bioanalytical.testassignment.preparationHelp"
                  defaultMessage="Document your specific implementation of the method requirements. Include reagent concentrations, equipment settings, time/temperature conditions, etc."
                />
              }
              disabled={!assignmentConfig.analyticalMethod}
            />

            {assignmentConfig.analyticalMethod && (
              <div style={{ marginTop: "1rem" }}>
                <h6 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                  <FormattedMessage
                    id="notebook.bioanalytical.testassignment.methodRequirements"
                    defaultMessage="Method Requirements & Considerations:"
                  />
                </h6>
                <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
                  <div style={{ flex: 1, minWidth: "250px" }}>
                    <p
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        marginBottom: "0.5rem",
                      }}
                    >
                      <strong>Applications:</strong>
                    </p>
                    <ul
                      style={{
                        marginLeft: "1rem",
                        fontSize: "0.875rem",
                        color: "#525252",
                      }}
                    >
                      {ANALYTICAL_METHODS.find(
                        (m) => m.id === assignmentConfig.analyticalMethod,
                      )?.applications.map((app, index) => (
                        <li key={index}>{app}</li>
                      ))}
                    </ul>
                  </div>
                  <div style={{ flex: 1, minWidth: "250px" }}>
                    <p
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        marginBottom: "0.5rem",
                      }}
                    >
                      <strong>Suitable Sample Types:</strong>
                    </p>
                    <ul
                      style={{
                        marginLeft: "1rem",
                        fontSize: "0.875rem",
                        color: "#525252",
                      }}
                    >
                      {ANALYTICAL_METHODS.find(
                        (m) => m.id === assignmentConfig.analyticalMethod,
                      )?.suitableFor.map((type, index) => (
                        <li key={index}>{type}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
            )}
          </FormGroup>

          {/* Notes */}
          <FormGroup legendText="">
            <TextArea
              id="assignment-notes"
              labelText={
                <FormattedMessage
                  id="notebook.bioanalytical.testassignment.notes"
                  defaultMessage="Additional Notes"
                />
              }
              placeholder="Additional comments or special instructions..."
              rows={3}
              value={assignmentConfig.notes}
              onChange={(e) => handleConfigChange("notes", e.target.value)}
            />
          </FormGroup>
        </Form>
      </Modal>

      {/* Summary Section for Assigned Tests */}
      {!showAssignmentForm && Object.keys(testAssignments).length > 0 && (
        <Grid style={{ marginTop: "2rem" }}>
          <Column lg={16} md={8} sm={4}>
            <div className="section-header">
              <h4>
                <FormattedMessage
                  id="notebook.bioanalytical.testassignment.assignmentSummary"
                  defaultMessage="Test Assignment Summary"
                />
              </h4>
              <p>
                <FormattedMessage
                  id="notebook.bioanalytical.testassignment.summaryHelp"
                  defaultMessage="Review completed test assignments and their configuration."
                />
              </p>

              <div
                style={{
                  marginTop: "1.5rem",
                  color: "#525252",
                  fontSize: "0.875rem",
                }}
              >
                <p>
                  <FormattedMessage
                    id="notebook.bioanalytical.testassignment.totalAssignments"
                    defaultMessage="{count} test assignments completed"
                    values={{ count: Object.keys(testAssignments).length }}
                  />
                </p>
              </div>
            </div>
          </Column>
        </Grid>
      )}
    </div>
  );
}

export default BioanalyticalTestAssignmentPage;
