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
  DatePicker,
  DatePickerInput,
  Checkbox,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  NumberInput,
} from "@carbon/react";
import {
  Add,
  ArrowRight,
  Renew,
  View,
  ChevronRight,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologySampleProcessingPage - Page 3 of the pathology workflow.
 * Purpose: Create slides, blocks, aliquots while maintaining traceability.
 * Who uses it: Technicians / pathologists
 */
function PathologySampleProcessingPage({
  entryId,
  pageData,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Processing Modal state
  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Create Children Modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [childCount, setChildCount] = useState(1);
  const [externalIdPrefix, setExternalIdPrefix] = useState("PATH-C");
  const [creating, setCreating] = useState(false);

  // View Children Modal state
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [parentChildren, setParentChildren] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

  // Advance to next page state
  const [advancing, setAdvancing] = useState(false);

  // Processing form state
  const [processingData, setProcessingData] = useState({
    processingAction: "",
    // Histopathology
    grossExamDone: false,
    grossDescription: "",
    sectioningDone: false,
    embeddingDone: false,
    microtomyThickness: 4,
    // Common
    processingDate: "",
    staffInitials: "",
    processingNotes: "",
  });

  // Load samples
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
              specimenCategory: sample.specimenCategory || "histopathology",
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
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

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setProcessingData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleDateChange = (dates, fieldName) => {
    if (dates && dates.length > 0) {
      const date = dates[0];
      const formattedDate = date.toISOString().split("T")[0];
      setProcessingData((prev) => ({
        ...prev,
        [fieldName]: formattedDate,
      }));
    }
  };

  const openProcessingModal = (sample) => {
    setSelectedSample(sample);
    setProcessingData({
      processingAction: "",
      grossExamDone: false,
      grossDescription: "",
      sectioningDone: false,
      embeddingDone: false,
      microtomyThickness: 4,
      processingDate: new Date().toISOString().split("T")[0],
      staffInitials: "",
      processingNotes: "",
    });
    setProcessingModalOpen(true);
  };

  const handleSubmitProcessing = () => {
    if (submitting) return;

    if (
      !processingData.processingAction ||
      !processingData.staffInitials ||
      !processingData.processingDate
    ) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.requiredFields",
          defaultMessage:
            "Please fill in Processing Action, Staff Initials, and Processing Date",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const payload = {
      sampleId: selectedSample?.id,
      pageId: pageData?.id,
      entryId: entryId,
      ...processingData,
    };

    postToOpenElisServer(
      `/rest/notebook/pathology/processing/submit`,
      JSON.stringify(payload),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setProcessingModalOpen(false);
          setSelectedSample(null);
          setSuccess(
            intl.formatMessage({
              id: "pathology.processing.success",
              defaultMessage: "Processing data submitted successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.processing.error.submitFailed",
              defaultMessage:
                "Failed to submit processing data. Please try again.",
            }),
          );
        }
      },
    );
  };

  // Handle create children modal open
  const handleOpenCreateModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.selectSample",
          defaultMessage: "Please select at least one parent sample.",
        }),
      );
      return;
    }
    setCreateModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Handle create children
  const handleCreateChildren = useCallback(() => {
    if (selectedSampleIds.length === 0 || !hasRealPageId) return;

    setCreating(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/samples/create-children`,
      JSON.stringify({
        parentSampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        childCountPerParent: childCount,
        externalIdPrefix: externalIdPrefix,
        pageId: pageData?.id,
      }),
      (response) => {
        setCreating(false);
        setCreateModalOpen(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.processing.childrenCreated",
                defaultMessage: "Successfully created {count} child samples.",
              },
              { count: response.createdCount },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "pathology.processing.error.createChildrenFailed",
                defaultMessage: "Failed to create child samples.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    entryId,
    childCount,
    externalIdPrefix,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
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

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "pathology.processing.error.pageNotInitialized",
            defaultMessage:
              "Cannot update status: Page not properly initialized. Please refresh the page.",
          }),
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
            setError(
              intl.formatMessage({
                id: "pathology.processing.error.statusUpdateFailed",
                defaultMessage:
                  "Failed to update sample status. Please try again.",
              }),
            );
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate, intl],
  );

  // Bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized. Please refresh the page.",
        }),
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
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.processing.bulkCompleted",
                defaultMessage:
                  "Successfully marked {count} samples as completed.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.processing.error.statusUpdateFailed",
              defaultMessage:
                "Failed to update sample status. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Advance completed samples to next page (Page 4: Testing, Staining & Microscopy)
  const handleAdvanceToNextPage = useCallback(() => {
    const completedSamples = samples.filter((s) => s.status === "COMPLETED");
    if (completedSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.noCompletedSamples",
          defaultMessage:
            "No completed samples to advance. Please mark samples as completed first.",
        }),
      );
      return;
    }

    if (!hasRealPageId || !notebookId) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.pageNotInitialized",
          defaultMessage:
            "Cannot advance samples: Page not properly initialized. Please refresh the page.",
        }),
      );
      return;
    }

    setAdvancing(true);
    setError(null);

    const completedSampleIds = completedSamples.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/advance`,
      JSON.stringify({
        sampleIds: completedSampleIds,
        fromPageId: pageData?.id,
        toPageIndex: 4, // Page 4: Testing, Staining & Microscopy
      }),
      (response) => {
        setAdvancing(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.processing.advanceSuccess",
                defaultMessage:
                  "Successfully advanced {count} samples to Testing & Microscopy.",
              },
              { count: response.advancedCount || completedSampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "pathology.processing.error.advanceFailed",
                defaultMessage: "Failed to advance samples to next page.",
              }),
          );
        }
      },
    );
  }, [
    samples,
    hasRealPageId,
    notebookId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Calculate stats
  const processedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  const processingActionOptions = [
    { id: "section_slides", text: "Section to slides" },
    { id: "aliquot_lbc", text: "Aliquot for LBC" },
    { id: "aliquot_cell_block", text: "Aliquot for cell block" },
    { id: "aliquot_molecular", text: "Aliquot for molecular testing" },
    { id: "aliquot_biobank", text: "Aliquot for biobanking" },
  ];

  // Custom column for processing action
  const renderProcessAction = (sample) => {
    return (
      <Button
        kind="ghost"
        size="sm"
        renderIcon={ChevronRight}
        onClick={() => openProcessingModal(sample)}
      >
        <FormattedMessage
          id="pathology.page.processing.process"
          defaultMessage="Process"
        />
      </Button>
    );
  };

  // Custom column for children actions
  const renderChildrenAction = (sample) => {
    return (
      <Button
        kind="ghost"
        size="sm"
        hasIconOnly
        iconDescription={intl.formatMessage({
          id: "pathology.page.processing.viewChildren",
          defaultMessage: "View Children",
        })}
        renderIcon={View}
        onClick={() => handleViewChildren(sample.id)}
      />
    );
  };

  return (
    <div className="pathology-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.processing.title"
            defaultMessage="Sample Processing & Aliquoting"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.processing.description"
            defaultMessage="Create slides, blocks, and aliquots while maintaining traceability. Process samples by type and create child samples."
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
                  id="pathology.page.processing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{processedCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f0ff" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.pending"
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
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="pathology.page.processing.createChildren"
            defaultMessage="Create Children ({count} selected)"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={ArrowRight}
            onClick={handleBulkMarkCompleted}
          >
            <FormattedMessage
              id="pathology.page.processing.markCompleted"
              defaultMessage="Mark Processing Complete ({count})"
              values={{ count: selectedSampleIds.length }}
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
            id="pathology.page.processing.refresh"
            defaultMessage="Refresh"
          />
        </Button>

        {processedCount > 0 && (
          <Button
            kind="primary"
            size="sm"
            renderIcon={ArrowRight}
            onClick={handleAdvanceToNextPage}
            disabled={advancing}
          >
            <FormattedMessage
              id="pathology.page.processing.advanceToNext"
              defaultMessage="Advance to Testing ({count})"
              values={{ count: processedCount }}
            />
          </Button>
        )}
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
          gridId="pathology-sample-processing"
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          showHierarchy={true}
          loading={loading}
          additionalColumns={[
            {
              key: "process",
              header: intl.formatMessage({
                id: "pathology.page.processing.processColumn",
                defaultMessage: "Process",
              }),
              render: renderProcessAction,
            },
            {
              key: "children",
              header: intl.formatMessage({
                id: "pathology.page.processing.childrenColumn",
                defaultMessage: "Children",
              }),
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
              id="pathology.page.processing.empty"
              defaultMessage="No samples available for processing. Samples must pass QC on the previous page first."
            />
          </p>
        </div>
      )}

      {/* Create Children Modal */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.page.processing.createModal.title",
          defaultMessage: "Create Child Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.page.processing.createModal.create",
          defaultMessage: "Create Children",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setCreateModalOpen(false)}
        onRequestSubmit={handleCreateChildren}
        primaryButtonDisabled={creating}
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="pathology.page.processing.createModal.description"
              defaultMessage="Create child samples from {count} selected parent sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        <NumberInput
          id="childCount"
          label={intl.formatMessage({
            id: "pathology.page.processing.createModal.childCount",
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
            id: "pathology.page.processing.createModal.prefix",
            defaultMessage: "External ID Prefix",
          })}
          value={externalIdPrefix}
          onChange={(e) => setExternalIdPrefix(e.target.value)}
          helperText={intl.formatMessage(
            {
              id: "pathology.page.processing.createModal.prefixHelp",
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
              id="pathology.page.processing.createModal.total"
              defaultMessage="Total children to create: {total}"
              values={{ total: selectedSampleIds.length * childCount }}
            />
          </p>
        </div>
      </Modal>

      {/* View Children Modal */}
      <Modal
        open={viewChildrenModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.page.processing.viewModal.title",
          defaultMessage: "Child Samples",
        })}
        passiveModal
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setParentChildren([]);
        }}
      >
        {loadingChildren ? (
          <p>
            <FormattedMessage
              id="pathology.page.processing.viewModal.loading"
              defaultMessage="Loading children..."
            />
          </p>
        ) : parentChildren.length === 0 ? (
          <p>
            <FormattedMessage
              id="pathology.page.processing.viewModal.noChildren"
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
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "pathology.page.processing.viewModal.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "pathology.page.processing.viewModal.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "pathology.page.processing.viewModal.status",
                  defaultMessage: "Status",
                }),
              },
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

      {/* Processing Modal */}
      <Modal
        open={processingModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.processing.title",
            defaultMessage: "Process Sample - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={intl.formatMessage({
          id: "label.button.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setProcessingModalOpen(false);
          setSelectedSample(null);
          setError(null);
        }}
        onRequestSubmit={handleSubmitProcessing}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          {/* Processing Action */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.action"
                defaultMessage="Processing Action"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="processingAction"
              name="processingAction"
              labelText={intl.formatMessage({
                id: "pathology.processing.processingAction",
                defaultMessage: "Processing Action *",
              })}
              value={processingData.processingAction}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="" />
              {processingActionOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>

          {/* Histopathology Processing */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.histopathology"
                defaultMessage="Histopathology Processing"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="grossExamDone"
              name="grossExamDone"
              labelText={intl.formatMessage({
                id: "pathology.processing.grossExamDone",
                defaultMessage: "Gross Examination Done",
              })}
              checked={processingData.grossExamDone}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="sectioningDone"
              name="sectioningDone"
              labelText={intl.formatMessage({
                id: "pathology.processing.sectioningDone",
                defaultMessage: "Sectioning Done",
              })}
              checked={processingData.sectioningDone}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="embeddingDone"
              name="embeddingDone"
              labelText={intl.formatMessage({
                id: "pathology.processing.embeddingDone",
                defaultMessage: "Embedding Done",
              })}
              checked={processingData.embeddingDone}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="microtomyThickness"
              name="microtomyThickness"
              label={intl.formatMessage({
                id: "pathology.processing.microtomyThickness",
                defaultMessage: "Microtomy Thickness (um)",
              })}
              value={processingData.microtomyThickness}
              onChange={(e, { value }) =>
                setProcessingData((prev) => ({
                  ...prev,
                  microtomyThickness: value,
                }))
              }
              min={1}
              max={10}
              step={1}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="grossDescription"
              name="grossDescription"
              labelText={intl.formatMessage({
                id: "pathology.processing.grossDescription",
                defaultMessage: "Gross Description",
              })}
              value={processingData.grossDescription}
              onChange={handleInputChange}
              rows={3}
            />
          </Column>

          {/* Common Fields */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.log"
                defaultMessage="Processing Log"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "processingDate")}
            >
              <DatePickerInput
                id="processingDate"
                labelText={intl.formatMessage({
                  id: "pathology.processing.processingDate",
                  defaultMessage: "Processing Date *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="staffInitials"
              name="staffInitials"
              labelText={intl.formatMessage({
                id: "pathology.processing.staffInitials",
                defaultMessage: "Staff Initials *",
              })}
              value={processingData.staffInitials}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="processingNotes"
              name="processingNotes"
              labelText={intl.formatMessage({
                id: "pathology.processing.notes",
                defaultMessage: "Processing Notes",
              })}
              value={processingData.processingNotes}
              onChange={handleInputChange}
              rows={3}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologySampleProcessingPage;
