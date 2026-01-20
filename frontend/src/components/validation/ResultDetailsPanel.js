import React, { useState, useEffect } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const ResultDetailsPanel = ({ analysisId, tab }) => {
  const intl = useIntl();
  const [details, setDetails] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (analysisId) {
      setIsLoading(true);
      setError(null);

      getFromOpenElisServer(
        `/rest/AccessionValidation/${analysisId}/details`,
        (data) => {
          setIsLoading(false);
          setDetails(data);
        },
        (error) => {
          setIsLoading(false);
          setError(error);
        },
      );
    }
  }, [analysisId]);

  if (isLoading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading description={intl.formatMessage({ id: "label.loading" })} />
      </div>
    );
  }

  if (error) {
    return (
      <InlineNotification
        kind="error"
        title={intl.formatMessage({ id: "notification.error" })}
        subtitle={
          error.message ||
          intl.formatMessage({ id: "validation.details.error" })
        }
      />
    );
  }

  if (!details) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <FormattedMessage id="validation.details.nodata" />
      </div>
    );
  }

  switch (tab) {
    case "method":
      return renderMethodTab(details, intl);
    case "orderInfo":
      return renderOrderInfoTab(details, intl);
    case "attachments":
      return renderAttachmentsTab(details, intl);
    case "history":
      return renderHistoryTab(details, intl);
    case "qc":
      return renderQCTab(details, intl);
    default:
      return null;
  }
};

const renderMethodTab = (details, intl) => {
  const reagentLots = details.reagentLots || [];

  if (reagentLots.length === 0) {
    return (
      <div style={{ padding: "1rem" }}>
        <FormattedMessage id="validation.details.method.noreagents" />
      </div>
    );
  }

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "validation.details.method.reagent" }),
    },
    {
      key: "lot",
      header: intl.formatMessage({ id: "validation.details.method.lot" }),
    },
    {
      key: "expires",
      header: intl.formatMessage({ id: "validation.details.method.expires" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "validation.details.method.status" }),
    },
  ];

  const rows = reagentLots.map((reagent, idx) => ({
    id: `${idx}`,
    name: reagent.name,
    lot: reagent.lot,
    expires: reagent.expires,
    status: reagent.status,
  }));

  return (
    <div style={{ padding: "1rem" }}>
      <h5>
        <FormattedMessage id="validation.details.method.title" />
      </h5>
      <DataTable rows={rows} headers={headers}>
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  {row.cells.map((cell) => {
                    if (cell.info.header === "status") {
                      return (
                        <TableCell key={cell.id}>
                          <Tag
                            type={
                              cell.value === "ok"
                                ? "green"
                                : cell.value === "expiring-soon"
                                  ? "yellow"
                                  : "red"
                            }
                          >
                            {cell.value}
                          </Tag>
                        </TableCell>
                      );
                    }
                    return <TableCell key={cell.id}>{cell.value}</TableCell>;
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DataTable>
    </div>
  );
};

const renderOrderInfoTab = (details, intl) => {
  const orderInfo = details.orderInfo || {};

  return (
    <div style={{ padding: "1rem" }}>
      <h5>
        <FormattedMessage id="validation.details.orderinfo.title" />
      </h5>
      <StructuredListWrapper>
        <StructuredListHead>
          <StructuredListRow head>
            <StructuredListCell head>
              <FormattedMessage id="validation.details.orderinfo.field" />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="validation.details.orderinfo.value" />
            </StructuredListCell>
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.clinician" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.clinician || "-"}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.phone" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.clinicianPhone || "-"}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.department" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.department || "-"}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.priority" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.priority ? (
                <Tag type={orderInfo.priority === "Urgent" ? "red" : "green"}>
                  {orderInfo.priority}
                </Tag>
              ) : (
                "-"
              )}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.collection" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.collectionDate || "-"}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.received" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.receivedDate || "-"}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.history" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.clinicalHistory || "-"}
            </StructuredListCell>
          </StructuredListRow>
          <StructuredListRow>
            <StructuredListCell>
              <FormattedMessage id="validation.details.orderinfo.diagnosis" />
            </StructuredListCell>
            <StructuredListCell>
              {orderInfo.diagnosis || "-"}
            </StructuredListCell>
          </StructuredListRow>
        </StructuredListBody>
      </StructuredListWrapper>
    </div>
  );
};

const renderAttachmentsTab = (details, intl) => {
  const attachments = details.attachments || [];

  if (attachments.length === 0) {
    return (
      <div style={{ padding: "1rem" }}>
        <FormattedMessage id="validation.details.attachments.none" />
      </div>
    );
  }

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "validation.details.attachments.name" }),
    },
    {
      key: "type",
      header: intl.formatMessage({ id: "validation.details.attachments.type" }),
    },
    {
      key: "size",
      header: intl.formatMessage({ id: "validation.details.attachments.size" }),
    },
    {
      key: "uploadedBy",
      header: intl.formatMessage({
        id: "validation.details.attachments.uploadedby",
      }),
    },
    {
      key: "uploadedAt",
      header: intl.formatMessage({
        id: "validation.details.attachments.uploadedat",
      }),
    },
  ];

  const rows = attachments.map((attachment) => ({
    id: attachment.id,
    name: attachment.name,
    type: attachment.type,
    size: attachment.size,
    uploadedBy: attachment.uploadedBy,
    uploadedAt: attachment.uploadedAt,
  }));

  return (
    <div style={{ padding: "1rem" }}>
      <h5>
        <FormattedMessage id="validation.details.attachments.title" />
      </h5>
      <DataTable rows={rows} headers={headers}>
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
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
    </div>
  );
};

const renderHistoryTab = (details, intl) => {
  const previousResults = details.previousResults || [];

  if (previousResults.length === 0) {
    return (
      <div style={{ padding: "1rem" }}>
        <FormattedMessage id="validation.details.history.none" />
      </div>
    );
  }

  const headers = [
    {
      key: "date",
      header: intl.formatMessage({ id: "validation.details.history.date" }),
    },
    {
      key: "value",
      header: intl.formatMessage({ id: "validation.details.history.value" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "validation.details.history.status" }),
    },
  ];

  const rows = previousResults.map((result, idx) => ({
    id: `${idx}`,
    date: result.date,
    value: result.value,
    status: result.status,
  }));

  return (
    <div style={{ padding: "1rem" }}>
      <h5>
        <FormattedMessage id="validation.details.history.title" />
      </h5>

      {details.deltaCheck && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "validation.details.history.deltacheck",
          })}
          subtitle={intl.formatMessage(
            { id: "validation.details.history.deltacheck.message" },
            {
              previous: details.deltaCheck.previous,
              change: details.deltaCheck.change,
              threshold: details.deltaCheck.threshold,
            },
          )}
          lowContrast
        />
      )}

      <DataTable rows={rows} headers={headers}>
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  {row.cells.map((cell) => {
                    if (cell.info.header === "status") {
                      return (
                        <TableCell key={cell.id}>
                          <Tag
                            type={
                              cell.value === "normal"
                                ? "green"
                                : cell.value === "abnormal"
                                  ? "red"
                                  : "blue"
                            }
                          >
                            {cell.value}
                          </Tag>
                        </TableCell>
                      );
                    }
                    return <TableCell key={cell.id}>{cell.value}</TableCell>;
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DataTable>
    </div>
  );
};

const renderQCTab = (details, intl) => {
  const qcData = details.qcData || [];

  if (qcData.length === 0) {
    return (
      <div style={{ padding: "1rem" }}>
        <FormattedMessage id="validation.details.qc.none" />
      </div>
    );
  }

  const headers = [
    {
      key: "level",
      header: intl.formatMessage({ id: "validation.details.qc.level" }),
    },
    {
      key: "expected",
      header: intl.formatMessage({ id: "validation.details.qc.expected" }),
    },
    {
      key: "actual",
      header: intl.formatMessage({ id: "validation.details.qc.actual" }),
    },
    {
      key: "cv",
      header: intl.formatMessage({ id: "validation.details.qc.cv" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "validation.details.qc.status" }),
    },
  ];

  const rows = qcData.map((qc, idx) => ({
    id: `${idx}`,
    level: qc.level,
    expected: qc.expected,
    actual: qc.actual,
    cv: qc.cv,
    status: qc.status,
  }));

  return (
    <div style={{ padding: "1rem" }}>
      <h5>
        <FormattedMessage id="validation.details.qc.title" />
      </h5>
      <DataTable rows={rows} headers={headers}>
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  {row.cells.map((cell) => {
                    if (cell.info.header === "status") {
                      return (
                        <TableCell key={cell.id}>
                          <Tag type={cell.value === "pass" ? "green" : "red"}>
                            {cell.value}
                          </Tag>
                        </TableCell>
                      );
                    }
                    return <TableCell key={cell.id}>{cell.value}</TableCell>;
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DataTable>
    </div>
  );
};

export default ResultDetailsPanel;
