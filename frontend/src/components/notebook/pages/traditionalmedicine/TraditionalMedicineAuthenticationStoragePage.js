import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextInput,
  TextArea,
  Loading,
  NumberInput,
} from "@carbon/react";
import {
  CloudUpload,
  Checkmark,
  Renew,
  Chemistry,
  CheckmarkFilled,
  Pending,
  WarningAltFilled,
  Archive,
  Location,
  Temperature,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { useMemo as useMemoHook } from "react";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useTMMRDPermissions } from "../../../../hooks/useTMMRDPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineAuthenticationStoragePage - Page 2 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGES 2-3:
 * 1. Authentication (Stage 2) - Verify botanical identification and authentication results
 * 2. Storage & Herbarium Placement (Stage 3) - Manage physical storage and herbarium cataloging
 *
 * This page displays authenticated samples from Page 1 and provides:
 * - Authentication review and confirmation
 * - Storage location assignment (Fresh/Dried/Preserved)
 * - Herbarium specimen placement and cataloging
 * - Link to projects and ongoing research
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TraditionalMedicineAuthenticationStoragePage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const {
    getPagePermissionLevel,
    canSaveData,
    canApproveData,
    canAccessStage2,
  } = useTMMRDPermissions();

  // All state must be declared before any conditional returns (React Hooks Rule)
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  // Storage modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [isApplyingStorage, setIsApplyingStorage] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  // Storage form fields - following immunology pattern
  const [selectedCondition, setSelectedCondition] = useState(null);
  const [retentionYears, setRetentionYears] = useState(5);
  const [storageNotes, setStorageNotes] = useState("");
  const [herbariumSpecimenId, setHerbariumSpecimenId] = useState("");
  const [herbariumNotes, setHerbariumNotes] = useState("");
  const [linkedProject, setLinkedProject] = useState("");
  // Additional herbarium fields
  const [collector, setCollector] = useState("");
  const [collectionLocation, setCollectionLocation] = useState("");
  const [collectionDate, setCollectionDate] = useState("");
  const [herbariumCabinetLocation, setHerbariumCabinetLocation] = useState("");

  // Standardized storage hierarchy state (following other lab patterns)
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});

  // STAGE 2 allowed roles per TMMRD SRS Section 11 - Pharmacognosists lead authentication
  const allowedRoles = [
    "TMMRD Pharmacognosist",
    "TMMRD Lab Manager",
    "TMMRD Principal Investigator",
  ];

  const canAccessPage = canAccessStage2();

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(
    "Authentication & Storage",
  );
  const canEditData = canSaveData(pagePermissionLevel);
  const canApproveAuth = canApproveData(pagePermissionLevel);

  // TMMRD-specific storage conditions using backend-compatible enum values
  const storageConditionOptions = [
    {
      id: "REFRIGERATED", // Backend enum value
      label: intl.formatMessage({
        id: "notebook.tradmed.storage.condition.refrigerated",
        defaultMessage:
          "Refrigerated Storage (2-8°C) - Fresh Plants & Extracts",
      }),
      category: "refrigerated",
      tempRange: "2-8°C",
      description: "For fresh plant materials and stable extracts",
    },
    {
      id: "ROOM_TEMP", // Backend enum value (correct spelling)
      label: intl.formatMessage({
        id: "notebook.tradmed.storage.condition.roomTemp",
        defaultMessage:
          "Room Temperature - Dried Plants, Herbarium & Preserved Samples",
      }),
      category: "room_temp",
      tempRange: "18-25°C",
      description:
        "For dried materials, herbarium specimens, and preserved samples",
    },
    {
      id: "FROZEN_MINUS20", // Backend enum value
      label: intl.formatMessage({
        id: "notebook.tradmed.storage.condition.frozen20",
        defaultMessage: "Frozen Storage (-20°C) - Unstable Extracts",
      }),
      category: "extracts",
      tempRange: "-20°C",
      description: "For unstable plant extracts requiring freezing",
    },
    {
      id: "FROZEN_MINUS80", // Backend enum value
      label: intl.formatMessage({
        id: "notebook.tradmed.storage.condition.frozen80",
        defaultMessage: "Ultra-Low Storage (-80°C) - Purified Compounds",
      }),
      category: "compounds",
      tempRange: "-80°C",
      description: "For purified compounds, light-protected storage",
    },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // Load box occupancy data
  const loadBoxOccupancy = useCallback((boxId) => {
    if (!boxId) return;

    getFromOpenElisServer(
      `/rest/storage/boxes/${boxId}/occupancy`,
      (occupancyData) => {
        if (occupancyData && typeof occupancyData === "object") {
          setBoxLayout(occupancyData);
        }
      },
    );
  }, []);

  // Storage hierarchy selection handler - following immunology pattern
  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
    setWellAssignments({});
  }, []);

  // Handle box layout loaded - following immunology pattern
  const handleBoxLayoutLoaded = useCallback((wells) => {
    setBoxLayout(wells || {});
  }, []);

  // Handle well click - following immunology pattern
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage(
            {
              id: "notebook.tradmed.storage.positionOccupied",
              defaultMessage:
                "Position {position} is already occupied. Choose another position.",
            },
            { position: wellCoord },
          ),
        });
        return;
      }

      // Find the first selected sample without well assignment
      const unassignedSample = selectedSampleIds.find(
        (sampleId) => !Object.keys(wellAssignments).includes(sampleId),
      );

      if (unassignedSample) {
        setWellAssignments((prev) => ({
          ...prev,
          [unassignedSample]: wellCoord,
        }));
      }
    },
    [selectedSampleIds, wellAssignments, intl, notify],
  );

  // Auto-assign positions for organized storage (all sample types)
  const handleAutoPopulate = useCallback(() => {
    if (!storageSelection.box) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.storage.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      });
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
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage(
          {
            id: "notebook.tradmed.storage.notEnoughPositions",
            defaultMessage:
              "Not enough empty positions. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      });
    } else {
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage(
          {
            id: "notebook.tradmed.storage.autoAssignSuccess",
            defaultMessage:
              "Auto-assigned {count} samples to storage positions.",
          },
          { count: sampleIndex },
        ),
      });
    }
  }, [
    storageSelection.box,
    selectedSampleIds,
    boxLayout,
    intl,
    notify,
  ]);

  // Get combined layout - following immunology pattern exactly
  const getCombinedLayout = useCallback(() => {
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
  }, [boxLayout, wellAssignments, samples]);

  // Check if page has a real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples for this page
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
    console.log("DEBUG - Starting to load samples for page:", pageData.id);

    let samplesData = [];
    let routingData = [];
    let loadCount = 0;
    let hasError = false;

    const processData = () => {
      loadCount++;
      console.log("DEBUG - processData called, loadCount:", loadCount);

      // Always process data once we have at least samples data, even if routing fails
      const shouldProcess =
        loadCount >= 2 || (loadCount === 1 && !entryId) || hasError;

      if (!shouldProcess) return;

      console.log(
        "DEBUG - Processing data with samplesData.length:",
        samplesData.length,
        "routingData.length:",
        routingData.length,
      );

      if (componentMounted.current) {
        const routingMap = {};
        console.log(
          "DEBUG - Processing routing data, total records:",
          routingData.length,
        );
        routingData.forEach((routing) => {
          if (routing.destinationType === "STORAGE" && routing.sampleItemId) {
            console.log("DEBUG - Building routing map entry for sample:", {
              sampleItemId: routing.sampleItemId,
              roomName: routing.roomName,
              deviceName: routing.deviceName,
              shelfName: routing.shelfName,
              rackName: routing.rackName,
              boxName: routing.boxName,
            });
            routingMap[String(routing.sampleItemId)] = {
              boxId: routing.boxId,
              boxName: routing.boxName,
              wellCoordinate: routing.wellCoordinate,
              roomId: routing.roomId,
              roomName: routing.roomName,
              deviceId: routing.deviceId,
              deviceName: routing.deviceName,
              shelfId: routing.shelfId,
              shelfName: routing.shelfName,
              rackId: routing.rackId,
              rackName: routing.rackName,
              routedAt: routing.routedAt,
              hasRouting: true,
            };
          }
        });
        console.log("DEBUG - Final routingMap:", routingMap);

        const transformedSamples = samplesData.map((sample) => {
          const sampleId = String(sample.id || sample.sampleItemId);
          const routing = routingMap[sampleId];

          // Build storage hierarchy from stored data OR routing data
          let storageHierarchy = sample.data?.storageHierarchy || null;
          console.log("DEBUG - Processing sample for storage hierarchy:", {
            sampleId,
            hasExistingHierarchy: !!storageHierarchy,
            hasRouting: !!routing,
            sampleData: sample.data,
          });

          // First try to build from stored sample data (after storage assignment)
          if (!storageHierarchy && sample.data?.storagePath) {
            console.log(
              "DEBUG - Building storageHierarchy from stored data for sample:",
              sampleId,
            );
            storageHierarchy = {
              room: sample.data.storageRoom
                ? {
                    name: sample.data.storageRoom,
                    label: sample.data.storageRoom,
                  }
                : null,
              device: sample.data.storageDevice
                ? {
                    name: sample.data.storageDevice,
                    label: sample.data.storageDevice,
                  }
                : null,
              shelf: sample.data.storageShelf
                ? {
                    name: sample.data.storageShelf,
                    label: sample.data.storageShelf,
                  }
                : null,
              rack: sample.data.storageRack
                ? {
                    name: sample.data.storageRack,
                    label: sample.data.storageRack,
                  }
                : null,
              box: sample.data.storageBox
                ? {
                    name: sample.data.storageBox,
                    label: sample.data.storageBox,
                  }
                : null,
            };
            console.log("DEBUG - Built storageHierarchy from stored data:", {
              sampleId,
              storageHierarchy,
            });
          }
          // Fallback to routing data if no stored data
          else if (!storageHierarchy && routing?.hasRouting) {
            console.log(
              "DEBUG - Building storageHierarchy from routing for sample:",
              sampleId,
            );
            storageHierarchy = {
              room: routing.roomName
                ? {
                    id: routing.roomId,
                    name: routing.roomName,
                    label: routing.roomName,
                  }
                : null,
              device: routing.deviceName
                ? {
                    id: routing.deviceId,
                    name: routing.deviceName,
                    label: routing.deviceName,
                  }
                : null,
              shelf: routing.shelfName
                ? {
                    id: routing.shelfId,
                    name: routing.shelfName,
                    label: routing.shelfName,
                  }
                : null,
              rack: routing.rackName
                ? {
                    id: routing.rackId,
                    name: routing.rackName,
                    label: routing.rackName,
                  }
                : null,
              box: routing.boxName
                ? {
                    id: routing.boxId,
                    name: routing.boxName,
                    label: routing.boxName,
                  }
                : null,
            };
            console.log("DEBUG - Built storageHierarchy from routing:", {
              sampleId,
              storageHierarchy,
            });
          }

          const storageBox =
            sample.data?.storageBox || routing?.boxName || null;
          const storageWell =
            sample.data?.storageWell || routing?.wellCoordinate || null;

          const hasStorageAssignment = !!(
            sample.data?.storageCondition ||
            sample.data?.storagePath ||
            sample.data?.storageAssignmentId ||
            sample.storageCondition ||
            sample.condition ||
            sample.storage?.condition ||
            routing?.hasRouting ||
            storageHierarchy
          );

          return {
            id: sampleId,
            externalId: sample.externalId,
            accessionNumber: sample.accessionNumber,
            sampleType: sample.sampleType || sample.typeOfSample?.description,
            status: sample.pageStatus || sample.status || "PENDING",
            // Preserve entire data object for grid column renderers
            data: sample.data,
            // Traditional medicine specific fields
            sampleCategory: sample.data?.sampleCategory,
            localName: sample.data?.localName,
            scientificName: sample.data?.scientificName,
            species: sample.data?.species,
            // Authentication data from Page 1
            authenticationMethod: sample.data?.authenticationMethod,
            authenticationMethodLabel: sample.data?.authenticationMethodLabel,
            authenticationResult: sample.data?.authenticationResult,
            authenticationResultLabel: sample.data?.authenticationResultLabel,
            verifiedBy: sample.data?.verifiedBy,
            verificationDate: sample.data?.verificationDate,
            // Storage data (following immunology pattern)
            storageCondition:
              sample.data?.storageCondition ||
              sample.storageCondition ||
              sample.condition,
            storageLocation:
              sample.data?.storagePath ||
              sample.data?.storageLocation ||
              sample.storageLocation ||
              sample.storage?.location,
            storageHierarchy: storageHierarchy,
            storageBox: storageBox,
            storageWell: storageWell,
            wellAssignment:
              sample.data?.wellAssignment || sample.wellAssignment,
            retentionExpiry:
              sample.data?.retentionExpiry ||
              sample.retentionExpiry ||
              sample.storage?.expiryDate,
            hasStorageAssignment: hasStorageAssignment,
            // TMMRD-specific fields
            herbariumSpecimenId: sample.data?.herbariumSpecimenId,
            herbariumNotes: sample.data?.herbariumNotes,
            linkedProject: sample.data?.linkedProject,
            // Additional herbarium fields
            collector: sample.data?.collector,
            collectionLocation: sample.data?.collectionLocation,
            collectionDate: sample.data?.collectionDate,
            herbariumCabinetLocation: sample.data?.herbariumCabinetLocation,
            storedAt:
              sample.data?.storedAt ||
              sample.data?.assignedDateTime ||
              sample.storedAt,
            storedBy: sample.data?.storedBy || sample.storedBy,
            // Additional storage fields from stored data
            storageAssignmentId: sample.data?.storageAssignmentId,
            storagePath: sample.data?.storagePath,
          };
        });

        setSamples(transformedSamples);
        setLoading(false);
        console.log(
          "DEBUG - Finished processing, set loading to false. Samples loaded:",
          transformedSamples.length,
        );
        console.log(
          "DEBUG - Sample details:",
          transformedSamples.map((s) => ({
            id: s.id,
            externalId: s.externalId,
            authenticationResult: s.authenticationResult,
            status: s.status,
            authenticationMethod: s.authenticationMethod,
          })),
        );
      }
    };

    // Load samples data (required)
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        console.log(
          "DEBUG - Samples API response:",
          response ? `${response.length} samples` : "null/undefined",
        );
        if (response && Array.isArray(response)) {
          samplesData = response;
        } else {
          console.warn("DEBUG - Invalid samples response:", response);
        }
        processData();
      },
    );

    // Load routing data for storage assignments (optional - don't let this block the UI)
    if (entryId) {
      // Add timeout to prevent infinite loading if routing call hangs
      const timeoutId = setTimeout(() => {
        console.warn(
          "DEBUG - Routing API call timed out, proceeding without routing data",
        );
        hasError = true;
        processData();
      }, 10000); // 10 second timeout

      getFromOpenElisServer(
        `/rest/notebook/${entryId}/routing?destinationType=STORAGE`,
        (response) => {
          clearTimeout(timeoutId);
          console.log(
            "DEBUG - Routing API response:",
            response ? `${response.length} routes` : "null/undefined",
          );
          if (response && Array.isArray(response)) {
            routingData = response;
          } else {
            console.warn("DEBUG - Invalid routing response:", response);
          }
          processData();
        },
      );
    } else {
      console.log("DEBUG - No entryId, skipping routing data");
      processData();
    }
  }, [pageData?.id, entryId]);

  // Load samples on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  // Reset storage form - following immunology pattern
  const resetStorageForm = useCallback(() => {
    setSelectedCondition(null);
    setRetentionYears(5);
    setStorageNotes("");
    setHerbariumSpecimenId("");
    setHerbariumNotes("");
    setLinkedProject("");
    setCollector("");
    setCollectionLocation("");
    setCollectionDate("");
    setHerbariumCabinetLocation("");
    setStorageSelection({
      room: null,
      device: null,
      shelf: null,
      rack: null,
      box: null,
    });
    setBoxLayout({});
    setWellAssignments({});
  }, []);

  // Open storage modal
  const openStorageModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetStorageForm();
    setStorageModalOpen(true);
  }, [selectedSampleIds, intl, resetStorageForm, notify]);

  // Handle storage assignment - TMMRD-specific validation
  const handleAssignStorage = useCallback(() => {
    if (!selectedCondition) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.storage.selectCondition",
          defaultMessage: "Please select a storage condition.",
        }),
      });
      return;
    }

    // Check if user selected box-level storage (organized wells/slots)
    const useBoxLayout = !!storageSelection.box;

    if (useBoxLayout && Object.keys(wellAssignments).length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.storage.noWellAssignments",
          defaultMessage:
            "Please assign samples to positions using Auto-Assign or click on slots.",
        }),
      });
      return;
    }

    // For bulk storage (no box selected), need rack/shelf level selection
    if (
      !useBoxLayout &&
      !storageSelection.rack &&
      !storageSelection.shelf &&
      !storageSelection.device
    ) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.storage.selectRackLevel",
          defaultMessage:
            "Please select storage location down to shelf/rack level.",
        }),
      });
      return;
    }

    setIsApplyingStorage(true);

    const nbId = entryId; // Use entryId as notebook ID for TMMRD

    let payload;

    if (useBoxLayout) {
      // For organized storage using box/well assignments (all sample types)
      const wellAssignmentsForBackend = {};
      Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
        wellAssignmentsForBackend[sampleId] = wellCoord;
      });

      payload = {
        sampleIds: Object.keys(wellAssignments).map((id) => parseInt(id, 10)),
        boxId: storageSelection.box.id,
        wellAssignments: wellAssignmentsForBackend,
        condition: selectedCondition.id,
        retentionYears: retentionYears,
        reassign: false,
        data: {
          storageNotes: storageNotes,
          postAnalysisStorage: true, // Mark as traditional medicine storage
          herbariumSpecimenId: herbariumSpecimenId || null,
          herbariumNotes: herbariumNotes || null,
          linkedProject: linkedProject || null,
          // Additional herbarium entry fields
          collector: collector || null,
          collectionLocation: collectionLocation || null,
          collectionDate: collectionDate || null,
          herbariumCabinetLocation: herbariumCabinetLocation || null,
          storageHierarchy: {
            room: storageSelection.room,
            device: storageSelection.device,
            shelf: storageSelection.shelf,
            rack: storageSelection.rack,
            box: storageSelection.box,
          },
        },
      };
    } else {
      // For bulk storage using hierarchy-level assignment
      if (!storageSelection.room) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.tradmed.storage.selectRoom",
            defaultMessage:
              "Please select at least a storage room for traditional medicine samples.",
          }),
        });
        setIsApplyingStorage(false);
        return;
      }

      // Determine the most specific level selected (following MNTD pattern)
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

      // Build storage path (following MNTD pattern)
      const storagePath = [
        storageSelection.room?.label,
        storageSelection.device?.label,
        storageSelection.shelf?.label,
        storageSelection.rack?.label,
      ]
        .filter(Boolean)
        .join(" > ");

      // For hierarchy-level assignment without box/well coordinates (following MNTD pattern)
      payload = {
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        boxId: null, // Key: Use null for bulk storage (following MNTD pattern)
        condition: selectedCondition.id,
        retentionYears: retentionYears,
        reassign: false,
        data: {
          storageRoom: storageSelection.room?.label,
          storageDevice: storageSelection.device?.label,
          storageShelf: storageSelection.shelf?.label,
          storageRack: storageSelection.rack?.label,
          storagePath: storagePath,
          assignedDateTime: new Date().toISOString(),
          locationId: locationId,
          locationType: locationType,
          notes: `Traditional Medicine ${locationType}-level storage: ${storagePath}`,
          // TMMRD-specific fields
          storageNotes: storageNotes,
          postAnalysisStorage: true,
          herbariumSpecimenId: herbariumSpecimenId || null,
          herbariumNotes: herbariumNotes || null,
          linkedProject: linkedProject || null,
          // Additional herbarium entry fields
          collector: collector || null,
          collectionLocation: collectionLocation || null,
          collectionDate: collectionDate || null,
          herbariumCabinetLocation: herbariumCabinetLocation || null,
        },
      };
    }

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/storage`,
      JSON.stringify(payload),
      (response) => {
        setIsApplyingStorage(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.storage.assignSuccess",
                defaultMessage:
                  "Successfully assigned {count} samples to traditional medicine storage ({condition}).",
              },
              {
                count: response.assignedCount || selectedSampleIds.length,
                condition: selectedCondition.label,
              },
            ),
          });
          setStorageModalOpen(false);
          setSelectedSampleIds([]);
          setWellAssignments({});
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.tradmed.storage.assignError",
                defaultMessage: "Failed to assign samples to storage.",
              }),
          });
        }
      },
    );
  }, [
    storageSelection,
    selectedCondition,
    wellAssignments,
    selectedSampleIds,
    retentionYears,
    storageNotes,
    herbariumSpecimenId,
    herbariumNotes,
    linkedProject,
    collector,
    collectionLocation,
    collectionDate,
    herbariumCabinetLocation,
    pageData?.id,
    entryId,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle marking samples complete (moving to next page)
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected, have authentication result (any result), not already completed, and have storage condition assigned
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.status !== "COMPLETED" &&
        s.authenticationResult && // Must have any authentication result (confirmed, not_confirmed, pending, etc.)
        (s.storageCondition || s.hasStorageAssignment), // Must have storage condition OR storage assignment
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.storage.noEligibleSamples",
          defaultMessage:
            "Selected samples must have authentication result and storage condition assigned before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.storage.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.tradmed.storage.completeFailed",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);


  // Split samples by storage completion status (only 2 tables: IN_PROGRESS and COMPLETED)
  // IN_PROGRESS: Authenticated samples (from Page 2) ready for storage assignment
  // COMPLETED: Samples that have been assigned storage and marked complete to move to Page 4

  const authenticatedInProgressSamples = useMemoHook(() => {
    console.log(
      "DEBUG - Filtering samples for in-progress. Total samples:",
      samples.length,
    );
    const filtered = samples.filter((s) => {
      const hasAuthResult = !!s.authenticationResult; // Any authentication result (confirmed, not_confirmed, pending, etc.)
      const isNotCompleted = s.status !== "COMPLETED";
      console.log("DEBUG - Sample filter check:", {
        id: s.id,
        externalId: s.externalId,
        authenticationResult: s.authenticationResult,
        status: s.status,
        hasAuthResult,
        isNotCompleted,
        includeInFilter: hasAuthResult && isNotCompleted,
      });
      return hasAuthResult && isNotCompleted;
    });
    console.log("DEBUG - Filtered in-progress samples:", filtered.length);

    // TEMPORARY: If no authenticated samples found, show ALL samples for debugging
    if (filtered.length === 0 && samples.length > 0) {
      console.log(
        "DEBUG - No samples with auth results found, showing ALL samples for debugging",
      );
      return samples.filter((s) => s.status !== "COMPLETED");
    }

    return filtered;
  }, [samples]);

  const authenticatedCompletedSamples = useMemoHook(() => {
    const filtered = samples.filter(
      (s) => s.authenticationResult && s.status === "COMPLETED", // Any authentication result + completed status
    );

    // TEMPORARY: If no samples with auth results found, show completed samples regardless
    if (filtered.length === 0 && samples.length > 0) {
      console.log(
        "DEBUG - No samples with auth results completed found, showing ALL completed samples for debugging",
      );
      return samples.filter((s) => s.status === "COMPLETED");
    }

    return filtered;
  }, [samples]);

  const authenticatedInProgressCount = authenticatedInProgressSamples.length;
  const authenticatedCompletedCount = authenticatedCompletedSamples.length;

  // Helper to render authentication status
  const renderAuthenticationStatus = (sample) => {
    if (!sample.authenticationMethod) {
      return (
        <Tag type="gray" size="sm" renderIcon={Pending}>
          <FormattedMessage
            id="notebook.page.tradmed.auth.notStarted"
            defaultMessage="Not Started"
          />
        </Tag>
      );
    }
    if (sample.authenticationResult === "confirmed") {
      return (
        <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
          {sample.authenticationMethodLabel || "Verified"}
        </Tag>
      );
    }
    if (sample.authenticationResult === "not_confirmed") {
      return (
        <Tag type="red" size="sm" renderIcon={WarningAltFilled}>
          <FormattedMessage
            id="notebook.page.tradmed.auth.notConfirmed"
            defaultMessage="Not Confirmed"
          />
        </Tag>
      );
    }
    return (
      <Tag type="blue" size="sm" renderIcon={Chemistry}>
        {sample.authenticationResultLabel || "In Progress"}
      </Tag>
    );
  };

  // Helper to render storage status - simple status display matching API response
  const renderStorageStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.tradmed.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm" renderIcon={Archive}>
            <FormattedMessage
              id="notebook.tradmed.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.tradmed.status.skipped"
              defaultMessage="Skipped"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.tradmed.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  // Render storage location tag - following immunology pattern
  const renderStorageTag = (sample) => {
    // Check if sample has any storage assignment
    const hasStorageAssignment =
      sample.storageCondition ||
      sample.hasStorageAssignment ||
      sample.storageHierarchy ||
      sample.storageLocation ||
      sample.storagePath ||
      sample.storageAssignmentId;

    if (!hasStorageAssignment) {
      // Debug: Log when no storage assignment found
      if (sample.id) {
        console.log("DEBUG - No storage assignment for sample:", {
          id: sample.id,
          externalId: sample.externalId,
          storageCondition: sample.storageCondition,
          hasStorageAssignment: sample.hasStorageAssignment,
          storageHierarchy: sample.storageHierarchy,
          storageLocation: sample.storageLocation,
          storagePath: sample.storagePath,
          storageAssignmentId: sample.storageAssignmentId,
        });
      }
      return null;
    }

    // Build hierarchy path - prefer stored storagePath over building from hierarchy
    let locationPath = "";

    // First try to use the stored storage path (most accurate)
    if (sample.storagePath) {
      locationPath = sample.storagePath;
      console.log("DEBUG - Using stored storagePath:", {
        sampleId: sample.id,
        storagePath: sample.storagePath,
      });
    }
    // Then try building from storage hierarchy
    else if (sample.storageHierarchy) {
      const parts = [];
      // Follow MNTD order: room -> device -> shelf -> rack -> box
      if (
        sample.storageHierarchy.room?.label ||
        sample.storageHierarchy.room?.name
      )
        parts.push(
          sample.storageHierarchy.room.label ||
            sample.storageHierarchy.room.name,
        );
      if (
        sample.storageHierarchy.device?.label ||
        sample.storageHierarchy.device?.name
      )
        parts.push(
          sample.storageHierarchy.device.label ||
            sample.storageHierarchy.device.name,
        );
      if (
        sample.storageHierarchy.shelf?.label ||
        sample.storageHierarchy.shelf?.name
      )
        parts.push(
          sample.storageHierarchy.shelf.label ||
            sample.storageHierarchy.shelf.name,
        );
      if (
        sample.storageHierarchy.rack?.label ||
        sample.storageHierarchy.rack?.name
      )
        parts.push(
          sample.storageHierarchy.rack.label ||
            sample.storageHierarchy.rack.name,
        );
      if (
        sample.storageHierarchy.box?.label ||
        sample.storageHierarchy.box?.name
      )
        parts.push(
          sample.storageHierarchy.box.label || sample.storageHierarchy.box.name,
        );

      locationPath = parts.length > 0 ? parts.join(" > ") : "Storage Assigned";
      console.log("DEBUG - Storage hierarchy path built:", {
        sampleId: sample.id,
        hierarchy: sample.storageHierarchy,
        path: locationPath,
      });
    }
    // Then try storageLocation
    else if (sample.storageLocation) {
      locationPath = sample.storageLocation;
      console.log("DEBUG - Using storageLocation fallback:", {
        sampleId: sample.id,
        storageLocation: sample.storageLocation,
      });
    } else {
      // Fallback for successful storage assignment without full hierarchy details
      locationPath = "Storage Assigned";
      console.log("DEBUG - Using generic fallback for sample:", {
        sampleId: sample.id,
        hasStorageAssignment: sample.hasStorageAssignment,
        storageCondition: sample.storageCondition,
      });
    }

    return (
      <Tag type="blue" renderIcon={Location} size="sm" title={locationPath}>
        {locationPath}
      </Tag>
    );
  };

  // Render storage condition tag - following immunology pattern
  const renderConditionTag = (sample) => {
    if (!sample.storageCondition) return null;

    // TMMRD-specific condition labels
    const conditionLabels = {
      REFRIGERATED: "2-8°C",
      ROOM_TEMP: "Room Temp",
      FROZEN_MINUS20: "-20°C",
      FROZEN_MINUS80: "-80°C",
      LIQUID_NITROGEN: "LN₂",
    };

    return (
      <Tag type="cool-gray" renderIcon={Temperature} size="sm">
        {conditionLabels[sample.storageCondition] || sample.storageCondition}
      </Tag>
    );
  };


  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Authentication & Storage"
        reason="This page requires specific Traditional Medicine authentication roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="tradmed-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.storage.title"
            defaultMessage="Authentication Review & Sample Storage / Herbarium Placement"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.storage.description"
            defaultMessage="Review authentication results, assign storage conditions, and catalog specimens in herbarium with species, collector, location, and date information."
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
                  id="notebook.page.tradmed.storage.readyForStorage"
                  defaultMessage="Ready for Storage Assignment"
                />
              </span>
              <span className="progress-value">
                {authenticatedInProgressCount}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.storage.stored"
                  defaultMessage="Storage Complete"
                />
              </span>
              <span className="progress-value">
                {authenticatedCompletedCount}
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
          renderIcon={CloudUpload}
          onClick={openStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.storage.assignStorage"
            defaultMessage="Assign Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleMarkComplete}
            disabled={isCompleting || !pageData?.id}
          >
            <FormattedMessage
              id="notebook.tradmed.storage.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Authenticated Samples Section - IN PROGRESS (Ready for Storage Assignment) */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.storage.inProgress.title"
              defaultMessage="Processed Authentication - Ready for Storage Assignment"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {authenticatedInProgressCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.storage.inProgress.description"
              defaultMessage="Samples with authentication results from Page 2. Select samples and use 'Assign Storage' to designate storage locations and herbarium cataloging, then 'Mark Complete' to move to next page."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && authenticatedInProgressSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.storage.inProgress.empty"
                  defaultMessage="No samples with authentication results awaiting storage assignment. Complete authentication on Page 2 first."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="authenticated-in-progress-samples"
              samples={authenticatedInProgressSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStorageStatus(sample),
                },
                {
                  key: "storage",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.storage.column.storage",
                    defaultMessage: "Storage Location",
                  }),
                  render: (_value, sample) => (
                    <div
                      style={{
                        display: "flex",
                        gap: "4px",
                        alignItems: "center",
                      }}
                    >
                      {renderStorageTag(sample)}
                      {renderConditionTag(sample)}
                    </div>
                  ),
                },
                {
                  key: "herbariumSpecimenId",
                  header: "Herbarium ID",
                  render: (_value, sample) => {
                    if (!sample.herbariumSpecimenId) {
                      return <span>—</span>;
                    }
                    return <span>{sample.herbariumSpecimenId}</span>;
                  },
                },
                {
                  key: "collector",
                  header: "Collector",
                  render: (_value, sample) => {
                    const collector = sample.data?.collector || sample.collector;
                    if (!collector) {
                      return <span>—</span>;
                    }
                    return <span>{collector}</span>;
                  },
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Authenticated Samples Section - COMPLETED */}
      {authenticatedCompletedCount > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.storage.completed.title"
                defaultMessage="Storage Assignment Complete"
              />
              <Tag type="green" size="sm" className="count-tag">
                {authenticatedCompletedCount}
              </Tag>
            </h5>
            <p className="table-section-description">
              <FormattedMessage
                id="notebook.page.tradmed.storage.completed.description"
                defaultMessage="Samples with storage assigned and marked complete, ready to proceed to Page 4 (Preparation)."
              />
            </p>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="authenticated-completed-samples"
              samples={authenticatedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStorageStatus(sample),
                },
                {
                  key: "storage",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.storage.column.storage",
                    defaultMessage: "Storage Location",
                  }),
                  render: (_value, sample) => (
                    <div
                      style={{
                        display: "flex",
                        gap: "4px",
                        alignItems: "center",
                      }}
                    >
                      {renderStorageTag(sample)}
                      {renderConditionTag(sample)}
                    </div>
                  ),
                },
                {
                  key: "herbariumSpecimenId",
                  header: "Herbarium ID",
                  render: (_value, sample) => {
                    if (!sample.herbariumSpecimenId) {
                      return <span>—</span>;
                    }
                    return <span>{sample.herbariumSpecimenId}</span>;
                  },
                },
                {
                  key: "collector",
                  header: "Collector",
                  render: (_value, sample) => {
                    const collector = sample.data?.collector || sample.collector;
                    if (!collector) {
                      return <span>—</span>;
                    }
                    return <span>{collector}</span>;
                  },
                },
              ]}
            />
          </div>
        </div>
      )}

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        onRequestSubmit={handleAssignStorage}
        modalHeading={intl.formatMessage({
          id: "notebook.tradmed.storage.storageModal.title",
          defaultMessage: "Assign Traditional Medicine Storage",
        })}
        primaryButtonText={
          isApplyingStorage
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.tradmed.storage.storageModal.assign",
                defaultMessage: "Assign to Storage",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={
          !selectedCondition ||
          isApplyingStorage ||
          // For box-based storage: need box and well assignments
          (storageSelection.box && Object.keys(wellAssignments).length === 0) ||
          // For bulk storage: need rack/shelf level selection
          (!storageSelection.box &&
            !storageSelection.rack &&
            !storageSelection.shelf &&
            !storageSelection.device)
        }
        size="md"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.tradmed.storage.storageModal.description"
            defaultMessage="Store {count} traditional medicine samples under defined conditions. Select appropriate storage based on sample type and retention requirements."
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
                  id="notebook.tradmed.storage.storageLocation"
                  defaultMessage="Traditional Medicine Storage Location"
                />
              </h5>
              <p
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                  marginBottom: "0.5rem",
                }}
              >
                <FormattedMessage
                  id="notebook.tradmed.storage.locationHint"
                  defaultMessage="Select storage location. Choose a box for organized grid-based storage, or select shelf/rack level for bulk storage."
                />
              </p>
              <StorageHierarchySelector
                onSelectionChange={handleStorageSelectionChange}
                entryId={entryId}
                onBoxLayoutLoaded={handleBoxLayoutLoaded}
                boxRequired={false}
                showPath={true}
              />
            </div>
          </Column>

          {/* TMMRD Storage Assignment Preview */}
          <Column lg={8} md={4} sm={4}>
            {selectedCondition ? (
              <div>
                <div style={{ marginBottom: "0.5rem" }}>
                  <h5>
                    <Archive size={16} style={{ marginRight: "0.5rem" }} />
                    <FormattedMessage
                      id="notebook.tradmed.storage.assignmentPreview"
                      defaultMessage="Storage Assignment"
                    />
                  </h5>
                </div>

                <div
                  style={{
                    padding: "1rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    border: "1px solid #ddd",
                  }}
                >
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>
                      <FormattedMessage
                        id="notebook.tradmed.storage.storageType"
                        defaultMessage="Storage Type:"
                      />
                    </strong>{" "}
                    {selectedCondition.label}
                  </div>

                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>
                      <FormattedMessage
                        id="notebook.tradmed.storage.sampleCount"
                        defaultMessage="Samples to Store:"
                      />
                    </strong>{" "}
                    {selectedSampleIds.length}
                  </div>

                  {storageSelection.room && (
                    <div style={{ marginBottom: "0.5rem" }}>
                      <strong>
                        <FormattedMessage
                          id="notebook.tradmed.storage.location"
                          defaultMessage="Location:"
                        />
                      </strong>{" "}
                      {storageSelection.room.name}
                      {storageSelection.device &&
                        ` > ${storageSelection.device.name}`}
                      {storageSelection.shelf &&
                        ` > ${storageSelection.shelf.name}`}
                      {storageSelection.rack &&
                        ` > ${storageSelection.rack.name}`}
                      {storageSelection.box &&
                        ` > ${storageSelection.box.name}`}
                    </div>
                  )}

                  {!storageSelection.box &&
                    !storageSelection.rack &&
                    !storageSelection.shelf &&
                    storageSelection.room && (
                      <div
                        style={{
                          marginBottom: "0.5rem",
                          color: "#da1e28",
                          fontSize: "0.875rem",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.tradmed.storage.needsRackSelection"
                          defaultMessage="Please select down to shelf/rack level for traditional medicine storage, or select a box for organized positions."
                        />
                      </div>
                    )}

                  {/* Show box layout for organized storage (all sample types) */}
                  {storageSelection.box ? (
                    <div>
                      <div
                        style={{ marginTop: "1rem", marginBottom: "0.5rem" }}
                      >
                        <Button
                          kind="tertiary"
                          size="sm"
                          renderIcon={Renew}
                          onClick={handleAutoPopulate}
                          disabled={selectedSampleIds.length === 0}
                        >
                          <FormattedMessage
                            id="notebook.tradmed.storage.autoAssignPositions"
                            defaultMessage="Auto-Assign Positions"
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
                          id="notebook.tradmed.storage.positionAssignments"
                          defaultMessage="{assigned} of {total} samples assigned to positions"
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
                        marginTop: "1rem",
                        fontSize: "0.875rem",
                        color: "#525252",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.tradmed.storage.bulkAssignment"
                        defaultMessage="Samples will be assigned to the selected storage location."
                      />
                    </div>
                  )}
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
                <Temperature size={32} />
                <p style={{ marginTop: "0.5rem", color: "#525252" }}>
                  <FormattedMessage
                    id="notebook.tradmed.storage.selectConditionFirst"
                    defaultMessage="Select a storage condition to see assignment details"
                  />
                </p>
              </div>
            )}
          </Column>

          {/* Storage Settings */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.storage.storageSettings"
                defaultMessage="Storage Settings"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="storage-condition-dropdown"
              titleText={intl.formatMessage({
                id: "notebook.tradmed.storage.condition",
                defaultMessage: "Storage Condition *",
              })}
              label={intl.formatMessage({
                id: "notebook.tradmed.storage.selectCondition",
                defaultMessage: "Select condition...",
              })}
              items={storageConditionOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={selectedCondition}
              onChange={({ selectedItem }) =>
                setSelectedCondition(selectedItem)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="retention-years"
              label={intl.formatMessage({
                id: "notebook.tradmed.storage.retentionYears",
                defaultMessage: "Retention Period (Years)",
              })}
              value={retentionYears}
              min={1}
              max={30}
              step={1}
              onChange={(e, { value }) => setRetentionYears(value)}
              helperText={intl.formatMessage(
                {
                  id: "notebook.tradmed.storage.expiryDate",
                  defaultMessage: "Expiry date will be: {date}",
                },
                {
                  date: new Date(
                    Date.now() + retentionYears * 365 * 24 * 60 * 60 * 1000,
                  ).toLocaleDateString(),
                },
              )}
            />
          </Column>

          {/* Traditional Medicine Specific Fields */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.storage.herbariumSection"
                defaultMessage="Herbarium & Documentation"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="herbarium-specimen-id"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.herbariumId",
                defaultMessage: "Herbarium Specimen ID",
              })}
              value={herbariumSpecimenId}
              onChange={(e) => setHerbariumSpecimenId(e.target.value)}
              placeholder="e.g., HB-2025-001"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="collector"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.collector",
                defaultMessage: "Collector",
              })}
              value={collector}
              onChange={(e) => setCollector(e.target.value)}
              placeholder="Name of the person who collected the specimen"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="collection-location"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.collectionLocation",
                defaultMessage: "Collection Location",
              })}
              value={collectionLocation}
              onChange={(e) => setCollectionLocation(e.target.value)}
              placeholder="Geographic location where specimen was collected"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="collection-date"
              type="date"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.collectionDate",
                defaultMessage: "Collection Date",
              })}
              value={collectionDate}
              onChange={(e) => setCollectionDate(e.target.value)}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="herbarium-cabinet-location"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.herbariumCabinetLocation",
                defaultMessage: "Herbarium Cabinet Location",
              })}
              value={herbariumCabinetLocation}
              onChange={(e) => setHerbariumCabinetLocation(e.target.value)}
              placeholder="e.g., Cabinet A, Shelf 3, Section B"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="linked-project"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.linkedProject",
                defaultMessage: "Linked Project",
              })}
              value={linkedProject}
              onChange={(e) => setLinkedProject(e.target.value)}
              placeholder="e.g., Project Name or ID"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              id="herbarium-notes"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.herbariumNotes",
                defaultMessage: "Herbarium Notes",
              })}
              value={herbariumNotes}
              onChange={(e) => setHerbariumNotes(e.target.value)}
              rows={2}
              placeholder="Specimen mounting, labeling, condition notes, etc."
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              id="storage-notes"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.storage.notes",
                defaultMessage: "Storage Notes",
              })}
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              rows={2}
              placeholder="Storage condition details, environmental requirements, etc."
            />
          </Column>
        </Grid>
      </Modal>

    </div>
  );
}

export default TraditionalMedicineAuthenticationStoragePage;
