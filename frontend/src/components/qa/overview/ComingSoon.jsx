import React from "react";
import { Tag, Tile } from "@carbon/react";
import { FormattedMessage } from "react-intl";

/**
 * Shared gray placeholder for QA Overview slots that future features light
 * up (OGC-683 delivery plan). Each instance is replaced wholesale by its live
 * component — placeholders are deleted, never reworked. The Tag names the
 * ticket that lights the slot up.
 *
 * Variants: "tile" (Today KPI / pillar chip), "stat" (This Week counter),
 * "row" (attention / inspector / activity line).
 */
const ComingSoon = ({ titleKey, ticket, variant = "tile" }) => {
  const body = (
    <div className="qa-cs-body">
      <div className="qa-cs-title">
        <FormattedMessage id={titleKey} />
      </div>
      <div className="qa-cs-caption">
        <FormattedMessage id="qa.overview.comingSoon" />
      </div>
    </div>
  );
  const ticketTag = (
    <Tag size="sm" type="gray" className="qa-cs-ticket">
      {ticket}
    </Tag>
  );

  if (variant === "row") {
    return (
      <div className="qa-cs qa-cs-row">
        {body}
        {ticketTag}
      </div>
    );
  }
  return (
    <Tile className={`qa-cs qa-cs-${variant}`}>
      {body}
      {ticketTag}
    </Tile>
  );
};

export default ComingSoon;
