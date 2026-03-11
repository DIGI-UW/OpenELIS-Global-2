import React, { useState } from "react";
import { TextInput } from "@carbon/react";

export interface CustomTextInputProps {
  id?: string;
  defaultValue?: string;
  value?: string;
  onChange?: (value: string) => void;
  labelText?: React.ReactNode;
}

const CustomTextInput: React.FC<CustomTextInputProps> = ({
  id,
  defaultValue = "",
  value,
  onChange,
  labelText = "",
}) => {
  const [inputText, setInputText] = useState<string>(value ?? "");

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (onChange) {
      const val = e.target.value;
      setInputText(val);
      onChange(val);
    }
  };

  return (
    <TextInput
      id={id ?? "custom-text-input"}
      onChange={handleInputChange}
      labelText={labelText === "" ? "" : labelText}
      value={inputText === "" || inputText == null ? defaultValue : inputText}
    />
  );
};

export default CustomTextInput;
