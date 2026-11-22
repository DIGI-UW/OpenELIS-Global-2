import React from "react";
import { Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import {
  CheckmarkFilled,
  WarningAltFilled,
  WarningFilled,
  MisuseOutline,
} from "@carbon/icons-react";

/**
 * OGC-1022 (R3, FR-L1) — result flags as leading icon + Carbon Tag + bold text
 * at WCAG 2.2 AA, never background tint alone. The flag itself is computed
 * server-side (TestResultItem.resultFlag) against the patient-conditional
 * result limit.
 */
export type ResultFlag = "NORMAL" | "ABNORMAL" | "CRITICAL" | "INVALID";

export const isResultFlag = (value: unknown): value is ResultFlag =>
  value === "NORMAL" ||
  value === "ABNORMAL" ||
  value === "CRITICAL" ||
  value === "INVALID";

const FLAG_META: Record<
  ResultFlag,
  { Icon: React.ComponentType<{ size?: number }>; labelKey: string }
> = {
  NORMAL: { Icon: CheckmarkFilled, labelKey: "label.results.flag.normal" },
  ABNORMAL: {
    Icon: WarningAltFilled,
    labelKey: "label.results.flag.abnormal",
  },
  CRITICAL: { Icon: WarningFilled, labelKey: "label.results.flag.critical" },
  INVALID: { Icon: MisuseOutline, labelKey: "label.results.flag.invalid" },
};

export const FlagChip: React.FC<{ flag?: unknown }> = ({ flag }) => {
  if (!isResultFlag(flag)) {
    return null;
  }
  const { Icon, labelKey } = FLAG_META[flag];
  return (
    <Tag
      size="sm"
      type="gray"
      className={`unifiedFlagTag unifiedFlagTag--${flag.toLowerCase()}`}
      data-testid={`flag-${flag}`}
    >
      <Icon size={12} />
      <FormattedMessage id={labelKey} />
    </Tag>
  );
};

/** css modifier for the value cell's left accent bar (FR-L1 scannability). */
export const accentClass = (flag?: unknown): string =>
  isResultFlag(flag)
    ? `unifiedValueAccent unifiedValueAccent--${flag.toLowerCase()}`
    : "unifiedValueAccent";
