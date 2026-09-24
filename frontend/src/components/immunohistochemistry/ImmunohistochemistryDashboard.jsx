import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Checkbox,
  Heading,
  Select,
  SelectItem,
  Button,
  Grid,
  Column,
  Tile,
  Loading,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Section,
  Pagination,
} from "@carbon/react";
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
import "./../pathology/PathologyDashboard.css";
import { useHistory } from "react-router-dom";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import PageBreadCrumb from "../common/PageBreadCrumb";

function ImmunohistochemistryDashboard() {
  const componentMounted = useRef(false);
  const history = useHistory();

  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { notificationVisible } = useContext(NotificationContext);

  const intl = useIntl();

  const [counts, setCounts] = useState({
    inProgress: 0,
    awaitingReview: 0,
    complete: 0,
  });
  const [statuses, setStatuses] = useState([]);
  const [immunohistochemistryEntries, setImmunohistochemistryEntries] =
    useState([]);
  const [filters, setFilters] = useState({
    searchTerm: "",
    myCases: false,
    statuses: [
      {
        id: "IN_PROGRESS",
        value: "In Progress",
      },
    ],
  });
  const [loading, setLoading] = useState(true);
  // The server's page announcement for the list shown, and the rows a full
  // server page holds; Carbon's items per page is pinned to the latter so
  // Carbon's page is the server's page.
  const [paging, setPaging] = useState();
  const [serverPageSize, setServerPageSize] = useState();

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
      title: intl.formatMessage({ id: "pathology.label.casesInProgress" }),
      count: counts.inProgress,
    },
    {
      title: intl.formatMessage({ id: "immunohistochemistry.label.review" }),
      count: counts.awaitingReview,
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

  const setStatusList = (statusList) => {
    if (componentMounted.current) {
      setStatuses(statusList);
    }
  };

  const assignCurrentUserAsTechnician = (
    event,
    immunohistochemistrySampleId,
  ) => {
    postToOpenElisServerFullResponse(
      "/rest/immunohistochemistry/assignTechnician?immunohistochemistrySampleId=" +
        immunohistochemistrySampleId,
      {},
      () => refreshItems(Number(paging?.currentPage) || 1),
    );
  };

  const assignCurrentUserAsPathologist = (
    event,
    immunohistochemistrySampleId,
  ) => {
    postToOpenElisServerFullResponse(
      "/rest/immunohistochemistry/assignPathologist?immunohistochemistrySampleId=" +
        immunohistochemistrySampleId,
      {},
      () => refreshItems(Number(paging?.currentPage) || 1),
    );
  };

  const renderCell = (cell, row) => {
    var status = row.cells.find((e) => e.info.header === "status").value;
    var immunohistochemistrySampleId = row.id;

    if (cell.info.header === "assignedTechnician" && !cell.value) {
      return (
        <TableCell key={cell.id}>
          <Button
            type="button"
            onClick={(e) => {
              assignCurrentUserAsTechnician(e, immunohistochemistrySampleId);
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
              assignCurrentUserAsPathologist(e, immunohistochemistrySampleId);
            }}
          >
            <FormattedMessage id="label.button.start" />
          </Button>
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  /** One server page of cases and the page announcement it came with. */
  const setImmunohistochemistryEntriesWithIds = (response) => {
    if (componentMounted.current) {
      const entries = response?.items || [];
      if (entries.length > 0) {
        setImmunohistochemistryEntries(
          entries.map((entry) => {
            return { ...entry, id: "" + entry.immunohistochemistrySampleId };
          }),
        );
      } else {
        setImmunohistochemistryEntries([]);
      }
      setPaging(response?.paging);
      setServerPageSize((previous) =>
        serverPageSizeOf(response?.paging, entries.length, previous),
      );
      setLoading(false);
    }
  };

  const setStatusFilter = (event) => {
    if (event.target.value === "All") {
      setFilters({ ...filters, statuses: statuses });
    } else {
      setFilters({ ...filters, statuses: [{ id: event.target.value }] });
    }
  };
  const loadCounts = (data) => {
    setCounts(data);
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
    getFromOpenElisServer(
      "/rest/immunohistochemistry/dashboard?page=" + pageNumber,
      setImmunohistochemistryEntriesWithIds,
    );
  };

  /**
   * Runs the search again and, when asked, reopens the page that was showing
   * (an assignment changes a row, not the list) as long as it still exists.
   */
  const refreshItems = (pageToReopen) => {
    getFromOpenElisServer(
      "/rest/immunohistochemistry/dashboard?" + filtersToParameters(),
      (response) => {
        setImmunohistochemistryEntriesWithIds(response);
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
    history.push("/ImmunohistochemistryCaseView/" + id);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/displayList/IMMUNOHISTOCHEMISTRY_STATUS",
      setStatusList,
    );
    getFromOpenElisServer(
      "/rest/immunohistochemistry/dashboard/count",
      loadCounts,
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    componentMounted.current = true;
    setFilters({
      ...filters,
      statuses: [
        {
          id: "IN_PROGRESS",
          value: "In Progress",
        },
      ],
    });

    return () => {
      componentMounted.current = false;
    };
  }, [statuses]);

  useEffect(() => {
    componentMounted.current = true;
    refreshItems();
    return () => {
      componentMounted.current = false;
    };
  }, [filters]);

  let breadcrumbs = [
    { label: "home.label", link: "/" },
    {
      label: "immunohistochemistry.label.dashboard",
      link: "/ImmunohistochemistryDashboard",
    },
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
                <FormattedMessage id="immunohistochemistry.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="dashboard-container">
        {tileList.map((tile, index) => (
          <Tile key={index} className="dashboard-tile">
            <h3 className="tile-title tile-title-Immuno">{tile.title}</h3>
            <p className="tile-value">{tile.count}</p>
          </Tile>
        ))}
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
          <Column lg={8} md={4} sm={4}>
            <Grid fullWidth={true}>
              <Column lg={1} md={1} sm={1}>
                <div style={{ marginTop: "9px" }}>
                  <FormattedMessage id="filters.label" />
                </div>
              </Column>
              <div style={{ marginTop: "4px" }}>
                <Checkbox
                  labelText={intl.formatMessage({
                    id: "label.filters.mycases",
                  })}
                  id="filterMyCases"
                  value={filters.myCases}
                  onChange={(e) =>
                    setFilters({ ...filters, myCases: e.target.checked })
                  }
                />
              </div>
              <Column lg={3} md={2} sm={2}>
                <Select
                  id="statusFilter"
                  name="statusFilter"
                  labelText={intl.formatMessage({
                    id: "label.filters.status",
                  })}
                  value={
                    filters.statuses.length > 1
                      ? "All"
                      : filters.statuses[0]?.id || "IN_PROGRESS"
                  }
                  onChange={setStatusFilter}
                  noLabel
                >
                  <SelectItem disabled value="placeholder" text="Status" />
                  <SelectItem text="All" value="All" />
                  {statuses.map((status, index) => {
                    return (
                      <SelectItem
                        key={index}
                        text={status.value}
                        value={status.id}
                      />
                    );
                  })}
                </Select>
              </Column>
            </Grid>
          </Column>

          <Column lg={16} md={8} sm={4}>
            {arrows.show && <ServerPageArrows {...arrows} />}
            <DataTable
              rows={immunohistochemistryEntries}
              headers={[
                {
                  key: "requestDate",
                  header: "Request Date",
                },
                {
                  key: "status",
                  header: "Stage",
                },
                {
                  key: "lastName",
                  header: "Last Name",
                },
                {
                  key: "firstName",
                  header: "First Name",
                },
                {
                  key: "assignedTechnician",
                  header: "Assigned Technician",
                },
                {
                  key: "assignedPathologist",
                  header: "Assigned Pathologist",
                },
                {
                  key: "labNumber",
                  header: "Lab Number",
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
                rowsOnPage: immunohistochemistryEntries.length,
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

export default ImmunohistochemistryDashboard;
