import React from "react";
import {
  Tile,
  ClickableTile,
  Loading,
  Grid,
  Button,
  Column,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Link,
  Tab,
  Tabs,
  TabList,
  Tag,
} from "@carbon/react";
import "./Dashboard.css";
import { Minimize, Maximize, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { Copy } from "@carbon/icons-react";
import { useState, useEffect, useRef, useContext } from "react";
import {
  getFromOpenElisServer,
  convertAlphaNumLabNumForDisplay,
  hasRole,
} from "../utils/Utils.js";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

interface DashBoardProps {}

interface Tile {
  title: string | JSX.Element;
  subTitle: string | JSX.Element;
  type: MetricType;
  value: number;
  id?: number;
}
type MetricType =
  | "ORDERS_IN_PROGRESS"
  | "ORDERS_READY_FOR_VALIDATION"
  | "ORDERS_COMPLETED_TODAY"
  | "INCOMING_ORDERS";

interface UserSessionDetails {
  userSessionDetails: any;
}

interface Notification {
  notificationVisible: any;
  setNotificationVisible: any;
  addNotification: any;
}

const LabDashboard: React.FC<DashBoardProps> = () => {
  const intl = useIntl();

  const [counts, setCounts] = useState({
    ordersInProgress: 0,
    ordersReadyForValidation: 0,
    ordersCompletedToday: 0,
    patiallyCompletedToday: 0,
    orderEnterdByUserToday: 0,
    ordersRejectedToday: 0,
    unPritendResults: 0,
    incomigOrders: 0,
    averageTurnAroudTime: 0,
    delayedTurnAround: 0,
  });

  const [timeMetrics, setTimeMetrics] = useState({
    receptionToResult: 0,
    resultToValidation: 0,
    receptionToValidation: 0,
  });

  const [data, setData] = useState([]);
  const [testSections, setTestSections] = useState([]);
  const [selectedTestSection, setSelectedTestSection] = useState("");
  const [loading, setLoading] = useState(true);
  const componentMounted = useRef(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [selectedTile, setSelectedTile] = useState<Tile>(null);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [url, setUrl] = useState("");
  const { userSessionDetails } = useContext(
    UserSessionDetailsContext,
  ) as UserSessionDetails;
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as Notification;

  useEffect(() => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
  }, []);

  useEffect(() => {
    getFromOpenElisServer("/rest/home-dashboard/metrics", loadCount);

    return () => {
      // This code runs when component is unmounted
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!selectedTile) return;
    if (selectedTile.type !== "ORDERS_IN_PROGRESS") return;

    setLoading(true);

    // ✅ CHANGED LOGIC FOR ENDPOINT
    const endpoint =
      selectedTestSection === "all"
        ? "/rest/home-dashboard/ORDERS-Grouped"
        : "/rest/home-dashboard/ORDERS_IN_PROGRESS";

    getFromOpenElisServer(endpoint, loadData);
  }, [selectedTestSection]);

  useEffect(() => {
    if (selectedTile != null) {
      setNextPage(null);
      setPreviousPage(null);
      setPagination(false);
      setLoading(true);
      if (selectedTile.type === "ORDERS_IN_PROGRESS") {
        const endpoint =
          selectedTestSection === "all"
            ? "/rest/home-dashboard/ORDERS-Grouped"
            : "/rest/home-dashboard/ORDERS_IN_PROGRESS";

        getFromOpenElisServer(endpoint, loadData);
      } else {
        getFromOpenElisServer(
          "/rest/home-dashboard/" + selectedTile.type,
          loadData,
        );
      }
    }

    return () => {
      componentMounted.current = false;
    };
  }, [selectedTile]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/user-test-sections/ALL",
      (fetchedTestSections) => {
        fetchTestSections(fetchedTestSections);
      },
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const fetchTestSections = (res) => {
    setTestSections(res);
    hasRole(userSessionDetails, "Global Administrator")
      ? setSelectedTestSection("all")
      : setSelectedTestSection(res[0]?.id);
  };

  const loadNextResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/home-dashboard/" + selectedTile.type + "?page=" + nextPage,
      loadData,
    );
  };

  const loadPreviousResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/home-dashboard/" + selectedTile.type + "?page=" + previousPage,
      loadData,
    );
  };

  const loadCount = (data) => {
    if (componentMounted.current) {
      setCounts(data);
      setLoading(false);
    }
  };

  const loadData = (res) => {
    // If the response object is not null and has displayItems array with length greater than 0 then set it as data.
    if (res && res.displayItems && res.displayItems.length > 0) {
      setData(res.displayItems);
    } else {
      setData([]);
    }

    // Sets next and previous page numbers based on the total pages and current page number.
    if (res && res.paging) {
      const { totalPages, currentPage } = res.paging;
      if (totalPages > 1) {
        setPagination(true);
        setCurrentApiPage(currentPage);
        setTotalApiPages(totalPages);
        if (parseInt(currentPage) < parseInt(totalPages)) {
          setNextPage(parseInt(currentPage) + 1);
        } else {
          setNextPage(null);
        }

        if (parseInt(currentPage) > 1) {
          setPreviousPage(parseInt(currentPage) - 1);
        } else {
          setPreviousPage(null);
        }
      }
    }

    setLoading(false);
  };

  const loadTimeMetrics = (data) => {
    setTimeMetrics(data);
    setLoading(false);
  };

  const tileList: Array<Tile> = [
    {
      title: <FormattedMessage id="dashboard.in.progress.label" />,
      subTitle: <FormattedMessage id="dashboard.in.progress.subtitle.label" />,
      type: "ORDERS_IN_PROGRESS",
      value: counts.ordersInProgress,
    },
    {
      title: <FormattedMessage id="dashboard.validation.ready.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.validation.ready.subtitle.label" />
      ),
      type: "ORDERS_READY_FOR_VALIDATION",
      value: counts.ordersReadyForValidation,
    },
    {
      title: <FormattedMessage id="dashboard.print.orders.label" />,
      subTitle: <FormattedMessage id="dashboard.print.orders.subtitle.label" />,
      type: "ORDERS_COMPLETED_TODAY",
      value: counts.ordersCompletedToday,
    },
    {
      title: <FormattedMessage id="sidenav.label.incomingorder" />,
      subTitle: <FormattedMessage id="label.electronic.orders" />,
      type: "INCOMING_ORDERS",
      value: counts.incomigOrders,
    }
  ];

  const tilesWithTabs = [
    "ORDERS_IN_PROGRESS",
    "ORDERS_READY_FOR_VALIDATION",
    "ORDERS_COMPLETED_TODAY"
  ];

  const handleMinimizeClick = () => {
    console.log("Icon clicked!");
      setSelectedTile(null);
      hasRole(userSessionDetails, "Global Administrator")
        ? setSelectedTestSection("all")
        : setSelectedTestSection(testSections[0]?.id);
  };

  const handleMaximizeClick = (tile) => {
    if (
      testSections?.length > 0 ||
      hasRole(userSessionDetails, "Global Administrator")
    ) {
      setSelectedTile(tile);
    } else {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "accessDenied.title" }),
        message: intl.formatMessage({ id: "accessDenied.message" }),
      });
    }
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };
  const renderCell = (cell, row) => {
    if (cell.info.header === "labNumber" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <>
            <div style={{ display: "flex", alignItems: "center" }}>
              <Button
                onClick={async () => {
                  if ("clipboard" in navigator) {
                    return await navigator.clipboard.writeText(cell.value);
                  } else {
                    return document.execCommand("copy", true, cell.value);
                  }
                }}
                kind="ghost"
                iconDescription={intl.formatMessage({
                  id: "instructions.copy.labnum",
                })}
                hasIconOnly
                renderIcon={Copy}
              />
              {selectedTile.type == "ORDERS_IN_PROGRESS" ||
              selectedTile.type == "ORDERS_READY_FOR_VALIDATION" ? (
                <Link
                  style={{ color: "blue" }}
                  href={
                    selectedTile.type == "ORDERS_IN_PROGRESS"
                      ? "/result?type=order&doRange=false&accessionNumber=" +
                        cell.value
                      : "validation?type=order&accessionNumber=" + cell.value
                  }
                >
                  <u>{convertAlphaNumLabNumForDisplay(cell.value)}</u>
                </Link>
              ) : (
                <> {convertAlphaNumLabNumForDisplay(cell.value)}</>
              )}
            </div>
          </>
        </TableCell>
      );
    } else if (cell.info.header === "countOfOrdersEntered" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <Link style={{ color: "blue" }}>{cell.value} </Link>
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  const orderHeaders = [
    {
      key: "priority",
      header: <FormattedMessage id="eorder.priority" />,
    },
    {
      key: "orderDate",
      header: <FormattedMessage id="sample.label.orderdate" />,
    },
    {
      key: "patientId",
      header: <FormattedMessage id="patient.id" />,
    },
    {
      key: "labNumber",
      header: <FormattedMessage id="eorder.labNumber" />,
    },
    {
      key: "testName",
      header: <FormattedMessage id="eorder.test.name" />,
    },
  ];

  const userHeaders = [
    {
      key: "userFirstName",
      header: "First Name",
    },
    {
      key: "userLastName",
      header: "Last Name",
    },
    {
      key: "countOfOrdersEntered",
      header: "Orders Entered",
    },
  ];

  return (
    <>
      {loading && <Loading description="Loading Dasboard..." />}
      {notificationVisible === true ? <AlertDialog /> : ""}
      {selectedTile == null ? (
        <div className="home-dashboard-container">
          {tileList.map((tile, index) => (
            <ClickableTile
              key={index}
              className="dashboard-tile"
              onClick={() => handleMaximizeClick(tile)}
            >
              <h3 className="tile-title">{tile.title}</h3>
              <p className="tile-subtitle">{tile.subTitle}</p>
              <p className="tile-value">{tile.value}</p>

              <div className="tile-icon">
                <div
                  onClick={() => handleMaximizeClick(tile)}
                  className="icon-wrapper"
                >
                  <Maximize
                    id="maximizeIcon"
                    size={20}
                    className="clickable-icon"
                  />
                </div>
              </div>
            </ClickableTile>
          ))}
        </div>
      ) : (
        <div className="dashboard-view">
          <Tile className="dashboard-tile">
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <h3 className="tile-title-view">{selectedTile.title}</h3>
                <p className="tile-subtitle-view">{selectedTile.subTitle}</p>
                <p className="tile-value-view">{selectedTile.value}</p>
                {
                  <div className="tile-icon">
                    <div onClick={handleMinimizeClick} className="icon-wrapper">
                      <Minimize
                        id="minimizeIcon"
                        size={20}
                        className="clickable-icon"
                      />
                    </div>
                  </div>
                }
              </Column>
            </Grid>
            <div className="gridBoundary">
                <Grid>
                  <Column lg={16} md={8} sm={4}>
                    {pagination && (
                      <Grid>
                        <Column lg={14} />
                        <Column
                          lg={2}
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "center",
                            gap: "10px",
                            width: "110%",
                          }}
                        >
                          <Link>
                            {currentApiPage} / {totalApiPages}
                          </Link>
                          <div style={{ display: "flex", gap: "10px" }}>
                            <Button
                              hasIconOnly
                              id="loadpreviousresults"
                              onClick={loadPreviousResultsPage}
                              disabled={previousPage != null ? false : true}
                              renderIcon={ArrowLeft}
                              iconDescription="previous"
                            ></Button>
                            <Button
                              hasIconOnly
                              id="loadnextresults"
                              onClick={loadNextResultsPage}
                              disabled={nextPage != null ? false : true}
                              renderIcon={ArrowRight}
                              iconDescription="next"
                            ></Button>
                          </div>
                        </Column>
                      </Grid>
                    )}
                    {tilesWithTabs.includes(selectedTile.type) && (
                      <Grid>
                        <Column lg={16} md={8} sm={4}>
                          <Tabs>
                            {hasRole(
                              userSessionDetails,
                              "Global Administrator",
                            ) ? (
                              <TabList
                                style={{ width: "100%" }}
                                aria-label="List of tabs"
                                contained
                              >
                                <Tab
                                  onClick={() => setSelectedTestSection("all")}
                                >
                                  <FormattedMessage id="all.label" />
                                </Tab>

                                {testSections?.map((item, id) => {
                                  return (
                                    <Tab
                                      key={id}
                                      onClick={() =>
                                        setSelectedTestSection(item.id)
                                      }
                                    >
                                      {item.value}
                                    </Tab>
                                  );
                                })}
                              </TabList>
                            ) : (
                              <TabList
                                style={{ width: "100%" }}
                                aria-label="List of tabs"
                                contained
                              >
                                {testSections?.map((item, id) => {
                                  return (
                                    <Tab
                                      key={id}
                                      onClick={() =>
                                        setSelectedTestSection(item.id)
                                      }
                                    >
                                      {item.value}
                                    </Tab>
                                  );
                                })}
                              </TabList>
                            )}
                          </Tabs>
                        </Column>
                      </Grid>
                    )}
                    <DataTable
                      rows={data
                        .filter((item) =>
                          tilesWithTabs.includes(selectedTile.type) &&
                          selectedTestSection != "all"
                            ? item.testSection === selectedTestSection
                            : true,
                        )
                        .slice((page - 1) * pageSize, page * pageSize)}
                      headers={
                          orderHeaders
                      }
                      isSortable
                    >
                      {({ rows, headers, getHeaderProps, getTableProps }) => (
                        <TableContainer title="" description="">
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                {headers.map((header) => (
                                  <TableHeader
                                    key={header.key}
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              <>
                                {rows.map((row) => (
                                  <TableRow
                                    key={row.id}
                                  >
                                    {row.cells.map((cell) =>
                                      renderCell(cell, row),
                                    )}
                                  </TableRow>
                                ))}
                              </>
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                    <Pagination
                      onChange={handlePageChange}
                      page={page}
                      pageSize={pageSize}
                      pageSizes={[10, 20, 30, 50, 100]}
                      totalItems={
                        data.filter((item) =>
                          tilesWithTabs.includes(selectedTile.type) &&
                          selectedTestSection != "all"
                            ? item.testSection === selectedTestSection
                            : true,
                        ).length
                      }
                      forwardText={intl.formatMessage({
                        id: "pagination.forward",
                      })}
                      backwardText={intl.formatMessage({
                        id: "pagination.backward",
                      })}
                      itemRangeText={(min, max, total) =>
                        intl.formatMessage(
                          { id: "pagination.item-range" },
                          { min: min, max: max, total: total },
                        )
                      }
                      itemsPerPageText={intl.formatMessage({
                        id: "pagination.items-per-page",
                      })}
                      itemText={(min, max) =>
                        intl.formatMessage(
                          { id: "pagination.item" },
                          { min: min, max: max },
                        )
                      }
                      pageNumberText={intl.formatMessage({
                        id: "pagination.page-number",
                      })}
                      pageRangeText={(_current, total) =>
                        intl.formatMessage(
                          { id: "pagination.page-range" },
                          { total: total },
                        )
                      }
                      pageText={(page, pagesUnknown) =>
                        intl.formatMessage(
                          { id: "pagination.page" },
                          { page: pagesUnknown ? "" : page },
                        )
                      }
                    />
                  </Column>
                </Grid>  
            </div>
          </Tile>
        </div>
      )}
    </>
  );
};
export default LabDashboard;
