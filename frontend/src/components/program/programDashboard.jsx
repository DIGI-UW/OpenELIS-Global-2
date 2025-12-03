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
} from "@carbon/react";

import "./../pathology/PathologyDashboard.css";

const ProgramDashboard = () => {
  const programDashboardUrl = "/rest/programSamplesList";

  const [summary, setSummary] = useState({
    totalEntries: 0,
    inProgressEntries: 0,
    completedEntries: 0,
  });

  const [tableRows, setTableRows] = useState([]);

  const fetchDashBoard = () => {
    getFromOpenElisServer(programDashboardUrl, (response) => {
      if (!response || !response.programSample) return;

      setSummary({
        totalEntries: response.totalEntries || 0,
        inProgressEntries: response.inProgressEntries || 0,
        completedEntries: response.completedEntries || 0,
      });

      // Convert backend ViewItems → Table Rows
      const formatted = response.programSample.map((item, index) => ({
        id: String(item.programSampleId), // use programSampleId for navigation
        programName: item.programName || "",
        programCode: item.programCode || "",
        accession: item.accessionNumber || "",
        receivedDate: item.receivedDate || "",
        status:
          response.completedEntries > 0
            ? "Completed"
            : "Pending",
      }));

      setTableRows(formatted);
    });
  };

  const handleRowClick = (programSampleId) => {
    window.location.href = `/programView/${programSampleId}`;
  };

  useEffect(() => {
    fetchDashBoard();
  }, []);

  const headers = [
    { key: "programName", header: "Program Name" },
    { key: "programCode", header: "Program Code" },
    { key: "accession", header: "Accession Number" },
    { key: "receivedDate", header: "Received Date" },
    { key: "status", header: "Status" },
  ];

  const tileList = [
    { title: "Total Entries", count: summary.totalEntries },
    { title: "Completed", count: summary.completedEntries },
    { title: "In Progress", count: summary.inProgressEntries },
  ];

  return (
    <div style={{ padding: "20px" }}>
      {/* ----- Dashboard Tiles ----- */}
      <div className="dashboard-container">
        {tileList.map((tile, idx) => (
          <Tile key={idx} className="dashboard-tile">
            <h3 className="tile-title">{tile.title}</h3>
            <p className="tile-value">{tile.count}</p>
          </Tile>
        ))}
      </div>

      {/* ----- Program Entries Table ----- */}
      <TableContainer
        title="Program Sample Entries"
        description="All program submissions"
      >
        <DataTable rows={tableRows} headers={headers}>
          {({ rows, headers, getHeaderProps }) => (
            <Table size="sm">
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
                    style={{ cursor: "pointer" }}
                    onClick={() => handleRowClick(row.id)}
                  >
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DataTable>
      </TableContainer>
    </div>
  );
};

export default ProgramDashboard;
