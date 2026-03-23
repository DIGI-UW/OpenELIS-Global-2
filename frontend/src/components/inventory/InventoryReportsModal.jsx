import React, { useState } from "react";
import {
  Modal,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Checkbox,
  FormGroup,
  FormLabel,
  Stack,
  Button,
  InlineNotification,
} from "@carbon/react";
import { Download } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { ReportsAPI } from "./InventoryService";

const InventoryReportsModal = ({ open, onClose }) => {
  const intl = useIntl();

  const reportTypes = [
    {
      id: "STOCK_LEVELS",
      text: intl.formatMessage({ id: "reports.type.stockLevels" }),
      description: intl.formatMessage({
        id: "reports.type.stockLevels.description",
      }),
    },
    {
      id: "EXPIRATION_FORECAST",
      text: intl.formatMessage({ id: "reports.type.expirationForecast" }),
      description: intl.formatMessage({
        id: "reports.type.expirationForecast.description",
      }),
    },
    {
      id: "USAGE_TRENDS",
      text: intl.formatMessage({ id: "reports.type.usageTrends" }),
      description: intl.formatMessage({
        id: "reports.type.usageTrends.description",
      }),
    },
    {
      id: "LOT_TRACEABILITY",
      text: intl.formatMessage({ id: "reports.type.lotTraceability" }),
      description: intl.formatMessage({
        id: "reports.type.lotTraceability.description",
      }),
    },
    {
      id: "LOW_STOCK",
      text: intl.formatMessage({ id: "reports.type.lowStock" }),
      description: intl.formatMessage({
        id: "reports.type.lowStock.description",
      }),
    },
    {
      id: "TRANSACTION_HISTORY",
      text: intl.formatMessage({ id: "reports.type.transactionHistory" }),
      description: intl.formatMessage({
        id: "reports.type.transactionHistory.description",
      }),
    },
  ];

  const exportFormats = [
    { id: "PDF", text: "PDF" },
    { id: "EXCEL", text: "Excel (.xlsx)" },
    { id: "CSV", text: "CSV" },
  ];

  const [formData, setFormData] = useState({
    reportType: reportTypes[0],
    exportFormat: exportFormats[0],
    startDate: null,
    endDate: null,
    includeInactive: false,
    includeExpired: true,
    groupByType: false,
    groupByLocation: false,
  });

  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  };

  const validate = () => {
    if (
      ["USAGE_TRENDS", "TRANSACTION_HISTORY"].includes(
        formData.reportType.id,
      ) &&
      (!formData.startDate || !formData.endDate)
    ) {
      setError(intl.formatMessage({ id: "reports.error.dateRangeRequired" }));
      return false;
    }

    if (
      formData.startDate &&
      formData.endDate &&
      new Date(formData.startDate) > new Date(formData.endDate)
    ) {
      setError(intl.formatMessage({ id: "reports.error.invalidDateRange" }));
      return false;
    }

    return true;
  };

  // Handle form submission
  const handleGenerate = async () => {
    if (!validate()) return;

    setGenerating(true);
    setError(null);

    try {
      const reportParams = {
        reportType: formData.reportType.id,
        exportFormat: formData.exportFormat.id,
        startDate: formData.startDate,
        endDate: formData.endDate,
        includeInactive: formData.includeInactive,
        includeExpired: formData.includeExpired,
        groupByType: formData.groupByType,
        groupByLocation: formData.groupByLocation,
      };

      const response = await ReportsAPI.generate(reportParams);

      // Download the file
      const blob = new Blob([response.data], {
        type: response.contentType,
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download =
        response.filename ||
        `inventory-report.${formData.exportFormat.id.toLowerCase()}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      onClose();
    } catch (err) {
      console.error("Error generating report:", err);
      setError(
        err.message ||
          intl.formatMessage({ id: "reports.error.generationFailed" }),
      );
    } finally {
      setGenerating(false);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    setFormData({
      reportType: reportTypes[0],
      exportFormat: exportFormats[0],
      startDate: null,
      endDate: null,
      includeInactive: false,
      includeExpired: true,
      groupByType: false,
      groupByLocation: false,
    });
    setError(null);
    onClose();
  };

  // Check if date range is required
  const isDateRangeRequired = ["USAGE_TRENDS", "TRANSACTION_HISTORY"].includes(
    formData.reportType.id,
  );

  return (
    <Modal
      open={open}
      onRequestClose={handleCancel}
      onRequestSubmit={handleGenerate}
      modalHeading={intl.formatMessage({ id: "reports.generate.title" })}
      primaryButtonText={intl.formatMessage({ id: "button.generate" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={generating}
      size="md"
    >
      <Stack gap={6}>
        {/* Info notification */}
        <InlineNotification
          kind="info"
          title={intl.formatMessage({ id: "reports.info.title" })}
          subtitle={intl.formatMessage({ id: "reports.info.message" })}
          hideCloseButton
          lowContrast
        />

        {/* Report Type */}
        <Dropdown
          id="reportType"
          titleText={intl.formatMessage({ id: "reports.type" })}
          label={intl.formatMessage({ id: "reports.type.select" })}
          items={reportTypes}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={formData.reportType}
          onChange={({ selectedItem }) =>
            handleChange("reportType", selectedItem)
          }
          helperText={formData.reportType?.description}
        />

        {/* Export Format */}
        <Dropdown
          id="exportFormat"
          titleText={intl.formatMessage({ id: "reports.format" })}
          label={intl.formatMessage({ id: "reports.format.select" })}
          items={exportFormats}
          selectedItem={formData.exportFormat}
          onChange={({ selectedItem }) =>
            handleChange("exportFormat", selectedItem)
          }
        />

        {/* Date Range */}
        <div>
          <FormLabel>
            <FormattedMessage id="reports.dateRange" />
            {isDateRangeRequired && (
              <span style={{ color: "#da1e28" }}> *</span>
            )}
          </FormLabel>
          <DatePicker
            datePickerType="range"
            value={[formData.startDate, formData.endDate]}
            onChange={(dates) => {
              handleChange("startDate", dates[0] || null);
              handleChange("endDate", dates[1] || null);
            }}
          >
            <DatePickerInput
              id="startDate"
              placeholder="mm/dd/yyyy"
              labelText={intl.formatMessage({ id: "reports.startDate" })}
              size="md"
            />
            <DatePickerInput
              id="endDate"
              placeholder="mm/dd/yyyy"
              labelText={intl.formatMessage({ id: "reports.endDate" })}
              size="md"
            />
          </DatePicker>
        </div>

        {/* Filter Options */}
        <FormGroup legendText={intl.formatMessage({ id: "reports.options" })}>
          <Checkbox
            id="includeInactive"
            labelText={intl.formatMessage({ id: "reports.includeInactive" })}
            checked={formData.includeInactive}
            onChange={(e) => handleChange("includeInactive", e.target.checked)}
          />
          <Checkbox
            id="includeExpired"
            labelText={intl.formatMessage({ id: "reports.includeExpired" })}
            checked={formData.includeExpired}
            onChange={(e) => handleChange("includeExpired", e.target.checked)}
          />
        </FormGroup>

        {/* Grouping Options */}
        {["STOCK_LEVELS", "LOW_STOCK", "EXPIRATION_FORECAST"].includes(
          formData.reportType.id,
        ) && (
          <FormGroup
            legendText={intl.formatMessage({ id: "reports.grouping" })}
          >
            <Checkbox
              id="groupByType"
              labelText={intl.formatMessage({ id: "reports.groupByType" })}
              checked={formData.groupByType}
              onChange={(e) => handleChange("groupByType", e.target.checked)}
            />
            <Checkbox
              id="groupByLocation"
              labelText={intl.formatMessage({ id: "reports.groupByLocation" })}
              checked={formData.groupByLocation}
              onChange={(e) =>
                handleChange("groupByLocation", e.target.checked)
              }
            />
          </FormGroup>
        )}

        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "notification.error" })}
            subtitle={error}
            hideCloseButton={false}
            onCloseButtonClick={() => setError(null)}
            lowContrast
          />
        )}
      </Stack>
    </Modal>
  );
};

export default InventoryReportsModal;
