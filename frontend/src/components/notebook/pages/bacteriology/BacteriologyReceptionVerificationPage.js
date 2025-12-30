import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextArea,
  RadioButtonGroup,
  RadioButton,
  Tag,
  TextInput,
  Checkbox,
  Dropdown,
} from "@carbon/react";
import { Checkmark, Edit, WarningAlt } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * QC Criteria checklist items for Bacteriology samples
 * Based on user requirements:
 * - Temperature compliance (maintained cold chain)
 * - Volume adequacy
 * - Labeling correctness
 * - Triple packaging integrity
 * - No contamination signs
 * - No leakage
 * - Proper container type
 */
const QC_CRITERIA = [
  {
    id: "temperatureCompliance",
    labelId: "notebook.bacteriology.qc.temperatureCompliance",
    defaultLabel: "Temperature compliance (maintained cold chain)",
  },
  {
    id: "volumeAdequacy",
    labelId: "notebook.bacteriology.qc.volumeAdequacy",
    defaultLabel: "Volume adequacy",
  },
  {
    id: "labelingCorrectness",
    labelId: "notebook.bacteriology.qc.labelingCorrectness",
    defaultLabel: "Labeling correctness",
  },
  {
    id: "packagingIntegrity",
    labelId: "notebook.bacteriology.qc.packagingIntegrity",
    defaultLabel: "Triple packaging integrity",
  },
  {
    id: "noContamination",
    labelId: "notebook.bacteriology.qc.noContamination",
    defaultLabel: "No contamination signs",
  },
  {
    id: "noLeakage",
    labelId: "notebook.bacteriology.qc.noLeakage",
    defaultLabel: "No leakage",
  },
  {
    id: "properContainer",
    labelId: "notebook.bacteriology.qc.properContainer",
    defaultLabel: "Proper container type",
  },
];

/**
 * Rejection reasons for failed QC
 */
const REJECTION_REASONS = [
  { id: "mislabeling", label: "Mislabeling" },
  { id: "insufficient_volume", label: "Insufficient volume" },
  { id: "contaminated", label: "Contaminated samples" },
  { id: "damaged_container", label: "Damaged container" },
  { id: "expired_transport", label: "Expired transport medium" },
  { id: "temperature_excursion", label: "Temperature excursion" },
  { id: "other", label: "Other" },
];

/**
 * BacteriologyReceptionVerificationPage - Page 2 of the Bacteriology workflow.
 * Handles laboratory reception and quality assessment of samples.
 *
 * Quality Assessment:
 * - QC Checklist: Temperature compliance, volume, labeling, packaging, contamination, leakage, container
 * - Pass: All criteria met, proceed to storage
 * - Fail: Select rejection reason and action:
 *   1. Discard and notify researcher (sample will not proceed)
 *   2. Keep with remarks and proceed with flagged status
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function BacteriologyReceptionVerificationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);

  // Bulk apply form values
  const [bulkApplyValues, setBulkApplyValues] = useState({
    // Receipt Information
    receivedDateTime: new Date().toISOString().slice(0, 16),
    receivedBy: "",
    // Temperature on arrival
    arrivalTemperature: "",
    // Condition notes
    conditionOnArrival: "",
    // QC Checklist - all criteria
    qcChecklist: QC_CRITERIA.reduce((acc, item) => {
      acc[item.id] = false;
      return acc;
    }, {}),
    // QC Result & Actions
    qcResult: "",
    rejectionReason: "",
    qcRemarks: "",
    failAction: "",
  });

  // Check if page has real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples for this page
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
              // Bacteriology specific fields from data
              projectName: sample.data?.projectName,
              sampleOrigin: sample.data?.sampleOrigin,
              // Reception verification fields
              receivedDateTime: sample.data?.receivedDateTime,
              receivedBy: sample.data?.receivedBy,
              arrivalTemperature: sample.data?.arrivalTemperature,
              conditionOnArrival: sample.data?.conditionOnArrival,
              qcChecklist: sample.data?.qcChecklist,
              qcResult: sample.data?.qcResult,
              rejectionReason: sample.data?.rejectionReason,
              qcRemarks: sample.data?.qcRemarks,
              failAction: sample.data?.failAction,
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

  // Reset bulk apply values
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      receivedDateTime: new Date().toISOString().slice(0, 16),
      receivedBy: "",
      arrivalTemperature: "",
      conditionOnArrival: "",
      qcChecklist: QC_CRITERIA.reduce((acc, item) => {
        acc[item.id] = false;
        return acc;
      }, {}),
      qcResult: "",
      rejectionReason: "",
      qcRemarks: "",
      failAction: "",
    });
  };

  // Calculate QC result based on checklist
  const calculateQCResult = (checklist) => {
    const allPassed = Object.values(checklist).every((v) => v === true);
    return allPassed ? "Pass" : "Fail";
  };

  // Handle checklist change
  const handleChecklistChange = (criteriaId, checked) => {
    setBulkApplyValues((prev) => {
      const newChecklist = { ...prev.qcChecklist, [criteriaId]: checked };
      const autoResult = calculateQCResult(newChecklist);
      return {
        ...prev,
        qcChecklist: newChecklist,
        // Auto-set QC result based on checklist
        qcResult: autoResult,
        // Clear fail-related fields if now passing
        failAction: autoResult === "Pass" ? "" : prev.failAction,
        rejectionReason: autoResult === "Pass" ? "" : prev.rejectionReason,
      };
    });
  };

  // Handle "Check All" for QC criteria
  const handleCheckAll = () => {
    setBulkApplyValues((prev) => ({
      ...prev,
      qcChecklist: QC_CRITERIA.reduce((acc, item) => {
        acc[item.id] = true;
        return acc;
      }, {}),
      qcResult: "Pass",
      failAction: "",
      rejectionReason: "",
    }));
  };

  // Handle "Clear All" for QC criteria
  const handleClearAll = () => {
    setBulkApplyValues((prev) => ({
      ...prev,
      qcChecklist: QC_CRITERIA.reduce((acc, item) => {
        acc[item.id] = false;
        return acc;
      }, {}),
      qcResult: "Fail",
    }));
  };

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.bacteriology.error.noSelection",
          defaultMessage: "Please select samples to apply values to.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.bacteriology.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate: QC result must be set
    if (!bulkApplyValues.qcResult) {
      setError(
        intl.formatMessage({
          id: "notebook.page.bacteriology.error.noQCResult",
          defaultMessage:
            "Please complete the QC checklist to determine pass/fail status.",
        }),
      );
      return;
    }

    // Validate: If fail, must have rejection reason and fail action
    if (bulkApplyValues.qcResult === "Fail") {
      if (!bulkApplyValues.rejectionReason) {
        setError(
          intl.formatMessage({
            id: "notebook.page.bacteriology.error.noRejectionReason",
            defaultMessage:
              "Please select a rejection reason for failed samples.",
          }),
        );
        return;
      }
      if (!bulkApplyValues.failAction) {
        setError(
          intl.formatMessage({
            id: "notebook.page.bacteriology.error.noFailAction",
            defaultMessage: "Please select an action for failed samples.",
          }),
        );
        return;
      }
    }

    setIsBulkApplying(true);
    setError(null);

    // Prepare the data to apply
    const data = {};

    // Receipt information
    if (bulkApplyValues.receivedDateTime)
      data.receivedDateTime = bulkApplyValues.receivedDateTime;
    if (bulkApplyValues.receivedBy)
      data.receivedBy = bulkApplyValues.receivedBy;
    if (bulkApplyValues.arrivalTemperature)
      data.arrivalTemperature = bulkApplyValues.arrivalTemperature;
    if (bulkApplyValues.conditionOnArrival)
      data.conditionOnArrival = bulkApplyValues.conditionOnArrival;

    // QC checklist and result
    data.qcChecklist = bulkApplyValues.qcChecklist;
    data.qcResult = bulkApplyValues.qcResult;
    if (bulkApplyValues.rejectionReason)
      data.rejectionReason = bulkApplyValues.rejectionReason;
    if (bulkApplyValues.qcRemarks) data.qcRemarks = bulkApplyValues.qcRemarks;
    if (bulkApplyValues.failAction)
      data.failAction = bulkApplyValues.failAction;

    const applyData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: data,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(applyData),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.bacteriology.success.applied",
                defaultMessage: "Applied values to {count} samples.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setBulkApplyModalOpen(false);
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.bacteriology.error.apply",
              defaultMessage: "Failed to apply values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    pageData?.id,
    bulkApplyValues,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle marking samples as verified (QC complete)
  const handleMarkVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.bacteriology.error.noPage",
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
            id: "notebook.page.bacteriology.error.missingQC",
            defaultMessage:
              "{count} sample(s) are missing QC result. Please complete verification first.",
          },
          { count: missingQC.length },
        ),
      );
      return;
    }

    // Separate samples by their fail action
    // Discarded samples should NOT proceed to next page
    const discardedSamples = selectedSamples.filter(
      (s) => s.failAction === "discard",
    );
    const proceedingSamples = selectedSamples.filter(
      (s) => s.failAction !== "discard",
    );

    // Track how many requests we need to make
    let completedRequests = 0;
    let failedRequests = 0;
    const totalRequests =
      (proceedingSamples.length > 0 ? 1 : 0) +
      (discardedSamples.length > 0 ? 1 : 0);

    const handleRequestComplete = () => {
      completedRequests++;
      if (completedRequests === totalRequests) {
        if (failedRequests === 0) {
          // Build appropriate success message
          let message = "";
          if (proceedingSamples.length > 0 && discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.bacteriology.success.verifiedMixed",
                defaultMessage:
                  "{passCount} sample(s) verified and can proceed to storage. {discardCount} sample(s) were discarded and will not proceed.",
              },
              {
                passCount: proceedingSamples.length,
                discardCount: discardedSamples.length,
              },
            );
          } else if (discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.bacteriology.success.verifiedDiscarded",
                defaultMessage:
                  "{count} sample(s) were discarded. They will not proceed to storage.",
              },
              { count: discardedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "notebook.page.bacteriology.success.verified",
                defaultMessage:
                  "Marked {count} sample(s) as verified. They can now proceed to Temporary Storage Assignment.",
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
              id: "notebook.page.bacteriology.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      }
    };

    // Update proceeding samples to COMPLETED (they will proceed to next page)
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

    // Update discarded samples to SKIPPED (they will NOT proceed to next page)
    if (discardedSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: discardedSamples.map((s) => parseInt(s.id, 10)),
          status: "SKIPPED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    // If no samples to process, just return
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
  const verifiedCount = samples.filter(
    (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
  ).length;

  // Count checked criteria
  const checkedCount = useMemo(() => {
    return Object.values(bulkApplyValues.qcChecklist).filter((v) => v).length;
  }, [bulkApplyValues.qcChecklist]);

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "Pass") return <Tag type="green">Pass</Tag>;
    if (qcResult === "Fail") return <Tag type="red">Fail</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  // Get origin badge type
  const getOriginBadgeType = (origin) => {
    switch (origin?.toLowerCase()) {
      case "human":
        return "blue";
      case "animal":
        return "purple";
      case "environmental":
        return "green";
      case "food/beverage":
        return "orange";
      default:
        return "gray";
    }
  };

  return (
    <div className="bacteriology-reception-verification-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.receptionVerification.title"
            defaultMessage="Laboratory Reception & Verification"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.receptionVerification.description"
            defaultMessage="Confirm physical receipt and validate sample quality. Use the QC checklist to assess temperature compliance, volume, labeling, packaging integrity, contamination, leakage, and container type. Samples that pass QC proceed to Temporary Storage Assignment."
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
                  id="notebook.page.bacteriology.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.qcFailed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.qcPending"
                  defaultMessage="QC Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.verified"
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
            id="notebook.page.bacteriology.bulkApply"
            defaultMessage="Bulk Apply QC ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkVerified}
          >
            <FormattedMessage
              id="notebook.page.bacteriology.markDone"
              defaultMessage="Mark as Done ({count})"
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

      {/* Pending Verification Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.bacteriology.pendingTable.title"
              defaultMessage="Samples Pending Verification"
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
              id="notebook.page.bacteriology.pendingTable.description"
              defaultMessage="Select samples and use 'Bulk Apply QC' to perform quality assessment. Samples with QC result can be marked as done."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="pending-verification"
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
                key: "sampleOrigin",
                header: intl.formatMessage({
                  id: "notebook.grid.origin",
                  defaultMessage: "Origin",
                }),
                render: (value) =>
                  value ? (
                    <Tag type={getOriginBadgeType(value)} size="sm">
                      {value}
                    </Tag>
                  ) : (
                    "-"
                  ),
              },
              {
                key: "receivedDateTime",
                header: intl.formatMessage({
                  id: "notebook.grid.received",
                  defaultMessage: "Received",
                }),
              },
              {
                key: "receivedBy",
                header: intl.formatMessage({
                  id: "notebook.grid.receivedBy",
                  defaultMessage: "Received By",
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
                key: "rejectionReason",
                header: intl.formatMessage({
                  id: "notebook.grid.rejectionReason",
                  defaultMessage: "Rejection Reason",
                }),
                render: (value) => {
                  if (!value) return "-";
                  const reason = REJECTION_REASONS.find((r) => r.id === value);
                  return reason ? reason.label : value;
                },
              },
              {
                key: "failAction",
                header: intl.formatMessage({
                  id: "notebook.grid.failAction",
                  defaultMessage: "Action",
                }),
                render: (value) => {
                  if (!value) return "-";
                  if (value === "discard")
                    return <Tag type="red">Discarded</Tag>;
                  if (value === "proceed_flagged")
                    return <Tag type="purple">Flagged</Tag>;
                  return value;
                },
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
                  id="notebook.page.bacteriology.pendingTable.empty"
                  defaultMessage="No samples pending verification. All samples have been processed."
                />
              </p>
            </div>
          )}
      </div>

      {/* Verified Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.bacteriology.completedTable.title"
              defaultMessage="Verified Samples"
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
              id="notebook.page.bacteriology.completedTable.description"
              defaultMessage="Samples that have completed verification. Passed samples can proceed to Temporary Storage Assignment."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="verified-samples"
            samples={samples.filter(
              (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
            )}
            selectedIds={[]}
            showSelection={false}
            loading={loading}
            columns={[
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
                key: "sampleOrigin",
                header: intl.formatMessage({
                  id: "notebook.grid.origin",
                  defaultMessage: "Origin",
                }),
                render: (value) =>
                  value ? (
                    <Tag type={getOriginBadgeType(value)} size="sm">
                      {value}
                    </Tag>
                  ) : (
                    "-"
                  ),
              },
              {
                key: "receivedBy",
                header: intl.formatMessage({
                  id: "notebook.grid.receivedBy",
                  defaultMessage: "Received By",
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
                key: "rejectionReason",
                header: intl.formatMessage({
                  id: "notebook.grid.rejectionReason",
                  defaultMessage: "Rejection Reason",
                }),
                render: (value) => {
                  if (!value) return "-";
                  const reason = REJECTION_REASONS.find((r) => r.id === value);
                  return reason ? reason.label : value;
                },
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.disposition",
                  defaultMessage: "Disposition",
                }),
                render: (value) => {
                  if (value === "SKIPPED") {
                    return (
                      <Tag type="red">
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
            (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.bacteriology.completedTable.empty"
                  defaultMessage="No samples have been verified yet."
                />
              </p>
            </div>
          )}
      </div>

      {/* Global empty state - only show when no samples at all */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.bacteriology.receptionVerification.empty"
              defaultMessage="No samples available for verification. Samples must be received in the Sample Reception page first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.bulkApply.title",
          defaultMessage: "Sample Quality Assessment",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : bulkApplyValues.qcResult === "Pass"
              ? intl.formatMessage({
                  id: "notebook.bacteriology.qc.action.pass",
                  defaultMessage: "Pass - Proceed to Storage",
                })
              : bulkApplyValues.qcResult === "Fail"
                ? intl.formatMessage({
                    id: "notebook.bacteriology.qc.action.fail",
                    defaultMessage: "Fail - Apply Action",
                  })
                : intl.formatMessage({
                    id: "label.apply",
                    defaultMessage: "Apply",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="md"
        primaryButtonDisabled={isBulkApplying || !bulkApplyValues.qcResult}
        danger={bulkApplyValues.qcResult === "Fail"}
      >
        <div className="qc-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.bacteriology.bulkApply.description"
              defaultMessage="Apply reception and quality assessment values to {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Receipt Information Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.bacteriology.section.receipt"
                defaultMessage="Receipt Information"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={5} md={4} sm={4}>
                <div className="cds--form-item">
                  <label className="cds--label">
                    <FormattedMessage
                      id="notebook.bacteriology.receivedDateTime"
                      defaultMessage="Received Date & Time"
                    />
                  </label>
                  <input
                    type="datetime-local"
                    className="cds--text-input"
                    value={bulkApplyValues.receivedDateTime}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        receivedDateTime: e.target.value,
                      }))
                    }
                  />
                </div>
              </Column>
              <Column lg={5} md={4} sm={4}>
                <TextInput
                  id="receivedBy"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.receivedBy",
                    defaultMessage: "Received By (Staff Name)",
                  })}
                  value={bulkApplyValues.receivedBy}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      receivedBy: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.bacteriology.receivedBy.placeholder",
                    defaultMessage: "Enter staff name",
                  })}
                />
              </Column>
              <Column lg={6} md={4} sm={4}>
                <TextInput
                  id="arrivalTemperature"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.arrivalTemperature",
                    defaultMessage: "Temperature on Arrival (°C)",
                  })}
                  value={bulkApplyValues.arrivalTemperature}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      arrivalTemperature: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.bacteriology.arrivalTemperature.placeholder",
                    defaultMessage: "e.g., 4, -20, Room Temperature",
                  })}
                />
              </Column>
            </Grid>
          </div>

          {/* Condition on Arrival */}
          <div className="qc-section">
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="conditionOnArrival"
                  labelText={intl.formatMessage({
                    id: "notebook.bacteriology.conditionOnArrival",
                    defaultMessage: "Condition on Arrival",
                  })}
                  value={bulkApplyValues.conditionOnArrival}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      conditionOnArrival: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.bacteriology.conditionOnArrival.placeholder",
                    defaultMessage: "Describe sample condition on arrival...",
                  })}
                  rows={2}
                />
              </Column>
            </Grid>
          </div>

          {/* QC Checklist Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.bacteriology.section.qcChecklist"
                defaultMessage="Quality Control Checklist"
              />
              <span className="qc-checklist-count">
                ({checkedCount}/{QC_CRITERIA.length})
              </span>
            </h5>
            <div className="qc-checklist-actions">
              <Button kind="ghost" size="sm" onClick={handleCheckAll}>
                <FormattedMessage
                  id="notebook.bacteriology.qc.checkAll"
                  defaultMessage="Check All (Pass)"
                />
              </Button>
              <Button kind="ghost" size="sm" onClick={handleClearAll}>
                <FormattedMessage
                  id="notebook.bacteriology.qc.clearAll"
                  defaultMessage="Clear All"
                />
              </Button>
            </div>
            <div className="qc-checklist-items">
              {QC_CRITERIA.map((criteria) => (
                <Checkbox
                  key={criteria.id}
                  id={`qc-${criteria.id}`}
                  labelText={intl.formatMessage({
                    id: criteria.labelId,
                    defaultMessage: criteria.defaultLabel,
                  })}
                  checked={bulkApplyValues.qcChecklist[criteria.id]}
                  onChange={(_, { checked }) =>
                    handleChecklistChange(criteria.id, checked)
                  }
                />
              ))}
            </div>
          </div>

          {/* QC Status Decision Section */}
          <div className="qc-section qc-decision-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.bacteriology.section.qcStatus"
                defaultMessage="QC Status Decision"
              />
            </h5>
            <p className="qc-decision-hint">
              <FormattedMessage
                id="notebook.bacteriology.qcStatus.hint"
                defaultMessage="Based on the checklist above, select the final QC decision. You can override the suggested result if needed."
              />
            </p>
            <div className="qc-decision-buttons">
              <Button
                kind={bulkApplyValues.qcResult === "Pass" ? "primary" : "ghost"}
                size="md"
                renderIcon={Checkmark}
                onClick={() =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    qcResult: "Pass",
                    rejectionReason: "",
                    failAction: "",
                    qcRemarks: "",
                  }))
                }
                className={`qc-decision-btn pass ${bulkApplyValues.qcResult === "Pass" ? "selected" : ""}`}
              >
                <FormattedMessage
                  id="notebook.bacteriology.qcStatus.pass"
                  defaultMessage="Pass - Proceed to Storage"
                />
              </Button>
              <Button
                kind={bulkApplyValues.qcResult === "Fail" ? "danger" : "ghost"}
                size="md"
                renderIcon={WarningAlt}
                onClick={() =>
                  setBulkApplyValues((prev) => ({
                    ...prev,
                    qcResult: "Fail",
                  }))
                }
                className={`qc-decision-btn fail ${bulkApplyValues.qcResult === "Fail" ? "selected" : ""}`}
              >
                <FormattedMessage
                  id="notebook.bacteriology.qcStatus.fail"
                  defaultMessage="Fail - Document Rejection"
                />
              </Button>
            </div>
            <div
              className={`qc-result-indicator ${bulkApplyValues.qcResult === "Pass" ? "pass" : bulkApplyValues.qcResult === "Fail" ? "fail" : ""}`}
            >
              {bulkApplyValues.qcResult === "Pass" && (
                <Tag type="green" size="md">
                  <Checkmark size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.bacteriology.qc.resultPass"
                    defaultMessage="QC PASSED - Sample will proceed to storage"
                  />
                </Tag>
              )}
              {bulkApplyValues.qcResult === "Fail" && (
                <Tag type="red" size="md">
                  <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.bacteriology.qc.resultFail"
                    defaultMessage="QC FAILED - Must document rejection reason and action"
                  />
                </Tag>
              )}
              {!bulkApplyValues.qcResult && (
                <Tag type="gray" size="md">
                  <FormattedMessage
                    id="notebook.bacteriology.qc.resultPending"
                    defaultMessage="Select Pass or Fail to continue"
                  />
                </Tag>
              )}
            </div>
          </div>

          {/* Fail Actions - Only show if QC result is Fail */}
          {bulkApplyValues.qcResult === "Fail" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.bacteriology.section.failActions"
                  defaultMessage="Rejection Details & Actions"
                />
              </h5>
              <div className="qc-fail-actions">
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <Dropdown
                      id="rejectionReason"
                      titleText={intl.formatMessage({
                        id: "notebook.bacteriology.rejectionReason.label",
                        defaultMessage: "Rejection Reason (Required)",
                      })}
                      label={intl.formatMessage({
                        id: "notebook.bacteriology.rejectionReason.placeholder",
                        defaultMessage: "Select rejection reason",
                      })}
                      items={REJECTION_REASONS}
                      itemToString={(item) => (item ? item.label : "")}
                      selectedItem={REJECTION_REASONS.find(
                        (r) => r.id === bulkApplyValues.rejectionReason,
                      )}
                      onChange={({ selectedItem }) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          rejectionReason: selectedItem?.id || "",
                        }))
                      }
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.bacteriology.failAction.label",
                        defaultMessage: "Action for Failed Samples (Required)",
                      })}
                      name="failAction"
                      valueSelected={bulkApplyValues.failAction}
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          failAction: value,
                        }))
                      }
                      orientation="vertical"
                    >
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "notebook.bacteriology.failAction.discard",
                          defaultMessage:
                            "Discard and notify researcher (sample will NOT proceed)",
                        })}
                        value="discard"
                        id="failAction-discard"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "notebook.bacteriology.failAction.proceed",
                          defaultMessage:
                            "Keep with remarks and proceed with flagged status",
                        })}
                        value="proceed_flagged"
                        id="failAction-proceed"
                      />
                    </RadioButtonGroup>
                  </Column>

                  {/* QC Remarks */}
                  <Column lg={16} md={8} sm={4}>
                    <TextArea
                      id="qcRemarks"
                      labelText={intl.formatMessage({
                        id: "notebook.bacteriology.qcRemarks",
                        defaultMessage: "Remarks / Notes",
                      })}
                      value={bulkApplyValues.qcRemarks}
                      onChange={(e) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          qcRemarks: e.target.value,
                        }))
                      }
                      placeholder={intl.formatMessage({
                        id: "notebook.bacteriology.qcRemarks.placeholder",
                        defaultMessage:
                          "Document the reason for failure and any observations...",
                      })}
                      rows={3}
                    />
                  </Column>
                </Grid>
              </div>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologyReceptionVerificationPage;
