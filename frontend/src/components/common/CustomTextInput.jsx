import React, { useState } from "react";
import { TextInput } from "@carbon/react";

const CustomTextInput = (props) => {
  const { id, defaultValue, onChange, labelText } = props;
  const [inputText, setInputText] = useState(props.value ? props.value : "");

  const handleInputChange = (e) => {
    if (onChange != null) {
      let value = e.target.value;
      setInputText(value);
      onChange(value);
    }
  };
  return (
    <>
      <TextInput
        id={id}
        onChange={handleInputChange}
        labelText={labelText === "" ? "" : labelText}
        value={inputText === "" ? defaultValue : inputText}
      />
    </>
  );
};
export default CustomTextInput;
