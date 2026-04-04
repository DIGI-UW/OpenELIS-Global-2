import React, { useState } from "react";
import {
  DatePicker,
  DatePickerInput,
  Dropdown,
  Checkbox,
  Button,
  ContentSwitcher,
  Switch,
} from "@carbon/react";
import { Search, Reset } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";

const SEGMENTS = [
  { id: "RECEIPT_TO_VALIDATION", labelKey: "reports.tat.segment.receiptToValidation" },
  { id: "ORDER_TO_COLLECTION", labelKey: "reports.tat.segment.orderToCollection" },
  { id: "COLLECTION_TO_RECEIPT", labelKey: "reports.tat.segment.collectionToReceipt" },
  { id: "RECEIPT_TO_TESTING", labelKey: "reports.tat.segment.receiptToTesting" },
  { id: "RECEIPT_TO_RESULT", labelKey: "reports.tat.segment.receiptToResult" },
  { id: "RESULT_TO_VALIDATION", labelKey: "reports.tat.segment.resultToValidation" },
  { id: "OVERALL", labelKey: "reports.tat.segment.overall" },
];

function getDefaultDates() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return {
    fromDate: from.toISOString().split("T")[0],
    toDate: to.toISOString().split("T")[0],
  };
}

function TATFilterBar({ onGenerate }) {
  const intl = useIntl();
  const defaults = getDefaultDates();

  const [fromDate, setFromDate] = useState(defaults.fromDate);
  const [toDate, setToDate] = useState(defaults.toDate);
  const [segment, setSegment] = useState("RECEIPT_TO_VALIDATION");
  const [calculationMode, setCalculationMode] = useState("CALENDAR");
  const [priority, setPriority] = useState("");
  const [includeCancelled, setIncludeCancelled] = useState(false);

  const handleGenerate = () => {
    onGenerate({
      fromDate,
      toDate,
      segment,
      calculationMode,
      priority,
      includeCancelled,
    });
  };

  const handleClear = () => {
    const d = getDefaultDates();
    setFromDate(d.fromDate);
    setToDate(d.toDate);
    setSegment("RECEIPT_TO_VALIDATION");
    setCalculationMode("CALENDAR");
    setPriority("");
    setIncludeCancelled(false);
  };

  return (
    <div
      style={{
        padding: "1rem",
        border: "1px solid #e0e0e0",
        borderRadius: "4px",
        marginBottom: "1rem",
      }}
    >
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(3, 1fr)",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        <DatePicker
          datePickerType="single"
          value={fromDate}
          onChange={([date]) =>
            date && setFromDate(date.toISOString().split("T")[0])
          }
        >
          <DatePickerInput
            id="tat-from-date"
            labelText={intl.formatMessage({ id: "reports.tat.dateRangeFrom" })}
            placeholder="yyyy-mm-dd"
            size="sm"
          />
        </DatePicker>

        <DatePicker
          datePickerType="single"
          value={toDate}
          onChange={([date]) =>
            date && setToDate(date.toISOString().split("T")[0])
          }
        >
          <DatePickerInput
            id="tat-to-date"
            labelText={intl.formatMessage({ id: "reports.tat.dateRangeTo" })}
            placeholder="yyyy-mm-dd"
            size="sm"
          />
        </DatePicker>

        <Dropdown
          id="tat-segment"
          titleText={intl.formatMessage({ id: "reports.tat.tatSegment" })}
          items={SEGMENTS.map((s) => ({
            id: s.id,
            text: intl.formatMessage({ id: s.labelKey }),
          }))}
          selectedItem={{
            id: segment,
            text: intl.formatMessage({
              id: SEGMENTS.find((s) => s.id === segment)?.labelKey,
            }),
          }}
          onChange={({ selectedItem }) => setSegment(selectedItem.id)}
        />
      </div>

      <div
        style={{
          display: "flex",
          gap: "1rem",
          alignItems: "flex-end",
          flexWrap: "wrap",
        }}
      >
        <div>
          <label
            style={{
              fontSize: "12px",
              fontWeight: 600,
              display: "block",
              marginBottom: "0.25rem",
            }}
          >
            <FormattedMessage id="reports.tat.calendarTime" /> /{" "}
            <FormattedMessage id="reports.tat.workingTime" />
          </label>
          <ContentSwitcher
            onChange={({ name }) => setCalculationMode(name)}
            selectedIndex={calculationMode === "CALENDAR" ? 0 : 1}
            size="sm"
          >
            <Switch name="CALENDAR" text={intl.formatMessage({ id: "reports.tat.calendarTime" })} />
            <Switch name="WORKING_TIME" text={intl.formatMessage({ id: "reports.tat.workingTime" })} />
          </ContentSwitcher>
        </div>

        <Dropdown
          id="tat-priority"
          titleText={intl.formatMessage({ id: "reports.tat.priority" })}
          size="sm"
          items={[
            { id: "", text: intl.formatMessage({ id: "reports.tat.priority.all" }) },
            { id: "ROUTINE", text: intl.formatMessage({ id: "reports.tat.priority.routine" }) },
            { id: "STAT", text: intl.formatMessage({ id: "reports.tat.priority.stat" }) },
            { id: "ASAP", text: intl.formatMessage({ id: "reports.tat.priority.asap" }) },
          ]}
          selectedItem={{
            id: priority,
            text: priority
              ? intl.formatMessage({ id: `reports.tat.priority.${priority.toLowerCase()}` })
              : intl.formatMessage({ id: "reports.tat.priority.all" }),
          }}
          onChange={({ selectedItem }) => setPriority(selectedItem.id)}
        />

        <Checkbox
          id="tat-include-cancelled"
          labelText={intl.formatMessage({
            id: "reports.tat.includeCancelled",
          })}
          checked={includeCancelled}
          onChange={(_, { checked }) => setIncludeCancelled(checked)}
        />

        <div style={{ marginLeft: "auto", display: "flex", gap: "0.5rem" }}>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Reset}
            onClick={handleClear}
          >
            <FormattedMessage id="reports.tat.clearFilters" />
          </Button>
          <Button
            renderIcon={Search}
            onClick={handleGenerate}
            data-testid="generate-report-button"
          >
            <FormattedMessage id="reports.tat.generateReport" />
          </Button>
        </div>
      </div>
    </div>
  );
}

export default TATFilterBar;
