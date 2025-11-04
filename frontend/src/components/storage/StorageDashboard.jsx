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
  const [filterRoom, setFilterRoom] = useState("");
  const [filterDevice, setFilterDevice] = useState("");
  const [filterStatus, setFilterStatus] = useState("");

  const [loading, setLoading] = useState(true);

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
    getFromOpenElisServer("/rest/storage/rooms", (response) => {
      if (componentMounted.current && response) {
        setRooms(response || []);
        setLoading(false);
      }
    });
  };

  const loadDevices = () => {
    getFromOpenElisServer("/rest/storage/devices", (response) => {
      if (componentMounted.current && response) {
        setDevices(response || []);
      }
    });
  };

  const loadShelves = () => {
    getFromOpenElisServer("/rest/storage/shelves", (response) => {
      if (componentMounted.current && response) {
        setShelves(response || []);
      }
    });
  };

  const loadRacks = () => {
    getFromOpenElisServer("/rest/storage/racks", (response) => {
      if (componentMounted.current && response) {
        setRacks(response || []);
      }
    });
  };

  const loadSamples = () => {
    getFromOpenElisServer("/rest/storage/samples", (response) => {
      if (componentMounted.current && response) {
        setSamples(response || []);
      }
    });
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

    if (searchTerm) {
      filtered = filtered.filter((item) => {
        const searchableText = JSON.stringify(item).toLowerCase();
        return searchableText.includes(searchTerm.toLowerCase());
      });
    }

    if (filterRoom && type !== "rooms") {
      filtered = filtered.filter((item) => {
        return (
          item.roomName?.toLowerCase().includes(filterRoom.toLowerCase()) ||
          item.roomCode?.toLowerCase().includes(filterRoom.toLowerCase())
        );
      });
    }

    if (filterDevice && type !== "devices" && type !== "rooms") {
      filtered = filtered.filter((item) => {
        return (
          item.deviceName?.toLowerCase().includes(filterDevice.toLowerCase()) ||
          item.deviceCode?.toLowerCase().includes(filterDevice.toLowerCase())
        );
      });
    }

    if (filterStatus) {
      filtered = filtered.filter((item) => {
        return (
          item.active?.toString() === filterStatus ||
          item.status === filterStatus
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
      id: room.id,
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
        id: device.id,
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
              size="sm"
              status={occupancyColor}
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
        id: shelf.id,
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
              size="sm"
              status={occupancyColor}
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
        id: rack.id,
        label: rack.label || "",
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
              size="sm"
              status={occupancyColor}
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
      id: sample.sampleId || sample.id || "",
      sampleId: sample.sampleId || sample.id || "",
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
        <Button kind="ghost" size="sm">
          ⋮
        </Button>
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
            <h3>
              <FormattedMessage id="storage.metrics.storage.locations" />
            </h3>
            <p className="metric-value">{metrics.storageLocations}</p>
          </Tile>
        </Column>

        {/* Search and Filters */}
        <Column lg={16} md={8} sm={4}>
          <Grid>
            <Column lg={8} md={4} sm={4}>
              <Search
                labelText={intl.formatMessage({
                  id: "storage.search.placeholder",
                })}
                placeholder={intl.formatMessage({
                  id: "storage.search.placeholder",
                })}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                size="lg"
              />
            </Column>
            <Column lg={4} md={2} sm={2}>
              <Dropdown
                id="filter-room"
                label={intl.formatMessage({ id: "storage.filter.room" })}
                titleText={intl.formatMessage({ id: "storage.filter.room" })}
                items={[
                  { id: "", label: intl.formatMessage({ id: "label.all" }) },
                  ...rooms.map((r) => ({ id: r.id, label: r.name })),
                ]}
                selectedItem={
                  filterRoom
                    ? {
                        id: filterRoom,
                        label:
                          rooms.find((r) => r.id === filterRoom)?.name ||
                          intl.formatMessage({ id: "storage.filter.room" }),
                      }
                    : {
                        id: "",
                        label: intl.formatMessage({
                          id: "storage.filter.room",
                        }),
                      }
                }
                onChange={(e) => setFilterRoom(e.selectedItem?.id || "")}
              />
            </Column>
            <Column lg={4} md={2} sm={2}>
              <Dropdown
                id="filter-status"
                label={intl.formatMessage({ id: "storage.filter.status" })}
                titleText={intl.formatMessage({ id: "storage.filter.status" })}
                items={[
                  { id: "", label: intl.formatMessage({ id: "label.all" }) },
                  {
                    id: "true",
                    label: intl.formatMessage({ id: "label.active" }),
                  },
                  {
                    id: "false",
                    label: intl.formatMessage({ id: "label.inactive" }),
                  },
                ]}
                selectedItem={
                  filterStatus
                    ? {
                        id: filterStatus,
                        label:
                          filterStatus === "true"
                            ? intl.formatMessage({ id: "label.active" })
                            : intl.formatMessage({ id: "label.inactive" }),
                      }
                    : {
                        id: "",
                        label: intl.formatMessage({
                          id: "storage.filter.status",
                        }),
                      }
                }
                onChange={(e) => setFilterStatus(e.selectedItem?.id || "")}
              />
            </Column>
          </Grid>
        </Column>

        {/* Tabs */}
        <Column lg={16} md={8} sm={4}>
          <Tabs selectedIndex={selectedTab} onChange={handleTabChange}>
            <TabList aria-label="Storage dashboard tabs" contained>
              <Tab>
                <FormattedMessage id="storage.tab.samples" />
              </Tab>
              <Tab>
                <FormattedMessage id="storage.tab.rooms" />
              </Tab>
              <Tab>
                <FormattedMessage id="storage.tab.devices" />
              </Tab>
              <Tab>
                <FormattedMessage id="storage.tab.shelves" />
              </Tab>
              <Tab>
                <FormattedMessage id="storage.tab.racks" />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
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
                              <TableHeader key={header.key || header.id || header.header} {...getHeaderProps({ header })}>
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {rows.map((row) => (
                            <TableRow key={row.id || row.key} {...getRowProps({ row })}>
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
              </TabPanel>
              <TabPanel>
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
                              <TableHeader key={header.key || header.id || header.header} {...getHeaderProps({ header })}>
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {rows.map((row) => (
                            <TableRow key={row.id || row.key} {...getRowProps({ row })}>
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
              </TabPanel>
              <TabPanel>
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
                              <TableHeader key={header.key || header.id || header.header} {...getHeaderProps({ header })}>
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {rows.map((row) => (
                            <TableRow key={row.id || row.key} {...getRowProps({ row })}>
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
              </TabPanel>
              <TabPanel>
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
                              <TableHeader key={header.key || header.id || header.header} {...getHeaderProps({ header })}>
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {rows.map((row) => (
                            <TableRow key={row.id || row.key} {...getRowProps({ row })}>
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
              </TabPanel>
              <TabPanel>
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
                              <TableHeader key={header.key || header.id || header.header} {...getHeaderProps({ header })}>
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {rows.map((row) => (
                            <TableRow key={row.id || row.key} {...getRowProps({ row })}>
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
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
};

export default StorageDashboard;
