import React, { useState } from "react";
import {
  Form,
  Heading,
  Grid,
  Column,
  Section,
  DataTable,
  TableContainer,
  Table,
  TableHeader,
  TableRow,
  TableCell,
  Pagination,
  TableBody,
  Button,
  TableHead,
  Loading,
} from "@carbon/react";
import "../../Style.css";

import { AlertDialog } from "../../common/CustomNotification";
import CustomLabNumberInput from "../../common/CustomLabNumberInput";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { auditTrailHeaderData } from "./AuditTrailTableHeader";

const AuditTrailReport = ({ id }) => {
  const [labNo, setLabNo] = useState("");
  const [isLabNoError, setIsLabNoError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showNotification, setShowNotification] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(30);
  const [auditTrailItems, setAuditTrailItems] = useState([]);
  const [data, setData] = useState(null);

  const intl = useIntl();

  const handlePageChange = (pageInfo) => {
    setPage(pageInfo.page);
    setPageSize(pageInfo.pageSize);
  };

  const handleViewReport = () => {
    if (labNo.length === 0) {
      setIsLabNoError("label.audittrail.labNo.missing");
      setData(null);
      setAuditTrailItems([]);
      return;
    }
    setIsLabNoError(null);
    setIsLoading(true);

    getFromOpenElisServer(
      "/rest/AuditTrailReport?accessionNumber=" + labNo,
      (data) => {
        if (!data.log) {
          setIsLabNoError("label.audittrail.labNo.invalidaccessionnumber");
          setData(null);
          setAuditTrailItems([]);
          setIsLoading(false);
          return;
        } else {
          // Add unique id and format timestamp for each item
          const updatedAuditTrailItems = data.log.map((item, index) => {
            const formattedTimeStamp = new Date(item.timeStamp).toLocaleString(
              "en-GB",
              {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
                hour12: false,
              },
            ); // Convert timestamp to localized format (DD/MM/YYYY HH:mm)
            return { ...item, id: index + 1, timeStamp: formattedTimeStamp };
          });

          setIsLabNoError(null);
          setAuditTrailItems(updatedAuditTrailItems);
          setData(data);
          console.log("site name", data.sampleOrderItems.referringSiteName);
        }
        setIsLoading(false);
        setShowNotification(true);
      },
    );
  };

  return (
    <>
      <br />
      <Grid fullWidth={true}>
        <Column lg={4} md={4} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id={id} />
              </Heading>
            </Section>
          </Section>
          <br />
        </Column>
      </Grid>
      <br />
      {showNotification && <AlertDialog />}
      <br />
      <Form>
        <Grid fullWidth={true}>
          <Column lg={6} md={16} sm={4}>
            <CustomLabNumberInput
              id="labNo"
              labelText={intl.formatMessage({
                id: "label.audittrail",
                defaultMessage: "Lab No",
              })}
              value={labNo}
              onChange={(event, rowVal) =>
                setLabNo(rowVal ? rowVal : event?.target?.value)
              }
              invalid={
                isLabNoError
                  ? intl.formatMessage({
                      id: "label.audittrail.labNo.missing",
                    })
                  : ""
              }
              invalidText={intl.formatMessage({
                id: `${isLabNoError}`,
              })}
            />
          </Column>
        </Grid>
        <br />
        <br />
        <Grid fullWidth={true}>
          <Column lg={16}>
            <Section>
              <Button type="button" onClick={handleViewReport}>
                <FormattedMessage id="label.button.viewReport" />
                <Loading
                  small={true}
                  withOverlay={false}
                  className={isLoading ? "show" : "hidden"}
                />
              </Button>
            </Section>
          </Column>
        </Grid>
      </Form>
      <br />
      {auditTrailItems && data && (
        <div
          style={{ display: "flex", justifyContent: "center", margin: "20px" }}
        >
          <Grid
            fullWidth={true}
            style={{
              padding: "2px",
              border: "1px solid #ccc",
              borderRadius: "5px",
              boxShadow: "0px 2px 4px rgba(0, 0, 0, 0.1)",
            }}
          >
            <Column lg={16} style={{ marginBottom: "20px" }}>
              <Section>
                <Heading>
                  <FormattedMessage
                    id="audittrail.table.heading"
                    defaultMessage={"Order Information"}
                  />
                </Heading>
              </Section>
            </Column>
            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.priority" />
                </span>
              </div>
              <div style={{ marginBottom: "10px", color: "#555" }}>
                {data?.sampleOrderItems.priority}
              </div>
            </Column>
            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.requestdate" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {data?.sampleOrderItems?.requestDate}
              </div>
            </Column>
            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.receiveddate" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {data?.sampleOrderItems?.receivedDateForDisplay}
              </div>
            </Column>
            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.nextvisitdate" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {data?.sampleOrderItems?.nextVisitDate}
              </div>
            </Column>
            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.sitename" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {data?.sampleOrderItems.referringSiteName}
              </div>
            </Column>

            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.program" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {data?.sampleOrderItems?.program}
              </div>
            </Column>

            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.requester" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {`${data?.sampleOrderItems?.providerFirstName} ${data?.sampleOrderItems?.providerLastName}`}
              </div>
            </Column>
            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.requester.firstname" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {`${data?.sampleOrderItems?.providerFirstName}`}
              </div>
            </Column>
            <Column lg={8} style={{ marginBottom: "20px" }}>
              <div style={{ marginBottom: "10px" }}>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <FormattedMessage id="label.audittrailreport.requester.lastname" />
                </span>
              </div>
              <div style={{ marginBottom: "10px" }}>
                {`${data?.sampleOrderItems?.providerLastName}`}
              </div>
            </Column>
          </Grid>
        </div>
      )}
      <Column lg={16}>
        <DataTable
          rows={auditTrailItems ?? []}
          headers={auditTrailHeaderData}
          isSortable
        >
          {({ rows, headers, getHeaderProps, getTableProps }) => (
            <TableContainer title="Patient Results">
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
                  {rows
                    .slice((page - 1) * pageSize)
                    .slice(0, pageSize)
                    .map((row) => (
                      <TableRow key={row.id}>
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
        <Pagination
          onChange={handlePageChange}
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 30, 50, 100]}
          totalItems={auditTrailItems.length}
          forwardText={intl.formatMessage({ id: "pagination.forward" })}
          backwardText={intl.formatMessage({ id: "pagination.backward" })}
          itemRangeText={(min, max, total) =>
            intl.formatMessage(
              { id: "pagination.item-range" },
              { min: min, max: max, total: total },
            )
          }
          itemsPerPageText={intl.formatMessage({
            id: "pagination.items-per-page",
          })}
          itemText={(min, max) =>
            intl.formatMessage(
              { id: "pagination.item" },
              { min: min, max: max },
            )
          }
          pageNumberText={intl.formatMessage({
            id: "pagination.page-number",
          })}
          pageRangeText={(_current, total) =>
            intl.formatMessage(
              { id: "pagination.page-range" },
              { total: total },
            )
          }
          pageText={(page, pagesUnknown) =>
            intl.formatMessage(
              { id: "pagination.page" },
              { page: pagesUnknown ? "" : page },
            )
          }
        />
      </Column>
    </>
  );
};

export default AuditTrailReport;
