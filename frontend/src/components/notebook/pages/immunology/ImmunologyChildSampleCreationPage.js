import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  NumberInput,
  TextInput,
  TextArea,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Select,
  SelectItem,
  Accordion,
  AccordionItem,
  Checkbox,
} from "@carbon/react";
import {
  Add,
  ArrowRight,
  Renew,
  View,
  Checkmark,
  Chemistry,
  DataBase,
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
 * ImmunologyChildSampleCreationPage - Page 4 of the Immunology workflow.
 * STAGE 4: Child Sample Creation
 *
 * Process:
 * - Extract defined portion from isolated material
 * - Assign Unique Child Sample Identifier
 * - Link Child ID to Parent Sample ID in system
 * - Record extraction volume/quantity
 * - Document remaining parent sample volume
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID (used for API calls)
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ImmunologyChildSampleCreationPage({
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

  // Child sample creation form values
  const [creationValues, setCreationValues] = useState({
    // Number of children per parent
    childCountPerParent: 1,

    // Child sample identification
    externalIdPrefix: "IMM-C",
    childSampleType: "",

    // Extraction details
    extractionVolume: "",
    extractionVolumeUnit: "uL",

    // Parent sample tracking
    trackRemainingVolume: true,
    remainingParentVolume: "",
    remainingParentVolumeUnit: "uL",

    // Notes
    extractionNotes: "",

    // Operator
    operatorName: "",
  });

  // View children modal
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [parentChildren, setParentChildren] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

  // Sample types for children
  const childSampleTypes = useMemo(
    () => [
      {
        value: "aliquot",
        label: intl.formatMessage({
          id: "notebook.immunology.childType.aliquot",
          defaultMessage: "Aliquot",
        }),
      },
      {
        value: "pbmc_fraction",
        label: intl.formatMessage({
          id: "notebook.immunology.childType.pbmcFraction",
          defaultMessage: "PBMC Fraction",
        }),
      },
      {
        value: "serum_fraction",
        label: intl.formatMessage({
          id: "notebook.immunology.childType.serumFraction",
          defaultMessage: "Serum Fraction",
        }),
      },
      {
        value: "plasma_fraction",
        label: intl.formatMessage({
          id: "notebook.immunology.childType.plasmaFraction",
          defaultMessage: "Plasma Fraction",
        }),
      },
      {
        value: "cell_pellet",
        label: intl.formatMessage({
          id: "notebook.immunology.childType.cellPellet",
          defaultMessage: "Cell Pellet",
        }),
      },
      {
        value: "supernatant",
        label: intl.formatMessage({
          id: "notebook.immunology.childType.supernatant",
          defaultMessage: "Supernatant",
        }),
      },
      {
        value: "other",
        label: intl.formatMessage({
          id: "notebook.immunology.childType.other",
          defaultMessage: "Other",
        }),
      },
    ],
    [intl],
  );

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
              patientName: sample.patientName,
              volume: sample.volume,
              // Hierarchy information
              hasChildren: sample.hasChildren || false,
              childAliquotCount: sample.childAliquotCount || 0,
              isAliquot: sample.isAliquot || false,
              nestingLevel: sample.nestingLevel || 0,
              parentSampleItemId: sample.parentSampleItemId,
              parentExternalId: sample.parentExternalId,
              // Child creation data from JSONB
              extractionVolume: sample.data?.extractionVolume,
              remainingParentVolume: sample.data?.remainingParentVolume,
              operatorName: sample.data?.operatorName,
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
  const resetCreationValues = () => {
    setCreationValues({
      childCountPerParent: 1,
      externalIdPrefix: "IMM-C",
      childSampleType: "",
      extractionVolume: "",
      extractionVolumeUnit: "uL",
      trackRemainingVolume: true,
      remainingParentVolume: "",
      remainingParentVolumeUnit: "uL",
      extractionNotes: "",
      operatorName: "",
    });
  };

  // Handle create modal open
  const handleOpenCreateModal = useCallback(() => {
    if (selectedParentIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.child.error.noSelection",
          defaultMessage: "Please select at least one parent sample.",
        }),
      );
      return;
    }
    setCreateModalOpen(true);
  }, [selectedParentIds, intl]);

  // Handle create children
  const handleCreateChildren = useCallback(() => {
    if (selectedParentIds.length === 0 || !hasRealPageId) return;

    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.child.error.noNotebook",
          defaultMessage: "Notebook ID is required to create child samples.",
        }),
      );
      return;
    }

    // Validate required fields
    if (!creationValues.extractionVolume) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.child.error.noVolume",
          defaultMessage: "Extraction volume is required.",
        }),
      );
      return;
    }

    setCreating(true);
    setError(null);

    // Build aliquot data (structure expected by backend)
    // Backend AliquotData expects: volume, volumeUnit, aliquotType, notes, initialVolume, dbsSpots
    // Note: volume and initialVolume must be numbers (Double in Java)
    const aliquotData = {
      volumeUnit: creationValues.extractionVolumeUnit,
      aliquotType: creationValues.childSampleType,
      notes: creationValues.extractionNotes,
    };

    // Only add volume if it's a valid number
    if (creationValues.extractionVolume) {
      const vol = parseFloat(creationValues.extractionVolume);
      if (!isNaN(vol)) {
        aliquotData.volume = vol;
      }
    }

    // Use initialVolume to track remaining parent volume
    if (
      creationValues.trackRemainingVolume &&
      creationValues.remainingParentVolume
    ) {
      const remaining = parseFloat(creationValues.remainingParentVolume);
      if (!isNaN(remaining)) {
        aliquotData.initialVolume = remaining;
      }
    }

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/create-children`,
      JSON.stringify({
        parentSampleIds: selectedParentIds.map((id) => parseInt(id, 10)),
        childCountPerParent: creationValues.childCountPerParent,
        externalIdPrefix: creationValues.externalIdPrefix,
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
                id: "notebook.immunology.child.success.created",
                defaultMessage:
                  "Successfully created {count} child sample(s). Parent-child links established.",
              },
              { count: response.createdCount },
            ),
          );
          setSelectedParentIds([]);
          resetCreationValues();
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.immunology.child.error.create",
                defaultMessage: "Failed to create child samples.",
              }),
          );
        }
      },
    );
  }, [
    selectedParentIds,
    hasRealPageId,
    notebookId,
    creationValues,
    pageData?.id,
    intl,
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

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "notebook.immunology.child.error.noPage",
            defaultMessage:
              "Cannot update status: Page not properly initialized.",
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
                id: "notebook.immunology.child.error.status",
                defaultMessage: "Failed to update sample status.",
              }),
            );
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, intl, loadPageSamples, onProgressUpdate],
  );

  // Bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedParentIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.child.error.noPage",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    // Note: Child sample creation (aliquoting) is optional.
    // Samples can be marked as complete without creating children.

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
                id: "notebook.immunology.child.success.completed",
                defaultMessage:
                  "Marked {count} sample(s) as Child Creation Complete.",
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
              id: "notebook.immunology.child.error.status",
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
    samples,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Calculate stats
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter(
    (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
  ).length;
  const withChildrenCount = samples.filter(
    (s) => s.hasChildren || s.childAliquotCount > 0,
  ).length;
  const totalChildrenCreated = samples.reduce(
    (acc, s) => acc + (s.childAliquotCount || 0),
    0,
  );

  // Render children action column
  const renderChildrenAction = (sample) => {
    const childCount = sample.childAliquotCount || 0;
    return (
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
        {childCount > 0 && (
          <Tag type="blue" size="sm">
            {childCount}
          </Tag>
        )}
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          iconDescription={intl.formatMessage({
            id: "notebook.immunology.child.viewChildren",
            defaultMessage: "View Children",
          })}
          renderIcon={View}
          onClick={() => handleViewChildren(sample.id)}
        />
      </div>
    );
  };

  return (
    <div className="immunology-child-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.immunology.childCreation.title"
            defaultMessage="Child Sample Creation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.immunology.childCreation.description"
            defaultMessage="Extract defined portions from isolated material, assign unique child sample identifiers, and document extraction volumes. Parent-child relationships are automatically linked in the system."
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
                  id="notebook.page.immunology.childCreation.parentSamples"
                  defaultMessage="Parent Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.childCreation.withChildren"
                  defaultMessage="With Children"
                />
              </span>
              <span className="progress-value">{withChildrenCount}</span>
            </Tile>
            <Tile className="progress-tile success">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.childCreation.totalChildren"
                  defaultMessage="Total Children Created"
                />
              </span>
              <span className="progress-value">{totalChildrenCreated}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.childCreation.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.childCreation.pending"
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
            id="notebook.page.immunology.childCreation.createChildren"
            defaultMessage="Create Child Samples ({count} selected)"
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
              id="notebook.page.immunology.childCreation.markComplete"
              defaultMessage="Mark Complete ({count})"
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
            id="notebook.page.immunology.childCreation.refresh"
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
              id="notebook.page.immunology.childCreation.parentTable.title"
              defaultMessage="Parent Samples"
            />
            <Tag type="gray" className="count-tag">
              {samples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.immunology.childCreation.parentTable.description"
              defaultMessage="Select parent samples to create child samples. Each child will be linked to its parent in the system."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="immunology-child-creation"
            samples={samples}
            selectedIds={selectedParentIds}
            onSelectionChange={setSelectedParentIds}
            onStatusChange={handleStatusChange}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            showHierarchy={true}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession #",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.externalId",
                  defaultMessage: "External ID",
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
                key: "extractionVolume",
                header: intl.formatMessage({
                  id: "notebook.grid.extractionVolume",
                  defaultMessage: "Extracted Vol.",
                }),
                render: (value, sample) =>
                  value
                    ? `${value} ${sample.extractionVolumeUnit || "uL"}`
                    : "-",
              },
              {
                key: "remainingParentVolume",
                header: intl.formatMessage({
                  id: "notebook.grid.remainingVolume",
                  defaultMessage: "Remaining Vol.",
                }),
                render: (value, sample) =>
                  value
                    ? `${value} ${sample.remainingParentVolumeUnit || "uL"}`
                    : "-",
              },
              {
                key: "children",
                header: intl.formatMessage({
                  id: "notebook.grid.children",
                  defaultMessage: "Children",
                }),
                render: (_, sample) => renderChildrenAction(sample),
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
              id="notebook.page.immunology.childCreation.empty"
              defaultMessage="No samples available for child creation. Please complete the previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Create Children Modal */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.immunology.childCreation.modal.title",
          defaultMessage: "Create Child Samples",
        })}
        primaryButtonText={
          creating
            ? intl.formatMessage({
                id: "notebook.creating",
                defaultMessage: "Creating...",
              })
            : intl.formatMessage({
                id: "notebook.page.immunology.childCreation.modal.create",
                defaultMessage: "Create Children",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "notebook.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setCreateModalOpen(false);
          resetCreationValues();
        }}
        onRequestSubmit={handleCreateChildren}
        primaryButtonDisabled={creating}
        size="lg"
      >
        <div className="bulk-apply-modal-content">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.page.immunology.childCreation.modal.description"
              defaultMessage="Creating child samples from {count} selected parent sample(s). Configure extraction details below."
              values={{ count: selectedParentIds.length }}
            />
          </p>

          <Accordion>
            {/* Section A: Child Sample Configuration */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <DataBase size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.child.section.config"
                    defaultMessage="A. Child Sample Configuration"
                  />
                </span>
              }
              open
            >
              <Grid narrow>
                <Column lg={4} md={4} sm={4}>
                  <NumberInput
                    id="childCountPerParent"
                    label={intl.formatMessage({
                      id: "notebook.immunology.child.count",
                      defaultMessage: "Children per Parent",
                    })}
                    value={creationValues.childCountPerParent}
                    onChange={(e, { value }) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        childCountPerParent: value,
                      }))
                    }
                    min={1}
                    max={20}
                    step={1}
                  />
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <TextInput
                    id="externalIdPrefix"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.child.prefix",
                      defaultMessage: "External ID Prefix",
                    })}
                    value={creationValues.externalIdPrefix}
                    onChange={(e) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        externalIdPrefix: e.target.value,
                      }))
                    }
                    helperText={intl.formatMessage(
                      {
                        id: "notebook.immunology.child.prefixHelp",
                        defaultMessage:
                          "Children will be named: {prefix}-{year}-{sequence}",
                      },
                      {
                        prefix: creationValues.externalIdPrefix || "PREFIX",
                        year: new Date().getFullYear(),
                        sequence: "001",
                      },
                    )}
                  />
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <Select
                    id="childSampleType"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.child.type",
                      defaultMessage: "Child Sample Type",
                    })}
                    value={creationValues.childSampleType}
                    onChange={(e) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        childSampleType: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select type..." />
                    {childSampleTypes.map((type) => (
                      <SelectItem
                        key={type.value}
                        value={type.value}
                        text={type.label}
                      />
                    ))}
                  </Select>
                </Column>
              </Grid>
            </AccordionItem>

            {/* Section B: Extraction Details */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.child.section.extraction"
                    defaultMessage="B. Extraction Details"
                  />
                </span>
              }
              open
            >
              <Grid narrow>
                <Column lg={4} md={3} sm={2}>
                  <NumberInput
                    id="extractionVolume"
                    label={intl.formatMessage({
                      id: "notebook.immunology.child.extractionVolume",
                      defaultMessage: "Extraction Volume (per child)",
                    })}
                    value={creationValues.extractionVolume}
                    onChange={(e, { value }) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        extractionVolume: value,
                      }))
                    }
                    min={0}
                    step={0.1}
                  />
                </Column>
                <Column lg={2} md={1} sm={2}>
                  <Select
                    id="extractionVolumeUnit"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.unit",
                      defaultMessage: "Unit",
                    })}
                    value={creationValues.extractionVolumeUnit}
                    onChange={(e) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        extractionVolumeUnit: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="uL" text="uL" />
                    <SelectItem value="mL" text="mL" />
                  </Select>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <Checkbox
                    id="trackRemainingVolume"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.child.trackRemaining",
                      defaultMessage: "Track remaining parent sample volume",
                    })}
                    checked={creationValues.trackRemainingVolume}
                    onChange={(_, { checked }) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        trackRemainingVolume: checked,
                      }))
                    }
                  />
                </Column>
                {creationValues.trackRemainingVolume && (
                  <>
                    <Column lg={4} md={3} sm={2}>
                      <NumberInput
                        id="remainingParentVolume"
                        label={intl.formatMessage({
                          id: "notebook.immunology.child.remainingVolume",
                          defaultMessage: "Remaining Parent Volume",
                        })}
                        value={creationValues.remainingParentVolume}
                        onChange={(e, { value }) =>
                          setCreationValues((prev) => ({
                            ...prev,
                            remainingParentVolume: value,
                          }))
                        }
                        min={0}
                        step={0.1}
                      />
                    </Column>
                    <Column lg={2} md={1} sm={2}>
                      <Select
                        id="remainingParentVolumeUnit"
                        labelText={intl.formatMessage({
                          id: "notebook.immunology.unit",
                          defaultMessage: "Unit",
                        })}
                        value={creationValues.remainingParentVolumeUnit}
                        onChange={(e) =>
                          setCreationValues((prev) => ({
                            ...prev,
                            remainingParentVolumeUnit: e.target.value,
                          }))
                        }
                      >
                        <SelectItem value="uL" text="uL" />
                        <SelectItem value="mL" text="mL" />
                      </Select>
                    </Column>
                  </>
                )}
                <Column lg={6} md={4} sm={4}>
                  <TextInput
                    id="operatorName"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.child.operator",
                      defaultMessage: "Operator Name",
                    })}
                    value={creationValues.operatorName}
                    onChange={(e) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        operatorName: e.target.value,
                      }))
                    }
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="extractionNotes"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.child.notes",
                      defaultMessage: "Extraction Notes",
                    })}
                    value={creationValues.extractionNotes}
                    onChange={(e) =>
                      setCreationValues((prev) => ({
                        ...prev,
                        extractionNotes: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.child.notes.placeholder",
                      defaultMessage:
                        "Any additional notes about the extraction process...",
                    })}
                    rows={2}
                  />
                </Column>
              </Grid>
            </AccordionItem>
          </Accordion>

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
                id="notebook.immunology.child.summary"
                defaultMessage="Summary"
              />
            </p>
            <p>
              <FormattedMessage
                id="notebook.immunology.child.totalToCreate"
                defaultMessage="Total children to create: {total}"
                values={{
                  total:
                    selectedParentIds.length *
                    creationValues.childCountPerParent,
                }}
              />
            </p>
            {creationValues.extractionVolume && (
              <p>
                <FormattedMessage
                  id="notebook.immunology.child.totalVolume"
                  defaultMessage="Total volume per parent: {volume} {unit}"
                  values={{
                    volume:
                      creationValues.extractionVolume *
                      creationValues.childCountPerParent,
                    unit: creationValues.extractionVolumeUnit,
                  }}
                />
              </p>
            )}
          </div>
        </div>
      </Modal>

      {/* View Children Modal */}
      <Modal
        open={viewChildrenModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.immunology.childCreation.viewModal.title",
          defaultMessage: "Child Samples",
        })}
        passiveModal
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setParentChildren([]);
        }}
        size="lg"
      >
        {loadingChildren ? (
          <p>
            <FormattedMessage
              id="notebook.loading"
              defaultMessage="Loading..."
            />
          </p>
        ) : parentChildren.length === 0 ? (
          <p>
            <FormattedMessage
              id="notebook.page.immunology.childCreation.viewModal.noChildren"
              defaultMessage="No child samples found for this parent."
            />
          </p>
        ) : (
          <DataTable
            rows={parentChildren.map((child) => ({
              id: String(child.id),
              externalId: child.externalId || "-",
              sampleType: child.sampleType || "-",
              volume: child.volume
                ? `${child.volume} ${child.volumeUnit || "uL"}`
                : "-",
              status: child.status || "PENDING",
            }))}
            headers={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.externalId",
                  defaultMessage: "External ID",
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
                key: "volume",
                header: intl.formatMessage({
                  id: "notebook.grid.volume",
                  defaultMessage: "Volume",
                }),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.status",
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
    </div>
  );
}

export default ImmunologyChildSampleCreationPage;
