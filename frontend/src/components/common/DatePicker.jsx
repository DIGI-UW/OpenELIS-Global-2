import React, {
  Children,
  cloneElement,
  forwardRef,
  isValidElement,
  useRef,
} from "react";
import { DatePicker as CarbonDatePicker } from "@carbon/react";
import useTypedDateCommit from "./useTypedDateCommit";

let pickers = 0;

/**
 * Carbon's DatePicker with typed dates committed (OGC-1635): a date typed into
 * the field reaches onChange when the user clicks elsewhere, tabs or presses
 * Enter, and text that is not a date stays in the field, marked, while the date
 * is cleared. Use it in place of the Carbon DatePicker; the props are the same.
 * It attaches on focus, and on a click or key press too: a field that had focus
 * from the start (autoFocus) got it before Carbon made the calendar.
 */
export const DatePicker = forwardRef(function DatePicker(
  { children, invalid, ...props },
  ref,
) {
  const key = useRef(null);
  if (key.current === null) {
    pickers += 1;
    key.current = `date-picker-${pickers}`;
  }
  const typedDate = useTypedDateCommit();
  const typedInput = typedDate.input(key.current);
  let first = true;
  const inputs = Children.map(children, (child) => {
    if (!isValidElement(child)) {
      return child;
    }
    const message = first ? typedInput.invalidText : undefined;
    first = false;
    return cloneElement(child, {
      onFocus: (event) => {
        typedInput.onFocus(event);
        child.props.onFocus?.(event);
      },
      onMouseDown: (event) => {
        typedInput.onFocus(event);
        child.props.onMouseDown?.(event);
      },
      onKeyDown: (event) => {
        typedInput.onFocus(event);
        child.props.onKeyDown?.(event);
      },
      invalidText: message || child.props.invalidText,
    });
  });
  return (
    <CarbonDatePicker
      ref={ref}
      {...props}
      invalid={Boolean(invalid) || typedDate.picker(key.current).invalid}
    >
      {inputs}
    </CarbonDatePicker>
  );
});

export default DatePicker;
