import { Page, TestInfo } from "@playwright/test";
import { showSceneLabel, showStepCard, showTitleCard } from "./title-card";
import { isVideoProject, videoPause } from "./video-pause";

export type DemoPresentation = {
  readonly isVideo: boolean;
  title: (
    title: string,
    subtitle?: string,
    durationMs?: number,
  ) => Promise<void>;
  step: (
    stepNumber: number,
    description: string,
    durationMs?: number,
  ) => Promise<void>;
  scene: (label: string) => Promise<void>;
  pause: (ms: number) => Promise<void>;
};

export function createDemoPresentation(
  page: Page,
  testInfo: TestInfo,
): DemoPresentation {
  const isVideo = isVideoProject(testInfo);

  return {
    isVideo,
    title: (title, subtitle, durationMs = 3000) =>
      showTitleCard(page, title, subtitle, durationMs, testInfo),
    step: (stepNumber, description, durationMs = 2000) =>
      showStepCard(page, stepNumber, description, durationMs, testInfo),
    scene: (label) => showSceneLabel(page, label, testInfo),
    pause: (ms) => videoPause(page, ms, testInfo),
  };
}
