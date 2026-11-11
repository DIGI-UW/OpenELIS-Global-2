import React from "react";
import { Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";

// FHIR Task-aligned ReferralStatus values (org.openelisglobal.referral.valueholder.ReferralStatus).
const STATUS_CONFIG = {
  DRAFT: { type: "gray", labelId: "label.referOut.status.draft" },
  REQUESTED: { type: "blue", labelId: "label.referOut.status.requested" },
  RECEIVED: { type: "teal", labelId: "label.referOut.status.received" },
  IN_PROGRESS: { type: "purple", labelId: "label.referOut.status.inProgress" },
  COMPLETED: { type: "green", labelId: "label.referOut.status.completed" },
  CANCELLED: { type: "warm-gray", labelId: "label.referOut.status.cancelled" },
  REJECTED: { type: "red", labelId: "label.referOut.status.rejected" },
};

const ReferralStatusTag = ({ status, size = "sm" }) => {
  if (!status) {
    return (
      <Tag type="outline" size={size}>
        <FormattedMessage
          id="label.referOut.status.notReferred"
          defaultMessage="In-house"
        />
      </Tag>
    );
  }
  const config = STATUS_CONFIG[status] || {
    type: "gray",
    labelId: "label.referOut.status.draft",
  };
  return (
    <Tag type={config.type} size={size}>
      <FormattedMessage id={config.labelId} defaultMessage={status} />
    </Tag>
  );
};

export default ReferralStatusTag;
