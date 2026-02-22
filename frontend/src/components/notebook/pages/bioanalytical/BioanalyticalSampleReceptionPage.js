import React, { useState, useCallback, useContext, useEffect } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Checkbox,
  TextArea,
  NumberInput,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import BioanalyticalManifestImportModal from "../../modals/BioanalyticalManifestImportModal";
import BulkApplyForm from "../../workflow/BulkApplyForm";
import SampleGrid from "../../workflow/SampleGrid";
import { Upload, Checkmark, Edit, Chemistry } from "@carbon/react/icons";
import { postToOpenElisServer } from "../../../utils/Utils";
import config from "../../../../config.json";
import { usePermissions } from "../../../../hooks/usePermissions";
import { Permissions } from "../../../../constants/roles";
import PermissionGate from "../../../security/PermissionGate";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalSampleReceptionPage - STAGE 1 of bioanalytical workflow.
 *
 * Following established OpenELIS laboratory workflow patterns:
 * - Import samples via manifest CSV
 * - Display samples in SampleGrid with checkboxes for bulk selection
 * - Action buttons for bulk operations (Edit Metadata, Mark Complete)
 * - Two-section layout: Pending samples (with selection) + Completed samples (read-only)
 * - Progress tracking tiles showing counts and status
 *
 * STAGE 1 Requirements:
 * ● Receive processed biological samples from Medical Laboratory at clinical site
 * ● Receive pharmaceutical samples directly from researchers or external clients
 * ● Register metadata: sample type, requested tests, storage condition, source laboratory/client
 * ● Link to project or bioequivalence study
 * ● Mark samples as verified to proceed to Test Assignment & Preparation
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after sample changes
 */
function BioanalyticalSampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { hasAnyRole } = usePermissions();

  // Core state following established patterns
  const [isLoading, setIsLoading] = useState(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Modal states
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [isBulkApplyModalOpen, setIsBulkApplyModalOpen] = useState(false);

  // QC Verification modal state
  const [isQCVerificationModalOpen, setIsQCVerificationModalOpen] =
    useState(false);
  const [qcVerificationData, setQcVerificationData] = useState({
    volumeAdequate: false,
    containerIntegrity: false,
    temperatureOK: false,
    labelingComplete: false,
    chainOfCustody: false,
    comments: "",
    measuredVolume: "",
    measuredTemperature: "",
  });

  // Control Sample Classification state
  const [controlSampleData, setControlSampleData] = useState({
    sampleClassification: {
      type: "STUDY_SAMPLE", // STUDY_SAMPLE, POSITIVE_CONTROL, NEGATIVE_CONTROL, QC_SAMPLE, BLANK
      isControlSample: false,
      controlType: "", // "POSITIVE", "NEGATIVE", "BLANK", "QC_LOW", "QC_MEDIUM", "QC_HIGH"
      controlCategory: "", // "SYSTEM_SUITABILITY", "METHOD_VALIDATION", "RUN_ACCEPTANCE", "MATRIX_EFFECT"
      expectedResult: "",
      controlSource: "", // "REFERENCE_STANDARD", "SPIKED_MATRIX", "BLANK_MATRIX"
      batchInfo: {
        controlBatch: "",
        expiryDate: "",
        storageCondition: "",
        supplier: "",
      },
    },
  });

  // Progress tracking
  // Include both PENDING and IN_PROGRESS statuses as "pending" for the workflow
  // IN_PROGRESS means samples have been opened/started but not yet completed
  const pendingSamples = samples.filter(
    (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
  );
  const completedSamples = samples.filter((s) => s.status === "COMPLETED");
  const pendingCount = pendingSamples.length;
  const completedCount = completedSamples.length;

  // Check if we have a real page ID (not a default placeholder)
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Notification helper following established patterns
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // Modal handlers following established patterns
  const handleImportModalOpen = () => {
    setIsImportModalOpen(true);
  };

  const handleImportModalClose = () => {
    setIsImportModalOpen(false);
  };

  const handleImportSuccess = useCallback(
    (results) => {
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.reception.success",
          defaultMessage: "Success",
        }),
        message: intl.formatMessage(
          {
            id: "notebook.bioanalytical.reception.importSuccess",
            defaultMessage:
              "{count} samples imported successfully for Stage 1 reception",
          },
          { count: results.totalCreated || 0 },
        ),
      });

      // Refresh sample list and close modal
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
      setIsImportModalOpen(false);
    },
    [intl, onProgressUpdate, notify],
  );

  // Bulk operation: Mark samples as verified/completed following established patterns
  const markAsVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.reception.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    // Use established bulk status update pattern
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notebook.bioanalytical.reception.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage(
              {
                id: "notebook.bioanalytical.reception.success.verified",
                defaultMessage:
                  "Marked {count} sample(s) as verified. They are ready for Test Assignment & Preparation.",
              },
              { count: selectedSampleIds.length },
            ),
          });
          setSelectedSampleIds([]); // Clear selection
          loadPageSamples(); // Refresh list
          if (onProgressUpdate) {
            onProgressUpdate(); // Notify parent
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.bioanalytical.reception.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "notebook.bioanalytical.reception.error.status",
              defaultMessage: "Failed to verify samples. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    notify,
    pageData?.id,
    onProgressUpdate,
  ]);

  // Handle QC Verification modal open
  const handleQCVerificationModalOpen = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: "No Samples Selected",
        message: "Please select samples to perform QC verification.",
      });
      return;
    }

    // Reset form data
    setQcVerificationData({
      volumeAdequate: false,
      containerIntegrity: false,
      temperatureOK: false,
      labelingComplete: false,
      chainOfCustody: false,
      comments: "",
      measuredVolume: "",
      measuredTemperature: "",
    });

    // Reset control sample data
    setControlSampleData({
      sampleClassification: {
        type: "STUDY_SAMPLE",
        isControlSample: false,
        controlType: "",
        controlCategory: "",
        expectedResult: "",
        controlSource: "",
        batchInfo: {
          controlBatch: "",
          expiryDate: "",
          storageCondition: "",
          supplier: "",
        },
      },
    });

    setIsQCVerificationModalOpen(true);
  }, [selectedSampleIds.length, addNotification]);

  // Handle QC Verification submission
  const handleQCVerificationSubmit = useCallback(async () => {
    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");

    if (!hasRealPageId) {
      addNotification({
        kind: NotificationKinds.error,
        title: "Error",
        message: "Cannot perform QC verification without a valid page ID.",
      });
      return;
    }

    // Calculate QC status
    const qcChecks = [
      qcVerificationData.volumeAdequate,
      qcVerificationData.containerIntegrity,
      qcVerificationData.temperatureOK,
      qcVerificationData.labelingComplete,
      qcVerificationData.chainOfCustody,
    ];

    const passedChecks = qcChecks.filter(Boolean).length;
    const totalChecks = qcChecks.length;
    const qcPassed = qcChecks.every(Boolean);

    // Prepare QC data for storage in sample.data (using the same pattern as BulkApplyForm)
    // Explicitly extract only primitive values to avoid circular references
    const qcData = {
      receptionQC: {
        volumeAdequate: Boolean(qcVerificationData.volumeAdequate),
        containerIntegrity: Boolean(qcVerificationData.containerIntegrity),
        temperatureOK: Boolean(qcVerificationData.temperatureOK),
        labelingComplete: Boolean(qcVerificationData.labelingComplete),
        chainOfCustody: Boolean(qcVerificationData.chainOfCustody),
        comments: String(qcVerificationData.comments || ""),
        measuredVolume: String(qcVerificationData.measuredVolume || ""),
        measuredTemperature: String(
          qcVerificationData.measuredTemperature || "",
        ),
        qcPerformed: true,
        qcDate: new Date().toISOString(),
        passedChecks,
        totalChecks,
        qcPassed,
        overallStatus: qcPassed ? "PASS" : "FAIL",
      },
      // Control Sample Classification
      sampleClassification: {
        type: String(controlSampleData.sampleClassification.type),
        isControlSample: Boolean(
          controlSampleData.sampleClassification.isControlSample,
        ),
        controlType: String(
          controlSampleData.sampleClassification.controlType || "",
        ),
        controlCategory: String(
          controlSampleData.sampleClassification.controlCategory || "",
        ),
        expectedResult: String(
          controlSampleData.sampleClassification.expectedResult || "",
        ),
        controlSource: String(
          controlSampleData.sampleClassification.controlSource || "",
        ),
        batchInfo: {
          controlBatch: String(
            controlSampleData.sampleClassification.batchInfo.controlBatch || "",
          ),
          expiryDate: String(
            controlSampleData.sampleClassification.batchInfo.expiryDate || "",
          ),
          storageCondition: String(
            controlSampleData.sampleClassification.batchInfo.storageCondition ||
              "",
          ),
          supplier: String(
            controlSampleData.sampleClassification.batchInfo.supplier || "",
          ),
        },
        classificationDate: new Date().toISOString(),
        classificationBy: "current_user", // In real implementation, get from user context
      },
    };

    setIsLoading(true);

    // Use the same bulk apply endpoint as Edit Metadata modal
    const requestBody = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: qcData,
    };

    console.log("QC Verification - requestBody:", requestBody);
    console.log("QC Verification - qcData:", qcData);
    console.log("QC Verification - qcVerificationData:", qcVerificationData);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(requestBody),
      (status, response) => {
        setIsLoading(false);
        if (status === 200) {
          const passedSamples = qcPassed ? selectedSampleIds.length : 0;
          const failedSamples = selectedSampleIds.length - passedSamples;

          addNotification({
            kind: qcPassed
              ? NotificationKinds.success
              : NotificationKinds.warning,
            title: "QC Verification Complete",
            message: `${selectedSampleIds.length} sample(s) processed. ${passedSamples} passed, ${failedSamples} failed QC.`,
          });

          setIsQCVerificationModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();

          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: "Error",
            message: "Failed to save QC verification data. Please try again.",
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    qcVerificationData,
    addNotification,
    onProgressUpdate,
  ]);

  // Render QC Status for sample grid
  const renderQCStatus = useCallback((value, sample) => {
    // Handle both value-first and sample-first parameter patterns
    const sampleData = sample || value;

    console.log("renderQCStatus - value:", value);
    console.log("renderQCStatus - sample:", sample);
    console.log("renderQCStatus - sampleData:", sampleData);
    console.log("renderQCStatus - sampleData.data:", sampleData?.data);
    console.log(
      "renderQCStatus - sampleData.receptionQC:",
      sampleData?.receptionQC,
    );
    console.log(
      "renderQCStatus - sampleData.data.receptionQC:",
      sampleData?.data?.receptionQC,
    );

    // Safety check for undefined sample
    if (!sampleData || typeof sampleData !== "object") {
      console.log(
        "renderQCStatus - returning QC Pending due to invalid sampleData",
      );
      return (
        <Tag type="gray" size="sm" title="QC not yet performed">
          QC Pending
        </Tag>
      );
    }

    // QC data is available directly on the sample object after transformation
    // (loadPageSamples spreads ...sampleDataFields which includes receptionQC)
    const qc = sampleData.receptionQC;

    console.log("renderQCStatus - qc:", qc);

    if (!qc || !qc.qcPerformed) {
      console.log(
        "renderQCStatus - returning QC Pending due to no QC data or qcPerformed=false",
      );
      return (
        <Tag type="gray" size="sm" title="QC not yet performed">
          QC Pending
        </Tag>
      );
    }

    const { qcPassed, passedChecks, totalChecks } = qc;

    if (qcPassed) {
      return (
        <Tag
          type="green"
          size="sm"
          title={`QC passed all ${totalChecks} checks on ${new Date(qc.qcDate).toLocaleString()}`}
        >
          QC PASS ({passedChecks}/{totalChecks})
        </Tag>
      );
    } else {
      return (
        <Tag
          type="red"
          size="sm"
          title={`QC failed - ${passedChecks}/${totalChecks} checks passed on ${new Date(qc.qcDate).toLocaleString()}`}
        >
          QC FAIL ({passedChecks}/{totalChecks})
        </Tag>
      );
    }
  }, []);

  // Render Control Sample Type for sample grid
  const renderControlType = useCallback((value, sample) => {
    // Handle both value-first and sample-first parameter patterns
    const sampleData = sample || value;

    // Safety check for undefined sample
    if (!sampleData || typeof sampleData !== "object") {
      return (
        <Tag type="gray" size="sm" title="No classification">
          Study Sample
        </Tag>
      );
    }

    // Check for control classification data
    const classification = sampleData.sampleClassification;

    if (!classification || !classification.isControlSample) {
      return (
        <Tag type="gray" size="sm" title="Regular study sample">
          Study Sample
        </Tag>
      );
    }

    // Determine tag type and text based on control type
    const getControlTypeDisplay = () => {
      switch (classification.type) {
        case "POSITIVE_CONTROL":
          return {
            type: "green",
            text: "Positive Ctrl",
            title: `Positive Control - ${classification.controlType || "Generic"}`,
          };
        case "NEGATIVE_CONTROL":
          return {
            type: "red",
            text: "Negative Ctrl",
            title: `Negative Control - ${classification.controlType || "Generic"}`,
          };
        case "QC_SAMPLE":
          return {
            type: "blue",
            text: "QC Sample",
            title: `QC Sample - ${classification.controlType || "Generic"}`,
          };
        case "BLANK":
          return {
            type: "purple",
            text: "Blank",
            title: `Blank Control - ${classification.controlType || "Generic"}`,
          };
        default:
          return {
            type: "cyan",
            text: "Control",
            title: `Control Sample - ${classification.type}`,
          };
      }
    };

    const { type, text, title } = getControlTypeDisplay();

    return (
      <Tag type={type} size="sm" title={title}>
        {text}
      </Tag>
    );
  }, []);

  // Load Stage 1 samples from backend API
  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setIsLoading(false);
      setSamples([]);
      return;
    }

    setIsLoading(true);

    // Load samples for this bioanalytical workflow page
    fetch(`${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`, {
      method: "GET",
      credentials: "include",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Content-Type": "application/json",
      },
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Failed to load samples: HTTP ${response.status}`);
        }
        return response.json();
      })
      .then((data) => {
        console.debug(
          "Loaded samples for bioanalytical page:",
          pageData.id,
          "Count:",
          Array.isArray(data) ? data.length : 0,
          "Data:",
          data,
        );

        if (Array.isArray(data)) {
          const transformedSamples = data.map((sample) => {
            // Combine base sample properties with all data JSONB fields
            // This ensures any bulk-applied values are captured
            const sampleDataFields = sample.data || {};
            const sampleStatus =
              sample.pageStatus || sample.status || "PENDING";

            console.debug(
              "Sample transform debug:",
              sample.externalId,
              "pageStatus:",
              sample.pageStatus,
              "status:",
              sample.status,
              "final status:",
              sampleStatus,
            );

            return {
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              status: sampleStatus,

              // Map bioanalytical-specific fields to SampleGrid default columns
              // to avoid duplicates while providing the data
              sourceFacility: sampleDataFields.sourceOrigin, // Maps to "Source" column
              receivedDate: sampleDataFields.dateTimeOfReceipt, // Maps to "Received Date" column
              // collectionDate is intentionally NOT mapped (will show as "-")

              // Stage 1 bioanalytical-specific metadata from JSONB data
              // Spread all JSONB data fields to capture bulk-applied values
              ...sampleDataFields,

              // Ensure these core fields have fallbacks
              uniqueSampleId:
                sampleDataFields.uniqueSampleId || sample.externalId,
              projectStudyAssociation: sampleDataFields.projectStudyAssociation,
              storageConditionPrior: sampleDataFields.storageConditionPrior,
              manifestVerificationStatus:
                sampleDataFields.manifestVerificationStatus,
              timepoint: sampleDataFields.timepoint,
              sourceOrigin: sampleDataFields.sourceOrigin,
              requestedTests: sampleDataFields.requestedTests,
              dateTimeOfReceipt: sampleDataFields.dateTimeOfReceipt,
              receivingPersonnel: sampleDataFields.receivingPersonnel,
              sampleVolume: sampleDataFields.sampleVolume,
              transportTemperature: sampleDataFields.transportTemperature,
              subjectId: sampleDataFields.subjectId,
              notes: sampleDataFields.notes,
            };
          });
          console.debug(
            "Transformed samples:",
            transformedSamples.length,
            transformedSamples,
          );
          setSamples(transformedSamples);
        } else {
          console.warn("Response data is not an array:", data);
          setSamples([]);
        }
        setIsLoading(false);
      })
      .catch((error) => {
        console.error("Failed to load Stage 1 samples:", error);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.reception.error",
            defaultMessage: "Error",
          }),
          message: intl.formatMessage({
            id: "notebook.bioanalytical.stage1.error.loadSamples",
            defaultMessage: "Failed to load samples. Please refresh the page.",
          }),
        });
        setSamples([]);
        setIsLoading(false);
      });
  }, [pageData?.id, intl, notify]);

  // Load samples when component mounts or page changes
  useEffect(() => {
    loadPageSamples();
  }, [loadPageSamples]);

  // Page-level access control is handled by usePageAccessControl() in parent workflow component
  // This component assumes it's only rendered when user has page access
  // Individual UI elements use PermissionGate for action-level control

  return (
    <div className="bioanalytical-page">
      {/* Stage 1 Header - Following established patterns */}
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.stage1.title"
            defaultMessage="STAGE 1: Sample Reception & Registration"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.stage1.description"
            defaultMessage="Import samples via manifest CSV, review and verify metadata, then mark as complete to proceed to Test Assignment & Preparation."
          />
        </p>
      </div>

      {/* Progress Tiles Following Established Pattern */}
      <Grid
        fullWidth
        className="progress-section"
        style={{ marginBottom: "1.5rem" }}
      >
        <Column lg={16} md={8} sm={4}>
          <div
            className="progress-tiles"
            style={{ display: "flex", gap: "1rem" }}
          >
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.awaitingVerification"
                  defaultMessage="Awaiting Verification"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.biological"
                  defaultMessage="Biological"
                />
              </span>
              <span className="progress-value">
                {
                  samples.filter((s) =>
                    ["Plasma", "Serum", "Urine", "Whole Blood"].includes(
                      s.sampleType,
                    ),
                  ).length
                }
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.pharmaceutical"
                  defaultMessage="Pharmaceutical"
                />
              </span>
              <span className="progress-value">
                {
                  samples.filter((s) =>
                    ["API", "Tablet", "Capsule", "Suspension"].includes(
                      s.sampleType,
                    ),
                  ).length
                }
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Bar Following Established Pattern */}
      <Grid fullWidth style={{ marginBottom: "1.5rem" }}>
        <Column lg={16} md={8} sm={4}>
          <div
            className="page-actions-bar"
            style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}
          >
            <PermissionGate
              roles={[
                Permissions.SAMPLE_RECEIVER,
                Permissions.CHEMICAL_ANALYST,
                Permissions.PHARMACIST,
                Permissions.RESEARCHER,
                Permissions.LAB_SUPERVISOR,
              ]}
              disabledTooltip={intl.formatMessage({
                id: "notebook.bioanalytical.stage1.insufficientPermissions",
                defaultMessage: "Insufficient permissions to import samples",
              })}
            >
              <Button
                kind="primary"
                size="sm"
                renderIcon={Upload}
                onClick={handleImportModalOpen}
              >
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.importManifest"
                  defaultMessage="Import from Manifest"
                />
              </Button>
            </PermissionGate>

            {/* Conditional buttons that appear when samples are selected */}
            {selectedSampleIds.length > 0 && (
              <>
                <PermissionGate
                  roles={[
                    Permissions.CHEMICAL_ANALYST,
                    Permissions.PHARMACIST,
                    Permissions.RESEARCHER,
                    Permissions.LAB_SUPERVISOR,
                  ]}
                  disabledTooltip={intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.insufficientPermissionsEdit",
                    defaultMessage: "Insufficient permissions to edit metadata",
                  })}
                >
                  <Button
                    kind="primary"
                    size="sm"
                    renderIcon={Edit}
                    onClick={() => setIsBulkApplyModalOpen(true)}
                    disabled={selectedSampleIds.length === 0}
                  >
                    Edit Metadata ({selectedSampleIds.length})
                  </Button>
                </PermissionGate>

                {/* QC Verification Button */}
                <PermissionGate
                  roles={[
                    Permissions.CHEMICAL_ANALYST,
                    Permissions.PHARMACIST,
                    Permissions.RESEARCHER,
                    Permissions.LAB_SUPERVISOR,
                  ]}
                  disabledTooltip={
                    selectedSampleIds.length === 0
                      ? "Select samples to perform QC verification"
                      : intl.formatMessage({
                          id: "notebook.bioanalytical.stage1.insufficientPermissionsQC",
                          defaultMessage:
                            "Insufficient permissions for QC verification",
                        })
                  }
                >
                  <Button
                    kind="secondary"
                    size="sm"
                    renderIcon={Chemistry}
                    onClick={handleQCVerificationModalOpen}
                    disabled={selectedSampleIds.length === 0}
                    title={`Perform QC verification on ${selectedSampleIds.length} selected sample(s)`}
                  >
                    <FormattedMessage
                      id="notebook.bioanalytical.stage1.qcVerification"
                      defaultMessage="QC Verification ({count})"
                      values={{ count: selectedSampleIds.length }}
                    />
                  </Button>
                </PermissionGate>

                <PermissionGate
                  roles={[
                    Permissions.CHEMICAL_ANALYST,
                    Permissions.PHARMACIST,
                    Permissions.RESEARCHER,
                    Permissions.LAB_SUPERVISOR,
                  ]}
                  disabledTooltip={intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.insufficientPermissionsVerify",
                    defaultMessage:
                      "Insufficient permissions to verify samples",
                  })}
                >
                  <Button
                    kind="primary"
                    size="sm"
                    renderIcon={Checkmark}
                    onClick={markAsVerified}
                    disabled={selectedSampleIds.length === 0}
                  >
                    Mark as Verified ({selectedSampleIds.length})
                  </Button>
                </PermissionGate>
              </>
            )}
          </div>
        </Column>
      </Grid>

      {/* Two-Section Layout Following Established Pattern */}

      {/* Section 1: Pending Samples (with checkboxes and selection) */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              marginBottom: "0.5rem",
            }}
          >
            <FormattedMessage
              id="notebook.bioanalytical.stage1.pending.title"
              defaultMessage="Pending Verification"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
            <FormattedMessage
              id="notebook.bioanalytical.stage1.pending.description"
              defaultMessage="Select samples to edit metadata or mark as verified. Verified samples will proceed to Test Assignment & Preparation."
            />
          </p>
        </div>

        <div className="sample-grid-container">
          {!isLoading && pendingSamples.length === 0 ? (
            <div
              className="empty-table-state"
              style={{ textAlign: "center", padding: "2rem", color: "#525252" }}
            >
              <p>
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.pending.empty"
                  defaultMessage="No pending samples. Import a CSV manifest to add samples for verification."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-bioanalytical-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={isLoading}
              columns={[
                {
                  key: "externalId",
                  header: intl.formatMessage({
                    id: "notebook.sample.externalId",
                    defaultMessage: "External ID",
                  }),
                },
                {
                  key: "accessionNumber",
                  header: intl.formatMessage({
                    id: "notebook.sample.accessionNumber",
                    defaultMessage: "Accession #",
                  }),
                },
                {
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.sample.type",
                    defaultMessage: "Sample Type",
                  }),
                },
                {
                  key: "sampleCategory",
                  header: intl.formatMessage({
                    id: "notebook.sample.category",
                    defaultMessage: "Category",
                  }),
                },
                {
                  key: "sourceFacility",
                  header: intl.formatMessage({
                    id: "notebook.sample.sourceFacility",
                    defaultMessage: "Source",
                  }),
                },
                // Collection Date intentionally removed (not used in bioanalytical workflow)
                {
                  key: "receivedDate",
                  header: intl.formatMessage({
                    id: "notebook.sample.receivedDate",
                    defaultMessage: "Received Date",
                  }),
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.sample.status",
                    defaultMessage: "Status",
                  }),
                  render: (value, sample) => {
                    const status = sample?.status || value || "PENDING";
                    return (
                      <Tag
                        size="sm"
                        type={
                          status === "COMPLETED"
                            ? "green"
                            : status === "IN_PROGRESS"
                              ? "blue"
                              : "red"
                        }
                      >
                        {status}
                      </Tag>
                    );
                  },
                },
                {
                  key: "qcStatus",
                  header: "QC Status",
                  render: renderQCStatus,
                  width: "120px",
                  sortable: false,
                },
                {
                  key: "controlType",
                  header: "Control Type",
                  render: renderControlType,
                  width: "140px",
                  sortable: false,
                },
              ]}
              additionalColumns={[
                {
                  key: "pending-accessionNumber",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.accessionNumber",
                    defaultMessage: "Accession Number",
                  }),
                  render: (value, sample) => {
                    const accession = sample?.accessionNumber || value;
                    return accession ? (
                      <div
                        style={{
                          fontSize: "0.875rem",
                          fontFamily: "monospace",
                        }}
                      >
                        {accession}
                      </div>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "pending-uniqueSampleId",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.uniqueSampleId",
                    defaultMessage: "Sample ID",
                  }),
                  render: (value, sample) => {
                    const sampleId =
                      sample?.uniqueSampleId || sample?.externalId || value;
                    return sampleId ? (
                      <div
                        style={{
                          fontSize: "0.875rem",
                          fontFamily: "monospace",
                        }}
                      >
                        {sampleId}
                      </div>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "pending-projectStudyAssociation",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.projectStudyAssociation",
                    defaultMessage: "Project/Study",
                  }),
                  render: (value, sample) => {
                    const project = sample?.projectStudyAssociation || value;
                    return project ? (
                      <div style={{ fontSize: "0.875rem" }}>🔗 {project}</div>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "pending-storageConditionPrior",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.storageCondition",
                    defaultMessage: "Storage Condition",
                  }),
                  render: (value, sample) => {
                    const condition = sample?.storageConditionPrior || value;
                    if (!condition) return "-";

                    // Extract temperature info for quick visualization
                    const tempMatch = condition.match(/\((.*?)\)/);
                    const displayText = tempMatch ? tempMatch[1] : condition;
                    return (
                      <Tag
                        size="sm"
                        type={
                          condition.toLowerCase().includes("frozen")
                            ? "purple"
                            : condition.toLowerCase().includes("refrigerat")
                              ? "cyan"
                              : "gray"
                        }
                      >
                        {displayText}
                      </Tag>
                    );
                  },
                },
                {
                  key: "pending-manifestVerificationStatus",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.verificationStatus",
                    defaultMessage: "Verification Status",
                  }),
                  render: (value, sample) => {
                    const status =
                      sample?.manifestVerificationStatus || value || "Pending";
                    return (
                      <Tag
                        size="sm"
                        type={
                          status === "Verified"
                            ? "green"
                            : status === "Discrepancy"
                              ? "red"
                              : "gray"
                        }
                      >
                        {status}
                      </Tag>
                    );
                  },
                },
                {
                  key: "pending-timepoint",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.timepoint",
                    defaultMessage: "Timepoint",
                  }),
                  render: (value, sample) => {
                    const timepoint = sample?.timepoint || value;
                    if (!timepoint) return "-";

                    // Color code timepoints for quick visualization
                    const getTimepointColor = (tp) => {
                      const lowerTp = tp?.toLowerCase() || "";
                      if (lowerTp.includes("pre")) return "purple";
                      if (lowerTp.includes("1h")) return "blue";
                      if (lowerTp.includes("2h")) return "cyan";
                      if (lowerTp.includes("4h")) return "teal";
                      if (lowerTp.includes("8h")) return "blue";
                      if (lowerTp.includes("24h")) return "green";
                      return "gray";
                    };

                    return (
                      <Tag size="sm" type={getTimepointColor(timepoint)}>
                        {timepoint}
                      </Tag>
                    );
                  },
                },
                {
                  key: "pending-requestedTests",
                  header: intl.formatMessage({
                    id: "notebook.bioanalytical.stage1.column.requestedTests",
                    defaultMessage: "Requested Tests",
                  }),
                  render: (value, sample) => {
                    const tests = sample?.requestedTests || value;
                    if (!tests) return "-";

                    // Handle both string (single test) and array (multiple tests) formats
                    const testArray = Array.isArray(tests)
                      ? tests
                      : typeof tests === "string"
                        ? tests.split(",").map((t) => t.trim())
                        : [tests];

                    // Define distinct colors for different test types
                    const getTestColor = (test) => {
                      const lowerTest = test?.toLowerCase() || "";
                      if (lowerTest.includes("bioavail")) return "blue";
                      if (
                        lowerTest.includes("phar") ||
                        lowerTest.includes("pk")
                      )
                        return "cyan";
                      if (lowerTest.includes("stability")) return "purple";
                      if (lowerTest.includes("impurity")) return "teal";
                      if (lowerTest.includes("assay")) return "green";
                      return "gray";
                    };

                    return (
                      <div style={{ fontSize: "0.875rem" }}>
                        {testArray.slice(0, 2).map((test, index) => (
                          <Tag
                            key={index}
                            size="sm"
                            type={getTestColor(test)}
                            style={{
                              marginRight: "0.25rem",
                              marginBottom: "0.125rem",
                            }}
                          >
                            {test}
                          </Tag>
                        ))}
                        {testArray.length > 2 && (
                          <Tag
                            size="sm"
                            type="gray"
                            style={{
                              marginRight: "0.25rem",
                              marginBottom: "0.125rem",
                            }}
                          >
                            +{testArray.length - 2}
                          </Tag>
                        )}
                      </div>
                    );
                  },
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Section 2: Verified Samples (read-only, no checkboxes) */}
      {completedSamples.length > 0 && (
        <div className="sample-table-section" style={{ marginTop: "2rem" }}>
          <div className="table-section-header">
            <h5
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                marginBottom: "0.5rem",
              }}
            >
              <FormattedMessage
                id="notebook.bioanalytical.stage1.verified.title"
                defaultMessage="Verified Samples"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedCount}
              </Tag>
            </h5>
            <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.bioanalytical.stage1.verified.description"
                defaultMessage="Samples verified and ready for Test Assignment & Preparation."
              />
            </p>
          </div>

          <SampleGrid
            gridId="verified-bioanalytical-samples"
            samples={completedSamples}
            showSelection={false}
            loading={isLoading}
            columns={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.sample.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.sample.accessionNumber",
                  defaultMessage: "Accession #",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.sample.type",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "sampleCategory",
                header: intl.formatMessage({
                  id: "notebook.sample.category",
                  defaultMessage: "Category",
                }),
              },
              {
                key: "sourceFacility",
                header: intl.formatMessage({
                  id: "notebook.sample.sourceFacility",
                  defaultMessage: "Source",
                }),
              },
              // Collection Date intentionally removed (not used in bioanalytical workflow)
              {
                key: "receivedDate",
                header: intl.formatMessage({
                  id: "notebook.sample.receivedDate",
                  defaultMessage: "Received Date",
                }),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.sample.status",
                  defaultMessage: "Status",
                }),
                render: (value, sample) => {
                  const status = sample?.status || value || "PENDING";
                  return (
                    <Tag
                      size="sm"
                      type={
                        status === "COMPLETED"
                          ? "green"
                          : status === "IN_PROGRESS"
                            ? "blue"
                            : "red"
                      }
                    >
                      {status}
                    </Tag>
                  );
                },
              },
            ]}
            additionalColumns={[
              {
                key: "verified-accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
                render: (value, sample) => {
                  const accession = sample?.accessionNumber || value;
                  return accession ? (
                    <div
                      style={{ fontSize: "0.875rem", fontFamily: "monospace" }}
                    >
                      {accession}
                    </div>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-uniqueSampleId",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.uniqueSampleId",
                  defaultMessage: "Sample ID",
                }),
                render: (value, sample) => {
                  const sampleId =
                    sample?.uniqueSampleId || sample?.externalId || value;
                  return sampleId ? (
                    <div
                      style={{ fontSize: "0.875rem", fontFamily: "monospace" }}
                    >
                      {sampleId}
                    </div>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-sampleType",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.sampleType",
                  defaultMessage: "Sample Type",
                }),
                render: (value, sample) => {
                  const sampleType = sample?.sampleType || value;
                  return sampleType ? (
                    <Tag
                      type={
                        // Color coding: Blue for biological samples, Green for pharmaceutical samples
                        [
                          "Plasma",
                          "Serum",
                          "Urine",
                          "Whole Blood",
                          "Saliva",
                          "CSF",
                        ].includes(sampleType)
                          ? "blue"
                          : "green"
                      }
                      size="sm"
                    >
                      {sampleType}
                    </Tag>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-projectStudyAssociation",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.projectStudyAssociation",
                  defaultMessage: "Project/Study",
                }),
                render: (value, sample) => {
                  const project = sample?.projectStudyAssociation || value;
                  return project ? (
                    <div style={{ fontSize: "0.875rem" }}>🔗 {project}</div>
                  ) : (
                    "-"
                  );
                },
              },
              {
                key: "verified-requestedTests",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.requestedTests",
                  defaultMessage: "Requested Tests",
                }),
                render: (value, sample) => {
                  const tests = sample?.requestedTests || value;
                  if (!tests) return "-";

                  // Handle both string (single test) and array (multiple tests) formats
                  const testArray = Array.isArray(tests)
                    ? tests
                    : typeof tests === "string"
                      ? tests.split(",").map((t) => t.trim())
                      : [tests];

                  // Define distinct colors for different test types
                  const getTestColor = (test) => {
                    const lowerTest = test?.toLowerCase() || "";
                    if (lowerTest.includes("bioavail")) return "blue";
                    if (lowerTest.includes("phar") || lowerTest.includes("pk"))
                      return "cyan";
                    if (lowerTest.includes("stability")) return "purple";
                    if (lowerTest.includes("impurity")) return "teal";
                    if (lowerTest.includes("assay")) return "green";
                    return "gray";
                  };

                  return (
                    <div style={{ fontSize: "0.875rem" }}>
                      {testArray.slice(0, 2).map((test, index) => (
                        <Tag
                          key={index}
                          size="sm"
                          type={getTestColor(test)}
                          style={{
                            marginRight: "0.25rem",
                            marginBottom: "0.125rem",
                          }}
                        >
                          {test}
                        </Tag>
                      ))}
                      {testArray.length > 2 && (
                        <Tag
                          size="sm"
                          type="gray"
                          style={{
                            marginRight: "0.25rem",
                            marginBottom: "0.125rem",
                          }}
                        >
                          +{testArray.length - 2}
                        </Tag>
                      )}
                    </div>
                  );
                },
              },
              {
                key: "verified-timepoint",
                header: intl.formatMessage({
                  id: "notebook.bioanalytical.stage1.column.timepoint",
                  defaultMessage: "Timepoint",
                }),
                render: (value, sample) => {
                  const timepoint = sample?.timepoint || value;
                  if (!timepoint) return "-";

                  // Color code timepoints for quick visualization
                  const getTimepointColor = (tp) => {
                    const lowerTp = tp?.toLowerCase() || "";
                    if (lowerTp.includes("pre")) return "purple";
                    if (lowerTp.includes("1h")) return "blue";
                    if (lowerTp.includes("2h")) return "cyan";
                    if (lowerTp.includes("4h")) return "teal";
                    if (lowerTp.includes("8h")) return "blue";
                    if (lowerTp.includes("24h")) return "green";
                    return "gray";
                  };

                  return (
                    <Tag size="sm" type={getTimepointColor(timepoint)}>
                      {timepoint}
                    </Tag>
                  );
                },
              },
            ]}
          />
        </div>
      )}

      {/* Modals Following Established Patterns */}

      {/* Import Modal */}
      <BioanalyticalManifestImportModal
        open={isImportModalOpen}
        onClose={handleImportModalClose}
        entryId={entryId}
        onSuccess={handleImportSuccess}
      />

      {/* Bulk Apply Metadata Modal */}
      {isBulkApplyModalOpen && hasRealPageId && (
        <BulkApplyForm
          open={isBulkApplyModalOpen}
          onClose={() => setIsBulkApplyModalOpen(false)}
          pageId={pageData.id}
          selectedSampleIds={selectedSampleIds}
          formFields={[
            // Core Sample Information (Stage 1 Requirements)
            {
              key: "sampleType",
              label: "Sample Type",
              type: "dropdown",
              required: true,
              options: [
                // Biological Samples (from Medical Laboratory)
                { id: "Plasma", text: "Plasma (Biological)" },
                { id: "Serum", text: "Serum (Biological)" },
                { id: "Urine", text: "Urine (Biological)" },
                { id: "Whole Blood", text: "Whole Blood (Biological)" },
                { id: "Saliva", text: "Saliva (Biological)" },
                { id: "CSF", text: "Cerebrospinal Fluid (Biological)" },
                // Pharmaceutical Samples (from Researchers/Clients)
                { id: "API", text: "API - Active Pharmaceutical Ingredient" },
                { id: "Tablet", text: "Tablet (Solid Dosage)" },
                { id: "Capsule", text: "Capsule (Solid Dosage)" },
                { id: "Suspension", text: "Suspension (Liquid Dosage)" },
                { id: "Solution", text: "Solution (Liquid Dosage)" },
                { id: "Emulsion", text: "Emulsion (Liquid Dosage)" },
                { id: "Cream", text: "Cream (Topical)" },
                { id: "Ointment", text: "Ointment (Topical)" },
              ],
            },
            {
              key: "requestedTests",
              label: "Requested Test(s)",
              type: "text",
              required: true,
              placeholder:
                "e.g., LC-MS/MS, HPLC, Dissolution (comma-separated for multiple tests)",
            },
            {
              key: "sourceOrigin",
              label: "Source Origin/Laboratory",
              type: "dropdown",
              required: true,
              options: [
                {
                  id: "AHRI Medical Lab",
                  text: "AHRI Medical Laboratory (Clinical Site)",
                },
                {
                  id: "External Medical Lab",
                  text: "External Medical Laboratory",
                },
                {
                  id: "Research Institution",
                  text: "Research Institution/University",
                },
                {
                  id: "Pharmaceutical Company",
                  text: "Pharmaceutical Company",
                },
                { id: "CRO", text: "Contract Research Organization (CRO)" },
                { id: "Government Lab", text: "Government Laboratory" },
                { id: "Private Clinic", text: "Private Clinical Site" },
                { id: "Other", text: "Other (Specify in Notes)" },
              ],
            },
            {
              key: "storageConditionPrior",
              label: "Storage Condition Prior to Testing",
              type: "dropdown",
              required: true,
              options: [
                {
                  id: "Room Temperature (15-25°C)",
                  text: "Room Temperature (15-25°C)",
                },
                { id: "Refrigerated (2-8°C)", text: "Refrigerated (2-8°C)" },
                { id: "Frozen (-20°C)", text: "Frozen (-20°C)" },
                { id: "Ultra-frozen (-80°C)", text: "Ultra-frozen (-80°C)" },
                {
                  id: "Controlled Room Temp",
                  text: "Controlled Room Temperature",
                },
                { id: "Do not freeze", text: "Do not freeze" },
                { id: "Unknown", text: "Unknown" },
              ],
            },

            // Study/Project Linkage
            {
              key: "projectStudyAssociation",
              label: "Project/Study/Protocol ID",
              type: "text",
              placeholder: "Link to bioequivalence study or research project",
            },

            // Sample Details
            {
              key: "sampleVolume",
              label: "Sample Volume/Quantity",
              type: "text",
              placeholder: "e.g., 2.5 mL, 10 tablets, 5g",
            },
            {
              key: "subjectId",
              label: "Subject/Patient ID",
              type: "text",
              placeholder: "Anonymous subject identifier",
            },
            {
              key: "timepoint",
              label: "Collection Timepoint",
              type: "dropdown",
              options: [
                { id: "Pre-dose", text: "Pre-dose" },
                { id: "1h", text: "1h post-dose" },
                { id: "2h", text: "2h post-dose" },
                { id: "4h", text: "4h post-dose" },
                { id: "8h", text: "8h post-dose" },
                { id: "24h", text: "24h post-dose" },
                { id: "Other", text: "Other (specify in notes)" },
              ],
            },

            // Administrative
            {
              key: "receivingPersonnel",
              label: "Receiving Personnel",
              type: "text",
              placeholder: "Staff member who received the sample",
            },
            {
              key: "manifestVerificationStatus",
              label: "Manifest Verification Status",
              type: "dropdown",
              options: [
                { id: "Verified", text: "Verified - All information matches" },
                { id: "Pending", text: "Pending - Awaiting verification" },
                { id: "Discrepancy", text: "Discrepancy - Issues identified" },
              ],
            },
            {
              key: "notes",
              label: "Notes/Comments",
              type: "text",
              placeholder:
                "Special handling instructions, observations, or additional details",
            },
          ]}
          onApplySuccess={(response) => {
            notify({
              kind: NotificationKinds.success,
              title: intl.formatMessage({
                id: "notebook.bioanalytical.reception.success",
                defaultMessage: "Success",
              }),
              message: intl.formatMessage(
                {
                  id: "notebook.bioanalytical.reception.bulkApplySuccess",
                  defaultMessage:
                    "Metadata applied to {count} sample(s) successfully.",
                },
                { count: selectedSampleIds.length },
              ),
            });
            setSelectedSampleIds([]); // Clear selection

            // Refresh data with a small delay to ensure modal closes and state updates
            setTimeout(() => {
              loadPageSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            }, 300);
          }}
        />
      )}

      {/* QC Verification Modal */}
      <Modal
        open={isQCVerificationModalOpen}
        onRequestClose={() => setIsQCVerificationModalOpen(false)}
        modalLabel="Sample Reception"
        modalHeading={`QC Verification for ${selectedSampleIds.length} Sample(s)`}
        primaryButtonText="Complete QC Verification"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleQCVerificationSubmit}
        size="md"
        preventCloseOnClickOutside={isLoading}
      >
        <div className="qc-verification-form">
          <p style={{ marginBottom: "1rem", color: "#6f6f6f" }}>
            Complete quality control verification for the selected samples. All
            criteria must pass for QC approval.
          </p>

          {/* QC Checklist */}
          <div style={{ marginBottom: "1.5rem" }}>
            <h5
              style={{
                marginBottom: "1rem",
                fontSize: "1rem",
                fontWeight: "500",
              }}
            >
              Reception QC Criteria:
            </h5>

            <div style={{ display: "grid", gap: "0.75rem" }}>
              <Checkbox
                id="qc-volume"
                labelText="Volume Adequate (≥2.0mL for bioanalysis)"
                checked={qcVerificationData.volumeAdequate}
                onChange={(checked, { name, id }) =>
                  setQcVerificationData((prev) => ({
                    ...prev,
                    volumeAdequate: checked,
                  }))
                }
                disabled={isLoading}
              />

              <Checkbox
                id="qc-container"
                labelText="Container integrity acceptable (no cracks, leaks, contamination)"
                checked={qcVerificationData.containerIntegrity}
                onChange={(checked, { name, id }) =>
                  setQcVerificationData((prev) => ({
                    ...prev,
                    containerIntegrity: checked,
                  }))
                }
                disabled={isLoading}
              />

              <Checkbox
                id="qc-temperature"
                labelText="Temperature maintained during transport (2-8°C)"
                checked={qcVerificationData.temperatureOK}
                onChange={(checked, { name, id }) =>
                  setQcVerificationData((prev) => ({
                    ...prev,
                    temperatureOK: checked,
                  }))
                }
                disabled={isLoading}
              />

              <Checkbox
                id="qc-labeling"
                labelText="Sample labeling complete and legible"
                checked={qcVerificationData.labelingComplete}
                onChange={(checked, { name, id }) =>
                  setQcVerificationData((prev) => ({
                    ...prev,
                    labelingComplete: checked,
                  }))
                }
                disabled={isLoading}
              />

              <Checkbox
                id="qc-custody"
                labelText="Chain of custody documentation complete"
                checked={qcVerificationData.chainOfCustody}
                onChange={(checked, { name, id }) =>
                  setQcVerificationData((prev) => ({
                    ...prev,
                    chainOfCustody: checked,
                  }))
                }
                disabled={isLoading}
              />
            </div>
          </div>

          {/* Optional Measurements */}
          <div style={{ marginBottom: "1.5rem" }}>
            <h6
              style={{
                marginBottom: "0.75rem",
                fontSize: "0.875rem",
                color: "#6f6f6f",
              }}
            >
              Optional Measurements:
            </h6>

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "1rem",
              }}
            >
              <NumberInput
                id="measured-volume"
                label="Measured Volume (mL)"
                placeholder="e.g., 2.5"
                value={qcVerificationData.measuredVolume}
                onChange={(e, { value }) =>
                  setQcVerificationData((prev) => ({
                    ...prev,
                    measuredVolume: value,
                  }))
                }
                min={0}
                step={0.1}
                disabled={isLoading}
              />

              <NumberInput
                id="measured-temperature"
                label="Measured Temperature (°C)"
                placeholder="e.g., 4.2"
                value={qcVerificationData.measuredTemperature}
                onChange={(e, { value }) =>
                  setQcVerificationData((prev) => ({
                    ...prev,
                    measuredTemperature: value,
                  }))
                }
                min={-20}
                max={50}
                step={0.1}
                disabled={isLoading}
              />
            </div>
          </div>

          {/* Comments */}
          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="qc-comments"
              labelText="QC Comments"
              placeholder="Enter any QC observations, deviations, or corrective actions taken..."
              value={qcVerificationData.comments}
              onChange={(e) =>
                setQcVerificationData((prev) => ({
                  ...prev,
                  comments: e.target.value,
                }))
              }
              disabled={isLoading}
              rows={3}
            />
          </div>

          {/* Control Sample Classification */}
          <div
            style={{
              marginBottom: "1.5rem",
              borderTop: "1px solid #e0e0e0",
              paddingTop: "1.5rem",
            }}
          >
            <h5
              style={{
                marginBottom: "1rem",
                fontSize: "1rem",
                fontWeight: "500",
              }}
            >
              Control Sample Classification:
            </h5>

            {/* Sample Type Selection */}
            <div style={{ marginBottom: "1rem" }}>
              <Select
                id="sample-type"
                labelText="Sample Type"
                value={controlSampleData.sampleClassification.type}
                onChange={(e) => {
                  const newType = e.target.value;
                  const isControlSample = newType !== "STUDY_SAMPLE";
                  setControlSampleData((prev) => ({
                    ...prev,
                    sampleClassification: {
                      ...prev.sampleClassification,
                      type: newType,
                      isControlSample: isControlSample,
                      controlType: isControlSample
                        ? prev.sampleClassification.controlType || ""
                        : "",
                      controlCategory: isControlSample
                        ? prev.sampleClassification.controlCategory || ""
                        : "",
                      expectedResult: isControlSample
                        ? prev.sampleClassification.expectedResult || ""
                        : "",
                      controlSource: isControlSample
                        ? prev.sampleClassification.controlSource || ""
                        : "",
                    },
                  }));
                }}
                disabled={isLoading}
              >
                <SelectItem value="STUDY_SAMPLE" text="Study Sample" />
                <SelectItem value="POSITIVE_CONTROL" text="Positive Control" />
                <SelectItem value="NEGATIVE_CONTROL" text="Negative Control" />
                <SelectItem value="QC_SAMPLE" text="QC Sample" />
                <SelectItem value="BLANK" text="Blank" />
              </Select>
            </div>

            {/* Control Sample Details - Only show if control sample is selected */}
            {controlSampleData.sampleClassification.isControlSample && (
              <>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    gap: "1rem",
                    marginBottom: "1rem",
                  }}
                >
                  <Select
                    id="control-type"
                    labelText="Control Type"
                    value={controlSampleData.sampleClassification.controlType}
                    onChange={(e) =>
                      setControlSampleData((prev) => ({
                        ...prev,
                        sampleClassification: {
                          ...prev.sampleClassification,
                          controlType: e.target.value,
                        },
                      }))
                    }
                    disabled={isLoading}
                  >
                    <SelectItem value="" text="Select control type..." />
                    <SelectItem value="POSITIVE" text="Positive Control" />
                    <SelectItem value="NEGATIVE" text="Negative Control" />
                    <SelectItem value="BLANK" text="Blank Control" />
                    <SelectItem value="QC_LOW" text="QC Low" />
                    <SelectItem value="QC_MEDIUM" text="QC Medium" />
                    <SelectItem value="QC_HIGH" text="QC High" />
                  </Select>

                  <Select
                    id="control-category"
                    labelText="Control Category"
                    value={
                      controlSampleData.sampleClassification.controlCategory
                    }
                    onChange={(e) =>
                      setControlSampleData((prev) => ({
                        ...prev,
                        sampleClassification: {
                          ...prev.sampleClassification,
                          controlCategory: e.target.value,
                        },
                      }))
                    }
                    disabled={isLoading}
                  >
                    <SelectItem value="" text="Select category..." />
                    <SelectItem
                      value="SYSTEM_SUITABILITY"
                      text="System Suitability"
                    />
                    <SelectItem
                      value="METHOD_VALIDATION"
                      text="Method Validation"
                    />
                    <SelectItem value="RUN_ACCEPTANCE" text="Run Acceptance" />
                    <SelectItem value="MATRIX_EFFECT" text="Matrix Effect" />
                  </Select>
                </div>

                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    gap: "1rem",
                    marginBottom: "1rem",
                  }}
                >
                  <Select
                    id="control-source"
                    labelText="Control Source"
                    value={controlSampleData.sampleClassification.controlSource}
                    onChange={(e) =>
                      setControlSampleData((prev) => ({
                        ...prev,
                        sampleClassification: {
                          ...prev.sampleClassification,
                          controlSource: e.target.value,
                        },
                      }))
                    }
                    disabled={isLoading}
                  >
                    <SelectItem value="" text="Select source..." />
                    <SelectItem
                      value="REFERENCE_STANDARD"
                      text="Reference Standard"
                    />
                    <SelectItem value="SPIKED_MATRIX" text="Spiked Matrix" />
                    <SelectItem value="BLANK_MATRIX" text="Blank Matrix" />
                  </Select>

                  <TextArea
                    id="expected-result"
                    labelText="Expected Result"
                    placeholder="e.g., 100 ng/mL ± 15%"
                    value={
                      controlSampleData.sampleClassification.expectedResult
                    }
                    onChange={(e) =>
                      setControlSampleData((prev) => ({
                        ...prev,
                        sampleClassification: {
                          ...prev.sampleClassification,
                          expectedResult: e.target.value,
                        },
                      }))
                    }
                    disabled={isLoading}
                    rows={2}
                  />
                </div>

                {/* Control Batch Information */}
                <div
                  style={{
                    marginBottom: "1rem",
                    padding: "1rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                  }}
                >
                  <h6
                    style={{
                      marginBottom: "0.75rem",
                      fontSize: "0.875rem",
                      fontWeight: "500",
                    }}
                  >
                    Control Batch Information:
                  </h6>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "1rem",
                      marginBottom: "1rem",
                    }}
                  >
                    <TextArea
                      id="control-batch"
                      labelText="Control Batch/Lot"
                      placeholder="e.g., CTL-2025-001"
                      value={
                        controlSampleData.sampleClassification.batchInfo
                          .controlBatch
                      }
                      onChange={(e) =>
                        setControlSampleData((prev) => ({
                          ...prev,
                          sampleClassification: {
                            ...prev.sampleClassification,
                            batchInfo: {
                              ...prev.sampleClassification.batchInfo,
                              controlBatch: e.target.value,
                            },
                          },
                        }))
                      }
                      disabled={isLoading}
                      rows={1}
                    />

                    <TextArea
                      id="control-supplier"
                      labelText="Supplier"
                      placeholder="e.g., Sigma-Aldrich"
                      value={
                        controlSampleData.sampleClassification.batchInfo
                          .supplier
                      }
                      onChange={(e) =>
                        setControlSampleData((prev) => ({
                          ...prev,
                          sampleClassification: {
                            ...prev.sampleClassification,
                            batchInfo: {
                              ...prev.sampleClassification.batchInfo,
                              supplier: e.target.value,
                            },
                          },
                        }))
                      }
                      disabled={isLoading}
                      rows={1}
                    />
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "1rem",
                    }}
                  >
                    <DatePicker
                      datePickerType="single"
                      value={
                        controlSampleData.sampleClassification.batchInfo
                          .expiryDate
                      }
                      onChange={([date]) =>
                        setControlSampleData((prev) => ({
                          ...prev,
                          sampleClassification: {
                            ...prev.sampleClassification,
                            batchInfo: {
                              ...prev.sampleClassification.batchInfo,
                              expiryDate: date
                                ? date.toISOString().split("T")[0]
                                : "",
                            },
                          },
                        }))
                      }
                      disabled={isLoading}
                    >
                      <DatePickerInput
                        id="expiry-date"
                        labelText="Expiry Date"
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>

                    <Select
                      id="storage-condition"
                      labelText="Storage Condition"
                      value={
                        controlSampleData.sampleClassification.batchInfo
                          .storageCondition
                      }
                      onChange={(e) =>
                        setControlSampleData((prev) => ({
                          ...prev,
                          sampleClassification: {
                            ...prev.sampleClassification,
                            batchInfo: {
                              ...prev.sampleClassification.batchInfo,
                              storageCondition: e.target.value,
                            },
                          },
                        }))
                      }
                      disabled={isLoading}
                    >
                      <SelectItem value="" text="Select storage..." />
                      <SelectItem
                        value="-80°C"
                        text="-80°C (Ultra-low freezer)"
                      />
                      <SelectItem value="-20°C" text="-20°C (Freezer)" />
                      <SelectItem value="2-8°C" text="2-8°C (Refrigerator)" />
                      <SelectItem
                        value="15-25°C"
                        text="15-25°C (Room temperature)"
                      />
                      <SelectItem value="DESICCATED" text="Desiccated" />
                    </Select>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* QC Status Preview */}
          <div
            style={{
              padding: "0.75rem",
              backgroundColor: (() => {
                const allPassed = [
                  qcVerificationData.volumeAdequate,
                  qcVerificationData.containerIntegrity,
                  qcVerificationData.temperatureOK,
                  qcVerificationData.labelingComplete,
                  qcVerificationData.chainOfCustody,
                ].every(Boolean);
                return allPassed ? "#e7f6ed" : "#ffeae6";
              })(),
              borderRadius: "4px",
              border: (() => {
                const allPassed = [
                  qcVerificationData.volumeAdequate,
                  qcVerificationData.containerIntegrity,
                  qcVerificationData.temperatureOK,
                  qcVerificationData.labelingComplete,
                  qcVerificationData.chainOfCustody,
                ].every(Boolean);
                return allPassed ? "1px solid #198038" : "1px solid #da1e28";
              })(),
            }}
          >
            <div
              style={{
                fontSize: "0.875rem",
                fontWeight: "500",
                marginBottom: "0.25rem",
              }}
            >
              QC Status Preview:
            </div>
            <div style={{ fontSize: "0.875rem" }}>
              {(() => {
                const checks = [
                  qcVerificationData.volumeAdequate,
                  qcVerificationData.containerIntegrity,
                  qcVerificationData.temperatureOK,
                  qcVerificationData.labelingComplete,
                  qcVerificationData.chainOfCustody,
                ];
                const passed = checks.filter(Boolean).length;
                const total = checks.length;
                const allPassed = checks.every(Boolean);

                return allPassed
                  ? `✅ QC PASS - All ${total} criteria met`
                  : `❌ QC FAIL - ${passed}/${total} criteria met`;
              })()}
            </div>
          </div>

          {isLoading && (
            <div style={{ textAlign: "center", marginTop: "1rem" }}>
              <div>Processing QC verification...</div>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default BioanalyticalSampleReceptionPage;
