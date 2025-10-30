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
  FilterableMultiSelect,
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

function NoteBookDashBoard() {
  const componentMounted = useRef(false);

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [noteBookEntries, setNoteBookEntries] = useState([]);
  const [types, setTypes] = useState([]);
  const [filters, setFilters] = useState({
    searchTerm: "",
    statuses: [],
    types: [],
    fromdate: "",
    todate: "",
  });

  const [counts, setCounts] = useState({
    total: 0,
    drafts: 0,
    pending: 0,
    finalized: 0,
  });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const intl = useIntl();

  const setStatusList = (statusList) => {
    if (componentMounted.current) {
      setStatuses(statusList);
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
    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  const setNoteBookEntriesWithIds = (entries) => {
    if (componentMounted.current) {
      if (entries && entries.length > 0) {
        setNoteBookEntries(entries);
      } else {
        setNoteBookEntries([]);
      }
      setLoading(false);
    }
  };

  const filtersToParameters = () => {
    return (
      "statuses=" +
      filters.statuses
        .map((entry) => {
          return entry.id;
        })
        .join(",") +
      "&types=" +
      filters.types
        .map((entry) => {
          return entry.id;
        })
        .join(",")
    );
  };

  const refreshItems = () => {
    getFromOpenElisServer(
      "/rest/notebook/dashboard/items?" + filtersToParameters(),
      setNoteBookEntriesWithIds,
    );
  };

  const openNoteBookView = (id) => {
    window.location.href = "/NoteBookEntryForm/" + id;
  };

  const openNoteBookEntryForm = () => {
    window.location.href = "/NoteBookEntryForm/0";
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_STATUS", setStatusList);
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_EXPT_TYPE", setTypes);
    getFromOpenElisServer("/rest/notebook/dashboard/metrics", loadCounts);

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
      title: intl.formatMessage({ id: "notebook.label.total" }),
      count: counts.total,
    },
    {
      title: intl.formatMessage({ id: "notebook.label.drafts" }),
      count: counts.drafts,
    },
    {
      title: intl.formatMessage({ id: "notebook.label.pending" }),
      count: counts.pending,
    },
    {
      title:
        intl.formatMessage({ id: "notebook.label.finalized" }) +
        "(Week " +
        getPastWeek() +
        " )",
      count: counts.finalized,
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
    { label: "label.button.newEntry", link: "/NoteBookEntryForm" },
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
                <FormattedMessage id="notebook.page.title" />
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
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Button
              onClick={() => {
                openNoteBookEntryForm();
              }}
            >
              <FormattedMessage id="label.button.newEntry" />
            </Button>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <br />
          </Column>
        </Grid>
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={4} md={4} sm={2}>
            <Search
              value={filters.searchTerm}
              // onChange={(e) =>
              //   setFilters({ ...filters, searchTerm: e.target.value })
              // }
              placeholder={intl.formatMessage({
                id: "label.search.labno.family",
              })}
              labelText={intl.formatMessage({
                id: "label.search.labno.family",
              })}
            />
          </Column>
          <Column lg={1} md={4} sm={2}>
            <FormattedMessage id="filters.label" />
          </Column>
          <Column lg={3} md={4} sm={2}>
            <FilterableMultiSelect
              id="statuses"
              titleText={intl.formatMessage({ id: "label.filters.status" })}
              items={statuses}
              itemToString={(item) => (item ? item.value : "")}
              onChange={(changes) => {
                setFilters({ ...filters, statuses: changes.selectedItems });
              }}
              selectionFeedback="top-after-reopen"
            />
          </Column>
          <Column lg={3} md={4} sm={2}>
            <FilterableMultiSelect
              id="types"
              titleText={intl.formatMessage({
                id: "notebook.label.filter.types",
              })}
              items={types}
              itemToString={(item) => (item ? item.value : "")}
              onChange={(changes) => {
                setFilters({ ...filters, types: changes.selectedItems });
              }}
              selectionFeedback="top-after-reopen"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={noteBookEntries.slice(
                (page - 1) * pageSize,
                page * pageSize,
              )}
              headers={[
                {
                  key: "title",
                  header: intl.formatMessage({ id: "Title" }),
                },
                {
                  key: "typeName",
                  header: intl.formatMessage({ id: "Experiment Type" }),
                },
                {
                  key: "status",
                  header: intl.formatMessage({ id: "Status" }),
                },
                {
                  key: "firstName",
                  header: intl.formatMessage({ id: "patient.first.name" }),
                },
                {
                  key: "lastName",
                  header: intl.formatMessage({ id: "patient.last.name" }),
                },
                {
                  key: "gender",
                  header: intl.formatMessage({ id: "patient.first.gender" }),
                },

                {
                  key: "dateCreated",
                  header: intl.formatMessage({ id: "Date Created" }),
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
                              openNoteBookView(row.id);
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
              totalItems={noteBookEntries.length}
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

export default NoteBookDashBoard;
