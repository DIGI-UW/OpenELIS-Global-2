import React from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Section E: Sample Release Record (AHRI BR-F-02)
 * Read-only display of release details after samples have been retrieved.
 */
function ReleaseRecordSection({ request }) {
  const intl = useIntl();

  if (!request || !request.items || request.items.length === 0) return null;

  // Only show items that have been retrieved or beyond
  const releasedItems = request.items.filter(
    (item) =>
      item.status === "RETRIEVED" ||
      item.status === "IN_ANALYSIS" ||
      item.status === "RETURNED" ||
      item.status === "PARTIALLY_USED" ||
      item.status === "CONSUMED",
  );

  if (releasedItems.length === 0) return null;

  const headers = [
    {
      key: "sampleNumber",
      header: intl.formatMessage({
        id: "biorepo.import.release.sampleNumber",
        defaultMessage: "Sample Number",
      }),
    },
    {
      key: "dateReleased",
      header: intl.formatMessage({
        id: "biorepo.import.release.dateReleased",
        defaultMessage: "Date Released",
      }),
    },
    {
      key: "releasedBy",
      header: intl.formatMessage({
        id: "biorepo.import.release.releasedBy",
        defaultMessage: "Released By",
      }),
    },
    {
      key: "condition",
      header: intl.formatMessage({
        id: "biorepo.import.release.condition",
        defaultMessage: "Condition at Release",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "biorepo.import.release.status",
        defaultMessage: "Status",
      }),
    },
  ];

  const rows = releasedItems.map((item) => ({
    id: item.id.toString(),
    sampleNumber: item.barcode || item.sampleNumber || "-",
    dateReleased: item.retrievedTimestamp || item.releasedTimestamp || "-",
    releasedBy: item.retrievedByName || "-",
    condition: item.conditionAtRelease || "-",
    status: item.status,
  }));

  const statusTagType = {
    RETRIEVED: "blue",
    IN_ANALYSIS: "teal",
    RETURNED: "green",
    PARTIALLY_USED: "warm-gray",
    CONSUMED: "cool-gray",
  };

  return (
    <div className="biorepo-section" style={{ marginBottom: "2rem" }}>
      <h4 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="biorepo.import.section.release"
          defaultMessage="Section E: Sample Release Record"
        />
      </h4>
      <DataTable rows={rows} headers={headers} size="sm">
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
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
                {rows.map((row) => {
                  const item = releasedItems.find(
                    (i) => i.id.toString() === row.id,
                  );
                  return (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            <Tag
                              type={statusTagType[cell.value] || "cool-gray"}
                              size="sm"
                            >
                              {cell.value}
                            </Tag>
                          ) : cell.info.header === "condition" ? (
                            <Tag
                              type={
                                cell.value === "Intact" || cell.value === "Good"
                                  ? "green"
                                  : "red"
                              }
                              size="sm"
                            >
                              {cell.value}
                            </Tag>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
    </div>
  );
}

export default ReleaseRecordSection;
