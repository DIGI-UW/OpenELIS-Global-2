import React, { useEffect, useMemo, useState } from "react";
import {
  Column,
  DataTable,
  Grid,
  InlineNotification,
  Link as CarbonLink,
  Loading,
  OverflowMenu,
  OverflowMenuItem,
  Search,
  Section,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  Toggle,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { Link, useHistory, useLocation } from "react-router-dom";
import config from "../../../config.json";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../../utils/Utils";
import {
  AnalyzerTypeMappingProgress,
  AnalyzerTypeSourceTag,
  AnalyzerTypeStatusTag,
  isMappingComplete,
  mappingProgress,
  profileMetadata,
} from "./AnalyzerTypePresentation";
import "./AnalyzerTypeManagement.scss";

const SEARCH_DEBOUNCE_MS = 250;

const SOURCE_OPTIONS = ["", "SITE", "SHIPPED"];
const PROTOCOL_OPTIONS = ["", "ASTM", "HL7", "FILE"];
const MAPPING_STATUS_OPTIONS = ["", "INCOMPLETE", "COMPLETE"];

const AnalyzerTypeManagement = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const initialParams = useMemo(
    () => new URLSearchParams(location.search),
    // Query state is initialized once. Subsequent changes are owned here and
    // mirrored back to the route in canonical order.
    [],
  );

  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState(initialParams.get("q") || "");
  const [debouncedSearch, setDebouncedSearch] = useState(
    initialParams.get("q") || "",
  );
  const [source, setSource] = useState(initialParams.get("source") || "");
  const [protocol, setProtocol] = useState(initialParams.get("protocol") || "");
  const [mappingStatus, setMappingStatus] = useState(
    initialParams.get("mappingStatus") || "",
  );
  const [showInactive, setShowInactive] = useState(
    initialParams.get("showInactive") === "true",
  );

  useEffect(() => {
    const timer = setTimeout(
      () => setDebouncedSearch(search.trim()),
      SEARCH_DEBOUNCE_MS,
    );
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    const routeParams = new URLSearchParams();
    const apiParams = new URLSearchParams();

    if (debouncedSearch) {
      routeParams.set("q", debouncedSearch);
      apiParams.set("q", debouncedSearch);
    }
    if (source) {
      routeParams.set("source", source);
      apiParams.set("source", source);
    }
    if (protocol) {
      routeParams.set("protocol", protocol);
      apiParams.set("protocol", protocol);
    }
    if (mappingStatus) routeParams.set("mappingStatus", mappingStatus);
    if (showInactive) routeParams.set("showInactive", "true");
    if (!showInactive) apiParams.set("status", "ACTIVE");

    history.replace({
      pathname: location.pathname,
      search: routeParams.toString(),
    });

    const controller = new AbortController();
    setLoading(true);
    setError(null);
    getFromOpenElisServer(
      `/rest/analyzer/types?${apiParams.toString()}`,
      (response) => {
        setLoading(false);
        if (Array.isArray(response)) {
          setProfiles(response);
          return;
        }
        setProfiles([]);
        setError(
          response?.error ||
            intl.formatMessage({ id: "analyzerType.error.loadDetail" }),
        );
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [
    debouncedSearch,
    history,
    intl,
    location.pathname,
    mappingStatus,
    protocol,
    showInactive,
    source,
  ]);

  const visibleProfiles = useMemo(() => {
    if (!mappingStatus) return profiles;
    const expectedComplete = mappingStatus === "COMPLETE";
    return profiles.filter(
      (profile) => isMappingComplete(profile) === expectedComplete,
    );
  }, [mappingStatus, profiles]);

  const headers = [
    {
      key: "analyzerType",
      header: intl.formatMessage({ id: "analyzerType.column.analyzerType" }),
    },
    {
      key: "protocol",
      header: intl.formatMessage({ id: "analyzerType.column.protocol" }),
    },
    {
      key: "tests",
      header: intl.formatMessage({ id: "analyzerType.column.testsMapped" }),
    },
    {
      key: "results",
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

  const rows = visibleProfiles.map((profile) => ({
    id: profile.profileId,
    analyzerType: profile.displayName,
    protocol: profile.protocol,
    tests: mappingProgress(profile.testMappings),
    results: mappingProgress(profile.resultValueMappings),
    usedBy: intl.formatMessage(
      { id: "analyzerType.usedBy" },
      { count: profile.analyzerCount || 0 },
    ),
    status: profile.status,
    actions: "",
    profile,
  }));

  const renderCell = (row, cell) => {
    const profile = row.profile;
    if (cell.info.header === "analyzerType") {
      return (
        <div className="analyzer-type-name">
          <CarbonLink
            as={Link}
            to={`/analyzers/types/${encodeURIComponent(
              profile.profileId,
            )}?revision=${profile.revision}`}
          >
            {profile.displayName}
          </CarbonLink>
          <span className="analyzer-type-meta">{profileMetadata(profile)}</span>
          <AnalyzerTypeSourceTag source={profile.source} />
        </div>
      );
    }
    if (cell.info.header === "tests") {
      return <AnalyzerTypeMappingProgress mapping={profile.testMappings} />;
    }
    if (cell.info.header === "results") {
      return (
        <AnalyzerTypeMappingProgress mapping={profile.resultValueMappings} />
      );
    }
    if (cell.info.header === "status") {
      return <AnalyzerTypeStatusTag status={profile.status} />;
    }
    if (cell.info.header === "actions") {
      const profileRoute = `/analyzers/types/${encodeURIComponent(
        profile.profileId,
      )}?revision=${profile.revision}`;
      return (
        <OverflowMenu
          aria-label={intl.formatMessage(
            { id: "analyzerType.action.menu" },
            { name: profile.displayName },
          )}
          iconDescription={intl.formatMessage(
            { id: "analyzerType.action.menu" },
            { name: profile.displayName },
          )}
          flipped
          size="sm"
        >
          <OverflowMenuItem
            itemText={intl.formatMessage({ id: "analyzerType.action.review" })}
            onClick={() => history.push(profileRoute)}
          />
          <OverflowMenuItem
            itemText={intl.formatMessage({
              id: "analyzerType.action.forkShort",
            })}
            onClick={() => history.push(`${profileRoute}&action=fork`)}
          />
          <OverflowMenuItem
            itemText={intl.formatMessage({ id: "analyzerType.action.export" })}
            href={`${config.serverBaseUrl}/rest/analyzer/types/${encodeURIComponent(
              profile.profileId,
            )}/export?revision=${profile.revision}`}
          />
          <OverflowMenuItem
            itemText={intl.formatMessage({
              id:
                profile.status === "ACTIVE"
                  ? "analyzerType.action.deactivate"
                  : "analyzerType.action.reactivate",
            })}
            isDelete={profile.status === "ACTIVE"}
            onClick={() =>
              history.push(
                `${profileRoute}&action=${
                  profile.status === "ACTIVE" ? "deactivate" : "reactivate"
                }`,
              )
            }
          />
        </OverflowMenu>
      );
    }
    return cell.value;
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
      <Grid fullWidth className="analyzer-type-catalog">
        <Column lg={16} md={8} sm={4}>
          <Section>
            <h1 className="analyzer-type-heading">
              {intl.formatMessage({ id: "analyzerType.page.title" })}
            </h1>
            <DataTable rows={rows} headers={headers} size="md">
              {({
                rows: tableRows,
                headers: tableHeaders,
                getHeaderProps,
                getRowProps,
                getTableProps,
              }) => (
                <TableContainer>
                  <TableToolbar>
                    <TableToolbarContent className="analyzer-type-toolbar">
                      <Search
                        id="analyzer-type-search"
                        labelText={intl.formatMessage({
                          id: "analyzerType.search.label",
                        })}
                        placeholder={intl.formatMessage({
                          id: "analyzerType.search.placeholder",
                        })}
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                      />
                      <Select
                        id="analyzer-type-source"
                        labelText={intl.formatMessage({
                          id: "analyzerType.filter.created",
                        })}
                        value={source}
                        onChange={(event) => setSource(event.target.value)}
                      >
                        {SOURCE_OPTIONS.map((option) => (
                          <SelectItem
                            key={option || "ALL"}
                            value={option}
                            text={intl.formatMessage({
                              id: `analyzerType.filter.source.${
                                option ? option.toLowerCase() : "all"
                              }`,
                            })}
                          />
                        ))}
                      </Select>
                      <Select
                        id="analyzer-type-protocol"
                        labelText={intl.formatMessage({
                          id: "analyzerType.filter.protocol",
                        })}
                        value={protocol}
                        onChange={(event) => setProtocol(event.target.value)}
                      >
                        {PROTOCOL_OPTIONS.map((option) => (
                          <SelectItem
                            key={option || "ALL"}
                            value={option}
                            text={
                              option ||
                              intl.formatMessage({
                                id: "analyzerType.filter.protocol.all",
                              })
                            }
                          />
                        ))}
                      </Select>
                      <Select
                        id="analyzer-type-mapping-status"
                        labelText={intl.formatMessage({
                          id: "analyzerType.filter.mappingStatus",
                        })}
                        value={mappingStatus}
                        onChange={(event) =>
                          setMappingStatus(event.target.value)
                        }
                      >
                        {MAPPING_STATUS_OPTIONS.map((option) => (
                          <SelectItem
                            key={option || "ALL"}
                            value={option}
                            text={intl.formatMessage({
                              id: `analyzerType.filter.mappingStatus.${
                                option ? option.toLowerCase() : "all"
                              }`,
                            })}
                          />
                        ))}
                      </Select>
                      <Toggle
                        id="analyzer-type-show-inactive"
                        labelText={intl.formatMessage({
                          id: "analyzerType.filter.showInactive",
                        })}
                        labelA={intl.formatMessage({
                          id: "analyzerType.filter.showInactive.off",
                        })}
                        labelB={intl.formatMessage({
                          id: "analyzerType.filter.showInactive.on",
                        })}
                        toggled={showInactive}
                        onToggle={setShowInactive}
                        size="sm"
                      />
                    </TableToolbarContent>
                  </TableToolbar>

                  {error && (
                    <InlineNotification
                      kind="error"
                      lowContrast
                      hideCloseButton
                      title={intl.formatMessage({
                        id: "analyzerType.error.loadTitle",
                      })}
                      subtitle={error}
                    />
                  )}

                  {loading ? (
                    <div className="analyzer-type-loading">
                      <Loading withOverlay={false} small />
                    </div>
                  ) : !error && rows.length === 0 ? (
                    <InlineNotification
                      kind="info"
                      lowContrast
                      hideCloseButton
                      title={intl.formatMessage({
                        id: "analyzerType.empty.title",
                      })}
                      subtitle={intl.formatMessage({
                        id: "analyzerType.empty.detail",
                      })}
                    />
                  ) : !error ? (
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {tableHeaders.map((header) => (
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
                        {tableRows.map((tableRow) => {
                          const row = rows.find(
                            (candidate) => candidate.id === tableRow.id,
                          );
                          return (
                            <TableRow
                              key={tableRow.id}
                              {...getRowProps({ row: tableRow })}
                            >
                              {tableRow.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {renderCell(row, cell)}
                                </TableCell>
                              ))}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  ) : null}
                </TableContainer>
              )}
            </DataTable>
          </Section>
        </Column>
      </Grid>
    </>
  );
};

export default AnalyzerTypeManagement;
