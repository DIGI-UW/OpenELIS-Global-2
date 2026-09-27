/**
 * QITile Component
 *
 * The one KPI tile the QA module renders — on the QI Dashboard (OGC-695) and
 * on the QA Overview's Today row, which shows the same five indicators. Value,
 * delta, target and threshold captions, secondary context and the detail link
 * are each optional, so a surface shows only what it has.
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
  thresholdLine,
  secondary,
  message,
  detailPath,
}) => {
  const intl = useIntl();
  const history = useHistory();

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
          {thresholdLine && (
            <p className="qi-tile__threshold">{thresholdLine}</p>
          )}
          {secondary && <p className="qi-tile__secondary">{secondary}</p>}
        </>
      )}
      {detailPath && (
        // The tile itself navigates; the link is here so the route is
        // reachable from the keyboard. Stop the click from reaching the tile,
        // or following the link would navigate twice.
        <Link
          className="qi-tile__detail-link"
          to={detailPath}
          onClick={(e) => e.stopPropagation()}
        >
          <FormattedMessage id="qa.qi.dashboard.viewDetail" /> ↗
        </Link>
      )}
    </Tile>
  );
};

export default QITile;
