import {
  Button,
  Column,
  DataTable,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Grid,
  Loading,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
  Tile,
} from "@carbon/react";
import { DocumentPdf, Download } from "@carbon/icons-react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import jsPDF from "jspdf";
import "jspdf-autotable";
import * as XLSX from "xlsx";
import { OEToastNotification } from "../common/OEToastNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { getFromOpenElisServer } from "../utils/Utils";
import ShipmentNavigation from "./ShipmentNavigation";
import "./ShipmentDashboard.css";

const ShipmentReport = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [boxes, setBoxes] = useState([]);
  const [facilities, setFacilities] = useState([]);
  const [loading, setLoading] = useState(false);

  // Filter state
  const [filterBoxId, setFilterBoxId] = useState("");
  const [filterState, setFilterState] = useState("");
  const [filterFacility, setFilterFacility] = useState("");
  const [filterDateFrom, setFilterDateFrom] = useState(null);
  const [filterDateTo, setFilterDateTo] = useState(null);

  const boxStates = [
    { id: "", text: intl.formatMessage({ id: "label.all" }) },
    { id: "DRAFT", text: intl.formatMessage({ id: "shipment.state.draft" }) },
    {
      id: "READY_TO_SEND",
      text: intl.formatMessage({ id: "shipment.state.readyToSend" }),
    },
    { id: "SENT", text: intl.formatMessage({ id: "shipment.state.sent" }) },
    {
      id: "RECEIVED",
      text: intl.formatMessage({ id: "shipment.state.received" }),
    },
    {
      id: "RECONCILED",
      text: intl.formatMessage({ id: "shipment.state.reconciled" }),
    },
  ];

  useEffect(() => {
    fetchFacilities();
  }, []);

  const fetchFacilities = () => {
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      (response) => {
        if (response) {
          const facilityOptions = [
            { id: "", text: intl.formatMessage({ id: "label.all" }) },
            ...response.map((org) => ({ id: org.id, text: org.value })),
          ];
          setFacilities(facilityOptions);
        }
      },
    );
  };

  const handleGenerateReport = () => {
    setLoading(true);

    // Always fetch all boxes and apply all filters client-side
    // This allows combining state + facility + date + boxId filters
    getFromOpenElisServer("/rest/shipping-box", (response) => {
      if (response && Array.isArray(response)) {
        let filtered = response;

        // Filter by state
        if (filterState) {
          filtered = filtered.filter((box) => box.state === filterState);
        }

        // Filter by facility (reference lab)
        if (filterFacility) {
          filtered = filtered.filter(
            (box) => box.destinationFacilityId?.toString() === filterFacility,
          );
        }

        // Filter by box ID (partial match)
        if (filterBoxId) {
          const lowerBoxId = filterBoxId.toLowerCase();
          filtered = filtered.filter((box) =>
            box.boxId?.toLowerCase().includes(lowerBoxId),
          );
        }

        // Filter by date range
        if (filterDateFrom) {
          const from = new Date(filterDateFrom);
          from.setHours(0, 0, 0, 0);
          filtered = filtered.filter(
            (box) => box.createdDate && new Date(box.createdDate) >= from,
          );
        }
        if (filterDateTo) {
          const to = new Date(filterDateTo);
          to.setHours(23, 59, 59, 999);
          filtered = filtered.filter(
            (box) => box.createdDate && new Date(box.createdDate) <= to,
          );
        }

        setBoxes(filtered);
      } else {
        setBoxes([]);
      }
      setLoading(false);
    });
  };

  const getStateLabel = (state) => {
    const stateKey = {
      DRAFT: "shipment.state.draft",
      READY_TO_SEND: "shipment.state.readyToSend",
      SENT: "shipment.state.sent",
      RECEIVED: "shipment.state.received",
      RECONCILED: "shipment.state.reconciled",
    };
    return intl.formatMessage({
      id: stateKey[state] || "shipment.state.draft",
    });
  };

  const renderStateTag = (state) => {
    const stateConfig = {
      DRAFT: "gray",
      READY_TO_SEND: "blue",
      SENT: "purple",
      RECEIVED: "green",
      RECONCILED: "teal",
    };
    return (
      <Tag type={stateConfig[state] || "gray"}>{getStateLabel(state)}</Tag>
    );
  };

  // Report table headers
  const headers = [
    {
      key: "boxId",
      header: intl.formatMessage({ id: "shipment.label.boxId" }),
    },
    {
      key: "contents",
      header: intl.formatMessage({ id: "shipment.report.contents" }),
    },
    {
      key: "sentDate",
      header: intl.formatMessage({ id: "shipment.report.sentDate" }),
    },
    {
      key: "sender",
      header: intl.formatMessage({ id: "shipment.report.sender" }),
    },
    {
      key: "receivedStatus",
      header: intl.formatMessage({ id: "shipment.report.receivedStatus" }),
    },
    {
      key: "receivingUser",
      header: intl.formatMessage({ id: "shipment.report.receivingUser" }),
    },
  ];

  const formatDateTime = (dateStr) => {
    if (!dateStr) return "-";
    const d = new Date(dateStr);
    return (
      d.toLocaleDateString() +
      " " +
      d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    );
  };

  const renderRows = () => {
    return boxes.map((box) => ({
      id: box.id?.toString() || box.boxId,
      boxId: box.boxId || "-",
      contents: box.contents || "-",
      sentDate: formatDateTime(box.sentDate),
      sender: box.createdByName || "-",
      receivedStatus: renderStateTag(box.state),
      receivingUser:
        box.state === "RECEIVED" || box.state === "RECONCILED"
          ? box.createdByName || "-"
          : "-",
    }));
  };

  // Calculate summary stats
  const totalBoxes = boxes.length;
  const totalSamples = boxes.reduce(
    (sum, box) => sum + (box.sampleCount || 0),
    0,
  );

  // Export as PDF
  const handleExportPDF = () => {
    try {
      const doc = new jsPDF();

      doc.setFontSize(16);
      doc.setFont(undefined, "bold");
      doc.text(intl.formatMessage({ id: "shipment.report.title" }), 105, 20, {
        align: "center",
      });

      doc.setFontSize(10);
      doc.setFont(undefined, "normal");
      doc.text(
        intl.formatMessage({ id: "shipment.manifest.generated" }) +
          " " +
          new Date().toLocaleString(),
        14,
        30,
      );

      // Summary
      doc.text(
        intl.formatMessage({ id: "shipment.report.totalBoxes" }) +
          ": " +
          totalBoxes,
        14,
        38,
      );
      doc.text(
        intl.formatMessage({ id: "shipment.report.totalSamples" }) +
          ": " +
          totalSamples,
        100,
        38,
      );

      // Table
      const tableHeaders = headers.map((h) => h.header);
      const tableData = boxes.map((box) => [
        box.boxId || "-",
        box.contents || "-",
        formatDateTime(box.sentDate),
        box.createdByName || "-",
        getStateLabel(box.state),
        box.state === "RECEIVED" || box.state === "RECONCILED"
          ? box.createdByName || "-"
          : "-",
      ]);

      doc.autoTable({
        head: [tableHeaders],
        body: tableData,
        startY: 45,
        styles: { fontSize: 8, cellPadding: 2 },
        headStyles: { fillColor: [0, 112, 210] },
      });

      doc.save("shipment-report.pdf");

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({ id: "shipment.report.export.pdf" }),
      });
    } catch (error) {
      console.error("Error generating PDF:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.generateLabel" }),
      });
    }
  };

  // Export as true Excel (.xlsx)
  const handleExportExcel = () => {
    try {
      const wsData = [
        headers.map((h) => h.header),
        ...boxes.map((box) => [
          box.boxId || "-",
          box.contents || "-",
          formatDateTime(box.sentDate),
          box.createdByName || "-",
          getStateLabel(box.state),
          box.state === "RECEIVED" || box.state === "RECONCILED"
            ? box.createdByName || "-"
            : "-",
        ]),
      ];

      const ws = XLSX.utils.aoa_to_sheet(wsData);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Shipment Report");
      XLSX.writeFile(wb, "shipment-report.xlsx");

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({ id: "shipment.report.export.excel" }),
      });
    } catch (error) {
      console.error("Error generating Excel:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.generateManifest" }),
      });
    }
  };

  const handleClearFilters = () => {
    setFilterBoxId("");
    setFilterState("");
    setFilterFacility("");
    setFilterDateFrom(null);
    setFilterDateTo(null);
    setBoxes([]);
  };

  return (
    <div className="shipment-dashboard">
      <OEToastNotification />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
          {
            label: "shipment.report.title",
            link: "/SampleShipment/reports",
          },
        ]}
      />
      <ShipmentNavigation />

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Tile className="dashboard-header">
            <h2>
              <FormattedMessage id="shipment.report.title" />
            </h2>
            <p>
              <FormattedMessage id="shipment.report.description" />
            </p>
          </Tile>
        </Column>
      </Grid>

      {/* Filters */}
      <Grid fullWidth className="tab-toolbar" style={{ marginTop: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="filter-box-id"
            labelText={intl.formatMessage({ id: "shipment.label.boxId" })}
            placeholder={intl.formatMessage({ id: "shipment.label.boxId" })}
            value={filterBoxId}
            onChange={(e) => setFilterBoxId(e.target.value)}
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <Dropdown
            id="report-state-filter"
            titleText={intl.formatMessage({ id: "shipment.filter.state" })}
            label={intl.formatMessage({ id: "label.select" })}
            items={boxStates}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={boxStates.find((s) => s.id === filterState)}
            onChange={({ selectedItem }) =>
              setFilterState(selectedItem?.id || "")
            }
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <Dropdown
            id="report-facility-filter"
            titleText={intl.formatMessage({
              id: "shipment.filter.referenceLab",
            })}
            label={intl.formatMessage({ id: "label.select" })}
            items={facilities}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={facilities.find((f) => f.id === filterFacility)}
            onChange={({ selectedItem }) =>
              setFilterFacility(selectedItem?.id || "")
            }
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            onChange={([date]) => setFilterDateFrom(date)}
            value={filterDateFrom}
          >
            <DatePickerInput
              id="report-date-from"
              placeholder="mm/dd/yyyy"
              labelText={intl.formatMessage({ id: "shipment.filter.dateFrom" })}
              size="md"
            />
          </DatePicker>
        </Column>
        <Column lg={3} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            onChange={([date]) => setFilterDateTo(date)}
            value={filterDateTo}
          >
            <DatePickerInput
              id="report-date-to"
              placeholder="mm/dd/yyyy"
              labelText={intl.formatMessage({ id: "shipment.filter.dateTo" })}
              size="md"
            />
          </DatePicker>
        </Column>
      </Grid>

      <Grid fullWidth style={{ marginTop: "1rem", marginBottom: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button onClick={handleGenerateReport} disabled={loading}>
              <FormattedMessage id="shipment.report.generate" />
            </Button>
            <Button kind="ghost" onClick={handleClearFilters}>
              <FormattedMessage id="shipment.filter.clear" />
            </Button>
          </div>
        </Column>
      </Grid>

      {loading && <Loading />}

      {!loading && boxes.length > 0 && (
        <>
          {/* Summary */}
          <Grid fullWidth style={{ marginBottom: "1rem" }}>
            <Column lg={4} md={4} sm={4}>
              <Tile>
                <div className="stat-value">{totalBoxes}</div>
                <div className="stat-label">
                  <FormattedMessage id="shipment.report.totalBoxes" />
                </div>
              </Tile>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Tile>
                <div className="stat-value">{totalSamples}</div>
                <div className="stat-label">
                  <FormattedMessage id="shipment.report.totalSamples" />
                </div>
              </Tile>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <div
                style={{
                  display: "flex",
                  gap: "0.5rem",
                  justifyContent: "flex-end",
                  alignItems: "center",
                  height: "100%",
                }}
              >
                <Button
                  kind="tertiary"
                  renderIcon={DocumentPdf}
                  onClick={handleExportPDF}
                >
                  <FormattedMessage id="shipment.report.export.pdf" />
                </Button>
                <Button
                  kind="tertiary"
                  renderIcon={Download}
                  onClick={handleExportExcel}
                >
                  <FormattedMessage id="shipment.report.export.excel" />
                </Button>
              </div>
            </Column>
          </Grid>

          {/* Report Table */}
          <Grid fullWidth>
            <Column lg={16} md={8} sm={4}>
              <DataTable rows={renderRows()} headers={headers}>
                {({
                  rows,
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
                        {rows.map((row) => (
                          <TableRow {...getRowProps({ row })} key={row.id}>
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
            </Column>
          </Grid>
        </>
      )}

      {!loading && boxes.length === 0 && filterBoxId === "" && (
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Tile style={{ textAlign: "center", padding: "2rem" }}>
              <p>
                <FormattedMessage id="shipment.report.noResults" />
              </p>
            </Tile>
          </Column>
        </Grid>
      )}
    </div>
  );
};

export default ShipmentReport;
