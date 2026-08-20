import React, { useEffect, useMemo, useState } from "react";
import { Tag, TextArea } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ReferenceSection from "./ReferenceSection";
import { getFromOpenElisServer } from "../../utils/Utils";
import {
  InterpretationBucket,
  bucketMatches,
  bucketTagType,
  matchingBucket,
} from "./interpretationMatch";

/**
 * OGC-1026 (R7, FR-G1) + OGC-1021 (R2 FR-G) — the Interpretation section:
 * categorical rule buckets configured on the test (Test Catalog Editor,
 * OGC-949 M7) auto-matched to the entered value, layered over a free-text
 * editor. Picking a bucket applies its text as a non-binding suggestion the
 * tech can edit. The text rides the row's Save as a report-visible
 * "Interpretation" note. Auto-opens when a rule fires (FR-C4 auto-open).
 */
interface BucketWithComponent extends InterpretationBucket {
  componentId?: string;
}

interface InterpretationSectionProps {
  testId?: string;
  componentId?: string;
  resultValue?: string;
  latestInterpretation?: string;
  draft: string | null;
  onDraftChange: (draft: string | null) => void;
  editable: boolean;
  openOverride: boolean | undefined;
  onToggle: (open: boolean) => void;
}

const InterpretationSection: React.FC<InterpretationSectionProps> = ({
  testId,
  componentId,
  resultValue,
  latestInterpretation,
  draft,
  onDraftChange,
  editable,
  openOverride,
  onToggle,
}) => {
  const intl = useIntl();
  const [buckets, setBuckets] = useState<InterpretationBucket[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    if (!testId) {
      setLoaded(true);
      return;
    }
    getFromOpenElisServer(
      `/rest/results-entry/test/${testId}/interpretations`,
      (body: BucketWithComponent[]) => {
        const all = Array.isArray(body) ? body : [];
        const scoped = componentId
          ? all.filter((b) => b.componentId === componentId)
          : all;
        // a single-component test's buckets apply even when the row carries no
        // component id (legacy rows)
        setBuckets(scoped.length > 0 ? scoped : all);
        setLoaded(true);
      },
    );
  }, [testId, componentId]);

  const fired = useMemo(
    () => matchingBucket(buckets, resultValue),
    [buckets, resultValue],
  );

  const text = draft !== null ? draft : latestInterpretation || "";
  const hasContent =
    buckets.length > 0 || Boolean(latestInterpretation) || editable;
  if (!loaded || !hasContent) {
    return null;
  }

  const open = openOverride !== undefined ? openOverride : Boolean(fired);

  return (
    <ReferenceSection
      sectionId="interpretation"
      title={<FormattedMessage id="label.results.section.interpretation" />}
      summary={
        fired
          ? intl.formatMessage(
              { id: "label.results.interpretation.ruleFired" },
              { 0: fired.valueMatch },
            )
          : intl.formatMessage({ id: "label.results.interpretation.summary" })
      }
      open={open}
      autoOpened={openOverride === undefined && Boolean(fired)}
      autoOpenHint={
        <FormattedMessage id="label.results.interpretation.autoOpen" />
      }
      onToggle={onToggle}
    >
      {fired && (
        <div className="unifiedRuleBanner" data-testid="interpretation-rule">
          <FormattedMessage
            id="label.results.interpretation.ruleBanner"
            values={{ 0: resultValue, 1: fired.valueMatch }}
          />
        </div>
      )}
      <div className="unifiedInterpretationLayout">
        {buckets.length > 0 && (
          <div className="unifiedBucketList">
            {[...buckets]
              .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
              .map((bucket) => {
                const applied =
                  draft !== null && draft.trim() === (bucket.text || "").trim();
                const matches = bucketMatches(bucket.valueMatch, resultValue);
                return (
                  <button
                    type="button"
                    key={bucket.id || bucket.valueMatch}
                    className={`unifiedBucket${applied ? " unifiedBucket--applied" : ""}`}
                    disabled={!editable}
                    onClick={() => onDraftChange(bucket.text || "")}
                    data-testid={`bucket-${bucket.valueMatch}`}
                  >
                    <Tag size="sm" type={bucketTagType(bucket)}>
                      {bucket.valueMatch}
                    </Tag>
                    {matches && (
                      <Tag size="sm" type="blue">
                        <FormattedMessage id="label.results.interpretation.match" />
                      </Tag>
                    )}
                    <div className="unifiedBucketText">{bucket.text}</div>
                  </button>
                );
              })}
          </div>
        )}
        <div className="unifiedInterpretationText">
          <TextArea
            id={`interpretation-${testId}-${componentId || "primary"}`}
            labelText={intl.formatMessage({
              id: "label.results.interpretation.freeText",
            })}
            rows={4}
            disabled={!editable}
            value={text}
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
              onDraftChange(e.target.value)
            }
          />
        </div>
      </div>
      <div className="unifiedHistoryFootnote">
        <FormattedMessage id="label.results.interpretation.footnote" />
      </div>
    </ReferenceSection>
  );
};

export default InterpretationSection;
