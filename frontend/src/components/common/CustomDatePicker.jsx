import React, { useState, useContext } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format } from "date-fns";
import { useIntl } from "react-intl";
import { ConfigurationContext } from "../layout/Layout";

const CustomDatePicker = (props) => {
  const [currentDate, setCurrentDate] = useState(
    props.value ? props.value : "",
  );
  const { configurationProperties = { DEFAULT_DATE_LOCALE: "en-US" } } =
    useContext(ConfigurationContext) || {};
  const intl = useIntl();
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
      props.onChange("");
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
      props.onChange(inputValue);
    }
  }

  const displayedDate = props.updateStateValue
    ? props.value || ""
    : currentDate;

  return (
    <>
      <DatePicker
        id={`${props.id}-picker`}
        dateFormat={
          configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
            ? "d/m/Y"
            : "m/d/Y"
        }
        className={props.className}
        datePickerType="single"
        value={displayedDate}
        onChange={(e) => handleDatePickerChange(e)}
        invalid={props.invalid}
        invalidText={props.invalidText}
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
          placeholder={intl.formatMessage({
            id:
              configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
                ? "datepicker.placeholder.dmy"
                : "datepicker.placeholder.mdy",
            defaultMessage:
              configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
                ? "dd/mm/yyyy"
                : "mm/dd/yyyy",
          })}
          type="text"
          labelText={props.labelText}
          helperText={props.helperText}
          invalid={props.invalid}
          invalidText={props.invalidText}
          aria-invalid={props.invalid || undefined}
          disabled={props.disabled}
          onInput={handleInputChange}
        />
      </DatePicker>
    </>
  );
};

export default CustomDatePicker;
