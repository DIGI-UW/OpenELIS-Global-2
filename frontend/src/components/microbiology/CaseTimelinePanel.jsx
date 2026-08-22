import React, { useEffect, useRef, useState } from "react";
import { Add } from "@carbon/icons-react";
import { Button, Stack, Tag, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  formatCulturePurpose,
  formatMicrobiologyEnum,
} from "./MicrobiologyLabels";

const RECENT_TIMELINE_EVENT_LIMIT = 30;

const parseStructuredData = (structuredData) => {
  if (!structuredData) return {};
  if (typeof structuredData === "object") return structuredData;
  try {
    return JSON.parse(structuredData);
  } catch (_error) {
    return {};
  }
};

const activityPresentation = (intl, activity) => {
  if (activity.activityType !== "CULTURE_PURPOSE_CHANGED") {
    return {
      title: formatMicrobiologyEnum(activity.activityType),
      note: activity.note,
    };
  }
  const data = parseStructuredData(activity.structuredData);
  return {
    title: intl.formatMessage({
      id: "microbiology.case.timeline.culturePurposeChanged",
    }),
    note: intl.formatMessage(
      { id: "microbiology.case.timeline.culturePurposeChange" },
      {
        from: formatCulturePurpose(intl, data.fromPurpose),
        to: formatCulturePurpose(intl, data.toPurpose),
      },
    ),
  };
};

const CaseTimelinePanel = ({
  activities = [],
  timelineSectionId,
  onAddNote,
  saving = false,
}) => {
  const intl = useIntl();
  const [addingNote, setAddingNote] = useState(false);
  const [note, setNote] = useState("");
  const [showAllEvents, setShowAllEvents] = useState(false);
  const addNoteTriggerRef = useRef(null);
  const noteFieldRef = useRef(null);
  const wasAddingNoteRef = useRef(false);
  const hasOlderEvents = activities.length > RECENT_TIMELINE_EVENT_LIMIT;
  const visibleActivities = showAllEvents
    ? activities
    : activities.slice(-RECENT_TIMELINE_EVENT_LIMIT);

  const saveNote = () => {
    Promise.resolve(onAddNote(note.trim()))
      .then(() => {
        setNote("");
        setAddingNote(false);
      })
      .catch(() => undefined);
  };

  const closeNote = () => {
    setNote("");
    setAddingNote(false);
  };

  useEffect(() => {
    if (addingNote) {
      noteFieldRef.current?.focus();
    } else if (wasAddingNoteRef.current) {
      addNoteTriggerRef.current?.focus();
    }
    wasAddingNoteRef.current = addingNote;
  }, [addingNote]);

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
            ref={addNoteTriggerRef}
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
        <div
          onKeyDown={(event) => {
            if (event.key === "Escape" && !saving) {
              event.preventDefault();
              closeNote();
            }
          }}
        >
          <Stack gap={4}>
            <TextArea
              ref={noteFieldRef}
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
                onClick={closeNote}
              >
                {intl.formatMessage({ id: "button.cancel" })}
              </Button>
            </div>
          </Stack>
        </div>
      )}
      <div className="cds--visually-hidden" role="status" aria-live="polite">
        {addingNote
          ? intl.formatMessage({
              id: "microbiology.case.timeline.noteExpanded",
            })
          : ""}
      </div>
      {activities.length === 0 ? (
        <p>{intl.formatMessage({ id: "microbiology.case.timeline.empty" })}</p>
      ) : (
        <>
          <ol className="microbiology-list">
            {visibleActivities.map((activity) => {
              const presentation = activityPresentation(intl, activity);
              return (
                <li
                  className="microbiology-list__row"
                  key={
                    activity.id ||
                    `${activity.activityType}-${activity.occurredAt}`
                  }
                >
                  <div className="microbiology-inline-actions">
                    <strong>{presentation.title}</strong>
                    <Tag type="cool-gray" size="sm">
                      {intl.formatMessage({
                        id:
                          activity.activityType === "MANUAL_NOTE"
                            ? "microbiology.case.timeline.manual"
                            : "microbiology.case.timeline.auto",
                      })}
                    </Tag>
                  </div>
                  {presentation.note &&
                    (activity.activityType === "CULTURE_PURPOSE_CHANGED" ? (
                      <span>{presentation.note}</span>
                    ) : (
                      `: ${presentation.note}`
                    ))}
                  {(activity.performedByDisplay ||
                    activity.performedBy ||
                    activity.occurredAt) && (
                    <div className="microbiology-list__meta">
                      {(activity.performedByDisplay ||
                        activity.performedBy) && (
                        <span>
                          {intl.formatMessage(
                            {
                              id: "microbiology.case.timeline.performedBy",
                            },
                            {
                              actor:
                                activity.performedByDisplay ||
                                activity.performedBy,
                            },
                          )}
                        </span>
                      )}
                      {activity.occurredAt && (
                        <time dateTime={activity.occurredAt}>
                          {intl.formatDate(activity.occurredAt, {
                            dateStyle: "medium",
                            timeStyle: "short",
                          })}
                        </time>
                      )}
                    </div>
                  )}
                </li>
              );
            })}
          </ol>
          {hasOlderEvents && (
            <Button
              kind="ghost"
              size="sm"
              onClick={() => setShowAllEvents((current) => !current)}
            >
              {intl.formatMessage(
                {
                  id: showAllEvents
                    ? "microbiology.case.timeline.showRecent"
                    : "microbiology.case.timeline.showAll",
                },
                {
                  count: showAllEvents
                    ? RECENT_TIMELINE_EVENT_LIMIT
                    : activities.length,
                },
              )}
            </Button>
          )}
        </>
      )}
    </section>
  );
};

export default CaseTimelinePanel;
