import { useState, useEffect, useRef, useCallback } from "react";
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
  Accordion,
  AccordionItem,
  Checkbox,
  NumberInput,
} from "@carbon/react";
import { Checkmark, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBQualityCheckPage - Page 2 of the TB workflow.
 * Performs raw sample quality check with TB-specific criteria.
 *
 * QC Checks:
 * - Leak check (container integrity)
 * - Temperature check (transport temperature compliance)
 * - Packaging check (triple packaging intact)
 * - Labeling check (accuracy and legibility)
 * - Volume check (adequate specimen volume)
 * - Request match check (matches requisition form)
 *
 * QC Results:
 * - PASS: Proceed to processing
 * - PASS_TO_STORAGE: Route to storage
 * - FAIL_DISCARD: Discard sample
 * - FAIL_PROCEED: Failed but proceed with remarks
 */
function TBQualityCheckPage({
  entryId,
  pageData,
  pages,
  progress,
  onProgressUpdate,
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
  // Track if user has manually overridden the QC result via dropdown
  // This prevents auto-calculation from overwriting manual FAIL_DISCARD selections
  const [qcResultManuallySet, setQcResultManuallySet] = useState(false);

  // TB-specific QC checks state
  const [bulkApplyValues, setBulkApplyValues] = useState({
    // Individual QC checks
    leakCheck: null,
    temperatureCheck: null,
    packagingCheck: null,
    labelingCheck: null,
    volumeCheck: null,
    requestMatchCheck: null,
    // QC Result & Actions
    qcResult: "",
    destination: "",
    rejectionReason: "",
    rejectionRemarks: "",
    // Culture QC
    culturePositiveControl: null,
    cultureNegativeControl: null,
    cultureContaminationRate: "",
    // Smear Microscopy QC
    smearPositiveControl: null,
    smearNegativeControl: null,
    smearInternalQC: null,
    smearEQA: null,
    // GeneXpert QC
    genexpertCalibration: null,
    genexpertRunControls: null,
    genexpertErrorRate: "",
    // DST QC
    dstReferenceStrains: null,
    dstMICRanges: null,
    dstEQA: null,
  });

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
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
              // Use sampleType from entity (set during sample creation), fallback to page data
              specimenType:
                sample.sampleType ||
                sample.typeOfSample?.description ||
                sample.data?.specimenType,
              status: sample.pageStatus || sample.status || "PENDING",
              // QC fields from data
              qcResult: sample.data?.qcResult,
              destination: sample.data?.destination,
              rejectionReason: sample.data?.rejectionReason,
              rejectionRemarks: sample.data?.rejectionRemarks,
              // Individual check results
              leakCheck: sample.data?.leakCheck,
              temperatureCheck: sample.data?.temperatureCheck,
              packagingCheck: sample.data?.packagingCheck,
              labelingCheck: sample.data?.labelingCheck,
              volumeCheck: sample.data?.volumeCheck,
              requestMatchCheck: sample.data?.requestMatchCheck,
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

  // Route samples to a target page after QC completion
  const routeSamplesToPage = useCallback(
    (sampleIds, targetPageOrder) => {
      console.log(
        `[routeSamplesToPage] Called with sampleIds:`,
        sampleIds,
        `targetPageOrder: ${targetPageOrder}`,
      );

      if (!pages || sampleIds.length === 0) return;

      const targetPage = pages.find((p) => p.order === targetPageOrder);
      console.log(
        `[routeSamplesToPage] Found target page:`,
        targetPage
          ? `ID=${targetPage.id}, order=${targetPage.order}, title=${targetPage.title}`
          : "NOT FOUND",
      );

      if (targetPage && !String(targetPage.id).startsWith("default-")) {
        console.log(
          `[routeSamplesToPage] Posting to /rest/notebook/bulk/page/${targetPage.id}/samples/add`,
        );
        postToOpenElisServer(
          `/rest/notebook/bulk/page/${targetPage.id}/samples/add`,
          JSON.stringify({
            sampleIds: sampleIds.map((id) => parseInt(id, 10)),
          }),
          (status) => {
            console.log(
              `[routeSamplesToPage] Response status: ${status} for page ${targetPageOrder}`,
            );
            if (status === 200) {
              console.log(
                `Routed ${sampleIds.length} sample(s) to page ${targetPageOrder}`,
              );
            }
          },
        );
      }
    },
    [pages],
  );

  // Reset bulk apply form
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      leakCheck: null,
      temperatureCheck: null,
      packagingCheck: null,
      labelingCheck: null,
      volumeCheck: null,
      requestMatchCheck: null,
      qcResult: "",
      destination: "",
      rejectionReason: "",
      rejectionRemarks: "",
      // Culture QC
      culturePositiveControl: null,
      cultureNegativeControl: null,
      cultureContaminationRate: "",
      // Smear Microscopy QC
      smearPositiveControl: null,
      smearNegativeControl: null,
      smearInternalQC: null,
      smearEQA: null,
      // GeneXpert QC
      genexpertCalibration: null,
      genexpertRunControls: null,
      genexpertErrorRate: "",
      // DST QC
      dstReferenceStrains: null,
      dstMICRanges: null,
      dstEQA: null,
    });
    setQcResultManuallySet(false);
  };

  // Auto-calculate QC result based on checklist items and destination
  const calculateQcResult = useCallback(() => {
    const checks = [
      // Basic QC checks
      bulkApplyValues.leakCheck,
      bulkApplyValues.temperatureCheck,
      bulkApplyValues.packagingCheck,
      bulkApplyValues.labelingCheck,
      bulkApplyValues.volumeCheck,
      bulkApplyValues.requestMatchCheck,
      // Culture QC
      bulkApplyValues.culturePositiveControl,
      bulkApplyValues.cultureNegativeControl,
      // Smear Microscopy QC
      bulkApplyValues.smearPositiveControl,
      bulkApplyValues.smearNegativeControl,
      // GeneXpert QC
      bulkApplyValues.genexpertCalibration,
      bulkApplyValues.genexpertRunControls,
      // DST QC
      bulkApplyValues.dstReferenceStrains,
      bulkApplyValues.dstMICRanges,
    ];

    let hasAnyFail = false;
    let hasAnyCheck = false;

    checks.forEach((check) => {
      if (check !== null) {
        hasAnyCheck = true;
        if (check === false) hasAnyFail = true;
      }
    });

    // Check numeric fields with thresholds
    // Contamination Rate should be <5%
    if (
      bulkApplyValues.cultureContaminationRate !== "" &&
      bulkApplyValues.cultureContaminationRate !== null
    ) {
      hasAnyCheck = true;
      const rate = parseFloat(bulkApplyValues.cultureContaminationRate);
      if (!isNaN(rate) && rate >= 5) {
        hasAnyFail = true;
      }
    }

    // Error Rate - consider any value >10% as a failure (monitoring threshold)
    if (
      bulkApplyValues.genexpertErrorRate !== "" &&
      bulkApplyValues.genexpertErrorRate !== null
    ) {
      hasAnyCheck = true;
      const rate = parseFloat(bulkApplyValues.genexpertErrorRate);
      if (!isNaN(rate) && rate > 10) {
        hasAnyFail = true;
      }
    }

    if (!hasAnyCheck) return "";

    // If failed, return FAIL_PROCEED
    if (hasAnyFail) return "FAIL_PROCEED";

    // If passed, determine result based on destination
    const destination = bulkApplyValues.destination;
    if (
      destination === "TEMPORARY_STORAGE" ||
      destination === "LONG_TERM_STORAGE"
    ) {
      return "PASS_TO_STORAGE";
    }

    return "PASS";
  }, [bulkApplyValues]);

  // Update QC result when checks change (only if not manually set by user)
  // This prevents auto-calculation from overwriting manual FAIL_DISCARD selections
  useEffect(() => {
    // Skip auto-calculation if user has manually set a value via the dropdown
    if (qcResultManuallySet) {
      return;
    }
    const calculatedResult = calculateQcResult();
    if (calculatedResult && bulkApplyValues.qcResult !== calculatedResult) {
      setBulkApplyValues((prev) => ({
        ...prev,
        qcResult: calculatedResult,
      }));
    }
  }, [calculateQcResult, bulkApplyValues.qcResult, qcResultManuallySet]);

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.qc.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Build data object
    const data = {};

    // Add individual checks
    if (bulkApplyValues.leakCheck !== null)
      data.leakCheck = bulkApplyValues.leakCheck;
    if (bulkApplyValues.temperatureCheck !== null)
      data.temperatureCheck = bulkApplyValues.temperatureCheck;
    if (bulkApplyValues.packagingCheck !== null)
      data.packagingCheck = bulkApplyValues.packagingCheck;
    if (bulkApplyValues.labelingCheck !== null)
      data.labelingCheck = bulkApplyValues.labelingCheck;
    if (bulkApplyValues.volumeCheck !== null)
      data.volumeCheck = bulkApplyValues.volumeCheck;
    if (bulkApplyValues.requestMatchCheck !== null)
      data.requestMatchCheck = bulkApplyValues.requestMatchCheck;

    // Add QC result and routing
    if (bulkApplyValues.qcResult) data.qcResult = bulkApplyValues.qcResult;
    if (bulkApplyValues.destination)
      data.destination = bulkApplyValues.destination;
    if (bulkApplyValues.rejectionReason)
      data.rejectionReason = bulkApplyValues.rejectionReason;
    if (bulkApplyValues.rejectionRemarks)
      data.rejectionRemarks = bulkApplyValues.rejectionRemarks;

    // Add Culture QC checks
    if (bulkApplyValues.culturePositiveControl !== null)
      data.culturePositiveControl = bulkApplyValues.culturePositiveControl;
    if (bulkApplyValues.cultureNegativeControl !== null)
      data.cultureNegativeControl = bulkApplyValues.cultureNegativeControl;
    if (bulkApplyValues.cultureContaminationRate)
      data.cultureContaminationRate = bulkApplyValues.cultureContaminationRate;

    // Add Smear Microscopy QC checks
    if (bulkApplyValues.smearPositiveControl !== null)
      data.smearPositiveControl = bulkApplyValues.smearPositiveControl;
    if (bulkApplyValues.smearNegativeControl !== null)
      data.smearNegativeControl = bulkApplyValues.smearNegativeControl;
    if (bulkApplyValues.smearInternalQC !== null)
      data.smearInternalQC = bulkApplyValues.smearInternalQC;
    if (bulkApplyValues.smearEQA !== null)
      data.smearEQA = bulkApplyValues.smearEQA;

    // Add GeneXpert QC checks
    if (bulkApplyValues.genexpertCalibration !== null)
      data.genexpertCalibration = bulkApplyValues.genexpertCalibration;
    if (bulkApplyValues.genexpertRunControls !== null)
      data.genexpertRunControls = bulkApplyValues.genexpertRunControls;
    if (bulkApplyValues.genexpertErrorRate)
      data.genexpertErrorRate = bulkApplyValues.genexpertErrorRate;

    // Add DST QC checks
    if (bulkApplyValues.dstReferenceStrains !== null)
      data.dstReferenceStrains = bulkApplyValues.dstReferenceStrains;
    if (bulkApplyValues.dstMICRanges !== null)
      data.dstMICRanges = bulkApplyValues.dstMICRanges;
    if (bulkApplyValues.dstEQA !== null) data.dstEQA = bulkApplyValues.dstEQA;

    // Check if any QC checks were actually performed
    const hasAnyCheck = Object.keys(data).length > 0;
    if (!hasAnyCheck) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.qc.error.noData",
          defaultMessage:
            "Please complete at least one QC check before applying.",
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
          const isPass =
            bulkApplyValues.qcResult === "PASS" ||
            bulkApplyValues.qcResult === "PASS_TO_STORAGE";
          const resultMessage = isPass
            ? intl.formatMessage(
                {
                  id: "notebook.page.tb.qc.applied.pass",
                  defaultMessage:
                    "QC passed for {count} sample(s). Samples can proceed.",
                },
                { count: selectedSampleIds.length },
              )
            : intl.formatMessage(
                {
                  id: "notebook.page.tb.qc.applied.fail",
                  defaultMessage: "QC result applied to {count} sample(s).",
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
              id: "notebook.page.tb.qc.error.apply",
              defaultMessage: "Failed to apply QC values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    bulkApplyValues,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Mark samples as QC complete
  const handleMarkQcComplete = useCallback(() => {
    console.log(
      "[handleMarkQcComplete] Function called at",
      new Date().toISOString(),
      "with selectedSampleIds:",
      selectedSampleIds,
    );

    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.error.noPage",
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
            id: "notebook.page.tb.qc.error.missingQC",
            defaultMessage:
              "{count} sample(s) are missing QC result. Please complete verification first.",
          },
          { count: missingQC.length },
        ),
      );
      return;
    }

    // Separate samples by their QC result
    const discardedSamples = selectedSamples.filter(
      (s) => s.qcResult === "FAIL_DISCARD",
    );
    const passingSamples = selectedSamples.filter(
      (s) => s.qcResult !== "FAIL_DISCARD",
    );

    let completedRequests = 0;
    let failedRequests = 0;
    const totalRequests =
      (passingSamples.length > 0 ? 1 : 0) +
      (discardedSamples.length > 0 ? 1 : 0);

    const handleRequestComplete = () => {
      completedRequests++;
      if (completedRequests === totalRequests) {
        if (failedRequests === 0) {
          let message = "";
          if (passingSamples.length > 0 && discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.tb.qc.completedMixed",
                defaultMessage:
                  "{passCount} sample(s) completed QC. {discardCount} sample(s) were discarded.",
              },
              {
                passCount: passingSamples.length,
                discardCount: discardedSamples.length,
              },
            );
          } else if (discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.tb.qc.completedDiscarded",
                defaultMessage: "{count} sample(s) were discarded.",
              },
              { count: discardedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "notebook.page.tb.qc.completed",
                defaultMessage: "Marked {count} sample(s) as QC Complete.",
              },
              { count: passingSamples.length },
            );
          }
          setSuccessMessage(message);
          setSelectedSampleIds([]);

          // DEBUG: Log passing samples with their destinations
          console.log("=== ROUTING DEBUG ===");
          console.log(
            "Passing samples:",
            passingSamples.map((s) => ({
              id: s.id,
              accessionNumber: s.accessionNumber,
              destination: s.destination,
              qcResult: s.qcResult,
            })),
          );

          // Route samples to next page based on destination
          const processingIds = passingSamples
            .filter((s) => s.destination === "PROCESSING")
            .map((s) => parseInt(s.id, 10));

          const storageIds = passingSamples
            .filter(
              (s) =>
                s.destination === "TEMPORARY_STORAGE" ||
                s.destination === "LONG_TERM_STORAGE",
            )
            .map((s) => parseInt(s.id, 10));

          console.log(
            "Processing IDs (destination=PROCESSING):",
            processingIds,
          );
          console.log(
            "Storage IDs (destination=TEMPORARY_STORAGE or LONG_TERM_STORAGE):",
            storageIds,
          );
          console.log("==================");

          if (processingIds.length > 0) {
            routeSamplesToPage(processingIds, 3); // Route to Page 3 (Initial Sample Processing)
          }

          if (storageIds.length > 0) {
            routeSamplesToPage(storageIds, 6); // Route to Page 6 (Storage Assignment)
          }

          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.tb.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      }
    };

    // Update passing samples to COMPLETED
    if (passingSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: passingSamples.map((s) => parseInt(s.id, 10)),
          status: "COMPLETED",
          skipAutoRouting: true, // TB workflow handles routing explicitly based on destination
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    // Update discarded samples to SKIPPED
    if (discardedSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: discardedSamples.map((s) => parseInt(s.id, 10)),
          status: "SKIPPED",
          skipAutoRouting: true, // TB workflow handles routing explicitly
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
    routeSamplesToPage,
  ]);

  // Calculate stats
  const qcPassedCount = samples.filter(
    (s) => s.qcResult === "PASS" || s.qcResult === "PASS_TO_STORAGE",
  ).length;
  const qcFailedCount = samples.filter(
    (s) => s.qcResult === "FAIL_DISCARD" || s.qcResult === "FAIL_PROCEED",
  ).length;
  const qcPendingCount = samples.filter((s) => !s.qcResult).length;
  const verifiedCount = samples.filter((s) => s.status === "COMPLETED").length;

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "PASS") return <Tag type="green">Pass</Tag>;
    if (qcResult === "PASS_TO_STORAGE")
      return <Tag type="teal">Pass (Storage)</Tag>;
    if (qcResult === "FAIL_DISCARD")
      return <Tag type="red">Fail (Discard)</Tag>;
    if (qcResult === "FAIL_PROCEED")
      return <Tag type="purple">Fail (Proceed)</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  // Get destination tag
  const getDestinationTag = (destination) => {
    if (!destination) return null;
    const destinationLabels = {
      PROCESSING: { type: "blue", label: "Processing" },
      TEMPORARY_STORAGE: { type: "cyan", label: "Temp Storage" },
      SHIPMENT: { type: "teal", label: "Shipment" },
      LONG_TERM_STORAGE: { type: "purple", label: "Long-term Storage" },
      DISCARDED: { type: "red", label: "Discarded" },
    };
    const config = destinationLabels[destination] || {
      type: "gray",
      label: destination,
    };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  return (
    <div className="tb-quality-check-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tb.qc.title"
            defaultMessage="Raw Sample Quality Check (QC)"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.qc.description"
            defaultMessage="Verify container integrity, temperature compliance, packaging, labeling accuracy, volume, and requisition matching. Select samples and use Bulk Apply to perform quality checks."
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
                  id="notebook.page.tb.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.qc.passed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.qc.failed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.qc.pending"
                  defaultMessage="QC Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.qc.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
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
            resetBulkApplyValues();
            setBulkApplyModalOpen(true);
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tb.qc.bulkApply"
            defaultMessage="Bulk Apply QC ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkQcComplete}
          >
            <FormattedMessage
              id="notebook.page.tb.qc.markComplete"
              defaultMessage="Mark QC Complete ({count})"
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

      {/* Pending QC Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.qc.pendingTable.title"
              defaultMessage="Samples Pending QC"
            />
            <Tag type="gray" className="count-tag">
              {
                samples.filter(
                  (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.qc.pendingTable.description"
              defaultMessage="Select samples and use 'Bulk Apply QC' to perform quality checks."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="pending-qc"
            samples={samples.filter(
              (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
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
                key: "specimenType",
                header: intl.formatMessage({
                  id: "notebook.grid.specimenType",
                  defaultMessage: "Specimen Type",
                }),
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
                key: "destination",
                header: intl.formatMessage({
                  id: "notebook.grid.destination",
                  defaultMessage: "Destination",
                }),
                render: (value) => getDestinationTag(value),
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
            (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tb.qc.pendingTable.empty"
                  defaultMessage="No samples pending QC. All samples have been processed."
                />
              </p>
            </div>
          )}
      </div>

      {/* Completed QC Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.qc.completedTable.title"
              defaultMessage="QC Completed Samples"
            />
            <Tag type="green" className="count-tag">
              {
                samples.filter(
                  (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.qc.completedTable.description"
              defaultMessage="Samples that have completed quality check."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="completed-qc"
            samples={samples.filter(
              (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
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
                key: "specimenType",
                header: intl.formatMessage({
                  id: "notebook.grid.specimenType",
                  defaultMessage: "Specimen Type",
                }),
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
                key: "destination",
                header: intl.formatMessage({
                  id: "notebook.grid.destination",
                  defaultMessage: "Destination",
                }),
                render: (value) => getDestinationTag(value),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.disposition",
                  defaultMessage: "Disposition",
                }),
                render: (value, sample) => {
                  if (value === "SKIPPED") {
                    return (
                      <Tag type="red">
                        <FormattedMessage
                          id="notebook.disposition.discarded"
                          defaultMessage="Discarded"
                        />
                      </Tag>
                    );
                  }
                  if (value === "COMPLETED") {
                    // Check qcResult to determine disposition
                    if (sample?.qcResult === "PASS_TO_STORAGE") {
                      return (
                        <Tag type="cyan">
                          <FormattedMessage
                            id="notebook.disposition.toStorage"
                            defaultMessage="To Storage"
                          />
                        </Tag>
                      );
                    }
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
            (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tb.qc.completedTable.empty"
                  defaultMessage="No samples have completed QC yet."
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
              id="notebook.page.tb.qc.empty"
              defaultMessage="No samples available yet. Complete Sample Creation first, then perform QC."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tb.bulkApply.title",
          defaultMessage: "TB Sample Quality Check",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : bulkApplyValues.qcResult === "PASS" ||
                bulkApplyValues.qcResult === "PASS_TO_STORAGE"
              ? intl.formatMessage({
                  id: "notebook.tb.qc.action.pass",
                  defaultMessage: "Pass - Proceed",
                })
              : bulkApplyValues.qcResult === "FAIL_DISCARD"
                ? intl.formatMessage({
                    id: "notebook.tb.qc.action.discard",
                    defaultMessage: "Fail - Discard Sample(s)",
                  })
                : intl.formatMessage({
                    id: "label.apply",
                    defaultMessage: "Apply QC",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isBulkApplying || !bulkApplyValues.qcResult}
        danger={bulkApplyValues.qcResult === "FAIL_DISCARD"}
      >
        <div className="qc-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.tb.bulkApply.description"
              defaultMessage="Perform quality checks on {count} selected TB sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* TB QC Checks Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.tb.qc.section.checks"
                defaultMessage="Sample QC Checks"
              />
            </h5>
            <Grid fullWidth className="qc-checklist">
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.tb.qc.leakCheck",
                    defaultMessage: "Leak Check (container integrity)",
                  })}
                  name="leakCheck"
                  valueSelected={
                    bulkApplyValues.leakCheck === true
                      ? "pass"
                      : bulkApplyValues.leakCheck === false
                        ? "fail"
                        : ""
                  }
                  onChange={(value) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      leakCheck: value === "pass",
                    }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton
                    labelText="Pass"
                    value="pass"
                    id="leak-check-pass"
                  />
                  <RadioButton
                    labelText="Fail"
                    value="fail"
                    id="leak-check-fail"
                  />
                </RadioButtonGroup>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.tb.qc.temperatureCheck",
                    defaultMessage: "Temperature Check (transport compliance)",
                  })}
                  name="temperatureCheck"
                  valueSelected={
                    bulkApplyValues.temperatureCheck === true
                      ? "pass"
                      : bulkApplyValues.temperatureCheck === false
                        ? "fail"
                        : ""
                  }
                  onChange={(value) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      temperatureCheck: value === "pass",
                    }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton
                    labelText="Pass"
                    value="pass"
                    id="temp-check-pass"
                  />
                  <RadioButton
                    labelText="Fail"
                    value="fail"
                    id="temp-check-fail"
                  />
                </RadioButtonGroup>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.tb.qc.packagingCheck",
                    defaultMessage: "Packaging Check (triple packaging intact)",
                  })}
                  name="packagingCheck"
                  valueSelected={
                    bulkApplyValues.packagingCheck === true
                      ? "pass"
                      : bulkApplyValues.packagingCheck === false
                        ? "fail"
                        : ""
                  }
                  onChange={(value) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      packagingCheck: value === "pass",
                    }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton
                    labelText="Pass"
                    value="pass"
                    id="packaging-check-pass"
                  />
                  <RadioButton
                    labelText="Fail"
                    value="fail"
                    id="packaging-check-fail"
                  />
                </RadioButtonGroup>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.tb.qc.labelingCheck",
                    defaultMessage: "Labeling Check (accurate and legible)",
                  })}
                  name="labelingCheck"
                  valueSelected={
                    bulkApplyValues.labelingCheck === true
                      ? "pass"
                      : bulkApplyValues.labelingCheck === false
                        ? "fail"
                        : ""
                  }
                  onChange={(value) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      labelingCheck: value === "pass",
                    }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton
                    labelText="Pass"
                    value="pass"
                    id="labeling-check-pass"
                  />
                  <RadioButton
                    labelText="Fail"
                    value="fail"
                    id="labeling-check-fail"
                  />
                </RadioButtonGroup>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.tb.qc.volumeCheck",
                    defaultMessage: "Volume Check (adequate specimen volume)",
                  })}
                  name="volumeCheck"
                  valueSelected={
                    bulkApplyValues.volumeCheck === true
                      ? "pass"
                      : bulkApplyValues.volumeCheck === false
                        ? "fail"
                        : ""
                  }
                  onChange={(value) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      volumeCheck: value === "pass",
                    }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton
                    labelText="Pass"
                    value="pass"
                    id="volume-check-pass"
                  />
                  <RadioButton
                    labelText="Fail"
                    value="fail"
                    id="volume-check-fail"
                  />
                </RadioButtonGroup>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <RadioButtonGroup
                  legendText={intl.formatMessage({
                    id: "notebook.tb.qc.requestMatchCheck",
                    defaultMessage: "Request Match (matches requisition form)",
                  })}
                  name="requestMatchCheck"
                  valueSelected={
                    bulkApplyValues.requestMatchCheck === true
                      ? "pass"
                      : bulkApplyValues.requestMatchCheck === false
                        ? "fail"
                        : ""
                  }
                  onChange={(value) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      requestMatchCheck: value === "pass",
                    }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton
                    labelText="Pass"
                    value="pass"
                    id="request-match-pass"
                  />
                  <RadioButton
                    labelText="Fail"
                    value="fail"
                    id="request-match-fail"
                  />
                </RadioButtonGroup>
              </Column>
            </Grid>
          </div>

          {/* Additional QC Checks (Optional) */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.tb.qc.section.additionalQC"
                defaultMessage="Additional QC Checks (Optional)"
              />
            </h5>
            <p
              style={{
                marginBottom: "1rem",
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              <FormattedMessage
                id="notebook.tb.qc.section.additionalQC.description"
                defaultMessage="Expand sections below to record method-specific quality control results."
              />
            </p>
            <Accordion>
              {/* Culture QC */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.tb.qc.cultureQC",
                  defaultMessage: "Culture QC",
                })}
              >
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.culturePositiveControl",
                        defaultMessage: "Positive Control (known MTB strain)",
                      })}
                      name="culturePositiveControl"
                      valueSelected={
                        bulkApplyValues.culturePositiveControl === true
                          ? "pass"
                          : bulkApplyValues.culturePositiveControl === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          culturePositiveControl: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="culture-pos-control-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="culture-pos-control-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.cultureNegativeControl",
                        defaultMessage: "Negative Control (sterile media)",
                      })}
                      name="cultureNegativeControl"
                      valueSelected={
                        bulkApplyValues.cultureNegativeControl === true
                          ? "pass"
                          : bulkApplyValues.cultureNegativeControl === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          cultureNegativeControl: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="culture-neg-control-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="culture-neg-control-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <NumberInput
                      id="cultureContaminationRate"
                      label={intl.formatMessage({
                        id: "notebook.tb.qc.cultureContaminationRate",
                        defaultMessage: "Contamination Rate (%)",
                      })}
                      helperText={intl.formatMessage({
                        id: "notebook.tb.qc.cultureContaminationRate.helper",
                        defaultMessage: "Should be <5%",
                      })}
                      min={0}
                      max={100}
                      step={0.1}
                      value={bulkApplyValues.cultureContaminationRate}
                      onChange={(e, { value }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          cultureContaminationRate: value,
                        }))
                      }
                      invalid={false}
                      allowEmpty={true}
                    />
                  </Column>
                </Grid>
              </AccordionItem>

              {/* Smear Microscopy QC */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.tb.qc.smearMicroscopyQC",
                  defaultMessage: "Smear Microscopy QC",
                })}
              >
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.smearPositiveControl",
                        defaultMessage: "Positive Control Slide",
                      })}
                      name="smearPositiveControl"
                      valueSelected={
                        bulkApplyValues.smearPositiveControl === true
                          ? "pass"
                          : bulkApplyValues.smearPositiveControl === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          smearPositiveControl: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="smear-pos-control-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="smear-pos-control-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.smearNegativeControl",
                        defaultMessage: "Negative Control Slide",
                      })}
                      name="smearNegativeControl"
                      valueSelected={
                        bulkApplyValues.smearNegativeControl === true
                          ? "pass"
                          : bulkApplyValues.smearNegativeControl === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          smearNegativeControl: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="smear-neg-control-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="smear-neg-control-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="smearInternalQC"
                      labelText={intl.formatMessage({
                        id: "notebook.tb.qc.smearInternalQC",
                        defaultMessage: "Internal QC (10% re-read)",
                      })}
                      checked={bulkApplyValues.smearInternalQC === true}
                      onChange={(e, { checked }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          smearInternalQC: checked,
                        }))
                      }
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="smearEQA"
                      labelText={intl.formatMessage({
                        id: "notebook.tb.qc.smearEQA",
                        defaultMessage: "External Quality Assessment (EQA)",
                      })}
                      checked={bulkApplyValues.smearEQA === true}
                      onChange={(e, { checked }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          smearEQA: checked,
                        }))
                      }
                    />
                  </Column>
                </Grid>
              </AccordionItem>

              {/* GeneXpert QC */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.tb.qc.genexpertQC",
                  defaultMessage: "GeneXpert QC",
                })}
              >
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.genexpertCalibration",
                        defaultMessage: "Daily Calibration Check",
                      })}
                      name="genexpertCalibration"
                      valueSelected={
                        bulkApplyValues.genexpertCalibration === true
                          ? "pass"
                          : bulkApplyValues.genexpertCalibration === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          genexpertCalibration: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="genexpert-cal-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="genexpert-cal-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.genexpertRunControls",
                        defaultMessage: "Run Controls (per manufacturer)",
                      })}
                      name="genexpertRunControls"
                      valueSelected={
                        bulkApplyValues.genexpertRunControls === true
                          ? "pass"
                          : bulkApplyValues.genexpertRunControls === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          genexpertRunControls: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="genexpert-controls-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="genexpert-controls-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <NumberInput
                      id="genexpertErrorRate"
                      label={intl.formatMessage({
                        id: "notebook.tb.qc.genexpertErrorRate",
                        defaultMessage: "Error Rate (%)",
                      })}
                      helperText={intl.formatMessage({
                        id: "notebook.tb.qc.genexpertErrorRate.helper",
                        defaultMessage: "Monitor error rate trends",
                      })}
                      min={0}
                      max={100}
                      step={0.1}
                      value={bulkApplyValues.genexpertErrorRate}
                      onChange={(e, { value }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          genexpertErrorRate: value,
                        }))
                      }
                      invalid={false}
                      allowEmpty={true}
                    />
                  </Column>
                </Grid>
              </AccordionItem>

              {/* DST QC */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.tb.qc.dstQC",
                  defaultMessage: "Drug Susceptibility Testing (DST) QC",
                })}
              >
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.dstReferenceStrains",
                        defaultMessage: "ATCC Reference Strains (per batch)",
                      })}
                      name="dstReferenceStrains"
                      valueSelected={
                        bulkApplyValues.dstReferenceStrains === true
                          ? "pass"
                          : bulkApplyValues.dstReferenceStrains === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          dstReferenceStrains: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="dst-ref-strains-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="dst-ref-strains-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.tb.qc.dstMICRanges",
                        defaultMessage: "MIC Ranges (within acceptable limits)",
                      })}
                      name="dstMICRanges"
                      valueSelected={
                        bulkApplyValues.dstMICRanges === true
                          ? "pass"
                          : bulkApplyValues.dstMICRanges === false
                            ? "fail"
                            : ""
                      }
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          dstMICRanges: value === "pass",
                        }))
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        labelText="Pass"
                        value="pass"
                        id="dst-mic-ranges-pass"
                      />
                      <RadioButton
                        labelText="Fail"
                        value="fail"
                        id="dst-mic-ranges-fail"
                      />
                    </RadioButtonGroup>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="dstEQA"
                      labelText={intl.formatMessage({
                        id: "notebook.tb.qc.dstEQA",
                        defaultMessage: "EQA Participation",
                      })}
                      checked={bulkApplyValues.dstEQA === true}
                      onChange={(e, { checked }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          dstEQA: checked,
                        }))
                      }
                    />
                  </Column>
                </Grid>
              </AccordionItem>
            </Accordion>
          </div>

          {/* QC Result Summary */}
          <div className="qc-section qc-result-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.tb.qc.section.result"
                defaultMessage="QC Result"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <div className="qc-result-display">
                  {bulkApplyValues.qcResult === "PASS" && (
                    <Tag type="green" size="lg">
                      <FormattedMessage
                        id="notebook.tb.qc.result.pass"
                        defaultMessage="PASS - Proceed"
                      />
                    </Tag>
                  )}
                  {bulkApplyValues.qcResult === "PASS_TO_STORAGE" && (
                    <Tag type="teal" size="lg">
                      <FormattedMessage
                        id="notebook.tb.qc.result.passToStorage"
                        defaultMessage="PASS - Route to Storage"
                      />
                    </Tag>
                  )}
                  {(bulkApplyValues.qcResult === "FAIL_DISCARD" ||
                    bulkApplyValues.qcResult === "FAIL_PROCEED") && (
                    <Tag type="red" size="lg">
                      <FormattedMessage
                        id="notebook.tb.qc.result.fail"
                        defaultMessage="FAIL"
                      />
                    </Tag>
                  )}
                  {!bulkApplyValues.qcResult && (
                    <Tag type="gray" size="lg">
                      <FormattedMessage
                        id="notebook.tb.qc.result.pending"
                        defaultMessage="Complete checklist above"
                      />
                    </Tag>
                  )}
                </div>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="qcResultOverride"
                  labelText={intl.formatMessage({
                    id: "notebook.tb.qc.resultOverride",
                    defaultMessage: "Override QC Result",
                  })}
                  value={bulkApplyValues.qcResult}
                  onChange={(e) => {
                    const newValue = e.target.value;
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      qcResult: newValue,
                    }));
                    // Mark as manually set if user selects a specific value
                    // This prevents auto-calculation from overwriting their choice
                    setQcResultManuallySet(newValue !== "");
                  }}
                >
                  <SelectItem value="" text="Auto (based on checklist)" />
                  <SelectItem
                    value="PASS"
                    text="Pass - Proceed to Processing"
                  />
                  <SelectItem
                    value="PASS_TO_STORAGE"
                    text="Pass - Route to Storage"
                  />
                  <SelectItem
                    value="FAIL_PROCEED"
                    text="Fail - Proceed with Remarks"
                  />
                  <SelectItem
                    value="FAIL_DISCARD"
                    text="Fail - Discard Sample"
                  />
                </Select>
              </Column>
            </Grid>
          </div>

          {/* Destination Selection */}
          <div className="qc-section">
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="destination"
                  labelText={intl.formatMessage({
                    id: "notebook.tb.qc.destination",
                    defaultMessage: "Destination Routing",
                  })}
                  value={bulkApplyValues.destination}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      destination: e.target.value,
                    }))
                  }
                >
                  <SelectItem value="" text="Select destination..." />
                  <SelectItem value="PROCESSING" text="Processing" />
                  <SelectItem
                    value="TEMPORARY_STORAGE"
                    text="Temporary Storage"
                  />
                  <SelectItem value="SHIPMENT" text="Shipment" />
                  <SelectItem
                    value="LONG_TERM_STORAGE"
                    text="Long-term Storage"
                  />
                  <SelectItem value="DISCARDED" text="Discarded" />
                </Select>
              </Column>
            </Grid>
          </div>

          {/* Fail Actions */}
          {(bulkApplyValues.qcResult === "FAIL_DISCARD" ||
            bulkApplyValues.qcResult === "FAIL_PROCEED") && (
            <div className="qc-section qc-fail-actions">
              <h5 className="qc-section-header">
                <FormattedMessage
                  id="notebook.tb.qc.section.failActions"
                  defaultMessage="Rejection Details"
                />
              </h5>
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.tb.qc.failWarning.title",
                  defaultMessage: "Sample(s) failed QC",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.tb.qc.failWarning.subtitle",
                  defaultMessage:
                    "Please provide rejection reason for audit trail.",
                })}
                hideCloseButton
                lowContrast
              />
              <Grid fullWidth>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="rejectionReason"
                    labelText={intl.formatMessage({
                      id: "notebook.tb.qc.rejectionReason",
                      defaultMessage: "Rejection Reason",
                    })}
                    value={bulkApplyValues.rejectionReason}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        rejectionReason: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select reason..." />
                    <SelectItem value="MISLABELING" text="Mislabeling" />
                    <SelectItem
                      value="INSUFFICIENT_SAMPLE"
                      text="Insufficient Sample"
                    />
                    <SelectItem value="CONTAMINATED" text="Contaminated" />
                    <SelectItem
                      value="TEMPERATURE_DEVIATION"
                      text="Temperature Deviation"
                    />
                    <SelectItem
                      value="PACKAGING_ISSUE"
                      text="Packaging Issue"
                    />
                    <SelectItem
                      value="REQUEST_MISMATCH"
                      text="Request Mismatch"
                    />
                    <SelectItem value="OTHER" text="Other" />
                  </Select>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="rejectionRemarks"
                    labelText={intl.formatMessage({
                      id: "notebook.tb.qc.rejectionRemarks",
                      defaultMessage: "Rejection Remarks",
                    })}
                    value={bulkApplyValues.rejectionRemarks}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        rejectionRemarks: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.tb.qc.rejectionRemarks.placeholder",
                      defaultMessage:
                        "Describe the reason for rejection (required for audit trail)...",
                    })}
                  />
                </Column>
              </Grid>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default TBQualityCheckPage;
