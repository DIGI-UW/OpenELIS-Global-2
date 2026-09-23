import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import config from "../../../config.json";

/** Where an accrediting body's stored logo is served from. */
export const accreditationLogoUrl = (logoImageId) =>
  `${config.serverBaseUrl}/rest/accreditation/logo/${logoImageId}`;

/**
 * A body's or enrollment's accreditation status, tagged. One status shown two
 * ways would read as two different things, so the QMS page and the test
 * catalog's accreditation section render it from here.
 */
const STATUS_TAG_TYPE = {
  ACTIVE: "green",
  EXPIRING: "magenta",
  EXPIRED: "red",
  INACTIVE: "gray",
};

const AccreditationStatusTag = ({ status, size }) => {
  const intl = useIntl();
  if (!status) {
    return "—";
  }
  return (
    <Tag type={STATUS_TAG_TYPE[status] || "gray"} size={size}>
      {intl.formatMessage({ id: `qa.qms.accreditation.status.${status}` })}
    </Tag>
  );
};

export default AccreditationStatusTag;
