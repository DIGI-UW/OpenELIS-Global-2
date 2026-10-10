import React from "react";
import { FormattedMessage } from "react-intl";

/**
 * The asterisk after a required field's label. It is the visual cue only:
 * assistive technology learns that the field is required from aria-required on
 * the input itself ({@link requiredProps}), so the asterisk is hidden from it.
 * Where there is no single input to carry aria-required (a section heading),
 * `announce` adds the word for screen readers instead.
 */
export const RequiredMarker = ({ required = true, announce = false }) =>
  required ? (
    <>
      <span className="requiredlabel" aria-hidden="true">
        {" *"}
      </span>
      {announce && (
        <span className="cds--visually-hidden">
          <FormattedMessage id="label.required" defaultMessage="required" />
        </span>
      )}
    </>
  ) : null;

/** The attributes that tell assistive technology an input is required. */
export const requiredProps = (required = true) =>
  required ? { "aria-required": true } : {};
