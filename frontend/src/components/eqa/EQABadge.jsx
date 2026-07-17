import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

const PRIORITY_COLOR_MAP = {
  STANDARD: "blue",
  URGENT: "warm-gray",
  CRITICAL: "red",
};

const EQABadge = ({ priority }) => {
  const intl = useIntl();
  const tagType = PRIORITY_COLOR_MAP[priority] || "blue";

  return (
    <Tag type={tagType} size="sm">
      {intl.formatMessage({ id: "eqa.sample.badge" })}
    </Tag>
  );
};

export default EQABadge;
