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
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { Search } from "@carbon/react";
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
} from "@carbon/react/icons";
import "./NoteBook.css";

function NoteBookDashBoard() {
  const componentMounted = useRef(false);

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [noteBookEntries, setNoteBookEntries] = useState([]);
  const [types, setTypes] = useState([]);
  const [filters, setFilters] = useState({
    statuses: [],
    types: [],
    tags: "",
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

  const statusColors = {
    DRAFT: "gray",
    SUBMITTED: "cyan",
    FINALIZED: "green",
    LOCKED: "purple",
    ARCHIVED: "gray",
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
        .join(",") +
      "&fromDate=" +
      filters.fromdate +
      "&toDate=" +
      filters.todate +
      "&tags=" +
      filters.tags
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
    window.location.href = "/NoteBookEntryForm";
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
          <Column lg={1} md={4} sm={2}>
            <FormattedMessage id="filters.label" />
          </Column>
          <Column lg={3} md={4} sm={2}>
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
          <Column lg={3} md={4} sm={2}>
            <TextInput
              id="title"
              name="title"
              labelText={intl.formatMessage({
                id: "notebook.tags.modal.add.label",
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
              onChange={(date) => handleDatePickerChangeDate("startDate", date)}
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
              onChange={(date) => handleDatePickerChangeDate("endDate", date)}
            />
          </Column>

          <Column lg={16} md={8} sm={4}></Column>
        </Grid>
        <div className="notebook-dashboard-container">
          {noteBookEntries.map((entry, index) => (
            <Tile key={index} className="notebook-dashboard-tile">
              <Grid>
                <Column lg={8} md={8} sm={4}>
                  <h3 className="notebook-tile-title">{entry.title}</h3>
                </Column>
                <Column lg={8} md={8} sm={4}>
                  <Tag
                    style={{
                      fontWeight: "bold",
                    }}
                    type={statusColors[entry.status]}
                  >
                    {entry.status}
                  </Tag>
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <UserAvatarFilledAlt size={15} />
                </Column>
                <Column lg={14} md={8} sm={4}>
                  <div className="notebook-tile-subtitle">
                    {entry.firstName} {entry.lastName}
                  </div>
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <Document size={15} />
                </Column>
                <Column lg={14} md={8} sm={4}>
                  <div className="notebook-tile-subtitle">{entry.typeName}</div>
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
                <Column lg={8} md={8} sm={4}>
                  <Button
                    kind="secondary"
                    size="sm"
                    onClick={() => openNoteBookView(entry.id)}
                  >
                    View
                  </Button>
                </Column>
                <Column lg={8} md={8} sm={4}>
                  {entry.status === "DRAFT" && (
                    <Button
                      kind="primary"
                      size="sm"
                      onClick={() => openNoteBookView(entry.id)}
                    >
                      Edit
                    </Button>
                  )}
                </Column>
              </Grid>
            </Tile>
          ))}
        </div>
      </div>
    </>
  );
}

export default NoteBookDashBoard;
