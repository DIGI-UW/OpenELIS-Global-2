import React, { useState, useEffect } from "react";
import {
  Button,
  ButtonSet,
  DatePicker,
  DatePickerInput,
  Loading,
  InlineNotification,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Printer } from "@carbon/icons-react";
import CartridgeUsageAPI from "./EquipmentUsageService";
import "./EquipmentUsagePrintable.css";

/**
 * EquipmentUsagePrintableReport Component
 *
 * Displays a formatted, printable report of equipment usage.
 * Features:
 * - Select date range for report
 * - View usage summary
 * - Print-friendly layout
 * - Export to PDF
 */
const EquipmentUsagePrintableReport = () => {
  const intl = useIntl();

  // State
  const [usageData, setUsageData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [reportGenerated, setReportGenerated] = useState(false);

  // Filter State
  const [filters, setFilters] = useState({
    startDate: "",
    endDate: "",
  });

  // Load Usage Data when report is generated
  const handleGenerateReport = () => {
    if (!filters.startDate || !filters.endDate) {
      setError(
        intl.formatMessage({
          id: "equipment.usage.error.selectDates",
          defaultMessage: "Please select both start and end dates",
        })
      );
      return;
    }

    setLoading(true);
    setError(null);

    try {
      CartridgeUsageAPI.getUsageHistory(null, (data) => {
        if (data && Array.isArray(data)) {
          // Filter by date range
          let filtered = data;
          const startDate = new Date(filters.startDate);
          const endDate = new Date(filters.endDate);
          endDate.setHours(23, 59, 59, 999);

          filtered = filtered.filter((record) => {
            const recordDate = new Date(record.usageDate);
            return recordDate >= startDate && recordDate <= endDate;
          });

          setUsageData(filtered);
          setReportGenerated(true);
        }
        setLoading(false);
      });
    } catch (error) {
      console.error("Error generating report:", error);
      setError(
        intl.formatMessage({
          id: "equipment.usage.error.reportFailed",
          defaultMessage: "Failed to generate report",
        })
      );
      setLoading(false);
    }
  };

  // Print Report
  const handlePrint = () => {
    window.print();
  };

  // Export to CSV (simplified alternative to PDF)
  const handleExportCSV = () => {
    if (usageData.length === 0) {
      setError(
        intl.formatMessage({
          id: "equipment.usage.error.noData",
          defaultMessage: "No data to export",
        })
      );
      return;
    }

    // Prepare CSV data
    const headers = [
      "Equipment Usage Report",
      "",
      "Date Range: " + filters.startDate + " to " + filters.endDate,
      "Generated: " + new Date().toLocaleString(),
      "",
      "Date",
      "Equipment",
      "Lot Number",
      "Quantity",
      "User",
      "Test Result ID",
    ];

    const rows = usageData.map((record) => [
      new Date(record.usageDate).toLocaleDateString(),
      record.inventoryItem?.name || "",
      record.lot?.lotNumber || "",
      record.quantityUsed || "",
      record.performedByUser || "",
      record.testResultId || "",
    ]);

    // Create CSV content
    const csvContent = [
      ...headers,
      ...rows.map((row) => row.map((cell) => `"${cell}"`).join(",")),
    ].join("\n");

    // Create and download file
    const blob = new Blob([csvContent], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `equipment-usage-report-${new Date().toISOString().split("T")[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="equipmentUsageReportContainer">
      <h3>
        <FormattedMessage
          id="equipment.usage.reports.title"
          defaultMessage="Equipment Usage Report"
        />
      </h3>

      {error && (
        <InlineNotification
          kind="error"
          title="Error"
          subtitle={error}
          onClose={() => setError(null)}
        />
      )}

      {/* Report Filters */}
      <div className="reportFiltersSection">
        <h4>
          <FormattedMessage
            id="equipment.usage.reports.selectDateRange"
            defaultMessage="Select Date Range"
          />
        </h4>
        <div className="filterRow">
          <DatePickerInput
            labelText={intl.formatMessage({
              id: "equipment.usage.reports.fromDate",
              defaultMessage: "From Date",
            })}
            placeholder="mm/dd/yyyy"
            value={filters.startDate}
            onChange={(e) =>
              setFilters({ ...filters, startDate: e.target.value })
            }
          />
          <DatePickerInput
            labelText={intl.formatMessage({
              id: "equipment.usage.reports.toDate",
              defaultMessage: "To Date",
            })}
            placeholder="mm/dd/yyyy"
            value={filters.endDate}
            onChange={(e) =>
              setFilters({ ...filters, endDate: e.target.value })
            }
          />
          <Button kind="primary" onClick={handleGenerateReport}>
            <FormattedMessage
              id="equipment.usage.reports.generate"
              defaultMessage="Generate Report"
            />
          </Button>
        </div>
      </div>

      {/* Loading State */}
      {loading && <Loading description="Generating report..." />}

      {/* Report Display */}
      {reportGenerated && !loading && usageData.length > 0 && (
        <>
          {/* Printable Report */}
          <div className="printableReport">
            <div className="reportHeader">
              <h2>
                <FormattedMessage
                  id="equipment.usage.reports.title"
                  defaultMessage="Equipment Usage Report"
                />
              </h2>
              <div className="reportMetadata">
                <p>
                  <strong>
                    <FormattedMessage
                      id="equipment.usage.reports.dateRange"
                      defaultMessage="Date Range"
                    />
                    :
                  </strong>{" "}
                  {filters.startDate} to {filters.endDate}
                </p>
                <p>
                  <strong>
                    <FormattedMessage
                      id="equipment.usage.reports.generated"
                      defaultMessage="Generated"
                    />
                    :
                  </strong>{" "}
                  {new Date().toLocaleString()}
                </p>
              </div>
            </div>

            {/* Summary Stats */}
            <div className="reportSummary">
              <div className="summaryCard">
                <span className="summaryLabel">
                  <FormattedMessage
                    id="equipment.usage.reports.totalRecords"
                    defaultMessage="Total Records"
                  />
                </span>
                <span className="summaryValue">{usageData.length}</span>
              </div>
              <div className="summaryCard">
                <span className="summaryLabel">
                  <FormattedMessage
                    id="equipment.usage.reports.uniqueEquipment"
                    defaultMessage="Unique Equipment"
                  />
                </span>
                <span className="summaryValue">
                  {new Set(
                    usageData.map((r) => r.inventoryItem?.id)
                  ).size}
                </span>
              </div>
              <div className="summaryCard">
                <span className="summaryLabel">
                  <FormattedMessage
                    id="equipment.usage.reports.totalQuantity"
                    defaultMessage="Total Quantity Used"
                  />
                </span>
                <span className="summaryValue">
                  {usageData.reduce((sum, r) => sum + (r.quantityUsed || 0), 0)}
                </span>
              </div>
            </div>

            {/* Report Table */}
            <div className="reportTableSection">
              <table className="reportTable">
                <thead>
                  <tr>
                    <th>
                      <FormattedMessage
                        id="equipment.usage.reports.date"
                        defaultMessage="Date"
                      />
                    </th>
                    <th>
                      <FormattedMessage
                        id="equipment.usage.reports.equipment"
                        defaultMessage="Equipment"
                      />
                    </th>
                    <th>
                      <FormattedMessage
                        id="equipment.usage.reports.lotNumber"
                        defaultMessage="Lot Number"
                      />
                    </th>
                    <th>
                      <FormattedMessage
                        id="equipment.usage.reports.quantity"
                        defaultMessage="Quantity"
                      />
                    </th>
                    <th>
                      <FormattedMessage
                        id="equipment.usage.reports.user"
                        defaultMessage="User"
                      />
                    </th>
                    <th>
                      <FormattedMessage
                        id="equipment.usage.reports.testResult"
                        defaultMessage="Test Result ID"
                      />
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {usageData.map((record, index) => (
                    <tr key={record.id || index}>
                      <td>
                        {record.usageDate
                          ? new Date(record.usageDate).toLocaleDateString()
                          : "—"}
                      </td>
                      <td>{record.inventoryItem?.name || "—"}</td>
                      <td>{record.lot?.lotNumber || "—"}</td>
                      <td>{record.quantityUsed || "—"}</td>
                      <td>{record.performedByUser || "—"}</td>
                      <td>{record.testResultId || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Report Footer */}
            <div className="reportFooter">
              <p>
                <FormattedMessage
                  id="equipment.usage.reports.signature"
                  defaultMessage="Authorized by: ___________________________"
                />
              </p>
              <p>
                <FormattedMessage
                  id="equipment.usage.reports.date"
                  defaultMessage="Date: ___________________________"
                />
              </p>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="reportActionsSection">
            <ButtonSet>
              <Button kind="secondary" onClick={handlePrint}>
                <Printer size={16} style={{ marginRight: "8px" }} />
                <FormattedMessage
                  id="common.print"
                  defaultMessage="Print"
                />
              </Button>
              <Button kind="secondary" onClick={handleExportCSV}>
                <FormattedMessage
                  id="common.export"
                  defaultMessage="Export"
                />
              </Button>
            </ButtonSet>
          </div>
        </>
      )}

      {/* Empty State */}
      {reportGenerated && !loading && usageData.length === 0 && (
        <div className="noDataMessage">
          <p>
            <FormattedMessage
              id="equipment.usage.reports.noData"
              defaultMessage="No usage records found for the selected date range"
            />
          </p>
        </div>
      )}
    </div>
  );
};

export default EquipmentUsagePrintableReport;
