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
  InlineNotification,
  Stack,
} from "@carbon/react";
import "./Dashboard.css";
import {
  Minimize,
  Maximize,
  InProgress,
  TaskView,
  CheckmarkFilled,
  IncompleteCancel,
  DocumentAdd,
  CloseOutline,
  Printer,
  EmailNew,
  Time,
  WarningSquareFilled,
} from "@carbon/react/icons";
import { Copy } from "@carbon/icons-react";

// Map each metric type to a representative icon shown in the top-left of its card.
const TILE_ICONS: Record<string, any> = {
  ORDERS_IN_PROGRESS: InProgress,
  ORDERS_READY_FOR_VALIDATION: TaskView,
  ORDERS_COMPLETED_TODAY: CheckmarkFilled,
  ORDERS_PARTIALLY_COMPLETED_TODAY: IncompleteCancel,
  ORDERS_ENTERED_BY_USER_TODAY: DocumentAdd,
  ORDERS_REJECTED_TODAY: CloseOutline,
  UN_PRINTED_RESULTS: Printer,
  INCOMING_ORDERS: EmailNew,
  AVERAGE_TURN_AROUND_TIME: Time,
  DELAYED_TURN_AROUND: WarningSquareFilled,
};
import { useState, useEffect, useContext, useRef } from "react";
import {
  getFromOpenElisServer,
  convertAlphaNumLabNumForDisplay,
  hasRole,
} from "../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

interface DashBoardProps {}

interface Tile {
  title: string | JSX.Element;
  subTitle?: string | JSX.Element;
  type: MetricType;
  value: number;
  id?: number;
}
type MetricType =
  | "ORDERS_IN_PROGRESS"
  | "ORDERS_READY_FOR_VALIDATION"
  | "ORDERS_COMPLETED_TODAY"
  | "ORDERS_PARTIALLY_COMPLETED_TODAY"
  | "ORDERS_ENTERED_BY_USER_TODAY"
  | "ORDERS_REJECTED_TODAY"
  | "UN_PRINTED_RESULTS"
  | "INCOMING_ORDERS"
  | "AVERAGE_TURN_AROUND_TIME"
  | "DELAYED_TURN_AROUND"
  | "ORDERS_FOR_USER";

interface UserSessionDetails {
  userSessionDetails: any;
}

interface Notification {
  notificationVisible: any;
  setNotificationVisible: any;
  addNotification: any;
}

const HomeDashBoard: React.FC<DashBoardProps> = () => {
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
  const [testSections, setTestSections] = useState<
    { id: string; value: string }[]
  >([]);
  const [selectedTestSection, setSelectedTestSection] = useState("");
  const [loading, setLoading] = useState(true);
  const [metricsFailed, setMetricsFailed] = useState(false);
  const [metricsAttempt, setMetricsAttempt] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [selectedTile, setSelectedTile] = useState<Tile>(null);
  // Identifies the tile load in flight, so a superseded response is dropped.
  const latestRequest = useRef(0);
  const [url, setUrl] = useState("");
  const { userSessionDetails } = useContext(
    UserSessionDetailsContext,
  ) as UserSessionDetails;
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as Notification;

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setMetricsFailed(false);
    getFromOpenElisServer<typeof counts>(
      "/rest/home-dashboard/metrics",
      (data) => {
        if (controller.signal.aborted) return;
        if (data == null) {
          setMetricsFailed(true);
        } else {
          setCounts(data);
        }
        setLoading(false);
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [metricsAttempt]);

  useEffect(() => {
    if (selectedTile != null) {
      const requestId = ++latestRequest.current;
      setPage(1);
      setLoading(true);
      if (selectedTile.type == "AVERAGE_TURN_AROUND_TIME") {
        getFromOpenElisServer(
          "/rest/home-dashboard/turn-around-time-metrics",
          loadTimeMetrics,
        );
      } else if (selectedTile.type == "ORDERS_FOR_USER") {
        getFromOpenElisServer(
          "/rest/home-dashboard/" +
            selectedTile.type +
            "?systemUserId=" +
            selectedTile.id,
          (res) => loadData(res, requestId),
        );
      } else {
        getFromOpenElisServer(
          "/rest/home-dashboard/" + selectedTile.type,
          (res) => loadData(res, requestId),
        );
      }
    }
  }, [selectedTile]);

  // A narrowed list is shorter, so the page the user was on may no longer exist.
  useEffect(() => {
    setPage(1);
  }, [selectedTestSection]);

  useEffect(() => {
    if (!userSessionDetails?.loginName) return;
    getFromOpenElisServer("/rest/user-test-sections/ALL", (res: any) => {
      const sections = Array.isArray(res) ? res : [];
      setTestSections(sections);
      if (hasRole(userSessionDetails, "Global Administrator")) {
        setSelectedTestSection("all");
      } else {
        setSelectedTestSection(sections[0]?.id);
      }
    });
  }, [userSessionDetails]);

  // The server splits the list into pages of its own. The table below pages it
  // again, so with a table page size as large as the server's, every order past
  // the first server page was unreachable: the next button had nothing left to
  // show. Pull the remaining server pages in and hand the table the full list.
  const loadRemainingResultPages = (
    loadedItems: any[],
    pageToLoad: number,
    totalPages: number,
    requestId: number,
  ) => {
    getFromOpenElisServer(
      "/rest/home-dashboard/" + selectedTile.type + "?page=" + pageToLoad,
      (res) => {
        if (requestId !== latestRequest.current) {
          return;
        }
        const items = loadedItems.concat(res?.displayItems ?? []);
        setData(items);
        if (pageToLoad < totalPages) {
          loadRemainingResultPages(
            items,
            pageToLoad + 1,
            totalPages,
            requestId,
          );
        } else {
          setLoading(false);
        }
      },
    );
  };

  const loadData = (res, requestId: number) => {
    // A newer tile was opened while this request was in flight; its data wins.
    if (requestId !== latestRequest.current) {
      return;
    }

    // If the response object is not null and has displayItems array with length greater than 0 then set it as data.
    const items =
      res && res.displayItems && res.displayItems.length > 0
        ? res.displayItems
        : [];
    setData(items);
    setPage(1);

    // The rest of the server's pages belong to the same list, so fetch them
    // before handing over to the table's own pagination.
    const totalPages = parseInt(res?.paging?.totalPages) || 1;
    const currentPage = parseInt(res?.paging?.currentPage) || 1;
    if (totalPages > currentPage) {
      loadRemainingResultPages(items, currentPage + 1, totalPages, requestId);
    } else {
      setLoading(false);
    }
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
      title: <FormattedMessage id="dashboard.complete.orders.label" />,
      type: "ORDERS_COMPLETED_TODAY",
      value: counts.ordersCompletedToday,
    },
    {
      title: <FormattedMessage id="dashboard.partially.completed.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.partially.completed.subtitle.label" />
      ),
      type: "ORDERS_PARTIALLY_COMPLETED_TODAY",
      value: counts.patiallyCompletedToday,
    },
    {
      title: <FormattedMessage id="dashboard.user.orders.label" />,
      type: "ORDERS_ENTERED_BY_USER_TODAY",
      value: counts.orderEnterdByUserToday,
    },
    {
      title: <FormattedMessage id="dashboard.rejected.orders" />,
      type: "ORDERS_REJECTED_TODAY",
      value: counts.ordersRejectedToday,
    },
    {
      title: <FormattedMessage id="dashboard.unprints.results.label" />,
      type: "UN_PRINTED_RESULTS",
      value: counts.unPritendResults,
    },
    {
      title: <FormattedMessage id="sidenav.label.incomingorder" />,
      type: "INCOMING_ORDERS",
      value: counts.incomigOrders,
    },
    {
      title: <FormattedMessage id="dashboard.avg.turn.around.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.avg.turn.around.subtitle.label" />
      ),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: counts.averageTurnAroudTime,
    },
    {
      title: <FormattedMessage id="dashboard.turn.around.label" />,
      subTitle: <FormattedMessage id="dashboard.turn.around.subtitle.label" />,
      type: "DELAYED_TURN_AROUND",
      value: counts.delayedTurnAround,
    },
  ];

  const averageTimeTileList: Array<Tile> = [
    {
      title: (
        <FormattedMessage id="dashboard.avg.turn.around.reception.to.validation.label" />
      ),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToValidation,
    },
    {
      title: (
        <FormattedMessage id="dashboard.avg.turn.around.reception.to.result.label" />
      ),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToResult,
    },
    {
      title: (
        <FormattedMessage id="dashboard.avg.turn.around.result.to.validation.label" />
      ),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.resultToValidation,
    },
  ];

  const tilesWithTabs = [
    "ORDERS_IN_PROGRESS",
    "ORDERS_READY_FOR_VALIDATION",
    "ORDERS_COMPLETED_TODAY",
    "ORDERS_REJECTED_TODAY",
    "UN_PRINTED_RESULTS",
    "DELAYED_TURN_AROUND",
    "ORDERS_FOR_USER",
    "ORDERS_PARTIALLY_COMPLETED_TODAY",
  ];

  const handleMinimizeClick = () => {
    console.log("Icon clicked!");
    if (selectedTile.type == "ORDERS_FOR_USER") {
      const tile: Tile = {
        title: <FormattedMessage id="dashboard.user.orders.label" />,
        type: "ORDERS_ENTERED_BY_USER_TODAY",
        value: counts.orderEnterdByUserToday,
      };
      setSelectedTile(tile);
    } else {
      setSelectedTile(null);
      hasRole(userSessionDetails, "Global Administrator")
        ? setSelectedTestSection("all")
        : setSelectedTestSection(testSections[0]?.id);
    }
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

  const viewUserOrders = (row) => {
    console.log("Icon clicked!");
    const firstName = row.cells.find(
      (e) => e.info.header === "userFirstName",
    ).value;
    const lastName = row.cells.find(
      (e) => e.info.header === "userLastName",
    ).value;
    const value = row.cells.find(
      (e) => e.info.header === "countOfOrdersEntered",
    ).value;

    const tile: Tile = {
      title: <FormattedMessage id="dashboard.user.orders.today.label" />,
      subTitle: firstName + " " + lastName,
      type: "ORDERS_FOR_USER",
      value: value,
      id: row.id,
    };
    setSelectedTile(tile);
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

  if (metricsFailed) {
    return (
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <Stack gap={5}>
            <InlineNotification
              kind="error"
              role="alert"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "dashboard.metrics.loadFailed" })}
            />
            <Button onClick={() => setMetricsAttempt((attempt) => attempt + 1)}>
              <FormattedMessage id="common.retry" />
            </Button>
          </Stack>
        </Column>
      </Grid>
    );
  }

  return (
    <>
      {loading && <Loading description="Loading Dasboard..." />}
      {notificationVisible === true ? <AlertDialog /> : ""}
      {selectedTile == null ? (
        <div className="home-dashboard-container">
          {tileList.map((tile, index) => {
            const TileIcon = TILE_ICONS[tile.type];
            return (
              <ClickableTile
                key={index}
                className="dashboard-tile"
                onClick={() => handleMaximizeClick(tile)}
              >
                {TileIcon && (
                  <div className="tile-leading-icon">
                    <TileIcon size={28} />
                  </div>
                )}
                <h5 className="dashboard-tile__title">{tile.title}</h5>
                <p className="dashboard-tile__subtitle">
                  {tile.subTitle ?? " "}
                </p>
                <h2 className="dashboard-tile__value">{tile.value}</h2>

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
            );
          })}
        </div>
      ) : (
        <div className="dashboard-view">
          <Tile className="dashboard-tile">
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <h2 className="dashboard-tile__title-view">
                  {selectedTile.title}
                </h2>
                <p className="dashboard-tile__subtitle-view">
                  {selectedTile.subTitle ?? " "}
                </p>
                <h1 className="dashboard-tile__value-view">
                  {selectedTile.value}
                </h1>
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
              {selectedTile.type == "AVERAGE_TURN_AROUND_TIME" ? (
                <>
                  <div className="home-dashboard-container">
                    {averageTimeTileList.map((tile, index) => (
                      <Tile key={index} className="dashboard-tile">
                        <h5 className="dashboard-tile__title">{tile.title}</h5>
                        <p className="dashboard-tile__subtitle">
                          {tile.subTitle}
                        </p>
                        <h2 className="dashboard-tile__value">{tile.value}</h2>
                      </Tile>
                    ))}
                  </div>
                </>
              ) : (
                <Grid>
                  <Column lg={16} md={8} sm={4}>
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
                        selectedTile.type != "ORDERS_ENTERED_BY_USER_TODAY"
                          ? orderHeaders
                          : userHeaders
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
                                    onClick={() => {
                                      selectedTile.type ==
                                      "ORDERS_ENTERED_BY_USER_TODAY"
                                        ? viewUserOrders(row)
                                        : {};
                                    }}
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
              )}
            </div>
          </Tile>
        </div>
      )}
    </>
  );
};
export default HomeDashBoard;
