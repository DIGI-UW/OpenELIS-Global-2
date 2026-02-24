import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextArea,
  Tile,
  Modal,
  RadioButtonGroup,
  RadioButton,
  Tag,
  Checkbox,
  TextInput,
  MultiSelect,
  DatePicker,
  DatePickerInput,
  TimePicker,
  TimePickerSelect,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import {
  Checkmark,
  Edit,
  InventoryManagement,
  Chemistry,
  Warning,
  Time,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * ImmunologyInitialProcessingPage - Page 2 of the Immunology workflow.
 * STAGE 2: Initial Processing with comprehensive procedures and quality checks.
 *
 * Procedures:
 * A. Volume Determination
 *    - Measure initial sample volume
 *    - Record volume in system
 *    - Flag samples with insufficient volume
 *
 * B. Cell Count & Isolation
 *    - Perform cell count (automated or manual)
 *    - Record cell concentration (cells/mL)
 *    - Isolate specific cell populations if required
 *    - Record isolation method:
 *      - Density gradient centrifugation
 *      - Magnetic bead separation
 *      - FACS (Fluorescence-Activated Cell Sorting)
 *      - Other methods
 *
 * C. Parameter Logging
 *    - Centrifugation speed and time
 *    - Temperature during processing
 *    - Reagent lot numbers
 *    - Equipment used
 *    - Cell viability percentage
 *    - Final cell yield
 *    - Timestamp all processing steps
 *    - Record operator name
 *
 * Quality Checks:
 *    - Cell viability threshold (typically >80% for most assays)
 *    - Cell count adequacy
 *    - Visual inspection for contamination
 *    - Pass/fail determination
 */
function ImmunologyInitialProcessingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);

  // Reagents and Instruments from inventory
  const [reagents, setReagents] = useState([]);
  const [instruments, setInstruments] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);
  const [loadingInstruments, setLoadingInstruments] = useState(false);

  // Processing form values
  const [processingValues, setProcessingValues] = useState({
    // A. Volume Determination
    initialVolume: "",
    volumeUnit: "mL",
    volumeSufficient: null,
    minimumVolumeRequired: "",
    volumeNotes: "",

    // B. Cell Count & Isolation
    cellCountMethod: "",
    cellConcentration: "",
    cellConcentrationUnit: "cells/mL",
    isolationRequired: false,
    isolationMethod: "",
    isolationMethodOther: "",
    cellPopulationIsolated: "",

    // C. Parameter Logging
    centrifugationSpeed: "",
    centrifugationSpeedUnit: "rpm",
    centrifugationTime: "",
    centrifugationTimeUnit: "min",
    processingTemperature: "",
    temperatureUnit: "C",
    selectedReagents: [],
    selectedEquipment: [],
    cellViabilityPercentage: "",
    finalCellYield: "",
    finalCellYieldUnit: "cells",
    processingStartTime: "",
    processingEndTime: "",
    operatorName: "",

    // Quality Checks
    viabilityThresholdMet: null,
    cellCountAdequate: null,
    visualInspectionPassed: null,
    contaminationObserved: null,

    // Overall Result
    qcResult: "",
    qcRemarks: "",
    failAction: "",
    failureReason: "",
  });

  // Load reagents from inventory
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

  // Load instruments from template or inventory
  const loadInstruments = useCallback(() => {
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

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadReagents();
    loadInstruments();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    setSuccessMessage(null);

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
              status: sample.pageStatus || sample.status || "PENDING",
              // Processing fields from data
              initialVolume: sample.data?.initialVolume,
              cellConcentration: sample.data?.cellConcentration,
              cellViabilityPercentage: sample.data?.cellViabilityPercentage,
              isolationMethod: sample.data?.isolationMethod,
              qcResult: sample.data?.qcResult,
              qcRemarks: sample.data?.qcRemarks,
              failAction: sample.data?.failAction,
              volumeSufficient: sample.data?.volumeSufficient,
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

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Reset form
  const resetProcessingValues = () => {
    setProcessingValues({
      initialVolume: "",
      volumeUnit: "mL",
      volumeSufficient: null,
      minimumVolumeRequired: "",
      volumeNotes: "",
      cellCountMethod: "",
      cellConcentration: "",
      cellConcentrationUnit: "cells/mL",
      isolationRequired: false,
      isolationMethod: "",
      isolationMethodOther: "",
      cellPopulationIsolated: "",
      centrifugationSpeed: "",
      centrifugationSpeedUnit: "rpm",
      centrifugationTime: "",
      centrifugationTimeUnit: "min",
      processingTemperature: "",
      temperatureUnit: "C",
      selectedReagents: [],
      selectedEquipment: [],
      cellViabilityPercentage: "",
      finalCellYield: "",
      finalCellYieldUnit: "cells",
      processingStartTime: "",
      processingEndTime: "",
      operatorName: "",
      viabilityThresholdMet: null,
      cellCountAdequate: null,
      visualInspectionPassed: null,
      contaminationObserved: null,
      qcResult: "",
      qcRemarks: "",
      failAction: "",
      failureReason: "",
    });
  };

  // Auto-calculate QC result based on quality checks
  const calculateQcResult = useCallback(() => {
    const checks = [
      processingValues.volumeSufficient,
      processingValues.viabilityThresholdMet,
      processingValues.cellCountAdequate,
      processingValues.visualInspectionPassed,
    ];

    // Check if contamination is observed (should be false to pass)
    const contaminationCheck =
      processingValues.contaminationObserved === false ||
      processingValues.contaminationObserved === null;

    let hasAnyFail = false;
    let hasAnyCheck = false;

    checks.forEach((check) => {
      if (check !== null) {
        hasAnyCheck = true;
        if (check === false) hasAnyFail = true;
      }
    });

    // Contamination observed means fail
    if (processingValues.contaminationObserved === true) {
      hasAnyCheck = true;
      hasAnyFail = true;
    }

    if (!hasAnyCheck) return "";
    return hasAnyFail ? "Fail" : "Pass";
  }, [processingValues]);

  // Update QC result when checks change
  useEffect(() => {
    const calculatedResult = calculateQcResult();
    if (calculatedResult && processingValues.qcResult !== calculatedResult) {
      setProcessingValues((prev) => ({
        ...prev,
        qcResult: calculatedResult,
      }));
    }
  }, [calculateQcResult, processingValues.qcResult]);

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.processing.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Build data object with non-empty values
    const data = {};

    // Volume Determination
    if (processingValues.initialVolume)
      data.initialVolume = processingValues.initialVolume;
    if (processingValues.volumeUnit)
      data.volumeUnit = processingValues.volumeUnit;
    if (processingValues.volumeSufficient !== null)
      data.volumeSufficient = processingValues.volumeSufficient;
    if (processingValues.minimumVolumeRequired)
      data.minimumVolumeRequired = processingValues.minimumVolumeRequired;
    if (processingValues.volumeNotes)
      data.volumeNotes = processingValues.volumeNotes;

    // Cell Count & Isolation
    if (processingValues.cellCountMethod)
      data.cellCountMethod = processingValues.cellCountMethod;
    if (processingValues.cellConcentration)
      data.cellConcentration = processingValues.cellConcentration;
    if (processingValues.cellConcentrationUnit)
      data.cellConcentrationUnit = processingValues.cellConcentrationUnit;
    if (processingValues.isolationRequired)
      data.isolationRequired = processingValues.isolationRequired;
    if (processingValues.isolationMethod)
      data.isolationMethod = processingValues.isolationMethod;
    if (processingValues.isolationMethodOther)
      data.isolationMethodOther = processingValues.isolationMethodOther;
    if (processingValues.cellPopulationIsolated)
      data.cellPopulationIsolated = processingValues.cellPopulationIsolated;

    // Parameter Logging
    if (processingValues.centrifugationSpeed)
      data.centrifugationSpeed = processingValues.centrifugationSpeed;
    if (processingValues.centrifugationSpeedUnit)
      data.centrifugationSpeedUnit = processingValues.centrifugationSpeedUnit;
    if (processingValues.centrifugationTime)
      data.centrifugationTime = processingValues.centrifugationTime;
    if (processingValues.centrifugationTimeUnit)
      data.centrifugationTimeUnit = processingValues.centrifugationTimeUnit;
    if (processingValues.processingTemperature)
      data.processingTemperature = processingValues.processingTemperature;
    if (processingValues.temperatureUnit)
      data.temperatureUnit = processingValues.temperatureUnit;
    if (processingValues.selectedReagents.length > 0)
      data.selectedReagents = processingValues.selectedReagents;
    if (processingValues.selectedEquipment.length > 0)
      data.selectedEquipment = processingValues.selectedEquipment;
    if (processingValues.cellViabilityPercentage)
      data.cellViabilityPercentage = processingValues.cellViabilityPercentage;
    if (processingValues.finalCellYield)
      data.finalCellYield = processingValues.finalCellYield;
    if (processingValues.finalCellYieldUnit)
      data.finalCellYieldUnit = processingValues.finalCellYieldUnit;
    if (processingValues.processingStartTime)
      data.processingStartTime = processingValues.processingStartTime;
    if (processingValues.processingEndTime)
      data.processingEndTime = processingValues.processingEndTime;
    if (processingValues.operatorName)
      data.operatorName = processingValues.operatorName;

    // Quality Checks
    if (processingValues.viabilityThresholdMet !== null)
      data.viabilityThresholdMet = processingValues.viabilityThresholdMet;
    if (processingValues.cellCountAdequate !== null)
      data.cellCountAdequate = processingValues.cellCountAdequate;
    if (processingValues.visualInspectionPassed !== null)
      data.visualInspectionPassed = processingValues.visualInspectionPassed;
    if (processingValues.contaminationObserved !== null)
      data.contaminationObserved = processingValues.contaminationObserved;

    // QC Result
    if (processingValues.qcResult) data.qcResult = processingValues.qcResult;
    if (processingValues.qcRemarks) data.qcRemarks = processingValues.qcRemarks;
    if (processingValues.failAction)
      data.failAction = processingValues.failAction;
    if (processingValues.failureReason)
      data.failureReason = processingValues.failureReason;

    // Check if any data was provided
    if (Object.keys(data).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.processing.error.noData",
          defaultMessage: "Please complete at least one field before applying.",
        }),
      );
      return;
    }

    setIsBulkApplying(true);
    setError(null);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data,
      }),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          const resultMessage =
            processingValues.qcResult === "Pass"
              ? intl.formatMessage(
                  {
                    id: "notebook.page.immunology.processing.applied.pass",
                    defaultMessage:
                      "Processing complete for {count} sample(s). Samples can proceed to next step.",
                  },
                  { count: selectedSampleIds.length },
                )
              : processingValues.qcResult === "Fail"
                ? intl.formatMessage(
                    {
                      id: "notebook.page.immunology.processing.applied.fail",
                      defaultMessage:
                        "Processing failed for {count} sample(s). Samples flagged for review.",
                    },
                    { count: selectedSampleIds.length },
                  )
                : intl.formatMessage(
                    {
                      id: "notebook.page.immunology.processing.applied",
                      defaultMessage:
                        "Applied processing values to {count} sample(s).",
                    },
                    { count: selectedSampleIds.length },
                  );
          setSuccessMessage(resultMessage);
          setBulkApplyModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.immunology.processing.error.apply",
              defaultMessage:
                "Failed to apply processing values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    processingValues,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Mark samples as processing complete
  const handleMarkProcessingComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Check if all selected samples have QC result
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingQC = selectedSamples.filter((s) => !s.qcResult);
    if (missingQC.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.immunology.processing.error.missingQC",
            defaultMessage:
              "{count} sample(s) are missing QC result. Please complete processing first.",
          },
          { count: missingQC.length },
        ),
      );
      return;
    }

    // Separate samples by their QC result and fail action
    const passedSamples = selectedSamples.filter((s) => s.qcResult === "Pass");
    // Failed samples with "continue_with_caution" can proceed
    const continueWithCautionSamples = selectedSamples.filter(
      (s) => s.qcResult === "Fail" && s.failAction === "continue_with_caution",
    );
    // Failed samples with "reject" or no action should be rejected
    const rejectedSamples = selectedSamples.filter(
      (s) =>
        s.qcResult === "Fail" && (s.failAction === "reject" || !s.failAction),
    );

    // Check if failed samples have an action selected
    const failedNoAction = selectedSamples.filter(
      (s) => s.qcResult === "Fail" && !s.failAction,
    );
    if (failedNoAction.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.immunology.processing.error.noFailAction",
            defaultMessage:
              "{count} failed sample(s) do not have an action selected. Please select 'Reject' or 'Continue with Caution' for failed samples.",
          },
          { count: failedNoAction.length },
        ),
      );
      return;
    }

    // Samples that will proceed (passed + continue with caution)
    const proceedingSamples = [...passedSamples, ...continueWithCautionSamples];

    let completedRequests = 0;
    let failedRequests = 0;
    const totalRequests =
      (proceedingSamples.length > 0 ? 1 : 0) +
      (rejectedSamples.length > 0 ? 1 : 0);

    const handleRequestComplete = () => {
      completedRequests++;
      if (completedRequests === totalRequests) {
        if (failedRequests === 0) {
          let message = "";
          if (proceedingSamples.length > 0 && rejectedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.immunology.processing.completedMixed",
                defaultMessage:
                  "{proceedCount} sample(s) completed and can proceed. {rejectCount} sample(s) rejected.",
              },
              {
                proceedCount: proceedingSamples.length,
                rejectCount: rejectedSamples.length,
              },
            );
          } else if (rejectedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.immunology.processing.completedRejected",
                defaultMessage: "{count} sample(s) rejected.",
              },
              { count: rejectedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "notebook.page.immunology.processing.completed",
                defaultMessage:
                  "Marked {count} sample(s) as Processing Complete. They can now proceed to next step.",
              },
              { count: proceedingSamples.length },
            );
          }
          setSuccessMessage(message);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.immunology.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      }
    };

    // Update proceeding samples (passed + continue with caution) to COMPLETED
    if (proceedingSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: proceedingSamples.map((s) => parseInt(s.id, 10)),
          status: "COMPLETED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    // Update rejected samples to REJECTED status
    if (rejectedSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: rejectedSamples.map((s) => parseInt(s.id, 10)),
          status: "REJECTED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    if (totalRequests === 0) {
      return;
    }
  }, [
    selectedSampleIds,
    samples,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Calculate stats
  const qcPassedCount = samples.filter((s) => s.qcResult === "Pass").length;
  const qcFailedCount = samples.filter((s) => s.qcResult === "Fail").length;
  const qcPendingCount = samples.filter((s) => !s.qcResult).length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const rejectedCount = samples.filter((s) => s.status === "REJECTED").length;
  const insufficientVolumeCount = samples.filter(
    (s) => s.volumeSufficient === false,
  ).length;

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "Pass") return <Tag type="green">Pass</Tag>;
    if (qcResult === "Fail") return <Tag type="red">Fail</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  // Get volume status tag
  const getVolumeTag = (volumeSufficient) => {
    if (volumeSufficient === null || volumeSufficient === undefined)
      return null;
    if (volumeSufficient === true)
      return (
        <Tag type="green" size="sm">
          Sufficient
        </Tag>
      );
    return (
      <Tag type="red" size="sm">
        Insufficient
      </Tag>
    );
  };

  // Isolation method options
  const isolationMethods = useMemo(
    () => [
      {
        value: "density_gradient",
        label: intl.formatMessage({
          id: "notebook.immunology.isolation.densityGradient",
          defaultMessage: "Density Gradient Centrifugation",
        }),
      },
      {
        value: "magnetic_bead",
        label: intl.formatMessage({
          id: "notebook.immunology.isolation.magneticBead",
          defaultMessage: "Magnetic Bead Separation",
        }),
      },
      {
        value: "facs",
        label: intl.formatMessage({
          id: "notebook.immunology.isolation.facs",
          defaultMessage: "FACS (Fluorescence-Activated Cell Sorting)",
        }),
      },
      {
        value: "other",
        label: intl.formatMessage({
          id: "notebook.immunology.isolation.other",
          defaultMessage: "Other Method",
        }),
      },
    ],
    [intl],
  );

  // Cell count method options
  const cellCountMethods = useMemo(
    () => [
      {
        value: "automated",
        label: intl.formatMessage({
          id: "notebook.immunology.cellCount.automated",
          defaultMessage: "Automated",
        }),
      },
      {
        value: "manual",
        label: intl.formatMessage({
          id: "notebook.immunology.cellCount.manual",
          defaultMessage: "Manual",
        }),
      },
    ],
    [intl],
  );

  return (
    <div className="immunology-initial-processing-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.immunology.initialProcessing.title"
            defaultMessage="Initial Processing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.immunology.initialProcessing.description"
            defaultMessage="Perform volume determination, cell count & isolation, log processing parameters, and complete quality checks. Use Bulk Apply to process multiple samples at once."
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
                  id="notebook.page.immunology.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.processing.passed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.processing.failed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.processing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            {insufficientVolumeCount > 0 && (
              <Tile className="progress-tile warning">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.page.immunology.processing.insufficientVolume"
                    defaultMessage="Insufficient Volume"
                  />
                </span>
                <span className="progress-value">
                  {insufficientVolumeCount}
                </span>
              </Tile>
            )}
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.processing.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            {rejectedCount > 0 && (
              <Tile className="progress-tile error">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.page.immunology.processing.rejected"
                    defaultMessage="Rejected"
                  />
                </span>
                <span className="progress-value">{rejectedCount}</span>
              </Tile>
            )}
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={() => {
            resetProcessingValues();
            setBulkApplyModalOpen(true);
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.immunology.processing.bulkApply"
            defaultMessage="Bulk Apply Processing ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkProcessingComplete}
          >
            <FormattedMessage
              id="notebook.page.immunology.processing.markComplete"
              defaultMessage="Mark Processing Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Pending Processing Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.immunology.processing.pendingTable.title"
              defaultMessage="Samples Pending Processing"
            />
            <Tag type="gray" className="count-tag">
              {
                samples.filter(
                  (s) =>
                    s.status !== "COMPLETED" &&
                    s.status !== "SKIPPED" &&
                    s.status !== "REJECTED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.immunology.processing.pendingTable.description"
              defaultMessage="Select samples and use 'Bulk Apply Processing' to record processing parameters and quality checks."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="pending-processing"
            samples={samples.filter(
              (s) =>
                s.status !== "COMPLETED" &&
                s.status !== "SKIPPED" &&
                s.status !== "REJECTED",
            )}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "initialVolume",
                header: intl.formatMessage({
                  id: "notebook.grid.volume",
                  defaultMessage: "Volume",
                }),
                render: (value, sample) =>
                  value ? (
                    <span>
                      {value} mL {getVolumeTag(sample.volumeSufficient)}
                    </span>
                  ) : (
                    "-"
                  ),
              },
              {
                key: "cellViabilityPercentage",
                header: intl.formatMessage({
                  id: "notebook.grid.viability",
                  defaultMessage: "Viability",
                }),
                render: (value) => (value ? `${value}%` : "-"),
              },
              {
                key: "qcResult",
                header: intl.formatMessage({
                  id: "notebook.grid.qcResult",
                  defaultMessage: "QC Result",
                }),
                render: (value) => getQCTag(value),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) =>
              s.status !== "COMPLETED" &&
              s.status !== "SKIPPED" &&
              s.status !== "REJECTED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.immunology.processing.pendingTable.empty"
                  defaultMessage="No samples pending processing. All samples have been processed."
                />
              </p>
            </div>
          )}
      </div>

      {/* Completed Processing Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.immunology.processing.completedTable.title"
              defaultMessage="Processing Completed"
            />
            <Tag type="green" className="count-tag">
              {
                samples.filter(
                  (s) =>
                    s.status === "COMPLETED" ||
                    s.status === "SKIPPED" ||
                    s.status === "REJECTED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.immunology.processing.completedTable.description"
              defaultMessage="Samples that have completed initial processing. Passed samples can proceed to the next workflow step."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="completed-processing"
            samples={samples.filter(
              (s) =>
                s.status === "COMPLETED" ||
                s.status === "SKIPPED" ||
                s.status === "REJECTED",
            )}
            selectedIds={[]}
            showSelection={false}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "cellViabilityPercentage",
                header: intl.formatMessage({
                  id: "notebook.grid.viability",
                  defaultMessage: "Viability",
                }),
                render: (value) => (value ? `${value}%` : "-"),
              },
              {
                key: "isolationMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.isolationMethod",
                  defaultMessage: "Isolation Method",
                }),
                render: (value) => {
                  if (!value) return "-";
                  const method = isolationMethods.find(
                    (m) => m.value === value,
                  );
                  return method ? method.label : value;
                },
              },
              {
                key: "qcResult",
                header: intl.formatMessage({
                  id: "notebook.grid.qcResult",
                  defaultMessage: "QC Result",
                }),
                render: (value) => getQCTag(value),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.disposition",
                  defaultMessage: "Disposition",
                }),
                render: (value) => {
                  if (value === "REJECTED") {
                    return (
                      <Tag type="red">
                        <FormattedMessage
                          id="notebook.disposition.rejected"
                          defaultMessage="Rejected"
                        />
                      </Tag>
                    );
                  }
                  if (value === "SKIPPED") {
                    return (
                      <Tag type="magenta">
                        <FormattedMessage
                          id="notebook.disposition.notProceeding"
                          defaultMessage="Not Proceeding"
                        />
                      </Tag>
                    );
                  }
                  if (value === "COMPLETED") {
                    return (
                      <Tag type="green">
                        <FormattedMessage
                          id="notebook.disposition.proceeding"
                          defaultMessage="Proceeding"
                        />
                      </Tag>
                    );
                  }
                  return "-";
                },
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) =>
              s.status === "COMPLETED" ||
              s.status === "SKIPPED" ||
              s.status === "REJECTED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.immunology.processing.completedTable.empty"
                  defaultMessage="No samples have completed processing yet."
                />
              </p>
            </div>
          )}
      </div>

      {/* Global empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state global-empty">
          <p>
            <FormattedMessage
              id="notebook.page.immunology.processing.empty"
              defaultMessage="No samples available yet. Complete Sample Reception first, then perform Initial Processing."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.bulkApply.title",
          defaultMessage: "Initial Processing",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : processingValues.qcResult === "Pass"
              ? intl.formatMessage({
                  id: "notebook.immunology.processing.action.pass",
                  defaultMessage: "Pass - Proceed to Next Step",
                })
              : processingValues.qcResult === "Fail"
                ? intl.formatMessage({
                    id: "notebook.immunology.processing.action.fail",
                    defaultMessage: "Fail - Flag for Review",
                  })
                : intl.formatMessage({
                    id: "label.apply",
                    defaultMessage: "Apply Processing",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isBulkApplying}
        danger={processingValues.qcResult === "Fail"}
      >
        <div className="processing-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.immunology.bulkApply.description"
              defaultMessage="Perform initial processing on {count} selected sample(s). Complete volume determination, cell count & isolation, and quality checks."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <Accordion>
            {/* Section A: Volume Determination */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.section.volumeDetermination"
                    defaultMessage="A. Volume Determination"
                  />
                </span>
              }
              open
            >
              <Grid fullWidth className="processing-section">
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="initialVolume"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.initialVolume",
                      defaultMessage: "Initial Sample Volume",
                    })}
                    value={processingValues.initialVolume}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        initialVolume: e.target.value,
                      }))
                    }
                    type="number"
                    step="0.1"
                    min="0"
                  />
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <Select
                    id="volumeUnit"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.volumeUnit",
                      defaultMessage: "Unit",
                    })}
                    value={processingValues.volumeUnit}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        volumeUnit: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="mL" text="mL" />
                    <SelectItem value="uL" text="uL" />
                    <SelectItem value="L" text="L" />
                  </Select>
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TextInput
                    id="minimumVolumeRequired"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.minimumVolume",
                      defaultMessage: "Minimum Required",
                    })}
                    value={processingValues.minimumVolumeRequired}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        minimumVolumeRequired: e.target.value,
                      }))
                    }
                    type="number"
                    step="0.1"
                    min="0"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.immunology.volumeSufficient",
                      defaultMessage: "Volume Sufficient for Testing?",
                    })}
                    name="volumeSufficient"
                    valueSelected={
                      processingValues.volumeSufficient === true
                        ? "yes"
                        : processingValues.volumeSufficient === false
                          ? "no"
                          : ""
                    }
                    onChange={(value) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        volumeSufficient: value === "yes",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton labelText="Yes" value="yes" id="vol-yes" />
                    <RadioButton labelText="No" value="no" id="vol-no" />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {processingValues.volumeSufficient === false && (
                    <InlineNotification
                      kind="warning"
                      title={intl.formatMessage({
                        id: "notebook.immunology.volumeWarning",
                        defaultMessage: "Insufficient volume flagged",
                      })}
                      subtitle={intl.formatMessage({
                        id: "notebook.immunology.volumeWarning.subtitle",
                        defaultMessage:
                          "Sample will be flagged for review or rejection.",
                      })}
                      hideCloseButton
                      lowContrast
                    />
                  )}
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="volumeNotes"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.volumeNotes",
                      defaultMessage: "Volume Notes",
                    })}
                    value={processingValues.volumeNotes}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        volumeNotes: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.volumeNotes.placeholder",
                      defaultMessage: "Any notes about volume determination...",
                    })}
                    rows={2}
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Section B: Cell Count & Isolation */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.section.cellCountIsolation"
                    defaultMessage="B. Cell Count & Isolation"
                  />
                </span>
              }
            >
              <Grid fullWidth className="processing-section">
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="cellCountMethod"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.cellCountMethod",
                      defaultMessage: "Cell Count Method",
                    })}
                    value={processingValues.cellCountMethod}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        cellCountMethod: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select method..." />
                    {cellCountMethods.map((method) => (
                      <SelectItem
                        key={method.value}
                        value={method.value}
                        text={method.label}
                      />
                    ))}
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="cellConcentration"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.cellConcentration",
                      defaultMessage: "Cell Concentration",
                    })}
                    value={processingValues.cellConcentration}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        cellConcentration: e.target.value,
                      }))
                    }
                    placeholder="e.g., 2.4x10^6"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="isolationRequired"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.isolationRequired",
                      defaultMessage:
                        "Cell population isolation required/performed",
                    })}
                    checked={processingValues.isolationRequired}
                    onChange={(_, { checked }) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        isolationRequired: checked,
                      }))
                    }
                  />
                </Column>
                {processingValues.isolationRequired && (
                  <>
                    <Column lg={8} md={4} sm={4}>
                      <Select
                        id="isolationMethod"
                        labelText={intl.formatMessage({
                          id: "notebook.immunology.isolationMethod",
                          defaultMessage: "Isolation Method",
                        })}
                        value={processingValues.isolationMethod}
                        onChange={(e) =>
                          setProcessingValues((prev) => ({
                            ...prev,
                            isolationMethod: e.target.value,
                          }))
                        }
                      >
                        <SelectItem value="" text="Select method..." />
                        {isolationMethods.map((method) => (
                          <SelectItem
                            key={method.value}
                            value={method.value}
                            text={method.label}
                          />
                        ))}
                      </Select>
                    </Column>
                    {processingValues.isolationMethod === "other" && (
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="isolationMethodOther"
                          labelText={intl.formatMessage({
                            id: "notebook.immunology.isolationMethodOther",
                            defaultMessage: "Other Isolation Method",
                          })}
                          value={processingValues.isolationMethodOther}
                          onChange={(e) =>
                            setProcessingValues((prev) => ({
                              ...prev,
                              isolationMethodOther: e.target.value,
                            }))
                          }
                          placeholder="Describe method..."
                        />
                      </Column>
                    )}
                    <Column lg={8} md={4} sm={4}>
                      <TextInput
                        id="cellPopulationIsolated"
                        labelText={intl.formatMessage({
                          id: "notebook.immunology.cellPopulationIsolated",
                          defaultMessage: "Cell Population Isolated",
                        })}
                        value={processingValues.cellPopulationIsolated}
                        onChange={(e) =>
                          setProcessingValues((prev) => ({
                            ...prev,
                            cellPopulationIsolated: e.target.value,
                          }))
                        }
                        placeholder="e.g., CD4+ T cells, PBMCs"
                      />
                    </Column>
                  </>
                )}
              </Grid>
            </AccordionItem>

            {/* Section C: Parameter Logging */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Time size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.section.parameterLogging"
                    defaultMessage="C. Parameter Logging"
                  />
                </span>
              }
            >
              <Grid fullWidth className="processing-section">
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="centrifugationSpeed"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.centrifugationSpeed",
                      defaultMessage: "Centrifugation Speed",
                    })}
                    value={processingValues.centrifugationSpeed}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        centrifugationSpeed: e.target.value,
                      }))
                    }
                    type="number"
                    step="100"
                    min="0"
                    max="20000"
                  />
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <Select
                    id="centrifugationSpeedUnit"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.speedUnit",
                      defaultMessage: "Unit",
                    })}
                    value={processingValues.centrifugationSpeedUnit}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        centrifugationSpeedUnit: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="rpm" text="RPM" />
                    <SelectItem value="xg" text="x g" />
                    <SelectItem value="rcf" text="RCF" />
                  </Select>
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TextInput
                    id="centrifugationTime"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.centrifugationTime",
                      defaultMessage: "Centrifugation Time",
                    })}
                    value={processingValues.centrifugationTime}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        centrifugationTime: e.target.value,
                      }))
                    }
                    type="number"
                    step="1"
                    min="0"
                    max="120"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="processingTemperature"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.processingTemperature",
                      defaultMessage: "Processing Temperature",
                    })}
                    value={processingValues.processingTemperature}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        processingTemperature: e.target.value,
                      }))
                    }
                    type="number"
                    step="0.5"
                    min="-80"
                    max="100"
                  />
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <Select
                    id="temperatureUnit"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.tempUnit",
                      defaultMessage: "Unit",
                    })}
                    value={processingValues.temperatureUnit}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        temperatureUnit: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="C" text="°C" />
                    <SelectItem value="F" text="°F" />
                  </Select>
                </Column>
                {/* Reagent & Instrument Selection Section */}
                <Column lg={16} md={8} sm={4}>
                  <h6
                    style={{
                      marginTop: "1.5rem",
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
                      id="notebook.immunology.section.reagentInstrument"
                      defaultMessage="Reagent & Instrument Selection"
                    />
                  </h6>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <MultiSelect
                    id="selectedReagents"
                    titleText={intl.formatMessage({
                      id: "notebook.immunology.reagents",
                      defaultMessage: "Reagents",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.immunology.reagents.placeholder",
                      defaultMessage: "Select reagents...",
                    })}
                    items={reagents}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItems={reagents.filter((r) =>
                      processingValues.selectedReagents.includes(r.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        selectedReagents: selectedItems.map((r) => r.id),
                      }))
                    }
                    disabled={loadingReagents}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <MultiSelect
                    id="selectedEquipment"
                    titleText={intl.formatMessage({
                      id: "notebook.immunology.equipment",
                      defaultMessage: "Instruments / Equipment",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.immunology.equipment.placeholder",
                      defaultMessage: "Select instruments...",
                    })}
                    items={instruments}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItems={instruments.filter((i) =>
                      processingValues.selectedEquipment.includes(i.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        selectedEquipment: selectedItems.map((i) => i.id),
                      }))
                    }
                    disabled={loadingInstruments}
                  />
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TextInput
                    id="cellViabilityPercentage"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.cellViability",
                      defaultMessage: "Cell Viability (%)",
                    })}
                    value={processingValues.cellViabilityPercentage}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        cellViabilityPercentage: e.target.value,
                      }))
                    }
                    type="number"
                    step="0.1"
                    min="0"
                    max="100"
                  />
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TextInput
                    id="finalCellYield"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.finalCellYield",
                      defaultMessage: "Final Cell Yield",
                    })}
                    value={processingValues.finalCellYield}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        finalCellYield: e.target.value,
                      }))
                    }
                    placeholder="e.g., 5x10^6"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="processingStartTime"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.processingStartTime",
                      defaultMessage: "Processing Start Time",
                    })}
                    type="datetime-local"
                    value={processingValues.processingStartTime}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        processingStartTime: e.target.value,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="processingEndTime"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.processingEndTime",
                      defaultMessage: "Processing End Time",
                    })}
                    type="datetime-local"
                    value={processingValues.processingEndTime}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        processingEndTime: e.target.value,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="operatorName"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.operatorName",
                      defaultMessage: "Operator Name",
                    })}
                    value={processingValues.operatorName}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        operatorName: e.target.value,
                      }))
                    }
                    placeholder="Enter operator name..."
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Quality Checks */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Warning size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.section.qualityChecks"
                    defaultMessage="Quality Checks"
                  />
                </span>
              }
            >
              <Grid fullWidth className="processing-section">
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.immunology.viabilityThresholdMet",
                      defaultMessage:
                        "Cell Viability Threshold Met (typically >80%)",
                    })}
                    name="viabilityThresholdMet"
                    valueSelected={
                      processingValues.viabilityThresholdMet === true
                        ? "pass"
                        : processingValues.viabilityThresholdMet === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        viabilityThresholdMet: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="viability-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="viability-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.immunology.cellCountAdequate",
                      defaultMessage: "Cell Count Adequate for Assay",
                    })}
                    name="cellCountAdequate"
                    valueSelected={
                      processingValues.cellCountAdequate === true
                        ? "pass"
                        : processingValues.cellCountAdequate === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        cellCountAdequate: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="cellcount-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="cellcount-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.immunology.visualInspectionPassed",
                      defaultMessage: "Visual Inspection Passed",
                    })}
                    name="visualInspectionPassed"
                    valueSelected={
                      processingValues.visualInspectionPassed === true
                        ? "pass"
                        : processingValues.visualInspectionPassed === false
                          ? "fail"
                          : ""
                    }
                    onChange={(value) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        visualInspectionPassed: value === "pass",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Pass"
                      value="pass"
                      id="visual-pass"
                    />
                    <RadioButton
                      labelText="Fail"
                      value="fail"
                      id="visual-fail"
                    />
                  </RadioButtonGroup>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.immunology.contaminationObserved",
                      defaultMessage: "Contamination Observed?",
                    })}
                    name="contaminationObserved"
                    valueSelected={
                      processingValues.contaminationObserved === true
                        ? "yes"
                        : processingValues.contaminationObserved === false
                          ? "no"
                          : ""
                    }
                    onChange={(value) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        contaminationObserved: value === "yes",
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText="Yes (Fail)"
                      value="yes"
                      id="contamination-yes"
                    />
                    <RadioButton
                      labelText="No (Pass)"
                      value="no"
                      id="contamination-no"
                    />
                  </RadioButtonGroup>
                </Column>
              </Grid>
            </AccordionItem>
          </Accordion>

          {/* QC Result Summary */}
          <div className="qc-section qc-result-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.immunology.section.result"
                defaultMessage="Processing Result"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <div className="qc-result-display">
                  {processingValues.qcResult === "Pass" && (
                    <Tag type="green" size="md">
                      <FormattedMessage
                        id="notebook.immunology.result.pass"
                        defaultMessage="PASS - Proceed to Next Step"
                      />
                    </Tag>
                  )}
                  {processingValues.qcResult === "Fail" && (
                    <Tag type="red" size="md">
                      <FormattedMessage
                        id="notebook.immunology.result.fail"
                        defaultMessage="FAIL - Flag for Review"
                      />
                    </Tag>
                  )}
                  {!processingValues.qcResult && (
                    <Tag type="gray" size="md">
                      <FormattedMessage
                        id="notebook.immunology.result.pending"
                        defaultMessage="Complete quality checks above"
                      />
                    </Tag>
                  )}
                </div>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="qcResultOverride"
                  labelText={intl.formatMessage({
                    id: "notebook.immunology.resultOverride",
                    defaultMessage: "Override Result (optional)",
                  })}
                  value={processingValues.qcResult}
                  onChange={(e) =>
                    setProcessingValues((prev) => ({
                      ...prev,
                      qcResult: e.target.value,
                    }))
                  }
                >
                  <SelectItem value="" text="Auto (based on checks)" />
                  <SelectItem value="Pass" text="Pass" />
                  <SelectItem value="Fail" text="Fail" />
                </Select>
              </Column>
            </Grid>
          </div>

          {/* Fail Actions */}
          {processingValues.qcResult === "Fail" && (
            <div className="qc-section qc-fail-actions">
              <h5 className="qc-section-header">
                <FormattedMessage
                  id="notebook.immunology.section.failActions"
                  defaultMessage="Fail Actions"
                />
              </h5>
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.immunology.failWarning.title",
                  defaultMessage: "Sample(s) will be flagged",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.immunology.failWarning.subtitle",
                  defaultMessage:
                    "Failed samples will be logged and marked for review.",
                })}
                hideCloseButton
                lowContrast
              />
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="failureReason"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.failureReason",
                      defaultMessage: "Failure Reason",
                    })}
                    value={processingValues.failureReason}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        failureReason: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.failureReason.placeholder",
                      defaultMessage:
                        "Describe the reason for failure (required for audit trail)...",
                    })}
                    required
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="failAction"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.failAction",
                      defaultMessage: "Action to Take",
                    })}
                    value={processingValues.failAction}
                    onChange={(e) =>
                      setProcessingValues((prev) => ({
                        ...prev,
                        failAction: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select action..." />
                    <SelectItem
                      value="reject"
                      text={intl.formatMessage({
                        id: "notebook.immunology.failAction.reject",
                        defaultMessage: "Reject",
                      })}
                    />
                    <SelectItem
                      value="continue_with_caution"
                      text={intl.formatMessage({
                        id: "notebook.immunology.failAction.continueWithCaution",
                        defaultMessage: "Continue with Caution",
                      })}
                    />
                  </Select>
                </Column>
              </Grid>
            </div>
          )}

          {/* Remarks */}
          <div className="qc-section">
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="qcRemarks"
                  labelText={intl.formatMessage({
                    id: "notebook.immunology.remarks",
                    defaultMessage: "Processing Remarks (optional)",
                  })}
                  value={processingValues.qcRemarks}
                  onChange={(e) =>
                    setProcessingValues((prev) => ({
                      ...prev,
                      qcRemarks: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.immunology.remarks.placeholder",
                    defaultMessage:
                      "Add any additional notes or observations...",
                  })}
                />
              </Column>
            </Grid>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default ImmunologyInitialProcessingPage;
