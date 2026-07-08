/**
 * QITile Component
 *
 * Shared tile shell for the QI Dashboard (OGC-695). Renders either a live
 * KPI (value + delta + secondary context + detail link) or a gray
 * "coming soon" placeholder annotated with the ticket that lights it up.
 */

import React from "react";
import {
  Tile,
  SkeletonText,
  Toggletip,
  ToggletipButton,
  ToggletipContent,
} from "@carbon/react";
import { Information } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { Link, useHistory } from "react-router-dom";

const QITile = ({
  testId,
  titleKey,
  tooltipKey,
  accent = "blue",
  loading,
  primary,
  delta,
  targetLine,
  secondary,
  message,
  detailPath,
  comingSoonTicket,
}) => {
  const intl = useIntl();
  const history = useHistory();

  if (comingSoonTicket) {
    return (
      <Tile className="qi-tile qi-tile--gray" data-testid={testId}>
        <div className="qi-tile__title-row">
          <span className="qi-tile__title">
            <FormattedMessage id={titleKey} />
          </span>
        </div>
        <p className="qi-tile__message">
          <FormattedMessage
            id="qa.qi.dashboard.comingSoon"
            values={{ ticket: comingSoonTicket }}
          />
        </p>
      </Tile>
    );
  }

  return (
    <Tile
      className={`qi-tile qi-tile--${accent} ${detailPath ? "qi-tile--clickable" : ""}`}
      data-testid={testId}
      onClick={detailPath ? () => history.push(detailPath) : undefined}
    >
      <div className="qi-tile__title-row">
        <span className="qi-tile__title">
          <FormattedMessage id={titleKey} />
        </span>
        {tooltipKey && (
          <span onClick={(e) => e.stopPropagation()}>
            <Toggletip align="bottom">
              <ToggletipButton
                label={intl.formatMessage({ id: "qa.qi.dashboard.tileInfo" })}
              >
                <Information />
              </ToggletipButton>
              <ToggletipContent>
                <p>
                  <FormattedMessage id={tooltipKey} />
                </p>
              </ToggletipContent>
            </Toggletip>
          </span>
        )}
      </div>
      {loading ? (
        <SkeletonText paragraph lineCount={3} />
      ) : message ? (
        <p className="qi-tile__message">{message}</p>
      ) : (
        <>
          <div className="qi-tile__value-row">
            <span className="qi-tile__value">{primary}</span>
            {delta && (
              <span className={`qi-tile__delta qi-tile__delta--${delta.tone}`}>
                {delta.arrow} {delta.text}
              </span>
            )}
          </div>
          {targetLine && <p className="qi-tile__target">{targetLine}</p>}
          {secondary && <p className="qi-tile__secondary">{secondary}</p>}
        </>
      )}
      {detailPath && (
        <Link className="qi-tile__detail-link" to={detailPath}>
          <FormattedMessage id="qa.qi.dashboard.viewDetail" /> ↗
        </Link>
      )}
    </Tile>
  );
};

export default QITile;
