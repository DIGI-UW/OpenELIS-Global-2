import CultureSetSummary from "../../../microbiology/CultureSetSummary";
import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import {
  Tile,
  Tag,
  InlineLoading,
  InlineNotification,
  Button,
} from "@carbon/react";
import { previewMicrobiologyOrder } from "../../../microbiology/MicrobiologyService";

export default function MicroOrderPreview({ samples, savedOrder = false }) {
  const intl = useIntl();
  const [result, setResult] = useState(null);
  const [failedPayload, setFailedPayload] = useState(null);
  const [attempt, setAttempt] = useState(0);
  const payload = JSON.stringify({
    specimens: samples
      .filter((sample) => sample.sampleTypeId)
      .map((sample) => ({
        sampleTypeId: sample.sampleTypeId,
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

  if (existing || !hasTests) return null;
  // Hide the previous selection's answer immediately, even before its effect cleans up.
  const preview = result?.payload === payload ? result.response : null;
  if (preview && preview.cases.length === 0) return null;
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
  return (
    <Tile role="region" aria-label={text("title")} aria-live="polite">
      <h3>{text("title")}</h3>
      {failedPayload === payload ? (
        <>
          <InlineNotification
            kind="error"
            title={text("failed")}
            subtitle={text("failedDetail")}
            hideCloseButton
          />
          <Button kind="ghost" onClick={() => setAttempt((value) => value + 1)}>
            {intl.formatMessage({ id: "common.retry" })}
          </Button>
        </>
      ) : !preview ? (
        <InlineLoading description={text("loading")} />
      ) : (
        <div>
          {preview.cases.map((entry, index) => (
            <div key={`case-${index}`}>
              <p>
                <Tag type="blue">{text("opens")}</Tag>{" "}
                {text("case", {
                  tests: entry.testNames.join(", "),
                  unit: entry.labUnitName,
                  samples: entry.specimens
                    .map((sample) =>
                      text("sample", {
                        type: sample.sampleTypeName,
                        number: sample.index + 1,
                      }),
                    )
                    .join(", "),
                })}
              </p>
              <CultureSetSummary
                specimens={entry.bottles}
                warnings={entry.setWarnings}
              />
            </div>
          ))}
          {preview.ordinaryTests.map((entry) => (
            <p key={`${entry.specimenIndex}-${entry.testId}`}>
              <Tag type="gray">{text("ordinary")}</Tag>{" "}
              {text("stays", { test: entry.testName })}
            </p>
          ))}
          {preview.warnings.map((warning) => (
            <p key={`split-${warning.specimenIndex}`}>
              <Tag type="warm-gray">{text("splitTag")}</Tag>{" "}
              {text("split", {
                number: warning.specimenIndex + 1,
                count: warning.labUnits.length,
                units: warning.labUnits.join(", "),
              })}
            </p>
          ))}
          {preview.newUnitWarnings.map((warning) => (
            <p key={`unit-${warning.labUnitId}`}>
              <Tag type="warm-gray">{text("newUnitTag")}</Tag>{" "}
              {text("newUnit", {
                test: warning.testName,
                unit: warning.labUnitName,
              })}
            </p>
          ))}
          {preview.reflexRules.map((rule, index) => (
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
        </div>
      )}
    </Tile>
  );
}
