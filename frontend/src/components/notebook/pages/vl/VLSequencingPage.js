import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useRef,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  DatePickerInput,
  Grid,
  Column,
  Tile,
  TextArea,
  Tag,
  Loading,
} from "@carbon/react";
import { Renew, CheckmarkFilled, Chemistry } from "@carbon/react/icons";
import useVLPermissions from "../../../../hooks/useVLPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VLSequencingPage - Page 8: Sequencing
 *
 * Manages sequencing data collection and run metrics using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Records sequencer, run ID, cluster density, Q30 score, and operator info
 * - Tracks sample progression to Bioinformatics Analysis (Page 9)
 *
 * Data stored in sample.data JSONB:
 * {
 *   sequencing: {
 *     sequencer: "Illumina|DNBSEQ",
 *     runId: "Run identifier string",
 *     clusterDensity: Number (M),
 *     q30Score: Number (%),
 *     operator: "Name of sequencer operator",
 *     dateTime: "2024-01-27",
 *     notes: "Run observations"
 *   }
 * }
 */
export const VLSequencingPage = ({
  entryId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { getPagePermissionLevel, canSaveData, canAccessSequencing } =
    useVLPermissions();
  const { hasAnyRole } = usePermissions();

  // Sequencing allowed roles - per VL permission mapping
  const allowedRoles = [
    "VL Lab Technician",
    "VL Bioinformatician",
    "VL Manager",
    "VL Principal Investigator",
  ];

  // Layer 1: Page access check - use both VL-specific and role-based checking
  const canAccessPage = canAccessSequencing() || hasAnyRole(allowedRoles);

  // Layer 2: Action permission check - what can user do on this page
  const pagePermissionLevel = getPagePermissionLevel("Sequencing");
  const canPerformSequencing = canSaveData(pagePermissionLevel);

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [sequencingModalOpen, setSequencingModalOpen] = useState(false);
  const [isApplyingSequencing, setIsApplyingSequencing] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  // Form state for sequencing data
  const [sequencer, setSequencer] = useState(null);
  const [normalizationNotes, setNormalizationNotes] = useState("");
  const [poolingDetails, setPoolingDetails] = useState("");
  const [finalQuantityCheck, setFinalQuantityCheck] = useState("");
  const [loadingNotes, setLoadingNotes] = useState("");
  const [runId, setRunId] = useState("");
  const [readLength, setReadLength] = useState("");
  const [numberOfCycles, setNumberOfCycles] = useState("");
  const [flowCellType, setFlowCellType] = useState("");
  const [clusterDensity, setClusterDensity] = useState("");
  const [q30Score, setQ30Score] = useState("");
  const [errorRate, setErrorRate] = useState("");
  const [operator, setOperator] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [sequencingTime, setSequencingTime] = useState("09:00");
  const [notes, setNotes] = useState("");

  // ALCOA+ Audit Trail fields
  const [recordedBy, setRecordedBy] = useState("");
  const [recordedDate, setRecordedDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [recordedTime, setRecordedTime] = useState("09:00");
  const [lastModifiedBy, setLastModifiedBy] = useState("");
  const [lastModifiedDate, setLastModifiedDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [lastModifiedTime, setLastModifiedTime] = useState("09:00");

  // 3-Tier Review Workflow fields
  const [primaryReviewCompleted, setPrimaryReviewCompleted] = useState(false);
  const [primaryReviewedBy, setPrimaryReviewedBy] = useState("");
  const [primaryReviewedDate, setPrimaryReviewedDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [primaryReviewedTime, setPrimaryReviewedTime] = useState("09:00");

  const [bioReviewCompleted, setBioReviewCompleted] = useState(false);
  const [bioReviewedBy, setBioReviewedBy] = useState("");
  const [bioReviewedDate, setBioReviewedDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [bioReviewedTime, setBioReviewedTime] = useState("09:00");

  const [finalApprovalCompleted, setFinalApprovalCompleted] = useState(false);
  const [finalApprovedBy, setFinalApprovedBy] = useState("");
  const [finalApprovedDate, setFinalApprovedDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [finalApprovedTime, setFinalApprovedTime] = useState("09:00");

  const sequencerOptions = [
    { id: "nextseq-500", label: "Illumina NextSeq 500" },
    { id: "nextseq-2000", label: "Illumina NextSeq 2000" },
    { id: "dnbseq-g400", label: "MGI DNBSEQ G400" },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          let samplesToProcess = [];

          // Handle both array and object responses from API
          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          setSamples(
            samplesToProcess.length > 0
              ? samplesToProcess.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  sampleType: s.data?.sampleType,
                  collectionDate: s.data?.collectionDate,
                  source: s.data?.source,
                  sequencingPlatform: s.data?.libraryPrep?.platform,
                  libraryConcentration: s.data?.libraryPrep?.concentration,
                  chipType: s.data?.bioanalyzerQc?.chipType,
                  bioanalyzerConcentration:
                    s.data?.bioanalyzerQc?.concentration,
                  sequencer: s.data?.sequencing?.sequencer,
                  runId: s.data?.sequencing?.runId,
                  readLength: s.data?.sequencing?.readLength,
                  numberOfCycles: s.data?.sequencing?.numberOfCycles,
                  flowCellType: s.data?.sequencing?.flowCellType,
                  clusterDensity: s.data?.sequencing?.clusterDensity,
                  q30Score: s.data?.sequencing?.q30Score,
                  errorRate: s.data?.sequencing?.errorRate,
                  sequencingOperator: s.data?.sequencing?.operator,
                  sequencingDateTime: s.data?.sequencing?.dateTime,
                  sequencingNotes: s.data?.sequencing?.notes,
                  // ALCOA+ Audit Trail
                  recordedBy: s.data?.sequencing?.auditTrail?.recordedBy,
                  recordedDate: s.data?.sequencing?.auditTrail?.recordedDate,
                  recordedTime: s.data?.sequencing?.auditTrail?.recordedTime,
                  lastModifiedBy:
                    s.data?.sequencing?.auditTrail?.lastModifiedBy,
                  lastModifiedDate:
                    s.data?.sequencing?.auditTrail?.lastModifiedDate,
                  lastModifiedTime:
                    s.data?.sequencing?.auditTrail?.lastModifiedTime,
                  // 3-Tier Review Workflow
                  primaryReviewCompleted:
                    s.data?.sequencing?.review?.primaryReview?.completed,
                  primaryReviewedBy:
                    s.data?.sequencing?.review?.primaryReview?.reviewedBy,
                  bioReviewCompleted:
                    s.data?.sequencing?.review?.bioinformaticsReview?.completed,
                  bioReviewedBy:
                    s.data?.sequencing?.review?.bioinformaticsReview
                      ?.reviewedBy,
                  finalApprovalCompleted:
                    s.data?.sequencing?.review?.finalApproval?.completed,
                  finalApprovedBy:
                    s.data?.sequencing?.review?.finalApproval?.approvedBy,
                }))
              : [],
          );
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetForm = useCallback(() => {
    setSequencer(null);
    setNormalizationNotes("");
    setPoolingDetails("");
    setFinalQuantityCheck("");
    setLoadingNotes("");
    setRunId("");
    setReadLength("");
    setNumberOfCycles("");
    setFlowCellType("");
    setClusterDensity("");
    setQ30Score("");
    setErrorRate("");
    setOperator("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setSequencingTime("09:00");
    setNotes("");

    // Reset ALCOA+ audit trail fields
    setRecordedBy("");
    setRecordedDate(new Date().toISOString().split("T")[0]);
    setRecordedTime("09:00");
    setLastModifiedBy("");
    setLastModifiedDate(new Date().toISOString().split("T")[0]);
    setLastModifiedTime("09:00");

    // Reset 3-Tier Review Workflow fields
    setPrimaryReviewCompleted(false);
    setPrimaryReviewedBy("");
    setPrimaryReviewedDate(new Date().toISOString().split("T")[0]);
    setPrimaryReviewedTime("09:00");

    setBioReviewCompleted(false);
    setBioReviewedBy("");
    setBioReviewedDate(new Date().toISOString().split("T")[0]);
    setBioReviewedTime("09:00");

    setFinalApprovalCompleted(false);
    setFinalApprovedBy("");
    setFinalApprovedDate(new Date().toISOString().split("T")[0]);
    setFinalApprovedTime("09:00");
  }, []);

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.sequencing.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setSequencingModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applySequencing = useCallback(() => {
    // Validation - sequencer is required
    if (!sequencer) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.sequencing.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.vl.sequencing.error.sequencerRequired",
          defaultMessage: "Sequencer is required",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.sequencing.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingSequencing(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    // Use the bulk apply endpoint to save sequencing data
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          sequencing: {
            preSequencing: {
              normalizationNotes: normalizationNotes || null,
              poolingDetails: poolingDetails || null,
              finalQuantityCheck: finalQuantityCheck || null,
              loadingNotes: loadingNotes || null,
            },
            sequencer: sequencer.id,
            runId: runId || null,
            readLength: readLength ? parseInt(readLength, 10) : null,
            numberOfCycles: numberOfCycles
              ? parseInt(numberOfCycles, 10)
              : null,
            flowCellType: flowCellType || null,
            clusterDensity: clusterDensity ? parseFloat(clusterDensity) : null,
            q30Score: q30Score ? parseFloat(q30Score) : null,
            errorRate: errorRate ? parseFloat(errorRate) : null,
            operator,
            dateTime:
              dateTime && sequencingTime
                ? `${dateTime}T${sequencingTime}:00Z`
                : null,
            notes,
            // ALCOA+ Audit Trail
            auditTrail: {
              recordedBy: recordedBy || null,
              recordedDate: recordedDate
                ? `${recordedDate}T${recordedTime}:00Z`
                : null,
              lastModifiedBy: lastModifiedBy || null,
              lastModifiedDate: lastModifiedDate
                ? `${lastModifiedDate}T${lastModifiedTime}:00Z`
                : null,
            },
            // 3-Tier Review Workflow
            review: {
              primaryReview: {
                completed: primaryReviewCompleted,
                reviewedBy: primaryReviewCompleted
                  ? primaryReviewedBy || null
                  : null,
                reviewedDate:
                  primaryReviewCompleted && primaryReviewedDate
                    ? `${primaryReviewedDate}T${primaryReviewedTime}:00Z`
                    : null,
              },
              bioinformaticsReview: {
                completed: bioReviewCompleted,
                reviewedBy: bioReviewCompleted ? bioReviewedBy || null : null,
                reviewedDate:
                  bioReviewCompleted && bioReviewedDate
                    ? `${bioReviewedDate}T${bioReviewedTime}:00Z`
                    : null,
              },
              finalApproval: {
                completed: finalApprovalCompleted,
                approvedBy: finalApprovalCompleted
                  ? finalApprovedBy || null
                  : null,
                approvedDate:
                  finalApprovalCompleted && finalApprovedDate
                    ? `${finalApprovedDate}T${finalApprovedTime}:00Z`
                    : null,
              },
            },
          },
        },
      }),
      (response) => {
        setIsApplyingSequencing(false);
        if (response?.success) {
          // Update sample status to IN_PROGRESS after sequencing recording
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title: intl.formatMessage(
                    {
                      id: "notebook.vl.sequencing.success",
                      defaultMessage:
                        "Sequencing recorded for {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setSequencingModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.vl.sequencing.error.statusUpdate",
                    defaultMessage:
                      "Sequencing recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Sequencing failed",
          });
        }
      },
    );
  }, [
    sequencer,
    normalizationNotes,
    poolingDetails,
    finalQuantityCheck,
    loadingNotes,
    runId,
    readLength,
    numberOfCycles,
    flowCellType,
    clusterDensity,
    q30Score,
    errorRate,
    operator,
    dateTime,
    sequencingTime,
    notes,
    recordedBy,
    recordedDate,
    recordedTime,
    lastModifiedBy,
    lastModifiedDate,
    lastModifiedTime,
    primaryReviewCompleted,
    primaryReviewedBy,
    primaryReviewedDate,
    primaryReviewedTime,
    bioReviewCompleted,
    bioReviewedBy,
    bioReviewedDate,
    bioReviewedTime,
    finalApprovalCompleted,
    finalApprovedBy,
    finalApprovedDate,
    finalApprovedTime,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Handle marking sequencing samples complete
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected and in sequencing (IN_PROGRESS status)
    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.vl.sequencing.noEligibleSamples",
          defaultMessage:
            "Selected samples must have sequencing recorded (status: In Progress) before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.vl.sequencing.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.vl.sequencing.completeFailed",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Count of selected samples eligible for completion (IN_PROGRESS status)
  const eligibleForCompletionCount = useMemo(
    () =>
      samples.filter(
        (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
      ).length,
    [samples, selectedSampleIds],
  );

  // Filter samples by status
  const readyForSequencingSamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "AWAITING" ||
          s.status === "IN_PROGRESS",
      ),
    [samples],
  );

  const sequencingCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Sequencing"
        reason="This page requires specific VL laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Helper to render sample status
  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.vl.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm">
            <FormattedMessage
              id="notebook.vl.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.vl.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  // Helper to render review status
  const renderReviewStatus = (sample) => {
    const primaryCompleted = sample.primaryReviewCompleted;
    const bioCompleted = sample.bioReviewCompleted;
    const finalCompleted = sample.finalApprovalCompleted;

    if (finalCompleted) {
      return (
        <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
          <FormattedMessage
            id="notebook.vl.sequencing.fullyApproved"
            defaultMessage="Fully Approved"
          />
        </Tag>
      );
    }

    if (bioCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.vl.sequencing.bioApproved"
            defaultMessage="Bioinformatics Approved"
          />
        </Tag>
      );
    }

    if (primaryCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.vl.sequencing.primaryApproved"
            defaultMessage="Primary Approved"
          />
        </Tag>
      );
    }

    return (
      <Tag type="gray" size="sm">
        <FormattedMessage
          id="notebook.vl.sequencing.reviewPending"
          defaultMessage="Pending Review"
        />
      </Tag>
    );
  };

  return (
    <div className="vl-sequencing-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.vl.sequencing.title"
            defaultMessage="Sequencing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.vl.sequencing.description"
            defaultMessage="Record sequencing run data and quality metrics. Capture sequencer, run ID, cluster density, Q30 score, and operator information."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.vl.sequencing.ready"
                  defaultMessage="Ready for Sequencing"
                />
              </span>
              <span className="progress-value">
                {readyForSequencingSamples.length}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.vl.sequencing.completed"
                  defaultMessage="Sequencing Completed"
                />
              </span>
              <span className="progress-value">
                {sequencingCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={openModal}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            !canPerformSequencing
          }
        >
          <FormattedMessage
            id="notebook.vl.recordSequencing"
            defaultMessage="Record Sequencing ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={
            eligibleForCompletionCount === 0 ||
            isCompleting ||
            !hasRealPageId ||
            !canPerformSequencing
          }
        >
          <FormattedMessage
            id="notebook.vl.markComplete"
            defaultMessage="Mark Complete ({count})"
            values={{ count: eligibleForCompletionCount }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.vl.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.vl.sequencing.ready.title"
              defaultMessage="Samples Ready for Sequencing"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForSequencingSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForSequencingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.vl.sequencing.ready.empty"
                  defaultMessage="No samples ready for sequencing."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="vl-sequencing-ready"
              samples={readyForSequencingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={canPerformSequencing}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "sequencingPlatform",
                  header: "Sequencing\nPlatform",
                  render: (_value, sample) => sample.sequencingPlatform || "-",
                },
                {
                  key: "libraryConcentration",
                  header: "Library Conc\n(ng/µL)",
                  render: (_value, sample) =>
                    sample.libraryConcentration || "-",
                },
                {
                  key: "chipType",
                  header: "Chip Type",
                  render: (_value, sample) => {
                    if (!sample.chipType) return "-";
                    return sample.chipType.toUpperCase();
                  },
                },
                {
                  key: "bioanalyzerConcentration",
                  header: "Bioanalyzer\nConc (ng/µL)",
                  render: (_value, sample) =>
                    sample.bioanalyzerConcentration || "-",
                },
                {
                  key: "reviewStatus",
                  header: "Review Status",
                  render: (_value, sample) => renderReviewStatus(sample),
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.vl.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Sequencing Completed Section */}
      {sequencingCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.vl.sequencing.completed.title"
                defaultMessage="Sequencing Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {sequencingCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="vl-sequencing-completed"
              samples={sequencingCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                {
                  key: "sequencer",
                  header: "Sequencer",
                  render: (_value, sample) => {
                    if (sample.sequencer === "nextseq-500")
                      return "NextSeq 500";
                    if (sample.sequencer === "nextseq-2000")
                      return "NextSeq 2000";
                    if (sample.sequencer === "dnbseq-g400")
                      return "DNBSEQ G400";
                    return sample.sequencer || "-";
                  },
                },
                {
                  key: "runId",
                  header: "Run ID",
                  render: (_value, sample) => sample.runId || "-",
                },
                {
                  key: "clusterDensity",
                  header: "Cluster Density\n(M)",
                  render: (_value, sample) => sample.clusterDensity || "-",
                },
                {
                  key: "q30Score",
                  header: "Q30 Score\n(%)",
                  render: (_value, sample) => sample.q30Score || "-",
                },
                {
                  key: "sequencingOperator",
                  header: "Sequencing Operator",
                  render: (_value, sample) => sample.sequencingOperator || "-",
                },
                {
                  key: "sequencingDateTime",
                  header: "Sequencing Date",
                  render: (_value, sample) => {
                    const date = sample.sequencingDateTime;
                    if (!date) return "-";
                    return new Date(date).toLocaleDateString();
                  },
                },
                {
                  key: "reviewStatus",
                  header: "Review Status",
                  render: (_value, sample) => renderReviewStatus(sample),
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.vl.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          </div>
        </div>
      )}

      <Modal
        open={sequencingModalOpen}
        onRequestClose={() => setSequencingModalOpen(false)}
        onRequestSubmit={applySequencing}
        modalHeading={intl.formatMessage({
          id: "notebook.vl.sequencing.modal.title",
          defaultMessage: "Record Sequencing",
        })}
        primaryButtonText={
          isApplyingSequencing
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.vl.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingSequencing}
        size="lg"
      >
        {isApplyingSequencing && <Loading withOverlay={false} small />}

        <Grid narrow>
          {/* Pre-Sequencing Section */}
          <Column
            lg={16}
            md={16}
            sm={4}
            style={{
              marginBottom: "1rem",
              fontWeight: "bold",
              fontSize: "0.875rem",
              textTransform: "uppercase",
              color: "#525252",
            }}
          >
            Pre-Sequencing Preparation
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="normalization-notes"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.normalization",
                defaultMessage:
                  "Normalization: Library Concentration Adjustment",
              })}
              value={normalizationNotes}
              onChange={(e) => setNormalizationNotes(e.target.value)}
              rows={2}
              placeholder="Notes on adjusting library concentrations to equal molarity"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="pooling-details"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.pooling",
                defaultMessage: "Pooling: Library Combination Details",
              })}
              value={poolingDetails}
              onChange={(e) => setPoolingDetails(e.target.value)}
              rows={2}
              placeholder="Details on combining multiple libraries (if multiplexed)"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="final-quantity-check"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.quantityCheck",
                defaultMessage: "Final Quantity Check: Pool Concentration",
              })}
              value={finalQuantityCheck}
              onChange={(e) => setFinalQuantityCheck(e.target.value)}
              placeholder="e.g., Pool concentration in nM"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="loading-notes"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.loading",
                defaultMessage:
                  "Loading: Sequencing Instrument Loading Details",
              })}
              value={loadingNotes}
              onChange={(e) => setLoadingNotes(e.target.value)}
              rows={2}
              placeholder="Notes on loading the pool onto the sequencing instrument"
            />
          </Column>

          {/* Sequencing Run Section */}
          <Column
            lg={16}
            md={16}
            sm={4}
            style={{
              marginBottom: "1rem",
              fontWeight: "bold",
              fontSize: "0.875rem",
              textTransform: "uppercase",
              color: "#525252",
              marginTop: "1rem",
            }}
          >
            Sequencing Run Details
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="sequencer"
              titleText={intl.formatMessage({
                id: "notebook.vl.sequencing.sequencer",
                defaultMessage: "Sequencer *",
              })}
              label="Select..."
              items={sequencerOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={sequencer}
              onChange={({ selectedItem }) => setSequencer(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="run-id"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.runId",
                defaultMessage: "Run ID",
              })}
              value={runId}
              onChange={(e) => setRunId(e.target.value)}
              placeholder="e.g., RUN-001"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="read-length"
              label={intl.formatMessage({
                id: "notebook.vl.sequencing.readLength",
                defaultMessage: "Read Length (bp)",
              })}
              value={readLength}
              onChange={(e) =>
                setReadLength(e.imaginaryTarget?.value || e.target?.value || "")
              }
              step={1}
              min={25}
              max={300}
              placeholder="150"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="number-of-cycles"
              label={intl.formatMessage({
                id: "notebook.vl.sequencing.numberOfCycles",
                defaultMessage: "Number of Cycles",
              })}
              value={numberOfCycles}
              onChange={(e) =>
                setNumberOfCycles(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={1}
              min={1}
              max={500}
              placeholder="300"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="flow-cell-type"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.flowCellType",
                defaultMessage: "Flow Cell Type",
              })}
              value={flowCellType}
              onChange={(e) => setFlowCellType(e.target.value)}
              placeholder="e.g., High Output, Mid Output"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="cluster-density"
              label={intl.formatMessage({
                id: "notebook.vl.sequencing.clusterDensity",
                defaultMessage: "Cluster Density (M)",
              })}
              value={clusterDensity}
              onChange={(e) =>
                setClusterDensity(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
              min={0}
              max={2000}
              placeholder="800"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="q30-score"
              label={intl.formatMessage({
                id: "notebook.vl.sequencing.q30Score",
                defaultMessage: "Q30 Score (%)",
              })}
              value={q30Score}
              onChange={(e) =>
                setQ30Score(e.imaginaryTarget?.value || e.target?.value || "")
              }
              step={0.1}
              min={0}
              max={100}
              placeholder="85"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="error-rate"
              label={intl.formatMessage({
                id: "notebook.vl.sequencing.errorRate",
                defaultMessage: "Error Rate (%)",
              })}
              value={errorRate}
              onChange={(e) =>
                setErrorRate(e.imaginaryTarget?.value || e.target?.value || "")
              }
              step={0.01}
              min={0}
              max={10}
              placeholder="0.5"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="operator"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.operator",
                defaultMessage: "Sequencing Operator Name",
              })}
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="Name of person running sequencer"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="sequencing-dateTime"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.dateTime",
                defaultMessage: "Sequencing Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="sequencing-notes"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.notes",
                defaultMessage: "Sequencing Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Any observations about run quality, issues, or additional details"
            />
          </Column>

          {/* ALCOA+ Audit Trail Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.vl.sequencing.auditTrail"
                defaultMessage="ALCOA+ Audit Trail"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="recorded-by"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.recordedBy",
                defaultMessage: "Recorded By",
              })}
              value={recordedBy}
              onChange={(e) => setRecordedBy(e.target.value)}
              placeholder="Name of person recording"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="recorded-date"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.recordedDate",
                defaultMessage: "Recorded Date",
              })}
              value={recordedDate}
              onChange={(e) => setRecordedDate(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="recorded-time"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.recordedTime",
                defaultMessage: "Recorded Time",
              })}
              value={recordedTime}
              onChange={(e) => setRecordedTime(e.target.value)}
              type="time"
              placeholder="HH:MM"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="last-modified-by"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.lastModifiedBy",
                defaultMessage: "Last Modified By",
              })}
              value={lastModifiedBy}
              onChange={(e) => setLastModifiedBy(e.target.value)}
              placeholder="Name of person who last modified"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="last-modified-date"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.lastModifiedDate",
                defaultMessage: "Last Modified Date",
              })}
              value={lastModifiedDate}
              onChange={(e) => setLastModifiedDate(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="last-modified-time"
              labelText={intl.formatMessage({
                id: "notebook.vl.sequencing.lastModifiedTime",
                defaultMessage: "Last Modified Time",
              })}
              value={lastModifiedTime}
              onChange={(e) => setLastModifiedTime(e.target.value)}
              type="time"
              placeholder="HH:MM"
            />
          </Column>

          {/* 3-Tier Review Workflow Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.vl.sequencing.reviewWorkflow"
                defaultMessage="3-Tier Review Workflow"
              />
            </h6>
          </Column>

          {/* Primary Review */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "0.5rem" }}>
            <div>
              <label
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <input
                  type="checkbox"
                  checked={primaryReviewCompleted}
                  onChange={(e) => setPrimaryReviewCompleted(e.target.checked)}
                />
                <span>
                  <FormattedMessage
                    id="notebook.vl.sequencing.primaryReview"
                    defaultMessage="Primary Review Completed"
                  />
                </span>
              </label>
            </div>
          </Column>

          {primaryReviewCompleted && (
            <>
              <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                <TextInput
                  id="primary-reviewer"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.primaryReviewer",
                    defaultMessage: "Primary Reviewer Name",
                  })}
                  value={primaryReviewedBy}
                  onChange={(e) => setPrimaryReviewedBy(e.target.value)}
                  placeholder="Name of reviewer"
                />
              </Column>

              <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                <DatePickerInput
                  id="primary-review-date"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.primaryReviewDate",
                    defaultMessage: "Review Date",
                  })}
                  value={primaryReviewedDate}
                  onChange={(e) => setPrimaryReviewedDate(e.target.value)}
                />
              </Column>

              <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
                <TextInput
                  id="primary-review-time"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.primaryReviewTime",
                    defaultMessage: "Review Time",
                  })}
                  value={primaryReviewedTime}
                  onChange={(e) => setPrimaryReviewedTime(e.target.value)}
                  type="time"
                  placeholder="HH:MM"
                />
              </Column>
            </>
          )}

          {/* Bioinformatics Review */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "0.5rem" }}>
            <div>
              <label
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <input
                  type="checkbox"
                  checked={bioReviewCompleted}
                  onChange={(e) => setBioReviewCompleted(e.target.checked)}
                />
                <span>
                  <FormattedMessage
                    id="notebook.vl.sequencing.bioinformaticsReview"
                    defaultMessage="Bioinformatics Review Completed"
                  />
                </span>
              </label>
            </div>
          </Column>

          {bioReviewCompleted && (
            <>
              <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                <TextInput
                  id="bio-reviewer"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.bioReviewer",
                    defaultMessage: "Bioinformatics Reviewer Name",
                  })}
                  value={bioReviewedBy}
                  onChange={(e) => setBioReviewedBy(e.target.value)}
                  placeholder="Name of reviewer"
                />
              </Column>

              <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                <DatePickerInput
                  id="bio-review-date"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.bioReviewDate",
                    defaultMessage: "Review Date",
                  })}
                  value={bioReviewedDate}
                  onChange={(e) => setBioReviewedDate(e.target.value)}
                />
              </Column>

              <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
                <TextInput
                  id="bio-review-time"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.bioReviewTime",
                    defaultMessage: "Review Time",
                  })}
                  value={bioReviewedTime}
                  onChange={(e) => setBioReviewedTime(e.target.value)}
                  type="time"
                  placeholder="HH:MM"
                />
              </Column>
            </>
          )}

          {/* Final Approval */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "0.5rem" }}>
            <div>
              <label
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <input
                  type="checkbox"
                  checked={finalApprovalCompleted}
                  onChange={(e) => setFinalApprovalCompleted(e.target.checked)}
                />
                <span>
                  <FormattedMessage
                    id="notebook.vl.sequencing.finalApproval"
                    defaultMessage="Final Approval Completed"
                  />
                </span>
              </label>
            </div>
          </Column>

          {finalApprovalCompleted && (
            <>
              <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                <TextInput
                  id="final-approver"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.finalApprover",
                    defaultMessage: "Final Approver Name",
                  })}
                  value={finalApprovedBy}
                  onChange={(e) => setFinalApprovedBy(e.target.value)}
                  placeholder="Name of approver"
                />
              </Column>

              <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                <DatePickerInput
                  id="final-approval-date"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.finalApprovalDate",
                    defaultMessage: "Approval Date",
                  })}
                  value={finalApprovedDate}
                  onChange={(e) => setFinalApprovedDate(e.target.value)}
                />
              </Column>

              <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
                <TextInput
                  id="final-approval-time"
                  labelText={intl.formatMessage({
                    id: "notebook.vl.sequencing.finalApprovalTime",
                    defaultMessage: "Approval Time",
                  })}
                  value={finalApprovedTime}
                  onChange={(e) => setFinalApprovedTime(e.target.value)}
                  type="time"
                  placeholder="HH:MM"
                />
              </Column>
            </>
          )}
        </Grid>
      </Modal>
    </div>
  );
};

export default VLSequencingPage;
