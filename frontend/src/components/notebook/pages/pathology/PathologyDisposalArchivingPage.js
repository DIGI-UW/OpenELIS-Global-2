import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  Modal,
  Tag,
  Loading,
  Checkbox,
} from "@carbon/react";
import { TrashCan, Archive, Renew, Locked } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyDisposalArchivingPage - Page 7: Disposal & Archiving
 *
 * Allows technicians to:
 * - Dispose samples based on criteria (retention expired, degraded, contaminated)
 * - Apply pathology-specific disposal methods
 * - Archive raw data, final reports, and lineage
 * - Mark samples as closed with pathologist sign-off
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID (optional)
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PathologyDisposalArchivingPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Disposal modal state
  const [showDisposalModal, setShowDisposalModal] = useState(false);
  const [disposalData, setDisposalData] = useState({
    disposalReason: "",
    disposalMethod: "",
    disposalDate: new Date().toISOString().split("T")[0],
    disposedBy: "",
    pathologistSignOff: "",
    qualityOfficerSignOff: "",
    certificateNumber: "",
    archiveLocation: "",
    retentionEndDate: "",
    notes: "",
    confirmDisposal: false,
  });

  // Summary counts
  const [summary, setSummary] = useState({
    total: 0,
    pendingDisposal: 0,
    disposed: 0,
    archived: 0,
    retentionSamples: 0,
  });

  // Pathology-specific disposal reasons
  const disposalReasons = [
    { value: "retention_expired", label: "Retention Period Expired" },
    { value: "study_complete", label: "Study/Case Complete" },
    { value: "degraded", label: "Sample Degraded - No Longer Viable" },
    { value: "contaminated", label: "Contaminated - Safety Concern" },
    { value: "storage_limit", label: "Storage Capacity Limit Reached" },
    { value: "patient_request", label: "Patient/Family Request" },
    { value: "legal_requirement", label: "Legal/Regulatory Requirement" },
    { value: "other", label: "Other" },
  ];

  // Pathology-specific disposal methods
  const disposalMethods = [
    { value: "incineration", label: "Incineration (Tissue/Biohazard)" },
    { value: "autoclaving", label: "Autoclaving (Before Disposal)" },
    { value: "chemical_neutralization", label: "Chemical Neutralization" },
    { value: "certified_waste", label: "Certified Medical Waste Disposal" },
    { value: "return_to_family", label: "Return to Patient/Family" },
    { value: "transfer_archive", label: "Transfer to Archive Facility" },
  ];

  // Load samples for this specific page
  const loadSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
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
              specimenType: sample.data?.specimenType,
              blockId: sample.data?.blockId,
              storageLocation: sample.data?.storageLocation,
              // Disposal-specific data
              disposalReason: sample.data?.disposalReason || "",
              disposalMethod: sample.data?.disposalMethod || "",
              disposalDate: sample.data?.disposalDate || "",
              disposedBy: sample.data?.disposedBy || "",
              archiveLocation: sample.data?.archiveLocation || "",
              isRetentionSample: sample.data?.isRetentionSample || false,
              isClosed: sample.data?.isClosed || false,
            }));

            // Fetch storage locations from the storage system for samples that don't have one
            const samplesWithoutStorage = transformedSamples.filter(
              (s) => !s.storageLocation,
            );

            if (samplesWithoutStorage.length > 0) {
              // Fetch storage locations in parallel
              const storagePromises = samplesWithoutStorage.map(
                (sample) =>
                  new Promise((resolve) => {
                    getFromOpenElisServer(
                      `/rest/storage/sample-items/${sample.id}`,
                      (storageResponse) => {
                        if (storageResponse && storageResponse.location) {
                          resolve({
                            id: sample.id,
                            storageLocation: storageResponse.location,
                          });
                        } else if (
                          storageResponse &&
                          storageResponse.hierarchicalPath
                        ) {
                          resolve({
                            id: sample.id,
                            storageLocation: storageResponse.hierarchicalPath,
                          });
                        } else {
                          resolve({ id: sample.id, storageLocation: null });
                        }
                      },
                    );
                  }),
              );

              Promise.all(storagePromises).then((storageResults) => {
                // Create a map of sample ID to storage location
                const storageMap = {};
                storageResults.forEach((result) => {
                  if (result.storageLocation) {
                    storageMap[result.id] = result.storageLocation;
                  }
                });

                // Update samples with storage locations
                const updatedSamples = transformedSamples.map((sample) => ({
                  ...sample,
                  storageLocation:
                    sample.storageLocation || storageMap[sample.id] || null,
                }));

                setSamples(updatedSamples);
                calculateSummary(updatedSamples);
                setLoading(false);
              });
            } else {
              setSamples(transformedSamples);
              calculateSummary(transformedSamples);
              setLoading(false);
            }
          } else {
            setSamples([]);
            setLoading(false);
          }
        }
      },
    );
  }, [pageData?.id]);

  // Calculate summary
  const calculateSummary = (sampleData) => {
    const total = sampleData.length;
    const disposed = sampleData.filter((s) => s.disposalDate).length;
    const archived = sampleData.filter((s) => s.archiveLocation).length;
    const retention = sampleData.filter((s) => s.isRetentionSample).length;
    const pending = total - disposed;

    setSummary({
      total,
      pendingDisposal: pending,
      disposed,
      archived,
      retentionSamples: retention,
    });
  };

  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples]);

  const handleApplyDisposalData = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.disposal.noSamplesSelected",
          defaultMessage: "Please select samples to apply disposal data",
        }),
      );
      return;
    }

    if (!disposalData.disposalReason || !disposalData.disposalMethod) {
      setError(
        intl.formatMessage({
          id: "pathology.disposal.reasonMethodRequired",
          defaultMessage: "Disposal reason and method are required",
        }),
      );
      return;
    }

    if (!disposalData.pathologistSignOff) {
      setError(
        intl.formatMessage({
          id: "pathology.disposal.pathologistRequired",
          defaultMessage: "Pathologist sign-off is required",
        }),
      );
      return;
    }

    if (!disposalData.confirmDisposal) {
      setError(
        intl.formatMessage({
          id: "pathology.disposal.confirmRequired",
          defaultMessage: "Please confirm the disposal action",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowDisposalModal(false);
      return;
    }

    // Use string IDs for composite sample IDs (e.g., "4_cassette_0_block_0_slide_0")
    const stringIds = selectedIds.map((id) => String(id));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply-string`,
      JSON.stringify({
        sampleIds: stringIds,
        data: {
          disposalReason: disposalData.disposalReason,
          disposalMethod: disposalData.disposalMethod,
          disposalDate: disposalData.disposalDate,
          disposedBy: disposalData.disposedBy,
          pathologistSignOff: disposalData.pathologistSignOff,
          qualityOfficerSignOff: disposalData.qualityOfficerSignOff,
          certificateNumber: disposalData.certificateNumber,
          archiveLocation: disposalData.archiveLocation,
          retentionEndDate: disposalData.retentionEndDate,
          notes: disposalData.notes,
          isClosed: true,
        },
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Mark samples as COMPLETED (closed) using string IDs endpoint
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status-string`,
              JSON.stringify({
                sampleIds: stringIds,
                status: "COMPLETED",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "pathology.disposal.dataSaved",
                      defaultMessage:
                        "Disposal data applied to {count} samples. Records locked and archived.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowDisposalModal(false);
                setSelectedIds([]);
                // Reset form
                setDisposalData({
                  disposalReason: "",
                  disposalMethod: "",
                  disposalDate: new Date().toISOString().split("T")[0],
                  disposedBy: "",
                  pathologistSignOff: "",
                  qualityOfficerSignOff: "",
                  certificateNumber: "",
                  archiveLocation: "",
                  retentionEndDate: "",
                  notes: "",
                  confirmDisposal: false,
                });
                loadSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to apply disposal data");
          }
        }
      },
    );
  };

  const handleMarkAsRetention = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.disposal.noSamplesSelected",
          defaultMessage: "Please select samples to mark as retention",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    // Use string IDs for composite sample IDs (e.g., "4_cassette_0_block_0_slide_0")
    const stringIds = selectedIds.map((id) => String(id));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply-string`,
      JSON.stringify({
        sampleIds: stringIds,
        data: {
          isRetentionSample: true,
        },
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Mark samples as COMPLETED after marking as retention using string IDs
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status-string`,
              JSON.stringify({
                sampleIds: stringIds,
                status: "COMPLETED",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "pathology.disposal.markedRetention",
                      defaultMessage:
                        "Marked {count} samples as retention samples",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setSelectedIds([]);
                loadSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to mark samples");
          }
        }
      },
    );
  };

  // Render page status tag with color coding (PENDING/IN_PROGRESS/COMPLETED)
  // SampleGrid render signature: (value, row) => JSX
  const renderStatusTag = (statusValue) => {
    const status = statusValue || "PENDING";
    switch (status) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm">
            Completed
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm">
            In Progress
          </Tag>
        );
      case "PENDING":
      default:
        return (
          <Tag type="gray" size="sm">
            Pending
          </Tag>
        );
    }
  };

  // Render disposal/retention status tag - this is the "Final Status" showing whether sample is disposed or retained
  // SampleGrid render signature: (value, row) => JSX - use row (second arg) for the full sample
  const renderFinalStatusTag = (value, sample) => {
    // If closed with disposal date = DISPOSED
    if (sample?.isClosed && sample?.disposalDate) {
      return (
        <Tag type="red" renderIcon={TrashCan} size="sm">
          Disposed
        </Tag>
      );
    }
    // If marked as retention sample = RETAINED
    if (sample?.isRetentionSample) {
      return (
        <Tag type="teal" renderIcon={Archive} size="sm">
          Retained
        </Tag>
      );
    }
    // If closed but no disposal date (just closed without disposal)
    if (sample?.isClosed) {
      return (
        <Tag type="purple" renderIcon={Locked} size="sm">
          Closed
        </Tag>
      );
    }
    // Active/Pending disposal
    return (
      <Tag type="gray" size="sm">
        Pending
      </Tag>
    );
  };

  // Render storage location - show from previous workflow pages
  const renderStorageLocation = (value, sample) => {
    const location = sample?.storageLocation || value;
    if (!location) {
      return <span style={{ color: "#8d8d8d" }}>—</span>;
    }
    return (
      <Tag type="blue" size="sm">
        {location}
      </Tag>
    );
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="pathology-disposal-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pathology.disposal.title"
            defaultMessage="Disposal &amp; Archiving"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pathology.disposal.description"
            defaultMessage="Close sample lifecycle compliantly. Dispose samples based on criteria, archive records per regulatory requirements."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {/* Summary Tiles */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.disposal.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{summary.total}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.disposal.pendingDisposal"
                  defaultMessage="Pending Disposal"
                />
              </span>
              <span className="progress-value">{summary.pendingDisposal}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.disposal.disposed"
                  defaultMessage="Disposed"
                />
              </span>
              <span className="progress-value">{summary.disposed}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.disposal.archived"
                  defaultMessage="Archived"
                />
              </span>
              <span className="progress-value">{summary.archived}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.disposal.retention"
                  defaultMessage="Retention Samples"
                />
              </span>
              <span className="progress-value">{summary.retentionSamples}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="danger"
          size="sm"
          renderIcon={TrashCan}
          onClick={() => setShowDisposalModal(true)}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="pathology.disposal.disposeSelected"
            defaultMessage="Dispose Selected ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Archive}
          onClick={handleMarkAsRetention}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="pathology.disposal.markRetention"
            defaultMessage="Mark as Retention"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={loadSamples}
        >
          <FormattedMessage
            id="pathology.disposal.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="pathology-disposal"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "specimenType", header: "Specimen Type" },
            { key: "blockId", header: "Block ID" },
            {
              key: "storageLocation",
              header: intl.formatMessage({
                id: "pathology.disposal.storageLocation",
                defaultMessage: "Storage Location",
              }),
              render: renderStorageLocation,
            },
            {
              key: "status",
              header: "Page Status",
              render: renderStatusTag,
            },
          ]}
          additionalColumns={[
            {
              key: "finalStatus",
              header: intl.formatMessage({
                id: "pathology.disposal.finalStatus",
                defaultMessage: "Final Status",
              }),
              render: renderFinalStatusTag,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.pathology.disposal.empty"
              defaultMessage="No samples available for disposal. Complete Reporting & Validation first."
            />
          </p>
        </div>
      )}

      {/* Disposal Modal */}
      <Modal
        open={showDisposalModal}
        onRequestClose={() => setShowDisposalModal(false)}
        onRequestSubmit={handleApplyDisposalData}
        modalHeading={intl.formatMessage({
          id: "pathology.disposal.modalTitle",
          defaultMessage: "Dispose & Archive Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.disposal.confirm",
          defaultMessage: "Confirm Disposal",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "pathology.disposal.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "pathology.disposal.warning",
              defaultMessage: "Warning: This action is irreversible",
            })}
            subtitle={intl.formatMessage(
              {
                id: "pathology.disposal.warningSubtitle",
                defaultMessage:
                  "You are about to dispose {count} samples. Records will be locked and archived.",
              },
              { count: selectedIds.length },
            )}
            hideCloseButton
            lowContrast
          />

          {/* Disposal Reason and Method */}
          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="disposal-reason"
                labelText={intl.formatMessage({
                  id: "pathology.disposal.reason",
                  defaultMessage: "Disposal Reason",
                })}
                value={disposalData.disposalReason}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    disposalReason: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select reason..." />
                {disposalReasons.map((reason) => (
                  <SelectItem
                    key={reason.value}
                    value={reason.value}
                    text={reason.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="disposal-method"
                labelText={intl.formatMessage({
                  id: "pathology.disposal.method",
                  defaultMessage: "Disposal Method",
                })}
                value={disposalData.disposalMethod}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    disposalMethod: e.target.value,
                  })
                }
              >
                <SelectItem value="" text="Select method..." />
                {disposalMethods.map((method) => (
                  <SelectItem
                    key={method.value}
                    value={method.value}
                    text={method.label}
                  />
                ))}
              </Select>
            </Column>
          </Grid>

          {/* Date and Person */}
          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setDisposalData({
                    ...disposalData,
                    disposalDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="disposal-date"
                  labelText={intl.formatMessage({
                    id: "pathology.disposal.date",
                    defaultMessage: "Disposal Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="disposed-by"
                labelText={intl.formatMessage({
                  id: "pathology.disposal.disposedBy",
                  defaultMessage: "Disposed By",
                })}
                value={disposalData.disposedBy}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    disposedBy: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          {/* Sign-offs (Required) */}
          <h5>
            <FormattedMessage
              id="pathology.disposal.signOffs"
              defaultMessage="Required Sign-Offs"
            />
          </h5>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="pathologist-signoff"
                labelText={intl.formatMessage({
                  id: "pathology.disposal.pathologist",
                  defaultMessage: "Pathologist *",
                })}
                value={disposalData.pathologistSignOff}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    pathologistSignOff: e.target.value,
                  })
                }
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="quality-signoff"
                labelText={intl.formatMessage({
                  id: "pathology.disposal.qualityOfficer",
                  defaultMessage: "Quality Officer (if applicable)",
                })}
                value={disposalData.qualityOfficerSignOff}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    qualityOfficerSignOff: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          {/* Archive Information */}
          <h5>
            <FormattedMessage
              id="pathology.disposal.archiving"
              defaultMessage="Archiving Information"
            />
          </h5>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="certificate-number"
                labelText={intl.formatMessage({
                  id: "pathology.disposal.certificate",
                  defaultMessage: "Disposal Certificate Number",
                })}
                value={disposalData.certificateNumber}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    certificateNumber: e.target.value,
                  })
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="archive-location"
                labelText={intl.formatMessage({
                  id: "pathology.disposal.archiveLocation",
                  defaultMessage: "Archive Location",
                })}
                value={disposalData.archiveLocation}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    archiveLocation: e.target.value,
                  })
                }
                placeholder="e.g., Archive Room A, Box 15"
              />
            </Column>
          </Grid>

          <TextArea
            id="notes"
            labelText={intl.formatMessage({
              id: "pathology.disposal.notes",
              defaultMessage: "Notes / Comments",
            })}
            value={disposalData.notes}
            onChange={(e) =>
              setDisposalData({ ...disposalData, notes: e.target.value })
            }
            rows={2}
          />

          {/* Confirmation Checkbox */}
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#fff1f1",
              borderRadius: "4px",
            }}
          >
            <Checkbox
              id="confirm-disposal"
              labelText={intl.formatMessage({
                id: "pathology.disposal.confirmCheckbox",
                defaultMessage:
                  "I confirm that all data has been archived and these samples can be permanently disposed. This action cannot be undone.",
              })}
              checked={disposalData.confirmDisposal}
              onChange={(_, { checked }) =>
                setDisposalData({ ...disposalData, confirmDisposal: checked })
              }
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default PathologyDisposalArchivingPage;
