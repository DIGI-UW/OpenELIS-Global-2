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
  Checkbox,
  Loading,
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
 * GBDBioinformaticsAnalysisPage - Page 9: Bioinformatics Analysis & Data Submission
 *
 * Manages bioinformatics analysis tracking and results integration using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Records data transfer, analysis types, and result submission status
 * - Tracks integration of bioinformatics results back to LMIS
 * - Final page in the GBD workflow
 *
 * Data stored in sample.data JSONB:
 * {
 *   bioinformatics: {
 *     fastqTransferred: Boolean,
 *     dataRepository: "NCBI|ENA|DDBJ",
 *     analysisTypes: ["QC", "Alignment", "Variant Calling", "Annotation", "Phylogenetic"],
 *     vcfSubmitted: Boolean,
 *     bamSubmitted: Boolean,
 *     reportsSubmitted: Boolean,
 *     resultsSummary: "Summary statistics and key findings",
 *     reportUrl: "Link to full analysis report",
 *     analyst: "Name of bioinformatician",
 *     dateTime: "2024-01-27",
 *     notes: "Analysis observations"
 *   }
 * }
 */
export const GBDBioinformaticsAnalysisPage = ({
  entryId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const {
    getPagePermissionLevel,
    canSaveData,
    canPerformWork,
    hasFullControl,
    isReadOnly,
    canAccessBioinformatics,
    GBD_ROLES,
    GBD_PAGES,
  } = useGBDPermissions();

  // Page access check
  const canAccessPage = canAccessBioinformatics();

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(GBD_PAGES.BIOINFORMATICS);

  // Function-level permissions per permission matrix
  // Matrix: Lab Technicians (No), Bioinformaticians (Full), Lab Manager (View), Principal Investigator (View), Data Managers (View)
  const canPerformBioinformatics = canPerformWork(pagePermissionLevel); // For Bioinformaticians with Full access
  const canModifyData = canSaveData(pagePermissionLevel);
  const canMarkComplete = canPerformWork(pagePermissionLevel);
  const isViewOnly = isReadOnly(pagePermissionLevel);

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [bioinformaticsModalOpen, setBioinformaticsModalOpen] = useState(false);
  const [isApplyingAnalysis, setIsApplyingAnalysis] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  // Form state for bioinformatics data
  const [fastqTransferred, setFastqTransferred] = useState(false);
  const [dataRepository, setDataRepository] = useState(null);
  const [analysisTypes, setAnalysisTypes] = useState([]);
  const [vcfSubmitted, setVcfSubmitted] = useState(false);
  const [bamSubmitted, setBamSubmitted] = useState(false);
  const [reportsSubmitted, setReportsSubmitted] = useState(false);
  const [resultsSummary, setResultsSummary] = useState("");
  const [reportUrl, setReportUrl] = useState("");
  const [analyst, setAnalyst] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [bioinformaticsTime, setBioinformaticsTime] = useState("09:00");
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

  const dataRepositoryOptions = [
    { id: "ncbi", label: "NCBI SRA" },
    { id: "ena", label: "ENA" },
    { id: "ddbj", label: "DDBJ" },
  ];

  const analysisTypeOptions = [
    { id: "qc", label: "Quality Control (FastQC)" },
    { id: "trimming", label: "Trimming (Trimmomatic)" },
    { id: "alignment", label: "Alignment (BWA/Bowtie2/STAR)" },
    { id: "variant_calling", label: "Variant Calling (GATK/FreeBayes)" },
    { id: "annotation", label: "Annotation (SnpEff/VEP)" },
    { id: "phylogenetic", label: "Phylogenetic Analysis" },
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
                  fastqTransferred: s.data?.bioinformatics?.fastqTransferred,
                  dataRepository: s.data?.bioinformatics?.dataRepository,
                  analysisTypes: s.data?.bioinformatics?.analysisTypes,
                  vcfSubmitted: s.data?.bioinformatics?.vcfSubmitted,
                  bamSubmitted: s.data?.bioinformatics?.bamSubmitted,
                  reportsSubmitted: s.data?.bioinformatics?.reportsSubmitted,
                  resultsSummary: s.data?.bioinformatics?.resultsSummary,
                  reportUrl: s.data?.bioinformatics?.reportUrl,
                  analyst: s.data?.bioinformatics?.analyst,
                  bioinformaticsDateTime: s.data?.bioinformatics?.dateTime,
                  bioinformaticsNotes: s.data?.bioinformatics?.notes,
                  // ALCOA+ Audit Trail
                  recordedBy: s.data?.bioinformatics?.auditTrail?.recordedBy,
                  recordedDate:
                    s.data?.bioinformatics?.auditTrail?.recordedDate,
                  recordedTime:
                    s.data?.bioinformatics?.auditTrail?.recordedTime,
                  lastModifiedBy:
                    s.data?.bioinformatics?.auditTrail?.lastModifiedBy,
                  lastModifiedDate:
                    s.data?.bioinformatics?.auditTrail?.lastModifiedDate,
                  lastModifiedTime:
                    s.data?.bioinformatics?.auditTrail?.lastModifiedTime,
                  // 3-Tier Review Workflow
                  primaryReviewCompleted:
                    s.data?.bioinformatics?.review?.primaryReview?.completed,
                  primaryReviewedBy:
                    s.data?.bioinformatics?.review?.primaryReview?.reviewedBy,
                  bioReviewCompleted:
                    s.data?.bioinformatics?.review?.bioinformaticsReview
                      ?.completed,
                  bioReviewedBy:
                    s.data?.bioinformatics?.review?.bioinformaticsReview
                      ?.reviewedBy,
                  finalApprovalCompleted:
                    s.data?.bioinformatics?.review?.finalApproval?.completed,
                  finalApprovedBy:
                    s.data?.bioinformatics?.review?.finalApproval?.approvedBy,
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
    setFastqTransferred(false);
    setDataRepository(null);
    setAnalysisTypes([]);
    setVcfSubmitted(false);
    setBamSubmitted(false);
    setReportsSubmitted(false);
    setResultsSummary("");
    setReportUrl("");
    setAnalyst("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setBioinformaticsTime("09:00");
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
          id: "notebook.gbd.bioinformatics.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setBioinformaticsModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyAnalysis = useCallback(() => {
    // Validation - at least FASTQ transferred or analysis types selected
    if (!fastqTransferred && analysisTypes.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.bioinformatics.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.bioinformatics.error.dataRequired",
          defaultMessage:
            "Either confirm FASTQ transfer or select analysis types",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.bioinformatics.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingAnalysis(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    // Use the bulk apply endpoint to save bioinformatics data
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          bioinformatics: {
            fastqTransferred: fastqTransferred || false,
            dataRepository: dataRepository?.id || null,
            analysisTypes: analysisTypes.map((a) => a.id),
            vcfSubmitted: vcfSubmitted || false,
            bamSubmitted: bamSubmitted || false,
            reportsSubmitted: reportsSubmitted || false,
            resultsSummary: resultsSummary || null,
            reportUrl: reportUrl || null,
            analyst: analyst || null,
            dateTime: dateTime ? `${dateTime}T${bioinformaticsTime}:00Z` : null,
            notes: notes || null,
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
        setIsApplyingAnalysis(false);
        if (response?.success) {
          // Update sample status to IN_PROGRESS after analysis recording
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
                      id: "notebook.gbd.bioinformatics.success",
                      defaultMessage:
                        "Bioinformatics data recorded for {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setBioinformaticsModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.gbd.bioinformatics.error.statusUpdate",
                    defaultMessage:
                      "Bioinformatics data recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Bioinformatics analysis failed",
          });
        }
      },
    );
  }, [
    fastqTransferred,
    analysisTypes,
    dataRepository,
    vcfSubmitted,
    bamSubmitted,
    reportsSubmitted,
    resultsSummary,
    reportUrl,
    analyst,
    dateTime,
    notes,
    bioinformaticsTime,
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
          id: "notebook.gbd.bioinformatics.noEligibleSamples",
          defaultMessage:
            "Selected samples must have bioinformatics analysis recorded (status: In Progress) before completing.",
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
                id: "notebook.gbd.bioinformatics.completeSuccess",
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
                id: "notebook.gbd.bioinformatics.completeFailed",
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

  const readyForAnalysisSamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "AWAITING" ||
          s.status === "IN_PROGRESS",
      ),
    [samples],
  );

  const analysisCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Bioinformatics Analysis & Data Submission"
        reason="This page requires specific GBD bioinformatics roles to access."
        requiredRoles={[
          GBD_ROLES.BIOINFORMATICIAN,
          GBD_ROLES.MANAGER,
          GBD_ROLES.PRINCIPAL_INVESTIGATOR,
          GBD_ROLES.DATA_MANAGER,
        ]}
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
            id="notebook.gbd.bioinformatics.fullyApproved"
            defaultMessage="Fully Approved"
          />
        </Tag>
      );
    }

    if (bioCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.bioinformatics.bioApproved"
            defaultMessage="Bioinformatics Approved"
          />
        </Tag>
      );
    }

    if (primaryCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.bioinformatics.primaryApproved"
            defaultMessage="Primary Approved"
          />
        </Tag>
      );
    }

    return (
      <Tag type="gray" size="sm">
        <FormattedMessage
          id="notebook.gbd.bioinformatics.reviewPending"
          defaultMessage="Pending Review"
        />
      </Tag>
    );
  };

  const renderAnalysisTypes = (sample) => {
    if (!sample.analysisTypes || sample.analysisTypes.length === 0) {
      return "-";
    }
    return sample.analysisTypes
      .map((type) => {
        const typeOpt = analysisTypeOptions.find((o) => o.id === type);
        return typeOpt ? typeOpt.label.split("(")[0].trim() : type;
      })
      .join(", ");
  };

  return (
    <div className="gbd-bioinformatics-analysis-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.bioinformatics.title"
            defaultMessage="Bioinformatics Analysis & Data Submission"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.bioinformatics.description"
            defaultMessage="Record bioinformatics analysis types, data transfer, and results submission. Track integration of analysis results back to LMIS."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.bioinformatics.ready"
                  defaultMessage="Ready for Analysis"
                />
              </span>
              <span className="progress-value">
                {readyForAnalysisSamples.length}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.bioinformatics.completed"
                  defaultMessage="Analysis Completed"
                />
              </span>
              <span className="progress-value">
                {analysisCompletedSamples.length}
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
            !canPerformBioinformatics ||
            isViewOnly
          }
          title={
            !canPerformBioinformatics
              ? intl.formatMessage({
                  id: "notebook.gbd.bioinformatics.insufficientPermissions.record",
                  defaultMessage: "Insufficient permissions to record bioinformatics analysis. Only Bioinformaticians (with Full access) can record analysis.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.bioinformatics.viewOnlyAccess",
                    defaultMessage: "You have view-only access to this page.",
                  })
                : undefined
          }
        >
          <FormattedMessage
            id="notebook.gbd.recordAnalysis"
            defaultMessage="Record Analysis ({count})"
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
            !canMarkComplete ||
            isViewOnly
          }
          title={
            !canMarkComplete
              ? intl.formatMessage({
                  id: "notebook.gbd.bioinformatics.insufficientPermissions.complete",
                  defaultMessage: "Insufficient permissions to mark samples complete. Only users with work permissions can complete samples.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.bioinformatics.viewOnlyAccess",
                    defaultMessage: "You have view-only access to this page.",
                  })
                : undefined
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
              id="notebook.gbd.bioinformatics.ready.title"
              defaultMessage="Samples Ready for Bioinformatics Analysis"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForAnalysisSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForAnalysisSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.gbd.bioinformatics.ready.empty"
                  defaultMessage="No samples ready for bioinformatics analysis."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="gbd-bioinformatics-ready"
              samples={readyForAnalysisSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={canPerformBioinformatics}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "fastqTransferred",
                  header: "FASTQ\nTransferred",
                  render: (_value, sample) =>
                    sample.fastqTransferred ? (
                      <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
                        Yes
                      </Tag>
                    ) : (
                      <Tag type="gray" size="sm">
                        -
                      </Tag>
                    ),
                },
                {
                  key: "dataRepository",
                  header: "Data\nRepository",
                  render: (_value, sample) => {
                    if (!sample.dataRepository) return "-";
                    return sample.dataRepository.toUpperCase();
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

      {/* Analysis Completed Section */}
      {analysisCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.gbd.bioinformatics.completed.title"
                defaultMessage="Bioinformatics Analysis Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {analysisCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="gbd-bioinformatics-completed"
              samples={analysisCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                {
                  key: "analysisTypes",
                  header: "Analysis Types",
                  render: (_value, sample) => renderAnalysisTypes(sample),
                },
                {
                  key: "vcfSubmitted",
                  header: "VCF",
                  render: (_value, sample) =>
                    sample.vcfSubmitted ? (
                      <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
                        ✓
                      </Tag>
                    ) : (
                      <Tag type="gray" size="sm">
                        -
                      </Tag>
                    ),
                },
                {
                  key: "bamSubmitted",
                  header: "BAM",
                  render: (_value, sample) =>
                    sample.bamSubmitted ? (
                      <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
                        ✓
                      </Tag>
                    ) : (
                      <Tag type="gray" size="sm">
                        -
                      </Tag>
                    ),
                },
                {
                  key: "reportsSubmitted",
                  header: "Reports",
                  render: (_value, sample) =>
                    sample.reportsSubmitted ? (
                      <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
                        ✓
                      </Tag>
                    ) : (
                      <Tag type="gray" size="sm">
                        -
                      </Tag>
                    ),
                },
                {
                  key: "analyst",
                  header: "Analyst",
                  render: (_value, sample) => sample.analyst || "-",
                },
                {
                  key: "bioinformaticsDateTime",
                  header: "Analysis Date",
                  render: (_value, sample) => {
                    const date = sample.bioinformaticsDateTime;
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
        open={bioinformaticsModalOpen}
        onRequestClose={() => setBioinformaticsModalOpen(false)}
        onRequestSubmit={applyAnalysis}
        modalHeading={intl.formatMessage({
          id: "notebook.gbd.bioinformatics.modal.title",
          defaultMessage: "Record Bioinformatics Analysis & Data Submission",
        })}
        primaryButtonText={
          isApplyingAnalysis
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
        primaryButtonDisabled={
          isApplyingAnalysis || !canModifyData || isViewOnly
        }
        size="lg"
      >
        {isApplyingAnalysis && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1.5rem" }}>
            <div
              style={{
                padding: "0.75rem",
                backgroundColor: "#e8f4f8",
                borderRadius: "4px",
              }}
            >
              <p style={{ margin: 0, fontSize: "0.875rem", color: "#333" }}>
                <strong>
                  <FormattedMessage
                    id="notebook.gbd.bioinformatics.instructions"
                    defaultMessage="STAGE 9: Bioinformatics Analysis & Data Submission"
                  />
                </strong>
                <br />
                <FormattedMessage
                  id="notebook.gbd.bioinformatics.description.detailed"
                  defaultMessage="Record bioinformatics analysis status: confirm FASTQ file transfer, select analysis types performed, indicate which results files (VCF, BAM, reports) were generated, and provide links to analysis results and summary statistics for integration back into LMIS."
                />
              </p>
            </div>
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Checkbox
              id="fastq-transferred"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.fastqTransferred",
                defaultMessage:
                  "FASTQ files transferred to bioinformatics server",
              })}
              checked={fastqTransferred}
              onChange={(e) => setFastqTransferred(e.target.checked)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="data-repository"
              titleText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.dataRepository",
                defaultMessage: "Data Repository",
              })}
              label="Select..."
              items={dataRepositoryOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={dataRepository}
              onChange={({ selectedItem }) => setDataRepository(selectedItem)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div style={{ marginBottom: "0.5rem" }}>
              <label
                style={{
                  display: "block",
                  fontSize: "0.875rem",
                  fontWeight: 600,
                  marginBottom: "0.5rem",
                }}
              >
                <FormattedMessage
                  id="notebook.gbd.bioinformatics.analysisTypes"
                  defaultMessage="Analysis Types Performed"
                />
              </label>
              {analysisTypeOptions.map((option) => (
                <div key={option.id} style={{ marginBottom: "0.5rem" }}>
                  <Checkbox
                    id={`analysis-type-${option.id}`}
                    labelText={option.label}
                    checked={analysisTypes.some((a) => a.id === option.id)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setAnalysisTypes([...analysisTypes, option]);
                      } else {
                        setAnalysisTypes(
                          analysisTypes.filter((a) => a.id !== option.id),
                        );
                      }
                    }}
                  />
                </div>
              ))}
            </div>
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div style={{ marginBottom: "0.5rem" }}>
              <label
                style={{
                  display: "block",
                  fontSize: "0.875rem",
                  fontWeight: 600,
                  marginBottom: "0.75rem",
                }}
              >
                <FormattedMessage
                  id="notebook.gbd.bioinformatics.resultsSubmitted"
                  defaultMessage="Results Files Submitted"
                />
              </label>
              <Checkbox
                id="vcf-submitted"
                labelText={intl.formatMessage({
                  id: "notebook.gbd.bioinformatics.vcfSubmitted",
                  defaultMessage: "VCF Files (Variant Calls)",
                })}
                checked={vcfSubmitted}
                onChange={(e) => setVcfSubmitted(e.target.checked)}
                style={{ marginBottom: "0.5rem" }}
              />
              <Checkbox
                id="bam-submitted"
                labelText={intl.formatMessage({
                  id: "notebook.gbd.bioinformatics.bamSubmitted",
                  defaultMessage: "BAM Files (Alignments)",
                })}
                checked={bamSubmitted}
                onChange={(e) => setBamSubmitted(e.target.checked)}
                style={{ marginBottom: "0.5rem" }}
              />
              <Checkbox
                id="reports-submitted"
                labelText={intl.formatMessage({
                  id: "notebook.gbd.bioinformatics.reportsSubmitted",
                  defaultMessage: "Analysis Reports (PDF/HTML)",
                })}
                checked={reportsSubmitted}
                onChange={(e) => setReportsSubmitted(e.target.checked)}
              />
            </div>
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="report-url"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.reportUrl",
                defaultMessage: "Link to Full Analysis Report",
              })}
              value={reportUrl}
              onChange={(e) => setReportUrl(e.target.value)}
              placeholder="https://example.com/analysis/report"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="results-summary"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.resultsSummary",
                defaultMessage: "Results Summary & Key Findings",
              })}
              value={resultsSummary}
              onChange={(e) => setResultsSummary(e.target.value)}
              rows={3}
              placeholder="Summary statistics, significant findings, quality metrics, etc."
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="analyst"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.analyst",
                defaultMessage: "Bioinformatician/Analyst Name",
              })}
              value={analyst}
              onChange={(e) => setAnalyst(e.target.value)}
              placeholder="Name of person performing analysis"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="bioinformatics-dateTime"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.dateTime",
                defaultMessage: "Analysis Completion Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="bioinformatics-notes"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.notes",
                defaultMessage: "Analysis Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Any observations about analysis quality, issues, limitations, or additional details"
            />
          </Column>

          {/* ALCOA+ Audit Trail Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.gbd.bioinformatics.auditTrail"
                defaultMessage="ALCOA+ Audit Trail"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="recorded-by"
              labelText={intl.formatMessage({
                id: "notebook.gbd.bioinformatics.recordedBy",
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
                id: "notebook.gbd.bioinformatics.recordedDate",
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
                id: "notebook.gbd.bioinformatics.recordedTime",
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
                id: "notebook.gbd.bioinformatics.lastModifiedBy",
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
                id: "notebook.gbd.bioinformatics.lastModifiedDate",
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
                id: "notebook.gbd.bioinformatics.lastModifiedTime",
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
                id="notebook.gbd.bioinformatics.reviewWorkflow"
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
                    id="notebook.gbd.bioinformatics.primaryReview"
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
                    id: "notebook.gbd.bioinformatics.primaryReviewer",
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
                    id: "notebook.gbd.bioinformatics.primaryReviewDate",
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
                    id: "notebook.gbd.bioinformatics.primaryReviewTime",
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
                    id="notebook.gbd.bioinformatics.bioinformaticsReview"
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
                    id: "notebook.gbd.bioinformatics.bioReviewer",
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
                    id: "notebook.gbd.bioinformatics.bioReviewDate",
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
                    id: "notebook.gbd.bioinformatics.bioReviewTime",
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
                    id="notebook.gbd.bioinformatics.finalApproval"
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
                    id: "notebook.gbd.bioinformatics.finalApprover",
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
                    id: "notebook.gbd.bioinformatics.finalApprovalDate",
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
                    id: "notebook.gbd.bioinformatics.finalApprovalTime",
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

export default GBDBioinformaticsAnalysisPage;
