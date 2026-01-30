import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Tile,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Button,
  Loading,
  InlineNotification,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { Renew, ChartBar } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import "../../../Style.css";

// Helper to format date as ISO string (YYYY-MM-DD)
const formatDateISO = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

// Get default date range (last 30 days)
const getDefaultDateRange = () => {
  const today = new Date();
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(today.getDate() - 30);
  return {
    start: formatDateISO(thirtyDaysAgo),
    end: formatDateISO(today),
  };
};

/**
 * TBReportingPage - Dashboard for TB laboratory workflow metrics
 *
 * Displays key performance indicators including:
 * - Sample intake metrics (by specimen type, facility)
 * - QC pass/fail rates
 * - Culture positivity rates
 * - Smear/GeneXpert/DST results
 * - Turnaround times
 * - MDR/XDR resistance trends
 */
const TBReportingPage = ({ notebookId }) => {
  const intl = useIntl();
  const defaultRange = getDefaultDateRange();

  // State for date range filter (last 30 days by default)
  const [startDate, setStartDate] = useState(defaultRange.start);
  const [endDate, setEndDate] = useState(defaultRange.end);

  // State for dashboard data
  const [dashboardData, setDashboardData] = useState(null);
  const [intakeData, setIntakeData] = useState(null);
  const [qcData, setQcData] = useState(null);
  const [cultureData, setCultureData] = useState(null);
  const [smearData, setSmearData] = useState(null);
  const [geneXpertData, setGeneXpertData] = useState(null);
  const [dstData, setDstData] = useState(null);
  const [tatData, setTatData] = useState(null);
  const [resistanceData, setResistanceData] = useState(null);
  const [disposalData, setDisposalData] = useState(null);

  // Loading and error states
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Active tab
  const [activeTab, setActiveTab] = useState(0);

  // Build query params
  const queryParams = useMemo(() => {
    return `?startDate=${startDate}&endDate=${endDate}`;
  }, [startDate, endDate]);

  // Fetch dashboard summary
  const fetchDashboardData = useCallback(() => {
    if (!notebookId) return;

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/tb/reporting/${notebookId}/dashboard${queryParams}`,
      (response) => {
        if (response) {
          setDashboardData(response);
        }
        setLoading(false);
      },
    );
  }, [notebookId, queryParams]);

  // Fetch detailed metrics based on active tab
  const fetchDetailedMetrics = useCallback(() => {
    if (!notebookId) return;

    const endpoints = {
      0: {
        url: `/rest/tb/reporting/${notebookId}/intake`,
        setter: setIntakeData,
      },
      1: { url: `/rest/tb/reporting/${notebookId}/qc`, setter: setQcData },
      2: {
        url: `/rest/tb/reporting/${notebookId}/culture`,
        setter: setCultureData,
      },
      3: {
        url: `/rest/tb/reporting/${notebookId}/smear`,
        setter: setSmearData,
      },
      4: {
        url: `/rest/tb/reporting/${notebookId}/genexpert`,
        setter: setGeneXpertData,
      },
      5: { url: `/rest/tb/reporting/${notebookId}/dst`, setter: setDstData },
      6: { url: `/rest/tb/reporting/${notebookId}/tat`, setter: setTatData },
      7: {
        url: `/rest/tb/reporting/${notebookId}/resistance`,
        setter: setResistanceData,
      },
      8: {
        url: `/rest/tb/reporting/${notebookId}/disposal`,
        setter: setDisposalData,
      },
    };

    const endpoint = endpoints[activeTab];
    if (endpoint) {
      getFromOpenElisServer(`${endpoint.url}${queryParams}`, (response) => {
        if (response) {
          endpoint.setter(response);
        }
      });
    }
  }, [notebookId, activeTab, queryParams]);

  // Initial load
  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  // Load detailed data when tab changes
  useEffect(() => {
    fetchDetailedMetrics();
  }, [fetchDetailedMetrics]);

  // Refresh all data
  const handleRefresh = () => {
    fetchDashboardData();
    fetchDetailedMetrics();
  };

  // Format percentage
  const formatPercent = (value) => {
    if (value === null || value === undefined) return "N/A";
    return `${Number(value).toFixed(1)}%`;
  };

  // Format number
  const formatNumber = (value) => {
    if (value === null || value === undefined) return "0";
    return Number(value).toLocaleString();
  };

  // Get tag type based on rate
  const getRateTagType = (rate, thresholds = { good: 80, warning: 60 }) => {
    if (rate >= thresholds.good) return "green";
    if (rate >= thresholds.warning) return "yellow";
    return "red";
  };

  // Render metric tile
  const renderMetricTile = (title, value, subtitle, tagType = null) => (
    <Tile className="metric-tile">
      <div className="metric-title">{title}</div>
      <div className="metric-value">
        {tagType ? (
          <Tag type={tagType} size="md">
            {value}
          </Tag>
        ) : (
          value
        )}
      </div>
      {subtitle && <div className="metric-subtitle">{subtitle}</div>}
    </Tile>
  );

  // Render dashboard summary tiles
  const renderDashboardSummary = () => {
    if (!dashboardData) return null;

    return (
      <Grid className="progress-tiles" narrow>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalSamples",
              defaultMessage: "Total Samples",
            }),
            formatNumber(dashboardData.totalSamplesReceived),
            intl.formatMessage({
              id: "tb.reporting.received",
              defaultMessage: "Received",
            }),
          )}
        </Column>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.qcPassRate",
              defaultMessage: "QC Pass Rate",
            }),
            formatPercent(dashboardData.qcPassRate),
            intl.formatMessage({
              id: "tb.reporting.qualityCheck",
              defaultMessage: "Quality Check",
            }),
            getRateTagType(dashboardData.qcPassRate),
          )}
        </Column>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.culturePositivity",
              defaultMessage: "Culture Positivity",
            }),
            formatPercent(dashboardData.culturePositivityRate),
            intl.formatMessage({
              id: "tb.reporting.cultureResults",
              defaultMessage: "Culture Results",
            }),
            getRateTagType(dashboardData.culturePositivityRate, {
              good: 30,
              warning: 15,
            }),
          )}
        </Column>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.smearPositivity",
              defaultMessage: "Smear Positivity",
            }),
            formatPercent(dashboardData.smearPositivityRate),
            intl.formatMessage({
              id: "tb.reporting.smearResults",
              defaultMessage: "Smear Results",
            }),
            getRateTagType(dashboardData.smearPositivityRate, {
              good: 20,
              warning: 10,
            }),
          )}
        </Column>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.mtbDetection",
              defaultMessage: "MTB Detection",
            }),
            formatPercent(dashboardData.mtbDetectionRate),
            intl.formatMessage({
              id: "tb.reporting.geneXpert",
              defaultMessage: "GeneXpert",
            }),
          )}
        </Column>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.mdrRate",
              defaultMessage: "MDR-TB Rate",
            }),
            formatPercent(dashboardData.mdrRate),
            intl.formatMessage({
              id: "tb.reporting.drugResistance",
              defaultMessage: "Drug Resistance",
            }),
            getRateTagType(100 - (dashboardData.mdrRate || 0)),
          )}
        </Column>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.avgCultureTAT",
              defaultMessage: "Avg Culture TAT",
            }),
            dashboardData.avgCultureTatDays
              ? `${formatNumber(dashboardData.avgCultureTatDays)} days`
              : "N/A",
            intl.formatMessage({
              id: "tb.reporting.turnaroundTime",
              defaultMessage: "Turnaround Time",
            }),
          )}
        </Column>
        <Column lg={3} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.contaminationRate",
              defaultMessage: "Contamination Rate",
            }),
            formatPercent(dashboardData.cultureContaminationRate),
            intl.formatMessage({
              id: "tb.reporting.cultureQC",
              defaultMessage: "Culture QC",
            }),
            getRateTagType(
              100 - (dashboardData.cultureContaminationRate || 0),
              {
                good: 95,
                warning: 90,
              },
            ),
          )}
        </Column>
      </Grid>
    );
  };

  // Render intake metrics tab
  const renderIntakeTab = () => {
    if (!intakeData) return <Loading />;

    const specimenTypeRows = Object.entries(
      intakeData.bySpecimenType || {},
    ).map(([type, count], index) => ({
      id: `specimen-${index}`,
      specimenType: type || "Unknown",
      count: formatNumber(count),
      percentage: formatPercent((count / intakeData.totalReceived) * 100),
    }));

    const facilityRows = Object.entries(
      intakeData.byReferringFacility || {},
    ).map(([facility, count], index) => ({
      id: `facility-${index}`,
      facility: facility || "Unknown",
      count: formatNumber(count),
      percentage: formatPercent((count / intakeData.totalReceived) * 100),
    }));

    return (
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h4>
            <FormattedMessage
              id="tb.reporting.totalReceived"
              defaultMessage="Total Samples Received: {count}"
              values={{ count: formatNumber(intakeData.totalReceived) }}
            />
          </h4>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <DataTable
            rows={specimenTypeRows}
            headers={[
              {
                key: "specimenType",
                header: intl.formatMessage({
                  id: "tb.reporting.specimenType",
                  defaultMessage: "Specimen Type",
                }),
              },
              {
                key: "count",
                header: intl.formatMessage({
                  id: "tb.reporting.count",
                  defaultMessage: "Count",
                }),
              },
              {
                key: "percentage",
                header: intl.formatMessage({
                  id: "tb.reporting.percentage",
                  defaultMessage: "Percentage",
                }),
              },
            ]}
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
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
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <DataTable
            rows={facilityRows}
            headers={[
              {
                key: "facility",
                header: intl.formatMessage({
                  id: "tb.reporting.referringFacility",
                  defaultMessage: "Referring Facility",
                }),
              },
              {
                key: "count",
                header: intl.formatMessage({
                  id: "tb.reporting.count",
                  defaultMessage: "Count",
                }),
              },
              {
                key: "percentage",
                header: intl.formatMessage({
                  id: "tb.reporting.percentage",
                  defaultMessage: "Percentage",
                }),
              },
            ]}
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
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
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        </Column>
      </Grid>
    );
  };

  // Render QC metrics tab
  const renderQcTab = () => {
    if (!qcData) return <Loading />;

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalChecked",
              defaultMessage: "Total Checked",
            }),
            formatNumber(qcData.totalChecked),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.passRate",
              defaultMessage: "Pass Rate",
            }),
            formatPercent(qcData.passRate),
            null,
            getRateTagType(qcData.passRate),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.failRate",
              defaultMessage: "Fail Rate",
            }),
            formatPercent(qcData.failRate),
            null,
            getRateTagType(100 - qcData.failRate),
          )}
        </Column>
        <Column lg={16} md={8} sm={4}>
          <h5>
            <FormattedMessage
              id="tb.reporting.rejectionBreakdown"
              defaultMessage="Rejection Breakdown"
            />
          </h5>
          {qcData.rejectionBreakdown &&
          Object.keys(qcData.rejectionBreakdown).length > 0 ? (
            <DataTable
              rows={Object.entries(qcData.rejectionBreakdown).map(
                ([reason, count], index) => ({
                  id: `rejection-${index}`,
                  reason: reason || "Unknown",
                  count: formatNumber(count),
                }),
              )}
              headers={[
                {
                  key: "reason",
                  header: intl.formatMessage({
                    id: "tb.reporting.rejectionReason",
                    defaultMessage: "Rejection Reason",
                  }),
                },
                {
                  key: "count",
                  header: intl.formatMessage({
                    id: "tb.reporting.count",
                    defaultMessage: "Count",
                  }),
                },
              ]}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
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
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          ) : (
            <p>
              <FormattedMessage
                id="tb.reporting.noRejections"
                defaultMessage="No rejections in selected period"
              />
            </p>
          )}
        </Column>
      </Grid>
    );
  };

  // Render culture metrics tab
  const renderCultureTab = () => {
    if (!cultureData) return <Loading />;

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalWithResults",
              defaultMessage: "Total with Results",
            }),
            formatNumber(cultureData.totalWithResults),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.positivityRate",
              defaultMessage: "Positivity Rate",
            }),
            formatPercent(cultureData.positivityRate),
            null,
            "blue",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.negativityRate",
              defaultMessage: "Negativity Rate",
            }),
            formatPercent(cultureData.negativityRate),
            null,
            "green",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.contaminationRate",
              defaultMessage: "Contamination Rate",
            }),
            formatPercent(cultureData.contaminationRate),
            null,
            getRateTagType(100 - (cultureData.contaminationRate || 0), {
              good: 95,
              warning: 90,
            }),
          )}
        </Column>
        <Column lg={8} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.avgPositiveWeek",
              defaultMessage: "Avg Week to Positive",
            }),
            cultureData.avgPositiveWeek
              ? `Week ${Number(cultureData.avgPositiveWeek).toFixed(1)}`
              : "N/A",
            intl.formatMessage({
              id: "tb.reporting.timeToPositivity",
              defaultMessage: "Time to Positivity",
            }),
          )}
        </Column>
        <Column lg={8} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.avgCultureTATDays",
              defaultMessage: "Avg Culture TAT",
            }),
            cultureData.avgCultureTATDays
              ? `${formatNumber(cultureData.avgCultureTATDays)} days`
              : "N/A",
            intl.formatMessage({
              id: "tb.reporting.inoculationToResult",
              defaultMessage: "Inoculation to Result",
            }),
          )}
        </Column>
      </Grid>
    );
  };

  // Render smear metrics tab
  const renderSmearTab = () => {
    if (!smearData) return <Loading />;

    const gradingRows = Object.entries(smearData.byGrading || {}).map(
      ([grading, count], index) => ({
        id: `grading-${index}`,
        grading: grading || "Unknown",
        count: formatNumber(count),
        percentage: formatPercent((count / smearData.totalSmears) * 100),
      }),
    );

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalSmears",
              defaultMessage: "Total Smears",
            }),
            formatNumber(smearData.totalSmears),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.positivityRate",
              defaultMessage: "Positivity Rate",
            }),
            formatPercent(smearData.positivityRate),
            null,
            "blue",
          )}
        </Column>
        <Column lg={8} md={8} sm={4}>
          <h5>
            <FormattedMessage
              id="tb.reporting.gradingBreakdown"
              defaultMessage="Grading Breakdown"
            />
          </h5>
          <DataTable
            rows={gradingRows}
            headers={[
              {
                key: "grading",
                header: intl.formatMessage({
                  id: "tb.reporting.grading",
                  defaultMessage: "Grading",
                }),
              },
              {
                key: "count",
                header: intl.formatMessage({
                  id: "tb.reporting.count",
                  defaultMessage: "Count",
                }),
              },
              {
                key: "percentage",
                header: intl.formatMessage({
                  id: "tb.reporting.percentage",
                  defaultMessage: "Percentage",
                }),
              },
            ]}
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
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
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        </Column>
      </Grid>
    );
  };

  // Render GeneXpert metrics tab
  const renderGeneXpertTab = () => {
    if (!geneXpertData) return <Loading />;

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalTests",
              defaultMessage: "Total Tests",
            }),
            formatNumber(geneXpertData.totalTests),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.mtbDetectionRate",
              defaultMessage: "MTB Detection Rate",
            }),
            formatPercent(geneXpertData.mtbDetectionRate),
            null,
            "blue",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.rifResistanceRate",
              defaultMessage: "RIF Resistance Rate",
            }),
            formatPercent(geneXpertData.rifResistanceRate),
            null,
            getRateTagType(100 - (geneXpertData.rifResistanceRate || 0)),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.invalidRate",
              defaultMessage: "Invalid Rate",
            }),
            formatPercent(geneXpertData.invalidRate),
            null,
            getRateTagType(100 - (geneXpertData.invalidRate || 0), {
              good: 98,
              warning: 95,
            }),
          )}
        </Column>
      </Grid>
    );
  };

  // Render DST metrics tab
  const renderDstTab = () => {
    if (!dstData) return <Loading />;

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalDst",
              defaultMessage: "Total DST",
            }),
            formatNumber(dstData.totalDst),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.fullySensitive",
              defaultMessage: "Fully Sensitive",
            }),
            formatNumber(dstData.fullySensitiveCount),
            formatPercent(dstData.fullySensitiveRate),
            "green",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.mdrCount",
              defaultMessage: "MDR-TB Cases",
            }),
            formatNumber(dstData.mdrCount),
            formatPercent(dstData.mdrRate),
            "red",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.xdrCount",
              defaultMessage: "XDR-TB Cases",
            }),
            formatNumber(dstData.xdrCount),
            formatPercent(dstData.xdrRate),
            "magenta",
          )}
        </Column>
      </Grid>
    );
  };

  // Render TAT metrics tab
  const renderTatTab = () => {
    if (!tatData) return <Loading />;

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.smearTAT",
              defaultMessage: "Smear TAT",
            }),
            tatData.smearTATDays
              ? `${formatNumber(tatData.smearTATDays)} days`
              : "N/A",
            intl.formatMessage({
              id: "tb.reporting.receptionToResult",
              defaultMessage: "Reception to Result",
            }),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.geneXpertTAT",
              defaultMessage: "GeneXpert TAT",
            }),
            tatData.geneXpertTATDays
              ? `${formatNumber(tatData.geneXpertTATDays)} days`
              : "N/A",
            intl.formatMessage({
              id: "tb.reporting.receptionToResult",
              defaultMessage: "Reception to Result",
            }),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.cultureTAT",
              defaultMessage: "Culture TAT",
            }),
            tatData.cultureTATDays
              ? `${formatNumber(tatData.cultureTATDays)} days`
              : "N/A",
            intl.formatMessage({
              id: "tb.reporting.inoculationToResult",
              defaultMessage: "Inoculation to Result",
            }),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.dstTAT",
              defaultMessage: "DST TAT",
            }),
            tatData.dstTATDays
              ? `${formatNumber(tatData.dstTATDays)} days`
              : "N/A",
            intl.formatMessage({
              id: "tb.reporting.cultureToResult",
              defaultMessage: "Culture to Result",
            }),
          )}
        </Column>
      </Grid>
    );
  };

  // Render resistance trends tab
  const renderResistanceTab = () => {
    if (!resistanceData) return <Loading />;

    const facilityRows = Object.entries(resistanceData.byFacility || {}).map(
      ([facility, count], index) => ({
        id: `facility-${index}`,
        facility: facility || "Unknown",
        mdrCases: formatNumber(count),
      }),
    );

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalDst",
              defaultMessage: "Total DST",
            }),
            formatNumber(resistanceData.totalDst),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.mdrTbCount",
              defaultMessage: "MDR-TB Cases",
            }),
            formatNumber(resistanceData.mdrTbCount),
            formatPercent(resistanceData.mdrTbRate),
            "red",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.xdrTbCount",
              defaultMessage: "XDR-TB Cases",
            }),
            formatNumber(resistanceData.xdrTbCount),
            formatPercent(resistanceData.xdrTbRate),
            "magenta",
          )}
        </Column>
        {facilityRows.length > 0 && (
          <Column lg={16} md={8} sm={4}>
            <h5>
              <FormattedMessage
                id="tb.reporting.mdrByFacility"
                defaultMessage="MDR-TB Cases by Facility"
              />
            </h5>
            <DataTable
              rows={facilityRows}
              headers={[
                {
                  key: "facility",
                  header: intl.formatMessage({
                    id: "tb.reporting.facility",
                    defaultMessage: "Facility",
                  }),
                },
                {
                  key: "mdrCases",
                  header: intl.formatMessage({
                    id: "tb.reporting.mdrCases",
                    defaultMessage: "MDR Cases",
                  }),
                },
              ]}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
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
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          </Column>
        )}
      </Grid>
    );
  };

  // Render disposal & archiving metrics tab
  const renderDisposalTab = () => {
    if (!disposalData) return <Loading />;

    const reasonRows = Object.entries(disposalData.byDisposalReason || {}).map(
      ([reason, count], index) => ({
        id: `reason-${index}`,
        reason: reason || "Unknown",
        count: formatNumber(count),
        percentage: formatPercent((count / disposalData.totalProcessed) * 100),
      }),
    );

    const methodRows = Object.entries(disposalData.byDisposalMethod || {}).map(
      ([method, count], index) => ({
        id: `method-${index}`,
        method: method || "Unknown",
        count: formatNumber(count),
        percentage: formatPercent((count / disposalData.totalProcessed) * 100),
      }),
    );

    return (
      <Grid className="progress-tiles">
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.totalProcessed",
              defaultMessage: "Total Processed",
            }),
            formatNumber(disposalData.totalProcessed),
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.disposedCount",
              defaultMessage: "Disposed",
            }),
            formatNumber(disposalData.disposedCount),
            formatPercent(
              disposalData.totalProcessed > 0
                ? (disposalData.disposedCount / disposalData.totalProcessed) *
                    100
                : 0,
            ),
            "gray",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.archivedCount",
              defaultMessage: "Archived",
            }),
            formatNumber(disposalData.archivedCount),
            formatPercent(
              disposalData.totalProcessed > 0
                ? (disposalData.archivedCount / disposalData.totalProcessed) *
                    100
                : 0,
            ),
            "blue",
          )}
        </Column>
        <Column lg={4} md={4} sm={4}>
          {renderMetricTile(
            intl.formatMessage({
              id: "tb.reporting.biorepositoryTransfers",
              defaultMessage: "Biorepository Transfers",
            }),
            formatNumber(disposalData.biorepositoryTransferCount),
            intl.formatMessage({
              id: "tb.reporting.samplesTransferred",
              defaultMessage: "Samples Transferred",
            }),
            "purple",
          )}
        </Column>

        {reasonRows.length > 0 && (
          <Column lg={8} md={4} sm={4}>
            <h5>
              <FormattedMessage
                id="tb.reporting.byDisposalReason"
                defaultMessage="By Disposal Reason"
              />
            </h5>
            <DataTable
              rows={reasonRows}
              headers={[
                {
                  key: "reason",
                  header: intl.formatMessage({
                    id: "tb.reporting.reason",
                    defaultMessage: "Reason",
                  }),
                },
                {
                  key: "count",
                  header: intl.formatMessage({
                    id: "tb.reporting.count",
                    defaultMessage: "Count",
                  }),
                },
                {
                  key: "percentage",
                  header: intl.formatMessage({
                    id: "tb.reporting.percentage",
                    defaultMessage: "Percentage",
                  }),
                },
              ]}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
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
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          </Column>
        )}

        {methodRows.length > 0 && (
          <Column lg={8} md={4} sm={4}>
            <h5>
              <FormattedMessage
                id="tb.reporting.byDisposalMethod"
                defaultMessage="By Disposal Method"
              />
            </h5>
            <DataTable
              rows={methodRows}
              headers={[
                {
                  key: "method",
                  header: intl.formatMessage({
                    id: "tb.reporting.method",
                    defaultMessage: "Method",
                  }),
                },
                {
                  key: "count",
                  header: intl.formatMessage({
                    id: "tb.reporting.count",
                    defaultMessage: "Count",
                  }),
                },
                {
                  key: "percentage",
                  header: intl.formatMessage({
                    id: "tb.reporting.percentage",
                    defaultMessage: "Percentage",
                  }),
                },
              ]}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
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
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          </Column>
        )}
      </Grid>
    );
  };

  if (loading && !dashboardData) {
    return (
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <Loading description="Loading reporting data..." />
        </Column>
      </Grid>
    );
  }

  return (
    <div className="tb-reporting-page">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h3>
            <ChartBar size={24} style={{ marginRight: "8px" }} />
            <FormattedMessage
              id="tb.reporting.title"
              defaultMessage="TB Laboratory Reporting Dashboard"
            />
          </h3>
        </Column>

        {/* Date Range Filter */}
        <Column lg={8} md={4} sm={4}>
          <DatePicker
            datePickerType="range"
            dateFormat="Y-m-d"
            value={[startDate, endDate]}
            onChange={(dates) => {
              if (dates && dates.length >= 1) {
                const formatDate = (date) => {
                  if (!date) return "";
                  const d = new Date(date);
                  const year = d.getFullYear();
                  const month = String(d.getMonth() + 1).padStart(2, "0");
                  const day = String(d.getDate()).padStart(2, "0");
                  return `${year}-${month}-${day}`;
                };
                setStartDate(formatDate(dates[0]));
                setEndDate(dates[1] ? formatDate(dates[1]) : "");
              }
            }}
          >
            <DatePickerInput
              id="start-date"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({
                id: "tb.reporting.startDate",
                defaultMessage: "Start Date",
              })}
            />
            <DatePickerInput
              id="end-date"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({
                id: "tb.reporting.endDate",
                defaultMessage: "End Date",
              })}
            />
          </DatePicker>
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Button
            kind="tertiary"
            renderIcon={Renew}
            onClick={handleRefresh}
            style={{ marginTop: "24px" }}
          >
            <FormattedMessage
              id="tb.reporting.refresh"
              defaultMessage="Refresh"
            />
          </Button>
        </Column>

        {error && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "tb.reporting.error",
                defaultMessage: "Error",
              })}
              subtitle={error}
              onClose={() => setError(null)}
            />
          </Column>
        )}

        {/* Dashboard Summary */}
        <Column lg={16} md={8} sm={4}>
          <h4>
            <FormattedMessage
              id="tb.reporting.summary"
              defaultMessage="Summary"
            />
          </h4>
          {renderDashboardSummary()}
        </Column>

        {/* Detailed Metrics Tabs */}
        <Column lg={16} md={8} sm={4}>
          <Tabs
            selectedIndex={activeTab}
            onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
          >
            <TabList aria-label="TB Reporting Tabs">
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.intake"
                  defaultMessage="Sample Intake"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.qc"
                  defaultMessage="Quality Check"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.culture"
                  defaultMessage="Culture"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.smear"
                  defaultMessage="Smear"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.genexpert"
                  defaultMessage="GeneXpert"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.dst"
                  defaultMessage="DST"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.tat"
                  defaultMessage="Turnaround Time"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.resistance"
                  defaultMessage="Resistance Trends"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="tb.reporting.tab.disposal"
                  defaultMessage="Disposal & Archiving"
                />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>{renderIntakeTab()}</TabPanel>
              <TabPanel>{renderQcTab()}</TabPanel>
              <TabPanel>{renderCultureTab()}</TabPanel>
              <TabPanel>{renderSmearTab()}</TabPanel>
              <TabPanel>{renderGeneXpertTab()}</TabPanel>
              <TabPanel>{renderDstTab()}</TabPanel>
              <TabPanel>{renderTatTab()}</TabPanel>
              <TabPanel>{renderResistanceTab()}</TabPanel>
              <TabPanel>{renderDisposalTab()}</TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
};

export default TBReportingPage;
