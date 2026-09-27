import React, { useCallback, useContext, useRef, useState } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format, isValid, parse } from "date-fns";
import { useIntl } from "react-intl";
import { ConfigurationContext } from "../layout/Layout";

const CustomDatePicker = (props) => {
  const [currentDate, setCurrentDate] = useState(
    props.value ? props.value : "",
  );
  const { configurationProperties = { DEFAULT_DATE_LOCALE: "en-US" } } =
    useContext(ConfigurationContext) || {};
  const intl = useIntl();
  const dateLocale = useRef(configurationProperties.DEFAULT_DATE_LOCALE);
  dateLocale.current = configurationProperties.DEFAULT_DATE_LOCALE;
  // The calendar keeps the parser it was created with. Carbon's built-in one
  // only reads month-first dates, so a picker created before the site's
  // day-first format loaded read "26/09/2026" as 9 January. This parser reads
  // whichever format the site uses at the time.
  const parseDisplayDate = useCallback((text) => {
    const parsed = parse(
      text,
      dateLocale.current == "fr-FR" ? "dd/MM/yyyy" : "MM/dd/yyyy",
      new Date(),
    );
    return isValid(parsed) ? parsed : undefined;
  }, []);
  // Today's bounds as timestamps. A formatted date string is parsed back by
  // the calendar with its own rules, which read "26/09/2026" as a January
  // date, and a value set after mount was then clamped to that bound.
  const [todayBounds] = useState(() => ({
    start: new Date().setHours(0, 0, 0, 0),
    end: new Date().setHours(23, 59, 59, 999),
  }));
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
        parseDate={parseDisplayDate}
        value={displayedDate}
        onChange={(e) => handleDatePickerChange(e)}
        invalid={props.invalid}
        invalidText={props.invalidText}
        maxDate={props.disallowFutureDate ? todayBounds.end : ""}
        minDate={props.disallowPastDate ? todayBounds.start : ""}
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
