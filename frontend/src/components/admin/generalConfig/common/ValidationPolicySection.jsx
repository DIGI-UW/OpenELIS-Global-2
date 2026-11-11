import React, { useState } from "react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  NumberInput,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { putToOpenElisServerFullResponse } from "../../../utils/Utils";
import { useInvalidateServerData } from "../../../utils/useServerData";

export const VALIDATION_POLICY_API = "/rest/validation-policy/default";
const TRIGGERS = ["ALL", "NEEDS_REVIEW", "NONE"];

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

const toDraft = (policy) => ({
  trigger: policy.trigger,
  levels: policy.levels,
});

export default function ValidationPolicySection({ policy: loaded }) {
  const intl = useIntl();
  const invalidate = useInvalidateServerData();
  const [policy, setPolicy] = useState(loaded);
  const [draft, setDraft] = useState(() => toDraft(loaded));
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);

  const [seen, setSeen] = useState(loaded);
  if (loaded !== seen) {
    setSeen(loaded);
    setPolicy(loaded);
    if (draft.trigger === policy.trigger && draft.levels === policy.levels) {
      setDraft(toDraft(loaded));
    }
  }

  const error = policyError(draft, policy.maxLevels);
  const changed =
    draft.trigger !== policy.trigger || draft.levels !== policy.levels;
  const roleName =
    (policy.levelRoles[0] && policy.levelRoles[0].roleName) || "Validation";
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);

  const setTrigger = (trigger) => {
    const levels =
      trigger === "NONE"
        ? 0
        : Math.max(1, Math.min(draft.levels, policy.maxLevels));
    setDraft({ trigger, levels });
    setNotice(null);
  };

  const setLevels = (value) => {
    const levels = Number.isNaN(Number(value)) ? 0 : Number(value);
    setDraft({ ...draft, levels });
    setNotice(null);
  };

  const reviews = message("summary.reviews", {
    count: draft.levels,
    role: roleName,
  });
  const released = message("summary.released");
  const blocked = policy.blockSelfValidation && draft.trigger !== "ALL";
  const waits = message("summary.blocked");
  const clean = blocked ? waits : draft.trigger === "ALL" ? reviews : released;
  const flagged = blocked
    ? waits
    : draft.trigger === "NONE"
      ? released
      : reviews;

  const save = () => {
    setSaving(true);
    setNotice(null);
    putToOpenElisServerFullResponse(
      VALIDATION_POLICY_API,
      JSON.stringify({ trigger: draft.trigger, levels: draft.levels }),
      async (response) => {
        setSaving(false);
        if (response && response.ok) {
          const saved = await response.json();
          setPolicy(saved);
          setDraft(toDraft(saved));
          invalidate(VALIDATION_POLICY_API);
          setNotice({ kind: "success", code: "saved" });
          return;
        }
        let body;
        try {
          body = (await response?.json()) || {};
        } catch {
          body = {};
        }
        setNotice({
          kind: "error",
          code: `refused.${body.code || "saveFailed"}`,
        });
      },
    );
  };

  return (
    <div className="validationPolicySection" data-testid="validation-policy">
      <Stack gap={5}>
        <div>
          <h4>
            <FormattedMessage id="admin.validationPolicy.title" />
          </h4>
          <h5>
            <FormattedMessage id="admin.validationPolicy.labDefault" />
          </h5>
          <p className="cds--label">
            <FormattedMessage id="admin.validationPolicy.labDefault.help" />
          </p>
        </div>
        <RadioButtonGroup
          legendText={message("trigger.legend")}
          name="validation-policy-trigger"
          orientation="vertical"
          valueSelected={draft.trigger}
          onChange={(value) => setTrigger(value)}
        >
          {TRIGGERS.map((trigger) => (
            <RadioButton
              key={trigger}
              id={`validation-policy-trigger-${trigger}`}
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
          id="validation-policy-levels"
          label={message("levels.label")}
          helperText={message("levels.help", { max: policy.maxLevels })}
          min={0}
          max={policy.maxLevels}
          step={1}
          value={draft.levels}
          allowEmpty={false}
          onChange={(event, { value }) => setLevels(value)}
        />
        {Array.from({
          length: Math.min(draft.levels, policy.maxLevels),
        }).map((_, index) => (
          <Select
            key={index}
            id={`validation-policy-level-${index + 1}-role`}
            labelText={message("level.role", { level: index + 1 })}
            helperText={message("level.role.help")}
            value={roleName}
            disabled
          >
            <SelectItem value={roleName} text={roleName} />
          </Select>
        ))}
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={message(`refused.${error}`)}
          />
        )}
        {blocked && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={message("blockSelfValidation")}
          />
        )}
        {!error && (
          <div data-testid="validation-policy-summary">
            <p>
              <FormattedMessage
                id="admin.validationPolicy.summary.clean"
                values={{ outcome: clean }}
              />
            </p>
            <p>
              <FormattedMessage
                id="admin.validationPolicy.summary.flagged"
                values={{ outcome: flagged }}
              />
            </p>
          </div>
        )}
        {notice && (
          <InlineNotification
            kind={notice.kind}
            lowContrast
            title={message(notice.code)}
            onClose={() => setNotice(null)}
          />
        )}
        <div>
          {saving ? (
            <InlineLoading description={message("saving")} />
          ) : (
            <Button
              id="validation-policy-save"
              disabled={!changed || Boolean(error)}
              onClick={save}
            >
              <FormattedMessage id="admin.validationPolicy.save" />
            </Button>
          )}
        </div>
      </Stack>
    </div>
  );
}
