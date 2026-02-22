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
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import { Permissions } from "../../../../constants/roles";
import PermissionGate from "../../../security/PermissionGate";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * GBDLibraryPreparationPage - Page 6: Library Preparation
 *
 * Manages library preparation for sequencing using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Records sequencing platform, library concentration, size distribution
 * - Tracks sample progression to Bioanalyzer QC (Page 7)
 *
 * Data stored in sample.data JSONB:
 * {
 *   libraryPrep: {
 *     platform: "illumina|dnbseq",
 *     concentration: Molar concentration in nM,
 *     sizeDistribution: Library size range description,
 *     operator: "Name of technician",
 *     dateTime: "2024-01-27",
 *     notes: "Library preparation observations"
 *   }
 * }
 */
export const GBDLibraryPreparationPage = ({
  entryId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [libraryModalOpen, setLibraryModalOpen] = useState(false);
  const [isApplyingLibrary, setIsApplyingLibrary] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [platform, setPlatform] = useState(null);
  const [concentration, setConcentration] = useState("");
  const [sizeDistribution, setSizeDistribution] = useState("");
  const [barcode, setBarcode] = useState("");
  const [adapterIndex1, setAdapterIndex1] = useState("");
  const [adapterIndex2, setAdapterIndex2] = useState("");
  const [poolingGroup, setPoolingGroup] = useState("");
  const [operator, setOperator] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [libraryTime, setLibraryTime] = useState("09:00");
  const [notes, setNotes] = useState("");
  const [libraryProtocol, setLibraryProtocol] = useState(null);

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

  const platformOptions = [
    { id: "nextseq-500", label: "Illumina NextSeq 500" },
    { id: "nextseq-2000", label: "Illumina NextSeq 2000" },
    { id: "dnbseq-g400", label: "MGI DNBSEQ G400" },
  ];

  const protocolOptions = [
    { id: "fragmentation", label: "Fragmentation & End-Repair" },
    { id: "adapter-ligation", label: "Adapter Ligation" },
    { id: "size-selection", label: "Size Selection & Cleanup" },
    { id: "amplification", label: "Amplification" },
    { id: "final-qc", label: "Final Library QC" },
  ];

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
                  platform: s.data?.libraryPrep?.platform,
                  concentration: s.data?.libraryPrep?.concentration,
                  sizeDistribution: s.data?.libraryPrep?.sizeDistribution,
                  barcode: s.data?.libraryPrep?.barcode,
                  adapterIndex1: s.data?.libraryPrep?.adapterIndex1,
                  adapterIndex2: s.data?.libraryPrep?.adapterIndex2,
                  poolingGroup: s.data?.libraryPrep?.poolingGroup,
                  libraryOperator: s.data?.libraryPrep?.operator,
                  libraryDateTime: s.data?.libraryPrep?.dateTime,
                  libraryNotes: s.data?.libraryPrep?.notes,
                  libraryProtocol: s.data?.libraryPrep?.protocol,
                  // ALCOA+ Audit Trail
                  recordedBy: s.data?.libraryPrep?.auditTrail?.recordedBy,
                  recordedDate: s.data?.libraryPrep?.auditTrail?.recordedDate,
                  recordedTime: s.data?.libraryPrep?.auditTrail?.recordedTime,
                  lastModifiedBy:
                    s.data?.libraryPrep?.auditTrail?.lastModifiedBy,
                  lastModifiedDate:
                    s.data?.libraryPrep?.auditTrail?.lastModifiedDate,
                  lastModifiedTime:
                    s.data?.libraryPrep?.auditTrail?.lastModifiedTime,
                  // 3-Tier Review Workflow
                  primaryReviewCompleted:
                    s.data?.libraryPrep?.review?.primaryReview?.completed,
                  primaryReviewedBy:
                    s.data?.libraryPrep?.review?.primaryReview?.reviewedBy,
                  bioReviewCompleted:
                    s.data?.libraryPrep?.review?.bioinformaticsReview
                      ?.completed,
                  bioReviewedBy:
                    s.data?.libraryPrep?.review?.bioinformaticsReview
                      ?.reviewedBy,
                  finalApprovalCompleted:
                    s.data?.libraryPrep?.review?.finalApproval?.completed,
                  finalApprovedBy:
                    s.data?.libraryPrep?.review?.finalApproval?.approvedBy,
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
    setPlatform(null);
    setConcentration("");
    setSizeDistribution("");
    setBarcode("");
    setAdapterIndex1("");
    setAdapterIndex2("");
    setPoolingGroup("");
    setOperator("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setLibraryTime("09:00");
    setNotes("");
    setLibraryProtocol(null);

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
          id: "notebook.gbd.library.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setLibraryModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyLibrary = useCallback(() => {
    if (!platform) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.library.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.library.error.platformRequired",
          defaultMessage: "Sequencing platform is required",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.library.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingLibrary(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          libraryPrep: {
            platform: platform.id,
            concentration: concentration ? parseFloat(concentration) : null,
            sizeDistribution: sizeDistribution || null,
            barcode: barcode || null,
            adapterIndex1: adapterIndex1 || null,
            adapterIndex2: adapterIndex2 || null,
            poolingGroup: poolingGroup || null,
            protocol: libraryProtocol?.id || null,
            operator,
            dateTime:
              dateTime && libraryTime ? `${dateTime}T${libraryTime}:00Z` : null,
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
        setIsApplyingLibrary(false);
        if (response?.success) {
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
                      id: "notebook.gbd.library.success",
                      defaultMessage:
                        "Library preparation recorded for {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setLibraryModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.gbd.library.error.statusUpdate",
                    defaultMessage:
                      "Library prep recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Library preparation failed",
          });
        }
      },
    );
  }, [
    platform,
    concentration,
    sizeDistribution,
    barcode,
    adapterIndex1,
    adapterIndex2,
    poolingGroup,
    operator,
    dateTime,
    libraryTime,
    notes,
    libraryProtocol,
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

  // Handle marking library prep samples complete
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected and in library prep (IN_PROGRESS status)
    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.library.noEligibleSamples",
          defaultMessage:
            "Selected samples must have library preparation recorded (status: In Progress) before completing.",
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
                id: "notebook.gbd.library.completeSuccess",
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
                id: "notebook.gbd.library.completeFailed",
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

  const eligibleForCompletionCount = useMemo(
    () =>
      samples.filter(
        (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
      ).length,
    [samples, selectedSampleIds],
  );

  const readyForLibrarySamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "IN_PROGRESS" ||
          s.status === "AWAITING",
      ),
    [samples],
  );

  const libraryCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.gbd.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm">
            <FormattedMessage
              id="notebook.gbd.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.gbd.status.pending"
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
            id="notebook.gbd.library.fullyApproved"
            defaultMessage="Fully Approved"
          />
        </Tag>
      );
    }

    if (bioCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.library.bioApproved"
            defaultMessage="Bioinformatics Approved"
          />
        </Tag>
      );
    }

    if (primaryCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.library.primaryApproved"
            defaultMessage="Primary Approved"
          />
        </Tag>
      );
    }

    return (
      <Tag type="gray" size="sm">
        <FormattedMessage
          id="notebook.gbd.library.reviewPending"
          defaultMessage="Pending Review"
        />
      </Tag>
    );
  };

  return (
    <div className="gbd-library-preparation-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.library.title"
            defaultMessage="Library Preparation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.library.description"
            defaultMessage="Perform library preparation for sequencing. Record sequencing platform, library concentration, and size distribution."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.library.ready"
                  defaultMessage="Ready for Library Prep"
                />
              </span>
              <span className="progress-value">
                {readyForLibrarySamples.length}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.library.completed"
                  defaultMessage="Library Prep Completed"
                />
              </span>
              <span className="progress-value">
                {libraryCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <PermissionGate permission={Permissions.UPDATE_SAMPLES}>
          <Button
            kind="primary"
            size="sm"
            renderIcon={Chemistry}
            onClick={openModal}
            disabled={selectedSampleIds.length === 0 || !hasRealPageId}
          >
            <FormattedMessage
              id="notebook.gbd.recordLibrary"
              defaultMessage="Record Library ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </PermissionGate>

        <PermissionGate permission={Permissions.PROCESS_SAMPLES}>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleMarkComplete}
            disabled={
              eligibleForCompletionCount === 0 || isCompleting || !hasRealPageId
            }
          >
            <FormattedMessage
              id="notebook.gbd.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: eligibleForCompletionCount }}
            />
          </Button>
        </PermissionGate>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.gbd.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.gbd.library.ready.title"
              defaultMessage="Samples Ready for Library Preparation"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForLibrarySamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForLibrarySamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.gbd.library.ready.empty"
                  defaultMessage="No samples ready for library preparation."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="gbd-library-ready"
              samples={readyForLibrarySamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "source", header: "Source" },
                {
                  key: "platform",
                  header: "Platform",
                  render: (_value, sample) => {
                    if (sample.platform === "nextseq-500") return "NextSeq 500";
                    if (sample.platform === "nextseq-2000")
                      return "NextSeq 2000";
                    if (sample.platform === "dnbseq-g400") return "DNBSEQ G400";
                    return "-";
                  },
                },
                {
                  key: "concentration",
                  header: "Concentration\n(nM)",
                  render: (_value, sample) => sample.concentration || "-",
                },
                {
                  key: "sizeDistribution",
                  header: "Size\nDistribution",
                  render: (_value, sample) => sample.sizeDistribution || "-",
                },
                {
                  key: "libraryProtocol",
                  header: "Protocol",
                  render: (_value, sample) => {
                    if (!sample.libraryProtocol) return "-";
                    const protocolLabel = protocolOptions.find(
                      (p) => p.id === sample.libraryProtocol,
                    )?.label;
                    return protocolLabel || sample.libraryProtocol;
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
                    id: "notebook.gbd.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Library Preparation Completed Section */}
      {libraryCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.gbd.library.completed.title"
                defaultMessage="Library Preparation Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {libraryCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="gbd-library-completed"
              samples={libraryCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "platform",
                  header: "Platform",
                  render: (_value, sample) => {
                    if (sample.platform === "nextseq-500") return "NextSeq 500";
                    if (sample.platform === "nextseq-2000")
                      return "NextSeq 2000";
                    if (sample.platform === "dnbseq-g400") return "DNBSEQ G400";
                    return "-";
                  },
                },
                {
                  key: "concentration",
                  header: "Concentration\n(nM)",
                  render: (_value, sample) => sample.concentration || "-",
                },
                {
                  key: "sizeDistribution",
                  header: "Size\nDistribution",
                  render: (_value, sample) => sample.sizeDistribution || "-",
                },
                {
                  key: "libraryProtocol",
                  header: "Protocol",
                  render: (_value, sample) => {
                    if (!sample.libraryProtocol) return "-";
                    const protocolLabel = protocolOptions.find(
                      (p) => p.id === sample.libraryProtocol,
                    )?.label;
                    return protocolLabel || sample.libraryProtocol;
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
                    id: "notebook.gbd.column.status",
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
        open={libraryModalOpen}
        onRequestClose={() => setLibraryModalOpen(false)}
        onRequestSubmit={applyLibrary}
        modalHeading={intl.formatMessage({
          id: "notebook.gbd.library.modal.title",
          defaultMessage: "Record Library Preparation",
        })}
        primaryButtonText={
          isApplyingLibrary
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.gbd.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingLibrary}
        size="lg"
      >
        {isApplyingLibrary && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="platform"
              titleText={intl.formatMessage({
                id: "notebook.gbd.library.platform",
                defaultMessage: "Sequencing Platform *",
              })}
              label="Select..."
              items={platformOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={platform}
              onChange={({ selectedItem }) => setPlatform(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="libraryProtocol"
              titleText={intl.formatMessage({
                id: "notebook.gbd.library.protocol",
                defaultMessage: "Library Prep Protocol",
              })}
              label="Select..."
              items={protocolOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={libraryProtocol}
              onChange={({ selectedItem }) => setLibraryProtocol(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="concentration"
              label={intl.formatMessage({
                id: "notebook.gbd.library.concentration",
                defaultMessage: "Library Concentration (nM)",
              })}
              value={concentration}
              onChange={(e) =>
                setConcentration(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
              min={0}
              max={10000}
              placeholder="50"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="sizeDistribution"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.sizeDistribution",
                defaultMessage: "Size Distribution",
              })}
              value={sizeDistribution}
              onChange={(e) => setSizeDistribution(e.target.value)}
              placeholder="e.g., 200-500 bp, 300-700 bp"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="barcode"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.barcode",
                defaultMessage: "Barcode / Index",
              })}
              value={barcode}
              onChange={(e) => setBarcode(e.target.value)}
              placeholder="e.g., P1-D1, BC001"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="adapterIndex1"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.adapterIndex1",
                defaultMessage: "Adapter Index 1",
              })}
              value={adapterIndex1}
              onChange={(e) => setAdapterIndex1(e.target.value)}
              placeholder="e.g., AACGCTAT"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="adapterIndex2"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.adapterIndex2",
                defaultMessage: "Adapter Index 2",
              })}
              value={adapterIndex2}
              onChange={(e) => setAdapterIndex2(e.target.value)}
              placeholder="e.g., TTCGCAAT"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="poolingGroup"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.poolingGroup",
                defaultMessage: "Pooling Group ID",
              })}
              value={poolingGroup}
              onChange={(e) => setPoolingGroup(e.target.value)}
              placeholder="e.g., POOL-2024-001"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="operator"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.operator",
                defaultMessage: "Library Prep Operator Name",
              })}
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="Name of person performing library prep"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="library-dateTime"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.dateTime",
                defaultMessage: "Library Prep Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="library-notes"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.notes",
                defaultMessage: "Library Prep Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Any observations about library quality, yield, or concerns"
            />
          </Column>

          {/* ALCOA+ Audit Trail Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.gbd.library.auditTrail"
                defaultMessage="ALCOA+ Audit Trail"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="recorded-by"
              labelText={intl.formatMessage({
                id: "notebook.gbd.library.recordedBy",
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
                id: "notebook.gbd.library.recordedDate",
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
                id: "notebook.gbd.library.recordedTime",
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
                id: "notebook.gbd.library.lastModifiedBy",
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
                id: "notebook.gbd.library.lastModifiedDate",
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
                id: "notebook.gbd.library.lastModifiedTime",
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
                id="notebook.gbd.library.reviewWorkflow"
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
                    id="notebook.gbd.library.primaryReview"
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
                    id: "notebook.gbd.library.primaryReviewer",
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
                    id: "notebook.gbd.library.primaryReviewDate",
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
                    id: "notebook.gbd.library.primaryReviewTime",
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
                    id="notebook.gbd.library.bioinformaticsReview"
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
                    id: "notebook.gbd.library.bioReviewer",
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
                    id: "notebook.gbd.library.bioReviewDate",
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
                    id: "notebook.gbd.library.bioReviewTime",
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
                    id="notebook.gbd.library.finalApproval"
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
                    id: "notebook.gbd.library.finalApprover",
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
                    id: "notebook.gbd.library.finalApprovalDate",
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
                    id: "notebook.gbd.library.finalApprovalTime",
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

export default GBDLibraryPreparationPage;
