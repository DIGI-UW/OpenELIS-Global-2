import { useCallback, useContext, useRef } from "react";
import { useIntl } from "react-intl";
import { isValid, parse } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";
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
    const parsed = parse(text, patternRef.current, labNow());
    return isValid(parsed) ? parsed : undefined;
  }, []);
  return {
    pattern,
    dateFormat: dayFirst ? "d/m/Y" : "m/d/Y",
    parseDate,
    placeholder: intl.formatMessage({
      id: dayFirst
        ? "datepicker.placeholder.dmy"
        : "datepicker.placeholder.mdy",
      defaultMessage: dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy",
    }),
  };
}
