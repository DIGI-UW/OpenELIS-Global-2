import React, { useEffect, useState } from "react";
import { Select, SelectItem, TextArea, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { profileAuthoringMessage } from "./profileAuthoringMessages";

const kindOf = (value) =>
  value === null ? "null" : Array.isArray(value) ? "array" : typeof value;
const asText = (value) =>
  typeof value === "string" ? value : JSON.stringify(value);
const emptyValue = {
  string: "",
  number: 0,
  boolean: false,
  array: [],
  object: {},
  null: null,
};
const parse = (raw, kind) => {
  if (kind === "string") return { valid: true, value: raw };
  try {
    const value = JSON.parse(raw);
    return {
      valid:
        kindOf(value) === kind && (kind !== "number" || Number.isFinite(value)),
      value,
    };
  } catch {
    return { valid: false };
  }
};

// JSON is only needed for structured choices; ordinary values use typed controls.
const ProfileValueEditor = ({
  id,
  label,
  value = "",
  onChange,
  onValidityChange,
}) => {
  const intl = useIntl();
  const text = (key) => profileAuthoringMessage(intl, key);
  const encoded = JSON.stringify(value);
  const [kind, setKind] = useState(kindOf(value));
  const [raw, setRaw] = useState(asText(value));
  useEffect(() => {
    const next = JSON.parse(encoded);
    setKind(kindOf(next));
    setRaw(asText(next));
  }, [encoded]);
  const valid = parse(raw, kind).valid;
  useEffect(() => {
    onValidityChange(id, valid);
  }, [id, valid, onValidityChange]);
  useEffect(() => () => onValidityChange(id, true), [id, onValidityChange]);
  const edit = (nextRaw) => {
    setRaw(nextRaw);
    const parsed = parse(nextRaw, kind);
    if (parsed.valid) onChange(parsed.value);
  };
  const props = {
    id: `${id}-value`,
    labelText: label,
    value: raw,
    invalid: !valid,
    invalidText: text("invalidTypedValue"),
    onChange: (event) => edit(event.target.value),
  };
  return (
    <>
      <Select
        id={`${id}-type`}
        labelText={text("valueType")}
        value={kind}
        onChange={(event) => {
          const nextKind = event.target.value;
          const next = emptyValue[nextKind];
          setKind(nextKind);
          setRaw(asText(next));
          onChange(next);
        }}
      >
        {Object.keys(emptyValue).map((type) => (
          <SelectItem
            key={type}
            value={type}
            text={text(`valueType.${type}`)}
          />
        ))}
      </Select>
      {kind === "boolean" ? (
        <Select
          id={`${id}-value`}
          labelText={label}
          value={raw}
          onChange={(event) => edit(event.target.value)}
        >
          <SelectItem value="true" text={text("yes")} />
          <SelectItem value="false" text={text("no")} />
        </Select>
      ) : kind === "null" ? (
        <p>{text("nullValue")}</p>
      ) : kind === "array" || kind === "object" ? (
        <TextArea {...props} helperText={text("structuredValueHelp")} />
      ) : (
        <TextInput {...props} />
      )}
    </>
  );
};
export default ProfileValueEditor;
