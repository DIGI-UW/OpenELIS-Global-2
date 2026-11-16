import { useCallback, useRef, useState } from "react";
import { useIntl } from "react-intl";

const FULL_YEAR = /\d{4}/;

function flatpickrOf(input) {
  if (input?._flatpickr) return input._flatpickr;
  return (
    input?.closest(".cds--date-picker")?.querySelector("input")?._flatpickr ||
    null
  );
}

function inputsOf(fp) {
  if (fp.config.mode !== "range") return [fp.input];
  const wrapper = fp.input.closest(".cds--date-picker");
  const inputs = wrapper
    ? Array.from(wrapper.querySelectorAll("input")).filter(
        (input) => input.type !== "hidden",
      )
    : [fp.input];
  return inputs.slice(0, 2);
}

/**
 * Whether a typed date names the date it was parsed into: the parser rolls
 * 2026-13-45 over into 2027, so the date must format back to the same numbers
 * (leading zeros aside).
 */
function sameDigits(formatted, typed) {
  const digits = (text) =>
    (text.match(/\d+/g) || []).map((part) => String(Number(part))).join("-");
  return digits(formatted) === digits(typed) && !/[^\d\s/.\-:]/.test(typed);
}

/** The format a picker expects, as its field shows it (e.g. mm/dd/yyyy). */
function formatOf(fp) {
  return (
    fp.input.getAttribute("placeholder") ||
    fp.config.dateFormat
      .replace("Y", "yyyy")
      .replace("m", "mm")
      .replace("d", "dd")
  );
}

function shownText(fp, index) {
  const date = fp.selectedDates[index];
  return date ? fp.formatDate(date, fp.config.dateFormat) : "";
}

function selectedText(fp) {
  return fp.selectedDates
    .map((date) => fp.formatDate(date, fp.config.dateFormat))
    .join("|");
}

/**
 * Makes a date typed into a Carbon DatePicker reach the page's onChange.
 *
 * flatpickr commits typed text without an onChange when a click outside the
 * calendar closes it (and Carbon drops that close), and blanks text it cannot
 * parse. A filter then showed a date the search did not use (OGC-1635). Spread
 * picker(key) on the DatePicker and input(key) on its DatePickerInputs: on
 * close, blur or Enter, typed text is parsed with the picker's own parser and sent
 * through the picker's onChange, an empty field clears the date, and text that
 * is not a date stays in the field, marked invalid, while the date is cleared.
 * A typed start of a range is kept when the click that commits it lands outside:
 * flatpickr drops a one-date range there without telling onChange, so the page
 * held a date the field no longer showed.
 */
export default function useTypedDateCommit() {
  const intl = useIntl();
  const [invalidKeys, setInvalidKeys] = useState({});
  const attached = useRef(new WeakSet());

  const setInvalid = useCallback((key, reason) => {
    setInvalidKeys((prev) =>
      (prev[key] || null) === reason ? prev : { ...prev, [key]: reason },
    );
  }, []);

  const attach = useCallback(
    (key, fp) => {
      if (attached.current.has(fp)) return;
      attached.current.add(fp);
      const inputs = inputsOf(fp);
      const pending = new Map();
      let emitted = selectedText(fp);
      let typedStartOnly = false;
      const clear = fp.clear;
      fp.clear = (triggerChange = true, toInitial = true) => {
        const keep =
          typedStartOnly &&
          triggerChange === false &&
          fp.selectedDates.length === 1;
        typedStartOnly = false;
        if (keep) return undefined;
        return clear(triggerChange, toInitial);
      };

      const reconcile = () => {
        if (pending.size === 0) return false;
        const texts = inputs.map((input) =>
          (pending.has(input) ? pending.get(input) : input.value).trim(),
        );
        pending.clear();
        const filled = texts.filter(Boolean);
        if (filled.length === 0) {
          if (emitted) fp.clear();
          setInvalid(key, null);
          return false;
        }
        const refuse = (reason) => {
          if (emitted || fp.selectedDates.length > 0) fp.clear();
          inputs.forEach((input, index) => {
            input.value = texts[index];
            pending.set(input, texts[index]);
          });
          setInvalid(
            key,
            reason === "format" ? `format|${formatOf(fp)}` : reason,
          );
          return true;
        };
        const startMissing = fp.config.mode === "range" && !texts[0];
        const dates = filled.map((text) => {
          const date = FULL_YEAR.test(text)
            ? fp.parseDate(text, fp.config.dateFormat)
            : undefined;
          return date &&
            sameDigits(fp.formatDate(date, fp.config.dateFormat), text)
            ? date
            : undefined;
        });
        const { maxDate, minDate } = fp.config;
        if (startMissing || dates.some((date) => !date)) {
          return refuse("format");
        }
        if (maxDate && dates.some((date) => date > maxDate)) {
          return refuse("future");
        }
        if (minDate && dates.some((date) => date < minDate)) {
          return refuse("past");
        }
        if (dates.some((date) => !fp.isEnabled(date, false))) {
          return refuse("format");
        }
        const typedKey = dates
          .map((date) => fp.formatDate(date, fp.config.dateFormat))
          .join("|");
        if (typedKey !== emitted) fp.setDate(dates, true);
        typedStartOnly = fp.config.mode === "range" && dates.length === 1;
        setInvalid(key, null);
        return false;
      };

      fp.config.onChange.push(() => {
        emitted = selectedText(fp);
        if (fp.selectedDates.length > 0) {
          pending.clear();
          setInvalid(key, null);
        }
      });
      fp.config.onClose.push(() => {
        reconcile();
      });
      const wrapper =
        fp.input.closest(".cds--date-picker") || fp.input.parentElement;
      const isOwnInput = (target) => inputs.includes(target);
      wrapper.addEventListener(
        "keydown",
        (event) => {
          if (!isOwnInput(event.target) || event.key !== "Enter") return;
          if (pending.size > 0 && reconcile()) {
            event.preventDefault();
            event.stopImmediatePropagation();
          }
        },
        true,
      );
      wrapper.addEventListener(
        "blur",
        (event) => {
          if (!isOwnInput(event.target)) return;
          const next = event.relatedTarget;
          if (next && fp.calendarContainer?.contains(next)) return;
          if (reconcile()) event.stopImmediatePropagation();
        },
        true,
      );
      inputs.forEach((input, index) => {
        input.addEventListener("input", () => {
          if (input.value === shownText(fp, index) && !pending.has(input)) {
            return;
          }
          pending.set(input, input.value);
        });
      });
    },
    [setInvalid],
  );

  const messageFor = (reason) => {
    if (!reason) {
      return undefined;
    }
    if (reason.startsWith("format|")) {
      return intl.formatMessage(
        { id: "datepicker.invalidDate" },
        { format: reason.slice("format|".length) },
      );
    }
    return intl.formatMessage({
      id:
        reason === "future"
          ? "datepicker.future.notAllowed"
          : "datepicker.past.notAllowed",
    });
  };
  const picker = useCallback(
    (key) => ({ invalid: Boolean(invalidKeys[key]) }),
    [invalidKeys],
  );
  const input = useCallback(
    (key, { message = true } = {}) => ({
      onFocus: (event) => {
        const fp = flatpickrOf(event.target);
        if (fp) attach(key, fp);
      },
      invalidText: message ? messageFor(invalidKeys[key]) : undefined,
    }),
    [attach, invalidKeys, intl],
  );
  return { picker, input };
}
