import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

const STATUS_TYPES = {
  IN_PROGRESS: "blue",
  PARTIAL: "cyan",
  FINAL: "green",
  AMENDED: "magenta",
};

export default function CaseStatusTags({ caseStatus, statusTags = [] }) {
  const intl = useIntl();
  const t = (id) => intl.formatMessage({ id });
  // i18n-keys: microbiology.case.status.*, microbiology.case.tag.*
  return (
    <>
      {caseStatus && (
        <Tag type={STATUS_TYPES[caseStatus] || "gray"}>
          {t(`microbiology.case.status.${caseStatus}`)}
        </Tag>
      )}
      {statusTags.map((tag) => (
        <Tag key={tag} type="purple">
          {t(`microbiology.case.tag.${tag}`)}
        </Tag>
      ))}
    </>
  );
}
