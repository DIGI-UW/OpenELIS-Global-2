import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Checkbox,
  Heading,
  Select,
  SelectItem,
  Button,
  Grid,
  Column,
  Section,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tile,
  ClickableTile,
  Loading,
  Pagination,
} from "@carbon/react";
import { useHistory } from "react-router-dom";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { Search } from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  hasRole,
} from "../utils/Utils";
import {
  serverPageArrowsProps,
  serverPageSizeOf,
  serverPaginationProps,
} from "../utils/serverPaging";
import ServerPageArrows from "../common/ServerPageArrows";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import "./PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { inProgressStageIds, stageLabel } from "./pathologyStages";

function PathologyDashboard() {
  const componentMounted = useRef(false);
  const history = useHistory();

  const intl = useIntl();

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [statuses, setStatuses] = useState([]);
  const [pathologyEntries, setPathologyEntries] = useState([]);
  // The server's page announcement for the list shown, and the rows a full
  // server page holds; Carbon's items per page is pinned to the latter so
  // Carbon's page is the server's page.
  const [paging, setPaging] = useState();
  const [serverPageSize, setServerPageSize] = useState();
  // Identifies the load in flight: the search runs more than once while the
  // filters settle, and a late answer must not pull the page back to 1.
  const latestRequest = useRef(0);
  const [filters, setFilters] = useState({
    searchTerm: "",
    myCases: false,
    statuses: [{}],
  });

  const [inProgressStatuses, setInProgressStatuses] = useState([]);

  const [counts, setCounts] = useState({
    inProgress: 0,
    awaitingReview: 0,
    additionalRequests: 0,
    complete: 0,
  });
  const [loading, setLoading] = useState(true);
  const [inProgressStatusObjects, setInProgressStatusObjects] = useState(
    inProgressStatuses.map((statusId) => ({ id: statusId })),
  );
  const setStatusList = (statusList) => {
    if (componentMounted.current) {
      // Set all statuses
      setStatuses(statusList);

      // Mirrors the backend dashboard tile grouping (everything except
      // awaiting-review and complete counts as in progress) so the filter
      // and the tiles never disagree about which cases are in progress.
      const filteredStatuses = inProgressStageIds(
        statusList.map((status) => status.id),
      );

      setInProgressStatuses(filteredStatuses);

      // Update the inProgressStatusObjects state
      setInProgressStatusObjects(
        filteredStatuses.map((statusId) => ({ id: statusId })),
      );

      // Set filters using the updated state
      setFilters((prev) => ({
        ...prev,
        statuses: filteredStatuses.map((statusId) => ({ id: statusId })),
      }));
    }
  };

  const assignCurrentUserAsTechnician = (event, pathologySampleId) => {
    postToOpenElisServerFullResponse(
      "/rest/pathology/assignTechnician?pathologySampleId=" + pathologySampleId,
      {},
      () => refreshItems(Number(paging?.currentPage) || 1),
    );
  };

  const assignCurrentUserAsPathologist = (event, pathologySampleId) => {
    postToOpenElisServerFullResponse(
      "/rest/pathology/assignPathologist?pathologySampleId=" +
        pathologySampleId,
      {},
      () => refreshItems(Number(paging?.currentPage) || 1),
    );
  };

  const renderCell = (cell, row) => {
    var status = row.cells.find((e) => e.info.header === "status").value;
    var pathologySampleId = row.id;

    if (cell.info.header === "assignedTechnician" && !cell.value) {
      return (
        <TableCell key={cell.id}>
          <Button
            type="button"
            onClick={(e) => {
              assignCurrentUserAsTechnician(e, pathologySampleId);
            }}
          >
            <FormattedMessage id="label.button.start" />
          </Button>
        </TableCell>
      );
    }
    if (
      cell.info.header === "assignedPathologist" &&
      !cell.value &&
      status === "READY_PATHOLOGIST" &&
      hasRole(userSessionDetails, "Pathologist")
    ) {
      return (
        <TableCell key={cell.id}>
          <Button
            type="button"
            onClick={(e) => {
              assignCurrentUserAsPathologist(e, pathologySampleId);
            }}
          >
            <FormattedMessage id="label.button.start" />
          </Button>
        </TableCell>
      );
    }
    if (cell.info.header === "status") {
      return (
        <TableCell key={cell.id}>
          {stageLabel(
            intl,
            cell.value,
            statuses.find((s) => s.id === cell.value)?.value,
          )}
        </TableCell>
      );
    }
    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  /** One server page of cases and the page announcement it came with. */
  const setPathologyEntriesWithIds = (response) => {
    if (componentMounted.current) {
      const entries = response?.items || [];
      if (entries.length > 0) {
        setPathologyEntries(
          entries.map((entry) => {
            return { ...entry, id: "" + entry.pathologySampleId };
          }),
        );
      } else {
        setPathologyEntries([]);
      }
      setPaging(response?.paging);
      setServerPageSize((previous) =>
        serverPageSizeOf(response?.paging, entries.length, previous),
      );
      setLoading(false);
    }
  };

  const setStatusFilter = (event) => {
    const { value } = event.target;

    if (value === "All") {
      setFilters({ ...filters, statuses: statuses });
    } else if (value === "IN_PROGRESS") {
      setFilters({ ...filters, statuses: inProgressStatusObjects });
    } else {
      setFilters({ ...filters, statuses: [{ id: value }] });
    }
  };

  const getSelectedValue = () => {
    const selectedValue =
      filters.statuses.length === inProgressStatuses.length &&
      filters.statuses.every((status) => inProgressStatuses.includes(status.id))
        ? "IN_PROGRESS"
        : filters.statuses.length > 1
          ? "All"
          : filters.statuses[0]?.id || "IN_PROGRESS";

    return selectedValue;
  };

  const selectStatusFilter = (statusId) => {
    setFilters({ ...filters, statuses: [{ id: statusId }] });
  };

  const filtersToParameters = () => {
    return (
      "statuses=" +
      filters.statuses
        .map((entry) => {
          return entry.id;
        })
        .join(",") +
      "&searchTerm=" +
      filters.searchTerm
    );
  };

  /** One server page of the last search, the same request for the arrows and for Carbon. */
  const loadPage = (pageNumber) => {
    const requestId = ++latestRequest.current;
    getFromOpenElisServer(
      "/rest/pathology/dashboard?page=" + pageNumber,
      (response) => {
        if (requestId === latestRequest.current) {
          setPathologyEntriesWithIds(response);
        }
      },
    );
  };

  /**
   * Runs the search again and, when asked, reopens the page that was showing
   * (an assignment changes a row, not the list) as long as it still exists.
   */
  const refreshItems = (pageToReopen) => {
    const requestId = ++latestRequest.current;
    getFromOpenElisServer(
      "/rest/pathology/dashboard?" + filtersToParameters(),
      (response) => {
        if (requestId !== latestRequest.current) {
          return;
        }
        setPathologyEntriesWithIds(response);
        const reopen = Number(pageToReopen) || 1;
        if (
          reopen > 1 &&
          reopen <= (Number(response?.paging?.totalPages) || 1)
        ) {
          loadPage(reopen);
        }
      },
    );
  };

  const arrows = serverPageArrowsProps({ paging, onPageRequest: loadPage });

  const openCaseView = (id) => {
    history.push("/PathologyCaseView/" + id);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/PATHOLOGY_STATUS", setStatusList);
    getFromOpenElisServer("/rest/pathology/dashboard/count", loadCounts);

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadCounts = (data) => {
    setCounts(data);
  };

  function formatDateToDDMMYYYY(date) {
    var day = date.getDate();
    var month = date.getMonth() + 1; // Month is zero-based
    var year = date.getFullYear();

    // Ensure leading zeros for single-digit day and month
    var formattedDay = (day < 10 ? "0" : "") + day;
    var formattedMonth = (month < 10 ? "0" : "") + month;

    // Construct the formatted string
    var formattedDate = formattedDay + "/" + formattedMonth + "/" + year;
    return formattedDate;
  }

  const getPastWeek = () => {
    // Get the current date
    var currentDate = new Date();

    // Calculate the date of the past week
    var pastWeekDate = new Date(currentDate);
    pastWeekDate.setDate(currentDate.getDate() - 7);

    return (
      formatDateToDDMMYYYY(pastWeekDate) +
      " - " +
      formatDateToDDMMYYYY(currentDate)
    );
  };

  const tileList = [
    {
      title: <FormattedMessage id="pathology.label.casesInProgress" />,
      count: counts.inProgress,
    },
    {
      title: <FormattedMessage id="pathology.label.review" />,
      count: counts.awaitingReview,
      onClick: () => selectStatusFilter("READY_PATHOLOGIST"),
    },
    {
      title: <FormattedMessage id="pathology.label.requests" />,
      count: counts.additionalRequests,
    },
    {
      title:
        intl.formatMessage({ id: "pathology.label.complete" }) +
        "(Week " +
        getPastWeek() +
        " )",
      count: counts.complete,
    },
  ];

  useEffect(() => {
    componentMounted.current = true;
    refreshItems();
    return () => {
      componentMounted.current = false;
    };
  }, [filters]);

  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "pathology.label.dashboard", link: "/PathologyDashboard" },
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading description="Loading Dasboard..." />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="pathology.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="dashboard-container">
        {tileList.map((tile, index) => {
          const DashboardTile = tile.onClick ? ClickableTile : Tile;

          return (
            <DashboardTile
              key={index}
              className="dashboard-tile"
              onClick={tile.onClick}
            >
              <h3 className="tile-title">{tile.title}</h3>
              <p className="tile-value">{tile.count}</p>
            </DashboardTile>
          );
        })}
      </div>
      <div className="orderLegendBody">
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={8} md={4} sm={2}>
            <Search
              size="sm"
              value={filters.searchTerm}
              onChange={(e) =>
                setFilters({ ...filters, searchTerm: e.target.value })
              }
              placeholder={intl.formatMessage({
                id: "label.search.labno.family",
              })}
              labelText={intl.formatMessage({
                id: "label.search.labno.family",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={2}>
            <div className="inlineDivBlock">
              <div>Filters:</div>
              <Checkbox
                labelText={intl.formatMessage({ id: "label.filters.mycases" })}
                id="filterMyCases"
                value={filters.myCases}
                onChange={(e) =>
                  setFilters({ ...filters, myCases: e.target.checked })
                }
              />
              <Select
                id="statusFilter"
                name="statusFilter"
                labelText={intl.formatMessage({ id: "label.filters.status" })}
                value={getSelectedValue()}
                onChange={setStatusFilter}
                noLabel
              >
                <SelectItem
                  disabled
                  value="placeholder"
                  text={intl.formatMessage({ id: "common.status" })}
                />
                <SelectItem
                  text={intl.formatMessage({ id: "common.all" })}
                  value="All"
                />
                <SelectItem
                  text={intl.formatMessage({ id: "common.inProgress" })}
                  value="IN_PROGRESS"
                />
                {statuses.map((status, index) => (
                  <SelectItem
                    key={index}
                    text={stageLabel(intl, status.id, status.value)}
                    value={status.id}
                  />
                ))}
              </Select>
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            {arrows.show && <ServerPageArrows {...arrows} />}
            <DataTable
              rows={pathologyEntries}
              headers={[
                {
                  key: "requestDate",
                  header: <FormattedMessage id="sample.requestDate" />,
                },
                {
                  key: "status",
                  header: <FormattedMessage id="pathology.label.stage" />,
                },
                {
                  key: "lastName",
                  header: <FormattedMessage id="patient.last.name" />,
                },
                {
                  key: "firstName",
                  header: <FormattedMessage id="patient.first.name" />,
                },
                {
                  key: "assignedTechnician",
                  header: <FormattedMessage id="assigned.technician.label" />,
                },
                {
                  key: "assignedPathologist",
                  header: <FormattedMessage id="assigned.pathologist.label" />,
                },
                {
                  key: "labNumber",
                  header: <FormattedMessage id="sample.label.labnumber" />,
                },
              ]}
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
                              openCaseView(row.id);
                            }}
                          >
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
                        ))}
                      </>
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
            <Pagination
              {...serverPaginationProps({
                paging,
                rowsOnPage: pathologyEntries.length,
                pageSize: serverPageSize,
                onPageRequest: loadPage,
                intl,
              })}
            />
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default PathologyDashboard;
