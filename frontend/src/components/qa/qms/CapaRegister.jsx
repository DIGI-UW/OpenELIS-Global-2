import React, { useEffect, useMemo, useState } from "react";
import {
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Pagination,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, toLocalIsoDate } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { formatActionType } from "../../nonconform/common/actionTypes";
import QASimpleTable from "../common/QASimpleTable";
import { deriveStatus } from "../common/capa";
import { isoDaysFromToday } from "../common/qaDates";
import QAEmptyState from "../common/QAEmptyState";
import QAStatTiles from "../common/QAStatTiles";
import "../common/QAStyles.css";

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
  { key: "nceNumber", labelKey: "nce.field.nceNumber" },
  {
    key: "correctiveAction",
    labelKey: "qa.qms.capaRegister.column.correctiveAction",
  },
  { key: "actionType", labelKey: "common.type" },
  { key: "personResponsible", labelKey: "qa.qms.capaRegister.column.assignee" },
  { key: "dueDate", labelKey: "nce.capa.dueDate" },
  { key: "dateCompleted", labelKey: "common.completed" },
  { key: "status", labelKey: "common.status" },
];

const STATUS_TAG_TYPE = { open: "blue", overdue: "red", completed: "green" };

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

  const today = toLocalIsoDate(new Date());
  const weekAhead = isoDaysFromToday(7);
  const ninetyAgo = isoDaysFromToday(-90);

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
      // The shared formatter returns "" for no codes; this table prints a dash.
      actionType: formatActionType(item.actionType, intl) || "—",
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
        <FormattedMessage id="sideNav.label.qa.qms.capaRegister" />
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
          <QAStatTiles
            tiles={[
              {
                labelKey: "nce.status.open",
                value: tiles.open,
                accent: "blue",
              },
              {
                labelKey: "common.overdue",
                value: tiles.overdue,
                accent: "red",
              },
              {
                labelKey: "qa.qms.capaRegister.tile.dueThisWeek",
                value: tiles.dueThisWeek,
                accent: "amber",
              },
              {
                labelKey: "qa.qms.capaRegister.tile.completed",
                value: tiles.completed,
                accent: "green",
              },
            ]}
          />

          <div className="qi-dashboard__controls">
            <Dropdown
              id="capa-status-filter"
              titleText={intl.formatMessage({
                id: "common.status",
              })}
              label=""
              items={statusItems}
              selectedItem={statusFilter}
              itemToString={(item) =>
                item
                  ? intl.formatMessage({
                      id:
                        item === "all"
                          ? "common.all"
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
                        fromDate: toLocalIsoDate(dates[0]),
                        toDate: toLocalIsoDate(dates[1]),
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
              <QASimpleTable rows={rows} headers={HEADERS} />
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
