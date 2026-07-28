import React from "react";
import { ProgressIndicator, ProgressStep } from "@carbon/react";
import { useIntl } from "react-intl";
import type { AnalyzerSetupStep } from "../analyzerRoutes";
import "./AnalyzerSetupProgress.css";

interface AnalyzerSetupProgressProps {
  currentStep: AnalyzerSetupStep;
}

const steps: Array<{ id: AnalyzerSetupStep; messageId: string }> = [
  { id: "instrument", messageId: "analyzer.setup.step.instrument" },
  { id: "verify", messageId: "analyzer.setup.step.verify" },
  { id: "connect", messageId: "analyzer.setup.step.connect" },
  { id: "review", messageId: "analyzer.setup.step.review" },
];

const AnalyzerSetupProgress = ({ currentStep }: AnalyzerSetupProgressProps) => {
  const intl = useIntl();
  const currentIndex = Math.max(
    0,
    steps.findIndex((step) => step.id === currentStep),
  );

  return (
    <ProgressIndicator
      className="analyzer-setup-progress"
      currentIndex={currentIndex}
      spaceEqually
      data-testid="analyzer-setup-progress"
      data-current-step={currentStep}
    >
      {steps.map((step) => (
        <ProgressStep
          key={step.id}
          label={intl.formatMessage({ id: step.messageId })}
        />
      ))}
    </ProgressIndicator>
  );
};

export default AnalyzerSetupProgress;
