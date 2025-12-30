import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  NumberInput,
  TextInput,
  Modal,
  Tag,
  Select,
  SelectItem,
} from "@carbon/react";
import { Add, Renew, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologyIsolateCreationPage - Page 4 of the Bacteriology workflow.
 * STAGE 4: Isolate Creation
 *
 * Process:
 * - Create bacterial isolates from parent samples
 * - Assign Unique Child Sample Identifier
 * - Link Child ID to Parent Sample ID in system
 * - Document isolate type and description
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID (used for API calls)
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function BacteriologyIsolateCreationPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedParentIds, setSelectedParentIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Create modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);

  // Isolate creation form values
  const [isolateData, setIsolateData] = useState({
    // Number of isolates per parent
    numberOfIsolates: 1,

    // Isolate identification
    externalIdPrefix: "BACT-ISO",
    isolateType: "", // Parent's sample type (preselected)
    isolateTypeFreeText: "", // Custom description
  });

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

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
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              // Hierarchy information
              hasChildren: sample.hasChildren || false,
              childAliquotCount: sample.childAliquotCount || 0,
              isAliquot: sample.isAliquot || false,
              nestingLevel: sample.nestingLevel || 0,
              parentSampleItemId: sample.parentSampleItemId,
              parentExternalId: sample.parentExternalId,
              // Isolate data from JSONB (stored in notes field as JSON)
              isolateTypeFreeText: (() => {
                try {
                  const metadata = sample.data?.notes
                    ? JSON.parse(sample.data.notes)
                    : null;
                  return metadata?.isolateTypeFreeText || "";
                } catch (e) {
                  return "";
                }
              })(),
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

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Reset form
  const resetIsolateData = () => {
    setIsolateData({
      numberOfIsolates: 1,
      externalIdPrefix: "BACT-ISO",
      isolateType: "",
      isolateTypeFreeText: "",
    });
  };

  // Get parent sample type for preselecting isolate type
  const getParentSampleType = useCallback(() => {
    if (selectedParentIds.length === 0) return "";
    const firstSelected = samples.find((s) => s.id === selectedParentIds[0]);
    return firstSelected?.sampleType || "";
  }, [selectedParentIds, samples]);

  // Handle create modal open
  const handleOpenCreateModal = useCallback(() => {
    if (selectedParentIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.isolate.error.noSelection",
          defaultMessage: "Please select at least one parent sample.",
        }),
      );
      return;
    }

    // Preselect isolate type as parent's sample type
    const parentType = getParentSampleType();
    setIsolateData((prev) => ({
      ...prev,
      isolateType: parentType,
    }));

    setCreateModalOpen(true);
  }, [selectedParentIds, intl, getParentSampleType]);

  // Handle create isolates
  const handleCreateIsolates = useCallback(() => {
    if (selectedParentIds.length === 0 || !hasRealPageId) return;

    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.isolate.error.noNotebook",
          defaultMessage: "Notebook ID is required to create isolates.",
        }),
      );
      return;
    }

    setCreating(true);
    setError(null);

    // Build aliquot data for isolates
    // Store isolate-specific data in notes field as JSON
    const isolateMetadata = {
      isolateType: isolateData.isolateType,
      isolateTypeFreeText: isolateData.isolateTypeFreeText,
      parentSampleType: getParentSampleType(),
      createdDate: new Date().toISOString(),
    };

    const aliquotData = {
      aliquotType: isolateData.isolateType,
      notes: JSON.stringify(isolateMetadata),
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/create-children`,
      JSON.stringify({
        parentSampleIds: selectedParentIds.map((id) => parseInt(id, 10)),
        childCountPerParent: isolateData.numberOfIsolates,
        externalIdPrefix: isolateData.externalIdPrefix,
        pageId: pageData?.id,
        aliquotData: aliquotData,
      }),
      (response) => {
        setCreating(false);
        setCreateModalOpen(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.bacteriology.isolate.success.created",
                defaultMessage:
                  "Successfully created {count} isolate(s). Parent-child links established.",
              },
              { count: response.createdCount },
            ),
          );
          setSelectedParentIds([]);
          resetIsolateData();
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.bacteriology.isolate.error.create",
                defaultMessage: "Failed to create isolates.",
              }),
          );
        }
      },
    );
  }, [
    selectedParentIds,
    hasRealPageId,
    notebookId,
    isolateData,
    pageData?.id,
    intl,
    loadPageSamples,
    onProgressUpdate,
    getParentSampleType,
  ]);

  // Bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedParentIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.bacteriology.isolate.error.noPage",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedParentIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.bacteriology.isolate.success.completed",
                defaultMessage:
                  "Marked {count} sample(s) as processing complete.",
              },
              { count: selectedParentIds.length },
            ),
          );
          loadPageSamples();
          setSelectedParentIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.bacteriology.isolate.error.status",
              defaultMessage: "Failed to update sample status.",
            }),
          );
        }
      },
    );
  }, [
    selectedParentIds,
    pageData?.id,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Calculate stats
  // Parent samples are root-level samples (not children of other samples)
  const parentSamples = samples.filter(
    (s) => !s.isAliquot && s.nestingLevel === 0,
  );
  const completedCount = parentSamples.filter(
    (s) => s.status === "COMPLETED",
  ).length;
  const pendingCount = parentSamples.filter(
    (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
  ).length;
  const withIsolatesCount = parentSamples.filter(
    (s) => s.hasChildren || s.childAliquotCount > 0,
  ).length;
  const totalIsolatesCreated = parentSamples.reduce(
    (acc, s) => acc + (s.childAliquotCount || 0),
    0,
  );

  return (
    <div className="bacteriology-isolate-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.isolateCreation.title"
            defaultMessage="Isolate Creation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.isolateCreation.description"
            defaultMessage="Create bacterial isolates from verified samples. Select parent samples and specify isolate type and number of isolates. Parent-child relationships are automatically linked in the system."
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
                  id="notebook.page.bacteriology.isolateCreation.parentSamples"
                  defaultMessage="Parent Samples"
                />
              </span>
              <span className="progress-value">{parentSamples.length}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.isolateCreation.withIsolates"
                  defaultMessage="With Isolates"
                />
              </span>
              <span className="progress-value">{withIsolatesCount}</span>
            </Tile>
            <Tile className="progress-tile success">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.isolateCreation.totalIsolates"
                  defaultMessage="Total Isolates Created"
                />
              </span>
              <span className="progress-value">{totalIsolatesCreated}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.isolateCreation.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.isolateCreation.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Add}
          onClick={handleOpenCreateModal}
          disabled={selectedParentIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.bacteriology.isolateCreation.createIsolates"
            defaultMessage="Create Isolates ({count} selected)"
            values={{ count: selectedParentIds.length }}
          />
        </Button>

        {selectedParentIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkCompleted}
          >
            <FormattedMessage
              id="notebook.page.bacteriology.isolateCreation.markComplete"
              defaultMessage="Mark Processing Complete ({count})"
              values={{ count: selectedParentIds.length }}
            />
          </Button>
        )}

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.page.bacteriology.isolateCreation.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
        />
      )}

      {/* Parent Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.bacteriology.isolateCreation.parentTable.title"
              defaultMessage="Parent Samples"
            />
            <Tag type="gray" className="count-tag">
              {samples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.bacteriology.isolateCreation.parentTable.description"
              defaultMessage="Select parent samples to create bacterial isolates. Each isolate will be linked to its parent in the system."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="bacteriology-isolate-creation"
            samples={samples}
            selectedIds={selectedParentIds}
            onSelectionChange={setSelectedParentIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            showHierarchy={true}
            loading={loading}
            columns={[
              {
                key: "hierarchy",
                header: intl.formatMessage({
                  id: "notebook.sample.hierarchy",
                  defaultMessage: "Hierarchy",
                }),
                render: (value, sample) => {
                  const nestingLevel = sample.nestingLevel || 0;
                  const nestingIndent = nestingLevel * 16;
                  const hasChildren =
                    sample.hasChildren || sample.childAliquotCount > 0;

                  return (
                    <div style={{ display: "flex", alignItems: "center" }}>
                      {nestingLevel > 0 && (
                        <span
                          style={{
                            marginLeft: `${nestingIndent}px`,
                            marginRight: "4px",
                          }}
                        >
                          {"└─"}
                        </span>
                      )}
                      <span
                        style={{
                          marginRight: "4px",
                          fontSize: "16px",
                        }}
                      >
                        {hasChildren ? "📁" : "📄"}
                      </span>
                      {hasChildren && (
                        <span style={{ fontSize: "12px", color: "#525252" }}>
                          ({sample.childAliquotCount || 0})
                        </span>
                      )}
                      {nestingLevel > 0 && sample.parentExternalId && (
                        <span
                          style={{
                            fontSize: "11px",
                            color: "#8d8d8d",
                            marginLeft: "4px",
                          }}
                        >
                          from {sample.parentExternalId}
                        </span>
                      )}
                    </div>
                  );
                },
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession #",
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
                key: "isolateTypeFreeText",
                header: intl.formatMessage({
                  id: "notebook.bacteriology.grid.isolateType",
                  defaultMessage: "Isolate Type",
                }),
                render: (value) => value || "-",
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.status",
                  defaultMessage: "Status",
                }),
                render: (value) => {
                  const status = value || "PENDING";
                  switch (status) {
                    case "COMPLETED":
                      return (
                        <Tag type="green">
                          <FormattedMessage
                            id="notebook.status.completed"
                            defaultMessage="Completed"
                          />
                        </Tag>
                      );
                    case "IN_PROGRESS":
                      return (
                        <Tag type="blue">
                          <FormattedMessage
                            id="notebook.status.inProgress"
                            defaultMessage="In Progress"
                          />
                        </Tag>
                      );
                    case "SKIPPED":
                      return (
                        <Tag type="purple">
                          <FormattedMessage
                            id="notebook.status.skipped"
                            defaultMessage="Skipped"
                          />
                        </Tag>
                      );
                    case "REJECTED":
                      return (
                        <Tag type="red">
                          <FormattedMessage
                            id="notebook.status.rejected"
                            defaultMessage="Rejected"
                          />
                        </Tag>
                      );
                    default:
                      return (
                        <Tag type="gray">
                          <FormattedMessage
                            id="notebook.status.pending"
                            defaultMessage="Pending"
                          />
                        </Tag>
                      );
                  }
                },
              },
            ]}
          />
        </div>
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.bacteriology.isolateCreation.empty"
              defaultMessage="No samples available for isolate creation. Please complete the previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Create Isolates Modal */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.bacteriology.isolateCreation.modal.title",
          defaultMessage: "Create Isolates",
        })}
        primaryButtonText={
          creating
            ? intl.formatMessage({
                id: "notebook.creating",
                defaultMessage: "Creating...",
              })
            : intl.formatMessage({
                id: "notebook.page.bacteriology.isolateCreation.modal.create",
                defaultMessage: "Create Isolates",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "notebook.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setCreateModalOpen(false);
          resetIsolateData();
        }}
        onRequestSubmit={handleCreateIsolates}
        primaryButtonDisabled={creating}
        size="md"
      >
        <div className="bulk-apply-modal-content">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.page.bacteriology.isolateCreation.modal.description"
              defaultMessage="Creating isolates from {count} selected parent sample(s). Configure isolate details below."
              values={{ count: selectedParentIds.length }}
            />
          </p>

          <Grid narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="isolateType"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.isolate.type",
                  defaultMessage: "Isolate Type",
                })}
                value={isolateData.isolateType}
                onChange={(e) =>
                  setIsolateData((prev) => ({
                    ...prev,
                    isolateType: e.target.value,
                  }))
                }
              >
                <SelectItem
                  value={getParentSampleType()}
                  text={getParentSampleType()}
                />
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="isolateTypeFreeText"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.isolate.typeFreeText",
                  defaultMessage: "Isolate Type Description",
                })}
                placeholder={intl.formatMessage({
                  id: "notebook.bacteriology.isolate.typeFreeText.placeholder",
                  defaultMessage: "e.g., E. coli Colony 1",
                })}
                value={isolateData.isolateTypeFreeText}
                onChange={(e) =>
                  setIsolateData((prev) => ({
                    ...prev,
                    isolateTypeFreeText: e.target.value,
                  }))
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <NumberInput
                id="numberOfIsolates"
                label={intl.formatMessage({
                  id: "notebook.bacteriology.isolate.number",
                  defaultMessage: "Number of Isolates",
                })}
                value={isolateData.numberOfIsolates}
                onChange={(e, { value }) =>
                  setIsolateData((prev) => ({
                    ...prev,
                    numberOfIsolates: value,
                  }))
                }
                min={1}
                max={20}
                step={1}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="externalIdPrefix"
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.isolate.prefix",
                  defaultMessage: "External ID Prefix",
                })}
                value={isolateData.externalIdPrefix}
                onChange={(e) =>
                  setIsolateData((prev) => ({
                    ...prev,
                    externalIdPrefix: e.target.value,
                  }))
                }
                helperText={intl.formatMessage(
                  {
                    id: "notebook.bacteriology.isolate.prefixHelp",
                    defaultMessage:
                      "Isolates will be named: {prefix}-{year}-{sequence}",
                  },
                  {
                    prefix: isolateData.externalIdPrefix || "PREFIX",
                    year: new Date().getFullYear(),
                    sequence: "0001",
                  },
                )}
              />
            </Column>
          </Grid>

          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <p style={{ fontWeight: "bold", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.bacteriology.isolate.summary"
                defaultMessage="Summary"
              />
            </p>
            <p>
              <FormattedMessage
                id="notebook.bacteriology.isolate.totalToCreate"
                defaultMessage="Total isolates to create: {total}"
                values={{
                  total:
                    selectedParentIds.length * isolateData.numberOfIsolates,
                }}
              />
            </p>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologyIsolateCreationPage;
