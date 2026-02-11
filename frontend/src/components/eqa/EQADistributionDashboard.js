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
} from "@carbon/react";
import {
  Time,
  SendFilled,
  CheckmarkFilled,
  GroupPresentation,
  Add,
  ChartBar,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const STATUS_TAG_MAP = {
  DRAFT: { color: "gray", label: "draft" },
  SHIPPED: { color: "purple", label: "shipped" },
  COMPLETED: { color: "green", label: "completed" },
};

const EQADistributionDashboard = () => {
  const intl = useIntl();
  const [shipments, setShipments] = useState([]);
  const [statusFilter, setStatusFilter] = useState("");
  const [summary, setSummary] = useState({
    draft: 0,
    shipped: 0,
    completed: 0,
    participants: 0,
  });
  const [participantNetwork, setParticipantNetwork] = useState({
    totalParticipants: 0,
    activeParticipants: 0,
    averageResponseRate: null,
  });

  const fetchData = useCallback(() => {
    let url = "/rest/eqa/distributions/dashboard";
    if (statusFilter) {
      url += `?status=${statusFilter}`;
    }
    getFromOpenElisServer(url, (data) => {
      if (data) {
        setShipments(data.shipments || data.distributions || []);
        if (data.summary) setSummary(data.summary);
        if (data.participantNetwork)
          setParticipantNetwork(data.participantNetwork);
      }
    });
  }, [statusFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const summaryTiles = [
    {
      key: "draft",
      label: intl.formatMessage({ id: "eqa.distribution.summary.draft" }),
      value: summary.draft,
      subtitle: intl.formatMessage({
        id: "eqa.distribution.summary.draft.sub",
      }),
      color: "#f4f4f4",
      textColor: "#525252",
    },
    {
      key: "shipped",
      label: intl.formatMessage({ id: "eqa.distribution.summary.shipped" }),
      value: summary.shipped,
      subtitle: intl.formatMessage({
        id: "eqa.distribution.summary.shipped.sub",
      }),
      color: "#edf5ff",
      textColor: "#0043ce",
    },
    {
      key: "completed",
      label: intl.formatMessage({ id: "eqa.distribution.summary.completed" }),
      value: summary.completed,
      subtitle: intl.formatMessage({
        id: "eqa.distribution.summary.completed.sub",
      }),
      color: "#defbe6",
      textColor: "#198038",
    },
    {
      key: "participants",
      label: intl.formatMessage({
        id: "eqa.distribution.summary.participants",
      }),
      value: summary.participants,
      subtitle: intl.formatMessage({
        id: "eqa.distribution.summary.participants.sub",
      }),
      color: "#f0fdf4",
      textColor: "#0e6027",
    },
  ];

  const headers = [
    {
      key: "shipmentId",
      header: intl.formatMessage({ id: "eqa.distribution.col.shipmentId" }),
    },
    {
      key: "program",
      header: intl.formatMessage({ id: "eqa.distribution.col.program" }),
    },
    {
      key: "round",
      header: intl.formatMessage({ id: "eqa.distribution.col.round" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "eqa.distribution.col.status" }),
    },
    {
      key: "participants",
      header: intl.formatMessage({ id: "eqa.distribution.col.participants" }),
    },
    {
      key: "samples",
      header: intl.formatMessage({ id: "eqa.distribution.col.samples" }),
    },
    {
      key: "responses",
      header: intl.formatMessage({ id: "eqa.distribution.col.responses" }),
    },
    {
      key: "deadline",
      header: intl.formatMessage({ id: "eqa.distribution.col.deadline" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "eqa.distribution.col.actions" }),
    },
  ];

  const rows = shipments.map((s) => ({
    id: String(s.id || s.shipmentId),
    shipmentId: s.shipmentId || s.distributionName || "",
    program: s.programName || "",
    round: s.round || "",
    status: s.status || "DRAFT",
    participantCount: s.participantCount || 0,
    sampleCount: s.sampleCount || 0,
    responseCount: s.responseCount || 0,
    responseRate: s.responseRate || 0,
    deadline: s.deadline || "",
    shippedDate: s.shippedDate || "",
  }));

  const getActionButton = (row) => {
    if (row.status === "COMPLETED") {
      return (
        <Button kind="secondary" size="sm" renderIcon={ChartBar}>
          {intl.formatMessage({ id: "eqa.distribution.action.viewReport" })}
        </Button>
      );
    }
    if (row.status === "SHIPPED") {
      return (
        <Button kind="secondary" size="sm">
          {intl.formatMessage({ id: "eqa.distribution.action.track" })}
        </Button>
      );
    }
    return (
      <Button kind="primary" size="sm">
        {intl.formatMessage({ id: "eqa.distribution.action.continue" })}
      </Button>
    );
  };

  return (
    <div style={{ padding: "1rem" }}>
      <h2>
        {intl.formatMessage({ id: "eqa.distribution.dashboard.title" })}
      </h2>
      <p style={{ color: "#525252", marginBottom: "1.5rem" }}>
        {intl.formatMessage({ id: "eqa.distribution.dashboard.subtitle" })}
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
            id="eqa-shipment-filter"
            labelText=""
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "eqa.distribution.filter.allShipments",
              })}
            />
            <SelectItem
              value="DRAFT"
              text={intl.formatMessage({
                id: "eqa.distribution.status.draft",
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
        </div>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="primary" size="md" renderIcon={Add}>
            {intl.formatMessage({ id: "eqa.distribution.createShipment" })}
          </Button>
          <Button kind="secondary" size="md" renderIcon={GroupPresentation}>
            {intl.formatMessage({
              id: "eqa.distribution.manageParticipants",
            })}
          </Button>
        </div>
      </div>

      {/* Shipments Table */}
      <div
        style={{
          padding: "1rem",
          backgroundColor: "#fff",
          borderRadius: "8px",
          border: "1px solid #e0e0e0",
          marginBottom: "1.5rem",
        }}
      >
        <h4>
          {intl.formatMessage({ id: "eqa.distribution.shipments.title" })}
        </h4>
        <p
          style={{
            color: "#525252",
            marginBottom: "1rem",
            fontSize: "0.875rem",
          }}
        >
          {intl.formatMessage({ id: "eqa.distribution.shipments.subtitle" })}
        </p>

        {shipments.length === 0 ? (
          <p style={{ color: "#525252" }}>
            {intl.formatMessage({ id: "eqa.distribution.empty" })}
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
                          if (cell.info.header === "round") {
                            return (
                              <TableCell key={cell.id}>
                                {cell.value && (
                                  <Tag type="gray" size="sm">
                                    {cell.value}
                                  </Tag>
                                )}
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "status") {
                            const statusConfig =
                              STATUS_TAG_MAP[cell.value] ||
                              STATUS_TAG_MAP.DRAFT;
                            return (
                              <TableCell key={cell.id}>
                                <Tag type={statusConfig.color} size="sm">
                                  {intl.formatMessage({
                                    id: `eqa.distribution.status.${(cell.value || "draft").toLowerCase()}`,
                                    defaultMessage: cell.value,
                                  })}
                                </Tag>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "participants") {
                            return (
                              <TableCell key={cell.id}>
                                {rawRow?.participantCount}
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "samples") {
                            return (
                              <TableCell key={cell.id}>
                                {rawRow?.sampleCount}
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "responses") {
                            return (
                              <TableCell key={cell.id}>
                                <div>
                                  {rawRow?.responseCount}/
                                  {rawRow?.participantCount}
                                </div>
                                <div
                                  style={{
                                    fontSize: "0.75rem",
                                    color: "#198038",
                                  }}
                                >
                                  {rawRow?.responseRate}%{" "}
                                  {intl.formatMessage({
                                    id: "eqa.distribution.responseRate",
                                  })}
                                </div>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "deadline") {
                            const date = rawRow?.deadline
                              ? new Date(
                                  rawRow.deadline,
                                ).toLocaleDateString()
                              : "";
                            const shipped = rawRow?.shippedDate
                              ? new Date(
                                  rawRow.shippedDate,
                                ).toLocaleDateString()
                              : "";
                            return (
                              <TableCell key={cell.id}>
                                <div>{date}</div>
                                {shipped && (
                                  <div
                                    style={{
                                      fontSize: "0.75rem",
                                      color: "#525252",
                                    }}
                                  >
                                    {intl.formatMessage({
                                      id: "eqa.distribution.shipped",
                                    })}
                                    : {shipped}
                                  </div>
                                )}
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

      {/* Participant Network */}
      <div
        style={{
          padding: "1rem",
          backgroundColor: "#fff",
          borderRadius: "8px",
          border: "1px solid #e0e0e0",
        }}
      >
        <h4 style={{ color: "#0043ce" }}>
          {intl.formatMessage({ id: "eqa.distribution.network.title" })}
        </h4>
        <p
          style={{
            color: "#525252",
            marginBottom: "1rem",
            fontSize: "0.875rem",
          }}
        >
          {intl.formatMessage({ id: "eqa.distribution.network.subtitle" })}
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
              <div style={{ color: "#0043ce", fontSize: "0.875rem" }}>
                {intl.formatMessage({
                  id: "eqa.distribution.network.total",
                })}
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 700 }}>
                {participantNetwork.totalParticipants}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#198038" }}>
                {intl.formatMessage({
                  id: "eqa.distribution.network.total.sub",
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
                  id: "eqa.distribution.network.active",
                })}
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 700 }}>
                {participantNetwork.activeParticipants}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#198038" }}>
                {intl.formatMessage({
                  id: "eqa.distribution.network.active.sub",
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
              <div style={{ color: "#6929c4", fontSize: "0.875rem" }}>
                {intl.formatMessage({
                  id: "eqa.distribution.network.responseRate",
                })}
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 700 }}>
                {participantNetwork.averageResponseRate != null
                  ? `${participantNetwork.averageResponseRate}%`
                  : "—"}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#525252" }}>
                {intl.formatMessage({
                  id: "eqa.distribution.network.responseRate.sub",
                })}
              </div>
            </div>
          </Column>
        </Grid>
      </div>
    </div>
  );
};

export default EQADistributionDashboard;
