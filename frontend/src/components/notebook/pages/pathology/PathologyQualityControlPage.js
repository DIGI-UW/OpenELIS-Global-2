import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  ProgressBar,
} from "@carbon/react";
import { Checkmark, Application, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import BulkApplyForm from "../../workflow/BulkApplyForm";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyQualityControlPage - Page 2 of the pathology workflow.
 * Purpose: Verify sample suitability before processing.
 * Who uses it: Lab technicians
 *
 * Uses bulk apply functionality similar to Immunology's InitialProcessingPage
 * but with pathology-specific QC data points.
 */
function PathologyQualityControlPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pageProgress, setPageProgress] = useState(null);

  // Modal state
  const [bulkApplyOpen, setBulkApplyOpen] = useState(false);

  // Load samples and progress for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadPageProgress();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    // Skip loading for synthetic page IDs
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
              // QC-specific data
              qcStatus: sample.data?.qcStatus,
              qcDate: sample.data?.qcDate,
              specimenCategory: sample.specimenCategory || "histopathology",
              sampleCategory: sample.sampleCategory || "Clinical diagnostic",
              // Page-specific data from JSONB
              qcData: sample.data || {},
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

  const loadPageProgress = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    getFromOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/progress`,
      (response) => {
        if (componentMounted.current && response) {
          setPageProgress(response);
        }
      },
    );
  }, [pageData?.id]);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle bulk apply success
  const handleBulkApplySuccess = useCallback(
    (result) => {
      setBulkApplyOpen(false);
      setSelectedSampleIds([]);
      loadPageSamples();
      loadPageProgress();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    },
    [loadPageSamples, loadPageProgress, onProgressUpdate],
  );

  // Handle marking selected samples as QC passed
  const handleBulkMarkPassed = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        "Cannot update status: Page not properly initialized. Please refresh the page.",
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          loadPageSamples();
          loadPageProgress();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    loadPageProgress,
    onProgressUpdate,
  ]);

  // Handle individual sample status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          "Cannot update status: Page not properly initialized. Please refresh the page.",
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            loadPageProgress();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [
      pageData?.id,
      hasRealPageId,
      loadPageSamples,
      loadPageProgress,
      onProgressUpdate,
    ],
  );

  // Handle mark page complete
  const handleMarkPageComplete = useCallback(() => {
    if (!hasRealPageId) {
      setError(
        "Cannot mark page complete: Page not properly initialized. Please refresh the page.",
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/complete`,
      JSON.stringify({ requireComplete: false }),
      (status) => {
        if (status === 200) {
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            "Failed to mark page complete. Some samples may still be pending.",
          );
        }
      },
    );
  }, [pageData?.id, hasRealPageId, onProgressUpdate]);

  // Calculate stats
  const passedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const failedCount = samples.filter(
    (s) => s.qcData?.qcStatus === "Fail",
  ).length;

  // Pathology-specific QC fields for bulk apply
  // Based on the spec: Histology QC, Cytology QC, Blood QC, Research QC, Tissue Block QC, QC Outcome
  const qcFields = [
    // QC Outcome (most important - always visible)
    {
      id: "qcStatus",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.qcStatus",
        defaultMessage: "QC Status",
      }),
      options: [
        { id: "Pass", text: "Pass" },
        { id: "Fail", text: "Fail" },
      ],
    },
    {
      id: "staffInitials",
      type: "text",
      label: intl.formatMessage({
        id: "pathology.qc.staffInitials",
        defaultMessage: "Staff Initials",
      }),
      placeholder: "e.g., JD",
    },
    {
      id: "qcDate",
      type: "date",
      label: intl.formatMessage({
        id: "pathology.qc.qcDate",
        defaultMessage: "QC Date",
      }),
    },
    {
      id: "qcRemarks",
      type: "text",
      label: intl.formatMessage({
        id: "pathology.qc.qcRemarks",
        defaultMessage: "QC Remarks",
      }),
      placeholder: "Enter any remarks",
    },
    // Histology QC
    {
      id: "fixativeUsed",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.fixativeUsed",
        defaultMessage: "Fixative Used",
      }),
      options: [
        { id: "10% NBF", text: "10% NBF" },
        { id: "Formalin", text: "Formalin" },
        { id: "Other", text: "Other" },
      ],
    },
    {
      id: "fixativeRatio",
      type: "text",
      label: intl.formatMessage({
        id: "pathology.qc.fixativeRatio",
        defaultMessage: "Fixative Ratio",
      }),
      placeholder: "e.g., 10:1",
    },
    {
      id: "fixationDuration",
      type: "text",
      label: intl.formatMessage({
        id: "pathology.qc.fixationDuration",
        defaultMessage: "Fixation Duration",
      }),
      placeholder: "e.g., 24 hours",
    },
    {
      id: "tissueIntegrity",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.tissueIntegrity",
        defaultMessage: "Tissue Integrity",
      }),
      options: [
        { id: "Intact", text: "Intact" },
        { id: "Compromised", text: "Compromised" },
      ],
    },
    // Cytology QC
    {
      id: "containerIntegrity",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.containerIntegrity",
        defaultMessage: "Container Integrity",
      }),
      options: [
        { id: "Intact", text: "Intact" },
        { id: "Compromised", text: "Compromised" },
      ],
    },
    {
      id: "preservativeType",
      type: "text",
      label: intl.formatMessage({
        id: "pathology.qc.preservativeType",
        defaultMessage: "Preservative Type",
      }),
      placeholder: "e.g., Cytolyt",
    },
    {
      id: "volume",
      type: "number",
      label: intl.formatMessage({
        id: "pathology.qc.volume",
        defaultMessage: "Volume (mL)",
      }),
      min: 0,
      max: 1000,
      step: 0.1,
    },
    {
      id: "clotPresence",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.clotPresence",
        defaultMessage: "Clot Presence",
      }),
      options: [
        { id: "None", text: "None" },
        { id: "Present", text: "Present" },
      ],
    },
    // Blood QC
    {
      id: "clotCheck",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.clotCheck",
        defaultMessage: "Clot Check (EDTA)",
      }),
      options: [
        { id: "No clot", text: "No clot" },
        { id: "Clot present", text: "Clot present" },
      ],
    },
    // Tissue Block QC (Post-Embedding)
    {
      id: "surfaceQuality",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.surfaceQuality",
        defaultMessage: "Surface Quality",
      }),
      options: [
        { id: "Smooth", text: "Smooth" },
        { id: "Cracks", text: "Cracks" },
        { id: "Bubbles", text: "Bubbles" },
      ],
    },
    {
      id: "depthOrientation",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.depthOrientation",
        defaultMessage: "Depth/Orientation",
      }),
      options: [
        { id: "Correct", text: "Correct" },
        { id: "Shallow", text: "Shallow" },
        { id: "Deep", text: "Deep" },
      ],
    },
    // Research QC
    {
      id: "storageMedium",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.storageMedium",
        defaultMessage: "Storage Medium",
      }),
      options: [
        { id: "OCT", text: "OCT" },
        { id: "RNAlater", text: "RNAlater" },
        { id: "None", text: "None" },
        { id: "Other", text: "Other" },
      ],
    },
    // Fail Actions
    {
      id: "actionTaken",
      type: "dropdown",
      label: intl.formatMessage({
        id: "pathology.qc.actionTaken",
        defaultMessage: "Action Taken (if failed)",
      }),
      options: [
        { id: "Recollection requested", text: "Recollection requested" },
        { id: "Process with limitations", text: "Process with limitations" },
        { id: "Re-embed", text: "Re-embed" },
        { id: "Melt block", text: "Melt block" },
      ],
    },
  ];

  return (
    <div className="pathology-qc-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.qc.title"
            defaultMessage="Sample Quality Control"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.qc.description"
            defaultMessage="Verify sample suitability before processing. Perform QC checks by sample type and record outcomes. Use bulk apply to set common values across multiple samples."
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
                  id="pathology.page.qc.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile completed">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.qc.passed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{passedCount}</span>
            </Tile>
            <Tile className="progress-tile in-progress">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.qc.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.qc.pending"
                  defaultMessage="Pending QC"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            {failedCount > 0 && (
              <Tile
                className="progress-tile"
                style={{ backgroundColor: "#fff1f1" }}
              >
                <span className="progress-label">
                  <FormattedMessage
                    id="pathology.page.qc.failed"
                    defaultMessage="QC Failed"
                  />
                </span>
                <span className="progress-value">{failedCount}</span>
              </Tile>
            )}
          </div>
          {/* Progress bar */}
          {pageProgress && (
            <ProgressBar
              value={pageProgress.percentage}
              label={intl.formatMessage(
                {
                  id: "notebook.page.progress",
                  defaultMessage: "{completed} of {total} completed",
                },
                {
                  completed: pageProgress.completed,
                  total: pageProgress.total,
                },
              )}
              className="page-progress-bar"
            />
          )}
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Application}
          onClick={() => setBulkApplyOpen(true)}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="pathology.page.qc.bulkApply"
            defaultMessage="Bulk Apply QC Values ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkPassed}
          >
            <FormattedMessage
              id="pathology.page.qc.markPassed"
              defaultMessage="Mark Selected as Passed ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={() => {
            loadPageSamples();
            loadPageProgress();
          }}
        >
          <FormattedMessage
            id="notebook.page.refresh"
            defaultMessage="Refresh"
          />
        </Button>

        {samples.length > 0 && pendingCount === 0 && inProgressCount === 0 && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkPageComplete}
          >
            <FormattedMessage
              id="notebook.page.markComplete"
              defaultMessage="Mark Page Complete"
            />
          </Button>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="pathology-qc"
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="pathology.page.qc.empty"
              defaultMessage="No samples available for QC. Samples must be created on Page 1 (Sample Creation & Metadata Capture) first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <BulkApplyForm
        open={bulkApplyOpen}
        onClose={() => setBulkApplyOpen(false)}
        pageId={pageData?.id}
        selectedSampleIds={selectedSampleIds}
        onApplySuccess={handleBulkApplySuccess}
        fields={qcFields}
      />
    </div>
  );
}

export default PathologyQualityControlPage;
