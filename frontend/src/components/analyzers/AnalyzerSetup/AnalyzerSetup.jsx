import React from "react";
import { Button } from "@carbon/react";
import { Close } from "@carbon/icons-react";
import { useIntl } from "react-intl";

import "./AnalyzerSetup.scss";

const SETUP_STEPS = ["instrument", "verify", "connect"];

const AnalyzerSetup = ({ currentStep = "instrument", onClose }) => {
  const intl = useIntl();
  const currentIndex = Math.max(SETUP_STEPS.indexOf(currentStep), 0);

  return (
    <section
      className="analyzer-setup"
      aria-labelledby="analyzer-setup-title"
      data-testid="analyzer-setup"
    >
      <header className="analyzer-setup__header">
        <h2 id="analyzer-setup-title">
          {intl.formatMessage({ id: "analyzer.setup.title" })}
        </h2>
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          renderIcon={Close}
          iconDescription={intl.formatMessage({
            id: "analyzer.setup.close",
          })}
          onClick={onClose}
          data-testid="analyzer-setup-close"
        />
      </header>

      <ol
        className="analyzer-setup__steps"
        aria-label={intl.formatMessage({ id: "analyzer.setup.steps" })}
      >
        {SETUP_STEPS.map((step, index) => {
          const state =
            index < currentIndex
              ? "complete"
              : index === currentIndex
                ? "current"
                : "future";

          return (
            <li
              key={step}
              className={`analyzer-setup__step analyzer-setup__step--${state}`}
              aria-current={state === "current" ? "step" : undefined}
              aria-disabled={state === "future" ? true : undefined}
            >
              <div className="analyzer-setup__step-heading">
                <span className="analyzer-setup__step-number" aria-hidden>
                  {index + 1}
                </span>
                <h3>
                  {intl.formatMessage({ id: `analyzer.setup.${step}.title` })}
                </h3>
              </div>
            </li>
          );
        })}
      </ol>
    </section>
  );
};

export default AnalyzerSetup;
