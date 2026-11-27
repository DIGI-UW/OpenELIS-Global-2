import React from "react";
import { useIntl } from "react-intl";
import StatusBadge from "../caseView/StatusBadge";
import { coverslipQcBadge, stainStatusBadge } from "./pathologyRows";

/**
 * How far a slide has got at staining (`kind="stain"`) or coverslipping
 * (`kind="qc"`), in words as well as in color. `caseStatus` is the stage the
 * slide's case stands at, which decides whether nothing recorded reads
 * Pending or Not recorded; without it, as on another program's screen, a
 * slide with nothing recorded shows no tag at all.
 *
 * A QC result read away from its column ("Passed" beside "Stained") does not
 * say what passed, so `named` puts it in a sentence that does.
 */
const SlideStatusTag = ({ kind, slide, caseStatus, named = false }) => {
  const intl = useIntl();
  const badge =
    kind === "qc"
      ? coverslipQcBadge(slide, caseStatus)
      : stainStatusBadge(slide, caseStatus);
  if (!badge) {
    return null;
  }
  if (kind === "qc" && named) {
    return (
      <StatusBadge
        kind={badge.kind}
        textKey="pathology.badge.coverslipQcResult"
        values={{ result: intl.formatMessage({ id: badge.textKey }) }}
      />
    );
  }
  return <StatusBadge {...badge} />;
};

export default SlideStatusTag;
