import React, { useEffect, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import {
  Stack,
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Search,
  Select,
  SelectItem,
  TextInput,
  Button,
  InlineLoading,
  InlineNotification,
  Tag,
  Link,
} from "@carbon/react";
import serviceDefault from "./CaseWorkspaceService";
import "./CaseWorkspace.scss";
import { MICROBIOLOGY_CASE_PATH } from "./MicrobiologyRoutes";

export default function CaseWorklist({ service = serviceDefault }) {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const t = (id, values) => intl.formatMessage({ id }, values);
  const params = new URLSearchParams(location.search);
  const [data, setData] = useState(null);
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(true);
  const [refresh, setRefresh] = useState(0);
  useEffect(() => {
    let live = true;
    setLoading(true);
    setError(false);
    service
      .searchCases(location.search.replace(/^\?/, ""))
      .then((d) => {
        if (live) setData(d);
      })
      .catch(() => {
        if (live) setError(true);
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [service, location.search, refresh]);
  const change = (key, value) => {
    const next = new URLSearchParams(location.search);
    if (value) next.set(key, String(value));
    else next.delete(key);
    if (key !== "page") next.delete("page");
    history.push({ pathname: location.pathname, search: next.toString() });
  };
  const headers = [
    { key: "accessionNumber", header: t("common.labNumber") },
    { key: "patientName", header: t("common.patient") },
    { key: "labUnit", header: t("microbiology.case.labUnit") },
    { key: "specimenType", header: t("microbiology.worklist.column.specimen") },
    { key: "status", header: t("common.status") },
    { key: "createdAt", header: t("microbiology.case.opened") },
  ];
  const formatStatus = (status) => t(`microbiology.case.status.${status}`);
  // i18n-keys: microbiology.case.status.*
  return (
    <Grid fullWidth className="microbiology-case-workspace">
      <Column lg={16} md={8} sm={4}>
        <Stack gap={5}>
          <h1>{t("microbiology.worklist.title")}</h1>
          <Search
            id="case-search"
            labelText={t("microbiology.case.search")}
            placeholder={t("microbiology.case.search")}
            value={params.get("q") || ""}
            onChange={(e) => change("q", e.target.value)}
          />
          <Grid condensed>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="case-unit"
                labelText={t("microbiology.case.labUnit")}
                value={params.get("labUnitId") || ""}
                onChange={(e) => change("labUnitId", e.target.value)}
              >
                <SelectItem value="" text={t("microbiology.case.allUnits")} />
                {(data?.labUnits || []).map((u) => (
                  <SelectItem key={u.id} value={u.id} text={u.value} />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="case-status"
                labelText={t("common.status")}
                value={params.get("status") || ""}
                onChange={(e) => change("status", e.target.value)}
              >
                <SelectItem
                  value=""
                  text={t("microbiology.case.allStatuses")}
                />
                {["ACTIVE", "CANCELLED", "REJECTED"].map((s) => (
                  <SelectItem key={s} value={s} text={formatStatus(s)} />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="case-sort"
                labelText={t("microbiology.case.sort")}
                value={params.get("sort") || "newest"}
                onChange={(e) => change("sort", e.target.value)}
              >
                <SelectItem
                  value="newest"
                  text={t("microbiology.case.newest")}
                />
                <SelectItem value="accession" text={t("common.labNumber")} />
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Button kind="ghost" onClick={() => setRefresh((n) => n + 1)}>
                {t("common.refresh")}
              </Button>
            </Column>
          </Grid>
          <TextInput
            type="date"
            id="case-from"
            labelText={t("microbiology.case.from")}
            value={params.get("from") || ""}
            onChange={(e) => change("from", e.target.value)}
          />
          <TextInput
            type="date"
            id="case-to"
            labelText={t("microbiology.case.to")}
            value={params.get("to") || ""}
            onChange={(e) => change("to", e.target.value)}
          />
          {loading && <InlineLoading description={t("common.loading")} />}
          {error && (
            <InlineNotification
              kind="error"
              title={t("microbiology.case.loadError")}
              hideCloseButton
            />
          )}
          {!loading && !error && data && (
            <>
              <DataTable rows={data.rows} headers={headers}>
                {({
                  rows,
                  headers,
                  getHeaderProps,
                  getRowProps,
                  getTableProps,
                }) => (
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map((h) => (
                          <TableHeader
                            key={h.key}
                            {...getHeaderProps({ header: h })}
                          >
                            {h.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "accessionNumber" ? (
                                <Link
                                  href={`${MICROBIOLOGY_CASE_PATH}/${encodeURIComponent(row.id)}${location.search}`}
                                  onClick={(e) => {
                                    e.preventDefault();
                                    history.push(
                                      `${MICROBIOLOGY_CASE_PATH}/${encodeURIComponent(row.id)}${location.search}`,
                                    );
                                  }}
                                >
                                  {cell.value || row.id}
                                </Link>
                              ) : cell.info.header === "status" ? (
                                <Tag
                                  type={
                                    cell.value === "ACTIVE" ? "blue" : "gray"
                                  }
                                >
                                  {formatStatus(cell.value)}
                                </Tag>
                              ) : cell.info.header === "createdAt" &&
                                cell.value ? (
                                intl.formatDate(new Date(cell.value))
                              ) : (
                                cell.value || t("not.available")
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
              {data.rows.length === 0 && (
                <p>{t("microbiology.case.noCases")}</p>
              )}
              <Pagination
                page={data.page}
                pageSize={data.pageSize}
                pageSizes={[10, 20, 50, 100]}
                totalItems={data.total}
                backwardText={t("common.previous")}
                forwardText={t("common.next")}
                itemsPerPageText={t("microbiology.case.itemsPerPage")}
                itemRangeText={(min, max, total) =>
                  t("microbiology.case.range", { min, max, total })
                }
                pageRangeText={(current, total) =>
                  t("microbiology.case.pages", { current, total })
                }
                onChange={({ page, pageSize }) => {
                  const next = new URLSearchParams(location.search);
                  next.set("page", String(page));
                  next.set("pageSize", String(pageSize));
                  history.push({
                    pathname: location.pathname,
                    search: next.toString(),
                  });
                }}
              />
            </>
          )}
        </Stack>
      </Column>
    </Grid>
  );
}
