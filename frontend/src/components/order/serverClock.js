import { getFromOpenElisServer } from "../utils/Utils";
import { currentLocalTime, todayLocalIso } from "./dateUtils";

/**
 * "Now" as the lab server keeps it, `{ date: yyyy-MM-dd, time: HH:mm }`.
 *
 * Collection and receipt times are stored in the server's clock, so a sample
 * stamped from the browser's clock is off by the difference between the two
 * zones: an environmental sample saved at 08:56 server time was recorded as
 * collected at 11:56 from a browser three hours ahead. The browser's clock is
 * used only when the server cannot be asked.
 */
export const fetchServerNow = () =>
  new Promise((resolve) => {
    getFromOpenElisServer("/rest/server-time", (response) => {
      resolve(
        response?.date && response?.time
          ? { date: response.date, time: response.time }
          : { date: todayLocalIso(), time: currentLocalTime() },
      );
    });
  });
