import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * The one badge vocabulary the anatomic-pathology case views share.
 *
 * Pathology, immunohistochemistry and cytology each grew their own colour
 * conventions, so the same outcome read green on one screen and grey on the
 * next. The seven kinds below are the shared meanings; a screen picks a kind,
 * never a Carbon colour and never a hex value.
 */
export const BADGE_KINDS = Object.freeze({
  complete: "green",
  critical: "red",
  inProgress: "blue",
  pending: "purple",
  verified: "teal",
  partial: "warm-gray",
  none: "gray",
});

/**
 * A badge always renders its meaning as text, because colour alone fails
 * WCAG 2.1 AA for anyone who cannot tell the seven kinds apart. Nothing here
 * carries an aria-label: the visible text is the badge's whole content, and
 * an aria-label would only replace those words with the same words. Where a
 * badge is deliberately terse, the control that holds it names what it stands
 * for, as the progress rail does.
 */
const StatusBadge = ({ kind = "none", textKey, values }) => {
  const intl = useIntl();

  return (
    <Tag type={BADGE_KINDS[kind] ?? BADGE_KINDS.none} size="sm">
      {intl.formatMessage({ id: textKey }, values)}
    </Tag>
  );
};

export default StatusBadge;
