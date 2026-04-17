import React from "react";
import { Checkbox } from "@carbon/react";

export interface CustomCheckBoxProps {
  id?: string;
  label?: React.ReactNode;
  onChange: (isChecked: boolean) => void;
}

const CustomCheckBox: React.FC<CustomCheckBoxProps> = ({
  id,
  label,
  onChange,
}) => {
  const handleCheckBox = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange(!!e.currentTarget.checked);
  };

  return (
    <Checkbox
      id={id ?? "custom-checkbox"}
      labelText={label}
      onChange={handleCheckBox}
    />
  );
};

export default CustomCheckBox;
