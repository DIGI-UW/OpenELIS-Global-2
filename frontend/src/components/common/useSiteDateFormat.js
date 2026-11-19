import { useCallback, useContext, useRef } from "react";
import { useIntl } from "react-intl";
import { format, isValid, parse } from "date-fns";
import { ConfigurationContext } from "../layout/contexts";
import { labNow } from "../utils/labClock";

export default function useSiteDateFormat() {
  const { configurationProperties = { DEFAULT_DATE_LOCALE: "en-US" } } =
    useContext(ConfigurationContext) || {};
  const intl = useIntl();
  const dayFirst = configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
  const pattern = dayFirst ? "dd/MM/yyyy" : "MM/dd/yyyy";
  const patternRef = useRef(pattern);
  patternRef.current = pattern;
  // The calendar keeps the parser it was created with, so a picker created
  // before the site settings loaded would keep reading dates month-first.
  const parseDate = useCallback((text) => {
    if (!/^\d{1,2}\/\d{1,2}\/\d{4}$/.test(String(text).trim())) {
      return undefined;
    }
    const parsed = parse(String(text).trim(), patternRef.current, labNow());
    return isValid(parsed) ? parsed : undefined;
  }, []);
  const formatIsoDate = useCallback(
    (text) => {
      const day = String(text ?? "").slice(0, 10);
      const parsed = /^\d{4}-\d{2}-\d{2}$/.test(day)
        ? parse(day, "yyyy-MM-dd", labNow())
        : null;
      return parsed && isValid(parsed) ? format(parsed, pattern) : (text ?? "");
    },
    [pattern],
  );
  const formatIsoDateTime = useCallback(
    (text) => {
      const time = /^\d{4}-\d{2}-\d{2}[ T](\d{2}:\d{2})/.exec(
        String(text ?? ""),
      );
      const day = formatIsoDate(text);
      return time && day !== text ? `${day} ${time[1]}` : day;
    },
    [formatIsoDate],
  );
  return {
    pattern,
    dateFormat: dayFirst ? "d/m/Y" : "m/d/Y",
    parseDate,
    formatIsoDate,
    formatIsoDateTime,
    placeholder: intl.formatMessage({
      id: dayFirst
        ? "datepicker.placeholder.dmy"
        : "datepicker.placeholder.mdy",
      defaultMessage: dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy",
    }),
  };
}
