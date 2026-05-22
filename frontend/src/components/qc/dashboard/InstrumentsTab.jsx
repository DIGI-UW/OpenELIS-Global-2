/**
 * InstrumentsTab Component
 *
 * Displays a DataTable of all instruments with their QC compliance status.
 * Features client-side search and pagination.
 *
 * Columns: Instrument ID, Name, Type, Location, Status, Analytes,
 *          Recent Violations, Last Update, Actions
 */

import React, { useState, useMemo } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Search,
  Tag,
  Pagination,
  Button,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getComplianceTagType,
  getComplianceLabelKey,
  getZScoreBadgeType,
  formatTimestamp,
} from "./qcDashboardUtils";
import { useHistory } from "react-router-dom";
import "./InstrumentsTab.css";

const InstrumentsTab = ({ instruments = [], loading }) => {
  const intl = useIntl();
  const history = useHistory();

  const [searchTerm, setSearchTerm] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Client-side search filtering
  const filteredInstruments = useMemo(() => {
    if (!searchTerm) return instruments;
    const term = searchTerm.toLowerCase();
    return instruments.filter(
      (inst) =>
        (inst.instrumentId || "").toLowerCase().includes(term) ||
        (inst.instrumentName || "").toLowerCase().includes(term) ||
        (inst.instrumentType || "").toLowerCase().includes(term) ||
        (inst.instrumentLocation || "").toLowerCase().includes(term),
    );
  }, [instruments, searchTerm]);

  // Paginated subset
  const paginatedInstruments = useMemo(() => {
    const start = (page - 1) * pageSize;
    return filteredInstruments.slice(start, start + pageSize);
  }, [filteredInstruments, page, pageSize]);

  // Table headers
  const headers = [
    {
      key: "instrumentName",
      header: intl.formatMessage({ id: "qc.dashboard.instruments.col.name" }),
    },
    {
      key: "instrumentType",
      header: intl.formatMessage({ id: "qc.dashboard.instruments.col.type" }),
    },
    {
      key: "instrumentLocation",
      header: intl.formatMessage({
        id: "qc.dashboard.instruments.col.location",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "qc.dashboard.instruments.col.status",
      }),
    },
    {
      key: "analytes",
      header: intl.formatMessage({
        id: "qc.dashboard.instruments.col.analytes",
      }),
    },
    {
      key: "violations",
      header: intl.formatMessage({
        id: "qc.dashboard.instruments.col.violations",
      }),
    },
    {
      key: "lastUpdate",
      header: intl.formatMessage({
        id: "qc.dashboard.instruments.col.lastUpdate",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "qc.dashboard.instruments.col.actions",
      }),
    },
  ];

  // Format rows for DataTable
  const rows = paginatedInstruments.map((inst) => ({
    id: String(inst.instrumentId || inst.id),
    instrumentName: inst.instrumentName || "-",
    instrumentType: inst.instrumentType || "-",
    instrumentLocation: inst.instrumentLocation || "-",
    status: inst.complianceColor || "GREEN",
    analytes: inst.analyteDetails || [],
    violations: inst.triggeredRuleDetails || [],
    lastUpdate: inst.lastResultTime || "",
    actions: inst.instrumentId || inst.id,
  }));

  const handleViewInstrument = (instrumentId) => {
    const inst = instruments.find(
      (i) => String(i.instrumentId || i.id) === String(instrumentId),
    );
    history.push(`/analyzers/qc/instruments/${inst.instrumentId}`);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    setPage(1);
  };

  const handlePaginationChange = ({ page: newPage, pageSize: newPageSize }) => {
    setPage(newPage);
    setPageSize(newPageSize);
  };

  return (
    <div className="instruments-tab" data-testid="instruments-tab">
      <div className="instruments-tab__header">
        <div>
          <h4 className="instruments-tab__title">
            {intl.formatMessage({ id: "qc.dashboard.instruments.title" })}
          </h4>
          <p className="instruments-tab__subtitle">
            {intl.formatMessage({ id: "qc.dashboard.instruments.subtitle" })}
          </p>
        </div>
      </div>

      <Search
        className="instruments-tab__search"
        placeholder={intl.formatMessage({
          id: "qc.dashboard.instruments.search",
        })}
        labelText={intl.formatMessage({
          id: "qc.dashboard.instruments.search",
        })}
        value={searchTerm}
        onChange={handleSearchChange}
        data-testid="instruments-search"
      />

      <TableContainer data-testid="instruments-table-container">
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
            <Table {...getTableProps()} data-testid="instruments-table">
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
                {rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={headers.length}>
                      <div className="instruments-tab__empty">
                        {intl.formatMessage({
                          id: "qc.dashboard.instruments.empty",
                        })}
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  rows.map((row) => {
                    const instrument = paginatedInstruments.find(
                      (inst) => String(inst.instrumentId || inst.id) === row.id,
                    );

                    return (
                      <TableRow
                        key={row.id}
                        {...getRowProps({ row })}
                        data-testid={`instrument-row-${row.id}`}
                      >
                        {row.cells.map((cell) => {
                          let cellContent = cell.value;

                          if (cell.info.header === "status") {
                            cellContent = (
                              <Tag type={getComplianceTagType(cell.value)}>
                                {intl.formatMessage({
                                  id: getComplianceLabelKey(cell.value),
                                })}
                              </Tag>
                            );
                          } else if (cell.info.header === "analytes") {
                            const analyteList = cell.value || [];
                            cellContent =
                              analyteList.length > 0 ? (
                                <div className="instruments-tab__analytes">
                                  {analyteList.map((analyte, idx) => (
                                    <span
                                      key={idx}
                                      className="instruments-tab__analyte"
                                    >
                                      {analyte.testName}
                                      {analyte.latestZScore != null && (
                                        <Tag
                                          type={getZScoreBadgeType(
                                            analyte.latestZScore,
                                          )}
                                          size="sm"
                                        >
                                          {Math.abs(
                                            parseFloat(analyte.latestZScore),
                                          ).toFixed(1)}
                                          &sigma;
                                        </Tag>
                                      )}
                                    </span>
                                  ))}
                                </div>
                              ) : (
                                "-"
                              );
                          } else if (cell.info.header === "violations") {
                            const ruleList = cell.value || [];
                            cellContent =
                              ruleList.length > 0 ? (
                                <div className="instruments-tab__violations">
                                  {ruleList.map((rule, idx) => (
                                    <Tag key={idx} type="red" size="sm">
                                      {rule.ruleCode || rule}
                                    </Tag>
                                  ))}
                                </div>
                              ) : (
                                <span className="instruments-tab__no-violations">
                                  {intl.formatMessage({
                                    id: "qc.dashboard.instruments.noViolations",
                                  })}
                                </span>
                              );
                          } else if (cell.info.header === "lastUpdate") {
                            cellContent = formatTimestamp(cell.value);
                          } else if (cell.info.header === "actions") {
                            cellContent = (
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => handleViewInstrument(cell.value)}
                                data-testid={`instrument-view-${row.id}`}
                              >
                                {intl.formatMessage({
                                  id: "qc.dashboard.instruments.view",
                                })}
                              </Button>
                            );
                          }

                          return (
                            <TableCell key={cell.id}>{cellContent}</TableCell>
                          );
                        })}
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          )}
        </DataTable>
      </TableContainer>

      {filteredInstruments.length > 0 && (
        <Pagination
          totalItems={filteredInstruments.length}
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 25, 50]}
          onChange={handlePaginationChange}
          data-testid="instruments-pagination"
        />
      )}
    </div>
  );
};

export default InstrumentsTab;
