import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  Tile,
  Loading,
  FilterableMultiSelect,
  TextInput,
  Tag,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer, hasRole } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import "../pathology/PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomDatePicker from "../common/CustomDatePicker";
import {
  UserAvatar,
  Document,
  Time,
  UserAvatarFilledAlt,
  Tag as TagIcon,
  Add,
  Edit,
  Checkmark,
  Edit as EditIcon,
  InProgress,
  Locked,
  Archive,
  View,
  List,
} from "@carbon/react/icons";
import "./NoteBook.css";

function NoteBookDashBoard() {
  const componentMounted = useRef(false);

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [noteBookEntries, setNoteBookEntries] = useState([]);
  const [noteBooks, setNoteBooks] = useState([]);
  const [selectedNoteBook, setSelectedNoteBook] = useState(null);
  const [types, setTypes] = useState([]);
  const [filters, setFilters] = useState({
    statuses: [],
    types: [],
    tags: "",
    fromdate: "",
    todate: "",
    notebookid: null,
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

  const statusColors = {
    DRAFT: "gray",
    SUBMITTED: "cyan",
    FINALIZED: "green",
    LOCKED: "purple",
    ARCHIVED: "gray",
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "DRAFT":
        return <EditIcon size={15} />;
      case "SUBMITTED":
        return <InProgress size={15} />;
      case "FINALIZED":
        return <Checkmark size={15} />;
      case "LOCKED":
        return <Locked size={15} />;
      case "ARCHIVED":
        return <Archive size={15} />;
      default:
        return <Document size={15} />;
    }
  };

  const handleDatePickerChangeDate = (datePicker, date) => {
    let obj = null;
    switch (datePicker) {
      case "startDate":
        setFilters({ ...filters, fromdate: date });
        break;
      case "endDate":
        setFilters({ ...filters, todate: date });
        break;
      default:
    }
  };

  const loadNoteBookEntries = (entries) => {
    if (componentMounted.current) {
      if (entries && entries.length > 0) {
        setNoteBookEntries(entries);
      } else {
        setNoteBookEntries([]);
      }
      setLoading(false);
    }
  };

  const loadNoteBooks = (entries) => {
    if (componentMounted.current) {
      if (entries && entries.length > 0) {
        setNoteBooks(entries);
      } else {
        setNoteBooks([]);
      }
      setLoading(false);
    }
  };

  const filtersToParameters = () => {
    let params =
      "statuses=" +
      filters.statuses.map((entry) => entry.id).join(",") +
      "&types=" +
      filters.types.map((entry) => entry.id).join(",") +
      "&fromDate=" +
      filters.fromdate +
      "&toDate=" +
      filters.todate +
      "&tags=" +
      filters.tags;

    if (selectedNoteBook) {
      params += "&noteBookId=" + filters.notebookid;
    }
    return params;
  };

  const refreshItems = () => {
    getFromOpenElisServer(
      "/rest/notebook/dashboard/entries?" + filtersToParameters(),
      loadNoteBookEntries,
    );
  };

  const openNoteBookView = (id) => {
    window.location.href = "/NoteBookEntryForm/" + id;
  };

  const openNoteBookEntryForm = () => {
    window.location.href = "/NoteBookEntryForm";
  };

  const openNoteBookInstanceEntryForm = () => {
    window.location.href = "/NoteBookInstanceEntryForm/" + selectedNoteBook.id;
  };

  const openNoteBookInstanceView = (id) => {
    window.location.href = "/NoteBookInstanceEditForm/" + id;
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_STATUS", setStatusList);
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_EXPT_TYPE", setTypes);
    getFromOpenElisServer("/rest/notebook/dashboard/metrics", loadCounts);
    getFromOpenElisServer("/rest/notebook//dashboard/notebooks", loadNoteBooks);

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
      title: intl.formatMessage({ id: "notebook.label.finalized" }),
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

  useEffect(() => {
    componentMounted.current = true;
    if (selectedNoteBook) {
      setFilters({ ...filters, notebookid: selectedNoteBook.id });
    }
    return () => {
      componentMounted.current = false;
    };
  }, [selectedNoteBook]);

  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "label.button.newEntry", link: "/NoteBookEntryForm" },
  ];

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };
  const handleSelectNoteBook = (id) => {
    const notebook = noteBooks.find((entry) => entry.id === id);
    setSelectedNoteBook(notebook);
  };

  const renderCell = (cell, row) => {
    if (cell.info.header === "title") {
      return (
        <TableCell key={cell.id}>
          <div style={{ display: "flex", alignItems: "center" }}>
            <Button
              kind="ghost"
              hasIconOnly
              renderIcon={Edit}
              iconDescription={intl.formatMessage({
                id: "notebook.icon.edit",
              })}
              size="sm"
              onClick={() => openNoteBookView(row.id)}
            ></Button>
            {cell.value}
          </div>
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && (
        <Loading
          description={intl.formatMessage({ id: "loading.description" })}
        />
      )}

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
      <Grid fullWidth={true}>
        <Column lg={3} md={8} sm={4}>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Button
                style={{ width: "70%" }}
                size="sm"
                onClick={() => {
                  openNoteBookEntryForm();
                }}
              >
                <Add />
                <FormattedMessage id="notebook.button.newLabNotebook" />
              </Button>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Button
                style={{ width: "70%" }}
                size="sm"
                onClick={() => {
                  setFilters({
                    statuses: [],
                    types: [],
                    tags: "",
                    fromdate: "",
                    todate: "",
                    notebookid: null,
                  });
                  setSelectedNoteBook(null);
                }}
              >
                <List />
                <FormattedMessage id="notebook.heading.allEntries" />
              </Button>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
          </Grid>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="notebook.heading.notebooks" />
              </h4>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <DataTable
                rows={noteBooks.slice((page - 1) * pageSize, page * pageSize)}
                headers={[
                  {
                    key: "title",
                    header: <FormattedMessage id="notebook.label.title" />,
                  },
                  {
                    key: "entriesCount",
                    header: (
                      <FormattedMessage id="notebook.table.header.entries" />
                    ),
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
                                handleSelectNoteBook(row.id);
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
              <div style={{ overflowX: "auto" }}>
                <Pagination
                  onChange={handlePageChange}
                  page={page}
                  pageSize={pageSize}
                  pageSizes={[10, 20, 30, 50, 100]}
                  totalItems={noteBooks.length}
                  forwardText={intl.formatMessage({ id: "pagination.forward" })}
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
              </div>
            </Column>
          </Grid>
        </Column>
        <Column lg={13}>
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
              {selectedNoteBook ? (
                <>
                  <Column lg={11} md={8} sm={4}>
                    <h4> {selectedNoteBook.title} </h4>
                  </Column>
                  <Column lg={4} md={8} sm={4}>
                    <Button
                      size="sm"
                      onClick={() => {
                        openNoteBookInstanceEntryForm();
                      }}
                    >
                      <FormattedMessage id="label.button.newEntry" />
                    </Button>
                  </Column>
                </>
              ) : (
                <Column lg={16} md={8} sm={4}>
                  <>
                    {" "}
                    <h4>
                      <FormattedMessage id="notebook.heading.allEntries" />
                    </h4>
                  </>
                </Column>
              )}

              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
            </Grid>

            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={4} sm={2}>
                <FormattedMessage id="filters.label" /> :
              </Column>
              <Column lg={2} md={4} sm={2}>
                <FilterableMultiSelect
                  id="statuses"
                  titleText={intl.formatMessage({ id: "label.filters.status" })}
                  items={statuses}
                  itemToString={(item) => (item ? item.value : "")}
                  initialSelectedItems={filters.statuses}
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
                  initialSelectedItems={filters.types}
                  itemToString={(item) => (item ? item.value : "")}
                  onChange={(changes) => {
                    setFilters({ ...filters, types: changes.selectedItems });
                  }}
                  selectionFeedback="top-after-reopen"
                />
              </Column>
              <Column lg={2} md={4} sm={2}>
                <TextInput
                  id="title"
                  name="title"
                  labelText={intl.formatMessage({
                    id: "notebook.tags.modal.add.label",
                  })}
                  placeholder={intl.formatMessage({
                    id: "notebook.tag.placeholder",
                  })}
                  value={filters.tags}
                  onChange={(e) => {
                    setFilters({ ...filters, tags: e.target.value });
                  }}
                  required
                />
              </Column>
              <Column lg={3} md={8} sm={4}>
                <CustomDatePicker
                  key="startDate"
                  id={"startDate"}
                  labelText={intl.formatMessage({
                    id: "eorder.date.start",
                    defaultMessage: "Start Date",
                  })}
                  // disallowFutureDate={true}
                  autofillDate={true}
                  value={filters.statuses}
                  onChange={(date) =>
                    handleDatePickerChangeDate("startDate", date)
                  }
                />
              </Column>
              <Column lg={3} md={8} sm={4}>
                <CustomDatePicker
                  key="endDate"
                  id={"endDate"}
                  labelText={intl.formatMessage({
                    id: "eorder.date.end",
                    defaultMessage: "End Date",
                  })}
                  //disallowFutureDate={true}
                  autofillDate={true}
                  value={filters.todate}
                  onChange={(date) =>
                    handleDatePickerChangeDate("endDate", date)
                  }
                />
              </Column>

              <Column lg={16} md={8} sm={4}></Column>
            </Grid>
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <div className="notebook-dashboard-container">
                  {noteBookEntries.map((entry, index) => (
                    <Tile key={index} className="notebook-dashboard-tile">
                      <div className="notebook-tile-content">
                        <Grid>
                          <Column lg={16} md={8} sm={4}>
                            <h3 className="notebook-tile-title">
                              {entry.title}
                            </h3>
                            <hr></hr>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            {getStatusIcon(entry.status)}
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            <Tag
                              style={{
                                fontWeight: "bold",
                              }}
                              size="sm"
                              type={statusColors[entry.status]}
                            >
                              {entry.status}
                            </Tag>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            <Document size={15} />
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            <div className="notebook-tile-subtitle">
                              {entry.typeName}
                            </div>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            <Time size={15} />
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            <div className="notebook-tile-subtitle">
                              {entry.dateCreated}
                            </div>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            <TagIcon size={15} />
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            {entry.tags.map((tag) => (
                              <Tag
                                key={tag}
                                style={{
                                  fontSize: "0.6rem",
                                }}
                              >
                                {tag}
                              </Tag>
                            ))}
                          </Column>
                        </Grid>
                      </div>
                      <div className="notebook-tile-buttons">
                        <Grid>
                          <Column lg={8} md={8} sm={4}>
                            <Button
                              kind="secondary"
                              size="sm"
                              onClick={() => openNoteBookInstanceView(entry.id)}
                            >
                              <View size={13} />
                              <FormattedMessage id="notebook.button.view" />
                            </Button>
                          </Column>
                          <Column lg={8} md={8} sm={4}>
                            {entry.status === "DRAFT" && (
                              <Button
                                kind="primary"
                                size="sm"
                                onClick={() =>
                                  openNoteBookInstanceView(entry.id)
                                }
                              >
                                <Edit size={13} />
                                <FormattedMessage id="notebook.button.edit" />
                              </Button>
                            )}
                          </Column>
                        </Grid>
                      </div>
                    </Tile>
                  ))}
                </div>
              </Column>
            </Grid>
          </div>
        </Column>
      </Grid>
    </>
  );
}

export default NoteBookDashBoard;
