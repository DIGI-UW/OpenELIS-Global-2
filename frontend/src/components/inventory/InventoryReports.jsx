import React, { useEffect, useState } from "react";
import {
  Form,
  Stack,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Checkbox,
  FormGroup,
  FormLabel,
  FilterableMultiSelect,
  Button,
  InlineNotification,
  Tile,
  Grid,
  Column,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  SkeletonText,
} from "@carbon/react";
import { DocumentPdf, DocumentBlank, TableSplit } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { ReportsAPI, InventoryTagAPI } from "./InventoryService";
import { toIsoDate } from "./dates";

// Which date control each report needs, and whether it can run without one.
// "range" is a period, "asOf" a single instant, and EXPIRING's range is a window
// of expiry dates rather than a period of events.
export const DATE_MODE = {
  RECEIVED: { control: "range", required: true },
  CONSUMED: { control: "range", required: true },
  STOCK_ON_HAND: { control: "asOf", required: false },
  EXPIRING: { control: "range", required: false },
};

// Exported because it carries a rule the form itself cannot show: a report that
// asks for a single instant must not send the period start left behind by the
// report the user was looking at a moment ago.
export const toRequestParams = (formData, dateMode) => ({
  reportType: formData.reportType.id,
  exportFormat: formData.exportFormat.id,
  startDate: dateMode.control === "asOf" ? null : toIsoDate(formData.startDate),
  endDate: toIsoDate(formData.endDate),
  includeInactive: formData.includeInactive,
  includeExpired: formData.includeExpired,
  tags: formData.tags,
});

/**
 * Stock at a past date is replayed from the transaction log rather than counted,
 * so a result carrying that date needs saying so — including when it is empty,
 * where a bare zero would claim the lab held nothing.
 */
export const isReconstructedStock = (reportType, endDate) =>
  reportType === "STOCK_ON_HAND" && !!endDate;

const InventoryReports = () => {
  const intl = useIntl();

  const reportTypes = [
    {
      id: "RECEIVED",
      text: intl.formatMessage({ id: "reports.type.received" }),
      description: intl.formatMessage({
        id: "reports.type.received.description",
      }),
    },
    {
      id: "CONSUMED",
      text: intl.formatMessage({ id: "reports.type.consumed" }),
      description: intl.formatMessage({
        id: "reports.type.consumed.description",
      }),
    },
    {
      id: "STOCK_ON_HAND",
      text: intl.formatMessage({ id: "reports.type.stockOnHand" }),
      description: intl.formatMessage({
        id: "reports.type.stockOnHand.description",
      }),
    },
    {
      id: "EXPIRING",
      text: intl.formatMessage({ id: "reports.type.expiring" }),
      description: intl.formatMessage({
        id: "reports.type.expiring.description",
      }),
    },
  ];

  // Export formats
  const exportFormats = [
    { id: "CSV", text: "CSV", icon: DocumentBlank },
    { id: "EXCEL", text: "Excel (.xlsx)", icon: TableSplit },
    { id: "PDF", text: "PDF", icon: DocumentPdf },
  ];

  // Form state
  const [formData, setFormData] = useState({
    reportType: reportTypes[0],
    exportFormat: exportFormats[0],
    startDate: null,
    endDate: null,
    includeInactive: false,
    includeExpired: true,
    tags: [],
  });

  const [tagOptions, setTagOptions] = useState([]);
  const [running, setRunning] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [table, setTable] = useState(null);

  // The directory rather than the board's rows: this screen fetches no items of
  // its own, so it has nothing to derive a tag list from.
  useEffect(() => {
    let cancelled = false;
    InventoryTagAPI.getDirectory()
      .then((directory) => {
        if (cancelled) return;
        setTagOptions(
          (directory || [])
            .filter((entry) => entry.active !== false)
            .map((entry) => entry.name),
        );
      })
      .catch(() => {
        // A missing tag list costs the filter, never the report.
        if (!cancelled) setTagOptions([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const dateMode = DATE_MODE[formData.reportType.id];

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
    setSuccess(null);
    // A result belongs to the filters that produced it.
    setTable(null);
  };

  const validate = () => {
    if (
      dateMode.required &&
      dateMode.control === "range" &&
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

  const currentParams = () => toRequestParams(formData, dateMode);

  const handleRun = async () => {
    if (!validate()) return;

    setRunning(true);
    setError(null);
    setSuccess(null);
    try {
      setTable(await ReportsAPI.preview(currentParams()));
    } catch (err) {
      console.error("Error running report:", err);
      setTable(null);
      setError(intl.formatMessage({ id: "reports.error.generationFailed" }));
    } finally {
      setRunning(false);
    }
  };

  const handleExport = async () => {
    if (!validate()) return;

    setExporting(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await ReportsAPI.generate(currentParams());

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

      setSuccess(intl.formatMessage({ id: "reports.generation.success" }));
    } catch (err) {
      console.error("Error generating report:", err);
      setError(intl.formatMessage({ id: "reports.error.generationFailed" }));
    } finally {
      setExporting(false);
    }
  };

  const busy = running || exporting;

  return (
    <div style={{ marginTop: "2rem" }}>
      <Grid fullWidth={false}>
        <Column lg={10} md={6} sm={4}>
          <Stack gap={7}>
            <InlineNotification
              kind="info"
              title={intl.formatMessage({ id: "reports.info.title" })}
              subtitle={intl.formatMessage({ id: "reports.info.message" })}
              hideCloseButton
              lowContrast
            />

            <Form>
              <Stack gap={6}>
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

                {dateMode.control === "range" && (
                  <div>
                    <FormLabel>
                      <FormattedMessage id="reports.dateRange" />
                      {dateMode.required && (
                        <span style={{ color: "#da1e28" }}> *</span>
                      )}
                    </FormLabel>
                    <DatePicker
                      datePickerType="range"
                      dateFormat="Y-m-d"
                      value={[formData.startDate, formData.endDate]}
                      onChange={(dates) => {
                        handleChange("startDate", dates[0] || null);
                        handleChange("endDate", dates[1] || null);
                      }}
                    >
                      <DatePickerInput
                        id="startDate"
                        placeholder="yyyy-mm-dd"
                        labelText={intl.formatMessage({
                          id:
                            formData.reportType.id === "EXPIRING"
                              ? "reports.expiringFrom"
                              : "reports.startDate",
                        })}
                        size="md"
                      />
                      <DatePickerInput
                        id="endDate"
                        placeholder="yyyy-mm-dd"
                        labelText={intl.formatMessage({
                          id:
                            formData.reportType.id === "EXPIRING"
                              ? "reports.expiringTo"
                              : "reports.endDate",
                        })}
                        size="md"
                      />
                    </DatePicker>
                  </div>
                )}

                {dateMode.control === "asOf" && (
                  <DatePicker
                    datePickerType="single"
                    dateFormat="Y-m-d"
                    value={formData.endDate ? [formData.endDate] : []}
                    onChange={(dates) =>
                      handleChange("endDate", dates[0] || null)
                    }
                  >
                    <DatePickerInput
                      id="endDate"
                      placeholder="yyyy-mm-dd"
                      labelText={intl.formatMessage({ id: "reports.asOfDate" })}
                      helperText={intl.formatMessage({
                        id: "reports.asOfDate.help",
                      })}
                      size="md"
                    />
                  </DatePicker>
                )}

                <div>
                  <FilterableMultiSelect
                    id="reportTagFilter"
                    titleText={intl.formatMessage({ id: "reports.tags" })}
                    helperText={intl.formatMessage({ id: "reports.tags.help" })}
                    placeholder={intl.formatMessage({
                      id: "reports.tags.placeholder",
                    })}
                    items={tagOptions}
                    itemToString={(item) => item || ""}
                    selectedItems={formData.tags}
                    onChange={({ selectedItems }) =>
                      handleChange("tags", selectedItems || [])
                    }
                  />
                  {formData.tags.length > 0 && (
                    <div style={{ marginTop: "0.5rem" }}>
                      {formData.tags.map((tag) => (
                        <Tag
                          key={tag}
                          type="cool-gray"
                          filter
                          onClose={() =>
                            handleChange(
                              "tags",
                              formData.tags.filter((t) => t !== tag),
                            )
                          }
                          title={intl.formatMessage(
                            { id: "inventory.item.tags.remove" },
                            { tag },
                          )}
                        >
                          {tag}
                        </Tag>
                      ))}
                    </div>
                  )}
                </div>

                {["STOCK_ON_HAND", "EXPIRING"].includes(
                  formData.reportType.id,
                ) && (
                  <FormGroup
                    legendText={intl.formatMessage({ id: "reports.options" })}
                  >
                    <Checkbox
                      id="includeInactive"
                      labelText={intl.formatMessage({
                        id: "reports.includeInactive",
                      })}
                      checked={formData.includeInactive}
                      onChange={(e) =>
                        handleChange("includeInactive", e.target.checked)
                      }
                    />
                    {formData.reportType.id === "EXPIRING" && (
                      <Checkbox
                        id="includeExpired"
                        labelText={intl.formatMessage({
                          id: "reports.includeExpired",
                        })}
                        checked={formData.includeExpired}
                        onChange={(e) =>
                          handleChange("includeExpired", e.target.checked)
                        }
                      />
                    )}
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

                {success && (
                  <InlineNotification
                    kind="success"
                    title={intl.formatMessage({ id: "notification.success" })}
                    subtitle={success}
                    hideCloseButton={false}
                    onCloseButtonClick={() => setSuccess(null)}
                    lowContrast
                  />
                )}

                <Stack gap={4} orientation="horizontal">
                  <Button
                    onClick={handleRun}
                    disabled={busy}
                    kind="primary"
                    size="lg"
                  >
                    {running ? (
                      <FormattedMessage id="reports.running" />
                    ) : (
                      <FormattedMessage id="reports.run" />
                    )}
                  </Button>
                  <Dropdown
                    id="exportFormat"
                    titleText={intl.formatMessage({ id: "reports.format" })}
                    label={intl.formatMessage({ id: "reports.format.select" })}
                    items={exportFormats}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={formData.exportFormat}
                    onChange={({ selectedItem }) =>
                      handleChange("exportFormat", selectedItem)
                    }
                    size="lg"
                  />
                  <Button
                    onClick={handleExport}
                    disabled={busy}
                    kind="tertiary"
                    size="lg"
                  >
                    {exporting ? (
                      <FormattedMessage id="reports.generating" />
                    ) : (
                      <FormattedMessage id="reports.export" />
                    )}
                  </Button>
                </Stack>
              </Stack>
            </Form>

            {running && <SkeletonText paragraph lineCount={4} />}

            {!running && table && (
              <div>
                <h4 style={{ marginBottom: "0.75rem" }}>{table.title}</h4>
                {isReconstructedStock(
                  formData.reportType.id,
                  formData.endDate,
                ) && (
                  <p style={{ marginBottom: "0.75rem" }} className="cds--label">
                    <FormattedMessage id="reports.asOfDate.basis" />
                  </p>
                )}
                {table.rows?.length ? (
                  <div style={{ overflowX: "auto" }}>
                    <Table size="sm" useZebraStyles>
                      <TableHead>
                        <TableRow>
                          {table.headers.map((header) => (
                            <TableHeader key={header}>{header}</TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {table.rows.map((row, rowIndex) => (
                          <TableRow key={rowIndex}>
                            {row.map((cell, cellIndex) => (
                              <TableCell key={cellIndex}>{cell}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                ) : (
                  <p>
                    <FormattedMessage
                      id={
                        isReconstructedStock(
                          formData.reportType.id,
                          formData.endDate,
                        )
                          ? "reports.empty.stockOnHandAsOf"
                          : "reports.empty"
                      }
                    />
                  </p>
                )}
              </div>
            )}
          </Stack>
        </Column>

        {/* Help sidebar */}
        <Column lg={6} md={2} sm={4}>
          <Tile style={{ marginTop: "3rem" }}>
            <h4 style={{ marginBottom: "1rem" }}>
              <FormattedMessage id="reports.help.title" />
            </h4>
            <Stack gap={4}>
              <p>
                <FormattedMessage id="reports.help.description" />
              </p>
              <ul style={{ marginLeft: "1.5rem" }}>
                <li>
                  <FormattedMessage id="reports.help.tip1" />
                </li>
                <li>
                  <FormattedMessage id="reports.help.tip2" />
                </li>
                <li>
                  <FormattedMessage id="reports.help.tip3" />
                </li>
              </ul>
            </Stack>
          </Tile>
        </Column>
      </Grid>
    </div>
  );
};

export default InventoryReports;
