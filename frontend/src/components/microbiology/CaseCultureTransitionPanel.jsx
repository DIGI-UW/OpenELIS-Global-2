import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  ButtonSet,
  InlineNotification,
  Select,
  SelectItem,
  Stack,
} from "@carbon/react";
import { useIntl } from "react-intl";

const TRANSITIONS = {
  "mark-positive": {
    titleId: "microbiology.cultureAction.positive.title",
    detailId: "microbiology.cultureAction.positive.detail",
    confirmId: "microbiology.cultureAction.positive.confirm",
    nextStage: "POSITIVE_SIGNAL",
    noteId: "microbiology.cultureAction.positive.note",
  },
  "mark-no-growth": {
    titleId: "microbiology.cultureAction.noGrowth.title",
    detailId: "microbiology.cultureAction.noGrowth.detail",
    confirmId: "microbiology.cultureAction.noGrowth.confirm",
    nextStage: "NO_GROWTH_READY",
    noteId: "microbiology.cultureAction.noGrowth.note",
  },
};

const CaseCultureTransitionPanel = ({
  action,
  caseId,
  specimens = [],
  readOnly = true,
  service,
  onComplete,
  onCancel,
}) => {
  const intl = useIntl();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [sourceSampleItemId, setSourceSampleItemId] = useState("");
  const transition = TRANSITIONS[action];
  const titleRef = useRef(null);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => titleRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [action]);

  if (!transition) {
    return null;
  }

  const confirm = () => {
    if (
      readOnly ||
      saving ||
      !specimens.some((sample) => sample.sampleItemId === sourceSampleItemId)
    ) {
      return;
    }
    setSaving(true);
    setError("");
    service
      .recordCaseActivity(caseId, {
        nextStage: transition.nextStage,
        note: intl.formatMessage({ id: transition.noteId }),
        sourceSampleItemId,
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
          <h3
            ref={titleRef}
            id="microbiology-culture-transition-title"
            tabIndex={-1}
          >
            {intl.formatMessage({ id: transition.titleId })}
          </h3>
          <p>{intl.formatMessage({ id: transition.detailId })}</p>
        </div>
        <Select
          id="microbiology-culture-observation-sample"
          labelText={intl.formatMessage({
            id: "microbiology.inoculation.sample",
          })}
          value={sourceSampleItemId}
          disabled={saving || readOnly}
          onChange={(event) => setSourceSampleItemId(event.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "microbiology.inoculation.samplePlaceholder",
            })}
          />
          {specimens.map((sample) => (
            <SelectItem
              key={sample.sampleItemId}
              value={sample.sampleItemId}
              text={sample.label || sample.sampleItemId}
            />
          ))}
        </Select>
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
          <Button
            disabled={saving || readOnly || !sourceSampleItemId}
            onClick={confirm}
          >
            {intl.formatMessage({ id: transition.confirmId })}
          </Button>
        </ButtonSet>
      </Stack>
    </section>
  );
};

export default CaseCultureTransitionPanel;
