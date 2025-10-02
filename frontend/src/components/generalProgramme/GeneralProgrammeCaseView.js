import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import {
  Heading,
  Grid,
  Column,
  Section,
  Loading,
  Tile,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import PageBreadCrumb from "../common/PageBreadCrumb";
import "../pathology/PathologyDashboard.css";

function GeneralProgrammeCaseView() {
  const { programmeId } = useParams();
  const { notificationVisible } = useContext(NotificationContext);
  const intl = useIntl();
  const [programme, setProgramme] = useState(null);
  const [loading, setLoading] = useState(true);
  const [orderEntries, setOrderEntries] = useState([]);
  const [orderLoading, setOrderLoading] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [questionnaireResponse, setQuestionnaireResponse] = useState(null);

  useEffect(() => {
    setLoading(true);
    fetch(`/rest/programs/${programmeId}`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch programme");
        return res.json();
      })
      .then((data) => {
        setProgramme(data);
        setLoading(false);
      })
      .catch(() => {
        setProgramme(null);
        setLoading(false);
      });
    setOrderLoading(true);
    fetch(`/rest/program/${programmeId}/orders`)
      .then((res) => res.json())
      .then((data) => {
        setOrderEntries(data);
        setOrderLoading(false);
      })
      .catch(() => setOrderLoading(false));
  }, [programmeId]);

  // Fetch questionnaire response for selected order
  useEffect(() => {
    if (selectedOrder) {
      fetch(`/rest/order/${selectedOrder.orderId}/questionnaireResponse`)
        .then((res) => res.json())
        .then((data) => setQuestionnaireResponse(data));
    }
  }, [selectedOrder]);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    {
      label: "banner.menu.generalProgramme",
      link: "/GeneralProgrammeDashboard",
    },
    { label: programme ? programme.name : "", link: "#" },
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading description="Loading Case..." />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                {programme ? programme.name : <FormattedMessage id="loading" />}
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      {programme && (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Tile>
              <h4>
                <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.description" />
                :
              </h4>
              <p>{programme.description}</p>
            </Tile>
          </Column>
        </Grid>
      )}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="label.order.entries.for" />
            </Heading>
            {orderLoading ? (
              <Loading description="Loading Order Entries..." />
            ) : orderEntries.length === 0 ? (
              <p>
                <FormattedMessage
                  id="label.no.order.entries"
                  defaultMessage="No order entries found for this programme."
                />
              </p>
            ) : (
              <DataTable
                rows={orderEntries}
                headers={[
                  { key: "orderId", header: "Order ID" },
                  { key: "patientName", header: "Patient Name" },
                  { key: "orderDate", header: "Order Date" },
                  { key: "status", header: "Status" },
                  { key: "accessionNumber", header: "Accession Number" },
                ]}
                isSortable
              >
                {({ rows, headers, getHeaderProps, getTableProps }) => (
                  <TableContainer title="" description="">
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
                          <TableHeader>
                            <FormattedMessage id="label.button.view" />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => (
                          <TableRow key={row.id || row.orderId}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                            <TableCell>
                              <button
                                onClick={() => setSelectedOrder(row)}
                                style={{ padding: "4px 8px" }}
                              >
                                <FormattedMessage id="label.button.view" />
                              </button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            )}
          </Section>
        </Column>
      </Grid>
      {selectedOrder && (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="label.order.details"
                  defaultMessage="Order Details"
                />
              </Heading>

              {selectedOrder.patient && (
                <PatientHeader patient={selectedOrder.patient} />
              )}
              {questionnaireResponse && (
                <QuestionnaireResponse
                  questionnaireResponse={questionnaireResponse}
                />
              )}
            </Section>
          </Column>
        </Grid>
      )}
    </>
  );
}

export default GeneralProgrammeCaseView;
