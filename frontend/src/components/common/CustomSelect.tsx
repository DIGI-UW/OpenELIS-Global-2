import React from "react";
import { Select, SelectItem } from "@carbon/react";

export interface SelectOption {
  id: string | number;
  value: string;
}

export interface CustomSelectProps {
  id?: string;
  labelText?: React.ReactNode;
  value?: string | number;
  defaultValue?: string | number;
  disabled?: boolean;
  placeholder?: string;
  defaultSelect?: SelectOption;
  options?: SelectOption[] | null;
  onChange: (value: string) => void;
}

const CustomSelect: React.FC<CustomSelectProps> = ({
  id,
  labelText = "",
  value,
  defaultValue = "",
  disabled = false,
  placeholder = "Select...",
  defaultSelect,
  options,
  onChange,
}) => {
  const handleSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onChange(e.target.value);
  };

  const selectValue = value !== undefined && value !== null ? value : "";

  return (
    <Select
      id={id ?? "custom-select"}
      onChange={handleSelect}
      labelText={labelText}
      defaultValue={selectValue !== "" ? selectValue : defaultValue}
      value={selectValue}
      disabled={disabled}
    >
      <SelectItem text={placeholder} value="" />
      {defaultSelect && (
        <SelectItem text={defaultSelect.value} value={defaultSelect.id} />
      )}
      {options != null &&
        options.map((option, index) => (
          <SelectItem key={index} text={option.value} value={option.id} />
        ))}
    </Select>
  );
};

export default CustomSelect;
