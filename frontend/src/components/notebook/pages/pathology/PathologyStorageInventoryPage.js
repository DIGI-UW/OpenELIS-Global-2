import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  Loading,
  TextInput,
  DatePicker,
  DatePickerInput,
  Modal,
  NumberInput,
  Dropdown,
} from "@carbon/react";
import {
  Archive,
  Temperature,
  Undo,
  Checkmark,
  Renew,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyStorageInventoryPage - Page 5 of the pathology workflow.
 * Purpose: Track physical storage and retrieval of pathology materials.
 * Who uses it: Store manager / lab staff
 *
 * Features:
 * - Hierarchical storage selection (Room → Device → Shelf → Rack → Box)
 * - 96-well grid visualization with auto-populate functionality
 * - Storage condition and retention period tracking
 * - Temperature monitoring (AM/PM checks)
 * - Sample retrieval with signature tracking
 */
function PathologyStorageInventoryPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Storage assignment modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);
  const [isReassignment, setIsReassignment] = useState(false);

  // Hierarchical storage selection state
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);
  const [boxes, setBoxes] = useState([]);

  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);
  const [selectedBox, setSelectedBox] = useState(null);

  const [loadingHierarchy, setLoadingHierarchy] = useState(false);

  // Box layout state
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});

  // Storage form fields
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [expectedDuration, setExpectedDuration] = useState("");

  // Temperature log modal state
  const [tempLogModalOpen, setTempLogModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [tempLogData, setTempLogData] = useState({
    storageUnit: "",
    temperatureCheckAM: 4,
    temperatureCheckPM: 4,
    checkedBy: "",
    checkDate: "",
  });

  // Retrieval modal state
  const [retrievalModalOpen, setRetrievalModalOpen] = useState(false);
  const [selectedSampleForRetrieval, setSelectedSampleForRetrieval] =
    useState(null);
  const [retrievalData, setRetrievalData] = useState({
    dateRetrieved: "",
    retrievedBy: "",
    recipientSignature: "",
  });

  // Storage condition options matching backend StorageCondition enum
  const storageConditionOptions = [
    {
      id: "ROOM_TEMP",
      label: intl.formatMessage({
        id: "pathology.storage.condition.roomTemp",
        defaultMessage: "Room Temperature Cabinet (15-25°C)",
      }),
    },
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({
        id: "pathology.storage.condition.refrigerated",
        defaultMessage: "Refrigerated (2-8°C)",
      }),
    },
    {
      id: "FROZEN_MINUS20",
      label: intl.formatMessage({
        id: "pathology.storage.condition.frozen20",
        defaultMessage: "Frozen (-20°C)",
      }),
    },
    {
      id: "FROZEN_MINUS80",
      label: intl.formatMessage({
        id: "pathology.storage.condition.frozen80",
        defaultMessage: "Frozen (-80°C)",
      }),
    },
    {
      id: "LIQUID_NITROGEN",
      label: intl.formatMessage({
        id: "pathology.storage.condition.liquidNitrogen",
        defaultMessage: "LN2 Vapor (-196°C)",
      }),
    },
    {
      id: "SLIDE_BOX",
      label: intl.formatMessage({
        id: "pathology.storage.condition.slideBox",
        defaultMessage: "Slide Box",
      }),
    },
  ];

  // Summary counts
  const [storageSummary, setStorageSummary] = useState({
    pending: 0,
    stored: 0,
    total: 0,
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  // Load rooms on mount
  useEffect(() => {
    loadRooms();
  }, []);

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
            const transformedSamples = response.map((sample) => {
              const sampleId = String(sample.id || sample.sampleItemId);

              // Get storage location from sample data
              const storageLocation = sample.data?.storageLocation || null;
              const storageCondition = sample.data?.storageCondition || null;

              // Determine status
              let status = sample.pageStatus || "PENDING";
              if (storageLocation && status === "PENDING") {
                status = "IN_PROGRESS";
              }

              return {
                id: sampleId,
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                specimenType:
                  sample.sampleType || sample.typeOfSample?.description,
                collectionDate: sample.collectionDate,
                status: status,
                storageLocation: storageLocation,
                storageCondition: storageCondition,
                retentionExpiry: sample.data?.retentionExpiry || null,
                boxId: sample.data?.boxId || null,
                wellCoordinate: sample.data?.wellCoordinate || null,
                data: sample.data,
              };
            });

            setSamples(transformedSamples);

            // Calculate summary
            const stored = transformedSamples.filter(
              (s) => s.storageLocation || s.status === "COMPLETED",
            ).length;
            setStorageSummary({
              pending: transformedSamples.length - stored,
              stored: stored,
              total: transformedSamples.length,
            });
          } else {
            setSamples([]);
            setStorageSummary({ pending: 0, stored: 0, total: 0 });
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Load rooms
  const loadRooms = () => {
    getFromOpenElisServer("/rest/storage/rooms?status=active", (response) => {
      if (componentMounted.current && response && Array.isArray(response)) {
        setRooms(
          response.map((r) => ({
            id: r.id,
            label: r.name,
            ...r,
          })),
        );
      }
    });
  };

  // Load devices when room changes
  const handleRoomChange = ({ selectedItem }) => {
    setSelectedRoom(selectedItem);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/devices?roomId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setDevices(
              response.map((d) => ({
                id: d.id,
                label: d.name,
                ...d,
              })),
            );
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
    setSelectedBox(null);
    setShelves([]);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/shelves?deviceId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setShelves(
              response.map((s) => ({
                id: s.id,
                label: s.label || s.name,
                ...s,
              })),
            );
          }
        },
      );
    }
  };

  // Load racks when shelf changes
  const handleShelfChange = ({ selectedItem }) => {
    setSelectedShelf(selectedItem);
    setSelectedRack(null);
    setSelectedBox(null);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/racks?shelfId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setRacks(
              response.map((r) => ({
                id: r.id,
                label: r.label || r.name,
                ...r,
              })),
            );
          }
        },
      );
    }
  };

  // Load boxes when rack changes
  const handleRackChange = ({ selectedItem }) => {
    setSelectedRack(selectedItem);
    setSelectedBox(null);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/boxes?rackId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setBoxes(
              response.map((b) => ({
                id: b.id,
                label: b.label || b.name,
                rows: b.rows || 8,
                columns: b.columns || 12,
                ...b,
              })),
            );
          }
        },
      );
    }
  };

  // Load box layout when box changes
  const handleBoxChange = ({ selectedItem }) => {
    setSelectedBox(selectedItem);
    setBoxLayout({});
    setWellAssignments({});

    if (selectedItem && entryId) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/notebook/${entryId}/box/${selectedItem.id}/layout`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response) {
            const layoutData = response.wells || {};
            setBoxLayout(layoutData);
          }
        },
      );
    }
  };

  // Handle selection change
  const handleSelectionChange = useCallback((selectedIds) => {
    setSelectedSampleIds(selectedIds.map(String));
  }, []);

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    // Check if any selected samples already have storage assignments
    const alreadyAssigned = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.storageLocation,
    );

    if (alreadyAssigned.length > 0) {
      setSamplesToReassign(alreadyAssigned);
      setConfirmReassignModalOpen(true);
      return;
    }

    openStorageAssignmentModal(false);
  };

  // Open the storage assignment modal
  const openStorageAssignmentModal = (reassigning) => {
    setIsReassignment(reassigning);
    setStorageModalOpen(true);
    setError(null);
    setSelectedRoom(null);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);
    setBoxes([]);
    setBoxLayout({});
    setWellAssignments({});
    setSelectedCondition(null);
    setRetentionYears(5);
    setExpectedDuration("");
  };

  // Handle confirmation of reassignment
  const handleConfirmReassignment = () => {
    setConfirmReassignModalOpen(false);
    openStorageAssignmentModal(true);
  };

  // Auto-populate wells
  const handleAutoPopulate = () => {
    if (!selectedBox) return;

    const rows = selectedBox.rows || 8;
    const columns = selectedBox.columns || 12;
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
            id: "pathology.storage.notEnoughWells",
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
            id: "pathology.storage.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Handle well click
  const handleWellClick = (wellCoord, wellInfo) => {
    if (wellInfo) {
      return;
    }

    const unassignedSamples = selectedSampleIds.filter(
      (id) => !wellAssignments[id],
    );
    if (unassignedSamples.length > 0) {
      setWellAssignments((prev) => ({
        ...prev,
        [unassignedSamples[0]]: wellCoord,
      }));
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
          externalId: sample?.externalId || sample?.accessionNumber || sampleId,
          pending: true,
        };
      }
    });

    return combined;
  };

  // Handle storage assignment
  const handleAssignStorage = () => {
    if (!selectedBox) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectBox",
          defaultMessage: "Please select a storage box.",
        }),
      );
      return;
    }
    if (!selectedCondition) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      );
      return;
    }
    if (Object.keys(wellAssignments).length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noWellAssignments",
          defaultMessage:
            "Please assign samples to wells using Auto-Populate or click on wells.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const wellAssignmentsForBackend = {};
    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      wellAssignmentsForBackend[parseInt(sampleId, 10)] = wellCoord;
    });

    const payload = {
      sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
      boxId: selectedBox.id,
      wellAssignments: wellAssignmentsForBackend,
      condition: selectedCondition.id,
      retentionYears: retentionYears,
      expectedDuration: expectedDuration,
      reassign: isReassignment,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/samples/assign-storage`,
      JSON.stringify(payload),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          const messageId = isReassignment
            ? "pathology.storage.reassignSuccess"
            : "pathology.storage.assignSuccess";
          const defaultMessage = isReassignment
            ? "Successfully reassigned {count} samples to new storage location."
            : "Successfully assigned {count} samples to storage.";

          setSuccess(
            intl.formatMessage(
              {
                id: messageId,
                defaultMessage: defaultMessage,
              },
              {
                count:
                  response.assignedCount || Object.keys(wellAssignments).length,
              },
            ),
          );
          setIsReassignment(false);
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "pathology.storage.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          );
        }
      },
    );
  };

  // Handle mark complete
  const handleMarkComplete = () => {
    const pendingSamples = samples.filter(
      (s) => s.status !== "COMPLETED" && s.storageLocation,
    );

    if (pendingSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.storage.noStoredSamples",
          defaultMessage:
            "No stored samples to mark complete. Assign storage first.",
        }),
      );
      return;
    }

    setAssigning(true);
    setError(null);

    const sampleIds = pendingSamples.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setAssigning(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.storage.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to mark samples complete.");
        }
      },
    );
  };

  // Temperature log handlers
  const handleInputChange = (e, setState) => {
    const { name, value } = e.target;
    setState((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName, setState) => {
    if (dates?.[0]) {
      setState((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  const handleSubmitTempLog = () => {
    if (submitting) return;
    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/temperature-log`,
      JSON.stringify({ pageId: pageData?.id, ...tempLogData }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setTempLogModalOpen(false);
          setSuccess(
            intl.formatMessage({
              id: "pathology.storage.tempLogSuccess",
              defaultMessage: "Temperature logged successfully.",
            }),
          );
        } else {
          setError("Failed to log temperature. Please try again.");
        }
      },
    );
  };

  // Retrieval handlers
  const openRetrievalModal = (sample) => {
    setSelectedSampleForRetrieval(sample);
    setRetrievalData({
      dateRetrieved: new Date().toISOString().split("T")[0],
      retrievedBy: "",
      recipientSignature: "",
    });
    setRetrievalModalOpen(true);
  };

  const handleSubmitRetrieval = () => {
    if (submitting) return;
    if (!retrievalData.retrievedBy || !retrievalData.recipientSignature) {
      setError("Please fill in Retrieved By and Recipient Signature");
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/retrieve`,
      JSON.stringify({
        sampleId: selectedSampleForRetrieval?.id,
        pageId: pageData?.id,
        ...retrievalData,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setRetrievalModalOpen(false);
          setSuccess(
            intl.formatMessage({
              id: "pathology.storage.retrievalSuccess",
              defaultMessage: "Sample retrieval recorded successfully.",
            }),
          );
          loadPageSamples();
          onProgressUpdate?.();
        } else {
          setError("Failed to record retrieval. Please try again.");
        }
      },
    );
  };

  // Render storage status tag
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

  // Render condition tag
  const renderConditionTag = (sample) => {
    if (!sample.storageCondition) return null;

    const conditionLabels = {
      REFRIGERATED: "2-8°C",
      FROZEN_MINUS20: "-20°C",
      FROZEN_MINUS80: "-80°C",
      ROOM_TEMP: "15-25°C",
      LIQUID_NITROGEN: "-196°C",
      SLIDE_BOX: "Slide Box",
    };

    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {conditionLabels[sample.storageCondition] || sample.storageCondition}
      </Tag>
    );
  };

  // Build hierarchical path
  const getHierarchicalPath = () => {
    const parts = [];
    if (selectedRoom) parts.push(selectedRoom.label);
    if (selectedDevice) parts.push(selectedDevice.label);
    if (selectedShelf) parts.push(selectedShelf.label);
    if (selectedRack) parts.push(selectedRack.label);
    if (selectedBox) parts.push(selectedBox.label);
    return parts.join(" > ");
  };

  // Grid columns
  const columns = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.column.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({
        id: "pathology.column.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "storage",
      header: intl.formatMessage({
        id: "pathology.column.storage",
        defaultMessage: "Storage Status",
      }),
      render: (sample) => (
        <div style={{ display: "flex", gap: "4px", alignItems: "center" }}>
          {renderStorageTag(sample)}
          {renderConditionTag(sample)}
        </div>
      ),
    },
    {
      key: "retentionExpiry",
      header: intl.formatMessage({
        id: "pathology.column.expiry",
        defaultMessage: "Retention Expiry",
      }),
      render: (sample) =>
        sample.retentionExpiry ? (
          <span>{sample.retentionExpiry}</span>
        ) : (
          <span className="text-muted">-</span>
        ),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "pathology.column.actions",
        defaultMessage: "Actions",
      }),
      render: (sample) =>
        sample.status === "COMPLETED" && sample.storageLocation ? (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Undo}
            onClick={(e) => {
              e.stopPropagation();
              openRetrievalModal(sample);
            }}
          >
            <FormattedMessage
              id="pathology.storage.retrieve"
              defaultMessage="Retrieve"
            />
          </Button>
        ) : null,
    },
  ];

  return (
    <div className="pathology-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.storage.title"
            defaultMessage="Storage & Inventory Management"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.storage.description"
            defaultMessage="Track physical storage and retrieval of pathology materials. Assign locations and log environmental monitoring."
          />
        </p>
      </div>

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
          lowContrast
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
        />
      )}

      {/* Storage Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{storageSummary.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.stored"
                  defaultMessage="Stored"
                />
              </span>
              <span className="progress-value">{storageSummary.stored}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.storage.pending"
                  defaultMessage="Pending Storage"
                />
              </span>
              <span className="progress-value">{storageSummary.pending}</span>
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
            id="pathology.storage.assignSelected"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={storageSummary.stored === 0 || assigning || !hasRealPageId}
        >
          <FormattedMessage
            id="pathology.storage.markComplete"
            defaultMessage="Mark Stored Samples Complete"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Temperature}
          onClick={() => setTempLogModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.storage.logTemperature"
            defaultMessage="Log Temperature"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <SampleGrid
        samples={samples}
        loading={loading}
        columns={columns}
        onSelectionChange={handleSelectionChange}
        selectedIds={selectedSampleIds}
        statusFilter={statusFilter}
        onStatusFilterChange={setStatusFilter}
        emptyStateMessage={intl.formatMessage({
          id: "pathology.storage.noSamples",
          defaultMessage: "No samples available for storage assignment.",
        })}
      />

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.modal.assign",
          defaultMessage: "Assign",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        primaryButtonDisabled={
          !selectedBox ||
          !selectedCondition ||
          Object.keys(wellAssignments).length === 0 ||
          assigning
        }
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="pathology.storage.modal.description"
              defaultMessage="Assign {count} selected samples to storage."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Hierarchical Storage Selection */}
          <div className="storage-hierarchy-selection">
            <Grid fullWidth narrow>
              <Column lg={8} md={4} sm={4}>
                <Dropdown
                  id="room-dropdown"
                  titleText={intl.formatMessage({
                    id: "pathology.storage.room",
                    defaultMessage: "Room",
                  })}
                  label={intl.formatMessage({
                    id: "pathology.storage.selectRoom",
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
                    id: "pathology.storage.device",
                    defaultMessage: "Device/Cabinet",
                  })}
                  label={intl.formatMessage({
                    id: "pathology.storage.selectDevice",
                    defaultMessage: "Select device...",
                  })}
                  items={devices}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedDevice}
                  onChange={handleDeviceChange}
                  disabled={!selectedRoom}
                />
              </Column>
            </Grid>

            <Grid fullWidth narrow style={{ marginTop: "0.5rem" }}>
              <Column lg={5} md={3} sm={4}>
                <Dropdown
                  id="shelf-dropdown"
                  titleText={intl.formatMessage({
                    id: "pathology.storage.shelf",
                    defaultMessage: "Shelf",
                  })}
                  label={intl.formatMessage({
                    id: "pathology.storage.selectShelf",
                    defaultMessage: "Select shelf...",
                  })}
                  items={shelves}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedShelf}
                  onChange={handleShelfChange}
                  disabled={!selectedDevice}
                />
              </Column>
              <Column lg={5} md={3} sm={4}>
                <Dropdown
                  id="rack-dropdown"
                  titleText={intl.formatMessage({
                    id: "pathology.storage.rack",
                    defaultMessage: "Rack",
                  })}
                  label={intl.formatMessage({
                    id: "pathology.storage.selectRack",
                    defaultMessage: "Select rack...",
                  })}
                  items={racks}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedRack}
                  onChange={handleRackChange}
                  disabled={!selectedShelf}
                />
              </Column>
              <Column lg={6} md={2} sm={4}>
                <Dropdown
                  id="box-dropdown"
                  titleText={intl.formatMessage({
                    id: "pathology.storage.box",
                    defaultMessage: "Box",
                  })}
                  label={intl.formatMessage({
                    id: "pathology.storage.selectBox",
                    defaultMessage: "Select box...",
                  })}
                  items={boxes}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={selectedBox}
                  onChange={handleBoxChange}
                  disabled={!selectedRack}
                />
              </Column>
            </Grid>

            {/* Hierarchical Path Display */}
            {getHierarchicalPath() && (
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                  backgroundColor: "#f4f4f4",
                  padding: "0.5rem",
                  borderRadius: "4px",
                }}
              >
                <strong>
                  <FormattedMessage
                    id="pathology.storage.path"
                    defaultMessage="Path:"
                  />
                </strong>{" "}
                {getHierarchicalPath()}
              </div>
            )}
          </div>

          {/* Box Layout Viewer */}
          {selectedBox && (
            <div style={{ marginTop: "1rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "0.5rem",
                }}
              >
                <h5>
                  <FormattedMessage
                    id="pathology.storage.boxLayout"
                    defaultMessage="Box Layout"
                  />
                </h5>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Renew}
                  onClick={handleAutoPopulate}
                  disabled={selectedSampleIds.length === 0}
                >
                  <FormattedMessage
                    id="pathology.storage.autoPopulate"
                    defaultMessage="Auto-Populate"
                  />
                </Button>
              </div>

              {loadingHierarchy ? (
                <Loading withOverlay={false} small />
              ) : (
                <BoxLayoutViewer
                  boxId={selectedBox.id}
                  layout={getCombinedLayout()}
                  rows={selectedBox.rows || 8}
                  columns={selectedBox.columns || 12}
                  onWellClick={handleWellClick}
                />
              )}

              {/* Assignment Summary */}
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="pathology.storage.assignmentSummary"
                  defaultMessage="{assigned} of {total} samples assigned to wells"
                  values={{
                    assigned: Object.keys(wellAssignments).length,
                    total: selectedSampleIds.length,
                  }}
                />
              </div>
            </div>
          )}

          {/* Storage Condition Selector */}
          <Dropdown
            id="storage-condition-dropdown"
            titleText={intl.formatMessage({
              id: "pathology.storage.condition",
              defaultMessage: "Storage Condition",
            })}
            label={intl.formatMessage({
              id: "pathology.storage.selectCondition",
              defaultMessage: "Select condition...",
            })}
            items={storageConditionOptions}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedCondition}
            onChange={({ selectedItem }) => setSelectedCondition(selectedItem)}
          />

          {/* Retention Period */}
          <NumberInput
            id="retention-years"
            label={intl.formatMessage({
              id: "pathology.storage.retentionYears",
              defaultMessage: "Retention Period (Years)",
            })}
            value={retentionYears}
            min={1}
            max={30}
            step={1}
            onChange={(e, { value }) => setRetentionYears(value)}
            helperText={intl.formatMessage(
              {
                id: "pathology.storage.expiryDate",
                defaultMessage: "Expiry date will be: {date}",
              },
              {
                date: new Date(
                  Date.now() + retentionYears * 365 * 24 * 60 * 60 * 1000,
                ).toLocaleDateString(),
              },
            )}
          />

          {/* Expected Duration (pathology-specific) */}
          <TextInput
            id="expectedDuration"
            labelText={intl.formatMessage({
              id: "pathology.storage.expectedDuration",
              defaultMessage: "Expected Duration (Optional)",
            })}
            value={expectedDuration}
            onChange={(e) => setExpectedDuration(e.target.value)}
            placeholder={intl.formatMessage({
              id: "pathology.storage.expectedDurationPlaceholder",
              defaultMessage: "e.g., 6 months, 1 year",
            })}
          />
        </div>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.reassignConfirm.confirm",
          defaultMessage: "Reassign",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleConfirmReassignment}
        danger
        size="sm"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="pathology.storage.reassignConfirm.warning"
              defaultMessage="{count} of the selected samples already have storage assignments. Reassigning will remove them from their current locations."
              values={{ count: samplesToReassign.length }}
            />
          </p>

          <div
            style={{
              backgroundColor: "#fff1f1",
              border: "1px solid #da1e28",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <strong style={{ color: "#da1e28" }}>
              <FormattedMessage
                id="pathology.storage.reassignConfirm.currentLocations"
                defaultMessage="Current storage locations:"
              />
            </strong>
            <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
              {samplesToReassign.map((sample) => (
                <li key={sample.id} style={{ fontSize: "0.875rem" }}>
                  <strong>
                    {sample.accessionNumber || sample.externalId}
                  </strong>
                  : {sample.storageLocation}
                  {sample.storageCondition && ` (${sample.storageCondition})`}
                </li>
              ))}
            </ul>
          </div>

          <p style={{ fontStyle: "italic", color: "#525252" }}>
            <FormattedMessage
              id="pathology.storage.reassignConfirm.proceed"
              defaultMessage="Do you want to proceed with reassigning these samples to a new storage location?"
            />
          </p>
        </div>
      </Modal>

      {/* Temperature Log Modal */}
      <Modal
        open={tempLogModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.storage.tempLog.title",
          defaultMessage: "Log Temperature",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setTempLogModalOpen(false)}
        onRequestSubmit={handleSubmitTempLog}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="tempStorageUnit"
              name="storageUnit"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.storageUnit",
                defaultMessage: "Storage Unit",
              })}
              value={tempLogData.storageUnit}
              onChange={(e) => handleInputChange(e, setTempLogData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="temperatureCheckAM"
              label={intl.formatMessage({
                id: "pathology.storage.tempLog.tempAM",
                defaultMessage: "Temperature AM (°C)",
              })}
              value={tempLogData.temperatureCheckAM}
              onChange={(e, { value }) =>
                setTempLogData((prev) => ({
                  ...prev,
                  temperatureCheckAM: value,
                }))
              }
              min={-200}
              max={200}
              step={0.1}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="temperatureCheckPM"
              label={intl.formatMessage({
                id: "pathology.storage.tempLog.tempPM",
                defaultMessage: "Temperature PM (°C)",
              })}
              value={tempLogData.temperatureCheckPM}
              onChange={(e, { value }) =>
                setTempLogData((prev) => ({
                  ...prev,
                  temperatureCheckPM: value,
                }))
              }
              min={-200}
              max={200}
              step={0.1}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="tempCheckedBy"
              name="checkedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.tempLog.checkedBy",
                defaultMessage: "Checked By",
              })}
              value={tempLogData.checkedBy}
              onChange={(e) => handleInputChange(e, setTempLogData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "checkDate", setTempLogData)
              }
            >
              <DatePickerInput
                id="checkDate"
                labelText={intl.formatMessage({
                  id: "pathology.storage.tempLog.checkDate",
                  defaultMessage: "Check Date",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
        </Grid>
      </Modal>

      {/* Retrieval Modal */}
      <Modal
        open={retrievalModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.storage.retrieval.title",
            defaultMessage: "Retrieve Sample - {accession}",
          },
          {
            accession:
              selectedSampleForRetrieval?.accessionNumber ||
              selectedSampleForRetrieval?.externalId ||
              "",
          },
        )}
        primaryButtonText={intl.formatMessage({
          id: "pathology.storage.retrieval.submit",
          defaultMessage: "Record Retrieval",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setRetrievalModalOpen(false)}
        onRequestSubmit={handleSubmitRetrieval}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "dateRetrieved", setRetrievalData)
              }
            >
              <DatePickerInput
                id="dateRetrieved"
                labelText={intl.formatMessage({
                  id: "pathology.storage.retrieval.dateRetrieved",
                  defaultMessage: "Date Retrieved *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="retrievedBy"
              name="retrievedBy"
              labelText={intl.formatMessage({
                id: "pathology.storage.retrieval.retrievedBy",
                defaultMessage: "Retrieved By *",
              })}
              value={retrievalData.retrievedBy}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="recipientSignature"
              name="recipientSignature"
              labelText={intl.formatMessage({
                id: "pathology.storage.retrieval.recipientSignature",
                defaultMessage: "Recipient Signature *",
              })}
              value={retrievalData.recipientSignature}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyStorageInventoryPage;
