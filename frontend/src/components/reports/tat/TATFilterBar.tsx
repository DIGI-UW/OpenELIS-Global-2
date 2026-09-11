import React, { useState, useEffect } from "react";
import {
  DatePicker,
  DatePickerInput,
  Dropdown,
  Checkbox,
  Button,
  ContentSwitcher,
  Switch,
  FilterableMultiSelect,
  ComboBox,
} from "@carbon/react";
import { Search, Reset } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import type {
  CalculationMode,
  DisplayListItem,
  Priority,
  SelectOption,
  TatFilters,
  TatSegment,
} from "./types";

export const SEGMENTS = [
  { id: "RECEIPT_TO_VALIDATION", labelKey: "reports.tat.segment.receiptToValidation" },
  { id: "ORDER_TO_COLLECTION", labelKey: "reports.tat.segment.orderToCollection" },
  { id: "COLLECTION_TO_RECEIPT", labelKey: "reports.tat.segment.collectionToReceipt" },
  { id: "RECEIPT_TO_TESTING", labelKey: "reports.tat.segment.receiptToTesting" },
  { id: "RECEIPT_TO_RESULT", labelKey: "reports.tat.segment.receiptToResult" },
  { id: "RESULT_TO_VALIDATION", labelKey: "reports.tat.segment.resultToValidation" },
  { id: "OVERALL", labelKey: "reports.tat.segment.overall" },
] as const;

interface DatePreset {
  labelKey: string;
  compute: () => { from: Date; to: Date };
}

const DATE_PRESETS: DatePreset[] = [
  { labelKey: "reports.tat.preset.today", compute: () => ({ from: new Date(), to: new Date() }) },
  { labelKey: "reports.tat.preset.7days", compute: () => { const to = new Date(); const from = new Date(); from.setDate(from.getDate() - 7); return { from, to }; } },
  { labelKey: "reports.tat.preset.30days", compute: () => { const to = new Date(); const from = new Date(); from.setDate(from.getDate() - 30); return { from, to }; } },
  { labelKey: "reports.tat.preset.90days", compute: () => { const to = new Date(); const from = new Date(); from.setDate(from.getDate() - 90); return { from, to }; } },
  { labelKey: "reports.tat.preset.thisMonth", compute: () => {
    const now = new Date();
    return { from: new Date(now.getFullYear(), now.getMonth(), 1), to: now };
  }},
  { labelKey: "reports.tat.preset.lastMonth", compute: () => {
    const now = new Date();
    return {
      from: new Date(now.getFullYear(), now.getMonth() - 1, 1),
      to: new Date(now.getFullYear(), now.getMonth(), 0),
    };
  }},
  { labelKey: "reports.tat.preset.thisQuarter", compute: () => {
    const now = new Date();
    const q = Math.floor(now.getMonth() / 3);
    return { from: new Date(now.getFullYear(), q * 3, 1), to: now };
  }},
];

function formatDate(d: Date): string {
  return d.toISOString().split("T")[0];
}

function getDefaultDates(): Pick<TatFilters, "fromDate" | "toDate"> {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return {
    fromDate: from.toISOString().split("T")[0],
    toDate: to.toISOString().split("T")[0],
  };
}

interface TATFilterBarProps {
  onGenerate: (filters: TatFilters) => void;
}

function TATFilterBar({ onGenerate }: TATFilterBarProps) {
  const intl = useIntl();
  const defaults = getDefaultDates();

  const [fromDate, setFromDate] = useState(defaults.fromDate);
  const [toDate, setToDate] = useState(defaults.toDate);
  const [segment, setSegment] = useState<TatSegment>("RECEIPT_TO_VALIDATION");
  const [calculationMode, setCalculationMode] = useState<CalculationMode>("CALENDAR");
  const [priority, setPriority] = useState<Priority>("");
  const [includeCancelled, setIncludeCancelled] = useState(false);

  const [labUnitIds, setLabUnitIds] = useState<Array<string | undefined>>([]);
  const [testIds, setTestIds] = useState<Array<string | undefined>>([]);
  const [sampleTypeId, setSampleTypeId] = useState("");
  const [orderingSiteId, setOrderingSiteId] = useState("");

  const [labUnitOptions, setLabUnitOptions] = useState<SelectOption[]>([]);
  const [testOptions, setTestOptions] = useState<SelectOption[]>([]);
  const [sampleTypeOptions, setSampleTypeOptions] = useState<SelectOption[]>([]);
  const [siteOptions, setSiteOptions] = useState<SelectOption[]>([]);

  useEffect(() => {
    getFromOpenElisServer("/rest/displayList/TEST_SECTION_ACTIVE", (res: DisplayListItem[] | null) => {
      if (res)
        setLabUnitOptions(
          res.map((item) => ({ id: item.id, text: item.value })),
        );
    });
    getFromOpenElisServer("/rest/displayList/ALL_TESTS", (res: DisplayListItem[] | null) => {
      if (res)
        setTestOptions(
          res.map((item) => ({ id: item.id, text: item.value })),
        );
    });
    getFromOpenElisServer("/rest/user-sample-types", (res: DisplayListItem[] | null) => {
      if (res)
        setSampleTypeOptions(
          res.map((item) => ({ id: item.id, text: item.value })),
        );
    });
    getFromOpenElisServer("/rest/displayList/SAMPLE_PATIENT_REFERRING_CLINIC", (res: DisplayListItem[] | null) => {
      if (res)
        setSiteOptions(
          res.map((item) => ({
            id: item.id || item.value,
            text: item.value,
          })),
        );
    });
  }, []);

  const handleGenerate = () => {
    onGenerate({
      fromDate,
      toDate,
      segment,
      calculationMode,
      priority,
      includeCancelled,
      labUnitIds,
      testIds,
      sampleTypeId,
      orderingSiteId,
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
    setLabUnitIds([]);
    setTestIds([]);
    setSampleTypeId("");
    setOrderingSiteId("");
  };

  return (
    <div
      style={{
        padding: "1rem",
        border: "1px solid var(--cds-border-subtle)",
        borderRadius: "4px",
        marginBottom: "1rem",
      }}
    >
      <div
        style={{
          display: "flex",
          gap: "0.25rem",
          marginBottom: "0.5rem",
          flexWrap: "wrap",
        }}
      >
        {DATE_PRESETS.map((p) => (
          <Button
            key={p.labelKey}
            kind="ghost"
            size="sm"
            onClick={() => {
              const { from, to } = p.compute();
              setFromDate(formatDate(from));
              setToDate(formatDate(to));
            }}
          >
            {intl.formatMessage({ id: p.labelKey })}
          </Button>
        ))}
      </div>

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
          dateFormat="Y-m-d"
          value={fromDate}
          onChange={(_dates, dateStr) => setFromDate(dateStr)}
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
          dateFormat="Y-m-d"
          value={toDate}
          onChange={(_dates, dateStr) => setToDate(dateStr)}
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
          itemToString={(item) => item?.text || ""}
          selectedItem={{
            id: segment,
            text: intl.formatMessage({
              id: SEGMENTS.find((s) => s.id === segment)?.labelKey,
            }),
          }}
          onChange={({ selectedItem }) =>
            setSegment((selectedItem as SelectOption).id as TatSegment)
          }
        />
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(4, 1fr)",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        <FilterableMultiSelect
          id="tat-lab-unit"
          titleText={intl.formatMessage({ id: "reports.tat.labUnit" })}
          items={labUnitOptions}
          itemToString={(item) => item?.text || ""}
          onChange={({ selectedItems }) =>
            setLabUnitIds(selectedItems.map((i: SelectOption) => i.id))
          }
          size="sm"
        />

        <FilterableMultiSelect
          id="tat-test"
          titleText={intl.formatMessage({ id: "reports.tat.testPanel" })}
          items={testOptions}
          itemToString={(item) => item?.text || ""}
          onChange={({ selectedItems }) =>
            setTestIds(selectedItems.map((i: SelectOption) => i.id))
          }
          size="sm"
        />

        <Dropdown
          id="tat-sample-type"
          titleText={intl.formatMessage({ id: "reports.tat.sampleType" })}
          items={[
            {
              id: "",
              text: intl.formatMessage({ id: "reports.tat.all" }),
            },
            ...sampleTypeOptions,
          ]}
          itemToString={(item) => item?.text || ""}
          selectedItem={{
            id: sampleTypeId,
            text:
              sampleTypeOptions.find((o) => o.id === sampleTypeId)?.text ||
              intl.formatMessage({ id: "reports.tat.all" }),
          }}
          onChange={({ selectedItem }) =>
            setSampleTypeId(selectedItem?.id || "")
          }
          size="sm"
        />

        <ComboBox
          id="tat-ordering-site"
          titleText={intl.formatMessage({ id: "reports.tat.orderingSite" })}
          items={siteOptions}
          itemToString={(item) => item?.text || ""}
          onChange={({ selectedItem }) =>
            setOrderingSiteId(selectedItem?.id || "")
          }
          size="sm"
          placeholder={intl.formatMessage({ id: "reports.tat.all" })}
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
        <div style={{ minWidth: "260px" }}>
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
            onChange={({ name }) => setCalculationMode(name as CalculationMode)}
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
          itemToString={(item) => item?.text || ""}
          selectedItem={{
            id: priority,
            text: priority
              ? intl.formatMessage({ id: `reports.tat.priority.${priority.toLowerCase()}` })
              : intl.formatMessage({ id: "reports.tat.priority.all" }),
          }}
          onChange={({ selectedItem }) =>
            setPriority((selectedItem as SelectOption).id as Priority)
          }
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
