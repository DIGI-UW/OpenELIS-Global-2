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
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import "./../pathology/PathologyDashboard.css";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import PageBreadCrumb from "../common/PageBreadCrumb";

function ImmunohistochemistryDashboard() {
  const componentMounted = useRef(false);

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
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);

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
  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
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
      refreshItems,
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
      refreshItems,
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

  const setImmunohistochemistryEntriesWithIds = (entries) => {
    if (componentMounted.current) {
      if (entries && entries.length > 0) {
        setImmunohistochemistryEntries(
          entries.map((entry) => {
            return { ...entry, id: "" + entry.immunohistochemistrySampleId };
          }),
        );
      } else {
        setImmunohistochemistryEntries([]);
      }
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

  const refreshItems = () => {
    getFromOpenElisServer(
      "/rest/immunohistochemistry/dashboard?" + filtersToParameters(),
      setImmunohistochemistryEntriesWithIds,
    );
  };

  const openCaseView = (id) => {
    window.location.href = "/ImmunohistochemistryCaseView/" + id;
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
          <Column lg={8} md={4} sm={2}>
            <div className="inlineDivBlock">
              <div>Filters:</div>
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
              <Select
                id="statusFilter"
                name="statusFilter"
                labelText={intl.formatMessage({
                  id: "label.filters.status",
                })}
                value={
                  filters.statuses.length > 1 ? "All" : filters.statuses[0].id
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
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={immunohistochemistryEntries.slice(
                (page - 1) * pageSize,
                page * pageSize,
              )}
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
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={immunohistochemistryEntries.length}
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

export default ImmunohistochemistryDashboard;
