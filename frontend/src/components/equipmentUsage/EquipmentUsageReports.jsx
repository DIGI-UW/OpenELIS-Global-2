import React, { useState } from "react";
import {
  Button,
  Grid,
  Column,
  Stack,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  InlineNotification,
} from "@carbon/react";
import { DocumentExport, Printer } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import { EquipmentUsageEntryAPI } from "./EquipmentUsageService";
import EquipmentUsagePrintableForm from "./EquipmentUsagePrintableForm";

const EquipmentUsageReports = () => {
  const [reportType, setReportType] = useState("all");
  const [dateRange, setDateRange] = useState({ start: "", end: "" });
  const [error, setError] = useState(null);
  const [showPrintForm, setShowPrintForm] = useState(false);
  const [printEntries, setPrintEntries] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleExportPDF = async () => {
    // For now, use the print modal as a PDF-capable view
    // In future, can implement backend PDF generation with iText
    try {
      setLoading(true);
      await loadEntriesForPrint();
      setShowPrintForm(true);
    } catch (err) {
      setError("Failed to load entries for export");
    } finally {
      setLoading(false);
    }
  };

  const handleExportExcel = () => {
    // Implement Excel export logic
    console.log("Exporting to Excel with filters:", { reportType, dateRange });
    setError("Excel export coming soon");
  };

  const loadEntriesForPrint = async () => {
    try {
      const filters = {};

      if (reportType !== "all") {
        filters.status = reportType === "pending" ? "DRAFT" :
                        reportType === "approved" ? "APPROVED" :
                        reportType === "by-equipment" ? "ALL" :
                        reportType === "by-operator" ? "ALL" :
                        reportType === "by-department" ? "ALL" : "ALL";
      }

      if (dateRange.start) {
        filters.startDate = formatDateForApi(dateRange.start);
      }
      if (dateRange.end) {
        filters.endDate = formatDateForApi(dateRange.end);
      }

      const entries = await EquipmentUsageEntryAPI.search(filters);
      setPrintEntries(Array.isArray(entries) ? entries : []);
    } catch (err) {
      console.error("Error loading entries for print:", err);
      setError("Failed to load entries for print");
    }
  };

  const handlePrint = async () => {
    try {
      setLoading(true);
      await loadEntriesForPrint();
      setShowPrintForm(true);
    } catch (err) {
      setError("Failed to load entries for printing");
    } finally {
      setLoading(false);
    }
  };

  const formatDateForApi = (dateString) => {
    if (!dateString) return "";
    // Handle both ISO string and Date object
    const date = typeof dateString === "string" ? new Date(dateString) : dateString;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  return (
    <>
      {error && (
        <InlineNotification
          title="Note"
          subtitle={error}
          kind="info"
          onClose={() => setError(null)}
        />
      )}

      <Grid fullWidth={true} style={{ marginBottom: "2rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="report-type"
            labelText="Report Type"
            value={reportType}
            onChange={(e) => setReportType(e.target.value)}
          >
            <SelectItem value="all" text="All Entries" />
            <SelectItem value="pending" text="Pending Approval" />
            <SelectItem value="approved" text="Approved Only" />
            <SelectItem value="by-equipment" text="By Equipment" />
            <SelectItem value="by-operator" text="By Operator" />
            <SelectItem value="by-department" text="By Department" />
          </Select>
        </Column>

        <Column lg={6} md={4} sm={4}>
          <DatePicker
            datePickerType="range"
            onChange={(dates) => {
              if (dates && dates.length === 2) {
                setDateRange({
                  start: dates[0],
                  end: dates[1],
                });
              }
            }}
          >
            <DatePickerInput
              id="start-date"
              placeholder="Start Date"
              labelText="Date Range"
            />
            <DatePickerInput
              id="end-date"
              placeholder="End Date"
              labelText="End Date"
            />
          </DatePicker>
        </Column>

        <Column lg={6} md={4} sm={4} style={{ display: "flex", alignItems: "flex-end" }}>
          <Stack orientation="horizontal" gap={3}>
            <Button
              kind="primary"
              renderIcon={DocumentExport}
              onClick={handleExportPDF}
            >
              <FormattedMessage
                id="reports.button.exportPDF"
                defaultMessage="Export PDF"
              />
            </Button>
            <Button
              kind="secondary"
              renderIcon={DocumentExport}
              onClick={handleExportExcel}
            >
              <FormattedMessage
                id="reports.button.exportExcel"
                defaultMessage="Export Excel"
              />
            </Button>
            <Button
              kind="ghost"
              renderIcon={Printer}
              onClick={handlePrint}
            >
              <FormattedMessage
                id="reports.button.print"
                defaultMessage="Print"
              />
            </Button>
          </Stack>
        </Column>
      </Grid>

      <div className="report-info">
        <h3>
          <FormattedMessage
            id="equipment.usage.reports.info"
            defaultMessage="Report Information"
          />
        </h3>
        <p>
          <FormattedMessage
            id="equipment.usage.reports.description"
            defaultMessage="Select report type and date range to generate equipment usage reports. Reports can be exported in PDF or Excel format, or printed directly."
          />
        </p>

        <h4>Report Types:</h4>
        <ul>
          <li>
            <strong>All Entries:</strong> All equipment usage entries for the selected date range
          </li>
          <li>
            <strong>Pending Approval:</strong> Entries awaiting supervisor approval
          </li>
          <li>
            <strong>Approved Only:</strong> Approved entries only
          </li>
          <li>
            <strong>By Equipment:</strong> Usage summarized by equipment
          </li>
          <li>
            <strong>By Operator:</strong> Usage summarized by operator
          </li>
          <li>
            <strong>By Department:</strong> Usage summarized by department
          </li>
        </ul>

        <h4>MNTD Format:</h4>
        <p>
          All reports are formatted according to MNTD Equipment Usage Format 5.3-003,
          including operator signature, approval status, and equipment condition tracking.
        </p>
      </div>

      {showPrintForm && (
        <div style={{ marginTop: "40px", marginBottom: "20px" }}>
          <EquipmentUsagePrintableForm
            entries={printEntries}
            onClose={() => setShowPrintForm(false)}
          />
        </div>
      )}
    </>
  );
};

export default EquipmentUsageReports;
