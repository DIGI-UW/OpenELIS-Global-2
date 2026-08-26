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
  DataTableSkeleton,
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
import { useHistory, useLocation } from "react-router-dom";
import {
  getAnalyzers,
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
  type AnalyzerFilters,
  type AnalyzersResponse,
  type AnalyzerTypeCatalog,
} from "../../../services/analyzerService";
import AnalyzerLifecycleModal, {
  type AnalyzerLifecycleAction,
} from "../AnalyzerLifecycleModal/AnalyzerLifecycleModal";
import AnalyzerSetup, {
  type AnalyzerSetupStep,
} from "../AnalyzerSetup/AnalyzerSetup";

import PageBreadCrumb from "../../common/PageBreadCrumb";
import type { Analyzer, AnalyzerStatus } from "../types";
import "./AnalyzersList.css";

interface AnalyzerStats {
  total: number;
  active: number;
  setup: number;
  inactive: number;
  pluginWarnings: number;
}

interface AnalyzerTableRow {
  id: string;
  name: string;
  connection: string;
  labUnits: string;
  type: string;
  status: AnalyzerStatus;
  actions: string;
  _analyzer: Analyzer;
}

const profileRevisionKey = (profileId: string, revision: number) =>
  `${profileId}@${revision}`;

const hasPluginWarning = (analyzer: Analyzer) =>
  analyzer.profileBindingStatus !== "PINNED" && analyzer.pluginLoaded === false;

const isAnalyzerSetupStep = (
  value: string | null,
): value is AnalyzerSetupStep =>
  value === "instrument" || value === "verify" || value === "connect";

const isAnalyzerLifecycleAction = (
  value: string | null,
): value is AnalyzerLifecycleAction =>
  value === "deactivate" || value === "reactivate";

const lifecycleActionsFor = (
  status: AnalyzerStatus,
): AnalyzerLifecycleAction[] => {
  if (status === "INACTIVE") {
    return ["reactivate"];
  }
  if (status === "ERROR_PENDING" || status === "OFFLINE") {
    return ["reactivate", "deactivate"];
  }
  return ["deactivate"];
};

const AnalyzersList = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [analyzers, setAnalyzers] = useState<Analyzer[]>([]);
  const [filteredAnalyzers, setFilteredAnalyzers] = useState<Analyzer[]>([]);
  const [profileNames, setProfileNames] = useState<Record<
    string,
    string
  > | null>(null);
  const [labUnitNames, setLabUnitNames] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [filters, setFilters] = useState<AnalyzerFilters>({
    status: "",
    testUnit: "",
    analyzerType: "",
  });
  const [stats, setStats] = useState<AnalyzerStats>({
    total: 0,
    active: 0,
    setup: 0,
    inactive: 0,
    pluginWarnings: 0,
  });
  const queryParams = new URLSearchParams(location.search);
  const setupStep = queryParams.get("setup");
  const visibleSetupStep = isAnalyzerSetupStep(setupStep) ? setupStep : null;
  const setupAnalyzerId = queryParams.get("analyzerId");
  const lifecycleActionParam = queryParams.get("lifecycle");
  const lifecycleAction = isAnalyzerLifecycleAction(lifecycleActionParam)
    ? lifecycleActionParam
    : null;
  const lifecycleAnalyzerId = queryParams.get("lifecycleAnalyzerId");
  const lifecycleAnalyzer = lifecycleAnalyzerId
    ? analyzers.find((analyzer) => analyzer.id === lifecycleAnalyzerId) || null
    : null;
  const listSearch = queryParams.get("search") || "";
  const listStatus = queryParams.get("status") || "";
  const listTestUnit = queryParams.get("testUnit") || "";
  const listAnalyzerType = queryParams.get("analyzerType") || "";

  const openSetup = () => {
    const params = new URLSearchParams(location.search);
    ["analyzerId", "profile", "revision"].forEach((key) => params.delete(key));
    params.set("setup", "instrument");
    history.push({ pathname: "/analyzers", search: params.toString() });
  };

  const openExistingSetup = (analyzer: Analyzer, step: AnalyzerSetupStep) => {
    if (!analyzer.id) {
      return;
    }

    const params = new URLSearchParams(location.search);
    params.set("setup", step);
    params.set("analyzerId", analyzer.id);

    if (
      analyzer.profileId &&
      Number.isInteger(Number(analyzer.profileRevision)) &&
      Number(analyzer.profileRevision) >= 1
    ) {
      params.set("profile", analyzer.profileId);
      params.set("revision", String(analyzer.profileRevision));
    } else {
      params.delete("profile");
      params.delete("revision");
    }

    history.push({ pathname: "/analyzers", search: params.toString() });
  };

  const openQualityControl = (analyzer: Analyzer) => {
    if (!analyzer.id) {
      return;
    }

    const returnTo = `${location.pathname}${location.search}`;
    const params = new URLSearchParams({ returnTo });
    history.push({
      pathname: `/analyzers/qc/instruments/${encodeURIComponent(analyzer.id)}`,
      search: params.toString(),
    });
  };

  const openResults = (analyzer: Analyzer) => {
    if (!analyzer.id) {
      return;
    }

    history.push({
      pathname: "/AnalyzerResults",
      search: new URLSearchParams({ id: analyzer.id }).toString(),
    });
  };

  const openLifecycle = (
    analyzer: Analyzer,
    action: AnalyzerLifecycleAction,
  ) => {
    if (!analyzer.id) {
      return;
    }

    const params = new URLSearchParams(location.search);
    params.set("lifecycle", action);
    params.set("lifecycleAnalyzerId", analyzer.id);
    history.push({ pathname: "/analyzers", search: params.toString() });
  };

  const closeLifecycle = () => {
    const params = new URLSearchParams(location.search);
    params.delete("lifecycle");
    params.delete("lifecycleAnalyzerId");
    history.replace({ pathname: "/analyzers", search: params.toString() });
  };

  const loadAnalyzers = useCallback(
    (
      searchFilters: AnalyzerFilters = {},
      signal: AbortSignal | null = null,
    ) => {
      setLoading(true);
      getAnalyzers(
        searchFilters,
        (data: AnalyzersResponse | undefined) => {
          const list =
            data && Array.isArray(data.analyzers) ? data.analyzers : [];
          setAnalyzers(list);
          setFilteredAnalyzers(list);

          // Calculate statistics based on unified status
          const activeCount = list.filter((a) => a.status === "ACTIVE").length;
          const setupCount = list.filter(
            (a) => a.status === "SETUP" || a.status === "VALIDATION",
          ).length;
          const inactiveCount = list.filter(
            (a) => a.status === "INACTIVE",
          ).length;
          const pluginWarningCount = list.filter(hasPluginWarning).length;
          setStats({
            total: list.length,
            active: activeCount,
            setup: setupCount,
            inactive: inactiveCount,
            pluginWarnings: pluginWarningCount,
          });
          setLoading(false);
        },
        signal,
      );
    },
    [],
  );

  const closeSetup = () => {
    const params = new URLSearchParams(location.search);
    ["setup", "analyzerId", "profile", "revision"].forEach((key) =>
      params.delete(key),
    );
    loadAnalyzers({
      status: listStatus,
      testUnit: listTestUnit,
      analyzerType: listAnalyzerType,
      ...(listSearch ? { search: listSearch } : {}),
    });
    history.push({ pathname: "/analyzers", search: params.toString() });
  };

  useEffect(() => {
    const controller = new AbortController();
    getAnalyzerTypeCatalog((data: AnalyzerTypeCatalog | undefined) => {
      const names = Object.fromEntries(
        (data?.types || []).map((type) => [
          profileRevisionKey(type.profileId, type.revision),
          type.displayName,
        ]),
      );
      setProfileNames(names);
    }, controller.signal);
    getAnalyzerLabUnits((units) => {
      setLabUnitNames(
        Object.fromEntries(
          (Array.isArray(units) ? units : []).map((unit) => [
            String(unit.id),
            unit.name,
          ]),
        ),
      );
    }, controller.signal);
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setSearchTerm(listSearch);
    const initialFilters = {
      status: listStatus,
      testUnit: listTestUnit,
      analyzerType: listAnalyzerType,
    };
    setFilters(initialFilters);
    loadAnalyzers(
      {
        ...initialFilters,
        ...(listSearch ? { search: listSearch } : {}),
      },
      controller.signal,
    );

    return () => controller.abort();
  }, [listAnalyzerType, listSearch, listStatus, listTestUnit, loadAnalyzers]);

  useEffect(() => {
    const storedScrollY = sessionStorage.getItem("analyzers.scrollY");
    if (storedScrollY) {
      try {
        window.scrollTo(0, parseInt(storedScrollY, 10));
      } catch {
        // ignore
      }
    }

    const onBeforeUnload = () => {
      sessionStorage.setItem("analyzers.scrollY", String(window.scrollY));
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
      window.removeEventListener("beforeunload", onBeforeUnload);
      sessionStorage.setItem("analyzers.scrollY", String(window.scrollY));
    };
  }, []);

  const handleSearch = (value: string) => {
    setSearchTerm(value);

    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    searchTimeoutRef.current = setTimeout(() => {
      const params = new URLSearchParams(location.search);
      if (value.trim()) {
        params.set("search", value.trim());
      } else {
        params.delete("search");
      }
      history.replace({
        pathname: location.pathname,
        search: params.toString(),
      });
    }, 300);
  };

  const handleFilterChange = (
    filterName: keyof AnalyzerFilters,
    value: string,
  ) => {
    const newFilters = { ...filters, [filterName]: value };
    setFilters(newFilters);
    const params = new URLSearchParams(location.search);
    if (value) {
      params.set(filterName, value);
    } else {
      params.delete(filterName);
    }
    history.push({
      pathname: location.pathname,
      search: params.toString(),
    });
  };

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "analyzer.table.header.name" }),
    },
    {
      key: "connection",
      header: intl.formatMessage({ id: "analyzer.table.header.connection" }),
    },
    {
      key: "labUnits",
      header: intl.formatMessage({ id: "analyzer.table.header.testUnits" }),
    },
    {
      key: "type",
      header: intl.formatMessage({ id: "analyzer.table.header.type" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "analyzer.table.header.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "analyzer.table.actions" }),
    },
  ];

  const rows: AnalyzerTableRow[] = filteredAnalyzers.map((analyzer) => {
    const connection = intl.formatMessage({
      id: analyzer.bridgeConnectionId
        ? "analyzer.connection.reference.configured"
        : "analyzer.connection.reference.missing",
    });

    const unifiedStatus = analyzer.status || "SETUP";
    const profileName =
      analyzer.profileId && analyzer.profileRevision && profileNames
        ? profileNames[
            profileRevisionKey(analyzer.profileId, analyzer.profileRevision)
          ]
        : undefined;

    return {
      id: analyzer.id || "",
      name: analyzer.name || "-",
      connection: connection,
      labUnits:
        analyzer.testUnitIds && analyzer.testUnitIds.length > 0
          ? analyzer.testUnitIds
              .map((id) => labUnitNames[String(id)] || String(id))
              .join(", ")
          : "-",
      type:
        profileName ||
        (analyzer.profileId
          ? intl.formatMessage({
              id:
                profileNames === null
                  ? "analyzer.table.type.loading"
                  : "analyzer.table.type.unavailable",
            })
          : analyzer.analyzerType || analyzer.type || "-"),
      status: unifiedStatus,
      actions: "",
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
          <PageBreadCrumb
            breadcrumbs={[
              { label: "home.label", link: "/" },
              {
                label: "analyzer.page.hierarchy.root",
                link: "/analyzers",
                isCurrentPage: true,
              },
            ]}
          />
          <h1>{intl.formatMessage({ id: "analyzer.list.title" })}</h1>
          <p className="analyzers-list-subtitle">
            {intl.formatMessage({ id: "analyzer.list.subtitle" })}
          </p>
        </div>
        <Button
          kind="primary"
          renderIcon={Add}
          data-testid="add-analyzer-button"
          onClick={openSetup}
        >
          {intl.formatMessage({ id: "analyzer.action.add" })}
        </Button>
      </div>

      {visibleSetupStep && (
        <AnalyzerSetup
          key={setupAnalyzerId || "new-analyzer"}
          currentStep={visibleSetupStep}
          onClose={closeSetup}
        />
      )}

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
          <Tile data-testid="stat-setup">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.stat.setup" })}
            </div>
            <div className="stat-value">{stats.setup}</div>
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
          {loading ? (
            <div data-testid="analyzers-loading">
              <DataTableSkeleton
                columnCount={headers.length}
                rowCount={5}
                showHeader={false}
                showToolbar={false}
              />
            </div>
          ) : (
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
                            {...getHeaderProps({ header })}
                            key={header.key}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const analyzer = filteredAnalyzers.find(
                          (a) => a.id === row.id,
                        );
                        const unifiedStatus = analyzer?.status || "SETUP";

                        return (
                          <TableRow
                            {...getRowProps({ row })}
                            key={row.id}
                            data-testid={`analyzer-row-${row.id}`}
                          >
                            {row.cells.map((cell) => {
                              const headerKey = cell.info.header;
                              let testId = null;
                              let cellContent = cell.value;

                              if (headerKey === "name") {
                                testId = `analyzer-name-${row.id}`;
                                if (analyzer && hasPluginWarning(analyzer)) {
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
                              } else if (headerKey === "labUnits") {
                                testId = `analyzer-lab-units-${row.id}`;
                              } else if (headerKey === "status") {
                                testId = `analyzer-status-${row.id}`;
                                const statusColorMap: Record<
                                  AnalyzerStatus,
                                  "gray" | "blue" | "green" | "red" | "purple"
                                > = {
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
                              } else if (headerKey === "actions") {
                                testId = `analyzer-actions-${row.id}`;
                                cellContent = analyzer ? (
                                  <OverflowMenu
                                    flipped
                                    aria-label={intl.formatMessage({
                                      id: "analyzer.table.actions",
                                    })}
                                    iconDescription={intl.formatMessage({
                                      id: "analyzer.table.actions",
                                    })}
                                    data-testid={`analyzer-row-overflow-${row.id}`}
                                  >
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzer.action.viewResults",
                                      })}
                                      onClick={() => openResults(analyzer)}
                                      data-testid={`analyzer-action-view-results-${row.id}`}
                                    />
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzer.action.editSetup",
                                      })}
                                      onClick={() =>
                                        openExistingSetup(
                                          analyzer,
                                          "instrument",
                                        )
                                      }
                                      data-testid={`analyzer-action-edit-setup-${row.id}`}
                                    />
                                    {analyzer.profileId &&
                                      Number.isInteger(
                                        Number(analyzer.profileRevision),
                                      ) &&
                                      Number(analyzer.profileRevision) >= 1 && (
                                        <OverflowMenuItem
                                          itemText={intl.formatMessage({
                                            id: "analyzer.action.configureConnection",
                                          })}
                                          onClick={() =>
                                            openExistingSetup(
                                              analyzer,
                                              "connect",
                                            )
                                          }
                                          data-testid={`analyzer-action-configure-connection-${row.id}`}
                                        />
                                      )}
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzer.action.qualityControl",
                                      })}
                                      onClick={() =>
                                        openQualityControl(analyzer)
                                      }
                                      data-testid={`analyzer-action-quality-control-${row.id}`}
                                    />
                                    {lifecycleActionsFor(unifiedStatus).map(
                                      (action) => (
                                        <OverflowMenuItem
                                          key={action}
                                          itemText={intl.formatMessage({
                                            id: `analyzer.action.${action}`,
                                          })}
                                          onClick={() =>
                                            openLifecycle(analyzer, action)
                                          }
                                          data-testid={`analyzer-action-${action}-${row.id}`}
                                        />
                                      ),
                                    )}
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
          )}
        </Column>
      </Grid>

      {lifecycleAction && lifecycleAnalyzer && (
        <AnalyzerLifecycleModal
          key={`${lifecycleAction}-${lifecycleAnalyzer.id}`}
          action={lifecycleAction}
          analyzer={lifecycleAnalyzer}
          open
          onClose={closeLifecycle}
          onConfirm={() => loadAnalyzers()}
        />
      )}
    </div>
  );
};

export default AnalyzersList;
