import React, {
  useState,
  useEffect,
  useRef,
  useContext,
  useCallback,
} from "react";
import {
  Tile,
  Grid,
  Column,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Tag,
  ProgressBar,
  Search,
  Dropdown,
  Button,
  Tooltip,
  TextInput,
  TextArea,
  InlineNotification,
  Pagination,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import {
  getFromOpenElisServer,
  postToOpenElisServerForPDF,
} from "../utils/Utils";
import config from "../../config.json";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import StorageLocationsMetricCard from "./StorageDashboard/StorageLocationsMetricCard";
import LocationFilterDropdown from "./StorageDashboard/LocationFilterDropdown";
import BoxCrudControls from "./StorageDashboard/BoxCrudControls";
import SampleActionsContainer from "./SampleStorage/SampleActionsContainer";
import LocationActionsOverflowMenu from "./LocationManagement/LocationActionsOverflowMenu";
import EditLocationModal from "./LocationManagement/EditLocationModal";
import DeleteLocationModal from "./LocationManagement/DeleteLocationModal";
import EditBoxModal from "./LocationManagement/EditBoxModal";
import DeleteBoxModal from "./LocationManagement/DeleteBoxModal";
import StorageLocationModal from "./StorageLocationModal";
import PrintLabelButton from "./LocationManagement/PrintLabelButton";
import PrintLabelConfirmationDialog from "./LocationManagement/PrintLabelConfirmationDialog";
import LocationManagementModal from "./SampleStorage/LocationManagementModal";
import DisposeSampleModal from "./SampleStorage/DisposeSampleModal";
import { useSampleStorage } from "./hooks/useSampleStorage";
import "./StorageDashboard.css";

const TAB_ROUTES = ["samples", "rooms", "devices", "shelves", "racks", "boxes"];

const StorageDashboard = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const componentMounted = useRef(true);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const {
    assignSampleItem,
    moveSampleItem,
    updateSampleItemMetadata,
    isSubmitting: isMovingSample,
  } = useSampleStorage();

  // Metric cards state
  const [metrics, setMetrics] = useState({
    totalSamples: 0,
    active: 0,
    disposed: 0,
    storageLocations: 0,
  });

  // Dynamic status filter options (loaded from backend for maintainability)
  const [sampleStatusOptions, setSampleStatusOptions] = useState([
    { id: "", label: "All" },
    { id: "active", label: "Active" },
    { id: "disposed", label: "Disposed" },
  ]);

  // Callback for child components to refresh metrics (specs/001-sample-storage/spec.md FR-057b, FR-057c)
  const refreshMetrics = useCallback(() => {
    const controller = new AbortController();

    getFromOpenElisServer(
      "/rest/storage/sample-items?countOnly=true",
      (response) => {
        if (response) {
          const metricsData = Array.isArray(response) ? response[0] : response;
          setMetrics({
            totalSamples: metricsData?.totalSampleItems ?? 0,
            active: metricsData?.active ?? 0,
            disposed: metricsData?.disposed ?? 0,
            storageLocations: metricsData?.storageLocations ?? 0,
          });
        }
      },
      controller.signal,
    );

    return () => controller.abort();
  }, []);

  // Tab state - derive from URL
  const getTabFromUrl = () => {
    const pathParts = location.pathname.split("/");
    const tabName = pathParts[pathParts.length - 1];
    const tabIndex = TAB_ROUTES.indexOf(tabName);
    return tabIndex >= 0 ? tabIndex : 0; // Default to samples (index 0)
  };

  const [selectedTab, setSelectedTab] = useState(getTabFromUrl());

  // Data state for each tab
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);
  const [samples, setSamples] = useState([]);
  const [selectedRackIdForGrid, setSelectedRackIdForGrid] = useState("");
  const [selectedRackForGrid, setSelectedRackForGrid] = useState(null);
  const [boxesForGrid, setBoxesForGrid] = useState([]); // Boxes for grid assignment workflow
  const [boxesLoading, setBoxesLoading] = useState(false);
  const [boxesError, setBoxesError] = useState(null);
  const [selectedBoxId, setSelectedBoxId] = useState("");
  const [selectedBox, setSelectedBox] = useState(null);
  const [selectedCoordinate, setSelectedCoordinate] = useState("");
  const [assignSampleId, setAssignSampleId] = useState("");
  const [assignNotes, setAssignNotes] = useState("");
  const [assignStatus, setAssignStatus] = useState(null);

  // Box CRUD modal state
  const [boxModalOpen, setBoxModalOpen] = useState(false);
  const [boxModalMode, setBoxModalMode] = useState("create"); // "create" | "edit"
  const [selectedBoxForCrud, setSelectedBoxForCrud] = useState(null);
  const [boxDeleteModalOpen, setBoxDeleteModalOpen] = useState(false);

  // OGC-150: Pagination state
  const [page, setPage] = useState(1); // Carbon Pagination uses 1-based indexing
  const [pageSize, setPageSize] = useState(25); // Default page size per OGC-150
  const [totalItems, setTotalItems] = useState(0);

  // Debug logging for pagination responses
  const logPaginationResponse = (url, response, parsedPage, parsedSize) => {
    // eslint-disable-next-line no-console
    console.info("[OGC-150] pagination fetch", {
      url,
      page: parsedPage,
      size: parsedSize,
      type: Array.isArray(response) ? "array" : typeof response,
      keys:
        response && typeof response === "object" ? Object.keys(response) : null,
      itemsLength:
        response &&
        typeof response === "object" &&
        Array.isArray(response.items)
          ? response.items.length
          : Array.isArray(response)
            ? response.length
            : null,
      totalItems: response?.totalItems ?? response?.totalElements ?? null,
      totalPages: response?.totalPages ?? null,
      pageSize: response?.pageSize ?? null,
      currentPage: response?.currentPage ?? null,
    });
  };

  // Filter state
  const [searchTerm, setSearchTerm] = useState("");
  const [locationFilter, setLocationFilter] = useState(null); // { id, type, name} for single location dropdown (Samples tab)
  const [filterRoom, setFilterRoom] = useState(""); // For other tabs (devices, shelves, racks)
  const [filterDevice, setFilterDevice] = useState(""); // For other tabs
  const [filterStatus, setFilterStatus] = useState("");

  const [loading, setLoading] = useState(true);

  // Location CRUD modal state
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [selectedLocationType, setSelectedLocationType] = useState(null);
  const [selectedParentLocation, setSelectedParentLocation] = useState(null);
  const [printLabelLocation, setPrintLabelLocation] = useState(null);

  // Print label dialog state (single instance)
  const [printLabelDialogOpen, setPrintLabelDialogOpen] = useState(false);
  const [printLabelLocationData, setPrintLabelLocationData] = useState(null);

  // Sample modal state (single modal instances)
  const [locationModalOpen, setLocationModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [disposeModalOpen, setDisposeModalOpen] = useState(false);
  const [selectedSampleForDispose, setSelectedSampleForDispose] =
    useState(null);

  // Expandable row state - Object mapping row IDs to expanded state (allows multiple rows to be expanded)
  const [expandedRowIds, setExpandedRowIds] = useState({});

  // Handle row expand/collapse for expandable rows
  const handleRowExpand = (rowId) => {
    const rowIdStr = String(rowId || "");
    setExpandedRowIds((prevExpanded) => ({
      ...prevExpanded,
      [rowIdStr]: !prevExpanded[rowIdStr],
    }));
  };

  // Reset expanded state when tab changes
  useEffect(() => {
    setExpandedRowIds({});
  }, [selectedTab]);

  // Handle Create location
  const handleCreateLocation = (parentLocation = null) => {
    // Determine location type from current tab
    const tabName = TAB_ROUTES[selectedTab] || "rooms";
    const locationTypeMap = {
      rooms: "room",
      devices: "device",
      shelves: "shelf",
      racks: "rack",
    };
    const locationType = locationTypeMap[tabName] || tabName.slice(0, -1);
    setSelectedLocationType(locationType);

    // Store parent location with type information
    if (parentLocation) {
      // Ensure parent has type field for modal
      const parentWithType = {
        ...parentLocation,
        type:
          locationType === "device"
            ? "room"
            : locationType === "shelf"
              ? "device"
              : "shelf",
      };
      setSelectedParentLocation(parentWithType);
    } else {
      setSelectedParentLocation(null);
    }
    setCreateModalOpen(true);
  };

  // Handle Create modal close
  const handleCreateModalClose = () => {
    setCreateModalOpen(false);
    setSelectedLocationType(null);
    setSelectedParentLocation(null);
  };

  // Handle Create modal save
  const handleCreateModalSave = (newLocation) => {
    // Refresh the appropriate table based on location type
    const tabName = TAB_ROUTES[selectedTab] || "rooms";
    switch (tabName) {
      case "rooms":
        loadRooms();
        break;
      case "devices":
        loadDevices();
        break;
      case "shelves":
        loadShelves();
        break;
      case "racks":
        loadRacks();
        break;
    }
    // Refresh metrics
    refreshMetrics();
    // Show success notification
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "storage.add.location.success",
        defaultMessage: "Location created successfully",
      }),
      kind: "success",
    });
    setNotificationVisible(true);
    handleCreateModalClose();
  };

  // Handle Edit location
  const handleEditLocation = (location) => {
    setSelectedLocation(location);
    // Determine location type from current tab (rooms -> room, devices -> device, etc.)
    const tabName = TAB_ROUTES[selectedTab] || "rooms";
    // Map tab names to location types (handle special cases like shelves -> shelf)
    const locationTypeMap = {
      rooms: "room",
      devices: "device",
      shelves: "shelf",
      racks: "rack",
      samples: "sample",
    };
    const locationType = locationTypeMap[tabName] || tabName.slice(0, -1);
    setSelectedLocationType(locationType);
    setEditModalOpen(true);
  };

  // Handle Delete location
  const handleDeleteLocation = (location) => {
    setSelectedLocation(location);
    // Determine location type from current tab (rooms -> room, devices -> device, etc.)
    const tabName = TAB_ROUTES[selectedTab] || "rooms";
    // Map tab names to location types (handle special cases like shelves -> shelf)
    const locationTypeMap = {
      rooms: "room",
      devices: "device",
      shelves: "shelf",
      racks: "rack",
      samples: "sample",
    };
    const locationType = locationTypeMap[tabName] || tabName.slice(0, -1);
    setSelectedLocationType(locationType);
    setDeleteModalOpen(true);
  };

  // Handle Edit modal close
  const handleEditModalClose = () => {
    setEditModalOpen(false);
    setSelectedLocation(null);
    setSelectedLocationType(null);
  };

  // Handle Edit modal save
  const handleEditModalSave = (updatedLocation) => {
    // Refresh the appropriate table based on location type
    const tabName = TAB_ROUTES[selectedTab] || "rooms";
    switch (tabName) {
      case "rooms":
        loadRooms();
        break;
      case "devices":
        loadDevices();
        break;
      case "shelves":
        loadShelves();
        break;
      case "racks":
        loadRacks();
        break;
    }
    // Show success notification
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "storage.edit.location.success",
        defaultMessage: "Location updated successfully",
      }),
      kind: "success",
    });
    setNotificationVisible(true);
    handleEditModalClose();
  };

  // Handle Delete modal close
  const handleDeleteModalClose = () => {
    setDeleteModalOpen(false);
    setSelectedLocation(null);
    setSelectedLocationType(null);
  };

  // Handle Delete modal confirm
  const handleDeleteModalConfirm = (deletedLocation) => {
    // Refresh the appropriate table based on location type
    const tabName = TAB_ROUTES[selectedTab] || "rooms";
    switch (tabName) {
      case "rooms":
        loadRooms();
        break;
      case "devices":
        loadDevices();
        break;
      case "shelves":
        loadShelves();
        break;
      case "racks":
        loadRacks();
        break;
    }
    // Refresh metrics
    refreshMetrics();
    // Show success notification
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "storage.delete.location.success",
        defaultMessage: "Location deleted successfully",
      }),
      kind: "success",
    });
    setNotificationVisible(true);
    handleDeleteModalClose();
  };

  // Handle Print Label (opens confirmation dialog)
  const handlePrintLabel = (location) => {
    setPrintLabelLocationData(location);
    setPrintLabelDialogOpen(true);
  };

  // Handle Print Label confirmation (actually prints)
  const handlePrintLabelConfirm = async () => {
    if (!printLabelLocationData) return;

    const { type, id, name, label, code } = printLabelLocationData;
    const locationName = name || label || "";
    const locationCode = code || label || "";

    try {
      const endpoint = `${config.serverBaseUrl}/rest/storage/${type}/${id}/print-label`;

      // Fetch PDF using POST request
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        credentials: "include",
      });

      // Check if response is PDF or error JSON
      const contentType = response.headers.get("content-type");
      if (contentType && contentType.includes("application/pdf")) {
        // PDF response - create blob and open in new tab
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        // Open in new tab instead of downloading
        window.open(url, "_blank");
        // Delay revoking URL to allow the new tab to load the PDF
        setTimeout(() => window.URL.revokeObjectURL(url), 60000);

        // Show success notification
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "label.print.success",
            defaultMessage: "Label printed successfully",
          }),
          kind: "success",
        });
        setNotificationVisible(true);
      } else {
        // Error response - parse JSON error
        const errorData = await response.json();
        const errorMessage =
          errorData.error ||
          intl.formatMessage({
            id: "label.print.error",
            defaultMessage: "Failed to print label",
          });
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: errorMessage,
          kind: "error",
        });
        setNotificationVisible(true);
      }
    } catch (error) {
      console.error("Error printing label:", error);
      const errorMessage =
        error?.message ||
        intl.formatMessage({
          id: "label.print.error",
          defaultMessage: "Failed to print label",
        });
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: errorMessage,
        kind: "error",
      });
      setNotificationVisible(true);
    } finally {
      // Close dialog
      setPrintLabelDialogOpen(false);
      setPrintLabelLocationData(null);
    }
  };

  // Handle Print Label cancel
  const handlePrintLabelCancel = () => {
    setPrintLabelDialogOpen(false);
    setPrintLabelLocationData(null);
  };

  // Handle Print Label success
  const handlePrintLabelSuccess = () => {
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "label.print.success",
        defaultMessage: "Label printed successfully",
      }),
      kind: "success",
    });
    setNotificationVisible(true);
    setPrintLabelLocation(null);
  };

  // Handle Print Label error
  const handlePrintLabelError = (error) => {
    // If error is null, user cancelled - don't show error notification
    if (error === null) {
      setPrintLabelLocation(null);
      return;
    }
    const errorMessage =
      error?.message ||
      intl.formatMessage({
        id: "label.print.error",
        defaultMessage: "Failed to print label",
      });
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: errorMessage,
      kind: "error",
    });
    setNotificationVisible(true);
    setPrintLabelLocation(null);
  };

  // Handle Manage Location (open location modal)
  const handleManageLocation = (sample) => {
    setSelectedSample(sample);
    setLocationModalOpen(true);
  };

  // Handle Location Modal close
  const handleLocationModalClose = () => {
    setLocationModalOpen(false);
    setSelectedSample(null);
  };

  // Handle Location Modal confirm (reuse existing onLocationConfirm logic)
  const handleLocationModalConfirm = async (locationData) => {
    // Reuse the existing onLocationConfirm logic from SampleActionsContainer
    // This is the same logic that was in the inline callback
    const {
      sample,
      newLocation,
      reason,
      conditionNotes,
      positionCoordinate: directPositionCoordinate,
    } = locationData;
    const positionCoordinate =
      directPositionCoordinate ||
      newLocation?.positionCoordinate ||
      newLocation?.position?.coordinate ||
      null;

    // Determine if this is assignment (no current location) or movement (location exists)
    const isAssignment = !sample.location || !sample.location.trim();

    // Check if only metadata is being updated (no location change)
    // This happens when newLocation is null/undefined but position or notes are provided
    // Note: check for !== undefined to handle empty strings
    const hasPositionUpdate =
      positionCoordinate !== undefined && positionCoordinate !== null;
    const hasNotesUpdate =
      conditionNotes !== undefined && conditionNotes !== null;
    const isMetadataOnlyUpdate =
      !isAssignment && !newLocation && (hasPositionUpdate || hasNotesUpdate);

    try {
      // Handle metadata-only update
      if (isMetadataOnlyUpdate) {
        const updates = {};
        if (positionCoordinate !== undefined && positionCoordinate !== null) {
          updates.positionCoordinate = positionCoordinate;
        }
        if (conditionNotes !== undefined && conditionNotes !== null) {
          updates.notes = conditionNotes;
        }

        const response = await updateSampleItemMetadata(
          sample.sampleItemId || sample.id,
          updates,
        );

        loadSamples();
        refreshMetrics();

        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "storage.update.success",
            defaultMessage: "Storage metadata updated successfully",
          }),
          kind: "success",
        });
        setNotificationVisible(true);

        // Close modal on success
        handleLocationModalClose();
        return;
      }
      let locationId = null;
      let locationType = null;
      let finalPositionCoordinate = positionCoordinate;

      // Determine locationId and locationType based on selected hierarchy level
      if (newLocation.rack && newLocation.rack.id) {
        locationId = newLocation.rack.id;
        locationType = "rack";
        finalPositionCoordinate =
          positionCoordinate ||
          newLocation.position?.coordinate ||
          newLocation.positionCoordinate ||
          null;
      } else if (newLocation.shelf && newLocation.shelf.id) {
        locationId = newLocation.shelf.id;
        locationType = "shelf";
        finalPositionCoordinate =
          positionCoordinate ||
          newLocation.position?.coordinate ||
          newLocation.positionCoordinate ||
          null;
      } else if (newLocation.device && newLocation.device.id) {
        locationId = newLocation.device.id;
        locationType = "device";
        finalPositionCoordinate =
          positionCoordinate ||
          newLocation.position?.coordinate ||
          newLocation.positionCoordinate ||
          null;
      } else if (newLocation.id && newLocation.type) {
        if (
          newLocation.type === "rack" ||
          newLocation.type === "shelf" ||
          newLocation.type === "device"
        ) {
          locationId = newLocation.id;
          locationType = newLocation.type;
          finalPositionCoordinate =
            positionCoordinate || newLocation.positionCoordinate || null;
        } else if (newLocation.type === "room") {
          throw new Error(
            "Please select at least a device (minimum 2 levels: room + device).",
          );
        }
      } else {
        const hasRoom =
          newLocation.room && (newLocation.room.id || newLocation.room.name);
        const hasDevice =
          newLocation.device &&
          (newLocation.device.id || newLocation.device.name);

        if (!hasRoom || !hasDevice) {
          throw new Error(
            "Room and Device are required (minimum 2 levels). Please select at least a device.",
          );
        }

        locationId = newLocation.device.id;
        locationType = "device";
        finalPositionCoordinate =
          positionCoordinate ||
          newLocation.position?.coordinate ||
          newLocation.positionCoordinate ||
          null;
      }

      if (!locationId || !locationType) {
        throw new Error(
          "Could not determine target location. Please ensure a complete location hierarchy is selected.",
        );
      }

      let locationPayload;
      if (isAssignment) {
        locationPayload = {
          sampleItemId: sample.sampleItemId || sample.id,
          locationId: locationId,
          locationType: locationType,
          positionCoordinate: finalPositionCoordinate || null,
          notes: conditionNotes || null,
        };
        const response = await assignSampleItem(locationPayload);

        loadSamples();
        refreshMetrics();

        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "storage.assign.success",
            defaultMessage: "Storage location assigned successfully",
          }),
          kind: "success",
        });
        setNotificationVisible(true);

        if (response.shelfCapacityWarning) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: response.shelfCapacityWarning,
            kind: "warning",
          });
          setNotificationVisible(true);
        }
      } else {
        locationPayload = {
          sampleItemId: sample.sampleItemId || sample.id,
          locationId: locationId,
          locationType: locationType,
          positionCoordinate: finalPositionCoordinate || null,
          reason: reason || null,
          notes: conditionNotes || null,
        };
        const response = await moveSampleItem(locationPayload);

        loadSamples();
        refreshMetrics();

        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "storage.move.success",
            defaultMessage: "Sample moved successfully",
          }),
          kind: "success",
        });
        setNotificationVisible(true);

        if (response.shelfCapacityWarning) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: response.shelfCapacityWarning,
            kind: "warning",
          });
          setNotificationVisible(true);
        }
      }

      // Close modal on success
      handleLocationModalClose();
    } catch (error) {
      console.error(
        `Failed to ${isAssignment ? "assign" : "move"} sample:`,
        error,
      );
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          intl.formatMessage({
            id: isAssignment ? "storage.assign.error" : "storage.move.error",
            defaultMessage: isAssignment
              ? "Failed to assign storage location"
              : "Failed to move sample",
          }) + (error.message ? `: ${error.message}` : ""),
        kind: "error",
      });
      setNotificationVisible(true);
      // Don't close modal on error so user can retry
    }
  };

  // Handle Dispose (open dispose modal)
  const handleDispose = (sample) => {
    setSelectedSampleForDispose(sample);
    setDisposeModalOpen(true);
  };

  // Handle Dispose Modal close
  const handleDisposeModalClose = () => {
    setDisposeModalOpen(false);
    setSelectedSampleForDispose(null);
  };

  // Handle Dispose Modal confirm (OGC-73: Call API to dispose sample)
  const handleDisposeModalConfirm = async (disposalData) => {
    const { sample, reason, method, notes } = disposalData;

    try {
      // Call disposal API
      const response = await fetch(
        `${config.serverBaseUrl}/rest/storage/sample-items/dispose`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify({
            // Prefer external ID for API lookup (backend resolves by external ID or accession)
            // Fall back to numeric ID if external ID not available
            sampleItemId:
              sample?.sampleItemExternalId ||
              sample?.id ||
              sample?.sampleItemId,
            reason,
            method,
            notes,
          }),
        },
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to dispose sample");
      }

      // Success - show notification and refresh data
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "storage.dispose.success",
          defaultMessage: "Sample disposed successfully",
        }),
        kind: "success",
      });
      setNotificationVisible(true);
      handleDisposeModalClose();

      // Refresh sample list and metrics to show updated status
      loadSamples();
      refreshMetrics();
    } catch (error) {
      console.error("Error disposing sample:", error);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          error.message ||
          intl.formatMessage({
            id: "storage.dispose.error",
            defaultMessage: "Failed to dispose sample",
          }),
        kind: "error",
      });
      setNotificationVisible(true);
    }
  };

  // Determine which filters should be visible based on active tab
  const getVisibleFilters = () => {
    const tabName = TAB_ROUTES[selectedTab] || "samples";
    const visibleFilters = {
      room: false,
      device: false,
      status: false,
    };

    switch (tabName) {
      case "samples":
        visibleFilters.room = true;
        visibleFilters.device = true;
        visibleFilters.status = true;
        break;
      case "rooms":
        visibleFilters.status = true;
        break;
      case "devices":
        visibleFilters.room = true;
        visibleFilters.status = true;
        break;
      case "shelves":
        visibleFilters.room = true;
        visibleFilters.device = true;
        visibleFilters.status = true;
        break;
      case "racks":
        visibleFilters.room = true;
        visibleFilters.device = true;
        visibleFilters.status = true;
        break;
      case "boxes":
        // Custom UI handles its own inputs; hide default filters
        visibleFilters.room = false;
        visibleFilters.device = false;
        visibleFilters.status = false;
        break;
      default:
        visibleFilters.status = true;
    }

    return visibleFilters;
  };

  const visibleFilters = getVisibleFilters();

  // Reset filters when tab changes
  useEffect(() => {
    setLocationFilter(null);
    setFilterRoom("");
    setFilterDevice("");
    setFilterStatus("");
    setSearchTerm("");
  }, [selectedTab]);

  // OGC-150: Reset pagination to page 1 when filters/search change (prevent empty results)
  useEffect(() => {
    if (selectedTab === 0) {
      // Samples tab - reset pagination when any filter changes
      setPage(1);
    }
  }, [searchTerm, locationFilter, filterStatus, selectedTab]);

  // Sync tab with URL changes and handle default route
  useEffect(() => {
    if (location.pathname === "/Storage") {
      // Redirect to default tab (samples)
      history.replace("/Storage/samples");
      return;
    }
    const tabIndex = getTabFromUrl();
    if (tabIndex !== selectedTab) {
      setSelectedTab(tabIndex);
    }
  }, [location.pathname, history]);

  // Load metrics
  useEffect(() => {
    const abortMetrics = refreshMetrics();
    loadRooms();
    loadDevices();
    loadShelves();
    loadRacks();
    loadSamples();

    return () => {
      componentMounted.current = false;
      if (abortMetrics) abortMetrics(); // Cancel metrics request on unmount
    };
  }, [refreshMetrics]);

  // Load sample item status options dynamically from backend
  useEffect(() => {
    getFromOpenElisServer(
      "/rest/displayList/sample-item-status-types",
      (response) => {
        if (response && Array.isArray(response)) {
          setSampleStatusOptions(
            response.map((item) => ({
              id: item.id,
              label: item.value,
            })),
          );
        }
      },
    );
  }, []);

  // Reload data when filters change (server-side filtering for all tabs)
  // Note: When searchTerm is present, filters are applied client-side on search results (AND logic)
  // OGC-150: Also reload when page or pageSize changes for samples tab
  useEffect(() => {
    const tabName = TAB_ROUTES[selectedTab] || "samples";

    // Skip filter reload if searchTerm is present (filters applied client-side on search results)
    // Exception: For samples tab, debounced search effect handles reload
    if (searchTerm && searchTerm.trim() && tabName === "samples") {
      // For samples tab with search, debounced search effect will handle reload with filters
      return;
    }

    switch (tabName) {
      case "samples":
        loadSamples();
        break;
      case "rooms":
        loadRooms();
        break;
      case "devices":
        loadDevices();
        break;
      case "shelves":
        loadShelves();
        break;
      case "racks":
        loadRacks();
        break;
    }
  }, [
    locationFilter,
    filterRoom,
    filterDevice,
    filterStatus,
    selectedTab,
    page,
    pageSize,
  ]); // OGC-150: Added page, pageSize

  // Debounced search for samples tab (300-500ms delay after typing stops) - FR-064a
  // For other tabs, search triggers immediate reload
  useEffect(() => {
    const tabName = TAB_ROUTES[selectedTab] || "samples";

    // Only apply debouncing for samples tab (FR-064a)
    if (tabName === "samples") {
      // Clear existing timeout
      const timeoutId = setTimeout(() => {
        // Reload samples when search term changes (after debounce delay)
        loadSamples();
      }, 400); // 400ms debounce delay (within 300-500ms range per FR-064a)

      return () => clearTimeout(timeoutId);
    } else {
      // For other tabs, search triggers immediate reload
      if (searchTerm && searchTerm.trim()) {
        // Search term present - trigger reload
        switch (tabName) {
          case "rooms":
            loadRooms();
            break;
          case "devices":
            loadDevices();
            break;
          case "shelves":
            loadShelves();
            break;
          case "racks":
            loadRacks();
            break;
        }
      } else {
        // Search cleared - reload without search (use filter endpoint)
        switch (tabName) {
          case "rooms":
            loadRooms();
            break;
          case "devices":
            loadDevices();
            break;
          case "shelves":
            loadShelves();
            break;
          case "racks":
            loadRacks();
            break;
        }
      }
    }
  }, [searchTerm, selectedTab]); // Trigger on searchTerm or tab change

  // Handle tab change - update URL
  const handleTabChange = (index) => {
    const tabIndex =
      typeof index === "object" && index.selectedIndex !== undefined
        ? index.selectedIndex
        : typeof index === "number"
          ? index
          : 0;

    setSelectedTab(tabIndex);
    const tabName = TAB_ROUTES[tabIndex] || "samples";
    history.push(`/Storage/${tabName}`);
  };

  const loadRooms = () => {
    // Use search endpoint if searchTerm is present, otherwise use filter endpoint
    if (searchTerm && searchTerm.trim()) {
      // Call search endpoint (FR-064: Rooms tab - search by name and code)
      const url = `/rest/storage/rooms/search?q=${encodeURIComponent(searchTerm.trim())}`;
      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          // Apply status filter client-side on search results (AND logic)
          let filtered = response || [];
          if (filterStatus && visibleFilters.status) {
            const activeFilter = filterStatus === "true";
            filtered = filtered.filter((room) => {
              const roomActive = room.active === true || room.active === "true";
              return roomActive === activeFilter;
            });
          }
          setRooms(filtered);
          setLoading(false);
        }
      });
    } else {
      // Build query parameters for filtering (FR-065: Rooms tab - filter by status)
      const params = new URLSearchParams();

      if (filterStatus && visibleFilters.status) {
        if (filterStatus === "true") {
          params.append("status", "active");
        } else if (filterStatus === "false") {
          params.append("status", "inactive");
        }
      }

      const queryString = params.toString();
      const url = `/rest/storage/rooms${queryString ? "?" + queryString : ""}`;

      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          setRooms(response || []);
          setLoading(false);
        }
      });
    }
  };

  const loadDevices = () => {
    // Use search endpoint if searchTerm is present, otherwise use filter endpoint
    if (searchTerm && searchTerm.trim()) {
      // Call search endpoint (FR-064: Devices tab - search by name, code, and type)
      const url = `/rest/storage/devices/search?q=${encodeURIComponent(searchTerm.trim())}`;
      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          // Apply filters client-side on search results (AND logic)
          let filtered = response || [];

          if (filterRoom && visibleFilters.room && rooms && rooms.length > 0) {
            const selectedRoom = rooms.find(
              (r) => r.id === filterRoom || r.id?.toString() === filterRoom,
            );
            if (selectedRoom) {
              filtered = filtered.filter((device) => {
                const deviceRoomId = device.roomId || device.parentRoomId;
                return (
                  deviceRoomId === selectedRoom.id ||
                  deviceRoomId?.toString() === selectedRoom.id?.toString()
                );
              });
            }
          }

          if (filterStatus && visibleFilters.status) {
            const activeFilter = filterStatus === "true";
            filtered = filtered.filter((device) => {
              const deviceActive =
                device.active === true || device.active === "true";
              return deviceActive === activeFilter;
            });
          }

          setDevices(filtered);
        }
      });
    } else {
      // Build query parameters for filtering (FR-065: Devices tab - filter by type, room, and status)
      const params = new URLSearchParams();

      if (filterRoom && visibleFilters.room && rooms && rooms.length > 0) {
        const selectedRoom = rooms.find(
          (r) => r.id === filterRoom || r.id?.toString() === filterRoom,
        );
        if (selectedRoom) {
          params.append("roomId", selectedRoom.id);
        }
      }

      // Note: type filter not implemented in UI yet, backend supports it
      // When type filter is added, include: params.append("type", filterType);

      if (filterStatus && visibleFilters.status) {
        if (filterStatus === "true") {
          params.append("status", "active");
        } else if (filterStatus === "false") {
          params.append("status", "inactive");
        }
      }

      const queryString = params.toString();
      const url = `/rest/storage/devices${queryString ? "?" + queryString : ""}`;

      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          setDevices(response || []);
        }
      });
    }
  };

  const loadShelves = () => {
    // Use search endpoint if searchTerm is present, otherwise use filter endpoint
    if (searchTerm && searchTerm.trim()) {
      // Call search endpoint (FR-064: Shelves tab - search by label)
      const url = `/rest/storage/shelves/search?q=${encodeURIComponent(searchTerm.trim())}`;
      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          // Apply filters client-side on search results (AND logic)
          let filtered = response || [];

          if (
            filterDevice &&
            visibleFilters.device &&
            devices &&
            devices.length > 0
          ) {
            const selectedDevice = devices.find(
              (d) => d.id === filterDevice || d.id?.toString() === filterDevice,
            );
            if (selectedDevice) {
              filtered = filtered.filter((shelf) => {
                const shelfDeviceId = shelf.deviceId || shelf.parentDeviceId;
                return (
                  shelfDeviceId === selectedDevice.id ||
                  shelfDeviceId?.toString() === selectedDevice.id?.toString()
                );
              });
            }
          }

          if (filterRoom && visibleFilters.room && rooms && rooms.length > 0) {
            const selectedRoom = rooms.find(
              (r) => r.id === filterRoom || r.id?.toString() === filterRoom,
            );
            if (selectedRoom) {
              filtered = filtered.filter((shelf) => {
                const shelfRoomId = shelf.roomId;
                return (
                  shelfRoomId === selectedRoom.id ||
                  shelfRoomId?.toString() === selectedRoom.id?.toString()
                );
              });
            }
          }

          if (filterStatus && visibleFilters.status) {
            const activeFilter = filterStatus === "true";
            filtered = filtered.filter((shelf) => {
              const shelfActive =
                shelf.active === true || shelf.active === "true";
              return shelfActive === activeFilter;
            });
          }

          setShelves(filtered);
        }
      });
    } else {
      // Build query parameters for filtering (FR-065: Shelves tab - filter by device, room, and status)
      const params = new URLSearchParams();

      if (
        filterDevice &&
        visibleFilters.device &&
        devices &&
        devices.length > 0
      ) {
        const selectedDevice = devices.find(
          (d) => d.id === filterDevice || d.id?.toString() === filterDevice,
        );
        if (selectedDevice) {
          params.append("deviceId", selectedDevice.id);
        }
      }

      if (filterRoom && visibleFilters.room && rooms && rooms.length > 0) {
        const selectedRoom = rooms.find(
          (r) => r.id === filterRoom || r.id?.toString() === filterRoom,
        );
        if (selectedRoom) {
          params.append("roomId", selectedRoom.id);
        }
      }

      if (filterStatus && visibleFilters.status) {
        if (filterStatus === "true") {
          params.append("status", "active");
        } else if (filterStatus === "false") {
          params.append("status", "inactive");
        }
      }

      const queryString = params.toString();
      const url = `/rest/storage/shelves${queryString ? "?" + queryString : ""}`;

      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          setShelves(response || []);
        }
      });
    }
  };

  const loadRacks = () => {
    // Use search endpoint if searchTerm is present, otherwise use filter endpoint
    if (searchTerm && searchTerm.trim()) {
      // Call search endpoint (FR-064: Racks tab - search by label)
      const url = `/rest/storage/racks/search?q=${encodeURIComponent(searchTerm.trim())}`;
      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          // Apply filters client-side on search results (AND logic)
          let filtered = response || [];

          if (filterRoom && visibleFilters.room && rooms && rooms.length > 0) {
            const selectedRoom = rooms.find(
              (r) => r.id === filterRoom || r.id?.toString() === filterRoom,
            );
            if (selectedRoom) {
              filtered = filtered.filter((rack) => {
                const rackRoomId = rack.roomId;
                return (
                  rackRoomId === selectedRoom.id ||
                  rackRoomId?.toString() === selectedRoom.id?.toString()
                );
              });
            }
          }

          if (
            filterDevice &&
            visibleFilters.device &&
            devices &&
            devices.length > 0
          ) {
            const selectedDevice = devices.find(
              (d) => d.id === filterDevice || d.id?.toString() === filterDevice,
            );
            if (selectedDevice) {
              filtered = filtered.filter((rack) => {
                const rackDeviceId = rack.deviceId;
                return (
                  rackDeviceId === selectedDevice.id ||
                  rackDeviceId?.toString() === selectedDevice.id?.toString()
                );
              });
            }
          }

          if (filterStatus && visibleFilters.status) {
            const activeFilter = filterStatus === "true";
            filtered = filtered.filter((rack) => {
              const rackActive = rack.active === true || rack.active === "true";
              return rackActive === activeFilter;
            });
          }

          setRacks(filtered);
        }
      });
    } else {
      // Build query parameters for filtering (FR-065: Racks tab - filter by room, shelf, device, and status)
      const params = new URLSearchParams();

      // Note: shelf filter not implemented in UI yet, backend supports it
      // When shelf filter is added, include: params.append("shelfId", filterShelf);

      if (
        filterDevice &&
        visibleFilters.device &&
        devices &&
        devices.length > 0
      ) {
        const selectedDevice = devices.find(
          (d) => d.id === filterDevice || d.id?.toString() === filterDevice,
        );
        if (selectedDevice) {
          params.append("deviceId", selectedDevice.id);
        }
      }

      if (filterRoom && visibleFilters.room && rooms && rooms.length > 0) {
        const selectedRoom = rooms.find(
          (r) => r.id === filterRoom || r.id?.toString() === filterRoom,
        );
        if (selectedRoom) {
          params.append("roomId", selectedRoom.id);
        }
      }

      if (filterStatus && visibleFilters.status) {
        if (filterStatus === "true") {
          params.append("status", "active");
        } else if (filterStatus === "false") {
          params.append("status", "inactive");
        }
      }

      const queryString = params.toString();
      const url = `/rest/storage/racks${queryString ? "?" + queryString : ""}`;

      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current && response) {
          setRacks(response || []);
        }
      });
    }
  };

  const loadSamples = () => {
    // Use search endpoint if searchTerm is present, otherwise use filter endpoint
    if (searchTerm && searchTerm.trim()) {
      // Call search endpoint (FR-064: Sample Items tab - search by SampleItem ID/External ID, parent Sample accession, location path)
      const url = `/rest/storage/sample-items/search?q=${encodeURIComponent(searchTerm.trim())}`;
      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Apply filters client-side on search results (AND logic)
            let filtered = response || [];

            // Apply location filter from LocationFilterDropdown (Sample Items tab uses single location filter)
            if (locationFilter && locationFilter.id) {
              const locationName =
                locationFilter.name || locationFilter.label || "";
              filtered = filtered.filter((sampleItem) => {
                const sampleItemLocation = sampleItem.location || "";
                return sampleItemLocation
                  .toLowerCase()
                  .includes(locationName.toLowerCase());
              });
            }

            // Apply status filter for Samples tab
            // filterStatus contains the status ID from the dropdown (e.g., "active", "disposed", or actual status ID)
            if (filterStatus && visibleFilters.status) {
              filtered = filtered.filter((sampleItem) => {
                const sampleItemStatus = sampleItem.status || "";
                // Direct ID comparison - works for both legacy ("active"/"disposed") and actual status IDs
                return sampleItemStatus === filterStatus;
              });
            }

            setSamples(filtered);
            // OGC-150: Update pagination totalItems for search results
            setTotalItems(filtered.length);
          } else {
            console.error(
              "Sample Items search API returned non-array response:",
              response,
            );
            setSamples([]);
          }
        }
      });
    } else {
      // Build query parameters for filtering (FR-065: Samples tab - filter by location and status)
      // Backend expects "location" parameter (hierarchical path substring) for downward inclusive filtering
      const params = new URLSearchParams();

      // Use single location filter (from LocationFilterDropdown) if available
      // Pass location name/path for substring matching (backend does case-insensitive contains)
      if (locationFilter && locationFilter.id) {
        const locationName = locationFilter.name || locationFilter.label || "";
        if (locationName) {
          params.append("location", locationName);
        }
      }

      // Pass status filter to backend - can be any status ID from the dropdown
      // Backend handles "active", "disposed", or actual status IDs
      if (filterStatus && visibleFilters.status) {
        params.append("status", filterStatus);
      }

      // OGC-150: Add pagination parameters
      params.append("page", String(page - 1)); // Convert 1-based to 0-based
      params.append("size", String(pageSize));

      const queryString = params.toString();
      const url = `/rest/storage/sample-items${queryString ? "?" + queryString : ""}`;

      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current) {
          logPaginationResponse(url, response, page - 1, pageSize);
          // OGC-150: Handle paginated response with metadata
          if (response && typeof response === "object") {
            if (Array.isArray(response)) {
              // Backward compatibility: if response is array (old format without pagination)
              setSamples(response);
              setTotalItems(response.length);
              if (response.length === 0) {
                console.warn(
                  "Sample Items API returned empty array - no sample item assignments found matching filters",
                );
              }
            } else if (response.items && Array.isArray(response.items)) {
              // New format with pagination metadata (OGC-150)
              setSamples(response.items);
              const total =
                response.totalItems ??
                response.totalElements ??
                response.items.length;
              setTotalItems(total);
              if (response.items.length === 0) {
                console.warn(
                  "Sample Items API returned empty items array - no sample item assignments found matching filters",
                );
              }
            } else {
              console.error(
                "Sample Items API returned unexpected response format:",
                response,
              );
              setSamples([]);
              setTotalItems(0);
            }
          } else {
            console.error(
              "Sample Items API returned non-object response:",
              response,
            );
            setSamples([]);
            setTotalItems(0);
          }
        }
      });
    }
  };

  const fetchBoxesForRack = useCallback(
    (rackId) => {
      if (!rackId) {
        setBoxesForGrid([]);
        setSelectedBox(null);
        return;
      }
      setBoxesLoading(true);
      setBoxesError(null);

      const occupiedUrl = `/rest/storage/boxes?rackId=${rackId}&occupied=true`;
      const allUrl = `/rest/storage/boxes?rackId=${rackId}`;

      // Fetch all boxes and occupied boxes to derive occupancy status
      Promise.all([
        new Promise((resolve) =>
          getFromOpenElisServer(allUrl, (response) => resolve(response || [])),
        ),
        new Promise((resolve) =>
          getFromOpenElisServer(occupiedUrl, (response) =>
            resolve(response || []),
          ),
        ),
      ])
        .then(([allBoxes, occupiedBoxes]) => {
          if (!componentMounted.current) {
            return;
          }
          const occupiedIds = new Set(
            (occupiedBoxes || []).map((box) => box.id?.toString()),
          );
          const merged =
            (allBoxes || []).map((box) => ({
              ...box,
              occupied: occupiedIds.has(box.id?.toString()),
            })) || [];
          setBoxesForGrid(merged);

          // Update selected box with fresh data if one is selected
          if (selectedBoxId) {
            const updatedBox = merged.find((box) => box.id === selectedBoxId);
            if (updatedBox) {
              setSelectedBox(updatedBox);
            }
          }
        })
        .catch((err) => {
          console.error("Error loading boxes for rack", rackId, err);
          if (componentMounted.current) {
            setBoxesError(
              intl.formatMessage({
                id: "storage.boxes.load.error",
                defaultMessage: "Unable to load boxes for this rack.",
              }),
            );
          }
        })
        .finally(() => {
          if (componentMounted.current) {
            setBoxesLoading(false);
          }
        });
    },
    [intl, selectedBoxId],
  );

  useEffect(() => {
    if (selectedRackIdForGrid) {
      fetchBoxesForRack(selectedRackIdForGrid);
    } else {
      setBoxesForGrid([]);
      setSelectedBoxId("");
      setSelectedBox(null);
    }
  }, [selectedRackIdForGrid, fetchBoxesForRack]);

  const handleRackSelect = (selectedRack) => {
    if (!selectedRack) {
      setSelectedRackIdForGrid("");
      setSelectedRackForGrid(null);
      setSelectedBoxId("");
      setSelectedBox(null);
      setAssignStatus(null);
      return;
    }
    setSelectedRackIdForGrid(selectedRack.id);
    setSelectedRackForGrid(selectedRack);
    setSelectedBoxId("");
    setSelectedBox(null);
    setAssignStatus(null);
  };

  const handleBoxSelect = (box) => {
    if (!box) {
      setSelectedBoxId("");
      setSelectedBox(null);
      setSelectedCoordinate("");
      setAssignStatus(null);
      return;
    }
    setSelectedBoxId(box.id);
    setSelectedBox(box);
    setSelectedCoordinate("");
    setAssignStatus(null);
  };

  const handleCreateBox = () => {
    if (!selectedRackIdForGrid) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "storage.box.validation.parentRack.required",
          defaultMessage: "Please select a rack first.",
        }),
        kind: "error",
      });
      setNotificationVisible(true);
      return;
    }
    setSelectedBoxForCrud(null);
    setBoxModalMode("create");
    setBoxModalOpen(true);
  };

  const handleEditBox = (box) => {
    setSelectedBoxForCrud(box);
    setBoxModalMode("edit");
    setBoxModalOpen(true);
  };

  const handleDeleteBox = (box) => {
    setSelectedBoxForCrud(box);
    setBoxDeleteModalOpen(true);
  };

  const handleBoxModalClose = () => {
    setBoxModalOpen(false);
    setSelectedBoxForCrud(null);
  };

  const handleBoxDeleteModalClose = () => {
    setBoxDeleteModalOpen(false);
    setSelectedBoxForCrud(null);
  };

  const handleBoxSaved = async (savedBox) => {
    if (selectedRackIdForGrid) {
      await fetchBoxesForRack(selectedRackIdForGrid);
    }
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        {
          id: "storage.box.save.success",
          defaultMessage: "{label} saved successfully",
        },
        { label: savedBox?.label || "" },
      ),
      kind: "success",
    });
    setNotificationVisible(true);
    handleBoxModalClose();
  };

  const handleBoxDeleted = async () => {
    if (selectedRackIdForGrid) {
      await fetchBoxesForRack(selectedRackIdForGrid);
    }
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "storage.box.delete.success",
        defaultMessage: "Box deleted successfully",
      }),
      kind: "success",
    });
    setNotificationVisible(true);
    handleBoxDeleteModalClose();
  };

  const handleCoordinateSelect = (coordinate, isOccupied) => {
    if (isOccupied) {
      // Show error notification for occupied position
      setAssignStatus({
        kind: "error",
        message: intl.formatMessage(
          {
            id: "storage.boxes.coordinate.occupied",
            defaultMessage:
              "Position {coordinate} is already occupied. Please select a different position.",
          },
          { coordinate },
        ),
      });
      return;
    }
    setSelectedCoordinate(coordinate);
    setAssignStatus(null);
  };

  const handleAssignToBox = async () => {
    if (!selectedBox || !assignSampleId || !selectedCoordinate) {
      return;
    }
    setAssignStatus(null);
    try {
      await assignSampleItem({
        sampleItemId: assignSampleId,
        locationId: selectedBox.id,
        locationType: "box",
        positionCoordinate: selectedCoordinate,
        notes: assignNotes || undefined,
      });
      setAssignStatus({
        kind: "success",
        message: intl.formatMessage(
          {
            id: "storage.boxes.assign.success",
            defaultMessage: "Sample assigned to {coordinate} in {boxLabel}.",
          },
          { coordinate: selectedCoordinate, boxLabel: selectedBox.label },
        ),
      });
      setAssignSampleId("");
      setAssignNotes("");
      setSelectedCoordinate("");

      // Refresh boxes data to update occupancy status
      await fetchBoxesForRack(selectedRackIdForGrid);
    } catch (error) {
      setAssignStatus({
        kind: "error",
        message:
          error?.message ||
          intl.formatMessage({
            id: "storage.boxes.assign.error",
            defaultMessage: "Unable to assign sample to box.",
          }),
      });
    }
  };

  // Calculate occupancy percentage
  const calculateOccupancy = (occupied, total) => {
    if (!total || total === 0) return 0;
    return Math.round((occupied / total) * 100);
  };

  // Get occupancy color based on percentage
  const getOccupancyColor = (percentage) => {
    if (percentage < 70) return "green";
    if (percentage < 90) return "yellow";
    return "red";
  };

  // Filter data based on search and filters
  const filterData = (data, type) => {
    let filtered = [...data];

    // Search is now handled by backend search endpoints (FR-064)
    // No client-side search filtering needed - data is already filtered by backend
    // This function is kept for legacy compatibility but returns data as-is for tabs with backend search

    // All tabs now use server-side filtering and search, so return data as-is
    if (
      type === "samples" ||
      type === "rooms" ||
      type === "devices" ||
      type === "shelves" ||
      type === "racks"
    ) {
      return filtered; // Data already filtered by backend (search + filters)
    }

    // Legacy client-side filtering for other types (if any)
    if (filterRoom && type !== "rooms") {
      // filterRoom can be a string ID or empty string
      const roomFilterValue = typeof filterRoom === "string" ? filterRoom : "";
      if (roomFilterValue) {
        // Find the room by ID to get its name/code for filtering
        const selectedRoom = rooms.find(
          (r) =>
            r.id === roomFilterValue || r.id?.toString() === roomFilterValue,
        );
        if (selectedRoom) {
          filtered = filtered.filter((item) => {
            const roomName = item.roomName || item.room?.name || "";
            const roomCode = item.roomCode || item.room?.code || "";
            const roomId = item.roomId || item.room?.id || "";
            return (
              roomId === roomFilterValue ||
              roomId?.toString() === roomFilterValue ||
              roomName
                .toLowerCase()
                .includes(selectedRoom.name.toLowerCase()) ||
              roomCode.toLowerCase().includes(selectedRoom.code.toLowerCase())
            );
          });
        }
      }
    }

    if (filterDevice && type !== "devices" && type !== "rooms") {
      // filterDevice can be a string ID or empty string
      const deviceFilterValue =
        typeof filterDevice === "string" ? filterDevice : "";
      if (deviceFilterValue) {
        // Find the device by ID to get its name/code for filtering
        const selectedDevice = devices.find(
          (d) =>
            d.id === deviceFilterValue ||
            d.id?.toString() === deviceFilterValue,
        );
        if (selectedDevice) {
          filtered = filtered.filter((item) => {
            const deviceName = item.deviceName || item.device?.name || "";
            const deviceCode = item.deviceCode || item.device?.code || "";
            const deviceId = item.deviceId || item.device?.id || "";
            return (
              deviceId === deviceFilterValue ||
              deviceId?.toString() === deviceFilterValue ||
              deviceName
                .toLowerCase()
                .includes(selectedDevice.name.toLowerCase()) ||
              deviceCode
                .toLowerCase()
                .includes(selectedDevice.code.toLowerCase())
            );
          });
        }
      }
    }

    // Status filtering (client-side for non-samples types)
    if (filterStatus && type !== "samples") {
      filtered = filtered.filter((item) => {
        const statusValue =
          typeof filterStatus === "string" ? filterStatus : "";
        if (!statusValue) return true;
        return (
          item.active?.toString() === statusValue ||
          item.status === statusValue ||
          (statusValue === "true" && item.active === true) ||
          (statusValue === "false" && item.active === false)
        );
      });
    }

    return filtered;
  };

  // Rooms table headers
  const roomsHeaders = [
    { key: "name", header: intl.formatMessage({ id: "storage.room.name" }) },
    { key: "code", header: intl.formatMessage({ id: "storage.room.code" }) },
    {
      key: "devices",
      header: intl.formatMessage({ id: "storage.room.devices" }),
    },
    {
      key: "samples",
      header: intl.formatMessage({ id: "storage.room.samples" }),
    },
    { key: "status", header: intl.formatMessage({ id: "storage.status" }) },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  // Devices table headers
  const devicesHeaders = [
    { key: "name", header: intl.formatMessage({ id: "storage.device.name" }) },
    { key: "code", header: intl.formatMessage({ id: "storage.device.code" }) },
    { key: "room", header: intl.formatMessage({ id: "storage.device.room" }) },
    { key: "type", header: intl.formatMessage({ id: "storage.device.type" }) },
    {
      key: "occupancy",
      header: intl.formatMessage({ id: "storage.occupancy" }),
    },
    { key: "status", header: intl.formatMessage({ id: "storage.status" }) },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  // Shelves table headers
  const shelvesHeaders = [
    { key: "label", header: intl.formatMessage({ id: "storage.shelf.label" }) },
    {
      key: "device",
      header: intl.formatMessage({ id: "storage.shelf.device" }),
    },
    { key: "room", header: intl.formatMessage({ id: "storage.shelf.room" }) },
    {
      key: "occupancy",
      header: intl.formatMessage({ id: "storage.occupancy" }),
    },
    { key: "status", header: intl.formatMessage({ id: "storage.status" }) },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  // Racks table headers
  const racksHeaders = [
    { key: "label", header: intl.formatMessage({ id: "storage.rack.label" }) },
    { key: "room", header: intl.formatMessage({ id: "storage.rack.room" }) }, // Per FR-065a
    { key: "shelf", header: intl.formatMessage({ id: "storage.rack.shelf" }) },
    {
      key: "device",
      header: intl.formatMessage({ id: "storage.rack.device" }),
    },
    {
      key: "dimensions",
      header: intl.formatMessage({ id: "storage.rack.dimensions" }),
    },
    {
      key: "occupancy",
      header: intl.formatMessage({ id: "storage.occupancy" }),
    },
    { key: "status", header: intl.formatMessage({ id: "storage.status" }) },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  // Sample Items table headers (per spec: SampleItem ID/External ID as primary, Sample accession as secondary)
  const samplesHeaders = [
    {
      key: "sampleItemId",
      header: intl.formatMessage(
        { id: "storage.sampleitem.id" },
        { defaultMessage: "SampleItem ID" },
      ),
    },
    {
      key: "sampleAccessionNumber",
      header: intl.formatMessage(
        { id: "sample.accession.number" },
        { defaultMessage: "Sample Accession" },
      ),
    },
    { key: "type", header: intl.formatMessage({ id: "sample.type" }) },
    { key: "status", header: intl.formatMessage({ id: "storage.status" }) },
    { key: "location", header: intl.formatMessage({ id: "storage.location" }) },
    {
      key: "assignedBy",
      header: intl.formatMessage({ id: "storage.assigned.by" }),
    },
    {
      key: "date",
      header: intl.formatMessage({ id: "storage.assigned.date" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  // Format rooms data for table
  const formatRoomsData = (roomsData) => {
    if (!roomsData || roomsData.length === 0) {
      return [];
    }
    return roomsData.map((room) => ({
      id: String(room.id || ""),
      name: room.name || "",
      code: room.code || "",
      devices: room.deviceCount || 0,
      samples: room.sampleCount || 0,
      status: room.active ? (
        <Tag type="green">
          <FormattedMessage id="label.active" />
        </Tag>
      ) : (
        <Tag type="red">
          <FormattedMessage id="label.inactive" />
        </Tag>
      ),
      actions: (
        <LocationActionsOverflowMenu
          location={room}
          onEdit={handleEditLocation}
          onDelete={handleDeleteLocation}
        />
      ),
      isExpanded: !!expandedRowIds[String(room.id || "")],
    }));
  };

  // Format devices data for table
  const formatDevicesData = (devicesData) => {
    if (!devicesData || devicesData.length === 0) {
      return [];
    }
    return devicesData.map((device) => {
      // Ensure device has type field for overflow menu
      const deviceWithType = { ...device, type: "device" };
      const occupied = device.occupiedCount || 0;
      const capacityType = device.capacityType; // "manual", "calculated", or null
      const total = device.capacityLimit || device.totalCapacity || 0;

      // Determine if capacity can be displayed
      const canDisplayCapacity = capacityType !== null && total > 0;
      const occupancyPct = canDisplayCapacity
        ? calculateOccupancy(occupied, total)
        : 0;
      const occupancyColor = getOccupancyColor(occupancyPct);

      // Format occupancy display
      let occupancyDisplay;
      if (!canDisplayCapacity) {
        // Capacity cannot be determined - show "N/A" with tooltip
        occupancyDisplay = (
          <div>
            <Tooltip
              label={intl.formatMessage({
                id: "storage.capacity.undetermined.tooltip",
                defaultMessage:
                  "Capacity cannot be calculated: some child locations lack defined capacities",
              })}
            >
              <span>N/A</span>
            </Tooltip>
          </div>
        );
      } else {
        // Capacity is defined - show fraction, percentage, and badge
        const capacityBadge =
          capacityType === "manual" ? (
            <Tag type="blue" size="sm" style={{ marginLeft: "8px" }}>
              <FormattedMessage
                id="storage.capacity.manual"
                defaultMessage="Manual Limit"
              />
            </Tag>
          ) : capacityType === "calculated" ? (
            <Tag type="cyan" size="sm" style={{ marginLeft: "8px" }}>
              <FormattedMessage
                id="storage.capacity.calculated"
                defaultMessage="Calculated"
              />
            </Tag>
          ) : null;

        occupancyDisplay = (
          <div>
            <div style={{ display: "flex", alignItems: "center" }}>
              <span>
                {occupied.toLocaleString()}/{total.toLocaleString()} (
                {occupancyPct}%)
              </span>
              {capacityBadge}
            </div>
            <ProgressBar
              value={occupancyPct}
              label=""
              size="small"
              status={
                occupancyPct >= 90
                  ? "error"
                  : occupancyPct >= 70
                    ? "active"
                    : "finished"
              }
            />
          </div>
        );
      }

      return {
        id: String(device.id || ""),
        name: device.name || "",
        code: device.code || "",
        room: device.roomName || device.parentRoomName || "",
        type: (
          <Tag
            type={
              device.deviceType === "freezer"
                ? "blue"
                : device.deviceType === "refrigerator"
                  ? "cyan"
                  : "gray"
            }
          >
            {device.deviceType || ""}
          </Tag>
        ),
        occupancy: occupancyDisplay,
        status: device.active ? (
          <Tag type="green">
            <FormattedMessage id="label.active" />
          </Tag>
        ) : (
          <Tag type="red">
            <FormattedMessage id="label.inactive" />
          </Tag>
        ),
        actions: (
          <LocationActionsOverflowMenu
            location={deviceWithType}
            onEdit={handleEditLocation}
            onDelete={handleDeleteLocation}
            onPrintLabel={handlePrintLabel}
          />
        ),
        isExpanded: !!expandedRowIds[String(device.id || "")],
      };
    });
  };

  // Format shelves data for table
  const formatShelvesData = (shelvesData) => {
    if (!shelvesData || shelvesData.length === 0) {
      return [];
    }
    return shelvesData.map((shelf) => {
      // Ensure shelf has type field for overflow menu
      const shelfWithType = { ...shelf, type: "shelf" };
      const occupied = shelf.occupiedCount || 0;
      const capacityType = shelf.capacityType; // "manual", "calculated", or null
      const total = shelf.capacityLimit || shelf.totalCapacity || 0;

      // Determine if capacity can be displayed
      const canDisplayCapacity = capacityType !== null && total > 0;
      const occupancyPct = canDisplayCapacity
        ? calculateOccupancy(occupied, total)
        : 0;
      const occupancyColor = getOccupancyColor(occupancyPct);

      // Format occupancy display (same logic as devices)
      let occupancyDisplay;
      if (!canDisplayCapacity) {
        // Capacity cannot be determined - show "N/A" with tooltip
        occupancyDisplay = (
          <div>
            <Tooltip
              label={intl.formatMessage({
                id: "storage.capacity.undetermined.tooltip",
                defaultMessage:
                  "Capacity cannot be calculated: some child locations lack defined capacities",
              })}
            >
              <span>N/A</span>
            </Tooltip>
          </div>
        );
      } else {
        // Capacity is defined - show fraction, percentage, and badge
        const capacityBadge =
          capacityType === "manual" ? (
            <Tag type="blue" size="sm" style={{ marginLeft: "8px" }}>
              <FormattedMessage
                id="storage.capacity.manual"
                defaultMessage="Manual Limit"
              />
            </Tag>
          ) : capacityType === "calculated" ? (
            <Tag type="cyan" size="sm" style={{ marginLeft: "8px" }}>
              <FormattedMessage
                id="storage.capacity.calculated"
                defaultMessage="Calculated"
              />
            </Tag>
          ) : null;

        occupancyDisplay = (
          <div>
            <div style={{ display: "flex", alignItems: "center" }}>
              <span>
                {occupied.toLocaleString()}/{total.toLocaleString()} (
                {occupancyPct}%)
              </span>
              {capacityBadge}
            </div>
            <ProgressBar
              value={occupancyPct}
              label=""
              size="small"
              status={
                occupancyPct >= 90
                  ? "error"
                  : occupancyPct >= 70
                    ? "active"
                    : "finished"
              }
            />
          </div>
        );
      }

      return {
        id: String(shelf.id || ""),
        label: shelf.label || "",
        device: shelf.deviceName || shelf.parentDeviceName || "",
        room: shelf.roomName || "",
        occupancy: occupancyDisplay,
        status: shelf.active ? (
          <Tag type="green">
            <FormattedMessage id="label.active" />
          </Tag>
        ) : (
          <Tag type="red">
            <FormattedMessage id="label.inactive" />
          </Tag>
        ),
        actions: (
          <LocationActionsOverflowMenu
            location={shelfWithType}
            onEdit={handleEditLocation}
            onDelete={handleDeleteLocation}
            onPrintLabel={handlePrintLabel}
          />
        ),
        isExpanded: !!expandedRowIds[String(shelf.id || "")],
      };
    });
  };

  // Format racks data for table
  // Note: Racks always use calculated capacity (rows × columns per FR-017)
  // Racks do not have a static capacity_limit field - capacity is always calculated
  const formatRacksData = (racksData) => {
    if (!racksData || racksData.length === 0) {
      return [];
    }
    return racksData.map((rack) => {
      // Ensure rack has type field for overflow menu
      const rackWithType = { ...rack, type: "rack" };
      const occupied = rack.occupiedCount || 0;
      // Rack capacity is ALWAYS calculated as rows × columns (per FR-017)
      const total = (rack.rows || 0) * (rack.columns || 0);
      const occupancyPct = calculateOccupancy(occupied, total);
      const occupancyColor = getOccupancyColor(occupancyPct);

      return {
        id: String(rack.id || ""),
        label: rack.label || "",
        room: rack.roomName || "", // Per FR-065a
        shelf: rack.shelfLabel || rack.parentShelfLabel || "",
        device: rack.deviceName || "",
        dimensions:
          rack.rows && rack.columns ? `${rack.rows} × ${rack.columns}` : "-",
        occupancy: (
          <div>
            <div>
              {occupied}/{total} ({occupancyPct}%)
            </div>
            <ProgressBar
              value={occupancyPct}
              label=""
              size="small"
              status={
                occupancyPct >= 90
                  ? "error"
                  : occupancyPct >= 70
                    ? "active"
                    : "finished"
              }
            />
          </div>
        ),
        status: rack.active ? (
          <Tag type="green">
            <FormattedMessage id="label.active" />
          </Tag>
        ) : (
          <Tag type="red">
            <FormattedMessage id="label.inactive" />
          </Tag>
        ),
        actions: (
          <LocationActionsOverflowMenu
            location={rackWithType}
            onEdit={handleEditLocation}
            onDelete={handleDeleteLocation}
            onPrintLabel={handlePrintLabel}
          />
        ),
        isExpanded: !!expandedRowIds[String(rack.id || "")],
      };
    });
  };

  // Render expanded content for Rooms
  const renderExpandedContentRoom = (row) => {
    // Find the original room data from the formatted row
    const room = rooms.find((r) => String(r.id) === row.id);
    if (!room) return null;

    const formatDate = (dateString) => {
      if (!dateString) return "N/A";
      try {
        return intl.formatDate(new Date(dateString), {
          year: "numeric",
          month: "short",
          day: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        });
      } catch (e) {
        return "N/A";
      }
    };

    return (
      <div
        role="region"
        aria-label="Additional room details"
        data-testid={`expanded-room-${row.id}`}
        style={{ padding: "1rem" }}
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <div
              data-testid={`expanded-room-${row.id}-description`}
              style={{ marginBottom: "0.5rem" }}
            >
              <strong>
                <FormattedMessage
                  id="storage.expanded.description"
                  defaultMessage="Description"
                />
                :
              </strong>{" "}
              {room.description || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div
              data-testid={`expanded-room-${row.id}-created-date`}
              style={{ marginBottom: "0.5rem" }}
            >
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdDate"
                  defaultMessage="Created Date"
                />
                :
              </strong>{" "}
              {formatDate(room.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div
              data-testid={`expanded-room-${row.id}-created-by`}
              style={{ marginBottom: "0.5rem" }}
            >
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdBy"
                  defaultMessage="Created By"
                />
                :
              </strong>{" "}
              {room.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div
              data-testid={`expanded-room-${row.id}-last-modified-date`}
              style={{ marginBottom: "0.5rem" }}
            >
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedDate"
                  defaultMessage="Last Modified Date"
                />
                :
              </strong>{" "}
              {formatDate(room.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div
              data-testid={`expanded-room-${row.id}-last-modified-by`}
              style={{ marginBottom: "0.5rem" }}
            >
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedBy"
                  defaultMessage="Last Modified By"
                />
                :
              </strong>{" "}
              {room.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
        </Grid>
      </div>
    );
  };

  // Render expanded content for Devices
  const renderExpandedContentDevice = (row) => {
    const device = devices.find((d) => String(d.id) === row.id);
    if (!device) return null;

    const formatDate = (dateString) => {
      if (!dateString) return "N/A";
      try {
        return intl.formatDate(new Date(dateString), {
          year: "numeric",
          month: "short",
          day: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        });
      } catch (e) {
        return "N/A";
      }
    };

    return (
      <div
        role="region"
        aria-label="Additional device details"
        data-testid={`expanded-device-${row.id}`}
        style={{ padding: "1rem" }}
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.temperatureSetting"
                  defaultMessage="Temperature Setting"
                />
                :
              </strong>{" "}
              {device.temperatureSetting != null ? (
                `${device.temperatureSetting}°C`
              ) : (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.capacityLimit"
                  defaultMessage="Capacity Limit"
                />
                :
              </strong>{" "}
              {device.capacityLimit != null ? (
                device.capacityLimit
              ) : (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.description"
                  defaultMessage="Description"
                />
                :
              </strong>{" "}
              {device.description || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdDate"
                  defaultMessage="Created Date"
                />
                :
              </strong>{" "}
              {formatDate(device.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdBy"
                  defaultMessage="Created By"
                />
                :
              </strong>{" "}
              {device.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedDate"
                  defaultMessage="Last Modified Date"
                />
                :
              </strong>{" "}
              {formatDate(device.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedBy"
                  defaultMessage="Last Modified By"
                />
                :
              </strong>{" "}
              {device.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
        </Grid>
      </div>
    );
  };

  // Render expanded content for Shelves
  const renderExpandedContentShelf = (row) => {
    const shelf = shelves.find((s) => String(s.id) === row.id);
    if (!shelf) return null;

    const formatDate = (dateString) => {
      if (!dateString) return "N/A";
      try {
        return intl.formatDate(new Date(dateString), {
          year: "numeric",
          month: "short",
          day: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        });
      } catch (e) {
        return "N/A";
      }
    };

    return (
      <div
        role="region"
        aria-label="Additional shelf details"
        data-testid={`expanded-shelf-${row.id}`}
        style={{ padding: "1rem" }}
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.capacityLimit"
                  defaultMessage="Capacity Limit"
                />
                :
              </strong>{" "}
              {shelf.capacityLimit != null ? (
                shelf.capacityLimit
              ) : (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.description"
                  defaultMessage="Description"
                />
                :
              </strong>{" "}
              {shelf.description || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdDate"
                  defaultMessage="Created Date"
                />
                :
              </strong>{" "}
              {formatDate(shelf.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdBy"
                  defaultMessage="Created By"
                />
                :
              </strong>{" "}
              {shelf.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedDate"
                  defaultMessage="Last Modified Date"
                />
                :
              </strong>{" "}
              {formatDate(shelf.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedBy"
                  defaultMessage="Last Modified By"
                />
                :
              </strong>{" "}
              {shelf.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
        </Grid>
      </div>
    );
  };

  // Render expanded content for Racks
  const renderExpandedContentRack = (row) => {
    const rack = racks.find((r) => String(r.id) === row.id);
    if (!rack) return null;

    const formatDate = (dateString) => {
      if (!dateString) return "N/A";
      try {
        return intl.formatDate(new Date(dateString), {
          year: "numeric",
          month: "short",
          day: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        });
      } catch (e) {
        return "N/A";
      }
    };

    return (
      <div
        role="region"
        aria-label="Additional rack details"
        data-testid={`expanded-rack-${row.id}`}
        style={{ padding: "1rem" }}
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.positionSchemaHint"
                  defaultMessage="Position Schema Hint"
                />
                :
              </strong>{" "}
              {rack.positionSchemaHint || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.description"
                  defaultMessage="Description"
                />
                :
              </strong>{" "}
              {rack.description || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdDate"
                  defaultMessage="Created Date"
                />
                :
              </strong>{" "}
              {formatDate(rack.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.createdBy"
                  defaultMessage="Created By"
                />
                :
              </strong>{" "}
              {rack.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedDate"
                  defaultMessage="Last Modified Date"
                />
                :
              </strong>{" "}
              {formatDate(rack.lastupdated)}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="storage.expanded.lastModifiedBy"
                  defaultMessage="Last Modified By"
                />
                :
              </strong>{" "}
              {rack.sysUserId || (
                <FormattedMessage
                  id="storage.expanded.notAvailable"
                  defaultMessage="N/A"
                />
              )}
            </div>
          </Column>
        </Grid>
      </div>
    );
  };

  // Format Sample Items data for table (per spec: SampleItem ID/External ID as primary, Sample accession as secondary)
  const formatSamplesData = (samplesData) => {
    if (!samplesData || samplesData.length === 0) {
      return [];
    }
    return samplesData.map((sampleItem) => {
      // Primary identifier: SampleItem ID or External ID (prefer External ID if available)
      const sampleItemId = String(
        sampleItem.sampleItemId || sampleItem.id || "",
      );
      const sampleItemExternalId = sampleItem.sampleItemExternalId || null;
      const displayId = sampleItemExternalId || sampleItemId;

      // Secondary context: Parent Sample accession number
      const sampleAccessionNumber = sampleItem.sampleAccessionNumber || "";

      return {
        id: sampleItemId, // Use sampleItemId for row ID
        sampleItemId: displayId, // Display: External ID if available, otherwise ID
        sampleAccessionNumber: sampleAccessionNumber, // Parent Sample accession for context
        type: sampleItem.type || sampleItem.sampleType || "",
        status:
          sampleItem.status === "disposed" ||
          sampleItem.status === "Disposed" ? (
            <Tag type="red">
              <FormattedMessage id="storage.status.disposed" />
            </Tag>
          ) : (
            <Tag type="green">
              <FormattedMessage id="label.active" />
            </Tag>
          ),
        location: sampleItem.location || sampleItem.hierarchicalPath || "",
        assignedBy: sampleItem.assignedBy || sampleItem.assignedByUserId || "",
        date: sampleItem.date || sampleItem.assignedDate || "",
        actions: (
          <SampleActionsContainer
            sample={{
              id: sampleItemId,
              sampleId: sampleItemId,
              sampleItemId: sampleItemId,
              sampleItemExternalId: sampleItemExternalId,
              sampleAccessionNumber: sampleAccessionNumber,
              type: sampleItem.type || sampleItem.sampleType || "",
              status: sampleItem.status || "Active",
              location:
                sampleItem.location || sampleItem.hierarchicalPath || "",
              positionCoordinate: sampleItem.positionCoordinate || "",
              notes: sampleItem.notes || "",
            }}
            onManageLocation={handleManageLocation}
            onDispose={handleDispose}
          />
        ),
      };
    });
  };

  const filteredRooms = filterData(rooms, "rooms");
  const filteredDevices = filterData(devices, "devices");
  const filteredShelves = filterData(shelves, "shelves");
  const filteredRacks = filterData(racks, "racks");
  const filteredSamples = filterData(samples, "samples");
  const rackDropdownItems = (racks || []).map((rack) => ({
    id: rack.id,
    label: rack.label,
    description: rack.roomName ? `${rack.roomName}` : rack.label,
    ...rack,
  }));

  const boxDropdownItems = (boxesForGrid || []).map((box) => ({
    id: box.id,
    label: box.label,
    description: `${box.type || ""} (${box.rows || 0}×${box.columns || 0}) ${box.occupied ? "• Occupied" : ""}`,
    ...box,
  }));

  const renderBoxGrid = () => {
    if (!selectedBox) {
      return (
        <Tile className="rack-grid-placeholder">
          <FormattedMessage
            id="storage.boxes.selectBox"
            defaultMessage="Select a box to view its grid."
          />
        </Tile>
      );
    }

    const rows = Array.from({ length: selectedBox.rows || 0 });
    const cols = Array.from({ length: selectedBox.columns || 0 });
    // occupiedCoordinates is now an object: { "A1": { externalId: "...", sampleItemId: "..." }, ... }
    const occupiedCoordinates = selectedBox.occupiedCoordinates || {};

    // Generate coordinate labels based on position schema hint
    const getCoordinateLabel = (rowIdx, colIdx) => {
      const hint = selectedBox.positionSchemaHint || "letter-number";
      if (hint === "letter-number") {
        // A1, A2, ..., B1, B2, etc.
        const letter = String.fromCharCode(65 + rowIdx); // A=65
        return `${letter}${colIdx + 1}`;
      } else {
        // 1-1, 1-2, etc.
        return `${rowIdx + 1}-${colIdx + 1}`;
      }
    };

    return (
      <div className="rack-grid">
        {rows.map((_, rowIdx) => (
          <div className="rack-grid-row" key={`row-${rowIdx}`}>
            {cols.map((__, colIdx) => {
              const coordinate = getCoordinateLabel(rowIdx, colIdx);
              const isSelected = selectedCoordinate === coordinate;
              const sampleInfo = occupiedCoordinates[coordinate];
              const occupied = !!sampleInfo;
              const externalId = sampleInfo?.externalId || "";
              const tooltip = externalId || sampleInfo?.sampleItemId || "";

              return (
                <button
                  key={`cell-${rowIdx}-${colIdx}`}
                  type="button"
                  className={`rack-grid-cell ${
                    occupied ? "occupied" : "available"
                  } ${isSelected ? "selected" : ""}`}
                  onClick={() => handleCoordinateSelect(coordinate, occupied)}
                  aria-disabled={occupied}
                  title={
                    occupied && tooltip
                      ? `Sample: ${tooltip}`
                      : occupied
                        ? intl.formatMessage({
                            id: "storage.boxes.status.occupied",
                            defaultMessage: "Occupied",
                          })
                        : undefined
                  }
                  aria-label={
                    occupied
                      ? intl.formatMessage(
                          {
                            id: "storage.boxes.grid.occupied",
                            defaultMessage: "Position {coordinate} (occupied)",
                          },
                          { coordinate },
                        )
                      : intl.formatMessage(
                          {
                            id: "storage.boxes.grid.available",
                            defaultMessage: "Position {coordinate} (available)",
                          },
                          { coordinate },
                        )
                  }
                >
                  <span className="rack-grid-label">{coordinate}</span>
                  <span className="rack-grid-status">
                    {occupied
                      ? intl.formatMessage({
                          id: "storage.boxes.status.occupied",
                          defaultMessage: "Occupied",
                        })
                      : intl.formatMessage({
                          id: "storage.boxes.status.available",
                          defaultMessage: "Available",
                        })}
                  </span>
                </button>
              );
            })}
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="storage-dashboard">
      {notificationVisible && <AlertDialog />}
      <Grid fullWidth>
        {/* Dashboard Title */}
        <Column lg={16} md={8} sm={4}>
          <h1 className="dashboard-title">
            <FormattedMessage
              id="storage.dashboard.title"
              defaultMessage="Storage Management Dashboard"
            />
          </h1>
        </Column>

        {/* Metric Cards */}
        <Column lg={4} md={4} sm={4}>
          <Tile>
            <h3>
              <FormattedMessage id="storage.metrics.total.samples" />
            </h3>
            <p className="metric-value">{metrics.totalSamples}</p>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile>
            <h3>
              <FormattedMessage id="storage.metrics.active" />
            </h3>
            <p className="metric-value">{metrics.active}</p>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="metric-disposed">
            <h3>
              <FormattedMessage id="storage.metrics.disposed" />
            </h3>
            <p className="metric-value">{metrics.disposed}</p>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile>
            <StorageLocationsMetricCard />
          </Tile>
        </Column>

        {/* Tabs - positioned right below metric cards */}
        <Column lg={16} md={8} sm={4} className="tabs-column">
          <Tabs selectedIndex={selectedTab} onChange={handleTabChange}>
            <TabList aria-label="Storage dashboard tabs" contained>
              <Tab data-testid="tab-samples">
                <FormattedMessage id="storage.tab.samples" />
              </Tab>
              <Tab className="tab-rooms" data-testid="tab-rooms">
                <FormattedMessage id="storage.tab.rooms" />
              </Tab>
              <Tab className="tab-devices" data-testid="tab-devices">
                <FormattedMessage id="storage.tab.devices" />
              </Tab>
              <Tab className="tab-shelves" data-testid="tab-shelves">
                <FormattedMessage id="storage.tab.shelves" />
              </Tab>
              <Tab className="tab-racks" data-testid="tab-racks">
                <FormattedMessage id="storage.tab.racks" />
              </Tab>
              <Tab className="tab-boxes" data-testid="tab-boxes">
                <FormattedMessage
                  id="storage.tab.boxes"
                  defaultMessage="Boxes"
                />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <Grid fullWidth>
                  {/* Search - full width */}
                  <Column lg={16} md={8} sm={4} className="search-section">
                    <Search
                      data-testid="sample-search-input"
                      labelText={intl.formatMessage({
                        id: "storage.search.samples.placeholder",
                        defaultMessage: "Search by sample ID or location...",
                      })}
                      placeholder={intl.formatMessage({
                        id: "storage.search.samples.placeholder",
                        defaultMessage: "Search by sample ID or location...",
                      })}
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      size="lg"
                    />
                  </Column>

                  {/* Filters - own row */}
                  {(visibleFilters.room ||
                    visibleFilters.device ||
                    visibleFilters.status) && (
                    <Column lg={16} md={8} sm={4}>
                      <Grid className="filters-row">
                        {/* Samples tab: Single location dropdown (replaces separate room/device dropdowns per FR-065b) */}
                        {selectedTab === 0 &&
                          (visibleFilters.room || visibleFilters.device) && (
                            <Column lg={6} md={6} sm={4}>
                              <LocationFilterDropdown
                                onLocationChange={setLocationFilter}
                                selectedLocation={locationFilter}
                                allowInactive={true}
                              />
                            </Column>
                          )}
                        {/* Other tabs: Keep existing room/device filters */}
                        {selectedTab !== 0 && visibleFilters.room && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-room"
                              data-testid="room-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.room",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                ...rooms.map((r) => ({
                                  id: r.id,
                                  label: r.name,
                                })),
                              ]}
                              selectedItem={
                                filterRoom
                                  ? {
                                      id: filterRoom,
                                      label:
                                        rooms.find((r) => r.id === filterRoom)
                                          ?.name ||
                                        intl.formatMessage({
                                          id: "storage.filter.room",
                                        }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.room",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterRoom(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {selectedTab !== 0 && visibleFilters.device && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-device"
                              data-testid="device-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.device",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                ...devices.map((d) => ({
                                  id: d.id,
                                  label: d.name,
                                })),
                              ]}
                              selectedItem={
                                filterDevice
                                  ? {
                                      id: filterDevice,
                                      label:
                                        devices.find(
                                          (d) => d.id === filterDevice,
                                        )?.name ||
                                        intl.formatMessage({
                                          id: "storage.filter.device",
                                        }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.device",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterDevice(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {visibleFilters.status && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-status"
                              data-testid="status-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.status",
                              })}
                              items={
                                selectedTab === 0
                                  ? sampleStatusOptions
                                  : [
                                      // Other tabs: active/inactive (boolean field)
                                      {
                                        id: "",
                                        label: intl.formatMessage({
                                          id: "label.all",
                                        }),
                                      },
                                      {
                                        id: "true",
                                        label: intl.formatMessage({
                                          id: "label.active",
                                        }),
                                      },
                                      {
                                        id: "false",
                                        label: intl.formatMessage({
                                          id: "label.inactive",
                                        }),
                                      },
                                    ]
                              }
                              selectedItem={
                                filterStatus
                                  ? selectedTab === 0
                                    ? sampleStatusOptions.find(
                                        (opt) => opt.id === filterStatus,
                                      ) || {
                                        id: "",
                                        label: intl.formatMessage({
                                          id: "label.all",
                                        }),
                                      }
                                    : {
                                        id: filterStatus,
                                        label:
                                          filterStatus === "true"
                                            ? intl.formatMessage({
                                                id: "label.active",
                                              })
                                            : intl.formatMessage({
                                                id: "label.inactive",
                                              }),
                                      }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.status",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterStatus(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {/* Clear Filters button (per FR-067) */}
                        {(locationFilter ||
                          filterRoom ||
                          filterDevice ||
                          filterStatus) && (
                          <Column lg={2} md={2} sm={4}>
                            <Button
                              kind="secondary"
                              size="md"
                              data-testid="clear-filters-button"
                              onClick={() => {
                                setLocationFilter(null);
                                setFilterRoom("");
                                setFilterDevice("");
                                setFilterStatus("");
                              }}
                            >
                              <FormattedMessage
                                id="storage.filter.clear"
                                defaultMessage="Clear Filters"
                              />
                            </Button>
                          </Column>
                        )}
                      </Grid>
                    </Column>
                  )}

                  {/* Table with title */}
                  <Column lg={16} md={8} sm={4} className="table-section">
                    <h3 className="table-title">
                      <FormattedMessage id="storage.tab.samples" />
                    </h3>
                    <div data-testid="sample-list">
                      <DataTable
                        rows={formatSamplesData(filteredSamples)}
                        headers={samplesHeaders}
                        isSortable
                      >
                        {({
                          rows,
                          headers,
                          getTableProps,
                          getHeaderProps,
                          getRowProps,
                        }) => (
                          <TableContainer>
                            <Table {...getTableProps()}>
                              <TableHead>
                                <TableRow>
                                  {headers.map((header) => (
                                    <TableHeader
                                      key={
                                        header.key || header.id || header.header
                                      }
                                      {...getHeaderProps({ header })}
                                    >
                                      {header.header}
                                    </TableHeader>
                                  ))}
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {rows.map((row) => (
                                  <TableRow
                                    key={row.id || row.key}
                                    data-testid="sample-row"
                                    {...getRowProps({ row })}
                                  >
                                    {row.cells.map((cell, index) => {
                                      // Add test IDs to location and position cells
                                      const testId =
                                        cell.info.header === "location"
                                          ? "sample-location"
                                          : cell.info.header === "sampleId"
                                            ? "sample-id"
                                            : null;
                                      return (
                                        <TableCell
                                          key={cell.id}
                                          data-testid={testId || undefined}
                                        >
                                          {cell.value}
                                        </TableCell>
                                      );
                                    })}
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          </TableContainer>
                        )}
                      </DataTable>
                    </div>
                  </Column>
                  {/* OGC-150: Pagination for Samples tab */}
                  <Column lg={16} md={8} sm={4}>
                    <Pagination
                      data-testid="sample-items-pagination"
                      page={page}
                      pageSize={pageSize}
                      pageSizes={[5, 25, 50, 100]}
                      totalItems={totalItems}
                      onChange={({ page, pageSize }) => {
                        // eslint-disable-next-line no-console
                        console.info("[OGC-150] pagination change", {
                          page,
                          pageSize,
                          totalItems,
                        });
                        setPage(page);
                        setPageSize(pageSize);
                      }}
                    />
                  </Column>
                </Grid>
              </TabPanel>
              <TabPanel>
                <Grid fullWidth>
                  {/* Search - full width */}
                  <Column lg={16} md={8} sm={4} className="search-section">
                    <Search
                      data-testid="room-search-input"
                      labelText={intl.formatMessage({
                        id: "storage.search.rooms.placeholder",
                        defaultMessage: "Search by room name or code...",
                      })}
                      placeholder={intl.formatMessage({
                        id: "storage.search.rooms.placeholder",
                        defaultMessage: "Search by room name or code...",
                      })}
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      size="lg"
                    />
                  </Column>

                  {/* Filters - own row */}
                  {visibleFilters.status && (
                    <Column lg={16} md={8} sm={4}>
                      <Grid className="filters-row">
                        <Column lg={4} md={4} sm={4}>
                          <Dropdown
                            id="filter-status"
                            data-testid="status-filter"
                            label=""
                            hideLabel
                            titleText={intl.formatMessage({
                              id: "storage.filter.status",
                            })}
                            items={[
                              {
                                id: "",
                                label: intl.formatMessage({ id: "label.all" }),
                              },
                              {
                                id: "true",
                                label: intl.formatMessage({
                                  id: "label.active",
                                }),
                              },
                              {
                                id: "false",
                                label: intl.formatMessage({
                                  id: "label.inactive",
                                }),
                              },
                            ]}
                            selectedItem={
                              filterStatus
                                ? {
                                    id: filterStatus,
                                    label:
                                      filterStatus === "true"
                                        ? intl.formatMessage({
                                            id: "label.active",
                                          })
                                        : intl.formatMessage({
                                            id: "label.inactive",
                                          }),
                                  }
                                : {
                                    id: "",
                                    label: intl.formatMessage({
                                      id: "storage.filter.status",
                                    }),
                                  }
                            }
                            onChange={(e) =>
                              setFilterStatus(e.selectedItem?.id || "")
                            }
                          />
                        </Column>
                        <Column lg={4} md={4} sm={4}>
                          <Button
                            kind="secondary"
                            onClick={() => {
                              setFilterStatus("");
                              setSearchTerm("");
                            }}
                          >
                            <FormattedMessage id="label.clear" />
                          </Button>
                        </Column>
                      </Grid>
                    </Column>
                  )}

                  {/* Table with title */}
                  <Column lg={16} md={8} sm={4} className="table-section">
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "1rem",
                      }}
                    >
                      <h3 className="table-title" style={{ margin: 0 }}>
                        <FormattedMessage id="storage.tab.rooms" />
                      </h3>
                      <Button
                        kind="primary"
                        onClick={() => handleCreateLocation()}
                        data-testid="add-room-button"
                      >
                        <FormattedMessage
                          id="storage.add.room"
                          defaultMessage="Add Room"
                        />
                      </Button>
                    </div>
                    <DataTable
                      rows={formatRoomsData(filteredRooms)}
                      headers={roomsHeaders}
                      isSortable
                      expandableRows
                    >
                      {({
                        rows,
                        headers,
                        getTableProps,
                        getHeaderProps,
                        getRowProps,
                      }) => (
                        <TableContainer>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                <TableExpandHeader aria-label="expand row" />
                                {headers.map((header) => (
                                  <TableHeader
                                    key={
                                      header.key || header.id || header.header
                                    }
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <React.Fragment key={row.id || row.key}>
                                  <TableExpandRow
                                    data-testid={`room-row-${row.id}`}
                                    isExpanded={
                                      !!expandedRowIds[String(row.id)]
                                    }
                                    {...getRowProps({
                                      row,
                                      onClick: (e) => {
                                        const target = e.target;

                                        // Don't expand if clicking on action button (overflow menu)
                                        if (
                                          target.closest(
                                            '[data-testid="location-actions-overflow-menu"]',
                                          ) ||
                                          target.closest(
                                            ".cds--overflow-menu",
                                          ) ||
                                          target.closest(
                                            'button[aria-label*="Location actions"]',
                                          )
                                        ) {
                                          return; // Let the action button handle its own click
                                        }

                                        // Expand on click anywhere else in the row (including expand button)
                                        handleRowExpand(row.id);
                                      },
                                    })}
                                  >
                                    {row.cells.map((cell) => (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    ))}
                                  </TableExpandRow>
                                  {expandedRowIds[String(row.id)] && (
                                    <TableExpandedRow
                                      data-testid={`expanded-room-${row.id}`}
                                      colSpan={headers.length + 1}
                                    >
                                      {renderExpandedContentRoom(row)}
                                    </TableExpandedRow>
                                  )}
                                </React.Fragment>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>
              <TabPanel>
                <Grid fullWidth>
                  {/* Search - full width */}
                  <Column lg={16} md={8} sm={4} className="search-section">
                    <Search
                      data-testid="device-search-input"
                      labelText={intl.formatMessage({
                        id: "storage.search.devices.placeholder",
                        defaultMessage: "Search by device name or code...",
                      })}
                      placeholder={intl.formatMessage({
                        id: "storage.search.devices.placeholder",
                        defaultMessage: "Search by device name or code...",
                      })}
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      size="lg"
                    />
                  </Column>

                  {/* Filters - own row */}
                  {(visibleFilters.room || visibleFilters.status) && (
                    <Column lg={16} md={8} sm={4}>
                      <Grid className="filters-row">
                        {visibleFilters.room && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-room"
                              data-testid="room-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.room",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                ...rooms.map((r) => ({
                                  id: r.id,
                                  label: r.name,
                                })),
                              ]}
                              selectedItem={
                                filterRoom
                                  ? {
                                      id: filterRoom,
                                      label:
                                        rooms.find((r) => r.id === filterRoom)
                                          ?.name ||
                                        intl.formatMessage({
                                          id: "storage.filter.room",
                                        }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.room",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterRoom(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {visibleFilters.status && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-status"
                              data-testid="status-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.status",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                {
                                  id: "true",
                                  label: intl.formatMessage({
                                    id: "label.active",
                                  }),
                                },
                                {
                                  id: "false",
                                  label: intl.formatMessage({
                                    id: "label.inactive",
                                  }),
                                },
                              ]}
                              selectedItem={
                                filterStatus
                                  ? {
                                      id: filterStatus,
                                      label:
                                        filterStatus === "true"
                                          ? intl.formatMessage({
                                              id: "label.active",
                                            })
                                          : intl.formatMessage({
                                              id: "label.inactive",
                                            }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.status",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterStatus(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        <Column lg={4} md={4} sm={4}>
                          <Button
                            kind="secondary"
                            onClick={() => {
                              setFilterRoom("");
                              setFilterStatus("");
                              setSearchTerm("");
                            }}
                          >
                            <FormattedMessage id="label.clear" />
                          </Button>
                        </Column>
                      </Grid>
                    </Column>
                  )}

                  {/* Table with title */}
                  <Column lg={16} md={8} sm={4} className="table-section">
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "1rem",
                      }}
                    >
                      <h3 className="table-title" style={{ margin: 0 }}>
                        <FormattedMessage id="storage.tab.devices" />
                      </h3>
                      <Button
                        kind="primary"
                        onClick={() => {
                          // For devices, parent room is required - use filterRoom if set, or first room
                          const parentRoom = filterRoom
                            ? rooms.find((r) => r.id === filterRoom)
                            : rooms.length > 0
                              ? rooms[0]
                              : null;
                          handleCreateLocation(parentRoom);
                        }}
                        disabled={rooms.length === 0}
                        data-testid="add-device-button"
                      >
                        <FormattedMessage
                          id="storage.add.device"
                          defaultMessage="Add Device"
                        />
                      </Button>
                    </div>
                    <DataTable
                      rows={formatDevicesData(filteredDevices)}
                      headers={devicesHeaders}
                      isSortable
                      expandableRows
                    >
                      {({
                        rows,
                        headers,
                        getTableProps,
                        getHeaderProps,
                        getRowProps,
                      }) => (
                        <TableContainer>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                <TableExpandHeader aria-label="expand row" />
                                {headers.map((header) => (
                                  <TableHeader
                                    key={
                                      header.key || header.id || header.header
                                    }
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <React.Fragment key={row.id || row.key}>
                                  <TableExpandRow
                                    data-testid={`device-row-${row.id}`}
                                    isExpanded={row.isExpanded}
                                    onExpand={() => handleRowExpand(row.id)}
                                    ariaLabel={
                                      row.isExpanded
                                        ? intl.formatMessage({
                                            id: "carbon.table.row.collapse",
                                            defaultMessage:
                                              "Collapse current row",
                                          })
                                        : intl.formatMessage({
                                            id: "carbon.table.row.expand",
                                            defaultMessage:
                                              "Expand current row",
                                          })
                                    }
                                    {...getRowProps({
                                      row,
                                      onClick: (e) => {
                                        const target = e.target;

                                        // Don't expand if clicking on action button (overflow menu)
                                        if (
                                          target.closest(
                                            '[data-testid="location-actions-overflow-menu"]',
                                          ) ||
                                          target.closest(
                                            ".cds--overflow-menu",
                                          ) ||
                                          target.closest(
                                            'button[aria-label*="Location actions"]',
                                          )
                                        ) {
                                          e.stopPropagation();
                                          return;
                                        }

                                        // Expand on click anywhere else in the row (including expand button)
                                        handleRowExpand(row.id);
                                      },
                                    })}
                                  >
                                    {row.cells.map((cell) => (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    ))}
                                  </TableExpandRow>
                                  {expandedRowIds[String(row.id)] && (
                                    <TableExpandedRow
                                      data-testid={`expanded-device-${row.id}`}
                                      colSpan={headers.length + 1}
                                    >
                                      {renderExpandedContentDevice(row)}
                                    </TableExpandedRow>
                                  )}
                                </React.Fragment>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>
              <TabPanel>
                <Grid fullWidth>
                  {/* Search - full width */}
                  <Column lg={16} md={8} sm={4} className="search-section">
                    <Search
                      data-testid="shelf-search-input"
                      labelText={intl.formatMessage({
                        id: "storage.search.shelves.placeholder",
                        defaultMessage: "Search by shelf label...",
                      })}
                      placeholder={intl.formatMessage({
                        id: "storage.search.shelves.placeholder",
                        defaultMessage: "Search by shelf label...",
                      })}
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      size="lg"
                    />
                  </Column>

                  {/* Filters - own row */}
                  {(visibleFilters.room ||
                    visibleFilters.device ||
                    visibleFilters.status) && (
                    <Column lg={16} md={8} sm={4}>
                      <Grid className="filters-row">
                        {visibleFilters.room && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-room"
                              data-testid="room-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.room",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                ...rooms.map((r) => ({
                                  id: r.id,
                                  label: r.name,
                                })),
                              ]}
                              selectedItem={
                                filterRoom
                                  ? {
                                      id: filterRoom,
                                      label:
                                        rooms.find((r) => r.id === filterRoom)
                                          ?.name ||
                                        intl.formatMessage({
                                          id: "storage.filter.room",
                                        }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.room",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterRoom(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {visibleFilters.device && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-device"
                              data-testid="device-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.device",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                ...devices.map((d) => ({
                                  id: d.id,
                                  label: d.name,
                                })),
                              ]}
                              selectedItem={
                                filterDevice
                                  ? {
                                      id: filterDevice,
                                      label:
                                        devices.find(
                                          (d) => d.id === filterDevice,
                                        )?.name ||
                                        intl.formatMessage({
                                          id: "storage.filter.device",
                                        }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.device",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterDevice(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {visibleFilters.status && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-status"
                              data-testid="status-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.status",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                {
                                  id: "true",
                                  label: intl.formatMessage({
                                    id: "label.active",
                                  }),
                                },
                                {
                                  id: "false",
                                  label: intl.formatMessage({
                                    id: "label.inactive",
                                  }),
                                },
                              ]}
                              selectedItem={
                                filterStatus
                                  ? {
                                      id: filterStatus,
                                      label:
                                        filterStatus === "true"
                                          ? intl.formatMessage({
                                              id: "label.active",
                                            })
                                          : intl.formatMessage({
                                              id: "label.inactive",
                                            }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.status",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterStatus(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        <Column lg={4} md={4} sm={4}>
                          <Button
                            kind="secondary"
                            onClick={() => {
                              setFilterRoom("");
                              setFilterDevice("");
                              setFilterStatus("");
                              setSearchTerm("");
                            }}
                          >
                            <FormattedMessage id="label.clear" />
                          </Button>
                        </Column>
                      </Grid>
                    </Column>
                  )}

                  {/* Table with title */}
                  <Column lg={16} md={8} sm={4} className="table-section">
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "1rem",
                      }}
                    >
                      <h3 className="table-title" style={{ margin: 0 }}>
                        <FormattedMessage id="storage.tab.shelves" />
                      </h3>
                      <Button
                        kind="primary"
                        onClick={() => {
                          // For shelves, parent device is required - use filterDevice if set, or first device
                          const parentDevice = filterDevice
                            ? devices.find((d) => d.id === filterDevice)
                            : devices.length > 0
                              ? devices[0]
                              : null;
                          handleCreateLocation(parentDevice);
                        }}
                        disabled={devices.length === 0}
                        data-testid="add-shelf-button"
                      >
                        <FormattedMessage
                          id="storage.add.shelf"
                          defaultMessage="Add Shelf"
                        />
                      </Button>
                    </div>
                    <DataTable
                      rows={formatShelvesData(filteredShelves)}
                      headers={shelvesHeaders}
                      isSortable
                      expandableRows
                    >
                      {({
                        rows,
                        headers,
                        getTableProps,
                        getHeaderProps,
                        getRowProps,
                      }) => (
                        <TableContainer>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                <TableExpandHeader aria-label="expand row" />
                                {headers.map((header) => (
                                  <TableHeader
                                    key={
                                      header.key || header.id || header.header
                                    }
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <React.Fragment key={row.id || row.key}>
                                  <TableExpandRow
                                    data-testid={`shelf-row-${row.id}`}
                                    isExpanded={row.isExpanded}
                                    onExpand={() => handleRowExpand(row.id)}
                                    ariaLabel={
                                      row.isExpanded
                                        ? intl.formatMessage({
                                            id: "carbon.table.row.collapse",
                                            defaultMessage:
                                              "Collapse current row",
                                          })
                                        : intl.formatMessage({
                                            id: "carbon.table.row.expand",
                                            defaultMessage:
                                              "Expand current row",
                                          })
                                    }
                                    {...getRowProps({
                                      row,
                                      onClick: (e) => {
                                        const target = e.target;

                                        // Don't expand if clicking on action button (overflow menu)
                                        if (
                                          target.closest(
                                            '[data-testid="location-actions-overflow-menu"]',
                                          ) ||
                                          target.closest(
                                            ".cds--overflow-menu",
                                          ) ||
                                          target.closest(
                                            'button[aria-label*="Location actions"]',
                                          )
                                        ) {
                                          e.stopPropagation();
                                          return;
                                        }

                                        // Expand on click anywhere else in the row (including expand button)
                                        handleRowExpand(row.id);
                                      },
                                    })}
                                  >
                                    {row.cells.map((cell) => (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    ))}
                                  </TableExpandRow>
                                  {expandedRowIds[String(row.id)] && (
                                    <TableExpandedRow
                                      data-testid={`expanded-shelf-${row.id}`}
                                      colSpan={headers.length + 1}
                                    >
                                      {renderExpandedContentShelf(row)}
                                    </TableExpandedRow>
                                  )}
                                </React.Fragment>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>
              <TabPanel>
                <Grid fullWidth>
                  {/* Search - full width */}
                  <Column lg={16} md={8} sm={4} className="search-section">
                    <Search
                      data-testid="rack-search-input"
                      labelText={intl.formatMessage({
                        id: "storage.search.racks.placeholder",
                        defaultMessage: "Search by rack label...",
                      })}
                      placeholder={intl.formatMessage({
                        id: "storage.search.racks.placeholder",
                        defaultMessage: "Search by rack label...",
                      })}
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      size="lg"
                    />
                  </Column>

                  {/* Filters - own row */}
                  {(visibleFilters.room ||
                    visibleFilters.device ||
                    visibleFilters.status) && (
                    <Column lg={16} md={8} sm={4}>
                      <Grid className="filters-row">
                        {visibleFilters.room && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-room"
                              data-testid="room-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.room",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                ...rooms.map((r) => ({
                                  id: r.id,
                                  label: r.name,
                                })),
                              ]}
                              selectedItem={
                                filterRoom
                                  ? {
                                      id: filterRoom,
                                      label:
                                        rooms.find((r) => r.id === filterRoom)
                                          ?.name ||
                                        intl.formatMessage({
                                          id: "storage.filter.room",
                                        }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.room",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterRoom(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {visibleFilters.device && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-device"
                              data-testid="device-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.device",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                ...devices.map((d) => ({
                                  id: d.id,
                                  label: d.name,
                                })),
                              ]}
                              selectedItem={
                                filterDevice
                                  ? {
                                      id: filterDevice,
                                      label:
                                        devices.find(
                                          (d) => d.id === filterDevice,
                                        )?.name ||
                                        intl.formatMessage({
                                          id: "storage.filter.device",
                                        }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.device",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterDevice(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        {visibleFilters.status && (
                          <Column lg={4} md={4} sm={4}>
                            <Dropdown
                              id="filter-status"
                              data-testid="status-filter"
                              label=""
                              hideLabel
                              titleText={intl.formatMessage({
                                id: "storage.filter.status",
                              })}
                              items={[
                                {
                                  id: "",
                                  label: intl.formatMessage({
                                    id: "label.all",
                                  }),
                                },
                                {
                                  id: "true",
                                  label: intl.formatMessage({
                                    id: "label.active",
                                  }),
                                },
                                {
                                  id: "false",
                                  label: intl.formatMessage({
                                    id: "label.inactive",
                                  }),
                                },
                              ]}
                              selectedItem={
                                filterStatus
                                  ? {
                                      id: filterStatus,
                                      label:
                                        filterStatus === "true"
                                          ? intl.formatMessage({
                                              id: "label.active",
                                            })
                                          : intl.formatMessage({
                                              id: "label.inactive",
                                            }),
                                    }
                                  : {
                                      id: "",
                                      label: intl.formatMessage({
                                        id: "storage.filter.status",
                                      }),
                                    }
                              }
                              onChange={(e) =>
                                setFilterStatus(e.selectedItem?.id || "")
                              }
                            />
                          </Column>
                        )}
                        <Column lg={4} md={4} sm={4}>
                          <Button
                            kind="secondary"
                            onClick={() => {
                              setFilterRoom("");
                              setFilterDevice("");
                              setFilterStatus("");
                              setSearchTerm("");
                            }}
                          >
                            <FormattedMessage id="label.clear" />
                          </Button>
                        </Column>
                      </Grid>
                    </Column>
                  )}

                  {/* Table with title */}
                  <Column lg={16} md={8} sm={4} className="table-section">
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "1rem",
                      }}
                    >
                      <h3 className="table-title" style={{ margin: 0 }}>
                        <FormattedMessage id="storage.tab.racks" />
                      </h3>
                      <Button
                        kind="primary"
                        onClick={() => {
                          // For racks, parent shelf is required - use first shelf from filtered shelves
                          const parentShelf =
                            filteredShelves.length > 0
                              ? filteredShelves[0]
                              : shelves.length > 0
                                ? shelves[0]
                                : null;
                          handleCreateLocation(parentShelf);
                        }}
                        disabled={shelves.length === 0}
                        data-testid="add-rack-button"
                      >
                        <FormattedMessage
                          id="storage.add.rack"
                          defaultMessage="Add Rack"
                        />
                      </Button>
                    </div>
                    <DataTable
                      rows={formatRacksData(filteredRacks)}
                      headers={racksHeaders}
                      isSortable
                      expandableRows
                    >
                      {({
                        rows,
                        headers,
                        getTableProps,
                        getHeaderProps,
                        getRowProps,
                      }) => (
                        <TableContainer>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                <TableExpandHeader aria-label="expand row" />
                                {headers.map((header) => (
                                  <TableHeader
                                    key={
                                      header.key || header.id || header.header
                                    }
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <React.Fragment key={row.id || row.key}>
                                  <TableExpandRow
                                    data-testid={`rack-row-${row.id}`}
                                    isExpanded={row.isExpanded}
                                    onExpand={() => handleRowExpand(row.id)}
                                    ariaLabel={
                                      row.isExpanded
                                        ? intl.formatMessage({
                                            id: "carbon.table.row.collapse",
                                            defaultMessage:
                                              "Collapse current row",
                                          })
                                        : intl.formatMessage({
                                            id: "carbon.table.row.expand",
                                            defaultMessage:
                                              "Expand current row",
                                          })
                                    }
                                    {...getRowProps({
                                      row,
                                      onClick: (e) => {
                                        const target = e.target;

                                        // Don't expand if clicking on action button (overflow menu)
                                        if (
                                          target.closest(
                                            '[data-testid="location-actions-overflow-menu"]',
                                          ) ||
                                          target.closest(
                                            ".cds--overflow-menu",
                                          ) ||
                                          target.closest(
                                            'button[aria-label*="Location actions"]',
                                          )
                                        ) {
                                          e.stopPropagation();
                                          return;
                                        }

                                        // Expand on click anywhere else in the row (including expand button)
                                        handleRowExpand(row.id);
                                      },
                                    })}
                                  >
                                    {row.cells.map((cell) => (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    ))}
                                  </TableExpandRow>
                                  {expandedRowIds[String(row.id)] && (
                                    <TableExpandedRow
                                      data-testid={`expanded-rack-${row.id}`}
                                      colSpan={headers.length + 1}
                                    >
                                      {renderExpandedContentRack(row)}
                                    </TableExpandedRow>
                                  )}
                                </React.Fragment>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>
              <TabPanel>
                <Grid fullWidth className="boxes-tab">
                  <Column lg={16} md={8} sm={4} className="boxes-tab-header">
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "1rem",
                      }}
                    >
                      <div>
                        <h3 className="table-title">
                          <FormattedMessage
                            id="storage.tab.boxes"
                            defaultMessage="Boxes"
                          />
                        </h3>
                        <p className="helper-text">
                          <FormattedMessage
                            id="storage.boxes.helper"
                            defaultMessage="Manage boxes/plates, or select a rack and box to assign samples to coordinates."
                          />
                        </p>
                      </div>
                    </div>
                  </Column>

                  {/* Grid Assignment Section (existing functionality) */}
                  <Column lg={16} md={8} sm={4} className="boxes-tab-header">
                    <h4>
                      <FormattedMessage
                        id="storage.boxes.grid.assignment"
                        defaultMessage="Grid Assignment"
                      />
                    </h4>
                    <p className="helper-text">
                      <FormattedMessage
                        id="storage.boxes.helper.grid"
                        defaultMessage="Select a rack, then a box (plate) to view its grid and assign samples to coordinates."
                      />
                    </p>
                  </Column>

                  <Column lg={8} md={8} sm={4} className="boxes-controls">
                    <Dropdown
                      id="rack-selector"
                      data-testid="rack-selector"
                      titleText={intl.formatMessage({
                        id: "storage.boxes.selectRack",
                        defaultMessage: "Select rack",
                      })}
                      label={intl.formatMessage({
                        id: "storage.boxes.selectRack",
                        defaultMessage: "Select rack",
                      })}
                      items={rackDropdownItems}
                      itemToString={(item) =>
                        item ? `${item.label} (${item.description})` : ""
                      }
                      selectedItem={
                        selectedRackIdForGrid
                          ? rackDropdownItems.find(
                              (r) =>
                                r.id?.toString() ===
                                selectedRackIdForGrid?.toString(),
                            )
                          : null
                      }
                      onChange={({ selectedItem }) =>
                        handleRackSelect(selectedItem)
                      }
                    />
                  </Column>

                  <Column lg={8} md={8} sm={4} className="boxes-controls">
                    <Dropdown
                      id="box-selector"
                      data-testid="box-selector"
                      titleText={intl.formatMessage({
                        id: "storage.boxes.selectBox",
                        defaultMessage: "Select box/plate",
                      })}
                      label={intl.formatMessage({
                        id: "storage.boxes.selectBox",
                        defaultMessage: "Select box/plate",
                      })}
                      items={boxDropdownItems}
                      itemToString={(item) =>
                        item ? `${item.label} - ${item.description}` : ""
                      }
                      selectedItem={
                        selectedBoxId
                          ? boxDropdownItems.find(
                              (b) =>
                                b.id?.toString() === selectedBoxId?.toString(),
                            )
                          : null
                      }
                      onChange={({ selectedItem }) =>
                        handleBoxSelect(selectedItem)
                      }
                      disabled={!selectedRackIdForGrid || boxesLoading}
                    />
                    <BoxCrudControls
                      selectedRackId={selectedRackIdForGrid}
                      selectedBox={selectedBox}
                      onCreate={handleCreateBox}
                      onEdit={handleEditBox}
                      onDelete={handleDeleteBox}
                    />
                  </Column>

                  <Column lg={16} md={8} sm={4} className="boxes-status">
                    {selectedBox && (
                      <Tile>
                        <p className="rack-details">
                          <strong>{selectedBox.label}</strong>{" "}
                          {selectedBox.type ? `(${selectedBox.type})` : ""}
                        </p>
                        <p className="rack-details">
                          <FormattedMessage
                            id="storage.boxes.grid.dimensions"
                            defaultMessage="Grid: {rows} × {cols} = {capacity} positions"
                            values={{
                              rows: selectedBox.rows || 0,
                              cols: selectedBox.columns || 0,
                              capacity: selectedBox.capacity || 0,
                            }}
                          />
                        </p>
                      </Tile>
                    )}
                  </Column>

                  <Column lg={10} md={8} sm={4}>
                    {boxesError && (
                      <InlineNotification
                        lowContrast
                        kind="error"
                        title={intl.formatMessage({
                          id: "storage.boxes.load.error",
                          defaultMessage: "Unable to load boxes",
                        })}
                        subtitle={boxesError}
                      />
                    )}
                    {boxesLoading && (
                      <ProgressBar
                        hideLabel
                        label={intl.formatMessage({
                          id: "storage.boxes.loading",
                          defaultMessage: "Loading boxes…",
                        })}
                      />
                    )}
                    {renderBoxGrid()}
                  </Column>

                  <Column lg={6} md={8} sm={4}>
                    <Tile className="assign-box-tile">
                      <h4>
                        <FormattedMessage
                          id="storage.boxes.assign.title"
                          defaultMessage="Assign sample to box"
                        />
                      </h4>
                      <p className="helper-text">
                        {selectedBox && selectedCoordinate ? (
                          <FormattedMessage
                            id="storage.boxes.assign.selected"
                            defaultMessage="Selected: {boxLabel} position {coordinate}"
                            values={{
                              boxLabel: selectedBox.label,
                              coordinate: selectedCoordinate,
                            }}
                          />
                        ) : selectedBox ? (
                          <FormattedMessage
                            id="storage.boxes.assign.selectCoordinate"
                            defaultMessage="Select a position in the grid to assign."
                          />
                        ) : (
                          <FormattedMessage
                            id="storage.boxes.assign.noSelection"
                            defaultMessage="Select a box and position from the grid to assign."
                          />
                        )}
                      </p>
                      <TextInput
                        id="assign-sample-id"
                        data-testid="assign-sample-id"
                        labelText={intl.formatMessage({
                          id: "storage.boxes.assign.sampleLabel",
                          defaultMessage: "Sample item ID or barcode",
                        })}
                        placeholder={intl.formatMessage({
                          id: "storage.boxes.assign.samplePlaceholder",
                          defaultMessage: "Enter Sample Item ID",
                        })}
                        value={assignSampleId}
                        onChange={(e) => setAssignSampleId(e.target.value)}
                        disabled={!selectedBox || !selectedCoordinate}
                      />
                      <TextArea
                        id="assign-notes"
                        data-testid="assign-notes"
                        labelText={intl.formatMessage({
                          id: "storage.boxes.assign.notesLabel",
                          defaultMessage: "Notes (optional)",
                        })}
                        value={assignNotes}
                        onChange={(e) => setAssignNotes(e.target.value)}
                        rows={3}
                        disabled={!selectedBox}
                      />
                      {assignStatus && (
                        <InlineNotification
                          lowContrast
                          kind={assignStatus.kind}
                          title={
                            assignStatus.kind === "success"
                              ? intl.formatMessage({
                                  id: "storage.boxes.assign.success.title",
                                  defaultMessage: "Assignment saved",
                                })
                              : intl.formatMessage({
                                  id: "storage.boxes.assign.error.title",
                                  defaultMessage: "Assignment failed",
                                })
                          }
                          subtitle={assignStatus.message}
                        />
                      )}
                      <div className="assign-actions">
                        <Button
                          kind="primary"
                          disabled={
                            !selectedBox ||
                            !assignSampleId ||
                            boxesLoading ||
                            isMovingSample
                          }
                          onClick={handleAssignToBox}
                        >
                          <FormattedMessage
                            id="storage.boxes.assign.button"
                            defaultMessage="Assign"
                          />
                        </Button>
                      </div>
                    </Tile>
                  </Column>
                </Grid>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>

      {/* Location CRUD Modals */}
      {createModalOpen && (
        <StorageLocationModal
          open={createModalOpen}
          locationType={selectedLocationType}
          mode="create"
          parentRoom={
            selectedLocationType === "device" ? selectedParentLocation : null
          }
          parentDevice={
            selectedLocationType === "shelf" ? selectedParentLocation : null
          }
          parentShelf={
            selectedLocationType === "rack" ? selectedParentLocation : null
          }
          onClose={handleCreateModalClose}
          onSave={handleCreateModalSave}
        />
      )}
      {editModalOpen && (
        <EditLocationModal
          open={editModalOpen}
          location={selectedLocation}
          locationType={selectedLocationType}
          onClose={handleEditModalClose}
          onSave={handleEditModalSave}
        />
      )}
      {deleteModalOpen && (
        <DeleteLocationModal
          open={deleteModalOpen}
          location={selectedLocation}
          locationType={selectedLocationType}
          onClose={handleDeleteModalClose}
          onDelete={handleDeleteModalConfirm}
        />
      )}
      {boxModalOpen && (
        <EditBoxModal
          open={boxModalOpen}
          mode={boxModalMode}
          box={selectedBoxForCrud}
          parentRack={selectedRackForGrid}
          onClose={handleBoxModalClose}
          onSave={handleBoxSaved}
        />
      )}
      {boxDeleteModalOpen && (
        <DeleteBoxModal
          open={boxDeleteModalOpen}
          box={selectedBoxForCrud}
          onClose={handleBoxDeleteModalClose}
          onDeleted={handleBoxDeleted}
        />
      )}
      {/* Print Label Confirmation Dialog (single instance) */}
      <PrintLabelConfirmationDialog
        open={printLabelDialogOpen}
        locationName={
          printLabelLocationData?.name || printLabelLocationData?.label || ""
        }
        locationCode={
          printLabelLocationData?.code || printLabelLocationData?.label || ""
        }
        onConfirm={handlePrintLabelConfirm}
        onCancel={handlePrintLabelCancel}
      />

      {/* Legacy Print Label Button (auto-triggers dialog when printLabelLocation is set) */}
      {/* TODO: Remove this after migrating all usage to handlePrintLabel callback */}
      {printLabelLocation && (
        <PrintLabelButton
          locationType={printLabelLocation.type}
          locationId={String(printLabelLocation.id)}
          locationName={printLabelLocation.name || printLabelLocation.label}
          locationCode={printLabelLocation.code || printLabelLocation.label}
          onPrintSuccess={handlePrintLabelSuccess}
          onPrintError={handlePrintLabelError}
          autoTrigger={true}
        />
      )}

      {/* Sample Modals (single instances) - always render, control via open prop */}
      <LocationManagementModal
        open={locationModalOpen && !!selectedSample}
        sample={selectedSample}
        currentLocation={(() => {
          const currentLoc = selectedSample?.location
            ? {
                path: selectedSample.location,
                position: selectedSample.positionCoordinate
                  ? { coordinate: selectedSample.positionCoordinate }
                  : null,
                notes: selectedSample.notes || "",
              }
            : null;
          return currentLoc;
        })()}
        onClose={handleLocationModalClose}
        onConfirm={handleLocationModalConfirm}
        onAssignmentSuccess={refreshMetrics}
      />
      <DisposeSampleModal
        open={disposeModalOpen && !!selectedSampleForDispose}
        sample={selectedSampleForDispose}
        currentLocation={
          selectedSampleForDispose?.location
            ? { path: selectedSampleForDispose.location, position: null }
            : null
        }
        onClose={handleDisposeModalClose}
        onConfirm={handleDisposeModalConfirm}
        onDisposalSuccess={refreshMetrics}
      />
    </div>
  );
};

export default StorageDashboard;
