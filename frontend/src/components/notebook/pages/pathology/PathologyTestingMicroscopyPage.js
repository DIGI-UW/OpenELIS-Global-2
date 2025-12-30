import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  Loading,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Checkbox,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  FileUploaderDropContainer,
  FileUploaderItem,
} from "@carbon/react";
import {
  Add,
  Checkmark,
  Microscope,
  Renew,
  Chemistry,
  DocumentView,
  WarningAlt,
  CheckmarkFilled,
  CloseFilled,
  Upload,
  Edit,
  Download,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyTestingMicroscopyPage - Page 8 of the pathology workflow (Microscopy & Diagnosis).
 * Purpose: Perform microscopic examination and diagnostic interpretation of stained slides.
 * Who uses it: Pathologists / PIs / technicians
 *
 * Two-phase workflow:
 * 1. Add Tests - Record advanced techniques (IHC, ISH), controls, microscopy observation
 * 2. Enter Results - After tests are added, enter results via CSV import or manual entry
 *
 * Note: Routine and special staining is handled in the separate Staining page (Page 7).
 */
function PathologyTestingMicroscopyPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state for bulk operations
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Add Test Modal state
  const [testingModalOpen, setTestingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [isBulkMode, setIsBulkMode] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Enter Results Modal state
  const [resultsModalOpen, setResultsModalOpen] = useState(false);
  const [resultsEntryMode, setResultsEntryMode] = useState("manual"); // "manual" or "csv"
  const [resultsStage, setResultsStage] = useState("initial"); // "initial" or "final"
  const [resultsLoading, setResultsLoading] = useState(false);
  const [resultsViewMode, setResultsViewMode] = useState(false); // true = viewing existing results

  // Slide images for results - separate arrays for initial and final
  const [initialSlideImages, setInitialSlideImages] = useState([]);
  const [finalSlideImages, setFinalSlideImages] = useState([]);

  const [resultsData, setResultsData] = useState({
    // === INITIAL FINDINGS (Stage 1) ===
    initialFindingsDate: "",
    initialExaminer: "",
    initialExaminerInitials: "",
    microscopicDescription: "", // Detailed microscopic findings
    cellularFeatures: "", // Cell types, patterns, abnormalities
    architecturalFindings: "", // Tissue architecture observations
    nuclearFeatures: "", // Nuclear grade, mitotic activity
    stromalFindings: "", // Stroma, vascular, inflammatory
    specialStainResults: "", // Results from special stains
    ihcResults: "", // IHC marker results
    ishResults: "", // ISH findings
    initialImpression: "", // Preliminary diagnostic impression
    differentialDiagnosis: "", // DDx considerations
    additionalStudiesRecommended: "", // Recommended additional tests
    initialFindingsComplete: false,

    // === FINAL DIAGNOSIS (Stage 2) ===
    finalDiagnosisDate: "",
    diagnosingPathologist: "",
    pathologistCredentials: "", // MD, DO, etc.
    finalDiagnosis: "", // Final diagnostic statement
    diagnosisCode: "", // ICD-O or SNOMED code
    tumorType: "", // WHO classification
    histologicGrade: "", // Grade 1, 2, 3, etc.
    tumorStage: "", // pT, pN, pM staging
    marginStatus: "", // Positive, negative, close
    lymphovascularInvasion: "", // Present, absent
    perineuralInvasion: "", // Present, absent
    additionalFindings: "", // Other significant findings
    clinicalCorrelation: "", // Correlation with clinical history
    prognosticFactors: "", // Relevant prognostic features
    synopticReportComplete: false,
    verifiedByPathologist: false,
    verifyingPathologistName: "",
    verificationDate: "",
    pathologistSignature: "",
    pathologistDate: "",
    additionalNotes: "",
    reportFinalized: false,
  });

  // CSV Import state for results
  const [csvFile, setCsvFile] = useState(null);
  const [csvPreview, setCsvPreview] = useState(null);
  const [csvErrors, setCsvErrors] = useState([]);
  const [isImporting, setIsImporting] = useState(false);

  // Test form state - for Add Test modal
  const [testData, setTestData] = useState({
    // === ADVANCED TECHNIQUES ===
    // IHC/ICC
    ihcIccPerformed: false,
    ihcIccSampleType: "", // FFPE, cell_block, cytology_smear
    ihcIccMarkers: "",
    ihcIccPrimaryAntibody: "",
    ihcIccClone: "",
    ihcIccDilution: "",
    ihcIccAntigenRetrieval: "",

    // ISH
    ishPerformed: false,
    ishTargetType: "", // DNA, RNA
    ishTargetSequence: "",
    ishProbeDetails: "",

    // Research Assays
    researchAssays: [],
    researchAssayDetails: "",

    // === CONTROLS & VALIDATION ===
    positiveControlRun: false,
    positiveControlTissue: "",
    positiveControlResult: "",
    positiveControlExpectedStaining: "",
    negativeControlRun: false,
    negativeControlType: "",
    negativeControlResult: "",
    batchNumber: "",
    batchDate: "",
    controlsAccepted: false,
    stainQualityAdequate: false,

    // === MICROSCOPY OBSERVATION ===
    microscopyPerformed: false,
    microscopist: "", // pathologist or PI name
    magnificationUsed: "",
    microscopeType: "",
    findingsDocumented: false,
    microscopicFindings: "",

    // === DOCUMENTATION ===
    accessionNumber: "",
    blockSlideId: "",
    testName: "",
    testType: "",
    controlsUsedSummary: "",
    technicianSignature: "",
    technicianDate: "",
  });

  // Load samples
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
            const transformedSamples = response.map((sample) => {
              const sampleData = sample.data || {};
              const allStains = [
                ...(sampleData.routineStains || []),
                ...(sampleData.specialStains || []),
              ];
              const stainsDisplay =
                allStains.length > 0
                  ? allStains.slice(0, 3).join(", ") +
                    (allStains.length > 3 ? "..." : "")
                  : "";

              return {
                id: String(sample.id || sample.sampleItemId),
                accessionNumber: sample.accessionNumber,
                blockSlideId: sampleData.blockSlideId || sample.blockSlideId,
                specimenType:
                  sample.sampleType || sample.typeOfSample?.description,
                status: sample.pageStatus || "PENDING",
                testsPerformed: sample.testsPerformed || 0,
                testName: sampleData.testName || "",
                result: sampleData.resultFindings || sampleData.result || "",
                stains: stainsDisplay,
                hasTestData: !!(sampleData.testName || allStains.length > 0),
                verifiedByPathologist:
                  sampleData.verifiedByPathologist === true ||
                  sampleData.verifiedByPathologist === "true",
                technicianSignature: sampleData.technicianSignature || "",
                testDate: sampleData.technicianDate || "",
                // Initial and Final Diagnosis status
                initialFindingsComplete:
                  sampleData.initialFindingsComplete === true ||
                  sampleData.initialFindingsComplete === "true",
                initialImpression: sampleData.initialImpression || "",
                reportFinalized:
                  sampleData.reportFinalized === true ||
                  sampleData.reportFinalized === "true",
                finalDiagnosis: sampleData.finalDiagnosis || "",
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
  }, [pageData?.id]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setTestData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleResultsInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setResultsData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleToggleChange = (fieldName, checked) => {
    setTestData((prev) => ({
      ...prev,
      [fieldName]: checked,
    }));
  };

  const handleMultiCheckbox = (fieldName, value, checked) => {
    setTestData((prev) => ({
      ...prev,
      [fieldName]: checked
        ? [...prev[fieldName], value]
        : prev[fieldName].filter((v) => v !== value),
    }));
  };

  const handleDateChange = (dates, fieldName, isResultsForm = false) => {
    if (dates && dates.length > 0) {
      const date = dates[0];
      const formattedDate = date.toISOString().split("T")[0];
      if (isResultsForm) {
        setResultsData((prev) => ({
          ...prev,
          [fieldName]: formattedDate,
        }));
      } else {
        setTestData((prev) => ({
          ...prev,
          [fieldName]: formattedDate,
        }));
      }
    }
  };

  // ========================================
  // SLIDE IMAGE UPLOAD HANDLERS
  // ========================================

  // Handle slide image upload for initial findings
  const handleInitialSlideImageUpload = useCallback(
    (event, { addedFiles }) => {
      const currentCount = initialSlideImages.length;
      const maxImages = 96;

      if (currentCount + addedFiles.length > maxImages) {
        setError(
          intl.formatMessage(
            {
              id: "pathology.results.error.maxImages",
              defaultMessage:
                "Maximum {max} images allowed. You can add {remaining} more.",
            },
            { max: maxImages, remaining: maxImages - currentCount },
          ),
        );
        return;
      }

      const newImages = addedFiles.map((file, index) => {
        const imageNumber = currentCount + index + 1;
        return {
          id: `initial-${Date.now()}-${index}`,
          file: file,
          fileName: file.name,
          fileSize: file.size,
          fileType: file.type,
          slideId: "",
          stainType: "",
          magnification: "",
          fieldDescription: "",
          imageNumber: imageNumber,
          captureTime: new Date().toISOString(),
          notes: "",
          status: "uploading",
          preview: null,
        };
      });

      // Generate previews
      newImages.forEach((img) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          setInitialSlideImages((prev) =>
            prev.map((i) =>
              i.id === img.id
                ? {
                    ...i,
                    preview: e.target.result,
                    base64Data: e.target.result,
                    status: "complete",
                  }
                : i,
            ),
          );
        };
        reader.readAsDataURL(img.file);
      });

      setInitialSlideImages((prev) => [...prev, ...newImages]);
    },
    [initialSlideImages.length, intl],
  );

  // Handle slide image upload for final diagnosis
  const handleFinalSlideImageUpload = useCallback(
    (event, { addedFiles }) => {
      const currentCount = finalSlideImages.length;
      const maxImages = 96;

      if (currentCount + addedFiles.length > maxImages) {
        setError(
          intl.formatMessage(
            {
              id: "pathology.results.error.maxImages",
              defaultMessage:
                "Maximum {max} images allowed. You can add {remaining} more.",
            },
            { max: maxImages, remaining: maxImages - currentCount },
          ),
        );
        return;
      }

      const newImages = addedFiles.map((file, index) => {
        const imageNumber = currentCount + index + 1;
        return {
          id: `final-${Date.now()}-${index}`,
          file: file,
          fileName: file.name,
          fileSize: file.size,
          fileType: file.type,
          slideId: "",
          stainType: "",
          magnification: "",
          fieldDescription: "",
          imageNumber: imageNumber,
          captureTime: new Date().toISOString(),
          notes: "",
          status: "complete",
          preview: null,
        };
      });

      // Generate previews
      newImages.forEach((img) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          setFinalSlideImages((prev) =>
            prev.map((i) =>
              i.id === img.id
                ? {
                    ...i,
                    preview: e.target.result,
                    base64Data: e.target.result,
                    status: "complete",
                  }
                : i,
            ),
          );
        };
        reader.readAsDataURL(img.file);
      });

      setFinalSlideImages((prev) => [...prev, ...newImages]);
    },
    [finalSlideImages.length, intl],
  );

  // Remove a slide image
  const handleRemoveSlideImage = useCallback((imageId, stage) => {
    if (stage === "initial") {
      setInitialSlideImages((prev) => prev.filter((img) => img.id !== imageId));
    } else {
      setFinalSlideImages((prev) => prev.filter((img) => img.id !== imageId));
    }
  }, []);

  // Update slide image metadata
  const handleSlideImageMetadataChange = useCallback(
    (imageId, field, value, stage) => {
      const updateFn = (prev) =>
        prev.map((img) =>
          img.id === imageId ? { ...img, [field]: value } : img,
        );

      if (stage === "initial") {
        setInitialSlideImages(updateFn);
      } else {
        setFinalSlideImages(updateFn);
      }
    },
    [],
  );

  // Open results modal with existing data loaded
  const openResultsModalWithData = useCallback(
    (sample) => {
      setSelectedSample(sample);
      setResultsViewMode(false);
      setResultsLoading(true);
      setResultsStage("initial");
      setInitialSlideImages([]);
      setFinalSlideImages([]);

      // Reset results data
      setResultsData({
        initialFindingsDate: new Date().toISOString().split("T")[0],
        initialExaminer: "",
        initialExaminerInitials: "",
        microscopicDescription: "",
        cellularFeatures: "",
        architecturalFindings: "",
        nuclearFeatures: "",
        stromalFindings: "",
        specialStainResults: "",
        ihcResults: "",
        ishResults: "",
        initialImpression: "",
        differentialDiagnosis: "",
        additionalStudiesRecommended: "",
        initialFindingsComplete: false,
        finalDiagnosisDate: "",
        diagnosingPathologist: "",
        pathologistCredentials: "",
        finalDiagnosis: "",
        diagnosisCode: "",
        tumorType: "",
        histologicGrade: "",
        tumorStage: "",
        marginStatus: "",
        lymphovascularInvasion: "",
        perineuralInvasion: "",
        additionalFindings: "",
        clinicalCorrelation: "",
        prognosticFactors: "",
        synopticReportComplete: false,
        verifiedByPathologist: false,
        verifyingPathologistName: "",
        verificationDate: "",
        pathologistSignature: "",
        pathologistDate: "",
        additionalNotes: "",
        reportFinalized: false,
      });

      setResultsModalOpen(true);

      // Try to load existing results data
      if (pageData?.id && sample?.id) {
        getFromOpenElisServer(
          `/rest/notebook/pathology/results/${sample.id}?pageId=${pageData.id}`,
          (response) => {
            setResultsLoading(false);
            if (response && response.success && response.hasData) {
              setResultsViewMode(true);
              // Determine which stage to show based on data
              if (response.reportFinalized) {
                setResultsStage("final");
              } else if (response.initialFindingsComplete) {
                setResultsStage("final"); // Move to final if initial is complete
              }

              // Populate with existing data
              setResultsData((prev) => ({
                ...prev,
                ...response,
              }));

              // Load existing images
              if (
                response.initialSlideImages &&
                Array.isArray(response.initialSlideImages)
              ) {
                const loadedImages = response.initialSlideImages.map(
                  (img, index) => ({
                    id: `existing-initial-${index}`,
                    fileName: img.fileName || `Slide ${index + 1}`,
                    slideId: img.slideId || "",
                    stainType: img.stainType || "",
                    magnification: img.magnification || "",
                    fieldDescription: img.fieldDescription || "",
                    imageNumber: index + 1,
                    status: "complete",
                    preview: img.base64Data || img.imageUrl || null,
                    base64Data: img.base64Data || null,
                    isExisting: true,
                  }),
                );
                setInitialSlideImages(loadedImages);
              }

              if (
                response.finalSlideImages &&
                Array.isArray(response.finalSlideImages)
              ) {
                const loadedImages = response.finalSlideImages.map(
                  (img, index) => ({
                    id: `existing-final-${index}`,
                    fileName: img.fileName || `Slide ${index + 1}`,
                    slideId: img.slideId || "",
                    stainType: img.stainType || "",
                    magnification: img.magnification || "",
                    fieldDescription: img.fieldDescription || "",
                    imageNumber: index + 1,
                    status: "complete",
                    preview: img.base64Data || img.imageUrl || null,
                    base64Data: img.base64Data || null,
                    isExisting: true,
                  }),
                );
                setFinalSlideImages(loadedImages);
              }
            }
          },
        );
      } else {
        setResultsLoading(false);
      }
    },
    [pageData?.id],
  );

  // Reset test form data
  const resetTestData = (sample = null) => {
    setTestData({
      ihcIccPerformed: false,
      ihcIccSampleType: "",
      ihcIccMarkers: "",
      ihcIccPrimaryAntibody: "",
      ihcIccClone: "",
      ihcIccDilution: "",
      ihcIccAntigenRetrieval: "",
      ishPerformed: false,
      ishTargetType: "",
      ishTargetSequence: "",
      ishProbeDetails: "",
      researchAssays: [],
      researchAssayDetails: "",
      positiveControlRun: false,
      positiveControlTissue: "",
      positiveControlResult: "",
      positiveControlExpectedStaining: "",
      negativeControlRun: false,
      negativeControlType: "",
      negativeControlResult: "",
      batchNumber: "",
      batchDate: new Date().toISOString().split("T")[0],
      controlsAccepted: false,
      stainQualityAdequate: false,
      microscopyPerformed: false,
      microscopist: "",
      magnificationUsed: "",
      microscopeType: "",
      findingsDocumented: false,
      microscopicFindings: "",
      accessionNumber: sample?.accessionNumber || "",
      blockSlideId: sample?.blockSlideId || "",
      testName: "",
      testType: "",
      controlsUsedSummary: "",
      technicianSignature: "",
      technicianDate: new Date().toISOString().split("T")[0],
    });
  };

  // Reset results form data
  const resetResultsData = () => {
    setResultsData({
      resultFindings: "",
      diagnosisCode: "",
      clinicalInterpretation: "",
      verifiedByPathologist: false,
      verifyingPathologistName: "",
      verificationDate: "",
      pathologistSignature: "",
      pathologistDate: "",
      additionalNotes: "",
    });
    setCsvFile(null);
    setCsvPreview(null);
    setCsvErrors([]);
  };

  // Open modal for bulk Add Test operation
  const openBulkTestingModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noSelection",
          defaultMessage: "Please select at least one sample for bulk testing.",
        }),
      );
      return;
    }

    setSelectedSample(null);
    setIsBulkMode(true);
    resetTestData();
    setTestingModalOpen(true);
  };

  // Open Results Entry modal
  const openResultsModal = () => {
    // Only allow entering results for samples that have tests added
    const samplesWithTests = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.hasTestData,
    );

    if (samplesWithTests.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noTestsAdded",
          defaultMessage:
            "Please add tests to samples first before entering results.",
        }),
      );
      return;
    }

    // For the two-stage workflow, open with the first selected sample
    // that has tests - this allows entering detailed results with slide images
    openResultsModalWithData(samplesWithTests[0]);
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
  };

  // Get selected samples for display
  const getSelectedSamples = () => {
    return samples.filter((s) => selectedSampleIds.includes(s.id));
  };

  // Get samples with tests for results entry
  const getSamplesWithTests = () => {
    return samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.hasTestData,
    );
  };

  // Submit Add Test data
  const handleSubmitTest = () => {
    if (submitting) return;

    // Validate required fields
    if (!testData.testName || !testData.technicianSignature) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.requiredFields",
          defaultMessage: "Please fill in Test Name and Technician Signature",
        }),
      );
      return;
    }

    // For single mode, blockSlideId is required
    if (!isBulkMode && !testData.blockSlideId) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.blockSlideRequired",
          defaultMessage: "Please fill in Block/Slide ID",
        }),
      );
      return;
    }

    // Validate controls for IHC/ICC/ISH
    if (testData.ihcIccPerformed || testData.ishPerformed) {
      if (!testData.positiveControlRun || !testData.negativeControlRun) {
        setError(
          intl.formatMessage({
            id: "pathology.testing.error.controlsRequired",
            defaultMessage:
              "For IHC/ICC/ISH, both positive and negative controls are required.",
          }),
        );
        return;
      }
    }

    setSubmitting(true);
    setError(null);

    const sampleIdsToProcess = isBulkMode
      ? selectedSampleIds.map((id) => parseInt(id, 10))
      : [parseInt(selectedSample?.id, 10)];

    // Save test data (status stays IN_PROGRESS until results are entered)
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleIdsToProcess,
        data: testData,
      }),
      (applyResponse) => {
        if (applyResponse && applyResponse.success) {
          // Update status to IN_PROGRESS (tests added, awaiting results)
          postToOpenElisServerJsonResponse(
            `/rest/notebook/bulk/page/${pageData?.id}/samples/status`,
            JSON.stringify({
              sampleIds: sampleIdsToProcess,
              status: "IN_PROGRESS",
            }),
            (statusResponse) => {
              setSubmitting(false);
              if (statusResponse && statusResponse.success) {
                setTestingModalOpen(false);
                if (isBulkMode) {
                  setSelectedSampleIds([]);
                } else {
                  setSelectedSample(null);
                }
                setSuccessMessage(
                  intl.formatMessage(
                    {
                      id: "pathology.testing.success.testAdded",
                      defaultMessage:
                        "Successfully added tests to {count} samples. You can now enter results.",
                    },
                    { count: sampleIdsToProcess.length },
                  ),
                );
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              } else {
                setError(
                  statusResponse?.error ||
                    intl.formatMessage({
                      id: "pathology.testing.error.statusUpdateFailed",
                      defaultMessage:
                        "Failed to update sample status. Please try again.",
                    }),
                );
              }
            },
          );
        } else {
          setSubmitting(false);
          setError(
            applyResponse?.error ||
              intl.formatMessage({
                id: "pathology.testing.error.bulkSubmitFailed",
                defaultMessage: "Failed to submit test data. Please try again.",
              }),
          );
        }
      },
    );
  };

  // Handle manual results submission - Two-stage workflow
  const handleSubmitResults = () => {
    if (submitting) return;

    // Get the sample being edited
    if (!selectedSample) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noSampleSelected",
          defaultMessage: "No sample selected for results entry.",
        }),
      );
      return;
    }

    // Validate based on current stage
    if (resultsStage === "initial") {
      // Require at least microscopic description for initial findings
      if (
        !resultsData.microscopicDescription &&
        !resultsData.initialImpression
      ) {
        setError(
          intl.formatMessage({
            id: "pathology.testing.error.initialFindingsRequired",
            defaultMessage:
              "Please enter microscopic description or initial impression.",
          }),
        );
        return;
      }
    } else if (resultsStage === "final") {
      // Require final diagnosis for final stage
      if (!resultsData.finalDiagnosis) {
        setError(
          intl.formatMessage({
            id: "pathology.testing.error.finalDiagnosisRequired",
            defaultMessage: "Please enter the final diagnosis.",
          }),
        );
        return;
      }
    }

    setSubmitting(true);
    setError(null);

    // Prepare the request payload
    const payload = {
      sampleId: selectedSample.id,
      pageId: pageData?.id,
      stage: resultsStage,
      ...resultsData,
      // Include slide images
      initialSlideImages: initialSlideImages.map((img) => ({
        base64Data: img.base64Data,
        fileName: img.fileName,
        fileType: img.fileType,
        description: img.description || "",
      })),
      finalSlideImages: finalSlideImages.map((img) => ({
        base64Data: img.base64Data,
        fileName: img.fileName,
        fileType: img.fileType,
        description: img.description || "",
      })),
    };

    console.log("Submitting results payload:", payload);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/results/submit`,
      JSON.stringify(payload),
      (response) => {
        console.log("Results submit response:", response);
        setSubmitting(false);

        // Check for HTTP error responses (404, 500, etc.)
        if (response && response.status && response.status >= 400) {
          setError(
            `Server error (${response.status}): ${response.message || response.error || "Unknown error"}`,
          );
          return;
        }

        if (response && response.success) {
          setResultsModalOpen(false);
          setSelectedSample(null);
          setSuccessMessage(
            intl.formatMessage({
              id:
                resultsStage === "final" && resultsData.reportFinalized
                  ? "pathology.testing.success.reportFinalized"
                  : resultsStage === "initial" &&
                      resultsData.initialFindingsComplete
                    ? "pathology.testing.success.initialComplete"
                    : "pathology.testing.success.resultsSaved",
              defaultMessage:
                resultsStage === "final" && resultsData.reportFinalized
                  ? "Report finalized successfully."
                  : resultsStage === "initial" &&
                      resultsData.initialFindingsComplete
                    ? "Initial findings saved. Ready for final diagnosis."
                    : "Results saved successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              response?.message ||
              "Failed to save results. Please try again.",
          );
        }
      },
    );
  };

  // Handle marking samples as complete (pushes to next page)
  const handleMarkComplete = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    // Allow marking complete for samples that:
    // 1. Are selected
    // 2. Are IN_PROGRESS status (results have been entered)
    // OR have result data (result field) OR have test data (hasTestData)
    const samplesWithResults = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        (s.status === "IN_PROGRESS" || s.result || s.hasTestData),
    );

    if (samplesWithResults.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noResultsToComplete",
          defaultMessage:
            "Please enter results for samples before marking them as complete.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const sampleIds = samplesWithResults.map((s) => parseInt(s.id, 10));

    // Mark as COMPLETED - this triggers the next page creation in the backend
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/status`,
      JSON.stringify({
        sampleIds: sampleIds,
        status: "COMPLETED",
      }),
      (statusResponse) => {
        setSubmitting(false);
        if (statusResponse && statusResponse.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.testing.success.markedComplete",
                defaultMessage:
                  "Successfully marked {count} samples as complete. Samples will advance to the next page.",
              },
              { count: sampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            statusResponse?.error || "Failed to mark samples as complete.",
          );
        }
      },
    );
  };

  // Handle verifying results (pathologist sign-off)
  // This keeps samples IN_PROGRESS - only Mark Complete changes status to COMPLETED
  const handleVerifyResults = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    // Only allow verifying samples that have results/test data and are not already verified
    const samplesToVerify = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        (s.result || s.hasTestData || s.status === "IN_PROGRESS") &&
        !s.verifiedByPathologist,
    );

    if (samplesToVerify.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noSamplesToVerify",
          defaultMessage:
            "No unverified samples with results found in selection.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const sampleIds = samplesToVerify.map((s) => parseInt(s.id, 10));

    // Apply verification data to selected samples
    const verificationData = {
      verifiedByPathologist: true,
      verificationDate: new Date().toISOString().split("T")[0],
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleIds,
        data: verificationData,
      }),
      (applyResponse) => {
        setSubmitting(false);
        if (applyResponse && applyResponse.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.testing.success.verified",
                defaultMessage:
                  "Successfully verified results for {count} samples.",
              },
              { count: sampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(applyResponse?.error || "Failed to verify results.");
        }
      },
    );
  };

  // Handle CSV file upload for results
  const handleCsvFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

      if (!addedFile.name.endsWith(".csv")) {
        setCsvErrors([
          {
            message: intl.formatMessage({
              id: "pathology.testing.error.invalidFileType",
              defaultMessage: "Please upload a CSV file",
            }),
          },
        ]);
        return;
      }

      setCsvFile(addedFile);
      setCsvErrors([]);

      // Parse CSV for preview
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target.result;
        const lines = text.split("\n").filter((line) => line.trim());
        if (lines.length < 2) {
          setCsvErrors([
            { message: "CSV file must have header and data rows" },
          ]);
          return;
        }

        const headers = lines[0]
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        const rows = lines.slice(1).map((line, idx) => {
          const values = line.split(",").map((v) => v.trim().replace(/"/g, ""));
          const row = { rowNumber: idx + 2 };
          headers.forEach((h, i) => {
            row[h] = values[i] || "";
          });
          return row;
        });

        setCsvPreview({ headers, rows, totalRows: rows.length });
      };
      reader.readAsText(addedFile);
    },
    [intl],
  );

  // Handle CSV import for results
  const handleCsvImport = async () => {
    if (!csvPreview?.rows || !pageData?.id) return;

    setIsImporting(true);
    setCsvErrors([]);

    const endpoint = `${config.serverBaseUrl}/rest/notebook/pathology/page/${pageData.id}/results/import-csv`;

    try {
      // Send parsed CSV rows as JSON (backend expects { rows: [...] })
      const response = await fetch(endpoint, {
        method: "POST",
        body: JSON.stringify({ rows: csvPreview.rows }),
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setResultsModalOpen(false);
        setSelectedSampleIds([]);
        setCsvFile(null);
        setCsvPreview(null);
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "pathology.testing.success.csvImport",
              defaultMessage:
                "Successfully imported results for {count} samples.",
            },
            { count: data.processedCount || 0 },
          ),
        );
        loadPageSamples();
        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        setCsvErrors(
          data.errors || [{ message: data.error || "Import failed" }],
        );
      }
    } catch (error) {
      setCsvErrors([{ message: error.message }]);
    } finally {
      setIsImporting(false);
    }
  };

  // Download CSV template for results
  const handleDownloadResultsTemplate = () => {
    const templateContent = `accessionNumber,blockSlideId,resultFindings,diagnosisCode,clinicalInterpretation,verifiedByPathologist,verifyingPathologistName,verificationDate,additionalNotes
ACC-2024-001,BLK-001-A,"Positive for malignancy",C50.9,"Invasive ductal carcinoma, Grade 2",true,Dr. Smith,2024-01-15,"Review recommended"
ACC-2024-002,BLK-002-A,"Negative for malignancy",,Benign fibrocystic changes,true,Dr. Jones,2024-01-15,""`;

    const blob = new Blob([templateContent], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "pathology_results_template.csv";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  };

  // Calculate stats
  const testedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const verifiedCount = samples.filter((s) => s.verifiedByPathologist).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.table.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "blockSlideId",
      header: intl.formatMessage({
        id: "pathology.table.blockSlideId",
        defaultMessage: "Block/Slide ID",
      }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "testName",
      header: intl.formatMessage({
        id: "pathology.table.testName",
        defaultMessage: "Test Name",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "pathology.table.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "verified",
      header: intl.formatMessage({
        id: "pathology.table.verified",
        defaultMessage: "Verified",
      }),
    },
    {
      key: "initialDiagnosis",
      header: intl.formatMessage({
        id: "pathology.table.initialDiagnosis",
        defaultMessage: "Initial Diagnosis",
      }),
    },
    {
      key: "finalDiagnosis",
      header: intl.formatMessage({
        id: "pathology.table.finalDiagnosis",
        defaultMessage: "Final Diagnosis",
      }),
    },
  ];

  // === ASSAY OPTIONS (Staining moved to separate StainingPage) ===
  const researchAssayOptions = [
    { id: "laser_microdissection", label: "Laser Microdissection" },
    { id: "multiplex_ihc", label: "Multiplex IHC" },
    { id: "immunofluorescent", label: "Immunofluorescent Staining" },
    { id: "rnascope", label: "RNAscope®" },
  ];

  const commonIhcMarkers = [
    "Ki-67",
    "p53",
    "ER",
    "PR",
    "HER2",
    "CK7",
    "CK20",
    "CD3",
    "CD20",
    "CD45",
    "S100",
    "Melan-A",
    "TTF-1",
    "CDX2",
    "PAX8",
    "GATA3",
  ];

  return (
    <div className="pathology-testing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.microscopy.title"
            defaultMessage="Microscopy & Diagnosis"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.microscopy.description"
            defaultMessage="Perform microscopic examination and diagnostic interpretation of stained slides. Record advanced techniques (IHC, ISH), controls, and enter initial/final diagnoses."
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
                  id="pathology.page.testing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.pending"
                  defaultMessage="Pending Tests"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f0ff" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.inProgress"
                  defaultMessage="Awaiting Results"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{testedCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f7e0" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <Grid fullWidth className="action-buttons-section">
        <Column lg={16} md={8} sm={4}>
          <div
            className="action-buttons"
            style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
          >
            <Button
              kind="primary"
              size="md"
              renderIcon={Add}
              onClick={openBulkTestingModal}
              disabled={selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="pathology.page.testing.addTests"
                defaultMessage="Add Tests ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="secondary"
              size="md"
              renderIcon={Edit}
              onClick={openResultsModal}
              disabled={selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="pathology.page.testing.enterResults"
                defaultMessage="Enter Results ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="tertiary"
              size="md"
              renderIcon={CheckmarkFilled}
              onClick={handleVerifyResults}
              disabled={selectedSampleIds.length === 0 || submitting}
            >
              <FormattedMessage
                id="pathology.page.testing.verifyResults"
                defaultMessage="Verify Results ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="tertiary"
              size="md"
              renderIcon={Checkmark}
              onClick={handleMarkComplete}
              disabled={selectedSampleIds.length === 0 || submitting}
            >
              <FormattedMessage
                id="pathology.page.testing.markComplete"
                defaultMessage="Mark Complete ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="ghost"
              size="md"
              renderIcon={Renew}
              onClick={loadPageSamples}
            >
              <FormattedMessage
                id="label.button.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>
        </Column>
      </Grid>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Success Display */}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Sample Table */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading withOverlay={false} description="Loading samples..." />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.testing.empty"
                defaultMessage="No samples available for testing. Samples must be processed on the previous page first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((s) => ({
              ...s,
              verified: s.verifiedByPathologist ? "Yes" : "No",
              initialDiagnosis: s.initialFindingsComplete
                ? s.initialImpression || "Complete"
                : "Pending",
              finalDiagnosis: s.reportFinalized
                ? s.finalDiagnosis || "Finalized"
                : "Pending",
            }))}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              if (
                selectedRows.length !== selectedSampleIds.length ||
                !selectedRows.every((r) => selectedSampleIds.includes(r.id))
              ) {
                setTimeout(() => handleSelectionChange(selectedRows), 0);
              }

              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = samples.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "status" ? (
                                  <Tag
                                    type={
                                      cell.value === "COMPLETED"
                                        ? "green"
                                        : cell.value === "IN_PROGRESS"
                                          ? "blue"
                                          : "gray"
                                    }
                                    size="sm"
                                  >
                                    {cell.value === "IN_PROGRESS"
                                      ? "Tests Added"
                                      : cell.value}
                                  </Tag>
                                ) : cell.info.header === "verified" ? (
                                  sample?.verifiedByPathologist ? (
                                    <CheckmarkFilled
                                      size={16}
                                      style={{ color: "#24a148" }}
                                    />
                                  ) : (
                                    <CloseFilled
                                      size={16}
                                      style={{ color: "#da1e28" }}
                                    />
                                  )
                                ) : cell.info.header === "initialDiagnosis" ? (
                                  <span
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.25rem",
                                      cursor: "pointer",
                                    }}
                                    onClick={() =>
                                      openResultsModalWithData(sample)
                                    }
                                    title={
                                      sample?.initialImpression ||
                                      (sample?.initialFindingsComplete
                                        ? "Click to view"
                                        : "Click to enter initial findings")
                                    }
                                  >
                                    {sample?.initialFindingsComplete ? (
                                      <>
                                        <CheckmarkFilled
                                          size={16}
                                          style={{ color: "#24a148" }}
                                        />
                                        <span
                                          style={{
                                            maxWidth: "100px",
                                            overflow: "hidden",
                                            textOverflow: "ellipsis",
                                            whiteSpace: "nowrap",
                                          }}
                                        >
                                          {sample?.initialImpression
                                            ? sample.initialImpression.length >
                                              15
                                              ? sample.initialImpression.substring(
                                                  0,
                                                  15,
                                                ) + "..."
                                              : sample.initialImpression
                                            : "Complete"}
                                        </span>
                                      </>
                                    ) : (
                                      <Tag type="gray" size="sm">
                                        Pending
                                      </Tag>
                                    )}
                                  </span>
                                ) : cell.info.header === "finalDiagnosis" ? (
                                  <span
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.25rem",
                                      cursor: "pointer",
                                    }}
                                    onClick={() =>
                                      openResultsModalWithData(sample)
                                    }
                                    title={
                                      sample?.finalDiagnosis ||
                                      (sample?.reportFinalized
                                        ? "Click to view"
                                        : "Click to enter final diagnosis")
                                    }
                                  >
                                    {sample?.reportFinalized ? (
                                      <>
                                        <CheckmarkFilled
                                          size={16}
                                          style={{ color: "#198038" }}
                                        />
                                        <span
                                          style={{
                                            maxWidth: "100px",
                                            overflow: "hidden",
                                            textOverflow: "ellipsis",
                                            whiteSpace: "nowrap",
                                          }}
                                        >
                                          {sample?.finalDiagnosis
                                            ? sample.finalDiagnosis.length > 15
                                              ? sample.finalDiagnosis.substring(
                                                  0,
                                                  15,
                                                ) + "..."
                                              : sample.finalDiagnosis
                                            : "Finalized"}
                                        </span>
                                      </>
                                    ) : sample?.initialFindingsComplete ? (
                                      <Tag type="blue" size="sm">
                                        Ready
                                      </Tag>
                                    ) : (
                                      <Tag type="gray" size="sm">
                                        Pending
                                      </Tag>
                                    )}
                                  </span>
                                ) : (
                                  cell.value || "-"
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          />
        )}
      </div>

      {/* Add Test Modal */}
      <Modal
        open={testingModalOpen}
        modalHeading={
          isBulkMode
            ? intl.formatMessage(
                {
                  id: "pathology.modal.testing.bulkTitle",
                  defaultMessage: "Add Tests - {count} Samples",
                },
                { count: selectedSampleIds.length },
              )
            : intl.formatMessage(
                {
                  id: "pathology.modal.testing.title",
                  defaultMessage: "Add Test - {accession}",
                },
                { accession: selectedSample?.accessionNumber || "" },
              )
        }
        primaryButtonText={intl.formatMessage({
          id: "label.button.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setTestingModalOpen(false);
          setSelectedSample(null);
          setIsBulkMode(false);
          setError(null);
        }}
        onRequestSubmit={handleSubmitTest}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
          {/* Show selected samples in bulk mode */}
          {isBulkMode && (
            <div style={{ marginBottom: "1.5rem" }}>
              <p style={{ marginBottom: "0.5rem", fontWeight: "bold" }}>
                <FormattedMessage
                  id="pathology.modal.testing.selectedSamples"
                  defaultMessage="Selected Samples:"
                />
              </p>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                {getSelectedSamples().map((sample) => (
                  <Tag key={sample.id} type="blue" size="sm">
                    {sample.accessionNumber || sample.id}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          <Tabs>
            <TabList aria-label="Testing sections">
              <Tab>
                <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.advanced"
                  defaultMessage="Advanced Techniques"
                />
              </Tab>
              <Tab>
                <Microscope size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.microscopy"
                  defaultMessage="Microscopy"
                />
              </Tab>
              <Tab>
                <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.controls"
                  defaultMessage="Controls"
                />
              </Tab>
              <Tab>
                <DocumentView size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.testing.tab.documentation"
                  defaultMessage="Documentation"
                />
              </Tab>
            </TabList>

            <TabPanels>
              {/* === TAB 1: ADVANCED TECHNIQUES === */}
              <TabPanel>
                <Grid fullWidth>
                  {/* IHC/ICC Section */}
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "1rem",
                        marginBottom: "1rem",
                      }}
                    >
                      <h5 style={{ margin: 0 }}>
                        <FormattedMessage
                          id="pathology.testing.ihcIcc"
                          defaultMessage="IHC/ICC (Immunohistochemistry/Immunocytochemistry)"
                        />
                      </h5>
                      <Toggle
                        id="ihcIccPerformed"
                        labelA="Off"
                        labelB="On"
                        toggled={testData.ihcIccPerformed}
                        onToggle={(checked) =>
                          handleToggleChange("ihcIccPerformed", checked)
                        }
                      />
                    </div>
                  </Column>

                  {testData.ihcIccPerformed && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="ihcIccSampleType"
                          name="ihcIccSampleType"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccSampleType",
                            defaultMessage: "Sample Type",
                          })}
                          value={testData.ihcIccSampleType}
                          onChange={handleInputChange}
                        >
                          <SelectItem value="" text="-- Select Type --" />
                          <SelectItem
                            value="FFPE"
                            text="FFPE Blocks (Formalin-Fixed Paraffin-Embedded)"
                          />
                          <SelectItem value="cell_block" text="Cell Blocks" />
                          <SelectItem
                            value="cytology_smear"
                            text="Cytology Smears"
                          />
                          <SelectItem value="frozen" text="Frozen Sections" />
                        </Select>
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="ihcIccMarkers"
                          name="ihcIccMarkers"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccMarkers",
                            defaultMessage:
                              "Markers/Antigens (comma-separated)",
                          })}
                          value={testData.ihcIccMarkers}
                          onChange={handleInputChange}
                          placeholder="e.g., Ki-67, ER, PR, HER2"
                          helperText={`Common: ${commonIhcMarkers.slice(0, 5).join(", ")}...`}
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="ihcIccPrimaryAntibody"
                          name="ihcIccPrimaryAntibody"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccPrimaryAntibody",
                            defaultMessage: "Primary Antibody",
                          })}
                          value={testData.ihcIccPrimaryAntibody}
                          onChange={handleInputChange}
                        />
                      </Column>

                      <Column lg={4} md={2} sm={4}>
                        <TextInput
                          id="ihcIccClone"
                          name="ihcIccClone"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccClone",
                            defaultMessage: "Clone",
                          })}
                          value={testData.ihcIccClone}
                          onChange={handleInputChange}
                        />
                      </Column>

                      <Column lg={4} md={2} sm={4}>
                        <TextInput
                          id="ihcIccDilution"
                          name="ihcIccDilution"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccDilution",
                            defaultMessage: "Dilution",
                          })}
                          value={testData.ihcIccDilution}
                          onChange={handleInputChange}
                          placeholder="e.g., 1:100"
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="ihcIccAntigenRetrieval"
                          name="ihcIccAntigenRetrieval"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ihcIccAntigenRetrieval",
                            defaultMessage: "Antigen Retrieval Method",
                          })}
                          value={testData.ihcIccAntigenRetrieval}
                          onChange={handleInputChange}
                        >
                          <SelectItem value="" text="-- Select Method --" />
                          <SelectItem
                            value="heat_citrate"
                            text="Heat-Induced (Citrate Buffer pH 6)"
                          />
                          <SelectItem
                            value="heat_edta"
                            text="Heat-Induced (EDTA Buffer pH 9)"
                          />
                          <SelectItem
                            value="enzyme_proteinase"
                            text="Enzymatic (Proteinase K)"
                          />
                          <SelectItem
                            value="enzyme_trypsin"
                            text="Enzymatic (Trypsin)"
                          />
                          <SelectItem value="none" text="None Required" />
                        </Select>
                      </Column>
                    </>
                  )}

                  {/* ISH Section */}
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "1rem",
                        marginTop: "2rem",
                        marginBottom: "1rem",
                      }}
                    >
                      <h5 style={{ margin: 0 }}>
                        <FormattedMessage
                          id="pathology.testing.ish"
                          defaultMessage="ISH (In Situ Hybridization)"
                        />
                      </h5>
                      <Toggle
                        id="ishPerformed"
                        labelA="Off"
                        labelB="On"
                        toggled={testData.ishPerformed}
                        onToggle={(checked) =>
                          handleToggleChange("ishPerformed", checked)
                        }
                      />
                    </div>
                  </Column>

                  {testData.ishPerformed && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <RadioButtonGroup
                          legendText={intl.formatMessage({
                            id: "pathology.testing.ishTargetType",
                            defaultMessage: "Target Type",
                          })}
                          name="ishTargetType"
                          valueSelected={testData.ishTargetType}
                          onChange={(value) =>
                            setTestData((prev) => ({
                              ...prev,
                              ishTargetType: value,
                            }))
                          }
                        >
                          <RadioButton
                            labelText="DNA"
                            value="DNA"
                            id="ish-dna"
                          />
                          <RadioButton
                            labelText="RNA"
                            value="RNA"
                            id="ish-rna"
                          />
                        </RadioButtonGroup>
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="ishTargetSequence"
                          name="ishTargetSequence"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ishTargetSequence",
                            defaultMessage: "Target Sequence/Gene",
                          })}
                          value={testData.ishTargetSequence}
                          onChange={handleInputChange}
                          placeholder="e.g., HPV, EBER, HER2"
                        />
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="ishProbeDetails"
                          name="ishProbeDetails"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.ishProbeDetails",
                            defaultMessage: "Probe Details",
                          })}
                          value={testData.ishProbeDetails}
                          onChange={handleInputChange}
                          rows={2}
                          placeholder="Probe manufacturer, catalog number, etc."
                        />
                      </Column>
                    </>
                  )}

                  {/* Research Assays */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.researchAssays"
                        defaultMessage="Research Assays"
                      />
                    </h5>
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        flexWrap: "wrap",
                        gap: "1rem",
                      }}
                    >
                      {researchAssayOptions.map((assay) => (
                        <Checkbox
                          key={assay.id}
                          id={`research-${assay.id}`}
                          labelText={assay.label}
                          checked={testData.researchAssays.includes(assay.id)}
                          onChange={(e) =>
                            handleMultiCheckbox(
                              "researchAssays",
                              assay.id,
                              e.target.checked,
                            )
                          }
                        />
                      ))}
                    </div>
                  </Column>

                  {testData.researchAssays.length > 0 && (
                    <Column lg={16} md={8} sm={4}>
                      <TextArea
                        id="researchAssayDetails"
                        name="researchAssayDetails"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.researchAssayDetails",
                          defaultMessage: "Research Assay Details",
                        })}
                        value={testData.researchAssayDetails}
                        onChange={handleInputChange}
                        rows={3}
                        placeholder="Protocol details, parameters, etc."
                        style={{ marginTop: "1rem" }}
                      />
                    </Column>
                  )}
                </Grid>
              </TabPanel>

              {/* === TAB 2: MICROSCOPY OBSERVATION === */}
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "1rem",
                        marginBottom: "1rem",
                      }}
                    >
                      <h5 style={{ margin: 0 }}>
                        <FormattedMessage
                          id="pathology.testing.microscopyObservation"
                          defaultMessage="Microscopy Observation"
                        />
                      </h5>
                      <Toggle
                        id="microscopyPerformed"
                        labelA="Not Done"
                        labelB="Performed"
                        toggled={testData.microscopyPerformed}
                        onToggle={(checked) =>
                          handleToggleChange("microscopyPerformed", checked)
                        }
                      />
                    </div>
                    <p
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        marginBottom: "1rem",
                      }}
                    >
                      <FormattedMessage
                        id="pathology.testing.microscopyObservation.help"
                        defaultMessage="Performed by pathologist or PI (for research). Findings must be documented."
                      />
                    </p>
                  </Column>

                  {testData.microscopyPerformed && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="microscopist"
                          name="microscopist"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.microscopist",
                            defaultMessage: "Microscopist (Pathologist/PI)",
                          })}
                          value={testData.microscopist}
                          onChange={handleInputChange}
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="microscopeType"
                          name="microscopeType"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.microscopeType",
                            defaultMessage: "Microscope Type",
                          })}
                          value={testData.microscopeType}
                          onChange={handleInputChange}
                        >
                          <SelectItem value="" text="-- Select Type --" />
                          <SelectItem value="brightfield" text="Brightfield" />
                          <SelectItem
                            value="fluorescence"
                            text="Fluorescence"
                          />
                          <SelectItem value="confocal" text="Confocal" />
                          <SelectItem
                            value="phase_contrast"
                            text="Phase Contrast"
                          />
                          <SelectItem
                            value="polarized"
                            text="Polarized Light"
                          />
                          <SelectItem value="darkfield" text="Darkfield" />
                        </Select>
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="magnificationUsed"
                          name="magnificationUsed"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.magnificationUsed",
                            defaultMessage: "Magnification(s) Used",
                          })}
                          value={testData.magnificationUsed}
                          onChange={handleInputChange}
                          placeholder="e.g., 4x, 10x, 40x, 100x (oil)"
                        />
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <Checkbox
                          id="findingsDocumented"
                          name="findingsDocumented"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.findingsDocumented",
                            defaultMessage: "Findings Documented",
                          })}
                          checked={testData.findingsDocumented}
                          onChange={handleInputChange}
                          style={{ marginTop: "1rem" }}
                        />
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="microscopicFindings"
                          name="microscopicFindings"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.microscopicFindings",
                            defaultMessage: "Microscopic Findings",
                          })}
                          value={testData.microscopicFindings}
                          onChange={handleInputChange}
                          rows={5}
                          placeholder="Describe morphological features, staining patterns, cellular characteristics, etc."
                          style={{ marginTop: "1rem" }}
                        />
                      </Column>
                    </>
                  )}
                </Grid>
              </TabPanel>

              {/* === TAB 3: CONTROLS & VALIDATION === */}
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "pathology.testing.controls.requirement",
                        defaultMessage:
                          "For IHC/ICC/ISH: Controls must be run with each batch. Both positive and negative controls must pass for assay acceptance.",
                      })}
                      hideCloseButton
                      lowContrast
                      style={{ marginBottom: "1rem" }}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="batchNumber"
                      name="batchNumber"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.batchNumber",
                        defaultMessage: "Batch Number",
                      })}
                      value={testData.batchNumber}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) => handleDateChange(dates, "batchDate")}
                    >
                      <DatePickerInput
                        id="batchDate"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.batchDate",
                          defaultMessage: "Batch Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>

                  {/* Positive Control */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.positiveControl"
                        defaultMessage="Positive Control"
                      />
                    </h5>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <Checkbox
                      id="positiveControlRun"
                      name="positiveControlRun"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlRun",
                        defaultMessage: "Positive Control Run",
                      })}
                      checked={testData.positiveControlRun}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <TextInput
                      id="positiveControlTissue"
                      name="positiveControlTissue"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlTissue",
                        defaultMessage: "Control Tissue/Sample",
                      })}
                      value={testData.positiveControlTissue}
                      onChange={handleInputChange}
                      disabled={!testData.positiveControlRun}
                      placeholder="e.g., Tonsil, Placenta"
                    />
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <Select
                      id="positiveControlResult"
                      name="positiveControlResult"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlResult",
                        defaultMessage: "Result",
                      })}
                      value={testData.positiveControlResult}
                      onChange={handleInputChange}
                      disabled={!testData.positiveControlRun}
                    >
                      <SelectItem value="" text="-- Select --" />
                      <SelectItem value="pass" text="Pass" />
                      <SelectItem value="fail" text="Fail" />
                    </Select>
                  </Column>

                  {/* Negative Control */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.negativeControl"
                        defaultMessage="Negative Control"
                      />
                    </h5>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <Checkbox
                      id="negativeControlRun"
                      name="negativeControlRun"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlRun",
                        defaultMessage: "Negative Control Run",
                      })}
                      checked={testData.negativeControlRun}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <Select
                      id="negativeControlType"
                      name="negativeControlType"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlType",
                        defaultMessage: "Control Type",
                      })}
                      value={testData.negativeControlType}
                      onChange={handleInputChange}
                      disabled={!testData.negativeControlRun}
                    >
                      <SelectItem value="" text="-- Select --" />
                      <SelectItem
                        value="omit_primary"
                        text="Omit Primary Antibody"
                      />
                      <SelectItem
                        value="irrelevant_antibody"
                        text="Isotype Control"
                      />
                      <SelectItem
                        value="known_negative"
                        text="Known Negative Tissue"
                      />
                    </Select>
                  </Column>

                  <Column lg={6} md={4} sm={4}>
                    <Select
                      id="negativeControlResult"
                      name="negativeControlResult"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlResult",
                        defaultMessage: "Result",
                      })}
                      value={testData.negativeControlResult}
                      onChange={handleInputChange}
                      disabled={!testData.negativeControlRun}
                    >
                      <SelectItem value="" text="-- Select --" />
                      <SelectItem value="pass" text="Pass" />
                      <SelectItem value="fail" text="Fail" />
                    </Select>
                  </Column>

                  {/* Acceptance */}
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.acceptance"
                        defaultMessage="Assay Acceptance"
                      />
                    </h5>
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="controlsAccepted"
                      name="controlsAccepted"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.controlsAccepted",
                        defaultMessage: "Controls Passed",
                      })}
                      checked={testData.controlsAccepted}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="stainQualityAdequate"
                      name="stainQualityAdequate"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.stainQualityAdequate",
                        defaultMessage: "Stain Quality Adequate",
                      })}
                      checked={testData.stainQualityAdequate}
                      onChange={handleInputChange}
                    />
                  </Column>
                </Grid>
              </TabPanel>

              {/* === TAB 4: DOCUMENTATION === */}
              <TabPanel>
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.requisitionLogbook"
                        defaultMessage="Test Requisition & Result Logbook"
                      />
                    </h5>
                  </Column>

                  {!isBulkMode && (
                    <>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="accessionNumber"
                          name="accessionNumber"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.accessionNumber",
                            defaultMessage: "Accession Number",
                          })}
                          value={testData.accessionNumber}
                          onChange={handleInputChange}
                          disabled
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="blockSlideId"
                          name="blockSlideId"
                          labelText={intl.formatMessage({
                            id: "pathology.testing.blockSlideId",
                            defaultMessage: "Block/Slide ID *",
                          })}
                          value={testData.blockSlideId}
                          onChange={handleInputChange}
                        />
                      </Column>
                    </>
                  )}

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="testName"
                      name="testName"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.testName",
                        defaultMessage: "Test Name *",
                      })}
                      value={testData.testName}
                      onChange={handleInputChange}
                      placeholder="e.g., H&E, Ki-67 IHC, AFB stain"
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="testType"
                      name="testType"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.testType",
                        defaultMessage: "Test Type",
                      })}
                      value={testData.testType}
                      onChange={handleInputChange}
                    >
                      <SelectItem value="" text="-- Select Type --" />
                      <SelectItem value="HE" text="H&E (Routine)" />
                      <SelectItem value="special_stain" text="Special Stain" />
                      <SelectItem value="IHC" text="IHC Marker" />
                      <SelectItem value="ICC" text="ICC Marker" />
                      <SelectItem value="ISH" text="ISH" />
                      <SelectItem value="FISH" text="FISH" />
                      <SelectItem value="research" text="Research Assay" />
                    </Select>
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <TextInput
                      id="controlsUsedSummary"
                      name="controlsUsedSummary"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.controlsUsedSummary",
                        defaultMessage: "Controls Used (Summary)",
                      })}
                      value={testData.controlsUsedSummary}
                      onChange={handleInputChange}
                      placeholder="e.g., Pos: Tonsil (Pass), Neg: No primary (Pass)"
                      style={{ marginTop: "1rem" }}
                    />
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="pathology.testing.technicianSignoff"
                        defaultMessage="Technician Sign-off"
                      />
                    </h5>
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="technicianSignature"
                      name="technicianSignature"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.technicianSignature",
                        defaultMessage: "Technician Signature *",
                      })}
                      value={testData.technicianSignature}
                      onChange={handleInputChange}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) =>
                        handleDateChange(dates, "technicianDate")
                      }
                    >
                      <DatePickerInput
                        id="technicianDate"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.technicianDate",
                          defaultMessage: "Date",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>
                </Grid>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </div>
      </Modal>

      {/* Enter Results Modal - Two Stage Workflow with Slide Images */}
      <Modal
        open={resultsModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: resultsViewMode
              ? "pathology.modal.results.title.view"
              : "pathology.modal.results.title",
            defaultMessage: resultsViewMode
              ? "View Results - {sampleId}"
              : resultsStage === "initial"
                ? "Initial Findings - {sampleId}"
                : "Final Diagnosis - {sampleId}",
          },
          {
            sampleId:
              selectedSample?.accessionNumber || selectedSample?.id || "",
            count: getSamplesWithTests().length,
          },
        )}
        primaryButtonText={
          resultsViewMode
            ? intl.formatMessage({
                id: "label.button.edit",
                defaultMessage: "Edit",
              })
            : resultsEntryMode === "csv"
              ? intl.formatMessage({
                  id: "pathology.results.import",
                  defaultMessage: "Import",
                })
              : resultsStage === "initial"
                ? intl.formatMessage({
                    id: "pathology.results.saveInitial",
                    defaultMessage: "Save Initial Findings",
                  })
                : intl.formatMessage({
                    id: "pathology.results.saveFinal",
                    defaultMessage: "Finalize Report",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setResultsModalOpen(false);
          resetResultsData();
          setInitialSlideImages([]);
          setFinalSlideImages([]);
          setResultsViewMode(false);
          setError(null);
        }}
        onRequestSubmit={
          resultsViewMode
            ? () => setResultsViewMode(false)
            : resultsEntryMode === "csv"
              ? handleCsvImport
              : handleSubmitResults
        }
        primaryButtonDisabled={
          submitting ||
          isImporting ||
          resultsLoading ||
          (resultsEntryMode === "csv" && !csvFile)
        }
        size="lg"
        hasScrollingContent
        preventCloseOnClickOutside
      >
        {/* Loading indicator */}
        {resultsLoading && (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading
              description="Loading results data..."
              withOverlay={false}
            />
          </div>
        )}

        {!resultsLoading && (
          <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
            {/* View Mode Banner */}
            {resultsViewMode && (
              <InlineNotification
                kind={resultsData.reportFinalized ? "success" : "info"}
                title={intl.formatMessage({
                  id: resultsData.reportFinalized
                    ? "pathology.results.finalized"
                    : "pathology.results.inProgress",
                  defaultMessage: resultsData.reportFinalized
                    ? "Report Finalized"
                    : "Report In Progress",
                })}
                subtitle={intl.formatMessage(
                  {
                    id: "pathology.results.viewMode.description",
                    defaultMessage: resultsData.reportFinalized
                      ? "Finalized by {pathologist} on {date}"
                      : "Initial findings recorded. Click 'Edit' to continue.",
                  },
                  {
                    pathologist: resultsData.diagnosingPathologist || "Unknown",
                    date:
                      resultsData.finalDiagnosisDate ||
                      resultsData.initialFindingsDate ||
                      "Unknown",
                  },
                )}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {/* Stage Selector - Only show in edit mode */}
            {!resultsViewMode && resultsEntryMode === "manual" && (
              <div style={{ marginBottom: "1.5rem" }}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "pathology.results.stage",
                    defaultMessage: "Result Entry Stage",
                  })}
                  name="resultsStage"
                  valueSelected={resultsStage}
                  onChange={(value) => setResultsStage(value)}
                  orientation="horizontal"
                >
                  <RadioButton
                    labelText={intl.formatMessage({
                      id: "pathology.results.stage.initial",
                      defaultMessage: "1. Initial Findings",
                    })}
                    value="initial"
                    id="stage-initial"
                  />
                  <RadioButton
                    labelText={intl.formatMessage({
                      id: "pathology.results.stage.final",
                      defaultMessage: "2. Final Diagnosis",
                    })}
                    value="final"
                    id="stage-final"
                    disabled={
                      !resultsData.initialFindingsComplete && !resultsViewMode
                    }
                  />
                </RadioButtonGroup>
                {!resultsData.initialFindingsComplete &&
                  resultsStage === "initial" && (
                    <p
                      style={{
                        fontSize: "0.875rem",
                        color: "#525252",
                        marginTop: "0.5rem",
                      }}
                    >
                      <FormattedMessage
                        id="pathology.results.completeInitialFirst"
                        defaultMessage="Complete initial findings before proceeding to final diagnosis."
                      />
                    </p>
                  )}
              </div>
            )}

            {/* Mode Selection - Manual vs CSV */}
            {!resultsViewMode && (
              <div style={{ marginBottom: "1.5rem" }}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "pathology.results.entryMode",
                    defaultMessage: "Entry Mode",
                  })}
                  name="resultsEntryMode"
                  valueSelected={resultsEntryMode}
                  onChange={(value) => setResultsEntryMode(value)}
                >
                  <RadioButton
                    labelText={intl.formatMessage({
                      id: "pathology.results.manualEntry",
                      defaultMessage: "Manual Entry",
                    })}
                    value="manual"
                    id="results-manual"
                  />
                  <RadioButton
                    labelText={intl.formatMessage({
                      id: "pathology.results.csvImport",
                      defaultMessage: "CSV Import",
                    })}
                    value="csv"
                    id="results-csv"
                  />
                </RadioButtonGroup>
              </div>
            )}

            {resultsEntryMode === "manual" ? (
              /* Manual Entry Form - Two Stages */
              <Tabs selectedIndex={resultsStage === "initial" ? 0 : 1}>
                <TabList aria-label="Results entry stages">
                  <Tab onClick={() => setResultsStage("initial")}>
                    <FormattedMessage
                      id="pathology.results.tab.initialFindings"
                      defaultMessage="Initial Findings"
                    />
                    {resultsData.initialFindingsComplete && (
                      <CheckmarkFilled
                        style={{ marginLeft: "0.5rem", color: "#24a148" }}
                      />
                    )}
                  </Tab>
                  <Tab
                    onClick={() => setResultsStage("final")}
                    disabled={
                      !resultsData.initialFindingsComplete && !resultsViewMode
                    }
                  >
                    <FormattedMessage
                      id="pathology.results.tab.finalDiagnosis"
                      defaultMessage="Final Diagnosis"
                    />
                    {resultsData.reportFinalized && (
                      <CheckmarkFilled
                        style={{ marginLeft: "0.5rem", color: "#24a148" }}
                      />
                    )}
                  </Tab>
                </TabList>

                <TabPanels>
                  {/* Tab 1: Initial Findings */}
                  <TabPanel>
                    <Grid fullWidth style={{ padding: "1rem 0" }}>
                      {/* Examiner Info */}
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="initialExaminer"
                          name="initialExaminer"
                          labelText={intl.formatMessage({
                            id: "pathology.results.initialExaminer",
                            defaultMessage: "Examining Pathologist/Resident *",
                          })}
                          value={resultsData.initialExaminer}
                          onChange={handleResultsInputChange}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="initialExaminerInitials"
                          name="initialExaminerInitials"
                          labelText={intl.formatMessage({
                            id: "pathology.results.initials",
                            defaultMessage: "Initials",
                          })}
                          value={resultsData.initialExaminerInitials}
                          onChange={handleResultsInputChange}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <DatePicker
                          datePickerType="single"
                          onChange={(dates) =>
                            handleDateChange(dates, "initialFindingsDate", true)
                          }
                          value={resultsData.initialFindingsDate}
                        >
                          <DatePickerInput
                            id="initialFindingsDate"
                            labelText={intl.formatMessage({
                              id: "pathology.results.date",
                              defaultMessage: "Date",
                            })}
                            placeholder="mm/dd/yyyy"
                            disabled={resultsViewMode}
                          />
                        </DatePicker>
                      </Column>

                      {/* Microscopic Description */}
                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="microscopicDescription"
                          name="microscopicDescription"
                          labelText={intl.formatMessage({
                            id: "pathology.results.microscopicDescription",
                            defaultMessage: "Microscopic Description *",
                          })}
                          value={resultsData.microscopicDescription}
                          onChange={handleResultsInputChange}
                          rows={4}
                          placeholder="Detailed microscopic findings..."
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Cellular Features */}
                      <Column lg={8} md={4} sm={4}>
                        <TextArea
                          id="cellularFeatures"
                          name="cellularFeatures"
                          labelText={intl.formatMessage({
                            id: "pathology.results.cellularFeatures",
                            defaultMessage: "Cellular Features",
                          })}
                          value={resultsData.cellularFeatures}
                          onChange={handleResultsInputChange}
                          rows={3}
                          placeholder="Cell types, patterns, abnormalities..."
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Architectural Findings */}
                      <Column lg={8} md={4} sm={4}>
                        <TextArea
                          id="architecturalFindings"
                          name="architecturalFindings"
                          labelText={intl.formatMessage({
                            id: "pathology.results.architecturalFindings",
                            defaultMessage: "Architectural Findings",
                          })}
                          value={resultsData.architecturalFindings}
                          onChange={handleResultsInputChange}
                          rows={3}
                          placeholder="Tissue architecture observations..."
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Special Stain & IHC Results */}
                      <Column lg={8} md={4} sm={4}>
                        <TextArea
                          id="specialStainResults"
                          name="specialStainResults"
                          labelText={intl.formatMessage({
                            id: "pathology.results.specialStainResults",
                            defaultMessage: "Special Stain Results",
                          })}
                          value={resultsData.specialStainResults}
                          onChange={handleResultsInputChange}
                          rows={2}
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={8} md={4} sm={4}>
                        <TextArea
                          id="ihcResults"
                          name="ihcResults"
                          labelText={intl.formatMessage({
                            id: "pathology.results.ihcResults",
                            defaultMessage: "IHC/ICC Results",
                          })}
                          value={resultsData.ihcResults}
                          onChange={handleResultsInputChange}
                          rows={2}
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Initial Impression & DDx */}
                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="initialImpression"
                          name="initialImpression"
                          labelText={intl.formatMessage({
                            id: "pathology.results.initialImpression",
                            defaultMessage:
                              "Preliminary Diagnostic Impression *",
                          })}
                          value={resultsData.initialImpression}
                          onChange={handleResultsInputChange}
                          rows={3}
                          placeholder="Initial diagnostic impression..."
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="differentialDiagnosis"
                          name="differentialDiagnosis"
                          labelText={intl.formatMessage({
                            id: "pathology.results.differentialDiagnosis",
                            defaultMessage: "Differential Diagnosis",
                          })}
                          value={resultsData.differentialDiagnosis}
                          onChange={handleResultsInputChange}
                          rows={2}
                          placeholder="DDx considerations..."
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Slide Images Section */}
                      <Column lg={16} md={8} sm={4}>
                        <h5
                          style={{ marginTop: "1.5rem", marginBottom: "1rem" }}
                        >
                          <FormattedMessage
                            id="pathology.results.slideImages"
                            defaultMessage="Slide Images ({count}/96)"
                            values={{ count: initialSlideImages.length }}
                          />
                        </h5>
                        <InlineNotification
                          kind="info"
                          title={intl.formatMessage({
                            id: "pathology.results.slideImages.info",
                            defaultMessage: "Microscopy Images",
                          })}
                          subtitle={intl.formatMessage({
                            id: "pathology.results.slideImages.description",
                            defaultMessage:
                              "Upload microscopy images to document findings. Up to 96 images allowed.",
                          })}
                          lowContrast
                          hideCloseButton
                          style={{ marginBottom: "1rem" }}
                        />

                        {!resultsViewMode && (
                          <FileUploaderDropContainer
                            accept={[
                              ".jpg",
                              ".jpeg",
                              ".png",
                              ".tiff",
                              ".tif",
                              ".bmp",
                            ]}
                            labelText={intl.formatMessage({
                              id: "pathology.results.dropSlideImages",
                              defaultMessage:
                                "Drag and drop slide images here or click to upload",
                            })}
                            onAddFiles={handleInitialSlideImageUpload}
                            multiple
                          />
                        )}

                        {/* Image Gallery */}
                        {initialSlideImages.length > 0 && (
                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns:
                                "repeat(auto-fill, minmax(150px, 1fr))",
                              gap: "1rem",
                              marginTop: "1rem",
                            }}
                          >
                            {initialSlideImages.map((img) => (
                              <div
                                key={img.id}
                                style={{
                                  border: "1px solid #e0e0e0",
                                  borderRadius: "4px",
                                  padding: "0.5rem",
                                  position: "relative",
                                }}
                              >
                                {img.preview && (
                                  <img
                                    src={img.preview}
                                    alt={img.fileName}
                                    style={{
                                      width: "100%",
                                      height: "100px",
                                      objectFit: "cover",
                                      borderRadius: "4px",
                                    }}
                                  />
                                )}
                                <p
                                  style={{
                                    fontSize: "0.75rem",
                                    marginTop: "0.25rem",
                                    overflow: "hidden",
                                    textOverflow: "ellipsis",
                                    whiteSpace: "nowrap",
                                  }}
                                >
                                  {img.fileName}
                                </p>
                                {!resultsViewMode && (
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    hasIconOnly
                                    iconDescription="Remove"
                                    renderIcon={CloseFilled}
                                    onClick={() =>
                                      handleRemoveSlideImage(img.id, "initial")
                                    }
                                    style={{
                                      position: "absolute",
                                      top: "0",
                                      right: "0",
                                    }}
                                  />
                                )}
                                <TextInput
                                  id={`slide-${img.id}`}
                                  size="sm"
                                  placeholder="Magnification (e.g., 40x)"
                                  value={img.magnification || ""}
                                  onChange={(e) =>
                                    handleSlideImageMetadataChange(
                                      img.id,
                                      "magnification",
                                      e.target.value,
                                      "initial",
                                    )
                                  }
                                  disabled={resultsViewMode}
                                  style={{ marginTop: "0.25rem" }}
                                />
                              </div>
                            ))}
                          </div>
                        )}
                      </Column>

                      {/* Mark Initial Findings Complete */}
                      {!resultsViewMode && (
                        <Column lg={16} md={8} sm={4}>
                          <Checkbox
                            id="initialFindingsComplete"
                            name="initialFindingsComplete"
                            labelText={intl.formatMessage({
                              id: "pathology.results.markInitialComplete",
                              defaultMessage:
                                "Initial findings complete - ready for final diagnosis",
                            })}
                            checked={resultsData.initialFindingsComplete}
                            onChange={handleResultsInputChange}
                            style={{ marginTop: "1.5rem" }}
                          />
                        </Column>
                      )}
                    </Grid>
                  </TabPanel>

                  {/* Tab 2: Final Diagnosis */}
                  <TabPanel>
                    <Grid fullWidth style={{ padding: "1rem 0" }}>
                      {/* Pathologist Info */}
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="diagnosingPathologist"
                          name="diagnosingPathologist"
                          labelText={intl.formatMessage({
                            id: "pathology.results.diagnosingPathologist",
                            defaultMessage: "Diagnosing Pathologist *",
                          })}
                          value={resultsData.diagnosingPathologist}
                          onChange={handleResultsInputChange}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="pathologistCredentials"
                          name="pathologistCredentials"
                          labelText={intl.formatMessage({
                            id: "pathology.results.credentials",
                            defaultMessage: "Credentials",
                          })}
                          value={resultsData.pathologistCredentials}
                          onChange={handleResultsInputChange}
                          placeholder="MD, DO, etc."
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <DatePicker
                          datePickerType="single"
                          onChange={(dates) =>
                            handleDateChange(dates, "finalDiagnosisDate", true)
                          }
                          value={resultsData.finalDiagnosisDate}
                        >
                          <DatePickerInput
                            id="finalDiagnosisDate"
                            labelText={intl.formatMessage({
                              id: "pathology.results.date",
                              defaultMessage: "Date",
                            })}
                            placeholder="mm/dd/yyyy"
                            disabled={resultsViewMode}
                          />
                        </DatePicker>
                      </Column>

                      {/* Final Diagnosis */}
                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="finalDiagnosis"
                          name="finalDiagnosis"
                          labelText={intl.formatMessage({
                            id: "pathology.results.finalDiagnosis",
                            defaultMessage: "Final Diagnosis *",
                          })}
                          value={resultsData.finalDiagnosis}
                          onChange={handleResultsInputChange}
                          rows={4}
                          placeholder="Final pathological diagnosis..."
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Diagnosis Code & Tumor Type */}
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="diagnosisCode"
                          name="diagnosisCode"
                          labelText={intl.formatMessage({
                            id: "pathology.results.diagnosisCode",
                            defaultMessage: "Diagnosis Code (ICD-O/SNOMED)",
                          })}
                          value={resultsData.diagnosisCode}
                          onChange={handleResultsInputChange}
                          placeholder="e.g., C50.9, M8500/3"
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="tumorType"
                          name="tumorType"
                          labelText={intl.formatMessage({
                            id: "pathology.results.tumorType",
                            defaultMessage: "Tumor Type (WHO Classification)",
                          })}
                          value={resultsData.tumorType}
                          onChange={handleResultsInputChange}
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Grade & Stage */}
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="histologicGrade"
                          name="histologicGrade"
                          labelText={intl.formatMessage({
                            id: "pathology.results.histologicGrade",
                            defaultMessage: "Histologic Grade",
                          })}
                          value={resultsData.histologicGrade}
                          onChange={handleResultsInputChange}
                          placeholder="G1, G2, G3"
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="tumorStage"
                          name="tumorStage"
                          labelText={intl.formatMessage({
                            id: "pathology.results.tumorStage",
                            defaultMessage: "pTNM Stage",
                          })}
                          value={resultsData.tumorStage}
                          onChange={handleResultsInputChange}
                          placeholder="pT1N0M0"
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="marginStatus"
                          name="marginStatus"
                          labelText={intl.formatMessage({
                            id: "pathology.results.marginStatus",
                            defaultMessage: "Margin Status",
                          })}
                          value={resultsData.marginStatus}
                          onChange={handleResultsInputChange}
                          placeholder="Negative/Positive"
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="lymphovascularInvasion"
                          name="lymphovascularInvasion"
                          labelText={intl.formatMessage({
                            id: "pathology.results.lvi",
                            defaultMessage: "LVI",
                          })}
                          value={resultsData.lymphovascularInvasion}
                          onChange={handleResultsInputChange}
                          placeholder="Present/Absent"
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Additional Findings */}
                      <Column lg={16} md={8} sm={4}>
                        <TextArea
                          id="additionalFindings"
                          name="additionalFindings"
                          labelText={intl.formatMessage({
                            id: "pathology.results.additionalFindings",
                            defaultMessage: "Additional Findings",
                          })}
                          value={resultsData.additionalFindings}
                          onChange={handleResultsInputChange}
                          rows={2}
                          style={{ marginTop: "1rem" }}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {/* Final Slide Images */}
                      <Column lg={16} md={8} sm={4}>
                        <h5
                          style={{ marginTop: "1.5rem", marginBottom: "1rem" }}
                        >
                          <FormattedMessage
                            id="pathology.results.finalSlideImages"
                            defaultMessage="Final Diagnosis Images ({count}/96)"
                            values={{ count: finalSlideImages.length }}
                          />
                        </h5>

                        {!resultsViewMode && (
                          <FileUploaderDropContainer
                            accept={[
                              ".jpg",
                              ".jpeg",
                              ".png",
                              ".tiff",
                              ".tif",
                              ".bmp",
                            ]}
                            labelText={intl.formatMessage({
                              id: "pathology.results.dropFinalImages",
                              defaultMessage:
                                "Upload additional images for final diagnosis",
                            })}
                            onAddFiles={handleFinalSlideImageUpload}
                            multiple
                          />
                        )}

                        {/* Final Image Gallery */}
                        {finalSlideImages.length > 0 && (
                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns:
                                "repeat(auto-fill, minmax(150px, 1fr))",
                              gap: "1rem",
                              marginTop: "1rem",
                            }}
                          >
                            {finalSlideImages.map((img) => (
                              <div
                                key={img.id}
                                style={{
                                  border: "1px solid #e0e0e0",
                                  borderRadius: "4px",
                                  padding: "0.5rem",
                                  position: "relative",
                                }}
                              >
                                {img.preview && (
                                  <img
                                    src={img.preview}
                                    alt={img.fileName}
                                    style={{
                                      width: "100%",
                                      height: "100px",
                                      objectFit: "cover",
                                      borderRadius: "4px",
                                    }}
                                  />
                                )}
                                <p
                                  style={{
                                    fontSize: "0.75rem",
                                    marginTop: "0.25rem",
                                    overflow: "hidden",
                                    textOverflow: "ellipsis",
                                    whiteSpace: "nowrap",
                                  }}
                                >
                                  {img.fileName}
                                </p>
                                {!resultsViewMode && (
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    hasIconOnly
                                    iconDescription="Remove"
                                    renderIcon={CloseFilled}
                                    onClick={() =>
                                      handleRemoveSlideImage(img.id, "final")
                                    }
                                    style={{
                                      position: "absolute",
                                      top: "0",
                                      right: "0",
                                    }}
                                  />
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                      </Column>

                      {/* Pathologist Verification */}
                      <Column lg={16} md={8} sm={4}>
                        <h5
                          style={{ marginTop: "1.5rem", marginBottom: "1rem" }}
                        >
                          <FormattedMessage
                            id="pathology.results.pathologistVerification"
                            defaultMessage="Pathologist Verification"
                          />
                        </h5>
                        <InlineNotification
                          kind="warning"
                          title={intl.formatMessage({
                            id: "pathology.results.verificationRequired",
                            defaultMessage:
                              "Clinical results must be verified by a certified/licensed pathologist before release.",
                          })}
                          hideCloseButton
                          lowContrast
                          style={{ marginBottom: "1rem" }}
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <Checkbox
                          id="verifiedByPathologist"
                          name="verifiedByPathologist"
                          labelText={intl.formatMessage({
                            id: "pathology.results.verifiedByPathologist",
                            defaultMessage: "Verified by Certified Pathologist",
                          })}
                          checked={resultsData.verifiedByPathologist}
                          onChange={handleResultsInputChange}
                          disabled={resultsViewMode}
                        />
                      </Column>

                      {resultsData.verifiedByPathologist && (
                        <>
                          <Column lg={8} md={4} sm={4}>
                            <TextInput
                              id="verifyingPathologistName"
                              name="verifyingPathologistName"
                              labelText={intl.formatMessage({
                                id: "pathology.results.verifyingPathologistName",
                                defaultMessage: "Verifying Pathologist Name",
                              })}
                              value={resultsData.verifyingPathologistName}
                              onChange={handleResultsInputChange}
                              disabled={resultsViewMode}
                            />
                          </Column>
                          <Column lg={8} md={4} sm={4}>
                            <TextInput
                              id="pathologistSignature"
                              name="pathologistSignature"
                              labelText={intl.formatMessage({
                                id: "pathology.results.pathologistSignature",
                                defaultMessage: "Electronic Signature",
                              })}
                              value={resultsData.pathologistSignature}
                              onChange={handleResultsInputChange}
                              disabled={resultsViewMode}
                            />
                          </Column>
                        </>
                      )}

                      {/* Finalize Report */}
                      {!resultsViewMode && (
                        <Column lg={16} md={8} sm={4}>
                          <Checkbox
                            id="reportFinalized"
                            name="reportFinalized"
                            labelText={intl.formatMessage({
                              id: "pathology.results.finalizeReport",
                              defaultMessage:
                                "Finalize Report - Lock for release",
                            })}
                            checked={resultsData.reportFinalized}
                            onChange={handleResultsInputChange}
                            style={{ marginTop: "1.5rem" }}
                            disabled={!resultsData.verifiedByPathologist}
                          />
                          {!resultsData.verifiedByPathologist && (
                            <p
                              style={{
                                fontSize: "0.875rem",
                                color: "#da1e28",
                                marginTop: "0.25rem",
                              }}
                            >
                              <FormattedMessage
                                id="pathology.results.mustVerifyFirst"
                                defaultMessage="Report must be verified before finalizing."
                              />
                            </p>
                          )}
                        </Column>
                      )}
                    </Grid>
                  </TabPanel>
                </TabPanels>
              </Tabs>
            ) : (
              /* CSV Import - unchanged */
              <div>
                <p style={{ marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.results.csvDescription"
                    defaultMessage="Upload a CSV file with results for multiple samples. The CSV must include accessionNumber or blockSlideId to match samples."
                  />
                </p>

                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Download}
                  onClick={handleDownloadResultsTemplate}
                  style={{ marginBottom: "1rem" }}
                >
                  <FormattedMessage
                    id="pathology.results.downloadTemplate"
                    defaultMessage="Download CSV Template"
                  />
                </Button>

                {!csvFile ? (
                  <FileUploaderDropContainer
                    accept={[".csv"]}
                    labelText={intl.formatMessage({
                      id: "pathology.results.dropzone",
                      defaultMessage:
                        "Drag and drop a CSV file here or click to upload",
                    })}
                    onAddFiles={handleCsvFileAdded}
                  />
                ) : (
                  <FileUploaderItem
                    name={csvFile.name}
                    status="edit"
                    onDelete={() => {
                      setCsvFile(null);
                      setCsvPreview(null);
                      setCsvErrors([]);
                    }}
                  />
                )}

                {csvErrors.length > 0 && (
                  <InlineNotification
                    kind="error"
                    title="Validation Errors"
                    subtitle={
                      <ul style={{ margin: 0, paddingLeft: "1rem" }}>
                        {csvErrors.map((err, idx) => (
                          <li key={idx}>{err.message}</li>
                        ))}
                      </ul>
                    }
                    hideCloseButton
                    lowContrast
                    style={{ marginTop: "1rem" }}
                  />
                )}

                {csvPreview && csvErrors.length === 0 && (
                  <div style={{ marginTop: "1rem" }}>
                    <div
                      style={{
                        display: "flex",
                        gap: "0.5rem",
                        marginBottom: "0.5rem",
                      }}
                    >
                      <Tag type="blue">{csvPreview.totalRows} rows</Tag>
                      <Tag type="green">Ready to import</Tag>
                    </div>
                    <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                      <FormattedMessage
                        id="pathology.results.csvPreviewColumns"
                        defaultMessage="Columns: {columns}"
                        values={{ columns: csvPreview.headers.join(", ") }}
                      />
                    </p>
                  </div>
                )}

                {isImporting && (
                  <Loading
                    withOverlay={false}
                    description="Importing results..."
                  />
                )}
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

export default PathologyTestingMicroscopyPage;
