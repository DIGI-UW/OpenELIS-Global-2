import React, { useEffect, useState, useContext } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";

const CustomDatePicker = (props) => {
  const [currentDate, setCurrentDate] = useState(
    props.value ? props.value : "",
  );
  const { configurationProperties } = useContext(ConfigurationContext);
  function handleDatePickerChange(e) {
    const raw = e?.[0];
    if (!raw || isNaN(new Date(raw).getTime())) {
      setCurrentDate("");
      props.onChange("");
      return;
    }
    const formatDate = format(
      new Date(raw),
      configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
        ? "dd/MM/yyyy"
        : "MM/dd/yyyy",
    );
    setCurrentDate(formatDate);
    props.onChange(formatDate);
  }

  function handleInputChange(e) {
    const inputValue = e.target.value;

    // Empty input must clear state and propagate to the parent. The partial
    // regex below accepts the empty string (all groups are zero-or-more), so
    // without this branch a manual clear silently leaves the prior value in
    // place.
    if (inputValue === "") {
      setCurrentDate("");
      return;
    }

    const isFrenchLocale =
      configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
    const partialDateRegex = isFrenchLocale
      ? /^(\d{0,2})(\/(\d{0,2})(\/(\d{0,4})?)?)?$/
      : /^(\d{0,2})(\/(\d{0,2})(\/(\d{0,4})?)?)?$/;

    const fullDateRegex = isFrenchLocale
      ? /^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[0-2])\/\d{4}$/
      : /^(0[1-9]|1[0-2])\/(0[1-9]|[12][0-9]|3[01])\/\d{4}$/;

    if (!partialDateRegex.test(inputValue)) {
      e.target.value = "";
      return;
    }
    if (fullDateRegex.test(inputValue)) {
      setCurrentDate(inputValue);
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
        maxDate={
          props.disallowFutureDate
            ? format(
                new Date(),
                configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
                  ? "dd/MM/yyyy"
                  : "MM/dd/yyyy",
              )
            : ""
        }
        minDate={
          props.disallowPastDate
            ? format(
                new Date(),
                configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
                  ? "dd/MM/yyyy"
                  : "MM/dd/yyyy",
              )
            : ""
        }
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
          invalid={props.invalid}
          invalidText={props.invalidText}
          disabled={props.disabled}
          onChange={handleInputChange}
        />
      </DatePicker>
    </>
  );
};

export default CustomDatePicker;
