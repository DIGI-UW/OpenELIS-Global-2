import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActionableNotification,
  Button,
  Column,
  DataTable,
  Grid,
  InlineNotification,
  Link as CarbonLink,
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
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  Copy,
  WarningAltFilled,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { Link, Redirect, useHistory, useLocation } from "react-router-dom";
import AnalyzerCountChips from "../AnalyzerCountChips";
import AnalyzerPageHeader from "../AnalyzerPageHeader";
import { safeInternalPath } from "../../utils/UrlUtils";
import { getAnalyzerTypeCatalog } from "../../../services/analyzerService";
import AnalyzerTypeLifecycleModals from "./AnalyzerTypeLifecycleModals";
import "./AnalyzerTypeManagement.scss";

const SOURCE_VALUES = new Set(["ALL", "SHIPPED", "SITE"]);
const PROTOCOL_VALUES = new Set(["ALL", "ASTM", "HL7", "FILE"]);
// The count chips: each is a view of the catalog.
const VIEWS = ["all", "inUse", "needsMapping", "deactivated"];

const readEnum = (params, key, allowed) => {
  const value = params.get(key) || "ALL";
  return allowed.has(value) ? value : "ALL";
};

const readFilters = (search) => {
  const params = new URLSearchParams(search);
  const view = params.get("view");
  return {
    query: params.get("q") || "",
    source: readEnum(params, "source", SOURCE_VALUES),
    protocol: readEnum(params, "protocol", PROTOCOL_VALUES),
    view: VIEWS.includes(view) ? view : "all",
  };
};

const getMappingState = (type) => {
  const summaries = [type.testMappings, type.resultMappings].filter(
    (summary) => summary && summary.state !== "NOT_APPLICABLE",
  );
  return summaries.every((summary) => summary.state === "COMPLETE")
    ? "COMPLETE"
    : "INCOMPLETE";
};

// Deactivated types show only in their own view.
const inView = (type, view) => {
  if (view === "deactivated") {
    return type.status === "INACTIVE";
  }
  if (type.status === "INACTIVE") {
    return false;
  }
  if (view === "inUse") {
    return type.usedBy > 0;
  }
  if (view === "needsMapping") {
    return getMappingState(type) === "INCOMPLETE";
  }
  return true;
};

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
  // A draft published on its own page comes back here with what it did.
  const [notification, setNotification] = useState(() =>
    location.state?.analyzerTypeNotice
      ? {
          kind: "success",
          titleId: `analyzerType.notification.${location.state.analyzerTypeNotice}.success`,
          detail: "",
        }
      : null,
  );
  const actionState = useMemo(() => {
    const params = new URLSearchParams(location.search);
    const requestedRevision = Number(params.get("revision"));
    return {
      action: params.get("action"),
      profileId: params.get("profile"),
      draftId: params.get("draft"),
      revision:
        Number.isInteger(requestedRevision) && requestedRevision > 0
          ? requestedRevision
          : null,
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
    const returnTo = safeInternalPath(params.get("returnTo"));
    if (returnTo) {
      history.push(returnTo);
      return;
    }
    params.delete("action");
    params.delete("profile");
    params.delete("draft");
    params.delete("revision");
    params.delete("returnTo");
    history.push({
      pathname: location.pathname,
      search: params.toString() ? `?${params.toString()}` : "",
    });
  }, [history, location.pathname, location.search]);

  // A draft is edited on its own page, which returns to where the action began.
  const draftLocation = useCallback(
    (draftId) => {
      const params = new URLSearchParams(location.search);
      const returnTo = safeInternalPath(params.get("returnTo"));
      ["action", "profile", "draft", "revision", "returnTo"].forEach((key) =>
        params.delete(key),
      );
      const list = params.toString()
        ? `/analyzers/types?${params.toString()}`
        : "/analyzers/types";
      return {
        pathname: `/analyzers/types/drafts/${encodeURIComponent(draftId)}`,
        search: `?${new URLSearchParams({ returnTo: returnTo || list }).toString()}`,
      };
    },
    [location.search],
  );

  const handleDraftCreated = useCallback(
    (draftId) => history.push(draftLocation(draftId)),
    [draftLocation, history],
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
        value === "all" ||
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

  const types = catalog?.types || [];
  const viewCounts = Object.fromEntries(
    VIEWS.map((view) => [
      view,
      types.filter((type) => inView(type, view)).length,
    ]),
  );

  const visibleTypes = useMemo(() => {
    const query = filters.query.trim().toLowerCase();
    return (catalog?.types || []).filter((type) => {
      if (!inView(type, filters.view)) {
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
      return filters.protocol === "ALL" || type.protocol === filters.protocol;
    });
  }, [catalog, filters]);

  const headers = [
    {
      key: "displayName",
      header: intl.formatMessage({ id: "analyzerType.column.type" }),
    },
    {
      key: "mapping",
      header: intl.formatMessage({ id: "analyzerType.column.mapping" }),
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
    mapping: type.profileId,
    usedBy: type.usedBy,
    status: type.status,
    actions: type.profileId,
  }));
  const typesById = new Map(types.map((type) => [type.profileId, type]));

  // One line per kind of mapping the type has: tests, and result values when
  // its tests report coded results.
  const mappingLine = (summary, kind) => {
    if (!summary || summary.state === "NOT_APPLICABLE") {
      return null;
    }
    const complete = summary.state === "COMPLETE";
    const Icon = complete ? CheckmarkFilled : WarningAltFilled;
    return (
      <span
        className={`analyzer-type-mapping-line analyzer-type-mapping-line--${
          complete ? "complete" : "incomplete"
        }`}
      >
        <Icon aria-hidden="true" />
        {intl.formatMessage(
          {
            id:
              summary.excluded > 0
                ? `analyzerType.mapping.${kind}WithExcluded`
                : `analyzerType.mapping.${kind}`,
          },
          {
            mapped: summary.mapped,
            total: summary.total,
            excluded: summary.excluded,
          },
        )}
      </span>
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

  const mappingPath = (type) => ({
    pathname: `/analyzers/types/${encodeURIComponent(type.profileId)}/mapping`,
    search: `?${new URLSearchParams({
      revision: String(type.revision),
      returnTo: `${location.pathname}${location.search}`,
    }).toString()}`,
  });

  const protocolLabel = (protocol) =>
    protocol === "FILE"
      ? intl.formatMessage({ id: "analyzerType.protocol.file" })
      : protocol;

  // An address saved while drafts were edited in a dialog opens the draft's page.
  if (actionState.draftId) {
    return <Redirect to={draftLocation(actionState.draftId)} />;
  }

  return (
    <>
      <Grid fullWidth className="analyzer-type-page">
        <Column lg={16} md={8} sm={4}>
          <AnalyzerPageHeader
            breadcrumbs={[
              { label: "home.label", link: "/" },
              { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
              {
                label: "analyzerType.page.title",
                link: "/analyzers/types",
                isCurrentPage: true,
              },
            ]}
            title={<FormattedMessage id="analyzerType.page.title" />}
            subtitle={<FormattedMessage id="analyzerType.page.subtitle" />}
            actions={
              <>
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
              </>
            }
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

          <AnalyzerCountChips
            testId="analyzer-type-views"
            label={intl.formatMessage({ id: "analyzerType.view.label" })}
            chips={VIEWS.map((view) => ({
              id: view,
              value: viewCounts[view],
              label: intl.formatMessage({ id: `analyzerType.view.${view}` }),
              current: filters.view === view,
              warning: view === "needsMapping" && viewCounts[view] > 0,
              testId: `analyzer-type-view-${view}`,
              onSelect: () => updateQuery("view", view),
            }))}
          />

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
              <SelectItem value="FILE" text={protocolLabel("FILE")} />
            </Select>
          </div>

          {loadError && (
            <ActionableNotification
              inline
              kind="error"
              lowContrast
              title={intl.formatMessage({
                id: "analyzerType.notification.loadError",
              })}
              subtitle={intl.formatMessage({
                id: "analyzerType.notification.loadError.subtitle",
              })}
              actionButtonLabel={intl.formatMessage({
                id: "common.retry",
              })}
              onActionButtonClick={retryLoad}
            />
          )}

          {catalog?.issues?.length > 0 && (
            <div className="analyzer-type-page__catalog-issues">
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "analyzerType.notification.catalogIssues",
                })}
                subtitle={intl.formatMessage({
                  id: "analyzerType.notification.catalogIssues.subtitle",
                })}
              />
              <ul>
                {catalog.issues.map((issue) => (
                  <li key={issue.source}>
                    <code>{issue.source}</code>: {issue.reason}
                  </li>
                ))}
              </ul>
            </div>
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
                  <TableContainer className="analyzer-type-table">
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
                              <p className="analyzer-type-page__empty">
                                <strong>
                                  <FormattedMessage id="analyzerType.empty.title" />
                                </strong>{" "}
                                <FormattedMessage id="analyzerType.empty.subtitle" />
                              </p>
                            </TableCell>
                          </TableRow>
                        ) : (
                          carbonRows.map((row) => {
                            const type = typesById.get(row.id);
                            if (!type) return null;
                            return (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableCell>
                                  <div className="analyzer-type-name">
                                    <CarbonLink
                                      as={Link}
                                      to={mappingPath(type)}
                                      className="analyzer-type-name__link"
                                    >
                                      {type.displayName}
                                    </CarbonLink>
                                    <div className="analyzer-type-name__meta">
                                      {[
                                        type.manufacturer,
                                        type.model,
                                        protocolLabel(type.protocol),
                                        intl.formatMessage(
                                          { id: "analyzerType.revision" },
                                          { revision: type.revision },
                                        ),
                                      ]
                                        .filter(Boolean)
                                        .join(" · ")}
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
                                <TableCell>
                                  <div className="analyzer-type-mapping-cell">
                                    {mappingLine(type.testMappings, "tests")}
                                    {mappingLine(type.resultMappings, "values")}
                                  </div>
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
                                    flipped
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
                                        id: "analyzerType.action.editMappings",
                                      })}
                                      onClick={() =>
                                        history.push(mappingPath(type))
                                      }
                                    />
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzerType.button.duplicate",
                                      })}
                                      onClick={() =>
                                        openAction("duplicate", type.profileId)
                                      }
                                    />
                                    {type.source === "SITE" &&
                                      type.status === "ACTIVE" && (
                                        <OverflowMenuItem
                                          itemText={intl.formatMessage({
                                            id: "analyzerType.action.updateShared",
                                          })}
                                          onClick={() =>
                                            openAction("update", type.profileId)
                                          }
                                        />
                                      )}
                                    <OverflowMenuItem
                                      itemText={intl.formatMessage({
                                        id: "analyzerType.action.history",
                                      })}
                                      onClick={() =>
                                        openAction("history", type.profileId)
                                      }
                                    />
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
          key={`${actionState.action}:${actionState.profileId || ""}:${actionState.revision || ""}`}
          action={actionState.action}
          profileId={actionState.profileId}
          revision={actionState.revision}
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
