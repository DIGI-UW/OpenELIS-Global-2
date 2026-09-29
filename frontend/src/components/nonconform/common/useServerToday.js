import { useEffect, useState } from "react";
import { format, parse } from "date-fns";
import { fetchServerNow } from "../../order/serverClock";

const PICKER_FORMAT = "MM/dd/yyyy";

/**
 * Today on the lab server, written the way the NCE date pickers take it. The
 * browser's day is used until the server answers, or if it cannot be asked.
 */
export default function useServerToday() {
  const [today, setToday] = useState(() => format(new Date(), PICKER_FORMAT));

  useEffect(() => {
    let mounted = true;
    fetchServerNow().then(({ date }) => {
      if (mounted) {
        setToday(format(parse(date, "yyyy-MM-dd", new Date()), PICKER_FORMAT));
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  return today;
}
