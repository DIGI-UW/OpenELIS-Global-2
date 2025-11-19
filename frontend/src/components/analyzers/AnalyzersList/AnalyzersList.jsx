import React, { useState, useEffect, useRef, useCallback } from "react";
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
  Grid,
  Column,
  Tile,
  Button,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { getAnalyzers } from "../../../services/analyzerService";
import AnalyzerForm from "../AnalyzerForm/AnalyzerForm";
import "./AnalyzersList.css";

const AnalyzersList = () => {
  const intl = useIntl();
  const history = useHistory();
  const searchTimeoutRef = useRef(null);

  // State
  const [analyzers, setAnalyzers] = useState([]);
  const [filteredAnalyzers, setFilteredAnalyzers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [filters, setFilters] = useState({
    status: "",
    testUnit: "",
    analyzerType: "",
  });
  const [stats, setStats] = useState({
    total: 0,
    active: 0,
    inactive: 0,
  });
  const [analyzerFormOpen, setAnalyzerFormOpen] = useState(false);
  const [selectedAnalyzer, setSelectedAnalyzer] = useState(null);

  // Load analyzers from API
  const loadAnalyzers = useCallback((searchFilters = {}) => {
    setLoading(true);
    getAnalyzers(searchFilters, (data) => {
      if (Array.isArray(data)) {
        setAnalyzers(data);
        setFilteredAnalyzers(data);

        // Calculate statistics
        const activeCount = data.filter(
          (a) => a.active === true || a.active === "true",
        ).length;
        const inactiveCount = data.length - activeCount;
        setStats({
          total: data.length,
          active: activeCount,
          inactive: inactiveCount,
        });
      } else {
        setAnalyzers([]);
        setFilteredAnalyzers([]);
        setStats({ total: 0, active: 0, inactive: 0 });
      }
      setLoading(false);
    });
  }, []);

  // Initial load + restore state from URL/sessionStorage
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const initialSearch = params.get("search") || "";
    const initialStatus = params.get("status") || "";
    const initialTestUnit = params.get("testUnit") || "";
    const initialAnalyzerType = params.get("analyzerType") || "";

    setSearchTerm(initialSearch);
    const initialFilters = {
      status: initialStatus,
      testUnit: initialTestUnit,
      analyzerType: initialAnalyzerType,
    };
    setFilters(initialFilters);
    loadAnalyzers({
      ...initialFilters,
      ...(initialSearch ? { search: initialSearch } : {}),
    });

    // Restore scroll position (session)
    const storedScrollY = sessionStorage.getItem("analyzers.scrollY");
    if (storedScrollY) {
      try {
        window.scrollTo(0, parseInt(storedScrollY, 10));
      } catch (_) {
        // ignore
      }
    }

    // Persist scroll position on unload
    const onBeforeUnload = () => {
      sessionStorage.setItem("analyzers.scrollY", String(window.scrollY));
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      sessionStorage.setItem("analyzers.scrollY", String(window.scrollY));
    };
  }, [loadAnalyzers]);

  // Search handler with debounce
  const handleSearch = (value) => {
    setSearchTerm(value);

    // Clear existing timeout
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    // Set new timeout for debounced search
    searchTimeoutRef.current = setTimeout(() => {
      const searchFilters = { ...filters };
      if (value.trim()) {
        searchFilters.search = value.trim();
      }
      loadAnalyzers(searchFilters);
      // Update URL
      const params = new URLSearchParams(window.location.search);
      if (value.trim()) {
        params.set("search", value.trim());
      } else {
        params.delete("search");
      }
      history.replace({ search: params.toString() });
    }, 300);
  };

  // Filter handlers
  const handleFilterChange = (filterName, value) => {
    const newFilters = { ...filters, [filterName]: value };
    setFilters(newFilters);
    loadAnalyzers(newFilters);
    // Update URL
    const params = new URLSearchParams(window.location.search);
    if (value) {
      params.set(filterName, value);
    } else {
      params.delete(filterName);
    }
    history.replace({ search: params.toString() });
  };

  // Table headers
  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "analyzer.table.header.name" }),
    },
    {
      key: "type",
      header: intl.formatMessage({ id: "analyzer.table.header.type" }),
    },
    {
      key: "connection",
      header: intl.formatMessage({ id: "analyzer.table.header.connection" }),
    },
    {
      key: "testUnits",
      header: intl.formatMessage({ id: "analyzer.table.header.testUnits" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "analyzer.table.header.status" }),
    },
    {
      key: "lastModified",
      header: intl.formatMessage({ id: "analyzer.table.header.lastModified" }),
    },
    { key: "actions", header: "" },
  ];

  // Format analyzer data for table rows (Carbon DataTable format)
  const rows = filteredAnalyzers.map((analyzer) => {
    const connection =
      analyzer.ipAddress && analyzer.port
        ? `${analyzer.ipAddress}:${analyzer.port}`
        : "-";

    const isActive = analyzer.active === true || analyzer.active === "true";

    return {
      id: analyzer.id,
      name: analyzer.name || "-",
      type: analyzer.analyzerType || analyzer.type || "-",
      connection: connection,
      testUnits:
        analyzer.testUnitIds && analyzer.testUnitIds.length > 0
          ? `${analyzer.testUnitIds.length} unit(s)`
          : "-",
      status: isActive ? "Active" : "Inactive",
      lastModified: analyzer.lastModified
        ? new Date(analyzer.lastModified).toLocaleDateString()
        : "-",
      _analyzer: analyzer, // Store full analyzer object for actions (prefixed with _ to avoid conflicts)
    };
  });

  return (
    <div className="analyzers-list" data-testid="analyzers-list">
      {/* Header */}
      <div
        className="analyzers-list-header"
        data-testid="analyzers-list-header"
      >
        <h1>{intl.formatMessage({ id: "analyzer.list.title" })}</h1>
        <Button
          data-testid="add-analyzer-button"
          onClick={() => {
            setSelectedAnalyzer(null);
            setAnalyzerFormOpen(true);
          }}
        >
          {intl.formatMessage({ id: "analyzer.action.add" })}
        </Button>
      </div>

      {/* Statistics Cards */}
      <Grid className="analyzers-list-stats" data-testid="analyzers-list-stats">
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="stat-total">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.stat.total" })}
            </div>
            <div className="stat-value">{stats.total}</div>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="stat-active">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.stat.active" })}
            </div>
            <div className="stat-value">{stats.active}</div>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile data-testid="stat-inactive">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.stat.inactive" })}
            </div>
            <div className="stat-value">{stats.inactive}</div>
          </Tile>
        </Column>
      </Grid>

      {/* Search and Filters */}
      <div
        className="analyzers-list-filters"
        data-testid="analyzers-list-filters"
      >
        <Search
          data-testid="analyzer-search-input"
          placeholder={intl.formatMessage({
            id: "analyzer.search.placeholder",
          })}
          labelText={intl.formatMessage({ id: "analyzer.search.label" })}
          value={searchTerm}
          onChange={(e) => handleSearch(e.target.value)}
          size="lg"
        />
        {/* TODO: Add filter dropdowns for status, testUnit, analyzerType */}
      </div>

      {/* DataTable */}
      <TableContainer data-testid="analyzers-table-container">
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
            <Table {...getTableProps()} data-testid="analyzers-table">
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
                {rows.map((row) => {
                  const analyzer =
                    row._analyzer ||
                    filteredAnalyzers.find((a) => a.id === row.id);
                  const isActive =
                    analyzer?.active === true || analyzer?.active === "true";

                  return (
                    <TableRow
                      key={row.id}
                      {...getRowProps({ row })}
                      data-testid={`analyzer-row-${row.id}`}
                    >
                      {row.cells.map((cell, index) => {
                        // Map cell headers to testids
                        const headerKey = cell.info.header;
                        let testId = null;
                        let cellContent = cell.value;

                        if (headerKey === "name") {
                          testId = `analyzer-name-${row.id}`;
                        } else if (headerKey === "type") {
                          testId = `analyzer-type-${row.id}`;
                        } else if (headerKey === "connection") {
                          testId = `analyzer-connection-${row.id}`;
                        } else if (headerKey === "testUnits") {
                          testId = `analyzer-test-units-${row.id}`;
                        } else if (headerKey === "status") {
                          testId = `analyzer-status-${row.id}`;
                          cellContent = (
                            <Tag type={isActive ? "green" : "gray"}>
                              {isActive ? "Active" : "Inactive"}
                            </Tag>
                          );
                        } else if (headerKey === "lastModified") {
                          testId = `analyzer-last-modified-${row.id}`;
                        } else if (headerKey === "actions") {
                          testId = `analyzer-actions-${row.id}`;
                          cellContent = analyzer ? (
                            <OverflowMenu
                              ariaLabel={intl.formatMessage({
                                id: "analyzer.table.actions",
                              })}
                            >
                              <OverflowMenuItem
                                itemText={intl.formatMessage({
                                  id: "analyzer.action.fieldMappings",
                                })}
                                onClick={() =>
                                  history.push(
                                    `/analyzers/${analyzer.id}/mappings`,
                                  )
                                }
                                data-testid={`analyzer-action-mappings-${row.id}`}
                              />
                              <OverflowMenuItem
                                itemText={intl.formatMessage({
                                  id: "analyzer.action.edit",
                                })}
                                onClick={() => {
                                  setSelectedAnalyzer(analyzer);
                                  setAnalyzerFormOpen(true);
                                }}
                                data-testid={`analyzer-action-edit-${row.id}`}
                              />
                              <OverflowMenuItem
                                itemText={intl.formatMessage({
                                  id: "analyzer.action.delete",
                                })}
                                isDelete
                                onClick={() => {
                                  // TODO: Implement delete
                                }}
                                data-testid={`analyzer-action-delete-${row.id}`}
                              />
                            </OverflowMenu>
                          ) : null;
                        }

                        return (
                          <TableCell key={cell.id} data-testid={testId}>
                            {cellContent}
                          </TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </DataTable>
      </TableContainer>

      {/* AnalyzerForm Modal */}
      {analyzerFormOpen && (
        <AnalyzerForm
          analyzer={selectedAnalyzer}
          open={analyzerFormOpen}
          onClose={() => {
            setAnalyzerFormOpen(false);
            setSelectedAnalyzer(null);
            loadAnalyzers(); // Reload list after form closes
          }}
        />
      )}
    </div>
  );
};

export default AnalyzersList;
