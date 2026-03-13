import React, { useEffect, useState, useContext } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format, parse, isValid } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";

interface CustomDatePickerProps {
  id: string;
  value?: string;
  className?: string;
  labelText?: string;
  invalid?: boolean;
  invalidText?: string;
  disabled?: boolean;
  disallowFutureDate?: boolean;
  disallowPastDate?: boolean;
  updateStateValue?: boolean;
  onChange: (date: string) => void;
}

const CustomDatePicker: React.FC<CustomDatePickerProps> = (props) => {
  const [currentDate, setCurrentDate] = useState<string>(
    props.value ? props.value : "",
  );
  const [inputValue, setInputValue] = useState<string>(
    props.value ? props.value : "",
  );

  const { configurationProperties } = useContext(ConfigurationContext) as {
    configurationProperties: {
      DEFAULT_DATE_LOCALE: string;
    };
  };

  const isFrenchLocale =
    configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
  const dateFormat = isFrenchLocale ? "dd/MM/yyyy" : "MM/dd/yyyy";
  const displayFormat = isFrenchLocale ? "dd/mm/yyyy" : "mm/dd/yyyy";
  const datePickerFormat = isFrenchLocale ? "d/m/Y" : "m/d/Y";

  const parseDateString = (dateString: string): Date | null => {
    if (!dateString) return null;

    const parsedDate = parse(dateString, dateFormat, new Date());

    if (isValid(parsedDate)) {
      return parsedDate;
    }

    // Alternative common formats
    const alternativeFormats = isFrenchLocale
      ? ["yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd"]
      : ["yyyy-MM-dd", "dd/MM/yyyy", "MM-dd-yyyy", "yyyy/MM/dd"];

    for (const format of alternativeFormats) {
      const alternativeParse = parse(dateString, format, new Date());
      if (isValid(alternativeParse)) {
        return alternativeParse;
      }
    }

    return null;
  };

  const formatDateToString = (date: Date): string => {
    return format(date, dateFormat);
  };

  function handleDatePickerChange(e: Date[]) {
    if (e[0] && isValid(e[0])) {
      const formattedDate = formatDateToString(e[0]);
      setCurrentDate(formattedDate);
      setInputValue(formattedDate);
      props.onChange(formattedDate);
    }
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const rawValue = e.target.value;
    setInputValue(rawValue);
  }

  function handleInputBlur() {
    if (!inputValue) {
      setCurrentDate("");
      props.onChange("");
      return;
    }

    const parsedDate = parseDateString(inputValue);

    if (parsedDate && isValid(parsedDate)) {
      if (props.disallowFutureDate) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (parsedDate > today) {
          if (currentDate) {
            setInputValue(currentDate);
          } else {
            setInputValue("");
          }
          return;
        }
      }

      // Check disallowPastDate
      if (props.disallowPastDate) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (parsedDate < today) {
          if (currentDate) {
            setInputValue(currentDate);
          } else {
            setInputValue("");
          }
          return;
        }
      }

      const formattedDate = formatDateToString(parsedDate);
      setCurrentDate(formattedDate);
      setInputValue(formattedDate);
      props.onChange(formattedDate);
    } else {
      if (currentDate) {
        setInputValue(currentDate);
      } else {
        setInputValue("");
      }
    }
  }

  useEffect(() => {
    props.onChange(currentDate);
  }, [currentDate]);

  useEffect(() => {
    if (props.updateStateValue) {
      const newValue = props.value ?? "";
      setCurrentDate(newValue);
      setInputValue(newValue);
    }
  }, [props.value]);

  const getMinMaxDate = (): { minDate?: string; maxDate?: string } => {
    const today = format(new Date(), dateFormat);

    return {
      ...(props.disallowPastDate ? { minDate: today } : {}),
      ...(props.disallowFutureDate ? { maxDate: today } : {}),
    };
  };

  const { minDate, maxDate } = getMinMaxDate();

  return (
    <>
      <DatePicker
        id={props.id}
        dateFormat={datePickerFormat}
        className={props.className}
        datePickerType="single"
        value={currentDate}
        onChange={(e: Date[]) => handleDatePickerChange(e)}
        minDate={minDate}
        maxDate={maxDate}
      >
        <DatePickerInput
          id={props.id}
          placeholder={displayFormat}
          type="text"
          labelText={props.labelText}
          invalid={props.invalid}
          invalidText={props.invalidText}
          disabled={props.disabled}
          value={inputValue}
          onChange={handleInputChange}
          onBlur={handleInputBlur}
        />
      </DatePicker>
    </>
  );
};

export default CustomDatePicker;
