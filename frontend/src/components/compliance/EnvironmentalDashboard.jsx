import React, {
  useState,
  useEffect,
  useContext,
  useRef,
  useCallback,
} from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  Select,
  SelectItem,
  MultiSelect,
  DatePicker,
  DatePickerInput,
  InlineLoading,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Tag,
} from "@carbon/react";
import { LineChart, SimpleBarChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";
import { generateCompliancePdf } from "./utils/compliancePdfGenerator";

const KPI_KEYS = [
  { key: "totalOrders", i18n: "compliance.dashboard.kpi.totalOrders" },
  {
    key: "complianceRate",
    i18n: "compliance.dashboard.kpi.complianceRate",
    suffix: "%",
  },
  { key: "totalExceedances", i18n: "compliance.dashboard.kpi.exceedances" },
  { key: "sitesMonitored", i18n: "compliance.dashboard.kpi.sitesMonitored" },
];

const EXC_HEADERS = [
  { key: "date", header: "Date" },
  { key: "labNumber", header: "Lab #" },
  { key: "siteName", header: "Site" },
  { key: "parameter", header: "Parameter" },
  { key: "result", header: "Result" },
  { key: "threshold", header: "Threshold" },
  { key: "status", header: "Status" },
];

function todayStr() {
  // Add 1 day to avoid timezone skew cutting off samples collected today in UTC+N zones
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
}
function monthsAgoStr(n) {
  const d = new Date();
  d.setMonth(d.getMonth() - n);
  return d.toISOString().slice(0, 10);
}

export default function EnvironmentalDashboard() {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [startDate, setStartDate] = useState(monthsAgoStr(12));
  const [endDate, setEndDate] = useState(todayStr());
  const [standardId, setStandardId] = useState("");
  const [selectedSiteIds, setSelectedSiteIds] = useState([]);
  const [standards, setStandards] = useState([]);
  const [sites, setSites] = useState([]);
  const [summary, setSummary] = useState(null);
  const [trend, setTrend] = useState(null);
  const [comparison, setComparison] = useState(null);
  const [exceedances, setExceedances] = useState(null);
  const [siteParams, setSiteParams] = useState(null);
  const [selectedSite, setSelectedSite] = useState(null);
  const [excPage, setExcPage] = useState(1);
  const [excPageSize, setExcPageSize] = useState(20);
  const [loadingSummary, setLoadingSummary] = useState(false);
  const [loadingTrend, setLoadingTrend] = useState(false);
  const [loadingComparison, setLoadingComparison] = useState(false);
  const [loadingExceedances, setLoadingExceedances] = useState(false);
  const [loadingDrilldown, setLoadingDrilldown] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);

  const trendRef = useRef(null);
  const drilldownRef = useRef(null);
  const comparisonRef = useRef(null);

  useEffect(() => {
    getFromOpenElisServer("/rest/compliance/standards/active", (res) => {
      if (Array.isArray(res)) setStandards(res);
    });
    getFromOpenElisServer("/rest/admin/vector/sampling-sites", (res) => {
      if (Array.isArray(res)) setSites(res);
    });
  }, []);

  const buildQuery = useCallback(() => {
    const p = new URLSearchParams();
    if (startDate) p.append("startDate", startDate);
    if (endDate) p.append("endDate", endDate);
    if (standardId) p.append("standardId", standardId);
    if (selectedSiteIds.length > 0)
      p.append("siteIds", selectedSiteIds.join(","));
    return p.toString();
  }, [startDate, endDate, standardId, selectedSiteIds]);

  const fetchExceedances = useCallback((q, pg, sz) => {
    setLoadingExceedances(true);
    getFromOpenElisServer(
      "/rest/compliance/dashboard/exceedances?" +
        q +
        "&page=" +
        pg +
        "&size=" +
        sz,
      (res) => {
        setExceedances(res);
        setLoadingExceedances(false);
      },
    );
  }, []);

  useEffect(() => {
    const q = buildQuery();
    setLoadingSummary(true);
    setLoadingTrend(true);
    setLoadingComparison(true);
    getFromOpenElisServer("/rest/compliance/dashboard/summary?" + q, (r) => {
      setSummary(r);
      setLoadingSummary(false);
    });
    getFromOpenElisServer("/rest/compliance/dashboard/trend?" + q, (r) => {
      setTrend(r);
      setLoadingTrend(false);
    });
    getFromOpenElisServer(
      "/rest/compliance/dashboard/sites/comparison?" + q,
      (r) => {
        setComparison(r);
        setLoadingComparison(false);
      },
    );
    fetchExceedances(q, 0, excPageSize);
  }, [buildQuery, fetchExceedances, excPageSize]);

  const fetchDrilldown = useCallback(
    (siteId) => {
      setLoadingDrilldown(true);
      setSelectedSite(siteId);
      getFromOpenElisServer(
        "/rest/compliance/dashboard/sites/" +
          siteId +
          "/parameters?" +
          buildQuery(),
        (r) => {
          setSiteParams(r);
          setLoadingDrilldown(false);
        },
      );
    },
    [buildQuery],
  );

  const trendData = React.useMemo(() => {
    if (!trend || !trend.series) return [];
    return trend.series.flatMap((site) =>
      (site.dataPoints || []).map((pt) => ({
        group: site.siteName,
        date: pt.month,
        value: pt.complianceRate,
      })),
    );
  }, [trend]);

  const drilldownData = React.useMemo(() => {
    if (!siteParams || !siteParams.parameters) return [];
    return siteParams.parameters
      .filter((p) => !p.descriptive)
      .flatMap((param) =>
        (param.dataPoints || []).map((pt) => ({
          group: param.displayName,
          date: pt.month,
          value: pt.avgValue,
        })),
      );
  }, [siteParams]);

  const comparisonData = React.useMemo(() => {
    if (!Array.isArray(comparison)) return [];
    return comparison.map((s) => ({
      group: s.siteName,
      value: s.complianceRate,
    }));
  }, [comparison]);

  const trendOptions = {
    title: intl.formatMessage({ id: "compliance.dashboard.chart.trend.title" }),
    axes: {
      bottom: { title: "", mapsTo: "date", scaleType: "labels" },
      left: {
        title: "%",
        mapsTo: "value",
        scaleType: "linear",
        domain: [0, 100],
      },
    },
    height: "320px",
    curve: "curveMonotoneX",
  };

  const drilldownOptions = {
    title: siteParams
      ? intl.formatMessage({
          id: "compliance.dashboard.chart.drilldown.title",
        }) +
        " — " +
        siteParams.siteName
      : intl.formatMessage({
          id: "compliance.dashboard.chart.drilldown.title",
        }),
    axes: {
      bottom: { title: "", mapsTo: "date", scaleType: "labels" },
      left: { mapsTo: "value", scaleType: "linear" },
    },
    height: "300px",
  };

  const comparisonOptions = {
    title: intl.formatMessage({
      id: "compliance.dashboard.chart.comparison.title",
    }),
    axes: {
      left: { mapsTo: "group", scaleType: "labels" },
      bottom: { mapsTo: "value", scaleType: "linear", domain: [0, 100] },
    },
    orientation: "horizontal",
    height: "280px",
    legend: { enabled: false },
  };

  const handleExport = async () => {
    setExportLoading(true);
    try {
      const labName = await new Promise((resolve) =>
        getFromOpenElisServer("/rest/site-information?name=siteName", (r) =>
          resolve((r && r.value) || "OpenELIS Lab"),
        ),
      );
      const preparedBy = (
        (userSessionDetails && userSessionDetails.firstName
          ? userSessionDetails.firstName
          : "") +
        " " +
        (userSessionDetails && userSessionDetails.lastName
          ? userSessionDetails.lastName
          : "")
      ).trim();
      const singleSite =
        selectedSiteIds.length === 1
          ? (sites.find((s) => String(s.id) === selectedSiteIds[0]) || {})
              .name || ""
          : "";
      const resolvedSiteName =
        (siteParams && siteParams.siteName) || singleSite;
      await generateCompliancePdf(
        { summary, trend, siteParams, comparison, exceedances },
        {
          labName,
          period: startDate + " – " + endDate,
          standard:
            (standards.find((s) => String(s.id) === standardId) || {}).name ||
            "All Standards",
          siteName: resolvedSiteName,
          preparedBy,
          trendRef,
          drilldownRef,
          comparisonRef,
        },
      );
    } finally {
      setExportLoading(false);
    }
  };

  const excRows = (
    exceedances && exceedances.items ? exceedances.items : []
  ).map((item, i) => ({
    id: String(i),
    date: item.date,
    labNumber: item.labNumber,
    siteName: item.siteName,
    parameter: item.parameter,
    result: item.result,
    threshold: item.threshold,
    status:
      item.status === "FAIL"
        ? React.createElement(Tag, { type: "red" }, "FAIL")
        : React.createElement(Tag, { type: "warm-gray" }, item.status),
  }));

  return (
    <div className="pageContent">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h2 style={{ marginBottom: "1rem" }}>
            <FormattedMessage id="compliance.dashboard.title" />
          </h2>
        </Column>

        {/* Filters: start date | end date | sites | standard | export  (3+3+4+4+2 = 16) */}
        <Column lg={3} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            value={startDate}
            onChange={([s]) => {
              if (s)
                setStartDate(
                  typeof s.toISOString === "function"
                    ? s.toISOString().slice(0, 10)
                    : s,
                );
            }}
          >
            <DatePickerInput
              id="dp-start"
              labelText={intl.formatMessage({
                id: "compliance.dashboard.filter.dateRange",
                defaultMessage: "Start date",
              })}
              placeholder="yyyy-mm-dd"
            />
          </DatePicker>
        </Column>
        <Column lg={3} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            value={endDate}
            onChange={([e]) => {
              if (e)
                setEndDate(
                  typeof e.toISOString === "function"
                    ? e.toISOString().slice(0, 10)
                    : e,
                );
            }}
          >
            <DatePickerInput
              id="dp-end"
              labelText={intl.formatMessage({
                id: "compliance.dashboard.filter.dateRangeEnd",
                defaultMessage: "End date",
              })}
              placeholder="yyyy-mm-dd"
            />
          </DatePicker>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <MultiSelect
            id="sites-multi"
            titleText={intl.formatMessage({
              id: "compliance.dashboard.filter.sites",
            })}
            items={sites}
            itemToString={(s) => (s ? s.name : "")}
            onChange={({ selectedItems }) =>
              setSelectedSiteIds(selectedItems.map((s) => String(s.id)))
            }
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="std-select"
            labelText={intl.formatMessage({
              id: "compliance.dashboard.filter.standard",
            })}
            value={standardId}
            onChange={(e) => setStandardId(e.target.value)}
          >
            <SelectItem value="" text="All Standards" />
            {standards.map((s) => (
              <SelectItem key={s.id} value={String(s.id)} text={s.name} />
            ))}
          </Select>
        </Column>
        <Column lg={2} md={8} sm={4} style={{ alignSelf: "flex-end" }}>
          <Button
            kind="primary"
            onClick={handleExport}
            disabled={exportLoading}
          >
            {exportLoading
              ? React.createElement(InlineLoading, {
                  description: "Generating…",
                })
              : React.createElement(FormattedMessage, {
                  id: "compliance.dashboard.export.pdf",
                })}
          </Button>
        </Column>

        {/* KPI Cards */}
        {KPI_KEYS.map(function (kpi) {
          return (
            <Column
              key={kpi.key}
              lg={4}
              md={4}
              sm={4}
              style={{ marginTop: "1rem" }}
            >
              <Tile>
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#525252",
                    marginBottom: "0.25rem",
                  }}
                >
                  <FormattedMessage id={kpi.i18n} />
                </p>
                {loadingSummary ? (
                  <InlineLoading />
                ) : (
                  <>
                    <p style={{ fontSize: "2rem", fontWeight: 600 }}>
                      {summary ? summary[kpi.key] + (kpi.suffix || "") : "—"}
                    </p>
                    {summary && summary.trend && (
                      <p
                        style={{
                          fontSize: "0.75rem",
                          color:
                            summary.trend[kpi.key] >= 0 ? "#198038" : "#da1e28",
                        }}
                      >
                        {summary.trend[kpi.key] >= 0 ? "↑" : "↓"}{" "}
                        {Math.abs(summary.trend[kpi.key])}
                        {kpi.suffix || ""}
                      </p>
                    )}
                  </>
                )}
              </Tile>
            </Column>
          );
        })}

        {/* Compliance Trend */}
        <Column lg={16} md={8} sm={4} style={{ marginTop: "1.5rem" }}>
          {loadingTrend ? (
            <InlineLoading description="Loading trend…" />
          ) : trendData.length === 0 ? (
            <Tile>
              <p>
                <FormattedMessage id="compliance.dashboard.empty" />
              </p>
            </Tile>
          ) : (
            <div ref={trendRef}>
              <LineChart data={trendData} options={trendOptions} />
            </div>
          )}
        </Column>

        {/* Site Comparison */}
        <Column lg={8} md={8} sm={4} style={{ marginTop: "1.5rem" }}>
          {loadingComparison ? (
            <InlineLoading description="Loading comparison…" />
          ) : (
            <div ref={comparisonRef}>
              <SimpleBarChart
                data={comparisonData}
                options={comparisonOptions}
              />
            </div>
          )}
        </Column>

        {/* Site Drill-Down — site selected via dropdown */}
        <Column lg={8} md={8} sm={4} style={{ marginTop: "1.5rem" }}>
          <Select
            id="drilldown-site-select"
            labelText={intl.formatMessage({
              id: "compliance.dashboard.filter.drilldownSite",
              defaultMessage: "Drill down by site",
            })}
            value={selectedSite || ""}
            onChange={(e) => {
              if (e.target.value) fetchDrilldown(e.target.value);
              else {
                setSelectedSite(null);
                setSiteParams(null);
              }
            }}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "compliance.dashboard.drilldown.placeholder",
                defaultMessage: "Select a site…",
              })}
            />
            {(Array.isArray(comparison) ? comparison : []).map((s) => (
              <SelectItem key={s.siteId} value={s.siteId} text={s.siteName} />
            ))}
          </Select>
          {loadingDrilldown ? (
            <InlineLoading
              description="Loading drill-down…"
              style={{ marginTop: "1rem" }}
            />
          ) : selectedSite && drilldownData.length > 0 ? (
            <>
              {siteParams &&
                siteParams.parameters &&
                siteParams.parameters.some((p) => p.descriptive) && (
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      margin: "0.5rem 0",
                    }}
                  >
                    <FormattedMessage id="compliance.dashboard.descriptive.excluded" />
                  </p>
                )}
              <div ref={drilldownRef}>
                <LineChart data={drilldownData} options={drilldownOptions} />
              </div>
            </>
          ) : selectedSite ? (
            <p
              style={{
                fontSize: "0.875rem",
                color: "#525252",
                marginTop: "1rem",
              }}
            >
              <FormattedMessage id="compliance.dashboard.empty" />
            </p>
          ) : null}
        </Column>

        {/* Exceedance Table */}
        <Column lg={16} md={8} sm={4} style={{ marginTop: "1.5rem" }}>
          <h4 style={{ marginBottom: "0.5rem" }}>
            <FormattedMessage id="compliance.dashboard.table.exceedances.title" />
          </h4>
          {loadingExceedances ? (
            <InlineLoading description="Loading exceedances…" />
          ) : (
            <>
              <DataTable rows={excRows} headers={EXC_HEADERS} isSortable>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
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
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
              <Pagination
                page={excPage}
                pageSize={excPageSize}
                pageSizes={[10, 20, 50]}
                totalItems={(exceedances && exceedances.totalCount) || 0}
                onChange={({ page: p, pageSize: s }) => {
                  setExcPage(p);
                  setExcPageSize(s);
                  fetchExceedances(buildQuery(), p - 1, s);
                }}
              />
            </>
          )}
        </Column>
      </Grid>
    </div>
  );
}
