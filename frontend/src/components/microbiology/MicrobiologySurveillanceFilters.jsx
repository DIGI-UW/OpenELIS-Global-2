import React, { useEffect, useMemo, useState } from "react";
import {
  DatePicker,
  DatePickerInput,
  FilterableMultiSelect,
  MultiSelect,
  Select,
  SelectItem,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getWhonetDatePreset, getWhonetDateRange } from "./WhonetRoutes";
import "./MicrobiologySurveillanceFilters.scss";

const EMPTY_OPTIONS = {
  specimenTypes: [],
  organisms: [],
  patientOrigins: [],
  significance: [],
};

const optionsWithSelections = (items, selectedIds) => {
  const known = new Set(items.map((item) => item.id));
  return [
    ...items,
    ...selectedIds
      .filter((id) => !known.has(id))
      .map((id) => ({ id, label: id })),
  ];
};

const selectedOptions = (items, selectedIds) => {
  const byId = new Map(items.map((item) => [item.id, item]));
  return selectedIds.map((id) => byId.get(id) || { id, label: id });
};

const MicrobiologySurveillanceFilters = ({
  state,
  filterOptions = EMPTY_OPTIONS,
  onChange,
  now,
  disabled = false,
  idPrefix = "microbiology-surveillance",
}) => {
  const intl = useIntl();
  const referenceNow = useMemo(() => now || new Date(), [now]);
  const inferredPeriod = getWhonetDatePreset(state, referenceNow);
  const [period, setPeriod] = useState(inferredPeriod);

  useEffect(() => setPeriod(inferredPeriod), [inferredPeriod]);

  const significanceItems = (filterOptions.significance || []).map((item) => ({
    ...item,
    label: intl.formatMessage({
      id: `microbiology.whonet.significance.${item.id}`,
      defaultMessage: item.label,
    }),
  }));
  const invalidPeriod = state.to < state.from;

  const updatePeriod = (value) => {
    setPeriod(value);
    if (value !== "CUSTOM") {
      onChange(getWhonetDateRange(value, referenceNow));
    }
  };

  return (
    <div className="microbiology-surveillance-filters">
      <Select
        id={`${idPrefix}-period`}
        labelText={intl.formatMessage({
          id: "microbiology.whonet.period.preset",
        })}
        value={period}
        disabled={disabled}
        onChange={(event) => updatePeriod(event.target.value)}
      >
        <SelectItem
          value="THIS_MONTH"
          text={intl.formatMessage({
            id: "microbiology.whonet.period.thisMonth",
          })}
        />
        <SelectItem
          value="LAST_MONTH"
          text={intl.formatMessage({
            id: "microbiology.whonet.period.lastMonth",
          })}
        />
        <SelectItem
          value="THIS_QUARTER"
          text={intl.formatMessage({
            id: "microbiology.whonet.period.thisQuarter",
          })}
        />
        <SelectItem
          value="CUSTOM"
          text={intl.formatMessage({
            id: "microbiology.whonet.period.custom",
          })}
        />
      </Select>
      <DatePicker
        datePickerType="single"
        dateFormat="Y-m-d"
        value={state.from}
        onChange={(_dates, dateString) =>
          dateString && onChange({ from: dateString })
        }
      >
        <DatePickerInput
          id={`${idPrefix}-from`}
          labelText={intl.formatMessage({ id: "microbiology.whonet.from" })}
          placeholder={intl.formatMessage({
            id: "microbiology.whonet.date.placeholder",
          })}
          invalid={invalidPeriod}
          invalidText={intl.formatMessage({
            id: "microbiology.whonet.period.invalid",
          })}
          disabled={disabled}
        />
      </DatePicker>
      <DatePicker
        datePickerType="single"
        dateFormat="Y-m-d"
        value={state.to}
        onChange={(_dates, dateString) =>
          dateString && onChange({ to: dateString })
        }
      >
        <DatePickerInput
          id={`${idPrefix}-to`}
          labelText={intl.formatMessage({ id: "microbiology.whonet.to" })}
          placeholder={intl.formatMessage({
            id: "microbiology.whonet.date.placeholder",
          })}
          invalid={invalidPeriod}
          invalidText={intl.formatMessage({
            id: "microbiology.whonet.period.invalid",
          })}
          disabled={disabled}
        />
      </DatePicker>
      {[
        ["specimen", "specimenTypes", "microbiology.whonet.filter.specimen"],
        ["organism", "organisms", "microbiology.whonet.filter.organism"],
        ["origin", "patientOrigins", "microbiology.whonet.filter.origin"],
      ].map(([key, optionKey, labelId]) => {
        const available = optionsWithSelections(
          filterOptions[optionKey] || [],
          state[key],
        );
        return (
          <FilterableMultiSelect
            key={key}
            id={`${idPrefix}-${key}`}
            titleText={intl.formatMessage({ id: labelId })}
            label={intl.formatMessage({
              id: "microbiology.whonet.filter.any",
            })}
            items={available}
            selectedItems={selectedOptions(available, state[key])}
            itemToString={(item) => item?.label || ""}
            disabled={disabled}
            selectionFeedback="top-after-reopen"
            onChange={({ selectedItems: selection }) =>
              onChange({ [key]: selection.map((item) => item.id) })
            }
          />
        );
      })}
      {(() => {
        const available = optionsWithSelections(
          significanceItems,
          state.significance,
        );
        return (
          <MultiSelect
            id={`${idPrefix}-significance`}
            titleText={intl.formatMessage({
              id: "microbiology.whonet.significance",
            })}
            label={intl.formatMessage({
              id: "microbiology.whonet.filter.choose",
            })}
            items={available}
            selectedItems={selectedOptions(available, state.significance)}
            itemToString={(item) => item?.label || ""}
            disabled={disabled}
            selectionFeedback="top-after-reopen"
            onChange={({ selectedItems: selection }) =>
              onChange({
                significance: selection.map((item) => item.id),
              })
            }
          />
        );
      })()}
    </div>
  );
};

export default MicrobiologySurveillanceFilters;
