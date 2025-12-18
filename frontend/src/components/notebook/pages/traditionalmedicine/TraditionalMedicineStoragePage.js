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
  RadioButtonGroup,
  RadioButton,
  Tag,
  Dropdown,
  Loading,
} from "@carbon/react";
import {
  Checkmark,
  Archive,
  Tree,
  Renew,
  Temperature,
  Edit,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineStoragePage - Page 3 of the Traditional Medicine workflow.
 * Assign proper storage or permanent herbarium placement.
 *
 * Storage Options (per SRS):
 * - Refrigerated (2-8°C) - for samples requiring cold storage
 * - Dried (ambient) - air-dried samples at room temperature
 * - Preserved (chemical/ethanol) - samples in preservation solutions
 * - Frozen (-20°C or colder) - for long-term preservation
 *
 * Storage Location - Uses existing backend hierarchy:
 * - Room (from /rest/storage/rooms)
 * - Device (from /rest/storage/devices?roomId=X)
 * - Shelf (from /rest/storage/shelves?deviceId=X)
 * - Rack (from /rest/storage/racks?shelfId=X)
 * - Position (free text for final placement within rack)
 *
 * Note: Traditional Medicine does NOT use 96-well box assignment.
 * Samples are stored in containers/jars/bags on racks/shelves.
 *
 * Herbarium Placement (for reference specimens):
 * - Herbarium ID - unique identifier for the herbarium collection
 * - Mounted (Yes/No) - whether specimen is mounted on archival paper
 * - Label confirmed (Yes/No) - whether identification label is verified
 * - Catalog number - catalog entry in the herbarium database
 * - Date cataloged - when the specimen was cataloged
 *
 * System Actions:
 * - Location logged with full hierarchical path
 * - Sample status: "In Storage" or "Herbarium Reference"
 */
function TraditionalMedicineStoragePage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Core state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Storage assignment modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);

  // Storage type: 'storage' or 'herbarium'
  const [placementType, setPlacementType] = useState("storage");

  // Hierarchical storage location state (from backend APIs)
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);

  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);
  const [loadingHierarchy, setLoadingHierarchy] = useState(false);

  // Storage form fields
  const [storagePosition, setStoragePosition] = useState("");
  const [storageNotes, setStorageNotes] = useState("");

  // Herbarium form fields (for reference specimens)
  const [herbariumId, setHerbariumId] = useState("");
  const [mounted, setMounted] = useState(null);
  const [labelConfirmed, setLabelConfirmed] = useState(null);
  const [catalogNumber, setCatalogNumber] = useState("");
  const [dateCataloged, setDateCataloged] = useState(
    new Date().toISOString().slice(0, 10),
  );
  const [herbariumNotes, setHerbariumNotes] = useState("");

  // Summary counts
  const [storageSummary, setStorageSummary] = useState({
    pending: 0,
    inStorage: 0,
    herbarium: 0,
    completed: 0,
    total: 0,
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  // Load rooms on mount (for storage hierarchy)
  useEffect(() => {
    loadRooms();
  }, []);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
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
            const transformedSamples = response.map((sample) => {
              const sampleId = String(sample.id || sample.sampleItemId);

              // Determine status based on storage/herbarium assignment
              let status = sample.pageStatus || sample.status || "PENDING";
              const hasStorage = sample.data?.storageRoomId;
              const hasHerbarium = sample.data?.herbariumId;

              if ((hasStorage || hasHerbarium) && status === "PENDING") {
                status = "IN_PROGRESS";
              }

              return {
                id: sampleId,
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                status: status,
                // Sample identification from Page 1
                localName: sample.data?.localName,
                scientificName: sample.data?.scientificName,
                plantPart: sample.data?.plantPart,
                sampleCondition: sample.data?.sampleCondition,
                // Storage data
                storageLocation: sample.data?.storageLocation,
                storageRoomId: sample.data?.storageRoomId,
                storageRoomName: sample.data?.storageRoomName,
                storageDeviceId: sample.data?.storageDeviceId,
                storageDeviceName: sample.data?.storageDeviceName,
                storageShelfId: sample.data?.storageShelfId,
                storageShelfLabel: sample.data?.storageShelfLabel,
                storageRackId: sample.data?.storageRackId,
                storageRackLabel: sample.data?.storageRackLabel,
                storagePosition: sample.data?.storagePosition,
                // Herbarium data
                isHerbariumPlacement: sample.data?.isHerbariumPlacement,
                herbariumId: sample.data?.herbariumId,
                mounted: sample.data?.mounted,
                labelConfirmed: sample.data?.labelConfirmed,
                catalogNumber: sample.data?.catalogNumber,
                dateCataloged: sample.data?.dateCataloged,
              };
            });

            setSamples(transformedSamples);

            // Calculate summary
            const inStorage = transformedSamples.filter(
              (s) => s.storageRoomId && !s.isHerbariumPlacement,
            ).length;
            const herbarium = transformedSamples.filter(
              (s) => s.isHerbariumPlacement && s.herbariumId,
            ).length;
            const completed = transformedSamples.filter(
              (s) => s.status === "COMPLETED",
            ).length;
            const pending = transformedSamples.filter(
              (s) => !s.storageRoomId && !s.herbariumId,
            ).length;

            setStorageSummary({
              pending,
              inStorage,
              herbarium,
              completed,
              total: transformedSamples.length,
            });
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Load rooms from backend
  const loadRooms = () => {
    getFromOpenElisServer("/rest/storage/rooms?status=active", (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setRooms(
            response.map((r) => ({
              id: r.id,
              label: r.name || r.label,
              code: r.code,
              ...r,
            })),
          );
        } else {
          // Handle error or empty response
          console.warn(
            "Storage rooms API returned unexpected response:",
            response,
          );
          setRooms([]);
        }
      }
    });
  };

  // Load devices when room changes
  const handleRoomChange = ({ selectedItem }) => {
    setSelectedRoom(selectedItem);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/devices?roomId=${selectedItem.id}&status=active`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current) {
            if (response && Array.isArray(response)) {
              setDevices(
                response.map((d) => ({
                  id: d.id,
                  label: d.name || d.label,
                  type: d.deviceType || d.type,
                  temperatureSetting: d.temperatureSetting,
                  ...d,
                })),
              );
            } else {
              console.warn(
                "Storage devices API returned unexpected response:",
                response,
              );
              setDevices([]);
            }
          }
        },
      );
    }
  };

  // Load shelves when device changes
  const handleDeviceChange = ({ selectedItem }) => {
    setSelectedDevice(selectedItem);
    setSelectedShelf(null);
    setSelectedRack(null);
    setShelves([]);
    setRacks([]);

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/shelves?deviceId=${selectedItem.id}&status=active`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current) {
            if (response && Array.isArray(response)) {
              setShelves(
                response.map((s) => ({
                  id: s.id,
                  label: s.label || s.name,
                  ...s,
                })),
              );
            } else {
              console.warn(
                "Storage shelves API returned unexpected response:",
                response,
              );
              setShelves([]);
            }
          }
        },
      );
    }
  };

  // Load racks when shelf changes
  const handleShelfChange = ({ selectedItem }) => {
    setSelectedShelf(selectedItem);
    setSelectedRack(null);
    setRacks([]);

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/racks?shelfId=${selectedItem.id}&status=active`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current) {
            if (response && Array.isArray(response)) {
              setRacks(
                response.map((r) => ({
                  id: r.id,
                  label: r.label || r.name,
                  shortCode: r.shortCode,
                  ...r,
                })),
              );
            } else {
              console.warn(
                "Storage racks API returned unexpected response:",
                response,
              );
              setRacks([]);
            }
          }
        },
      );
    }
  };

  // Handle rack change
  const handleRackChange = ({ selectedItem }) => {
    setSelectedRack(selectedItem);
  };

  // Reset modal form
  const resetModalForm = () => {
    setPlacementType("storage");
    setSelectedRoom(null);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);
    setStoragePosition("");
    setStorageNotes("");
    setHerbariumId("");
    setMounted(null);
    setLabelConfirmed(null);
    setCatalogNumber("");
    setDateCataloged(new Date().toISOString().slice(0, 10));
    setHerbariumNotes("");
  };

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.tradmed.storage.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    resetModalForm();
    setStorageModalOpen(true);
    setError(null);
  };

  // Build hierarchical storage location path
  const buildStorageLocationPath = () => {
    const parts = [];
    if (selectedRoom) parts.push(selectedRoom.label);
    if (selectedDevice) parts.push(selectedDevice.label);
    if (selectedShelf) parts.push(selectedShelf.label);
    if (selectedRack) parts.push(selectedRack.label);
    if (storagePosition) parts.push(`Position: ${storagePosition}`);
    return parts.join(" > ");
  };

  // Handle storage assignment
  const handleAssignStorage = () => {
    if (placementType === "herbarium") {
      // Herbarium placement validation
      if (!herbariumId) {
        setError(
          intl.formatMessage({
            id: "notebook.tradmed.storage.error.herbariumIdRequired",
            defaultMessage: "Herbarium ID is required for herbarium placement.",
          }),
        );
        return;
      }

      setAssigning(true);
      setError(null);

      const payload = {
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data: {
          isHerbariumPlacement: true,
          herbariumId: herbariumId,
          mounted: mounted,
          labelConfirmed: labelConfirmed,
          catalogNumber: catalogNumber || null,
          dateCataloged: dateCataloged || null,
          herbariumNotes: herbariumNotes || null,
        },
      };

      postToOpenElisServerJsonResponse(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify(payload),
        (response) => {
          setAssigning(false);
          if (response && response.success) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.tradmed.storage.herbarium.success",
                  defaultMessage:
                    "Assigned {count} sample(s) to herbarium collection.",
                },
                { count: selectedSampleIds.length },
              ),
            );
            setStorageModalOpen(false);
            setSelectedSampleIds([]);
            loadPageSamples();
            if (onProgressUpdate) onProgressUpdate();
          } else {
            setError(
              response?.error ||
                intl.formatMessage({
                  id: "notebook.tradmed.storage.error.assign",
                  defaultMessage: "Failed to assign. Please try again.",
                }),
            );
          }
        },
      );
    } else {
      // Regular storage assignment - validate required fields
      if (!selectedRoom) {
        setError(
          intl.formatMessage({
            id: "notebook.tradmed.storage.error.roomRequired",
            defaultMessage: "Please select a storage room.",
          }),
        );
        return;
      }

      setAssigning(true);
      setError(null);

      const storageLocation = buildStorageLocationPath();

      const payload = {
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data: {
          isHerbariumPlacement: false,
          // Hierarchical location IDs (for reference/lookup)
          storageRoomId: selectedRoom?.id || null,
          storageRoomName: selectedRoom?.label || null,
          storageDeviceId: selectedDevice?.id || null,
          storageDeviceName: selectedDevice?.label || null,
          storageDeviceType: selectedDevice?.type || null,
          storageDeviceTemp: selectedDevice?.temperatureSetting || null,
          storageShelfId: selectedShelf?.id || null,
          storageShelfLabel: selectedShelf?.label || null,
          storageRackId: selectedRack?.id || null,
          storageRackLabel: selectedRack?.label || null,
          // Position and full path
          storagePosition: storagePosition || null,
          storageLocation: storageLocation || null,
          storageNotes: storageNotes || null,
        },
      };

      postToOpenElisServerJsonResponse(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify(payload),
        (response) => {
          setAssigning(false);
          if (response && response.success) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.tradmed.storage.success",
                  defaultMessage: "Assigned {count} sample(s) to storage.",
                },
                { count: selectedSampleIds.length },
              ),
            );
            setStorageModalOpen(false);
            setSelectedSampleIds([]);
            loadPageSamples();
            if (onProgressUpdate) onProgressUpdate();
          } else {
            setError(
              response?.error ||
                intl.formatMessage({
                  id: "notebook.tradmed.storage.error.assign",
                  defaultMessage: "Failed to assign. Please try again.",
                }),
            );
          }
        },
      );
    }
  };

  // Mark samples as storage complete
  const handleMarkComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Check if all selected samples have storage assigned
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingStorage = selectedSamples.filter(
      (s) => !s.storageRoomId && !s.herbariumId,
    );
    if (missingStorage.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.tradmed.storage.error.missingStorage",
            defaultMessage:
              "{count} sample(s) have no storage or herbarium assignment. Please assign first.",
          },
          { count: missingStorage.length },
        ),
      );
      return;
    }

    const numericIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: numericIds,
        status: "COMPLETED",
      }),
      (response) => {
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.tradmed.storage.completed",
                defaultMessage:
                  "Marked {count} sample(s) as Storage Complete. They can proceed to Preparation.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Render storage info as tags
  const renderStorageInfo = (sample) => {
    if (sample.isHerbariumPlacement && sample.herbariumId) {
      return (
        <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
          <Tag type="purple" renderIcon={Tree} size="sm">
            Herbarium: {sample.herbariumId}
          </Tag>
          {sample.catalogNumber && (
            <Tag type="gray" size="sm">
              Cat: {sample.catalogNumber}
            </Tag>
          )}
        </div>
      );
    }
    if (sample.storageRoomId || sample.storageRoomName) {
      // Build location display from hierarchy
      const locationParts = [];
      if (sample.storageRoomName) locationParts.push(sample.storageRoomName);
      if (sample.storageDeviceName)
        locationParts.push(sample.storageDeviceName);
      if (sample.storageShelfLabel)
        locationParts.push(sample.storageShelfLabel);
      if (sample.storageRackLabel) locationParts.push(sample.storageRackLabel);

      return (
        <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
          <Tag type="cyan" renderIcon={Temperature} size="sm">
            {locationParts.join(" > ") || "Stored"}
          </Tag>
          {sample.storagePosition && (
            <Tag type="gray" size="sm">
              {sample.storagePosition}
            </Tag>
          )}
        </div>
      );
    }
    return (
      <Tag type="gray" size="sm">
        Pending
      </Tag>
    );
  };

  return (
    <div className="tradmed-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.storage.title"
            defaultMessage="Sample Storage &amp; Herbarium Placement"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.storage.description"
            defaultMessage="Assign proper storage (refrigerated, dried, preserved, or frozen) or permanent herbarium placement for reference specimens."
          />
        </p>
      </div>

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
      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "label.success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
        />
      )}

      {/* Progress Summary Tiles */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{storageSummary.total}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.storage.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{storageSummary.pending}</span>
            </Tile>
            <Tile className="progress-tile in-progress">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.storage.inStorage"
                  defaultMessage="In Storage"
                />
              </span>
              <span className="progress-value">{storageSummary.inStorage}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.storage.herbarium"
                  defaultMessage="Herbarium"
                />
              </span>
              <span className="progress-value">{storageSummary.herbarium}</span>
            </Tile>
            <Tile className="progress-tile completed">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{storageSummary.completed}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons Bar */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={handleOpenStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.storage.assign"
            defaultMessage="Assign Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.storage.markComplete"
            defaultMessage="Mark Complete ({count})"
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
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={handleSelectionChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "localName", header: "Local Name" },
            { key: "sampleCondition", header: "Condition" },
            {
              key: "storage",
              header: "Storage/Herbarium",
              render: (value, sample) => renderStorageInfo(sample),
            },
            { key: "status", header: "Status" },
          ]}
        />
      )}

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.storage.empty"
              defaultMessage="No samples pending storage assignment. Complete authentication first."
            />
          </p>
        </div>
      )}

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tradmed.storage.modal.title",
          defaultMessage: "Assign Storage or Herbarium Placement",
        })}
        primaryButtonText={
          assigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "label.assign",
                defaultMessage: "Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        size="lg"
        primaryButtonDisabled={assigning}
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.tradmed.storage.modal.description"
              defaultMessage="Assign storage or herbarium placement for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Placement Type Selection */}
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "notebook.tradmed.storage.placementType",
              defaultMessage: "Placement Type",
            })}
            name="placementType"
            valueSelected={placementType}
            onChange={(value) => setPlacementType(value)}
            orientation="horizontal"
          >
            <RadioButton
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.type.storage",
                defaultMessage: "Storage",
              })}
              value="storage"
              id="placement-storage"
            />
            <RadioButton
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.type.herbarium",
                defaultMessage: "Herbarium Reference",
              })}
              value="herbarium"
              id="placement-herbarium"
            />
          </RadioButtonGroup>

          {/* Storage Fields */}
          {placementType === "storage" && (
            <>
              <h5
                style={{
                  marginTop: "0.5rem",
                  display: "flex",
                  alignItems: "center",
                }}
              >
                <Archive size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.tradmed.storage.section.location"
                  defaultMessage="Storage Location"
                />
              </h5>

              {/* Warning if no storage rooms configured */}
              {rooms.length === 0 && (
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage({
                    id: "notebook.tradmed.storage.noRooms.title",
                    defaultMessage: "No Storage Locations",
                  })}
                  subtitle={intl.formatMessage({
                    id: "notebook.tradmed.storage.noRooms.message",
                    defaultMessage:
                      "No storage rooms have been configured. Please set up storage locations in the Storage Management module first.",
                  })}
                  lowContrast
                  hideCloseButton
                  style={{ marginBottom: "1rem" }}
                />
              )}

              {/* Hierarchical Location Selection - Cascading Dropdowns */}
              <Grid fullWidth narrow>
                <Column lg={8} md={4} sm={4}>
                  <Dropdown
                    id="room-dropdown"
                    titleText={intl.formatMessage({
                      id: "notebook.tradmed.storage.room",
                      defaultMessage: "Room *",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.tradmed.storage.selectRoom",
                      defaultMessage: "Select room...",
                    })}
                    items={rooms}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={selectedRoom}
                    onChange={handleRoomChange}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Dropdown
                    id="device-dropdown"
                    titleText={intl.formatMessage({
                      id: "notebook.tradmed.storage.device",
                      defaultMessage: "Storage Device",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.tradmed.storage.selectDevice",
                      defaultMessage: "Select device...",
                    })}
                    items={devices}
                    itemToString={(item) =>
                      item
                        ? `${item.label}${item.temperatureSetting ? ` (${item.temperatureSetting})` : ""}`
                        : ""
                    }
                    selectedItem={selectedDevice}
                    onChange={handleDeviceChange}
                    disabled={!selectedRoom || devices.length === 0}
                  />
                </Column>
              </Grid>

              <Grid fullWidth narrow>
                <Column lg={8} md={4} sm={4}>
                  <Dropdown
                    id="shelf-dropdown"
                    titleText={intl.formatMessage({
                      id: "notebook.tradmed.storage.shelf",
                      defaultMessage: "Shelf",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.tradmed.storage.selectShelf",
                      defaultMessage: "Select shelf...",
                    })}
                    items={shelves}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={selectedShelf}
                    onChange={handleShelfChange}
                    disabled={!selectedDevice || shelves.length === 0}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Dropdown
                    id="rack-dropdown"
                    titleText={intl.formatMessage({
                      id: "notebook.tradmed.storage.rack",
                      defaultMessage: "Rack",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.tradmed.storage.selectRack",
                      defaultMessage: "Select rack...",
                    })}
                    items={racks}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={selectedRack}
                    onChange={handleRackChange}
                    disabled={!selectedShelf || racks.length === 0}
                  />
                </Column>
              </Grid>

              {loadingHierarchy && (
                <Loading withOverlay={false} small description="Loading..." />
              )}

              {/* Helper text for cascading dropdowns */}
              {selectedRoom && devices.length === 0 && !loadingHierarchy && (
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontStyle: "italic",
                  }}
                >
                  <FormattedMessage
                    id="notebook.tradmed.storage.noDevices"
                    defaultMessage="No storage devices configured for this room. You can still assign storage with just the room selected."
                  />
                </p>
              )}

              {selectedDevice && shelves.length === 0 && !loadingHierarchy && (
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontStyle: "italic",
                  }}
                >
                  <FormattedMessage
                    id="notebook.tradmed.storage.noShelves"
                    defaultMessage="No shelves configured for this device. You can continue with just device-level placement."
                  />
                </p>
              )}

              {/* Position (free text for Traditional Medicine - no 96-well boxes) */}
              <TextInput
                id="storage-position"
                labelText={intl.formatMessage({
                  id: "notebook.tradmed.storage.position",
                  defaultMessage: "Position / Container",
                })}
                value={storagePosition}
                onChange={(e) => setStoragePosition(e.target.value)}
                placeholder={intl.formatMessage({
                  id: "notebook.tradmed.storage.positionPlaceholder",
                  defaultMessage:
                    "e.g., Jar A-12, Bag TM-045, Container 3, Row 2",
                })}
                helperText={intl.formatMessage({
                  id: "notebook.tradmed.storage.positionHelper",
                  defaultMessage:
                    "Specify the container or position within the rack",
                })}
              />

              {/* Storage Location Preview */}
              {buildStorageLocationPath() && (
                <div
                  style={{
                    fontSize: "0.875rem",
                    color: "#525252",
                    backgroundColor: "#f4f4f4",
                    padding: "0.5rem",
                    borderRadius: "4px",
                  }}
                >
                  <strong>
                    <FormattedMessage
                      id="notebook.tradmed.storage.locationPreview"
                      defaultMessage="Full Path:"
                    />
                  </strong>{" "}
                  {buildStorageLocationPath()}
                </div>
              )}

              <TextArea
                id="storage-notes"
                labelText={intl.formatMessage({
                  id: "notebook.tradmed.storage.notes",
                  defaultMessage: "Storage Notes",
                })}
                value={storageNotes}
                onChange={(e) => setStorageNotes(e.target.value)}
                rows={2}
                placeholder="Enter any additional storage notes..."
              />
            </>
          )}

          {/* Herbarium Fields */}
          {placementType === "herbarium" && (
            <>
              <h5
                style={{
                  marginTop: "0.5rem",
                  display: "flex",
                  alignItems: "center",
                }}
              >
                <Tree size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.tradmed.storage.section.herbarium"
                  defaultMessage="Herbarium Placement"
                />
              </h5>
              <p style={{ fontSize: "0.75rem", color: "#525252" }}>
                <FormattedMessage
                  id="notebook.tradmed.storage.herbarium.description"
                  defaultMessage="For reference specimens: Mount the specimen on archival paper, attach identification label, and catalog in the herbarium collection."
                />
              </p>

              <Grid fullWidth narrow>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="herbarium-id"
                    labelText={intl.formatMessage({
                      id: "notebook.tradmed.storage.herbariumId",
                      defaultMessage: "Herbarium ID *",
                    })}
                    value={herbariumId}
                    onChange={(e) => setHerbariumId(e.target.value)}
                    placeholder="e.g., ETH-HERB-001"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="catalog-number"
                    labelText={intl.formatMessage({
                      id: "notebook.tradmed.storage.catalogNumber",
                      defaultMessage: "Catalog Number",
                    })}
                    value={catalogNumber}
                    onChange={(e) => setCatalogNumber(e.target.value)}
                    placeholder="e.g., CAT-2025-0001"
                  />
                </Column>
              </Grid>

              <Grid fullWidth narrow>
                <Column lg={5} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.tradmed.storage.mounted",
                      defaultMessage: "Specimen Mounted",
                    })}
                    name="mounted"
                    valueSelected={
                      mounted === true ? "yes" : mounted === false ? "no" : ""
                    }
                    onChange={(value) => setMounted(value === "yes")}
                  >
                    <RadioButton labelText="Yes" value="yes" id="mounted-yes" />
                    <RadioButton labelText="No" value="no" id="mounted-no" />
                  </RadioButtonGroup>
                </Column>
                <Column lg={5} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.tradmed.storage.labelConfirmed",
                      defaultMessage: "Label Confirmed",
                    })}
                    name="labelConfirmed"
                    valueSelected={
                      labelConfirmed === true
                        ? "yes"
                        : labelConfirmed === false
                          ? "no"
                          : ""
                    }
                    onChange={(value) => setLabelConfirmed(value === "yes")}
                  >
                    <RadioButton labelText="Yes" value="yes" id="label-yes" />
                    <RadioButton labelText="No" value="no" id="label-no" />
                  </RadioButtonGroup>
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <TextInput
                    id="date-cataloged"
                    type="date"
                    labelText={intl.formatMessage({
                      id: "notebook.tradmed.storage.dateCataloged",
                      defaultMessage: "Date Cataloged",
                    })}
                    value={dateCataloged}
                    onChange={(e) => setDateCataloged(e.target.value)}
                  />
                </Column>
              </Grid>

              <TextArea
                id="herbarium-notes"
                labelText={intl.formatMessage({
                  id: "notebook.tradmed.storage.herbariumNotes",
                  defaultMessage: "Herbarium Notes",
                })}
                value={herbariumNotes}
                onChange={(e) => setHerbariumNotes(e.target.value)}
                rows={2}
                placeholder="Enter any additional notes about the herbarium specimen..."
              />
            </>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineStoragePage;
