import React from "react";
import {
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import StatusBadge from "../../caseView/StatusBadge";
import { stageLabel } from "../pathologyStages";

/**
 * The record of who moved the case and when, read-only: the operator and the
 * time come from the session and the server clock when the case is moved,
 * so nothing here is ever an input (S-8.2). A return is marked as one in
 * words, with the reason the bench gave beside it.
 *
 * A list of one stage's entries leaves the stage column out, since every row
 * would repeat it, and the Returned tag then sits with the entry time, the
 * return's reason under it, so the list keeps one column fewer where the
 * section is narrow. The processor run is shown where the list is of
 * processing entries, so a run corrected by a return stays visible beside
 * the one it replaced; there it comes first, since the run is what tells one
 * processing entry from another.
 *
 * The case's whole history leaves the completion columns out: an entry is
 * completed by the move that opens the next one, in the same moment and by
 * the same person, so the next row already says when and by whom, and the
 * two columns only took the room the reason needs. A one-stage list keeps
 * them, since there the next entry is not on the list.
 *
 * The list scrolls on its own where the centre column is too narrow for its
 * columns, so the scrolling region is a named stop in the tab order: a
 * keyboard user can reach it and scroll it, and a screen reader names it by
 * the list it holds.
 */
const StageEventList = ({
  events,
  ariaLabelKey,
  showStage = true,
  showCompletion = true,
  showRunReference = false,
}) => {
  const intl = useIntl();

  const returned = (event) =>
    event.direction === "RETURN" && (
      <StatusBadge kind="partial" textKey="pathology.badge.returned" />
    );

  // With the stage column left out, the reason sits with the Returned tag.
  const showReasonColumn = showStage;
  const label = intl.formatMessage({ id: ariaLabelKey });

  // The run, where shown, leads the row.
  const headers = [
    showRunReference && "pathology.label.runReference",
    showStage && "pathology.label.stage",
    "pathology.label.enteredAt",
    "pathology.label.enteredBy",
    showCompletion && "common.completed",
    showCompletion && "pathology.label.completedBy",
    showReasonColumn && "common.reason",
  ].filter(Boolean);

  const runCell = (event) => (
    <StructuredListCell>{event.runReference}</StructuredListCell>
  );

  return (
    // The list scrolls on its own where the centre column is too narrow for
    // its columns, rather than running past the column.
    <div
      className="pathology-case-view__stage-events"
      role="region"
      aria-label={label}
      tabIndex={0}
    >
      <StructuredListWrapper isCondensed aria-label={label}>
        <StructuredListHead>
          <StructuredListRow head>
            {headers.map((key) => (
              <StructuredListCell head key={key}>
                <FormattedMessage id={key} />
              </StructuredListCell>
            ))}
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          {events.map((event) => (
            <StructuredListRow key={event.id}>
              {showRunReference && runCell(event)}
              {showStage && (
                <StructuredListCell>
                  <span className="pathology-case-view__stage-event-stage">
                    {stageLabel(intl, event.stage)}
                    {returned(event)}
                  </span>
                </StructuredListCell>
              )}
              <StructuredListCell>
                {showStage ? (
                  event.enteredAt
                ) : (
                  <>
                    <span className="pathology-case-view__stage-event-stage">
                      {event.enteredAt}
                      {returned(event)}
                    </span>
                    {event.reason && (
                      <span className="pathology-case-view__stage-event-reason">
                        {event.reason}
                      </span>
                    )}
                  </>
                )}
              </StructuredListCell>
              <StructuredListCell>{event.enteredBy}</StructuredListCell>
              {showCompletion && (
                <StructuredListCell>{event.completedAt}</StructuredListCell>
              )}
              {showCompletion && (
                <StructuredListCell>{event.completedBy}</StructuredListCell>
              )}
              {showReasonColumn && (
                <StructuredListCell>{event.reason}</StructuredListCell>
              )}
            </StructuredListRow>
          ))}
        </StructuredListBody>
      </StructuredListWrapper>
    </div>
  );
};

export default StageEventList;
