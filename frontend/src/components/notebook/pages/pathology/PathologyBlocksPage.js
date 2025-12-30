import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  NumberInput,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Checkbox,
  Loading,
  TableContainer,
} from "@carbon/react";
import {
  Add,
  Checkmark,
  Renew,
  View,
  Edit,
  CheckmarkFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyBlocksPage - Tissue Block Creation workflow step.
 * Purpose: Create paraffin blocks from processed cassettes.
 * Who uses it: Histology Technicians
 *
 * Key activities:
 * - Embed tissue in paraffin wax
 * - Create blocks from cassettes
 * - Verify tissue orientation
 * - Document embedding quality
 */
function PathologyBlocksPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Block Modal state
  const [blockModalOpen, setBlockModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [blockLoading, setBlockLoading] = useState(false);
  const [blockViewMode, setBlockViewMode] = useState(false);

  // Block form data
  const [blockData, setBlockData] = useState({
    numberOfBlocks: 1,
    blockPrefix: "",
    blockLabels: [],
    // Embedding info
    embeddingMedium: "paraffin",
    embeddingTemperature: "",
    embeddingStation: "",
    // Orientation
    tissueOrientation: "",
    orientationVerified: false,
    orientationNotes: "",
    // Quality
    embeddingQuality: "",
    qualityNotes: "",
    // Processing
    processorUsed: "",
    processingProtocol: "",
    processingDuration: "",
    // Staff
    technicianName: "",
    technicianInitials: "",
    blockDate: "",
    notes: "",
    // Quality Control
    qcStatus: "PASS",
    qcEmbeddingQuality: true,
    qcOrientationCorrect: true,
    qcLabelingCorrect: true,
    qcBlockIntegrity: true,
    qcIssues: "",
    qcCorrectiveAction: "",
    qcReviewedBy: "",
    qcReviewDate: "",
  });

  // Embedding medium options
  const embeddingMediums = [
    { value: "paraffin", text: "Paraffin Wax" },
    { value: "oct", text: "OCT (Frozen)" },
    { value: "resin", text: "Resin" },
    { value: "celloidin", text: "Celloidin" },
  ];

  // Quality options
  const qualityOptions = [
    { value: "excellent", text: "Excellent" },
    { value: "good", text: "Good" },
    { value: "acceptable", text: "Acceptable" },
    { value: "poor", text: "Poor - Needs Re-embedding" },
  ];

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!entryId || !pageData?.id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Fetch samples that have completed the previous step (cassettes)
    // and merge with current blocks page data
    getFromOpenElisServer(
      `/rest/notebook/pathology/workflow/samples-ready?entryId=${entryId}&currentStep=blocks`,
      (workflowResponse) => {
        if (!componentMounted.current) return;

        // Also fetch current page samples to get block data
        getFromOpenElisServer(
          `/rest/notebook/page/${pageData.id}/samples`,
          (pageResponse) => {
            if (!componentMounted.current) return;

            // Build a map of current page sample data by sampleItemId
            const pageSampleMap = {};
            if (pageResponse && Array.isArray(pageResponse)) {
              pageResponse.forEach((ps) => {
                const sampleId = String(ps.sampleItemId || ps.id);
                pageSampleMap[sampleId] = ps;
              });
            }

            if (workflowResponse && Array.isArray(workflowResponse)) {
              const transformedSamples = workflowResponse.map((sample) => {
                const sampleId = String(sample.id || sample.sampleItemId);
                // For expanded items (e.g., "123_cassette_0"), try the full ID first,
                // then fall back to parent ID for backward compatibility
                const pageSample =
                  pageSampleMap[sampleId] ||
                  pageSampleMap[sample.parentSampleId] ||
                  pageSampleMap[sampleId.split("_")[0]];
                const blockData = pageSample?.data || {};
                // Note: workflowData contains data from PREVIOUS step (cassettes)
                // We should NOT use it for block-specific fields

                return {
                  id: sampleId,
                  externalId: sample.externalId,
                  accessionNumber: sample.accessionNumber,
                  sampleType:
                    sample.sampleType || sample.typeOfSample?.description,
                  specimenCategory: sample.specimenCategory || "histopathology",
                  collectionDate: sample.collectionDate,
                  // ONLY use status from current blocks page, default to PENDING
                  status: pageSample?.status || "PENDING",
                  patientName: sample.patientName,
                  // Parent info from cassette step (from workflow expansion)
                  parentSampleId: sample.parentSampleId,
                  childIndex: sample.childIndex,
                  childLabel: sample.childLabel,
                  cassetteLabel:
                    sample.cassetteLabel || sample.childLabel || "",
                  // Block status from current page data ONLY
                  blocksCreated: blockData.blocksCreated === true,
                  blockCount: blockData.blockCount || 0,
                  embeddingMedium: blockData.embeddingMedium || "",
                  embeddingQuality: blockData.embeddingQuality || "",
                  technicianName: blockData.technicianName || "",
                  blockDate: blockData.blockDate || "",
                  // QC status from current page ONLY - show nothing until blocks created
                  qcStatus: blockData.qcStatus || "",
                };
              });
              setSamples(transformedSamples);
            } else {
              setSamples([]);
            }
            setLoading(false);
          },
        );
      },
    );
  }, [entryId, pageData?.id]);

  // Check if we have the required entry ID to load samples
  const hasValidEntry = !!entryId;

  const handleBlockInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setBlockData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  // Open block modal
  const openBlockModal = (sample) => {
    setSelectedSample(sample);
    setBlockViewMode(false);
    setBlockLoading(true);

    // Reset form data with defaults
    setBlockData({
      numberOfBlocks: 1,
      blockPrefix: sample.accessionNumber || "",
      blockLabels: [],
      embeddingMedium: "paraffin",
      embeddingTemperature: "",
      embeddingStation: "",
      tissueOrientation: "",
      orientationVerified: false,
      orientationNotes: "",
      embeddingQuality: "",
      qualityNotes: "",
      processorUsed: "",
      processingProtocol: "",
      processingDuration: "",
      technicianName: "",
      technicianInitials: "",
      blockDate: new Date().toISOString().split("T")[0],
      notes: "",
      // QC fields
      qcStatus: "PASS",
      qcEmbeddingQuality: true,
      qcOrientationCorrect: true,
      qcLabelingCorrect: true,
      qcBlockIntegrity: true,
      qcIssues: "",
      qcCorrectiveAction: "",
      qcReviewedBy: "",
      qcReviewDate: new Date().toISOString().split("T")[0],
    });

    setBlockModalOpen(true);

    // Fetch existing block data if available
    if (pageData?.id && sample?.id) {
      getFromOpenElisServer(
        `/rest/notebook/pathology/blocks/${sample.id}?pageId=${pageData.id}`,
        (response) => {
          setBlockLoading(false);
          if (response && response.success && response.hasData) {
            setBlockViewMode(true);
            setBlockData((prev) => ({
              ...prev,
              numberOfBlocks: response.numberOfBlocks || 1,
              blockPrefix: response.blockPrefix || "",
              blockLabels: response.blockLabels || [],
              embeddingMedium: response.embeddingMedium || "paraffin",
              embeddingTemperature: response.embeddingTemperature || "",
              embeddingStation: response.embeddingStation || "",
              tissueOrientation: response.tissueOrientation || "",
              orientationVerified: response.orientationVerified === true,
              orientationNotes: response.orientationNotes || "",
              embeddingQuality: response.embeddingQuality || "",
              qualityNotes: response.qualityNotes || "",
              processorUsed: response.processorUsed || "",
              processingProtocol: response.processingProtocol || "",
              processingDuration: response.processingDuration || "",
              technicianName: response.technicianName || "",
              technicianInitials: response.technicianInitials || "",
              blockDate: response.blockDate || "",
              notes: response.notes || "",
              // QC fields
              qcStatus: response.qcStatus || "PASS",
              qcEmbeddingQuality: response.qcEmbeddingQuality !== false,
              qcOrientationCorrect: response.qcOrientationCorrect !== false,
              qcLabelingCorrect: response.qcLabelingCorrect !== false,
              qcBlockIntegrity: response.qcBlockIntegrity !== false,
              qcIssues: response.qcIssues || "",
              qcCorrectiveAction: response.qcCorrectiveAction || "",
              qcReviewedBy: response.qcReviewedBy || "",
              qcReviewDate: response.qcReviewDate || "",
            }));
          }
        },
      );
    } else {
      setBlockLoading(false);
    }
  };

  // Submit block data
  const handleSubmitBlocks = () => {
    if (submitting) return;

    setSubmitting(true);
    setError(null);

    // Generate block labels
    const labels = [];
    for (let i = 1; i <= blockData.numberOfBlocks; i++) {
      labels.push(`${blockData.blockPrefix}-B${String(i).padStart(2, "0")}`);
    }

    // Extract the real sample ID from composite ID (e.g., "123_cassette_0" -> "123")
    const realSampleId =
      selectedSample.parentSampleId || selectedSample.id.split("_")[0];

    const payload = {
      sampleId: realSampleId,
      pageId: pageData?.id,
      ...blockData,
      blockLabels: labels,
      blocksCreated: true,
      blockCount: blockData.numberOfBlocks,
      // Include parent hierarchy info
      parentCassetteLabel:
        selectedSample.cassetteLabel || selectedSample.childLabel,
      parentCassetteIndex: selectedSample.childIndex,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/blocks/submit`,
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setBlockModalOpen(false);
          setSuccessMessage(
            intl.formatMessage({
              id: "pathology.blocks.success",
              defaultMessage: "Blocks created successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to save block data.");
        }
      },
    );
  };

  // Mark samples as complete
  const handleMarkComplete = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.blocksCreated,
    );

    if (samplesToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.blocks.error.noComplete",
          defaultMessage:
            "Please create blocks before marking samples as complete.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    // Use string IDs for composite sample IDs (e.g., "123_cassette_0")
    const sampleIds = samplesToComplete.map((s) => s.id);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/status-string`,
      JSON.stringify({
        sampleIds: sampleIds,
        status: "COMPLETED",
      }),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.blocks.success.complete",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: samplesToComplete.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to update status.");
        }
      },
    );
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
  };

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.table.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "cassetteLabel",
      header: intl.formatMessage({
        id: "pathology.table.cassette",
        defaultMessage: "Cassette",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "blockStatus",
      header: intl.formatMessage({
        id: "pathology.table.blockStatus",
        defaultMessage: "Block Status",
      }),
    },
    {
      key: "blockCount",
      header: intl.formatMessage({
        id: "pathology.table.blockCount",
        defaultMessage: "Blocks",
      }),
    },
    {
      key: "embeddingQuality",
      header: intl.formatMessage({
        id: "pathology.table.quality",
        defaultMessage: "Quality",
      }),
    },
    {
      key: "qcStatus",
      header: intl.formatMessage({
        id: "pathology.table.qcStatus",
        defaultMessage: "QC",
      }),
    },
  ];

  // Computed values
  const completedCount = samples.filter((s) => s.blocksCreated).length;
  const pendingCount = samples.filter(
    (s) => s.status === "PENDING" || !s.blocksCreated,
  ).length;

  return (
    <div className="pathology-page blocks-page">
      {/* Header */}
      <Grid className="page-header">
        <Column lg={12} md={8} sm={4}>
          <h3>
            <FormattedMessage
              id="pathology.page.blocks.title"
              defaultMessage="Block Creation"
            />
          </h3>
          <p className="page-description">
            <FormattedMessage
              id="pathology.page.blocks.description"
              defaultMessage="Create paraffin blocks from processed cassettes."
            />
          </p>
        </Column>
        <Column lg={4} md={8} sm={4} className="header-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            onClick={loadPageSamples}
            disabled={loading}
          >
            <FormattedMessage
              id="pathology.page.refresh"
              defaultMessage="Refresh"
            />
          </Button>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {successMessage && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          })}
          subtitle={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Summary Stats */}
      <Grid className="summary-stats" style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value">{samples.length}</span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.total"
                defaultMessage="Total Samples"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#24a148" }}>
              {completedCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.blocksCreated"
                defaultMessage="Blocks Created"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#f1c21b" }}>
              {pendingCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.pending"
                defaultMessage="Pending"
              />
            </span>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons */}
      {hasValidEntry && samples.length > 0 && (
        <div
          className="action-buttons"
          style={{ marginBottom: "1rem", display: "flex", gap: "0.5rem" }}
        >
          <Button
            kind="primary"
            size="md"
            renderIcon={Add}
            onClick={() => {
              if (selectedSampleIds.length === 1) {
                const sample = samples.find(
                  (s) => s.id === selectedSampleIds[0],
                );
                if (sample) openBlockModal(sample);
              }
            }}
            disabled={selectedSampleIds.length !== 1 || submitting}
          >
            <FormattedMessage
              id="pathology.page.createBlocks"
              defaultMessage="Create Blocks"
            />
          </Button>
          <Button
            kind="secondary"
            size="md"
            renderIcon={Checkmark}
            onClick={handleMarkComplete}
            disabled={selectedSampleIds.length === 0 || submitting}
          >
            <FormattedMessage
              id="pathology.page.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </div>
      )}

      {/* Sample Table */}
      <div className="sample-table">
        {loading ? (
          <Loading description="Loading samples..." withOverlay={false} />
        ) : !hasValidEntry ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.noEntryId"
                defaultMessage="No workflow entry available. Samples will appear here once the workflow is started."
              />
            </p>
          </div>
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.blocks.empty"
                defaultMessage="No samples available for block creation. Samples must have cassettes created first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((s) => ({
              ...s,
              blockStatus:
                s.status === "COMPLETED"
                  ? "Complete"
                  : s.blocksCreated
                    ? "In Progress"
                    : "Pending",
            }))}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              if (
                selectedRows.length !== selectedSampleIds.length ||
                !selectedRows.every((r) => selectedSampleIds.includes(r.id))
              ) {
                setTimeout(() => handleSelectionChange(selectedRows), 0);
              }

              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = samples.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "blockStatus" ? (
                                  <span
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.25rem",
                                      cursor: "pointer",
                                    }}
                                    onClick={() => openBlockModal(sample)}
                                    title="Click to view/edit blocks"
                                  >
                                    {sample?.status === "COMPLETED" ? (
                                      <>
                                        <CheckmarkFilled
                                          size={16}
                                          style={{ color: "#24a148" }}
                                        />
                                        <span>Complete</span>
                                      </>
                                    ) : sample?.blocksCreated ? (
                                      <Tag type="blue" size="sm">
                                        In Progress
                                      </Tag>
                                    ) : (
                                      <Tag type="gray" size="sm">
                                        Pending
                                      </Tag>
                                    )}
                                  </span>
                                ) : cell.info.header === "blockCount" ? (
                                  <span>
                                    {sample?.blockCount > 0
                                      ? sample.blockCount
                                      : "-"}
                                  </span>
                                ) : cell.info.header === "embeddingQuality" ? (
                                  sample?.embeddingQuality ? (
                                    <Tag
                                      type={
                                        sample.embeddingQuality === "excellent"
                                          ? "green"
                                          : sample.embeddingQuality === "good"
                                            ? "teal"
                                            : sample.embeddingQuality ===
                                                "acceptable"
                                              ? "blue"
                                              : "red"
                                      }
                                      size="sm"
                                    >
                                      {sample.embeddingQuality}
                                    </Tag>
                                  ) : (
                                    "-"
                                  )
                                ) : cell.info.header === "qcStatus" ? (
                                  sample?.qcStatus ? (
                                    <Tag
                                      type={
                                        sample.qcStatus === "PASS"
                                          ? "green"
                                          : sample.qcStatus === "FLAG"
                                            ? "yellow"
                                            : sample.qcStatus === "FAIL"
                                              ? "red"
                                              : "gray"
                                      }
                                      size="sm"
                                    >
                                      {sample.qcStatus === "PASS"
                                        ? "✓ Pass"
                                        : sample.qcStatus === "FLAG"
                                          ? "⚠ Flag"
                                          : sample.qcStatus === "FAIL"
                                            ? "✗ Fail"
                                            : "-"}
                                    </Tag>
                                  ) : (
                                    "-"
                                  )
                                ) : (
                                  cell.value || "-"
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          />
        )}
      </div>

      {/* Block Modal */}
      <Modal
        open={blockModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.blocks.title",
            defaultMessage: "Block Creation - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={
          blockViewMode
            ? intl.formatMessage({
                id: "pathology.modal.edit",
                defaultMessage: "Edit",
              })
            : intl.formatMessage({
                id: "pathology.modal.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "pathology.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setBlockModalOpen(false);
          setSelectedSample(null);
          setBlockViewMode(false);
          setError(null);
        }}
        onRequestSubmit={
          blockViewMode ? () => setBlockViewMode(false) : handleSubmitBlocks
        }
        primaryButtonDisabled={submitting || blockLoading}
        size="lg"
        hasScrollingContent
        preventCloseOnClickOutside
      >
        {blockLoading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading description="Loading data..." withOverlay={false} />
          </div>
        ) : (
          <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
            {/* View Mode Banner */}
            {blockViewMode && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "pathology.blocks.viewMode",
                  defaultMessage: "Viewing existing data",
                })}
                subtitle={intl.formatMessage({
                  id: "pathology.blocks.viewMode.description",
                  defaultMessage: "Click 'Edit' to make changes.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {/* Block Configuration */}
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.blocks.section.configuration"
                defaultMessage="Block Configuration"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="numberOfBlocks"
                  name="numberOfBlocks"
                  label={intl.formatMessage({
                    id: "pathology.blocks.numberOfBlocks",
                    defaultMessage: "Number of Blocks",
                  })}
                  value={blockData.numberOfBlocks}
                  min={1}
                  max={50}
                  onChange={(e, { value }) =>
                    setBlockData((prev) => ({
                      ...prev,
                      numberOfBlocks: value,
                    }))
                  }
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="blockPrefix"
                  name="blockPrefix"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.prefix",
                    defaultMessage: "Block ID Prefix",
                  })}
                  value={blockData.blockPrefix}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="embeddingMedium"
                  name="embeddingMedium"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.embeddingMedium",
                    defaultMessage: "Embedding Medium",
                  })}
                  value={blockData.embeddingMedium}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                >
                  {embeddingMediums.map((medium) => (
                    <SelectItem
                      key={medium.value}
                      value={medium.value}
                      text={medium.text}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>

            {/* Block Labels Preview */}
            {blockData.numberOfBlocks > 0 && blockData.blockPrefix && (
              <div style={{ marginTop: "1rem" }}>
                <p style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
                  <FormattedMessage
                    id="pathology.blocks.labelsPreview"
                    defaultMessage="Generated Labels:"
                  />
                </p>
                <div
                  style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}
                >
                  {Array.from(
                    { length: Math.min(blockData.numberOfBlocks, 10) },
                    (_, i) => (
                      <Tag key={i} type="purple" size="sm">
                        {`${blockData.blockPrefix}-B${String(i + 1).padStart(2, "0")}`}
                      </Tag>
                    ),
                  )}
                  {blockData.numberOfBlocks > 10 && (
                    <Tag type="gray" size="sm">
                      +{blockData.numberOfBlocks - 10} more
                    </Tag>
                  )}
                </div>
              </div>
            )}

            {/* Embedding Details */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.blocks.section.embedding"
                defaultMessage="Embedding Details"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="embeddingTemperature"
                  name="embeddingTemperature"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.temperature",
                    defaultMessage: "Temperature (°C)",
                  })}
                  value={blockData.embeddingTemperature}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="embeddingStation"
                  name="embeddingStation"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.station",
                    defaultMessage: "Embedding Station",
                  })}
                  value={blockData.embeddingStation}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="embeddingQuality"
                  name="embeddingQuality"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.quality",
                    defaultMessage: "Embedding Quality",
                  })}
                  value={blockData.embeddingQuality}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                >
                  <SelectItem value="" text="Select quality..." />
                  {qualityOptions.map((opt) => (
                    <SelectItem
                      key={opt.value}
                      value={opt.value}
                      text={opt.text}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>

            {/* Orientation */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.blocks.section.orientation"
                defaultMessage="Tissue Orientation"
              />
            </h5>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="tissueOrientation"
                  name="tissueOrientation"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.tissueOrientation",
                    defaultMessage: "Orientation Description",
                  })}
                  value={blockData.tissueOrientation}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                  placeholder="e.g., Cut surface down, epidermis at 12 o'clock"
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="orientationVerified"
                  name="orientationVerified"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.orientationVerified",
                    defaultMessage: "Orientation Verified",
                  })}
                  checked={blockData.orientationVerified}
                  onChange={(e) =>
                    setBlockData((prev) => ({
                      ...prev,
                      orientationVerified: e.target.checked,
                    }))
                  }
                  disabled={blockViewMode}
                />
              </Column>
            </Grid>

            {/* Staff Info */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.blocks.section.staff"
                defaultMessage="Staff & Date"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianName"
                  name="technicianName"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.technicianName",
                    defaultMessage: "Technician Name",
                  })}
                  value={blockData.technicianName}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianInitials"
                  name="technicianInitials"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.technicianInitials",
                    defaultMessage: "Initials",
                  })}
                  value={blockData.technicianInitials}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="blockDate"
                  name="blockDate"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.date",
                    defaultMessage: "Date",
                  })}
                  value={blockData.blockDate}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                  type="date"
                />
              </Column>
            </Grid>

            {/* Notes */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="notes"
                  name="notes"
                  labelText={intl.formatMessage({
                    id: "pathology.blocks.notes",
                    defaultMessage: "Additional Notes",
                  })}
                  value={blockData.notes}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                  rows={2}
                />
              </Column>
            </Grid>

            {/* Quality Control Section */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.qc.section.title"
                defaultMessage="Quality Control & Assessment"
              />
            </h5>

            {/* QC Status Indicator */}
            <div
              style={{
                padding: "1rem",
                marginBottom: "1rem",
                backgroundColor:
                  blockData.qcStatus === "PASS"
                    ? "#defbe6"
                    : blockData.qcStatus === "FLAG"
                      ? "#fff8e1"
                      : "#fff1f1",
                borderRadius: "4px",
                border: `1px solid ${
                  blockData.qcStatus === "PASS"
                    ? "#24a148"
                    : blockData.qcStatus === "FLAG"
                      ? "#f1c21b"
                      : "#da1e28"
                }`,
              }}
            >
              <Grid>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="qcStatus"
                    name="qcStatus"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.status",
                      defaultMessage: "QC Status",
                    })}
                    value={blockData.qcStatus}
                    onChange={handleBlockInputChange}
                    disabled={blockViewMode}
                  >
                    <SelectItem value="PASS" text="✓ Pass - All criteria met" />
                    <SelectItem
                      value="FLAG"
                      text="⚠ Flag - Proceed with caution"
                    />
                    <SelectItem
                      value="FAIL"
                      text="✗ Fail - Requires corrective action"
                    />
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {blockData.qcStatus === "FLAG" && (
                    <InlineNotification
                      kind="warning"
                      title={intl.formatMessage({
                        id: "pathology.qc.flagged",
                        defaultMessage: "Sample Flagged",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.flagged.description",
                        defaultMessage:
                          "Processing can continue but issue is documented.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                  {blockData.qcStatus === "FAIL" && (
                    <InlineNotification
                      kind="error"
                      title={intl.formatMessage({
                        id: "pathology.qc.failed",
                        defaultMessage: "QC Failed",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.failed.description",
                        defaultMessage:
                          "Corrective action required. Sample is still processed to preserve specimen.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                </Column>
              </Grid>
            </div>

            {/* QC Checkpoints */}
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcEmbeddingQuality"
                  name="qcEmbeddingQuality"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.blocks.embeddingQuality",
                    defaultMessage: "Embedding quality acceptable",
                  })}
                  checked={blockData.qcEmbeddingQuality}
                  onChange={(e) =>
                    setBlockData((prev) => ({
                      ...prev,
                      qcEmbeddingQuality: e.target.checked,
                    }))
                  }
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcOrientationCorrect"
                  name="qcOrientationCorrect"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.blocks.orientationCorrect",
                    defaultMessage: "Tissue orientation correct",
                  })}
                  checked={blockData.qcOrientationCorrect}
                  onChange={(e) =>
                    setBlockData((prev) => ({
                      ...prev,
                      qcOrientationCorrect: e.target.checked,
                    }))
                  }
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcLabelingCorrect"
                  name="qcLabelingCorrect"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.labelingCorrect",
                    defaultMessage: "Labeling correct & complete",
                  })}
                  checked={blockData.qcLabelingCorrect}
                  onChange={(e) =>
                    setBlockData((prev) => ({
                      ...prev,
                      qcLabelingCorrect: e.target.checked,
                    }))
                  }
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcBlockIntegrity"
                  name="qcBlockIntegrity"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.blocks.blockIntegrity",
                    defaultMessage: "Block integrity verified",
                  })}
                  checked={blockData.qcBlockIntegrity}
                  onChange={(e) =>
                    setBlockData((prev) => ({
                      ...prev,
                      qcBlockIntegrity: e.target.checked,
                    }))
                  }
                  disabled={blockViewMode}
                />
              </Column>
            </Grid>

            {/* QC Issues and Corrective Actions */}
            {(blockData.qcStatus === "FLAG" ||
              blockData.qcStatus === "FAIL") && (
              <Grid style={{ marginTop: "1rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcIssues"
                    name="qcIssues"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.issues",
                      defaultMessage: "QC Issues Identified",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.issues.placeholder",
                      defaultMessage: "Describe any quality issues observed...",
                    })}
                    value={blockData.qcIssues}
                    onChange={handleBlockInputChange}
                    disabled={blockViewMode}
                    rows={3}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcCorrectiveAction"
                    name="qcCorrectiveAction"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.correctiveAction",
                      defaultMessage: "Corrective Action Taken",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.correctiveAction.placeholder",
                      defaultMessage:
                        "Describe corrective actions or mitigation steps...",
                    })}
                    value={blockData.qcCorrectiveAction}
                    onChange={handleBlockInputChange}
                    disabled={blockViewMode}
                    rows={3}
                  />
                </Column>
              </Grid>
            )}

            {/* QC Reviewer */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qcReviewedBy"
                  name="qcReviewedBy"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.reviewedBy",
                    defaultMessage: "QC Reviewed By",
                  })}
                  value={blockData.qcReviewedBy}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qcReviewDate"
                  name="qcReviewDate"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.reviewDate",
                    defaultMessage: "QC Review Date",
                  })}
                  value={blockData.qcReviewDate}
                  onChange={handleBlockInputChange}
                  disabled={blockViewMode}
                  type="date"
                />
              </Column>
            </Grid>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default PathologyBlocksPage;
