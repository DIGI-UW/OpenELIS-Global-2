import React from "react";
import { Button, Select, SelectItem, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { profileAuthoringMessage } from "./profileAuthoringMessages";
import ProfileStringList from "./ProfileStringList";

const ProfileTestDefinitions = ({ rows, onChange }) => {
  const intl = useIntl();
  // i18n-keys: analyzerType.editor.*
  const text = (key, values) => profileAuthoringMessage(intl, key, values);
  const update = (index, key, value) =>
    onChange(
      rows.map((row, current) => {
        if (current !== index) return row;
        const changed = { ...row };
        if (value === undefined) delete changed[key];
        else changed[key] = value;
        return changed;
      }),
    );
  return (
    <section aria-label={text("testDefinitions")}>
      <h4>{text("testDefinitions")}</h4>
      <p>{text("testDefinitionsHelp")}</p>
      {rows.map((row, index) => (
        <fieldset
          key={index}
          aria-label={text("testNumber", { number: index + 1 })}
          className="analyzer-type-modal__form"
        >
          <legend>{text("testNumber", { number: index + 1 })}</legend>
          {[
            ["test_code", "testCode"],
            ["test_name_hint", "testName"],
            ["loinc", "loinc"],
            ["unit", "testUnit"],
          ].map(([key, label]) => (
            <TextInput
              key={key}
              id={`profile-test-${index}-${key}`}
              labelText={text(label)}
              value={row[key] ?? ""}
              onChange={(event) =>
                update(
                  index,
                  key,
                  event.target.value === "" && key !== "unit"
                    ? undefined
                    : event.target.value,
                )
              }
            />
          ))}
          <Select
            id={`profile-test-${index}-result-type`}
            labelText={text("resultType")}
            aria-label={text("resultType")}
            value={row.result_type || ""}
            onChange={(event) =>
              update(index, "result_type", event.target.value || undefined)
            }
          >
            <SelectItem value="" text={text("choose")} />
            {["quantitative", "qualitative", "text"].map((value) => (
              <SelectItem
                key={value}
                value={value}
                text={text(`resultType.${value}`)}
              />
            ))}
          </Select>
          <ProfileStringList
            id={`test-${index}-aliases`}
            label={text("testAliases")}
            values={row.aliases || []}
            onChange={(values) =>
              update(index, "aliases", values.length ? values : undefined)
            }
          />
          {(row.result_type === "qualitative" || row.values) && (
            <ProfileStringList
              id={`test-${index}-values`}
              label={text("testValues")}
              values={row.values || []}
              onChange={(values) =>
                update(index, "values", values.length ? values : undefined)
              }
            />
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() =>
              onChange(rows.filter((_, current) => current !== index))
            }
          >
            {text("removeTest")}
          </Button>
        </fieldset>
      ))}
      <Button kind="tertiary" size="sm" onClick={() => onChange([...rows, {}])}>
        {text("addTest")}
      </Button>
    </section>
  );
};
export default ProfileTestDefinitions;
