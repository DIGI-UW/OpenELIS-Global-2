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
 * QC Criteria checklist items for Medical Laboratory samples
 * Simplified checklist based on Bacteriology pattern with Medical Lab specific criteria
 */
const QC_CRITERIA = [
  {
    id: "labelingCorrect",
    labelId: "notebook.medlab.qc.labelingCorrect",
    defaultLabel: "Labeling correct and legible",
  },
  {
    id: "correctContainer",
    labelId: "notebook.medlab.qc.correctContainer",
    defaultLabel: "Correct container for test",
  },
  {
    id: "volumeAdequate",
    labelId: "notebook.medlab.qc.volumeAdequate",
    defaultLabel: "Volume adequate for testing",
  },
  {
    id: "matchingOrder",
    labelId: "notebook.medlab.qc.matchingOrder",
    defaultLabel: "Matching lab order",
  },
  {
    id: "noHemolysis",
    labelId: "notebook.medlab.qc.noHemolysis",
    defaultLabel: "No significant hemolysis (< moderate)",
  },
  {
    id: "noContamination",
    labelId: "notebook.medlab.qc.noContamination",
    defaultLabel: "No visible contamination",
  },
  {
    id: "properTemperature",
    labelId: "notebook.medlab.qc.properTemperature",
    defaultLabel: "Proper storage temperature maintained",
  },
];

/**
 * Rejection reasons for failed QC
 */
const REJECTION_REASONS = [
  { id: "hemolysis", label: "Hemolysis" },
  { id: "lipemia", label: "Lipemia" },
  { id: "icterus", label: "Icterus" },
  { id: "clotted", label: "Clotted Sample" },
  { id: "insufficient_volume", label: "Insufficient Volume" },
  { id: "wrong_container", label: "Wrong Container" },
  { id: "labeling_error", label: "Labeling Error" },
  { id: "delayed", label: "Excessive Delay" },
  { id: "contamination", label: "Contamination" },
  { id: "wrong_temperature", label: "Wrong Storage Temperature" },
  { id: "order_mismatch", label: "Order Mismatch" },
  { id: "other", label: "Other" },
];

/**
 * MedLabQualityCheckPage - Page 3 of the Medical Laboratory workflow.
 * Handles laboratory reception and quality assessment of samples.
 *
 * Quality Assessment:
 * - QC Checklist: Labeling, container, volume, order matching, hemolysis, contamination, temperature
 * - Pass: All criteria met, proceed to routing
 * - Fail: Select rejection reason and action:
 *   1. Discard and notify submitter (sample will not proceed)
 *   2. Keep with remarks and proceed with flagged status
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function MedLabQualityCheckPage({
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
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    console.log("[QC Page] Loading samples for entry:", entryId);

    // Use custom Medical Lab endpoint that returns ALL samples (pending + verified)
    // The endpoint checks Sample Collection page and QC page, returning samples with their QC status
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/samples-for-qc`,
      (response) => {
        if (componentMounted.current) {
          console.log("[QC Page] Raw response from backend:", response);

          if (response && Array.isArray(response)) {
            console.log(
              "[QC Page] Number of samples received:",
              response.length,
            );

            // Show breakdown by pageStatus
            const statusCounts = {
              PENDING: 0,
              COMPLETED: 0,
              SKIPPED: 0,
            };
            response.forEach((sample) => {
              const status = sample.pageStatus || "PENDING";
              if (status in statusCounts) {
                statusCounts[status]++;
              }
            });
            console.log(
              "[QC Page] Sample breakdown by pageStatus:",
              statusCounts,
            );

            // Show first sample's complete structure for debugging
            if (response.length > 0) {
              console.log(
                "[QC Page] First sample complete structure:",
                JSON.stringify(response[0], null, 2),
              );
            }

            const transformedSamples = response.map((sample) => ({
              id: String(sample.sampleItemId || sample.id),
              externalId: sample.externalId,
              accessionNumber: sample.labNo || sample.accessionNumber,
              sampleType: sample.sampleType,
              collectionDate: sample.collectionDate,
              // Use pageStatus which maps to NotebookPageSample.Status (PENDING, COMPLETED, SKIPPED)
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              linkedOrderLabNo: sample.orderLabNo,
              // Reception verification fields from data object (if QC has been performed)
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

            console.log("[QC Page] Transformed samples:", transformedSamples);
            console.log(
              "[QC Page] Transformed samples count by status:",
              transformedSamples.reduce((acc, s) => {
                acc[s.status] = (acc[s.status] || 0) + 1;
                return acc;
              }, {}),
            );

            setSamples(transformedSamples);
          } else {
            console.log(
              "[QC Page] Response is not an array or is null/undefined:",
              response,
            );
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

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
          id: "notebook.page.medlab.error.noSelection",
          defaultMessage: "Please select samples to apply values to.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.medlab.error.noPage",
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
          id: "notebook.page.medlab.error.noQCResult",
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
            id: "notebook.page.medlab.error.noRejectionReason",
            defaultMessage:
              "Please select a rejection reason for failed samples.",
          }),
        );
        return;
      }
      if (!bulkApplyValues.failAction) {
        setError(
          intl.formatMessage({
            id: "notebook.page.medlab.error.noFailAction",
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
                id: "notebook.page.medlab.success.applied",
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
              id: "notebook.page.medlab.error.apply",
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
          id: "notebook.page.medlab.error.noPage",
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
            id: "notebook.page.medlab.error.missingQC",
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
                id: "notebook.page.medlab.success.verifiedMixed",
                defaultMessage:
                  "{passCount} sample(s) verified and can proceed to routing. {discardCount} sample(s) were discarded and will not proceed.",
              },
              {
                passCount: proceedingSamples.length,
                discardCount: discardedSamples.length,
              },
            );
          } else if (discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.medlab.success.verifiedDiscarded",
                defaultMessage:
                  "{count} sample(s) were discarded. They will not proceed to routing.",
              },
              { count: discardedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "notebook.page.medlab.success.verified",
                defaultMessage:
                  "Marked {count} sample(s) as verified. They can now proceed to Sample Routing.",
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
              id: "notebook.page.medlab.error.status",
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

  return (
    <div className="medlab-quality-check-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.medlab.qualityCheck.title"
            defaultMessage="Sample Receipt & Quality Assessment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.medlab.qualityCheck.description"
            defaultMessage="Confirm physical receipt and validate sample quality. Use the QC checklist to assess labeling, container type, volume adequacy, order matching, hemolysis, contamination, and temperature. Samples that pass QC proceed to Sample Routing."
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
                  id="notebook.page.medlab.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.medlab.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.medlab.qcFailed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.medlab.qcPending"
                  defaultMessage="QC Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.medlab.verified"
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
            id="notebook.page.medlab.bulkApply"
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
              id="notebook.page.medlab.markDone"
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
              id="notebook.page.medlab.pendingTable.title"
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
              id="notebook.page.medlab.pendingTable.description"
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
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "sample.label.labnumber",
                  defaultMessage: "Lab Number",
                }),
              },
              {
                key: "patientName",
                header: intl.formatMessage({
                  id: "patient.label",
                  defaultMessage: "Patient",
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
                key: "linkedOrderLabNo",
                header: intl.formatMessage({
                  id: "notebook.grid.linkedOrder",
                  defaultMessage: "Linked Order",
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
                  id="notebook.page.medlab.pendingTable.empty"
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
              id="notebook.page.medlab.completedTable.title"
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
              id="notebook.page.medlab.completedTable.description"
              defaultMessage="Samples that have completed verification. Passed samples can proceed to Sample Routing."
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
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "sample.label.labnumber",
                  defaultMessage: "Lab Number",
                }),
              },
              {
                key: "patientName",
                header: intl.formatMessage({
                  id: "patient.label",
                  defaultMessage: "Patient",
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
                  id="notebook.page.medlab.completedTable.empty"
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
              id="notebook.page.medlab.qualityCheck.empty"
              defaultMessage="No samples available for verification. Samples must be collected in the Sample Collection page first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.medlab.bulkApply.title",
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
                  id: "notebook.medlab.qc.action.pass",
                  defaultMessage: "Pass - Proceed to Routing",
                })
              : bulkApplyValues.qcResult === "Fail"
                ? intl.formatMessage({
                    id: "notebook.medlab.qc.action.fail",
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
              id="notebook.medlab.bulkApply.description"
              defaultMessage="Apply reception and quality assessment values to {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Receipt Information Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.medlab.section.receipt"
                defaultMessage="Receipt Information"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={5} md={4} sm={4}>
                <div className="cds--form-item">
                  <label className="cds--label">
                    <FormattedMessage
                      id="notebook.medlab.receivedDateTime"
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
                    id: "notebook.medlab.receivedBy",
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
                    id: "notebook.medlab.receivedBy.placeholder",
                    defaultMessage: "Enter staff name",
                  })}
                />
              </Column>
              <Column lg={6} md={4} sm={4}>
                <TextInput
                  id="arrivalTemperature"
                  labelText={intl.formatMessage({
                    id: "notebook.medlab.arrivalTemperature",
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
                    id: "notebook.medlab.arrivalTemperature.placeholder",
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
                    id: "notebook.medlab.conditionOnArrival",
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
                    id: "notebook.medlab.conditionOnArrival.placeholder",
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
                id="notebook.medlab.section.qcChecklist"
                defaultMessage="Quality Control Checklist"
              />
              <span className="qc-checklist-count">
                ({checkedCount}/{QC_CRITERIA.length})
              </span>
            </h5>
            <div className="qc-checklist-actions">
              <Button kind="ghost" size="sm" onClick={handleCheckAll}>
                <FormattedMessage
                  id="notebook.medlab.qc.checkAll"
                  defaultMessage="Check All (Pass)"
                />
              </Button>
              <Button kind="ghost" size="sm" onClick={handleClearAll}>
                <FormattedMessage
                  id="notebook.medlab.qc.clearAll"
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
                id="notebook.medlab.section.qcStatus"
                defaultMessage="QC Status Decision"
              />
            </h5>
            <p className="qc-decision-hint">
              <FormattedMessage
                id="notebook.medlab.qcStatus.hint"
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
                  id="notebook.medlab.qcStatus.pass"
                  defaultMessage="Pass - Proceed to Routing"
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
                  id="notebook.medlab.qcStatus.fail"
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
                    id="notebook.medlab.qc.resultPass"
                    defaultMessage="QC PASSED - Sample will proceed to routing"
                  />
                </Tag>
              )}
              {bulkApplyValues.qcResult === "Fail" && (
                <Tag type="red" size="md">
                  <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.medlab.qc.resultFail"
                    defaultMessage="QC FAILED - Must document rejection reason and action"
                  />
                </Tag>
              )}
              {!bulkApplyValues.qcResult && (
                <Tag type="gray" size="md">
                  <FormattedMessage
                    id="notebook.medlab.qc.resultPending"
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
                  id="notebook.medlab.section.failActions"
                  defaultMessage="Rejection Details & Actions"
                />
              </h5>
              <div className="qc-fail-actions">
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <Dropdown
                      id="rejectionReason"
                      titleText={intl.formatMessage({
                        id: "notebook.medlab.rejectionReason.label",
                        defaultMessage: "Rejection Reason (Required)",
                      })}
                      label={intl.formatMessage({
                        id: "notebook.medlab.rejectionReason.placeholder",
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
                        id: "notebook.medlab.failAction.label",
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
                          id: "notebook.medlab.failAction.discard",
                          defaultMessage:
                            "Discard and notify submitter (sample will NOT proceed)",
                        })}
                        value="discard"
                        id="failAction-discard"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "notebook.medlab.failAction.proceed",
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
                        id: "notebook.medlab.qcRemarks",
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
                        id: "notebook.medlab.qcRemarks.placeholder",
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

export default MedLabQualityCheckPage;
