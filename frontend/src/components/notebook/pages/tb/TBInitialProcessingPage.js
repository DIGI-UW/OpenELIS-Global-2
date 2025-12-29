import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Tag,
  Accordion,
  AccordionItem,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  OverflowMenu,
  OverflowMenuItem,
} from "@carbon/react";
import {
  CheckmarkFilled,
  Renew,
  Chemistry,
  Microscope,
  Add,
  CheckmarkOutline,
  CloseOutline,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import MediaPreparationModal from "./components/MediaPreparationModal";
import SampleProcessingModal from "./components/SampleProcessingModal";
import InoculationModal from "./components/InoculationModal";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBInitialProcessingPage - Page 4 of the TB workflow.
 *
 * Three-step workflow:
 * 1. Media Preparation: Create/manage media batches (LJ, MGIT)
 * 2. Sample Processing: Decontamination (NALC-NaOH)
 * 3. Inoculation: Link processed samples to media batches
 *
 * After inoculation, samples move to Incubation Monitoring page.
 */
function TBInitialProcessingPage({
  entryId,
  pageData,
  pages,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Modal states
  const [mediaModalOpen, setMediaModalOpen] = useState(false);
  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [inoculationModalOpen, setInoculationModalOpen] = useState(false);

  // For inoculation modal - single or bulk mode
  const [selectedSampleForInoculation, setSelectedSampleForInoculation] =
    useState(null);
  const [bulkInoculationMode, setBulkInoculationMode] = useState(false);

  // Statistics from backend
  const [stats, setStats] = useState({
    mediaPending: 0,
    mediaPassed: 0,
    samplesPending: 0,
    samplesProcessed: 0,
    samplesReadyForInoculation: 0,
    inoculatedCount: 0,
  });

  // Media batches state
  const [mediaBatches, setMediaBatches] = useState([]);
  const [loadingBatches, setLoadingBatches] = useState(false);

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadStatistics();
    loadMediaBatches();

    return () => {
      componentMounted.current = false;
    };
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
              sampleItemId: sample.sampleItemId,
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              // Processing data
              processingStatus: sample.data?.processingStatus || "PENDING",
              decontaminationMethod: sample.data?.decontaminationMethod,
              processingDate: sample.data?.processingDate,
              // Inoculation data
              inoculatedDate: sample.data?.inoculatedDate,
              mediaBatchId: sample.data?.mediaBatchId,
              cultureMethod: sample.data?.cultureMethod,
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

  const loadStatistics = useCallback(() => {
    getFromOpenElisServer("/rest/tb/processing/statistics", (response) => {
      if (componentMounted.current && response) {
        setStats({
          mediaPending: response.mediaPending || 0,
          mediaPassed: response.mediaPassed || 0,
          samplesPending: response.samplesPending || 0,
          samplesProcessed: response.samplesProcessed || 0,
          samplesReadyForInoculation: response.samplesReadyForInoculation || 0,
          inoculatedCount: response.inoculatedCount || 0,
        });
      }
    });
  }, []);

  // Load all media batches for display
  const loadMediaBatches = useCallback(() => {
    setLoadingBatches(true);
    getFromOpenElisServer("/rest/tb/processing/media", (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setMediaBatches(response);
        } else {
          setMediaBatches([]);
        }
        setLoadingBatches(false);
      }
    });
  }, []);

  // Update QC status for a media batch
  const handleUpdateMediaBatchQC = useCallback(
    (batchId, newQcStatus) => {
      postToOpenElisServer(
        `/rest/tb/processing/media/${batchId}/qc-status`,
        JSON.stringify({ status: newQcStatus }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.tb.processing.qcUpdated",
                    defaultMessage:
                      "Media batch QC status updated to {status}.",
                  },
                  { status: newQcStatus },
                ),
              );
              loadMediaBatches();
              loadStatistics();
            } else {
              setError(response?.error || "Failed to update QC status.");
            }
          }
        },
      );
    },
    [intl, loadMediaBatches, loadStatistics],
  );

  // Route samples to a target page after inoculation
  const routeSamplesToPage = useCallback(
    (sampleIds, targetPageOrder) => {
      if (!pages || sampleIds.length === 0) return;

      const targetPage = pages.find((p) => p.order === targetPageOrder);
      if (targetPage && !String(targetPage.id).startsWith("default-")) {
        postToOpenElisServer(
          `/rest/notebook/bulk/page/${targetPage.id}/samples/add`,
          JSON.stringify({
            sampleIds: sampleIds.map((id) => parseInt(id, 10)),
          }),
          (status) => {
            if (status === 200) {
              console.log(
                `Routed ${sampleIds.length} sample(s) to page ${targetPageOrder} (Incubation Monitoring)`,
              );
            }
          },
        );
      }
    },
    [pages],
  );

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Split samples: pending (not inoculated) vs inoculated
  const { pendingSamples, inoculatedSamples } = useMemo(() => {
    const pending = samples.filter((s) => !s.inoculatedDate);
    const inoculated = samples.filter((s) => s.inoculatedDate);
    return { pendingSamples: pending, inoculatedSamples: inoculated };
  }, [samples]);

  // Calculate local stats from samples
  const localStats = useMemo(() => {
    const pending = samples.filter(
      (s) => s.processingStatus === "PENDING",
    ).length;
    const processed = samples.filter(
      (s) =>
        s.processingStatus === "PROCESSED" ||
        s.processingStatus === "READY_FOR_INOCULATION",
    ).length;
    const ready = samples.filter(
      (s) => s.processingStatus === "READY_FOR_INOCULATION",
    ).length;
    const inoculated = samples.filter((s) => s.inoculatedDate).length;

    return {
      total: samples.length,
      pending,
      processed,
      ready,
      inoculated,
    };
  }, [samples]);

  // ==========================================
  // Step 1: Media Preparation Handlers
  // ==========================================

  const handleOpenMediaModal = useCallback(() => {
    setMediaModalOpen(true);
  }, []);

  const handleSaveMediaBatch = useCallback(
    (batchData) => {
      postToOpenElisServer(
        "/rest/tb/processing/media",
        JSON.stringify(batchData),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.tb.processing.mediaBatchCreated",
                    defaultMessage:
                      "Media batch {batchId} created successfully.",
                  },
                  { batchId: batchData.batchId },
                ),
              );
              setMediaModalOpen(false);
              loadMediaBatches();
              loadStatistics();
            } else {
              setError(response?.error || "Failed to create media batch.");
            }
          }
        },
      );
    },
    [intl, loadMediaBatches, loadStatistics],
  );

  // ==========================================
  // Step 2: Sample Processing Handlers
  // ==========================================

  const handleOpenProcessingModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.processing.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setProcessingModalOpen(true);
  }, [selectedIds, intl]);

  const handleSaveProcessing = useCallback(
    (processingData) => {
      if (!hasRealPageId) {
        setProcessingModalOpen(false);
        return;
      }

      const numericIds = selectedIds.map((id) => parseInt(id, 10));

      // Determine status based on markReadyForInoculation flag
      const newStatus = processingData.markReadyForInoculation
        ? "READY_FOR_INOCULATION"
        : "PROCESSED";

      const dataToSave = {
        processingStatus: newStatus,
        decontaminationMethod: processingData.method,
        methodNotes: processingData.methodNotes,
        processingDate: processingData.processingDate,
      };

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: numericIds,
          data: dataToSave,
        }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              // Also save to TB processing service
              postToOpenElisServer(
                "/rest/tb/processing/sample/batch",
                JSON.stringify({
                  sampleItemIds: samples
                    .filter((s) => selectedIds.includes(s.id))
                    .map((s) => s.sampleItemId || s.id),
                  method: processingData.method,
                }),
                () => {
                  setSuccess(
                    intl.formatMessage(
                      {
                        id: "notebook.tb.processing.sampleProcessed",
                        defaultMessage:
                          "{count} sample(s) processed successfully.",
                      },
                      { count: selectedIds.length },
                    ),
                  );
                  setProcessingModalOpen(false);
                  setSelectedIds([]);
                  loadPageSamples();
                  loadStatistics();
                  if (onProgressUpdate) onProgressUpdate();
                },
              );
            } else {
              setError(response?.error || "Failed to save processing data.");
            }
          }
        },
      );
    },
    [
      selectedIds,
      samples,
      hasRealPageId,
      pageData?.id,
      intl,
      loadPageSamples,
      loadStatistics,
      onProgressUpdate,
    ],
  );

  // ==========================================
  // Step 3: Inoculation Handlers
  // ==========================================

  const handleOpenInoculationModal = useCallback(
    (sample) => {
      // Check if sample is ready for inoculation
      if (
        sample.processingStatus !== "PROCESSED" &&
        sample.processingStatus !== "READY_FOR_INOCULATION"
      ) {
        setError(
          intl.formatMessage({
            id: "notebook.tb.processing.sampleNotProcessed",
            defaultMessage: "Sample must be processed before inoculation.",
          }),
        );
        return;
      }
      setBulkInoculationMode(false);
      setSelectedSampleForInoculation(sample);
      setInoculationModalOpen(true);
    },
    [intl],
  );

  // Bulk inoculation - for multiple selected samples
  const handleOpenBulkInoculation = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.processing.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    // Get selected samples that are ready for inoculation
    const readySamples = samples.filter(
      (s) =>
        selectedIds.includes(s.id) &&
        !s.inoculatedDate &&
        (s.processingStatus === "PROCESSED" ||
          s.processingStatus === "READY_FOR_INOCULATION"),
    );

    if (readySamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.processing.noReadySamples",
          defaultMessage:
            "None of the selected samples are ready for inoculation. Process them first.",
        }),
      );
      return;
    }

    setBulkInoculationMode(true);
    setSelectedSampleForInoculation(readySamples); // Pass array for bulk mode
    setInoculationModalOpen(true);
  }, [selectedIds, samples, intl]);

  const handleSaveInoculation = useCallback(
    (inoculationData) => {
      // Determine if this is bulk mode (array) or single mode (object)
      const isBulk =
        bulkInoculationMode && Array.isArray(selectedSampleForInoculation);
      const samplesToProcess = isBulk
        ? selectedSampleForInoculation
        : [selectedSampleForInoculation];

      if (!samplesToProcess || samplesToProcess.length === 0) {
        setError("No samples selected for inoculation.");
        return;
      }

      // Process each sample
      let successCount = 0;
      let errorCount = 0;
      const sampleIds = samplesToProcess.map((s) => parseInt(s.id, 10));

      // For bulk, we use a batch endpoint or process sequentially
      const processNextSample = (index) => {
        if (index >= samplesToProcess.length) {
          // All done - update notebook data
          if (hasRealPageId && successCount > 0) {
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
              JSON.stringify({
                sampleIds: sampleIds,
                data: {
                  inoculatedDate: inoculationData.inoculationDate,
                  mediaBatchId: inoculationData.mediaBatchId,
                  cultureMethod: inoculationData.cultureMethod,
                  processingStatus: "INOCULATED",
                },
              }),
              () => {
                // Update status to COMPLETED
                postToOpenElisServer(
                  `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                  JSON.stringify({
                    sampleIds: sampleIds,
                    status: "COMPLETED",
                  }),
                  () => {
                    // Route samples to Page 5 (Incubation Monitoring)
                    routeSamplesToPage(sampleIds, 5);

                    setSuccess(
                      intl.formatMessage(
                        {
                          id: "notebook.tb.processing.samplesInoculated",
                          defaultMessage:
                            "{count} sample(s) inoculated successfully.",
                        },
                        { count: successCount },
                      ),
                    );
                    setInoculationModalOpen(false);
                    setSelectedSampleForInoculation(null);
                    setBulkInoculationMode(false);
                    setSelectedIds([]);
                    loadPageSamples();
                    loadStatistics();
                    if (onProgressUpdate) onProgressUpdate();
                  },
                );
              },
            );
          }
          return;
        }

        const sample = samplesToProcess[index];
        const sampleData = {
          sampleItemId: sample.sampleItemId || sample.id,
          mediaBatchId: parseInt(inoculationData.mediaBatchId, 10),
          processingId: null,
          cultureMethod: inoculationData.cultureMethod,
          inoculationDate: inoculationData.inoculationDate,
        };

        postToOpenElisServer(
          "/rest/tb/processing/inoculate",
          JSON.stringify(sampleData),
          (response) => {
            if (componentMounted.current) {
              if (response && !response.error) {
                successCount++;
              } else {
                errorCount++;
              }
              processNextSample(index + 1);
            }
          },
        );
      };

      // Start processing
      processNextSample(0);
    },
    [
      bulkInoculationMode,
      hasRealPageId,
      selectedSampleForInoculation,
      pageData?.id,
      intl,
      loadPageSamples,
      loadStatistics,
      onProgressUpdate,
      routeSamplesToPage,
    ],
  );

  // Quick action: Mark ready for inoculation
  const handleBulkMarkReady = useCallback(() => {
    if (selectedIds.length === 0) return;

    if (!hasRealPageId) {
      setError("Page not properly initialized.");
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: {
          processingStatus: "READY_FOR_INOCULATION",
        },
      }),
      (response) => {
        if (response && !response.error) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.tb.processing.markedReady",
                defaultMessage:
                  "{count} sample(s) marked ready for inoculation.",
              },
              { count: selectedIds.length },
            ),
          );
          setSelectedIds([]);
          loadPageSamples();
          loadStatistics();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(response?.error || "Failed to update samples.");
        }
      },
    );
  }, [
    selectedIds,
    hasRealPageId,
    pageData?.id,
    intl,
    loadPageSamples,
    loadStatistics,
    onProgressUpdate,
  ]);

  // Render processing status column
  const renderProcessingInfo = (_, sample) => {
    if (!sample) return null;

    const statusConfig = {
      PENDING: { type: "gray", text: "Pending" },
      PROCESSED: { type: "blue", text: "Processed" },
      READY_FOR_INOCULATION: { type: "purple", text: "Ready for Inoculation" },
      INOCULATED: { type: "green", text: "Inoculated" },
    };

    const config =
      statusConfig[sample.processingStatus] || statusConfig.PENDING;

    return (
      <div style={{ fontSize: "12px" }}>
        <Tag type={config.type} size="sm">
          {config.text}
        </Tag>
        {sample.decontaminationMethod && (
          <div style={{ marginTop: "4px", color: "#525252" }}>
            Method: {sample.decontaminationMethod.replace("_", "-")}
          </div>
        )}
        {sample.processingDate && (
          <div style={{ color: "#8d8d8d", fontSize: "11px" }}>
            {sample.processingDate}
          </div>
        )}
      </div>
    );
  };

  // Render inoculation status column
  const renderInoculationInfo = (_, sample) => {
    if (!sample) return null;

    if (sample.inoculatedDate) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="green" size="sm">
            <CheckmarkFilled size={12} style={{ marginRight: "4px" }} />
            Inoculated
          </Tag>
          <div style={{ marginTop: "4px", color: "#525252" }}>
            {sample.cultureMethod || "LJ"}
          </div>
          <div style={{ color: "#8d8d8d", fontSize: "11px" }}>
            {sample.inoculatedDate}
          </div>
        </div>
      );
    }

    // Show inoculate button for ready samples
    if (
      sample.processingStatus === "PROCESSED" ||
      sample.processingStatus === "READY_FOR_INOCULATION"
    ) {
      return (
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Add}
          onClick={() => handleOpenInoculationModal(sample)}
        >
          <FormattedMessage
            id="notebook.tb.processing.inoculate"
            defaultMessage="Inoculate"
          />
        </Button>
      );
    }

    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.tb.processing.notReady"
          defaultMessage="Not ready"
        />
      </span>
    );
  };

  const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));

  return (
    <div className="tb-initial-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <Chemistry
            size={20}
            style={{ marginRight: "8px", verticalAlign: "middle" }}
          />
          <FormattedMessage
            id="notebook.page.tb.initialProcessing.title"
            defaultMessage="TB Initial Processing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.initialProcessing.description"
            defaultMessage="Process samples through decontamination and inoculation to culture media."
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
                  id="notebook.tb.processing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{localStats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.processing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{localStats.pending}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.processing.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{localStats.processed}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.processing.readyForInoc"
                  defaultMessage="Ready for Inoc."
                />
              </span>
              <span className="progress-value">{localStats.ready}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.processing.inoculated"
                  defaultMessage="Inoculated"
                />
              </span>
              <span className="progress-value">{localStats.inoculated}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Action Buttons - Three-step workflow */}
      <div className="page-actions-bar">
        {/* Step 1: Create Media Batch */}
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={handleOpenMediaModal}
        >
          <FormattedMessage
            id="notebook.tb.processing.createMediaBatch"
            defaultMessage="Create Media Batch"
          />
        </Button>

        {/* Step 2: Record Processing */}
        <Button
          kind="primary"
          size="sm"
          renderIcon={Microscope}
          onClick={handleOpenProcessingModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.tb.processing.recordProcessing"
            defaultMessage="Record Processing ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {/* Quick action: Mark Ready */}
        {selectedIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleBulkMarkReady}
          >
            <FormattedMessage
              id="notebook.tb.processing.markReady"
              defaultMessage="Mark Ready ({count})"
              values={{ count: selectedIds.length }}
            />
          </Button>
        )}

        {/* Step 3: Bulk Inoculation */}
        {selectedIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Add}
            onClick={handleOpenBulkInoculation}
          >
            <FormattedMessage
              id="notebook.tb.processing.bulkInoculate"
              defaultMessage="Bulk Inoculate ({count})"
              values={{ count: selectedIds.length }}
            />
          </Button>
        )}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={() => {
            loadPageSamples();
            loadStatistics();
            loadMediaBatches();
          }}
        >
          <FormattedMessage
            id="notebook.tb.processing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Media Batches Management Section */}
      <Accordion>
        <AccordionItem
          title={
            <span style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <FormattedMessage
                id="notebook.tb.processing.mediaBatches"
                defaultMessage="Media Batches"
              />
              <Tag type="blue" size="sm">
                {mediaBatches.length}
              </Tag>
            </span>
          }
        >
          <div style={{ padding: "1rem 0" }}>
            {loadingBatches ? (
              <p>
                <FormattedMessage
                  id="loading.label"
                  defaultMessage="Loading..."
                />
              </p>
            ) : mediaBatches.length === 0 ? (
              <p style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.tb.processing.noBatches"
                  defaultMessage="No media batches created yet. Click 'Create Media Batch' to add one."
                />
              </p>
            ) : (
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage
                        id="notebook.tb.processing.batchId"
                        defaultMessage="Batch ID"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="notebook.tb.processing.mediaType"
                        defaultMessage="Media Type"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="notebook.tb.processing.prepDate"
                        defaultMessage="Prep Date"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="notebook.tb.processing.expiryDate"
                        defaultMessage="Expiry"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="notebook.tb.processing.qcStatus"
                        defaultMessage="QC Status"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="notebook.tb.processing.actions"
                        defaultMessage="Actions"
                      />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {mediaBatches.map((batch) => (
                    <TableRow key={batch.id}>
                      <TableCell>{batch.batchId}</TableCell>
                      <TableCell>
                        <Tag
                          type={batch.mediaType === "LJ" ? "purple" : "teal"}
                          size="sm"
                        >
                          {batch.mediaType}
                        </Tag>
                      </TableCell>
                      <TableCell>
                        {batch.preparationDate
                          ? new Date(batch.preparationDate).toLocaleDateString()
                          : "-"}
                      </TableCell>
                      <TableCell>
                        {batch.expiryDate
                          ? new Date(batch.expiryDate).toLocaleDateString()
                          : "-"}
                      </TableCell>
                      <TableCell>
                        <Tag
                          type={
                            batch.qcStatus === "PASSED"
                              ? "green"
                              : batch.qcStatus === "FAILED"
                                ? "red"
                                : "gray"
                          }
                          size="sm"
                        >
                          {batch.qcStatus || "PENDING"}
                        </Tag>
                      </TableCell>
                      <TableCell>
                        {batch.qcStatus === "PENDING" && (
                          <OverflowMenu size="sm" flipped>
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: "notebook.tb.processing.markQcPassed",
                                defaultMessage: "Mark QC Passed",
                              })}
                              onClick={() =>
                                handleUpdateMediaBatchQC(batch.id, "PASSED")
                              }
                            />
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: "notebook.tb.processing.markQcFailed",
                                defaultMessage: "Mark QC Failed",
                              })}
                              onClick={() =>
                                handleUpdateMediaBatchQC(batch.id, "FAILED")
                              }
                              isDelete
                            />
                          </OverflowMenu>
                        )}
                        {batch.qcStatus !== "PENDING" && (
                          <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
                            {batch.qcStatus === "PASSED" ? (
                              <CheckmarkOutline
                                size={16}
                                style={{ color: "#24a148" }}
                              />
                            ) : (
                              <CloseOutline
                                size={16}
                                style={{ color: "#da1e28" }}
                              />
                            )}
                          </span>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </AccordionItem>
      </Accordion>

      {/* Sample Grid - Pending samples only */}
      <div className="sample-grid-container">
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="notebook.tb.processing.pendingSamples"
            defaultMessage="Pending Samples"
          />
          <Tag type="blue" size="sm" style={{ marginLeft: "0.5rem" }}>
            {pendingSamples.length}
          </Tag>
        </h5>
        <SampleGrid
          gridId="tb-initial-processing"
          samples={pendingSamples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={[
            {
              key: "processingInfo",
              header: intl.formatMessage({
                id: "notebook.tb.processing.processingStatus",
                defaultMessage: "Processing",
              }),
              render: renderProcessingInfo,
            },
            {
              key: "inoculationInfo",
              header: intl.formatMessage({
                id: "notebook.tb.processing.inoculationStatus",
                defaultMessage: "Inoculation",
              }),
              render: renderInoculationInfo,
            },
          ]}
        />
      </div>

      {/* Empty state for pending samples */}
      {!loading && pendingSamples.length === 0 && samples.length > 0 && (
        <div className="empty-state" style={{ marginBottom: "2rem" }}>
          <p>
            <FormattedMessage
              id="notebook.tb.processing.allInoculated"
              defaultMessage="All samples have been inoculated. Check the completed samples below."
            />
          </p>
        </div>
      )}

      {/* Inoculated Samples Table */}
      {inoculatedSamples.length > 0 && (
        <div className="sample-grid-container" style={{ marginTop: "2rem" }}>
          <h5 style={{ marginBottom: "0.5rem" }}>
            <CheckmarkFilled
              size={16}
              style={{
                marginRight: "8px",
                verticalAlign: "middle",
                color: "#24a148",
              }}
            />
            <FormattedMessage
              id="notebook.tb.processing.inoculatedSamples"
              defaultMessage="Inoculated Samples (Completed)"
            />
            <Tag type="green" size="sm" style={{ marginLeft: "0.5rem" }}>
              {inoculatedSamples.length}
            </Tag>
          </h5>
          <Table size="sm">
            <TableHead>
              <TableRow>
                <TableHeader>
                  <FormattedMessage
                    id="notebook.sample.accessionNumber"
                    defaultMessage="Accession Number"
                  />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage
                    id="notebook.sample.sampleType"
                    defaultMessage="Sample Type"
                  />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage
                    id="notebook.tb.processing.cultureMethod"
                    defaultMessage="Culture Method"
                  />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage
                    id="notebook.tb.processing.inoculatedDate"
                    defaultMessage="Inoculated Date"
                  />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage
                    id="notebook.tb.processing.status"
                    defaultMessage="Status"
                  />
                </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {inoculatedSamples.map((sample) => (
                <TableRow key={sample.id}>
                  <TableCell>{sample.accessionNumber || sample.id}</TableCell>
                  <TableCell>{sample.sampleType || "-"}</TableCell>
                  <TableCell>
                    <Tag
                      type={sample.cultureMethod === "MGIT" ? "teal" : "purple"}
                      size="sm"
                    >
                      {sample.cultureMethod || "LJ"}
                    </Tag>
                  </TableCell>
                  <TableCell>
                    {sample.inoculatedDate
                      ? new Date(sample.inoculatedDate).toLocaleDateString()
                      : "-"}
                  </TableCell>
                  <TableCell>
                    <Tag type="green" size="sm">
                      <CheckmarkFilled
                        size={12}
                        style={{ marginRight: "4px" }}
                      />
                      <FormattedMessage
                        id="notebook.tb.processing.completed"
                        defaultMessage="Completed"
                      />
                    </Tag>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.tb.processing.empty"
              defaultMessage="No samples available for processing. Please complete the storage assignment step first."
            />
          </p>
        </div>
      )}

      {/* Step 1: Media Preparation Modal */}
      <MediaPreparationModal
        open={mediaModalOpen}
        onClose={() => setMediaModalOpen(false)}
        onSave={handleSaveMediaBatch}
      />

      {/* Step 2: Sample Processing Modal */}
      <SampleProcessingModal
        open={processingModalOpen}
        onClose={() => setProcessingModalOpen(false)}
        onSave={handleSaveProcessing}
        selectedCount={selectedIds.length}
        selectedSamples={selectedSamples}
      />

      {/* Step 3: Inoculation Modal */}
      <InoculationModal
        open={inoculationModalOpen}
        onClose={() => {
          setInoculationModalOpen(false);
          setSelectedSampleForInoculation(null);
          setBulkInoculationMode(false);
        }}
        onSave={handleSaveInoculation}
        sample={selectedSampleForInoculation}
        bulkMode={bulkInoculationMode}
      />
    </div>
  );
}

export default TBInitialProcessingPage;
