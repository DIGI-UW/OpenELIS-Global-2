import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { Link as RouterLink, useHistory, useLocation } from "react-router-dom";
import {
  Button,
  Column,
  DataTable,
  DataTableSkeleton,
  Grid,
  InlineLoading,
  InlineNotification,
  Pagination,
  Select,
  SelectItem,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer, postToOpenElisServerJsonResponse } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { AlertDialog, NotificationKinds } from "../../common/CustomNotification";

const PRODUCTS = ["REPORT", "ALERT", "NOTICE", "TRIGGER", "TEST"];
const STATUSES = ["QUEUED", "SENT", "FAILED", "SKIPPED"];
const CHANNELS = ["EMAIL", "SMS", "WHATSAPP"];
const PAGE_SIZES = [10, 20, 50, 100];
const QUEUED_REFRESH_MS = 5000;
const STATUS_TAG = { QUEUED: "blue", SENT: "green", FAILED: "red", SKIPPED: "gray" };

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.reports.sentMessages", link: "/SentMessages" },
];

function formatDateTime(iso) {
  if (!iso) {
    return "";
  }
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString();
}

/** Hides most of an address so a shared screen does not show it in full. */
export function maskAddress(value) {
  if (!value) {
    return "";
  }
  const at = value.indexOf("@");
  if (at > 0) {
    const name = value.slice(0, at);
    return `${name.slice(0, 2)}${"•".repeat(Math.max(2, name.length - 2))}${value.slice(at)}`;
  }
  const digits = value.replace(/\s/g, "");
  if (digits.length <= 4) {
    return digits;
  }
  return `${digits.slice(0, 4)}${"•".repeat(digits.length - 7 > 0 ? digits.length - 7 : 2)}${digits.slice(-3)}`;
}

function readFilters(search) {
  const params = new URLSearchParams(search);
  return {
    product: PRODUCTS.includes((params.get("product") || "").toUpperCase())
      ? params.get("product").toUpperCase()
      : "",
    status: STATUSES.includes((params.get("status") || "").toUpperCase())
      ? params.get("status").toUpperCase()
      : "",
    channel: CHANNELS.includes((params.get("channel") || "").toUpperCase())
      ? params.get("channel").toUpperCase()
      : "",
    from: params.get("from") || "",
    to: params.get("to") || "",
    labNo: params.get("labNo") || "",
    recipient: params.get("recipient") || "",
  };
}

export default function SentMessages() {
  const intl = useIntl();
  const location = useLocation();
  const history = useHistory();
  const { notificationVisible, setNotificationVisible, addNotification } = useContext(NotificationContext);

  const [filters, setFilters] = useState(() => readFilters(location.search));
  const [typed, setTyped] = useState(() => readFilters(location.search));
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [details, setDetails] = useState({});
  const [resendingId, setResendingId] = useState(null);

  const load = useCallback(() => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    params.set("page", String(page));
    params.set("pageSize", String(pageSize));
    setLoading(true);
    setLoadError(false);
    getFromOpenElisServer(`/rest/delivery/messages?${params.toString()}`, (response) => {
      if (!response || !Array.isArray(response.rows)) {
        setLoadError(true);
        setLoading(false);
        return;
      }
      setData(response);
      setLoading(false);
    });
  }, [filters, page, pageSize]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const hasQueued = Boolean(data && data.rows.some((row) => row.status === "QUEUED"));
  useEffect(() => {
    if (!hasQueued) {
      return undefined;
    }
    const timer = setInterval(load, QUEUED_REFRESH_MS);
    return () => clearInterval(timer);
  }, [hasQueued, load]);

  const applyFilters = (next) => {
    setFilters(next);
    setPage(1);
    const params = new URLSearchParams();
    Object.entries(next).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    const search = params.toString();
    history.replace(search ? `${location.pathname}?${search}` : location.pathname);
  };

  const changeSelect = (key, value) => {
    const next = { ...typed, [key]: value };
    setTyped(next);
    applyFilters(next);
  };

  const submitTyped = (event) => {
    event.preventDefault();
    applyFilters(typed);
  };

  const clearFilters = () => {
    const empty = readFilters("");
    setTyped(empty);
    applyFilters(empty);
  };

  const loadDetail = useCallback((id) => {
    setDetails((current) => ({ ...current, [id]: { loading: true } }));
    getFromOpenElisServer(`/rest/delivery/messages/${id}`, (detail) => {
      setDetails((current) => ({
        ...current,
        [id]: detail && detail.id ? { loading: false, detail } : { loading: false, error: true },
      }));
    });
  }, []);

  const resend = (row) => {
    setResendingId(row.id);
    postToOpenElisServerJsonResponse(`/rest/delivery/messages/${row.id}/resend`, JSON.stringify({}), (response) => {
      setResendingId(null);
      const queued = response && typeof response.id !== "undefined";
      addNotification({
        kind: queued ? NotificationKinds.success : NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: queued
          ? intl.formatMessage({ id: "delivery.sent.resend.done" })
          : (response && response.message) || intl.formatMessage({ id: "delivery.sent.resend.failed" }),
      });
      setNotificationVisible(true);
      if (queued) {
        if (page !== 1) {
          setPage(1);
        } else {
          load();
        }
      }
    });
  };

  const label = (prefix, value) =>
    value ? intl.formatMessage({ id: `${prefix}.${value.toLowerCase()}`, defaultMessage: value }) : "";

  const headers = useMemo(
    () => [
      { key: "time", header: intl.formatMessage({ id: "delivery.sent.col.time" }) },
      { key: "product", header: intl.formatMessage({ id: "delivery.sent.col.product" }) },
      { key: "labNo", header: intl.formatMessage({ id: "delivery.sent.col.labNo" }) },
      { key: "recipient", header: intl.formatMessage({ id: "delivery.sent.col.recipient" }) },
      { key: "channel", header: intl.formatMessage({ id: "delivery.sent.col.channel" }) },
      { key: "status", header: intl.formatMessage({ id: "delivery.sent.col.status" }) },
      { key: "attempts", header: intl.formatMessage({ id: "delivery.sent.col.attempts" }) },
      { key: "sentBy", header: intl.formatMessage({ id: "delivery.sent.col.sentBy" }) },
      { key: "action", header: intl.formatMessage({ id: "common.action", defaultMessage: "Action" }) },
    ],
    [intl],
  );

  const rows = (data?.rows || []).map((row) => ({ ...row, id: String(row.id) }));
  const activeFilters = Object.values(filters).some((value) => value);

  const renderDetail = (row) => {
    const entry = details[row.id];
    if (!entry || entry.loading) {
      return <InlineLoading description={intl.formatMessage({ id: "label.loading", defaultMessage: "Loading" })} />;
    }
    if (entry.error) {
      return (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "delivery.sent.detail.loadError" })}
          actions={<Button kind="ghost" size="sm" onClick={() => loadDetail(row.id)}>Retry</Button>}
        />
      );
    }
    const detail = entry.detail;
    return (
      <Grid narrow>
        <Column lg={8} md={8} sm={4}>
          <h5>
            <FormattedMessage id="delivery.sent.detail.subject" />
          </h5>
          <div style={{ fontFamily: "monospace", marginBottom: "0.5rem" }}>{detail.renderedSubject || "–"}</div>
          <h5>
            <FormattedMessage id="delivery.sent.detail.message" />
          </h5>
          <pre style={{ whiteSpace: "pre-wrap", fontFamily: "monospace", background: "#f4f4f4", padding: "0.5rem" }}>
            {detail.renderedMessage || "–"}
          </pre>
          {detail.ruleName && (
            <div>
              <FormattedMessage id="delivery.sent.detail.rule" />: {detail.ruleName}
            </div>
          )}
          {detail.reportVersionId && (
            <div>
              <FormattedMessage
                id="delivery.sent.detail.report"
                values={{ n: detail.reportVersionId, format: label("delivery.format", detail.reportFormat) }}
              />
            </div>
          )}
          {detail.labNumber && (
            <div style={{ display: "flex", gap: "1rem", marginTop: "0.5rem" }}>
              {detail.workflow && (
                <RouterLink to={`/order/${detail.workflow}/enter?labNumber=${encodeURIComponent(detail.labNumber)}`}>
                  <FormattedMessage id="delivery.sent.detail.openOrder" />
                </RouterLink>
              )}
              <RouterLink to={`/ReportPrintQueue?labNo=${encodeURIComponent(detail.labNumber)}`}>
                <FormattedMessage id="delivery.sent.detail.openQueue" />
              </RouterLink>
            </div>
          )}
        </Column>
        <Column lg={8} md={8} sm={4}>
          <h5>
            <FormattedMessage id="delivery.sent.detail.attempts" />
          </h5>
          {detail.attempts.length === 0 ? (
            <div>
              <FormattedMessage id="delivery.sent.detail.noAttempts" />
            </div>
          ) : (
            <StructuredListWrapper>
              <StructuredListBody>
                {detail.attempts.map((attempt, index) => (
                  <StructuredListRow key={`${attempt.channel}-${index}`}>
                    <StructuredListCell>{formatDateTime(attempt.attemptedAt)}</StructuredListCell>
                    <StructuredListCell>{label("delivery.channel", attempt.channel)}</StructuredListCell>
                    <StructuredListCell>
                      <Tag type={attempt.succeeded ? "green" : "red"} size="sm">
                        {attempt.succeeded
                          ? intl.formatMessage({ id: "delivery.status.sent" })
                          : intl.formatMessage({ id: "delivery.status.failed" })}
                      </Tag>
                    </StructuredListCell>
                    <StructuredListCell>{attempt.errorMessage || ""}</StructuredListCell>
                  </StructuredListRow>
                ))}
              </StructuredListBody>
            </StructuredListWrapper>
          )}
          {detail.channels.some((channel) => channel.status === "QUEUED" && channel.nextAttemptAt) && (
            <div>
              <FormattedMessage
                id="delivery.sent.detail.nextAttempt"
                values={{
                  time: formatDateTime(
                    detail.channels.find((channel) => channel.status === "QUEUED" && channel.nextAttemptAt)
                      .nextAttemptAt,
                  ),
                }}
              />
            </div>
          )}
        </Column>
      </Grid>
    );
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="orderLegendBody">
        <h2>
          <FormattedMessage id="delivery.sent.title" />
        </h2>
        <p style={{ color: "#525252", marginBottom: "1rem" }}>
          <FormattedMessage id="delivery.sent.subtitle" />
        </p>

        <form onSubmit={submitTyped}>
          <Grid narrow style={{ marginBottom: "1rem", alignItems: "flex-end" }}>
            <Column lg={3} md={4} sm={4}>
              <Select
                id="sent-product"
                labelText={intl.formatMessage({ id: "delivery.sent.filter.product" })}
                value={typed.product}
                onChange={(event) => changeSelect("product", event.target.value)}
              >
                <SelectItem value="" text={intl.formatMessage({ id: "delivery.sent.filter.all" })} />
                {PRODUCTS.map((product) => (
                  <SelectItem key={product} value={product} text={label("delivery.product", product)} />
                ))}
              </Select>
            </Column>
            <Column lg={3} md={4} sm={4}>
              <Select
                id="sent-status"
                labelText={intl.formatMessage({ id: "delivery.sent.filter.status" })}
                value={typed.status}
                onChange={(event) => changeSelect("status", event.target.value)}
              >
                <SelectItem value="" text={intl.formatMessage({ id: "delivery.sent.filter.all" })} />
                {STATUSES.map((status) => (
                  <SelectItem key={status} value={status} text={label("delivery.status", status)} />
                ))}
              </Select>
            </Column>
            <Column lg={3} md={4} sm={4}>
              <Select
                id="sent-channel"
                labelText={intl.formatMessage({ id: "delivery.sent.filter.channel" })}
                value={typed.channel}
                onChange={(event) => changeSelect("channel", event.target.value)}
              >
                <SelectItem value="" text={intl.formatMessage({ id: "delivery.sent.filter.all" })} />
                {CHANNELS.map((channel) => (
                  <SelectItem key={channel} value={channel} text={label("delivery.channel", channel)} />
                ))}
              </Select>
            </Column>
            <Column lg={3} md={4} sm={4}>
              <TextInput
                id="sent-from"
                type="date"
                labelText={intl.formatMessage({ id: "common.from", defaultMessage: "From" })}
                value={typed.from}
                onChange={(event) => setTyped({ ...typed, from: event.target.value })}
              />
            </Column>
            <Column lg={3} md={4} sm={4}>
              <TextInput
                id="sent-to"
                type="date"
                labelText={intl.formatMessage({ id: "delivery.sent.filter.to" })}
                value={typed.to}
                onChange={(event) => setTyped({ ...typed, to: event.target.value })}
              />
            </Column>
          </Grid>
          <Grid narrow style={{ marginBottom: "1rem", alignItems: "flex-end" }}>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="sent-labno"
                labelText={intl.formatMessage({ id: "common.labNumber", defaultMessage: "Lab Number" })}
                value={typed.labNo}
                onChange={(event) => setTyped({ ...typed, labNo: event.target.value })}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="sent-recipient"
                labelText={intl.formatMessage({ id: "delivery.sent.filter.recipient" })}
                value={typed.recipient}
                onChange={(event) => setTyped({ ...typed, recipient: event.target.value })}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <div style={{ display: "flex", gap: "0.5rem" }}>
                <Button type="submit" size="md">
                  <FormattedMessage id="label.button.search" defaultMessage="Search" />
                </Button>
                {activeFilters && (
                  <Button kind="ghost" size="md" onClick={clearFilters}>
                    <FormattedMessage id="reportQueue.filter.clear" defaultMessage="Clear filters" />
                  </Button>
                )}
              </div>
            </Column>
          </Grid>
        </form>

        {loadError && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "delivery.sent.loadError" })}
            actions={<Button kind="ghost" size="sm" onClick={load}>Retry</Button>}
          />
        )}

        {loading && !data ? (
          <DataTableSkeleton headers={headers} rowCount={5} showHeader={false} showToolbar={false} />
        ) : (
          <DataTable rows={rows} headers={headers} isSortable={false}>
            {({ rows: tableRows, headers: tableHeaders, getHeaderProps, getRowProps, getTableProps, getExpandHeaderProps }) => (
              <TableContainer>
                <Table {...getTableProps()} size="md">
                  <TableHead>
                    <TableRow>
                      <TableExpandHeader {...getExpandHeaderProps()} />
                      {tableHeaders.map((header) => (
                        <TableHeader key={header.key} {...getHeaderProps({ header })}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.length === 0 && !loading && (
                      <TableRow>
                        <TableCell colSpan={headers.length + 1} style={{ textAlign: "center", padding: "2rem" }}>
                          <FormattedMessage id="delivery.sent.empty" />
                        </TableCell>
                      </TableRow>
                    )}
                    {tableRows.map((tableRow) => {
                      const row = rows.find((r) => r.id === tableRow.id);
                      if (!row) {
                        return null;
                      }
                      const rowProps = getRowProps({ row: tableRow });
                      const canResend = row.status !== "QUEUED";
                      return (
                        <React.Fragment key={tableRow.id}>
                          <TableExpandRow
                            {...rowProps}
                            data-testid={`sent-row-${row.id}`}
                            onExpand={(event) => {
                              if (!tableRow.isExpanded && !details[row.id]) {
                                loadDetail(row.id);
                              }
                              rowProps.onExpand(event);
                            }}
                          >
                            <TableCell>{formatDateTime(row.firedAt)}</TableCell>
                            <TableCell>
                              <Tag type="cool-gray" size="sm">
                                {label("delivery.product", row.product)}
                              </Tag>
                              {row.ruleName && <div style={{ fontSize: "0.75rem" }}>{row.ruleName}</div>}
                              {!row.ruleName && row.product === "TRIGGER" && (
                                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>{row.eventCode}</div>
                              )}
                            </TableCell>
                            <TableCell>
                              <span style={{ fontFamily: "monospace" }}>{row.labNumber || "–"}</span>
                            </TableCell>
                            <TableCell>
                              {row.recipientDisplayName &&
                                row.recipientDisplayName !== (row.recipientEmail || row.recipientPhone) && (
                                  <div>{row.recipientDisplayName}</div>
                                )}
                              <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                                {maskAddress(row.recipientEmail || row.recipientPhone) || "–"}
                              </div>
                            </TableCell>
                            <TableCell>
                              {row.channels.map((channel) => (
                                <div key={channel.channel}>{label("delivery.channel", channel.channel)}</div>
                              ))}
                            </TableCell>
                            <TableCell>
                              <Tag type={STATUS_TAG[row.status] || "gray"} size="sm" data-testid={`sent-status-${row.id}`}>
                                {label("delivery.status", row.status)}
                              </Tag>
                              {row.channels
                                .filter((channel) => channel.errorMessage)
                                .map((channel) => (
                                  <div key={channel.channel} style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                                    {channel.errorMessage}
                                  </div>
                                ))}
                            </TableCell>
                            <TableCell>{row.channels.reduce((sum, channel) => sum + channel.attempts, 0)}</TableCell>
                            <TableCell>
                              {row.automatic ? intl.formatMessage({ id: "delivery.sent.sentBy.auto" }) : row.sentBy}
                            </TableCell>
                            <TableCell>
                              {canResend && (
                                <Button
                                  kind="tertiary"
                                  size="sm"
                                  disabled={resendingId === row.id}
                                  onClick={() => resend(row)}
                                  data-testid={`resend-${row.id}`}
                                >
                                  {resendingId === row.id ? (
                                    <InlineLoading description="" />
                                  ) : (
                                    <FormattedMessage id="delivery.sent.resend" />
                                  )}
                                </Button>
                              )}
                            </TableCell>
                          </TableExpandRow>
                          <TableExpandedRow colSpan={headers.length + 1}>{renderDetail(row)}</TableExpandedRow>
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}

        <Pagination
          key={`sent-page-size-${pageSize}`}
          page={page}
          pageSize={pageSize}
          pageSizes={PAGE_SIZES}
          totalItems={data ? data.total : 0}
          onChange={({ page: nextPage, pageSize: nextSize }) => {
            if (nextSize !== pageSize) {
              setPageSize(nextSize);
              setPage(1);
            } else {
              setPage(nextPage);
            }
          }}
        />
      </div>
    </>
  );
}
