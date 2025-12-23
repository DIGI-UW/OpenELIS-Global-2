import React, {
  useEffect,
  useCallback,
  useState,
  useContext,
  useMemo,
} from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format, parse } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";

const CustomDatePicker = (props) => {
  const [currentDate, setCurrentDate] = useState(props.value || "");
  const { configurationProperties } = useContext(ConfigurationContext);

  const { displayDateFormat, carbonDateFormat } = useMemo(() => {
    const formatDate = getDateFormat(
      configurationProperties.DEFAULT_DATE_LOCALE,
    );

    return {
      displayDateFormat: formatDate("display"),
      carbonDateFormat: formatDate("carbon"),
    };
  }, [configurationProperties.DEFAULT_DATE_LOCALE]);

  useEffect(() => {
    if (props.updateStateValue) {
      setCurrentDate(props.value);
    }
  }, [props.value, props.updateStateValue]);

  const todayFormatted = useMemo(
    () => format(new Date(), displayDateFormat),
    [displayDateFormat],
  );

  const parseDate = useCallback(
    (dateString) => {
      if (!dateString) return false;

      const formats = [
        displayDateFormat,
        displayDateFormat.replaceAll("/", ""),
      ];

      for (const f of formats) {
        const parsed = parse(dateString, f, new Date());
        if (!isNaN(+parsed)) return parsed;
      }

      return false;
    },
    [displayDateFormat],
  );

  const handleDatePickerChange = useCallback(
    (e) => {
      const date = e?.[0] ? new Date(e[0]) : null;

      if (!date || isNaN(+date)) {
        setCurrentDate("");
        props.onChange("");
        return;
      }

      const formatted = format(date, displayDateFormat);
      setCurrentDate(formatted);
      props.onChange(formatted);
    },
    [displayDateFormat, props.onChange],
  );

  return (
    <>
      <DatePicker
        id={props.id}
        dateFormat={carbonDateFormat}
        className={props.className}
        datePickerType="single"
        value={currentDate}
        onChange={(e) => handleDatePickerChange(e)}
        maxDate={props.disallowFutureDate ? todayFormatted : ""}
        minDate={props.disallowPastDate ? todayFormatted : ""}
        parseDate={parseDate}
      >
        <DatePickerInput
          id={props.id}
          placeholder={displayDateFormat}
          type="text"
          labelText={props.labelText}
          invalid={props.invalid}
          invalidText={props.invalidText}
          disabled={props.disabled}
        />
      </DatePicker>
    </>
  );
};

function getDateFormat(locale) {
  const dateFormat = new Intl.DateTimeFormat(locale);
  const parts = dateFormat.formatToParts(new Date());
  return function (formatType) {
    return parts
      .map((part) => {
        switch (part.type) {
          case "month":
            return formatType === "carbon" ? "m" : "MM";
          case "day":
            return formatType === "carbon" ? "d" : "dd";
          case "year":
            return formatType === "carbon" ? "Y" : "yyyy";
          case "literal":
            return part.value;
          default:
            null;
        }
      })
      .filter(Boolean)
      .join("");
  };
}

export default CustomDatePicker;
