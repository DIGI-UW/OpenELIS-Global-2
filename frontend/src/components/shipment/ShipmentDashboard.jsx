import {
  Button,
  Column,
  DataTable,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Grid,
  Loading,
  Search,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Tag,
  Tile,
} from "@carbon/react";
import { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useNavigate, useLocation } from "react-router-dom";
import { AlertDialog } from "../common/CustomNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { getFromOpenElisServer } from "../utils/Utils";
import AddToBoxModal from "./AddToBoxModal";
import CancelReferralModal from "./CancelReferralModal";
import MarkAsLostModal from "./MarkAsLostModal";
import "./ShipmentDashboard.css";
import ShipmentNavigation from "./ShipmentNavigation";

const TAB_ROUTES = ["boxes", "unassigned"];

const ShipmentDashboard = () => {
  const intl = useIntl();
  const navigate = useNavigate();
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

  // Statistics state
  const [statistics, setStatistics] = useState({
    inTransit: 0,
    delivered: 0,
    reconciled: 0,
    totalSamples: 0,
  });

  // Filter state
  const [searchTerm, setSearchTerm] = useState("");
  const [filterState, setFilterState] = useState("");
  const [filterFacility, setFilterFacility] = useState("");
  const [filterDateFrom, setFilterDateFrom] = useState(null);
  const [filterDateTo, setFilterDateTo] = useState(null);

  // Facilities dropdown data
  const [facilities, setFacilities] = useState([]);

  const [loading, setLoading] = useState(true);

  // Add to Box modal state
  const [showAddToBoxModal, setShowAddToBoxModal] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);

  // Mark as Lost modal state
  const [showMarkAsLostModal, setShowMarkAsLostModal] = useState(false);
  const [sampleToMarkLost, setSampleToMarkLost] = useState(null);

  // Cancel Referral modal state
  const [showCancelReferralModal, setShowCancelReferralModal] = useState(false);
  const [sampleToCancel, setSampleToCancel] = useState(null);

  // Box states for filtering
  const boxStates = [
    { id: "", text: intl.formatMessage({ id: "label.all" }) },
    { id: "DRAFT", text: intl.formatMessage({ id: "shipment.state.draft" }) },
    {
      id: "READY_TO_SEND",
      text: intl.formatMessage({ id: "shipment.state.readyToSend" }),
    },
    { id: "SENT", text: intl.formatMessage({ id: "shipment.state.sent" }) },
    {
      id: "IN_TRANSIT",
      text: intl.formatMessage({ id: "shipment.state.inTransit" }),
    },
    {
      id: "PARTIALLY_RECEIVED",
      text: intl.formatMessage({ id: "shipment.state.partiallyReceived" }),
    },
    {
      id: "RECEIVED",
      text: intl.formatMessage({ id: "shipment.state.received" }),
    },
    {
      id: "RECONCILED",
      text: intl.formatMessage({ id: "shipment.state.reconciled" }),
    },
    {
      id: "CANCELLED",
      text: intl.formatMessage({ id: "shipment.state.cancelled" }),
    },
    {
      id: "LOST_IN_TRANSIT",
      text: intl.formatMessage({ id: "shipment.state.lostInTransit" }),
    },
  ];

  // Update URL when tab changes
  useEffect(() => {
    const newPath = `/SampleShipment/${TAB_ROUTES[selectedTab]}`;
    if (location.pathname !== newPath) {
      navigate(newPath);
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
      fetchStatistics();
      // Load unassigned count for badge regardless of active tab (silent = no loading spinner)
      fetchUnassignedSamples(true);
    }
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Fetch data when tab changes or filters change
  useEffect(() => {
    setSearchTerm(""); // Reset search when switching tabs
    if (selectedTab === 0) {
      fetchBoxes();
    } else if (selectedTab === 1) {
      fetchUnassignedSamples();
    }
  }, [selectedTab, filterState, filterFacility]);

  // Refetch statistics when boxes change
  useEffect(() => {
    if (selectedTab === 0 && !loading) {
      fetchStatistics();
    }
  }, [boxes]);

  const fetchFacilities = () => {
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      (response) => {
        if (componentMounted.current && response) {
          const facilityOptions = [
            { id: "", text: intl.formatMessage({ id: "label.all" }) },
            ...response.map((org) => ({ id: org.id, text: org.value })),
          ];
          setFacilities(facilityOptions);
        }
      },
    );
  };

  const fetchStatistics = () => {
    getFromOpenElisServer("/rest/shipping-box/statistics", (response) => {
      if (componentMounted.current && response) {
        setStatistics(response);
      }
    });
  };

  const fetchBoxes = () => {
    setLoading(true);
    let url = "/rest/shipping-box";
    if (filterState) {
      url = `/rest/shipping-box/by-state/${filterState}`;
    } else if (filterFacility) {
      url = `/rest/shipping-box/by-facility/${filterFacility}`;
    }

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response) {
          setBoxes(response);
        }
        setLoading(false);
      }
    });
  };

  const fetchUnassignedSamples = (silent = false) => {
    if (!silent) setLoading(true);
    // Use new SampleItem-based endpoint
    let url = "/rest/unassigned-sample/items";
    if (filterFacility) {
      url = `/rest/unassigned-sample/items/by-facility/${filterFacility}`;
    }

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response) {
          setUnassignedSamples(response);
        }
        if (!silent) setLoading(false);
      }
    });
  };

  // Filter data based on search term and date range
  const getFilteredBoxes = () => {
    let filtered = boxes;

    if (searchTerm) {
      const lowerSearch = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (box) =>
          box.boxId?.toLowerCase().includes(lowerSearch) ||
          box.destinationFacilityName?.toLowerCase().includes(lowerSearch),
      );
    }

    if (filterDateFrom) {
      const from = new Date(filterDateFrom);
      from.setHours(0, 0, 0, 0);
      filtered = filtered.filter((box) => {
        if (!box.createdDate) return false;
        return new Date(box.createdDate) >= from;
      });
    }

    if (filterDateTo) {
      const to = new Date(filterDateTo);
      to.setHours(23, 59, 59, 999);
      filtered = filtered.filter((box) => {
        if (!box.createdDate) return false;
        return new Date(box.createdDate) <= to;
      });
    }

    return filtered;
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

  // Handle scan/search box ID — navigate directly if exact match found
  const handleScanBoxId = () => {
    if (!searchTerm.trim()) return;
    const exactMatch = boxes.find(
      (box) => box.boxId?.toLowerCase() === searchTerm.trim().toLowerCase(),
    );
    if (exactMatch) {
      navigate(`/SampleShipment/box/${exactMatch.id}`);
    } else {
      addNotification({
        kind: "info",
        title: intl.formatMessage({ id: "notification.info" }),
        message: intl.formatMessage(
          { id: "shipment.dashboard.boxNotFound" },
          { boxId: searchTerm.trim() },
        ),
      });
    }
  };

  // Handle create new box
  const handleCreateBox = () => {
    navigate("/SampleShipment/create-box");
  };

  // Handle view box details
  const handleViewBox = (boxId) => {
    navigate(`/SampleShipment/box/${boxId}`);
  };

  // Handle add sample to box
  const handleAddToBox = (sample) => {
    setSelectedSample(sample);
    setShowAddToBoxModal(true);
  };

  const handleAddToBoxSuccess = () => {
    fetchUnassignedSamples();
    setShowAddToBoxModal(false);
    setSelectedSample(null);
  };

  // Handle mark sample as lost
  const handleMarkAsLost = (sample) => {
    setSampleToMarkLost(sample);
    setShowMarkAsLostModal(true);
  };

  const handleMarkAsLostSuccess = () => {
    fetchUnassignedSamples();
    setShowMarkAsLostModal(false);
    setSampleToMarkLost(null);
  };

  // Handle cancel referral
  const handleCancelReferral = (sample) => {
    setSampleToCancel(sample);
    setShowCancelReferralModal(true);
  };

  const handleCancelReferralSuccess = () => {
    fetchUnassignedSamples();
    setShowCancelReferralModal(false);
    setSampleToCancel(null);
  };

  // Render box state tag
  const renderStateTag = (state) => {
    const stateConfig = {
      DRAFT: {
        type: "gray",
        label: intl.formatMessage({ id: "shipment.state.draft" }),
      },
      READY_TO_SEND: {
        type: "blue",
        label: intl.formatMessage({ id: "shipment.state.readyToSend" }),
      },
      SENT: {
        type: "purple",
        label: intl.formatMessage({ id: "shipment.state.sent" }),
      },
      IN_TRANSIT: {
        type: "cyan",
        label: intl.formatMessage({ id: "shipment.state.inTransit" }),
      },
      PARTIALLY_RECEIVED: {
        type: "warm-gray",
        label: intl.formatMessage({ id: "shipment.state.partiallyReceived" }),
      },
      RECEIVED: {
        type: "green",
        label: intl.formatMessage({ id: "shipment.state.received" }),
      },
      RECONCILED: {
        type: "teal",
        label: intl.formatMessage({ id: "shipment.state.reconciled" }),
      },
      CANCELLED: {
        type: "red",
        label: intl.formatMessage({ id: "shipment.state.cancelled" }),
      },
      LOST_IN_TRANSIT: {
        type: "magenta",
        label: intl.formatMessage({ id: "shipment.state.lostInTransit" }),
      },
    };

    const cfg = stateConfig[state] || { type: "gray", label: state };
    return <Tag type={cfg.type}>{cfg.label}</Tag>;
  };

  // Render days unassigned with visual indicators
  const renderDaysUnassigned = (days) => {
    if (days === undefined || days === null) return "-";

    let tagType = "gray"; // Default
    if (days >= 30) {
      tagType = "red"; // Critical - 30+ days
    } else if (days >= 7) {
      tagType = "yellow"; // Warning - 7-29 days
    }

    return (
      <Tag type={tagType}>
        {days} {intl.formatMessage({ id: "label.days" })}
      </Tag>
    );
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
      key: "typeOfSample",
      header: intl.formatMessage({ id: "sample.label.typeOfSample" }),
    },
    {
      key: "referralTests",
      header: intl.formatMessage({ id: "shipment.label.tests" }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({ id: "sample.label.collectionDate" }),
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
      id: sample.sampleItemId || sample.id?.toString() || "-",
      accessionNumber: sample.accessionNumber || "-",
      typeOfSample: sample.typeOfSample || "-",
      referralTests: sample.referralTests
        ? sample.referralTests.map((t) => t.testName).join(", ")
        : "-",
      collectionDate: sample.collectionDate
        ? new Date(sample.collectionDate).toLocaleDateString()
        : "-",
      actions: (
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button
            kind="ghost"
            size="sm"
            onClick={() => handleAddToBox(sample)}
            style={{ paddingLeft: 0 }}
          >
            <FormattedMessage id="shipment.action.addToBox" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            onClick={() => handleMarkAsLost(sample)}
          >
            <FormattedMessage id="shipment.action.markAsLost" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            onClick={() => handleCancelReferral(sample)}
          >
            <FormattedMessage id="shipment.action.cancelReferral" />
          </Button>
        </div>
      ),
    }));
  };

  return (
    <div className="shipment-dashboard">
      <AlertDialog />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
        ]}
      />
      <ShipmentNavigation />
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

      <Grid fullWidth className="statistics-cards">
        <Column lg={4} md={2} sm={4}>
          <Tile className="stat-card in-transit">
            <div className="stat-value">{statistics.inTransit}</div>
            <div className="stat-label">
              <FormattedMessage id="shipment.stats.inTransit" />
            </div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile className="stat-card delivered">
            <div className="stat-value">{statistics.delivered}</div>
            <div className="stat-label">
              <FormattedMessage id="shipment.stats.delivered" />
            </div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile className="stat-card reconciled">
            <div className="stat-value">{statistics.reconciled}</div>
            <div className="stat-label">
              <FormattedMessage id="shipment.stats.reconciled" />
            </div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile className="stat-card total-samples">
            <div className="stat-value">{statistics.totalSamples}</div>
            <div className="stat-label">
              <FormattedMessage id="shipment.stats.totalSamples" />
            </div>
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
              <Tab className="unassigned-tab">
                <span className="tab-label-with-badge">
                  <FormattedMessage id="shipment.tab.unassignedSamples" />
                  <Tag size="sm" type="red">
                    {unassignedSamples.length}
                  </Tag>
                </span>
              </Tab>
            </TabList>

            <TabPanels>
              {/* Boxes Tab */}
              <TabPanel>
                <div className="tab-toolbar">
                  <Search
                    size="lg"
                    placeholder={intl.formatMessage({
                      id: "shipment.dashboard.scanBoxPlaceholder",
                    })}
                    labelText={intl.formatMessage({ id: "search.label" })}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") handleScanBoxId();
                    }}
                    value={searchTerm}
                  />
                  <Dropdown
                    id="state-filter"
                    titleText={intl.formatMessage({
                      id: "shipment.filter.state",
                    })}
                    label={intl.formatMessage({ id: "label.select" })}
                    items={boxStates}
                    itemToString={(item) => (item ? item.text : "")}
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
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={facilities.find(
                      (f) => f.id === filterFacility,
                    )}
                    onChange={({ selectedItem }) =>
                      setFilterFacility(selectedItem?.id || "")
                    }
                  />
                  <DatePicker
                    datePickerType="single"
                    onChange={([date]) => setFilterDateFrom(date)}
                    value={filterDateFrom}
                  >
                    <DatePickerInput
                      id="date-from"
                      placeholder="mm/dd/yyyy"
                      labelText={intl.formatMessage({
                        id: "shipment.filter.dateFrom",
                      })}
                      size="md"
                    />
                  </DatePicker>
                  <DatePicker
                    datePickerType="single"
                    onChange={([date]) => setFilterDateTo(date)}
                    value={filterDateTo}
                  >
                    <DatePickerInput
                      id="date-to"
                      placeholder="mm/dd/yyyy"
                      labelText={intl.formatMessage({
                        id: "shipment.filter.dateTo",
                      })}
                      size="md"
                    />
                  </DatePicker>
                  {(filterDateFrom || filterDateTo) && (
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={() => {
                        setFilterDateFrom(null);
                        setFilterDateTo(null);
                      }}
                    >
                      <FormattedMessage id="shipment.filter.clear" />
                    </Button>
                  )}
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
                    itemToString={(item) => (item ? item.text : "")}
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

      {/* Add to Box Modal */}
      {showAddToBoxModal && (
        <AddToBoxModal
          open={showAddToBoxModal}
          onClose={() => {
            setShowAddToBoxModal(false);
            setSelectedSample(null);
          }}
          sample={selectedSample}
          onSuccess={handleAddToBoxSuccess}
        />
      )}

      {/* Mark as Lost Modal */}
      {showMarkAsLostModal && (
        <MarkAsLostModal
          open={showMarkAsLostModal}
          onClose={() => {
            setShowMarkAsLostModal(false);
            setSampleToMarkLost(null);
          }}
          sample={sampleToMarkLost}
          onSuccess={handleMarkAsLostSuccess}
        />
      )}

      {/* Cancel Referral Modal */}
      {showCancelReferralModal && (
        <CancelReferralModal
          open={showCancelReferralModal}
          onClose={() => {
            setShowCancelReferralModal(false);
            setSampleToCancel(null);
          }}
          sample={sampleToCancel}
          onSuccess={handleCancelReferralSuccess}
        />
      )}
    </div>
  );
};

export default ShipmentDashboard;
