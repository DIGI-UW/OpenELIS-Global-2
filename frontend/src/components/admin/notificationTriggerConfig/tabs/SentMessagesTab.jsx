import React, { useContext, useEffect, useMemo, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import {
  Button,
  Column,
  ComposedModal,
  DataTable,
  Grid,
  InlineLoading,
  InlineNotification,
  Loading,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Pagination,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../../common/CustomNotification";
import { TRIGGER_DESCRIPTORS } from "../descriptors";

const STATUS_ALL = "";
const STATUS_SENT = "SENT";
const STATUS_FAILED = "FAILED";
const TYPE_ALL = "";

function ViewLogModal({ logId, open, onClose, onResend, resending }) {
  const intl = useIntl();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open || !logId) {
      setDetail(null);
      return;
    }
    setLoading(true);
    getFromOpenElisServer(
      `/rest/NotificationTriggerConfig/sent-messages/${logId}`,
      (res) => {
        setDetail(res || null);
        setLoading(false);
      },
    );
  }, [open, logId]);

  const handleCloseAndBlur = () => {
    // Mirrors the ErrorDetailsModal blur trick — avoids Carbon's aria-hidden
    // warning when the row-button that opened the modal still holds focus.
    if (
      typeof document !== "undefined" &&
      document.activeElement &&
      typeof document.activeElement.blur === "function"
    ) {
      document.activeElement.blur();
    }
    onClose();
  };

  const hasFailedChannel =
    detail?.channels?.some((c) => c.status === "FAILED") || false;

  return (
    <ComposedModal open={open} onClose={handleCloseAndBlur}>
      <ModalHeader
        title={intl.formatMessage({
          id: "notificationtrigger.sentmessages.viewlog.heading",
        })}
        label={detail?.eventCode || ""}
      />
      <ModalBody>
        {loading && <Loading withOverlay={false} small />}
        {!loading && detail && (
          <div
            style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
          >
            <div>
              <div
                style={{
                  fontSize: "0.875rem",
                  fontWeight: 600,
                  marginBottom: "0.25rem",
                }}
              >
                <FormattedMessage id="notificationtrigger.sentmessages.viewlog.subject" />
              </div>
              <div style={{ fontFamily: "monospace" }}>
                {detail.renderedSubject || ""}
              </div>
            </div>
            <div>
              <div
                style={{
                  fontSize: "0.875rem",
                  fontWeight: 600,
                  marginBottom: "0.25rem",
                }}
              >
                <FormattedMessage id="notificationtrigger.sentmessages.viewlog.message" />
              </div>
              <pre
                style={{
                  fontFamily: "monospace",
                  whiteSpace: "pre-wrap",
                  background: "#f4f4f4",
                  padding: "0.5rem",
                  margin: 0,
                  borderRadius: "4px",
                }}
              >
                {detail.renderedMessage || ""}
              </pre>
            </div>
            <div>
              <div
                style={{
                  fontSize: "0.875rem",
                  fontWeight: 600,
                  marginBottom: "0.25rem",
                }}
              >
                <FormattedMessage id="notificationtrigger.sentmessages.viewlog.channels" />
              </div>
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="notificationtrigger.sentmessages.viewlog.column.channel" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="notificationtrigger.sentmessages.viewlog.column.status" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="notificationtrigger.sentmessages.viewlog.column.attempts" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="notificationtrigger.sentmessages.viewlog.column.error" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {detail.channels?.map((c) => (
                    <TableRow key={c.channel}>
                      <TableCell>{c.channel}</TableCell>
                      <TableCell>
                        <Tag type={c.status === "SUCCESS" ? "green" : "red"}>
                          {c.status}
                        </Tag>
                      </TableCell>
                      <TableCell>{c.attempts}</TableCell>
                      <TableCell>{c.errorMessage || "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            {hasFailedChannel && (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title=""
                subtitle={intl.formatMessage({
                  id: "notificationtrigger.sentmessages.resend.disclaimer",
                })}
              />
            )}
          </div>
        )}
      </ModalBody>
      <ModalFooter>
        {hasFailedChannel && (
          <Button
            kind="primary"
            onClick={() => onResend(detail.id)}
            disabled={resending}
          >
            {resending ? (
              <InlineLoading description="Resending..." />
            ) : (
              <FormattedMessage id="notificationtrigger.sentmessages.action.resend" />
            )}
          </Button>
        )}
        <Button kind="secondary" onClick={handleCloseAndBlur}>
          <FormattedMessage id="label.button.close" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}

export default function SentMessagesTab() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [messages, setMessages] = useState([]);
  const [totalItems, setTotalItems] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [eventCodeFilter, setEventCodeFilter] = useState(TYPE_ALL);
  const [statusFilter, setStatusFilter] = useState(STATUS_ALL);
  const [loading, setLoading] = useState(true);
  const [resendingId, setResendingId] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedLogId, setSelectedLogId] = useState(null);

  const fetchPage = (p, sz, evt, st) => {
    setLoading(true);
    const params = new URLSearchParams();
    params.set("page", String(p));
    params.set("size", String(sz));
    if (evt) params.set("eventCode", evt);
    if (st) params.set("status", st);
    getFromOpenElisServer(
      `/rest/NotificationTriggerConfig/sent-messages?${params.toString()}`,
      (res) => {
        if (res) {
          setMessages(Array.isArray(res.messages) ? res.messages : []);
          setTotalItems(
            typeof res.totalItems === "number" ? res.totalItems : 0,
          );
        }
        setLoading(false);
      },
    );
  };

  useEffect(() => {
    fetchPage(page, pageSize, eventCodeFilter, statusFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize, eventCodeFilter, statusFilter]);

  const onChangeEventCode = (value) => {
    setEventCodeFilter(value);
    setPage(1);
  };

  const onChangeStatus = (value) => {
    setStatusFilter(value);
    setPage(1);
  };

  const onResend = (id) => {
    setResendingId(id);
    postToOpenElisServerJsonResponse(
      `/rest/NotificationTriggerConfig/sent-messages/${id}/resend`,
      JSON.stringify({}),
      (res) => {
        setResendingId(null);
        if (res && typeof res.id !== "undefined") {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notificationtrigger.sentmessages.resend.success",
            }),
          });
          // Resends appear at the top of page 1 — jump there so the toast
          // isn't a lie if the user was on a later page.
          setModalOpen(false);
          setSelectedLogId(null);
          if (page !== 1) {
            setPage(1);
          } else {
            fetchPage(1, pageSize, eventCodeFilter, statusFilter);
          }
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              (res && (res.error || res.message)) ||
              intl.formatMessage({
                id: "notificationtrigger.sentmessages.resend.failure",
              }),
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const triggerName = (eventCode) => {
    const descriptor = TRIGGER_DESCRIPTORS[eventCode];
    if (descriptor) {
      return intl.formatMessage({
        id: descriptor.nameKey,
        defaultMessage: eventCode,
      });
    }
    return eventCode;
  };

  const triggerStatusLabel = (eventCode) => {
    const descriptor = TRIGGER_DESCRIPTORS[eventCode];
    return descriptor
      ? intl.formatMessage({ id: descriptor.statusKey, defaultMessage: "" })
      : "";
  };

  const typeOptions = useMemo(() => Object.keys(TRIGGER_DESCRIPTORS), []);

  const renderChannels = (channels) => {
    if (!channels || channels.length === 0) return "—";
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
        {channels.map((c) => (
          <Tag
            key={c.channel}
            type={c.status === "SUCCESS" ? "green" : "red"}
            size="sm"
          >
            {c.channel} —{" "}
            {c.status === "SUCCESS"
              ? intl.formatMessage({
                  id: "notificationtrigger.sentmessages.channel.sent",
                })
              : intl.formatMessage({
                  id: "notificationtrigger.sentmessages.channel.failed",
                })}
          </Tag>
        ))}
      </div>
    );
  };

  const renderReference = (msg) => {
    if (!msg.referenceAccession) return "—";
    if (!msg.referenceWorkflow) return msg.referenceAccession;
    return (
      <RouterLink
        to={`/order/${msg.referenceWorkflow}/enter?labNumber=${encodeURIComponent(msg.referenceAccession)}`}
      >
        {msg.referenceAccession}
      </RouterLink>
    );
  };

  const headers = useMemo(
    () => [
      {
        key: "datetime",
        header: intl.formatMessage({
          id: "notificationtrigger.sentmessages.column.datetime",
        }),
      },
      {
        key: "type",
        header: intl.formatMessage({
          id: "notificationtrigger.sentmessages.column.type",
        }),
      },
      {
        key: "recipient",
        header: intl.formatMessage({
          id: "notificationtrigger.sentmessages.column.recipient",
        }),
      },
      {
        key: "channels",
        header: intl.formatMessage({
          id: "notificationtrigger.sentmessages.column.channels",
        }),
      },
      {
        key: "status",
        header: intl.formatMessage({
          id: "notificationtrigger.sentmessages.column.status",
        }),
      },
      {
        key: "reference",
        header: intl.formatMessage({
          id: "notificationtrigger.sentmessages.column.reference",
        }),
      },
      {
        key: "actions",
        header: intl.formatMessage({
          id: "notificationtrigger.sentmessages.column.actions",
        }),
      },
    ],
    [intl],
  );

  const rowData = useMemo(
    // Spread first, then override id with the stringified form — Carbon DataTable
    // requires rows[*].id to be a string and emits a PropType warning otherwise.
    () => messages.map((m) => ({ ...m, id: String(m.id) })),
    [messages],
  );

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <h4>
            <FormattedMessage id="notificationtrigger.sentmessages.heading" />
          </h4>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage id="notificationtrigger.sentmessages.subtitle" />
          </p>
        </Column>
      </Grid>

      <Grid fullWidth={true} style={{ marginBottom: "1rem" }}>
        <Column lg={5} md={4} sm={4}>
          <Select
            id="sm-filter-type"
            labelText={intl.formatMessage({
              id: "notificationtrigger.sentmessages.filter.type.label",
            })}
            value={eventCodeFilter}
            onChange={(e) => onChangeEventCode(e.target.value)}
          >
            <SelectItem
              value={TYPE_ALL}
              text={intl.formatMessage({
                id: "notificationtrigger.sentmessages.filter.type.all",
              })}
            />
            {typeOptions.map((code) => (
              <SelectItem key={code} value={code} text={triggerName(code)} />
            ))}
          </Select>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <Select
            id="sm-filter-status"
            labelText={intl.formatMessage({
              id: "notificationtrigger.sentmessages.filter.status.label",
            })}
            value={statusFilter}
            onChange={(e) => onChangeStatus(e.target.value)}
          >
            <SelectItem
              value={STATUS_ALL}
              text={intl.formatMessage({
                id: "notificationtrigger.sentmessages.filter.status.all",
              })}
            />
            <SelectItem
              value={STATUS_SENT}
              text={intl.formatMessage({
                id: "notificationtrigger.sentmessages.filter.status.sent",
              })}
            />
            <SelectItem
              value={STATUS_FAILED}
              text={intl.formatMessage({
                id: "notificationtrigger.sentmessages.filter.status.failed",
              })}
            />
          </Select>
        </Column>
        <Column
          lg={6}
          md={8}
          sm={4}
          style={{
            display: "flex",
            alignItems: "flex-end",
            justifyContent: "flex-end",
            color: "#525252",
            fontSize: "0.875rem",
          }}
        >
          <FormattedMessage
            id="notificationtrigger.sentmessages.count"
            values={{ count: messages.length, total: totalItems }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <DataTable rows={rowData} headers={headers}>
            {({ getTableProps, getHeaderProps }) => (
              <TableContainer>
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
                    {messages.length === 0 && !loading && (
                      <TableRow>
                        <TableCell
                          colSpan={headers.length}
                          style={{ textAlign: "center", padding: "2rem" }}
                        >
                          <FormattedMessage id="notificationtrigger.sentmessages.empty.heading" />
                        </TableCell>
                      </TableRow>
                    )}
                    {messages.map((m) => {
                      const failed =
                        m.overallStatus === "FAILED" ||
                        (m.channels &&
                          m.channels.some((c) => c.status === "FAILED"));
                      return (
                        <TableRow key={m.id}>
                          <TableCell>
                            {m.firedAt
                              ? new Date(m.firedAt).toLocaleString()
                              : "—"}
                          </TableCell>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                              }}
                            >
                              <div
                                style={{
                                  display: "flex",
                                  alignItems: "center",
                                  gap: "0.25rem",
                                }}
                              >
                                <strong>{triggerName(m.eventCode)}</strong>
                                {triggerStatusLabel(m.eventCode) && (
                                  <Tag size="sm" type="green">
                                    {triggerStatusLabel(m.eventCode)}
                                  </Tag>
                                )}
                              </div>
                              <span
                                style={{
                                  color: "#6f6f6f",
                                  fontSize: "0.75rem",
                                }}
                              >
                                {m.eventCode}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                              }}
                            >
                              <span>{m.recipientDisplayName || "—"}</span>
                              <span
                                style={{
                                  color: "#6f6f6f",
                                  fontSize: "0.75rem",
                                }}
                              >
                                {m.recipientEmail ||
                                  m.recipientPhone ||
                                  m.recipientType}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>{renderChannels(m.channels)}</TableCell>
                          <TableCell>
                            <Tag
                              type={
                                m.overallStatus === "SENT" ? "green" : "red"
                              }
                            >
                              {m.overallStatus === "SENT"
                                ? intl.formatMessage({
                                    id: "notificationtrigger.sentmessages.status.sent",
                                  })
                                : intl.formatMessage({
                                    id: "notificationtrigger.sentmessages.status.failed",
                                  })}
                            </Tag>
                          </TableCell>
                          <TableCell>{renderReference(m)}</TableCell>
                          <TableCell>
                            <div style={{ display: "flex", gap: "0.25rem" }}>
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => {
                                  setSelectedLogId(m.id);
                                  setModalOpen(true);
                                }}
                              >
                                <FormattedMessage id="notificationtrigger.sentmessages.action.viewlog" />
                              </Button>
                              {failed && (
                                <Button
                                  kind="tertiary"
                                  size="sm"
                                  disabled={resendingId === m.id}
                                  onClick={() => onResend(m.id)}
                                >
                                  {resendingId === m.id ? (
                                    <InlineLoading description="" />
                                  ) : (
                                    <FormattedMessage id="notificationtrigger.sentmessages.action.resend" />
                                  )}
                                </Button>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </Column>
      </Grid>

      <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <Pagination
            page={page}
            pageSize={pageSize}
            pageSizes={[10, 20, 50, 100]}
            totalItems={totalItems}
            onChange={({ page: newPage, pageSize: newSize }) => {
              if (newSize !== pageSize) {
                setPageSize(newSize);
                setPage(1);
              } else {
                setPage(newPage);
              }
            }}
          />
        </Column>
      </Grid>

      <ViewLogModal
        logId={selectedLogId}
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setSelectedLogId(null);
        }}
        onResend={onResend}
        resending={resendingId === selectedLogId}
      />
    </>
  );
}
