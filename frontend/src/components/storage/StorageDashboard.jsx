import React, { useState, useEffect, useRef, useContext } from "react";
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
  Tag,
  ProgressBar,
  Search,
  Dropdown,
  Button,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import StorageLocationsMetricCard from "./StorageDashboard/StorageLocationsMetricCard";
import LocationFilterDropdown from "./StorageDashboard/LocationFilterDropdown";
import SampleActionsContainer from "./SampleStorage/SampleActionsContainer";
import "./StorageDashboard.css";

const TAB_ROUTES = ["samples", "rooms", "devices", "shelves", "racks"];

const StorageDashboard = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const componentMounted = useRef(true);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Metric cards state
  const [metrics, setMetrics] = useState({
    totalSamples: 0,
    active: 0,
    disposed: 0,
    storageLocations: 0,
  });

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

  // Filter state
  const [searchTerm, setSearchTerm] = useState("");
  const [locationFilter, setLocationFilter] = useState(null); // { id, type, name } for single location dropdown (Samples tab)
  const [filterRoom, setFilterRoom] = useState(""); // For other tabs (devices, shelves, racks)
  const [filterDevice, setFilterDevice] = useState(""); // For other tabs
  const [filterStatus, setFilterStatus] = useState("");

  const [loading, setLoading] = useState(true);

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
    loadMetrics();
    loadRooms();
    loadDevices();
    loadShelves();
    loadRacks();
    loadSamples();

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Reload data when filters change (server-side filtering for all tabs)
  // Note: When searchTerm is present, filters are applied client-side on search results (AND logic)
  useEffect(() => {
    const tabName = TAB_ROUTES[selectedTab] || "samples";

    // Skip filter reload if searchTerm is present (filters applied client-side on search results)
    // Exception: For samples tab, debounced search effect handles reload
    if (searchTerm && searchTerm.trim() && tabName === "samples") {
      // For samples tab with search, debounced search effect will handle reload with filters
      return;
    }

    console.log(
      "Filters changed, reloading data for tab:",
      tabName,
      "with filters:",
      { filterRoom, filterDevice, filterStatus },
    );

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
  }, [locationFilter, filterRoom, filterDevice, filterStatus, selectedTab]);

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

  const loadMetrics = () => {
    getFromOpenElisServer(
      "/rest/storage/samples?countOnly=true",
      (response) => {
        if (componentMounted.current && response) {
          // Response is an array with one metrics object
          const metricsData = Array.isArray(response) ? response[0] : response;
          setMetrics({
            totalSamples: metricsData?.totalSamples || 0,
            active: metricsData?.active || 0,
            disposed: metricsData?.disposed || 0,
            storageLocations: metricsData?.storageLocations || 0,
          });
        }
      },
    );
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
      // Call search endpoint (FR-064: Samples tab - search by ID, accession prefix, location path)
      const url = `/rest/storage/samples/search?q=${encodeURIComponent(searchTerm.trim())}`;
      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Apply filters client-side on search results (AND logic)
            let filtered = response || [];

            // Apply location filter from LocationFilterDropdown (Samples tab uses single location filter)
            if (locationFilter && locationFilter.id) {
              const locationName =
                locationFilter.name || locationFilter.label || "";
              filtered = filtered.filter((sample) => {
                const sampleLocation = sample.location || "";
                return sampleLocation
                  .toLowerCase()
                  .includes(locationName.toLowerCase());
              });
            }

            // Apply status filter
            if (filterStatus && visibleFilters.status) {
              const statusFilter =
                filterStatus === "true"
                  ? "active"
                  : filterStatus === "false"
                    ? "disposed"
                    : null;
              if (statusFilter) {
                filtered = filtered.filter((sample) => {
                  const sampleStatus = sample.status || "active";
                  return (
                    sampleStatus.toLowerCase() === statusFilter.toLowerCase()
                  );
                });
              }
            }

            setSamples(filtered);
          } else {
            console.error(
              "Samples search API returned non-array response:",
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

      // Convert status filter: "true" -> "active", "false" -> "disposed", "" -> no filter
      if (filterStatus && visibleFilters.status) {
        if (filterStatus === "true") {
          params.append("status", "active");
        } else if (filterStatus === "false") {
          params.append("status", "disposed");
        }
        // If filterStatus is empty string, don't add status param (show all)
      }

      const queryString = params.toString();
      const url = `/rest/storage/samples${queryString ? "?" + queryString : ""}`;

      console.log("Loading samples from", url, "with filters:", {
        locationFilter,
        filterStatus,
      });
      getFromOpenElisServer(url, (response) => {
        if (componentMounted.current) {
          console.log(
            "Samples API response received:",
            response,
            "Type:",
            typeof response,
          );
          if (response && Array.isArray(response)) {
            console.log("Samples loaded:", response.length, response);
            setSamples(response);
            if (response.length === 0) {
              console.warn(
                "Samples API returned empty array - no sample assignments found matching filters",
              );
            }
          } else {
            console.error("Samples API returned non-array response:", response);
            console.error("Expected array but got:", typeof response, response);
            console.error("Response is:", JSON.stringify(response));
            setSamples([]);
          }
        }
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

  // Samples table headers
  const samplesHeaders = [
    { key: "sampleId", header: intl.formatMessage({ id: "sample.id" }) },
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
        <Button kind="ghost" size="sm">
          ⋮
        </Button>
      ),
    }));
  };

  // Format devices data for table
  const formatDevicesData = (devicesData) => {
    if (!devicesData || devicesData.length === 0) {
      return [];
    }
    return devicesData.map((device) => {
      const occupied = device.occupiedCount || 0;
      const total = device.capacityLimit || device.totalCapacity || 0;
      const occupancyPct = calculateOccupancy(occupied, total);
      const occupancyColor = getOccupancyColor(occupancyPct);

      return {
        id: String(device.id || ""),
        name: device.name || "",
        code: device.code || "",
        room: device.roomName || device.parentRoomName || "",
        type: (
          <Tag
            type={
              device.type === "freezer"
                ? "blue"
                : device.type === "refrigerator"
                  ? "cyan"
                  : "gray"
            }
          >
            {device.type || ""}
          </Tag>
        ),
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
          <Button kind="ghost" size="sm">
            ⋮
          </Button>
        ),
      };
    });
  };

  // Format shelves data for table
  const formatShelvesData = (shelvesData) => {
    if (!shelvesData || shelvesData.length === 0) {
      return [];
    }
    return shelvesData.map((shelf) => {
      const occupied = shelf.occupiedCount || 0;
      const total = shelf.capacityLimit || shelf.totalCapacity || 0;
      const occupancyPct = calculateOccupancy(occupied, total);
      const occupancyColor = getOccupancyColor(occupancyPct);

      return {
        id: String(shelf.id || ""),
        label: shelf.label || "",
        device: shelf.deviceName || shelf.parentDeviceName || "",
        room: shelf.roomName || "",
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
          <Button kind="ghost" size="sm">
            ⋮
          </Button>
        ),
      };
    });
  };

  // Format racks data for table
  const formatRacksData = (racksData) => {
    if (!racksData || racksData.length === 0) {
      return [];
    }
    return racksData.map((rack) => {
      const occupied = rack.occupiedCount || 0;
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
          <Button kind="ghost" size="sm">
            ⋮
          </Button>
        ),
      };
    });
  };

  // Format samples data for table
  const formatSamplesData = (samplesData) => {
    if (!samplesData || samplesData.length === 0) {
      return [];
    }
    return samplesData.map((sample) => ({
      id: String(sample.sampleId || sample.id || ""),
      sampleId: String(sample.sampleId || sample.id || ""),
      type: sample.type || sample.sampleType || "",
      status:
        sample.status === "disposed" || sample.status === "Disposed" ? (
          <Tag type="red">
            <FormattedMessage id="storage.status.disposed" />
          </Tag>
        ) : (
          <Tag type="green">
            <FormattedMessage id="label.active" />
          </Tag>
        ),
      location: sample.location || sample.hierarchicalPath || "",
      assignedBy: sample.assignedBy || sample.assignedByUserId || "",
      date: sample.date || sample.assignedDate || "",
      actions: (
        <SampleActionsContainer
          sample={{
            id: String(sample.sampleId || sample.id || ""),
            sampleId: String(sample.sampleId || sample.id || ""),
            type: sample.type || sample.sampleType || "",
            status: sample.status || "Active",
            location: sample.location || sample.hierarchicalPath || "",
          }}
          onMoveConfirm={(sample, newLocation, reason) => {
            console.log("Move sample confirmed", {
              sample,
              newLocation,
              reason,
            });
            // TODO: Implement API call to move sample
          }}
          onDisposeConfirm={(sample, reason, method, notes) => {
            console.log("Dispose sample confirmed", {
              sample,
              reason,
              method,
              notes,
            });
            // TODO: Implement API call to dispose sample
          }}
          onViewStorageSave={(sample, location) => {
            console.log("Save storage location", { sample, location });
            // TODO: Implement API call to save/update location
          }}
          onNotification={addNotification}
        />
      ),
    }));
  };

  const filteredRooms = filterData(rooms, "rooms");
  const filteredDevices = filterData(devices, "devices");
  const filteredShelves = filterData(shelves, "shelves");
  const filteredRacks = filterData(racks, "racks");
  const filteredSamples = filterData(samples, "samples");

  return (
    <div className="storage-dashboard">
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
          <Tile>
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
              <Tab>
                <FormattedMessage id="storage.tab.samples" />
              </Tab>
              <Tab className="tab-rooms">
                <FormattedMessage id="storage.tab.rooms" />
              </Tab>
              <Tab className="tab-devices">
                <FormattedMessage id="storage.tab.devices" />
              </Tab>
              <Tab className="tab-shelves">
                <FormattedMessage id="storage.tab.shelves" />
              </Tab>
              <Tab className="tab-racks">
                <FormattedMessage id="storage.tab.racks" />
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
                    <h3 className="table-title">
                      <FormattedMessage id="storage.tab.rooms" />
                    </h3>
                    <DataTable
                      rows={formatRoomsData(filteredRooms)}
                      headers={roomsHeaders}
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
                                  {...getRowProps({ row })}
                                >
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
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
                    <h3 className="table-title">
                      <FormattedMessage id="storage.tab.devices" />
                    </h3>
                    <DataTable
                      rows={formatDevicesData(filteredDevices)}
                      headers={devicesHeaders}
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
                                  {...getRowProps({ row })}
                                >
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
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
                    <h3 className="table-title">
                      <FormattedMessage id="storage.tab.shelves" />
                    </h3>
                    <DataTable
                      rows={formatShelvesData(filteredShelves)}
                      headers={shelvesHeaders}
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
                                  {...getRowProps({ row })}
                                >
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
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
                    <h3 className="table-title">
                      <FormattedMessage id="storage.tab.racks" />
                    </h3>
                    <DataTable
                      rows={formatRacksData(filteredRacks)}
                      headers={racksHeaders}
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
                                  {...getRowProps({ row })}
                                >
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  </Column>
                </Grid>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
};

export default StorageDashboard;
