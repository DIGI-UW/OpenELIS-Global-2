import React from "react";
import { useIntl } from "react-intl";
import StatusBadge from "../caseView/StatusBadge";
import { hasVerifiableLabel, isLabelVerified } from "./pathologyRows";

/**
 * Whether a slide's label has been checked against its block, in words as well
 * as in colour (FR-7.3). A verified slide names who checked it and when, from
 * the case read's `labelVerifications`; a slide that cannot be checked, being
 * unsaved or cut before slides named their block, carries no tag at all.
 *
 * Carbon's Tag keeps a `title` for itself and gives its label the label's own
 * words as a title, so the verifier is named on a wrapper for the mouse and in
 * hidden text for a screen reader.
 */
const LabelVerificationTag = ({ slide, labelVerifications }) => {
  const intl = useIntl();

  if (!hasVerifiableLabel(slide)) {
    return null;
  }
  if (!isLabelVerified(slide, labelVerifications)) {
    return <StatusBadge kind="none" textKey="pathology.badge.unverified" />;
  }
  const verification = labelVerifications[String(slide.id)] ?? {};
  const verifiedBy = intl.formatMessage(
    { id: "pathology.badge.verifiedBy" },
    {
      name: verification.verifiedBy ?? "",
      date: verification.verifiedAt ?? "",
    },
  );
  return (
    <span
      className="pathology-case-view__label-verification"
      title={verifiedBy}
    >
      <StatusBadge kind="verified" textKey="pathology.badge.verified" />
      <span className="cds--visually-hidden">{verifiedBy}</span>
    </span>
  );
};

export default LabelVerificationTag;
