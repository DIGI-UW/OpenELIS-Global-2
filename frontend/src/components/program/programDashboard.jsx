import React, { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";
import {
  Tile,
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Pagination,
  Grid,
  Column,
  Search,
  Button,
  Link,
  Heading,
  Section,
} from "@carbon/react";

import "./programCaseView.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { FormattedMessage, useIntl } from "react-intl";
import { ArrowLeft, ArrowRight } from "@carbon/icons-react";
import AsyncAvatar from "../patient/photoManagement/photoAvatar/AyncAvatar";

let breadcrumbs = [{ label: "home.label", link: "/" }];

const ProgramDashboard = () => {
  const programDashboardUrl = "/rest/programSamplesList";

  const [summary, setSummary] = useState({
    totalEntries: 0,
    currentPage: 1,
    totalPages: 1,
  });
  const [tableRows, setTableRows] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const intl = useIntl();

  const fetchDashBoard = (pageNumber, filter) => {
    let url = programDashboardUrl;
    if (pageNumber && pageNumber > 1) url += `?page=${pageNumber}`;
    if (filter)
      url +=
        pageNumber && pageNumber > 1
          ? `&filter=${encodeURIComponent(filter)}`
          : `?filter=${encodeURIComponent(filter)}`;

    getFromOpenElisServer(url, (response) => {
      if (!response || !response.orderProgramsDashboardForm) return;

      const form = response.orderProgramsDashboardForm;

      const paging = form.paging || {};

      const totalPages = Number(paging.totalPages || 1);
      const currentPage = Number(paging.currentPage || 1);

      setSummary({
        totalEntries: response.totalEntries || 0,
        totalPages,
        currentPage,
      });

      const formatted = form.orderPrograms.map((item) => ({
        id: String(item.programSampleId),
        patientId: item.patientPK,
        patientName: `${item.firstName || ""} ${item.lastName || ""}`,
        firstName: item.firstName || "",
        lastName: item.lastName || "",
        programName: item.programName || "",
        programCode: item.programCode || "",
        accession: item.accessionNumber || "",
        receivedDate: new Date(item.receivedDate).toLocaleDateString() || "",
        questionnaireResponseUuid: item.questionnaireResponseUuid || "",
      }));

      setTableRows(formatted);
      setPage(1);
    });
  };

  const handleRowClick = (programSampleId) => {
    window.location.href = `/programView/${programSampleId}`;
  };

  useEffect(() => {
    fetchDashBoard(page, searchTerm);
  }, [searchTerm]);

  const headers = [
    { key: "avatar", header: "" }, // avatar column
    { key: "firstName", header: <FormattedMessage id="eorder.name.first" /> },
    { key: "lastName", header: <FormattedMessage id="eorder.name.last" /> },
    {
      key: "programName",
      header: <FormattedMessage id="program.name.label" />,
    },
    { key: "programCode", header: <FormattedMessage id="storage.room.code" /> },
    {
      key: "accession",
      header: <FormattedMessage id="barcode.label.info.labnumber" />,
    },
    {
      key: "receivedDate",
      header: <FormattedMessage id="label.audittrailreport.receiveddate" />,
    },
    {
      key: "questionnaireResponseUuid",
      header: <FormattedMessage id="notebook.label.questionnaire" />,
    },
  ];

  const displayedRows = tableRows.slice((page - 1) * pageSize, page * pageSize);

  const handlePageChange = ({ page: newPage, pageSize: newSize }) => {
    setPage(newPage);
    if (newSize !== pageSize) setPageSize(newSize);
  };

  const tileList = [
    {
      title: <FormattedMessage id="notebook.label.total" />,
      count: summary.totalEntries,
    },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage
                id="banner.menu.results.order.programmes"
                defaultMessage="Order Programmes"
              />
            </Heading>
          </Section>
        </Column>
        <Column sm={4} md={8} lg={16} className="dashboard-containar">
          {tileList.map((tile, idx) => (
            <Tile className="dashboard-div" key={idx}>
              <h3 className="tile-title-Programe">{tile.title}</h3>
              <p className="tile-value">{tile.count}</p>
            </Tile>
          ))}
        </Column>

        <Column sm={4} md={8} lg={16} className="table-container">
          <div className="table-item">
            {summary.totalPages > 1 && (
              <>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "flex-end",
                    alignItems: "center",
                    marginRight: "3rem",
                    gap: "1rem",
                    fontSize: "1rem",
                  }}
                >
                  <Link>
                    {summary.currentPage}/{summary.totalPages}
                  </Link>
                </div>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "flex-end",
                    alignItems: "center",
                    marginBottom: "1rem",
                    gap: "1rem",
                  }}
                >
                  <div style={{ display: "flex", gap: "0.5rem" }}>
                    <Button
                      onClick={() => {
                        if (summary.currentPage > 1)
                          fetchDashBoard(summary.currentPage - 1, searchTerm);
                      }}
                      renderIcon={ArrowLeft}
                      hasIconOnly
                      iconDescription="previous"
                    />
                    <Button
                      onClick={() => {
                        if (summary.currentPage < summary.totalPages)
                          fetchDashBoard(summary.currentPage + 1, searchTerm);
                      }}
                      renderIcon={ArrowRight}
                      hasIconOnly
                      iconDescription="next"
                      disabled={summary.currentPage === summary.totalPages}
                    />
                  </div>
                </div>
              </>
            )}

            <Search
              size="lg"
              labelText={intl.formatMessage({
                id: "sampleManagement.search.label",
              })}
              placeholder={intl.formatMessage({
                id: "sampleManagement.search.label",
              })}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />

            <DataTable rows={displayedRows} headers={headers} isSortable>
              {({ headers, getHeaderProps, getTableProps }) => (
                <>
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
                        {displayedRows.map((row) => (
                          <TableRow
                            key={row.id}
                            onClick={() => handleRowClick(row.id)}
                            style={{ cursor: "pointer", padding: "0.5rem" }}
                          >
                            {headers.map((header) => (
                              <TableCell key={header.key}>
                                {header.key === "avatar" ? (
                                  <AsyncAvatar
                                    patientId={row.patientId}
                                    patientName={row.patientName}
                                    hasPhoto={true}
                                    size={40}
                                  />
                                ) : (
                                  row[header.key]
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>

                  <Pagination
                    page={page}
                    pageSize={pageSize}
                    totalItems={tableRows.length}
                    pageSizes={[2, 5, 10, 20]}
                    onChange={handlePageChange}
                  />
                </>
              )}
            </DataTable>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default ProgramDashboard;
