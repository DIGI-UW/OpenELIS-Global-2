import React, { useContext, useEffect, useMemo, useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Button,
  Loading,
  Tag,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableContainer,
  InlineNotification,
  Form,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

const initialQueueState = {
  entries: [],
  pendingCount: 0,
  failedCount: 0,
  odooAvailable: false,
  statusMessage: "",
};

function OdooSyncQueue() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [queueData, setQueueData] = useState(initialQueueState);
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 530);

  const breadcrumbs = useMemo(
    () => [
      {
        label: { id: "home.label", defaultMessage: "Home" },
        link: "/",
      },
      {
        label: {
          id: "breadcrums.admin.managment",
          defaultMessage: "Administration",
        },
        link: "/MasterListsPage",
      },
      {
        label: {
          id: "odoo.syncQueue.breadcrumb",
          defaultMessage: "Odoo Sync Queue",
        },
        link: "/MasterListsPage#odooSyncQueue",
      },
    ],
    [],
  );

  const fetchQueueData = () => {
    setLoading(true);
    getFromOpenElisServer("/api/odoo/queue", (response) => {
      if (response) {
        setQueueData({
          entries: response.entries || [],
          pendingCount: response.pendingCount || 0,
          failedCount: response.failedCount || 0,
          odooAvailable: Boolean(response.odooAvailable),
          statusMessage: response.statusMessage || "",
        });
      } else {
        setQueueData(initialQueueState);
        setNotificationVisible(true);
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "odoo.syncQueue.fetch.error",
            defaultMessage: "Unable to load Odoo queue data.",
          }),
          kind: NotificationKinds.error,
        });
      }
      setLoading(false);
    });
  };

  useEffect(() => {
    fetchQueueData();
  }, []);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 530);
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const handleRetry = () => {
    setRetrying(true);
    postToOpenElisServerJsonResponse(
      "/api/odoo/queue/retry",
      JSON.stringify({}),
      (response) => {
        if (response) {
          setQueueData({
            entries: response.entries || [],
            pendingCount: response.pendingCount || 0,
            failedCount: response.failedCount || 0,
            odooAvailable: Boolean(response.odooAvailable),
            statusMessage: response.statusMessage || "",
          });
          setNotificationVisible(true);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              response.statusMessage ||
              intl.formatMessage({
                id: "odoo.syncQueue.retry.success",
                defaultMessage: "Odoo retry job completed.",
              }),
            kind: NotificationKinds.success,
          });
        } else {
          setNotificationVisible(true);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "odoo.syncQueue.retry.error",
              defaultMessage: "Retry failed.",
            }),
            kind: NotificationKinds.error,
          });
        }
        setRetrying(false);
      },
    );
  };

  const renderDate = (value) => {
    if (!value) {
      return "-";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return "-";
    }
    return new Intl.DateTimeFormat(intl.locale, {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    }).format(date);
  };

  const connectionTagType = queueData.odooAvailable ? "green" : "red";

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading ? <Loading withOverlay={true} /> : null}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="odoo.syncQueue.title"
                  defaultMessage="Odoo Sync Queue"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section style={{ marginTop: "1.5rem" }}>
              <Form
                style={{
                  display: "flex",
                  flexDirection: isMobile ? "column" : "row",
                  gap: isMobile ? "1rem" : "2rem",
                  justifyContent: "space-between",
                  alignItems: isMobile ? "stretch" : "center",
                  flexWrap: "wrap",
                }}
              >
                <Column
                  lg={16}
                  md={8}
                  sm={4}
                  style={{
                    display: "flex",
                    gap: isMobile ? "0.75rem" : "0.5rem",
                    flexDirection: isMobile ? "column" : "row",
                    width: isMobile ? "100%" : "auto",
                    margin: "0",
                  }}
                >
                  <Button
                    kind="primary"
                    disabled={retrying}
                    onClick={handleRetry}
                    style={{ width: isMobile ? "100%" : "auto" }}
                  >
                    <FormattedMessage
                      id="odoo.syncQueue.retry"
                      defaultMessage="Run Retry Now"
                    />
                  </Button>
                  <Button
                    kind="secondary"
                    disabled={loading}
                    onClick={fetchQueueData}
                    style={{ width: isMobile ? "100%" : "auto" }}
                  >
                    <FormattedMessage
                      id="odoo.syncQueue.refresh"
                      defaultMessage="Refresh"
                    />
                  </Button>
                </Column>
                <Column
                  lg={16}
                  md={8}
                  sm={4}
                  style={{
                    display: "flex",
                    flexDirection: isMobile ? "column" : "row",
                    gap: isMobile ? "0.75rem" : "1.5rem",
                    alignItems: isMobile ? "flex-start" : "center",
                    justifyContent: isMobile ? "flex-start" : "flex-end",
                  }}
                >
                  <h4
                    style={{
                      margin: 0,
                      fontSize: isMobile ? "1rem" : "1.1rem",
                    }}
                  >
                    <FormattedMessage
                      id="odoo.syncQueue.pending"
                      defaultMessage="Pending"
                    />
                    : <strong>{queueData.pendingCount}</strong>
                  </h4>
                  <h4
                    style={{
                      margin: 0,
                      fontSize: isMobile ? "1rem" : "1.1rem",
                    }}
                  >
                    <FormattedMessage
                      id="odoo.syncQueue.failed"
                      defaultMessage="Failed"
                    />
                    : <strong>{queueData.failedCount}</strong>
                  </h4>
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.5rem",
                    }}
                  >
                    <FormattedMessage
                      id="odoo.syncQueue.connection"
                      defaultMessage="Odoo connection"
                    />
                    <Tag type={connectionTagType} size="md">
                      <FormattedMessage
                        id={
                          queueData.odooAvailable
                            ? "odoo.syncQueue.connection.available"
                            : "odoo.syncQueue.connection.unavailable"
                        }
                        defaultMessage={
                          queueData.odooAvailable ? "Available" : "Unavailable"
                        }
                      />
                    </Tag>
                  </div>
                </Column>
              </Form>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <TableContainer>
            <Table size="lg" useZebraStyles={true}>
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.id"
                      defaultMessage="ID"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.accession"
                      defaultMessage="Accession"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.status"
                      defaultMessage="Status"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.retries"
                      defaultMessage="Retries"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.maxRetries"
                      defaultMessage="Max retries"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.created"
                      defaultMessage="Created"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.lastRetry"
                      defaultMessage="Last retry"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.completed"
                      defaultMessage="Completed"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.invoiceId"
                      defaultMessage="Invoice ID"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="odoo.syncQueue.table.error"
                      defaultMessage="Last error"
                    />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {queueData.entries.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={10}>
                      <FormattedMessage
                        id="odoo.syncQueue.table.empty"
                        defaultMessage="No entries in the queue."
                      />
                    </TableCell>
                  </TableRow>
                ) : (
                  queueData.entries.map((entry) => (
                    <TableRow key={entry.id}>
                      <TableCell>{entry.id}</TableCell>
                      <TableCell>{entry.accessionNumber || "-"}</TableCell>
                      <TableCell>{entry.status || "-"}</TableCell>
                      <TableCell>{entry.retryCount ?? "-"}</TableCell>
                      <TableCell>{entry.maxRetries ?? "-"}</TableCell>
                      <TableCell>{renderDate(entry.createdDate)}</TableCell>
                      <TableCell>{renderDate(entry.lastRetryDate)}</TableCell>
                      <TableCell>{renderDate(entry.completedDate)}</TableCell>
                      <TableCell>{entry.odooInvoiceId ?? "-"}</TableCell>
                      <TableCell>{entry.errorMessage || "-"}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </div>
        {queueData.statusMessage ? (
          <Grid fullWidth={true} style={{ marginTop: "1.5rem" }}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <InlineNotification
                  kind="info"
                  lowContrast={true}
                  subtitle={queueData.statusMessage}
                  title={intl.formatMessage({
                    id: "odoo.syncQueue.notice",
                    defaultMessage: "Latest queue status",
                  })}
                />
              </Section>
            </Column>
          </Grid>
        ) : null}
      </div>
    </>
  );
}

export default OdooSyncQueue;
