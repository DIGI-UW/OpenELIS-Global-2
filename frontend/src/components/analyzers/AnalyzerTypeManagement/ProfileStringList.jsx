import React from "react";
import { Button, Select, SelectItem, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { profileAuthoringMessage } from "./profileAuthoringMessages";

const ProfileStringList = ({ id, label, values, choices, onChange }) => {
  const intl = useIntl();
  const text = (key) => profileAuthoringMessage(intl, key);
  return (
    <fieldset aria-label={label}>
      <legend>{label}</legend>
      {values.map((value, index) => (
        <div key={index}>
          {choices ? (
            <Select
              id={`profile-list-${id}-${index}`}
              labelText={`${label} ${index + 1}`}
              value={value}
              onChange={(event) =>
                onChange(
                  values.map((item, current) =>
                    current === index ? event.target.value : item,
                  ),
                )
              }
            >
              <SelectItem value="" text={text("choose")} />
              {choices.map((choice) => (
                <SelectItem
                  key={choice}
                  value={choice}
                  text={text(`semantic.${choice}`)}
                />
              ))}
            </Select>
          ) : (
            <TextInput
              id={`profile-list-${id}-${index}`}
              labelText={`${label} ${index + 1}`}
              value={value}
              onChange={(event) =>
                onChange(
                  values.map((item, current) =>
                    current === index ? event.target.value : item,
                  ),
                )
              }
            />
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() =>
              onChange(values.filter((_, current) => current !== index))
            }
          >
            {text("remove")}
          </Button>
        </div>
      ))}
      <Button
        kind="tertiary"
        size="sm"
        onClick={() => onChange([...values, ""])}
      >
        {text("add")}
      </Button>
    </fieldset>
  );
};

export default ProfileStringList;
