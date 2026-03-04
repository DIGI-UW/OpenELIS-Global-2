import React from "react";
import { FormattedMessage } from "react-intl";
import { Tag } from "@carbon/react";
import "./NCEInlineWrapper.scss";

/**
 * NCEContextBanner displays auto-populated context information
 * for the NCE being reported (lab number, test, result value, etc.).
 */
const NCEContextBanner = ({ context }) => {
  if (!context) return null;

  return (
    <div className="nce-context-banner">
      <div className="nce-context-banner__fields">
        {context.labNumber && (
          <div className="nce-context-banner__field">
            <span className="nce-context-banner__label">
              <FormattedMessage id="nce.context.labNumber" />:
            </span>
            <span className="nce-context-banner__value">
              {context.labNumber}
            </span>
          </div>
        )}
        {context.testName && (
          <div className="nce-context-banner__field">
            <span className="nce-context-banner__label">
              <FormattedMessage id="nce.context.testName" />:
            </span>
            <span className="nce-context-banner__value">
              {context.testName}
            </span>
          </div>
        )}
        {context.resultValue && (
          <div className="nce-context-banner__field">
            <span className="nce-context-banner__label">
              <FormattedMessage id="nce.context.resultValue" />:
            </span>
            <span className="nce-context-banner__value">
              {context.resultValue}
            </span>
          </div>
        )}
        {context.patientInfo && (
          <div className="nce-context-banner__field">
            <span className="nce-context-banner__label">
              <FormattedMessage id="nce.context.patientInfo" />:
            </span>
            <span className="nce-context-banner__value">
              {context.patientInfo}
            </span>
          </div>
        )}
        {context.qualityFlags?.length > 0 && (
          <div className="nce-context-banner__field">
            <span className="nce-context-banner__label">
              <FormattedMessage id="nce.context.qualityFlags" />:
            </span>
            <span className="nce-context-banner__value">
              {context.qualityFlags.map((flag, index) => (
                <Tag key={index} type="red" size="sm">
                  {flag}
                </Tag>
              ))}
            </span>
          </div>
        )}
      </div>
    </div>
  );
};

export default NCEContextBanner;
