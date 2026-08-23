import type { ComponentType } from "react";

export type AnalyzerSetupStep = "instrument" | "verify" | "connect";

export interface AnalyzerSetupProps {
  currentStep?: AnalyzerSetupStep;
  onClose: () => void;
}

declare const AnalyzerSetup: ComponentType<AnalyzerSetupProps>;

export default AnalyzerSetup;
