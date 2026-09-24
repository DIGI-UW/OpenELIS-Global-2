import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  DataTable,
  Heading,
  InlineLoading,
  InlineNotification,
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
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const ENDPOINT = "/rest/analyzer/delivery-issues";

const hasMessage = (intl, id) =>
  Object.prototype.hasOwnProperty.call(intl.messages, id);

const formatReason = (intl, reason) => {
  if (!reason) {
    return intl.formatMessage({ id: "analyzer.deliveryIssues.reason.none" });
  }
  const id = `analyzer.deliveryIssues.reason.${reason}`;
  return hasMessage(intl, id)
    ? intl.formatMessage({ id })
    : intl.formatMessage(
        { id: "analyzer.deliveryIssues.reason.unknown" },
        { code: reason },
      );
};

const formatActionError = (intl, response) => {
  const id = response?.messageKey;
  return id && hasMessage(intl, id)
    ? intl.formatMessage({ id }, response.messageArgs)
    : intl.formatMessage({ id: "analyzer.deliveryIssues.error.actionFailed" });
};

const DeliveryIssuesPanel = () => {
  const intl = useIntl();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [pendingId, setPendingId] = useState(null);
  const [actionError, setActionError] = useState(null);

  const load = useCallback(() => {
    getFromOpenElisServer(ENDPOINT, (response) => {
      const data = response?.status === "success" ? response.data : null;
      setRows(data?.rows || []);
      setLoadFailed(!data);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const act = (row, action) => {
    setPendingId(row.id);
    setActionError(null);
    postToOpenElisServerJsonResponse(
      `${ENDPOINT}/${encodeURIComponent(row.id)}/${action}`,
      "{}",
      (response) => {
        setPendingId(null);
        if (response?.status !== "success") {
          setActionError(formatActionError(intl, response));
          return;
        }
        load();
      },
    );
  };

  const headers = useMemo(
    () =>
      ["analyzer", "received", "status", "reason", "detail", "actions"].map(
        (key) => ({
          key,
          header: intl.formatMessage({
            id: `analyzer.deliveryIssues.column.${key}`,
          }),
        }),
      ),
    [intl],
  );

  const tableRows = rows.map((row) => ({
    id: row.id,
    analyzer:
      row.analyzerName ||
      intl.formatMessage(
        { id: "analyzer.deliveryIssues.unrecognizedSender" },
        { source: row.sourceId || "-" },
      ),
    received: row.receivedAt
      ? intl.formatDate(new Date(row.receivedAt), {
          dateStyle: "medium",
          timeStyle: "short",
        })
      : "-",
    status: (
      <Tag type={row.state === "DMQ" ? "red" : "warm-gray"}>
        {intl.formatMessage({
          id:
            row.state === "DMQ"
              ? "analyzer.deliveryIssues.state.deadLettered"
              : "analyzer.deliveryIssues.state.retrying",
        })}
      </Tag>
    ),
    reason: formatReason(intl, row.failureReason),
    detail: row.lastError || "-",
    actions: row.actionable ? (
      <>
        <Button
          kind="ghost"
          size="sm"
          disabled={pendingId !== null}
          onClick={() => act(row, "retry")}
        >
          {intl.formatMessage({ id: "analyzer.deliveryIssues.retry" })}
        </Button>
        <Button
          kind="ghost"
          size="sm"
          disabled={pendingId !== null}
          onClick={() => act(row, "dismiss")}
        >
          {intl.formatMessage({ id: "analyzer.deliveryIssues.dismiss" })}
        </Button>
      </>
    ) : (
      "-"
    ),
  }));

  return (
    <Section
      level={3}
      data-testid="analyzer-delivery-issues"
      style={{ marginTop: "2rem" }}
    >
      <Heading>
        {intl.formatMessage({ id: "analyzer.deliveryIssues.title" })}
      </Heading>
      <p>{intl.formatMessage({ id: "analyzer.deliveryIssues.description" })}</p>
      {actionError && (
        <InlineNotification
          kind="error"
          lowContrast
          title={actionError}
          onCloseButtonClick={() => setActionError(null)}
        />
      )}
      {loading ? (
        <InlineLoading
          description={intl.formatMessage({
            id: "analyzer.deliveryIssues.loading",
          })}
        />
      ) : loadFailed ? (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzer.deliveryIssues.loadFailed",
          })}
        />
      ) : tableRows.length === 0 ? (
        <InlineNotification
          kind="success"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "analyzer.deliveryIssues.empty" })}
        />
      ) : (
        <DataTable rows={tableRows} headers={headers}>
          {({
            rows: dataRows,
            headers: dataHeaders,
            getHeaderProps,
            getRowProps,
          }) => (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    {dataHeaders.map((header) => (
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
                  {dataRows.map((row) => (
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
      )}
    </Section>
  );
};

export default DeliveryIssuesPanel;
