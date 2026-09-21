import React from "react";
import { Tile } from "@carbon/react";
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
 * A row with nothing to show renders an em dash and says so, rather than a
 * zero or a blank: an unknown value and a value of zero are different facts,
 * and only the caller can tell them apart.
 */
const CaseSummaryPanel = ({ rows = [] }) => {
  const intl = useIntl();

  return (
    <Tile className="case-view__summary">
      <h2>
        <FormattedMessage id="caseView.label.caseSummary" />
      </h2>
      {rows.map(({ id, labelKey, value }) => {
        const isEmpty = value === null || value === undefined || value === "";

        return (
          <div className="case-view__summary-row" key={id ?? labelKey}>
            <span className="case-view__summary-label">
              {intl.formatMessage({ id: labelKey })}
            </span>
            {isEmpty ? (
              <span className="case-view__summary-label">
                <FormattedMessage id="caseView.label.notRecorded" />
              </span>
            ) : (
              value
            )}
          </div>
        );
      })}
    </Tile>
  );
};

export default CaseSummaryPanel;
