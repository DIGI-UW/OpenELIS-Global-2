import * as fs from "fs";
import * as path from "path";
import { Page, TestInfo } from "@playwright/test";
import { showSceneLabel, showStepCard, showTitleCard } from "./title-card";
import { isVideoProject, videoPause } from "./video-pause";

/** Directory where loose screenshot evidence files are saved (video mode). */
const EVIDENCE_DIR = path.resolve(__dirname, "../../e2e-evidence");

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
  evidence: (name: string) => Promise<void>;
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
    evidence: async (name: string) => {
      if (!isVideo) return;
      const screenshot = await page.screenshot({ fullPage: true });
      // Attach to HTML report
      await testInfo.attach(name, {
        body: screenshot,
        contentType: "image/png",
      });
      // Also save as loose file for direct viewing
      fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
      const safeName = name.replace(/[^a-zA-Z0-9._-]/g, "-");
      fs.writeFileSync(path.join(EVIDENCE_DIR, `${safeName}.png`), screenshot);
    },
  };
}
