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
  Loading,
  Pagination,
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { Search } from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  hasRole,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import "../pathology/PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";

function CytologyDashboard() {
  const componentMounted = useRef(false);

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [pathologyEntries, setPathologyEntries] = useState([]);
  const [filters, setFilters] = useState({
    searchTerm: "",
    myCases: false,
    statuses: [{}],
  });
  const [inProgressStatuses, setInProgressStatuses] = useState([]);

  const [counts, setCounts] = useState({
    inProgress: 0,
    awaitingReview: 0,
    complete: 0,
  });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const intl = useIntl();
  const [inProgressStatusObjects, setInProgressStatusObjects] = useState(
    inProgressStatuses.map((statusId) => ({ id: statusId })),
  );
  const setStatusList = (statusList) => {
    if (componentMounted.current) {
      // Set all statuses
      setStatuses(statusList);

      // Filter out COMPLETED statuses and update the in-progress statuses state
      const filteredStatuses = statusList
        .filter((status) => status.id !== "COMPLETED")
        .map((status) => status.id);

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
      "/rest/cytology/assignTechnician?cytologySampleId=" + pathologySampleId,
      {},
      refreshItems,
    );
  };

  const assignCurrentUserAsPathologist = (event, pathologySampleId) => {
    postToOpenElisServerFullResponse(
      "/rest/cytology/assignCytoPathologist?cytologySampleId=" +
        pathologySampleId,
      {},
      refreshItems,
    );
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
      cell.info.header === "assignedCytoPathologist" &&
      !cell.value &&
      status === "READY_FOR_CYTOPATHOLOGIST" &&
      hasRole(userSessionDetails, "Cytopathologist")
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
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  const setPathologyEntriesWithIds = (entries) => {
    if (componentMounted.current) {
      if (entries && entries.length > 0) {
        setPathologyEntries(
          entries.map((entry) => {
            return { ...entry, id: "" + entry.pathologySampleId };
          }),
        );
      } else {
        setPathologyEntries([]);
      }
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
          : filters.statuses[0].id;

    return selectedValue;
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

  const refreshItems = () => {
    getFromOpenElisServer(
      "/rest/cytology/dashboard?" + filtersToParameters(),
      setPathologyEntriesWithIds,
    );
  };

  const openCaseView = (id) => {
    window.location.href = "/CytologyCaseView/" + id;
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/CYTOLOGY_STATUS", setStatusList);
    getFromOpenElisServer("/rest/cytology/dashboard/count", loadCounts);

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
      title: intl.formatMessage({ id: "pathology.label.casesInProgress" }),
      count: counts.inProgress,
    },
    {
      title: intl.formatMessage({ id: "cytology.label.review" }),
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

  useEffect(() => {
    componentMounted.current = true;
    refreshItems();
    return () => {
      componentMounted.current = false;
    };
  }, [filters]);

  let breadcrumbs = [{ label: "home.label", link: "/" }];

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
                <FormattedMessage id="cytology.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="dashboard-container">
        {tileList.map((tile, index) => (
          <Tile key={index} className="dashboard-tile">
            <h3 className="tile-title">{tile.title}</h3>
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
          <Column lg={8} md={4} sm={2}>
            <div className="inlineDivBlock">
              <div>
                <FormattedMessage id="filters.label" />
              </div>
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
                <SelectItem disabled value="placeholder" text="Status" />
                <SelectItem text="All" value="All" />
                <SelectItem text="In Progress" value="IN_PROGRESS" />
                {statuses.map((status, index) => (
                  <SelectItem
                    key={index}
                    text={status.value}
                    value={status.id}
                  />
                ))}
              </Select>
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={pathologyEntries.slice(
                (page - 1) * pageSize,
                page * pageSize,
              )}
              headers={[
                {
                  key: "requestDate",
                  header: intl.formatMessage({ id: "sample.requestDate" }),
                },
                {
                  key: "status",
                  header: intl.formatMessage({ id: "label.filters.status" }),
                },
                {
                  key: "lastName",
                  header: intl.formatMessage({ id: "patient.last.name" }),
                },
                {
                  key: "firstName",
                  header: intl.formatMessage({ id: "patient.first.name" }),
                },
                {
                  key: "assignedTechnician",
                  header: intl.formatMessage({
                    id: "label.button.select.technician",
                  }),
                },
                {
                  key: "assignedCytoPathologist",
                  header: intl.formatMessage({
                    id: "assigned.cytopathologist.label",
                  }),
                },
                {
                  key: "labNumber",
                  header: intl.formatMessage({ id: "sample.label.labnumber" }),
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
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={pathologyEntries.length}
              forwardText={intl.formatMessage({ id: "pagination.forward" })}
              backwardText={intl.formatMessage({ id: "pagination.backward" })}
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
    </>
  );
}

export default CytologyDashboard;
