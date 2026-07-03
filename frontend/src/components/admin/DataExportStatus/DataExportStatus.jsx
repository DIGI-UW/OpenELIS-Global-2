import React, { useEffect, useMemo, useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Loading,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableContainer,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Tag,
  Button,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "dataexport.status.title",
    link: "/MasterListsPage/dataExportStatus",
  },
];

const REFRESH_INTERVAL_MS = 30000;
const ATTEMPT_LIMIT = 20;

function statusTagType(status, failedLast24h) {
  if (status === "SUCCEEDED" && failedLast24h === 0) return "green";
  if (status === "SUCCEEDED" && failedLast24h > 0) return "teal";
  if (status === "FAILED" || status === "INCOMPLETE") return "red";
  if (status == null) return "gray";
  return "blue";
}

function attemptStatusTagType(status) {
  if (status === "SUCCEEDED") return "green";
  if (status === "FAILED") return "red";
  if (status === "INCOMPLETE") return "magenta";
  return "blue";
}

function formatInstant(intl, value) {
  if (!value) {
    return intl.formatMessage({ id: "dataexport.status.never" });
  }
  return new Date(value).toLocaleString(intl.locale);
}

function formatOptionalInstant(intl, value) {
  if (!value) {
    return intl.formatMessage({ id: "dataexport.status.placeholderDash" });
  }
  return new Date(value).toLocaleString(intl.locale);
}

function formatDuration(intl, ms) {
  if (ms == null) {
    return intl.formatMessage({ id: "dataexport.status.placeholderDash" });
  }
  if (ms < 1000) {
    return intl.formatMessage(
      { id: "dataexport.status.duration.ms" },
      { value: intl.formatNumber(ms, { maximumFractionDigits: 0 }) },
    );
  }
  return intl.formatMessage(
    { id: "dataexport.status.duration.s" },
    {
      value: intl.formatNumber(ms / 1000, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    },
  );
}

function formatInterval(intl, minutes) {
  if (minutes == null) {
    return intl.formatMessage({ id: "dataexport.status.placeholderDash" });
  }
  return intl.formatMessage(
    { id: "dataexport.status.intervalValue" },
    { value: intl.formatNumber(minutes, { maximumFractionDigits: 0 }) },
  );
}

function AttemptHistory({ taskId }) {
  const intl = useIntl();
  const [attempts, setAttempts] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/DataExportStatus/${taskId}/attempts?limit=${ATTEMPT_LIMIT}`,
      (res) => {
        if (Array.isArray(res)) {
          setAttempts(res);
        } else {
          setAttempts([]);
        }
        setLoading(false);
      },
    );
  }, [taskId]);

  if (loading) {
    return (
      <InlineLoading
        description={intl.formatMessage({
          id: "dataexport.status.loadingAttempts",
        })}
      />
    );
  }

  if (!attempts || attempts.length === 0) {
    return (
      <p>
        <FormattedMessage id="dataexport.status.noAttempts" />
      </p>
    );
  }

  return (
    <Table size="sm">
      <TableHead>
        <TableRow>
          <TableHeader>
            <FormattedMessage id="dataexport.status.attempt.startTime" />
          </TableHeader>
          <TableHeader>
            <FormattedMessage id="dataexport.status.attempt.endTime" />
          </TableHeader>
          <TableHeader>
            <FormattedMessage id="dataexport.status.attempt.duration" />
          </TableHeader>
          <TableHeader>
            <FormattedMessage id="dataexport.status.attempt.status" />
          </TableHeader>
        </TableRow>
      </TableHead>
      <TableBody>
        {attempts.map((a) => (
          <TableRow key={a.id}>
            <TableCell>{formatOptionalInstant(intl, a.startTime)}</TableCell>
            <TableCell>{formatOptionalInstant(intl, a.endTime)}</TableCell>
            <TableCell>{formatDuration(intl, a.durationMs)}</TableCell>
            <TableCell>
              <Tag type={attemptStatusTagType(a.status)}>{a.status}</Tag>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function DataExportStatus() {
  const intl = useIntl();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastRefreshed, setLastRefreshed] = useState(null);
  const [triggeringId, setTriggeringId] = useState(null);

  const fetchStatuses = () => {
    getFromOpenElisServer("/rest/DataExportStatus", (res) => {
      if (Array.isArray(res)) {
        setRows(res);
        setLastRefreshed(new Date());
      }
      setLoading(false);
    });
  };

  const triggerRetry = (taskId) => {
    if (triggeringId != null) return;
    setTriggeringId(taskId);
    postToOpenElisServerFullResponse(
      `/rest/DataExportStatus/${taskId}/trigger`,
      "",
      (response) => {
        setTriggeringId(null);
        if (response && response.ok) {
          fetchStatuses();
        }
      },
    );
  };

  useEffect(() => {
    fetchStatuses();
    const handle = setInterval(fetchStatuses, REFRESH_INTERVAL_MS);
    return () => clearInterval(handle);
  }, []);

  const headers = [
    {
      key: "endpoint",
      header: intl.formatMessage({ id: "dataexport.status.endpoint" }),
    },
    {
      key: "lastStatus",
      header: intl.formatMessage({ id: "dataexport.status.lastStatus" }),
    },
    {
      key: "lastSuccess",
      header: intl.formatMessage({ id: "dataexport.status.lastSuccess" }),
    },
    {
      key: "lastAttempt",
      header: intl.formatMessage({ id: "dataexport.status.lastAttempt" }),
    },
    {
      key: "failedLast24h",
      header: intl.formatMessage({ id: "dataexport.status.failed24h" }),
    },
    {
      key: "intervalMin",
      header: intl.formatMessage({ id: "dataexport.status.intervalMin" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "dataexport.status.actions" }),
    },
  ];

  const tableRows = rows.map((r) => ({
    id: String(r.id),
    endpoint: r.endpoint,
    lastStatus:
      r.lastStatus ||
      intl.formatMessage({ id: "dataexport.status.noAttempts" }),
    lastSuccess: formatInstant(intl, r.lastSuccess),
    lastAttempt: formatInstant(intl, r.lastAttempt),
    failedLast24h: intl.formatMessage(
      { id: "dataexport.status.failed24hValue" },
      {
        failed: intl.formatNumber(r.failedLast24h),
        total: intl.formatNumber(r.totalLast24h),
      },
    ),
    intervalMin: formatInterval(intl, r.maxIntervalMinutes),
    actions: "",
  }));

  const sourceById = useMemo(
    () => Object.fromEntries(rows.map((r) => [String(r.id), r])),
    [rows],
  );

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="dataexport.status.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <br />
      {loading && (
        <Loading
          description={intl.formatMessage({ id: "loading" })}
          withOverlay={false}
        />
      )}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <DataTable rows={tableRows} headers={headers}>
            {({
              rows: dtRows,
              headers: dtHeaders,
              getHeaderProps,
              getRowProps,
              getTableProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableExpandHeader />
                      {dtHeaders.map((header) => (
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
                    {dtRows.length === 0 && !loading && (
                      <TableRow>
                        <TableCell colSpan={dtHeaders.length + 1}>
                          <FormattedMessage id="dataexport.status.empty" />
                        </TableCell>
                      </TableRow>
                    )}
                    {dtRows.map((row) => {
                      const source = sourceById[row.id];
                      return (
                        <React.Fragment key={row.id}>
                          <TableExpandRow {...getRowProps({ row })}>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "lastStatus") {
                                return (
                                  <TableCell key={cell.id}>
                                    <Tag
                                      type={statusTagType(
                                        source?.lastStatus,
                                        source?.failedLast24h ?? 0,
                                      )}
                                    >
                                      {cell.value}
                                    </Tag>
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "actions") {
                                const isTriggering =
                                  triggeringId === source?.id;
                                return (
                                  <TableCell key={cell.id}>
                                    {isTriggering ? (
                                      <InlineLoading
                                        description={intl.formatMessage({
                                          id: "dataexport.status.retryInFlight",
                                        })}
                                      />
                                    ) : (
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        disabled={
                                          !source ||
                                          triggeringId != null ||
                                          loading
                                        }
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          if (source) triggerRetry(source.id);
                                        }}
                                        data-cy={`retry-${source?.id}`}
                                      >
                                        <FormattedMessage id="dataexport.status.retry" />
                                      </Button>
                                    )}
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                          </TableExpandRow>
                          <TableExpandedRow colSpan={dtHeaders.length + 1}>
                            {row.isExpanded && source && (
                              <AttemptHistory taskId={source.id} />
                            )}
                          </TableExpandedRow>
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <br />
          <Button kind="ghost" onClick={fetchStatuses} size="sm">
            <FormattedMessage id="dataexport.status.refresh" />
          </Button>
          {lastRefreshed && (
            <span
              style={{
                marginLeft: "1rem",
                color: "var(--cds-text-secondary)",
              }}
            >
              <FormattedMessage id="dataexport.status.lastRefreshed" />:{" "}
              {lastRefreshed.toLocaleTimeString(intl.locale)}
            </span>
          )}
        </Column>
      </Grid>
    </>
  );
}

export default DataExportStatus;
