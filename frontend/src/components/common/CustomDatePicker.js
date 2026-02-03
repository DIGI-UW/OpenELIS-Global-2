import React, { useEffect, useState, useContext } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";
import { FormattedMessage, useIntl } from "react-intl";

const CustomDatePicker = (props) => {
  const intl = useIntl();
  const [isInvalid, setIsInvalid] = useState(false);
  const [currentDate, setCurrentDate] = useState(
    props.value ? props.value : "",
  );
  const { configurationProperties } = useContext(ConfigurationContext);

  function handleDatePickerChange(e) {
    const selectedDate = e[0];

    if (!selectedDate) {
      setIsInvalid(false);
      if (props.isInvalidExternal) props.isInvalidExternal(false);
      props.onChange("");
      return;
    }

    const dateObj = new Date(selectedDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (props.disallowFutureDate && dateObj > today) {
      setIsInvalid(true);
      if (props.isInvalidExternal) props.isInvalidExternal(true);

      const formatDate = format(
        dateObj,
        configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
          ? "dd/MM/yyyy"
          : "MM/dd/yyyy",
      );
      setCurrentDate(formatDate);
      props.onChange("");

      return;
    }

    setIsInvalid(false);
    if (props.isInvalidExternal) props.isInvalidExternal(false);

    const formatDate = format(
      dateObj,
      configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
        ? "dd/MM/yyyy"
        : "MM/dd/yyyy",
    );
    setCurrentDate(formatDate);
    props.onChange(formatDate);
  }

  function handleInputChange(e) {
    const inputValue = e.target.value;

    if (inputValue === "") {
      setIsInvalid(false);
      if (props.isInvalidExternal) props.isInvalidExternal(false);
      setCurrentDate("");
      props.onChange("");
      return;
    }

    const isFrenchLocale =
      configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
    const partialDateRegex = /^(\d{0,2})(\/(\d{0,2})(\/(\d{0,4})?)?)?$/;

    if (partialDateRegex.test(inputValue)) {
      e.target.value = inputValue;
      const parts = inputValue.split("/");

      if (parts.length === 3 && parts[2].length === 4) {
        const day = isFrenchLocale ? parseInt(parts[0]) : parseInt(parts[1]);
        const month = isFrenchLocale ? parseInt(parts[1]) : parseInt(parts[0]);
        const year = parseInt(parts[2]);
        const dateObj = new Date(year, month - 1, day);

        if (
          dateObj.getDate() === day &&
          dateObj.getMonth() === month - 1 &&
          dateObj.getFullYear() === year
        ) {
          const today = new Date();
          today.setHours(0, 0, 0, 0);

          if (props.disallowFutureDate && dateObj > today) {
            setIsInvalid(true);

            if (props.isInvalidExternal) props.isInvalidExternal(true);

            setCurrentDate("");
            props.onChange("");
            e.target.value = "";

            if (props.isInvalidExternal) props.isInvalidExternal(false);
          } else {
            setIsInvalid(false);
            if (props.isInvalidExternal) props.isInvalidExternal(false);
            setCurrentDate(inputValue);
            props.onChange(inputValue);
          }
        } else {
          setIsInvalid(true);
          if (props.isInvalidExternal) props.isInvalidExternal(true);
          setCurrentDate("");
          props.onChange("");
          e.target.value = "";
          if (props.isInvalidExternal) props.isInvalidExternal(false);
        }
      }
    } else {
      e.target.value = "";
    }
  }

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
          configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
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
                configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
                  ? "dd/MM/yyyy"
                  : "MM/dd/yyyy",
              )
            : ""
        }
        minDate={
          props.disallowPastDate
            ? format(
                new Date(),
                configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
                  ? "dd/MM/yyyy"
                  : "MM/dd/yyyy",
              )
            : ""
        }
      >
        <DatePickerInput
          id={props.id}
          placeholder={
            configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR"
              ? "dd/mm/yyyy"
              : "mm/dd/yyyy"
          }
          type="text"
          labelText={props.labelText}
          invalid={isInvalid}
          invalidText={
            isInvalid
              ? props.disallowFutureDate
                ? intl.formatMessage({
                    id: "genericSample.order.error.futureDate",
                  })
                : intl.formatMessage({
                    id: "genericSample.order.error.invalidDate",
                  })
              : ""
          }
          disabled={props.disabled}
          onChange={handleInputChange}
        />
      </DatePicker>
    </>
  );
};

export default CustomDatePicker;
