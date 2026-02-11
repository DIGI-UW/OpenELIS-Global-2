import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  ClickableTile,
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
  Button,
  ProgressBar,
} from "@carbon/react";
import {
  Time,
  InProgress,
  CheckmarkFilled,
  SendFilled,
  Upload,
  ChartBar,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const STATUS_TAG_MAP = {
  PENDING: { color: "gray", icon: Time },
  IN_PROGRESS: { color: "blue", icon: InProgress },
  COMPLETED: { color: "green", icon: CheckmarkFilled },
  SUBMITTED: { color: "teal", icon: SendFilled },
};

const EQAManagementDashboard = () => {
  const intl = useIntl();
  const [samples, setSamples] = useState([]);
  const [statusFilter, setStatusFilter] = useState("");
  const [summary, setSummary] = useState({
    pending: 0,
    inProgress: 0,
    completed: 0,
    submitted: 0,
  });
  const [performance, setPerformance] = useState({
    participationRate: null,
    onTimeSubmission: null,
    averageZScore: null,
  });

  const fetchData = useCallback(() => {
    let url = "/rest/eqa/samples/dashboard";
    if (statusFilter) {
      url += `?status=${statusFilter}`;
    }
    getFromOpenElisServer(url, (data) => {
      if (data) {
        setSamples(data.samples || []);
        if (data.summary) setSummary(data.summary);
        if (data.performance) setPerformance(data.performance);
      }
    });
  }, [statusFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const summaryTiles = [
    {
      key: "pending",
      label: intl.formatMessage({ id: "eqa.management.status.pending" }),
      value: summary.pending,
      subtitle: intl.formatMessage({ id: "eqa.management.status.pending.sub" }),
      color: "#f4f4f4",
      textColor: "#525252",
    },
    {
      key: "inProgress",
      label: intl.formatMessage({ id: "eqa.management.status.inProgress" }),
      value: summary.inProgress,
      subtitle: intl.formatMessage({
        id: "eqa.management.status.inProgress.sub",
      }),
      color: "#edf5ff",
      textColor: "#0043ce",
    },
    {
      key: "completed",
      label: intl.formatMessage({ id: "eqa.management.status.completed" }),
      value: summary.completed,
      subtitle: intl.formatMessage({
        id: "eqa.management.status.completed.sub",
      }),
      color: "#defbe6",
      textColor: "#198038",
    },
    {
      key: "submitted",
      label: intl.formatMessage({ id: "eqa.management.status.submitted" }),
      value: summary.submitted,
      subtitle: intl.formatMessage({
        id: "eqa.management.status.submitted.sub",
      }),
      color: "#f0fdf4",
      textColor: "#0e6027",
    },
  ];

  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "eqa.management.col.accession" }),
    },
    {
      key: "providerProgram",
      header: intl.formatMessage({ id: "eqa.management.col.provider" }),
    },
    {
      key: "sampleId",
      header: intl.formatMessage({ id: "eqa.management.col.sampleId" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "eqa.management.col.status" }),
    },
    {
      key: "progress",
      header: intl.formatMessage({ id: "eqa.management.col.progress" }),
    },
    {
      key: "deadline",
      header: intl.formatMessage({ id: "eqa.management.col.deadline" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "eqa.management.col.actions" }),
    },
  ];

  const rows = samples.map((s) => ({
    id: String(s.id || s.accessionNumber),
    accessionNumber: s.accessionNumber,
    providerProgram: s.providerName || "",
    programName: s.programName || "",
    sampleId: s.sampleId || "",
    status: s.status || "PENDING",
    testsCompleted: s.testsCompleted || 0,
    testsTotal: s.testsTotal || 0,
    testNames: s.testNames || "",
    deadline: s.deadline || "",
    daysLeft: s.daysLeft,
  }));

  const getDeadlineDisplay = (row) => {
    const date = row.deadline
      ? new Date(row.deadline).toLocaleDateString()
      : "";
    const daysLeft = row.daysLeft;
    return (
      <div>
        <div>{date}</div>
        {daysLeft != null && (
          <div
            style={{
              fontSize: "0.75rem",
              color: daysLeft <= 3 ? "#da1e28" : "#198038",
            }}
          >
            {daysLeft} {intl.formatMessage({ id: "eqa.management.daysLeft" })}
          </div>
        )}
      </div>
    );
  };

  const getActionButton = (row) => {
    if (row.status === "COMPLETED") {
      return (
        <Button kind="primary" size="sm">
          {intl.formatMessage({ id: "eqa.management.action.submit" })}
        </Button>
      );
    }
    return (
      <Button kind="primary" size="sm">
        {intl.formatMessage({ id: "eqa.management.action.enterResults" })}
      </Button>
    );
  };

  return (
    <div style={{ padding: "1rem" }}>
      <h2>{intl.formatMessage({ id: "eqa.management.title" })}</h2>
      <p style={{ color: "#525252", marginBottom: "1.5rem" }}>
        {intl.formatMessage({ id: "eqa.management.subtitle" })}
      </p>

      {/* Summary Tiles */}
      <Grid condensed style={{ marginBottom: "1.5rem" }}>
        {summaryTiles.map((tile) => (
          <Column key={tile.key} lg={4} md={4} sm={4}>
            <ClickableTile
              style={{
                backgroundColor: tile.color,
                borderRadius: "8px",
                padding: "1rem",
                minHeight: "120px",
              }}
            >
              <div
                style={{
                  color: tile.textColor,
                  fontWeight: 600,
                  fontSize: "0.875rem",
                }}
              >
                {tile.label}
              </div>
              <div
                style={{
                  fontSize: "2rem",
                  fontWeight: 700,
                  margin: "0.25rem 0",
                }}
              >
                {tile.value}
              </div>
              <div style={{ fontSize: "0.75rem", color: tile.textColor }}>
                {tile.subtitle}
              </div>
            </ClickableTile>
          </Column>
        ))}
      </Grid>

      {/* Filter bar */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-end",
          marginBottom: "1rem",
          padding: "1rem",
          backgroundColor: "#f4f4f4",
          borderRadius: "8px",
        }}
      >
        <div style={{ width: "200px" }}>
          <Select
            id="eqa-sample-filter"
            labelText=""
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "eqa.management.filter.allSamples",
              })}
            />
            <SelectItem
              value="PENDING"
              text={intl.formatMessage({
                id: "eqa.management.status.pending",
              })}
            />
            <SelectItem
              value="IN_PROGRESS"
              text={intl.formatMessage({
                id: "eqa.management.status.inProgress",
              })}
            />
            <SelectItem
              value="COMPLETED"
              text={intl.formatMessage({
                id: "eqa.management.status.completed",
              })}
            />
            <SelectItem
              value="SUBMITTED"
              text={intl.formatMessage({
                id: "eqa.management.status.submitted",
              })}
            />
          </Select>
        </div>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" size="md" renderIcon={Upload}>
            {intl.formatMessage({ id: "eqa.management.bulkUpload" })}
          </Button>
          <Button kind="secondary" size="md" renderIcon={ChartBar}>
            {intl.formatMessage({ id: "eqa.management.viewPerformance" })}
          </Button>
        </div>
      </div>

      {/* EQA Samples Table */}
      <div
        style={{
          padding: "1rem",
          backgroundColor: "#fff",
          borderRadius: "8px",
          border: "1px solid #e0e0e0",
          marginBottom: "1.5rem",
        }}
      >
        <h4>{intl.formatMessage({ id: "eqa.management.samples.title" })}</h4>
        <p
          style={{
            color: "#525252",
            marginBottom: "1rem",
            fontSize: "0.875rem",
          }}
        >
          {intl.formatMessage({ id: "eqa.management.samples.subtitle" })}
        </p>

        {samples.length === 0 ? (
          <p style={{ color: "#525252" }}>
            {intl.formatMessage({ id: "eqa.management.samples.empty" })}
          </p>
        ) : (
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
                  {tableRows.map((row) => {
                    const rawRow = rows.find((r) => r.id === row.id);
                    return (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => {
                          if (cell.info.header === "accessionNumber") {
                            return (
                              <TableCell key={cell.id}>
                                <div
                                  style={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "0.5rem",
                                  }}
                                >
                                  <Tag type="blue" size="sm">
                                    EQA
                                  </Tag>
                                  {cell.value}
                                </div>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "providerProgram") {
                            return (
                              <TableCell key={cell.id}>
                                <div>{cell.value}</div>
                                <div
                                  style={{
                                    fontSize: "0.75rem",
                                    color: "#525252",
                                  }}
                                >
                                  {rawRow?.programName}
                                </div>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "status") {
                            const statusConfig =
                              STATUS_TAG_MAP[cell.value] ||
                              STATUS_TAG_MAP.PENDING;
                            return (
                              <TableCell key={cell.id}>
                                <Tag type={statusConfig.color} size="sm">
                                  {intl.formatMessage({
                                    id: `eqa.management.status.${(cell.value || "pending").toLowerCase().replace("_", "")}`,
                                    defaultMessage: cell.value,
                                  })}
                                </Tag>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "progress") {
                            return (
                              <TableCell key={cell.id}>
                                <div>
                                  <ProgressBar
                                    value={
                                      rawRow?.testsTotal > 0
                                        ? (rawRow.testsCompleted /
                                            rawRow.testsTotal) *
                                          100
                                        : 0
                                    }
                                    size="small"
                                  />
                                  <div
                                    style={{
                                      fontSize: "0.75rem",
                                      color: "#525252",
                                      marginTop: "0.25rem",
                                    }}
                                  >
                                    {rawRow?.testsCompleted}/
                                    {rawRow?.testsTotal}
                                  </div>
                                  <div
                                    style={{
                                      fontSize: "0.7rem",
                                      color: "#6f6f6f",
                                    }}
                                  >
                                    {rawRow?.testNames}
                                  </div>
                                </div>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "deadline") {
                            return (
                              <TableCell key={cell.id}>
                                {getDeadlineDisplay(rawRow)}
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "actions") {
                            return (
                              <TableCell key={cell.id}>
                                {getActionButton(rawRow)}
                              </TableCell>
                            );
                          }
                          return (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          );
                        })}
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            )}
          </DataTable>
        )}
      </div>

      {/* Performance Summary */}
      <div
        style={{
          padding: "1rem",
          backgroundColor: "#fff",
          borderRadius: "8px",
          border: "1px solid #e0e0e0",
        }}
      >
        <h4 style={{ color: "#0043ce" }}>
          {intl.formatMessage({ id: "eqa.management.performance.title" })}
        </h4>
        <p
          style={{
            color: "#525252",
            marginBottom: "1rem",
            fontSize: "0.875rem",
          }}
        >
          {intl.formatMessage({ id: "eqa.management.performance.subtitle" })}
        </p>
        <Grid condensed>
          <Column lg={5} md={4} sm={4}>
            <div
              style={{
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "8px",
              }}
            >
              <div style={{ color: "#198038", fontSize: "0.875rem" }}>
                {intl.formatMessage({
                  id: "eqa.management.performance.participation",
                })}
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 700 }}>
                {performance.participationRate != null
                  ? `${performance.participationRate}%`
                  : "—"}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#198038" }}>
                {intl.formatMessage({
                  id: "eqa.management.performance.participation.sub",
                })}
              </div>
            </div>
          </Column>
          <Column lg={5} md={4} sm={4}>
            <div
              style={{
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "8px",
              }}
            >
              <div style={{ color: "#198038", fontSize: "0.875rem" }}>
                {intl.formatMessage({
                  id: "eqa.management.performance.onTime",
                })}
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 700 }}>
                {performance.onTimeSubmission != null
                  ? `${performance.onTimeSubmission}%`
                  : "—"}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#198038" }}>
                {performance.onTimeCount || ""}
              </div>
            </div>
          </Column>
          <Column lg={5} md={4} sm={4}>
            <div
              style={{
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "8px",
              }}
            >
              <div style={{ color: "#198038", fontSize: "0.875rem" }}>
                {intl.formatMessage({
                  id: "eqa.management.performance.zScore",
                })}
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 700 }}>
                {performance.averageZScore != null
                  ? performance.averageZScore
                  : "—"}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#198038" }}>
                {intl.formatMessage({
                  id: "eqa.management.performance.zScore.sub",
                })}
              </div>
            </div>
          </Column>
        </Grid>
      </div>
    </div>
  );
};

export default EQAManagementDashboard;
