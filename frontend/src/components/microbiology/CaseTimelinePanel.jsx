import React, { useState } from "react";
import { Button, Select, SelectItem, Stack, TextArea } from "@carbon/react";
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
    <section aria-labelledby="microbiology-timeline-heading">
      <Stack gap={5}>
        <h3 id="microbiology-timeline-heading">
          {intl.formatMessage({ id: "microbiology.case.timeline" })}
        </h3>
        <div>
          {activities.length === 0 ? (
            <p>
              {intl.formatMessage({ id: "microbiology.case.timeline.empty" })}
            </p>
          ) : (
            <ol>
              {activities.map((activity) => (
                <li
                  key={
                    activity.id ||
                    `${activity.activityType}-${activity.occurredAt}`
                  }
                >
                  <strong>{activity.activityType}</strong>
                  {activity.note ? `: ${activity.note}` : ""}
                </li>
              ))}
            </ol>
          )}
        </div>
        <Select
          id="microbiology-next-stage"
          labelText={intl.formatMessage({ id: "microbiology.case.nextStage" })}
          value={nextStage}
          onChange={(event) => setNextStage(event.target.value)}
        >
          {STAGE_OPTIONS.map((stage) => (
            <SelectItem key={stage} value={stage} text={stage} />
          ))}
        </Select>
        <TextArea
          id="microbiology-activity-note"
          labelText={intl.formatMessage({
            id: "microbiology.case.activityNote",
          })}
          value={note}
          onChange={(event) => setNote(event.target.value)}
        />
        <Button onClick={submit} disabled={saving}>
          {intl.formatMessage({ id: "microbiology.case.recordActivity" })}
        </Button>
      </Stack>
    </section>
  );
};

export default CaseTimelinePanel;
