import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Button,
  DataTable,
  InlineLoading,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { useHistory } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

export default function IncomingOrders() {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const history = useHistory();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState([]);

  // Format timestamp to mm/dd hh:mm (24-hour format)
  const formatReceivedTimestamp = (timestamp) => {
    if (!timestamp) return "";
    try {
      const date = new Date(Number(timestamp));
      if (isNaN(date.getTime())) return "";
      const mm = String(date.getMonth() + 1).padStart(2, "0");
      const dd = String(date.getDate()).padStart(2, "0");
      const hh = String(date.getHours()).padStart(2, "0");
      const min = String(date.getMinutes()).padStart(2, "0");
      return `${mm}/${dd} ${hh}:${min}`;
    } catch (e) {
      return "";
    }
  };

  const headers = [
    {
      key: "patientName",
      header: intl.formatMessage({ id: "incomingOrders.table.patientName" }),
    },
    {
      key: "receivedTimestamp",
      header: intl.formatMessage({
        id: "incomingOrders.table.receivedTimestamp",
      }),
    },
    {
      key: "testCount",
      header: intl.formatMessage({ id: "incomingOrders.table.testCount" }),
    },
    {
      key: "source",
      header: intl.formatMessage({ id: "incomingOrders.table.source" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "incomingOrders.table.actions" }),
    },
  ];

  const loadRows = () => {
    setLoading(true);
    getFromOpenElisServer("/rest/incoming-orders", (data) => {
      if (!componentMounted.current) {
        return;
      }
      const list = Array.isArray(data) ? data : [];
      const mapped = list.map((item) => ({
        id: String(item.externalOrderNumber || ""),
        // Keep externalOrderNumber in data for collection flow (hidden from UI)
        externalOrderNumber: item.externalOrderNumber || "",
        patientName: item.patientName || "",
        receivedTimestamp: formatReceivedTimestamp(item.receivedTimestamp),
        testCount: item.testCount != null ? String(item.testCount) : "",
        source: item.source || "",
      }));
      setRows(mapped);
      setLoading(false);
    });
  };

  useEffect(() => {
    componentMounted.current = true;
    loadRows();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const onCollect = (row) => {
    if (!row) {
      return;
    }

    const externalOrderNumber = row.id || row.externalOrderNumber || "";
    if (!externalOrderNumber) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Missing external order number",
      });
      return;
    }

    history.push(
      "/SamplePatientEntry?incomingOrderNumber=" +
        encodeURIComponent(externalOrderNumber),
    );
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}

      <TableContainer
        title={intl.formatMessage({ id: "incomingOrders.title" })}
        description={intl.formatMessage({ id: "incomingOrders.description" })}
      >
        {loading ? (
          <InlineLoading
            description={intl.formatMessage({ id: "incomingOrders.loading" })}
          />
        ) : null}

        <DataTable rows={rows} headers={headers} isSortable={false}>
          {({ rows, headers, getHeaderProps, getTableProps }) => (
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
                  <TableRow key={row.id}>
                    {row.cells.map((cell) => {
                      if (cell.info.header === "actions") {
                        return (
                          <TableCell key={cell.id}>
                            <Button
                              size="sm"
                              kind="primary"
                              disabled={loading}
                              onClick={() => {
                                onCollect(row);
                              }}
                            >
                              <FormattedMessage id="incomingOrders.collect" />
                            </Button>
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
      </TableContainer>
    </>
  );
}
