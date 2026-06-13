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
  Search,
  Dropdown,
  Pagination,
  Tag,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";

/**
 * OGC-949 M3 / OGC-928 — Test List View.
 *
 * Filterable, paginated list of tests; click a row to open the editor
 * (M2 shell) for that test. Domain / Status / AMR filters + search run against
 * the M1 schema. The Coverage-incomplete tag lights up with Ranges (M7).
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

const TestCatalogList = () => {
  const intl = useIntl();
  const history = useHistory();

  const [loading, setLoading] = useState(true);
  const [pageData, setPageData] = useState({ rows: [], total: 0 });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [domain, setDomain] = useState("");
  const [status, setStatus] = useState("all");
  const [search, setSearch] = useState("");

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (domain) params.set("domain", domain);
    if (status) params.set("status", status);
    if (search) params.set("search", search);
    params.set("page", String(page));
    params.set("pageSize", String(pageSize));
    getFromOpenElisServer(
      `/rest/test-catalog/tests?${params.toString()}`,
      (res) => {
        setLoading(false);
        setPageData(res || { rows: [], total: 0 });
      },
    );
  }, [domain, status, search, page, pageSize]);

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
    history.push(`/MasterListsPage/TestCatalogEditor/${testId}`);
  };

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
          </div>
        </Column>

        <Column lg={16} md={8} sm={4}>
          {loading ? (
            <Loading description="Loading" withOverlay={false} />
          ) : (
            <DataTable rows={tableRows} headers={headers}>
              {({ rows, headers: hdrs, getHeaderProps, getTableProps }) => (
                <TableContainer>
                  <TableToolbar>
                    <TableToolbarContent>
                      <Search
                        size="lg"
                        labelText={intl.formatMessage({
                          id: "label.search",
                        })}
                        placeholder={intl.formatMessage({
                          id: "label.testCatalog.list.search",
                        })}
                        onChange={(e) => {
                          setPage(1);
                          setSearch(e.target.value);
                        }}
                        value={search}
                      />
                    </TableToolbarContent>
                  </TableToolbar>
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
                            style={{ cursor: "pointer" }}
                            data-cy={`test-row-${row.id}`}
                          >
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "domain" ? (
                                  <>
                                    {cell.value && (
                                      <Tag type="gray" size="sm">
                                        {cell.value}
                                      </Tag>
                                    )}
                                    {source && source.amr && (
                                      <Tag type="magenta" size="sm">
                                        AMR
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
        </Column>
      </Grid>
    </>
  );
};

export default TestCatalogList;
