import React from "react";
import { Heading, Section, Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./caseView.scss";

/**
 * The sticky summary a pathology, immunohistochemistry or cytology case view
 * keeps at its side: current stage, how many of each physical object the
 * screen tracks, the headline finding, the report status.
 *
 * Every value this panel shows is handed to it already computed. A count of
 * blocks embedded cannot answer which block is still outstanding unless it
 * came from summing identified rows in the first place, so the summing has
 * to happen where those rows live, not here. Accepting a finished value
 * rather than a list is what keeps this panel from becoming a second place a
 * count could be wrong.
 *
 * A row may carry a title naming what a counted value counted; it is shown on
 * hover and repeated in visually hidden text, since a title alone never
 * reaches assistive technology.
 *
 * A row with nothing to show renders an em dash and says so, rather than a
 * zero or a blank: an unknown value and a value of zero are different facts,
 * and only the caller can tell them apart. A boolean is likewise a recorded
 * fact with two values, so it is rendered as Yes or No. React would otherwise
 * render nothing at all for either value, leaving the row's label beside an
 * empty space that reads as neither an answer nor an absence.
 */
const CaseSummaryPanel = ({ rows = [] }) => {
  const intl = useIntl();

  return (
    <Tile className="case-view__summary">
      {/* Level follows the adopting screen's Section nesting. */}
      <Section>
        <Heading className="case-view__summary-title">
          <FormattedMessage id="caseView.label.caseSummary" />
        </Heading>
      </Section>
      {rows.map(({ id, labelKey, value, title }) => {
        const isEmpty = value === null || value === undefined || value === "";

        let content = value;
        if (isEmpty) {
          content = (
            <span className="case-view__summary-label">
              <FormattedMessage id="caseView.label.notRecorded" />
            </span>
          );
        } else if (typeof value === "boolean") {
          content = intl.formatMessage({
            id: value ? "label.yes" : "label.no",
          });
        }

        return (
          <div
            className="case-view__summary-row"
            key={id ?? labelKey}
            title={title}
          >
            <span className="case-view__summary-label">
              {intl.formatMessage({ id: labelKey })}
            </span>
            {content}
            {title && <span className="cds--visually-hidden">{title}</span>}
          </div>
        );
      })}
    </Tile>
  );
};

export default CaseSummaryPanel;
