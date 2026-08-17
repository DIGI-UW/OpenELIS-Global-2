import React, { useEffect, useMemo, useState } from "react";
import {
  DataTable,
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Pagination,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QAEmptyState from "../common/QAEmptyState";
import "../qi/QIDashboard.css";

/**
 * Cross-NCE CAPA Register (OGC-707) at /qa/qms/capa-register. Read-only view of every
 * corrective/preventive action with its parent NCE. Completion is read from the parent NCE
 * (status/date_completed) since the action log's own completion columns are unused in the React
 * flow. Tiles, filters and pagination are derived client-side from one capped fetch.
 * Filtering and paging are client-side; the backend caps the fetch at 500 rows.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qms.capaRegister", link: "" },
];

const HEADERS = [
  { key: "nceNumber", labelKey: "qa.qms.capaRegister.column.nceNumber" },
  {
    key: "correctiveAction",
    labelKey: "qa.qms.capaRegister.column.correctiveAction",
  },
  { key: "actionType", labelKey: "qa.qms.capaRegister.column.actionType" },
  { key: "personResponsible", labelKey: "qa.qms.capaRegister.column.assignee" },
  { key: "dueDate", labelKey: "qa.qms.capaRegister.column.dueDate" },
  { key: "dateCompleted", labelKey: "qa.qms.capaRegister.column.completed" },
  { key: "status", labelKey: "qa.qms.capaRegister.column.status" },
];

// action_type is a comma-joined set of these codes (see NCECorrectiveAction.jsx checkboxes).
const ACTION_TYPE_KEYS = {
  1: "banner.menu.nonconformity.correctiveActions",
  2: "nonconform.nce.preventive.action",
  3: "nonconform.nce.concurrent.control.action",
};

const STATUS_TAG_TYPE = { open: "blue", overdue: "red", completed: "green" };

function localISO(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
    2,
    "0",
  )}-${String(date.getDate()).padStart(2, "0")}`;
}

function shift(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return localISO(d);
}

// Backend sends dueDate/dateCompleted as yyyy-MM-dd strings, so lexical compare == date compare.
// Exported for the QA Overview's overdue-CAPAs row, so the two counts agree.
export function deriveStatus(row, today) {
  if ((row.nceStatus || "").toLowerCase() === "completed") {
    return "completed";
  }
  if (row.dueDate && row.dueDate < today) {
    return "overdue";
  }
  return "open";
}

function formatActionType(actionType, intl) {
  if (!actionType) {
    return "—";
  }
  return actionType
    .split(",")
    .map((c) => c.trim())
    .filter((c) => ACTION_TYPE_KEYS[c])
    .map((c) => intl.formatMessage({ id: ACTION_TYPE_KEYS[c] }))
    .join(", ");
}

const CapaRegister = () => {
  const intl = useIntl();
  // undefined = loading, null = fetch yielded no data / error
  const [items, setItems] = useState();
  const [statusFilter, setStatusFilter] = useState("all");
  const [assignee, setAssignee] = useState("");
  const [range, setRange] = useState({ fromDate: "", toDate: "" });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);

  useEffect(() => {
    getFromOpenElisServer("/rest/nce/capa-register", (res) =>
      setItems(Array.isArray(res) ? res : null),
    );
  }, []);

  const today = localISO(new Date());
  const weekAhead = shift(7);
  const ninetyAgo = shift(-90);

  const withStatus = useMemo(
    () =>
      (items || []).map((it) => ({ ...it, status: deriveStatus(it, today) })),
    [items, today],
  );

  const tiles = useMemo(() => {
    const open = withStatus.filter((r) => r.status !== "completed");
    return {
      open: open.length,
      overdue: open.filter((r) => r.status === "overdue").length,
      dueThisWeek: open.filter(
        (r) => r.dueDate && r.dueDate >= today && r.dueDate <= weekAhead,
      ).length,
      completed: withStatus.filter(
        (r) =>
          r.status === "completed" &&
          r.dateCompleted &&
          r.dateCompleted >= ninetyAgo,
      ).length,
    };
  }, [withStatus, today, weekAhead, ninetyAgo]);

  const filtered = useMemo(() => {
    const needle = assignee.trim().toLowerCase();
    return withStatus.filter((r) => {
      if (statusFilter !== "all" && r.status !== statusFilter) {
        return false;
      }
      if (
        needle &&
        !(r.personResponsible || "").toLowerCase().includes(needle)
      ) {
        return false;
      }
      if (range.fromDate && range.toDate) {
        if (
          !r.dueDate ||
          r.dueDate < range.fromDate ||
          r.dueDate > range.toDate
        ) {
          return false;
        }
      }
      return true;
    });
  }, [withStatus, statusFilter, assignee, range]);

  const rows = filtered
    .slice(page * pageSize, page * pageSize + pageSize)
    .map((item) => ({
      id: String(item.id),
      nceNumber: item.nceNumber || "—",
      correctiveAction: item.correctiveAction || "—",
      actionType: formatActionType(item.actionType, intl),
      personResponsible: item.personResponsible || "—",
      dueDate: item.dueDate || "—",
      dateCompleted: item.dateCompleted || "—",
      status: (
        <Tag type={STATUS_TAG_TYPE[item.status]} size="sm">
          {intl.formatMessage({
            id: `qa.qms.capaRegister.status.${item.status}`,
          })}
        </Tag>
      ),
    }));

  const statusItems = ["all", "open", "overdue", "completed"];

  return (
    <div className="pageContent qi-dashboard">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qms.capaRegister.title" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qms.capaRegister.subtitle" />
      </p>

      {items === undefined ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={5} />
      ) : items === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qms.capaRegister.error" />
        </p>
      ) : (
        <>
          <div className="qi-dashboard__tiles">
            <div className="qi-tile qi-tile--blue">
              <div className="qi-tile__title">
                <FormattedMessage id="qa.qms.capaRegister.tile.open" />
              </div>
              <div className="qi-tile__value">{tiles.open}</div>
            </div>
            <div className="qi-tile qi-tile--red">
              <div className="qi-tile__title">
                <FormattedMessage id="qa.qms.capaRegister.tile.overdue" />
              </div>
              <div className="qi-tile__value">{tiles.overdue}</div>
            </div>
            <div className="qi-tile qi-tile--amber">
              <div className="qi-tile__title">
                <FormattedMessage id="qa.qms.capaRegister.tile.dueThisWeek" />
              </div>
              <div className="qi-tile__value">{tiles.dueThisWeek}</div>
            </div>
            <div className="qi-tile qi-tile--green">
              <div className="qi-tile__title">
                <FormattedMessage id="qa.qms.capaRegister.tile.completed" />
              </div>
              <div className="qi-tile__value">{tiles.completed}</div>
            </div>
          </div>

          <div className="qi-dashboard__controls">
            <Dropdown
              id="capa-status-filter"
              titleText={intl.formatMessage({
                id: "qa.qms.capaRegister.filter.status",
              })}
              label=""
              items={statusItems}
              selectedItem={statusFilter}
              itemToString={(item) =>
                item
                  ? intl.formatMessage({
                      id:
                        item === "all"
                          ? "qa.qms.capaRegister.filter.status.all"
                          : `qa.qms.capaRegister.status.${item}`,
                    })
                  : ""
              }
              onChange={({ selectedItem }) => {
                setPage(0);
                setStatusFilter(selectedItem);
              }}
            />
            <TextInput
              id="capa-assignee-filter"
              labelText={intl.formatMessage({
                id: "qa.qms.capaRegister.filter.assignee",
              })}
              value={assignee}
              onChange={(e) => {
                setPage(0);
                setAssignee(e.target.value);
              }}
            />
            <DatePicker
              datePickerType="range"
              dateFormat="Y-m-d"
              value={[range.fromDate, range.toDate]}
              onChange={(dates) => {
                setPage(0);
                setRange(
                  dates.length === 2
                    ? {
                        fromDate: localISO(dates[0]),
                        toDate: localISO(dates[1]),
                      }
                    : { fromDate: "", toDate: "" },
                );
              }}
            >
              <DatePickerInput
                id="capa-due-from"
                labelText={intl.formatMessage({
                  id: "qa.qms.capaRegister.filter.from",
                })}
                placeholder="yyyy-mm-dd"
              />
              <DatePickerInput
                id="capa-due-to"
                labelText={intl.formatMessage({
                  id: "qa.qms.capaRegister.filter.to",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </div>

          {filtered.length === 0 ? (
            <QAEmptyState
              titleKey="qa.empty.capaRegister.title"
              subheadKey="qa.empty.capaRegister.subhead"
            />
          ) : (
            <>
              <DataTable
                rows={rows}
                headers={HEADERS.map((h) => ({
                  key: h.key,
                  header: intl.formatMessage({ id: h.labelKey }),
                }))}
              >
                {({
                  rows: tableRows,
                  headers,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
                    <Table size="sm">
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader
                              {...getHeaderProps({ header })}
                              key={header.key}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {tableRows.map((row) => (
                          <TableRow {...getRowProps({ row })} key={row.id}>
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
                page={page + 1}
                pageSize={pageSize}
                pageSizes={[25, 50, 100]}
                totalItems={filtered.length}
                onChange={({ page: newPage, pageSize: newPageSize }) => {
                  setPage(newPage - 1);
                  setPageSize(newPageSize);
                }}
              />
            </>
          )}
        </>
      )}
    </div>
  );
};

export default CapaRegister;
