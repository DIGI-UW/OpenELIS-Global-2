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
  Dropdown,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { getAnalyzers } from "../../../services/analyzerService";
import AnalyzerForm from "../AnalyzerForm/AnalyzerForm";
import TestConnectionModal from "../TestConnectionModal/TestConnectionModal";
import DeleteAnalyzerModal from "../DeleteAnalyzerModal/DeleteAnalyzerModal";
import CopyMappingsModal from "../FieldMapping/CopyMappingsModal";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./AnalyzersList.css";

const AnalyzersList = () => {
  const intl = useIntl();
  const history = useHistory();
  const searchTimeoutRef = useRef(null);

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
    pluginWarnings: 0,
  });
  const [analyzerFormOpen, setAnalyzerFormOpen] = useState(false);
  const [selectedAnalyzer, setSelectedAnalyzer] = useState(null);
  const [testConnectionModal, setTestConnectionModal] = useState({
    open: false,
    analyzer: null,
  });
  const [deleteModal, setDeleteModal] = useState({
    open: false,
    analyzer: null,
  });
  const [copyMappingsModal, setCopyMappingsModal] = useState({
    open: false,
    analyzer: null,
  });

  const loadAnalyzers = useCallback((searchFilters = {}) => {
    setLoading(true);
    getAnalyzers(searchFilters, (data) => {
      const list = data && Array.isArray(data.analyzers) ? data.analyzers : [];
      setAnalyzers(list);
      setFilteredAnalyzers(list);

      // Calculate statistics based on unified status
      const activeCount = list.filter((a) => a.status === "ACTIVE").length;
      const inactiveCount = list.filter((a) => a.status === "INACTIVE").length;
      const pluginWarningCount = list.filter(
        (a) => a.pluginLoaded === false,
      ).length;
      setStats({
        total: list.length,
        active: activeCount,
        inactive: inactiveCount,
        pluginWarnings: pluginWarningCount,
      });
      setLoading(false);
    });
  }, []);

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

    const storedScrollY = sessionStorage.getItem("analyzers.scrollY");
    if (storedScrollY) {
      try {
        window.scrollTo(0, parseInt(storedScrollY, 10));
      } catch (_) {
        // ignore
      }
    }

    const onBeforeUnload = () => {
      sessionStorage.setItem("analyzers.scrollY", String(window.scrollY));
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      sessionStorage.setItem("analyzers.scrollY", String(window.scrollY));
    };
  }, [loadAnalyzers]);

  const handleSearch = (value) => {
    setSearchTerm(value);

    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    searchTimeoutRef.current = setTimeout(() => {
      const searchFilters = { ...filters };
      if (value.trim()) {
        searchFilters.search = value.trim();
      }
      loadAnalyzers(searchFilters);
      const params = new URLSearchParams(window.location.search);
      if (value.trim()) {
        params.set("search", value.trim());
      } else {
        params.delete("search");
      }
      history.replace({ search: params.toString() });
    }, 300);
  };

  const handleFilterChange = (filterName, value) => {
    const newFilters = { ...filters, [filterName]: value };
    setFilters(newFilters);
    loadAnalyzers(newFilters);
    const params = new URLSearchParams(window.location.search);
    if (value) {
      params.set(filterName, value);
    } else {
      params.delete(filterName);
    }
    history.replace({ search: params.toString() });
  };

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
    {
      key: "actions",
      header: intl.formatMessage({ id: "analyzer.table.actions" }),
    },
  ];

  const rows = filteredAnalyzers.map((analyzer) => {
    const connection =
      analyzer.ipAddress && analyzer.port
        ? `${analyzer.ipAddress}:${analyzer.port}`
        : "-";

    const unifiedStatus = analyzer.status || "SETUP";

    return {
      id: analyzer.id,
      name: analyzer.name || "-",
      type: analyzer.analyzerType || analyzer.type || "-",
      connection: connection,
      testUnits:
        analyzer.testUnitIds && analyzer.testUnitIds.length > 0
          ? `${analyzer.testUnitIds.length} unit(s)`
          : "-",
      status: unifiedStatus,
      lastModified: analyzer.lastModified
        ? new Date(analyzer.lastModified).toLocaleDateString()
        : "-",
      _analyzer: analyzer, // Store full analyzer object for actions (prefixed with _ to avoid conflicts)
    };
  });

  return (
    <div className="analyzers-list" data-testid="analyzers-list">
      <div
        className="analyzers-list-header"
        data-testid="analyzers-list-header"
      >
        <div className="analyzers-list-header-title">
          <PageTitle
            breadcrumbs={[
              {
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.root",
                }),
              },
              {
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.list",
                }),
              },
            ]}
            subtitle={intl.formatMessage({ id: "analyzer.list.subtitle" })}
          />
        </div>
        <Button
          kind="primary"
          renderIcon={Add}
          data-testid="add-analyzer-button"
          onClick={() => {
            setSelectedAnalyzer(null);
            setAnalyzerFormOpen(true);
          }}
        >
          {intl.formatMessage({ id: "analyzer.action.add" })}
        </Button>
      </div>

      <Grid className="analyzers-list-stats" data-testid="analyzers-list-stats">
        <Column lg={4} md={2} sm={2}>
          <Tile data-testid="stat-total">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.stat.total" })}
            </div>
            <div className="stat-value">{stats.total}</div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Tile data-testid="stat-active">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.stat.active" })}
            </div>
            <div className="stat-value">{stats.active}</div>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Tile data-testid="stat-inactive">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.stat.inactive" })}
            </div>
            <div className="stat-value">{stats.inactive}</div>
          </Tile>
        </Column>
        {stats.pluginWarnings > 0 && (
          <Column lg={4} md={2} sm={2}>
            <Tile data-testid="stat-plugin-warnings">
              <div className="stat-label">
                {intl.formatMessage({ id: "analyzer.stat.pluginWarnings" })}
              </div>
              <div className="stat-value stat-value--warning">
                {stats.pluginWarnings}
              </div>
            </Tile>
          </Column>
        )}
      </Grid>

      <div
        className="analyzers-list-filters"
        data-testid="analyzers-list-filters"
      >
        <Grid>
          <Column lg={16} md={8} sm={4}>
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
          </Column>
        </Grid>
        <Grid>
          <Column lg={4} md={4} sm={4}>
            <Dropdown
              id="status-filter"
              data-testid="analyzer-status-filter"
              titleText={intl.formatMessage({
                id: "analyzer.filter.status.label",
              })}
              label={intl.formatMessage({
                id: "analyzer.filter.status.label",
              })}
              items={[
                {
                  id: "",
                  text: intl.formatMessage({
                    id: "analyzer.filter.status.all",
                  }),
                },
                {
                  id: "INACTIVE",
                  text: intl.formatMessage({
                    id: "analyzer.status.inactive",
                  }),
                },
                {
                  id: "SETUP",
                  text: intl.formatMessage({
                    id: "analyzer.status.setup",
                  }),
                },
                {
                  id: "VALIDATION",
                  text: intl.formatMessage({
                    id: "analyzer.status.validation",
                  }),
                },
                {
                  id: "ACTIVE",
                  text: intl.formatMessage({
                    id: "analyzer.status.active",
                  }),
                },
                {
                  id: "ERROR_PENDING",
                  text: intl.formatMessage({
                    id: "analyzer.status.error_pending",
                  }),
                },
                {
                  id: "OFFLINE",
                  text: intl.formatMessage({
                    id: "analyzer.status.offline",
                  }),
                },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                filters.status
                  ? {
                      id: filters.status,
                      text: intl.formatMessage({
                        id:
                          filters.status === "ERROR_PENDING"
                            ? "analyzer.status.error_pending"
                            : `analyzer.status.${filters.status.toLowerCase()}`,
                      }),
                    }
                  : {
                      id: "",
                      text: intl.formatMessage({
                        id: "analyzer.filter.status.all",
                      }),
                    }
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  handleFilterChange("status", selectedItem.id || "");
                }
              }}
              size="lg"
            />
          </Column>
        </Grid>
      </div>

      <Grid>
        <Column lg={16} md={8} sm={4}>
          <TableContainer
            data-testid="analyzers-table-container"
            className="analyzers-list-table-container"
          >
            <DataTable rows={rows} headers={headers} isSortable>
              {({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getTableProps,
              }) => (
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
                      const unifiedStatus =
                        analyzer?.status || row.status || "SETUP";

                      return (
                        <TableRow
                          key={row.id}
                          {...getRowProps({ row })}
                          data-testid={`analyzer-row-${row.id}`}
                        >
                          {row.cells.map((cell, index) => {
                            const headerKey = cell.info.header;
                            let testId = null;
                            let cellContent = cell.value;

                            if (headerKey === "name") {
                              testId = `analyzer-name-${row.id}`;
                              if (analyzer?.pluginLoaded === false) {
                                cellContent = (
                                  <span>
                                    {cell.value}{" "}
                                    <Tag
                                      type="red"
                                      size="sm"
                                      data-testid={`plugin-warning-${row.id}`}
                                    >
                                      {intl.formatMessage({
                                        id: "analyzer.plugin.missing",
                                      })}
                                    </Tag>
                                  </span>
                                );
                              }
                            } else if (headerKey === "type") {
                              testId = `analyzer-type-${row.id}`;
                            } else if (headerKey === "connection") {
                              testId = `analyzer-connection-${row.id}`;
                            } else if (headerKey === "testUnits") {
                              testId = `analyzer-test-units-${row.id}`;
                            } else if (headerKey === "status") {
                              testId = `analyzer-status-${row.id}`;
                              const statusColorMap = {
                                INACTIVE: "gray",
                                SETUP: "gray",
                                VALIDATION: "blue",
                                ACTIVE: "green",
                                ERROR_PENDING: "red", // Carbon doesn't support "orange", use "red" for error states
                                OFFLINE: "red",
                              };
                              const statusColor =
                                statusColorMap[unifiedStatus] || "gray";
                              // Convert ERROR_PENDING to error_pending for i18n key
                              const statusKey =
                                unifiedStatus === "ERROR_PENDING"
                                  ? "analyzer.status.error_pending"
                                  : `analyzer.status.${unifiedStatus.toLowerCase()}`;
                              cellContent = (
                                <Tag
                                  type={statusColor}
                                  data-testid={`status-badge-${row.id}`}
                                >
                                  {intl.formatMessage({
                                    id: statusKey,
                                  })}
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
                                    onClick={() => {
                                      if (analyzer?.id) {
                                        history.push(
                                          `/analyzers/${analyzer.id}/mappings`,
                                        );
                                      }
                                    }}
                                    data-testid={`analyzer-action-mappings-${row.id}`}
                                  />
                                  <OverflowMenuItem
                                    itemText={intl.formatMessage({
                                      id: "analyzer.action.testConnection",
                                    })}
                                    onClick={() => {
                                      setTestConnectionModal({
                                        open: true,
                                        analyzer: analyzer,
                                      });
                                    }}
                                    data-testid={`analyzer-action-test-connection-${row.id}`}
                                  />
                                  <OverflowMenuItem
                                    itemText={intl.formatMessage({
                                      id: "analyzer.action.copyMappings",
                                    })}
                                    onClick={() => {
                                      setCopyMappingsModal({
                                        open: true,
                                        analyzer: analyzer,
                                      });
                                    }}
                                    data-testid={`analyzer-action-copy-mappings-${row.id}`}
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
                                      setDeleteModal({
                                        open: true,
                                        analyzer: analyzer,
                                      });
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
        </Column>
      </Grid>

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

      {testConnectionModal.open && (
        <TestConnectionModal
          analyzer={testConnectionModal.analyzer}
          open={testConnectionModal.open}
          onClose={() => {
            setTestConnectionModal({ open: false, analyzer: null });
          }}
        />
      )}

      {deleteModal.open && (
        <DeleteAnalyzerModal
          analyzer={deleteModal.analyzer}
          open={deleteModal.open}
          onClose={() => {
            setDeleteModal({ open: false, analyzer: null });
          }}
          onConfirm={(deletedId) => {
            loadAnalyzers();
          }}
        />
      )}

      {copyMappingsModal.open && copyMappingsModal.analyzer && (
        <CopyMappingsModal
          open={copyMappingsModal.open}
          sourceAnalyzerId={copyMappingsModal.analyzer.id}
          sourceAnalyzerName={copyMappingsModal.analyzer.name}
          sourceAnalyzerType={
            copyMappingsModal.analyzer.analyzerType ||
            copyMappingsModal.analyzer.type
          }
          onClose={() => {
            setCopyMappingsModal({ open: false, analyzer: null });
          }}
          onSuccess={(result, targetAnalyzerId) => {}}
        />
      )}
    </div>
  );
};

export default AnalyzersList;
