import React from "react";
import {
  InlineNotification,
  NumberInput,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

export const TRIGGERS = ["ALL", "NEEDS_REVIEW", "NONE"];

export function policyError(policy, maxLevels) {
  if (maxLevels != null && policy.levels > maxLevels) {
    return "levelsOutOfRange";
  }
  if (policy.levels === 0 && policy.trigger !== "NONE") {
    return "zeroReviewsNeedsNoReview";
  }
  if (policy.levels > 0 && policy.trigger === "NONE") {
    return "noReviewWithReviews";
  }
  return null;
}

export const toDraft = (policy) => ({
  trigger: policy.trigger,
  levels: policy.levels,
});

export const roleNameOf = (policy) =>
  (policy &&
    policy.levelRoles &&
    policy.levelRoles[0] &&
    policy.levelRoles[0].roleName) ||
  "Validation";

/**
 * What a policy does with the results the clearance rule clears and with the
 * ones that need review, as the page words them.
 */
export function policyOutcomes(policy, roleName, blockSelfValidation, intl) {
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);
  const reviews = message("summary.reviews", {
    count: policy.levels,
    role: roleName,
  });
  const released = message("summary.released");
  if (blockSelfValidation && policy.trigger !== "ALL") {
    const waits = message("summary.blocked");
    return { clean: waits, flagged: waits };
  }
  return {
    clean: policy.trigger === "ALL" ? reviews : released,
    flagged: policy.trigger === "NONE" ? released : reviews,
  };
}

/**
 * Which results need review, the number of reviews and the role of each level,
 * shared by the lab-wide default and the lab unit overrides.
 */
export default function ValidationPolicyEditor({
  idPrefix,
  draft,
  onChange,
  maxLevels,
  roleName,
}) {
  const intl = useIntl();
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);
  const error = policyError(draft, maxLevels);

  const setTrigger = (trigger) => {
    const levels =
      trigger === "NONE" ? 0 : Math.max(1, Math.min(draft.levels, maxLevels));
    onChange({ trigger, levels });
  };

  const setLevels = (value) => {
    const levels = Number.isNaN(Number(value)) ? 0 : Number(value);
    onChange({ ...draft, levels });
  };

  return (
    <>
      <RadioButtonGroup
        legendText={message("trigger.legend")}
        name={`${idPrefix}-trigger`}
        orientation="vertical"
        valueSelected={draft.trigger}
        onChange={(value) => setTrigger(value)}
      >
        {TRIGGERS.map((trigger) => (
          <RadioButton
            key={trigger}
            id={`${idPrefix}-trigger-${trigger}`}
            value={trigger}
            labelText={message(`trigger.${trigger}`)}
          />
        ))}
      </RadioButtonGroup>
      {draft.trigger === "NEEDS_REVIEW" && (
        <p className="cds--label">
          <FormattedMessage id="admin.validationPolicy.trigger.NEEDS_REVIEW.help" />
        </p>
      )}
      <NumberInput
        id={`${idPrefix}-levels`}
        label={message("levels.label")}
        helperText={message("levels.help", { max: maxLevels })}
        min={0}
        max={maxLevels}
        step={1}
        value={draft.levels}
        allowEmpty={false}
        onChange={(event, { value }) => setLevels(value)}
      />
      {Array.from({ length: Math.min(draft.levels, maxLevels) }).map(
        (_, index) => (
          <Select
            key={index}
            id={`${idPrefix}-level-${index + 1}-role`}
            labelText={message("level.role", { level: index + 1 })}
            helperText={message("level.role.help")}
            value={roleName}
            disabled
          >
            <SelectItem value={roleName} text={roleName} />
          </Select>
        ),
      )}
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={message(`refused.${error}`)}
        />
      )}
    </>
  );
}
