import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  TextInput,
  TextArea,
  InlineNotification,
  Loading,
  Modal,
  Tag,
  FileUploader,
  Dropdown,
  ProgressBar,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Accordion,
  AccordionItem,
  NumberInput,
  Checkbox,
  Select,
  SelectItem,
  Toggle,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DatePicker,
  DatePickerInput,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  MultiSelect,
} from "@carbon/react";
import {
  Upload,
  CheckmarkFilled,
  WarningAlt,
  Checkmark,
  Edit,
  Chemistry,
  Renew,
  DataBase,
  Settings,
  Play,
  Document,
  Add,
  TrashCan,
  View,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFormDataJson,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";

/**
 * AnalysisPage - Page 6: Main Analysis Execution
 *
 * STAGE 6: Main Analysis Execution
 *
 * Two main workflows:
 * 1. PROCESS SETUP - Configure assay before running
 * 2. DATA CAPTURE - Import/enter results after assay completion
 *
 * Primary Assays:
 *
 * A. ELISA (Enzyme-Linked Immunosorbent Assay)
 * Process: Plate preparation, Sample/standard loading, Incubation steps,
 *          Washing steps, Substrate addition, Optical density reading
 * Data Capture: Plate layout, Standards curve data, Sample OD values,
 *               Calculated concentrations, Controls, Assay run ID
 *
 * B. Flow Cytometry
 * Process: Cell staining with fluorescent antibodies, Acquisition on flow cytometer,
 *          Data analysis (gating strategy)
 * Data Capture: Antibody panel, Staining protocol, Compensation settings,
 *               Acquisition parameters, FCS files, Gating strategy,
 *               Population percentages and counts, MFI values
 */
function AnalysisPage({ entryId, pageData, progress, onProgressUpdate }) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // Main tab state (0 = Process Setup, 1 = Data Capture)
  const [activeMainTab, setActiveMainTab] = useState(0);

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Assay type selection
  const [assayType, setAssayType] = useState("ELISA");

  // ============ PROCESS SETUP STATE ============
  // Active assay runs (setup configurations ready for data capture)
  const [assayRuns, setAssayRuns] = useState([]);
  const [selectedAssayRunId, setSelectedAssayRunId] = useState(null);
  const [showSetupModal, setShowSetupModal] = useState(false);
  const [savingSetup, setSavingSetup] = useState(false);

  // ELISA Process Setup
  const [elisaSetup, setElisaSetup] = useState({
    assayRunId: "",
    assayName: "",
    operatorName: "",
    setupDate: new Date().toISOString().split("T")[0],
    // Selected Inventory Items (from inventory system)
    selectedReagents: [], // Array of reagent IDs from inventory
    selectedInstruments: [], // Array of instrument IDs from inventory
    // Equipment (manual entry fallback)
    plateReaderModel: "",
    plateReaderSerial: "",
    wavelength: "450",
    referenceWavelength: "570",
    calibrationStatus: "",
    calibrationDate: "",
    // Plate Layout
    plateLayoutDescription: "",
    standardsPositions: "",
    positiveControlPositions: "",
    negativeControlPositions: "",
    blankPositions: "",
    // Process Parameters
    incubationTemp: "37",
    primaryIncubationTime: "",
    secondaryIncubationTime: "",
    washCycles: "3",
    substrateIncubationTime: "",
    // Standards Curve
    standardConcentrations: "",
    // Process Step Completion
    platePreparationComplete: false,
    sampleLoadingComplete: false,
    incubationComplete: false,
    washingComplete: false,
    substrateComplete: false,
    odReadingComplete: false,
    // Notes
    notes: "",
    status: "SETUP", // SETUP, IN_PROGRESS, COMPLETED
  });

  // Flow Cytometry Process Setup
  const [flowSetup, setFlowSetup] = useState({
    assayRunId: "",
    assayName: "",
    operatorName: "",
    setupDate: new Date().toISOString().split("T")[0],
    panelName: "",
    // Selected Inventory Items (from inventory system)
    selectedReagents: [], // Array of reagent IDs (antibodies, staining reagents)
    selectedInstruments: [], // Array of instrument IDs (flow cytometers)
    // Instrument (manual entry fallback)
    instrumentModel: "",
    instrumentSerial: "",
    calibrationStatus: "",
    calibrationDate: "",
    lastCSTDate: "", // Cytometer Setup & Tracking
    // Antibody Panel (manual entry)
    antibodies: "", // Free text for antibody panel details
    // Staining Protocol
    stainingProtocol: "",
    lysisMethod: "",
    fixationMethod: "",
    washBuffer: "",
    // Acquisition Parameters
    targetEvents: "10000",
    flowRate: "Medium",
    thresholdParameter: "FSC",
    thresholdValue: "",
    voltageSettings: "",
    // Compensation
    compensationMethod: "",
    compensationBeads: "",
    compensationDate: "",
    // Gating Strategy
    gatingTemplate: "",
    gatingDescription: "",
    // Process Step Completion
    stainingComplete: false,
    acquisitionComplete: false,
    analysisComplete: false,
    // Notes
    notes: "",
    status: "SETUP",
  });

  // ============ DATA CAPTURE STATE ============
  // Import state
  const [showImportModal, setShowImportModal] = useState(false);
  const [importStep, setImportStep] = useState(1); // 1=select run, 2=upload, 3=map, 4=preview, 5=confirm
  const [uploadedFile, setUploadedFile] = useState(null);
  const [parseResult, setParseResult] = useState(null);
  const [columnMapping, setColumnMapping] = useState({});
  const [autoMappedColumns, setAutoMappedColumns] = useState({});
  const [previewData, setPreviewData] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importProgress, setImportProgress] = useState(0);

  // Import summary
  const [importSummary, setImportSummary] = useState(null);

  // Completing state
  const [completing, setCompleting] = useState(false);

  // Manual entry modal state
  const [showManualEntryModal, setShowManualEntryModal] = useState(false);
  const [manualEntryData, setManualEntryData] = useState({
    result: "",
    assayRunId: "",
    notes: "",
    assayType: "ELISA",
    // ELISA-specific
    odValue: "",
    concentration: "",
    // Flow-specific
    cd4Percent: "",
    cd4Count: "",
    cd4Mfi: "",
  });
  const [savingManualEntry, setSavingManualEntry] = useState(false);

  // View assay run details modal
  const [showRunDetailsModal, setShowRunDetailsModal] = useState(false);
  const [selectedRunDetails, setSelectedRunDetails] = useState(null);

  // Assay type options
  const assayTypes = [
    {
      id: "ELISA",
      text: intl.formatMessage({
        id: "notebook.analysis.elisa",
        defaultMessage: "ELISA (Enzyme-Linked Immunosorbent Assay)",
      }),
    },
    {
      id: "FLOW_CYTOMETRY",
      text: intl.formatMessage({
        id: "notebook.analysis.flowCytometry",
        defaultMessage: "Flow Cytometry",
      }),
    },
  ];

  // ============ INVENTORY REAGENTS & INSTRUMENTS STATE ============
  const [reagents, setReagents] = useState([]);
  const [instruments, setInstruments] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);
  const [loadingInstruments, setLoadingInstruments] = useState(false);

  // Auto-mapping configuration for known column headers
  const autoMappingConfig = {
    wellCoordinate: [
      "well",
      "well_position",
      "position",
      "tube",
      "tube_number",
      "plate_position",
    ],
    externalId: [
      "sample_id",
      "sampleid",
      "sample",
      "specimen_id",
      "external_id",
      "accession",
      "id",
      "barcode",
    ],
    result: [
      "result",
      "results",
      "interpretation",
      "status",
      "finding",
      "conclusion",
    ],
    odValue: [
      "od_value",
      "odvalue",
      "od",
      "optical_density",
      "absorbance",
      "reading",
    ],
    concentration: [
      "concentration",
      "conc",
      "pg_ml",
      "ng_ml",
      "iu_ml",
      "value",
    ],
    populationPercent: [
      "cd3_percent",
      "cd4_percent",
      "cd8_percent",
      "lymphocyte_percent",
      "population_percent",
      "percent",
    ],
    mfi: ["cd4_mfi", "cd8_mfi", "mfi", "mean_fluorescence", "mfi_value"],
    sampleType: [
      "sample_type",
      "sampletype",
      "type",
      "specimen_type",
      "sample_category",
    ],
  };

  // Function to perform auto-mapping based on headers
  const performAutoMapping = useCallback(
    (headers, currentAssayType) => {
      if (!headers || headers.length === 0) return {};

      const mapping = {};
      const autoMapped = {};
      const normalizedHeaders = headers.map((h) =>
        h.toLowerCase().replace(/[\s-]/g, "_"),
      );

      const fieldsToMap =
        currentAssayType === "ELISA"
          ? [
              "wellCoordinate",
              "externalId",
              "result",
              "odValue",
              "concentration",
              "sampleType",
            ]
          : [
              "wellCoordinate",
              "externalId",
              "result",
              "populationPercent",
              "mfi",
              "sampleType",
            ];

      fieldsToMap.forEach((field) => {
        const patterns = autoMappingConfig[field] || [];
        for (const pattern of patterns) {
          const normalizedPattern = pattern
            .toLowerCase()
            .replace(/[\s-]/g, "_");
          const matchIndex = normalizedHeaders.findIndex(
            (h) => h === normalizedPattern || h.includes(normalizedPattern),
          );
          if (matchIndex !== -1 && !mapping[field]) {
            mapping[field] = headers[matchIndex];
            autoMapped[field] = headers[matchIndex];
            break;
          }
        }
      });

      return { mapping, autoMapped };
    },
    [autoMappingConfig],
  );

  // Load samples for this page
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
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              data: sample.data,
              analyzerResult: sample.data?.analyzerResult || null,
              assayRunId: sample.data?.analyzerResult?.assayRunId || "",
              resultValue: sample.data?.analyzerResult?.result || "",
              importDate: sample.data?.analyzerResult?.importDate || "",
              assayType: sample.data?.analyzerResult?.assayType || "",
              odValue: sample.data?.analyzerResult?.odValue || "",
              concentration: sample.data?.analyzerResult?.concentration || "",
              mfi: sample.data?.analyzerResult?.mfi || "",
              populationPercent:
                sample.data?.analyzerResult?.populationPercent || "",
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

  // Load assay runs for this page
  const loadAssayRuns = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/assay-runs`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setAssayRuns(response);
        }
      },
    );
  }, [pageData?.id]);

  // Load import summary
  const loadImportSummary = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    getFromOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import/summary`,
      (response) => {
        if (componentMounted.current && response && !response.error) {
          setImportSummary(response);
        }
      },
    );
  }, [pageData?.id]);

  // Load reagents from inventory system
  const loadReagents = useCallback(() => {
    setLoadingReagents(true);
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setReagents(
              response.map((r) => ({
                id: String(r.id || r.itemId),
                label: `${r.name}${r.lotNumber ? ` (Lot: ${r.lotNumber})` : ""}${r.expirationDate ? ` - Exp: ${r.expirationDate}` : ""}`,
                name: r.name,
                lotNumber: r.lotNumber,
                lotId: r.lotId,
                expirationDate: r.expirationDate,
                manufacturer: r.manufacturer,
                category: r.category,
                qcStatus: r.qcStatus,
                currentQuantity: r.currentQuantity,
              })),
            );
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  // Load instruments from inventory system
  const loadInstruments = useCallback(() => {
    setLoadingInstruments(true);
    getFromOpenElisServer(
      "/rest/inventory/instruments?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setInstruments(
              response.map((i) => ({
                id: String(i.id || i.itemId),
                label: `${i.name}${i.serialNumber ? ` (S/N: ${i.serialNumber})` : ""}${i.manufacturer ? ` - ${i.manufacturer}` : ""}`,
                name: i.name,
                serialNumber: i.serialNumber,
                lotId: i.lotId,
                manufacturer: i.manufacturer,
                category: i.category,
                calibrationRequired: i.calibrationRequired,
                qcStatus: i.qcStatus,
              })),
            );
          }
          setLoadingInstruments(false);
        }
      },
    );
  }, []);

  // Reset state and load data when page changes
  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();
    loadAssayRuns();
    loadImportSummary();
    loadReagents();
    loadInstruments();

    return () => {
      componentMounted.current = false;
    };
  }, [
    pageData?.id,
    loadSamples,
    loadAssayRuns,
    loadImportSummary,
    loadReagents,
    loadInstruments,
  ]);

  // Generate unique assay run ID
  const generateAssayRunId = () => {
    const prefix = assayType === "ELISA" ? "ELISA" : "FLOW";
    const date = new Date().toISOString().slice(0, 10).replace(/-/g, "");
    const random = Math.random().toString(36).substring(2, 6).toUpperCase();
    return `${prefix}-${date}-${random}`;
  };

  // Open setup modal for new assay run
  const handleOpenSetupModal = () => {
    const newRunId = generateAssayRunId();
    if (assayType === "ELISA") {
      setElisaSetup((prev) => ({
        ...prev,
        assayRunId: newRunId,
        setupDate: new Date().toISOString().split("T")[0],
        status: "SETUP",
      }));
    } else {
      setFlowSetup((prev) => ({
        ...prev,
        assayRunId: newRunId,
        setupDate: new Date().toISOString().split("T")[0],
        status: "SETUP",
      }));
    }
    setShowSetupModal(true);
  };

  // Save assay setup
  const handleSaveSetup = () => {
    const setup = assayType === "ELISA" ? elisaSetup : flowSetup;

    if (!setup.assayRunId || !setup.operatorName) {
      setError(
        intl.formatMessage({
          id: "notebook.analysis.setupRequired",
          defaultMessage: "Assay Run ID and Operator Name are required",
        }),
      );
      return;
    }

    setSavingSetup(true);
    setError(null);

    const payload = {
      ...setup,
      assayType,
      pageId: pageData.id,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/page/${pageData.id}/assay-runs`,
      JSON.stringify(payload),
      (response) => {
        setSavingSetup(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage({
              id: "notebook.analysis.setupSaved",
              defaultMessage:
                "Assay setup saved successfully. Ready for data capture.",
            }),
          );
          setShowSetupModal(false);
          loadAssayRuns();
          // Reset setup form
          resetSetupForm();
        } else {
          setError(response?.error || "Failed to save setup");
        }
      },
    );
  };

  // Reset setup form
  const resetSetupForm = () => {
    setElisaSetup({
      assayRunId: "",
      assayName: "",
      operatorName: "",
      setupDate: new Date().toISOString().split("T")[0],
      selectedReagents: [],
      selectedInstruments: [],
      plateReaderModel: "",
      plateReaderSerial: "",
      wavelength: "450",
      referenceWavelength: "570",
      calibrationStatus: "",
      calibrationDate: "",
      plateLayoutDescription: "",
      standardsPositions: "",
      positiveControlPositions: "",
      negativeControlPositions: "",
      blankPositions: "",
      incubationTemp: "37",
      primaryIncubationTime: "",
      secondaryIncubationTime: "",
      washCycles: "3",
      substrateIncubationTime: "",
      standardConcentrations: "",
      platePreparationComplete: false,
      sampleLoadingComplete: false,
      incubationComplete: false,
      washingComplete: false,
      substrateComplete: false,
      odReadingComplete: false,
      notes: "",
      status: "SETUP",
    });
    setFlowSetup({
      assayRunId: "",
      assayName: "",
      operatorName: "",
      setupDate: new Date().toISOString().split("T")[0],
      panelName: "",
      selectedReagents: [],
      selectedInstruments: [],
      instrumentModel: "",
      instrumentSerial: "",
      calibrationStatus: "",
      calibrationDate: "",
      lastCSTDate: "",
      antibodies: "",
      stainingProtocol: "",
      lysisMethod: "",
      fixationMethod: "",
      washBuffer: "",
      targetEvents: "10000",
      flowRate: "Medium",
      thresholdParameter: "FSC",
      thresholdValue: "",
      voltageSettings: "",
      compensationMethod: "",
      compensationBeads: "",
      compensationDate: "",
      gatingTemplate: "",
      gatingDescription: "",
      stainingComplete: false,
      acquisitionComplete: false,
      analysisComplete: false,
      notes: "",
      status: "SETUP",
    });
  };

  // Handle file upload
  const handleFileUpload = (event) => {
    const file = event.target?.files?.[0];
    if (!file) return;

    setUploadedFile(file);
    setError(null);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("assayType", assayType);

    postToOpenElisServerFormDataJson(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import/parse`,
      formData,
      (response) => {
        if (response && response.success) {
          setParseResult(response);
          if (response.headers && response.headers.length > 0) {
            const { mapping, autoMapped } = performAutoMapping(
              response.headers,
              assayType,
            );
            setColumnMapping(mapping);
            setAutoMappedColumns(autoMapped);
          }
          setImportStep(3);
        } else {
          setError(response?.error || "Failed to parse file");
        }
      },
    );
  };

  // Handle column mapping change
  const handleMappingChange = (targetField, sourceColumn) => {
    setColumnMapping((prev) => ({
      ...prev,
      [targetField]: sourceColumn,
    }));
  };

  // Generate preview
  const handleGeneratePreview = () => {
    if (!uploadedFile) {
      setError("Please upload a file first");
      return;
    }

    setError(null);
    const formData = new FormData();
    formData.append("file", uploadedFile);
    formData.append("assayType", assayType);
    Object.entries(columnMapping).forEach(([key, value]) => {
      formData.append(key, value);
    });

    postToOpenElisServerFormDataJson(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import/preview`,
      formData,
      (response) => {
        if (response && response.success) {
          setPreviewData(response);
          setImportStep(4);
        } else {
          setError(response?.error || "Failed to generate preview");
        }
      },
    );
  };

  // Execute import
  const handleExecuteImport = () => {
    if (!uploadedFile || !selectedAssayRunId) {
      setError("Please select an assay run and upload a file");
      return;
    }

    setImporting(true);
    setImportProgress(0);
    setError(null);

    const formData = new FormData();
    formData.append("file", uploadedFile);
    formData.append("assayType", assayType);
    formData.append("assayRunId", selectedAssayRunId);

    Object.entries(columnMapping).forEach(([key, value]) => {
      formData.append(key, value);
    });

    postToOpenElisServerFormDataJson(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import`,
      formData,
      (response) => {
        setImporting(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.analysis.importSuccess",
                defaultMessage:
                  "Successfully imported {count} of {total} results",
              },
              { count: response.successfulRows, total: response.totalRows },
            ),
          );
          setShowImportModal(false);
          resetImportState();
          loadSamples();
          loadImportSummary();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(response?.error || "Import failed");
        }
      },
    );
  };

  // Mark samples complete
  const handleMarkComplete = (sampleIdsToComplete = null) => {
    let idsToComplete = sampleIdsToComplete;

    if (!idsToComplete) {
      if (selectedIds.length > 0) {
        idsToComplete = selectedIds.map((id) => parseInt(id, 10));
      } else {
        const samplesWithResults = samples.filter(
          (s) => s.status === "IN_PROGRESS" && s.data?.analyzerResult,
        );
        if (samplesWithResults.length === 0) {
          setError(
            intl.formatMessage({
              id: "notebook.analysis.noSamplesToComplete",
              defaultMessage: "No samples to mark complete",
            }),
          );
          return;
        }
        idsToComplete = samplesWithResults.map((s) => parseInt(s.id, 10));
      }
    }

    if (idsToComplete.length === 0) return;

    setCompleting(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: idsToComplete, status: "COMPLETED" }),
      (response) => {
        setCompleting(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.analysis.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete",
              },
              { count: response.updatedCount },
            ),
          );
          setSelectedIds([]);
          loadSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(response?.error || "Failed to mark samples complete");
        }
      },
    );
  };

  // Handle status change for individual samples
  const handleStatusChange = (sampleId, newStatus) => {
    if (newStatus === "COMPLETED") {
      handleMarkComplete([parseInt(sampleId, 10)]);
    }
  };

  // Handle manual result entry
  const handleOpenManualEntry = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.analysis.selectSamplesFirst",
          defaultMessage: "Please select at least one sample to add results to",
        }),
      );
      return;
    }
    setManualEntryData({
      result: "",
      assayRunId: selectedAssayRunId || "",
      notes: "",
      assayType: assayType,
      odValue: "",
      concentration: "",
      cd4Percent: "",
      cd4Count: "",
      cd4Mfi: "",
    });
    setShowManualEntryModal(true);
  };

  // Save manual results
  const handleSaveManualResults = () => {
    if (!manualEntryData.result.trim()) {
      setError(
        intl.formatMessage({
          id: "notebook.analysis.resultRequired",
          defaultMessage: "Result value is required",
        }),
      );
      return;
    }

    setSavingManualEntry(true);
    setError(null);

    const request = {
      sampleIds: selectedIds.map((id) => parseInt(id, 10)),
      result: manualEntryData.result.trim(),
      assayRunId: manualEntryData.assayRunId.trim() || null,
      notes: manualEntryData.notes.trim() || null,
      entryType: "MANUAL",
      assayType: manualEntryData.assayType,
      odValue: manualEntryData.odValue || null,
      concentration: manualEntryData.concentration || null,
      cd4Percent: manualEntryData.cd4Percent || null,
      cd4Count: manualEntryData.cd4Count || null,
      cd4Mfi: manualEntryData.cd4Mfi || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/manual-results`,
      JSON.stringify(request),
      (response) => {
        setSavingManualEntry(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.analysis.manualEntrySaved",
                defaultMessage:
                  "Successfully added results to {count} sample(s)",
              },
              { count: response.updatedCount || selectedIds.length },
            ),
          );
          setShowManualEntryModal(false);
          setSelectedIds([]);
          loadSamples();
          loadImportSummary();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(response?.error || "Failed to save manual results");
        }
      },
    );
  };

  // Reset import modal state
  const resetImportState = () => {
    setImportStep(1);
    setUploadedFile(null);
    setParseResult(null);
    setColumnMapping({});
    setAutoMappedColumns({});
    setPreviewData(null);
    setSelectedAssayRunId(null);
  };

  // View assay run details
  const handleViewRunDetails = (run) => {
    setSelectedRunDetails(run);
    setShowRunDetailsModal(true);
  };

  // Render result info column
  const renderResultInfo = (value, sample) => {
    const s = sample || value;
    if (s?.analyzerResult) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag
            type={s.assayType === "FLOW_CYTOMETRY" ? "purple" : "green"}
            size="sm"
          >
            {s.assayType === "FLOW_CYTOMETRY" ? "Flow" : "ELISA"}
          </Tag>
          {s.resultValue && (
            <span style={{ marginLeft: "4px" }}>{s.resultValue}</span>
          )}
          {s.odValue && (
            <span style={{ marginLeft: "4px", color: "#525252" }}>
              OD: {s.odValue}
            </span>
          )}
          {s.mfi && (
            <span style={{ marginLeft: "4px", color: "#525252" }}>
              MFI: {s.mfi}
            </span>
          )}
          {s.assayRunId && (
            <div style={{ marginTop: "2px", color: "#8d8d8d" }}>
              Run: {s.assayRunId}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.analysis.noResult"
          defaultMessage="No result imported"
        />
      </span>
    );
  };

  // ============ RENDER ELISA SETUP FORM ============
  // ELISA Process Steps:
  // 1. Plate preparation
  // 2. Sample/standard loading
  // 3. Incubation steps
  // 4. Washing steps
  // 5. Substrate addition
  // 6. Optical density reading
  const renderElisaSetupForm = () => (
    <div>
      {/* Run Identification Header */}
      <Grid narrow style={{ marginBottom: "1.5rem" }}>
        <Column lg={6} md={4} sm={4}>
          <TextInput
            id="elisa-run-id"
            labelText="Assay Run ID *"
            value={elisaSetup.assayRunId}
            readOnly
            size="sm"
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="elisa-operator"
            labelText="Operator Name *"
            value={elisaSetup.operatorName}
            onChange={(e) =>
              setElisaSetup({ ...elisaSetup, operatorName: e.target.value })
            }
            size="sm"
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="elisa-name"
            labelText="Assay Name"
            value={elisaSetup.assayName}
            onChange={(e) =>
              setElisaSetup({ ...elisaSetup, assayName: e.target.value })
            }
            placeholder="e.g., HIV p24 ELISA"
            size="sm"
          />
        </Column>
      </Grid>

      <Accordion>
        {/* Reagent & Instrument Selection from Inventory */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Chemistry size={16} />
              <span>
                <FormattedMessage
                  id="notebook.analysis.reagentInstrumentSelection"
                  defaultMessage="Reagent & Instrument Selection"
                />
              </span>
              {elisaSetup.selectedReagents.length > 0 &&
                elisaSetup.selectedInstruments.length > 0 && (
                  <CheckmarkFilled
                    size={16}
                    style={{ color: "#24a148", marginLeft: "auto" }}
                  />
                )}
            </div>
          }
          open
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            <FormattedMessage
              id="notebook.analysis.selectFromInventory"
              defaultMessage="Select reagents and instruments from your laboratory inventory. This ensures proper lot tracking and inventory consumption."
            />
          </p>
          <Grid narrow>
            <Column lg={8} md={8} sm={4}>
              <MultiSelect
                id="elisa-reagents"
                titleText={intl.formatMessage({
                  id: "notebook.analysis.reagents",
                  defaultMessage: "Reagents (ELISA Kit, Antibodies, Buffers)",
                })}
                label={loadingReagents ? "Loading..." : "Select reagents..."}
                items={reagents}
                itemToString={(item) => (item ? item.label : "")}
                selectedItems={reagents.filter((r) =>
                  elisaSetup.selectedReagents.includes(r.id),
                )}
                onChange={({ selectedItems }) =>
                  setElisaSetup({
                    ...elisaSetup,
                    selectedReagents: selectedItems.map((r) => r.id),
                  })
                }
                disabled={loadingReagents}
              />
              {elisaSetup.selectedReagents.length > 0 && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.75rem",
                    color: "#0f62fe",
                  }}
                >
                  {elisaSetup.selectedReagents.length} reagent(s) selected
                </p>
              )}
            </Column>
            <Column lg={8} md={8} sm={4}>
              <MultiSelect
                id="elisa-instruments"
                titleText={intl.formatMessage({
                  id: "notebook.analysis.instruments",
                  defaultMessage: "Instruments (Plate Reader)",
                })}
                label={
                  loadingInstruments ? "Loading..." : "Select instruments..."
                }
                items={instruments}
                itemToString={(item) => (item ? item.label : "")}
                selectedItems={instruments.filter((i) =>
                  elisaSetup.selectedInstruments.includes(i.id),
                )}
                onChange={({ selectedItems }) =>
                  setElisaSetup({
                    ...elisaSetup,
                    selectedInstruments: selectedItems.map((i) => i.id),
                  })
                }
                disabled={loadingInstruments}
              />
              {elisaSetup.selectedInstruments.length > 0 && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.75rem",
                    color: "#0f62fe",
                  }}
                >
                  {elisaSetup.selectedInstruments.length} instrument(s) selected
                </p>
              )}
            </Column>
          </Grid>
          {(reagents.length === 0 || instruments.length === 0) &&
            !loadingReagents &&
            !loadingInstruments && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "notebook.analysis.inventoryInfo",
                  defaultMessage: "Inventory Note",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.analysis.inventoryInfoMessage",
                  defaultMessage:
                    "Add reagents and instruments in the Inventory Management module for lot tracking.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            )}
        </AccordionItem>

        {/* PROCESS STEP 1: Plate Preparation */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                Step 1
              </Tag>
              <span>Plate Preparation</span>
              {elisaSetup.platePreparationComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Prepare the ELISA plate with coating antibody and blocking buffer.
          </p>
          <Grid narrow>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="elisa-plate-desc"
                labelText="Plate Layout Description"
                value={elisaSetup.plateLayoutDescription}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    plateLayoutDescription: e.target.value,
                  })
                }
                placeholder="Describe plate coating and blocking procedure"
                rows={2}
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="elisa-plate-prep-complete"
                labelText="Plate preparation completed"
                checked={elisaSetup.platePreparationComplete || false}
                onChange={(e, { checked }) =>
                  setElisaSetup({
                    ...elisaSetup,
                    platePreparationComplete: checked,
                  })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* PROCESS STEP 2: Sample/Standard Loading */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                Step 2
              </Tag>
              <span>Sample/Standard Loading</span>
              {elisaSetup.sampleLoadingComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Load samples, standards, and controls onto the prepared plate.
          </p>
          <Grid narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="elisa-standards-pos"
                labelText="Standards Positions"
                value={elisaSetup.standardsPositions}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    standardsPositions: e.target.value,
                  })
                }
                placeholder="e.g., A1-A8"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="elisa-standard-conc"
                labelText="Standard Concentrations"
                value={elisaSetup.standardConcentrations}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    standardConcentrations: e.target.value,
                  })
                }
                placeholder="e.g., 0, 10, 25, 50, 100, 200, 400, 800 pg/mL"
              />
            </Column>
            <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="elisa-pos-ctrl"
                labelText="Positive Control Positions"
                value={elisaSetup.positiveControlPositions}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    positiveControlPositions: e.target.value,
                  })
                }
                placeholder="e.g., B1, B2"
              />
            </Column>
            <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="elisa-neg-ctrl"
                labelText="Negative Control Positions"
                value={elisaSetup.negativeControlPositions}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    negativeControlPositions: e.target.value,
                  })
                }
                placeholder="e.g., B3, B4"
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="elisa-sample-loading-complete"
                labelText="Sample and standard loading completed"
                checked={elisaSetup.sampleLoadingComplete || false}
                onChange={(e, { checked }) =>
                  setElisaSetup({
                    ...elisaSetup,
                    sampleLoadingComplete: checked,
                  })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* PROCESS STEP 3: Incubation Steps */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                Step 3
              </Tag>
              <span>Incubation Steps</span>
              {elisaSetup.incubationComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Primary and secondary incubation with detection antibody.
          </p>
          <Grid narrow>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="elisa-inc-temp"
                labelText="Incubation Temperature (°C)"
                value={elisaSetup.incubationTemp}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    incubationTemp: e.target.value,
                  })
                }
                placeholder="37"
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="elisa-primary-inc"
                labelText="Primary Incubation (min)"
                value={elisaSetup.primaryIncubationTime}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    primaryIncubationTime: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="elisa-secondary-inc"
                labelText="Secondary Incubation (min)"
                value={elisaSetup.secondaryIncubationTime}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    secondaryIncubationTime: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="elisa-incubation-complete"
                labelText="Incubation steps completed"
                checked={elisaSetup.incubationComplete || false}
                onChange={(e, { checked }) =>
                  setElisaSetup({ ...elisaSetup, incubationComplete: checked })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* PROCESS STEP 4: Washing Steps */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                Step 4
              </Tag>
              <span>Washing Steps</span>
              {elisaSetup.washingComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Wash plate to remove unbound material.
          </p>
          <Grid narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="elisa-wash-cycles"
                labelText="Number of Wash Cycles"
                value={elisaSetup.washCycles}
                onChange={(e) =>
                  setElisaSetup({ ...elisaSetup, washCycles: e.target.value })
                }
                placeholder="3"
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="elisa-washing-complete"
                labelText="Washing steps completed"
                checked={elisaSetup.washingComplete || false}
                onChange={(e, { checked }) =>
                  setElisaSetup({ ...elisaSetup, washingComplete: checked })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* PROCESS STEP 5: Substrate Addition */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                Step 5
              </Tag>
              <span>Substrate Addition</span>
              {elisaSetup.substrateComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Add substrate solution and stop solution after color development.
          </p>
          <Grid narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="elisa-substrate-inc"
                labelText="Substrate Incubation (min)"
                value={elisaSetup.substrateIncubationTime}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    substrateIncubationTime: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="elisa-substrate-complete"
                labelText="Substrate addition and reaction stopped"
                checked={elisaSetup.substrateComplete || false}
                onChange={(e, { checked }) =>
                  setElisaSetup({ ...elisaSetup, substrateComplete: checked })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* PROCESS STEP 6: Optical Density Reading */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                Step 6
              </Tag>
              <span>Optical Density Reading</span>
              {elisaSetup.odReadingComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Read plate absorbance on plate reader.
          </p>
          <Grid narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="elisa-reader-model"
                labelText="Plate Reader Model"
                value={elisaSetup.plateReaderModel}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    plateReaderModel: e.target.value,
                  })
                }
                placeholder="e.g., BioTek Synergy H1"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="elisa-reader-serial"
                labelText="Serial Number"
                value={elisaSetup.plateReaderSerial}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    plateReaderSerial: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={4} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="elisa-wavelength"
                labelText="Wavelength (nm)"
                value={elisaSetup.wavelength}
                onChange={(e) =>
                  setElisaSetup({ ...elisaSetup, wavelength: e.target.value })
                }
                placeholder="450"
              />
            </Column>
            <Column lg={4} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="elisa-ref-wavelength"
                labelText="Reference (nm)"
                value={elisaSetup.referenceWavelength}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    referenceWavelength: e.target.value,
                  })
                }
                placeholder="570"
              />
            </Column>
            <Column lg={4} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <Select
                id="elisa-calibration"
                labelText="Calibration Status"
                value={elisaSetup.calibrationStatus}
                onChange={(e) =>
                  setElisaSetup({
                    ...elisaSetup,
                    calibrationStatus: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select..." />
                <SelectItem value="CALIBRATED" text="Calibrated" />
                <SelectItem value="DUE_SOON" text="Due Soon" />
                <SelectItem value="OVERDUE" text="Overdue" />
              </Select>
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="elisa-od-complete"
                labelText="OD reading completed - ready for data capture"
                checked={elisaSetup.odReadingComplete || false}
                onChange={(e, { checked }) =>
                  setElisaSetup({ ...elisaSetup, odReadingComplete: checked })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* Notes */}
        <AccordionItem
          title={intl.formatMessage({
            id: "notebook.analysis.notes",
            defaultMessage: "Notes & Observations",
          })}
        >
          <TextArea
            id="elisa-notes"
            labelText="Additional Notes"
            value={elisaSetup.notes}
            onChange={(e) =>
              setElisaSetup({ ...elisaSetup, notes: e.target.value })
            }
            placeholder="Any deviations from protocol, observations, or issues encountered..."
            rows={3}
          />
        </AccordionItem>
      </Accordion>
    </div>
  );

  // ============ RENDER FLOW CYTOMETRY SETUP FORM ============
  // Flow Cytometry Process Steps:
  // 1. Cell staining with fluorescent antibodies
  // 2. Acquisition on flow cytometer
  // 3. Data analysis (gating strategy)
  const renderFlowSetupForm = () => (
    <div>
      {/* Run Identification Header */}
      <Grid narrow style={{ marginBottom: "1.5rem" }}>
        <Column lg={6} md={4} sm={4}>
          <TextInput
            id="flow-run-id"
            labelText="Assay Run ID *"
            value={flowSetup.assayRunId}
            readOnly
            size="sm"
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="flow-operator"
            labelText="Operator Name *"
            value={flowSetup.operatorName}
            onChange={(e) =>
              setFlowSetup({ ...flowSetup, operatorName: e.target.value })
            }
            size="sm"
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="flow-panel-name"
            labelText="Panel Name"
            value={flowSetup.panelName}
            onChange={(e) =>
              setFlowSetup({ ...flowSetup, panelName: e.target.value })
            }
            placeholder="e.g., TBNK Panel, CD4 Panel"
            size="sm"
          />
        </Column>
      </Grid>

      <Accordion>
        {/* Reagent & Instrument Selection from Inventory */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Chemistry size={16} />
              <span>
                <FormattedMessage
                  id="notebook.analysis.reagentInstrumentSelection"
                  defaultMessage="Reagent & Instrument Selection"
                />
              </span>
              {flowSetup.selectedReagents.length > 0 &&
                flowSetup.selectedInstruments.length > 0 && (
                  <CheckmarkFilled
                    size={16}
                    style={{ color: "#24a148", marginLeft: "auto" }}
                  />
                )}
            </div>
          }
          open
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            <FormattedMessage
              id="notebook.analysis.selectFlowFromInventory"
              defaultMessage="Select antibodies, staining reagents, and flow cytometer from your laboratory inventory."
            />
          </p>
          <Grid narrow>
            <Column lg={8} md={8} sm={4}>
              <MultiSelect
                id="flow-reagents"
                titleText={intl.formatMessage({
                  id: "notebook.analysis.flowReagents",
                  defaultMessage:
                    "Reagents (Antibodies, Lysis Buffer, Staining)",
                })}
                label={loadingReagents ? "Loading..." : "Select reagents..."}
                items={reagents}
                itemToString={(item) => (item ? item.label : "")}
                selectedItems={reagents.filter((r) =>
                  flowSetup.selectedReagents.includes(r.id),
                )}
                onChange={({ selectedItems }) =>
                  setFlowSetup({
                    ...flowSetup,
                    selectedReagents: selectedItems.map((r) => r.id),
                  })
                }
                disabled={loadingReagents}
              />
              {flowSetup.selectedReagents.length > 0 && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.75rem",
                    color: "#0f62fe",
                  }}
                >
                  {flowSetup.selectedReagents.length} reagent(s) selected
                </p>
              )}
            </Column>
            <Column lg={8} md={8} sm={4}>
              <MultiSelect
                id="flow-instruments"
                titleText={intl.formatMessage({
                  id: "notebook.analysis.flowInstruments",
                  defaultMessage: "Instruments (Flow Cytometer)",
                })}
                label={
                  loadingInstruments ? "Loading..." : "Select instruments..."
                }
                items={instruments}
                itemToString={(item) => (item ? item.label : "")}
                selectedItems={instruments.filter((i) =>
                  flowSetup.selectedInstruments.includes(i.id),
                )}
                onChange={({ selectedItems }) =>
                  setFlowSetup({
                    ...flowSetup,
                    selectedInstruments: selectedItems.map((i) => i.id),
                  })
                }
                disabled={loadingInstruments}
              />
              {flowSetup.selectedInstruments.length > 0 && (
                <p
                  style={{
                    marginTop: "0.25rem",
                    fontSize: "0.75rem",
                    color: "#0f62fe",
                  }}
                >
                  {flowSetup.selectedInstruments.length} instrument(s) selected
                </p>
              )}
            </Column>
          </Grid>
          {(reagents.length === 0 || instruments.length === 0) &&
            !loadingReagents &&
            !loadingInstruments && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "notebook.analysis.inventoryInfo",
                  defaultMessage: "Inventory Note",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.analysis.inventoryInfoMessage",
                  defaultMessage:
                    "Add reagents and instruments in the Inventory Management module for lot tracking.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            )}
        </AccordionItem>

        {/* PROCESS STEP 1: Cell Staining with Fluorescent Antibodies */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="purple" size="sm">
                Step 1
              </Tag>
              <span>Cell Staining with Fluorescent Antibodies</span>
              {flowSetup.stainingComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Prepare and stain samples with fluorescent antibody panel.
          </p>
          <Grid narrow>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="flow-antibodies"
                labelText="Antibody Panel Details"
                value={
                  typeof flowSetup.antibodies === "string"
                    ? flowSetup.antibodies
                    : ""
                }
                onChange={(e) =>
                  setFlowSetup({ ...flowSetup, antibodies: e.target.value })
                }
                placeholder="Format: Marker/Fluorochrome/Clone/Lot/Vendor&#10;e.g., CD3/FITC/UCHT1/12345/BD&#10;CD4/PE/SK3/23456/BD&#10;CD8/APC/SK1/34567/BD"
                rows={4}
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <TextArea
                id="flow-staining"
                labelText="Staining Protocol"
                value={flowSetup.stainingProtocol}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    stainingProtocol: e.target.value,
                  })
                }
                placeholder="Describe staining procedure (incubation times, volumes, wash steps)"
                rows={3}
              />
            </Column>
            <Column lg={5} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="flow-lysis"
                labelText="Lysis Method"
                value={flowSetup.lysisMethod}
                onChange={(e) =>
                  setFlowSetup({ ...flowSetup, lysisMethod: e.target.value })
                }
                placeholder="e.g., BD FACS Lysing Solution"
              />
            </Column>
            <Column lg={5} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="flow-fixation"
                labelText="Fixation Method"
                value={flowSetup.fixationMethod}
                onChange={(e) =>
                  setFlowSetup({ ...flowSetup, fixationMethod: e.target.value })
                }
                placeholder="e.g., 1% PFA, None"
              />
            </Column>
            <Column lg={6} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="flow-wash"
                labelText="Wash Buffer"
                value={flowSetup.washBuffer}
                onChange={(e) =>
                  setFlowSetup({ ...flowSetup, washBuffer: e.target.value })
                }
                placeholder="e.g., PBS + 1% BSA"
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="flow-staining-complete"
                labelText="Cell staining completed"
                checked={flowSetup.stainingComplete || false}
                onChange={(e, { checked }) =>
                  setFlowSetup({ ...flowSetup, stainingComplete: checked })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* PROCESS STEP 2: Acquisition on Flow Cytometer */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="purple" size="sm">
                Step 2
              </Tag>
              <span>Acquisition on Flow Cytometer</span>
              {flowSetup.acquisitionComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Run samples on flow cytometer and acquire data.
          </p>
          <Grid narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="flow-instrument"
                labelText="Instrument Model"
                value={flowSetup.instrumentModel}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    instrumentModel: e.target.value,
                  })
                }
                placeholder="e.g., BD FACSCanto II, BD LSRFortessa"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="flow-serial"
                labelText="Serial Number"
                value={flowSetup.instrumentSerial}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    instrumentSerial: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={4} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <Select
                id="flow-calibration"
                labelText="Calibration Status"
                value={flowSetup.calibrationStatus}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    calibrationStatus: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select..." />
                <SelectItem value="CALIBRATED" text="CS&T Passed" />
                <SelectItem value="DUE_SOON" text="Due Soon" />
                <SelectItem value="OVERDUE" text="Overdue" />
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="flow-events"
                labelText="Target Events"
                value={flowSetup.targetEvents}
                onChange={(e) =>
                  setFlowSetup({ ...flowSetup, targetEvents: e.target.value })
                }
                placeholder="10000"
              />
            </Column>
            <Column lg={4} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <Select
                id="flow-rate"
                labelText="Flow Rate"
                value={flowSetup.flowRate}
                onChange={(e) =>
                  setFlowSetup({ ...flowSetup, flowRate: e.target.value })
                }
              >
                <SelectItem value="Low" text="Low" />
                <SelectItem value="Medium" text="Medium" />
                <SelectItem value="High" text="High" />
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="flow-threshold"
                labelText="Threshold Parameter"
                value={flowSetup.thresholdParameter}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    thresholdParameter: e.target.value,
                  })
                }
                placeholder="FSC"
              />
            </Column>
            <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <Select
                id="flow-comp-method"
                labelText="Compensation Method"
                value={flowSetup.compensationMethod}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    compensationMethod: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select..." />
                <SelectItem value="AUTO" text="Automatic (BD FACSDiva)" />
                <SelectItem value="MANUAL" text="Manual" />
                <SelectItem value="SAVED" text="Saved Matrix" />
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
              <TextInput
                id="flow-comp-beads"
                labelText="Compensation Beads"
                value={flowSetup.compensationBeads}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    compensationBeads: e.target.value,
                  })
                }
                placeholder="e.g., BD CompBeads, UltraComp"
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <TextArea
                id="flow-voltages"
                labelText="Voltage/Gain Settings"
                value={flowSetup.voltageSettings}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    voltageSettings: e.target.value,
                  })
                }
                placeholder="e.g., FSC: 350, SSC: 280, FITC: 400, PE: 450, APC: 500"
                rows={2}
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="flow-acquisition-complete"
                labelText="Sample acquisition completed"
                checked={flowSetup.acquisitionComplete || false}
                onChange={(e, { checked }) =>
                  setFlowSetup({ ...flowSetup, acquisitionComplete: checked })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* PROCESS STEP 3: Data Analysis (Gating Strategy) */}
        <AccordionItem
          title={
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="purple" size="sm">
                Step 3
              </Tag>
              <span>Data Analysis (Gating Strategy)</span>
              {flowSetup.analysisComplete && (
                <CheckmarkFilled
                  size={16}
                  style={{ color: "#24a148", marginLeft: "auto" }}
                />
              )}
            </div>
          }
        >
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Apply gating strategy and analyze population data.
          </p>
          <Grid narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="flow-gating-template"
                labelText="Gating Template Name"
                value={flowSetup.gatingTemplate}
                onChange={(e) =>
                  setFlowSetup({ ...flowSetup, gatingTemplate: e.target.value })
                }
                placeholder="e.g., TBNK_Standard_v2"
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <TextArea
                id="flow-gating-desc"
                labelText="Gating Strategy Description"
                value={flowSetup.gatingDescription}
                onChange={(e) =>
                  setFlowSetup({
                    ...flowSetup,
                    gatingDescription: e.target.value,
                  })
                }
                placeholder="e.g., Lymphocytes (FSC/SSC) → Singlets (FSC-H/FSC-A) → CD45+ → CD3+ → CD4+/CD8+"
                rows={3}
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Checkbox
                id="flow-analysis-complete"
                labelText="Data analysis completed - ready for data capture"
                checked={flowSetup.analysisComplete || false}
                onChange={(e, { checked }) =>
                  setFlowSetup({ ...flowSetup, analysisComplete: checked })
                }
              />
            </Column>
          </Grid>
        </AccordionItem>

        {/* Notes */}
        <AccordionItem
          title={intl.formatMessage({
            id: "notebook.analysis.notes",
            defaultMessage: "Notes & Observations",
          })}
        >
          <TextArea
            id="flow-notes"
            labelText="Additional Notes"
            value={flowSetup.notes}
            onChange={(e) =>
              setFlowSetup({ ...flowSetup, notes: e.target.value })
            }
            placeholder="Any deviations from protocol, observations, or issues encountered..."
            rows={3}
          />
        </AccordionItem>
      </Accordion>
    </div>
  );

  // ============ RENDER ASSAY RUNS LIST ============
  const renderAssayRunsList = () => (
    <div style={{ marginTop: "1rem" }}>
      <h5 style={{ marginBottom: "0.5rem" }}>
        <FormattedMessage
          id="notebook.analysis.activeRuns"
          defaultMessage="Active Assay Runs"
        />
      </h5>
      {assayRuns.length === 0 ? (
        <p style={{ color: "#525252", fontStyle: "italic" }}>
          <FormattedMessage
            id="notebook.analysis.noRuns"
            defaultMessage="No assay runs configured yet. Create a new setup to begin."
          />
        </p>
      ) : (
        <StructuredListWrapper>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>Run ID</StructuredListCell>
              <StructuredListCell head>Type</StructuredListCell>
              <StructuredListCell head>Operator</StructuredListCell>
              <StructuredListCell head>Date</StructuredListCell>
              <StructuredListCell head>Status</StructuredListCell>
              <StructuredListCell head>Actions</StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {assayRuns.map((run) => (
              <StructuredListRow key={run.assayRunId}>
                <StructuredListCell>{run.assayRunId}</StructuredListCell>
                <StructuredListCell>
                  <Tag
                    type={
                      run.assayType === "FLOW_CYTOMETRY" ? "purple" : "green"
                    }
                    size="sm"
                  >
                    {run.assayType === "FLOW_CYTOMETRY" ? "Flow" : "ELISA"}
                  </Tag>
                </StructuredListCell>
                <StructuredListCell>{run.operatorName}</StructuredListCell>
                <StructuredListCell>{run.setupDate}</StructuredListCell>
                <StructuredListCell>
                  <Tag
                    type={
                      run.status === "COMPLETED"
                        ? "green"
                        : run.status === "IN_PROGRESS"
                          ? "blue"
                          : "gray"
                    }
                    size="sm"
                  >
                    {run.status}
                  </Tag>
                </StructuredListCell>
                <StructuredListCell>
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={View}
                    iconDescription="View Details"
                    hasIconOnly
                    onClick={() => handleViewRunDetails(run)}
                  />
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
    </div>
  );

  // ============ RENDER IMPORT STEP CONTENT ============
  const renderImportStep = () => {
    switch (importStep) {
      case 1:
        // Step 1: Select assay run
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.selectRunForImport"
                defaultMessage="Select the assay run to associate with the imported results. The run setup contains equipment, reagent, and process information."
              />
            </p>
            {assayRuns.length === 0 ? (
              <InlineNotification
                kind="warning"
                title="No Assay Runs"
                subtitle="Please create an assay setup in the Process Setup tab before importing results."
                hideCloseButton
              />
            ) : (
              <>
                <Dropdown
                  id="select-assay-run"
                  titleText="Select Assay Run"
                  label="Choose a run..."
                  items={assayRuns.filter((r) => r.status !== "COMPLETED")}
                  itemToString={(item) =>
                    item
                      ? `${item.assayRunId} - ${item.operatorName} (${item.setupDate})`
                      : ""
                  }
                  onChange={({ selectedItem }) => {
                    setSelectedAssayRunId(selectedItem?.assayRunId || null);
                    setAssayType(selectedItem?.assayType || "ELISA");
                  }}
                  style={{ marginBottom: "1rem" }}
                />
                <Button
                  kind="primary"
                  onClick={() => setImportStep(2)}
                  disabled={!selectedAssayRunId}
                >
                  <FormattedMessage
                    id="notebook.analysis.continueToUpload"
                    defaultMessage="Continue to File Upload"
                  />
                </Button>
              </>
            )}
          </div>
        );

      case 2:
        // Step 2: Upload file
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.uploadInstructions"
                defaultMessage="Upload a CSV or Excel file containing {assayType} results."
                values={{
                  assayType: assayType === "ELISA" ? "ELISA" : "Flow Cytometry",
                }}
              />
            </p>

            {/* Show expected format */}
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h6 style={{ marginBottom: "0.5rem" }}>Expected CSV Format</h6>
              <code
                style={{
                  display: "block",
                  padding: "0.5rem",
                  backgroundColor: "#e0e0e0",
                  borderRadius: "4px",
                  fontSize: "0.75rem",
                }}
              >
                {assayType === "ELISA" ? (
                  <>
                    Well,Sample_ID,OD_Value,Concentration,Result
                    <br />
                    A1,STD1,0.050,0,Standard
                    <br />
                    C1,SAMPLE001,0.450,45.5,Positive
                  </>
                ) : (
                  <>
                    Tube,Sample_ID,CD3_Percent,CD4_Percent,CD8_Percent,CD4_MFI,Result
                    <br />
                    1,SAMPLE001,68.5,42.3,25.1,15200,Normal
                    <br />
                    2,SAMPLE002,55.2,18.5,35.8,8500,Abnormal
                  </>
                )}
              </code>
            </div>

            <FileUploader
              accept={[".csv", ".xlsx", ".xls"]}
              buttonLabel="Select file"
              filenameStatus="edit"
              iconDescription="Clear file"
              labelDescription="Max file size: 10MB. Formats: CSV, XLSX, XLS"
              labelTitle="Upload analyzer results"
              onChange={handleFileUpload}
            />
            {parseResult && (
              <div style={{ marginTop: "1rem" }}>
                <Tag type="blue">{parseResult.totalRows} rows found</Tag>
              </div>
            )}
          </div>
        );

      case 3:
        // Step 3: Column mapping with auto-detect
        const renderAutoMappedTag = (fieldName) => {
          if (autoMappedColumns[fieldName]) {
            return (
              <Tag type="green" size="sm" style={{ marginLeft: "0.5rem" }}>
                <CheckmarkFilled size={12} style={{ marginRight: "4px" }} />
                Auto-detected
              </Tag>
            );
          }
          return null;
        };

        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.mappingInstructions"
                defaultMessage="Map columns from your file to result fields."
              />
            </p>

            {Object.keys(autoMappedColumns).length > 0 && (
              <div
                style={{
                  marginBottom: "1rem",
                  padding: "0.75rem",
                  backgroundColor: "#defbe6",
                  borderRadius: "4px",
                  borderLeft: "3px solid #24a148",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  <CheckmarkFilled size={16} style={{ color: "#24a148" }} />
                  <strong>
                    Auto-mapping detected{" "}
                    {Object.keys(autoMappedColumns).length} column(s)
                  </strong>
                </div>
                <p
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  You can adjust these mappings if needed.
                </p>
              </div>
            )}

            <div
              style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
            >
              <div>
                <div style={{ display: "flex", alignItems: "center" }}>
                  <span style={{ fontWeight: "500" }}>
                    {assayType === "ELISA" ? "Well Position" : "Tube/Position"}
                  </span>
                  {renderAutoMappedTag("wellCoordinate")}
                </div>
                <Dropdown
                  id="wellCoordinate-mapping"
                  label="Select column"
                  items={parseResult?.headers || []}
                  selectedItem={columnMapping.wellCoordinate || null}
                  onChange={({ selectedItem }) =>
                    handleMappingChange("wellCoordinate", selectedItem)
                  }
                />
              </div>

              <div>
                <div style={{ display: "flex", alignItems: "center" }}>
                  <span style={{ fontWeight: "500" }}>Sample ID</span>
                  {renderAutoMappedTag("externalId")}
                </div>
                <Dropdown
                  id="externalId-mapping"
                  label="Select column"
                  items={parseResult?.headers || []}
                  selectedItem={columnMapping.externalId || null}
                  onChange={({ selectedItem }) =>
                    handleMappingChange("externalId", selectedItem)
                  }
                />
              </div>

              <div>
                <div style={{ display: "flex", alignItems: "center" }}>
                  <span style={{ fontWeight: "500" }}>
                    Result/Interpretation
                  </span>
                  {renderAutoMappedTag("result")}
                </div>
                <Dropdown
                  id="result-mapping"
                  label="Select column"
                  items={parseResult?.headers || []}
                  selectedItem={columnMapping.result || null}
                  onChange={({ selectedItem }) =>
                    handleMappingChange("result", selectedItem)
                  }
                />
              </div>

              {assayType === "ELISA" && (
                <>
                  <div>
                    <div style={{ display: "flex", alignItems: "center" }}>
                      <span style={{ fontWeight: "500" }}>OD Value</span>
                      {renderAutoMappedTag("odValue")}
                    </div>
                    <Dropdown
                      id="od-mapping"
                      label="Select column"
                      items={parseResult?.headers || []}
                      selectedItem={columnMapping.odValue || null}
                      onChange={({ selectedItem }) =>
                        handleMappingChange("odValue", selectedItem)
                      }
                    />
                  </div>
                  <div>
                    <div style={{ display: "flex", alignItems: "center" }}>
                      <span style={{ fontWeight: "500" }}>Concentration</span>
                      {renderAutoMappedTag("concentration")}
                    </div>
                    <Dropdown
                      id="concentration-mapping"
                      label="Select column"
                      items={parseResult?.headers || []}
                      selectedItem={columnMapping.concentration || null}
                      onChange={({ selectedItem }) =>
                        handleMappingChange("concentration", selectedItem)
                      }
                    />
                  </div>
                </>
              )}

              {assayType === "FLOW_CYTOMETRY" && (
                <>
                  <div>
                    <div style={{ display: "flex", alignItems: "center" }}>
                      <span style={{ fontWeight: "500" }}>Population %</span>
                      {renderAutoMappedTag("populationPercent")}
                    </div>
                    <Dropdown
                      id="population-mapping"
                      label="Select column"
                      items={parseResult?.headers || []}
                      selectedItem={columnMapping.populationPercent || null}
                      onChange={({ selectedItem }) =>
                        handleMappingChange("populationPercent", selectedItem)
                      }
                    />
                  </div>
                  <div>
                    <div style={{ display: "flex", alignItems: "center" }}>
                      <span style={{ fontWeight: "500" }}>MFI</span>
                      {renderAutoMappedTag("mfi")}
                    </div>
                    <Dropdown
                      id="mfi-mapping"
                      label="Select column"
                      items={parseResult?.headers || []}
                      selectedItem={columnMapping.mfi || null}
                      onChange={({ selectedItem }) =>
                        handleMappingChange("mfi", selectedItem)
                      }
                    />
                  </div>
                </>
              )}
            </div>

            <Button
              kind="primary"
              onClick={handleGeneratePreview}
              style={{ marginTop: "1rem" }}
              disabled={
                !columnMapping.wellCoordinate && !columnMapping.externalId
              }
            >
              Generate Preview
            </Button>
          </div>
        );

      case 4:
        // Step 4: Preview
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              Review the matching results below. Matched samples will be
              updated.
            </p>
            {previewData && (
              <div style={{ marginBottom: "1rem" }}>
                <Tag type="green">{previewData.matchedCount} matched</Tag>
                <Tag type="red" style={{ marginLeft: "0.5rem" }}>
                  {previewData.unmatchedCount} unmatched
                </Tag>
              </div>
            )}
            {previewData?.warnings?.length > 0 && (
              <InlineNotification
                kind="warning"
                title="Warnings"
                subtitle={previewData.warnings.join("; ")}
                style={{ marginBottom: "1rem" }}
              />
            )}
            <DataTable
              rows={
                previewData?.rows?.slice(0, 10).map((row, idx) => ({
                  id: String(idx),
                  rowNumber: row.rowNumber,
                  matchStatus: row.matchStatus,
                  matchType: row.matchType || "-",
                  sampleId: row.matchedSampleId || "-",
                })) || []
              }
              headers={[
                { key: "rowNumber", header: "Row" },
                { key: "matchStatus", header: "Status" },
                { key: "matchType", header: "Match Type" },
                { key: "sampleId", header: "Sample ID" },
              ]}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer>
                  <Table {...getTableProps()} size="sm">
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            {...getHeaderProps({ header })}
                            key={header.key}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow {...getRowProps({ row })} key={row.id}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "matchStatus" ? (
                                <Tag
                                  type={
                                    cell.value === "MATCHED" ? "green" : "red"
                                  }
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>

            {importing && (
              <ProgressBar
                label="Importing results..."
                value={importProgress}
                max={100}
                style={{ marginTop: "1rem" }}
              />
            )}
          </div>
        );

      default:
        return null;
    }
  };

  // Check if using default pages
  const isDefaultPage =
    !pageData?.id || String(pageData.id).startsWith("default-");

  if (loading && !isDefaultPage) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  // Render placeholder for default pages
  if (isDefaultPage) {
    return (
      <div className="analysis-page">
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div
              className="page-instructions"
              style={{ marginBottom: "1.5rem" }}
            >
              <h5 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.analysis.title"
                  defaultMessage="Main Analysis Execution"
                />
              </h5>
              <p style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.analysis.instructions"
                  defaultMessage="Configure assay process parameters, then import or enter results after assay completion."
                />
              </p>
            </div>
            <InlineNotification
              kind="info"
              title="Page Not Configured"
              subtitle="Create a notebook entry to enable analysis functionality."
              hideCloseButton
            />
          </Column>
        </Grid>
      </div>
    );
  }

  return (
    <div className="analysis-page">
      {error && (
        <InlineNotification
          kind="error"
          title="Error"
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title="Success"
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-instructions" style={{ marginBottom: "1.5rem" }}>
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.analysis.title"
                defaultMessage="Main Analysis Execution"
              />
            </h5>
            <p style={{ color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.stageDescription"
                defaultMessage="Stage 6: Configure assay setup (equipment, reagents, process parameters), then capture results after assay completion."
              />
            </p>
          </div>
        </Column>

        {/* Main Tabs: Process Setup / Data Capture */}
        <Column lg={16} md={8} sm={4}>
          <Tabs
            selectedIndex={activeMainTab}
            onChange={({ selectedIndex }) => setActiveMainTab(selectedIndex)}
          >
            <TabList aria-label="Analysis workflow tabs">
              <Tab renderIcon={Settings}>
                <FormattedMessage
                  id="notebook.analysis.processSetup"
                  defaultMessage="Process Setup"
                />
              </Tab>
              <Tab renderIcon={DataBase}>
                <FormattedMessage
                  id="notebook.analysis.dataCapture"
                  defaultMessage="Data Capture"
                />
              </Tab>
            </TabList>

            <TabPanels>
              {/* ============ PROCESS SETUP TAB ============ */}
              <TabPanel>
                <div style={{ padding: "1rem 0" }}>
                  <p style={{ marginBottom: "1rem", color: "#525252" }}>
                    <FormattedMessage
                      id="notebook.analysis.setupDescription"
                      defaultMessage="Configure assay parameters before running. Record equipment, reagent lots, and process settings for complete audit trail."
                    />
                  </p>

                  {/* Assay Type Selection & New Setup Button */}
                  <div
                    style={{
                      display: "flex",
                      gap: "1rem",
                      alignItems: "flex-end",
                      marginBottom: "1.5rem",
                      padding: "1rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <Dropdown
                      id="setup-assay-type"
                      titleText="Assay Type"
                      label="Select type"
                      items={assayTypes}
                      itemToString={(item) => (item ? item.text : "")}
                      selectedItem={assayTypes.find((t) => t.id === assayType)}
                      onChange={({ selectedItem }) =>
                        setAssayType(selectedItem?.id || "ELISA")
                      }
                      style={{ minWidth: "300px" }}
                    />
                    <Button
                      kind="primary"
                      renderIcon={Add}
                      onClick={handleOpenSetupModal}
                    >
                      <FormattedMessage
                        id="notebook.analysis.newSetup"
                        defaultMessage="New Assay Setup"
                      />
                    </Button>
                  </div>

                  {/* List of Active Assay Runs */}
                  {renderAssayRunsList()}
                </div>
              </TabPanel>

              {/* ============ DATA CAPTURE TAB ============ */}
              <TabPanel>
                <div style={{ padding: "1rem 0" }}>
                  <p style={{ marginBottom: "1rem", color: "#525252" }}>
                    <FormattedMessage
                      id="notebook.analysis.dataCaptureDescription"
                      defaultMessage="Import results from analyzer output files or enter results manually. Results are linked to the assay run setup."
                    />
                  </p>

                  {/* Import Summary */}
                  {importSummary && importSummary.importCount > 0 && (
                    <div
                      style={{
                        padding: "1rem",
                        backgroundColor: "#e0f0e0",
                        borderRadius: "4px",
                        marginBottom: "1rem",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          gap: "1rem",
                          alignItems: "center",
                        }}
                      >
                        <CheckmarkFilled
                          size={20}
                          style={{ color: "#24a148" }}
                        />
                        <span>
                          {importSummary.importCount} imports completed,{" "}
                          {importSummary.overallSuccessRate?.toFixed(1) || 0}%
                          success rate
                        </span>
                      </div>
                    </div>
                  )}

                  {/* Actions */}
                  <div
                    style={{
                      display: "flex",
                      gap: "1rem",
                      marginBottom: "1rem",
                      padding: "1rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                      flexWrap: "wrap",
                    }}
                  >
                    <Button
                      kind="primary"
                      size="md"
                      renderIcon={Upload}
                      onClick={() => setShowImportModal(true)}
                    >
                      <FormattedMessage
                        id="notebook.analysis.importResults"
                        defaultMessage="Import Analyzer Results"
                      />
                    </Button>
                    <Button
                      kind="tertiary"
                      size="md"
                      renderIcon={Edit}
                      onClick={handleOpenManualEntry}
                      disabled={selectedIds.length === 0}
                    >
                      Add Results Manually
                      {selectedIds.length > 0 && ` (${selectedIds.length})`}
                    </Button>
                    <Button
                      kind="secondary"
                      size="md"
                      renderIcon={Checkmark}
                      onClick={() => handleMarkComplete()}
                      disabled={
                        completing ||
                        (selectedIds.length === 0 &&
                          samples.filter(
                            (s) =>
                              s.status === "IN_PROGRESS" &&
                              s.data?.analyzerResult,
                          ).length === 0)
                      }
                    >
                      {completing
                        ? "Completing..."
                        : selectedIds.length > 0
                          ? `Mark Selected Complete (${selectedIds.length})`
                          : `Mark All with Results Complete (${
                              samples.filter(
                                (s) =>
                                  s.status === "IN_PROGRESS" &&
                                  s.data?.analyzerResult,
                              ).length
                            })`}
                    </Button>
                    <Button
                      kind="ghost"
                      size="md"
                      renderIcon={Renew}
                      onClick={loadSamples}
                    >
                      Refresh
                    </Button>
                  </div>

                  {/* Selection info */}
                  {selectedIds.length > 0 && (
                    <div
                      style={{
                        padding: "0.5rem 1rem",
                        backgroundColor: "#e0e0e0",
                        borderRadius: "4px",
                        marginBottom: "1rem",
                      }}
                    >
                      {selectedIds.length} samples selected
                    </div>
                  )}

                  {/* Sample Grid */}
                  <SampleGrid
                    gridId="analysis"
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
                        key: "resultInfo",
                        header: intl.formatMessage({
                          id: "notebook.analysis.resultInfo",
                          defaultMessage: "Result Info",
                        }),
                        render: renderResultInfo,
                      },
                    ]}
                  />
                </div>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>

      {/* ============ SETUP MODAL ============ */}
      <Modal
        open={showSetupModal}
        onRequestClose={() => setShowSetupModal(false)}
        onRequestSubmit={handleSaveSetup}
        modalHeading={intl.formatMessage({
          id: "notebook.analysis.setupTitle",
          defaultMessage: "Assay Process Setup",
        })}
        primaryButtonText={savingSetup ? "Saving..." : "Save Setup"}
        primaryButtonDisabled={savingSetup}
        secondaryButtonText="Cancel"
        size="lg"
      >
        <div style={{ minHeight: "500px" }}>
          <div
            style={{
              marginBottom: "1rem",
              padding: "0.75rem",
              backgroundColor: assayType === "ELISA" ? "#e6f4ea" : "#f3e8ff",
              borderRadius: "4px",
            }}
          >
            <Tag type={assayType === "FLOW_CYTOMETRY" ? "purple" : "green"}>
              {assayType === "FLOW_CYTOMETRY" ? "Flow Cytometry" : "ELISA"}
            </Tag>
            <span style={{ marginLeft: "0.5rem", color: "#525252" }}>
              Configure equipment, reagents, and process parameters before
              running the assay.
            </span>
          </div>

          {assayType === "ELISA"
            ? renderElisaSetupForm()
            : renderFlowSetupForm()}
        </div>
      </Modal>

      {/* ============ IMPORT MODAL ============ */}
      <Modal
        open={showImportModal}
        onRequestClose={() => {
          setShowImportModal(false);
          resetImportState();
        }}
        onRequestSubmit={handleExecuteImport}
        modalHeading="Import Analyzer Results"
        primaryButtonText={importStep === 4 ? "Import" : undefined}
        primaryButtonDisabled={importStep !== 4 || importing}
        secondaryButtonText="Cancel"
        size="lg"
      >
        <div style={{ minHeight: "400px" }}>
          {/* Step indicator */}
          <div
            style={{
              display: "flex",
              marginBottom: "1.5rem",
              borderBottom: "1px solid #e0e0e0",
              paddingBottom: "1rem",
            }}
          >
            {[1, 2, 3, 4].map((step) => (
              <div
                key={step}
                style={{
                  flex: 1,
                  textAlign: "center",
                  color: importStep >= step ? "#0f62fe" : "#8d8d8d",
                  fontWeight: importStep === step ? "bold" : "normal",
                }}
              >
                {step === 1 && "Select Run"}
                {step === 2 && "Upload"}
                {step === 3 && "Map Columns"}
                {step === 4 && "Preview"}
              </div>
            ))}
          </div>

          {renderImportStep()}
        </div>
      </Modal>

      {/* ============ MANUAL ENTRY MODAL ============ */}
      <Modal
        open={showManualEntryModal}
        onRequestClose={() => setShowManualEntryModal(false)}
        onRequestSubmit={handleSaveManualResults}
        modalHeading="Add Results Manually"
        primaryButtonText="Save Results"
        primaryButtonDisabled={
          savingManualEntry || !manualEntryData.result.trim()
        }
        secondaryButtonText="Cancel"
        size="md"
      >
        <div style={{ minHeight: "300px" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Enter result for {selectedIds.length} selected sample(s).
          </p>

          <Dropdown
            id="manual-assay-run"
            titleText="Assay Run (Optional)"
            label="Select run..."
            items={assayRuns}
            itemToString={(item) =>
              item ? `${item.assayRunId} - ${item.operatorName}` : ""
            }
            onChange={({ selectedItem }) =>
              setManualEntryData({
                ...manualEntryData,
                assayRunId: selectedItem?.assayRunId || "",
                assayType: selectedItem?.assayType || manualEntryData.assayType,
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          <TextInput
            id="manual-result"
            labelText="Result Value *"
            value={manualEntryData.result}
            onChange={(e) =>
              setManualEntryData({ ...manualEntryData, result: e.target.value })
            }
            placeholder="e.g., Positive, Negative, 1.5 OD, 45% CD4+"
            style={{ marginBottom: "1rem" }}
            required
          />

          {manualEntryData.assayType === "ELISA" && (
            <Grid narrow style={{ marginBottom: "1rem" }}>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="manual-od"
                  labelText="OD Value"
                  value={manualEntryData.odValue}
                  onChange={(e) =>
                    setManualEntryData({
                      ...manualEntryData,
                      odValue: e.target.value,
                    })
                  }
                  placeholder="e.g., 0.450"
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="manual-conc"
                  labelText="Concentration"
                  value={manualEntryData.concentration}
                  onChange={(e) =>
                    setManualEntryData({
                      ...manualEntryData,
                      concentration: e.target.value,
                    })
                  }
                  placeholder="e.g., 45.5 pg/mL"
                />
              </Column>
            </Grid>
          )}

          {manualEntryData.assayType === "FLOW_CYTOMETRY" && (
            <Grid narrow style={{ marginBottom: "1rem" }}>
              <Column lg={5} md={4} sm={4}>
                <TextInput
                  id="manual-cd4-pct"
                  labelText="CD4 %"
                  value={manualEntryData.cd4Percent}
                  onChange={(e) =>
                    setManualEntryData({
                      ...manualEntryData,
                      cd4Percent: e.target.value,
                    })
                  }
                  placeholder="e.g., 42.3"
                />
              </Column>
              <Column lg={5} md={4} sm={4}>
                <TextInput
                  id="manual-cd4-count"
                  labelText="CD4 Count"
                  value={manualEntryData.cd4Count}
                  onChange={(e) =>
                    setManualEntryData({
                      ...manualEntryData,
                      cd4Count: e.target.value,
                    })
                  }
                  placeholder="e.g., 880"
                />
              </Column>
              <Column lg={6} md={4} sm={4}>
                <TextInput
                  id="manual-cd4-mfi"
                  labelText="CD4 MFI"
                  value={manualEntryData.cd4Mfi}
                  onChange={(e) =>
                    setManualEntryData({
                      ...manualEntryData,
                      cd4Mfi: e.target.value,
                    })
                  }
                  placeholder="e.g., 15200"
                />
              </Column>
            </Grid>
          )}

          <TextArea
            id="manual-notes"
            labelText="Notes (Optional)"
            value={manualEntryData.notes}
            onChange={(e) =>
              setManualEntryData({ ...manualEntryData, notes: e.target.value })
            }
            placeholder="Any additional notes..."
            rows={2}
          />

          {savingManualEntry && (
            <div
              style={{
                marginTop: "1rem",
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
              }}
            >
              <Loading withOverlay={false} small />
              <span>Saving results...</span>
            </div>
          )}
        </div>
      </Modal>

      {/* ============ RUN DETAILS MODAL ============ */}
      <Modal
        open={showRunDetailsModal}
        onRequestClose={() => setShowRunDetailsModal(false)}
        modalHeading={`Assay Run Details: ${selectedRunDetails?.assayRunId || ""}`}
        passiveModal
        size="lg"
      >
        {selectedRunDetails && (
          <div style={{ maxHeight: "500px", overflowY: "auto" }}>
            <Grid narrow>
              <Column lg={8} md={4} sm={4}>
                <p>
                  <strong>Type:</strong>{" "}
                  <Tag
                    type={
                      selectedRunDetails.assayType === "FLOW_CYTOMETRY"
                        ? "purple"
                        : "green"
                    }
                  >
                    {selectedRunDetails.assayType === "FLOW_CYTOMETRY"
                      ? "Flow Cytometry"
                      : "ELISA"}
                  </Tag>
                </p>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <p>
                  <strong>Status:</strong>{" "}
                  <Tag
                    type={
                      selectedRunDetails.status === "COMPLETED"
                        ? "green"
                        : selectedRunDetails.status === "IN_PROGRESS"
                          ? "blue"
                          : "gray"
                    }
                  >
                    {selectedRunDetails.status}
                  </Tag>
                </p>
              </Column>
              <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
                <p>
                  <strong>Operator:</strong> {selectedRunDetails.operatorName}
                </p>
              </Column>
              <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
                <p>
                  <strong>Setup Date:</strong> {selectedRunDetails.setupDate}
                </p>
              </Column>
            </Grid>
            <hr style={{ margin: "1rem 0" }} />
            <pre
              style={{
                fontSize: "0.75rem",
                backgroundColor: "#f4f4f4",
                padding: "1rem",
                overflowX: "auto",
              }}
            >
              {JSON.stringify(selectedRunDetails, null, 2)}
            </pre>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default AnalysisPage;
