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
  FileUploaderDropContainer,
} from "@carbon/react";
import { Renew, CheckmarkFilled, Chemistry, Image } from "@carbon/react/icons";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import { Permissions } from "../../../../common/Permissions";
import PermissionGate from "../../../common/PermissionGate";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * GBDGelElectrophoresesPage - Page 5: Gel Electrophoresis
 *
 * Manages gel electrophoresis analysis for PCR products using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Analyzes PCR product quality and size verification
 * - Records gel type, product size, quality assessment, and integrity status
 * - Tracks sample progression to Library Preparation (Page 6)
 *
 * Data stored in sample.data JSONB:
 * {
 *   gel: {
 *     gelType: "agarose|polyacrylamide",
 *     productSize: PCR product size in bp,
 *     concentration: DNA concentration,
 *     quality: "excellent|good|acceptable|poor",
 *     integrityStatus: "pass|fail",
 *     operator: "Name of technician",
 *     dateTime: "2024-01-27",
 *     notes: "Gel analysis observations"
 *   }
 * }
 */
export const GBDGelElectrophoresesPage = ({
  entryId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Page-level access control is managed by the parent notebook component

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [gelModalOpen, setGelModalOpen] = useState(false);
  const [isApplyingGel, setIsApplyingGel] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [gelType, setGelType] = useState(null);
  const [productSize, setProductSize] = useState("");
  const [concentration, setConcentration] = useState("");
  const [quality, setQuality] = useState(null);
  const [integrityStatus, setIntegrityStatus] = useState(null);
  const [operator, setOperator] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [gelTime, setGelTime] = useState("09:00");
  const [notes, setNotes] = useState("");
  const [gelImageFileName, setGelImageFileName] = useState("");
  const [gelImageFilePath, setGelImageFilePath] = useState(null);
  const [isUploadingImage, setIsUploadingImage] = useState(false);

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

  const gelTypeOptions = [
    { id: "agarose", label: "Agarose Gel" },
    { id: "polyacrylamide", label: "Polyacrylamide (PAGE)" },
  ];

  const qualityOptions = [
    { id: "excellent", label: "Excellent" },
    { id: "good", label: "Good" },
    { id: "acceptable", label: "Acceptable" },
    { id: "poor", label: "Poor" },
  ];

  const integrityOptions = [
    { id: "pass", label: "Pass" },
    { id: "fail", label: "Fail" },
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
                  expectedBandSize: s.data?.pcr?.expectedBandSize,
                  gelType: s.data?.gel?.gelType,
                  productSize: s.data?.gel?.productSize,
                  concentration: s.data?.gel?.concentration,
                  quality: s.data?.gel?.quality,
                  integrityStatus: s.data?.gel?.integrityStatus,
                  gelOperator: s.data?.gel?.operator,
                  gelDateTime: s.data?.gel?.dateTime,
                  gelNotes: s.data?.gel?.notes,
                  gelImageFileName: s.data?.gel?.imageFileName,
                  gelImageFilePath: s.data?.gel?.imageFilePath,
                  // ALCOA+ Audit Trail
                  recordedBy: s.data?.gel?.auditTrail?.recordedBy,
                  recordedDate: s.data?.gel?.auditTrail?.recordedDate,
                  recordedTime: s.data?.gel?.auditTrail?.recordedTime,
                  lastModifiedBy: s.data?.gel?.auditTrail?.lastModifiedBy,
                  lastModifiedDate: s.data?.gel?.auditTrail?.lastModifiedDate,
                  lastModifiedTime: s.data?.gel?.auditTrail?.lastModifiedTime,
                  // 3-Tier Review Workflow
                  primaryReviewCompleted:
                    s.data?.gel?.review?.primaryReview?.completed,
                  primaryReviewedBy:
                    s.data?.gel?.review?.primaryReview?.reviewedBy,
                  bioReviewCompleted:
                    s.data?.gel?.review?.bioinformaticsReview?.completed,
                  bioReviewedBy:
                    s.data?.gel?.review?.bioinformaticsReview?.reviewedBy,
                  finalApprovalCompleted:
                    s.data?.gel?.review?.finalApproval?.completed,
                  finalApprovedBy:
                    s.data?.gel?.review?.finalApproval?.approvedBy,
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
    setGelType(null);
    setProductSize("");
    setConcentration("");
    setQuality(null);
    setIntegrityStatus(null);
    setOperator("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setGelTime("09:00");
    setNotes("");
    setGelImageFileName("");
    setGelImageFilePath(null);
    setIsUploadingImage(false);

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
          id: "notebook.gbd.gel.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setGelModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const handleGelImageUpload = useCallback(
    (files) => {
      if (!files || files.length === 0) {
        return;
      }

      const file = files[0];
      const fileName = file.name.toLowerCase();
      const allowedExtensions = ["png", "jpg", "jpeg", "tiff", "tif"];
      const fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

      const isValidType =
        (file.type && file.type.startsWith("image/")) ||
        allowedExtensions.includes(fileExtension);

      if (!isValidType) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.gbd.gel.error.invalidFileType",
            defaultMessage: "Invalid File Type",
          }),
          message: intl.formatMessage({
            id: "notebook.gbd.gel.error.imageRequired",
            defaultMessage:
              "Please upload an image file (PNG, JPG, JPEG, TIFF)",
          }),
        });
        return;
      }

      const maxSizeBytes = 50 * 1024 * 1024;
      if (file.size > maxSizeBytes) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.gbd.gel.error.fileTooLarge",
            defaultMessage: "File Too Large",
          }),
          message: intl.formatMessage({
            id: "notebook.gbd.gel.error.fileSizeLimit",
            defaultMessage: "File size must not exceed 50MB",
          }),
        });
        return;
      }

      setGelImageFileName(file.name);

      setIsUploadingImage(true);

      const formData = new FormData();
      formData.append("file", file);

      postToOpenElisServerJsonResponse(
        `/rest/notebook/bulk/page/${pageData.id}/samples/upload-gel-image`,
        formData,
        (response) => {
          setIsUploadingImage(false);

          if (response?.success || response?.filePath) {
            const filePath = response.filePath || response.fileName;
            setGelImageFilePath(filePath);

            notify({
              kind: NotificationKinds.success,
              title: intl.formatMessage({
                id: "notebook.gbd.gel.imageUploaded",
                defaultMessage: "Image Uploaded Successfully",
              }),
              message: intl.formatMessage(
                {
                  id: "notebook.gbd.gel.imageUploadedMessage",
                  defaultMessage:
                    "File '{fileName}' uploaded and ready to be saved",
                },
                { fileName: file.name },
              ),
            });
          } else {
            setGelImageFileName("");
            setGelImageFilePath(null);

            notify({
              kind: NotificationKinds.error,
              title: intl.formatMessage({
                id: "notebook.gbd.gel.error.imageUploadFailed",
                defaultMessage: "Image Upload Failed",
              }),
              message: intl.formatMessage({
                id: "notebook.gbd.gel.error.tryAgain",
                defaultMessage: "Failed to upload image. Please try again.",
              }),
            });
          }
        },
      );
    },
    [intl, notify, pageData?.id],
  );

  const applyGel = useCallback(() => {
    if (!gelType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.gel.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.gel.error.typeRequired",
          defaultMessage: "Gel type is required",
        }),
      });
      return;
    }

    if (!integrityStatus) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.gel.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.gel.error.integrityRequired",
          defaultMessage: "Integrity status (Pass/Fail) is required",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.gel.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingGel(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    const gelData = {
      gelType: gelType.id,
      productSize: productSize ? parseInt(productSize, 10) : null,
      concentration: concentration ? parseFloat(concentration) : null,
      quality: quality ? quality.id : null,
      integrityStatus: integrityStatus.id,
      operator,
      dateTime: dateTime ? `${dateTime}T${gelTime}:00Z` : null,
      notes,
      imageFileName: gelImageFileName || null,
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
          reviewedBy: primaryReviewCompleted ? primaryReviewedBy || null : null,
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
          approvedBy: finalApprovalCompleted ? finalApprovedBy || null : null,
          approvedDate:
            finalApprovalCompleted && finalApprovedDate
              ? `${finalApprovedDate}T${finalApprovedTime}:00Z`
              : null,
        },
      },
    };

    // Add file path if image was already uploaded
    if (gelImageFilePath) {
      gelData.imageFilePath = gelImageFilePath;
    }

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          gel: gelData,
        },
      }),
      (response) => {
        setIsApplyingGel(false);
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
                      id: "notebook.gbd.gel.success",
                      defaultMessage:
                        "Gel electrophoresis recorded for {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setGelModalOpen(false);
                setSelectedSampleIds([]);
                resetForm();
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.gbd.gel.error.statusUpdate",
                    defaultMessage:
                      "Gel recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Gel electrophoresis failed",
          });
        }
      },
    );
  }, [
    gelType,
    productSize,
    concentration,
    quality,
    integrityStatus,
    operator,
    dateTime,
    gelTime,
    notes,
    gelImageFilePath,
    gelImageFileName,
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
          id: "notebook.gbd.gel.noEligibleSamples",
          defaultMessage:
            "Selected samples must have gel electrophoresis recorded (status: In Progress) before completing.",
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
                id: "notebook.gbd.gel.completeSuccess",
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
                id: "notebook.gbd.gel.completeFailed",
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

  const readyForGelSamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "IN_PROGRESS" ||
          s.status === "AWAITING",
      ),
    [samples],
  );

  const gelCompletedSamples = useMemo(
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

  // Helper to render integrity status with color coding
  const renderIntegrityStatus = (sample) => {
    const status = sample.integrityStatus;

    if (!status) {
      return (
        <Tag type="gray" size="sm">
          <FormattedMessage
            id="notebook.gbd.gel.notRecorded"
            defaultMessage="Not Recorded"
          />
        </Tag>
      );
    }

    if (status === "pass") {
      return (
        <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
          <FormattedMessage id="notebook.gbd.gel.pass" defaultMessage="PASS" />
        </Tag>
      );
    }

    return (
      <Tag type="red" size="sm">
        <FormattedMessage id="notebook.gbd.gel.fail" defaultMessage="FAIL" />
      </Tag>
    );
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
            id="notebook.gbd.gel.fullyApproved"
            defaultMessage="Fully Approved"
          />
        </Tag>
      );
    }

    if (bioCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.gel.bioApproved"
            defaultMessage="Bioinformatics Approved"
          />
        </Tag>
      );
    }

    if (primaryCompleted) {
      return (
        <Tag type="blue" size="sm">
          <FormattedMessage
            id="notebook.gbd.gel.primaryApproved"
            defaultMessage="Primary Approved"
          />
        </Tag>
      );
    }

    return (
      <Tag type="gray" size="sm">
        <FormattedMessage
          id="notebook.gbd.gel.reviewPending"
          defaultMessage="Pending Review"
        />
      </Tag>
    );
  };

  return (
    <div className="gbd-gel-electrophoresis-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.gel.title"
            defaultMessage="Gel Electrophoresis"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.gel.description"
            defaultMessage="Perform gel electrophoresis analysis to verify PCR product quality and size. Record gel type, product size, quality assessment, and integrity status."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.gel.ready"
                  defaultMessage="Ready for Gel Analysis"
                />
              </span>
              <span className="progress-value">
                {readyForGelSamples.length}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.gel.analyzed"
                  defaultMessage="Gel Analysis Completed"
                />
              </span>
              <span className="progress-value">
                {gelCompletedSamples.length}
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
              id="notebook.gbd.recordGel"
              defaultMessage="Record Gel ({count})"
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
              id="notebook.gbd.gel.ready.title"
              defaultMessage="Samples Ready for Gel Electrophoresis"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForGelSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForGelSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.gbd.gel.ready.empty"
                  defaultMessage="No samples ready for gel electrophoresis."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="gbd-gel-ready"
              samples={readyForGelSamples}
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
                  key: "expectedBandSize",
                  header: "Expected Band\nSize (bp)",
                  render: (_value, sample) => sample.expectedBandSize || "-",
                },
                {
                  key: "gelType",
                  header: "Gel Type *",
                  render: (_value, sample) => {
                    if (sample.gelType === "polyacrylamide") return "PAGE";
                    if (sample.gelType === "agarose") return "Agarose";
                    return "-";
                  },
                },
                {
                  key: "quality",
                  header: "Quality\nAssessment",
                  render: (_value, sample) => {
                    if (!sample.quality) return "-";
                    return (
                      sample.quality.charAt(0).toUpperCase() +
                      sample.quality.slice(1)
                    );
                  },
                },
                {
                  key: "integrityStatus",
                  header: "Integrity\nStatus *",
                  render: (_value, sample) => renderIntegrityStatus(sample),
                },
                {
                  key: "gelImageFileName",
                  header: "Gel Image",
                  render: (_value, sample) => {
                    if (!sample.gelImageFileName) {
                      return "-";
                    }
                    return (
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                        title={sample.gelImageFileName}
                      >
                        <Image size={16} />
                        <span style={{ fontSize: "0.875rem" }}>
                          {sample.gelImageFileName.length > 20
                            ? sample.gelImageFileName.substring(0, 17) + "..."
                            : sample.gelImageFileName}
                        </span>
                      </div>
                    );
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

      {/* Gel Analysis Completed Section */}
      {gelCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.gbd.gel.completed.title"
                defaultMessage="Gel Electrophoresis Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {gelCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="gbd-gel-completed"
              samples={gelCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "gelType",
                  header: "Gel Type *",
                  render: (_value, sample) => {
                    if (sample.gelType === "polyacrylamide") return "PAGE";
                    if (sample.gelType === "agarose") return "Agarose";
                    return "-";
                  },
                },
                {
                  key: "expectedBandSize",
                  header: "Expected\nSize (bp)",
                  render: (_value, sample) => sample.expectedBandSize || "-",
                },
                {
                  key: "productSize",
                  header: "Observed\nSize (bp)",
                  render: (_value, sample) => sample.productSize || "-",
                },
                {
                  key: "concentration",
                  header: "Concentration\n(ng/µL)",
                  render: (_value, sample) => sample.concentration || "-",
                },
                {
                  key: "quality",
                  header: "Quality\nAssessment",
                  render: (_value, sample) => {
                    if (!sample.quality) return "-";
                    return (
                      sample.quality.charAt(0).toUpperCase() +
                      sample.quality.slice(1)
                    );
                  },
                },
                {
                  key: "integrityStatus",
                  header: "Integrity\nStatus *",
                  render: (_value, sample) => renderIntegrityStatus(sample),
                },
                {
                  key: "gelImageFileName",
                  header: "Gel Image",
                  render: (_value, sample) => {
                    if (!sample.gelImageFileName) {
                      return "-";
                    }
                    return (
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                        title={sample.gelImageFileName}
                      >
                        <Image size={16} />
                        <span style={{ fontSize: "0.875rem" }}>
                          {sample.gelImageFileName.length > 20
                            ? sample.gelImageFileName.substring(0, 17) + "..."
                            : sample.gelImageFileName}
                        </span>
                      </div>
                    );
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
        open={gelModalOpen}
        onRequestClose={() => {
          setGelModalOpen(false);
          resetForm();
        }}
        onRequestSubmit={applyGel}
        modalHeading={intl.formatMessage({
          id: "notebook.gbd.gel.modal.title",
          defaultMessage: "Record Gel Electrophoresis Analysis",
        })}
        primaryButtonText={
          isApplyingGel
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
        primaryButtonDisabled={isApplyingGel}
        size="lg"
      >
        {isApplyingGel && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="gel-type"
              titleText={intl.formatMessage({
                id: "notebook.gbd.gel.type",
                defaultMessage: "Gel Type *",
              })}
              label="Select..."
              items={gelTypeOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={gelType}
              onChange={({ selectedItem }) => setGelType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="product-size"
              label={intl.formatMessage({
                id: "notebook.gbd.gel.productSize",
                defaultMessage: "Observed Product Size (bp)",
              })}
              value={productSize}
              onChange={(e) =>
                setProductSize(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={10}
              min={50}
              max={15000}
              placeholder="500"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="concentration"
              label={intl.formatMessage({
                id: "notebook.gbd.gel.concentration",
                defaultMessage: "Concentration (ng/µL)",
              })}
              value={concentration}
              onChange={(e) =>
                setConcentration(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
              min={0}
              max={5000}
              placeholder="50"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="quality"
              titleText={intl.formatMessage({
                id: "notebook.gbd.gel.quality",
                defaultMessage: "Quality Assessment",
              })}
              label="Select..."
              items={qualityOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={quality}
              onChange={({ selectedItem }) => setQuality(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="integrity-status"
              titleText={intl.formatMessage({
                id: "notebook.gbd.gel.integrityStatus",
                defaultMessage: "Integrity Status *",
              })}
              label="Select..."
              items={integrityOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={integrityStatus}
              onChange={({ selectedItem }) => setIntegrityStatus(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="operator"
              labelText={intl.formatMessage({
                id: "notebook.gbd.gel.operator",
                defaultMessage: "Gel Operator Name",
              })}
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="Name of person performing gel analysis"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="gel-dateTime"
              labelText={intl.formatMessage({
                id: "notebook.gbd.gel.dateTime",
                defaultMessage: "Gel Analysis Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div style={{ marginBottom: "1rem" }}>
              <p style={{ marginBottom: "0.5rem", fontWeight: "bold" }}>
                <FormattedMessage
                  id="notebook.gbd.gel.image"
                  defaultMessage="Gel Image (Optional)"
                />
              </p>
              <FileUploaderDropContainer
                accept={[
                  "image/png",
                  "image/jpeg",
                  "image/jpg",
                  "image/tiff",
                  "image/tif",
                ]}
                onAddFiles={(e) => {
                  handleGelImageUpload(e.addedFiles);
                }}
                multiple={false}
                name="gelImage"
                labelText={intl.formatMessage({
                  id: "notebook.gbd.gel.imageDrop",
                  defaultMessage:
                    "Drag and drop gel image here or click to browse (PNG, JPG, TIFF)",
                })}
              />
              {gelImageFileName && (
                <div
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.875rem",
                    color: "#24a148",
                  }}
                >
                  <strong>
                    <FormattedMessage
                      id="notebook.gbd.gel.imageUploaded"
                      defaultMessage="Uploaded:"
                    />
                  </strong>{" "}
                  {gelImageFileName}
                </div>
              )}
            </div>
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="gel-notes"
              labelText={intl.formatMessage({
                id: "notebook.gbd.gel.notes",
                defaultMessage: "Gel Analysis Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Any observations about band clarity, smearing, contamination, or quality concerns"
            />
          </Column>

          {/* ALCOA+ Audit Trail Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.gbd.gel.auditTrail"
                defaultMessage="ALCOA+ Audit Trail"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="recorded-by"
              labelText={intl.formatMessage({
                id: "notebook.gbd.gel.recordedBy",
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
                id: "notebook.gbd.gel.recordedDate",
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
                id: "notebook.gbd.gel.recordedTime",
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
                id: "notebook.gbd.gel.lastModifiedBy",
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
                id: "notebook.gbd.gel.lastModifiedDate",
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
                id: "notebook.gbd.gel.lastModifiedTime",
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
                id="notebook.gbd.gel.reviewWorkflow"
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
                    id="notebook.gbd.gel.primaryReview"
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
                    id: "notebook.gbd.gel.primaryReviewer",
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
                    id: "notebook.gbd.gel.primaryReviewDate",
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
                    id: "notebook.gbd.gel.primaryReviewTime",
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
                    id="notebook.gbd.gel.bioinformaticsReview"
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
                    id: "notebook.gbd.gel.bioReviewer",
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
                    id: "notebook.gbd.gel.bioReviewDate",
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
                    id: "notebook.gbd.gel.bioReviewTime",
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
                    id="notebook.gbd.gel.finalApproval"
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
                    id: "notebook.gbd.gel.finalApprover",
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
                    id: "notebook.gbd.gel.finalApprovalDate",
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
                    id: "notebook.gbd.gel.finalApprovalTime",
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

export default GBDGelElectrophoresesPage;
