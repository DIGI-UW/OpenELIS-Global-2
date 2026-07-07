import React, { useState } from "react";
import {
  Modal,
  Select,
  SelectItem,
  TextInput,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { DAYS_PER, toDays } from "./rangeUtils";

/**
 * OGC-949 M7 / OGC-970 — add/edit a single reference range.
 *
 * Age is entered as a value + unit (days/months/years) and converted to the
 * day-based unit the backend stores. Presentational: the parent owns the ranges
 * list and the persisting PUT; this only emits a normalized range on save.
 *
 * Rendered only while open (Carbon Modal keeps its footer buttons in the DOM
 * even when closed, which would collide with sibling modals' buttons).
 */
const numOrEmpty = (v) => (v === null || v === undefined ? "" : String(v));

const parseOrNull = (s) => {
  if (s === "" || s === null || s === undefined) {
    return null;
  }
  const n = parseFloat(s);
  return Number.isNaN(n) ? null : n;
};

// Pick a lossless display unit for an existing range: whole years if both bounds
// divide evenly, otherwise raw days (months round-trips are lossy).
const displayAge = (range) => {
  const min = range.minAge || 0;
  const max = range.maxAge;
  const cleanYears = (d) => d != null && d >= 365 && d % 365 === 0;
  const useYears = cleanYears(min) && (max == null || cleanYears(max));
  if (useYears) {
    return {
      ageUnit: "years",
      minAgeValue: String(min / 365),
      maxAgeValue: max == null ? "" : String(max / 365),
    };
  }
  return {
    ageUnit: "days",
    minAgeValue: String(min),
    maxAgeValue: max == null ? "" : String(max),
  };
};

const RangeModal = ({ range, components = [], onSave, onCancel }) => {
  const intl = useIntl();
  const editing = !!(range && range.id);

  // Default a range to the test's only component so a single-component test's
  // ranges are never saved unassociated (FR-19).
  const defaultComponentId = components.length === 1 ? components[0].id : "";

  const [draft, setDraft] = useState(() => {
    if (!range) {
      return {
        id: undefined,
        componentId: defaultComponentId,
        gender: "",
        ageUnit: "years",
        minAgeValue: "0",
        maxAgeValue: "",
        lowNormal: "",
        highNormal: "",
        lowCritical: "",
        highCritical: "",
      };
    }
    return {
      id: range.id,
      // Preserve the existing association; fall back to the sole component.
      componentId: range.componentId || defaultComponentId,
      gender: range.gender || "",
      ...displayAge(range),
      lowNormal: numOrEmpty(range.lowNormal),
      highNormal: numOrEmpty(range.highNormal),
      lowCritical: numOrEmpty(range.lowCritical),
      highCritical: numOrEmpty(range.highCritical),
    };
  });
  const [ageError, setAgeError] = useState(false);

  const set = (patch) => setDraft((prev) => ({ ...prev, ...patch }));

  const handleSubmit = () => {
    const minDays =
      toDays(
        draft.minAgeValue === "" ? "0" : draft.minAgeValue,
        draft.ageUnit,
      ) || 0;
    const maxDays =
      draft.maxAgeValue === ""
        ? null
        : toDays(draft.maxAgeValue, draft.ageUnit);
    if (maxDays !== null && maxDays <= minDays) {
      setAgeError(true);
      return;
    }
    onSave({
      id: draft.id,
      componentId: draft.componentId || null,
      gender: draft.gender || null,
      minAge: minDays,
      maxAge: maxDays,
      lowNormal: parseOrNull(draft.lowNormal),
      highNormal: parseOrNull(draft.highNormal),
      lowCritical: parseOrNull(draft.lowCritical),
      highCritical: parseOrNull(draft.highCritical),
    });
  };

  const numField = (key, labelId) => (
    <TextInput
      id={`range-${key}`}
      type="number"
      labelText={intl.formatMessage({ id: labelId })}
      value={draft[key]}
      onChange={(e) => set({ [key]: e.target.value })}
    />
  );

  return (
    <Modal
      open
      modalHeading={intl.formatMessage({
        id: editing
          ? "label.testCatalog.ranges.modal.titleEdit"
          : "label.testCatalog.ranges.modal.titleAdd",
      })}
      primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      onRequestSubmit={handleSubmit}
      onRequestClose={onCancel}
      onSecondarySubmit={onCancel}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
        {components.length > 1 && (
          <Select
            id="range-component"
            labelText={intl.formatMessage({
              id: "label.testCatalog.ranges.modal.component",
            })}
            value={draft.componentId || ""}
            onChange={(e) => set({ componentId: e.target.value })}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "label.testCatalog.ranges.modal.component.all",
              })}
            />
            {components.map((c) => (
              <SelectItem key={c.id} value={c.id} text={c.label} />
            ))}
          </Select>
        )}

        <Select
          id="range-sex"
          labelText={intl.formatMessage({
            id: "label.testCatalog.ranges.modal.sex",
          })}
          value={draft.gender}
          onChange={(e) => set({ gender: e.target.value })}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({ id: "label.testCatalog.ranges.both" })}
          />
          <SelectItem
            value="M"
            text={intl.formatMessage({ id: "label.testCatalog.ranges.male" })}
          />
          <SelectItem
            value="F"
            text={intl.formatMessage({ id: "label.testCatalog.ranges.female" })}
          />
        </Select>

        <Select
          id="range-age-unit"
          labelText={intl.formatMessage({
            id: "label.testCatalog.ranges.modal.ageUnit",
          })}
          value={draft.ageUnit}
          onChange={(e) => set({ ageUnit: e.target.value })}
        >
          {Object.keys(DAYS_PER).map((u) => (
            <SelectItem
              key={u}
              value={u}
              text={intl.formatMessage({ id: `label.testCatalog.ranges.${u}` })}
            />
          ))}
        </Select>

        <div style={{ display: "flex", gap: "1rem" }}>
          {numField("minAgeValue", "label.testCatalog.ranges.modal.minAge")}
          {numField("maxAgeValue", "label.testCatalog.ranges.modal.maxAge")}
        </div>
        {ageError && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "label.testCatalog.ranges.modal.ageError",
            })}
          />
        )}

        <div style={{ display: "flex", gap: "1rem" }}>
          {numField("lowNormal", "label.testCatalog.ranges.modal.lowNormal")}
          {numField("highNormal", "label.testCatalog.ranges.modal.highNormal")}
        </div>
        <div style={{ display: "flex", gap: "1rem" }}>
          {numField(
            "lowCritical",
            "label.testCatalog.ranges.modal.lowCritical",
          )}
          {numField(
            "highCritical",
            "label.testCatalog.ranges.modal.highCritical",
          )}
        </div>
      </div>
    </Modal>
  );
};

export default RangeModal;
