import React, { useState, useContext, useEffect } from "react";
import { TextInput } from "@carbon/react";
import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";
import { ConfigurationContext } from "../layout/Layout";

const CustomLabNumberInput = (props) => {
  const { configurationProperties } = useContext(ConfigurationContext);

  const [formattedInput, setFormattedInput] = useState("");

  useEffect(() => {
    setDisplayValue();
  }, [props.value]);

  const setDisplayValue = () => {
    if (
      configurationProperties.AccessionFormat === "ALPHANUM" &&
      props.value?.length < 13
    ) {
      const formatted = convertAlphaNumLabNumForDisplay(props.value); // use your own format function here
      setFormattedInput(formatted);
    } else {
      setFormattedInput(props.value);
    }
  };

  if (configurationProperties.AccessionFormat !== "ALPHANUM") {
    return (
      <>
        <TextInput {...props} />
      </>
    );
  } else {
    return (
      <>
        <input
          type="hidden"
          value={props.value ? props.value : ""}
          name={props.name}
          id={props.id}
        />
        <TextInput
          {...props}
          onChange={(e) => {
            let val = e.target.value;
            for (
              let numDashes = (e.target.value.match(/-/g) || []).length;
              numDashes > 1;
              --numDashes
            ) {
              val = val.replace("-", "");
            }
            let vals = val.split("-");
            if (vals.length > 1) {
              //combine values after dashes unless a full accession number exists before the dash
              //this will fail if 100+ tests are run on a sample
              if (vals[1].length > 2 || vals[0].length <= 7) {
                vals = [vals[0] + vals[1]];
              } else {
                vals = [vals[0] + "-" + vals[1]];
              }
            }
            props.onChange(e, vals[0]);
          }}
          labelText={props.labelText}
          id={"display_" + props.id}
          value={formattedInput}
          enableCounter
          maxCount={23}
          name={"display_" + props.name}
        />
      </>
    );
  }
};
export default CustomLabNumberInput;
