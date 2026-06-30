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
  Search,
  Dropdown,
  Pagination,
  Tag,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { DEFAULT_SECTION } from "./sectionConfig";

/**
 * OGC-949 M3 / OGC-928 — Test List View.
 *
 * Filterable, paginated list of tests; click (or keyboard-activate) a row to
 * open the editor (M2 shell) for that test. Domain / Status / AMR filters + a
 * debounced name search run against the M1 schema; the filter + page state is
 * mirrored into the URL so a reload restores it (US3). A failed fetch shows an
 * error state instead of silently rendering an empty list. The
 * Coverage-incomplete tag lights up with Ranges (M7).
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

  const sampleTypeItems = [
    {
      id: "",
      name: intl.formatMessage({
        id: "label.testCatalog.list.filter.allSampleTypes",
      }),
    },
    ...sampleTypes,
  ];

  const tableRows = (pageData.rows || []).map((r) => ({
    id: r.testId,
    name: r.name,
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
          <div
            style={{
              display: "flex",
              gap: "1rem",
              flexWrap: "wrap",
              margin: "1rem 0",
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
            <Dropdown
              id="filter-sample-type"
              titleText={intl.formatMessage({
                id: "label.testCatalog.list.filter.sampleType",
              })}
              label=""
              items={sampleTypeItems}
              itemToString={(item) => (item ? item.name : "")}
              selectedItem={sampleTypeItems.find((o) => o.id === sampleType)}
              onChange={({ selectedItem }) => {
                setPage(1);
                setSampleType(selectedItem ? selectedItem.id : "");
              }}
            />
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
          ) : tableRows.length === 0 ? (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "label.testCatalog.list.empty" })}
            />
          ) : (
            <DataTable rows={tableRows} headers={headers}>
              {({ rows, headers: hdrs, getHeaderProps, getTableProps }) => (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
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
