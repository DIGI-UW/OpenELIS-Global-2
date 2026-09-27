import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import AnalyzerConnectionFields, {
  initializeConnectionValues,
  isConnectionFieldVisible,
  serializeConnectionValues,
} from "./AnalyzerConnectionFields";

const choices = [
  { value: "  literal  ", labelKey: "test.choice.whitespace" },
  { value: "1", labelKey: "test.choice.text" },
  { value: 1, labelKey: "test.choice.number" },
  { value: false, labelKey: "test.choice.boolean" },
  { value: "false", labelKey: "test.choice.booleanText" },
  { value: [1, "2"], labelKey: "test.choice.array" },
  {
    value: { mode: "different", enabled: true },
    labelKey: "test.choice.otherObject",
  },
  {
    value: { mode: "synthetic", enabled: true },
    labelKey: "test.choice.object",
  },
];
const labels = {
  "test.choice.whitespace": "Exact text",
  "test.setting": "Synthetic setting",
  "test.choice.text": "Text one",
  "test.choice.number": "Numeric one",
  "test.choice.boolean": "Boolean false",
  "test.choice.booleanText": "Text false",
  "test.choice.array": "Array choice",
  "test.choice.otherObject": "Other object choice",
  "test.choice.object": "Object choice",
};
const field = {
  key: "syntheticSetting",
  labelKey: "test.setting",
  inputKind: "SELECT",
  required: true,
  choices,
};

function SetupFields({ fields, onSave }) {
  const [values, setValues] = useState(() =>
    initializeConnectionValues(fields),
  );
  return (
    <>
      <AnalyzerConnectionFields
        fields={fields}
        values={values}
        changedSecrets={new Set()}
        submitAttempted={false}
        onChange={(changedField, value) =>
          setValues((previous) => ({ ...previous, [changedField.key]: value }))
        }
      />
      <button onClick={() => onSave(serializeConnectionValues(fields, values))}>
        Save settings
      </button>
    </>
  );
}

const renderFields = (fields, onSave = vi.fn()) =>
  render(
    <IntlProvider locale="en" messages={{ ...messages, ...labels }}>
      <SetupFields fields={fields} onSave={onSave} />
    </IntlProvider>,
  );

describe("profile-defined connection choices", () => {
  it.each(choices)(
    "preserves the JSON value for $labelKey when saving",
    async (choice) => {
      const onSave = vi.fn();
      renderFields([field], onSave);
      await userEvent.selectOptions(
        screen.getByLabelText("Synthetic setting"),
        screen.getByRole("option", { name: labels[choice.labelKey] }),
      );
      await userEvent.click(
        screen.getByRole("button", { name: "Save settings" }),
      );
      expect(onSave).toHaveBeenCalledWith({ syntheticSetting: choice.value });
    },
  );

  it("reopens a saved object choice by value regardless of object key order", () => {
    renderFields([
      { ...field, currentValue: { enabled: true, mode: "synthetic" } },
    ]);
    expect(screen.getByRole("option", { name: "Object choice" }).selected).toBe(
      true,
    );
  });

  it("does not display the first choice as selected when the saved value is absent from the profile", () => {
    renderFields([{ ...field, currentValue: "removed choice" }]);
    expect(screen.getByRole("option", { name: "Exact text" }).selected).toBe(
      false,
    );
    expect(screen.getByLabelText("Synthetic setting")).toHaveDisplayValue(
      "Select a value",
    );
  });

  it("reveals and saves dependent fields only for the matching typed choice", async () => {
    const onSave = vi.fn();
    renderFields(
      [
        field,
        {
          key: "dependentValue",
          labelKey: "analyzer.connection.field.host",
          inputKind: "TEXT",
          required: true,
          currentValue: "synthetic.invalid",
          visibleWhen: { fieldKey: field.key, operator: "EQUALS", value: 1 },
        },
      ],
      onSave,
    );
    await userEvent.selectOptions(
      screen.getByLabelText("Synthetic setting"),
      "Numeric one",
    );
    expect(screen.getByLabelText("Host")).toHaveValue("synthetic.invalid");
    await userEvent.click(
      screen.getByRole("button", { name: "Save settings" }),
    );
    expect(onSave).toHaveBeenLastCalledWith({
      syntheticSetting: 1,
      dependentValue: "synthetic.invalid",
    });
    await userEvent.selectOptions(
      screen.getByLabelText("Synthetic setting"),
      "Text one",
    );
    expect(screen.queryByLabelText("Host")).not.toBeInTheDocument();
    await userEvent.click(
      screen.getByRole("button", { name: "Save settings" }),
    );
    expect(onSave).toHaveBeenLastCalledWith({ syntheticSetting: "1" });
  });

  it.each([
    ["NOT_IN", "invalid non-array", 1, false],
    [
      "EQUALS",
      { enabled: true, mode: "synthetic" },
      { mode: "synthetic", enabled: true },
      true,
    ],
    [
      "NOT_EQUALS",
      { enabled: true, mode: "synthetic" },
      { mode: "synthetic", enabled: true },
      false,
    ],
    [
      "IN",
      [{ enabled: true, mode: "synthetic" }],
      { mode: "synthetic", enabled: true },
      true,
    ],
    [
      "NOT_IN",
      [{ enabled: true, mode: "synthetic" }],
      { mode: "synthetic", enabled: true },
      false,
    ],
    ["EQUALS", "1", 1, false],
    ["IN", ["1"], 1, false],
  ])(
    "matches %s visibility using JSON values (%j)",
    (operator, expected, actual, visible) => {
      expect(
        isConnectionFieldVisible(
          {
            key: "dependent",
            visibleWhen: {
              fieldKey: "syntheticSetting",
              operator,
              value: expected,
            },
          },
          { syntheticSetting: actual },
        ),
      ).toBe(visible);
    },
  );
});
