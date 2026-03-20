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
  Select,
  SelectItem,
  Grid,
  Column,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

const STATUS_TAG_MAP = {
  DRAFT: "gray",
  PREPARED: "blue",
  SHIPPED: "teal",
  COMPLETED: "green",
};

const DistributionList = ({ onSelect }) => {
  const intl = useIntl();
  const [distributions, setDistributions] = useState([]);
  const [statusFilter, setStatusFilter] = useState("");

  useEffect(() => {
    let url = "/rest/eqa/distributions";
    if (statusFilter) {
      url += `?status=${statusFilter}`;
    }
    getFromOpenElisServer(url, (data) => {
      if (data && data.distributions) {
        setDistributions(data.distributions);
      }
    });
  }, [statusFilter]);

  const headers = [
    {
      key: "distributionName",
      header: intl.formatMessage({ id: "eqa.distribution.name" }),
    },
    {
      key: "programName",
      header: intl.formatMessage({ id: "eqa.distribution.program" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "alerts.table.status" }),
    },
    {
      key: "deadline",
      header: intl.formatMessage({ id: "eqa.distribution.deadline" }),
    },
  ];

  const rows = distributions.map((d) => ({
    id: String(d.id),
    distributionName: d.distributionName,
    programName: d.programName || "",
    status: d.status,
    deadline: d.deadline ? new Date(d.deadline).toLocaleDateString() : "",
  }));

  return (
    <div>
      <h3>{intl.formatMessage({ id: "eqa.distribution.list.title" })}</h3>
      <Grid condensed style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="dist-status-filter"
            labelText={intl.formatMessage({
              id: "eqa.distribution.filter.status",
            })}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <SelectItem value="" text="" />
            <SelectItem
              value="DRAFT"
              text={intl.formatMessage({ id: "eqa.distribution.status.draft" })}
            />
            <SelectItem
              value="PREPARED"
              text={intl.formatMessage({
                id: "eqa.distribution.status.prepared",
              })}
            />
            <SelectItem
              value="SHIPPED"
              text={intl.formatMessage({
                id: "eqa.distribution.status.shipped",
              })}
            />
            <SelectItem
              value="COMPLETED"
              text={intl.formatMessage({
                id: "eqa.distribution.status.completed",
              })}
            />
          </Select>
        </Column>
      </Grid>

      <DataTable rows={rows} headers={headers}>
        {({
          rows: tableRows,
          headers: tableHeaders,
          getTableProps,
          getHeaderProps,
          getRowProps,
        }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {tableHeaders.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {tableRows.map((row) => (
                <TableRow
                  key={row.id}
                  {...getRowProps({ row })}
                  onClick={() => onSelect && onSelect(row.id)}
                  style={{ cursor: "pointer" }}
                >
                  {row.cells.map((cell) => {
                    if (cell.info.header === "status") {
                      return (
                        <TableCell key={cell.id}>
                          <Tag
                            type={STATUS_TAG_MAP[cell.value] || "gray"}
                            size="sm"
                          >
                            {intl.formatMessage({
                              id: `eqa.distribution.status.${(cell.value || "").toLowerCase()}`,
                              defaultMessage: cell.value,
                            })}
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

export default DistributionList;
