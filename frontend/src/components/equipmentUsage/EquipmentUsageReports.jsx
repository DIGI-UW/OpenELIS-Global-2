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

const EquipmentUsageReports = () => {
  const [reportType, setReportType] = useState("all");
  const [dateRange, setDateRange] = useState({ start: "", end: "" });
  const [error, setError] = useState(null);

  const handleExportPDF = () => {
    // Implement PDF export logic
    console.log("Exporting to PDF with filters:", { reportType, dateRange });
    setError("PDF export coming soon");
  };

  const handleExportExcel = () => {
    // Implement Excel export logic
    console.log("Exporting to Excel with filters:", { reportType, dateRange });
    setError("Excel export coming soon");
  };

  const handlePrint = () => {
    // Implement print logic
    window.print();
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
            <DatePickerInput id="end-date" placeholder="End Date" />
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
    </>
  );
};

export default EquipmentUsageReports;
