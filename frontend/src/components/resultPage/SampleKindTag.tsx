import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * The kind of sample a result row belongs to: a client sample, or a QC sample
 * of a given kind (blank, control, duplicate). One rendering shared by the
 * legacy Results screen's Sample Kind column and the unified screen's expanded
 * row, so the two never drift apart.
 */
const SampleKindTag: React.FC<{ qcType?: string | null }> = ({ qcType }) => {
  const intl = useIntl();
  if (!qcType) {
    return (
      <Tag size="sm" type="outline">
        {intl.formatMessage({ id: "label.sampleKind.client" })}
      </Tag>
    );
  }
  const labelKey = `label.sampleKind.${qcType.toLowerCase()}`;
  return (
    <span style={{ display: "inline-flex", gap: "0.25rem" }}>
      <Tag size="sm" type="purple">
        {intl.formatMessage({ id: "label.sampleKind.qc" })}
      </Tag>
      <Tag size="sm" type="warm-gray">
        {intl.formatMessage({ id: labelKey, defaultMessage: qcType })}
      </Tag>
    </span>
  );
};

export default SampleKindTag;
