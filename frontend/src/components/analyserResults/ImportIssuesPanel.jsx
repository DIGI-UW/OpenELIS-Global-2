import React, { useEffect, useMemo, useState } from "react";
import {
  Column,
  DataTable,
  Grid,
  Heading,
  InlineLoading,
  InlineNotification,
  Link,
  Section,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const formatEventType = (value = "") => {
  const words = value.toLowerCase().split("_").filter(Boolean);
  return words.length
    ? words
        .map((word, index) =>
          word === "ast" || word === "qc"
            ? word.toUpperCase()
            : index === 0
              ? word.charAt(0).toUpperCase() + word.slice(1)
              : word,
        )
        .join(" ")
    : "-";
};

const formatFailureReason = (intl, value) => {
  if (value === "AST_ANALYZER_RUN_NOT_MATCHED") {
    return intl.formatMessage({
      id: "analyzer.importIssues.failure.astRunNotMatched",
    });
  }
  return formatEventType(value);
};

const ImportIssuesTable = ({ headers, rows, title, description }) => (
  <DataTable rows={rows} headers={headers}>
    {({
      rows: tableRows,
      headers: tableHeaders,
      getHeaderProps,
      getRowProps,
    }) => (
      <TableContainer title={title} description={description}>
        <Table>
          <TableHead>
            <TableRow>
              {tableHeaders.map((header) => (
                <TableHeader key={header.key} {...getHeaderProps({ header })}>
                  {header.header}
                </TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {tableRows.map((row) => (
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
);

const ImportIssuesPanel = () => {
  const intl = useIntl();
  const [issues, setIssues] = useState({ eventRows: [], rows: [] });
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  useEffect(() => {
    getFromOpenElisServer("/rest/analyzer/import-issues", (response) => {
      const data = response?.status === "success" ? response.data : null;
      setIssues({
        eventRows: data?.eventRows || [],
        rows: data?.rows || [],
      });
      setLoadFailed(!data);
      setLoading(false);
    });
  }, []);

  const eventHeaders = useMemo(
    () => [
      {
        key: "eventType",
        header: intl.formatMessage({ id: "analyzer.importIssues.event" }),
      },
      {
        key: "sourceId",
        header: intl.formatMessage({ id: "analyzer.importIssues.source" }),
      },
      {
        key: "targetReference",
        header: intl.formatMessage({ id: "analyzer.importIssues.target" }),
      },
      {
        key: "failureReason",
        header: intl.formatMessage({ id: "analyzer.importIssues.reason" }),
      },
      {
        key: "receivedAt",
        header: intl.formatMessage({ id: "analyzer.importIssues.received" }),
      },
      {
        key: "action",
        header: intl.formatMessage({ id: "analyzer.importIssues.action" }),
      },
    ],
    [intl],
  );

  const stagingHeaders = useMemo(
    () => [
      {
        key: "accessionNumber",
        header: intl.formatMessage({ id: "analyzer.importIssues.accession" }),
      },
      {
        key: "testName",
        header: intl.formatMessage({ id: "analyzer.importIssues.test" }),
      },
      {
        key: "result",
        header: intl.formatMessage({ id: "analyzer.importIssues.result" }),
      },
      {
        key: "importIssueReason",
        header: intl.formatMessage({ id: "analyzer.importIssues.reason" }),
      },
    ],
    [intl],
  );

  const eventRows = issues.eventRows.map((event) => ({
    id: String(event.id),
    eventType: <Tag type="red">{formatEventType(event.eventType)}</Tag>,
    sourceId: event.sourceId || "-",
    targetReference: event.targetReference || "-",
    failureReason: formatFailureReason(intl, event.failureReason),
    receivedAt: event.receivedAt
      ? intl.formatDate(new Date(event.receivedAt), {
          dateStyle: "medium",
          timeStyle: "short",
        })
      : "-",
    action: event.analyzerId ? (
      <Link href={`/analyzers/${event.analyzerId}/mappings`}>
        {intl.formatMessage({ id: "analyzer.importIssues.openMappings" })}
      </Link>
    ) : (
      "-"
    ),
  }));

  const stagingRows = issues.rows.map((issue) => ({
    id: String(issue.id),
    accessionNumber: issue.accessionNumber || "-",
    testName: issue.testName || "-",
    result: [issue.result, issue.units].filter(Boolean).join(" ") || "-",
    importIssueReason: issue.importIssueReason || "-",
  }));

  return (
    <Grid fullWidth data-testid="analyzer-import-issues">
      <Column lg={16} md={8} sm={4}>
        <Section>
          <Heading>
            {intl.formatMessage({ id: "analyzer.importIssues.title" })}
          </Heading>
          <p>
            {intl.formatMessage({ id: "analyzer.importIssues.description" })}
          </p>
        </Section>
        {loading ? (
          <InlineLoading
            description={intl.formatMessage({
              id: "analyzer.importIssues.loading",
            })}
          />
        ) : loadFailed ? (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "analyzer.importIssues.loadFailed",
            })}
            hideCloseButton
          />
        ) : eventRows.length === 0 && stagingRows.length === 0 ? (
          <InlineNotification
            kind="success"
            title={intl.formatMessage({ id: "analyzer.importIssues.empty" })}
            hideCloseButton
          />
        ) : (
          <>
            {eventRows.length > 0 ? (
              <ImportIssuesTable
                headers={eventHeaders}
                rows={eventRows}
                title={intl.formatMessage({
                  id: "analyzer.importIssues.events.title",
                })}
                description={intl.formatMessage({
                  id: "analyzer.importIssues.events.description",
                })}
              />
            ) : null}
            {stagingRows.length > 0 ? (
              <ImportIssuesTable
                headers={stagingHeaders}
                rows={stagingRows}
                title={intl.formatMessage({
                  id: "analyzer.importIssues.staging.title",
                })}
                description={intl.formatMessage({
                  id: "analyzer.importIssues.staging.description",
                })}
              />
            ) : null}
          </>
        )}
      </Column>
    </Grid>
  );
};

export default ImportIssuesPanel;
