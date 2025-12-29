import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Tag,
  TextInput,
  Dropdown,
} from "@carbon/react";
import { Archive, Checkmark, Renew, Location } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBStorageAssignmentPage - Page 3 of the TB workflow.
 * Handles storage assignment for samples that passed QC with PASS_TO_STORAGE result.
 *
 * This page only shows samples where qcResult === "PASS_TO_STORAGE"
 * These samples need to be assigned to storage locations instead of processing.
 *
 * Storage destinations (from QC page):
 * - TEMPORARY_STORAGE: Short-term hold before processing
 * - LONG_TERM_STORAGE: Biorepository/biobank
 * - SHIPMENT: External reference lab transfer
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {Array} props.pages - All workflow pages (to find QC page)
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TBStorageAssignmentPage({
  entryId,
  pageData,
  pages = [],
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Storage hierarchy state
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [boxLayout, setBoxLayout] = useState({});

  // Storage assignment modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [wellAssignments, setWellAssignments] = useState({});

  // Assignment form fields
  const [assignedBy, setAssignedBy] = useState("");
  const [storageCondition, setStorageCondition] = useState("FROZEN_MINUS80"); // Default for TB samples

  // Storage condition options
  const storageConditionOptions = [
    { id: "REFRIGERATED", label: "Refrigerated (2-8°C)" },
    { id: "FROZEN_MINUS20", label: "Frozen (-20°C)" },
    { id: "FROZEN_MINUS80", label: "Frozen (-80°C)" },
    { id: "ROOM_TEMP", label: "Room Temperature (15-25°C)" },
    { id: "LIQUID_NITROGEN", label: "Liquid Nitrogen (-196°C)" },
  ];

  // Summary counts
  const [storageSummary, setStorageSummary] = useState({
    pending: 0,
    assigned: 0,
    completed: 0,
    total: 0,
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples from Page 2 (QC page) where qcResult is stored
  const loadPageSamples = useCallback(() => {
    // Page 2 has order=2 in the TB workflow
    const qcPage = pages.find((p) => p.order === 2);
    const qcPageId = qcPage?.id;

    if (!qcPageId || String(qcPageId).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Load samples from Page 2 (QC page) to get qcResult data
    const url = `/rest/notebook/page/${qcPageId}/samples`;

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          // Filter to only show samples with PASS_TO_STORAGE QC result
          const storageSamples = response.filter(
            (sample) => sample.data?.qcResult === "PASS_TO_STORAGE",
          );

          const transformedSamples = storageSamples.map((sample) => {
            const storageLocation = sample.data?.storageLocation || null;
            const hasStorageAssignment = !!storageLocation;

            // For storage page, status is based on storage assignment, NOT QC pageStatus
            // - PENDING: No storage assigned
            // - IN_PROGRESS: Storage location assigned
            // - COMPLETED: Storage confirmed (needs explicit marking)
            let status = "PENDING";
            if (sample.data?.storageCompleted) {
              status = "COMPLETED";
            } else if (hasStorageAssignment) {
              status = "IN_PROGRESS";
            }

            // Try to get patient name from various possible locations
            const patientName =
              sample.data?.patientName ||
              sample.patientName ||
              (sample.patient
                ? `${sample.patient.firstName || ""} ${sample.patient.lastName || ""}`.trim()
                : null) ||
              "-";

            // Try to get specimen type from various possible locations
            const specimenType =
              sample.data?.specimenType ||
              sample.sampleType ||
              sample.typeOfSample?.description ||
              sample.typeOfSample?.localizedName ||
              "-";

            return {
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: specimenType,
              status: status,
              patientName: patientName,
              patientId: sample.data?.patientId || sample.patientId || "-",
              specimenType: specimenType,
              destination: sample.data?.destination || "-",
              qcResult: sample.data?.qcResult,
              storageLocation: storageLocation,
              storagePath: sample.data?.storagePath || null,
              hasStorageAssignment: hasStorageAssignment,
              data: sample.data,
            };
          });

          setSamples(transformedSamples);

          const assigned = transformedSamples.filter(
            (s) => s.hasStorageAssignment,
          ).length;
          const completed = transformedSamples.filter(
            (s) => s.status === "COMPLETED",
          ).length;
          setStorageSummary({
            pending: transformedSamples.length - assigned,
            assigned: assigned,
            completed: completed,
            total: transformedSamples.length,
          });
        } else {
          setSamples([]);
          setStorageSummary({
            pending: 0,
            assigned: 0,
            completed: 0,
            total: 0,
          });
        }
        setLoading(false);
      }
    });
  }, [pages]);

  // Load samples when pages are available
  useEffect(() => {
    componentMounted.current = true;
    if (pages && pages.length > 0) {
      loadPageSamples();
    }

    return () => {
      componentMounted.current = false;
    };
  }, [pages, loadPageSamples]);

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
    setWellAssignments({});
  }, []);

  // Handle box layout loaded
  const handleBoxLayoutLoaded = useCallback((wells) => {
    setBoxLayout(wells || {});
  }, []);

  // Handle well click from BoxLayoutViewer
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        setError(
          intl.formatMessage(
            {
              id: "notebook.tb.storage.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied. Choose another position.",
            },
            { well: wellCoord },
          ),
        );
        return;
      }

      if (storageModalOpen) {
        // Single well assignment during modal
        const unassignedSamples = selectedSampleIds.filter(
          (id) => !wellAssignments[id],
        );
        if (unassignedSamples.length > 0) {
          setWellAssignments((prev) => ({
            ...prev,
            [unassignedSamples[0]]: wellCoord,
          }));
        }
      }
    },
    [selectedSampleIds, wellAssignments, storageModalOpen, intl],
  );

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    setStorageModalOpen(true);
    setError(null);
    setWellAssignments({});
    setAssignedBy("");
  };

  // Auto-populate wells
  const handleAutoPopulate = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.storage.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      );
      return;
    }

    const rows = storageSelection.box.rows || 8;
    const columns = storageSelection.box.columns || 12;
    const rowLetters = Array.from({ length: rows }, (_, i) =>
      String.fromCharCode("A".charCodeAt(0) + i),
    );

    const newAssignments = {};
    let sampleIndex = 0;

    for (let row of rowLetters) {
      for (let col = 1; col <= columns; col++) {
        if (sampleIndex >= selectedSampleIds.length) break;

        const wellCoord = `${row}${col}`;
        if (!boxLayout[wellCoord]) {
          newAssignments[selectedSampleIds[sampleIndex]] = wellCoord;
          sampleIndex++;
        }
      }
      if (sampleIndex >= selectedSampleIds.length) break;
    }

    setWellAssignments(newAssignments);

    if (sampleIndex < selectedSampleIds.length) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.tb.storage.notEnoughWells",
            defaultMessage:
              "Not enough empty wells. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      );
    } else {
      setError(null);
      setSuccess(
        intl.formatMessage(
          {
            id: "notebook.tb.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Build combined layout for visualization
  const getCombinedLayout = () => {
    const combined = { ...boxLayout };

    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      if (!combined[wellCoord]) {
        const sample = samples.find((s) => s.id === sampleId);
        combined[wellCoord] = {
          sampleItemId: sampleId,
          externalId: sample?.externalId || sampleId,
          pending: true,
        };
      }
    });

    return combined;
  };

  // Handle storage assignment
  const handleAssignStorage = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.storage.noWellAssignments",
          defaultMessage:
            "Please assign samples to wells using Auto-Populate or click on wells.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    // Build wellAssignments map with integer keys (sample IDs)
    const wellAssignmentsForBackend = {};
    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      wellAssignmentsForBackend[parseInt(sampleId, 10)] = wellCoord;
    });

    // Find QC page ID for the pageId field
    const qcPage = pages.find((p) => p.order === 2);

    // Build payload matching backend AssignStorageRequest
    // Known fields: locationId, positionCoordinate, reassign, boxId, wellAssignments,
    //               pageId, condition, sampleIds, locationType, retentionYears
    const payload = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: parseInt(storageSelection.box.id, 10),
      wellAssignments: wellAssignmentsForBackend,
      pageId: qcPage?.id ? parseInt(qcPage.id, 10) : null,
      condition: storageCondition, // Required: REFRIGERATED, FROZEN_MINUS20, FROZEN_MINUS80, ROOM_TEMP, LIQUID_NITROGEN
    };

    postToOpenElisServer(
      `/rest/notebook/${entryId}/samples/assign-storage`,
      JSON.stringify(payload),
      (status) => {
        setAssigning(false);

        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.tb.storage.assignSuccess",
                defaultMessage:
                  "Successfully assigned {count} samples to storage.",
              },
              { count: Object.keys(wellAssignments).length },
            ),
          );
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.tb.storage.assignError",
              defaultMessage: "Failed to assign samples to storage.",
            }),
          );
        }
      },
    );
  };

  // Handle mark samples complete
  const handleMarkComplete = () => {
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.status !== "COMPLETED" &&
        s.hasStorageAssignment,
    );

    if (samplesToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.storage.noSamplesToComplete",
          defaultMessage:
            "No eligible samples to complete. Samples must have storage assigned first.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (status) => {
        setAssigning(false);
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.tb.storage.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: sampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.tb.storage.completeError",
              defaultMessage: "Failed to mark samples complete.",
            }),
          );
        }
      },
    );
  };

  // Render storage tag
  const renderStorageTag = (sample) => {
    if (sample.status === "COMPLETED" && sample.storageLocation) {
      return (
        <Tag type="green" renderIcon={Checkmark}>
          {sample.storageLocation}
        </Tag>
      );
    }
    if (sample.storageLocation) {
      return (
        <Tag type="cyan" renderIcon={Archive}>
          {sample.storageLocation}
        </Tag>
      );
    }
    return (
      <Tag type="gray">
        <FormattedMessage
          id="notebook.status.pending"
          defaultMessage="Pending"
        />
      </Tag>
    );
  };

  // Render destination tag
  const getDestinationTag = (destination) => {
    switch (destination) {
      case "TEMPORARY_STORAGE":
        return <Tag type="teal">Temporary Storage</Tag>;
      case "LONG_TERM_STORAGE":
        return <Tag type="purple">Long-term Storage</Tag>;
      case "SHIPMENT":
        return <Tag type="blue">Shipment</Tag>;
      default:
        return <Tag type="gray">{destination || "-"}</Tag>;
    }
  };

  // Grid columns
  const columns = [
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "notebook.column.externalId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "patientName",
      header: intl.formatMessage({
        id: "notebook.column.patientName",
        defaultMessage: "Patient Name",
      }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({
        id: "notebook.column.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "destination",
      header: intl.formatMessage({
        id: "notebook.column.destination",
        defaultMessage: "Destination",
      }),
      render: (value) => getDestinationTag(value),
    },
    {
      key: "storage",
      header: intl.formatMessage({
        id: "notebook.column.storageLocation",
        defaultMessage: "Storage Location",
      }),
      render: (value, sample) => renderStorageTag(sample),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "notebook.column.status",
        defaultMessage: "Status",
      }),
      render: (value) => {
        if (value === "COMPLETED") {
          return <Tag type="green">Completed</Tag>;
        }
        if (value === "IN_PROGRESS") {
          return <Tag type="blue">Assigned</Tag>;
        }
        return <Tag type="gray">Pending</Tag>;
      },
    },
  ];

  // Empty state for no PASS_TO_STORAGE samples
  if (!loading && samples.length === 0) {
    return (
      <div className="tb-storage-assignment-page">
        <div className="page-section-header">
          <h4>
            <FormattedMessage
              id="notebook.page.tb.storage.title"
              defaultMessage="Sample Storage Assignment"
            />
          </h4>
          <p className="page-description">
            <FormattedMessage
              id="notebook.page.tb.storage.description"
              defaultMessage="Assign samples with PASS_TO_STORAGE QC result to hierarchical storage locations."
            />
          </p>
        </div>

        <Tile style={{ padding: "2rem", textAlign: "center" }}>
          <Archive size={48} style={{ marginBottom: "1rem", opacity: 0.5 }} />
          <p style={{ color: "#525252" }}>
            <FormattedMessage
              id="notebook.tb.storage.noSamples"
              defaultMessage="No samples requiring storage assignment. Samples with PASS_TO_STORAGE QC result from the QC page will appear here."
            />
          </p>
        </Tile>
      </div>
    );
  }

  return (
    <div className="tb-storage-assignment-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tb.storage.title"
            defaultMessage="Sample Storage Assignment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.storage.description"
            defaultMessage="Assign samples with PASS_TO_STORAGE QC result to hierarchical storage locations."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Storage Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.storage.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{storageSummary.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.storage.assigned"
                  defaultMessage="Assigned"
                />
              </span>
              <span className="progress-value">{storageSummary.assigned}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.storage.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{storageSummary.pending}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.storage.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{storageSummary.completed}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Archive}
          onClick={handleOpenStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.tb.storage.assignSelected"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={
            selectedSampleIds.length === 0 || assigning || !hasRealPageId
          }
        >
          <FormattedMessage
            id="notebook.tb.storage.markComplete"
            defaultMessage="Mark Complete"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.tb.storage.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container" style={{ marginTop: "1.5rem" }}>
        <SampleGrid
          samples={samples}
          loading={loading}
          columns={columns}
          onSelectionChange={handleSelectionChange}
          selectedIds={selectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          emptyStateMessage={intl.formatMessage({
            id: "notebook.tb.storage.noSamplesGrid",
            defaultMessage: "No samples requiring storage assignment.",
          })}
        />
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tb.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.tb.storage.modal.assign",
                defaultMessage: "Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        primaryButtonDisabled={
          !storageSelection.box ||
          Object.keys(wellAssignments).length === 0 ||
          assigning
        }
        size="lg"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.tb.storage.modal.description"
            defaultMessage="Assign {count} selected samples to storage. Select a storage location and assign samples to wells."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Storage Location Selection */}
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Location size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.tb.storage.storageLocation"
                  defaultMessage="Storage Location"
                />
              </h5>
              <StorageHierarchySelector
                onSelectionChange={handleStorageSelectionChange}
                entryId={entryId}
                onBoxLayoutLoaded={handleBoxLayoutLoaded}
                boxRequired={true}
                showPath={true}
              />
            </div>
          </Column>

          {/* Box Layout Preview */}
          <Column lg={8} md={4} sm={4}>
            {storageSelection.box ? (
              <div>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "0.5rem",
                  }}
                >
                  <h5>
                    <Archive size={16} style={{ marginRight: "0.5rem" }} />
                    <FormattedMessage
                      id="notebook.tb.storage.boxLayout"
                      defaultMessage="Box Layout"
                    />
                  </h5>
                  <Button
                    kind="tertiary"
                    size="sm"
                    onClick={handleAutoPopulate}
                    disabled={selectedSampleIds.length === 0}
                  >
                    <FormattedMessage
                      id="notebook.tb.storage.autoPopulate"
                      defaultMessage="Auto-Populate"
                    />
                  </Button>
                </div>

                <BoxLayoutViewer
                  boxId={storageSelection.box.id}
                  layout={getCombinedLayout()}
                  rows={storageSelection.box.rows || 8}
                  columns={storageSelection.box.columns || 12}
                  onWellClick={handleWellClick}
                />

                <div
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  <FormattedMessage
                    id="notebook.tb.storage.assignmentSummary"
                    defaultMessage="{assigned} of {total} samples assigned to wells"
                    values={{
                      assigned: Object.keys(wellAssignments).length,
                      total: selectedSampleIds.length,
                    }}
                  />
                </div>
              </div>
            ) : (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                }}
              >
                <Archive size={32} />
                <p style={{ marginTop: "0.5rem", color: "#525252" }}>
                  <FormattedMessage
                    id="notebook.tb.storage.selectBoxFirst"
                    defaultMessage="Select a storage location to preview box layout"
                  />
                </p>
              </div>
            )}
          </Column>

          {/* Assignment Details */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="storage-condition"
              titleText={intl.formatMessage({
                id: "notebook.tb.storage.condition",
                defaultMessage: "Storage Condition",
              })}
              label={intl.formatMessage({
                id: "notebook.tb.storage.selectCondition",
                defaultMessage: "Select condition",
              })}
              items={storageConditionOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={storageConditionOptions.find(
                (opt) => opt.id === storageCondition,
              )}
              onChange={({ selectedItem }) =>
                setStorageCondition(selectedItem?.id || "FROZEN_MINUS80")
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="assigned-by"
              labelText={intl.formatMessage({
                id: "notebook.tb.storage.assignedBy",
                defaultMessage: "Assigned By (optional)",
              })}
              value={assignedBy}
              onChange={(e) => setAssignedBy(e.target.value)}
              placeholder="Enter staff name"
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TBStorageAssignmentPage;
