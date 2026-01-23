import { useState, useEffect, useRef, useCallback } from "react";
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
 * MNTDReceptionVerificationPage - Page 2 of the MNTD workflow.
 * Handles laboratory reception and verification of samples.
 *
 * Quality Assessment:
 * - Pass: Proceed to storage/processing
 * - Fail: Two options:
 *   1. Discard and notify researcher (sample will not proceed)
 *   2. Keep with remarks and proceed with flagged status
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID for questionnaire loading
 */
function MNTDReceptionVerificationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
  notebookId: _notebookId,
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

  // Bulk apply form values - Simplified: direct Pass/Fail selection
  const [bulkApplyValues, setBulkApplyValues] = useState({
    // Receipt Information
    receivedDateTime: new Date().toISOString().slice(0, 16),
    receivedBy: "",
    // Condition notes
    conditionOnArrival: "",
    // QC Result & Actions - Direct selection (no checklist)
    qcResult: "",
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
              // Reception verification fields from data
              receivedDateTime: sample.data?.receivedDateTime,
              receivedBy: sample.data?.receivedBy,
              conditionOnArrival: sample.data?.conditionOnArrival,
              qcResult: sample.data?.qcResult,
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
      conditionOnArrival: "",
      qcResult: "",
      qcRemarks: "",
      failAction: "",
    });
  };

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.mntd.error.noSelection",
          defaultMessage: "Please select samples to apply values to.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.mntd.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
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
    if (bulkApplyValues.conditionOnArrival)
      data.conditionOnArrival = bulkApplyValues.conditionOnArrival;

    // QC result and actions
    if (bulkApplyValues.qcResult) data.qcResult = bulkApplyValues.qcResult;
    if (bulkApplyValues.qcRemarks) data.qcRemarks = bulkApplyValues.qcRemarks;
    if (bulkApplyValues.failAction)
      data.failAction = bulkApplyValues.failAction;

    // Check if any data was entered
    if (Object.keys(data).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.mntd.error.noData",
          defaultMessage: "Please enter at least one value to apply.",
        }),
      );
      setIsBulkApplying(false);
      return;
    }

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
                id: "notebook.page.mntd.success.applied",
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
              id: "notebook.page.mntd.error.apply",
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
          id: "notebook.page.mntd.error.noPage",
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
            id: "notebook.page.mntd.error.missingQC",
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
                id: "notebook.page.mntd.success.verifiedMixed",
                defaultMessage:
                  "{passCount} sample(s) verified and can proceed. {discardCount} sample(s) were discarded and will not proceed.",
              },
              {
                passCount: proceedingSamples.length,
                discardCount: discardedSamples.length,
              },
            );
          } else if (discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.mntd.success.verifiedDiscarded",
                defaultMessage:
                  "{count} sample(s) were discarded. They will not proceed to storage/processing.",
              },
              { count: discardedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "notebook.page.mntd.success.verified",
                defaultMessage:
                  "Marked {count} sample(s) as verified. They can now proceed to storage/processing.",
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
              id: "notebook.page.mntd.error.status",
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

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "Pass") return <Tag type="green">Pass</Tag>;
    if (qcResult === "Fail") return <Tag type="red">Fail</Tag>;
    if (qcResult === "Pass with remarks") return <Tag type="teal">Pass*</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  return (
    <div className="mntd-reception-verification-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.receptionVerification.title"
            defaultMessage="Laboratory Reception & Verification"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.receptionVerification.description"
            defaultMessage="Confirm physical receipt and validate sample quality. Use bulk value entry to verify multiple samples at once. Record receipt information, verification status, and QC assessment."
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
                  id="notebook.page.mntd.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.qcFailed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.qcPending"
                  defaultMessage="QC Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.verified"
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
            id="notebook.page.mntd.bulkApply"
            defaultMessage="Bulk Apply Values ({count})"
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
              id="notebook.page.mntd.markDone"
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
              id="notebook.page.mntd.pendingTable.title"
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
              id="notebook.page.mntd.pendingTable.description"
              defaultMessage="Select samples and use 'Bulk Apply Values' to perform verification. Samples with QC result can be marked as verified."
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
                key: "failAction",
                header: intl.formatMessage({
                  id: "notebook.grid.failAction",
                  defaultMessage: "Action Taken",
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
                  id="notebook.page.mntd.pendingTable.empty"
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
              id="notebook.page.mntd.completedTable.title"
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
              id="notebook.page.mntd.completedTable.description"
              defaultMessage="Samples that have completed verification. Passed samples can proceed to storage/processing."
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
                key: "failAction",
                header: intl.formatMessage({
                  id: "notebook.grid.failAction",
                  defaultMessage: "Action Taken",
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
                  id="notebook.page.mntd.completedTable.empty"
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
              id="notebook.page.mntd.receptionVerification.empty"
              defaultMessage="No samples available for verification. Samples must be created in the Sample Intake page first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.bulkApply.title",
          defaultMessage: "Laboratory Reception & Verification",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : bulkApplyValues.qcResult === "Pass"
              ? intl.formatMessage({
                  id: "notebook.mntd.qc.action.pass",
                  defaultMessage: "Pass - Proceed to Storage",
                })
              : bulkApplyValues.qcResult === "Fail"
                ? intl.formatMessage({
                    id: "notebook.mntd.qc.action.fail",
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
        size="lg"
        primaryButtonDisabled={isBulkApplying || !bulkApplyValues.qcResult}
        danger={bulkApplyValues.qcResult === "Fail"}
      >
        <div className="qc-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.mntd.bulkApply.description"
              defaultMessage="Apply reception and verification values to {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Receipt Information Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.mntd.section.receipt"
                defaultMessage="Receipt Information"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <div className="cds--form-item">
                  <label className="cds--label">
                    <FormattedMessage
                      id="notebook.mntd.receivedDateTime"
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
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="receivedBy"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.receivedBy",
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
                    id: "notebook.mntd.receivedBy.placeholder",
                    defaultMessage: "Enter staff name",
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
                    id: "notebook.mntd.conditionOnArrival",
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
                    id: "notebook.mntd.conditionOnArrival.placeholder",
                    defaultMessage: "Describe sample condition on arrival...",
                  })}
                  rows={2}
                />
              </Column>
            </Grid>
          </div>

          {/* Quality Assessment - Direct Pass/Fail Selection */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="notebook.mntd.section.qualityAssessment"
                defaultMessage="Quality Assessment"
              />
            </h5>
            <div
              className={`qc-result-section ${bulkApplyValues.qcResult === "Fail" ? "fail" : bulkApplyValues.qcResult === "Pass" ? "pass" : ""}`}
            >
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.mntd.qcResult.label",
                      defaultMessage: "Sample Quality Assessment",
                    })}
                    name="qcResult"
                    valueSelected={bulkApplyValues.qcResult}
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        qcResult: value,
                        // Clear fail action if now passing
                        failAction: value === "Pass" ? "" : prev.failAction,
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: "notebook.mntd.qcResult.pass",
                        defaultMessage: "Pass - Proceed to storage/processing",
                      })}
                      value="Pass"
                      id="qcResult-pass"
                    />
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: "notebook.mntd.qcResult.fail",
                        defaultMessage: "Fail - Requires action",
                      })}
                      value="Fail"
                      id="qcResult-fail"
                    />
                  </RadioButtonGroup>
                </Column>
              </Grid>
            </div>
          </div>

          {/* Fail Actions - Only show if QC result is Fail */}
          {bulkApplyValues.qcResult === "Fail" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.mntd.section.failActions"
                  defaultMessage="Fail Actions"
                />
              </h5>
              <div className="qc-fail-actions">
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "notebook.mntd.failAction.label",
                        defaultMessage: "Select action for failed samples",
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
                          id: "notebook.mntd.failAction.discard",
                          defaultMessage:
                            "Discard and notify researcher (sample will NOT proceed)",
                        })}
                        value="discard"
                        id="failAction-discard"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "notebook.mntd.failAction.proceed",
                          defaultMessage:
                            "Keep with remarks and proceed with flagged status",
                        })}
                        value="proceed_flagged"
                        id="failAction-proceed"
                      />
                    </RadioButtonGroup>
                  </Column>

                  {/* QC Remarks - Required for both fail actions */}
                  <Column lg={16} md={8} sm={4}>
                    <TextArea
                      id="qcRemarks"
                      labelText={intl.formatMessage({
                        id: "notebook.mntd.qcRemarks",
                        defaultMessage: "Remarks (Required for failed samples)",
                      })}
                      value={bulkApplyValues.qcRemarks}
                      onChange={(e) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          qcRemarks: e.target.value,
                        }))
                      }
                      placeholder={intl.formatMessage({
                        id: "notebook.mntd.qcRemarks.placeholder",
                        defaultMessage:
                          "Document the reason for failure and any observations...",
                      })}
                      rows={3}
                      required
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

export default MNTDReceptionVerificationPage;
