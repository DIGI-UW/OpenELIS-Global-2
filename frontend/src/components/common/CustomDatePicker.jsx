import React, { useEffect, useState, useContext } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";

const CustomDatePicker = (props) => {
  const [currentDate, setCurrentDate] = useState(
    props.value ? props.value : "",
  );
  const [isLocalInvalid, setIsLocalInvalid] = useState(false);
  const [localInvalidText, setLocalInvalidText] = useState("");

  const { configurationProperties } = useContext(ConfigurationContext);

  function handleDatePickerChange(e) {
    if (!e) return;

    if (!e[0]) {
      setCurrentDate("");
      props.onChange("");
      setIsLocalInvalid(false);
      return;
    }

    let date = new Date(e[0]);
    if (isNaN(date.getTime())) return;

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const selectedDate = new Date(date);
    selectedDate.setHours(0, 0, 0, 0);

    if (props.disallowFutureDate && selectedDate > today) {
      setIsLocalInvalid(true);
      setLocalInvalidText("Future dates are not allowed");
      setCurrentDate("");
      props.onChange("");
      return;
    }

    if (props.disallowPastDate && selectedDate < today) {
      setIsLocalInvalid(true);
      setLocalInvalidText("Past dates are not allowed");
      setCurrentDate("");
      props.onChange("");
      return;
    }

    setIsLocalInvalid(false);
    setLocalInvalidText("");

    const formatDate = format(
      new Date(date),
      configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
        ? "dd/MM/yyyy"
        : "MM/dd/yyyy",
    );
    setCurrentDate(formatDate);
    props.onChange(formatDate);
  }

  function handleInputChange(e) {
    const inputValue = e.target.value;
    const isFrenchLocale =
      configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
    const partialDateRegex = /^(\d{0,2})(\/(\d{0,2})(\/(\d{0,4})?)?)?$/;

    if (partialDateRegex.test(inputValue)) {
      e.target.value = inputValue;
    } else {
      e.target.value = "";
    }
  }

  useEffect(() => {
    props.onChange(currentDate);
  }, [currentDate]);

  useEffect(() => {
    if (props.updateStateValue) {
      setCurrentDate(props.value);
    }
  }, [props.value]);

  return (
    <>
      <DatePicker
        id={props.id}
        dateFormat={
          configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
            ? "d/m/Y"
            : "m/d/Y"
        }
        className={props.className}
        datePickerType="single"
        value={currentDate}
        onChange={(e) => handleDatePickerChange(e)}
      >
        <DatePickerInput
          id={props.id}
          placeholder={
            configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
              ? "dd/mm/yyyy"
              : "mm/dd/yyyy"
          }
          type="text"
          labelText={props.labelText}
          invalid={props.invalid || isLocalInvalid}
          invalidText={props.invalidText || localInvalidText}
          disabled={props.disabled}
          onChange={handleInputChange}
        />
      </DatePicker>
    </>
  );
};

export default CustomDatePicker;
