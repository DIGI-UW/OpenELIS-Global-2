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
} from "@carbon/react";

import "./programCaseView.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { FormattedMessage, useIntl } from "react-intl";

let breadcrumbs = [{ label: "home.label", link: "/" }];

const ProgramDashboard = () => {
  const programDashboardUrl = "/rest/programSamplesList";

  const [summary, setSummary] = useState({
    totalEntries: 0,
  });

  const [tableRows, setTableRows] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [searchTerm, setSearchTerm] = useState("");
  const intel = useIntl();

  const fetchDashBoard = (page, pageSize, filter) => {
    const url = `${programDashboardUrl}?size=${pageSize}&page=${page}${
      filter ? `&filter=${encodeURIComponent(filter)}` : ""
    }`;

    getFromOpenElisServer(url, (response) => {
      if (!response || !response.programSample) return;

      setSummary({
        totalEntries: response.totalEntries || 0,
      });

      const formatted = response.programSample.map((item) => ({
        id: String(item.programSampleId),
        programName: item.programName || "",
        programCode: item.programCode || "",
        accession: item.accessionNumber || "",
        receivedDate: new Date(item.receivedDate).toLocaleDateString() || "",
        questionnaireResponseUuid: item.questionnaireResponseUuid || "",
      }));

      setTableRows(formatted);
    });
  };

  const handleRowClick = (programSampleId) => {
    window.location.href = `/programView/${programSampleId}`;
  };

  useEffect(() => {
    fetchDashBoard(page, pageSize, searchTerm);
  }, [page, pageSize, searchTerm]);

  const headers = [
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

  const handlePageChange = ({ page: newPage, pageSize: newSize }) => {
    setPage(newPage);
    setPageSize(newSize);
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
        <Column sm={4} md={8} lg={16} className="dashboard-container">
          {tileList.map((tile, idx) => (
            <Tile className="dashboard-tile" key={idx}>
              <h3 className="tile-title-Program">{tile.title}</h3>
              <p className="tile-value">{tile.count}</p>
            </Tile>
          ))}
        </Column>

        <Column sm={4} md={8} lg={16} className="table-container">
          <div className="table-item">
            <Search
              size="lg"
              labelText={intel.formatMessage({
                id: "sampleManagement.search.label",
              })}
              placeholder={intel.formatMessage({
                id: "sampleManagement.search.label",
              })}
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPage(1);
              }}
            />

            <DataTable rows={tableRows} headers={headers} isSortable>
              {({ rows, headers, getHeaderProps, getTableProps }) => (
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
                        {rows.map((row) => (
                          <TableRow
                            key={row.id}
                            onClick={() => handleRowClick(row.id)}
                            style={{ cursor: "pointer" }}
                          >
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>

                  <Pagination
                    page={page}
                    pageSize={pageSize}
                    totalItems={summary.totalEntries}
                    pageSizes={[5, 10, 20, 30, 50, 100]}
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
