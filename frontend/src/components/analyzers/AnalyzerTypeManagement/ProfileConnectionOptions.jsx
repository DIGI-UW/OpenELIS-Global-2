import React from "react";
import { Button, Select, SelectItem, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { profileAuthoringMessage } from "./profileAuthoringMessages";
import ProfileValueEditor from "./ProfileValueEditor";

const ProfileConnectionOptions = ({
  field,
  index,
  fields,
  onChange,
  onValidityChange,
}) => {
  const intl = useIntl();
  const text = (key, values) => profileAuthoringMessage(intl, key, values);
  const choices = field.choices || [];
  const condition = field.visibleWhen;
  const updateChoice = (position, changes) =>
    onChange(
      "choices",
      choices.map((choice, current) =>
        current === position ? { ...choice, ...changes } : choice,
      ),
    );
  const updateCondition = (changes) =>
    onChange("visibleWhen", { ...condition, ...changes });
  return (
    <>
      {(field.inputKind === "SELECT" || choices.length > 0) && (
        <fieldset aria-label={text("choices")}>
          <legend>{text("choices")}</legend>
          {choices.map((choice, position) => (
            <div
              key={position}
              role="group"
              aria-label={text("choiceNumber", { number: position + 1 })}
            >
              <TextInput
                id={`profile-choice-${index}-${position}-label`}
                labelText={text("fieldLabel")}
                value={choice.labelKey || ""}
                onChange={(event) =>
                  updateChoice(position, { labelKey: event.target.value })
                }
              />
              <ProfileValueEditor
                id={`profile-choice-${index}-${position}`}
                label={text("choiceValue")}
                value={choice.value}
                onChange={(value) => updateChoice(position, { value })}
                onValidityChange={onValidityChange}
              />
              <Button
                kind="ghost"
                size="sm"
                onClick={() =>
                  onChange(
                    "choices",
                    choices.filter((_, current) => current !== position),
                  )
                }
              >
                {text("removeChoice")}
              </Button>
            </div>
          ))}
          <Button
            kind="tertiary"
            size="sm"
            onClick={() =>
              onChange("choices", [...choices, { value: "", labelKey: "" }])
            }
          >
            {text("addChoice")}
          </Button>
        </fieldset>
      )}
      {condition ? (
        <fieldset aria-label={text("visibility")}>
          <legend>{text("visibility")}</legend>
          <Select
            id={`profile-condition-${index}-field`}
            labelText={text("controllingField")}
            value={condition.fieldKey || ""}
            onChange={(event) =>
              updateCondition({ fieldKey: event.target.value })
            }
          >
            <SelectItem value="" text={text("choose")} />
            {fields
              .filter(
                (candidate, current) => current !== index && candidate.key,
              )
              .map((candidate, current) => (
                <SelectItem
                  key={current}
                  value={candidate.key}
                  text={candidate.key}
                />
              ))}
          </Select>
          <Select
            id={`profile-condition-${index}-operator`}
            labelText={text("comparison")}
            value={condition.operator || ""}
            onChange={(event) =>
              updateCondition({ operator: event.target.value })
            }
          >
            <SelectItem value="" text={text("choose")} />
            {["EQUALS", "NOT_EQUALS", "IN", "NOT_IN"].map((operator) => (
              <SelectItem
                key={operator}
                value={operator}
                text={text(`comparison.${operator}`)}
              />
            ))}
          </Select>
          <ProfileValueEditor
            id={`profile-condition-${index}`}
            label={text("comparisonValue")}
            value={condition.value}
            onChange={(value) => updateCondition({ value })}
            onValidityChange={onValidityChange}
          />
          <Button
            kind="ghost"
            size="sm"
            onClick={() => onChange("visibleWhen", undefined)}
          >
            {text("removeCondition")}
          </Button>
        </fieldset>
      ) : (
        <Button
          kind="tertiary"
          size="sm"
          onClick={() =>
            onChange("visibleWhen", { fieldKey: "", operator: "", value: "" })
          }
        >
          {text("addCondition")}
        </Button>
      )}
    </>
  );
};
export default ProfileConnectionOptions;
