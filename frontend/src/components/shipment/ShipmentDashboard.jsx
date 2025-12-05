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
  Search,
  Dropdown,
  Button,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import "./ShipmentDashboard.css";

const TAB_ROUTES = ["boxes", "unassigned"];

const ShipmentDashboard = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const componentMounted = useRef(true);
  const { addNotification } = useContext(NotificationContext);

  // Tab state - derive from URL
  const getTabFromUrl = () => {
    const pathParts = location.pathname.split("/");
    const tabName = pathParts[pathParts.length - 1];
    const tabIndex = TAB_ROUTES.indexOf(tabName);
    return tabIndex >= 0 ? tabIndex : 0; // Default to boxes (index 0)
  };

  const [selectedTab, setSelectedTab] = useState(getTabFromUrl());

  // Data state for each tab
  const [boxes, setBoxes] = useState([]);
  const [unassignedSamples, setUnassignedSamples] = useState([]);

  // Filter state
  const [searchTerm, setSearchTerm] = useState("");
  const [filterState, setFilterState] = useState("");
  const [filterFacility, setFilterFacility] = useState("");

  // Facilities dropdown data
  const [facilities, setFacilities] = useState([]);

  const [loading, setLoading] = useState(true);

  // Box states for filtering
  const boxStates = [
    { id: "", text: intl.formatMessage({ id: "label.all" }) },
    { id: "DRAFT", text: "Draft" },
    { id: "READY_TO_SEND", text: "Ready to Send" },
    { id: "SENT", text: "Sent" },
    { id: "RECEIVED", text: "Received" },
    { id: "RECONCILED", text: "Reconciled" },
  ];

  // Update URL when tab changes
  useEffect(() => {
    const newPath = `/SampleShipment/${TAB_ROUTES[selectedTab]}`;
    if (location.pathname !== newPath) {
      history.push(newPath);
    }
  }, [selectedTab, history, location.pathname]);

  // Update tab when URL changes (e.g., browser back button)
  useEffect(() => {
    const newTabIndex = getTabFromUrl();
    if (newTabIndex !== selectedTab) {
      setSelectedTab(newTabIndex);
    }
  }, [location.pathname]);

  // Fetch facilities on mount
  useEffect(() => {
    if (componentMounted.current) {
      fetchFacilities();
    }
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Fetch data when tab changes or filters change
  useEffect(() => {
    if (selectedTab === 0) {
      fetchBoxes();
    } else if (selectedTab === 1) {
      fetchUnassignedSamples();
    }
  }, [selectedTab, filterState, filterFacility]);

  const fetchFacilities = async () => {
    try {
      const response = await getFromOpenElisServer("/rest/organization");
      if (componentMounted.current && response) {
        const facilityOptions = [
          { id: "", text: intl.formatMessage({ id: "label.all" }) },
          ...response.map((org) => ({
            id: org.id,
            text: org.organizationName,
          })),
        ];
        setFacilities(facilityOptions);
      }
    } catch (error) {
      console.error("Error fetching facilities:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "error.fetch.facilities" }),
      });
    }
  };

  const fetchBoxes = async () => {
    setLoading(true);
    try {
      let url = "/rest/shipping-box";
      if (filterState) {
        url = `/rest/shipping-box/by-state/${filterState}`;
      } else if (filterFacility) {
        url = `/rest/shipping-box/by-facility/${filterFacility}`;
      }

      const response = await getFromOpenElisServer(url);
      if (componentMounted.current && response) {
        setBoxes(response);
      }
    } catch (error) {
      console.error("Error fetching boxes:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "error.fetch.boxes" }),
      });
    } finally {
      if (componentMounted.current) {
        setLoading(false);
      }
    }
  };

  const fetchUnassignedSamples = async () => {
    setLoading(true);
    try {
      let url = "/rest/unassigned-sample";
      if (filterFacility) {
        url = `/rest/unassigned-sample/by-facility/${filterFacility}`;
      }

      const response = await getFromOpenElisServer(url);
      if (componentMounted.current && response) {
        setUnassignedSamples(response);
      }
    } catch (error) {
      console.error("Error fetching unassigned samples:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "error.fetch.unassignedSamples" }),
      });
    } finally {
      if (componentMounted.current) {
        setLoading(false);
      }
    }
  };

  // Filter data based on search term
  const getFilteredBoxes = () => {
    if (!searchTerm) return boxes;
    const lowerSearch = searchTerm.toLowerCase();
    return boxes.filter(
      (box) =>
        box.boxId?.toLowerCase().includes(lowerSearch) ||
        box.destinationFacilityName?.toLowerCase().includes(lowerSearch),
    );
  };

  const getFilteredUnassignedSamples = () => {
    if (!searchTerm) return unassignedSamples;
    const lowerSearch = searchTerm.toLowerCase();
    return unassignedSamples.filter(
      (sample) =>
        sample.accessionNumber?.toLowerCase().includes(lowerSearch) ||
        sample.destinationFacilityName?.toLowerCase().includes(lowerSearch),
    );
  };

  // Handle create new box
  const handleCreateBox = () => {
    history.push("/SampleShipment/box/create");
  };

  // Handle view box details
  const handleViewBox = (boxId) => {
    history.push(`/SampleShipment/box/${boxId}`);
  };

  // Render box state tag
  const renderStateTag = (state) => {
    const stateConfig = {
      DRAFT: { type: "gray", label: "Draft" },
      READY_TO_SEND: { type: "blue", label: "Ready to Send" },
      SENT: { type: "purple", label: "Sent" },
      RECEIVED: { type: "green", label: "Received" },
      RECONCILED: { type: "teal", label: "Reconciled" },
    };

    const config = stateConfig[state] || { type: "gray", label: state };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  // Boxes table headers
  const boxHeaders = [
    {
      key: "boxId",
      header: intl.formatMessage({ id: "shipment.label.boxId" }),
    },
    {
      key: "destinationFacilityName",
      header: intl.formatMessage({ id: "shipment.label.destination" }),
    },
    {
      key: "state",
      header: intl.formatMessage({ id: "shipment.label.state" }),
    },
    {
      key: "sampleCount",
      header: intl.formatMessage({ id: "shipment.label.sampleCount" }),
    },
    {
      key: "createdDate",
      header: intl.formatMessage({ id: "shipment.label.createdDate" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  // Unassigned samples table headers
  const unassignedHeaders = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.accessionNumber" }),
    },
    {
      key: "referralTestName",
      header: intl.formatMessage({ id: "shipment.label.referralTest" }),
    },
    {
      key: "destinationFacilityName",
      header: intl.formatMessage({ id: "shipment.label.destination" }),
    },
    {
      key: "priority",
      header: intl.formatMessage({ id: "shipment.label.priority" }),
    },
    {
      key: "createdDate",
      header: intl.formatMessage({ id: "shipment.label.createdDate" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  // Render boxes table rows
  const renderBoxRows = () => {
    const filteredBoxes = getFilteredBoxes();
    return filteredBoxes.map((box) => ({
      id: box.id.toString(),
      boxId: box.boxId,
      destinationFacilityName: box.destinationFacilityName || "-",
      state: renderStateTag(box.state),
      sampleCount: box.sampleCount || 0,
      createdDate: box.createdDate
        ? new Date(box.createdDate).toLocaleDateString()
        : "-",
      actions: (
        <Button
          kind="ghost"
          size="sm"
          onClick={() => handleViewBox(box.id)}
          style={{ paddingLeft: 0 }}
        >
          <FormattedMessage id="label.view" />
        </Button>
      ),
    }));
  };

  // Render unassigned samples table rows
  const renderUnassignedRows = () => {
    const filteredSamples = getFilteredUnassignedSamples();
    return filteredSamples.map((sample) => ({
      id: sample.id.toString(),
      accessionNumber: sample.accessionNumber || "-",
      referralTestName: sample.referralTestName || "-",
      destinationFacilityName: sample.destinationFacilityName || "-",
      priority: sample.priority || "-",
      createdDate: sample.createdDate
        ? new Date(sample.createdDate).toLocaleDateString()
        : "-",
      actions: (
        <Button
          kind="ghost"
          size="sm"
          onClick={() => console.log("Add to box:", sample.id)}
          style={{ paddingLeft: 0 }}
        >
          <FormattedMessage id="shipment.action.addToBox" />
        </Button>
      ),
    }));
  };

  return (
    <div className="shipment-dashboard">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Tile className="dashboard-header">
            <h2>
              <FormattedMessage id="shipment.dashboard.title" />
            </h2>
            <p>
              <FormattedMessage id="shipment.dashboard.description" />
            </p>
          </Tile>
        </Column>
      </Grid>

      <Grid fullWidth className="dashboard-content">
        <Column lg={16} md={8} sm={4}>
          <Tabs
            selectedIndex={selectedTab}
            onChange={({ selectedIndex }) => setSelectedTab(selectedIndex)}
          >
            <TabList aria-label="Shipment tabs">
              <Tab>
                <FormattedMessage id="shipment.tab.boxes" />
              </Tab>
              <Tab>
                <FormattedMessage id="shipment.tab.unassignedSamples" />
              </Tab>
            </TabList>

            <TabPanels>
              {/* Boxes Tab */}
              <TabPanel>
                <div className="tab-toolbar">
                  <Search
                    size="lg"
                    placeholder={intl.formatMessage({
                      id: "search.placeholder",
                    })}
                    labelText={intl.formatMessage({ id: "search.label" })}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    value={searchTerm}
                  />
                  <Dropdown
                    id="state-filter"
                    titleText={intl.formatMessage({
                      id: "shipment.filter.state",
                    })}
                    label={intl.formatMessage({ id: "label.select" })}
                    items={boxStates}
                    selectedItem={boxStates.find((s) => s.id === filterState)}
                    onChange={({ selectedItem }) =>
                      setFilterState(selectedItem?.id || "")
                    }
                  />
                  <Dropdown
                    id="facility-filter"
                    titleText={intl.formatMessage({
                      id: "shipment.filter.facility",
                    })}
                    label={intl.formatMessage({ id: "label.select" })}
                    items={facilities}
                    selectedItem={facilities.find(
                      (f) => f.id === filterFacility,
                    )}
                    onChange={({ selectedItem }) =>
                      setFilterFacility(selectedItem?.id || "")
                    }
                  />
                  <Button onClick={handleCreateBox}>
                    <FormattedMessage id="shipment.action.createBox" />
                  </Button>
                </div>

                {loading ? (
                  <Loading />
                ) : (
                  <DataTable rows={renderBoxRows()} headers={boxHeaders}>
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
                                  {...getHeaderProps({ header })}
                                  key={header.key}
                                >
                                  {header.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {rows.map((row) => (
                              <TableRow {...getRowProps({ row })} key={row.id}>
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
                )}
              </TabPanel>

              {/* Unassigned Samples Tab */}
              <TabPanel>
                <div className="tab-toolbar">
                  <Search
                    size="lg"
                    placeholder={intl.formatMessage({
                      id: "search.placeholder",
                    })}
                    labelText={intl.formatMessage({ id: "search.label" })}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    value={searchTerm}
                  />
                  <Dropdown
                    id="facility-filter-unassigned"
                    titleText={intl.formatMessage({
                      id: "shipment.filter.facility",
                    })}
                    label={intl.formatMessage({ id: "label.select" })}
                    items={facilities}
                    selectedItem={facilities.find(
                      (f) => f.id === filterFacility,
                    )}
                    onChange={({ selectedItem }) =>
                      setFilterFacility(selectedItem?.id || "")
                    }
                  />
                </div>

                {loading ? (
                  <Loading />
                ) : (
                  <DataTable
                    rows={renderUnassignedRows()}
                    headers={unassignedHeaders}
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
                                  {...getHeaderProps({ header })}
                                  key={header.key}
                                >
                                  {header.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {rows.map((row) => (
                              <TableRow {...getRowProps({ row })} key={row.id}>
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
                )}
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
};

export default ShipmentDashboard;
