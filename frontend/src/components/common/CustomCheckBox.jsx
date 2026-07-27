import React from "react";
import { Checkbox } from "@carbon/react";

const CustomCheckBox = (props) => {
  function handleCheckBox(e) {
    let isChecked;
    isChecked = !!e.currentTarget.checked;
    props.onChange(isChecked);
  }

  return (
    <>
      <Checkbox
        labelText={props.label}
        id={props.id}
        onChange={(e) => handleCheckBox(e)}
      />
    </>
  );
};

export default CustomCheckBox;
