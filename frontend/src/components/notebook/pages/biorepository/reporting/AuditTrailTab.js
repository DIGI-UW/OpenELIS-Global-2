import React, { useState, useEffect, useRef } from "react";
import {
  Grid,
  Column,
  Loading,
  InlineNotification,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Search,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Button,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Reset } from "@carbon/icons-react";
import PropTypes from "prop-types";
import config from "../../../../../config.json";

/**
 * AuditTrailTab - Immutable chain of custody log viewer
 *
 * Displays searchable, filterable audit trail of all custody changes.
 * Features:
 * - Search by sample barcode/accession number
 * - Filter by custody action type
 * - Filter by date range
 * - Server-side pagination
 * - Expandable rows for full details
 */
function AuditTrailTab({ entryId, notebookId, pageData }) {
  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [auditLogs, setAuditLogs] = useState([]);
  const [error, setError] = useState(null);
  const [actions, setActions] = useState([]);

  // Filter states
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedAction, setSelectedAction] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(50);
  const [totalCount, setTotalCount] = useState(0);

  // Helper function to format Date to YYYY-MM-DD
  const formatDateToISO = (date) => {
    if (!date) return "";
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  // Fetch custody action types
  useEffect(() => {
    fetch(`${config.serverBaseUrl}/rest/biorepository/custody/actions`, {
      credentials: "include",
    })
      .then((r) => r.json())
      .then((data) => setActions(data))
      .catch((err) => console.error("Failed to load action types:", err));
  }, []);

  // Fetch audit logs
  const fetchAuditLogs = (page, filters = {}) => {
    setIsLoading(true);
    setError(null);

    const params = new URLSearchParams();
    params.append("page", page - 1); // Convert to 0-indexed
    params.append("pageSize", pageSize);

    if (filters.searchQuery)
      params.append("sampleExternalId", filters.searchQuery);
    if (filters.action && filters.action !== "ALL")
      params.append("action", filters.action);
    // Always format dates to ISO string
    if (filters.startDate) {
      const formattedStart =
        typeof filters.startDate === "string"
          ? filters.startDate
          : formatDateToISO(filters.startDate);
      if (formattedStart) params.append("startDate", formattedStart);
    }
    if (filters.endDate) {
      const formattedEnd =
        typeof filters.endDate === "string"
          ? filters.endDate
          : formatDateToISO(filters.endDate);
      if (formattedEnd) params.append("endDate", formattedEnd);
    }

    fetch(
      `${config.serverBaseUrl}/rest/biorepository/custody/search?${params}`,
      {
        credentials: "include",
      },
    )
      .then((r) => {
        if (!r.ok) throw new Error("Failed to load audit trail");
        return r.json();
      })
      .then((response) => {
        setAuditLogs(response.data || []);
        setTotalCount(response.totalCount || 0);
        setIsLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setIsLoading(false);
      });
  };

  // Store current filters in a ref to access in useEffect
  const currentFiltersRef = React.useRef({
    searchQuery: "",
    selectedAction: "ALL",
    startDate: "",
    endDate: "",
  });

  useEffect(() => {
    fetchAuditLogs(currentPage, currentFiltersRef.current);
  }, [currentPage, pageSize]);

  const handleApplyFilters = () => {
    // Update the filters ref
    currentFiltersRef.current = {
      searchQuery,
      action: selectedAction,
      startDate,
      endDate,
    };
    // Reset to page 1 and fetch
    setCurrentPage(1);
    fetchAuditLogs(1, currentFiltersRef.current);
  };

  const handleResetFilters = () => {
    setSearchQuery("");
    setSelectedAction("ALL");
    setStartDate("");
    setEndDate("");
    // Update the filters ref
    currentFiltersRef.current = {
      searchQuery: "",
      action: "ALL",
      startDate: "",
      endDate: "",
    };
    // Reset to page 1 and fetch
    setCurrentPage(1);
    fetchAuditLogs(1, currentFiltersRef.current);
  };

  if (isLoading && auditLogs.length === 0) {
    return (
      <div style={{ padding: "2rem" }}>
        <Loading
          description={intl.formatMessage({
            id: "biorepository.reporting.audit.loading",
            defaultMessage: "Loading audit trail...",
          })}
        />
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: "2rem" }}>
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.reporting.audit.error",
            defaultMessage: "Error Loading Audit Trail",
          })}
          subtitle={error}
          lowContrast
        />
      </div>
    );
  }

  // Map action to color
  const getActionTag = (action) => {
    const tagMap = {
      CHECKOUT_REQUESTED: { type: "blue", label: "Checkout Requested" },
      CHECKOUT_APPROVED: { type: "green", label: "Checkout Approved" },
      CHECKOUT_RETRIEVED: { type: "cyan", label: "Checkout Retrieved" },
      CHECKOUT_RELEASED: { type: "teal", label: "Checkout Released" },
      TRANSFER_INITIATED: { type: "magenta", label: "Transfer Initiated" },
      TRANSFER_IN_TRANSIT: { type: "purple", label: "Transfer In Transit" },
      TRANSFER_RECEIVED: { type: "cyan", label: "Transfer Received" },
      RETURN_INITIATED: { type: "blue", label: "Return Initiated" },
      RETURN_RECEIVED: { type: "green", label: "Return Received" },
      RETURN_INSPECTED: { type: "teal", label: "Return Inspected" },
      RETURN_STORED: { type: "purple", label: "Return Stored" },
    };

    const tag = tagMap[action] || { type: "gray", label: action };
    return <Tag type={tag.type}>{tag.label}</Tag>;
  };

  // DataTable rows
  const rows = auditLogs.map((log) => ({
    id: log.id.toString(),
    timestamp: new Date(log.actionTimestamp).toLocaleString(),
    sampleId: log.sampleExternalId || log.accessionNumber || "N/A",
    action: log.custodyAction,
    custodian: log.toCustodianName || log.fromCustodianName || "System",
    fromLocation: log.fromLocation || "-",
    toLocation: log.toLocation || "-",
    temperature: log.temperature ? `${log.temperature}°C` : "-",
    notes: log.notes || "-",
  }));

  const headers = [
    { key: "timestamp", header: "Timestamp" },
    { key: "sampleId", header: "Sample Barcode" },
    { key: "action", header: "Custody Action" },
    { key: "custodian", header: "Custodian" },
    { key: "fromLocation", header: "From Location" },
    { key: "toLocation", header: "To Location" },
  ];

  return (
    <div className="audit-trail-tab" style={{ padding: "1.5rem" }}>
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.reporting.audit.title"
              defaultMessage="Chain of Custody Audit Trail"
            />
          </h4>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              marginBottom: "1.5rem",
            }}
          >
            <FormattedMessage
              id="biorepository.reporting.audit.description"
              defaultMessage="Complete immutable audit trail of all sample custody changes. Search by barcode, filter by action type or date range."
            />
          </p>
        </Column>

        {/* Filters */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
          <div
            style={{
              display: "flex",
              gap: "1rem",
              alignItems: "flex-end",
              flexWrap: "wrap",
            }}
          >
            <Search
              id="audit-search"
              labelText={intl.formatMessage({
                id: "biorepository.reporting.audit.search",
                defaultMessage: "Search by sample barcode or accession number",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.reporting.audit.searchPlaceholder",
                defaultMessage: "Enter barcode...",
              })}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              size="md"
              style={{ minWidth: "250px" }}
            />

            <Select
              id="action-filter"
              labelText={intl.formatMessage({
                id: "biorepository.reporting.audit.actionFilter",
                defaultMessage: "Custody Action",
              })}
              value={selectedAction}
              onChange={(e) => setSelectedAction(e.target.value)}
              size="md"
              style={{ minWidth: "200px" }}
            >
              <SelectItem value="ALL" text="All Actions" />
              {actions.map((action) => (
                <SelectItem
                  key={action.value}
                  value={action.value}
                  text={action.label}
                />
              ))}
            </Select>

            <DatePicker
              datePickerType="single"
              value={startDate}
              onChange={(dates) => setStartDate(dates[0] || "")}
            >
              <DatePickerInput
                id="start-date"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "biorepository.reporting.audit.startDate",
                  defaultMessage: "Start Date",
                })}
                size="md"
              />
            </DatePicker>

            <DatePicker
              datePickerType="single"
              value={endDate}
              onChange={(dates) => setEndDate(dates[0] || "")}
            >
              <DatePickerInput
                id="end-date"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "biorepository.reporting.audit.endDate",
                  defaultMessage: "End Date",
                })}
                size="md"
              />
            </DatePicker>

            <Button kind="primary" onClick={handleApplyFilters} size="md">
              <FormattedMessage
                id="biorepository.reporting.audit.apply"
                defaultMessage="Apply Filters"
              />
            </Button>

            <Button
              kind="secondary"
              onClick={handleResetFilters}
              renderIcon={Reset}
              size="md"
            >
              <FormattedMessage
                id="biorepository.reporting.audit.reset"
                defaultMessage="Reset"
              />
            </Button>
          </div>
        </Column>

        {/* Results Summary */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
          <p style={{ fontSize: "0.875rem", color: "#525252" }}>
            <FormattedMessage
              id="biorepository.reporting.audit.resultsCount"
              defaultMessage="Showing {start}-{end} of {total} custody records"
              values={{
                start:
                  auditLogs.length > 0 ? (currentPage - 1) * pageSize + 1 : 0,
                end: Math.min(currentPage * pageSize, totalCount),
                total: totalCount,
              }}
            />
          </p>
        </Column>

        {/* Audit Trail Table */}
        <Column lg={16} md={8} sm={4}>
          <DataTable rows={rows} headers={headers}>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer title="">
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
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "action"
                              ? getActionTag(cell.value)
                              : cell.value}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>

          {/* Pagination */}
          <Pagination
            backwardText={intl.formatMessage({
              id: "biorepository.reporting.audit.previousPage",
              defaultMessage: "Previous page",
            })}
            forwardText={intl.formatMessage({
              id: "biorepository.reporting.audit.nextPage",
              defaultMessage: "Next page",
            })}
            itemsPerPageText={intl.formatMessage({
              id: "biorepository.reporting.audit.itemsPerPage",
              defaultMessage: "Items per page:",
            })}
            page={currentPage}
            pageSize={pageSize}
            pageSizes={[25, 50, 100]}
            totalItems={totalCount}
            onChange={({ page, pageSize: newPageSize }) => {
              if (newPageSize !== pageSize) {
                setPageSize(newPageSize);
                setCurrentPage(1);
              } else {
                setCurrentPage(page);
              }
            }}
          />
        </Column>
      </Grid>
    </div>
  );
}

AuditTrailTab.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
};

export default AuditTrailTab;
