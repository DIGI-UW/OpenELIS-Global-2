import React, { useState } from "react";
import { Button, ButtonSet, InlineNotification, Stack } from "@carbon/react";
import { useIntl } from "react-intl";

const TRANSITIONS = {
  "mark-positive": {
    titleId: "microbiology.cultureAction.positive.title",
    detailId: "microbiology.cultureAction.positive.detail",
    confirmId: "microbiology.cultureAction.positive.confirm",
    nextStage: "POSITIVE_SIGNAL",
    note: "Culture marked positive",
  },
  "mark-no-growth": {
    titleId: "microbiology.cultureAction.noGrowth.title",
    detailId: "microbiology.cultureAction.noGrowth.detail",
    confirmId: "microbiology.cultureAction.noGrowth.confirm",
    nextStage: "NO_GROWTH_READY",
    note: "Incubation complete with no growth",
  },
};

const CaseCultureTransitionPanel = ({
  action,
  caseId,
  service,
  onComplete,
  onCancel,
}) => {
  const intl = useIntl();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const transition = TRANSITIONS[action];

  if (!transition) {
    return null;
  }

  const confirm = () => {
    setSaving(true);
    setError("");
    service
      .recordCaseActivity(caseId, {
        nextStage: transition.nextStage,
        note: transition.note,
      })
      .then(onComplete)
      .catch(() => setError("transition"))
      .finally(() => setSaving(false));
  };

  return (
    <section
      className="microbiology-culture-transition"
      aria-labelledby="microbiology-culture-transition-title"
    >
      <Stack gap={4}>
        <div>
          <h3 id="microbiology-culture-transition-title">
            {intl.formatMessage({ id: transition.titleId })}
          </h3>
          <p>{intl.formatMessage({ id: transition.detailId })}</p>
        </div>
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.cultureAction.error",
            })}
          />
        )}
        <ButtonSet>
          <Button kind="secondary" disabled={saving} onClick={onCancel}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button disabled={saving} onClick={confirm}>
            {intl.formatMessage({ id: transition.confirmId })}
          </Button>
        </ButtonSet>
      </Stack>
    </section>
  );
};

export default CaseCultureTransitionPanel;
