import React, { useState } from "react";
import { Add } from "@carbon/icons-react";
import { Button, Stack, Tag, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const CaseTimelinePanel = ({
  activities = [],
  timelineSectionId,
  onAddNote,
  saving = false,
}) => {
  const intl = useIntl();
  const [addingNote, setAddingNote] = useState(false);
  const [note, setNote] = useState("");

  const saveNote = () => {
    Promise.resolve(onAddNote(note.trim()))
      .then(() => {
        setNote("");
        setAddingNote(false);
      })
      .catch(() => undefined);
  };

  return (
    <section
      id={timelineSectionId}
      className="microbiology-card"
      data-testid="microbiology-timeline-card"
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
        <div className="microbiology-inline-actions">
          <Tag type="cool-gray">
            {activities.length}{" "}
            {intl.formatMessage({ id: "microbiology.case.events" })}
          </Tag>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Add}
            disabled={saving}
            onClick={() => setAddingNote(true)}
          >
            {intl.formatMessage({ id: "microbiology.case.timeline.addNote" })}
          </Button>
        </div>
      </div>
      {addingNote && (
        <Stack gap={4}>
          <TextArea
            id="microbiology-timeline-note"
            labelText={intl.formatMessage({
              id: "microbiology.case.timeline.note",
            })}
            value={note}
            onChange={(event) => setNote(event.target.value)}
          />
          <div className="microbiology-inline-actions">
            <Button
              size="sm"
              disabled={!note.trim() || saving}
              onClick={saveNote}
            >
              {intl.formatMessage({
                id: "microbiology.case.timeline.saveNote",
              })}
            </Button>
            <Button
              kind="secondary"
              size="sm"
              disabled={saving}
              onClick={() => {
                setNote("");
                setAddingNote(false);
              }}
            >
              {intl.formatMessage({ id: "button.cancel" })}
            </Button>
          </div>
        </Stack>
      )}
      {activities.length === 0 ? (
        <p>{intl.formatMessage({ id: "microbiology.case.timeline.empty" })}</p>
      ) : (
        <ol className="microbiology-list">
          {activities.map((activity) => (
            <li
              className="microbiology-list__row"
              key={
                activity.id || `${activity.activityType}-${activity.occurredAt}`
              }
            >
              <div className="microbiology-inline-actions">
                <strong>{formatMicrobiologyEnum(activity.activityType)}</strong>
                <Tag type="cool-gray" size="sm">
                  {intl.formatMessage({
                    id:
                      activity.activityType === "MANUAL_NOTE"
                        ? "microbiology.case.timeline.manual"
                        : "microbiology.case.timeline.auto",
                  })}
                </Tag>
              </div>
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
  );
};

export default CaseTimelinePanel;
