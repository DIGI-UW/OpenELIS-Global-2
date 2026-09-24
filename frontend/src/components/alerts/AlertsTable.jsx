import React from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Button,
  Pagination,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { serverPageArrowsProps } from "../utils/serverPaging";
import ServerPageArrows from "../common/ServerPageArrows";

const SEVERITY_TAG_MAP = {
  CRITICAL: "red",
  WARNING: "warm-gray",
};

const STATUS_TAG_MAP = {
  OPEN: "red",
  ACKNOWLEDGED: "blue",
  RESOLVED: "green",
};

const AlertsTable = ({
  alerts,
  totalCount,
  page,
  pageSize,
  onPageChange,
  onAcknowledge,
}) => {
  const intl = useIntl();

  // The endpoint pages by page + pageSize, so the arrows above the table and
  // Carbon's pagination below it move through the same pages.
  const arrows = serverPageArrowsProps({
    paging: {
      currentPage: page + 1,
      totalPages: Math.max(Math.ceil((totalCount || 0) / (pageSize || 1)), 1),
    },
    onPageRequest: (pageNumber) => onPageChange(pageNumber - 1, pageSize),
  });

  const headers = [
    {
      key: "alertType",
      header: intl.formatMessage({ id: "alerts.table.type" }),
    },
    {
      key: "severity",
      header: intl.formatMessage({ id: "alerts.table.severity" }),
    },
    {
      key: "message",
      header: intl.formatMessage({ id: "alerts.table.message" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "alerts.table.status" }),
    },
    {
      key: "startTime",
      header: intl.formatMessage({ id: "alerts.table.created" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "alerts.table.actions" }),
    },
  ];

  const formatAlertType = (type) => {
    const key = `alerts.type.${(type || "").toLowerCase()}`;
    return intl.formatMessage({ id: key, defaultMessage: type });
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return dateStr;
    }
  };

  const rows = (alerts || []).map((alert) => ({
    id: String(alert.id),
    alertType: formatAlertType(alert.alertType),
    severity: alert.severity,
    message: alert.message,
    status: alert.status,
    startTime: formatDate(alert.startTime),
    actions: alert.status === "OPEN" ? "acknowledge" : "",
    _original: alert,
  }));

  return (
    <>
      {arrows.show && <ServerPageArrows {...arrows} />}
      <DataTable rows={rows} headers={headers}>
        {({
          rows: tableRows,
          headers: tableHeaders,
          getTableProps,
          getHeaderProps,
          getRowProps,
        }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {tableHeaders.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {tableRows.map((row) => {
                const original = (alerts || []).find(
                  (a) => String(a.id) === row.id,
                );
                return (
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => {
                      if (cell.info.header === "severity") {
                        return (
                          <TableCell key={cell.id}>
                            <Tag
                              type={SEVERITY_TAG_MAP[cell.value] || "gray"}
                              size="sm"
                            >
                              {intl.formatMessage({
                                id: `alerts.severity.${(cell.value || "").toLowerCase()}`,
                                defaultMessage: cell.value,
                              })}
                            </Tag>
                          </TableCell>
                        );
                      }
                      if (cell.info.header === "status") {
                        return (
                          <TableCell key={cell.id}>
                            <Tag
                              type={STATUS_TAG_MAP[cell.value] || "gray"}
                              size="sm"
                            >
                              {intl.formatMessage({
                                id: `alerts.status.${(cell.value || "").toLowerCase()}`,
                                defaultMessage: cell.value,
                              })}
                            </Tag>
                          </TableCell>
                        );
                      }
                      if (cell.info.header === "actions") {
                        return (
                          <TableCell key={cell.id}>
                            {original && original.status === "OPEN" && (
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => onAcknowledge(original)}
                              >
                                {intl.formatMessage({
                                  id: "alerts.acknowledge.button",
                                })}
                              </Button>
                            )}
                          </TableCell>
                        );
                      }
                      return <TableCell key={cell.id}>{cell.value}</TableCell>;
                    })}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </DataTable>
      {totalCount > 0 && (
        <Pagination
          totalItems={totalCount}
          page={page + 1}
          pageSize={pageSize}
          pageSizes={[25, 50, 100, 200]}
          onChange={({ page: newPage, pageSize: newPageSize }) => {
            onPageChange(newPage - 1, newPageSize);
          }}
        />
      )}
    </>
  );
};

export default AlertsTable;
