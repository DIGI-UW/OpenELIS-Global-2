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
  Checkbox,
} from "@carbon/react";
import { Renew, CheckmarkFilled, Chemistry } from "@carbon/react/icons";
import useGBDPermissions from "../../../../hooks/useGBDPermissions";
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
 * GBDBioanalyzerQCPage - Page 7: Bioanalyzer QC
 *
 * Manages Bioanalyzer quality control analysis using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Records chip type, concentration, size distribution, adapter dimers
 * - Tracks sample progression to Sequencing (Page 8)
 *
 * Data stored in sample.data JSONB:
 * {
 *   bioanalyzerQc: {
 *     chipType: "rna|dna",
 *     concentration: Concentration value,
 *     sizeDistribution: Size distribution description,
 *     adapterDimers: Adapter dimer observations,
 *     operator: "Name of technician",
 *     dateTime: "2024-01-27",
 *     notes: "Bioanalyzer QC observations"
 *   }
 * }
 */
export const GBDBioanalyzerQCPage = ({
  entryId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { getPagePermissionLevel, canSaveData, canAccessBioanalyzerQC } =
    useGBDPermissions();

  // Bioanalyzer QC allowed roles - per GBD permission mapping
  const allowedRoles = [
    "GBD Lab Technician",
    "GBD Manager",
    "GBD Principal Investigator",
  ];

  // Page access check - GBD-specific permission function only
  const canAccessPage = canAccessBioanalyzerQC();

  // Action permission check - what can user do on this page
  const pagePermissionLevel = getPagePermissionLevel("Bioanalyzer QC");
  const canPerformBioanalyzerQC = canSaveData(pagePermissionLevel);

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [bioanalyzerModalOpen, setBioanalyzerModalOpen] = useState(false);
  const [isApplyingBioanalyzer, setIsApplyingBioanalyzer] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  // Form state for bioanalyzer QC data
  const [chipType, setChipType] = useState(null);
  const [concentration, setConcentration] = useState("");
  const [sizeDistribution, setSizeDistribution] = useState("");
  const [adapterDimers, setAdapterDimers] = useState("");
  const [operator, setOperator] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [bioanalyzerTime, setBioanalyzerTime] = useState("09:00");
  const [notes, setNotes] = useState("");
  const [acceptanceCriteriaMet, setAcceptanceCriteriaMet] = useState(false);
  const [acceptanceCriteriaDetails, setAcceptanceCriteriaDetails] =
    useState("");

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

  const chipTypeOptions = [
    { id: "rna", label: "RNA" },
    { id: "dna", label: "DNA" },
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
                  chipType: s.data?.bioanalyzerQc?.chipType,
                  concentration: s.data?.bioanalyzerQc?.concentration,
                  sizeDistribution: s.data?.bioanalyzerQc?.sizeDistribution,
                  adapterDimers: s.data?.bioanalyzerQc?.adapterDimers,
                  bioanalyzerOperator: s.data?.bioanalyzerQc?.operator,
                  bioanalyzerDateTime: s.data?.bioanalyzerQc?.dateTime,
                  bioanalyzerNotes: s.data?.bioanalyzerQc?.notes,
                  // ALCOA+ Audit Trail
                  recordedBy: s.data?.bioanalyzerQc?.auditTrail?.recordedBy,
                  recordedDate: s.data?.bioanalyzerQc?.auditTrail?.recordedDate,
                  recordedTime: s.data?.bioanalyzerQc?.auditTrail?.recordedTime,
                  lastModifiedBy:
                    s.data?.bioanalyzerQc?.auditTrail?.lastModifiedBy,
                  lastModifiedDate:
                    s.data?.bioanalyzerQc?.auditTrail?.lastModifiedDate,
                  lastModifiedTime:
                    s.data?.bioanalyzerQc?.auditTrail?.lastModifiedTime,
                  // 3-Tier Review Workflow
                  primaryReviewCompleted:
                    s.data?.bioanalyzerQc?.review?.primaryReview?.completed,
                  primaryReviewedBy:
                    s.data?.bioanalyzerQc?.review?.primaryReview?.reviewedBy,
                  bioReviewCompleted:
                    s.data?.bioanalyzerQc?.review?.bioinformaticsReview
                      ?.completed,
                  bioReviewedBy:
                    s.data?.bioanalyzerQc?.review?.bioinformaticsReview
                      ?.reviewedBy,
                  finalApprovalCompleted:
                    s.data?.bioanalyzerQc?.review?.finalApproval?.completed,
                  finalApprovedBy:
                    s.data?.bioanalyzerQc?.review?.finalApproval?.approvedBy,
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
    setChipType(null);
    setConcentration("");
    setSizeDistribution("");
    setAdapterDimers("");
    setOperator("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setBioanalyzerTime("09:00");
    setNotes("");
    setAcceptanceCriteriaMet(false);
    setAcceptanceCriteriaDetails("");

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
          id: "notebook.gbd.bioanalyzer.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setBioanalyzerModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyBioanalyzer = useCallback(() => {
    // Validation - chip type is required
    if (!chipType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.bioanalyzer.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.bioanalyzer.error.chipTypeRequired",
          defaultMessage: "Chip type is required",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.bioanalyzer.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingBioanalyzer(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    // Use the bulk apply endpoint to save bioanalyzer QC data
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          bioanalyzerQc: {
            chipType: chipType.id,
            concentration: concentration ? parseFloat(concentration) : null,
            sizeDistribution: sizeDistribution || null,
            adapterDimers: adapterDimers || null,
            operator,
            dateTime: dateTime ? `${dateTime}T${bioanalyzerTime}:00Z` : null,
            notes,
            acceptanceCriteriaMet,
            acceptanceCriteriaDetails: acceptanceCriteriaDetails || null,
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
        setIsApplyingBioanalyzer(false);
        if (response?.success) {
          // Update sample status to IN_PROGRESS after bioanalyzer QC recording
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
                      id: "notebook.gbd.bioanalyzer.success",
                      defaultMessage:
                        "Bioanalyzer QC recorded for {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setBioanalyzerModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.gbd.bioanalyzer.error.statusUpdate",
                    defaultMessage:
                      "Bioanalyzer QC recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Bioanalyzer QC failed",
          });
        }
      },
    );
  }, [
    chipType,
    concentration,
    sizeDistribution,
    adapterDimers,
    operator,
    dateTime,
    bioanalyzerTime,
    notes,
    acceptanceCriteriaMet,
    acceptanceCriteriaDetails,
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

  const handleMarkComplete = useCallback(() => {
    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.bioanalyzer.noEligibleSamples",
          defaultMessage:
            "Selected samples must have Bioanalyzer QC recorded (status: In Progress) before completing.",
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
                id: "notebook.gbd.bioanalyzer.completeSuccess",
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
                id: "notebook.gbd.bioanalyzer.completeFailed",
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

  const readyForBioanalyzerSamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "IN_PROGRESS" ||
          s.status === "AWAITING",
      ),
    [samples],
  );

  const bioanalyzerCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Bioanalyzer QC"
        reason="This page requires specific GBD laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

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
            id="notebook.gbd.bioanalyzer.fullyApproved"
            defaultMessage="Fully Approved"
          />
        </Tag>
      );
    }

    if (bioCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.bioanalyzer.bioApproved"
            defaultMessage="Bioinformatics Approved"
          />
        </Tag>
      );
    }

    if (primaryCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.bioanalyzer.primaryApproved"
            defaultMessage="Primary Approved"
          />
        </Tag>
      );
    }

    return (
      <Tag type="gray" size="sm">
        <FormattedMessage
          id="notebook.gbd.bioanalyzer.reviewPending"
          defaultMessage="Pending Review"
        />
      </Tag>
    );
  };

  return (
    <div className="gbd-bioanalyzer-qc-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.bioanalyzer.title"
            defaultMessage="Bioanalyzer QC"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.bioanalyzer.description"
            defaultMessage="Perform Bioanalyzer quality control analysis. Record chip type, concentration, size distribution, and adapter dimer observations."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.bioanalyzer.ready"
                  defaultMessage="Ready for Bioanalyzer QC"
                />
              </span>
              <span className="progress-value">
                {readyForBioanalyzerSamples.length}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.bioanalyzer.completed"
                  defaultMessage="Bioanalyzer QC Completed"
                />
              </span>
              <span className="progress-value">
                {bioanalyzerCompletedSamples.length}
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
            !canPerformBioanalyzerQC
          }
        >
          <FormattedMessage
            id="notebook.gbd.recordBioanalyzer"
            defaultMessage="Record Bioanalyzer ({count})"
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
            !canPerformBioanalyzerQC
          }
        >
          <FormattedMessage
            id="notebook.gbd.markComplete"
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
            id="notebook.gbd.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.gbd.bioanalyzer.ready.title"
              defaultMessage="Samples Ready for Bioanalyzer QC"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForBioanalyzerSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForBioanalyzerSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.gbd.bioanalyzer.ready.empty"
                  defaultMessage="No samples ready for Bioanalyzer QC."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="gbd-bioanalyzer-ready"
              samples={readyForBioanalyzerSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={canPerformBioanalyzerQC}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "source", header: "Source" },
                {
                  key: "chipType",
                  header: "Chip Type",
                  render: (_value, sample) => {
                    if (sample.chipType === "dna") return "DNA";
                    if (sample.chipType === "rna") return "RNA";
                    return "-";
                  },
                },
                {
                  key: "concentration",
                  header: "Concentration",
                  render: (_value, sample) => sample.concentration || "-",
                },
                {
                  key: "sizeDistribution",
                  header: "Size\nDistribution",
                  render: (_value, sample) => sample.sizeDistribution || "-",
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

      {/* Bioanalyzer QC Completed Section */}
      {bioanalyzerCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.gbd.bioanalyzer.completed.title"
                defaultMessage="Bioanalyzer QC Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {bioanalyzerCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="gbd-bioanalyzer-completed"
              samples={bioanalyzerCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "chipType",
                  header: "Chip Type",
                  render: (_value, sample) => {
                    if (sample.chipType === "dna") return "DNA";
                    if (sample.chipType === "rna") return "RNA";
                    return "-";
                  },
                },
                {
                  key: "concentration",
                  header: "Concentration",
                  render: (_value, sample) => sample.concentration || "-",
                },
                {
                  key: "sizeDistribution",
                  header: "Size\nDistribution",
                  render: (_value, sample) => sample.sizeDistribution || "-",
                },
                {
                  key: "adapterDimers",
                  header: "Adapter\nDimers",
                  render: (_value, sample) => sample.adapterDimers || "-",
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
        open={bioanalyzerModalOpen}
        onRequestClose={() => setBioanalyzerModalOpen(false)}
        onRequestSubmit={applyBioanalyzer}
        modalHeading={intl.formatMessage({
          id: "notebook.gbd.bioanalyzer.modal.title",
          defaultMessage: "Record Bioanalyzer QC",
        })}
        primaryButtonText={
          isApplyingBioanalyzer
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
        primaryButtonDisabled={isApplyingBioanalyzer}
        size="lg"
      >
        {isApplyingBioanalyzer && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="chip-type"
              titleText={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.chipType",
                defaultMessage: "Chip Type *",
              })}
              label="Select..."
              items={chipTypeOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={chipType}
              onChange={({ selectedItem }) => setChipType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="concentration"
              label={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.concentration",
                defaultMessage: "Concentration",
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
              id="size-distribution"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.sizeDistribution",
                defaultMessage: "Size Distribution",
              })}
              value={sizeDistribution}
              onChange={(e) => setSizeDistribution(e.target.value)}
              placeholder="e.g., 200-500 bp"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="adapter-dimers"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.adapterDimers",
                defaultMessage: "Adapter Dimers",
              })}
              value={adapterDimers}
              onChange={(e) => setAdapterDimers(e.target.value)}
              placeholder="e.g., Present, Absent, Faint"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="operator"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.operator",
                defaultMessage: "Bioanalyzer Operator Name",
              })}
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="Name of person performing Bioanalyzer analysis"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="bioanalyzer-dateTime"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.dateTime",
                defaultMessage: "Bioanalyzer Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div
              style={{
                border: "1px solid #ccc",
                padding: "1rem",
                borderRadius: "4px",
                backgroundColor: "#f9f9f9",
              }}
            >
              <h5
                style={{ marginTop: 0, marginBottom: "1rem", color: "#161616" }}
              >
                <FormattedMessage
                  id="notebook.gbd.bioanalyzer.acceptanceCriteria"
                  defaultMessage="QC Acceptance Criteria"
                />
              </h5>

              <div style={{ marginBottom: "1rem" }}>
                <Checkbox
                  id="acceptance-criteria-met"
                  labelText={intl.formatMessage({
                    id: "notebook.gbd.bioanalyzer.criteriaMetLabel",
                    defaultMessage:
                      "Acceptance Criteria Met (Concentration ≥10 ng/µL, RIN ≥7, No excessive degradation)",
                  })}
                  checked={acceptanceCriteriaMet}
                  onChange={(e) => setAcceptanceCriteriaMet(e.target.checked)}
                />
              </div>

              <Column lg={16} md={16} sm={4} style={{ marginBottom: 0 }}>
                <TextArea
                  id="acceptance-criteria-details"
                  labelText={intl.formatMessage({
                    id: "notebook.gbd.bioanalyzer.acceptanceCriteriaDetails",
                    defaultMessage:
                      "QC Criteria Details (e.g., RIN score, concentration values, degradation notes)",
                  })}
                  value={acceptanceCriteriaDetails}
                  onChange={(e) => setAcceptanceCriteriaDetails(e.target.value)}
                  rows={2}
                  placeholder="Document specific QC metrics and assessment results"
                />
              </Column>
            </div>
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="bioanalyzer-notes"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.notes",
                defaultMessage: "Bioanalyzer Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Any observations about sample quality, degradation, or concerns"
            />
          </Column>

          {/* ALCOA+ Audit Trail Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.gbd.bioanalyzer.auditTrail"
                defaultMessage="ALCOA+ Audit Trail"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="recorded-by"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioanalyzer.recordedBy",
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
                id: "notebook.gbd.bioanalyzer.recordedDate",
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
                id: "notebook.gbd.bioanalyzer.recordedTime",
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
                id: "notebook.gbd.bioanalyzer.lastModifiedBy",
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
                id: "notebook.gbd.bioanalyzer.lastModifiedDate",
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
                id: "notebook.gbd.bioanalyzer.lastModifiedTime",
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
                id="notebook.gbd.bioanalyzer.reviewWorkflow"
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
                    id="notebook.gbd.bioanalyzer.primaryReview"
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
                    id: "notebook.gbd.bioanalyzer.primaryReviewer",
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
                    id: "notebook.gbd.bioanalyzer.primaryReviewDate",
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
                    id: "notebook.gbd.bioanalyzer.primaryReviewTime",
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
                    id="notebook.gbd.bioanalyzer.bioinformaticsReview"
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
                    id: "notebook.gbd.bioanalyzer.bioReviewer",
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
                    id: "notebook.gbd.bioanalyzer.bioReviewDate",
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
                    id: "notebook.gbd.bioanalyzer.bioReviewTime",
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
                    id="notebook.gbd.bioanalyzer.finalApproval"
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
                    id: "notebook.gbd.bioanalyzer.finalApprover",
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
                    id: "notebook.gbd.bioanalyzer.finalApprovalDate",
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
                    id: "notebook.gbd.bioanalyzer.finalApprovalTime",
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

export default GBDBioanalyzerQCPage;
