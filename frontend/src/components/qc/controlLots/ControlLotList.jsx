/**
 * ControlLotList Component
 *
 * Lists all QC control lots with status filtering and navigation to create/edit.
 */

import React, { useState, useEffect, useCallback } from "react";
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
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageTitle from "../../common/PageTitle/PageTitle";

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
    </div>
  );
};

export default ControlLotList;
