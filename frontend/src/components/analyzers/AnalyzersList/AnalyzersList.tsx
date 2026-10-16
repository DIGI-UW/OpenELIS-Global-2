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
  Button,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Dropdown,
  Callout,
  InlineNotification,
} from "@carbon/react";
import { Add, CheckmarkFilled, WarningFilled } from "@carbon/icons-react";
import { Link as RouterLink } from "react-router-dom";
import AnalyzerPageHeader from "../AnalyzerPageHeader";
import AnalyzerCountChips from "../AnalyzerCountChips";
import { useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import {
  getAnalyzerDeliveryIssues,
  getAnalyzers,
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
  type AnalyzerFilters,
  type AnalyzersResponse,
  type AnalyzerTypeCatalog,
} from "../../../services/analyzerService";
import AnalyzerLifecycleModal from "../AnalyzerLifecycleModal/AnalyzerLifecycleModal";
import AnalyzerSetup, {
  type AnalyzerSetupStep,
} from "../AnalyzerSetup/AnalyzerSetup";
import BridgePairing from "../BridgePairing/BridgePairing";
import {
  analyzerResultsPath,
  importIssuesPath,
} from "../../analyserResults/analyzerResultsRoutes.jsx";

import type { Analyzer, AnalyzerStatus } from "../types";
import "./AnalyzersList.css";

interface AnalyzerStats {
  total: number;
  active: number;
  setup: number;
  needsAttention: number;
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

const hasHeldResults = (analyzer: Analyzer) =>
  Number(analyzer.heldResultCount || 0) > 0;

const isAnalyzerSetupStep = (
  value: string | null,
): value is AnalyzerSetupStep =>
  value === "instrument" ||
  value === "assays" ||
  value === "verify" ||
  value === "connect";

// Setup's own address; the rest of the list's address survives it.
const SETUP_PARAMS = [
  "setup",
  "analyzerId",
  "profile",
  "revision",
  "name",
  "labUnits",
];

// What setup's Connect step did, said on the list it returns to.
interface AnalyzerSetupNotice {
  kind: "activated" | "saved" | "updated";
  name: string;
}

// Deactivate is confirmed in a dialog; Activate and Reactivate open setup's
// Connect step, the one place an analyzer is activated.
type AnalyzerLifecycleAction = "activate" | "deactivate" | "reactivate";

const lifecycleActionsFor = (
  status: AnalyzerStatus,
): AnalyzerLifecycleAction[] => {
  if (status === "SETUP") {
    return ["activate"];
  }
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
  const [setupNotice, setSetupNotice] = useState<AnalyzerSetupNotice | null>(
    null,
  );
  // The list comes back at setup's scroll position; its notice is at the top.
  useEffect(() => {
    if (setupNotice && document.scrollingElement) {
      document.scrollingElement.scrollTop = 0;
    }
  }, [setupNotice]);
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
    needsAttention: 0,
  });
  const queryParams = new URLSearchParams(location.search);
  const setupStep = queryParams.get("setup");
  const visibleSetupStep = isAnalyzerSetupStep(setupStep) ? setupStep : null;
  const setupAnalyzerId = queryParams.get("analyzerId");
  const lifecycleActionParam = queryParams.get("lifecycle");
  const lifecycleAction =
    lifecycleActionParam === "deactivate" ? lifecycleActionParam : null;
  const lifecycleAnalyzerId = queryParams.get("lifecycleAnalyzerId");
  const lifecycleAnalyzer = lifecycleAnalyzerId
    ? analyzers.find((analyzer) => analyzer.id === lifecycleAnalyzerId) || null
    : null;
  const listSearch = queryParams.get("search") || "";
  const listStatus = queryParams.get("status") || "";
  const listTestUnit = queryParams.get("testUnit") || "";
  const listAnalyzerType = queryParams.get("analyzerType") || "";
  const firstAttentionAnalyzer = analyzers.find(hasHeldResults);
  const [undeliveredCount, setUndeliveredCount] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    getAnalyzerDeliveryIssues((response) => {
      setUndeliveredCount(
        response?.status === "success" ? response.data?.count || 0 : 0,
      );
    }, controller.signal);
    return () => controller.abort();
  }, []);

  const openSetup = () => {
    const params = new URLSearchParams(location.search);
    SETUP_PARAMS.forEach((key) => params.delete(key));
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

    history.push(analyzerResultsPath(analyzer.id));
  };

  const openLifecycle = (
    analyzer: Analyzer,
    action: AnalyzerLifecycleAction,
  ) => {
    if (!analyzer.id) {
      return;
    }
    if (action !== "deactivate") {
      openExistingSetup(analyzer, "connect");
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
          const needsAttentionCount = list.filter(hasHeldResults).length;
          setStats({
            total: list.length,
            active: activeCount,
            setup: setupCount,
            needsAttention: needsAttentionCount,
          });
          setLoading(false);
        },
        signal,
      );
    },
    [],
  );

  const closeSetup = (notice?: AnalyzerSetupNotice) => {
    setSetupNotice(notice ?? null);
    const params = new URLSearchParams(location.search);
    SETUP_PARAMS.forEach((key) => params.delete(key));
    loadAnalyzers({
      status: listStatus,
      testUnit: listTestUnit,
      analyzerType: listAnalyzerType,
      ...(listSearch ? { search: listSearch } : {}),
    });
    // Replace, not push: Back from the list must not reopen the setup.
    history.replace({ pathname: "/analyzers", search: params.toString() });
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
      const currentLocation = history.location;
      const params = new URLSearchParams(currentLocation.search);
      if (value.trim()) {
        params.set("search", value.trim());
      } else {
        params.delete("search");
      }
      history.replace({
        pathname: currentLocation.pathname,
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
        ? "analyzer.connection.state.configured"
        : "analyzer.connection.state.missing",
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

  if (visibleSetupStep) {
    return (
      <div className="analyzers-list" data-testid="analyzers-list">
        <AnalyzerSetup
          key={setupAnalyzerId || "new-analyzer"}
          currentStep={visibleSetupStep}
          onClose={closeSetup}
        />
      </div>
    );
  }

  return (
    <div className="analyzers-list" data-testid="analyzers-list">
      <AnalyzerPageHeader
        testId="analyzers-list-header"
        breadcrumbs={[
          { label: "home.label", link: "/" },
          {
            label: "analyzer.page.hierarchy.root",
            link: "/analyzers",
            isCurrentPage: true,
          },
        ]}
        title={intl.formatMessage({ id: "analyzer.list.title" })}
        subtitle={intl.formatMessage({ id: "analyzer.list.subtitle" })}
        actions={
          <Button
            kind="primary"
            renderIcon={Add}
            data-testid="add-analyzer-button"
            onClick={openSetup}
          >
            {intl.formatMessage({ id: "analyzer.action.add" })}
          </Button>
        }
      />

      {setupNotice && (
        <div className="analyzers-list__notice">
          <InlineNotification
            kind="success"
            lowContrast
            data-testid="analyzer-setup-notice"
            title={intl.formatMessage(
              { id: `analyzer.list.notice.${setupNotice.kind}` },
              { name: setupNotice.name },
            )}
            subtitle={
              setupNotice.kind === "saved"
                ? intl.formatMessage({
                    id: "analyzer.list.notice.saved.detail",
                  })
                : undefined
            }
            onCloseButtonClick={() => setSetupNotice(null)}
          />
        </div>
      )}

      <BridgePairing />

      {firstAttentionAnalyzer && (
        <Callout
          kind="warning"
          lowContrast
          data-testid="held-results-attention"
          title={intl.formatMessage(
            {
              id:
                stats.needsAttention === 1
                  ? "analyzer.attention.title.one"
                  : "analyzer.attention.title.many",
            },
            {
              name: firstAttentionAnalyzer.name,
              count: stats.needsAttention,
            },
          )}
          subtitle={intl.formatMessage({
            id: "analyzer.attention.subtitle",
          })}
          actionButtonLabel={intl.formatMessage({
            id: "analyzer.attention.review",
          })}
          onActionButtonClick={() => openResults(firstAttentionAnalyzer)}
        />
      )}

      {undeliveredCount > 0 && (
        <Callout
          kind="warning"
          lowContrast
          data-testid="delivery-issues-attention"
          title={intl.formatMessage(
            { id: "analyzer.deliveryIssues.attention.title" },
            { count: undeliveredCount },
          )}
          subtitle={intl.formatMessage({
            id: "analyzer.deliveryIssues.attention.subtitle",
          })}
          actionButtonLabel={intl.formatMessage({
            id: "analyzer.deliveryIssues.attention.review",
          })}
          onActionButtonClick={() => history.push(importIssuesPath)}
        />
      )}

      <AnalyzerCountChips
        testId="analyzers-list-stats"
        label={intl.formatMessage({ id: "analyzer.stat.label" })}
        chips={[
          { id: "total", value: stats.total, status: "" },
          { id: "active", value: stats.active, status: "ACTIVE" },
          { id: "setup", value: stats.setup, status: "SETUP" },
        ]
          .map((stat) => ({
            id: stat.id,
            value: stat.value,
            label: intl.formatMessage({ id: `analyzer.stat.${stat.id}` }),
            current: stat.status === (filters.status || ""),
            testId: `stat-${stat.id}`,
            onSelect: () => handleFilterChange("status", stat.status),
          }))
          .concat({
            id: "needs-attention",
            value: stats.needsAttention,
            label: intl.formatMessage({ id: "analyzer.stat.needsAttention" }),
            warning: stats.needsAttention > 0,
            testId: "stat-needs-attention",
            onSelect: () =>
              firstAttentionAnalyzer && openResults(firstAttentionAnalyzer),
          })}
      />

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
                                cellContent = analyzer?.id ? (
                                  <RouterLink
                                    className="analyzers-list-name"
                                    to={analyzerResultsPath(analyzer.id)}
                                  >
                                    {cell.value}
                                  </RouterLink>
                                ) : (
                                  cell.value
                                );
                              } else if (headerKey === "connection") {
                                testId = `analyzer-connection-${row.id}`;
                                const configured = Boolean(
                                  analyzer?.bridgeConnectionId,
                                );
                                cellContent = (
                                  <span className="analyzers-list-indicator">
                                    {configured ? (
                                      <CheckmarkFilled
                                        className="analyzers-list-indicator__icon analyzers-list-indicator__icon--ok"
                                        aria-hidden="true"
                                      />
                                    ) : (
                                      <WarningFilled
                                        className="analyzers-list-indicator__icon analyzers-list-indicator__icon--warn"
                                        aria-hidden="true"
                                      />
                                    )}
                                    {cell.value}
                                  </span>
                                );
                              } else if (headerKey === "type") {
                                testId = `analyzer-type-${row.id}`;
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
                                  <span className="analyzers-list-status">
                                    <Tag
                                      type={statusColor}
                                      data-testid={`status-badge-${row.id}`}
                                    >
                                      {intl.formatMessage({
                                        id: statusKey,
                                      })}
                                    </Tag>
                                    {analyzer && hasHeldResults(analyzer) && (
                                      <RouterLink
                                        className="analyzers-list-held"
                                        to={analyzerResultsPath(
                                          analyzer.id || "",
                                        )}
                                        data-testid={`held-results-tag-${row.id}`}
                                      >
                                        <WarningFilled
                                          className="analyzers-list-indicator__icon analyzers-list-indicator__icon--warn"
                                          aria-hidden="true"
                                        />
                                        {intl.formatMessage(
                                          { id: "analyzer.status.held" },
                                          { count: analyzer.heldResultCount },
                                        )}
                                      </RouterLink>
                                    )}
                                  </span>
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
