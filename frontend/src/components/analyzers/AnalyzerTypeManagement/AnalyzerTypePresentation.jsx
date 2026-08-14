import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

export const mappingPercentage = (mapping) => {
  const total = mapping?.total || 0;
  const complete = (mapping?.bound || 0) + (mapping?.ignored || 0);
  return total > 0 ? Math.round((complete / total) * 100) : 100;
};

export const mappingProgress = (mapping) => {
  const total = mapping?.total || 0;
  const complete = (mapping?.bound || 0) + (mapping?.ignored || 0);
  return `${complete} / ${total} \u00b7 ${mappingPercentage(mapping)}%`;
};

export const isProgressComplete = (mapping) =>
  Boolean(
    mapping &&
    mapping.unresolved === 0 &&
    mapping.missing === 0 &&
    mapping.extra === 0 &&
    mapping.bound + mapping.ignored === mapping.total,
  );

export const isMappingComplete = (profile) =>
  isProgressComplete(profile.testMappings) &&
  isProgressComplete(profile.resultValueMappings);

export const profileMetadata = (profile) =>
  [
    profile.manufacturer,
    profile.model,
    profile.legacyVersion ? `v${profile.legacyVersion}` : null,
  ]
    .filter(Boolean)
    .join(" \u00b7 ");

export const AnalyzerTypeSourceTag = ({ source }) => {
  const intl = useIntl();
  return (
    <Tag type={source === "SITE" ? "blue" : "gray"} size="sm">
      {intl.formatMessage({
        id: `analyzerType.source.${source.toLowerCase()}`,
      })}
    </Tag>
  );
};

export const AnalyzerTypeStatusTag = ({ status }) => {
  const intl = useIntl();
  return (
    <Tag type={status === "ACTIVE" ? "green" : "gray"}>
      {intl.formatMessage({
        id: `analyzerType.status.${status.toLowerCase()}`,
      })}
    </Tag>
  );
};

export const AnalyzerTypeMappingProgress = ({ mapping }) => (
  <span
    className={
      isProgressComplete(mapping)
        ? "analyzer-type-progress--complete"
        : "analyzer-type-progress--incomplete"
    }
  >
    {mappingProgress(mapping)}
  </span>
);
