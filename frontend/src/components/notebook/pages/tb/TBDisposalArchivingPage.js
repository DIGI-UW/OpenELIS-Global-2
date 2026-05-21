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
  Modal,
  Tag,
  Loading,
  Checkbox,
} from "@carbon/react";
import { TrashCan, Archive, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBDisposalArchivingPage - Page 8: Disposal & Archiving
 *
 * Allows technicians to:
 * - Dispose samples based on TB-specific criteria (culture negative, testing complete, contaminated)
 * - Apply TB-specific disposal methods (autoclave, incineration, chemical)
 * - Transfer samples to biorepository for long-term archiving
 * - Maintain complete audit trail of disposal actions
 *
 * Sample sources:
 * - Storage page (Page 6): Stored isolates
 * - Incubation page (Page 4): Completed cultures with final result
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID (optional)
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TBDisposalArchivingPage({
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

  // Split samples into pending and disposed groups
  const pendingSamples = samples.filter(
    (s) => s.disposalStatus !== "DISPOSED" && s.disposalStatus !== "ARCHIVED",
  );
  const disposedSamples = samples.filter(
    (s) => s.disposalStatus === "DISPOSED" || s.disposalStatus === "ARCHIVED",
  );

  // Disposal modal state
  const [showDisposalModal, setShowDisposalModal] = useState(false);
  const [disposalData, setDisposalData] = useState({
    disposalReason: "",
    disposalMethod: "",
    disposedBy: "",
    notes: "",
    confirmDisposal: false,
  });

  // Archive modal state
  const [showArchiveModal, setShowArchiveModal] = useState(false);
  const [archiveData, setArchiveData] = useState({
    notes: "",
  });

  // Summary counts
  const [summary, setSummary] = useState({
    total: 0,
    pendingDisposal: 0,
    disposed: 0,
    archived: 0,
  });

  // TB-specific disposal reasons (matching TbEnums.DisposalReason)
  const disposalReasons = [
    { value: "CULTURE_NEGATIVE", label: "Culture Negative (8 weeks)" },
    { value: "TESTING_COMPLETE", label: "Testing Complete" },
    { value: "CONTAMINATED", label: "Contaminated" },
    { value: "DEGRADED", label: "Sample Degraded" },
    { value: "STORAGE_LIMIT", label: "Storage Capacity Limit" },
    { value: "DUPLICATE", label: "Duplicate Sample" },
    { value: "EXPIRED", label: "Expired" },
    { value: "OTHER", label: "Other (specify in notes)" },
  ];

  // TB-specific disposal methods (matching TbEnums.DisposalMethod)
  const disposalMethods = [
    { value: "AUTOCLAVE", label: "Biohazard Autoclave" },
    { value: "INCINERATION", label: "Incineration" },
    { value: "CHEMICAL", label: "Chemical Neutralization" },
    { value: "BIOREPOSITORY", label: "Transfer to Biorepository" },
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
            const transformedSamples = response.map((sample) => {
              // Build storage location from storage fields
              const storagePath = sample.data?.storagePath || null;
              const storageWell = sample.data?.storageWell || null;
              const storageBox = sample.data?.storageBox || null;
              const storageLocation = storagePath
                ? storageWell
                  ? `${storageBox || "Box"} - ${storageWell}`
                  : storagePath
                : null;

              // Infer source page from data fields
              const hasStorageData = !!(storagePath || storageWell);
              const sourcePageOrder =
                sample.data?.sourcePageOrder || (hasStorageData ? 6 : null);

              return {
                id: String(sample.id || sample.sampleItemId),
                sampleItemId: sample.sampleItemId, // For storage API calls
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType: sample.sampleType || "-",
                collectionDate: sample.collectionDate,
                status: sample.pageStatus || "PENDING",
                specimenType: sample.sampleType || "-",
                // Source tracking
                sourcePageOrder: sourcePageOrder,
                sourceSampleStatus: sample.data?.sourceSampleStatus || null,
                storageLocation: storageLocation,
                hasStorageAssignment: hasStorageData, // Track if sample has storage
                // Disposal-specific data
                disposalStatus: sample.data?.disposalStatus || "PENDING",
                disposalReason: sample.data?.disposalReason || "",
                disposalMethod: sample.data?.disposalMethod || "",
                disposalDate: sample.data?.disposalDate || "",
                disposedBy: sample.data?.disposedBy || "",
                archiveLocation: sample.data?.archiveLocation || "",
                notes: sample.data?.notes || "",
              };
            });

            setSamples(transformedSamples);
            calculateSummary(transformedSamples);
          } else {
            setSamples([]);
            calculateSummary([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Calculate summary
  const calculateSummary = (sampleData) => {
    const total = sampleData.length;
    const disposed = sampleData.filter(
      (s) => s.disposalStatus === "DISPOSED",
    ).length;
    const archived = sampleData.filter(
      (s) => s.disposalStatus === "ARCHIVED",
    ).length;
    const pending = total - disposed - archived;

    setSummary({
      total,
      pendingDisposal: pending,
      disposed,
      archived,
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

  // Handle dispose samples
  const handleDisposeSamples = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "tb.disposal.noSamplesSelected",
          defaultMessage: "Please select samples to dispose",
        }),
      );
      return;
    }

    if (!disposalData.disposalReason || !disposalData.disposalMethod) {
      setError(
        intl.formatMessage({
          id: "tb.disposal.reasonMethodRequired",
          defaultMessage: "Disposal reason and method are required",
        }),
      );
      return;
    }

    if (!disposalData.confirmDisposal) {
      setError(
        intl.formatMessage({
          id: "tb.disposal.confirmRequired",
          defaultMessage: "Please confirm the disposal action",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowDisposalModal(false);
      return;
    }

    const sampleIds = selectedIds.map((id) => parseInt(id, 10));

    // Get selected samples to check for storage assignments
    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const samplesWithStorage = selectedSamples.filter(
      (s) => s.hasStorageAssignment && s.sampleItemId,
    );

    // Map disposal reason to human-readable format for storage API
    const reasonLabel =
      disposalReasons.find((r) => r.value === disposalData.disposalReason)
        ?.label || disposalData.disposalReason;
    const methodLabel =
      disposalMethods.find((m) => m.value === disposalData.disposalMethod)
        ?.label || disposalData.disposalMethod;

    // Function to complete notebook page updates after storage disposal
    const completeNotebookDisposal = () => {
      postToOpenElisServerJsonResponse(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: sampleIds,
          data: {
            disposalStatus: "DISPOSED",
            disposalReason: disposalData.disposalReason,
            disposalMethod: disposalData.disposalMethod,
            disposalDate: new Date().toISOString().split("T")[0],
            disposedBy: disposalData.disposedBy,
            notes: disposalData.notes,
          },
        }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              // Mark samples as COMPLETED
              postToOpenElisServerJsonResponse(
                `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                JSON.stringify({
                  sampleIds: sampleIds,
                  status: "COMPLETED",
                }),
                () => {
                  setSuccess(
                    intl.formatMessage(
                      {
                        id: "tb.disposal.disposeSuccess",
                        defaultMessage:
                          "Successfully disposed {count} samples. Records archived.",
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
                    disposedBy: "",
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
              setError(response?.error || "Failed to dispose samples");
            }
          }
        },
      );
    };

    // If there are samples with storage assignments, dispose them from storage first
    if (samplesWithStorage.length > 0) {
      let storageDisposalCount = 0;
      let storageDisposalErrors = [];

      console.log("Gonna dispose samples", { samplesWithStorage });
      // Dispose each sample from storage
      samplesWithStorage.forEach((sample) => {
        postToOpenElisServerJsonResponse(
          `/rest/storage/sample-items/dispose`,
          JSON.stringify({
            sampleItemId: String(sample.sampleItemId),
            reason: reasonLabel,
            method: methodLabel,
            notes: disposalData.notes || "",
          }),
          (response) => {
            storageDisposalCount++;

            if (response && response.error) {
              storageDisposalErrors.push(
                `${sample.externalId}: ${response.error}`,
              );
            }

            // When all storage disposals are done, proceed with notebook updates
            if (storageDisposalCount === samplesWithStorage.length) {
              if (storageDisposalErrors.length > 0) {
                console.warn(
                  "Some storage disposal errors:",
                  storageDisposalErrors,
                );
              }
              // Continue with notebook disposal regardless of storage errors
              // (storage might already be disposed, or sample might not be in storage system)
              completeNotebookDisposal();
            }
          },
        );
      });
    } else {
      // No samples with storage, proceed directly to notebook disposal
      completeNotebookDisposal();
    }
  };

  // Handle archive samples (transfer to biorepository)
  const handleArchiveSamples = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "tb.disposal.noSamplesSelected",
          defaultMessage: "Please select samples to archive",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowArchiveModal(false);
      return;
    }

    // Get selected samples to extract sampleItemIds for biorepository transfer
    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const sampleItemIds = selectedSamples
      .filter((s) => s.sampleItemId)
      .map((s) => parseInt(s.sampleItemId, 10));

    if (sampleItemIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "tb.disposal.noValidSamples",
          defaultMessage: "No valid samples to transfer to biorepository",
        }),
      );
      return;
    }

    const sampleIds = selectedIds.map((id) => parseInt(id, 10));

    // Step 1: Create biorepository transfer request (like MedLab pattern)
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/transfer`,
      JSON.stringify({
        sourceLab: "TB_LAB",
        sampleItemIds: sampleItemIds,
        requestNotes:
          archiveData.notes ||
          `Transfer from TB Lab - Notebook Entry ${entryId || ""}`,
      }),
      (transferResponse) => {
        if (componentMounted.current) {
          if (
            transferResponse &&
            !transferResponse.error &&
            transferResponse.id
          ) {
            // Step 2: Update notebook page sample data with transfer info
            postToOpenElisServerJsonResponse(
              `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
              JSON.stringify({
                sampleIds: sampleIds,
                data: {
                  disposalStatus: "ARCHIVED",
                  disposalMethod: "BIOREPOSITORY",
                  archiveDate: new Date().toISOString().split("T")[0],
                  notes: archiveData.notes,
                  // Store biorepository transfer reference
                  biorepositoryTransferId: transferResponse.id,
                  biorepositoryTransferStatus:
                    transferResponse.status || "PENDING",
                },
              }),
              (applyResponse) => {
                if (componentMounted.current) {
                  if (applyResponse && !applyResponse.error) {
                    // Step 3: Mark samples as COMPLETED on this page
                    postToOpenElisServerJsonResponse(
                      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                      JSON.stringify({
                        sampleIds: sampleIds,
                        status: "COMPLETED",
                      }),
                      () => {
                        setSuccess(
                          intl.formatMessage(
                            {
                              id: "tb.disposal.archiveSuccess",
                              defaultMessage:
                                "Successfully transferred {count} samples to biorepository. Transfer ID: {transferId}",
                            },
                            {
                              count: selectedIds.length,
                              transferId: transferResponse.id,
                            },
                          ),
                        );
                        setShowArchiveModal(false);
                        setSelectedIds([]);
                        // Reset form
                        setArchiveData({
                          notes: "",
                        });
                        loadSamples();
                        if (onProgressUpdate) {
                          onProgressUpdate();
                        }
                      },
                    );
                  } else {
                    setError(
                      applyResponse?.error || "Failed to update sample data",
                    );
                  }
                }
              },
            );
          } else {
            // Biorepository transfer failed
            setError(
              transferResponse?.error ||
                intl.formatMessage({
                  id: "tb.disposal.biorepositoryTransferFailed",
                  defaultMessage:
                    "Failed to create biorepository transfer request. Please try again.",
                }),
            );
          }
        }
      },
    );
  };

  // Render disposal status tag
  const renderDisposalStatusTag = (value, sample) => {
    const status = sample?.disposalStatus || "PENDING";

    switch (status) {
      case "DISPOSED":
        return (
          <Tag type="red" renderIcon={TrashCan} size="sm">
            <FormattedMessage
              id="tb.disposal.status.disposed"
              defaultMessage="Disposed"
            />
          </Tag>
        );
      case "ARCHIVED":
        return (
          <Tag type="teal" renderIcon={Archive} size="sm">
            <FormattedMessage
              id="tb.disposal.status.archived"
              defaultMessage="Archived"
            />
          </Tag>
        );
      case "PENDING":
      default:
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="tb.disposal.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  // Render source tag (where the sample came from)
  const renderSourceTag = (value, sample) => {
    const sourceOrder = sample?.sourcePageOrder;

    if (sourceOrder === 4) {
      return (
        <Tag type="purple" size="sm">
          <FormattedMessage
            id="tb.disposal.source.incubation"
            defaultMessage="Incubation"
          />
        </Tag>
      );
    }
    if (sourceOrder === 6) {
      return (
        <Tag type="cyan" size="sm">
          <FormattedMessage
            id="tb.disposal.source.storage"
            defaultMessage="Storage"
          />
        </Tag>
      );
    }
    return <span style={{ color: "#8d8d8d" }}>-</span>;
  };

  // Render page status tag
  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="tb-disposal-archiving-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tb.disposal.title"
            defaultMessage="Disposal &amp; Archiving"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.disposal.description"
            defaultMessage="Dispose of completed samples or transfer to biorepository for long-term archiving. Track disposal reasons, methods, and maintain complete audit trail."
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
                  id="tb.disposal.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{summary.total}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="tb.disposal.pendingDisposal"
                  defaultMessage="Pending Disposal"
                />
              </span>
              <span className="progress-value">{summary.pendingDisposal}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="tb.disposal.disposed"
                  defaultMessage="Disposed"
                />
              </span>
              <span className="progress-value">{summary.disposed}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="tb.disposal.archived"
                  defaultMessage="Archived"
                />
              </span>
              <span className="progress-value">{summary.archived}</span>
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
            id="tb.disposal.disposeSelected"
            defaultMessage="Dispose Selected ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Archive}
          onClick={() => setShowArchiveModal(true)}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="tb.disposal.archiveSelected"
            defaultMessage="Archive to Biorepository"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={loadSamples}
        >
          <FormattedMessage id="tb.disposal.refresh" defaultMessage="Refresh" />
        </Button>
      </div>

      {/* Pending Samples Grid */}
      {pendingSamples.length > 0 && (
        <div className="sample-grid-container">
          <h5 style={{ marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="tb.disposal.pendingSamples"
              defaultMessage="Samples Pending Disposal ({count})"
              values={{ count: pendingSamples.length }}
            />
          </h5>
          <SampleGrid
            gridId="tb-disposal-pending"
            samples={pendingSamples}
            selectedIds={selectedIds}
            onSelectionChange={setSelectedIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            columns={[
              { key: "externalId", header: "Sample ID" },
              { key: "specimenType", header: "Specimen Type" },
              {
                key: "source",
                header: intl.formatMessage({
                  id: "tb.disposal.source",
                  defaultMessage: "Source",
                }),
                render: renderSourceTag,
              },
              {
                key: "storageLocation",
                header: intl.formatMessage({
                  id: "tb.disposal.storageLocation",
                  defaultMessage: "Storage Location",
                }),
                render: (value) =>
                  value ? (
                    <Tag type="blue" size="sm">
                      {value}
                    </Tag>
                  ) : (
                    <span style={{ color: "#8d8d8d" }}>-</span>
                  ),
              },
              {
                key: "disposalStatus",
                header: intl.formatMessage({
                  id: "tb.disposal.disposalStatus",
                  defaultMessage: "Disposal Status",
                }),
                render: renderDisposalStatusTag,
              },
            ]}
          />
        </div>
      )}

      {/* Empty state for pending samples */}
      {!loading &&
        pendingSamples.length === 0 &&
        disposedSamples.length === 0 && (
          <div className="empty-state">
            <Tile style={{ padding: "2rem", textAlign: "center" }}>
              <Archive
                size={48}
                style={{ marginBottom: "1rem", opacity: 0.5 }}
              />
              <p style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.page.tb.disposal.empty"
                  defaultMessage="No samples available for disposal. Complete Storage and Incubation pages first to route samples here."
                />
              </p>
            </Tile>
          </div>
        )}

      {/* Disposed/Archived Samples Grid */}
      {disposedSamples.length > 0 && (
        <div className="sample-grid-container" style={{ marginTop: "2rem" }}>
          <h5 style={{ marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="tb.disposal.disposedSamples"
              defaultMessage="Disposed/Archived Samples ({count})"
              values={{ count: disposedSamples.length }}
            />
          </h5>
          <SampleGrid
            gridId="tb-disposal-completed"
            samples={disposedSamples}
            showSelection={false}
            loading={loading}
            columns={[
              { key: "externalId", header: "Sample ID" },
              { key: "specimenType", header: "Specimen Type" },
              {
                key: "disposalStatus",
                header: intl.formatMessage({
                  id: "tb.disposal.disposalStatus",
                  defaultMessage: "Status",
                }),
                render: renderDisposalStatusTag,
              },
              {
                key: "disposalReason",
                header: intl.formatMessage({
                  id: "tb.disposal.reason",
                  defaultMessage: "Reason",
                }),
                render: (value) => {
                  const reason = disposalReasons.find((r) => r.value === value);
                  return reason ? reason.label : value || "-";
                },
              },
              {
                key: "disposalMethod",
                header: intl.formatMessage({
                  id: "tb.disposal.method",
                  defaultMessage: "Method",
                }),
                render: (value) => {
                  const method = disposalMethods.find((m) => m.value === value);
                  return method ? method.label : value || "-";
                },
              },
              {
                key: "disposalDate",
                header: intl.formatMessage({
                  id: "tb.disposal.date",
                  defaultMessage: "Date",
                }),
                render: (value) => value || "-",
              },
              {
                key: "disposedBy",
                header: intl.formatMessage({
                  id: "tb.disposal.disposedBy",
                  defaultMessage: "Disposed By",
                }),
                render: (value) => value || "-",
              },
            ]}
          />
        </div>
      )}

      {/* Disposal Modal */}
      <Modal
        open={showDisposalModal}
        onRequestClose={() => setShowDisposalModal(false)}
        onRequestSubmit={handleDisposeSamples}
        modalHeading={intl.formatMessage({
          id: "tb.disposal.modalTitle",
          defaultMessage: "Dispose TB Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "tb.disposal.confirm",
          defaultMessage: "Confirm Disposal",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "tb.disposal.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "tb.disposal.warning",
              defaultMessage: "Warning: This action is irreversible",
            })}
            subtitle={intl.formatMessage(
              {
                id: "tb.disposal.warningSubtitle",
                defaultMessage:
                  "You are about to dispose {count} TB samples. Records will be permanently archived.",
              },
              { count: selectedIds.length },
            )}
            hideCloseButton
            lowContrast
          />

          {/* Disposal Reason */}
          <Select
            id="disposal-reason"
            labelText={intl.formatMessage({
              id: "tb.disposal.reason",
              defaultMessage: "Disposal Reason *",
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

          {/* Disposal Method */}
          <Select
            id="disposal-method"
            labelText={intl.formatMessage({
              id: "tb.disposal.method",
              defaultMessage: "Disposal Method *",
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

          {/* Disposed By */}
          <TextInput
            id="disposed-by"
            labelText={intl.formatMessage({
              id: "tb.disposal.disposedBy",
              defaultMessage: "Disposed By",
            })}
            value={disposalData.disposedBy}
            onChange={(e) =>
              setDisposalData({
                ...disposalData,
                disposedBy: e.target.value,
              })
            }
            placeholder="Enter staff name"
          />

          {/* Notes */}
          <TextArea
            id="disposal-notes"
            labelText={intl.formatMessage({
              id: "tb.disposal.notes",
              defaultMessage: "Notes / Comments",
            })}
            value={disposalData.notes}
            onChange={(e) =>
              setDisposalData({ ...disposalData, notes: e.target.value })
            }
            rows={2}
            placeholder="Additional notes (required if 'Other' reason selected)"
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
                id: "tb.disposal.confirmCheckbox",
                defaultMessage:
                  "I confirm that these TB samples can be permanently disposed using the selected method. This action cannot be undone.",
              })}
              checked={disposalData.confirmDisposal}
              onChange={(_, { checked }) =>
                setDisposalData({ ...disposalData, confirmDisposal: checked })
              }
            />
          </div>
        </div>
      </Modal>

      {/* Archive Modal */}
      <Modal
        open={showArchiveModal}
        onRequestClose={() => setShowArchiveModal(false)}
        onRequestSubmit={handleArchiveSamples}
        modalHeading={intl.formatMessage({
          id: "tb.disposal.archiveModalTitle",
          defaultMessage: "Archive to Biorepository",
        })}
        primaryButtonText={intl.formatMessage({
          id: "tb.disposal.archiveConfirm",
          defaultMessage: "Confirm Archive",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "tb.disposal.cancel",
          defaultMessage: "Cancel",
        })}
        size="sm"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="tb.disposal.archiveDescription"
              defaultMessage="Transfer {count} samples to biorepository for long-term storage."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Notes */}
          <TextArea
            id="archive-notes"
            labelText={intl.formatMessage({
              id: "tb.disposal.notes",
              defaultMessage: "Notes / Comments",
            })}
            value={archiveData.notes}
            onChange={(e) =>
              setArchiveData({ ...archiveData, notes: e.target.value })
            }
            rows={2}
          />
        </div>
      </Modal>
    </div>
  );
}

export default TBDisposalArchivingPage;
