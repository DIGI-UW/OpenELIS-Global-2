import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  DataTable,
  Grid,
  InlineNotification,
  Loading,
  OverflowMenu,
  OverflowMenuItem,
  Search,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Tile,
} from "@carbon/react";
import { Add, Copy } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getAnalyzerTypeCatalog } from "../../../services/analyzerService";
import AnalyzerTypeLifecycleModals from "./AnalyzerTypeLifecycleModals";
import "./AnalyzerTypeManagement.scss";

const SOURCE_VALUES = new Set(["ALL", "SHIPPED", "SITE"]);
const PROTOCOL_VALUES = new Set(["ALL", "ASTM", "HL7", "FILE"]);
const MAPPING_VALUES = new Set(["ALL", "COMPLETE", "INCOMPLETE"]);

const readEnum = (params, key, allowed) => {
  const value = params.get(key) || "ALL";
  return allowed.has(value) ? value : "ALL";
};

const readFilters = (search) => {
  const params = new URLSearchParams(search);
  return {
    query: params.get("q") || "",
    source: readEnum(params, "source", SOURCE_VALUES),
    protocol: readEnum(params, "protocol", PROTOCOL_VALUES),
    mapping: readEnum(params, "mapping", MAPPING_VALUES),
    showDeactivated: params.get("showDeactivated") === "true",
  };
};

const getMappingState = (type) => {
  const summaries = [type.testMappings, type.resultMappings].filter(
    (summary) => summary && summary.state !== "NOT_APPLICABLE",
  );
  return summaries.every(
    (summary) =>
      summary.state === "COMPLETE" && summary.mapped === summary.total,
  )
    ? "COMPLETE"
    : "INCOMPLETE";
};

const SummaryTile = ({ label, value }) => (
  <Tile className="analyzer-type-summary__tile">
    <div className="analyzer-type-summary__label">{label}</div>
    <div className="analyzer-type-summary__value">{value}</div>
  </Tile>
);

const AnalyzerTypeManagement = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const filters = useMemo(
    () => readFilters(location.search),
    [location.search],
  );
  const [catalog, setCatalog] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [notification, setNotification] = useState(null);
  const actionState = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return {
      action: params.get("action"),
      profileId: params.get("profile"),
      draftId: params.get("draft"),
    };
  }, [location.search]);

  const fetchAnalyzerTypes = useCallback(() => {
    getAnalyzerTypeCatalog((data) => {
      if (!data || !Array.isArray(data.types) || !data.summary) {
        setCatalog(null);
        setLoadError(true);
      } else {
        setCatalog(data);
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    fetchAnalyzerTypes();
  }, [fetchAnalyzerTypes]);

  const retryLoad = () => {
    setLoading(true);
    setLoadError(false);
    fetchAnalyzerTypes();
  };

  const closeAction = useCallback(() => {
    const params = new URLSearchParams(location.search);
    params.delete("action");
    params.delete("profile");
    params.delete("draft");
    history.push({
      pathname: location.pathname,
      search: params.toString() ? `?${params.toString()}` : "",
    });
  }, [history, location.pathname, location.search]);

  const handleDraftCreated = useCallback(
    (draftId) => {
      const params = new URLSearchParams(location.search);
      params.set("draft", draftId);
      history.push({
        pathname: location.pathname,
        search: `?${params.toString()}`,
      });
    },
    [history, location.pathname, location.search],
  );

  const handleActionSuccess = useCallback(
    (action) => {
      closeAction();
      setNotification({
        kind: "success",
        titleId: `analyzerType.notification.${action}.success`,
        detail: "",
      });
      fetchAnalyzerTypes();
    },
    [closeAction, fetchAnalyzerTypes],
  );

  const handleActionError = useCallback(
    (detail) => {
      closeAction();
      setNotification({
        kind: "error",
        titleId: "analyzerType.notification.action.error",
        detail:
          detail ||
          intl.formatMessage({
            id: "analyzerType.notification.action.error.subtitle",
          }),
      });
    },
    [closeAction, intl],
  );

  const updateQuery = useCallback(
    (key, value, replace = false) => {
      const params = new URLSearchParams(location.search);
      const isDefault =
        value === "" ||
        value === "ALL" ||
        value === false ||
        value === undefined;
      if (isDefault) {
        params.delete(key);
      } else {
        params.set(key, String(value));
      }
      const nextLocation = {
        pathname: location.pathname,
        search: params.toString() ? `?${params.toString()}` : "",
      };
      if (replace) {
        history.replace(nextLocation);
      } else {
        history.push(nextLocation);
      }
    },
    [history, location.pathname, location.search],
  );

  const visibleTypes = useMemo(() => {
    const types = catalog?.types || [];
    const query = filters.query.trim().toLowerCase();
    return types.filter((type) => {
      if (!filters.showDeactivated && type.status === "INACTIVE") {
        return false;
      }
      if (
        query &&
        ![type.displayName, type.manufacturer, type.model, type.profileId]
          .filter(Boolean)
          .some((value) => value.toLowerCase().includes(query))
      ) {
        return false;
      }
      if (filters.source !== "ALL" && type.source !== filters.source) {
        return false;
      }
      if (filters.protocol !== "ALL" && type.protocol !== filters.protocol) {
        return false;
      }
      return (
        filters.mapping === "ALL" || getMappingState(type) === filters.mapping
      );
    });
  }, [catalog, filters]);

  const headers = [
    {
      key: "displayName",
      header: intl.formatMessage({ id: "analyzerType.column.type" }),
    },
    {
      key: "protocol",
      header: intl.formatMessage({ id: "analyzerType.column.protocol" }),
    },
    {
      key: "testMappings",
      header: intl.formatMessage({ id: "analyzerType.column.testsMapped" }),
    },
    {
      key: "resultMappings",
      header: intl.formatMessage({ id: "analyzerType.column.resultsMapped" }),
    },
    {
      key: "usedBy",
      header: intl.formatMessage({ id: "analyzerType.column.usedBy" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "analyzerType.column.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "analyzerType.column.actions" }),
    },
  ];

  const rows = visibleTypes.map((type) => ({
    id: type.profileId,
    displayName: type.displayName,
    protocol: type.protocol,
    testMappings: type.testMappings,
    resultMappings: type.resultMappings,
    usedBy: type.usedBy,
    status: type.status,
    actions: type.profileId,
    source: type.source,
    manufacturer: type.manufacturer,
    model: type.model,
    revision: type.revision,
    parentProfileId: type.parentProfileId,
    parentRevision: type.parentRevision,
  }));
  const typesById = new Map(
    (catalog?.types || []).map((type) => [type.profileId, type]),
  );

  const formatMappings = (summary) => {
    if (!summary || summary.state === "NOT_APPLICABLE") {
      return intl.formatMessage({ id: "analyzerType.mapping.notApplicable" });
    }
    if (summary.state === "NOT_STARTED") {
      return intl.formatMessage({ id: "analyzerType.mapping.notStarted" });
    }
    return intl.formatMessage(
      { id: "analyzerType.mapping.count" },
      { mapped: summary.mapped, total: summary.total },
    );
  };

  const openAction = (action, profileId) => {
    const params = new URLSearchParams(location.search);
    params.set("action", action);
    if (profileId) {
      params.set("profile", profileId);
    } else {
      params.delete("profile");
    }
    history.push({
      pathname: location.pathname,
      search: `?${params.toString()}`,
    });
  };

  const summary = catalog?.summary || {
    total: 0,
    inUse: 0,
    needsAttention: 0,
    deactivated: 0,
  };

  return (
    <>
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
          {
            label: "analyzerType.page.title",
            link: "/analyzers/types",
            isCurrentPage: true,
          },
        ]}
      />
      <Grid fullWidth className="analyzer-type-page">
        <Column lg={16} md={8} sm={4}>
          <div className="analyzer-type-page__heading">
            <div>
              <h1>
                <FormattedMessage id="analyzerType.page.title" />
              </h1>
              <p className="analyzer-type-page__subtitle">
                <FormattedMessage id="analyzerType.page.subtitle" />
              </p>
            </div>
            <div className="analyzer-type-page__actions">
              <Button
                kind="secondary"
                renderIcon={Copy}
                onClick={() => openAction("duplicate")}
              >
                <FormattedMessage id="analyzerType.button.duplicate" />
              </Button>
              <Button renderIcon={Add} onClick={() => openAction("create")}>
                <FormattedMessage id="analyzerType.button.create" />
              </Button>
            </div>
          </div>

          <InlineNotification
            className="analyzer-type-page__explainer"
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "analyzerType.explainer.title",
            })}
            subtitle={intl.formatMessage({
              id: "analyzerType.explainer.subtitle",
            })}
          />

          {notification && (
            <InlineNotification
              className="analyzer-type-page__notification"
              kind={notification.kind}
              lowContrast
              title={intl.formatMessage({
                id: notification.titleId,
              })}
              subtitle={notification.detail}
              onCloseButtonClick={() => setNotification(null)}
            />
          )}

          <section
            className="analyzer-type-summary"
            aria-label={intl.formatMessage({
              id: "analyzerType.summary.ariaLabel",
            })}
          >
            <SummaryTile
              label={intl.formatMessage({ id: "analyzerType.summary.total" })}
              value={summary.total}
            />
            <SummaryTile
              label={intl.formatMessage({ id: "analyzerType.summary.inUse" })}
              value={summary.inUse}
            />
            <SummaryTile
              label={intl.formatMessage({
                id: "analyzerType.summary.needsAttention",
              })}
              value={summary.needsAttention}
            />
            <SummaryTile
              label={intl.formatMessage({
                id: "analyzerType.summary.deactivated",
              })}
              value={summary.deactivated}
            />
          </section>

          <div className="analyzer-type-filters">
            <Search
              id="analyzer-type-search"
              size="lg"
              labelText={intl.formatMessage({
                id: "analyzerType.search.label",
              })}
              placeholder={intl.formatMessage({
                id: "analyzerType.search.placeholder",
              })}
              value={filters.query}
              onChange={(event) => updateQuery("q", event.target.value, true)}
            />
            <Select
              id="analyzer-type-source"
              aria-label={intl.formatMessage({
                id: "analyzerType.filter.source",
              })}
              labelText={intl.formatMessage({
                id: "analyzerType.filter.source",
              })}
              value={filters.source}
              onChange={(event) => updateQuery("source", event.target.value)}
            >
              <SelectItem
                value="ALL"
                text={intl.formatMessage({
                  id: "analyzerType.filter.all",
                })}
              />
              <SelectItem
                value="SITE"
                text={intl.formatMessage({
                  id: "analyzerType.source.site",
                })}
              />
              <SelectItem
                value="SHIPPED"
                text={intl.formatMessage({
                  id: "analyzerType.source.shipped",
                })}
              />
            </Select>
            <Select
              id="analyzer-type-protocol"
              aria-label={intl.formatMessage({
                id: "analyzerType.filter.protocol",
              })}
              labelText={intl.formatMessage({
                id: "analyzerType.filter.protocol",
              })}
              value={filters.protocol}
              onChange={(event) => updateQuery("protocol", event.target.value)}
            >
              <SelectItem
                value="ALL"
                text={intl.formatMessage({
                  id: "analyzerType.filter.all",
                })}
              />
              <SelectItem value="ASTM" text="ASTM" />
              <SelectItem value="HL7" text="HL7" />
              <SelectItem
                value="FILE"
                text={intl.formatMessage({
                  id: "analyzerType.protocol.file",
                })}
              />
            </Select>
            <Select
              id="analyzer-type-mapping"
              aria-label={intl.formatMessage({
                id: "analyzerType.filter.mapping",
              })}
              labelText={intl.formatMessage({
                id: "analyzerType.filter.mapping",
              })}
              value={filters.mapping}
              onChange={(event) => updateQuery("mapping", event.target.value)}
            >
              <SelectItem
                value="ALL"
                text={intl.formatMessage({
                  id: "analyzerType.filter.all",
                })}
              />
              <SelectItem
                value="INCOMPLETE"
                text={intl.formatMessage({
                  id: "analyzerType.mapping.incomplete",
                })}
              />
              <SelectItem
                value="COMPLETE"
                text={intl.formatMessage({
                  id: "analyzerType.mapping.complete",
                })}
              />
            </Select>
            <Checkbox
              id="analyzer-type-show-deactivated"
              aria-label={intl.formatMessage({
                id: "analyzerType.filter.showDeactivated",
              })}
              labelText={intl.formatMessage({
                id: "analyzerType.filter.showDeactivated",
              })}
              checked={filters.showDeactivated}
              onChange={(_, state) =>
                updateQuery("showDeactivated", state.checked)
              }
            />
          </div>

          {loadError && (
            <InlineNotification
              kind="error"
              lowContrast
              title={intl.formatMessage({
                id: "analyzerType.notification.loadError",
              })}
              subtitle={intl.formatMessage({
                id: "analyzerType.notification.loadError.subtitle",
              })}
              actionButtonLabel={intl.formatMessage({
                id: "analyzerType.button.retry",
              })}
              onActionButtonClick={retryLoad}
            />
          )}

          {loading ? (
            <div className="analyzer-type-page__loading">
              <Loading
                withOverlay={false}
                description={intl.formatMessage({
                  id: "analyzerType.loading",
                })}
              />
            </div>
          ) : (
            !loadError && (
              <DataTable rows={rows} headers={headers}>
                {({
                  rows: carbonRows,
                  headers: carbonHeaders,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
                    <Table {...getTableProps()} size="lg">
                      <TableHead>
                        <TableRow>
                          {carbonHeaders.map((header) => (
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
                        {carbonRows.length === 0 ? (
                          <TableRow>
                            <TableCell colSpan={headers.length}>
                              <InlineNotification
                                className="analyzer-type-page__empty"
                                kind="info"
                                lowContrast
                                hideCloseButton
                                title={intl.formatMessage({
                                  id: "analyzerType.empty.title",
                                })}
                                subtitle={intl.formatMessage({
                                  id: "analyzerType.empty.subtitle",
                                })}
                              />
                            </TableCell>
                          </TableRow>
                        ) : (
                          carbonRows.map((row) => {
                            const type = typesById.get(row.id);
                            return (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableCell>
                                  <div className="analyzer-type-name">
                                    <strong>{type.displayName}</strong>
                                    <div className="analyzer-type-name__meta">
                                      {[type.manufacturer, type.model]
                                        .filter(Boolean)
                                        .join(" · ")}
                                      {(type.manufacturer || type.model) &&
                                        " · "}
                                      <FormattedMessage
                                        id="analyzerType.revision"
                                        values={{ revision: type.revision }}
                                      />
                                    </div>
                                    <div className="analyzer-type-name__tags">
                                      <Tag
                                        size="sm"
                                        type={
                                          type.source === "SHIPPED"
                                            ? "blue"
                                            : "teal"
                                        }
                                      >
                                        <FormattedMessage
                                          id={
                                            type.source === "SHIPPED"
                                              ? "analyzerType.source.shipped"
                                              : "analyzerType.source.site"
                                          }
                                        />
                                      </Tag>
                                      {type.parentProfileId && (
                                        <span className="analyzer-type-name__lineage">
                                          <FormattedMessage
                                            id="analyzerType.lineage"
                                            values={{
                                              profileId: type.parentProfileId,
                                              revision: type.parentRevision,
                                            }}
                                          />
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                </TableCell>
                                <TableCell>{type.protocol}</TableCell>
                                <TableCell>
                                  <span
                                    className={
                                      getMappingState({
                                        testMappings: type.testMappings,
                                        resultMappings: {
                                          state: "NOT_APPLICABLE",
                                        },
                                      }) === "COMPLETE"
                                        ? "analyzer-type-mapping--complete"
                                        : "analyzer-type-mapping--incomplete"
                                    }
                                  >
                                    {formatMappings(type.testMappings)}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    className={
                                      !type.resultMappings ||
                                      type.resultMappings.state ===
                                        "NOT_APPLICABLE" ||
                                      getMappingState({
                                        testMappings: type.resultMappings,
                                        resultMappings: {
                                          state: "NOT_APPLICABLE",
                                        },
                                      }) === "COMPLETE"
                                        ? "analyzer-type-mapping--complete"
                                        : "analyzer-type-mapping--incomplete"
                                    }
                                  >
                                    {formatMappings(type.resultMappings)}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <FormattedMessage
                                    id="analyzerType.usedBy"
                                    values={{ count: type.usedBy }}
                                  />
                                </TableCell>
                                <TableCell>
                                  <Tag
                                    type={
                                      type.status === "ACTIVE"
                                        ? "green"
                                        : "gray"
                                    }
                                  >
                                    <FormattedMessage
                                      id={
                                        type.status === "ACTIVE"
                                          ? "analyzerType.status.active"
                                          : "analyzerType.status.inactive"
                                      }
                                    />
                                  </Tag>
                                </TableCell>
                                <TableCell>
                                  <OverflowMenu
                                    size="sm"
                                    aria-label={intl.formatMessage(
                                      { id: "analyzerType.actions.ariaLabel" },
                                      { name: type.displayName },
                                    )}
                                    iconDescription={intl.formatMessage(
                                      { id: "analyzerType.actions.ariaLabel" },
                                      { name: type.displayName },
                                    )}
                                  >
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzerType.button.duplicate",
                                      })}
                                      onClick={() =>
                                        openAction("duplicate", type.profileId)
                                      }
                                    />
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzerType.action.history",
                                      })}
                                      onClick={() =>
                                        openAction("history", type.profileId)
                                      }
                                    />
                                    {type.source === "SITE" && (
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id:
                                            type.status === "ACTIVE"
                                              ? "analyzerType.action.deactivate"
                                              : "analyzerType.action.reactivate",
                                        })}
                                        onClick={() =>
                                          openAction(
                                            type.status === "ACTIVE"
                                              ? "deactivate"
                                              : "reactivate",
                                            type.profileId,
                                          )
                                        }
                                      />
                                    )}
                                  </OverflowMenu>
                                </TableCell>
                              </TableRow>
                            );
                          })
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            )
          )}
        </Column>
      </Grid>
      {catalog && actionState.action && (
        <AnalyzerTypeLifecycleModals
          key={`${actionState.action}:${actionState.profileId || ""}`}
          action={actionState.action}
          profileId={actionState.profileId}
          types={catalog.types}
          onClose={closeAction}
          onSuccess={handleActionSuccess}
          onError={handleActionError}
          onDraftCreated={handleDraftCreated}
        />
      )}
    </>
  );
};

export default AnalyzerTypeManagement;
