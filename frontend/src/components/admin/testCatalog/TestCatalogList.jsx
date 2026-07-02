import React, { useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import {
  Grid,
  Column,
  Section,
  Heading,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  TableSelectAll,
  TableSelectRow,
  Search,
  Dropdown,
  ComboBox,
  Button,
  Pagination,
  Tag,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Add, Edit, Filter } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { DEFAULT_SECTION } from "./sectionConfig";

/**
 * OGC-949 / OGC-1112 — Test List View.
 *
 * Filterable, paginated list of tests. Click a row to open the single-test
 * editor; select 2+ rows to open the combined "edit related tests together"
 * editor (FR-6). A "New test" button starts create-in-place (FR-1). Filters
 * (Domain / Status / AMR / Sample Type) live in a collapsible panel with an
 * active-filter count (FR-40); the Sample Type filter is a typeahead. A Sample
 * Type column (FR-39) disambiguates same-name sibling tests. Filter + page state
 * is mirrored to the URL so a reload restores it.
 */
const DOMAIN_OPTIONS = [
  { id: "", label: "label.testCatalog.list.filter.allDomains" },
  { id: "CLINICAL", label: "label.testCatalog.basicInfo.domain.CLINICAL" },
  {
    id: "ENVIRONMENTAL",
    label: "label.testCatalog.basicInfo.domain.ENVIRONMENTAL",
  },
  { id: "VECTOR", label: "label.testCatalog.basicInfo.domain.VECTOR" },
];

const STATUS_OPTIONS = [
  { id: "all", label: "label.testCatalog.list.filter.allStatus" },
  { id: "active", label: "label.testCatalog.basicInfo.active" },
  { id: "inactive", label: "label.testCatalog.list.filter.inactive" },
];

const AMR_OPTIONS = [
  { id: "", label: "label.testCatalog.list.filter.anyAmr" },
  { id: "true", label: "label.testCatalog.list.filter.amrOnly" },
  { id: "false", label: "label.testCatalog.list.filter.nonAmr" },
];

const SEARCH_DEBOUNCE_MS = 300;

const TestCatalogList = () => {
  const intl = useIntl();
  const history = useHistory();

  // Initialize from the URL so filter + page state survives a reload (US3).
  const initParams = new URLSearchParams(history.location.search);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [pageData, setPageData] = useState({ rows: [], total: 0 });
  const [page, setPage] = useState(Number(initParams.get("page")) || 1);
  const [pageSize, setPageSize] = useState(
    Number(initParams.get("pageSize")) || 25,
  );
  const [domain, setDomain] = useState(initParams.get("domain") || "");
  const [status, setStatus] = useState(initParams.get("status") || "all");
  const [amr, setAmr] = useState(initParams.get("amr") || "");
  const [sampleType, setSampleType] = useState(
    initParams.get("sampleType") || "",
  );
  const [sampleTypes, setSampleTypes] = useState([]);
  const [search, setSearch] = useState(initParams.get("search") || "");
  const [debouncedSearch, setDebouncedSearch] = useState(
    initParams.get("search") || "",
  );
  const [filtersOpen, setFiltersOpen] = useState(false);

  // Sample types for the filter dropdown — fetched once (static reference data).
  useEffect(() => {
    getFromOpenElisServer("/rest/test-catalog/sample-types", (res) => {
      setSampleTypes(Array.isArray(res) ? res : []);
    });
  }, []);

  // Debounce the search box: fetch once the user pauses, not on every keystroke.
  useEffect(() => {
    const timer = setTimeout(
      () => setDebouncedSearch(search),
      SEARCH_DEBOUNCE_MS,
    );
    return () => clearTimeout(timer);
  }, [search]);

  // Fetch the page and mirror the applied state into the URL. The
  // AbortController cancels an in-flight request whenever the inputs change
  // again, so a slow earlier response can never overwrite a newer one.
  useEffect(() => {
    const params = new URLSearchParams();
    if (domain) params.set("domain", domain);
    if (status && status !== "all") params.set("status", status);
    if (amr) params.set("amr", amr);
    if (sampleType) params.set("sampleType", sampleType);
    if (debouncedSearch) params.set("search", debouncedSearch);
    params.set("page", String(page));
    params.set("pageSize", String(pageSize));
    history.replace({ search: params.toString() });

    const controller = new AbortController();
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests?${params.toString()}`,
      (res) => {
        setLoading(false);
        if (res && Array.isArray(res.rows)) {
          setPageData(res);
        } else {
          // getFromOpenElisServer calls back with undefined on a failed fetch
          // (and not at all on abort) — distinguish that from an empty result.
          setError(true);
          setPageData({ rows: [], total: 0 });
        }
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [
    domain,
    status,
    amr,
    sampleType,
    debouncedSearch,
    page,
    pageSize,
    history,
  ]);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "label.testCatalog.list",
      link: "/MasterListsPage/TestCatalogList",
    },
  ];

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "label.testCatalog.basicInfo.name" }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "label.testCatalog.list.col.sampleType",
      }),
    },
    {
      key: "code",
      header: intl.formatMessage({ id: "label.testCatalog.basicInfo.code" }),
    },
    {
      key: "domain",
      header: intl.formatMessage({ id: "label.testCatalog.basicInfo.domain" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "label.testCatalog.list.col.status" }),
    },
  ];

  const openEditor = (testId) => {
    // Push the canonical section URL so the deep-link is fully formed and the
    // first section + its SideNav item light up immediately.
    history.push(
      `/MasterListsPage/TestCatalogEditor/${testId}/${DEFAULT_SECTION}`,
    );
  };

  const openNewTest = () => {
    // Create-in-place: open Basic Info blank in the editor shell (FR-1/FR-2).
    history.push(`/MasterListsPage/TestCatalogEditor/new/${DEFAULT_SECTION}`);
  };

  const openRelatedEditor = (selectedRows) => {
    // Combined editor over the selected set (FR-6/FR-8).
    const ids = selectedRows.map((r) => r.id).join(",");
    history.push(
      `/MasterListsPage/TestCatalogEditor/group/${ids}/${DEFAULT_SECTION}`,
    );
  };

  const sampleTypeItems = [
    {
      id: "",
      name: intl.formatMessage({
        id: "label.testCatalog.list.filter.allSampleTypes",
      }),
    },
    ...sampleTypes,
  ];

  const activeFilterCount =
    (domain ? 1 : 0) +
    (status && status !== "all" ? 1 : 0) +
    (amr ? 1 : 0) +
    (sampleType ? 1 : 0);

  const tableRows = (pageData.rows || []).map((r) => ({
    id: r.testId,
    name: r.name,
    sampleType: r.sampleType || "",
    code: r.code || "",
    domain: r.domain || "",
    active: r.active,
    amr: r.amr,
    coverageIncomplete: r.coverageIncomplete,
  }));

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="label.testCatalog.list" />
            </Heading>
          </Section>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <div style={{ margin: "1rem 0" }}>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Filter}
              onClick={() => setFiltersOpen((o) => !o)}
              aria-expanded={filtersOpen}
            >
              {activeFilterCount > 0
                ? intl.formatMessage(
                    { id: "label.testCatalog.list.filters.count" },
                    { count: activeFilterCount },
                  )
                : intl.formatMessage({ id: "label.testCatalog.list.filters" })}
            </Button>
            {filtersOpen && (
              <div
                style={{
                  display: "flex",
                  gap: "1rem",
                  flexWrap: "wrap",
                  marginTop: "0.5rem",
                  alignItems: "flex-end",
                }}
              >
                <Dropdown
                  id="filter-domain"
                  titleText={intl.formatMessage({
                    id: "label.testCatalog.basicInfo.domain",
                  })}
                  label=""
                  items={DOMAIN_OPTIONS}
                  itemToString={(item) =>
                    item ? intl.formatMessage({ id: item.label }) : ""
                  }
                  selectedItem={DOMAIN_OPTIONS.find((o) => o.id === domain)}
                  onChange={({ selectedItem }) => {
                    setPage(1);
                    setDomain(selectedItem ? selectedItem.id : "");
                  }}
                />
                <Dropdown
                  id="filter-status"
                  titleText={intl.formatMessage({
                    id: "label.testCatalog.list.col.status",
                  })}
                  label=""
                  items={STATUS_OPTIONS}
                  itemToString={(item) =>
                    item ? intl.formatMessage({ id: item.label }) : ""
                  }
                  selectedItem={STATUS_OPTIONS.find((o) => o.id === status)}
                  onChange={({ selectedItem }) => {
                    setPage(1);
                    setStatus(selectedItem ? selectedItem.id : "all");
                  }}
                />
                <Dropdown
                  id="filter-amr"
                  titleText={intl.formatMessage({
                    id: "label.testCatalog.list.filter.amr",
                  })}
                  label=""
                  items={AMR_OPTIONS}
                  itemToString={(item) =>
                    item ? intl.formatMessage({ id: item.label }) : ""
                  }
                  selectedItem={AMR_OPTIONS.find((o) => o.id === amr)}
                  onChange={({ selectedItem }) => {
                    setPage(1);
                    setAmr(selectedItem ? selectedItem.id : "");
                  }}
                />
                <ComboBox
                  id="filter-sample-type"
                  titleText={intl.formatMessage({
                    id: "label.testCatalog.list.filter.sampleType",
                  })}
                  items={sampleTypeItems}
                  itemToString={(item) => (item ? item.name : "")}
                  selectedItem={sampleTypeItems.find(
                    (o) => o.id === sampleType,
                  )}
                  onChange={({ selectedItem }) => {
                    setPage(1);
                    setSampleType(selectedItem ? selectedItem.id : "");
                  }}
                />
              </div>
            )}
          </div>
        </Column>

        <Column lg={16} md={8} sm={4}>
          {loading ? (
            <Loading
              description={intl.formatMessage({ id: "label.loading" })}
              withOverlay={false}
            />
          ) : error ? (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "label.testCatalog.list.error" })}
            />
          ) : (
            <DataTable rows={tableRows} headers={headers}>
              {({
                rows,
                headers: hdrs,
                getHeaderProps,
                getRowProps,
                getSelectionProps,
                getBatchActionProps,
                getTableProps,
                getToolbarProps,
                selectedRows,
              }) => (
                <TableContainer>
                  <TableToolbar {...getToolbarProps()}>
                    <TableBatchActions {...getBatchActionProps()}>
                      <TableBatchAction
                        renderIcon={Edit}
                        disabled={selectedRows.length < 2}
                        onClick={() => openRelatedEditor(selectedRows)}
                      >
                        {intl.formatMessage({
                          id: "button.testCatalog.editRelated",
                        })}
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <Search
                        size="lg"
                        id="test-search"
                        labelText={intl.formatMessage({ id: "label.search" })}
                        placeholder={intl.formatMessage({
                          id: "label.testCatalog.list.search",
                        })}
                        onChange={(e) => {
                          setPage(1);
                          setSearch(e.target.value);
                        }}
                        value={search}
                      />
                      <Button
                        renderIcon={Add}
                        onClick={openNewTest}
                        data-testid="new-test-button"
                      >
                        {intl.formatMessage({
                          id: "button.testCatalog.newTest",
                        })}
                      </Button>
                    </TableToolbarContent>
                  </TableToolbar>
                  {tableRows.length === 0 ? (
                    <InlineNotification
                      kind="info"
                      lowContrast
                      hideCloseButton
                      title={intl.formatMessage({
                        id: "label.testCatalog.list.empty",
                      })}
                    />
                  ) : (
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          <TableSelectAll {...getSelectionProps()} />
                          {hdrs.map((header) => (
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
                          const source = tableRows.find((t) => t.id === row.id);
                          return (
                            <TableRow
                              {...getRowProps({ row })}
                              key={row.id}
                              onClick={() => openEditor(row.id)}
                              onKeyDown={(e) => {
                                if (e.key === "Enter" || e.key === " ") {
                                  e.preventDefault();
                                  openEditor(row.id);
                                }
                              }}
                              tabIndex={0}
                              style={{ cursor: "pointer" }}
                              data-cy={`test-row-${row.id}`}
                            >
                              {/* Selecting a row must not open the editor. */}
                              <TableSelectRow
                                {...getSelectionProps({ row })}
                                onClick={(e) => e.stopPropagation()}
                              />
                              {row.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.info.header === "domain" ? (
                                    <>
                                      {cell.value && (
                                        <Tag type="gray" size="sm">
                                          {intl.formatMessage({
                                            id: `label.testCatalog.basicInfo.domain.${cell.value}`,
                                            defaultMessage: cell.value,
                                          })}
                                        </Tag>
                                      )}
                                      {source && source.amr && (
                                        <Tag type="magenta" size="sm">
                                          <FormattedMessage id="label.testCatalog.list.amrTag" />
                                        </Tag>
                                      )}
                                    </>
                                  ) : cell.info.header === "status" ? (
                                    <Tag
                                      type={
                                        source && source.active
                                          ? "green"
                                          : "cool-gray"
                                      }
                                      size="sm"
                                    >
                                      <FormattedMessage
                                        id={
                                          source && source.active
                                            ? "label.testCatalog.basicInfo.active"
                                            : "label.testCatalog.list.filter.inactive"
                                        }
                                      />
                                    </Tag>
                                  ) : (
                                    cell.value
                                  )}
                                  {cell.info.header === "name" &&
                                    source &&
                                    source.coverageIncomplete && (
                                      <Tag type="red" size="sm">
                                        <FormattedMessage id="label.testCatalog.list.coverageIncomplete" />
                                      </Tag>
                                    )}
                                </TableCell>
                              ))}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  )}
                </TableContainer>
              )}
            </DataTable>
          )}
          {!loading && !error && (
            <Pagination
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 25, 50, 100]}
              totalItems={pageData.total || 0}
              onChange={({ page: p, pageSize: ps }) => {
                setPage(p);
                setPageSize(ps);
              }}
            />
          )}
        </Column>
      </Grid>
    </>
  );
};

export default TestCatalogList;
