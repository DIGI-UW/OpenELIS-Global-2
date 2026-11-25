import CultureSetSummary from "../../../microbiology/CultureSetSummary";
import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import { Callout, InlineNotification, Button } from "@carbon/react";
import { previewMicrobiologyOrder } from "../../../microbiology/MicrobiologyService";

export function useMicroOrderPreview({
  samples,
  savedOrder = false,
  domain,
  samplingSiteId,
  samplingSiteName,
}) {
  const [result, setResult] = useState(null);
  const [failedPayload, setFailedPayload] = useState(null);
  const [attempt, setAttempt] = useState(0);
  const payload = JSON.stringify({
    ...(domain ? { domain, samplingSiteId, samplingSiteName } : {}),
    specimens: samples
      .filter((sample) => sample.sampleTypeId)
      .map((sample) => ({
        sampleTypeId: sample.sampleTypeId,
        ...(sample.collectionLocationId
          ? { collectionLocationId: sample.collectionLocationId }
          : {}),
        ...(sample.container != null ? { container: sample.container } : {}),
        ...(sample.bodySite != null ? { bodySite: sample.bodySite } : {}),
        ...(sample.collectionDate
          ? { collectionDate: sample.collectionDate }
          : {}),
        ...(sample.collectionTime
          ? { collectionTime: sample.collectionTime }
          : {}),
        ...(Number.isInteger(Number(sample.cultureSetNumber)) &&
        Number(sample.cultureSetNumber) > 0
          ? { cultureSetNumber: Number(sample.cultureSetNumber) }
          : {}),
        testIds: (sample.tests || []).map((test) => test.id),
      })),
  });
  const hasTests = samples.some((sample) => sample.tests?.length);
  const existing = savedOrder || samples.some((sample) => sample.sampleItemId);
  useEffect(() => {
    let current = true;
    setResult(null);
    setFailedPayload(null);
    if (!existing && hasTests) {
      previewMicrobiologyOrder(JSON.parse(payload)).then(
        (response) => {
          if (current) setResult({ payload, response });
        },
        () => {
          if (current) setFailedPayload(payload);
        },
      );
    }
    return () => {
      current = false;
    };
  }, [payload, existing, hasTests, attempt]);

  const preview = result?.payload === payload ? result.response : null;
  const hidden =
    existing || !hasTests || (preview && preview.cases.length === 0);
  const typedSampleIndexes = samples.reduce(
    (indexes, sample, index) =>
      sample.sampleTypeId ? [...indexes, index] : indexes,
    [],
  );
  const caseIndexesBySample = {};
  (preview?.cases || []).forEach((entry, caseIndex) => {
    entry.specimens.forEach((specimen) => {
      const sampleIndex = typedSampleIndexes[specimen.index];
      if (sampleIndex === undefined) return;
      caseIndexesBySample[sampleIndex] = [
        ...(caseIndexesBySample[sampleIndex] || []),
        caseIndex,
      ];
    });
  });
  const splitSamples = {};
  (preview?.warnings || []).forEach((warning) => {
    const sampleIndex = typedSampleIndexes[warning.specimenIndex];
    if (sampleIndex === undefined) return;
    splitSamples[sampleIndex] = true;
  });
  const ordinaryTestsBySample = {};
  (preview?.ordinaryTests || []).forEach((entry) => {
    const sampleIndex = typedSampleIndexes[entry.specimenIndex];
    if (sampleIndex === undefined) return;
    ordinaryTestsBySample[sampleIndex] = [
      ...(ordinaryTestsBySample[sampleIndex] || []),
      entry.testName,
    ];
  });

  return {
    hidden,
    loading: !hidden && !preview && failedPayload !== payload,
    failed: failedPayload === payload,
    retry: () => setAttempt((value) => value + 1),
    preview,
    caseIndexesBySample,
    splitSamples,
    ordinaryTestsBySample,
  };
}

export function MicroSampleCaseNote({ preview, sampleIndex }) {
  const intl = useIntl();
  if (!preview || preview.hidden || !preview.preview) return null;
  const answer = preview.preview;
  const caseIndexes = preview.caseIndexesBySample[sampleIndex] || [];
  const cases = caseIndexes.map((index) => answer.cases[index]);
  const ordinaryTests = preview.ordinaryTestsBySample[sampleIndex] || [];
  const hasOtherWork =
    answer.cases.length > 1 || (answer.ordinaryTests || []).length > 0;
  const unitWarnings =
    hasOtherWork && !preview.splitSamples[sampleIndex]
      ? (answer.newUnitWarnings || []).filter((warning) =>
          cases.some((entry) => entry.labUnitId === warning.labUnitId),
        )
      : [];
  const sentence = (entry) =>
    intl.formatMessage(
      { id: "order.microPreview.sampleNote" },
      {
        tests: intl.formatList(
          entry.testNames.map((name) => <strong key={name}>{name}</strong>),
          { type: "conjunction" },
        ),
        count: entry.testNames.length,
        unit: <strong>{entry.labUnitName}</strong>,
      },
    );
  const siteName = cases.find((entry) => entry.siteName)?.siteName;
  return (
    <>
      {cases.length > 0 &&
        (preview.splitSamples[sampleIndex] ? (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            className="micro-sample-callout"
            title={intl.formatMessage(
              { id: "order.microPreview.sampleSplit" },
              { count: cases.length },
            )}
            subtitle={intl.formatList(
              cases.map((entry) =>
                intl.formatMessage(
                  { id: "order.microPreview.caseIn" },
                  {
                    tests: intl.formatList(entry.testNames, {
                      type: "conjunction",
                    }),
                    unit: entry.labUnitName,
                  },
                ),
              ),
              { type: "unit" },
            )}
          />
        ) : (
          <Callout
            kind="info"
            lowContrast
            className="micro-sample-callout"
            statusIconDescription={intl.formatMessage({
              id: "notification.info",
            })}
            subtitle={
              <>
                {cases.map((entry, index) => (
                  <span
                    key={`case-${index}`}
                    className="micro-sample-note__line"
                  >
                    {sentence(entry)}
                  </span>
                ))}
                {siteName && (
                  <span className="micro-sample-note__site">
                    {intl.formatMessage(
                      { id: "order.microPreview.site" },
                      { site: siteName },
                    )}
                  </span>
                )}
              </>
            }
          />
        ))}
      {unitWarnings.map((warning, index) => (
        <InlineNotification
          key={`unit-${warning.labUnitId}-${index}`}
          kind="warning"
          lowContrast
          hideCloseButton
          className="micro-sample-callout"
          title={intl.formatMessage(
            { id: "order.microPreview.noOtherWork" },
            { unit: warning.labUnitName },
          )}
        />
      ))}
      {ordinaryTests.length > 0 && (
        <p className="micro-sample-note__results">
          {intl.formatMessage(
            { id: "order.microPreview.staysInResults" },
            { tests: intl.formatList(ordinaryTests, { type: "unit" }) },
          )}
        </p>
      )}
    </>
  );
}

export default function MicroOrderPreview({ preview }) {
  const intl = useIntl();
  if (!preview || preview.hidden || preview.loading) return null;
  const text = (key, values) =>
    intl.formatMessage({ id: `order.microPreview.${key}` }, values);
  const describeCondition = (condition) => {
    let subject = condition.testName;
    if (condition.componentLabel) {
      subject = text("component", {
        test: subject,
        component: condition.componentLabel,
      });
    }
    if (condition.sampleTypeName) {
      subject = text("specimen", {
        test: subject,
        type: condition.sampleTypeName,
      });
    }
    return text(`condition.${condition.relation}`, {
      subject,
      value: condition.value,
      value2: condition.value2,
    });
  };
  const answer = preview.preview;
  const hasSets = (answer?.cases || []).some((entry) => entry.bottles?.length);
  const hasReflexRules = (answer?.reflexRules || []).length > 0;
  if (!preview.failed && !hasSets && !hasReflexRules) return null;
  return (
    <div className="order-micro-preview">
      {preview.failed ? (
        <>
          <InlineNotification
            kind="error"
            title={text("failed")}
            subtitle={text("failedDetail")}
            hideCloseButton
          />
          <Button kind="ghost" onClick={preview.retry}>
            {intl.formatMessage({ id: "common.retry" })}
          </Button>
        </>
      ) : (
        <>
          {(answer?.cases || []).map((entry, index) => (
            <CultureSetSummary
              key={`case-${index}`}
              specimens={entry.bottles}
              warnings={entry.setWarnings}
            />
          ))}
          {(answer?.reflexRules || []).map((rule, index) => (
            <p key={`rule-${index}`}>
              {text("reflex", {
                name: rule.name,
                conditions: rule.conditions
                  .map(describeCondition)
                  .map((condition) =>
                    rule.conditions.length > 1 ? `(${condition})` : condition,
                  )
                  .join(text(rule.overall === "ALL" ? "and" : "or")),
                tests: rule.addedTests.join(", "),
              })}
            </p>
          ))}
        </>
      )}
    </div>
  );
}
