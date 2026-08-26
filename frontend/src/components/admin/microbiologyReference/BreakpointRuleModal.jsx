import React, { useEffect, useMemo, useState } from "react";
import {
  Checkbox,
  ComposedModal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Select,
  SelectItem,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";

const EMPTY_RULE = {
  organismId: "",
  organismGroup: "",
  antibioticId: "",
  method: "MIC",
  specimenTypeId: "",
  breakpointType: "MIC",
  susceptibleValue: "",
  intermediateLowerValue: "",
  intermediateUpperValue: "",
  resistantValue: "",
  units: "",
  notes: "",
  active: true,
};

const numericOrNull = (value) =>
  value === "" || value === null || value === undefined ? null : Number(value);

const BreakpointRuleModal = ({
  value,
  organisms,
  organismGroups,
  antibiotics,
  specimenTypes,
  saving,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  const [draft, setDraft] = useState(EMPTY_RULE);

  useEffect(() => {
    setDraft(value ? { ...EMPTY_RULE, ...value } : EMPTY_RULE);
  }, [value]);

  const hasContext = Boolean(draft.organismId) !== Boolean(draft.organismGroup);
  const hasThreshold = useMemo(
    () =>
      [
        draft.susceptibleValue,
        draft.intermediateLowerValue,
        draft.intermediateUpperValue,
        draft.resistantValue,
      ].some((threshold) => threshold !== "" && threshold !== null),
    [draft],
  );
  const valid =
    hasContext &&
    Boolean(draft.antibioticId) &&
    Boolean(draft.method) &&
    Boolean(draft.breakpointType) &&
    hasThreshold;

  const update = (key, nextValue) =>
    setDraft((current) => ({ ...current, [key]: nextValue }));

  const save = () =>
    onSave({
      ...draft,
      susceptibleValue: numericOrNull(draft.susceptibleValue),
      intermediateLowerValue: numericOrNull(draft.intermediateLowerValue),
      intermediateUpperValue: numericOrNull(draft.intermediateUpperValue),
      resistantValue: numericOrNull(draft.resistantValue),
    });

  return (
    <ComposedModal open={Boolean(value)} size="lg" onClose={onClose}>
      <ModalHeader
        title={intl.formatMessage({
          id: value?.id
            ? "microbiology.admin.breakpoints.correction.edit"
            : "microbiology.admin.breakpoints.correction.add",
        })}
        closeModal={onClose}
      />
      <ModalBody>
        <div className="microbiology-admin__form-grid">
          <Select
            id="microbiology-breakpoint-rule-organism"
            labelText={intl.formatMessage({
              id: "microbiology.admin.breakpoints.organism",
            })}
            value={draft.organismId}
            onChange={(event) =>
              setDraft((current) => ({
                ...current,
                organismId: event.target.value,
                organismGroup: event.target.value ? "" : current.organismGroup,
              }))
            }
          >
            <SelectItem value="" text="" />
            {organisms.map((item) => (
              <SelectItem
                key={item.id}
                value={item.id}
                text={`${item.label}${item.code ? ` (${item.code})` : ""}`}
              />
            ))}
          </Select>
          <Select
            id="microbiology-breakpoint-rule-group"
            labelText={intl.formatMessage({
              id: "microbiology.admin.breakpoints.organismGroup",
            })}
            value={draft.organismGroup}
            onChange={(event) =>
              setDraft((current) => ({
                ...current,
                organismGroup: event.target.value,
                organismId: event.target.value ? "" : current.organismId,
              }))
            }
          >
            <SelectItem value="" text="" />
            {organismGroups.map((item) => (
              <SelectItem key={item.id} value={item.id} text={item.label} />
            ))}
          </Select>
          <Select
            id="microbiology-breakpoint-rule-antibiotic"
            labelText={intl.formatMessage({
              id: "microbiology.admin.astPanels.antibiotic",
            })}
            value={draft.antibioticId}
            onChange={(event) => update("antibioticId", event.target.value)}
          >
            <SelectItem value="" text="" />
            {antibiotics.map((item) => (
              <SelectItem
                key={item.id}
                value={item.id}
                text={`${item.label}${item.code ? ` (${item.code})` : ""}`}
              />
            ))}
          </Select>
          <Select
            id="microbiology-breakpoint-rule-specimen"
            labelText={intl.formatMessage({
              id: "microbiology.admin.breakpoints.specimen",
            })}
            value={draft.specimenTypeId}
            onChange={(event) => update("specimenTypeId", event.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "microbiology.admin.breakpoints.specimen.any",
              })}
            />
            {specimenTypes.map((item) => (
              <SelectItem key={item.id} value={item.id} text={item.label} />
            ))}
          </Select>
          <Select
            id="microbiology-breakpoint-rule-method"
            labelText={intl.formatMessage({
              id: "microbiology.admin.field.method",
            })}
            value={draft.method}
            onChange={(event) => update("method", event.target.value)}
          >
            <SelectItem value="MIC" text="MIC" />
            <SelectItem
              value="ZONE"
              text={intl.formatMessage({
                id: "microbiology.admin.breakpoints.method.zone",
              })}
            />
          </Select>
          <Select
            id="microbiology-breakpoint-rule-type"
            labelText={intl.formatMessage({
              id: "microbiology.admin.breakpoints.breakpointType",
            })}
            value={draft.breakpointType}
            onChange={(event) => update("breakpointType", event.target.value)}
          >
            <SelectItem value="MIC" text="MIC" />
            <SelectItem
              value="ZONE"
              text={intl.formatMessage({
                id: "microbiology.admin.breakpoints.method.zone",
              })}
            />
          </Select>
          {[
            ["susceptibleValue", "microbiology.admin.breakpoints.susceptible"],
            [
              "intermediateLowerValue",
              "microbiology.admin.breakpoints.intermediateLower",
            ],
            [
              "intermediateUpperValue",
              "microbiology.admin.breakpoints.intermediateUpper",
            ],
            ["resistantValue", "microbiology.admin.breakpoints.resistant"],
          ].map(([key, label]) => (
            <TextInput
              key={key}
              id={`microbiology-breakpoint-rule-${key}`}
              type="number"
              step="any"
              labelText={intl.formatMessage({ id: label })}
              value={draft[key] ?? ""}
              onChange={(event) => update(key, event.target.value)}
            />
          ))}
          <TextInput
            id="microbiology-breakpoint-rule-units"
            labelText={intl.formatMessage({
              id: "microbiology.admin.breakpoints.units",
            })}
            value={draft.units || ""}
            onChange={(event) => update("units", event.target.value)}
          />
          <TextArea
            id="microbiology-breakpoint-rule-notes"
            labelText={intl.formatMessage({
              id: "microbiology.admin.field.notes",
            })}
            value={draft.notes || ""}
            onChange={(event) => update("notes", event.target.value)}
          />
          <Checkbox
            id="microbiology-breakpoint-rule-active"
            labelText={intl.formatMessage({
              id: "microbiology.admin.field.active",
            })}
            checked={Boolean(draft.active)}
            onChange={(_, { checked }) => update("active", checked)}
          />
        </div>
      </ModalBody>
      <ModalFooter
        primaryButtonText={intl.formatMessage({ id: "button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={!valid || saving}
        onRequestSubmit={save}
        onRequestClose={onClose}
      />
    </ComposedModal>
  );
};

export default BreakpointRuleModal;
