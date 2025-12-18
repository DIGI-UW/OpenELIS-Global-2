import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextInput,
  TextArea,
  Checkbox,
  Tag,
  Toggle,
  Loading,
} from "@carbon/react";
import {
  Checkmark,
  Edit,
  Document,
  Locked,
  Archive,
  Renew,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineArchivalPage - Page 9 of the Traditional Medicine workflow.
 * Close the lifecycle while maintaining traceability.
 * Generate comprehensive reports and archive all data with metadata locking
 * for regulatory compliance.
 *
 * Data Points (per SRS):
 * - Reports Generated:
 *   - Sample History Report
 *   - Processing Summary Report
 *   - Product Lineage Report
 *   - Quality Certificate
 * - Data Archival:
 *   - Raw Laboratory Data
 *   - Spectral Data (NMR, MS, IR)
 *   - Image/Photo Data
 * - Archive Location
 * - Metadata Locking (Immutable)
 */
function TraditionalMedicineArchivalPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk Apply Modal State
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [isApplying, setIsApplying] = useState(false);

  // Report checkboxes
  const [sampleHistoryReport, setSampleHistoryReport] = useState(false);
  const [processingSummaryReport, setProcessingSummaryReport] = useState(false);
  const [productLineageReport, setProductLineageReport] = useState(false);
  const [qualityCertificate, setQualityCertificate] = useState(false);
  const [reportGeneratedBy, setReportGeneratedBy] = useState("");
  const [reportGeneratedDate, setReportGeneratedDate] = useState(
    new Date().toISOString().slice(0, 10),
  );

  // Archival checkboxes
  const [rawDataArchived, setRawDataArchived] = useState(false);
  const [spectralDataArchived, setSpectralDataArchived] = useState(false);
  const [imageDataArchived, setImageDataArchived] = useState(false);
  const [archiveLocation, setArchiveLocation] = useState("");
  const [metadataLocked, setMetadataLocked] = useState(false);
  const [archivalNotes, setArchivalNotes] = useState("");

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessMessage(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              status: sample.pageStatus || sample.status || "PENDING",
              localName: sample.data?.localName,
              productName: sample.data?.productName,
              batchId: sample.data?.batchId,
              // Reporting
              sampleHistoryReport: sample.data?.sampleHistoryReport,
              processingSummaryReport: sample.data?.processingSummaryReport,
              productLineageReport: sample.data?.productLineageReport,
              qualityCertificate: sample.data?.qualityCertificate,
              reportGeneratedDate: sample.data?.reportGeneratedDate,
              reportGeneratedBy: sample.data?.reportGeneratedBy,
              // Archival
              rawDataArchived: sample.data?.rawDataArchived,
              spectralDataArchived: sample.data?.spectralDataArchived,
              imageDataArchived: sample.data?.imageDataArchived,
              metadataLocked: sample.data?.metadataLocked,
              archiveLocation: sample.data?.archiveLocation,
              archivalNotes: sample.data?.archivalNotes,
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

  // Reset modal state
  const resetModalState = () => {
    setSampleHistoryReport(false);
    setProcessingSummaryReport(false);
    setProductLineageReport(false);
    setQualityCertificate(false);
    setReportGeneratedBy("");
    setReportGeneratedDate(new Date().toISOString().slice(0, 10));
    setRawDataArchived(false);
    setSpectralDataArchived(false);
    setImageDataArchived(false);
    setArchiveLocation("");
    setMetadataLocked(false);
    setArchivalNotes("");
  };

  // Open bulk apply modal
  const openBulkModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    resetModalState();
    setBulkModalOpen(true);
    setError(null);
  }, [selectedSampleIds, intl]);

  // Apply bulk archival data
  const applyBulkArchival = useCallback(() => {
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate at least one report or archive option is selected
    const hasReports =
      sampleHistoryReport ||
      processingSummaryReport ||
      productLineageReport ||
      qualityCertificate;
    const hasArchival =
      rawDataArchived || spectralDataArchived || imageDataArchived;

    if (!hasReports && !hasArchival) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.archival.error.noSelection",
          defaultMessage:
            "Please select at least one report or data archival option.",
        }),
      );
      return;
    }

    setIsApplying(true);
    setError(null);
    setSuccessMessage(null);

    // Build reports generated list
    const reportsGenerated = [];
    if (sampleHistoryReport) reportsGenerated.push("Sample History");
    if (processingSummaryReport) reportsGenerated.push("Processing Summary");
    if (productLineageReport) reportsGenerated.push("Product Lineage");
    if (qualityCertificate) reportsGenerated.push("Quality Certificate");

    // Build archived data list
    const archivedData = [];
    if (rawDataArchived) archivedData.push("Raw Data");
    if (spectralDataArchived) archivedData.push("Spectral Data");
    if (imageDataArchived) archivedData.push("Image Data");

    const data = {
      // Reporting
      sampleHistoryReport,
      processingSummaryReport,
      productLineageReport,
      qualityCertificate,
      reportsGenerated: reportsGenerated.join(", "),
      reportGeneratedDate: reportGeneratedDate || null,
      reportGeneratedBy: reportGeneratedBy || null,
      // Archival
      rawDataArchived,
      spectralDataArchived,
      imageDataArchived,
      archivedData: archivedData.join(", "),
      archiveLocation: archiveLocation || null,
      metadataLocked,
      archivalNotes: archivalNotes || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data,
      }),
      (response) => {
        setIsApplying(false);

        if (response && response.success) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.success.archivalApplied",
                defaultMessage: "Applied archival data to {count} sample(s).",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setBulkModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.bulkApply",
                defaultMessage:
                  "Failed to apply archival data. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    sampleHistoryReport,
    processingSummaryReport,
    productLineageReport,
    qualityCertificate,
    reportGeneratedDate,
    reportGeneratedBy,
    rawDataArchived,
    spectralDataArchived,
    imageDataArchived,
    archiveLocation,
    metadataLocked,
    archivalNotes,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Mark as lifecycle completed (archived)
  const markAsArchived = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate selected samples have archival data
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const incompleteCount = selectedSamples.filter(
      (s) =>
        !s.sampleHistoryReport &&
        !s.processingSummaryReport &&
        !s.productLineageReport &&
        !s.qualityCertificate &&
        !s.rawDataArchived &&
        !s.spectralDataArchived &&
        !s.imageDataArchived,
    ).length;
    if (incompleteCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.error.incompleteArchival",
            defaultMessage:
              "{count} sample(s) missing archival data. Apply archival details first.",
          },
          { count: incompleteCount },
        ),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (response) => {
        if (response && response.success !== false) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.success.archived",
                defaultMessage:
                  "Marked {count} sample(s) as Lifecycle Completed. Data archived successfully.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.status",
                defaultMessage: "Failed to update samples. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    samples,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Lock metadata (final archival step)
  const lockMetadataAction = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data: {
          metadataLocked: true,
          metadataLockedDate: new Date().toISOString(),
        },
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.success.locked",
                defaultMessage:
                  "Locked metadata for {count} sample(s). Records are now immutable.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.lockFailed",
                defaultMessage: "Failed to lock metadata. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const lockedCount = samples.filter((s) => s.metadataLocked).length;

  // Render archival info as tags
  const renderArchivalInfo = (sample) => {
    const tags = [];
    if (
      sample.rawDataArchived ||
      sample.spectralDataArchived ||
      sample.imageDataArchived
    ) {
      tags.push(
        <Tag key="archived" type="green" renderIcon={Archive} size="sm">
          Data Archived
        </Tag>,
      );
    }
    if (sample.metadataLocked) {
      tags.push(
        <Tag key="locked" type="purple" renderIcon={Locked} size="sm">
          Locked
        </Tag>,
      );
    }
    if (sample.archiveLocation) {
      tags.push(
        <Tag key="loc" type="blue" size="sm">
          {sample.archiveLocation.length > 20
            ? sample.archiveLocation.substring(0, 20) + "..."
            : sample.archiveLocation}
        </Tag>,
      );
    }
    return tags.length > 0 ? (
      <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
        {tags}
      </div>
    ) : (
      "-"
    );
  };

  // Render reports info
  const renderReportsInfo = (sample) => {
    const reports = [];
    if (sample.sampleHistoryReport) reports.push("History");
    if (sample.processingSummaryReport) reports.push("Processing");
    if (sample.productLineageReport) reports.push("Lineage");
    if (sample.qualityCertificate) reports.push("QC Cert");
    return reports.length > 0 ? reports.join(", ") : "-";
  };

  // Custom column renderer for archival data
  const enhancedSamples = samples.map((sample) => ({
    ...sample,
    archivalInfo: renderArchivalInfo(sample),
    reportsInfo: renderReportsInfo(sample),
  }));

  return (
    <div className="tradmed-archival-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.archival.title"
            defaultMessage="Reporting &amp; Data Archival"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.archival.description"
            defaultMessage="Close the lifecycle while maintaining full traceability. Generate comprehensive reports, archive all laboratory data, and lock metadata for regulatory compliance."
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
                  id="notebook.page.tradmed.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.pendingArchival"
                  defaultMessage="Pending Archival"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.archived"
                  defaultMessage="Archived"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile locked">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.metadataLocked"
                  defaultMessage="Metadata Locked"
                />
              </span>
              <span className="progress-value">{lockedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "label.success",
            defaultMessage: "Success",
          })}
          subtitle={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Action Buttons Bar */}
      <div className="page-actions-bar">
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Edit}
          onClick={openBulkModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.applyArchival"
            defaultMessage="Apply Archival Details ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
        <Button
          kind="primary"
          size="sm"
          renderIcon={Checkmark}
          onClick={markAsArchived}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.markArchived"
            defaultMessage="Complete Lifecycle ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
        <Button
          kind="danger"
          size="sm"
          renderIcon={Locked}
          onClick={lockMetadataAction}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.lockMetadata"
            defaultMessage="Lock Metadata ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage id="label.refresh" defaultMessage="Refresh" />
        </Button>
      </div>

      {/* Sample Grid */}
      {loading ? (
        <Loading withOverlay={false} />
      ) : (
        <SampleGrid
          samples={enhancedSamples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "localName", header: "Local Name" },
            { key: "productName", header: "Product" },
            { key: "reportsInfo", header: "Reports Generated" },
            { key: "archivalInfo", header: "Archive Status" },
            { key: "status", header: "Status" },
          ]}
        />
      )}

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.archival.empty"
              defaultMessage="No samples pending archival. Complete formulation first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.archival.modal.title",
          defaultMessage: "Apply Reporting & Archival Details",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : intl.formatMessage({
                id: "common.apply",
                defaultMessage: "Apply",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setBulkModalOpen(false)}
        onRequestSubmit={applyBulkArchival}
        size="md"
        primaryButtonDisabled={isApplying}
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.archival.modal.description"
              defaultMessage="Apply reporting and archival details to {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Reporting Section */}
          <h5 className="modal-section-title">
            <Document size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="notebook.page.tradmed.archival.reportingSection"
              defaultMessage="Reports Generated"
            />
          </h5>

          <div className="checkbox-group">
            <div className="checkbox-group-items checkbox-grid">
              <Checkbox
                id="sampleHistoryReport"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.sampleHistory",
                  defaultMessage: "Sample History Report",
                })}
                checked={sampleHistoryReport}
                onChange={(_, { checked }) => setSampleHistoryReport(checked)}
              />
              <Checkbox
                id="processingSummaryReport"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.processingSummary",
                  defaultMessage: "Processing Summary Report",
                })}
                checked={processingSummaryReport}
                onChange={(_, { checked }) =>
                  setProcessingSummaryReport(checked)
                }
              />
              <Checkbox
                id="productLineageReport"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.productLineage",
                  defaultMessage: "Product Lineage Report",
                })}
                checked={productLineageReport}
                onChange={(_, { checked }) => setProductLineageReport(checked)}
              />
              <Checkbox
                id="qualityCertificate"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.qualityCertificate",
                  defaultMessage: "Quality Certificate",
                })}
                checked={qualityCertificate}
                onChange={(_, { checked }) => setQualityCertificate(checked)}
              />
            </div>
          </div>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="reportGeneratedBy"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.reportGeneratedBy",
                  defaultMessage: "Report Generated By",
                })}
                value={reportGeneratedBy}
                onChange={(e) => setReportGeneratedBy(e.target.value)}
                placeholder="Enter staff name"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="reportGeneratedDate"
                type="date"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.reportGeneratedDate",
                  defaultMessage: "Report Date",
                })}
                value={reportGeneratedDate}
                onChange={(e) => setReportGeneratedDate(e.target.value)}
              />
            </Column>
          </Grid>

          {/* Data Archival Section */}
          <h5 className="modal-section-title">
            <Archive size={16} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="notebook.page.tradmed.archival.dataArchivalSection"
              defaultMessage="Data Archival"
            />
          </h5>

          <div className="checkbox-group">
            <div className="checkbox-group-items checkbox-grid">
              <Checkbox
                id="rawDataArchived"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.rawData",
                  defaultMessage: "Raw Laboratory Data",
                })}
                checked={rawDataArchived}
                onChange={(_, { checked }) => setRawDataArchived(checked)}
              />
              <Checkbox
                id="spectralDataArchived"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.spectralData",
                  defaultMessage: "Spectral Data (NMR, MS, IR)",
                })}
                checked={spectralDataArchived}
                onChange={(_, { checked }) => setSpectralDataArchived(checked)}
              />
              <Checkbox
                id="imageDataArchived"
                labelText={intl.formatMessage({
                  id: "notebook.page.tradmed.archival.imageData",
                  defaultMessage: "Image/Photo Data",
                })}
                checked={imageDataArchived}
                onChange={(_, { checked }) => setImageDataArchived(checked)}
              />
            </div>
          </div>

          <TextInput
            id="archiveLocation"
            labelText={intl.formatMessage({
              id: "notebook.page.tradmed.archival.archiveLocation",
              defaultMessage: "Archive Location/Server",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.page.tradmed.archival.archiveLocationPlaceholder",
              defaultMessage:
                "e.g., Archive Server A, Path: /tradmed/2024/batch001",
            })}
            value={archiveLocation}
            onChange={(e) => setArchiveLocation(e.target.value)}
          />

          {/* Metadata Locking */}
          <Toggle
            id="metadataLocked"
            labelText={intl.formatMessage({
              id: "notebook.page.tradmed.archival.lockMetadata",
              defaultMessage: "Lock Metadata (Immutable)",
            })}
            labelA={intl.formatMessage({
              id: "common.no",
              defaultMessage: "No",
            })}
            labelB={intl.formatMessage({
              id: "common.yes",
              defaultMessage: "Yes",
            })}
            toggled={metadataLocked}
            onToggle={(checked) => setMetadataLocked(checked)}
          />
          <p
            className="field-helper-text"
            style={{
              marginTop: "-0.5rem",
              fontSize: "0.75rem",
              color: "#6f6f6f",
            }}
          >
            <FormattedMessage
              id="notebook.page.tradmed.archival.lockWarning"
              defaultMessage="Warning: Once metadata is locked, records cannot be modified. Use for regulatory compliance."
            />
          </p>

          {/* Notes */}
          <TextArea
            id="archivalNotes"
            labelText={intl.formatMessage({
              id: "notebook.page.tradmed.archival.notes",
              defaultMessage: "Archival Notes",
            })}
            value={archivalNotes}
            onChange={(e) => setArchivalNotes(e.target.value)}
            rows={2}
            placeholder="Enter any additional notes about the archival process..."
          />
        </div>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineArchivalPage;
