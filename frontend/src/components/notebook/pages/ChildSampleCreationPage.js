import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  NumberInput,
  TextInput,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Tag,
} from "@carbon/react";
import { Add, ArrowRight, Renew, View } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";
import "../workflow/NotebookWorkflow.css";

/**
 * ChildSampleCreationPage - Page 4 of the immunology workflow.
 * Handles child sample creation (aliquoting) from parent samples.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ChildSampleCreationPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [childSamples, setChildSamples] = useState([]);
  const [selectedParentIds, setSelectedParentIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [childCount, setChildCount] = useState(1);
  const [externalIdPrefix, setExternalIdPrefix] = useState("IMM-C");
  const [creating, setCreating] = useState(false);

  // View children modal
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [parentChildren, setParentChildren] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

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

    // Skip loading for synthetic page IDs
    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Load samples with hierarchy info
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
              // Hierarchy information from backend
              hasChildren: sample.hasChildren || false,
              childAliquotCount: sample.childAliquotCount || 0,
              isAliquot: sample.isAliquot || false,
              nestingLevel: sample.nestingLevel || 0,
              parentSampleItemId: sample.parentSampleItemId,
              parentExternalId: sample.parentExternalId,
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

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle create children modal open
  const handleOpenCreateModal = useCallback(() => {
    if (selectedParentIds.length === 0) {
      setError("Please select at least one parent sample.");
      return;
    }
    setCreateModalOpen(true);
  }, [selectedParentIds]);

  // Handle create children
  const handleCreateChildren = useCallback(() => {
    if (selectedParentIds.length === 0 || !hasRealPageId) return;

    setCreating(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/samples/create-children`,
      JSON.stringify({
        parentSampleIds: selectedParentIds.map((id) => parseInt(id, 10)),
        childCountPerParent: childCount,
        externalIdPrefix: externalIdPrefix,
        pageId: pageData?.id, // Link child samples to this page for SampleRoutingPage
      }),
      (response) => {
        setCreating(false);
        setCreateModalOpen(false);

        if (response && response.success) {
          setSuccess(
            `Successfully created ${response.createdCount} child samples.`,
          );
          setSelectedParentIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to create child samples.");
        }
      },
    );
  }, [
    selectedParentIds,
    hasRealPageId,
    entryId,
    childCount,
    externalIdPrefix,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle view children for a parent
  const handleViewChildren = useCallback((parentSampleId) => {
    setSelectedParentForView(parentSampleId);
    setViewChildrenModalOpen(true);
    setLoadingChildren(true);

    getFromOpenElisServer(
      `/rest/notebook/samples/${parentSampleId}/children`,
      (response) => {
        setLoadingChildren(false);
        if (response && Array.isArray(response)) {
          setParentChildren(response);
        } else {
          setParentChildren([]);
        }
      },
    );
  }, []);

  // Handle status change for aliquoting completion
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
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate],
  );

  // Bulk mark as completed (aliquoting done)
  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedParentIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        "Cannot update status: Page not properly initialized. Please refresh the page.",
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
          loadPageSamples();
          setSelectedParentIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedParentIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Calculate stats
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Custom column for children actions
  const renderChildrenAction = (sample) => {
    return (
      <Button
        kind="ghost"
        size="sm"
        hasIconOnly
        iconDescription="View Children"
        renderIcon={View}
        onClick={() => handleViewChildren(sample.id)}
      />
    );
  };

  return (
    <div className="child-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.childCreation.title"
            defaultMessage="Child Sample Creation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.childCreation.description"
            defaultMessage="Create child samples (aliquots) from parent samples for internal analysis. Select parent samples and specify the number of children to create per parent."
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
                  id="notebook.page.childCreation.totalParents"
                  defaultMessage="Parent Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.childCreation.aliquoted"
                  defaultMessage="Aliquoted"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.childCreation.pending"
                  defaultMessage="Pending Aliquoting"
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
            id="notebook.page.childCreation.createChildren"
            defaultMessage="Create Children ({count} selected)"
            values={{ count: selectedParentIds.length }}
          />
        </Button>

        {selectedParentIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={ArrowRight}
            onClick={handleBulkMarkCompleted}
          >
            <FormattedMessage
              id="notebook.page.childCreation.markCompleted"
              defaultMessage="Mark Aliquoting Complete ({count})"
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
            id="notebook.page.childCreation.refresh"
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

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="child-sample-creation"
          samples={samples}
          selectedIds={selectedParentIds}
          onSelectionChange={setSelectedParentIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          showHierarchy={true}
          loading={loading}
          additionalColumns={[
            {
              key: "children",
              header: "Children",
              render: renderChildrenAction,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.childCreation.empty"
              defaultMessage="No samples available for aliquoting. Please complete the sample reception step first."
            />
          </p>
        </div>
      )}

      {/* Create Children Modal */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.childCreation.modal.title",
          defaultMessage: "Create Child Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.page.childCreation.modal.create",
          defaultMessage: "Create Children",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.page.childCreation.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setCreateModalOpen(false)}
        onRequestSubmit={handleCreateChildren}
        primaryButtonDisabled={creating}
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.childCreation.modal.description"
              defaultMessage="Create child samples from {count} selected parent sample(s)."
              values={{ count: selectedParentIds.length }}
            />
          </p>
        </div>

        <NumberInput
          id="childCount"
          label={intl.formatMessage({
            id: "notebook.page.childCreation.modal.childCount",
            defaultMessage: "Children per Parent",
          })}
          value={childCount}
          onChange={(e, { value }) => setChildCount(value)}
          min={1}
          max={10}
          step={1}
          style={{ marginBottom: "1rem" }}
        />

        <TextInput
          id="externalIdPrefix"
          labelText={intl.formatMessage({
            id: "notebook.page.childCreation.modal.prefix",
            defaultMessage: "External ID Prefix",
          })}
          value={externalIdPrefix}
          onChange={(e) => setExternalIdPrefix(e.target.value)}
          helperText={intl.formatMessage(
            {
              id: "notebook.page.childCreation.modal.prefixHelp",
              defaultMessage:
                "Children will be named: {prefix}-{year}-{sequence}",
            },
            {
              prefix: externalIdPrefix || "PREFIX",
              year: new Date().getFullYear(),
              sequence: "001",
            },
          )}
        />

        <div style={{ marginTop: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.childCreation.modal.total"
              defaultMessage="Total children to create: {total}"
              values={{ total: selectedParentIds.length * childCount }}
            />
          </p>
        </div>
      </Modal>

      {/* View Children Modal */}
      <Modal
        open={viewChildrenModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.childCreation.viewModal.title",
          defaultMessage: "Child Samples",
        })}
        passiveModal
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setParentChildren([]);
        }}
      >
        {loadingChildren ? (
          <p>Loading children...</p>
        ) : parentChildren.length === 0 ? (
          <p>
            <FormattedMessage
              id="notebook.page.childCreation.viewModal.noChildren"
              defaultMessage="No child samples found for this parent."
            />
          </p>
        ) : (
          <DataTable
            rows={parentChildren.map((child) => ({
              id: String(child.id),
              externalId: child.externalId || "-",
              sampleType: child.sampleType || "-",
              status: child.status || "PENDING",
            }))}
            headers={[
              { key: "externalId", header: "External ID" },
              { key: "sampleType", header: "Sample Type" },
              { key: "status", header: "Status" },
            ]}
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
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
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            <Tag
                              type={
                                cell.value === "COMPLETED"
                                  ? "green"
                                  : cell.value === "IN_PROGRESS"
                                    ? "blue"
                                    : "gray"
                              }
                            >
                              {cell.value}
                            </Tag>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        )}
      </Modal>
    </div>
  );
}

export default ChildSampleCreationPage;
