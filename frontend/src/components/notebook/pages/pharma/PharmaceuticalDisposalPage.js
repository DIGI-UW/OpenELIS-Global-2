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
import { TrashCan, Archive, Renew, Warning, Locked } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PharmaceuticalDisposalPage - Page 7: Disposal & Archiving
 *
 * Allows technicians to:
 * - Dispose samples based on criteria (expiry, exhaustion, failed QC, safety)
 * - Apply material-specific disposal methods
 * - Archive raw data, final reports, and lineage
 * - Mark samples as closed with supervisor sign-off
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID for fetching storage routing data
 */
function PharmaceuticalDisposalPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
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
    supervisorSignOff: "",
    biosafetyOfficerSignOff: "",
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

  // Disposal reason options
  const disposalReasons = [
    { value: "expiry", label: "Expiry - Past retest/expiry date" },
    { value: "exhaustion", label: "Exhaustion - Sample depleted" },
    { value: "failed_qc", label: "Failed QC - Cannot be used" },
    { value: "safety", label: "Safety Concerns - Contamination/hazard" },
    { value: "study_complete", label: "Study Complete - No longer needed" },
    { value: "retention_expired", label: "Retention Period Expired" },
    { value: "other", label: "Other" },
  ];

  // Disposal method options (material-specific)
  const disposalMethods = [
    {
      value: "incineration",
      label: "High-temperature Incineration (Pharmaceuticals)",
    },
    { value: "autoclaving", label: "Autoclaving (Biologicals)" },
    { value: "chemical_neutralization", label: "Chemical Neutralization" },
    { value: "certified_waste", label: "Certified Waste Disposal" },
    { value: "return_sponsor", label: "Return to Sponsor" },
    { value: "transfer_archive", label: "Transfer to Archive Facility" },
  ];

  // Load samples for this specific page, also fetching storage data from Storage page (page 5)
  const loadSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    let pageSamples = [];
    let storagePageSamples = [];

    const finishProcessing = () => {
      if (componentMounted.current) {
        // Build storage map from Storage page samples
        const storageMap = {};
        storagePageSamples.forEach((sample) => {
          const sampleId = String(sample.id || sample.sampleItemId);
          if (sample.data) {
            storageMap[sampleId] = {
              storageRoom: sample.data.storageRoom,
              storageFreezer: sample.data.storageFreezer,
              storageRack: sample.data.storageRack,
              storageBox: sample.data.storageBox,
              storageWell:
                sample.data.storageWell || sample.data.wellCoordinate,
              storagePath: sample.data.storagePath,
              storageLocation: sample.data.storageLocation,
            };
          }
        });

        const transformedSamples = pageSamples.map((sample) => {
          const sampleId = String(sample.id || sample.sampleItemId);
          const storageData = storageMap[sampleId];

          // Merge storage data: prefer page data, fallback to Storage page data
          const storageRoom =
            sample.data?.storageRoom || storageData?.storageRoom || null;
          const storageRack =
            sample.data?.storageRack || storageData?.storageRack || null;
          const storageBox =
            sample.data?.storageBox || storageData?.storageBox || null;
          const storageWell =
            sample.data?.storageWell ||
            sample.data?.wellCoordinate ||
            storageData?.storageWell ||
            null;
          const storagePath =
            sample.data?.storagePath || storageData?.storagePath || null;
          const storageFreezer =
            sample.data?.storageFreezer || storageData?.storageFreezer || null;

          return {
            id: sampleId,
            externalId: sample.externalId,
            accessionNumber: sample.accessionNumber,
            sampleType: sample.sampleType || sample.typeOfSample?.description,
            collectionDate: sample.collectionDate,
            status: sample.pageStatus || "PENDING",
            sampleCategory: sample.data?.sampleCategory,
            lotNumber: sample.data?.lotNumber,
            expiryDate: sample.data?.expiryOrRetestDate,
            storageLocation:
              sample.data?.storageLocation || storageData?.storageLocation,
            // Storage assignment data (merged from page and Storage page data)
            storagePath: storagePath,
            storageBox: storageBox,
            storageWell: storageWell,
            storageRoom: storageRoom,
            storageRack: storageRack,
            storageFreezer: storageFreezer,
            // Disposal-specific data
            disposalReason: sample.data?.disposalReason || "",
            disposalMethod: sample.data?.disposalMethod || "",
            disposalDate: sample.data?.disposalDate || "",
            disposedBy: sample.data?.disposedBy || "",
            archiveLocation: sample.data?.archiveLocation || "",
            isRetentionSample: sample.data?.isRetentionSample || false,
            isClosed: sample.data?.isClosed || false,
          };
        });
        setSamples(transformedSamples);
        calculateSummary(transformedSamples);
        setLoading(false);
      }
    };

    // Load this page's samples first
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && Array.isArray(response)) {
          pageSamples = response;
        }

        // Now fetch notebook pages to find the Storage page (order 5)
        if (notebookId) {
          getFromOpenElisServer(
            `/rest/notebook/view/${notebookId}`,
            (nbResponse) => {
              if (nbResponse && nbResponse.pages) {
                // Find Storage page (order 5)
                const storagePage = nbResponse.pages.find(
                  (p) => (p.pageOrder || p.order) === 5,
                );
                if (storagePage && storagePage.id) {
                  getFromOpenElisServer(
                    `/rest/notebook/page/${storagePage.id}/samples`,
                    (storageResponse) => {
                      if (storageResponse && Array.isArray(storageResponse)) {
                        storagePageSamples = storageResponse;
                      }
                      finishProcessing();
                    },
                  );
                } else {
                  finishProcessing();
                }
              } else {
                finishProcessing();
              }
            },
          );
        } else {
          finishProcessing();
        }
      },
    );
  }, [pageData?.id, notebookId]);

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
          id: "notebook.pharma.disposal.noSamplesSelected",
          defaultMessage: "Please select samples to apply disposal data",
        }),
      );
      return;
    }

    if (!disposalData.disposalReason || !disposalData.disposalMethod) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.disposal.reasonMethodRequired",
          defaultMessage: "Disposal reason and method are required",
        }),
      );
      return;
    }

    if (!disposalData.supervisorSignOff) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.disposal.supervisorRequired",
          defaultMessage: "Supervisor sign-off is required",
        }),
      );
      return;
    }

    if (!disposalData.confirmDisposal) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.disposal.confirmRequired",
          defaultMessage: "Please confirm the disposal action",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowDisposalModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: {
          disposalReason: disposalData.disposalReason,
          disposalMethod: disposalData.disposalMethod,
          disposalDate: disposalData.disposalDate,
          disposedBy: disposalData.disposedBy,
          supervisorSignOff: disposalData.supervisorSignOff,
          biosafetyOfficerSignOff: disposalData.biosafetyOfficerSignOff,
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
            // Mark samples as COMPLETED (closed)
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: "COMPLETED",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.pharma.disposal.dataSaved",
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
                  supervisorSignOff: "",
                  biosafetyOfficerSignOff: "",
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

  // Simply flag samples as retention (storage is assigned on the Storage page)
  const handleMarkAsRetention = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.disposal.noSamplesSelected",
          defaultMessage: "Please select samples to mark as retention",
        }),
      );
      return;
    }

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.disposal.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setError(null);
    setSuccess(null);

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Simply mark samples as retention samples
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: {
          isRetentionSample: true,
        },
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.pharma.disposal.retentionMarked",
                  defaultMessage:
                    "Marked {count} sample(s) as retention. Assign storage on the Storage & Inventory page.",
                },
                { count: selectedIds.length },
              ),
            );
            setSelectedIds([]);
            loadSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to mark samples as retention");
          }
        }
      },
    );
  }, [selectedIds, pageData?.id, intl, loadSamples, onProgressUpdate]);

  // Render disposal status tag
  const renderDisposalTag = (sample) => {
    // Disposed takes priority - show if sample has been disposed
    if (sample.disposalDate) {
      return (
        <Tag type="red" renderIcon={TrashCan} size="sm">
          Disposed
        </Tag>
      );
    }
    // Retention samples (stored for future reference)
    if (sample.isRetentionSample) {
      return (
        <Tag type="cyan" renderIcon={Archive} size="sm">
          Retained
        </Tag>
      );
    }
    // Closed but not disposed or retained
    if (sample.isClosed) {
      return (
        <Tag type="purple" renderIcon={Locked} size="sm">
          Closed
        </Tag>
      );
    }
    return (
      <Tag type="gray" size="sm">
        Active
      </Tag>
    );
  };

  // Render expiry status
  const renderExpiryStatus = (sample) => {
    if (!sample.expiryDate) return <span style={{ color: "#8d8d8d" }}>-</span>;

    const expiryDate = new Date(sample.expiryDate);
    const today = new Date();
    const daysUntilExpiry = Math.ceil(
      (expiryDate - today) / (1000 * 60 * 60 * 24),
    );

    if (daysUntilExpiry < 0) {
      return (
        <Tag type="red" renderIcon={Warning} size="sm">
          Expired
        </Tag>
      );
    }
    if (daysUntilExpiry <= 30) {
      return (
        <Tag type="magenta" size="sm">
          Expires in {daysUntilExpiry}d
        </Tag>
      );
    }
    if (daysUntilExpiry <= 90) {
      return (
        <Tag type="teal" size="sm">
          {sample.expiryDate}
        </Tag>
      );
    }
    return <span>{sample.expiryDate}</span>;
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="pharma-disposal-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.disposal.title"
            defaultMessage="Disposal &amp; Archiving"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.disposal.description"
            defaultMessage="Close sample lifecycle compliantly. Dispose samples based on criteria, archive records for 5-10 years per GxP/GMP requirements."
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
                  id="notebook.pharma.disposal.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{summary.total}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.disposal.pendingDisposal"
                  defaultMessage="Pending Disposal"
                />
              </span>
              <span className="progress-value">{summary.pendingDisposal}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.disposal.disposed"
                  defaultMessage="Disposed"
                />
              </span>
              <span className="progress-value">{summary.disposed}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.disposal.archived"
                  defaultMessage="Archived"
                />
              </span>
              <span className="progress-value">{summary.archived}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.disposal.retention"
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
            id="notebook.pharma.disposal.disposeSelected"
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
            id="notebook.pharma.disposal.markRetention"
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
            id="notebook.pharma.disposal.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="pharma-disposal"
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
            {
              key: "sampleType",
              header: intl.formatMessage({
                id: "notebook.pharma.disposal.sampleType",
                defaultMessage: "Sample Type",
              }),
              render: (value) => value || "-",
            },
            {
              key: "retentionStatus",
              header: intl.formatMessage({
                id: "notebook.pharma.disposal.retentionStatus",
                defaultMessage: "Status",
              }),
              render: (value, row) => renderDisposalTag(row),
            },
            {
              key: "storage",
              header: intl.formatMessage({
                id: "notebook.pharma.disposal.storage",
                defaultMessage: "Storage Location",
              }),
              render: (value, row) => {
                // If we have a full storagePath, use it and append well coordinate
                if (row.storagePath) {
                  const displayPath = row.storageWell
                    ? `${row.storagePath} > ${row.storageWell}`
                    : row.storagePath;
                  return <span title={displayPath}>{displayPath}</span>;
                }

                // Build storage path from individual components: room > rack > box > coordinate
                const pathParts = [
                  row.storageRoom,
                  row.storageFreezer,
                  row.storageRack,
                  row.storageBox,
                  row.storageWell,
                ].filter(Boolean);

                if (pathParts.length > 0) {
                  const displayPath = pathParts.join(" > ");
                  return <span title={displayPath}>{displayPath}</span>;
                }

                // Fallback to storageLocation field
                if (row.storageLocation) {
                  return row.storageLocation;
                }

                return <span style={{ color: "#8d8d8d" }}>-</span>;
              },
            },
          ]}
          additionalColumns={[
            {
              key: "expiry",
              header: intl.formatMessage({
                id: "notebook.pharma.disposal.expiry",
                defaultMessage: "Expiry",
              }),
              render: (_, sample) => renderExpiryStatus(sample),
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.pharma.disposal.empty"
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
          id: "notebook.pharma.disposal.modalTitle",
          defaultMessage: "Dispose & Archive Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.pharma.disposal.confirm",
          defaultMessage: "Confirm Disposal",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.pharma.disposal.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "notebook.pharma.disposal.warning",
              defaultMessage: "Warning: This action is irreversible",
            })}
            subtitle={intl.formatMessage(
              {
                id: "notebook.pharma.disposal.warningSubtitle",
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
                  id: "notebook.pharma.disposal.reason",
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
                  id: "notebook.pharma.disposal.method",
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
                    id: "notebook.pharma.disposal.date",
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
                  id: "notebook.pharma.disposal.disposedBy",
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
              id="notebook.pharma.disposal.signOffs"
              defaultMessage="Required Sign-Offs"
            />
          </h5>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="supervisor-signoff"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.disposal.supervisor",
                  defaultMessage: "Laboratory Supervisor *",
                })}
                value={disposalData.supervisorSignOff}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    supervisorSignOff: e.target.value,
                  })
                }
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="biosafety-signoff"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.disposal.biosafetyOfficer",
                  defaultMessage: "Biosafety Officer (if applicable)",
                })}
                value={disposalData.biosafetyOfficerSignOff}
                onChange={(e) =>
                  setDisposalData({
                    ...disposalData,
                    biosafetyOfficerSignOff: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          {/* Archive Information */}
          <h5>
            <FormattedMessage
              id="notebook.pharma.disposal.archiving"
              defaultMessage="Archiving Information"
            />
          </h5>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="certificate-number"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.disposal.certificate",
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
                  id: "notebook.pharma.disposal.archiveLocation",
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
              id: "notebook.pharma.disposal.notes",
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
                id: "notebook.pharma.disposal.confirmCheckbox",
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

export default PharmaceuticalDisposalPage;
