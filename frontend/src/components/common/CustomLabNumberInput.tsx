import React, { useState, useContext, useEffect } from "react";
import { TextInput } from "@carbon/react";
import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";
import { ConfigurationContext } from "../layout/Layout";

export interface CustomLabNumberInputProps {
  id?: string;
  name?: string;
  value?: string;
  labelText?: React.ReactNode;
  onChange?: (
    e: React.ChangeEvent<HTMLInputElement> | any,
    formattedValue?: string,
  ) => void;
  [key: string]: any; // Allow any additional props typical for TextInput
}

const CustomLabNumberInput: React.FC<CustomLabNumberInputProps> = ({
  id,
  name,
  value = "",
  labelText,
  onChange,
  ...rest
}) => {
  const { configurationProperties } = useContext(ConfigurationContext) as any;
  const [formattedInput, setFormattedInput] = useState<string>("");

  useEffect(() => {
    setDisplayValue();
  }, [value]);

  const setDisplayValue = () => {
    if (
      configurationProperties?.AccessionFormat === "ALPHANUM" &&
      value.length < 13
    ) {
      const formatted = convertAlphaNumLabNumForDisplay(value);
      setFormattedInput(formatted);
    } else {
      setFormattedInput(value);
    }
  };

  if (configurationProperties?.AccessionFormat !== "ALPHANUM") {
    return (
      <TextInput
        id={id ?? "custom-lab-number"}
        name={name}
        value={value}
        labelText={labelText ?? ""}
        onChange={onChange}
        {...rest}
      />
    );
  }

  return (
    <>
      <input type="hidden" value={value} name={name} id={id} />
      <TextInput
        id={`display_${id}`}
        name={`display_${name}`}
        value={formattedInput}
        labelText={labelText ?? ""}
        enableCounter
        maxCount={23}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
          let val = e.target.value;
          for (
            let numDashes = (val.match(/-/g) || []).length;
            numDashes > 1;
            --numDashes
          ) {
            val = val.replace("-", "");
          }
          let vals = val.split("-");
          if (vals.length > 1) {
            if (vals[1].length > 2 || vals[0].length <= 7) {
              vals = [vals[0] + vals[1]];
            } else {
              vals = [vals[0] + "-" + vals[1]];
            }
          }
          if (onChange) {
            onChange(e, vals[0]);
          }
        }}
        {...rest}
      />
    </>
  );
};

export default CustomLabNumberInput;
