import React, { useState, useCallback } from "react";
import { Tabs, TabList, Tab, TabPanels, TabPanel, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import TATFilterBar, { SEGMENTS } from "./TATFilterBar";
import TATSummaryTab from "./TATSummaryTab";
import TATDetailListTab from "./TATDetailListTab";
import TATTrendsTab from "./TATTrendsTab";
import TATExport from "./TATExport";
import PageBreadCrumb from "../../common/PageBreadCrumb";

function TATReport() {
  const intl = useIntl();
  const [filters, setFilters] = useState(null);
  const [summaryData, setSummaryData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const buildQueryString = useCallback((f, extra = "") => {
    if (!f) return "";
    let qs = `fromDate=${f.fromDate}&toDate=${f.toDate}&segment=${f.segment}&calculationMode=${f.calculationMode}`;
    if (f.priority) qs += `&priority=${f.priority}`;
    if (f.includeCancelled) qs += "&includeCancelled=true";
    if (f.labUnitIds?.length) qs += `&labUnitIds=${f.labUnitIds.join(",")}`;
    if (f.testIds?.length) qs += `&testIds=${f.testIds.join(",")}`;
    if (f.sampleTypeId) qs += `&sampleTypeId=${f.sampleTypeId}`;
    if (f.orderingSiteId) qs += `&orderingSiteId=${f.orderingSiteId}`;
    return qs + extra;
  }, []);

  const handleGenerateReport = useCallback(
    (newFilters) => {
      setFilters(newFilters);
      setLoading(true);
      setError(null);
      setSummaryData(null);
      const qs = buildQueryString(newFilters);
      getFromOpenElisServer(`/rest/reports/tat/summary?${qs}`, (res) => {
        if (res) {
          setSummaryData(res);
        } else {
          setError(intl.formatMessage({ id: "reports.tat.loadError" }));
        }
        setLoading(false);
      });
    },
    [buildQueryString, intl],
  );

  const breadcrumb = [
    { label: intl.formatMessage({ id: "reports.tat.home" }), link: "/" },
    {
      label: intl.formatMessage({ id: "reports.tat.title" }),
      link: "/TATReport",
    },
  ];

  return (
    <div data-testid="tat-report">
      <PageBreadCrumb breadcrumbs={breadcrumb} />
      <div style={{ padding: "0 1rem" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "1rem",
          }}
        >
          <div>
            <h2>
              <FormattedMessage id="reports.tat.title" />
            </h2>
            <p style={{ color: "var(--cds-text-secondary)", fontSize: "14px" }}>
              <FormattedMessage id="reports.tat.description" />
            </p>
          </div>
          {filters && <TATExport filters={filters} buildQueryString={buildQueryString} />}
        </div>

        <TATFilterBar onGenerate={handleGenerateReport} />

        {filters && (
          <div
            style={{
              display: "flex",
              gap: "0.5rem",
              marginBottom: "1rem",
              flexWrap: "wrap",
            }}
            data-testid="filter-summary-badges"
          >
            <Tag type="blue">
              {intl.formatMessage({
                id: SEGMENTS.find((s) => s.id === filters.segment)?.labelKey || "reports.tat.segment.receiptToValidation",
              })}
            </Tag>
            <Tag type={filters.calculationMode === "WORKING_TIME" ? "purple" : "gray"}>
              {filters.calculationMode === "WORKING_TIME"
                ? intl.formatMessage({ id: "reports.tat.workingTime" })
                : intl.formatMessage({ id: "reports.tat.calendarTime" })}
            </Tag>
            <Tag type="gray">
              {filters.fromDate} — {filters.toDate}
            </Tag>
          </div>
        )}

        {error && (
          <div
            style={{
              color: "var(--cds-support-error)",
              padding: "1rem",
              marginBottom: "1rem",
            }}
          >
            {error}
          </div>
        )}

        <Tabs>
          <TabList aria-label="TAT Report tabs">
            <Tab data-testid="tab-summary">
              <FormattedMessage id="reports.tat.summary" />
            </Tab>
            <Tab data-testid="tab-detail">
              <FormattedMessage id="reports.tat.detailList" />
            </Tab>
            <Tab data-testid="tab-trends">
              <FormattedMessage id="reports.tat.trends" />
            </Tab>
          </TabList>
          <TabPanels>
            <TabPanel>
              <TATSummaryTab data={summaryData} loading={loading} filters={filters} />
            </TabPanel>
            <TabPanel>
              <TATDetailListTab filters={filters} buildQueryString={buildQueryString} />
            </TabPanel>
            <TabPanel>
              <TATTrendsTab filters={filters} buildQueryString={buildQueryString} />
            </TabPanel>
          </TabPanels>
        </Tabs>
      </div>
    </div>
  );
}

export default TATReport;
