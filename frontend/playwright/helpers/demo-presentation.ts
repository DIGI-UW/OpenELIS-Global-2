import * as fs from "fs";
import * as path from "path";
import { Locator, Page, TestInfo } from "@playwright/test";
import { showSceneLabel, showStepCard, showTitleCard } from "./title-card";
import { isVideoProject, videoPause } from "./video-pause";

/** Directory where loose screenshot evidence files are saved (video mode). */
const EVIDENCE_DIR = new URL("../../e2e-evidence", import.meta.url).pathname;

export type DemoPresentation = {
  readonly isVideo: boolean;
  chapter: (options: {
    eyebrow: string;
    title: string;
    subtitle?: string;
    accent?: string;
    durationMs?: number;
  }) => Promise<void>;
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
  evidence: (
    name: string,
    options?: { fullPage?: boolean; locator?: Locator },
  ) => Promise<void>;
};

export async function captureEvidenceScreenshot(
  page: Page,
  testInfo: TestInfo,
  name: string,
  options: { fullPage?: boolean; locator?: Locator } = {},
) {
  const screenshot = options.locator
    ? await options.locator.screenshot()
    : await page.screenshot({ fullPage: options.fullPage ?? false });
  await testInfo.attach(name, {
    body: screenshot,
    contentType: "image/png",
  });
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
  const safeName = name.replace(/[^a-zA-Z0-9._-]/g, "-");
  fs.writeFileSync(path.join(EVIDENCE_DIR, `${safeName}.png`), screenshot);
}

export function createDemoPresentation(
  page: Page,
  testInfo: TestInfo,
): DemoPresentation {
  const isVideo = isVideoProject(testInfo);

  return {
    isVideo,
    chapter: ({
      eyebrow,
      title,
      subtitle,
      accent = "#0f62fe",
      durationMs = 3500,
    }) =>
      showTitleCard(page, title, subtitle, durationMs, testInfo, {
        eyebrow,
        accent,
      }),
    title: (title, subtitle, durationMs = 4500) =>
      showTitleCard(page, title, subtitle, durationMs, testInfo),
    step: (stepNumber, description, durationMs = 3000) =>
      showStepCard(page, stepNumber, description, durationMs, testInfo),
    scene: (label) => showSceneLabel(page, label, testInfo),
    pause: (ms) => videoPause(page, ms, testInfo),
    evidence: async (
      name: string,
      options: { fullPage?: boolean; locator?: Locator } = {},
    ) => {
      if (!isVideo) return;
      await captureEvidenceScreenshot(page, testInfo, name, options);
    },
  };
}
