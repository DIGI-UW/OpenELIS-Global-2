import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  DataTable,
  TableContainer,
  Table,
  TableHeader,
  TableRow,
  TableCell,
  TableBody,
  TableHead,
  Pagination,
  Button,
  DatePicker,
  DatePickerInput,
  Dropdown,
  TextInput,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";
import "../../Style.css";

const headers = [
  {
    key: "timestamp",
    header: <FormattedMessage id="systemAudit.table.heading.time" />,
  },
  {
    key: "entityType",
    header: <FormattedMessage id="systemAudit.table.heading.entityType" />,
  },
  {
    key: "entityId",
    header: <FormattedMessage id="systemAudit.table.heading.entityId" />,
  },
  {
    key: "action",
    header: <FormattedMessage id="systemAudit.table.heading.action" />,
  },
  {
    key: "user",
    header: <FormattedMessage id="systemAudit.table.heading.user" />,
  },
  {
    key: "changes",
    header: <FormattedMessage id="systemAudit.table.heading.changes" />,
  },
];

const SystemAuditEvents = () => {
  const intl = useIntl();
  const [events, setEvents] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [totalItems, setTotalItems] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [entityTypes, setEntityTypes] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedEntityType, setSelectedEntityType] = useState("");
  const [selectedAction, setSelectedAction] = useState("");
  const [selectedUser, setSelectedUser] = useState("");
  const [searchText, setSearchText] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const allLabel = intl.formatMessage({ id: "systemAudit.filter.all" });

  const actionOptions = [
    { id: "", text: allLabel },
    {
      id: "I",
      text: intl.formatMessage({ id: "systemAudit.action.insert" }),
    },
    {
      id: "U",
      text: intl.formatMessage({ id: "systemAudit.action.update" }),
    },
    {
      id: "D",
      text: intl.formatMessage({ id: "systemAudit.action.delete" }),
    },
  ];

  useEffect(() => {
    getFromOpenElisServer("/rest/systemAuditEvents/entityTypes", (data) => {
      if (data) {
        setEntityTypes([{ id: "", name: allLabel }, ...data]);
      }
    });
    getFromOpenElisServer("/rest/users", (data) => {
      if (data) {
        setUsers([{ id: "", value: allLabel }, ...data]);
      }
    });
  }, []);

  const buildParams = useCallback(
    (p, ps) => {
      const params = new URLSearchParams();
      if (p !== undefined) params.set("page", p);
      if (ps !== undefined) params.set("pageSize", ps);
      if (startDate) params.set("startDate", startDate);
      if (endDate) params.set("endDate", endDate);
      if (selectedEntityType) params.set("entityType", selectedEntityType);
      if (selectedAction) params.set("action", selectedAction);
      if (selectedUser) params.set("userId", selectedUser);
      if (searchText) params.set("search", searchText);
      return params;
    },
    [
      startDate,
      endDate,
      selectedEntityType,
      selectedAction,
      selectedUser,
      searchText,
    ],
  );

  const fetchEvents = useCallback(
    (p, ps) => {
      setIsLoading(true);
      const params = buildParams(p, ps);

      getFromOpenElisServer(
        "/rest/systemAuditEvents?" + params.toString(),
        (data) => {
          if (data && data.events) {
            const formatted = data.events.map((e, idx) => {
              const changesObj = e.changes || {};
              const changesStr = Object.keys(changesObj).length > 0
                ? Object.entries(changesObj)
                    .map(([k, v]) => `${k}: ${v}`)
                    .join(", ")
                : "";
              return {
                ...e,
                id: String((p - 1) * ps + idx + 1),
                timestamp: e.timestamp
                  ? new Date(e.timestamp).toLocaleString(navigator.language, {
                      day: "2-digit",
                      month: "2-digit",
                      year: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                      hour12: false,
                    })
                  : "",
                changes: changesStr,
              };
            });
            setEvents(formatted);
            setTotalItems(data.totalItems);
          } else {
            setEvents([]);
            setTotalItems(0);
          }
          setIsLoading(false);
        },
      );
    },
    [buildParams],
  );

  const handleSearch = () => {
    setPage(1);
    fetchEvents(1, pageSize);
  };

  const handlePageChange = (pageInfo) => {
    setPage(pageInfo.page);
    setPageSize(pageInfo.pageSize);
    fetchEvents(pageInfo.page, pageInfo.pageSize);
  };

  const handleExportCsv = () => {
    const params = buildParams();
    window.open(
      config.serverBaseUrl +
        "/rest/systemAuditEvents/export?" +
        params.toString(),
      "_blank",
    );
  };

  const handleExportPdf = () => {
    const params = buildParams();
    window.open(
      config.serverBaseUrl +
        "/rest/systemAuditEvents/exportPdf?" +
        params.toString(),
      "_blank",
    );
  };

  return (
    <>
      <br />
      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Heading>
              <FormattedMessage id="reports.systemAuditTrail" />
            </Heading>
          </Section>
        </Column>
      </Grid>
      <br />
      <Grid fullWidth={true}>
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            onChange={(dates, dateStr) => setStartDate(dateStr)}
          >
            <DatePickerInput
              id="startDate"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({
                id: "systemAudit.filter.startDate",
              })}
            />
          </DatePicker>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            onChange={(dates, dateStr) => setEndDate(dateStr)}
          >
            <DatePickerInput
              id="endDate"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({
                id: "systemAudit.filter.endDate",
              })}
            />
          </DatePicker>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="entityType"
            titleText={intl.formatMessage({
              id: "systemAudit.filter.entityType",
            })}
            items={entityTypes}
            itemToString={(item) => (item ? item.name : "")}
            onChange={({ selectedItem }) =>
              setSelectedEntityType(
                selectedItem
                  ? selectedItem.name === allLabel
                    ? ""
                    : selectedItem.name
                  : "",
              )
            }
            label={intl.formatMessage({ id: "systemAudit.filter.entityType" })}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="action"
            titleText={intl.formatMessage({ id: "systemAudit.filter.action" })}
            items={actionOptions}
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) =>
              setSelectedAction(selectedItem ? selectedItem.id : "")
            }
            label={intl.formatMessage({ id: "systemAudit.filter.action" })}
          />
        </Column>
      </Grid>
      <br />
      <Grid fullWidth={true}>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="user"
            titleText={intl.formatMessage({ id: "systemAudit.filter.user" })}
            items={users}
            itemToString={(item) => (item ? item.value : "")}
            onChange={({ selectedItem }) =>
              setSelectedUser(selectedItem ? selectedItem.id : "")
            }
            label={intl.formatMessage({ id: "systemAudit.filter.user" })}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="searchText"
            labelText={intl.formatMessage({
              id: "systemAudit.filter.searchText",
            })}
            placeholder={intl.formatMessage({
              id: "systemAudit.filter.searchText.placeholder",
            })}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
        </Column>
      </Grid>
      <br />
      <Grid fullWidth={true}>
        <Column lg={16}>
          <Button onClick={handleSearch} style={{ marginRight: "1rem" }}>
            <FormattedMessage id="systemAudit.filter.search" />
            <Loading
              small={true}
              withOverlay={false}
              className={isLoading ? "show" : "hidden"}
            />
          </Button>
          <Button
            kind="secondary"
            onClick={handleExportCsv}
            style={{ marginRight: "1rem" }}
          >
            <FormattedMessage id="systemAudit.filter.export" />
          </Button>
          <Button kind="tertiary" onClick={handleExportPdf}>
            <FormattedMessage id="systemAudit.filter.exportPdf" />
          </Button>
        </Column>
      </Grid>
      <br />
      {events.length === 0 && !isLoading && (
        <Grid fullWidth={true}>
          <Column lg={16}>
            <p>
              <FormattedMessage id="systemAudit.noResults" />
            </p>
          </Column>
        </Grid>
      )}
      {events.length > 0 && (
        <Grid fullWidth={true}>
        <Column lg={16}>
          <DataTable rows={events} headers={headers} isSortable>
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer>
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
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <Pagination
            onChange={handlePageChange}
            page={page}
            pageSize={pageSize}
            pageSizes={[25, 50, 100]}
            totalItems={totalItems}
            forwardText={intl.formatMessage({ id: "pagination.forward" })}
            backwardText={intl.formatMessage({ id: "pagination.backward" })}
            itemRangeText={(min, max, total) =>
              intl.formatMessage(
                { id: "pagination.item-range" },
                { min, max, total },
              )
            }
            itemsPerPageText={intl.formatMessage({
              id: "pagination.items-per-page",
            })}
            itemText={(min, max) =>
              intl.formatMessage({ id: "pagination.item" }, { min, max })
            }
            pageNumberText={intl.formatMessage({
              id: "pagination.page-number",
            })}
            pageRangeText={(_current, total) =>
              intl.formatMessage({ id: "pagination.page-range" }, { total })
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
    </>
  );
};

export default SystemAuditEvents;
