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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../../config.json";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
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
  },
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
 * Bioanalytical analyzers/instruments available per SRS requirements
 */
const ANALYZERS = [
  {
    id: "1",
    machine: "LC-MS/MS",
    model: "-",
    dataOutput: "Yes",
    integration: "Automatic",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "2",
    machine: "HPLC",
    model: "-",
    dataOutput: "Yes",
    integration: "Automatic",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "3",
    machine: "Dissolution Apparatus",
    model: "-",
    dataOutput: "Yes",
    integration: "Both",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "4",
    machine: "Disintegration Tester",
    model: "-",
    dataOutput: "Yes",
    integration: "Manual",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "5",
    machine: "Hardness Tester",
    model: "-",
    dataOutput: "Yes",
    integration: "Manual",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "6",
    machine: "Friability Tester",
    model: "-",
    dataOutput: "Yes",
    integration: "Manual",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "7",
    machine: "Stability Chamber",
    model: "-",
    dataOutput: "Yes",
    integration: "Automatic",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "8",
    machine: "UV-Vis Spectrophotometer",
    model: "-",
    dataOutput: "Yes",
    integration: "Both",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "9",
    machine: "FTIR",
    model: "-",
    dataOutput: "Yes",
    integration: "Both",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "10",
    machine: "Freezers (-20°C, -80°C)",
    model: "-",
    dataOutput: "Yes",
    integration: "Manual",
    lisManual: "No",
    status: "Active",
  },
  {
    id: "11",
    machine: "Millipore Water Purification",
    model: "-",
    dataOutput: "Yes",
    integration: "Automatic",
    lisManual: "No",
    status: "Active",
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
      low: { concentration: "", tolerance: "" },
      medium: { concentration: "", tolerance: "" },
      high: { concentration: "", tolerance: "" },
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

  // Get analyzer/instrument name by ID
  const getAnalyzerName = useCallback((analyzerId) => {
    return ANALYZERS.find((a) => a.id === analyzerId)?.machine || analyzerId;
  }, []);

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
            low: { concentration: "", tolerance: "" },
            medium: { concentration: "", tolerance: "" },
            high: { concentration: "", tolerance: "" },
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
                <div style={{ marginTop: "1.5rem", marginBottom: "1.5rem" }}>
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
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell
                            colSpan="5"
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

                {selectedSamples.size > 0 && (
                  <div style={{ marginTop: "1.5rem" }}>
                    <Button kind="primary" onClick={handleShowAssignmentForm}>
                      <FormattedMessage
                        id="notebook.bioanalytical.testassignment.configureTests"
                        defaultMessage="Configure Tests for {count} Sample(s)"
                        values={{ count: selectedSamples.size }}
                      />
                    </Button>
                  </div>
                )}

                {/* Show completion button if samples have test assignments */}
                {samples.filter((s) => testAssignments[s.id]).length > 0 && (
                  <div style={{ marginTop: "1.5rem" }}>
                    <Button
                      kind="secondary"
                      onClick={handleMarkCompleteAndAdvance}
                      disabled={isAdvancing}
                    >
                      <FormattedMessage
                        id="notebook.bioanalytical.testassignment.completeAndAdvance"
                        defaultMessage="Mark Complete & Move to Next Stage"
                      />
                    </Button>
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
                  </div>
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
          !assignmentConfig.samplePreparation?.trim()
        }
        size="md"
      >
        <Form>
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
                  {ANALYZERS.map((analyzer) => (
                    <SelectItem
                      key={analyzer.id}
                      value={analyzer.id}
                      text={`${analyzer.machine} (${analyzer.integration})`}
                    />
                  ))}
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
