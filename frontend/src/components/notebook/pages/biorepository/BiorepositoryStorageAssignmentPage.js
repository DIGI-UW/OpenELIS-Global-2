import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Tag,
} from "@carbon/react";
import {
  Archive,
  Temperature,
  Checkmark,
  Location,
  Automatic,
  Renew,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * Storage temperature options for Biorepository samples
 * Based on biorepository requirements:
 * - Room Temperature (for certain sample types)
 * - Refrigerated (2-8°C)
 * - Frozen (-20°C)
 * - Ultra-Low Frozen (-80°C)
 * - Liquid Nitrogen (-196°C)
 *
 * min/max values are used for validation against sample requirements
 */
const STORAGE_CONDITIONS = [
  {
    id: "ROOM_TEMP",
    label: "Room Temperature (15-25°C)",
    tempRange: "15-25°C",
    min: 15,
    max: 25,
    description: "For stable samples at ambient temperature",
  },
  {
    id: "REFRIGERATED",
    label: "Refrigerator (2-8°C)",
    tempRange: "2-8°C",
    min: 2,
    max: 8,
    description: "Standard refrigerated storage",
  },
  {
    id: "FROZEN_MINUS20",
    label: "Freezer (-20°C)",
    tempRange: "-20°C",
    min: -25,
    max: -15,
    description: "Standard frozen storage",
  },
  {
    id: "FROZEN_MINUS80",
    label: "Ultra-Low Freezer (-80°C)",
    tempRange: "-80°C",
    min: -85,
    max: -75,
    description: "Long-term sample preservation",
  },
  {
    id: "LIQUID_NITROGEN",
    label: "Liquid Nitrogen (-196°C)",
    tempRange: "-196°C",
    min: -210,
    max: -180,
    description: "Cryopreservation",
  },
];

/**
 * BiorepositoryStorageAssignmentPage - Storage Assignment workflow page
 * Stage 2 of the Biorepository workflow
 *
 * Receives samples advanced from Stage 1 (Intake) via notebook page samples.
 *
 * Storage Hierarchy: Room → Device/Freezer → Shelf → Rack → Box → Well
 * Location recording is critical: Anyone needing samples must know exact placement
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration from notebook
 * @param {Object} props.progress - Progress tracking data
 * @param {Function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function BiorepositoryStorageAssignmentPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

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
  const [selectedWell, setSelectedWell] = useState(null);
  const [isAssigning, setIsAssigning] = useState(false);
  const [wellAssignments, setWellAssignments] = useState({});
  const [isReassignment, setIsReassignment] = useState(false);
  const [bulkAssignValues, setBulkAssignValues] = useState({
    storageCondition: "",
    assignedBy: "",
    assignedDateTime: new Date().toISOString().slice(0, 16),
    notes: "",
  });
  const [temperatureWarning, setTemperatureWarning] = useState(null);
  const [bioSampleData, setBioSampleData] = useState({}); // Map of sampleItemId -> BioSample data

  // Reassignment confirmation modal state
  const [confirmReassignModalOpen, setConfirmReassignModalOpen] =
    useState(false);
  const [samplesToReassign, setSamplesToReassign] = useState([]);

  /**
   * Validate storage condition against sample temperature requirements.
   * Returns warning message if selected condition is outside sample requirements.
   * Uses bioSampleData (fetched from BioSample API) for temperature requirements.
   */
  const validateStorageCondition = useCallback(
    (conditionId) => {
      if (!conditionId) {
        setTemperatureWarning(null);
        return;
      }

      const condition = STORAGE_CONDITIONS.find((c) => c.id === conditionId);
      if (!condition) {
        setTemperatureWarning(null);
        return;
      }

      // Get selected samples
      const selectedSamples = samples.filter((s) =>
        selectedSampleIds.includes(s.id),
      );

      // Check each sample's temperature requirements using bioSampleData
      const incompatibleSamples = selectedSamples.filter((sample) => {
        // Get temperature requirements from bioSampleData (keyed by sampleItemId)
        const bioData = bioSampleData[sample.sampleItemId];

        // Skip samples without bioSample data or temperature requirements
        if (!bioData) {
          return false;
        }
        if (
          bioData.requiredTempMin === null &&
          bioData.requiredTempMax === null
        ) {
          return false;
        }

        const sampleMin = bioData.requiredTempMin;
        const sampleMax = bioData.requiredTempMax;

        // Check if storage condition range overlaps with sample requirements
        // Sample requires temp between sampleMin and sampleMax
        // Storage provides temp between condition.min and condition.max
        // Warning if storage temp is outside sample's required range
        if (sampleMin !== null && condition.max < sampleMin) {
          return true; // Storage too cold
        }
        if (sampleMax !== null && condition.min > sampleMax) {
          return true; // Storage too warm
        }

        return false;
      });

      if (incompatibleSamples.length > 0) {
        const sampleList = incompatibleSamples
          .slice(0, 3)
          .map((s) => s.externalId || s.barcode || s.id)
          .join(", ");
        const moreCount =
          incompatibleSamples.length > 3
            ? ` and ${incompatibleSamples.length - 3} more`
            : "";

        setTemperatureWarning(
          intl.formatMessage(
            {
              id: "biorepository.storage.tempWarning",
              defaultMessage:
                "Warning: Selected temperature ({tempRange}) may not be suitable for {count} sample(s): {samples}{more}. These samples have specific temperature requirements.",
            },
            {
              tempRange: condition.tempRange,
              count: incompatibleSamples.length,
              samples: sampleList,
              more: moreCount,
            },
          ),
        );
      } else {
        setTemperatureWarning(null);
      }
    },
    [samples, selectedSampleIds, bioSampleData, intl],
  );

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  /**
   * Fetch BioSample data (including retention policy) for a list of sample item IDs.
   * Merges the data into the existing samples array.
   */
  const fetchBioSampleRetentionData = useCallback((sampleItemIds) => {
    if (!sampleItemIds || sampleItemIds.length === 0) {
      return;
    }

    const idsParam = sampleItemIds.join(",");
    getFromOpenElisServer(
      `/rest/biorepository/sample?sampleItemIds=${idsParam}`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          // Build a map of sampleItemId -> bioSample data
          const bioSampleMap = {};
          response.forEach((bioSample) => {
            if (bioSample.sampleItemId) {
              bioSampleMap[bioSample.sampleItemId] = {
                retentionPolicyId: bioSample.retentionPolicyId,
                retentionPolicyName: bioSample.retentionPolicyName,
                retentionExpiryDate: bioSample.retentionExpiryDate,
                biosafetyLevel: bioSample.biosafetyLevel,
              };
            }
          });

          // Merge bioSample data into samples
          setSamples((prevSamples) =>
            prevSamples.map((sample) => {
              const bioData = bioSampleMap[sample.sampleItemId];
              if (bioData) {
                return {
                  ...sample,
                  retentionPolicyName:
                    bioData.retentionPolicyName || sample.retentionPolicyName,
                  retentionExpiryDate:
                    bioData.retentionExpiryDate || sample.retentionExpiryDate,
                  biosafetyLevel:
                    bioData.biosafetyLevel || sample.biosafetyLevel,
                };
              }
              return sample;
            }),
          );
        }
      },
    );
  }, []);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    // Skip synthetic page IDs
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
              externalId: sample.externalId || "-",
              accessionNumber: sample.accessionNumber || "-",
              sampleType:
                sample.sampleType || sample.typeOfSample?.description || "-",
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              // Biorepository specific fields
              biosafetyLevel: sample.data?.biosafetyLevel,
              projectName: sample.data?.projectName,
              originLab: sample.data?.originLab,
              // Storage assignment fields from data
              storageRoom: sample.data?.storageRoom,
              storageFreezer: sample.data?.storageFreezer,
              storageRack: sample.data?.storageRack,
              storageBox: sample.data?.storageBox,
              storageWell: sample.data?.storageWell,
              storagePath: sample.data?.storagePath,
              storageCondition: sample.data?.storageCondition,
              assignedBy: sample.data?.assignedBy,
              assignedDateTime: sample.data?.assignedDateTime,
              // Retention policy fields (initial values, will be enriched by biorepository API)
              retentionPolicyName:
                sample.retentionPolicyName || sample.data?.retentionPolicyName,
              retentionExpiryDate:
                sample.retentionExpiryDate || sample.data?.retentionExpiryDate,
            }));
            setSamples(transformedSamples);

            // Fetch retention policy data from biorepository API
            const sampleItemIds = transformedSamples
              .map((s) => s.sampleItemId)
              .filter(Boolean);
            if (sampleItemIds.length > 0) {
              fetchBioSampleRetentionData(sampleItemIds);
            }
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id, fetchBioSampleRetentionData]);

  // Load box occupancy from storage API
  const loadBoxOccupancy = useCallback((boxId) => {
    if (!boxId) return;

    getFromOpenElisServer(
      `/rest/storage/boxes/${boxId}/occupancy`,
      (response) => {
        if (componentMounted.current && response) {
          const occupiedCoordinates = response.occupiedCoordinates || {};
          setBoxLayout(occupiedCoordinates);
        }
      },
    );
  }, []);

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback(
    (selection) => {
      setStorageSelection(selection);
      setWellAssignments({});
      if (selection.box?.id) {
        loadBoxOccupancy(selection.box.id);
      } else {
        setBoxLayout({});
      }
    },
    [loadBoxOccupancy],
  );

  // Handle box layout loaded (from StorageHierarchySelector - fallback)
  const handleBoxLayoutLoaded = useCallback(
    (wells) => {
      if (Object.keys(boxLayout).length === 0) {
        setBoxLayout(wells || {});
      }
    },
    [boxLayout],
  );

  // Handle well click from BoxLayoutViewer
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        setError(
          intl.formatMessage(
            {
              id: "biorepository.storage.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied by {sample}. Choose another position.",
            },
            { well: wellCoord, sample: wellInfo.externalId || "a sample" },
          ),
        );
        return;
      }

      if (storageModalOpen) {
        const unassignedSamples = selectedSampleIds.filter(
          (id) => !wellAssignments[id],
        );
        if (unassignedSamples.length > 0) {
          setWellAssignments((prev) => ({
            ...prev,
            [unassignedSamples[0]]: wellCoord,
          }));
        }
      } else {
        if (selectedSampleIds.length === 0) {
          setError(
            intl.formatMessage({
              id: "biorepository.storage.selectSamplesFirst",
              defaultMessage:
                "Please select samples to assign to storage first.",
            }),
          );
          return;
        }

        setSelectedWell(wellCoord);
        setStorageModalOpen(true);
      }
    },
    [selectedSampleIds, wellAssignments, storageModalOpen, intl],
  );

  // Handle open storage modal
  const handleOpenStorageModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.storage.noSamplesSelected",
          defaultMessage: "Please select samples to assign to storage.",
        }),
      );
      return;
    }

    const alreadyAssigned = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) && (s.storageWell || s.storagePath),
    );

    if (alreadyAssigned.length > 0) {
      setSamplesToReassign(alreadyAssigned);
      setConfirmReassignModalOpen(true);
      return;
    }

    openStorageAssignmentModal(false);
  };

  // Fetch BioSample data for selected samples to get temperature requirements
  const fetchBioSampleData = useCallback((sampleItemIds) => {
    if (!sampleItemIds || sampleItemIds.length === 0) {
      setBioSampleData({});
      return;
    }

    // Fetch BioSample records for the selected sampleItemIds
    // The API returns BioSample data including requiredTempMin/Max
    getFromOpenElisServer(
      `/rest/biorepository/sample?sampleItemIds=${sampleItemIds.join(",")}`,
      (response) => {
        if (response && Array.isArray(response)) {
          const dataMap = {};
          response.forEach((bioSample) => {
            if (bioSample.sampleItemId) {
              dataMap[bioSample.sampleItemId] = {
                requiredTempMin: bioSample.requiredTempMin,
                requiredTempMax: bioSample.requiredTempMax,
                barcode: bioSample.barcode,
              };
            }
          });
          setBioSampleData(dataMap);
        }
      },
    );
  }, []);

  const openStorageAssignmentModal = (reassigning) => {
    setIsReassignment(reassigning);
    setStorageModalOpen(true);
    setError(null);
    setWellAssignments({});
    setTemperatureWarning(null);
    setBulkAssignValues({
      storageCondition: "",
      assignedBy: "",
      assignedDateTime: new Date().toISOString().slice(0, 16),
      notes: "",
    });

    // Fetch BioSample data for temperature validation
    const sampleItemIds = samples
      .filter((s) => selectedSampleIds.includes(s.id))
      .map((s) => s.sampleItemId)
      .filter(Boolean);
    fetchBioSampleData(sampleItemIds);
  };

  const handleConfirmReassignment = () => {
    setConfirmReassignModalOpen(false);
    openStorageAssignmentModal(true);
  };

  // Auto-populate wells with selected samples
  const handleAutoPopulate = () => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "biorepository.storage.selectBoxFirst",
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
            id: "biorepository.storage.notEnoughWells",
            defaultMessage:
              "Not enough empty wells. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      );
    } else {
      setError(null);
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "biorepository.storage.autoPopulateSuccess",
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

  // Handle bulk storage assignment
  const handleAssignStorage = useCallback(() => {
    // Validate at least room selection
    if (!storageSelection.room) {
      setError(
        intl.formatMessage({
          id: "biorepository.storage.selectRoom",
          defaultMessage: "Please select at least a storage room.",
        }),
      );
      return;
    }

    // Validation depends on whether box is selected
    if (storageSelection.box) {
      // Box-level assignment: require well assignments
      if (Object.keys(wellAssignments).length === 0) {
        setError(
          intl.formatMessage({
            id: "biorepository.storage.noWellAssignments",
            defaultMessage:
              "Please assign samples to wells using Auto-Populate or click on wells.",
          }),
        );
        return;
      }
    } else {
      // Hierarchy-level assignment: require sample selection
      if (selectedSampleIds.length === 0) {
        setError(
          intl.formatMessage({
            id: "biorepository.storage.noSampleSelection",
            defaultMessage: "Please select samples to assign to storage.",
          }),
        );
        return;
      }
    }

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    setIsAssigning(true);
    setError(null);

    // Build storage path
    const storagePath = [
      storageSelection.room?.label,
      storageSelection.device?.label,
      storageSelection.shelf?.label,
      storageSelection.rack?.label,
      storageSelection.box?.label,
    ]
      .filter(Boolean)
      .join(" > ");

    let assignData;

    // Common data for both box and shelf assignments
    const commonData = {
      storageRoom: storageSelection.room?.label,
      storageFreezer: storageSelection.device?.label,
      storageShelf: storageSelection.shelf?.label,
      storageRack: storageSelection.rack?.label,
      storageBox: storageSelection.box?.label,
      storagePath: storagePath,
      storageCondition: bulkAssignValues.storageCondition,
      assignedBy: bulkAssignValues.assignedBy,
      assignedDateTime:
        bulkAssignValues.assignedDateTime || new Date().toISOString(),
      notes: bulkAssignValues.notes,
    };

    if (storageSelection.box) {
      // Box-level assignment with well coordinates
      assignData = {
        sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
        boxId: storageSelection.box?.id,
        wellAssignments: wellAssignments,
        reassign: isReassignment,
        data: commonData,
      };
    } else {
      // Hierarchy-level assignment without box/well coordinates
      let locationType = "room";
      let locationId = storageSelection.room?.id;

      if (storageSelection.rack && storageSelection.rack.id) {
        locationType = "rack";
        locationId = storageSelection.rack.id;
      } else if (storageSelection.shelf && storageSelection.shelf.id) {
        locationType = "shelf";
        locationId = storageSelection.shelf.id;
      } else if (storageSelection.device && storageSelection.device.id) {
        locationType = "device";
        locationId = storageSelection.device.id;
      }

      assignData = {
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        boxId: null,
        reassign: isReassignment,
        data: {
          ...commonData,
          locationId: locationId,
          locationType: locationType,
          notes:
            `${commonData.notes || ""} | Biorepository ${locationType}-level storage: ${storagePath}`.trim(),
        },
      };
    }

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage`,
      JSON.stringify(assignData),
      (response) => {
        setIsAssigning(false);
        const assignedCount =
          response?.assignedCount ||
          (storageSelection.box
            ? Object.keys(wellAssignments).length
            : selectedSampleIds.length);
        const hasErrors =
          response?.errors &&
          Array.isArray(response.errors) &&
          response.errors.length > 0;

        if (response && (response.success || assignedCount > 0)) {
          const messageId = isReassignment
            ? "biorepository.storage.reassignSuccess"
            : "biorepository.storage.assignSuccess";
          const defaultMessage = isReassignment
            ? "Successfully reassigned {count} samples to new storage location."
            : "Successfully assigned {count} samples to storage.";

          setSuccessMessage(
            intl.formatMessage(
              { id: messageId, defaultMessage: defaultMessage },
              { count: assignedCount },
            ),
          );

          if (hasErrors) {
            setError(
              `Some samples could not be assigned: ${response.errors.join("; ")}`,
            );
          }

          setIsReassignment(false);
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          if (storageSelection.box) {
            loadBoxOccupancy(storageSelection.box.id);
          }
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          let errorMessage =
            response?.error || "Failed to assign storage. Please try again.";
          if (hasErrors) {
            errorMessage = response.errors.join("; ");
          }
          setError(errorMessage);
        }
      },
    );
  }, [
    pageData?.id,
    storageSelection,
    wellAssignments,
    bulkAssignValues,
    isReassignment,
    selectedSampleIds,
    intl,
    loadPageSamples,
    loadBoxOccupancy,
    onProgressUpdate,
  ]);

  // Calculate stats
  const storedCount = samples.filter(
    (s) => s.storageWell || s.storagePath,
  ).length;
  const pendingCount = samples.filter(
    (s) => !s.storageWell && !s.storagePath,
  ).length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;

  // Get storage status tag
  const getStorageTag = (sample) => {
    const hasStorage = sample.storageWell || sample.storagePath;
    const storageLocation = sample.storageWell || sample.storagePath;

    if (sample.status === "COMPLETED" && hasStorage) {
      return (
        <Tag
          type="green"
          renderIcon={Checkmark}
          title={sample.storagePath || storageLocation}
        >
          {storageLocation} (
          <FormattedMessage
            id="notebook.status.sentToNext"
            defaultMessage="Sent"
          />
          )
        </Tag>
      );
    }
    if (hasStorage) {
      return (
        <Tag
          type="cyan"
          renderIcon={Archive}
          title={sample.storagePath || storageLocation}
        >
          {storageLocation} (
          <FormattedMessage
            id="notebook.status.inProgress"
            defaultMessage="In Progress"
          />
          )
        </Tag>
      );
    }
    return (
      <Tag type="gray">
        <FormattedMessage
          id="notebook.status.awaitingStorage"
          defaultMessage="Awaiting Storage"
        />
      </Tag>
    );
  };

  // Get storage condition tag
  const getConditionTag = (sample) => {
    if (!sample.storageCondition) return null;

    const condition = STORAGE_CONDITIONS.find(
      (c) => c.id === sample.storageCondition,
    );
    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {condition ? condition.tempRange : sample.storageCondition}
      </Tag>
    );
  };

  // Get biosafety level badge type
  const getBiosafetyBadgeType = (level) => {
    switch (level) {
      case "BSL-1":
        return "green";
      case "BSL-2":
        return "blue";
      case "BSL-3":
        return "purple";
      case "BSL-4":
        return "red";
      default:
        return "gray";
    }
  };

  // Check if page has real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  return (
    <div className="biorepository-storage-assignment-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="biorepository.storage.title"
            defaultMessage="Storage Assignment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="biorepository.storage.description"
            defaultMessage="Assign samples to storage locations. Select storage hierarchy: Room → Device (Freezer/Refrigerator/Cabinet) → Rack → Box → Position. Record exact placement for retrieval."
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
                  id="biorepository.storage.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.storage.awaitingStorage"
                  defaultMessage="Awaiting Storage"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.storage.inStorage"
                  defaultMessage="In Storage (In Progress)"
                />
              </span>
              <span className="progress-value">
                {storedCount - completedCount}
              </span>
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
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            // Disable if all selected samples are already assigned
            samples
              .filter((s) => selectedSampleIds.includes(s.id))
              .every((s) => s.storageWell || s.storagePath)
          }
        >
          <FormattedMessage
            id="biorepository.storage.assignToStorage"
            defaultMessage="Assign to Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="biorepository.storage.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container" style={{ marginTop: "1.5rem" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="biorepository.storage.sampleList"
            defaultMessage="Sample List"
          />
        </h5>
        <SampleGrid
          gridId="biorepository-storage"
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            {
              key: "externalId",
              header: intl.formatMessage({
                id: "biorepository.column.externalId",
                defaultMessage: "Barcode/ID",
              }),
            },
            {
              key: "sampleType",
              header: intl.formatMessage({
                id: "biorepository.column.sampleType",
                defaultMessage: "Sample Type",
              }),
            },
            {
              key: "biosafetyLevel",
              header: intl.formatMessage({
                id: "biorepository.column.biosafetyLevel",
                defaultMessage: "BSL",
              }),
              render: (value) =>
                value ? (
                  <Tag type={getBiosafetyBadgeType(value)} size="sm">
                    {value}
                  </Tag>
                ) : (
                  "-"
                ),
            },
            {
              key: "storage",
              header: intl.formatMessage({
                id: "biorepository.column.storage",
                defaultMessage: "Storage Location",
              }),
              render: (value, row) => (
                <div
                  style={{ display: "flex", gap: "4px", alignItems: "center" }}
                >
                  {getStorageTag(row)}
                  {getConditionTag(row)}
                </div>
              ),
            },
            {
              key: "assignedBy",
              header: intl.formatMessage({
                id: "biorepository.column.assignedBy",
                defaultMessage: "Assigned By",
              }),
            },
            {
              key: "retentionPolicyName",
              header: intl.formatMessage({
                id: "biorepository.column.retentionPolicy",
                defaultMessage: "Retention Policy",
              }),
              render: (value) => value || "-",
            },
            {
              key: "retentionExpiryDate",
              header: intl.formatMessage({
                id: "biorepository.column.retentionExpiry",
                defaultMessage: "Expiry Date",
              }),
              render: (value) =>
                value ? new Date(value).toLocaleDateString() : "-",
            },
            {
              key: "status",
              header: intl.formatMessage({
                id: "biorepository.column.status",
                defaultMessage: "Status",
              }),
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "biorepository.storage.noSamples",
              defaultMessage: "No Samples",
            })}
            subtitle={intl.formatMessage({
              id: "biorepository.storage.noSamples.message",
              defaultMessage:
                "No samples have been advanced to this stage. Go to Stage 1 (Intake) and use 'Advance to Storage' to send samples here.",
            })}
            lowContrast
            hideCloseButton
          />
        </div>
      )}

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "biorepository.storage.modal.title",
          defaultMessage: "Assign to Storage",
        })}
        primaryButtonText={
          isAssigning
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "biorepository.storage.modal.assign",
                defaultMessage: "Assign",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleAssignStorage}
        primaryButtonDisabled={
          !storageSelection.room ||
          (storageSelection.box && Object.keys(wellAssignments).length === 0) ||
          (!storageSelection.box && selectedSampleIds.length === 0) ||
          isAssigning
        }
        size="lg"
      >
        <p className="modal-description">
          <FormattedMessage
            id="biorepository.storage.modal.description"
            defaultMessage="Assign {count} selected samples to storage. Select a storage location, then use Auto-Populate or click wells to assign samples."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Storage Location Selection */}
          <Column lg={8} md={4} sm={4}>
            {/* Storage Location */}
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>
                <Location size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="biorepository.storage.storageLocation"
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

            {/* Storage Condition Selection */}
            <div style={{ marginTop: "1rem" }}>
              <Select
                id="storageCondition"
                labelText={intl.formatMessage({
                  id: "biorepository.storage.condition",
                  defaultMessage: "Storage Temperature",
                })}
                value={bulkAssignValues.storageCondition}
                onChange={(e) => {
                  const newValue = e.target.value;
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    storageCondition: newValue,
                  }));
                  validateStorageCondition(newValue);
                }}
              >
                <SelectItem value="" text="Select temperature condition..." />
                {STORAGE_CONDITIONS.map((cond) => (
                  <SelectItem
                    key={cond.id}
                    value={cond.id}
                    text={`${cond.label} - ${cond.description}`}
                  />
                ))}
              </Select>
              {temperatureWarning && (
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage({
                    id: "biorepository.storage.tempWarning.title",
                    defaultMessage: "Temperature Mismatch",
                  })}
                  subtitle={temperatureWarning}
                  lowContrast
                  hideCloseButton
                  style={{ marginTop: "0.5rem" }}
                />
              )}
            </div>

            {/* Assigned By */}
            <div style={{ marginTop: "1rem" }}>
              <TextInput
                id="assignedBy"
                labelText={intl.formatMessage({
                  id: "biorepository.storage.assignedBy",
                  defaultMessage: "Assigned By (Staff Initials)",
                })}
                value={bulkAssignValues.assignedBy}
                onChange={(e) =>
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    assignedBy: e.target.value,
                  }))
                }
                placeholder="Staff name/initials"
              />
            </div>

            {/* Notes */}
            <div style={{ marginTop: "1rem" }}>
              <TextArea
                id="storageNotes"
                labelText={intl.formatMessage({
                  id: "biorepository.storage.notes",
                  defaultMessage: "Notes",
                })}
                value={bulkAssignValues.notes}
                onChange={(e) =>
                  setBulkAssignValues((prev) => ({
                    ...prev,
                    notes: e.target.value,
                  }))
                }
                placeholder="Any additional notes about storage..."
                rows={2}
              />
            </div>
          </Column>

          {/* Box Layout Preview with Auto-Populate */}
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
                      id="biorepository.storage.boxLayout"
                      defaultMessage="Box Layout"
                    />
                  </h5>
                  <Button
                    kind="tertiary"
                    size="sm"
                    renderIcon={Automatic}
                    onClick={handleAutoPopulate}
                    disabled={selectedSampleIds.length === 0}
                  >
                    <FormattedMessage
                      id="biorepository.storage.autoPopulate"
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
                    id="biorepository.storage.assignmentSummary"
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
                    id="biorepository.storage.selectBoxPrompt"
                    defaultMessage="Select a storage location to preview box layout"
                  />
                </p>
              </div>
            )}
          </Column>
        </Grid>
      </Modal>

      {/* Reassignment Confirmation Modal */}
      <Modal
        open={confirmReassignModalOpen}
        onRequestClose={() => setConfirmReassignModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "biorepository.storage.reassignConfirm.title",
          defaultMessage: "Confirm Reassignment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "biorepository.storage.reassignConfirm.confirm",
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
              id="biorepository.storage.reassignConfirm.warning"
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
                id="biorepository.storage.reassignConfirm.currentLocations"
                defaultMessage="Current storage locations:"
              />
            </strong>
            <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
              {samplesToReassign.map((sample) => (
                <li key={sample.id} style={{ fontSize: "0.875rem" }}>
                  <strong>{sample.externalId}</strong>:{" "}
                  {sample.storageWell || sample.storagePath}
                  {sample.storageCondition && ` (${sample.storageCondition})`}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </Modal>
    </div>
  );
}

BiorepositoryStorageAssignmentPage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositoryStorageAssignmentPage;
