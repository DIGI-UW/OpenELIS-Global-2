import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  ProgressBar,
} from "@carbon/react";
import { Checkmark, Application, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";
import BulkApplyForm from "../workflow/BulkApplyForm";
import "../workflow/NotebookWorkflow.css";

/**
 * InitialProcessingPage - Page 2 of the immunology workflow.
 * Handles initial sample processing: centrifugation, aliquoting, recording volumes.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress {total, pending, inProgress, completed, skipped, percentage}
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function InitialProcessingPage({
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
              patientName: sample.patientName,
              volume: sample.volume,
              // Page-specific data from JSONB
              processingData: sample.data || {},
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

  // Handle marking selected samples as processed
  const handleBulkMarkProcessed = useCallback(() => {
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
  const processedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;

  // Custom fields for initial processing
  // Per spec: a) Volume Determination, b) Cell Count & Isolation, c) Log parameters/timestamps
  const processingFields = [
    // Section a: Volume Determination
    {
      id: "initialVolume",
      type: "number",
      label: intl.formatMessage({
        id: "notebook.processing.initialVolume",
        defaultMessage: "Initial Volume (mL)",
      }),
      min: 0,
      max: 100,
      step: 0.1,
    },
    {
      id: "finalVolume",
      type: "number",
      label: intl.formatMessage({
        id: "notebook.processing.finalVolume",
        defaultMessage: "Final Volume (mL)",
      }),
      min: 0,
      max: 100,
      step: 0.1,
    },
    // Section b: Cell Count & Isolation
    {
      id: "cellCount",
      type: "text",
      label: intl.formatMessage({
        id: "notebook.processing.cellCount",
        defaultMessage: "Cell Count (cells/mL)",
      }),
      placeholder: "e.g., 2.4×10⁶",
    },
    {
      id: "cellViability",
      type: "number",
      label: intl.formatMessage({
        id: "notebook.processing.cellViability",
        defaultMessage: "Cell Viability (%)",
      }),
      min: 0,
      max: 100,
      step: 0.1,
    },
    {
      id: "isolationMethod",
      type: "dropdown",
      label: intl.formatMessage({
        id: "notebook.processing.isolationMethod",
        defaultMessage: "Isolation Method",
      }),
      options: [
        { id: "ficoll", text: "Ficoll Density Gradient" },
        { id: "centrifugation", text: "Centrifugation" },
        { id: "magnetic", text: "Magnetic Bead Separation" },
        { id: "other", text: "Other" },
      ],
    },
    {
      id: "centrifugeSpeed",
      type: "number",
      label: intl.formatMessage({
        id: "notebook.processing.centrifugeSpeed",
        defaultMessage: "Centrifuge Speed (RPM)",
      }),
      min: 0,
      max: 20000,
      step: 100,
    },
    {
      id: "centrifugeTime",
      type: "number",
      label: intl.formatMessage({
        id: "notebook.processing.centrifugeTime",
        defaultMessage: "Centrifuge Time (min)",
      }),
      min: 0,
      max: 60,
      step: 1,
    },
    // Section c: Log parameters and timestamps
    {
      id: "processingStartTime",
      type: "date",
      label: intl.formatMessage({
        id: "notebook.processing.startTime",
        defaultMessage: "Processing Start Time",
      }),
    },
    {
      id: "processingEndTime",
      type: "date",
      label: intl.formatMessage({
        id: "notebook.processing.endTime",
        defaultMessage: "Processing End Time",
      }),
    },
    {
      id: "technician",
      type: "text",
      label: intl.formatMessage({
        id: "notebook.processing.technician",
        defaultMessage: "Technician",
      }),
      placeholder: "Enter technician name",
    },
    {
      id: "notes",
      type: "text",
      label: intl.formatMessage({
        id: "notebook.processing.notes",
        defaultMessage: "Notes",
      }),
      placeholder: "Enter processing notes",
    },
  ];

  return (
    <div className="initial-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.initialProcessing.title"
            defaultMessage="Initial Processing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.initialProcessing.description"
            defaultMessage="Process samples through centrifugation and aliquoting. Record processing parameters and mark samples as processed."
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
                  id="notebook.page.initialProcessing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile completed">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.initialProcessing.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{processedCount}</span>
            </Tile>
            <Tile className="progress-tile in-progress">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.initialProcessing.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.initialProcessing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
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
            id="notebook.page.initialProcessing.bulkApply"
            defaultMessage="Bulk Apply Values ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkProcessed}
          >
            <FormattedMessage
              id="notebook.page.initialProcessing.markProcessed"
              defaultMessage="Mark Selected as Processed ({count})"
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
          hideCloseButton
          lowContrast
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="initial-processing"
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
              id="notebook.page.initialProcessing.empty"
              defaultMessage="No samples available for processing. Samples must first be received on Page 1."
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
        fields={processingFields}
      />
    </div>
  );
}

export default InitialProcessingPage;
