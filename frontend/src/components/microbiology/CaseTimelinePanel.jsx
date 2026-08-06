import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const CaseTimelinePanel = ({ activities = [], timelineSectionId }) => {
  const intl = useIntl();

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
        <Tag type="cool-gray">
          {activities.length}{" "}
          {intl.formatMessage({ id: "microbiology.case.events" })}
        </Tag>
      </div>
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
                <strong>
                  {formatMicrobiologyEnum(activity.activityType, intl)}
                </strong>
                <Tag type="cool-gray" size="sm">
                  {intl.formatMessage({
                    id: "microbiology.case.timeline.auto",
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
