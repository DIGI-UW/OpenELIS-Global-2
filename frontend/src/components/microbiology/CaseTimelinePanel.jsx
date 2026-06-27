import React, { useState } from "react";
import { Button, Select, SelectItem, Tag, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";

const STAGE_OPTIONS = [
  "SETUP_RECORDED",
  "INCUBATING",
  "GROWTH_DETECTED",
  "NO_GROWTH_READY",
  "REJECTED",
];

const CaseTimelinePanel = ({ activities = [], onRecordActivity, saving }) => {
  const intl = useIntl();
  const [nextStage, setNextStage] = useState("SETUP_RECORDED");
  const [note, setNote] = useState("");

  const submit = () => {
    onRecordActivity({ nextStage, note });
    setNote("");
  };

  return (
    <>
      <section
        className="microbiology-card microbiology-card--current"
        aria-labelledby="microbiology-setup-heading"
      >
        <div className="microbiology-card__header">
          <div>
            <h3 id="microbiology-setup-heading">
              {intl.formatMessage({ id: "microbiology.case.setup" })}
            </h3>
            <p className="microbiology-card__hint">
              {intl.formatMessage({ id: "microbiology.case.setup.hint" })}
            </p>
          </div>
        </div>
        <div className="microbiology-form-grid">
          <Select
            id="microbiology-next-stage"
            labelText={intl.formatMessage({
              id: "microbiology.case.nextStage",
            })}
            value={nextStage}
            onChange={(event) => setNextStage(event.target.value)}
          >
            {STAGE_OPTIONS.map((stage) => (
              <SelectItem key={stage} value={stage} text={stage} />
            ))}
          </Select>
          <div />
          <div className="microbiology-form-grid__wide">
            <TextArea
              id="microbiology-activity-note"
              labelText={intl.formatMessage({
                id: "microbiology.case.activityNote",
              })}
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
          </div>
          <div>
            <Button onClick={submit} disabled={saving}>
              {intl.formatMessage({ id: "microbiology.case.recordActivity" })}
            </Button>
          </div>
        </div>
      </section>

      <section
        className="microbiology-card"
        aria-labelledby="microbiology-timeline-heading"
      >
        <div className="microbiology-card__header">
          <div>
            <h3 id="microbiology-timeline-heading">
              {intl.formatMessage({ id: "microbiology.case.timeline" })}
            </h3>
            <p className="microbiology-card__hint">
              {intl.formatMessage({ id: "microbiology.case.timeline.hint" })}
            </p>
          </div>
          <Tag type="cool-gray">
            {activities.length}{" "}
            {intl.formatMessage({ id: "microbiology.case.events" })}
          </Tag>
        </div>
        {activities.length === 0 ? (
          <p>
            {intl.formatMessage({ id: "microbiology.case.timeline.empty" })}
          </p>
        ) : (
          <ol className="microbiology-list">
            {activities.map((activity) => (
              <li
                className="microbiology-list__row"
                key={
                  activity.id ||
                  `${activity.activityType}-${activity.occurredAt}`
                }
              >
                <strong>{activity.activityType}</strong>
                {activity.note ? `: ${activity.note}` : ""}
                {activity.occurredAt && (
                  <div className="microbiology-list__meta">
                    {activity.occurredAt}
                  </div>
                )}
              </li>
            ))}
          </ol>
        )}
      </section>
    </>
  );
};

export default CaseTimelinePanel;
