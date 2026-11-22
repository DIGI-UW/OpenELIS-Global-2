import React from "react";
import { Checkbox } from "@carbon/react";

const CustomCheckBox = (props) => {
  function handleCheckBox(e) {
    let isChecked;
    isChecked = !!e.currentTarget.checked;
    props.onChange(isChecked);
  }

  // A caller that owns the value passes `checked`, which keeps the box in step
  // with that state across re-renders. Without it the DOM input is the only
  // record of the choice, so any re-render can show a cleared box while the
  // caller still holds the value.
  const controlled = props.checked !== undefined;

  return (
    <>
      <Checkbox
        labelText={props.label}
        id={props.id}
        onChange={(e) => handleCheckBox(e)}
        {...(controlled ? { checked: props.checked } : {})}
      />
    </>
  );
};

export default CustomCheckBox;
