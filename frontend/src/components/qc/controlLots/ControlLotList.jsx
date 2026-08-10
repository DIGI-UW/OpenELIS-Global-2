/**
 * ControlLotList Component
 *
 * Lists all QC control lots with status filtering and navigation to create/edit.
 */

import React, { useState, useEffect, useCallback, useRef } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Grid,
  Column,
  Dropdown,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Button,
  Loading,
  InlineNotification,
  Modal,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageTitle from "../../common/PageTitle/PageTitle";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import LeveyJenningsChart from "../charts/LeveyJenningsChart";

const STATUS_TAG = {
  ESTABLISHMENT: "gray",
  ACTIVE: "green",
  EXPIRED: "red",
};

const headers = [
  { key: "lotNumber", header: "qc.controlLot.field.lotNumber" },
  { key: "productName", header: "qc.controlLot.field.material" },
  { key: "manufacturer", header: "qc.controlLot.field.manufacturer" },
  { key: "controlLevel", header: "qc.controlLot.field.level" },
  { key: "status", header: "qc.controlLot.field.status" },
  { key: "calculationMethod", header: "qc.controlLot.statistics.method" },
  { key: "expirationDate", header: "qc.controlLot.field.expiration" },
  { key: "actions", header: "" },
];

const ControlLotList = () => {
  const intl = useIntl();
  const history = useHistory();

  const [lots, setLots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState("");
  // GAP-3: lot-level Levey-Jennings chart. The lot-scoped endpoints existed
  // before this — the list just never linked to them.
  const [chartLot, setChartLot] = useState(null);
  const [chartData, setChartData] = useState([]);
  const [chartStatistics, setChartStatistics] = useState(null);
  const [chartLoading, setChartLoading] = useState(false);

  // Guards against a stale response: reopening the modal for another lot bumps
  // the sequence, and any response from a superseded request is dropped —
  // otherwise lot A's slow chart data could render under lot B's heading.
  const chartRequestSeq = useRef(0);

  const openChart = (lot) => {
    const seq = ++chartRequestSeq.current;
    setChartLot(lot);
    setChartData([]);
    setChartStatistics(null);
    setChartLoading(true);

    let completedCalls = 0;
    const checkDone = () => {
      completedCalls++;
      if (completedCalls >= 2) {
        setChartLoading(false);
      }
    };

    getFromOpenElisServer(`/rest/qc/charts/${lot.id}`, (response) => {
      if (seq !== chartRequestSeq.current) return;
      const dataPoints =
        response?.dataPoints || response?.data?.dataPoints || [];
      setChartData(
        dataPoints
          .filter((pt) => (pt.zscore ?? pt.zScore) != null)
          .map((pt) => ({
            id: pt.resultId,
            runDateTime: pt.timestamp,
            resultValue: pt.value,
            value: pt.value,
            zScore: pt.zscore ?? pt.zScore,
            violated: pt.hasViolation,
            violations: (pt.violatedRules || []).map((rule) => ({
              code: rule,
            })),
          })),
      );
      checkDone();
    });

    getFromOpenElisServer(
      `/rest/qc/charts/${lot.id}/statistics`,
      (response) => {
        if (seq !== chartRequestSeq.current) return;
        setChartStatistics(response && response.mean != null ? response : null);
        checkDone();
      },
    );
  };

  const statusOptions = [
    {
      id: "",
      label: intl.formatMessage({ id: "qc.controlLot.filter.allStatuses" }),
    },
    {
      id: "ESTABLISHMENT",
      label: intl.formatMessage({
        id: "qc.controlLot.status.establishment",
      }),
    },
    {
      id: "ACTIVE",
      label: intl.formatMessage({ id: "qc.controlLot.status.active" }),
    },
    {
      id: "EXPIRED",
      label: intl.formatMessage({ id: "qc.controlLot.status.expired" }),
    },
  ];

  const loadLots = useCallback(() => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer("/rest/qc/control-lots", (response) => {
      if (Array.isArray(response)) {
        setLots(response);
      } else if (response && Array.isArray(response.data)) {
        setLots(response.data);
      } else {
        setLots([]);
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    loadLots();
  }, [loadLots]);

  const filteredLots = statusFilter
    ? lots.filter((l) => l.status === statusFilter)
    : lots;

  const rows = filteredLots.map((lot) => ({
    id: lot.id,
    lotNumber: lot.lotNumber || "-",
    productName: lot.productName || "-",
    manufacturer: lot.manufacturer || "-",
    controlLevel: lot.controlLevel || "-",
    status: lot.status,
    calculationMethod: lot.calculationMethod
      ? intl.formatMessage({
          id: `qc.controlLot.statistics.method.${lot.calculationMethod.toLowerCase()}`,
          defaultMessage: lot.calculationMethod,
        })
      : "-",
    expirationDate: lot.expirationDate
      ? new Date(lot.expirationDate).toLocaleDateString()
      : "-",
    actions: lot.id,
  }));

  const translatedHeaders = headers.map((h) => ({
    key: h.key,
    header: h.header
      ? intl.formatMessage({ id: h.header, defaultMessage: h.header })
      : "",
  }));

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "qc.controlLot.loading" })}
        withOverlay={false}
      />
    );
  }

  return (
    <div data-testid="control-lot-list">
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "analyzer.page.hierarchy.root", link: "" },
          { label: "qc.dashboard.title", link: "" },
          { label: "qc.controlLots.title", link: "" },
        ]}
      />
      <PageTitle
        breadcrumbs={[
          {
            label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
            link: "/analyzers",
          },
          {
            label: intl.formatMessage({ id: "qc.dashboard.title" }),
            link: "/analyzers/qc/db",
          },
          {
            label: intl.formatMessage({ id: "qc.controlLots.title" }),
          },
        ]}
        subtitle={intl.formatMessage({ id: "qc.controlLots.subtitle" })}
      />

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.controlLot.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
        />
      )}

      <Grid>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="status-filter"
            titleText={intl.formatMessage({ id: "qc.controlLot.field.status" })}
            label={intl.formatMessage({
              id: "qc.controlLot.filter.allStatuses",
            })}
            items={statusOptions}
            itemToString={(item) => item?.label || ""}
            selectedItem={
              statusOptions.find((o) => o.id === statusFilter) ||
              statusOptions[0]
            }
            onChange={({ selectedItem }) =>
              setStatusFilter(selectedItem?.id || "")
            }
          />
        </Column>
        <Column
          lg={12}
          md={4}
          sm={4}
          style={{
            display: "flex",
            alignItems: "flex-end",
            justifyContent: "flex-end",
          }}
        >
          <Button
            renderIcon={Add}
            onClick={() => history.push("/analyzers/qc/control-lots/new")}
            data-testid="add-control-lot-button"
          >
            {intl.formatMessage({ id: "qc.controlLot.new.title" })}
          </Button>
        </Column>
      </Grid>

      <DataTable rows={rows} headers={translatedHeaders} isSortable>
        {({
          rows: tableRows,
          headers: tableHeaders,
          getTableProps,
          getHeaderProps,
          getRowProps,
        }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {tableHeaders.map((header) => (
                    <TableHeader
                      {...getHeaderProps({ header })}
                      key={header.key}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {tableRows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={headers.length}>
                      {intl.formatMessage({ id: "qc.controlLots.empty" })}
                    </TableCell>
                  </TableRow>
                ) : (
                  tableRows.map((row) => (
                    <TableRow {...getRowProps({ row })} key={row.id}>
                      {row.cells.map((cell) => {
                        if (cell.info.header === "status") {
                          return (
                            <TableCell key={cell.id}>
                              <Tag type={STATUS_TAG[cell.value] || "gray"}>
                                {cell.value
                                  ? intl.formatMessage({
                                      id: `qc.controlLot.status.${cell.value.toLowerCase()}`,
                                      defaultMessage: cell.value,
                                    })
                                  : "-"}
                              </Tag>
                            </TableCell>
                          );
                        }
                        if (cell.info.header === "actions") {
                          return (
                            <TableCell key={cell.id}>
                              <OverflowMenu flipped size="sm">
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "button.edit",
                                  })}
                                  onClick={() =>
                                    history.push(
                                      `/analyzers/qc/control-lots/${cell.value}`,
                                    )
                                  }
                                />
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "qc.controlLot.viewChart",
                                  })}
                                  onClick={() =>
                                    openChart(
                                      filteredLots.find(
                                        (lot) => lot.id === cell.value,
                                      ),
                                    )
                                  }
                                />
                              </OverflowMenu>
                            </TableCell>
                          );
                        }
                        return (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        );
                      })}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>

      {chartLot && (
        <Modal
          open
          passiveModal
          size="lg"
          modalHeading={`${chartLot.lotNumber || ""} — ${intl.formatMessage({
            id: "qc.controlLot.viewChart",
          })}`}
          onRequestClose={() => setChartLot(null)}
          data-testid="control-lot-chart-modal"
        >
          {chartLoading ? (
            <Loading
              withOverlay={false}
              small
              description={intl.formatMessage({
                id: "qc.instrumentDetail.chart.loading",
              })}
            />
          ) : chartData.length === 0 ? (
            <p data-testid="control-lot-chart-empty">
              {intl.formatMessage({ id: "qc.instrumentDetail.chart.noData" })}
            </p>
          ) : (
            <LeveyJenningsChart
              data={chartData}
              statistics={chartStatistics}
              height="350px"
              showLegend={true}
            />
          )}
        </Modal>
      )}
    </div>
  );
};

export default ControlLotList;
