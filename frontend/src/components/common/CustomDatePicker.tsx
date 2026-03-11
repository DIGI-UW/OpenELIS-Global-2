import React, { useEffect, useState, useContext } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";

export interface CustomDatePickerProps {
  id?: string;
  value?: string;
  onChange: (value: string) => void;
  className?: string;
  disallowFutureDate?: boolean;
  disallowPastDate?: boolean;
  labelText?: React.ReactNode;
  invalid?: boolean;
  invalidText?: React.ReactNode;
  disabled?: boolean;
  updateStateValue?: boolean;
}

const CustomDatePicker: React.FC<CustomDatePickerProps> = ({
  id,
  value = "",
  onChange,
  className,
  disallowFutureDate,
  disallowPastDate,
  labelText,
  invalid,
  invalidText,
  disabled,
  updateStateValue,
}) => {
  const [currentDate, setCurrentDate] = useState<string>(value);
  const { configurationProperties } = useContext(ConfigurationContext) as any;

  const isFrenchLocale =
    configurationProperties?.DEFAULT_DATE_LOCALE === "fr-FR";

  const handleDatePickerChange = (e: Date[]) => {
    if (!e || e.length === 0) return;
    const date = e[0];
    const formatDate = format(
      date,
      isFrenchLocale ? "dd/MM/yyyy" : "MM/dd/yyyy",
    );
    setCurrentDate(formatDate);
    onChange(formatDate);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputValue = e.target.value;
    const partialDateRegex = /^(\d{0,2})(\/(\d{0,2})(\/(\d{0,4})?)?)?$/;

    if (partialDateRegex.test(inputValue)) {
      // allow
    } else {
      e.target.value = ""; // Clear invalid input
    }
  };

  useEffect(() => {
    onChange(currentDate);
  }, [currentDate, onChange]);

  useEffect(() => {
    if (updateStateValue) {
      setCurrentDate(value);
    }
  }, [value, updateStateValue]);

  const dateFormat = isFrenchLocale ? "d/m/Y" : "m/d/Y";
  const placeHolderFormat = isFrenchLocale ? "dd/mm/yyyy" : "mm/dd/yyyy";
  const limitDateString = format(
    new Date(),
    isFrenchLocale ? "dd/MM/yyyy" : "MM/dd/yyyy",
  );

  return (
    <DatePicker
      className={className}
      datePickerType="single"
      dateFormat={dateFormat}
      value={currentDate}
      onChange={handleDatePickerChange}
      maxDate={disallowFutureDate ? limitDateString : ""}
      minDate={disallowPastDate ? limitDateString : ""}
    >
      <DatePickerInput
        id={id ?? "custom-datepicker"}
        placeholder={placeHolderFormat}
        type="text"
        labelText={labelText ?? ""}
        invalid={invalid}
        invalidText={invalidText}
        disabled={disabled}
        onChange={handleInputChange}
      />
    </DatePicker>
  );
};

export default CustomDatePicker;
